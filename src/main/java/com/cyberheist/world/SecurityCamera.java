package com.cyberheist.world;

import com.cyberheist.collision.BoundingBox;
import com.cyberheist.collision.CollisionWorld;
import com.cyberheist.core.GameConfig;
import com.cyberheist.entity.Player;
import com.cyberheist.hacking.Hackable;
import com.cyberheist.hacking.HackingDifficulty;
import com.cyberheist.interact.Interactable;
import com.cyberheist.security.SecurityDirector;
import com.cyberheist.security.SecurityLevel;
import com.cyberheist.stealth.Guard;
import com.cyberheist.stealth.LineOfSight;
import com.cyberheist.util.Logger;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.Point2D;
import java.util.List;

/**
 * Wall-mounted CCTV surveillance camera with panning vision cone.
 * Realistically detects the player using Line-of-Sight raycasts, communicates
 * with the facility SecurityDirector, and reacts dynamically to security levels.
 */
public final class SecurityCamera implements Interactable, Hackable {

    private static final Logger LOGGER = Logger.getLogger(SecurityCamera.class);

    private final String name;
    private final double x;
    private final double y;
    private final double baseAngleDegrees;
    private final double sweepRangeDegrees;
    private final double coneDistance;
    private final double coneFOV;
    private final BoundingBox interactionZone;

    private double currentAngle;
    private double sweepTimer = 0.0;
    private double baseSweepSpeed = 0.8; // Radians per sec

    private boolean disabled = false;
    private boolean hacked = false;
    private double empDisableTimer = 0.0;

    // Sensory Detection Model
    private double detectionProgress = 0.0; // 0.0 to 1.0
    private boolean playerInCone = false;
    private boolean hasClearLOS = false;
    private boolean alertedDirector = false;

    public SecurityCamera(String name, double x, double y, double baseAngleDegrees,
                          double sweepRangeDegrees, double coneDistance, double coneFOV) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.baseAngleDegrees = baseAngleDegrees;
        this.sweepRangeDegrees = sweepRangeDegrees;
        this.coneDistance = coneDistance;
        this.coneFOV = coneFOV;
        this.currentAngle = Math.toRadians(baseAngleDegrees);

