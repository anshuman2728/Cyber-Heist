package com.cyberheist.world;

import com.cyberheist.collision.BoundingBox;
import com.cyberheist.collision.Collidable;
import com.cyberheist.core.GameConfig;
import com.cyberheist.hacking.Hackable;
import com.cyberheist.hacking.HackingDifficulty;
import com.cyberheist.interact.Interactable;
import com.cyberheist.util.Logger;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Heavy Reinforced Electronic Safe / Lockbox containing classified heist bounty.
 * Implements Hackable with high-security (Tier-IV) biometric/cryptographic cipher sequencing.
 */
public final class Safe implements Interactable, Collidable, Hackable {

    private static final Logger LOGGER = Logger.getLogger(Safe.class);

    private final String name;
    private final double x;
    private final double y;
    private final double width;
    private final double height;
    private final BoundingBox bounds;
    private final BoundingBox interactionZone;

    private boolean opened = false;
    private boolean looted = false;
    private final int lootValueCredits = 500000;

    public Safe(String name, double x, double y, double width, double height) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.bounds = new BoundingBox(x, y, width, height);

        double pad = 24.0;
        this.interactionZone = new BoundingBox(x - pad, y - pad, width + (pad * 2), height + (pad * 2));
    }

    @Override
    public String getInteractionPrompt() {
        if (opened && looted) {
            return "[EMPTY] " + name + " (Loot Secured)";
        } else if (opened) {
            return "[E] Take Classified Bearer Bonds ($" + lootValueCredits + ")";
        }
        return "[E] Crack Electronic Safe // " + name + " (VERY HARD)";
    }

    @Override
    public boolean canInteract() {
        return !looted;
    }

    @Override
    public void onInteract() {
        if (opened && !looted) {
            this.looted = true;
            LOGGER.info(String.format("Safe '%s' looted! Claimed $%d credits in classified bonds.", name, lootValueCredits));
        }
    }

    public boolean unlockWithLockpick() {
        if (opened) return false;
        this.opened = true;
        LOGGER.info("Safe '" + name + "' tumblers picked with electronic bypass probe!");
        return true;
    }

    @Override
    public BoundingBox getBoundingBox() {
        return bounds;
    }

    @Override
    public BoundingBox getInteractionTriggerZone() {
        return interactionZone;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public HackingDifficulty getDifficulty() {
        return HackingDifficulty.VERY_HARD;
    }

    @Override
    public String getTargetName() {
        return name + " // CRYPTO SAFE";
    }

    @Override
    public void onHackSuccess() {
        this.opened = true;
        LOGGER.info("Electronic Safe '" + name + "' cracked! Heavy locking bolts retracted.");
    }

    @Override
    public void onHackFailure() {
        LOGGER.info("Electronic Safe '" + name + "' anti-tamper deadlock engaged.");
    }

    @Override
    public boolean isHacked() {
        return opened;
    }

    public boolean isOpened() {
        return opened;
    }

    public void setOpened(boolean opened) {
        this.opened = opened;
    }

    public boolean isLooted() {
        return looted;
    }

    public void setLooted(boolean looted) {
        this.looted = looted;
    }

    public int getLootValueCredits() {
        return lootValueCredits;
    }

    public void render(Graphics2D g2d, boolean playerInRange) {
        int dx = (int) Math.round(x);
        int dy = (int) Math.round(y);
        int dw = (int) Math.round(width);
        int dh = (int) Math.round(height);

        // Safe Steel Outer Shell
        g2d.setColor(new Color(0x1E, 0x24, 0x2E));
        g2d.fillRect(dx, dy, dw, dh);
        g2d.setColor(opened ? GameConfig.COLOR_GREEN : new Color(0x60, 0x75, 0x8E));
        g2d.drawRect(dx, dy, dw, dh);

        // Heavy Reinforced Dial / Keypad
        int dialSize = Math.min(dw, dh) / 2;
        int dialX = dx + (dw - dialSize) / 2;
        int dialY = dy + (dh - dialSize) / 2;

        g2d.setColor(opened ? new Color(0x0E, 0x3E, 0x24) : new Color(0x32, 0x3E, 0x4D));
        g2d.fillOval(dialX, dialY, dialSize, dialSize);
        g2d.setColor(opened ? GameConfig.COLOR_GREEN : (playerInRange ? Color.YELLOW : Color.DARK_GRAY));
        g2d.drawOval(dialX, dialY, dialSize, dialSize);

        // Electronic status light
        g2d.setColor(opened ? GameConfig.COLOR_GREEN : (playerInRange ? Color.YELLOW : Color.RED));
        g2d.fillRect(dx + 4, dy + 4, 4, 4);

        if (playerInRange) {
            g2d.setColor(new Color(0, 240, 255, 90));
            g2d.drawRect(dx - 2, dy - 2, dw + 4, dh + 4);
        }
    }
}
