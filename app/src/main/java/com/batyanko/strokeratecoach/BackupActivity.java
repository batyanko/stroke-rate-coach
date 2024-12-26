package com.batyanko.strokeratecoach;

import static com.batyanko.strokeratecoach.Fragments.SlideFragment.SPP_VALID;
import static com.batyanko.strokeratecoach.WaveActivity.windowHeight;
import static com.batyanko.strokeratecoach.WaveActivity.windowWidth;
import static com.batyanko.strokeratecoach.data.WorkoutContract.WorkoutEntry1.COLUMN_DESC;
import static com.batyanko.strokeratecoach.data.WorkoutContract.WorkoutEntry1.COLUMN_GEARS_CSV;
import static com.batyanko.strokeratecoach.data.WorkoutContract.WorkoutEntry1.COLUMN_NAME;
import static com.batyanko.strokeratecoach.data.WorkoutContract.WorkoutEntry1.COLUMN_SPP_CSV;
import static com.batyanko.strokeratecoach.data.WorkoutContract.WorkoutEntry1.COLUMN_SPP_TYPE;
import static com.batyanko.strokeratecoach.sync.BeeperTasks.SPP_TYPE_METERS;
import static com.batyanko.strokeratecoach.sync.BeeperTasks.SPP_TYPE_SECONDS;
import static com.batyanko.strokeratecoach.sync.BeeperTasks.SPP_TYPE_STROKES;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroupOverlay;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.batyanko.strokeratecoach.data.WorkoutContract;
import com.batyanko.strokeratecoach.data.WorkoutDBHelper;

import org.w3c.dom.Text;

import java.util.Arrays;

public class BackupActivity extends AppCompatActivity {

    private static final String BKP_ID_NAME = "Name: ";
    private static final String BKP_ID_DESC = "Desc: ";
    private static final String BKP_ID_TYPE = "Type: ";
    private static final String BKP_ID_GEARS = "SPM: ";
    private static final String BKP_ID_SPP = "SPP: ";

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

        Button createBkpButton = findViewById(R.id.create_backup);
        Button restoreBkpButton = findViewById(R.id.restore_backup);

        createBkpButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (workoutDb == null) {
                            // TODO handle DB problems
                        } else {
                            createBkp(workoutDb);
                        }
                    }
                }
        );
        restoreBkpButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (workoutDb == null) {
                            // TODO handle DB problems
                        } else {
                            restoreBkp(workoutDb);
                        }
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
        Cursor bkpCursor = workoutDb.query(
                WorkoutContract.WorkoutEntry1.TABLE_NAME_PRESETS,
                null,
                null,
                null,
                null,
                null,
                WorkoutContract.WorkoutEntry1.COLUMN_TIMESTAMP
        );
        bkpCursor.moveToFirst();
        StringBuilder dump = new StringBuilder();
        dump.append(this.getString(R.string.backup_comment));
        while (!bkpCursor.isAfterLast()) {
            dump.append(BKP_ID_NAME).append(bkpCursor.getString(bkpCursor.getColumnIndex(COLUMN_NAME))).append("\n")
                    .append(BKP_ID_DESC).append(bkpCursor.getString(bkpCursor.getColumnIndex(COLUMN_DESC))).append("\n")
                    .append(BKP_ID_TYPE).append(bkpCursor.getString(bkpCursor.getColumnIndex(COLUMN_SPP_TYPE))).append("\n")
                    .append(BKP_ID_GEARS).append(bkpCursor.getString(bkpCursor.getColumnIndex(COLUMN_GEARS_CSV))).append("\n")
                    .append(BKP_ID_SPP).append(bkpCursor.getString(bkpCursor.getColumnIndex(COLUMN_SPP_CSV))).append("\n")
                    .append(this.getString(R.string.preset_delimiter)).append("\n");
            bkpCursor.moveToNext();
        }
        bkpCursor.close();
        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("simple text", dump.toString());
        clipboard.setPrimaryClip(clip);
    }

    private void restoreBkp(SQLiteDatabase workoutDb) {
        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(CLIPBOARD_SERVICE);
        String gottenDump = clipboard.getPrimaryClip().getItemAt(0).getText().toString();
        String[] presets = gottenDump.split(this.getString(R.string.preset_delimiter));
        StringBuilder presetsAdded = new StringBuilder();
        StringBuilder presetsBotched = new StringBuilder();
        int cntAdded = 0;
        int cntFailed = 0;

        presetsLoop:
        for (int i = 0; i < presets.length; i++) {
            String preset = presets[i];
            String trimmedPreset = preset.trim();
            if (trimmedPreset.isBlank() || trimmedPreset.charAt(0) == '#') {
                continue;
            }
            String[] details = trimmedPreset.split("\n");
            ContentValues cv = new ContentValues();

            String name = String.valueOf(i);
            Log.d("Next preset:", preset);

            for (String s : details) {
                String detail = s.trim();
                if (detail.length() <= 6) {
                    continue; // positions reserved for workout detail ID
                }
                Log.d("presetDetail:", detail);
                Log.d("dbDetail:", detail);
                //TODO Validity checks
                if (detail.startsWith(BKP_ID_NAME)) {
                    name = detail.substring(BKP_ID_NAME.length());
                    cv.put(COLUMN_NAME, name);
                } else if (detail.startsWith(BKP_ID_DESC)) {
                    cv.put(COLUMN_DESC, detail.substring(BKP_ID_DESC.length()));
                } else if (detail.startsWith(BKP_ID_TYPE)) {
                    String type = detail.substring(BKP_ID_TYPE.length());
                    if (!Arrays.asList(SPP_TYPE_STROKES, SPP_TYPE_METERS, SPP_TYPE_SECONDS).contains(type)) {
                        presetsBotched.append("- ").append(name)
                                .append(" (Workout type may only be 0 (strokes), 1 (meters) or 2 (seconds))\n");
                        cntFailed++;
                        continue presetsLoop;
                    }
                    cv.put(COLUMN_SPP_TYPE, detail.substring(BKP_ID_TYPE.length()));
                } else if (detail.startsWith(BKP_ID_GEARS)) {
                    String gears = detail.substring(BKP_ID_GEARS.length());
                    if (!gears.matches(SPP_VALID)) {
                        presetsBotched.append("- ").append(name)
                                .append(" (Gears/Tempo setting may only include 0-9 and ',')\n");
                        cntFailed++;
                        continue presetsLoop;
                    }
                    cv.put(COLUMN_GEARS_CSV, gears);
                } else if (detail.startsWith(BKP_ID_SPP)) {
                    String spp = detail.substring(BKP_ID_SPP.length());
                    if (!spp.matches(SPP_VALID)) {
                        presetsBotched.append("- ").append(name)
                                .append(" (Phase length setting setting may only include 0-9 and ',')\n");
                        cntFailed++;
                        continue presetsLoop;
                    }
                    cv.put(COLUMN_SPP_CSV, spp);
                }
            }
            long rowInserted = workoutDb.insert(WorkoutContract.WorkoutEntry1.TABLE_NAME_PRESETS, null, cv);
            if (rowInserted != -1) {
                presetsAdded.append("- ").append(name).append("\n");
                cntAdded++;
            } else {
                presetsBotched.append("- ").append(name).append("\n");
                cntFailed++;
            }
        }
        String summary = cntAdded + " restored, " + cntFailed + " failed.";
        if (cntFailed > 0) {
            summary = summary + "\nMake sure that the backup data for these workouts is not malformed, and try again.";
        }

        displayReport(summary, presetsAdded.toString(), presetsBotched.toString());
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
//        int tvHeight = (int) ((float) windowHeight /3 * density + 0.5f);
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

        descPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                overlay.clear();
            }
        });
        descPopupWindow.showAtLocation(backupInfoLayout, Gravity.CENTER, 0, (int) density * 100);

    }

}