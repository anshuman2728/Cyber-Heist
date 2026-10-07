package com.cyberheist.hacking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Headless unit test suite verifying the Network Node hacking minigame mechanics,
 * deterministic sequencing, time/trace bounds, difficulty tiers, and ASCII bar formatting.
 */
public class HackingMinigameTest {

    private static class MockHackable implements Hackable {
        private final HackingDifficulty difficulty;
        private boolean hacked = false;
        private boolean failureCalled = false;

        public MockHackable(HackingDifficulty difficulty) {
            this.difficulty = difficulty;
        }

        @Override public HackingDifficulty getDifficulty() { return difficulty; }
        @Override public String getTargetName() { return "TEST-SYSTEM-01"; }
        @Override public void onHackSuccess() { this.hacked = true; }
        @Override public void onHackFailure() { this.failureCalled = true; }
        @Override public boolean isHacked() { return hacked; }
    }

    private MockHackable easyTarget;
    private MockHackable hardTarget;

    @BeforeEach
    public void setUp() {
        easyTarget = new MockHackable(HackingDifficulty.EASY);
        hardTarget = new MockHackable(HackingDifficulty.HARD);
    }

    @Test
    @DisplayName("Difficulty configurations specify valid progression rules")
    public void testDifficultyTiers() {
        assertTrue(HackingDifficulty.EASY.getNodePoolCount() >= 4);
        assertTrue(HackingDifficulty.EASY.getSequenceLength() >= 3);
        assertTrue(HackingDifficulty.EASY.getTimeLimitSeconds() > 10.0);

        assertTrue(HackingDifficulty.MEDIUM.getSequenceLength() > HackingDifficulty.EASY.getSequenceLength() ||
                HackingDifficulty.MEDIUM.getTimeLimitSeconds() < HackingDifficulty.EASY.getTimeLimitSeconds());

        assertTrue(HackingDifficulty.HARD.getSequenceLength() >= 5);
        assertTrue(HackingDifficulty.HARD.getMistakePenalty() >= 20.0);

        assertEquals(8, HackingDifficulty.VERY_HARD.getNodePoolCount());
        assertEquals(6, HackingDifficulty.VERY_HARD.getSequenceLength());
        assertEquals(6.0, HackingDifficulty.VERY_HARD.getTimeLimitSeconds());
    }

    @Test
    @DisplayName("Session initializes pool and target sequence deterministically with seed")
    public void testDeterministicSessionGeneration() {
        Random random1 = new Random(1337);
        HackingSession session1 = new HackingSession(easyTarget, random1);

        assertEquals(HackingDifficulty.EASY.getNodePoolCount(), session1.getNodePool().size());
        assertEquals(HackingDifficulty.EASY.getSequenceLength(), session1.getTargetSequence().size());
        assertEquals(0, session1.getCurrentSequenceIndex());
        assertEquals(HackingSession.SessionStatus.IN_PROGRESS, session1.getStatus());
        assertEquals(12.0, session1.getTimeRemaining(), 0.001);
        assertEquals(0.0, session1.getSecurityTrace(), 0.001);

        // Every target sequence element exists in the node pool
        List<String> poolCodes = session1.getNodePool().stream().map(NetworkNode::getCode).toList();
        for (String seqCode : session1.getTargetSequence()) {
            assertTrue(poolCodes.contains(seqCode));
        }
    }

    @Test
    @DisplayName("Matching complete sequence results in SUCCESS and triggers target callback")
    public void testSuccessfulSequenceMatch() {
        Random random = new Random(42);
        HackingSession session = new HackingSession(easyTarget, random);

        List<String> targetSeq = session.getTargetSequence();
        List<NetworkNode> pool = session.getNodePool();

        for (int i = 0; i < targetSeq.size(); i++) {
            String neededCode = targetSeq.get(i);
            // Locate node index in pool
            int nodeIdx = -1;
            for (NetworkNode n : pool) {
                if (n.getCode().equals(neededCode)) {
                    nodeIdx = n.getIndex();
                    break;
                }
            }
            assertTrue(nodeIdx > 0);

            boolean result = session.selectNode(nodeIdx);
            assertTrue(result);
            assertEquals(i + 1, session.getCurrentSequenceIndex());
        }

        assertEquals(HackingSession.SessionStatus.SUCCESS, session.getStatus());
        assertTrue(easyTarget.isHacked());
        assertTrue(session.isFinished());
        assertTrue(session.getStatusMessage().contains("ACCESS GRANTED"));
    }

