package com.wob.workout.ui.workout;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.*;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
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
import com.wob.workout.data.WorkoutExercise;
import com.wob.workout.databinding.FragmentWorkoutEditBinding;
import com.wob.workout.ui.EmptyState;
import com.wob.workout.ui.Navigator;
import com.wob.workout.ui.ScreenToolbar;
import com.wob.workout.ui.exercise.ExerciseEditFragment;

import java.util.List;

public class WorkoutEditFragment extends Fragment {

    private static final String ARG_WORKOUT_ID = "workout_id";
    private static final String ARG_OPENED_AS_NEW = "opened_as_new";
    private FragmentWorkoutEditBinding binding;
    private WorkoutEditViewModel viewModel;
    private WorkoutExerciseAdapter adapter;

    /** Enabled only while the draft has unsaved changes: then leaving asks first. Without
     *  them, back closes the editor right away. */
    private final OnBackPressedCallback confirmDiscardOnBack = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            confirmDiscard();
        }
    };

    private final ItemTouchHelper itemTouchHelper = new ItemTouchHelper(
            new ItemTouchHelper.SimpleCallback(
                    ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {

                @Override
                public boolean onMove(@NonNull RecyclerView recyclerView,
                                      @NonNull RecyclerView.ViewHolder viewHolder,
                                      @NonNull RecyclerView.ViewHolder target) {
                    // The draft moves it; the list follows from the published draft.
                    viewModel.moveExercise(
                            viewHolder.getBindingAdapterPosition(),
                            target.getBindingAdapterPosition());
                    return true;
                }

                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                }
            });

    /** Here rather than in onCreateView, so the exercise picker sheet, a child fragment
     *  that shares this ViewModel, finds it even when it is restored first. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        long workoutId = getArguments() != null
                ? getArguments().getLong(ARG_WORKOUT_ID, -1)
                : -1;
        AppContainer appContainer =
                ((WorkoutApplication) requireActivity().getApplication()).getAppContainer();
        viewModel = new ViewModelProvider(
                this, new WorkoutViewModelFactory(appContainer, workoutId))
                .get(WorkoutEditViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentWorkoutEditBinding.inflate(inflater, container, false);

        AppContainer appContainer =
                ((WorkoutApplication) requireActivity().getApplication()).getAppContainer();

        adapter = new WorkoutExerciseAdapter(this::showExerciseDialog, appContainer.thumbnails);
        binding.listExercises.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.listExercises.setAdapter(adapter);
        itemTouchHelper.attachToRecyclerView(binding.listExercises);

        ScreenToolbar.setUp(this, binding.toolbar);
        // From how the screen was opened, so it doesn't change once the first edit saves it.
        boolean openedAsNew =
                getArguments() != null && getArguments().getBoolean(ARG_OPENED_AS_NEW);
        binding.toolbar.setTitle(
                openedAsNew ? R.string.neues_workout : R.string.workout_bearbeiten);
        binding.toolbar.setOnMenuItemClickListener(this::onMenuItemClick);
        // Only a stored workout can be deleted; duplicating saves the draft first.
        binding.toolbar.getMenu().findItem(R.id.action_delete).setEnabled(!viewModel.isNew());
        requireActivity().getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), confirmDiscardOnBack);
        viewModel.getUnsavedChanges().observe(getViewLifecycleOwner(),
                unsaved -> confirmDiscardOnBack.setEnabled(Boolean.TRUE.equals(unsaved)));

        // Fills the name field once for an existing workout (see takeNameToShow()).
        viewModel.getWorkout().observe(getViewLifecycleOwner(), workout -> {
            if (workout == null) return;
            if (viewModel.takeNameToShow() && TextUtils.isEmpty(binding.edtName.getText())) {
                binding.edtName.setText(workout.name);
            }
        });
        // Added after the seeding above has had its chance, but a restored or seeded text
        // still reaches it later; the same name again is no change to the draft.
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
        EmptyState.setUp(binding.emptyState, R.drawable.ic_list, R.string.workout_ohne_uebungen,
                R.string.uebungen_hinzufuegen, v -> ExercisePickerSheet.show(this));
        viewModel.getExercises().observe(getViewLifecycleOwner(), workoutExercises -> {
            if (workoutExercises == null) return;
            adapter.setData(workoutExercises);
            EmptyState.show(
                    binding.emptyState, workoutExercises.isEmpty(), binding.btnAddExercise);
        });
        viewModel.getSummary().observe(getViewLifecycleOwner(), binding.txtSummary::setText);
        viewModel.getTransition().observe(getViewLifecycleOwner(), text -> {
            binding.btnTransition.setText(text);
            binding.btnTransition.setContentDescription(
                    getString(R.string.uebergang) + ": " + text);
        });
        binding.btnTransition.setOnClickListener(v -> showTransitionDialog());

        // Not just the first creation: this Fragment can sit on the back stack while the
        // user edits or deletes an Exercise from the catalog on a different screen, so the
        // list needs a fresh reload every time this view comes back, not only after this
        // screen's own writes.
        viewModel.refreshExercises();

        binding.btnAddExercise.setOnClickListener(v -> ExercisePickerSheet.show(this));
        binding.btnAddRest.setOnClickListener(v -> viewModel.addRest());
        binding.btnSave.setOnClickListener(v -> save());
        // Nothing to save or copy until an existing workout has loaded into the draft.
        viewModel.getReady().observe(getViewLifecycleOwner(), ready -> {
            boolean enabled = Boolean.TRUE.equals(ready);
            binding.btnSave.setEnabled(enabled);
            binding.toolbar.getMenu().findItem(R.id.action_duplicate).setEnabled(enabled);
        });

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /** Opens the editor on a new workout, created only by "Speichern". */
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

    /** Saves the draft and closes the editor. */
    private void save() {
        // Once only: a quick second tap would otherwise save again.
        binding.btnSave.setEnabled(false);
        viewModel.save().observe(getViewLifecycleOwner(), saved -> {
            if (saved == null) {
                binding.btnSave.setEnabled(true);
                Snackbar.make(binding.getRoot(), "Speichern fehlgeschlagen",
                        Snackbar.LENGTH_SHORT).show();
                return;
            }
            Navigator.of(this).close();
        });
    }

    private void confirmDiscard() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.aenderungen_verwerfen)
                .setNegativeButton(R.string.weiter_bearbeiten, null)
                .setPositiveButton(R.string.verwerfen, (dialog, which) ->
                        Navigator.of(this).close())
                .show();
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

    /** Saves the draft, then opens the copy in place of this editor, so back still leads
     *  to the workout list. */
    private void duplicate() {
        viewModel.duplicate().observe(getViewLifecycleOwner(), copyId -> {
            if (copyId == null) {
                Snackbar.make(binding.getRoot(), "Duplizieren fehlgeschlagen",
                        Snackbar.LENGTH_SHORT).show();
                return;
            }
            Navigator.of(this).replace(edit(copyId));
        });
    }

    private void confirmDelete() {
        if (viewModel.isNew()) return;

        String name = viewModel.nameToConfirmDelete(binding.edtName.getText().toString());

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Workout löschen?")
                .setMessage("\"" + name + "\" wird endgültig gelöscht.")
                .setNegativeButton("Abbrechen", null)
                .setPositiveButton("Löschen", (dialog, which) ->
                        viewModel.delete().observe(getViewLifecycleOwner(), deleted -> {
                            if (deleted != null && deleted) {
                                Navigator.of(this).close();
                            }
                        }))
                .show();
    }

    /** What tapping a workout exercise or rest opens: its duration, ready to change, with
     *  "Entfernen" and, for an exercise, "Übung bearbeiten" below it. */
    private void showExerciseDialog(int position) {
        List<WorkoutExercise> shown = viewModel.getExercises().getValue();
        if (shown == null || position >= shown.size()) return;
        WorkoutExercise workoutExercise = shown.get(position);

        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_duration_input, null, false);

        TextInputEditText edtDuration = view.findViewById(R.id.edtDuration);
        edtDuration.setText(String.valueOf(workoutExercise.durationSeconds));
        view.findViewById(R.id.btnEditExercise)
                .setVisibility(workoutExercise.isRest() ? View.GONE : View.VISIBLE);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(titleOf(workoutExercise))
                .setView(view)
                .setNegativeButton("Abbrechen", null)
                .setPositiveButton("OK", null)
                .show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {

            String typed = edtDuration.getText() != null ? edtDuration.getText().toString() : "";
            if (viewModel.changeDuration(position, typed)
                    == WorkoutDraft.DurationAnswer.NOT_VALID_SECONDS) {
                Snackbar.make(binding.getRoot(),
                        "Bitte eine gültige Dauer in Sekunden eingeben",
                        Snackbar.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
        });

        view.findViewById(R.id.btnRemove).setOnClickListener(v -> {
            dialog.dismiss();
            removeExercise(position);
        });
        view.findViewById(R.id.btnEditExercise).setOnClickListener(v -> {
            dialog.dismiss();
            openExercise(workoutExercise.exerciseId());
        });
    }

    /** The workout's own transition length, ready to change, or "Standard" to follow the
     *  default transition again. */
    private void showTransitionDialog() {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_transition_input, null, false);
        TextInputEditText edtTransition = view.findViewById(R.id.edtTransition);
        edtTransition.setText(String.valueOf(viewModel.transitionSecondsToEdit()));

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.uebergang)
                .setView(view)
                .setNeutralButton(R.string.standard,
                        (d, which) -> viewModel.followDefaultTransition())
                .setNegativeButton("Abbrechen", null)
                .setPositiveButton("OK", null)
                .show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String typed =
                    edtTransition.getText() != null ? edtTransition.getText().toString() : "";
            if (viewModel.changeTransition(typed)
                    == WorkoutDraft.TransitionAnswer.NOT_VALID_SECONDS) {
                Snackbar.make(binding.getRoot(),
                        "Bitte 0 bis 30 Sekunden eingeben",
                        Snackbar.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
        });
    }

    /** No question first: nothing is saved before "Speichern", and the snackbar undoes it. */
    private void removeExercise(int position) {
        WorkoutExercise removed = viewModel.removeExercise(position);
        if (removed == null) return;
        Snackbar.make(binding.getRoot(),
                        "„" + titleOf(removed) + "“ entfernt", Snackbar.LENGTH_LONG)
                .setAction("Rückgängig", v -> viewModel.restoreExercise(position, removed))
                .show();
    }

    /** The exercise's title, or "Pause" for a rest. */
    private String titleOf(WorkoutExercise workoutExercise) {
        return workoutExercise.isRest()
                ? getString(R.string.pause)
                : workoutExercise.exercise.title;
    }

    /** Edits the exercise itself, for every workout; this editor's draft waits on the back
     *  stack and takes the changes over when it returns. */
    private void openExercise(long exerciseId) {
        Navigator.of(this).open(ExerciseEditFragment.edit(exerciseId));
    }
}
