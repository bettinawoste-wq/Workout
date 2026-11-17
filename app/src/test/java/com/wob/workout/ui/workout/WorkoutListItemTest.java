package com.wob.workout.ui.workout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.wob.workout.data.entity.Exercise;
import com.wob.workout.data.entity.Workout;
import java.util.Collections;
import org.junit.Test;

public class WorkoutListItemTest {

    @Test
    public void aWorkoutWithoutExercisesCannotStart() {
        assertFalse(empty().canStart());
    }

    @Test
    public void aWorkoutWithExercisesCanStart() {
        assertTrue(withOnePlank().canStart());
    }

    @Test
    public void aWorkoutWhoseRowsLeadToNoExerciseCannotStart() {
        // A row counted, but its exercise missing: the player would have nothing to play.
        WorkoutListItem item = new WorkoutListItem(new Workout(), 1, 30, Collections.emptyList());

        assertFalse(item.canStart());
    }

    @Test
    public void theInfoShowsCountAndDuration() {
        assertEquals("1 Übung • 0:30", withOnePlank().infoText());
    }

    @Test
    public void theInfoOfAnEmptyWorkoutSaysHowToStartIt() {
        assertEquals("Noch keine Übungen – füge eine hinzu, um zu starten",
                empty().infoText());
    }

    @Test
    public void onlyADisabledStartButtonExplainsItself() {
        assertNull(withOnePlank().startButtonDescription());
        assertEquals("Start nicht möglich: noch keine Übungen",
                empty().startButtonDescription());
    }

    private static WorkoutListItem empty() {
        return new WorkoutListItem(new Workout(), 0, 0, Collections.emptyList());
    }

    private static WorkoutListItem withOnePlank() {
        Exercise plank = new Exercise();
        plank.title = "Plank";
        return new WorkoutListItem(new Workout(), 1, 30, Collections.singletonList(plank));
    }
}
