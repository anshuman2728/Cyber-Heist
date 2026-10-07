package com.cyberheist.scene;

import java.awt.Graphics2D;

/**
 * Interface defining the lifecycle of a game scene or state.
 * All concrete scenes (menus, playable stages, etc.) implement this contract.
 */
public interface Scene {

    /**
     * Called once when the scene is loaded or switched into view.
     */
    void initialize();

    /**
     * Executes scene-specific game logic.
     *
     * @param deltaTime Elapsed time in seconds since previous logic tick
     */
    void update(double deltaTime);

    /**
     * Renders scene visuals onto the provided Graphics2D context.
     *
     * @param g2d Active graphics context with anti-aliasing pre-configured
     */
    void render(Graphics2D g2d);

    /**
     * Called when the scene is unloaded to clean up resources.
     */
    void dispose();

    /**
     * Identifies the scene for diagnostics and logging.
     *
     * @return Readable name of the scene
     */
    default String getName() {
        return getClass().getSimpleName();
    }
}
