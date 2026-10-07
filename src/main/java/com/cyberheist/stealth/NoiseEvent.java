package com.cyberheist.stealth;

/**
 * An acoustic impulse propagating through the game world.
 */
public final class NoiseEvent {

    private final double x;
    private final double y;
    private final NoiseType type;
    private double age = 0.0;

    public NoiseEvent(double x, double y, NoiseType type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public void update(double deltaTime) {
        this.age += deltaTime;
    }

    public boolean isExpired() {
        return age >= type.getLifetime();
    }

    /**
     * Expands rapidly and decelerates as sound wavefront travels outwards.
     */
    public double getCurrentRadius() {
        double progress = Math.min(1.0, age / type.getLifetime());
        // Quadratic ease-out for sound wave expansion
        double ease = 1.0 - Math.pow(1.0 - progress, 2);
        return type.getRadius() * ease;
    }

    public double getIntensity() {
        double progress = Math.min(1.0, age / type.getLifetime());
        return Math.max(0.0, 1.0 - progress);
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public NoiseType getType() { return type; }
    public double getAge() { return age; }
}
