package com.industry.printer.hardware;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.os.Handler;

import com.industry.printer.FileFormat.SystemConfigFile;
import com.industry.printer.Utils.Configs;
import com.industry.printer.Utils.Debug;

public class N_RFIDManager extends RFIDManager implements IInkDevice {
    private static final String TAG = N_RFIDManager.class.getSimpleName();

    private static volatile N_RFIDManager mInstance = null;
    private List<N_RFIDDevice> mRfidDevices;
    private Handler mCallback;
    private Timer mTimer;
    private int mCurrent;

    public static int TOTAL_RFID_DEVICES = 8;

    public static final int MSG_RFID_INIT_SUCCESS = 101;
    public static final int MSG_RFID_INIT_FAIL = 102;
    public static final int MSG_RFID_WRITE_SUCCESS = 103;
    public static final int MSG_RFID_WRITE_FAIL = 104;
    public static final int MSG_RFID_READ_SUCCESS = 105;
    public static final int MSG_RFID_READ_FAIL = 106;
    public static final int MSG_RFID_INIT = 107;
    public static final int MSG_RFID_CHECK_FAIL = 108;
    public static final int MSG_RFID_CHECK_SUCCESS = 109;
    // H.M.Wang 2022-8-31 追加一个消息，显示提示不要带电更换墨盒
    public static final int MSG_RFID_CHECK_FAIL_INK_CHANGED = 110;
// End of H.M.Wang 2022-8-31 追加一个消息，显示提示不要带电更换墨盒

    public static N_RFIDManager getInstance(Context ctx) {
        if (mInstance == null) {
            synchronized (RFIDManager.class) {
                if (mInstance == null) {
                    Debug.d(TAG, "--->new N_RFIDManager");
                    mInstance = new N_RFIDManager(ctx);
                }
            }
        }
        return mInstance;
    }

    public N_RFIDManager(Context ctx) {
        super(ctx);
        SystemConfigFile configFile = SystemConfigFile.getInstance(ctx);

        TOTAL_RFID_DEVICES = configFile.getPNozzle().mHeads;
        TOTAL_RFID_DEVICES *= configFile.getHeadFactor();

        mRfidDevices = new ArrayList<N_RFIDDevice>();
        mTimer = new Timer();
        mCurrent = -1;
    }

