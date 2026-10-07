package com.cyberheist.input;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.Arrays;

/**
 * Handles mouse button states, screen position, and scroll wheel events.
 * Synchronizes AWT events with the game loop cycle.
 */
public final class MouseInput extends MouseAdapter {

    private static final int BUTTON_COUNT = 8;

    // Raw AWT event state
    private final boolean[] rawButtons = new boolean[BUTTON_COUNT];
    private int rawX = 0;
    private int rawY = 0;
    private double rawScrollDelta = 0;

    // Synchronized game tick state
    private final boolean[] currentButtons = new boolean[BUTTON_COUNT];
    private final boolean[] previousButtons = new boolean[BUTTON_COUNT];
    private int currentX = 0;
    private int currentY = 0;
    private double currentScrollDelta = 0;

    private final Object stateLock = new Object();

    /**
     * Synchronizes raw AWT mouse state into current frame state.
     */
    public void poll() {
        synchronized (stateLock) {
            System.arraycopy(currentButtons, 0, previousButtons, 0, BUTTON_COUNT);
            System.arraycopy(rawButtons, 0, currentButtons, 0, BUTTON_COUNT);
            currentX = rawX;
            currentY = rawY;
            currentScrollDelta = rawScrollDelta;
            rawScrollDelta = 0; // Reset scroll delta per poll
        }
    }

    public boolean isButtonDown(int button) {
        if (button < 0 || button >= BUTTON_COUNT) return false;
        return currentButtons[button];
    }

    public boolean isButtonClicked(int button) {
        if (button < 0 || button >= BUTTON_COUNT) return false;
        return currentButtons[button] && !previousButtons[button];
    }

    public boolean isButtonReleased(int button) {
        if (button < 0 || button >= BUTTON_COUNT) return false;
        return !currentButtons[button] && previousButtons[button];
    }

    public int getX() {
        return currentX;
    }

    public int getY() {
        return currentY;
    }

    public Point getPosition() {
        return new Point(currentX, currentY);
    }

    public double getScrollDelta() {
        return currentScrollDelta;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        int button = e.getButton();
        if (button >= 0 && button < BUTTON_COUNT) {
            synchronized (stateLock) {
                rawButtons[button] = true;
                rawX = e.getX();
                rawY = e.getY();
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        int button = e.getButton();
        if (button >= 0 && button < BUTTON_COUNT) {
            synchronized (stateLock) {
                rawButtons[button] = false;
                rawX = e.getX();
                rawY = e.getY();
            }
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        synchronized (stateLock) {
            rawX = e.getX();
            rawY = e.getY();
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        synchronized (stateLock) {
            rawX = e.getX();
            rawY = e.getY();
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        synchronized (stateLock) {
            rawScrollDelta += e.getPreciseWheelRotation();
        }
    }

    public void reset() {
        synchronized (stateLock) {
            Arrays.fill(rawButtons, false);
            Arrays.fill(currentButtons, false);
            Arrays.fill(previousButtons, false);
            rawScrollDelta = 0;
            currentScrollDelta = 0;
        }
    }
}
