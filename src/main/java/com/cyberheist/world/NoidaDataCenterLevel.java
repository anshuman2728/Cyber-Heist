package com.cyberheist.world;

import com.cyberheist.ai.GuardAI;
import com.cyberheist.ai.pathfinding.AStarPathfinder;
import com.cyberheist.ai.pathfinding.NavGrid;
import com.cyberheist.collision.BoundingBox;
import com.cyberheist.collision.CollisionWorld;
import com.cyberheist.collision.Obstacle;
import com.cyberheist.core.GameConfig;
import com.cyberheist.interact.Interactable;
import com.cyberheist.stealth.Guard;
import com.cyberheist.util.Logger;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * Handcrafted Level 1: "Noida Corporate Data Center".
 * Features 7 distinct zones designed with realistic corporate architecture:
 * 1. Street / Exterior Entrance
 * 2. Corporate Reception
 * 3. Security Checkpoint
 * 4. Open Office Area
 * 5. High-Density Server Room
 * 6. Restricted Data Vault
 * 7. Emergency Exit & Extraction
 */
public final class NoidaDataCenterLevel {

    private static final Logger LOGGER = Logger.getLogger(NoidaDataCenterLevel.class);

    private static final int TILE_SIZE = 40;
    private static final int COLS = 44; // 1760px
    private static final int ROWS = 26; // 1040px

    private final TileMap tileMap;
    private final CollisionWorld collisionWorld;
    private final NavGrid navGrid;
    private final AStarPathfinder pathfinder;

    private final List<Door> doors = new ArrayList<>();
    private final List<CoverObject> coverObjects = new ArrayList<>();
    private final List<ComputerWorkstation> workstations = new ArrayList<>();
    private final List<SecurityCamera> cameras = new ArrayList<>();
    private final List<SecurityTerminal> terminals = new ArrayList<>();
    private final List<AlarmPanel> alarms = new ArrayList<>();
    private final List<ServerRack> serverRacks = new ArrayList<>();
    private final List<Elevator> elevators = new ArrayList<>();
    private final List<Safe> safes = new ArrayList<>();
    private final List<Guard> guards = new ArrayList<>();
    private final List<Interactable> interactables = new ArrayList<>();
    private com.cyberheist.security.SecurityDirector securityDirector = null;

    // Key points
    private final Point spawnPoint = new Point(220, 100); // Exterior sidewalk
    private final BoundingBox exitZone = new BoundingBox(1560, 880, 140, 100); // Extraction area

