package com.batyanko.strokeratecoach.workout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class WorkoutPhaseCursorTest {
    @Test
    public void returnsEachPhaseInOrderAndCompletesOnceWithoutMutatingWorkout() {
        CompiledWorkout workout = new WorkoutCompiler().compile(new WorkoutDefinition(
                WorkoutUnit.STROKES,
                Arrays.asList(new PhaseNode(10, 20), new PhaseNode(20, 22))));
        List<CompiledPhase> originalPhases = workout.getPhases();
        WorkoutPhaseCursor cursor = new WorkoutPhaseCursor(workout);

        assertNull(cursor.getCurrent());
        assertFalse(cursor.isComplete());
        CompiledPhase first = cursor.startNext();
        assertSame(originalPhases.get(0), first);
        assertSame(first, cursor.getCurrent());
        assertEquals(1, first.getPhaseIndex());

        CompiledPhase second = cursor.startNext();
        assertSame(originalPhases.get(1), second);
        assertEquals(2, second.getPhaseIndex());
        assertTrue(cursor.isComplete());

        assertNull(cursor.startNext());
        assertNull(cursor.startNext());
        assertNull(cursor.getCurrent());
        assertSame(originalPhases.get(0), workout.getPhases().get(0));
        assertThrows(UnsupportedOperationException.class, () -> workout.getPhases().clear());
    }
}
