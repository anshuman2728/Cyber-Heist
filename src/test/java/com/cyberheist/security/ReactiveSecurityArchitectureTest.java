package com.cyberheist.security;

import com.cyberheist.ai.GuardAI;
import com.cyberheist.ai.pathfinding.AStarPathfinder;
import com.cyberheist.ai.pathfinding.NavGrid;
import com.cyberheist.ai.state.GuardState;
import com.cyberheist.collision.CollisionWorld;
import com.cyberheist.collision.Obstacle;
import com.cyberheist.entity.Player;
import com.cyberheist.stealth.Guard;
import com.cyberheist.stealth.NoiseType;
import com.cyberheist.stealth.StealthSystem;
import com.cyberheist.world.AlarmPanel;
import com.cyberheist.world.Door;
import com.cyberheist.world.SecurityCamera;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates the Reactive Security Architecture:
 * SecurityDirector, dynamic camera detection, guard disturbance investigation,
 * facility lockdown blast doors, and the five tactical gameplay approaches.
 */
public class ReactiveSecurityArchitectureTest {

    private SecurityDirector director;
    private CollisionWorld collisionWorld;
    private NavGrid navGrid;
    private AStarPathfinder pathfinder;
    private StealthSystem stealthSystem;

    @BeforeEach
    void setUp() {
        director = new SecurityDirector();
        collisionWorld = new CollisionWorld(1200, 1200);
        navGrid = new NavGrid(1200, 1200, 20);
        navGrid.updateWalkability(collisionWorld, 14.0);
        pathfinder = new AStarPathfinder(navGrid);
        stealthSystem = new StealthSystem();
    }

    @Test
    @DisplayName("Approach A: Sneak through camera blind spots keeps facility in NORMAL security")
    void testCameraBlindSpotStealth() {
        Player player = new Player(300, 300);
        // Camera pointing south (90 deg), range 150px
        SecurityCamera camera = new SecurityCamera("TEST-CAM", 300, 100, 90, 60, 150, 45);

        // Solid obstacle between camera and player
        Obstacle wall = new Obstacle("WALL", 250, 180, 100, 20, Color.BLACK, Color.GRAY);
        collisionWorld.add(wall);

        // Update camera for multiple frames
        for (int i = 0; i < 20; i++) {
            camera.update(0.1, player, collisionWorld, director, Collections.emptyList());
        }

        // Because line of sight is obstructed, camera has zero detection
        assertEquals(0.0, camera.getDetectionProgress(), 0.001);
        assertEquals(SecurityLevel.NORMAL, director.getCurrentLevel());
    }

    @Test
    @DisplayName("Approach B: Hacking cameras triggers guard investigation and systemic suspicion")
    void testCameraDisablingTriggersInvestigationAndSuspicion() {
        Guard guard = new Guard("GUARD-PATROL", 300, 200, 0);
        GuardAI ai = new GuardAI(guard, pathfinder);
        guard.setAI(ai);

        List<Guard> guards = List.of(guard);

        SecurityCamera cam1 = new SecurityCamera("CAM-01", 320, 210, 0, 45, 150, 45);
        cam1.setCameraStateListener(c -> director.onCameraDisabled(c, guards));

        SecurityCamera cam2 = new SecurityCamera("CAM-02", 500, 210, 0, 45, 150, 45);
        cam2.setCameraStateListener(c -> director.onCameraDisabled(c, guards));

        // 1. Disable first camera
        cam1.onHackSuccess();
        assertTrue(cam1.isDisabled());
        assertEquals(1, director.getDisabledCamerasCount());

        // Guard should be assigned to investigate the offline junction
        assertEquals(GuardState.INVESTIGATE, ai.getState());
        assertNotNull(ai.getInvestigationTarget());

        // 2. Disable second camera -> elevates facility security to SUSPICIOUS
        cam2.onHackSuccess();
        assertTrue(cam2.isDisabled());
        assertEquals(2, director.getDisabledCamerasCount());
        assertEquals(SecurityLevel.SUSPICIOUS, director.getCurrentLevel());
    }

    @Test
    @DisplayName("Approach C: Distraction pulls guard from post to investigate noise source")
    void testDistractionPullsGuard() {
        Guard guard = new Guard("GUARD-POST", 400, 400, 0);
        GuardAI ai = new GuardAI(guard, pathfinder);
        guard.setAI(ai);
        stealthSystem.addGuard(guard);

        // Emit distraction sound at (200, 400)
        stealthSystem.emitNoise(200, 400, NoiseType.RUNNING);
        stealthSystem.update(0.05, new Player(600, 600), collisionWorld, Collections.emptyList());

        // Guard transitions from PATROL to SUSPICIOUS / INVESTIGATE toward noise
        assertTrue(ai.getState() == GuardState.SUSPICIOUS || ai.getState() == GuardState.INVESTIGATE);
        assertNotNull(ai.getInvestigationTarget());
        // Target is localized near the noise coordinates
        assertTrue(Math.abs(ai.getInvestigationTarget().x - 200.0) < 60.0);
    }

