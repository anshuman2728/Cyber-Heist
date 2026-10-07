package com.cyberheist.scene;

import com.cyberheist.util.Logger;

import java.awt.Graphics2D;

/**
 * Manages scene lifecycle transitions and delegates frame updates/rendering
 * to the currently active Scene.
 */
public final class SceneManager {

    private static final Logger LOGGER = Logger.getLogger(SceneManager.class);

    private Scene currentScene;
    private Scene nextScene;
    private final Object transitionLock = new Object();

    public SceneManager() {
        this.currentScene = null;
        this.nextScene = null;
    }

    /**
     * Schedules a scene transition to occur during the next lifecycle update.
     *
     * @param scene The new scene to transition to
     */
    public void setScene(Scene scene) {
        synchronized (transitionLock) {
            this.nextScene = scene;
        }
    }

    /**
     * Applies pending scene transitions, then updates the active scene.
     *
     * @param deltaTime Elapsed delta time in seconds
     */
    public void update(double deltaTime) {
        processPendingTransition();

        if (currentScene != null) {
            currentScene.update(deltaTime);
        }
    }

    /**
     * Delegates rendering to the active scene.
     *
     * @param g2d Active graphics context
     */
    public void render(Graphics2D g2d) {
        if (currentScene != null) {
            currentScene.render(g2d);
        }
    }

    private void processPendingTransition() {
        Scene pending;
        synchronized (transitionLock) {
            pending = this.nextScene;
            this.nextScene = null;
        }

        if (pending != null) {
            if (currentScene != null) {
                LOGGER.info("Disposing scene: " + currentScene.getName());
                try {
                    currentScene.dispose();
                } catch (Exception e) {
                    LOGGER.error("Error during scene disposal: " + currentScene.getName(), e);
                }
            }

            currentScene = pending;
            LOGGER.info("Initializing scene: " + currentScene.getName());
            try {
                currentScene.initialize();
            } catch (Exception e) {
                LOGGER.error("Error during scene initialization: " + currentScene.getName(), e);
            }
        }
    }

    public Scene getCurrentScene() {
        return currentScene;
    }

    public void dispose() {
        synchronized (transitionLock) {
            if (currentScene != null) {
                LOGGER.info("Disposing active scene on engine shutdown: " + currentScene.getName());
                currentScene.dispose();
                currentScene = null;
            }
            nextScene = null;
        }
    }
}
