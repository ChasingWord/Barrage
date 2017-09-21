package com.example.barrage.barrage;

import com.example.barrage.R;

/**
 * Created by chasing on 2017/9/20.
 */

public class Barrage {
    private long mTime;
    private String mMsg;
    private int mTextColorResId;

    public Barrage(){
        mTextColorResId = android.R.color.black;
    }

    public long getTime(){
        return mTime;
    }

    public String getMsg(){
        return mMsg;
    }

    public void setTime(long time){
        mTime = time;
    }

    public void setMsg(String msg){
        mMsg = msg;
    }

    public void setTextColorResId(int textColorResId){
        mTextColorResId = textColorResId;
    }

    public int getTextColorResId(){
        return mTextColorResId;
    }
}
