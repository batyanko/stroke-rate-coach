package com.batyanko.strokeratecoach.Fragments;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.batyanko.strokeratecoach.R;

/**
 * Created by yankog on 14.01.18.
 */

public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
    }
}