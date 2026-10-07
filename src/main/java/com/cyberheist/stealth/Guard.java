package com.cyberheist.stealth;

import com.cyberheist.collision.BoundingBox;
import com.cyberheist.collision.Collidable;
import com.cyberheist.core.GameConfig;
import com.cyberheist.entity.FacingDirection;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.Point2D;

/**
 * Security Guard observer entity with vision cone, directional hearing,
 * line-of-sight tracking, and progressive detection.
 * Designed so guards do not have supernatural knowledge of player coordinates.
 */
public final class Guard implements Collidable {

    private final String name;
    private double x;
    private double y;
    private final double width = 28.0;
    private final double height = 28.0;

    private double facingAngle; // Radians
    private final double originalAngle;
    private final VisionCone visionCone;
    private final DetectionMeter detectionMeter;
    private final double hearingRadius;

    // Sensory State
    private boolean playerInVision = false;
    private boolean hasClearLOS = false;
    private Point2D.Double lastKnownPlayerPos = null;
    private Point2D.Double currentLOSObstacleHit = null;

    // Suspicion turn speed
    private Double targetTurnAngle = null;

    // Combat / Incapacitation state
    private boolean unconscious = false;
    private double stunTimer = 0.0;

    public Guard(String name, double x, double y, double baseAngleDegrees) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.facingAngle = Math.toRadians(baseAngleDegrees);
        this.originalAngle = this.facingAngle;
        this.visionCone = VisionCone.createDefaultGuardVision();
        this.detectionMeter = DetectionMeter.createDefault();
        this.hearingRadius = 160.0;
    }

    public void knockOut() {
        this.unconscious = true;
        this.stunTimer = 0.0;
        this.playerInVision = false;
        this.hasClearLOS = false;
        this.lastKnownPlayerPos = null;
        this.targetTurnAngle = null;
        this.detectionMeter.update(100.0, false, 0.0);
    }

    public void applyStun(double duration) {
        if (unconscious) return;
        this.stunTimer = Math.max(this.stunTimer, duration);
        this.playerInVision = false;
        this.hasClearLOS = false;
        this.targetTurnAngle = null;
    }

    public boolean isUnconscious() { return unconscious; }
    public void setUnconscious(boolean unconscious) { this.unconscious = unconscious; }
    public boolean isStunned() { return stunTimer > 0.0; }
    public boolean isIncapacitated() { return unconscious || isStunned(); }
    public double getStunTimer() { return stunTimer; }
    public void setStunTimer(double stunTimer) { this.stunTimer = stunTimer; }

    /**
     * Updates sensory processing, hearing checks, and detection meter.
     */
    public void update(double deltaTime, NoiseEvent audibleNoise) {
        if (unconscious) {
            return;
        }
        if (stunTimer > 0.0) {
            stunTimer -= deltaTime;
            if (stunTimer < 0.0) stunTimer = 0.0;
            return;
        }

        // 1. Process auditory stimuli
        if (audibleNoise != null) {
            double angleToSound = Math.atan2(audibleNoise.getY() - y, audibleNoise.getX() - x);
            // Noise raises suspicion if guard is not already alerted
            if (detectionMeter.getState() != DetectionState.DETECTED) {
                targetTurnAngle = angleToSound;
                // Add instantaneous suspicion bump based on noise intensity
                detectionMeter.addInstantDetection(audibleNoise.getType() == NoiseType.WEAPON ? 45.0 : 15.0);
            }
        }

        // 2. Smoothly rotate toward sound or investigation point if suspicious
        if (targetTurnAngle != null) {
            double diff = normalizeAngle(targetTurnAngle - facingAngle);
            double turnSpeed = 3.5 * deltaTime;
            if (Math.abs(diff) <= turnSpeed) {
                facingAngle = targetTurnAngle;
                targetTurnAngle = null;
            } else {
                facingAngle += Math.signum(diff) * turnSpeed;
            }
        } else if (detectionMeter.getState() == DetectionState.UNAWARE && facingAngle != originalAngle) {
            // Gradually return to base patrol facing
            double diff = normalizeAngle(originalAngle - facingAngle);
            double turnSpeed = 1.5 * deltaTime;
            if (Math.abs(diff) <= turnSpeed) {
                facingAngle = originalAngle;
            } else {
                facingAngle += Math.signum(diff) * turnSpeed;
            }
        }
    }

    public void evaluatePlayerVisibility(double playerX, double playerY, double playerSpeed,
                                         boolean inCover, double ambientLighting, boolean clearLOS,
                                         Point2D.Double obstacleHit, double deltaTime) {

        if (isIncapacitated()) {
            this.hasClearLOS = false;
            this.playerInVision = false;
            return;
        }

        this.hasClearLOS = clearLOS;
        this.currentLOSObstacleHit = obstacleHit;

        // Check if player center is within vision cone geometry
        this.playerInVision = visionCone.contains(x + 14.0, y + 14.0, facingAngle, playerX + 14.0, playerY + 14.0);

        boolean effectivelySeen = playerInVision && clearLOS;

        if (effectivelySeen) {
            // 1. Angular & Distance weight (0.0 to 1.0)
            double spatialWeight = visionCone.getVisibilityWeight(x + 14.0, y + 14.0, facingAngle, playerX + 14.0, playerY + 14.0);

            // 2. Movement speed influence (running = 1.6x, walking = 1.0x, stationary = 0.5x)
            double movementFactor = (playerSpeed > 250.0) ? 1.6 : (playerSpeed > 20.0 ? 1.0 : 0.5);

            // 3. Cover reduction (standing behind cover cuts visibility rate by 65%)
            double coverFactor = inCover ? 0.35 : 1.0;

            // 4. Lighting multiplier (dark room = slower build)
            double lightFactor = Math.max(0.3, ambientLighting);

            double combinedRate = spatialWeight * movementFactor * coverFactor * lightFactor;

            detectionMeter.update(deltaTime, true, combinedRate);

            // Face the suspicious sighting directly
            this.targetTurnAngle = Math.atan2(playerY - y, playerX - x);

            // Once detected or highly suspicious, track the observed position
            if (detectionMeter.getState() != DetectionState.UNAWARE) {
                this.lastKnownPlayerPos = new Point2D.Double(playerX, playerY);
            }
        } else {
            // Player is outside vision or behind full wall cover -> decay detection
            detectionMeter.update(deltaTime, false, 0.0);

            // Clear last known position once fully unaware
            if (detectionMeter.getState() == DetectionState.UNAWARE) {
                this.lastKnownPlayerPos = null;
            }
        }
    }

    public void render(Graphics2D g2d) {
        int gx = (int) Math.round(x);
        int gy = (int) Math.round(y);
        int gw = (int) Math.round(width);
        int gh = (int) Math.round(height);
        int cx = gx + (gw / 2);
        int cy = gy + (gh / 2);

        if (unconscious) {
            // Render slumped unconscious body
            g2d.setColor(new Color(0, 0, 0, 70));
            g2d.fillOval(gx - 4, gy + gh - 4, gw + 8, 12);

            g2d.setColor(new Color(0x1A, 0x22, 0x2E));
            g2d.fillRoundRect(gx - 2, gy + 4, gw + 4, gh - 8, 6, 6);

            g2d.setColor(new Color(0x3B, 0x4D, 0x66));
            g2d.drawRoundRect(gx - 2, gy + 4, gw + 4, gh - 8, 6, 6);

            g2d.setFont(GameConfig.FONT_CODE);
            g2d.setColor(new Color(0x70, 0x85, 0x9E));
            g2d.drawString("[KO // UNCONSCIOUS]", cx - 52, gy - 6);
            return;
        }

        // Guard shadow
        g2d.setColor(new Color(0, 0, 0, 80));
        g2d.fillOval(gx - 2, gy + gh - 6, gw + 4, 10);

        // Guard uniform hull
        g2d.setColor(new Color(0x28, 0x36, 0x48)); // Corporate security navy
        g2d.fillRoundRect(gx, gy, gw, gh, 8, 8);

        // Security vest
        g2d.setColor(new Color(0x3B, 0x4D, 0x66));
        g2d.drawRoundRect(gx, gy, gw, gh, 8, 8);

        // Tactical shoulder flashings
        g2d.setColor(new Color(0xF0, 0xAD, 0x4E));
        g2d.fillRect(gx + 2, gy + 4, 4, 6);
        g2d.fillRect(gx + gw - 6, gy + 4, 4, 6);

        // Facing visor / helmet peak
        int hx = cx + (int) Math.round(Math.cos(facingAngle) * 13.0);
        int hy = cy + (int) Math.round(Math.sin(facingAngle) * 13.0);

        g2d.setColor(Color.WHITE);
        g2d.drawLine(cx, cy, hx, hy);
        g2d.fillOval(hx - 2, hy - 2, 5, 5);

        // Stunned electric spark distortion
        if (isStunned()) {
            g2d.setColor(GameConfig.COLOR_MAGENTA);
            g2d.drawOval(gx - 4, gy - 4, gw + 8, gh + 8);
            g2d.setColor(GameConfig.COLOR_CYAN);
            g2d.drawLine(gx, gy + 4, cx, cy - 4);
            g2d.drawLine(cx, cy - 4, gx + gw, gy + gh - 2);
        }

        // Overhead status alert indicator & mini-bar
        renderOverheadStatus(g2d, cx, gy - 12);
    }

    private void renderOverheadStatus(Graphics2D g2d, int cx, int topY) {
        if (isStunned()) {
            g2d.setFont(GameConfig.FONT_CODE);
            g2d.setColor(GameConfig.COLOR_MAGENTA);
            g2d.drawString(String.format("STUNNED (%.1fs)", stunTimer), cx - 40, topY - 2);
            return;
        }

        DetectionState state = detectionMeter.getState();
        if (state == DetectionState.UNAWARE && detectionMeter.getPercentage() <= 0.0) {
            return;
        }

        // Mini detection bar
        int barW = 32;
        int barH = 4;
        int barX = cx - (barW / 2);
        int barY = topY;

        g2d.setColor(new Color(15, 20, 28, 200));
        g2d.fillRect(barX, barY, barW, barH);

        Color fill = switch (state) {
            case DETECTED -> Color.RED;
            case SUSPICIOUS -> new Color(255, 180, 0);
            default -> GameConfig.COLOR_GREEN;
        };

        int fillW = (int) Math.round((barW * detectionMeter.getPercentage()) / 100.0);
        g2d.setColor(fill);
        g2d.fillRect(barX, barY, fillW, barH);
        g2d.setColor(Color.DARK_GRAY);
        g2d.drawRect(barX, barY, barW, barH);

        // Alert icon
        g2d.setFont(GameConfig.FONT_CODE);
        if (state == DetectionState.DETECTED) {
            g2d.setColor(Color.RED);
            g2d.drawString("!", cx - 2, topY - 4);
        } else if (state == DetectionState.SUSPICIOUS) {
            g2d.setColor(new Color(255, 180, 0));
            g2d.drawString("?", cx - 3, topY - 4);
        }
    }

    /**
     * Builds the vision cone polygon for rendering or frustum testing.
     */
    public Polygon getVisionPolygon() {
        if (isIncapacitated()) {
            return new Polygon();
        }
        int cx = (int) Math.round(x + 14.0);
        int cy = (int) Math.round(y + 14.0);
        double dist = visionCone.getViewDistance();
        double halfFov = Math.toRadians(visionCone.getFovDegrees() / 2.0);

        int arcSteps = 12;
        int[] px = new int[arcSteps + 2];
        int[] py = new int[arcSteps + 2];

        px[0] = cx;
        py[0] = cy;

        for (int i = 0; i <= arcSteps; i++) {
            double angle = (facingAngle - halfFov) + (2 * halfFov * (i / (double) arcSteps));
            px[i + 1] = cx + (int) Math.round(Math.cos(angle) * dist);
            py[i + 1] = cy + (int) Math.round(Math.sin(angle) * dist);
        }

        return new Polygon(px, py, px.length);
    }

    private static double normalizeAngle(double angle) {
        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }

    @Override
    public BoundingBox getBoundingBox() {
        return new BoundingBox(x, y, width, height);
    }

    public String getName() { return name; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getFacingAngle() { return facingAngle; }
    public VisionCone getVisionCone() { return visionCone; }
    public DetectionMeter getDetectionMeter() { return detectionMeter; }
    public double getHearingRadius() { return hearingRadius; }
    public boolean isPlayerInVision() { return playerInVision; }
    public boolean hasClearLOS() { return hasClearLOS; }
    public Point2D.Double getLastKnownPlayerPos() { return lastKnownPlayerPos; }
    public Point2D.Double getCurrentLOSObstacleHit() { return currentLOSObstacleHit; }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void setFacingAngle(double facingAngle) {
        this.facingAngle = facingAngle;
    }

    private com.cyberheist.ai.GuardAI ai = null;

    public com.cyberheist.ai.GuardAI getAI() { return ai; }
    public void setAI(com.cyberheist.ai.GuardAI ai) { this.ai = ai; }
}
