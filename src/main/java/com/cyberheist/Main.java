package com.cyberheist;

import com.cyberheist.core.GameConfig;
import com.cyberheist.core.GameEngine;
import com.cyberheist.scene.EmptyPlayableScene;
import com.cyberheist.util.Logger;

import javax.swing.SwingUtilities;

/**
 * Application entry point for Cyber Heist.
 */
public final class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class);

    public static void main(String[] args) {
        LOGGER.info("========================================");
        LOGGER.info("Starting " + GameConfig.GAME_TITLE + " - " + GameConfig.GAME_VERSION);
        LOGGER.info("Runtime: Java " + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
        LOGGER.info("========================================");

        // Run UI creation on the Swing Event Dispatch Thread as per standard AWT/Swing practice
        SwingUtilities.invokeLater(() -> {
            try {
                GameEngine engine = new GameEngine();
                com.cyberheist.scene.MainMenuScene initialScene = new com.cyberheist.scene.MainMenuScene(engine);

                // Register shutdown hook for clean resource disposal
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    LOGGER.info("JVM Shutdown detected. Ensuring engine stopped.");
                    engine.stop();
                }, "Shutdown-Hook"));

                engine.initialize(initialScene);
                engine.start();

            } catch (Exception e) {
                LOGGER.error("Fatal exception during engine launch", e);
                System.exit(1);
            }
        });
    }
}
