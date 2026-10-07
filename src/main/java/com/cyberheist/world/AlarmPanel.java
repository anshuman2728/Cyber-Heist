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
 * Wall-mounted Facility Alarm Panel.
 * Controls the sector sirens and security strobe lights.
 * Can be sabotaged/disarmed via Network Node hacking to neutralize facility alert broadcasts.
 */
public final class AlarmPanel implements Interactable, Collidable, Hackable {

    private static final Logger LOGGER = Logger.getLogger(AlarmPanel.class);

    private final String name;
    private final double x;
    private final double y;
    private final double width;
    private final double height;
    private final BoundingBox bounds;
    private final BoundingBox interactionZone;

    private boolean disarmed = false;
    private boolean activeAlarm = false;
    private double strobeTimer = 0.0;

    public AlarmPanel(String name, double x, double y, double width, double height) {
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
        if (activeAlarm) {
            strobeTimer += deltaTime * 8.0;
        }
    }

    @Override
    public String getInteractionPrompt() {
        if (disarmed) {
            return "[DISARMED] " + name + " Sirens Bypassed";
        }
        return "[E] Sabotage Alarm Panel // " + name + " (MEDIUM)";
    }

    @Override
    public boolean canInteract() {
        return !disarmed;
    }

    @Override
    public void onInteract() {
        LOGGER.info("Interacting with Alarm Panel: " + name);
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
        return HackingDifficulty.MEDIUM;
    }

    @Override
    public String getTargetName() {
        return name + " // SIREN CONTROLLER";
    }

    public interface AlarmStateListener {
        void onAlarmTriggered(AlarmPanel panel);
        void onAlarmDisarmed(AlarmPanel panel);
    }

    private AlarmStateListener listener = null;

    public void setAlarmStateListener(AlarmStateListener listener) {
        this.listener = listener;
    }

    @Override
    public void onHackSuccess() {
        this.disarmed = true;
        this.activeAlarm = false;
        LOGGER.info("Alarm Panel '" + name + "' successfully sabotaged and disarmed!");
        if (listener != null) {
            listener.onAlarmDisarmed(this);
        }
    }

    @Override
    public void onHackFailure() {
        this.activeAlarm = true;
        LOGGER.info("Alarm Panel '" + name + "' tamper sensor triggered! SIREN ACTIVATED!");
        if (listener != null) {
            listener.onAlarmTriggered(this);
        }
    }

    @Override
    public boolean isHacked() {
        return disarmed;
    }

    public boolean isDisarmed() {
        return disarmed;
    }

    public void setDisarmed(boolean disarmed) {
        this.disarmed = disarmed;
    }

    public boolean isActiveAlarm() {
        return activeAlarm;
    }

    public void setActiveAlarm(boolean activeAlarm) {
        this.activeAlarm = activeAlarm;
        if (activeAlarm && listener != null) {
            listener.onAlarmTriggered(this);
        } else if (!activeAlarm && listener != null) {
            listener.onAlarmDisarmed(this);
        }
    }

    public void triggerAlarm() {
        if (!disarmed) {
            this.activeAlarm = true;
            LOGGER.info("ALARM ACTIVATED on panel: " + name);
            if (listener != null) {
                listener.onAlarmTriggered(this);
            }
        }
    }

    public void render(Graphics2D g2d, boolean playerInRange) {
        int dx = (int) Math.round(x);
        int dy = (int) Math.round(y);
        int dw = (int) Math.round(width);
        int dh = (int) Math.round(height);

        // Panel Housing
        g2d.setColor(new Color(0x28, 0x1A, 0x1A));
        g2d.fillRect(dx, dy, dw, dh);
        g2d.setColor(disarmed ? GameConfig.COLOR_GREEN : (activeAlarm ? Color.RED : new Color(0xC0, 0x40, 0x40)));
        g2d.drawRect(dx, dy, dw, dh);

        // Strobe Siren Beacon
        Color strobeColor = disarmed ? GameConfig.COLOR_GREEN :
                (activeAlarm ? ((Math.sin(strobeTimer) > 0) ? Color.RED : Color.DARK_GRAY) : new Color(200, 40, 40));
        g2d.setColor(strobeColor);
        g2d.fillOval(dx + (dw / 2) - 5, dy + 4, 10, 10);
        g2d.setColor(Color.WHITE);
        g2d.drawOval(dx + (dw / 2) - 5, dy + 4, 10, 10);

        // Speaker grille slits
        g2d.setColor(Color.BLACK);
        for (int i = 0; i < 3; i++) {
            g2d.drawLine(dx + 4, dy + 20 + (i * 4), dx + dw - 4, dy + 20 + (i * 4));
        }

        if (playerInRange) {
            g2d.setColor(new Color(0, 240, 255, 90));
            g2d.drawRect(dx - 2, dy - 2, dw + 4, dh + 4);
        }
    }
}
