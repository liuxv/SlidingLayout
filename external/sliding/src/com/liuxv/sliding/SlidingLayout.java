package com.liuxv.sliding;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.DrawableRes;
import android.support.v4.view.AccessibilityDelegateCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;

import com.liuxv.sliding.listener.SlidingListener;
import com.liuxv.sliding.utils.ViewDragHelper;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 目前只支持横向移动 .
 * 可以完全打开的 SlidingPaneLayout. {@link android.support.v4.widget.SlidingPaneLayout} 修改了只允许 1个子 view .
 * 
 * 1. 并且通过 RightFlingInterceptor 可以使子 view 决定是否允许 父 view 拦截.
 * 2. 增加 slientSmoothSlideTo
 * 3. 增加 setXOffset
 *
 * @author liuxu87@gmail.com (Liu Xu)
 */
public class SlidingLayout extends ViewGroup {

  private static final String TAG = "SlidingPaneLayout";

  /**
   * Default shadow width
   */
  private static final int DEFAULT_SHADOW_WIDTH = 15; // dp;

  /**
   * If no fade color is given by default it will fade to 80% gray.
   */
  private static final int DEFAULT_FADE_COLOR = 0xcccccccc;

  /**
   * The fade color used for the sliding panel. 0 = no fading.
   */
  private int mSliderFadeColor = 0;

  /**
   * Minimum velocity that will be detected as a fling
   */
  private static final int MIN_FLING_VELOCITY = ViewConfiguration.getMinimumFlingVelocity(); // dips
                                                                                             // per
                                                                                             // second

  /**
   * The fade color used for the panel covered by the slider. 0 = no fading.
   */
  private int mCoveredFadeColor;

  /**
   * Drawable used to draw the shadow between panes by default.
   */
  private Drawable mShadowDrawableLeft;

  /**
   * Drawable used to draw the shadow between panes to support RTL (right to left language).
   */
  private Drawable mShadowDrawableRight;

  /**
   * shadow cast by sliding view
   */
  private boolean mNeedShadow = true;

  /**
   * True if a panel can slide with the current measurements
   */
  private boolean mCanSlide = true;

  /**
   * The child view that can slide, if any.
   */
  private View mSlideableView;

  /**
   * How far the panel is offset from its closed position.
   * range [0, 1] where 0 = closed, 1 = open.
   */
  private float mSlideOffset;

  /**
   * How far in pixels the slideable panel may move.
   */
  private int mSlideRange;

  /**
   * A panel view is locked into internal scrolling or another condition that
   * is preventing a drag.
   */
  private boolean mIsUnableToDrag;

  private float mInitialMotionX;
  private float mInitialMotionY;

  private SlidingListener mSlidingListener;

  private final ViewDragHelper mDragHelper;

  private boolean mFirstLayout = true;

  /**
   * 是否只允许边界滑动
   */
  private boolean mIsOnlyEdgeEnable = false;

  /**
   * 是否是一次不 callback 的滚动.
   */
  private boolean mIsSlientScroll = false;

  /**
   * 当存在右划手势冲突的子 view ，向父 view 注册拦截器。不允许父 view 直接拦截事件。
   */
  private final List<WeakReference<RightFlingInterceptor>> mInterceptorList = new ArrayList<>();
  /**
   * 确定是否是一次滑动操作，一旦确定方向不可改变
   */
  private boolean mFlingFlag = false;
  /**
   * 确定是都是右划操作
   */
  private boolean mRightFlingFlag = false;
  /**
   * 计算滑动速度
   */
  private VelocityTracker mVelocityTracker;

  private float mDensity;

  private final Rect mTmpRect = new Rect();

  private final ArrayList<DisableLayerRunnable> mPostedRunnables =
      new ArrayList<DisableLayerRunnable>();

  static final SlidingPanelLayoutImpl IMPL;

  static {
    final int deviceVersion = Build.VERSION.SDK_INT;
    if (deviceVersion >= 17) {
      IMPL = new SlidingPanelLayoutImplJBMR1();
    } else if (deviceVersion >= 16) {
      IMPL = new SlidingPanelLayoutImplJB();
    } else {
      IMPL = new SlidingPanelLayoutImplBase();
    }
  }

  public SlidingLayout(Context context) {
    this(context, null);
  }

  public SlidingLayout(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public SlidingLayout(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);

    mDensity = context.getResources().getDisplayMetrics().density;

    setWillNotDraw(false);

    ViewCompat.setAccessibilityDelegate(this, new AccessibilityDelegate());
    ViewCompat.setImportantForAccessibility(this, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);

    mDragHelper = ViewDragHelper.create(this, new DragHelperCallback());
    mDragHelper.setMinVelocity(MIN_FLING_VELOCITY * mDensity);
  }


  /**
   * Set the color used to fade the sliding pane out when it is slid most of the way offscreen.
   *
   * @param color An ARGB-packed color value
   */
  public void setSliderFadeColor(int color) {
    mSliderFadeColor = color;
  }

  /**
   * @return The ARGB-packed color value used to fade the sliding pane
   */
  public int getSliderFadeColor() {
    return mSliderFadeColor;
  }

  /**
   * Set the color used to fade the pane covered by the sliding pane out when the pane
   * will become fully covered in the closed state.
   *
   * @param color An ARGB-packed color value
   */
  public void setCoveredFadeColor(int color) {
    mCoveredFadeColor = color;
  }

  /**
   * @return The ARGB-packed color value used to fade the fixed pane
   */
  public int getCoveredFadeColor() {
    return mCoveredFadeColor;
  }

