package com.cyberheist.ai.pathfinding;

import com.cyberheist.collision.BoundingBox;
import com.cyberheist.collision.CollisionWorld;

import java.util.ArrayList;
import java.util.List;

/**
 * 2D Navigation Grid managing walkable graph nodes and 8-way neighbor queries.
 * Completely independent of rendering.
 */
public final class NavGrid implements Graph<NavNode> {

    private final int columns;
    private final int rows;
    private final int nodeSize;
    private final NavNode[][] nodes;

    public NavGrid(int worldWidth, int worldHeight, int nodeSize) {
        this.nodeSize = nodeSize;
        this.columns = Math.max(1, (worldWidth + nodeSize - 1) / nodeSize);
        this.rows = Math.max(1, (worldHeight + nodeSize - 1) / nodeSize);
        this.nodes = new NavNode[columns][rows];

        for (int c = 0; c < columns; c++) {
            for (int r = 0; r < rows; r++) {
                double wx = (c * nodeSize) + (nodeSize / 2.0);
                double wy = (r * nodeSize) + (nodeSize / 2.0);
                nodes[c][r] = new NavNode(c, r, wx, wy);
            }
        }
    }

    /**
     * Rebuilds node walkability flags based on static/dynamic obstacles in the collision world.
     */
    public void updateWalkability(CollisionWorld world, double agentRadius) {
        if (world == null) return;

        double boxPadding = agentRadius * 0.9;
        double testDim = boxPadding * 2.0;

        for (int c = 0; c < columns; c++) {
            for (int r = 0; r < rows; r++) {
                NavNode node = nodes[c][r];
                BoundingBox testBox = new BoundingBox(
                        node.getWorldX() - boxPadding,
                        node.getWorldY() - boxPadding,
                        testDim, testDim
                );
                boolean blocked = world.checkCollision(testBox);
                node.setWalkable(!blocked);
            }
        }
    }

    /**
     * Incrementally updates walkability within an axis-aligned bounding area.
     * Allows O(1) dynamic obstacle handling (e.g. doors opening/closing) without global recalculation.
     */
    public void updateWalkabilityRegion(CollisionWorld world, double minX, double minY, double maxX, double maxY, double agentRadius) {
        if (world == null) return;

        double boxPadding = agentRadius * 0.9;
        double testDim = boxPadding * 2.0;

        int minCol = Math.max(0, (int) Math.floor((minX - agentRadius) / nodeSize));
        int maxCol = Math.min(columns - 1, (int) Math.floor((maxX + agentRadius) / nodeSize));
        int minRow = Math.max(0, (int) Math.floor((minY - agentRadius) / nodeSize));
        int maxRow = Math.min(rows - 1, (int) Math.floor((maxY + agentRadius) / nodeSize));

        for (int c = minCol; c <= maxCol; c++) {
            for (int r = minRow; r <= maxRow; r++) {
                NavNode node = nodes[c][r];
                BoundingBox testBox = new BoundingBox(
                        node.getWorldX() - boxPadding,
                        node.getWorldY() - boxPadding,
                        testDim, testDim
                );
                boolean blocked = world.checkCollision(testBox);
                node.setWalkable(!blocked);
            }
        }
    }

    private static final double SQRT_2 = Math.sqrt(2.0);

    @Override
    public double getStepCost(NavNode from, NavNode to) {
        return (from.getGridX() != to.getGridX() && from.getGridY() != to.getGridY()) ? SQRT_2 : 1.0;
    }

    @Override
    public NavNode getNodeAtWorld(double worldX, double worldY) {
        int c = (int) Math.floor(worldX / nodeSize);
        int r = (int) Math.floor(worldY / nodeSize);
        c = Math.max(0, Math.min(columns - 1, c));
        r = Math.max(0, Math.min(rows - 1, r));
        return nodes[c][r];
    }

    public NavNode getNode(int col, int row) {
        if (col >= 0 && col < columns && row >= 0 && row < rows) {
            return nodes[col][row];
        }
        return null;
    }

    /**
     * Resolves 8-directional neighbors with corner-cutting prevention.
     */
    public List<NavNode> getNeighbors(NavNode node) {
        List<NavNode> neighbors = new ArrayList<>(8);
        int cx = node.getGridX();
        int cy = node.getGridY();

        boolean north = isValidAndWalkable(cx, cy - 1);
        boolean south = isValidAndWalkable(cx, cy + 1);
        boolean west = isValidAndWalkable(cx - 1, cy);
        boolean east = isValidAndWalkable(cx + 1, cy);

        // Cardinal directions
        if (north) neighbors.add(nodes[cx][cy - 1]);
        if (south) neighbors.add(nodes[cx][cy + 1]);
        if (west) neighbors.add(nodes[cx - 1][cy]);
        if (east) neighbors.add(nodes[cx + 1][cy]);

        // Diagonal directions (only allowed if both adjacent orthogonal neighbors are walkable)
        if (north && east && isValidAndWalkable(cx + 1, cy - 1)) {
            neighbors.add(nodes[cx + 1][cy - 1]);
        }
        if (north && west && isValidAndWalkable(cx - 1, cy - 1)) {
            neighbors.add(nodes[cx - 1][cy - 1]);
        }
        if (south && east && isValidAndWalkable(cx + 1, cy + 1)) {
            neighbors.add(nodes[cx + 1][cy + 1]);
        }
        if (south && west && isValidAndWalkable(cx - 1, cy + 1)) {
            neighbors.add(nodes[cx - 1][cy + 1]);
        }

        return neighbors;
    }

    private boolean isValidAndWalkable(int col, int row) {
        if (col >= 0 && col < columns && row >= 0 && row < rows) {
            return nodes[col][row].isWalkable();
        }
        return false;
    }

    public int getColumns() { return columns; }
    public int getRows() { return rows; }
    public int getNodeSize() { return nodeSize; }
}
