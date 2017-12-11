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

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.batyanko.strokeratecoach.R;
import com.batyanko.strokeratecoach.sync.BeeperTasks;

import static com.batyanko.strokeratecoach.data.WorkoutContract.*;

/**
 * Created by batyanko on 8/23/17.
 */

public class WorkoutDBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "presets.db";
    private static final int DATABASE_VERSION = 1;

    private Context mContext;

    private int mTime;

    public WorkoutDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        final String SQL_CREATE_PRESET_TABLE = "CREATE TABLE " + WorkoutEntry1.TABLE_NAME_PRESETS + " (" +
                WorkoutEntry1._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                WorkoutEntry1.COLUMN_NAME + " TEXT DEFAULT 'Untitled', " +
                WorkoutEntry1.COLUMN_DESC + " TEXT DEFAULT 'No description', " +
                WorkoutEntry1.COLUMN_TIMESTAMP + " INTEGER DEFAULT CURRENT_TIMESTAMP, " +
                WorkoutEntry1.COLUMN_SPP_TYPE + " INTEGER DEFAULT 0, " +
                WorkoutEntry1.COLUMN_SPP_CSV + " TEXT NOT NULL, " +
                WorkoutEntry1.COLUMN_GEARS_CSV + " TEXT NOT NULL" +
                "); ";

        final String SQL_CREATE_HISTORY_TABLE = "CREATE TABLE " + WorkoutEntry1.TABLE_NAME_HISTORY + " (" +
                WorkoutEntry1._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                WorkoutEntry1.COLUMN_NAME + " TEXT DEFAULT 'Untitled', " +
                WorkoutEntry1.COLUMN_DESC + " TEXT DEFAULT 'No description', " +
                WorkoutEntry1.COLUMN_TIMESTAMP + " INTEGER DEFAULT CURRENT_TIMESTAMP, " +
                WorkoutEntry1.COLUMN_SPP_TYPE + " INTEGER DEFAULT 0, " +
                WorkoutEntry1.COLUMN_SPP_CSV + " TEXT NOT NULL, " +
                WorkoutEntry1.COLUMN_GEARS_CSV + " TEXT NOT NULL" +
                "); ";

        sqLiteDatabase.execSQL(SQL_CREATE_PRESET_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_HISTORY_TABLE);

        //Initialize with factory presets
        Log.d("SQLite BENCH", "START");
        addPreset(sqLiteDatabase,
                mContext.getString(R.string.small_wave_name),
                mContext.getString(R.string.small_wave_desc),
                mContext.getString(R.string.small_wave_spp),
                mContext.getString(R.string.small_wave_gears),
                BeeperTasks.SPP_TYPE_STROKES
        );

        addPreset(sqLiteDatabase,
                mContext.getString(R.string.big_wave_name),
                mContext.getString(R.string.big_wave_desc),
                mContext.getString(R.string.big_wave_spp),
                mContext.getString(R.string.big_wave_gears),

                BeeperTasks.SPP_TYPE_STROKES
        );

        addPreset(sqLiteDatabase,
                mContext.getString(R.string.progress50_name),
                mContext.getString(R.string.progress50_desc),
                mContext.getString(R.string.progress50_spp),
                mContext.getString(R.string.progress50_gears),
                BeeperTasks.SPP_TYPE_METERS
        );
    }

/*    public int getRowCount(SQLiteDatabase sqLiteDatabase) {
        final String SQL_GET_ROW_COUNT = "SELECT COUNT(*) FROM " +
                WorkoutEntry1.tableName
        sqLiteDatabase.rawQuery()
    }*/

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + WorkoutEntry1.TABLE_NAME_PRESETS);
        onCreate(sqLiteDatabase);
    }

    private long addPreset(SQLiteDatabase db, String name, String description,
                           String spp, String gears, int sppType) {
        ContentValues cv = new ContentValues();
        cv.put(WorkoutEntry1.COLUMN_NAME, name);
        cv.put(WorkoutEntry1.COLUMN_DESC, description);
        cv.put(WorkoutEntry1.COLUMN_SPP_CSV, spp);
        cv.put(WorkoutEntry1.COLUMN_GEARS_CSV, gears);
//        cv.put(WorkoutEntry1.COLUMN_TIMESTAMP, time);
        cv.put(WorkoutEntry1.COLUMN_SPP_TYPE, sppType);
        return db.insert(WorkoutEntry1.TABLE_NAME_PRESETS, null, cv);
    }
}
