package com.wob.workout.ui.settings;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.wob.workout.AppContainer;
import com.wob.workout.R;
import com.wob.workout.WorkoutApplication;
import com.wob.workout.WorkoutViewModelFactory;
import com.wob.workout.data.entity.Reminder;
import com.wob.workout.databinding.DialogReminderBinding;
import com.wob.workout.databinding.FragmentSettingsBinding;
import com.wob.workout.databinding.ItemReminderBinding;
import com.wob.workout.reminders.ReminderNotification;
import com.wob.workout.ui.ScreenToolbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** The settings screen, opened from the gear button on the home screen. */
public class SettingsFragment extends Fragment {

    /** A new reminder's time and weekdays until changed: 18:00, Monday to Friday. */
    private static final int NEW_REMINDER_MINUTE = 18 * 60;
    private static final int NEW_REMINDER_WEEKDAYS = 0b0011111;

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

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        ScreenToolbar.setUp(this, binding.toolbar);
        ViewCompat.setAccessibilityHeading(binding.txtAppearanceHeading, true);
        ViewCompat.setAccessibilityHeading(binding.txtPlayerHeading, true);
        ViewCompat.setAccessibilityHeading(binding.txtTrainingHeading, true);

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
            if (!fromUser) return;
            int trainingDays = Math.round(value);
            viewModel.setWeeklyGoal(trainingDays == 0 ? null : trainingDays);
        });

        viewModel.getReminders().observe(getViewLifecycleOwner(), list -> {
            reminders = list;
            showReminders();
            showNotificationHint();
        });
        binding.btnAddReminder.setOnClickListener(v -> showReminderDialog(null));
        binding.btnNotificationSettings.setOnClickListener(v -> openNotificationSettings());

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
            item.txtReminderWeekdays.setText(weekdaysOf(reminder));
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
        int minuteOfDay = reminder == null ? NEW_REMINDER_MINUTE : reminder.minuteOfDay;
        int weekdays = reminder == null ? NEW_REMINDER_WEEKDAYS : reminder.weekdays;
        dialogBinding.timePicker.setIs24HourView(true);
        dialogBinding.timePicker.setHour(minuteOfDay / 60);
        dialogBinding.timePicker.setMinute(minuteOfDay % 60);
        String[] names = getResources().getStringArray(R.array.wochentage_kurz);
        List<Chip> chips = new ArrayList<>();
        for (int day = 0; day < names.length; day++) {
            Chip chip = new Chip(requireContext());
            chip.setText(names[day]);
            chip.setCheckable(true);
            chip.setChecked((weekdays & (Reminder.MONDAY << day)) != 0);
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
                    int chosenWeekdays = weekdaysOf(chips);
                    if (reminder != null) {
                        viewModel.changeReminder(reminder, chosenMinute, chosenWeekdays);
                        return;
                    }
                    boolean first = reminders.isEmpty();
                    viewModel.addReminder(chosenMinute, chosenWeekdays);
                    if (first) askForNotificationPermission();
                })
                .show();
        // A reminder needs at least one weekday.
        Runnable updateSave = () -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setEnabled(weekdaysOf(chips) != 0);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    /** The hint shows while there are reminders but notifications are not allowed. */
    private void showNotificationHint() {
        if (binding == null) return;
        boolean off = !reminders.isEmpty() && !ReminderNotification.isAllowed(requireContext());
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

    private String weekdaysOf(Reminder reminder) {
        String[] names = getResources().getStringArray(R.array.wochentage_kurz);
        List<String> days = new ArrayList<>();
        for (int day = 0; day < names.length; day++) {
            if (reminder.isOn(day)) days.add(names[day]);
        }
        return days.size() == names.length
                ? getString(R.string.taeglich) : String.join(", ", days);
    }

    private static int weekdaysOf(List<Chip> chips) {
        int weekdays = 0;
        for (int day = 0; day < chips.size(); day++) {
            if (chips.get(day).isChecked()) weekdays |= Reminder.MONDAY << day;
        }
        return weekdays;
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
