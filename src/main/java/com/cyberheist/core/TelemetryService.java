package com.cyberheist.core;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.List;

/**
 * High-precision performance and diagnostics profiler.
 * Monitors real-time FPS, CPU load, JVM heap usage, GC activity,
 * active entity counts, render vs logic timing, and A* pathfinding performance.
 */
public final class TelemetryService {

    private static final TelemetryService INSTANCE = new TelemetryService();

    // Frame Rate & Timing
    private double currentFps = 60.0;
    private double currentUps = 60.0;
    private double currentDeltaTime = 0.016;
    private double updateDurationMs = 0.0;
    private double renderDurationMs = 0.0;

    // Entity & World Counts
    private int activeEntityCount = 0;
    private int activeParticleCount = 0;
    private int activeGuardCount = 0;

    // A* Pathfinding Profiling
    private long totalPathSearches = 0;
    private double searchesPerSecond = 0.0;
    private double avgPathSearchTimeMicros = 0.0;
    private long lastPathSearchCount = 0;
    private double pathSecondTimer = 0.0;

    // Operating System & Hardware
    private final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    private final List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

    private TelemetryService() {
    }

    public static TelemetryService getInstance() {
        return INSTANCE;
    }

    public void updateMetrics(int fps, int ups, double deltaTime, double updateMs, double renderMs) {
        this.currentFps = fps;
        this.currentUps = ups;
        this.currentDeltaTime = deltaTime;
        this.updateDurationMs = updateMs;
        this.renderDurationMs = renderMs;

        pathSecondTimer += deltaTime;
        if (pathSecondTimer >= 1.0) {
            this.searchesPerSecond = (totalPathSearches - lastPathSearchCount) / pathSecondTimer;
            this.lastPathSearchCount = totalPathSearches;
            this.pathSecondTimer = 0.0;
        }
    }

    public void recordPathSearch(long durationMicros) {
        totalPathSearches++;
        if (avgPathSearchTimeMicros <= 0.001) {
            avgPathSearchTimeMicros = durationMicros;
        } else {
            // Exponential moving average (alpha = 0.05)
            avgPathSearchTimeMicros = avgPathSearchTimeMicros * 0.95 + durationMicros * 0.05;
        }
    }

    public void recordPathfindingSearch(long durationMicros) {
        recordPathSearch(durationMicros);
    }

    public void setEntityCounts(int totalEntities, int particles, int guards) {
        this.activeEntityCount = totalEntities;
        this.activeParticleCount = particles;
        this.activeGuardCount = guards;
    }

    // Memory Metrics
    public long getUsedMemoryMb() {
        Runtime rt = Runtime.getRuntime();
        return (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
    }

    public long getTotalMemoryMb() {
        return Runtime.getRuntime().totalMemory() / (1024 * 1024);
    }

    public long getMaxMemoryMb() {
        return Runtime.getRuntime().maxMemory() / (1024 * 1024);
    }

    public long getTotalGcCount() {
        long count = 0;
        for (GarbageCollectorMXBean gc : gcBeans) {
            long c = gc.getCollectionCount();
            if (c > 0) count += c;
        }
        return count;
    }

    public double getSystemLoadAverage() {
        return osBean.getSystemLoadAverage();
    }

    public int getAvailableProcessors() {
        return osBean.getAvailableProcessors();
    }

    // Getters
    public double getCurrentFps() { return currentFps; }
    public double getCurrentUps() { return currentUps; }
    public double getCurrentDeltaTime() { return currentDeltaTime; }
    public double getUpdateDurationMs() { return updateDurationMs; }
    public double getRenderDurationMs() { return renderDurationMs; }
    public int getActiveEntityCount() { return activeEntityCount; }
    public int getActiveParticleCount() { return activeParticleCount; }
    public int getActiveGuardCount() { return activeGuardCount; }
    public long getTotalPathSearches() { return totalPathSearches; }
    public double getSearchesPerSecond() { return searchesPerSecond; }
    public double getAvgPathSearchTimeMicros() { return avgPathSearchTimeMicros; }
}
