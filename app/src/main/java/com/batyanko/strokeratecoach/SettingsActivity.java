package com.batyanko.strokeratecoach;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import com.batyanko.strokeratecoach.Fragments.SettingsFragment;

import static com.batyanko.strokeratecoach.WaveActivity.THEME;
import static com.batyanko.strokeratecoach.WaveActivity.THEME_COLOR;
import static com.batyanko.strokeratecoach.WaveActivity.THEME_LIGHT;

public class SettingsActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pref = PreferenceManager.getDefaultSharedPreferences(this);
        pref.registerOnSharedPreferenceChangeListener(this);

        this.getWindow().getDecorView().setBackgroundColor(
                pref.getInt(THEME_COLOR, getResources().getColor(R.color.backgroundLight)));

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pref.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(WaveActivity.THEME)) {
            int backgroundColor = (pref.getBoolean(THEME, THEME_LIGHT)) ?
                    getResources().getColor(R.color.backgroundDark)
                    :
                    getResources().getColor(R.color.backgroundLight);

            this.getWindow().getDecorView().setBackgroundColor(backgroundColor);
        }
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        Log.d("OAR CHECK", "BOOOOO");
//
//        if (requestCode != 123 || resultCode != RESULT_OK) {
//            return;
//        }
//
//        Uri stuffs;
//        if (data != null) {
//            stuffs = data.getData();
//            Log.d("FILE URI STUFFS", stuffs.getPath());
//        }
//
//
//        super.onActivityResult(requestCode, resultCode, data);
//    }
}
