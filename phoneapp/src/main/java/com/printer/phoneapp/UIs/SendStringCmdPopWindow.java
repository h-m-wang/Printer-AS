package com.printer.phoneapp.UIs;

import android.content.Context;
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
import com.printer.phoneapp.Sockets.SocketThread;

/**
 * Created by hmwan on 2021/9/7.
 */

public class SendStringCmdPopWindow {
    private Context mContext = null;
    private PopupWindow mPopupWindow = null;
    private DataXfer.OnDataXferListener mListener = null;
    private ConnectDevice mDevice;

    public SendStringCmdPopWindow(Context ctx, ConnectDevice dev, DataXfer.OnDataXferListener l) {
        mContext = ctx;
        mListener = l;
        mDevice = dev;
    }

    public void show(View v) {
        if(null == mContext) {
            return;
        }

        View popupView = LayoutInflater.from(mContext).inflate(R.layout.phone_send_string_cmd_pop_window, null);

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

        final EditText msgET = (EditText)popupView.findViewById(R.id.idSendString);

        TextView okTV = (TextView)popupView.findViewById(R.id.btn_ok);
        okTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPopupWindow.dismiss();
                mDevice.sendString(msgET.getText().toString(), mListener);
            }
        });

        mPopupWindow.showAtLocation(v, Gravity.NO_GRAVITY, 0, 0);
    }
}
