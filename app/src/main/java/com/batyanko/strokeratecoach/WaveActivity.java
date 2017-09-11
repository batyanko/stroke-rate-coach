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

package com.batyanko.strokeratecoach;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.batyanko.strokeratecoach.Fragments.SlideFragment;
import com.batyanko.strokeratecoach.sync.BeeperService;
import com.batyanko.strokeratecoach.sync.BeeperTasks;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import static android.text.TextUtils.join;

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

    public static final int MY_LOCATION_PERMISSION = 22;

    public static final int DEFAULT_RATE = 22;
    public static final String RATE_KEY = "rate";
    public static final String TOTAL_WORKOUT_LENGTH = "total-strokes-in-workout";
    public static final String TOTAL_PROGRESS_ELAPSED = "total-strokes-elapsed";
    public static final String CURRENT_COLOR = "current-phase";
    public static final String CURRENT_SPEED = "current-speed";

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

    //TODO delete redundant arrays
    public static int[] GEAR_SETTINGS = new int[]{
            40, 50, 60
    };

    public static int[] STROKES_PER_PHASE = new int[]{
            3, 3, 3
    };

    public static int[][] PRESET_SETTINGS;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wave);

        Log.d("INTHEBEGINNING", "" + BeeperTasks.spm);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

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
        spm = pref.getInt(SPM_SETTING, 22);

        //Initialize UI elements

        waveProgress = (ProgressBar) findViewById(R.id.wave_progress_bar_2);
        waveProgress.setVisibility(View.INVISIBLE);

        spmTextView = (TextView) findViewById(R.id.SpmTextView_2);
        spmTextView.setText(String.valueOf(spm));
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
                        Log.d("WOOSH", "--Regular Stuff--");
                        autoProgress();
                        break;
                    case 2:
                        Log.d("WOOSH", "--Regular Stuff--");
                        autoProgress();
                        break;
                    default:
                        endWorkout();
                }
            }
        };


        PackageManager manager = getPackageManager();
        int permission = manager.checkPermission("android.permission.ACCESS_FINE_LOCATION",
                "com.batyanko.strokeratecoach");
        boolean hasPermission = (permission == manager.PERMISSION_GRANTED);
