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
- **MVVM migration: every screen on the map is now migrated.** `WorkoutListFragment`, `PlayerFragment`,
  `WorkoutEditFragment`, `ExerciseListFragment`, `ExerciseEditFragment`, `HistoryListFragment`, and
  `HistoryDetailFragment` all follow the new pattern (`HomeFragment` needed no ViewModel — pure
  navigation, zero state, settled without a ticket). `WorkoutListViewModel` is a thin passthrough over
  `WorkoutSummaryRepository`'s reactive `LiveData` (built via `LiveDataExecutor` running on
  `AppContainer.ioExecutor`); `PlayerViewModel` owns the player's state machine, a consolidated
  `PlayerTimer`, and a standalone `PlayerHistoryTracker` for session bookkeeping;
  `WorkoutEditViewModel` deliberately does *not* use a reactive Room query — it re-runs a manual
  load-and-reload after each of its own writes, plus on every view creation (to pick up changes
  made elsewhere, e.g. an Exercise deleted from the catalog while this screen sat on the back
  stack) — see the class doc on `WorkoutEditViewModel` for the full rationale; `ExerciseListViewModel`
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
- **Persistence**: Room, with explicit versioned `Migration`s in `AppDatabase` — never
  `fallbackToDestructiveMigration()`. `DatabaseProvider` still calls `.allowMainThreadQueries()`
  (flagged in-code with a TODO) — now that every screen is migrated this was expected to be the
  final cleanup step, but it's still blocked: `PlayerViewModel.onCleared()` calls
  `PlayerHistoryTracker.closeOutCurrentExercise()`/`.finish()` synchronously on the main thread
  (reachable via the normal "cancel workout mid-session" path, not just an edge case), so removing
  the flag today would crash with `IllegalStateException: Cannot access database on the main
  thread`. That call needs to move onto `AppContainer.ioExecutor` before this flag can go away —
  tracked as a gap in ticket #4's migration, found while verifying ticket #9.

## Consumer rules

- Read this file before making architectural changes; treat it as the source of truth over
  inferring conventions from code alone.
- A new architectural decision goes in `docs/adr/`, not just a commit message — see
  `docs/agents/domain.md`.
- Issues are tracked in GitHub (`docs/agents/issue-tracker.md`).
