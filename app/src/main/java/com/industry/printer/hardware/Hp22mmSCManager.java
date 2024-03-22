package com.industry.printer.hardware;

import android.content.Context;
import android.os.Handler;

import com.industry.printer.FileFormat.SystemConfigFile;

public class Hp22mmSCManager implements IInkDevice {
    private static final String TAG = Hp22mmSCManager.class.getSimpleName();

    private final static int PEN_VS_BAG_RATIO           = 3;
    private static int MAX_BAG_INK_VOLUME         = 3150;
    private static int MAX_PEN_INK_VOLUME         = MAX_BAG_INK_VOLUME * PEN_VS_BAG_RATIO;

    private Context mContext;
    private Handler mCallback;
    private boolean mLibInited;
    private int mInkLevel;

    public Hp22mmSCManager(Context context) {
        mContext = context;
        mInkLevel = 0;
    }

    @Override
    public void init(Handler callback) {
        mCallback = callback;
        mCallback.sendEmptyMessage(SmartCardManager.MSG_SMARTCARD_INIT_SUCCESS);
        mInkLevel = MAX_PEN_INK_VOLUME / 2;
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
        return 100.0f * mInkLevel / MAX_PEN_INK_VOLUME;
    }

    @Override
    public boolean isValid(int dev) {
        return true;
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
}
