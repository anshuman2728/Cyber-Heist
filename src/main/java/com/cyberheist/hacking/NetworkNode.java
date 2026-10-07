package com.cyberheist.hacking;

import java.awt.geom.Rectangle2D;

/**
 * Represents a single clickable / keyable network memory node in the hacking minigame.
 * Example: [ 04 ], [ 17 ], [ 82 ], [ 39 ].
 */
public final class NetworkNode {

    public enum NodeState {
        IDLE,
        SELECTED,
        MATCHED,
        ERROR_FLASH
    }

    private final int index;         // 1-based index (Key shortcut 1..N)
    private final String code;        // 2-character hex or decimal string
    private NodeState state = NodeState.IDLE;
    private double flashTimer = 0.0;
    private final Rectangle2D.Double bounds = new Rectangle2D.Double();

    public NetworkNode(int index, String code) {
        this.index = index;
        this.code = code;
    }

    public void update(double deltaTime) {
        if (flashTimer > 0.0) {
            flashTimer -= deltaTime;
            if (flashTimer <= 0.0 && state == NodeState.ERROR_FLASH) {
                state = NodeState.IDLE;
            }
        }
    }

    public void triggerErrorFlash(double duration) {
        this.state = NodeState.ERROR_FLASH;
        this.flashTimer = duration;
    }

    public void setBounds(double x, double y, double width, double height) {
        this.bounds.setRect(x, y, width, height);
    }

    public boolean containsPoint(double x, double y) {
        return bounds.contains(x, y);
    }

    public int getIndex() { return index; }
    public String getCode() { return code; }
    public NodeState getState() { return state; }
    public void setState(NodeState state) { this.state = state; }
    public Rectangle2D.Double getBounds() { return bounds; }
}
