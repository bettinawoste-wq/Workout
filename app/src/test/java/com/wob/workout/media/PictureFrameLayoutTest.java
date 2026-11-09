package com.wob.workout.media;

import static org.junit.Assert.assertArrayEquals;

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
}
