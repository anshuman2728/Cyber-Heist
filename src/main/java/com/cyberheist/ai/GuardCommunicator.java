package com.cyberheist.ai;

import com.cyberheist.stealth.Guard;
import com.cyberheist.util.Logger;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Facility guard communication and alert broadcast bus.
 */
public final class GuardCommunicator {

    private static final Logger LOGGER = Logger.getLogger(GuardCommunicator.class);

    public interface AlertListener {
        void onSecurityAlert(Guard source, Point2D.Double alertLocation, String reason);
    }

    private final List<AlertListener> listeners = new ArrayList<>();

    public void register(AlertListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void unregister(AlertListener listener) {
        listeners.remove(listener);
    }

    public void broadcastAlert(Guard source, Point2D.Double alertLocation, String reason) {
        LOGGER.info(String.format("RADIO BROADCAST from [%s] at (%.0f, %.0f): %s",
                source != null ? source.getName() : "HQ",
                alertLocation.x, alertLocation.y, reason));

        for (AlertListener listener : listeners) {
            listener.onSecurityAlert(source, alertLocation, reason);
        }
    }
}
