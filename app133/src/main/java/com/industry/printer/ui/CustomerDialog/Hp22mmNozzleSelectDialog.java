package com.industry.printer.ui.CustomerDialog;

import com.industry.printer.R;
import com.industry.printer.ui.CustomerAdapter.SettingsListAdapter;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.ResourceBundle;

/**
 * Created by hmwan on 2024/4/3.
 */

// H.M.Wang 2023-11-28 追加RelightableDialog作为所有对话窗的父类，用来支持点按屏幕点亮屏幕
public class Hp22mmNozzleSelectDialog extends RelightableDialog implements android.view.View.OnClickListener {
    //public class PrintRepeatEditDialog extends Dialog implements android.view.View.OnClickListener {
// End of H.M.Wang 2023-11-28 追加RelightableDialog作为所有对话窗的父类，用来支持点按屏幕点亮屏幕
    private static final String TAG = CounterEditDialog.class.getSimpleName();

    private Context mContext;

    private TextView mConfirm;
    private TextView mCancel;
    private TextView mPen2_7;
    private TextView mPen2_6;
    private TextView mPen2_5;
    private TextView mPen2_4;
    private TextView mPen1_3;
    private TextView mPen1_2;
    private TextView mPen1_1;
    private TextView mPen1_0;

    private Handler mHandler;
    private int  mValue;
    public Hp22mmNozzleSelectDialog(Context context, Handler handler, int value) {
// H.M.Wang 2023-7-20 取消Theme，因为这样生成的对话窗会在显示的时候，屏幕亮度随系统的亮度立即调整，如系统的亮度设的偏暗，则屏幕会立即变暗，看起来很费劲
// 这里不指定Theme，然后在onCreate函数中通过指定Layout为Match_Parent的方法，既可以达到全屏的效果，也可以避免变暗
//		super(context, R.style.Dialog_Fullscreen);
        super(context);
// End of H.M.Wang 2023-7-20 取消Theme，因为这样生成的对话窗会在显示的时候，屏幕亮度随系统的亮度立即调整，如系统的亮度设的偏暗，则屏幕会立即变暗，看起来很费劲
        mContext = context;
        mHandler = handler;
        mValue = value;
    }

    private void dispComponent(TextView v, int mask) {
        if((mValue & mask) == mask) {
            v.setBackgroundColor(Color.GREEN);
            v.setText("1");
        } else {
            v.setBackgroundColor(Color.LTGRAY);
            v.setText("0");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.hp22mm_nozzle_select_dialog);

// H.M.Wang 2023-7-20 取消Theme，因为这样生成的对话窗会在显示的时候，屏幕亮度随系统的亮度立即调整，如系统的亮度设的偏暗，则屏幕会立即变暗，看起来很费劲
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        getWindow().setAttributes(lp);
// End of H.M.Wang 2023-7-20 取消Theme，因为这样生成的对话窗会在显示的时候，屏幕亮度随系统的亮度立即调整，如系统的亮度设的偏暗，则屏幕会立即变暗，看起来很费劲

        mPen2_7 = (TextView) findViewById(R.id.pen2_7);
        dispComponent(mPen2_7, 0x00000080);
        mPen2_7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if((mValue & 0x00000080) == 0x00000080) {
                    mValue &= 0x0000007F;
                } else {
                    mValue |= 0x00000080;
                }
                dispComponent(mPen2_7, 0x00000080);
            }
        });

        mPen2_6 = (TextView) findViewById(R.id.pen2_6);
        dispComponent(mPen2_6, 0x00000040);
        mPen2_6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if((mValue & 0x00000040) == 0x00000040) {
                    mValue &= 0x000000BF;
                } else {
                    mValue |= 0x00000040;
                }
                dispComponent(mPen2_6, 0x00000040);
            }
        });

        mPen2_5 = (TextView) findViewById(R.id.pen2_5);
        dispComponent(mPen2_5, 0x00000020);
        mPen2_5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if((mValue & 0x00000020) == 0x00000020) {
                    mValue &= 0x000000DF;
                } else {
                    mValue |= 0x00000020;
                }
                dispComponent(mPen2_5, 0x00000020);
            }
        });

        mPen2_4 = (TextView) findViewById(R.id.pen2_4);
        dispComponent(mPen2_4, 0x00000010);
        mPen2_4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if((mValue & 0x00000010) == 0x00000010) {
                    mValue &= 0x000000EF;
                } else {
                    mValue |= 0x00000010;
                }
                dispComponent(mPen2_4, 0x00000010);
            }
        });

        mPen1_3 = (TextView) findViewById(R.id.pen1_3);
        dispComponent(mPen1_3, 0x00000008);
        mPen1_3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if((mValue & 0x00000008) == 0x00000008) {
                    mValue &= 0x00000007;
                } else {
                    mValue |= 0x00000008;
                }
                dispComponent(mPen1_3, 0x00000008);
            }
        });

        mPen1_2 = (TextView) findViewById(R.id.pen1_2);
        dispComponent(mPen1_2, 0x00000004);
        mPen1_2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if((mValue & 0x00000004) == 0x00000004) {
                    mValue &= 0x0000000B;
                } else {
                    mValue |= 0x00000004;
                }
                dispComponent(mPen1_2, 0x00000004);
            }
        });

        mPen1_1 = (TextView) findViewById(R.id.pen1_1);
        dispComponent(mPen1_1, 0x00000002);
        mPen1_1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if((mValue & 0x00000002) == 0x00000002) {
                    mValue &= 0x0000000D;
                } else {
                    mValue |= 0x00000002;
                }
                dispComponent(mPen1_1, 0x00000002);
            }
        });

        mPen1_0 = (TextView) findViewById(R.id.pen1_0);
        dispComponent(mPen1_0, 0x00000001);
        mPen1_0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if((mValue & 0x00000001) == 0x00000001) {
                    mValue &= 0x0000000E;
                } else {
                    mValue |= 0x00000001;
                }
                dispComponent(mPen1_0, 0x00000001);
            }
        });

        mConfirm = (TextView) findViewById(R.id.confirm);
        mCancel = (TextView) findViewById(R.id.cancel);
        mConfirm.setOnClickListener(this);
        mCancel.setOnClickListener(this);
    }

    @Override
    public void onClick(View arg0) {
        switch (arg0.getId()) {
            case R.id.confirm:
                Message msg = mHandler.obtainMessage(SettingsListAdapter.MSG_HP22MM_NOZZLE_SEL);
                msg.arg1 = mValue;
                mHandler.sendMessage(msg);
                dismiss();
                break;
            case R.id.cancel:
                dismiss();
            default:
                break;
        }
    }
}
