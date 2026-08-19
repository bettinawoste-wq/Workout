package com.wob.workout.ui.workout;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.wob.workout.data.entity.Workout;
import com.wob.workout.databinding.ItemWorkoutBinding;
import java.util.ArrayList;
import java.util.List;

public class WorkoutAdapter extends RecyclerView.Adapter<WorkoutAdapter.ViewHolder> {

    public interface Listener {
        void onWorkoutClicked(Workout workout);
        void onWorkoutStartClicked(Workout workout);
    }

    private final Listener listener;
    private final List<WorkoutListItem> workouts = new ArrayList<>();

    public WorkoutAdapter(Listener listener) {
        this.listener = listener;
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
                item.exerciseCount + " Übungen • " + formatDuration(item.totalDurationSeconds));

        holder.itemView.setOnClickListener(
                v -> listener.onWorkoutClicked(item.workout));

        holder.binding.btnStart.setOnClickListener(
                v -> listener.onWorkoutStartClicked(item.workout));
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