    /** implement IInkDevice*/
    @Override
    public void init(final Handler callback) {
        mCallback = callback;
        Debug.d(TAG, "--->init");

        if (mRfidDevices.size() != TOTAL_RFID_DEVICES) {
            mRfidDevices.clear();
            for (int i = 0; i < TOTAL_RFID_DEVICES; i++) {
                N_RFIDDevice device = new N_RFIDDevice(i);
                mRfidDevices.add(device);
            }
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (N_RFIDManager.this) {
                    mCallback.sendEmptyMessageDelayed(MSG_RFID_READ_SUCCESS, 3000L);

                    while(true) {
                        boolean init_success = true;

                        for(int i=0; i<mRfidDevices.size(); i++) {
                            N_RFIDDevice device = mRfidDevices.get(i);
                            if(!device.isValid()) {
                                synchronized (ExtGpio.RFID_ACCESS_LOCK) {
                                    Debug.d(TAG, "Init RFID[" + i + "]");
                                    switchRfid(i);
                                    init_success = (device.init() && init_success);
                                }
                            }
                        }

                        if(init_success) {
                            mTimer.scheduleAtFixedRate(new TimerTask() {
                                @Override
                                public void run() {
                                    synchronized (N_RFIDManager.this) {
                                        for(int i=0; i<mRfidDevices.size(); i++) {
                                            N_RFIDDevice device = mRfidDevices.get(i);
                                            if(device.isValid()) {
                                                if(device.inkModified()) {
                                                    synchronized (ExtGpio.RFID_ACCESS_LOCK) {
                                                        switchRfid(i);
                                                        device.writeInkLevel();
                                                    }
                                                }
//                            Debug.d(TAG, "RFID[" + i + "] absent? " + device.checkCardAbsence());
//                                            } else {
//                                                Debug.d(TAG, "Initializing RFID[" + i + "]");
//                                                if(device.isValid()) continue;
//                                                device.init();
                                            }
                                        }
                                    }
                                }
                            }, 3000L, 3000L);

                            break;
                        }

                        try { Thread.sleep(1000); } catch(InterruptedException e){};
                    }
                }
            }
        }).start();
    }

    /** implement IInkDevice*/
    @Override
    public boolean checkUID(int heads) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (N_RFIDManager.this) {
                    for(int i=0; i<mRfidDevices.size(); i++) {
                        synchronized (ExtGpio.RFID_ACCESS_LOCK) {
                            switchRfid(i);
                            Debug.d(TAG, "Checking UID of RFID[" + i + "]");
                            N_RFIDDevice device = mRfidDevices.get(i);
                            int ret = device.checkUID();

                            if(ret == 0) {
                                mCallback.sendEmptyMessage(MSG_RFID_CHECK_FAIL);
                                return;
                            } else if(ret == -1) {
                                mCallback.sendEmptyMessage(MSG_RFID_CHECK_FAIL_INK_CHANGED);
                                return;
                            }
                        }
                    }
                    mCallback.sendEmptyMessage(MSG_RFID_CHECK_SUCCESS);
                }
            }
        }).start();

        return true;
    }

    /** implement IInkDevice*/
    @Override
    public void switchRfid(final int i) {
        ExtGpio.rfidSwitch(i);
        mCurrent = i;
        try { Thread.sleep(100); } catch (Exception e) {}
    }

    /** implement IInkDevice*/
    @Override
    public float getLocalInk(int dev) {
//        Debug.d(TAG, "---> enter getLocalInk()");

        if (dev >= mRfidDevices.size()) {
            return 0;
        }

        N_RFIDDevice device = mRfidDevices.get(dev);
        if (device == null) {
            return 0;
        }

        int max = device.getMax();
        if (max <= 0 && Configs.READING) {
// H.M.Wang 2020-2-25 修改max值
//            max = 370;
            max = 2000;
// End of H.M.Wang 2020-2-25 修改max值
        }

        float ink = device.getLocalInk();
        if (max <= 0) {
            return 0;
        } else if (max < ink) {
            return 100;
        }

        return ink;
    }

    @Override
    public float getLocalInkPercentage(int dev) {
//        Debug.d(TAG, "---> enter getLocalInkPercentage()");

        if (dev >= mRfidDevices.size()) {
            return 0;
        }

        N_RFIDDevice device = mRfidDevices.get(dev);
        if (device == null) {
            return 0;
        }
        int max = device.getMax();
        if (max <= 0 && Configs.READING) {
// H.M.Wang 2020-2-25 修改max值
//            max = 370;
            max = 2000;
// End of H.M.Wang 2020-2-25 修改max值
        }
        float ink = device.getLocalInk();
        if (max <= 0) {
            return 0;
        } else if (max < ink) {
            return 100;
        }

        return (ink*100/max);
    }

    /** implement IInkDevice*/
    @Override
    public void downLocal(int dev) {
        if (dev >= mRfidDevices.size()) {
            return ;
        }

        N_RFIDDevice device = mRfidDevices.get(dev);
        if (device == null) {
            return ;
        }

        device.down();
    }

    /** implement IInkDevice*/
    @Override
    public boolean isValid(int dev) {
        if (dev >= mRfidDevices.size()) {
            return false;
        }
        if (Configs.READING) {
            return true;
        }
        N_RFIDDevice device = mRfidDevices.get(dev);

        return device.isValid();
    }
/*
    public N_RFIDDevice getDevice(int index) {
        if (index >= mRfidDevices.size()) {
            return null;
        }
        return mRfidDevices.get(index);
    }
*/
    /** implement IInkDevice*/
    @Override
    public void defaultInkForIgnoreRfid() {
        Debug.e(TAG, "Function defaultInkForIgnoreRfid() has been deprecapted");
    }

// H.M.Wang 2023-12-3 修改锁值记录方法。增加一个mStep的传递方法
    @Override
    public float getMaxRatio(int dev) {
        if (dev >= mRfidDevices.size()) {
            return 1.0f;
        }
        N_RFIDDevice device = mRfidDevices.get(dev);

        return device.getMaxRatio();
    }
// End of H.M.Wang 2023-12-3 修改锁值记录方法。增加一个mStep的传递方法

    /**
     * 获取设备Feature信息
     * @param device
     * @param index
     * @return
     */

    @Override
    public int getFeature(int device, int index) {
        if(device >= mRfidDevices.size()) return 0;

        N_RFIDDevice dev = mRfidDevices.get(device);

        if (dev == null) return 0;

        return dev.getFeature(index);
    }
}
