package com.cyberheist.scene;

import com.cyberheist.core.GameConfig;
import com.cyberheist.core.GameEngine;
import com.cyberheist.display.Camera2D;
import com.cyberheist.display.LightingRenderer;
import com.cyberheist.display.LoadingTransition;
import com.cyberheist.display.PauseOverlay;
import com.cyberheist.display.SettingsOverlay;
import com.cyberheist.particle.ParticleSystem;
import com.cyberheist.entity.Player;
import com.cyberheist.entity.PlayerState;
import com.cyberheist.input.KeyboardInput;
import com.cyberheist.input.MouseInput;
import com.cyberheist.interact.Interactable;
import com.cyberheist.stealth.DetectionState;
import com.cyberheist.stealth.Guard;
import com.cyberheist.stealth.NoiseType;
import com.cyberheist.stealth.StealthSystem;
import com.cyberheist.util.Logger;
import com.cyberheist.hacking.Hackable;
import com.cyberheist.hacking.HackingOverlayRenderer;
import com.cyberheist.hacking.HackingSession;
import com.cyberheist.hacking.NetworkNode;
import com.cyberheist.mission.MissionManager;
import com.cyberheist.mission.MissionHUDOverlay;
import com.cyberheist.mission.RadioDialogueManager;
import com.cyberheist.world.ComputerWorkstation;
import com.cyberheist.world.Door;
import com.cyberheist.world.NoidaDataCenterLevel;
import com.cyberheist.world.Safe;
import com.cyberheist.world.ServerRack;
import com.cyberheist.combat.CombatManager;
import com.cyberheist.combat.TacticalHUDOverlay;
import com.cyberheist.inventory.Inventory;
import com.cyberheist.inventory.ItemType;
import com.cyberheist.progression.ProgressionHUDOverlay;
import com.cyberheist.progression.ProgressionManager;
import com.cyberheist.progression.ProgressionTrack;

import com.cyberheist.save.SaveData;
import com.cyberheist.save.SaveManager;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Active Playable Scene hosting Level 1: "Noida Corporate Data Center".
 * Features realistic non-binary stealth calculations, acoustic noise propagation,
 * dynamic guard vision cones, line-of-sight raycasting, and toggleable debug tools.
 */
public final class EmptyPlayableScene implements Scene {

    private static final Logger LOGGER = Logger.getLogger(EmptyPlayableScene.class);

    private final GameEngine engine;
    private final KeyboardInput keyboard;
    private final MouseInput mouse;

    // Level, Viewport & Entities
    private NoidaDataCenterLevel level;
    private Camera2D camera;
    private Player player;
    private StealthSystem stealthSystem;
    private final com.cyberheist.security.SecurityDirector securityDirector = new com.cyberheist.security.SecurityDirector();

    // Hacking Minigame
    private HackingSession activeHackingSession = null;
    private final HackingOverlayRenderer hackingRenderer = new HackingOverlayRenderer();

    // Mission 1: Ghost in the Server & Radio Comms
    private final MissionManager missionManager = new MissionManager();
    private final RadioDialogueManager radioDialogue = new RadioDialogueManager();
    private final MissionHUDOverlay missionHUD = new MissionHUDOverlay();

    // Inventory, Progression & Combat Systems
    private final Inventory inventory = Inventory.createStartingLoadout();
    private final ProgressionManager progression = new ProgressionManager();
    private final CombatManager combat = new CombatManager();
    private final TacticalHUDOverlay tacticalHUD = new TacticalHUDOverlay();
    private final ProgressionHUDOverlay progressionHUD = new ProgressionHUDOverlay();
    private boolean progressionMatrixOpen = false;

    // Environmental click ping
    private final List<ClickPing> clickPings = new ArrayList<>();

    // Audio, Particles, Lighting & UI Overlays (Game Feel & Polish)
    private final ParticleSystem particleSystem = new ParticleSystem();
    private final LightingRenderer lightingRenderer = new LightingRenderer();
    private final PauseOverlay pauseOverlay = new PauseOverlay();
    private final SettingsOverlay settingsOverlay = new SettingsOverlay();
    private final LoadingTransition loadingTransition = new LoadingTransition();
    private boolean isPaused = false;
    private boolean isSettingsOpen = false;

    // Save & Load Status Toast Banner
    private String saveToastMessage = null;
    private double saveToastTimer = 0.0;

    public EmptyPlayableScene(GameEngine engine) {
        this.engine = engine;
        this.keyboard = engine.getKeyboardInput();
        this.mouse = engine.getMouseInput();
    }

