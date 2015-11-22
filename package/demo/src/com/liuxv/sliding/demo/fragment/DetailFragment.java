package com.liuxv.sliding.demo.fragment;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.liuxv.sliding.demo.R;
import com.liuxv.sliding.demo.activity.DetailActivity;
import com.liuxv.sliding.demo.activity.HorizontalDetailActivity;

/**
 * @author liuxu87@gmail.com (Liu Xu)
 */
public class DetailFragment extends BaseFragment {

  private static final int[] BACKGROUND_COLOR_RES = {R.color.colorPrimary,
      R.color.colorPrimaryDark, R.color.colorAccent, R.color.green};

  private RelativeLayout mContainer;
  private Button mButton;


  private int mPrePageColorIndex = -1;
  private int mCurrentColorIndex = 0;

  @Override
  protected int getLayoutResId() {
    return R.layout.detail_fragment;
  }

  @Override
  protected void onInflated(View contentView, Bundle savedInstanceState) {
    getBundle();
    initView();
    randomBackGround(mPrePageColorIndex);
    setListener();
  }

  private void getBundle() {
    Bundle bundle = getArguments();
    if (bundle != null) {
      mPrePageColorIndex = bundle.getInt(DetailActivity.LAST_COLOR_INDEX);
    }
  }

  private void initView() {
    mContainer = (RelativeLayout) mContentView.findViewById(R.id.detail_background);
    mButton = (Button) mContentView.findViewById(R.id.detail_open_button);
  }

  private void randomBackGround(int lastIndex) {

    mCurrentColorIndex = (int) Math.floor(Math.random() * BACKGROUND_COLOR_RES.length);
    while (true) {
      if (lastIndex < 0 || mCurrentColorIndex != lastIndex) {
        break;
      }
      mCurrentColorIndex = (int) Math.floor(Math.random() * BACKGROUND_COLOR_RES.length);
    }

    mContainer.setBackgroundResource(BACKGROUND_COLOR_RES[mCurrentColorIndex]);
  }

  private void setListener() {
    mButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (Math.random() < 0.3) {
          HorizontalDetailActivity.launch(v.getContext());
          return;
        }
        DetailActivity.launch(v.getContext(), mCurrentColorIndex);
      }
    });
  }

}
