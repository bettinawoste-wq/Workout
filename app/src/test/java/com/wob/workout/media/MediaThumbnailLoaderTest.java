package com.wob.workout.media;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MediaThumbnailLoaderTest {

    private static final int TARGET = MediaThumbnailLoader.MIN_DECODED_EDGE_PX;

    @Test
    public void picturesAtOrBelowTheTargetAreNotDownsampled() {
        assertEquals(1, MediaThumbnailLoader.sampleSizeFor(1536, 1024, TARGET));
    }

    @Test
    public void aPhonePhotoIsHalvedWithoutDroppingBelowTheTarget() {
        assertEquals(2, MediaThumbnailLoader.sampleSizeFor(4032, 3024, TARGET));
    }

    @Test
    public void portraitPhotosCountTheirLongestEdge() {
        assertEquals(2, MediaThumbnailLoader.sampleSizeFor(3024, 4032, TARGET));
    }

    @Test
    public void veryLargePhotosAreReducedFurther() {
        assertEquals(4, MediaThumbnailLoader.sampleSizeFor(8160, 6120, TARGET));
    }

    @Test
    public void aSmallerTargetReducesMore() {
        assertEquals(8, MediaThumbnailLoader.sampleSizeFor(4032, 3024, 480));
    }

    @Test
    public void anUndecodableFileReportsNegativeSizeAndIsNotSampled() {
        assertEquals(1, MediaThumbnailLoader.sampleSizeFor(-1, -1, TARGET));
    }
}
