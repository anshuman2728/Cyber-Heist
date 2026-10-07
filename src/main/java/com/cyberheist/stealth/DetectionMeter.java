package com.cyberheist.stealth;

/**
 * Tracks progressive detection percentage [0.0% to 100.0%] and handles decay.
 */
public final class DetectionMeter {

    private double percentage = 0.0;
    private final double baseBuildRate; // Max percentage build per second at 100% visibility
    private final double decayRate;     // Percentage reduction per second when unobserved

    public DetectionMeter(double baseBuildRate, double decayRate) {
        this.baseBuildRate = baseBuildRate;
        this.decayRate = decayRate;
    }

    public static DetectionMeter createDefault() {
        return new DetectionMeter(65.0, 24.0);
    }

    /**
     * Updates detection level based on visibility factors and delta time.
     *
     * @param deltaTime   Elapsed frame time
     * @param visible     Whether player is within FOV with clear LOS
     * @param rateFactor  Combined modifier from proximity, angle, cover, movement, and lighting [0.0 to 1.0+]
     */
    public void update(double deltaTime, boolean visible, double rateFactor) {
        if (visible && rateFactor > 0.0) {
            percentage += baseBuildRate * rateFactor * deltaTime;
            if (percentage > 100.0) {
                percentage = 100.0;
            }
        } else {
            // Decay when not in sight
            percentage -= decayRate * deltaTime;
            if (percentage < 0.0) {
                percentage = 0.0;
            }
        }
    }

    public void addInstantDetection(double amount) {
        percentage = Math.max(0.0, Math.min(100.0, percentage + amount));
    }

    public DetectionState getState() {
        return DetectionState.fromPercentage(percentage);
    }

    public double getPercentage() {
        return percentage;
    }

    public void reset() {
        this.percentage = 0.0;
    }
}
