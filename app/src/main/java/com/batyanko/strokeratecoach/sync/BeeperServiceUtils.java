/*
 * Copyright (C) 2018 Yanko Georgiev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.batyanko.strokeratecoach.sync;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

/**
 * Created by batyanko on 10/19/17.
 */

public class BeeperServiceUtils {

    public BeeperServiceUtils() {

    }

    private static BeeperService mBeeperService;
    private static boolean mIsBound = false;
    private static ServiceConnection serviceConnection;

    public static BeeperService getBeeperService() {
        if (mBeeperService == null) {
            createServiceConnection();
        }
        return mBeeperService;
    }

    public static ServiceConnection getServiceConnection() {
        return createServiceConnection();
    }

    public static boolean serviceIsRunning() {
        return mBeeperService != null;
    }

    private static ServiceConnection createServiceConnection() {
        ServiceConnection mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                mBeeperService = ((BeeperService.BeeperBinder) iBinder).getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                mBeeperService = null;
            }
        };
        serviceConnection = mConnection;
        return mConnection;
    }

    public static void doBindService(Intent intent, Context context, ServiceConnection connection) {
        if (mIsBound) {
            doUnbindService(context);
        }
        mIsBound = context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    public static void doUnbindService(Context context) {

        if (mIsBound) {
            try {
                context.unbindService(serviceConnection);
            } catch (IllegalArgumentException exception) {
                //if not bound then everything is alrighty...
            }
            mIsBound = false;
        }
    }
}
