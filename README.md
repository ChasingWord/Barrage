# Barrage
安卓弹幕

![Image text](https://raw.githubusercontent.com/ChasingWord/Barrage/master/screenshots/1.png)

使用:
1、布局添加
    <com.example.barrage.barrage.BarrageLayout
        android:id="@+id/bl"
        android:layout_above="@id/et_input"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
        
2、设置数据源及总时间
    ArrayList<Barrage> all = new ArrayList<>();
    all.add();  .....数据源
    mBarrageLayout.setData(all);
    mBarrageLayout.setTotalTime(all.get(all.size() - 1).getTime() + 200);
    
3、开始滚动
    mBarrageLayout.startScrollBarrage();
    mBarrageLayout.pause();   
    mBarrageLayout.resume();
 
4、进度更新时重新设置当前时间
    mBarrageLayout.setCurrentTime(int progress);
