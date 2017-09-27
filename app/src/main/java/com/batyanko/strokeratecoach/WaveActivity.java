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

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.batyanko.strokeratecoach.Fragments.SlideFragment;
import com.batyanko.strokeratecoach.sync.BeeperService;
import com.batyanko.strokeratecoach.sync.BeeperTasks;

import java.util.Random;

public class WaveActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = "StrokeRateCoach";

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

    public static final int MY_LOCATION_PERMISSION = 22;

    public static final String PHASE_LENGTH = "phase-length";
    public static final String TOTAL_WORKOUT_LENGTH = "total-workout-length";
    public static final String PHASE_PROGRESS = "total-strokes-elapsed";
    public static final String CURRENT_COLOR = "current-phase";
    public static final String CURRENT_SPEED = "current-speed";
    public static final String COUNTDOWN_RUNNING = "countdown-running";
    public static final String COUNTDOWN_DURATION = "countdown-duration";   //In ms
    public static final String COUNTDOWN_DURATION_LEFT = "countdown-duration-left";   //In ms
    public static final String BEEP = "beep";

    /* Global spm setting to hold current spm */
    public static int spm;

    //UI elements
    private ProgressBar waveProgress;
    private TextView countdownView;
    private TextView spmTextView;
    private TextView progressTextView;

    private ImageView water;


    private SharedPreferences pref;

    //autoWave values
    private static int workoutRunning = 0;     //0 = off, 1 = autoWave, 2 = autoProgress

    public static int windowWidth;
    public static int windowHeight;
    public static int statusbarHeight;

    private static BeeperService mBeeperService;
    private static boolean mIsBound;

    private static Toast mToast;

    static int random;
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

        //Initialize spm at last setting, or default at 0
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        spm = pref.getInt(SPM_SETTING, 0);

        //Initialize UI elements

        waveProgress = (ProgressBar) findViewById(R.id.wave_progress_bar_2);
        waveProgress.setVisibility(View.INVISIBLE);

        spmTextView = (TextView) findViewById(R.id.SpmTextView_2);
        spmTextView.setText(String.valueOf(spm));
        progressTextView = (TextView) findViewById(R.id.progressTextView);

        Button waveButton = (Button) findViewById(R.id.create_workout_button);
        waveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(WaveActivity.this, EntryFormActivity.class);
                startActivity(intent);
            }
        });

        int resource = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resource > 0) {
            statusbarHeight = getResources().getDimensionPixelSize(resource);
//            Log.d("Statusbar Height!!!: ", "" + statusbarHeight);
        }

        PackageManager manager = getPackageManager();
        int permission = manager.checkPermission("android.permission.ACCESS_FINE_LOCATION",
                "com.batyanko.strokeratecoach");
        boolean hasPermission = (permission == PackageManager.PERMISSION_GRANTED);
//
        if (!hasPermission) {
            Log.d("I CAN HAZ PERMISSION?", "NO");
            ActivityCompat.requestPermissions(this,
                    new String[]{"android.permission.ACCESS_FINE_LOCATION"}, MY_LOCATION_PERMISSION);
        }

        countdownView = findViewById(R.id.countdown_text_view);
        countdownView.setVisibility((View.INVISIBLE));
    }

    @Override
    protected void onStart() {
        super.onStart();
        onProgressChange();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    /**
     * Reset the GUI at workout's end.
     */
    public void resetGUI() {
        waveProgress.setVisibility(View.INVISIBLE);
        progressTextView.setVisibility(View.INVISIBLE);
//        spmTextView.setText("0");
//        spmTextView.setBackgroundColor(Color.TRANSPARENT);
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

        if (s.equals(SPM_SETTING)) {
            spm = sharedPreferences.getInt(SPM_SETTING, 22);
            spmTextView.setText(String.valueOf(spm));
        } else if (s.equals(SWITCH_SETTING)) {
            int operation = sharedPreferences.getInt(OPERATION_SETTING, 0);
            switch (operation) {
                case WORKOUT_PROGRESS: {
                    workoutRunning = WORKOUT_PROGRESS;
                    onProgressChange();
                    break;
                }
                case WORKOUT_STOP: {
                    workoutRunning = WORKOUT_STOP;
                    resetGUI();
                    break;
                }
            }
        } else if (s.equals(PHASE_PROGRESS)) {
            onProgressChange();
        } else if (s.equals(CURRENT_COLOR)) {
            //TODO show color somewhere on the UI
//            spmTextView.setBackgroundColor(sharedPreferences.getInt(CURRENT_COLOR, Color.TRANSPARENT));
        } else if (s.equals(COUNTDOWN_DURATION_LEFT)) {
            //TODO show a countdown
            int durationLeft = sharedPreferences.getInt(COUNTDOWN_DURATION_LEFT, 0) / 1000;
            String countdownString = durationLeft + "";
            Log.d("TEHDURATION", durationLeft + "");
            if (durationLeft == 0) {
                countdownView.setVisibility(View.INVISIBLE);
//                SlideFragment.lastClickedEngageButton.setBackgroundResource(R.drawable.ic_menu_play_clip_negative);
            } else {
                countdownView.setText(countdownString);
                countdownView.bringToFront();
//                countdownView.requestLayout();
                countdownView.setVisibility(View.VISIBLE);
            }
        } else if (s.equals(BEEP)) {
            Log.d("BEEP", "beep");
            spmTextView.startAnimation(AnimationUtils.loadAnimation(WaveActivity.this, R.anim.on_click));
        }
    }

    /**
     * Update GUI according to workout
     */
    private void onProgressChange() {
        if (workoutRunning == WORKOUT_STOP) {
            return;
        }
        int[] progress = new int[2];
        progress[0] = pref.getInt(PHASE_PROGRESS, 0);
        progress[1] = pref.getInt(PHASE_LENGTH, 0);

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
//        spmTextView.setBackgroundColor(pref.getInt(CURRENT_COLOR, Color.TRANSPARENT));
    }

    public void onSpmClicked(View view) {
//        view.performClick();
        spmTextView.startAnimation(AnimationUtils.loadAnimation(WaveActivity.this, R.anim.on_click));
    }
}