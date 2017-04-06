package com.example.yanko.strokeratecoach.Utils;

import android.content.Context;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.yanko.strokeratecoach.MainActivity;
import com.example.yanko.strokeratecoach.R;

/**
 * Created by yanko on 4/4/17.
 */

public class TextAdapter extends BaseAdapter {

    private Context mContext;
    private String[] digitIds;

    public TextAdapter(Context c) {
        mContext = c;
        Log.d("Context in constr!!!!: ", mContext.getPackageName());

        digitIds = new String[] {
                "1", "2", "3", "4", "5", "6", "7", "8", "9", "0",
                mContext.getString(R.string.stopper_button_text), mContext.getString(R.string.speed_button_text)
        };
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

        Log.d("Context in getView: ", mContext.getPackageName());

        TextView textView;

        if (convertView == null) {
            textView = new TextView(mContext);
            textView.setTextSize(20);
            textView.setGravity(1);
        } else {
            textView = (TextView) convertView;
        }

        /*
        String text;
        if (digitIds[position + 1] < 10) {
            text = String.valueOf(digitIds[position + 1]);
        } else {
            text = "0";
        }*/

        textView.setText(digitIds[position]);

        return textView;
    }


}
