package com.batyanko.strokeratecoach.Utils;

import static com.batyanko.strokeratecoach.WaveActivity.MY_LOCATION_PERMISSION;
import static com.batyanko.strokeratecoach.WaveActivity.NOTIFICATION_PERMISSION;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

/**
 * Created by yanko on 6/12/17.
 */

public class WaveUtilities {
    private static Toast mToast;

    public static void requestLocation(Activity activity) {
        PackageManager manager = activity.getPackageManager();
        int permission = manager.checkPermission("android.permission.ACCESS_FINE_LOCATION",
                "com.batyanko.strokeratecoach");
        boolean hasPermission = (permission == PackageManager.PERMISSION_GRANTED);

        if (!hasPermission) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{"android.permission.ACCESS_FINE_LOCATION"}, MY_LOCATION_PERMISSION);
        }
    }

    public static void requestNotifications(Activity activity) {
        PackageManager manager = activity.getPackageManager();
        int permission = manager.checkPermission("android.permission.POST_NOTIFICATIONS",
                "com.batyanko.strokeratecoach");
        boolean hasPermission = (permission == PackageManager.PERMISSION_GRANTED);

        if (!hasPermission) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{"android.permission.POST_NOTIFICATIONS"}, NOTIFICATION_PERMISSION);
        }
    }

    public static void ShowShortToast(String string, Context context) {
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(context, string, Toast.LENGTH_SHORT);
        mToast.show();
    }

    public static void ShowLongToast(String string, Context context) {
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(context, string, Toast.LENGTH_LONG);
        mToast.show();
    }
}
