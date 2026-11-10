package com.wob.workout.media;

/**
 * How a photo sits in the picture frame (see GLOSSARY.md and ADR 0003). Everything is relative
 * to the frame and the photo, never to pixels, so a framing looks the same on every device.
 *
 * <p>All positions and sizes below are fractions: of the frame's width for x, of its height
 * for y. {@code photoAspect} is the photo's width divided by its height.
 */
public final class Framing {

    /** The whole photo just fits. Any smaller would leave a margin on all four edges. */
    static final float MIN_ZOOM = 1f;
    static final float MAX_ZOOM = 4f;

    private static final float FRAME_ASPECT =
            (float) PictureFrameLayout.RATIO_WIDTH / PictureFrameLayout.RATIO_HEIGHT;
    /** 1 means the whole photo just fits inside the frame. */
    public final float zoom;
    /** Where the photo's centre is, relative to the centre of the frame. */
    public final float offsetX;
    public final float offsetY;

    public Framing(float zoom, float offsetX, float offsetY) {
        this.zoom = zoom;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    /** The default: filling the frame, centred, with the overhanging edges cut off. */
    public static Framing filling(float photoAspect) {
        return new Framing(fillingZoom(photoAspect), 0, 0);
    }

    public float photoWidth(float photoAspect) {
        return zoom * fitWidth(photoAspect);
    }

    public float photoHeight(float photoAspect) {
        return zoom * fitHeight(photoAspect);
    }

    public float photoLeft(float photoAspect) {
        return 0.5f + offsetX - photoWidth(photoAspect) / 2;
    }

    public float photoTop(float photoAspect) {
        return 0.5f + offsetY - photoHeight(photoAspect) / 2;
    }

    public Framing movedBy(float deltaX, float deltaY, float photoAspect) {
        return clamped(zoom, offsetX + deltaX, offsetY + deltaY, photoAspect);
    }

    /** Zooms so that the point of the photo at the given position in the frame stays there. */
    public Framing zoomedBy(float factor, float focusX, float focusY, float photoAspect) {
        // A very narrow or wide photo needs more than MAX_ZOOM just to fill the frame.
        float maxZoom = Math.max(MAX_ZOOM, fillingZoom(photoAspect));
        float newZoom = Math.max(MIN_ZOOM, Math.min(maxZoom, zoom * factor));
        float appliedFactor = newZoom / zoom;

        float centreX = focusX + (0.5f + offsetX - focusX) * appliedFactor;
        float centreY = focusY + (0.5f + offsetY - focusY) * appliedFactor;

        return clamped(newZoom, centreX - 0.5f, centreY - 0.5f, photoAspect);
    }

    private static Framing clamped(float zoom, float offsetX, float offsetY, float photoAspect) {
        float limitX = offsetLimit(zoom * fitWidth(photoAspect));
        float limitY = offsetLimit(zoom * fitHeight(photoAspect));

        return new Framing(
                zoom,
                Math.max(-limitX, Math.min(limitX, offsetX)),
                Math.max(-limitY, Math.min(limitY, offsetY)));
    }

    /**
     * A margin only ever shows on two opposite edges, the same width on both. Where the photo
     * is smaller than the frame it stays centred; where it is larger it can be moved, but never
     * so far that it uncovers an edge.
     */
    private static float offsetLimit(float photoSize) {
        return Math.max(0, (photoSize - 1) / 2);
    }

    private static float fillingZoom(float photoAspect) {
        return 1 / Math.min(fitWidth(photoAspect), fitHeight(photoAspect));
    }

    private static float fitWidth(float photoAspect) {
        return Math.min(1, photoAspect / FRAME_ASPECT);
    }

    private static float fitHeight(float photoAspect) {
        return Math.min(1, FRAME_ASPECT / photoAspect);
    }
}
