package com.cyberheist.security;

import com.cyberheist.stealth.Guard;
import com.cyberheist.util.Logger;
import com.cyberheist.world.AlarmPanel;
import com.cyberheist.world.Door;
import com.cyberheist.world.SecurityCamera;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Facility Security Director coordinating reactions between guards, cameras,
 * alarms, doors, and facility-wide threat escalation.
 * Replaces decorative NPCs with a dynamic, interconnected security network.
 */
public final class SecurityDirector {

    private static final Logger LOGGER = Logger.getLogger(SecurityDirector.class);

    /**
     * Active communication link between security entities for visual feedback.
     */
    public static final class CommunicationLink {
        public final Point2D.Double source;
        public final Point2D.Double target;
        public final String message;
        public double lifetime;

        public CommunicationLink(Point2D.Double source, Point2D.Double target, String message, double lifetime) {
            this.source = source;
            this.target = target;
            this.message = message;
            this.lifetime = lifetime;
        }
    }

    private SecurityLevel currentLevel = SecurityLevel.NORMAL;
    private double levelDecayTimer = 0.0;
    private static final double DECAY_TIME_ALERT = 22.0;
    private static final double DECAY_TIME_LOCKDOWN = 30.0;
    private static final double DECAY_TIME_SUSPICIOUS = 15.0;

    // Disturbance metrics
    private int disabledCamerasCount = 0;
    private int discoveredBodiesCount = 0;
    private int activeAlarmCount = 0;
    private Point2D.Double lastKnownIntruderPos = null;
    private String currentStatusDetail = "Facility perimeter secure.";

    // Communication visual links
    private final List<CommunicationLink> activeLinks = new ArrayList<>();
    private final List<SecurityEventListener> listeners = new ArrayList<>();

    // Reference position of central security desk (Noida Data Center security checkpoint)
    private final Point2D.Double securityHQ = new Point2D.Double(880.0, 360.0);

    public SecurityDirector() {
        // Default constructor
    }

