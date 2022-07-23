package com.batyanko.strokeratecoach;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import com.batyanko.strokeratecoach.Fragments.SettingsFragment;
import com.batyanko.strokeratecoach.Utils.WaveUtilities;

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
        Log.d("!!PREFERENCE CHANGED!!!", key);
        if (key.equals(WaveActivity.THEME)) {
            int backgroundColor = (pref.getBoolean(THEME, THEME_LIGHT)) ?
                    getResources().getColor(R.color.backgroundDark)
                    :
                    getResources().getColor(R.color.backgroundLight);

            this.getWindow().getDecorView().setBackgroundColor(backgroundColor);
        } else if (key.equals(WaveActivity.REQUEST_LOC)){
            WaveUtilities.requestLocation(this);
        }
    }
}
