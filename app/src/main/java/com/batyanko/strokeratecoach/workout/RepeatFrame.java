package com.batyanko.strokeratecoach.workout;

import java.util.Objects;

public final class RepeatFrame {
    private final String label;
    private final int iteration;
    private final int repeatCount;

    RepeatFrame(String label, int iteration, int repeatCount) {
        this.label = Objects.requireNonNull(label, "label");
        if (iteration < 1 || repeatCount < 1 || iteration > repeatCount) {
            throw new IllegalArgumentException("Invalid repeat iteration");
        }
        this.iteration = iteration;
        this.repeatCount = repeatCount;
    }

    public String getLabel() {
        return label;
    }

    public int getIteration() {
        return iteration;
    }

    public int getRepeatCount() {
        return repeatCount;
    }
}
