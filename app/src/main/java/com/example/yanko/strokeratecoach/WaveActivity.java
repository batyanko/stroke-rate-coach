package com.example.yanko.strokeratecoach;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.yanko.strokeratecoach.Utils.RowingUtilities;

import java.util.Timer;
import java.util.TimerTask;

public class WaveActivity extends AppCompatActivity {

    public int spm;
    long strokeDuration;
    String spmString;
    ToneGenerator toneGen1;
    Timer timer;
    TimerTask timerTask;

    ProgressBar waveProgress;
    Button waveButton;
    TextView textView;

    //malkaValna testing
    private static int strokeCount = 0;
    private static int strokeCountTrigger = 0;
    private static int phase = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wave);

        //Initialize UI elements

        waveProgress = (ProgressBar) findViewById(R.id.wave_progress_bar_2);
        waveProgress.setVisibility(View.INVISIBLE);

        textView = (TextView) findViewById(R.id.SpmTextView_2);

        toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);





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
        final int[] GEARS = new int[]{
                32, 20
        };
        final int[] STROKES_PER_PHASE = new int[]{
                10, 20, 30
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
                startPhase(PHASES_IN_WAVE, STROKES_PER_PHASE[0], GEARS[0], Color.RED);
                break;
            }
            case 2: {
                startPhase(PHASES_IN_WAVE, STROKES_PER_PHASE[0], GEARS[1], Color.GREEN);
                break;
            }
            case 3: {
                startPhase(PHASES_IN_WAVE, STROKES_PER_PHASE[1], GEARS[0], Color.RED);
                break;
            }
            case 4: {
                startPhase(PHASES_IN_WAVE, STROKES_PER_PHASE[1], GEARS[1], Color.GREEN);
                break;
            }
            case 5: {
                startPhase(PHASES_IN_WAVE, STROKES_PER_PHASE[2], GEARS[0], Color.RED);
                break;
            }
            case 6: {
                startPhase(PHASES_IN_WAVE, STROKES_PER_PHASE[2], GEARS[1], Color.GREEN);
                break;
            }
            case 7: {
                startPhase(PHASES_IN_WAVE, STROKES_PER_PHASE[1], GEARS[0], Color.RED);
                break;
            }
            case 8: {
                startPhase(PHASES_IN_WAVE, STROKES_PER_PHASE[1], GEARS[1], Color.GREEN);
                break;
            }
            case 9: {
                startPhase(PHASES_IN_WAVE, STROKES_PER_PHASE[0], GEARS[0], Color.RED);
                break;
            }

            default:
                endWave();
        }
    }

    private void startPhase(int phasesTotal, int lengthTrigger, int spm, int color) {
        strokeCountTrigger = lengthTrigger;
        this.spm = spm;
        textView.setBackgroundColor(color);
        final String phase_progress = phase + "/" + phasesTotal;
        waveButton.setText(phase_progress);
        waveProgress.setVisibility(View.VISIBLE);
        int progress = (int) (((float) phase / (float) phasesTotal) * 100);
        waveProgress.setProgress(progress);
        startTheTempo();
    }

    private void endWave() {
        phase = 0;
        waveProgress.setVisibility(View.INVISIBLE);
        stopTheTempo();
    }
}
