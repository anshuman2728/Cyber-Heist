package com.cyberheist.mission;

import com.cyberheist.collision.BoundingBox;
import com.cyberheist.entity.Player;
import com.cyberheist.stealth.DetectionState;
import com.cyberheist.stealth.Guard;
import com.cyberheist.stealth.StealthSystem;
import com.cyberheist.world.NoidaDataCenterLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Headless test suite verifying Mission 1: "Ghost in the Server"
 * objective progression, story triggers, human contextual dialogue, and extraction debrief.
 */
public class MissionProgressionTest {

    private MissionManager mission;
    private RadioDialogueManager dialogue;
    private NoidaDataCenterLevel level;
    private Player player;
    private StealthSystem stealthSystem;

    @BeforeEach
    public void setUp() {
        mission = new MissionManager();
        dialogue = new RadioDialogueManager();
        level = new NoidaDataCenterLevel();
        player = new Player(level.getSpawnPoint().x, level.getSpawnPoint().y);
        stealthSystem = new StealthSystem();
        for (Guard g : level.getGuards()) {
            stealthSystem.addGuard(g);
        }
    }

    @Test
    @DisplayName("Mission 1 initializes with all 7 sequential objectives")
    public void testMissionObjectivesInitialState() {
        assertEquals("Ghost in the Server", mission.getMissionTitle());
        assertEquals(MissionManager.MissionStatus.IN_PROGRESS, mission.getStatus());

        List<MissionObjective> list = mission.getObjectives();
        assertEquals(7, list.size());

        assertEquals(MissionStep.ENTER_FACILITY, list.get(0).getStep());
        assertEquals(MissionStep.AVOID_SECURITY, list.get(1).getStep());
        assertEquals(MissionStep.ACCESS_TERMINAL, list.get(2).getStep());
        assertEquals(MissionStep.DISABLE_CAMERAS, list.get(3).getStep());
        assertEquals(MissionStep.REACH_SERVER_ROOM, list.get(4).getStep());
        assertEquals(MissionStep.STEAL_DATA, list.get(5).getStep());
        assertEquals(MissionStep.ESCAPE, list.get(6).getStep());

        assertTrue(list.get(0).isActive());
        assertEquals(0, mission.getCompletedCount());
        assertEquals(7, mission.getTotalCount());
    }

    @Test
    @DisplayName("Opening radio comms feature natural human dialogue without AI clichés")
    public void testOpeningHumanDialogue() {
        mission.startMission(dialogue);

        assertTrue(dialogue.hasActiveDialogue());
        DialogueLine line1 = dialogue.getCurrentLine();
        assertEquals("MIRA", line1.getSpeaker());
        assertEquals("You're late.", line1.getText());

        // Advance to next line
        dialogue.skipCurrentLine();
        DialogueLine line2 = dialogue.getCurrentLine();
        assertEquals("KAVIN", line2.getSpeaker());
        assertTrue(line2.getText().contains("Traffic"));

        // Advance to third line
        dialogue.skipCurrentLine();
        DialogueLine line3 = dialogue.getCurrentLine();
        assertEquals("MIRA", line3.getSpeaker());
        assertEquals("You weren't followed?", line3.getText());
    }

    @Test
    @DisplayName("Radio dialogue manager advances and expires cleanly with delta time")
    public void testDialogueManagerTiming() {
        dialogue.queueLine(DialogueLine.mira("Test message", 1.0));
        assertTrue(dialogue.hasActiveDialogue());

        dialogue.update(0.5);
        assertTrue(dialogue.hasActiveDialogue());
        assertEquals(0.5, dialogue.getCurrentLineTimer(), 0.001);

        dialogue.update(0.6);
        assertFalse(dialogue.hasActiveDialogue());
    }

    @Test
    @DisplayName("Complete end-to-end progression through all 7 objectives to VICTORY")
    public void testFullMissionProgression() {
        // Start mission
        mission.startMission(dialogue);
        assertEquals(MissionStep.ENTER_FACILITY, mission.getCurrentObjective().getStep());

        // Step 1: Player moves inside Reception Lobby (X=400, Y=300)
        player.setPosition(400, 300);
        mission.update(0.1, player, level, stealthSystem, dialogue);
        assertEquals(MissionStep.AVOID_SECURITY, mission.getCurrentObjective().getStep());
        assertEquals(1, mission.getCompletedCount());

        // Step 2: Player approaches Security Checkpoint (X=900, Y=300)
        player.setPosition(900, 300);
        mission.update(0.1, player, level, stealthSystem, dialogue);
        assertEquals(MissionStep.ACCESS_TERMINAL, mission.getCurrentObjective().getStep());
        assertEquals(2, mission.getCompletedCount());

        // Step 3: Player overrides Security Terminal
        level.getTerminals().get(0).onHackSuccess();
        mission.update(0.1, player, level, stealthSystem, dialogue);

        // Terminal override also loops all cameras, completing both Step 3 and Step 4!
        assertEquals(MissionStep.REACH_SERVER_ROOM, mission.getCurrentObjective().getStep());
        assertEquals(4, mission.getCompletedCount());

        // Step 5: Player enters Server Room (X=1100, Y=550)
        player.setPosition(1100, 550);
        mission.update(0.1, player, level, stealthSystem, dialogue);
        assertEquals(MissionStep.STEAL_DATA, mission.getCurrentObjective().getStep());
        assertEquals(5, mission.getCompletedCount());

        // Step 6: Player hacks server rack for data payload
        level.getServerRacks().get(0).onHackSuccess();
        mission.update(0.1, player, level, stealthSystem, dialogue);
        assertEquals(MissionStep.ESCAPE, mission.getCurrentObjective().getStep());
        assertEquals(6, mission.getCompletedCount());
        assertEquals(250000, mission.getCreditsExfiltrated());

        // Step 7: Player reaches rear exit extraction zone
        BoundingBox exit = level.getExitZone();
        player.setPosition(exit.getX() + 10, exit.getY() + 10);
        mission.update(0.1, player, level, stealthSystem, dialogue);

        assertEquals(MissionManager.MissionStatus.SUCCESS, mission.getStatus());
        assertEquals(7, mission.getCompletedCount());
        assertEquals("GHOST // UNDETECTED", mission.getStealthRating());
    }

    @Test
    @DisplayName("Stealth breach triggers urgent reactive radio comms from handler")
    public void testReactiveVoiceOnDetection() {
        mission.startMission(dialogue);
        dialogue.clear(); // Clear opening chatter

        // Simulate guard spotting player and detection hitting 100%
        level.getGuards().get(0).getDetectionMeter().addInstantDetection(100.0);
        stealthSystem.update(0.001, player, level.getCollisionWorld(), level.getCoverObjects());

        assertEquals(DetectionState.DETECTED, stealthSystem.getGlobalState());

        mission.update(0.1, player, level, stealthSystem, dialogue);

        assertTrue(mission.isAlertTriggered());
        assertTrue(dialogue.hasActiveDialogue());
        DialogueLine urgentLine = dialogue.getCurrentLine();
        assertEquals("MIRA", urgentLine.getSpeaker());
        assertTrue(urgentLine.getText().contains("break line of sight"));
        assertEquals("SHADOW // DETECTED ONCE", mission.getStealthRating());
    }
}
