/*
 * Copyright (C) 2017 Yanko Georgiev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.batyanko.strokeratecoach.sync;

import static android.widget.Toast.LENGTH_LONG;
import static com.batyanko.strokeratecoach.WaveActivity.USE_LOC_KEY;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.batyanko.strokeratecoach.R;
import com.batyanko.strokeratecoach.Speed.CLocation;
import com.batyanko.strokeratecoach.Speed.IBaseGpsListener;
import com.batyanko.strokeratecoach.Utils.NotificationUtils;
import com.batyanko.strokeratecoach.Utils.SpmUtilities;
import com.batyanko.strokeratecoach.WaveActivity;

import java.io.IOException;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by batyanko on 9/4/17.
 */

public class BeeperTasks {

    public static final String EXTRA_WORKOUT_SPP_TYPE = "workout-spp-type";
    public static final String EXTRA_PHASE_LENGTHS = "phase_lengths";
    public static final String EXTRA_TARGET_SPMS = "target_spms";
    public static final String EXTRA_PHASE_BREADCRUMBS = "phase_breadcrumbs";
    public static final String EXTRA_WORKOUT_ID = "workout-id";
    public static final String ACTION_START_BEEP = "start-beep";
    public static final String ACTION_STOP_BEEP = "stop-beep";
    public static final String ACTION_CHECK_SERVICE = "check-service";
    public static final String ACTION_JUST_BIND = "just-bind";
    public static final String ACTION_START_WARNING = "start-warning";
    public static final String ACTION_STOP_WARNING = "stop-warning";

    //SPP (strokes or stuff per phase) unit types
    public static final String SPP_TYPE_STROKES = "0";
    public static final String SPP_TYPE_METERS = "1";
    public static final String SPP_TYPE_SECONDS = "2";

    //VALUES FROM WaveActivity

    /* Global spm setting to hold current spm */
    public static int spm = 22;
    //Variables used in beeping workoutTimer setup
    private static long strokeDuration;

    private MediaPlayer player;
    private static ToneGenerator workoutToneGen;
    private static ToneGenerator countdownToneGen;
    private static ToneGenerator warningToneGen;

    private Timer workoutTimer;
    private TimerTask workoutTimerTask;

    private Timer timeTimer;
    private TimerTask timeTimerTask;

    private Timer countdownTimer;
    private TimerTask countdownTimerTask;

    private TimerTask speedLimitTimerTask;

    private Timer speedLimitTimer;
    //autoWave values
    private int workoutRunning;     //0=off, 1=wave(depreciated), 2=interval, 3=simple 9=lastPhase
    private static int countdownRunning;    //0=off, 1=on
    private int phaseTrigger;
    private int phaseProgress;
    private int color;

    private String mSppType;

    private static SharedPreferences pref;

    //END OF COPIED VALUES

    private static CLocation currentLocation;

    private static CLocation startingPhaseLocation;

    //TODO > 20 for emulator testing, < 5 for practical use
    public static final float ACCEPTABLE_ACCURACY_DEFAULT = 5;
    public static float acceptableAccuracy = ACCEPTABLE_ACCURACY_DEFAULT;
    private static float locationAccuracy;

    private static CLocation[] locationPool;

    private static float averageSpeed;  //In m/s

    private static int locCycleCount;
    //TODO add preference for sample count
    private static final int SPEED_SAMPLE_COUNT_DEFAULT = 10;
    private static int speedSampleCount = SPEED_SAMPLE_COUNT_DEFAULT;

    private static boolean locationPoolIsFull;

    private int prevPhasesDistance;

    //TODO make static?
    private long phaseStartElapsedRealtime;
    private int timeTimerPhaseProgress;
    public static int countdownCyclesTotal;
    public static int countdownCyclesElapsed;
    private int countdownCycleDuration;
    private int countdownDurationMs;
    private String sndPref;
    private int oldWorkoutProgress;

    private final Object runLock = new Object();
    private long runGeneration;
    private RunState activeRun;

    private int beeps;
    private static int warns;
    private static boolean warningIsRunning = false;

    private static final class RunState {
        private final long generation;
        private final int[] phaseLengths;
        private final int[] targetSpms;
        private final String[] breadcrumbs;
        private final int totalPhases;
        private final int totalLength;
        private int currentPhaseIndex;
        private int currentWorkoutProgress;
        private String currentBreadcrumb = "";

