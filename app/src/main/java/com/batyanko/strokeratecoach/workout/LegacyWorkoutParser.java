package com.batyanko.strokeratecoach.workout;

import java.util.ArrayList;
import java.util.List;

public final class LegacyWorkoutParser {
    public WorkoutDefinition parse(String sppCsv, String gearsCsv, String sppType) {
        WorkoutUnit unit = WorkoutUnit.fromDbValue(sppType);
        if (isBlank(sppCsv) || isBlank(gearsCsv)) {
            throw invalid("Legacy SPP and SPM values must not be empty");
        }

        String[] lengths = sppCsv.split(",", -1);
        String[] targets = gearsCsv.split(",", -1);
        if (lengths.length != targets.length) {
            throw invalid("Legacy SPP and SPM values must have the same non-zero item count");
        }

        List<WorkoutNode> nodes = new ArrayList<>(lengths.length);
        for (int i = 0; i < lengths.length; i++) {
            int length = parsePositiveInteger(lengths[i], "phase length", i);
            int targetSpm = parseTargetSpm(targets[i], i, unit);
            nodes.add(new PhaseNode(length, targetSpm));
        }
        return new WorkoutDefinition(unit, nodes);
    }

    private int parseTargetSpm(String token, int index, WorkoutUnit unit) {
        String trimmed = token.trim();
        if (trimmed.isEmpty()) {
            throw invalid("Legacy target SPM at item " + (index + 1) + " is empty");
        }
        final int value;
        try {
            value = Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            throw new WorkoutValidationException(
                    WorkoutValidationException.Code.INVALID_LEGACY_CSV,
                    "Legacy target SPM at item " + (index + 1) + " is not a valid integer", e);
        }
        if (value < 0 || (value == 0 && unit == WorkoutUnit.STROKES)) {
            throw invalid("Legacy target SPM at item " + (index + 1)
                    + " must be positive for stroke-count workouts; zero-SPM rests require"
                    + " a metres- or seconds-based workout");
        }
        return value;
    }

    private int parsePositiveInteger(String token, String field, int index) {
        String trimmed = token.trim();
        if (trimmed.isEmpty()) {
            throw invalid("Legacy " + field + " at item " + (index + 1) + " is empty");
        }
        final int value;
        try {
            value = Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            throw new WorkoutValidationException(
                    WorkoutValidationException.Code.INVALID_LEGACY_CSV,
                    "Legacy " + field + " at item " + (index + 1) + " is not a valid integer", e);
        }
        if (value <= 0) {
            throw invalid("Legacy " + field + " at item " + (index + 1)
                    + " must be positive");
        }
        return value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private WorkoutValidationException invalid(String message) {
        return new WorkoutValidationException(
                WorkoutValidationException.Code.INVALID_LEGACY_CSV, message);
    }
}
