package com.liuxv.sliding;

import com.liuxv.sliding.component.SlideActivity;

/**
 * @author liuxu87@gmail.com (Liu Xu)
 */
public class SlidingPager {

  private SlideActivity mSlideActivity;
  private SlidingLayout mSlidingLayout;
  private boolean mNeedEnterAnim;


  public SlidingPager(SlideActivity baseActivity, SlidingLayout slidingLayout) {
    mSlideActivity = baseActivity;
    mSlidingLayout = slidingLayout;
    mNeedEnterAnim = false;
  }

  public SlideActivity getSlideActivity() {
    return mSlideActivity;
  }

  public SlidingLayout getSlidingLayout() {
    return mSlidingLayout;
  }

  public boolean isNeedEnterAnim() {
    return mNeedEnterAnim;
  }

  public void setNeedEnterAnim(boolean needEnterAnim) {
    mNeedEnterAnim = needEnterAnim;
  }


  public void resetAnimFlag() {
    mNeedEnterAnim = false;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof SlidingPager) {
      return ((SlidingPager) o).getSlideActivity() == mSlideActivity;
    } else {
      return false;
    }
  }
}