        private RunState(long generation, int[] phaseLengths, int[] targetSpms,
                         String[] breadcrumbs, int totalLength) {
            this.generation = generation;
            this.phaseLengths = phaseLengths == null ? null : phaseLengths.clone();
            this.targetSpms = targetSpms.clone();
            this.breadcrumbs = breadcrumbs == null ? null : breadcrumbs.clone();
            this.totalPhases = phaseLengths == null ? 0 : phaseLengths.length;
            this.totalLength = totalLength;
        }
    }

    void executeTask(BeeperService beeperService, String action,
                     int[] phaseLengths, int[] targetSpms,
                     String[] breadcrumbs, String sppType) {
        if (action.equals(ACTION_START_BEEP)) {
            String invalidPayload = validateStartPayload(
                    phaseLengths, targetSpms, breadcrumbs, sppType);
            if (invalidPayload != null) {
                Log.e("BeeperTasks", "Rejected workout start payload: " + invalidPayload);
                Toast.makeText(beeperService, "Workout could not start: " + invalidPayload,
                        LENGTH_LONG).show();
                return;
            }

            long generation;
            int totalLength = 0;
            if (phaseLengths != null) {
                for (int length : phaseLengths) {
                    totalLength += length;
                }
            }
            synchronized (runLock) {
                invalidateRunLocked(beeperService);
                generation = ++runGeneration;
                activeRun = new RunState(
                        generation, phaseLengths, targetSpms, breadcrumbs, totalLength);
            }
            synchronized (runLock) {
                if (!isCurrentRun(generation)) {
                    return;
                }
                startRun(beeperService, phaseLengths, targetSpms, sppType, generation);
            }
        } else if (action.equals(ACTION_STOP_BEEP)) {
            endWorkout(beeperService);
        } else if (action.equals(ACTION_CHECK_SERVICE)) {
            if (pref == null) {
                pref = PreferenceManager.getDefaultSharedPreferences(beeperService);
            }
            pref.edit().putInt(WaveActivity.OPERATION_SETTING, workoutRunning).apply();
        } else if (action.equals(ACTION_START_WARNING)) {
            if (speedLimitTimer == null || speedLimitTimerTask == null || !warningIsRunning) {
                startSpeedLimit(beeperService);
                warningIsRunning = true;
            }
        } else if (action.equals(ACTION_STOP_WARNING)) {
            cancelTimer(speedLimitTimer, speedLimitTimerTask);
            warningIsRunning = false;
        }
    }

    private void startRun(BeeperService beeperService, int[] phaseLengths,
                          int[] targetSpms, String sppType, long generation) {
        releaseAudioResources();
        NotificationUtils.showWorkoutNotification(beeperService);
        mSppType = sppType;
        initBeeper(beeperService);
        if (Objects.equals(mSppType, SPP_TYPE_METERS)) {
            boolean enabled = pref.getBoolean(USE_LOC_KEY, false);
            if (!enabled) {
                endWorkout(beeperService);
                Toast.makeText(beeperService.getBaseContext(),
                        "Distance based workout.\nPlease enable location in settings.",
                        LENGTH_LONG).show();
                return;
            }
            pref.edit().putBoolean(WaveActivity.GPS_LOCKING, true).apply();
        }
        initLocation(beeperService, generation);
        if (phaseLengths == null) {
            workoutRunning = WaveActivity.WORKOUT_SIMPLE;
            flushUI();
            spm = targetSpms[0];
            startTheTempo(beeperService, generation);
            return;
        }

        workoutRunning = WaveActivity.WORKOUT_INTERVAL;
        pref.edit().putInt(WaveActivity.OPERATION_SETTING, workoutRunning).apply();
        pref.edit().putInt(WaveActivity.WORKOUT_LENGTH, activeRun.totalLength).apply();
        startCountdown(beeperService, generation);
    }

