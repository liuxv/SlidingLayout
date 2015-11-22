package com.liuxv.sliding.utils;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;

import com.liuxv.sliding.SlidingLayout;

/**
 * @author liuxu34@wanda.cn (Liu Xu)
 */
public class SlidingUtils {

  /**
   * find parent and register interceptor
   * 
   * @param view
   * @param interceptor
   */
  public static void findAndRegisterRightFlingInterceptor(View view,
      SlidingLayout.RightFlingInterceptor interceptor) {
    if (view == null) {
      return;
    }

    ViewParent viewParent = view.getParent();

    while (true) {
      if (viewParent == null) {
        return;
      }
      if (viewParent instanceof SlidingLayout) {
        ((SlidingLayout) viewParent).addInterceptor(interceptor);
        return;
      }
      viewParent = viewParent.getParent();
    }

  }

  /**
   * get coordinate in parent's coordinate system
   * 
   * @param view target view
   * @param location an array of two integers in which to hold the coordinates
   */
  public static void getCoordinateInParent(View view, MotionEvent event, int[] location) {
    if (view == null || event == null || view.getParent() == null || location.length < 2) {
      return;
    }
    int parentLocation[] = new int[2];
    ((View) view.getParent()).getLocationOnScreen(parentLocation);
    if (parentLocation != null && parentLocation.length > 1) {
      location[0] = (int) event.getX() - parentLocation[0];
      location[1] = (int) event.getY() - parentLocation[1];
    }
  }


}
