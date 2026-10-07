package com.cyberheist.world;

import com.cyberheist.collision.BoundingBox;
import com.cyberheist.hacking.Hackable;
import com.cyberheist.hacking.HackingDifficulty;
import com.cyberheist.interact.Interactable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Headless test suite verifying the 8 core interaction entities:
 * 1. Computer (ComputerWorkstation)
 * 2. Camera (SecurityCamera)
 * 3. Door (Door)
 * 4. Terminal (SecurityTerminal)
 * 5. Alarm (AlarmPanel)
 * 6. Server (ServerRack)
 * 7. Elevator (Elevator)
 * 8. Safe (Safe)
 * and their integration into NoidaDataCenterLevel.
 */
public class WorldInteractionsTest {

    private NoidaDataCenterLevel level;

    @BeforeEach
    public void setUp() {
        level = new NoidaDataCenterLevel();
    }

    @Test
    @DisplayName("Level contains all 8 required interaction object types registered in interactables")
    public void testAllEightInteractionsRegisteredInLevel() {
        List<Interactable> interactables = level.getInteractables();
        assertNotNull(interactables);
        assertFalse(interactables.isEmpty());

        boolean hasComputer = false;
        boolean hasCamera = false;
        boolean hasDoor = false;
        boolean hasTerminal = false;
        boolean hasAlarm = false;
        boolean hasServer = false;
        boolean hasElevator = false;
        boolean hasSafe = false;

        for (Interactable item : interactables) {
            if (item instanceof ComputerWorkstation) hasComputer = true;
            if (item instanceof SecurityCamera) hasCamera = true;
            if (item instanceof Door) hasDoor = true;
            if (item instanceof SecurityTerminal) hasTerminal = true;
            if (item instanceof AlarmPanel) hasAlarm = true;
            if (item instanceof ServerRack) hasServer = true;
            if (item instanceof Elevator) hasElevator = true;
            if (item instanceof Safe) hasSafe = true;
        }

        assertTrue(hasComputer, "Level must contain Computer interactions");
        assertTrue(hasCamera, "Level must contain Camera interactions");
        assertTrue(hasDoor, "Level must contain Door interactions");
        assertTrue(hasTerminal, "Level must contain Terminal interactions");
        assertTrue(hasAlarm, "Level must contain Alarm interactions");
        assertTrue(hasServer, "Level must contain Server interactions");
        assertTrue(hasElevator, "Level must contain Elevator interactions");
        assertTrue(hasSafe, "Level must contain Safe interactions");
    }

    @Test
    @DisplayName("1. Computer Workstation behaves as an interactive hackable device")
    public void testComputerWorkstation() {
        ComputerWorkstation pc = new ComputerWorkstation("OFFICE-PC",
                ComputerWorkstation.WorkstationType.OFFICE_PC, 100, 100, 40, 30);

        assertTrue(pc.canInteract());
        assertTrue(pc.canBeHacked());
        assertFalse(pc.isHacked());
        assertEquals(HackingDifficulty.EASY, pc.getDifficulty());

        pc.onHackSuccess();
        assertTrue(pc.isHacked());
        assertFalse(pc.canBeHacked());
        assertTrue(pc.isAccessed());

        // Subsequent interact toggles file view
        pc.onInteract();
        assertFalse(pc.isAccessed());
    }

    @Test
    @DisplayName("2. Security Camera can be hacked to loop and disable surveillance cone")
    public void testSecurityCamera() {
        SecurityCamera cam = new SecurityCamera("CAM-01", 100, 100, 45, 60, 180, 50);

        assertTrue(cam.canInteract());
        assertFalse(cam.isDisabled());
        assertTrue(cam.canBeHacked());
        assertEquals(HackingDifficulty.MEDIUM, cam.getDifficulty());

        cam.onHackSuccess();
        assertTrue(cam.isHacked());
        assertTrue(cam.isDisabled());
        assertFalse(cam.canInteract());
        assertFalse(cam.canBeHacked());
    }

    @Test
    @DisplayName("3. Door distinguishes unlocked toggle from locked security hacking")
    public void testDoorLockAndHacking() {
        // Unlocked door
        Door normalDoor = new Door("Office Door", Door.DoorType.SECURITY_BADGE, 200, 200, 80, 16, true);
        assertFalse(normalDoor.canBeHacked());
        assertFalse(normalDoor.isOpen());
        normalDoor.onInteract();
        assertTrue(normalDoor.isOpen());

        // Locked door
        Door lockedDoor = new Door("Vault Gate", Door.DoorType.HEAVY_VAULT, 300, 300, 80, 16, true);
        lockedDoor.setLocked(true, null);
        assertTrue(lockedDoor.canBeHacked());
        assertEquals(HackingDifficulty.HARD, lockedDoor.getDifficulty());

        // Interacting while locked does not open
        lockedDoor.onInteract();
        assertFalse(lockedDoor.isOpen());

        // Hacking unlocks and opens door
        lockedDoor.onHackSuccess();
        assertTrue(lockedDoor.isHacked());
        assertFalse(lockedDoor.canBeHacked());
        assertTrue(lockedDoor.isOpen());
    }

