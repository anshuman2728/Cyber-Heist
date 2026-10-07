package com.cyberheist.ai;

import com.cyberheist.ai.pathfinding.AStarPathfinder;
import com.cyberheist.ai.state.GuardState;
import com.cyberheist.collision.CollisionWorld;
import com.cyberheist.entity.Player;
import com.cyberheist.security.SecurityDirector;
import com.cyberheist.security.SecurityLevel;
import com.cyberheist.stealth.DetectionState;
import com.cyberheist.stealth.Guard;
import com.cyberheist.stealth.LineOfSight;
import com.cyberheist.stealth.NoiseEvent;
import com.cyberheist.util.Logger;
import com.cyberheist.world.AlarmPanel;
import com.cyberheist.world.Door;
import com.cyberheist.world.SecurityCamera;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * State-based AI controller driving guard behavior, patrol routes,
 * imperfect human perception, noise investigation, environmental disturbance inspection,
 * search patterns, and A* path execution.
 */
public final class GuardAI {

    private static final Logger LOGGER = Logger.getLogger(GuardAI.class);
    private static final Random RANDOM = new Random(42);

    private final Guard guard;
    private final AStarPathfinder pathfinder;
    private GuardState state = GuardState.PATROL;

    // Patrol Route & Navigation
    private final List<Point2D.Double> patrolRoute = new ArrayList<>();
    private int patrolIndex = 0;
    private double waypointWaitTimer = 0.0;
    private static final double WAYPOINT_WAIT_TIME = 2.0;

    // Path Execution
    private List<Point2D.Double> currentPath = new ArrayList<>();
    private int pathIndex = 0;
    private Point2D.Double pathDestination = null;

    // Pathfinding Throttling & Profiling
    private double pathRecalcCooldown = 0.0;
    private double pathRecalcInterval = 0.35; // Configurable frequency: ~3 Hz
    private long totalPathCalculations = 0;

    // Sensory History & De-duplication
    private NoiseEvent lastHeardNoise = null;

    // Imperfect Awareness & Targets
    private Point2D.Double lastKnownPlayerPos = null;
    private Point2D.Double investigationTarget = null;
    private Door investigatedDoor = null;
    private SecurityCamera investigatedCamera = null;
    private double stateTimer = 0.0;
    private double searchDuration = 5.0;
    private int searchStep = 0;

    // Movement Speeds
    private static final double SPEED_PATROL = 110.0;
    private static final double SPEED_INVESTIGATE = 140.0;
    private static final double SPEED_CHASE = 230.0;

    // Environmental Scan Throttling
    private double envScanTimer = 0.0;

    // Guard Anti-Stuck & Fail-Safe Mechanics
    private double lastGuardX = 0.0;
    private double lastGuardY = 0.0;
    private double stuckTimer = 0.0;

    public GuardAI(Guard guard, AStarPathfinder pathfinder) {
        this.guard = guard;
        this.pathfinder = pathfinder;
    }

    public void addPatrolWaypoint(double x, double y) {
        patrolRoute.add(new Point2D.Double(x, y));
    }

    /**
     * Backward-compatible update overload.
     */
    public void update(double deltaTime, Player player, GuardCommunicator communicator) {
        update(deltaTime, player, communicator, null, null, null, null, null, null);
    }

