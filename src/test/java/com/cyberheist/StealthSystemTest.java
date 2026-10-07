package com.cyberheist;

import com.cyberheist.collision.BoundingBox;
import com.cyberheist.collision.CollisionWorld;
import com.cyberheist.collision.Obstacle;
import com.cyberheist.entity.Player;
import com.cyberheist.stealth.*;
import com.cyberheist.world.CoverObject;
import com.cyberheist.world.Door;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class StealthSystemTest {

    private CollisionWorld world;
    private StealthSystem stealthSystem;
    private Guard guard;
    private Player player;
    private List<CoverObject> coverObjects;

    @BeforeEach
    void setUp() {
        world = new CollisionWorld(1760, 1040);
        stealthSystem = new StealthSystem();
        // Guard at (200, 200) facing East (0 degrees)
        guard = new Guard("GUARD-TEST", 200, 200, 0);
        stealthSystem.addGuard(guard);
        player = new Player(350, 200); // 150px in front of guard
        coverObjects = new ArrayList<>();
    }

    @Test
    void testClearLineOfSight() {
        boolean clear = LineOfSight.hasClearLineOfSight(guard.getX() + 14, guard.getY() + 14,
                player.getX() + 14, player.getY() + 14, world, guard);
        assertTrue(clear, "Line of sight should be clear in open room");
    }

    @Test
    void testLineOfSightBlockedByWall() {
        // Place a wall between guard and player at X=280
        Obstacle wall = new Obstacle("WALL", 280, 150, 20, 100);
        world.add(wall);

        boolean clear = LineOfSight.hasClearLineOfSight(guard.getX() + 14, guard.getY() + 14,
                player.getX() + 14, player.getY() + 14, world, guard);
        assertFalse(clear, "Line of sight must be blocked by solid wall");
        assertNotNull(LineOfSight.findObstacleIntersection(guard.getX() + 14, guard.getY() + 14,
                player.getX() + 14, player.getY() + 14, world, guard), "Ray must report obstacle impact point");
    }

    @Test
    void testLineOfSightBlockedByClosedDoorAndClearedOnOpen() {
        Door door = new Door("GATE", Door.DoorType.SECURITY_BADGE, 280, 180, 16, 80, false);
        world.add(door);

        // When door is closed
        assertFalse(LineOfSight.hasClearLineOfSight(guard.getX() + 14, guard.getY() + 14,
                player.getX() + 14, player.getY() + 14, world, guard), "Closed door must block LOS");

        // Open door and advance opening animation
        door.onInteract();
        for (int i = 0; i < 20; i++) door.update(0.1);

        assertTrue(LineOfSight.hasClearLineOfSight(guard.getX() + 14, guard.getY() + 14,
                player.getX() + 14, player.getY() + 14, world, guard), "Open door must permit clear LOS");
    }

    @Test
    void testVisionConeFOVBounds() {
        VisionCone cone = guard.getVisionCone();

        // 100px directly in front (angle 0) -> inside FOV
        assertTrue(cone.contains(200, 200, 0, 300, 200));

        // 100px directly behind guard (angle PI) -> outside FOV
        assertFalse(cone.contains(200, 200, 0, 100, 200));

        // Beyond max view distance (400px away) -> outside FOV
        assertFalse(cone.contains(200, 200, 0, 700, 200));
    }

    @Test
    void testDetectionBuildAndDecay() {
        assertEquals(0.0, guard.getDetectionMeter().getPercentage(), 0.01);
        assertEquals(DetectionState.UNAWARE, guard.getDetectionMeter().getState());

        // Player observed for 0.5s -> detection accumulates
        for (int i = 0; i < 10; i++) {
            stealthSystem.update(0.05, player, world, coverObjects);
        }

        double pctAfterLook = guard.getDetectionMeter().getPercentage();
        assertTrue(pctAfterLook > 0.0, "Detection percentage must accumulate while player is visible");

        // Move player behind a solid wall to break LOS
        Obstacle wall = new Obstacle("WALL", 260, 150, 30, 100);
        world.add(wall);

        // Update 1.5s while unobserved -> detection must decay
        for (int i = 0; i < 30; i++) {
            stealthSystem.update(0.05, player, world, coverObjects);
        }

        assertTrue(guard.getDetectionMeter().getPercentage() < pctAfterLook, "Detection must decay when unobserved");
    }

    @Test
    void testRunningVersusWalkingDetectionSpeed() {
        // Measure build when stationary/walking
        stealthSystem.update(0.2, player, world, coverObjects);
        double walkBuild = guard.getDetectionMeter().getPercentage();

        guard.getDetectionMeter().reset();

        // Simulate sprinting (speed > 250 px/s)
        player.setSprintSpeed(380.0);
        // Emulate moving at sprint velocity by adding speed
        guard.evaluatePlayerVisibility(player.getX(), player.getY(), 380.0,
                false, 1.0, true, null, 0.2);
        double sprintBuild = guard.getDetectionMeter().getPercentage();

        assertTrue(sprintBuild > walkBuild, "Running must increase detection rate faster than walking");
    }

    @Test
    void testCoverReducesVisibilityRate() {
        // Open visibility build rate
        guard.evaluatePlayerVisibility(player.getX(), player.getY(), 0.0,
                false, 1.0, true, null, 0.2);
        double openBuild = guard.getDetectionMeter().getPercentage();

        guard.getDetectionMeter().reset();

        // In cover build rate (65% reduction)
        guard.evaluatePlayerVisibility(player.getX(), player.getY(), 0.0,
                true, 1.0, true, null, 0.2);
        double coverBuild = guard.getDetectionMeter().getPercentage();

        assertTrue(coverBuild < openBuild, "Taking cover must significantly reduce detection rate");
        assertEquals(openBuild * 0.35, coverBuild, 0.01, "Cover reduces rate by 65%");
    }

    @Test
    void testNoisePropagationAndHearing() {
        NoiseManager noiseManager = stealthSystem.getNoiseManager();

        // Emit footstep walking noise 80px from guard (within 120px radius)
        noiseManager.emit(280, 200, NoiseType.WALKING);
        NoiseEvent heard = noiseManager.findStrongestAudibleNoise(guard.getX(), guard.getY(), guard.getHearingRadius());
        assertNotNull(heard, "Guard must hear walk noise within radius");

        noiseManager.clear();

        // Emit walk noise 500px away (beyond radius)
        noiseManager.emit(700, 200, NoiseType.WALKING);
        NoiseEvent unreached = noiseManager.findStrongestAudibleNoise(guard.getX(), guard.getY(), guard.getHearingRadius());
        assertNull(unreached, "Guard must not hear quiet noise beyond radius");

        noiseManager.clear();

        // Emit weapon gunfire noise 500px away (within 620px radius)
        noiseManager.emit(700, 200, NoiseType.WEAPON);
        NoiseEvent heardGunshot = noiseManager.findStrongestAudibleNoise(guard.getX(), guard.getY(), guard.getHearingRadius());
        assertNotNull(heardGunshot, "Guard must hear loud gunfire from long distance");
    }

    @Test
    void testNonBinaryDetectionTiers() {
        DetectionMeter meter = new DetectionMeter(100.0, 20.0);

        assertEquals(DetectionState.UNAWARE, meter.getState());

        meter.addInstantDetection(35.0);
        assertEquals(DetectionState.SUSPICIOUS, meter.getState());

        meter.addInstantDetection(35.0); // Now 70%
        assertEquals(DetectionState.DETECTED, meter.getState());
    }
}
