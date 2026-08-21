package com.wob.workout.ui.workout;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.wob.workout.data.entity.Exercise;
import com.wob.workout.data.entity.WorkoutExercise;
import com.wob.workout.databinding.ItemWorkoutExerciseBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WorkoutExerciseAdapter
        extends RecyclerView.Adapter<WorkoutExerciseAdapter.ViewHolder> {

    public static class ExerciseItem {
        public final Exercise exercise;
        public final WorkoutExercise workoutExercise;

        public ExerciseItem(Exercise exercise, WorkoutExercise workoutExercise) {
            this.exercise = exercise;
            this.workoutExercise = workoutExercise;
        }
    }

    public interface Listener {
        void onItemClicked(ExerciseItem item);
    }

    private final List<ExerciseItem> items = new ArrayList<>();
    private final Listener listener;

    public WorkoutExerciseAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setData(List<ExerciseItem> data) {
        items.clear();
        items.addAll(data);
        notifyDataSetChanged();
    }

    public List<ExerciseItem> getItems() {
        return items;
    }

    public void moveItem(int fromPosition, int toPosition) {
        Collections.swap(items, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemWorkoutExerciseBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ExerciseItem item = items.get(position);

        holder.binding.txtTitle.setText(item.exercise.title);
        holder.binding.txtDuration.setText(item.workoutExercise.durationSeconds + "s");
        holder.itemView.setOnClickListener(v -> listener.onItemClicked(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        final ItemWorkoutExerciseBinding binding;

        ViewHolder(ItemWorkoutExerciseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
