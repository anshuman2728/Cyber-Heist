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
 * Interactive corporate door (glass slider, security badge gate, heavy vault door).
 * Dynamically toggles collision and animates sliding/pivot transitions.
 * Implements Hackable allowing electronic lock bypass.
 */
public final class Door implements Interactable, Collidable, Hackable {

    private static final Logger LOGGER = Logger.getLogger(Door.class);

    public enum DoorType {
        GLASS_SLIDER("Glass Sliding Door", new Color(0x3A, 0x55, 0x6E, 180), new Color(0x6A, 0x88, 0xA8)),
        SECURITY_BADGE("Security Access Gate", new Color(0x28, 0x33, 0x44), GameConfig.COLOR_CYAN),
        HEAVY_VAULT("Reinforced Blast Door", new Color(0x22, 0x27, 0x30), new Color(0x8A, 0x98, 0xAA)),
        EMERGENCY_EXIT("Fire Emergency Door", new Color(0x5A, 0x1A, 0x1A), new Color(0xE0, 0x30, 0x30));

        private final String label;
        private final Color panelColor;
        private final Color frameColor;

        DoorType(String label, Color panelColor, Color frameColor) {
            this.label = label;
            this.panelColor = panelColor;
            this.frameColor = frameColor;
        }
    }

    private final String name;
    private final DoorType type;
    private final double x;
    private final double y;
    private final double width;
    private final double height;
    private final boolean horizontal;

    private boolean open = false;
    private boolean locked = false;
    private boolean hacked = false;
    private boolean lockdownProtected = false;
    private boolean lockedDown = false;
    private String requiredKey = null;

    private double openProgress = 0.0; // 0.0 = closed, 1.0 = fully open
    private final BoundingBox closedBounds;
    private final BoundingBox interactionZone;
    private Runnable noiseCallback = null;

    public Door(String name, DoorType type, double x, double y, double width, double height, boolean horizontal) {
        this.name = name;
        this.type = type;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.horizontal = horizontal;

        this.closedBounds = new BoundingBox(x, y, width, height);

        // Interaction zone extends 28px around door frame
        double pad = 28.0;
        this.interactionZone = new BoundingBox(x - pad, y - pad, width + (pad * 2), height + (pad * 2));
    }

    public void update(double deltaTime) {
        double speed = 3.5; // Open/close animation speed
        if (open && openProgress < 1.0) {
            openProgress = Math.min(1.0, openProgress + speed * deltaTime);
        } else if (!open && openProgress > 0.0) {
            openProgress = Math.max(0.0, openProgress - speed * deltaTime);
        }
    }

    @Override
    public String getInteractionPrompt() {
        if (lockedDown) {
            return "[LOCKDOWN SEALED] " + name + " // [E] Override (HARD)";
        }
        if (locked) {
            return "[E] Hack Door Lock // " + name + " (" + getDifficulty().name() + ")";
        }
        return open ? "[E] Close " + name : "[E] Open " + name;
    }

    @Override
    public boolean canInteract() {
        return true;
    }

    @Override
    public HackingDifficulty getDifficulty() {
        if (lockedDown) return HackingDifficulty.HARD;
        return type == DoorType.HEAVY_VAULT ? HackingDifficulty.HARD : HackingDifficulty.MEDIUM;
    }

    @Override
    public String getTargetName() {
        return name + " // ACCESS BYPASS";
    }

    @Override
    public void onHackSuccess() {
        this.lockedDown = false;
        this.locked = false;
        this.hacked = true;
        this.open = true;
        LOGGER.info("Door '" + name + "' security lock breached! Door unlocked and opened.");
        if (stateListener != null) {
            stateListener.onDoorStateChanged(this);
        }
        if (noiseCallback != null) {
            noiseCallback.run();
        }
    }

    @Override
    public void onHackFailure() {
        LOGGER.info("Door '" + name + "' security lock breach failed!");
    }

    @Override
    public boolean isHacked() {
        return hacked;
    }

    @Override
    public boolean canBeHacked() {
        return locked;
    }

    public interface DoorStateListener {
        void onDoorStateChanged(Door door);
    }

    private DoorStateListener stateListener = null;

    public void setStateListener(DoorStateListener listener) {
        this.stateListener = listener;
    }

    @Override
    public void onInteract() {
        if (!locked) {
            open = !open;
            LOGGER.info("Door '" + name + "' toggled to: " + (open ? "OPEN" : "CLOSED"));
            if (stateListener != null) {
                stateListener.onDoorStateChanged(this);
            }
            if (noiseCallback != null) {
                noiseCallback.run();
            }
        }
    }

