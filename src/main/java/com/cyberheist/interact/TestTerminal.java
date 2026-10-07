package com.cyberheist.interact;

import com.cyberheist.collision.BoundingBox;
import com.cyberheist.collision.Collidable;
import com.cyberheist.core.GameConfig;
import com.cyberheist.util.Logger;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Concrete test terminal proving interaction and physical collision.
 */
public final class TestTerminal implements Interactable, Collidable {

    private static final Logger LOGGER = Logger.getLogger(TestTerminal.class);

    private final String name;
    private final BoundingBox physicalBounds;
    private final BoundingBox interactionZone;

    private boolean accessed = false;
    private int accessCount = 0;

    public TestTerminal(String name, double x, double y, double width, double height) {
        this.name = name;
        this.physicalBounds = new BoundingBox(x, y, width, height);

        // Interaction zone extends 24px around physical bounds
        double padding = 24.0;
        this.interactionZone = new BoundingBox(
                x - padding,
                y - padding,
                width + (padding * 2),
                height + (padding * 2)
        );
    }

    @Override
    public String getInteractionPrompt() {
        return accessed ? "[E] Re-sync Terminal" : "[E] Access Security Terminal";
    }

    @Override
    public boolean canInteract() {
        return true;
    }

    @Override
    public void onInteract() {
        accessed = !accessed;
        accessCount++;
        LOGGER.info("Terminal '" + name + "' toggled. Access count: " + accessCount + ", State: " + (accessed ? "ACTIVE" : "STANDBY"));
    }

    @Override
    public BoundingBox getInteractionTriggerZone() {
        return interactionZone;
    }

    @Override
    public BoundingBox getBoundingBox() {
        return physicalBounds;
    }

    @Override
    public boolean isSolid() {
        return true;
    }

    @Override
    public String getName() {
        return name;
    }

    public boolean isAccessed() {
        return accessed;
    }

    public void render(Graphics2D g2d, boolean playerInRange) {
        int x = (int) Math.round(physicalBounds.getX());
        int y = (int) Math.round(physicalBounds.getY());
        int w = (int) Math.round(physicalBounds.getWidth());
        int h = (int) Math.round(physicalBounds.getHeight());

        // Interaction zone aura if player is in range
        if (playerInRange) {
            g2d.setColor(new Color(0, 240, 255, 35));
            g2d.fillRoundRect((int) interactionZone.getX(), (int) interactionZone.getY(),
                    (int) interactionZone.getWidth(), (int) interactionZone.getHeight(), 12, 12);

            g2d.setColor(new Color(0, 240, 255, 120));
            g2d.drawRoundRect((int) interactionZone.getX(), (int) interactionZone.getY(),
                    (int) interactionZone.getWidth(), (int) interactionZone.getHeight(), 12, 12);
        }

        // Terminal Base
        g2d.setColor(new Color(0x11, 0x1A, 0x2C));
        g2d.fillRect(x, y, w, h);

        // Terminal Screen Glow
        Color screenColor = accessed ? GameConfig.COLOR_GREEN : GameConfig.COLOR_MAGENTA;
        g2d.setColor(screenColor);
        g2d.fillRect(x + 6, y + 6, w - 12, h - 12);

        // Outer Border
        g2d.setColor(playerInRange ? GameConfig.COLOR_CYAN : GameConfig.COLOR_GRID_LINE);
        g2d.drawRect(x, y, w, h);

        // Header label
        g2d.setFont(GameConfig.FONT_CODE);
        g2d.setColor(GameConfig.COLOR_TEXT_BRIGHT);
        g2d.drawString(name, x + 4, y - 6);

        // Status text on console
        g2d.setColor(Color.BLACK);
        g2d.drawString(accessed ? "ONLINE" : "LOCKED", x + 8, y + (h / 2) + 4);
    }
}
