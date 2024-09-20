package com.industry.printer.ui.CustomerDialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.industry.printer.R;

/**
 * Created by hmwan on 2020/11/4.
 */

// H.M.Wang 2023-11-28 追加RelightableDialog作为所有对话窗的父类，用来支持点按屏幕点亮屏幕
public class RemoteMsgPrompt extends RelightableDialog {
//public class RemoteMsgPrompt extends Dialog {
// End of H.M.Wang 2023-11-28 追加RelightableDialog作为所有对话窗的父类，用来支持点按屏幕点亮屏幕

    private Context mContext;
    private RelativeLayout mTotalView;
    private TextView mMessage;

    private RelativeLayout mEditTotalView;
    private EditText mEditText;
    private TextView mOK;
    private TextView mClose;

    private TextView[] mPenBtns;
    private int[] mPenIds = new int[] {R.id.p1, R.id.p2, R.id.p3, R.id.p4};
    private TextView mBPBtn;
    private static int mPrintPens = 0x0f;
    private static boolean mBackPrint = false;

    public interface EditActionListener {
        public void onOK(String edit, int pens, boolean backward);
    }
    private EditActionListener mEditActionListener;

    public RemoteMsgPrompt(Context context) {
        super(context, R.style.Dialog);
        mContext = context;
        mEditActionListener = null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.dialog_remote_msg_prompt);

        mMessage = (TextView) findViewById(R.id.RemoteMsgTV);
        mTotalView = (RelativeLayout) findViewById(R.id.RemoteMsgTotal);

        mTotalView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hide();
            }
        });

        mEditTotalView = (RelativeLayout) findViewById(R.id.EditRemoteMsgTotal);
        mEditText = (EditText) findViewById(R.id.RemoteMsgEdit);
        mOK = (TextView) findViewById(R.id.RMOK);
        mOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(null != mEditActionListener) mEditActionListener.onOK(mEditText.getText().toString(), mPrintPens, mBackPrint);
            }
        });
        mClose = (TextView) findViewById(R.id.RMClose);
        mClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setMessageView();
                hide();
            }
        });

        mPenBtns = new TextView[mPenIds.length];
        for(int i=0; i<mPenIds.length; i++) {
            final int id=i;
            mPenBtns[id] = (TextView) findViewById(mPenIds[id]);
            mPenBtns[id].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    togglePrintPen(id);
                    dispPrintPen(id);
                }
            });
            dispPrintPen(id);
        }

        mBPBtn = (TextView) findViewById(R.id.mirror);
        mBPBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBackPrint = !mBackPrint;
                dispBPBtn();
            }
        });
        dispBPBtn();
    }

    private void dispBPBtn() {
        if(mBackPrint) {
            mBPBtn.setBackgroundColor(mContext.getResources().getColor(R.color.green));
        } else {
            mBPBtn.setBackgroundColor(mContext.getResources().getColor(R.color.gray));
        }
    }

    private void togglePrintPen(int pIndex) {
        int pValue = (0x01 << pIndex);
        if((mPrintPens & pValue) == pValue) {
            mPrintPens &= (~(pValue));
        } else {
            mPrintPens |= pValue;
        }
    }

    private void dispPrintPen(int pIndex) {
        int pValue = (0x01 << pIndex);
        if((mPrintPens & pValue) == pValue) {
            mPenBtns[pIndex].setBackgroundColor(mContext.getResources().getColor(R.color.green));
        } else {
            mPenBtns[pIndex].setBackgroundColor(mContext.getResources().getColor(R.color.gray));
        }
    }

    public void setEditActionListener(EditActionListener l) {
        mEditActionListener = l;
    }

    public void setMessageView() {
        mTotalView.setVisibility(View.VISIBLE);
        mEditTotalView.setVisibility(View.GONE);
    }

    public void setEditView() {
        mTotalView.setVisibility(View.GONE);
        mEditTotalView.setVisibility(View.VISIBLE);
    }

    public void setMessage(String msg) {
        if(mEditTotalView.getVisibility() == View.VISIBLE) {
            mEditText.setText(msg);
        } else {
            mMessage.setText(msg);
        }
    }
}
