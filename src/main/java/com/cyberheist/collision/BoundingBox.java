package com.cyberheist.collision;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Axis-Aligned Bounding Box (AABB) for 2D collision detection.
 */
public final class BoundingBox {

    private final double x;
    private final double y;
    private final double width;
    private final double height;

    public BoundingBox(double x, double y, double width, double height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("BoundingBox width and height must be non-negative.");
        }
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Checks if this bounding box intersects another bounding box.
     */
    public boolean intersects(BoundingBox other) {
        if (other == null) return false;
        return this.x < other.x + other.width &&
               this.x + this.width > other.x &&
               this.y < other.y + other.height &&
               this.y + this.height > other.y;
    }

    /**
     * Checks if a point lies inside this bounding box.
     */
    public boolean contains(double px, double py) {
        return px >= this.x && px <= this.x + this.width &&
               py >= this.y && py <= this.y + this.height;
    }

    /**
     * Returns a new bounding box shifted to the specified (newX, newY) origin.
     */
    public BoundingBox atPosition(double newX, double newY) {
        return new BoundingBox(newX, newY, this.width, this.height);
    }

    /**
     * Debug visualization helper.
     */
    public void render(Graphics2D g2d, Color color, boolean fill) {
        g2d.setColor(color);
        int rx = (int) Math.round(x);
        int ry = (int) Math.round(y);
        int rw = (int) Math.round(width);
        int rh = (int) Math.round(height);

        if (fill) {
            g2d.fillRect(rx, ry, rw, rh);
        } else {
            g2d.drawRect(rx, ry, rw, rh);
        }
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public double getMinX() { return x; }
    public double getMaxX() { return x + width; }
    public double getMinY() { return y; }
    public double getMaxY() { return y + height; }

    @Override
    public String toString() {
        return String.format("AABB[x=%.1f, y=%.1f, w=%.1f, h=%.1f]", x, y, width, height);
    }
}
