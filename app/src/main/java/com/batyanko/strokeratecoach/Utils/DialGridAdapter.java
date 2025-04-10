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

package com.batyanko.strokeratecoach.Utils;

import static android.gesture.GestureOverlayView.ORIENTATION_VERTICAL;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.annotation.ColorInt;

import com.batyanko.strokeratecoach.R;
import com.batyanko.strokeratecoach.WaveActivity;

/**
 * Created by yanko on 4/4/17.
 */

public class DialGridAdapter extends BaseAdapter {

    private Context mContext;
    private String[] digitIds;

    TypedValue typedValue;
    @ColorInt
    int color;

    public DialGridAdapter(Context c) {
        mContext = c;

        digitIds = new String[]{
                "1", "2", "3",
                "4", "5", "6",
                "7", "8", "9",
                " ", "0", " "
        };

        //Get style background color
        typedValue = new TypedValue();
        mContext.getTheme().resolveAttribute(androidx.appcompat.R.attr.colorBackgroundFloating, typedValue, true);
        color = typedValue.data;
    }

    @Override
    public int getCount() {
        return digitIds.length;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view;

        if (convertView == null) {
            LayoutInflater inflater =
                    (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.digit_view_item, null);
            TextView text = view.findViewById(R.id.digit_item);
            text.setText(digitIds[position]);
            if (position == 11) {
                text.setTextSize(20);
            }

            int cellWidthFraction;
            int cellHeightFraction;
            if (WaveActivity.orientation == ORIENTATION_VERTICAL) {
                cellWidthFraction = 3;
                cellHeightFraction = 10;
            } else {
                cellWidthFraction = 6;
                cellHeightFraction = 5;
            }
            view.setLayoutParams(
                    new android.widget.AbsListView.LayoutParams(WaveActivity.windowWidth / cellWidthFraction - 5,
                            (WaveActivity.windowHeight - WaveActivity.statusbarHeight) / cellHeightFraction - 1
                    )
            );

        } else {
            view = convertView;
        }

        return view;
    }
}
