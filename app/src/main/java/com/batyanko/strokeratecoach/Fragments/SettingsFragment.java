package com.batyanko.strokeratecoach.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
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
//        Preference filePicker = findPreference("filePicker");
//        SwitchPreference customSoundSwitch = (SwitchPreference) findPreference(getActivity().getString(R.string.custom_sound_switch_key));
        ListPreference soundPicker = (ListPreference) findPreference(getActivity().getString(R.string.beep_sound_list_key));
//        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());


        useLocation.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                WaveUtilities.requestLocation(getActivity());
                return true;
            }
        });

        useLocation.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((boolean) newValue) {
                    WaveUtilities.requestLocation(getActivity());
                }
                return true;
            }
        });

        useInBackground.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((boolean) newValue) {
                    Log.d("PREF TEST", "NOTIF TRUE");
                    WaveUtilities.requestNotifications(getActivity());
                } else {
                    Log.d("PREF TEST", "NOTIF FALSE");

                }
                return true;
            }
        });

//        filePicker.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
//            @Override
//            public boolean onPreferenceClick(Preference preference) {
//
//                return true;
//                // Got this from
//                // https://riptutorial.com/android/example/14425/showing-a-file-chooser-and-reading-the-result
//                Intent intent;
//                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
//                    intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
//                } else {
//                intent = new Intent(Intent.ACTION_GET_CONTENT);
//                }
//
//                // Update with mime types
//                intent.setType("audio/*");
//
//                // Update with additional mime types here using a String[].
////                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
//
//                // Only pick openable and local files. Theoretically we could pull files from google drive
//                // or other applications that have networked files, but that's unnecessary for this example.
//                intent.addCategory(Intent.CATEGORY_OPENABLE);
////                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
//
//                // REQUEST_CODE = <some-integer>
//                startActivityForResult(intent, 123);
//            }
//        });
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
        //        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
//    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        Log.d("OAR CHECK", "BOOOOO");
//
//        if (requestCode != 123) {
//            return;
//        }
//
//        if (data != null) {
//            String uri = data.getData().toString();
//
//            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
//            pref.edit().putString(CUSTOM_SOUND_PATH, uri).apply();
//            Log.d("FILE URI STUFFS", uri);
//            Log.d("FILE URI IN PREF", pref.getString(CUSTOM_SOUND_PATH, ""));
//        }
//        super.onActivityResult(requestCode, resultCode, data);
//    }
}