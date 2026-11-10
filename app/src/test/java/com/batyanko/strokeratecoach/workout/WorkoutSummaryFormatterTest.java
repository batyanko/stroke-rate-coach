package com.batyanko.strokeratecoach.workout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class WorkoutSummaryFormatterTest {
    private final WorkoutSummaryFormatter formatter = new WorkoutSummaryFormatter();

    @Test
    public void formatsFlatWorkoutPhasesInOriginalOrder() {
        WorkoutDefinition definition = new WorkoutDefinition(WorkoutUnit.METERS,
                Arrays.asList(new PhaseNode(500, 22), new PhaseNode(1, 24)));

        assertEquals("500 metres at 22 SPM\n1 metre at 24 SPM\n",
                formatter.format(definition));
        assertFalse(formatter.containsRepeats(definition));
    }

    @Test
    public void formatsNestedRepeatTreeWithoutFlattening() {
        WorkoutDefinition definition = new WorkoutDefinition(WorkoutUnit.SECONDS,
                Collections.singletonList(new RepeatNode(3, "Cycle",
                        Collections.singletonList(new RepeatNode(3, "Set",
                                Collections.singletonList(new RepeatNode(5, "Rep",
                                        Collections.singletonList(
                                                new PhaseNode(60, 22)))))))));

        assertEquals("Repeat Cycle \u00d73\n"
                        + "  Repeat Set \u00d73\n"
                        + "    Repeat Rep \u00d75\n"
                        + "      60 seconds at 22 SPM\n",
                formatter.format(definition));
        assertTrue(formatter.containsRepeats(definition));
    }

    @Test
    public void formatsRestPhasesAndSkipLastRestSetting() {
        WorkoutDefinition definition = new WorkoutDefinition(WorkoutUnit.SECONDS,
                Collections.singletonList(new RepeatNode(2,
                        Arrays.asList(new PhaseNode(60, 22), new PhaseNode(10, 0)))),
                true);

        assertEquals("Skip last repeat rest\n"
                        + "Repeat Repeat \u00d72\n"
                        + "  60 seconds at 22 SPM\n"
                        + "  Rest for 10 seconds\n",
                formatter.format(definition));
    }
}