    public NoidaDataCenterLevel() {
        LOGGER.info("Constructing handcrafted Noida Corporate Data Center layout...");

        this.tileMap = new TileMap(COLS, ROWS, TILE_SIZE, TileType.STREET_ASPHALT);
        this.collisionWorld = new CollisionWorld(COLS * TILE_SIZE, ROWS * TILE_SIZE);

        buildFloorSurfaces();
        buildWallsAndPartitions();
        buildDoors();
        buildFurnitureAndCover();
        buildWorkstations();
        buildSecurityCameras();
        buildTerminals();
        buildAlarms();
        buildServerRacks();
        buildElevators();
        buildSafes();
        buildGuards();

        // Register all interactables (All 8 interaction objects)
        interactables.addAll(workstations); // 1. Computer
        interactables.addAll(cameras);      // 2. Camera
        interactables.addAll(doors);        // 3. Door
        interactables.addAll(terminals);    // 4. Terminal
        interactables.addAll(alarms);       // 5. Alarm
        interactables.addAll(serverRacks);  // 6. Server
        interactables.addAll(elevators);    // 7. Elevator
        interactables.addAll(safes);        // 8. Safe

        // Register collidables
        for (Door d : doors) collisionWorld.add(d);
        for (CoverObject c : coverObjects) collisionWorld.add(c);
        for (ComputerWorkstation w : workstations) collisionWorld.add(w);
        for (SecurityTerminal t : terminals) collisionWorld.add(t);
        for (AlarmPanel a : alarms) collisionWorld.add(a);
        for (ServerRack s : serverRacks) collisionWorld.add(s);
        for (Elevator e : elevators) collisionWorld.add(e);
        for (Safe s : safes) collisionWorld.add(s);
        for (Guard g : guards) collisionWorld.add(g);

        // Wire terminal master override to loop all camera feeds and clear security alert
        for (SecurityTerminal t : terminals) {
            t.setOverrideCallback(() -> {
                for (SecurityCamera cam : cameras) {
                    cam.onHackSuccess();
                }
                if (securityDirector != null) {
                    securityDirector.onMasterTerminalOverride();
                }
            });
        }

        // Initialize Navigation Grid and A* Pathfinder (decoupled from rendering)
        this.navGrid = new NavGrid(COLS * TILE_SIZE, ROWS * TILE_SIZE, 20);
        this.navGrid.updateWalkability(collisionWorld, 14.0);
        this.pathfinder = new AStarPathfinder(navGrid);

        // Wire dynamic obstacle handling: Door state changes trigger local nav grid rebuilds
        for (Door door : doors) {
            door.setStateListener(d -> {
                navGrid.updateWalkabilityRegion(collisionWorld,
                        d.getX() - 30, d.getY() - 30,
                        d.getX() + d.getWidth() + 30, d.getY() + d.getHeight() + 30, 14.0);
            });
        }

        // Attach state-based AI with patrol routes
        setupGuardAI();

        LOGGER.info(String.format("Level built: %d doors, %d cover objects, %d workstations, %d cameras, %d guards, nav grid: %dx%d.",
                doors.size(), coverObjects.size(), workstations.size(), cameras.size(), guards.size(),
                navGrid.getColumns(), navGrid.getRows()));
    }

    private void setupGuardAI() {
        if (guards.size() >= 3) {
            // Guard 1: Security Checkpoint
            Guard g1 = guards.get(0);
            GuardAI ai1 = new GuardAI(g1, pathfinder);
            ai1.addPatrolWaypoint(1020, 340);
            ai1.addPatrolWaypoint(1160, 340);
            ai1.addPatrolWaypoint(1020, 430);
            ai1.addPatrolWaypoint(920, 360);
            g1.setAI(ai1);

            // Guard 2: Office Area
            Guard g2 = guards.get(1);
            GuardAI ai2 = new GuardAI(g2, pathfinder);
            ai2.addPatrolWaypoint(500, 580);
            ai2.addPatrolWaypoint(500, 700);
            ai2.addPatrolWaypoint(300, 700);
            ai2.addPatrolWaypoint(300, 580);
            g2.setAI(ai2);

            // Guard 3: Vault / Server Room junction
            Guard g3 = guards.get(2);
            GuardAI ai3 = new GuardAI(g3, pathfinder);
            ai3.addPatrolWaypoint(1120, 680);
            ai3.addPatrolWaypoint(1340, 680);
            ai3.addPatrolWaypoint(1340, 630);
            ai3.addPatrolWaypoint(1120, 630);
            g3.setAI(ai3);
        }
    }

    private void buildGuards() {
        // Guard 1: Stationed at Security Checkpoint watching turnstiles (facing West)
        guards.add(new Guard("GUARD-01 [SECURITY]", 1020, 310, 180));

        // Guard 2: Patrolling central aisle in Open Office (facing East)
        guards.add(new Guard("GUARD-02 [OFFICE]", 490, 580, 0));

        // Guard 3: Stationed outside Data Vault & Server Room junction (facing South)
        guards.add(new Guard("GUARD-03 [VAULT]", 1100, 680, 90));
    }

