package com.cyberheist.progression;

/**
 * Progression skill tracks in Cyber Heist.
 */
public enum ProgressionTrack {
    HACKING("Hacking Mastery", "Cyber intrusion efficiency, node buffer bypass, and counter-trace suppression."),
    STEALTH("Stealth Camouflage", "Acoustic footstep dampening, shadow cover blending, and rapid detection decay."),
    MOVEMENT("Kinetic Conditioning", "Sprint velocity, maximum stamina capacity, and rapid aerobic recovery."),
    EQUIPMENT("Field Equipment", "Tactical gear capacity, overclocked shock coils, and throwable decoy range.");

    private final String displayName;
    private final String description;

    ProgressionTrack(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }

    public String getPerkName(int rank) {
        return switch (this) {
            case HACKING -> switch (rank) {
                case 1 -> "Buffer Bypass (+2.5s hack time, -20% trace build rate)";
                case 2 -> "ICE Breaker (First sequence node auto-solved, -50% error penalty)";
                default -> "Base Cyberdeck";
            };
            case STEALTH -> switch (rank) {
                case 1 -> "Ghost Protocol (-35% footstep noise radius)";
                case 2 -> "Shadow Veil (-30% detection build rate, +50% decay speed)";
                default -> "Standard Infiltration";
            };
            case MOVEMENT -> switch (rank) {
                case 1 -> "Conditioning (+15% sprint speed, +25 max stamina)";
                case 2 -> "Adrenaline Surge (+50% stamina regen, -35% sprint drain)";
                default -> "Standard Mobility";
            };
            case EQUIPMENT -> switch (rank) {
                case 1 -> "Deep Pockets (+2 item capacity, +40% throw range)";
                case 2 -> "Overclocked Coils (Stun cooldown halved, +60px EMP blast radius)";
                default -> "Standard Loadout";
            };
        };
    }
}
