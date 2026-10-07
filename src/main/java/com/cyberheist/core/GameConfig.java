package com.cyberheist.core;

import java.awt.Color;
import java.awt.Font;

/**
 * Immutable configuration constants for Cyber Heist.
 * Centralizes engine parameters, rendering dimensions, and styling constants.
 */
public final class GameConfig {

    private GameConfig() {
        // Prevent instantiation
    }

    // Application Metadata
    public static final String GAME_TITLE = "Cyber Heist";
    public static final String GAME_VERSION = "0.1.0-alpha (Phase 1)";

    // Window & Canvas Settings
    public static final int WINDOW_WIDTH = 1280;
    public static final int WINDOW_HEIGHT = 720;
    public static final boolean WINDOW_RESIZABLE = false;

    // Game Loop Timing Settings
    public static final int TARGET_UPS = 60; // Fixed update ticks per second
    public static final int TARGET_FPS = 60; // Target render frames per second
    public static final double MAX_DELTA_TIME = 0.25; // Clamps delta time to prevent spiral of death

    // Graphics & Buffering
    public static final int BUFFER_COUNT = 3; // Triple buffering strategy

    // Cyberpunk Theme Palette
    public static final Color COLOR_BACKGROUND = new Color(0x0A, 0x0E, 0x17); // Dark navy/abyss
    public static final Color COLOR_GRID_LINE = new Color(0x15, 0x22, 0x38);   // Faint circuit grid
    public static final Color COLOR_CYAN = new Color(0x00, 0xF0, 0xFF);        // Neon cyan
    public static final Color COLOR_MAGENTA = new Color(0xFF, 0x00, 0x7F);     // Neon magenta
    public static final Color COLOR_GREEN = new Color(0x00, 0xFF, 0x66);       // Terminal green
    public static final Color COLOR_TEXT_MUTED = new Color(0x8A, 0x9B, 0xAE);  // Slate muted text
    public static final Color COLOR_TEXT_BRIGHT = new Color(0xED, 0xF2, 0xF7); // Off-white

    // Default Fonts
    public static final Font FONT_TITLE = new Font("Consolas", Font.BOLD, 22);
    public static final Font FONT_UI = new Font("Consolas", Font.PLAIN, 14);
    public static final Font FONT_CODE = new Font("Monospaced", Font.PLAIN, 12);

    // Diagnostics Toggles
    public static final boolean DEBUG_OVERLAY_ENABLED = true;
    public static final boolean DEBUG_GRID_ENABLED = true;
}
