package com.example.barrage.barrage;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

import com.example.barrage.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

/**
 * Created by chasing on 2017/9/20.
 * 弹幕布局
 * 前提：弹幕数据以时间排序，由小到大
 */
public class BarrageLayout extends ViewGroup {
    private static final String LOG_INFO = "TEST_BARRAGE_LAYOUT";
    private static final int HANDLER_START_NEXT_ANIMATOR = 1;
    private static final int HANDLER_CLEAR_ANIMATOR = 2;
    public static final int HANDLER_START_ANIMATOR = 3;

    private float mTextViewHeight = 200;//每条弹幕高度，单位：px
    private float mTextSize = 15;//每条弹幕字体大小，单位：px

    private ArrayList<Barrage> mAllBarrages;//所有弹幕
    private int mRowCount;//弹幕行数
    private long mTotalTime, mCurrentTime = 0;//时间单位：s
    private Timer mTimer;//当前时间计时器
    private TimerTask mDealCurrentTime;//当前时间计时任务
    private HashMap<Integer, Animator> mAllScrollAnim;//所有正在执行的动画
    private boolean isOpen = true;//是否开启弹幕
    private boolean isStart = false;//是否已经开始播放弹幕
    private boolean mPause = true;//是否暂停
    private boolean mCancel = false;//是否取消滚动
    private SparseBooleanArray mRowIsShowing;//记录每一行是否有弹幕刚刚出现（即正在显示但刚开始显示，未显示完整的）
    private TreeMap<Integer, Barrage> mAddedBarrage;//用户刚刚评论添加的弹幕
    private SparseIntArray mInterruptedBarrage;//由于新添加的弹幕中断了原来的顺序，记录中断位置
    private SparseIntArray mRowShowingIndex;//记录每一行正在显示的弹幕的索引

    private boolean isMeasure;
    private boolean isLayout;

    private int mWidthMeasureSpec, mHeightMeasureSpec;
    private PauseThread mPauseThread;

