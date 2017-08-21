package com.xslider.android;

/**
 * 滑动条选中位置改变时的监听事件。
 *
 * @author wuzhen
 * @since 2017/08/21
 */
public interface OnPositionChangeListener {

    /**
     * 滑动条的位置改变。
     *
     * @param view     Slider
     * @param fromUser 是否用户改变的
     * @param oldPos   原来的位置
     * @param newPos   新的位置
     * @param oldValue 原来的值
     * @param newValue 新的值
     */
    void onPositionChanged(XSlider view, boolean fromUser, float oldPos, float newPos,
                           int oldValue, int newValue);
}
