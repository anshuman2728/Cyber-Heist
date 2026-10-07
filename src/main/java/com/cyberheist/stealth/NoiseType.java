package com.cyberheist.stealth;

/**
 * Noise emission classifications and physical propagation radii in pixels.
 */
public enum NoiseType {
    NONE(0, 0.0),
    WALKING(120, 0.4),      // Low noise
    DOOR_OPEN(150, 0.5),    // Low noise
    HACKING(90, 0.4),       // Low noise
    RUNNING(280, 0.5),      // Medium noise
    EMP(360, 0.7),          // Medium noise
    WEAPON(620, 0.8);       // High noise

    private final double radius;
    private final double lifetime; // Duration the noise ripple expands and lingers (seconds)

    NoiseType(double radius, double lifetime) {
        this.radius = radius;
        this.lifetime = lifetime;
    }

    public double getRadius() {
        return radius;
    }

    public double getLifetime() {
        return lifetime;
    }
}
