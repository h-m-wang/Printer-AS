package com.industry.printer.hardware;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.industry.printer.ControlTabActivity;
import com.industry.printer.FileFormat.SystemConfigFile;
import com.industry.printer.PHeader.PrinterNozzle;
import com.industry.printer.Utils.Debug;

public class Hp22mmSCManager implements IInkDevice {
    private static final String TAG = Hp22mmSCManager.class.getSimpleName();

    private final static int PEN_VS_BAG_RATIO           = 3;
    private static int MAX_BAG_INK_VOLUME         = 3150;
    private static int MAX_PEN_INK_VOLUME         = MAX_BAG_INK_VOLUME * PEN_VS_BAG_RATIO;

    private Context mContext;
    private Handler mCallback;
    private boolean mLibInited;

    // 暂时只支持1个打印头，将来如果支持多个打印头的话，需要对这里一系列的参数分头管理
    private int mInkLevel;
    private boolean mValid;
// H.M.Wang 2024-6-15 初始化成功的标识，用来阻止初始化完成前getLocalInkPercentage函数返回0，导致ControlTabActivity出现报警的问题
    private boolean mInitialized;
// End of H.M.Wang 2024-6-15 初始化成功的标识，用来阻止初始化完成前getLocalInkPercentage函数返回0，导致ControlTabActivity出现报警的问题

// H.M.Wang 2024-7-10 追加错误信息返回主控制页面的功能
    public static final int MSG_HP22MM_ERROR = 23;
// End of H.M.Wang 2024-7-10 追加错误信息返回主控制页面的功能

    public Hp22mmSCManager(Context context) {
        mContext = context;
        mInkLevel = 0;
        mValid = false;
        mInitialized = false;
    }

    @Override
    public void init(Handler callback) {
        mCallback = callback;
        mCallback.sendEmptyMessage(SmartCardManager.MSG_SMARTCARD_INIT_SUCCESS);
        mInkLevel = MAX_PEN_INK_VOLUME / 2;
        mValid = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(Hp22mm.initHp22mm() != 0) {
// H.M.Wang 2024-7-10 追加错误信息返回主控制页面的功能
                    mCallback.sendEmptyMessage(MSG_HP22MM_ERROR);
// End of H.M.Wang 2024-7-10 追加错误信息返回主控制页面的功能
                    mValid = false;
                    try{Thread.sleep(3000);}catch(Exception e){}
                }
                mValid = true;
// H.M.Wang 2024-7-10 追加错误信息返回主控制页面的功能
                mCallback.obtainMessage(MSG_HP22MM_ERROR, "").sendToTarget();
// End of H.M.Wang 2024-7-10 追加错误信息返回主控制页面的功能
                mInitialized = true;
            }
        }).start();
    }

    @Override
    public boolean checkUID(int heads) {
        mCallback.sendEmptyMessage(SmartCardManager.MSG_SMARTCARD_CHECK_SUCCESS);
        return true;
    }

    @Override
    public float getLocalInk(int head) {
        return mInkLevel;
    }

    @Override
    public float getLocalInkPercentage(int head) {
        int usableVol = Hp22mm.getUsableVol();
        if(usableVol > 0)
            return 100.0f - 100.0f * Hp22mm.getConsumedVol() / Hp22mm.getUsableVol();
        else if(!mInitialized)
            return 50.0f;           // 在还没有初始化的情况下，返回100以防止报警
        else
            return 0.0f;
    }

    @Override
    public boolean isValid(int dev) {
        return mValid;
    }

    @Override
    public int getFeature(int device, int index) {
        if(index == 4) {
            // Voltage
            return SystemConfigFile.getInstance(mContext).getParam(25);
        } else if(index == 5) {
            // Pulse
            return SystemConfigFile.getInstance(mContext).getParam(27);
        }
        return 0;
    }

    @Override
    public void downLocal(int dev) {
        mInkLevel--;
    }

    @Override
    public void switchRfid(int i) {

    }

    @Override
    public void defaultInkForIgnoreRfid() {

    }

    @Override
    public float getMaxRatio(int dev) {
        return 1.0f;
    }

    public int startPrint() {
// H.M.Wang 2024-7-10 追加错误信息返回主控制页面的功能
        int ret = Hp22mm.startPrint();
        if(ret < 0) {
            mCallback.obtainMessage(MSG_HP22MM_ERROR, Hp22mm.getErrString()).sendToTarget();
        } else {
            mCallback.obtainMessage(MSG_HP22MM_ERROR, "").sendToTarget();
        }
// End of H.M.Wang 2024-7-10 追加错误信息返回主控制页面的功能
        return ret;
    }

    public int stopPrint() {
// H.M.Wang 2024-7-10 追加错误信息返回主控制页面的功能
        int ret = Hp22mm.stopPrint();
        if(ret < 0) {
            mCallback.obtainMessage(MSG_HP22MM_ERROR, Hp22mm.getErrString()).sendToTarget();
        } else {
            mCallback.obtainMessage(MSG_HP22MM_ERROR, "").sendToTarget();
        }
// End of H.M.Wang 2024-7-10 追加错误信息返回主控制页面的功能
        return ret;
    }
}
