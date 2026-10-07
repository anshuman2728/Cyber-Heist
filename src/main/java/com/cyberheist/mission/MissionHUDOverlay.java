package com.cyberheist.mission;

import com.cyberheist.core.GameConfig;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.List;

/**
 * Renders the Mission 1 objective checklist, radio communications popups,
 * and mission complete debrief overlay.
 */
public final class MissionHUDOverlay {

    private static final Font FONT_HEADER = new Font(Font.MONOSPACED, Font.BOLD, 12);
    private static final Font FONT_OBJECTIVE = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    private static final Font FONT_OBJECTIVE_BOLD = new Font(Font.MONOSPACED, Font.BOLD, 12);
    private static final Font FONT_COMMS_NAME = new Font(Font.MONOSPACED, Font.BOLD, 12);
    private static final Font FONT_COMMS_TEXT = new Font(Font.MONOSPACED, Font.PLAIN, 14);
    private static final Font FONT_VICTORY_TITLE = new Font(Font.MONOSPACED, Font.BOLD, 22);
    private static final Font FONT_VICTORY_SUB = new Font(Font.MONOSPACED, Font.BOLD, 14);

    public void render(Graphics2D g2d, MissionManager mission, RadioDialogueManager dialogue,
                       int screenWidth, int screenHeight) {
        if (mission == null) return;

        // 1. Render Objective Checklist Tracker (Top-Left under Area banner)
        renderObjectiveTracker(g2d, mission);

        // 2. Render Active Radio Earpiece Dialogue (Top-Center)
        if (dialogue != null && dialogue.hasActiveDialogue()) {
            renderRadioComms(g2d, dialogue.getCurrentLine(), dialogue.getAudioWaveformTimer(), screenWidth);
        }

        // 3. Render Mission Accomplished Debrief (Screen Center)
        if (mission.getStatus() == MissionManager.MissionStatus.SUCCESS) {
            renderVictoryDebrief(g2d, mission, screenWidth, screenHeight);
        }
    }

    private void renderObjectiveTracker(Graphics2D g2d, MissionManager mission) {
        int panelX = 24;
        int panelY = 62;
        int panelW = 340;
        int panelH = 158;

        // Panel background
        g2d.setColor(new Color(10, 14, 23, 215));
        g2d.fillRoundRect(panelX, panelY, panelW, panelH, 6, 6);
        g2d.setColor(new Color(0x28, 0x3A, 0x50));
        g2d.drawRoundRect(panelX, panelY, panelW, panelH, 6, 6);

        // Header Title
        g2d.setFont(FONT_HEADER);
        g2d.setColor(GameConfig.COLOR_CYAN);
        g2d.drawString("MISSION 01 // GHOST IN THE SERVER", panelX + 12, panelY + 18);

        // Progress counter badge
        String progress = String.format("[%d/%d]", mission.getCompletedCount(), mission.getTotalCount());
        g2d.setColor(GameConfig.COLOR_TEXT_BRIGHT);
        g2d.drawString(progress, panelX + panelW - 54, panelY + 18);

        // Divider
        g2d.setColor(new Color(0x28, 0x3A, 0x50));
        g2d.drawLine(panelX + 12, panelY + 24, panelX + panelW - 12, panelY + 24);

        // Objectives List
        int lineY = panelY + 40;
        List<MissionObjective> list = mission.getObjectives();

        for (MissionObjective obj : list) {
            String check;
            Color textColor;
            Font font = FONT_OBJECTIVE;

            switch (obj.getStatus()) {
                case COMPLETED -> {
                    check = "[✓]";
                    textColor = GameConfig.COLOR_GREEN;
                }
                case ACTIVE -> {
                    check = "[▶]";
                    textColor = GameConfig.COLOR_CYAN;
                    font = FONT_OBJECTIVE_BOLD;
                }
                default -> {
                    check = "[ ]";
                    textColor = GameConfig.COLOR_TEXT_MUTED;
                }
            }

            g2d.setFont(font);
            g2d.setColor(textColor);
            g2d.drawString(check + " " + obj.getTitle(), panelX + 12, lineY);
            lineY += 16;
        }
    }

