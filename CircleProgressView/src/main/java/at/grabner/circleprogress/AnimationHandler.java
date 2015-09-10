package at.grabner.circleprogress;

import android.animation.TimeInterpolator;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import java.lang.ref.WeakReference;

public class AnimationHandler extends Handler {

    private final WeakReference<CircleProgressView> mCircleViewWeakReference;
    private long mAnimationStartTime;
    // The interpolator for value animations
    private TimeInterpolator mInterpolator = new AccelerateDecelerateInterpolator();

    private static final boolean DEBUG = true;

    AnimationHandler(CircleProgressView circleView) {
        super(circleView.getContext().getMainLooper());
        mCircleViewWeakReference = new WeakReference<CircleProgressView>(circleView);
    }

    @Override
    public void handleMessage(Message msg) {
        CircleProgressView circleView = mCircleViewWeakReference.get();
        if (circleView == null) {
            return;
        }
        AnimationMsg msgType = AnimationMsg.values()[msg.what];
        if (msgType == AnimationMsg.TICK) {
            removeMessages(AnimationMsg.TICK.ordinal()); // necessary to remove concurrent ticks.
        }

        if (DEBUG) {
            Log.d("jch", "animation state: " + circleView.mAnimationState + " msgType: " + msgType);
        }

        switch (circleView.mAnimationState) {

            case IDLE:
                switch (msgType) {

                    case SET_VALUE:
                        setValue(msg, circleView);
                        break;
                    case SET_VALUE_ANIMATED:
                        enterSetValueAnimated(msg, circleView);
                        break;
                    case TICK:
                        removeMessages(AnimationMsg.TICK.ordinal()); // remove old ticks
                        break;
                }
                break;

            case ANIMATING:
                switch (msgType) {

                    case TICK:
                        if (calcNextAnimationValue(circleView)) {
                            //animation finished
                            circleView.mAnimationState = AnimationState.IDLE;
                            if (circleView.mAnimationStateChangedListener != null) {
                                circleView.mAnimationStateChangedListener.onAnimationStateChanged(circleView.mAnimationState);
                            }
                            circleView.mCurrentValue = circleView.mValueTo;
                        }
                        sendEmptyMessageDelayed(AnimationMsg.TICK.ordinal(), circleView.mDelayMillis);
                        circleView.invalidate();
                        break;
                }
                break;
        }
    }

    private void enterSetValueAnimated(Message msg, CircleProgressView circleView) {
        circleView.mValueFrom = ((float[]) msg.obj)[0];
        circleView.mValueTo = ((float[]) msg.obj)[1];
        mAnimationStartTime = System.currentTimeMillis();
        circleView.mAnimationState = AnimationState.ANIMATING;
        if (circleView.mAnimationStateChangedListener != null) {
            circleView.mAnimationStateChangedListener.onAnimationStateChanged(circleView.mAnimationState);
        }
        sendEmptyMessageDelayed(AnimationMsg.TICK.ordinal(), circleView.mDelayMillis);
    }

    /**
     * @param circleView the circle view
     * @return false if animation still running, true if animation is finished.
     */
    private boolean calcNextAnimationValue(CircleProgressView circleView) {
        float t = (float) ((System.currentTimeMillis() - mAnimationStartTime)
                / circleView.mAnimationDuration);
        t = t > 1.0f ? 1.0f : t;
        float interpolatedRatio = mInterpolator.getInterpolation(t);

        circleView.mCurrentValue = (circleView.mValueFrom + ((circleView.mValueTo - circleView.mValueFrom) * interpolatedRatio));

        return t >= 1;
    }

    private void setValue(Message msg, CircleProgressView circleView) {
        circleView.mValueFrom = circleView.mValueTo;
        circleView.mCurrentValue = circleView.mValueTo = ((float[]) msg.obj)[0];
        circleView.mAnimationState = AnimationState.IDLE;
        if (circleView.mAnimationStateChangedListener != null) {
            circleView.mAnimationStateChangedListener.onAnimationStateChanged(circleView.mAnimationState);
        }
        circleView.invalidate();
    }
}
