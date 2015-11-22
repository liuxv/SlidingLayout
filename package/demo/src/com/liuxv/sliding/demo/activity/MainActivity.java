package com.liuxv.sliding.demo.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.liuxv.sliding.demo.fragment.DetailFragment;

public class MainActivity extends BaseActivity {

  @Override
  protected void onCreate(Bundle onSaveInstanceState) {
    super.onCreate(onSaveInstanceState);

    DetailFragment fragment =
        (DetailFragment) Fragment.instantiate(this, DetailFragment.class.getName());
    replaceFragment(fragment);
  }

  @Override
  public boolean getCanFlingBack() {
    return false;
  }

  @Override
  public boolean getCanRelativeMove() {
    return false;
  }
}
