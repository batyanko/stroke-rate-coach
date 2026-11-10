package com.batyanko.strokeratecoach.workout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.Test;

public class LegacyWorkoutParserTest {
    private final LegacyWorkoutParser parser = new LegacyWorkoutParser();

    @Test
    public void parsesFlatValuesInOrderAndMapsDatabaseUnit() {
        WorkoutDefinition definition = parser.parse(" 10,20 ", "22,24", "1");

        assertEquals(WorkoutUnit.METERS, definition.getUnit());
        assertEquals(2, definition.getNodes().size());
        PhaseNode first = (PhaseNode) definition.getNodes().get(0);
        PhaseNode second = (PhaseNode) definition.getNodes().get(1);
        assertEquals(10, first.getLength());
        assertEquals(22, first.getTargetSpm());
        assertEquals(20, second.getLength());
        assertEquals(24, second.getTargetSpm());
        assertEquals("0", WorkoutUnit.STROKES.dbValue());
        assertEquals(WorkoutUnit.STROKES, WorkoutUnit.fromDbValue("0"));
        assertEquals(WorkoutUnit.SECONDS, WorkoutUnit.fromDbValue("2"));
    }

    @Test
    public void rejectsInvalidCsvValuesAndShape() {
        String[][] invalidCsv = {
                {"1,", "22,22"},
                {"1,,2", "22,23,24"},
                {"", "22"},
                {"1", ""},
                {"1,2", "22"},
                {"1,x", "22,23"},
                {"0", "22"},
                {"-1", "22"},
                {"1", "0"},
                {"1", "-22"},
                {"2147483648", "22"},
                {"1", "2147483648"}
        };
        for (String[] pair : invalidCsv) {
            assertInvalid(pair[0], pair[1], "0");
        }
    }

    @Test
    public void rejectsNullAndInvalidUnitType() {
        assertInvalid(null, "22", "0");
        assertInvalid("1", null, "0");
        try {
            parser.parse("1", "22", "9");
            fail("Expected invalid unit type");
        } catch (WorkoutValidationException exception) {
            assertEquals(WorkoutValidationException.Code.INVALID_UNIT_TYPE,
                    exception.getCode());
        }
    }

    private void assertInvalid(String spp, String gears, String type) {
        try {
            parser.parse(spp, gears, type);
            fail("Expected invalid CSV for " + Arrays.toString(new String[]{spp, gears, type}));
        } catch (WorkoutValidationException exception) {
            assertEquals(WorkoutValidationException.Code.INVALID_LEGACY_CSV,
                    exception.getCode());
            assertTrue(exception.getMessage().contains("Legacy")
                    || exception.getMessage().contains("unit"));
        }
    }
}
