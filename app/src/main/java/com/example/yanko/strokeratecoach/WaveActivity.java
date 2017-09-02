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

package com.example.yanko.strokeratecoach;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.yanko.strokeratecoach.Fragments.SlideFragment;
import com.example.yanko.strokeratecoach.Utils.SpmUtilities;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class WaveActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    //Constants for OnSharedPreferenceChangeListener
    public static final String OPERATION_SETTING = "operation";
    public static final String SWITCH_SETTING = "switch";
    public static final String SPM_SETTING = "spm";

    //Workout types
    public static final int WORKOUT_WAVE = 1;
    public static final int WORKOUT_PROGRESS = 2;
    public static final int WORKOUT_STOP = 0;

    //Workout item function IDs
    public static final int FAV_BUTTON_FUNCTION = 1;
    public static final int WORKOUT_ITEM_FUNCTION = 2;
    public static final int ENGAGE_WORKOUT_FUNCTION = 3;

    //SPP (strokes or stuff per phase) unit types
    public static final int SPP_UNIT_STROKES = 0;
    public static final int SPP_UNIT_SECONDS = 1;
    public static final int SPP_UNIT_METERS = 2;



    public static final int DEFAULT_RATE = 22;
    public static final String RATE_KEY = "rate";

    //First digit of spm (Strokes per Minute) used to allow automatic spm
    // initialization upon dialing two digits
    public static int firstDigit;

    /* Global spm setting to hold current spm */
    public static int spm;

    //Variables used in beeping timer setup
    public static long strokeDuration;
    public static String spmString;
    public static ToneGenerator toneGen1;
    public static Timer timer;
    public static TimerTask timerTask;

    //UI elements
    private ProgressBar waveProgress;
    private Button waveButton;
    private TextView spmTextView;
    private TextView progressTextView;

    //TODO ??
    private int[] colors;

    //UI reference to dialGrid and an element of it
    GridView dialGrid;
    View firstDigitView;

    private SharedPreferences pref;

    //autoWave values
    private static int workoutRunning = 0;     //0 = off, 1 = autoWave, 2 = autoProgress
    private static int strokeCount = 0;
    private static int strokeCountTrigger = 0;
    //phase = 0 = wave not running
    private static int phase = 0;
    private static int phasesTotal = 0;
    private static int gear = 0;
    private static int color;


    public static int windowWidth;
    public static int windowHeight;
    public static int statusbarHeight;

    static Runnable runForestRun;

    public static int[] GEAR_SETTINGS = new int[]{
            40, 50, 60
    };

    public static int[] STROKES_PER_PHASE = new int[]{
            3, 3, 3
    };

    //TODO delete temp workouts

    public static final int[] exerciseSPP1 = {
            10, 20, 30
    };
    public static final int[] exerciseSPP2 = {
            20, 40, 60
    };
    public static final int[] exerciseSPP3 = {
            5, 10, 15
    };
    public static final int[] exerciseSPP4 = {
            3, 6, 9
    };

    public static final int[] exerciseG1 = {
            32, 20
    };
    public static final int[] exerciseG2 = {
            24, 28, 32
    };
    public static final int[] exerciseG3 = {
            24, 28, 32
    };
    public static final int[] exerciseG4 = {
            24, 28, 32
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wave);

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        SlideFragment slideFragment = new SlideFragment();
        transaction.replace(R.id.slide_frame_layout, slideFragment);
        transaction.commit();

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        windowWidth = metrics.widthPixels;
        windowHeight = metrics.heightPixels;

        firstDigit = 0;

        //Initialize spm at last setting, or default at 22
        pref = PreferenceManager.getDefaultSharedPreferences(this);

        spm = pref.getInt("spm", 22);

        //Initialize UI elements

        waveProgress = (ProgressBar) findViewById(R.id.wave_progress_bar_2);
        waveProgress.setVisibility(View.INVISIBLE);

        spmTextView = (TextView) findViewById(R.id.SpmTextView_2);
        progressTextView = (TextView) findViewById(R.id.progressTextView);

        toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);

        waveButton = (Button) findViewById(R.id.create_workout_button);
        waveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(WaveActivity.this, EntryFormActivity.class);
                startActivity(intent);
            }
        });

        colors = new int[2];
        colors[0] = Color.GREEN;
        colors[1] = Color.YELLOW;

        //Try with a GridView

        int resource = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resource > 0) {
            statusbarHeight = getResources().getDimensionPixelSize(resource);
//            Log.d("Statusbar Height!!!: ", "" + statusbarHeight);
        }

        runForestRun = new Runnable() {
            @Override
            public void run() {
                switch (workoutRunning) {
                    case 1:
                        autoProgress();
                        break;
                    case 2:
                        autoProgress();
                        break;
                    default:
                        endWorkout();
                }
            }
        };

    }

    @Override
    protected void onStart() {
        startTheTempo();
        Log.i("Stroke rate at start: ", "" + spm);
        super.onStart();
    }

    @Override
    protected void onStop() {
        //Stop beeping on leaving, as screen is expected to remain on while rowing.
        //Change if intended otherwise.
        stopTheTempo();
        super.onStop();
    }

    /**
     * Initiate beeping according to active workout settings.
     */

    private void startTheTempo() {

        strokeCount = 0;

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
                toneGen1.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 150);
                strokeCount++;
                if (workoutRunning > 0 && strokeCount > strokeCountTrigger) {
                    cancel();
                    //Got to run on the Main Thread as we need to access UI elements
                    runOnUiThread(
                            runForestRun
                    );
                }
            }
        };

        timer = new Timer();
        timer.scheduleAtFixedRate(timerTask, 1, strokeDuration);

        spmString = String.valueOf(spm);
        spmTextView.setText(spmString);
    }


    /**
     * Stop the beeping mechanism.
     */
    private static void stopTheTempo() {
        strokeCount = 0;
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

    //TODO delete wave method, waves are currently saved in Progress format
    /**
     * Initiate a wave type of workout according to the active workout settings.
     */
    private void autoWave() {
        //Set excercise to autoWave;
        workoutRunning = 1;

        phasesTotal = (STROKES_PER_PHASE.length * 2 - 1) * 2;
        final int[] ALL_PHASES = new int[phasesTotal];

        //
        for (int i = 0, j = 0; i < STROKES_PER_PHASE.length; i++, j = j + 2) {
            //Fill up ascending waves
            ALL_PHASES[j] = STROKES_PER_PHASE[i];
            ALL_PHASES[j + 1] = STROKES_PER_PHASE[i];
            //Fill up descending waves
            ALL_PHASES[phasesTotal - 1 - j] = STROKES_PER_PHASE[i];
            ALL_PHASES[phasesTotal - 1 - j - 1] = STROKES_PER_PHASE[i];
            Log.d("Wave array: ", Arrays.toString(ALL_PHASES));
        }

        //Initialise phase to 1, or iterate through phases;
        stopTheTempo();

        //Initialize / iterate phase
        phase++;
        if (gear == 1 || phase == 1) {
            gear = 0;
            color = colors[0];
        } else {
            gear = 1;
            color = colors[1];
        }

        startPhase(ALL_PHASES[phase - 1],
                GEAR_SETTINGS[gear]);
    }

    /**
     * Initiate a progression kind of training according to the active workout settings.
     */
    private void autoProgress() {

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
                STROKES_PER_PHASE[phase - 1],
                GEAR_SETTINGS[phase - 1]
        );
    }

    /**
     * Initiate workout phase.
     * @param lengthTrigger phase length
     * @param spm           strokes per minute
     */
    private void startPhase(int lengthTrigger, int spm) {
        strokeCountTrigger = lengthTrigger;
        this.spm = spm;
        spmTextView.setBackgroundColor(color);
        final String phase_progress = phase + "/" + phasesTotal;
//        waveButton.setText(phase_progress);
        progressTextView.setText(phase_progress);
        progressTextView.setVisibility(View.VISIBLE);
        waveProgress.setVisibility(View.VISIBLE);
        int progress = (int) (((float) phase / (float) phasesTotal) * 100);
        waveProgress.setProgress(progress);
        if (phase == phasesTotal) {
            workoutRunning = 9;}
        startTheTempo();
    }

    /**
     * Reset the current workout and stop beeping.
     */
    public void endWorkout() {
        phase = 0;
        workoutRunning = 0;
        waveProgress.setVisibility(View.INVISIBLE);
        progressTextView.setVisibility(View.INVISIBLE);
//        spmTextView.setBackgroundResource(0);
        spmTextView.setBackgroundColor(Color.TRANSPARENT);
        stopTheTempo();
    }


    //TODO delete redundant method, already used from SlideFragment
    /**
     * Handle digit input and set the spm accordingly.
     * @param digitalInput  digit input
     * @param view          reference to the view that represents the digit
     */
    public void setSpmFromDigital(int digitalInput, View view) {
        if (firstDigit != 0) {
            endWorkout();
            firstDigitView.setBackgroundColor(Color.TRANSPARENT);
            spm = firstDigit * 10 + digitalInput;
//            spmString = String.valueOf(spm);
//            Log.d("SpmString / spm: ", spmString + " / " + spm);
            pref.edit().putInt("spm", spm).apply();
            startTheTempo();
            firstDigit = 0;

        } else {
            firstDigit = digitalInput;
            firstDigitView = view;
            view.setBackgroundColor(Color.RED);

            Log.d("GridHeight!!!: ", "" + dialGrid.getHeight());
            Log.d("WindowHeight!!!: ", "" + windowHeight);

            ConstraintLayout constraintLayout = (ConstraintLayout) findViewById(R.id.activity_wave);
            Log.d("Constrai PostCreate???:", "" + constraintLayout.getHeight());
        }
    }

    /**
     * Listen for preference changes from child fragments.
     * Acts as an indirect way to handle UI clicks in child fragments.
     * @param sharedPreferences a SharedPreferences that was changed
     * @param s                 key for the changed preference
     */

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (s.equals(SPM_SETTING)) {
            spm = sharedPreferences.getInt("spm", 22);
            startTheTempo();
        } else if (s.equals(SWITCH_SETTING)) {
            switch (sharedPreferences.getInt(OPERATION_SETTING, 0)) {
                case WORKOUT_WAVE: {
                    endWorkout();
                    workoutRunning = 1;
                    startTheTempo();
                    break;
                }
                case WORKOUT_PROGRESS: {
                    endWorkout();
                    workoutRunning = 2;
                    startTheTempo();
                    break;
                }
                default: {
                    endWorkout();
                    break;
                }
            }
        }
    }
}