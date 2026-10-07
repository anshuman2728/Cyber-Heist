package com.cyberheist.ai.pathfinding;

import java.awt.geom.Point2D;
import java.util.*;

/**
 * High-performance A* pathfinding engine using PriorityQueue and admissible heuristics.
 * Independent of rendering. Fully decoupled from concrete grid representations via Graph and Heuristic abstractions.
 */
public final class AStarPathfinder {

    private final Graph<NavNode> graph;
    private final Heuristic heuristic;
    private int currentSearchToken = 0;

    // Profiling & Diagnostic Metrics
    private long totalPathsComputed = 0;
    private long lastComputeTimeNanos = 0;
    private long totalComputeTimeNanos = 0;

    public AStarPathfinder(NavGrid grid) {
        this(grid, Heuristic.OCTILE);
    }

    public AStarPathfinder(Graph<NavNode> graph, Heuristic heuristic) {
        this.graph = graph;
        this.heuristic = heuristic != null ? heuristic : Heuristic.OCTILE;
    }

    /**
     * Computes optimal path from start world coordinates to target world coordinates.
     * Returns empty list if no valid path exists.
     */
    public List<Point2D.Double> findPath(double startX, double startY, double targetX, double targetY) {
        long startTime = System.nanoTime();

        NavNode startNode = graph.getNodeAtWorld(startX, startY);
        NavNode targetNode = graph.getNodeAtWorld(targetX, targetY);

        if (startNode == null || targetNode == null) {
            recordProfile(startTime);
            return Collections.emptyList();
        }

        // If target is blocked by obstacle, locate closest walkable neighboring node
        if (!targetNode.isWalkable()) {
            targetNode = findClosestWalkableNode(targetNode);
            if (targetNode == null) {
                recordProfile(startTime);
                return Collections.emptyList();
            }
        }

        if (startNode == targetNode) {
            List<Point2D.Double> single = new ArrayList<>(1);
            single.add(new Point2D.Double(targetX, targetY));
            recordProfile(startTime);
            return single;
        }

        // Increment search token for O(1) state reset without reallocating
        currentSearchToken++;

        PriorityQueue<NavNode> openSet = new PriorityQueue<>();
        Set<NavNode> closedSet = new HashSet<>();

        startNode.reset(currentSearchToken);
        startNode.setGCost(0.0);
        startNode.setHCost(heuristic.estimate(startNode, targetNode));
        openSet.add(startNode);

        while (!openSet.isEmpty()) {
            NavNode current = openSet.poll();

            if (current == targetNode) {
                List<Point2D.Double> path = reconstructPath(current, targetX, targetY);
                recordProfile(startTime);
                return path;
            }

            closedSet.add(current);

            for (NavNode neighbor : graph.getNeighbors(current)) {
                if (closedSet.contains(neighbor)) {
                    continue;
                }

                if (neighbor.getSearchToken() != currentSearchToken) {
                    neighbor.reset(currentSearchToken);
                }

                double stepCost = graph.getStepCost(current, neighbor);
                double tentativeGCost = current.getGCost() + stepCost;

                if (tentativeGCost < neighbor.getGCost()) {
                    neighbor.setParent(current);
                    neighbor.setGCost(tentativeGCost);
                    neighbor.setHCost(heuristic.estimate(neighbor, targetNode));

                    // PriorityQueue update: re-insert if modified
                    openSet.remove(neighbor);
                    openSet.add(neighbor);
                }
            }
        }

        // No complete path found
        recordProfile(startTime);
        return Collections.emptyList();
    }

    private void recordProfile(long startTime) {
        long duration = System.nanoTime() - startTime;
        this.totalPathsComputed++;
        this.lastComputeTimeNanos = duration;
        this.totalComputeTimeNanos += duration;
        com.cyberheist.core.TelemetryService.getInstance().recordPathSearch(duration / 1000);
    }

    private List<Point2D.Double> reconstructPath(NavNode endNode, double finalTargetX, double finalTargetY) {
        List<Point2D.Double> waypoints = new ArrayList<>();
        NavNode curr = endNode;

        while (curr != null) {
            waypoints.add(new Point2D.Double(curr.getWorldX(), curr.getWorldY()));
            curr = curr.getParent();
        }

        // Reverse to start -> destination order
        Collections.reverse(waypoints);

        // Replace the final grid node with exact destination coordinates for precision
        if (!waypoints.isEmpty()) {
            waypoints.set(waypoints.size() - 1, new Point2D.Double(finalTargetX, finalTargetY));
        }

        return waypoints;
    }

    private NavNode findClosestWalkableNode(NavNode blockedNode) {
        int radius = 3;
        NavNode best = null;
        double bestDist = Double.POSITIVE_INFINITY;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                NavNode candidate = graph.getNode(blockedNode.getGridX() + dx, blockedNode.getGridY() + dy);
                if (candidate != null && candidate.isWalkable()) {
                    double d = Math.hypot(dx, dy);
                    if (d < bestDist) {
                        bestDist = d;
                        best = candidate;
                    }
                }
            }
        }
        return best;
    }

    public Graph<NavNode> getGraph() { return graph; }
    public Heuristic getHeuristic() { return heuristic; }
    public long getTotalPathsComputed() { return totalPathsComputed; }
    public long getLastComputeTimeNanos() { return lastComputeTimeNanos; }
    public double getLastComputeTimeMillis() { return lastComputeTimeNanos / 1_000_000.0; }
    public double getAverageComputeTimeMillis() {
        return totalPathsComputed == 0 ? 0.0 : (totalComputeTimeNanos / (double) totalPathsComputed) / 1_000_000.0;
    }
}
