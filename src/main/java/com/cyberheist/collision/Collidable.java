package com.cyberheist.collision;

/**
 * Contract for any object that occupies physical space in the game world.
 */
public interface Collidable {

    /**
     * Returns the axis-aligned collision bounds.
     */
    BoundingBox getBoundingBox();

    /**
     * Indicates whether this collidable blocks movement.
     */
    default boolean isSolid() {
        return true;
    }
}
