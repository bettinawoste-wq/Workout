package com.wob.workout.ui;

/** How typed numbers are judged: a whole number within its bounds, or nothing. */
public final class WholeNumber {

    private WholeNumber() {
    }

    /** @return the typed whole number if it lies within the bounds, otherwise null */
    public static Integer within(String typed, int min, int max) {
        int number;
        try {
            number = Integer.parseInt(typed.trim());
        } catch (NumberFormatException e) {
            return null;
        }
        return number >= min && number <= max ? number : null;
    }
}
