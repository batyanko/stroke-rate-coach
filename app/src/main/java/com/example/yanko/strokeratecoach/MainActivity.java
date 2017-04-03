package com.example.yanko.strokeratecoach;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.yanko.strokeratecoach.Utils.RowingUtilities;

import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {

    public static final int DEFAULT_RATE = 22;
    public static final String RATE_KEY = "rate";
    public int firstDigit;
    public int spm;
    int strokeRate;
    long strokeDuration;
    String spmString;
    ToneGenerator toneGen1;
    Timer timer;
    TimerTask timerTaskBlueprint;
    TimerTask timerTask;

    ProgressBar waveProgress;
    Button waveButton;
    TextView textView;
    EditText spmEditText;

    Button[] digits;
    Button dig2;

    private int[] colors;

    Button speedButton;
    Button stopperButton;

    private int progressVisibility;

    boolean isFirstRun = true;

    private SharedPreferences pref;

    //malkaValna testing
    private static int strokeCount = 0;
    private static int strokeCountTrigger = 0;
    private static int phase = 0;

    //VariableListener testing
    private VariableChangeListener variableChangeListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firstDigit = 0;

        pref = PreferenceManager.getDefaultSharedPreferences(this);
        spm = pref.getInt("zz", 22);

        //Initialize UI elements
        waveButton = (Button) findViewById(R.id.wave_button);
        waveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                phase = 0;
                malkaValna();
            }
        });

        waveProgress = (ProgressBar) findViewById(R.id.wave_progress_bar);
        waveProgress.setVisibility(View.INVISIBLE);

        isFirstRun = false;

        textView = (TextView) findViewById(R.id.textView);

        spmEditText = (EditText) findViewById(R.id.rateInputField);


        //spmEditText.addTextChangedListener(new CustomWatcher());
        toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);

        stopperButton = (Button) findViewById(R.id.stopper_button);
        stopperButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                waveEnder();
            }
        });

        speedButton = (Button) findViewById(R.id.speed_button);
        speedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SpeedActivity.class);
                startActivity(intent);
            }
        });

        //TODO: Find a way to use colors from xml...
        colors = new int[2];
        colors[0] = Color.BLUE;
        colors[1] = Color.WHITE;

        //Find out hex colors...
//        String hexColor = String.format("#%06X", (0xFFFFFF & colors[0]));
//        Toast.makeText(this, hexColor, Toast.LENGTH_LONG).show();

        digits = new Button[10];

        digits[1] = (Button) findViewById(R.id.dig_1);
        digits[2] = (Button) findViewById(R.id.dig_2);
        digits[3] = (Button) findViewById(R.id.dig_3);
        digits[4] = (Button) findViewById(R.id.dig_4);
        digits[5] = (Button) findViewById(R.id.dig_5);
        digits[6] = (Button) findViewById(R.id.dig_6);
        digits[7] = (Button) findViewById(R.id.dig_7);
        digits[8] = (Button) findViewById(R.id.dig_8);
        digits[9] = (Button) findViewById(R.id.dig_9);
        digits[0] = (Button) findViewById(R.id.dig_0);

        digits[1].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSpmFromDigital(1);
            }
        });
        digits[2].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSpmFromDigital(2);
            }
        });
        digits[3].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSpmFromDigital(3);
            }
        });
        digits[4].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSpmFromDigital(4);
            }
        });
        digits[5].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSpmFromDigital(5);
            }
        });
        digits[6].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSpmFromDigital(6);
            }
        });
        digits[7].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSpmFromDigital(7);
            }
        });
        digits[8].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSpmFromDigital(8);
            }
        });
        digits[9].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSpmFromDigital(9);
            }
        });
        digits[0].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSpmFromDigital(0);
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
//                    strokeCount = 0;
//                    strokeCountTrigger = 5;
//                    spmString = "32";
//                    startTheTempo();
                }
            }
        };

        timer = new Timer();
        timer.scheduleAtFixedRate(timerTask, 1, strokeDuration);

        spmString = String.valueOf(spm);
        textView.setText(spmString);

        spmEditText.selectAll();
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



    //Try to make some waves...
    private void malkaValna() {

        //
        final String[] GEARS = new String[] {
                "40", "20"
        };
        final int[] STROKES_PER_PHASE = new int[] {
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
        int progress = (int) (((float)phase/(float)phasesTotal)*100);
        waveProgress.setProgress(progress);
        startTheTempo();
    }

    private void waveEnder() {
        phase = 0;
        waveProgress.setVisibility(View.INVISIBLE);
        stopTheTempo();

    }


    public void setSpmFromDigital (int digitalInput) {
        if (firstDigit != 0) {
            spm = firstDigit * 10 + digitalInput;
            //TODO: make startTheTempo() use spm instead spmString
            spmString = String.valueOf(spm);
            Log.d("SpmString / spm: ", spmString + " / " + spm);
            pref.edit().putInt("zz", spm).apply();
            startTheTempo();
            digits[firstDigit].setBackgroundColor(colors[0]);
            firstDigit = 0;

        } else {
            firstDigit = digitalInput;
            digits[firstDigit].setBackgroundColor(colors[1]);
        }

    }

    /*//Upon entry of 2 digits, start the tempo and clear the EditText
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
