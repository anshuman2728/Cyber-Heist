package com.cyberheist.collision;

import com.cyberheist.core.GameConfig;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Concrete static obstacle for walls, barricades, and physical fixtures.
 */
public class Obstacle implements Collidable {

    private final BoundingBox bounds;
    private final String name;
    private final Color primaryColor;
    private final Color borderColor;

    public Obstacle(String name, double x, double y, double width, double height) {
        this(name, x, y, width, height, new Color(0x16, 0x22, 0x36), GameConfig.COLOR_CYAN);
    }

    public Obstacle(String name, double x, double y, double width, double height,
                    Color primaryColor, Color borderColor) {
        this.name = name;
        this.bounds = new BoundingBox(x, y, width, height);
        this.primaryColor = primaryColor;
        this.borderColor = borderColor;
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

        // Body
        g2d.setColor(primaryColor);
        g2d.fillRect(x, y, w, h);

        // Border
        g2d.setColor(borderColor);
        g2d.drawRect(x, y, w, h);

        // Tech accent pattern
        g2d.setColor(new Color(borderColor.getRed(), borderColor.getGreen(), borderColor.getBlue(), 50));
        g2d.drawLine(x, y, x + w, y + h);
        g2d.drawLine(x + w, y, x, y + h);

        // Label if large enough
        if (w >= 60 && h >= 24) {
            g2d.setFont(GameConfig.FONT_CODE);
            g2d.setColor(GameConfig.COLOR_TEXT_MUTED);
            g2d.drawString(name, x + 8, y + 16);
        }
    }

    public String getName() {
        return name;
    }
}
