package com.printer.phoneapp.Sockets;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

public class WifiDataXfer extends DataXfer {
    private static final String TAG = WifiDataXfer.class.getSimpleName();

    private String mIPAddress;

    public WifiDataXfer(Context ctx, String addr) {
        super(ctx);
        mIPAddress = addr;
    }

    @Override
    public void connect() {

    }

    @Override
    public void disconnect() {

    }

    @Override
    public void sendString(String msg, OnDataXferListener l) {

    }
}
