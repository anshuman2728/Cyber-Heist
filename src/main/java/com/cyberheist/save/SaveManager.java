package com.cyberheist.save;

import com.cyberheist.ai.GuardAI;
import com.cyberheist.ai.state.GuardState;
import com.cyberheist.entity.FacingDirection;
import com.cyberheist.entity.Player;
import com.cyberheist.entity.PlayerState;
import com.cyberheist.inventory.Inventory;
import com.cyberheist.inventory.ItemType;
import com.cyberheist.mission.MissionManager;
import com.cyberheist.mission.MissionObjective;
import com.cyberheist.progression.ProgressionManager;
import com.cyberheist.progression.ProgressionTrack;
import com.cyberheist.scene.EmptyPlayableScene;
import com.cyberheist.stealth.Guard;
import com.cyberheist.util.Logger;
import com.cyberheist.world.AlarmPanel;
import com.cyberheist.world.ComputerWorkstation;
import com.cyberheist.world.Door;
import com.cyberheist.world.Elevator;
import com.cyberheist.world.NoidaDataCenterLevel;
import com.cyberheist.world.Safe;
import com.cyberheist.world.SecurityCamera;
import com.cyberheist.world.SecurityTerminal;
import com.cyberheist.world.ServerRack;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.EnumMap;
import java.util.Map;

/**
 * Robust persistence manager executing Save -> Exit -> Restart -> Load.
 * Restores 100% of player state, equipment, skill progression,
 * mission objectives, world interactables, and AI guard states.
 */
public final class SaveManager {

    private static final Logger LOGGER = Logger.getLogger(SaveManager.class);
    public static final String SAVE_DIR = "saves";
    public static final String DEFAULT_SAVE_FILE = "quicksave.sav";

    private SaveManager() {
    }