    private void renderRadioComms(Graphics2D g2d, DialogueLine line, double waveformTimer, int screenWidth) {
        if (line == null) return;

        int boxW = 520;
        int boxH = 68;
        int boxX = (screenWidth - boxW) / 2;
        int boxY = 80;

        // Comms Card Background
        g2d.setColor(new Color(8, 12, 18, 240));
        g2d.fillRoundRect(boxX, boxY, boxW, boxH, 8, 8);
        g2d.setColor(line.getColor());
        g2d.setStroke(new BasicStroke(1.4f));
        g2d.drawRoundRect(boxX, boxY, boxW, boxH, 8, 8);
        g2d.setStroke(new BasicStroke(1.0f));

        // Pulsing audio waveform visualizer bars
        int waveX = boxX + boxW - 120;
        int waveY = boxY + 20;
        for (int i = 0; i < 9; i++) {
            double h = Math.abs(Math.sin(waveformTimer + (i * 0.7))) * 14.0 + 3.0;
            g2d.setColor(line.getColor());
            g2d.fillRect(waveX + (i * 10), (int) (waveY - (h / 2.0)), 4, (int) h);
        }

        // Speaker Header
        g2d.setFont(FONT_COMMS_NAME);
        g2d.setColor(line.getColor());
        String speakerTag = String.format("[COMMS] %s // %s", line.getSpeaker(), line.getRole());
        g2d.drawString(speakerTag, boxX + 16, boxY + 22);

        // Spoken Dialogue Text
        g2d.setFont(FONT_COMMS_TEXT);
        g2d.setColor(GameConfig.COLOR_TEXT_BRIGHT);
        g2d.drawString("\"" + line.getText() + "\"", boxX + 16, boxY + 48);
    }

    private void renderVictoryDebrief(Graphics2D g2d, MissionManager mission, int screenWidth, int screenHeight) {
        int debriefW = 540;
        int debriefH = 280;
        int debriefX = (screenWidth - debriefW) / 2;
        int debriefY = (screenHeight - debriefH) / 2;

        // Dimmer backdrop
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, screenWidth, screenHeight);

        // Debrief frame
        g2d.setColor(new Color(10, 16, 24, 250));
        g2d.fillRoundRect(debriefX, debriefY, debriefW, debriefH, 12, 12);
        g2d.setColor(GameConfig.COLOR_GREEN);
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.drawRoundRect(debriefX, debriefY, debriefW, debriefH, 12, 12);
        g2d.setStroke(new BasicStroke(1.0f));

        // Header Title
        g2d.setFont(FONT_VICTORY_TITLE);
        g2d.setColor(GameConfig.COLOR_GREEN);
        g2d.drawString("MISSION ACCOMPLISHED", debriefX + 32, debriefY + 44);

        g2d.setFont(FONT_VICTORY_SUB);
        g2d.setColor(GameConfig.COLOR_TEXT_MUTED);
        g2d.drawString("OPERATION: GHOST IN THE SERVER", debriefX + 32, debriefY + 70);

        // Divider
        g2d.setColor(new Color(0x28, 0x3A, 0x50));
        g2d.drawLine(debriefX + 32, debriefY + 84, debriefX + debriefW - 32, debriefY + 84);

        // Metrics Table
        int rowY = debriefY + 115;
        g2d.setFont(FONT_OBJECTIVE_BOLD);

        drawDebriefRow(g2d, "ALL OBJECTIVES:", "7 / 7 COMPLETED", GameConfig.COLOR_GREEN, debriefX + 32, rowY, debriefW - 64);
        rowY += 26;

        drawDebriefRow(g2d, "DATA EXFILTRATED:", "$" + mission.getCreditsExfiltrated() + " CREDITS", GameConfig.COLOR_CYAN, debriefX + 32, rowY, debriefW - 64);
        rowY += 26;

        drawDebriefRow(g2d, "OPERATIONAL TIME:", String.format("%.1f SECONDS", mission.getMissionTime()), GameConfig.COLOR_TEXT_BRIGHT, debriefX + 32, rowY, debriefW - 64);
        rowY += 26;

        drawDebriefRow(g2d, "TACTICAL RATING:", mission.getStealthRating(), GameConfig.COLOR_GREEN, debriefX + 32, rowY, debriefW - 64);
        rowY += 34;

        // Footer prompt
        g2d.setFont(FONT_HEADER);
        g2d.setColor(GameConfig.COLOR_TEXT_MUTED);
        g2d.drawString("[VAN DOORS SEALED // ESCAPE COMPLETE]", debriefX + 32, rowY);
    }

    private void drawDebriefRow(Graphics2D g2d, String label, String value, Color valueColor, int x, int y, int width) {
        g2d.setColor(GameConfig.COLOR_TEXT_MUTED);
        g2d.drawString(label, x, y);
        g2d.setColor(valueColor);
        g2d.drawString(value, x + width - 180, y);
    }
}