  public void setSlidingListener(SlidingListener listener) {
    mSlidingListener = listener;
  }

  void dispatchOnPanelSlide(View panel) {
    if (!mIsSlientScroll && mSlidingListener != null) {
      mSlidingListener.onPanelSlide(panel, mSlideOffset);
    }
  }

  void dispatchOnPanelOpened(View panel) {
    if (!mIsSlientScroll && mSlidingListener != null) {
      mSlidingListener.onPanelOpened(panel);
    }
    sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
  }

  void dispatchOnPanelClosed(View panel) {
    if (!mIsSlientScroll && mSlidingListener != null) {
      mSlidingListener.onPanelClosed(panel);
    }
    sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
  }

  void updateObscuredViewsVisibility(View panel) {
    final boolean isLayoutRtl = isLayoutRtlSupport();
    final int startBound = isLayoutRtl ? (getWidth() - getPaddingRight()) :
        getPaddingLeft();
    final int endBound = isLayoutRtl ? getPaddingLeft() :
        (getWidth() - getPaddingRight());
    final int topBound = getPaddingTop();
    final int bottomBound = getHeight() - getPaddingBottom();
    final int left;
    final int right;
    final int top;
    final int bottom;
    if (panel != null && viewIsOpaque(panel)) {
      left = panel.getLeft();
      right = panel.getRight();
      top = panel.getTop();
      bottom = panel.getBottom();
    } else {
      left = right = top = bottom = 0;
    }

    for (int i = 0, childCount = getChildCount(); i < childCount; i++) {
      final View child = getChildAt(i);

      if (child == panel) {
        // There are still more children above the panel but they won't be affected.
        break;
      }

      final int clampedChildLeft = Math.max((isLayoutRtl ? endBound :
          startBound), child.getLeft());
      final int clampedChildTop = Math.max(topBound, child.getTop());
      final int clampedChildRight = Math.min((isLayoutRtl ? startBound :
          endBound), child.getRight());
      final int clampedChildBottom = Math.min(bottomBound, child.getBottom());
      final int vis;
      if (clampedChildLeft >= left && clampedChildTop >= top &&
          clampedChildRight <= right && clampedChildBottom <= bottom) {
        vis = INVISIBLE;
      } else {
        vis = VISIBLE;
      }
      child.setVisibility(vis);
    }
  }

  void setAllChildrenVisible() {
    for (int i = 0, childCount = getChildCount(); i < childCount; i++) {
      final View child = getChildAt(i);
      if (child.getVisibility() == INVISIBLE) {
        child.setVisibility(VISIBLE);
      }
    }
  }

  private static boolean viewIsOpaque(View v) {
    if (ViewCompat.isOpaque(v)) return true;

    // View#isOpaque didn't take all valid opaque scrollbar modes into account
    // before API 18 (JB-MR2). On newer devices rely solely on isOpaque above and return false
    // here. On older devices, check the view's background drawable directly as a fallback.
    if (Build.VERSION.SDK_INT >= 18) return false;

    final Drawable bg = v.getBackground();
    if (bg != null) {
      return bg.getOpacity() == PixelFormat.OPAQUE;
    }
    return false;
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    mFirstLayout = true;
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    mFirstLayout = true;

    for (int i = 0, count = mPostedRunnables.size(); i < count; i++) {
      final DisableLayerRunnable dlr = mPostedRunnables.get(i);
      dlr.run();
    }
    mPostedRunnables.clear();
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

    int widthSize = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
    int heightSize = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);

    int maxLayoutHeight = heightSize - getPaddingTop() - getPaddingBottom();
    int widthAvailable = widthSize - getPaddingLeft() - getPaddingRight();


    final int childCount = getChildCount();

    if (childCount > 1) {
      throw new IllegalStateException(TAG + " can host only one child");
    }

