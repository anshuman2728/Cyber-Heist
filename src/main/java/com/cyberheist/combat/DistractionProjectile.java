package com.cyberheist.combat;

/**
 * Animated physical decoy projectile thrown by the player to create an acoustic distraction.
 */
public final class DistractionProjectile {

    private final double startX;
    private final double startY;
    private final double targetX;
    private final double targetY;

    private double currentX;
    private double currentY;
    private double elapsed = 0.0;
    private final double duration = 0.32; // Seconds in air
    private boolean landed = false;

    public DistractionProjectile(double startX, double startY, double targetX, double targetY) {
        this.startX = startX;
        this.startY = startY;
        this.targetX = targetX;
        this.targetY = targetY;
        this.currentX = startX;
        this.currentY = startY;
    }

    public void update(double deltaTime) {
        if (landed) return;

        elapsed += deltaTime;
        double progress = Math.min(1.0, elapsed / duration);

        this.currentX = startX + ((targetX - startX) * progress);
        this.currentY = startY + ((targetY - startY) * progress);

        if (progress >= 1.0) {
            this.landed = true;
        }
    }

    public boolean hasLanded() { return landed; }
    public double getCurrentX() { return currentX; }
    public double getCurrentY() { return currentY; }
    public double getTargetX() { return targetX; }
    public double getTargetY() { return targetY; }
}
