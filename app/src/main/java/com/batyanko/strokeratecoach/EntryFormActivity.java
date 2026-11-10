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
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.batyanko.strokeratecoach.data.WorkoutContract;
import com.batyanko.strokeratecoach.data.WorkoutDBHelper;
import com.batyanko.strokeratecoach.data.WorkoutRecordMapper;
import com.batyanko.strokeratecoach.workout.LegacyWorkoutParser;
import com.batyanko.strokeratecoach.workout.WorkoutCompiler;
import com.batyanko.strokeratecoach.workout.WorkoutDefinition;
import com.batyanko.strokeratecoach.workout.WorkoutEditorDraft;
import com.batyanko.strokeratecoach.workout.WorkoutJsonCodec;
import com.batyanko.strokeratecoach.workout.WorkoutNode;
import com.batyanko.strokeratecoach.workout.WorkoutUnit;
import com.batyanko.strokeratecoach.workout.WorkoutValidationException;

import java.util.ArrayList;
import java.util.List;

public class EntryFormActivity extends AppCompatActivity {
    private static final String STATE_DRAFT = "editor-draft";
    private static final String STATE_SELECTED_UNIT = "editor-selected-unit";

    private EditText nameEt;
    private EditText descEt;
    private RadioGroup unitRadioGroup;
    private RadioButton radioStrokes;
    private RadioButton radioMeters;
    private RadioButton radioSeconds;
    private LinearLayout nodeList;
    private TextView pathLabel;
    private Button upButton;
    private Button addPhaseButton;
    private Button addRepeatButton;
    private Button saveButton;

