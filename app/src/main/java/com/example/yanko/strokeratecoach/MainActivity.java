package com.example.yanko.strokeratecoach;

import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.preference.PreferenceManager;
import android.support.v4.content.SharedPreferencesCompat;
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

public class MainActivity extends AppCompatActivity implements TextWatcher {

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

    Button changerButton;
    Button stopperButton;

    boolean isFirstRun = true;

    private SharedPreferences pref;
    
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

        changerButton = (Button) findViewById(R.id.changer_button);

//        changerButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                startTheTempo();
//            }
//        });

        stopperButton = (Button) findViewById(R.id.stopper_button);
        stopperButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopTheTempo();
            }
        });

        isFirstRun = false;
    }

    @Override
    protected void onStart() {
        startTheTempo();
        super.onStart();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {

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

    //Method to start beeping in the rate needed

    private void startTheTempo() {
//        strokeRateString = spmEditText.getText().toString();

        if (strokeRateString.length() < 2) {
            /*if (isFirstRun) {
                strokeRateString = textView.getText().toString();
                isFirstRun = false;
            } else */
            return;
        }
        Log.v("StrokeRateString", strokeRateString);


        strokeRate = Integer.parseInt(strokeRateString.substring(0, 2));
//        Log.v("Parsed Thing", Integer.toString(strokeRate));
        strokeDuration = (long) (1 / (((double) strokeRate) / 60) * 1000);
//        Log.v("Non-positive thing", Long.toString(strokeDuration));

        textView.setText(strokeRateString);
/*                if (!timerTask.equals(null)) {

                }*/

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
            }
        };

        timer.schedule(timerTask, 1, strokeDuration);

        spmEditText.selectAll();

//                toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150);

                /*try {
                    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    Ringtone r = RingtoneManager.getRingtone(MainActivity.this, notification);
                    r.play();
                } catch (Exception e) {
                    e.printStackTrace();
                }*/
    }


/*    @Override
    protected void onResume() {
        textView.setText("Boo!");

        super.onResume();
    }*/

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

//
//            // get entered value (if required)
//            String enteredValue  = s.toString();
//
//            String newValue = "20";
//
//            // don't get trap into infinite loop
//            mWasEdited = true;
//            // just replace entered value with whatever you want
//            s.replace(0, s.length(), newValue);

        }
    }

    @Override
    protected void onStop() {
        stopTheTempo();
        super.onStop();
    }
}
