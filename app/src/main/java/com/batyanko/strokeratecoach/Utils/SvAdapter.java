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

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.batyanko.strokeratecoach.R;
import com.batyanko.strokeratecoach.WaveActivity;
import com.batyanko.strokeratecoach.data.WorkoutContract;
import com.batyanko.strokeratecoach.sync.BeeperTasks;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static com.batyanko.strokeratecoach.data.WorkoutContract.WorkoutEntry1.COLUMN_GEARS_CSV;
import static com.batyanko.strokeratecoach.data.WorkoutContract.WorkoutEntry1.COLUMN_NAME;
import static com.batyanko.strokeratecoach.data.WorkoutContract.WorkoutEntry1.COLUMN_SPP_CSV;
import static com.batyanko.strokeratecoach.data.WorkoutContract.WorkoutEntry1.COLUMN_SPP_TYPE;
import static com.batyanko.strokeratecoach.data.WorkoutContract.WorkoutEntry1.COLUMN_TIMESTAMP;

/**
 * Created by ku4ekasi4ka on 8/17/17.
 */

public class SvAdapter extends RecyclerView.Adapter<SvAdapter.ExerciseViewHolder> {

    private String textHolder;

    private String tableName;


    private int mNumItems;
    final private ListItemClickListener mOnClickListener;

    private SharedPreferences sharedPreferences;

    //////////////////
    //Database stuff
    private Cursor gottenCursor;
    private Cursor historyCursor;
    private Context mContext;
    private boolean workoutIsRunning;
    private int workoutId;

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
    }

    /**
     * DB adapter for the history list
     */
    public SvAdapter(Context context, Cursor gottenCursor, Cursor historyCursor, ListItemClickListener listener) {
        mContext = context;
        this.gottenCursor = gottenCursor;
        this.historyCursor = historyCursor;
        mOnClickListener = listener;
    }


    @Override
    public ExerciseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        int layoutIdForListItem = R.layout.exercise_item;
        LayoutInflater inflater = LayoutInflater.from(context); //TODO use mContext
        boolean shouldAttachToParentImmediately = false;

        View view = inflater.inflate(layoutIdForListItem, parent, shouldAttachToParentImmediately);

        return new ExerciseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ExerciseViewHolder holder, int position) {
        //More like holder.bind(DatabaseHelper.getExercise(exerciseID));
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        //SQLite stuff
        return gottenCursor.getCount();
    }

    public class ExerciseViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView exerciseItem;
        private Button favButton;
        private TextView engageButton;
        private final int itemId;
        private final int favButtonId;
        private final int engageButtonId;


        public ExerciseViewHolder(View itemView) {
            super(itemView);

            exerciseItem = (TextView) itemView.findViewById(R.id.exercise_text);
            favButton = (Button) itemView.findViewById(R.id.fav_button);
            engageButton = (Button) itemView.findViewById(R.id.engage_button);
            //TODO Make sure IDs are unique?
            itemId = exerciseItem.getId();
            favButtonId = favButton.getId();
            engageButtonId = engageButton.getId();
        }

        @Override
        public void onClick(View view) {
            long clickedTag = (long) itemView.getTag();
            int clickedPosition = getAdapterPosition();
            int viewId = view.getId();
            if (historyCursor == null) {
                Log.d("TEHCURSOR", "is null");
            } else {
                Log.d("TEHCURSOR", historyCursor.toString());
            }
            if (viewId == favButtonId) {
                mOnClickListener.onListItemClick(view, clickedTag, clickedPosition, tableName, gottenCursor, WaveActivity.FAV_BUTTON_FUNCTION);
            } else if (viewId == engageButtonId) {
                mOnClickListener.onListItemClick(view, clickedTag, clickedPosition, tableName, gottenCursor, WaveActivity.ENGAGE_WORKOUT_FUNCTION);
            } else if (viewId == itemId) {
                mOnClickListener.onListItemClick(view, clickedTag, clickedPosition, tableName, gottenCursor, WaveActivity.WORKOUT_ITEM_FUNCTION);
            }
        }

        private void bind(int position) {
//            exerciseItem.setText(String.valueOf(position));

            ////////////////
            //SQLite stuff
            if (!gottenCursor.moveToPosition(position))
                return;

            Log.d("WORKOUTID", "WORKOUTID" + workoutId + "");
            Log.d("WORKOUTID", "GOTTENID" + gottenCursor.getInt(gottenCursor.getColumnIndex(WorkoutContract.WorkoutEntry1._ID)) + "");
            if (workoutIsRunning && gottenCursor
                    .getInt(gottenCursor.getColumnIndex(WorkoutContract.WorkoutEntry1._ID)) == workoutId) {
                engageButton.setBackgroundResource(R.drawable.emo_im_tongue_sticking_out);
            } else {
                engageButton.setBackgroundResource(R.drawable.ic_menu_play_clip);
            }
            favButton.setBackgroundResource(R.drawable.dialog_ic_close_focused_holo_light);
            favButton.setOnClickListener(this);
            exerciseItem.setOnClickListener(this);
            engageButton.setOnClickListener(this);

            String scrollItemName;

            String timestampFromSQLite = gottenCursor.getString(gottenCursor.getColumnIndex(COLUMN_TIMESTAMP));

            Log.d("DATE BENCH", "START");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.US);
            SimpleDateFormat sdfSimple = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            ParsePosition pp = new ParsePosition(0);
            Date dt = sdf.parse(timestampFromSQLite, pp);
            if (dt != null) {
                long currentEpoch = dt.getTime();

                long localOffset = TimeZone.getDefault().getOffset(currentEpoch);
                //Current local time?
                    String localDate = sdfSimple.format(currentEpoch + localOffset);
                    String localTime = sdf.format(currentEpoch + localOffset);
                Log.d("DATE BENCH", "FINISH");

                String sppType;
                switch (gottenCursor.getInt(gottenCursor.getColumnIndex(COLUMN_SPP_TYPE))) {
                    case BeeperTasks.SPP_TYPE_STROKES: {
                        sppType = "(" + mContext.getString(R.string.strokes)+ ") ";
                        break;
                    }
                    case BeeperTasks.SPP_TYPE_METERS: {
                        sppType = "(" + mContext.getString(R.string.meters) + ") ";
                        break;
                    }
                    case BeeperTasks.SPP_TYPE_SECONDS: {
                        sppType = "(" + mContext.getString(R.string.seconds) + " ";
                        break;
                    }
                    default: {
                        sppType = "";
                    }
                }
                String at = mContext.getString(R.string.gear_title_text) + " ";

                if (tableName.equals(WorkoutContract.WorkoutEntry1.TABLE_NAME_PRESETS)) {
                    scrollItemName = localDate +
                            ": " +
                            sppType +
                            gottenCursor.getString(gottenCursor.getColumnIndex(COLUMN_NAME)) +
                            "\n" +
                            gottenCursor.getString(gottenCursor.getColumnIndex(COLUMN_SPP_CSV)) +
                            " " +
                            at +
                            gottenCursor.getString(gottenCursor.getColumnIndex(COLUMN_GEARS_CSV));
                } else {
                    scrollItemName = localTime +
                            " " +
                            sppType +
                            "\n" +
                            gottenCursor.getString(gottenCursor.getColumnIndex(COLUMN_SPP_CSV)) +
                            " " +
                            at +
                            gottenCursor.getString(gottenCursor.getColumnIndex(COLUMN_GEARS_CSV));
                }

            } else {
                scrollItemName = gottenCursor.getString(gottenCursor.getColumnIndex(COLUMN_NAME));
            }


            exerciseItem.setText(scrollItemName);

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
