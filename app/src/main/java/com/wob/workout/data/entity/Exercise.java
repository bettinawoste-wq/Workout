package com.wob.workout.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.wob.workout.media.Framing;

@Entity
public class Exercise {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String title;

    public String description;

    public String mediaFile;

    public MediaType mediaType;

    /** All three are null while the photo has no stored framing. */
    public Float framingZoom;

    public Float framingOffsetX;

    public Float framingOffsetY;

    /** @return the stored framing, or null for the default */
    public Framing framing() {
        if (framingZoom == null || framingOffsetX == null || framingOffsetY == null) return null;
        return new Framing(framingZoom, framingOffsetX, framingOffsetY);
    }

    public void storeFraming(Framing framing) {
        framingZoom = framing != null ? framing.zoom : null;
        framingOffsetX = framing != null ? framing.offsetX : null;
        framingOffsetY = framing != null ? framing.offsetY : null;
    }
}
