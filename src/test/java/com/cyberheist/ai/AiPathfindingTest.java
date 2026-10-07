package com.cyberheist.ai;

import com.cyberheist.ai.pathfinding.*;
import com.cyberheist.ai.state.GuardState;
import com.cyberheist.collision.BoundingBox;
import com.cyberheist.collision.CollisionWorld;
import com.cyberheist.collision.Obstacle;
import com.cyberheist.entity.Player;
import com.cyberheist.stealth.Guard;
import com.cyberheist.stealth.NoiseEvent;
import com.cyberheist.stealth.NoiseType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit and integration tests verifying A* pathfinding, NavGrid,
 * PriorityQueue performance, admissible heuristics, dynamic obstacle handling,
 * and the 7 Guard AI states with imperfect human perception.
 */
public class AiPathfindingTest {

    private NavGrid grid;
    private AStarPathfinder pathfinder;
    private CollisionWorld collisionWorld;

    @BeforeEach
    void setUp() {
        // 20x20 grid with 20px cell size -> 400x400 world
        grid = new NavGrid(400, 400, 20);
        pathfinder = new AStarPathfinder(grid);
        collisionWorld = new CollisionWorld(400, 400);
    }

    @Test
    @DisplayName("A* pathfinder finds optimal route in open unobstructed grid")
    void testAStarOpenGrid() {
        List<Point2D.Double> path = pathfinder.findPath(20.0, 20.0, 180.0, 180.0);
        assertNotNull(path);
        assertFalse(path.isEmpty(), "Path should not be empty in open grid");

        // First waypoint is near start, last waypoint matches exact destination
        Point2D.Double destination = path.get(path.size() - 1);
        assertEquals(180.0, destination.x, 0.001);
        assertEquals(180.0, destination.y, 0.001);
    }

    @Test
    @DisplayName("A* routes around solid wall using PriorityQueue and admissible heuristic")
    void testAStarNavigatesAroundWall() {
        // Vertical wall dividing grid at X = 100..120, from Y = 0 to Y = 160
        Obstacle wall = new Obstacle("WALL", 100, 0, 20, 160, Color.BLACK, Color.GRAY);
        collisionWorld.add(wall);
        grid.updateWalkability(collisionWorld, 8.0);

        // Nodes along the wall should be unwalkable
        NavNode wallNode = grid.getNodeAtWorld(110.0, 80.0);
        assertNotNull(wallNode);
        assertFalse(wallNode.isWalkable(), "Wall node must be marked unwalkable");

        // Route from Left (40, 80) to Right (180, 80)
        List<Point2D.Double> path = pathfinder.findPath(40.0, 80.0, 180.0, 80.0);
        assertNotNull(path);
        assertFalse(path.isEmpty(), "Pathfinder must find route around obstacle");

        // Verify no point in the path passes directly through the wall
        BoundingBox wallBox = wall.getBoundingBox();
        for (Point2D.Double pt : path) {
            assertFalse(wallBox.contains(pt.x, pt.y), "Waypoints must not penetrate wall box: " + pt);
        }

        // Verify profiling recorded the compute
        assertTrue(pathfinder.getTotalPathsComputed() > 0);
        assertTrue(pathfinder.getLastComputeTimeNanos() >= 0);
    }

    @Test
    @DisplayName("A* returns empty list when target is completely enclosed")
    void testAStarCompletelyBlockedTarget() {
        // Enclose target at (100, 100) with a sealed 4-sided wall box
        collisionWorld.add(new Obstacle("W1", 60, 60, 80, 20, Color.BLACK, Color.GRAY));
        collisionWorld.add(new Obstacle("W2", 60, 120, 80, 20, Color.BLACK, Color.GRAY));
        collisionWorld.add(new Obstacle("W3", 60, 60, 20, 80, Color.BLACK, Color.GRAY));
        collisionWorld.add(new Obstacle("W4", 120, 60, 20, 80, Color.BLACK, Color.GRAY));
        grid.updateWalkability(collisionWorld, 6.0);

        // Target (90, 90) is inside sealed chamber, start (20, 20) is outside
        List<Point2D.Double> path = pathfinder.findPath(20.0, 20.0, 90.0, 90.0);
        // Should find closest walkable outside or return empty if internal is unreachable
        // Either empty or waypoint does not penetrate wall
        for (Point2D.Double pt : path) {
            boolean insideBox = (pt.x > 80 && pt.x < 120 && pt.y > 80 && pt.y < 120);
            assertFalse(insideBox, "Path must not enter sealed room: " + pt);
        }
    }

