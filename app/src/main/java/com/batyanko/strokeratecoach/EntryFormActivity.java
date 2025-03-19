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

package com.batyanko.strokeratecoach;

import static android.text.TextUtils.join;
import static com.batyanko.strokeratecoach.WaveActivity.THEME_COLOR;
import static com.batyanko.strokeratecoach.sync.BeeperTasks.EXTRA_WORKOUT_ID;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.batyanko.strokeratecoach.data.WorkoutContract;
import com.batyanko.strokeratecoach.data.WorkoutDBHelper;
import com.batyanko.strokeratecoach.sync.BeeperTasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EntryFormActivity extends AppCompatActivity {

    private EditText nameEt;
    private EditText descEt;
    Button addPhaseButton;
    Button createWorkoutButton;
    private static int numberOfLines;
    private static boolean editMode;

    RadioGroup rGroup;
    RadioButton radioStrokes;
    RadioButton radioMeters;
    RadioButton radioSeconds;

    LinearLayout ll;
    LinearLayout ld;
    LinearLayout lb;

    private SQLiteDatabase mDb;

    List<EditText> sppEditTexts;
    List<EditText> gearEditTexts;

    private SharedPreferences pref;
    private String workoutId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry_form);

        pref = PreferenceManager.getDefaultSharedPreferences(this);

        this.getWindow().getDecorView().setBackgroundColor(
                pref.getInt(THEME_COLOR, getResources().getColor(R.color.backgroundLight)));
        //TODO implement back button?
        numberOfLines = 0;
        sppEditTexts = new ArrayList<>();
        gearEditTexts = new ArrayList<>();

        WorkoutDBHelper presetDbHelper = new WorkoutDBHelper(this);
        mDb = presetDbHelper.getWritableDatabase();

        rGroup = findViewById(R.id.create_workout_radio_group);
        radioStrokes = findViewById(R.id.radio_strokes);
        radioMeters = findViewById(R.id.radio_meters);
        radioSeconds = findViewById(R.id.radio_seconds);

        nameEt = findViewById(R.id.workout_name_et);
        descEt = findViewById(R.id.workout_desc_et);

        Intent intent = getIntent();

        workoutId = intent.getStringExtra(EXTRA_WORKOUT_ID);

        String sppCSV;
        String gearCSV;

        // In case we are editing...
        if (workoutId != null && !workoutId.isEmpty()) {
            editMode = true;
            Cursor cursor = getPreset(mDb, workoutId);
            if (cursor.getCount() <= 0) {
                // TODO Bail
            }

            cursor.moveToFirst();

            int cIdxName = cursor.getColumnIndex(WorkoutContract.WorkoutEntry1.COLUMN_NAME);
            String workoutName = cursor.getString(cIdxName);
            nameEt.setText(workoutName);

            int cIdxDesc = cursor.getColumnIndex(WorkoutContract.WorkoutEntry1.COLUMN_DESC);
            String workoutDesc = cursor.getString(cIdxDesc);
            descEt.setText(workoutDesc);

            int cIdxType = cursor.getColumnIndex(WorkoutContract.WorkoutEntry1.COLUMN_SPP_TYPE);
            String workoutType = cursor.getString(cIdxType);
            switch (workoutType) {
                case BeeperTasks.SPP_TYPE_STROKES:
                    radioStrokes.toggle();
                    break;
                case BeeperTasks.SPP_TYPE_METERS:
                    radioMeters.toggle();
                    break;
                case BeeperTasks.SPP_TYPE_SECONDS:
                    radioSeconds.toggle();
                    break;
                default:
                    //TODO bail
            }

            int cIdxSpp = cursor.getColumnIndex(WorkoutContract.WorkoutEntry1.COLUMN_SPP_CSV);
            sppCSV = cursor.getString(cIdxSpp);

            int cIdxGears = cursor.getColumnIndex(WorkoutContract.WorkoutEntry1.COLUMN_GEARS_CSV);
            gearCSV = cursor.getString(cIdxGears);

            final String[] gears = gearCSV.split("\\s*,\\s*");
            final String[] spp = sppCSV.split("\\s*,\\s*");

            for (int i = 0; i < gears.length; i++) {
                addLine(spp[i], gears[i]);
            }
            cursor.close();
            return;
        }
        //...or else start with blank form.
        addLine("", "");
    }

    private void addLine(String spp, String gear) {
        numberOfLines++;

        LinearLayout.LayoutParams pHorizontal = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        LinearLayout.LayoutParams pHorizontalWrap = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        LinearLayout.LayoutParams pVertical = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pHorizontal.width = 300;

        // Remove buttons to make way for added line. Buttons are regenerated after each new line.
        if (ll != null && lb != null) {
            ll.removeView(lb);
        }

        ll = (LinearLayout) findViewById(R.id.entry_layout);
        ld = new LinearLayout(this);
        ld.setLayoutParams(pVertical);
        lb = new LinearLayout(this);
        lb.setLayoutParams(pVertical);

        ll.addView(ld);

        String SPPTitle = (" " + numberOfLines + ": ");
        TextView SPPTitleView = new TextView(this);
        SPPTitleView.setLayoutParams(pHorizontalWrap);
        SPPTitleView.setText(SPPTitle);
        ld.addView(SPPTitleView);

        EditText etSpp = new EditText(this);
        EditText etGears = new EditText(this);
        if (!"".equals(spp) && !"".equals(gear)) {
            etSpp.setText(spp);
            etGears.setText((gear));
        }

        sppEditTexts.add(etSpp);
        sppEditTexts.get(sppEditTexts.size() - 1).setLayoutParams(pHorizontal);
        sppEditTexts.get(sppEditTexts.size() - 1).setRawInputType(2);
        ld.addView(sppEditTexts.get(sppEditTexts.size() - 1));

        String gearTitle = (" " + getString(R.string.gear_title_text) + " ");
        TextView gearTitleView = new TextView(this);
        gearTitleView.setLayoutParams(pHorizontalWrap);
        gearTitleView.setText(gearTitle);
        ld.addView(gearTitleView);

        gearEditTexts.add(etGears);
        gearEditTexts.get(gearEditTexts.size() - 1).setLayoutParams(pHorizontal);
        gearEditTexts.get(gearEditTexts.size() - 1).setRawInputType(2);
        ld.addView(gearEditTexts.get(gearEditTexts.size() - 1));

        addPhaseButton = new Button(this);
        addPhaseButton.setLayoutParams(pHorizontal);
        addPhaseButton.setBackgroundResource(R.drawable.ic_rectangle);
        addPhaseButton.setTextColor(getResources().getColor(android.R.color.white));
        addPhaseButton.setOnClickListener(view -> addLine("", ""));
        addPhaseButton.setText(R.string.button_add_phase_label);

        createWorkoutButton = new Button(this);
        createWorkoutButton.setLayoutParams(pVertical);
        createWorkoutButton.setBackgroundResource(R.drawable.ic_rectangle);
        createWorkoutButton.setTextColor(getResources().getColor(android.R.color.white));
        if (editMode) {
            createWorkoutButton.setOnClickListener(EditButtonListener);
            createWorkoutButton.setText(R.string.edit_workout_button_label);
        } else {
            createWorkoutButton.setOnClickListener(CreateButtonListener);
            createWorkoutButton.setText(R.string.create_workout_button_label);
        }

        ll.addView(lb);
        lb.addView(addPhaseButton);
        lb.addView(createWorkoutButton);
    }

    private View.OnClickListener CreateButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            String sppType = getSPPFromRadio();

            //Don't create training if no SPP type selected
            if (sppType.equals("9")) {
                Toast.makeText(EntryFormActivity.this, R.string.missing_workout_type_warning, Toast.LENGTH_LONG).show();
                return;
            }

            nameEt = EntryFormActivity.this.findViewById(R.id.workout_name_et);
            String name = nameEt.getText().toString();
            descEt = EntryFormActivity.this.findViewById(R.id.workout_desc_et);
            String desc = descEt.getText().toString();

            String[] spp = new String[numberOfLines];
            String[] gears = new String[numberOfLines];
            int j = 0;
            boolean emptyLinesExist = false;

            for (int i = 1; i <= numberOfLines; i++) {

                String sppString = sppEditTexts.get(i - 1).getText().toString();
                String gearString = gearEditTexts.get(i - 1).getText().toString();

                //Skip phases with empty data
                if (sppString.length() == 0 || gearString.length() == 0) {
                    emptyLinesExist = true;
                    continue;
                }

                j++;

                spp[j - 1] = sppString;
                gears[j - 1] = gearString;
            }

            String emptyPhasesPresent = "";
            //Check for empty lines or an empty workout
            if (j == 0) {
                Toast.makeText(
                        EntryFormActivity.this,
                        R.string.empty_workout_warning,
                        Toast.LENGTH_LONG).show();
                return;
            } else if (emptyLinesExist) {

                emptyPhasesPresent = getResources().getString(R.string.empty_data_notification) + " ";
            }

            String[] sppResized = Arrays.copyOfRange(spp, 0, j);
            String[] gearsResized = Arrays.copyOfRange(gears, 0, j);

            long i = addPreset(
                    mDb,
                    name,
                    desc,
                    join(",", sppResized),
                    join(",", gearsResized),
                    sppType);
            Toast.makeText(EntryFormActivity.this,
                    emptyPhasesPresent + getString(R.string.preset_added_notification),
                    Toast.LENGTH_LONG).show();
            finish();
        }
    };
    private View.OnClickListener EditButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            String sppType = getSPPFromRadio();

            //Don't create training if no SPP type selected
            if (sppType.equals("9")) {
                Toast.makeText(EntryFormActivity.this, R.string.missing_workout_type_warning, Toast.LENGTH_LONG).show();
                return;
            }

            nameEt = EntryFormActivity.this.findViewById(R.id.workout_name_et);
            String name = nameEt.getText().toString();
            descEt = EntryFormActivity.this.findViewById(R.id.workout_desc_et);
            String desc = descEt.getText().toString();

            String[] spp = new String[numberOfLines];
            String[] gears = new String[numberOfLines];
            int j = 0;
            boolean emptyLinesExist = false;

            for (int i = 1; i <= numberOfLines; i++) {

                String sppString = sppEditTexts.get(i - 1).getText().toString();
                String gearString = gearEditTexts.get(i - 1).getText().toString();

                //Skip phases with empty data
                if (sppString.length() == 0 || gearString.length() == 0) {
                    emptyLinesExist = true;
                    continue;
                }

                j++;

                spp[j - 1] = sppString;
                gears[j - 1] = gearString;
            }

            String emptyPhasesPresent = "";
            //Check for empty lines or an empty workout
            if (j == 0) {
                Toast.makeText(
                        EntryFormActivity.this,
                        R.string.empty_workout_warning,
                        Toast.LENGTH_LONG).show();
                return;
            } else if (emptyLinesExist) {

                emptyPhasesPresent = getResources().getString(R.string.empty_data_notification) + " ";
            }

            String[] sppResized = Arrays.copyOfRange(spp, 0, j);
            String[] gearsResized = Arrays.copyOfRange(gears, 0, j);

            long i = updatePreset(
                    mDb,
                    workoutId,
                    name,
                    desc,
                    join(",", sppResized),
                    join(",", gearsResized),
                    sppType);
            Toast.makeText(EntryFormActivity.this,
                    emptyPhasesPresent + getString(R.string.preset_added_notification),
                    Toast.LENGTH_LONG).show();

            finish();
        }
    };

    // TODO use same SQLite methods from all activities?
    private long addPreset(SQLiteDatabase db, String name, String description,
                           String spp, String gears, String sppType) {
        ContentValues cv = new ContentValues();
        cv.put(WorkoutContract.WorkoutEntry1.COLUMN_NAME, name);
        cv.put(WorkoutContract.WorkoutEntry1.COLUMN_DESC, description);
        cv.put(WorkoutContract.WorkoutEntry1.COLUMN_SPP_CSV, spp);
        cv.put(WorkoutContract.WorkoutEntry1.COLUMN_GEARS_CSV, gears);
        cv.put(WorkoutContract.WorkoutEntry1.COLUMN_SPP_TYPE, sppType);
        return db.insert(WorkoutContract.WorkoutEntry1.TABLE_NAME_PRESETS, null, cv);
    }

    private long updatePreset(SQLiteDatabase db, String workoutId, String name, String description,
                              String spp, String gears, String sppType) {
        ContentValues cv = new ContentValues();
        cv.put(WorkoutContract.WorkoutEntry1.COLUMN_NAME, name);
        cv.put(WorkoutContract.WorkoutEntry1.COLUMN_DESC, description);
        cv.put(WorkoutContract.WorkoutEntry1.COLUMN_SPP_CSV, spp);
        cv.put(WorkoutContract.WorkoutEntry1.COLUMN_GEARS_CSV, gears);
        cv.put(WorkoutContract.WorkoutEntry1.COLUMN_SPP_TYPE, sppType);
        return db.update(WorkoutContract.WorkoutEntry1.TABLE_NAME_PRESETS,
                cv,
                WorkoutContract.WorkoutEntry1._ID + " = " + workoutId + ";",
                null);
    }

    private Cursor getPreset(SQLiteDatabase db, String workoutId) {
        return db.query(WorkoutContract.WorkoutEntry1.TABLE_NAME_PRESETS,
                null,
                WorkoutContract.WorkoutEntry1._ID + " = " + workoutId + ";",
                null,
                null,
                null,
                WorkoutContract.WorkoutEntry1.COLUMN_TIMESTAMP);
    }

    private String getSPPFromRadio() {
        if (radioStrokes.isChecked()) {
            return BeeperTasks.SPP_TYPE_STROKES;
        } else if (radioMeters.isChecked()) {
            return BeeperTasks.SPP_TYPE_METERS;
        } else if (radioSeconds.isChecked()) {
            return BeeperTasks.SPP_TYPE_SECONDS;
        } else return "9";
    }
}
