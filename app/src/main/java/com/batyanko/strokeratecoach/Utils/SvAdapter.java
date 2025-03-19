/*

Copyright (C) 2016 The Android Open Source Project

Modifications copyright (C) 2017 Yanko Georgiev

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
   */

package com.batyanko.strokeratecoach.Utils;

import static com.batyanko.strokeratecoach.data.WorkoutContract.WorkoutEntry1.COLUMN_NAME;
import static com.batyanko.strokeratecoach.data.WorkoutContract.WorkoutEntry1.COLUMN_SPP_TYPE;
import static com.batyanko.strokeratecoach.data.WorkoutContract.WorkoutEntry1.COLUMN_TIMESTAMP;

import android.content.Context;
import android.database.Cursor;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.recyclerview.widget.RecyclerView;

import com.batyanko.strokeratecoach.R;
import com.batyanko.strokeratecoach.WaveActivity;
import com.batyanko.strokeratecoach.data.WorkoutContract;
import com.batyanko.strokeratecoach.sync.BeeperTasks;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by ku4ekasi4ka on 8/17/17.
 */

public class SvAdapter extends RecyclerView.Adapter<SvAdapter.ExerciseViewHolder> {

    private String tableName;

    final private ListItemClickListener mOnClickListener;

    //////////////////
    //Database stuff
    private Cursor gottenCursor;
    private Context mContext;
    private boolean workoutIsRunning;
    private int workoutId;

    TypedValue typedValue;
    @ColorInt
    int color;

    public interface ListItemClickListener {
        void onListItemClick(View vie, long clickedItemIndex, int position, String tableName, Cursor cursor, int itemFunction);
    }

    /**
     * DB adapter for the preset list
     */
    public SvAdapter(Context context, Cursor cursor, String tableName, ListItemClickListener listener) {
        mContext = context;
        gottenCursor = cursor;
        this.tableName = tableName;
        mOnClickListener = listener;
        workoutIsRunning = false;

        //Get style background color
        typedValue = new TypedValue();
        mContext.getTheme().resolveAttribute(androidx.appcompat.R.attr.colorBackgroundFloating, typedValue, true);
        color = typedValue.data;
    }

    @Override
    public ExerciseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        Context context = parent.getContext();
        int layoutIdForListItem = R.layout.preset_item;
        LayoutInflater inflater = LayoutInflater.from(context); //TODO use mContext
        boolean shouldAttachToParentImmediately = false;

        View itemView = inflater.inflate(layoutIdForListItem, parent, shouldAttachToParentImmediately);
        return new ExerciseViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ExerciseViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        //SQLite stuff
        return gottenCursor.getCount();
    }

    public class ExerciseViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView workoutTitle;
        private TextView workoutTime;
        private TextView workoutDate;
        private TextView workoutDesc;
        //        private Button delButton;
        private ImageView engageButton;
        //        private final int delButtonId;
        private final int engageButtonId;


        public ExerciseViewHolder(View itemView) {
            super(itemView);

            workoutTitle = itemView.findViewById(R.id.workout_title);
            workoutTime = itemView.findViewById(R.id.workout_time);
            workoutDate = itemView.findViewById(R.id.workout_date);
            workoutDesc = itemView.findViewById(R.id.exercise_desc);
            engageButton = itemView.findViewById(R.id.engage_button);
            engageButtonId = engageButton.getId();
        }

        @Override
        public void onClick(View view) {
            long clickedTag = (long) itemView.getTag();
            int clickedPosition = getAdapterPosition();
            int viewId = view.getId();
            if (viewId == engageButtonId) {
                mOnClickListener.onListItemClick(
                        view,
                        clickedTag,
                        clickedPosition,
                        tableName,
                        gottenCursor,
                        WaveActivity.ENGAGE_WORKOUT_FUNCTION
                );
            } else {
                mOnClickListener.onListItemClick(
                        view,
                        clickedTag,
                        clickedPosition,
                        tableName,
                        gottenCursor,
                        WaveActivity.WORKOUT_ITEM_FUNCTION
                );
            }
        }

        private void bind(int position) {
            ////////////////
            //SQLite stuff
            if (!gottenCursor.moveToPosition(position))
                return;

            if (workoutIsRunning && gottenCursor
                    .getInt(gottenCursor.getColumnIndex(WorkoutContract.WorkoutEntry1._ID)) == workoutId) {
                engageButton.setBackgroundResource(R.drawable.ic_play_4_negative);

            } else {
                engageButton.setBackgroundResource(R.drawable.ic_play_4);
            }
            workoutDesc.setOnClickListener(this);
            engageButton.setOnClickListener(this);
            itemView.setOnClickListener(this);

            String sppType;

            String timestampFromSQLite = gottenCursor.getString(gottenCursor.getColumnIndex(COLUMN_TIMESTAMP));

            //TODO add setting for 12/24h format
            Locale locale = Locale.getDefault();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss", locale);
            SimpleDateFormat sdfDateTime = new SimpleDateFormat("yyyy-MM-dd',' kk:mm:ss", locale);
            SimpleDateFormat sdfDate = new SimpleDateFormat("dd MMM yyyy", locale);
            SimpleDateFormat sdfTime = new SimpleDateFormat("kk:mm:ss", locale);
            ParsePosition pp = new ParsePosition(0);
            Date dt = sdf.parse(timestampFromSQLite, pp);
            String localDate = "";
            String localTime = "";
            if (dt != null) {
                long currentEpoch = dt.getTime();

                long localOffset = TimeZone.getDefault().getOffset(currentEpoch);
                //Current local time?
                localDate = sdfDate.format(currentEpoch + localOffset);
                localTime = sdfTime.format(currentEpoch + localOffset);

                switch (gottenCursor.getString(gottenCursor.getColumnIndex(COLUMN_SPP_TYPE))) {
                    case BeeperTasks.SPP_TYPE_STROKES: {
                        sppType = "" + mContext.getString(R.string.strokes) + "";
                        break;
                    }
                    case BeeperTasks.SPP_TYPE_METERS: {
                        sppType = "" + mContext.getString(R.string.meters) + "";
                        break;
                    }
                    case BeeperTasks.SPP_TYPE_SECONDS: {
                        sppType = "" + mContext.getString(R.string.seconds) + "";
                        break;
                    }
                    default: {
                        sppType = "";
                    }
                }

            } else {
                sppType = "";
            }


            workoutDesc.setText(sppType);
            workoutTitle.setText(gottenCursor.getString(gottenCursor.getColumnIndex(COLUMN_NAME)));
            workoutTime.setText(localTime);
            workoutDate.setText(localDate);

            this.itemView.setTag(gottenCursor.getLong(gottenCursor.getColumnIndex(WorkoutContract.WorkoutEntry1._ID)));
        }

        ///////////////////
        //SQLite stuff
    }

    public void swapCursor(Cursor newCursor, boolean workoutIsRunning, int workoutId) {
        // Always close the previous gottenCursor first
        this.workoutIsRunning = workoutIsRunning;
        this.workoutId = workoutId;
        if (gottenCursor != null) gottenCursor.close();
        gottenCursor = newCursor;
        if (newCursor != null) {
            // Force the RecyclerView to refresh
            this.notifyDataSetChanged();
        }
    }
}