    @Override
    public void initialize() {
        LOGGER.info("Initializing Level 1 with Reactive Security Architecture.");

        // 1. Build handcrafted level
        this.level = new NoidaDataCenterLevel();
        this.level.setSecurityDirector(securityDirector);

        // 2. Setup 2D viewport camera
        this.camera = new Camera2D(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT,
                level.getWorldWidth(), level.getWorldHeight());

        // 3. Spawn player at designated exterior sidewalk spawn point
        Point spawn = level.getSpawnPoint();
        this.player = new Player(spawn.x, spawn.y);

        // Connect procedural particle dust on footstep
        this.player.setFootstepDustListener((fx, fy, dirX, dirY, running) ->
                particleSystem.spawnFootstepDust(fx, fy, dirX, dirY, running)
        );

        // 4. Setup Stealth System and register guards
        this.stealthSystem = new StealthSystem();
        for (Guard guard : level.getGuards()) {
            this.stealthSystem.addGuard(guard);
        }
        this.stealthSystem.setSecurityContext(securityDirector, level.getCameras(), level.getDoors(), level.getAlarms());

        // Connect Security Director alerts to tactical radio comms & audio feedback
        this.securityDirector.addListener((oldLvl, newLvl, cause) -> {
            switch (newLvl) {
                case SUSPICIOUS -> {
                    radioDialogue.queueTransmission("DISPATCH", "Perimeter disturbance noted. Stay alert.", 3.0);
                    com.cyberheist.audio.AudioManager.getInstance().play(com.cyberheist.audio.SoundEffect.RADIO_CHIRP);
                }
                case ALERT -> {
                    radioDialogue.queueTransmission("DISPATCH", "Facility Alert: Intrusion confirmed! Converge!", 3.5);
                    com.cyberheist.audio.AudioManager.getInstance().play(com.cyberheist.audio.SoundEffect.ALARM_ALERT);
                }
                case LOCKDOWN -> {
                    radioDialogue.queueTransmission("OPERATOR", "LOCKDOWN INITIATED! Main blast doors sealing!", 4.0);
                    com.cyberheist.audio.AudioManager.getInstance().play(com.cyberheist.audio.SoundEffect.ALARM_ALERT);
                }
                case NORMAL -> {
                    radioDialogue.queueTransmission("OPERATOR", "Facility threat clear. Resuming normal status.", 3.0);
                }
            }
        });

        // 5. Connect noise emission callbacks & audio triggers
        this.player.setNoiseEmitter(type ->
                stealthSystem.emitNoise(player.getX() + 14.0, player.getY() + 14.0, type)
        );

        for (Door door : level.getDoors()) {
            door.setNoiseCallback(() -> {
                com.cyberheist.audio.AudioManager.getInstance().play(com.cyberheist.audio.SoundEffect.DOOR_SLIDE);
                stealthSystem.emitNoise(door.getBoundingBox().getMinX() + (door.getBoundingBox().getWidth() / 2.0),
                        door.getBoundingBox().getMinY() + (door.getBoundingBox().getHeight() / 2.0),
                        NoiseType.DOOR_OPEN);
            });
        }

        for (Interactable interactable : level.getInteractables()) {
            if (interactable instanceof ComputerWorkstation ws) {
                ws.setNoiseCallback(() ->
                        stealthSystem.emitNoise(ws.getBoundingBox().getMinX() + (ws.getBoundingBox().getWidth() / 2.0),
                                ws.getBoundingBox().getMinY() + (ws.getBoundingBox().getHeight() / 2.0),
                                NoiseType.HACKING)
                );
            }
        }

        // 6. Connect Hacking Interaction Listener with Progression Perks
        this.player.setInteractionListener(interactable -> {
            if (interactable instanceof Hackable hackable && hackable.canBeHacked()) {
                activeHackingSession = new HackingSession(hackable);
                activeHackingSession.applyProgression(progression);
                stealthSystem.emitNoise(player.getX() + 14.0, player.getY() + 14.0, NoiseType.HACKING);
                com.cyberheist.audio.AudioManager.getInstance().play(com.cyberheist.audio.SoundEffect.HACK_CLICK);
                LOGGER.info("Initiated Network Node hacking session for: " + hackable.getTargetName());
                return true; // Intercepted for hacking minigame
            }
            return false;
        });

        // 7. Snap camera to initial player position
        this.camera.snapTo(player.getX() + 14.0, player.getY() + 14.0);

        // 8. Start Mission 1: "Ghost in the Server" & trigger opening radio transmission
        this.missionManager.startMission(radioDialogue);
        com.cyberheist.audio.AudioManager.getInstance().play(com.cyberheist.audio.SoundEffect.RADIO_CHIRP);

        // 9. Start ambient HVAC server drone
        com.cyberheist.audio.AudioManager.getInstance().startAmbient();

        this.clickPings.clear();
        this.particleSystem.clear();
        this.loadingTransition.reset();
        this.isPaused = false;
        this.isSettingsOpen = false;
        LOGGER.info("Stealth system and observers initialized.");
    }

