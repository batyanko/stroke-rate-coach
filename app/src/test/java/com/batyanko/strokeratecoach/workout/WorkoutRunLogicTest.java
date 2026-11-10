package com.batyanko.strokeratecoach.workout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class WorkoutRunLogicTest {
    @Test
    public void invalidatesCallbacksFromStoppedAndSupersededRuns() {
        WorkoutRunGeneration generations = new WorkoutRunGeneration();
        long firstRun = generations.startNewRun();
        assertTrue(generations.isCurrent(firstRun));

        generations.invalidate();
        assertFalse(generations.isCurrent(firstRun));

        long secondRun = generations.startNewRun();
        assertTrue(generations.isCurrent(secondRun));
        assertFalse(generations.isCurrent(firstRun));
    }

    @Test
    public void aggregatesProgressAcrossExpandedNestedPhases() {
        int[] expandedLengths = {10, 20, 10, 20, 10, 20};

        assertEquals(0, WorkoutProgress.aggregate(expandedLengths, 0, 0));
        assertEquals(15, WorkoutProgress.aggregate(expandedLengths, 2, 5));
        assertEquals(40, WorkoutProgress.aggregate(expandedLengths, 3, 10));
        assertEquals(90, WorkoutProgress.aggregate(expandedLengths, 6, 20));
        assertEquals(90, WorkoutProgress.aggregate(expandedLengths, 6, 100));
    }

    @Test
    public void compiledNestedPhaseOrderIsIndependentOfUnit() {
        WorkoutNode tree = new RepeatNode(2, "Set", Arrays.asList(
                new PhaseNode(10, 20),
                new RepeatNode(2, "Rep",
                        Collections.singletonList(new PhaseNode(5, 24)))));
        WorkoutCompiler compiler = new WorkoutCompiler();

        CompiledWorkout strokes = compiler.compile(new WorkoutDefinition(
                WorkoutUnit.STROKES, Collections.singletonList(tree)));
        CompiledWorkout meters = compiler.compile(new WorkoutDefinition(
                WorkoutUnit.METERS, Collections.singletonList(tree)));
        CompiledWorkout seconds = compiler.compile(new WorkoutDefinition(
                WorkoutUnit.SECONDS, Collections.singletonList(tree)));

        assertEquals(strokes.getPhases().size(), meters.getPhases().size());
        assertEquals(strokes.getPhases().size(), seconds.getPhases().size());
        for (int i = 0; i < strokes.getPhases().size(); i++) {
            CompiledPhase strokePhase = strokes.getPhases().get(i);
            assertEquals(strokePhase.getLength(), meters.getPhases().get(i).getLength());
            assertEquals(strokePhase.getLength(), seconds.getPhases().get(i).getLength());
            assertEquals(strokePhase.getTargetSpm(), meters.getPhases().get(i).getTargetSpm());
            assertEquals(strokePhase.getTargetSpm(), seconds.getPhases().get(i).getTargetSpm());
            assertEquals(strokePhase.getBreadcrumb(), meters.getPhases().get(i).getBreadcrumb());
            assertEquals(strokePhase.getBreadcrumb(), seconds.getPhases().get(i).getBreadcrumb());
        }
    }

    @Test
    public void rejectsInvalidProgressStateRatherThanReturningSuccessShape() {
        try {
            WorkoutProgress.aggregate(new int[]{10}, 2, 1);
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Invalid phase index should be rejected");
    }
}
