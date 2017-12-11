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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.*;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.*;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.widget.*;

import com.batyanko.strokeratecoach.Fragments.SlideFragment;
import com.batyanko.strokeratecoach.Utils.SpeedViewAdapter;
import com.batyanko.strokeratecoach.sync.BeeperService;
import com.batyanko.strokeratecoach.sync.BeeperServiceUtils;
import com.batyanko.strokeratecoach.sync.BeeperTasks;

public class WaveActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = "StrokeRateCoach";

    //Constants for OnSharedPreferenceChangeListener
    public static final String OPERATION_SETTING = "operation";
    public static final String SWITCH_SETTING = "switch";
    public static final String SPM_SETTING = "spm";

    //Workout types
    public static final int WORKOUT_WAVE = 1;
    public static final int WORKOUT_PROGRESS = 2;
    public static final int WORKOUT_STOP = 0;

    //Workout item function IDs
    public static final int FAV_BUTTON_FUNCTION = 1;
    public static final int WORKOUT_ITEM_FUNCTION = 2;
    public static final int ENGAGE_WORKOUT_FUNCTION = 3;

    public static final int MY_LOCATION_PERMISSION = 22;

    //Shared preferences
    public static final String PHASE_LENGTH = "phase-length";
    public static final String TOTAL_WORKOUT_LENGTH = "total-workout-length";
    public static final String PHASE_PROGRESS = "total-strokes-elapsed";
    public static final String CURRENT_COLOR = "current-phase";
    public static final String CURRENT_SPEED = "current-speed";
    public static final String SPEED_LIMIT = "speed-limit";
    public static final String SPEED_LIMIT_SWITCH = "speed-limit-switch";
    public static final String SPEED_UNIT = "speed-unit";
    public static final String SPEED_MS = "m/s";
    public static final String SPEED_500M = "/500m";
    public static final String COUNTDOWN_RUNNING = "countdown-running";
    public static final String COUNTDOWN_DURATION = "countdown-duration";   //In ms
    public static final String COUNTDOWN_DURATION_LEFT = "countdown-duration-left";   //In ms
    public static final String BEEP = "beep";
    public static final String THEME = "theme";
    public static final int THEME_DARK = 0;
    public static final int THEME_LIGHT = 1;
    public static final String THEME_COLOR = "theme-color";

    /* Global spm setting to hold current spm */
    public static int spm;

    //UI elements
    private ProgressBar waveProgress;
    private Button createButton;
    private ImageView countdownView;
    private TextView spmTextView;
    private TextView progressTextView;

    private TextView countdownDigit;

    private TextView speedView;
    private TextView speedLimitView;
    private Spinner speedUnitSpinner;

    private ImageView water;


    private SharedPreferences pref;

    //autoWave values
    private static int workoutRunning = 0;     //0 = off, 1 = autoWave, 2 = autoProgress

    public static int windowWidth;
    public static int windowHeight;
    public static int statusbarHeight;

    private static BeeperService mBeeperService;
    private static boolean mIsBound;

    private static Toast mToast;

    private SlideFragment slideFragment;
    static int random;

    //SpeedLimit popup UI
    private EditText speedLimitEditText;
    private View speedLimitPopupLayout;
    private PopupWindow popupWindow;
    private SwitchCompat speedLimitSwitch;

    private static AsyncTask asyncTask = null;
    boolean upIsTouched = false;
    boolean downIsTouched = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("BENCHMARKING", "0");

        super.onCreate(savedInstanceState);
        Log.d("BENCHMARKING", "0.5");

        setContentView(R.layout.activity_wave);
        final ViewGroup viewGroup = (ViewGroup) findViewById(R.id.activity_wave);

        Log.d("INTHEBEGINNING", "" + BeeperTasks.spm);

        Log.d("BENCHMARKING", "1");

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        slideFragment = new SlideFragment();
        transaction.replace(R.id.slide_frame_layout, slideFragment);
        //TODO add to back stack?
        transaction.commit();
        Log.d("BENCHMARKING", "2");

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        windowWidth = metrics.widthPixels;
        windowHeight = metrics.heightPixels;

        //Initialize spm at last setting, or default at 0
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        pref.registerOnSharedPreferenceChangeListener(this);
        spm = pref.getInt(SPM_SETTING, 0);

        Log.d("BENCHMARKING", "3");
        //Initialize UI elements
        this.getWindow().getDecorView().setBackgroundColor(
                pref.getInt(THEME_COLOR, getResources().getColor(R.color.backgroundLight)));

        waveProgress = (ProgressBar) findViewById(R.id.wave_progress_bar_2);
        waveProgress.setVisibility(View.INVISIBLE);

        spmTextView = (TextView) findViewById(R.id.SpmTextView_2);
        spmTextView.setText(String.valueOf(spm));
        progressTextView = (TextView) findViewById(R.id.progressTextView);

        speedView = (TextView) findViewById(R.id.speed_view);

        String[] speeds = {SPEED_500M, SPEED_MS};
        speedUnitSpinner = (Spinner) findViewById(R.id.speed_unit);
        final SpeedViewAdapter myAdapter = new SpeedViewAdapter(this, R.layout.spinner_tv, speeds);
        speedUnitSpinner.setAdapter(myAdapter);
        speedUnitSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                String string = myAdapter.getItem(position);
                if (string != null && (string.equals(SPEED_MS) || string.equals(SPEED_500M))) {
                    pref.edit().putString(SPEED_UNIT, string).apply();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }

        });
        String selectedString = (String) ((Spinner) findViewById(R.id.speed_unit)).getSelectedItem();

        speedLimitView = (TextView) findViewById(R.id.speed_limit_view);
        String speedLimit = "" + pref.getInt(SPEED_LIMIT, 0);
        speedLimit = getSpeedString(speedLimit);
        speedLimitView.setText(speedLimit);
        speedLimitView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater inflater = (LayoutInflater) WaveActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                float density = WaveActivity.this.getResources().getDisplayMetrics().density;
                speedLimitPopupLayout = inflater.inflate(R.layout.speed_limit_layout, null);

                popupWindow = new PopupWindow(speedLimitPopupLayout, windowWidth, LinearLayout.LayoutParams.WRAP_CONTENT, true);
                popupWindow.setBackgroundDrawable(
                        new ColorDrawable(pref.getInt(THEME_COLOR, getResources().getColor(R.color.backgroundLight))));

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    popupWindow.setElevation(100f);
                }

                popupWindow.showAsDropDown(speedLimitView);
                popupWindow.showAtLocation(speedLimitPopupLayout, Gravity.CENTER, 0, (int) density * 100);

                speedLimitSwitch =
                        speedLimitPopupLayout.findViewById(R.id.speed_limit_switch);
                speedLimitEditText = speedLimitPopupLayout.findViewById(R.id.speed_limit_edit_text);
                final Button setLimit = speedLimitPopupLayout.findViewById(R.id
                        .speed_limit_setter_button);
                final Button saveAndExit = speedLimitPopupLayout.findViewById(R.id
                        .speed_limit_confirm_button);
                final ImageView up = speedLimitPopupLayout.findViewById(R.id.increase_speed);
                final ImageView down = speedLimitPopupLayout.findViewById(R.id.decrease_speed);

                String speedLimit = "" + pref.getInt(SPEED_LIMIT, 0);
                speedLimit = getSpeedString(speedLimit);
                speedLimitEditText.setText(speedLimit);
                speedLimitEditText.selectAll();

                setLimit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        float speedLimitFloat = Float.parseFloat(speedLimitEditText.getText().toString());
                        pref.edit().putInt(SPEED_LIMIT, (int) (speedLimitFloat * 100)).apply();
                    }
                });

                speedLimitSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean isOn) {
                        pref.edit().putBoolean(SPEED_LIMIT_SWITCH, isOn).apply();
                        updateSpeedSwitchGUI(isOn);
                    }
                });
                speedLimitSwitch.setChecked(pref.getBoolean(SPEED_LIMIT_SWITCH, false));

                up.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                            upIsTouched = true;
                            incrementSpeedTrigger();
                        } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                            upIsTouched = false;
                        }
                        return true;
                    }
                });
                down.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                            downIsTouched = true;
                            incrementSpeedTrigger();
                        } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                            downIsTouched = false;
                        }
                        return true;
                    }
                });


                saveAndExit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        popupWindow.dismiss();
                    }
                });

            }
        });

        createButton = (Button) findViewById(R.id.create_workout_button);
        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(WaveActivity.this, EntryFormActivity.class);
                startActivity(intent);
            }
        });
        createButton.setVisibility(View.VISIBLE);
        Log.d("BENCHMARKING", "4");

        int resource = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resource > 0) {
            statusbarHeight = getResources().getDimensionPixelSize(resource);
//            Log.d("Statusbar Height!!!: ", "" + statusbarHeight);
        }

        PackageManager manager = getPackageManager();
        int permission = manager.checkPermission("android.permission.ACCESS_FINE_LOCATION",
                "com.batyanko.strokeratecoach");
        boolean hasPermission = (permission == PackageManager.PERMISSION_GRANTED);
