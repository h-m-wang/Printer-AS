package com.industry.printer.ui.Test;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.industry.printer.R;
import com.industry.printer.Utils.Debug;
import com.industry.printer.hardware.IInkDevice;
import com.industry.printer.hardware.InkManagerFactory;
import com.industry.printer.hardware.SmartCardManager;

/*
  连供的阀开关测试
 */
public class TestValveOnOff implements ITestOperation {
    public static final String TAG = TestValveOnOff.class.getSimpleName();

    private Context mContext = null;
    private FrameLayout mContainer = null;
    private LinearLayout mTestAreaLL = null;

    private int mSubIndex = 0;
    private IInkDevice mSCManager;

    private final String TITLE = "SmartCard Valve On/Off Test";

    public TestValveOnOff(Context ctx, int index) {
        mContext = ctx;
        mSubIndex = index;
        mSCManager = InkManagerFactory.inkManager(mContext);
    }

    @Override
    public void show(FrameLayout f) {
        mContainer = f;

        mTestAreaLL = (LinearLayout)LayoutInflater.from(mContext).inflate(R.layout.test_valve_onoff, null);

        TextView scValve1 = (TextView)mTestAreaLL.findViewById(R.id.sc_valve1);
        scValve1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mSCManager instanceof SmartCardManager) {
                    ((SmartCardManager)mSCManager).addInkOn(0);
                    try {Thread.sleep(100);} catch (Exception e) {}
                    ((SmartCardManager)mSCManager).addInkOff(0);
                }
            }
        });
        TextView scValve2 = (TextView)mTestAreaLL.findViewById(R.id.sc_valve2);
        scValve2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mSCManager instanceof SmartCardManager) {
                    ((SmartCardManager)mSCManager).addInkOn(1);
                    try {Thread.sleep(100);} catch (Exception e) {}
                    ((SmartCardManager)mSCManager).addInkOff(1);
                }
            }
        });

        mContainer.addView(mTestAreaLL);
    }

    @Override
    public void setTitle(TextView tv) {
        tv.setText(TITLE);
    }

    @Override
    public boolean quit() {
        mContainer.removeView(mTestAreaLL);
        return true;
    }
}
