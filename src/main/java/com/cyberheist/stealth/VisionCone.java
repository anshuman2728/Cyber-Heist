package com.cyberheist.stealth;

/**
 * Geometric vision cone for guard and sensor optics.
 * Calculates field of view, distance falloff, and angular proximity.
 */
public final class VisionCone {

    private final double viewDistance;
    private final double fovDegrees;
    private final double peripheralRadius;

    public VisionCone(double viewDistance, double fovDegrees, double peripheralRadius) {
        this.viewDistance = viewDistance;
        this.fovDegrees = fovDegrees;
        this.peripheralRadius = peripheralRadius;
    }

    public static VisionCone createDefaultGuardVision() {
        return new VisionCone(280.0, 75.0, 36.0);
    }

    /**
     * Determines whether a target point falls inside this vision cone or immediate peripheral radius.
     */
    public boolean contains(double originX, double originY, double facingAngleRad, double targetX, double targetY) {
        double dist = Math.hypot(targetX - originX, targetY - originY);
        if (dist > viewDistance) {
            return false;
        }

        // Immediate close-proximity peripheral vision
        if (dist <= peripheralRadius) {
            return true;
        }

        double angleToTarget = Math.atan2(targetY - originY, targetX - originX);
        double angleDiff = normalizeAngle(angleToTarget - facingAngleRad);

        double halfFovRad = Math.toRadians(fovDegrees / 2.0);
        return Math.abs(angleDiff) <= halfFovRad;
    }

    /**
     * Computes a visibility factor from 0.0 (edge/far) to 1.0 (dead center/close).
     */
    public double getVisibilityWeight(double originX, double originY, double facingAngleRad, double targetX, double targetY) {
        if (!contains(originX, originY, facingAngleRad, targetX, targetY)) {
            return 0.0;
        }

        double dist = Math.hypot(targetX - originX, targetY - originY);
        // Distance falloff (closer = higher)
        double distFactor = Math.max(0.1, 1.0 - (dist / viewDistance));

        double angleToTarget = Math.atan2(targetY - originY, targetX - originX);
        double angleDiff = Math.abs(normalizeAngle(angleToTarget - facingAngleRad));
        double halfFovRad = Math.toRadians(fovDegrees / 2.0);

        // Angular falloff (center of vision = higher)
        double angleFactor = Math.max(0.2, 1.0 - (angleDiff / halfFovRad));

        return distFactor * angleFactor;
    }

    private static double normalizeAngle(double angle) {
        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }

    public double getViewDistance() { return viewDistance; }
    public double getFovDegrees() { return fovDegrees; }
    public double getPeripheralRadius() { return peripheralRadius; }
}
