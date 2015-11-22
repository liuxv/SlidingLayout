package com.liuxv.sliding.demo.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;

import com.liuxv.sliding.SlidingHelper;
import com.liuxv.sliding.component.SlideActivity;
import com.liuxv.sliding.demo.R;
import com.liuxv.sliding.demo.fragment.BaseFragment;

/**
 * @author liuxu87@gmail.com (Liu Xu)
 */
public class BaseActivity extends AppCompatActivity implements SlideActivity {

  protected BaseFragment mFragment;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(getLayoutId());
    SlidingHelper.onCreate(this);
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    SlidingHelper.onWindowFocusChanged(this, hasFocus);
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    SlidingHelper.onNewIntent(this);
    if (mFragment != null) {
      mFragment.onNewIntent(intent);
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    SlidingHelper.onDestroy(this);
  }

  @Override
  public void finish() {
    super.finish();
    SlidingHelper.finish(this);
  }

  protected void replaceFragment(Fragment newFragment) {
    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
    transaction.replace(R.id.fragment_container, newFragment);
    transaction.commitAllowingStateLoss();
  }

  /**
   * get layout id .
   * notice: id : fragment_container , sliding_pane_layout is need in layout
   */
  protected int getLayoutId() {
    return R.layout.base_fragment_activity;
  }

  /**
   * @return this activity is need right fling close
   */
  @Override
  public boolean getCanFlingBack() {
    return true;
  }

  /**
   * @return under this activity is need relative move
   */
  @Override
  public boolean getCanRelativeMove() {
    return true;
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (mFragment != null) {
      if (mFragment.onKeyDown(keyCode, event)) {
        return true;
      }
    }
    return super.onKeyDown(keyCode, event);
  }

}
