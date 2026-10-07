package com.cyberheist.hacking;

import com.cyberheist.util.Logger;

import java.util.*;

/**
 * Encapsulates the runtime state and logic of an active Network Node hacking minigame.
 * Completely independent of rendering and headless testable.
 */
public final class HackingSession {

    private static final Logger LOGGER = Logger.getLogger(HackingSession.class);

    private static final String[] CODE_CATALOG = {
            "04", "17", "82", "39", "5A", "F1", "C8", "2B", "94", "D0", "3E", "77"
    };

    public enum SessionStatus {
        IN_PROGRESS,
        SUCCESS,
        FAILED_TIMEOUT,
        FAILED_TRACED,
        ABORTED
    }

    private final Hackable target;
    private final HackingDifficulty difficulty;
    private SessionStatus status = SessionStatus.IN_PROGRESS;

    // Timer & Security Countermeasures
    private double timeRemaining;
    private double securityTrace = 0.0; // 0.0% to 100.0%

    // Network Node Pool & Sequence Memory
    private final List<NetworkNode> nodePool = new ArrayList<>();
    private final List<String> targetSequence = new ArrayList<>();
    private int currentSequenceIndex = 0;

    // Feedback
    private String statusMessage = "INITIALIZING INTRUSION INJECTION...";
    private double resultDisplayTimer = 0.0;

    // Progression Modifiers
    private double traceRateMultiplier = 1.0;
    private double tracePenaltyMultiplier = 1.0;

    public HackingSession(Hackable target) {
        this(target, new Random());
    }

    public HackingSession(Hackable target, Random random) {
        this.target = target;
        this.difficulty = target != null ? target.getDifficulty() : HackingDifficulty.EASY;
        this.timeRemaining = this.difficulty.getTimeLimitSeconds();

        generateNodesAndSequence(random);
    }

    public void applyProgression(com.cyberheist.progression.ProgressionManager progression) {
        if (progression == null) return;
        this.timeRemaining += progression.getHackTimeBonus();
        this.traceRateMultiplier = progression.getTraceRateMultiplier();
        this.tracePenaltyMultiplier = progression.getTracePenaltyMultiplier();

        if (progression.isAutoSolveFirstNode() && !targetSequence.isEmpty() && currentSequenceIndex == 0) {
            String firstCode = targetSequence.get(0);
            for (NetworkNode n : nodePool) {
                if (n.getCode().equals(firstCode)) {
                    n.setState(NetworkNode.NodeState.MATCHED);
                    currentSequenceIndex = 1;
                    statusMessage = String.format("ICE BREAKER // AUTO-SOLVED [1/%d]", targetSequence.size());
                    break;
                }
            }
        }
    }

    private void generateNodesAndSequence(Random random) {
        // Pick unique codes from catalog for the node pool
        List<String> shuffledCatalog = new ArrayList<>(Arrays.asList(CODE_CATALOG));
        Collections.shuffle(shuffledCatalog, random);

        int poolSize = difficulty.getNodePoolCount();
        List<String> chosenCodes = shuffledCatalog.subList(0, Math.min(poolSize, shuffledCatalog.size()));

        for (int i = 0; i < chosenCodes.size(); i++) {
            nodePool.add(new NetworkNode(i + 1, chosenCodes.get(i)));
        }

        // Generate target sequence
        int seqLen = Math.min(difficulty.getSequenceLength(), chosenCodes.size());
        List<String> poolCopy = new ArrayList<>(chosenCodes);
        Collections.shuffle(poolCopy, random);

        for (int i = 0; i < seqLen; i++) {
            targetSequence.add(poolCopy.get(i));
        }

        this.statusMessage = String.format("LOCATE NODE SEQUENCE [0/%d]", targetSequence.size());
        LOGGER.info(String.format("Hacking Session started for [%s] (%s). Target sequence: %s",
                target != null ? target.getTargetName() : "Target", difficulty.name(), targetSequence));
    }