    /**
     * Executes AI state machine tick with full facility and environmental awareness.
     */
    public void update(double deltaTime, Player player, GuardCommunicator communicator,
                       SecurityDirector director, CollisionWorld collisionWorld,
                       List<Guard> allGuards, List<SecurityCamera> cameras,
                       List<Door> doors, List<AlarmPanel> alarms) {
        if (guard.isUnconscious()) {
            if (state != GuardState.UNCONSCIOUS) {
                transitionTo(GuardState.UNCONSCIOUS);
            }
            return;
        }

        if (guard.isStunned()) {
            if (state != GuardState.STUNNED) {
                transitionTo(GuardState.STUNNED);
            }
            return;
        } else if (state == GuardState.STUNNED) {
            // Guard recovered from stun
            transitionTo(GuardState.SUSPICIOUS);
        }

        stateTimer += deltaTime;
        if (pathRecalcCooldown > 0.0) {
            pathRecalcCooldown -= deltaTime;
        }

        // Throttled environmental disturbance scanning (unconscious guards, offline cams, open doors, sirens)
        envScanTimer -= deltaTime;
        if (envScanTimer <= 0.0) {
            envScanTimer = 0.20 + (Math.abs(guard.getName().hashCode()) % 5) * 0.03;
            performEnvironmentalScans(collisionWorld, director, allGuards, cameras, doors, alarms, communicator);
        }

        // Evaluate sensory stimuli from guard's detection meter
        DetectionState detectionTier = guard.getDetectionMeter().getState();

        // High priority: Direct visual detection overrides lower states
        if (detectionTier == DetectionState.DETECTED) {
            if (state != GuardState.CHASE && state != GuardState.ALERT) {
                transitionTo(GuardState.ALERT);
                if (player != null) {
                    this.lastKnownPlayerPos = new Point2D.Double(player.getX() + 14.0, player.getY() + 14.0);
                }
                if (communicator != null) {
                    communicator.broadcastAlert(guard, new Point2D.Double(player.getX(), player.getY()),
                            "Visual contact confirmed!");
                }
                if (director != null && player != null) {
                    director.elevateSecurity(SecurityLevel.ALERT, "Visual contact by " + guard.getName(),
                            new Point2D.Double(player.getX(), player.getY()));
                }
            }
        }

        switch (state) {
            case PATROL -> handlePatrol(deltaTime, director);
            case SUSPICIOUS -> handleSuspicious(deltaTime, player);
            case INVESTIGATE -> handleInvestigate(deltaTime, player);
            case ALERT -> handleAlert(deltaTime, player);
            case CHASE -> handleChase(deltaTime, player);
            case SEARCH -> handleSearch(deltaTime, director);
            case RETURN_TO_PATROL -> handleReturnToPatrol(deltaTime);
            case STUNNED, UNCONSCIOUS -> {}
        }
    }

    private void performEnvironmentalScans(CollisionWorld collisionWorld, SecurityDirector director,
                                          List<Guard> allGuards, List<SecurityCamera> cameras,
                                          List<Door> doors, List<AlarmPanel> alarms,
                                          GuardCommunicator communicator) {
        if (state == GuardState.CHASE || state == GuardState.ALERT || guard.isIncapacitated()) {
            return;
        }

        double gx = guard.getX() + 14.0;
        double gy = guard.getY() + 14.0;
        double facing = guard.getFacingAngle();
        double halfFOV = Math.toRadians(guard.getVisionCone().getFovDegrees() / 2.0);

        // 1. Scan for unconscious colleagues in line of sight
        if (allGuards != null && collisionWorld != null) {
            for (Guard other : allGuards) {
                if (other == guard || !other.isIncapacitated()) continue;
                double ox = other.getX() + 14.0;
                double oy = other.getY() + 14.0;
                double dist = Math.hypot(ox - gx, oy - gy);
                if (dist > 220.0) continue; // Spatial early-out

                double angleToBody = Math.atan2(oy - gy, ox - gx);
                if (Math.abs(normalizeAngle(angleToBody - facing)) <= halfFOV) {
                    if (LineOfSight.hasClearLineOfSight(gx, gy, ox, oy, collisionWorld, guard)) {
                        LOGGER.info(guard.getName() + " discovered unconscious officer " + other.getName() + "!");
                        this.investigationTarget = new Point2D.Double(ox, oy);
                        transitionTo(GuardState.ALERT);
                        if (director != null) {
                            director.onGuardDiscoveredBody(guard, other, allGuards);
                        }
                        if (communicator != null) {
                            communicator.broadcastAlert(guard, new Point2D.Double(ox, oy), "Man down! Officer discovered unconscious!");
                        }
                        return;
                    }
                }
            }
        }

        // 2. Scan for active siren alarms
        if (alarms != null) {
            for (AlarmPanel alarm : alarms) {
                if (!alarm.isActiveAlarm()) continue;
                double ax = alarm.getBoundingBox().getMinX() + (alarm.getBoundingBox().getWidth() / 2.0);
                double ay = alarm.getBoundingBox().getMinY() + (alarm.getBoundingBox().getHeight() / 2.0);
                double dist = Math.hypot(ax - gx, ay - gy);
                if (dist <= 650.0) { // Siren heard facility-wide
                    this.investigationTarget = new Point2D.Double(ax, ay);
                    if (state == GuardState.PATROL || state == GuardState.RETURN_TO_PATROL) {
                        LOGGER.info(guard.getName() + " converging on active alarm siren " + alarm.getName());
                        transitionTo(GuardState.INVESTIGATE);
                    }
                    return;
                }
            }
        }

        // 3. Scan for unsecured / ajar security doors
        if (doors != null && collisionWorld != null && state == GuardState.PATROL) {
            for (Door door : doors) {
                if (!door.isOpen()) continue;
                String dname = door.getName().toLowerCase();
                if (!dname.contains("checkpoint") && !dname.contains("office") && !dname.contains("vault")) {
                    continue;
                }
                double dx = door.getX() + (door.getWidth() / 2.0);
                double dy = door.getY() + (door.getHeight() / 2.0);
                double dist = Math.hypot(dx - gx, dy - gy);
                if (dist > 160.0) continue;

                double angleToDoor = Math.atan2(dy - gy, dx - gx);
                if (Math.abs(normalizeAngle(angleToDoor - facing)) <= halfFOV) {
                    if (LineOfSight.hasClearLineOfSight(gx, gy, dx, dy, collisionWorld, guard)) {
                        this.investigatedDoor = door;
                        this.investigationTarget = new Point2D.Double(dx, dy);
                        LOGGER.info(guard.getName() + " noticed unsecured security door standing open: " + door.getName());
                        transitionTo(GuardState.INVESTIGATE);
                        if (director != null) {
                            director.onGuardNoticedOpenDoor(guard, door);
                        }
                        return;
                    }
                }
            }
        }

        // 4. Scan for offline / disabled cameras
        if (cameras != null && collisionWorld != null && state == GuardState.PATROL) {
            for (SecurityCamera cam : cameras) {
                if (!cam.isDisabled()) continue;
                double cx = cam.getX();
                double cy = cam.getY();
                double dist = Math.hypot(cx - gx, cy - gy);
                if (dist > 180.0) continue;

                double angleToCam = Math.atan2(cy - gy, cx - gx);
                if (Math.abs(normalizeAngle(angleToCam - facing)) <= halfFOV) {
                    if (LineOfSight.hasClearLineOfSight(gx, gy, cx, cy, collisionWorld, guard)) {
                        this.investigatedCamera = cam;
                        this.investigationTarget = new Point2D.Double(cx, cy);
                        LOGGER.info(guard.getName() + " noticed offline CCTV camera: " + cam.getName());
                        transitionTo(GuardState.INVESTIGATE);
                        if (director != null) {
                            director.addCommunicationLink(new Point2D.Double(gx, gy), new Point2D.Double(cx, cy),
                                    "Inspecting offline CCTV feed", 2.5);
                        }
                        return;
                    }
                }
            }
        }
    }

