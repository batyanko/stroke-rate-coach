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
import android.content.Intent;
import android.util.Log;

public class BeeperIntentService extends IntentService {

    public BeeperIntentService () {
        super("BeeperIntentService");
    }
    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        int[] workoutSpp= intent.getIntArrayExtra(BeeperTasks.EXTRA_WORKOUT_SPP);
        int[] workoutGeears= intent.getIntArrayExtra(BeeperTasks.EXTRA_WORKOUT_GEARS);
        if (workoutSpp == null) {
            Log.d("EXTRAARRAYS", "spp is null");
        }
        if (workoutGeears == null) {
            Log.d("EXTRAARRAYS", "gears is null");
        } else {
            Log.d("EXTRAARRAYS", "" + workoutGeears[0]);
        }
        Log.d("WOOSH", "-- Here Be Otherthreadly Stuff--");
        Log.d("HANDLED", "PASsS");
        BeeperTasks.executeTask(this, action, workoutSpp, workoutGeears);
        Log.d("HANDLED", "PASsS2");
    }
}
