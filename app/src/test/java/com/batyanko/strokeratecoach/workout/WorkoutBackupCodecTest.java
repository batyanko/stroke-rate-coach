package com.batyanko.strokeratecoach.workout;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class WorkoutBackupCodecTest {
    private WorkoutBackupCodec backupCodec;
    private WorkoutJsonCodec jsonCodec;

    @Before
    public void setUp() {
        jsonCodec = new WorkoutJsonCodec();
        backupCodec = new WorkoutBackupCodec(
                jsonCodec, new LegacyWorkoutParser(), new WorkoutCompiler());
    }

    @Test
    public void restoresOldFlatBackupWithoutTreeField() {
        WorkoutBackupCodec.BackupRecord record = backupCodec.parseRecord(
                "Name: Old workout\nDesc: Legacy\nType: 0\nSPM: 22,24\nSPP: 10,20",
                "fallback");

        assertEquals("Old workout", record.getName());
        assertEquals("Legacy", record.getDescription());
        WorkoutDefinition definition = record.getDefinition();
        assertEquals("0", definition.getUnit().dbValue());
        assertEquals(2, definition.getNodes().size());
        assertEquals(10, ((PhaseNode) definition.getNodes().get(0)).getLength());
        assertEquals(24, ((PhaseNode) definition.getNodes().get(1)).getTargetSpm());
    }

    @Test
    public void nestedBackupRoundTripPreservesLabelsAndExecutionProjection() {
        String label = "Unicode ü; quotes \" and commas, Name: Desc: Type: SPM: SPP: "
                + "TREE_B64: newline\n-------";
        WorkoutDefinition original = new WorkoutDefinition(WorkoutUnit.SECONDS,
                Arrays.<WorkoutNode>asList(
                        new PhaseNode(12, 21),
                        new RepeatNode(2, label, Collections.<WorkoutNode>singletonList(
                                new PhaseNode(30, 25)))));

        String exported = backupCodec.encodeRecord("Nested", "Description", original);
        assertEquals(6, exported.split("\n", -1).length);
        WorkoutBackupCodec.BackupRecord record = backupCodec.parseRecord(exported, "fallback");
        WorkoutDefinition restored = record.getDefinition();

        assertEquals(jsonCodec.serialize(original), jsonCodec.serialize(restored));
        CompiledWorkout compiled = new WorkoutCompiler().compile(restored);
        assertEquals(3, compiled.getTotalPhases());
        assertEquals(72, compiled.getTotalLength());
        assertEquals(label, ((RepeatNode) restored.getNodes().get(1)).getLabel());
    }

    @Test
    public void rejectsTreeWhenItsCsvProjectionDiffers() {
        String record = backupCodec.encodeRecord(
                "Mismatch", "", new WorkoutDefinition(WorkoutUnit.STROKES,
                        Collections.<WorkoutNode>singletonList(new PhaseNode(10, 22))));
        String mismatch = record.replace("SPM: 22", "SPM: 23");

        try {
            backupCodec.parseRecord(mismatch, "fallback").getDefinition();
        } catch (WorkoutValidationException exception) {
            assertEquals(WorkoutValidationException.Code.TREE_CSV_MISMATCH,
                    exception.getCode());
            return;
        }
        throw new AssertionError("Mismatched CSV projection must be rejected");
    }

    @Test
    public void rejectsInvalidPresentTreeInsteadOfUsingLegacyCsv() {
        String record = "Name: Invalid\nType: 0\nSPM: 22\nSPP: 10\nTREE_B64: invalid!";

        try {
            backupCodec.parseRecord(record, "fallback").getDefinition();
        } catch (WorkoutValidationException exception) {
            assertEquals(WorkoutValidationException.Code.INVALID_BACKUP_TREE,
                    exception.getCode());
            return;
        }
        throw new AssertionError("Invalid tree must not fall back to CSV");
    }
}
