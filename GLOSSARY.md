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
A named, ordered sequence of exercises, each with its own duration, with rests placed between them.
_Avoid_: Routine, plan, training

**Workout exercise**:
One exercise's place inside a workout: its position and how long it lasts there.
_Avoid_: Step, item, entry

**Rest**:
A deliberate pause placed in a workout between two exercises, with its own duration and no exercise. It takes the place of the transition on both sides and beeps its last 3 seconds like one. A rest that ends up at the start or end, or right after another rest, is skipped. Time spent resting is shown on its own and does not count towards a workout session's duration. Not to be confused with pausing a running workout session.
_Avoid_: Pause, break

**Transition**:
The short, automatic gap between two exercises of a workout that follow each other directly, with no rest between them. It lasts as long as the workout says, or the default transition if the workout says nothing; 0 seconds means straight on. The last 3 seconds before every exercise beep once per second, reaching back into the running exercise when the transition is shorter than that.
_Avoid_: Break, rest

**Default transition**:
The transition length, chosen in the settings, that every workout uses unless it sets its own. Changing it changes every workout that sets none.
_Avoid_: Global transition, standard pause

**Reminder**:
A notification at a set time on chosen weekdays that prompts a workout. Several can exist, each switched on or off on its own; it stays silent on a day that already has a completed workout session, and in a week whose weekly goal is already reached.
_Avoid_: Alarm, notification, alert

**Weekly goal**:
The number of training days to reach in a week, Monday to Sunday. A training day is a day with at least one completed workout session; aborted ones don't count, and several on one day count once. There is none until one is set, and it can be removed again.
_Avoid_: Target, quota

**Streak**:
The number of weeks in a row, up to the current one, in which the weekly goal was reached, each week measured against the goal that applied then. The current week doesn't break it while it is still running; a week without a weekly goal does, so a goal set again starts it afresh.
_Avoid_: Run, chain

**Backup**:
One file holding all exercises with their demonstrations, all workouts and the history, made and read back by hand. Reading it back either replaces everything on the phone or adds its contents alongside, as copies, even of what is already there. Settings and reminders are not part of it.
_Avoid_: Export, sync

**Workout session**:
One run through a workout, from start until it is completed or aborted.
_Avoid_: Run, attempt, training session

**Exercise session**:
What happened to one exercise during a workout session: planned versus actual duration.
_Avoid_: Set, rep

**Skipped exercise**:
An exercise session that ended before its planned duration was up.
_Avoid_: Incomplete, missed

**Recent workouts**:
The up to three workouts with the newest workout session, aborted ones included. While fewer than three workouts have been run, the most recently created ones fill the remaining places.
_Avoid_: Favourites, last used

**History**:
The list of workout sessions, newest first. Survives deletion of the workout it was recorded against.
_Avoid_: Log, archive

**Starter set**:
The exercises and the workout created once when a fresh installation is first launched. Afterwards they are ordinary exercises and an ordinary workout.
_Avoid_: Default exercises, preset, seed data
