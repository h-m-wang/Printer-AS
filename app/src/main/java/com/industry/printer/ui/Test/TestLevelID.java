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
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.industry.printer.R;
import com.industry.printer.hardware.IInkDevice;
import com.industry.printer.hardware.InkManagerFactory;
import com.industry.printer.hardware.SmartCardManager;

/*
  读取Level的设备ID，100次实验
 */
public class TestLevelID {
    public static final String TAG = TestLevelID.class.getSimpleName();

    private Context mContext = null;
    private PopupWindow mPopupWindow = null;
    private int mSubIndex = 0;
    private IInkDevice mSCManager;

    private TextView mTestResult;

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

    public void show(final View v) {
        if (null == mContext) {
            return;
        }

        View popupView = LayoutInflater.from(mContext).inflate(R.layout.test_level, null);

        mPopupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);
        mPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#CC000000")));
        mPopupWindow.setOutsideTouchable(true);
        mPopupWindow.setTouchable(true);
        mPopupWindow.update();

        TextView quitTV = (TextView)popupView.findViewById(R.id.btn_quit);
        quitTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPopupWindow.dismiss();
                if(mSCManager instanceof SmartCardManager) {
                    ((SmartCardManager)mSCManager).stopIDTest();
                }
                TestSub tmp = new TestSub(mContext, mSubIndex);
                tmp.show(v);
            }
        });

        TextView titleTV = (TextView)popupView.findViewById(R.id.test_title);
        titleTV.setText("Level ID Test");

        mTestResult = (TextView)popupView.findViewById(R.id.test_result);

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

        mPopupWindow.showAtLocation(v, Gravity.NO_GRAVITY, 0, 0);
    }

}
