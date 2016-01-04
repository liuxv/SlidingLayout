package com.liuxv.sliding;

import android.app.Activity;
import android.util.Log;
import android.view.View;

import com.liuxv.sliding.component.SlideActivity;
import com.liuxv.sliding.listener.SlidingListener;
import com.liuxv.sliding.utils.ActivityUtils;

import java.util.Stack;

/**
 * Sliding 空间的入口类，管理 activity 调用桟，对应生命周期中的动画，和维护 SlidingLayout 的属性
 * 
 * @author liuxu87@gmail.com (Liu Xu)
 */
public class SlidingHelper {

  private static final float CLOSE_PERCENT = -0.3f;
  private static final float ORIGIN_POSITION = 0.0f;
  private static final float ORIGIN_OFFSET = 0.0f;

  /**
   * 由于 activity 的进入动画没有回调，我们只能设计一个 delay 。通过和 onWindowFocusChanged 配合，达到最小误差
   */
  private static final long ENTER_EXIT_ANIM_DELAY = 100;

  private static final Stack<SlidingPager> ACTIVITY_STACK = new Stack<>();

  /**
   * activity 生命周期相关
   */

  public static void onCreate(SlideActivity slideActivity) {
    if (slideActivity != null) {
      push(slideActivity);
      setEnterExitAnim(slideActivity);
    }
  }

  public static void onWindowFocusChanged(SlideActivity slideActivity, boolean hasFocus) {
    if (slideActivity == null) {
      return;
    }
    SlidingPager pager = getPrePage(slideActivity);
    if (pager == null) {
      return;
    }
    if (hasFocus) {
      if (pager.getPageTransaction().isAnimEnable()) {
        startEnterExitAnim(pager);
      }
    }
  }

  public static void onNewIntent(SlideActivity slideActivity) {
    if (slideActivity != null) {
      push(slideActivity);
      SlidingPager pager = findPagerByActivity(slideActivity);
      if (pager != null) {
        startCloseEnterAnim(pager);
      }
    }
  }

  public static void onDestroy(SlideActivity slideActivity) {
    if (slideActivity == null) {
      return;
    }
    SlidingPager removePager = findPagerByActivity(slideActivity);
    ACTIVITY_STACK.remove(removePager);
  }

  public static void finish(SlideActivity slideActivity) {
    if (slideActivity == null) {
      return;
    }
    SlidingPager prePage = getPrePage(slideActivity);
    if (prePage != null && prePage.getPageTransaction().isAnimEnable()) {
      startCloseEnterAnim(prePage);
    }
  }


  /**
   * 执行动画相关
   */

  /**
   * 设置前一页是否可以视察滚动
   *
   * @param slideActivity
   */
  private static void setEnterExitAnim(SlideActivity slideActivity) {
    if (slideActivity == null) {
      return;
    }
    SlidingPager prePage = getPrePage(slideActivity);
    if (prePage != null) {
      prePage.getPageTransaction().setAnimEnable(slideActivity.getCanRelativeMove());
    }
  }

  private static void startEnterExitAnim(SlidingPager pager) {
    if (pager == null || pager.getSlidingLayout() == null) {
      return;
    }
    PageTransaction pageTransaction = pager.getPageTransaction();

    if (pageTransaction.isTransactionEnded()) {
      pageTransaction.beginNewTransaction();
    }

    if (pageTransaction.isEnterAnimSchedule()) {
      return;
    }
    pageTransaction.scheduleEnterAnimSchedule();

    final SlidingPager finalPager = pager;
    finalPager.getSlidingLayout().postDelayed(new Runnable() {
      @Override
      public void run() {
        if (finalPager.getPageTransaction().needDoEnterAnim()) {
          finalPager.getPageTransaction().doEnterAnim(finalPager.getSlidingLayout(), CLOSE_PERCENT);
        }
        finalPager.getPageTransaction().endEnterTransaction();
      }
    }, ENTER_EXIT_ANIM_DELAY);
  }


  private static void startCloseEnterAnim(SlidingPager pager) {
    if (pager == null || pager.getSlidingLayout() == null) {
      return;
    }
    if (pager.getPageTransaction().needDoExitAnim()) {
      pager.getPageTransaction().doExitAnim(pager.getSlidingLayout(), ORIGIN_POSITION);
    }
    pager.getPageTransaction().endExitTransaction();
  }

  /**
   * 创建和查找 pager 相关 ，以及 pager 的桟操作
   */

  private static SlidingPager createPager(SlideActivity slideActivity) {
    return new SlidingPager(slideActivity, initFlingLayout(slideActivity));
  }



  private static SlidingPager getCurPage() {
    if (ACTIVITY_STACK.isEmpty()) {
      return null;
    }
    return ACTIVITY_STACK.peek();

  }

