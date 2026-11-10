package com.batyanko.strokeratecoach.workout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class WorkoutCompilerTest {
    private final WorkoutCompiler compiler = new WorkoutCompiler();

    @Test
    public void compilesFlatAndSequentialPhasesInOrder() {
        CompiledWorkout workout = compiler.compile(new WorkoutDefinition(
                WorkoutUnit.STROKES,
                Arrays.asList(new PhaseNode(10, 20), new PhaseNode(20, 22, "Fast"))));

        assertEquals(2, workout.getTotalPhases());
        assertEquals(30, workout.getTotalLength());
        assertPhase(workout, 0, 1, 2, 10, 20, "");
        assertPhase(workout, 1, 2, 2, 20, 22, "");
    }

    @Test
    public void expandsMixedSequentialAndRepeatedNodesInOrder() {
        CompiledWorkout workout = compiler.compile(new WorkoutDefinition(
                WorkoutUnit.SECONDS,
                Arrays.asList(
                        new PhaseNode(1, 10),
                        new RepeatNode(2, "Set", Arrays.asList(
                                new PhaseNode(2, 20),
                                new PhaseNode(3, 30))),
                        new PhaseNode(4, 40))));

        assertEquals(6, workout.getTotalPhases());
        assertEquals(15, workout.getTotalLength());
        int[] lengths = {1, 2, 3, 2, 3, 4};
        int[] targets = {10, 20, 30, 20, 30, 40};
        for (int i = 0; i < lengths.length; i++) {
            CompiledPhase phase = workout.getPhases().get(i);
            assertEquals(i + 1, phase.getPhaseIndex());
            assertEquals(lengths[i], phase.getLength());
            assertEquals(targets[i], phase.getTargetSpm());
        }
        assertEquals("Set 2/2", workout.getPhases().get(3).getBreadcrumb());
    }

    @Test
    public void compilesNested45PhaseSampleWithExactLengthAndBreadcrumbs() {
        WorkoutNode tree = new RepeatNode(3, "Cycle", Collections.singletonList(
                new RepeatNode(3, "Set", Collections.singletonList(
                        new RepeatNode(5, "Rep",
                                Collections.singletonList(new PhaseNode(60, 22)))))));
        CompiledWorkout workout = compiler.compile(
                new WorkoutDefinition(WorkoutUnit.SECONDS, Collections.singletonList(tree)));

        assertEquals(45, workout.getTotalPhases());
        assertEquals(2700, workout.getTotalLength());
        for (int i = 0; i < workout.getTotalPhases(); i++) {
            CompiledPhase phase = workout.getPhases().get(i);
            assertEquals(i + 1, phase.getPhaseIndex());
            assertEquals(45, phase.getTotalPhases());
            assertEquals(60, phase.getLength());
            assertEquals(22, phase.getTargetSpm());
        }
        assertEquals("Cycle 1/3 · Set 1/3 · Rep 1/5",
                workout.getPhases().get(0).getBreadcrumb());
        assertEquals("Cycle 2/3 · Set 2/3 · Rep 3/5",
                workout.getPhases().get(22).getBreadcrumb());
        assertEquals("Cycle 3/3 · Set 3/3 · Rep 5/5",
                workout.getPhases().get(44).getBreadcrumb());
        assertEquals(3, workout.getPhases().get(22).getRepeatPath().size());
    }

    @Test
    public void acceptsFourRepeatLevelsAndRejectsFive() {
        WorkoutNode fourDeep = nestedRepeat(4);
        assertEquals(1, compiler.compile(new WorkoutDefinition(
                WorkoutUnit.STROKES, Collections.singletonList(fourDeep))).getTotalPhases());
        assertCode(WorkoutValidationException.Code.MAX_REPEAT_DEPTH_EXCEEDED,
                new WorkoutDefinition(WorkoutUnit.STROKES,
                        Collections.singletonList(nestedRepeat(5))));
    }

    @Test
    public void acceptsExactly500PhasesAndRejects501() {
        WorkoutNode fiveHundred = new RepeatNode(500,
                Collections.singletonList(new PhaseNode(1, 1)));
        assertEquals(500, compiler.compile(new WorkoutDefinition(
                WorkoutUnit.STROKES, Collections.singletonList(fiveHundred))).getTotalPhases());
        WorkoutNode fiveHundredOne = new RepeatNode(501,
                Collections.singletonList(new PhaseNode(1, 1)));
        assertCode(WorkoutValidationException.Code.MAX_PHASE_COUNT_EXCEEDED,
                new WorkoutDefinition(WorkoutUnit.STROKES,
                        Collections.singletonList(fiveHundredOne)));
    }

    @Test
    public void rejectsEmptyWorkoutAndRepeat() {
        assertCode(WorkoutValidationException.Code.EMPTY_WORKOUT,
                new WorkoutDefinition(WorkoutUnit.STROKES, Collections.emptyList()));
        assertCode(WorkoutValidationException.Code.EMPTY_REPEAT,
                new WorkoutDefinition(WorkoutUnit.STROKES, Collections.singletonList(
                        new RepeatNode(2, Collections.emptyList()))));
    }

    @Test
    public void rejectsNonPositiveRepeatCountPhaseLengthAndTarget() {
        for (int count : new int[]{0, -1}) {
            assertCode(WorkoutValidationException.Code.INVALID_REPEAT_COUNT,
                    new WorkoutDefinition(WorkoutUnit.STROKES, Collections.singletonList(
                            new RepeatNode(count, Collections.singletonList(new PhaseNode(1, 1))))));
        }
        for (int length : new int[]{0, -1}) {
            assertCode(WorkoutValidationException.Code.INVALID_PHASE_LENGTH,
                    definition(new PhaseNode(length, 1)));
        }
        for (int target : new int[]{0, -1}) {
            assertCode(WorkoutValidationException.Code.INVALID_TARGET_SPM,
                    definition(new PhaseNode(1, target)));
        }
    }

    @Test
    public void rejectsAggregateLengthAboveIntegerMaximum() {
        WorkoutNode repeated = new RepeatNode(2,
                Collections.singletonList(new PhaseNode(Integer.MAX_VALUE, 1)));
        assertCode(WorkoutValidationException.Code.WORKOUT_LENGTH_OVERFLOW,
                new WorkoutDefinition(WorkoutUnit.SECONDS,
                        Collections.singletonList(repeated)));
    }

    @Test
    public void nodeListsAreDefensiveAndUnmodifiable() {
        List<WorkoutNode> children = new ArrayList<>();
        children.add(new PhaseNode(1, 1));
        RepeatNode repeat = new RepeatNode(2, children);
        children.clear();

        List<WorkoutNode> nodes = new ArrayList<>();
        nodes.add(repeat);
        WorkoutDefinition definition = new WorkoutDefinition(WorkoutUnit.STROKES, nodes);
        nodes.clear();

        assertEquals(1, definition.getNodes().size());
        assertEquals(1, repeat.getChildren().size());
        assertThrows(UnsupportedOperationException.class, () -> definition.getNodes().clear());
        assertThrows(UnsupportedOperationException.class, () -> repeat.getChildren().clear());
    }

    private WorkoutNode nestedRepeat(int depth) {
        WorkoutNode node = new PhaseNode(1, 1);
        for (int i = 0; i < depth; i++) {
            node = new RepeatNode(1, Collections.singletonList(node));
        }
        return node;
    }

    private WorkoutDefinition definition(WorkoutNode node) {
        return new WorkoutDefinition(WorkoutUnit.STROKES, Collections.singletonList(node));
    }

    private void assertPhase(CompiledWorkout workout, int listIndex, int index, int total,
                             int length, int targetSpm, String breadcrumb) {
        CompiledPhase phase = workout.getPhases().get(listIndex);
        assertEquals(index, phase.getPhaseIndex());
        assertEquals(total, phase.getTotalPhases());
        assertEquals(length, phase.getLength());
        assertEquals(targetSpm, phase.getTargetSpm());
        assertEquals(breadcrumb, phase.getBreadcrumb());
    }

    private void assertCode(WorkoutValidationException.Code code, WorkoutDefinition definition) {
        try {
            compiler.compile(definition);
            fail("Expected " + code);
        } catch (WorkoutValidationException exception) {
            assertEquals(code, exception.getCode());
            assertTrue(exception.getMessage().length() > 0);
        }
    }

}
