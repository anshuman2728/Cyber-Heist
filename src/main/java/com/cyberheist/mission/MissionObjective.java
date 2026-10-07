package com.cyberheist.mission;

/**
 * Encapsulates a single trackable mission objective, its status, and progression lifecycle.
 */
public final class MissionObjective {

    public enum Status {
        PENDING,
        ACTIVE,
        COMPLETED,
        FAILED
    }

    private final MissionStep step;
    private Status status = Status.PENDING;
    private double completedAtTime = 0.0;

    public MissionObjective(MissionStep step) {
        this.step = step;
    }

    public MissionStep getStep() {
        return step;
    }

    public String getTitle() {
        return step.getLabel();
    }

    public String getDescription() {
        return step.getDescription();
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean isCompleted() {
        return status == Status.COMPLETED;
    }

    public boolean isActive() {
        return status == Status.ACTIVE;
    }

    public double getCompletedAtTime() {
        return completedAtTime;
    }

    public void setCompletedAtTime(double completedAtTime) {
        this.completedAtTime = completedAtTime;
    }
}