    @Test
    @DisplayName("Mistaken node selection resets sequence and spikes security trace")
    public void testMistakeResetsSequenceAndAddsTrace() {
        Random random = new Random(99);
        HackingSession session = new HackingSession(easyTarget, random);

        List<String> targetSeq = session.getTargetSequence();
        List<NetworkNode> pool = session.getNodePool();

        // 1. Enter first code correctly
        String firstCode = targetSeq.get(0);
        int correctIdx = pool.stream().filter(n -> n.getCode().equals(firstCode)).findFirst().get().getIndex();
        session.selectNode(correctIdx);
        assertEquals(1, session.getCurrentSequenceIndex());

        // 2. Intentionally pick wrong code
        String secondCode = targetSeq.get(1);
        int wrongIdx = pool.stream().filter(n -> !n.getCode().equals(secondCode)).findFirst().get().getIndex();
        boolean success = session.selectNode(wrongIdx);

        assertFalse(success);
        // Sequence reset to 0
        assertEquals(0, session.getCurrentSequenceIndex());
        // Trace penalized
        assertEquals(HackingDifficulty.EASY.getMistakePenalty(), session.getSecurityTrace(), 0.001);
        assertTrue(session.getStatusMessage().contains("TRACE SPIKE"));
    }

    @Test
    @DisplayName("Countdown timer expires resulting in FAILED_TIMEOUT")
    public void testTimeoutFailure() {
        HackingSession session = new HackingSession(easyTarget, new Random(1));
        assertEquals(12.0, session.getTimeRemaining(), 0.001);

        session.update(6.0);
        assertEquals(6.0, session.getTimeRemaining(), 0.001);
        assertEquals(HackingSession.SessionStatus.IN_PROGRESS, session.getStatus());

        session.update(6.1);
        assertEquals(0.0, session.getTimeRemaining(), 0.001);
        assertEquals(HackingSession.SessionStatus.FAILED_TIMEOUT, session.getStatus());
        assertTrue(easyTarget.failureCalled);
    }

    @Test
    @DisplayName("Repeated mistakes cause security trace to hit 100% resulting in FAILED_TRACED")
    public void testTraceOverflowFailure() {
        HackingSession session = new HackingSession(hardTarget, new Random(10));
        // Hard mistake penalty is 20.0%
        List<String> targetSeq = session.getTargetSequence();
        List<NetworkNode> pool = session.getNodePool();
        String expectedCode = targetSeq.get(0);
        int wrongIdx = pool.stream().filter(n -> !n.getCode().equals(expectedCode)).findFirst().get().getIndex();

        for (int i = 0; i < 5; i++) {
            session.selectNode(wrongIdx);
        }

        assertEquals(100.0, session.getSecurityTrace(), 0.001);
        assertEquals(HackingSession.SessionStatus.FAILED_TRACED, session.getStatus());
        assertTrue(hardTarget.failureCalled);
    }

    @Test
    @DisplayName("Manual abort cleanly stops session")
    public void testAbortSession() {
        HackingSession session = new HackingSession(easyTarget, new Random(5));
        session.abort();
        assertEquals(HackingSession.SessionStatus.ABORTED, session.getStatus());
        assertTrue(session.isFinished());
        assertFalse(easyTarget.isHacked());
    }

    @Test
    @DisplayName("ASCII security trace blocks render correctly (e.g. ███████░░░)")
    public void testAsciiSecurityBlocks() {
        HackingSession session = new HackingSession(easyTarget, new Random(7));

        assertEquals("\u2591\u2591\u2591\u2591\u2591\u2591\u2591\u2591\u2591\u2591", session.getSecurityTraceBlocks());

        // Introduce mistakes to increment trace by 10% per mistake
        String expectedCode = session.getTargetSequence().get(0);
        int wrongIdx = session.getNodePool().stream()
                .filter(n -> !n.getCode().equals(expectedCode))
                .findFirst().get().getIndex();

        // 7 mistakes = 70% security trace
        for (int i = 0; i < 7; i++) {
            session.selectNode(wrongIdx);
        }

        assertEquals(70.0, session.getSecurityTrace(), 0.001);
        // Security 70% should yield 7 filled blocks and 3 empty blocks: ███████░░░
        assertEquals("\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2591\u2591\u2591", session.getSecurityTraceBlocks());
    }
}
