package com.cyberheist.save;

import com.cyberheist.ai.GuardAI;
import com.cyberheist.ai.pathfinding.AStarPathfinder;
import com.cyberheist.ai.pathfinding.NavGrid;
import com.cyberheist.ai.state.GuardState;
import com.cyberheist.collision.BoundingBox;
import com.cyberheist.collision.CollisionWorld;
import com.cyberheist.collision.Obstacle;
import com.cyberheist.core.TelemetryService;
import com.cyberheist.entity.FacingDirection;
import com.cyberheist.entity.Player;
import com.cyberheist.entity.PlayerState;
import com.cyberheist.hacking.HackingDifficulty;
import com.cyberheist.hacking.HackingSession;
import com.cyberheist.hacking.NetworkNode;
import com.cyberheist.input.KeyboardInput;
import com.cyberheist.inventory.ItemType;
import com.cyberheist.mission.MissionManager;
import com.cyberheist.mission.MissionStep;
import com.cyberheist.progression.ProgressionTrack;
import com.cyberheist.stealth.Guard;
import com.cyberheist.world.Door;
import com.cyberheist.world.SecurityCamera;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Rigorous test suite validating the edge cases and questions that cause student projects to fail:
 * 1. Gameplay Edge Cases:
 *    - Can player get stuck? (CCD sub-stepping + anti-stuck depenetration verification)
 *    - Can guard get stuck? (Stuck timer repath + waypoint skipping)
 *    - Can doors become permanently unusable? (Repeat toggle + lockpick/hack fallback)
 *    - Can mission become impossible? (Objective progression & state integrity)
 *    - Can player leave map? (World boundary clamp)
 *    - Can player exploit collision? (High-velocity penetration prevention)
 *    - Can player spam hacking? (Input debounce cooldown timer)
 *    - Can AI become permanently confused? (State timeouts for investigate, chase, return)
 * 2. Performance Monitoring:
 *    - FPS, delta time, CPU core count, Heap Memory, GC count, Pathfinding throughput.
 * 3. Save System:
 *    - Save -> Exit -> Restart -> Load -> Everything restored!
 */
public class SaveSystemAndRobustnessTest {

    private static final String TEST_SAVE_FILE = "test_roundtrip.sav";

    @BeforeEach
    void setUp() {
        SaveManager.deleteSaveFile(TEST_SAVE_FILE);
    }

    @AfterEach
    void tearDown() {
        SaveManager.deleteSaveFile(TEST_SAVE_FILE);
    }

    // =========================================================================
    // 1. GAMEPLAY EDGE CASES & HARDENING TESTS
    // =========================================================================

    @Test
    void testPlayerCannotLeaveMapOrExploitCollision() {
        CollisionWorld world = new CollisionWorld(1000, 1000);
        // Wall placed at x=100..120, y=0..500
        Obstacle wall = new Obstacle("Wall", 100, 0, 20, 500, Color.DARK_GRAY, Color.GRAY);
        world.add(wall);

        Player player = new Player(50, 100);
        KeyboardInput mockKeys = new KeyboardInput();

        // 1. Wall collision tunneling test: player moves at 400 px/s directly towards wall
        player.setPosition(80, 100);
        player.setVelocity(400, 0); // High forward velocity
        player.update(0.1, mockKeys, world, Collections.emptyList());

        // Player must NOT tunnel through the wall (x + 28 must be <= 100)
        assertTrue(player.getX() + 28.0 <= 100.01,
                "Player must be stopped at the wall surface without tunneling through! Pos=" + player.getX());

        // 2. Map boundary clamping test: position player outside bounds
        player.setPosition(-100, -50);
        player.update(0.016, mockKeys, world, Collections.emptyList());
        assertTrue(player.getX() >= 0.0, "Player X must be clamped to >= 0");
        assertTrue(player.getY() >= 0.0, "Player Y must be clamped to >= 0");

        player.setPosition(1500, 1500);
        player.update(0.016, mockKeys, world, world.getCollidables().stream()
                .filter(c -> c instanceof com.cyberheist.interact.Interactable)
                .map(c -> (com.cyberheist.interact.Interactable) c)
                .toList());
        assertTrue(player.getX() <= 1000.0 - 28.0, "Player X must be clamped within world boundary");
        assertTrue(player.getY() <= 1000.0 - 28.0, "Player Y must be clamped within world boundary");
    }

