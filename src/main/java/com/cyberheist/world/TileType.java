package com.cyberheist.world;

import java.awt.Color;

/**
 * Floor and structural surface classifications for corporate architecture.
 */
public enum TileType {
    STREET_ASPHALT(new Color(0x1F, 0x22, 0x27), new Color(0x18, 0x1A, 0x1F), false),
    STREET_SIDEWALK(new Color(0x3B, 0x40, 0x48), new Color(0x2E, 0x32, 0x38), false),
    LOBBY_MARBLE(new Color(0x2C, 0x33, 0x3F), new Color(0x22, 0x27, 0x31), false),
    SECURITY_CONCRETE(new Color(0x23, 0x28, 0x32), new Color(0x1B, 0x1F, 0x26), false),
    OFFICE_CARPET(new Color(0x1B, 0x26, 0x36), new Color(0x15, 0x1E, 0x2B), false),
    SERVER_COOLING_TILES(new Color(0x15, 0x1F, 0x28), new Color(0x0E, 0x16, 0x1E), false),
    VAULT_STEEL_PLATING(new Color(0x19, 0x1C, 0x22), new Color(0x12, 0x14, 0x18), false),
    HAZARD_ZONE(new Color(0x3A, 0x35, 0x1A), new Color(0x4D, 0x42, 0x15), false),
    WALL(new Color(0x0C, 0x10, 0x18), new Color(0x18, 0x24, 0x36), true);

    private final Color baseColor;
    private final Color gridColor;
    private final boolean solid;

    TileType(Color baseColor, Color gridColor, boolean solid) {
        this.baseColor = baseColor;
        this.gridColor = gridColor;
        this.solid = solid;
    }

    public Color getBaseColor() { return baseColor; }
    public Color getGridColor() { return gridColor; }
    public boolean isSolid() { return solid; }
}