    private void buildFloorSurfaces() {
        // Section 1: Street & Exterior (Rows 0 - 4)
        tileMap.fillRect(0, 0, COLS, 2, TileType.STREET_ASPHALT);
        tileMap.fillRect(0, 2, COLS, 3, TileType.STREET_SIDEWALK);

        // Section 2: Reception (Cols 2 - 20, Rows 5 - 11)
        tileMap.fillRect(2, 5, 19, 7, TileType.LOBBY_MARBLE);

        // Section 3: Security Checkpoint (Cols 21 - 41, Rows 5 - 11)
        tileMap.fillRect(21, 5, 21, 7, TileType.SECURITY_CONCRETE);

        // Section 4: Office Area (Cols 2 - 23, Rows 12 - 18)
        tileMap.fillRect(2, 12, 22, 7, TileType.OFFICE_CARPET);

        // Section 5: Server Room (Cols 24 - 41, Rows 12 - 18)
        tileMap.fillRect(24, 12, 18, 7, TileType.SERVER_COOLING_TILES);

        // Section 6: Restricted Data Vault (Cols 8 - 29, Rows 19 - 24)
        tileMap.fillRect(8, 19, 22, 6, TileType.VAULT_STEEL_PLATING);

        // Section 7: Emergency Exit Corridor (Cols 30 - 41, Rows 19 - 24)
        tileMap.fillRect(30, 19, 12, 6, TileType.SECURITY_CONCRETE);
        tileMap.fillRect(36, 21, 6, 2, TileType.HAZARD_ZONE);
    }

    private void buildWallsAndPartitions() {
        int w = COLS * TILE_SIZE;
        int h = ROWS * TILE_SIZE;

        // 1. External Facility Perimeter Walls
        // Top building facade (Row 4, between sidewalk and interior)
        addWall("FACADE_WEST", 0, 190, 360, 20);
        addWall("FACADE_EAST", 440, 190, w - 440, 20); // Entrance doorway at x=360..440

        // Building Left & Right outer boundaries
        addWall("EXT_WALL_WEST", 0, 190, 80, h - 190);
        addWall("EXT_WALL_EAST", w - 80, 190, 80, h - 190);

        // Building Bottom outer boundary
        addWall("EXT_WALL_SOUTH", 0, h - 40, w, 40);

        // 2. Interior Dividing Walls
        // Wall between Reception/Security and Office/Server (Row 11 / Y=470)
        addWall("DIVIDER_NORTH_A", 80, 470, 400, 20);
        // Doorway at 480..560
        addWall("DIVIDER_NORTH_B", 560, 470, 400, 20);
        // Doorway to server room at 960..1040
        addWall("DIVIDER_NORTH_C", 1040, 470, w - 1120, 20);

        // Vertical Wall between Reception and Security (Col 21 / X=840, Rows 5 - 11)
        addWall("RECEPTION_SEC_WALL_TOP", 840, 210, 20, 100);
        // Turnstile gate opening at Y=310..410
        addWall("RECEPTION_SEC_WALL_BOT", 840, 410, 20, 60);

        // Vertical Wall between Office and Server Room (Col 24 / X=960, Rows 12 - 18)
        addWall("OFFICE_SERVER_WALL_TOP", 960, 490, 20, 110);
        // Secure badge door opening at Y=600..680
        addWall("OFFICE_SERVER_WALL_BOT", 960, 680, 20, 80);

        // Horizontal Wall separating Upper facility from Vault / Emergency Exit (Row 18 / Y=750)
        addWall("VAULT_NORTH_WALL_A", 80, 750, 440, 20);
        // Vault access corridor doorway at 520..600
        addWall("VAULT_NORTH_WALL_B", 600, 750, 600, 20);
        // Emergency exit access doorway at 1200..1280
        addWall("VAULT_NORTH_WALL_C", 1280, 750, w - 1360, 20);

        // Vertical Reinforced Wall between Data Vault and Emergency Exit (Col 30 / X=1200, Rows 19 - 24)
        addWall("VAULT_FIRE_WALL", 1200, 770, 24, h - 810);
    }

    private void addWall(String name, double x, double y, double width, double height) {
        Obstacle wall = new Obstacle(name, x, y, width, height, new Color(0x18, 0x22, 0x30), new Color(0x28, 0x3A, 0x50));
        collisionWorld.add(wall);
    }

