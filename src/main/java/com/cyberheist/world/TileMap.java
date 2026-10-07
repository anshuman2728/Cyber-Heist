package com.cyberheist.world;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Grid-based floor surface representation.
 * Renders floor textures, patterns, and materials per room type.
 */
public final class TileMap {

    private final int columns;
    private final int rows;
    private final int tileSize;
    private final TileType[][] tiles;

    public TileMap(int columns, int rows, int tileSize, TileType defaultTile) {
        this.columns = columns;
        this.rows = rows;
        this.tileSize = tileSize;
        this.tiles = new TileType[columns][rows];

        for (int c = 0; c < columns; c++) {
            for (int r = 0; r < rows; r++) {
                tiles[c][r] = defaultTile;
            }
        }
    }

    public void setTile(int col, int row, TileType type) {
        if (col >= 0 && col < columns && row >= 0 && row < rows) {
            tiles[col][row] = type;
        }
    }

    public void fillRect(int startCol, int startRow, int width, int height, TileType type) {
        for (int c = startCol; c < startCol + width && c < columns; c++) {
            for (int r = startRow; r < startRow + height && r < rows; r++) {
                if (c >= 0 && r >= 0) {
                    tiles[c][r] = type;
                }
            }
        }
    }

    public TileType getTile(int col, int row) {
        if (col >= 0 && col < columns && row >= 0 && row < rows) {
            return tiles[col][row];
        }
        return TileType.WALL;
    }

    /**
     * Renders floor tiles within the specified bounding rectangle.
     */
    public void render(Graphics2D g2d, int viewMinX, int viewMinY, int viewMaxX, int viewMaxY) {
        int startCol = Math.max(0, viewMinX / tileSize);
        int endCol = Math.min(columns - 1, (viewMaxX / tileSize) + 1);
        int startRow = Math.max(0, viewMinY / tileSize);
        int endRow = Math.min(rows - 1, (viewMaxY / tileSize) + 1);

        for (int c = startCol; c <= endCol; c++) {
            for (int r = startRow; r <= endRow; r++) {
                TileType type = tiles[c][r];
                int x = c * tileSize;
                int y = r * tileSize;

                // Base floor
                g2d.setColor(type.getBaseColor());
                g2d.fillRect(x, y, tileSize, tileSize);

                // Pattern rendering
                renderTilePattern(g2d, type, x, y, tileSize);
            }
        }
    }

    private void renderTilePattern(Graphics2D g2d, TileType type, int x, int y, int size) {
        switch (type) {
            case LOBBY_MARBLE, SECURITY_CONCRETE, OFFICE_CARPET -> {
                g2d.setColor(type.getGridColor());
                g2d.drawRect(x, y, size, size);
            }
            case SERVER_COOLING_TILES -> {
                // Perforated vent grill pattern
                g2d.setColor(type.getGridColor());
                g2d.drawRect(x, y, size, size);
                g2d.setColor(new Color(0x28, 0x38, 0x48));
                g2d.fillRect(x + (size / 4), y + (size / 4), size / 2, size / 2);
            }
            case VAULT_STEEL_PLATING -> {
                // Steel plate border with corner rivets
                g2d.setColor(type.getGridColor());
                g2d.drawRect(x, y, size, size);
                g2d.setColor(new Color(0x38, 0x40, 0x50));
                g2d.fillRect(x + 2, y + 2, 2, 2);
                g2d.fillRect(x + size - 4, y + 2, 2, 2);
                g2d.fillRect(x + 2, y + size - 4, 2, 2);
                g2d.fillRect(x + size - 4, y + size - 4, 2, 2);
            }
            case HAZARD_ZONE -> {
                // Diagonal yellow / dark hazard chevrons
                g2d.setColor(new Color(0x8A, 0x75, 0x18));
                g2d.drawLine(x, y + size, x + size, y);
                g2d.drawLine(x, y + (size / 2), x + (size / 2), y);
                g2d.drawLine(x + (size / 2), y + size, x + size, y + (size / 2));
            }
            case STREET_ASPHALT -> {
                // Subtle road texture
                g2d.setColor(type.getGridColor());
                g2d.drawLine(x, y + size - 1, x + size, y + size - 1);
            }
            default -> {}
        }
    }

    public int getColumns() { return columns; }
    public int getRows() { return rows; }
    public int getTileSize() { return tileSize; }
    public int getWorldWidth() { return columns * tileSize; }
    public int getWorldHeight() { return rows * tileSize; }
}
