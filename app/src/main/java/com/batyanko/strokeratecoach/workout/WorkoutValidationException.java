package com.batyanko.strokeratecoach.workout;

public final class WorkoutValidationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public enum Code {
        MALFORMED_JSON,
        UNSUPPORTED_SCHEMA_VERSION,
        UNKNOWN_NODE_TYPE,
        MISSING_FIELD,
        INVALID_FIELD_TYPE,
        INVALID_PHASE_LENGTH,
        INVALID_TARGET_SPM,
        INVALID_REPEAT_COUNT,
        EMPTY_WORKOUT,
        EMPTY_REPEAT,
        MAX_REPEAT_DEPTH_EXCEEDED,
        MAX_PHASE_COUNT_EXCEEDED,
        WORKOUT_LENGTH_OVERFLOW,
        INVALID_UNIT_TYPE,
        INVALID_LEGACY_CSV,
        TREE_CSV_MISMATCH,
        INVALID_BACKUP_TREE,
        INVALID_BACKUP_RECORD
    }

    private final Code code;

    public WorkoutValidationException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public WorkoutValidationException(Code code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public Code getCode() {
        return code;
    }
}
