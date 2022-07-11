package com.batyanko.strokeratecoach;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import static com.batyanko.strokeratecoach.WaveActivity.THEME_COLOR;

public class AboutActivity extends AppCompatActivity {

    private SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        pref = PreferenceManager.getDefaultSharedPreferences(this);
        this.getWindow().getDecorView().setBackgroundColor(
                pref.getInt(THEME_COLOR, getResources().getColor(R.color.backgroundLight)));
    }
}
