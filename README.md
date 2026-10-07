# Cyber Heist

A 2D cyberpunk top-down tactical espionage game built in Java.

---

## Stealth System & Sensory Simulation

Cyber Heist features a realistic, non-binary sensory simulation where guards do not have supernatural knowledge of player coordinates. Stealth calculations are completely independent from the rendering subsystem.

### Detection Model

```text
0% ───────────────────────── 25% ──────────────────────── 60% ──────────────────────── 100%
      UNAWARE / SAFE               SUSPICIOUS               CONFIRMED DETECTION
   (Patrol & Watch)            (Turns toward stimulus)     (Alert / Location Known)
```

* **Progressive Accumulation**:
  * **Distance & Angle**: Sight at point-blank range and down the center line of the vision cone builds detection rapidly; peripheral vision or distant observation builds slowly.
  * **Movement Multiplier**: Sprinting/running increases visibility rate (`1.6x`), walking is baseline (`1.0x`), crouching/stationary is reduced (`0.5x`).
  * **Cover Multiplier**: Taking cover behind desks, partitions, or server racks reduces visibility rate by **65%** (`0.35x`).
  * **Ambient Lighting Multiplier**: Dark server rooms (`0.65x`) and dim corridors reduce detection speed compared to brightly lit reception areas (`1.0x`).
* **Progressive Decay**:
  * Breaking line-of-sight behind a solid wall or closed door causes the detection meter to decay smoothly at `24% / sec`.

---

### Acoustic Noise Propagation

Different actions emit physical acoustic wavefronts that travel through the environment:

| Action | Noise Tier | Wave Radius | Alert Impact |
|---|---|---|---|
| **Crouch / Still** | None | `0 px` | Completely silent |
| **Hacking Terminal** | Low | `90 px` | Faint electronic chirp |
| **Walking** | Low | `120 px` | Quiet soft footsteps |
| **Door Opening** | Low | `150 px` | Mechanical slider/latch |
| **Running / Sprint** | Medium | `280 px` | Rapid heavy footsteps |
| **EMP Discharge** | Medium | `360 px` | Electrical pulse blast |
| **Weapon Gunfire** | High | `620 px` | Facility-wide gunshot |

Guards within hearing range investigate sound origins by rotating toward the noise without instantly knowing the player's exact coordinates.

---

### Toggleable Debug Tools (`F4`)

Press **`F4`** during gameplay to toggle real-time stealth visualization overlays:
* **Vision Cones**: Rendered with dynamic state tinting (Green = Unaware, Amber = Suspicious, Red = Detected).
* **Line-of-Sight (LOS) Raycasts**: Green line indicating uninterrupted visual contact; Red line indicating beam obstruction with exact collision impact dot on the blocking wall.
* **Acoustic Noise Wavefronts**: Real-time pulsating expanding rings showing current wave expansion radius, dissipation lifetime, and action label.
* **Overhead Detection Meters**: Mini-progress bars and alert markers (`?` / `!`) over individual guards.

---

---

## Enemy AI & A* Pathfinding Architecture

Cyber Heist features a decoupled, state-based enemy artificial intelligence engine powered by $O(1)$-reset A* search, admissible Octile heuristics, PriorityQueue sorting, and realistic imperfect sensory modeling.

### Guard State Machine

```text
       PATROL
         │ (auditory disturbance / peripheral glimpse)
         ▼
     SUSPICIOUS
         │ (continued suspicion or sound localization)
         ▼
    INVESTIGATE
         │ (line-of-sight confirmed, detection reaches 100%)
         ▼
       ALERT ──► [Radio broadcast to facility colleagues]
         │ (0.3s weapon draw / reaction hesitation)
         ▼
       CHASE
         │ (line-of-sight broken behind wall / cover)
         ▼
      SEARCH ──► [Inspects 2-3 randomized points near last-known position]
         │ (5.0s search duration expires without contact)
         ▼
RETURN_TO_PATROL ──► Resumes assigned patrol route
```

### Imperfect Sensory Modeling (Never Omniscient)

