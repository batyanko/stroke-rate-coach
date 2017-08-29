package com.example.yanko.strokeratecoach.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.yanko.strokeratecoach.R;
import com.example.yanko.strokeratecoach.WaveActivity;

import static com.example.yanko.strokeratecoach.data.WorkoutContract.*;

/**
 * Created by ku4ekasi4ka on 8/23/17.
 */

public class PresetDBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "presets.db";

    private static final int DATABASE_VERSION = 1;

    private Context mContext;

    private int mTime;

    public PresetDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    //TODO Make some NOT NULL
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        final String SQL_CREATE_PRESET_TABLE = "CREATE TABLE " + PresetEntry1.TABLE_NAME + " (" +
                PresetEntry1._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                PresetEntry1.COLUMN_NAME + " TEXT NOT NULL, " +
                PresetEntry1.COLUMN_DESC + " TEXT DEFAULT 'No description.', " +
                PresetEntry1.COLUMN_TIMESTAMP + " INTEGER DEFAULT CURRENT_TIMESTAMP, " +
                PresetEntry1.COLUMN_WORKOUT_TYPE + " INTEGER, " +
                PresetEntry1.COLUMN_SPP_TYPE + " INTEGER, " +
                PresetEntry1.COLUMN_SPP_CSV + " TEXT, " +
                PresetEntry1.COLUMN_GEARS_CSV + " TEXT" +
                "); ";

        //Initialize with factory presets
        sqLiteDatabase.execSQL(SQL_CREATE_PRESET_TABLE);
        Log.d("SQLite BENCH", "START");
        addFactoryPreset(sqLiteDatabase,
                mContext.getString(R.string.small_wave_name),
                mContext.getString(R.string.small_wave_desc),
                mContext.getString(R.string.small_wave_spp),
                mContext.getString(R.string.small_wave_gears),
                WaveActivity.WORKOUT_PROGRESS,
                WaveActivity.SPP_UNIT_STROKES
                );

        addFactoryPreset(sqLiteDatabase,
                mContext.getString(R.string.big_wave_name),
                mContext.getString(R.string.big_wave_desc),
                mContext.getString(R.string.big_wave_spp),
                mContext.getString(R.string.big_wave_gears),

                WaveActivity.WORKOUT_PROGRESS,
                WaveActivity.SPP_UNIT_STROKES
        );

        addFactoryPreset(sqLiteDatabase,
                mContext.getString(R.string.progress50_name),
                mContext.getString(R.string.progress50_desc),
                mContext.getString(R.string.progress50_spp),
                mContext.getString(R.string.progress50_gears),
                WaveActivity.WORKOUT_PROGRESS,
                WaveActivity.SPP_UNIT_STROKES
        );

        Log.d("SQLite BENCH", "FINISH");
    }

/*    public int getRowCount(SQLiteDatabase sqLiteDatabase) {
        final String SQL_GET_ROW_COUNT = "SELECT COUNT(*) FROM " +
                PresetEntry1.TABLE_NAME
        sqLiteDatabase.rawQuery()
    }*/

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + PresetEntry1.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }

    private long addFactoryPreset(SQLiteDatabase db, String name, String description,
                                  String spp, String gears,int workoutType, int sppType) {
        ContentValues cv = new ContentValues();
        cv.put(PresetEntry1.COLUMN_NAME, name);
        cv.put(PresetEntry1.COLUMN_DESC, description);
        cv.put(PresetEntry1.COLUMN_SPP_CSV, spp);
        cv.put(PresetEntry1.COLUMN_GEARS_CSV, gears);
//        cv.put(PresetEntry1.COLUMN_TIMESTAMP, time);
        cv.put(PresetEntry1.COLUMN_WORKOUT_TYPE, workoutType);
        cv.put(PresetEntry1.COLUMN_SPP_TYPE, sppType);
        return db.insert(PresetEntry1.TABLE_NAME, null, cv);
    }
}