    public BarrageLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BarrageLayout);
        mTextViewHeight = a.getDimension(R.styleable.BarrageLayout_lineHeight, 200);
        mTextSize = a.getDimensionPixelSize(R.styleable.BarrageLayout_textSize, (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 12, getResources().getDisplayMetrics()));
        a.recycle();

        init();
    }

    private void init() {
        mAllBarrages = new ArrayList<>();
        mAllScrollAnim = new HashMap<>();
        mAddedBarrage = new TreeMap<>();
        mInterruptedBarrage = new SparseIntArray();
        mRowIsShowing = new SparseBooleanArray();
        mRowShowingIndex = new SparseIntArray();
        mPauseThread = new PauseThread(mHandler);
        mPauseThread.start();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (!isMeasure) { //不需要一直对子View进行重测
            isMeasure = true;
            mWidthMeasureSpec = widthMeasureSpec;
            mHeightMeasureSpec = heightMeasureSpec;
            for (int index = 0; index < getChildCount(); index++) {
                View v = getChildAt(index);
                //测量子View的宽和高
                measureChild(v, widthMeasureSpec, heightMeasureSpec);
            }
        }
    }

    @Override
    protected void onLayout(boolean b, int i, int i1, int i2, int i3) {
        if (!isLayout) {
            isLayout = true;
            mRowCount = (int) (getMeasuredHeight() / mTextViewHeight);
            for (int row = 0; row < mRowCount; row++) {
                mRowShowingIndex.put(row, -1);
            }
        }
        if (mRowCount == 0) {
            throw new RuntimeException("BarrageLayout's Height is not enough height for barrage.Please reset BarrageLayout's Height.");
        }
        for (int index = 0; index < getChildCount(); index++) {
            View v = getChildAt(index);
            v.layout(0, v.getMeasuredHeight() * (index % mRowCount),
                    v.getMeasuredWidth(), v.getMeasuredHeight() * ((index % mRowCount) + 1));
        }
    }

    /**
     * 是否开启弹幕
     */
    public void setOpen(boolean isOpen) {
        this.isOpen = isOpen;
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
     * 添加单条弹幕
     */
    public void addBarrage(Barrage barrage) {
        TextView textView = addBarrageView(barrage, mAllBarrages.size());
        measureChild(textView, mWidthMeasureSpec, mHeightMeasureSpec);//测量宽高
        mAddedBarrage.put(mAllBarrages.size(), barrage);
        mAllBarrages.add(barrage);
        int thisIndex = mAllBarrages.size() - 1;
        if (!mRowIsShowing.get(thisIndex % mRowCount)) {
            //如果没有同行没有数据在显示，则发送信息通知对添加的弹幕进行显示
            sendAnimatorMessage(thisIndex, 0);
        }
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
        int totalCount = mAllBarrages.size();
        for (int i = 0; i < totalCount; i++) {
            Barrage barrage = mAllBarrages.get(i);
            addBarrageView(barrage, i);
        }
    }

    /**
     * 添加单条弹幕view
     *
     * @param tag 为弹幕添加索引标记
     */
    private TextView addBarrageView(Barrage barrage, int tag) {
        TextView textView = new TextView(getContext());
        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, (int) mTextViewHeight);
        textView.setLayoutParams(lp);
        textView.setGravity(Gravity.CENTER);
        String msg = barrage.getMsg();
        msg = '\t' + msg + '\t';
        textView.setText(msg);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX,mTextSize);
        textView.setTag(tag);
        textView.setTextColor(ContextCompat.getColor(getContext(), barrage.getTextColorResId()));
        textView.setVisibility(View.GONE);
        addView(textView);
        return textView;
    }

    /**
     * 开始循环滚动mRowCount每一行的第一条弹幕
     */
    public void startScrollBarrage() {
        if (isStart) return;
        isStart = true;
        startFirstBarrage(0);

        mPause = false;
        mPauseThread.setPause(false);
        if (mTimer == null) {
            mTimer = new Timer();
            mDealCurrentTime = new TimerTask() {
                @Override
                public void run() {
                    //计时
                    if (mPause) return;
                    mCurrentTime += 1;
                    if (mCurrentTime > mTotalTime) {
                        mHandler.sendEmptyMessage(HANDLER_CLEAR_ANIMATOR);//timer线程不能更新UI，通过handler调用
                    }
                }
            };
            mTimer.schedule(mDealCurrentTime, 1000, 1000);
        }
    }

    /**
     * 设置当前时间（进度条移动后重新设置当前时间）
     * 取消正在显示的弹幕，重新按照当前时间显示每一行的第一条弹幕
     */
    public void setCurrentTime(long currentTime) {
        mPause = false;
        mPauseThread.setPause(false);
        cancelAnim();
        mCurrentTime = currentTime;
        for (int i = 0; i < mAllBarrages.size(); i++) {
            if (mAllBarrages.get(i).getTime() >= currentTime) {
                startFirstBarrage(i);
                break;
            }
        }
    }

    public long getCurrentTime() {
        return mCurrentTime;
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
                if (barrageTime >= mCurrentTime - 1 && barrageTime <= mCurrentTime + 1) {
//                    setAnimationParent2Self(i);
                    mPauseThread.push(i);
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
        mPauseThread.setPause(true);
        Collection<Animator> values = mAllScrollAnim.values();
        for (Animator anim : values) {
            anim.pause();
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void resume() {
        if (!mPause) return;
        mPause = false;
        mPauseThread.setPause(false);
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
        mPauseThread.setDestroy(true);
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
        mRowIsShowing.clear();
        mAddedBarrage.clear();
        mInterruptedBarrage.clear();
        mRowShowingIndex.clear();
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

    private final Object clock = new Object();
    /**
     * 接收延迟弹幕滚动，达到指定时间内显示滚动
     * 自定义弹幕显示时间段（当前时间-1 <= 弹幕时间 <= 当前时间+1）的时候进行显示
     */
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HANDLER_START_NEXT_ANIMATOR://通知下一条弹幕进行滚动
                    synchronized (clock) {
                        int rowPoi = (int) msg.obj;
                        removeMessages(HANDLER_START_NEXT_ANIMATOR, rowPoi);//移除同一行的其它弹幕
                        int nextIndex = msg.arg1;
                        mRowIsShowing.put(nextIndex % mRowCount, false);

                        if (!mAddedBarrage.isEmpty()) {
                            boolean hadAdded = false;
                            //当有用户新添加的评论时，显示刚刚添加的弹幕
                            for (Map.Entry<Integer, Barrage> entry : mAddedBarrage.entrySet()) {
                                int index = entry.getKey();
                                if (index % mRowCount == nextIndex % mRowCount) {
                                    int trueNextIndex = mRowShowingIndex.get(nextIndex % mRowCount);
                                    if (trueNextIndex != -1) {//添加弹幕之前该行已经有弹幕滚动过，则中断位置为上次的弹幕索引
                                        trueNextIndex += mRowCount;
                                        //如果中断的index和新添加的index一样，说明是连续添加了mRowCount+1次弹幕，则不进行中断标记
                                        //如果小于当前时间，证明添加新弹幕的时候并没有弹幕在显示，则不进行中断标记
                                        if (trueNextIndex < mAllBarrages.size() && trueNextIndex != index) {
                                            mInterruptedBarrage.put(nextIndex % mRowCount, trueNextIndex);//添加原本的弹幕为中断弹幕，以进行恢复
                                        }
                                    } else {//添加弹幕之前该行没有弹幕滚动过，则中断位置为每行的首个索引
                                        mInterruptedBarrage.put(nextIndex % mRowCount, index % mRowCount);//添加原本的弹幕为中断弹幕，以进行恢复
                                    }
                                    mAddedBarrage.remove(index);
                                    mPauseThread.push(index);
                                    hadAdded = true;
                                    break;
                                }
                            }
                            if (hadAdded) break;
                        }

                        View nextView = getChildAt(nextIndex);
                        if (nextView != null) {
                            long barrageTime = mAllBarrages.get(nextIndex).getTime();
                            if (barrageTime >= mCurrentTime - 1 && barrageTime <= mCurrentTime + 1) {
                                //弹幕时间正好符合当前时间
                                mPauseThread.push(nextIndex);
                            } else if (barrageTime > mCurrentTime + 1) {
                                //当前时间未到达弹幕时间
                                int duration = (int) (barrageTime - mCurrentTime) * 1000;
                                sendAnimatorMessage(nextIndex, duration);
//                            Log.e(LOG_INFO, duration + " ");
                            } else if (barrageTime < mCurrentTime - 1) {
                                //弹幕数量太多，导致弹幕时间到时当前时间已经远超过弹幕时间，则立即显示同行后面一条
                                Log.e(LOG_INFO, "Message's time is out of date.");
                                int doubleNextIndex = (int) nextView.getTag() + mRowCount;
                                sendAnimatorMessage(doubleNextIndex, 0);
                            }
                        }
                    }
                    break;

                case HANDLER_CLEAR_ANIMATOR://清楚弹幕
                    clear();
                    break;

                case HANDLER_START_ANIMATOR://开始下一条弹幕的滚动
                    int index = msg.arg1;
                    setAnimationParent2Self(index);
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
        msg.obj = index % mRowCount;//设置行位置
        msg.arg1 = index;//设置同一行的弹幕索引
        mHandler.sendMessageDelayed(msg, duration);
    }

    /**
     * 设置TextView（弹幕）的滚动动画
     */
    public void setAnimationParent2Self(final int index) {
        if (mPause) {
            //如果处于暂停状态则重新放入分配线程
            mPauseThread.push(index);
            return;
        }

        //判断上一个显示的弹幕是否已经显示完整
        int showingIndex = mRowShowingIndex.get(index % mRowCount);
        View showingView = getChildAt(showingIndex);
        if (showingView != null) {
            float exceedWidth = showingView.getMeasuredWidth() + showingView.getTranslationX() - getMeasuredWidth();
            if (exceedWidth > 0) {//上一条弹幕未显示完整，还有exceedWidth的宽度未显示
                sendAnimatorMessage(index, getDuration(index, (int) exceedWidth));
                return;
            }
        }

        /*
        当pause的时候，handler.sendMessageDelay的延迟时间还是会继续计算
        所以当点击resume的时候，delay的弹幕就会立即显示出来
        所以添加以下判断：
         */
        long barrageTime = mAllBarrages.get(index).getTime();
        if (barrageTime >= mCurrentTime + 1) {
            //当前时间未到达弹幕时间
            int duration = (int) (barrageTime - mCurrentTime) * 1000;
            sendAnimatorMessage(index, duration);
            return;
        }

        final View view = getChildAt(index);
        ObjectAnimator translateParent2Self = ObjectAnimator.ofFloat(view, "translationX",
                getMeasuredWidth(), -view.getMeasuredWidth());
        translateParent2Self.setDuration(getDuration(index, getMeasuredWidth() + view.getMeasuredWidth()));
        translateParent2Self.setRepeatCount(0);
        translateParent2Self.setInterpolator(new LinearInterpolator());
        translateParent2Self.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                if (mCancel) {
                    animator.cancel();
                } else {
                    if (isOpen) {
                        view.setVisibility(View.VISIBLE);
                    }
                    //动画开始（即滚动开始）的时候，计算自身需要duration时长才能显示完整，则延迟duration时长后显示下一条弹幕
                    int curIndex = (int) view.getTag();
//                    Log.e(LOG_INFO, curIndex + "    cur");
                    mAllScrollAnim.put(curIndex, animator);
                    mRowIsShowing.put(curIndex % mRowCount, true);
                    int nextIndex = curIndex + mRowCount;//设置同一行的弹幕索引
                    int duration = getDuration(index, view.getMeasuredWidth());//设置开始滚动的时间

                    //显示新添加的弹幕时不更新正在显示的索引，保持被中断时的索引，方便之后的弹幕重新从中断位置开始
                    if (mInterruptedBarrage.size() != 0) {
                        int interruptedIndex = mInterruptedBarrage.get(curIndex % mRowCount, -1);
                        if (interruptedIndex != -1) {
                            //如果同行被中断过，则返回中断点继续进行滚动
                            mInterruptedBarrage.delete(curIndex % mRowCount);
                            if (interruptedIndex != curIndex) {
                                nextIndex = interruptedIndex;
                            }
                        } else {
                            mRowShowingIndex.put(curIndex % mRowCount, curIndex);
                        }
                    } else {
                        mRowShowingIndex.put(curIndex % mRowCount, curIndex);
                    }
//                    Log.e(LOG_INFO, nextIndex + "    next");

                    sendAnimatorMessage(nextIndex, duration);
//                    Log.e(LOG_INFO, duration + " start to next");
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

    private int[] mSpeedArray;

    /**
     * 根据行数索引及长度获取滚动时间
     * 每一行的速度一致，避免追尾
     */
    private int getDuration(int index, int width) {
        if (mSpeedArray == null) {
            mSpeedArray = new int[mRowCount];
            Random random = new Random(48);
            for (int i = 0; i < mRowCount; i++) {
                mSpeedArray[i] = random.nextInt(200) + 250;
            }
        }
        int speed = mSpeedArray[index % mRowCount];
        return width * 1000 / speed;  //转成毫秒
    }
}