* **Auditory Localization Fuzz**: Guards localizing heard acoustic events (footsteps, opening doors, gunfire) suffer from $\pm 25\text{px}$ perceptual error rather than knowing exact coordinate values.
* **Loss of Visual Contact**: When a player runs behind cover or a solid wall, the guard **does not know where the player went**. They rush directly to the player's **last-known position**.
* **Imperfect Area Search**: Upon reaching the last-known position, guards enter `SEARCH` mode, probing 2 to 3 randomized offset locations within 80–140px and oscillating their facing direction before conceding and returning to patrol.
* **Security Radio Communication**: When a guard escalates to `ALERT`, they broadcast their target coordinates over the security radio bus, prompting nearby guards to enter `INVESTIGATE` mode and converge on the scene.

### A* Pathfinding Specifications

* **PriorityQueue Open Set**: Node evaluation prioritized by $f(n) = g(n) + h(n)$, with tie-breaking toward lower heuristic cost.
* **Admissible Octile Heuristic**: Incorporates 8-way movement costs ($1.0$ for orthogonal, $\sqrt{2} \approx 1.414$ for diagonal steps) with corner-cutting prevention.
* **Decoupled Architecture**: `Graph<N>` and `Heuristic` interfaces keep pathfinding algorithms completely independent of rendering and concrete data structures.
* **$O(1)$ State Resets**: Search tokens avoid re-allocating or zeroing 2D arrays across pathfinding queries.
* **Throttled Path Recalculation**: In `CHASE` and `PATROL`, path recalculation is throttled to configurable intervals (`0.35s` / $\approx 3\text{Hz}$), eliminating wasteful per-frame recalculations and maintaining 60 FPS.
* **Dynamic Obstacle Handling**: When doors are opened or closed, `updateWalkabilityRegion` recalculates only the local bounding box cells in $O(1)$ time, dynamically opening or closing navigable corridors.

---

### AI Debug Overlays (`F4`)

Press **`F4`** during gameplay to toggle full AI and stealth diagnostic overlays:
* **Current State**: Overhead badges (`[PATROL]`, `[SUSPICIOUS]`, `[INVESTIGATE]`, `[ALERT]`, `[CHASE]`, `[SEARCH]`, `[RETURN_TO_PATROL]`).
* **Detection Percentage**: Real-time value display (`Det: 0%` to `Det: 100%`).
* **Active A* Path**: Cyan dashed waypoint trajectory lines with node dots.
* **Target Marker**: Amber crosshair marking the current sound or investigation target.
* **Last Known Player Position**: Red crosshair and label at the coordinates where visual contact was severed.
* **Line-of-Sight Ray**: Green when clear, red with impact marker when obstructed.

---

---

## World Interactions & Cyberpunk Identity

Cyber Heist features 8 distinct, interactive physical world elements that define its tactical espionage identity. Breaching secured devices activates the Network Node intrusion minigame.

| Object | Difficulty | Function & Gameplay Impact |
|---|---|---|
| **Computer** (`ComputerWorkstation`) | `EASY` / `HARD` | Read visitor logs, employee dossiers, and extract classified vault mainframe data. |
| **Camera** (`SecurityCamera`) | `MEDIUM` | Loop CCTV video feeds; permanently disables surveillance vision cones and detection zones. |
| **Door** (`Door`) | `MEDIUM` / `HARD` | Bypass electronic security locks on turnstile gates and reinforced heavy blast doors. |
| **Terminal** (`SecurityTerminal`) | `HARD` | Master security override station; hacking it loops all sector surveillance cameras simultaneously. |
| **Alarm** (`AlarmPanel`) | `MEDIUM` | Wall-mounted siren panel with strobe beacons; sabotage it to disarm sector emergency alerts. |
| **Server** (`ServerRack`) | `HARD` | High-density Tier-IV data core; exfiltrates encrypted corporate files worth **$250,000 credits**. |
| **Elevator** (`Elevator`) | `MEDIUM` | Bypass elevator call panels; summons transit car and retracts shaft doors for evacuation. |
| **Safe** (`Safe`) | `VERY_HARD` | Heavy crypto lockbox; crack the Tier-IV cipher to loot **$500,000 credits** in bearer bonds. |

---

## Network Node Hacking Minigame

Interacting with any locked or secured device launches the **Network Node Intrusion Interface**: a high-fidelity cyberpunk CRT terminal complete with phosphor glow and scanlines.

```text
========================================================================================
  NETWORK NODE // INTRUSION INTERFACE
  TARGET: CENTRAL-DATA-CORE // TIER-IV DATA CORE  |  Military Countermeasures
----------------------------------------------------------------------------------------
  Find the correct sequence.
  [ 04 ]  [ 17 ]  [ 82 ]  [ 39 ]

  [1] [ 04 ]    [2] [ 17 ]    [3] [ 82 ]    [4] [ 39 ]    [5] [ 5A ]    [6] [ F1 ]

  Time: 08.4s                  Security: ███████░░ [ 70%]
  [Keys 1-8 or Click to Select | ESC: Abort]
========================================================================================
```

