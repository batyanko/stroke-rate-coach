package com.batyanko.strokeratecoach.Utils;

import static com.batyanko.strokeratecoach.WaveActivity.THEME_COLOR;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.batyanko.strokeratecoach.R;
/*
 * Copyright (C) 2018 Yanko Georgiev
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

public class SpeedViewAdapter extends ArrayAdapter<String> implements SpinnerAdapter {
    private Context context;
    private String[] contentString;
    private int viewResource;

    private SharedPreferences pref;

    LayoutInflater inflater;
    View view;
    String text;

    public SpeedViewAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull String[] objects) {
        super(context, resource, objects);
        this.context = context;
        this.contentString = objects;
        this.viewResource = resource;

        pref = PreferenceManager.getDefaultSharedPreferences(context);



    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        inflater = LayoutInflater.from(context);
        view = inflater.inflate(viewResource, parent, false);

        text = contentString[position];

        TextView textView = view.findViewById(R.id.spinner_tv);
        textView.setText(text);
//        textView.setTextSize(20);
        return textView;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        inflater = LayoutInflater.from(context);
        int color = pref.getInt(THEME_COLOR, parent.getResources().getColor(R.color.backgroundLight));
        view = inflater.inflate(viewResource, parent, false);

        text = contentString[position];

//        parent.setBackgroundResource(R.color.colorAccent);
        TextView textView = view.findViewById(R.id.spinner_tv);
        textView.setText(text);

        textView.setBackgroundDrawable(new ColorDrawable(color));
        textView.setTextSize(parent.getResources().getDimension(R.dimen.speed_strip_text_size)
        / parent.getResources().getDisplayMetrics().density);
        return textView;
    }


}
