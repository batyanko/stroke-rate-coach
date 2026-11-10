package com.batyanko.strokeratecoach.sync;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class BeeperTasksPayloadTest {
    @Test
    public void acceptsManualPaceWithoutPresetPhaseMetadata() {
        assertNull(BeeperTasks.validateStartPayload(
                null, new int[]{22}, null, BeeperTasks.SPP_TYPE_STROKES));
    }

    @Test
    public void acceptsCompiledPresetPayloadIncludingPhasesOutsideRepeats() {
        assertNull(BeeperTasks.validateStartPayload(
                new int[]{10, 20},
                new int[]{22, 24},
                new String[]{"", "Set 1/2"},
                BeeperTasks.SPP_TYPE_SECONDS));
    }

    @Test
    public void rejectsMismatchedOrMissingPresetMetadata() {
        assertNotNull(BeeperTasks.validateStartPayload(
                new int[]{10}, new int[]{22, 24}, new String[]{""},
                BeeperTasks.SPP_TYPE_STROKES));
        assertNotNull(BeeperTasks.validateStartPayload(
                new int[]{10}, new int[]{22}, null, BeeperTasks.SPP_TYPE_STROKES));
    }

    @Test
    public void rejectsInvalidValuesUnitsAndExcessiveAggregateLength() {
        assertNotNull(BeeperTasks.validateStartPayload(
                new int[]{0}, new int[]{22}, new String[]{""},
                BeeperTasks.SPP_TYPE_STROKES));
        assertNotNull(BeeperTasks.validateStartPayload(
                null, new int[]{0}, null, BeeperTasks.SPP_TYPE_STROKES));
        assertNotNull(BeeperTasks.validateStartPayload(
                new int[]{Integer.MAX_VALUE, 1}, new int[]{22, 24},
                new String[]{"", ""}, BeeperTasks.SPP_TYPE_STROKES));
        assertNotNull(BeeperTasks.validateStartPayload(
                null, new int[]{22}, null, "unknown"));
    }
}
