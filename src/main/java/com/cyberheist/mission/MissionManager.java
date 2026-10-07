package com.cyberheist.mission;

import com.cyberheist.entity.Player;
import com.cyberheist.stealth.DetectionState;
import com.cyberheist.stealth.StealthSystem;
import com.cyberheist.util.Logger;
import com.cyberheist.world.NoidaDataCenterLevel;
import com.cyberheist.world.SecurityCamera;
import com.cyberheist.world.SecurityTerminal;
import com.cyberheist.world.ServerRack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages the objectives, story state, and reactive triggers for Mission 1: "Ghost in the Server".
 * Decoupled from rendering and completely headless testable.
 */
public final class MissionManager {

    private static final Logger LOGGER = Logger.getLogger(MissionManager.class);

    public enum MissionStatus {
        IN_PROGRESS,
        SUCCESS,
        FAILED
    }

    private final String missionTitle = "Ghost in the Server";
    private final String missionCodename = "MISSION 01 // NOIDA DATA CENTER";
    private MissionStatus status = MissionStatus.IN_PROGRESS;

    private final List<MissionObjective> objectives = new ArrayList<>();
    private int currentObjectiveIndex = 0;

    // Mission Statistics
    private double missionTime = 0.0;
    private int creditsExfiltrated = 0;
    private boolean alertTriggered = false;
    private boolean introPlayed = false;
    private boolean detectedAlertVoicePlayed = false;
    private boolean alarmVoicePlayed = false;

    public MissionManager() {
        // Initialize the 7 sequential objectives
        for (MissionStep step : MissionStep.values()) {
            objectives.add(new MissionObjective(step));
        }

        if (!objectives.isEmpty()) {
            objectives.get(0).setStatus(MissionObjective.Status.ACTIVE);
        }
    }

    /**
     * Queues the atmospheric opening dialogue upon spawning at the street.
     */
    public void startMission(RadioDialogueManager dialogue) {
        if (introPlayed) return;
        introPlayed = true;

        if (dialogue != null) {
            dialogue.queueLine(DialogueLine.mira("You're late.", 2.4));
            dialogue.queueLine(DialogueLine.kavin("Traffic on the expressway. Relax.", 2.8));
            dialogue.queueLine(DialogueLine.mira("You weren't followed?", 2.4));
            dialogue.queueLine(DialogueLine.kavin("Clean. Front entrance in sight.", 2.6));
            dialogue.queueLine(DialogueLine.mira("Cameras are live. Slip inside before the shift change.", 3.4));
        }
        LOGGER.info("Mission 1 'Ghost in the Server' initiated.");
    }

    /**
     * Evaluates gameplay state and updates mission objective transitions.
     */
    public void update(double deltaTime, Player player, NoidaDataCenterLevel level,
                       StealthSystem stealthSystem, RadioDialogueManager dialogue) {
        if (status != MissionStatus.IN_PROGRESS) {
            return;
        }

        missionTime += deltaTime;

        // Dynamic voice reactions to stealth state
        if (stealthSystem != null && dialogue != null) {
            if (!detectedAlertVoicePlayed && stealthSystem.getGlobalState() == DetectionState.DETECTED) {
                detectedAlertVoicePlayed = true;
                alertTriggered = true;
                dialogue.queueLine(DialogueLine.alert("They made you! Don't engage—break line of sight!"));
            }

            if (!alarmVoicePlayed && level != null) {
                boolean anyAlarmActive = level.getAlarms().stream().anyMatch(a -> a.isActiveAlarm());
                if (anyAlarmActive) {
                    alarmVoicePlayed = true;
                    alertTriggered = true;
                    dialogue.queueLine(DialogueLine.alert("Facility alarm is screaming! Security is converging!"));
                }
            }
        }

        // Objective Progression Checks
        boolean advanced;
        do {
            advanced = checkObjectiveProgression(player, level, dialogue);
        } while (advanced && status == MissionStatus.IN_PROGRESS);
    }

