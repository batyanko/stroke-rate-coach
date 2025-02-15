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

import static com.batyanko.strokeratecoach.WaveActivity.THEME_COLOR;
import static com.batyanko.strokeratecoach.WaveActivity.windowWidth;
import static com.batyanko.strokeratecoach.data.WorkoutContract.WorkoutEntry1.COLUMN_DESC;
import static com.batyanko.strokeratecoach.data.WorkoutContract.WorkoutEntry1.COLUMN_GEARS_CSV;
import static com.batyanko.strokeratecoach.data.WorkoutContract.WorkoutEntry1.COLUMN_NAME;
import static com.batyanko.strokeratecoach.data.WorkoutContract.WorkoutEntry1.COLUMN_SPP_CSV;
import static com.batyanko.strokeratecoach.data.WorkoutContract.WorkoutEntry1.COLUMN_SPP_TYPE;
import static com.batyanko.strokeratecoach.data.WorkoutContract.WorkoutEntry1.TABLE_NAME_HISTORY;
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
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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


/**
 * A simple {@link Fragment} subclass.
 */
public class SlideFragment extends Fragment implements SvAdapter.ListItemClickListener {

    private static final int TAB_COUNT = 3;

    private int firstDigit;
    private View firstDigitView;
    private int spm;
    private SharedPreferences pref;

    public static int width;
    public static int height;

    DialGridAdapter dialAdapter;
    private SvAdapter presetsAdapter;
    public SvAdapter historyAdapter;

    Cursor presetCursor;
    Cursor historyCursor;

    public RecyclerView presetRV;
    public RecyclerView historyRV;
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

        presetsAdapter = new SvAdapter(getContext(), presetCursor, WorkoutEntry1.TABLE_NAME_PRESETS, this);
        historyAdapter = new SvAdapter(getContext(), historyCursor, WorkoutEntry1.TABLE_NAME_HISTORY, this);

        viewGroup = container;
        return inflater.inflate(R.layout.fragment_slide, viewGroup, false);


    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        // Get the ViewPager and set it's PagerAdapter so that it can display items
        mViewPager = (ViewPager) view.findViewById(R.id.viewpager);
        mViewPager.setAdapter(new SlidePagerAdapter());

