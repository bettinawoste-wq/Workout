# 1. Manual DI container instead of a DI framework

## Status

Accepted

## Context

The app is migrating screen-by-screen to a `ViewModel` + `LiveData` architecture (see the "Lightweight MVVM Migration" map, [issue #1](https://github.com/bettinawoste-wq/Workout/issues/1)). ViewModels need repositories and a shared background-execution mechanism handed to them, rather than constructing their own dependencies inline the way Fragments do today (`new XRepository(context)`).

## Decision

Introduce a single hand-written `AppContainer` class ([`AppContainer.java`](../../app/src/main/java/com/wob/workout/AppContainer.java)) holding one instance of each repository plus a shared `ExecutorService`. It's constructed once in a new `WorkoutApplication` ([`WorkoutApplication.java`](../../app/src/main/java/com/wob/workout/WorkoutApplication.java)) and reached via `getAppContainer()`. ViewModels are built through one generic `WorkoutViewModelFactory` ([`WorkoutViewModelFactory.java`](../../app/src/main/java/com/wob/workout/WorkoutViewModelFactory.java)) that takes the container, with one branch added per ViewModel as screens migrate.

No DI framework (Dagger, Hilt) was introduced. This is deliberately the smallest mechanism that solves the problem: no annotation processing, no generated code, no new build-time dependency — appropriate for a single-developer app of this size. `AppContainer` is not behind an interface; there is exactly one implementation and no plan for a second.

Full design detail, alternatives considered, and validation (a compiled throwaway prototype) are recorded in [issue #2](https://github.com/bettinawoste-wq/Workout/issues/2)'s resolution comment.

## Consequences

- Fragments/ViewModels obtain dependencies via `((WorkoutApplication) context.getApplicationContext()).getAppContainer()` instead of constructing repositories directly.
- Adding a new repository means adding one field to `AppContainer`; adding a new ViewModel means adding one branch to `WorkoutViewModelFactory`.
- If the app ever grows enough screens/repositories that the factory's branch-per-ViewModel chain or the container's flat field list becomes unwieldy, that's the point to revisit this decision — not before.
