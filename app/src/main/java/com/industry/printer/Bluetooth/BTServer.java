package com.industry.printer.Bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.StreamTransport;
import com.industry.printer.pccommand.PCCommandHandler;
import com.industry.printer.pccommand.PCCommandManager;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Created by hmwan on 2025/3/15.
 * 通过U盘RTL8723DU模块实现蓝牙服务器。
 */

public class BTServer extends BluetoothServer {
    private static String TAG = BTServer.class.getSimpleName();

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothServerSocket mServerSocket;
    private BluetoothSocket mClientSocket;
    private StreamTransport mStreamTransport;
    private Thread mThread;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    protected BTServer() {
        super();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mServerSocket = null;
        mClientSocket = null;
        mStreamTransport = null;
        mThread = null;
    }

    private void setServerDiscoverableConnectable() {
        try {
            // 获取 BluetoothAdapter 的 Class 对象
            Class<?> bluetoothAdapterClass = BluetoothAdapter.class;

            // 获取 setScanMode 方法
            Method setScanModeMethod = bluetoothAdapterClass.getDeclaredMethod("setScanMode", int.class, int.class);

            // 设置方法为可访问
            setScanModeMethod.setAccessible(true);

            // 调用 setScanMode 方法
            boolean result = (Boolean) setScanModeMethod.invoke(BluetoothAdapter.getDefaultAdapter(), BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, 300);

//            Debug.d(TAG, "setScanMode 调用结果: " + result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void closeServer() {
        mInitialized = false;

        try {
            if(mClientSocket != null) {
                mClientSocket.close();
                mClientSocket = null;
            }
            if(mServerSocket != null) {
                mServerSocket.close();
                mServerSocket = null;
            }
        } catch(IOException e) {
            e.printStackTrace();
        }

        if(null != PCCommandManager.getInstance()) {
            if(PCCommandManager.getInstance().getBLEHandler() != null) {
                PCCommandManager.getInstance().getBLEHandler().close();
            }
            PCCommandManager.getInstance().addBLEHandler(null);
        }
        Debug.d(TAG, "BTServer Closed");
    }

    @Override
    public void initServer(int devno) {
        if (mBluetoothAdapter == null) {
            Debug.e(TAG, "蓝牙不可用. " + mBluetoothAdapter);
            return;
        }

        createServerName(devno);
        closeServer();
        if(!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        }

        if(mThread == null) {
            mThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    mInitialized = false;
                    while(true) {
                        try {
                            Thread.sleep(1000);
                            if(!mBluetoothAdapter.isEnabled()) {
                                continue;
                            }

                            PCCommandManager pcCM = PCCommandManager.getInstance();
                            if(null == pcCM) continue;
                            PCCommandHandler pcCH = pcCM.getBLEHandler();
                            if(pcCH != null && pcCH.isWorking()) continue;

                            Debug.d(TAG, "BTServer Starting...");
                            setServerDiscoverableConnectable();
                            mBluetoothAdapter.setName(mServerName);
                            mServerSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("MyBluetoothServer", MY_UUID);
                            Debug.d(TAG, "服务器已启动，等待客户端连接...");

                            mInitialized = true;

                            mClientSocket = mServerSocket.accept();
                            Debug.d(TAG, "客户端已连接: " + mClientSocket.getRemoteDevice().getName());

                            mStreamTransport = new StreamTransport(mClientSocket.getInputStream(), mClientSocket.getOutputStream());
                            pcCM.addBLEHandler(mStreamTransport);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                if (mServerSocket != null) {
                                    mServerSocket.close();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });
            mThread.start();
        }
    }
}
