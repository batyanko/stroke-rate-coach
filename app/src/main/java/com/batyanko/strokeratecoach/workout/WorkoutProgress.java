package com.batyanko.strokeratecoach.workout;

public final class WorkoutProgress {
    private WorkoutProgress() {
    }

    public static int aggregate(int[] phaseLengths, int currentPhaseIndex,
                                int currentPhaseProgress) {
        if (phaseLengths == null || currentPhaseIndex < 0
                || currentPhaseIndex > phaseLengths.length || currentPhaseProgress < 0) {
            throw new IllegalArgumentException("Invalid workout progress state");
        }
        if (currentPhaseIndex == 0) {
            return 0;
        }

        long totalProgress = 0;
        for (int i = 0; i < currentPhaseIndex - 1; i++) {
            if (phaseLengths[i] < 0) {
                throw new IllegalArgumentException("Phase lengths must not be negative");
            }
            totalProgress += phaseLengths[i];
        }
        int currentPhaseLength = phaseLengths[currentPhaseIndex - 1];
        if (currentPhaseLength < 0) {
            throw new IllegalArgumentException("Phase lengths must not be negative");
        }
        totalProgress += Math.min(currentPhaseProgress, currentPhaseLength);
        if (totalProgress > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Total progress exceeds supported maximum");
        }
        return (int) totalProgress;
    }
}
