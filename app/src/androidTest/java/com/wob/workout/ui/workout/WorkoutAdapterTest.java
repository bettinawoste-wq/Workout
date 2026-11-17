package com.wob.workout.ui.workout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.widget.FrameLayout;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.wob.workout.R;
import com.wob.workout.data.entity.Exercise;
import com.wob.workout.data.entity.Workout;
import com.wob.workout.media.ExerciseThumbnails;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

/** The workout card binds its start button to {@link WorkoutListItem#canStart()}. */
@RunWith(AndroidJUnit4.class)
public class WorkoutAdapterTest {

    @Test
    public void theStartButtonIsDisabledOnlyForAWorkoutWithoutExercises() {
        Exercise plank = new Exercise();
        plank.title = "Plank";
        List<WorkoutListItem> items = List.of(
                new WorkoutListItem(new Workout(), 0, 0, Collections.emptyList()),
                new WorkoutListItem(new Workout(), 1, 30, Collections.singletonList(plank)));

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            WorkoutAdapter adapter = adapter(items);
            WorkoutAdapter.ViewHolder holder =
                    adapter.onCreateViewHolder(new FrameLayout(context()), 0);

            adapter.onBindViewHolder(holder, 0);
            assertFalse(holder.binding.btnStart.isEnabled());
            assertEquals(items.get(0).startButtonDescription(),
                    String.valueOf(holder.binding.btnStart.getContentDescription()));
            assertEquals(items.get(0).infoText(), holder.binding.txtInfo.getText().toString());

            // The same card recycled for a workout with exercises.
            adapter.onBindViewHolder(holder, 1);
            assertTrue(holder.binding.btnStart.isEnabled());
            assertNull(holder.binding.btnStart.getContentDescription());
        });
    }

    private static Context context() {
        return new ContextThemeWrapper(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                R.style.Theme_Workout);
    }

    private static WorkoutAdapter adapter(List<WorkoutListItem> items) {
        WorkoutAdapter adapter = new WorkoutAdapter(new WorkoutAdapter.Listener() {
            @Override
            public void onWorkoutClicked(Workout workout) {
            }

            @Override
            public void onWorkoutStartClicked(Workout workout) {
            }
        }, new ExerciseThumbnails(context(), Executors.newSingleThreadExecutor()));
        adapter.setData(items);
        return adapter;
    }
}
