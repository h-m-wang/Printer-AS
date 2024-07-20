package com.printer.phoneapp.Sockets;

import android.content.Context;
import android.util.Log;

import com.printer.phoneapp.Devices.ConnectDevice;

import java.util.concurrent.ExecutorService;

abstract public class DataXfer {
    private static final String TAG = DataXfer.class.getSimpleName();

    protected Context mContext;
    protected ExecutorService mSocketThread = null;

    public interface OnDeviceConnectionListener {
        public void onConnected();
        public void onDisConnected();
    }
    protected OnDeviceConnectionListener mOnDeviceConnectionListener;
    public boolean mConnected;

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
        mConnected = false;
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
