package com.cyberheist.scene;

import com.cyberheist.audio.AudioManager;
import com.cyberheist.audio.SoundEffect;
import com.cyberheist.core.GameConfig;
import com.cyberheist.core.GameEngine;
import com.cyberheist.display.SettingsOverlay;
import com.cyberheist.input.KeyboardInput;
import com.cyberheist.particle.ParticleSystem;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

/**
 * High-polish Cyberpunk Main Menu Scene featuring:
 * - Animated data stream particle backdrop
 * - Mission deployment launcher
 * - Embedded tactical controls field guide
 * - Audio-visual settings manager integration
 */
public final class MainMenuScene implements Scene {

    private final GameEngine engine;
    private final ParticleSystem particles = new ParticleSystem();
    private final SettingsOverlay settingsOverlay = new SettingsOverlay();

    private enum MenuState {
        MAIN,
        FIELD_GUIDE,
        SETTINGS
    }

    private MenuState state = MenuState.MAIN;

    private static final String[] MENU_ITEMS = {
            "DEPLOY MISSION 1: GHOST IN THE SERVER",
            "CONTINUE HEIST // LOAD ARCHIVE",
            "TACTICAL FIELD GUIDE & CONTROLS",
            "SETTINGS",
            "TERMINATE APPLICATION"
    };

    private int selectedIndex = 0;
    private double animTimer = 0.0;

    private static final Font LOGO_FONT = new Font("Monospaced", Font.BOLD, 42);
    private static final Font SUB_FONT = new Font("Monospaced", Font.BOLD, 14);
    private static final Font ITEM_FONT = new Font("Monospaced", Font.BOLD, 16);
    private static final Font GUIDE_TITLE_FONT = new Font("Monospaced", Font.BOLD, 20);
    private static final Font GUIDE_BODY_FONT = new Font("Monospaced", Font.PLAIN, 13);
    private static final Font FOOTER_FONT = new Font("Monospaced", Font.PLAIN, 12);

    public MainMenuScene(GameEngine engine) {
        this.engine = engine;
    }

    @Override
    public void initialize() {
        AudioManager.getInstance().startAmbient();
    }

    @Override
    public void update(double deltaTime) {
        animTimer += deltaTime;
        particles.update(deltaTime);

        // Periodically spawn floating matrix data bits in background
        if (Math.random() < 0.25) {
            double rx = Math.random() * GameConfig.WINDOW_WIDTH;
            particles.spawnDataBits(rx, GameConfig.WINDOW_HEIGHT + 10, 1);
        }

        KeyboardInput keyboard = engine.getKeyboardInput();
        if (keyboard == null) return;

        if (state == MenuState.SETTINGS) {
            settingsOverlay.update(deltaTime);
            if (settingsOverlay.handleInput(keyboard)) {
                state = MenuState.MAIN;
            }
            return;
        }

        if (state == MenuState.FIELD_GUIDE) {
            if (keyboard.isKeyPressed(KeyEvent.VK_ESCAPE) || keyboard.isKeyPressed(KeyEvent.VK_ENTER) || keyboard.isKeyPressed(KeyEvent.VK_SPACE)) {
                AudioManager.getInstance().play(SoundEffect.HACK_SUCCESS);
                state = MenuState.MAIN;
            }
            return;
        }

        // Main Menu navigation
        if (keyboard.isKeyPressed(KeyEvent.VK_UP) || keyboard.isKeyPressed(KeyEvent.VK_W)) {
            selectedIndex = (selectedIndex - 1 + MENU_ITEMS.length) % MENU_ITEMS.length;
            AudioManager.getInstance().play(SoundEffect.HACK_CLICK);
        }

        if (keyboard.isKeyPressed(KeyEvent.VK_DOWN) || keyboard.isKeyPressed(KeyEvent.VK_S)) {
            selectedIndex = (selectedIndex + 1) % MENU_ITEMS.length;
            AudioManager.getInstance().play(SoundEffect.HACK_CLICK);
        }

        if (keyboard.isKeyPressed(KeyEvent.VK_ENTER) || keyboard.isKeyPressed(KeyEvent.VK_SPACE)) {
            AudioManager.getInstance().play(SoundEffect.HACK_SUCCESS);
            switch (selectedIndex) {
                case 0: // Start Mission
                    engine.getSceneManager().setScene(new EmptyPlayableScene(engine));
                    break;
                case 1: // Continue Heist
                    EmptyPlayableScene savedScene = new EmptyPlayableScene(engine);
                    engine.getSceneManager().setScene(savedScene);
                    savedScene.quickLoad();
                    break;
                case 2: // Field Guide
                    state = MenuState.FIELD_GUIDE;
                    break;
                case 3: // Settings
                    state = MenuState.SETTINGS;
                    break;
                case 4: // Exit
                    engine.stop();
                    System.exit(0);
                    break;
            }
        }
    }

    @Override
    public void render(Graphics2D g2d) {
        int width = GameConfig.WINDOW_WIDTH;
        int height = GameConfig.WINDOW_HEIGHT;

        // Dark navy atmospheric background
        g2d.setColor(new Color(3, 7, 14));
        g2d.fillRect(0, 0, width, height);

        // Cyberpunk blueprint grid
        g2d.setColor(new Color(0, 180, 240, 12));
        for (int x = 0; x < width; x += 36) {
            g2d.drawLine(x, 0, x, height);
        }
        for (int y = 0; y < height; y += 36) {
            g2d.drawLine(0, y, width, y);
        }

        // Render ambient floating data bits
        particles.render(g2d);

        if (state == MenuState.SETTINGS) {
            settingsOverlay.render(g2d, width, height);
            return;
        }

        if (state == MenuState.FIELD_GUIDE) {
            renderFieldGuide(g2d, width, height);
            return;
        }

        renderMainMenu(g2d, width, height);
    }