    private void buildDoors() {
        // Door 1: Main Entrance Double Glass Slider (Horizontal)
        doors.add(new Door("Main Entrance Slider", Door.DoorType.GLASS_SLIDER, 360, 192, 80, 16, true));

        // Door 2: Security Turnstile Badge Gate (Vertical) - LOCKED (Hackable)
        Door turnstile = new Door("Security Checkpoint Gate", Door.DoorType.SECURITY_BADGE, 842, 310, 16, 90, false);
        turnstile.setLocked(true, null);
        doors.add(turnstile);

        // Door 3: Reception to Office Hallway Door (Horizontal) - Lockdown Protected
        Door officeDoor = new Door("Office Access Door", Door.DoorType.SECURITY_BADGE, 480, 472, 80, 16, true);
        officeDoor.setLockdownProtected(true);
        doors.add(officeDoor);

        // Door 4: Office to Server Room Biometric Door (Vertical) - Lockdown Protected
        Door serverDoor = new Door("Server Room Air-Lock", Door.DoorType.SECURITY_BADGE, 962, 600, 16, 80, false);
        serverDoor.setLockdownProtected(true);
        doors.add(serverDoor);

        // Door 5: Server Room to Vault Heavy Blast Door (Horizontal) - LOCKED (Hackable)
        Door vaultBlastDoor = new Door("Restricted Vault Blast Door", Door.DoorType.HEAVY_VAULT, 520, 752, 80, 16, true);
        vaultBlastDoor.setLocked(true, null);
        doors.add(vaultBlastDoor);

        // Door 6: Emergency Fire Exit Door (Horizontal)
        doors.add(new Door("Emergency Fire Exit", Door.DoorType.EMERGENCY_EXIT, 1200, 752, 80, 16, true));
    }

