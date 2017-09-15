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
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.batyanko.strokeratecoach.Speed.CLocation;
import com.batyanko.strokeratecoach.Speed.IBaseGpsListener;
import com.batyanko.strokeratecoach.Utils.SpmUtilities;
import com.batyanko.strokeratecoach.WaveActivity;

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

    //SPP (strokes or stuff per phase) unit types
    public static final int SPP_TYPE_STROKES = 0;
    public static final int SPP_TYPE_METERS = 1;
    public static final int SPP_TYPE_SECONDS = 2;


    //VALUES FROM WaveActivity

    /* Global spm setting to hold current spm */
    public static int spm = 22;

    //Variables used in beeping workoutTimer setup
    public static long strokeDuration;
    public static String spmString;
    public static ToneGenerator toneGen1;

    public static Timer workoutTimer;
    public static TimerTask workoutTimerTask;
    public static Timer timeTimer;
    public static TimerTask timeTimerTask;

    //autoWave values
    private static int workoutRunning;     //0=off, 1=autoWave, 2=autoProgress, 9=lastPhase
    private static int totalWorkoutCount;
    private static int phaseTrigger;
    //phase = 0 = wave not running
    private static int phase;
    private static int phasesTotal;
    private static int color;
    private static int phaseProgress;


    private int mSppType;

    private static int[] SPP_SETTINGS;

    private static int[] GEAR_SETTINGS;

    private static SharedPreferences pref;

    //END OF COPIED VALUES

    private Location startingLocation;
    private Location currentLocation;
    private TehLocListener locListener;
    private static float currentSpeed;
    private static float locationAccuracy;
    private static final float ACCEPTABLE_ACCURACY = 21;

    private long startTime = System.currentTimeMillis();
    private int timerProgress;

    void executeTask(BeeperService beeperService, String action,
                     int[] sppSettings, int[] gearSettings, int sppType) {
        if (action.equals(ACTION_START_BEEP)) {
            mSppType = sppType;
            initBeeper();

            if (mSppType == SPP_TYPE_METERS) {
                initLocation(beeperService);
            }
            if (mSppType == SPP_TYPE_SECONDS) {
                initTimeTimerTask();
            }

            if (gearSettings != null) {
                //Manual SPM setting or preset workout
                if (sppSettings == null) {
                    spm = gearSettings[0];
                    startTheTempo(beeperService);
                } else {
                    SPP_SETTINGS = sppSettings;
                    GEAR_SETTINGS = gearSettings;
                    autoProgress(beeperService);
                }
            }
        } else if (action.equals(ACTION_STOP_BEEP)) {
            endWorkout(beeperService);
        }
    }

    private void initBeeper() {

        resetVariables();

        toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
    }

    private void initTimeTimerTask() {
        timeTimerTask = new TimerTask() {
            @Override
            public void run() {
                pref.edit().putInt(WaveActivity.PHASE_PROGRESS, timerProgress++).apply();
            }
        };
    }

    private void resetVariables() {
        workoutRunning = 0;     //0=off, 1=autoWave, 2=autoProgress, 9=lastPhase
        phaseProgress = 0;
        totalWorkoutCount = 0;
        phaseTrigger = 0;
        phase = 0;
        phasesTotal = 0;
        color = 0;

        phaseProgress = 0;
        locationAccuracy = 500;     //Init at an extremely inaccurate value

        startTime = System.currentTimeMillis();
    }

    private void startTheTempo(final BeeperService beeperService) {
        pref = PreferenceManager.getDefaultSharedPreferences(beeperService);
        pref.edit().putInt(WaveActivity.SPM_SETTING, spm).apply();
        phaseProgress = 0;

        strokeDuration = SpmUtilities.spmToMilis(spm);

        cancelTimer(workoutTimer, workoutTimerTask);
        if (mSppType == SPP_TYPE_SECONDS) {
            timerProgress = 0;
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
                                startingLocation = currentLocation;
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
                pref.edit().putFloat(WaveActivity.CURRENT_SPEED, currentSpeed).apply();
                Log.d("TEHBEEP", "BEEP!");
                toneGen1.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 150);
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
        resetVariables();
        cancelTimer(workoutTimer, workoutTimerTask);
        cancelTimer(timeTimer, timeTimerTask);

        pref = PreferenceManager.getDefaultSharedPreferences(beeperService);
        pref.edit().putInt(WaveActivity.OPERATION_SETTING, workoutRunning).apply();
        Boolean bool = pref.getBoolean(WaveActivity.SWITCH_SETTING, true);
        pref.edit().putBoolean(WaveActivity.SWITCH_SETTING, !bool).apply();

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

        pref = PreferenceManager.getDefaultSharedPreferences(beeperService);
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

    private void initLocation(Context context) {

        PackageManager manager = context.getPackageManager();
        int permission = manager.checkPermission("android.permission.ACCESS_FINE_LOCATION", "com.batyanko.strokeratecoach");
        boolean hasPermission = (permission == PackageManager.PERMISSION_GRANTED);
//
        if (!hasPermission) {
            Log.d("I CAN HAZ PERMISSION?", "NO!");
            return;
        }

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        locListener = new TehLocListener(context);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 200, 1, locListener);

        //locListener.updateDistance(null);

    }

    private class TehLocListener implements IBaseGpsListener {

        Context context;

        Location location;

        private TehLocListener(Context context) {
            this.context = context;
        }

        private void updateDistance(CLocation location) {
            Log.d("UPDATESPEED", "YEP");


//                location.setUseMetricunits(true);
            locationAccuracy = location.getAccuracy();
            Log.d("UPDATESPEED", "Accuracy: " + locationAccuracy);
            if (locationAccuracy <= ACCEPTABLE_ACCURACY) {

                if (startingLocation == null) {
                    startingLocation = location;
                } else {
                    locationAccuracy = location.getAccuracy();
                    Log.d("UPDATESPEED", "Accuracy: " + locationAccuracy);
                    currentLocation = location;
                    currentSpeed = location.getSpeed();
                    phaseProgress = (int) location.distanceTo(startingLocation);
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
                this.updateDistance(myLocation);
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
                if (startingLocation == null) {
                    startingLocation
                }
            }*/
        }

        @Override
        public void onGpsStatusChanged(int event) {

        }
    }
}
