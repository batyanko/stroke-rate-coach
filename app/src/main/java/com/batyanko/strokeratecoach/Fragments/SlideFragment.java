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

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import com.batyanko.strokeratecoach.R;
import com.batyanko.strokeratecoach.Sliding.*;
import com.batyanko.strokeratecoach.SpeedActivity;
import com.batyanko.strokeratecoach.Utils.DialGridAdapter;
import com.batyanko.strokeratecoach.Utils.SvAdapter;
import com.batyanko.strokeratecoach.WaveActivity;
import com.batyanko.strokeratecoach.data.WorkoutContract.PresetEntry1;
import com.batyanko.strokeratecoach.data.PresetDBHelper;


/**
 * A simple {@link Fragment} subclass.
 */
public class SlideFragment extends Fragment implements SvAdapter.ListItemClickListener {

    private int firstDigit;
    private View firstDigitView;
    private int spm;
    private SharedPreferences pref;

    public static int width;
    public static int height;

    DialGridAdapter dialAdapter;
    SvAdapter svAdapter;

    Cursor mCursor;

    RecyclerView workoutRV;

    Boolean bool;
    private SQLiteDatabase mDb;

    ViewGroup viewGroup;

    public SlideFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        pref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        dialAdapter = new DialGridAdapter(getContext());

        //////////////////////
        //SQLite stuff
        PresetDBHelper dbHelper = new PresetDBHelper(getActivity());
        mDb = dbHelper.getWritableDatabase();
//        addNewPreset("Hoplaaa", 34);
//        addNewPreset("sdgfgsfdg", 45);
//        addNewPreset("sdgfgsfdg", 45);
        mCursor = getAllPresets();

        svAdapter = new SvAdapter(getContext(), mCursor, this);

        viewGroup = container;
        return inflater.inflate(R.layout.fragment_slide, container, false);


    }

    private SlidingTabLayout mSlidingTabLayout;

    /**
     * A {@link ViewPager} which will be used in conjunction with the {@link SlidingTabLayout} above.
     */
    private ViewPager mViewPager;

    //Force workout ScrollView update, as it seems to persist after return from another activity.
    @Override
    public void onStart() {
        super.onStart();
        svAdapter.swapCursor(getAllPresets());
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

    //OnClickListener for the Workout list items
    @Override
    public void onListItemClick(long clickedItemId, int position, int itemFunction) {

        Log.d("BENCH onClick", "START");
        mCursor = getAllPresets();

        if (!mCursor.moveToPosition(position)) {
            Toast.makeText(getActivity(), "MOVEISFALSE", Toast.LENGTH_LONG).show();
            return;
        }
        String message;

        if (itemFunction == WaveActivity.FAV_BUTTON_FUNCTION) {
            Toast.makeText(getActivity(), "button works 8-)", Toast.LENGTH_LONG).show();
            removePreset(clickedItemId);
            svAdapter.swapCursor(getAllPresets());

        } else if (itemFunction == WaveActivity.WORKOUT_ITEM_FUNCTION) {

            message = mCursor.getString(mCursor.getColumnIndex(PresetEntry1.COLUMN_NAME)) +
                    " : " +
                    "\n" +
                    mCursor.getString(mCursor.getColumnIndex(PresetEntry1.COLUMN_SPP_CSV)) +
                    " at " +
                    mCursor.getString(mCursor.getColumnIndex(PresetEntry1.COLUMN_GEARS_CSV)) +
                    "\n" +
                    mCursor.getString(mCursor.getColumnIndex(PresetEntry1.COLUMN_DESC));
            Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();

        } else if (itemFunction == WaveActivity.ENGAGE_WORKOUT_FUNCTION) {
            final int workoutType =
                    mCursor.getInt(mCursor.getColumnIndex(PresetEntry1.COLUMN_WORKOUT_TYPE));
            final String gearCSV =  mCursor.getString(mCursor.getColumnIndex(PresetEntry1.COLUMN_GEARS_CSV));
            final String sppCSV = mCursor.getString(mCursor.getColumnIndex(PresetEntry1.COLUMN_SPP_CSV));
            final String[] gears = gearCSV.split("\\s*,\\s*");
            final String[] spp = sppCSV.split("\\s*,\\s*");

            //Number of phases must match number of gears
            if (gears.length != spp.length) {
                return;
            }

            int[] gearInts = new int[gears.length];
            int[] sppInts = new int[gears.length];
            for (int i = 0; i < gearInts.length; i++)
            {
                gearInts[i] = Integer.parseInt(gears[i]);
                sppInts[i] = Integer.parseInt(spp[i]);
            }

            WaveActivity.GEAR_SETTINGS = gearInts;
            WaveActivity.STROKES_PER_PHASE = sppInts;



            pref.edit().putInt(WaveActivity.OPERATION_SETTING, workoutType).apply();
            bool = pref.getBoolean(WaveActivity.SWITCH_SETTING, true);
            pref.edit().putBoolean(WaveActivity.SWITCH_SETTING, !bool).apply();
        }
        Log.d("BENCH onClick", "FINISH");
    }

    private class SlidePagerAdapter extends PagerAdapter {
        private String pageTitle;

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return object == view;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0: {
                    pageTitle = "Speed Dial";
                    break;
                }
                case 1: {
                    pageTitle = "Workouts";
                    break;
                }
                default: {
                    pageTitle = "Woot";
                    break;
                }
            }

            return pageTitle;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {

            View view;
            GridView dialGrid;


            switch (position) {
                case 0: {
                    view = getActivity().getLayoutInflater().inflate(R.layout.fragment_dial, container, false);
                    dialGrid = (GridView) view.findViewById(R.id.dial_grid_frag);
                    dialGrid.setAdapter(dialAdapter);

                    //Define dial grid button functions
                    dialGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                            if (position < 9) {
                                setSpmFromDigital(position + 1, view);
                            } else if (position == 9) {
                                setSpmFromDigital(0, view);
                            } else if (position == 10) {
                                //Hit the switch
                                pref.edit().putInt(WaveActivity.OPERATION_SETTING, WaveActivity.WORKOUT_STOP).apply();
                                bool = pref.getBoolean(WaveActivity.SWITCH_SETTING, true);
                                pref.edit().putBoolean(WaveActivity.SWITCH_SETTING, !bool).apply();


//                                pref.edit().putInt(WaveActivity.OPERATION_SETTING, WaveActivity.WORKOUT_STOP).apply();
                            } else if (position == 11) {
                                Intent intent = new Intent(getActivity(), SpeedActivity.class);
                                startActivity(intent);
                            }
                                /* else if (position == 12) {
                                Intent intent = new Intent(WaveActivity.this, Activity.class);
                                startActivity(intent);
                            }*/
                        }
                    });
                    break;
                }
                case 1: {
                    view = getActivity().getLayoutInflater().inflate(R.layout.fragment_workouts, container, false);
                    workoutRV = (RecyclerView) view.findViewById(R.id.workout_rv);
                    LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
                    workoutRV.setLayoutManager(layoutManager);
                    workoutRV.setAdapter(svAdapter);
                    mCursor = getAllPresets();
                    svAdapter.swapCursor(mCursor);
                    break;
                }
                default: {
                    view = getActivity().getLayoutInflater().inflate(R.layout.fragment_dial, container, false);
                }
            }
            /*
            View view = getActivity().getLayoutInflater().inflate(R.layout.teh_content, container, false);
                     */

            container.addView(view);

