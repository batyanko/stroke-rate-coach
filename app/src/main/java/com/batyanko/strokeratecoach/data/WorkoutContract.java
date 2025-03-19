/**
 * Copyright (C) 2016 The Android Open Source Project
 * Modifications Copyright (C) 2017 Yanko Georgiev
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
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
    public static final class WorkoutEntry1 implements BaseColumns {
        public static final String TABLE_NAME_PRESETS = "presets";
        public static final String TABLE_NAME_HISTORY = "history";
        public static final String TABLE_NAME_TRASH = "trash";
        public static final String COLUMN_NAME = "preset_name";
        public static final String COLUMN_DESC = "preset_desc";
        public static final String COLUMN_TIMESTAMP = "date_added";
        public static final String COLUMN_SPP_TYPE = "spp_type";
        public static final String COLUMN_SPP_CSV = "spp";
        public static final String COLUMN_GEARS_CSV = "gears";
    }
}