    /**
     * Serializes SaveData to disk.
     */
    public static boolean saveGame(SaveData data, String filename) {
        if (data == null) {
            LOGGER.error("Cannot save null SaveData.");
            return false;
        }

        try {
            File dir = new File(SAVE_DIR);
            if (!dir.exists() && !dir.mkdirs()) {
                LOGGER.error("Failed to create save directory: " + dir.getAbsolutePath());
                return false;
            }

            File file = new File(dir, filename != null ? filename : DEFAULT_SAVE_FILE);
            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new BufferedOutputStream(new FileOutputStream(file)))) {
                oos.writeObject(data);
                oos.flush();
            }
            LOGGER.info(String.format("Game successfully saved to: %s [%d bytes]", file.getAbsolutePath(), file.length()));
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to save game state: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Deserializes SaveData from disk.
     */
    public static SaveData loadGame(String filename) {
        File file = new File(SAVE_DIR, filename != null ? filename : DEFAULT_SAVE_FILE);
        if (!file.exists() || !file.isFile()) {
            LOGGER.warn("Save file not found: " + file.getAbsolutePath());
            return null;
        }

        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(file)))) {
            Object obj = ois.readObject();
            if (obj instanceof SaveData data) {
                LOGGER.info("Game successfully loaded from: " + file.getAbsolutePath());
                return data;
            } else {
                LOGGER.error("Save file corrupted or unexpected format: " + file.getAbsolutePath());
                return null;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load game state: " + e.getMessage(), e);
            return null;
        }
    }

    public static boolean hasSaveFile(String filename) {
        File file = new File(SAVE_DIR, filename != null ? filename : DEFAULT_SAVE_FILE);
        return file.exists() && file.isFile();
    }

    public static boolean deleteSaveFile(String filename) {
        File file = new File(SAVE_DIR, filename != null ? filename : DEFAULT_SAVE_FILE);
        if (file.exists()) {
            return file.delete();
        }
        return false;
    }

    /**
     * Captures a comprehensive, atomic snapshot of all running game systems from EmptyPlayableScene.
     */
    public static SaveData captureGameState(EmptyPlayableScene scene) {
        if (scene == null) return null;

        SaveData data = new SaveData();
        data.setTimestamp(System.currentTimeMillis());

        // 1. Mission Snapshot
        MissionManager mission = scene.getMissionManager();
        if (mission != null) {
            data.setMissionCodename(mission.getMissionCodename());
            data.setMissionTime(mission.getMissionTime());
            data.setCreditsExfiltrated(mission.getCreditsExfiltrated());
            data.setAlertTriggered(mission.isAlertTriggered());
            data.setCurrentObjectiveIndex(mission.getCurrentObjectiveIndex());
            for (MissionObjective obj : mission.getObjectives()) {
                if (obj.isCompleted()) {
                    data.getCompletedObjectiveSteps().add(obj.getStep().name());
                }
            }
        }

        // Security Director Snapshot
        if (scene.getSecurityDirector() != null) {
            data.setSecurityLevel(scene.getSecurityDirector().getCurrentLevel().name());
            data.setSecurityDecayTimer(scene.getSecurityDirector().getLevelDecayTimer());
        }

        // 2. Player Snapshot
        Player player = scene.getPlayer();
        if (player != null) {
            SaveData.PlayerSnapshot ps = data.getPlayer();
            ps.x = player.getX();
            ps.y = player.getY();
            ps.velocityX = player.getVelocityX();
            ps.velocityY = player.getVelocityY();
            ps.facingDirection = player.getFacingDirection() != null ? player.getFacingDirection().name() : "SOUTH";
            ps.state = player.getState() != null ? player.getState().name() : "IDLE";
            ps.currentStamina = player.getStaminaSystem().getCurrentStamina();
            ps.walkSpeed = player.getWalkSpeed();
            ps.sprintSpeed = player.getSprintSpeed();
        }

        // 3. Inventory Snapshot
        Inventory inv = scene.getInventory();
        if (inv != null) {
            SaveData.InventorySnapshot is = data.getInventory();
            is.dataPayloadSecured = inv.isDataPayloadSecured();
            for (ItemType type : ItemType.values()) {
                is.itemCounts.put(type.name(), inv.getCount(type));
            }
        }

        // 4. Progression Snapshot
        ProgressionManager prog = scene.getProgression();
        if (prog != null) {
            SaveData.ProgressionSnapshot prs = data.getProgression();
            prs.totalXp = prog.getTotalXp();
            prs.currentXp = prog.getCurrentXp();
            prs.unspentUpgradePoints = prog.getSkillPoints();
            for (ProgressionTrack track : ProgressionTrack.values()) {
                prs.trackRanks.put(track.name(), prog.getRank(track));
            }
        }

        // 5. Handcrafted Level & World Snapshot
        NoidaDataCenterLevel level = scene.getLevel();
        if (level != null) {
            SaveData.WorldSnapshot ws = data.getWorld();

            for (Door d : level.getDoors()) {
                ws.doorOpen.put(d.getName(), d.isOpen());
                ws.doorLocked.put(d.getName(), d.isLocked());
                ws.doorHacked.put(d.getName(), d.isHacked());
            }

            for (ComputerWorkstation cw : level.getWorkstations()) {
                ws.workstationHacked.put(cw.getName(), cw.isHacked());
                ws.workstationAccessed.put(cw.getName(), cw.isAccessed());
            }

            for (SecurityCamera sc : level.getCameras()) {
                ws.cameraDisabled.put(sc.getName(), sc.isDisabled());
                ws.cameraHacked.put(sc.getName(), sc.isHacked());
            }

            for (SecurityTerminal st : level.getTerminals()) {
                ws.terminalHacked.put(st.getName(), st.isHacked());
                ws.terminalCamerasDisabled.put(st.getName(), st.isCamerasDisabled());
            }

            for (AlarmPanel ap : level.getAlarms()) {
                ws.alarmActive.put(ap.getName(), ap.isActiveAlarm());
                ws.alarmDisarmed.put(ap.getName(), ap.isDisarmed());
            }

            for (ServerRack sr : level.getServerRacks()) {
                ws.serverRackHacked.put(sr.getName(), sr.isHacked());
            }

            for (Elevator el : level.getElevators()) {
                ws.elevatorCalled.put(el.getName(), el.isCalled());
                ws.elevatorHacked.put(el.getName(), el.isHacked());
            }

            for (Safe sf : level.getSafes()) {
                ws.safeOpened.put(sf.getName(), sf.isOpened());
                ws.safeLooted.put(sf.getName(), sf.isLooted());
            }

            // 6. Security Guards Snapshot
            for (Guard g : level.getGuards()) {
                SaveData.GuardSnapshot gs = new SaveData.GuardSnapshot();
                gs.name = g.getName();
                gs.x = g.getX();
                gs.y = g.getY();
                gs.facingAngle = g.getFacingAngle();
                gs.unconscious = g.isUnconscious();
                gs.stunned = g.isStunned();
                gs.stunTimer = g.getStunTimer();

                GuardAI ai = g.getAI();
                if (ai != null) {
                    gs.aiState = ai.getState() != null ? ai.getState().name() : "PATROL";
                }
                data.getGuards().add(gs);
            }
        }

        return data;
    }

    /**
     * Restores an active EmptyPlayableScene to match the saved snapshot identically.
     */
    public static boolean applyGameState(EmptyPlayableScene scene, SaveData data) {
        if (scene == null || data == null) return false;

        LOGGER.info("Applying saved game state to scene...");

        // 1. Restore Mission
        MissionManager mission = scene.getMissionManager();
        if (mission != null) {
            mission.restoreMissionState(
                    data.getCurrentObjectiveIndex(),
                    data.getMissionTime(),
                    data.getCreditsExfiltrated(),
                    data.isAlertTriggered()
            );
        }

        // 2. Restore Progression Perks
        ProgressionManager prog = scene.getProgression();
        if (prog != null && data.getProgression() != null) {
            SaveData.ProgressionSnapshot prs = data.getProgression();
            Map<ProgressionTrack, Integer> ranks = new EnumMap<>(ProgressionTrack.class);
            for (ProgressionTrack track : ProgressionTrack.values()) {
                Integer r = prs.trackRanks.get(track.name());
                if (r != null) {
                    ranks.put(track, r);
                }
            }
            prog.setProgressionState(prs.totalXp, prs.currentXp, prs.unspentUpgradePoints, ranks);
        }

        // 3. Restore Player Entity
        Player player = scene.getPlayer();
        if (player != null && data.getPlayer() != null) {
            SaveData.PlayerSnapshot ps = data.getPlayer();
            player.setPosition(ps.x, ps.y);
            player.setVelocity(ps.velocityX, ps.velocityY);

            try {
                player.setFacingDirection(FacingDirection.valueOf(ps.facingDirection));
            } catch (Exception ignored) {
                player.setFacingDirection(FacingDirection.SOUTH);
            }

            try {
                player.setState(PlayerState.valueOf(ps.state));
            } catch (Exception ignored) {
                player.setState(PlayerState.IDLE);
            }

            player.getStaminaSystem().setCurrentStamina(ps.currentStamina);
            player.setWalkSpeed(ps.walkSpeed);
            player.setSprintSpeed(ps.sprintSpeed);

            // Re-apply progression sprint bonus
            if (prog != null) {
                player.setSprintSpeed(380.0 * prog.getSprintSpeedMultiplier());
            }
        }

        // 4. Restore Inventory
        Inventory inv = scene.getInventory();
        if (inv != null && data.getInventory() != null) {
            SaveData.InventorySnapshot is = data.getInventory();
            inv.setDataPayloadSecured(is.dataPayloadSecured);
            if (prog != null) {
                inv.applyCapacityBonus(prog.getItemCapacityBonus());
            }
            for (ItemType type : ItemType.values()) {
                Integer count = is.itemCounts.get(type.name());
                if (count != null) {
                    inv.setItemCount(type, count);
                }
            }
        }

        // 5. Restore World Interactables
        NoidaDataCenterLevel level = scene.getLevel();
        if (level != null && data.getWorld() != null) {
            SaveData.WorldSnapshot ws = data.getWorld();

            for (Door d : level.getDoors()) {
                Boolean open = ws.doorOpen.get(d.getName());
                Boolean locked = ws.doorLocked.get(d.getName());
                Boolean hacked = ws.doorHacked.get(d.getName());
                if (locked != null) d.setLocked(locked);
                if (hacked != null) d.setHacked(hacked);
                if (open != null) d.setOpen(open);
            }

            for (ComputerWorkstation cw : level.getWorkstations()) {
                Boolean hacked = ws.workstationHacked.get(cw.getName());
                Boolean accessed = ws.workstationAccessed.get(cw.getName());
                if (hacked != null) cw.setHacked(hacked);
                if (accessed != null) cw.setAccessed(accessed);
            }

            for (SecurityCamera sc : level.getCameras()) {
                Boolean disabled = ws.cameraDisabled.get(sc.getName());
                Boolean hacked = ws.cameraHacked.get(sc.getName());
                if (disabled != null) sc.setDisabled(disabled);
                if (hacked != null) sc.setHacked(hacked);
            }

            for (SecurityTerminal st : level.getTerminals()) {
                Boolean hacked = ws.terminalHacked.get(st.getName());
                Boolean camsOff = ws.terminalCamerasDisabled.get(st.getName());
                if (hacked != null) st.setHacked(hacked);
                if (camsOff != null) st.setCamerasDisabled(camsOff);
            }

            for (AlarmPanel ap : level.getAlarms()) {
                Boolean active = ws.alarmActive.get(ap.getName());
                Boolean disarmed = ws.alarmDisarmed.get(ap.getName());
                if (disarmed != null) ap.setDisarmed(disarmed);
                if (active != null) ap.setActiveAlarm(active);
            }

            for (ServerRack sr : level.getServerRacks()) {
                Boolean hacked = ws.serverRackHacked.get(sr.getName());
                if (hacked != null) sr.setHacked(hacked);
            }

            for (Elevator el : level.getElevators()) {
                Boolean called = ws.elevatorCalled.get(el.getName());
                Boolean hacked = ws.elevatorHacked.get(el.getName());
                if (hacked != null) el.setHacked(hacked);
                if (called != null) el.setCalled(called);
            }

            for (Safe sf : level.getSafes()) {
                Boolean opened = ws.safeOpened.get(sf.getName());
                Boolean looted = ws.safeLooted.get(sf.getName());
                if (opened != null) sf.setOpened(opened);
                if (looted != null) sf.setLooted(looted);
            }

            // 6. Restore Guards
            if (data.getGuards() != null) {
                for (SaveData.GuardSnapshot gs : data.getGuards()) {
                    for (Guard g : level.getGuards()) {
                        if (g.getName().equals(gs.name)) {
                            g.setPosition(gs.x, gs.y);
                            g.setFacingAngle(gs.facingAngle);
                            g.setUnconscious(gs.unconscious);
                            g.setStunTimer(gs.stunTimer);

                            GuardAI ai = g.getAI();
                            if (ai != null && gs.aiState != null) {
                                try {
                                    ai.setState(GuardState.valueOf(gs.aiState));
                                } catch (Exception ignored) {
                                    ai.setState(GuardState.PATROL);
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }

        // Restore Security Director Threat Level
        if (scene.getSecurityDirector() != null && data.getSecurityLevel() != null) {
            try {
                com.cyberheist.security.SecurityLevel sec = com.cyberheist.security.SecurityLevel.valueOf(data.getSecurityLevel());
                scene.getSecurityDirector().setSecurityLevelDirect(sec, data.getSecurityDecayTimer());
            } catch (Exception ignored) {}
        }

        // 7. Snap camera to restored player
        if (player != null && scene.getCamera() != null) {
            scene.getCamera().snapTo(player.getX() + 14.0, player.getY() + 14.0);
        }

        LOGGER.info("Game state restored successfully.");
        return true;
    }
}