    // measure child
    if (childCount == 1) {
      final View child = getChildAt(0);
      final LayoutParams lp = (LayoutParams) child.getLayoutParams();
      lp.slideable = mCanSlide;

      mSlideableView = child;

      int childWidthSpec;
      final int horizontalMargin = lp.leftMargin + lp.rightMargin;
      if (lp.width == LayoutParams.WRAP_CONTENT) {
        childWidthSpec = MeasureSpec.makeMeasureSpec(widthAvailable - horizontalMargin,
            MeasureSpec.AT_MOST);
      } else if (lp.width == LayoutParams.FILL_PARENT) {
        childWidthSpec = MeasureSpec.makeMeasureSpec(widthAvailable - horizontalMargin,
            MeasureSpec.EXACTLY);
      } else {
        childWidthSpec = MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
      }

      int childHeightSpec;
      if (lp.height == LayoutParams.WRAP_CONTENT) {
        childHeightSpec = MeasureSpec.makeMeasureSpec(maxLayoutHeight, MeasureSpec.AT_MOST);
      } else if (lp.height == LayoutParams.FILL_PARENT) {
        childHeightSpec = MeasureSpec.makeMeasureSpec(maxLayoutHeight, MeasureSpec.EXACTLY);
      } else {
        childHeightSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
      }

      child.measure(childWidthSpec, childHeightSpec);

    }

    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {

    final boolean isLayoutRtl = isLayoutRtlSupport();
    if (isLayoutRtl) {
      mDragHelper.setEdgeTrackingEnabled(ViewDragHelper.EDGE_RIGHT);
    } else {
      mDragHelper.setEdgeTrackingEnabled(ViewDragHelper.EDGE_LEFT);
    }

    final int width = r - l;
    final int paddingStart = isLayoutRtl ? getPaddingRight() : getPaddingLeft();
    final int paddingEnd = isLayoutRtl ? getPaddingLeft() : getPaddingRight();
    final int paddingTop = getPaddingTop();

    final int childCount = getChildCount();
    int xStart = paddingStart;

    if (mFirstLayout) {
      mSlideOffset = 0.f;
    }

    int offset = 0;

    final View child = getChildAt(0);
    final int childWidth = child.getMeasuredWidth();

    final LayoutParams lp = (LayoutParams) child.getLayoutParams();

    final int margin = lp.leftMargin + lp.rightMargin;
    final int range = getMeasuredWidth() - xStart - margin;
    mSlideRange = range;
    final int lpMargin = isLayoutRtl ? lp.rightMargin : lp.leftMargin;
    lp.dimWhenOffset = xStart + lpMargin + range + childWidth / 2 >
        width - paddingEnd;
    final int pos = (int) (range * mSlideOffset);
    xStart += pos + lpMargin;
    mSlideOffset = (float) pos / mSlideRange;

    final int childRight;
    final int childLeft;
    if (isLayoutRtl) {
      childRight = width - xStart + offset;
      childLeft = childRight - childWidth;
    } else {
      childLeft = xStart - offset;
      childRight = childLeft + childWidth;
    }

    final int childTop = paddingTop;
    final int childBottom = childTop + child.getMeasuredHeight();
    child.layout(childLeft, paddingTop, childRight, childBottom);


    if (mFirstLayout) {
      if (mCanSlide) {
        if (((LayoutParams) mSlideableView.getLayoutParams()).dimWhenOffset) {
          dimChildView(mSlideableView, mSlideOffset, mSliderFadeColor);
        }
      } else {
        // Reset the dim level of all children; it's irrelevant when nothing moves.
        for (int i = 0; i < childCount; i++) {
          dimChildView(getChildAt(i), 0, mSliderFadeColor);
        }
      }
      updateObscuredViewsVisibility(mSlideableView);
    }

    mFirstLayout = false;
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    // Recalculate sliding panes and their details
    if (w != oldw) {
      mFirstLayout = true;
    }
  }


  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    if (!mCanSlide) {
      return super.onInterceptTouchEvent(ev);
    }

    detectorFling(ev);
    if (isRightFling() && !canInterceptorRightFling(ev)) {
      return false;
    }

    final int action = MotionEventCompat.getActionMasked(ev);


    if (mIsUnableToDrag && action != MotionEvent.ACTION_DOWN) {
      mDragHelper.cancel();
      return super.onInterceptTouchEvent(ev);
    }

