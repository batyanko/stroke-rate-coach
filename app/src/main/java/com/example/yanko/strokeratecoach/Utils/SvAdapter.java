package com.example.yanko.strokeratecoach.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.yanko.strokeratecoach.MainActivity;
import com.example.yanko.strokeratecoach.R;
import com.example.yanko.strokeratecoach.WaveActivity;

import java.util.Arrays;

import static com.example.yanko.strokeratecoach.WaveActivity.exerciseG1;

/**
 * Created by ku4ekasi4ka on 8/17/17.
 */

public class SvAdapter extends RecyclerView.Adapter<SvAdapter.ExerciseViewHolder> {

    private String textHolder;



    private int mNumItems;
    final private ListItemClickListener mOnClickListener;

    private SharedPreferences sharedPreferences;


    public interface ListItemClickListener {
        void onListItemClick(int clickedItemIndex, int itemFunction);
    }

    public SvAdapter(int numItems, ListItemClickListener listener) {
        mNumItems = numItems;
        mOnClickListener = listener;
    }


    @Override
    public ExerciseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        int layoutIdForListItem = R.layout.exercise_item;
        LayoutInflater inflater = LayoutInflater.from(context);
        boolean shouldAttachToParentImmediately = false;

        View view = inflater.inflate(layoutIdForListItem, parent, shouldAttachToParentImmediately);
        ExerciseViewHolder viewHolder = new ExerciseViewHolder(view);

/*
        viewHolder.viewHolderIndex.setText("ViewHolder index: " + viewHolderCount);
        viewHolderCount++;*/

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ExerciseViewHolder holder, int position) {
        //More like holder.bind(DatabaseHelper.getExercise(exerciseID));
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return mNumItems;
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
            favButton.setBackgroundResource(R.drawable.emo_im_tongue_sticking_out);
            engageButton = (Button) itemView.findViewById(R.id.engage_button);
            engageButton.setBackgroundResource(R.drawable.ic_menu_play_clip);
            //TODO Make sure IDs are unique?
            itemId = exerciseItem.getId();
            favButtonId = favButton.getId();
            engageButtonId = engageButton.getId();
            favButton.setOnClickListener(this);
            exerciseItem.setOnClickListener(this);
            engageButton.setOnClickListener(this);

        }

        @Override
        public void onClick(View view) {
            int clickedPosition = getAdapterPosition();
            int viewId = view.getId();
            if (viewId == favButtonId) {
                mOnClickListener.onListItemClick(clickedPosition, WaveActivity.FAV_BUTTON_FUNCTION);
            } else if (viewId == engageButtonId) {
                mOnClickListener.onListItemClick(clickedPosition, WaveActivity.ENGAGE_EXERCISE_FUNCTION);
            } else if (viewId == itemId){
                mOnClickListener.onListItemClick(clickedPosition, WaveActivity.EXERCISE_ITEM_FUNCTION);
            }
            Log.d("VIEW ID: ", viewId + "");
        }

        public void bind(int position) {
//            exerciseItem.setText(String.valueOf(position));
            switch (position) {
                case 0: {
                    textHolder = Arrays.toString(WaveActivity.exerciseSPP1)
                            + " strokes at " +Arrays.toString(exerciseG1) + " spm";
                    break;
                }
                case 1: {
                    textHolder = Arrays.toString(WaveActivity.exerciseSPP2)
                            + " strokes at " +Arrays.toString(WaveActivity.exerciseG2) + " spm";
                    break;
                }
                case 2: {
                    textHolder = Arrays.toString(WaveActivity.exerciseSPP3)
                            + " strokes at " +Arrays.toString(WaveActivity.exerciseG3) + " spm";
                    break;
                }
                case 3: {
                    textHolder = Arrays.toString(WaveActivity.exerciseSPP4)
                            + " strokes at " +Arrays.toString(WaveActivity.exerciseG4) + " spm";
                    break;
                }
                default: {
                    textHolder = "Nycki";
                }

            }
            exerciseItem.setText(textHolder);
        }
    }
}
