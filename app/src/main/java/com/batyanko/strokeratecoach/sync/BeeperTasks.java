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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.batyanko.strokeratecoach.Speed.CLocation;
import com.batyanko.strokeratecoach.Speed.IBaseGpsListener;
import com.batyanko.strokeratecoach.Utils.SpmUtilities;
import com.batyanko.strokeratecoach.WaveActivity;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by batyanko on 9/4/17.
 */

public class BeeperTasks {

    public static final String EXTRA_WORKOUT_SPP = "workout_spp";
    public static final String EXTRA_WORKOUT_GEARS = "workout_gears";
    public static final String EXTRA_WORKOUT_SPP_TYPE = "workout-spp-type";
    public static final String ACTION_START_BEEP = "start-beep";
    public static final String ACTION_STOP_BEEP = "stop-beep";
    public static final String ACTION_CHECK_SERVICE = "check-service";
    public static final String ACTION_JUST_BIND = "just-bind";

    //SPP (strokes or stuff per phase) unit types
    public static final int SPP_TYPE_STROKES = 0;
    public static final int SPP_TYPE_METERS = 1;


    public static final int SPP_TYPE_SECONDS = 2;

    //VALUES FROM WaveActivity

    /* Global spm setting to hold current spm */
    public static int spm = 22;
    //Variables used in beeping workoutTimer setup
    private static long strokeDuration;
    public static String spmString;
    private static ToneGenerator toneGen1;

    private static ToneGenerator countdownToneGen;
    private static Timer workoutTimer;

    private static TimerTask workoutTimerTask;
    private static Timer timeTimer;

    private static TimerTask timeTimerTask;
    private static Timer countdownTimer;


    private static TimerTask countdownTimerTask;
    //autoWave values
    private static int workoutRunning;     //0=off, 1=autoWave, 2=autoProgress, 9=lastPhase
    private static int countdownRunning;    //0=off, 1=on
    private static int totalWorkoutCount;
    private static int phaseTrigger;
    private static int phase;    //phase = 0 = wave not running
    private static int phasesTotal;
    private static int color;
    private static int phaseProgress;


    private int mSppType;

    private static int[] SPP_SETTINGS;

    private static int[] GEAR_SETTINGS;

    private static SharedPreferences pref;

    //END OF COPIED VALUES

    private CLocation currentLocation;
    private CLocation startingPhaseLocation;
    private static final float ACCEPTABLE_ACCURACY = 5;
    private static float locationAccuracy;
    private static float currentSpeed;

    private CLocation[] locationPool;

    private static float averageSpeed;  //In m/s

    private static int locCycleCount;
    //TODO add preference for sample count
    private static final int SPEED_SAMPLE_COUNT = 10;

    private static boolean locationPoolIsFull;

    //TODO make static?
    private long startTime;
    private int timeTimerProgress;
    private int countdownCyclesTotal;
    private int countdownCycles;
    private int countdownCycleDuration;
    private int countdownDuration;
    private static int beeps;

    void executeTask(BeeperService beeperService, String action,
                     int[] sppSettings, int[] gearSettings, int sppType) {
        if (action.equals(ACTION_START_BEEP)) {
            Log.d("TEHSERVICE", "at execute task");
            mSppType = sppType;
            initBeeper(beeperService);
            initLocation(beeperService);

            if (mSppType == SPP_TYPE_METERS) {
//                initLocation(beeperService);
            }
            if (mSppType == SPP_TYPE_SECONDS) {
                initTimeTimerTask();
            }

            if (gearSettings != null) {
                if (sppSettings == null) {  //Manual SPM setting
                    //TODO show speed?
                    flushUI();
                    spm = gearSettings[0];
                    startTheTempo(beeperService);
                } else {                    //Preset workout
                    SPP_SETTINGS = sppSettings;
                    GEAR_SETTINGS = gearSettings;
                    startCountdown(beeperService);
//                    autoProgress(beeperService);
                }
            }
        } else if (action.equals(ACTION_STOP_BEEP)) {
            Log.d("TEHSERVICE", "at execute task");
            endWorkout(beeperService);
        } else if (action.equals(ACTION_CHECK_SERVICE)) {
            Log.d("TEHSERVICE", "at execute task");
        }
    }

    private void initBeeper(BeeperService beeperService) {

        resetVariables(beeperService);

        toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        countdownToneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
    }

    private void initTimeTimerTask() {
        timeTimerTask = new TimerTask() {
            @Override
            public void run() {
                pref.edit().putInt(WaveActivity.PHASE_PROGRESS, timeTimerProgress++).apply();
            }
        };
    }

