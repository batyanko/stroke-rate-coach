package com.batyanko.strokeratecoach.Fragments;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.batyanko.strokeratecoach.R;


/**
 * A simple {@link Fragment} subclass.
 */
public class HistoryFragment extends Fragment {


    public HistoryFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // TODO swapped presets<>history layouts?
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_presets, container, false);
    }

}