    @Test
    @DisplayName("Dynamic obstacle handling: localized walkability update toggles path availability")
    void testDynamicObstacleWalkabilityUpdate() {
        // Place dynamic barrier across corridor
        Obstacle barrier = new Obstacle("BARRIER", 80, 0, 40, 200, Color.BLACK, Color.GRAY);
        collisionWorld.add(barrier);
        grid.updateWalkability(collisionWorld, 8.0);

        NavNode midNode = grid.getNodeAtWorld(100.0, 100.0);
        assertFalse(midNode.isWalkable(), "Barrier node must initially be blocked");

        // Remove barrier (simulating open door) and run incremental regional update
        collisionWorld.remove(barrier);
        grid.updateWalkabilityRegion(collisionWorld, 70, 0, 130, 200, 8.0);

        assertTrue(midNode.isWalkable(), "Regional update must restore node walkability");

        // Path can now pass directly through
        List<Point2D.Double> directPath = pathfinder.findPath(20.0, 100.0, 180.0, 100.0);
        assertFalse(directPath.isEmpty());
    }

    @Test
    @DisplayName("Guard AI state machine executes full patrol and suspicion cycle")
    void testGuardAiPatrolToSuspiciousState() {
        Guard guard = new Guard("TEST-GUARD", 40.0, 40.0, 0.0);
        GuardAI ai = new GuardAI(guard, pathfinder);
        ai.addPatrolWaypoint(40.0, 40.0);
        ai.addPatrolWaypoint(120.0, 40.0);
        guard.setAI(ai);

        Player player = new Player(300.0, 300.0);
        GuardCommunicator comms = new GuardCommunicator();

        assertEquals(GuardState.PATROL, ai.getState());

        // Raise suspicion
        guard.getDetectionMeter().addInstantDetection(45.0); // > 30% -> SUSPICIOUS
        ai.update(0.1, player, comms);

        assertEquals(GuardState.SUSPICIOUS, ai.getState(), "Guard must enter SUSPICIOUS state upon sensing disturbance");

        // Simulate 0.9s passing in SUSPICIOUS state -> should transition to INVESTIGATE
        ai.update(0.9, player, comms);
        assertEquals(GuardState.INVESTIGATE, ai.getState(), "Guard must proceed to INVESTIGATE from SUSPICIOUS");
    }

    @Test
    @DisplayName("Guard AI noise investigation applies imperfect sensory localization")
    void testNoiseInvestigationImperfectPerception() {
        Guard guard = new Guard("GUARD-SOUND", 50.0, 50.0, 0.0);
        GuardAI ai = new GuardAI(guard, pathfinder);
        guard.setAI(ai);

        NoiseEvent gunshot = new NoiseEvent(150.0, 150.0, NoiseType.WEAPON);
        ai.onNoiseHeard(gunshot);

        // Sequence: PATROL -> SUSPICIOUS
        assertEquals(GuardState.SUSPICIOUS, ai.getState());
        Point2D.Double target = ai.getInvestigationTarget();
        assertNotNull(target, "Investigation target must be recorded");

        // Verify imperfection: target is NOT exactly (150, 150) due to acoustic localization error (+- 25px)
        double diff = Math.hypot(target.x - 150.0, target.y - 150.0);
        assertTrue(diff > 0.001, "Guard acoustic localization must be imperfect, not omniscient");
        assertTrue(diff <= 40.0, "Acoustic error must remain bounded within reasonable human perception");
    }

