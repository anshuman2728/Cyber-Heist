package com.cyberheist.input;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;

/**
 * Handles keyboard input by buffering AWT key events and synchronizing them
 * with the game loop's update cycle to avoid concurrency issues.
 */
public final class KeyboardInput extends KeyAdapter {

    private static final int KEY_COUNT = 512;

    // Raw state written asynchronously by AWT Event Dispatch Thread
    private final boolean[] rawKeys = new boolean[KEY_COUNT];

    // Synchronized states read deterministically by Game Engine thread
    private final boolean[] currentKeys = new boolean[KEY_COUNT];
    private final boolean[] previousKeys = new boolean[KEY_COUNT];

    private final Object stateLock = new Object();

    /**
     * Synchronizes raw AWT event state into current frame state.
     * Must be called once per game loop tick before logic update.
     */
    public void poll() {
        synchronized (stateLock) {
            System.arraycopy(currentKeys, 0, previousKeys, 0, KEY_COUNT);
            System.arraycopy(rawKeys, 0, currentKeys, 0, KEY_COUNT);
        }
    }

    /**
     * Checks if a key is currently held down.
     */
    public boolean isKeyDown(int keyCode) {
        if (keyCode < 0 || keyCode >= KEY_COUNT) return false;
        return currentKeys[keyCode];
    }

    /**
     * Checks if a key was pressed during the current frame (single trigger).
     */
    public boolean isKeyPressed(int keyCode) {
        if (keyCode < 0 || keyCode >= KEY_COUNT) return false;
        return currentKeys[keyCode] && !previousKeys[keyCode];
    }

    /**
     * Checks if a key was released during the current frame.
     */
    public boolean isKeyReleased(int keyCode) {
        if (keyCode < 0 || keyCode >= KEY_COUNT) return false;
        return !currentKeys[keyCode] && previousKeys[keyCode];
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code >= 0 && code < KEY_COUNT) {
            synchronized (stateLock) {
                rawKeys[code] = true;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (code >= 0 && code < KEY_COUNT) {
            synchronized (stateLock) {
                rawKeys[code] = false;
            }
        }
    }

    /**
     * Clears all pressed states (useful during window focus loss).
     */
    public void reset() {
        synchronized (stateLock) {
            Arrays.fill(rawKeys, false);
            Arrays.fill(currentKeys, false);
            Arrays.fill(previousKeys, false);
        }
    }
}
