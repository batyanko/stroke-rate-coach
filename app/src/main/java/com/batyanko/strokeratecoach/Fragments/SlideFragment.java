/*
 * Copyright (C) 2013 The Android Open Source Project
 * Modifications Copyright (C) 2017 Yanko Georgiev
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

package com.batyanko.strokeratecoach.Fragments;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.batyanko.strokeratecoach.WaveActivity.THEME_COLOR;
import static com.batyanko.strokeratecoach.WaveActivity.windowWidth;
import static com.batyanko.strokeratecoach.sync.BeeperTasks.EXTRA_WORKOUT_ID;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.batyanko.strokeratecoach.EntryFormActivity;
import com.batyanko.strokeratecoach.R;
import com.batyanko.strokeratecoach.Sliding.SlidingTabLayout;
import com.batyanko.strokeratecoach.Utils.DialGridAdapter;
import com.batyanko.strokeratecoach.Utils.SvAdapter;
import com.batyanko.strokeratecoach.Utils.WaveUtilities;
import com.batyanko.strokeratecoach.WaveActivity;
import com.batyanko.strokeratecoach.data.WorkoutContract.WorkoutEntry1;
import com.batyanko.strokeratecoach.data.WorkoutDBHelper;
import com.batyanko.strokeratecoach.sync.BeeperService;
import com.batyanko.strokeratecoach.sync.BeeperServiceUtils;
import com.batyanko.strokeratecoach.sync.BeeperTasks;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class SlideFragment extends Fragment implements SvAdapter.ListItemClickListener {

    private static final int TAB_COUNT = 4;

    private int firstDigit;
    private View firstDigitView;
    private int spm;
    private SharedPreferences pref;

    public static int width;
    public static int height;

    DialGridAdapter dialAdapter;
    private SvAdapter presetsAdapter;
    public SvAdapter historyAdapter;
    public SvAdapter trashAdapter;

    Cursor presetCursor;
    Cursor historyCursor;
    Cursor trashCursor;

    public RecyclerView presetRV;
    public RecyclerView historyRV;
    public RecyclerView trashRV;
    public GridView dialGrid;

    private static SQLiteDatabase workoutDb;

    ViewGroup viewGroup;
    private BeeperService mBeeperService;
    private ServiceConnection mConnection;

    private Intent intent;
    public View lastClickedEngageButton;

    public int lastWorkoutId;
    private boolean workoutIsRunning;

    public static final String SPP_VALID = "[0123456789,]+";

    public SlidingTabLayout mSlidingTabLayout;
    /**
     * A {@link ViewPager} which will be used in conjunction with the {@link SlidingTabLayout} above.
     */
    private ViewPager mViewPager;
    private View workoutInfoLayout;
    private View workoutCopyButton;
    private View workoutEditButton;
    private View workoutDelButton;
    private PopupWindow descPopupWindow;
    private TextView descPopupTextView;
    private FloatingActionButton adderFab;
    private final int SPEED_DIAL_POSITION = 0;
    private final int PRESETS_POSITION = 1;
    private final int HISTORY_POSITION = 2;
    private final int TRASH_POSITION = 3;


    public SlideFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBeeperService = BeeperServiceUtils.getBeeperService();
        mConnection = BeeperServiceUtils.getServiceConnection();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        intent = new Intent(this.getActivity(), BeeperService.class);

        workoutIsRunning = false;

        dialAdapter = new DialGridAdapter(getContext());

        //////////////////////
        //SQLite stuff
        WorkoutDBHelper presetDbHelper = new WorkoutDBHelper(getActivity());
        workoutDb = presetDbHelper.getWritableDatabase();
        presetCursor = getAllPresets();
        historyCursor = getAllHistory();
        trashCursor = getAllHistory();

        presetsAdapter = new SvAdapter(getContext(), presetCursor, WorkoutEntry1.TABLE_NAME_PRESETS, this);
        historyAdapter = new SvAdapter(getContext(), historyCursor, WorkoutEntry1.TABLE_NAME_HISTORY, this);
        trashAdapter = new SvAdapter(getContext(), trashCursor, WorkoutEntry1.TABLE_NAME_TRASH, this);

        viewGroup = container;
        return inflater.inflate(R.layout.fragment_slide, viewGroup, false);


    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        adderFab = view.findViewById(R.id.adder_fab);
        adderFab.setVisibility(INVISIBLE);
        adderFab.setAlpha(0.5F);
        adderFab.setOnClickListener(v -> {
            Intent entryIntent = new Intent(getActivity(), EntryFormActivity.class);
            startActivity(entryIntent);
        });

        // Get the ViewPager and set it's PagerAdapter so that it can display items
        mViewPager = view.findViewById(R.id.viewpager);
        mViewPager.setAdapter(new SlidePagerAdapter());
        mViewPager.addOnPageChangeListener((new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                if (position == PRESETS_POSITION) {
                    adderFab.setVisibility(VISIBLE);
                } else {
                    adderFab.setVisibility(INVISIBLE);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        }));

        // Give the SlidingTabLayout the ViewPager, this must be done AFTER the ViewPager has had
        // it's PagerAdapter set.
        mSlidingTabLayout = view.findViewById(R.id.sliding_tabs);
        mSlidingTabLayout.setViewPager(mViewPager);

        ViewGroup bigChild = (ViewGroup) mSlidingTabLayout.getChildAt(0);
        View tabTitle = bigChild.getChildAt(TRASH_POSITION);
        tabTitle.setBackground(ResourcesCompat.getDrawable(view.getResources(), R.drawable.ic_delete, view.getContext().getTheme()));


    }


    //Force workout ScrollView update, as it seems to persist after return from another activity.
    @Override
    public void onStart() {
        super.onStart();
        //TODO take running workout status from beeper service
        presetsAdapter.swapCursor(getAllPresets(), workoutIsRunning, lastWorkoutId);
        historyAdapter.swapCursor(getAllHistory(), workoutIsRunning, lastWorkoutId);
        trashAdapter.swapCursor(getAllTrash(), workoutIsRunning, lastWorkoutId);
    }

    //OnClickListener for the Workout list items
    @Override
    public void onListItemClick(View view, long clickedItemId, int position, String tableName, Cursor cursor, int itemFunction) {


        //TODO get only clicked preset for efficiency

        if (!cursor.moveToPosition(position)) {
            Toast.makeText(getActivity(), "MOVEISFALSE", Toast.LENGTH_LONG).show();
            return;
        }
        String message;

        if (itemFunction == WaveActivity.WORKOUT_ITEM_FUNCTION) { // Tap anywhere on workout

            message = cursor.getString(cursor.getColumnIndex(WorkoutEntry1.COLUMN_NAME)) +
                    " : " +
                    "\n" +
                    cursor.getString(cursor.getColumnIndex(WorkoutEntry1.COLUMN_SPP_CSV)) +
                    " at " +
                    cursor.getString(cursor.getColumnIndex(WorkoutEntry1.COLUMN_GEARS_CSV)) +
                    "\n" +
                    cursor.getString(cursor.getColumnIndex(WorkoutEntry1.COLUMN_DESC));

            LayoutInflater inflater = (LayoutInflater) requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            float density = requireActivity().getResources().getDisplayMetrics().density;
            workoutInfoLayout = inflater.inflate(R.layout.workout_info_layout, null);

            descPopupTextView = workoutInfoLayout.findViewById(R.id.popup_desc_text_view);
            descPopupTextView.setText(message);
            descPopupWindow = new PopupWindow(workoutInfoLayout, windowWidth, LinearLayout.LayoutParams.WRAP_CONTENT, true);
            descPopupWindow.setBackgroundDrawable(
                    new ColorDrawable(pref.getInt(THEME_COLOR, getResources().getColor(R.color.backgroundLight)))
            );
            descPopupWindow.setElevation(100f);
            descPopupWindow.showAtLocation(workoutInfoLayout, Gravity.CENTER, 0, (int) density * 100);

            workoutDelButton = workoutInfoLayout.findViewById(R.id.delete_button_inside);
            workoutEditButton = workoutInfoLayout.findViewById(R.id.edit_button);
            workoutCopyButton = workoutInfoLayout.findViewById(R.id.copy_button);


            workoutInfoLayout.setOnClickListener(view4 -> descPopupWindow.dismiss());

            // Editing enabled only in Presets tab
            if (tableName.equals(WorkoutEntry1.TABLE_NAME_PRESETS)) {
                workoutEditButton.setOnClickListener(view3 -> {
                    String wId = cursor.getString(cursor.getColumnIndex(WorkoutEntry1._ID));
                    Intent entryIntent = new Intent(getActivity(), EntryFormActivity.class);
                    entryIntent.putExtra(EXTRA_WORKOUT_ID, wId);
                    startActivity(entryIntent);
                    descPopupWindow.dismiss();
                });
            } else {
                ((ViewGroup) workoutEditButton.getParent()).removeView(workoutEditButton);
                ((TextView) workoutCopyButton.findViewById(R.id.copy_button_text)).setText(R.string.copy_to_presets);
            }

            workoutCopyButton.setOnClickListener(view2 -> {
                Cursor bkpCursor = getPreset(clickedItemId, tableName);
                bkpCursor.moveToFirst();
                boolean added = addPreset(workoutDb, WorkoutEntry1.TABLE_NAME_PRESETS,
                        bkpCursor.getString(bkpCursor.getColumnIndex(WorkoutEntry1.COLUMN_NAME)),
                        bkpCursor.getString(bkpCursor.getColumnIndex(WorkoutEntry1.COLUMN_DESC)),
                        bkpCursor.getString(bkpCursor.getColumnIndex(WorkoutEntry1.COLUMN_TIMESTAMP)),
                        bkpCursor.getString(bkpCursor.getColumnIndex(WorkoutEntry1.COLUMN_SPP_CSV)),
                        bkpCursor.getString(bkpCursor.getColumnIndex(WorkoutEntry1.COLUMN_GEARS_CSV)),
                        bkpCursor.getString(bkpCursor.getColumnIndex(WorkoutEntry1.COLUMN_SPP_TYPE))
                );
                if (added) {
                    presetsAdapter.swapCursor(getAllPresets(), workoutIsRunning, lastWorkoutId);
                    if (tableName.equals(WorkoutEntry1.TABLE_NAME_TRASH)) {
                        removeWorkout(clickedItemId, WorkoutEntry1.TABLE_NAME_TRASH);
                        trashAdapter.swapCursor(getAllTrash(), workoutIsRunning, lastWorkoutId);
                        WaveUtilities.ShowShortToast("Restored to Workouts.", getContext());
                    } else {
                        WaveUtilities.ShowShortToast("Copied to Workouts.", getContext());
                    }
                } else {
                    WaveUtilities.ShowLongToast("Copy failed.", getContext());
                }
                bkpCursor.close();
                descPopupWindow.dismiss();
            });
            workoutDelButton.setOnClickListener(view1 -> {
                boolean success;
                // Move to trash, or...
                if (!tableName.equals(WorkoutEntry1.TABLE_NAME_TRASH)) {
                    Cursor bkpCursor = getPreset(clickedItemId, tableName);
                    bkpCursor.moveToFirst();
                    success = addPreset(workoutDb, WorkoutEntry1.TABLE_NAME_TRASH,
                            bkpCursor.getString(bkpCursor.getColumnIndex(WorkoutEntry1.COLUMN_NAME)),
                            bkpCursor.getString(bkpCursor.getColumnIndex(WorkoutEntry1.COLUMN_DESC)),
                            bkpCursor.getString(bkpCursor.getColumnIndex(WorkoutEntry1.COLUMN_TIMESTAMP)),
                            bkpCursor.getString(bkpCursor.getColumnIndex(WorkoutEntry1.COLUMN_SPP_CSV)),
                            bkpCursor.getString(bkpCursor.getColumnIndex(WorkoutEntry1.COLUMN_GEARS_CSV)),
                            bkpCursor.getString(bkpCursor.getColumnIndex(WorkoutEntry1.COLUMN_SPP_TYPE))
                    );
                    if (success) {
                        trashAdapter.swapCursor(getAllTrash(), workoutIsRunning, lastWorkoutId);
                        if (removeWorkout(clickedItemId, tableName)) {
                            WaveUtilities.ShowShortToast("Moved to trash.", getContext());
                        } else {
                            WaveUtilities.ShowShortToast("Sorry, failed.", getContext());
                        }
                    } else {
                        WaveUtilities.ShowShortToast("Sorry, failed.", getContext());
                    }
                    // ...erase from Trash
                } else {
                    success = removeWorkout(clickedItemId, tableName);
                    if (success) {
                        WaveUtilities.ShowShortToast("Removed.", getContext());
                    } else {
                        WaveUtilities.ShowShortToast("Sorry, failed.", getContext());
                    }
                }
                refresh(tableName);
                descPopupWindow.dismiss();
            });

        } else if (itemFunction == WaveActivity.ENGAGE_WORKOUT_FUNCTION) {
            stopBeeper();

            lastClickedEngageButton = view;
            final String name = cursor.getString(cursor.getColumnIndex(WorkoutEntry1.COLUMN_NAME));
            final String description = cursor.getString(cursor.getColumnIndex(WorkoutEntry1.COLUMN_DESC));
            final String ts = cursor.getString(cursor.getColumnIndex(WorkoutEntry1.COLUMN_TIMESTAMP));
            final String sppType =
                    cursor.getString(cursor.getColumnIndex(WorkoutEntry1.COLUMN_SPP_TYPE));
            lastWorkoutId = cursor.getInt(cursor.getColumnIndex(WorkoutEntry1._ID));
            final String gearCSV = cursor.getString(cursor.getColumnIndex(WorkoutEntry1.COLUMN_GEARS_CSV));
            final String sppCSV = cursor.getString(cursor.getColumnIndex(WorkoutEntry1.COLUMN_SPP_CSV));
            //TODO check validity
            final String[] gears = gearCSV.split("\\s*,\\s*");
            final String[] spp = sppCSV.split("\\s*,\\s*");

            //Number of phases must match number of gears
            if (gears.length != spp.length) {
                return;
            }

            int[] gearInts = new int[gears.length];
            int[] sppInts = new int[gears.length];
            for (int i = 0; i < gearInts.length; i++) {
                gearInts[i] = Integer.parseInt(gears[i]);
                sppInts[i] = Integer.parseInt(spp[i]);
            }

            //Update History db table
            lastClickedEngageButton.setBackgroundResource(R.drawable.ic_play_4_negative);
            addPreset(workoutDb, WorkoutEntry1.TABLE_NAME_HISTORY, name, description, ts, sppCSV, gearCSV, sppType);
            historyAdapter.swapCursor(getAllHistory(), true, lastWorkoutId);

            // Start beeping?
            initIntent(sppInts, gearInts, sppType);
        }
    }

    private void refresh(String tableName) {
        //refresh ScrollView after DB update
        switch (tableName) {
            case WorkoutEntry1.TABLE_NAME_PRESETS:
                presetsAdapter.swapCursor(getAllPresets(), workoutIsRunning, lastWorkoutId);
                break;
            case WorkoutEntry1.TABLE_NAME_HISTORY:
                historyAdapter.swapCursor(getAllHistory(), workoutIsRunning, lastWorkoutId);
                break;
            case WorkoutEntry1.TABLE_NAME_TRASH:
                trashAdapter.swapCursor(getAllTrash(), workoutIsRunning, lastWorkoutId);
                break;
            default:
        }
    }

    private class SlidePagerAdapter extends PagerAdapter {
        private String pageTitle;


        @Override
        public int getCount() {
            return TAB_COUNT;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return object == view;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case SPEED_DIAL_POSITION: {
                    pageTitle = getString(R.string.speed_dial_title);
                    break;
                }
                case PRESETS_POSITION: {
                    pageTitle = getString(R.string.workouts_title);
                    break;
                }
                case HISTORY_POSITION: {
                    pageTitle = getString(R.string.history_title);
                    break;
                }
                default: {
                    pageTitle = "";
                    break;
                }
            }
            return pageTitle;
        }

        @NonNull
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view;

            switch (position) {
                case SPEED_DIAL_POSITION: {
                    view = requireActivity().getLayoutInflater().inflate(R.layout.fragment_dial, container, false);
                    dialGrid = view.findViewById(R.id.dial_grid_frag);
                    dialGrid.setAdapter(dialAdapter);
                    dialGrid.setBackgroundColor(pref.getInt(THEME_COLOR, 0xfffafafa));
                    //Define dial grid button functions
                    dialGrid.setOnItemClickListener((parent, view1, position1, id) -> {

                        if (position1 < 9) {
                            setSpmFromDigital(position1 + 1, view1);
                        } else if (position1 == 10) {
                            setSpmFromDigital(0, view1);
                        }
                    });
                    break;
                }
                case PRESETS_POSITION: {
                    view = requireActivity().getLayoutInflater().inflate(R.layout.fragment_presets, container, false);
                    presetRV = view.findViewById(R.id.preset_rv);
                    LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
                    presetRV.setLayoutManager(layoutManager);
                    presetRV.setAdapter(presetsAdapter);
                    presetRV.setBackgroundColor(pref.getInt(THEME_COLOR, 0xfffafafa));
                    presetCursor = getAllPresets();
                    presetsAdapter.swapCursor(presetCursor, workoutIsRunning, lastWorkoutId);
                    break;
                }
                case HISTORY_POSITION: {
                    view = requireActivity().getLayoutInflater().inflate(R.layout.fragment_history, container, false);
                    historyRV = view.findViewById(R.id.history_rv);
                    LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
                    historyRV.setLayoutManager(layoutManager);
                    historyRV.setAdapter(historyAdapter);
                    historyRV.setBackgroundColor(pref.getInt(THEME_COLOR, 0xfffafafa));
                    historyCursor = getAllHistory();
                    historyAdapter.swapCursor(historyCursor, workoutIsRunning, lastWorkoutId);
                    break;
                }
                case TRASH_POSITION: {
                    view = requireActivity().getLayoutInflater().inflate(R.layout.fragment_trash, container, false);
                    trashRV = view.findViewById(R.id.trash_rv);
                    LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
                    trashRV.setLayoutManager(layoutManager);
                    trashRV.setAdapter(trashAdapter);
                    trashRV.setBackgroundColor(pref.getInt(THEME_COLOR, 0xfffafafa));
                    trashCursor = getAllTrash();
                    trashAdapter.swapCursor(trashCursor, workoutIsRunning, lastWorkoutId);
                    break;
                }
                default: {
                    view = requireActivity().getLayoutInflater().inflate(R.layout.fragment_dial, container, false);
                }
            }
            container.addView(view);

            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }
    }

    public void setSpmFromDigital(int digitalInput, View view) {
        if (firstDigit != 0) {
            stopBeeper();
            firstDigitView.animate().scaleX(1);
            firstDigitView.animate().scaleY(1);
            firstDigitView.setBackgroundColor(Color.TRANSPARENT);
            spm = firstDigit * 10 + digitalInput;
            int[] gearArray = new int[]{spm};
            initIntent(null, gearArray, BeeperTasks.SPP_TYPE_STROKES);
            firstDigit = 0;

        } else {
            view.setBackgroundColor(requireActivity().getResources().getColor(R.color.colorAccent));
            //Bug fix - scaled view does not accept touches on API 17 / Nexus 4
            view.animate().scaleX(1.15f).setDuration(50).start();
            view.animate().scaleY(1.15f).setDuration(50).start();

            //SPM lower than 10 not allowed
            if (digitalInput == 0) {
                view.startAnimation(WaveActivity.warningAnimation);
                view.setBackgroundColor(requireActivity().getResources().getColor(R.color.colorTransparent));
            } else {
                firstDigit = digitalInput;
                firstDigitView = view;
            }
        }
    }

    private void initIntent(int[] sppSettings, int[] gearSettings, String sppType) {

        intent.setAction(BeeperTasks.ACTION_START_BEEP);
        intent.putExtra(BeeperTasks.EXTRA_WORKOUT_SPP, sppSettings);
        intent.putExtra(BeeperTasks.EXTRA_WORKOUT_GEARS, gearSettings);
        intent.putExtra(BeeperTasks.EXTRA_WORKOUT_SPP_TYPE, sppType);

        startBeeper();
    }

    public void startBeeper() {
        this.requireActivity().startService(intent);
        BeeperServiceUtils.doBindService(intent, requireActivity(), mConnection);
    }

    //TODO extract static method and use in Notification stopper
    public void stopBeeper() {
        pref.edit().putInt(WaveActivity.OPERATION_SETTING, WaveActivity.WORKOUT_STOP).apply();
        if (lastClickedEngageButton != null) {
            lastClickedEngageButton.setBackgroundResource(R.drawable.ic_play_4);
        }

        mBeeperService = BeeperServiceUtils.getBeeperService();
        mConnection = BeeperServiceUtils.getServiceConnection();

        if (mBeeperService == null) {
            return;
        }
        intent.setAction(BeeperTasks.ACTION_STOP_BEEP);
        mBeeperService.modWorkout(intent);
    }

    /////////////////
    //DBStuff
    //TODO use getAllRows
    private Cursor getAllPresets() {
        return workoutDb.query(
                WorkoutEntry1.TABLE_NAME_PRESETS,
                null,
                null,
                null,
                null,
                null,
                WorkoutEntry1.COLUMN_TIMESTAMP
        );
    }

    public static Cursor getAllHistory() {
        return workoutDb.query(
                WorkoutEntry1.TABLE_NAME_HISTORY,
                null,
                null,
                null,
                null,
                null,
                WorkoutEntry1.COLUMN_TIMESTAMP + " DESC"
        );
    }

    public static Cursor getAllTrash() {
        return workoutDb.query(
                WorkoutEntry1.TABLE_NAME_TRASH,
                null,
                null,
                null,
                null,
                null,
                WorkoutEntry1.COLUMN_TIMESTAMP + " DESC"
        );
    }

    private Cursor getPreset(long position, String tab) {
        return workoutDb.query(
                tab,
                null,
                WorkoutEntry1._ID + "=" + position,
                null,
                null,
                null,
                WorkoutEntry1.COLUMN_TIMESTAMP
        );
    }

    private boolean addPreset(SQLiteDatabase db, String tab, String name, String description,
                              String ts, String spp, String gears, String sppType) {
        ContentValues cv = new ContentValues();
        cv.put(WorkoutEntry1.COLUMN_NAME, name);
        cv.put(WorkoutEntry1.COLUMN_DESC, description);
        cv.put(WorkoutEntry1.COLUMN_TIMESTAMP, ts);
        cv.put(WorkoutEntry1.COLUMN_SPP_CSV, spp);
        cv.put(WorkoutEntry1.COLUMN_GEARS_CSV, gears);
        cv.put(WorkoutEntry1.COLUMN_SPP_TYPE, sppType);
        return addToDb(db, tab, cv) != -1;
    }

    private long addToDb(SQLiteDatabase db, String table, android.content.ContentValues values) {
        return db.insert(table, null, values);
    }

    private boolean removeWorkout(long position, String tableName) {
        return workoutDb.delete(tableName, WorkoutEntry1._ID + "=" + position, null) > 0;
    }

}