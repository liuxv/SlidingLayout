package com.liuxv.sliding.component;


/**
 * 可滑动的 activity 需要集成的接口
 * 
 * @author liuxu87@gmail.com (Liu Xu)
 */
public interface SlideActivity {

  /**
   * @return true 当前界面可以滑动退出， false 不能滑动退出。
   */
  boolean getCanFlingBack();

  /**
   * @return true 下面的界面(前一个界面)将跟随滑动， false 不跟随滑动
   */
  boolean getCanRelativeMove();
}