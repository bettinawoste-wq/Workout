package com.wob.workout.media;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MediaThumbnailLoaderTest {

    @Test
    public void picturesAtOrBelowTheTargetAreNotDownsampled() {
        assertEquals(1, MediaThumbnailLoader.sampleSizeFor(1536, 1024));
    }

    @Test
    public void aPhonePhotoIsHalvedWithoutDroppingBelowTheTarget() {
        assertEquals(2, MediaThumbnailLoader.sampleSizeFor(4032, 3024));
    }

    @Test
    public void portraitPhotosCountTheirLongestEdge() {
        assertEquals(2, MediaThumbnailLoader.sampleSizeFor(3024, 4032));
    }

    @Test
    public void veryLargePhotosAreReducedFurther() {
        assertEquals(4, MediaThumbnailLoader.sampleSizeFor(8160, 6120));
    }

    @Test
    public void anUndecodableFileReportsNegativeSizeAndIsNotSampled() {
        assertEquals(1, MediaThumbnailLoader.sampleSizeFor(-1, -1));
    }
}