    if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
      mDragHelper.cancel();
      return false;
    }

    boolean interceptTap = false;

    switch (action) {
      case MotionEvent.ACTION_DOWN: {
        mIsUnableToDrag = false;
        final float x = ev.getX();
        final float y = ev.getY();
        mInitialMotionX = x;
        mInitialMotionY = y;

        if (isViewUnder(mSlideableView, ev) && isDimmed(mSlideableView)) {
          interceptTap = true;
        }
        break;
      }

      case MotionEvent.ACTION_MOVE: {
        final float x = ev.getX();
        final float y = ev.getY();
        final float adx = Math.abs(x - mInitialMotionX);
        final float ady = Math.abs(y - mInitialMotionY);
        final int slop = mDragHelper.getTouchSlop();
        if (adx > slop && ady > adx) {
          mDragHelper.cancel();
          mIsUnableToDrag = true;
          return false;
        }
      }
    }

    final boolean interceptForDrag = mDragHelper.shouldInterceptTouchEvent(ev);

    return interceptForDrag || interceptTap;
  }

  @Override
  public boolean onTouchEvent(MotionEvent ev) {

    if (!mCanSlide) {
      return super.onTouchEvent(ev);
    }

    mDragHelper.processTouchEvent(ev);

    final int action = ev.getAction();
    boolean wantTouchEvents = true;

    switch (action & MotionEventCompat.ACTION_MASK) {
      case MotionEvent.ACTION_DOWN: {
        final float x = ev.getX();
        final float y = ev.getY();
        mInitialMotionX = x;
        mInitialMotionY = y;
        break;
      }

      case MotionEvent.ACTION_UP: {
        if (isDimmed(mSlideableView)) {
          final float x = ev.getX();
          final float y = ev.getY();
          final float dx = x - mInitialMotionX;
          final float dy = y - mInitialMotionY;
          final int slop = mDragHelper.getTouchSlop();
          if (dx * dx + dy * dy < slop * slop && isViewUnder(mSlideableView, ev)) {
            // Taps close a dimmed open pane.
            closePane(mSlideableView);
            break;
          }
        }
        break;
      }
    }

    return wantTouchEvents;
  }

  private boolean closePane(View pane) {
    return mFirstLayout || smoothSlideTo(0.f);
  }

  private boolean openPane(View pane) {
    return mFirstLayout || smoothSlideTo(1.f);
  }

  /**
   * @deprecated Renamed to {@link #openPane()} - this method is going away soon!
   */
  @Deprecated
  public void smoothSlideOpen() {
    openPane();
  }

  /**
   * Open the sliding pane if it is currently slideable. If first layout
   * has already completed this will animate.
   *
   * @return true if the pane was slideable and is now open/in the process of opening
   */
  public boolean openPane() {
    return openPane(mSlideableView);
  }

  /**
   * @deprecated Renamed to {@link #closePane()} - this method is going away soon!
   */
  @Deprecated
  public void smoothSlideClosed() {
    closePane();
  }

  /**
   * Close the sliding pane if it is currently slideable. If first layout
   * has already completed this will animate.
   *
   * @return true if the pane was slideable and is now closed/in the process of closing
   */
  public boolean closePane() {
    return closePane(mSlideableView);
  }

  /**
   * set slideable view to position without callback
   *
   * @param offset range [0, 1] where 0 = closed, 1 = open.
   */
  public void setXOffset(float offset) {
    int left = (int) (mSlideRange * offset);
    int oldLeft = mSlideableView.getLeft();
    mSlideOffset = offset;
    mSlideableView.offsetLeftAndRight(left - oldLeft);
  }

  /**
   * Check if the layout is completely open. It can be open either because the slider
   * itself is open revealing the left pane, or if all content fits without sliding.
   *
   * @return true if sliding panels are completely open
   */
  public boolean isOpen() {
    return !mCanSlide || mSlideOffset == 1;
  }

  /**
   * @return true if content in this layout can be slid open and closed
   * @deprecated Renamed to {@link #isSlideable()} - this method is going away soon!
   */
  @Deprecated
  public boolean canSlide() {
    return mCanSlide;
  }

  /**
   * Check if the content in this layout cannot fully fit side by side and therefore
   * the content pane can be slid back and forth.
   *
   * @return true if content in this layout can be slid open and closed
   */
  public boolean isSlideable() {
    return mCanSlide;
  }

  /**
   * set if this layout can be slideable
   *
   * @param slideable
   */
  public void setSlideable(boolean slideable) {
    mCanSlide = slideable;
  }


  public void setOnlyEdgeEnable(boolean isOnlyEdgeEnable) {
    mIsOnlyEdgeEnable = isOnlyEdgeEnable;
  }

  private void onPanelDragged(int newLeft) {
    if (mSlideableView == null) {
      // This can happen if we're aborting motion during layout because everything now fits.
      mSlideOffset = 0;
      return;
    }
    final boolean isLayoutRtl = isLayoutRtlSupport();
    final LayoutParams lp = (LayoutParams) mSlideableView.getLayoutParams();

    int childWidth = mSlideableView.getWidth();
    final int newStart = isLayoutRtl ? getWidth() - newLeft - childWidth : newLeft;

    final int paddingStart = isLayoutRtl ? getPaddingRight() : getPaddingLeft();
    final int lpMargin = isLayoutRtl ? lp.rightMargin : lp.leftMargin;
    final int startBound = paddingStart + lpMargin;

    mSlideOffset = (float) (newStart - startBound) / mSlideRange;

    if (lp.dimWhenOffset) {
      dimChildView(mSlideableView, mSlideOffset, mSliderFadeColor);
    }
    dispatchOnPanelSlide(mSlideableView);
  }

  private void dimChildView(View v, float mag, int fadeColor) {
    final LayoutParams lp = (LayoutParams) v.getLayoutParams();

    if (mag > 0 && fadeColor != 0) {
      final int baseAlpha = (fadeColor & 0xff000000) >>> 24;
      int imag = (int) (baseAlpha * mag);
      int color = imag << 24 | (fadeColor & 0xffffff);
      if (lp.dimPaint == null) {
        lp.dimPaint = new Paint();
      }
      lp.dimPaint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_OVER));
      if (ViewCompat.getLayerType(v) != ViewCompat.LAYER_TYPE_HARDWARE) {
        ViewCompat.setLayerType(v, ViewCompat.LAYER_TYPE_HARDWARE, lp.dimPaint);
      }
      invalidateChildRegion(v);
    } else if (ViewCompat.getLayerType(v) != ViewCompat.LAYER_TYPE_NONE) {
      if (lp.dimPaint != null) {
        lp.dimPaint.setColorFilter(null);
      }
      final DisableLayerRunnable dlr = new DisableLayerRunnable(v);
      mPostedRunnables.add(dlr);
      ViewCompat.postOnAnimation(this, dlr);
    }
  }

  @Override
  protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
    final LayoutParams lp = (LayoutParams) child.getLayoutParams();
    boolean result;
    final int save = canvas.save(Canvas.CLIP_SAVE_FLAG);

    if (mCanSlide && !lp.slideable && mSlideableView != null) {
      // Clip against the slider; no sense drawing what will immediately be covered.
      canvas.getClipBounds(mTmpRect);
      if (isLayoutRtlSupport()) {
        mTmpRect.left = Math.max(mTmpRect.left, mSlideableView.getRight());
      } else {
        mTmpRect.right = Math.min(mTmpRect.right, mSlideableView.getLeft());
      }
      canvas.clipRect(mTmpRect);
    }

    if (Build.VERSION.SDK_INT >= 11) { // HC
      result = super.drawChild(canvas, child, drawingTime);
    } else {
      if (lp.dimWhenOffset && mSlideOffset > 0) {
        if (!child.isDrawingCacheEnabled()) {
          child.setDrawingCacheEnabled(true);
        }
        final Bitmap cache = child.getDrawingCache();
        if (cache != null) {
          canvas.drawBitmap(cache, child.getLeft(), child.getTop(), lp.dimPaint);
          result = false;
        } else {
          Log.e(TAG, "drawChild: child view " + child + " returned null drawing cache");
          result = super.drawChild(canvas, child, drawingTime);
        }
      } else {
        if (child.isDrawingCacheEnabled()) {
          child.setDrawingCacheEnabled(false);
        }
        result = super.drawChild(canvas, child, drawingTime);
      }
    }

    canvas.restoreToCount(save);

    return result;
  }

  private void invalidateChildRegion(View v) {
    IMPL.invalidateChildRegion(this, v);
  }

  /**
   * Smoothly scroll without callback onPanelSlide
   *
   * @param slideOffset position to animate to
   */
  boolean slientSmoothSlideTo(float slideOffset) {
    mIsSlientScroll = true;
    return smoothSlideTo(slideOffset);
  }

  /**
   * Smoothly animate mDraggingPane to the target X position within its range.
   *
   * @param slideOffset position to animate to
   */
  boolean smoothSlideTo(float slideOffset) {
    if (mSlideableView == null) {
      return false;
    }

    final boolean isLayoutRtl = isLayoutRtlSupport();
    final LayoutParams lp = (LayoutParams) mSlideableView.getLayoutParams();

    int x;
    if (isLayoutRtl) {
      int startBound = getPaddingRight() + lp.rightMargin;
      int childWidth = mSlideableView.getWidth();
      x = (int) (getWidth() - (startBound + slideOffset * mSlideRange + childWidth));
    } else {
      int startBound = getPaddingLeft() + lp.leftMargin;
      x = (int) (startBound + slideOffset * mSlideRange);
    }
    // 当没有移动时, 将标志位重置
    if (x == mSlideableView.getLeft()) {
      mIsSlientScroll = false;
    }
    if (mDragHelper.smoothSlideViewTo(mSlideableView, x, mSlideableView.getTop())) {
      setAllChildrenVisible();
      ViewCompat.postInvalidateOnAnimation(this);
      return true;
    }
    return false;
  }

  @Override
  public void computeScroll() {
    if (mDragHelper.continueSettling(true)) {
      ViewCompat.postInvalidateOnAnimation(this);
    }
  }

  /**
   * @param d drawable to use as a shadow
   * @deprecated Renamed to {@link #setShadowDrawableLeft(Drawable d)} to support LTR (left to
   *             right language) and {@link #setShadowDrawableRight(Drawable d)} to support RTL
   *             (right to left
   *             language) during opening/closing.
   */
  @Deprecated
  public void setShadowDrawable(Drawable d) {
    setShadowDrawableLeft(d);
  }

  /**
   * Set a drawable to use as a shadow cast by the right pane onto the left pane
   * during opening/closing.
   *
   * @param d drawable to use as a shadow
   */
  public void setShadowDrawableLeft(Drawable d) {
    mShadowDrawableLeft = d;
  }

  /**
   * Set a drawable to use as a shadow cast by the left pane onto the right pane
   * during opening/closing to support right to left language.
   *
   * @param d drawable to use as a shadow
   */
  public void setShadowDrawableRight(Drawable d) {
    mShadowDrawableRight = d;
  }

  /**
   * set if need shadow cast by sliding view
   * 
   * @param needShadow
   */
  public void setNeedShadow(boolean needShadow) {
    mNeedShadow = needShadow;
  }

  /**
   * Set a drawable to use as a shadow cast by the right pane onto the left pane
   * during opening/closing.
   *
   * @param resId Resource ID of a drawable to use
   */
  @Deprecated
  public void setShadowResource(@DrawableRes int resId) {
    setShadowDrawable(getResources().getDrawable(resId));
  }

  /**
   * Set a drawable to use as a shadow cast by the right pane onto the left pane
   * during opening/closing.
   *
   * @param resId Resource ID of a drawable to use
   */
  public void setShadowResourceLeft(int resId) {
    setShadowDrawableLeft(getResources().getDrawable(resId));
  }

  /**
   * Set a drawable to use as a shadow cast by the left pane onto the right pane
   * during opening/closing to support right to left language.
   *
   * @param resId Resource ID of a drawable to use
   */
  public void setShadowResourceRight(int resId) {
    setShadowDrawableRight(getResources().getDrawable(resId));
  }


  @Override
  public void draw(Canvas c) {
    super.draw(c);
    final boolean isLayoutRtl = isLayoutRtlSupport();
    Drawable shadowDrawable;
    if (isLayoutRtl) {
      shadowDrawable = mShadowDrawableRight;
    } else {
      shadowDrawable = mShadowDrawableLeft;
    }

    final View shadowView = mSlideableView;

    if (shadowView == null || !mNeedShadow) {
      // No need to draw a shadow if we don't have one.
      return;
    }

    if (shadowDrawable == null) {
      shadowDrawable = createDefaultShadowDrawable();
    }

    final int top = shadowView.getTop();
    final int bottom = shadowView.getBottom();

    int shadowWidth = shadowDrawable.getIntrinsicWidth();
    if (shadowWidth <= 0) {
      shadowWidth = (int) (DEFAULT_SHADOW_WIDTH * mDensity + 0.5f);
    }
    final int left;
    final int right;
    if (isLayoutRtlSupport()) {
      left = shadowView.getRight();
      right = left + shadowWidth;
    } else {
      right = shadowView.getLeft();
      left = right - shadowWidth;
    }

    shadowDrawable.setBounds(left, top, right, bottom);
    shadowDrawable.draw(c);
  }

  private GradientDrawable createDefaultShadowDrawable() {
    int[] color = {0x333333, 0xb0333333};
    GradientDrawable shadowDrawable;
    if (isLayoutRtlSupport()) {
      shadowDrawable = new GradientDrawable(
          GradientDrawable.Orientation.RIGHT_LEFT, color);
      shadowDrawable
          .setGradientType(GradientDrawable.LINEAR_GRADIENT);
    } else {
      shadowDrawable = new GradientDrawable(
          GradientDrawable.Orientation.LEFT_RIGHT, color);
      shadowDrawable
          .setGradientType(GradientDrawable.LINEAR_GRADIENT);
    }
    return shadowDrawable;
  }


  /**
   * Tests scrollability within child views of v given a delta of dx.
   *
   * @param v View to test for horizontal scrollability
   * @param checkV Whether the view v passed should itself be checked for scrollability (true),
   *          or just its children (false).
   * @param dx Delta scrolled in pixels
   * @param x X coordinate of the active touch point
   * @param y Y coordinate of the active touch point
   * @return true if child views of v can be scrolled by delta of dx.
   */
  protected boolean canScroll(View v, boolean checkV, int dx, int x, int y) {
    if (v instanceof ViewGroup) {
      final ViewGroup group = (ViewGroup) v;
      final int scrollX = v.getScrollX();
      final int scrollY = v.getScrollY();
      final int count = group.getChildCount();
      // Count backwards - let topmost views consume scroll distance first.
      for (int i = count - 1; i >= 0; i--) {
        // TODO: Add versioned support here for transformed views.
        // This will not work for transformed views in Honeycomb+
        final View child = group.getChildAt(i);
        if (x + scrollX >= child.getLeft() && x + scrollX < child.getRight() &&
            y + scrollY >= child.getTop() && y + scrollY < child.getBottom() &&
            canScroll(child, true, dx, x + scrollX - child.getLeft(),
                y + scrollY - child.getTop())) {
          return true;
        }
      }
    }

    return checkV && ViewCompat.canScrollHorizontally(v, (isLayoutRtlSupport() ? dx : -dx));
  }

  boolean isDimmed(View child) {
    if (child == null) {
      return false;
    }
    final LayoutParams lp = (LayoutParams) child.getLayoutParams();
    return mCanSlide && lp.dimWhenOffset && mSlideOffset > 0;
  }

  @Override
  protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
    return new LayoutParams();
  }

  @Override
  protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
    return p instanceof MarginLayoutParams
        ? new LayoutParams((MarginLayoutParams) p)
        : new LayoutParams(p);
  }

  @Override
  protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
    return p instanceof LayoutParams && super.checkLayoutParams(p);
  }

  @Override
  public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
    return new LayoutParams(getContext(), attrs);
  }

  @Override
  protected Parcelable onSaveInstanceState() {
    Parcelable superState = super.onSaveInstanceState();

    SavedState ss = new SavedState(superState);
    ss.isOpen = isOpen();

    return ss;
  }

  @Override
  protected void onRestoreInstanceState(Parcelable state) {
    SavedState ss = (SavedState) state;
    super.onRestoreInstanceState(ss.getSuperState());

    if (ss.isOpen) {
      openPane();
    } else {
      closePane();
    }
  }

  private class DragHelperCallback extends ViewDragHelper.Callback {

    @Override
    public boolean tryCaptureView(View child, int pointerId) {
      if (mIsUnableToDrag || mIsOnlyEdgeEnable) {
        return false;
      }

      return ((LayoutParams) child.getLayoutParams()).slideable;
    }

    @Override
    public void onViewDragStateChanged(int state) {
      if (mDragHelper.getViewDragState() == ViewDragHelper.STATE_IDLE) {
        if (mSlideOffset == 0) {
          updateObscuredViewsVisibility(mSlideableView);
          dispatchOnPanelClosed(mSlideableView);
        } else if (mSlideOffset == 1) {
          dispatchOnPanelOpened(mSlideableView);
        }
        // 当移动结束时, 将标志位重置
        mIsSlientScroll = false;
      }
    }

    @Override
    public void onViewCaptured(View capturedChild, int activePointerId) {
      // Make all child views visible in preparation for sliding things around
      setAllChildrenVisible();
    }

    @Override
    public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
      onPanelDragged(left);
      invalidate();
    }

    @Override
    public void onViewReleased(View releasedChild, float xvel, float yvel) {
      final LayoutParams lp = (LayoutParams) releasedChild.getLayoutParams();

      int left;
      if (isLayoutRtlSupport()) {
        int startToRight = getPaddingRight() + lp.rightMargin;
        if (xvel < 0 || (xvel == 0 && mSlideOffset > 0.5f)) {
          startToRight += mSlideRange;
        }
        int childWidth = mSlideableView.getWidth();
        left = getWidth() - startToRight - childWidth;
      } else {
        left = getPaddingLeft() + lp.leftMargin;
        if (xvel > 0 || (xvel == 0 && mSlideOffset > 0.5f)) {
          left += mSlideRange;
        }
      }
      mDragHelper.settleCapturedViewAt(left, releasedChild.getTop());
      invalidate();
    }

    @Override
    public int getViewHorizontalDragRange(View child) {
      return mSlideRange;
    }

    @Override
    public int clampViewPositionHorizontal(View child, int left, int dx) {
      final LayoutParams lp = (LayoutParams) mSlideableView.getLayoutParams();

      final int newLeft;
      if (isLayoutRtlSupport()) {
        int startBound = getWidth() -
            (getPaddingRight() + lp.rightMargin + mSlideableView.getWidth());
        int endBound = startBound - mSlideRange;
        newLeft = Math.max(Math.min(left, startBound), endBound);
      } else {
        int startBound = getPaddingLeft() + lp.leftMargin;
        int endBound = startBound + mSlideRange;
        newLeft = Math.min(Math.max(left, startBound), endBound);
      }
      return newLeft;
    }

    @Override
    public int clampViewPositionVertical(View child, int top, int dy) {
      // Make sure we never move views vertically.
      // This could happen if the child has less height than its parent.
      return child.getTop();
    }

    @Override
    public void onEdgeDragStarted(int edgeFlags, int pointerId) {
      mDragHelper.captureChildView(mSlideableView, pointerId);
    }
  }

  public static class LayoutParams extends ViewGroup.MarginLayoutParams {
    private static final int[] ATTRS = new int[] {
        android.R.attr.layout_weight
    };

    /**
     * The weighted proportion of how much of the leftover space
     * this child should consume after measurement.
     */
    public float weight = 0;

    /**
     * True if this pane is the slideable pane in the layout.
     */
    boolean slideable;

    /**
     * True if this view should be drawn dimmed
     * when it's been offset from its default position.
     */
    boolean dimWhenOffset;

    Paint dimPaint;

    public LayoutParams() {
      super(FILL_PARENT, FILL_PARENT);
    }

    public LayoutParams(int width, int height) {
      super(width, height);
    }

    public LayoutParams(android.view.ViewGroup.LayoutParams source) {
      super(source);
    }

    public LayoutParams(MarginLayoutParams source) {
      super(source);
    }

    public LayoutParams(LayoutParams source) {
      super(source);
      this.weight = source.weight;
    }

    public LayoutParams(Context c, AttributeSet attrs) {
      super(c, attrs);

      final TypedArray a = c.obtainStyledAttributes(attrs, ATTRS);
      this.weight = a.getFloat(0, 0);
      a.recycle();
    }

  }

  static class SavedState extends BaseSavedState {
    boolean isOpen;

    SavedState(Parcelable superState) {
      super(superState);
    }

    private SavedState(Parcel in) {
      super(in);
      isOpen = in.readInt() != 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
      super.writeToParcel(out, flags);
      out.writeInt(isOpen ? 1 : 0);
    }

    public static final Parcelable.Creator<SavedState> CREATOR =
        new Parcelable.Creator<SavedState>() {
          public SavedState createFromParcel(Parcel in) {
            return new SavedState(in);
          }

          public SavedState[] newArray(int size) {
            return new SavedState[size];
          }
        };
  }

  interface SlidingPanelLayoutImpl {
    void invalidateChildRegion(SlidingLayout parent, View child);
  }

  static class SlidingPanelLayoutImplBase implements SlidingPanelLayoutImpl {
    public void invalidateChildRegion(SlidingLayout parent, View child) {
      ViewCompat.postInvalidateOnAnimation(parent, child.getLeft(), child.getTop(),
          child.getRight(), child.getBottom());
    }
  }

  static class SlidingPanelLayoutImplJB extends SlidingPanelLayoutImplBase {
    /*
     * Private API hacks! Nasty! Bad!
     * In Jellybean, some optimizations in the hardware UI renderer
     * prevent a changed Paint on a View using a hardware layer from having
     * the intended effect. This twiddles some internal bits on the view to force
     * it to recreate the display list.
     */
    private Method mGetDisplayList;
    private Field mRecreateDisplayList;

    SlidingPanelLayoutImplJB() {
      try {
        mGetDisplayList = View.class.getDeclaredMethod("getDisplayList", (Class[]) null);
      } catch (NoSuchMethodException e) {
        Log.e(TAG, "Couldn't fetch getDisplayList method; dimming won't work right.", e);
      }
      try {
        mRecreateDisplayList = View.class.getDeclaredField("mRecreateDisplayList");
        mRecreateDisplayList.setAccessible(true);
      } catch (NoSuchFieldException e) {
        Log.e(TAG, "Couldn't fetch mRecreateDisplayList field; dimming will be slow.", e);
      }
    }

    @Override
    public void invalidateChildRegion(SlidingLayout parent, View child) {
      if (mGetDisplayList != null && mRecreateDisplayList != null) {
        try {
          mRecreateDisplayList.setBoolean(child, true);
          mGetDisplayList.invoke(child, (Object[]) null);
        } catch (Exception e) {
          Log.e(TAG, "Error refreshing display list state", e);
        }
      } else {
        // Slow path. REALLY slow path. Let's hope we don't get here.
        child.invalidate();
        return;
      }
      super.invalidateChildRegion(parent, child);
    }
  }

  static class SlidingPanelLayoutImplJBMR1 extends SlidingPanelLayoutImplBase {
    @Override
    public void invalidateChildRegion(SlidingLayout parent, View child) {
      ViewCompat.setLayerPaint(child, ((LayoutParams) child.getLayoutParams()).dimPaint);
    }
  }

  class AccessibilityDelegate extends AccessibilityDelegateCompat {
    private final Rect mTmpRect = new Rect();

    @Override
    public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
      final AccessibilityNodeInfoCompat superNode = AccessibilityNodeInfoCompat.obtain(info);
      super.onInitializeAccessibilityNodeInfo(host, superNode);
      copyNodeInfoNoChildren(info, superNode);
      superNode.recycle();

      info.setClassName(SlidingLayout.class.getName());
      info.setSource(host);

      final ViewParent parent = ViewCompat.getParentForAccessibility(host);
      if (parent instanceof View) {
        info.setParent((View) parent);
      }

      // This is a best-approximation of addChildrenForAccessibility()
      // that accounts for filtering.
      final int childCount = getChildCount();
      for (int i = 0; i < childCount; i++) {
        final View child = getChildAt(i);
        if (!filter(child) && (child.getVisibility() == View.VISIBLE)) {
          // Force importance to "yes" since we can't read the value.
          ViewCompat.setImportantForAccessibility(
              child, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
          info.addChild(child);
        }
      }
    }

    @Override
    public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {
      super.onInitializeAccessibilityEvent(host, event);

      event.setClassName(SlidingLayout.class.getName());
    }

    @Override
    public boolean onRequestSendAccessibilityEvent(ViewGroup host, View child,
        AccessibilityEvent event) {
      if (!filter(child)) {
        return super.onRequestSendAccessibilityEvent(host, child, event);
      }
      return false;
    }

    public boolean filter(View child) {
      return isDimmed(child);
    }

    /**
     * This should really be in AccessibilityNodeInfoCompat, but there unfortunately
     * seem to be a few elements that are not easily cloneable using the underlying API.
     * Leave it private here as it's not general-purpose useful.
     */
    private void copyNodeInfoNoChildren(AccessibilityNodeInfoCompat dest,
        AccessibilityNodeInfoCompat src) {
      final Rect rect = mTmpRect;

      src.getBoundsInParent(rect);
      dest.setBoundsInParent(rect);

      src.getBoundsInScreen(rect);
      dest.setBoundsInScreen(rect);

      dest.setVisibleToUser(src.isVisibleToUser());
      dest.setPackageName(src.getPackageName());
      dest.setClassName(src.getClassName());
      dest.setContentDescription(src.getContentDescription());

      dest.setEnabled(src.isEnabled());
      dest.setClickable(src.isClickable());
      dest.setFocusable(src.isFocusable());
      dest.setFocused(src.isFocused());
      dest.setAccessibilityFocused(src.isAccessibilityFocused());
      dest.setSelected(src.isSelected());
      dest.setLongClickable(src.isLongClickable());

      dest.addAction(src.getActions());

      dest.setMovementGranularities(src.getMovementGranularities());
    }
  }

  private class DisableLayerRunnable implements Runnable {
    final View mChildView;

    DisableLayerRunnable(View childView) {
      mChildView = childView;
    }

    @Override
    public void run() {
      if (mChildView.getParent() == SlidingLayout.this) {
        ViewCompat.setLayerType(mChildView, ViewCompat.LAYER_TYPE_NONE, null);
        invalidateChildRegion(mChildView);
      }
      mPostedRunnables.remove(this);
    }
  }

  private boolean isLayoutRtlSupport() {
    return ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL;
  }


  /**
   * 右划拦截器，用于右划手势冲突时的阻止父 view (SlidingLayout)，直接拦截右划手势.
   * 必须使用 view 实现，因为需要判断响应的拦截器是否是当前 view.
   */
  public interface RightFlingInterceptor {
    /**
     * 是否允许父 view (SlidingLayout)，拦截右划手势
     * 
     * @return true 允许拦截，false 不允许拦截
     * @param ev
     */
    boolean isAllowedRightFlingBack(MotionEvent ev);
  }

  public synchronized void addInterceptor(RightFlingInterceptor interceptor) {
    if (!(interceptor instanceof View)) {
      throw new IllegalStateException(TAG + " must use view implement interceptor");
    }
    mInterceptorList.add(new WeakReference<>(interceptor));
  }

  /**
   * 检查子 View 是否允许父 View 拦截右划手势。
   * 
   * @return true 允许拦截，false 不允许拦截
   */
  private boolean canInterceptorRightFling(MotionEvent ev) {
    Iterator<WeakReference<RightFlingInterceptor>> iterator = mInterceptorList.iterator();

    while (iterator.hasNext()) {
      WeakReference<RightFlingInterceptor> weakReference = iterator.next();
      RightFlingInterceptor interceptor = weakReference.get();
      if (interceptor != null) {
        /**
         * 只要有一个子 View 不允许拦截，则不再询问后续.
         * 不予许拦截的子 View 需要处于点击位置
         */
        if (!interceptor.isAllowedRightFlingBack(ev) && isViewUnder((View) interceptor, ev)) {
          return false;
        }
      } else {
        iterator.remove();
      }
    }
    return true;
  }

  private boolean isViewUnder(View view, MotionEvent event) {
    Rect rect = new Rect();
    view.getHitRect(rect);
    View p = (View) view.getParent();
    if (p != null) {
      int parentGlobalPosition[] = new int[2];
      p.getLocationOnScreen(parentGlobalPosition);
      rect.offset(parentGlobalPosition[0], parentGlobalPosition[1]);
    }
    return rect.contains((int) event.getRawX(), (int) event.getRawY());
  }

  /**
   * 检测是否满足滑动的最小速度
   * 
   * @param ev
   */
  private void detectorFling(MotionEvent ev) {
    if (mFlingFlag && ev.getAction() != MotionEvent.ACTION_DOWN) {
      return;
    }
    if (ev.getAction() == MotionEvent.ACTION_DOWN) {
      mFlingFlag = false;
    }

    if (mVelocityTracker == null) {
      mVelocityTracker = VelocityTracker.obtain();
    }
    mVelocityTracker.addMovement(ev);

    mVelocityTracker.computeCurrentVelocity(1000, ViewConfiguration.getMaximumFlingVelocity());

    if (mVelocityTracker.getXVelocity() >= mDragHelper.getTouchSlop()) {
      mRightFlingFlag = true;
      mFlingFlag = true;
    } else {
      mRightFlingFlag = false;
    }
  }

  private boolean isRightFling() {
    return mFlingFlag && mRightFlingFlag;
  }

}
