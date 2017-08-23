package com.example.yanko.strokeratecoach.data;

import android.provider.BaseColumns;
/**
 * Created by ku4ekasi4ka on 8/23/17.
 */

public class ExerciseContract {
    public static final class PresetEntry1 implements BaseColumns {
        public static final String TABLE_NAME = "presets";
        public static final String COLUMN_NAME = "preset_name";
        public static final String COLUMN_DESC = "reset_desc";
        public static final String COLUMN_TIMESTAMP = "date_added";
        public static final String COLUMN_EXERCISE_TYPE = "type";
        public static final String COLUMN_SPP_CSV = "spp";
        public static final String COLUMN_GEARS_CSV = "gears";
    }

    //Try if lots of colums work faster than parsing CSV
    public static final class PresetEntry2 implements BaseColumns {
        public static final String TABLE_NAME = "presets";
        public static final String COLUMN_NAME = "preset_name";
        public static final String COLUMN_DESC = "preset_desc";
        public static final String COLUMN_TIMESTAMP = "date_added";
        public static final String COLUMN_EXERCISE_TYPE = "type";
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
