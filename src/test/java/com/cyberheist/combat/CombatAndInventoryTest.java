package com.cyberheist.combat;

import com.cyberheist.entity.FacingDirection;
import com.cyberheist.entity.Player;
import com.cyberheist.inventory.Inventory;
import com.cyberheist.inventory.ItemType;
import com.cyberheist.progression.ProgressionManager;
import com.cyberheist.stealth.Guard;
import com.cyberheist.stealth.NoiseType;
import com.cyberheist.stealth.StealthSystem;
import com.cyberheist.world.Door;
import com.cyberheist.world.Safe;
import com.cyberheist.world.SecurityCamera;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Limited Tactical Combat & Inventory Tests")
public class CombatAndInventoryTest {

    private Inventory inventory;
    private ProgressionManager progression;
    private CombatManager combat;
    private StealthSystem stealth;
    private Player player;
    private Guard guard;

    @BeforeEach
    void setUp() {
        this.inventory = Inventory.createStartingLoadout();
        this.progression = new ProgressionManager();
        this.combat = new CombatManager();
        this.stealth = new StealthSystem();
        this.player = new Player(200.0, 200.0);
        this.guard = new Guard("GUARD-01", 200.0, 240.0, 90.0); // Facing South
        this.stealth.addGuard(guard);
    }

    @Test
    @DisplayName("Starting Inventory contains exactly EMP x2, Lockpick x3, Medkit x1, Data Drive x1")
    void testStartingLoadout() {
        assertEquals(2, inventory.getCount(ItemType.EMP), "Must start with 2 EMPs");
        assertEquals(3, inventory.getCount(ItemType.LOCKPICK), "Must start with 3 Lockpicks");
        assertEquals(1, inventory.getCount(ItemType.MEDKIT), "Must start with 1 Medkit");
        assertEquals(1, inventory.getCount(ItemType.DATA_DRIVE), "Must start with 1 Data Drive");
        assertFalse(inventory.isDataPayloadSecured(), "Data payload should not be secured initially");
    }

    @Test
    @DisplayName("Stun Device shocks guard in facing arc and applies incapacitation")
    void testStunDeviceArc() {
        // Player at (200, 200) facing SOUTH toward Guard at (200, 240)
        List<Guard> guards = List.of(guard);

        boolean fired = combat.fireStunDevice(player, guards, stealth, progression);
        assertTrue(fired, "Stun device should fire successfully");
        assertTrue(guard.isStunned(), "Guard should be stunned");
        assertTrue(guard.isIncapacitated(), "Guard should be marked incapacitated");
        assertTrue(combat.getStunCooldownTimer() > 0.0, "Cooldown must be triggered");

        // Stunned guard cannot detect player
        guard.evaluatePlayerVisibility(player.getX(), player.getY(), 0.0, false, 1.0, true, null, 0.1);
        assertFalse(guard.isPlayerInVision(), "Stunned guard cannot see player");
    }

    @Test
    @DisplayName("EMP Canister detonates, consumes 1 EMP, stuns guards and disables cameras in radius")
    void testEmpBlast() {
        List<Guard> guards = List.of(guard);
        SecurityCamera camera = new SecurityCamera("CAM-TEST", 220.0, 220.0, 0.0, 30.0, 150.0, 60.0);
        List<SecurityCamera> cameras = List.of(camera);

        assertEquals(2, inventory.getCount(ItemType.EMP));
        boolean empSuccess = combat.deployEmp(player, inventory, guards, cameras, stealth, progression);

        assertTrue(empSuccess, "EMP deployment should succeed");
        assertEquals(1, inventory.getCount(ItemType.EMP), "1 EMP must be consumed");
        assertTrue(guard.isStunned(), "Guard in EMP blast must be stunned");
        assertTrue(camera.isDisabled(), "Camera in EMP blast must be disabled");
    }

    @Test
    @DisplayName("Throwable Distraction travels to target, emits acoustic sound wave and lures guards")
    void testThrowableDistraction() {
        double targetX = 350.0;
        double targetY = 200.0;

        boolean thrown = combat.throwDistraction(player, targetX, targetY, stealth, progression);
        assertTrue(thrown, "Distraction throw should succeed");
        assertTrue(combat.getDistractionCooldownTimer() > 0.0, "Distraction cooldown must be active");

        // Advance time until projectile lands (~0.35s)
        combat.update(0.40, player, List.of(guard), stealth);

        // Acoustic sound wave should be active in stealth system
        assertFalse(stealth.getNoiseManager().getActiveNoises().isEmpty(),
                "Acoustic noise wave must be emitted upon decoy landing");
    }

    @Test
    @DisplayName("Non-lethal takedown knocks out guard when approaching behind back")
    void testNonLethalTakedownFromBehind() {
        // Guard at (200, 200) facing SOUTH (90 degrees).
        // Player is at (200, 160) - directly behind the guard!
        Guard targetGuard = new Guard("PATROL-GUARD", 200.0, 200.0, 90.0);
        Player sneakingPlayer = new Player(200.0, 160.0);

        List<Guard> guards = List.of(targetGuard);
        combat.update(0.01, sneakingPlayer, guards, stealth);

        Guard takedownCandidate = combat.getActiveTakedownTarget();
        assertNotNull(takedownCandidate, "Guard should be eligible for takedown from behind");

        int initialXp = progression.getTotalXp();
        boolean executed = combat.executeTakedown(sneakingPlayer, progression);

        assertTrue(executed, "Stealth takedown must succeed");
        assertTrue(targetGuard.isUnconscious(), "Guard must be knocked out / unconscious");
        assertTrue(targetGuard.isIncapacitated(), "Knocked out guard must be incapacitated");
        assertEquals(initialXp + 40, progression.getTotalXp(), "Takedown must award +40 XP");
    }

    @Test
    @DisplayName("Lockpick silently unlocks locked doors and safes without triggering alarms")
    void testLockpickUsage() {
        Door door = new Door("TEST-DOOR", Door.DoorType.SECURITY_BADGE, 100, 100, 40, 40, true);
        door.setLocked(true, "KEYCARD");
        assertTrue(door.isLocked(), "Door should initially be locked");

        boolean picked = door.unlockWithLockpick();
        assertTrue(picked, "Lockpick should unlock the door");
        assertFalse(door.isLocked(), "Door must no longer be locked");
        assertTrue(door.isOpen(), "Door should be opened");

        Safe safe = new Safe("VAULT-SAFE", 300, 300, 40, 40);
        assertFalse(safe.isOpened(), "Safe should initially be locked");

        boolean safePicked = safe.unlockWithLockpick();
        assertTrue(safePicked, "Lockpick should pick the safe tumblers");
        assertTrue(safe.isOpened(), "Safe must now be open");
    }

    @Test
    @DisplayName("Medkit restores exhausted stamina back to 100%")
    void testMedkitRestoresStamina() {
        // Drain stamina completely
        player.getStaminaSystem().update(4.0, true);
        assertTrue(player.getStaminaSystem().isExhausted(), "Player should be exhausted");
        assertEquals(0.0, player.getStaminaSystem().getCurrentStamina(), 0.01);

        // Use medkit
        boolean consumed = inventory.consumeItem(ItemType.MEDKIT, 1);
        assertTrue(consumed, "Medkit should be consumed");
        assertEquals(0, inventory.getCount(ItemType.MEDKIT));

        player.getStaminaSystem().restoreFull();
        assertFalse(player.getStaminaSystem().isExhausted(), "Exhaustion must be cleared");
        assertEquals(100.0, player.getStaminaSystem().getCurrentStamina(), 0.01);
    }
}
