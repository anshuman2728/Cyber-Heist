package com.cyberheist.world;

import com.cyberheist.collision.BoundingBox;
import com.cyberheist.collision.Collidable;
import com.cyberheist.core.GameConfig;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Solid architectural props providing cover and physical collision.
 * Features realistic corporate detailing (desks, server racks, turnstiles, partitions).
 */
public final class CoverObject implements Collidable {

    public enum CoverType {
        OFFICE_DESK,
        RECEPTION_COUNTER,
        SERVER_RACK,
        CUBICLE_PARTITION,
        SECURITY_TURNSTILE,
        LOBBY_COUCH,
        FILING_CABINET,
        GLASS_WINDOW
    }

    private final String name;
    private final CoverType type;
    private final BoundingBox bounds;
    private double blinkTimer = 0.0;

    public CoverObject(String name, CoverType type, double x, double y, double width, double height) {
        this.name = name;
        this.type = type;
        this.bounds = new BoundingBox(x, y, width, height);
    }

    public void update(double deltaTime) {
        if (type == CoverType.SERVER_RACK) {
            blinkTimer = (blinkTimer + deltaTime) % 2.0;
        }
    }

    @Override
    public BoundingBox getBoundingBox() {
        return bounds;
    }

    public void render(Graphics2D g2d) {
        int x = (int) Math.round(bounds.getX());
        int y = (int) Math.round(bounds.getY());
        int w = (int) Math.round(bounds.getWidth());
        int h = (int) Math.round(bounds.getHeight());

        switch (type) {
            case RECEPTION_COUNTER -> renderReceptionCounter(g2d, x, y, w, h);
            case OFFICE_DESK -> renderOfficeDesk(g2d, x, y, w, h);
            case SERVER_RACK -> renderServerRack(g2d, x, y, w, h);
            case SECURITY_TURNSTILE -> renderTurnstile(g2d, x, y, w, h);
            case CUBICLE_PARTITION -> renderCubiclePartition(g2d, x, y, w, h);
            case LOBBY_COUCH -> renderCouch(g2d, x, y, w, h);
            case FILING_CABINET -> renderFilingCabinet(g2d, x, y, w, h);
            case GLASS_WINDOW -> renderGlassWindow(g2d, x, y, w, h);
        }
    }

    private void renderReceptionCounter(Graphics2D g2d, int x, int y, int w, int h) {
        // Dark mahogany / brushed aluminum corporate counter
        g2d.setColor(new Color(0x3B, 0x43, 0x52));
        g2d.fillRoundRect(x, y, w, h, 6, 6);
        g2d.setColor(new Color(0x57, 0x62, 0x75));
        g2d.drawRoundRect(x, y, w, h, 6, 6);

        // Countertop ledge
        g2d.setColor(new Color(0x4C, 0x58, 0x6B));
        g2d.fillRect(x + 4, y + 4, w - 8, 8);

        // Reception terminal monitor on desk
        g2d.setColor(new Color(0x18, 0x22, 0x30));
        g2d.fillRect(x + (w / 2) - 10, y + 16, 20, 10);
        g2d.setColor(GameConfig.COLOR_CYAN);
        g2d.drawRect(x + (w / 2) - 10, y + 16, 20, 10);
    }

    private void renderOfficeDesk(Graphics2D g2d, int x, int y, int w, int h) {
        // Modern matte desk surface
        g2d.setColor(new Color(0x2E, 0x38, 0x46));
        g2d.fillRect(x, y, w, h);
        g2d.setColor(new Color(0x47, 0x54, 0x66));
        g2d.drawRect(x, y, w, h);

        // Dual monitor setup
        int monW = Math.min(16, (w - 12) / 2);
        g2d.setColor(new Color(0x11, 0x18, 0x24));
        g2d.fillRect(x + 6, y + 4, monW, 8);
        g2d.fillRect(x + 6 + monW + 4, y + 4, monW, 8);

        g2d.setColor(new Color(0x52, 0x79, 0xA4));
        g2d.drawRect(x + 6, y + 4, monW, 8);
        g2d.drawRect(x + 6 + monW + 4, y + 4, monW, 8);

        // Keyboard & mousepad
        g2d.setColor(new Color(0x20, 0x28, 0x34));
        g2d.fillRect(x + 10, y + 16, w - 20, 6);
    }

