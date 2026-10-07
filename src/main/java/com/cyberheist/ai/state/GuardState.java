package com.cyberheist.ai.state;

/**
 * Finite state machine states for guard artificial intelligence.
 */
public enum GuardState {
    PATROL,              // Regular route patrolling
    SUSPICIOUS,          // Senses disturbance (sound or peripheral glimpse), evaluates
    INVESTIGATE,         // Moving to investigate sound or sighting with pathfinding
    ALERT,               // High alarm, notifying colleagues, preparing pursuit
    CHASE,               // Active pursuit of visible player
    SEARCH,              // Lost visual contact, searching last known sector
    RETURN_TO_PATROL,    // Searching complete, pathfinding back to assigned patrol route
    STUNNED,             // Incapacitated by shock/EMP for a duration
    UNCONSCIOUS          // Permanently neutralized via non-lethal stealth takedown
}