    static String validateStartPayload(int[] phaseLengths, int[] targetSpms,
                                       String[] breadcrumbs, String sppType) {
        if (!SPP_TYPE_STROKES.equals(sppType)
                && !SPP_TYPE_METERS.equals(sppType)
                && !SPP_TYPE_SECONDS.equals(sppType)) {
            return "unknown workout unit";
        }
        if (phaseLengths == null) {
            if (!SPP_TYPE_STROKES.equals(sppType) || targetSpms == null
                    || targetSpms.length != 1 || breadcrumbs != null) {
                return "invalid manual-pace data";
            }
            return targetSpms[0] > 0 ? null : "target SPM must be positive";
        }
        if (phaseLengths.length < 1 || phaseLengths.length > 500
                || targetSpms == null || breadcrumbs == null
                || phaseLengths.length != targetSpms.length
                || phaseLengths.length != breadcrumbs.length) {
            return "phase arrays must contain 1 to 500 matching entries";
        }

        long totalLength = 0;
        for (int i = 0; i < phaseLengths.length; i++) {
            if (phaseLengths[i] < 1 || targetSpms[i] < 1 || breadcrumbs[i] == null) {
                return "phase lengths and target SPM must be positive";
            }
            totalLength += phaseLengths[i];
            if (totalLength > Integer.MAX_VALUE) {
                return "total workout length exceeds the supported maximum";
            }
        }
        return null;
    }

    private void initBeeper(BeeperService beeperService) {
        resetVariables(beeperService);

        countdownToneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        warningToneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);

        sndPref = pref.getString(beeperService.getString(R.string.beep_sound_list_key), beeperService.getString(R.string.beep_sound_default));

