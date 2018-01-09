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
import android.media.AudioManager;
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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
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
    public static final int WORKOUT_WAVE = 1;   //Depreciated
    public static final int WORKOUT_INTERVAL = 2;
    public static final int WORKOUT_SIMPLE = 3;
    public static final int WORKOUT_LAST = 9;
    public static final int WORKOUT_STOP = 0;

    //Workout item function IDs
    public static final int DEL_BUTTON_FUNCTION = 1;
    public static final int WORKOUT_ITEM_FUNCTION = 2;
    public static final int ENGAGE_WORKOUT_FUNCTION = 3;

    public static final int MY_LOCATION_PERMISSION = 22;

    //Shared preferences
    public static final String FIRST_RUN = "first-run";
    public static final String PHASE_LENGTH = "phase-length";
    public static final String WORKOUT_LENGTH = "workout-length";
    public static final String PHASE_PROGRESS = "phase-strokes-elapsed";
    public static final String WORKOUT_PROGRESS = "total-strokes-elapsed";
    public static final String CURRENT_COLOR = "current-phase";
    public static final String CURRENT_SPEED = "current-speed";
    public static final String CURRENT_SPEED_TIMESTAMP = "current-speed-timestamp";
    public static final String SPEED_LIMIT = "speed-limit";
    public static final String SPEED_LIMIT_SWITCH = "speed-limit-switch";
    public static final String SPEED_UNIT = "speed-unit";
    public static final String SPEED_MS_SETTING = "m/s";
    public static final String SPEED_500M_SETTING = "/500m";
    public static final String COUNTDOWN_RUNNING = "countdown-running";
    public static final String COUNTDOWN_DURATION = "countdown-duration";   //In ms
    public static final String COUNTDOWN_DURATION_LEFT = "countdown-duration-left";   //In ms
    public static final String BEEP = "beep";
    public static final String THEME = "theme";
    public static final int THEME_DARK = 0;
    public static final int THEME_LIGHT = 1;
    public static final String THEME_COLOR = "theme-color";
    public static final String DAT_HASH = "dat-hash";
    public static final String WARN = "teh-warn";


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
    private TextView stopperButton;
    private View speedUnitStack;
    private View speedLimitStack;

    private static Animation beeperAnimation;
    public static Animation warningAnimation;

    private View legendStrip;
    private ImageView speedUnitLegend;
    private TextView speedSpeedLegend;
    private TextView speedLimitLegend;

    private ImageView water;

    private SharedPreferences pref;

    //autoWave values
    private static int workoutRunning = WORKOUT_STOP;

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
    private TextView speedLimitTv500m;
    private View speedLimitPopupLayout;
    private PopupWindow popupWindow;
    private SwitchCompat speedLimitSwitch;

    private InputMethodManager imm;

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

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

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

        firstRunInit();

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

        legendStrip = findViewById(R.id.legend_strip);

        stopperButton = (TextView) findViewById(R.id.stop_button);
        stopperButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                slideFragment.stopBeeper();
                flushGUI();
            }
        });

        String[] speeds = {SPEED_MS_SETTING, SPEED_500M_SETTING};
        String initSpeedUnit = pref.getString(SPEED_UNIT, SPEED_MS_SETTING);
        speedUnitSpinner = (Spinner) findViewById(R.id.speed_unit);
        final SpeedViewAdapter myAdapter = new SpeedViewAdapter(this, R.layout.spinner_tv, speeds);

        AdapterView.OnItemSelectedListener spinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                String string = myAdapter.getItem(position);
                if (string != null && (string.equals(SPEED_MS_SETTING) || string.equals(SPEED_500M_SETTING))) {
                    pref.edit().putString(SPEED_UNIT, string).apply();
                }
                if (pref.getInt(OPERATION_SETTING, WORKOUT_STOP) == WORKOUT_STOP) {
                    //Don't show outdated speed, especially onCreate...
                    Log.d("SpeedViewText", "true" + pref.getInt(OPERATION_SETTING, WORKOUT_STOP));
                    speedView.setText("0.00");
                } else {
                    Log.d("SpeedViewText", "false" + pref.getInt(OPERATION_SETTING, WORKOUT_STOP));
                    onSpeedChange();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }

        };
        speedUnitSpinner.setAdapter(myAdapter);
        speedUnitSpinner.setOnItemSelectedListener(spinnerListener);

        speedUnitSpinner.setSelection(
                (initSpeedUnit.equals(SPEED_MS_SETTING) ? 0 : 1)
        );

        speedView = (TextView) findViewById(R.id.speed_view);
        speedView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                speedUnitSpinner.performClick();
            }
        });
        speedUnitLegend = findViewById(R.id.speed_unit_legend);
        speedUnitLegend.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                speedUnitSpinner.performClick();
            }
        });
        speedSpeedLegend = findViewById(R.id.speed_speed_legend);
        speedSpeedLegend.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                speedUnitSpinner.performClick();
            }
        });
        speedLimitLegend = findViewById(R.id.speed_limit_legend);
        speedLimitLegend.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                speedLimitView.performClick();
            }
        });


        imm = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);

        speedLimitStack = findViewById(R.id.speed_limit_stack);
        speedUnitStack = findViewById(R.id.speed_unit_stack);

        speedLimitView = (TextView) findViewById(R.id.speed_limit_view);
        updateSpeedUnit();
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
                } else {
                    int backgroundColor = (pref.getInt(THEME, THEME_LIGHT) == THEME_DARK) ?
                            getResources().getColor(R.color.backgroundLight)
                            :
                            getResources().getColor(R.color.backgroundDark);
                    popupWindow.setBackgroundDrawable(
                            new ColorDrawable(backgroundColor)
                    );
                }


                popupWindow.showAsDropDown(speedLimitView);
                popupWindow.showAtLocation(speedLimitPopupLayout, Gravity.CENTER, 0, (int) density * 100);

                speedLimitTv500m = speedLimitPopupLayout.findViewById(R.id.speed_limit_tv_500m);
                int speedLimit = pref.getInt(SPEED_LIMIT, 0);
                speedLimitTv500m.setText(getSpeedPer500(((float) speedLimit) / 100));

                speedLimitSwitch =
                        speedLimitPopupLayout.findViewById(R.id.speed_limit_switch);
                speedLimitEditText = speedLimitPopupLayout.findViewById(R.id.speed_limit_edit_text);
                speedLimitEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            Log.d("TehDone", "woohoo");
                            updateSpeedLimitPref();
                            popupWindow.dismiss();
                            return true;
                        }
                        return false;
                    }
                });
                speedLimitEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View view, boolean b) {
                        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
                        speedLimitEditText.post(new Runnable() {
                            @Override
                            public void run() {
                                imm.showSoftInput(speedLimitEditText, InputMethodManager.SHOW_IMPLICIT);
                            }
                        });
                    }
                });

                //                speedLimitEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
