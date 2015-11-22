package com.liuxv.sliding.view;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.liuxv.sliding.SlidingLayout;
import com.liuxv.sliding.utils.SlidingUtils;

/**
 * @author liuxu87@gmail.com (Liu Xu)
 */
public class SlidingViewPager extends ViewPager
    implements SlidingLayout.RightFlingInterceptor {

  private boolean mCanSlide = true;

  public SlidingViewPager(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public SlidingViewPager(Context context) {
    super(context);
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    SlidingUtils.findAndRegisterRightFlingInterceptor(this, this);
  }

  @Override
  public boolean isAllowedRightFlingBack(MotionEvent ev) {
    return getCurrentItem() == 0 && mCanSlide;
  }

  public void setSlideable(boolean slideable) {
    mCanSlide = slideable;
  }
}
