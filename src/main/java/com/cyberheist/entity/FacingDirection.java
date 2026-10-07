package com.cyberheist.entity;

/**
 * Represents the 8-way cardinal and ordinal orientation of an entity.
 */
public enum FacingDirection {
    NORTH(0, -1, 270),
    NORTH_EAST(1, -1, 315),
    EAST(1, 0, 0),
    SOUTH_EAST(1, 1, 45),
    SOUTH(0, 1, 90),
    SOUTH_WEST(-1, 1, 135),
    WEST(-1, 0, 180),
    NORTH_WEST(-1, -1, 225);

    private final double dirX;
    private final double dirY;
    private final double angleDegrees;

    FacingDirection(double dirX, double dirY, double angleDegrees) {
        this.dirX = dirX;
        this.dirY = dirY;
        this.angleDegrees = angleDegrees;
    }

    /**
     * Resolves the closest facing direction from normalized movement or aim vectors.
     */
    public static FacingDirection fromVector(double x, double y) {
        if (Math.abs(x) < 0.001 && Math.abs(y) < 0.001) {
            return SOUTH; // Default facing
        }

        double angle = Math.toDegrees(Math.atan2(y, x));
        if (angle < 0) {
            angle += 360.0;
        }

        // 8-directional quadrant mapping (each sector is 45 degrees wide, centered on direction)
        if (angle >= 337.5 || angle < 22.5) return EAST;
        if (angle >= 22.5 && angle < 67.5) return SOUTH_EAST;
        if (angle >= 67.5 && angle < 112.5) return SOUTH;
        if (angle >= 112.5 && angle < 157.5) return SOUTH_WEST;
        if (angle >= 157.5 && angle < 202.5) return WEST;
        if (angle >= 202.5 && angle < 247.5) return NORTH_WEST;
        if (angle >= 247.5 && angle < 292.5) return NORTH;
        return NORTH_EAST;
    }

    public double getDirX() { return dirX; }
    public double getDirY() { return dirY; }
    public double getAngleDegrees() { return angleDegrees; }
}
