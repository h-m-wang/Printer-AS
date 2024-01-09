package com.printer.phoneapp.Sockets;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by hmwan on 2021/9/8.
 */

public class BTManager extends BluetoothManager {
    public static final String TAG = "BT:" + BluetoothManager.TAG;

    private BroadcastReceiver mDiscoveryBroadcaster = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            Log.d(TAG, "Action: " + action);

            switch (action) {
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    Log.d(TAG, "Discovery started.");
                    mFoundDevices.clear();
                    if(null != mOnDiscoveryListener) {
                        mOnDiscoveryListener.onDiscoveryStarted();
                    };
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    Log.d(TAG, "Discovery finished.");
                    if(null != mOnDiscoveryListener) {
                        mOnDiscoveryListener.onDiscoveryFinished();
                    };
                    unregisterDiscoveryBroadcaster();
                    break;
                case BluetoothDevice.ACTION_FOUND:
                    Log.d(TAG, "Device found.");
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.d(TAG, "  Name = [" + device.getName() + "]");
                    Log.d(TAG, "  Address = [" + device.getAddress() + "]");
                    Log.d(TAG, "  Type = [" + device.getType() + "]");      // 可能无用
                    if(null != device.getUuids()) {
                        for(ParcelUuid uuid : device.getUuids()) {
                            Log.d(TAG, "  UUID = [" + uuid.getUuid() + "]");
                        }
                    } else {
                        Log.d(TAG, "  UUID = [null]");
                    }
                    Log.d(TAG, "  Bonded = [" + device.getBondState() + "]");
                    BluetoothClass bClass = intent.getParcelableExtra(BluetoothDevice.EXTRA_CLASS);
                    Log.d(TAG, "  Class = [" + bClass.getDeviceClass() + "]");
                    if(null != device.getName() &&
                            !device.getName().isEmpty() &&
                            !isAddressExists(device.getAddress()) &&
                            !mConnectDeviceManager.isAddressExists(device.getAddress())) {
                        mFoundDevices.add(device);
                        if(null != mOnDiscoveryListener) {
                            mOnDiscoveryListener.onDeviceFound(device);
                        };
                    }
                    break;
                case BluetoothDevice.ACTION_UUID:   // 这个只有是绑定了的设备才能够获得
                    BluetoothDevice dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.d(TAG, "UUIDs of Device [" + (null == dev ? "null" : dev.getName()) + "]");
                    Parcelable[] uuids = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
                    if(null != uuids) {
                        for(Parcelable uuid : uuids) {
                            Log.d(TAG, "  UUID = [" + uuid.toString() + "]");
                        }
                    } else {
                        Log.d(TAG, "  UUID = [null]");
                    }
                    break;
                case BluetoothDevice.ACTION_PAIRING_REQUEST:
                    dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.d(TAG, "ACTION_PAIRING_REQUEST of Device [" + (null == dev ? "null" : dev.getName()) + "]");
                    break;
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                    dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.d(TAG, "ACTION_BOND_STATE_CHANGED of Device [" + (null == dev ? "null" : dev.getName()) + "]");
                    Log.d(TAG, "Previous Bond state: " + intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.BOND_NONE));
                    Log.d(TAG, "Bond state: " + intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE));
                    break;
            }
        }
    };

    private boolean mDiscoveryReceiverRegisterred = false;

    private void registerDiscoveryBroadcaster() {
        try {
            if(null != mContext) {
                IntentFilter filter = new IntentFilter();
                filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
                filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                filter.addAction(BluetoothDevice.ACTION_FOUND);
                filter.addAction(BluetoothDevice.ACTION_UUID);
                filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
                filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
                mContext.registerReceiver(mDiscoveryBroadcaster, filter);
                mDiscoveryReceiverRegisterred = true;
            }
        } catch (Exception e) {
            Log.e(TAG, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private void unregisterDiscoveryBroadcaster() {
        try {
            if(null != mContext && mDiscoveryReceiverRegisterred) {
                mDiscoveryReceiverRegisterred = false;
                mContext.unregisterReceiver(mDiscoveryBroadcaster);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    public static BluetoothManager getInstance(Context ctx) {
        if(null == mBluetoothManager) {
            mBluetoothManager = new BTManager(ctx);
        }
        return mBluetoothManager;
    }

    protected BTManager(Context ctx) {
        super(ctx);
    }

    protected boolean isAddressExists(String address) {
        for(BluetoothDevice dev : mFoundDevices) {
            if(dev.getAddress().equalsIgnoreCase(address)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void stopDiscovery() {
        if(isEnabled() && mBluetoothAdapter.isDiscovering()) {
            mOnDiscoveryListener = null;
            unregisterDiscoveryBroadcaster();
            mBluetoothAdapter.cancelDiscovery();
        }
    }

    @Override
    public boolean startDiscovery(OnDiscoveryListener l) {
        if(super.startDiscovery(l)) {
            registerDiscoveryBroadcaster();
            return mBluetoothAdapter.startDiscovery();
        }
        return false;
    }

    @Override
    public void connectDevice(final BluetoothDevice device) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if(!isSupported()) {
                        Log.e(TAG, "Not supported!");
                        return;
                    }

                    //        if(!isEnabled()) {
                    //            enableBluetooth();
                    //            while(mEnabling) {try{Thread.sleep(100);}catch(Exception e){}};
                    //        }

                    if(!isEnabled()) {
                        Log.e(TAG, "Not enabled!");
                        return;
                    }
                    Log.d(TAG, "Start-1004");
//                        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice("F4:4E:FD:14:63:66");  // SANSUI 耳机
//                        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice("D0:9F:74:B1:ED:F9");    // 蓝牙鼠标
                    Log.d(TAG, "Device got " + device.getName());
                    Log.d(TAG, "Device Class " + device.getBluetoothClass().getDeviceClass());
                    Log.d(TAG, "Device UUID " + device.getUuids());

                    // 这里调用配对，配对成功后，系统的蓝牙会自动连接这个蓝牙设备（因为是音乐播放器，已有的播放器会主动连接），因此导致我们后续的连接失败（因为蓝牙只支持一个连接）
//                        Method m1 = device.getClass().getMethod("createBond");
//                        Log.d(TAG, "Bond: " + m1.invoke(device));

                    // 直接获得socket(0000111e-0000-1000-8000-00805f9b34fb)成功，无论是否配对，但是其他的都会失败，无论配对与否，连接与否
                    // 如果有配对，Method的方法可以成功，如果没有配对，会提示配对，如果不配对会失败，配对会成功。（但是出过pin不匹配错误而失败）
                    // 如果已有其他设备连接，则都会失败；因为只支持一个连接
// 蓝牙鼠标
//
//                        BluetoothSocket socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001812-0000-1000-8000-00805f9b34fb"));
// 已配对未连接状态下，不成功                        BluetoothSocket socket = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00001812-0000-1000-8000-00805f9b34fb"));
// 已配对未连接状态下，不成功                      Method m = device.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
//                        BluetoothSocket socket = (BluetoothSocket)m.invoke(device, 1);

// SANSUI 耳机。
// 这个失败。出现Pin不匹配的错误。不配对也可以执行到这里                        BluetoothSocket socket = device.createRfcommSocketToServiceRecord(UUID.fromString("0000111e-0000-1000-8000-00805f9b34fb"));
// 这个成功。即使没有配对也可以链接成功，而不提示配对
                    BluetoothSocket socket = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString("0000c300-0000-1000-8000-00805f9b34fb"));
//                    BluetoothSocket socket = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString("0000111e-0000-1000-8000-00805f9b34fb"));
// 这个不成功                        BluetoothSocket socket = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString("0000110e-0000-1000-8000-00805f9b34fb"));
// 这个不成功                        BluetoothSocket socket = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString("0000110b-0000-1000-8000-00805f9b34fb"));
// 这个不成功                        BluetoothSocket socket = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00000000-0000-1000-8000-00805f9b34fb"));
// 会提示配对                       Method m = device.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
//                        BluetoothSocket socket = (BluetoothSocket)m.invoke(device, 1);

                    Log.d(TAG, "Socket created " + socket.toString());
                    if(null != socket && !socket.isConnected()) {
                        Log.d(TAG, "Connecting...");
                        stopDiscovery();
                        socket.connect();
                        Log.d(TAG, "Socket connected: " + socket.isConnected());
                        if(null != socket) {
                            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            String is = br.readLine();      // 首次从耳机收到[AT+BRSF=639]
                            Log.d(TAG, "Socket Received: [" + is + "]");

                            while(true) {
                                OutputStream os = socket.getOutputStream();
                                os.write(new byte[]{0x0D});
                                os.write(new byte[]{0x0A});
                                os.write("+BRSF:639".getBytes());
                                os.write(new byte[]{0x0D});
                                os.write(new byte[]{0x0A});
                                os.flush();

                                is = br.readLine();
                                Log.d(TAG, "Socket Received: [" + is + "]");
                            }

//                                09-09 16:56:36.054 30112-30502/com.printer.phoneapp D/BluetoothManager: Socket Received: [AT+BRSF=639]
//                                发送 <CR><LF>+BRSF:639<CR><LF>
//                                09-09 16:56:42.760 30112-30502/com.printer.phoneapp D/BluetoothManager: Socket Received: [AT+CIND=?]
                        }

                    }
                } catch(Exception e) {
                    Log.e(TAG, e.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }
        }).start();
    }
}