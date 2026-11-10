package com.batyanko.strokeratecoach.workout;

public final class PhaseNode implements WorkoutNode {
    private final int length;
    private final int targetSpm;
    private final String label;

    public PhaseNode(int length, int targetSpm, String label) {
        this.length = length;
        this.targetSpm = targetSpm;
        this.label = label;
    }

    public PhaseNode(int length, int targetSpm) {
        this(length, targetSpm, null);
    }

    public int getLength() {
        return length;
    }

    public int getTargetSpm() {
        return targetSpm;
    }

    public String getLabel() {
        return label;
    }
}
