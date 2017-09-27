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

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.batyanko.strokeratecoach.data.WorkoutDBHelper;
import com.batyanko.strokeratecoach.data.WorkoutContract;
import com.batyanko.strokeratecoach.sync.BeeperTasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.text.TextUtils.join;

public class EntryFormActivity extends AppCompatActivity {

    private EditText nameEt;
    private EditText descEt;
    Button addPhaseButton;
    Button createWorkoutButton;
    private static int numberOfLines;

    RadioButton radioStrokes;
    RadioButton radioMeters;
    RadioButton radioSeconds;

    LinearLayout ll;
    LinearLayout ld;
    LinearLayout lb;

    private SQLiteDatabase mDb;

    List<EditText> sppEditTexts;
    List<EditText> gearEditTexts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry_form);

        numberOfLines = 0;
        sppEditTexts = new ArrayList<>();
        gearEditTexts = new ArrayList<>();

        WorkoutDBHelper presetDbHelper = new WorkoutDBHelper(this);
        mDb = presetDbHelper.getWritableDatabase();

        radioStrokes = findViewById(R.id.radio_strokes);
        radioMeters = findViewById(R.id.radio_meters);
        radioSeconds = findViewById(R.id.radio_seconds);

        nameEt = findViewById(R.id.workout_name_et);
        descEt = findViewById(R.id.workout_desc_et);
        addLine();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("DESTROY", "ON DESTROY");
    }

    public void onRadioButtonClicked(View view) {
        boolean isChecked = ((RadioButton) view).isChecked();
        switch (view.getId()) {
            case R.id.radio_strokes:
                if (isChecked) {
                    Log.d("RADIOGAGA", "strokes");
                    break;
                }
            case R.id.radio_meters:
                if (isChecked) {
                    Log.d("RADIOGAGA", "meters");
                    break;
                }
            case R.id.radio_seconds:
                if (isChecked) {
                    Log.d("RADIOGAGA", "seconds");
                    break;
                }
        }
    }

    private void addLine() {
        numberOfLines++;

        LinearLayout.LayoutParams pHorizontal = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        LinearLayout.LayoutParams pHorizontalWrap = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        LinearLayout.LayoutParams pVertical = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pHorizontal.width = 300;

        if (ll != null && lb != null) {
            ll.removeView(lb);
        }

        ll = (LinearLayout) findViewById(R.id.entry_layout);
        ld = new LinearLayout(this);
        ld.setLayoutParams(pVertical);
        lb = new LinearLayout(this);
        lb.setLayoutParams(pVertical);

        ll.addView(ld);

        //Add TextView title for SPP field
        String SPPTitle = (" " + numberOfLines + ": ");
        TextView SPPTitleView = new TextView(this);
        SPPTitleView.setLayoutParams(pHorizontalWrap);
        SPPTitleView.setText(SPPTitle);
        ld.addView(SPPTitleView);

        //Add SPP field for new phase
        sppEditTexts.add(new EditText(this));
        sppEditTexts.get(sppEditTexts.size() - 1).setLayoutParams(pHorizontal);
        sppEditTexts.get(sppEditTexts.size() - 1).setRawInputType(2);
        ld.addView(sppEditTexts.get(sppEditTexts.size() - 1));

        //Add TextView title for gear field
        String gearTitle = (" " + getString(R.string.gear_title_text) + " ");
        TextView gearTitleView = new TextView(this);
        gearTitleView.setLayoutParams(pHorizontalWrap);
        gearTitleView.setText(gearTitle);
        ld.addView(gearTitleView);

        //Add gear field for new phase
        Log.d("BENCHMARK LIST", "START");
        gearEditTexts.add(new EditText(this));
        gearEditTexts.get(gearEditTexts.size() - 1).setLayoutParams(pHorizontal);
        gearEditTexts.get(gearEditTexts.size() - 1).setRawInputType(2);
        ld.addView(gearEditTexts.get(gearEditTexts.size() - 1));
        Log.d("BENCHMARK LIST", "FINISH");

//        ll.removeView(addPhaseButton);
//        ll.removeView(createWorkoutButton);


        addPhaseButton = new Button(this);
        addPhaseButton.setLayoutParams(pHorizontal);
        addPhaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addLine();
            }
        });
        addPhaseButton.setText(R.string.button_add_phase_label);

        createWorkoutButton = new Button(this);
        createWorkoutButton.setLayoutParams(pHorizontal);
        createWorkoutButton.setOnClickListener(CreateButtonListener);
        createWorkoutButton.setText(R.string.create_workout_button_label);

        ll.addView(lb);
        lb.addView(addPhaseButton);
        lb.addView(createWorkoutButton);
    }

    private View.OnClickListener CreateButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            int sppType = getSPPFromRadio();

            //Don't create training if no SPP type selected
            if (sppType == 9) {
                Toast.makeText(EntryFormActivity.this,
                        R.string.missing_workout_type_warning,
                        Toast.LENGTH_LONG).show();
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
                Log.d("TEHJAY", j + "");

                spp[j - 1] = sppString;
                gears[j - 1] = gearString;
                Log.d("CURRENT PHASE SAVED", spp[j - 1] + " " + gears[j - 1]);
            }

            //Check for empty lines or an empty workout
            if (j == 0) {
                Toast.makeText(
                        EntryFormActivity.this,
                        R.string.empty_workout_warning,
                        Toast.LENGTH_LONG).show();
                return;
            } else if (emptyLinesExist) {
                Toast.makeText(
                        EntryFormActivity.this,
                        R.string.empty_data_notification,
                        Toast.LENGTH_SHORT).show();
            }

            String[] sppResized = Arrays.copyOfRange(spp, 0, j);
            String[] gearsResized = Arrays.copyOfRange(gears, 0, j);

            Log.d("CSV", join(",", sppResized) + "  " + join(",", sppResized));


            Log.d("TEHRADIO", "Selected: " + sppType);
            long i = addPreset(
                    mDb,
                    name,
                    desc,
                    join(",", sppResized),
                    join(",", gearsResized),
                    sppType);
            Log.d("ADDEDPRESETID", "" + i);
            Toast.makeText(EntryFormActivity.this,
                    "Preset added.",
                    Toast.LENGTH_LONG).show();

            finish();