  private static SlidingPager getPrePage(SlideActivity slideActivity) {
    if (slideActivity == null) {
      return null;
    }
    SlidingPager slidingPager = findPagerByActivity(slideActivity);
    int index = ACTIVITY_STACK.indexOf(slidingPager);
    if (index > 0) {
      return ACTIVITY_STACK.get(index - 1);
    } else {
      return null;
    }
  }

  private static SlidingPager push(SlideActivity slideActivity) {
    if (slideActivity == null) {
      return null;
    }
    SlidingPager pager = createPager(slideActivity);
    int index = ACTIVITY_STACK.indexOf(pager);
    if (index < 0) {
      ACTIVITY_STACK.push(pager);
      return pager;
    } else {
      while (!ACTIVITY_STACK.isEmpty()) {
        SlidingPager slidingPager = ACTIVITY_STACK.peek();
        if (pager.equals(slidingPager)) {
          return slidingPager;
        } else {
          ACTIVITY_STACK.pop();
        }
      }
    }
    return null;
  }

  private static SlidingPager findPagerByActivity(SlideActivity slideActivity) {
    if (slideActivity == null) {
      return null;
    }
    for (SlidingPager pager : ACTIVITY_STACK) {
      if (pager != null && pager.getSlideActivity() == slideActivity) {
        return pager;
      }
    }
    return null;
  }


  /**
   * SlidingLayout 初始化，滚动监听，位移
   */

  private static SlidingLayout initFlingLayout(SlideActivity slideActivity) {
    if (!(slideActivity instanceof Activity)) {
      throw new IllegalArgumentException("it must be a activity to implements SlideActivity");
    }
    SlidingLayout mSlidingLayout =
            (SlidingLayout) ((Activity) slideActivity).findViewById(R.id.sliding_pane_layout);
    if (mSlidingLayout == null) {
      Log.w("SlidingHelper", "there is no sliding layout , this activity can not sliding");
      return null;
    }
    mSlidingLayout.setSlideable(slideActivity.getCanFlingBack());
    mSlidingLayout.setShadowResourceLeft(R.drawable.sliding_shadow);
    if (slideActivity.getCanFlingBack()) {
      mSlidingLayout.setSlidingListener(new SlidingListenerDelegate());
    }
    return mSlidingLayout;
  }

  private static void findSlidingLayoutAndSetOffset(View panel, float slideOffset) {
    Activity activity = ActivityUtils.findActivity(panel);
    if (activity instanceof SlideActivity) {
      if (!((SlideActivity) activity).getCanRelativeMove()) {
        return;
      }
      SlidingPager prePage = getPrePage((SlideActivity) activity);
      if (prePage == null) {
        return;
      }
      SlidingLayout slidingLayout = prePage.getSlidingLayout();
      if (slidingLayout != null) {
        slidingLayout.setXOffset(CLOSE_PERCENT * Math.max(1.0f - slideOffset, ORIGIN_OFFSET));
      }
    }
  }

  private static void closeActivity(View panel) {
    Activity activity = ActivityUtils.findActivity(panel);
    if (activity instanceof SlideActivity) {
      if (!((SlideActivity) activity).getCanRelativeMove()) {
        activity.finish();
        return;
      }
      SlidingPager prePage = getPrePage((SlideActivity) activity);
      if (prePage == null) {
        activity.finish();
        return;
      }
      SlidingLayout slidingLayout = prePage.getSlidingLayout();
      if (slidingLayout != null) {
        slidingLayout.setXOffset(ORIGIN_OFFSET);
      }
      activity.finish();
    }
  }

  private static class SlidingListenerDelegate implements SlidingListener {

    @Override
    public void onPanelSlide(View panel, float slideOffset) {
      findSlidingLayoutAndSetOffset(panel, slideOffset);
    }

    @Override
    public void onPanelOpened(View panel) {
      closeActivity(panel);
    }

    @Override
    public void onPanelClosed(View panel) {

    }
  }

  /**
   * SlidingLayout 配置
   */

  public static void setSlidingListener(SlideActivity activity, final SlidingListener listener) {
    if (activity == null || listener == null) {
      return;
    }
    SlidingPager pager = findPagerByActivity(activity);
    pager.getSlidingLayout().setSlidingListener(new SlidingListenerDelegate() {
      @Override
      public void onPanelSlide(View panel, float slideOffset) {
        super.onPanelSlide(panel, slideOffset);
        listener.onPanelSlide(panel, slideOffset);
      }

      @Override
      public void onPanelOpened(View panel) {
        super.onPanelOpened(panel);
        listener.onPanelOpened(panel);
      }

      @Override
      public void onPanelClosed(View panel) {
        super.onPanelClosed(panel);
        listener.onPanelClosed(panel);
      }
    });
  }

}
