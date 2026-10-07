package com.cyberheist.entity;

/**
 * Mutually exclusive high-level states for the player character.
 */
public enum PlayerState {
    IDLE,
    WALKING,
    RUNNING,
    INTERACTING,
    STUNNED,
    DEAD;

    /**
     * Determines whether the player can accept movement input while in this state.
     */
    public boolean canMove() {
        return this == IDLE || this == WALKING || this == RUNNING;
    }

    /**
     * Determines whether the player can initiate an interaction in this state.
     */
    public boolean canInteract() {
        return this == IDLE || this == WALKING || this == RUNNING;
    }
}
