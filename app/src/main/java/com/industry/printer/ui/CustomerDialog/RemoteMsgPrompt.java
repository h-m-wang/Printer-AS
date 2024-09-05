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
    private TextView mCancel;

    public interface EditActionListener {
        public void onOK(String edit);
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
                if(null != mEditActionListener) mEditActionListener.onOK(mEditText.getText().toString());
                setMessageView();
                hide();
            }
        });
        mCancel = (TextView) findViewById(R.id.RMCancel);
        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setMessageView();
                hide();
            }
        });
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
