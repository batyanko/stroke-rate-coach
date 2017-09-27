package com.batyanko.strokeratecoach.Utils;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.batyanko.strokeratecoach.R;
import com.batyanko.strokeratecoach.WaveActivity;

/**
 * Created by yanko on 4/4/17.
 */

public class DialGridAdapter extends BaseAdapter {

    private Context mContext;
    private String[] digitIds;

    public DialGridAdapter(Context c) {
        mContext = c;
        Log.d("Context in constr!!!!: ", mContext.getPackageName());

        digitIds = new String[]{
                "1", "2", "3",
                "4", "5", "6",
                "7", "8", "9",
                "0", mContext.getString(R.string.stopper_button_text), mContext.getString(R.string.speed_button_text)
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
            view = inflater.inflate(R.layout.digit_view_item, null);
            TextView text = (TextView) view.findViewById(R.id.digit_item);
            text.setText(digitIds[position]);
            if (position == 11) {
                text.setTextSize(20);
            }

//            view.getLayoutParams().width = 30;
//            view.getLayoutParams().height = 30;
//            ViewGroup.LayoutParams lp = view.getLayoutParams();
//            lp.width = 20; //WaveActivity.windowWidth / 3 - 5;
//            lp.height = 30; //(WaveActivity.windowHeight - WaveActivity.statusbarHeight) / 10 - 1;
//
            view.setLayoutParams(
                    new android.widget.AbsListView.LayoutParams(WaveActivity.windowWidth / 3 - 5,
                            (WaveActivity.windowHeight - WaveActivity.statusbarHeight) / 10 - 1
                    )
            );

//            view.requestLayout();
//            Log.d("Cell Height!!!: ", "" + MainActivity.windowHeight / 10);
        } else {
            view = convertView;
        }

        return view;
    }
}
