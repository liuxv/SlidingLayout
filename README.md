# SlidingLayout

先看效果

![](https://raw.githubusercontent.com/liuxv/SlidingLayout/master/package/demo/sliding.gif)

* 主要提供右划退出，和前一个界面联动的效果。

* 实现简单，效率高。可在基类实现，会增加一层 layout。

* 比同类 sliding 库 提供的功能多出的亮点在于，已解决所有常见横滑 View Group 的滑动冲突问题（例如: ViewPager,HorizontalScrollView,Gallery），即滑到最左侧第一个时，才会右划退出。


下面是 HorizontalScrollView 的 Demo ，只有滑到第一个的时候才会右划退出。

![](https://raw.githubusercontent.com/liuxv/SlidingLayout/master/package/demo/horizontal.gif)


* 接口简单，只需要在 BaseActivity 加入这样的几行代码就可以了。

```java
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
```