    @Override
    public void update(double deltaTime) {
        // --- 0. Loading Mission Boot Transition ---
        if (!loadingTransition.isCompleted()) {
            loadingTransition.update(deltaTime, keyboard);
            return;
        }

        // --- 1. Settings Overlay Navigation ---
        if (isSettingsOpen) {
            settingsOverlay.update(deltaTime);
            if (settingsOverlay.handleInput(keyboard)) {
                isSettingsOpen = false;
            }
            return;
        }

        if (saveToastTimer > 0.0) {
            saveToastTimer -= deltaTime;
        }

        // --- 2. Tactical Pause Menu ---
        if (isPaused) {
            pauseOverlay.update(deltaTime);
            PauseOverlay.PauseAction action = pauseOverlay.handleInput(keyboard);
            switch (action) {
                case RESUME -> isPaused = false;
                case SAVE_GAME -> quickSave();
                case LOAD_GAME -> quickLoad();
                case RESTART -> initialize();
                case OPEN_SETTINGS -> isSettingsOpen = true;
                case QUIT_TO_TITLE -> {
                    com.cyberheist.audio.AudioManager.getInstance().stopAmbient();
                    engine.getSceneManager().setScene(new MainMenuScene(engine));
                }
                case NONE -> {
                    if (keyboard.isKeyPressed(KeyEvent.VK_ESCAPE)) {
                        isPaused = false;
                    }
                }
            }
            return;
        }

        // --- Quick Save (F5) / Quick Load (F9) Hotkeys ---
        if (keyboard.isKeyPressed(KeyEvent.VK_F5)) {
            quickSave();
        } else if (keyboard.isKeyPressed(KeyEvent.VK_F9)) {
            quickLoad();
        }

        // --- 3. Escape key routing (Hacking -> Upgrades -> Pause) ---
        if (keyboard.isKeyPressed(KeyEvent.VK_ESCAPE)) {
            if (activeHackingSession != null) {
                activeHackingSession.abort();
                com.cyberheist.audio.AudioManager.getInstance().play(com.cyberheist.audio.SoundEffect.HACK_ERROR);
                activeHackingSession = null;
                return;
            } else if (progressionMatrixOpen) {
                progressionMatrixOpen = false;
                return;
            } else {
                isPaused = true;
                pauseOverlay.reset();
                com.cyberheist.audio.AudioManager.getInstance().play(com.cyberheist.audio.SoundEffect.HACK_CLICK);
                return;
            }
        }

        // --- 4. Progression Matrix Toggle (P or U) ---
        if (keyboard.isKeyPressed(KeyEvent.VK_P) || keyboard.isKeyPressed(KeyEvent.VK_U)) {
            progressionMatrixOpen = !progressionMatrixOpen;
            return;
        }

        if (progressionMatrixOpen) {
            handleProgressionInput();
            return;
        }

        // Update atmospheric particles and lighting
        particleSystem.update(deltaTime);
        lightingRenderer.update(deltaTime, stealthSystem.getGlobalState() == DetectionState.DETECTED);

        // Mission progression and radio dialogue update continuously
        missionManager.update(deltaTime, player, level, stealthSystem, radioDialogue);
        radioDialogue.update(deltaTime);

        // Sync data payload from server rack to inventory
        if (!inventory.isDataPayloadSecured()) {
            for (ServerRack rack : level.getServerRacks()) {
                if (rack.isHacked()) {
                    inventory.setDataPayloadSecured(true);
                    progression.addXp(100);
                    particleSystem.spawnDataBits(rack.getBoundingBox().getMinX() + 20, rack.getBoundingBox().getMinY() + 20, 15);
                    com.cyberheist.audio.AudioManager.getInstance().play(com.cyberheist.audio.SoundEffect.HACK_SUCCESS);
                    break;
                }
            }
        }

        // If Hacking Minigame is active, route player input to hacking
        if (activeHackingSession != null) {
            handleHackingInput();
            activeHackingSession.update(deltaTime);

            if (activeHackingSession.isFinished() && activeHackingSession.getResultDisplayTimer() > 1.2) {
                if (activeHackingSession.getStatus() == HackingSession.SessionStatus.SUCCESS) {
                    progression.addXp(50);
                    particleSystem.spawnDataBits(player.getX() + 14.0, player.getY() + 14.0, 12);
                    com.cyberheist.audio.AudioManager.getInstance().play(com.cyberheist.audio.SoundEffect.HACK_SUCCESS);
                } else {
                    com.cyberheist.audio.AudioManager.getInstance().play(com.cyberheist.audio.SoundEffect.HACK_ERROR);
                }
                activeHackingSession = null;
            }

            // World and stealth tick while player hacks
            level.update(deltaTime, player);
            combat.update(deltaTime, player, level.getGuards(), stealthSystem);
            stealthSystem.update(deltaTime, player, level.getCollisionWorld(), level.getCoverObjects());
            camera.update(player.getX() + 14.0, player.getY() + 14.0, deltaTime, 6.5);
            return;
        }

        // 3. Toggle Stealth Debug Visualization (F4)
        if (keyboard.isKeyPressed(KeyEvent.VK_F4)) {
            stealthSystem.toggleDebugVisuals();
            LOGGER.info("Toggled Stealth Debug Tools. Enabled=" + stealthSystem.isDebugVisualsEnabled());
        }

        // 4. Handle Combat Gadgets & Inventory Input
        handleCombatAndInventoryInput();

        // 5. Update level architecture (doors, security cameras, server racks)
        level.update(deltaTime, player);

        // 6. Update tactical combat gadgets, projectiles, and takedowns
        combat.update(deltaTime, player, level.getGuards(), stealthSystem);

        // 7. Update player entity (movement, stamina, physical collisions, interactions)
        player.update(deltaTime, keyboard, level.getCollisionWorld(), level.getInteractables());

        // 8. Execute stealth sensory & acoustic simulation cycle
        stealthSystem.update(deltaTime, player, level.getCollisionWorld(), level.getCoverObjects());

        // 9. Smoothly track player with camera (including target lookahead and settings smoothing)
        double lookDist = com.cyberheist.settings.SettingsManager.getInstance().isCameraLeadEnabled() ? 35.0 : 0.0;
        double camTargetX = player.getX() + 14.0 + player.getFacingDirection().getDirX() * lookDist;
        double camTargetY = player.getY() + 14.0 + player.getFacingDirection().getDirY() * lookDist;
        camera.update(camTargetX, camTargetY, deltaTime, com.cyberheist.settings.SettingsManager.getInstance().getCameraSmoothing());

        // 10. Mouse click verification (translated to world coordinates)
        if (mouse.isButtonClicked(MouseEvent.BUTTON1)) {
            double worldClickX = camera.toWorldX(mouse.getX());
            double worldClickY = camera.toWorldY(mouse.getY());
            clickPings.add(new ClickPing(worldClickX, worldClickY));
        }

        Iterator<ClickPing> iterator = clickPings.iterator();
        while (iterator.hasNext()) {
            ClickPing ping = iterator.next();
            ping.update(deltaTime);
            if (ping.isExpired()) {
                iterator.remove();
            }
        }
    }

