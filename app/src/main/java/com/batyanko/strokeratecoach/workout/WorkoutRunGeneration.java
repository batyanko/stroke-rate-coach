package com.batyanko.strokeratecoach.workout;

public final class WorkoutRunGeneration {
    private long currentGeneration;

    public long startNewRun() {
        return ++currentGeneration;
    }

    public long invalidate() {
        return ++currentGeneration;
    }

    public boolean isCurrent(long generation) {
        return currentGeneration == generation;
    }
}