### Hacking Mechanics

* **Node Sequence Buffer**: Locate and select the correct node codes in exact sequential order.
* **Live Countdown Timer**: Real-time clock (`Time: 08.4s`). If the countdown expires before completion, the connection times out (`FAILED_TIMEOUT`).
* **Security Countermeasure Trace**: Dynamic trace meter (`Security: ███████░░`). Trace continuously builds over time. Selecting an incorrect node causes a trace spike penalty and resets the sequence progress. If trace reaches 100%, the intrusion is detected (`FAILED_TRACED`).
* **Dual Control Schemes**: Select nodes instantly using keyboard number hotkeys **`[1]`** through **`[8]`**, or click directly on the interactive visual node buttons using the mouse.
* **Hacking Difficulty Tiers**:

| Tier | Sequence Length | Node Pool | Time Limit | Trace Rate | Mistake Penalty | Typical Targets |
|---|---|---|---|---|---|---|
| **EASY** | 3 nodes | 4 nodes | 12.0s | 4.5% / sec | +10% trace | Reception Workstations, Office PCs |
| **MEDIUM** | 4 nodes | 5 nodes | 9.0s | 6.5% / sec | +15% trace | Security Cameras, Elevators, Alarms |
| **HARD** | 5 nodes | 6 nodes | 7.5s | 9.0% / sec | +20% trace | Command Terminals, Server Racks, Blast Doors |
| **VERY_HARD** | 6 nodes | 8 nodes | 6.0s | 13.0% / sec | +25% trace | Black-ICE Data Safes, Mainframe Cores |

---

## Mission 1: "Ghost in the Server"

### Objective Progression Flow
```text
Enter facility
      ↓
Avoid security
      ↓
Access terminal
      ↓
Disable camera network
      ↓
Reach server room
      ↓
Steal encrypted data
      ↓
Escape
```

### Human Storytelling & Authentic Radio Comms
Cyber Heist eschews robotic, formal AI tropes (e.g., *"Greetings. I am pleased to see you."*) in favor of grounded, succinct, tactical earpiece communication between handler **Mira** and infiltrator **Kavin**:

```text
[00:01] MIRA // HANDLER: "You're late."
[00:04] KAVIN // INFILTRATOR: "Traffic on the expressway. Relax."
[00:07] MIRA // HANDLER: "You weren't followed?"
[00:10] KAVIN // INFILTRATOR: "Clean. Front entrance in sight."
[00:13] MIRA // HANDLER: "Cameras are live. Slip inside before the shift change."
```

* **Contextual Triggers**: Dynamic radio transmissions fire upon room transitions (Reception, Security Checkpoint, Server Room, Rear Fire Exit), terminal overrides, server breaches, and guard alerts.
* **Animated Tactical Earpiece HUD**: Top-center radio transceiver display with live animated audio frequency waveforms (`|||||||||`), speaker identity, role tags, and typewriter dialogue feed.
* **Objective Checklist Card**: Upper-right tracking card with real-time step checkmarks (`[✓]`, `[▶]`, `[ ]`) and flashing active marker.
* **Debrief Overlay**: Clean cybernetic post-mission debriefing card confirming ghost status and extraction upon reaching the emergency fire exit.

---

## Tactical Non-Lethal Combat & Gadgets

Cyber Heist enforces deliberate, limited tactical combat rather than shooter mechanics:

