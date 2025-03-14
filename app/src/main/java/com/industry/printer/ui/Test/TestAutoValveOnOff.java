package com.industry.printer.ui.Test;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Message;
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
import com.industry.printer.Utils.ToastUtil;
import com.industry.printer.hardware.ExtGpio;
import com.industry.printer.hardware.IInkDevice;
import com.industry.printer.hardware.InkManagerFactory;
import com.industry.printer.hardware.SmartCardManager;

/*
  连供的阀开关测试
 */
public class TestAutoValveOnOff implements ITestOperation {
    public static final String TAG = TestValveOnOff.class.getSimpleName();

    private Context mContext = null;
    private FrameLayout mContainer = null;
    private LinearLayout mTestAreaLL = null;
    private TextView mValve1 = null;
    private TextView mValve2 = null;

    private int mSubIndex = 0;
    private IInkDevice mSCManager;

    private final String TITLE = "Auto SmartCard Valve On/Off Test";

    private final static int MESSAGE_VALVE_ON = 100;
    private final static int MESSAGE_VALVE_OFF = 101;

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_VALVE_ON:
                    if(mSCManager instanceof SmartCardManager) {
                        ((SmartCardManager)mSCManager).addInkOn(0);
                        ((SmartCardManager)mSCManager).addInkOn(1);
                        mValve2.setBackgroundColor(Color.GREEN);

                        if(ExtGpio.readGpioTestPin('E', 5) == 1) {
                            mValve1.setBackgroundColor(Color.GREEN);
                            mValve1.setText("Valve1(ON); PE5(1)");
                        } else {
                            mValve1.setBackgroundColor(Color.RED);
                            mValve1.setText("Valve1(ON); PE5(0)");
                        }
                        if(ExtGpio.readGpioTestPin('E', 4) == 1) {
                            mValve2.setBackgroundColor(Color.GREEN);
                            mValve2.setText("Valve2(ON); PE4(1)");
                        } else {
                            mValve2.setBackgroundColor(Color.RED);
                            mValve2.setText("Valve2(ON); PE4(0)");
                        }
                        mHandler.sendEmptyMessageDelayed(MESSAGE_VALVE_OFF, 1000);
                    }
                    break;
                case MESSAGE_VALVE_OFF:
                    if(mSCManager instanceof SmartCardManager) {
                        ((SmartCardManager)mSCManager).addInkOff(0);
                        ((SmartCardManager)mSCManager).addInkOff(1);
                        mValve1.setBackgroundColor(Color.GRAY);
                        mValve2.setBackgroundColor(Color.GRAY);
                        mValve1.setText("Valve1(OFF); PE5(" + ExtGpio.readGpioTestPin('E', 5) + ")");
                        mValve2.setText("Valve2(OFF); PE4(" + ExtGpio.readGpioTestPin('E', 4) + ")");
                        mHandler.sendEmptyMessageDelayed(MESSAGE_VALVE_ON, 9000);
                    }
                    break;
            }
        }
    };

    public TestAutoValveOnOff(Context ctx, int index) {
        mContext = ctx;
        mSubIndex = index;
        mSCManager = InkManagerFactory.inkManager(mContext);
    }

    @Override
    public void show(FrameLayout f) {
        mContainer = f;

        mTestAreaLL = (LinearLayout) LayoutInflater.from(mContext).inflate(R.layout.test_valve_onoff, null);

        mValve1 = (TextView)mTestAreaLL.findViewById(R.id.sc_valve1);
        mValve2 = (TextView)mTestAreaLL.findViewById(R.id.sc_valve2);

        if(mSCManager instanceof SmartCardManager) {
            mHandler.sendEmptyMessageDelayed(MESSAGE_VALVE_ON, 1000);
        } else {
            ToastUtil.show(mContext, "Not Smardcard Version!!!");
        }

        mContainer.addView(mTestAreaLL);
    }

    @Override
    public void setTitle(TextView tv) {
        tv.setText(TITLE);
    }

    @Override
    public boolean quit() {
        mHandler.removeMessages(MESSAGE_VALVE_ON);
        mHandler.removeMessages(MESSAGE_VALVE_OFF);
        mContainer.removeView(mTestAreaLL);
        return true;
    }
}