    private void handleCombatAndInventoryInput() {
        // [Q] Stun Device
        if (keyboard.isKeyPressed(KeyEvent.VK_Q)) {
            if (combat.fireStunDevice(player, level.getGuards(), stealthSystem, progression)) {
                camera.addTrauma(0.35);
                double sparkX = player.getX() + 14.0 + player.getFacingDirection().getDirX() * 50.0;
                double sparkY = player.getY() + 14.0 + player.getFacingDirection().getDirY() * 50.0;
                particleSystem.spawnStunSparks(sparkX, sparkY, 18);
                com.cyberheist.audio.AudioManager.getInstance().play(com.cyberheist.audio.SoundEffect.STUN_ZAP);
            }
        }

        // [F] Non-Lethal Takedown
        if (keyboard.isKeyPressed(KeyEvent.VK_F)) {
            if (combat.executeTakedown(player, progression)) {
                camera.addTrauma(0.28);
                particleSystem.spawnTakedownPuff(player.getX() + 14.0, player.getY() + 14.0);
                com.cyberheist.audio.AudioManager.getInstance().play(com.cyberheist.audio.SoundEffect.TAKEDOWN_THUD);
            }
        }

        // [G] or Right-Click: Throwable Distraction Decoy
        if (keyboard.isKeyPressed(KeyEvent.VK_G)) {
            double aimX = player.getX() + 14.0 + player.getFacingDirection().getDirX() * 160.0;
            double aimY = player.getY() + 14.0 + player.getFacingDirection().getDirY() * 160.0;
            if (combat.throwDistraction(player, aimX, aimY, stealthSystem, progression)) {
                com.cyberheist.audio.AudioManager.getInstance().play(com.cyberheist.audio.SoundEffect.DISTRACTION_PING);
            }
        } else if (mouse.isButtonClicked(MouseEvent.BUTTON3)) {
            double worldClickX = camera.toWorldX(mouse.getX());
            double worldClickY = camera.toWorldY(mouse.getY());
            if (combat.throwDistraction(player, worldClickX, worldClickY, stealthSystem, progression)) {
                com.cyberheist.audio.AudioManager.getInstance().play(com.cyberheist.audio.SoundEffect.DISTRACTION_PING);
            }
        }

        // [1] EMP Device
        if (keyboard.isKeyPressed(KeyEvent.VK_1)) {
            if (combat.deployEmp(player, inventory, level.getGuards(), level.getCameras(), stealthSystem, progression)) {
                camera.addTrauma(0.70);
                particleSystem.spawnEmpPulse(player.getX() + 14.0, player.getY() + 14.0, 35);
                com.cyberheist.audio.AudioManager.getInstance().play(com.cyberheist.audio.SoundEffect.EMP_BLAST);
            }
        }

        // [2] or [L]: Lockpick Action
        if (keyboard.isKeyPressed(KeyEvent.VK_2) || keyboard.isKeyPressed(KeyEvent.VK_L)) {
            handleLockpickAction();
        }

        // [3]: Medkit Action
        if (keyboard.isKeyPressed(KeyEvent.VK_3)) {
            handleMedkitAction();
        }

        // [4]: Data Drive Action
        if (keyboard.isKeyPressed(KeyEvent.VK_4)) {
            if (inventory.isDataPayloadSecured()) {
                LOGGER.info("Data Drive: Encrypted corporate data payload secured ($250k).");
            } else {
                LOGGER.info("Data Drive: Ready. Awaiting server room extraction.");
            }
        }
    }

    private void handleLockpickAction() {
        if (!inventory.hasItem(ItemType.LOCKPICK, 1)) {
            LOGGER.info("No lockpicks remaining in inventory.");
            return;
        }

        Interactable active = player.getActiveInteractable();
        if (active instanceof Door door && door.isLocked()) {
            if (inventory.consumeItem(ItemType.LOCKPICK, 1)) {
                door.unlockWithLockpick();
                progression.addXp(20);
                LOGGER.info("Door bypassed with lockpick. Remaining: " + inventory.getCount(ItemType.LOCKPICK));
            }
        } else if (active instanceof Safe safe && !safe.isOpened()) {
            if (inventory.consumeItem(ItemType.LOCKPICK, 1)) {
                safe.unlockWithLockpick();
                progression.addXp(35);
                LOGGER.info("Safe opened with lockpick. Remaining: " + inventory.getCount(ItemType.LOCKPICK));
            }
        }
    }

    private void handleMedkitAction() {
        if (!inventory.hasItem(ItemType.MEDKIT, 1)) {
            LOGGER.info("No medkits remaining in inventory.");
            return;
        }
        if (inventory.consumeItem(ItemType.MEDKIT, 1)) {
            player.getStaminaSystem().restoreFull();
            LOGGER.info("Medkit applied! Stamina fully restored.");
        }
    }

