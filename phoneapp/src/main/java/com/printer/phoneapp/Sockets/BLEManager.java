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

public class BLEManager extends BluetoothManager {
    public static final String TAG = "BLE:" + BluetoothManager.TAG;

    private BluetoothLeScanner mBLEScanner;

    public static BluetoothManager getInstance(Context ctx) {
        if(null == mBluetoothManager) {
            mBluetoothManager = new BLEManager(ctx);
        }
        return mBluetoothManager;
    }

    private BLEManager(Context ctx) {
        super(ctx);
        mBLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();

            if (null != device.getName() &&
                !device.getName().isEmpty() &&
                !isAddressExists(device.getAddress()) &&
                !mConnectDeviceManager.isAddressExists(device.getAddress())) {

                Log.d(TAG, "BLE Device found.");
                Log.d(TAG, "  Name = [" + device.getName() + "]");
                Log.d(TAG, "  Address = [" + device.getAddress() + "]");
                Log.d(TAG, "  Type = [" + device.getType() + "]");      // 可能无用
                if (null != device.getUuids()) {
                    for (ParcelUuid uuid : device.getUuids()) {
                        Log.d(TAG, "  UUID = [" + uuid.getUuid() + "]");
                    }
                } else {
                    Log.d(TAG, "  UUID = [null]");
                }
                Log.d(TAG, "  Bonded = [" + device.getBondState() + "]");

                mFoundDevices.add(device);
                if (null != mOnDiscoveryListener) {
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

    BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if(newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();
                Log.d(TAG, "Connected to GATT server.");
            } else if(newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected from GATT server.");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if(status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services Discovered:");
                for(BluetoothGattService service : gatt.getServices()) {
                    Log.d(TAG, "[Service] Type: " + service.getType() + "; UUID: " + service.getUuid());
                    for(BluetoothGattCharacteristic charr : service.getCharacteristics()) {
                        Log.d(TAG, "\t[Characteristic] Properties: " + charr.getProperties() + "; UUID: " + charr.getUuid());
                        for(BluetoothGattDescriptor desc : charr.getDescriptors()) {
                            Log.d(TAG, "\t\t[Descriptor] UUID: " + desc.getUuid());
                            if(charr.getProperties() == BluetoothGattCharacteristic.PROPERTY_INDICATE ||
                                    charr.getProperties() == BluetoothGattCharacteristic.PROPERTY_NOTIFY) {
                                gatt.setCharacteristicNotification(charr, true);
                            }
                        }
                        if(charr.getUuid().toString().indexOf("c304") > 0) {
                            mWriteChar = charr;
//                            charr.setValue(new String("123456789012345678901234567890").getBytes());
//                            Log.d(TAG, "Launch Write: " + gatt.writeCharacteristic(charr));
                        }
                    }
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if(status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "[Characteristic Read]: " + characteristic.getUuid() + "\n" + Arrays.toString(characteristic.getValue()));
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.d(TAG, "[Characteristic Changed]: " + characteristic.getUuid() + "\n" + Arrays.toString(characteristic.getValue()));
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if(status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "[Characteristic Write]: " + characteristic.getUuid() + "\n" + Arrays.toString(characteristic.getValue()));
//                if(count++ < 10) {
//                    characteristic.setValue(new String("123456789012345678901234567890ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ").getBytes());
//                    Log.d(TAG, "Launch Write: " + gatt.writeCharacteristic(characteristic));
//                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            if(status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "[Descriptor Write]: " + descriptor.getUuid() + "\n" + Arrays.toString(descriptor.getValue()));
            } else {
                Log.d(TAG, "[Descriptor Write]: failed. " + status);
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            if(status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "[Descriptor Read]: " + descriptor.getUuid() + "\n" + Arrays.toString(descriptor.getValue()));
            } else {
                Log.d(TAG, "[Descriptor Read]: failed. " + status);
            }
        }
    };

    private BluetoothGattCharacteristic mWriteChar;

    @Override
    public void connectDevice(final BluetoothDevice device) {
        Log.d(TAG, "connectDevice.");
        BluetoothGatt gatt = device.connectGatt(mContext, true, mGattCallback);

        if(null != gatt) {
            if(gatt.connect()) {
                Log.d(TAG, device.getName() + " connected.");
            } else {
                Log.d(TAG, "Connection failed.");
            }
        } else {
            Log.d(TAG, "BluetoothGatt null.");
        }
    }

    @Override
    public void disconnectDevice(final BluetoothDevice device) {
        Log.d(TAG, "disconnectDevice.");
        BluetoothGatt gatt = device.connectGatt(mContext, true, mGattCallback);

        if(null != gatt) {
            gatt.disconnect();
            Log.d(TAG, device.getName() + " disconnected.");
        }
    }
}