    private void buildFurnitureAndCover() {
        // --- 1. RECEPTION PROPS ---
        // Reception Counter Desk
        coverObjects.add(new CoverObject("Reception Counter", CoverObject.CoverType.RECEPTION_COUNTER, 340, 310, 160, 44));
        // Waiting Couches
        coverObjects.add(new CoverObject("Visitor Couch North", CoverObject.CoverType.LOBBY_COUCH, 120, 240, 120, 36));
        coverObjects.add(new CoverObject("Visitor Couch South", CoverObject.CoverType.LOBBY_COUCH, 120, 390, 120, 36));
        // Glass Observation Window between Reception and Security
        coverObjects.add(new CoverObject("Observation Glass", CoverObject.CoverType.GLASS_WINDOW, 844, 214, 12, 92));

        // --- 2. SECURITY CHECKPOINT PROPS ---
        // Guard Checkpoint Desk
        coverObjects.add(new CoverObject("Guard Desk", CoverObject.CoverType.OFFICE_DESK, 890, 240, 90, 44));
        // Turnstile pedestals
        coverObjects.add(new CoverObject("Turnstile Left", CoverObject.CoverType.SECURITY_TURNSTILE, 836, 310, 10, 26));
        coverObjects.add(new CoverObject("Turnstile Right", CoverObject.CoverType.SECURITY_TURNSTILE, 836, 374, 10, 26));
        // Security Locker & Cabinets
        coverObjects.add(new CoverObject("Security Lockers", CoverObject.CoverType.FILING_CABINET, 1100, 220, 110, 36));
        coverObjects.add(new CoverObject("Weapons Locker", CoverObject.CoverType.FILING_CABINET, 1100, 390, 110, 36));

        // --- 3. OPEN OFFICE AREA PROPS ---
        // Cubicle Cluster 1
        coverObjects.add(new CoverObject("Desk Pod 1A", CoverObject.CoverType.OFFICE_DESK, 160, 530, 110, 44));
        coverObjects.add(new CoverObject("Desk Pod 1B", CoverObject.CoverType.OFFICE_DESK, 160, 630, 110, 44));
        coverObjects.add(new CoverObject("Partition 1", CoverObject.CoverType.CUBICLE_PARTITION, 140, 530, 16, 144));

        // Cubicle Cluster 2
        coverObjects.add(new CoverObject("Desk Pod 2A", CoverObject.CoverType.OFFICE_DESK, 380, 530, 110, 44));
        coverObjects.add(new CoverObject("Desk Pod 2B", CoverObject.CoverType.OFFICE_DESK, 380, 630, 110, 44));
        coverObjects.add(new CoverObject("Partition 2", CoverObject.CoverType.CUBICLE_PARTITION, 360, 530, 16, 144));

        // Cubicle Cluster 3
        coverObjects.add(new CoverObject("Desk Pod 3A", CoverObject.CoverType.OFFICE_DESK, 600, 530, 110, 44));
        coverObjects.add(new CoverObject("Desk Pod 3B", CoverObject.CoverType.OFFICE_DESK, 600, 630, 110, 44));
        coverObjects.add(new CoverObject("Partition 3", CoverObject.CoverType.CUBICLE_PARTITION, 580, 530, 16, 144));

        // Office Archive Storage
        coverObjects.add(new CoverObject("Archive Cabinets", CoverObject.CoverType.FILING_CABINET, 780, 520, 130, 32));

        // --- 4. SERVER ROOM PROPS ---
        // Cold Aisle 1 Server Racks
        coverObjects.add(new CoverObject("Rack Bank 1A", CoverObject.CoverType.SERVER_RACK, 1040, 510, 140, 36));
        coverObjects.add(new CoverObject("Rack Bank 1B", CoverObject.CoverType.SERVER_RACK, 1040, 580, 140, 36));

        // Cold Aisle 2 Server Racks
        coverObjects.add(new CoverObject("Rack Bank 2A", CoverObject.CoverType.SERVER_RACK, 1260, 510, 140, 36));
        coverObjects.add(new CoverObject("Rack Bank 2B", CoverObject.CoverType.SERVER_RACK, 1260, 580, 140, 36));

        // Cold Aisle 3 Server Racks
        coverObjects.add(new CoverObject("Rack Bank 3A", CoverObject.CoverType.SERVER_RACK, 1480, 510, 140, 36));
        coverObjects.add(new CoverObject("Rack Bank 3B", CoverObject.CoverType.SERVER_RACK, 1480, 580, 140, 36));

        // --- 5. RESTRICTED DATA VAULT PROPS ---
        // Backup Tape Storage Units
        coverObjects.add(new CoverObject("Tape Vault Bank A", CoverObject.CoverType.SERVER_RACK, 380, 810, 120, 36));
        coverObjects.add(new CoverObject("Tape Vault Bank B", CoverObject.CoverType.SERVER_RACK, 380, 910, 120, 36));
        coverObjects.add(new CoverObject("Crypto Hardware HSM", CoverObject.CoverType.SERVER_RACK, 940, 810, 120, 36));
        coverObjects.add(new CoverObject("Air Gap Backup Rack", CoverObject.CoverType.SERVER_RACK, 940, 910, 120, 36));
    }

    private void buildWorkstations() {
        // Reception Visitor Log Workstation
        workstations.add(new ComputerWorkstation("RECEPTION-LOG",
                ComputerWorkstation.WorkstationType.RECEPTION_DESK, 410, 314, 40, 32));

        // Security CCTV Command Console
        workstations.add(new ComputerWorkstation("SEC-CCTV-01",
                ComputerWorkstation.WorkstationType.SECURITY_CCTV, 915, 246, 40, 32));

        // Office HR Workstation
        workstations.add(new ComputerWorkstation("OFFICE-PC-07",
                ComputerWorkstation.WorkstationType.OFFICE_PC, 210, 536, 36, 32));

        // Server Room Admin Terminal
        workstations.add(new ComputerWorkstation("SYS-ADMIN-TERMINAL",
                ComputerWorkstation.WorkstationType.OFFICE_PC, 1080, 680, 44, 34));

        // Vault Central Mainframe Core (Primary Level Objective)
        workstations.add(new ComputerWorkstation("CENTRAL-DATA-CORE",
                ComputerWorkstation.WorkstationType.VAULT_MAINFRAME, 660, 840, 80, 54));
    }

