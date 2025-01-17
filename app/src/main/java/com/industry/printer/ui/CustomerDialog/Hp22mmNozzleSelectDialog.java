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

// H.M.Wang 2024-12-25 增加IDS和PEN的选择功能，不再使用代码中固定指定的IDS和PEN。暂时只支持IDS和PEN各选1个
    private TextView mIDS1;
    private TextView mIDS0;
    private TextView mPen1;
    private TextView mPen0;
// End of H.M.Wang 2024-12-25 增加IDS和PEN的选择功能，不再使用代码中固定指定的IDS和PEN

    private TextView mPen1_7;
    private TextView mPen1_6;
    private TextView mPen1_5;
    private TextView mPen1_4;
    private TextView mPen0_3;
    private TextView mPen0_2;
    private TextView mPen0_1;
    private TextView mPen0_0;

    private final int IDS1_MASK       = 0x00000800;
    private final int IDS0_MASK       = 0x00000400;
    private final int PEN1_MASK       = 0x00000200;
    private final int PEN0_MASK       = 0x00000100;
    private final int PEN1_SLOT7_MASK = 0x00000080;
    private final int PEN1_SLOT6_MASK = 0x00000040;
    private final int PEN1_SLOT5_MASK = 0x00000020;
    private final int PEN1_SLOT4_MASK = 0x00000010;
    private final int PEN0_SLOT3_MASK = 0x00000008;
    private final int PEN0_SLOT2_MASK = 0x00000004;
    private final int PEN0_SLOT1_MASK = 0x00000002;
    private final int PEN0_SLOT0_MASK = 0x00000001;

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

    private void dispState(TextView v, int mask) {
        if((mValue & mask) == mask) {
            v.setBackgroundColor(Color.GREEN);
//            v.setText("1");
        } else {
            v.setBackgroundColor(Color.LTGRAY);
//            v.setText("0");
        }
    }

    private void toggleState(int mask) {
        if((mValue & mask) == mask) {
            mValue &= ~mask;
        } else {
            mValue |= mask;
        }
    }

    private void clearState(int mask) {
        mValue &= ~mask;
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

        mIDS1 = (TextView) findViewById(R.id.hp22mm_ids1_btn);
        dispState(mIDS1, IDS1_MASK);
        mIDS1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleState(IDS1_MASK);
                dispState(mIDS1, IDS1_MASK);
            }
        });

        mIDS0 = (TextView) findViewById(R.id.hp22mm_ids0_btn);
        dispState(mIDS0, IDS0_MASK);
        mIDS0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleState(IDS0_MASK);
                dispState(mIDS0, IDS0_MASK);
            }
        });

        mPen1 = (TextView) findViewById(R.id.hp22mm_pen1_btn);
        dispState(mPen1, PEN1_MASK);
        mPen1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleState(PEN1_MASK);
                dispState(mPen1, PEN1_MASK);
                if((mValue & PEN1_MASK) == 0x00000000) {
                    clearState(PEN1_SLOT7_MASK);
                    dispState(mPen1_7, PEN1_SLOT7_MASK);
                    clearState(PEN1_SLOT6_MASK);
                    dispState(mPen1_6, PEN1_SLOT6_MASK);
                    clearState(PEN1_SLOT5_MASK);
                    dispState(mPen1_5, PEN1_SLOT5_MASK);
                    clearState(PEN1_SLOT4_MASK);
                    dispState(mPen1_4, PEN1_SLOT4_MASK);
                }
            }
        });

        mPen0 = (TextView) findViewById(R.id.hp22mm_pen0_btn);
        dispState(mPen0, PEN0_MASK);
        mPen0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleState(PEN0_MASK);
                dispState(mPen0, PEN0_MASK);
                if((mValue & PEN0_MASK) == 0x00000000) {
                    clearState(PEN0_SLOT3_MASK);
                    dispState(mPen0_3, PEN0_SLOT3_MASK);
                    clearState(PEN0_SLOT2_MASK);
                    dispState(mPen0_2, PEN0_SLOT2_MASK);
                    clearState(PEN0_SLOT1_MASK);
                    dispState(mPen0_1, PEN0_SLOT1_MASK);
                    clearState(PEN0_SLOT0_MASK);
                    dispState(mPen0_0, PEN0_SLOT0_MASK);
                }
            }
        });

        mPen1_7 = (TextView) findViewById(R.id.pen1_7);
        dispState(mPen1_7, PEN1_SLOT7_MASK);
        mPen1_7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleState(PEN1_SLOT7_MASK);
                dispState(mPen1_7, PEN1_SLOT7_MASK);
            }
        });

        mPen1_6 = (TextView) findViewById(R.id.pen1_6);
        dispState(mPen1_6, PEN1_SLOT6_MASK);
        mPen1_6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleState(PEN1_SLOT6_MASK);
                dispState(mPen1_6, PEN1_SLOT6_MASK);
            }
        });

        mPen1_5 = (TextView) findViewById(R.id.pen1_5);
        dispState(mPen1_5, PEN1_SLOT5_MASK);
        mPen1_5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleState(PEN1_SLOT5_MASK);
                dispState(mPen1_5, PEN1_SLOT5_MASK);
            }
        });

        mPen1_4 = (TextView) findViewById(R.id.pen1_4);
        dispState(mPen1_4, PEN1_SLOT4_MASK);
        mPen1_4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleState(PEN1_SLOT4_MASK);
                dispState(mPen1_4, PEN1_SLOT4_MASK);
            }
        });

        mPen0_3 = (TextView) findViewById(R.id.pen0_3);
        dispState(mPen0_3, PEN0_SLOT3_MASK);
        mPen0_3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleState(PEN0_SLOT3_MASK);
                dispState(mPen0_3, PEN0_SLOT3_MASK);
            }
        });

        mPen0_2 = (TextView) findViewById(R.id.pen0_2);
        dispState(mPen0_2, PEN0_SLOT2_MASK);
        mPen0_2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleState(PEN0_SLOT2_MASK);
                dispState(mPen0_2, PEN0_SLOT2_MASK);
            }
        });

        mPen0_1 = (TextView) findViewById(R.id.pen0_1);
        dispState(mPen0_1, PEN0_SLOT1_MASK);
        mPen0_1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleState(PEN0_SLOT1_MASK);
                dispState(mPen0_1, PEN0_SLOT1_MASK);
            }
        });

        mPen0_0 = (TextView) findViewById(R.id.pen0_0);
        dispState(mPen0_0, PEN0_SLOT0_MASK);
        mPen0_0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleState(PEN0_SLOT0_MASK);
                dispState(mPen0_0, PEN0_SLOT0_MASK);
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
