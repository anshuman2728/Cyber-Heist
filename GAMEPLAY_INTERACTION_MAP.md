# Cyber Heist: Gameplay Interaction Map

## 1. System Topology & Information Flow

```
                         ┌──────────────────────────────────────────────────────────┐
                         │                      PLAYER ACTIONS                      │
                         └──────┬───────────────┬───────────────────┬───────────────┘
                                │               │                   │
                  Movement/Sprint               Hacking/EMP       Gadgets/Takedown
                                │               │                   │
                                ▼               ▼                   ▼
                         Acoustic Noise    Device Offline     Unconscious Guard
                                │               │                   │
                                └───────┬───────┴───────────┬───────┘
                                        │                   │
                                        ▼                   ▼
     ┌─────────────────────────────────────┐     ┌─────────────────────────────────────┐
     │           SECURITY CAMERAS          │     │            PATROL GUARDS            │
     │  - Panning Vision Cones             │     │  - Line-of-Sight & Vision Cones     │
     │  - Line-of-Sight Raycasts           │     │  - Directional Hearing              │
     │  - Progressive Suspicion Meter      │     │  - Disturbed Object Inspection      │
     │  - Blind Spots Exploitable          │     │  - Unconscious Body Discovery       │
     └──────────────────┬──────────────────┘     └──────────────────┬──────────────────┘
                        │                                           │
                        └─────────────────────┬─────────────────────┘
                                              │ Alerts & Disturbances
                                              ▼
                             ┌───────────────────────────────────┐
                             │         SECURITY DIRECTOR         │
                             │  Coordinates facility-wide state: │
                             │  NORMAL ➔ SUSP ➔ ALERT ➔ LOCKDOWN │
                             └─────────────────┬─────────────────┘
                                               │
               ┌───────────────────────────────┼───────────────────────────────┐
               ▼                               ▼                               ▼
    ┌───────────────────────┐       ┌───────────────────────┐       ┌───────────────────────┐
    │     GUARDS ESCALATE   │       │   CAMERAS ACCELERATE  │       │     DOORS LOCKDOWN    │
    │ - Tighter patrol loops│       │ - Faster sweep speed  │       │ - Blast doors seal    │
    │ - Faster movement     │       │ - Accelerated detect  │       │ - Forces rerouting    │
    │ - Converging searches │       │ - Wider FOV           │       │ - Bypass required     │
    └───────────────────────┘       └───────────────────────┘       └───────────────────────┘
               │                               │                               │
               └───────────────────────────────┼───────────────────────────────┘
                                               │
                                               ▼
                             ┌───────────────────────────────────┐
                             │       TACTICAL GAMEPLAY IMPACT    │
                             │  Player must adapt route, bypass  │
                             │  locks, or exploit alternate paths│
                             └───────────────────────────────────┘
```

---

## 2. Interaction Matrix: Cause & Gameplay Consequence

| Source Action / Trigger | Intermediate System | Primary Response | World & Gameplay Consequence |
|---|---|---|---|
| **Player in Camera Cone** | Camera Raycast & Detection | Camera meter builds (Blue $\rightarrow$ Amber $\rightarrow$ Red) | Player must break line of sight into blind spots before 100% detection. |
| **Camera Full Detection** | Camera $\rightarrow$ Security Director | Director broadcasts visual breach alert | Nearest guards break patrol and rush to camera zone; Security rises to `ALERT`. |
| **Player Disables 1 Camera** | Camera Hack / EMP | Camera feed loops, creates blind spot | Nearby guard notes offline feed during patrol and investigates the camera junction. |
| **Player Disables Multiple Cams**| Security Director Tracker | Director recognizes systemic intrusion | Security elevates to `SUSPICIOUS`; guards adopt vigilant posture, cameras sweep faster. |
| **Guard Knocks Out Colleague** | Takedown / Stun Gun | Guard incapacitated on floor | Body remains in world; any passing guard who sees the body screams into radio. |
| **Guard Discovers Body** | Guard LOS $\rightarrow$ Security Director | Immediate escalation to `ALERT` / `LOCKDOWN` | Guards search radius around body; security siren triggers; player route compromised. |
| **Player Leaves Secure Door Open**| Guard Environmental Scan | Guard notices open security badge gate | Guard pauses, becomes suspicious, inspects the adjacent room, and closes the door. |
| **Player Trips Alarm Panel** | Tamper Siren $\rightarrow$ Director | Facility-wide alarm sirens activate | Security elevates to `LOCKDOWN`; blast doors seal; all guards converge. |
| **Facility LOCKDOWN** | Security Director Policy | Blast doors engage across primary corridors | Direct route to Server Room blocked; player must reroute through Security Checkpoint or hack ventilation. |
| **Decoy Grenade / Sound** | Noise Wavefront Propagation | Guard directional hearing | Draws guards away from secure choke points, opening temporary traversal windows. |

---

## 3. Four Distinct Tactical Approaches Enabled

1. **Approach A (Ghost Infiltration / Pure Stealth)**:
   - Learn camera sweep intervals and exploit physical blind spots behind walls and cover pillars.
   - Leave doors closed behind you; avoid disabling cameras to prevent guard investigation.
   - Facility remains in `NORMAL` state throughout.

2. **Approach B (Network Infiltration / Controlled Disturbance)**:
   - Hack individual cameras or workstations to create tactical openings.
   - Anticipate that guards will investigate the disabled camera, using that predictable movement to slip past behind them.
   - Keep disturbances below the intrusion threshold to prevent full `ALERT`.

3. **Approach C (Tactical Misdirection / Decoy Manipulation)**:
   - Throw noise decoys (`G`) or trigger localized security terminal pings to pull guards from key choke points.
   - Slip through while guards are in `INVESTIGATE` mode inspecting the noise source.

4. **Approach D (Emergency Divergence / Lockdown Breach)**:
   - Deliberately trip a remote alarm panel or trigger an alert in Sector 1.
   - As guards rush toward the siren, lockdown blast doors close off the main hallway.
   - The player uses a secondary route (Security Turnstile Gate or side office), hacking past lockdown seals while security is focused elsewhere.

5. **Approach E (Precision Subjugation / Body Concealment)**:
   - Takedown isolated guards using non-lethal strikes (`F`) or stun darts (`Q`).
   - Timing and placement matter: if a colleague discovers the body, the facility immediately enters `ALERT` or `LOCKDOWN`.
