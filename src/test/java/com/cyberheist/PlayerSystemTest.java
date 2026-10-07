package com.cyberheist;

import com.cyberheist.collision.BoundingBox;
import com.cyberheist.collision.CollisionWorld;
import com.cyberheist.collision.Obstacle;
import com.cyberheist.entity.FacingDirection;
import com.cyberheist.entity.Player;
import com.cyberheist.entity.PlayerState;
import com.cyberheist.entity.StaminaSystem;
import com.cyberheist.input.KeyboardInput;
import com.cyberheist.interact.Interactable;
import com.cyberheist.interact.TestTerminal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PlayerSystemTest {

    private Player player;
    private CollisionWorld world;
    private KeyboardInput keyboard;
    private List<Interactable> interactables;

    @BeforeEach
    void setUp() {
        world = new CollisionWorld(1280, 720);
        player = new Player(100, 100);
        keyboard = new KeyboardInput();
        interactables = new ArrayList<>();
    }

    @Test
    void testInitialState() {
        assertEquals(PlayerState.IDLE, player.getState());
        assertEquals(100.0, player.getX(), 0.01);
        assertEquals(100.0, player.getY(), 0.01);
        assertEquals(0.0, player.getCurrentSpeed(), 0.01);
        assertEquals(100.0, player.getStaminaSystem().getCurrentStamina(), 0.01);
        assertFalse(player.getStaminaSystem().isExhausted());
    }

    @Test
    void testFacingDirectionFromVector() {
        assertEquals(FacingDirection.EAST, FacingDirection.fromVector(1.0, 0.0));
        assertEquals(FacingDirection.WEST, FacingDirection.fromVector(-1.0, 0.0));
        assertEquals(FacingDirection.NORTH, FacingDirection.fromVector(0.0, -1.0));
        assertEquals(FacingDirection.SOUTH, FacingDirection.fromVector(0.0, 1.0));
        assertEquals(FacingDirection.SOUTH_EAST, FacingDirection.fromVector(1.0, 1.0));
        assertEquals(FacingDirection.NORTH_WEST, FacingDirection.fromVector(-1.0, -1.0));
    }

    @Test
    void testMovementAndAcceleration() {
        // Press D (move right)
        keyboard.keyPressed(new KeyEvent(new java.awt.Component(){}, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_D, 'D'));
        keyboard.poll();

        double dt = 0.05;
        player.update(dt, keyboard, world, interactables);

        // Player should have positive velocityX and moved right
        assertTrue(player.getVelocityX() > 0.0, "VelocityX should be positive after pressing D");
        assertTrue(player.getX() > 100.0, "Player should have moved to the right");
        assertEquals(PlayerState.WALKING, player.getState());
    }

    @Test
    void testDecelerationWhenInputReleased() {
        // Move right first
        keyboard.keyPressed(new KeyEvent(new java.awt.Component(){}, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_D, 'D'));
        keyboard.poll();
        player.update(0.1, keyboard, world, interactables);
        double initialVx = player.getVelocityX();
        assertTrue(initialVx > 0.0);

        // Release D
        keyboard.keyReleased(new KeyEvent(new java.awt.Component(){}, KeyEvent.KEY_RELEASED, 0, 0, KeyEvent.VK_D, 'D'));
        keyboard.poll();

        // Update several frames to allow deceleration
        for (int i = 0; i < 10; i++) {
            player.update(0.05, keyboard, world, interactables);
        }

        assertEquals(0.0, player.getVelocityX(), 0.01, "Player should have decelerated to 0");
        assertEquals(PlayerState.IDLE, player.getState());
    }

    @Test
    void testSprintAndStaminaConsumption() {
        // Hold D and Shift
        keyboard.keyPressed(new KeyEvent(new java.awt.Component(){}, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_D, 'D'));
        keyboard.keyPressed(new KeyEvent(new java.awt.Component(){}, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_SHIFT, KeyEvent.CHAR_UNDEFINED));
        keyboard.poll();

        // Update 1 second
        for (int i = 0; i < 20; i++) {
            player.update(0.05, keyboard, world, interactables);
        }

        assertEquals(PlayerState.RUNNING, player.getState());
        assertTrue(player.getCurrentSpeed() > player.getWalkSpeed(), "Sprint speed should exceed walk speed");
        assertTrue(player.getStaminaSystem().getCurrentStamina() < 100.0, "Stamina should be drained during sprint");
    }

    @Test
    void testStaminaExhaustionAndRecovery() {
        StaminaSystem stamina = player.getStaminaSystem();

        // Drain until exhausted
        while (stamina.getCurrentStamina() > 0.0) {
            stamina.update(0.1, true);
        }

        assertTrue(stamina.isExhausted(), "Stamina should be exhausted when hitting 0");
        assertFalse(stamina.canSprint(), "Player should not be able to sprint while exhausted");

        // Wait out cooldown delay and recover past threshold
        for (int i = 0; i < 30; i++) {
            stamina.update(0.1, false);
        }

        assertFalse(stamina.isExhausted(), "Exhaustion should clear after recovering above threshold");
        assertTrue(stamina.canSprint(), "Player should be able to sprint again");
    }

    @Test
    void testCollisionStopsMovement() {
        // Place an obstacle right next to player (at x=130, y=100, width=50, height=50)
        Obstacle wall = new Obstacle("WALL", 130, 100, 50, 50);
        world.add(wall);

        // Move right continuously toward the wall
        keyboard.keyPressed(new KeyEvent(new java.awt.Component(){}, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_D, 'D'));
        keyboard.poll();

        for (int i = 0; i < 40; i++) {
            player.update(0.05, keyboard, world, interactables);
        }

        // Player's right edge (x + width) must not penetrate wall at x=130
        assertTrue(player.getX() + player.getBoundingBox().getWidth() <= 130.01,
                "Player should be stopped by the obstacle");
    }

    @Test
    void testWallSlidingPhysics() {
        // Horizontal wall below player (y=130, width=500, height=30)
        Obstacle wall = new Obstacle("FLOOR_WALL", 50, 130, 500, 30);
        world.add(wall);

        // Walk diagonally down-right (S + D)
        keyboard.keyPressed(new KeyEvent(new java.awt.Component(){}, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_S, 'S'));
        keyboard.keyPressed(new KeyEvent(new java.awt.Component(){}, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_D, 'D'));
        keyboard.poll();

        double startX = player.getX();
        for (int i = 0; i < 20; i++) {
            player.update(0.05, keyboard, world, interactables);
        }

        // Y movement should be blocked by wall, but X movement should continue smoothly (sliding)
        assertTrue(player.getY() + player.getBoundingBox().getHeight() <= 130.01, "Y should be blocked by wall");
        assertTrue(player.getX() > startX + 50.0, "X should slide along the wall");
    }

    @Test
    void testInteractionSystem() {
        TestTerminal terminal = new TestTerminal("TERM1", 140, 100, 48, 48);
        interactables.add(terminal);

        assertFalse(terminal.isAccessed(), "Terminal initially not accessed");

        // Player moves into interaction trigger zone
        keyboard.keyPressed(new KeyEvent(new java.awt.Component(){}, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_D, 'D'));
        keyboard.poll();
        player.update(0.1, keyboard, world, interactables);

        assertNotNull(player.getActiveInteractable(), "Player should detect nearby interactable");

        // Press 'E' to interact
        keyboard.keyPressed(new KeyEvent(new java.awt.Component(){}, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_E, 'E'));
        keyboard.poll();
        player.update(0.05, keyboard, world, interactables);

        assertTrue(terminal.isAccessed(), "Terminal should be accessed after pressing E");
        assertEquals(PlayerState.INTERACTING, player.getState(), "Player should be in INTERACTING state");
    }

    @Test
    void testStunStateImmobilizesPlayer() {
        player.applyStun(1.0);
        assertEquals(PlayerState.STUNNED, player.getState());
        assertFalse(player.getState().canMove());

        // Try to move while stunned
        keyboard.keyPressed(new KeyEvent(new java.awt.Component(){}, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_W, 'W'));
        keyboard.poll();
        player.update(0.2, keyboard, world, interactables);

        assertEquals(100.0, player.getY(), 0.01, "Player should not move while stunned");

        // After stun duration expires, player returns to IDLE
        player.update(0.9, keyboard, world, interactables);
        assertEquals(PlayerState.IDLE, player.getState(), "Player should recover to IDLE after stun timer expires");
    }
}