    private double normalizeAngle(double angle) {
        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }

    private void handlePatrol(double deltaTime, SecurityDirector director) {
        if (patrolRoute.isEmpty()) return;

        double patrolSpeed = SPEED_PATROL;
        double waitTime = WAYPOINT_WAIT_TIME;

        if (director != null) {
            SecurityLevel sec = director.getCurrentLevel();
            patrolSpeed = sec.getGuardPatrolSpeed();
            waitTime = (sec == SecurityLevel.NORMAL) ? WAYPOINT_WAIT_TIME :
                       (sec == SecurityLevel.SUSPICIOUS) ? 1.0 : 0.4;
        }

        Point2D.Double targetWaypoint = patrolRoute.get(patrolIndex);

        // If at waypoint, pause and look around
        double dist = Math.hypot(targetWaypoint.x - (guard.getX() + 14), targetWaypoint.y - (guard.getY() + 14));
        if (dist <= 18.0) {
            waypointWaitTimer += deltaTime;
            if (waypointWaitTimer >= waitTime) {
                waypointWaitTimer = 0.0;
                patrolIndex = (patrolIndex + 1) % patrolRoute.size();
                requestPath(patrolRoute.get(patrolIndex).x, patrolRoute.get(patrolIndex).y, true);
            }
        } else {
            if (currentPath.isEmpty() && pathRecalcCooldown <= 0.0) {
                requestPath(targetWaypoint.x, targetWaypoint.y, true);
            }
            followPath(patrolSpeed, deltaTime);
        }

        // Suspicion trigger from stealth system
        if (guard.getDetectionMeter().getState() == DetectionState.SUSPICIOUS) {
            transitionTo(GuardState.SUSPICIOUS);
        }
    }

