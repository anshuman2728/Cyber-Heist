package com.cyberheist;

import com.cyberheist.collision.BoundingBox;
import com.cyberheist.collision.CollisionWorld;
import com.cyberheist.display.Camera2D;
import com.cyberheist.world.Door;
import com.cyberheist.world.NoidaDataCenterLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Point;

import static org.junit.jupiter.api.Assertions.*;

public class LevelEnvironmentTest {

    private NoidaDataCenterLevel level;

    @BeforeEach
    void setUp() {
        level = new NoidaDataCenterLevel();
    }

    @Test
    void testLevelDimensionsAndSpawnPoint() {
        assertEquals(1760, level.getWorldWidth());
        assertEquals(1040, level.getWorldHeight());

        Point spawn = level.getSpawnPoint();
        assertNotNull(spawn);
        assertEquals(220, spawn.x);
        assertEquals(100, spawn.y);

        // Spawn point should be located in exterior sidewalk area (y < 190)
        assertTrue(spawn.y < 190, "Spawn point must be in exterior zone");
        assertFalse(level.getCollisionWorld().checkCollision(new BoundingBox(spawn.x, spawn.y, 28, 28)),
                "Spawn point must be clear of collisions");
    }

    @Test
    void testDoorCollisionAndInteraction() {
        Door door = new Door("Test Door", Door.DoorType.SECURITY_BADGE, 400, 300, 80, 16, true);
        assertFalse(door.isOpen(), "Door should initially be closed");
        assertTrue(door.isSolid(), "Closed door should be solid");

        // Collision check while closed
        BoundingBox testBox = new BoundingBox(420, 295, 20, 20);
        assertTrue(testBox.intersects(door.getBoundingBox()), "Closed door must intersect test box");

        // Interact to open
        door.onInteract();
        assertTrue(door.isOpen(), "Door should be open after interaction");

        // Advance door open animation
        for (int i = 0; i < 20; i++) {
            door.update(0.1);
        }

        assertFalse(door.isSolid(), "Fully opened door must not be solid");
        assertEquals(0, door.getBoundingBox().getWidth(), 0.01, "Opened door collision width collapses");
    }

    @Test
    void testExitZoneTrigger() {
        BoundingBox exitZone = level.getExitZone();
        assertNotNull(exitZone);

        // Outside exit zone
        BoundingBox outside = new BoundingBox(100, 100, 28, 28);
        assertFalse(outside.intersects(exitZone));

        // Inside exit zone
        BoundingBox inside = new BoundingBox(exitZone.getX() + 10, exitZone.getY() + 10, 28, 28);
        assertTrue(inside.intersects(exitZone));
    }

    @Test
    void testCameraClamping() {
        Camera2D camera = new Camera2D(1280, 720, 1760, 1040);

        // Target top-left corner
        camera.update(0, 0, 1.0, 10.0);
        assertEquals(0.0, camera.getX(), 0.01);
        assertEquals(0.0, camera.getY(), 0.01);

        // Target far bottom-right corner beyond world bounds
        camera.update(3000, 3000, 1.0, 10.0);
        double maxExpectedX = 1760 - 1280; // 480
        double maxExpectedY = 1040 - 720;  // 320
        assertEquals(maxExpectedX, camera.getX(), 0.01);
        assertEquals(maxExpectedY, camera.getY(), 0.01);
    }
}
