package com.industry.printer.Bluetooth;

import com.industry.printer.FileFormat.SystemConfigFile;

/**
 * Created by hmwan on 2025/3/15.
 */

abstract public class BluetoothServer {
    private static final String TAG = BluetoothServer.class.getSimpleName();

    protected final static String SERVER_PREFIX_NAME = "Printer";
    protected boolean mInitialized;
    protected int mDeviceNo;
    protected String mServerName;

    public BluetoothServer() {
        mInitialized = false;
        mServerName = "";
        mDeviceNo = SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_LOCAL_ID);
    }

    abstract public void closeServer();
    abstract public void initServer(int devno);

    protected String createServerName(int devno) {
        String ble_name = "000" + devno;
        mServerName = SERVER_PREFIX_NAME + ble_name.substring(ble_name.length()-3);
        return mServerName;
    }

    public void paramsChanged(int enable, int devno) {
        if(enable == 0) {
            if(mInitialized) {
                closeServer();
            }
        } else {
            if(devno != mDeviceNo || !mInitialized) {    // 如果蓝牙设备号发生变化，或者还没有初始化，则执行初始化
                initServer(devno);
                if(mInitialized) {
                    mDeviceNo = devno;
                }
            }
        }
    }
}
