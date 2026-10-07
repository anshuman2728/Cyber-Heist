package com.cyberheist.stealth;

import com.cyberheist.collision.BoundingBox;
import com.cyberheist.collision.CollisionWorld;
import com.cyberheist.core.GameConfig;
import com.cyberheist.entity.Player;
import com.cyberheist.world.CoverObject;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Coordinates all stealth calculations, observer vision evaluations,
 * acoustic propagation, cover occlusion, and debug visualizations.
 * Calculations are completely decoupled from rendering.
 */
public final class StealthSystem {

    private final NoiseManager noiseManager = new NoiseManager();
    private final List<Guard> guards = new ArrayList<>();
    private final com.cyberheist.ai.GuardCommunicator communicator = new com.cyberheist.ai.GuardCommunicator();

    // Global Detection State
    private double highestDetection = 0.0;
    private DetectionState globalState = DetectionState.UNAWARE;

    // Security Context
    private com.cyberheist.security.SecurityDirector securityDirector = null;
    private List<com.cyberheist.world.SecurityCamera> cameras = null;
    private List<com.cyberheist.world.Door> doors = null;
    private List<com.cyberheist.world.AlarmPanel> alarms = null;

    // Debug Display Toggle
    private boolean debugVisualsEnabled = true;

    public void setSecurityContext(com.cyberheist.security.SecurityDirector director,
                                   List<com.cyberheist.world.SecurityCamera> cameras,
                                   List<com.cyberheist.world.Door> doors,
                                   List<com.cyberheist.world.AlarmPanel> alarms) {
        this.securityDirector = director;
        this.cameras = cameras;
        this.doors = doors;
        this.alarms = alarms;
    }

    public com.cyberheist.security.SecurityDirector getSecurityDirector() {
        return securityDirector;
    }

    public void addGuard(Guard guard) {
        if (guard != null && !guards.contains(guard)) {
            guards.add(guard);
            // Wire radio alert reception
            communicator.register((source, loc, reason) -> {
                if (source != guard && guard.getAI() != null) {
                    guard.getAI().onRadioAlert(loc);
                }
            });
        }
    }

    /**
     * Executes the stealth simulation cycle for the current tick.
     */
    public void update(double deltaTime, Player player, CollisionWorld collisionWorld, List<CoverObject> coverObjects) {
        // 1. Advance acoustic propagation and lifetime decay
        noiseManager.update(deltaTime);

        if (player == null) return;

        double px = player.getX() + 14.0;
        double py = player.getY() + 14.0;
        double playerSpeed = player.getCurrentSpeed();

        // 2. Evaluate environmental cover (is player adjacent to or occluded by a cover prop?)
        boolean inCover = isPlayerTakingCover(player.getBoundingBox(), coverObjects);

        // 3. Evaluate ambient lighting factor based on facility room
        double ambientLighting = resolveAmbientLighting(player.getX(), player.getY());

        // 4. Update each guard observer
        highestDetection = 0.0;

        for (Guard guard : guards) {
            // Check auditory stimuli
            NoiseEvent audible = noiseManager.findStrongestAudibleNoise(guard.getX(), guard.getY(), guard.getHearingRadius());
            guard.update(deltaTime, audible);
            if (audible != null && guard.getAI() != null) {
                guard.getAI().onNoiseHeard(audible);
            }

            // Line of sight raycasting from guard eyes to player center
            double gx = guard.getX() + 14.0;
            double gy = guard.getY() + 14.0;

            boolean clearLOS = LineOfSight.hasClearLineOfSight(gx, gy, px, py, collisionWorld, guard);
            Point2D.Double obstacleHit = clearLOS ? null : LineOfSight.findObstacleIntersection(gx, gy, px, py, collisionWorld, guard);

            // Evaluate sensory visibility and detection build/decay
            guard.evaluatePlayerVisibility(player.getX(), player.getY(), playerSpeed,
                    inCover, ambientLighting, clearLOS, obstacleHit, deltaTime);

            // Update state-based Guard AI with full facility and environmental awareness
            if (guard.getAI() != null) {
                guard.getAI().update(deltaTime, player, communicator,
                        securityDirector, collisionWorld, guards, cameras, doors, alarms);
            }

            double guardPct = guard.getDetectionMeter().getPercentage();
            if (guardPct > highestDetection) {
                highestDetection = guardPct;
            }
        }

        // 5. Update global detection tier
        this.globalState = DetectionState.fromPercentage(highestDetection);
    }

