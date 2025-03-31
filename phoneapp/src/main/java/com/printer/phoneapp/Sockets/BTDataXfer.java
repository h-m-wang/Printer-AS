package com.printer.phoneapp.Sockets;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.UUID;
import java.util.concurrent.Executors;

public class BTDataXfer extends DataXfer {
    private static final String TAG = BTDataXfer.class.getSimpleName();
    private final String PRINTER_BT_UUID = "00001101-0000-1000-8000-00805F9B34FB";

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
        if(mConnectState == STATE_CONNECTING) return;

        mConnectState = STATE_CONNECTING;
        mSocketThread.execute(new Runnable() {
            @Override
            public void run() {
                try {
//                    mSocket = mDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString(PRINTER_BT_UUID));
                    mSocket = mDevice.createRfcommSocketToServiceRecord(UUID.fromString(PRINTER_BT_UUID));
                    if(null != mSocket) {
                        if (!mSocket.isConnected()) {
                            mSocket.connect();
                            Log.d(TAG, "Socket opened");
                        } else {
                            Log.d(TAG, "Socket already opened");
                        }
                        mConnectState = STATE_CONNECTED;
                        mNeedRecovery = true;
                        if(null != mOnDeviceConnectionListener) mOnDeviceConnectionListener.onConnected(mDevice);
                        while(mNeedRecovery) {
                            if(!mSocket.isConnected()) {
                                connect();
                                break;
                            }
                            try {Thread.sleep(3000);} catch(Exception e){}
                        }
                    } else {
                        mConnectState = STATE_DISCONNECTED;
                        if(null != mOnDeviceConnectionListener) mOnDeviceConnectionListener.onDisConnected();
                    }
                } catch (IOException e) {
                    mConnectState = STATE_DISCONNECTED;
                    if(null != mOnDeviceConnectionListener) mOnDeviceConnectionListener.onDisConnected();
                }
            }
        });
    }

    @Override
    public void disconnect() {
        try {
            mNeedRecovery = false;
            if(null != mSocket) {
                mSocket.close();
            }
        } catch (IOException e) {
        }
        mConnectState = STATE_DISCONNECTED;
        if(null != mOnDeviceConnectionListener) mOnDeviceConnectionListener.onDisConnected();
    }

    @Override
    public void sendString(String msg, OnDataXferListener l) {
        try {
            OutputStream os;

            Log.d(TAG, "Socket sendString [" + msg + "]");
//            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream()));
//            bw.write(msg);
//            bw.flush();
            os = mSocket.getOutputStream();
            os.write(msg.getBytes());
            os.write(new byte[]{0x0A});
            os.flush();
            l.onSent(msg);
            BufferedReader br = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
            l.onReceived(br.readLine());
        } catch (IOException e) {
            l.onFailed(e.getMessage());
        } catch (Exception e) {
            l.onFailed(e.getMessage());
        }
    }
}
