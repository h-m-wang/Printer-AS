package com.industry.printer.Baoqiao;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.support.annotation.UiThread;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;

import com.industry.printer.ControlTabActivity;
import com.industry.printer.DataTransferThread;
import com.industry.printer.FileFormat.SystemConfigFile;
import com.industry.printer.R;
import com.industry.printer.ThreadPoolManager;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.HttpUtils;
import com.industry.printer.Utils.SM2Cipher;
import com.industry.printer.hardware.BarcodeScanParser;

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
    private ControlTabActivity mCallback;
    private PopupWindow mLoginPopupWindow;
    private PopupWindow mPrintPopupWindow;

    private View mAnchorView;

    private EditText mUserNumberET;
    private EditText mPasswordET;
    private RadioButton mRemeberPassword;
    private ProgressBar mProgressBar;
    private TextView mResultError;
    private TextView mLogin;
    private boolean mLoginSucceeded;

    private String mToken;
    private String mUserNumber;

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

    public void setCallback(ControlTabActivity callback) {
        mCallback = callback;
    }

    private void doLogin(String userNumber, String password) {
        final StringBuilder sb = new StringBuilder();
        new HttpUtils()
            .setUrl("http://api.crbbiservice.com:8117/webapi/accountInterface/login?" +
                    "userNumber=" + Hex.toHexString(SM2Cipher.encrypt(userNumber.getBytes(Charset.forName("UTF-8")))) +
                    "&password=" + Hex.toHexString(SM2Cipher.encrypt(password.getBytes(Charset.forName("UTF-8")))))
            .setMethod("POST")
            .setListeners(new HttpUtils.HttpResponseListener() {
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
                                mUserNumber = mUserNumberET.getText().toString();
                                sp.edit().putString(PREF_BAOQIAO_USER_NUMBER, mUserNumber).apply();
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
//                                        mResultError.setText(R.string.str_login_success);
//                                        mResultError.setTextColor(Color.GREEN);
//                                        mResultError.setVisibility(View.VISIBLE);
                                        getInstance(mContext).setPrintContent(mAnchorView);
                                    }
                                });
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }).access();
    }

    private void fetchData(String lsCode) {
        Debug.d(TAG, lsCode);
        final StringBuilder sb = new StringBuilder();
        new HttpUtils()
            .setUrl("http://api.crbbiservice.com:8117/webapi/sales_extension/getSprayDescription?" +
                    "lsCode=" + lsCode)
            .setMethod("GET")
            .setHeaderParams("Authorization", mToken)
            .setListeners(new HttpUtils.HttpResponseListener() {
                @Override
                public void onReceived(String str) {
                mConfirm.post(new Runnable() {
                    @Override
                    public void run() {
                        mProgressBar.setVisibility(View.GONE);
                    }
                });
                if (null != str) {
                    sb.append(str);
                } else {
                    if (sb.length() == 0) {
                        mConfirm.post(new Runnable() {
                            @Override
                            public void run() {
                                mRecvContent.setText("Network access error!");
                                mRecvContent.setVisibility(View.VISIBLE);
                                mRecvContent.setTextColor(Color.RED);
                            }
                        });
                    } else {
                        String recvStr = sb.toString();
//                                                Debug.d(TAG, recvStr);
                        try {
                            JSONObject jObj = new JSONObject(recvStr);
                            final String message = jObj.getString("message");
                            if (!"请求成功！".equals(message)) {
                                mConfirm.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mRecvContent.setText(message);
                                        mRecvContent.setVisibility(View.VISIBLE);
                                        mRecvContent.setTextColor(Color.RED);
                                    }
                                });
                                return;
                            }
                            final String data = jObj.getString("data");
                            mConfirm.post(new Runnable() {
                                @Override
                                public void run() {
                                    mRecvContent.setText(data);
                                    mRecvContent.setVisibility(View.VISIBLE);
                                    mRecvContent.setTextColor(Color.BLACK);
                                }
                            });
                            SystemConfigFile.getInstance(mContext).setDTBuffer(0, data);
                            DataTransferThread dTransThread = DataTransferThread.getInstance(mContext);
                            if(null != dTransThread && dTransThread.isRunning()) {
                                dTransThread.mNeedUpdate = true;
                            }
                            postResult(data);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
                }
            }).access();
    }

    private void postResult(String data) {
        Debug.d(TAG, "http://api.crbbiservice.com:8117/webapi/sales_extension/addSprayOperationLog?" +
                "userNumber=" + mUserNumber + "&content=" + data);
        new HttpUtils()
            .setUrl("http://api.crbbiservice.com:8117/webapi/sales_extension/addSprayOperationLog?" +
                    "userNumber=" + mUserNumber + "&content=" + data)
            .setMethod("POST")
            .setHeaderParams("Authorization", mToken)
            .setListeners(new HttpUtils.HttpResponseListener() {
                @Override
                public void onReceived(String str) {
                    Debug.d(TAG, str);
                }
            }).access();
    }

    public void login(View v) {
        mAnchorView = v;
        if(null != mPrintPopupWindow && mPrintPopupWindow.isShowing()) mPrintPopupWindow.dismiss();

        View popupView = LayoutInflater.from(mContext).inflate(R.layout.baoqiao_user_login, null);

        mLoginPopupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);
        mLoginPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#CC000000")));
        mLoginPopupWindow.setOutsideTouchable(true);
        mLoginPopupWindow.setTouchable(true);
        mLoginPopupWindow.update();
        ImageView cancel = (ImageView) popupView.findViewById(R.id.cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mLoginPopupWindow.dismiss();
                if(mAnchorView instanceof RadioButton) ((RadioButton)mAnchorView).setChecked(true);
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
                        doLogin(mUserNumberET.getText().toString(), mPasswordET.getText().toString());
                    }
                });
            }
        });
        mLoginPopupWindow.showAtLocation(v, Gravity.NO_GRAVITY, 0, 0);
    }

    private EditText mSeqNumberET;
    private TextView mConfirm;
    private TextView mRecvContent;
    private long mLastKeyEvent;

    public void setPrintContent(final View v) {
        if(null != mLoginPopupWindow && mLoginPopupWindow.isShowing()) mLoginPopupWindow.dismiss();

        View popupView = LayoutInflater.from(mContext).inflate(R.layout.baoqiao_content, null);

        mPrintPopupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);
        mPrintPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#CC000000")));
        mPrintPopupWindow.setOutsideTouchable(true);
        mPrintPopupWindow.setTouchable(true);
        mPrintPopupWindow.update();

        ImageView cancel = (ImageView) popupView.findViewById(R.id.cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPrintPopupWindow.dismiss();
                if(v instanceof RadioButton) ((RadioButton)v).setChecked(true);
            }
        });

        mSeqNumberET = popupView.findViewById(R.id.sequence_number);
        BarcodeScanParser.setListener(new BarcodeScanParser.OnScanCodeListener() {
            @Override
            public void onCodeReceived(final String code) {
                Debug.d(TAG, "Scan: " + code);
                mSeqNumberET.post(new Runnable() {
                    @Override
                    public void run() {
                        mSeqNumberET.setText("");
                        mSeqNumberET.setText(code);
                    }
                });
                fetchData(code);
            }
        });

        mLastKeyEvent = System.currentTimeMillis();
        mSeqNumberET.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(System.currentTimeMillis() - mLastKeyEvent > 1000) mSeqNumberET.setText("");
                mLastKeyEvent = System.currentTimeMillis();
                if(event.getAction() == KeyEvent.ACTION_DOWN) {
                    if(keyCode == KeyEvent.KEYCODE_ENTER) {
                        return true;
                    } else {
                        BarcodeScanParser.append(keyCode, event.isShiftPressed());
                    }
                }
                return false;
            }
        });

        mProgressBar = (ProgressBar) popupView.findViewById(R.id.processing);

        mConfirm = popupView.findViewById(R.id.btn_confirm);
        mConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ThreadPoolManager.mThreads.execute(new Runnable() {
                    @Override
                    public void run() {
                        fetchData(mSeqNumberET.getText().toString());
                    }
                });
            }
        });

        mConfirm = popupView.findViewById(R.id.btn_confirm);
        mRecvContent = popupView.findViewById(R.id.recv_content);

        TextView start = popupView.findViewById(R.id.tv_start);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(null != mCallback) mCallback.mBtnStart.performClick();
            }
        });

        TextView stop = popupView.findViewById(R.id.tv_stop);
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(null != mCallback) mCallback.mBtnStop.performClick();
            }
        });

        TextView purge = popupView.findViewById(R.id.tv_purge);
        purge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(null != mCallback) mCallback.mBtnClean.performClick();
            }
        });

        mPrintPopupWindow.showAtLocation(v, Gravity.NO_GRAVITY, 0, 0);
    }
}