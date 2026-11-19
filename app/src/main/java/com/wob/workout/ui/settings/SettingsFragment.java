package com.wob.workout.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.wob.workout.AppContainer;
import com.wob.workout.R;
import com.wob.workout.WorkoutApplication;
import com.wob.workout.WorkoutViewModelFactory;
import com.wob.workout.databinding.FragmentSettingsBinding;
import com.wob.workout.ui.ScreenToolbar;

/** The settings screen, opened from the gear button on the home screen. */
public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        ScreenToolbar.setUp(this, binding.toolbar);
        ViewCompat.setAccessibilityHeading(binding.txtAppearanceHeading, true);
        ViewCompat.setAccessibilityHeading(binding.txtPlayerHeading, true);

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

        return binding.getRoot();
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