    /**
     * Executes real-time countdown, security trace accumulation, and node animations.
     */
    public void update(double deltaTime) {
        for (NetworkNode node : nodePool) {
            node.update(deltaTime);
        }

        if (status != SessionStatus.IN_PROGRESS) {
            resultDisplayTimer += deltaTime;
            return;
        }

        // 1. Advance countdown timer
        timeRemaining -= deltaTime;
        if (timeRemaining <= 0.0) {
            timeRemaining = 0.0;
            status = SessionStatus.FAILED_TIMEOUT;
            statusMessage = "HACK FAILED // CONNECTION TIMED OUT";
            LOGGER.info("Hacking session failed due to timeout.");
            if (target != null) target.onHackFailure();
            return;
        }

        // 2. Advance security trace accumulation
        securityTrace += difficulty.getTraceBuildRate() * traceRateMultiplier * deltaTime;
        if (securityTrace >= 100.0) {
            securityTrace = 100.0;
            status = SessionStatus.FAILED_TRACED;
            statusMessage = "CRITICAL BREACH // INTRUSION TRACE DETECTED!";
            LOGGER.info("Hacking session failed: Security trace hit 100%.");
            if (target != null) target.onHackFailure();
        }
    }

    /**
     * Player input: select a node by its 1-based index (Key 1..N or mouse click).
     */
    public boolean selectNode(int index) {
        if (status != SessionStatus.IN_PROGRESS) {
            return false;
        }

        if (index < 1 || index > nodePool.size()) {
            return false;
        }

        NetworkNode node = nodePool.get(index - 1);

        String expectedCode = targetSequence.get(currentSequenceIndex);

        if (node.getCode().equals(expectedCode)) {
            // Correct node sequence match
            node.setState(NetworkNode.NodeState.MATCHED);
            currentSequenceIndex++;
            statusMessage = String.format("NODE SYNCHRONIZED [%d/%d]", currentSequenceIndex, targetSequence.size());

            if (currentSequenceIndex >= targetSequence.size()) {
                // Entire sequence matched!
                status = SessionStatus.SUCCESS;
                statusMessage = "SECURITY OVERRIDE COMPLETE // ACCESS GRANTED";
                LOGGER.info("Hacking session SUCCESSFUL! Target breached.");
                if (target != null) target.onHackSuccess();
            }
            return true;
        } else {
            // Mistake penalty (reduced by ICE Breaker perk)
            node.triggerErrorFlash(0.4);
            double penalty = difficulty.getMistakePenalty() * tracePenaltyMultiplier;
            securityTrace = Math.min(100.0, securityTrace + penalty);

            // Reset matching progress
            for (NetworkNode n : nodePool) {
                if (n.getState() == NetworkNode.NodeState.MATCHED) {
                    n.setState(NetworkNode.NodeState.IDLE);
                }
            }
            currentSequenceIndex = 0;
            statusMessage = String.format("INVALID SEQUENCE // TRACE SPIKE (+%.0f%%)", penalty);
            LOGGER.info("Hacking mistake: Sequence reset. Trace now at " + securityTrace + "%");

            if (securityTrace >= 100.0) {
                securityTrace = 100.0;
                status = SessionStatus.FAILED_TRACED;
                statusMessage = "CRITICAL BREACH // INTRUSION TRACE DETECTED!";
                if (target != null) target.onHackFailure();
            }
            return false;
        }
    }

    public void abort() {
        if (status == SessionStatus.IN_PROGRESS) {
            status = SessionStatus.ABORTED;
            statusMessage = "DISCONNECTED FROM TARGET";
            LOGGER.info("Hacking session manually aborted by player.");
        }
    }

    /**
     * Generates ASCII progress bar representing Security Trace (e.g. "███████░░").
     */
    public String getSecurityTraceBlocks() {
        int totalBlocks = 10;
        int filled = (int) Math.round((securityTrace / 100.0) * totalBlocks);
        filled = Math.max(0, Math.min(totalBlocks, filled));

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < filled; i++) sb.append("\u2588");
        for (int i = filled; i < totalBlocks; i++) sb.append("\u2591");
        return sb.toString();
    }

    public Hackable getTarget() { return target; }
    public HackingDifficulty getDifficulty() { return difficulty; }
    public SessionStatus getStatus() { return status; }
    public double getTimeRemaining() { return timeRemaining; }
    public double getSecurityTrace() { return securityTrace; }
    public List<NetworkNode> getNodePool() { return Collections.unmodifiableList(nodePool); }
    public List<String> getTargetSequence() { return Collections.unmodifiableList(targetSequence); }
    public int getCurrentSequenceIndex() { return currentSequenceIndex; }
    public String getStatusMessage() { return statusMessage; }
    public double getResultDisplayTimer() { return resultDisplayTimer; }
    public boolean isFinished() { return status != SessionStatus.IN_PROGRESS; }
}