//
        if (!hasPermission) {
            Log.d("I CAN HAZ PERMISSION?", "NO");
            ActivityCompat.requestPermissions(this,
                    new String[]{"android.permission.ACCESS_FINE_LOCATION"}, MY_LOCATION_PERMISSION);
        }

        Log.d("BENCHMARKING", "5");

        countdownView = findViewById(R.id.countdown_image_view);
//        countdownView.setVisibility((View.INVISIBLE));

        countdownDigit = findViewById(R.id.countdown_digit);
        countdownDigit.setVisibility(View.INVISIBLE);

        //Bind to service if already running (in case of screen rotation / onDestroy)
        Log.d("BENCHMARKING", "6");

        flushGUI();
    }

    @Override
    protected void onStart() {
        super.onStart();
        onProgressChange();
        Log.d("BENCHMARKING", "7");
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
//        Animation animation = new AlphaAnimation(1,0);
//        animation.setDuration(2000);

            countdownView.animate().alpha(0f).setDuration(3000).withEndAction(
                    new Runnable() {
                        @Override
                        public void run() {
                            Log.d("ALPHAA", "" + countdownView.getAlpha());
                            countdownView.setAlpha(1f);
                            countdownView.setVisibility(View.INVISIBLE);
                        }
                    }
            );
        } else {
            countdownView.setVisibility(View.INVISIBLE);
        }
