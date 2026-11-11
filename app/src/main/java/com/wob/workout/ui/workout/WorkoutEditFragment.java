package com.wob.workout.ui.workout;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.wob.workout.AppContainer;
import com.wob.workout.R;
import com.wob.workout.WorkoutApplication;
import com.wob.workout.WorkoutViewModelFactory;
import com.wob.workout.data.entity.Exercise;
import com.wob.workout.databinding.FragmentWorkoutEditBinding;
import com.wob.workout.ui.ScreenToolbar;

import java.util.function.Consumer;

public class WorkoutEditFragment extends Fragment {

    private static final String ARG_WORKOUT_ID = "workout_id";
    private static final String ARG_OPENED_AS_NEW = "opened_as_new";
    private FragmentWorkoutEditBinding binding;
    private WorkoutEditViewModel viewModel;
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
                    viewModel.reorder(adapter.getItems());
                }
            });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentWorkoutEditBinding.inflate(inflater, container, false);

        long workoutId = getArguments() != null
                ? getArguments().getLong(ARG_WORKOUT_ID, -1)
                : -1;

        AppContainer appContainer =
                ((WorkoutApplication) requireActivity().getApplication()).getAppContainer();
        viewModel = new ViewModelProvider(this, new WorkoutViewModelFactory(appContainer, workoutId))
                .get(WorkoutEditViewModel.class);

        adapter = new WorkoutExerciseAdapter(this::showExerciseMenu, appContainer.thumbnails);
        binding.listExercises.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.listExercises.setAdapter(adapter);
        itemTouchHelper.attachToRecyclerView(binding.listExercises);

        ScreenToolbar.setUp(this, binding.toolbar);
        // From how the screen was opened, so it doesn't change once the first edit saves it.
        boolean openedAsNew = getArguments() != null && getArguments().getBoolean(ARG_OPENED_AS_NEW);
        binding.toolbar.setTitle(openedAsNew ? R.string.neues_workout : R.string.workout_bearbeiten);
        binding.toolbar.setOnMenuItemClickListener(this::onMenuItemClick);
        setWorkoutActionsEnabled(!viewModel.isNew());

        // Fills the name field once for an existing workout (see takeNameToShow()). Also
        // fires when a new workout gets created by its first change.
        viewModel.getWorkout().observe(getViewLifecycleOwner(), workout -> {
            if (workout == null) return;
            // Remembered so a recreated screen (e.g. after process death) edits this
            // workout rather than starting yet another new one.
            requireArguments().putLong(ARG_WORKOUT_ID, workout.id);
            setWorkoutActionsEnabled(true);
            if (viewModel.takeNameToShow() && TextUtils.isEmpty(binding.edtName.getText())) {
                binding.edtName.setText(workout.name);
            }
        });
        // Added after the seeding above has had its chance, but a restored or seeded text
        // still reaches it later; saving the same name again is harmless.
        binding.edtName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                viewModel.changeName(s.toString());
            }
        });
        viewModel.getExercises().observe(getViewLifecycleOwner(), items -> {
            if (items != null) adapter.setData(items);
        });

        // Not just the first creation: this Fragment can sit on the back stack while the
        // user edits or deletes an Exercise from the catalog on a different screen, so the
        // list needs a fresh reload every time this view comes back, not only after this
        // screen's own writes.
        viewModel.refreshExercises();

        binding.btnAddExercise.setOnClickListener(v -> showExercisePicker());

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /** Opens the editor on a new workout, saved as soon as anything is entered. */
    public static WorkoutEditFragment create() {
        WorkoutEditFragment fragment = edit(-1);
        fragment.requireArguments().putBoolean(ARG_OPENED_AS_NEW, true);
        return fragment;
    }

    public static WorkoutEditFragment edit(long workoutId) {
        WorkoutEditFragment fragment = new WorkoutEditFragment();

        Bundle args = new Bundle();
        args.putLong(ARG_WORKOUT_ID, workoutId);

        fragment.setArguments(args);

        return fragment;
    }

    /** Duplicating or deleting only makes sense once the workout exists. */
    private void setWorkoutActionsEnabled(boolean enabled) {
        binding.toolbar.getMenu().findItem(R.id.action_duplicate).setEnabled(enabled);
        binding.toolbar.getMenu().findItem(R.id.action_delete).setEnabled(enabled);
    }

    private boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.action_duplicate) {
            duplicate();
            return true;
        }
        if (item.getItemId() == R.id.action_delete) {
            confirmDelete();
            return true;
        }
        return false;
    }

    /** Opens the copy in place of this editor, so back still leads to the workout list. */
    private void duplicate() {
        viewModel.duplicate().observe(getViewLifecycleOwner(), copyId -> {
            if (copyId == null) {
                Snackbar.make(binding.getRoot(), "Duplizieren fehlgeschlagen", Snackbar.LENGTH_SHORT).show();
                return;
            }
            FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
            fragmentManager.popBackStack();
            fragmentManager.beginTransaction()
                    .replace(R.id.main, edit(copyId))
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void confirmDelete() {
        if (viewModel.isNew()) return;

        String name = binding.edtName.getText().toString();
        if (name.isEmpty() && viewModel.getWorkout().getValue() != null) {
            name = viewModel.getWorkout().getValue().name;
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Workout löschen?")
                .setMessage("\"" + name + "\" wird endgültig gelöscht.")
                .setNegativeButton("Abbrechen", null)
                .setPositiveButton("Löschen", (dialog, which) ->
                        viewModel.delete().observe(getViewLifecycleOwner(), deleted -> {
                            if (deleted != null && deleted) {
                                requireActivity().getSupportFragmentManager().popBackStack();
                            }
                        }))
                .show();
    }

    private void showExercisePicker() {

        viewModel.loadAllExercises().observe(getViewLifecycleOwner(), exercises -> {
            if (exercises == null) return;

            String[] titles = exercises.stream().map(e -> e.title).toArray(String[]::new);

            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Übung auswählen")
                    .setItems(titles, (dialog, which) -> {
                        Exercise selected = exercises.get(which);
                        showDurationInputDialog(selected.title, 60,
                                duration -> viewModel.addExercise(selected, duration));
                    })
                    .show();
        });
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
                duration -> viewModel.changeDuration(item, duration));
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
                .setPositiveButton("Entfernen", (dialog, which) -> viewModel.removeExercise(item))
                .show();
    }
}
