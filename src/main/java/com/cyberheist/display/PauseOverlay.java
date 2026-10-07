package com.cyberheist.display;

import com.cyberheist.audio.AudioManager;
import com.cyberheist.audio.SoundEffect;
import com.cyberheist.input.KeyboardInput;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

/**
 * Tactical In-Game Pause Menu overlay with keyboard navigation,
 * custom cyberpunk typography, and responsive audio feedback.
 */
public final class PauseOverlay {

    public enum PauseAction {
        NONE,
        RESUME,
        SAVE_GAME,
        LOAD_GAME,
        RESTART,
        OPEN_SETTINGS,
        QUIT_TO_TITLE
    }

    private static final String[] OPTIONS = {
            "RESUME OPERATION",
            "SAVE HEIST [F5]",
            "LOAD HEIST [F9]",
            "RESTART MISSION",
            "SETTINGS",
            "ABORT TO TITLE"
    };

    private int selectedIndex = 0;
    private double animTimer = 0.0;

    private static final Font TITLE_FONT = new Font("Monospaced", Font.BOLD, 26);
    private static final Font SUB_FONT = new Font("Monospaced", Font.PLAIN, 12);
    private static final Font OPTION_FONT = new Font("Monospaced", Font.BOLD, 16);

    public void update(double deltaTime) {
        animTimer += deltaTime;
    }

    public PauseAction handleInput(KeyboardInput keyboard) {
        if (keyboard.isKeyPressed(KeyEvent.VK_UP) || keyboard.isKeyPressed(KeyEvent.VK_W)) {
            selectedIndex = (selectedIndex - 1 + OPTIONS.length) % OPTIONS.length;
            AudioManager.getInstance().play(SoundEffect.HACK_CLICK);
            return PauseAction.NONE;
        }

        if (keyboard.isKeyPressed(KeyEvent.VK_DOWN) || keyboard.isKeyPressed(KeyEvent.VK_S)) {
            selectedIndex = (selectedIndex + 1) % OPTIONS.length;
            AudioManager.getInstance().play(SoundEffect.HACK_CLICK);
            return PauseAction.NONE;
        }

        if (keyboard.isKeyPressed(KeyEvent.VK_ENTER) || keyboard.isKeyPressed(KeyEvent.VK_SPACE)) {
            AudioManager.getInstance().play(SoundEffect.HACK_SUCCESS);
            switch (selectedIndex) {
                case 0: return PauseAction.RESUME;
                case 1: return PauseAction.SAVE_GAME;
                case 2: return PauseAction.LOAD_GAME;
                case 3: return PauseAction.RESTART;
                case 4: return PauseAction.OPEN_SETTINGS;
                case 5: return PauseAction.QUIT_TO_TITLE;
            }
        }

        return PauseAction.NONE;
    }

    public void render(Graphics2D g2d, int screenWidth, int screenHeight) {
        // Semi-transparent tactical dark backdrop
        g2d.setColor(new Color(4, 10, 20, 220));
        g2d.fillRect(0, 0, screenWidth, screenHeight);

        // Frame border
        int boxW = 460;
        int boxH = 430;
        int boxX = (screenWidth - boxW) / 2;
        int boxY = (screenHeight - boxH) / 2;

        g2d.setColor(new Color(15, 30, 48, 240));
        g2d.fillRect(boxX, boxY, boxW, boxH);

        g2d.setColor(new Color(0, 220, 255, 180));
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.drawRect(boxX, boxY, boxW, boxH);

        // Tactical Corner brackets
        int cSize = 12;
        g2d.setColor(new Color(0, 255, 180));
        g2d.drawLine(boxX - 2, boxY - 2, boxX - 2 + cSize, boxY - 2);
        g2d.drawLine(boxX - 2, boxY - 2, boxX - 2, boxY - 2 + cSize);
        g2d.drawLine(boxX + boxW + 2, boxY - 2, boxX + boxW + 2 - cSize, boxY - 2);
        g2d.drawLine(boxX + boxW + 2, boxY - 2, boxX + boxW + 2, boxY - 2 + cSize);

        // Header Title
        g2d.setFont(TITLE_FONT);
        g2d.setColor(new Color(0, 240, 255));
        String title = "MISSION SUSPENDED";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(title, boxX + (boxW - fm.stringWidth(title)) / 2, boxY + 45);

        g2d.setFont(SUB_FONT);
        g2d.setColor(new Color(120, 160, 180));
        String sub = "// TACTICAL HUD FROZEN // OPERATOR ON STANDBY";
        fm = g2d.getFontMetrics();
        g2d.drawString(sub, boxX + (boxW - fm.stringWidth(sub)) / 2, boxY + 68);

        // Divider
        g2d.setColor(new Color(0, 200, 255, 70));
        g2d.drawLine(boxX + 30, boxY + 85, boxX + boxW - 30, boxY + 85);

        // Options List
        g2d.setFont(OPTION_FONT);
        int optStartY = boxY + 120;
        int optSpacing = 44;

        for (int i = 0; i < OPTIONS.length; i++) {
            int itemY = optStartY + i * optSpacing;
            boolean selected = (i == selectedIndex);

            if (selected) {
                // Highlight box
                double pulse = (Math.sin(animTimer * 6.0) + 1.0) / 2.0;
                int alpha = (int) (40 + pulse * 45);
                g2d.setColor(new Color(0, 230, 255, alpha));
                g2d.fillRect(boxX + 40, itemY - 24, boxW - 80, 34);

                g2d.setColor(new Color(0, 240, 255));
                g2d.drawRect(boxX + 40, itemY - 24, boxW - 80, 34);

                g2d.setColor(new Color(0, 255, 180));
                g2d.drawString("► " + OPTIONS[i], boxX + 60, itemY);
            } else {
                g2d.setColor(new Color(150, 180, 200));
                g2d.drawString("  " + OPTIONS[i], boxX + 60, itemY);
            }
        }

        // Instructions Footer
        g2d.setFont(SUB_FONT);
        g2d.setColor(new Color(90, 130, 150));
        String hint = "[W/S or UP/DOWN] Select   [ENTER] Confirm   [ESC] Resume";
        fm = g2d.getFontMetrics();
        g2d.drawString(hint, boxX + (boxW - fm.stringWidth(hint)) / 2, boxY + boxH - 20);
    }

    public void reset() {
        selectedIndex = 0;
    }
}
