package com.xslider.android.samples;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.xslider.android.OnPositionChangeListener;
import com.xslider.android.XSlider;

public class MainActivity extends AppCompatActivity
        implements OnPositionChangeListener,
        View.OnClickListener {

    TextView tv1, tv2, tv3;
    XSlider slider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv1 = (TextView) findViewById(R.id.tv1);
        tv2 = (TextView) findViewById(R.id.tv2);
        tv3 = (TextView) findViewById(R.id.tv3);

        slider = (XSlider) findViewById(R.id.slider);
        slider.setOnPositionChangeListener(this);

        findViewById(R.id.btn1).setOnClickListener(this);
        findViewById(R.id.btn2).setOnClickListener(this);

        tv2.setText(String.valueOf(slider.getMinValue()));
        tv3.setText(String.valueOf(slider.getMaxValue()));
    }

    @Override
    public void onPositionChanged(XSlider view, boolean fromUser, float oldPos, float newPos, int oldValue, int newValue) {
        tv1.setText("当前值: " + view.getValue() + ", 确切值: " + view.getExactValue());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn1:
                slider.setValue(60, true);
                break;

            case R.id.btn2:
                slider.setValueRange(10, 80, true);

                tv2.setText(String.valueOf(slider.getMinValue()));
                tv3.setText(String.valueOf(slider.getMaxValue()));
                break;

            default:
                break;
        }
    }
}
