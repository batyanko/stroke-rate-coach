package com.example.yanko.strokeratecoach;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    public static final String DEFAULT_RATE = "22";
    public static final String RATE_KEY = "rate";
    int strokeRate;
    long strokeDuration;
    String strokeRateString;
    TextView textView;
    EditText spmEditText;
    ToneGenerator toneGen1;
    Timer timer;
    TimerTask timerTaskBlueprint;
    TimerTask timerTask;

    Button speedButton;
    Button stopperButton;

    boolean isFirstRun = true;

    private SharedPreferences pref;

    private static int strokeCount = 0;
    private static int strokeCountTrigger = 0;

    private static int phase = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pref = PreferenceManager.getDefaultSharedPreferences(this);


        strokeRateString = pref.getString(RATE_KEY, DEFAULT_RATE);

        textView = (TextView) findViewById(R.id.textView);
        textView.setText(strokeRateString);

        spmEditText = (EditText) findViewById(R.id.rateInputField);
        spmEditText.addTextChangedListener(new CustomWatcher());
        toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);

        stopperButton = (Button) findViewById(R.id.stopper_button);
        stopperButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopTheTempo();
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

        isFirstRun = false;
    }

    @Override
    protected void onStart() {
        startTheTempo();
        Log.i("Stroke rate at start: ", strokeRateString);
        super.onStart();
    }

    @Override
    protected void onStop() {
        stopTheTempo();
        super.onStop();
    }




    //Method to start beeping in the rate needed

    private void startTheTempo() {

        if (strokeRateString.length() < 2) {
            return;
        }
        Log.v("StrokeRateString", strokeRateString);


        strokeRate = Integer.parseInt(strokeRateString.substring(0, 2));
        strokeDuration = (long) (1 / (((double) strokeRate) / 60) * 1000);

        textView.setText(strokeRateString);

        try {
            timer.cancel();
            timerTask.cancel();
        } catch (IllegalStateException e) {
            Log.d("Exception", "Cancelled or scheduled");
        } catch (NullPointerException e) {
            Log.d("Exception", "Null pointer");
        }
        timer = new Timer();

        timerTask = new TimerTask() {
            @Override
            public void run() {
                toneGen1.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 150);
                strokeCount++;
                if (strokeCountTrigger > 0 && strokeCount == strokeCountTrigger) {
//                    stopTheTempo();
                    //TODO use listener count strokes and to iterate through phases?
//                    malkaValna();
                }
            }
        };

        timer.schedule(timerTask, 1, strokeDuration);

        spmEditText.selectAll();
    }

    private void stopTheTempo() {
        try {
            timer.cancel();
            timerTask.cancel();
        } catch (IllegalStateException e) {
            Log.d("Exception", "Cancelled or scheduled");
        } catch (NullPointerException e) {
            Log.d("Exception", "Null pointer");
        }
    }

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
                strokeRateString = mCharSequence;
                startTheTempo();
                pref.edit().putString(RATE_KEY, strokeRateString).apply();
                s.replace(0, s.length(), "");
                mWasEdited = true;
                return;
            }
        }
    }

    //Make waves...
    private void malkaValna() {
        //Initialise phase to 1, or iterate through phases;
        stopTheTempo();
        phase++;
        Log.i("Phase at start: ", "" + phase);

        switch (phase) {
            case 1: {
                strokeCount = 0;
                strokeCountTrigger = 5;
                strokeRateString = "32";
                startTheTempo();
                break;
            }
            case 2: {
                strokeCount = 0;
                strokeCountTrigger = 5;
                strokeRateString = "20";
                startTheTempo();
                break;
            }
            default: stopTheTempo();
        }
    }


}
