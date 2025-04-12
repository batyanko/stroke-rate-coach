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
import static com.batyanko.strokeratecoach.WaveActivity.CUSTOM_SOUND;
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

    public static final String EXTRA_WORKOUT_SPP = "workout_spp";   //Units per phase
    public static final String EXTRA_WORKOUT_GEARS = "workout_gears";
    public static final String EXTRA_WORKOUT_SPP_TYPE = "workout-spp-type";
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

    private static Timer workoutTimer;
    private static TimerTask workoutTimerTask;

    private static Timer timeTimer;
    private static TimerTask timeTimerTask;

    private static Timer countdownTimer;
    private static TimerTask countdownTimerTask;

    private static TimerTask speedLimitTimerTask;

    private static Timer speedLimitTimer;
    //autoWave values
    private static int workoutRunning;     //0=off, 1=wave(depreciated), 2=interval, 3=simple 9=lastPhase
    private static int countdownRunning;    //0=off, 1=on
    private static int phaseTrigger;
    private static int phase;    //phase = 0 = wave not running
    private static int phasesTotal;
    private static int color;
    private static int phaseProgress;
    private static int workoutProgress;
    private static int workoutLength;       //Length (in strokes, meters or seconds)

    private String mSppType;

    private static int[] sppSettings;

    private static int[] gearSettings;

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

    private static int prevPhasesDistance;

    //TODO make static?
    private long phaseStartTime;
    private int timeTimerPhaseProgress;
    public static int countdownCyclesTotal;
    public static int countdownCyclesElapsed;
    private int countdownCycleDuration;
    private int countdownDurationMs;
    private String sndPref;
    private static int oldWorkoutProgress;

    private static int beeps;
    private static int warns;
    private static boolean warningIsRunning = false;

    void executeTask(BeeperService beeperService, String action,
                     int[] sppSettings, int[] gearSettings, String sppType) {
        if (action.equals(ACTION_START_BEEP)) {
            NotificationUtils.showWorkoutNotification(beeperService);
            mSppType = sppType;
            initBeeper(beeperService);
            initLocation(beeperService);

            if (Objects.equals(mSppType, SPP_TYPE_METERS)) {
                boolean enabled = pref.getBoolean(USE_LOC_KEY, false);
                if (!enabled) {
                    endWorkout(beeperService);
                    flushUI();
                    Toast.makeText(beeperService.getBaseContext(), "Distance based workout.\nPlease enable location in settings.", LENGTH_LONG).show();
                    return;
                }
                pref.edit().putBoolean(WaveActivity.GPS_LOCKING, true).apply();
            }
            if (Objects.equals(mSppType, SPP_TYPE_SECONDS)) {
                startTimeTimerTask(beeperService);
            }

            if (gearSettings != null) {
                if (sppSettings == null) {  //Manual SPM setting
                    //TODO show speed?
                    workoutRunning = WaveActivity.WORKOUT_SIMPLE;
                    flushUI();
                    spm = gearSettings[0];
                    startTheTempo(beeperService);
                } else {                    //Preset workout
                    workoutRunning = WaveActivity.WORKOUT_INTERVAL;
                    pref.edit().putInt(WaveActivity.OPERATION_SETTING, workoutRunning).apply();
                    BeeperTasks.sppSettings = sppSettings;
                    BeeperTasks.gearSettings = gearSettings;
                    workoutLength = 0;
                    for (int i : sppSettings) {
                        workoutLength += i;     //Be it strokes, metres or seconds
                    }
                    pref.edit().putInt(WaveActivity.WORKOUT_LENGTH, workoutLength).apply();
                    startCountdown(beeperService);
                }
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
        workoutProgress = 0;
        oldWorkoutProgress = 0;
        phaseTrigger = 0;
        phase = 0;
        phasesTotal = 0;
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
        pref.edit().putInt(WaveActivity.WORKOUT_PROGRESS, workoutProgress).apply();
        pref.edit().putBoolean(WaveActivity.GPS_LOCKING, false).apply();
        flushUI();

        phaseProgress = 0;
        locationAccuracy = 500;     //Init to extremely inaccurate value, i.e. no accuracy
        pref.edit().putFloat(WaveActivity.LOCATION_ACCURACY, locationAccuracy).apply();

        phaseStartTime = System.currentTimeMillis();
        flushUI();
    }

    private void flushUI() {
        pref.edit().putInt(WaveActivity.OPERATION_SETTING, workoutRunning).apply();
        pref.edit().putInt(WaveActivity.COUNTDOWN_DURATION_LEFT, countdownRunning).apply();
        pref.edit().putFloat(WaveActivity.CURRENT_SPEED, averageSpeed).apply();
        Boolean bool = pref.getBoolean(WaveActivity.SWITCH_SETTING, true);
        pref.edit().putBoolean(WaveActivity.SWITCH_SETTING, !bool).apply();
    }

    private void startTheTempo(final BeeperService beeperService) {
        pref.edit().putInt(WaveActivity.SPM_SETTING, spm).apply();
        phaseProgress = 0;

        strokeDuration = SpmUtilities.spmToMilis(spm);

        cancelTimer(workoutTimer, workoutTimerTask);
        if (mSppType.equals(SPP_TYPE_SECONDS)) {
            timeTimerPhaseProgress = 0;
            cancelTimer(timeTimer, timeTimerTask);
            startTimeTimerTask(beeperService);
            timeTimer = new Timer();
            timeTimer.scheduleAtFixedRate(timeTimerTask, 1, 1000);
        }

        //TODO create Task just once?
        workoutTimerTask = new TimerTask() {
            @Override
            public void run() {

                //Abort if OPERATION set to WORKOUT_STOP. Avoid zombie service in the background?
                if (pref.getInt(WaveActivity.OPERATION_SETTING, WaveActivity.WORKOUT_STOP)
                        == WaveActivity.WORKOUT_STOP) {
                    endWorkout(beeperService);
                } else if (workoutRunning == WaveActivity.WORKOUT_INTERVAL
                        || workoutRunning == WaveActivity.WORKOUT_LAST) {

                    switch (mSppType) {
                        //Handle stroke-based workouts
                        case SPP_TYPE_STROKES: {
                            if (phaseProgress >= phaseTrigger) {
                                phaseProgress = 0;
                                switch (workoutRunning) {
                                    case WaveActivity.WORKOUT_INTERVAL: {
                                        phaseProgress++;
                                        autoProgress(beeperService);
                                        break;
                                    }
                                    default:
                                        endWorkout(beeperService);
                                        return;
                                }
                            } else {
                                phaseProgress++;
                                workoutProgress++;
                                pref.edit().putInt(WaveActivity.WORKOUT_PROGRESS, workoutProgress).apply();
                                pref.edit().putInt(WaveActivity.PHASE_PROGRESS, phaseProgress).apply();
                            }
                            break;
                        }
                        //Handle distance-based workouts
                        case SPP_TYPE_METERS: {
                            if (phaseProgress >= phaseTrigger) {
                                //TODO avoid extra beep after workout end?
                                cancel();
                                startingPhaseLocation = currentLocation;
                                prevPhasesDistance += phaseTrigger;
                                workoutProgress = prevPhasesDistance;
                                pref.edit().putInt(WaveActivity.WORKOUT_PROGRESS, workoutProgress).apply();
                                phaseProgress = 0;
                                switch (workoutRunning) {
                                    case WaveActivity.WORKOUT_INTERVAL:
                                        autoProgress(beeperService);
                                        break;
                                    default:
                                        endWorkout(beeperService);
                                        return;
                                }
                            } else {
                                workoutProgress = prevPhasesDistance + phaseProgress;
                                pref.edit().putInt(WaveActivity.WORKOUT_PROGRESS, workoutProgress).apply();
                                pref.edit().putInt(WaveActivity.PHASE_PROGRESS, phaseProgress).apply();
                            }
                            break;
                        }
                    }
                }
                if (sndPref.equals(beeperService.getString(R.string.beep_sound_default))) {
                    workoutToneGen.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 150);
                } else {
                    player.seekTo(0);
                    player.start();
                }
                // Register OnErrorListener
                pref.edit().putInt(WaveActivity.BEEP, ++beeps).apply();
            }
        };

        workoutTimer = new Timer();
        workoutTimer.scheduleAtFixedRate(workoutTimerTask, 1, strokeDuration);
    }

    //TimerTask for time-based workouts
    private void startTimeTimerTask(final BeeperService beeperService) {
        timeTimerTask = new TimerTask() {
            @Override
            public void run() {
                if (phase != 0 && timeTimerPhaseProgress == 0) {
                    workoutProgress--;
                }

                long currentTime = System.currentTimeMillis();
                phaseProgress = (int) (currentTime - phaseStartTime);
                if (phaseProgress >= phaseTrigger * 1000) {
                    //adjust for incomplete seconds in a phase
                    workoutProgress = oldWorkoutProgress + phaseTrigger;
                    oldWorkoutProgress = workoutProgress;
                    pref.edit().putInt(WaveActivity.WORKOUT_PROGRESS, workoutProgress).apply();
                    cancel();
                    phaseStartTime = currentTime;
                    phaseProgress = 0;

                    if (workoutRunning == WaveActivity.WORKOUT_INTERVAL) {
                        autoProgress(beeperService);
                    } else {
                        endWorkout(beeperService);
                    }
                    return;
                }

                pref.edit().putInt(WaveActivity.WORKOUT_PROGRESS, ++workoutProgress).apply();
                pref.edit().putInt(WaveActivity.PHASE_PROGRESS, timeTimerPhaseProgress++).apply();
            }
        };
    }

    private void startSpeedLimit(final BeeperService beeperService) {
        speedLimitTimerTask = new TimerTask() {
            @Override
            public void run() {
                //TODO check SpeedLimit prefs to avoid zombie beepers?
                pref.edit().putInt(WaveActivity.WARN, ++warns).apply();
                warningToneGen.startTone(ToneGenerator.TONE_CDMA_ANSWER, 50);
            }
        };
        speedLimitTimer = new Timer();
        speedLimitTimer.scheduleAtFixedRate(speedLimitTimerTask, 1, 1000);
    }

    //TODO cancel countdown on interrupt
    private void startCountdown(final BeeperService beeperService) {

        countdownTimerTask = new TimerTask() {

            @Override
            public void run() {
                //Wait if locking distance location
                if (pref.getBoolean(WaveActivity.GPS_LOCKING, false)) return;

                if (countdownCyclesElapsed % 10 == 0) {
                    pref.edit().putInt(WaveActivity.COUNTDOWN_DURATION_LEFT,
                                    countdownDurationMs - countdownCyclesElapsed * 100)
                            .apply();
                }
                if (++countdownCyclesElapsed >= countdownCyclesTotal) {
                    pref.edit().putInt(WaveActivity.COUNTDOWN_DURATION_LEFT, 0).apply();
                    autoProgress(beeperService);
                    cancelTimer(countdownTimer, this);
                } else {
                    try {
                        countdownToneGen.startTone(ToneGenerator.TONE_CDMA_INTERCEPT, 100);
                    } catch (Exception e) {
                        Log.d("TONE GENERATOR", "Starting tone exception");
//                        Catch undocumented ToneGenerator exception, starting tone after release()
                    }
                }
            }
        };

        countdownTimer = new Timer();
        countdownTimer.scheduleAtFixedRate(countdownTimerTask, 1, countdownCycleDuration);
    }

    //Initialize workout phase
    private void autoProgress(BeeperService beeperService) {

        //Set exercise to autoProgress;
        workoutRunning = WaveActivity.WORKOUT_INTERVAL;

        phasesTotal = gearSettings.length;

        final int[] COLOR_PROGRESSION = new int[]{
                Color.GREEN, Color.YELLOW
        };

        //Phase number starts from 1
        phase++;

        if (phase == phasesTotal) {
            workoutRunning = WaveActivity.WORKOUT_STOP;
        }

        if ((phase & 1) == 1) {
            color = COLOR_PROGRESSION[0];
        } else {
            color = COLOR_PROGRESSION[1];
        }

        startPhase(
                sppSettings[phase - 1],
                gearSettings[phase - 1],
                beeperService
        );
    }

    //Start phase
    private void startPhase(int lengthTrigger, int tempo, BeeperService beeperService) {
        //Init starting time for time-based workouts
        phaseStartTime = System.currentTimeMillis();

        phaseTrigger = lengthTrigger;
        spm = tempo;

        if (phase == phasesTotal) {
            workoutRunning = WaveActivity.WORKOUT_LAST;
        }

        pref.edit().putInt(WaveActivity.OPERATION_SETTING, workoutRunning).apply();

        pref.edit().putInt(WaveActivity.CURRENT_COLOR, color).apply();

        pref.edit().putInt(WaveActivity.PHASE_LENGTH, sppSettings[phase - 1]).apply();

        Boolean bool = pref.getBoolean(WaveActivity.SWITCH_SETTING, true);
        pref.edit().putBoolean(WaveActivity.SWITCH_SETTING, !bool).apply();

        startTheTempo(beeperService);
    }

    /**
     * Helper method to cancel running Timer, if any
     */
    private void cancelTimer(Timer timer, TimerTask timerTask) {
        try {
            timer.cancel();
            timerTask.cancel();
        } catch (IllegalStateException e) {
            Log.d("CANCEL TIMER", "IllegalStateException");
        } catch (NullPointerException e) {
            Log.d("CANCEL TIMER", "NullPointerException");
        }
    }

    /**
     * Reset the current workout and stop beeping.
     */
    private void endWorkout(BeeperService beeperService) {
        resetVariables(beeperService);
        cancelTimer(workoutTimer, workoutTimerTask);
        cancelTimer(timeTimer, timeTimerTask);
        cancelTimer(countdownTimer, countdownTimerTask);
        cancelTimer(speedLimitTimer, speedLimitTimerTask);

        pref.edit().putInt(WaveActivity.SPM_SETTING, 0).apply();
        NotificationUtils.clearAllNotifications(beeperService);

        flushUI();

        if (beeperService.locationManager != null) {
            beeperService.locationManager.removeUpdates(beeperService.locListener);
            beeperService.locationManager = null;
        }

        if (player != null && pref.getBoolean(CUSTOM_SOUND, false)) {
            player.release();
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
        beeperService.stopSelf();
    }

    private void initLocation(BeeperService beeperService) {

        PackageManager manager = beeperService.getPackageManager();
        int permission = manager.checkPermission("android.permission.ACCESS_FINE_LOCATION", "com.batyanko.strokeratecoach");

        boolean enabled = pref.getBoolean(USE_LOC_KEY, false);
        boolean hasPermission = (permission == PackageManager.PERMISSION_GRANTED);

        if (!hasPermission || !enabled) {
            return;
        }

        beeperService.locationManager = (LocationManager) beeperService.getSystemService(Context.LOCATION_SERVICE);
        beeperService.locListener = new TehLocListener(beeperService);
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

        Context context;

        private TehLocListener(Context context) {
            this.context = context;
        }

        private void updateLocation(CLocation location) {
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
            if (location != null) {
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
