package com.wob.workout.ui.workout;

import android.os.Bundle;
import android.view.*;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.wob.workout.data.entity.*;
import com.wob.workout.data.repository.*;
import com.wob.workout.databinding.FragmentWorkoutEditBinding;

import java.util.*;

public class WorkoutEditFragment extends Fragment {

    private static final String ARG_WORKOUT_ID = "workout_id";
    private FragmentWorkoutEditBinding binding;
    private WorkoutRepository repository;
    private Workout workout;
    private WorkoutExerciseRepository workoutExerciseRepository;
    private ExerciseRepository exerciseRepository;
    private final List<ExerciseItem> exerciseItems = new ArrayList<>();

    private static class ExerciseItem {
        Exercise exercise;
        WorkoutExercise workoutExercise;
        ExerciseItem(Exercise exercise, WorkoutExercise workoutExercise) {
            this.exercise = exercise;
            this.workoutExercise = workoutExercise;
        }
        @NonNull
        @Override
        public String toString() {
            return exercise.title + " (" + workoutExercise.durationSeconds + "s)";
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentWorkoutEditBinding.inflate(inflater, container, false);

        repository = new WorkoutRepository(requireContext());
        workoutExerciseRepository = new WorkoutExerciseRepository(requireContext());
        exerciseRepository = new ExerciseRepository(requireContext());

        loadWorkout();
        loadExercises();

        binding.btnSave.setOnClickListener(v -> save());
        binding.btnDelete.setOnClickListener(v -> delete());
        binding.btnAddExercise.setOnClickListener(v -> showExercisePicker());
        binding.listExercises.setOnItemClickListener(
                (parent, view, position, id) ->
                        showExerciseMenu(
                                exerciseItems.get(position)));

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public static WorkoutEditFragment edit(long workoutId) {
        WorkoutEditFragment fragment = new WorkoutEditFragment();

        Bundle args = new Bundle();
        args.putLong(ARG_WORKOUT_ID, workoutId);

        fragment.setArguments(args);

        return fragment;
    }

    private void save() {
        String name = binding.edtName.getText().toString().trim();

        if (name.isEmpty()) {
            Snackbar.make(binding.getRoot(), "Name erforderlich", Snackbar.LENGTH_SHORT).show();
            return;
        }

        if (workout == null) workout = new Workout();

        workout.name = name;

        if (workout.id == 0) {
            repository.save(workout);
        } else {
            repository.update(workout);
        }

        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private void delete() {
        if (workout == null) return;

        repository.delete(workout);

        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private void loadWorkout() {
        long workoutId = getArguments() != null
                ? getArguments().getLong(ARG_WORKOUT_ID, -1)
                : -1;

        if (workoutId == -1) {
            binding.btnDelete.setVisibility(View.GONE);
            return;
        }

        workout = repository.getById(workoutId);

        if (workout == null) return;

        binding.edtName.setText(workout.name);
    }

    private void loadExercises() {

        if (workout == null) return;

        exerciseItems.clear();

        List<WorkoutExercise> workoutExercises =
                workoutExerciseRepository.getByWorkout(workout.id);

        for (WorkoutExercise workoutExercise : workoutExercises) {

            Exercise exercise =
                    exerciseRepository.getById(workoutExercise.exerciseId);

            if (exercise != null) {
                exerciseItems.add(
                        new ExerciseItem(
                                exercise,
                                workoutExercise));
            }
        }

        binding.listExercises.setAdapter(
                new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_list_item_1,
                        exerciseItems));
    }

    private void showExercisePicker() {

        if (workout == null || workout.id == 0) {
            Snackbar.make(binding.getRoot(),
                    "Workout zuerst speichern",
                    Snackbar.LENGTH_SHORT).show();
            return;
        }

        List<Exercise> exercises =
                exerciseRepository.getAll();

        String[] titles =
                exercises.stream()
                        .map(e -> e.title)
                        .toArray(String[]::new);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Übung auswählen")
                .setItems(titles, (dialog, which) -> {

                    WorkoutExercise workoutExercise =
                            new WorkoutExercise();

                    workoutExercise.workoutId = workout.id;
                    workoutExercise.exerciseId = exercises.get(which).id;
                    workoutExercise.position =
                            workoutExerciseRepository
                                    .getByWorkout(workout.id)
                                    .size();

                    workoutExercise.durationSeconds = 60;

                    workoutExerciseRepository.save(workoutExercise);

                    loadExercises();
                })
                .show();
    }

    private void showDurationDialog(ExerciseItem item) {

        String[] values = {"5","15","30","45","60","90","120","180"};

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(item.exercise.title)
                .setItems(values, (dialog, which) -> {

                    item.workoutExercise.durationSeconds =
                            Integer.parseInt(values[which]);

                    workoutExerciseRepository.update(
                            item.workoutExercise);

                    loadExercises();
                })
                .show();
    }

    private void showExerciseMenu(ExerciseItem item) {

        String[] actions = {
                "Dauer ändern",
                "Nach oben",
                "Nach unten",
                "Aus Workout entfernen"
        };

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(item.exercise.title)
                .setItems(actions, (dialog, which) -> {

                    switch (which) {
                        case 0 -> showDurationDialog(item);
                        case 1 -> moveUp(item);
                        case 2 -> moveDown(item);
                        case 3 -> removeExercise(item);
                    }
                })
                .show();
    }

    private void removeExercise(ExerciseItem item) {

        workoutExerciseRepository.delete(
                item.workoutExercise);

        loadExercises();
    }

    private void moveUp(ExerciseItem item) {

        List<WorkoutExercise> list =
                workoutExerciseRepository.getByWorkout(workout.id);

        int index = -1;

        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id == item.workoutExercise.id) {
                index = i;
                break;
            }
        }

        if (index <= 0) return;

        WorkoutExercise current = list.get(index);
        WorkoutExercise previous = list.get(index - 1);

        int position = current.position;

        current.position = previous.position;
        previous.position = position;

        workoutExerciseRepository.update(current);
        workoutExerciseRepository.update(previous);

        loadExercises();
    }

    private void moveDown(ExerciseItem item) {

        List<WorkoutExercise> list =
                workoutExerciseRepository.getByWorkout(workout.id);

        int index = -1;

        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id == item.workoutExercise.id) {
                index = i;
                break;
            }
        }

        if (index == -1 || index >= list.size() - 1) return;

        WorkoutExercise current = list.get(index);
        WorkoutExercise next = list.get(index + 1);

        int position = current.position;

        current.position = next.position;
        next.position = position;

        workoutExerciseRepository.update(current);
        workoutExerciseRepository.update(next);

        loadExercises();
    }


}