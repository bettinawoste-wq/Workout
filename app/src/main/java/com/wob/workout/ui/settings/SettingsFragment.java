package com.wob.workout.ui.settings;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.wob.workout.AppContainer;
import com.wob.workout.R;
import com.wob.workout.WorkoutApplication;
import com.wob.workout.WorkoutViewModelFactory;
import com.wob.workout.backup.BackupPreview;
import com.wob.workout.backup.BackupRefused;
import com.wob.workout.data.entity.Reminder;
import com.wob.workout.databinding.DialogReminderBinding;
import com.wob.workout.databinding.FragmentSettingsBinding;
import com.wob.workout.databinding.ItemReminderBinding;
import com.wob.workout.reminders.ReminderNotification;
import com.wob.workout.training.Weekdays;
import com.wob.workout.ui.ScreenToolbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** The settings screen, opened from the gear button on the home screen. */
public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private SettingsViewModel viewModel;
    private List<Reminder> reminders = Collections.emptyList();
    private ActivityResultLauncher<String> notificationPermission;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        notificationPermission = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> showNotificationHint());
    }

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
        ViewCompat.setAccessibilityHeading(binding.txtTrainingHeading, true);
        ViewCompat.setAccessibilityHeading(binding.txtDataHeading, true);

        AppContainer appContainer =
                ((WorkoutApplication) requireActivity().getApplication()).getAppContainer();
        viewModel = new ViewModelProvider(this, new WorkoutViewModelFactory(appContainer))
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

        viewModel.getWeeklyGoal().observe(getViewLifecycleOwner(), trainingDays -> {
            binding.txtWeeklyGoal.setText(trainingDays == null
                    ? getString(R.string.kein_wochenziel)
                    : getResources().getQuantityString(
                            R.plurals.trainingstage_pro_woche, trainingDays, trainingDays));
            int value = trainingDays == null ? 0 : trainingDays;
            if (binding.sliderWeeklyGoal.getValue() != value) {
                binding.sliderWeeklyGoal.setValue(value);
            }
        });
        binding.sliderWeeklyGoal.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) viewModel.setWeeklyGoal(Math.round(value));
        });

        viewModel.getReminders().observe(getViewLifecycleOwner(), list -> {
            reminders = list;
            showReminders();
            showNotificationHint();
        });
        binding.btnAddReminder.setOnClickListener(v -> showReminderDialog(null));
        binding.btnNotificationSettings.setOnClickListener(v -> openNotificationSettings());
        setUpBackup(appContainer);

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Notifications may have been allowed in the system settings meanwhile.
        showNotificationHint();
    }

    private void showReminders() {
        binding.listReminders.removeAllViews();
        for (Reminder reminder : reminders) {
            ItemReminderBinding item = ItemReminderBinding.inflate(
                    getLayoutInflater(), binding.listReminders, true);
            String time = timeOf(reminder.minuteOfDay);
            item.txtReminderTime.setText(time);
            item.txtReminderWeekdays.setText(weekdaysText(reminder.days()));
            item.switchReminder.setChecked(reminder.enabled);
            item.switchReminder.setContentDescription(getString(R.string.erinnerung_an, time));
            item.switchReminder.setOnCheckedChangeListener(
                    (button, checked) -> viewModel.setReminderOn(reminder, checked));
            item.reminderText.setOnClickListener(v -> showReminderDialog(reminder));
            item.btnDeleteReminder.setOnClickListener(v -> confirmDelete(reminder, time));
        }
    }

    /** Adds a reminder when {@code reminder} is null, otherwise changes it. */
    private void showReminderDialog(Reminder reminder) {
        DialogReminderBinding dialogBinding = DialogReminderBinding.inflate(getLayoutInflater());
        int minuteOfDay =
                reminder == null ? viewModel.newReminderMinuteOfDay() : reminder.minuteOfDay;
        Weekdays weekdays = reminder == null ? viewModel.newReminderWeekdays() : reminder.days();
        dialogBinding.timePicker.setIs24HourView(true);
        dialogBinding.timePicker.setHour(minuteOfDay / 60);
        dialogBinding.timePicker.setMinute(minuteOfDay % 60);
        String[] names = getResources().getStringArray(R.array.wochentage_kurz);
        List<Chip> chips = new ArrayList<>();
        for (int day = 0; day < Weekdays.COUNT; day++) {
            Chip chip = new Chip(requireContext());
            chip.setText(names[day]);
            chip.setCheckable(true);
            chip.setChecked(weekdays.contains(day));
            dialogBinding.groupWeekdays.addView(chip);
            chips.add(chip);
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(reminder == null
                        ? R.string.neue_erinnerung : R.string.erinnerung_bearbeiten)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(R.string.abbrechen, null)
                .setPositiveButton(R.string.speichern, (d, which) -> {
                    // Commits an hour or minute typed in but not yet confirmed.
                    dialogBinding.timePicker.clearFocus();
                    int chosenMinute = dialogBinding.timePicker.getHour() * 60
                            + dialogBinding.timePicker.getMinute();
                    Weekdays chosenWeekdays = checkedWeekdays(chips);
                    if (reminder != null) {
                        viewModel.changeReminder(reminder, chosenMinute, chosenWeekdays);
                        return;
                    }
                    boolean first = reminders.isEmpty();
                    viewModel.addReminder(chosenMinute, chosenWeekdays);
                    if (first) askForNotificationPermission();
                })
                .show();
        Runnable updateSave = () -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setEnabled(viewModel.canSaveReminder(checkedWeekdays(chips)));
        for (Chip chip : chips) {
            chip.setOnCheckedChangeListener((button, checked) -> updateSave.run());
        }
        updateSave.run();
    }

    private void confirmDelete(Reminder reminder, String time) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.erinnerung_loeschen)
                .setMessage(getString(R.string.erinnerung_loeschen_text, time))
                .setNegativeButton(R.string.abbrechen, null)
                .setPositiveButton(R.string.loeschen,
                        (dialog, which) -> viewModel.deleteReminder(reminder))
                .show();
    }

    private void askForNotificationPermission() {
        if (ReminderNotification.needsPermission(requireContext())) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    /** The hint shows while notifications are not allowed, with or without reminders yet:
     *  it says that reminders stay silent, also before the first one is added. */
    private void showNotificationHint() {
        if (binding == null) return;
        boolean off = !ReminderNotification.isAllowed(requireContext());
        binding.groupNotificationsOff.setVisibility(off ? View.VISIBLE : View.GONE);
    }

    private void openNotificationSettings() {
        String packageName = requireContext().getPackageName();
        Intent intent = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                : new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", packageName, null));
        startActivity(intent);
    }

    private static String timeOf(int minuteOfDay) {
        return String.format(Locale.GERMANY, "%02d:%02d", minuteOfDay / 60, minuteOfDay % 60);
    }

    /** For example "Mo, Mi, Fr", or "Täglich". */
    private String weekdaysText(Weekdays weekdays) {
        if (weekdays.isEveryDay()) return getString(R.string.taeglich);
        String[] names = getResources().getStringArray(R.array.wochentage_kurz);
        List<String> days = new ArrayList<>();
        for (int day = 0; day < Weekdays.COUNT; day++) {
            if (weekdays.contains(day)) days.add(names[day]);
        }
        return String.join(", ", days);
    }

    /** The weekdays whose chips are checked, one chip per day from Monday. */
    private static Weekdays checkedWeekdays(List<Chip> chips) {
        Weekdays weekdays = Weekdays.NONE;
        for (int day = 0; day < chips.size(); day++) {
            weekdays = weekdays.with(day, chips.get(day).isChecked());
        }
        return weekdays;
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
                .setMessage(getString(R.string.backup_inhalt, contentsText(preview)))
                .setPositiveButton(R.string.ersetzen, (dialog, which) ->
                        backupViewModel.replace())
                .setNeutralButton(R.string.zusammenfuehren, (dialog, which) ->
                        confirmAddAlongside())
                .setNegativeButton(R.string.abbrechen, (dialog, which) ->
                        backupViewModel.cancel())
                .setOnCancelListener(dialog -> backupViewModel.cancel())
                .show();
    }

    /** For example "12 Übungen, 3 Workouts, 40 Einträge im Verlauf". */
    private String contentsText(BackupPreview preview) {
        return getString(R.string.backup_umfang,
                getResources().getQuantityString(R.plurals.backup_uebungen,
                        preview.exercises, preview.exercises),
                getResources().getQuantityString(R.plurals.backup_workouts,
                        preview.workouts, preview.workouts),
                getResources().getQuantityString(R.plurals.backup_eintraege_im_verlauf,
                        preview.workoutSessions, preview.workoutSessions));
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
