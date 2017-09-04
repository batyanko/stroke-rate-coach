/**
 * Copyright (C) 2016 The Android Open Source Project
 * Modifications Copyright (C) 2017 Yanko Georgiev
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

package com.batyanko.strokeratecoach.data;

import android.provider.BaseColumns;
/**
 * Created by batyanko on 8/23/17.
 */

public class WorkoutContract {
    public static final class PresetEntry1 implements BaseColumns {
        public static final String TABLE_NAME = "presets";
        public static final String COLUMN_NAME = "preset_name";
        public static final String COLUMN_DESC = "preset_desc";
        public static final String COLUMN_TIMESTAMP = "date_added";
        public static final String COLUMN_WORKOUT_TYPE = "workout_type";
        public static final String COLUMN_SPP_TYPE = "spp_type";
        public static final String COLUMN_SPP_CSV = "spp";
        public static final String COLUMN_GEARS_CSV = "gears";
    }

    //Try if 9 extra columns work faster than parsing CSV
    public static final class PresetEntry2 implements BaseColumns {
        public static final String TABLE_NAME = "presets";
        public static final String COLUMN_NAME = "preset_name";
        public static final String COLUMN_DESC = "preset_desc";
        public static final String COLUMN_TIMESTAMP = "date_added";
        public static final String COLUMN_WORKOUT_TYPE = "type";
        public static final String COLUMN_SPP0 = "spp0";
        public static final String COLUMN_SPP1 = "spp1";
        public static final String COLUMN_SPP2 = "spp2";
        public static final String COLUMN_SPP3 = "spp3";
        public static final String COLUMN_SPP4 = "spp4";
        public static final String COLUMN_SPP5 = "spp5";
        public static final String COLUMN_SPP6 = "spp6";
        public static final String COLUMN_SPP7 = "spp7";
        public static final String COLUMN_SPP8 = "spp8";
        public static final String COLUMN_SPP9 = "spp9";
        public static final String COLUMN_GEARS0 = "gears0";
        public static final String COLUMN_GEARS1 = "gears1";
        public static final String COLUMN_GEARS2 = "gears2";
        public static final String COLUMN_GEARS3 = "gears3";
        public static final String COLUMN_GEARS4 = "gears4";
        public static final String COLUMN_GEARS5 = "gears5";
        public static final String COLUMN_GEARS6 = "gears6";
        public static final String COLUMN_GEARS7 = "gears7";
        public static final String COLUMN_GEARS8 = "gears8";
        public static final String COLUMN_GEARS9 = "gears9";
    }
}
