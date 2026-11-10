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
import com.batyanko.strokeratecoach.data.WorkoutRecordMapper;
import com.batyanko.strokeratecoach.sync.BeeperTasks;
import com.batyanko.strokeratecoach.workout.CompiledPhase;
import com.batyanko.strokeratecoach.workout.CompiledWorkout;
import com.batyanko.strokeratecoach.workout.LegacyWorkoutParser;
import com.batyanko.strokeratecoach.workout.PhaseNode;
import com.batyanko.strokeratecoach.workout.WorkoutCompiler;
import com.batyanko.strokeratecoach.workout.WorkoutDefinition;
import com.batyanko.strokeratecoach.workout.WorkoutJsonCodec;
import com.batyanko.strokeratecoach.workout.WorkoutNode;
import com.batyanko.strokeratecoach.workout.WorkoutUnit;
import com.batyanko.strokeratecoach.workout.WorkoutValidationException;

import java.util.ArrayList;
import java.util.List;

public class EntryFormActivity extends AppCompatActivity {

    private EditText nameEt;
    private EditText descEt;
    Button addPhaseButton;
    Button createWorkoutButton;
    private int numberOfLines;
    private boolean editMode;

    RadioGroup rGroup;
    RadioButton radioStrokes;
    RadioButton radioMeters;
    RadioButton radioSeconds;

    LinearLayout ll;
    LinearLayout ld;
    LinearLayout lb;

    private SQLiteDatabase mDb;
    private WorkoutRecordMapper recordMapper;

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
        recordMapper = new WorkoutRecordMapper(
                new WorkoutJsonCodec(),
                new LegacyWorkoutParser(),
                new WorkoutCompiler());

        rGroup = findViewById(R.id.create_workout_radio_group);
        radioStrokes = findViewById(R.id.radio_strokes);
        radioMeters = findViewById(R.id.radio_meters);
        radioSeconds = findViewById(R.id.radio_seconds);

        nameEt = findViewById(R.id.workout_name_et);
        descEt = findViewById(R.id.workout_desc_et);

        Intent intent = getIntent();

        workoutId = intent.getStringExtra(EXTRA_WORKOUT_ID);

