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

/**
 * Created by batyanko on 9/4/17.
 */

import android.content.Intent;
import android.location.LocationManager;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.batyanko.strokeratecoach.Utils.NotificationUtils;

public class BeeperService extends NonStopIntentService {

    BeeperTasks beeperTasks;

    public BeeperTasks.TehLocListener locListener;
    public LocationManager locationManager;


    public BeeperService() {
        super("BeeperService");
    }

    public class BeeperBinder extends Binder {
        public BeeperService getService() {

            return BeeperService.this;
        }
    }

    public void doEpicShit(Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        String action = intent.getAction();
        if (beeperTasks == null) {
            beeperTasks = new BeeperTasks();
        }

        int[] workoutSpp = intent.getIntArrayExtra(BeeperTasks.EXTRA_WORKOUT_SPP);
        int[] workoutGears = intent.getIntArrayExtra(BeeperTasks.EXTRA_WORKOUT_GEARS);
        String sppType = intent.getStringExtra(BeeperTasks.EXTRA_WORKOUT_SPP_TYPE);

        beeperTasks.executeTask(this, action, workoutSpp, workoutGears, sppType);
    }

    public void stopShit(Intent intent) {

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private final IBinder mBinder = new BeeperBinder();

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        String action = intent.getAction();
        if (action.equals(BeeperTasks.ACTION_JUST_BIND)) {
            return;
        }

        beeperTasks = new BeeperTasks();
        int[] workoutSpp = intent.getIntArrayExtra(BeeperTasks.EXTRA_WORKOUT_SPP);
        int[] workoutGears = intent.getIntArrayExtra(BeeperTasks.EXTRA_WORKOUT_GEARS);
        String sppType = intent.getStringExtra(BeeperTasks.EXTRA_WORKOUT_SPP_TYPE);

        beeperTasks.executeTask(this, action, workoutSpp, workoutGears, sppType);
    }

    @Override
    public boolean stopService(Intent name) {
        beeperTasks.executeTask(this, BeeperTasks.ACTION_STOP_BEEP, null, null, BeeperTasks.SPP_TYPE_STROKES);
        return super.stopService(name);
    }

    @Override
    public void onDestroy() {
        if (beeperTasks != null) {
            beeperTasks.executeTask(this, BeeperTasks.ACTION_STOP_BEEP, null, null, BeeperTasks.SPP_TYPE_STROKES);
        }
        stopSelf();
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        NotificationUtils.clearAllNotifications(this);
        super.onTaskRemoved(rootIntent);
    }
}