    public void addListener(SecurityEventListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public SecurityLevel getCurrentLevel() {
        return currentLevel;
    }

    public String getCurrentStatusDetail() {
        return currentStatusDetail;
    }

    public Point2D.Double getLastKnownIntruderPos() {
        return lastKnownIntruderPos;
    }

    public List<CommunicationLink> getActiveLinks() {
        return activeLinks;
    }

    public int getDisabledCamerasCount() {
        return disabledCamerasCount;
    }

    public int getDiscoveredBodiesCount() {
        return discoveredBodiesCount;
    }

    public int getActiveAlarmCount() {
        return activeAlarmCount;
    }

    public void elevateSecurity(SecurityLevel targetLevel, String cause, Point2D.Double stimulusPos) {
        if (targetLevel.getSeverity() > currentLevel.getSeverity()) {
            SecurityLevel old = currentLevel;
            currentLevel = targetLevel;
            resetDecayTimer();
            currentStatusDetail = cause;
            if (stimulusPos != null) {
                this.lastKnownIntruderPos = new Point2D.Double(stimulusPos.x, stimulusPos.y);
            }
            LOGGER.info("[SECURITY DIRECTOR] Escalating: " + old + " -> " + currentLevel + " (" + cause + ")");
            notifyListeners(old, currentLevel, cause);
        } else if (targetLevel == currentLevel) {
            // Refresh timer
            resetDecayTimer();
            currentStatusDetail = cause;
            if (stimulusPos != null) {
                this.lastKnownIntruderPos = new Point2D.Double(stimulusPos.x, stimulusPos.y);
            }
        }
    }

    private void resetDecayTimer() {
        switch (currentLevel) {
            case LOCKDOWN -> levelDecayTimer = DECAY_TIME_LOCKDOWN;
            case ALERT -> levelDecayTimer = DECAY_TIME_ALERT;
            case SUSPICIOUS -> levelDecayTimer = DECAY_TIME_SUSPICIOUS;
            case NORMAL -> levelDecayTimer = 0.0;
        }
    }

    private void notifyListeners(SecurityLevel oldLevel, SecurityLevel newLevel, String cause) {
        for (SecurityEventListener listener : listeners) {
            try {
                listener.onSecurityLevelChanged(oldLevel, newLevel, cause);
            } catch (Exception e) {
                LOGGER.error("Error invoking security listener: " + e.getMessage());
            }
        }
    }

    /**
     * Called when a camera's detection meter reaches 100%.
     */
    public void onCameraSpottedPlayer(SecurityCamera camera, Point2D.Double playerPos, List<Guard> guards) {
        this.lastKnownIntruderPos = new Point2D.Double(playerPos.x, playerPos.y);
        addCommunicationLink(new Point2D.Double(camera.getX(), camera.getY()), securityHQ,
                "INTRUDER CONFIRMED @ " + camera.getName(), 3.5);

        elevateSecurity(SecurityLevel.ALERT, "Visual contact confirmed by " + camera.getName(), playerPos);
        dispatchNearestGuards(playerPos, "Camera intrusion breach", guards, 2);
    }

    /**
     * Called when a camera feed is hacked or disabled by EMP.
     */
    public void onCameraDisabled(SecurityCamera camera, List<Guard> guards) {
        disabledCamerasCount++;
        Point2D.Double camPos = new Point2D.Double(camera.getX(), camera.getY());
        addCommunicationLink(camPos, securityHQ, "SIGNAL LOSS: " + camera.getName(), 2.8);

        if (disabledCamerasCount >= 2) {
            elevateSecurity(SecurityLevel.SUSPICIOUS, "Multiple CCTV feed failures detected (" + disabledCamerasCount + " offline)", camPos);
            // Dispatch guard to check first offline camera
            dispatchNearestGuards(camPos, "Check offline camera junction", guards, 1);
        } else {
            // First camera disabled: nearby guard may casually investigate
            currentStatusDetail = "Checking offline camera: " + camera.getName();
            dispatchNearestGuards(camPos, "Inspect CCTV junction", guards, 1);
        }
    }

    /**
     * Called when a patrolling guard visually discovers an unconscious or stunned colleague.
     */
    public void onGuardDiscoveredBody(Guard discoverer, Guard body, List<Guard> guards) {
        discoveredBodiesCount++;
        Point2D.Double bodyPos = new Point2D.Double(body.getX() + 14.0, body.getY() + 14.0);
        Point2D.Double discPos = new Point2D.Double(discoverer.getX() + 14.0, discoverer.getY() + 14.0);

        addCommunicationLink(discPos, securityHQ, "MAN DOWN! Officer unconscious!", 4.0);

        // Immediate escalation to ALERT; if already ALERT, escalate to LOCKDOWN
        SecurityLevel newLevel = (currentLevel == SecurityLevel.ALERT) ? SecurityLevel.LOCKDOWN : SecurityLevel.ALERT;
        elevateSecurity(newLevel, "Officer down discovered by " + discoverer.getName(), bodyPos);

        // Dispatch all active guards in area to converge
        dispatchNearestGuards(bodyPos, "Converge on downed officer position", guards, 3);
    }

    /**
     * Called when a guard spots a security door left standing open.
     */
    public void onGuardNoticedOpenDoor(Guard guard, Door door) {
        Point2D.Double doorCenter = new Point2D.Double(
                door.getBoundingBox().getMinX() + (door.getBoundingBox().getWidth() / 2.0),
                door.getBoundingBox().getMinY() + (door.getBoundingBox().getHeight() / 2.0)
        );
        addCommunicationLink(new Point2D.Double(guard.getX() + 14.0, guard.getY() + 14.0), doorCenter,
                "Unsecured doorway inspected", 2.0);

        if (currentLevel == SecurityLevel.NORMAL) {
            elevateSecurity(SecurityLevel.SUSPICIOUS, "Security breach: Unsecured door discovered (" + door.getName() + ")", doorCenter);
        }
    }

    /**
     * Called when an alarm siren panel is tripped or sabotaged failed.
     */
    public void onAlarmTriggered(AlarmPanel alarm, List<Guard> guards) {
        activeAlarmCount++;
        Point2D.Double alarmPos = new Point2D.Double(
                alarm.getBoundingBox().getMinX() + (alarm.getBoundingBox().getWidth() / 2.0),
                alarm.getBoundingBox().getMinY() + (alarm.getBoundingBox().getHeight() / 2.0)
        );
        addCommunicationLink(alarmPos, securityHQ, "SIREN ACTIVATED: " + alarm.getName(), 5.0);

        elevateSecurity(SecurityLevel.LOCKDOWN, "Facility siren active at " + alarm.getName(), alarmPos);
        dispatchNearestGuards(alarmPos, "Alarm siren convergence", guards, 3);
    }

    public void onAlarmDisarmed(AlarmPanel alarm) {
        if (activeAlarmCount > 0) {
            activeAlarmCount--;
        }
        if (activeAlarmCount == 0 && currentLevel == SecurityLevel.LOCKDOWN) {
            // Downgrade to ALERT with timer
            SecurityLevel old = currentLevel;
            currentLevel = SecurityLevel.ALERT;
            levelDecayTimer = DECAY_TIME_ALERT;
            currentStatusDetail = "Siren disarmed; perimeter sweep underway.";
            LOGGER.info("[SECURITY DIRECTOR] Alarm disarmed; downgrading from LOCKDOWN to ALERT.");
            notifyListeners(old, currentLevel, "Siren disarmed");
        }
    }

    /**
     * Called when player overrides the master security terminal.
     */
    public void onMasterTerminalOverride() {
        SecurityLevel old = currentLevel;
        currentLevel = SecurityLevel.NORMAL;
        levelDecayTimer = 0.0;
        currentStatusDetail = "Master security clearance overridden. Defense protocols suspended.";
        LOGGER.info("[SECURITY DIRECTOR] Master security console overridden. All defense protocols cleared.");
        notifyListeners(old, currentLevel, "Console overridden");
    }

    private void dispatchNearestGuards(Point2D.Double target, String reason, List<Guard> guards, int maxGuards) {
        if (guards == null || target == null) return;

        List<Guard> available = new ArrayList<>();
        for (Guard g : guards) {
            if (!g.isIncapacitated()) {
                available.add(g);
            }
        }

        // Sort by distance to target
        available.sort((a, b) -> {
            double da = Math.hypot(a.getX() + 14.0 - target.x, a.getY() + 14.0 - target.y);
            double db = Math.hypot(b.getX() + 14.0 - target.y, b.getY() + 14.0 - target.y);
            return Double.compare(da, db);
        });

        int dispatched = 0;
        for (Guard g : available) {
            if (dispatched >= maxGuards) break;
            if (g.getAI() != null) {
                g.getAI().onInvestigateDisturbance(target, reason);
                dispatched++;
            }
        }
    }

    public void addCommunicationLink(Point2D.Double source, Point2D.Double target, String message, double duration) {
        activeLinks.add(new CommunicationLink(source, target, message, duration));
    }

    public void update(double deltaTime, List<Guard> guards) {
        // 1. Update communication links lifetimes
        Iterator<CommunicationLink> it = activeLinks.iterator();
        while (it.hasNext()) {
            CommunicationLink link = it.next();
            link.lifetime -= deltaTime;
            if (link.lifetime <= 0.0) {
                it.remove();
            }
        }

        // 2. Check if active alarms are sounding (holds LOCKDOWN)
        if (activeAlarmCount > 0) {
            levelDecayTimer = DECAY_TIME_LOCKDOWN;
            return;
        }

        // 3. Process natural decay if no fresh disturbances
        if (levelDecayTimer > 0.0) {
            levelDecayTimer -= deltaTime;
            if (levelDecayTimer <= 0.0) {
                decaySecurityLevel();
            }
        }
    }

    private void decaySecurityLevel() {
        SecurityLevel old = currentLevel;
        switch (currentLevel) {
            case LOCKDOWN -> {
                currentLevel = SecurityLevel.ALERT;
                levelDecayTimer = DECAY_TIME_ALERT;
                currentStatusDetail = "Lockdown released. High alert ongoing.";
                notifyListeners(old, currentLevel, "Lockdown expired");
            }
            case ALERT -> {
                currentLevel = SecurityLevel.SUSPICIOUS;
                levelDecayTimer = DECAY_TIME_SUSPICIOUS;
                currentStatusDetail = "Facility alert decaying. Security remain vigilant.";
                notifyListeners(old, currentLevel, "Alert expired");
            }
            case SUSPICIOUS -> {
                currentLevel = SecurityLevel.NORMAL;
                levelDecayTimer = 0.0;
                currentStatusDetail = "Facility perimeter resumed normal status.";
                notifyListeners(old, currentLevel, "Suspicion expired");
            }
            case NORMAL -> {}
        }
    }

    // State persistence getters and setters
    public void setSecurityLevelDirect(SecurityLevel level, double decayTimer) {
        this.currentLevel = level;
        this.levelDecayTimer = decayTimer;
    }

    public double getLevelDecayTimer() {
        return levelDecayTimer;
    }
}
