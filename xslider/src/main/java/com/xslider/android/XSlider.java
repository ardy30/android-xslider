package com.xslider.android;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

/**
 * 滑动选择器控件。
 *
 * @author wuzhen
 * @since 2017/08/21
 */
public class XSlider extends View {

    private static final long FRAME_DURATION = 1000 / 60;

    private static final int THUMB_TYPE_OVAL = 0;
    private static final int THUMB_TYPE_RECTANGLE = 1;

    private static final int DEFAULT_COLOR = Color.BLACK;
    private static final int DEFAULT_ENLARGE_TOUCH_RANGE = 20;

    private Paint mPaint;
    private RectF mDrawRect;
    private RectF mTempRect;
    private Path mLeftTrackPath;
    private Path mRightTrackPath;

    private int mMinValue = 0;
    private int mMaxValue = 100;

    private int mThumbColor;
    private int mTrackColor;
    private int mProgressColor;

    private int mTrackSize;
    private float mThumbPosition = -1;

    private int mThumbType = THUMB_TYPE_OVAL;
    private int mThumbWidth;
    private int mThumbHeight;
    private int mThumbRadius;

    private int mTouchSlop;
    private int mEnlargeTouchRange;
    private boolean mIsRtl;
    private boolean mIsDragging;

    private PointF mMemoPoint;
    private Interpolator mInterpolator;
    private ThumbMoveAnimator mThumbMoveAnimator;

    private OnPositionChangeListener mOnPositionChangeListener;

    public XSlider(Context context) {
        super(context);
        init(context, null, 0, 0);
    }

