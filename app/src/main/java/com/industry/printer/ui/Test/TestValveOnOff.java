package com.industry.printer.ui.Test;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.industry.printer.R;
import com.industry.printer.hardware.IInkDevice;
import com.industry.printer.hardware.InkManagerFactory;
import com.industry.printer.hardware.SmartCardManager;

/*
  连供的阀开关测试
 */
public class TestValveOnOff {
    public static final String TAG = TestValveOnOff.class.getSimpleName();

    private Context mContext = null;
    private PopupWindow mPopupWindow = null;
    private int mSubIndex = 0;
    private IInkDevice mSCManager;

    public TestValveOnOff(Context ctx, int index) {
        mContext = ctx;
        mSubIndex = index;
        mSCManager = InkManagerFactory.inkManager(mContext);
    }

    public void show(final View v) {
        if (null == mContext) {
            return;
        }

        View popupView = LayoutInflater.from(mContext).inflate(R.layout.test_valve_onoff, null);

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
                    ((SmartCardManager)mSCManager).stopBagReduce();
                }
                TestSub tmp = new TestSub(mContext, mSubIndex);
                tmp.show(v);
            }
        });

        TextView titleTV = (TextView)popupView.findViewById(R.id.test_title);
        titleTV.setText("SmartCard Valve On/Off Test");

        TextView scValve1 = (TextView)popupView.findViewById(R.id.sc_valve1);
        scValve1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final IInkDevice scm = InkManagerFactory.inkManager(mContext);
                if(scm instanceof SmartCardManager) {
                    ((SmartCardManager)scm).addInkOn(0);
                    try {Thread.sleep(100);} catch (Exception e) {}
                    ((SmartCardManager)scm).addInkOff(0);
                }
            }
        });
        TextView scValve2 = (TextView)popupView.findViewById(R.id.sc_valve2);
        scValve2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final IInkDevice scm = InkManagerFactory.inkManager(mContext);
                if(scm instanceof SmartCardManager) {
                    ((SmartCardManager)scm).addInkOn(1);
                    try {Thread.sleep(100);} catch (Exception e) {}
                    ((SmartCardManager)scm).addInkOff(1);
                }
            }
        });

        mPopupWindow.showAtLocation(v, Gravity.NO_GRAVITY, 0, 0);
    }

}
