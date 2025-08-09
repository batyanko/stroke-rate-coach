package com.batyanko.strokeratecoach;

import static com.batyanko.strokeratecoach.WaveActivity.THEME;
import static com.batyanko.strokeratecoach.WaveActivity.THEME_COLOR;
import static com.batyanko.strokeratecoach.WaveActivity.THEME_LIGHT;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.batyanko.strokeratecoach.Fragments.SettingsFragment;
import com.batyanko.strokeratecoach.Utils.WaveUtilities;

public class SettingsActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(this.getWindow().getDecorView(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(32, systemBars.top, 32, systemBars.bottom);
            return insets;
        });

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
        } else if (key.equals(WaveActivity.USE_LOC_KEY)) {
            WaveUtilities.requestLocation(this);
        } else if (key.equals(WaveActivity.USE_BACKGROUND_KEY)) {
            WaveUtilities.requestNotifications(this);
        }
    }
}