    private void buildSecurityCameras() {
        // Camera 1: Reception lobby entrance coverage (pointing south-east)
        cameras.add(new SecurityCamera("CAM-01 [RECEPTION]", 100, 200, 45, 60, 180, 50));

        // Camera 2: Security checkpoint turnstile lane (pointing south-west)
        cameras.add(new SecurityCamera("CAM-02 [SECURITY]", 1200, 200, 135, 65, 200, 50));

        // Camera 3: Office central aisle (pointing east)
        cameras.add(new SecurityCamera("CAM-03 [OFFICE]", 90, 600, 0, 55, 210, 45));

        // Camera 4: Server Room Hot Aisle (pointing south)
        cameras.add(new SecurityCamera("CAM-04 [SERVERS]", 1250, 480, 90, 70, 220, 55));

        // Camera 5: Vault Mainframe Core perimeter (pointing south-west)
        cameras.add(new SecurityCamera("CAM-05 [VAULT]", 860, 760, 180, 65, 200, 50));

        for (SecurityCamera cam : cameras) {
            cam.setCameraStateListener(c -> {
                if (securityDirector != null) {
                    securityDirector.onCameraDisabled(c, guards);
                }
            });
        }
    }

    private void buildTerminals() {
        // Master Security Command Console in Security Checkpoint
        terminals.add(new SecurityTerminal("SEC-NET-ROUTER", 990, 246, 44, 34));
    }

    private void buildAlarms() {
        // Wall-mounted Facility Alarm Panels with Strobe Beacons
        AlarmPanel a1 = new AlarmPanel("ALARM-01 [SECURITY]", 844, 214, 14, 26);
        AlarmPanel a2 = new AlarmPanel("ALARM-02 [OFFICE]", 84, 510, 14, 26);
        alarms.add(a1);
        alarms.add(a2);

        for (AlarmPanel alarm : alarms) {
            alarm.setAlarmStateListener(new AlarmPanel.AlarmStateListener() {
                @Override
                public void onAlarmTriggered(AlarmPanel panel) {
                    if (securityDirector != null) {
                        securityDirector.onAlarmTriggered(panel, guards);
                    }
                }
                @Override
                public void onAlarmDisarmed(AlarmPanel panel) {
                    if (securityDirector != null) {
                        securityDirector.onAlarmDisarmed(panel);
                    }
                }
            });
        }
    }

    private void buildServerRacks() {
        // High-density Tier-IV Enterprise Data Core Server Racks
        serverRacks.add(new ServerRack("DATABASE-CLUSTER-A", 1180, 640, 60, 36));
        serverRacks.add(new ServerRack("VAULT-AIRGAP-ARRAY", 820, 840, 60, 36));
    }

    private void buildElevators() {
        // Transit / Extraction Elevator in Emergency Corridor
        elevators.add(new Elevator("ELEV-01 [EXTRACTION]", 1360, 790, 72, 54));
    }

    private void buildSafes() {
        // Reinforced Electronic Safe in Data Vault
        safes.add(new Safe("TITAN-CIPHER-SAFE", 500, 840, 48, 40));
    }

    public void update(double deltaTime) {
        update(deltaTime, null);
    }

    public void update(double deltaTime, com.cyberheist.entity.Player player) {
        if (securityDirector != null) {
            securityDirector.update(deltaTime, guards);
            if (securityDirector.getCurrentLevel() == com.cyberheist.security.SecurityLevel.LOCKDOWN) {
                for (Door door : doors) {
                    if (door.isLockdownProtected() && !door.isLockedDown()) {
                        door.engageLockdown(true);
                    }
                }
            } else {
                for (Door door : doors) {
                    if (door.isLockdownProtected() && door.isLockedDown()) {
                        door.engageLockdown(false);
                    }
                }
            }
        }

        for (Door door : doors) {
            door.update(deltaTime);
        }
        for (CoverObject cover : coverObjects) {
            cover.update(deltaTime);
        }
        for (SecurityCamera camera : cameras) {
            camera.update(deltaTime, player, collisionWorld, securityDirector, guards);
        }
        for (AlarmPanel alarm : alarms) {
            alarm.update(deltaTime);
        }
        for (ServerRack rack : serverRacks) {
            rack.update(deltaTime);
        }
        for (Elevator elevator : elevators) {
            elevator.update(deltaTime);
        }
    }

