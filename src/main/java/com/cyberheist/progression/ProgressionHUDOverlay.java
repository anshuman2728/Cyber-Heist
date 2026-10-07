package com.cyberheist.progression;

import com.cyberheist.core.GameConfig;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/**
 * Screen-space UI rendering the Progression Matrix, displaying skill tracks,
 * ranks, perks, and interactive upgrade buttons.
 */
public final class ProgressionHUDOverlay {

    private static final Font FONT_TITLE = new Font("Consolas", Font.BOLD, 18);
    private static final Font FONT_HEADER = new Font("Consolas", Font.BOLD, 14);
    private static final Font FONT_BODY = new Font("Consolas", Font.PLAIN, 12);
    private static final Font FONT_SMALL = new Font("Consolas", Font.PLAIN, 10);

    public void render(Graphics2D g2d, ProgressionManager progression, int screenWidth, int screenHeight, int mouseX, int mouseY) {
        // Darkened backdrop
        g2d.setColor(new Color(5, 8, 14, 220));
        g2d.fillRect(0, 0, screenWidth, screenHeight);

        int modalW = 860;
        int modalH = 520;
        int modalX = (screenWidth - modalW) / 2;
        int modalY = (screenHeight - modalH) / 2;

        // Modal Frame
        g2d.setColor(new Color(12, 18, 28, 245));
        g2d.fillRoundRect(modalX, modalY, modalW, modalH, 12, 12);
        g2d.setColor(GameConfig.COLOR_CYAN);
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.drawRoundRect(modalX, modalY, modalW, modalH, 12, 12);
        g2d.setStroke(new BasicStroke(1.0f));

        // Header
        g2d.setFont(FONT_TITLE);
        g2d.setColor(GameConfig.COLOR_CYAN);
        g2d.drawString("OPERATIVE PROGRESSION MATRIX", modalX + 32, modalY + 42);

        g2d.setFont(FONT_HEADER);
        g2d.setColor(GameConfig.COLOR_GREEN);
        g2d.drawString(String.format("AVAILABLE SKILL POINTS: %d", progression.getSkillPoints()), modalX + 540, modalY + 42);

        g2d.setFont(FONT_BODY);
        g2d.setColor(GameConfig.COLOR_TEXT_MUTED);
        g2d.drawString(String.format("Total Operative Experience: %d XP (Next point in %d XP)",
                progression.getTotalXp(), 100 - progression.getCurrentXp()), modalX + 32, modalY + 66);

        // Divider
        g2d.setColor(new Color(40, 60, 85));
        g2d.drawLine(modalX + 32, modalY + 80, modalX + modalW - 32, modalY + 80);

        // Render 4 skill track cards in a 2x2 grid
        ProgressionTrack[] tracks = ProgressionTrack.values();
        int cardW = 380;
        int cardH = 175;

        for (int i = 0; i < tracks.length; i++) {
            ProgressionTrack track = tracks[i];
            int col = i % 2;
            int row = i / 2;
            int cardX = modalX + 36 + col * (cardW + 28);
            int cardY = modalY + 98 + row * (cardH + 20);

            renderTrackCard(g2d, track, progression, cardX, cardY, cardW, cardH, i + 1, mouseX, mouseY);
        }

        // Footer Banner
        g2d.setFont(FONT_BODY);
        g2d.setColor(GameConfig.COLOR_TEXT_BRIGHT);
        g2d.drawString("[Keys 1-4 or Click Button to Upgrade | ESC / P to Close Matrix]", modalX + 36, modalY + modalH - 24);
    }

