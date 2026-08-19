package com.wob.workout.ui.exercise;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.wob.workout.data.entity.Exercise;
import com.wob.workout.databinding.ItemExerciseBinding;

import java.util.ArrayList;
import java.util.List;

public class ExerciseAdapter
        extends RecyclerView.Adapter<ExerciseAdapter.ViewHolder> {

    public interface ExerciseClickListener {

        void onExerciseClicked(Exercise exercise);
    }

    private final List<Exercise> exercises =
            new ArrayList<>();

    private final ExerciseClickListener listener;

    public ExerciseAdapter(
            ExerciseClickListener listener) {

        this.listener = listener;
    }

    public void setData(List<Exercise> data) {

        exercises.clear();
        exercises.addAll(data);

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        ItemExerciseBinding binding =
                ItemExerciseBinding.inflate(
                        LayoutInflater.from(
                                parent.getContext()),
                        parent,
                        false);

        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position) {

        Exercise exercise =
                exercises.get(position);

        holder.binding.txtTitle.setText(
                exercise.title);

        holder.itemView.setOnClickListener(v ->
                listener.onExerciseClicked(
                        exercise));

        holder.binding.btnInfo
                .setOnClickListener(v -> {

                    if (exercise.description == null
                            || exercise.description.isBlank()) {
                        return;
                    }

                    new MaterialAlertDialogBuilder(
                            v.getContext())
                            .setTitle(
                                    exercise.title)
                            .setMessage(
                                    exercise.description)
                            .setPositiveButton(
                                    "OK",
                                    null)
                            .show();
                });
    }

    @Override
    public int getItemCount() {

        return exercises.size();
    }

    static class ViewHolder
            extends RecyclerView.ViewHolder {

        final ItemExerciseBinding binding;

        ViewHolder(
                ItemExerciseBinding binding) {

            super(binding.getRoot());

            this.binding = binding;
        }
    }
}