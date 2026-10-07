package com.cyberheist.hacking;

import com.cyberheist.core.GameConfig;

import java.awt.*;
import java.util.List;

/**
 * High-fidelity Cyberpunk CRT Terminal UI Renderer for the Network Node hacking minigame.
 * Displays node buttons, real-time timer countdown, and ASCII/graphical security trace bars.
 */
public final class HackingOverlayRenderer {

    private static final Font FONT_TITLE = new Font(Font.MONOSPACED, Font.BOLD, 16);
    private static final Font FONT_NODE = new Font(Font.MONOSPACED, Font.BOLD, 18);
    private static final Font FONT_HUD = new Font(Font.MONOSPACED, Font.BOLD, 13);
    private static final Font FONT_SMALL = new Font(Font.MONOSPACED, Font.PLAIN, 11);

    public void render(Graphics2D g2d, HackingSession session, int screenWidth, int screenHeight, int mouseX, int mouseY) {
        if (session == null) return;

        // 1. Semi-transparent backdrop blur/dimmer
        g2d.setColor(new Color(5, 8, 14, 215));
        g2d.fillRect(0, 0, screenWidth, screenHeight);

        // 2. Center Terminal Frame
        int frameW = 660;
        int frameH = 430;
        int frameX = (screenWidth - frameW) / 2;
        int frameY = (screenHeight - frameH) / 2;

        // Frame body
        g2d.setColor(new Color(12, 18, 28, 245));
        g2d.fillRoundRect(frameX, frameY, frameW, frameH, 12, 12);

        // Glowing border based on session state
        Color borderColor = switch (session.getStatus()) {
            case SUCCESS -> new Color(0, 255, 128);
            case FAILED_TIMEOUT, FAILED_TRACED -> new Color(255, 50, 50);
            default -> GameConfig.COLOR_CYAN;
        };

        g2d.setColor(borderColor);
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.drawRoundRect(frameX, frameY, frameW, frameH, 12, 12);
        g2d.setStroke(new BasicStroke(1.0f));

        // Scanlines overlay within terminal
        g2d.setColor(new Color(0, 240, 255, 12));
        for (int y = frameY + 6; y < frameY + frameH - 6; y += 4) {
            g2d.drawLine(frameX + 6, y, frameX + frameW - 6, y);
        }

        // 3. Header Banner
        g2d.setColor(new Color(20, 32, 48));
        g2d.fillRoundRect(frameX + 16, frameY + 16, frameW - 32, 52, 8, 8);
        g2d.setColor(borderColor);
        g2d.drawRoundRect(frameX + 16, frameY + 16, frameW - 32, 52, 8, 8);

        g2d.setFont(FONT_TITLE);
        g2d.setColor(GameConfig.COLOR_TEXT_BRIGHT);
        g2d.drawString("NETWORK NODE", frameX + 30, frameY + 38);

        g2d.setFont(FONT_SMALL);
        g2d.setColor(GameConfig.COLOR_CYAN);
        String subhead = String.format("TARGET: %s  |  %s",
                session.getTarget() != null ? session.getTarget().getTargetName() : "UNKNOWN HOST",
                session.getDifficulty().getDescription());
        g2d.drawString(subhead, frameX + 30, frameY + 56);

        // 4. Target Buffer Sequence Display
        int seqY = frameY + 95;
        g2d.setFont(FONT_HUD);
        g2d.setColor(GameConfig.COLOR_TEXT_MUTED);
        g2d.drawString("Find the correct sequence.", frameX + 30, seqY);

        List<String> targetSeq = session.getTargetSequence();
        int nodePillW = 54;
        int nodePillH = 28;
        int startSeqX = frameX + 200;

        for (int i = 0; i < targetSeq.size(); i++) {
            int px = startSeqX + (i * (nodePillW + 12));
            boolean matched = i < session.getCurrentSequenceIndex();
            boolean isCurrent = i == session.getCurrentSequenceIndex() && session.getStatus() == HackingSession.SessionStatus.IN_PROGRESS;

            // Pill background
            if (matched) {
                g2d.setColor(new Color(0x00, 0x55, 0x2A));
            } else if (isCurrent) {
                g2d.setColor(new Color(0x18, 0x3E, 0x56));
            } else {
                g2d.setColor(new Color(0x10, 0x1A, 0x24));
            }
            g2d.fillRoundRect(px, seqY - 18, nodePillW, nodePillH, 6, 6);

            // Pill border
            g2d.setColor(matched ? GameConfig.COLOR_GREEN : (isCurrent ? GameConfig.COLOR_CYAN : Color.DARK_GRAY));
            g2d.drawRoundRect(px, seqY - 18, nodePillW, nodePillH, 6, 6);

            // Text
            g2d.setFont(FONT_HUD);
            g2d.setColor(matched ? Color.WHITE : (isCurrent ? GameConfig.COLOR_CYAN : GameConfig.COLOR_TEXT_MUTED));
            g2d.drawString("[ " + targetSeq.get(i) + " ]", px + 4, seqY + 1);
        }

        // 5. Available Network Node Buttons (Grid / Row)
        renderNodeButtons(g2d, session, frameX, frameY, frameW, mouseX, mouseY);

        // 6. Security Countermeasures & Timer Bar (User prompt requirement)
        int telemetryY = frameY + 330;
        g2d.setColor(new Color(16, 24, 36));
        g2d.fillRoundRect(frameX + 24, telemetryY, frameW - 48, 56, 8, 8);
        g2d.setColor(new Color(0x28, 0x3A, 0x50));
        g2d.drawRoundRect(frameX + 24, telemetryY, frameW - 48, 56, 8, 8);

        // Timer Display
        double timeLeft = session.getTimeRemaining();
        Color timerColor = (timeLeft > 4.0) ? GameConfig.COLOR_GREEN : (timeLeft > 2.0 ? new Color(255, 185, 0) : Color.RED);
        g2d.setFont(FONT_TITLE);
        g2d.setColor(timerColor);
        String timerText = String.format("Time: %04.1fs", timeLeft);
        g2d.drawString(timerText, frameX + 44, telemetryY + 34);

        // Security Trace Blocks (User prompt requirement: "Security: ███████░░")
        double trace = session.getSecurityTrace();
        Color traceColor = (trace < 50.0) ? GameConfig.COLOR_GREEN : (trace < 80.0 ? new Color(255, 185, 0) : Color.RED);
        g2d.setColor(traceColor);
        String traceText = "Security: " + session.getSecurityTraceBlocks() + String.format(" [%3.0f%%]", trace);
        g2d.drawString(traceText, frameX + 260, telemetryY + 34);

        // 7. Status & Controls Footer
        g2d.setFont(FONT_SMALL);
        g2d.setColor(borderColor);
        g2d.drawString(session.getStatusMessage(), frameX + 30, frameY + 410);

        g2d.setColor(GameConfig.COLOR_TEXT_MUTED);
        g2d.drawString("[Keys 1-8 or Click to Select | ESC: Abort]", frameX + frameW - 270, frameY + 410);
    }

