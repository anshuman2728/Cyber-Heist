package com.cyberheist.hacking;

/**
 * Interface implemented by any interactive game entity that can be breached
 * via the Network Node hacking minigame.
 */
public interface Hackable {

    /**
     * Determines the complexity and countermeasure intensity of the hack.
     */
    HackingDifficulty getDifficulty();

    /**
     * Human-readable label displayed on the hacking terminal header.
     */
    String getTargetName();

    /**
     * Executed when the player successfully sequences the network nodes.
     */
    void onHackSuccess();

    /**
     * Executed when time expires, trace reaches 100%, or the hack fails.
     */
    void onHackFailure();

    /**
     * Whether this object has already been breached.
     */
    boolean isHacked();

    /**
     * Whether this object is currently eligible for hacking.
     */
    default boolean canBeHacked() {
        return !isHacked();
    }
}
