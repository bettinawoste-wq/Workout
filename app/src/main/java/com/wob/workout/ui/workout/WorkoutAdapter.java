package com.wob.workout.ui.workout;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.wob.workout.data.entity.Workout;
import com.wob.workout.databinding.ItemWorkoutBinding;
import com.wob.workout.media.ExerciseThumbnails;
import com.wob.workout.ui.ExerciseCount;
import java.util.ArrayList;
import java.util.List;

public class WorkoutAdapter extends RecyclerView.Adapter<WorkoutAdapter.ViewHolder> {

    public interface Listener {
        void onWorkoutClicked(Workout workout);
        void onWorkoutStartClicked(Workout workout);
    }

    private final Listener listener;
    private final ExerciseThumbnails thumbnails;
    /** All cards' strips show the same kind of item, so they can share the views. */
    private final RecyclerView.RecycledViewPool stripPool = new RecyclerView.RecycledViewPool();
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
        ItemWorkoutBinding binding = ItemWorkoutBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false);
        binding.stripPictures.setRecycledViewPool(stripPool);
        PictureStripAdapter strip = new PictureStripAdapter(thumbnails);
        binding.stripPictures.setAdapter(strip);
        return new ViewHolder(binding, strip);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        WorkoutListItem item = workouts.get(position);

        holder.binding.txtName.setText(item.workout.name);
        holder.binding.txtInfo.setText(
                ExerciseCount.text(item.exerciseCount)
                        + " • " + formatDuration(item.totalDurationSeconds));
        showPictureStrip(holder, item);

        holder.itemView.setOnClickListener(
                v -> listener.onWorkoutClicked(item.workout));

        holder.binding.btnStart.setOnClickListener(
                v -> listener.onWorkoutStartClicked(item.workout));
    }

    /** Shows every exercise of the workout in order, in one row that scrolls sideways. */
    private void showPictureStrip(ViewHolder holder, WorkoutListItem item) {
        holder.strip.setData(item.exercises, () -> listener.onWorkoutClicked(item.workout));
        if (holder.shownWorkoutId != item.workout.id) {
            holder.shownWorkoutId = item.workout.id;
            holder.binding.stripPictures.scrollToPosition(0);
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
        final PictureStripAdapter strip;
        /** Which workout the strip's scroll position belongs to. */
        long shownWorkoutId = -1;

        ViewHolder(ItemWorkoutBinding binding, PictureStripAdapter strip) {
            super(binding.getRoot());
            this.binding = binding;
            this.strip = strip;
        }
    }
}