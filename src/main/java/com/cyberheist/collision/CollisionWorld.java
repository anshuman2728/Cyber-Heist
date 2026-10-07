package com.cyberheist.collision;

import com.cyberheist.core.GameConfig;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages physical collidables and coordinates world boundary & entity collision checks.
 */
public final class CollisionWorld {

    private final List<Collidable> collidables = new ArrayList<>();
    private final double worldWidth;
    private final double worldHeight;

    public CollisionWorld(double worldWidth, double worldHeight) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
    }

    public void add(Collidable collidable) {
        if (collidable != null && !collidables.contains(collidable)) {
            collidables.add(collidable);
        }
    }

    public void remove(Collidable collidable) {
        collidables.remove(collidable);
    }

    public void clear() {
        collidables.clear();
    }

    /**
     * Checks if a target bounding box collides with world borders or any solid collidable.
     */
    public boolean checkCollision(BoundingBox box, Collidable ignore) {
        if (box == null) return false;

        // 1. World perimeter boundary check
        if (box.getMinX() < 0 || box.getMaxX() > worldWidth ||
            box.getMinY() < 0 || box.getMaxY() > worldHeight) {
            return true;
        }

        // 2. Obstacle collision check
        for (Collidable collidable : collidables) {
            if (collidable == ignore || !collidable.isSolid()) {
                continue;
            }
            if (box.intersects(collidable.getBoundingBox())) {
                return true;
            }
        }

        return false;
    }

    public boolean checkCollision(BoundingBox box) {
        return checkCollision(box, null);
    }

    public List<Collidable> getCollidables() {
        return Collections.unmodifiableList(collidables);
    }

    public void render(Graphics2D g2d) {
        for (Collidable collidable : collidables) {
            if (collidable instanceof Obstacle obstacle) {
                obstacle.render(g2d);
            }
        }
    }

    public double getWorldWidth() { return worldWidth; }
    public double getWorldHeight() { return worldHeight; }
}
