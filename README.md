# Workout

[![CI](https://github.com/bettinawoste-wq/Workout/actions/workflows/ci.yml/badge.svg)](https://github.com/bettinawoste-wq/Workout/actions/workflows/ci.yml)

Workout is an Android app for building your own workouts from your own exercises, running
them with a timer and audio cues, and looking back at what you actually did. It is made for
people who train at home with bodyweight exercises and want a plain, focused tool instead of
a fitness platform. Everything stays on the phone in a Room database: no account, no backend,
no tracking, and nothing to lose when a service shuts down. The interface is in German.

| Home | Workout editor | Player | History | History detail | Settings |
| :---: | :---: | :---: | :---: | :---: | :---: |
| <img src="docs/screenshots/home.png" width="190" alt="Home screen"> | <img src="docs/screenshots/workout-edit.png" width="190" alt="Editing the starter workout, with a rest before its last exercise"> | <img src="docs/screenshots/player.png" width="190" alt="Player during a rest, previewing the next exercise"> | <img src="docs/screenshots/history.png" width="190" alt="History with completed and aborted sessions"> | <img src="docs/screenshots/history-detail.png" width="190" alt="A completed session with its exercises and rest time"> | <img src="docs/screenshots/settings.png" width="190" alt="Settings with design, sound, vibration, default transition and backup"> |

## Features

- **Exercises** — title, description and an optional photo or video; a photo is framed by zooming
  with two fingers and moving with one, the file untouched; without a picture, a placeholder image.
- **Workouts** — a named, ordered list of exercises with a duration each and rests between them,
  reordered by drag and drop, kept as a draft until "Speichern" (leaving asks before discarding
  changes), duplicable, and shown with a thumbnail of every exercise. A rest that would not run
  (at either end, or right after another rest) is greyed out with "wird übersprungen".
- **Player** — countdowns, a soft start tone, a transition of 0 to 30 seconds (a default in the settings, or the workout's own) with 3-2-1 beeps before every exercise, the coming exercise darkened, skip and
  back; rests as their own phase with the next exercise previewed; a bar of exercise time left in the workout; a chime and a summary of total time, skipped exercises and rest time.
- **History** — every workout session, completed or aborted, with planned vs. actual duration per
  exercise and the rest time on its own; browse and delete sessions, which survive deleting the workout they were recorded against.
- **Starter set** — a fresh install comes with 10 bodyweight exercises and a full-body workout at one
  minute per exercise; afterwards they are ordinary data you can edit or delete.
- **Settings** — a gear button on the home screen opens them: the design (System, Light or Dark,
  applied at once), the player's sound switch, and vibration at every sound, one pattern per sound.
- **Backup** — the settings export all exercises with their photos and videos, the workouts and
  the history to one ZIP file anywhere the system's file dialog reaches; importing one shows
  what it holds, then replaces everything or adds it alongside as copies.
- **Design** — Material 3 with a petrol palette, light or dark as the system or the settings say.

## Architecture

Every screen follows the same MVVM layering; the player is the screen with the most logic of its own.

```mermaid
flowchart TD
    AC["AppContainer (wiring)"]

    subgraph screens["Every screen"]
        Fragment["Fragment (view, navigation)"] --> ViewModel["ViewModel (LiveData state)"]
    end

    subgraph player["Player: the special case"]
        PF["PlayerFragment (sound, video)"] --> PVM["PlayerViewModel (state machine)"]
        PVM --> Timer["PlayerTimer"]
        PVM --> Tracker["PlayerHistoryTracker"]
    end

    AC -.-> ViewModel
    AC -.-> PVM
    ViewModel --> Repository["Repository"]
    PVM --> Repository
    Tracker --> Repository
    Repository --> DAO["DAO"]
    DAO --> Room[("Room: AppDatabase")]
```

- ViewModels touch the database only off the main thread.
- Migrations are tested from version 4, the oldest schema any release shipped
  ([ADR 0004](docs/adr/0004-exported-room-schemas.md)).

[`CONTEXT.md`](CONTEXT.md) describes the architecture in detail.

## Development approach

This app was built by its owner together with [Claude Code](https://claude.com/claude-code),
with a clear split of roles.

**The owner** brings the product idea and the requirements, makes the decisions in grillings,
keeps the domain model ([`GLOSSARY.md`](GLOSSARY.md) and the [ADRs](docs/adr/)), cuts the
issues, reviews and accepts every change before it lands, tests on the device, and cuts the releases.

**Claude Code** implements test-first from those issues and reviews the code as a second check.

The workflow:

1. **Idea** — something the app should do, or do better.
2. **Grilling** — questions until the requirements and domain terms are settled; a lasting
   decision becomes an ADR.
3. **Issue** — the outcome, with acceptance criteria.
4. **TDD** — red → green against those criteria.
5. **Review** — Claude Code's code review as a second check, then the owner's review.
6. **PR** — one commit per issue, rebased onto `master` once CI is green and the owner accepts
   it, so the history stays a straight line.

The full flow, on framing exercise photos: the grilling's decision became
[ADR 0003](docs/adr/0003-non-destructive-framing.md), then
[issue #31](https://github.com/bettinawoste-wq/Workout/issues/31), then
[PR #36](https://github.com/bettinawoste-wq/Workout/pull/36).

More from this repo:

- Issue to PR, for a workout editor without a save button:
  [issue #45](https://github.com/bettinawoste-wq/Workout/issues/45) →
  [PR #49](https://github.com/bettinawoste-wq/Workout/pull/49).
- The MVVM migration: [issue #1](https://github.com/bettinawoste-wq/Workout/issues/1) as the
  map, [issue #2](https://github.com/bettinawoste-wq/Workout/issues/2) for the wiring, and one
  issue per screen, such as [issue #4](https://github.com/bettinawoste-wq/Workout/issues/4) for
  the player, built in commits straight on `master`.

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

## License

This project is licensed under the MIT License; see [`LICENSE`](LICENSE).
