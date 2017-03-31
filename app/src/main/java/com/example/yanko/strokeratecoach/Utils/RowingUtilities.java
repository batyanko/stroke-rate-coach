package com.example.yanko.strokeratecoach.Utils;

import android.util.Log;

/**
 * Created by yanko on 3/31/17.
 */

public class RowingUtilities {
    public static long spmToMilis(String spmString) {
        if (spmString.length() < 2) {
            return 0;
        }

        int strokeRate = Integer.parseInt(spmString.substring(0, 2));
        Long strokeDuration = (long) (1 / (((double) strokeRate) / 60) * 1000);

        Log.d("Produced duration: ", spmString + " / " + strokeDuration);
        return strokeDuration;
    }

}
