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
 * High-density Tier-IV Enterprise Server Rack holding classified data cores.
 * Implements Hackable allowing high-value corporate data exfiltration.
 */
public final class ServerRack implements Interactable, Collidable, Hackable {

    private static final Logger LOGGER = Logger.getLogger(ServerRack.class);

    private final String name;
    private final double x;
    private final double y;
    private final double width;
    private final double height;
    private final BoundingBox bounds;
    private final BoundingBox interactionZone;

    private boolean hacked = false;
    private double ledTimer = 0.0;
    private final int dataValueCredits = 250000;

    public ServerRack(String name, double x, double y, double width, double height) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.bounds = new BoundingBox(x, y, width, height);

        double pad = 24.0;
        this.interactionZone = new BoundingBox(x - pad, y - pad, width + (pad * 2), height + (pad * 2));
    }

    public void update(double deltaTime) {
        ledTimer += deltaTime * 5.0;
    }

    @Override
    public String getInteractionPrompt() {
        if (hacked) {
            return "[EXTRACTED] " + name + " Data Downloaded ($" + dataValueCredits + ")";
        }
        return "[E] Hack Data Core // " + name + " (HARD)";
    }

    @Override
    public boolean canInteract() {
        return !hacked;
    }

    @Override
    public void onInteract() {
        LOGGER.info("Interacting with server rack data port: " + name);
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
        return name + " // TIER-IV DATA CORE";
    }

    @Override
    public void onHackSuccess() {
        this.hacked = true;
        LOGGER.info(String.format("Server Rack '%s' cracked! Extracted encrypted records valued at $%d credits.",
                name, dataValueCredits));
    }

    @Override
    public void onHackFailure() {
        LOGGER.info("Server Rack '" + name + "' intrusion defense triggered thermal lock.");
    }

    @Override
    public boolean isHacked() {
        return hacked;
    }

    public void setHacked(boolean hacked) {
        this.hacked = hacked;
    }

    public int getDataValueCredits() {
        return dataValueCredits;
    }

    public void render(Graphics2D g2d, boolean playerInRange) {
        int dx = (int) Math.round(x);
        int dy = (int) Math.round(y);
        int dw = (int) Math.round(width);
        int dh = (int) Math.round(height);

        // Server Rack Cabinet Chassis
        g2d.setColor(new Color(0x12, 0x18, 0x22));
        g2d.fillRect(dx, dy, dw, dh);
        g2d.setColor(hacked ? GameConfig.COLOR_GREEN : new Color(0x28, 0x3E, 0x54));
        g2d.drawRect(dx, dy, dw, dh);

        // Rack Unit Shelves & Blinking Activity LEDs
        int units = Math.max(2, (int) (dh / 10));
        for (int i = 0; i < units; i++) {
            int uy = dy + 3 + (i * 9);
            g2d.setColor(new Color(0x1C, 0x26, 0x34));
            g2d.fillRect(dx + 3, uy, dw - 6, 7);

            // LED pulse
            boolean ledOn = Math.sin(ledTimer + i) > 0.0;
            Color ledColor = hacked ? (ledOn ? GameConfig.COLOR_GREEN : new Color(0, 80, 40)) :
                    (ledOn ? GameConfig.COLOR_CYAN : new Color(0, 40, 70));
            g2d.setColor(ledColor);
            g2d.fillRect(dx + dw - 10, uy + 2, 4, 3);
            g2d.fillRect(dx + dw - 16, uy + 2, 4, 3);
        }

        if (playerInRange) {
            g2d.setColor(new Color(0, 240, 255, 90));
            g2d.drawRect(dx - 2, dy - 2, dw + 4, dh + 4);
        }
    }
}
