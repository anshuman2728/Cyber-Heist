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
 * Interactive corporate computer terminal (reception logging, security CCTV, vault mainframe).
 * Implements Hackable with Tier-I/II encryption sequence puzzles.
 */
public final class ComputerWorkstation implements Interactable, Collidable, Hackable {

    private static final Logger LOGGER = Logger.getLogger(ComputerWorkstation.class);

    public enum WorkstationType {
        RECEPTION_DESK("Visitor Log Terminal", "[E] Access Visitor Logs", HackingDifficulty.EASY),
        SECURITY_CCTV("Security Monitor Feed", "[E] View CCTV Console", HackingDifficulty.MEDIUM),
        OFFICE_PC("Employee Workstation", "[E] Inspect Workstation", HackingDifficulty.EASY),
        VAULT_MAINFRAME("Central Data Core", "[E] Extract Encrypted Data", HackingDifficulty.HARD);

        private final String defaultLabel;
        private final String prompt;
        private final HackingDifficulty difficulty;

        WorkstationType(String defaultLabel, String prompt, HackingDifficulty difficulty) {
            this.defaultLabel = defaultLabel;
            this.prompt = prompt;
            this.difficulty = difficulty;
        }

        public HackingDifficulty getDifficulty() { return difficulty; }
    }

    private final String name;
    private final WorkstationType type;
    private final BoundingBox bounds;
    private final BoundingBox interactionZone;

    private boolean accessed = false;
    private boolean hacked = false;
    private int accessCount = 0;
    private Runnable noiseCallback = null;

    public ComputerWorkstation(String name, WorkstationType type, double x, double y, double width, double height) {
        this.name = name;
        this.type = type;
        this.bounds = new BoundingBox(x, y, width, height);

        double pad = 24.0;
        this.interactionZone = new BoundingBox(x - pad, y - pad, width + (pad * 2), height + (pad * 2));
    }

    @Override
    public String getInteractionPrompt() {
        if (!hacked) {
            return "[E] Hack " + name + " (" + type.getDifficulty().name() + ")";
        }
        return accessed ? "[E] Disconnect from " + name : "[E] Read Files (" + name + ")";
    }

    @Override
    public boolean canInteract() {
        return true;
    }

    @Override
    public void onInteract() {
        if (hacked) {
            this.accessed = !this.accessed;
        }
        this.accessCount++;
        LOGGER.info("Interacted with " + name + " (" + type.defaultLabel + "). Status: " + (accessed ? "CONNECTED" : "DISCONNECTED"));
        if (noiseCallback != null) {
            noiseCallback.run();
        }
    }

    @Override
    public HackingDifficulty getDifficulty() {
        return type.getDifficulty();
    }

    @Override
    public String getTargetName() {
        return name + " // " + type.defaultLabel;
    }

    @Override
    public void onHackSuccess() {
        this.hacked = true;
        this.accessed = true;
        LOGGER.info("ComputerWorkstation '" + name + "' successfully breached!");
    }

    @Override
    public void onHackFailure() {
        LOGGER.info("ComputerWorkstation '" + name + "' hack failed!");
    }

    @Override
    public boolean isHacked() {
        return hacked;
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

    public boolean isAccessed() {
        return accessed;
    }

    public void setHacked(boolean hacked) {
        this.hacked = hacked;
    }

    public void setAccessed(boolean accessed) {
        this.accessed = accessed;
    }

    public void setNoiseCallback(Runnable noiseCallback) {
        this.noiseCallback = noiseCallback;
    }

    public void render(Graphics2D g2d, boolean playerInRange) {
        int x = (int) Math.round(bounds.getX());
        int y = (int) Math.round(bounds.getY());
        int w = (int) Math.round(bounds.getWidth());
        int h = (int) Math.round(bounds.getHeight());

        // Interaction aura if in proximity
        if (playerInRange) {
            g2d.setColor(new Color(0, 240, 255, 30));
            g2d.fillRoundRect((int) interactionZone.getX(), (int) interactionZone.getY(),
                    (int) interactionZone.getWidth(), (int) interactionZone.getHeight(), 10, 10);
            g2d.setColor(new Color(0, 240, 255, 100));
            g2d.drawRoundRect((int) interactionZone.getX(), (int) interactionZone.getY(),
                    (int) interactionZone.getWidth(), (int) interactionZone.getHeight(), 10, 10);
        }

        // Terminal Base Table
        g2d.setColor(new Color(0x1A, 0x22, 0x2E));
        g2d.fillRect(x, y, w, h);
        g2d.setColor(new Color(0x35, 0x43, 0x58));
        g2d.drawRect(x, y, w, h);

        // Terminal Monitor Screen
        Color screenGlow = accessed ? GameConfig.COLOR_GREEN : (type == WorkstationType.VAULT_MAINFRAME ? GameConfig.COLOR_CYAN : new Color(0x42, 0x87, 0xF5));
        g2d.setColor(new Color(0x0A, 0x10, 0x18));
        g2d.fillRect(x + 4, y + 4, w - 8, h - 8);

        g2d.setColor(screenGlow);
        g2d.drawRect(x + 4, y + 4, w - 8, h - 8);

        // Small simulated screen lines
        g2d.setColor(screenGlow);
        g2d.drawLine(x + 8, y + 10, x + w - 8, y + 10);
        g2d.drawLine(x + 8, y + 16, x + (w / 2), y + 16);
    }
}
