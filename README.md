# SlidingLayout

先看效果

![](https://raw.githubusercontent.com/liuxv/SlidingLayout/master/package/demo/sliding.gif)

主要提供右划退出，和前一个界面联动的效果。

实现简单，效率高。可在基类实现，会增加一层 layout。

比同类 sliding 库 提供的功能多出的亮点在于，已解决所有常见横滑 View Group 的滑动冲突问题（例如: ViewPager,HorizontalScrollView,Gallery）
即滑到最左侧第一个时，才会右划退出。
下面是 HorizontalScrollView 的 Demo ，只有滑到第一个的时候才会右划退出。

![](https://raw.githubusercontent.com/liuxv/SlidingLayout/master/package/demo/horizontal.gif)


