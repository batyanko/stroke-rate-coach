package com.example.yanko.strokeratecoach;

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

import com.example.yanko.strokeratecoach.Utils.SpmUtilities;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class WaveActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {


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
    private TextView textView;

    //TODO ??
    private int[] colors;

    //UI reference to dialGrid and an element of it
    GridView dialGrid;
    View firstDigitView;

    private SharedPreferences pref;

    //autoWave values
    private static int exerciseRunning = 0;     //0 = off, 1 = autoWave, 2 = autoProgress
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wave);

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        SlideFragment slideFragment = new SlideFragment();
        transaction.replace(R.id.slide_frame_layout, slideFragment);
        //TODO add YAbaDabaDooFragment
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

        textView = (TextView) findViewById(R.id.SpmTextView_2);

        toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);

        waveButton = (Button) findViewById(R.id.wave_button_2);
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

        runForestRun = new Runnable() {
            @Override
            public void run() {
                switch (exerciseRunning) {
                    case 1:
                        autoWave();
                        Log.d("YOOOOO", "HOORAAAYYYY");
                        break;
                    case 2:
                        Log.d("YOOOOO", "HOORAAAYYYY");
                        autoProgress();
                        break;
                    default:
                        endExercise();
                        Log.d("YOOOOO", "HOORAAAYYYY");
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
                    //TODO: Only change views on the UI thread

                    runOnUiThread(
                            runForestRun
                    );
                }
            }
        };

        timer = new Timer();
        timer.scheduleAtFixedRate(timerTask, 1, strokeDuration);

        spmString = String.valueOf(spm);
        textView.setText(spmString);
    }

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

        startPhase(ALL_PHASES[phase - 1],
                GEAR_SETTINGS[gear],
                color);
    }

    private void autoProgress() {

        //Set excercise to autoProgress;
        exerciseRunning = 2;

        final int[] GEAR_SETTINGS = new int[]{
                40, 50, 60
        };

        final int[] STROKES_PER_PHASE = new int[]{
                3, 3, 3
        };

        phasesTotal = GEAR_SETTINGS.length;

        final int[] COLOR_PROGRESSION = new int[]{
                Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW
        };

        phase++;

        if (phase == phasesTotal) {
            exerciseRunning = 0;
        }

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
        if (phase == phasesTotal) {exerciseRunning = 9;}
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

            ConstraintLayout constraintLayout = (ConstraintLayout) findViewById(R.id.activity_wave);
            Log.d("Constrai PostCreate???:", "" + constraintLayout.getHeight());
        }
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        //TODO Make shit actually listen for pref "spm"
        if (s.equals("spm")) {
            spm = sharedPreferences.getInt("spm", 22);
            startTheTempo();
        }
    }
}