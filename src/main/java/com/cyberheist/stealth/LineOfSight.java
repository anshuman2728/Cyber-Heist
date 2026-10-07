package com.cyberheist.stealth;

import com.cyberheist.collision.BoundingBox;
import com.cyberheist.collision.Collidable;
import com.cyberheist.collision.CollisionWorld;

import java.awt.geom.Point2D;

/**
 * Pure mathematical 2D raycasting and line-of-sight evaluation against AABB obstacles.
 * Completely independent of rendering.
 */
public final class LineOfSight {

    private LineOfSight() {
        // Utility class
    }

    /**
     * Checks if a line-of-sight segment from (x1, y1) to (x2, y2) is unblocked by any solid collidable.
     */
    public static boolean hasClearLineOfSight(double x1, double y1, double x2, double y2,
                                             CollisionWorld world, Collidable ignore) {
        if (world == null) return true;

        for (Collidable collidable : world.getCollidables()) {
            if (collidable == ignore || !collidable.isSolid()) {
                continue;
            }
            if (lineIntersectsBox(x1, y1, x2, y2, collidable.getBoundingBox())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Finds the closest collision point along a ray segment, or null if unoccluded.
     */
    public static Point2D.Double findObstacleIntersection(double x1, double y1, double x2, double y2,
                                                          CollisionWorld world, Collidable ignore) {
        if (world == null) return null;

        double closestT = 1.0;
        boolean hit = false;

        for (Collidable collidable : world.getCollidables()) {
            if (collidable == ignore || !collidable.isSolid()) {
                continue;
            }
            Double t = getIntersectionFraction(x1, y1, x2, y2, collidable.getBoundingBox());
            if (t != null && t >= 0.0 && t <= closestT) {
                closestT = t;
                hit = true;
            }
        }

        if (hit) {
            double hx = x1 + closestT * (x2 - x1);
            double hy = y1 + closestT * (y2 - y1);
            return new Point2D.Double(hx, hy);
        }

        return null;
    }

    /**
     * Slab method: Tests line segment [P1, P2] against Axis-Aligned Bounding Box.
     */
    public static boolean lineIntersectsBox(double x1, double y1, double x2, double y2, BoundingBox box) {
        Double t = getIntersectionFraction(x1, y1, x2, y2, box);
        return t != null && t >= 0.0 && t <= 1.0;
    }

    /**
     * Calculates the parametric 't' (0.0 to 1.0) along ray segment where it enters the box.
     */
    public static Double getIntersectionFraction(double x1, double y1, double x2, double y2, BoundingBox box) {
        double dx = x2 - x1;
        double dy = y2 - y1;

        double minX = box.getMinX();
        double maxX = box.getMaxX();
        double minY = box.getMinY();
        double maxY = box.getMaxY();

        double tMin = 0.0;
        double tMax = 1.0;

        // X slab
        if (Math.abs(dx) < 1e-9) {
            // Ray is parallel to X slabs
            if (x1 < minX || x1 > maxX) return null;
        } else {
            double ood = 1.0 / dx;
            double t1 = (minX - x1) * ood;
            double t2 = (maxX - x1) * ood;
            if (t1 > t2) {
                double temp = t1; t1 = t2; t2 = temp;
            }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) return null;
        }

        // Y slab
        if (Math.abs(dy) < 1e-9) {
            // Ray is parallel to Y slabs
            if (y1 < minY || y1 > maxY) return null;
        } else {
            double ood = 1.0 / dy;
            double t1 = (minY - y1) * ood;
            double t2 = (maxY - y1) * ood;
            if (t1 > t2) {
                double temp = t1; t1 = t2; t2 = temp;
            }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) return null;
        }

        return tMin;
    }
}
