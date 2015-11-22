package com.liuxv.sliding.demo.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.liuxv.sliding.demo.fragment.DetailFragment;

/**
 * @author liuxu87@gmail.com (Liu Xu)
 */
public class DetailActivity extends BaseActivity {

  public static String LAST_COLOR_INDEX = "last_color_index";

  public static void launch(Context context, int lastColorIndex) {
    Intent intent = new Intent(context, DetailActivity.class);
    Bundle bundle = new Bundle();
    bundle.putInt(LAST_COLOR_INDEX, lastColorIndex);
    intent.putExtras(bundle);
    if (!(context instanceof Activity)) {
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }
    context.startActivity(intent);
  }

  @Override
  protected void onCreate(Bundle onSaveInstanceState) {
    super.onCreate(onSaveInstanceState);

    Intent intent = getIntent();
    Bundle bundle = null;
    if (intent != null) {
      bundle = intent.getExtras();
    }
    DetailFragment fragment =
        (DetailFragment) Fragment.instantiate(this, DetailFragment.class.getName(), bundle);
    replaceFragment(fragment);
  }
}
