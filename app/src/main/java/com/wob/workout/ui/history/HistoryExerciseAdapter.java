package com.wob.workout.ui.history;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.wob.workout.data.entity.ExerciseSession;
import com.wob.workout.databinding.ItemHistoryExerciseBinding;

import java.util.ArrayList;
import java.util.List;

public class HistoryExerciseAdapter
        extends RecyclerView.Adapter<HistoryExerciseAdapter.ViewHolder> {

    private final List<ExerciseSession> exercises = new ArrayList<>();

    public void setData(List<ExerciseSession> data) {
        exercises.clear();
        exercises.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemHistoryExerciseBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ExerciseSession item = exercises.get(position);

        holder.binding.txtTitle.setText(item.exerciseTitle);
        holder.binding.txtDuration.setText(
                item.actualDurationSeconds + "s / " + item.plannedDurationSeconds + "s geplant");
    }

    @Override
    public int getItemCount() {
        return exercises.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        final ItemHistoryExerciseBinding binding;

        ViewHolder(ItemHistoryExerciseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