* **Stun Device (`[Q]`)**: Directional high-voltage arc taser. Discharges an electrical shock arc in the player's facing direction (90px - 135px cone). Stuns targeted guards for 5.0s, halts vision and pathfinding, and triggers battery recharge cooldown (base 6.0s, reducible to 3.0s).
* **EMP Canister (`[1]`)**: Deployable electromagnetic pulse device (consumed from inventory). Detonates a 220px (up to 280px) radial shockwave that overloads security cameras for 15.0s and stuns all nearby guard implants for 6.0s. Emits acoustic medium noise.
* **Throwable Distraction (`[G]` / Right-Click)**: Sonic decoy projectile hurled to a target coordinate. Upon landing, emits an acoustic sound wave that lures nearby patrolling guards to investigate the landing site via A* pathfinding.
* **Non-Lethal Takedown (`[F]`)**: Silent melee sleeper hold executable when sneaking close behind unaware or suspicious guards (within 50px, >75° angle behind guard's facing). Permanently neutralizes the guard into an `UNCONSCIOUS` state without sound, awarding +40 XP.

---

## Tactical Inventory Loadout

Managed via the on-screen tactical quickbar:

| Item | Starting Qty | Max Cap | Function |
|---|---|---|---|
| **EMP Device** | **× 2** | 2 (Upgradable to 4) | Radial electronics pulse (disables cameras & stuns guards) |
| **Lockpick** | **× 3** | 3 (Upgradable to 5) | `[2]` or `[L]`: Silently unlocks locked doors and safes without hacking |
| **Trauma Medkit** | **× 1** | 1 (Upgradable to 3) | `[3]`: Emergency stimulant; instantly restores stamina to 100% and clears fatigue |
| **Data Drive** | **× 1** | 1 | `[4]`: Military-grade encrypted flash drive; stores stolen corporate records ($250k) |

---

## Operative Progression Matrix (`[P]`)

Press **`[P]`** or **`[U]`** during gameplay to toggle the cybernetic augmentation matrix. Gain **1 Skill Point (SP)** every **100 XP** earned from completing mission objectives (+100 XP), successful hacks (+50 XP), stealth takedowns (+40 XP), or lockpicking (+20 XP):

### 1. Hacking Mastery
* **Rank 1 - Buffer Bypass**: +2.5s hack time limit, 20% slower security countermeasure trace build rate.
* **Rank 2 - ICE Breaker**: Automatically matches and solves the first node in every sequence; sequence mistake penalty halved by 50%.

### 2. Stealth Camouflage
* **Rank 1 - Ghost Protocol**: -35% footstep sound generation radius (sprinting and walking are drastically quieter).
* **Rank 2 - Shadow Veil**: -30% guard detection accumulation rate; +50% faster detection decay when hiding in cover and shadows.

### 3. Kinetic Conditioning
* **Rank 1 - Athletic Conditioning**: +15% sprint velocity (~437 px/s), +25 maximum stamina capacity.
* **Rank 2 - Adrenaline Surge**: +50% faster stamina aerobic recovery rate, -35% stamina drain while sprinting.

### 4. Field Equipment
* **Rank 1 - Deep Pockets**: +2 maximum capacity to all consumable items (EMP cap: 4, Lockpick cap: 5), +40% distraction throw range.
* **Rank 2 - Overclocked Coils**: Stun device battery cooldown halved to 3.0s, +50% shock arc range, +60px EMP blast radius.

---

---

## Game Polish, Feel & Audiovisual Atmosphere

Cyber Heist is meticulously hand-tuned around the principle that **a great game feels designed rather than generated**. Every interaction delivers audiovisual feedback and tactile weight:

### 1. Human Game Designer Movement Calibration
* **Walk Velocity**: Tuned to `205.0 px/s` (~3.4 tiles/s) for deliberate, calculated stealth infiltration rather than floaty sliding.
* **Sprint Dash**: Tuned to `365.0 px/s` (~6.1 tiles/s) for explosive corridor dashes between guard vision sweeps.
* **Stopping Friction**: Tuned to `1950.0 px/s²` deceleration. Releasing movement keys halts the operative immediately without slippery drift into security laser cones.
* **Procedural Animation Bob**: Subtle stride oscillation bob (`strideCycle`) while walking/sprinting and smooth chest breathing expansion (`idlePulse`) when stationary.
* **Floating Interaction Badges**: Responsive pulsating `[E] <ACTION>` badges appear directly over interactable targets with spring bounce easing.

### 2. Procedural Audio Synthesis (Zero Assets)
Built entirely using Java Sound 16-bit PCM wave synthesis without external sound files or third-party dependencies:
* **Footstep Cadence**: Soft triangle frequency pulses on stride impact (differentiated walk vs. sprint).
* **High-Voltage Arc Zap**: Stun gun electric discharge sweeps.
* **Electromagnetic Pulse (EMP)**: Deep sub-harmonic bass rumble blast.
* **Acoustic Decoy**: Crisp resonant bell ping at landing site.
* **Silent Takedown**: Muffled physical impact thud.
* **Network Node Hacking**: Mechanical blips, failure buzzers, and 3-note ascending arpeggio on breach success.
* **Continuous Server HVAC Drone**: Generates low-frequency 58 Hz/116 Hz ambient hum blended with pink noise airflow.

### 3. Dynamic Particle Systems
High-performance 2D particle pool supporting alpha fade, drag physics, and decay:
* **Footstep Ground Dust**: Directional faint dust puffs scattered behind operative footsteps.
* **Electric Arc Sparks**: High-speed neon cyan and yellow sparks radiating upon stun gun contact.
* **EMP Blast Shockwaves**: Expanding cyan particle shock rings disabling electronics.
* **Matrix Data Bits**: Binary `[0, 1, #, <>]` data glyphs floating upward when hacking or extracting records.
* **Takedown Dust**: Radial cloud puff marking non-lethal knockouts.

### 4. Atmospheric Lighting & Post-Processing
* **Tactical Visor Spotlight**: Radial gradient cutout centered on the player illuminating the surrounding workspace while dimming distant areas.
* **Red Facility Alarm Strobe**: Harmonic red pulsating alert tint (`Math.sin(t * 5)`) when guards confirm player detection.
* **Cinematic Screen Vignette**: Radial edge darkening focusing attention on the tactical center.
* **CRT Scanline Rasterizer**: Optional vintage terminal monitor scanline filter.

### 5. Camera Trauma & Smooth Lookahead
* **Trauma-Squared Screen Shake**: Screen shake powered by quadratic trauma (`shakePower = trauma²`). Trauma decays naturally at `1.8 / s`. EMP detonations inject `0.70` trauma; stun shots inject `0.35` trauma.
* **Directional Lookahead Lead**: Automatically offsets the camera 35 px forward in the direction the player is facing, providing clear sightlines into rooms.

### 6. Menus, Settings, Pause & Loading Screen
* **Animated Main Menu**: Matrix particle background, blueprint grid, mission launcher, and "CONTINUE HEIST" archive loader.
* **Tactical Pause Screen (`ESC`)**: Cyberpunk glass overlay with Resume, Save Heist [F5], Load Heist [F9], Restart, Settings, and Return to Menu.
* **Live Tactical Settings Overlay**: Real-time adjustment of Master Volume, SFX Volume, Ambient Drone, Screen Shake, Scanlines, and Camera Smoothing.
* **Terminal Boot Sequence**: Animated mission deployment loading screen with telemetry logs, progress bar, and fast-forward infiltration prompt.

---

## Performance Telemetry & Profiler (`F3`)

Press **`F3`** at any time to toggle the real-time engineering telemetry profiler HUD:

```text
┌─── ENGINE TELEMETRY & PROFILER [F3] ───────────────────────────┐
│ FPS: 60  |  UPS: 60  |  Delta: 16.6 ms                         │
│ Update: 1.42 ms  |  Render: 3.18 ms  |  CPU: 16 Cores          │
│ JVM Heap: 48 MB / 256 MB (Max: 4096 MB)  |  GC Runs: 2         │
│ Entities: 52  |  Guards: 3  |  Particles: 18                   │
│ A* Pathfinding: 3 searches/sec  |  Avg Search: 142 μs          │
└────────────────────────────────────────────────────────────────┘
```

* **Zero-Allocation Sampling**: Telemetry metrics sample timestamps and JVM runtime statistics with zero garbage collection pressure.
* **Microsecond Pathfinding Profiling**: Moving average of A* search execution latency in microseconds ($\mu\text{s}$) alongside active search throughput per second.
* **Thread Diagnostics**: Update pass duration and render frame time tracked continuously.

---

## State Persistence & Save System (`F5` / `F9`)

Cyber Heist features a zero-dependency, deep serialization state persistence system saving complete mission snapshots to `saves/quicksave.sav`.

### Save & Load Features
* **F5 (Quick Save)**: Instantly snapshots the entire operative session to disk with on-screen HUD tactical toast confirmation.
* **F9 (Quick Load)**: Restores the archived heist snapshot, instantly restoring entities, camera focus, and mission state.
* **Pause Menu & Main Menu Integration**: Save and load directly via the pause menu, or resume past heists using the "CONTINUE HEIST // LOAD ARCHIVE" option from the main menu.
* **Complete State Restoration**:
  * **Operative**: Position $(x, y)$, velocity, facing angle, stamina, and state machine.
  * **Inventory**: All items (EMP, Lockpicks, Medkits, Data Drive) with exact count tallies.
  * **Progression Matrix**: Hacking mastery, stealth sound dampening, speed tiers, unlocked EMP capacity.
  * **Mission State**: Current active objective (1 through 6), all completed flags, terminal overrides, data stolen status.
  * **World Interactables**: Exact operational state of all 6 doors, 5 workstations, 5 security cameras, alarms, servers, elevators, and safes.
  * **Guard AI**: Position, velocity, facing angle, patrol path indexes, suspicion/detection meter values, active AI states (`PATROL`, `SUSPICIOUS`, `INVESTIGATE`, `ALERT`, `CHASE`, `SEARCH`, `RETURN_TO_PATROL`), and target coordinates.

---

## Gameplay Robustness & Edge-Case Protection

Built to eliminate student project edge cases:

1. **Anti-Stuck Depenetration**:
   * If an operative is ever forced inside geometry due to sudden dynamic obstacles or level changes, an 8-way radial collision resolver scans expanding offsets (`2px` to `80px`) and pushes the operative to the nearest valid walkable tile automatically.
2. **Continuous Collision Detection (CCD)**:
   * High-velocity movements (such as sprint bursts or future knockbacks) sub-step ray increments to prevent high-speed tunneling through thin walls.
3. **Hard Map Boundary Clamping**:
   * Operative coordinates are strictly clamped within the world boundaries (`[playerRadius, mapBounds - playerRadius]`), rendering boundary escape impossible.
4. **AI State Deadlock Timeouts**:
   * Guards cannot become permanently confused. If a target location becomes unreachable or unmoving during `INVESTIGATE` (7.5s limit), `CHASE` without line of sight (6.0s limit), or `RETURN_TO_PATROL` (8.5s limit), guards trigger internal watchdog timers and fail gracefully back into patrol routines.
   * Guard path following detects movement stalls (`< 0.8px` over 1.2s) and automatically advances waypoints or recalibrates.
5. **No Permanently Unusable Doors**:
   * Doors feature reversible open/close states and dual bypass options (lockpick item fallback + hacking minigame). If a hack fails, the door remains interactive and can be re-attempted or lockpicked.
6. **Hacking Spam & Brute-Force Lockout**:
   * Hacking sessions enforce security trace spikes upon failed node inputs (+20% trace per incorrect sequence input). Repeatedly spamming incorrect combinations trips 100% trace and temporarily locks out the session with an acoustic alarm chirp rather than corrupting state.

---

## Reactive Security Architecture & Security Director

Cyber Heist replaces decorative NPCs with an interconnected, reactive security ecosystem where security cameras, patrolling guards, alarms, access doors, and the central facility **Security Director** dynamically influence one another.

### Facility Security Levels

```text
NORMAL ──────────────> SUSPICIOUS ──────────────> ALERT ──────────────> LOCKDOWN
(Baseline Patrol)   (Guards on Edge)       (Active Pursuit)      (Blast Doors Sealed)
```

| Security Level | Guard Vision | Guard Patrol Speed | Camera Sweep Speed | Camera Detection Rate | Facility Status |
|---|---|---|---|---|---|
| **NORMAL** | `1.0x` | `1.0x` (`100 px/s`) | `1.0x` (`45°/s`) | `1.0x` | Standard facility operations |
| **SUSPICIOUS** | `1.15x` | `1.2x` (`120 px/s`) | `1.4x` (`63°/s`) | `1.35x` | Guards check doors, investigate offline nodes |
| **ALERT** | `1.30x` | `1.4x` (`140 px/s`) | `1.8x` (`81°/s`) | `1.80x` | Guards coordinate search grids; radio chatter |
| **LOCKDOWN** | `1.40x` | `1.6x` (`160 px/s`) | `2.2x` (`99°/s`) | `2.50x` | Heavy blast doors slam shut; sirens blare |

### Reactive World Interactions

1. **Security Cameras**:
   * Rotate dynamically with speeds determined by the security level.
   * Conduct line-of-sight raycasts against walls and doors; obstructed views create tactical blind spots.
   * Accumulate progressive suspicion; upon reaching 100% detection, instantly alert the Security Director.
   * If hacked or disabled, nearby guards notice the offline junction feed and investigate the terminal.
   * If 2 or more cameras are disabled, the Security Director escalates the facility state to `SUSPICIOUS`.
2. **Intelligent Guard Reactions**:
   * **Unconscious Colleague Detection**: Guards discovering a neutralized teammate escalate security to `ALERT` and sweep the sector.
   * **Unsecured Doors**: Guards noticing open security doors investigate the area, close the door, and elevate suspicion.
   * **Siren Localization**: Active alarm sirens prompt all available guards to converge on the tripped panel.
   * **Altered Patrol Cadence**: Search durations and patrol velocities scale dynamically with facility threat levels.
3. **Lockdown Blast Doors**:
   * Primary corridors (e.g. `Office Access Door`, `Server Room Air-Lock`) are equipped with hydraulic blast doors.
   * When `LOCKDOWN` triggers, blast doors seal shut with hazard warning markings, cutting off primary pathways and forcing operatives to use alternative routes (ventilation shafts, secondary offices, or heavy security terminal overrides).
4. **Active Comms & Radio Visualization**:
   * Security Director and field units establish encrypted comm links visualized via pulsing radio rays in debug mode (`F4`).
   * Real-time HUD badge displays the current facility threat state (`NORMAL`, `SUSPICIOUS`, `ALERT`, `LOCKDOWN`).

---

## Controls

| Key / Action | Function |
|---|---|
| **W, A, S, D** or **Arrows** | Smooth 8-way directional movement (tuned stealth cadence) |
| **Shift** or **Space** | Sprint (generates medium noise; consumes stamina) |
| **E** | **Interact / Hack** (Computer, Camera, Door, Terminal, Alarm, Server, Elevator, Safe) |
| **Q** | **Stun Device** (directional electric arc shock; triggers screen shake & sparks) |
| **F** | **Non-Lethal Takedown** (when sneaking behind guards) |
| **G** or **Right-Click** | **Throw Distraction Decoy** (lures guards with acoustic wave & audio ping) |
| **1** | **Deploy EMP Canister** (radial camera & guard overload with sub-bass blast) |
| **2** or **L** | **Use Lockpick** (silently bypass locked doors & safes) |
| **3** | **Use Trauma Medkit** (restore stamina to 100% & clear fatigue) |
| **4** | **Inspect Data Drive** (payload status) |
| **P** or **U** | **Toggle Operative Progression Matrix** (upgrade skills) |
| **1 - 8** or **Mouse Click** | **Select Network Node** in hacking minigame |
| **F3** | **Toggle Engine Telemetry & Profiler** (FPS, CPU, Heap, Entities, Pathfinding $\mu\text{s}$) |
| **F4** | **Toggle AI & Stealth Debug Overlays** (Paths, States, Targets, LOS, Cones, Comms Links) |
| **F5** | **Quick Save Heist** (archives session snapshot to disk) |
| **F9** | **Quick Load Heist** (restores archived session snapshot) |
| **Esc** | **Tactical Pause Menu** / Close Upgrades / Abort Hack |

---

## Running the Game

### Option 1: Double-Click Launcher
Double-click [run.bat](file:///c:/Users/anshu/Documents/Game/Cyber%20Heist/run.bat) in File Explorer.

### Option 2: From Terminal
```powershell
.\run.bat
```
*(or `java -jar target\cyber-heist-0.1.0-SNAPSHOT.jar`)*

---

## Running Automated Tests
```powershell
.\mvnw.cmd test
```
All **86 automated unit and integration test suites** pass with 0 failures:
* **Reactive Security Architecture**: Security Director state machine, camera LOS detection, disabled camera escalation, distraction audio lures, lockdown blast door sealing, and guard environmental awareness (unconscious colleague / unsecured doors).
* **Settings & Audio**: SettingsManager, Procedural AudioManager, Camera2D trauma shake.
* **VFX & Combat**: ParticleSystem, Tactical Combat gadgets, Inventory item stack management.
* **Progression & Narrative**: Operative Progression tracks, Mission 1 progression, radio dialogue manager.
* **Minigames & World**: Network Node hacking mechanics, 8 interactive world entity types, security cameras.
* **Sensory Simulation & AI**: Stealth detection, acoustic wave propagation, A* pathfinding, NavGrid, guard state machines.
* **Robustness & Persistence**: Map boundary clamping, Continuous Collision Detection substepping, anti-stuck depenetration, guard deadlock timeouts, door usability, hacking brute-force lockout, TelemetryService metrics, and full disk Save $\rightarrow$ Exit $\rightarrow$ Restart $\rightarrow$ Load roundtrip restoration.