    private boolean isPlayerTakingCover(BoundingBox playerBox, List<CoverObject> coverObjects) {
        if (coverObjects == null || playerBox == null) return false;

        // Player is considered in cover if within 18px of a solid cover object (desk, partition, server rack)
        double coverPadding = 18.0;
        BoundingBox proximityZone = new BoundingBox(
                playerBox.getX() - coverPadding,
                playerBox.getY() - coverPadding,
                playerBox.getWidth() + (coverPadding * 2),
                playerBox.getHeight() + (coverPadding * 2)
        );

        for (CoverObject cover : coverObjects) {
            if (proximityZone.intersects(cover.getBoundingBox())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Lighting map by facility sector (affects visibility build rate).
     */
    public double resolveAmbientLighting(double x, double y) {
        if (y < 190) {
            return 0.55; // Exterior night street / streetlights
        } else if (y < 470) {
            return 1.0;  // Brightly lit corporate reception / security checkpoint
        } else if (y < 750) {
            return (x < 960) ? 0.95 : 0.65; // Office is bright, Server Room is dim blue
        } else {
            return (x < 1200) ? 0.60 : 0.70; // Vault is restricted/dim, Exit has emergency lights
        }
    }

    public void emitNoise(double x, double y, NoiseType type) {
        noiseManager.emit(x, y, type);
    }

    /**
     * Renders debug overlays (Vision cones, LOS ray lines, Noise wavefronts, detection meters).
     */
    public void renderDebugVisuals(Graphics2D g2d, Player player) {
        if (!debugVisualsEnabled) return;

        // 1. Render active acoustic noise ripples
        renderNoiseRipples(g2d);

        // 2. Render guard vision cones & line-of-sight raycasts
        for (Guard guard : guards) {
            renderGuardVisionAndLOS(g2d, guard, player);
        }

        // 3. Render active communication links between security entities
        if (securityDirector != null) {
            for (com.cyberheist.security.SecurityDirector.CommunicationLink link : securityDirector.getActiveLinks()) {
                g2d.setColor(new Color(0x00, 0xF0, 0xFF, 180));
                g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, new float[]{4.0f, 4.0f}, 0.0f));
                g2d.drawLine((int) Math.round(link.source.x), (int) Math.round(link.source.y),
                             (int) Math.round(link.target.x), (int) Math.round(link.target.y));
                g2d.setStroke(new BasicStroke(1.0f));

                int midX = (int) Math.round((link.source.x + link.target.x) / 2.0);
                int midY = (int) Math.round((link.source.y + link.target.y) / 2.0);
                g2d.setFont(GameConfig.FONT_CODE);
                g2d.setColor(new Color(0x00, 0xF0, 0xFF, 220));
                g2d.drawString("[COMMS] " + link.message, midX - 40, midY);
            }
        }
    }

    private void renderNoiseRipples(Graphics2D g2d) {
        for (NoiseEvent noise : noiseManager.getActiveNoises()) {
            double curRadius = noise.getCurrentRadius();
            double maxRadius = noise.getType().getRadius();
            float alpha = (float) Math.max(0.05, noise.getIntensity() * 0.45);

            Color waveColor = switch (noise.getType()) {
                case WEAPON -> new Color(1.0f, 0.2f, 0.2f, alpha);
                case EMP -> new Color(0.2f, 0.9f, 1.0f, alpha);
                case RUNNING -> new Color(1.0f, 0.7f, 0.1f, alpha);
                default -> new Color(0.8f, 0.9f, 1.0f, alpha);
            };

            int cx = (int) Math.round(noise.getX());
            int cy = (int) Math.round(noise.getY());
            int r = (int) Math.round(curRadius);

            g2d.setColor(waveColor);
            g2d.setStroke(new BasicStroke(1.8f));
            g2d.drawOval(cx - r, cy - r, r * 2, r * 2);

            // Max boundary dotted ring
            g2d.setColor(new Color(waveColor.getRed(), waveColor.getGreen(), waveColor.getBlue(), 30));
            int mr = (int) Math.round(maxRadius);
            g2d.drawOval(cx - mr, cy - mr, mr * 2, mr * 2);

            g2d.setStroke(new BasicStroke(1.0f));
            g2d.setFont(GameConfig.FONT_CODE);
            g2d.setColor(new Color(waveColor.getRed(), waveColor.getGreen(), waveColor.getBlue(), 180));
            g2d.drawString(noise.getType().name() + " (" + (int) curRadius + "px)", cx + r + 4, cy);
        }
    }

    private void renderGuardVisionAndLOS(Graphics2D g2d, Guard guard, Player player) {
        // Vision cone polygon
        Polygon cone = guard.getVisionPolygon();

        Color coneFill = switch (guard.getDetectionMeter().getState()) {
            case DETECTED -> new Color(255, 30, 30, 35);
            case SUSPICIOUS -> new Color(255, 180, 0, 25);
            default -> new Color(0, 240, 120, 18);
        };

        Color coneOutline = switch (guard.getDetectionMeter().getState()) {
            case DETECTED -> new Color(255, 30, 30, 120);
            case SUSPICIOUS -> new Color(255, 180, 0, 90);
            default -> new Color(0, 240, 120, 60);
        };

        g2d.setColor(coneFill);
        g2d.fillPolygon(cone);
        g2d.setColor(coneOutline);
        g2d.drawPolygon(cone);

        // Immediate peripheral vision circle
        int gx = (int) Math.round(guard.getX() + 14.0);
        int gy = (int) Math.round(guard.getY() + 14.0);
        int pr = (int) Math.round(guard.getVisionCone().getPeripheralRadius());
        g2d.drawOval(gx - pr, gy - pr, pr * 2, pr * 2);

        // --- AI DEBUG OVERLAYS ---
        if (guard.getAI() != null) {
            com.cyberheist.ai.GuardAI ai = guard.getAI();

            // 1. Render active A* Path
            List<Point2D.Double> path = ai.getCurrentPath();
            if (path != null && !path.isEmpty()) {
                g2d.setColor(new Color(0x00, 0xF0, 0xFF, 180));
                g2d.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, new float[]{6.0f, 6.0f}, 0.0f));

                double prevX = gx;
                double prevY = gy;
                for (Point2D.Double wp : path) {
                    g2d.drawLine((int) Math.round(prevX), (int) Math.round(prevY), (int) Math.round(wp.x), (int) Math.round(wp.y));
                    g2d.fillOval((int) Math.round(wp.x) - 3, (int) Math.round(wp.y) - 3, 6, 6);
                    prevX = wp.x;
                    prevY = wp.y;
                }
                g2d.setStroke(new BasicStroke(1.0f));
            }

            // 2. Render Last Known Player Position (Red crosshair)
            if (ai.getLastKnownPlayerPos() != null) {
                Point2D.Double lkp = ai.getLastKnownPlayerPos();
                int lx = (int) Math.round(lkp.x);
                int ly = (int) Math.round(lkp.y);
                g2d.setColor(new Color(255, 40, 40, 220));
                g2d.drawOval(lx - 10, ly - 10, 20, 20);
                g2d.drawLine(lx - 14, ly, lx + 14, ly);
                g2d.drawLine(lx, ly - 14, lx, ly + 14);
                g2d.setFont(GameConfig.FONT_CODE);
                g2d.drawString("LAST KNOWN", lx + 12, ly - 4);
            }

            // 3. Render Target (Investigation or Path Destination)
            Point2D.Double currentTarget = ai.getInvestigationTarget() != null ? ai.getInvestigationTarget() : ai.getPathDestination();
            if (currentTarget != null) {
                int tx = (int) Math.round(currentTarget.x);
                int ty = (int) Math.round(currentTarget.y);
                g2d.setColor(new Color(255, 185, 0, 220));
                g2d.drawOval(tx - 8, ty - 8, 16, 16);
                g2d.drawLine(tx - 11, ty, tx + 11, ty);
                g2d.drawLine(tx, ty - 11, tx, ty + 11);
                g2d.setFont(GameConfig.FONT_CODE);
                g2d.drawString("TARGET", tx + 10, ty - 3);
            }

            // 4. Overhead AI State Badge & Detection Value
            g2d.setFont(GameConfig.FONT_CODE);
            Color stateBadgeColor = switch (ai.getState()) {
                case CHASE, ALERT -> Color.RED;
                case INVESTIGATE, SUSPICIOUS -> new Color(255, 185, 0);
                case SEARCH -> new Color(0, 240, 255);
                default -> GameConfig.COLOR_GREEN;
            };
            g2d.setColor(stateBadgeColor);
            String debugLabel = String.format("[%s] Det: %.0f%%", ai.getState().name(), guard.getDetectionMeter().getPercentage());
            g2d.drawString(debugLabel, gx - 34, (int) Math.round(guard.getY()) - 22);
        }

