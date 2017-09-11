/*
 * Copyright (C) 2017 Yanko Georgiev
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

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.util.Log;

public class BeeperService extends NonStopIntentService {

    private  BeeperTasks beeperTasks;
    public BeeperService() {
        super("BeeperService");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        String action = intent.getAction();
        beeperTasks = new BeeperTasks();
        int[] workoutSpp= intent.getIntArrayExtra(BeeperTasks.EXTRA_WORKOUT_SPP);
        int[] workoutGears= intent.getIntArrayExtra(BeeperTasks.EXTRA_WORKOUT_GEARS);
        int sppType = intent.getIntExtra(BeeperTasks.EXTRA_WORKOUT_SPP_TYPE, 1);
        if (workoutSpp == null) {
            Log.d("EXTRAARRAYS", "spp is null");
        }
        if (workoutGears == null) {
            Log.d("EXTRAARRAYS", "gears is null");
        } else {
            Log.d("EXTRAARRAYS", "" + workoutGears[0]);
        }
        Log.d("WOOSH", "-- Here Be Otherthreadly Stuff--");
        Log.d("HANDLED", "PASsS");
        beeperTasks.executeTask(this, action, workoutSpp, workoutGears, sppType);
        Log.d("HANDLED", "PASsS2");
    }

    //TODO call stopSelf();


    @Override
    public void onDestroy() {
        //TODO stop threads?
        stopSelf();
        super.onDestroy();
    }
}
