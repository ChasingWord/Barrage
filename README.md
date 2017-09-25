﻿# Barrage
安卓弹幕

![Image text](https://raw.githubusercontent.com/ChasingWord/Barrage/master/screenshots/1.png)

## 使用:
### 1、布局添加
	<com.example.barrage.barrage.BarrageLayout
		android:id="@+id/bl"
		android:layout_above="@id/et_input"
        	android:layout_width="match_parent"
        	android:layout_height="match_parent" />
      
### 2、设置数据源及总时间
	ArrayList<Barrage> all = new ArrayList<>();
	all.add();  .....数据源
	mBarrageLayout.setData(all);
	mBarrageLayout.setTotalTime(all.get(all.size() - 1).getTime() + 200);
    
### 3、开始滚动
	mBarrageLayout.startScrollBarrage();
	mBarrageLayout.pause();   
	mBarrageLayout.resume();
	
### 4、进度更新时重新设置当前时间
	mBarrageLayout.setCurrentTime(int progress);

设置行高、字体大小：<br/>
xmlns:barrage="http://schemas.android.com/apk/res-auto"<br/>
barrage:textSize="15sp"<br/>
barrage:lineHeight="32dp"<br/>

字体颜色在Barrage类进行设置：<br/>
barrage.setTextColorResId(int resId);