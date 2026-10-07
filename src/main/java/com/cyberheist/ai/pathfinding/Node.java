package com.cyberheist.ai.pathfinding;

/**
 * Common abstraction representing an indexable node within a spatial navigation graph.
 * Completely independent of rendering.
 */
public interface Node {

    int getGridX();

    int getGridY();

    double getWorldX();

    double getWorldY();

    boolean isWalkable();

    double getGCost();

    double getHCost();

    double getFCost();
}
