package com.liuxv.sliding.listener;

import android.view.View;

/**
 * Listener for monitoring events about sliding panes.
 *
 * @author liuxu34@wanda.cn (Liu Xu)
 */
public interface SlidingListener {
  /**
   * Called when a sliding pane's position changes.
   *
   * @param panel The child view that was moved
   * @param slideOffset The new offset of this sliding pane within its range, from 0-1
   */
  public void onPanelSlide(View panel, float slideOffset);

  /**
   * Called when a sliding pane becomes slid completely open. The pane may or may not
   * be interactive at this point depending on how much of the pane is visible.
   *
   * @param panel The child view that was slid to an open position, revealing other panes
   */
  public void onPanelOpened(View panel);

  /**
   * Called when a sliding pane becomes slid completely closed. The pane is now guaranteed
   * to be interactive. It may now obscure other views in the layout.
   *
   * @param panel The child view that was slid to a closed position
   */
  public void onPanelClosed(View panel);
}
