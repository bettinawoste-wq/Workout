# 5. The player runs on injectable time

## Status

Accepted. Reverses what wayfinder ticket #4 avoided.

## Context

When the player's state machine moved from `PlayerFragment` into `PlayerViewModel` (wayfinder ticket #4), its tests stayed instrumented: the ViewModel created a real `CountDownTimer` and read `System.currentTimeMillis()` for the history, and ticket #4 deliberately avoided an injectable timer in production code. The tests drove the state machine by calling the timer callbacks by hand while the real timer kept running, held the few-second countdowns for only a few milliseconds, and slept for seconds to check pauses. They needed the phone, took real time and could still be overtaken by the real timer on a slow device.

Issue #90 moves the player's rules (phases, stepping and pausing, beeps, the workout's remaining time, which positions are done, the timing of exercise sessions) into one plain-Java module, `WorkoutSessionEngine`, so that they can be tested on the JVM.

## Decision

The engine gets its time through a seam, `SessionTime`: one countdown at a time that can pause and resume, and the clock its exercise sessions are timed by. The app uses `AndroidSessionTime` (a `CountDownTimer` and the system clock); tests use a fake that only moves when the test advances it, ticking like a `CountDownTimer`.

Writing the workout session and its exercise sessions is a second seam, `SessionHistory`: `DatabaseSessionHistory` in the app (the history tracker's former database path, on the single-threaded io executor), an in-memory record in tests.

`PlayerViewModel` builds the engine with the two production adapters and only forwards to it.

## Consequences

- The player's rules are covered by `WorkoutSessionEngineTest` on the JVM, exactly and in milliseconds, including pauses of minutes. The instrumented side keeps one slim test that runs a short workout through the real wiring.
- Production code has an interface it did not need before, with one production implementation. That is the price ticket #4 avoided; it is paid because the alternative was slow, timing-dependent tests on the phone only.
- The engine emits a tick itself whenever it enters a phase, rather than waiting for the timer's first tick, so it behaves the same on both kinds of time.