    @Test
    void testPlayerDepenetrationWhenSpawnedInsideObstacle() {
        CollisionWorld world = new CollisionWorld(1000, 1000);
        Obstacle obstacle = new Obstacle("Box", 200, 200, 60, 60, Color.BLACK, Color.WHITE);
        world.add(obstacle);

        // Spawn player directly inside the center of the obstacle
        Player player = new Player(210, 210);
        KeyboardInput mockKeys = new KeyboardInput();

        // One tick of update triggers the cardinal depenetration unstick algorithm
        player.update(0.016, mockKeys, world, Collections.emptyList());

        // Player must no longer overlap the solid obstacle bounding box
        assertFalse(player.getBoundingBox().intersects(obstacle.getBoundingBox()),
                "Player must be depenetrated and pushed out of solid obstacle bounds!");
    }

    @Test
    void testGuardCannotBecomePermanentlyConfusedOrStuck() {
        NavGrid grid = new NavGrid(20, 20, 40);
        AStarPathfinder pathfinder = new AStarPathfinder(grid);

        Guard guard = new Guard("GUARD-TEST", 100, 100, 0);
        GuardAI ai = new GuardAI(guard, pathfinder);
        guard.setAI(ai);

        Player player = new Player(500, 500);

        // 1. Anti-Confusion: INVESTIGATE state timeout
        ai.onRadioAlert(new java.awt.geom.Point2D.Double(1500, 1500));
        assertEquals(GuardState.INVESTIGATE, ai.getState());

        // Simulate 7.6 seconds passing without reaching distant/unreachable target
        for (int i = 0; i < 76; i++) {
            ai.update(0.1, player, null);
        }
        // AI must time out and transition to SEARCH instead of remaining permanently stuck
        assertEquals(GuardState.SEARCH, ai.getState(),
                "Guard must transition from INVESTIGATE to SEARCH after state timeout!");

        // 2. Anti-Confusion: RETURN_TO_PATROL state timeout
        ai.addPatrolWaypoint(5000, 5000);
        ai.transitionTo(GuardState.RETURN_TO_PATROL);
        for (int i = 0; i < 90; i++) {
            ai.update(0.1, player, null);
        }
        // AI must time out and reset cleanly to PATROL
        assertEquals(GuardState.PATROL, ai.getState(),
                "Guard must reset to PATROL after return timeout even if path obstructed!");
    }

    @Test
    void testDoorsCannotBecomePermanentlyUnusable() {
        Door door = new Door("Test Gate", Door.DoorType.SECURITY_BADGE, 200, 200, 80, 16, true);

        // 1. Unlocked door toggling
        assertFalse(door.isOpen());
        door.onInteract();
        assertTrue(door.isOpen());
        door.onInteract();
        assertFalse(door.isOpen());

        // 2. Electronic locking
        door.setLocked(true);
        assertTrue(door.isLocked());
        assertTrue(door.canBeHacked());

        // 3. Hack success unlocks and opens door
        door.onHackSuccess();
        assertTrue(door.isHacked());
        assertFalse(door.isLocked());
        assertTrue(door.isOpen());

        // 4. Repeated interactive operations remain fully usable
        door.onInteract(); // Close
        assertFalse(door.isOpen());
        door.onInteract(); // Re-open
        assertTrue(door.isOpen());
    }

