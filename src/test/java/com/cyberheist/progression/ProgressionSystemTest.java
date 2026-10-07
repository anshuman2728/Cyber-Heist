package com.cyberheist.progression;

import com.cyberheist.hacking.HackingDifficulty;
import com.cyberheist.hacking.HackingSession;
import com.cyberheist.inventory.Inventory;
import com.cyberheist.inventory.ItemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Progression System & Skill Matrix Tests")
public class ProgressionSystemTest {

    private ProgressionManager progression;

    @BeforeEach
    void setUp() {
        this.progression = new ProgressionManager();
    }

    @Test
    @DisplayName("Initial state grants 2 Skill Points and leveling grants additional points at 100 XP intervals")
    void testInitialStateAndXpGain() {
        assertEquals(2, progression.getSkillPoints(), "Should start with 2 initial skill points");
        assertEquals(0, progression.getTotalXp());

        progression.addXp(60);
        assertEquals(2, progression.getSkillPoints(), "60 XP should not yet trigger new skill point");

        progression.addXp(40); // Total 100 XP
        assertEquals(3, progression.getSkillPoints(), "100 XP must award 1 additional skill point");
        assertEquals(100, progression.getTotalXp());

        progression.addXp(120); // Total 220 XP -> 200 threshold passed
        assertEquals(4, progression.getSkillPoints(), "200 XP threshold must award another skill point");
    }

    @Test
    @DisplayName("Hacking Progression grants time bonus, slower trace and ICE Breaker auto-solve")
    void testHackingProgression() {
        assertEquals(0, progression.getRank(ProgressionTrack.HACKING));
        assertEquals(0.0, progression.getHackTimeBonus());
        assertEquals(1.0, progression.getTraceRateMultiplier());
        assertFalse(progression.isAutoSolveFirstNode());

        // Upgrade to Rank 1
        assertTrue(progression.upgrade(ProgressionTrack.HACKING));
        assertEquals(1, progression.getRank(ProgressionTrack.HACKING));
        assertEquals(2.5, progression.getHackTimeBonus(), 0.01);
        assertEquals(0.80, progression.getTraceRateMultiplier(), 0.01);

        // Upgrade to Rank 2 (ICE Breaker)
        assertTrue(progression.upgrade(ProgressionTrack.HACKING));
        assertEquals(2, progression.getRank(ProgressionTrack.HACKING));
        assertTrue(progression.isAutoSolveFirstNode());
        assertEquals(0.50, progression.getTracePenaltyMultiplier(), 0.01);

        // Cannot exceed max rank
        assertFalse(progression.canUpgrade(ProgressionTrack.HACKING));
    }

    @Test
    @DisplayName("Stealth Progression dampens acoustic footprint and accelerates shadow decay")
    void testStealthProgression() {
        // Upgrade to Rank 1 (Ghost Protocol)
        assertTrue(progression.upgrade(ProgressionTrack.STEALTH));
        assertEquals(0.65, progression.getNoiseRadiusMultiplier(), 0.01,
                "Noise radius should be reduced by 35%");

        // Upgrade to Rank 2 (Shadow Veil)
        assertTrue(progression.upgrade(ProgressionTrack.STEALTH));
        assertEquals(0.70, progression.getDetectionRateMultiplier(), 0.01,
                "Detection rate should be reduced by 30%");
        assertEquals(1.50, progression.getDetectionDecayMultiplier(), 0.01,
                "Detection decay speed should be boosted by 50%");
    }

    @Test
    @DisplayName("Movement Progression boosts sprint velocity and maximum stamina")
    void testMovementProgression() {
        // Upgrade to Rank 1 (Conditioning)
        assertTrue(progression.upgrade(ProgressionTrack.MOVEMENT));
        assertEquals(1.15, progression.getSprintSpeedMultiplier(), 0.01,
                "Sprint speed should increase by 15%");
        assertEquals(25.0, progression.getMaxStaminaBonus(), 0.01,
                "Max stamina bonus should be +25");

        // Upgrade to Rank 2 (Adrenaline Surge)
        assertTrue(progression.upgrade(ProgressionTrack.MOVEMENT));
        assertEquals(1.50, progression.getStaminaRecoveryMultiplier(), 0.01,
                "Stamina recovery rate should be 1.5x");
        assertEquals(0.65, progression.getSprintStaminaDrainMultiplier(), 0.01,
                "Sprint stamina drain should be reduced by 35%");
    }

    @Test
    @DisplayName("Equipment Progression expands inventory capacity, throw range and halves stun cooldown")
    void testEquipmentProgression() {
        Inventory inv = Inventory.createStartingLoadout();
        assertEquals(2, inv.getMaxCapacity(ItemType.EMP));
        assertEquals(3, inv.getMaxCapacity(ItemType.LOCKPICK));

        // Upgrade to Rank 1 (Deep Pockets)
        assertTrue(progression.upgrade(ProgressionTrack.EQUIPMENT));
        inv.applyCapacityBonus(progression.getItemCapacityBonus());

        assertEquals(4, inv.getMaxCapacity(ItemType.EMP), "EMP capacity must increase to 4");
        assertEquals(5, inv.getMaxCapacity(ItemType.LOCKPICK), "Lockpick capacity must increase to 5");
        assertEquals(1.40, progression.getThrowRangeMultiplier(), 0.01, "Throw range should be 1.4x");

        // Upgrade to Rank 2 (Overclocked Coils)
        assertTrue(progression.upgrade(ProgressionTrack.EQUIPMENT));
        assertEquals(3.0, progression.getStunCooldown(), 0.01, "Stun cooldown should be halved to 3.0s");
        assertEquals(280.0, progression.getEmpRadius(), 0.01, "EMP blast radius should expand to 280px");
    }

    @Test
    @DisplayName("HackingSession applies progression bonuses accurately")
    void testHackingSessionWithProgression() {
        progression.upgrade(ProgressionTrack.HACKING); // Rank 1
        progression.upgrade(ProgressionTrack.HACKING); // Rank 2

        HackingSession session = new HackingSession(null, new Random(12345));
        double initialTime = session.getTimeRemaining();
        session.applyProgression(progression);

        // Time bonus added
        assertEquals(initialTime + 2.5, session.getTimeRemaining(), 0.01);

        // Auto-solve first node
        assertEquals(1, session.getCurrentSequenceIndex(), "ICE Breaker must auto-solve the 1st node");
    }
}
