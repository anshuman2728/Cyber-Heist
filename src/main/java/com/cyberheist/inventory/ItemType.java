package com.cyberheist.inventory;

/**
 * Tactical inventory item types in Cyber Heist.
 */
public enum ItemType {
    EMP("EMP Device", "Electromagnetic pulse device. Overloads cameras & stuns guards in 220px blast.", "[EMP]", 2),
    LOCKPICK("Lockpick", "Electronic bypass probe. Silently unlocks mechanical doors and safes.", "[LPK]", 3),
    MEDKIT("Trauma Medkit", "Emergency stimulant. Instantly restores stamina to 100% and cures fatigue.", "[MED]", 1),
    DATA_DRIVE("Data Drive", "Military-grade encrypted flash drive. Holds stolen data payload.", "[DAT]", 1);

    private final String displayName;
    private final String description;
    private final String iconGlyph;
    private final int defaultMax;

    ItemType(String displayName, String description, String iconGlyph, int defaultMax) {
        this.displayName = displayName;
        this.description = description;
        this.iconGlyph = iconGlyph;
        this.defaultMax = defaultMax;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public String getIconGlyph() { return iconGlyph; }
    public int getDefaultMax() { return defaultMax; }
}
