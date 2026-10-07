package com.cyberheist.display;

import com.cyberheist.settings.SettingsManager;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;

/**
 * Procedural atmospheric lighting and post-processing renderer:
 * - Dynamic dark ambient overlay with tactical player visor/flashlight vision circle
 * - Red emergency pulsation when alarm is triggered
 * - Cinematic vignette edge darkening
 * - Retro CRT scanline rasterization
 */
public final class LightingRenderer {

    private double alertPulseTime = 0.0;

    public void update(double deltaTime, boolean alertActive) {
        if (alertActive) {
            alertPulseTime += deltaTime * 5.0;
        } else {
            alertPulseTime = 0.0;
        }
    }

    /**
     * Renders atmospheric ambient lighting in screen space over the game world.
     *
     * @param g2d           Graphics context (in screen space)
     * @param screenWidth   Screen width
     * @param screenHeight  Screen height
     * @param playerScreenX Player center X on screen
     * @param playerScreenY Player center Y on screen
     * @param alertActive   Whether security alarm is currently sounding
     */
    public void render(Graphics2D g2d, int screenWidth, int screenHeight,
                       double playerScreenX, double playerScreenY, boolean alertActive) {

        // 1. Visor / Tactical Flashlight Radial Cutout
        float radius = 175.0f;
        Point2D center = new Point2D.Double(playerScreenX, playerScreenY);
        float[] dist = {0.0f, 0.45f, 1.0f};

        // Dark navy atmospheric darkness
        int baseAlpha = 115;
        Color centerColor = new Color(0, 15, 30, 0);       // Clear vision in center
        Color midColor = new Color(4, 10, 20, (int) (baseAlpha * 0.4));
        Color outerColor = new Color(3, 8, 16, baseAlpha);

        if (playerScreenX >= -radius && playerScreenX <= screenWidth + radius &&
            playerScreenY >= -radius && playerScreenY <= screenHeight + radius) {

            RadialGradientPaint p = new RadialGradientPaint(center, radius, dist,
                    new Color[]{centerColor, midColor, outerColor},
                    MultipleGradientPaint.CycleMethod.NO_CYCLE);

            g2d.setPaint(p);
            g2d.fillRect(0, 0, screenWidth, screenHeight);
        } else {
            g2d.setColor(outerColor);
            g2d.fillRect(0, 0, screenWidth, screenHeight);
        }

        // 2. Alarm Emergency Pulsing Red Light
        if (alertActive) {
            float pulse = (float) ((Math.sin(alertPulseTime) + 1.0) / 2.0);
            int redAlpha = Math.round(25.0f + pulse * 45.0f);
            g2d.setColor(new Color(230, 20, 40, redAlpha));
            g2d.fillRect(0, 0, screenWidth, screenHeight);
        }

        // 3. Cinematic Screen Vignette
        renderVignette(g2d, screenWidth, screenHeight);

        // 4. CRT Scanlines
        if (SettingsManager.getInstance().isScanlinesEnabled()) {
            renderScanlines(g2d, screenWidth, screenHeight);
        }
    }

    private void renderVignette(Graphics2D g2d, int width, int height) {
        Point2D center = new Point2D.Double(width / 2.0, height / 2.0);
        float radius = (float) Math.hypot(width / 2.0, height / 2.0);
        float[] dist = {0.0f, 0.70f, 1.0f};
        Color[] colors = {
                new Color(0, 0, 0, 0),
                new Color(0, 0, 0, 40),
                new Color(0, 0, 0, 130)
        };

        RadialGradientPaint vignette = new RadialGradientPaint(center, radius, dist, colors);
        g2d.setPaint(vignette);
        g2d.fillRect(0, 0, width, height);
    }

    private void renderScanlines(Graphics2D g2d, int width, int height) {
        g2d.setColor(new Color(0, 0, 0, 24));
        for (int y = 0; y < height; y += 4) {
            g2d.fillRect(0, y, width, 1);
        }
    }
}
