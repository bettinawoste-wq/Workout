# Workout — Context

Local-only Android app (Java) for building custom workouts, running them with a
timer/audio-cued player, and reviewing past sessions. No backend — everything
lives in an on-device Room database.

## Domain

- **Exercise** — a single movement, with an optional photo/video demo (`MediaStorageManager`
  copies the picked file into app storage; `MediaThumbnailLoader` handles EXIF rotation and
  video-frame preview).
- **Workout** — a named, ordered list of exercises and rests. A **workout exercise** is the
  immutable value `data.WorkoutExercise`: the exercise, its duration and the stored row's id
  (0 while unsaved); its position is its place in the list. A **rest** is the same value
  without an exercise (`isRest()`), stored in the same table with no exercise reference.
  `WorkoutExercise.isSkippedRest` is the one rule for which rests run: only one right after
  an exercise with another exercise somewhere after it, so rests at either end and rests
  right after another rest are skipped (the editor greys them out, the player passes them
  over; nothing removes them). Exercise counts, workout lengths and whether a workout can
  start count exercises only. The stored row is the Room entity
  `WorkoutExerciseRow` (table `WorkoutExercise`, carrying `durationSeconds`, `position` and
  an `exerciseId` that is null for a rest).
  Both the editor and the player load a workout's exercises through one method,
  `WorkoutRepository.getWorkoutExercises`, built on the `WorkoutWithExercises` relation; it is
  the only place that leaves out a workout exercise whose exercise is missing, and it keeps
  rests. Built and
  reordered in `WorkoutEditFragment`, which works on a `WorkoutDraft`: the draft owns its
  workout exercises (a change replaces a value, a drag step moves one from a position to
  another; the list adapter only renders), and every change stays in it until "Speichern"
  writes it in one transaction (`WorkoutRepository.saveWithExercises`); Back with unsaved
  changes asks before throwing it away, and a new workout is only created by saving.
- **WorkoutSession / ExerciseSession** — a historical record of one run through a workout:
  when it started/finished, whether it was completed or aborted, and the actual vs. planned
  duration per exercise. Its duration counts exercise time only; the actual rest time,
  pauses left out, is stored on its own (`WorkoutSession.restSeconds`, null for sessions
  recorded before rests existed) and shown as "Pausen: 1:30" on the completion screen and
  the history detail; rests get no exercise session. Written by the player's
  `WorkoutSessionEngine` as it runs, read by the
  `ui/history` screens. `WorkoutSession.workoutId` is deliberately **not** a cascading foreign
  key — deleting a `Workout` must not delete the history recorded against it.
- **Weekly goal and streak** — the weekly goal is not a preference but data with a history
  of changes (`WeeklyGoalChange`: the Monday of the week it applies from, as an epoch day, and
  the training days, null once removed; one per week, a later change in the same week
  replaces it), read and set through `WeeklyGoalRepository` from the settings screen's
  Training section. The pure `training.WeeklyProgress` takes the workout sessions, the goal's
  changes, the current time and the time zone and answers the current week's training days
  and goal, the streak, whether the home screen shows its line, and whether a reminder stays
  silent today. A training day is a local day on which a completed workout session started;
  weeks run Monday to Sunday. `HomeViewModel` observes the history and the goal's changes
  and works the progress out again on every change, so deleting a workout session updates
  the line. `WeeklyProgressTest` (JVM) covers the rules.

## Architecture

- **Dependency wiring**: a single hand-written `AppContainer` (see
  [`docs/adr/0001-manual-di-container.md`](docs/adr/0001-manual-di-container.md)), built once in
  `WorkoutApplication` and reached via `((WorkoutApplication) context.getApplicationContext()).getAppContainer()`.
  No DI framework — deliberately the smallest mechanism that fits a single-developer app this size.
