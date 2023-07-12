package com.industry.printer.ui.FuncTest;

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

public class TestMainPage {
    public static final String TAG = TestMainPage.class.getSimpleName();

    private Context mContext = null;
    private PopupWindow mPopupWindow = null;

    public TestMainPage(Context ctx) {
        mContext = ctx;
    }

    public void show(View v) {
        if (null == mContext) {
            return;
        }

        View popupView = LayoutInflater.from(mContext).inflate(R.layout.test_main_page, null);

        mPopupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);
        mPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#CC000000")));
        mPopupWindow.setOutsideTouchable(true);
        mPopupWindow.setTouchable(true);
        mPopupWindow.update();

        TextView quitIV = (TextView)popupView.findViewById(R.id.btn_quit);
        quitIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                mHandler.sendEmptyMessage(MSG_TERMINATE_TEST);
                mPopupWindow.dismiss();
            }
        });

        mPopupWindow.showAtLocation(v, Gravity.NO_GRAVITY, 0, 0);
    }
}
