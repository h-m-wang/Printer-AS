package com.printer.phoneapp.Sockets;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.Executors;

public class BTDataXfer extends DataXfer {
    private static final String TAG = BTDataXfer.class.getSimpleName();
    private final String PRINTER_BT_UUID = "0000C304-0000-1000-8000-00805f9b34fb";

    private BluetoothDevice mDevice;
    private BluetoothSocket mSocket;

    public BTDataXfer(Context ctx, BluetoothDevice device) {
        super(ctx);
        mDevice = device;
        mSocket = null;
        mSocketThread = Executors.newCachedThreadPool();
    }

    @Override
    public void connect() {
        mSocketThread.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    mSocket = mDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString(PRINTER_BT_UUID));
                    if(null != mSocket) {
                        if (!mSocket.isConnected()) {
                            mSocket.connect();
                            Log.d(TAG, "Socket opened");
                        } else {
                            Log.d(TAG, "Socket already opened");
                        }
                        mConnected = true;
                        if(null != mOnDeviceConnectionListener) mOnDeviceConnectionListener.onConnected();
                    } else {
                        mConnected = false;
                        if(null != mOnDeviceConnectionListener) mOnDeviceConnectionListener.onDisConnected();
                    }
                } catch (IOException e) {
                    mConnected = false;
                    if(null != mOnDeviceConnectionListener) mOnDeviceConnectionListener.onDisConnected();
                }
            }
        });
    }

    @Override
    public void disconnect() {
        try {
            if(null != mSocket) {
                mSocket.close();
            }
        } catch (IOException e) {
        }
        if(null != mOnDeviceConnectionListener) mOnDeviceConnectionListener.onDisConnected();
    }

    @Override
    public void sendString(String msg, OnDataXferListener l) {
        OutputStream os;

        try {
            Log.d(TAG, "Socket sendString [" + msg + "]");
            os = mSocket.getOutputStream();
            os.write(msg.getBytes());
            os.write(new byte[]{0x0A});
            os.flush();
            l.onSent(msg.getBytes());
        } catch (IOException e) {
            l.onFailed(e.getMessage());
        } catch (Exception e) {
            l.onFailed(e.getMessage());
        }
    }
}
