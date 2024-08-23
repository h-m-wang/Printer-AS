package com.printer.phoneapp.Sockets;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.Arrays;

/**
 * Created by hmwan on 2021/9/8.
 */

public class BLEDriver extends BTDriver {
    private static final String TAG = BLEDriver.class.getSimpleName();

    private BluetoothLeScanner mBLEScanner;

    public static BTDriver getInstance(Context ctx) {
        if(null == mBTDriver) {
            mBTDriver = new BLEDriver(ctx);
        }
        return mBTDriver;
    }

    private BLEDriver(Context ctx) {
        super(ctx);
        mBLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
    }

    // 接收搜索蓝牙设备的结果
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            mBluetoothAdapter.getRemoteDevice(device.getAddress());

            if (null != device.getName() &&
                !device.getName().isEmpty() &&
                !isAddressExists(device.getAddress())) {

                Log.d(TAG, "BLE Device found.");
                Log.d(TAG, "  Name = [" + device.getName() + "]");
                Log.d(TAG, "  Address = [" + device.getAddress() + "]");
                Log.d(TAG, "  Type = [" + device.getType() + "]");      // 可能无用
                if (null != device.getUuids()) {
                    for (ParcelUuid uuid : device.getUuids()) {
                        Log.d(TAG, "  UUID = [" + uuid.getUuid().toString() + "]");
                    }
                } else {
                    Log.d(TAG, "  UUID = [null]");
                }
                Log.d(TAG, "  Bonded = [" + device.getBondState() + "]");

                mFoundDevices.add(device);
                if (null != mOnDiscoveryListener) {
                    // 会送给搜索蓝牙设备功能的调用者，由调用者完成进一步的操作，如显示清单等
                    mOnDiscoveryListener.onDeviceFound(device);
                }
            }
        }
    };

    @Override
    public void stopDiscovery() {
        if (isEnabled()) {
            mBLEScanner.stopScan(mScanCallback);
            if (null != mOnDiscoveryListener) {
                mOnDiscoveryListener.onDiscoveryFinished();
            }
        }
    }

    @Override
    public boolean startDiscovery(OnDiscoveryListener l) {
        if (super.startDiscovery(l)) {
            mBLEScanner.startScan(mScanCallback);
            if (null != mOnDiscoveryListener) {
                mOnDiscoveryListener.onDiscoveryStarted();
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try{ Thread.sleep(30000);} catch(InterruptedException e){};
                    stopDiscovery();
                }
            }).start();

            return true;
        }
        return false;
    }
}