//        countdownView.startAnimation(animation);

        if (pref.getInt(OPERATION_SETTING, 0) == WORKOUT_PROGRESS) {
            //Bind to service if a workout is running
            Intent intent = new Intent(this, BeeperService.class);
            intent.setAction(BeeperTasks.ACTION_JUST_BIND);
            BeeperServiceUtils.doBindService(intent, this, BeeperServiceUtils.getServiceConnection());
        }

        Log.d("BENCHMARKING", "8");
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("ONDESTROY", "STOP");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        pref.unregisterOnSharedPreferenceChangeListener(this);
        Log.d("ONDESTROY", "DESTROY");
    }

    /**
     * Reset the GUI at workout's end.
     */
    public void flushGUI() {

        Log.d("flushh", "" + "flushGUI");
        if (pref.getInt(OPERATION_SETTING, WORKOUT_STOP) == WORKOUT_PROGRESS) {
            Log.d("flushh", "" + "workout progress");
            waveProgress.setVisibility(View.VISIBLE);
            progressTextView.setVisibility(View.VISIBLE);
            createButton.setVisibility(View.INVISIBLE);
            speedUnitSpinner.setVisibility(View.VISIBLE);
            speedView.setVisibility(View.VISIBLE);
            speedLimitView.setVisibility(View.VISIBLE);
        } else {
            waveProgress.setVisibility(View.INVISIBLE);
            progressTextView.setVisibility(View.INVISIBLE);
            createButton.setVisibility(View.VISIBLE);
            speedUnitSpinner.setVisibility(View.INVISIBLE);
            speedView.setVisibility(View.INVISIBLE);
            speedLimitView.setVisibility(View.INVISIBLE);
        }
        countdownDigit.setVisibility(View.INVISIBLE);
//        spmTextView.setText("0");
//        spmTextView.setBackgroundColor(Color.TRANSPARENT);
    }

    /**
     * Listen for preference changes from child fragments.
     * Acts as an indirect way to handle UI clicks in child fragments.
     *
     * @param sharedPreferences a SharedPreferences that was changed
     * @param s                 key for the changed preference
     */

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {

        if (s.equals(SPM_SETTING)) {
            Log.d("BENCHMARKING", "pref changed and registered");
            spm = sharedPreferences.getInt(SPM_SETTING, 22);
            spmTextView.setText(String.valueOf(spm));
        } else if (s.equals(SWITCH_SETTING)) {
            int operation = sharedPreferences.getInt(OPERATION_SETTING, 0);
            switch (operation) {
                case WORKOUT_PROGRESS: {
                    workoutRunning = WORKOUT_PROGRESS;
                    onProgressChange();
                    break;
                }
                case WORKOUT_STOP: {
                    workoutRunning = WORKOUT_STOP;
                    flushGUI();
                    break;
                }
            }
        } else if (s.equals(PHASE_PROGRESS) || s.equals(SPEED_UNIT)) {
            onProgressChange();
        } else if (s.equals(CURRENT_COLOR)) {
            //TODO show color somewhere on the UI
//            spmTextView.setBackgroundColor(sharedPreferences.getInt(CURRENT_COLOR, Color.TRANSPARENT));
        } else if (s.equals(COUNTDOWN_DURATION_LEFT)) {
            //TODO show a countdown
            createButton.setVisibility(View.INVISIBLE);
            int duration = sharedPreferences.getInt(COUNTDOWN_DURATION, 3000) / 1000;
            int durationLeft = sharedPreferences.getInt(COUNTDOWN_DURATION_LEFT, 0) / 1000;
            String countdownString = durationLeft + "";
            Log.d("TEHDURATION", durationLeft + "");
            if (durationLeft == 0) {
                countdownView.setVisibility(View.INVISIBLE);
                countdownDigit.setVisibility(View.INVISIBLE);
//                SlideFragment.lastClickedEngageButton.setBackgroundResource(R.drawable.ic_menu_play_clip_negative);
            } else {
                countdownDigit.setText(countdownString);
                countdownView.bringToFront();
//                countdownView.requestLayout();
                countdownView.setVisibility(View.VISIBLE);
                countdownDigit.setVisibility(View.VISIBLE);
                countdownDigit.bringToFront();
                Log.d("ANIMATEE", "values: " + duration + " " + durationLeft);
                if (durationLeft == duration) {
                    Log.d("ANIMATEE", "check");
                    AnimationSet animationSet = new AnimationSet(true);
                    /*Animation animationOut = AnimationUtils.loadAnimation(WaveActivity.this, R.anim.alpha_fade);
                    Animation animationIn = AnimationUtils.loadAnimation(WaveActivity.this, R.anim.alpha_fade_in);*/
                    Animation animationIn = new ScaleAnimation(0, 1, 0, 1, 20, windowHeight * 3 / 4);
                    Animation animationOut = new AlphaAnimation(1, 0);
                    animationOut.setDuration(duration * 1000 - 100);
                    animationOut.setStartOffset(100);
                    animationIn.setDuration(100);
                    animationSet.addAnimation(animationIn);
//                    animationSet.addAnimation(animationOut);
                    countdownView.startAnimation(animationSet);
                    countdownDigit.startAnimation(animationSet);
                }
            }
        } else if (s.equals(SPEED_LIMIT)) {
            String speedLimit = "" + pref.getInt(SPEED_LIMIT, 0);
            if (speedLimit.length() > 4) {
                pref.edit().putInt(SPEED_LIMIT, 9999).apply();
            }

            speedLimit = getSpeedString(speedLimit);

            Log.d("SPEED_LIMIT", speedLimit);
            speedLimitView.setText(speedLimit);
            if (speedLimitEditText != null) {
                speedLimitEditText.setText(speedLimit);
                speedLimitEditText.selectAll();
            }
            sharedPreferences.edit().putBoolean(SPEED_LIMIT_SWITCH, true).apply(); //Enable if limit changed
        } else if (s.equals(CURRENT_SPEED)) {
            if (sharedPreferences.getBoolean(SPEED_LIMIT_SWITCH, false)) {
                //TODO check if speed is below threshold, update speed alarm accordingly
            }
        } else if (s.equals(SPEED_LIMIT_SWITCH)) {
            Log.d("INACTIVATE", "check0");
            boolean isEnabled = sharedPreferences.getBoolean(SPEED_LIMIT_SWITCH, true);
            boolean isChecked = speedLimitSwitch.isChecked();
            if (isEnabled) {
                //TODO (function) Start speed alarm in BeeperTasks
            } else {
                //TODO (function) Stop speed alarm in BeeperTasks
            }
            if (isChecked != isEnabled) {
                Log.d("INACTIVATE", "check1");
                speedLimitSwitch.setChecked(isEnabled);
            }
        } else if (s.equals(THEME)) {
            int backgroundColor;
            switch (sharedPreferences.getInt(THEME, THEME_LIGHT)) {
                case THEME_DARK: {
                    backgroundColor = getResources().getColor(R.color.backgroundDark);
                    break;
                }
                default: {  //i.e. THEME_LIGHT
                    backgroundColor = getResources().getColor(R.color.backgroundLight);
                }
            }
            sharedPreferences.edit().putInt(THEME_COLOR, backgroundColor).apply();

            //TODO set backgrounds
            this.getWindow().getDecorView().setBackgroundColor(backgroundColor);
            if (slideFragment.dialGrid != null) {
                slideFragment.dialGrid.setBackgroundColor(backgroundColor);
            }
            if (slideFragment.presetRV != null) {
                slideFragment.presetRV.setBackgroundColor(backgroundColor);
            }
            if (slideFragment.historyRV != null) {
                slideFragment.historyRV.setBackgroundColor(backgroundColor);
            }
            if (slideFragment.mSlidingTabLayout != null) {
//                slideFragment.mSlidingTabLayout.setDividerColors(0xffffbb33, 0xffffbb33);
//                slideFragment.mSlidingTabLayout.set
            }
        } else if (s.equals(BEEP)) {
            Log.d("BEEP", "beep");
            spmTextView.startAnimation(AnimationUtils.loadAnimation(WaveActivity.this, R.anim.on_click));
        }
    }

    /**
     * Update GUI according to workout
     */
    private void onProgressChange() {
        if (workoutRunning == WORKOUT_STOP) {
            return;
        }
        int[] progress = new int[2];
        progress[0] = pref.getInt(PHASE_PROGRESS, 0);
        progress[1] = pref.getInt(PHASE_LENGTH, 0);

        if (progress[0] > progress[1]) {
            //Take a break :3
            return;
        }

        float currentSpeed = pref.getFloat(CURRENT_SPEED, 0);
        String rowingSpeedString;
        if (pref.getString(SPEED_UNIT, SPEED_MS).equals(SPEED_500M)) {
            float rowingSpeed = (1 / (currentSpeed)) * 500;
            if (rowingSpeed > 500) {
                rowingSpeedString = "0:00";
            } else {
                int rowingSpeedMins = (int) (rowingSpeed / 60);
                String remainder = "" + (((int) rowingSpeed) % 60);
                if (remainder.length() == 1) remainder = "0" + remainder;
                rowingSpeedString = rowingSpeedMins + ":" + remainder;
            }
        } else {
            rowingSpeedString = "" + currentSpeed;
            if (rowingSpeedString.length() >= 5) {
                rowingSpeedString = rowingSpeedString.substring(0, 4);
            }
        }

        Log.d("MATHTEST", "" + (int) (100 % 60));

        String progressViewText = progress[0] +
                " / " + progress[1]/* +
                " @ " +
                rowingSpeedString +
                "/500m"*/;
        progressTextView.setText(progressViewText);

        speedView.setText(rowingSpeedString);

        int progressPercent = (int) (((float) progress[0] / (float) progress[1]) * 100);
        waveProgress.setProgress(progressPercent);

        flushGUI();
//        speedView.setVisibility(View.VISIBLE);
//        speedUnitSpinner.setVisibility(View.VISIBLE);
//        speedLimitView.setVisibility(View.VISIBLE);
//        progressTextView.setVisibility(View.VISIBLE);
//        waveProgress.setVisibility(View.VISIBLE);
//        spmTextView.setBackgroundColor(pref.getInt(CURRENT_COLOR, Color.TRANSPARENT));
    }

    public void onSpmClicked(View view) {
//        view.performClick();
        //Bounce
        spmTextView.startAnimation(AnimationUtils.loadAnimation(WaveActivity.this, R.anim.on_click));

        //Swap background theme
        int currentTheme = (pref.getInt(THEME, THEME_DARK) == THEME_DARK) ? THEME_LIGHT : THEME_DARK;
        pref.edit().putInt(THEME, currentTheme).apply();
        Log.d("TehTheme", pref.getInt(THEME, THEME_DARK) + "");
    }

    private void incrementSpeedTrigger() {
        asyncTask = new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                //in cm
                int speedLimit;
                while (upIsTouched) {
                    speedLimit = pref.getInt(SPEED_LIMIT, 0);
                    pref.edit().putInt(SPEED_LIMIT, speedLimit + 1).apply();
                    try {
                        Thread.sleep(90);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                while (downIsTouched) {
                    speedLimit = pref.getInt(SPEED_LIMIT, 0);
                    pref.edit().putInt(SPEED_LIMIT, speedLimit - 1).apply();
                    try {
                        Thread.sleep(90);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }
        }.execute();
    }

    private void updateSpeedSwitchGUI(boolean isOn) {
        if (isOn) {
            speedLimitSwitch.setBackgroundResource(R.drawable.ic_rectangle);
            speedLimitSwitch.setTextColor(getResources().getColor(android.R.color.white));
        } else {
            speedLimitSwitch.setBackgroundResource(R.drawable.ic_rectangle2);
            speedLimitSwitch.setTextColor(getResources().getColor(R.color.grey));
        }
    }

    public String getSpeedString(String speedLimit) {
        String s1, s2, s3, s4;
        switch (speedLimit.length()) {
            case 4:
                s1 = speedLimit.substring(0, 1);
                s2 = speedLimit.substring(1, 2);
                s3 = speedLimit.substring(2, 3);
                s4 = speedLimit.substring(3);
                break;
            case 3:
                s1 = " ";
                s2 = speedLimit.substring(0, 1);
                s3 = speedLimit.substring(1, 2);
                s4 = speedLimit.substring(2, 3);
                break;
            case 2:
                s1 = " ";
                s2 = "0";
                s3 = speedLimit.substring(0, 1);
                s4 = speedLimit.substring(1, 2);
                break;
            case 1:
                s1 = " ";
                s2 = "0";
                s3 = "0";
                s4 = speedLimit.substring(0, 1);
                break;
            default:
                s1 = s2 = s3 = s4 = "0";
        }
        return s1 + s2 + "." + s3 + s4;
    }

}