# Workout

[![CI](https://github.com/bettinawoste-wq/Workout/actions/workflows/ci.yml/badge.svg)](https://github.com/bettinawoste-wq/Workout/actions/workflows/ci.yml)

A local-only Android app for building custom workouts, running them with a timer and
audio cues, and reviewing past sessions afterward. Everything is stored on-device in
Room — no account, no backend. The interface is in German.

| Home | Workout editor | Player | History |
| :---: | :---: | :---: | :---: |
| <img src="docs/screenshots/home.png" width="190" alt="Home screen"> | <img src="docs/screenshots/workout-edit.png" width="190" alt="Editing the starter workout"> | <img src="docs/screenshots/player.png" width="190" alt="Player running an exercise"> | <img src="docs/screenshots/history.png" width="190" alt="History with completed and aborted sessions"> |

## Features

- **Exercises** — title, description and an optional photo or video. Photos are framed in
  the editor: zoom with two fingers and move with one to decide how much of the edge is cut
  off; the original file stays untouched. Exercises without a picture show a placeholder.
- **Workouts** — a named, ordered list of exercises with a duration each, reordered by
  drag-and-drop. The editor saves every change as you make it, and a workout can be
  duplicated from its menu. The workout list shows a thumbnail of every exercise.
- **Player** — counts down into the first exercise and out of every exercise, marks each
  start with a soft tone, and keeps the pause between exercises quiet. While a countdown
  runs, the coming exercise is shown darkened with its title. A progress bar shows the
  exercise time left in the whole workout. Exercises can be skipped or revisited; a
  finished workout ends with a chime and a summary of total time and skipped exercises.
- **History** — every run is recorded (completed or aborted, with per-exercise actual vs.
  planned duration), browsable and deletable independently of whether the source workout
  still exists.
- **Starter set** — a fresh install comes with 10 bodyweight exercises and a ready-made
  full-body workout (one minute per exercise); afterwards they're ordinary data you can
  edit or delete.
- **Design** — Material 3 with a petrol palette, following the system's light or dark mode.

## Tech stack

- Java, Android Gradle Plugin, min SDK 24 / target SDK 37
- Room (SQLite) for persistence, with explicit versioned migrations
- Media3 (ExoPlayer) for video playback
- View Binding, ViewModel + LiveData; a small manual DI container instead of a framework
  (see [`docs/adr/0001-manual-di-container.md`](docs/adr/0001-manual-di-container.md))

## Building

```bash
./gradlew assembleDebug
```

Install to a connected device/emulator:

```bash
./gradlew installDebug
```

Run the unit tests, and the instrumented tests on a connected device or emulator:

```bash
./gradlew testDebugUnitTest
python scripts/device-test.py
```

`device-test.py` installs over the existing debug app and keeps its data, unlike
`./gradlew connectedDebugAndroidTest`, which uninstalls it afterwards.
`python scripts/readme-screenshots.py` renews the screenshots above from a release build.

Signed release APKs are attached to each [GitHub Release](https://github.com/bettinawoste-wq/Workout/releases); how they are built
is described in [`docs/releasing.md`](docs/releasing.md).

## Project docs

- [`CONTEXT.md`](CONTEXT.md) — domain model and current architecture
- [`docs/releasing.md`](docs/releasing.md) — signing key setup and how to cut a release
- [`GLOSSARY.md`](GLOSSARY.md) — the domain language used in code and issues
- [`docs/adr/`](docs/adr/) — architecture decision records
- [`docs/sounds.md`](docs/sounds.md) — where the app's sounds come from
- [`docs/agents/`](docs/agents/) — conventions for AI coding agents working in this repo