//
//                    @Override
//                    public void onFocusChange(View view, boolean b) {
//                        popupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
//                    }
//                });
                final Button setLimit = speedLimitPopupLayout.findViewById(R.id
                        .speed_limit_setter_button);
//                final Button saveAndExit = speedLimitPopupLayout.findViewById(R.id
//                        .speed_limit_confirm_button);
                final ImageView up = speedLimitPopupLayout.findViewById(R.id.increase_speed);
                final ImageView down = speedLimitPopupLayout.findViewById(R.id.decrease_speed);

                String speedLimitString = "" + pref.getInt(SPEED_LIMIT, 0);
                speedLimitString = getSpeedString(speedLimitString);
                speedLimitEditText.setText(speedLimitString);
                speedLimitEditText.selectAll();

                setLimit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        updateSpeedLimitPref();
                        popupWindow.dismiss();
                    }
                });

                speedLimitSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean isOn) {
                        pref.edit().putBoolean(SPEED_LIMIT_SWITCH, isOn).apply();
                        updateSpeedSwitchGUI(isOn);
                        if (isOn) {
                            imm.showSoftInput(speedLimitEditText, InputMethodManager.SHOW_IMPLICIT);
                        }

                    }
                });
                boolean switchOn = pref.getBoolean(SPEED_LIMIT_SWITCH, false);
                speedLimitSwitch.setChecked(switchOn);
                updateSpeedSwitchGUI(switchOn);

                //                speedLimitEditText.requestFocus();
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