        // In case we are editing...
        if (workoutId != null && !workoutId.isEmpty()) {
            editMode = true;
            loadPresetForEditing(workoutId);
            return;
        }
        //...or else start with blank form.
        addLine("", "");
    }

    private void loadPresetForEditing(String id) {
        try (Cursor cursor = getPreset(mDb, id)) {
            if (!cursor.moveToFirst()) {
                Toast.makeText(this, R.string.preset_add_failed, Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            nameEt.setText(cursor.getString(cursor.getColumnIndexOrThrow(
                    WorkoutContract.WorkoutEntry1.COLUMN_NAME)));
            descEt.setText(cursor.getString(cursor.getColumnIndexOrThrow(
                    WorkoutContract.WorkoutEntry1.COLUMN_DESC)));

            WorkoutDefinition definition = recordMapper.readDefinition(cursor);
            for (WorkoutNode node : definition.getNodes()) {
                if (!(node instanceof PhaseNode)) {
                    Toast.makeText(this, R.string.nested_workout_edit_not_supported,
                            Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
            }
            selectUnit(definition.getUnit());
            CompiledWorkout compiled = new WorkoutCompiler().compile(definition);
            for (CompiledPhase phase : compiled.getPhases()) {
                addLine(Integer.toString(phase.getLength()),
                        Integer.toString(phase.getTargetSpm()));
            }
        } catch (WorkoutValidationException exception) {
            Toast.makeText(this, exception.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void selectUnit(WorkoutUnit unit) {
        switch (unit) {
            case STROKES:
                rGroup.check(radioStrokes.getId());
                return;
            case METERS:
                rGroup.check(radioMeters.getId());
                return;
            case SECONDS:
                rGroup.check(radioSeconds.getId());
                return;
            default:
                throw new IllegalStateException("Unsupported workout unit: " + unit);
        }
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

    private final View.OnClickListener CreateButtonListener = view -> saveWorkout(false);
    private final View.OnClickListener EditButtonListener = view -> saveWorkout(true);

    private void saveWorkout(boolean editing) {
        String sppType = getSPPFromRadio();
        if ("9".equals(sppType)) {
            Toast.makeText(this, R.string.missing_workout_type_warning, Toast.LENGTH_LONG).show();
            return;
        }

        if (!hasCompletePhase()) {
            Toast.makeText(this, R.string.empty_workout_warning, Toast.LENGTH_LONG).show();
            return;
        }
        boolean incompletePhasesExist = hasIncompletePhase();

        ContentValues values = new ContentValues();
        try {
            WorkoutDefinition definition = readDefinitionFromForm(sppType);
            recordMapper.putDefinition(values, definition);
        } catch (WorkoutValidationException exception) {
            Toast.makeText(this, exception.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        values.put(WorkoutContract.WorkoutEntry1.COLUMN_NAME, nameEt.getText().toString());
        values.put(WorkoutContract.WorkoutEntry1.COLUMN_DESC, descEt.getText().toString());

        long result;
        if (editing) {
            result = mDb.update(WorkoutContract.WorkoutEntry1.TABLE_NAME_PRESETS,
                    values,
                    WorkoutContract.WorkoutEntry1._ID + " = ?",
                    new String[]{workoutId});
            if (result > 0) {
                updateTimestamp(mDb, workoutId);
            }
        } else {
            result = mDb.insert(WorkoutContract.WorkoutEntry1.TABLE_NAME_PRESETS, null, values);
        }

        if (result > 0) {
            String message = incompletePhasesExist
                    ? getString(R.string.empty_data_notification) + " "
                    : "";
            Toast.makeText(this, message + getString(R.string.preset_added),
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, R.string.preset_add_failed, Toast.LENGTH_LONG).show();
            return;
        }
        finish();
    }

    private WorkoutDefinition readDefinitionFromForm(String sppType) {
        StringBuilder lengths = new StringBuilder();
        StringBuilder targets = new StringBuilder();
        for (int i = 0; i < numberOfLines; i++) {
            String length = sppEditTexts.get(i).getText().toString().trim();
            String target = gearEditTexts.get(i).getText().toString().trim();
            if (length.isEmpty() || target.isEmpty()) {
                continue;
            }
            appendCsv(lengths, length);
            appendCsv(targets, target);
        }
        return new LegacyWorkoutParser().parse(
                lengths.toString(), targets.toString(), sppType);
    }

    private boolean hasCompletePhase() {
        for (int i = 0; i < numberOfLines; i++) {
            if (!sppEditTexts.get(i).getText().toString().trim().isEmpty()
                    && !gearEditTexts.get(i).getText().toString().trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasIncompletePhase() {
        for (int i = 0; i < numberOfLines; i++) {
            if (sppEditTexts.get(i).getText().toString().trim().isEmpty()
                    || gearEditTexts.get(i).getText().toString().trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void appendCsv(StringBuilder values, String value) {
        if (values.length() > 0) {
            values.append(',');
        }
        values.append(value);
    }

    private void updateTimestamp(SQLiteDatabase db, String wId) {
        db.execSQL("UPDATE " + WorkoutContract.WorkoutEntry1.TABLE_NAME_PRESETS +
                " SET " + WorkoutContract.WorkoutEntry1.COLUMN_TIMESTAMP + " = CURRENT_TIMESTAMP" +
                " WHERE " + WorkoutContract.WorkoutEntry1._ID + " = ?",
                new Object[]{wId});
    }

    private Cursor getPreset(SQLiteDatabase db, String workoutId) {
        return db.query(WorkoutContract.WorkoutEntry1.TABLE_NAME_PRESETS,
                null,
        WorkoutContract.WorkoutEntry1._ID + " = ?",
        new String[]{workoutId},
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