    private void handleSuspicious(double deltaTime, Player player) {
        // Face toward sound target or player if known
        Point2D.Double aimTarget = investigationTarget;
        if (aimTarget == null && player != null && guard.isPlayerInVision()) {
            aimTarget = new Point2D.Double(player.getX() + 14.0, player.getY() + 14.0);
        }
        if (aimTarget != null) {
            double angleToTarget = Math.atan2(aimTarget.y - (guard.getY() + 14.0), aimTarget.x - (guard.getX() + 14.0));
            guard.setFacingAngle(angleToTarget);
        }

        // Guard pauses, stares at suspicion source for 0.8s
        if (stateTimer >= 0.8) {
            DetectionState detectionState = guard.getDetectionMeter().getState();
            if (detectionState == DetectionState.DETECTED) {
                transitionTo(GuardState.ALERT);
            } else if (investigationTarget != null) {
                transitionTo(GuardState.INVESTIGATE);
            } else if (detectionState == DetectionState.SUSPICIOUS && player != null) {
                this.investigationTarget = new Point2D.Double(player.getX() + 14.0, player.getY() + 14.0);
                transitionTo(GuardState.INVESTIGATE);
            } else {
                transitionTo(GuardState.RETURN_TO_PATROL);
            }
        }
    }

    private void handleInvestigate(double deltaTime, Player player) {
        if (investigationTarget == null) {
            if (lastKnownPlayerPos != null) {
                investigationTarget = new Point2D.Double(lastKnownPlayerPos.x, lastKnownPlayerPos.y);
            } else {
                transitionTo(GuardState.RETURN_TO_PATROL);
                return;
            }
        }

        // Check if investigating a door: if arrived, secure and close it
        double distToTarget = Math.hypot(investigationTarget.x - (guard.getX() + 14),
                investigationTarget.y - (guard.getY() + 14));

        if (investigatedDoor != null && distToTarget <= 32.0) {
            if (investigatedDoor.isOpen()) {
                investigatedDoor.setOpen(false);
                LOGGER.info(guard.getName() + " secured and closed open door: " + investigatedDoor.getName());
            }
            investigatedDoor = null;
            transitionTo(GuardState.SEARCH);
            return;
        }

        // Check if investigating a camera: if arrived, acknowledge and search
        if (investigatedCamera != null && distToTarget <= 36.0) {
            LOGGER.info(guard.getName() + " completed visual inspection of offline CCTV camera: " + investigatedCamera.getName());
            investigatedCamera = null;
            transitionTo(GuardState.SEARCH);
            return;
        }

        // Anti-confusion timeout: If investigate exceeds 7.5s without reaching target, search area
        if (stateTimer >= 7.5) {
            LOGGER.info(guard.getName() + " investigation timed out. Shifting to search.");
            transitionTo(GuardState.SEARCH);
            return;
        }

        if (currentPath.isEmpty() && pathRecalcCooldown <= 0.0) {
            requestPath(investigationTarget.x, investigationTarget.y, true);
        }

        followPath(SPEED_INVESTIGATE, deltaTime);

        if (distToTarget <= 26.0) {
            // Reached suspicious location -> search area
            transitionTo(GuardState.SEARCH);
        }
    }

    private void handleAlert(double deltaTime, Player player) {
        // Brief shock/draw response (0.3s) before sprint pursuit
        if (stateTimer >= 0.3) {
            transitionTo(GuardState.CHASE);
        }
    }

    private void handleChase(double deltaTime, Player player) {
        boolean seePlayer = guard.isPlayerInVision() && guard.hasClearLOS();

        if (seePlayer) {
            // Update last known location continually while visible
            lastKnownPlayerPos = new Point2D.Double(player.getX() + 14, player.getY() + 14);

            // Throttled path recalculation towards player
            if (pathRecalcCooldown <= 0.0) {
                requestPath(lastKnownPlayerPos.x, lastKnownPlayerPos.y, false);
            }
            followPath(SPEED_CHASE, deltaTime);
        } else {
            // Line of sight broken! Rush to where the player was last seen
            if (lastKnownPlayerPos != null) {
                double distToLastSeen = Math.hypot(lastKnownPlayerPos.x - (guard.getX() + 14),
                        lastKnownPlayerPos.y - (guard.getY() + 14));

                if (distToLastSeen <= 28.0 || (currentPath.isEmpty() && pathRecalcCooldown <= 0.0) || stateTimer >= 6.0) {
                    // Reached last known point but player escaped -> begin searching sector
                    transitionTo(GuardState.SEARCH);
                } else {
                    if (currentPath.isEmpty()) {
                        requestPath(lastKnownPlayerPos.x, lastKnownPlayerPos.y, true);
                    }
                    followPath(SPEED_CHASE, deltaTime);
                }
            } else {
                transitionTo(GuardState.SEARCH);
            }
        }
    }

