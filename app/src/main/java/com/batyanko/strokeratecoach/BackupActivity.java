package com.batyanko.strokeratecoach;

import static com.batyanko.strokeratecoach.WaveActivity.windowWidth;
import static com.batyanko.strokeratecoach.data.WorkoutContract.WorkoutEntry1.COLUMN_DESC;
import static com.batyanko.strokeratecoach.data.WorkoutContract.WorkoutEntry1.COLUMN_NAME;
import static com.batyanko.strokeratecoach.data.WorkoutContract.WorkoutEntry1.TABLE_NAME_PRESETS;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.ViewGroupOverlay;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.batyanko.strokeratecoach.data.WorkoutContract;
import com.batyanko.strokeratecoach.data.WorkoutDBHelper;
import com.batyanko.strokeratecoach.data.WorkoutRecordMapper;
import com.batyanko.strokeratecoach.workout.LegacyWorkoutParser;
import com.batyanko.strokeratecoach.workout.WorkoutBackupCodec;
import com.batyanko.strokeratecoach.workout.WorkoutCompiler;
import com.batyanko.strokeratecoach.workout.WorkoutDefinition;
import com.batyanko.strokeratecoach.workout.WorkoutJsonCodec;
import com.batyanko.strokeratecoach.workout.WorkoutValidationException;

public class BackupActivity extends AppCompatActivity {

