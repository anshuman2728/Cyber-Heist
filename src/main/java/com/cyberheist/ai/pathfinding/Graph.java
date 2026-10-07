package com.cyberheist.ai.pathfinding;

import java.util.List;

/**
 * Spatial navigation graph abstraction connecting nodes with weighted edges.
 * Keeps graph traversal algorithms completely decoupled from specific grid/mesh data structures.
 *
 * @param <N> Node type extending Node
 */
public interface Graph<N extends Node> {

    /**
     * Resolves accessible neighboring nodes from the given node.
     */
    List<N> getNeighbors(N node);

    /**
     * Retrieves the node corresponding to continuous world coordinates.
     */
    N getNodeAtWorld(double worldX, double worldY);

    /**
     * Retrieves node by discrete grid column and row indices.
     */
    N getNode(int col, int row);

    /**
     * Returns movement step cost between two adjacent nodes (e.g. 1.0 for orthogonal, sqrt(2) for diagonal).
     */
    double getStepCost(N from, N to);
}
