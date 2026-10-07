package com.cyberheist.entity;

/**
 * Manages player stamina, consumption during sprint, exhaustion lockout,
 * and recovery cooldowns.
 */
public final class StaminaSystem {

    private final double maxStamina;
    private double currentStamina;

    private final double drainRate;    // Units per second while sprinting
    private final double regenRate;    // Units per second while recovering
    private final double regenDelay;   // Inactivity delay before regen begins (seconds)
    private final double recoveryThreshold; // Required stamina before exiting exhaustion state

    private double cooldownTimer = 0.0;
    private boolean exhausted = false;

    public StaminaSystem(double maxStamina, double drainRate, double regenRate,
                         double regenDelay, double recoveryThreshold) {
        this.maxStamina = maxStamina;
        this.currentStamina = maxStamina;
        this.drainRate = drainRate;
        this.regenRate = regenRate;
        this.regenDelay = regenDelay;
        this.recoveryThreshold = recoveryThreshold;
    }

    public static StaminaSystem createDefault() {
        return new StaminaSystem(100.0, 32.0, 22.0, 0.6, 20.0);
    }

    /**
     * Updates stamina levels based on delta time and sprinting activity.
     *
     * @param deltaTime  Elapsed frame time
     * @param isDraining True if currently consuming stamina (sprinting)
     */
    public void update(double deltaTime, boolean isDraining) {
        if (isDraining) {
            currentStamina -= drainRate * deltaTime;
            cooldownTimer = regenDelay; // Reset recovery cooldown

            if (currentStamina <= 0.0) {
                currentStamina = 0.0;
                exhausted = true; // Enter exhaustion penalty
            }
        } else {
            // Check recovery cooldown timer
            if (cooldownTimer > 0.0) {
                cooldownTimer -= deltaTime;
            } else if (currentStamina < maxStamina) {
                currentStamina += regenRate * deltaTime;
                if (currentStamina >= maxStamina) {
                    currentStamina = maxStamina;
                }

                // Clear exhaustion once above threshold
                if (exhausted && currentStamina >= recoveryThreshold) {
                    exhausted = false;
                }
            }
        }
    }

    /**
     * Checks if the player has sufficient stamina to maintain sprint.
     */
    public boolean canSprint() {
        return !exhausted && currentStamina > 0.0;
    }

    public boolean isExhausted() {
        return exhausted;
    }

    public double getCurrentStamina() {
        return currentStamina;
    }

    public void setCurrentStamina(double stamina) {
        this.currentStamina = Math.max(0.0, Math.min(maxStamina, stamina));
        this.exhausted = this.currentStamina <= 0.0;
    }

    public double getMaxStamina() {
        return maxStamina;
    }

    public double getPercentage() {
        return maxStamina > 0 ? (currentStamina / maxStamina) : 0.0;
    }

    public void reset() {
        this.currentStamina = maxStamina;
        this.exhausted = false;
        this.cooldownTimer = 0.0;
    }

    public void restoreFull() {
        this.currentStamina = maxStamina;
        this.exhausted = false;
        this.cooldownTimer = 0.0;
    }

    public void addStamina(double amount) {
        this.currentStamina = Math.min(maxStamina, this.currentStamina + amount);
        if (this.currentStamina >= recoveryThreshold) {
            this.exhausted = false;
        }
    }
}
