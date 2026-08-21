package com.wob.workout.ui.workout;

import android.os.Bundle;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.wob.workout.R;
import com.wob.workout.data.entity.*;
import com.wob.workout.data.repository.*;
import com.wob.workout.databinding.FragmentWorkoutEditBinding;

import java.util.*;
import java.util.function.Consumer;

public class WorkoutEditFragment extends Fragment {

    private static final String ARG_WORKOUT_ID = "workout_id";
    private FragmentWorkoutEditBinding binding;
    private WorkoutRepository repository;
    private Workout workout;
    private WorkoutExerciseRepository workoutExerciseRepository;
    private ExerciseRepository exerciseRepository;
    private WorkoutExerciseAdapter adapter;

    private final ItemTouchHelper itemTouchHelper = new ItemTouchHelper(
            new ItemTouchHelper.SimpleCallback(
                    ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {

                @Override
                public boolean onMove(@NonNull RecyclerView recyclerView,
                                      @NonNull RecyclerView.ViewHolder viewHolder,
                                      @NonNull RecyclerView.ViewHolder target) {
                    adapter.moveItem(
                            viewHolder.getBindingAdapterPosition(),
                            target.getBindingAdapterPosition());
                    return true;
                }

                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                }

                @Override
                public void clearView(@NonNull RecyclerView recyclerView,
                                      @NonNull RecyclerView.ViewHolder viewHolder) {
                    super.clearView(recyclerView, viewHolder);
                    persistOrder();
                }
            });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentWorkoutEditBinding.inflate(inflater, container, false);

        repository = new WorkoutRepository(requireContext());
        workoutExerciseRepository = new WorkoutExerciseRepository(requireContext());
        exerciseRepository = new ExerciseRepository(requireContext());

        adapter = new WorkoutExerciseAdapter(this::showExerciseMenu);
        binding.listExercises.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.listExercises.setAdapter(adapter);
        itemTouchHelper.attachToRecyclerView(binding.listExercises);

        loadWorkout();
        loadExercises();

        binding.btnSave.setOnClickListener(v -> save());
        binding.btnDelete.setOnClickListener(v -> confirmDelete());
        binding.btnAddExercise.setOnClickListener(v -> showExercisePicker());

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

    private void confirmDelete() {
        if (workout == null) return;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Workout löschen?")
                .setMessage("\"" + workout.name + "\" wird endgültig gelöscht.")
                .setNegativeButton("Abbrechen", null)
                .setPositiveButton("Löschen", (dialog, which) -> {
                    repository.delete(workout);
                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .show();
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

        List<WorkoutExerciseAdapter.ExerciseItem> items = new ArrayList<>();

        for (WorkoutExercise workoutExercise :
                workoutExerciseRepository.getByWorkout(workout.id)) {

            Exercise exercise =
                    exerciseRepository.getById(workoutExercise.exerciseId);

            if (exercise != null) {
                items.add(new WorkoutExerciseAdapter.ExerciseItem(exercise, workoutExercise));
            }
        }

        adapter.setData(items);
    }

    private void persistOrder() {
        List<WorkoutExerciseAdapter.ExerciseItem> items = adapter.getItems();

        for (int i = 0; i < items.size(); i++) {
            WorkoutExercise workoutExercise = items.get(i).workoutExercise;
            if (workoutExercise.position != i) {
                workoutExercise.position = i;
                workoutExerciseRepository.update(workoutExercise);
            }
        }
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

                    Exercise selected = exercises.get(which);

                    showDurationInputDialog(selected.title, 60, duration -> {

                        WorkoutExercise workoutExercise = new WorkoutExercise();

                        workoutExercise.workoutId = workout.id;
                        workoutExercise.exerciseId = selected.id;
                        workoutExercise.position =
                                workoutExerciseRepository
                                        .getByWorkout(workout.id)
                                        .size();

                        workoutExercise.durationSeconds = duration;

                        workoutExerciseRepository.save(workoutExercise);

                        loadExercises();
                    });
                })
                .show();
    }

    private void showDurationInputDialog(
            String title, int currentValue, Consumer<Integer> onConfirm) {

        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_duration_input, null, false);

        TextInputEditText edtDuration = view.findViewById(R.id.edtDuration);
        edtDuration.setText(String.valueOf(currentValue));

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setView(view)
                .setNegativeButton("Abbrechen", null)
                .setPositiveButton("OK", null)
                .show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {

            String text = edtDuration.getText() != null
                    ? edtDuration.getText().toString().trim()
                    : "";

            int value;
            try {
                value = Integer.parseInt(text);
            } catch (NumberFormatException e) {
                value = -1;
            }

            if (value <= 0) {
                Snackbar.make(binding.getRoot(),
                        "Bitte eine gültige Dauer in Sekunden eingeben",
                        Snackbar.LENGTH_SHORT).show();
                return;
            }

            onConfirm.accept(value);
            dialog.dismiss();
        });
    }

    private void showDurationDialog(WorkoutExerciseAdapter.ExerciseItem item) {
        showDurationInputDialog(
                item.exercise.title,
                item.workoutExercise.durationSeconds,
                duration -> {
                    item.workoutExercise.durationSeconds = duration;
                    workoutExerciseRepository.update(item.workoutExercise);
                    loadExercises();
                });
    }

    private void showExerciseMenu(WorkoutExerciseAdapter.ExerciseItem item) {

        String[] actions = {
                "Dauer ändern",
                "Entfernen"
        };

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(item.exercise.title)
                .setItems(actions, (dialog, which) -> {

                    switch (which) {
                        case 0 -> showDurationDialog(item);
                        case 1 -> confirmRemoveExercise(item);
                    }
                })
                .show();
    }

    private void confirmRemoveExercise(WorkoutExerciseAdapter.ExerciseItem item) {

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Übung entfernen?")
                .setMessage("\"" + item.exercise.title + "\" wird aus diesem Workout entfernt.")
                .setNegativeButton("Abbrechen", null)
                .setPositiveButton("Entfernen", (dialog, which) -> removeExercise(item))
                .show();
    }

    private void removeExercise(WorkoutExerciseAdapter.ExerciseItem item) {

        workoutExerciseRepository.delete(
                item.workoutExercise);

        loadExercises();
    }
}
