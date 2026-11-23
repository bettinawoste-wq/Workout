package com.wob.workout.media;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Outline;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;

import com.wob.workout.R;

/**
 * The picture frame: always 3:2, whatever the device, so a picture looks the same everywhere it
 * is shown. Fills the available width unless that would take up too much of the screen height,
 * as in landscape; then it shrinks while keeping its shape. With {@code app:fillsHeight}, as
 * beside the controls in the landscape player, it may take all the height it is given instead,
 * and up to 60 % of the width, leaving the rest to the controls.
 */
public class PictureFrameLayout extends FrameLayout {

    static final int RATIO_WIDTH = 3;
    static final int RATIO_HEIGHT = 2;
    private static final float MAX_SHARE_OF_SCREEN_HEIGHT = 0.55f;
    /** Beside the controls, the most of the given width a frame that fills the height takes. */
    private static final float MAX_SHARE_OF_WIDTH_BESIDE_CONTROLS = 0.6f;
    private static final int CORNER_RADIUS_DP = 8;

    private final boolean fillsHeight;

    public PictureFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray attributes =
                context.obtainStyledAttributes(attrs, R.styleable.PictureFrameLayout);
        fillsHeight = attributes.getBoolean(R.styleable.PictureFrameLayout_fillsHeight, false);
        attributes.recycle();

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
        int maxHeight = maxHeight(fillsHeight, getResources().getDisplayMetrics().heightPixels,
                MeasureSpec.getSize(heightMeasureSpec));
        int[] size = frameSize(maxWidth(fillsHeight, MeasureSpec.getSize(widthMeasureSpec)),
                maxHeight);

        super.onMeasure(
                MeasureSpec.makeMeasureSpec(size[0], MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(size[1], MeasureSpec.EXACTLY));
    }

    /** @return the most height the frame may take: all it is given if it fills the height,
     *  otherwise its share of the screen */
    static int maxHeight(boolean fillsHeight, int screenHeight, int givenHeight) {
        return fillsHeight ? givenHeight : (int) (screenHeight * MAX_SHARE_OF_SCREEN_HEIGHT);
    }

    /** @return the most width the frame may take: all it is given, unless it fills the
     *  height beside the controls, which keep the rest */
    static int maxWidth(boolean fillsHeight, int givenWidth) {
        return fillsHeight ? (int) (givenWidth * MAX_SHARE_OF_WIDTH_BESIDE_CONTROLS) : givenWidth;
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
