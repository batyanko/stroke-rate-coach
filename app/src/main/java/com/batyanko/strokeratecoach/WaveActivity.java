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

import static android.content.res.Configuration.ORIENTATION_PORTRAIT;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.FragmentTransaction;

import com.batyanko.strokeratecoach.Fragments.SlideFragment;
import com.batyanko.strokeratecoach.Utils.SpeedViewAdapter;
import com.batyanko.strokeratecoach.Utils.WaveUtilities;
import com.batyanko.strokeratecoach.sync.BeeperService;
import com.batyanko.strokeratecoach.sync.BeeperServiceUtils;
import com.batyanko.strokeratecoach.sync.BeeperTasks;

import java.util.Timer;
import java.util.TimerTask;

public class WaveActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener, PopupMenu.OnMenuItemClickListener {

    //Constants for OnSharedPreferenceChangeListener
    public static final String OPERATION_SETTING = "operation";
    public static final String SWITCH_SETTING = "switch";
    public static final String SPM_SETTING = "spm";

    //Workout types
    public static final int WORKOUT_INTERVAL = 2;
    public static final int WORKOUT_SIMPLE = 3;
    public static final int WORKOUT_LAST = 9;
    public static final int WORKOUT_STOP = 0;

    //Workout item function IDs
    public static int WORKOUT_ITEM_FUNCTION = 2;
    public static int ENGAGE_WORKOUT_FUNCTION = 3;

    public static final int MY_LOCATION_PERMISSION = 22;
    public static final int NOTIFICATION_PERMISSION = 33;
    public static final String LATEST_VERSION_KEY = "latest-version";

    //Shared preferences
    public static final String NOT_AGREED = "not-agreed";

    //TODO use from R.string
    public static final String USE_LOC_KEY = "use-location";
    public static final String USE_BACKGROUND_KEY = "use-in-background";

    public static final String PHASE_LENGTH = "phase-length";
    public static final String WORKOUT_LENGTH = "workout-length";
    public static final String PHASE_PROGRESS = "phase-strokes-elapsed";
    public static final String WORKOUT_PROGRESS = "total-strokes-elapsed";
    public static final String CURRENT_COLOR = "current-phase";
    public static final String CURRENT_SPEED = "current-speed";
    public static final String SPEED_LIMIT = "speed-limit";
    public static final String SPEED_LIMIT_SWITCH = "speed-limit-switch";
    public static final String SPEED_UNIT = "speed-unit";
    public static final String SPEED_MS_SETTING = "m/s";
    public static final String SPEED_500M_SETTING = "/500m";
    public static final String COUNTDOWN_DURATION = "countdown-duration";   //In ms
    public static final String COUNTDOWN_DURATION_LEFT = "countdown-duration-left";   //In ms
    public static final String BEEP = "beep";
    public static final String THEME_COLOR = "theme-color";
    public static final String WARN = "teh-warn";
    public static final String GPS_LOCKING = "gps-lock";
    public static final String LOCATION_ACCURACY = "loc-accuracy";
    public static final boolean THEME_LIGHT = false;
    public static final boolean THEME_DARK = true;
    public static final String THEME = "theme";
    public static final String LOCATION_ACCURACY_ACCEPTABLE = "starting-loc-accuracy";
    public static final String CUSTOM_SOUND = "custom-sound";
    public static final String SPEED_SAMPLE_COUNT = "speed-sample-count";

    public static final String LAST_PRESET_SETTING = "last-preset-item";
    public static final String LAST_HISTORY_SETTING = "last-history-item";
    public static final String LAST_TRASH_SETTING = "last-trash-item";
    /* Global spm setting to hold current spm */
    public static int spm;

    //UI elements
    private View progressFrameLayout;
    private ProgressBar waveProgress;
    private PopupMenu popupMenu;
    private ImageView countdownView;
    private static boolean countdownBeingAnimated;
    private TextView spmTextView;
    private TextView progressTextView;

    private TextView countdownDigit;

    private TextView speedView;
    private TextView speedLimitView;
    private Spinner speedUnitSpinner;
    private TextView stopperButton;

    private static Animation beeperAnimation;
    public static Animation warningAnimation;

    private View legendStrip;
    private ImageView speedUnitLegend;
    private TextView speedSpeedLegend;
    private TextView speedLimitLegend;