    private void handleSearch(double deltaTime, SecurityDirector director) {
        double currentSearchDuration = searchDuration;
        if (director != null && director.getCurrentLevel().isAtLeast(SecurityLevel.ALERT)) {
            currentSearchDuration = 7.5; // Alert searches are longer and more persistent
        }

        // Imperfect search: Guard scans around last known position, picking 2-3 randomized search points
        if (stateTimer >= currentSearchDuration) {
            LOGGER.info(guard.getName() + " lost player trail. Returning to patrol.");
            transitionTo(GuardState.RETURN_TO_PATROL);
            return;
        }

        if (currentPath.isEmpty()) {
            if (searchStep < 3) {
                searchStep++;
                // Pick an imperfect search spot within 100-140px
                double angle = RANDOM.nextDouble() * Math.PI * 2.0;
                double offsetDist = 80.0 + RANDOM.nextDouble() * 60.0;
                double sx = (guard.getX() + 14) + Math.cos(angle) * offsetDist;
                double sy = (guard.getY() + 14) + Math.sin(angle) * offsetDist;
                requestPath(sx, sy, true);
            } else {
                // Pause and look around
                guard.setFacingAngle(guard.getFacingAngle() + 1.8 * deltaTime);
            }
        } else {
            followPath(SPEED_PATROL * 0.9, deltaTime);
        }
    }

    private void handleReturnToPatrol(double deltaTime) {
        if (patrolRoute.isEmpty()) {
            transitionTo(GuardState.PATROL);
            return;
        }

        // Fail-safe timeout: Return to patrol must not get stuck forever
        if (stateTimer >= 8.5) {
            LOGGER.info(guard.getName() + " return-to-patrol timed out. Resetting to patrol.");
            transitionTo(GuardState.PATROL);
            return;
        }

        Point2D.Double targetWaypoint = patrolRoute.get(patrolIndex);
        double dist = Math.hypot(targetWaypoint.x - (guard.getX() + 14), targetWaypoint.y - (guard.getY() + 14));

        if (dist <= 24.0) {
            transitionTo(GuardState.PATROL);
        } else {
            if (currentPath.isEmpty() && pathRecalcCooldown <= 0.0) {
                requestPath(targetWaypoint.x, targetWaypoint.y, true);
            }
            followPath(SPEED_PATROL, deltaTime);
        }
    }

    /**
     * Steers guard smoothly along current A* path waypoints.
     */
    private void followPath(double speed, double deltaTime) {
        if (currentPath.isEmpty() || pathIndex >= currentPath.size()) {
            currentPath.clear();
            return;
        }

        Point2D.Double nextPoint = currentPath.get(pathIndex);
        double gx = guard.getX() + 14.0;
        double gy = guard.getY() + 14.0;

        double dx = nextPoint.x - gx;
        double dy = nextPoint.y - gy;
        double dist = Math.hypot(dx, dy);

        // Anti-stuck watchdog: detect if guard is making progress
        double moved = Math.hypot(gx - lastGuardX, gy - lastGuardY);
        if (moved < 0.8) {
            stuckTimer += deltaTime;
            if (stuckTimer >= 1.2) {
                // Guard has been stuck for 1.2s -> skip to next waypoint or recalculate path
                LOGGER.info(guard.getName() + " stuck while navigating. Advancing waypoint.");
                pathIndex++;
                stuckTimer = 0.0;
                if (pathIndex >= currentPath.size()) {
                    currentPath.clear();
                    pathRecalcCooldown = 0.0;
                }
                return;
            }
        } else {
            stuckTimer = 0.0;
        }
        lastGuardX = gx;
        lastGuardY = gy;

        if (dist <= 12.0) {
            pathIndex++;
            if (pathIndex >= currentPath.size()) {
                currentPath.clear();
            }
            return;
        }

        // Steer angle and integrate velocity
        double targetAngle = Math.atan2(dy, dx);
        guard.setFacingAngle(targetAngle);

        double step = speed * deltaTime;
        if (step > dist) step = dist;

        double vx = (dx / dist) * step;
        double vy = (dy / dist) * step;
        guard.setPosition(guard.getX() + vx, guard.getY() + vy);
    }

