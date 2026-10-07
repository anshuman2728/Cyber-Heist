package com.cyberheist.display;

import com.cyberheist.audio.AudioManager;
import com.cyberheist.audio.SoundEffect;
import com.cyberheist.input.KeyboardInput;
import com.cyberheist.settings.SettingsManager;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

/**
 * Live Tactical Settings Overlay allowing real-time adjustment of:
 * - Master, SFX, and Ambient Volume
 * - Screen Shake Intensity
 * - CRT Scanline filter
 * - Camera Smoothing response
 */
public final class SettingsOverlay {

    private static final String[] SETTING_NAMES = {
            "MASTER VOLUME",
            "SFX VOLUME",
            "AMBIENT DRONE",
            "SCREEN SHAKE",
            "CRT SCANLINES",
            "CAMERA SMOOTHING",
            "<< BACK >>"
    };

    private int selectedIndex = 0;
    private double animTimer = 0.0;

    private static final Font TITLE_FONT = new Font("Monospaced", Font.BOLD, 24);
    private static final Font LABEL_FONT = new Font("Monospaced", Font.BOLD, 15);
    private static final Font VALUE_FONT = new Font("Monospaced", Font.BOLD, 15);
    private static final Font HINT_FONT = new Font("Monospaced", Font.PLAIN, 12);

    public void update(double deltaTime) {
        animTimer += deltaTime;
    }

    /**
     * Handles keyboard navigation. Returns true if user requested to close/return.
     */
    public boolean handleInput(KeyboardInput keyboard) {
        SettingsManager settings = SettingsManager.getInstance();

        if (keyboard.isKeyPressed(KeyEvent.VK_UP) || keyboard.isKeyPressed(KeyEvent.VK_W)) {
            selectedIndex = (selectedIndex - 1 + SETTING_NAMES.length) % SETTING_NAMES.length;
            AudioManager.getInstance().play(SoundEffect.HACK_CLICK);
            return false;
        }

        if (keyboard.isKeyPressed(KeyEvent.VK_DOWN) || keyboard.isKeyPressed(KeyEvent.VK_S)) {
            selectedIndex = (selectedIndex + 1) % SETTING_NAMES.length;
            AudioManager.getInstance().play(SoundEffect.HACK_CLICK);
            return false;
        }

        boolean left = keyboard.isKeyPressed(KeyEvent.VK_LEFT) || keyboard.isKeyPressed(KeyEvent.VK_A);
        boolean right = keyboard.isKeyPressed(KeyEvent.VK_RIGHT) || keyboard.isKeyPressed(KeyEvent.VK_D);
        boolean enter = keyboard.isKeyPressed(KeyEvent.VK_ENTER) || keyboard.isKeyPressed(KeyEvent.VK_SPACE);

        if (left || right || enter) {
            switch (selectedIndex) {
                case 0: // Master Volume
                    double mv = settings.getMasterVolume() + (right ? 0.05 : -0.05);
                    settings.setMasterVolume(Math.round(mv * 100.0) / 100.0);
                    AudioManager.getInstance().play(SoundEffect.HACK_CLICK);
                    break;
                case 1: // SFX Volume
                    double sv = settings.getSfxVolume() + (right ? 0.05 : -0.05);
                    settings.setSfxVolume(Math.round(sv * 100.0) / 100.0);
                    AudioManager.getInstance().play(SoundEffect.HACK_CLICK);
                    break;
                case 2: // Ambient Volume
                    double av = settings.getAmbientVolume() + (right ? 0.05 : -0.05);
                    settings.setAmbientVolume(Math.round(av * 100.0) / 100.0);
                    AudioManager.getInstance().play(SoundEffect.HACK_CLICK);
                    break;
                case 3: // Screen Shake
                    settings.setScreenShakeEnabled(!settings.isScreenShakeEnabled());
                    AudioManager.getInstance().play(SoundEffect.HACK_SUCCESS);
                    break;
                case 4: // Scanlines
                    settings.setScanlinesEnabled(!settings.isScanlinesEnabled());
                    AudioManager.getInstance().play(SoundEffect.HACK_SUCCESS);
                    break;
                case 5: // Camera Smoothing
                    double cur = settings.getCameraSmoothing();
                    if (cur <= 4.0) settings.setCameraSmoothing(7.0);
                    else if (cur <= 8.0) settings.setCameraSmoothing(12.0);
                    else settings.setCameraSmoothing(4.0);
                    AudioManager.getInstance().play(SoundEffect.HACK_CLICK);
                    break;
                case 6: // Back
                    if (enter) {
                        AudioManager.getInstance().play(SoundEffect.HACK_SUCCESS);
                        return true;
                    }
                    break;
            }
        }

        if (keyboard.isKeyPressed(KeyEvent.VK_ESCAPE)) {
            AudioManager.getInstance().play(SoundEffect.HACK_SUCCESS);
            return true;
        }

        return false;
    }