    @Override
    public BoundingBox getBoundingBox() {
        // When open, collisions are disabled to allow walking through
        if (open) {
            return new BoundingBox(0, 0, 0, 0); // No collision
        }
        return closedBounds;
    }

    @Override
    public boolean isSolid() {
        return !open;
    }

    @Override
    public BoundingBox getInteractionTriggerZone() {
        return interactionZone;
    }

    @Override
    public String getName() {
        return name;
    }

    public void render(Graphics2D g2d, boolean playerInRange) {
        int dx = (int) Math.round(x);
        int dy = (int) Math.round(y);
        int dw = (int) Math.round(width);
        int dh = (int) Math.round(height);

        // Frame
        g2d.setColor(lockedDown ? new Color(0x8A, 0x20, 0x20) : type.frameColor);
        g2d.drawRect(dx, dy, dw, dh);

        // Calculate sliding leaf position
        int panelX = dx;
        int panelY = dy;
        int panelW = dw;
        int panelH = dh;

        if (horizontal) {
            // Slides sideways along width
            int slide = (int) Math.round((dw - 6) * openProgress);
            panelX += slide;
            panelW = Math.max(6, dw - slide);
        } else {
            // Slides vertically along height
            int slide = (int) Math.round((dh - 6) * openProgress);
            panelY += slide;
            panelH = Math.max(6, dh - slide);
        }

        // Door panel
        g2d.setColor(lockedDown ? new Color(0x3A, 0x18, 0x18) : type.panelColor);
        g2d.fillRect(panelX, panelY, panelW, panelH);

        // Door frame hardware / handle
        g2d.setColor(lockedDown ? Color.RED : (open ? GameConfig.COLOR_GREEN : (locked ? GameConfig.COLOR_MAGENTA : type.frameColor)));
        if (horizontal) {
            g2d.fillRect(panelX + (panelW / 2) - 3, panelY + 2, 6, panelH - 4);
        } else {
            g2d.fillRect(panelX + 2, panelY + (panelH / 2) - 3, panelW - 4, 6);
        }

        // Lockdown Hazard Indicator
        if (lockedDown) {
            g2d.setColor(new Color(255, 40, 40, 160));
            g2d.drawRect(dx - 1, dy - 1, dw + 2, dh + 2);
            // Hazard beacon
            g2d.setColor(Color.RED);
            g2d.fillRect(dx + (dw / 2) - 2, dy + (dh / 2) - 2, 4, 4);
        }

        // Highlight ring if player is in range
        if (playerInRange) {
            g2d.setColor(lockedDown ? new Color(255, 80, 80, 140) : new Color(0, 240, 255, 100));
            g2d.drawRect(dx - 2, dy - 2, dw + 4, dh + 4);
        }
    }

    public boolean isOpen() { return open; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }
    public void setLocked(boolean locked, String requiredKey) {
        this.locked = locked;
        this.requiredKey = requiredKey;
    }

    public boolean isLockdownProtected() { return lockdownProtected; }
    public void setLockdownProtected(boolean lockdownProtected) { this.lockdownProtected = lockdownProtected; }
    public boolean isLockedDown() { return lockedDown; }
    public void setLockedDown(boolean lockedDown) { this.lockedDown = lockedDown; }

    public void engageLockdown(boolean engage) {
        if (engage && lockdownProtected) {
            this.lockedDown = true;
            this.locked = true;
            this.open = false;
            this.openProgress = 0.0;
            if (stateListener != null) {
                stateListener.onDoorStateChanged(this);
            }
        } else if (!engage && lockedDown) {
            this.lockedDown = false;
            this.locked = false;
            if (stateListener != null) {
                stateListener.onDoorStateChanged(this);
            }
        }
    }

    public void setOpen(boolean open) {
        this.open = open;
        this.openProgress = open ? 1.0 : 0.0;
        if (stateListener != null) {
            stateListener.onDoorStateChanged(this);
        }
    }

    public void setHacked(boolean hacked) {
        this.hacked = hacked;
    }

    public boolean unlockWithLockpick() {
        if (!locked) return false;
        this.locked = false;
        this.open = true;
        LOGGER.info("Door '" + name + "' lock bypassed with electronic lockpick!");
        if (stateListener != null) {
            stateListener.onDoorStateChanged(this);
        }
        return true;
    }

    public void setNoiseCallback(Runnable noiseCallback) {
        this.noiseCallback = noiseCallback;
    }
}
