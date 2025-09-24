package com.industry.printer.ui.CustomerDialog;

import com.industry.printer.Utils.Configs;
import com.industry.printer.Utils.StringUtil;
import com.industry.printer.Utils.ToastUtil;

import rx.internal.util.atomic.MpscLinkedAtomicQueue;
import android.R.string;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.industry.printer.R;
import com.industry.printer.Utils.Debug;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class PWDialog4SaveMsg extends CustomerDialogBase implements View.OnClickListener {

    private String mPassword1;
    private String mPassword2;

    private Button mOk;
    private Button mCancel;
    private EditText mPasswd;

    public PWDialog4SaveMsg(Context context) {
// H.M.Wang 2023-7-20 取消Theme，因为这样生成的对话窗会在显示的时候，屏幕亮度随系统的亮度立即调整，如系统的亮度设的偏暗，则屏幕会立即变暗，看起来很费劲
// 这里不指定Theme，然后在onCreate函数中通过指定Layout为Match_Parent的方法，既可以达到全屏的效果，也可以避免变暗
//		super(context, R.style.Dialog_Fullscreen);
        super(context);
// End of H.M.Wang 2023-7-20 取消Theme，因为这样生成的对话窗会在显示的时候，屏幕亮度随系统的亮度立即调整，如系统的亮度设的偏暗，则屏幕会立即变暗，看起来很费劲

        mPassword1 = "ok";
        mPassword2 = "ok";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.password_dialog);

// H.M.Wang 2023-7-20 取消Theme，因为这样生成的对话窗会在显示的时候，屏幕亮度随系统的亮度立即调整，如系统的亮度设的偏暗，则屏幕会立即变暗，看起来很费劲
/*		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		WindowManager.LayoutParams lp = getWindow().getAttributes();
		lp.width = WindowManager.LayoutParams.MATCH_PARENT;
		lp.height = WindowManager.LayoutParams.MATCH_PARENT;
		getWindow().setAttributes(lp);*/
// End of H.M.Wang 2023-7-20 取消Theme，因为这样生成的对话窗会在显示的时候，屏幕亮度随系统的亮度立即调整，如系统的亮度设的偏暗，则屏幕会立即变暗，看起来很费劲

        try {
            BufferedReader br = new BufferedReader(new FileReader(Configs.SAVE_PW_FILE));
            if (null != br) {
                mPassword1 = br.readLine();
                mPassword2 = br.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        initView();
    }

    private void initView() {
        mOk = (Button) findViewById(R.id.btn_confirm);
        mCancel = (Button) findViewById(R.id.btn_objinfo_cnl);
        mPasswd = (EditText) findViewById(R.id.password);

        mOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (StringUtil.equal(mPasswd.getText().toString(), mPassword1) || StringUtil.equal(mPasswd.getText().toString(), mPassword2)) {
                    if (pListener != null) {
                        pListener.onClick();
                    }
                    dismiss();
                } else {
                    ToastUtil.show(getContext(), R.string.toast_passwd_error);
                    mPasswd.setText("");
                }
            }
        });
        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (nListener != null) {
                    nListener.onClick();
                }
                dismiss();
            }
        });
    }

    @Override
    public void onClick(View view) {

    }
}
