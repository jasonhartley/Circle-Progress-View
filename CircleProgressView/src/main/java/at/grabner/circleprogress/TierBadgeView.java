package at.grabner.circleprogress;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.util.AttributeSet;
import android.view.View;

public class TierBadgeView extends View {

  enum AnimationState {
    IDLE,
    ANIMATING
  }

  enum AnimationMsg {
    SET_VALUE,
    SET_VALUE_ANIMATED,
    TICK
  }

  interface AnimationStateChangedListener {

    /**
     * Call if animation state changes.
     * This code runs in the animation loop, so keep your code short!
     * @param tierBadgeAnimationState
     */
    void onAnimationStateChanged(TierBadgeView.AnimationState tierBadgeAnimationState);
  }

  private static final boolean DEBUG = false;

  float mCurrentValue = 0;
  float mValueTo = 0;
  float mValueFrom = 0;
  float mMaxValue = 100;

  private int mLayoutHeight = 0;
  private int mLayoutWidth = 0;
  private int mCircleRadius = 80;
  private int mBarWidth = 40;
  private int mRimWidth = 40;
  private int mStartAngle = 270;
  private float mContourSize  = 1;

  //Padding (with defaults)
  private int mPaddingTop = 5;
  private int mPaddingBottom = 5;
  private int mPaddingLeft = 5;
  private int mPaddingRight = 5;
  //Colors (with defaults)
  private int mBarColorStandard = Color.CYAN;
  private int mContourColor = Color.TRANSPARENT;
  private int mBackgroundCircleColor = Color.GRAY;
  private int mRimColor = Color.LTGRAY;
  private int[] mBarColors = new int[]{ mBarColorStandard };
  //Caps
  private Paint.Cap mBarStrokeCap = Paint.Cap.BUTT;
  //Paints
  private Paint mBarPaint = new Paint();
  private Paint mBarSpinnerPaint = new Paint();
  private Paint mBackgroundCirclePaint = new Paint();
  private Paint mRimPaint = new Paint();
  private Paint mContourPaint = new Paint();
  //Rectangles
  private RectF mCircleBounds = new RectF();
  private RectF mInnerCircleBound = new RectF();
  private RectF mInnerBitmapBound = new RectF();
  private PointF mCenter;// for future use: might be better for center bitmap placement
  // Center image
  private Bitmap mCenterBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8);
  private Paint mCenterPaint = new Paint();

  private RectF mCircleOuterContour = new RectF();
  private RectF mCircleInnerContour = new RectF();

  /**
   * The animation duration in ms
   */
  double mAnimationDuration = 900;

  //The number of milliseconds to wait in between each draw
  int mDelayMillis = 15;

  //The animation handler containing the animation state machine.
  Handler mAnimationHandler = new TierBadgeAnimationHandler(this);

  //The current state of the animation state machine.
  TierBadgeView.AnimationState mAnimationState = TierBadgeView.AnimationState.IDLE;

  //clipping
  private Bitmap mClippingBitmap;

  TierBadgeView.AnimationStateChangedListener mAnimationStateChangedListener;

  /**
   * The constructor for the CircleView
   *
   * @param context The context.
   * @param attrs   The attributes.
   */
  public TierBadgeView(Context context, AttributeSet attrs) {
    super(context, attrs);

    parseAttributes(context.obtainStyledAttributes(attrs, R.styleable.TierBadgeView));

    Paint mMaskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    mMaskPaint.setFilterBitmap(false);
    mMaskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
    setupPaints();
  }

  /**
   * Parse the attributes passed to the view from the XML
   *
   * @param a the attributes to parse
   */
  private void parseAttributes(TypedArray a) {
    setBarWidth((int) a.getDimension(R.styleable.TierBadgeView_barWidth,
        mBarWidth));

    setRimWidth((int) a.getDimension(R.styleable.TierBadgeView_rimWidth,
        mRimWidth));

    if (a.hasValue(R.styleable.TierBadgeView_barColor) && a.hasValue(R.styleable.TierBadgeView_barColor1) && a.hasValue(R.styleable.TierBadgeView_barColor2) && a.hasValue(R.styleable.TierBadgeView_barColor3)) {
      mBarColors = new int[]{a.getColor(R.styleable.TierBadgeView_barColor, mBarColorStandard), a.getColor(R.styleable.TierBadgeView_barColor1, mBarColorStandard), a.getColor(R.styleable.TierBadgeView_barColor2, mBarColorStandard), a.getColor(R.styleable.TierBadgeView_barColor3, mBarColorStandard)};

    } else if (a.hasValue(R.styleable.TierBadgeView_barColor) && a.hasValue(R.styleable.TierBadgeView_barColor1) && a.hasValue(R.styleable.TierBadgeView_barColor2)) {
      mBarColors = new int[]{a.getColor(R.styleable.TierBadgeView_barColor, mBarColorStandard), a.getColor(R.styleable.TierBadgeView_barColor1, mBarColorStandard), a.getColor(R.styleable.TierBadgeView_barColor2, mBarColorStandard)};

    } else if (a.hasValue(R.styleable.TierBadgeView_barColor) && a.hasValue(R.styleable.TierBadgeView_barColor1)) {
      mBarColors = new int[]{a.getColor(R.styleable.TierBadgeView_barColor, mBarColorStandard), a.getColor(R.styleable.TierBadgeView_barColor1, mBarColorStandard)};

    } else {
      mBarColors = new int[]{a.getColor(R.styleable.TierBadgeView_barColor, mBarColorStandard), a.getColor(R.styleable.TierBadgeView_barColor, mBarColorStandard)};
    }

    setRimColor(a.getColor(R.styleable.TierBadgeView_rimColor, mRimColor));

    setFillCircleColor(a.getColor(R.styleable.TierBadgeView_fillColor, mBackgroundCircleColor));

    setContourColor(a.getColor(R.styleable.TierBadgeView_contourColor, mContourColor));
    setContourSize(a.getDimension(R.styleable.TierBadgeView_contourSize, mContourSize));

    setMaxValue(a.getFloat(R.styleable.TierBadgeView_maxValue, mMaxValue));

    setStartAngle(a.getInt(R.styleable.TierBadgeView_startAngle, mStartAngle));

    // Recycle
    a.recycle();
  }

  /*
  * When this is called, make the view square.
  * From: http://www.jayway.com/2012/12/12/creating-custom-android-views-part-4-measuring-and-how-to-force-a-view-to-be-square/
  */
  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    // The first thing that happen is that we call the superclass
    // implementation of onMeasure. The reason for that is that measuring
    // can be quite a complex process and calling the super method is a
    // convenient way to get most of this complexity handled.
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    // We can’t use getWidth() or getHeight() here. During the measuring
    // pass the view has not gotten its final size yet (this happens first
    // at the start of the layout pass) so we have to use getMeasuredWidth()
    // and getMeasuredHeight().
    int size = 0;
    int width = getMeasuredWidth();
    int height = getMeasuredHeight();
    int widthWithoutPadding = width - getPaddingLeft() - getPaddingRight();
    int heightWithoutPadding = height - getPaddingTop() - getPaddingBottom();


    // Finally we have some simple logic that calculates the size of the view
    // and calls setMeasuredDimension() to set that size.
    // Before we compare the width and height of the view, we remove the padding,
    // and when we set the dimension we add it back again. Now the actual content
    // of the view will be square, but, depending on the padding, the total dimensions
    // of the view might not be.
    if (widthWithoutPadding > heightWithoutPadding) {
      size = heightWithoutPadding;
    } else {
      size = widthWithoutPadding;
    }

    // If you override onMeasure() you have to call setMeasuredDimension().
    // This is how you report back the measured size.  If you don’t call
    // setMeasuredDimension() the parent will throw an exception and your
    // application will crash.
    // We are calling the onMeasure() method of the superclass so we don’t
    // actually need to call setMeasuredDimension() since that takes care
    // of that. However, the purpose with overriding onMeasure() was to
    // change the default behaviour and to do that we need to call
    // setMeasuredDimension() with our own values.
    setMeasuredDimension(size + getPaddingLeft() + getPaddingRight(), size + getPaddingTop() + getPaddingBottom());
  }


  private RectF getInnerCircleRect(RectF _circleBounds) {
    double circleWidth = +_circleBounds.width() - (Math.max(mBarWidth, mRimWidth)) - (mContourSize * 2);
    double width = ((circleWidth / 2d) * Math.sqrt(2d));
    float widthDelta = (_circleBounds.width() - (float) width) / 2f;

    float scaleX = 1;
    float scaleY = 1;

    return new RectF(_circleBounds.left + (widthDelta * scaleX), _circleBounds.top + (widthDelta * scaleY), _circleBounds.right - (widthDelta * scaleX), _circleBounds.bottom - (widthDelta * scaleY));
  }

  /**
   * Use onSizeChanged instead of onAttachedToWindow to get the dimensions of the view,
   * because this method is called after measuring the dimensions of MATCH_PARENT and WRAP_CONTENT.
   * Use this dimensions to setup the bounds and paints.
   */
  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);

    // Share the dimensions
    mLayoutWidth = w;
    mLayoutHeight = h;

    setupBounds();
    setupBarPaint();

    if (mClippingBitmap != null) {
      mClippingBitmap = Bitmap.createScaledBitmap(mClippingBitmap, getWidth(), getHeight(), false);
    }

    invalidate();
  }

  public Paint.Cap getBarStrokeCap() {
    return mBarStrokeCap;
  }

  /**
   * @param _barStrokeCap The stroke cap of the progress bar.
   */
  public void setBarStrokeCap(Paint.Cap _barStrokeCap) {
    mBarStrokeCap = _barStrokeCap;
    mBarPaint.setStrokeCap(_barStrokeCap);
  }

  public int getContourColor() {
    return mContourColor;
  }

  /**
   * @param _contourColor The color of the background contour of the circle.
   */
  public void setContourColor(@ColorInt int _contourColor) {
    mContourColor = _contourColor;
    mContourPaint.setColor(_contourColor);
  }

  public float getContourSize() {
    return mContourSize;
  }

  /**
   * @param _contourSize The size of the background contour of the circle.
   */
  public void setContourSize(@FloatRange(from = 0.0) float _contourSize) {
    mContourSize = _contourSize;
    mContourPaint.setStrokeWidth(_contourSize);
  }

  public void setCenterImage(int id) {
    mCenterBitmap = BitmapFactory.decodeResource(getResources(), id);
    //mCenterBitmap.setHasAlpha(true);// Possibly unnecessary
  }

  public int getPaddingTop() {
    return mPaddingTop;
  }

  public void setPaddingTop(int paddingTop) {
    this.mPaddingTop = paddingTop;
  }

  public int getPaddingBottom() {
    return mPaddingBottom;
  }

  public void setPaddingBottom(int paddingBottom) {
    this.mPaddingBottom = paddingBottom;
  }

  public int getPaddingLeft() {
    return mPaddingLeft;
  }

  public void setPaddingLeft(int paddingLeft) {
    this.mPaddingLeft = paddingLeft;
  }

  public int getPaddingRight() {
    return mPaddingRight;
  }

  public void setPaddingRight(int paddingRight) {
    this.mPaddingRight = paddingRight;
  }

  public int getCircleRadius() {
    return mCircleRadius;
  }

  public int getBarWidth() {
    return mBarWidth;
  }

  /**
   * @param barWidth The width of the progress bar in pixel.
   */
  public void setBarWidth(@FloatRange(from = 0.0) int barWidth) {
    this.mBarWidth = barWidth;
    mBarPaint.setStrokeWidth(barWidth);
    mBarSpinnerPaint.setStrokeWidth(barWidth);
  }

  public int[] getBarColors() {
    return mBarColors;
  }

  /**
   * Sets the color of progress bar.
   *
   * @param barColors One or more colors. If more than one color is specified, a gradient of the colors is used.
   */
  public void setBarColor(@ColorInt int... barColors) {
    this.mBarColors = barColors;
    if (mBarColors.length > 1) {
      mBarPaint.setShader(new SweepGradient(mCircleBounds.centerX(), mCircleBounds.centerY(), mBarColors, null));
      Matrix matrix = new Matrix();
      mBarPaint.getShader().getLocalMatrix(matrix);

      matrix.postTranslate(-mCircleBounds.centerX(), -mCircleBounds.centerY());
      matrix.postRotate(mStartAngle);
      matrix.postTranslate(mCircleBounds.centerX(), mCircleBounds.centerY());
      mBarPaint.getShader().setLocalMatrix(matrix);
      mBarPaint.setColor(barColors[0]);
    } else if (mBarColors.length == 0) {
      mBarPaint.setColor(mBarColors[0]);
      mBarPaint.setShader(null);
    } else {
      mBarPaint.setColor(mBarColorStandard);
      mBarPaint.setShader(null);
    }
  }

  public int getBackgroundCircleColor() {
    return mBackgroundCirclePaint.getColor();
  }

  /**
   * Sets the background color of the entire Progress Circle.
   * Set the color to 0x00000000 (Color.TRANSPARENT) to hide it.
   *
   * @param circleColor the color.
   */
  public void setFillCircleColor(@ColorInt int circleColor) {
    mBackgroundCircleColor = circleColor;
    mBackgroundCirclePaint.setColor(circleColor);
  }

  public int getRimColor() {
    return mRimColor;
  }

  /**
   * @param rimColor The color of the rim around the Circle.
   */
  public void setRimColor(@ColorInt int rimColor) {
    mRimColor = rimColor;
    mRimPaint.setColor(rimColor);
  }

  public Shader getRimShader() {
    return mRimPaint.getShader();
  }

  public void setRimShader(Shader shader) {
    this.mRimPaint.setShader(shader);
  }

  public double getMaxValue() {
    return mMaxValue;
  }

  /**
   * The max value of the progress bar. Used to calculate the percentage of the current value.
   * The bar fills according to the percentage. The default value is 100.
   *
   * @param _maxValue The max value.
   */
  public void setMaxValue(@FloatRange(from = 0) float _maxValue) {
    mMaxValue = _maxValue;
  }

  public int getRimWidth() {
    return mRimWidth;
  }

  /**
   * @param rimWidth The width in pixel of the rim around the circle
   */
  public void setRimWidth(@IntRange(from = 0) int rimWidth) {
    mRimWidth = rimWidth;
    mRimPaint.setStrokeWidth(rimWidth);
  }

  /**
   * @return The number of ms to wait between each draw call.
   */
  public int getDelayMillis() {
    return mDelayMillis;
  }

  /**
   * @param delayMillis The number of ms to wait between each draw call.
   */
  public void setDelayMillis(int delayMillis) {
    this.mDelayMillis = delayMillis;
  }

  /**
   * @param clippingBitmap The bitmap used for clipping. Set to null to disable clipping.
   *            Default: No clipping.
   */
  public void setClippingBitmap(Bitmap clippingBitmap) {

    if (getWidth() > 0 && getHeight() > 0) {
      mClippingBitmap = Bitmap.createScaledBitmap(clippingBitmap, getWidth(), getHeight(), false);
    } else {
      mClippingBitmap = clippingBitmap;
    }
    if (mClippingBitmap == null) {
      // enable HW acceleration
      setLayerType(View.LAYER_TYPE_HARDWARE, null);
    } else {
      // disable HW acceleration
      setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }
  }

  public int getStartAngle() {
    return mStartAngle;
  }

  public void setStartAngle(int startAngle) {
    // get a angle between 0 and 360
    mStartAngle = (int) normalizeAngle(startAngle);
  }

  /**
   * Setup all paints.
   * Call only if changes to color or size properties are not visible.
   */
  public void setupPaints() {
    setupBarPaint();
    setupContourPaint();
    setupBackgroundCirclePaint();
    setupRimPaint();
  }


  private void setupContourPaint() {
    mContourPaint.setColor(mContourColor);
    mContourPaint.setAntiAlias(true);
    mContourPaint.setStyle(Style.STROKE);
    mContourPaint.setStrokeWidth(mContourSize);
  }

  private void setupBackgroundCirclePaint() {
    mBackgroundCirclePaint.setColor(mBackgroundCircleColor);
    mBackgroundCirclePaint.setAntiAlias(true);
    mBackgroundCirclePaint.setStyle(Style.FILL);
  }

  private void setupRimPaint() {
    mRimPaint.setColor(mRimColor);
    mRimPaint.setAntiAlias(true);
    mRimPaint.setStyle(Style.STROKE);
    mRimPaint.setStrokeWidth(mRimWidth);
  }

  private void setupBarPaint() {
    if (mBarColors.length > 1) {
      mBarPaint.setShader(new SweepGradient(mCircleBounds.centerX(), mCircleBounds.centerY(), mBarColors, null));
      Matrix matrix = new Matrix();
      mBarPaint.getShader().getLocalMatrix(matrix);

      matrix.postTranslate(-mCircleBounds.centerX(), -mCircleBounds.centerY());
      matrix.postRotate(mStartAngle);
      matrix.postTranslate(mCircleBounds.centerX(), mCircleBounds.centerY());
      mBarPaint.getShader().setLocalMatrix(matrix);
    } else {
      mBarPaint.setColor(mBarColors[0]);
      mBarPaint.setShader(null);
    }

    mBarPaint.setAntiAlias(true);
    mBarPaint.setStrokeCap(mBarStrokeCap);
    mBarPaint.setStyle(Style.STROKE);
    mBarPaint.setStrokeWidth(mBarWidth);
  }

  /**
   * Set the bounds of the component
   */
  private void setupBounds() {
    // Width should equal to Height, find the min value to setup the circle
    int minValue = Math.min(mLayoutWidth, mLayoutHeight);

    // Calc the Offset if needed
    int xOffset = mLayoutWidth - minValue;
    int yOffset = mLayoutHeight - minValue;

    // Add the offset
    mPaddingTop = this.getPaddingTop() + (yOffset / 2);
    mPaddingBottom = this.getPaddingBottom() + (yOffset / 2);
    mPaddingLeft = this.getPaddingLeft() + (xOffset / 2);
    mPaddingRight = this.getPaddingRight() + (xOffset / 2);

    int width = getWidth(); //this.getLayoutParams().width;
    int height = getHeight(); //this.getLayoutParams().height;


    mCircleBounds = new RectF(mPaddingLeft + mBarWidth,
        mPaddingTop + mBarWidth,
        width - mPaddingRight - mBarWidth,
        height - mPaddingBottom - mBarWidth);
    mInnerCircleBound = new RectF(
        mPaddingLeft + (mBarWidth * 2f),
        mPaddingTop + (mBarWidth * 2f),
        width - mPaddingRight - (mBarWidth * 2f),
        height - mPaddingBottom - (mBarWidth * 2f));
    mInnerBitmapBound = new RectF(
      mPaddingLeft + (mBarWidth * 6f),
      mPaddingTop + (mBarWidth * 6f),
      width - mPaddingRight - (mBarWidth * 6f),
      height - mPaddingBottom - (mBarWidth * 6f));
    mCircleInnerContour = new RectF(mCircleBounds.left + (mRimWidth / 2.0f) + (mContourSize / 2.0f), mCircleBounds.top + (mRimWidth / 2.0f) + (mContourSize / 2.0f), mCircleBounds.right - (mRimWidth / 2.0f) - (mContourSize / 2.0f), mCircleBounds.bottom - (mRimWidth / 2.0f) - (mContourSize / 2.0f));
    mCircleOuterContour = new RectF(mCircleBounds.left - (mRimWidth / 2.0f) - (mContourSize / 2.0f), mCircleBounds.top - (mRimWidth / 2.0f) - (mContourSize / 2.0f), mCircleBounds.right + (mRimWidth / 2.0f) + (mContourSize / 2.0f), mCircleBounds.bottom + (mRimWidth / 2.0f) + (mContourSize / 2.0f));

    int fullRadius = (width - mPaddingRight - mBarWidth) / 2;
    mCircleRadius = (fullRadius - mBarWidth) + 1;
    mCenter = new PointF(mCircleBounds.centerX(), mCircleBounds.centerY());
  }

  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);


    if (DEBUG) {
      drawDebug(canvas);
    }

    float degrees = (360f / mMaxValue * mCurrentValue);

    //Draw the background circle
    if (mBackgroundCircleColor != 0) {
      canvas.drawArc(mInnerCircleBound, 360, 360, false, mBackgroundCirclePaint);
    }
    //Draw the rim
    if (mRimWidth > 0) {
      canvas.drawArc(mCircleBounds, 360, 360, false, mRimPaint);
    }
    //Draw contour
    if (mContourSize > 0) {
      canvas.drawArc(mCircleOuterContour, 360, 360, false, mContourPaint);
      canvas.drawArc(mCircleInnerContour, 360, 360, false, mContourPaint);
    }

    drawBar(canvas, degrees);
    drawInnerBitmap(canvas);
  }

  private void drawDebug(Canvas canvas) {
    Paint innerRectPaint = new Paint();

    innerRectPaint.setColor(Color.MAGENTA);
    canvas.drawRect(mInnerCircleBound, innerRectPaint);

    innerRectPaint.setColor(Color.YELLOW);
    canvas.drawRect(mInnerBitmapBound, innerRectPaint);
  }

  private void drawBar(Canvas canvas, float degrees) {
    canvas.drawArc(mCircleBounds, mStartAngle, degrees, false, mBarPaint);
  }

  private void drawInnerBitmap(Canvas canvas) {
    canvas.drawBitmap(mCenterBitmap, null, mInnerBitmapBound, mCenterPaint);
  }

  /**
   * Set the value of the circle view without an animation.
   * Stops any currently active animations.
   *
   * @param value The value.
   */
  public void setValue(float value) {
    Message msg = new Message();
    msg.what = TierBadgeView.AnimationMsg.SET_VALUE.ordinal();
    msg.obj = new float[]{value, value};
    mAnimationHandler.sendMessage(msg);
  }

  /**
   * Sets the value of the circle view with an animation.
   *
   * @param valueFrom     start value of the animation
   * @param valueTo       value after animation
   * @param animationDuration the duration of the animation in milliseconds
   */
  public void setValueAnimated(float valueFrom, float valueTo, long animationDuration) {
    mAnimationDuration = animationDuration;
    Message msg = new Message();
    msg.what = TierBadgeView.AnimationMsg.SET_VALUE_ANIMATED.ordinal();
    msg.obj = new float[]{valueFrom, valueTo};
    mAnimationHandler.sendMessage(msg);
  }

  /**
   * Sets the value of the circle view with an animation.
   * The current value is used as the start value of the animation
   *
   * @param valueTo       value after animation
   * @param animationDuration the duration of the animation in milliseconds.
   */
  public void setValueAnimated(float valueTo, long animationDuration) {

    mAnimationDuration = animationDuration;
    Message msg = new Message();
    msg.what = TierBadgeView.AnimationMsg.SET_VALUE_ANIMATED.ordinal();
    msg.obj = new float[]{mCurrentValue, valueTo};
    mAnimationHandler.sendMessage(msg);
  }

  /**
   * Sets the value of the circle view with an animation.
   * The current value is used as the start value of the animation
   *
   * @param valueTo value after animation
   */
  public void setValueAnimated(float valueTo) {

    mAnimationDuration = 1200;
    Message msg = new Message();
    msg.what = TierBadgeView.AnimationMsg.SET_VALUE_ANIMATED.ordinal();
    msg.obj = new float[]{mCurrentValue, valueTo};
    mAnimationHandler.sendMessage(msg);
  }

  /**
   * @param angle The angle in degree to normalize
   * @return the angle between 0 (EAST) and 360
   */
  public static float normalizeAngle(float angle) {
    return (((angle % 360) + 360) % 360);
  }

}


