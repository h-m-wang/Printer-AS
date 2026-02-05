package com.industry.printer.Baoqiao;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.support.annotation.UiThread;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;

import com.industry.printer.R;
import com.industry.printer.ThreadPoolManager;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.HttpUtils;
import com.industry.printer.Utils.SM2Cipher;

import org.bouncycastle.util.encoders.Hex;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.util.ArrayList;

/*
    H.M.Wang 2026-2-2 追加宝桥公司的相应功能，工作在模式6下
 */
public class MainFunc {
    public static final String TAG = MainFunc.class.getSimpleName();

    private final static String PREF_BAOQIAO_PASSWORD = "BAOQIAO_PASSWORD";
    private final static String PREF_BAOQIAO_USER_NUMBER = "BAOQIAO_USER_NUMBER";

    private static MainFunc mInstance;
    private Context mContext;
    private Handler mCallback;
    private PopupWindow mPopupWindow;

    public static MainFunc getInstance(Context ctx) {
        if(null == mInstance) {
            mInstance = new MainFunc(ctx);
        }
        return mInstance;
    }

    private MainFunc(Context ctx) {
        mContext = ctx;
        mCallback = null;
        mToken = "";
        mLoginSucceeded = false;

        ThreadPoolManager.mThreads.execute(new Runnable() {
            @Override
            public void run() {
            }
        });
    }

    private EditText mUserNumberET;
    private EditText mPasswordET;
    private RadioButton mRemeberPassword;
    private ProgressBar mProgressBar;
    private TextView mResultError;
    private TextView mLogin;
    private String mToken;
    private boolean mLoginSucceeded;

    public void login(final View v) {
        View popupView = LayoutInflater.from(mContext).inflate(R.layout.baoqiao_user_login, null);

        mPopupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);
        mPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#CC000000")));
        mPopupWindow.setOutsideTouchable(true);
        mPopupWindow.setTouchable(true);
        mPopupWindow.update();

        ImageView cancel = (ImageView) popupView.findViewById(R.id.cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPopupWindow.dismiss();
                if(v instanceof RadioButton) ((RadioButton)v).setChecked(true);
            }
        });

        mUserNumberET = popupView.findViewById(R.id.user_number);
        SharedPreferences sp = mContext.getSharedPreferences(PREF_BAOQIAO_USER_NUMBER, Context.MODE_PRIVATE);
        String userNumber = sp.getString(PREF_BAOQIAO_USER_NUMBER, "");
        mUserNumberET.setText(userNumber);

        mPasswordET = popupView.findViewById(R.id.password);
        sp = mContext.getSharedPreferences(PREF_BAOQIAO_PASSWORD, Context.MODE_PRIVATE);
        String password = sp.getString(PREF_BAOQIAO_PASSWORD, "");
        mPasswordET.setText(password);

        mRemeberPassword = (RadioButton) popupView.findViewById(R.id.remember_password);
        mProgressBar = (ProgressBar) popupView.findViewById(R.id.processing);
        mResultError = (TextView) popupView.findViewById(R.id.result_error);

        mLogin = popupView.findViewById(R.id.btn_login);
        mLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mUserNumberET.getText().toString().isEmpty()) {
                    mResultError.setText(R.string.str_error_user_empty);
                    mResultError.setVisibility(View.VISIBLE);
                    return;
                }
                if(mPasswordET.getText().toString().isEmpty()) {
                    mResultError.setText(R.string.str_error_pw_empty);
                    mResultError.setVisibility(View.VISIBLE);
                    return;
                }
                mProgressBar.setVisibility(View.VISIBLE);
                mResultError.setVisibility(View.GONE);

                ThreadPoolManager.mThreads.execute(new Runnable() {
                    @Override
                    public void run() {
                        final StringBuilder sb = new StringBuilder();
                        HttpUtils httpUtils = new HttpUtils(
                                "http://api.crbbiservice.com:8117/webapi/accountInterface/login?" +
                                        "userNumber=" + Hex.toHexString(SM2Cipher.encrypt(mUserNumberET.getText().toString().getBytes(Charset.forName("UTF-8")))) +
                                        "&password=" + Hex.toHexString(SM2Cipher.encrypt(mPasswordET.getText().toString().getBytes(Charset.forName("UTF-8")))),
                                "POST",
                                "",
                                new HttpUtils.HttpResponseListener() {
                                    @Override
                                    public void onReceived(String str) {
                                        mResultError.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                mProgressBar.setVisibility(View.GONE);
                                            }
                                        });
                                        if (null != str) {
                                            sb.append(str);
                                        } else {
                                            if (sb.length() == 0) {
                                                mResultError.post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        mResultError.setText("Network access error!");
                                                        mResultError.setVisibility(View.VISIBLE);
                                                    }
                                                });
                                            } else {
                                                String recvStr = sb.toString();
//                                                Debug.d(TAG, recvStr);
                                                try {
                                                    JSONObject jObj = new JSONObject(recvStr);
                                                    final String message = jObj.getString("message");
                                                    if (!"请求成功！".equals(message)) {
                                                        mResultError.post(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                mResultError.setText(message);
                                                                mResultError.setVisibility(View.VISIBLE);
                                                            }
                                                        });
                                                        return;
                                                    }
                                                    mToken = jObj.getString("data");
                                                    mLoginSucceeded = true;
                                                    SharedPreferences sp = mContext.getSharedPreferences(PREF_BAOQIAO_USER_NUMBER, Context.MODE_PRIVATE);
                                                    sp.edit().putString(PREF_BAOQIAO_USER_NUMBER, mUserNumberET.getText().toString()).apply();
                                                    if (mRemeberPassword.isChecked()) {
                                                        sp = mContext.getSharedPreferences(PREF_BAOQIAO_PASSWORD, Context.MODE_PRIVATE);
                                                        sp.edit().putString(PREF_BAOQIAO_PASSWORD, mPasswordET.getText().toString()).apply();
                                                    } else {
                                                        sp = mContext.getSharedPreferences(PREF_BAOQIAO_PASSWORD, Context.MODE_PRIVATE);
                                                        sp.edit().putString(PREF_BAOQIAO_PASSWORD, "").apply();
                                                    }
                                                    mResultError.post(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            mResultError.setText(R.string.str_login_success);
                                                            mResultError.setTextColor(Color.GREEN);
                                                            mResultError.setVisibility(View.VISIBLE);
                                                        }
                                                    });
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }
                                    }
                                });
                        httpUtils.access();
                    }
                });
            }
        });
        mPopupWindow.showAtLocation(v, Gravity.NO_GRAVITY, 0, 0);
    }
}