//
        if (!hasPermission) {
            Log.d("I CAN HAZ PERMISSION?", "NO!");
            ActivityCompat.requestPermissions(this,
                    new String[]{"android.permission.ACCESS_FINE_LOCATION"}, MY_LOCATION_PERMISSION);
        }
    }

    @Override
    protected void onStart() {
//        startTheTempo();
        super.onStart();
        onProgressChange();
        if (PRESET_SETTINGS == null) {
            Log.d("PRESETSETTINGS onStart", "null");
        }
    }

    @Override
    protected void onStop() {
        //Stop beeping on leaving, as screen is expected to remain on while rowing.
        //Change if intended otherwise.
        stopTheTempo();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    /**
     * Initiate beeping according to active workout settings.
     */

    private void startTheTempo() {

        strokeCount = 0;

        //TODO cleanup...
        /*strokeDuration = SpmUtilities.spmToMilis(spm);

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
                strokeCount++;
                if (workoutRunning > 0 && strokeCount > strokeCountTrigger) {
                    cancel();
                    //Got to run on the Main Thread as we need to access UI elements
                    runOnUiThread(
                            runForestRun
                    );
                }
                toneGen1.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 150);
            }
        };

        timer = new Timer();
        timer.scheduleAtFixedRate(timerTask, 1, strokeDuration);

        spmString = String.valueOf(spm);
        spmTextView.setText(spmString);*/

        Intent intent = new Intent(this, BeeperService.class);
        intent.setAction(BeeperTasks.ACTION_START_BEEP);
        startService(intent);
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
     *
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
        Log.d("WOOSH", "--Regular Stuff--");
        waveProgress.setVisibility(View.VISIBLE);
        int progress = (int) (((float) phase / (float) phasesTotal) * 100);
        waveProgress.setProgress(progress);
        if (phase == phasesTotal) {
            workoutRunning = 9;
        }
        startTheTempo();
    }

    /**
     * Reset the current workout and stop beeping.
     */
    public void endWorkout() {
//        phase = 0;
//        workoutRunning = 0;
        waveProgress.setVisibility(View.INVISIBLE);
        progressTextView.setVisibility(View.INVISIBLE);
//        spmTextView.setBackgroundResource(0);
        spmTextView.setText("0");
        spmTextView.setBackgroundColor(Color.TRANSPARENT);
//        stopTheTempo();
    }


    //TODO delete redundant method, already used from SlideFragment

    /**
     * Handle digit input and set the spm accordingly.
     *
     * @param digitalInput digit input
     * @param view         reference to the view that represents the digit
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
     *
     * @param sharedPreferences a SharedPreferences that was changed
     * @param s                 key for the changed preference
     */

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        Log.d("WOOSH", "--onSharedPreferenceChanged--");

        if (s.equals(SPM_SETTING)) {
            spm = sharedPreferences.getInt(SPM_SETTING, 22);
            spmTextView.setText(String.valueOf(spm));
//            startTheTempo();
        } else if (s.equals(SWITCH_SETTING)) {
            int operation = sharedPreferences.getInt(OPERATION_SETTING, 0);
            switch (operation) {
                case WORKOUT_WAVE: {
//                    endWorkout();
                    workoutRunning = WORKOUT_WAVE;
                    onProgressChange();
//                    startTheTempo();
                    break;
                }
                case WORKOUT_PROGRESS: {
//                    endWorkout();
                    workoutRunning = WORKOUT_PROGRESS;
                    onProgressChange();
                    Log.d("THATOTHERA WaveActivity", "" + BeeperTasks.spm);

//                    startTheTempo();
                    break;
                }
                case WORKOUT_STOP: {
                    workoutRunning = WORKOUT_STOP;
                    endWorkout();
                    break;
                }
            }
        } else if (s.equals(TOTAL_PROGRESS_ELAPSED)) {
            onProgressChange();
//            progressTextView.setText(sharedPreferences.getInt(TOTAL_PROGRESS_ELAPSED, 0)
//                    + " / " + sharedPreferences.getInt(TOTAL_WORKOUT_LENGTH, 0));
        } else if (s.equals(CURRENT_COLOR)) {
            spmTextView.setBackgroundColor(sharedPreferences.getInt(CURRENT_COLOR, Color.TRANSPARENT));
        } else if (s.equals(TOTAL_PROGRESS_ELAPSED)) {
            String currentDistance =
                    sharedPreferences.getFloat(TOTAL_PROGRESS_ELAPSED, 42) +
                            " / " +
                            pref.getInt(TOTAL_WORKOUT_LENGTH, 0) +
                            " at " +
                            pref.getInt(CURRENT_SPEED, 0) +
                            " kph";
            progressTextView.setText(currentDistance);
            progressTextView.setVisibility(View.VISIBLE);
        }
    }

    //Initialize workout visuals
    private void onProgressChange() {
        Log.d("TEHPREFCOLOR", "onProgressChange");
        if (workoutRunning == WORKOUT_STOP) {
            return;
        }
        int[] progress = new int[2];
        progress[0] = pref.getInt(TOTAL_PROGRESS_ELAPSED, 0);
        Log.d("TEHPREFCOLOR", "prog0 " + progress[0]);
        progress[1] = pref.getInt(TOTAL_WORKOUT_LENGTH, 0);
        Log.d("TEHPREFCOLOR", "prog1 " + progress[1]);

        if (progress[0] > progress[1]) {
            //Take a break :3
            return;
        }

        progressTextView.setText(progress[0] +
                " / " + progress[1] +
                " at " +
                pref.getFloat(CURRENT_SPEED, 0) +
                " m/s");
        progressTextView.setVisibility(View.VISIBLE);

        int progressPercent = (int) (((float) progress[0] / (float) progress[1]) * 100);
        waveProgress.setProgress(progressPercent);
        waveProgress.setVisibility(View.VISIBLE);
        Log.d("TEHPREFCOLOR", "" + pref.getInt(CURRENT_COLOR, Color.TRANSPARENT));
        spmTextView.setBackgroundColor(pref.getInt(CURRENT_COLOR, Color.TRANSPARENT));

    }
}