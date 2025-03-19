package com.batyanko.strokeratecoach.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.batyanko.strokeratecoach.R;
import com.batyanko.strokeratecoach.SoundsActivity;
import com.batyanko.strokeratecoach.Utils.WaveUtilities;
import com.batyanko.strokeratecoach.WaveActivity;

/**
 * Created by yankog on 14.01.18.
 */

public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
        CheckBoxPreference useLocation = (CheckBoxPreference) findPreference(WaveActivity.USE_LOC_KEY);
        CheckBoxPreference useInBackground = (CheckBoxPreference) findPreference(WaveActivity.USE_BACKGROUND_KEY);


        useLocation.setOnPreferenceClickListener(preference -> {
            WaveUtilities.requestLocation(getActivity());
            return true;
        });

        useLocation.setOnPreferenceChangeListener((preference, newValue) -> {
            if ((boolean) newValue) {
                WaveUtilities.requestLocation(getActivity());
            }
            return true;
        });

        useInBackground.setOnPreferenceChangeListener((preference, newValue) -> {
            if ((boolean) newValue) {
                Log.d("PREF TEST", "NOTIF TRUE");
                WaveUtilities.requestNotifications(getActivity());
            } else {
                Log.d("PREF TEST", "NOTIF FALSE");

            }
            return true;
        });
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        String key = preference.getKey();
        if (key.equals(getString(R.string.beep_sound_screen_key))) {
            Intent intent = new Intent(getActivity(), SoundsActivity.class);
            startActivity(intent);
            return true;
        }
        return false;
    }
}