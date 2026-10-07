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
 * High-tech Transit / Extraction Elevator with floor indicator and call buttons.
 * Implements Hackable allowing electronic call panel bypass to call extraction car.
 */
public final class Elevator implements Interactable, Collidable, Hackable {

    private static final Logger LOGGER = Logger.getLogger(Elevator.class);

    private final String name;
    private final double x;
    private final double y;
    private final double width;
    private final double height;
    private final BoundingBox bounds;
    private final BoundingBox interactionZone;

    private boolean called = false;
    private boolean hacked = false;
    private double doorOpenProgress = 0.0;

    public Elevator(String name, double x, double y, double width, double height) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.bounds = new BoundingBox(x, y, width, height);

        double pad = 28.0;
        this.interactionZone = new BoundingBox(x - pad, y - pad, width + (pad * 2), height + (pad * 2));
    }

    public void update(double deltaTime) {
        if (called && doorOpenProgress < 1.0) {
            doorOpenProgress = Math.min(1.0, doorOpenProgress + deltaTime * 2.0);
        }
    }

    @Override
    public String getInteractionPrompt() {
        if (!hacked && !called) {
            return "[E] Hack Elevator Panel // " + name + " (MEDIUM)";
        }
        return called ? "[ARRIVED] " + name + " Ready for Evacuation" : "[E] Call Elevator Car";
    }

    @Override
    public boolean canInteract() {
        return true;
    }

    @Override
    public void onInteract() {
        if (hacked) {
            this.called = true;
            LOGGER.info("Elevator car summoned to floor.");
        }
    }

    @Override
    public BoundingBox getBoundingBox() {
        // Once doors are fully open (>70%), player can walk inside
        if (doorOpenProgress > 0.7) {
            return new BoundingBox(0, 0, 0, 0);
        }
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
        return HackingDifficulty.MEDIUM;
    }

    @Override
    public String getTargetName() {
        return name + " // ELEVATOR LOGIC";
    }

    @Override
    public void onHackSuccess() {
        this.hacked = true;
        this.called = true;
        LOGGER.info("Elevator '" + name + "' control logic bypassed! Shaft doors opening.");
    }

    @Override
    public void onHackFailure() {
        LOGGER.info("Elevator '" + name + "' call panel locked down.");
    }

    @Override
    public boolean isHacked() {
        return hacked;
    }

    public void setHacked(boolean hacked) {
        this.hacked = hacked;
    }

    public boolean isCalled() {
        return called;
    }

    public void setCalled(boolean called) {
        this.called = called;
        if (called) {
            this.doorOpenProgress = 1.0;
        }
    }

    public void render(Graphics2D g2d, boolean playerInRange) {
        int dx = (int) Math.round(x);
        int dy = (int) Math.round(y);
        int dw = (int) Math.round(width);
        int dh = (int) Math.round(height);

        // Shaft Frame
        g2d.setColor(new Color(0x22, 0x2A, 0x36));
        g2d.fillRect(dx, dy, dw, dh);
        g2d.setColor(called ? GameConfig.COLOR_GREEN : GameConfig.COLOR_CYAN);
        g2d.drawRect(dx, dy, dw, dh);

        // Interior elevator lighting visible as doors part
        if (doorOpenProgress > 0.1) {
            g2d.setColor(new Color(0x0E, 0x38, 0x48));
            g2d.fillRect(dx + 4, dy + 4, dw - 8, dh - 8);
        }

        // Sliding stainless steel double doors
        int slideW = (int) Math.round(((dw - 8) / 2.0) * (1.0 - doorOpenProgress));
        g2d.setColor(new Color(0x40, 0x4D, 0x5C));
        // Left door leaf
        g2d.fillRect(dx + 4, dy + 4, slideW, dh - 8);
        // Right door leaf
        g2d.fillRect(dx + dw - 4 - slideW, dy + 4, slideW, dh - 8);

        // Floor Indicator Display above door
        g2d.setColor(new Color(0x0A, 0x12, 0x1C));
        g2d.fillRect(dx + (dw / 2) - 16, dy - 8, 32, 10);
        g2d.setColor(called ? GameConfig.COLOR_GREEN : Color.YELLOW);
        g2d.setFont(GameConfig.FONT_CODE);
        g2d.drawString(called ? "01▲" : "B2▼", dx + (dw / 2) - 12, dy);

        if (playerInRange) {
            g2d.setColor(new Color(0, 240, 255, 90));
            g2d.drawRect(dx - 2, dy - 2, dw + 4, dh + 4);
        }
    }
}