        // Give the SlidingTabLayout the ViewPager, this must be done AFTER the ViewPager has had
        // it's PagerAdapter set.
        mSlidingTabLayout = (SlidingTabLayout) view.findViewById(R.id.sliding_tabs);
        mSlidingTabLayout.setViewPager(mViewPager);
    }

    //Force workout ScrollView update, as it seems to persist after return from another activity.
    @Override
    public void onStart() {
        super.onStart();
        //TODO take running workout status from beeper service
        presetsAdapter.swapCursor(getAllPresets(), workoutIsRunning, lastWorkoutId);
        historyAdapter.swapCursor(getAllHistory(), workoutIsRunning, lastWorkoutId);
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


            workoutInfoLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    descPopupWindow.dismiss();
                }
            });

            // No preset functions on History items
            if (tableName.equals(TABLE_NAME_HISTORY)) {
                ((ViewGroup) workoutDelButton.getParent()).removeView(workoutDelButton);
                ((ViewGroup) workoutEditButton.getParent()).removeView(workoutEditButton);
                ((ViewGroup) workoutCopyButton.getParent()).removeView(workoutCopyButton);
                return;
            }

            workoutEditButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String wId = cursor.getString(cursor.getColumnIndex(WorkoutEntry1._ID));
                    intent = new Intent(getActivity(), EntryFormActivity.class);
                    intent.putExtra(EXTRA_WORKOUT_ID, wId);
                    startActivity(intent);
                    descPopupWindow.dismiss();
                }
            });
            workoutCopyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Cursor bkpCursor = getPreset(clickedItemId);
                    bkpCursor.moveToFirst();
                    long added = addPreset(workoutDb,
                            bkpCursor.getString(bkpCursor.getColumnIndex(COLUMN_NAME)),
                            bkpCursor.getString(bkpCursor.getColumnIndex(COLUMN_DESC)),
                            bkpCursor.getString(bkpCursor.getColumnIndex(COLUMN_SPP_CSV)),
                            bkpCursor.getString(bkpCursor.getColumnIndex(COLUMN_GEARS_CSV)),
                            bkpCursor.getString(bkpCursor.getColumnIndex(COLUMN_SPP_TYPE))
                    );
                    if (added != -1) {
                        presetsAdapter.swapCursor(getAllPresets(), workoutIsRunning, lastWorkoutId);
                        WaveUtilities.ShowShortToast("Workout duplicated!", getContext());
                    } else {
                        WaveUtilities.ShowLongToast("Copy failed. Try backup from settings instead.", getContext());
                    }
                    bkpCursor.close();
                    descPopupWindow.dismiss();
                }
            });
            workoutDelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //            TODO Confirm Delete!
                    if (removeWorkout(clickedItemId, tableName)) {
                        WaveUtilities.ShowShortToast("Preset removed.", getContext());
                    } else {
                        WaveUtilities.ShowShortToast("Sorry, failed.", getContext());
                    }

                    //refresh ScrollView after DB update
                    presetsAdapter.swapCursor(getAllPresets(), workoutIsRunning, lastWorkoutId);
                    descPopupWindow.dismiss();
                }
            });

        } else if (itemFunction == WaveActivity.ENGAGE_WORKOUT_FUNCTION) {
            stopBeeper();

            lastClickedEngageButton = view;
            final String name = cursor.getString(cursor.getColumnIndex(WorkoutEntry1.COLUMN_NAME));
            final String description = cursor.getString(cursor.getColumnIndex(WorkoutEntry1.COLUMN_DESC));
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
            addHistory(workoutDb, name, description, sppCSV, gearCSV, sppType);
            historyAdapter.swapCursor(getAllHistory(), true, lastWorkoutId);

            // Start beeping?
            initIntent(sppInts, gearInts, sppType);
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
                case 0: {
                    pageTitle = getString(R.string.speed_dial_title);
                    break;
                }
                case 1: {
                    pageTitle = getString(R.string.workouts_title);
                    break;
                }
                case 2: {
                    pageTitle = getString(R.string.history_title);
                    break;
                }
                default: {
                    pageTitle = "Woot";
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
                case 0: {
                    view = requireActivity().getLayoutInflater().inflate(R.layout.fragment_dial, container, false);
                    dialGrid = (GridView) view.findViewById(R.id.dial_grid_frag);
                    dialGrid.setAdapter(dialAdapter);
                    dialGrid.setBackgroundColor(pref.getInt(THEME_COLOR, 0xfffafafa));
                    //Define dial grid button functions
                    dialGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                            if (position < 9) {
                                setSpmFromDigital(position + 1, view);
                            } else if (position == 10) {
                                setSpmFromDigital(0, view);
                            } else if (position == 9) {
//                                checkBeeper();
//                                stopBeeper();
                            } else if (position == 11) {
//                                getActivity().setTheme(R.style.AppThemeLight);
//                                final Context contextThemeWrapper = new ContextThemeWrapper(getActivity(), R.style.Theme_AppCompat_Light);
//                                contextThemeWrapper.setTheme(R.style.AppThemeLight);

//                                checkBeeper(intent);
//                                Intent intent = new Intent(SlideFragment.this.getActivity(), SpeedActivity.class);
//                                startActivity(intent);
                            }
                        }
                    });
                    break;
                }
                case 1: {
                    view = requireActivity().getLayoutInflater().inflate(R.layout.fragment_presets, container, false);
                    presetRV = (RecyclerView) view.findViewById(R.id.preset_rv);
                    LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
                    presetRV.setLayoutManager(layoutManager);
                    presetRV.setAdapter(presetsAdapter);
                    presetRV.setBackgroundColor(pref.getInt(THEME_COLOR, 0xfffafafa));
                    presetCursor = getAllPresets();
                    presetsAdapter.swapCursor(presetCursor, workoutIsRunning, lastWorkoutId);
                    break;
                }
                case 2: {
                    view = requireActivity().getLayoutInflater().inflate(R.layout.fragment_history, container, false);
                    historyRV = (RecyclerView) view.findViewById(R.id.history_rv);
                    LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
                    historyRV.setLayoutManager(layoutManager);
                    historyRV.setAdapter(historyAdapter);
                    historyRV.setBackgroundColor(pref.getInt(THEME_COLOR, 0xfffafafa));
                    historyCursor = getAllHistory();
                    historyAdapter.swapCursor(historyCursor, workoutIsRunning, lastWorkoutId);
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

//            Log.d("GridHeight!!!: ", "" + dialGrid.getHeight());
//            Log.d("WindowHeight!!!: ", "" + windowHeight);
//
//            ConstraintLayout constraintLayout = (ConstraintLayout) findViewById(R.id.activity_main);
//            Log.d("Constrai PostCreate???:", "" + constraintLayout.getHeight());
        }
    }

    private void initIntent(int[] sppSettings, int[] gearSettings, String sppType) {

        intent.setAction(BeeperTasks.ACTION_START_BEEP);
        intent.putExtra(BeeperTasks.EXTRA_WORKOUT_SPP, sppSettings);
        intent.putExtra(BeeperTasks.EXTRA_WORKOUT_GEARS, gearSettings);
        intent.putExtra(BeeperTasks.EXTRA_WORKOUT_SPP_TYPE, sppType);

//        if (requestLocation()) {
//            Log.d("HAS PERMISSON", "HAS LOCATION");
        startBeeper();
//        }
        ;
    }

    public void startBeeper() {
        this.requireActivity().startService(intent);
        BeeperServiceUtils.doBindService(intent, requireActivity(), mConnection);
        //        lastClickedEngageButton.setBackgroundResource(R.drawable.ic_menu_play_clip_negative);
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

    private Cursor getAllRows(String tableName) {
        return workoutDb.query(
                tableName,
                null,
                null,
                null,
                null,
                null,
                WorkoutEntry1.COLUMN_TIMESTAMP
        );
    }

    private Cursor getPreset(long position) {
        return workoutDb.query(
                WorkoutEntry1.TABLE_NAME_PRESETS,
                null,
                WorkoutEntry1._ID + "=" + position,
                null,
                null,
                null,
                WorkoutEntry1.COLUMN_TIMESTAMP
        );
    }

    private long addPreset(SQLiteDatabase db, String name, String description,
                           String spp, String gears, String sppType) {
        ContentValues cv = new ContentValues();
        cv.put(WorkoutEntry1.COLUMN_NAME, name);
        cv.put(WorkoutEntry1.COLUMN_DESC, description);
        cv.put(WorkoutEntry1.COLUMN_SPP_CSV, spp);
        cv.put(WorkoutEntry1.COLUMN_GEARS_CSV, gears);
        cv.put(WorkoutEntry1.COLUMN_SPP_TYPE, sppType);
        return addToDb(db, WorkoutEntry1.TABLE_NAME_PRESETS, cv);
    }

    private long addHistory(SQLiteDatabase db, String name, String description,
                            String spp, String gears, String sppType) {
        ContentValues cv = new ContentValues();
        cv.put(WorkoutEntry1.COLUMN_NAME, name);
        cv.put(WorkoutEntry1.COLUMN_DESC, description);
        cv.put(WorkoutEntry1.COLUMN_SPP_CSV, spp);
        cv.put(WorkoutEntry1.COLUMN_GEARS_CSV, gears);
        cv.put(WorkoutEntry1.COLUMN_SPP_TYPE, sppType);
        return addToDb(db, WorkoutEntry1.TABLE_NAME_HISTORY, cv);
    }

    private long addToDb(SQLiteDatabase db, String table, android.content.ContentValues values) {
        return db.insert(table, null, values);
    }

    private boolean removeWorkout(long position, String tableName) {
        return workoutDb.delete(tableName, WorkoutEntry1._ID + "=" + position, null) > 0;
    }


}