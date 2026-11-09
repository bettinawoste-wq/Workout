# Workout

A personal workout app: you compose workouts from exercises, run them with a timer, and look back at what you actually did.

## Language

**Exercise**:
A single movement with a title, a description and an optional photo or video demonstration.
_Avoid_: Move, activity

**Placeholder image**:
The generic picture shown for an exercise that has no demonstration of its own.
_Avoid_: Default image, fallback

**Picture frame**:
The area of one fixed shape in which an exercise's demonstration is shown.
_Avoid_: Preview, image area

**Framing**:
The choice, made when editing an exercise, of how large its photo appears in the picture frame: how much of the edge is cut off, or how much margin is left around it.
_Avoid_: Crop, zoom

**Workout**:
A named, ordered sequence of exercises, each with its own duration, plus a pause length between exercises and a switch for the start countdown.
_Avoid_: Routine, plan, training

**Workout exercise**:
One exercise's place inside a workout: its position and how long it lasts there.
_Avoid_: Step, item, entry

**Transition**:
The pause between two exercises of a workout, before the next one starts.
_Avoid_: Break, rest

**Workout session**:
One run through a workout, from start until it is completed or aborted.
_Avoid_: Run, attempt, training session

**Exercise session**:
What happened to one exercise during a workout session: planned versus actual duration.
_Avoid_: Set, rep

**Skipped exercise**:
An exercise session that ended before its planned duration was up.
_Avoid_: Incomplete, missed

**History**:
The list of workout sessions, newest first. Survives deletion of the workout it was recorded against.
_Avoid_: Log, archive

**Starter set**:
The exercises and the workout created once when a fresh installation is first launched. Afterwards they are ordinary exercises and an ordinary workout.
_Avoid_: Default exercises, preset, seed data