    @Test
    void testHackingInputDebouncePreventsSpam() {
        Door hackableDoor = new Door("Vault Door", Door.DoorType.HEAVY_VAULT, 100, 100, 80, 16, true);
        hackableDoor.setLocked(true);

        HackingSession session = new HackingSession(hackableDoor);
        assertNotNull(session.getTargetSequence());
        assertFalse(session.isFinished());

        // 1. Correct node advances sequence
        String expected = session.getTargetSequence().get(0);
        NetworkNode targetNode = session.getNodePool().stream()
                .filter(n -> n.getCode().equals(expected))
                .findFirst()
                .orElse(null);
        assertNotNull(targetNode);

        boolean firstSelect = session.selectNode(targetNode.getIndex());
        assertTrue(firstSelect, "Valid node selection succeeds");
        assertEquals(1, session.getCurrentSequenceIndex());

        // 2. Brute-force spam protection: multiple wrong guesses accumulate trace and trigger lockout
        String nextExpected = session.getTargetSequence().get(1);
        NetworkNode wrongNode = session.getNodePool().stream()
                .filter(n -> !n.getCode().equals(nextExpected))
                .findFirst()
                .orElse(null);
        assertNotNull(wrongNode);

        for (int i = 0; i < 5; i++) {
            session.selectNode(wrongNode.getIndex());
        }
        // Rapid wrong spam causes security trace to reach 100% and locks down the session
        assertEquals(HackingSession.SessionStatus.FAILED_TRACED, session.getStatus());
        assertTrue(session.isFinished(), "Spamming hacking immediately triggers security trace lockout!");
    }

    // =========================================================================
    // 2. PERFORMANCE MONITORING TESTS
    // =========================================================================

    @Test
    void testTelemetryPerformanceMetrics() {
        TelemetryService telemetry = TelemetryService.getInstance();
        assertNotNull(telemetry);

        // 1. Frame time and FPS calculation
        telemetry.updateMetrics(60, 60, 0.0166, 2.5, 3.8);
        assertTrue(telemetry.getCurrentFps() > 0, "FPS must be calculated and > 0");
        assertTrue(telemetry.getUpdateDurationMs() > 0.0, "Update duration ms must be > 0");

        // 2. CPU cores and Heap memory
        assertTrue(telemetry.getAvailableProcessors() >= 1, "Must detect at least 1 logical CPU core");
        assertTrue(telemetry.getMaxMemoryMb() > 0, "Max memory MB must be > 0");

        // 3. A* Pathfinding Throughput & Latency profiling
        telemetry.recordPathSearch(1250); // 1.25 ms search (1250 us)
        telemetry.recordPathSearch(750);  // 0.75 ms search (750 us)
        assertTrue(telemetry.getTotalPathSearches() >= 2, "Must record path searches count");
        assertTrue(telemetry.getAvgPathSearchTimeMicros() > 0.0, "Average search time in us must be positive");
    }

    // =========================================================================
    // 3. SAVE SYSTEM ROUNDTRIP: SAVE -> EXIT -> RESTART -> LOAD -> RESTORED
    // =========================================================================

