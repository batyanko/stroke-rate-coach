package com.batyanko.strokeratecoach.Fragments;


import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.batyanko.strokeratecoach.R;


/**
 * A simple {@link Fragment} subclass.
 */
// TODO Delete, history and presets fragment?
public class PresetsFragment extends Fragment {


    public PresetsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // TODO swapped presets<>history layouts?
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

}