    @Test
    @DisplayName("Approach D: Tripping alarm engages facility LOCKDOWN and seals blast doors")
    void testAlarmTriggersLockdownAndSealsDoors() {
        Door blastDoor = new Door("Server Blast Door", Door.DoorType.SECURITY_BADGE, 500, 300, 80, 20, true);
        blastDoor.setLockdownProtected(true);
        blastDoor.setOpen(true); // Initially open
        assertTrue(blastDoor.isOpen());
        assertFalse(blastDoor.isLocked());

        Door alternateTurnstile = new Door("Turnstile Gate", Door.DoorType.SECURITY_BADGE, 200, 300, 20, 80, false);
        alternateTurnstile.setLockdownProtected(false); // Alternate route is not blast-sealed

        List<Door> doors = List.of(blastDoor, alternateTurnstile);

        AlarmPanel alarm = new AlarmPanel("PANEL-01", 600, 200, 20, 30);
        alarm.setAlarmStateListener(new AlarmPanel.AlarmStateListener() {
            @Override
            public void onAlarmTriggered(AlarmPanel panel) {
                director.onAlarmTriggered(panel, Collections.emptyList());
            }

            @Override
            public void onAlarmDisarmed(AlarmPanel panel) {
                director.onAlarmDisarmed(panel);
            }
        });

        // Trigger alarm siren (e.g. failed sabotage or intentional alarm pull)
        alarm.triggerAlarm();

        assertEquals(SecurityLevel.LOCKDOWN, director.getCurrentLevel());

        // When director is in LOCKDOWN, lockdown doors engage
        for (Door d : doors) {
            if (d.isLockdownProtected()) {
                d.engageLockdown(true);
            }
        }

        // Primary blast door is now closed, locked down, and requires HARD override
        assertFalse(blastDoor.isOpen());
        assertTrue(blastDoor.isLocked());
        assertTrue(blastDoor.isLockedDown());

        // Alternate turnstile route remains available (not locked down)
        assertFalse(alternateTurnstile.isLockedDown());

        // Disarming alarm allows lockdown to release
        alarm.onHackSuccess();
        assertFalse(alarm.isActiveAlarm());
        assertEquals(SecurityLevel.ALERT, director.getCurrentLevel());

        for (Door d : doors) {
            if (d.isLockdownProtected() && d.isLockedDown()) {
                d.engageLockdown(false);
            }
        }
        assertFalse(blastDoor.isLockedDown());
    }

    @Test
    @DisplayName("Approach E: Guard notices unconscious colleague and triggers ALERT")
    void testGuardNoticesDownedColleague() {
        Guard guardA = new Guard("GUARD-PATROLLER", 200, 200, 0); // Facing east (0 rad)
        GuardAI aiA = new GuardAI(guardA, pathfinder);
        guardA.setAI(aiA);

        Guard guardB = new Guard("GUARD-VICTIM", 320, 200, 0);
        guardB.knockOut();
        assertTrue(guardB.isUnconscious());

        List<Guard> allGuards = List.of(guardA, guardB);

        // Guard A performs environmental scan seeing downed Guard B
        aiA.update(0.25, null, null, director, collisionWorld, allGuards,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        // Guard A should transition to ALERT and escalate director to ALERT
        assertEquals(GuardState.ALERT, aiA.getState());
        assertEquals(SecurityLevel.ALERT, director.getCurrentLevel());
        assertEquals(1, director.getDiscoveredBodiesCount());
    }

    @Test
    @DisplayName("Environmental Reaction: Guard notices open security door and closes it")
    void testGuardNoticesAndClosesSecurityDoor() {
        Guard guard = new Guard("GUARD-DOOR-INSPECTOR", 200, 200, 0); // Facing east toward door
        GuardAI ai = new GuardAI(guard, pathfinder);
        guard.setAI(ai);

        Door secureDoor = new Door("Office Access Door", Door.DoorType.SECURITY_BADGE, 260, 195, 20, 50, false);
        secureDoor.setOpen(true); // Unsecured open door

        List<Door> doors = List.of(secureDoor);

        // Guard notices the open door
        ai.update(0.25, null, null, director, collisionWorld, List.of(guard),
                Collections.emptyList(), doors, Collections.emptyList());

        // Guard transitions to INVESTIGATE and director notes disturbance
        assertEquals(GuardState.INVESTIGATE, ai.getState());
        assertEquals(SecurityLevel.SUSPICIOUS, director.getCurrentLevel());

        // Guard arrives at the door and secures it
        guard.setPosition(250, 195);
        ai.update(0.1, null, null, director, collisionWorld, List.of(guard),
                Collections.emptyList(), doors, Collections.emptyList());

        // Door is now closed and secured!
        assertFalse(secureDoor.isOpen());
    }
}
