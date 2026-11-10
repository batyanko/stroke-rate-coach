package com.batyanko.strokeratecoach.workout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class WorkoutDefinition {
    private final WorkoutUnit unit;
    private final List<WorkoutNode> nodes;

    public WorkoutDefinition(WorkoutUnit unit, List<WorkoutNode> nodes) {
        this.unit = Objects.requireNonNull(unit, "unit");
        this.nodes = immutableNodes(nodes, "nodes");
    }

    public WorkoutUnit getUnit() {
        return unit;
    }

    public List<WorkoutNode> getNodes() {
        return nodes;
    }

    static List<WorkoutNode> immutableNodes(List<WorkoutNode> nodes, String name) {
        Objects.requireNonNull(nodes, name);
        ArrayList<WorkoutNode> copy = new ArrayList<>(nodes.size());
        for (WorkoutNode node : nodes) {
            copy.add(Objects.requireNonNull(node, name + " contains null"));
        }
        return Collections.unmodifiableList(copy);
    }
}
