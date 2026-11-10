package com.batyanko.strokeratecoach.workout;

public enum WorkoutUnit {
    STROKES("0"),
    METERS("1"),
    SECONDS("2");

    private final String dbValue;

    WorkoutUnit(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static WorkoutUnit fromDbValue(String value) {
        if (value == null) {
            throw invalidUnit(value);
        }
        switch (value) {
            case "0":
                return STROKES;
            case "1":
                return METERS;
            case "2":
                return SECONDS;
            default:
                throw invalidUnit(value);
        }
    }

    private static WorkoutValidationException invalidUnit(String value) {
        return new WorkoutValidationException(
                WorkoutValidationException.Code.INVALID_UNIT_TYPE,
                "Unknown workout unit type: " + value);
    }
}
