package com.cyberheist.stealth;

/**
 * Three-tier detection state model for realistic non-binary stealth.
 */
public enum DetectionState {
    UNAWARE(0.0, 25.0),
    SUSPICIOUS(25.0, 60.0),
    DETECTED(60.0, 100.0);

    private final double minPercent;
    private final double maxPercent;

    DetectionState(double minPercent, double maxPercent) {
        this.minPercent = minPercent;
        this.maxPercent = maxPercent;
    }

    public static DetectionState fromPercentage(double percentage) {
        if (percentage >= 60.0) {
            return DETECTED;
        } else if (percentage >= 25.0) {
            return SUSPICIOUS;
        } else {
            return UNAWARE;
        }
    }

    public double getMinPercent() { return minPercent; }
    public double getMaxPercent() { return maxPercent; }
}