    private void resetVariables(BeeperService beeperService) {
        pref = PreferenceManager.getDefaultSharedPreferences(beeperService);

        workoutRunning = 0;     //0=off, 1=autoWave, 2=autoProgress, 9=lastPhase
        countdownRunning = 0;
        phaseProgress = 0;
        totalWorkoutCount = 0;
        phaseTrigger = 0;
        phase = 0;
        phasesTotal = 0;
        color = 0;
        beeps = 0;
        locationPool = new CLocation[SPEED_SAMPLE_COUNT];
        locCycleCount = 0;
        locationPoolIsFull = false;

        cancelTimer(workoutTimer, workoutTimerTask);
        cancelTimer(timeTimer, timeTimerTask);
        cancelTimer(countdownTimer, countdownTimerTask);

        countdownCycles = 0;
        //TODO add setting for countdown cycle length / frequency?
        countdownDuration = pref.getInt(WaveActivity.COUNTDOWN_DURATION, 3000);
        countdownCycleDuration = 100;
        countdownCyclesTotal = countdownDuration / countdownCycleDuration;

        phaseProgress = 0;
        locationAccuracy = 500;     //Init at an extremely inaccurate value, i.e. no accuracy

        startTime = System.currentTimeMillis();
    }

    private void flushUI() {
        pref.edit().putInt(WaveActivity.OPERATION_SETTING, workoutRunning).apply();
        pref.edit().putInt(WaveActivity.COUNTDOWN_DURATION_LEFT, countdownRunning).apply();
        Boolean bool = pref.getBoolean(WaveActivity.SWITCH_SETTING, true);
        pref.edit().putBoolean(WaveActivity.SWITCH_SETTING, !bool).apply();
    }

