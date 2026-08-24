# 2. Test-only seam on DatabaseProvider

## Status

Accepted

## Context

Adding automated tests for `WorkoutEditViewModel` and `PlayerViewModel` (see [issue #10](https://github.com/bettinawoste-wq/Workout/issues/10) through [#13](https://github.com/bettinawoste-wq/Workout/issues/13)) requires constructing a real `AppContainer` backed by an in-memory database. `AppContainer` builds `WorkoutRepository`, `ExerciseRepository`, and `WorkoutExerciseRepository` via `new XRepository(appContext)`, and all three route every call through `DatabaseProvider.get(context)` - a process-wide static singleton (`DatabaseProvider.java`) that lazily builds a real on-disk `workout.db` the first time it's called, with no test seam. Unlike `WorkoutSummaryRepository`/`WorkoutHistoryRepository`, which already expose a `@VisibleForTesting AppDatabase` constructor bypassing `DatabaseProvider` entirely, there was no way to build a real `AppContainer` against an in-memory database.

## Decision

Add `@VisibleForTesting @RestrictTo(RestrictTo.Scope.TESTS) setInstanceForTest(AppDatabase)` and `resetForTest()` to `DatabaseProvider`. A test points the singleton at a fresh in-memory database, constructs a real `AppContainer(context)` against it, and resets the singleton in teardown. `AppContainer` and every repository class stay unchanged - this exercises the exact same `Repository → DatabaseProvider.get()` wiring a Fragment uses in production, rather than adding a second `AppContainer` constructor.

This is a narrow, deliberate exception to the `WorkoutSummaryRepository`/`WorkoutHistoryRepository` precedent of not touching production code for tests, made because those two repositories' `@VisibleForTesting AppDatabase` constructor pattern doesn't help here: `AppContainer` has no way to hand a pre-built `AppDatabase` to `WorkoutRepository`/`ExerciseRepository`/`WorkoutExerciseRepository`, since those three take a `Context` and resolve the database themselves, per-call, via `DatabaseProvider.get()`.

`@RestrictTo(RestrictTo.Scope.TESTS)` (in addition to `@VisibleForTesting`) was added because `@VisibleForTesting` alone is advisory only in Java - it doesn't stop production code from calling a `public` method. `RestrictTo.Scope.TESTS` is enforced by Android Lint, giving this a real (if lint-time, not javac-time) guard rail against a production call site swapping the app's real database at runtime - the same pattern Room itself uses for its own test-only APIs.

## Consequences

- Only a test that constructs its own `AppContainer` (or repository) *after* calling `setInstanceForTest()` is affected. `WorkoutSummaryRepository`/`WorkoutHistoryRepository` cache the `AppDatabase` reference they're given at construction, so an already-existing instance (e.g. the app-wide `AppContainer` `WorkoutApplication` builds at process startup) is not retroactively affected by a later `setInstanceForTest()` call.
- Callers, not `DatabaseProvider`, own closing whatever `AppDatabase` they pass to `setInstanceForTest()` - `resetForTest()` only clears the reference.
- `DatabaseProvider.INSTANCE` remains a single field shared for the whole instrumentation process; a test using this seam must reset it in `@After` regardless of test outcome, or it leaks into whichever test runs next in the same process.
- If a second production consumer of `DatabaseProvider` ever needs runtime database swapping (not just tests), that's a sign this seam should become a real constructor parameter instead - not a reason to relax `RestrictTo.Scope.TESTS`.
