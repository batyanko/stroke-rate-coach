package com.batyanko.strokeratecoach.workout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class CompiledPhase {
    private final int phaseIndex;
    private final int totalPhases;
    private final int length;
    private final int targetSpm;
    private final List<RepeatFrame> repeatPath;
    private final String breadcrumb;

    CompiledPhase(int phaseIndex, int totalPhases, int length, int targetSpm,
                  List<RepeatFrame> repeatPath, String breadcrumb) {
        if (phaseIndex < 1 || totalPhases < phaseIndex) {
            throw new IllegalArgumentException("Invalid compiled phase index");
        }
        if (length < 1 || targetSpm < 1) {
            throw new IllegalArgumentException("Compiled phase values must be positive");
        }
        Objects.requireNonNull(repeatPath, "repeatPath");
        this.phaseIndex = phaseIndex;
        this.totalPhases = totalPhases;
        this.length = length;
        this.targetSpm = targetSpm;
        ArrayList<RepeatFrame> pathCopy = new ArrayList<>(repeatPath.size());
        for (RepeatFrame frame : repeatPath) {
            pathCopy.add(Objects.requireNonNull(frame, "repeatPath contains null"));
        }
        this.repeatPath = Collections.unmodifiableList(pathCopy);
        this.breadcrumb = Objects.requireNonNull(breadcrumb, "breadcrumb");
    }

    public int getPhaseIndex() {
        return phaseIndex;
    }

    public int getTotalPhases() {
        return totalPhases;
    }

    public int getLength() {
        return length;
    }

    public int getTargetSpm() {
        return targetSpm;
    }

    public List<RepeatFrame> getRepeatPath() {
        return repeatPath;
    }

    public String getBreadcrumb() {
        return breadcrumb;
    }
}
