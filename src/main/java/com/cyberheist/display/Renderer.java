package com.cyberheist.display;

import com.cyberheist.core.GameConfig;
import com.cyberheist.scene.Scene;
import com.cyberheist.util.Logger;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferStrategy;

/**
 * Encapsulates the 2D rendering pipeline using Graphics2D and BufferStrategy.
 * Handles rendering hints, screen clearing, and debug telemetry overlays.
 */
public final class Renderer {

    private static final Logger LOGGER = Logger.getLogger(Renderer.class);

    private final GameWindow gameWindow;
    private boolean showDebugOverlay = GameConfig.DEBUG_OVERLAY_ENABLED;

    public Renderer(GameWindow gameWindow) {
        this.gameWindow = gameWindow;
    }

    /**
     * Executes a full render pass for the active scene.
     *
     * @param activeScene Current active scene to render
     * @param fps         Current measured frames per second
     * @param ups         Current measured updates per second
     * @param deltaTime   Last frame delta time in seconds
     */
    public void render(Scene activeScene, int fps, int ups, double deltaTime) {
        BufferStrategy bs = gameWindow.getBufferStrategy();
        if (bs == null) {
            return;
        }

        Graphics2D g2d = null;
        try {
            g2d = (Graphics2D) bs.getDrawGraphics();

            // Configure rendering quality
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            // 1. Clear background
            g2d.setColor(GameConfig.COLOR_BACKGROUND);
            g2d.fillRect(0, 0, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);

            // 2. Render active scene content
            if (activeScene != null) {
                activeScene.render(g2d);
            }

            // 3. Render telemetry overlay if enabled
            if (showDebugOverlay) {
                renderDebugOverlay(g2d, fps, ups, deltaTime);
            }

        } catch (Exception e) {
            LOGGER.error("Error occurred during frame render", e);
        } finally {
            if (g2d != null) {
                g2d.dispose();
            }
        }

        // Show contents of buffer
        if (!bs.contentsLost()) {
            bs.show();
        }
    }

    private void renderDebugOverlay(Graphics2D g2d, int fps, int ups, double deltaTime) {
        int x = 16;
        int y = 24;

        g2d.setFont(GameConfig.FONT_CODE);

        // Semi-transparent tactical telemetry card
        int boxW = 280;
        int boxH = 175;
        g2d.setColor(new Color(6, 12, 22, 225));
        g2d.fillRoundRect(x - 8, y - 16, boxW, boxH, 8, 8);
        g2d.setColor(new Color(0, 220, 255, 140));
        g2d.drawRoundRect(x - 8, y - 16, boxW, boxH, 8, 8);

        com.cyberheist.core.TelemetryService telem = com.cyberheist.core.TelemetryService.getInstance();
        telem.updateMetrics(fps, ups, deltaTime, 0, 0);

        // 1. Frame Rate & Engine Tick
        g2d.setColor(GameConfig.COLOR_CYAN);
        g2d.drawString(String.format("FRAME RATE:   %3d FPS | %3d UPS", fps, ups), x, y);

        // 2. Frame Time & Delta
        g2d.setColor(GameConfig.COLOR_TEXT_BRIGHT);
        g2d.drawString(String.format("FRAME TIME:   %.2f ms (dt: %.4fs)", deltaTime * 1000.0, deltaTime), x, y + 20);

        // 3. Memory & GC
        long usedMem = telem.getUsedMemoryMb();
        long totalMem = telem.getTotalMemoryMb();
        long maxMem = telem.getMaxMemoryMb();
        long gcCount = telem.getTotalGcCount();
        g2d.setColor(new Color(100, 230, 180));
        g2d.drawString(String.format("HEAP MEMORY:  %d MB / %d MB (%d MB max)", usedMem, totalMem, maxMem), x, y + 40);
        g2d.setColor(GameConfig.COLOR_TEXT_MUTED);
        g2d.drawString(String.format("GC RUNS:      %d collections", gcCount), x, y + 60);

        // 4. CPU & Architecture
        int cores = telem.getAvailableProcessors();
        g2d.setColor(new Color(220, 200, 100));
        g2d.drawString(String.format("HARDWARE:     %d logical CPU cores", cores), x, y + 80);

        // 5. A* Pathfinding Telemetry
        g2d.setColor(new Color(255, 140, 200));
        g2d.drawString(String.format("A* SEARCHES:  %d total (%.1f/s)", telem.getTotalPathSearches(), telem.getSearchesPerSecond()), x, y + 100);
        g2d.drawString(String.format("AVG A* TIME:  %.1f µs", telem.getAvgPathSearchTimeMicros()), x, y + 120);

        // 6. Active Entity & Particle Counts
        g2d.setColor(GameConfig.COLOR_TEXT_BRIGHT);
        g2d.drawString(String.format("ENTITIES:     %d active (ptcl: %d)",
                telem.getActiveEntityCount(), telem.getActiveParticleCount()), x, y + 140);

        g2d.setColor(GameConfig.COLOR_GREEN);
        g2d.drawString("[F3] Close Diagnostic HUD", x, y + 160);
    }

    public void toggleDebugOverlay() {
        this.showDebugOverlay = !this.showDebugOverlay;
    }

    public boolean isShowDebugOverlay() {
        return showDebugOverlay;
    }

    public void setShowDebugOverlay(boolean showDebugOverlay) {
        this.showDebugOverlay = showDebugOverlay;
    }
}