    private void handleProgressionInput() {
        if (keyboard.isKeyPressed(KeyEvent.VK_ESCAPE) || keyboard.isKeyPressed(KeyEvent.VK_P) || keyboard.isKeyPressed(KeyEvent.VK_U)) {
            progressionMatrixOpen = false;
            return;
        }

        if (keyboard.isKeyPressed(KeyEvent.VK_1)) {
            upgradeTrack(ProgressionTrack.HACKING);
        } else if (keyboard.isKeyPressed(KeyEvent.VK_2)) {
            upgradeTrack(ProgressionTrack.STEALTH);
        } else if (keyboard.isKeyPressed(KeyEvent.VK_3)) {
            upgradeTrack(ProgressionTrack.MOVEMENT);
        } else if (keyboard.isKeyPressed(KeyEvent.VK_4)) {
            upgradeTrack(ProgressionTrack.EQUIPMENT);
        }

        if (mouse.isButtonClicked(MouseEvent.BUTTON1)) {
            ProgressionTrack track = progressionHUD.getTrackAt(mouse.getX(), mouse.getY(),
                    GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
            if (track != null) {
                upgradeTrack(track);
            }
        }
    }

    private void upgradeTrack(ProgressionTrack track) {
        if (progression.upgrade(track)) {
            LOGGER.info("Upgraded progression track: " + track.getDisplayName() + " to rank " + progression.getRank(track));
            player.setSprintSpeed(380.0 * progression.getSprintSpeedMultiplier());
            inventory.applyCapacityBonus(progression.getItemCapacityBonus());
        }
    }

    @Override
    public void render(Graphics2D g2d) {
        int viewMinX = (int) Math.floor(camera.getX());
        int viewMinY = (int) Math.floor(camera.getY());
        int viewMaxX = viewMinX + camera.getViewportWidth();
        int viewMaxY = viewMinY + camera.getViewportHeight();

        // --- WORLD SPACE RENDERING (Transformed by Camera) ---
        camera.applyTransform(g2d);

        // 1. Render level architecture, floors, cover, doors, and security cameras
        level.render(g2d, viewMinX, viewMinY, viewMaxX, viewMaxY, player.getBoundingBox());

        // 2. Render click pings in world space
        for (ClickPing ping : clickPings) {
            ping.render(g2d);
        }

        // 3. Render stealth debug overlays (Vision cones, LOS ray lines, noise ripples)
        stealthSystem.renderDebugVisuals(g2d, player);

        // 4. Render security guards
        for (Guard guard : level.getGuards()) {
            guard.render(g2d);
        }

        // 5. Render player character
        player.render(g2d);

        // 6. Render tactical combat effects, projectiles and takedown prompts
        combat.renderWorld(g2d);

        // 7. Render dynamic particle effects in world space
        particleSystem.render(g2d);

        camera.resetTransform(g2d);
        // --- SCREEN SPACE RENDERING (HUD & UI) ---

        // Atmospheric Dark Ambient Lighting & Player Visor Spotlight
        double playerScreenX = player.getX() + 14.0 - camera.getX();
        double playerScreenY = player.getY() + 14.0 - camera.getY();
        lightingRenderer.render(g2d, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT,
                playerScreenX, playerScreenY, stealthSystem.getGlobalState() == DetectionState.DETECTED);

        // 8. Current Area HUD Banner
        renderCurrentAreaBanner(g2d);

        // 9. Mission 1 Objective Tracker & Radio Dialogue Comms Overlay
        missionHUD.render(g2d, missionManager, radioDialogue, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);

        // 10. Central Stealth Detection Meter (0% to 100%) & Facility Security Badge
        renderStealthDetectionMeter(g2d);
        renderFacilitySecurityBadge(g2d);

        // 11. Interaction Prompt Banner (hidden when hacking)
        if (activeHackingSession == null) {
            renderInteractionPrompt(g2d);
        }

        // 12. Movement & Level Telemetry Panel
        renderTelemetryHUD(g2d);

        // 13. Tactical Weapons & Inventory Quickbar HUD
        tacticalHUD.render(g2d, combat, inventory, progression, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);

        // 14. Bottom Controls & Objective Banner
        renderBottomBar(g2d);

        // 15. Progression Upgrade Matrix Modal Overlay
        if (progressionMatrixOpen) {
            progressionHUD.render(g2d, progression, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT,
                    mouse.getX(), mouse.getY());
        }

        // 16. Hacking Minigame Overlay (Screen Space)
        if (activeHackingSession != null) {
            hackingRenderer.render(g2d, activeHackingSession, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT,
                    mouse.getX(), mouse.getY());
        }

        // 17. Tactical Settings Overlay
        if (isSettingsOpen) {
            settingsOverlay.render(g2d, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
        }
        // 18. Tactical Pause Overlay
        else if (isPaused) {
            pauseOverlay.render(g2d, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
        }

        // 19. Mission Boot & Loading Screen (Topmost until deployed)
        if (!loadingTransition.isCompleted()) {
            loadingTransition.render(g2d, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
        }

        // 20. Archive Save / Load HUD Toast Banner
        renderSaveToast(g2d);
    }

    private void renderStealthDetectionMeter(Graphics2D g2d) {
        double pct = stealthSystem.getHighestDetection();
        DetectionState state = stealthSystem.getGlobalState();

        int cx = GameConfig.WINDOW_WIDTH / 2;
        int topY = 20;
        int meterW = 320;
        int meterH = 14;
        int barX = cx - (meterW / 2);

        // Header Background
        g2d.setColor(new Color(10, 14, 23, 230));
        g2d.fillRoundRect(barX - 16, topY - 6, meterW + 32, 48, 8, 8);
        g2d.setColor(new Color(0x35, 0x48, 0x62));
        g2d.drawRoundRect(barX - 16, topY - 6, meterW + 32, 48, 8, 8);

        // Title and State Label
        g2d.setFont(GameConfig.FONT_CODE);
        g2d.setColor(GameConfig.COLOR_TEXT_MUTED);
        g2d.drawString("DETECTION LEVEL:", barX, topY + 10);

        Color stateColor = switch (state) {
            case DETECTED -> Color.RED;
            case SUSPICIOUS -> new Color(255, 185, 0);
            default -> GameConfig.COLOR_GREEN;
        };
        g2d.setColor(stateColor);
        g2d.drawString(String.format("%s (%.1f%%)", state.name(), pct), barX + 130, topY + 10);

        // Progress Bar Track
        int barY = topY + 18;
        g2d.setColor(new Color(20, 26, 38));
        g2d.fillRect(barX, barY, meterW, meterH);

        // Fill Bar
        int fillW = (int) Math.round((meterW * Math.min(100.0, pct)) / 100.0);
        g2d.setColor(stateColor);
        g2d.fillRect(barX, barY, fillW, meterH);

        // Threshold Markers (25% and 60%)
        g2d.setColor(new Color(255, 255, 255, 100));
        int t25 = barX + (meterW * 25 / 100);
        int t60 = barX + (meterW * 60 / 100);
        g2d.drawLine(t25, barY - 2, t25, barY + meterH + 2);
        g2d.drawLine(t60, barY - 2, t60, barY + meterH + 2);

        // Border
        g2d.setColor(new Color(0x40, 0x55, 0x70));
        g2d.drawRect(barX, barY, meterW, meterH);
    }

    private void renderFacilitySecurityBadge(Graphics2D g2d) {
        if (securityDirector == null) return;
        com.cyberheist.security.SecurityLevel secLvl = securityDirector.getCurrentLevel();

        int cx = GameConfig.WINDOW_WIDTH / 2;
        int badgeW = 280;
        int badgeH = 22;
        int bx = cx - (badgeW / 2);
        int by = 54;

        g2d.setColor(new Color(10, 14, 23, 230));
        g2d.fillRoundRect(bx, by, badgeW, badgeH, 4, 4);

        Color badgeColor = secLvl.getBadgeColor();
        if (secLvl == com.cyberheist.security.SecurityLevel.LOCKDOWN) {
            float strobe = (float) ((Math.sin(System.currentTimeMillis() * 0.008) + 1.0) * 0.5);
            badgeColor = new Color(1.0f, strobe * 0.2f, strobe * 0.2f);
        }

        g2d.setColor(badgeColor);
        g2d.drawRoundRect(bx, by, badgeW, badgeH, 4, 4);

        g2d.setFont(GameConfig.FONT_CODE);
        String label = "FACILITY: " + secLvl.getLabel();
        if (secLvl != com.cyberheist.security.SecurityLevel.NORMAL && securityDirector.getLevelDecayTimer() > 0.0) {
            label += String.format(" [%.0fs]", securityDirector.getLevelDecayTimer());
        }
        int textW = g2d.getFontMetrics().stringWidth(label);
        g2d.drawString(label, cx - (textW / 2), by + 15);
    }

    public com.cyberheist.security.SecurityDirector getSecurityDirector() {
        return securityDirector;
    }

    private String resolveCurrentAreaName() {
        double px = player.getX();
        double py = player.getY();

        if (py < 190) {
            return "ZONE 01 // EXTERIOR STREET & ENTRANCE";
        } else if (py < 470) {
            return (px < 840) ? "ZONE 02 // RECEPTION LOBBY" : "ZONE 03 // SECURITY CHECKPOINT";
        } else if (py < 750) {
            return (px < 960) ? "ZONE 04 // CORPORATE WORKSTATION FLOOR" : "ZONE 05 // HIGH-DENSITY SERVER FARM (TIER IV)";
        } else {
            return (px < 1200) ? "ZONE 06 // RESTRICTED DATA VAULT" : "ZONE 07 // EMERGENCY FIRE EXIT";
        }
    }

    private void renderCurrentAreaBanner(Graphics2D g2d) {
        String areaName = resolveCurrentAreaName();

        g2d.setFont(GameConfig.FONT_UI);
        g2d.setColor(new Color(10, 14, 23, 220));
        g2d.fillRoundRect(24, 18, 380, 36, 6, 6);
        g2d.setColor(new Color(0x35, 0x48, 0x62));
        g2d.drawRoundRect(24, 18, 380, 36, 6, 6);

        // Active zone indicator dot
        g2d.setColor(GameConfig.COLOR_GREEN);
        g2d.fillOval(38, 30, 8, 8);

        g2d.setColor(GameConfig.COLOR_TEXT_BRIGHT);
        g2d.drawString(areaName, 56, 41);
    }

    private void renderInteractionPrompt(Graphics2D g2d) {
        Interactable active = player.getActiveInteractable();
        if (active != null) {
            int cx = GameConfig.WINDOW_WIDTH / 2;
            int cy = 96;

            String prompt = active.getInteractionPrompt();
            g2d.setFont(GameConfig.FONT_TITLE);

            // Semi-transparent card
            g2d.setColor(new Color(10, 14, 23, 230));
            g2d.fillRoundRect(cx - 200, cy - 28, 400, 44, 8, 8);
            g2d.setColor(GameConfig.COLOR_CYAN);
            g2d.drawRoundRect(cx - 200, cy - 28, 400, 44, 8, 8);

            // Glowing prompt text
            g2d.setColor(GameConfig.COLOR_CYAN);
            g2d.drawString(prompt, cx - 180, cy);
        }
    }

    private void renderTelemetryHUD(Graphics2D g2d) {
        int panelX = GameConfig.WINDOW_WIDTH - 320;
        int panelY = 18;
        int panelW = 300;
        int panelH = 240;

        g2d.setColor(new Color(10, 14, 23, 220));
        g2d.fillRoundRect(panelX, panelY, panelW, panelH, 8, 8);
        g2d.setColor(new Color(0x28, 0x3A, 0x50));
        g2d.drawRoundRect(panelX, panelY, panelW, panelH, 8, 8);

        g2d.setFont(GameConfig.FONT_UI);
        g2d.setColor(GameConfig.COLOR_CYAN);
        g2d.drawString("STEALTH TELEMETRY", panelX + 14, panelY + 22);

        g2d.setFont(GameConfig.FONT_CODE);
        int lineY = panelY + 44;

        // Player state
        Color stateColor = switch (player.getState()) {
            case RUNNING -> GameConfig.COLOR_CYAN;
            case WALKING -> GameConfig.COLOR_GREEN;
            case INTERACTING -> GameConfig.COLOR_GREEN;
            case STUNNED -> GameConfig.COLOR_MAGENTA;
            case DEAD -> Color.RED;
            default -> GameConfig.COLOR_TEXT_BRIGHT;
        };
        g2d.setColor(GameConfig.COLOR_TEXT_MUTED);
        g2d.drawString("STATUS:", panelX + 14, lineY);
        g2d.setColor(stateColor);
        g2d.drawString(player.getState().name(), panelX + 90, lineY);
        lineY += 18;

        // Position & speed
        g2d.setColor(GameConfig.COLOR_TEXT_MUTED);
        g2d.drawString("POS:", panelX + 14, lineY);
        g2d.setColor(GameConfig.COLOR_TEXT_BRIGHT);
        g2d.drawString(String.format("X=%.0f, Y=%.0f", player.getX(), player.getY()), panelX + 90, lineY);
        lineY += 18;

        g2d.setColor(GameConfig.COLOR_TEXT_MUTED);
        g2d.drawString("SPEED:", panelX + 14, lineY);
        g2d.setColor(GameConfig.COLOR_CYAN);
        g2d.drawString(String.format("%.1f px/s (%s)", player.getCurrentSpeed(), player.getFacingDirection().name()), panelX + 90, lineY);
        lineY += 18;

        // Stamina
        double stam = player.getStaminaSystem().getCurrentStamina();
        double maxStam = player.getStaminaSystem().getMaxStamina();
        g2d.setColor(GameConfig.COLOR_TEXT_MUTED);
        g2d.drawString("STAMINA:", panelX + 14, lineY);
        g2d.setColor(player.getStaminaSystem().isExhausted() ? GameConfig.COLOR_MAGENTA : GameConfig.COLOR_GREEN);
        g2d.drawString(String.format("%.0f / %.0f", stam, maxStam), panelX + 90, lineY);
        lineY += 16;

        // Stamina bar
        int barX = panelX + 14;
        int barW = panelW - 28;
        int barH = 5;
        g2d.setColor(new Color(20, 25, 40));
        g2d.fillRect(barX, lineY, barW, barH);
        int fillW = (int) Math.round(barW * player.getStaminaSystem().getPercentage());
        g2d.setColor(player.getStaminaSystem().isExhausted() ? GameConfig.COLOR_MAGENTA : GameConfig.COLOR_GREEN);
        g2d.fillRect(barX, lineY, fillW, barH);
        lineY += 20;

        // Active noise events count
        int noises = stealthSystem.getNoiseManager().getActiveNoises().size();
        g2d.setColor(GameConfig.COLOR_TEXT_MUTED);
        g2d.drawString("ACOUSTICS:", panelX + 14, lineY);
        g2d.setColor(noises > 0 ? new Color(255, 180, 0) : GameConfig.COLOR_TEXT_BRIGHT);
        g2d.drawString(noises + " active waves", panelX + 90, lineY);
        lineY += 18;

        // Stealth Debug toggle status
        g2d.setColor(GameConfig.COLOR_TEXT_MUTED);
        g2d.drawString("DEBUG (F4):", panelX + 14, lineY);
        g2d.setColor(stealthSystem.isDebugVisualsEnabled() ? GameConfig.COLOR_GREEN : Color.GRAY);
        g2d.drawString(stealthSystem.isDebugVisualsEnabled() ? "ACTIVE (Cones+LOS)" : "OFF", panelX + 90, lineY);
        lineY += 18;

        // Objective indicator
        String objTitle = missionManager.getCurrentObjective() != null ?
                missionManager.getCurrentObjective().getTitle() : "MISSION COMPLETE";
        boolean isEscaped = missionManager.getStatus() == MissionManager.MissionStatus.SUCCESS;
        g2d.setColor(GameConfig.COLOR_TEXT_MUTED);
        g2d.drawString("OBJECTIVE:", panelX + 14, lineY);
        g2d.setColor(isEscaped ? GameConfig.COLOR_GREEN : GameConfig.COLOR_CYAN);
        g2d.drawString(objTitle.toUpperCase(), panelX + 90, lineY);
    }

    private void renderBottomBar(Graphics2D g2d) {
        int bottomY = GameConfig.WINDOW_HEIGHT - 35;
        g2d.setColor(new Color(10, 14, 23, 230));
        g2d.fillRoundRect(20, bottomY - 20, GameConfig.WINDOW_WIDTH - 40, 40, 8, 8);
        g2d.setColor(new Color(0x28, 0x3A, 0x50));
        g2d.drawRoundRect(20, bottomY - 20, GameConfig.WINDOW_WIDTH - 40, 40, 8, 8);

        g2d.setFont(GameConfig.FONT_CODE);
        g2d.setColor(GameConfig.COLOR_TEXT_BRIGHT);
        String controls = "[WASD] Move | [Shift] Sprint | [E] Interact | [Q] Stun | [1] EMP | [F] Takedown | [P] Upgrades | [F5] Save | [F9] Load";
        g2d.drawString(controls, 36, bottomY + 5);

        g2d.setColor(GameConfig.COLOR_CYAN);
        g2d.drawString("CYBER HEIST // INTRUSION OS", GameConfig.WINDOW_WIDTH - 250, bottomY + 5);
    }

    private void handleHackingInput() {
        if (activeHackingSession == null) return;

        // Abort hack
        if (keyboard.isKeyPressed(KeyEvent.VK_ESCAPE)) {
            activeHackingSession.abort();
            return;
        }

        // Keys 1 through 8 for node selection
        int[] numKeys = {
                KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4,
                KeyEvent.VK_5, KeyEvent.VK_6, KeyEvent.VK_7, KeyEvent.VK_8
        };
        for (int i = 0; i < numKeys.length; i++) {
            if (keyboard.isKeyPressed(numKeys[i])) {
                activeHackingSession.selectNode(i + 1);
                break;
            }
        }

        // Mouse click on node buttons
        if (mouse.isButtonClicked(MouseEvent.BUTTON1)) {
            int mx = mouse.getX();
            int my = mouse.getY();
            for (NetworkNode node : activeHackingSession.getNodePool()) {
                if (node.containsPoint(mx, my)) {
                    activeHackingSession.selectNode(node.getIndex());
                    break;
                }
            }
        }
    }

    public HackingSession getActiveHackingSession() { return activeHackingSession; }
    public void setActiveHackingSession(HackingSession session) { this.activeHackingSession = session; }
    public MissionManager getMissionManager() { return missionManager; }
    public RadioDialogueManager getRadioDialogue() { return radioDialogue; }
    public Inventory getInventory() { return inventory; }
    public ProgressionManager getProgression() { return progression; }
    public CombatManager getCombatManager() { return combat; }
    public boolean isProgressionMatrixOpen() { return progressionMatrixOpen; }
    public void setProgressionMatrixOpen(boolean open) { this.progressionMatrixOpen = open; }
    public Player getPlayer() { return player; }
    public NoidaDataCenterLevel getLevel() { return level; }
    public StealthSystem getStealthSystem() { return stealthSystem; }
    public Camera2D getCamera() { return camera; }

    public void showToast(String message) {
        this.saveToastMessage = message;
        this.saveToastTimer = 2.8;
    }

    public boolean quickSave() {
        SaveData data = SaveManager.captureGameState(this);
        boolean success = SaveManager.saveGame(data, SaveManager.DEFAULT_SAVE_FILE);
        if (success) {
            showToast("HEIST ARCHIVED // SAVES/QUICKSAVE.SAV");
            com.cyberheist.audio.AudioManager.getInstance().play(com.cyberheist.audio.SoundEffect.HACK_SUCCESS);
        } else {
            showToast("SAVE FAILED // DISK WRITE ERROR");
            com.cyberheist.audio.AudioManager.getInstance().play(com.cyberheist.audio.SoundEffect.HACK_ERROR);
        }
        return success;
    }

    public boolean quickLoad() {
        SaveData data = SaveManager.loadGame(SaveManager.DEFAULT_SAVE_FILE);
        if (data != null) {
            boolean success = SaveManager.applyGameState(this, data);
            if (success) {
                showToast("HEIST RESTORED // OPERATOR IN POSITION");
                com.cyberheist.audio.AudioManager.getInstance().play(com.cyberheist.audio.SoundEffect.HACK_SUCCESS);
                this.isPaused = false;
                return true;
            }
        }
        showToast("LOAD FAILED // NO ARCHIVED SAVE FOUND");
        com.cyberheist.audio.AudioManager.getInstance().play(com.cyberheist.audio.SoundEffect.HACK_ERROR);
        return false;
    }

    private void renderSaveToast(Graphics2D g2d) {
        if (saveToastTimer <= 0.0 || saveToastMessage == null) return;

        int cx = GameConfig.WINDOW_WIDTH / 2;
        int bannerY = 74;
        int bannerW = 440;
        int bannerH = 34;

        double alphaPct = Math.min(1.0, saveToastTimer * 2.0);
        int alpha = (int) (alphaPct * 230);
        int borderAlpha = (int) (alphaPct * 255);

        g2d.setColor(new Color(8, 14, 24, alpha));
        g2d.fillRoundRect(cx - (bannerW / 2), bannerY, bannerW, bannerH, 6, 6);

        boolean isSuccess = saveToastMessage.contains("ARCHIVED") || saveToastMessage.contains("RESTORED");
        Color borderColor = isSuccess ? new Color(0, 255, 180, borderAlpha) : new Color(255, 60, 60, borderAlpha);
        g2d.setColor(borderColor);
        g2d.drawRoundRect(cx - (bannerW / 2), bannerY, bannerW, bannerH, 6, 6);

        g2d.setFont(GameConfig.FONT_CODE);
        g2d.setColor(isSuccess ? GameConfig.COLOR_GREEN : Color.RED);
        g2d.drawString("●", cx - (bannerW / 2) + 14, bannerY + 22);

        g2d.setColor(GameConfig.COLOR_TEXT_BRIGHT);
        g2d.drawString(saveToastMessage, cx - (bannerW / 2) + 32, bannerY + 22);
    }

    @Override
    public void dispose() {
        LOGGER.info("Disposing EmptyPlayableScene.");
        com.cyberheist.audio.AudioManager.getInstance().stopAmbient();
        particleSystem.clear();
        clickPings.clear();
    }

    @Override
    public String getName() {
        return "NoidaDataCenterScene";
    }

    private static final class ClickPing {
        private final double x;
        private final double y;
        private double age = 0.0;
        private static final double LIFETIME = 0.5;

        public ClickPing(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public void update(double deltaTime) {
            age += deltaTime;
        }

        public boolean isExpired() {
            return age >= LIFETIME;
        }

        public void render(Graphics2D g2d) {
            double progress = age / LIFETIME;
            int radius = (int) (progress * 30.0);
            int alpha = (int) ((1.0 - progress) * 200.0);
            alpha = Math.max(0, Math.min(255, alpha));

            int px = (int) Math.round(x);
            int py = (int) Math.round(y);

            g2d.setColor(new Color(0, 240, 255, alpha));
            g2d.drawOval(px - radius, py - radius, radius * 2, radius * 2);
        }
    }
}
