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
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.preference.PreferenceManager;
import android.util.Log;

import com.batyanko.strokeratecoach.Utils.SpmUtilities;
import com.batyanko.strokeratecoach.WaveActivity;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by batyanko on 9/4/17.
 */

public class BeeperTasks {

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
    private static int strokeCountTrigger = 0;
    //phase = 0 = wave not running
    private static int phase = 0;
    private static int phasesTotal = 0;
    private static int gear = 0;
    private static int color;

    public static int[] SPP_SETTINGS = new int[]{
            3, 3, 3
    };

    public static int[] GEAR_SETTINGS = new int[]{
            40, 50, 60
    };

    private static SharedPreferences pref;

    //END OF COPIED VALUES


    public static final String ACTION_START_BEEP = "start-beep";
    public static final String ACTION_STOP_BEEP = "stop-beep";
    public static final String EXTRA_WORKOUT_SPP = "workout_spp";
    public static final String EXTRA_WORKOUT_GEARS = "workout_gears";

    public static void executeTask(Context context, String action,
                                   int[] sppSettings, int[] gearSettings) {
        endWorkout(context);
        if (action.equals(ACTION_START_BEEP)) {
            Log.d("IFPASS", "First pass");
            if (gearSettings != null) {
                Log.d("IFPASS", "Second pass");
                //Manual SPM setting or preset workout
                if (sppSettings == null) {
                    Log.d("IFPASS", "3.1 pass");
                    spm = gearSettings[0];
                    startTheTempo(context);
                } else {
                    Log.d("IFPASS", "3.2 pass");
                    SPP_SETTINGS = sppSettings;
                    GEAR_SETTINGS = gearSettings;
                    autoProgress(context);
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

    private static void startBeeping(Context context) {
        Log.d("WOOSH", "--Epic Shit--");
    }

    private static void startTheTempo(final Context context) {
        pref = PreferenceManager.getDefaultSharedPreferences(context);
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
                    if (PhaseStrokeCount > strokeCountTrigger){
                        cancel();
                        switch (workoutRunning) {
                            case 1:
                                autoProgress(context);
                                break;
                            case 2:
                                autoProgress(context);
                                break;
                            default:
                                endWorkout(context);
                        }

                    } else
                    pref.edit().putInt(WaveActivity.TOTAL_STROKES_ELAPSED, ++TotalstrokeCount).apply();
                }
                Log.d("TEHBEEP", "BEEP!");
                Log.d("THATOTHERA WaveActivity", WaveActivity.RATE_KEY);
                toneGen1.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 150);
            }
        };
        timer = new Timer();
        timer.scheduleAtFixedRate(timerTask, 1, strokeDuration);
    }

    //TODO merge with endWorkout?
    private static void stopTheTempo() {
        PhaseStrokeCount = 0;
        TotalstrokeCount = 0;
        strokeCountTrigger = 0;
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
    public static void endWorkout(Context context) {
        pref = PreferenceManager.getDefaultSharedPreferences(context);
        phase = 0;
        workoutRunning = 0;
        pref.edit().putInt(WaveActivity.OPERATION_SETTING, workoutRunning).apply();
        Boolean bool = pref.getBoolean(WaveActivity.SWITCH_SETTING, true);
        pref.edit().putBoolean(WaveActivity.SWITCH_SETTING, !bool).apply();
        stopTheTempo();
    }

    private static void autoProgress(Context context) {

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

        if ((phase & 1) == 1) {
            color = COLOR_PROGRESSION[0];
        } else {
            color = COLOR_PROGRESSION[1];
        }

        startPhase(
                SPP_SETTINGS[phase - 1],
                GEAR_SETTINGS[phase - 1],
                context
        );
    }

    private static void startPhase(int lengthTrigger, int tempo, Context context) {
        pref = PreferenceManager.getDefaultSharedPreferences(context);
        strokeCountTrigger = lengthTrigger;
        spm = tempo;
        int totalStrokes = 0;
        for (int i : SPP_SETTINGS) {
            totalStrokes += i;
        }
        final String workout_progress = TotalstrokeCount + "/" + totalStrokes;
        int progress = (int) (((float) phase / (float) phasesTotal) * 100);
        if (phase == phasesTotal) {
            workoutRunning = 9;
        }

        pref.edit().putInt(WaveActivity.OPERATION_SETTING, workoutRunning).apply();
        pref.edit().putInt(WaveActivity.CURRENT_COLOR, color).apply();
        pref.edit().putInt(WaveActivity.TOTAL_STROKES, totalStrokes).apply();
        Boolean bool = pref.getBoolean(WaveActivity.SWITCH_SETTING, true);
        pref.edit().putBoolean(WaveActivity.SWITCH_SETTING, !bool).apply();

        startTheTempo(context);
    }
}
