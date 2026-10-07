package com.cyberheist.security;

import java.awt.geom.Point2D;

/**
 * Listener interface for reacting to facility-wide security changes and director dispatches.
 */
@FunctionalInterface
public interface SecurityEventListener {
    void onSecurityLevelChanged(SecurityLevel oldLevel, SecurityLevel newLevel, String cause);
}
