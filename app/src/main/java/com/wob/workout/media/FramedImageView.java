package com.wob.workout.media;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewParent;

import androidx.appcompat.widget.AppCompatImageView;

/**
 * Draws a picture according to a {@link Framing}, the same way everywhere. Without a framing it
 * shows the default. With a {@link Listener} set, the user can change the framing by touch:
 * two fingers zoom, one finger moves. Any margin simply shows what is behind this view.
 */
public class FramedImageView extends AppCompatImageView {

    public interface Listener {
        void onFramingChanged(Framing framing);
    }

    private final Matrix matrix = new Matrix();
    private final ScaleGestureDetector scaleDetector;

    private Framing framing;
    private Listener listener;
    private float lastX;
    private float lastY;

    public FramedImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setScaleType(ScaleType.MATRIX);

        scaleDetector = new ScaleGestureDetector(context,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        framing = effectiveFraming().zoomedBy(
                                detector.getScaleFactor(),
                                detector.getFocusX() / getWidth(),
                                detector.getFocusY() / getHeight(),
                                photoAspect());
                        applyFraming();
                        return true;
                    }
                });
        scaleDetector.setQuickScaleEnabled(false);
    }

    /** @param framing the framing to draw, or null for the default */
    public void setFraming(Framing framing) {
        this.framing = framing;
        applyFraming();
    }

    /** @param listener told at the end of each gesture; null turns gestures off */
    public void setFramingListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        applyFraming();
    }

    @Override
    public void setImageResource(int resId) {
        super.setImageResource(resId);
        applyFraming();
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        applyFraming();
    }

    private boolean isReadyToDraw() {
        Drawable drawable = getDrawable();
        return drawable != null
                && drawable.getIntrinsicWidth() > 0
                && drawable.getIntrinsicHeight() > 0
                && getWidth() > 0
                && getHeight() > 0;
    }

    private float photoAspect() {
        Drawable drawable = getDrawable();
        return (float) drawable.getIntrinsicWidth() / drawable.getIntrinsicHeight();
    }

    private Framing effectiveFraming() {
        return framing != null ? framing : Framing.filling(photoAspect());
    }

    private void applyFraming() {
        if (!isReadyToDraw()) return;

        float aspect = photoAspect();
        Framing current = effectiveFraming();
        float scale = current.photoWidth(aspect) * getWidth() / getDrawable().getIntrinsicWidth();

        matrix.setScale(scale, scale);
        matrix.postTranslate(
                current.photoLeft(aspect) * getWidth(),
                current.photoTop(aspect) * getHeight());
        setImageMatrix(matrix);
    }

    // The gestures have no click equivalent, and the picture can be saved as it is without them.
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (listener == null || !isReadyToDraw()) return super.onTouchEvent(event);

        scaleDetector.onTouchEvent(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                setParentMayIntercept(false);
                rememberPosition(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_POINTER_UP:
                // One finger is lifting; continue from whichever one stays down.
                int remaining = event.getActionIndex() == 0 ? 1 : 0;
                rememberPosition(event.getX(remaining), event.getY(remaining));
                break;
            case MotionEvent.ACTION_MOVE:
                if (event.getPointerCount() == 1 && !scaleDetector.isInProgress()) {
                    moveBy(event.getX() - lastX, event.getY() - lastY);
                    rememberPosition(event.getX(), event.getY());
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                setParentMayIntercept(true);
                if (framing != null) listener.onFramingChanged(framing);
                break;
            default:
                break;
        }
        return true;
    }

    private void moveBy(float deltaX, float deltaY) {
        if (deltaX == 0 && deltaY == 0) return;

        framing = effectiveFraming().movedBy(
                deltaX / getWidth(), deltaY / getHeight(), photoAspect());
        applyFraming();
    }

    private void rememberPosition(float x, float y) {
        lastX = x;
        lastY = y;
    }

    /** The editor scrolls: without this, a vertical drag on the photo would scroll instead. */
    private void setParentMayIntercept(boolean mayIntercept) {
        ViewParent parent = getParent();
        if (parent != null) parent.requestDisallowInterceptTouchEvent(!mayIntercept);
    }
}
