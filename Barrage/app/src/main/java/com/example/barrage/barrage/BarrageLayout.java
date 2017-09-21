package com.example.barrage.barrage;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by chasing on 2017/9/20.
 * 弹幕布局
 * 前提：弹幕数据以时间排序，由小到大
 */
public class BarrageLayout extends ViewGroup {
    private static final String LOG_INFO = "TEST_BARRAGE_LAYOUT";
    private static final int HANDLER_START_NEXT_ANIMATOR = 1;
    private static final int TEXT_VIEW_HEIGHT = 200;//每条弹幕高度
    private ArrayList<Barrage> mAllBarrages;//所有弹幕
    private int mRowCount = 3;//弹幕行数
    private long mTotalTime, mCurrentTime = 0;//单位：s
    private Timer mTimer;//当前时间计时器
    private TimerTask mDealCurrentTime;//当前时间计时任务
    private HashMap<Integer, Animator> mAllScrollAnim;//所有正在执行的动画
    private boolean isStart = false;
    private boolean mPause = true;//是否暂停
    private boolean mCancel = false;//是否取消滚动

    public BarrageLayout(Context context) {
        super(context);
        init();
    }

    public BarrageLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BarrageLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mAllBarrages = new ArrayList<>();
        mAllScrollAnim = new HashMap<>();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        for (int index = 0; index < getChildCount(); index++) {
            View v = getChildAt(index);
            //测量子View的宽和高
            measureChild(v, widthMeasureSpec, heightMeasureSpec);
        }
    }

    @Override
    protected void onLayout(boolean b, int i, int i1, int i2, int i3) {
        int rowCount = getRowCount();
        if (rowCount == 0) {
            throw new RuntimeException("BarrageLayout's Height is not enough height for barrage.Please reset BarrageLayout's Height.");
        }
        for (int index = 0; index < getChildCount(); index++) {
            View v = getChildAt(index);
            if (index % rowCount == 0) {
                v.layout(0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());
            } else if (index % rowCount == 1) {
                v.layout(0, v.getMeasuredHeight(), v.getMeasuredWidth(), v.getMeasuredHeight() * 2);
            } else {
                v.layout(0, v.getMeasuredHeight() * 2, v.getMeasuredWidth(), v.getMeasuredHeight() * 3);
            }
        }
    }

    /**
     * 根据高度获取能显示的弹幕行数
     */
    private int getRowCount() {
        if (mRowCount * TEXT_VIEW_HEIGHT > getMeasuredHeight()) {
            mRowCount--;
            getRowCount();
        }
        return mRowCount;
    }

    /**
     * 设置所有弹幕数据
     */
    public void setData(ArrayList<Barrage> data) {
        mAllBarrages.clear();
        mAllBarrages.addAll(data);
        initViews();
    }

    /**
     * 设置弹幕滚动的时长
     */
    public void setTotalTime(long totalTime) {
        mTotalTime = totalTime;
    }

    /**
     * 初始化每条弹幕
     */
    private void initViews() {
        int mTotalChildCount = mAllBarrages.size();
        for (int i = 0; i < mTotalChildCount; i++) {
            Barrage barrage = mAllBarrages.get(i);
            TextView textView = new TextView(getContext());
            LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, TEXT_VIEW_HEIGHT);
            textView.setLayoutParams(lp);
            textView.setGravity(Gravity.CENTER);
            String msg = barrage.getMsg();
            msg = '\t' + msg + '\t';
            textView.setText(msg);
            textView.setVisibility(View.GONE);
            textView.setTag(i);
            textView.setTextColor(ContextCompat.getColor(getContext(), barrage.getTextColorResId()));
            addView(textView);
        }
    }

    /**
     * 开始循环滚动mRowCount每一行的第一条弹幕
     */
    public void startScrollBarrage() {
        if (isStart) return;
        isStart = true;
        startFirstBarrage(0);

        mPause = false;
        if (mTimer == null) {
            mTimer = new Timer();
            mDealCurrentTime = new TimerTask() {
                @Override
                public void run() {
                    if (mPause) return;
                    mCurrentTime += 1;
                    if (mCurrentTime > mTotalTime) {
                        clear();
                    }
                }
            };
            mTimer.schedule(mDealCurrentTime, 1000, 1000);
        }
    }

    /**
     * 设置当前时间（进度条移动后重新设置当前时间）
     */
    public void setCurrentTime(long currentTime) {
        cancelAnim();
        mCurrentTime = currentTime;
        for (int i = 0; i < mAllBarrages.size(); i++) {
            if (mAllBarrages.get(i).getTime() >= currentTime) {
                startFirstBarrage(i);
                break;
            }
        }
    }

    /**
     * 使每一行的第一条弹幕开始进行滚动
     *
     * @param index 开始滚动的弹幕索引
     */
    private void startFirstBarrage(int index) {
        for (int i = index; i < index + mRowCount; i++) {
            if (getChildAt(i) != null) {
                Barrage barrage = mAllBarrages.get(i);
                long barrageTime = barrage.getTime();
                if (barrageTime >= mCurrentTime && barrageTime <= mCurrentTime + 5) {
                    setAnimationParent2Self(getChildAt(i));
                } else {
                    int duration = (int) (barrageTime - mCurrentTime) * 1000;//设置开始滚动的时间
                    sendAnimatorMessage(i, duration);
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void pause() {
        if (mPause) return;
        mPause = true;
        Collection<Animator> values = mAllScrollAnim.values();
        for (Animator anim : values) {
            anim.pause();
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void resume() {
        if (!mPause) return;
        mPause = false;
        Collection<Animator> values = mAllScrollAnim.values();
        for (Animator anim : values) {
            anim.resume();
        }
    }

    /**
     * 退出清除数据并销毁
     */
    public void destroy() {
        clear();
        mHandler = null;
    }

    /**
     * 取消滚动动画
     */
    private void cancelAnim() {
        mCancel = true;
        Collection<Animator> values = mAllScrollAnim.values();
        for (Animator anim : values) {
            anim.cancel();
        }
        mAllScrollAnim.clear();
        mHandler.removeMessages(HANDLER_START_NEXT_ANIMATOR);
        mCancel = false;
    }

    /**
     * 清楚计时器、滚动动画，清空数据
     */
    public void clear() {
        cancelAnim();
        mTimer.cancel();
        mTimer = null;
        mDealCurrentTime = null;
        mCurrentTime = 0;
        isStart = false;
    }

    /**
     * 接收延迟弹幕滚动，达到指定时间内显示滚动
     * 自定义弹幕显示时间段（当前时间-1 <= 弹幕时间 <= 当前时间+1）的时候进行显示
     */
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HANDLER_START_NEXT_ANIMATOR:
                    int nextIndex = msg.arg1;
                    View nextView = getChildAt(nextIndex);
                    if (nextView != null) {
                        long barrageTime = mAllBarrages.get(nextIndex).getTime();
                        if (barrageTime >= mCurrentTime - 1 && barrageTime <= mCurrentTime + 1) {//弹幕时间正好符合当前时间
                            if (mPause) {
                                sendAnimatorMessage(nextIndex, (int) (barrageTime - mCurrentTime));
                            } else {
                                setAnimationParent2Self(nextView);
                            }
                        } else if (barrageTime > mCurrentTime + 1) {//当前时间未到达弹幕时间
                            int duration = (int) (barrageTime - mCurrentTime) * 1000;
                            sendAnimatorMessage(nextIndex, duration);
                            Log.e(LOG_INFO, duration + " ");
                        } else if (barrageTime < mCurrentTime - 1) {//弹幕数量太多，导致弹幕时间到时当前时间已经远超过弹幕时间
                            Log.e(LOG_INFO, "Message is too mach to show.");
                            int doubleNextIndex = (int) nextView.getTag() + mRowCount;
                            int duration = getDuration(nextView.getMeasuredWidth());
                            sendAnimatorMessage(doubleNextIndex, duration);
                        }
                    }
                    break;
            }
        }
    };

    /**
     * 发送一条Message，通知下一条弹幕开始滚动的时间及索引
     *
     * @param index    弹幕的索引
     * @param duration 弹幕在duration ms后显示滚动
     */
    private void sendAnimatorMessage(int index, int duration) {
        if (mHandler == null) return;
        Message msg = mHandler.obtainMessage();
        msg.what = HANDLER_START_NEXT_ANIMATOR;
        msg.arg1 = index;//设置同一行的弹幕索引
        mHandler.sendMessageDelayed(msg, duration);
    }

    /**
     * 设置TextView（弹幕）的滚动动画
     */
    public void setAnimationParent2Self(final View view) {
        ObjectAnimator translateParent2Self = ObjectAnimator.ofFloat(view, "translationX",
                getMeasuredWidth(), -view.getMeasuredWidth());
        translateParent2Self.setDuration(getDuration(getMeasuredWidth() + view.getMeasuredWidth()));
        translateParent2Self.setRepeatCount(0);
        translateParent2Self.setInterpolator(new LinearInterpolator());
        translateParent2Self.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                if (mCancel) {
                    animator.cancel();
                } else {
                    view.setVisibility(View.VISIBLE);
                    int nextIndex = (int) view.getTag() + mRowCount;//设置同一行的弹幕索引
                    int duration = getDuration(view.getMeasuredWidth());//设置开始滚动的时间
                    sendAnimatorMessage(nextIndex, duration);
                    Log.e(LOG_INFO, duration + " start to next");
                    mAllScrollAnim.put((int) view.getTag(), animator);
                }
            }

            //anim.cancel()也会经过onAnimationEnd方法
            @Override
            public void onAnimationEnd(Animator animator) {
                view.setVisibility(View.GONE);
                int index = (int) view.getTag();
                if (!mCancel) {
                    mAllScrollAnim.remove(index);
                }
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        translateParent2Self.start();
    }

    /**
     * 根据长度获取滚动时间
     */
    private int getDuration(int width) {
        return width * 1000 / 250;  //转成毫秒
    }
}