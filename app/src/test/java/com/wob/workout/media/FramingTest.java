package com.wob.workout.media;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FramingTest {

    private static final float DELTA = 0.001f;
    private static final float FRAME_SHAPED = 1.5f;
    private static final float PORTRAIT = 0.75f;
    private static final float CENTRE = 0.5f;

    @Test
    public void aFrameShapedPhotoFillsTheFrameAtZoomOne() {
        Framing framing = Framing.filling(FRAME_SHAPED);

        assertEquals(1f, framing.zoom, DELTA);
        assertEquals(1f, framing.photoWidth(FRAME_SHAPED), DELTA);
        assertEquals(1f, framing.photoHeight(FRAME_SHAPED), DELTA);
        assertEquals(0f, framing.photoLeft(FRAME_SHAPED), DELTA);
        assertEquals(0f, framing.photoTop(FRAME_SHAPED), DELTA);
    }

    @Test
    public void aPortraitPhotoFillsTheWidthAndOverhangsCentred() {
        Framing framing = Framing.filling(PORTRAIT);

        assertEquals(1f, framing.photoWidth(PORTRAIT), DELTA);
        assertEquals(2f, framing.photoHeight(PORTRAIT), DELTA);
        assertEquals(-0.5f, framing.photoTop(PORTRAIT), DELTA);
    }

    @Test
    public void atZoomOneThePortraitPhotoIsWhollyVisibleWithSideMargins() {
        Framing framing = new Framing(1, 0, 0);

        assertEquals(1f, framing.photoHeight(PORTRAIT), DELTA);
        assertEquals(0.5f, framing.photoWidth(PORTRAIT), DELTA);
        assertEquals(0.25f, framing.photoLeft(PORTRAIT), DELTA);
    }

    @Test
    public void zoomIsLimitedAtBothEnds() {
        Framing start = new Framing(1, 0, 0);

        assertEquals(Framing.MIN_ZOOM,
                start.zoomedBy(0.01f, CENTRE, CENTRE, FRAME_SHAPED).zoom, DELTA);
        assertEquals(Framing.MAX_ZOOM,
                start.zoomedBy(100f, CENTRE, CENTRE, FRAME_SHAPED).zoom, DELTA);
    }

    @Test
    public void anExtremePanoramaCanStillFillTheFrame() {
        float panorama = 9f;
        Framing filling = Framing.filling(panorama);

        assertEquals(6f, filling.zoom, DELTA);
        assertEquals(6f, filling.zoomedBy(1.5f, CENTRE, CENTRE, panorama).zoom, DELTA);
    }

    @Test
    public void thePointUnderTheFingersStaysPut() {
        Framing start = new Framing(1, 0, 0);
        float focus = 0.25f;

        Framing zoomed = start.zoomedBy(2f, focus, focus, FRAME_SHAPED);

        // A quarter into the frame was a quarter into the photo; it must still be at 0.25.
        float afterX = zoomed.photoLeft(FRAME_SHAPED) + 0.25f * zoomed.photoWidth(FRAME_SHAPED);
        float afterY = zoomed.photoTop(FRAME_SHAPED) + 0.25f * zoomed.photoHeight(FRAME_SHAPED);
        assertEquals(focus, afterX, DELTA);
        assertEquals(focus, afterY, DELTA);
    }

    @Test
    public void aPhotoCanNeverLeaveTheFrameCompletely() {
        Framing start = new Framing(1, 0, 0);

        Framing right = start.movedBy(50f, 0, FRAME_SHAPED);
        Framing left = start.movedBy(-50f, 0, FRAME_SHAPED);
        Framing down = start.movedBy(0, 50f, FRAME_SHAPED);

        assertTrue(right.photoLeft(FRAME_SHAPED) < 1f);
        assertTrue(left.photoLeft(FRAME_SHAPED) + left.photoWidth(FRAME_SHAPED) > 0f);
        assertTrue(down.photoTop(FRAME_SHAPED) < 1f);
    }

    @Test
    public void zoomingOutStopsOnceTheWholePhotoIsVisible() {
        Framing zoomedOut = new Framing(2, 0, 0).zoomedBy(0.1f, CENTRE, CENTRE, PORTRAIT);

        assertEquals(1f, zoomedOut.zoom, DELTA);
    }

    @Test
    public void aPhotoNarrowerThanTheFrameStaysCentredWithEqualMargins() {
        Framing moved = new Framing(1, 0, 0).movedBy(0.4f, 0.3f, PORTRAIT);

        assertEquals(0f, moved.offsetX, DELTA);
        assertEquals(0f, moved.offsetY, DELTA);
    }

    @Test
    public void aPhotoLargerThanTheFrameMovesOnlyUntilItsEdgeMeetsTheFrame() {
        Framing moved = new Framing(2, 0, 0).movedBy(5f, -5f, FRAME_SHAPED);

        assertEquals(0f, moved.photoLeft(FRAME_SHAPED), DELTA);
        assertEquals(1f, moved.photoTop(FRAME_SHAPED) + moved.photoHeight(FRAME_SHAPED), DELTA);
    }

    @Test
    public void marginsOnlyEverShowOnTwoOppositeEdgesAlike() {
        float[] aspects = {0.5f, 0.75f, 1f, 1.5f, 2f, 3f};
        float[] zoomFactors = {0.1f, 0.7f, 1f, 1.3f, 2.5f};
        float[] moves = {-2f, -0.3f, 0f, 0.2f, 2f};

        for (float aspect : aspects) {
            for (float factor : zoomFactors) {
                for (float deltaX : moves) {
                    for (float deltaY : moves) {
                        Framing framing = Framing.filling(aspect)
                                .zoomedBy(factor, 0.3f, 0.6f, aspect)
                                .movedBy(deltaX, deltaY, aspect);

                        assertMarginsOnOppositeEdgesOnly(framing, aspect,
                                "aspect " + aspect + ", zoom x" + factor
                                        + ", moved " + deltaX + "/" + deltaY);
                    }
                }
            }
        }
    }

    private static void assertMarginsOnOppositeEdgesOnly(
            Framing framing, float aspect, String scenario) {
        float left = framing.photoLeft(aspect);
        float top = framing.photoTop(aspect);
        float right = 1 - (left + framing.photoWidth(aspect));
        float bottom = 1 - (top + framing.photoHeight(aspect));

        boolean sides = left > DELTA || right > DELTA;
        boolean topOrBottom = top > DELTA || bottom > DELTA;
        assertTrue(scenario + ": margins left/right and top/bottom", !(sides && topOrBottom));
        if (sides) assertEquals(scenario, left, right, DELTA);
        if (topOrBottom) assertEquals(scenario, top, bottom, DELTA);
    }

    @Test
    public void movingIsRelativeToTheFrame() {
        Framing moved = new Framing(2, 0, 0).movedBy(0.1f, -0.2f, FRAME_SHAPED);

        assertEquals(0.1f, moved.offsetX, DELTA);
        assertEquals(-0.2f, moved.offsetY, DELTA);
    }

    @Test
    public void zoomingOutPastTheLimitKeepsThePhotoInTheFrame() {
        Framing farOut = new Framing(4, 2f, 0);

        Framing small = farOut.zoomedBy(0.125f, CENTRE, CENTRE, FRAME_SHAPED);

        assertEquals(Framing.MIN_ZOOM, small.zoom, DELTA);
        assertTrue(small.photoLeft(FRAME_SHAPED) < 1f);
    }
}
