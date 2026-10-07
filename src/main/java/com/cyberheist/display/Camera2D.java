package com.cyberheist.display;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

/**
 * 2D Viewport Camera supporting smooth target tracking, lerping,
 * and level boundary clamping.
 */
public final class Camera2D {

    private double x;
    private double y;
    private final int viewportWidth;
    private final int viewportHeight;

    private double worldWidth;
    private double worldHeight;

    private AffineTransform originalTransform;

    // Trauma-based Screen Shake (Trauma^2 system)
    private double trauma = 0.0;
    private double shakeOffsetX = 0.0;
    private double shakeOffsetY = 0.0;
    private static final double MAX_SHAKE_OFFSET = 14.0;
    private static final double TRAUMA_DECAY = 1.8; // Per second

    public Camera2D(int viewportWidth, int viewportHeight, double worldWidth, double worldHeight) {
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.x = 0;
        this.y = 0;
    }

    /**
     * Adds shake trauma (0.0 to 1.0). Trauma is clamped to 1.0.
     */
    public void addTrauma(double amount) {
        this.trauma = Math.min(1.0, this.trauma + Math.max(0.0, amount));
    }

    /**
     * Smoothly interpolates camera position toward target coordinates (e.g. player center).
     *
     * @param targetX    Target world X coordinate
     * @param targetY    Target world Y coordinate
     * @param deltaTime  Elapsed frame time
     * @param lerpSpeed  Smoothing factor (e.g. 8.0)
     */
    public void update(double targetX, double targetY, double deltaTime, double lerpSpeed) {
        // Calculate shake from trauma
        if (trauma > 0.0) {
            double shakePower = trauma * trauma; // Quadratic trauma falloff
            com.cyberheist.settings.SettingsManager settings = com.cyberheist.settings.SettingsManager.getInstance();
            if (settings.isScreenShakeEnabled()) {
                double intensity = settings.getScreenShakeIntensity();
                this.shakeOffsetX = (Math.random() * 2.0 - 1.0) * MAX_SHAKE_OFFSET * shakePower * intensity;
                this.shakeOffsetY = (Math.random() * 2.0 - 1.0) * MAX_SHAKE_OFFSET * shakePower * intensity;
            } else {
                this.shakeOffsetX = 0.0;
                this.shakeOffsetY = 0.0;
            }
            trauma = Math.max(0.0, trauma - TRAUMA_DECAY * deltaTime);
        } else {
            this.shakeOffsetX = 0.0;
            this.shakeOffsetY = 0.0;
        }

        double desiredX = targetX - (viewportWidth / 2.0);
        double desiredY = targetY - (viewportHeight / 2.0);

        // Clamp desired position to world bounds
        double maxX = Math.max(0, worldWidth - viewportWidth);
        double maxY = Math.max(0, worldHeight - viewportHeight);

        desiredX = Math.max(0, Math.min(maxX, desiredX));
        desiredY = Math.max(0, Math.min(maxY, desiredY));

        // Smooth exponential dampening / lerp
        double factor = Math.min(1.0, lerpSpeed * deltaTime);
        this.x += (desiredX - this.x) * factor;
        this.y += (desiredY - this.y) * factor;
    }

    /**
     * Immediately snaps the camera to target coordinates without smoothing.
     */
    public void snapTo(double targetX, double targetY) {
        double desiredX = targetX - (viewportWidth / 2.0);
        double desiredY = targetY - (viewportHeight / 2.0);

        double maxX = Math.max(0, worldWidth - viewportWidth);
        double maxY = Math.max(0, worldHeight - viewportHeight);

        this.x = Math.max(0, Math.min(maxX, desiredX));
        this.y = Math.max(0, Math.min(maxY, desiredY));
    }

    /**
     * Applies camera translation (with trauma shake) to the Graphics2D context for world rendering.
     */
    public void applyTransform(Graphics2D g2d) {
        this.originalTransform = g2d.getTransform();
        g2d.translate(-Math.round(x + shakeOffsetX), -Math.round(y + shakeOffsetY));
    }

    /**
     * Restores the original Graphics2D transform for screen-space UI / HUD rendering.
     */
    public void resetTransform(Graphics2D g2d) {
        if (originalTransform != null) {
            g2d.setTransform(originalTransform);
        }
    }

    public double toWorldX(int screenX) {
        return screenX + x + shakeOffsetX;
    }

    public double toWorldY(int screenY) {
        return screenY + y + shakeOffsetY;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getShakeOffsetX() { return shakeOffsetX; }
    public double getShakeOffsetY() { return shakeOffsetY; }
    public double getTrauma() { return trauma; }
    public int getViewportWidth() { return viewportWidth; }
    public int getViewportHeight() { return viewportHeight; }

    public void setWorldBounds(double worldWidth, double worldHeight) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
    }
}
