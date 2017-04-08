package com.example.yanko.strokeratecoach;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.Guideline;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.yanko.strokeratecoach.Utils.RowingUtilities;
import com.example.yanko.strokeratecoach.Utils.TextAdapter;

import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {

    public static final int DEFAULT_RATE = 22;
    public static final String RATE_KEY = "rate";
    public int firstDigit;
    public int spm;
    long strokeDuration;
    String spmString;
    ToneGenerator toneGen1;
    Timer timer;
    TimerTask timerTask;

    ProgressBar waveProgress;
    Button waveButton;
    TextView textView;

    private int[] colors;


    GridView dialGrid;
    View firstDigitView;

    boolean isFirstRun = true;

    private SharedPreferences pref;

    //malkaValna testing
    private static int strokeCount = 0;
    private static int strokeCountTrigger = 0;
    private static int phase = 0;

    public static int windowWidth;
    public static int windowHeight;
    public static int statusbarHeight;

    //Torba s hitrosti

    //EditText spmEditText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        windowWidth = metrics.widthPixels;
        windowHeight = metrics.heightPixels;

        firstDigit = 0;

        pref = PreferenceManager.getDefaultSharedPreferences(this);
        spm = pref.getInt("zz", 22);

        //Initialize UI elements

        waveProgress = (ProgressBar) findViewById(R.id.wave_progress_bar);
        waveProgress.setVisibility(View.INVISIBLE);

        isFirstRun = false;

        textView = (TextView) findViewById(R.id.textView);

        //EditText watcher, in case if an EditText is used
        //spmEditText = (EditText) findViewById(R.id.rateInputField);
        //spmEditText.addTextChangedListener(new CustomWatcher());

        toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);

        //Initialize digital dial buttons

        waveButton = (Button) findViewById(R.id.wave_button);
        waveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                phase = 0;
                malkaValna();
            }
        });

        //TODO: Find a way to use colors from xml...
        colors = new int[2];
        colors[0] = Color.BLUE;
        colors[1] = Color.WHITE;

        //Find out hex colors...
//        String hexColor = String.format("#%06X", (0xFFFFFF & colors[0]));
//        Toast.makeText(this, hexColor, Toast.LENGTH_LONG).show();

        //Try with a GridView

        int resource = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resource > 0) {
            statusbarHeight = getResources().getDimensionPixelSize(resource);
            Log.d("Statusbar Height!!!: ", "" + statusbarHeight);
        }

        dialGrid = (GridView) findViewById(R.id.dial_grid);
        dialGrid.setAdapter(new TextAdapter(this));

        //Define dial grid button functions
        dialGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if (position < 9){
                    setSpmFromDigital(position + 1, view);
                } else if (position == 9) {
                    setSpmFromDigital(0, view);
                } else if (position == 10) {
                    waveEnder();
                } else if (position == 11) {
                    Intent intent = new Intent(MainActivity.this, SpeedActivity.class);
                    startActivity(intent);
                }

                Toast.makeText(MainActivity.this, "" + position, Toast.LENGTH_SHORT).show();
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
        stopTheTempo();
        super.onStop();
    }


    //Method to start beeping in the rate needed

    private void startTheTempo() {

        strokeCount = 0;

        strokeDuration = RowingUtilities.spmToMilis(spm);

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
                if (strokeCountTrigger > 0 && strokeCount == strokeCountTrigger) {
                    cancel();
                    //TODO: Only change views on the UI thread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            malkaValna();
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


    //Make some waves...
    private void malkaValna() {

        //
        final String[] GEARS = new String[]{
                "40", "20"
        };
        final int[] STROKES_PER_PHASE = new int[]{
                3
        };
        final int PHASES_IN_WAVE = 9;

        final int STROKES_TOTAL = STROKES_PER_PHASE[0] * PHASES_IN_WAVE;

        //Initialise phase to 1, or iterate through phases;
        stopTheTempo();

        //Initialize / iterate phase
        phase++;
        Log.i("Phase at start: ", "" + phase);

        switch (phase) {
            case 1: {
                phaseStarter(PHASES_IN_WAVE, STROKES_PER_PHASE[0], GEARS[0], Color.RED);
                break;
            }
            case 2: {
                phaseStarter(PHASES_IN_WAVE, STROKES_PER_PHASE[0], GEARS[1], Color.GREEN);
                break;
            }
            case 3: {
                phaseStarter(PHASES_IN_WAVE, STROKES_PER_PHASE[0], GEARS[0], Color.RED);
                break;
            }
            case 4: {
                phaseStarter(PHASES_IN_WAVE, STROKES_PER_PHASE[0], GEARS[1], Color.GREEN);
                break;
            }
            case 5: {
                phaseStarter(PHASES_IN_WAVE, STROKES_PER_PHASE[0], GEARS[0], Color.RED);
                break;
            }
            case 6: {
                phaseStarter(PHASES_IN_WAVE, STROKES_PER_PHASE[0], GEARS[1], Color.GREEN);
                break;
            }
            case 7: {
                phaseStarter(PHASES_IN_WAVE, STROKES_PER_PHASE[0], GEARS[0], Color.RED);
                break;
            }
            case 8: {
                phaseStarter(PHASES_IN_WAVE, STROKES_PER_PHASE[0], GEARS[1], Color.GREEN);
                break;
            }
            case 9: {
                phaseStarter(PHASES_IN_WAVE, STROKES_PER_PHASE[0], GEARS[0], Color.RED);
                break;
            }

            default:
                waveEnder();
        }
    }

    private void phaseStarter(int phasesTotal, int lengthTrigger, String spm, int color) {
        strokeCountTrigger = lengthTrigger;
        spmString = spm;
        textView.setBackgroundColor(color);
        final String phase_progress = phase + "/" + phasesTotal;
        waveButton.setText(phase_progress);
        waveProgress.setVisibility(View.VISIBLE);
        int progress = (int) (((float) phase / (float) phasesTotal) * 100);
        waveProgress.setProgress(progress);
        startTheTempo();
    }

    private void waveEnder() {
        phase = 0;
        waveProgress.setVisibility(View.INVISIBLE);
        stopTheTempo();

    }


    public void setSpmFromDigital(int digitalInput, View view) {
        if (firstDigit != 0) {
            firstDigitView.setBackgroundColor(Color.TRANSPARENT);
            spm = firstDigit * 10 + digitalInput;
            //TODO: make startTheTempo() use spm instead spmString
            spmString = String.valueOf(spm);
            Log.d("SpmString / spm: ", spmString + " / " + spm);
            pref.edit().putInt("zz", spm).apply();
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

    //Torba s hitrosti

    /*
    //Upon entry of 2 digits, start the tempo and clear the EditText
    public class CustomWatcher implements TextWatcher {

        private boolean mWasEdited = false;
        private int mCount = 0;

        String mCharSequence = "";

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            mCount++;
            mCharSequence = s.toString();
            Log.d("Char Sequence: ", mCharSequence);
        }

        @Override
        public void afterTextChanged(Editable s) {

            if (mWasEdited) {

                mWasEdited = false;
                return;
            }
//
            Log.d("Current count: ", "" + mCount);

            if (mCharSequence.length() >= 2) {
                mCount = 0;
                spmString = mCharSequence;
                waveEnder();
                startTheTempo();
                pref.edit().putString(RATE_KEY, spmString).apply();
                s.replace(0, s.length(), "");
                mWasEdited = true;
                return;
            }
        }
    }*/
}
