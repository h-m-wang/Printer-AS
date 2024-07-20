package com.printer.phoneapp.Sockets;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
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
 * for non-BLE device
 */

public class NonBLEDriver extends BTDriver {
    private static final String TAG = NonBLEDriver.class.getSimpleName();

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
                            !isAddressExists(device.getAddress())) {
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

    public static BTDriver getInstance(Context ctx) {
        if(null == mBTDriver) {
            mBTDriver = new NonBLEDriver(ctx);
        }
        return mBTDriver;
    }

    protected NonBLEDriver(Context ctx) {
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
}