    private SQLiteDatabase database;
    private WorkoutRecordMapper recordMapper;
    private WorkoutEditorDraft draft;
    private String workoutId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry_form);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        getWindow().getDecorView().setBackgroundColor(
                preferences.getInt(THEME_COLOR, getResources().getColor(R.color.backgroundLight)));

        WorkoutDBHelper databaseHelper = new WorkoutDBHelper(this);
        database = databaseHelper.getWritableDatabase();
        recordMapper = new WorkoutRecordMapper(
                new WorkoutJsonCodec(), new LegacyWorkoutParser(), new WorkoutCompiler());

        unitRadioGroup = findViewById(R.id.create_workout_radio_group);
        radioStrokes = findViewById(R.id.radio_strokes);
        radioMeters = findViewById(R.id.radio_meters);
        radioSeconds = findViewById(R.id.radio_seconds);
        nameEt = findViewById(R.id.workout_name_et);
        descEt = findViewById(R.id.workout_desc_et);
        nodeList = findViewById(R.id.workout_node_list);
        pathLabel = findViewById(R.id.editor_path);
        upButton = findViewById(R.id.editor_up_button);
        addPhaseButton = findViewById(R.id.button_add_phase);
        addRepeatButton = findViewById(R.id.button_add_repeat);
        saveButton = findViewById(R.id.button_save_workout);

        upButton.setOnClickListener(view -> {
            draft.goUp();
            renderDraft();
        });
        addPhaseButton.setOnClickListener(view -> showPhaseDialog(-1));
        addRepeatButton.setOnClickListener(view -> showRepeatDialog(-1));
        saveButton.setOnClickListener(view -> saveWorkout());

        workoutId = getIntent().getStringExtra(EXTRA_WORKOUT_ID);
        if (savedInstanceState != null) {
            restoreDraft(savedInstanceState);
            return;
        }
        if (workoutId != null && !workoutId.isEmpty()) {
            loadPresetForEditing(workoutId);
            return;
        }
        draft = new WorkoutEditorDraft();
        renderDraft();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (draft != null) {
            outState.putString(STATE_DRAFT, draft.serialize());
        }
        outState.putInt(STATE_SELECTED_UNIT, unitRadioGroup.getCheckedRadioButtonId());
        super.onSaveInstanceState(outState);
    }

    private void restoreDraft(Bundle savedInstanceState) {
        String serializedDraft = savedInstanceState.getString(STATE_DRAFT);
        if (serializedDraft == null) {
            Toast.makeText(this, R.string.editor_state_restore_failed, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        try {
            draft = WorkoutEditorDraft.deserialize(serializedDraft);
        } catch (IllegalArgumentException exception) {
            Toast.makeText(this, exception.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        int selectedUnitId = savedInstanceState.getInt(STATE_SELECTED_UNIT,
                RadioGroup.NO_ID);
        if (selectedUnitId != RadioGroup.NO_ID) {
            unitRadioGroup.check(selectedUnitId);
        }
        saveButton.setText(isEditing()
                ? R.string.edit_workout_button_label : R.string.create_workout_button_label);
        renderDraft();
    }

    private void loadPresetForEditing(String id) {
        try (Cursor cursor = getPreset(database, id)) {
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
            selectUnit(definition.getUnit());
            draft = WorkoutEditorDraft.fromDefinition(definition);
            saveButton.setText(R.string.edit_workout_button_label);
            renderDraft();
        } catch (WorkoutValidationException exception) {
            Toast.makeText(this, exception.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void renderDraft() {
        List<Integer> path = draft.getPath();
        pathLabel.setText(buildPathLabel(path));
        upButton.setEnabled(!path.isEmpty());
        nodeList.removeAllViews();

        List<WorkoutEditorDraft.Node> nodes = draft.getCurrentNodes();
        for (int index = 0; index < nodes.size(); index++) {
            addNodeRow(nodes.get(index), index, path.size());
        }
    }

    private String buildPathLabel(List<Integer> path) {
        if (path.isEmpty()) {
            return getString(R.string.editor_root_path);
        }
        List<String> labels = new ArrayList<>();
        List<WorkoutEditorDraft.Node> siblings = draft.getRootNodes();
        for (Integer childIndex : path) {
            WorkoutEditorDraft.Node repeat = siblings.get(childIndex);
            labels.add(getRepeatTitle(repeat));
            siblings = repeat.getChildren();
        }
        return getString(R.string.editor_path_format, String.join(" › ", labels));
    }

    private void addNodeRow(WorkoutEditorDraft.Node node, int index, int depth) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(8 + depth * 16), dp(4), dp(8), dp(4));
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText(node.isRepeat() ? getRepeatTitle(node) : getPhaseTitle(node));
        title.setTextSize(16);
        row.addView(title);

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.END);
        if (node.isRepeat()) {
            addActionButton(controls, R.string.editor_open, view -> {
                draft.enterRepeat(index);
                renderDraft();
            });
        }
        addActionButton(controls, R.string.editor_edit, view -> {
            if (node.isRepeat()) {
                showRepeatDialog(index);
                return;
            }
            showPhaseDialog(index);
        });
        addActionButton(controls, R.string.editor_delete, view -> {
            if (node.isRepeat()) {
                confirmDeleteRepeat(index, node);
                return;
            }
            draft.getCurrentNodes().remove(index);
            renderDraft();
        });
        row.addView(controls);
        nodeList.addView(row);
    }

    private String getRepeatTitle(WorkoutEditorDraft.Node repeat) {
        String label = repeat.getLabel();
        if (label == null || label.trim().isEmpty()) {
            label = getString(R.string.editor_repeat_default_label);
        }
        return getString(R.string.editor_repeat_title, label, repeat.getRepeatCount());
    }

    private String getPhaseTitle(WorkoutEditorDraft.Node phase) {
        String unit = getSelectedUnitLabel();
        return getString(R.string.editor_phase_title, phase.getLength(), unit,
                phase.getTargetSpm());
    }

    private String getSelectedUnitLabel() {
        if (radioStrokes.isChecked()) {
            return getString(R.string.strokes);
        }
        if (radioMeters.isChecked()) {
            return getString(R.string.meters);
        }
        if (radioSeconds.isChecked()) {
            return getString(R.string.seconds);
        }
        return getString(R.string.editor_unit_unspecified);
    }

    private void showPhaseDialog(int index) {
        WorkoutEditorDraft.Node existing = index < 0
                ? null : draft.getCurrentNodes().get(index);
        LinearLayout fields = createDialogFields();
        EditText lengthInput = addNumberField(fields, R.string.editor_phase_length_hint,
                existing == null ? "" : Integer.toString(existing.getLength()));
        EditText spmInput = addNumberField(fields, R.string.editor_target_spm_hint,
                existing == null ? "" : Integer.toString(existing.getTargetSpm()));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(existing == null ? R.string.editor_add_phase_title
                        : R.string.editor_edit_phase_title)
                .setView(fields)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    Integer length = parsePositiveNumber(lengthInput);
                    Integer targetSpm = parsePositiveNumber(spmInput);
                    if (length == null || targetSpm == null) {
                        showInvalidNumber();
                        return;
                    }
                    if (existing == null) {
                        draft.getCurrentNodes().add(WorkoutEditorDraft.Node.phase(length, targetSpm));
                    } else {
                        existing.updatePhase(length, targetSpm);
                    }
                    dialog.dismiss();
                    renderDraft();
                }));
        dialog.show();
    }

    private void showRepeatDialog(int index) {
        WorkoutEditorDraft.Node existing = index < 0
                ? null : draft.getCurrentNodes().get(index);
        LinearLayout fields = createDialogFields();
        EditText labelInput = addTextField(fields, R.string.editor_repeat_label_hint,
                existing == null ? "" : existing.getLabel());
        EditText countInput = addNumberField(fields, R.string.editor_repeat_count_hint,
                existing == null ? "2" : Integer.toString(existing.getRepeatCount()));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(existing == null ? R.string.editor_add_repeat_title
                        : R.string.editor_edit_repeat_title)
                .setView(fields)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    Integer repeatCount = parsePositiveNumber(countInput);
                    if (repeatCount == null) {
                        showInvalidNumber();
                        return;
                    }
                    String label = labelInput.getText().toString().trim();
                    if (existing == null) {
                        draft.addRepeat(repeatCount, label);
                    } else {
                        existing.updateRepeat(repeatCount, label);
                    }
                    dialog.dismiss();
                    renderDraft();
                }));
        dialog.show();
    }

    private void confirmDeleteRepeat(int index, WorkoutEditorDraft.Node repeat) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.editor_delete_repeat_title)
                .setMessage(getString(R.string.editor_delete_repeat_confirmation,
                        getRepeatTitle(repeat)))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.editor_delete, (dialog, which) -> {
                    draft.getCurrentNodes().remove(index);
                    renderDraft();
                })
                .show();
    }

    private LinearLayout createDialogFields() {
        LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.VERTICAL);
        fields.setPadding(dp(24), dp(8), dp(24), 0);
        return fields;
    }

    private EditText addNumberField(LinearLayout parent, int hintResource, String value) {
        EditText input = addTextField(parent, hintResource, value);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
        return input;
    }

    private EditText addTextField(LinearLayout parent, int hintResource, String value) {
        EditText input = new EditText(this);
        input.setHint(hintResource);
        input.setText(value == null ? "" : value);
        parent.addView(input);
        return input;
    }

    private Integer parsePositiveNumber(EditText input) {
        try {
            int value = Integer.parseInt(input.getText().toString().trim());
            return value > 0 ? value : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void showInvalidNumber() {
        Toast.makeText(this, R.string.editor_positive_number_required, Toast.LENGTH_LONG).show();
    }

    private void addActionButton(LinearLayout parent, int labelResource,
                                 View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(labelResource);
        button.setOnClickListener(listener);
        parent.addView(button, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void saveWorkout() {
        WorkoutUnit unit = getSelectedUnit();
        if (unit == null) {
            Toast.makeText(this, R.string.missing_workout_type_warning, Toast.LENGTH_LONG).show();
            return;
        }

        ContentValues values = new ContentValues();
        try {
            WorkoutDefinition definition = draft.toDefinition(unit);
            new WorkoutCompiler().compile(definition);
            recordMapper.putDefinition(values, definition);
        } catch (WorkoutValidationException exception) {
            Toast.makeText(this, exception.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        values.put(WorkoutContract.WorkoutEntry1.COLUMN_NAME, nameEt.getText().toString());
        values.put(WorkoutContract.WorkoutEntry1.COLUMN_DESC, descEt.getText().toString());

        long result;
        if (isEditing()) {
            result = database.update(WorkoutContract.WorkoutEntry1.TABLE_NAME_PRESETS,
                    values,
                    WorkoutContract.WorkoutEntry1._ID + " = ?",
                    new String[]{workoutId});
            if (result > 0) {
                updateTimestamp(database, workoutId);
            }
        } else {
            result = database.insert(WorkoutContract.WorkoutEntry1.TABLE_NAME_PRESETS, null, values);
        }

        if (result <= 0) {
            Toast.makeText(this, R.string.preset_add_failed, Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, R.string.preset_added, Toast.LENGTH_LONG).show();
        finish();
    }

    private WorkoutUnit getSelectedUnit() {
        int selectedId = unitRadioGroup.getCheckedRadioButtonId();
        if (selectedId == radioStrokes.getId()) {
            return WorkoutUnit.STROKES;
        }
        if (selectedId == radioMeters.getId()) {
            return WorkoutUnit.METERS;
        }
        if (selectedId == radioSeconds.getId()) {
            return WorkoutUnit.SECONDS;
        }
        return null;
    }

    private void selectUnit(WorkoutUnit unit) {
        switch (unit) {
            case STROKES:
                unitRadioGroup.check(radioStrokes.getId());
                return;
            case METERS:
                unitRadioGroup.check(radioMeters.getId());
                return;
            case SECONDS:
                unitRadioGroup.check(radioSeconds.getId());
                return;
            default:
                throw new IllegalStateException("Unsupported workout unit: " + unit);
        }
    }

    private boolean isEditing() {
        return workoutId != null && !workoutId.isEmpty();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void updateTimestamp(SQLiteDatabase db, String id) {
        db.execSQL("UPDATE " + WorkoutContract.WorkoutEntry1.TABLE_NAME_PRESETS +
                        " SET " + WorkoutContract.WorkoutEntry1.COLUMN_TIMESTAMP
                        + " = CURRENT_TIMESTAMP WHERE "
                        + WorkoutContract.WorkoutEntry1._ID + " = ?",
                new Object[]{id});
    }

    private Cursor getPreset(SQLiteDatabase db, String id) {
        return db.query(WorkoutContract.WorkoutEntry1.TABLE_NAME_PRESETS,
                null,
                WorkoutContract.WorkoutEntry1._ID + " = ?",
                new String[]{id},
                null,
                null,
                WorkoutContract.WorkoutEntry1.COLUMN_TIMESTAMP);
    }
}
