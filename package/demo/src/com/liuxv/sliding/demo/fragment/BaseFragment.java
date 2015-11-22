package com.liuxv.sliding.demo.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Field;

/**
 * @author liuxu87@gmail.com (Liu Xu)
 */
public abstract class BaseFragment extends Fragment {

  protected View mContentView;
  protected boolean mIsInflated;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    mContentView = inflater.inflate(getLayoutResId(), container, false);
    return mContentView;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    if (mContentView != null) {
      onInflated(mContentView, savedInstanceState);
      mIsInflated = true;
    }
  }

  public boolean onKeyDown(int keyCode, KeyEvent event) {
    return false;
  }

  /**
   * Called when the Fragment is inflated
   *
   * @param contentView
   * @param savedInstanceState
   */
  protected abstract void onInflated(View contentView, Bundle savedInstanceState);

  /**
   * @return the layout resource id of the fragment content
   */
  protected abstract int getLayoutResId();


  /**
   * Get the root view of the fragment.
   *
   * @deprecated use {@link android.support.v4.app.Fragment#getView()} instead,
   *             but need to check that getView will return null after onDetached, so be careful.
   * @return the root view
   */
  public View getContentView() {
    return mContentView;
  }


  @Override
  public void onDetach() {
    super.onDetach();
    try {
      Field childFragmentManager = Fragment.class.getDeclaredField("mChildFragmentManager");
      childFragmentManager.setAccessible(true);
      childFragmentManager.set(this, null);
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  public void onNewIntent(Intent intent) {

  }

}