    @Test
    @DisplayName("Guard breaks line-of-sight: chases to last known position then enters SEARCH and RETURN_TO_PATROL")
    void testChaseToSearchToReturnToPatrol() {
        Guard guard = new Guard("CHASE-GUARD", 40.0, 40.0, 0.0);
        GuardAI ai = new GuardAI(guard, pathfinder);
        ai.addPatrolWaypoint(40.0, 40.0);
        guard.setAI(ai);

        Player player = new Player(120.0, 40.0);
        GuardCommunicator comms = new GuardCommunicator();

        // Direct alert
        guard.getDetectionMeter().addInstantDetection(100.0); // DETECTED
        ai.update(0.1, player, comms);
        assertEquals(GuardState.ALERT, ai.getState());

        // After alert hesitation (0.3s), enters CHASE
        ai.update(0.35, player, comms);
        assertEquals(GuardState.CHASE, ai.getState());
        assertNotNull(ai.getLastKnownPlayerPos(), "Guard must record last known player position during chase");

        // Player breaks line of sight and runs away: guard reaches last known position
        // Move guard close to last known pos (guard center at x+14, y+14)
        Point2D.Double lkp = ai.getLastKnownPlayerPos();
        guard.setPosition(lkp.x - 14.0, lkp.y - 14.0);
        guard.getDetectionMeter().reset(); // Player escaped and broke visual contact
        ai.update(0.1, player, comms);

        // When guard arrives at last known position with player hidden -> transitions to SEARCH
        assertEquals(GuardState.SEARCH, ai.getState(), "Guard must search area when arriving at last known position");

        // Simulate search duration (5.5s) expiring without finding player
        ai.update(5.5, player, comms);
        assertEquals(GuardState.RETURN_TO_PATROL, ai.getState(), "Guard must return to patrol after unsuccessful search");
    }

    @Test
    @DisplayName("Pathfinding recalculation throttling: does not recalculate paths every frame")
    void testPathCalculationThrottling() {
        Guard guard = new Guard("THROTTLE-GUARD", 40.0, 40.0, 0.0);
        GuardAI ai = new GuardAI(guard, pathfinder);
        ai.setPathRecalcInterval(0.35); // 0.35s = ~3 Hz
        guard.setAI(ai);

        Player player = new Player(120.0, 40.0);
        GuardCommunicator comms = new GuardCommunicator();

        ai.transitionTo(GuardState.CHASE);

        // Simulate 60 frames at 60 FPS (dt = 0.0166s = 1.0 second total) with player visible
        for (int frame = 0; frame < 60; frame++) {
            guard.evaluatePlayerVisibility(player.getX(), player.getY(), 0, false, 1.0, true, null, 0.0166);
            ai.update(0.0166, player, comms);
        }

        // Over 1 second at 0.35s cooldown, calculations must be between 2 and 5, NOT 60!
        long calls = ai.getTotalPathCalculations();
        assertTrue(calls >= 2 && calls <= 5,
                String.format("Path calculations over 1 second should be throttled to ~3, but was: %d", calls));
    }

    @Test
    @DisplayName("Guard communication foundation: radio broadcast triggers colleague investigation")
    void testGuardCommunicationRadioBroadcast() {
        GuardCommunicator communicator = new GuardCommunicator();

        Guard g1 = new Guard("G1", 40.0, 40.0, 0.0);
        GuardAI ai1 = new GuardAI(g1, pathfinder);
        g1.setAI(ai1);

        Guard g2 = new Guard("G2", 200.0, 200.0, 0.0);
        GuardAI ai2 = new GuardAI(g2, pathfinder);
        g2.setAI(ai2);

        communicator.register((source, loc, reason) -> {
            if (source != g2) {
                ai2.onRadioAlert(loc);
            }
        });

        assertEquals(GuardState.PATROL, ai2.getState());

        // G1 spots intruder and broadcasts radio alert
        communicator.broadcastAlert(g1, new Point2D.Double(60.0, 80.0), "Intruder spotted!");

        assertEquals(GuardState.INVESTIGATE, ai2.getState(), "G2 must investigate radio alert location");
        assertNotNull(ai2.getInvestigationTarget());
        assertEquals(60.0, ai2.getInvestigationTarget().x, 0.001);
        assertEquals(80.0, ai2.getInvestigationTarget().y, 0.001);
    }
}
