/**
 * Copyright (C) 2016 The Android Open Source Project
 * Modifications Copyright (C) 2017 Yanko Georgiev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.batyanko.strokeratecoach.data;

import static com.batyanko.strokeratecoach.data.WorkoutContract.WorkoutEntry1;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.batyanko.strokeratecoach.R;
import com.batyanko.strokeratecoach.sync.BeeperTasks;
import com.batyanko.strokeratecoach.workout.LegacyWorkoutParser;
import com.batyanko.strokeratecoach.workout.WorkoutCompiler;
import com.batyanko.strokeratecoach.workout.WorkoutDefinition;
import com.batyanko.strokeratecoach.workout.WorkoutJsonCodec;
import com.batyanko.strokeratecoach.workout.WorkoutValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class WorkoutDBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "presets.db";
    private static final int DATABASE_VERSION = 3;

    private final Context mContext;

    public static final String SQL_NON_UNIQUES = WorkoutEntry1.COLUMN_NAME + ", " +
            WorkoutEntry1.COLUMN_DESC + ", " +
            WorkoutEntry1.COLUMN_TIMESTAMP + ", " +
            WorkoutEntry1.COLUMN_SPP_TYPE + ", " +
            WorkoutEntry1.COLUMN_SPP_CSV + ", " +
            WorkoutEntry1.COLUMN_GEARS_CSV + ", " +
            WorkoutEntry1.COLUMN_SERIALIZED_TREE;
    public static final String SQL_MOVE_HISTORY_TO_TRASH = "INSERT INTO " +
            WorkoutEntry1.TABLE_NAME_TRASH +
            "(" + SQL_NON_UNIQUES + ")" +
            " SELECT " + SQL_NON_UNIQUES +
            " FROM " +
            WorkoutEntry1.TABLE_NAME_HISTORY +
            "; ";

    public static final String SQL_CLEAR_HISTORY = "DELETE FROM " +
            WorkoutEntry1.TABLE_NAME_HISTORY +
            "; ";

    public static final String SQL_CLEAR_TRASH = "DELETE FROM " +
            WorkoutEntry1.TABLE_NAME_TRASH +
            "; ";

    private final WorkoutRecordMapper recordMapper;
    private final String SQL_CREATE_TRASH_TABLE_V2 = createTableSql(
            WorkoutEntry1.TABLE_NAME_TRASH, "NO TITLE", "NO DESC", false);

    public WorkoutDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
        recordMapper = new WorkoutRecordMapper(
                new WorkoutJsonCodec(), new LegacyWorkoutParser(), new WorkoutCompiler());
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(createTableSql(WorkoutEntry1.TABLE_NAME_PRESETS,
                "Untitled", "No description", true));
        database.execSQL(createTableSql(WorkoutEntry1.TABLE_NAME_HISTORY,
                "Untitled", "No description", true));
        database.execSQL(createTableSql(WorkoutEntry1.TABLE_NAME_TRASH,
                "NO TITLE", "NO DESC", true));

        addFactoryPreset(database,
                R.string.small_wave_name,
                R.string.small_wave_desc,
                R.string.small_wave_spp,
                R.string.small_wave_gears,
                BeeperTasks.SPP_TYPE_STROKES);
        addFactoryPreset(database,
                R.string.big_wave_name,
                R.string.big_wave_desc,
                R.string.big_wave_spp,
                R.string.big_wave_gears,
                BeeperTasks.SPP_TYPE_STROKES);
        addFactoryPreset(database,
                R.string.progress50_name,
                R.string.progress50_desc,
                R.string.progress50_spp,
                R.string.progress50_gears,
                BeeperTasks.SPP_TYPE_METERS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            database.execSQL(SQL_CREATE_TRASH_TABLE_V2);
        }
        if (oldVersion < 3) {
            addSerializedTreeColumn(database, WorkoutEntry1.TABLE_NAME_PRESETS);
            addSerializedTreeColumn(database, WorkoutEntry1.TABLE_NAME_HISTORY);
            addSerializedTreeColumn(database, WorkoutEntry1.TABLE_NAME_TRASH);
            backfillTable(database, WorkoutEntry1.TABLE_NAME_PRESETS);
            backfillTable(database, WorkoutEntry1.TABLE_NAME_HISTORY);
            backfillTable(database, WorkoutEntry1.TABLE_NAME_TRASH);
        }
    }

    private String createTableSql(String tableName, String defaultName,
                                  String defaultDescription, boolean includeTree) {
        return "CREATE TABLE " + tableName + " (" +
                WorkoutEntry1._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                WorkoutEntry1.COLUMN_NAME + " TEXT DEFAULT '" + defaultName + "', " +
                WorkoutEntry1.COLUMN_DESC + " TEXT DEFAULT '" + defaultDescription + "', " +
                WorkoutEntry1.COLUMN_TIMESTAMP + " INTEGER DEFAULT CURRENT_TIMESTAMP, " +
                WorkoutEntry1.COLUMN_SPP_TYPE + " INTEGER DEFAULT 0, " +
                WorkoutEntry1.COLUMN_SPP_CSV + " TEXT NOT NULL, " +
                WorkoutEntry1.COLUMN_GEARS_CSV + " TEXT NOT NULL" +
                (includeTree ? ", " + WorkoutEntry1.COLUMN_SERIALIZED_TREE + " TEXT" : "") +
                "); ";
    }

    private void addSerializedTreeColumn(SQLiteDatabase database, String tableName) {
        if (hasColumn(database, tableName, WorkoutEntry1.COLUMN_SERIALIZED_TREE)) {
            return;
        }
        database.execSQL("ALTER TABLE " + tableName + " ADD COLUMN "
                + WorkoutEntry1.COLUMN_SERIALIZED_TREE + " TEXT");
    }

    private boolean hasColumn(SQLiteDatabase database, String tableName, String columnName) {
        try (Cursor cursor = database.rawQuery("PRAGMA table_info(" + tableName + ")", null)) {
            int nameIndex = cursor.getColumnIndexOrThrow("name");
            while (cursor.moveToNext()) {
                if (columnName.equals(cursor.getString(nameIndex))) {
                    return true;
                }
            }
        }
        return false;
    }

    private void backfillTable(SQLiteDatabase database, String tableName) {
        List<LegacyRow> rows = readLegacyRows(database, tableName);
        for (LegacyRow row : rows) {
            String serializedTree = serializeLegacyRow(row);
            if (serializedTree == null) {
                continue;
            }
            ContentValues values = new ContentValues();
            values.put(WorkoutEntry1.COLUMN_SERIALIZED_TREE, serializedTree);
            database.update(tableName, values,
                    WorkoutEntry1._ID + " = ?", new String[]{Long.toString(row.id)});
        }
    }

    private List<LegacyRow> readLegacyRows(SQLiteDatabase database, String tableName) {
        List<LegacyRow> rows = new ArrayList<>();
        String[] columns = {
                WorkoutEntry1._ID,
                WorkoutEntry1.COLUMN_SPP_CSV,
                WorkoutEntry1.COLUMN_GEARS_CSV,
                WorkoutEntry1.COLUMN_SPP_TYPE
        };
        try (Cursor cursor = database.query(tableName, columns, null, null, null, null, null)) {
            int idIndex = cursor.getColumnIndexOrThrow(WorkoutEntry1._ID);
            int sppIndex = cursor.getColumnIndexOrThrow(WorkoutEntry1.COLUMN_SPP_CSV);
            int gearsIndex = cursor.getColumnIndexOrThrow(WorkoutEntry1.COLUMN_GEARS_CSV);
            int typeIndex = cursor.getColumnIndexOrThrow(WorkoutEntry1.COLUMN_SPP_TYPE);
            while (cursor.moveToNext()) {
                rows.add(new LegacyRow(
                        cursor.getLong(idIndex),
                        cursor.getString(sppIndex),
                        cursor.getString(gearsIndex),
                        cursor.getString(typeIndex)));
            }
        }
        return rows;
    }

    private String serializeLegacyRow(LegacyRow row) {
        try {
            WorkoutDefinition definition = new LegacyWorkoutParser().parse(
                    row.spp, row.gears, row.sppType);
            new WorkoutCompiler().compile(definition);
            return new WorkoutJsonCodec().serialize(definition);
        } catch (WorkoutValidationException invalidLegacyRow) {
            return null;
        }
    }

    private void addFactoryPreset(SQLiteDatabase database, int nameId, int descriptionId,
                                  int sppId, int gearsId, String sppType) {
        String spp = mContext.getString(sppId);
        String gears = mContext.getString(gearsId);
        WorkoutDefinition definition = new LegacyWorkoutParser().parse(spp, gears, sppType);
        ContentValues values = new ContentValues();
        values.put(WorkoutEntry1.COLUMN_NAME, mContext.getString(nameId));
        values.put(WorkoutEntry1.COLUMN_DESC, mContext.getString(descriptionId));
        recordMapper.putDefinition(values, definition);
        database.insertOrThrow(WorkoutEntry1.TABLE_NAME_PRESETS, null, values);
    }

    private static final class LegacyRow {
        private final long id;
        private final String spp;
        private final String gears;
        private final String sppType;

        private LegacyRow(long id, String spp, String gears, String sppType) {
            this.id = id;
            this.spp = spp;
            this.gears = gears;
            this.sppType = sppType;
        }
    }
}
