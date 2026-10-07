package com.cyberheist.particle;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;

/**
 * Individual visual particle instance with physics, lifetime decay,
 * alpha fading, and optional character rendering (e.g. data bit glyphs).
 */
public final class Particle {

    private static final Font GLYPH_FONT = new Font("Monospaced", Font.BOLD, 10);

    private double x;
    private double y;
    private double vx;
    private double vy;
    private double life;
    private final double maxLife;
    private double size;
    private final Color color;
    private final boolean shrink;
    private final String glyph;

    public Particle(double x, double y, double vx, double vy, double maxLife,
                    double size, Color color, boolean shrink, String glyph) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.life = maxLife;
        this.maxLife = maxLife;
        this.size = size;
        this.color = color;
        this.shrink = shrink;
        this.glyph = glyph;
    }

    public void update(double deltaTime) {
        x += vx * deltaTime;
        y += vy * deltaTime;
        // Natural air resistance drag
        vx *= Math.pow(0.85, deltaTime * 60.0);
        vy *= Math.pow(0.85, deltaTime * 60.0);

        life -= deltaTime;
    }

    public void render(Graphics2D g2d) {
        if (!isAlive()) return;

        float progress = (float) Math.max(0.0, Math.min(1.0, life / maxLife));
        float alpha = progress;

        Composite originalComposite = g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        double renderSize = shrink ? size * progress : size;
        if (renderSize < 1.0) renderSize = 1.0;

        g2d.setColor(color);
        if (glyph != null) {
            g2d.setFont(GLYPH_FONT);
            g2d.drawString(glyph, (float) (x - renderSize / 2), (float) (y + renderSize / 2));
        } else {
            g2d.fillOval((int) (x - renderSize / 2), (int) (y - renderSize / 2),
                    (int) Math.round(renderSize), (int) Math.round(renderSize));
        }

        g2d.setComposite(originalComposite);
    }

    public boolean isAlive() {
        return life > 0.0;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getLife() { return life; }
    public double getMaxLife() { return maxLife; }
}