- **MVVM migration: every screen on the map is now migrated.** `WorkoutListFragment`, `PlayerFragment`,
  `WorkoutEditFragment`, `ExerciseListFragment`, `ExerciseEditFragment`, `HistoryListFragment`, and
  `HistoryDetailFragment` all follow the new pattern. `HomeFragment` was not on the map (pure
  navigation then) and got `HomeViewModel` later, with issue #44. `WorkoutListViewModel` is a thin
  passthrough over `WorkoutSummaryRepository`'s reactive Room `LiveData` (a `@Relation` query mapped
  via `Transformations.map`); `HomeViewModel` is the same, over `observeRecent()` for the home
  screen's recent workouts; `PlayerViewModel` only adapts the workout session engine (see
  **Player** below) to LiveData;
  `WorkoutEditViewModel` deliberately does *not* use a reactive Room query — it loads once into
  its draft and, on every view creation, updates the draft's exercises from the catalog (to pick
  up changes made elsewhere, e.g. an Exercise deleted while this screen sat on the back stack)
  — see the class doc on `WorkoutEditViewModel` for the full rationale; `ExerciseListViewModel`
  is the same thin-passthrough shape as `WorkoutListViewModel`, over `ExerciseRepository.observeAll()`
  (a plain reactive query, no relation/mapping needed); `ExerciseEditViewModel` follows the same
  one-shot-load shape as `WorkoutEditViewModel` (manual load via `LiveDataExecutor`, not a
  reactive query — this screen's edits are the only source of change while it's open) and adds
  the app's first one-shot LiveData event, `Event<T>` (`data/Event.java`), for `mediaError` so a
  Snackbar doesn't re-fire on rotation; `HistoryListViewModel` is the same thin-passthrough shape
  again, over `WorkoutHistoryRepository.observeHistory()`, which fixed that repository's N+1
  per-session exercise-count query the same way `WorkoutSummaryRepository` did in ticket #3 — a
  `@Transaction`/`@Relation` query (`WorkoutSessionWithExercises`) batched into one `LiveData`,
  folded into `WorkoutSessionSummary` rows via `Transformations.map()`; swipe-to-delete stays
  Fragment-side (`ItemTouchHelper` + a confirmation dialog), and on confirm it only calls
  `HistoryListViewModel.delete()` — Room's invalidation re-emits the list and the observer's
  `adapter.setData(...)` naturally drops the row, no manual `adapter.removeItem()` needed; only
  a cancelled swipe still calls `adapter.notifyItemChanged()` directly, to reverse the swipe
  animation without touching any data; `HistoryDetailViewModel` is a read-only one-shot load
  (no reactive `LiveData` — only one Fragment is visible at a time in this single-activity app,
  so nothing else can mutate a session's data while its detail screen is open, same reasoning as
  `WorkoutEditFragment`'s exercise list), combining `getSession()`/`getExercises()` into one
  `LiveDataExecutor.runOnExecutorOnce` round-trip via a small `HistoryDetail` holder, mirroring
  `ExerciseEditViewModel`'s `LoadResult` pattern. See GitHub issue #1 ("Lightweight MVVM
  Migration") for the full history of the map.
- **Navigation**: one `ui.Navigator`, a plain class over the activity's `FragmentManager`
  (`Navigator.of(fragment)`), makes every screen change: `open` a screen on top, `close` the
  current one, `replace` the current one so back still leads where it did ("Nochmal" in the
  player, "Duplizieren" in the workout editor), and `showFirst` for the home screen. Fragments
  never touch the `FragmentManager` themselves. Input rules live in drafts and view models, not
  in fragments: the workout draft takes a duration as typed and answers whether it was a valid
  number of seconds (and likewise a transition length of 0 to 30 seconds), the exercise editor's view model rejects a missing title, and both
  editors' view models name the workout or exercise in the delete confirmation.
- **Player**: the rules of a workout session live in `WorkoutSessionEngine`, plain Java
  without Android: the phases (countdown before the first exercise, exercise, transition,
  rest, finished), next / previous / pause, when it beeps (the last 3 seconds before every
  exercise, reaching back into the running exercise when the transition or rest is shorter;
  a transition of 0 goes straight on), the remaining time of the whole workout,
  which positions are done, and the start / pause / resume / close-out timing of exercise
  sessions. It takes a workout's exercises and rests and keeps the exercises as its
  positions, with the length of the rest that runs after each: a rest takes the place of the
  transition, "next" in a rest ends it, "previous" goes to the exercise before it, and
  stepping between exercises passes over rests; it measures the actual rest time, which goes
  into the completion summary and `SessionHistory.finishSession`. It has two seams (ADR 0005): `SessionTime`, the countdown and clock it runs on
  (`AndroidSessionTime` in the app, a fake in JVM tests), and `SessionHistory`, where it writes
  the workout session and its exercise sessions (`DatabaseSessionHistory` in the app, on the
  single-threaded io executor; in memory in tests). `WorkoutSessionEngineTest` covers the rules
  on fake time; the instrumented `PlayerViewModelTest` keeps one run through the real wiring.
  The engine reports what is going on as a `SessionState` (phase, time left, the exercise shown
  or previewed, whether stepping and pausing are possible, the summary once finished), not how
  it looks; the pure `PlayerScreen` turns that into the screen's texts and switches, and
  `PlayerFragment` only puts them in place. Sounds (`PlayerSound`: beep, start, success) come
  from the engine as events; `PlayerViewModel` drops them while the sound setting is off and
  hands the rest to the screen as one-shot `Event`s, so a rotation never plays one again.
- **Persistence**: Room, with explicit versioned `Migration`s in `AppDatabase` — never
  `fallbackToDestructiveMigration()`. Room runs without `allowMainThreadQueries()` (removed in
  #22, the last step of the MVVM migration): every database access happens off the main thread,
  either on `AppContainer.ioExecutor` (one-shot reads and writes via `LiveDataExecutor`, and
  `DatabaseSessionHistory`'s history writes) or on Room's own query executor behind reactive
  `LiveData`. ViewModel tests built on `TestAppContainer`'s in-memory database fail with
  `IllegalStateException` if code touches it from the main thread, so a regression shows up there.
- **Settings**: the app-wide choices that aren't data live in a `SharedPreferences` file that
  `AppContainer` opens, one small class per choice: the design (`ui.settings.DesignSetting`,
  System / Light / Dark, a fresh install follows the system), the player's sound switch
  (`SoundSetting`, issue #71, on by default) and vibration (`VibrationSetting`, off by
  default) and the default transition (`DefaultTransitionSetting`, 0 to 30 seconds, 3 on a
  fresh install). A workout's own transition is optional (`Workout.transitionSeconds`, null
  means "follows the default"); the workout draft owns that choice, and `PlayerViewModel`
  resolves it to plain seconds before starting the engine (`DefaultTransitionSetting.resolve`).
  `TestAppContainer` hands the container a test-only file instead (`getSettings()`),
  emptied on open and close. The **settings screen** (`ui.settings.SettingsFragment` with
  `SettingsViewModel`; its Data section has `BackupViewModel`, see **Backup**) is opened from
  a floating gear button on the home screen through the
  navigator. Its sound switch is the same `SoundSetting` as the player toolbar's. The design is
  put in place through the platform's night-mode switch (`Design.apply()`, i.e.
  `AppCompatDelegate.setDefaultNightMode`) once in `WorkoutApplication.onCreate` and again on
  every change, which recreates the open screens. While vibration is on, `PlayerViewModel`
  hands every sound event to the screen a second time, as a vibration `Event`, independent of
  the sound switch; `PlayerVibrator` plays one pattern per `PlayerSound`. The engine knows
  nothing about vibration. The weekly goal (see **Weekly goal and streak**) is the
  exception: it lives in the database, and the screen's Training section sets it with a
  slider from 0 (none) to 7 training days.
- **Backup**: `backup.Backup` (`AppContainer.backup`) writes and reads one ZIP file: a data
  document `data.json` (`BackupDocument`: the stored rows of exercises, workouts with their
  places and rests, workout sessions and exercise sessions, with a format version) plus the
  exercises' media under `media/`. Settings, reminders and the weekly goal are not part of
  it. Reading back first gives a `BackupPreview` (counts, or a `BackupRefused.Reason`: a
  newer format version or an unreadable file); "Ersetzen" (`replaceWith`) deletes all
  exercises, workouts and history first, "Zusammenführen" (`addAlongside`) keeps them. Both
  insert everything with new ids, the file's references remapped, in one transaction; a
  workout session of a workout deleted since gets an id no workout will have (a placeholder
  workout inserted and deleted, since workout ids are never reused). Media are copied under
  new names before the transaction and removed again if it fails; "Ersetzen" removes the
  replaced exercises' media once it holds. It takes the media folder as an argument, so the
  instrumented `BackupTest` runs on the in-memory database and a folder of its own. The
  settings screen's Data section drives it through `ui.settings.BackupViewModel` and the
  system's file dialogs (`CreateDocument` / `OpenDocument`).
- **Schema and migration tests** (ADR 0004): Room exports one schema per database version to
  `app/schemas/`, committed and shipped in the test APK. Every `version` bump commits the new
  JSON, adds its `Migration` to `AppDatabase.ALL_MIGRATIONS` (which `DatabaseProvider`
  registers) and adds a test built on Room's `MigrationTestHelper`, like `Migration4To5Test`.
  `AllMigrationsTest` migrates version 4 — the oldest any release shipped — to the current one.
- **Starter set**: `StarterSet` is a Room `onCreate` callback added in `DatabaseProvider`, so it
  runs only when the database file is first created — existing installs are untouched and
  anything the user deletes stays deleted. Its content (10 exercises, one workout, seconds per
  exercise) lives in `res/values/starter.xml`; the debug variant overrides only the seconds,
  5 instead of 60, in `app/src/debug/res/values/starter.xml`, and debug builds install as
  `com.wob.workout.debug` with their own database. In-memory test databases don't register the
  callback, so they start empty. `StarterResourcesTest` (JVM) pins what release ships and that
  debug overrides nothing else.

## Consumer rules

- Read this file before making architectural changes; treat it as the source of truth over
  inferring conventions from code alone.
- A new architectural decision goes in `docs/adr/`, not just a commit message — see
  `docs/agents/domain.md`.
- Issues are tracked in GitHub (`docs/agents/issue-tracker.md`).
