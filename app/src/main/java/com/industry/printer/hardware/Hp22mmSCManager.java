package com.industry.printer.hardware;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.industry.printer.ControlTabActivity;
import com.industry.printer.FileFormat.SystemConfigFile;
import com.industry.printer.PHeader.PrinterNozzle;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.StringUtil;

import java.util.Vector;

public class Hp22mmSCManager implements IInkDevice {
    private static final String TAG = Hp22mmSCManager.class.getSimpleName();

    private final static int PEN_VS_BAG_RATIO           = 3;
// H.M.Wang 2024-12-11 墨水最大值的定位原则：由于根据实验，4列同时打印，打印200次的时候，使用了全部775ml中的10ml，因此，最大值设置为15500会与实际情况同步。同时考虑到可能会有1列单独，2列，3列打印的情况，因此将最大值按1列打印为标准设置，乘以4
// getLocalInk的时候，按着读取的值除以列数，downLocal的时候，按列数减记次数，并且写入OEM_RW
    private static int MAX_BAG_INK_VOLUME_MAXIMUM       = 15500 * 4;
//    private static int MAX_BAG_INK_VOLUME         = 3150;
//    private static int MAX_PEN_INK_VOLUME         = MAX_BAG_INK_VOLUME * PEN_VS_BAG_RATIO;
// End of H.M.Wang 2024-12-11 墨水最大值

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

// H.M.Wang 2024-11-12 追加一个测试页面启动的标识，如果测试页面启动了，就暂停本类中的守护线程运行
    public static final Object LockObj = new Object();
// End of H.M.Wang 2024-11-12 追加一个测试页面启动的标识，如果测试页面启动了，就暂停本类中的守护线程运行

    public Hp22mmSCManager(Context context) {
        mContext = context;
        mInkLevel = -1;
        mValid = true;
        mInitialized = false;
    }

