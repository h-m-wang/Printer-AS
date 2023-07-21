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
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;

import com.industry.printer.R;
import com.industry.printer.Utils.Debug;
import com.industry.printer.hardware.IInkDevice;
import com.industry.printer.hardware.InkManagerFactory;
import com.industry.printer.hardware.SmartCardManager;

/*
  读取Level的设备ID，100次实验
 */
public class TestLevelID implements ITestOperation {
    public static final String TAG = TestLevelID.class.getSimpleName();

    private Context mContext = null;
    private FrameLayout mContainer = null;
    private ScrollView mTestAreaSV = null;

    private int mSubIndex = 0;
    private IInkDevice mSCManager;

    private TextView mTestResult;

    private final String TITLE = "Level ID Test";

    private final int MSG_SHOW_TEST_RESULT = 107;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SHOW_TEST_RESULT:
                    mTestResult.setTextColor(msg.arg1);
                    mTestResult.setText((CharSequence)msg.obj);
                    break;
            }
        }
    };

    public TestLevelID(Context ctx, int index) {
        mContext = ctx;
        mSubIndex = index;
        mSCManager = InkManagerFactory.inkManager(mContext);
    }

    @Override
    public void show(FrameLayout f) {
        mContainer = f;

        mTestAreaSV = (ScrollView)LayoutInflater.from(mContext).inflate(R.layout.test_level, null);
        mTestResult = (TextView)mTestAreaSV.findViewById(R.id.test_result);

        if(mSCManager instanceof SmartCardManager) {
            ((SmartCardManager)mSCManager).startIDTest(new SmartCardManager.SCTestListener() {
                @Override
                public void onError(String result) {
                    Message msg = mHandler.obtainMessage(MSG_SHOW_TEST_RESULT);
                    msg.obj = result;
                    msg.arg1 = Color.RED;
                    mHandler.sendMessage(msg);
                }

                @Override
                public void onResult(String result) {
                    Message msg = mHandler.obtainMessage(MSG_SHOW_TEST_RESULT);
                    msg.obj = result;
                    msg.arg1 = Color.BLACK;
                    mHandler.sendMessage(msg);
                }
            });
        } else {
            Message msg = mHandler.obtainMessage(MSG_SHOW_TEST_RESULT);
            msg.obj = "Smart card not installed.\n";
            msg.arg1 = Color.RED;
            mHandler.sendMessage(msg);
        }

        mContainer.addView(mTestAreaSV);
    }

    @Override
    public void setTitle(TextView tv) {
        tv.setText(TITLE);
    }

    @Override
    public boolean quit() {
        if(mSCManager instanceof SmartCardManager) {
            ((SmartCardManager)mSCManager).stopIDTest();
        }
        mContainer.removeView(mTestAreaSV);
        return true;
    }
}
