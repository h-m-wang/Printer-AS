package com.printer.phoneapp.Devices;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.printer.phoneapp.Sockets.BLEDataXfer;
import com.printer.phoneapp.Sockets.BTDataXfer;
import com.printer.phoneapp.Sockets.DataXfer;
import com.printer.phoneapp.Sockets.WifiDataXfer;

/**
 * Created by hmwan on 2021/9/7.
 */

public class ConnectDevice {
    private static final String TAG = ConnectDevice.class.getSimpleName();

    public static final int DEVICE_TYPE_UNKNOWN = 0;
    public static final int DEVICE_TYPE_WIFI = 1;
    public static final int DEVICE_TYPE_BT = 2;
    public static final int DEVICE_TYPE_BLE = 3;

    private String          mName;
    private String          mAddress;
    private int             mType;
    private DataXfer        mDataXfer;
    private String          mErrMsg = "";

    public ConnectDevice(Context ctx, BluetoothDevice dev, int type) {
        mName = dev.getName();
        mAddress = dev.getAddress();
        mType = type;
        if(mType == DEVICE_TYPE_BLE) {
            mDataXfer = new BLEDataXfer(ctx, dev);
        } else {
            mDataXfer = new BTDataXfer(ctx, dev);
        }
    }

    public ConnectDevice(Context ctx, String name, String address, int type) {
        mName = name;
        mAddress = address;
        mType = type;
        mErrMsg = "";
        mDataXfer = new WifiDataXfer(ctx, address);
    }

    public String getName() {
        return mName;
    }

    public String getAddress() {
        return mAddress;
    }

    public int getType() {
        return mType;
    }

    public boolean isConnected() {
        return mDataXfer.mConnected;
    }

    public void setErrMsg(String errMsg) {
        mErrMsg = errMsg;
    }

    public String getErrMsg() {
        return mErrMsg;
    }

    public void connect(DataXfer.OnDeviceConnectionListener l) {
        mDataXfer.setDeviceConnectionListener(l);
        mDataXfer.connect();
    }

    public void disconnect() {
        mDataXfer.disconnect();
    }

    public void sendString(String msg, DataXfer.OnDataXferListener l) {
        mDataXfer.sendString(msg, l);
    }
}