    private WorkoutRecordMapper recordMapper;
    private WorkoutBackupCodec backupCodec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_backup);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        SQLiteDatabase workoutDb;
        WorkoutDBHelper presetDbHelper;
        presetDbHelper = new WorkoutDBHelper(this);
        workoutDb = presetDbHelper.getWritableDatabase();
        WorkoutJsonCodec jsonCodec = new WorkoutJsonCodec();
        LegacyWorkoutParser legacyParser = new LegacyWorkoutParser();
        WorkoutCompiler compiler = new WorkoutCompiler();
        recordMapper = new WorkoutRecordMapper(jsonCodec, legacyParser, compiler);
        backupCodec = new WorkoutBackupCodec(jsonCodec, legacyParser, compiler);

        Button createBkpButton = findViewById(R.id.create_backup);
        Button restoreBkpButton = findViewById(R.id.restore_backup);

        createBkpButton.setOnClickListener(
                v -> {
                    if (workoutDb == null) {
                        // TODO handle DB problems
                    } else {
                        createBkp(workoutDb);
                    }
                }
        );
        restoreBkpButton.setOnClickListener(
                v -> {
                    if (workoutDb == null) {
                        // TODO handle DB problems
                    } else {
                        restoreBkp(workoutDb);
                    }
                }
        );

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //TODO close DB?
    }

    private void createBkp(SQLiteDatabase workoutDb) {
        StringBuilder dump = new StringBuilder();
        dump.append(this.getString(R.string.backup_comment));
        try (Cursor bkpCursor = workoutDb.query(
                TABLE_NAME_PRESETS,
                null,
                null,
                null,
                null,
                null,
                WorkoutContract.WorkoutEntry1.COLUMN_TIMESTAMP
        )) {
            while (bkpCursor.moveToNext()) {
                WorkoutDefinition definition = recordMapper.readDefinition(bkpCursor);
                String name = bkpCursor.getString(bkpCursor.getColumnIndexOrThrow(COLUMN_NAME));
                String description = bkpCursor.getString(
                        bkpCursor.getColumnIndexOrThrow(COLUMN_DESC));
                dump.append(backupCodec.encodeRecord(name, description, definition))
                        .append('\n')
                        .append(getString(R.string.preset_delimiter))
                        .append("\n\n");
            }
        } catch (WorkoutValidationException exception) {
            Toast.makeText(this, "Backup failed: " + exception.getMessage(),
                    Toast.LENGTH_LONG).show();
            return;
        }
        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("simple text", dump.toString());
        clipboard.setPrimaryClip(clip);
    }

    private void restoreBkp(SQLiteDatabase workoutDb) {
        ClipboardManager clipboardMan = (ClipboardManager)
                getSystemService(CLIPBOARD_SERVICE);
        ClipData primaryClip = clipboardMan.getPrimaryClip();
        if (primaryClip == null) {
            displayReport(getString(R.string.bkp_nobkp_error), "-", "-");
            return;
        }

        CharSequence clipboardText = primaryClip.getItemAt(0).getText();
        if (clipboardText == null) {
            displayReport(getString(R.string.bkp_nobkp_error), "-", "-");
            return;
        }
        String gottenDump = clipboardText.toString();

        String[] presets = gottenDump.split(this.getString(R.string.preset_delimiter));
        if (!containsBackupFields(presets)) {
            displayReport(getString(R.string.bkp_nobkp_error), "-", "-");
            return;
        }

        StringBuilder presetsAdded = new StringBuilder();
        StringBuilder presetsBotched = new StringBuilder();
        int cntAdded = 0;
        int cntFailed = 0;

        for (int i = 0; i < presets.length; i++) {
            String preset = presets[i];
            String trimmedPreset = preset.trim();
            if (trimmedPreset.isBlank() || trimmedPreset.charAt(0) == '#') {
                continue;
            }
            String name = extractName(preset, Integer.toString(i));
            try {
                WorkoutBackupCodec.BackupRecord record = backupCodec.parseRecord(preset, name);
                name = record.getName();
                WorkoutDefinition definition = record.getDefinition();
                ContentValues values = new ContentValues();
                values.put(COLUMN_NAME, name);
                if (record.getDescription() != null) {
                    values.put(COLUMN_DESC, record.getDescription());
                }
                recordMapper.putDefinition(values, definition);

                long rowInserted = workoutDb.insert(TABLE_NAME_PRESETS, null, values);
                if (rowInserted == -1) {
                    presetsBotched.append("- ").append(name)
                            .append(": database insert failed\n");
                    cntFailed++;
                    continue;
                }
                presetsAdded.append("- ").append(name).append("\n");
                cntAdded++;
            } catch (WorkoutValidationException exception) {
                presetsBotched.append("- ").append(name)
                        .append(": ").append(exception.getMessage()).append('\n');
                cntFailed++;
            }
        }
        String summary = cntAdded + " restored, " + cntFailed + " failed.";
        if (cntFailed > 0) {
            summary = summary + getString(R.string.bkp_malformed_error);
        }

        displayReport(summary, presetsAdded.toString(), presetsBotched.toString());
    }

    private boolean containsBackupFields(String[] presets) {
        for (String preset : presets) {
            String trimmed = preset.trim();
            if (trimmed.isEmpty() || trimmed.charAt(0) == '#') {
                continue;
            }
            if (preset.contains("Type: ") || preset.contains("SPM: ")
                    || preset.contains("SPP: ")) {
                return true;
            }
        }
        return false;
    }

    private String extractName(String record, String fallback) {
        for (String line : record.split("\n", -1)) {
            if (line.startsWith("Name: ")) {
                return line.substring("Name: ".length()).trim();
            }
        }
        return fallback;
    }

    private void displayReport(String summary, String added, String failed) {
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        float density = this.getResources().getDisplayMetrics().density;
        LinearLayout backupInfoLayout = (LinearLayout) inflater.inflate(R.layout.backup_report_layout, null);

        TextView summaryTv = backupInfoLayout.findViewById(R.id.report_summary_tv);
        EditText reportAddedTv = backupInfoLayout.findViewById(R.id.report_added_tv);
        EditText reportFailedTv = backupInfoLayout.findViewById(R.id.report_failed_tv);
        summaryTv.setText(summary);
        reportAddedTv.setText(added);
        reportFailedTv.setText(failed);
        int tvHeight = this.getResources().getDisplayMetrics().heightPixels / 4;
        reportAddedTv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, tvHeight));
        reportFailedTv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, tvHeight));
        reportAddedTv.setGravity(Gravity.TOP);
        reportFailedTv.setGravity(Gravity.TOP);

        PopupWindow descPopupWindow = new PopupWindow(backupInfoLayout, windowWidth, LinearLayout.LayoutParams.WRAP_CONTENT, true);
        descPopupWindow.setBackgroundDrawable(
                new ColorDrawable(getResources().getColor(R.color.greyLight))
        );
        descPopupWindow.setElevation(10f);
        ViewGroup root = (ViewGroup) getWindow().getDecorView().getRootView();
        Drawable dim = new ColorDrawable(getResources().getColor(R.color.backgroundDark));
        dim.setBounds(0, 0, root.getWidth(), root.getHeight());
        dim.setAlpha((int) (255 * 0.5f));
        ViewGroupOverlay overlay = root.getOverlay();
        overlay.add(dim);

        descPopupWindow.setOnDismissListener(() -> overlay.clear());
        backupInfoLayout.setOnClickListener(v -> descPopupWindow.dismiss());
        descPopupWindow.showAtLocation(backupInfoLayout, Gravity.CENTER, 0, (int) density * 100);
    }

}