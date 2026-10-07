package com.cyberheist.interact;

import com.cyberheist.collision.BoundingBox;

/**
 * Interface for interactive objects (consoles, doors, hacking nodes, lockboxes).
 * Designed to allow future gameplay systems to plug in seamlessly.
 */
public interface Interactable {

    /**
     * Text prompt displayed when the player enters interaction proximity.
     */
    String getInteractionPrompt();

    default String getPrompt() {
        return getInteractionPrompt();
    }

    /**
     * Determines whether the player meets interaction preconditions.
     */
    boolean canInteract();

    /**
     * Triggered when the player initiates interaction.
     */
    void onInteract();

    /**
     * Spatial bounds defining the trigger zone.
     */
    BoundingBox getInteractionTriggerZone();

    /**
     * Identifies the interactable object for diagnostics.
     */
    String getName();
}