        // Line-of-sight raycast visualization from guard eyes to player
        if (player != null) {
            int px = (int) Math.round(player.getX() + 14.0);
            int py = (int) Math.round(player.getY() + 14.0);

            if (guard.hasClearLOS()) {
                // Clear green line of sight
                g2d.setColor(new Color(0, 255, 100, 160));
                g2d.drawLine(gx, gy, px, py);
            } else if (guard.getCurrentLOSObstacleHit() != null) {
                // Obstructed ray: red line from guard to impact point
                Point2D.Double hit = guard.getCurrentLOSObstacleHit();
                g2d.setColor(new Color(255, 50, 50, 140));
                g2d.drawLine(gx, gy, (int) Math.round(hit.x), (int) Math.round(hit.y));
                // Hit marker dot on wall
                g2d.setColor(Color.RED);
                g2d.fillOval((int) Math.round(hit.x) - 3, (int) Math.round(hit.y) - 3, 6, 6);
            }
        }
    }

    public void toggleDebugVisuals() {
        this.debugVisualsEnabled = !this.debugVisualsEnabled;
    }

    public boolean isDebugVisualsEnabled() {
        return debugVisualsEnabled;
    }

    public double getHighestDetection() { return highestDetection; }
    public DetectionState getGlobalState() { return globalState; }
    public List<Guard> getGuards() { return Collections.unmodifiableList(guards); }
    public NoiseManager getNoiseManager() { return noiseManager; }
}
