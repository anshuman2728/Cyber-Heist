package com.cyberheist.core;

import com.cyberheist.util.Logger;

/**
 * High-precision game loop coordinating delta-time logic updates and rendering frames.
 * Uses a fixed-step accumulator pattern to ensure deterministic simulation
 * while preventing timing spirals of death.
 */
public final class GameLoop implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(GameLoop.class);

    private final GameEngine engine;
    private volatile boolean running = false;
    private Thread gameThread;

    private int currentFps = 0;
    private int currentUps = 0;

    public GameLoop(GameEngine engine) {
        this.engine = engine;
    }

    /**
     * Starts the game loop on a dedicated background thread.
     */
    public synchronized void start() {
        if (running) {
            LOGGER.warn("GameLoop is already running.");
            return;
        }

        running = true;
        gameThread = new Thread(this, "GameLoop-Thread");
        gameThread.start();
        LOGGER.info("GameLoop thread started.");
    }

    /**
     * Signals the loop to stop and waits for the thread to terminate.
     */
    public synchronized void stop() {
        if (!running) {
            return;
        }

        LOGGER.info("Stopping GameLoop thread...");
        running = false;
        if (gameThread != null) {
            try {
                gameThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.error("GameLoop thread join interrupted", e);
            }
        }
        LOGGER.info("GameLoop thread terminated.");
    }

    @Override
    public void run() {
        final double updateInterval = 1.0 / GameConfig.TARGET_UPS;
        final long targetFrameNanos = 1_000_000_000L / GameConfig.TARGET_FPS;

        long lastTime = System.nanoTime();
        double accumulator = 0.0;

        long secondTimer = System.currentTimeMillis();
        int frameCounter = 0;
        int updateCounter = 0;

        LOGGER.info("GameLoop running. Target UPS=" + GameConfig.TARGET_UPS + ", Target FPS=" + GameConfig.TARGET_FPS);

        while (running) {
            long frameStartTime = System.nanoTime();
            double elapsedTime = (frameStartTime - lastTime) / 1_000_000_000.0;
            lastTime = frameStartTime;

            // Clamp delta time to prevent spiral of death during debugging/pauses
            if (elapsedTime > GameConfig.MAX_DELTA_TIME) {
                elapsedTime = GameConfig.MAX_DELTA_TIME;
            }

            accumulator += elapsedTime;

            // Fixed timestep logic updates
            while (accumulator >= updateInterval) {
                engine.tick(updateInterval);
                updateCounter++;
                accumulator -= updateInterval;
            }

            // Render pass with latest telemetry
            engine.render(currentFps, currentUps, elapsedTime);
            frameCounter++;

            // Update FPS & UPS metrics every second
            long currentTime = System.currentTimeMillis();
            if (currentTime - secondTimer >= 1000) {
                currentFps = frameCounter;
                currentUps = updateCounter;
                frameCounter = 0;
                updateCounter = 0;
                secondTimer += 1000;
            }

            // Frame rate capping to avoid unnecessary CPU spin
            long frameDurationNanos = System.nanoTime() - frameStartTime;
            long sleepNanos = targetFrameNanos - frameDurationNanos;
            if (sleepNanos > 2_000_000L) { // If more than 2ms remaining, sleep
                try {
                    Thread.sleep(sleepNanos / 1_000_000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } else if (sleepNanos > 0) {
                Thread.yield();
            }
        }
    }

    public boolean isRunning() {
        return running;
    }

    public int getCurrentFps() {
        return currentFps;
    }

    public int getCurrentUps() {
        return currentUps;
    }
}
