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
 * Security Command Terminal controlling wing surveillance feeds, door interlocks, and master overrides.
 * High-tier interaction object requiring Tier-III hacking.
 */
public final class SecurityTerminal implements Interactable, Collidable, Hackable {

    private static final Logger LOGGER = Logger.getLogger(SecurityTerminal.class);

    private final String name;
    private final double x;
    private final double y;
    private final double width;
    private final double height;
    private final BoundingBox bounds;
    private final BoundingBox interactionZone;

    private boolean hacked = false;
    private boolean camerasDisabled = false;
    private Runnable overrideCallback = null;

    public SecurityTerminal(String name, double x, double y, double width, double height) {
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
        if (!hacked) {
            return "[E] Override Security Terminal // " + name + " (HARD)";
        }
        return camerasDisabled ? "[ACCESSED] Facility Feeds Looped" : "[E] Loop Facility Camera Feeds";
    }

    @Override
    public boolean canInteract() {
        return true;
    }

    @Override
    public void onInteract() {
        if (hacked && !camerasDisabled) {
            this.camerasDisabled = true;
            LOGGER.info("Security Terminal '" + name + "' triggered facility camera blackout!");
            if (overrideCallback != null) {
                overrideCallback.run();
            }
        }
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
        return HackingDifficulty.HARD;
    }

    @Override
    public String getTargetName() {
        return name + " // COMMAND MASTER";
    }

    @Override
    public void onHackSuccess() {
        this.hacked = true;
        this.camerasDisabled = true;
        LOGGER.info("Security Terminal '" + name + "' overridden! Master security clearance gained.");
        if (overrideCallback != null) {
            overrideCallback.run();
        }
    }

    @Override
    public void onHackFailure() {
        LOGGER.info("Security Terminal '" + name + "' counter-intrusion triggered!");
    }

    @Override
    public boolean isHacked() {
        return hacked;
    }

    public void setHacked(boolean hacked) {
        this.hacked = hacked;
    }

    public boolean isCamerasDisabled() {
        return camerasDisabled;
    }

    public void setCamerasDisabled(boolean camerasDisabled) {
        this.camerasDisabled = camerasDisabled;
    }

    public void setOverrideCallback(Runnable callback) {
        this.overrideCallback = callback;
    }

    public void render(Graphics2D g2d, boolean playerInRange) {
        int dx = (int) Math.round(x);
        int dy = (int) Math.round(y);
        int dw = (int) Math.round(width);
        int dh = (int) Math.round(height);

        // Terminal Base Station
        g2d.setColor(new Color(0x18, 0x22, 0x30));
        g2d.fillRect(dx, dy, dw, dh);
        g2d.setColor(hacked ? GameConfig.COLOR_GREEN : GameConfig.COLOR_CYAN);
        g2d.drawRect(dx, dy, dw, dh);

        // Multi-Monitor Array
        int screenW = (dw - 12) / 2;
        int screenH = dh - 10;
        g2d.setColor(hacked ? new Color(0x0A, 0x3E, 0x24) : new Color(0x10, 0x26, 0x3A));
        g2d.fillRect(dx + 4, dy + 5, screenW, screenH);
        g2d.fillRect(dx + 8 + screenW, dy + 5, screenW, screenH);

        g2d.setColor(hacked ? GameConfig.COLOR_GREEN : new Color(0x35, 0x7E, 0xB0));
        g2d.drawRect(dx + 4, dy + 5, screenW, screenH);
        g2d.drawRect(dx + 8 + screenW, dy + 5, screenW, screenH);

        // Blinking status LED
        g2d.setColor(hacked ? GameConfig.COLOR_GREEN : (playerInRange ? Color.YELLOW : Color.RED));
        g2d.fillRect(dx + dw - 6, dy + 2, 4, 4);

        if (playerInRange) {
            g2d.setColor(new Color(0, 240, 255, 90));
            g2d.drawRect(dx - 2, dy - 2, dw + 4, dh + 4);
        }
    }
}
