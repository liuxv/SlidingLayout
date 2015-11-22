package com.liuxv.sliding.demo.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.liuxv.sliding.demo.fragment.HorizontalDetailFragment;

/**
 * @author liuxu87@gmail.com (Liu Xu)
 */
public class HorizontalDetailActivity extends BaseActivity {


  public static void launch(Context context) {
    Intent intent = new Intent(context, HorizontalDetailActivity.class);
    if (!(context instanceof Activity)) {
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }
    context.startActivity(intent);
  }

  @Override
  protected void onCreate(Bundle onSaveInstanceState) {
    super.onCreate(onSaveInstanceState);

    HorizontalDetailFragment fragment = (HorizontalDetailFragment) Fragment.instantiate(this,
        HorizontalDetailFragment.class.getName());
    replaceFragment(fragment);
  }
}
