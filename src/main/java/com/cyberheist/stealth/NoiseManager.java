package com.cyberheist.stealth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Manages active acoustic events, sound propagation, and hearing detection.
 */
public final class NoiseManager {

    private final List<NoiseEvent> activeNoises = new ArrayList<>();

    public void emit(double x, double y, NoiseType type) {
        if (type != null && type != NoiseType.NONE) {
            activeNoises.add(new NoiseEvent(x, y, type));
        }
    }

    public void update(double deltaTime) {
        Iterator<NoiseEvent> iterator = activeNoises.iterator();
        while (iterator.hasNext()) {
            NoiseEvent event = iterator.next();
            event.update(deltaTime);
            if (event.isExpired()) {
                iterator.remove();
            }
        }
    }

    /**
     * Checks if any acoustic event reached the listener's coordinates within hearing range.
     * Returns the strongest audible noise event, or null if none heard.
     */
    public NoiseEvent findStrongestAudibleNoise(double listenerX, double listenerY, double hearingRadius) {
        NoiseEvent strongest = null;
        double maxIntensity = 0.0;

        for (NoiseEvent event : activeNoises) {
            double dist = Math.hypot(listenerX - event.getX(), listenerY - event.getY());
            // Sound is audible if listener is within the expanding wavefront or total radius
            if (dist <= event.getType().getRadius() + hearingRadius) {
                double intensity = event.getIntensity() * (1.0 - (dist / (event.getType().getRadius() + hearingRadius)));
                if (intensity > maxIntensity) {
                    maxIntensity = intensity;
                    strongest = event;
                }
            }
        }

        return strongest;
    }

    public List<NoiseEvent> getActiveNoises() {
        return Collections.unmodifiableList(activeNoises);
    }

    public void clear() {
        activeNoises.clear();
    }
}