    private void renderServerRack(Graphics2D g2d, int x, int y, int w, int h) {
        // Deep matte black server cabinet with ventilation slats
        g2d.setColor(new Color(0x11, 0x16, 0x1E));
        g2d.fillRect(x, y, w, h);
        g2d.setColor(new Color(0x25, 0x33, 0x44));
        g2d.drawRect(x, y, w, h);

        // Horizontal server blade slats
        g2d.setColor(new Color(0x1B, 0x24, 0x32));
        for (int ly = y + 6; ly < y + h - 6; ly += 8) {
            g2d.drawLine(x + 4, ly, x + w - 4, ly);
        }

        // Blinking status LEDs (green, amber, cyan)
        int blinkState = (int) (blinkTimer * 4);
        Color led1 = (blinkState % 2 == 0) ? GameConfig.COLOR_GREEN : new Color(0, 100, 40);
        Color led2 = (blinkState % 3 == 0) ? new Color(255, 170, 0) : new Color(120, 80, 0);
        Color led3 = ((blinkState + 1) % 4 == 0) ? GameConfig.COLOR_CYAN : new Color(0, 80, 100);

        g2d.setColor(led1);
        g2d.fillRect(x + 4, y + 4, 2, 2);
        g2d.setColor(led2);
        g2d.fillRect(x + 8, y + 4, 2, 2);
        g2d.setColor(led3);
        g2d.fillRect(x + 12, y + 4, 2, 2);
    }

    private void renderTurnstile(Graphics2D g2d, int x, int y, int w, int h) {
        // Stainless steel security barrier pedestal
        g2d.setColor(new Color(0x40, 0x4B, 0x5C));
        g2d.fillRoundRect(x, y, w, h, 4, 4);
        g2d.setColor(new Color(0x66, 0x76, 0x8E));
        g2d.drawRoundRect(x, y, w, h, 4, 4);

        // RFID scanner indicator
        g2d.setColor(GameConfig.COLOR_GREEN);
        g2d.fillRect(x + (w / 2) - 3, y + (h / 2) - 1, 6, 2);
    }

    private void renderCubiclePartition(Graphics2D g2d, int x, int y, int w, int h) {
        // Acoustic fabric divider screen
        g2d.setColor(new Color(0x23, 0x30, 0x40));
        g2d.fillRect(x, y, w, h);
        g2d.setColor(new Color(0x3B, 0x4C, 0x62));
        g2d.drawRect(x, y, w, h);
    }

    private void renderCouch(Graphics2D g2d, int x, int y, int w, int h) {
        // Corporate leather couch
        g2d.setColor(new Color(0x2C, 0x34, 0x42));
        g2d.fillRoundRect(x, y, w, h, 6, 6);
        g2d.setColor(new Color(0x44, 0x51, 0x64));
        g2d.drawRoundRect(x, y, w, h, 6, 6);
        // Cushion divisions
        if (w > h) {
            g2d.drawLine(x + (w / 2), y + 4, x + (w / 2), y + h - 4);
        } else {
            g2d.drawLine(x + 4, y + (h / 2), x + w - 4, y + (h / 2));
        }
    }

    private void renderFilingCabinet(Graphics2D g2d, int x, int y, int w, int h) {
        g2d.setColor(new Color(0x24, 0x2C, 0x38));
        g2d.fillRect(x, y, w, h);
        g2d.setColor(new Color(0x40, 0x4C, 0x5E));
        g2d.drawRect(x, y, w, h);
        // Draw drawer handles
        for (int dy = y + 8; dy < y + h - 6; dy += 12) {
            g2d.setColor(new Color(0x70, 0x80, 0x94));
            g2d.fillRect(x + (w / 2) - 4, dy, 8, 2);
        }
    }

    private void renderGlassWindow(Graphics2D g2d, int x, int y, int w, int h) {
        // Tinted corporate interior glass panel
        g2d.setColor(new Color(0x3A, 0x5A, 0x78, 120));
        g2d.fillRect(x, y, w, h);
        g2d.setColor(new Color(0x60, 0x88, 0xB0));
        g2d.drawRect(x, y, w, h);
        // Glass sheen diagonal reflection
        g2d.setColor(new Color(255, 255, 255, 60));
        g2d.drawLine(x + 4, y + 4, x + Math.min(w - 4, 20), y + Math.min(h - 4, 20));
    }

    public String getName() { return name; }
    public CoverType getType() { return type; }
}