    public com.cyberheist.security.SecurityDirector getSecurityDirector() {
        return securityDirector;
    }

    public void setSecurityDirector(com.cyberheist.security.SecurityDirector securityDirector) {
        this.securityDirector = securityDirector;
    }

    public void render(Graphics2D g2d, int viewMinX, int viewMinY, int viewMaxX, int viewMaxY, BoundingBox playerBox) {
        // 1. Render floor surfaces
        tileMap.render(g2d, viewMinX, viewMinY, viewMaxX, viewMaxY);

        // 2. Render road & facility environmental markings
        renderEnvironmentalMarkings(g2d);

        // 3. Render surveillance camera vision cones (under furniture)
        for (SecurityCamera camera : cameras) {
            camera.render(g2d);
        }

        // 4. Render physical walls and barriers
        collisionWorld.render(g2d);

        // 5. Render furniture and cover objects
        for (CoverObject cover : coverObjects) {
            cover.render(g2d);
        }

        // 6. Render interactive workstations (Computers)
        for (ComputerWorkstation ws : workstations) {
            boolean near = playerBox != null && playerBox.intersects(ws.getInteractionTriggerZone());
            ws.render(g2d, near);
        }

        // 7. Render interactive security terminals
        for (SecurityTerminal term : terminals) {
            boolean near = playerBox != null && playerBox.intersects(term.getInteractionTriggerZone());
            term.render(g2d, near);
        }

        // 8. Render interactive alarm panels
        for (AlarmPanel alarm : alarms) {
            boolean near = playerBox != null && playerBox.intersects(alarm.getInteractionTriggerZone());
            alarm.render(g2d, near);
        }

        // 9. Render interactive server racks
        for (ServerRack rack : serverRacks) {
            boolean near = playerBox != null && playerBox.intersects(rack.getInteractionTriggerZone());
            rack.render(g2d, near);
        }

        // 10. Render interactive elevators
        for (Elevator elev : elevators) {
            boolean near = playerBox != null && playerBox.intersects(elev.getInteractionTriggerZone());
            elev.render(g2d, near);
        }

        // 11. Render interactive electronic safes
        for (Safe safe : safes) {
            boolean near = playerBox != null && playerBox.intersects(safe.getInteractionTriggerZone());
            safe.render(g2d, near);
        }

        // 12. Render interactive doors
        for (Door door : doors) {
            boolean near = playerBox != null && playerBox.intersects(door.getInteractionTriggerZone());
            door.render(g2d, near);
        }

        // 8. Render extraction zone marker at rear exit
        renderExtractionZone(g2d, playerBox);

        // 9. Render section name plaques
        renderSectionPlaques(g2d);
    }

