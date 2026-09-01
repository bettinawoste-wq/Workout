# Workout — Context

Local-only Android app (Java) for building custom workouts, running them with a
timer/audio-cued player, and reviewing past sessions. No backend — everything
lives in an on-device Room database.

## Domain

- **Exercise** — a single movement, with an optional photo/video demo (`MediaStorageManager`
  copies the picked file into app storage; `MediaThumbnailLoader` handles EXIF rotation and
  video-frame preview).
- **Workout** — a named, ordered list of exercises (`WorkoutExercise` is the join row,
  carrying `durationSeconds` and position). Built and reordered in `WorkoutEditFragment`.
- **WorkoutSession / ExerciseSession** — a historical record of one run through a workout:
  when it started/finished, whether it was completed or aborted, and the actual vs. planned
  duration per exercise. Written by `PlayerFragment` as the player runs, read by the
  `ui/history` screens. `WorkoutSession.workoutId` is deliberately **not** a cascading foreign
  key — deleting a `Workout` must not delete the history recorded against it.

## Architecture

- **Dependency wiring**: a single hand-written `AppContainer` (see
  [`docs/adr/0001-manual-di-container.md`](docs/adr/0001-manual-di-container.md)), built once in
  `WorkoutApplication` and reached via `((WorkoutApplication) context.getApplicationContext()).getAppContainer()`.
  No DI framework — deliberately the smallest mechanism that fits a single-developer app this size.
- **MVVM migration is in progress, not finished.** `WorkoutListFragment`, `PlayerFragment`,
  `WorkoutEditFragment`, and `ExerciseListFragment` have been migrated to the new pattern so far.
  `WorkoutListViewModel` is a thin passthrough over `WorkoutSummaryRepository`'s reactive
  `LiveData` (built via `LiveDataExecutor` running on `AppContainer.ioExecutor`); `PlayerViewModel`
  owns the player's state machine, a consolidated `PlayerTimer`, and a standalone
  `PlayerHistoryTracker` for session bookkeeping; `WorkoutEditViewModel` deliberately does *not*
  use a reactive Room query — it re-runs a manual load-and-reload after each of its own writes,
  plus on every view creation (to pick up changes made elsewhere, e.g. an Exercise deleted from
  the catalog while this screen sat on the back stack) — see the class doc on
  `WorkoutEditViewModel` for the full rationale; `ExerciseListViewModel` is the same thin-passthrough
  shape as `WorkoutListViewModel`, over `ExerciseRepository.observeAll()` (a plain reactive query,
  no relation/mapping needed). The rest of the screens (`ExerciseEditFragment`, the `ui/history`
  screens) still construct repositories directly and call them synchronously — that's known,
  tracked incremental debt, not an oversight. See GitHub issue #1 ("Lightweight MVVM Migration")
  for the map of remaining screens.
- **Persistence**: Room, with explicit versioned `Migration`s in `AppDatabase` — never
  `fallbackToDestructiveMigration()`. `DatabaseProvider` still calls `.allowMainThreadQueries()`
  (flagged in-code with a TODO); this goes away as more screens move off synchronous
  repository calls onto the `LiveDataExecutor` pattern.

## Consumer rules

- Read this file before making architectural changes; treat it as the source of truth over
  inferring conventions from code alone.
- A new architectural decision goes in `docs/adr/`, not just a commit message — see
  `docs/agents/domain.md`.
- Issues are tracked in GitHub (`docs/agents/issue-tracker.md`).
