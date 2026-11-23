package com.wob.workout.media;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PictureFrameLayoutTest {

    @Test
    public void fillsTheWidthAtThreeToTwo() {
        assertArrayEquals(new int[] {900, 600}, PictureFrameLayout.frameSize(900, 1000));
    }

    @Test
    public void shrinksAndKeepsItsShapeWhenTheScreenIsTooLow() {
        assertArrayEquals(new int[] {600, 400}, PictureFrameLayout.frameSize(2000, 400));
    }

    @Test
    public void takesAtMostItsShareOfTheScreenHeight() {
        assertEquals(1100, PictureFrameLayout.maxHeight(false, 2000, 1500));
    }

    @Test
    public void aFrameThatFillsTheHeightLeavesRoomBesideItForTheControls() {
        assertEquals(1200, PictureFrameLayout.maxWidth(true, 2000));
        assertEquals(2000, PictureFrameLayout.maxWidth(false, 2000));
    }

    @Test
    public void aFrameThatFillsTheHeightTakesAllItIsGiven() {
        assertEquals(800, PictureFrameLayout.maxHeight(true, 1080, 800));
    }
}
