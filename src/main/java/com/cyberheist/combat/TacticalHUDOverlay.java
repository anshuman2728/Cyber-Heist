package com.cyberheist.combat;

import com.cyberheist.core.GameConfig;
import com.cyberheist.inventory.Inventory;
import com.cyberheist.inventory.ItemType;
import com.cyberheist.progression.ProgressionManager;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/**
 * Screen-space UI rendering tactical combat weapons, inventory slots,
 * cooldown timers, and combat toast notifications.
 */
public final class TacticalHUDOverlay {

    private static final Font FONT_CODE = new Font("Consolas", Font.BOLD, 11);
    private static final Font FONT_TOAST = new Font("Consolas", Font.BOLD, 13);
    private static final Font FONT_BADGE = new Font("Consolas", Font.BOLD, 10);

    public void render(Graphics2D g2d, CombatManager combat, Inventory inventory,
                       ProgressionManager progression, int screenWidth, int screenHeight) {

        // 1. Toast Notification (Upper Center)
        String notification = combat.getCombatNotification();
        if (notification != null) {
            renderToast(g2d, notification, screenWidth / 2, 148);
        }

        // 2. Tactical Quickbar (Lower Left, above bottom control bar)
        int barX = 24;
        int barY = screenHeight - 96;
        renderQuickbar(g2d, combat, inventory, progression, barX, barY);
    }

    private void renderToast(Graphics2D g2d, String text, int cx, int cy) {
        g2d.setFont(FONT_TOAST);
        int textW = g2d.getFontMetrics().stringWidth(text);
        int pad = 24;

        g2d.setColor(new Color(10, 16, 26, 235));
        g2d.fillRoundRect(cx - (textW / 2) - pad, cy - 16, textW + (pad * 2), 32, 6, 6);
        g2d.setColor(GameConfig.COLOR_CYAN);
        g2d.drawRoundRect(cx - (textW / 2) - pad, cy - 16, textW + (pad * 2), 32, 6, 6);

        g2d.drawString(text, cx - (textW / 2), cy + 5);
    }

    private void renderQuickbar(Graphics2D g2d, CombatManager combat, Inventory inventory,
                                ProgressionManager progression, int x, int y) {

        int slotW = 82;
        int slotH = 46;
        int gap = 8;

        // --- WEAPON SLOTS ---
        // Slot 1: [Q] STUN DEVICE
        double stunCd = combat.getStunCooldownTimer();
        boolean stunReady = stunCd <= 0.0;
        renderSlot(g2d, x, y, slotW, slotH, "[Q] STUN", stunReady ? "READY" : String.format("%.1fs", stunCd),
                stunReady ? GameConfig.COLOR_CYAN : Color.GRAY, stunReady);

        // Slot 2: [G] DISTRACTION DECOY
        double decoyCd = combat.getDistractionCooldownTimer();
        boolean decoyReady = decoyCd <= 0.0;
        renderSlot(g2d, x + (slotW + gap), y, slotW, slotH, "[G] DECOY", decoyReady ? "READY" : String.format("%.1fs", decoyCd),
                decoyReady ? new Color(255, 190, 0) : Color.GRAY, decoyReady);

        // Slot 3: [F] TAKEDOWN
        boolean takedownAvail = combat.getActiveTakedownTarget() != null;
        renderSlot(g2d, x + (slotW + gap) * 2, y, slotW, slotH, "[F] TAKEDOWN",
                takedownAvail ? "PROMPT" : "STANDBY",
                takedownAvail ? new Color(255, 0, 128) : new Color(60, 75, 95), takedownAvail);

        // Divider
        int divX = x + (slotW + gap) * 3 + 4;
        g2d.setColor(new Color(40, 60, 85));
        g2d.drawLine(divX, y + 4, divX, y + slotH - 4);

        // --- INVENTORY CONSUMABLES ---
        int invStartX = divX + 12;

        // Item 1: EMP × 2
        int empCount = inventory.getCount(ItemType.EMP);
        renderSlot(g2d, invStartX, y, slotW, slotH, "[1] EMP", "x" + empCount,
                empCount > 0 ? GameConfig.COLOR_CYAN : Color.DARK_GRAY, empCount > 0);

        // Item 2: Lockpick × 3
        int lockCount = inventory.getCount(ItemType.LOCKPICK);
        renderSlot(g2d, invStartX + (slotW + gap), y, slotW, slotH, "[2] PICK", "x" + lockCount,
                lockCount > 0 ? new Color(0, 230, 140) : Color.DARK_GRAY, lockCount > 0);

        // Item 3: Medkit × 1
        int medCount = inventory.getCount(ItemType.MEDKIT);
        renderSlot(g2d, invStartX + (slotW + gap) * 2, y, slotW, slotH, "[3] MEDKIT", "x" + medCount,
                medCount > 0 ? new Color(255, 80, 80) : Color.DARK_GRAY, medCount > 0);

        // Item 4: Data Drive × 1
        boolean payload = inventory.isDataPayloadSecured();
        renderSlot(g2d, invStartX + (slotW + gap) * 3, y, slotW, slotH, "[4] DATA", payload ? "SECURED" : "READY",
                payload ? GameConfig.COLOR_GREEN : new Color(120, 160, 200), payload);

        // --- PROGRESSION MATRIX BUTTON ---
        int progX = invStartX + (slotW + gap) * 4 + 14;
        int progW = 145;
        int sp = progression.getSkillPoints();
        boolean hasPoints = sp > 0;

        g2d.setColor(new Color(10, 15, 25, 230));
        g2d.fillRoundRect(progX, y, progW, slotH, 6, 6);
        g2d.setColor(hasPoints ? GameConfig.COLOR_GREEN : new Color(40, 60, 85));
        g2d.drawRoundRect(progX, y, progW, slotH, 6, 6);

        g2d.setFont(FONT_CODE);
        g2d.setColor(hasPoints ? GameConfig.COLOR_GREEN : GameConfig.COLOR_TEXT_BRIGHT);
        g2d.drawString("[P] UPGRADES", progX + 10, y + 18);

        g2d.setFont(FONT_BADGE);
        g2d.setColor(hasPoints ? new Color(0, 255, 128) : GameConfig.COLOR_TEXT_MUTED);
        g2d.drawString(String.format("POINTS: %d SP", sp), progX + 10, y + 34);

        if (hasPoints) {
            g2d.setColor(new Color(0, 255, 128));
            g2d.fillOval(progX + progW - 16, y + 10, 8, 8);
        }
    }

    private void renderSlot(Graphics2D g2d, int x, int y, int w, int h, String title, String value, Color accent, boolean active) {
        g2d.setColor(new Color(12, 17, 26, 225));
        g2d.fillRoundRect(x, y, w, h, 6, 6);

        g2d.setColor(active ? accent : new Color(35, 50, 70));
        g2d.drawRoundRect(x, y, w, h, 6, 6);

        g2d.setFont(FONT_CODE);
        g2d.setColor(active ? Color.WHITE : Color.GRAY);
        g2d.drawString(title, x + 8, y + 16);

        g2d.setFont(FONT_CODE);
        g2d.setColor(accent);
        g2d.drawString(value, x + 8, y + 34);
    }
}
