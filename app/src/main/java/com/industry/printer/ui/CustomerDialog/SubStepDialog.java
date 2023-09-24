package com.industry.printer.ui.CustomerDialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.industry.printer.FileFormat.SystemConfigFile;
import com.industry.printer.R;
import com.industry.printer.hardware.RTCDevice;

/**
 * Created by hmwan on 2023/9/20.
 */

public class SubStepDialog  extends Dialog {
    private static final String TAG = SubStepDialog.class.getSimpleName();

    private Context mContext;

    private EditText mSubStepEV;
    private ImageView mUpBtn;
    private ImageView mDownBtn;
    private TextView mConfirm;
    private TextView mCancel;

    private int mCurCount;
    private int mMaxValue;

    public SubStepDialog(Context ctx) {
        super(ctx, R.style.Dialog);
        mContext = ctx;

        mCurCount = RTCDevice.getInstance(mContext).readSubStep();
        mMaxValue = SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_SUB_STEP);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.sub_step_dialog);

        mSubStepEV = (EditText) findViewById(R.id.sub_step_et);
        mSubStepEV.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                try {
                    mCurCount = Integer.valueOf(editable.toString());
                    if(mCurCount > mMaxValue) mCurCount = mMaxValue;
                    if(mCurCount < 1) mCurCount = 1;
                } catch (NumberFormatException e) {

                }
            }
        });
        mSubStepEV.setText("" + mCurCount);

        mUpBtn = (ImageView) findViewById(R.id.btn_up);
        mUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCurCount++;
                if(mCurCount > mMaxValue) mCurCount = mMaxValue;
                mSubStepEV.setText("" + mCurCount);
            }
        });
        mDownBtn = (ImageView) findViewById(R.id.btn_down);
        mDownBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCurCount--;
                if(mCurCount < 1) mCurCount = 1;
                mSubStepEV.setText("" + mCurCount);
            }
        });

        mConfirm = (TextView) findViewById(R.id.confirm);
        mCancel = (TextView) findViewById(R.id.cancel);

        mConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                RTCDevice.getInstance(mContext).writeSubStep(mCurCount);
            }
        });

        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }
}
