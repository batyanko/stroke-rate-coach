package com.batyanko.strokeratecoach.workout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class CompiledWorkout {
    private final WorkoutUnit unit;
    private final List<CompiledPhase> phases;
    private final int totalPhases;
    private final long totalLength;

    CompiledWorkout(WorkoutUnit unit, List<CompiledPhase> phases, long totalLength) {
        this.unit = Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(phases, "phases");
        if (phases.isEmpty() || totalLength < phases.size()
                || totalLength > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid compiled workout totals");
        }
        ArrayList<CompiledPhase> copy = new ArrayList<>(phases.size());
        long calculatedLength = 0;
        for (int i = 0; i < phases.size(); i++) {
            CompiledPhase phase = Objects.requireNonNull(phases.get(i), "phases contains null");
            if (phase.getPhaseIndex() != i + 1 || phase.getTotalPhases() != phases.size()) {
                throw new IllegalArgumentException("Compiled phase positions are inconsistent");
            }
            calculatedLength += phase.getLength();
            copy.add(phase);
        }
        if (calculatedLength != totalLength) {
            throw new IllegalArgumentException("Compiled workout length is inconsistent");
        }
        this.phases = Collections.unmodifiableList(copy);
        this.totalPhases = copy.size();
        this.totalLength = totalLength;
    }

    public WorkoutUnit getUnit() {
        return unit;
    }

    public List<CompiledPhase> getPhases() {
        return phases;
    }

    public int getTotalPhases() {
        return totalPhases;
    }

    public long getTotalLength() {
        return totalLength;
    }
}