    @Test
    @DisplayName("4. Security Terminal override triggers camera loop callback")
    public void testSecurityTerminal() {
        SecurityTerminal terminal = new SecurityTerminal("SEC-CONSOLE", 400, 400, 48, 36);
        assertEquals(HackingDifficulty.HARD, terminal.getDifficulty());
        assertTrue(terminal.canBeHacked());

        boolean[] overrideFired = {false};
        terminal.setOverrideCallback(() -> overrideFired[0] = true);

        terminal.onHackSuccess();
        assertTrue(terminal.isHacked());
        assertTrue(overrideFired[0]);
    }

    @Test
    @DisplayName("5. Alarm Panel can be sabotaged; failure triggers sector alert")
    public void testAlarmPanel() {
        AlarmPanel alarm = new AlarmPanel("ALARM-01", 500, 500, 16, 28);
        assertEquals(HackingDifficulty.MEDIUM, alarm.getDifficulty());
        assertFalse(alarm.isDisarmed());
        assertFalse(alarm.isActiveAlarm());

        // Failed hack triggers alarm
        alarm.onHackFailure();
        assertTrue(alarm.isActiveAlarm());
        assertFalse(alarm.isDisarmed());

        // Successful hack disarms
        alarm.onHackSuccess();
        assertTrue(alarm.isDisarmed());
        assertFalse(alarm.isActiveAlarm());
    }

    @Test
    @DisplayName("6. Server Rack yields classified data payload upon breach")
    public void testServerRack() {
        ServerRack rack = new ServerRack("RACK-01", 600, 600, 44, 34);
        assertEquals(HackingDifficulty.HARD, rack.getDifficulty());
        assertFalse(rack.isHacked());
        assertEquals(250000, rack.getDataValueCredits());

        rack.onHackSuccess();
        assertTrue(rack.isHacked());
        assertFalse(rack.canInteract());
    }

    @Test
    @DisplayName("7. Elevator panel bypass calls extraction elevator and opens shaft bounds")
    public void testElevator() {
        Elevator elev = new Elevator("EXTRACTION-LIFT", 700, 700, 72, 54);
        assertEquals(HackingDifficulty.MEDIUM, elev.getDifficulty());
        assertFalse(elev.isCalled());
        assertFalse(elev.isHacked());

        // Collision bounds block player before doors open
        assertNotEquals(0, elev.getBoundingBox().getWidth());

        elev.onHackSuccess();
        assertTrue(elev.isHacked());
        assertTrue(elev.isCalled());

        // Update animation until doors open
        for (int i = 0; i < 10; i++) {
            elev.update(0.1);
        }
        // Collision opens up when doors are retracted
        assertEquals(0, elev.getBoundingBox().getWidth());
    }

    @Test
    @DisplayName("8. Safe cracked via Tier-IV cipher allows bearer bond exfiltration")
    public void testSafe() {
        Safe safe = new Safe("VAULT-SAFE", 800, 800, 44, 36);
        assertEquals(HackingDifficulty.VERY_HARD, safe.getDifficulty());
        assertFalse(safe.isOpened());
        assertFalse(safe.isLooted());
        assertEquals(500000, safe.getLootValueCredits());

        // Cannot loot before opening
        safe.onInteract();
        assertFalse(safe.isLooted());

        safe.onHackSuccess();
        assertTrue(safe.isOpened());
        assertTrue(safe.isHacked());

        // Now lootable
        safe.onInteract();
        assertTrue(safe.isLooted());
        assertFalse(safe.canInteract());
    }

    @Test
    @DisplayName("Level 1 master terminal overrides all level surveillance cameras")
    public void testLevelTerminalOverridesCameras() {
        List<SecurityCamera> cams = level.getCameras();
        List<SecurityTerminal> terms = level.getTerminals();

        assertFalse(cams.isEmpty());
        assertFalse(terms.isEmpty());

        for (SecurityCamera cam : cams) {
            assertFalse(cam.isDisabled());
        }

        SecurityTerminal terminal = terms.get(0);
        terminal.onHackSuccess();

        for (SecurityCamera cam : cams) {
            assertTrue(cam.isDisabled());
        }
    }
}
