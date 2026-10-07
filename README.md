<div align="center">

# 🕵️ CYBER HEIST

**A 2D top-down tactical espionage game built entirely in Java**

*Infiltrate. Deceive. Extract. Escape.*

[![Java](https://img.shields.io/badge/Java-21%2B-orange?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Build](https://img.shields.io/badge/Build-Maven-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![Tests](https://img.shields.io/badge/Tests-86%20Passing-brightgreen?logo=checkmarx&logoColor=white)](#running-automated-tests)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Windows-0078D6?logo=windows&logoColor=white)](https://github.com/anshuman2728/Cyber-Heist)

</div>

---

## Overview

Cyber Heist is a handcrafted tactical stealth game where every system is designed to be **reactive, meaningful, and interconnected**. Guards are not decorative NPCs — they observe, communicate, and adapt. Security cameras sweep for blind spots you can exploit. Alarms cascade into lockdowns that reshape your route. Every decision you make changes how the facility responds.

Built from scratch in **pure Java** with zero external game-engine dependencies. Every subsystem — the sensory AI, A\* pathfinding, audio synthesis, particle effects, save system, and hacking minigame — is written by hand.

---

## Table of Contents

- [Gameplay](#gameplay)
- [Reactive Security Architecture](#reactive-security-architecture)
- [Stealth & Sensory Simulation](#stealth--sensory-simulation)
- [Enemy AI & A\* Pathfinding](#enemy-ai--a-pathfinding)
- [World Interactions & Hacking](#world-interactions--hacking)
- [Mission 1: Ghost in the Server](#mission-1-ghost-in-the-server)
- [Combat & Gadgets](#combat--gadgets)
- [Progression System](#progression-system)
- [Audio & Visual Polish](#audio--visual-polish)
- [Save System & Persistence](#save-system--persistence)
- [Robustness & Edge-Case Protection](#robustness--edge-case-protection)
- [Controls](#controls)
- [Getting Started](#getting-started)
- [Running Automated Tests](#running-automated-tests)
- [Architecture](#architecture)

---

## Gameplay

Cyber Heist is built around **four distinct tactical approaches** — each producing a genuinely different game:

| Approach | Strategy | World Reaction |
|---|---|---|
| **Ghost** | Sneak through camera blind spots; avoid all contact | Facility stays at `NORMAL`; clean extraction |
| **Saboteur** | Hack cameras one-by-one to clear a path | Guards investigate offline junctions; security escalates to `SUSPICIOUS` |
| **Distractor** | Throw decoys to pull guards off their posts | Guards pursue noise sources; windows open for infiltration |
| **Provocateur** | Trip an alarm and escape through the alternate route | Blast doors seal the primary path; `LOCKDOWN` forces a reroute |
| **Ghost Hunter** | Neutralize guards silently and proceed | A discovered unconscious colleague triggers `ALERT` facility-wide |

---

## Reactive Security Architecture

The central innovation of Cyber Heist is a **living security ecosystem** — every action has a consequence, and consequences cascade.

### Facility Security States

```
NORMAL ──────────> SUSPICIOUS ──────────> ALERT ──────────> LOCKDOWN
  │                    │                     │                   │
Routine patrol    Guards on edge       Active pursuit     Blast doors seal
  │                    │                     │                   │
Cameras 45°/s    Cameras 63°/s        Cameras 81°/s      Cameras 99°/s
```

### Security State Multipliers

| State | Vision Range | Patrol Speed | Camera Sweep | Detection Rate | Consequence |
|---|---|---|---|---|---|
| `NORMAL` | `1.0×` | `100 px/s` | `45°/s` | `1.0×` | Standard operations |
| `SUSPICIOUS` | `1.15×` | `120 px/s` | `63°/s` | `1.35×` | Guards check unsecured doors and offline camera nodes |
| `ALERT` | `1.30×` | `140 px/s` | `81°/s` | `1.80×` | Guards coordinate search grids; radio chatter begins |
| `LOCKDOWN` | `1.40×` | `160 px/s` | `99°/s` | `2.50×` | Hydraulic blast doors seal; sirens activate |

### What Triggers Escalation

| Player Action | Immediate Response | Escalation |
|---|---|---|
| **1 camera hacked** | Nearby guard investigates the offline node | State unchanged |
| **2+ cameras hacked** | Security Director flags a pattern intrusion | `NORMAL` → `SUSPICIOUS` |
| **Guard discovers unconscious colleague** | Guard broadcasts alert; sector sweep begins | `*` → `ALERT` |
| **Alarm panel tripped** | All available guards converge on the panel | `*` → `LOCKDOWN` |
| **Open security door noticed** | Guard investigates, closes door, reports breach | `NORMAL` → `SUSPICIOUS` |
| **Player detected by camera** | Camera alerts Security Director with coordinates | Guard dispatch |
| **Alarm disarmed** | Security Director stands down siren response | `LOCKDOWN` → `ALERT` |
| **Master terminal overridden** | All cameras looped; security state cleared | Reset to `NORMAL` |

### Security Director

The `SecurityDirector` class acts as the facility's central command:
- Tracks the global threat level and manages timed decay back to safer states
- Dispatches the nearest available guard to any reported disturbance coordinate
- Maintains encrypted radio communication links between security entities
- Coordinates lockdown sequences and blast door engagement

---

## Stealth & Sensory Simulation

Guards have **no supernatural knowledge** of the player's position. Detection is physical, progressive, and decaying.

### Detection Model

```
 0%                  25%                   60%                  100%
 ├────────────────────┼─────────────────────┼─────────────────────┤
 UNAWARE           SUSPICIOUS           ALERT ZONE          CONFIRMED
 (Patrol & Watch)  (Turns toward)    (Closing in fast)   (Location Known)
```

**Detection builds when:**
- The guard's vision cone overlaps the player (faster at close range and center-cone)
- The player is sprinting (`1.6×` accumulation rate) vs. walking (`1.0×`) vs. crouching (`0.5×`)
- The area is brightly lit (`1.0×`) vs. a dim corridor or dark server room (`0.65×`)
- The player is in the open vs. behind cover (`0.35×` when behind desks, racks, or partitions)

**Detection decays when:**
- The player breaks line-of-sight behind a solid wall or closed door
- Decay rate: `24% per second`

### Acoustic Noise Propagation

Physical sound wavefronts propagate outward from the player's actions:

| Action | Noise Tier | Radius | Effect on Guards |
|---|---|---|---|
| Crouching / still | Silent | `0 px` | Completely undetectable |
| Hacking a terminal | Low | `90 px` | Faint electronic chirp |
| Walking | Low | `120 px` | Soft footsteps |
| Opening a door | Low | `150 px` | Mechanical latch sound |
| Running / sprinting | Medium | `280 px` | Rapid heavy footsteps |
| EMP discharge | Medium | `360 px` | Electrical pulse blast |
| Weapon fire | High | `620 px` | Facility-wide gunshot |

Guards localize sounds with ±25px perceptual error — they investigate *near* the origin, not the exact coordinate.

---

## Enemy AI & A\* Pathfinding

Guards run a full **state machine AI** backed by A\* pathfinding on a live navigation grid.

### Guard State Machine

```
         PATROL
            │  (hears noise / peripheral glimpse)
            ▼
        SUSPICIOUS
            │  (suspicion confirmed or sound localized)
            ▼
       INVESTIGATE
            │  (detection reaches 100%)
            ▼
          ALERT ──► [Radio broadcast to all facility guards]
            │  (0.3s reaction hesitation / weapon draw)
            ▼
          CHASE
            │  (line-of-sight broken)
            ▼
         SEARCH ──► [2–3 randomized probes near last-known position]
            │  (5.0s search timer expires)
            ▼
   RETURN_TO_PATROL ──► Resume assigned route
```

### Environmental Awareness (Always Active)

Guards continuously scan their environment and react without being told:

- **Unconscious colleague spotted** → transitions to `ALERT`, escalates facility security
- **Open security door noticed** → investigates, closes door, reports breach
- **Offline camera node nearby** → pathfinds to junction to check for tampering
- **Active alarm siren** → localizes the tripped panel and converges with other guards
- **Patrol speed and search duration** → scale dynamically with the current security level

### Imperfect Sensory Modeling

Guards are never omniscient:
- **Sound localization fuzz**: ±25px perceptual error on heard events
- **Lost visual contact**: guards rush to the *last-known position*, not the player's current location
- **Imperfect area search**: 2–3 randomized offset probes (80–140px radius) before conceding
- **Security radio bus**: a guard that escalates to `ALERT` broadcasts target coordinates; nearby guards enter `INVESTIGATE`

### A\* Pathfinding

| Property | Specification |
|---|---|
| Open set structure | `PriorityQueue` sorted by `f(n) = g(n) + h(n)` |
| Heuristic | Admissible Octile (`1.0` orthogonal, `√2 ≈ 1.414` diagonal, no corner-cutting) |
| Architecture | `Graph<N>` and `Heuristic` interfaces — decoupled from rendering |
| State resets | O(1) token-based reset; no 2D array re-allocation per query |
| Path recalculation throttle | `~0.35s / 3 Hz` during `CHASE` and `PATROL` |
| Dynamic obstacles | Doors update only their local bounding box cells on open/close |

---

## World Interactions & Hacking

Eight distinct interactive objects define the game's tactical vocabulary:

| Object | Hack Tier | Function |
|---|---|---|
| **Computer** (`ComputerWorkstation`) | Easy / Hard | Read logs, employee files; extract classified data |
| **Security Camera** (`SecurityCamera`) | Medium | Loop CCTV feed; eliminates vision cone and detection zone |
| **Door** (`Door`) | Medium / Hard | Bypass electronic security locks and reinforced blast doors |
| **Security Terminal** (`SecurityTerminal`) | Hard | Master override; loops all sector cameras simultaneously |
| **Alarm Panel** (`AlarmPanel`) | Medium | Sabotage wall-mounted siren panel to disarm emergency alerts |
| **Server Rack** (`ServerRack`) | Hard | Exfiltrate encrypted corporate files — **$250,000 credits** |
| **Elevator** (`Elevator`) | Medium | Bypass call panel; summon car and open shaft doors |
| **Safe** (`Safe`) | Very Hard | Crack crypto lockbox — **$500,000 credits** in bearer bonds |

### Network Node Hacking Minigame

Any secured device launches the **Network Node Intrusion Interface**:

```
╔══════════════════════════════════════════════════════════════╗
║  NETWORK NODE // INTRUSION INTERFACE                         ║
║  TARGET: CENTRAL-DATA-CORE // TIER-IV   Military Countermeasures ║
╠══════════════════════════════════════════════════════════════╣
║  Find the correct sequence.                                  ║
║                                                              ║
║  [ 04 ]  [ 17 ]  [ 82 ]  [ 39 ]                             ║
║                                                              ║
║  [1][ 04 ]  [2][ 17 ]  [3][ 82 ]  [4][ 39 ]  [5][ 5A ]     ║
║                                                              ║
║  Time: 08.4s           Security: ███████░░ [ 70% ]          ║
║  [Keys 1–8 or Click to select  |  ESC: Abort]               ║
╚══════════════════════════════════════════════════════════════╝
```

**Mechanics:**
- Select the correct node codes in exact sequential order
- A live countdown timer pressures the player
- A security trace meter builds continuously — incorrect inputs cause spike penalties (+20% per mistake)
- Trace at 100% locks out the session temporarily with an alarm chirp

**Difficulty Tiers:**

| Tier | Sequence | Pool | Timer | Trace Rate | Penalty | Targets |
|---|---|---|---|---|---|---|
| Easy | 3 nodes | 4 nodes | 12.0s | 4.5%/s | +10% | Reception PCs, Office workstations |
| Medium | 4 nodes | 5 nodes | 9.0s | 6.5%/s | +15% | Cameras, Elevators, Alarms |
| Hard | 5 nodes | 6 nodes | 7.5s | 9.0%/s | +20% | Terminals, Server Racks, Blast Doors |
| Very Hard | 6 nodes | 8 nodes | 6.0s | 13.0%/s | +25% | Black-ICE Safes, Mainframe Cores |

---

## Mission 1: Ghost in the Server

**Location:** Noida Corporate Data Center — a handcrafted, believable modern facility.

```
Enter facility  →  Reception  →  Security Checkpoint  →  Office Area
                                                               ↓
                                              Escape  ←  Server Room  ←  Vault
```

### Objective Sequence

1. Infiltrate the facility undetected
2. Navigate past the security checkpoint
3. Access a employee terminal and extract login credentials
4. Disable the camera network or circumvent it
5. Breach the server room and steal the encrypted data package
6. Reach the emergency exit and extract

### Radio Communications

Authentic, grounded earpiece dialogue between handler **Mira** and infiltrator **Kavin**:

```
[00:01] MIRA   // HANDLER     "You're late."
[00:04] KAVIN  // INFILTRATOR "Traffic on the expressway. Relax."
[00:07] MIRA   // HANDLER     "You weren't followed?"
[00:10] KAVIN  // INFILTRATOR "Clean. Front entrance in sight."
[00:13] MIRA   // HANDLER     "Cameras are live. Slip inside before the shift change."
```

Transmissions fire dynamically on room transitions, terminal overrides, server breaches, and guard alerts. No chatGPT-style robotic NPC dialogue.

---

## Combat & Gadgets

Cyber Heist enforces deliberate, non-lethal tactical play:

| Gadget | Key | Description |
|---|---|---|
| **Stun Device** | `Q` | High-voltage arc taser — 90–135px directional cone; stuns guard 5.0s; 6.0s recharge cooldown |
| **EMP Canister** | `1` | Deployable radial pulse — 220px shockwave; disables cameras 15.0s; stuns guards 6.0s |
| **Distraction Decoy** | `G` / Right-Click | Thrown sonic projectile — lures nearby guards to investigate the landing site via A\* |
| **Non-Lethal Takedown** | `F` | Silent sleeper hold — requires sneaking within 50px behind an unaware guard; awards +40 XP |

### Starting Inventory

| Item | Qty | Max | Function |
|---|---|---|---|
| EMP Device | ×2 | 4 (upgradable) | Radial electronics pulse |
| Lockpick | ×3 | 5 (upgradable) | `[L]` — silently bypass locked doors and safes |
| Trauma Medkit | ×1 | 3 (upgradable) | `[3]` — restore stamina to 100% |
| Data Drive | ×1 | 1 | `[4]` — stores stolen corporate records |

---

## Progression System

Press **`P`** or **`U`** to open the Operative Progression Matrix. Earn **1 Skill Point per 100 XP**:

| Source | XP Reward |
|---|---|
| Complete mission objective | +100 XP |
| Successful hack | +50 XP |
| Silent takedown | +40 XP |
| Lockpick bypass | +20 XP |

### Upgrade Tracks

**1. Hacking Mastery**
- Rank 1 — *Buffer Bypass*: +2.5s on all hack timers; trace builds 20% slower
- Rank 2 — *ICE Breaker*: First sequence node auto-matched; mistake penalty halved

**2. Stealth Camouflage**
- Rank 1 — *Ghost Protocol*: -35% footstep noise radius (walking and sprinting)
- Rank 2 — *Shadow Veil*: -30% detection accumulation rate; +50% faster decay while in cover

**3. Kinetic Conditioning**
- Rank 1 — *Athletic Conditioning*: +15% sprint velocity; +25 max stamina
- Rank 2 — *Adrenaline Surge*: +50% stamina recovery rate; -35% drain while sprinting

**4. Field Equipment**
- Rank 1 — *Deep Pockets*: +2 max capacity to all consumables; +40% distraction throw range
- Rank 2 — *Overclocked Coils*: Stun cooldown halved to 3.0s; +50% arc range; +60px EMP radius

---

## Audio & Visual Polish

Every interaction in Cyber Heist has deliberate audiovisual feedback — designed, not generated.

### Human-Tuned Movement
- **Walk**: `205 px/s` — deliberate, calculated, silent
- **Sprint**: `365 px/s` — explosive burst, generates noise
- **Friction**: `1950 px/s²` deceleration — zero slippery drift into laser cones
- **Procedural bob**: Stride oscillation while moving; idle chest breathing while still

### Procedural Audio (Zero External Assets)
All sound is synthesized in real-time using Java Sound PCM wave generation:
- Footstep cadence, stun arc discharge, EMP sub-bass rumble, sonic decoy ping
- Silent takedown thud, hacking blips and arpeggio success chord
- Continuous 58 Hz / 116 Hz server HVAC ambient drone

### Particle Systems
- Footstep dust, electric arc sparks, EMP shockwave rings
- Binary data glyph rain (`0`, `1`, `#`, `<>`) during hacking sessions
- Takedown radial dust cloud on knockouts

### Lighting & Post-Processing
- Tactical visor spotlight (radial gradient centered on player)
- Alarm strobe — red harmonic pulse tint during detection (`sin(t × 5)`)
- Cinematic edge vignette and optional CRT scanline filter

### Camera
- Trauma-squared screen shake (`shake = trauma²`); EMP injects `0.70`, stun injects `0.35`
- Directional 35px lookahead lead — clears sightlines into rooms

### UI
- Animated main menu with matrix particle background
- Tactical pause screen with cyberpunk glass overlay
- Terminal boot deployment screen with telemetry logs
- Live audio waveform display in radio comms HUD

---

## Save System & Persistence

Quick Save (`F5`) and Quick Load (`F9`) persist the complete heist state to `saves/quicksave.sav`.

**What is saved and fully restored:**

| Category | Fields Persisted |
|---|---|
| Operative | Position, velocity, facing angle, stamina, movement state |
| Inventory | EMP count, lockpick count, medkit count, data drive status |
| Progression | All 4 upgrade tracks, unlocked ranks, XP total |
| Mission | Active step, all completion flags, terminal override status |
| World | All 6 doors, 5 cameras, 5 workstations, alarms, servers, elevators, safes |
| Guards | Position, velocity, patrol index, AI state, detection meter, target coordinates |
| Security | Facility security level, threat decay timer |

---

## Robustness & Edge-Case Protection

| Protection | Implementation |
|---|---|
| **Anti-Stuck Depenetration** | 8-way radial resolver scans outward in 2–80px increments when the operative enters geometry |
| **Continuous Collision Detection** | High-velocity sprint sub-steps ray increments; prevents tunneling through thin walls |
| **Boundary Clamping** | Player coordinates hard-clamped to `[radius, mapBounds − radius]`; boundary escape impossible |
| **AI Deadlock Timeouts** | Guards failing to reach targets for >7.5s (`INVESTIGATE`), >6.0s (`CHASE`), or >8.5s (`RETURN`) auto-recover to patrol |
| **Stall Detection** | Path following detects <0.8px movement over 1.2s and advances the waypoint automatically |
| **Door Recovery** | Doors have dual bypass: lockpick fallback always available if hack fails |
| **Hacking Lockout** | Spam protection: trace spike per wrong input; 100% trace triggers temporary lockout, not state corruption |

---

## Controls

| Key / Action | Function |
|---|---|
| `W A S D` or Arrow Keys | 8-way directional movement |
| `Shift` / `Space` | Sprint — generates medium noise; drains stamina |
| `E` | Interact / Hack — works on all 8 interactable object types |
| `Q` | Stun Device — directional electric arc shock |
| `F` | Non-Lethal Takedown — must be sneaking close behind guard |
| `G` / Right-Click | Throw Distraction Decoy |
| `1` | Deploy EMP Canister |
| `2` / `L` | Use Lockpick — silently bypasses locked doors and safes |
| `3` | Use Trauma Medkit |
| `4` | Inspect Data Drive |
| `P` / `U` | Toggle Operative Progression Matrix |
| `1`–`8` / Mouse Click | Select Network Node (during hacking minigame) |
| `F3` | Toggle Engine Telemetry & Profiler (FPS, CPU, Heap, Pathfinding µs) |
| `F4` | Toggle AI & Stealth Debug Overlays (paths, states, LOS, cones, comms links) |
| `F5` | Quick Save |
| `F9` | Quick Load |
| `Esc` | Pause Menu / Close Upgrades / Abort Hack |

---

## Getting Started

### Prerequisites

- Java 21 or later ([Download OpenJDK](https://adoptium.net/))
- Maven (or use the included `mvnw.cmd` wrapper — no installation required)

### Build

```powershell
.\mvnw.cmd clean package
```

### Run

```powershell
# Option 1: Double-click launcher
.\run.bat

# Option 2: Command line
java -jar target\cyber-heist-0.1.0-SNAPSHOT.jar
```

---

## Running Automated Tests

```powershell
.\mvnw.cmd test
```

**86 / 86 tests passing — 0 failures, 0 errors**

| Test Suite | Coverage |
|---|---|
| `ReactiveSecurityArchitectureTest` | SecurityDirector state machine, camera LOS, disabled camera escalation, lockdown blast doors, guard environmental awareness |
| `StealthSystemTest` | Detection accumulation/decay, noise propagation, vision cone geometry |
| `AiPathfindingTest` | A\* correctness, heuristic admissibility, dynamic obstacle updates, throttled recalculation |
| `WorldInteractionsTest` | All 8 interactable object types, door states, camera hacking, alarm sabotage |
| `HackingMinigameTest` | All 4 difficulty tiers, trace mechanics, timeout/brute-force lockout |
| `MissionProgressionTest` | Step transitions, objective triggers, debrief overlay |
| `CombatAndInventoryTest` | Gadget mechanics, item consumption, stack management |
| `ProgressionSystemTest` | XP gain, SP calculation, all 4 upgrade track effects |
| `SaveSystemAndRobustnessTest` | Complete save → exit → restart → load roundtrip, anti-stuck, CCD, deadlock timeouts |
| `GamePolishAndFeelTest` | Movement calibration, camera trauma, particle lifecycle, audio manager |
| `LevelEnvironmentTest` | Level geometry, nav grid construction, cover object placement |
| `PlayerSystemTest` | Stamina, state transitions, collision, boundary clamping |

---

## Architecture

```
src/main/java/com/cyberheist/
├── core/           GameEngine, GameLoop, GameConfig, TelemetryService
├── entity/         Player, StaminaSystem, FacingDirection, PlayerState
├── ai/             GuardAI, GuardCommunicator
│   ├── pathfinding/ AStarPathfinder, NavGrid, NavNode, Graph, Heuristic
│   └── state/      GuardState
├── security/       SecurityDirector, SecurityLevel, SecurityEventListener
├── stealth/        StealthSystem, DetectionMeter, LineOfSight, VisionCone
│                   NoiseManager, NoiseEvent, NoiseType, Guard
├── world/          NoidaDataCenterLevel, Door, SecurityCamera, AlarmPanel
│                   ComputerWorkstation, ServerRack, SecurityTerminal
│                   Safe, Elevator, CoverObject, TileMap
├── hacking/        HackingSession, NetworkNode, HackingDifficulty, Hackable
├── mission/        MissionManager, RadioDialogueManager, DialogueLine
├── combat/         CombatManager, CombatEffect, DistractionProjectile
├── inventory/      Inventory, InventoryItem, ItemType
├── progression/    ProgressionManager, ProgressionTrack
├── scene/          SceneManager, EmptyPlayableScene, MainMenuScene
├── display/        Renderer, Camera2D, LightingRenderer, GameWindow
│                   HUD overlays: Hacking, Mission, Tactical, Progression
├── audio/          AudioManager, SoundEffect
├── particle/       ParticleSystem, Particle
├── collision/      CollisionWorld, BoundingBox, Collidable, Obstacle
├── save/           SaveManager, SaveData
├── animation/      AnimationController
├── input/          KeyboardInput, MouseInput
├── settings/       SettingsManager
└── util/           Logger
```

---

## Performance

The engine targets **60 FPS on a single thread** with strict per-frame budgets:

- **Rendering**: zero-allocation `BufferStrategy` double-buffering
- **AI scanning**: throttled to 4–5 Hz with distance-based early rejection (`> 220px`)
- **Pathfinding**: O(1) state resets; throttled at 3 Hz during active pursuit
- **Audio**: streamed PCM synthesis on a dedicated daemon thread
- **Save/Load**: full state serialization in < 10ms

Press `F3` in-game to view live telemetry:
```
FPS: 60  |  UPS: 60  |  Delta: 16.6 ms
Update: 1.42 ms  |  Render: 3.18 ms  |  CPU: 16 Cores
JVM Heap: 48 MB / 256 MB  |  GC Runs: 2
Entities: 52  |  Guards: 3  |  Particles: 18
A* Pathfinding: 3 searches/sec  |  Avg Search: 142 µs
```

---

<div align="center">

**Built by Anshuman Singh**

[GitHub](https://github.com/anshuman2728/Cyber-Heist) · [Report a Bug](https://github.com/anshuman2728/Cyber-Heist/issues)

</div>