    public void render(Graphics2D g2d, int screenWidth, int screenHeight) {
        g2d.setColor(new Color(4, 10, 20, 225));
        g2d.fillRect(0, 0, screenWidth, screenHeight);

        int boxW = 520;
        int boxH = 430;
        int boxX = (screenWidth - boxW) / 2;
        int boxY = (screenHeight - boxH) / 2;

        g2d.setColor(new Color(12, 25, 42, 245));
        g2d.fillRect(boxX, boxY, boxW, boxH);

        g2d.setColor(new Color(0, 220, 255, 180));
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.drawRect(boxX, boxY, boxW, boxH);

        // Header Title
        g2d.setFont(TITLE_FONT);
        g2d.setColor(new Color(0, 240, 255));
        String title = "TACTICAL SETTINGS";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(title, boxX + (boxW - fm.stringWidth(title)) / 2, boxY + 42);

        // Divider
        g2d.setColor(new Color(0, 200, 255, 60));
        g2d.drawLine(boxX + 30, boxY + 60, boxX + boxW - 30, boxY + 60);

        SettingsManager settings = SettingsManager.getInstance();
        int startY = boxY + 100;
        int spacing = 44;

        for (int i = 0; i < SETTING_NAMES.length; i++) {
            int itemY = startY + i * spacing;
            boolean selected = (i == selectedIndex);

            if (selected) {
                double pulse = (Math.sin(animTimer * 6.0) + 1.0) / 2.0;
                int alpha = (int) (35 + pulse * 40);
                g2d.setColor(new Color(0, 230, 255, alpha));
                g2d.fillRect(boxX + 25, itemY - 24, boxW - 50, 32);

                g2d.setColor(new Color(0, 240, 255));
                g2d.drawRect(boxX + 25, itemY - 24, boxW - 50, 32);

                g2d.setColor(new Color(0, 255, 180));
                g2d.setFont(LABEL_FONT);
                g2d.drawString("► " + SETTING_NAMES[i], boxX + 40, itemY);
            } else {
                g2d.setColor(new Color(160, 190, 210));
                g2d.setFont(LABEL_FONT);
                g2d.drawString("  " + SETTING_NAMES[i], boxX + 40, itemY);
            }

            // Value representation
            g2d.setFont(VALUE_FONT);
            String valStr = "";
            switch (i) {
                case 0:
                    valStr = renderSlider(settings.getMasterVolume());
                    break;
                case 1:
                    valStr = renderSlider(settings.getSfxVolume());
                    break;
                case 2:
                    valStr = renderSlider(settings.getAmbientVolume());
                    break;
                case 3:
                    valStr = settings.isScreenShakeEnabled() ? "[ ON ]" : "[ OFF ]";
                    break;
                case 4:
                    valStr = settings.isScanlinesEnabled() ? "[ ON ]" : "[ OFF ]";
                    break;
                case 5:
                    double sm = settings.getCameraSmoothing();
                    valStr = (sm <= 5.0) ? "[ HIGH DAMP ]" : (sm <= 8.0) ? "[ BALANCED ]" : "[ SHARP ]";
                    break;
                case 6:
                    valStr = "[ RETURN ]";
                    break;
            }

            g2d.setColor(selected ? new Color(0, 255, 230) : new Color(130, 170, 190));
            FontMetrics valFm = g2d.getFontMetrics();
            g2d.drawString(valStr, boxX + boxW - valFm.stringWidth(valStr) - 40, itemY);
        }

        // Hint Footer
        g2d.setFont(HINT_FONT);
        g2d.setColor(new Color(100, 140, 160));
        String hint = "[LEFT/RIGHT] Adjust Value   [W/S] Navigate   [ESC] Return";
        FontMetrics hintFm = g2d.getFontMetrics();
        g2d.drawString(hint, boxX + (boxW - hintFm.stringWidth(hint)) / 2, boxY + boxH - 18);
    }

    private String renderSlider(double val) {
        int filled = (int) Math.round(val * 10.0);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 10; i++) {
            sb.append(i < filled ? "■" : "░");
        }
        sb.append(String.format("] %3d%%", (int) Math.round(val * 100)));
        return sb.toString();
    }
}