/*            TextView textView = (TextView) getActivity().findViewById(R.id.text_content);
            textView.setText(String.valueOf(position));*/


            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView(   (View) object);
        }
    }

    public void setSpmFromDigital(int digitalInput, View view) {
        if (firstDigit != 0) {
//            endWorkout();
            firstDigitView.setBackgroundColor(Color.TRANSPARENT);
            spm = firstDigit * 10 + digitalInput;
//            spmString = String.valueOf(spm);
//            Log.d("SpmString / spm: ", spmString + " / " + spm);
            pref.edit().putInt("spm", spm).apply();
//            startTheTempo();
            firstDigit = 0;

        } else {
            firstDigit = digitalInput;
            firstDigitView = view;
            view.setBackgroundColor(Color.RED);

//            Log.d("GridHeight!!!: ", "" + dialGrid.getHeight());
//            Log.d("WindowHeight!!!: ", "" + windowHeight);
//
//            ConstraintLayout constraintLayout = (ConstraintLayout) findViewById(R.id.activity_main);
//            Log.d("Constrai PostCreate???:", "" + constraintLayout.getHeight());
        }
    }

    /////////////////
    //DBStuff
    private Cursor getAllPresets() {
        return mDb.query(
                PresetEntry1.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                PresetEntry1.COLUMN_TIMESTAMP
        );
    }

    private Cursor getPreset(long position) {
        return mDb.query(
                PresetEntry1.TABLE_NAME,
                null,
                PresetEntry1._ID + "=" + position,
                null,
                null,
                null,
                PresetEntry1.COLUMN_TIMESTAMP
        );
    }

    private boolean removePreset(long position) {
        return mDb.delete(PresetEntry1.TABLE_NAME, PresetEntry1._ID + "=" + position, null) > 0;
    }
}
