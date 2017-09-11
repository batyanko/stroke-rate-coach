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

    public static final String ACTION_START_BEEP = "start-beep";
    public static final String ACTION_STOP_BEEP = "stop-beep";
    public static final String EXTRA_WORKOUT_SPP = "workout_spp";
    public static final String EXTRA_WORKOUT_GEARS = "workout_gears";
    public static final String EXTRA_WORKOUT_SPP_TYPE = "workout-spp-type";
    public static final int SPP_TYPE_STROKES = 1;
    public static final int SPP_TYPE_METERS = 2;
    public static final int SPP_TYPE_SECONDS = 3;

    //VALUES FROM WaveActivity

    /* Global spm setting to hold current spm */
    public static int spm = 22;

    //Variables used in beeping timer setup
    public static long strokeDuration;
    public static String spmString;
    public static ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);

    public static Timer timer;
    public static TimerTask timerTask;

    //autoWave values
    private static int workoutRunning = 0;     //0=off, 1=autoWave, 2=autoProgress, 9=lastPhase
    private static int PhaseStrokeCount = 0;
    private static int TotalstrokeCount = 0;
    private static int phaseTrigger = 0;
    //phase = 0 = wave not running
    private static int phase = 0;
    private static int phasesTotal = 0;
    private static int gear = 0;
    private static int color = 0;

    private int mSppType;

    private static int[] SPP_SETTINGS = new int[]{
            3, 3, 3
    };

    private static int[] GEAR_SETTINGS = new int[]{
            40, 50, 60
    };

    private static SharedPreferences pref;

    //END OF COPIED VALUES

    private Location startingLocation;
    private Location currentLocation;
    private TehLocListener locListener;
    private static float distanceFromStart = 0;
    private float currentSpeed;


    void executeTask(BeeperService beeperService, String action,
                     int[] sppSettings, int[] gearSettings, int sppType) {
        //TODO init stuff in a separate method?
        //TODO bind to incoming sppType
        mSppType = SPP_TYPE_METERS;
        stopTheTempo();
        if (action.equals(ACTION_START_BEEP)) {

            if (mSppType == SPP_TYPE_METERS) {
                initLocation(beeperService);
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
        }
        //endWorkout() is called anyway
        /*else if (action.equals(ACTION_STOP_BEEP)) {
            endWorkout(context);
        }*/
//        Notification notification = new Notification();
//        notification.flags |= Notification.FLAG_ONGOING_EVENT;
    }

    private void startBeeping(BeeperService beeperService) {
        Log.d("WOOSH", "--Epic Shit--");
    }

    private void startTheTempo(final BeeperService beeperService) {
        pref = PreferenceManager.getDefaultSharedPreferences(beeperService);
        pref.edit().putInt(WaveActivity.SPM_SETTING, spm).apply();
        PhaseStrokeCount = 0;

        strokeDuration = SpmUtilities.spmToMilis(spm);

        try {
            timer.cancel();
            timerTask.cancel();
        } catch (IllegalStateException e) {
            Log.d("Exception", "Cancelled or scheduled");
        } catch (NullPointerException e) {
            Log.d("Exception", "Null pointer");
        }

        timerTask = new TimerTask() {
            @Override
            public void run() {
                PhaseStrokeCount++;
                if (workoutRunning > 0) {

                    switch (mSppType) {
                        //Handle stroke-based workouts
                        case SPP_TYPE_STROKES: {

                            if (PhaseStrokeCount > phaseTrigger) {
                                cancel();
                                switch (workoutRunning) {
                                    case 1:
                                        autoProgress(beeperService);
                                        break;
                                    case 2:
                                        autoProgress(beeperService);
                                        break;
                                    default:
                                        endWorkout(beeperService);
                                }

                            } else
                                pref.edit().putInt(WaveActivity.TOTAL_PROGRESS_ELAPSED, ++TotalstrokeCount).apply();
                            break;
                        }
                        //Handle distance-based workouts
                        case SPP_TYPE_METERS: {
                            if (distanceFromStart > phaseTrigger) {
                                cancel();
                                startingLocation = currentLocation;
                                distanceFromStart = 0;
                                toneGen1.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 1500);
                                switch (workoutRunning) {
                                    case 1:
                                        autoProgress(beeperService);
                                        break;
                                    case 2:
                                        autoProgress(beeperService);
                                        break;
                                    default:
                                        endWorkout(beeperService);
                                }
                            }
                            //TODO update sharedPrefs with distance
                            Log.d("DISTANCEFROMSTART", "" + distanceFromStart);
                            Log.d("DISTANCEFROMSTART", "" + ((int) distanceFromStart));
                            pref.edit().putInt(WaveActivity.TOTAL_PROGRESS_ELAPSED, (int) distanceFromStart).apply();
                            break;
                        }
                        default: {
                            break;
                        }
                    }
                }
                pref.edit().putFloat(WaveActivity.CURRENT_SPEED, currentSpeed).apply();
                Log.d("TEHBEEP", "BEEP!");
                Log.d("THATOTHERA WaveActivity", WaveActivity.RATE_KEY);
                toneGen1.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 150);
            }
        };
        timer = new Timer();
        timer.scheduleAtFixedRate(timerTask, 1, strokeDuration);
    }

    //TODO merge with endWorkout?
    private void stopTheTempo() {
        PhaseStrokeCount = 0;
        TotalstrokeCount = 0;
        phaseTrigger = 0;
        phase = 0;
        workoutRunning = 0;
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
        stopTheTempo();
        pref = PreferenceManager.getDefaultSharedPreferences(beeperService);
        pref.edit().putInt(WaveActivity.OPERATION_SETTING, workoutRunning).apply();
        Boolean bool = pref.getBoolean(WaveActivity.SWITCH_SETTING, true);
        pref.edit().putBoolean(WaveActivity.SWITCH_SETTING, !bool).apply();
        /*try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
//        beeperService.stopSelf();*/
    }

    private void autoProgress(BeeperService beeperService) {

        //Set excercise to autoProgress;
        workoutRunning = 2;

        phasesTotal = GEAR_SETTINGS.length;

        final int[] COLOR_PROGRESSION = new int[]{
                Color.GREEN, Color.YELLOW
        };

        phase++;

        if (phase == phasesTotal) {
            workoutRunning = 0;
        }

        Log.d("TEHCOLOR before", "" + color);
        if ((phase & 1) == 1) {
            color = COLOR_PROGRESSION[0];
        } else {
            color = COLOR_PROGRESSION[1];
        }
        Log.d("TEHCOLOR after", "" + color);

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
        //LEngthin the respective unit (strokes, meters or seconds)
        int totalworkoutLength = 0;
        if (mSppType == SPP_TYPE_STROKES) {

        }
        for (int i : SPP_SETTINGS) {
            totalworkoutLength += i;
        }

        final String workout_progress = TotalstrokeCount + "/" + totalworkoutLength;
        int progress = (int) (((float) phase / (float) phasesTotal) * 100);
        if (phase == phasesTotal) {
            workoutRunning = 9;
        }

        pref.edit().putInt(WaveActivity.OPERATION_SETTING, workoutRunning).apply();
        Log.d("TEHCOLOR", "" + color);
        pref.edit().putInt(WaveActivity.CURRENT_COLOR, color).apply();
        pref.edit().putInt(WaveActivity.TOTAL_WORKOUT_LENGTH, totalworkoutLength).apply();
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
            distanceFromStart = 0;
            return;
        }

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        locListener = new TehLocListener(context);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locListener);

        locListener.updateDistance(null);

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
            if (startingLocation == null) {
                startingLocation = location;
            } else {
                currentLocation = location;
                distanceFromStart = location.distanceTo(startingLocation);
                currentSpeed = location.getSpeed();
                Log.d("UPDATESPEED", "" + distanceFromStart);
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