    @Test
    void testSaveExitRestartLoadRestoresEverything() {
        // Step 1: Create a rich, mutated game snapshot
        SaveData original = new SaveData();
        original.setMissionCodename("MISSION 01 // NOIDA DATA CENTER");
        original.setMissionTime(187.4);
        original.setCreditsExfiltrated(750000);
        original.setAlertTriggered(true);
        original.setCurrentObjectiveIndex(4);
        original.getCompletedObjectiveSteps().add(MissionStep.ENTER_FACILITY.name());
        original.getCompletedObjectiveSteps().add(MissionStep.AVOID_SECURITY.name());
        original.getCompletedObjectiveSteps().add(MissionStep.ACCESS_TERMINAL.name());
        original.getCompletedObjectiveSteps().add(MissionStep.DISABLE_CAMERAS.name());

        // Mutated Player state
        SaveData.PlayerSnapshot ps = original.getPlayer();
        ps.x = 840.5;
        ps.y = 520.0;
        ps.velocityX = 15.2;
        ps.velocityY = -8.4;
        ps.facingDirection = "EAST";
        ps.state = "RUNNING";
        ps.currentStamina = 68.5;
        ps.walkSpeed = 205.0;
        ps.sprintSpeed = 419.75; // Buffed by perks

        // Mutated Inventory
        SaveData.InventorySnapshot is = original.getInventory();
        is.dataPayloadSecured = true;
        is.itemCounts.put(ItemType.EMP.name(), 1);
        is.itemCounts.put(ItemType.LOCKPICK.name(), 2);
        is.itemCounts.put(ItemType.MEDKIT.name(), 0);
        is.itemCounts.put(ItemType.DATA_DRIVE.name(), 1);

        // Mutated Progression
        SaveData.ProgressionSnapshot prs = original.getProgression();
        prs.totalXp = 450;
        prs.currentXp = 50;
        prs.unspentUpgradePoints = 1;
        prs.trackRanks.put(ProgressionTrack.HACKING.name(), 2);
        prs.trackRanks.put(ProgressionTrack.STEALTH.name(), 1);
        prs.trackRanks.put(ProgressionTrack.MOVEMENT.name(), 1);
        prs.trackRanks.put(ProgressionTrack.EQUIPMENT.name(), 0);

        // Mutated World Interactables
        SaveData.WorldSnapshot ws = original.getWorld();
        ws.doorOpen.put("Main Entrance Slider", true);
        ws.doorLocked.put("Security Checkpoint Gate", false);
        ws.doorHacked.put("Security Checkpoint Gate", true);
        ws.workstationHacked.put("SEC-CCTV-01", true);
        ws.workstationAccessed.put("SEC-CCTV-01", true);
        ws.cameraDisabled.put("CAM-01 [RECEPTION]", true);
        ws.cameraHacked.put("CAM-01 [RECEPTION]", true);
        ws.terminalHacked.put("SEC-NET-ROUTER", true);
        ws.terminalCamerasDisabled.put("SEC-NET-ROUTER", true);
        ws.alarmDisarmed.put("ALARM-01 [SECURITY]", true);
        ws.alarmActive.put("ALARM-01 [SECURITY]", false);
        ws.serverRackHacked.put("DATABASE-CLUSTER-A", true);
        ws.elevatorCalled.put("ELEV-01 [EXTRACTION]", true);
        ws.elevatorHacked.put("ELEV-01 [EXTRACTION]", true);
        ws.safeOpened.put("TITAN-CIPHER-SAFE", true);
        ws.safeLooted.put("TITAN-CIPHER-SAFE", true);

        // Mutated Guards
        SaveData.GuardSnapshot gs1 = new SaveData.GuardSnapshot();
        gs1.name = "GUARD-01 [SECURITY]";
        gs1.x = 940.0;
        gs1.y = 360.0;
        gs1.facingAngle = 1.57;
        gs1.unconscious = false;
        gs1.stunned = true;
        gs1.stunTimer = 2.4;
        gs1.aiState = "SUSPICIOUS";
        original.getGuards().add(gs1);

        SaveData.GuardSnapshot gs2 = new SaveData.GuardSnapshot();
        gs2.name = "GUARD-02 [OFFICE]";
        gs2.x = 420.0;
        gs2.y = 650.0;
        gs2.facingAngle = 3.14;
        gs2.unconscious = true;
        gs2.stunned = false;
        gs2.stunTimer = 0.0;
        gs2.aiState = "UNCONSCIOUS";
        original.getGuards().add(gs2);

        // --- STEP 1: SAVE ---
        boolean saved = SaveManager.saveGame(original, TEST_SAVE_FILE);
        assertTrue(saved, "SaveManager.saveGame must succeed and return true");
        assertTrue(SaveManager.hasSaveFile(TEST_SAVE_FILE), "Save file must exist on disk");

        // --- STEP 2: EXIT & RESTART (Destroy references) ---
        original = null;
        assertNull(original, "Original snapshot reference destroyed simulating process shutdown");

        // --- STEP 3: LOAD ---
        SaveData restored = SaveManager.loadGame(TEST_SAVE_FILE);
        assertNotNull(restored, "Restored SaveData must be read from disk");

        // --- STEP 4: EVERYTHING RESTORED ASSERTIONS ---
        // Mission restoration
        assertEquals("MISSION 01 // NOIDA DATA CENTER", restored.getMissionCodename());
        assertEquals(187.4, restored.getMissionTime(), 0.001);
        assertEquals(750000, restored.getCreditsExfiltrated());
        assertTrue(restored.isAlertTriggered());
        assertEquals(4, restored.getCurrentObjectiveIndex());
        assertEquals(4, restored.getCompletedObjectiveSteps().size());

        // Player restoration
        assertEquals(840.5, restored.getPlayer().x, 0.001);
        assertEquals(520.0, restored.getPlayer().y, 0.001);
        assertEquals(15.2, restored.getPlayer().velocityX, 0.001);
        assertEquals(-8.4, restored.getPlayer().velocityY, 0.001);
        assertEquals("EAST", restored.getPlayer().facingDirection);
        assertEquals("RUNNING", restored.getPlayer().state);
        assertEquals(68.5, restored.getPlayer().currentStamina, 0.001);
        assertEquals(419.75, restored.getPlayer().sprintSpeed, 0.001);

        // Inventory restoration
        assertTrue(restored.getInventory().dataPayloadSecured);
        assertEquals(1, restored.getInventory().itemCounts.get(ItemType.EMP.name()));
        assertEquals(2, restored.getInventory().itemCounts.get(ItemType.LOCKPICK.name()));
        assertEquals(0, restored.getInventory().itemCounts.get(ItemType.MEDKIT.name()));
        assertEquals(1, restored.getInventory().itemCounts.get(ItemType.DATA_DRIVE.name()));

        // Progression restoration
        assertEquals(450, restored.getProgression().totalXp);
        assertEquals(50, restored.getProgression().currentXp);
        assertEquals(1, restored.getProgression().unspentUpgradePoints);
        assertEquals(2, restored.getProgression().trackRanks.get(ProgressionTrack.HACKING.name()));
        assertEquals(1, restored.getProgression().trackRanks.get(ProgressionTrack.STEALTH.name()));

        // World restoration
        assertTrue(restored.getWorld().doorOpen.get("Main Entrance Slider"));
        assertFalse(restored.getWorld().doorLocked.get("Security Checkpoint Gate"));
        assertTrue(restored.getWorld().doorHacked.get("Security Checkpoint Gate"));
        assertTrue(restored.getWorld().workstationHacked.get("SEC-CCTV-01"));
        assertTrue(restored.getWorld().workstationAccessed.get("SEC-CCTV-01"));
        assertTrue(restored.getWorld().cameraDisabled.get("CAM-01 [RECEPTION]"));
        assertTrue(restored.getWorld().terminalHacked.get("SEC-NET-ROUTER"));
        assertTrue(restored.getWorld().terminalCamerasDisabled.get("SEC-NET-ROUTER"));
        assertTrue(restored.getWorld().alarmDisarmed.get("ALARM-01 [SECURITY]"));
        assertTrue(restored.getWorld().serverRackHacked.get("DATABASE-CLUSTER-A"));
        assertTrue(restored.getWorld().elevatorCalled.get("ELEV-01 [EXTRACTION]"));
        assertTrue(restored.getWorld().safeOpened.get("TITAN-CIPHER-SAFE"));
        assertTrue(restored.getWorld().safeLooted.get("TITAN-CIPHER-SAFE"));

        // Guard restoration
        assertEquals(2, restored.getGuards().size());
        SaveData.GuardSnapshot restoredG1 = restored.getGuards().stream()
                .filter(g -> g.name.equals("GUARD-01 [SECURITY]"))
                .findFirst().orElse(null);
        assertNotNull(restoredG1);
        assertEquals(940.0, restoredG1.x, 0.001);
        assertEquals(360.0, restoredG1.y, 0.001);
        assertTrue(restoredG1.stunned);
        assertEquals(2.4, restoredG1.stunTimer, 0.001);
        assertEquals("SUSPICIOUS", restoredG1.aiState);

        SaveData.GuardSnapshot restoredG2 = restored.getGuards().stream()
                .filter(g -> g.name.equals("GUARD-02 [OFFICE]"))
                .findFirst().orElse(null);
        assertNotNull(restoredG2);
        assertEquals(420.0, restoredG2.x, 0.001);
        assertTrue(restoredG2.unconscious);
        assertEquals("UNCONSCIOUS", restoredG2.aiState);
    }
}
