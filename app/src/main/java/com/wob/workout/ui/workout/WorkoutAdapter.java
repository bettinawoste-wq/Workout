package com.wob.workout.ui.workout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
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

    private void showPictureStrip(ViewHolder holder, WorkoutListItem item) {
        List<Exercise> strip = item.stripExercises();
        holder.binding.stripPictures.setVisibility(strip.isEmpty() ? View.GONE : View.VISIBLE);

        for (int slot = 0; slot < holder.stripSlots.length; slot++) {
            FramedImageView view = holder.stripSlots[slot];
            if (slot < strip.size()) {
                view.setVisibility(View.VISIBLE);
                thumbnails.show(view, strip.get(slot));
            } else {
                view.setVisibility(View.INVISIBLE);
            }
        }

        int more = item.morePictures();
        holder.binding.txtMorePictures.setVisibility(more > 0 ? View.VISIBLE : View.INVISIBLE);
        holder.binding.txtMorePictures.setText("+" + more);
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
        final FramedImageView[] stripSlots;

        ViewHolder(ItemWorkoutBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            this.stripSlots = new FramedImageView[] {
                    binding.imgStrip1,
                    binding.imgStrip2,
                    binding.imgStrip3,
                    binding.imgStrip4,
                    binding.imgStrip5
            };
        }
    }
}