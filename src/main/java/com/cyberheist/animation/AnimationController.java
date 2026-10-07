package com.cyberheist.animation;

import com.cyberheist.entity.PlayerState;

/**
 * Procedural and frame-based animation foundation.
 * Tracks state timers, stride oscillation, and visual feedback phases.
 */
public final class AnimationController {

    private PlayerState currentState = PlayerState.IDLE;
    private double stateTimer = 0.0;
    private double strideCycle = 0.0;

    public void update(double deltaTime, PlayerState state, double currentSpeed) {
        if (this.currentState != state) {
            this.currentState = state;
            this.stateTimer = 0.0;
        }

        this.stateTimer += deltaTime;

        // Advance stride cycle based on movement speed
        if (state == PlayerState.WALKING || state == PlayerState.RUNNING) {
            double frequency = (state == PlayerState.RUNNING) ? 14.0 : 8.0;
            this.strideCycle += deltaTime * frequency;
        } else {
            // Decay stride to neutral
            this.strideCycle = 0.0;
        }
    }

    /**
     * Stride oscillation offset (-1.0 to 1.0) for procedural walking/running motion.
     */
    public double getStrideSine() {
        return Math.sin(strideCycle);
    }

    /**
     * Idle breathing pulse (0.0 to 1.0).
     */
    public double getIdlePulse() {
        return (Math.sin(stateTimer * 3.0) + 1.0) / 2.0;
    }

    /**
     * Stun twitch factor.
     */
    public double getStunFlicker() {
        return Math.sin(stateTimer * 20.0);
    }

    public PlayerState getCurrentState() {
        return currentState;
    }

    public double getStateTimer() {
        return stateTimer;
    }
}
