package com.printer.phoneapp.UIs;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.printer.phoneapp.Devices.ConnectDevice;
import com.printer.phoneapp.R;
import com.printer.phoneapp.Sockets.DataXfer;

/**
 * Created by hmwan on 2021/9/7.
 */

public class SendFileCmdPopWindow {
    private Context mContext = null;
    private PopupWindow mPopupWindow = null;
    private DataXfer.OnDataXferListener mListener = null;
    private ConnectDevice mDevice;

    private TextView mSrcFileTV;
    private EditText mDstFileET;

    public SendFileCmdPopWindow(Context ctx, ConnectDevice dev, DataXfer.OnDataXferListener l) {
        mContext = ctx;
        mListener = l;
        mDevice = dev;
    }

    public void show(View v) {
        if(null == mContext) {
            return;
        }

        View popupView = LayoutInflater.from(mContext).inflate(R.layout.phone_send_file_cmd_pop_window, null);

        mPopupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);
        mPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#CC000000")));
        mPopupWindow.setOutsideTouchable(true);
        mPopupWindow.setTouchable(true);
        mPopupWindow.update();

        ImageView closeIV = (ImageView)popupView.findViewById(R.id.Close);
        closeIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPopupWindow.dismiss();
            }
        });

        mSrcFileTV = (TextView)popupView.findViewById(R.id.idSrcFile);
        mSrcFileTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
// 使用 ACTION_GET_CONTENT
//                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//                intent.setType("*/*"); // 设置文件类型，*/* 表示所有类型
//                startActivityForResult(Intent.createChooser(intent, "选择文件"), 10001);

// 使用 ACTION_OPEN_DOCUMENT (API 19+)
//                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
//                intent.addCategory(Intent.CATEGORY_OPENABLE);
//                intent.setType("*/*"); // 设置文件类型
//                startActivityForResult(intent, FILE_SELECT_CODE);
            }
        });

        mDstFileET = (EditText)popupView.findViewById(R.id.idDestFile);

        TextView okTV = (TextView)popupView.findViewById(R.id.btn_ok);
        okTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPopupWindow.dismiss();
                mDevice.sendString(mDstFileET.getText().toString(), mListener);
            }
        });

        mPopupWindow.showAtLocation(v, Gravity.NO_GRAVITY, 0, 0);
    }
}
