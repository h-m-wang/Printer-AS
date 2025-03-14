package com.industry.printer.Bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

import com.industry.printer.FileFormat.SystemConfigFile;
import com.industry.printer.Utils.Debug;
import com.industry.printer.hardware.Hp22mmSCManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class BluetoothServer {
    private static final String TAG = BluetoothServer.class.getSimpleName();

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothServerSocket serverSocket;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public BluetoothServer() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void startServer() {
        if (bluetoothAdapter == null) {
            Debug.e(TAG, "蓝牙不可用. " + bluetoothAdapter);
            return;
        }

        if(!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
            Debug.e(TAG, "蓝牙未启用. " + bluetoothAdapter.isEnabled());
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        if(!bluetoothAdapter.isEnabled()) {
                            bluetoothAdapter.enable();
                            Debug.e(TAG, "蓝牙未启用. " + bluetoothAdapter.isEnabled());
                        }
                        bluetoothAdapter.setName("Printer" + SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_LOCAL_ID));
                        serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("MyBluetoothServer", MY_UUID);
                        Debug.d(TAG, "服务器已启动，等待客户端连接...");

                        BluetoothSocket socket = serverSocket.accept();
                        Debug.d(TAG, "客户端已连接: " + socket.getRemoteDevice().getName());

                        manageConnectedSocket(socket);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (serverSocket != null) {
                                serverSocket.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    try{Thread.sleep(3000);}catch(Exception e){}
                }
            }
        }).start();
    }

    private void manageConnectedSocket(BluetoothSocket socket) {
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();

            byte[] buffer = new byte[1024];
            int bytes;
            while (true) {
                bytes = inputStream.read(buffer);
                String receivedMessage = new String(buffer, 0, bytes);
                Debug.d(TAG, "收到消息: " + receivedMessage);

                String response = "服务器已收到: " + receivedMessage;
                outputStream.write(response.getBytes());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void stopServer() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

