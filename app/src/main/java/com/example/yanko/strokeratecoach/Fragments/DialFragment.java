package com.example.yanko.strokeratecoach.Fragments;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import com.example.yanko.strokeratecoach.R;
import com.example.yanko.strokeratecoach.Utils.DialGridAdapter;

/**
 * A simple {@link Fragment} subclass.
 */
public class DialFragment extends Fragment {

    GridView dialGrid;

    public DialFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_dial, container, false);
/*
        dialGrid = (GridView) container.findViewById(R.id.dial_grid_frag);
        dialGrid.setAdapter(new DialGridAdapter(this));*/
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        dialGrid = (GridView) view.findViewById(R.id.dial_grid_frag);
        dialGrid.setAdapter(new DialGridAdapter(view.getContext()));
    }
}
