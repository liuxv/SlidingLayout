package com.liuxv.sliding;

/**
 * 由于进入进出顺序不确定，需要判断一些 transaction 状态
 * 此类用于维护 transaction
 *
 * @author liuxu87@gmail.com (Liu Xu)
 */
public class PageTransaction {

  /**
   * 是否允许视察动画
   */
  private boolean mAnimEnable;

  /**
   * 是否设定了进入动画
   */
  private boolean mIsEnterAnimSchedule;

  /**
   * 是否执行了进入、退出动画
   */
  private boolean mIsPerformedEnterAnim;
  private boolean mIsPerformedExitAnim;

  /**
   * 是否调用了进入、退出结束
   */
  private boolean mEndEnterTransaction;
  private boolean mEndExitTransaction;

  public PageTransaction() {}

  public boolean isAnimEnable() {
    return mAnimEnable;
  }

  public boolean isEnterAnimSchedule() {
    return mIsEnterAnimSchedule;
  }

  public boolean needDoExitAnim() {
    return mIsPerformedEnterAnim && (!mIsPerformedExitAnim)
            && mEndEnterTransaction && (!mEndExitTransaction);
  }

  public boolean needDoEnterAnim() {
    return (!mIsPerformedEnterAnim) && (!mIsPerformedExitAnim)
            && (!mEndEnterTransaction) && (!mEndExitTransaction);
  }

  public void setAnimEnable(boolean isEnable) {
    mAnimEnable = isEnable;
  }

  public void scheduleEnterAnimSchedule() {
    mIsEnterAnimSchedule = true;
  }

  public void doEnterAnim(SlidingLayout slidingLayout, float enterPosition) {
    mIsPerformedEnterAnim = true;
    slidingLayout.slientSmoothSlideTo(enterPosition);
  }

  public void doExitAnim(SlidingLayout slidingLayout, float closePosition) {
    mIsPerformedExitAnim = true;
    slidingLayout.slientSmoothSlideTo(closePosition);
  }

  public boolean isTransactionEnded() {
    return mEndEnterTransaction && mEndExitTransaction;
  }

  public void endEnterTransaction() {
    mEndEnterTransaction = true;
  }

  public void endExitTransaction() {
    mEndExitTransaction = true;
  }

  public void beginNewTransaction() {
    mIsEnterAnimSchedule = false;
    mIsPerformedEnterAnim = false;
    mIsPerformedExitAnim = false;
    mEndEnterTransaction = false;
    mEndExitTransaction = false;
  }

}
