package com.example.barrage.barrage;

import android.os.Handler;
import android.os.Message;

import java.util.concurrent.PriorityBlockingQueue;

/**
 * Created by chasing on 2017/9/22.
 * 分配线程，控制暂停及销毁
 */
public class PauseThread extends Thread{
    private Handler mMainThreadHandler;
    private boolean isDestroy = false;
    private boolean isPause = false;
    private PriorityBlockingQueue<Integer> mQueue;

    public PauseThread(Handler handler){
        mMainThreadHandler = handler;
        mQueue = new PriorityBlockingQueue<>();
    }

    public void setDestroy(boolean isDestroy){
        this.isDestroy = isDestroy;
    }

    public void setPause(boolean isPause){
        this.isPause = isPause;
    }

    public void push(int index){
        mQueue.put(index);
    }

    /**
     * 移除显示index的弹幕
     * 调用情况：
     *      添加新的弹幕的时候，需要移除即将显示的弹幕，优先显示新添加的弹幕
     * 可能造成：
     *      被延迟的弹幕可能超出时间没办法显示（小于当前时间-1s）
     *
     * @param index 将要显示的弹幕的索引
     */
    public void remove(int index){
        mQueue.remove(index);
    }

    @Override
    public void run() {
        while (!isDestroy){
            if (!isPause){
                try {
                    int index = mQueue.take();//没有数据可以取出时会阻塞直到有数据
                    Message msg = mMainThreadHandler.obtainMessage();
                    msg.what = BarrageLayout.HANDLER_START_ANIMATOR;
                    msg.arg1 = index;
                    if (isPause){
                        push(index);
                    } else {
                        mMainThreadHandler.sendMessage(msg);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}