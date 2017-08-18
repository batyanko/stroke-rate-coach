package com.example.yanko.strokeratecoach;

import android.content.Intent;
import android.content.SharedPreferences;
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

import com.example.yanko.strokeratecoach.Sliding.*;
import com.example.yanko.strokeratecoach.Utils.DialGridAdapter;
import com.example.yanko.strokeratecoach.Utils.SvAdapter;


/**
 * A simple {@link Fragment} subclass.
 */
public class SlideFragment extends Fragment implements SvAdapter.ListItemClickListener{

    private int firstDigit;
    private View firstDigitView;
    private int spm;
    private SharedPreferences pref;

    public static int width;
    public static int height;

    DialGridAdapter dialAdapter;
    SvAdapter svAdapter;

    Boolean bool;

    public SlideFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        pref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        return inflater.inflate(R.layout.fragment_slide, container, false);

    }

    private SlidingTabLayout mSlidingTabLayout;

    /**
     * A {@link ViewPager} which will be used in conjunction with the {@link SlidingTabLayout} above.
     */
    private ViewPager mViewPager;

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        // Get the ViewPager and set it's PagerAdapter so that it can display items
        mViewPager = (ViewPager) view.findViewById(R.id.viewpager);
        mViewPager.setAdapter(new SlidePagerAdapter());

        // Give the SlidingTabLayout the ViewPager, this must be done AFTER the ViewPager has had
        // it's PagerAdapter set.
        mSlidingTabLayout = (SlidingTabLayout) view.findViewById(R.id.sliding_tabs);
        mSlidingTabLayout.setViewPager(mViewPager);

        dialAdapter = new DialGridAdapter(view.getContext());
        svAdapter = new SvAdapter(15, this);
    }

    //OnClickListener for the Exercise list items
    @Override
    public void onListItemClick(int clickedItemIndex, int itemFunction) {

        String message = "Eh?";
        if (itemFunction == WaveActivity.FAV_BUTTON_FUNCTION) {
            message = "Fav yeah";
        } else if (itemFunction == WaveActivity.EXERCISE_ITEM_FUNCTION) {
            message = "Item yeah";
        } else if (itemFunction == WaveActivity.ENGAGE_EXERCISE_FUNCTION) {
            WaveActivity.GEAR_SETTINGS = WaveActivity.exerciseG1;
            WaveActivity.STROKES_PER_PHASE = WaveActivity.exerciseSPP1;
            pref.edit().putInt(WaveActivity.OPERATION_SETTING, WaveActivity.OPERATION_WAVE).apply();
            bool = pref.getBoolean(WaveActivity.SWITCH_SETTING, true);
            pref.edit().putBoolean(WaveActivity.SWITCH_SETTING, !bool).apply();
            message = "Engage yeah";
        }
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
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

            Log.d("KYDE SME NIE?", "" + position);

            View view;
            GridView dialGrid;
            RecyclerView exerciseRV;


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
                                pref.edit().putInt(WaveActivity.OPERATION_SETTING, WaveActivity.OPERATION_STOP).apply();
                                bool = pref.getBoolean(WaveActivity.SWITCH_SETTING, true);
                                pref.edit().putBoolean(WaveActivity.SWITCH_SETTING, !bool).apply();


//                                pref.edit().putInt(WaveActivity.OPERATION_SETTING, WaveActivity.OPERATION_STOP).apply();
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
                    view = getActivity().getLayoutInflater().inflate(R.layout.fragment_exercises, container, false);
                    exerciseRV = (RecyclerView) view.findViewById(R.id.exercise_rv);
                    LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
                    exerciseRV.setLayoutManager(layoutManager);
                    exerciseRV.setAdapter(svAdapter);
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
            container.removeView((View) object);
        }
    }

    public void setSpmFromDigital(int digitalInput, View view) {
        if (firstDigit != 0) {
//            endExercise();
            firstDigitView.setBackgroundColor(Color.TRANSPARENT);
            spm = firstDigit * 10 + digitalInput;
            //TODO: maendWaveke startTheTempo() use spm instead spmString
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
}
