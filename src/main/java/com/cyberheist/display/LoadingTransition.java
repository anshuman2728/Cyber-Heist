package com.cyberheist.display;

import com.cyberheist.audio.AudioManager;
import com.cyberheist.audio.SoundEffect;
import com.cyberheist.input.KeyboardInput;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Atmospheric Terminal Mission Deployment / Loading Screen.
 * Renders stylized mission telemetry, decryption progress bar, and smooth alpha fade transitions.
 */
public final class LoadingTransition {

    private double timer = 0.0;
    private static final double TOTAL_DURATION = 2.4; // Seconds
    private boolean completed = false;
    private final List<String> logMessages = new ArrayList<>();

    private static final Font TITLE_FONT = new Font("Monospaced", Font.BOLD, 28);
    private static final Font SUBTITLE_FONT = new Font("Monospaced", Font.BOLD, 14);
    private static final Font LOG_FONT = new Font("Monospaced", Font.PLAIN, 13);
    private static final Font HINT_FONT = new Font("Monospaced", Font.BOLD, 12);

    public LoadingTransition() {
        logMessages.add("[OK] QUANTUM CIPHER TUNNEL SYNCHRONIZED");
        logMessages.add("[OK] NOIDA METROPOLITAN GRID BYPASS INJECTED");
        logMessages.add("[OK] CCTV TELEMETRY SPOOFING ROUTE ARMED");
        logMessages.add("[OK] INFILTRATOR BIOMETRICS & AGILITY NOMINAL");
    }

    public void update(double deltaTime, KeyboardInput keyboard) {
        if (completed) return;

        timer += deltaTime;

        // Skip / Fast-forward loading
        if (keyboard != null && (keyboard.isKeyPressed(KeyEvent.VK_SPACE) || keyboard.isKeyPressed(KeyEvent.VK_ENTER))) {
            if (timer < TOTAL_DURATION - 0.3) {
                timer = TOTAL_DURATION - 0.3;
                AudioManager.getInstance().play(SoundEffect.HACK_SUCCESS);
            }
        }

        if (timer >= TOTAL_DURATION) {
            completed = true;
        }
    }

    public void render(Graphics2D g2d, int screenWidth, int screenHeight) {
        if (completed) return;

        // Calculate fade opacity
        float alpha = 1.0f;
        if (timer > TOTAL_DURATION - 0.4) {
            alpha = (float) Math.max(0.0, (TOTAL_DURATION - timer) / 0.4);
        }

        Composite originalComposite = g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        // Dark cyberpunk backdrop
        g2d.setColor(new Color(2, 6, 14));
        g2d.fillRect(0, 0, screenWidth, screenHeight);

        // Grid lines background
        g2d.setColor(new Color(0, 200, 255, 12));
        for (int x = 0; x < screenWidth; x += 40) {
            g2d.drawLine(x, 0, x, screenHeight);
        }
        for (int y = 0; y < screenHeight; y += 40) {
            g2d.drawLine(0, y, screenWidth, y);
        }

        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        // Title
        g2d.setFont(TITLE_FONT);
        g2d.setColor(new Color(0, 240, 255));
        String title = "NOIDA CORPORATE DATA CENTER";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(title, centerX - fm.stringWidth(title) / 2, centerY - 100);

        // Subtitle
        g2d.setFont(SUBTITLE_FONT);
        g2d.setColor(new Color(0, 255, 170));
        String subtitle = "// MISSION 01: GHOST IN THE SERVER // TOP SECRET";
        fm = g2d.getFontMetrics();
        g2d.drawString(subtitle, centerX - fm.stringWidth(subtitle) / 2, centerY - 70);

        // Terminal Log Lines
        g2d.setFont(LOG_FONT);
        int logY = centerY - 25;
        double progress = Math.min(1.0, timer / (TOTAL_DURATION - 0.4));
        int linesToShow = (int) Math.min(logMessages.size(), Math.floor(progress * (logMessages.size() + 1)));

        for (int i = 0; i < linesToShow; i++) {
            g2d.setColor(new Color(130, 200, 230));
            String msg = logMessages.get(i);
            fm = g2d.getFontMetrics();
            g2d.drawString(msg, centerX - fm.stringWidth(msg) / 2, logY + i * 24);
        }

        // Loading Progress Bar
        int barW = 440;
        int barH = 14;
        int barX = centerX - barW / 2;
        int barY = centerY + 105;

        g2d.setColor(new Color(10, 24, 40));
        g2d.fillRect(barX, barY, barW, barH);
        g2d.setColor(new Color(0, 200, 255, 120));
        g2d.drawRect(barX, barY, barW, barH);

        int filledW = (int) Math.round(barW * progress);
        g2d.setColor(new Color(0, 240, 255));
        g2d.fillRect(barX + 2, barY + 2, Math.max(0, filledW - 4), barH - 4);

        // Percentage Text
        g2d.setFont(HINT_FONT);
        g2d.setColor(new Color(0, 255, 180));
        String pct = String.format("DEPLOYING: %d%%", (int) Math.round(progress * 100));
        fm = g2d.getFontMetrics();
        g2d.drawString(pct, centerX - fm.stringWidth(pct) / 2, barY + 34);

        // Skip prompt
        g2d.setColor(new Color(100, 140, 160));
        String skip = "[SPACE / ENTER TO INFILTRATE]";
        fm = g2d.getFontMetrics();
        g2d.drawString(skip, centerX - fm.stringWidth(skip) / 2, barY + 60);

        g2d.setComposite(originalComposite);
    }

    public boolean isCompleted() {
        return completed;
    }

    public void reset() {
        this.timer = 0.0;
        this.completed = false;
    }
}