//                saveAndExit.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View view) {
//                        popupWindow.dismiss();
//                    }
//                });
            }
        });

        beeperAnimation = AnimationUtils.loadAnimation(WaveActivity.this, R.anim.on_click);
        warningAnimation = AnimationUtils.loadAnimation(WaveActivity.this, R.anim.on_warn);

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

        updateSpeedLimitView(pref.getBoolean(SPEED_LIMIT_SWITCH, false));
        flushGUI();

        Log.d("OperationAtStartup", "" + pref.getInt(OPERATION_SETTING, 99));

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

        if (pref.getInt(OPERATION_SETTING, WORKOUT_STOP) == WORKOUT_INTERVAL) {
            //Bind to service if a workout is running
            Intent intent = new Intent(this, BeeperService.class);
            intent.setAction(BeeperTasks.ACTION_JUST_BIND);
            BeeperServiceUtils.doBindService(intent, this, BeeperServiceUtils.getServiceConnection());
        }

        Log.d("BENCHMARKING", "8");
        pref.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        pref.unregisterOnSharedPreferenceChangeListener(this);
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
//        pref.unregisterOnSharedPreferenceChangeListener(this);
        Log.d("ONDESTROY", "DESTROY");
    }

    /**
     * Reset the GUI at workout's end.
     */
    public void flushGUI() {

        Log.d("flushh", "" + "flushGUI");
        switch (pref.getInt(OPERATION_SETTING, WORKOUT_STOP)) {
            case WORKOUT_STOP: {
                waveProgress.setVisibility(View.INVISIBLE);
                progressTextView.setVisibility(View.INVISIBLE);
                createButton.setVisibility(View.VISIBLE);
                speedUnitStack.setVisibility(View.INVISIBLE);
                speedView.setVisibility(View.INVISIBLE);
                speedLimitStack.setVisibility(View.INVISIBLE);
                stopperButton.setVisibility(View.INVISIBLE);
                legendStrip.setVisibility(View.INVISIBLE);
                break;
            }
            case WORKOUT_SIMPLE: {
                waveProgress.setVisibility(View.INVISIBLE);
                progressTextView.setVisibility(View.INVISIBLE);
                createButton.setVisibility(View.INVISIBLE);
                speedUnitStack.setVisibility(View.VISIBLE);
                speedView.setVisibility(View.VISIBLE);
                speedLimitStack.setVisibility(View.VISIBLE);
                stopperButton.setVisibility(View.VISIBLE);
                legendStrip.setVisibility(View.VISIBLE);
                break;
            }
            //Interval workout
            default: {
                Log.d("flushh", "" + "workout progress");
                waveProgress.setVisibility(View.VISIBLE);
                progressTextView.setVisibility(View.VISIBLE);
                createButton.setVisibility(View.INVISIBLE);
                speedUnitStack.setVisibility(View.VISIBLE);
                speedView.setVisibility(View.VISIBLE);
                speedLimitStack.setVisibility(View.VISIBLE);
                stopperButton.setVisibility(View.VISIBLE);
                legendStrip.setVisibility(View.VISIBLE);
            }
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
            Log.d("onPrefChange", "spmSetting");
            Log.d("BENCHMARKING", "pref changed and registered");
            spm = sharedPreferences.getInt(SPM_SETTING, 22);
            spmTextView.setText(String.valueOf(spm));
        } else if (s.equals(SWITCH_SETTING)) {
            int operation = sharedPreferences.getInt(OPERATION_SETTING, WORKOUT_STOP);
            Log.d("onPrefChange", "switchSetting: " + operation);
            switch (operation) {
                case WORKOUT_INTERVAL: {
                    workoutRunning = WORKOUT_INTERVAL;
                    flushGUI();
                    onProgressChange();
                    break;
                }
                case WORKOUT_LAST: {
                    workoutRunning = WORKOUT_LAST;
                    flushGUI();
                    onProgressChange();
                    break;
                }
                case WORKOUT_SIMPLE: {
                    workoutRunning = WORKOUT_SIMPLE;
                    flushGUI();
                    onProgressChange();
                    break;
                }
                case WORKOUT_STOP: {
                    workoutRunning = WORKOUT_STOP;
                    flushGUI();
                    break;
                }
            }
        } else if (s.equals(PHASE_PROGRESS)) {
            Log.d("onPrefChangeProgress", "DatProgress: " + sharedPreferences.getInt(PHASE_PROGRESS, 0));
            onProgressChange();
            //TODO update speed limit if necessary
        } else if (s.equals(SPEED_UNIT)) {
            updateSpeedUnit();
        } else if (s.equals(CURRENT_COLOR)) {
            Log.d("onPrefChange", "currentColor");
            //TODO show color somewhere on the UI
//            spmTextView.setBackgroundColor(sharedPreferences.getInt(CURRENT_COLOR, Color.TRANSPARENT));
        } else if (s.equals(COUNTDOWN_DURATION_LEFT)) {
            Log.d("onPrefChange", "countdownDuration");
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
            Log.d("onPrefChange", "speedLimit");
            boolean limitOn = sharedPreferences.getBoolean(SPEED_LIMIT_SWITCH, false);
            float speed = sharedPreferences.getFloat(CURRENT_SPEED, 0f);
            int speedLimit = sharedPreferences.getInt(SPEED_LIMIT, 0);
            String speedLimitString500m = getSpeedPer500(((float) speedLimit) / 100);
            String speedLimitStringMps;
            speedLimitStringMps = "" + speedLimit;
            //Handle high speeds
            if (speedLimitStringMps.length() > 4) {
                pref.edit().putInt(SPEED_LIMIT, 9999).apply();
            }
            //Put the comma :-]
            speedLimitStringMps = getSpeedString(speedLimitStringMps);
            //Set speedLimitView in m/s or /500m accordingly
            if (pref.getString(SPEED_UNIT, SPEED_MS_SETTING).equals(SPEED_MS_SETTING)) {
                speedLimitView.setText(speedLimitStringMps);
            } else {
                speedLimitView.setText(speedLimitString500m);
            }
            //Update Speed Limit Popup
            if (speedLimitEditText != null && speedLimitTv500m != null) {
                speedLimitEditText.setText(speedLimitStringMps);
                speedLimitEditText.selectAll();
                speedLimitTv500m.setText(speedLimitString500m);
            }
            if (speedLimit != 0 && !limitOn) {
                sharedPreferences.edit().putBoolean(SPEED_LIMIT_SWITCH, true).apply(); //Enable if limit changed
            }
            if (speedLimit != 0 && speed != 0) {
                setSpeedWarning(limitOn, speed, speedLimit);
            }
        } else if (s.equals(CURRENT_SPEED)) {
            Log.d("onPrefChange", "currentSpeed");
            boolean limitOn = sharedPreferences.getBoolean(SPEED_LIMIT_SWITCH, false);
            float speed = sharedPreferences.getFloat(CURRENT_SPEED, 0f);
            int speedLimit = sharedPreferences.getInt(SPEED_LIMIT, 0);

            Log.d("onSetSpeedWarning", "limitOn / speed / speedLimit: " + limitOn + " / " + speed + " / " + speedLimit);
            if (limitOn && speedLimit != 0 && speed != 0) {
                setSpeedWarning(limitOn, speed, speedLimit);
            }
            onSpeedChange();
        } else if (s.equals(SPEED_LIMIT_SWITCH)) {
            Log.d("onPrefChange", "speedLimitSwitch");
            boolean limitOn = sharedPreferences.getBoolean(SPEED_LIMIT_SWITCH, false);
            float speed = sharedPreferences.getFloat(CURRENT_SPEED, 0f);
            int speedLimit = sharedPreferences.getInt(SPEED_LIMIT, 0);
            boolean isChecked = speedLimitSwitch.isChecked();
            setSpeedWarning(limitOn, speed, speedLimit);
            if (isChecked != limitOn) {
                speedLimitSwitch.setChecked(limitOn);
            }
            updateSpeedLimitView(limitOn);

        } else if (s.equals(THEME)) {
            Log.d("onPrefChange", "themeChange");

            int backgroundColor = (pref.getInt(THEME, THEME_LIGHT) == THEME_DARK) ?
                    getResources().getColor(R.color.backgroundDark)
                    :
                    getResources().getColor(R.color.backgroundLight);

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
            Log.d("onPrefChange", "onBeep");
            Log.d("BEEP", "beep");
            spmTextView.startAnimation(beeperAnimation);
        } else if (s.equals(WARN)) {
            speedView.startAnimation(warningAnimation);
            speedLimitView.startAnimation(warningAnimation);
        }
    }

    /**
     * Update GUI according to workout
     */
    private void onProgressChange() {
        if (workoutRunning != WORKOUT_INTERVAL && workoutRunning != WORKOUT_LAST) return;

        int[] phaseProgress = new int[2];    //0 = current phaseProgress 1 = total length
        phaseProgress[0] = pref.getInt(PHASE_PROGRESS, 0);
        phaseProgress[1] = pref.getInt(PHASE_LENGTH, 0);

        if (phaseProgress[0] > phaseProgress[1]) {
            //Take a break :3
            return;
        }

        int[] workoutProgress = new int[2];    //0 = current phaseProgress 1 = total length
        workoutProgress[0] = pref.getInt(WORKOUT_PROGRESS, 0);
        workoutProgress[1] = pref.getInt(WORKOUT_LENGTH, 0);
        Log.d("workoutProgress", phaseProgress[0] + ": "
                + workoutProgress[0] + " / " + workoutProgress[1]);

        String progressViewText = phaseProgress[0] +
                " / " + phaseProgress[1]/* +
                " @ " +
                rowingSpeedString +
                "/500m"*/;
        progressTextView.setText(progressViewText);

        int phaseProgressPercent = (int) (((float) phaseProgress[0] / (float) phaseProgress[1]) * 100);
        waveProgress.setProgress(phaseProgressPercent);
        int workoutProgressPercent = (int) (((float) workoutProgress[0] / (float) workoutProgress[1]) * 100);
        waveProgress.setSecondaryProgress(workoutProgressPercent);

//        spmTextView.setBackgroundColor(pref.getInt(CURRENT_COLOR, Color.TRANSPARENT));
    }

    private void onSpeedChange() {
        float currentSpeed = pref.getFloat(CURRENT_SPEED, 0);
        String rowingSpeedString;
        if (pref.getString(SPEED_UNIT, SPEED_MS_SETTING).equals(SPEED_500M_SETTING)) {
            rowingSpeedString = getSpeedPer500(currentSpeed);
        } else {
            rowingSpeedString = "" + currentSpeed;
            if (rowingSpeedString.length() >= 5) {
                rowingSpeedString = rowingSpeedString.substring(0, 4);
            }
        }
        speedView.setText(rowingSpeedString);
    }

    private String getSpeedPer500(float speedInMs) {
        String rowingSpeedString;
        float rowingSpeed = (1 / (speedInMs)) * 500;     //Seconds per 500m
        if (rowingSpeed > 500) {
            rowingSpeedString = ">8:00";
        } else {
            int rowingSpeedMins = (int) (rowingSpeed / 60);
            String remainder = "" + (((int) rowingSpeed) % 60);
            if (remainder.length() == 1) remainder = "0" + remainder;
            rowingSpeedString = rowingSpeedMins + ":" + remainder;
        }
        return rowingSpeedString;
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

    private void updateSpeedLimitPref() {
        float speedLimitFloat = Float.parseFloat(speedLimitEditText.getText().toString());
        //Integer representation used for string parsing
        pref.edit().putInt(SPEED_LIMIT, (int) (speedLimitFloat * 100)).apply();
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

    private void updateSpeedUnit() {
        int speedLimit = pref.getInt(SPEED_LIMIT, 0);
        String speedLimitString;
        if (pref.getString(SPEED_UNIT, SPEED_MS_SETTING).equals(SPEED_MS_SETTING)) {
            speedLimitString = "" + speedLimit;
            if (speedLimitString.length() > 4) {
                pref.edit().putInt(SPEED_LIMIT, 9999).apply();
            }
            speedLimitString = getSpeedString(speedLimitString);
        } else {
            speedLimitString = getSpeedPer500(((float) speedLimit) / 100);
        }
        speedLimitView.setText(speedLimitString);
        onSpeedChange();
    }

    private void updateSpeedLimitView(boolean limitOn) {
        if (limitOn) {
            speedLimitView.setTextColor((getResources().getColor(R.color.blueAppColor)));
        } else {
            speedLimitView.setTextColor((getResources().getColor(R.color.grey)));
        }
    }

    private void setSpeedWarning(boolean switchOn, float speed, int speedLimit) {
        //TODO get float from speedLimit
        Log.d("WarningAction", "" + switchOn);
        float threshold = ((float) speedLimit) / 100;
        Log.d("onSetSpeedWarning", "speed/threshold: " + speed + " / " + threshold);
        String warningAction = (switchOn && speed < threshold)
                ? BeeperTasks.ACTION_START_WARNING : BeeperTasks.ACTION_STOP_WARNING;
        Intent intent = new Intent(this, BeeperService.class);
        intent.setAction(warningAction);
        BeeperService service = BeeperServiceUtils.getBeeperService();
        if (service != null) {
            service.doEpicShit(intent);
        }
    }

    private void firstRunInit() {
        if (pref.getBoolean(FIRST_RUN, true)) {

            pref.edit().putInt(WaveActivity.OPERATION_SETTING, WORKOUT_STOP).apply();

            pref.edit().putBoolean(FIRST_RUN, false).apply();
        }
    }
}