package com.cyberheist.combat;

import com.cyberheist.core.GameConfig;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Visual effects for combat actions (Shock arcs, EMP electromagnetic rings, decoy pings).
 */
public final class CombatEffect {

    public enum EffectType {
        SHOCK_ARC,
        EMP_WAVE,
        DECOY_PING
    }

    private final EffectType type;
    private final double x;
    private final double y;
    private double targetX;
    private double targetY;

    private double radius = 0.0;
    private final double maxRadius;
    private double lifetime = 0.0;
    private final double maxLifetime;

    public CombatEffect(EffectType type, double x, double y, double targetX, double targetY, double maxRadius, double maxLifetime) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.targetX = targetX;
        this.targetY = targetY;
        this.maxRadius = maxRadius;
        this.maxLifetime = maxLifetime;
    }

    public static CombatEffect createShockArc(double originX, double originY, double targetX, double targetY) {
        return new CombatEffect(EffectType.SHOCK_ARC, originX, originY, targetX, targetY, 0.0, 0.22);
    }

    public static CombatEffect createEmpWave(double centerX, double centerY, double maxRadius) {
        return new CombatEffect(EffectType.EMP_WAVE, centerX, centerY, 0.0, 0.0, maxRadius, 0.45);
    }

    public static CombatEffect createDecoyPing(double x, double y) {
        return new CombatEffect(EffectType.DECOY_PING, x, y, 0.0, 0.0, 48.0, 0.50);
    }

    public void update(double deltaTime) {
        lifetime += deltaTime;
        if (maxLifetime > 0.0) {
            double progress = Math.min(1.0, lifetime / maxLifetime);
            radius = maxRadius * progress;
        }
    }

    public boolean isExpired() {
        return lifetime >= maxLifetime;
    }

    public void render(Graphics2D g2d) {
        double alpha = Math.max(0.0, 1.0 - (lifetime / maxLifetime));
        int alpha255 = (int) Math.round(alpha * 255.0);

        if (type == EffectType.SHOCK_ARC) {
            g2d.setColor(new Color(0, 240, 255, alpha255));
            g2d.setStroke(new BasicStroke(3.0f));
            g2d.drawLine((int) Math.round(x), (int) Math.round(y), (int) Math.round(targetX), (int) Math.round(targetY));

            // Jagged fork line
            double midX = (x + targetX) / 2.0 + (Math.random() * 16.0 - 8.0);
            double midY = (y + targetY) / 2.0 + (Math.random() * 16.0 - 8.0);
            g2d.setColor(new Color(255, 0, 200, alpha255));
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawLine((int) Math.round(x), (int) Math.round(y), (int) Math.round(midX), (int) Math.round(midY));
            g2d.drawLine((int) Math.round(midX), (int) Math.round(midY), (int) Math.round(targetX), (int) Math.round(targetY));
            g2d.setStroke(new BasicStroke(1.0f));
        } else if (type == EffectType.EMP_WAVE) {
            int cx = (int) Math.round(x);
            int cy = (int) Math.round(y);
            int r = (int) Math.round(radius);

            g2d.setColor(new Color(0, 240, 255, Math.min(255, (int) Math.round(alpha * 180.0))));
            g2d.setStroke(new BasicStroke(3.5f));
            g2d.drawOval(cx - r, cy - r, r * 2, r * 2);

            g2d.setColor(new Color(180, 0, 255, Math.min(255, (int) Math.round(alpha * 100.0))));
            int rInner = Math.max(2, r - 12);
            g2d.drawOval(cx - rInner, cy - rInner, rInner * 2, rInner * 2);
            g2d.setStroke(new BasicStroke(1.0f));
        } else if (type == EffectType.DECOY_PING) {
            int cx = (int) Math.round(x);
            int cy = (int) Math.round(y);
            int r = (int) Math.round(radius);

            g2d.setColor(new Color(255, 200, 0, alpha255));
            g2d.setStroke(new BasicStroke(2.0f));
            g2d.drawOval(cx - r, cy - r, r * 2, r * 2);
            g2d.fillOval(cx - 3, cy - 3, 6, 6);
            g2d.setStroke(new BasicStroke(1.0f));
        }
    }
}
