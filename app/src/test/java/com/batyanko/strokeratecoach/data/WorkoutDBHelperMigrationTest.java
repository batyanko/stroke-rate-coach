package com.batyanko.strokeratecoach.data;

import static com.batyanko.strokeratecoach.data.WorkoutContract.WorkoutEntry1.COLUMN_DESC;
import static com.batyanko.strokeratecoach.data.WorkoutContract.WorkoutEntry1.COLUMN_GEARS_CSV;
import static com.batyanko.strokeratecoach.data.WorkoutContract.WorkoutEntry1.COLUMN_NAME;
import static com.batyanko.strokeratecoach.data.WorkoutContract.WorkoutEntry1.COLUMN_SERIALIZED_TREE;
import static com.batyanko.strokeratecoach.data.WorkoutContract.WorkoutEntry1.COLUMN_SPP_CSV;
import static com.batyanko.strokeratecoach.data.WorkoutContract.WorkoutEntry1.COLUMN_SPP_TYPE;
import static com.batyanko.strokeratecoach.data.WorkoutContract.WorkoutEntry1.COLUMN_TIMESTAMP;
import static com.batyanko.strokeratecoach.data.WorkoutContract.WorkoutEntry1.TABLE_NAME_HISTORY;
import static com.batyanko.strokeratecoach.data.WorkoutContract.WorkoutEntry1.TABLE_NAME_PRESETS;
import static com.batyanko.strokeratecoach.data.WorkoutContract.WorkoutEntry1.TABLE_NAME_TRASH;
import static com.batyanko.strokeratecoach.data.WorkoutContract.WorkoutEntry1._ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.batyanko.strokeratecoach.workout.CompiledWorkout;
import com.batyanko.strokeratecoach.workout.LegacyWorkoutParser;
import com.batyanko.strokeratecoach.workout.PhaseNode;
import com.batyanko.strokeratecoach.workout.RepeatNode;
import com.batyanko.strokeratecoach.workout.WorkoutCompiler;
import com.batyanko.strokeratecoach.workout.WorkoutDefinition;
import com.batyanko.strokeratecoach.workout.WorkoutJsonCodec;
import com.batyanko.strokeratecoach.workout.WorkoutNode;
import com.batyanko.strokeratecoach.workout.WorkoutUnit;
import com.batyanko.strokeratecoach.workout.WorkoutValidationException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class WorkoutDBHelperMigrationTest {
    private static final String DATABASE_NAME = "presets.db";
    private static final int LEGACY_VERSION_1 = 1;
    private static final int LEGACY_VERSION_2 = 2;

    private Context context;
    private WorkoutDBHelper helper;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        context.deleteDatabase(DATABASE_NAME);
    }

    @After
    public void tearDown() {
        if (helper != null) {
            helper.close();
            helper = null;
        }
        context.deleteDatabase(DATABASE_NAME);
    }

    @Test
    public void freshDatabaseCreatesTreeColumnsAndCanonicalFactoryPresets() {
        SQLiteDatabase database = openFreshDatabase();

        assertTrue(hasColumn(database, TABLE_NAME_PRESETS, COLUMN_SERIALIZED_TREE));
        assertTrue(hasColumn(database, TABLE_NAME_HISTORY, COLUMN_SERIALIZED_TREE));
        assertTrue(hasColumn(database, TABLE_NAME_TRASH, COLUMN_SERIALIZED_TREE));
        assertEquals(3, database.getVersion());
        assertEquals(3, countRows(database, TABLE_NAME_PRESETS));

        try (Cursor cursor = database.query(TABLE_NAME_PRESETS,
                new String[]{COLUMN_SPP_CSV, COLUMN_GEARS_CSV, COLUMN_SPP_TYPE,
                        COLUMN_SERIALIZED_TREE},
                null, null, null, null, null)) {
            WorkoutRecordMapper mapper = newMapper();
            while (cursor.moveToNext()) {
                assertNotNull(cursor.getString(
                        cursor.getColumnIndexOrThrow(COLUMN_SERIALIZED_TREE)));
                CompiledWorkout workout = mapper.readAndCompileDefinition(cursor);
                assertTrue(workout.getTotalPhases() > 0);
            }
        }
    }

    @Test
    public void v2UpgradeBackfillsValidRowsPreservesLegacyColumnsAndCopiesTreeToTrash() {
        SQLiteDatabase legacy = createLegacyDatabase(LEGACY_VERSION_2);
        insertLegacyRow(legacy, TABLE_NAME_PRESETS, 7, "Preset", "Description",
                "2018-01-02 03:04:05", "2", "60,30", "22,24");
        insertLegacyRow(legacy, TABLE_NAME_PRESETS, 8, "Malformed", "Keep",
                "2018-01-03 03:04:05", "0", "10,,20", "22,23,24");
        insertLegacyRow(legacy, TABLE_NAME_HISTORY, 11, "History", "Used",
                "2019-02-03 04:05:06", "1", "50,100", "22,24");
        insertLegacyRow(legacy, TABLE_NAME_TRASH, 15, "Trash", "Removed",
                "2020-03-04 05:06:07", "0", "10", "20");
        legacy.close();

        SQLiteDatabase database = openLegacyDatabase();
        assertEquals(3, database.getVersion());
        assertEquals(2, countRows(database, TABLE_NAME_PRESETS));
        assertEquals(1, countRows(database, TABLE_NAME_HISTORY));
        assertEquals(1, countRows(database, TABLE_NAME_TRASH));

        assertLegacyRow(database, TABLE_NAME_PRESETS, 7, "Preset", "Description",
                "2018-01-02 03:04:05", "2", "60,30", "22,24");
        assertLegacyRow(database, TABLE_NAME_PRESETS, 8, "Malformed", "Keep",
                "2018-01-03 03:04:05", "0", "10,,20", "22,23,24");
        assertLegacyRow(database, TABLE_NAME_HISTORY, 11, "History", "Used",
                "2019-02-03 04:05:06", "1", "50,100", "22,24");
        assertLegacyRow(database, TABLE_NAME_TRASH, 15, "Trash", "Removed",
                "2020-03-04 05:06:07", "0", "10", "20");

        assertNull(serializedTree(database, TABLE_NAME_PRESETS, 8));
        assertDefinition(database, TABLE_NAME_PRESETS, 7, WorkoutUnit.SECONDS,
                new int[]{60, 30}, new int[]{22, 24});
        assertDefinition(database, TABLE_NAME_HISTORY, 11, WorkoutUnit.METERS,
                new int[]{50, 100}, new int[]{22, 24});
        assertDefinition(database, TABLE_NAME_TRASH, 15, WorkoutUnit.STROKES,
                new int[]{10}, new int[]{20});

        database.execSQL(WorkoutDBHelper.SQL_MOVE_HISTORY_TO_TRASH);
        assertEquals(2, countRows(database, TABLE_NAME_TRASH));
        assertNotNull(serializedTreeByName(database, TABLE_NAME_TRASH, "History"));
    }

    @Test
    public void v1UpgradeCreatesVersion2TrashThenAddsTreeColumnOnce() {
        SQLiteDatabase legacy = createLegacyDatabase(LEGACY_VERSION_1);
        insertLegacyRow(legacy, TABLE_NAME_PRESETS, 3, "Old preset", "Description",
                "2017-01-01 01:02:03", "0", "12,24", "18,20");
        insertLegacyRow(legacy, TABLE_NAME_HISTORY, 4, "Old history", "Used",
                "2017-02-01 01:02:03", "1", "25", "21");
        legacy.close();

        SQLiteDatabase database = openLegacyDatabase();
        assertEquals(3, database.getVersion());
        assertTrue(hasTable(database, TABLE_NAME_TRASH));
        assertTrue(hasColumn(database, TABLE_NAME_TRASH, COLUMN_SERIALIZED_TREE));
        assertEquals(0, countRows(database, TABLE_NAME_TRASH));
        assertDefinition(database, TABLE_NAME_PRESETS, 3, WorkoutUnit.STROKES,
                new int[]{12, 24}, new int[]{18, 20});
        assertDefinition(database, TABLE_NAME_HISTORY, 4, WorkoutUnit.METERS,
                new int[]{25}, new int[]{21});
    }

    @Test
    public void mapperWritesCanonicalTreeAndExpandedCsvProjection() {
        SQLiteDatabase database = openFreshDatabase();
        WorkoutDefinition definition = new WorkoutDefinition(WorkoutUnit.SECONDS,
                Collections.singletonList(new RepeatNode(
                        2, "Set", Arrays.<WorkoutNode>asList(
                        new PhaseNode(60, 22),
                        new PhaseNode(30, 24)))));
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, "Nested");
        newMapper().putDefinition(values, definition);
        long id = database.insertOrThrow(TABLE_NAME_PRESETS, null, values);

        try (Cursor cursor = database.query(TABLE_NAME_PRESETS, null,
                _ID + " = ?", new String[]{Long.toString(id)}, null, null, null)) {
            assertTrue(cursor.moveToFirst());
            assertEquals("2", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SPP_TYPE)));
            assertEquals("60,30,60,30",
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SPP_CSV)));
            assertEquals("22,24,22,24",
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GEARS_CSV)));
            CompiledWorkout compiled = newMapper().readAndCompileDefinition(cursor);
            assertEquals(4, compiled.getTotalPhases());
            assertEquals(180, compiled.getTotalLength());
            assertEquals("Set 2/2", compiled.getPhases().get(2).getBreadcrumb());
        }
    }

    @Test
    public void mapperDoesNotUseLegacyFallbackWhenNonEmptyTreeIsInvalid() {
        SQLiteDatabase database = openFreshDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SPP_TYPE, "0");
        values.put(COLUMN_SPP_CSV, "10");
        values.put(COLUMN_GEARS_CSV, "22");
        values.put(COLUMN_SERIALIZED_TREE, "{\"schemaVersion\":2,\"nodes\":[]}");
        long id = database.insertOrThrow(TABLE_NAME_PRESETS, null, values);

        try (Cursor cursor = database.query(TABLE_NAME_PRESETS, null,
                _ID + " = ?", new String[]{Long.toString(id)}, null, null, null)) {
            assertTrue(cursor.moveToFirst());
            try {
                newMapper().readDefinition(cursor);
                throw new AssertionError("Invalid tree should not fall back to CSV");
            } catch (WorkoutValidationException exception) {
                assertEquals(WorkoutValidationException.Code.UNSUPPORTED_SCHEMA_VERSION,
                        exception.getCode());
            }
        }
    }

    @Test
    public void mapperUsesLegacyCsvWhenTreeIsNullOrEmpty() {
        SQLiteDatabase database = openFreshDatabase();
        insertLegacyRow(database, TABLE_NAME_PRESETS, 60, "Legacy", "CSV",
                "2021-04-05 06:07:08", "0", "10,20", "22,24");
        ContentValues emptyTree = new ContentValues();
        emptyTree.put(COLUMN_SERIALIZED_TREE, "");
        database.update(TABLE_NAME_PRESETS, emptyTree, _ID + " = ?",
                new String[]{"60"});

        try (Cursor cursor = database.query(TABLE_NAME_PRESETS, null,
                _ID + " = ?", new String[]{"60"}, null, null, null)) {
            assertTrue(cursor.moveToFirst());
            WorkoutDefinition definition = newMapper().readDefinition(cursor);
            assertEquals(WorkoutUnit.STROKES, definition.getUnit());
            assertEquals(2, definition.getNodes().size());
        }
    }

    private SQLiteDatabase openFreshDatabase() {
        helper = new WorkoutDBHelper(context);
        return helper.getWritableDatabase();
    }

    private SQLiteDatabase openLegacyDatabase() {
        helper = new WorkoutDBHelper(context);
        return helper.getWritableDatabase();
    }

    private SQLiteDatabase createLegacyDatabase(int version) {
        SQLiteDatabase database = context.openOrCreateDatabase(
                DATABASE_NAME, Context.MODE_PRIVATE, null);
        database.execSQL(createLegacyTableSql(TABLE_NAME_PRESETS));
        database.execSQL(createLegacyTableSql(TABLE_NAME_HISTORY));
        if (version >= LEGACY_VERSION_2) {
            database.execSQL(createLegacyTableSql(TABLE_NAME_TRASH));
        }
        database.setVersion(version);
        return database;
    }

    private String createLegacyTableSql(String tableName) {
        String defaultName = TABLE_NAME_TRASH.equals(tableName) ? "NO TITLE" : "Untitled";
        String defaultDescription = TABLE_NAME_TRASH.equals(tableName)
                ? "NO DESC" : "No description";
        return "CREATE TABLE " + tableName + " (" +
                _ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_NAME + " TEXT DEFAULT '" + defaultName + "', " +
                COLUMN_DESC + " TEXT DEFAULT '" + defaultDescription + "', " +
                COLUMN_TIMESTAMP + " INTEGER DEFAULT CURRENT_TIMESTAMP, " +
                COLUMN_SPP_TYPE + " INTEGER DEFAULT 0, " +
                COLUMN_SPP_CSV + " TEXT NOT NULL, " +
                COLUMN_GEARS_CSV + " TEXT NOT NULL" +
                ");";
    }

    private void insertLegacyRow(SQLiteDatabase database, String table, long id, String name,
                                 String description, String timestamp, String sppType,
                                 String spp, String gears) {
        ContentValues values = new ContentValues();
        values.put(_ID, id);
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_DESC, description);
        values.put(COLUMN_TIMESTAMP, timestamp);
        values.put(COLUMN_SPP_TYPE, sppType);
        values.put(COLUMN_SPP_CSV, spp);
        values.put(COLUMN_GEARS_CSV, gears);
        database.insertOrThrow(table, null, values);
    }

    private void assertLegacyRow(SQLiteDatabase database, String table, long id, String name,
                                 String description, String timestamp, String sppType,
                                 String spp, String gears) {
        try (Cursor cursor = database.query(table, null, _ID + " = ?",
                new String[]{Long.toString(id)}, null, null, null)) {
            assertTrue(cursor.moveToFirst());
            assertEquals(name, cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)));
            assertEquals(description,
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESC)));
            assertEquals(timestamp,
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)));
            assertEquals(sppType,
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SPP_TYPE)));
            assertEquals(spp, cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SPP_CSV)));
            assertEquals(gears,
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GEARS_CSV)));
        }
    }

    private void assertDefinition(SQLiteDatabase database, String table, long id,
                                  WorkoutUnit unit, int[] lengths, int[] targets) {
        try (Cursor cursor = database.query(table, null, _ID + " = ?",
                new String[]{Long.toString(id)}, null, null, null)) {
            assertTrue(cursor.moveToFirst());
            CompiledWorkout workout = newMapper().readAndCompileDefinition(cursor);
            assertEquals(unit, workout.getUnit());
            assertEquals(lengths.length, workout.getTotalPhases());
            for (int i = 0; i < lengths.length; i++) {
                assertEquals(lengths[i], workout.getPhases().get(i).getLength());
                assertEquals(targets[i], workout.getPhases().get(i).getTargetSpm());
            }
        }
    }

    private String serializedTree(SQLiteDatabase database, String table, long id) {
        try (Cursor cursor = database.query(table, new String[]{COLUMN_SERIALIZED_TREE},
                _ID + " = ?", new String[]{Long.toString(id)}, null, null, null)) {
            assertTrue(cursor.moveToFirst());
            return cursor.getString(0);
        }
    }

    private String serializedTreeByName(SQLiteDatabase database, String table, String name) {
        try (Cursor cursor = database.query(table, new String[]{COLUMN_SERIALIZED_TREE},
                COLUMN_NAME + " = ?", new String[]{name}, null, null, null)) {
            assertTrue(cursor.moveToFirst());
            return cursor.getString(0);
        }
    }

    private boolean hasColumn(SQLiteDatabase database, String table, String column) {
        try (Cursor cursor = database.rawQuery("PRAGMA table_info(" + table + ")", null)) {
            int nameIndex = cursor.getColumnIndexOrThrow("name");
            while (cursor.moveToNext()) {
                if (column.equals(cursor.getString(nameIndex))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasTable(SQLiteDatabase database, String table) {
        try (Cursor cursor = database.rawQuery(
                "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?",
                new String[]{table})) {
            return cursor.moveToFirst();
        }
    }

    private int countRows(SQLiteDatabase database, String table) {
        try (Cursor cursor = database.rawQuery("SELECT COUNT(*) FROM " + table, null)) {
            assertTrue(cursor.moveToFirst());
            return cursor.getInt(0);
        }
    }

    private WorkoutRecordMapper newMapper() {
        return new WorkoutRecordMapper(
                new WorkoutJsonCodec(), new LegacyWorkoutParser(), new WorkoutCompiler());
    }
}