/*            Intent intent = new Intent(EntryFormActivity.this, WaveActivity.class);
            NavUtils.navigateUpTo(EntryFormActivity.this, intent);*/
        }
    };

    private long addPreset(SQLiteDatabase db, String name, String description,
                           String spp, String gears, int sppType) {
        ContentValues cv = new ContentValues();
        cv.put(WorkoutContract.WorkoutEntry1.COLUMN_NAME, name);
        cv.put(WorkoutContract.WorkoutEntry1.COLUMN_DESC, description);
        cv.put(WorkoutContract.WorkoutEntry1.COLUMN_SPP_CSV, spp);
        cv.put(WorkoutContract.WorkoutEntry1.COLUMN_GEARS_CSV, gears);
//        cv.put(WorkoutEntry1.COLUMN_TIMESTAMP, time);
        cv.put(WorkoutContract.WorkoutEntry1.COLUMN_SPP_TYPE, sppType);
        return db.insert(WorkoutContract.WorkoutEntry1.TABLE_NAME_PRESETS, null, cv);
    }

    private int getSPPFromRadio() {
        if (radioStrokes.isChecked()) {
            return BeeperTasks.SPP_TYPE_STROKES;
        } else if (radioMeters.isChecked()) {
            return BeeperTasks.SPP_TYPE_METERS;
        } else if (radioSeconds.isChecked()) {
            return BeeperTasks.SPP_TYPE_SECONDS;
        } else return 9;
    }

    public class CustomWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    }
}
