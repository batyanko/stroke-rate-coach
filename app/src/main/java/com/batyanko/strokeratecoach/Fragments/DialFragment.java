package com.batyanko.strokeratecoach.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.batyanko.strokeratecoach.R;
import com.batyanko.strokeratecoach.Utils.DialGridAdapter;

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

        return inflater.inflate(R.layout.fragment_dial, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        dialGrid = (GridView) view.findViewById(R.id.dial_grid_frag);
        dialGrid.setAdapter(new DialGridAdapter(view.getContext()));
    }
}
