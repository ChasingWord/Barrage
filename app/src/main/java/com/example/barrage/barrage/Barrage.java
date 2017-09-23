package com.example.barrage.barrage;

/**
 * Created by chasing on 2017/9/20.
 */

public class Barrage {
    private long mTime;//弹幕要显示的时间
    private String mMsg;//弹幕信息
    private int mTextColorResId;//弹幕字体颜色

    public Barrage(){
        mTextColorResId = android.R.color.black;
    }

    public long getTime(){
        return mTime;
    }

    public String getMsg(){
        return mMsg;
    }

    public Barrage setTime(long time){
        mTime = time;
        return this;
    }

    public Barrage setMsg(String msg){
        mMsg = msg;
        return this;
    }

    public Barrage setTextColorResId(int textColorResId){
        mTextColorResId = textColorResId;
        return this;
    }

    public int getTextColorResId(){
        return mTextColorResId;
    }
}
