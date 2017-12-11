package com.batyanko.strokeratecoach.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.batyanko.strokeratecoach.R;
import com.batyanko.strokeratecoach.WaveActivity;

/**
 * Created by batyanko on 10/16/17.
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
        view = inflater.inflate(viewResource, parent, false);

        text = contentString[position];

        TextView textView = view.findViewById(R.id.spinner_tv);
        textView.setText(text);
//        textView.setTextSize(20);
        return textView;
    }
}
