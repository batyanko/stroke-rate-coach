package com.batyanko.strokeratecoach.Utils;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by yanko on 6/12/17.
 */

public class WaveUtilities {
    private static Toast mToast;

    public static void showShortToast (String string, Context context) {
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(context, string, Toast.LENGTH_SHORT);
        mToast.show();
    }
    public void showLongToast (String string, Context context) {
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(context, string, Toast.LENGTH_LONG);
        mToast.show();
    }
}
