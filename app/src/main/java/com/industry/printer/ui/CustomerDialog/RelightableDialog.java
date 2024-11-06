package com.industry.printer.ui.CustomerDialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;

import com.industry.printer.FileFormat.SystemConfigFile;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.PlatformInfo;

// H.M.Wang 2023-11-28 追加RelightableDialog作为所有对话窗的父类，用来支持点按屏幕点亮屏幕
public class RelightableDialog extends Dialog {
    private static final String TAG = RelightableDialog.class.getSimpleName();

    private Context mContext;
    private boolean mScreensaveMode;

    public static final int ENTER_LOWLIGHT_MODE = 5;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ENTER_LOWLIGHT_MODE:
                    setScreenBrightness(true);
                    break;
            }
        }
    };

    public RelightableDialog(Context context) {
        super(context);
        mContext = context;
        mScreensaveMode = true;
    }

    public RelightableDialog(Context context, int theme) {
        super(context, theme);
        mContext = context;
        mScreensaveMode = true;
    }

    private void setScreenBrightness(boolean save) {
        Debug.d(TAG, "--->setScreenBrightness. mScreensaveMode=" + mScreensaveMode + " ;save=" + save);
// H.M.Wang 2024-11-5 A133平台不能使用该方法设置亮度，否则死机
        if(PlatformInfo.isA133Product()) return;
// End of H.M.Wang 2024-11-5 A133平台不能使用该方法设置亮度，否则死机
        if (save == false) {
            mHandler.removeMessages(ENTER_LOWLIGHT_MODE);
            mHandler.sendEmptyMessageDelayed(ENTER_LOWLIGHT_MODE, 60 * 1000);
        } else {
            mHandler.removeMessages(ENTER_LOWLIGHT_MODE);
        }

        if (mScreensaveMode == save) {
            return;
        }

        mScreensaveMode = save;
        SystemConfigFile config = SystemConfigFile.getInstance(mContext);
        float percent = config.getParam(SystemConfigFile.INDEX_LIGHTNESS) / 100.0f;
// H.M.Wang 2023-7-17 3.5寸盘亮度固定为50，其余不变
        String info = PlatformInfo.getImgUniqueCode();
        if(info.startsWith("NNG3") || info.startsWith("ONG3") || info.startsWith("GZJ") || info.startsWith("NSM2")) {
            percent = 0.5f;
        }
// End of H.M.Wang 2023-7-17 3.5寸盘亮度固定为50，其余不变
        int brightness = mScreensaveMode ? (int)(255 * percent) : 255;
        Window window = getWindow();
        WindowManager.LayoutParams localLP = window.getAttributes();
        float f = brightness / 255.0f;
        localLP.screenBrightness = f;
        window.setAttributes(localLP);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
//            Debug.d(TAG, "--->onTouch：" + event.getX() + ", " + event.getY());
            setScreenBrightness(false);
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public void dismiss() {
        mHandler.removeMessages(ENTER_LOWLIGHT_MODE);
        super.dismiss();
    }
}
// End of H.M.Wang 2023-11-28 追加RelightableDialog作为所有对话窗的父类，用来支持点按屏幕点亮屏幕
