package com.industry.printer.ui.Test;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Message;
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
  墨袋减锁实验，每点击一次DO，减锁一次
 */
public class TestReduction {
    public static final String TAG = TestReduction.class.getSimpleName();

    private Context mContext = null;
    private PopupWindow mPopupWindow = null;
    private int mSubIndex = 0;
    private IInkDevice mSCManager;

    private TextView mTestResult;

    private final int MSG_SHOW_TEST_RESULT = 107;
    private final int MSG_SHOW_BAG_CONFIRM_DLG = 108;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SHOW_TEST_RESULT:
                    mTestResult.setTextColor(msg.arg1);
                    mTestResult.setText((CharSequence)msg.obj);
                    break;
                case MSG_SHOW_BAG_CONFIRM_DLG:
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

                    AlertDialog dlg;
                    dlg = builder.setTitle("Confirmation")
                            .setMessage("")
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
//                                    Message msg = mHandler.obtainMessage(MSG_SHOW_TEST_RESULT);
//                                    msg.obj = "Cancelled.\n";
//                                    msg.arg1 = Color.RED;
//                                    mHandler.sendMessage(msg);
                                    dialog.dismiss();
                                }
                            })
                            .setPositiveButton("Do", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if(mSCManager instanceof SmartCardManager) {
                                        ((SmartCardManager) mSCManager).reduceBag(new SmartCardManager.SCTestListener() {
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
                                        dialog.dismiss();
                                    }
                                }
                            })
                            .create();
                    dlg.show();
                    break;
            }
        }
    };

    public TestReduction(Context ctx, int index) {
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
                    ((SmartCardManager)mSCManager).stopBagReduce();
                }
                TestSub tmp = new TestSub(mContext, mSubIndex);
                tmp.show(v);
            }
        });

        TextView titleTV = (TextView)popupView.findViewById(R.id.test_title);
        titleTV.setText("Bag Reduction");

        mTestResult = (TextView)popupView.findViewById(R.id.test_result);

        if(mSCManager instanceof SmartCardManager) {
            ((SmartCardManager)mSCManager).startBagReduce(new SmartCardManager.SCTestListener() {
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

                    mHandler.sendEmptyMessageDelayed(MSG_SHOW_BAG_CONFIRM_DLG, 100);
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
