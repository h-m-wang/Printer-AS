package com.printer.phoneapp.Sockets;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import com.printer.phoneapp.Devices.ConnectDevice;

import java.util.concurrent.ExecutorService;

abstract public class DataXfer {
    private static final String TAG = DataXfer.class.getSimpleName();

    protected Context mContext;
    protected ExecutorService mSocketThread = null;

    public interface OnDeviceConnectionListener {
        public void onConnected(BluetoothDevice dev);
        public void onDisConnected();
    }
    protected OnDeviceConnectionListener mOnDeviceConnectionListener;

    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;

    public int mConnectState;
    protected boolean mNeedRecovery;

    public interface OnDataXferListener {
        public void onSent(byte[] sent);
        public void onSent(String sent);
        public void onReceived(byte[] recv);
        public void onReceived(String recv);
        public void onFailed(String errMsg);
    }
    protected OnDataXferListener mOnDataXferListener;

    public DataXfer(Context ctx) {
        mContext = ctx;
        mConnectState = STATE_DISCONNECTED;
        mNeedRecovery = false;
        mOnDeviceConnectionListener = null;
        mOnDataXferListener = null;
    }

    public void setDeviceConnectionListener(OnDeviceConnectionListener l) {
        mOnDeviceConnectionListener = l;
    }

    abstract public void connect();

    abstract public void disconnect();

    abstract public void sendString(final String msg, OnDataXferListener l);
}
