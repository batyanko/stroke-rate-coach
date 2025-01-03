package com.batyanko.strokeratecoach.Utils;


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
    Timer workoutTimer;
    TimerTask workoutTimerTask;

    private static int strokeCountTrigger = 0;


    private void startTheTempo() {

        strokeCount = 0;

        strokeDuration = SpmUtilities.spmToMilis(spm);

        try {
            workoutTimer.cancel();
            workoutTimerTask.cancel();
        } catch (IllegalStateException e) {
            Log.d("Exception", "Cancelled or scheduled");
        } catch (NullPointerException e) {
            Log.d("Exception", "Null pointer");
        }

        workoutTimerTask = new TimerTask() {
            @Override
            public void run() {
                toneGen1.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 150);
                strokeCount++;
                if (strokeCountTrigger > 0 && strokeCount >= strokeCountTrigger) {
                    cancel();
                    new Runnable() {
                        @Override
                        public void run() {
                            malkaValna();
                        }
                    };
                }
            }
        };

        workoutTimer = new java.util.Timer();
        workoutTimer.scheduleAtFixedRate(workoutTimerTask, 1, strokeDuration);

        spmString = String.valueOf(spm);
        textView.setText(spmString);
    }



    public void setSpmFromDigital(int digitalInput, View view) {
        if (firstDigit != 0) {
            MainActivity.flushGUI();
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
*//*
            Log.d("GridHeight!!!: ", "" + dialGrid.getHeight());
            Log.d("WindowHeight!!!: ", "" + windowHeight);

            ConstraintLayout constraintLayout = (ConstraintLayout) findViewById(R.id.activity_main);
            Log.d("Constrai PostCreate???:", "" + constraintLayout.getHeight());*//*

        }
    }*/

    public static long spmToMilis(int spm) {
/*        if (spm < 10) {
            return 0;
        }*/

        //int strokeRate = Integer.parseInt(spmString.substring(0, 2));
        Long strokeDuration = (long) (1 / (((double) spm) / 60) * 1000);

        return strokeDuration;
    }



}
