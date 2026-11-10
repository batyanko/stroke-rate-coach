package com.batyanko.strokeratecoach.workout;

import java.util.Objects;

public final class WorkoutPhaseCursor {
    private final CompiledWorkout workout;
    private int nextIndex;
    private CompiledPhase current;

    public WorkoutPhaseCursor(CompiledWorkout workout) {
        this.workout = Objects.requireNonNull(workout, "workout");
    }

    public CompiledPhase startNext() {
        if (nextIndex >= workout.getPhases().size()) {
            current = null;
            return null;
        }
        current = workout.getPhases().get(nextIndex++);
        return current;
    }

    public CompiledPhase getCurrent() {
        return current;
    }

    public boolean isComplete() {
        return nextIndex >= workout.getPhases().size();
    }
}