    private SharedPreferences pref;

    //autoWave values
    private static int workoutRunning = WORKOUT_STOP;

    public static int windowWidth;
    public static int windowHeight;
    public static int statusbarHeight;


    private SlideFragment slideFragment;

    //SpeedLimit popup UI
    private EditText speedLimitEditText;
    private TextView speedLimitTv500m;
    private View speedLimitPopupLayout;
    private PopupWindow speedPopupWindow;
    private SwitchCompat speedLimitSwitch;

    private InputMethodManager imm;

    private static AsyncTask asyncTask = null;
    boolean upIsTouched = false;
    boolean downIsTouched = false;
    private View gpsSplashLayout;
    private TextView gpsSplashText;
    private ImageView gpsLocatorImage;

    private static Timer timer;
    private static TimerTask gpsTask;
    private static Runnable gpsSplashRunnable;

    private TextView menuTextView;

    private static ConstraintSet constraintSet;
    private static ViewGroup viewGroup;
    private static ViewGroup speedStrip;

    public static int orientation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_wave);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //Initialize spm at last setting, or default at 0
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        pref.registerOnSharedPreferenceChangeListener(this);

        countdownView = findViewById(R.id.countdown_image_view);
        countdownView.setVisibility(View.INVISIBLE);

        // Avoid transparent click through countdownView
        countdownView.setOnClickListener(v -> countdownView.setVisibility(View.INVISIBLE));

        countdownDigit = findViewById(R.id.countdown_digit);
        countdownDigit.setVisibility(View.INVISIBLE);

        viewGroup = findViewById(R.id.activity_wave);
        speedStrip = findViewById(R.id.speed_strip);
        final LayoutInflater inflater = (LayoutInflater) WaveActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        orientation = getResources().getConfiguration().orientation;

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        slideFragment = new SlideFragment();
        transaction.replace(R.id.slide_frame_layout, slideFragment);
        //TODO add to back stack?
        transaction.commit();

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        windowWidth = metrics.widthPixels;
        windowHeight = metrics.heightPixels;

        if (orientation == ORIENTATION_PORTRAIT) {
            constraintSet = new ConstraintSet();
            constraintSet.clone((ConstraintLayout) viewGroup);
        }

        spm = pref.getInt(SPM_SETTING, 0);

        //Initialize UI elements
        this.getWindow().getDecorView().setBackgroundColor(
                pref.getInt(THEME_COLOR, getResources().getColor(R.color.backgroundLight)));

        progressFrameLayout = findViewById(R.id.progress_frame_layout);
        waveProgress = findViewById(R.id.wave_progress_bar);
        waveProgress.setVisibility(View.INVISIBLE);

        spmTextView = findViewById(R.id.spm_text_view);
        spmTextView.setText(String.valueOf(spm));
        progressTextView = findViewById(R.id.progressTextView);

        legendStrip = findViewById(R.id.legend_strip);

        stopperButton = findViewById(R.id.stop_button);
        stopperButton.setOnClickListener(view -> {
            slideFragment.stopBeeper();
            flushGUI();
        });

        String[] speeds = {SPEED_MS_SETTING, SPEED_500M_SETTING};
        String initSpeedUnit = pref.getString(SPEED_UNIT, SPEED_MS_SETTING);
        speedUnitSpinner = findViewById(R.id.speed_unit);
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
                    speedView.setText("0.00");
                } else {
                    onSpeedChange(pref);
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

        speedView = findViewById(R.id.speed_view);
        speedView.setOnClickListener(view -> speedUnitSpinner.performClick());
        speedUnitLegend = findViewById(R.id.speed_unit_legend);
        speedUnitLegend.setOnClickListener(view -> speedUnitSpinner.performClick());
        speedSpeedLegend = findViewById(R.id.speed_speed_legend);
        speedSpeedLegend.setOnClickListener(view -> speedUnitSpinner.performClick());
        speedLimitLegend = findViewById(R.id.speed_limit_legend);
        speedLimitLegend.setOnClickListener(view -> speedLimitView.performClick());


        imm = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);

        speedLimitView = findViewById(R.id.speed_limit_view);
        updateSpeedUnit();
        speedLimitView.setOnClickListener(view -> {
            float density = WaveActivity.this.getResources().getDisplayMetrics().density;
            speedLimitPopupLayout = inflater.inflate(R.layout.speed_limit_layout, null);

            speedPopupWindow = new PopupWindow(speedLimitPopupLayout, windowWidth, LinearLayout.LayoutParams.WRAP_CONTENT, true);
            speedPopupWindow.setBackgroundDrawable(
                    new ColorDrawable(pref.getInt(THEME_COLOR, getResources().getColor(R.color.backgroundLight))));

            speedPopupWindow.setElevation(100f);

            speedPopupWindow.showAsDropDown(speedLimitView);
            speedPopupWindow.showAtLocation(speedLimitPopupLayout, Gravity.CENTER, 0, (int) density * 100);

            speedLimitTv500m = speedLimitPopupLayout.findViewById(R.id.speed_limit_tv_500m);
            int speedLimit = pref.getInt(SPEED_LIMIT, 0);
            speedLimitTv500m.setText(getSpeedPer500(((float) speedLimit) / 100));

            speedLimitSwitch =
                    speedLimitPopupLayout.findViewById(R.id.speed_limit_switch);
            speedLimitEditText = speedLimitPopupLayout.findViewById(R.id.speed_limit_edit_text);
            speedLimitEditText.setOnEditorActionListener((textView, actionId, keyEvent) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    updateSpeedLimitPref();
                    speedPopupWindow.dismiss();
                    return true;
                }
                return false;
            });
            speedLimitEditText.setOnFocusChangeListener((view1, b) -> {
                imm.showSoftInput(view1, InputMethodManager.SHOW_IMPLICIT);
                speedLimitEditText.post(() -> imm.showSoftInput(speedLimitEditText, InputMethodManager.SHOW_IMPLICIT));
            });

            final Button setLimit = speedLimitPopupLayout.findViewById(R.id
                    .speed_limit_setter_button);
            final ImageView up = speedLimitPopupLayout.findViewById(R.id.increase_speed);
            final ImageView down = speedLimitPopupLayout.findViewById(R.id.decrease_speed);

            String speedLimitString = "" + pref.getInt(SPEED_LIMIT, 0);
            speedLimitString = getSpeedString(speedLimitString);
            speedLimitEditText.setText(speedLimitString);
            speedLimitEditText.selectAll();

            setLimit.setOnClickListener(view2 -> {
                updateSpeedLimitPref();
                speedPopupWindow.dismiss();
            });

            speedLimitSwitch.setOnCheckedChangeListener((compoundButton, isOn) -> {
                pref.edit().putBoolean(SPEED_LIMIT_SWITCH, isOn).apply();
                updateSpeedSwitchGUI(isOn);
                if (isOn) {
                    imm.showSoftInput(speedLimitEditText, InputMethodManager.SHOW_IMPLICIT);
                }

            });
            boolean switchOn = pref.getBoolean(SPEED_LIMIT_SWITCH, false);
            speedLimitSwitch.setChecked(switchOn);
            updateSpeedSwitchGUI(switchOn);

            up.setOnTouchListener((view3, motionEvent) -> {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    upIsTouched = true;
                    incrementSpeedTrigger();
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    upIsTouched = false;
                }
                return true;
            });
            down.setOnTouchListener((view4, motionEvent) -> {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    downIsTouched = true;
                    incrementSpeedTrigger();
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    downIsTouched = false;
                }
                return true;
            });
        });

        beeperAnimation = AnimationUtils.loadAnimation(WaveActivity.this, R.anim.on_click);
        warningAnimation = AnimationUtils.loadAnimation(WaveActivity.this, R.anim.on_warn);

        int resource = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resource > 0) {
            statusbarHeight = getResources().getDimensionPixelSize(resource);
        }

        gpsSplashLayout = findViewById(R.id.gps_splash_layout);
        gpsLocatorImage = findViewById(R.id.gps_locator_image);
        gpsSplashText = findViewById(R.id.gps_splash_text);
        gpsSplashLayout.setVisibility(View.INVISIBLE);
        gpsLocatorImage.setVisibility(View.INVISIBLE);
        gpsSplashText.setVisibility(View.INVISIBLE);

        gpsSplashRunnable = () -> gpsLocatorImage.startAnimation(getGpsAnimationSet());

        gpsSplashLayout.setOnClickListener(view -> BeeperTasks.completeLocking());


        menuTextView = findViewById(R.id.menu_image_view);
        popupMenu = new PopupMenu(WaveActivity.this, menuTextView);
        popupMenu.inflate(R.menu.menu_options);
        popupMenu.setOnMenuItemClickListener(WaveActivity.this);
        menuTextView.setOnClickListener(view -> popupMenu.show());

        //Bind to service if already running (in case of screen rotation / onDestroy)

        updateSpeedLimitView(pref.getBoolean(SPEED_LIMIT_SWITCH, false));
        firstRunInit();

        //Assuming no running workout at onCreate()
        if (!BeeperServiceUtils.serviceIsRunning()) {
            pref.edit().putInt(OPERATION_SETTING, WORKOUT_STOP).apply();
        }
        flushGUI();
    }

    @Override
    protected void onStart() {
        super.onStart();
        onProgressChange();
    }

    @Override
    protected void onResume() {
        super.onResume();

        onThemeChange(pref);

        if (pref.getInt(OPERATION_SETTING, WORKOUT_STOP) == WORKOUT_INTERVAL) {
            //Bind to service if a workout is running
            Intent intent = new Intent(this, BeeperService.class);
            intent.setAction(BeeperTasks.ACTION_JUST_BIND);
            BeeperServiceUtils.doBindService(intent, this, BeeperServiceUtils.getServiceConnection());
        }

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
        if (!pref.getBoolean(USE_BACKGROUND_KEY, false)) {
            slideFragment.stopBeeper();
        }
        flushGUI();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (BeeperServiceUtils.serviceIsRunning()) {
            BeeperServiceUtils.doUnbindService(this);
        }
    }

    /**
     * Reset the GUI at workout's end.
     */
    public void flushGUI() {
        switch (pref.getInt(OPERATION_SETTING, WORKOUT_STOP)) {
            case WORKOUT_STOP: {
                pref.edit().putBoolean(GPS_LOCKING, false).apply();
                if (orientation == ORIENTATION_PORTRAIT) {
                    constraintSet.connect(R.id.spm_text_view, ConstraintSet.TOP, R.id.activity_wave, ConstraintSet.TOP);
                    constraintSet.connect(R.id.spm_text_view, ConstraintSet.BOTTOM, R.id.guideline_hor50, ConstraintSet.BOTTOM);
                    constraintSet.applyTo((ConstraintLayout) viewGroup);
                }
                progressFrameLayout.setVisibility(View.GONE);
                speedStrip.setVisibility(View.GONE);
                legendStrip.setVisibility(View.GONE);
                countdownView.setVisibility(View.GONE);
                menuTextView.setVisibility(View.VISIBLE);
                break;
            }
            case WORKOUT_SIMPLE: {
                if (orientation == ORIENTATION_PORTRAIT) {
                    constraintSet.connect(R.id.legend_strip, ConstraintSet.TOP, R.id.activity_wave, ConstraintSet.TOP);
                    constraintSet.connect(R.id.speed_strip, ConstraintSet.TOP, R.id.legend_strip, ConstraintSet.BOTTOM);
                    constraintSet.connect(R.id.spm_text_view, ConstraintSet.TOP, R.id.speed_strip, ConstraintSet.BOTTOM);
                    constraintSet.applyTo((ConstraintLayout) viewGroup);
                    speedStrip.bringToFront();
                }
                pref.edit().putBoolean(GPS_LOCKING, false).apply();
                progressFrameLayout.setVisibility(View.GONE);
                speedStrip.setVisibility(View.VISIBLE);
                legendStrip.setVisibility(View.VISIBLE);
                countdownView.setVisibility(View.INVISIBLE);
                menuTextView.setVisibility(View.INVISIBLE);

                break;
            }
            //Interval workout
            default: {
                if (orientation == ORIENTATION_PORTRAIT) {
                    constraintSet.connect(R.id.spm_text_view, ConstraintSet.TOP, R.id.speed_strip, ConstraintSet.BOTTOM);
                    constraintSet.connect(R.id.legend_strip, ConstraintSet.TOP, R.id.progress_frame_layout, ConstraintSet.BOTTOM);
                    constraintSet.connect(R.id.spm_text_view, ConstraintSet.BOTTOM, R.id.guideline_hor50, ConstraintSet.BOTTOM);
                    constraintSet.applyTo((ConstraintLayout) viewGroup);
                    speedStrip.bringToFront();
                }
                progressFrameLayout.setVisibility(View.VISIBLE);
                waveProgress.setVisibility(View.VISIBLE);
                progressTextView.setVisibility(View.VISIBLE);
                speedStrip.setVisibility(View.VISIBLE);
                legendStrip.setVisibility(View.VISIBLE);
                countdownView.setVisibility(View.INVISIBLE);
                menuTextView.setVisibility(View.INVISIBLE);

            }
        }
        updateGpsSplash();
        countdownDigit.setVisibility(View.INVISIBLE);
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
            spm = sharedPreferences.getInt(SPM_SETTING, 22);
            spmTextView.setText(String.valueOf(spm));
        } else if (s.equals(SWITCH_SETTING)) {
            int operation = sharedPreferences.getInt(OPERATION_SETTING, WORKOUT_STOP);
            switch (operation) {
                case WORKOUT_INTERVAL: {
                    workoutRunning = WORKOUT_INTERVAL;
                    flushGUI();
                    Log.d("PROGRESS", "onSwitch Interval");
                    onProgressChange();
                    break;
                }
                case WORKOUT_LAST: {
                    workoutRunning = WORKOUT_LAST;
                    flushGUI();
                    Log.d("PROGRESS", "onSwitch Last");
                    onProgressChange();
                    break;
                }
                case WORKOUT_SIMPLE: {
                    workoutRunning = WORKOUT_SIMPLE;
                    flushGUI();
                    Log.d("PROGRESS", "onSwitch Simple");
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
            onProgressChange();
            //TODO update speed limit if necessary
        } else if (s.equals(SPEED_UNIT)) {
            updateSpeedUnit();
        } else if (s.equals(CURRENT_COLOR)) {
            //TODO show color somewhere on the UI
        } else if (s.equals(COUNTDOWN_DURATION_LEFT)) {
            int duration = sharedPreferences.getInt(COUNTDOWN_DURATION, 3000) / 1000;
            int durationLeft = sharedPreferences.getInt(COUNTDOWN_DURATION_LEFT, 0) / 1000;
            String countdownString = durationLeft + "";
            if (durationLeft == 0) {
                countdownView.setVisibility(View.INVISIBLE);
                countdownDigit.setVisibility(View.INVISIBLE);
            } else {
                countdownDigit.setText(countdownString);
                countdownView.bringToFront();
                countdownView.setVisibility(View.VISIBLE);
                countdownDigit.setVisibility(View.VISIBLE);
                countdownDigit.bringToFront();
                if (durationLeft == duration) {
                    AnimationSet animationSet = new AnimationSet(true);
                    Animation animationIn = new ScaleAnimation(0, 1, 0, 1, 20, (float) (windowHeight * 3) / 4);
                    Animation animationOut = new AlphaAnimation(1, 0);
                    animationOut.setDuration(duration * 1000 - 100);
                    animationOut.setStartOffset(100);
                    animationIn.setDuration(100);
                    animationSet.addAnimation(animationIn);
                    countdownView.startAnimation(animationSet);
                    countdownDigit.startAnimation(animationSet);
                }
            }
        } else if (s.equals(GPS_LOCKING)) {
            updateGpsSplash();
        } else if (s.equals(LOCATION_ACCURACY)) {
            if (pref.getBoolean(GPS_LOCKING, false)) {
                updateGpsSplashText();
            }
        } else if (s.equals(SPEED_LIMIT)) {
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
            onSpeedChange(sharedPreferences);
        } else if (s.equals(SPEED_LIMIT_SWITCH)) {
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

            onThemeChange(sharedPreferences);
        } else if (s.equals(BEEP)) {
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

    }

    private void onSpeedChange(SharedPreferences sharedPreferences) {
        boolean limitOn = sharedPreferences.getBoolean(SPEED_LIMIT_SWITCH, false);
        float speed = sharedPreferences.getFloat(CURRENT_SPEED, 0f);
        int speedLimit = sharedPreferences.getInt(SPEED_LIMIT, 0);

        if (limitOn && speedLimit != 0 && speed != 0) {
            setSpeedWarning(limitOn, speed, speedLimit);
        }

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
        //Bounce
        spmTextView.startAnimation(AnimationUtils.loadAnimation(WaveActivity.this, R.anim.on_click));

        //Swap background theme
        boolean darkEnabled = (pref.getBoolean(THEME, THEME_LIGHT)) ? THEME_LIGHT : THEME_DARK;
        pref.edit().putBoolean(THEME, darkEnabled).apply();
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
            speedLimitSwitch.setBackgroundResource(R.color.blueAppColor);
            speedLimitSwitch.setTextColor(getResources().getColor(android.R.color.white));
        } else {
            speedLimitSwitch.setBackgroundResource(R.color.greyLight);
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
        onSpeedChange(pref);
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
        float threshold = ((float) speedLimit) / 100;
        String warningAction = (switchOn && speed < threshold)
                ? BeeperTasks.ACTION_START_WARNING : BeeperTasks.ACTION_STOP_WARNING;
        Intent intent = new Intent(this, BeeperService.class);
        intent.setAction(warningAction);
        BeeperService service = BeeperServiceUtils.getBeeperService();
        if (service != null) {
            service.modWorkout(intent);
        }
    }

    private void animateSplash(long duration) {

        //first run splash handled at firstRunInit()
        if (pref.getBoolean(NOT_AGREED, true)) return;

        countdownView.setVisibility(View.VISIBLE);
        countdownView.setAlpha(1f);
        countdownView.bringToFront();

        countdownBeingAnimated = true;
        countdownView.animate().alpha(0f).setDuration(duration).withEndAction(
                () -> {
                    countdownView.setAlpha(1f);
                    countdownView.setVisibility(View.INVISIBLE);
                    countdownBeingAnimated = false;
                }
        );
    }

    private void updateGpsSplash() {
        if (pref.getBoolean(GPS_LOCKING, false)) {
            gpsSplashLayout.setVisibility(View.VISIBLE);
            gpsLocatorImage.setVisibility(View.VISIBLE);
            gpsSplashText.setVisibility(View.VISIBLE);

            updateGpsSplashText();

            gpsTask = new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(gpsSplashRunnable);
                }
            };
            timer = new Timer();
            timer.scheduleAtFixedRate(gpsTask, 1, 6050);
            gpsSplashLayout.bringToFront();
        } else {
            if (gpsTask != null) gpsTask.cancel();
            if (timer != null) timer.cancel();
            gpsLocatorImage.clearAnimation();
            gpsSplashLayout.setVisibility(View.INVISIBLE);
            gpsLocatorImage.setVisibility(View.INVISIBLE);
            gpsSplashText.setVisibility(View.INVISIBLE);
        }
    }

    private AnimationSet getGpsAnimationSet() {
        Animation animation1 = new TranslateAnimation(000f, -100f, 000f, 200f);
        Animation animation2 = new TranslateAnimation(0f, 100f, 0f, 200f);
        Animation animation3 = new TranslateAnimation(0f, -100f, 0f, 000f);
        Animation animation4 = new TranslateAnimation(0f, -100f, 0f, -300f);
        Animation animation5 = new TranslateAnimation(0f, 200f, -0f, -300f);
        Animation animation6 = new TranslateAnimation(0, 0, 0f, 200);
        animation1.setDuration(1000);
        animation2.setDuration(1000);
        animation2.setStartOffset(1000);
        animation3.setDuration(1000);
        animation3.setStartOffset(2000);
        animation4.setDuration(1000);
        animation4.setStartOffset(3000);
        animation5.setDuration(1000);
        animation5.setStartOffset(4000);
        animation6.setDuration(1000);
        animation6.setStartOffset(5000);
        final AnimationSet gpsSet = new AnimationSet(false);
        gpsSet.addAnimation(animation1);
        gpsSet.addAnimation(animation2);
        gpsSet.addAnimation(animation3);
        gpsSet.addAnimation(animation4);
        gpsSet.addAnimation(animation5);
        gpsSet.addAnimation(animation6);

        return gpsSet;
    }

    private void updateGpsSplashText() {
        String string = getString(R.string.gps_lock_text) + "\n"
                + pref.getFloat(LOCATION_ACCURACY, 0f)
                + " > " + BeeperTasks.acceptableAccuracy;
        gpsSplashText.setText(string);
    }

    private void onThemeChange(SharedPreferences sharedPreferences) {
        int backgroundColor = (pref.getBoolean(THEME, THEME_LIGHT)) ?
                getResources().getColor(R.color.backgroundDark)
                :
                getResources().getColor(R.color.backgroundLight);

        sharedPreferences.edit().putInt(THEME_COLOR, backgroundColor).apply();
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
        if (slideFragment.trashRV != null) {
            slideFragment.trashRV.setBackgroundColor(backgroundColor);
        }
        if (slideFragment.mSlidingTabLayout != null) {
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.item_add_workout: {
                Intent intent = new Intent(WaveActivity.this, EntryFormActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.menu_item_backup: {
                Intent intent = new Intent(WaveActivity.this, BackupActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.menu_item_settings: {
                Intent intent = new Intent(WaveActivity.this, SettingsActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.menu_item_help: {
                Intent intent = new Intent(WaveActivity.this, HelpActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.menu_item_about: {
                Intent intent = new Intent(WaveActivity.this, AboutActivity.class);
                startActivity(intent);
                return true;
            }
            default:
                return false;
        }
    }

    @Override
    public void onBackPressed() {
        if (pref.getBoolean(GPS_LOCKING, true)) {
            slideFragment.stopBeeper();
            flushGUI();
        } else {
            super.onBackPressed();
        }
    }

    private void firstRunInit() {
        // The whole "Welcome"...
        if (pref.getBoolean(NOT_AGREED, true)) {

            final LayoutInflater inflater = (LayoutInflater) WaveActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View welcomeLayout = inflater.inflate(R.layout.welcome_layout, null);

            final PopupWindow welcomePopupWindow = new PopupWindow(welcomeLayout, windowWidth, LinearLayout.LayoutParams.WRAP_CONTENT, true);
            welcomePopupWindow.setBackgroundDrawable(
                    new ColorDrawable(pref.getInt(THEME_COLOR, getResources().getColor(R.color.backgroundLight))));

            welcomePopupWindow.setElevation(100f);

            findViewById(R.id.activity_wave).post(() -> {
                countdownView.setAlpha(1f);
                countdownView.setVisibility(View.VISIBLE);
                welcomePopupWindow.showAtLocation(menuTextView, Gravity.CENTER, 0, 100);
            });

            Button agreeButton = welcomeLayout.findViewById(R.id.button_agree);
            Button disagreeButton = welcomeLayout.findViewById(R.id.button_disagree);

            agreeButton.setOnClickListener(view -> {
                pref.edit().putBoolean(NOT_AGREED, false).apply();
                welcomePopupWindow.dismiss();
            });
            disagreeButton.setOnClickListener(view -> {
                pref.edit().putBoolean(NOT_AGREED, true).apply();
                welcomePopupWindow.dismiss();
            });

            welcomePopupWindow.setOnDismissListener(() -> {
                if (pref.getBoolean(NOT_AGREED, true)) {
                    WaveActivity.this.finishAffinity();
                } else {
                    LocPopup(inflater);
                }
            });

            pref.edit().putInt(WaveActivity.OPERATION_SETTING, WORKOUT_STOP).apply();
            PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

            bumpLatestVer();

            //... or handle current version:
            // 1.20 Get notification permission
        } else {
            String whatsNew = "";
            if (pref.getInt(LATEST_VERSION_KEY, 0) < 121) {
                whatsNew += this.getResources().getString(R.string.whatsnew_121);
            }
            if (pref.getInt(LATEST_VERSION_KEY, 0) < 120) {
                whatsNew += this.getResources().getString(R.string.whatsnew_120);
            }
            if (!whatsNew.isEmpty()){
                whatsNew = this.getResources().getString(R.string.whatsnew_title) + whatsNew;

                final LayoutInflater inflater = (LayoutInflater) WaveActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View whatsNewLayout = inflater.inflate(R.layout.whatsnew_layout, null);
                TextView whatsNewTv = whatsNewLayout.findViewById(R.id.whatsnew_textview);
                whatsNewTv.setText(whatsNew);

                final PopupWindow whatsnewPopupWindow = new PopupWindow(whatsNewLayout, windowWidth, LinearLayout.LayoutParams.WRAP_CONTENT, true);
                whatsnewPopupWindow.setBackgroundDrawable(
                        new ColorDrawable(pref.getInt(THEME_COLOR, getResources().getColor(R.color.backgroundLight))));

                whatsnewPopupWindow.setElevation(100f);

                findViewById(R.id.activity_wave).post(() -> {
                    countdownView.setAlpha(1f);
                    countdownView.setVisibility(View.VISIBLE);
                    whatsnewPopupWindow.showAtLocation(menuTextView, Gravity.CENTER, 0, 100);
                });

                whatsNewLayout.setOnClickListener(view -> whatsnewPopupWindow.dismiss());

                if (pref.getInt(LATEST_VERSION_KEY, 0) < 120) {
                    whatsnewPopupWindow.setOnDismissListener(() -> notifPopup(inflater));
                } else {
                    whatsnewPopupWindow.setOnDismissListener(() -> animateSplash(3000));
                }
                bumpLatestVer();
            }
        }
    }

    private void bumpLatestVer() {
        // Save latest version for comparison with next update
        PackageInfo pInfo;
        try {
            pInfo = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            // Here be more robust handling ;)
            return;
        }
        int verCode = pInfo.versionCode;
        pref.edit().putInt(LATEST_VERSION_KEY, verCode).apply();
    }

    public void LocPopup(LayoutInflater inflater) {
        View locationLayout = inflater.inflate(R.layout.location_layout, null);

        final PopupWindow locPopupWindow = new PopupWindow(locationLayout, windowWidth, LinearLayout.LayoutParams.WRAP_CONTENT, true);
        locPopupWindow.setBackgroundDrawable(
                new ColorDrawable(pref.getInt(THEME_COLOR, getResources().getColor(R.color.backgroundLight))));

        locPopupWindow.setElevation(100f);

        findViewById(R.id.activity_wave).post(() -> {
            countdownView.setAlpha(1f);
            countdownView.setVisibility(View.VISIBLE);
            locPopupWindow.showAtLocation(menuTextView, Gravity.CENTER, 0, 100);
        });

        Button agreeButton = locationLayout.findViewById(R.id.button_use_loc);
        Button disagreeButton = locationLayout.findViewById(R.id.button_no_loc);

        agreeButton.setOnClickListener(view -> {
            pref.edit().putBoolean(USE_LOC_KEY, true).apply();
            locPopupWindow.dismiss();
            WaveUtilities.requestLocation(WaveActivity.this);
        });
        disagreeButton.setOnClickListener(view -> {
            pref.edit().putBoolean(USE_LOC_KEY, false).apply();
            locPopupWindow.dismiss();
        });
        locPopupWindow.setOnDismissListener(() -> notifPopup(inflater));
    }

    private void notifPopup(LayoutInflater inflater) {
        View notifLayout = inflater.inflate(R.layout.notif_layout, null);

        final PopupWindow notifPopupWindow = new PopupWindow(notifLayout, windowWidth, LinearLayout.LayoutParams.WRAP_CONTENT, true);
        notifPopupWindow.setBackgroundDrawable(
                new ColorDrawable(pref.getInt(THEME_COLOR, getResources().getColor(R.color.backgroundLight))));

        notifPopupWindow.setElevation(100f);

        findViewById(R.id.activity_wave).post(() -> {
            countdownView.setAlpha(1f);
            countdownView.setVisibility(View.VISIBLE);
            notifPopupWindow.showAtLocation(menuTextView, Gravity.CENTER, 0, 100);
        });

        Button yesButton = notifLayout.findViewById(R.id.button_background_yes);
        Button noButton = notifLayout.findViewById(R.id.button_background_no);

        yesButton.setOnClickListener(view -> {
            pref.edit().putBoolean(getString(R.string.notification_preference_key), true).apply();
            notifPopupWindow.dismiss();
            WaveUtilities.requestNotifications(WaveActivity.this);
        });
        noButton.setOnClickListener(view -> {
            pref.edit().putBoolean(getString(R.string.notification_preference_key), false).apply();
            notifPopupWindow.dismiss();
        });

        notifPopupWindow.setOnDismissListener(() -> animateSplash(3000));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        slideFragment.startBeeper();
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}