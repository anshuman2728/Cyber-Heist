package com.cyberheist.save;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Complete serializable snapshot of the Cyber Heist game state.
 * Stores player coordinates, inventory, skill progression, mission progress,
 * world interactables, and AI guard states.
 */
public final class SaveData implements Serializable {

    private static final long serialVersionUID = 1L;

    // Save Metadata
    private long timestamp = System.currentTimeMillis();
    private String missionCodename = "MISSION 01 // NOIDA DATA CENTER";
    private double missionTime = 0.0;
    private int creditsExfiltrated = 0;
    private boolean alertTriggered = false;
    private int currentObjectiveIndex = 0;
    private List<String> completedObjectiveSteps = new ArrayList<>();
    private String securityLevel = "NORMAL";
    private double securityDecayTimer = 0.0;

    // Sub-System Snapshots
    private PlayerSnapshot player = new PlayerSnapshot();
    private InventorySnapshot inventory = new InventorySnapshot();
    private ProgressionSnapshot progression = new ProgressionSnapshot();
    private WorldSnapshot world = new WorldSnapshot();
    private List<GuardSnapshot> guards = new ArrayList<>();

    public SaveData() {
    }

    // --- Sub-System Snapshot Data Transfer Objects ---

    public static final class PlayerSnapshot implements Serializable {
        private static final long serialVersionUID = 1L;
        public double x = 0.0;
        public double y = 0.0;
        public double velocityX = 0.0;
        public double velocityY = 0.0;
        public String facingDirection = "SOUTH";
        public String state = "IDLE";
        public double currentStamina = 100.0;
        public double walkSpeed = 205.0;
        public double sprintSpeed = 365.0;
    }

    public static final class InventorySnapshot implements Serializable {
        private static final long serialVersionUID = 1L;
        public Map<String, Integer> itemCounts = new HashMap<>();
        public boolean dataPayloadSecured = false;
    }

    public static final class ProgressionSnapshot implements Serializable {
        private static final long serialVersionUID = 1L;
        public int totalXp = 0;
        public int currentXp = 0;
        public int currentLevel = 1;
        public int unspentUpgradePoints = 0;
        public Map<String, Integer> trackRanks = new HashMap<>();
    }

    public static final class WorldSnapshot implements Serializable {
        private static final long serialVersionUID = 1L;
        public Map<String, Boolean> doorOpen = new HashMap<>();
        public Map<String, Boolean> doorLocked = new HashMap<>();
        public Map<String, Boolean> doorHacked = new HashMap<>();
        public Map<String, Boolean> workstationHacked = new HashMap<>();
        public Map<String, Boolean> workstationAccessed = new HashMap<>();
        public Map<String, Boolean> cameraDisabled = new HashMap<>();
        public Map<String, Boolean> cameraHacked = new HashMap<>();
        public Map<String, Boolean> terminalHacked = new HashMap<>();
        public Map<String, Boolean> terminalCamerasDisabled = new HashMap<>();
        public Map<String, Boolean> alarmActive = new HashMap<>();
        public Map<String, Boolean> alarmDisarmed = new HashMap<>();
        public Map<String, Boolean> serverRackHacked = new HashMap<>();
        public Map<String, Boolean> elevatorCalled = new HashMap<>();
        public Map<String, Boolean> elevatorHacked = new HashMap<>();
        public Map<String, Boolean> safeOpened = new HashMap<>();
        public Map<String, Boolean> safeLooted = new HashMap<>();
    }

    public static final class GuardSnapshot implements Serializable {
        private static final long serialVersionUID = 1L;
        public String name = "";
        public double x = 0.0;
        public double y = 0.0;
        public double facingAngle = 0.0;
        public boolean unconscious = false;
        public boolean stunned = false;
        public double stunTimer = 0.0;
        public String aiState = "PATROL";
    }

    // Getters and Setters
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getMissionCodename() { return missionCodename; }
    public void setMissionCodename(String missionCodename) { this.missionCodename = missionCodename; }
    public double getMissionTime() { return missionTime; }
    public void setMissionTime(double missionTime) { this.missionTime = missionTime; }
    public int getCreditsExfiltrated() { return creditsExfiltrated; }
    public void setCreditsExfiltrated(int creditsExfiltrated) { this.creditsExfiltrated = creditsExfiltrated; }
    public boolean isAlertTriggered() { return alertTriggered; }
    public void setAlertTriggered(boolean alertTriggered) { this.alertTriggered = alertTriggered; }
    public int getCurrentObjectiveIndex() { return currentObjectiveIndex; }
    public void setCurrentObjectiveIndex(int currentObjectiveIndex) { this.currentObjectiveIndex = currentObjectiveIndex; }
    public List<String> getCompletedObjectiveSteps() { return completedObjectiveSteps; }

    public PlayerSnapshot getPlayer() { return player; }
    public void setPlayer(PlayerSnapshot player) { this.player = player; }
    public InventorySnapshot getInventory() { return inventory; }
    public void setInventory(InventorySnapshot inventory) { this.inventory = inventory; }
    public ProgressionSnapshot getProgression() { return progression; }
    public void setProgression(ProgressionSnapshot progression) { this.progression = progression; }
    public WorldSnapshot getWorld() { return world; }
    public void setWorld(WorldSnapshot world) { this.world = world; }
    public List<GuardSnapshot> getGuards() { return guards; }
    public void setGuards(List<GuardSnapshot> guards) { this.guards = guards; }

    public String getSecurityLevel() { return securityLevel; }
    public void setSecurityLevel(String securityLevel) { this.securityLevel = securityLevel; }
    public double getSecurityDecayTimer() { return securityDecayTimer; }
    public void setSecurityDecayTimer(double securityDecayTimer) { this.securityDecayTimer = securityDecayTimer; }
}