    private boolean checkObjectiveProgression(Player player, NoidaDataCenterLevel level, RadioDialogueManager dialogue) {
        if (player == null || level == null) return false;

        double px = player.getX();
        double py = player.getY();

        MissionObjective current = getCurrentObjective();
        if (current == null) return false;

        switch (current.getStep()) {
            case ENTER_FACILITY -> {
                // Player enters reception lobby through the front doors
                if (py > 210.0 && py < 470.0 && px < 840.0) {
                    completeCurrentObjective(dialogue, List.of(
                            DialogueLine.kavin("Inside the lobby. Quiet so far.", 2.6),
                            DialogueLine.mira("Guard desk at the turnstiles ahead. Don't walk into their cone.", 3.2)
                    ));
                    activateNextObjective();
                    return true;
                }
            }
            case AVOID_SECURITY -> {
                // Player reaches security checkpoint without triggering alarms or approaches booth
                if (px >= 840.0 && py < 470.0) {
                    completeCurrentObjective(dialogue, List.of(
                            DialogueLine.mira("Security booth is on your right. Router terminal's inside.", 3.2),
                            DialogueLine.kavin("On it.", 1.8)
                    ));
                    activateNextObjective();
                    return true;
                }
            }
            case ACCESS_TERMINAL -> {
                // Player breaches or accesses the Security Terminal
                boolean terminalBreached = level.getTerminals().stream().anyMatch(SecurityTerminal::isHacked);
                if (terminalBreached) {
                    completeCurrentObjective(dialogue, List.of(
                            DialogueLine.kavin("Terminal breached. Patching into video switcher...", 2.8)
                    ));
                    activateNextObjective();
                    return true;
                }
            }
            case DISABLE_CAMERAS -> {
                // All cameras looped / disabled
                boolean allCamsDisabled = level.getCameras().stream().allMatch(SecurityCamera::isDisabled);
                if (allCamsDisabled) {
                    completeCurrentObjective(dialogue, List.of(
                            DialogueLine.kavin("Cameras are looped. Security grid is blind.", 2.8),
                            DialogueLine.mira("Good work. Hallway to the server room is open.", 3.0)
                    ));
                    activateNextObjective();
                    return true;
                }
            }
            case REACH_SERVER_ROOM -> {
                // Player enters server room zone
                if (px >= 960.0 && py >= 470.0 && py < 750.0) {
                    completeCurrentObjective(dialogue, List.of(
                            DialogueLine.kavin("Made it to the server room. Temperature's freezing.", 3.0),
                            DialogueLine.mira("Target is Database Cluster A. Extract the partition keys.", 3.2)
                    ));
                    activateNextObjective();
                    return true;
                }
            }
            case STEAL_DATA -> {
                // Server rack data exfiltrated
                boolean dataStolen = level.getServerRacks().stream().anyMatch(ServerRack::isHacked);
                if (dataStolen) {
                    this.creditsExfiltrated += 250000;
                    completeCurrentObjective(dialogue, List.of(
                            DialogueLine.kavin("Payload secured. Quarter-mil in corporate records.", 3.0),
                            DialogueLine.mira("We got what we came for. Van is at the rear emergency fire exit. Move!", 3.6)
                    ));
                    activateNextObjective();
                    return true;
                }
            }
            case ESCAPE -> {
                // Player reaches exit zone or elevator
                boolean atExit = player.getBoundingBox().intersects(level.getExitZone()) ||
                        level.getElevators().stream().anyMatch(e -> e.isCalled() && player.getBoundingBox().intersects(e.getInteractionTriggerZone()));
                if (atExit) {
                    completeCurrentObjective(dialogue, List.of(
                            DialogueLine.kavin("At the extraction point.", 2.2),
                            DialogueLine.mira("Van door's open. Get in.", 2.2)
                    ));
                    this.status = MissionStatus.SUCCESS;
                    LOGGER.info(String.format("Mission 1 SUCCESS! Time: %.1fs, Credits: $%d, Alert Triggered: %b",
                            missionTime, creditsExfiltrated, alertTriggered));
                    return true;
                }
            }
        }
        return false;
    }

    private void completeCurrentObjective(RadioDialogueManager dialogue, List<DialogueLine> lines) {
        MissionObjective current = getCurrentObjective();
        if (current != null) {
            current.setStatus(MissionObjective.Status.COMPLETED);
            current.setCompletedAtTime(missionTime);
            LOGGER.info("Objective COMPLETED: " + current.getTitle());
        }

        if (dialogue != null && lines != null) {
            dialogue.queueLines(lines);
        }
    }

    private void activateNextObjective() {
        currentObjectiveIndex++;
        if (currentObjectiveIndex < objectives.size()) {
            objectives.get(currentObjectiveIndex).setStatus(MissionObjective.Status.ACTIVE);
            LOGGER.info("Objective ACTIVATED: " + objectives.get(currentObjectiveIndex).getTitle());
        }
    }

    public MissionObjective getCurrentObjective() {
        if (currentObjectiveIndex >= 0 && currentObjectiveIndex < objectives.size()) {
            return objectives.get(currentObjectiveIndex);
        }
        return null;
    }

    public List<MissionObjective> getObjectives() {
        return Collections.unmodifiableList(objectives);
    }

    public MissionStatus getStatus() {
        return status;
    }

    public double getMissionTime() {
        return missionTime;
    }

    public int getCreditsExfiltrated() {
        return creditsExfiltrated;
    }

    public boolean isAlertTriggered() {
        return alertTriggered;
    }

    public String getMissionTitle() {
        return missionTitle;
    }

    public String getMissionCodename() {
        return missionCodename;
    }

    public int getCompletedCount() {
        int count = 0;
        for (MissionObjective obj : objectives) {
            if (obj.isCompleted()) count++;
        }
        return count;
    }

    public int getTotalCount() {
        return objectives.size();
    }

    public String getStealthRating() {
        if (!alertTriggered) {
            return "GHOST // UNDETECTED";
        }
        return "SHADOW // DETECTED ONCE";
    }

    public int getCurrentObjectiveIndex() {
        return currentObjectiveIndex;
    }

    public void restoreMissionState(int objectiveIndex, double missionTime, int creditsExfiltrated, boolean alertTriggered) {
        this.currentObjectiveIndex = Math.max(0, Math.min(objectiveIndex, objectives.size()));
        this.missionTime = Math.max(0.0, missionTime);
        this.creditsExfiltrated = Math.max(0, creditsExfiltrated);
        this.alertTriggered = alertTriggered;
        this.introPlayed = true;

        for (int i = 0; i < objectives.size(); i++) {
            MissionObjective obj = objectives.get(i);
            if (i < this.currentObjectiveIndex) {
                obj.setStatus(MissionObjective.Status.COMPLETED);
            } else if (i == this.currentObjectiveIndex) {
                obj.setStatus(MissionObjective.Status.ACTIVE);
            } else {
                obj.setStatus(MissionObjective.Status.PENDING);
            }
        }
    }
}
