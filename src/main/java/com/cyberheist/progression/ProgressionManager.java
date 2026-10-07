package com.cyberheist.progression;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Manages player XP, skill point acquisition, and passive bonuses across
 * Hacking, Stealth, Movement, and Equipment.
 */
public final class ProgressionManager {

    public static final int MAX_RANK = 2;
    private static final int XP_PER_SKILL_POINT = 100;

    private int skillPoints = 2; // Start with 2 initial points to experiment
    private int totalXp = 0;
    private int currentXp = 0;

    private final Map<ProgressionTrack, Integer> ranks = new EnumMap<>(ProgressionTrack.class);

    public ProgressionManager() {
        for (ProgressionTrack track : ProgressionTrack.values()) {
            ranks.put(track, 0);
        }
    }

    public void addXp(int amount) {
        if (amount <= 0) return;
        totalXp += amount;
        currentXp += amount;
        while (currentXp >= XP_PER_SKILL_POINT) {
            currentXp -= XP_PER_SKILL_POINT;
            skillPoints++;
        }
    }

    public void awardSkillPoint() {
        skillPoints++;
    }

    public boolean canUpgrade(ProgressionTrack track) {
        return skillPoints > 0 && getRank(track) < MAX_RANK;
    }

    public boolean upgrade(ProgressionTrack track) {
        if (!canUpgrade(track)) return false;
        skillPoints--;
        ranks.put(track, getRank(track) + 1);
        return true;
    }

    public int getRank(ProgressionTrack track) {
        return ranks.getOrDefault(track, 0);
    }

    // --- HACKING BONUSES ---
    public double getHackTimeBonus() {
        return getRank(ProgressionTrack.HACKING) >= 1 ? 2.5 : 0.0;
    }

    public double getTraceRateMultiplier() {
        return getRank(ProgressionTrack.HACKING) >= 1 ? 0.80 : 1.0;
    }

    public boolean isAutoSolveFirstNode() {
        return getRank(ProgressionTrack.HACKING) >= 2;
    }

    public double getTracePenaltyMultiplier() {
        return getRank(ProgressionTrack.HACKING) >= 2 ? 0.50 : 1.0;
    }

    // --- STEALTH BONUSES ---
    public double getNoiseRadiusMultiplier() {
        return getRank(ProgressionTrack.STEALTH) >= 1 ? 0.65 : 1.0;
    }

    public double getDetectionRateMultiplier() {
        return getRank(ProgressionTrack.STEALTH) >= 2 ? 0.70 : 1.0;
    }

    public double getDetectionDecayMultiplier() {
        return getRank(ProgressionTrack.STEALTH) >= 2 ? 1.50 : 1.0;
    }

    // --- MOVEMENT BONUSES ---
    public double getSprintSpeedMultiplier() {
        return getRank(ProgressionTrack.MOVEMENT) >= 1 ? 1.15 : 1.0;
    }

    public double getMaxStaminaBonus() {
        return getRank(ProgressionTrack.MOVEMENT) >= 1 ? 25.0 : 0.0;
    }

    public double getStaminaRecoveryMultiplier() {
        return getRank(ProgressionTrack.MOVEMENT) >= 2 ? 1.50 : 1.0;
    }

    public double getSprintStaminaDrainMultiplier() {
        return getRank(ProgressionTrack.MOVEMENT) >= 2 ? 0.65 : 1.0;
    }

    // --- EQUIPMENT BONUSES ---
    public int getItemCapacityBonus() {
        return getRank(ProgressionTrack.EQUIPMENT) >= 1 ? 2 : 0;
    }

    public double getThrowRangeMultiplier() {
        return getRank(ProgressionTrack.EQUIPMENT) >= 1 ? 1.40 : 1.0;
    }

    public double getStunCooldown() {
        return getRank(ProgressionTrack.EQUIPMENT) >= 2 ? 3.0 : 6.0;
    }

    public double getStunArcRange() {
        return getRank(ProgressionTrack.EQUIPMENT) >= 2 ? 135.0 : 90.0;
    }

    public double getEmpRadius() {
        return getRank(ProgressionTrack.EQUIPMENT) >= 2 ? 280.0 : 220.0;
    }

    public int getSkillPoints() { return skillPoints; }
    public int getTotalXp() { return totalXp; }
    public int getCurrentXp() { return currentXp; }
    public Map<ProgressionTrack, Integer> getRanks() { return Collections.unmodifiableMap(ranks); }

    public void setProgressionState(int totalXp, int currentXp, int skillPoints, Map<ProgressionTrack, Integer> newRanks) {
        this.totalXp = Math.max(0, totalXp);
        this.currentXp = Math.max(0, currentXp);
        this.skillPoints = Math.max(0, skillPoints);
        if (newRanks != null) {
            this.ranks.putAll(newRanks);
        }
    }
}
