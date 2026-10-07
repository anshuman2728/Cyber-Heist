package com.cyberheist.core;

import com.cyberheist.display.GameWindow;
import com.cyberheist.display.Renderer;
import com.cyberheist.input.KeyboardInput;
import com.cyberheist.input.MouseInput;
import com.cyberheist.scene.Scene;
import com.cyberheist.scene.SceneManager;
import com.cyberheist.util.Logger;

import java.awt.event.KeyEvent;

/**
 * Core engine coordinator for Cyber Heist.
 * Wires together input, display, rendering, scene state, and game loop subsystems.
 */
public final class GameEngine {

    private static final Logger LOGGER = Logger.getLogger(GameEngine.class);

    private final KeyboardInput keyboardInput;
    private final MouseInput mouseInput;
    private final GameWindow gameWindow;
    private final Renderer renderer;
    private final SceneManager sceneManager;
    private final GameLoop gameLoop;

    private boolean initialized = false;

    public GameEngine() {
        LOGGER.info("Bootstrapping Cyber Heist Engine...");

        this.keyboardInput = new KeyboardInput();
        this.mouseInput = new MouseInput();
        this.sceneManager = new SceneManager();

        this.gameWindow = new GameWindow(
                GameConfig.GAME_TITLE + " - " + GameConfig.GAME_VERSION,
                GameConfig.WINDOW_WIDTH,
                GameConfig.WINDOW_HEIGHT,
                this.keyboardInput,
                this.mouseInput,
                this::stop
        );

        this.renderer = new Renderer(this.gameWindow);
        this.gameLoop = new GameLoop(this);
    }

    /**
     * Initializes the window, creates hardware buffers, and sets the starting scene.
     */
    public synchronized void initialize(Scene initialScene) {
        if (initialized) {
            LOGGER.warn("GameEngine is already initialized.");
            return;
        }

        this.gameWindow.show();
        this.sceneManager.setScene(initialScene);
        this.initialized = true;
        LOGGER.info("Cyber Heist Engine initialization complete.");
    }

    /**
     * Starts the core loop execution.
     */
    public synchronized void start() {
        if (!initialized) {
            throw new IllegalStateException("GameEngine must be initialized before starting.");
        }
        this.gameLoop.start();
    }

    /**
     * Executes one fixed logic update tick.
     *
     * @param deltaTime Elapsed fixed time step in seconds
     */
    public void tick(double deltaTime) {
        // 1. Synchronize input buffers
        keyboardInput.poll();
        mouseInput.poll();

        // 2. Global engine shortcuts (e.g. F3 to toggle debug overlay)
        if (keyboardInput.isKeyPressed(KeyEvent.VK_F3)) {
            renderer.toggleDebugOverlay();
            LOGGER.debug("Debug overlay toggled: " + renderer.isShowDebugOverlay());
        }

        // 3. Update active scene logic
        sceneManager.update(deltaTime);
    }

    /**
     * Executes one render frame.
     *
     * @param fps       Current measured frames per second
     * @param ups       Current measured updates per second
     * @param deltaTime Elapsed frame time in seconds
     */
    public void render(int fps, int ups, double deltaTime) {
        renderer.render(sceneManager.getCurrentScene(), fps, ups, deltaTime);
    }

    /**
     * Shuts down the engine gracefully.
     */
    public synchronized void stop() {
        LOGGER.info("Initiating engine shutdown sequence...");
        this.gameLoop.stop();
        this.sceneManager.dispose();
        this.gameWindow.dispose();
        LOGGER.info("Cyber Heist Engine shutdown complete.");
    }

    public KeyboardInput getKeyboardInput() {
        return keyboardInput;
    }

    public MouseInput getMouseInput() {
        return mouseInput;
    }

    public SceneManager getSceneManager() {
        return sceneManager;
    }

    public Renderer getRenderer() {
        return renderer;
    }

    public GameWindow getGameWindow() {
        return gameWindow;
    }
}