    public XSlider(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0, 0);
    }

    public XSlider(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }

    public XSlider(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDrawRect = new RectF();
        mTempRect = new RectF();
        mLeftTrackPath = new Path();
        mRightTrackPath = new Path();
        mThumbMoveAnimator = new ThumbMoveAnimator();

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mMemoPoint = new PointF();

        applyStyle(getContext(), attrs, defStyleAttr, defStyleRes);
    }

    private void applyStyle(Context context, AttributeSet attrs, int defStyleAttr,
                            int defStyleRes) {

        TypedArray a = context
                .obtainStyledAttributes(attrs, R.styleable.XSlider, defStyleAttr, defStyleRes);
        setEnabled(a.getBoolean(R.styleable.XSlider_android_enabled, true));
        mProgressColor = a.getColor(R.styleable.XSlider_xslider_progressColor, DEFAULT_COLOR);
        mTrackColor = a.getColor(R.styleable.XSlider_xslider_trackColor, DEFAULT_COLOR);
        mThumbColor = a.getColor(R.styleable.XSlider_xslider_thumbColor, mProgressColor);
        mTrackSize = a.getDimensionPixelSize(R.styleable.XSlider_xslider_trackSize, dp2px(2));
        int value = a.getInteger(R.styleable.XSlider_xslider_value, 0);
        boolean valueDefined = a.hasValue(R.styleable.XSlider_xslider_value);
        int minValue = a.getInteger(R.styleable.XSlider_xslider_minValue, getMinValue());
        int maxValue = a.getInteger(R.styleable.XSlider_xslider_maxValue, getMaxValue());
        boolean valueRangeDefined = a.hasValue(R.styleable.XSlider_xslider_minValue)
                && a.hasValue(R.styleable.XSlider_xslider_maxValue);
        mThumbType = a.getInteger(R.styleable.XSlider_xslider_thumbType, THUMB_TYPE_OVAL);
        mThumbRadius = a.getDimensionPixelSize(R.styleable.XSlider_xslider_thumbRadius, dp2px(10));
        mThumbWidth = a.getDimensionPixelSize(R.styleable.XSlider_xslider_thumbWidth, dp2px(3));
        mThumbHeight = a.getDimensionPixelSize(R.styleable.XSlider_xslider_thumbHeight, dp2px(10));
        a.recycle();

        if (valueRangeDefined) {
            setValueRange(minValue, maxValue, false);
        }

        if (valueDefined) {
            setValue(value, false);
        } else if (mThumbPosition < 0) {
            setValue(mMinValue, false);
        }

        mEnlargeTouchRange = dp2px(DEFAULT_ENLARGE_TOUCH_RANGE);
        mInterpolator = new DecelerateInterpolator();

        invalidate();
    }

    /**
     * 获取选择器的最小值。
     *
     * @return 最小值
     */
    public int getMinValue() {
        return mMinValue;
    }

    /**
     * 获取选择器的最大值。
     *
     * @return 最大值
     */
    public int getMaxValue() {
        return mMaxValue;
    }

    /**
     * 设置可选值的范围。
     *
     * @param min       最小值
     * @param max       最大值
     * @param animation 范围改变是否有动画
     */
    public void setValueRange(int min, int max, boolean animation) {
        if (max < min || (min == mMinValue && max == mMaxValue)) {
            return;
        }

        float oldValue = getExactValue();
        float oldPosition = getPosition();
        mMinValue = min;
        mMaxValue = max;

        setValue(oldValue, animation);
        if (mOnPositionChangeListener != null && oldPosition == getPosition() &&
                oldValue != getExactValue()) {

            mOnPositionChangeListener
                    .onPositionChanged(this, false, oldPosition, oldPosition, Math.round(oldValue),
                            getValue());
        }
    }

    /**
     * 获取当前选择的值。
     *
     * @return 当前值
     */
    public int getValue() {
        return Math.round(getExactValue());
    }

    /**
     * 获取确切值。
     *
     * @return 确切值
     */
    public float getExactValue() {
        return (mMaxValue - mMinValue) * getPosition() + mMinValue;
    }

    /**
     * 获取当前滑动的位置的百分比。范围: [0..1]
     *
     * @return 当前滑动的位置的百分比
     */
    public float getPosition() {
        return mThumbMoveAnimator.isRunning() ? mThumbMoveAnimator.getPosition() : mThumbPosition;
    }

    /**
     * 设置当前滑动的位置。
     *
     * @param pos       当前位置的百分比。范围: [0..1]
     * @param animation 是否有动画
     */
    public void setPosition(float pos, boolean animation) {
        setPosition(pos, animation, false);
    }

    private void setPosition(float pos, boolean moveAnimation, boolean fromUser) {
        boolean change = getPosition() != pos;
        int oldValue = getValue();
        float oldPos = getPosition();

        if (!moveAnimation || !mThumbMoveAnimator.startAnimation(pos)) {
            mThumbPosition = pos;
        }

        int newValue = getValue();
        float newPos = getPosition();

        if (change && mOnPositionChangeListener != null) {
            mOnPositionChangeListener
                    .onPositionChanged(this, fromUser, oldPos, newPos, oldValue, newValue);
        }

        if (change) {
            invalidate();
        }
    }

    /**
     * 设置滑动进度条的颜色。
     *
     * @param color 颜色值
     */
    public void setProgressColor(int color) {
        mProgressColor = color;
        invalidate();
    }

    /**
     * 设置滑动条的颜色。
     *
     * @param color 颜色值
     */
    public void setTrackColor(int color) {
        mTrackColor = color;
        invalidate();
    }

    /**
     * 设置当前选中的值。
     *
     * @param value     值
     * @param animation 是否有切换动画
     */
    public void setValue(float value, boolean animation) {
        value = Math.min(mMaxValue, Math.max(value, mMinValue));
        setPosition((value - mMinValue) / (mMaxValue - mMinValue), animation);
    }

    /**
     * 设置滑动条位置改变的监听事件。
     *
     * @param listener 监听事件
     */
    public void setOnPositionChangeListener(OnPositionChangeListener listener) {
        this.mOnPositionChangeListener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);

        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        if (widthMode == MeasureSpec.UNSPECIFIED) {
            widthSize = getSuggestedMinimumWidth();
        } else if (widthMode == MeasureSpec.AT_MOST) {
            widthSize = Math.min(widthSize, getSuggestedMinimumWidth());
        }

        if (heightMode == MeasureSpec.UNSPECIFIED) {
            heightSize = getSuggestedMinimumHeight();
        } else if (heightMode == MeasureSpec.AT_MOST) {
            heightSize = Math.min(heightSize, getSuggestedMinimumHeight());
        }

        setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    public int getSuggestedMinimumWidth() {
        if (mThumbType == THUMB_TYPE_OVAL) {
            return mThumbRadius * 4 + getPaddingLeft() + getPaddingRight();
        } else {
            return mThumbWidth * 2 + getPaddingLeft() + getPaddingRight();
        }
    }

    @Override
    public int getSuggestedMinimumHeight() {
        final int height;
        if (mThumbType == THUMB_TYPE_OVAL) {
            height = mThumbRadius * 2 + getPaddingTop() + getPaddingBottom();
        } else {
            height = mThumbHeight + getPaddingTop() + getPaddingBottom();
        }
        return (height > mTrackSize ? height : mTrackSize);
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            boolean rtl = layoutDirection == LAYOUT_DIRECTION_RTL;
            if (mIsRtl != rtl) {
                mIsRtl = rtl;
                invalidate();
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        int offset = (mThumbType == THUMB_TYPE_OVAL ? mThumbRadius : mThumbWidth / 2);
        mDrawRect.left = getPaddingLeft() + offset;
        mDrawRect.right = w - getPaddingRight() - offset;

        int height = (mThumbType == THUMB_TYPE_OVAL ? mThumbRadius * 2 : mThumbHeight);
        mDrawRect.top = (h - height) / 2f;
        mDrawRect.bottom = mDrawRect.top + height;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }

        if (!isEnabled()) {
            return false;
        }

        float x = event.getX();
        float y = event.getY();
        if (mIsRtl) {
            x = 2 * mDrawRect.centerX() - x;
        }

        int i = event.getAction();
        if (i == MotionEvent.ACTION_DOWN) {
            mIsDragging = isThumbHit(x, y) && !mThumbMoveAnimator.isRunning();
            mMemoPoint.set(x, y);
        } else if (i == MotionEvent.ACTION_MOVE) {
            if (mIsDragging) {
                float offset = (x - mMemoPoint.x) / mDrawRect.width();
                float position = Math.min(1f, Math.max(0f, mThumbPosition + offset));
                setPosition(position, false, true);
                mMemoPoint.x = x;
                invalidate();
            }
        } else if (i == MotionEvent.ACTION_UP) {
            if (mIsDragging) {
                mIsDragging = false;
                setPosition(getPosition(), true, true);
            } else if (distance(mMemoPoint.x, mMemoPoint.y, x, y) <= mTouchSlop) {
                float position = Math.min(1f, Math.max(0f, (x - mDrawRect.left) / mDrawRect.width()));
                setPosition(position, true, true);
            }
        } else if (i == MotionEvent.ACTION_CANCEL) {
            if (mIsDragging) {
                mIsDragging = false;
                setPosition(getPosition(), true, true);
            }
        }
        return true;
    }

    private double distance(float x1, float y1, float x2, float y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    private int dp2px(float dpValue) {
        float value = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue,
                getContext().getResources().getDisplayMetrics());
        return (int) (value + 0.5f);
    }

    private boolean isThumbHit(float x, float y) {
        float cx = mDrawRect.width() * mThumbPosition + mDrawRect.left;
        float cy = mDrawRect.centerY();

        if (mThumbType == THUMB_TYPE_OVAL) {
            float max = Math.max(mEnlargeTouchRange, Math.max(mThumbRadius, getHeight() / 2));
            return x >= cx - max && x <= cx + max && y >= cy - max && y <= cy + max;
        } else {
            int max = Math.max(mEnlargeTouchRange, Math.max(mThumbWidth, mThumbHeight));
            return x >= cx - max && x <= cx + max && y >= cy - max && y < cy + max;
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        float x = mDrawRect.width() * mThumbPosition + mDrawRect.left;
        if (mIsRtl) {
            x = 2 * mDrawRect.centerX() - x;
        }
        float y = mDrawRect.centerY();
        int filledPrimaryColor = isEnabled() ? mProgressColor : mTrackColor;

        float trackRadius = (mThumbType == THUMB_TYPE_OVAL ? mThumbRadius : mThumbWidth / 2.f);
        getTrackPath(x, y, trackRadius);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mIsRtl ? filledPrimaryColor : mTrackColor);
        canvas.drawPath(mRightTrackPath, mPaint);
        mPaint.setColor(mIsRtl ? mTrackColor : filledPrimaryColor);
        canvas.drawPath(mLeftTrackPath, mPaint);

        int thumbColor = isEnabled() ? mThumbColor : mTrackColor;
        mPaint.setColor(thumbColor);
        mPaint.setStyle(Paint.Style.FILL);

        if (mThumbType == THUMB_TYPE_OVAL) {
            float radius = isEnabled() ? mThumbRadius : mThumbRadius;
            mPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(x, y, radius, mPaint);
        } else if (mThumbType == THUMB_TYPE_RECTANGLE) {
            float halfOfWidth = (isEnabled() ? mThumbWidth : mThumbWidth * 2) /
                    2.f;
            float halfOfHeight =
                    (isEnabled() ? mThumbHeight : mThumbHeight * 2) / 2.f;

            float l = x - halfOfWidth;
            float t = y - halfOfHeight;
            float r = x + halfOfWidth;
            float b = y + halfOfHeight;

            canvas.drawRoundRect(new RectF(l, t, r, b), mThumbRadius, mThumbRadius, mPaint);
        }
    }

    private void getTrackPath(float x, float y, float radius) {
        float halfStroke = mTrackSize / 2f;

        mLeftTrackPath.reset();
        mRightTrackPath.reset();

        final float left = (mThumbType == THUMB_TYPE_OVAL ? mDrawRect.left : 0);
        final float right = (mThumbType == THUMB_TYPE_OVAL ? mDrawRect.right : getWidth());
        if (radius - 1f < halfStroke) {
            if (x > left) {
                mLeftTrackPath.moveTo(left, y - halfStroke);
                mLeftTrackPath.lineTo(x, y - halfStroke);
                mLeftTrackPath.lineTo(x, y + halfStroke);
                mLeftTrackPath.lineTo(left, y + halfStroke);
                mLeftTrackPath.close();
            }

            if (x < right) {
                mRightTrackPath.moveTo(right, y + halfStroke);
                mRightTrackPath.lineTo(x, y + halfStroke);
                mRightTrackPath.lineTo(x, y - halfStroke);
                mRightTrackPath.lineTo(right, y - halfStroke);
                mRightTrackPath.close();
            }
        } else {
            mTempRect.set(x - radius + 1f, y - radius + 1f, x + radius - 1f, y + radius - 1f);
            float angle = (float) (Math.asin(halfStroke / (radius - 1f)) / Math.PI * 180);

            if (x - radius > left) {
                mLeftTrackPath.moveTo(left, y - halfStroke);
                mLeftTrackPath.arcTo(mTempRect, 180 + angle, -angle * 2);
                mLeftTrackPath.lineTo(left, y + halfStroke);
                mLeftTrackPath.close();
            }

            if (x + radius < right) {
                mRightTrackPath.moveTo(right, y - halfStroke);
                mRightTrackPath.arcTo(mTempRect, -angle, angle * 2);
                mRightTrackPath.lineTo(right, y + halfStroke);
                mRightTrackPath.close();
            }
        }
    }

    private class ThumbMoveAnimator implements Runnable {

        boolean mRunning = false;
        long mStartTime;
        float mStartFillPercent;
        float mStartPosition;
        float mPosition;
        float mFillPercent;
        int mDuration;

        boolean isRunning() {
            return mRunning;
        }

        float getPosition() {
            return mPosition;
        }

        void resetAnimation() {
            mStartTime = SystemClock.uptimeMillis();
            mStartPosition = mThumbPosition;
            mStartFillPercent = 1;
            mFillPercent = mPosition == 0 ? 0 : 1;
            mDuration = getResources().getInteger(android.R.integer.config_mediumAnimTime);
        }

        boolean startAnimation(float position) {
            if (mThumbPosition == position) {
                return false;
            }

            mPosition = position;

            if (getHandler() != null) {
                resetAnimation();
                mRunning = true;
                getHandler().postAtTime(this, SystemClock.uptimeMillis() + FRAME_DURATION);
                invalidate();
                return true;
            } else {
                mThumbPosition = position;
                invalidate();
                return false;
            }
        }

        void stopAnimation() {
            mRunning = false;
            mThumbPosition = mPosition;
            if (getHandler() != null) {
                getHandler().removeCallbacks(this);
            }
            invalidate();
        }

        @Override
        public void run() {
            long curTime = SystemClock.uptimeMillis();
            float progress = Math.min(1f, (float) (curTime - mStartTime) / mDuration);
            float value = mInterpolator.getInterpolation(progress);

            mThumbPosition = (mPosition - mStartPosition) * value + mStartPosition;

            if (progress == 1f) {
                stopAnimation();
            }

            if (mRunning) {
                if (getHandler() != null) {
                    getHandler().postAtTime(this, SystemClock.uptimeMillis() + FRAME_DURATION);
                } else {
                    stopAnimation();
                }
            }
            invalidate();
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.position = getPosition();
        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;

        super.onRestoreInstanceState(ss.getSuperState());
        setPosition(ss.position, false);
        requestLayout();
    }

    private static class SavedState extends BaseSavedState {

        float position;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            position = in.readFloat();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeFloat(position);
        }

        @Override
        public String toString() {
            return "Slider.SavedState{" + Integer.toHexString(System.identityHashCode(this)) +
                    " pos=" + position + "}";
        }

        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {

            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
