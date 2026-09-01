package com.wob.workout.data.relation;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.wob.workout.data.entity.ExerciseSession;
import com.wob.workout.data.entity.WorkoutSession;

import java.util.List;

/**
 * A WorkoutSession together with its recorded exercises, fetched via Room's batched
 * relation query (one query for sessions, one batched IN(...) query for their exercise
 * sessions) instead of one count query per session. See wayfinder ticket #8.
 */
public class WorkoutSessionWithExercises {

    @Embedded
    public WorkoutSession session;

    @Relation(parentColumn = "id", entityColumn = "sessionId")
    public List<ExerciseSession> exercises;
}
