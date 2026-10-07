package com.cyberheist.ai.pathfinding;

/**
 * Admissible heuristic estimator interface for A* pathfinding.
 * Used to guide PriorityQueue expansion toward the target node.
 */
@FunctionalInterface
public interface Heuristic {

    /**
     * Estimates distance cost between two navigation nodes.
     */
    double estimate(NavNode from, NavNode to);

    /**
     * Admissible Octile heuristic for 8-directional grids.
     * Accurately reflects diagonal movement cost of sqrt(2) vs orthogonal cost of 1.
     */
    Heuristic OCTILE = (from, to) -> {
        double dx = Math.abs(from.getGridX() - to.getGridX());
        double dy = Math.abs(from.getGridY() - to.getGridY());
        return Math.max(dx, dy) + (Math.sqrt(2.0) - 1.0) * Math.min(dx, dy);
    };

    /**
     * Standard Euclidean straight-line distance heuristic.
     */
    Heuristic EUCLIDEAN = (from, to) -> {
        double dx = from.getGridX() - to.getGridX();
        double dy = from.getGridY() - to.getGridY();
        return Math.hypot(dx, dy);
    };

    /**
     * Manhattan taxi-cab distance heuristic (4-directional movement).
     */
    Heuristic MANHATTAN = (from, to) -> {
        return Math.abs(from.getGridX() - to.getGridX()) + Math.abs(from.getGridY() - to.getGridY());
    };
}