        double pad = 36.0;
        this.interactionZone = new BoundingBox(x - pad, y - pad, pad * 2, pad * 2);
    }

    public void update(double deltaTime) {
        update(deltaTime, null, null, null, null);
    }

    public void update(double deltaTime, Player player, CollisionWorld collisionWorld,
                       SecurityDirector director, List<Guard> guards) {
        if (empDisableTimer > 0.0) {
            empDisableTimer -= deltaTime;
            if (empDisableTimer <= 0.0 && !hacked) {
                this.disabled = false;
                LOGGER.info("Camera '" + name + "' rebooted from EMP overload. Feed restored.");
            }
        }

        if (disabled) {
            detectionProgress = 0.0;
            playerInCone = false;
            hasClearLOS = false;
            return;
        }

        // Adjust sweep speed dynamically based on facility SecurityLevel
        double speedMultiplier = 1.0;
        if (director != null) {
            speedMultiplier = director.getCurrentLevel().getCameraSweepSpeed() / 0.8;
        }

        sweepTimer += deltaTime * baseSweepSpeed * speedMultiplier;
        double offset = Math.sin(sweepTimer) * Math.toRadians(sweepRangeDegrees / 2.0);
        this.currentAngle = Math.toRadians(baseAngleDegrees) + offset;

        // Visual & Line-of-Sight detection against the player
        if (player != null && collisionWorld != null) {
            double px = player.getX() + 14.0;
            double py = player.getY() + 14.0;

            double dx = px - x;
            double dy = py - y;
            double dist = Math.hypot(dx, dy);

            if (dist <= coneDistance) {
                double angleToPlayer = Math.atan2(dy, dx);
                double angleDiff = Math.abs(normalizeAngle(angleToPlayer - currentAngle));
                double halfFOV = Math.toRadians(coneFOV / 2.0);

                if (angleDiff <= halfFOV) {
                    playerInCone = true;
                    // Raycast line of sight
                    hasClearLOS = LineOfSight.hasClearLineOfSight(x, y, px, py, collisionWorld, null);
                    if (hasClearLOS) {
                        double rateMultiplier = (director != null)
                                ? director.getCurrentLevel().getCameraDetectionSpeedMultiplier() : 1.0;
                        // Center of beam detects faster than edge
                        double centerFactor = 1.0 - (angleDiff / halfFOV) * 0.4;
                        detectionProgress = Math.min(1.0, detectionProgress + (0.95 * centerFactor * rateMultiplier * deltaTime));

                        if (detectionProgress >= 1.0 && !alertedDirector) {
                            alertedDirector = true;
                            if (director != null) {
                                director.onCameraSpottedPlayer(this, new Point2D.Double(px, py), guards);
                            }
                            com.cyberheist.audio.AudioManager.getInstance().play(com.cyberheist.audio.SoundEffect.ALARM_ALERT);
                            LOGGER.info("Camera '" + name + "' detected intruder! Dispatching security.");
                        }
                        return;
                    }
                } else {
                    playerInCone = false;
                    hasClearLOS = false;
                }
            } else {
                playerInCone = false;
                hasClearLOS = false;
            }
        } else {
            playerInCone = false;
            hasClearLOS = false;
        }

        // Decay detection if player broke line of sight
        if (detectionProgress > 0.0) {
            detectionProgress = Math.max(0.0, detectionProgress - 0.70 * deltaTime);
            if (detectionProgress < 0.20) {
                alertedDirector = false;
            }
        }
    }

    private double normalizeAngle(double angle) {
        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }

    public void render(Graphics2D g2d) {
        int cx = (int) Math.round(x);
        int cy = (int) Math.round(y);

        // 1. Surveillance cone on floor (hidden when disabled)
        if (!disabled) {
            double halfFOV = Math.toRadians(coneFOV / 2.0);
            double leftAngle = currentAngle - halfFOV;
            double rightAngle = currentAngle + halfFOV;

            int lx = cx + (int) Math.round(Math.cos(leftAngle) * coneDistance);
            int ly = cy + (int) Math.round(Math.sin(leftAngle) * coneDistance);
            int rx = cx + (int) Math.round(Math.cos(rightAngle) * coneDistance);
            int ry = cy + (int) Math.round(Math.sin(rightAngle) * coneDistance);

            Polygon conePoly = new Polygon(new int[]{cx, lx, rx}, new int[]{cy, ly, ry}, 3);

            // Dynamic color tint based on detection level
            Color fillColor;
            Color outlineColor;
            if (detectionProgress >= 1.0) {
                fillColor = new Color(0xE0, 0x20, 0x20, 45); // Red alert
                outlineColor = new Color(0xFF, 0x40, 0x40, 110);
            } else if (detectionProgress > 0.15) {
                fillColor = new Color(0xE0, 0x90, 0x20, 32); // Amber warning
                outlineColor = new Color(0xFF, 0xA0, 0x30, 85);
            } else {
                fillColor = new Color(0x30, 0x90, 0xC0, 18); // Calm security blue
                outlineColor = new Color(0x30, 0x90, 0xC0, 50);
            }

            g2d.setColor(fillColor);
            g2d.fillPolygon(conePoly);

            g2d.setColor(outlineColor);
            g2d.drawPolygon(conePoly);

            // If building detection, draw progress arc or alert badge
            if (detectionProgress > 0.05) {
                int meterW = 32;
                int meterH = 4;
                int mx = cx - (meterW / 2);
                int my = cy - 14;
                g2d.setColor(new Color(0, 0, 0, 160));
                g2d.fillRect(mx, my, meterW, meterH);
                g2d.setColor(detectionProgress >= 1.0 ? Color.RED : Color.YELLOW);
                g2d.fillRect(mx, my, (int) Math.round(meterW * detectionProgress), meterH);
            }
        }

        // 2. Camera wall mounting bracket
        g2d.setColor(new Color(0x2C, 0x35, 0x42));
        g2d.fillRect(cx - 5, cy - 5, 10, 10);
        g2d.setColor(disabled ? Color.DARK_GRAY : new Color(0x50, 0x60, 0x75));
        g2d.drawRect(cx - 5, cy - 5, 10, 10);

        // 3. Rotating camera lens barrel
        int barrelEndX = cx + (int) Math.round(Math.cos(currentAngle) * 9.0);
        int barrelEndY = cy + (int) Math.round(Math.sin(currentAngle) * 9.0);

        g2d.setColor(disabled ? Color.GRAY : new Color(0x80, 0x90, 0xA8));
        g2d.drawLine(cx, cy, barrelEndX, barrelEndY);

        // 4. Recording LED dot (Red when active, amber when suspicious, dark when disabled)
        if (disabled) {
            g2d.setColor(new Color(50, 50, 50));
        } else if (detectionProgress >= 1.0) {
            g2d.setColor(Color.RED);
        } else if (detectionProgress > 0.2) {
            g2d.setColor(Color.ORANGE);
        } else {
            g2d.setColor(new Color(0x20, 0xD0, 0x80)); // Green operating LED
        }
        g2d.fillRect(cx - 1, cy - 1, 3, 3);
    }

    @Override
    public String getInteractionPrompt() {
        if (disabled) {
            return "[OFFLINE] " + name + " Feed Looped";
        }
        return "[E] Loop CCTV Feed // " + name + " (MEDIUM)";
    }

    @Override
    public boolean canInteract() {
        return !disabled;
    }

    @Override
    public void onInteract() {
        LOGGER.info("Interacting with CCTV camera junction: " + name);
    }

    @Override
    public BoundingBox getInteractionTriggerZone() {
        return interactionZone;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public HackingDifficulty getDifficulty() {
        return HackingDifficulty.MEDIUM;
    }

    @Override
    public String getTargetName() {
        return name + " // CCTV JUNCTION";
    }

    public interface CameraStateListener {
        void onCameraDisabled(SecurityCamera camera);
    }

    private CameraStateListener stateListener = null;

    public void setCameraStateListener(CameraStateListener listener) {
        this.stateListener = listener;
    }

    @Override
    public void onHackSuccess() {
        this.disabled = true;
        this.hacked = true;
        this.detectionProgress = 0.0;
        this.playerInCone = false;
        this.hasClearLOS = false;
        LOGGER.info("Camera '" + name + "' video feed looped and disabled!");
        if (stateListener != null) {
            stateListener.onCameraDisabled(this);
        }
    }

    @Override
    public void onHackFailure() {
        LOGGER.info("Camera '" + name + "' bypass failed!");
    }

    @Override
    public boolean isHacked() {
        return hacked;
    }

    public void applyEmpDisable(double duration) {
        this.disabled = true;
        this.empDisableTimer = Math.max(empDisableTimer, duration);
        this.detectionProgress = 0.0;
        this.playerInCone = false;
        this.hasClearLOS = false;
        LOGGER.info("Camera '" + name + "' knocked offline by EMP for " + duration + "s!");
        if (stateListener != null) {
            stateListener.onCameraDisabled(this);
        }
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public boolean isDisabled() { return disabled; }
    public void setDisabled(boolean disabled) { this.disabled = disabled; }
    public void setHacked(boolean hacked) { this.hacked = hacked; }
    public double getCurrentAngle() { return currentAngle; }
    public void setCurrentAngle(double currentAngle) { this.currentAngle = currentAngle; }
    public double getEmpDisableTimer() { return empDisableTimer; }
    public void setEmpDisableTimer(double empDisableTimer) { this.empDisableTimer = empDisableTimer; }
    public double getDetectionProgress() { return detectionProgress; }
    public boolean isPlayerInCone() { return playerInCone; }
    public boolean hasClearLOS() { return hasClearLOS; }
}