    @Override
    public void init(Handler callback) {
        mCallback = callback;
        mCallback.sendEmptyMessage(SmartCardManager.MSG_SMARTCARD_INIT_SUCCESS);
//        mInkLevel = MAX_BAG_INK_VOLUME_MAXIMUM / 2;
        mValid = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
// H.M.Wang 2024-11-10
                while(true) {
                    synchronized (LockObj) {
                        if (!mInitialized) {
                            // 如果还没有初始化，则尝试初始化。如果失败，则在主页面显示错误，mValid=false会导致所知显示红色，并且beep报警音，睡2秒+1秒再试
                            if (Hp22mm.initHp22mm() != 0) {
                                mCallback.obtainMessage(MSG_HP22MM_ERROR, Hp22mm.getErrString()).sendToTarget();
                                mValid = false;
                                try {
                                    Thread.sleep(2000);
                                } catch (Exception e) {
                                }
                            } else {
                                mValid = true;
                                mCallback.obtainMessage(MSG_HP22MM_ERROR, "").sendToTarget();
                                mInitialized = true;
                            }
                        } else {
                            // 如果初始化成功，则每个1秒获取底层的错误信息，如果有错误，则报错。如果没有，则恢复正常
                            String errStr = Hp22mm.getErrString();
                            if (StringUtil.isEmpty(errStr)) {
                                mValid = true;
                                mCallback.obtainMessage(MSG_HP22MM_ERROR, "").sendToTarget();
                            } else {
                                mValid = false;
                                mCallback.obtainMessage(MSG_HP22MM_ERROR, errStr).sendToTarget();
                            }
                        }
                    }
                    try {Thread.sleep(1000);} catch (Exception e) {}
                }
/*                while(Hp22mm.initHp22mm() != 0) {
// H.M.Wang 2024-7-10 追加错误信息返回主控制页面的功能
                    mCallback.obtainMessage(MSG_HP22MM_ERROR, Hp22mm.getErrString()).sendToTarget();
// End of H.M.Wang 2024-7-10 追加错误信息返回主控制页面的功能
                    mValid = false;
                    try{Thread.sleep(3000);}catch(Exception e){}
                }
                mValid = true;
// H.M.Wang 2024-7-10 追加错误信息返回主控制页面的功能
                mCallback.obtainMessage(MSG_HP22MM_ERROR, "").sendToTarget();
// End of H.M.Wang 2024-7-10 追加错误信息返回主控制页面的功能
                mInitialized = true;
*/
// End of H.M.Wang 2024-11-10
            }
        }).start();
    }

    @Override
    public boolean checkUID(int heads) {
        mCallback.sendEmptyMessage(SmartCardManager.MSG_SMARTCARD_CHECK_SUCCESS);
        return true;
    }

    private int getSlotCount() {
        int slotCount = 1;
        int ns = SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_22MM_NOZZLE_SEL);
        if(ns == 0x00 || ns == 0x01 || ns == 0x02 || ns == 0x04 || ns == 0x08) {
            slotCount = 1;
        } else
        if(ns == 0x03 || ns == 0x05 || ns == 0x06 || ns == 0x09 || ns == 0x0A || ns == 0x0C) {
            slotCount = 2;
        } else
        if(ns == 0x07 || ns == 0x0B || ns == 0x0D || ns == 0x0E) {
            slotCount = 3;
        } else
        if(ns == 0x0F) {
            slotCount = 4;
        }
        return slotCount;
    }

    @Override
    public float getLocalInk(int head) {
// H.M.Wang 2024-12-10 从SC的OEM中读取当前值
        if(mInitialized && mInkLevel == -1) {
            int level = Hp22mm.getLocalInk();
            if(level == -1) {
                mInkLevel = -1;
                mValid = false;
            } else {
                mInkLevel = MAX_BAG_INK_VOLUME_MAXIMUM - level;
                mValid = true;
            }
        }
// End of H.M.Wang 2024-12-10 从SC的OEM中读取当前值
// H.M.Wang 2024-12-11 根据列数调整返回值
        if(mInkLevel != -1) {
            return 1.0f * mInkLevel / getSlotCount();
        }
// End of H.M.Wang 2024-12-11 根据列数调整返回值
        return mInkLevel;
    }

    @Override
    public float getLocalInkPercentage(int head) {
// H.M.Wang 2024-12-10 从SC的OEM中读取当前值
//        int usableVol = Hp22mm.getUsableVol();
//        if(usableVol > 0)
//            return 100.0f - 100.0f * Hp22mm.getConsumedVol() / Hp22mm.getUsableVol();
        if(mInkLevel >= 0) {
            float ret = (100.0f * mInkLevel / MAX_BAG_INK_VOLUME_MAXIMUM) + 0.01f;
            return (ret > 100.0f ? 100.0f : ret);
// End of H.M.Wang 2024-12-10 从SC的OEM中读取当前值
        } else if(!mInitialized)
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
        if(mInkLevel > 0) {
// H.M.Wang 2024-12-11 根据列数调整减记值
            int c = getSlotCount();
            mInkLevel -= c;
            if(mInitialized) Hp22mm.downLocal(c);
// End of H.M.Wang 2024-12-11 根据列数调整减记值
        }
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
// H.M.Wang 2024-9-26 取消开始打印时在pd_power_on, pd_power_on改在初始化阶段完成，同时开启打印件事线程。并且判断Hp22mm类是否初始化成功，如果未成功则返回失败
/*
// H.M.Wang 2024-7-10 追加错误信息返回主控制页面的功能
        int ret = Hp22mm.startPrint();
        if(ret < 0) {
            mCallback.obtainMessage(MSG_HP22MM_ERROR, Hp22mm.getErrString()).sendToTarget();
        } else {
            mCallback.obtainMessage(MSG_HP22MM_ERROR, "").sendToTarget();
        }
// End of H.M.Wang 2024-7-10 追加错误信息返回主控制页面的功能
        return ret;
*/
    if(mInitialized) return 0; else return -1;
// End of H.M.Wang 2024-9-26 取消开始打印时在pd_power_on, 并且判断Hp22mm类是否初始化成功，如果未成功则返回失败
    }

    public int stopPrint() {
// H.M.Wang 2024-9-26 取消停止打印时pd_power_off，也不关闭打印监视线程（在so里面）
/*
// H.M.Wang 2024-7-10 追加错误信息返回主控制页面的功能
        int ret = Hp22mm.stopPrint();
        if(ret < 0) {
            mCallback.obtainMessage(MSG_HP22MM_ERROR, Hp22mm.getErrString()).sendToTarget();
        } else {
            mCallback.obtainMessage(MSG_HP22MM_ERROR, "").sendToTarget();
        }
// End of H.M.Wang 2024-7-10 追加错误信息返回主控制页面的功能
        return ret;
*/
        return 0;
// End of H.M.Wang 2024-9-26 取消停止打印时pd_power_off，也不关闭打印监视线程（在so里面）
    }
}
