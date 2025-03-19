package com.batyanko.strokeratecoach.Utils;


/**
 * Created by yanko on 3/31/17.
 */

public class SpmUtilities {
    public static long spmToMilis(int spm) {
        return (Long) (long) (1 / (((double) spm) / 60) * 1000);
    }
}
