package com.wob.workout.data.converter;

import androidx.room.TypeConverter;
import com.wob.workout.data.entity.MediaType;

public class MediaTypeConverter {

    @TypeConverter
    public static MediaType fromString(String value) {

        if (value == null) {
            return null;
        }

        return MediaType.valueOf(value);
    }

    @TypeConverter
    public static String toString(MediaType type) {

        if (type == null) {
            return null;
        }

        return type.name();
    }
}