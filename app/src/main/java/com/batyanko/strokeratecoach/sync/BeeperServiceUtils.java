package com.batyanko.strokeratecoach.sync;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by batyanko on 10/19/17.
 */

public class BeeperServiceUtils {

    public BeeperServiceUtils() {

    }

    private static BeeperService mBeeperService;
    private static boolean mIsBound = false;

    public static BeeperService getBeeperService() {
        if (mBeeperService == null) {
	        createServiceConnection();
        }
        return mBeeperService;
    }
    public static ServiceConnection getServiceConnection() {
        return createServiceConnection();
    }

    private static ServiceConnection createServiceConnection() {
        ServiceConnection mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                mBeeperService = ((BeeperService.BeeperBinder) iBinder).getService();

                Log.d("mBeeperService", "" + mBeeperService);

                //Check if Service is running and flush UI
//            checkBeeper(intent);
//            Boolean bool = pref.getBoolean(WaveActivity.SWITCH_SETTING, true);
//            pref.edit().putBoolean(WaveActivity.SWITCH_SETTING, !bool).apply();
//            Toast.makeText(getContext(), "Bound!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                mBeeperService = null;
//            Toast.makeText(getContext(), "Unbound", Toast.LENGTH_SHORT).show();
            }
        };
        return mConnection;
    }

    public static void doBindService(Intent intent, Context context, ServiceConnection connection) {
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    public static void doUnbindService(Context context, ServiceConnection connection) {
        if (mIsBound) {
            context.unbindService(connection);
            mIsBound = false;
        }
    }
}