    private void renderEnvironmentalMarkings(Graphics2D g2d) {
        // Street dashed road divider
        g2d.setColor(new Color(255, 255, 255, 70));
        g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{16.0f, 16.0f}, 0.0f));
        g2d.drawLine(0, 40, COLS * TILE_SIZE, 40);
        g2d.setStroke(new BasicStroke(1.0f));

        // Sidewalk curb edge
        g2d.setColor(new Color(0x55, 0x5D, 0x6A));
        g2d.fillRect(0, 80, COLS * TILE_SIZE, 4);

        // Exterior Bollards on sidewalk
        g2d.setColor(new Color(0x70, 0x7E, 0x90));
        for (int bx = 160; bx < COLS * TILE_SIZE - 160; bx += 140) {
            g2d.fillOval(bx - 4, 140, 8, 8);
            g2d.setColor(Color.BLACK);
            g2d.drawOval(bx - 4, 140, 8, 8);
            g2d.setColor(new Color(0x70, 0x7E, 0x90));
        }

        // Reception Entry Welcome Inlay
        g2d.setColor(new Color(0x18, 0x24, 0x34));
        g2d.fillRect(340, 220, 120, 40);
        g2d.setColor(new Color(0x35, 0x48, 0x60));
        g2d.drawRect(340, 220, 120, 40);
        g2d.setFont(GameConfig.FONT_CODE);
        g2d.setColor(GameConfig.COLOR_TEXT_MUTED);
        g2d.drawString("NOIDA DATA FACILITY", 346, 244);
    }

    private void renderExtractionZone(Graphics2D g2d, BoundingBox playerBox) {
        int ex = (int) Math.round(exitZone.getX());
        int ey = (int) Math.round(exitZone.getY());
        int ew = (int) Math.round(exitZone.getWidth());
        int eh = (int) Math.round(exitZone.getHeight());

        boolean playerInExit = playerBox != null && playerBox.intersects(exitZone);

        // Pulsing landing / extraction box
        Color exitColor = playerInExit ? GameConfig.COLOR_GREEN : new Color(0x00, 0xC8, 0x70, 80);
        g2d.setColor(exitColor);
        g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{8.0f, 8.0f}, 0.0f));
        g2d.drawRect(ex, ey, ew, eh);
        g2d.setStroke(new BasicStroke(1.0f));

        g2d.setColor(new Color(0x00, 0xC8, 0x70, playerInExit ? 40 : 20));
        g2d.fillRect(ex, ey, ew, eh);

        g2d.setFont(GameConfig.FONT_CODE);
        g2d.setColor(playerInExit ? GameConfig.COLOR_GREEN : GameConfig.COLOR_TEXT_BRIGHT);
        g2d.drawString("EMERGENCY EVAC ZONE", ex + 10, ey + 30);
        g2d.drawString(playerInExit ? "[OBJECTIVE READY]" : "[EXTRACTION POINT]", ex + 10, ey + 50);
    }

    private void renderSectionPlaques(Graphics2D g2d) {
        g2d.setFont(GameConfig.FONT_CODE);
        g2d.setColor(new Color(0x60, 0x75, 0x8E));

        // Area Labels subtle on wall crowns
        g2d.drawString("SEC 01 // EXTERIOR STREET", 120, 70);
        g2d.drawString("SEC 02 // RECEPTION LOBBY", 120, 215);
        g2d.drawString("SEC 03 // SECURITY CHECKPOINT", 880, 215);
        g2d.drawString("SEC 04 // CORPORATE WORKSTATIONS", 120, 495);
        g2d.drawString("SEC 05 // SERVER FARM (TIER IV)", 1000, 495);
        g2d.drawString("SEC 06 // RESTRICTED DATA VAULT", 420, 775);
        g2d.drawString("SEC 07 // EMERGENCY FIRE STAIRS", 1320, 775);
    }

    public CollisionWorld getCollisionWorld() { return collisionWorld; }
    public List<Interactable> getInteractables() { return interactables; }
    public List<Guard> getGuards() { return guards; }
    public List<CoverObject> getCoverObjects() { return coverObjects; }
    public List<Door> getDoors() { return doors; }
    public List<SecurityCamera> getCameras() { return cameras; }
    public List<ComputerWorkstation> getWorkstations() { return workstations; }
    public List<SecurityTerminal> getTerminals() { return terminals; }
    public List<AlarmPanel> getAlarms() { return alarms; }
    public List<ServerRack> getServerRacks() { return serverRacks; }
    public List<Elevator> getElevators() { return elevators; }
    public List<Safe> getSafes() { return safes; }
    public Point getSpawnPoint() { return spawnPoint; }
    public BoundingBox getExitZone() { return exitZone; }
    public int getWorldWidth() { return COLS * TILE_SIZE; }
    public int getWorldHeight() { return ROWS * TILE_SIZE; }
    public NavGrid getNavGrid() { return navGrid; }
    public AStarPathfinder getPathfinder() { return pathfinder; }
}
