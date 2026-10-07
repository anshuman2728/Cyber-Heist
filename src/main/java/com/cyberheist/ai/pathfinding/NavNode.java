package com.cyberheist.ai.pathfinding;

/**
 * Represents a single node on the 2D navigation grid for A* pathfinding.
 */
public final class NavNode implements Node, Comparable<NavNode> {

    private final int gridX;
    private final int gridY;
    private final double worldX;
    private final double worldY;

    private boolean walkable = true;

    // A* Metrics
    private double gCost = 0.0;
    private double hCost = 0.0;
    private NavNode parent = null;
    private int searchToken = 0; // Avoids O(N) array resets across path queries

    public NavNode(int gridX, int gridY, double worldX, double worldY) {
        this.gridX = gridX;
        this.gridY = gridY;
        this.worldX = worldX;
        this.worldY = worldY;
    }

    public double getFCost() {
        return gCost + hCost;
    }

    @Override
    public int compareTo(NavNode other) {
        int cmp = Double.compare(this.getFCost(), other.getFCost());
        if (cmp == 0) {
            // Tie-break with smaller heuristic cost toward destination
            return Double.compare(this.hCost, other.hCost);
        }
        return cmp;
    }

    public void reset(int currentSearchToken) {
        this.searchToken = currentSearchToken;
        this.gCost = Double.POSITIVE_INFINITY;
        this.hCost = 0.0;
        this.parent = null;
    }

    public int getGridX() { return gridX; }
    public int getGridY() { return gridY; }
    public double getWorldX() { return worldX; }
    public double getWorldY() { return worldY; }
    public boolean isWalkable() { return walkable; }
    public void setWalkable(boolean walkable) { this.walkable = walkable; }
    public double getGCost() { return gCost; }
    public void setGCost(double gCost) { this.gCost = gCost; }
    public double getHCost() { return hCost; }
    public void setHCost(double hCost) { this.hCost = hCost; }
    public NavNode getParent() { return parent; }
    public void setParent(NavNode parent) { this.parent = parent; }
    public int getSearchToken() { return searchToken; }
}
