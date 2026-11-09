package com.wob.workout.media;

import android.content.Context;
import android.graphics.Outline;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;

/**
 * The picture frame: always 3:2, whatever the device, so a picture looks the same everywhere it
 * is shown. Fills the available width unless that would take up too much of the screen height,
 * as in landscape; then it shrinks while keeping its shape.
 */
public class PictureFrameLayout extends FrameLayout {

    static final int RATIO_WIDTH = 3;
    static final int RATIO_HEIGHT = 2;
    private static final float MAX_SHARE_OF_SCREEN_HEIGHT = 0.45f;
    private static final int CORNER_RADIUS_DP = 8;

    public PictureFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        float cornerRadius = CORNER_RADIUS_DP * getResources().getDisplayMetrics().density;
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), cornerRadius);
            }
        });
        setClipToOutline(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int maxHeight = (int) (getResources().getDisplayMetrics().heightPixels
                * MAX_SHARE_OF_SCREEN_HEIGHT);
        int[] size = frameSize(MeasureSpec.getSize(widthMeasureSpec), maxHeight);

        super.onMeasure(
                MeasureSpec.makeMeasureSpec(size[0], MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(size[1], MeasureSpec.EXACTLY));
    }

    /** @return width and height of the frame */
    static int[] frameSize(int availableWidth, int maxHeight) {
        int height = availableWidth * RATIO_HEIGHT / RATIO_WIDTH;
        if (height <= maxHeight) {
            return new int[] {availableWidth, height};
        }
        return new int[] {maxHeight * RATIO_WIDTH / RATIO_HEIGHT, maxHeight};
    }
}
