package com.wob.workout.backup;

import com.wob.workout.data.database.AppDatabase;
import com.wob.workout.data.entity.Exercise;
import com.wob.workout.data.entity.ExerciseSession;
import com.wob.workout.data.entity.MediaType;
import com.wob.workout.data.entity.Workout;
import com.wob.workout.data.entity.WorkoutExerciseRow;
import com.wob.workout.data.entity.WorkoutSession;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * The backup's data document: the stored rows of exercises, workouts with their places,
 * and the history, as JSON. Rows keep the ids they had on the phone that wrote them; they
 * only serve to tie the rows of the document together.
 */
final class BackupDocument {

    final List<Exercise> exercises = new ArrayList<>();
    final List<Workout> workouts = new ArrayList<>();
    final List<WorkoutExerciseRow> workoutExercises = new ArrayList<>();
    final List<WorkoutSession> workoutSessions = new ArrayList<>();
    final List<ExerciseSession> exerciseSessions = new ArrayList<>();

    /** Everything the database holds that belongs in a backup. */
    static BackupDocument of(AppDatabase db) {
        BackupDocument document = new BackupDocument();
        document.exercises.addAll(db.exerciseDao().getAll());
        document.exercises.sort((a, b) -> Long.compare(a.id, b.id));
        document.workouts.addAll(db.workoutDao().getAll());
        document.workouts.sort((a, b) -> Long.compare(a.id, b.id));
        document.workoutExercises.addAll(db.workoutExerciseDao().getAll());
        document.workoutSessions.addAll(db.workoutSessionDao().getAll());
        document.exerciseSessions.addAll(db.exerciseSessionDao().getAll());
        return document;
    }

    String toJson(int formatVersion) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("formatVersion", formatVersion);

        JSONArray exerciseArray = new JSONArray();
        for (Exercise exercise : exercises) {
            exerciseArray.put(new JSONObject()
                    .put("id", exercise.id)
                    .put("title", orNull(exercise.title))
                    .put("description", orNull(exercise.description))
                    .put("mediaFile", orNull(exercise.mediaFile))
                    .put("mediaType", orNull(
                            exercise.mediaType != null ? exercise.mediaType.name() : null))
                    .put("framingZoom", orNull(exercise.framingZoom))
                    .put("framingOffsetX", orNull(exercise.framingOffsetX))
                    .put("framingOffsetY", orNull(exercise.framingOffsetY)));
        }
        json.put("exercises", exerciseArray);

        JSONArray workoutArray = new JSONArray();
        for (Workout workout : workouts) {
            workoutArray.put(new JSONObject()
                    .put("id", workout.id)
                    .put("name", orNull(workout.name))
                    .put("transitionSeconds", orNull(workout.transitionSeconds))
                    .put("countdownEnabled", workout.countdownEnabled));
        }
        json.put("workouts", workoutArray);

        JSONArray placeArray = new JSONArray();
        for (WorkoutExerciseRow place : workoutExercises) {
            placeArray.put(new JSONObject()
                    .put("workoutId", place.workoutId)
                    .put("exerciseId", orNull(place.exerciseId))
                    .put("position", place.position)
                    .put("durationSeconds", place.durationSeconds));
        }
        json.put("workoutExercises", placeArray);

        JSONArray sessionArray = new JSONArray();
        for (WorkoutSession session : workoutSessions) {
            sessionArray.put(new JSONObject()
                    .put("id", session.id)
                    .put("workoutId", session.workoutId)
                    .put("workoutName", orNull(session.workoutName))
                    .put("startedAt", session.startedAt)
                    .put("finishedAt", session.finishedAt)
                    .put("activeDurationSeconds", session.activeDurationSeconds)
                    .put("completed", session.completed)
                    .put("restSeconds", orNull(session.restSeconds)));
        }
        json.put("workoutSessions", sessionArray);

