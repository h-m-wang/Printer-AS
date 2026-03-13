package com.industry.printer.ui.CustomerDialog;

import com.industry.printer.R;
import com.industry.printer.Utils.Debug;
import com.industry.printer.hardware.BarcodeScanParser;
import com.industry.printer.ui.CustomerAdapter.SettingsListAdapter;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Created by hmwan on 2026/3/10.
 */

// H.M.Wang 2023-11-28 追加RelightableDialog作为所有对话窗的父类，用来支持点按屏幕点亮屏幕
public class CntConfirmDialogScanner12 extends RelightableDialog {
    //public class EncoderPPREditDialog extends Dialog implements android.view.View.OnClickListener {
// End of H.M.Wang 2023-11-28 追加RelightableDialog作为所有对话窗的父类，用来支持点按屏幕点亮屏幕
    private static final String TAG = CntConfirmDialogScanner12.class.getSimpleName();

    private Context mContext;

    private EditText mLabel;
    private TextView mLabelTV;
    private EditText mSize;
    private TextView mSizeTV;

    private String mLabelCnt;
    private String mSizeCnt;

    private TextView mConfirm;
    private TextView mCancel;

    public interface ConfirmListener {
        public void onConfirmed(String lable, String size);
    };
    private ConfirmListener mListener;

    public CntConfirmDialogScanner12(Context context, ConfirmListener l) {
// H.M.Wang 2023-7-20 取消Theme，因为这样生成的对话窗会在显示的时候，屏幕亮度随系统的亮度立即调整，如系统的亮度设的偏暗，则屏幕会立即变暗，看起来很费劲
// 这里不指定Theme，然后在onCreate函数中通过指定Layout为Match_Parent的方法，既可以达到全屏的效果，也可以避免变暗
//		super(context, R.style.Dialog_Fullscreen);
        super(context);
// End of H.M.Wang 2023-7-20 取消Theme，因为这样生成的对话窗会在显示的时候，屏幕亮度随系统的亮度立即调整，如系统的亮度设的偏暗，则屏幕会立即变暗，看起来很费劲
        mContext = context;
        mListener = l;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.dialog_scan12_comfirm);

// H.M.Wang 2023-7-20 取消Theme，因为这样生成的对话窗会在显示的时候，屏幕亮度随系统的亮度立即调整，如系统的亮度设的偏暗，则屏幕会立即变暗，看起来很费劲
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        getWindow().setAttributes(lp);
// End of H.M.Wang 2023-7-20 取消Theme，因为这样生成的对话窗会在显示的时候，屏幕亮度随系统的亮度立即调整，如系统的亮度设的偏暗，则屏幕会立即变暗，看起来很费劲

        mLabel = (EditText) findViewById(R.id.label);
        mLabelTV = (TextView) findViewById(R.id.labelTV);
        mLabelTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLabel.setText(mLabelTV.getText());
                mLabel.setVisibility(View.VISIBLE);
                mLabelTV.setVisibility(View.GONE);
            }
        });

        mSize = (EditText) findViewById(R.id.size);
        mSizeTV = (TextView) findViewById(R.id.sizeTV);
        mSizeTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSize.setText(mSizeTV.getText());
                mSize.setVisibility(View.VISIBLE);
                mSizeTV.setVisibility(View.GONE);
            }
        });
        mConfirm = (TextView) findViewById(R.id.btn_confirm);
        mCancel = (TextView) findViewById(R.id.btn_cancel);
        mConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(null != mListener) {
                    mListener.onConfirmed(mLabel.getText().toString(), mSize.getText().toString());
                }
                if(mLabel.getVisibility() == View.VISIBLE) {
                    mLabelTV.setText(mLabel.getText());
                    mLabelTV.setVisibility(View.VISIBLE);
                    mLabel.setVisibility(View.GONE);
                }
                if(mSize.getVisibility() == View.VISIBLE) {
                    mSizeTV.setText(mSize.getText());
                    mSizeTV.setVisibility(View.VISIBLE);
                    mSize.setVisibility(View.GONE);
                }
            }
        });
        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }

    public void setLabelStr(String labelCnt) {
        mLabelCnt = labelCnt;
        mLabelTV.setText(mLabelCnt);
        mLabelTV.setVisibility(View.VISIBLE);
        mLabel.setVisibility(View.GONE);
    }

    public void setSizeStr(String sizeCnt) {
        mSizeCnt = sizeCnt;
        mSizeTV.setText(mSizeCnt);
        mSizeTV.setVisibility(View.VISIBLE);
        mSize.setVisibility(View.GONE);
    }
}
