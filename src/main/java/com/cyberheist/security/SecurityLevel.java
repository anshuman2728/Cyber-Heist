package com.cyberheist.security;

import java.awt.Color;

/**
 * Facility-wide security readiness and threat level.
 * Materially changes guard awareness, camera sweep parameters, door lockouts, and mission routing.
 */
public enum SecurityLevel {
    NORMAL("NORMAL", 0, new Color(0x20, 0xD0, 0x90), 1.0, 1.0, 110.0, 0.8),
    SUSPICIOUS("SUSPICIOUS", 1, new Color(0xE0, 0xC0, 0x20), 1.15, 1.3, 125.0, 1.2),
    ALERT("ALERT", 2, new Color(0xFF, 0x70, 0x20), 1.3, 1.8, 150.0, 1.6),
    LOCKDOWN("LOCKDOWN", 3, new Color(0xFF, 0x20, 0x30), 1.45, 2.4, 180.0, 2.2);

    private final String label;
    private final int severity;
    private final Color badgeColor;
    private final double visionRangeMultiplier;
    private final double cameraDetectionSpeedMultiplier;
    private final double guardPatrolSpeed;
    private final double cameraSweepSpeed;

    SecurityLevel(String label, int severity, Color badgeColor,
                  double visionRangeMultiplier, double cameraDetectionSpeedMultiplier,
                  double guardPatrolSpeed, double cameraSweepSpeed) {
        this.label = label;
        this.severity = severity;
        this.badgeColor = badgeColor;
        this.visionRangeMultiplier = visionRangeMultiplier;
        this.cameraDetectionSpeedMultiplier = cameraDetectionSpeedMultiplier;
        this.guardPatrolSpeed = guardPatrolSpeed;
        this.cameraSweepSpeed = cameraSweepSpeed;
    }

    public String getLabel() { return label; }
    public int getSeverity() { return severity; }
    public Color getBadgeColor() { return badgeColor; }
    public double getVisionRangeMultiplier() { return visionRangeMultiplier; }
    public double getCameraDetectionSpeedMultiplier() { return cameraDetectionSpeedMultiplier; }
    public double getGuardPatrolSpeed() { return guardPatrolSpeed; }
    public double getCameraSweepSpeed() { return cameraSweepSpeed; }

    public boolean isAtLeast(SecurityLevel other) {
        return this.severity >= other.severity;
    }
}
