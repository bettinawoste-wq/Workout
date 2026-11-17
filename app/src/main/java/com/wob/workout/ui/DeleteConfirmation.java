package com.wob.workout.ui;

/** What a delete confirmation calls the workout or exercise it is about to delete. */
public final class DeleteConfirmation {

    private DeleteConfirmation() {
    }

    /** The name as it stands in the editor's field, or the stored one while the field is
     *  empty; empty if there is neither. */
    public static String name(String fieldText, String storedName) {
        if (fieldText != null && !fieldText.isEmpty()) return fieldText;
        return storedName != null ? storedName : "";
    }
}
