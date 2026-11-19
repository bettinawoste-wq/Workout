package com.wob.workout.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.wob.workout.AppContainer;
import com.wob.workout.R;
import com.wob.workout.WorkoutApplication;
import com.wob.workout.WorkoutViewModelFactory;
import com.wob.workout.backup.BackupPreview;
import com.wob.workout.backup.BackupRefused;
import com.wob.workout.databinding.FragmentSettingsBinding;
import com.wob.workout.ui.ScreenToolbar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** The settings screen, opened from the gear button on the home screen. */
public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private BackupViewModel backupViewModel;

    /** The system's file dialogs: where the backup goes, and which one to read back. */
    private final ActivityResultLauncher<String> exportFile = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/zip"), file -> {
                if (file != null) backupViewModel.exportTo(file);
            });
    private final ActivityResultLauncher<String[]> importFile = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), file -> {
                if (file != null) backupViewModel.open(file);
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        ScreenToolbar.setUp(this, binding.toolbar);
        ViewCompat.setAccessibilityHeading(binding.txtAppearanceHeading, true);
        ViewCompat.setAccessibilityHeading(binding.txtPlayerHeading, true);
        ViewCompat.setAccessibilityHeading(binding.txtDataHeading, true);

        AppContainer appContainer =
                ((WorkoutApplication) requireActivity().getApplication()).getAppContainer();
        SettingsViewModel viewModel =
                new ViewModelProvider(this, new WorkoutViewModelFactory(appContainer))
                        .get(SettingsViewModel.class);

        viewModel.getDesign().observe(getViewLifecycleOwner(),
                design -> binding.groupDesign.check(buttonOf(design)));
        binding.groupDesign.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            Design chosen = designOf(checkedId);
            if (chosen == viewModel.getDesign().getValue()) return;
            viewModel.setDesign(chosen);
            chosen.apply();
        });

        viewModel.isSoundOn().observe(getViewLifecycleOwner(), binding.switchSound::setChecked);
        binding.switchSound.setOnCheckedChangeListener(
                (button, checked) -> viewModel.setSoundOn(checked));
        viewModel.isVibrationOn().observe(getViewLifecycleOwner(),
                binding.switchVibration::setChecked);
        binding.switchVibration.setOnCheckedChangeListener(
                (button, checked) -> viewModel.setVibrationOn(checked));

        viewModel.getDefaultTransitionSeconds().observe(getViewLifecycleOwner(), seconds -> {
            binding.txtDefaultTransition.setText(getString(R.string.sekunden_kurz, seconds));
            if (binding.sliderDefaultTransition.getValue() != seconds) {
                binding.sliderDefaultTransition.setValue(seconds);
            }
        });
        binding.sliderDefaultTransition.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) viewModel.setDefaultTransitionSeconds(Math.round(value));
        });

        setUpBackup(appContainer);

        return binding.getRoot();
    }

    /** The Data section: export, and import with its preview and three choices. */
    private void setUpBackup(AppContainer appContainer) {
        backupViewModel =
                new ViewModelProvider(this, new WorkoutViewModelFactory(appContainer))
                        .get(BackupViewModel.class);
        binding.btnExport.setOnClickListener(v -> exportFile.launch("workout-backup-"
                + new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date())
                + ".zip"));
        binding.btnImport.setOnClickListener(v -> importFile.launch(new String[]{
                "application/zip", "application/x-zip-compressed",
                "application/octet-stream"}));
        backupViewModel.isBusy().observe(getViewLifecycleOwner(), busy -> {
            binding.progressBackup.setVisibility(busy ? View.VISIBLE : View.GONE);
            binding.btnExport.setEnabled(!busy);
            binding.btnImport.setEnabled(!busy);
        });
        backupViewModel.getPreview().observe(getViewLifecycleOwner(), event -> {
            BackupPreview preview = event.getContentIfNotHandled();
            if (preview != null) showPreview(preview);
        });
        backupViewModel.getOutcome().observe(getViewLifecycleOwner(), event -> {
            BackupViewModel.Outcome outcome = event.getContentIfNotHandled();
            if (outcome != null) {
                Snackbar.make(binding.getRoot(), messageOf(outcome), Snackbar.LENGTH_LONG)
                        .show();
            }
        });
    }

    private void showPreview(BackupPreview preview) {
        if (preview.refusal != null) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.backup_nicht_importierbar)
                    .setMessage(preview.refusal == BackupRefused.Reason.NEWER_VERSION
                            ? R.string.backup_neuere_version : R.string.backup_unlesbar)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.backup_importieren)
                .setMessage(getString(R.string.backup_inhalt, preview.text()))
                .setPositiveButton(R.string.ersetzen, (dialog, which) ->
                        backupViewModel.replace())
                .setNeutralButton(R.string.zusammenfuehren, (dialog, which) ->
                        confirmAddAlongside())
                .setNegativeButton(R.string.abbrechen, (dialog, which) ->
                        backupViewModel.cancel())
                .setOnCancelListener(dialog -> backupViewModel.cancel())
                .show();
    }

    /** Zusammenführen warns about duplicates first. */
    private void confirmAddAlongside() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.zusammenfuehren)
                .setMessage(R.string.zusammenfuehren_warnung)
                .setPositiveButton(R.string.zusammenfuehren, (dialog, which) ->
                        backupViewModel.addAlongside())
                .setNegativeButton(R.string.abbrechen, (dialog, which) ->
                        backupViewModel.cancel())
                .setOnCancelListener(dialog -> backupViewModel.cancel())
                .show();
    }

    private static int messageOf(BackupViewModel.Outcome outcome) {
        switch (outcome) {
            case EXPORTED:
                return R.string.backup_exportiert;
            case EXPORT_FAILED:
                return R.string.backup_export_fehlgeschlagen;
            case IMPORTED:
                return R.string.backup_importiert;
            case IMPORT_FAILED:
            default:
                return R.string.backup_import_fehlgeschlagen;
        }
    }

    private static int buttonOf(Design design) {
        switch (design) {
            case LIGHT:
                return R.id.btnDesignLight;
            case DARK:
                return R.id.btnDesignDark;
            case SYSTEM:
            default:
                return R.id.btnDesignSystem;
        }
    }

    private static Design designOf(int buttonId) {
        if (buttonId == R.id.btnDesignLight) return Design.LIGHT;
        if (buttonId == R.id.btnDesignDark) return Design.DARK;
        return Design.SYSTEM;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
