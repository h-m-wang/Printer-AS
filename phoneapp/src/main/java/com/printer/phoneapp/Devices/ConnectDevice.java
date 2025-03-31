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

    private String          mName;
    private String          mAddress;
    private int             mType;
    private DataXfer        mDataXfer;
    private String          mErrMsg = "";

    public ConnectDevice(Context ctx, BluetoothDevice dev) {
        mName = dev.getName();
        mAddress = dev.getAddress();
        mType = dev.getType();
        if(mType == BluetoothDevice.DEVICE_TYPE_LE) {
            mDataXfer = new BLEDataXfer(ctx, dev);
        } else {
            mDataXfer = new BTDataXfer(ctx, dev);
        }
    }

    public ConnectDevice(Context ctx, String name, String address, int type) {
        mName = name;
        mAddress = address;
        mType = type;   // -1: WIFI
        mErrMsg = "";
        mDataXfer = new WifiDataXfer(ctx, address);
    }

    public String getName() {
        return mName;
    }
    public void setName(String name) {
        mName = name;
    }

    public String getAddress() {
        return mAddress;
    }

    public int getType() {
        return mType;
    }

    public boolean isConnected() {
        return (mDataXfer.mConnectState == DataXfer.STATE_CONNECTED);
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
