package com.batyanko.strokeratecoach.workout;

import java.util.List;
import java.util.Objects;

public final class WorkoutSummaryFormatter {
    public String format(WorkoutDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        StringBuilder summary = new StringBuilder();
        appendNodes(summary, definition.getNodes(), definition.getUnit(), 0);
        return summary.toString();
    }

    public boolean containsRepeats(WorkoutDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        return containsRepeats(definition.getNodes());
    }

    private void appendNodes(StringBuilder summary, List<WorkoutNode> nodes,
                             WorkoutUnit unit, int depth) {
        for (WorkoutNode node : nodes) {
            appendIndent(summary, depth);
            if (node instanceof RepeatNode) {
                RepeatNode repeat = (RepeatNode) node;
                String label = repeat.getLabel();
                if (label == null || label.trim().isEmpty()) {
                    label = "Repeat";
                }
                summary.append("Repeat ").append(label)
                        .append(" \u00d7").append(repeat.getRepeatCount()).append('\n');
                appendNodes(summary, repeat.getChildren(), unit, depth + 1);
                continue;
            }

            PhaseNode phase = (PhaseNode) node;
            summary.append(phase.getLength()).append(' ')
                    .append(unitLabel(unit, phase.getLength()))
                    .append(" at ").append(phase.getTargetSpm()).append(" SPM\n");
        }
    }

    private boolean containsRepeats(List<WorkoutNode> nodes) {
        for (WorkoutNode node : nodes) {
            if (!(node instanceof RepeatNode)) {
                continue;
            }
            return true;
        }
        return false;
    }

    private void appendIndent(StringBuilder summary, int depth) {
        for (int i = 0; i < depth; i++) {
            summary.append("  ");
        }
    }

    private String unitLabel(WorkoutUnit unit, int length) {
        switch (unit) {
            case STROKES:
                return length == 1 ? "stroke" : "strokes";
            case METERS:
                return length == 1 ? "metre" : "metres";
            case SECONDS:
                return length == 1 ? "second" : "seconds";
            default:
                throw new IllegalStateException("Unsupported workout unit: " + unit);
        }
    }
}
