package com.batyanko.strokeratecoach.workout;

import java.util.List;

public final class RepeatNode implements WorkoutNode {
    private final int repeatCount;
    private final String label;
    private final List<WorkoutNode> children;

    public RepeatNode(int repeatCount, String label, List<WorkoutNode> children) {
        this.repeatCount = repeatCount;
        this.label = label;
        this.children = WorkoutDefinition.immutableNodes(children, "children");
    }

    public RepeatNode(int repeatCount, List<WorkoutNode> children) {
        this(repeatCount, null, children);
    }

    public int getRepeatCount() {
        return repeatCount;
    }

    public String getLabel() {
        return label;
    }

    public List<WorkoutNode> getChildren() {
        return children;
    }
}
