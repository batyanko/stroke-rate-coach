package com.batyanko.strokeratecoach.sync;

import android.app.Notification;
import android.content.Context;
import android.util.Log;

/**
 * Created by batyanko on 9/4/17.
 */

public class BeeperTasks {
    public static final String ACTION_START_BEEP = "start-beep";
    public static final String ACTION_STOP_BEEP = "stop-beep";

    public static void executeTask(Context context, String action){
        if (action.equals(ACTION_START_BEEP)) {
            startBeeping(context);
        }
        Notification notification = new Notification();
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
    }

    private static void startBeeping(Context context) {
        Log.d("WOOSH", "--Epic Shit--");
    }


}
