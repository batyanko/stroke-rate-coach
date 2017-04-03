package com.example.yanko.strokeratecoach.Utils;

import android.util.Log;

import com.example.yanko.strokeratecoach.MainActivity;

/**
 * Created by yanko on 3/31/17.
 */

public class RowingUtilities {
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
