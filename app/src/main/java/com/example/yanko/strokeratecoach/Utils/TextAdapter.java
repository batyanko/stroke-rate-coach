package com.example.yanko.strokeratecoach.Utils;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
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

        digitIds = new String[]{
                "1", "2", "3",
                "4", "5", "6",
                "7", "8", "9",
                "0",
                mContext.getString(R.string.stopper_button_text),
                mContext.getString(R.string.speed_button_text)
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

        View view;

        if (convertView == null) {
            LayoutInflater inflater =
                    (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.text_view_item, null);
            TextView text = (TextView) view.findViewById(R.id.digit_item);
            text.setText(digitIds[position]);

            view.setLayoutParams(
                    new ViewGroup.LayoutParams(MainActivity.windowWidth / 3 - 5,
                            (MainActivity.windowHeight - MainActivity.statusbarHeight) / 10 - 1
                    )
            );
            Log.d("Cell Height!!!: ", "" + MainActivity.windowHeight / 10);
        } else {
            view = convertView;
        }

        return view;
    }
}
