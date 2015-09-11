package at.grabner.circleprogress;

import android.animation.TimeInterpolator;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;

import java.lang.ref.WeakReference;

public class TierBadgeAnimationHandler extends Handler {

  private final WeakReference<TierBadgeView> mCircleViewWeakReference;
  private long mAnimationStartTime;
  // The interpolator for value animations
  private TimeInterpolator mInterpolator = new AccelerateDecelerateInterpolator();

  private static final boolean DEBUG = true;

  TierBadgeAnimationHandler(TierBadgeView tierBadgeView) {
    super(tierBadgeView.getContext().getMainLooper());
    mCircleViewWeakReference = new WeakReference<TierBadgeView>(tierBadgeView);
  }

  @Override
  public void handleMessage(Message msg) {
    TierBadgeView tierBadgeView = mCircleViewWeakReference.get();
    if (tierBadgeView == null) {
      return;
    }
    TierBadgeView.AnimationMsg msgType = TierBadgeView.AnimationMsg.values()[msg.what];
    if (msgType == TierBadgeView.AnimationMsg.TICK) {
      removeMessages(TierBadgeView.AnimationMsg.TICK.ordinal()); // necessary to remove concurrent ticks.
    }

    if (DEBUG) {
      Log.d("jch", "animation state: " + tierBadgeView.mAnimationState + " msgType: " + msgType);
    }

    switch (tierBadgeView.mAnimationState) {

      case IDLE:
        switch (msgType) {

          case SET_VALUE:
            setValue(msg, tierBadgeView);
            break;
          case SET_VALUE_ANIMATED:
            enterSetValueAnimated(msg, tierBadgeView);
            break;
          case TICK:
            removeMessages(TierBadgeView.AnimationMsg.TICK.ordinal()); // remove old ticks
            break;
        }
        break;

      case ANIMATING:
        switch (msgType) {

          case TICK:
            if (calcNextAnimationValue(tierBadgeView)) {
              //animation finished
              tierBadgeView.mAnimationState = TierBadgeView.AnimationState.IDLE;
              if (tierBadgeView.mAnimationStateChangedListener != null) {
                tierBadgeView.mAnimationStateChangedListener.onAnimationStateChanged(tierBadgeView.mAnimationState);
              }
              tierBadgeView.mCurrentValue = tierBadgeView.mValueTo;
            }
            sendEmptyMessageDelayed(TierBadgeView.AnimationMsg.TICK.ordinal(), tierBadgeView.mDelayMillis);
            tierBadgeView.invalidate();
            break;
        }
        break;
    }
  }

  private void enterSetValueAnimated(Message msg, TierBadgeView tierBadgeView) {
    tierBadgeView.mValueFrom = ((float[]) msg.obj)[0];
    tierBadgeView.mValueTo = ((float[]) msg.obj)[1];
    mAnimationStartTime = System.currentTimeMillis();
    tierBadgeView.mAnimationState = TierBadgeView.AnimationState.ANIMATING;
    if (tierBadgeView.mAnimationStateChangedListener != null) {
      tierBadgeView.mAnimationStateChangedListener.onAnimationStateChanged(tierBadgeView.mAnimationState);
    }
    sendEmptyMessageDelayed(TierBadgeView.AnimationMsg.TICK.ordinal(), tierBadgeView.mDelayMillis);
  }

  /**
   * @param tierBadgeView the circle view
   * @return false if animation still running, true if animation is finished.
   */
  private boolean calcNextAnimationValue(TierBadgeView tierBadgeView) {
    float t = (float) ((System.currentTimeMillis() - mAnimationStartTime)
        / tierBadgeView.mAnimationDuration);
    t = t > 1.0f ? 1.0f : t;
    float interpolatedRatio = mInterpolator.getInterpolation(t);

    tierBadgeView.mCurrentValue = (tierBadgeView.mValueFrom + ((tierBadgeView.mValueTo - tierBadgeView.mValueFrom) * interpolatedRatio));

    return t >= 1;
  }

  private void setValue(Message msg, TierBadgeView tierBadgeView) {
    tierBadgeView.mValueFrom = tierBadgeView.mValueTo;
    tierBadgeView.mCurrentValue = tierBadgeView.mValueTo = ((float[]) msg.obj)[0];
    tierBadgeView.mAnimationState = TierBadgeView.AnimationState.IDLE;
    if (tierBadgeView.mAnimationStateChangedListener != null) {
      tierBadgeView.mAnimationStateChangedListener.onAnimationStateChanged(tierBadgeView.mAnimationState);
    }
    tierBadgeView.invalidate();
  }
}
