package com.cyberheist.mission;

/**
 * Sequential objective stages defining Mission 1: "Ghost in the Server".
 */
public enum MissionStep {
    ENTER_FACILITY(
            "Enter facility",
            "Infiltrate through the exterior glass sliders into the corporate reception."
    ),
    AVOID_SECURITY(
            "Avoid security",
            "Slip past reception patrols and checkpoints without triggering facility alarms."
    ),
    ACCESS_TERMINAL(
            "Access terminal",
            "Locate and tap into the security command terminal in the security wing."
    ),
    DISABLE_CAMERAS(
            "Disable camera network",
            "Override the terminal logic to loop facility CCTV surveillance feeds."
    ),
    REACH_SERVER_ROOM(
            "Reach server room",
            "Penetrate deep into the facility and access the Tier-IV server farm."
    ),
    STEAL_DATA(
            "Steal encrypted data",
            "Breach the primary server cluster and exfiltrate the encrypted corporate payload."
    ),
    ESCAPE(
            "Escape",
            "Evacuate the facility through the rear emergency fire corridor to extraction."
    );

    private final String label;
    private final String description;

    MissionStep(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}
