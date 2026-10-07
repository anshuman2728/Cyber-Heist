package com.cyberheist.combat;

/**
 * Tactical non-lethal weapons and combat tools.
 */
public enum WeaponType {
    STUN_DEVICE("Stun Device", "[Q]", "Short-range high-voltage directional arc taser."),
    EMP("EMP Canister", "[1]", "Electromagnetic pulse device overloading cameras and guard optics."),
    DISTRACTION("Throwable Distraction", "[G]", "Acoustic decoy emitter thrown to lure guards with sound."),
    TAKEDOWN("Non-Lethal Takedown", "[F]", "Silent chokehold takedown when approaching behind unaware guards.");

    private final String displayName;
    private final String hotkeyPrompt;
    private final String description;

    WeaponType(String displayName, String hotkeyPrompt, String description) {
        this.displayName = displayName;
        this.hotkeyPrompt = hotkeyPrompt;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getHotkeyPrompt() { return hotkeyPrompt; }
    public String getDescription() { return description; }
}