    /**
     * Requests a new path via A* pathfinder.
     */
    private void requestPath(double destX, double destY, boolean throttle) {
        if (throttle && pathRecalcCooldown > 0.0) {
            return;
        }

        pathRecalcCooldown = pathRecalcInterval;
        this.pathDestination = new Point2D.Double(destX, destY);
        totalPathCalculations++;

        long startNanos = System.nanoTime();
        List<Point2D.Double> path = pathfinder.findPath(guard.getX() + 14.0, guard.getY() + 14.0, destX, destY);
        long elapsedMicros = (System.nanoTime() - startNanos) / 1000;
        com.cyberheist.core.TelemetryService.getInstance().recordPathfindingSearch(elapsedMicros);

        if (path != null && !path.isEmpty()) {
            this.currentPath = new ArrayList<>(path);
            this.pathIndex = 0;
        } else {
            this.currentPath.clear();
        }
    }

    /**
     * Direct dispatch from SecurityDirector or disturbance triggers.
     */
    public void onInvestigateDisturbance(Point2D.Double target, String reason) {
        if (state == GuardState.CHASE || state == GuardState.ALERT || guard.isIncapacitated()) {
            return;
        }
        this.investigationTarget = new Point2D.Double(target.x, target.y);
        LOGGER.info(guard.getName() + " assigned disturbance investigation: " + reason + " at " + target);
        transitionTo(GuardState.INVESTIGATE);
    }

    /**
     * Responds to heard acoustic sound waves with human-like perceptual error.
     */
    public void onNoiseHeard(NoiseEvent noise) {
        if (state == GuardState.CHASE || state == GuardState.ALERT || guard.isIncapacitated()) {
            return; // Already actively pursuing
        }
        if (noise == lastHeardNoise) {
            return; // Already responded to this propagating acoustic wave
        }
        this.lastHeardNoise = noise;

        // Imperfection: add acoustic localization fuzz (+- 25px)
        double fuzzX = (RANDOM.nextDouble() - 0.5) * 50.0;
        double fuzzY = (RANDOM.nextDouble() - 0.5) * 50.0;
        this.investigationTarget = new Point2D.Double(noise.getX() + fuzzX, noise.getY() + fuzzY);

        LOGGER.info(String.format("%s heard %s noise! Investigating sound near (%.0f, %.0f)",
                guard.getName(), noise.getType().name(), investigationTarget.x, investigationTarget.y));

        if (state == GuardState.PATROL) {
            transitionTo(GuardState.SUSPICIOUS);
        } else {
            transitionTo(GuardState.INVESTIGATE);
        }
    }

    public void onRadioAlert(Point2D.Double alertLocation) {
        if (state == GuardState.PATROL || state == GuardState.SUSPICIOUS || state == GuardState.SEARCH) {
            this.investigationTarget = new Point2D.Double(alertLocation.x, alertLocation.y);
            LOGGER.info(guard.getName() + " responding to radio alert at: " + alertLocation);
            transitionTo(GuardState.INVESTIGATE);
        }
    }

    public void transitionTo(GuardState newState) {
        if (this.state != newState) {
            LOGGER.info(String.format("[%s] State transition: %s -> %s", guard.getName(), this.state, newState));
            this.state = newState;
            this.stateTimer = 0.0;
            this.currentPath.clear();
            this.pathIndex = 0;
            if (newState == GuardState.SEARCH) {
                this.searchStep = 0;
            }
        }
    }

    public GuardState getState() { return state; }
    public void setState(GuardState state) { transitionTo(state); }
    public List<Point2D.Double> getCurrentPath() { return Collections.unmodifiableList(currentPath); }
    public Point2D.Double getLastKnownPlayerPos() { return lastKnownPlayerPos; }
    public Point2D.Double getInvestigationTarget() { return investigationTarget; }
    public Point2D.Double getPathDestination() { return pathDestination; }
    public Guard getGuard() { return guard; }
    public double getPathRecalcInterval() { return pathRecalcInterval; }
    public void setPathRecalcInterval(double interval) { this.pathRecalcInterval = Math.max(0.05, interval); }
    public long getTotalPathCalculations() { return totalPathCalculations; }
}
