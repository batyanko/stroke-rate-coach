package com.example.yanko.strokeratecoach.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static com.example.yanko.strokeratecoach.data.ExerciseContract.*;

/**
 * Created by ku4ekasi4ka on 8/23/17.
 */

public class PresetDBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "presets.db";

    private static final int DATABASE_VERSION = 1;

    public PresetDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    //TODO Make some NOT NULL
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        final String SQL_CREATE_PRESET_TABLE = "CREATE TABLE " + PresetEntry1.TABLE_NAME + " (" +
                PresetEntry1._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                PresetEntry1.COLUMN_NAME + " TEXT NOT NULL, " +
                PresetEntry1.COLUMN_DESC + " TEXT, " +
                PresetEntry1.COLUMN_TIMESTAMP + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                PresetEntry1.COLUMN_EXERCISE_TYPE + " INTEGER, " +
                PresetEntry1.COLUMN_SPP_CSV + " TEXT, " +
                PresetEntry1.COLUMN_GEARS_CSV + " TEXT" +
                "); ";

        sqLiteDatabase.execSQL(SQL_CREATE_PRESET_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + PresetEntry1.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}
