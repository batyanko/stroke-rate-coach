package com.batyanko.strokeratecoach.workout;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class WorkoutCompiler {
    public static final int MAX_REPEAT_DEPTH = 4;
    public static final int MAX_COMPILED_PHASES = 500;

    public CompiledWorkout compile(WorkoutDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        if (definition.getNodes().isEmpty()) {
            throw error(WorkoutValidationException.Code.EMPTY_WORKOUT,
                    "Workout must contain at least one phase");
        }

        ExpansionStats stats = measure(
                definition.getNodes(), 0, "workout", definition.getUnit(),
                definition.shouldSkipLastRest());
        if (stats.phaseCount > MAX_COMPILED_PHASES) {
            throw error(WorkoutValidationException.Code.MAX_PHASE_COUNT_EXCEEDED,
                    "Workout expands to " + stats.phaseCount + " phases; maximum is "
                            + MAX_COMPILED_PHASES);
        }
        if (stats.phaseCount == 0) {
            throw error(WorkoutValidationException.Code.EMPTY_WORKOUT,
                    "Workout must contain a phase after skipping repeat rests");
        }
        if (stats.totalLength > Integer.MAX_VALUE) {
            throw error(WorkoutValidationException.Code.WORKOUT_LENGTH_OVERFLOW,
                    "Workout length exceeds the supported maximum of " + Integer.MAX_VALUE);
        }

        List<CompiledPhase> phases = new ArrayList<>((int) stats.phaseCount);
        expand(definition.getNodes(), new ArrayList<>(), phases, (int) stats.phaseCount,
                "workout", false, definition.shouldSkipLastRest());
        return new CompiledWorkout(definition.getUnit(), phases, stats.totalLength);
    }

    private ExpansionStats measure(List<WorkoutNode> nodes, int repeatDepth, String path,
                                   WorkoutUnit unit, boolean skipLastRest) {
        long phaseCount = 0;
        long totalLength = 0;

        for (int i = 0; i < nodes.size(); i++) {
            String nodePath = path + " item " + (i + 1);
            WorkoutNode node = nodes.get(i);

            if (node instanceof PhaseNode) {
                ExpansionStats phaseStats = measurePhase((PhaseNode) node, nodePath, unit);
                phaseCount = addPhaseCounts(phaseCount, phaseStats.phaseCount, nodePath);
                totalLength = addLengths(totalLength, phaseStats.totalLength, nodePath);
                continue;
            }
            if (!(node instanceof RepeatNode)) {
                throw error(WorkoutValidationException.Code.INVALID_FIELD_TYPE,
                        nodePath + " has an unsupported workout node");
            }

            RepeatNode repeat = (RepeatNode) node;
            int childDepth = repeatDepth + 1;
            validateRepeat(repeat, childDepth, nodePath);

            ExpansionStats childStats = measure(
                    repeat.getChildren(), childDepth, nodePath, unit, skipLastRest);
            long repeatedPhases = multiplyPhaseCounts(
                    childStats.phaseCount, repeat.getRepeatCount(), nodePath);
            long repeatedLength = multiplyLengths(
                    childStats.totalLength, repeat.getRepeatCount(), nodePath);
            if (skipLastRest && hasTrailingRest(repeat.getChildren())) {
                PhaseNode trailingRest = (PhaseNode) repeat.getChildren().get(
                        repeat.getChildren().size() - 1);
                repeatedPhases--;
                repeatedLength -= trailingRest.getLength();
            }
            phaseCount = addPhaseCounts(phaseCount, repeatedPhases, nodePath);
            totalLength = addLengths(totalLength, repeatedLength, nodePath);
        }

        return new ExpansionStats(phaseCount, totalLength);
    }

    private ExpansionStats measurePhase(PhaseNode phase, String path, WorkoutUnit unit) {
        if (phase.getLength() < 1) {
            throw error(WorkoutValidationException.Code.INVALID_PHASE_LENGTH,
                    path + " has a phase length below 1");
        }
        if (phase.getTargetSpm() < 0
                || (phase.getTargetSpm() == 0 && unit == WorkoutUnit.STROKES)) {
            throw error(WorkoutValidationException.Code.INVALID_TARGET_SPM,
                    path + " uses zero SPM in a stroke-count workout; rests require a "
                            + "metres- or seconds-based workout");
        }
        return new ExpansionStats(1, phase.getLength());
    }

    private boolean hasTrailingRest(List<WorkoutNode> nodes) {
        if (nodes.isEmpty()) {
            return false;
        }
        WorkoutNode lastNode = nodes.get(nodes.size() - 1);
        return lastNode instanceof PhaseNode && ((PhaseNode) lastNode).getTargetSpm() == 0;
    }

    private void validateRepeat(RepeatNode repeat, int depth, String path) {
        if (depth > MAX_REPEAT_DEPTH) {
            throw error(WorkoutValidationException.Code.MAX_REPEAT_DEPTH_EXCEEDED,
                    path + " exceeds maximum repeat nesting depth of " + MAX_REPEAT_DEPTH);
        }
        if (repeat.getRepeatCount() < 1) {
            throw error(WorkoutValidationException.Code.INVALID_REPEAT_COUNT,
                    path + " repeat count must be at least 1");
        }
        if (repeat.getChildren().isEmpty()) {
            throw error(WorkoutValidationException.Code.EMPTY_REPEAT,
                    "Repeat at " + path + " has no child phases");
        }
    }

    private long addPhaseCounts(long left, long right, String path) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException e) {
            throw new WorkoutValidationException(
                    WorkoutValidationException.Code.MAX_PHASE_COUNT_EXCEEDED,
                    path + " expands beyond the supported phase count", e);
        }
    }

    private long multiplyPhaseCounts(long count, int repeatCount, String path) {
        try {
            return Math.multiplyExact(count, (long) repeatCount);
        } catch (ArithmeticException e) {
            throw new WorkoutValidationException(
                    WorkoutValidationException.Code.MAX_PHASE_COUNT_EXCEEDED,
                    path + " expands beyond the supported phase count", e);
        }
    }

    private long addLengths(long left, long right, String path) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException e) {
            throw new WorkoutValidationException(
                    WorkoutValidationException.Code.WORKOUT_LENGTH_OVERFLOW,
                    path + " total length overflows", e);
        }
    }

    private long multiplyLengths(long length, int repeatCount, String path) {
        try {
            return Math.multiplyExact(length, (long) repeatCount);
        } catch (ArithmeticException e) {
            throw new WorkoutValidationException(
                    WorkoutValidationException.Code.WORKOUT_LENGTH_OVERFLOW,
                    path + " total length overflows", e);
        }
    }

    private void expand(List<WorkoutNode> nodes, List<RepeatFrame> repeatPath,
                        List<CompiledPhase> phases, int totalPhases, String path,
                        boolean finalRepeatIteration, boolean skipLastRest) {
        for (int i = 0; i < nodes.size(); i++) {
            String nodePath = path + " item " + (i + 1);
            WorkoutNode node = nodes.get(i);

            if (node instanceof PhaseNode) {
                PhaseNode phase = (PhaseNode) node;
                if (skipLastRest && finalRepeatIteration && i == nodes.size() - 1
                        && phase.getTargetSpm() == 0) {
                    continue;
                }
                addCompiledPhase(phase, repeatPath, phases, totalPhases);
                continue;
            }
            if (!(node instanceof RepeatNode)) {
                throw error(WorkoutValidationException.Code.INVALID_FIELD_TYPE,
                        nodePath + " has an unsupported workout node");
            }
            expandRepeat((RepeatNode) node, repeatPath, phases, totalPhases, nodePath,
                    skipLastRest);
        }
    }

    private void addCompiledPhase(PhaseNode phase, List<RepeatFrame> repeatPath,
                                  List<CompiledPhase> phases, int totalPhases) {
        phases.add(new CompiledPhase(
                phases.size() + 1,
                totalPhases,
                phase.getLength(),
                phase.getTargetSpm(),
                repeatPath,
                formatBreadcrumb(repeatPath)));
    }

    private void expandRepeat(RepeatNode repeat, List<RepeatFrame> repeatPath,
                              List<CompiledPhase> phases, int totalPhases, String path,
                              boolean skipLastRest) {
        String label = normalizedLabel(repeat.getLabel());
        for (int iteration = 1; iteration <= repeat.getRepeatCount(); iteration++) {
            repeatPath.add(new RepeatFrame(label, iteration, repeat.getRepeatCount()));
            expand(repeat.getChildren(), repeatPath, phases, totalPhases, path,
                    iteration == repeat.getRepeatCount(), skipLastRest);
            repeatPath.remove(repeatPath.size() - 1);
        }
    }

    private String normalizedLabel(String label) {
        if (label == null || label.trim().isEmpty()) {
            return "Repeat";
        }
        return label;
    }

    private String formatBreadcrumb(List<RepeatFrame> repeatPath) {
        StringBuilder breadcrumb = new StringBuilder();
        for (RepeatFrame frame : repeatPath) {
            if (breadcrumb.length() > 0) {
                breadcrumb.append(" · ");
            }
            breadcrumb.append(frame.getLabel())
                    .append(' ')
                    .append(frame.getIteration())
                    .append('/')
                    .append(frame.getRepeatCount());
        }
        return breadcrumb.toString();
    }

    private WorkoutValidationException error(WorkoutValidationException.Code code, String message) {
        return new WorkoutValidationException(code, message);
    }

    private static final class ExpansionStats {
        private final long phaseCount;
        private final long totalLength;

        private ExpansionStats(long phaseCount, long totalLength) {
            this.phaseCount = phaseCount;
            this.totalLength = totalLength;
        }
    }
}
