package com.industry.printer.ui.Test;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.industry.printer.R;
import com.industry.printer.Utils.Debug;
import com.industry.printer.hardware.ExtGpio;
import com.industry.printer.hardware.SmartCardManager;

public class TestBaginkValveOnOff implements ITestOperation {
    public static final String TAG = TestBaginkValveOnOff.class.getSimpleName();

    private Context mContext = null;
    private FrameLayout mContainer = null;
    private LinearLayout mTestAreaLL = null;

    private int mSubIndex = 0;

    private final String TITLE = "Bagink Valve On/Off Test";

    private final static int[] sc_valve_list = {
            R.id.sc_valve1, R.id.sc_valve2, R.id.sc_valve3, R.id.sc_valve4,
            R.id.sc_valve5, R.id.sc_valve6, R.id.sc_valve7, R.id.sc_valve8,
            R.id.sc_valve9, R.id.sc_valve10, R.id.sc_valve11, R.id.sc_valve12,
            R.id.sc_valve13, R.id.sc_valve14, R.id.sc_valve15, R.id.sc_valve16};
    private TextView[] mSCValves;
    private int mCurrentValve;
    private TextView mPI9, mPE6, mPE5, mPE4, mPG3;

    public TestBaginkValveOnOff(Context ctx, int index) {
        mContext = ctx;
        mSubIndex = index;
        mSCValves = new TextView[sc_valve_list.length];
        mCurrentValve = -1;
    }

    @Override
    public void show(FrameLayout f) {
        mContainer = f;

        mTestAreaLL = (LinearLayout) LayoutInflater.from(mContext).inflate(R.layout.test_bagink_valve_onoff, null);

        for(int i=0; i<mSCValves.length; i++) {
            mSCValves[i] = (TextView)mTestAreaLL.findViewById(sc_valve_list[i]);
            mSCValves[i].setTag(i);
            mSCValves[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(mCurrentValve >= 0 && mCurrentValve < mSCValves.length) {
                        ExtGpio.setValve(mCurrentValve, 0);
                        mSCValves[mCurrentValve].setBackgroundColor(Color.GRAY);
                    }
                    if(mCurrentValve != (Integer) (view.getTag())) {
                        mCurrentValve = (Integer) (view.getTag());
                        mSCValves[mCurrentValve].setBackgroundColor(Color.GREEN);
                        ExtGpio.setValve(mCurrentValve, 1);
                        Debug.d(TAG, "mCurrentValve = " + mCurrentValve);
                    } else {
                        mCurrentValve = -1;
                    }
                    refreshPinLeds();
                }
            });
        }

        mPI9 = (TextView)mTestAreaLL.findViewById(R.id.pi9);
        mPE6 = (TextView)mTestAreaLL.findViewById(R.id.pe6);
        mPE5 = (TextView)mTestAreaLL.findViewById(R.id.pe5);
        mPE4 = (TextView)mTestAreaLL.findViewById(R.id.pe4);
        mPG3 = (TextView)mTestAreaLL.findViewById(R.id.pg3);
        refreshPinLeds();

        mContainer.addView(mTestAreaLL);
    }

    @Override
    public void setTitle(TextView tv) {
        tv.setText(TITLE);
    }

    @Override
    public boolean quit() {
        ExtGpio.setValve(0, 0);
        mContainer.removeView(mTestAreaLL);
        return true;
    }

    private void refreshPinLeds() {
        if(ExtGpio.readGpioTestPin('I', 9) == 0) mPI9.setBackgroundColor(Color.GRAY); else mPI9.setBackgroundColor(Color.GREEN);
        if(ExtGpio.readGpioTestPin('E', 6) == 0) mPE6.setBackgroundColor(Color.GRAY); else mPE6.setBackgroundColor(Color.GREEN);
        if(ExtGpio.readGpioTestPin('E', 5) == 0) mPE5.setBackgroundColor(Color.GRAY); else mPE5.setBackgroundColor(Color.GREEN);
        if(ExtGpio.readGpioTestPin('E', 4) == 0) mPE4.setBackgroundColor(Color.GRAY); else mPE4.setBackgroundColor(Color.GREEN);
        if(ExtGpio.readGpioTestPin('G', 3) == 0) mPG3.setBackgroundColor(Color.GRAY); else mPG3.setBackgroundColor(Color.GREEN);
    }
}
