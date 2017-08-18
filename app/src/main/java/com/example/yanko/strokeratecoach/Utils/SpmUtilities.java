package com.example.yanko.strokeratecoach.Utils;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.ToneGenerator;
import android.support.constraint.ConstraintLayout;
import android.util.Log;
import android.view.View;

import com.example.yanko.strokeratecoach.MainActivity;
import com.example.yanko.strokeratecoach.R;

import java.util.*;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by yanko on 3/31/17.
 */

public class SpmUtilities {

    /*public static int firstDigit;
    View firstDigitView;
    public static int spm;
    private SharedPreferences pref;
    private int strokeCount;
    private long strokeDuration;

    ToneGenerator toneGen1;
    Timer timer;
    TimerTask timerTask;

    private static int strokeCountTrigger = 0;


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
                if (strokeCountTrigger > 0 && strokeCount >= strokeCountTrigger) {
                    cancel();
                    //TODO: Only change views on the UI thread
                    new Runnable() {
                        @Override
                        public void run() {
                            malkaValna();
                        }
                    };
                }
            }
        };

        timer = new java.util.Timer();
        timer.scheduleAtFixedRate(timerTask, 1, strokeDuration);

        spmString = String.valueOf(spm);
        textView.setText(spmString);
    }



    public void setSpmFromDigital(int digitalInput, View view) {
        if (firstDigit != 0) {
            MainActivity.endExercise();
            firstDigitView.setBackgroundColor(Color.TRANSPARENT);
            spm = firstDigit * 10 + digitalInput;
            //TODO: make startTheTempo() use spm instead spmString
//            spmString = String.valueOf(spm);
//            Log.d("SpmString / spm: ", spmString + " / " + spm);
            pref.edit().putInt("spm", spm).apply();
            startTheTempo();
            firstDigit = 0;

        } else {
            firstDigit = digitalInput;
            firstDigitView = view;
            view.setBackgroundColor(Color.RED);
*//*
            Log.d("GridHeight!!!: ", "" + dialGrid.getHeight());
            Log.d("WindowHeight!!!: ", "" + windowHeight);

            ConstraintLayout constraintLayout = (ConstraintLayout) findViewById(R.id.activity_main);
            Log.d("Constrai PostCreate???:", "" + constraintLayout.getHeight());*//*

        }
    }*/

    public static long spmToMilis(int spm) {
        if (spm < 10) {
            return 0;
        }

        //int strokeRate = Integer.parseInt(spmString.substring(0, 2));
        Long strokeDuration = (long) (1 / (((double) spm) / 60) * 1000);

        Log.d("Produced duration: ", spm + " / " + strokeDuration);
        return strokeDuration;
    }



}
