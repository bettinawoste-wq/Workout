package com.wob.workout.ui.workout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.wob.workout.R;
import com.wob.workout.data.entity.Exercise;
import com.wob.workout.data.entity.Workout;
import com.wob.workout.databinding.ItemWorkoutBinding;
import com.wob.workout.media.ExerciseThumbnails;
import com.wob.workout.media.FramedImageView;
import com.wob.workout.ui.ExerciseCount;
import java.util.ArrayList;
import java.util.List;

public class WorkoutAdapter extends RecyclerView.Adapter<WorkoutAdapter.ViewHolder> {

    public interface Listener {
        void onWorkoutClicked(Workout workout);
        void onWorkoutStartClicked(Workout workout);
    }

    private static final int PICTURES_PER_ROW = 5;

    private final Listener listener;
    private final ExerciseThumbnails thumbnails;
    private final List<WorkoutListItem> workouts = new ArrayList<>();

    public WorkoutAdapter(Listener listener, ExerciseThumbnails thumbnails) {
        this.listener = listener;
        this.thumbnails = thumbnails;
    }

    public void setData(List<WorkoutListItem> data) {
        workouts.clear();
        workouts.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemWorkoutBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        WorkoutListItem item = workouts.get(position);

        holder.binding.txtName.setText(item.workout.name);
        holder.binding.txtInfo.setText(
                ExerciseCount.text(item.exerciseCount) + " • " + formatDuration(item.totalDurationSeconds));
        showPictureStrip(holder, item);

        holder.itemView.setOnClickListener(
                v -> listener.onWorkoutClicked(item.workout));

        holder.binding.btnStart.setOnClickListener(
                v -> listener.onWorkoutStartClicked(item.workout));
    }

    /** Shows every exercise of the workout in order, in as many rows as that takes. */
    private void showPictureStrip(ViewHolder holder, WorkoutListItem item) {
        LinearLayout strip = holder.binding.stripPictures;
        List<Exercise> exercises = item.exercises;
        int rows = (exercises.size() + PICTURES_PER_ROW - 1) / PICTURES_PER_ROW;

        while (strip.getChildCount() > rows) {
            strip.removeViewAt(strip.getChildCount() - 1);
        }
        LayoutInflater inflater = LayoutInflater.from(strip.getContext());
        while (strip.getChildCount() < rows) {
            inflater.inflate(R.layout.item_workout_picture_row, strip, true);
        }

        for (int slot = 0; slot < rows * PICTURES_PER_ROW; slot++) {
            ViewGroup row = (ViewGroup) strip.getChildAt(slot / PICTURES_PER_ROW);
            ViewGroup frame = (ViewGroup) row.getChildAt(slot % PICTURES_PER_ROW);
            if (slot < exercises.size()) {
                frame.setVisibility(View.VISIBLE);
                thumbnails.show((FramedImageView) frame.getChildAt(0), exercises.get(slot));
            } else {
                frame.setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return workouts.size();
    }

    private String formatDuration(int seconds) {
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        final ItemWorkoutBinding binding;

        ViewHolder(ItemWorkoutBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}