        JSONArray exerciseSessionArray = new JSONArray();
        for (ExerciseSession exerciseSession : exerciseSessions) {
            exerciseSessionArray.put(new JSONObject()
                    .put("sessionId", exerciseSession.sessionId)
                    .put("exerciseId", exerciseSession.exerciseId)
                    .put("exerciseTitle", orNull(exerciseSession.exerciseTitle))
                    .put("position", exerciseSession.position)
                    .put("plannedDurationSeconds", exerciseSession.plannedDurationSeconds)
                    .put("actualDurationSeconds", exerciseSession.actualDurationSeconds)
                    .put("startedAt", exerciseSession.startedAt)
                    .put("finishedAt", exerciseSession.finishedAt));
        }
        json.put("exerciseSessions", exerciseSessionArray);
        return json.toString();
    }

    /** The format version a document says it has. */
    static int formatVersionOf(JSONObject json) throws JSONException {
        return json.getInt("formatVersion");
    }

    /** Reads a document of a format version this app knows.
     *  @throws JSONException if anything it needs is missing or of the wrong kind */
    static BackupDocument fromJson(JSONObject json) throws JSONException {
        BackupDocument document = new BackupDocument();

        JSONArray exerciseArray = json.getJSONArray("exercises");
        for (int i = 0; i < exerciseArray.length(); i++) {
            JSONObject item = exerciseArray.getJSONObject(i);
            Exercise exercise = new Exercise();
            exercise.id = item.getLong("id");
            exercise.title = stringOrNull(item, "title");
            exercise.description = stringOrNull(item, "description");
            exercise.mediaFile = stringOrNull(item, "mediaFile");
            String mediaType = stringOrNull(item, "mediaType");
            exercise.mediaType = mediaType != null ? mediaTypeOf(mediaType) : null;
            exercise.framingZoom = floatOrNull(item, "framingZoom");
            exercise.framingOffsetX = floatOrNull(item, "framingOffsetX");
            exercise.framingOffsetY = floatOrNull(item, "framingOffsetY");
            document.exercises.add(exercise);
        }

        JSONArray workoutArray = json.getJSONArray("workouts");
        for (int i = 0; i < workoutArray.length(); i++) {
            JSONObject item = workoutArray.getJSONObject(i);
            Workout workout = new Workout();
            workout.id = item.getLong("id");
            workout.name = stringOrNull(item, "name");
            workout.transitionSeconds = item.isNull("transitionSeconds")
                    ? null : item.getInt("transitionSeconds");
            workout.countdownEnabled = item.optBoolean("countdownEnabled", true);
            document.workouts.add(workout);
        }

        JSONArray placeArray = json.getJSONArray("workoutExercises");
        for (int i = 0; i < placeArray.length(); i++) {
            JSONObject item = placeArray.getJSONObject(i);
            WorkoutExerciseRow place = new WorkoutExerciseRow();
            place.workoutId = item.getLong("workoutId");
            place.exerciseId = item.isNull("exerciseId") ? null : item.getLong("exerciseId");
            place.position = item.getInt("position");
            place.durationSeconds = item.getInt("durationSeconds");
            document.workoutExercises.add(place);
        }

        JSONArray sessionArray = json.getJSONArray("workoutSessions");
        for (int i = 0; i < sessionArray.length(); i++) {
            JSONObject item = sessionArray.getJSONObject(i);
            WorkoutSession session = new WorkoutSession();
            session.id = item.getLong("id");
            session.workoutId = item.getLong("workoutId");
            session.workoutName = stringOrNull(item, "workoutName");
            session.startedAt = item.getLong("startedAt");
            session.finishedAt = item.getLong("finishedAt");
            session.activeDurationSeconds = item.getInt("activeDurationSeconds");
            session.completed = item.getBoolean("completed");
            session.restSeconds = item.isNull("restSeconds") ? null : item.getInt("restSeconds");
            document.workoutSessions.add(session);
        }

        JSONArray exerciseSessionArray = json.getJSONArray("exerciseSessions");
        for (int i = 0; i < exerciseSessionArray.length(); i++) {
            JSONObject item = exerciseSessionArray.getJSONObject(i);
            ExerciseSession exerciseSession = new ExerciseSession();
            exerciseSession.sessionId = item.getLong("sessionId");
            exerciseSession.exerciseId = item.getLong("exerciseId");
            exerciseSession.exerciseTitle = stringOrNull(item, "exerciseTitle");
            exerciseSession.position = item.getInt("position");
            exerciseSession.plannedDurationSeconds = item.getInt("plannedDurationSeconds");
            exerciseSession.actualDurationSeconds = item.getInt("actualDurationSeconds");
            exerciseSession.startedAt = item.getLong("startedAt");
            exerciseSession.finishedAt = item.getLong("finishedAt");
            document.exerciseSessions.add(exerciseSession);
        }
        return document;
    }

    private static Object orNull(Object value) {
        return value != null ? value : JSONObject.NULL;
    }

    private static String stringOrNull(JSONObject item, String key) throws JSONException {
        return item.isNull(key) ? null : item.getString(key);
    }

    private static Float floatOrNull(JSONObject item, String key) throws JSONException {
        return item.isNull(key) ? null : (float) item.getDouble(key);
    }

    private static MediaType mediaTypeOf(String name) throws JSONException {
        try {
            return MediaType.valueOf(name);
        } catch (IllegalArgumentException e) {
            throw new JSONException("Unknown media type " + name);
        }
    }
}
