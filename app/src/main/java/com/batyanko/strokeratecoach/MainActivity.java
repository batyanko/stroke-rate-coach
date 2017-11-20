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
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.batyanko.strokeratecoach.Utils.SpmUtilities;
import com.batyanko.strokeratecoach.Utils.DialGridAdapter;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

//TODO Remove MainActivity in favor if WaveActivity
public class MainActivity extends AppCompatActivity {

    public static final int DEFAULT_RATE = 22;
    public static final String RATE_KEY = "rate";

    //First digit of spm (Strokes per Minute) used to allow automatic spm
    // initialization upon dialing two digits
    public static int firstDigit;

    /* Global spm setting to hold current spm */
    public static int spm;

    //Variables used in beeping workoutTimer setup
    long strokeDuration;
    String spmString;
    ToneGenerator toneGen1;
    Timer timer;
    TimerTask timerTask;

    //UI elements
    ProgressBar waveProgress;
    Button waveButton;
    TextView textView;

    //TODO ??
    private int[] colors;

    //UI reference to dialGrid and an element of it
    GridView dialGrid;
    View firstDigitView;

    private SharedPreferences pref;

    //autoWave values
    private static int exerciseRunning = 0;     //0 = off, 1 = autoWave, 2 = autoProgress
                                                //9 = last phase in progress
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        windowWidth = metrics.widthPixels;
        windowHeight = metrics.heightPixels;

        firstDigit = 0;

        //Initialize spm at last setting, or default at 22
        pref = PreferenceManager.getDefaultSharedPreferences(this);

        spm = pref.getInt("spm", 22);

        //ProgressBar to show wave progress
        waveProgress = (ProgressBar) findViewById(R.id.wave_progress_bar);
        waveProgress.setVisibility(View.INVISIBLE);

        textView = (TextView) findViewById(R.id.SpmTextView);

        toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);

        //Initialize digital dial buttons

        waveButton = (Button) findViewById(R.id.wave_button);
        waveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                phase = 0;
                autoProgress();
//                Intent waveIntent = new Intent(MainActivity.this, WaveActivity.class);
//                startActivity(waveIntent);
            }
        });

        //TODO: Find a way to use colors from xml...
        colors = new int[2];
        colors[0] = Color.GREEN;
        colors[1] = Color.YELLOW;

        //Try with a GridView

        int resource = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resource > 0) {
            statusbarHeight = getResources().getDimensionPixelSize(resource);
//            Log.d("Statusbar Height!!!: ", "" + statusbarHeight);
        }

        //A dial that allows the user to set the spm rate
        dialGrid = (GridView) findViewById(R.id.dial_grid);
        dialGrid.setAdapter(new DialGridAdapter(this));

        //Define dial grid button functions
        dialGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if (position < 9) {
                    setSpmFromDigital(position + 1, view);
                } else if (position == 9) {
                    setSpmFromDigital(0, view);
                } else if (position == 10) {
                    endExercise();
                } else if (position == 11) {
                    Intent intent = new Intent(MainActivity.this, WaveActivity.class);
                    startActivity(intent);
                } else if (position == 12) {
                    Intent intent = new Intent(MainActivity.this, WaveActivity.class);
                    startActivity(intent);
                }
            }
        });

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

    //Method to start beeping in the specified spm

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
                if (exerciseRunning > 0 && strokeCount > strokeCountTrigger) {
                    cancel();

                    Log.d("EXCERCISE:", "" + exerciseRunning);
                    //TODO: Only change views on the UI thread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            switch (exerciseRunning) {
                                case 1:
                                    autoWave();
                                    break;
                                case 2:
                                    autoProgress();
                                    break;
                                default:
                                    Log.d("DEFAULT", "DEFAULT");
                                    endExercise();
                            }
                        }
                    });
                }
            }
        };

        timer = new Timer();
        timer.scheduleAtFixedRate(timerTask, 1, strokeDuration);

        spmString = String.valueOf(spm);
        textView.setText(spmString);
    }

    private void stopTheTempo() {
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

    private void autoWave() {
        //Set excercise to autoWave;
        exerciseRunning = 1;

        final int[] GEAR_SETTINGS = new int[]{
                32, 20
        };
        final int[] STROKES_PER_PHASE = new int[]{
                10, 20, 30, 40, 50, 60, 70
        };

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
            Log.d("Teh Array: ", Arrays.toString(ALL_PHASES));
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

        startPhase(ALL_PHASES[phase - 1], GEAR_SETTINGS[gear], color);
    }

    private void autoProgress() {

        //Set excercise to autoProgress;
        exerciseRunning = 2;

        final int[] GEAR_SETTINGS = new int[]{
                30, 40, 50
        };

        final int[] STROKES_PER_PHASE = new int[]{
                3, 3, 3
        };

        phasesTotal = GEAR_SETTINGS.length;

        final int[] COLOR_PROGRESSION = new int[]{
                Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW
        };

        phase++;

/*        if (phase == NUM_PHASES) {
            exerciseRunning = 0;
        }*/


        startPhase(
                STROKES_PER_PHASE[phase - 1],
                GEAR_SETTINGS[phase - 1],
                COLOR_PROGRESSION[phase - 1]
        );
    }

    private void startPhase(int lengthTrigger, int spm, int color) {
        strokeCountTrigger = lengthTrigger;
        this.spm = spm;
        textView.setBackgroundColor(color);
        final String phase_progress = phase + "/" + phasesTotal;
        waveButton.setText(phase_progress);
        waveProgress.setVisibility(View.VISIBLE);
        int progress = (int) (((float) phase / (float) phasesTotal) * 100);
        waveProgress.setProgress(progress);
        if (phasesTotal == phase) {exerciseRunning = 9;}
        startTheTempo();
    }

    public void endExercise() {
        phase = 0;
        exerciseRunning = 0;
        waveProgress.setVisibility(View.INVISIBLE);
//        textView.setBackgroundResource(0);
        textView.setBackgroundColor(Color.TRANSPARENT);
        stopTheTempo();
    }

    public void setSpmFromDigital(int digitalInput, View view) {
        if (firstDigit != 0) {
            endExercise();
            firstDigitView.setBackgroundColor(Color.TRANSPARENT);
            spm = firstDigit * 10 + digitalInput;
            //TODO: maendWaveke startTheTempo() use spm instead spmString
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

            ConstraintLayout constraintLayout = (ConstraintLayout) findViewById(R.id.activity_main);
            Log.d("Constrai PostCreate???:", "" + constraintLayout.getHeight());
        }
    }
}