    private void renderMainMenu(Graphics2D g2d, int width, int height) {
        int centerX = width / 2;

        // Title Glow & Logo
        double pulse = (Math.sin(animTimer * 3.5) + 1.0) / 2.0;
        int glowAlpha = (int) (40 + pulse * 40);
        g2d.setColor(new Color(0, 240, 255, glowAlpha));
        g2d.setFont(LOGO_FONT);
        String logo = "CYBER HEIST";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(logo, centerX - fm.stringWidth(logo) / 2 - 2, 172);
        g2d.drawString(logo, centerX - fm.stringWidth(logo) / 2 + 2, 172);

        g2d.setColor(new Color(0, 255, 230));
        g2d.drawString(logo, centerX - fm.stringWidth(logo) / 2, 170);

        // Subtitle
        g2d.setFont(SUB_FONT);
        g2d.setColor(new Color(0, 255, 170));
        String sub = "// TACTICAL STEALTH INFILTRATION // PROTOCOL NOIDA";
        fm = g2d.getFontMetrics();
        g2d.drawString(sub, centerX - fm.stringWidth(sub) / 2, 205);

        // Divider
        g2d.setColor(new Color(0, 200, 255, 70));
        g2d.drawLine(centerX - 240, 225, centerX + 240, 225);

        // Menu items
        g2d.setFont(ITEM_FONT);
        int startY = 285;
        int spacing = 52;

        for (int i = 0; i < MENU_ITEMS.length; i++) {
            int itemY = startY + i * spacing;
            boolean selected = (i == selectedIndex);

            if (selected) {
                double itemPulse = (Math.sin(animTimer * 7.0) + 1.0) / 2.0;
                int alpha = (int) (40 + itemPulse * 45);
                g2d.setColor(new Color(0, 220, 255, alpha));
                g2d.fillRect(centerX - 220, itemY - 26, 440, 36);

                g2d.setColor(new Color(0, 240, 255));
                g2d.setStroke(new BasicStroke(1.5f));
                g2d.drawRect(centerX - 220, itemY - 26, 440, 36);

                g2d.setColor(new Color(0, 255, 190));
                g2d.drawString("►  " + MENU_ITEMS[i], centerX - 190, itemY);
            } else {
                g2d.setColor(new Color(130, 165, 190));
                g2d.drawString("   " + MENU_ITEMS[i], centerX - 190, itemY);
            }
        }

        // Footer & Copyright
        g2d.setFont(FOOTER_FONT);
        g2d.setColor(new Color(80, 120, 145));
        String footer = "CYBER HEIST v1.0.0 // NOIDA HIGH-SECURITY FACILITY // WASD OR ARROWS TO NAVIGATE";
        fm = g2d.getFontMetrics();
        g2d.drawString(footer, centerX - fm.stringWidth(footer) / 2, height - 30);
    }

    private void renderFieldGuide(Graphics2D g2d, int width, int height) {
        int boxW = 680;
        int boxH = 500;
        int boxX = (width - boxW) / 2;
        int boxY = (height - boxH) / 2;

        g2d.setColor(new Color(8, 16, 28, 240));
        g2d.fillRect(boxX, boxY, boxW, boxH);

        g2d.setColor(new Color(0, 220, 255, 180));
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.drawRect(boxX, boxY, boxW, boxH);

        g2d.setFont(GUIDE_TITLE_FONT);
        g2d.setColor(new Color(0, 240, 255));
        String title = "OPERATOR TACTICAL FIELD GUIDE";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(title, boxX + (boxW - fm.stringWidth(title)) / 2, boxY + 45);

        g2d.setFont(GUIDE_BODY_FONT);
        g2d.setColor(new Color(180, 220, 240));

        String[][] controls = {
                {"MOVEMENT & AGILITY", "W, A, S, D  (Hold SHIFT to sprint; consumes stamina)"},
                {"INTERACTIONS", "E  (Bypass doors, hack terminals, access servers)"},
                {"HACKING CONTROLS", "1-4 keys to select hexadecimal memory buffer address"},
                {"STUN DEVICE", "KEY 1  (Zap & incapacitate guard within 130px for 5.0s)"},
                {"EMP BLAST DEVICE", "KEY 2  (Deploy electromagnetic pulse; shuts down cameras)"},
                {"DISTRACTION DECOY", "KEY 3  (Throw sonic acoustic ping lure up to 180px)"},
                {"SILENT TAKEDOWN", "F / SPACEBAR (Execute non-lethal sleeper hold from behind)"},
                {"TACTICAL ARCHIVE", "F5 Quick Save | F9 Quick Load (Restores full game state)"},
                {"TACTICAL PAUSE", "ESCAPE  (Suspend mission, access pause menu, settings)"}
        };

        int guideY = boxY + 95;
        for (String[] entry : controls) {
            g2d.setColor(new Color(0, 255, 180));
            g2d.drawString(entry[0] + ":", boxX + 40, guideY);
            g2d.setColor(new Color(200, 225, 240));
            g2d.drawString(entry[1], boxX + 225, guideY);
            guideY += 40;
        }

        g2d.setColor(new Color(100, 150, 180));
        String backPrompt = "[PRESS ENTER / ESCAPE TO RETURN TO MENU]";
        fm = g2d.getFontMetrics();
        g2d.drawString(backPrompt, boxX + (boxW - fm.stringWidth(backPrompt)) / 2, boxY + boxH - 24);
    }

    @Override
    public void dispose() {
        particles.clear();
    }
}