    private void startTheTempo(final BeeperService beeperService) {
        Log.d("TEHSERVICE", "at startTheTempo");

        pref.edit().putInt(WaveActivity.SPM_SETTING, spm).apply();
        phaseProgress = 0;

        strokeDuration = SpmUtilities.spmToMilis(spm);

        cancelTimer(workoutTimer, workoutTimerTask);
        if (mSppType == SPP_TYPE_SECONDS) {
            timeTimerProgress = 0;
            cancelTimer(timeTimer, timeTimerTask);
            initTimeTimerTask();
            timeTimer = new Timer();
            timeTimer.scheduleAtFixedRate(timeTimerTask, 1, 1000);
        }

        workoutTimerTask = new TimerTask() {
            @Override
            public void run() {
                if (workoutRunning > 0) {

                    switch (mSppType) {
                        //Handle stroke-based workouts
                        case SPP_TYPE_STROKES: {
                            if (phaseProgress >= phaseTrigger) {
                                phaseProgress = 0;
                                cancel();
                                switch (workoutRunning) {
                                    case 2:
                                        phaseProgress++;
                                        autoProgress(beeperService);
                                        break;
                                    default:
                                        endWorkout(beeperService);
                                }

                            } else
                                pref.edit().putInt(WaveActivity.PHASE_PROGRESS, ++phaseProgress).apply();
                            break;
                        }
                        //Handle distance-based workouts
                        case SPP_TYPE_METERS: {
                            if (phaseProgress > phaseTrigger) {
                                cancel();
                                startingPhaseLocation = currentLocation;
                                phaseProgress = 0;
                                switch (workoutRunning) {
                                    case 2:
                                        autoProgress(beeperService);
                                        break;
                                    default:
                                        endWorkout(beeperService);
                                }
                            } else
                                pref.edit().putInt(WaveActivity.PHASE_PROGRESS, phaseProgress).apply();
                            break;
                        }
                        //Handle time-based workouts
                        case SPP_TYPE_SECONDS: {
                            long currentTime = System.currentTimeMillis();
                            phaseProgress = (int) (currentTime - startTime);
                            if (phaseProgress >= phaseTrigger * 1000) {
                                cancel();
                                startTime = currentTime;
                                phaseProgress = 0;
                                switch (workoutRunning) {
                                    case 2:
                                        autoProgress(beeperService);

                                        break;
                                    default:
                                        endWorkout(beeperService);
                                }
                            } else {
                                if (timeTimer == null) {
                                    Log.d("TEHTIMER", "TIMER is null");
                                }
                                if (timeTimerTask == null) {
                                    Log.d("TEHTIMER", "TIMERTASK is null");

                                }
                                //pref.edit().putInt(WaveActivity.PHASE_PROGRESS, phaseProgress/1000).apply();
                            }
                            break;
                        }
                        default: {
                            break;
                        }
                    }
                }
                toneGen1.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 150);
                updateAverageSpeed();
                pref.edit().putInt(WaveActivity.BEEP, ++beeps).apply();
                Log.d("TEHBEEP", "BEEP!");

            }
        };
        workoutTimer = new Timer();
        workoutTimer.scheduleAtFixedRate(workoutTimerTask, 1, strokeDuration);
    }


    /**
     * Cancel running Timer, if any
     */
    private void cancelTimer(Timer timer, TimerTask timerTask) {
        try {
            timer.cancel();
            timerTask.cancel();
        } catch (IllegalStateException e) {
            Log.d("Exception", "Cancelled or scheduled");
        } catch (NullPointerException e) {
            Log.d("Exception", "Null pointer");
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

//        pref = PreferenceManager.getDefaultSharedPreferences(beeperService);
        pref.edit().putInt(WaveActivity.SPM_SETTING, 0).apply();

        flushUI();

        Log.d("LOCSS", "Stopping.");
        if (beeperService.locationManager != null) {
            beeperService.locationManager.removeUpdates(beeperService.locListener);
            beeperService.locationManager = null;
        }

        beeperService.stopSelf();
    }

    private void autoProgress(BeeperService beeperService) {

        //Set exercise to autoProgress;
        workoutRunning = 2;

        phasesTotal = GEAR_SETTINGS.length;

        final int[] COLOR_PROGRESSION = new int[]{
                Color.GREEN, Color.YELLOW
        };

        //Phase number starts from 1
        phase++;

        if (phase == phasesTotal) {
            workoutRunning = 0;
        }

        if ((phase & 1) == 1) {
            color = COLOR_PROGRESSION[0];
        } else {
            color = COLOR_PROGRESSION[1];
        }

        startPhase(
                SPP_SETTINGS[phase - 1],
                GEAR_SETTINGS[phase - 1],
                beeperService
        );
    }

    private void startPhase(int lengthTrigger, int tempo, BeeperService beeperService) {
        //Init starting time for time-based workouts
        startTime = System.currentTimeMillis();

//        pref = PreferenceManager.getDefaultSharedPreferences(beeperService);
        phaseTrigger = lengthTrigger;
        spm = tempo;
        //Length in the respective unit (strokes, meters or seconds)
        int totalWorkoutLength = 0;

        for (int i : SPP_SETTINGS) {
            totalWorkoutLength += i;
        }

        final String workout_progress = totalWorkoutCount + "/" + totalWorkoutLength;
        int progress = (int) (((float) phase / (float) phasesTotal) * 100);
        if (phase == phasesTotal) {
            workoutRunning = 9;
        }

        pref.edit().putInt(WaveActivity.OPERATION_SETTING, workoutRunning).apply();

        pref.edit().putInt(WaveActivity.CURRENT_COLOR, color).apply();


        pref.edit().putInt(WaveActivity.PHASE_LENGTH, SPP_SETTINGS[phase - 1]).apply();
        pref.edit().putInt(WaveActivity.TOTAL_WORKOUT_LENGTH, totalWorkoutLength).apply();


        Boolean bool = pref.getBoolean(WaveActivity.SWITCH_SETTING, true);
        pref.edit().putBoolean(WaveActivity.SWITCH_SETTING, !bool).apply();

        startTheTempo(beeperService);
    }

    //TODO cancel countdown on interrupt
    private void startCountdown(final BeeperService beeperService) {

//        pref.edit().putInt(WaveActivity.COUNTDOWN_RUNNING, 1).apply();

        countdownTimerTask = new TimerTask() {

            @Override
            public void run() {
                if (countdownCycles % 10 == 0) {
                    pref.edit().putInt(WaveActivity.COUNTDOWN_DURATION_LEFT, countdownDuration - countdownCycles * 100).apply();
                    Log.d("CountdownDigits", "" + (countdownDuration - countdownCycles * 100));
                }
                if (++countdownCycles >= countdownCyclesTotal) {
                    pref.edit().putInt(WaveActivity.COUNTDOWN_DURATION_LEFT, 0).apply();
                    autoProgress(beeperService);
                    cancelTimer(countdownTimer, this);
                } else
                    countdownToneGen.startTone(ToneGenerator.TONE_CDMA_INTERCEPT, 100);
            }
        };

        countdownTimer = new Timer();
        countdownTimer.scheduleAtFixedRate(countdownTimerTask, 1, countdownCycleDuration);

    }

    private void initLocation(BeeperService beeperService) {

        PackageManager manager = beeperService.getPackageManager();
        int permission = manager.checkPermission("android.permission.ACCESS_FINE_LOCATION", "com.batyanko.strokeratecoach");
        boolean hasPermission = (permission == PackageManager.PERMISSION_GRANTED);
//
        if (!hasPermission) {
            Log.d("I CAN HAZ PERMISSION?", "NO!");
            return;
        }
        Log.d("TEHSERVICE", "at initLocation");

        beeperService.locationManager = (LocationManager) beeperService.getSystemService(Context.LOCATION_SERVICE);
        beeperService.locListener = new TehLocListener(beeperService);
        beeperService.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 200, 1, beeperService.locListener);
        Log.d("LOCSS", "Initialized.");

    }

    private void updateAverageSpeed() {
        if (currentLocation == null) return;
        locationPool[locCycleCount] = currentLocation;

        //TESTING
        for (CLocation i : locationPool) {
            Log.d("SPEEDCYCLE array", "" + i);
        }
        Log.d("SPEEDCYCLE", "" + locCycleCount);
        Log.d("SPEEDCYCLE POOLISFULL", " " + locationPoolIsFull);
        if (locCycleCount == 9) {
            locCycleCount = 0;
            locationPoolIsFull = true;
            Log.d("SPEEDCYCLE REACH9", "check");
        } else locCycleCount++;
        if (locationPoolIsFull && locationPool[locCycleCount] != null) {
            Log.d("SPEEDCYCLE", "poolisfull");
            long timeDiffMillis;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                timeDiffMillis =
                        (currentLocation.getElapsedRealtimeNanos() - locationPool[locCycleCount].getElapsedRealtimeNanos())/1000000;
                Log.d("TEHFLOAT", "timediff1: " + timeDiffMillis);

            } else {
                timeDiffMillis =
                        currentLocation.getTime() - locationPool[locCycleCount].getTime();
                Log.d("TEHFLOAT", "timediff2" + (int)timeDiffMillis);
            }
            averageSpeed = (currentLocation.distanceTo(locationPool[locCycleCount]) / (((float)timeDiffMillis)/1000));
            Log.d("TEHFLOAT", "distance: " + currentLocation.distanceTo(locationPool[locCycleCount]));
            Log.d("TEHFLOAT", "timediffMillis: " + (float)timeDiffMillis);
            Log.d("TEHFLOAT", "AVG speed" + (int)averageSpeed);

/*            float z = 0;
            for(float x : averageSpeedPool) {
                z+=x;
                Log.d("SPEEDCYCLE TEHZ", "" + z);
            }
            z/=10;
            Log.d("SPEEDCYCLE TEHZfinal(", "" + z);*/
            pref.edit().putFloat(WaveActivity.CURRENT_SPEED, averageSpeed).apply();
        }
    }

    public class TehLocListener implements IBaseGpsListener {

        Context context;

        Location location;

        private TehLocListener(Context context) {
            this.context = context;
        }

        private void updateLocation(CLocation location) {
            location.setUseMetricunits(true);
            locationAccuracy = location.getAccuracy();
            Log.d("UPDATESPEED", "Accuracy: " + locationAccuracy);
            if (locationAccuracy <= ACCEPTABLE_ACCURACY) {

                //TESTING
                long getNanos = 0;
                long gettime = location.getTime();
                long currNanoTime = 0;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    getNanos = location.getElapsedRealtimeNanos();
                    currNanoTime = SystemClock.elapsedRealtimeNanos();
                }
                long currTime = System.currentTimeMillis();

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.US);
                SimpleDateFormat sdfSimple = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                ParsePosition pp = new ParsePosition(0);
                Date dtCurr = new Date(currTime);
                Date dtLoc = new Date(gettime);

                Log.d("SPEEDCYCLE TIMEACC", "--------------------");
                Log.d("SPEEDCYCLE TIMEACC", "currtime: " + currTime + " " + sdf.format(dtCurr));
                Log.d("SPEEDCYCLE TIMEACC", "currnanotime: " + currNanoTime);
                Log.d("SPEEDCYCLE TIMEACC", "gettime: " + gettime + " " + sdf.format(dtLoc));
                Log.d("SPEEDCYCLE TIMEACC", "gettimeDIF: " + (currTime - gettime));
                Log.d("SPEEDCYCLE TIMEACC", "getNanos: " + getNanos);
                Log.d("SPEEDCYCLE TIMEACC", "nanosDIF: " + (currNanoTime - getNanos));

                if (startingPhaseLocation == null) {
                    startingPhaseLocation = location;
                } else {
                    locationAccuracy = location.getAccuracy();
                    currentLocation = location;
                    currentSpeed = location.getSpeed();

                    phaseProgress = (int) location.distanceTo(startingPhaseLocation);
                    Log.d("UPDATESPEED", "" + phaseProgress);
                }
            }
        }

        private boolean useMetricUnits() {
            // TODO Auto-generated method stub
//        CheckBox chkUseMetricUnits = (CheckBox) this.findViewById(R.id.chkMetricUnits);
            return true;
        }

        @Override
        public void onLocationChanged(Location location) {
            Log.d("UPDATESPEED", "onLocationChanged");
            if (location != null) {
                CLocation myLocation = new CLocation(location, this.useMetricUnits());
                this.updateLocation(myLocation);
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
            /*if (status == LocationProvider.AVAILABLE) {
                if (startingPhaseLocation == null) {
                    startingPhaseLocation
                }
            }*/
        }

        @Override
        public void onGpsStatusChanged(int event) {

        }
    }
}
