# 3. Non-destructive framing of exercise photos

## Status

Accepted

## Context

Exercise photos come in any aspect ratio, but the picture frame has one fixed shape (3:2). When editing an exercise, the user decides how much of the edge is cut off or how much margin is left (see **Framing** in `GLOSSARY.md`), and wants to be able to come back and adjust that later.

## Decision

Keep the original photo file untouched and store the framing (zoom and position) on the exercise. Every place that shows the photo applies the stored framing when drawing it. An exercise without stored framing is shown filling the frame, centred.

## Considered Options

Rendering a new, already-cropped file when saving would need no schema change and leave every display site trivially simple, and it would also cap the stored image size. It was rejected because whatever gets cut off is gone, so a framing could never be widened again without picking the photo anew.

## Consequences

- The stored numbers must be relative to the photo and the frame, not pixels on one device, or the same framing would look different on another phone.
- Replacing an exercise's photo resets its framing.
- Originals stay at full size on disk, so display sites must downsample when loading them.
- Videos have no framing for now.
