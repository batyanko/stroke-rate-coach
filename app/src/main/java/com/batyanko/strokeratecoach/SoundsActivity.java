package com.batyanko.strokeratecoach;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.LayoutParams;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;

public class SoundsActivity extends AppCompatActivity {

    private SharedPreferences pref;
    private String sndPref;
    private boolean prefSndExists;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sounds);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        pref = PreferenceManager.getDefaultSharedPreferences(SoundsActivity.this);
        sndPref = pref.getString(getString(R.string.beep_sound_list_key), "");
        if (sndPref.isEmpty()) {
            pref.edit().putString(getString(R.string.beep_sound_list_key), getString(R.string.beep_sound_default)).apply();
            sndPref = getString(R.string.beep_sound_default);
        }

        Button confirm = findViewById(R.id.sound_confirm_button);
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pref.edit().putString(getString(R.string.beep_sound_list_key), sndPref).apply();
                startActivity(new Intent(SoundsActivity.this, WaveActivity.class));
            }
        });
        Button cancel = findViewById(R.id.sound_cancel_button);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        RadioGroup sounds_rg = findViewById(R.id.sounds_rg);

        String[] sounds;
        try {
            sounds = getAssets().list("");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        RadioButton defRb = addSndOption(getString(R.string.beep_sound_default), sounds_rg);

        if (sounds != null) {
            for (String snd : sounds) {
                if (!snd.endsWith(".wav")) {
                    continue;
                }
                addSndOption(snd.substring(0, snd.length() - 4), sounds_rg);
            }
        }

        if (!prefSndExists) {
            pref.edit().putString(getString(R.string.beep_sound_list_key), getString(R.string.beep_sound_default)).apply();
            sndPref = getString(R.string.beep_sound_default);
            defRb.toggle();
        }


    }

    private RadioButton addSndOption(String snd, RadioGroup sounds_rg) {
        RadioButton rb = new RadioButton(this);
        LayoutParams lParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        lParams.setMargins(15, 15, 15, 15);
        rb.setLayoutParams(lParams);
        rb.setText(snd);
        rb.setTextSize(getResources().getDimension(R.dimen.text_size_very_small));

        rb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sndPref = snd;

                if (snd.equals(getString(R.string.beep_sound_default))) {
                    ToneGenerator workoutToneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
                    workoutToneGen.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 150);
                } else {
                    AssetFileDescriptor afd;
                    try {
                        afd = getAssets().openFd(snd + ".wav");
                        MediaPlayer player = new MediaPlayer();
                        player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                        player.prepare();
                        player.start();
                        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                mp.seekTo(0);
                                mp.release();
                            }
                        });
                        player.setLooping(false);
                    } catch (IOException e) {
                        pref.edit().putString(getString(R.string.beep_sound_list_key), getString(R.string.beep_sound_default)).apply();
                        sndPref = getString(R.string.beep_sound_default);
                    }
                }
            }
        });
        sounds_rg.addView(rb);
        if (snd.equals(sndPref)) {
            sounds_rg.check(sounds_rg.getChildAt(sounds_rg.getChildCount() - 1).getId());
            prefSndExists = true;
        }
        return rb;
    }

}