    private void renderTrackCard(Graphics2D g2d, ProgressionTrack track, ProgressionManager progression,
                                 int x, int y, int w, int h, int numKey, int mouseX, int mouseY) {
        int rank = progression.getRank(track);
        boolean canUpgrade = progression.canUpgrade(track);
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;

        // Card background
        g2d.setColor(hovered ? new Color(20, 30, 48, 230) : new Color(14, 22, 34, 220));
        g2d.fillRoundRect(x, y, w, h, 8, 8);
        g2d.setColor(canUpgrade ? GameConfig.COLOR_CYAN : new Color(45, 65, 90));
        g2d.drawRoundRect(x, y, w, h, 8, 8);

        // Card Title
        g2d.setFont(FONT_HEADER);
        g2d.setColor(GameConfig.COLOR_TEXT_BRIGHT);
        g2d.drawString(String.format("[%d] %s", numKey, track.getDisplayName().toUpperCase()), x + 16, y + 26);

        // Rank pips
        int pipX = x + w - 100;
        int pipY = y + 14;
        for (int p = 1; p <= ProgressionManager.MAX_RANK; p++) {
            if (p <= rank) {
                g2d.setColor(GameConfig.COLOR_GREEN);
                g2d.fillRect(pipX + (p * 24), pipY, 18, 12);
            } else {
                g2d.setColor(new Color(30, 45, 60));
                g2d.fillRect(pipX + (p * 24), pipY, 18, 12);
                g2d.setColor(new Color(60, 85, 110));
                g2d.drawRect(pipX + (p * 24), pipY, 18, 12);
            }
        }

        // Description
        g2d.setFont(FONT_SMALL);
        g2d.setColor(GameConfig.COLOR_TEXT_MUTED);
        g2d.drawString(track.getDescription(), x + 16, y + 48);

        // Current Rank Perk
        g2d.setFont(FONT_BODY);
        g2d.setColor(new Color(130, 190, 240));
        g2d.drawString("CURRENT: " + track.getPerkName(rank), x + 16, y + 78);

        // Next Rank Perk
        if (rank < ProgressionManager.MAX_RANK) {
            g2d.setColor(GameConfig.COLOR_GREEN);
            g2d.drawString("NEXT RANK: " + track.getPerkName(rank + 1), x + 16, y + 104);
        } else {
            g2d.setColor(new Color(255, 200, 0));
            g2d.drawString("MAX LEVEL ATTAINED", x + 16, y + 104);
        }

        // Upgrade Button
        int btnW = 140;
        int btnH = 28;
        int btnX = x + 16;
        int btnY = y + 130;

        if (rank >= ProgressionManager.MAX_RANK) {
            g2d.setColor(new Color(30, 40, 55));
            g2d.fillRoundRect(btnX, btnY, btnW, btnH, 6, 6);
            g2d.setColor(new Color(100, 110, 125));
            g2d.drawString("MAXED OUT", btnX + 32, btnY + 18);
        } else if (canUpgrade) {
            boolean btnHovered = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
            g2d.setColor(btnHovered ? GameConfig.COLOR_GREEN : new Color(0, 180, 120));
            g2d.fillRoundRect(btnX, btnY, btnW, btnH, 6, 6);
            g2d.setColor(Color.BLACK);
            g2d.drawString(String.format("UPGRADE [%d]", numKey), btnX + 28, btnY + 18);
        } else {
            g2d.setColor(new Color(30, 40, 55));
            g2d.fillRoundRect(btnX, btnY, btnW, btnH, 6, 6);
            g2d.setColor(new Color(100, 110, 125));
            g2d.drawString("1 POINT NEEDED", btnX + 18, btnY + 18);
        }
    }

    public ProgressionTrack getTrackAt(int mouseX, int mouseY, int screenWidth, int screenHeight) {
        int modalW = 860;
        int modalH = 520;
        int modalX = (screenWidth - modalW) / 2;
        int modalY = (screenHeight - modalH) / 2;

        ProgressionTrack[] tracks = ProgressionTrack.values();
        int cardW = 380;
        int cardH = 175;

        for (int i = 0; i < tracks.length; i++) {
            int col = i % 2;
            int row = i / 2;
            int cardX = modalX + 36 + col * (cardW + 28);
            int cardY = modalY + 98 + row * (cardH + 20);

            int btnW = 140;
            int btnH = 28;
            int btnX = cardX + 16;
            int btnY = cardY + 130;

            if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                return tracks[i];
            }
        }
        return null;
    }
}