    private void renderNodeButtons(Graphics2D g2d, HackingSession session, int frameX, int frameY, int frameW, int mouseX, int mouseY) {
        List<NetworkNode> nodes = session.getNodePool();
        if (nodes.isEmpty()) return;

        int totalNodes = nodes.size();
        int btnW = 90;
        int btnH = 68;
        int gap = 16;
        int totalRowW = (totalNodes * btnW) + ((totalNodes - 1) * gap);
        int startX = frameX + (frameW - totalRowW) / 2;
        int startY = frameY + 170;

        for (int i = 0; i < totalNodes; i++) {
            NetworkNode node = nodes.get(i);
            int bx = startX + (i * (btnW + gap));
            int by = startY;
            node.setBounds(bx, by, btnW, btnH);

            boolean isHovered = node.containsPoint(mouseX, mouseY) && session.getStatus() == HackingSession.SessionStatus.IN_PROGRESS;

            // Fill color
            Color btnFill = switch (node.getState()) {
                case MATCHED -> new Color(0x0A, 0x4E, 0x24);
                case ERROR_FLASH -> new Color(0x7A, 0x14, 0x14);
                default -> isHovered ? new Color(0x1B, 0x30, 0x48) : new Color(0x10, 0x1C, 0x2A);
            };

            // Border color
            Color btnBorder = switch (node.getState()) {
                case MATCHED -> GameConfig.COLOR_GREEN;
                case ERROR_FLASH -> Color.RED;
                default -> isHovered ? Color.WHITE : GameConfig.COLOR_CYAN;
            };

            g2d.setColor(btnFill);
            g2d.fillRoundRect(bx, by, btnW, btnH, 8, 8);
            g2d.setColor(btnBorder);
            g2d.setStroke(new BasicStroke(isHovered ? 2.0f : 1.2f));
            g2d.drawRoundRect(bx, by, btnW, btnH, 8, 8);
            g2d.setStroke(new BasicStroke(1.0f));

            // Hotkey indicator in top-left of button
            g2d.setFont(FONT_SMALL);
            g2d.setColor(GameConfig.COLOR_TEXT_MUTED);
            g2d.drawString("[" + node.getIndex() + "]", bx + 8, by + 18);

            // Node Code (e.g. "[ 04 ]", "[ 17 ]", "[ 82 ]", "[ 39 ]")
            g2d.setFont(FONT_NODE);
            g2d.setColor(node.getState() == NetworkNode.NodeState.MATCHED ? Color.WHITE : GameConfig.COLOR_TEXT_BRIGHT);
            g2d.drawString("[ " + node.getCode() + " ]", bx + 12, by + 46);
        }
    }
}