        if (sndPref.equals(beeperService.getString(R.string.beep_sound_default))) {
            workoutToneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        } else {
            AssetFileDescriptor afd;
            try {
                afd = beeperService.getAssets().openFd(sndPref + ".wav");
                player = new MediaPlayer();
                player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                player.prepare();
                player.setOnCompletionListener(mp -> mp.seekTo(0));
                player.setLooping(false);
            } catch (IOException e) {
                workoutToneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
                sndPref = beeperService.getString(R.string.beep_sound_default);
                SharedPreferences.Editor editor = pref.edit();
                editor.putString(beeperService.getString(R.string.beep_sound_list_key), beeperService.getString(R.string.beep_sound_default));
                editor.apply();
            }
        }
    }

    private void resetVariables(BeeperService beeperService) {
        pref = PreferenceManager.getDefaultSharedPreferences(beeperService);

        workoutRunning = WaveActivity.WORKOUT_STOP;
        countdownRunning = 0;
        phaseProgress = 0;
        if (activeRun != null) {
            activeRun.currentWorkoutProgress = 0;
        }
        oldWorkoutProgress = 0;
        phaseTrigger = 0;
        color = 0;
        beeps = 0;
        warns = 0;
        locCycleCount = 0;
        locationPoolIsFull = false;
        prevPhasesDistance = 0;
        averageSpeed = 0;
        //Remove sensitive data?
        currentLocation = null;
        startingPhaseLocation = null;

        try {
            acceptableAccuracy = Float.parseFloat(pref.getString(WaveActivity.LOCATION_ACCURACY_ACCEPTABLE, acceptableAccuracy + ""));
        } catch (NumberFormatException e) {
            // Already at default
        }
        if (acceptableAccuracy <= .0001f) {
            acceptableAccuracy = .0001f;
        }
        try {
            speedSampleCount = Integer.parseInt(pref.getString(WaveActivity.SPEED_SAMPLE_COUNT, speedSampleCount + ""));
        } catch (NumberFormatException e) {
            // Already at default
        }
        if (speedSampleCount < 3) {
            speedSampleCount = 3;
        }
        locationPool = new CLocation[speedSampleCount];

        //TODO called 3 times on startup, why?
        cancelTimer(workoutTimer, workoutTimerTask);
        cancelTimer(timeTimer, timeTimerTask);
        cancelTimer(countdownTimer, countdownTimerTask);
        cancelTimer(speedLimitTimer, speedLimitTimerTask);
        speedLimitTimer = null;
        speedLimitTimerTask = null;
        warningIsRunning = false;

        countdownCyclesElapsed = 0;
        try {
            countdownDurationMs = Integer.parseInt(pref.getString(WaveActivity.COUNTDOWN_DURATION, "3")) * 1000;
        } catch (NumberFormatException e) {
            countdownDurationMs = 3000;
        }
        countdownCycleDuration = 100;
        countdownCyclesTotal = countdownDurationMs / countdownCycleDuration;

        //Init speed unit setting if necessary
        String speedUnit = pref.getString(WaveActivity.SPEED_UNIT, "");
        if (!speedUnit.equals(WaveActivity.SPEED_500M_SETTING) && !speedUnit.equals(WaveActivity.SPEED_MS_SETTING)) {
            pref.edit().putString(WaveActivity.SPEED_UNIT, WaveActivity.SPEED_MS_SETTING).apply();
        }
        pref.edit().putInt(WaveActivity.WORKOUT_PROGRESS,
                activeRun == null ? 0 : activeRun.currentWorkoutProgress).apply();
        pref.edit().putInt(WaveActivity.PHASE_INDEX, 0)
                .putInt(WaveActivity.PHASES_TOTAL, 0)
                .putString(WaveActivity.PHASE_BREADCRUMB, "")
                .putInt(WaveActivity.PHASE_PROGRESS, 0)
                .putInt(WaveActivity.PHASE_LENGTH, 0)
                .putInt(WaveActivity.WORKOUT_LENGTH, 0)
                .apply();
        pref.edit().putBoolean(WaveActivity.GPS_LOCKING, false).apply();
        flushUI();

        phaseProgress = 0;
        locationAccuracy = 500;     //Init to extremely inaccurate value, i.e. no accuracy
        pref.edit().putFloat(WaveActivity.LOCATION_ACCURACY, locationAccuracy).apply();

        phaseStartElapsedRealtime = SystemClock.elapsedRealtime();
        flushUI();
    }

    private void flushUI() {
        pref.edit().putInt(WaveActivity.OPERATION_SETTING, workoutRunning).apply();
        pref.edit().putInt(WaveActivity.COUNTDOWN_DURATION_LEFT, countdownRunning).apply();
        pref.edit().putFloat(WaveActivity.CURRENT_SPEED, averageSpeed).apply();
        Boolean bool = pref.getBoolean(WaveActivity.SWITCH_SETTING, true);
        pref.edit().putBoolean(WaveActivity.SWITCH_SETTING, !bool).apply();
    }

    private void startTheTempo(final BeeperService beeperService, final long generation) {
        synchronized (runLock) {
            if (!isCurrentRun(generation)) {
                return;
            }
            pref.edit().putInt(WaveActivity.SPM_SETTING, spm).apply();
            phaseProgress = 0;
            pref.edit().putInt(WaveActivity.PHASE_PROGRESS, 0).apply();

            strokeDuration = SpmUtilities.spmToMilis(spm);

            cancelTimer(workoutTimer, workoutTimerTask);
            if (mSppType.equals(SPP_TYPE_SECONDS)) {
                timeTimerPhaseProgress = 0;
                cancelTimer(timeTimer, timeTimerTask);
                startTimeTimerTask(beeperService, generation);
                timeTimer = new Timer();
                timeTimer.scheduleAtFixedRate(timeTimerTask, 1, 1000);
            }

            workoutTimerTask = new TimerTask() {
                @Override
                public void run() {
                    synchronized (runLock) {
                        if (!isCurrentRun(generation)) {
                            return;
                        }

                        if (pref.getInt(WaveActivity.OPERATION_SETTING,
                                WaveActivity.WORKOUT_STOP) == WaveActivity.WORKOUT_STOP) {
                            endWorkout(beeperService);
                            return;
                        }
                        if (workoutRunning != WaveActivity.WORKOUT_INTERVAL
                                && workoutRunning != WaveActivity.WORKOUT_LAST
                                && workoutRunning != WaveActivity.WORKOUT_SIMPLE) {
                            return;
                        }

                        if (workoutRunning != WaveActivity.WORKOUT_SIMPLE) {
                            switch (mSppType) {
                                case SPP_TYPE_STROKES: {
                                    if (phaseProgress >= phaseTrigger) {
                                        phaseProgress = 0;
                                        if (workoutRunning != WaveActivity.WORKOUT_INTERVAL) {
                                            endWorkout(beeperService);
                                            return;
                                        }
                                        autoProgress(beeperService, generation);
                                        return;
                                    } else {
                                        phaseProgress++;
                                        activeRun.currentWorkoutProgress++;
                                        pref.edit().putInt(WaveActivity.WORKOUT_PROGRESS,
                                                        activeRun.currentWorkoutProgress)
                                                .putInt(WaveActivity.PHASE_PROGRESS, phaseProgress)
                                                .apply();
                                    }
                                    break;
                                }
                                case SPP_TYPE_METERS: {
                                    if (phaseProgress >= phaseTrigger) {
                                        cancel();
                                        startingPhaseLocation = currentLocation;
                                        prevPhasesDistance += phaseTrigger;
                                        activeRun.currentWorkoutProgress = prevPhasesDistance;
                                        pref.edit().putInt(WaveActivity.WORKOUT_PROGRESS,
                                                        activeRun.currentWorkoutProgress)
                                                .apply();
                                        phaseProgress = 0;
                                        if (workoutRunning != WaveActivity.WORKOUT_INTERVAL) {
                                            endWorkout(beeperService);
                                            return;
                                        }
                                        autoProgress(beeperService, generation);
                                        return;
                                    } else {
                                        activeRun.currentWorkoutProgress =
                                                prevPhasesDistance + phaseProgress;
                                        pref.edit().putInt(WaveActivity.WORKOUT_PROGRESS,
                                                        activeRun.currentWorkoutProgress)
                                                .putInt(WaveActivity.PHASE_PROGRESS, phaseProgress)
                                                .apply();
                                    }
                                    break;
                                }
                            }
                        }
                        if (!isCurrentRun(generation)) {
                            return;
                        }
                        if (sndPref.equals(beeperService.getString(R.string.beep_sound_default))) {
                            workoutToneGen.startTone(
                                    ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 150);
                        } else {
                            player.seekTo(0);
                            player.start();
                        }
                        pref.edit().putInt(WaveActivity.BEEP, ++beeps).apply();
                    }
                }
            };

            workoutTimer = new Timer();
            workoutTimer.scheduleAtFixedRate(workoutTimerTask, 1, strokeDuration);
        }
    }

    private void startTimeTimerTask(final BeeperService beeperService, final long generation) {
        timeTimerTask = new TimerTask() {
            @Override
            public void run() {
                synchronized (runLock) {
                    if (!isCurrentRun(generation)) {
                        return;
                    }
                    long elapsedMillis = SystemClock.elapsedRealtime()
                            - phaseStartElapsedRealtime;
                    if (isTimePhaseComplete(elapsedMillis, phaseTrigger)) {
                        activeRun.currentWorkoutProgress = oldWorkoutProgress + phaseTrigger;
                        oldWorkoutProgress = activeRun.currentWorkoutProgress;
                        pref.edit().putInt(WaveActivity.WORKOUT_PROGRESS,
                                activeRun.currentWorkoutProgress).apply();
                        cancel();
                        phaseProgress = 0;

                        if (workoutRunning == WaveActivity.WORKOUT_INTERVAL) {
                            autoProgress(beeperService, generation);
                        } else {
                            endWorkout(beeperService);
                        }
                        return;
                    }

                    int elapsedSeconds = (int) (elapsedMillis / 1000L);
                    phaseProgress = elapsedSeconds;
                    activeRun.currentWorkoutProgress = oldWorkoutProgress + elapsedSeconds;
                    pref.edit().putInt(WaveActivity.WORKOUT_PROGRESS,
                                    activeRun.currentWorkoutProgress)
                            .putInt(WaveActivity.PHASE_PROGRESS, elapsedSeconds)
                            .apply();
                    timeTimerPhaseProgress = elapsedSeconds;
                }
            }
        };
    }

    static boolean isTimePhaseComplete(long elapsedMillis, int phaseLengthSeconds) {
        long targetMillis = ((long) phaseLengthSeconds) * 1000L;
        return elapsedMillis >= targetMillis;
    }

    private void startSpeedLimit(final BeeperService beeperService) {
        long generation;
        synchronized (runLock) {
            if (activeRun == null) {
                return;
            }
            generation = activeRun.generation;
        }
        speedLimitTimerTask = new TimerTask() {
            @Override
            public void run() {
                synchronized (runLock) {
                    if (!isCurrentRun(generation)) {
                        return;
                    }
                    pref.edit().putInt(WaveActivity.WARN, ++warns).apply();
                    warningToneGen.startTone(ToneGenerator.TONE_CDMA_ANSWER, 50);
                }
            }
        };
        speedLimitTimer = new Timer();
        speedLimitTimer.scheduleAtFixedRate(speedLimitTimerTask, 1, 1000);
    }

    //TODO cancel countdown on interrupt
    private void startCountdown(final BeeperService beeperService, final long generation) {

        countdownTimerTask = new TimerTask() {

            @Override
            public void run() {
                synchronized (runLock) {
                    if (!isCurrentRun(generation)) {
                        return;
                    }
                    if (pref.getBoolean(WaveActivity.GPS_LOCKING, false)) {
                        return;
                    }

                    if (countdownCyclesElapsed % 10 == 0) {
                        pref.edit().putInt(WaveActivity.COUNTDOWN_DURATION_LEFT,
                                countdownDurationMs - countdownCyclesElapsed * 100).apply();
                    }
                    if (++countdownCyclesElapsed >= countdownCyclesTotal) {
                        pref.edit().putInt(WaveActivity.COUNTDOWN_DURATION_LEFT, 0).apply();
                        autoProgress(beeperService, generation);
                        cancelTimer(countdownTimer, this);
                    } else {
                        try {
                            countdownToneGen.startTone(ToneGenerator.TONE_CDMA_INTERCEPT, 100);
                        } catch (IllegalStateException exception) {
                            Log.d("TONE GENERATOR", "Starting tone exception", exception);
                        }
                    }
                }
            }
        };

        countdownTimer = new Timer();
        countdownTimer.scheduleAtFixedRate(countdownTimerTask, 1, countdownCycleDuration);
    }

    //Initialize workout phase
    private void autoProgress(BeeperService beeperService, long generation) {
        RunState run = activeRun;
        if (!isCurrentRun(generation) || run.phaseLengths == null) {
            return;
        }
        if (run.currentPhaseIndex >= run.totalPhases) {
            endWorkout(beeperService);
            return;
        }
        final int[] COLOR_PROGRESSION = new int[]{
                Color.GREEN, Color.YELLOW
        };

        run.currentPhaseIndex++;
        workoutRunning = run.currentPhaseIndex == run.totalPhases
                ? WaveActivity.WORKOUT_LAST : WaveActivity.WORKOUT_INTERVAL;
        run.currentBreadcrumb = run.breadcrumbs[run.currentPhaseIndex - 1];

        if ((run.currentPhaseIndex & 1) == 1) {
            color = COLOR_PROGRESSION[0];
        } else {
            color = COLOR_PROGRESSION[1];
        }

        startPhase(
                run.phaseLengths[run.currentPhaseIndex - 1],
                run.targetSpms[run.currentPhaseIndex - 1],
                beeperService,
                generation
        );
    }

    //Start phase
    private void startPhase(int lengthTrigger, int tempo, BeeperService beeperService,
                            long generation) {
        if (!isCurrentRun(generation)) {
            return;
        }
        phaseStartElapsedRealtime = SystemClock.elapsedRealtime();
        phaseTrigger = lengthTrigger;
        spm = tempo;

        if (Objects.equals(mSppType, SPP_TYPE_METERS)) {
            startingPhaseLocation = currentLocation;
        }

        pref.edit().putInt(WaveActivity.OPERATION_SETTING, workoutRunning).apply();
        pref.edit().putInt(WaveActivity.CURRENT_COLOR, color).apply();
        pref.edit().putInt(WaveActivity.PHASE_LENGTH, lengthTrigger)
                .putInt(WaveActivity.SPM_SETTING, tempo)
                .putInt(WaveActivity.PHASE_INDEX, activeRun.currentPhaseIndex)
                .putInt(WaveActivity.PHASES_TOTAL, activeRun.totalPhases)
                .putString(WaveActivity.PHASE_BREADCRUMB, activeRun.currentBreadcrumb)
                .putInt(WaveActivity.PHASE_PROGRESS, 0)
                .apply();

        Boolean bool = pref.getBoolean(WaveActivity.SWITCH_SETTING, true);
        pref.edit().putBoolean(WaveActivity.SWITCH_SETTING, !bool).apply();

        timeTimerPhaseProgress = 0;
        startTheTempo(beeperService, generation);
    }

    /**
     * Helper method to cancel running Timer, if any
     */
    private void cancelTimer(Timer timer, TimerTask timerTask) {
        if (timer != null) {
            timer.cancel();
        }
        if (timerTask != null) {
            timerTask.cancel();
        }
    }

    /**
     * Reset the current workout and stop beeping.
     */
    private void endWorkout(BeeperService beeperService) {
        synchronized (runLock) {
            invalidateRunLocked(beeperService);
            resetVariables(beeperService);
            workoutTimer = null;
            workoutTimerTask = null;
            timeTimer = null;
            timeTimerTask = null;
            countdownTimer = null;
            countdownTimerTask = null;

            pref.edit().putInt(WaveActivity.SPM_SETTING, 0).apply();
            NotificationUtils.clearAllNotifications(beeperService);

            flushUI();

            releaseAudioResources();
            beeperService.stopSelf();
        }
    }

    private void releaseAudioResources() {
        if (player != null) {
            player.release();
            player = null;
        }
        if (countdownToneGen != null) {
            countdownToneGen.release();
            countdownToneGen = null;
        }
        if (workoutToneGen != null) {
            workoutToneGen.release();
            workoutToneGen = null;
        }
        if (warningToneGen != null) {
            warningToneGen.release();
            warningToneGen = null;
        }
    }

    private void invalidateRunLocked(BeeperService beeperService) {
        runGeneration++;
        activeRun = null;
        cancelTimer(workoutTimer, workoutTimerTask);
        cancelTimer(timeTimer, timeTimerTask);
        cancelTimer(countdownTimer, countdownTimerTask);
        workoutTimer = null;
        workoutTimerTask = null;
        timeTimer = null;
        timeTimerTask = null;
        countdownTimer = null;
        countdownTimerTask = null;
        if (beeperService.locationManager != null && beeperService.locListener != null) {
            beeperService.locationManager.removeUpdates(beeperService.locListener);
            beeperService.locationManager = null;
            beeperService.locListener = null;
        }
    }

    private boolean isCurrentRun(long generation) {
        return activeRun != null && activeRun.generation == generation
                && runGeneration == generation;
    }

    private void initLocation(BeeperService beeperService, long generation) {

        PackageManager manager = beeperService.getPackageManager();
        int permission = manager.checkPermission("android.permission.ACCESS_FINE_LOCATION", "com.batyanko.strokeratecoach");

        boolean enabled = pref.getBoolean(USE_LOC_KEY, false);
        boolean hasPermission = (permission == PackageManager.PERMISSION_GRANTED);

        if (!hasPermission || !enabled) {
            return;
        }

        beeperService.locationManager = (LocationManager) beeperService.getSystemService(Context.LOCATION_SERVICE);
        beeperService.locListener = new TehLocListener(generation);
        beeperService.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 200, 1, beeperService.locListener);
    }

    private void updateAverageSpeed() {
        if (currentLocation == null) return;
        locationPool[locCycleCount] = currentLocation;

        if (locCycleCount == speedSampleCount - 1) {
            locCycleCount = 0;
            locationPoolIsFull = true;
        } else locCycleCount++;
        if (locationPoolIsFull && locationPool[locCycleCount] != null) {
            long timeDiffMillis =
                    (currentLocation.getElapsedRealtimeNanos() - locationPool[locCycleCount].getElapsedRealtimeNanos()) / 1000000;

            averageSpeed = (currentLocation.distanceTo(locationPool[locCycleCount]) / (((float) timeDiffMillis) / 1000));

            pref.edit().putFloat(WaveActivity.CURRENT_SPEED, averageSpeed).apply();
        }
    }

    public class TehLocListener implements IBaseGpsListener {

        private final long generation;

        private TehLocListener(long generation) {
            this.generation = generation;
        }

        private void updateLocation(CLocation location) {
            if (!isCurrentRun(generation)) {
                return;
            }
            location.setUseMetricunits(true);
            currentLocation = location;

            if (mSppType.equals(SPP_TYPE_METERS)) {

                locationAccuracy = location.getAccuracy();
                pref.edit().putFloat(WaveActivity.LOCATION_ACCURACY, locationAccuracy).apply();

                if (startingPhaseLocation == null && locationAccuracy <= acceptableAccuracy) {
                    completeLocking();
                } else if (startingPhaseLocation != null) {
                    phaseProgress = (int) location.distanceTo(startingPhaseLocation);
                }
            }
        }

        private boolean useMetricUnits() {
            return true;
        }

        @Override
        public void onLocationChanged(Location location) {
            if (location == null) {
                return;
            }
            synchronized (runLock) {
                if (!isCurrentRun(generation)) {
                    return;
                }
                CLocation myLocation = new CLocation(location, this.useMetricUnits());
                this.updateLocation(myLocation);
                updateAverageSpeed();
            }
        }

        @Override
        public void onProviderDisabled(String provider) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onGpsStatusChanged(int event) {

        }
    }

    public static void completeLocking() {
        pref.edit().putBoolean(WaveActivity.GPS_LOCKING, false).apply();
        startingPhaseLocation = currentLocation;
    }
}
