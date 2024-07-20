package com.printer.phoneapp.Sockets;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.util.Arrays;
import java.util.UUID;

public class BLEDataXfer extends DataXfer {
    private static final String TAG = BLEDataXfer.class.getSimpleName();

    private BluetoothDevice mDevice;
    //    private BluetoothGattCharacteristic mCharRead;
    private BluetoothGattCharacteristic mWriteChar;

    public BLEDataXfer(Context ctx, BluetoothDevice device) {
        super(ctx);
        mDevice = device;
    }

    // 连接或断开蓝牙设备时的回调接口
    BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if(newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();
                Log.d(TAG, gatt.getDevice().getName() + " connected to GATT server.");
                mConnected = true;
                if(null != mOnDeviceConnectionListener) mOnDeviceConnectionListener.onConnected();
            } else if(newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, gatt.getDevice().getName() + " disconnected from GATT server.");
                mConnected = false;
                if(null != mOnDeviceConnectionListener) mOnDeviceConnectionListener.onDisConnected();
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
                        Log.d(TAG, "\t[Characteristic] Properties: " + BTDriver.getPropString(charr.getProperties()) + "; UUID: " + charr.getUuid());
                        for(BluetoothGattDescriptor desc : charr.getDescriptors()) {
                            Log.d(TAG, "\t\t[Descriptor] UUID: " + desc.getUuid());
                            if(charr.getProperties() == BluetoothGattCharacteristic.PROPERTY_INDICATE) {
//                                对于INDICATE：
                                if(charr.getUuid().toString().equalsIgnoreCase("0000c306-0000-1000-8000-00805f9b34fb")) {
                                    gatt.setCharacteristicNotification(charr, true);
                                    desc.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                                    Log.d(TAG, "ENABLE_INDICATION_VALUE: " + gatt.writeDescriptor(desc));
                                }
//                            } else if(charr.getProperties() == BluetoothGattCharacteristic.PROPERTY_NOTIFY) {
//                                对于NOTIFY：
//                                gatt.setCharacteristicNotification(charr, true);
//                                desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//                                Log.d(TAG, "ENABLE_NOTIFICATION_VALUE: " + gatt.writeDescriptor(desc));
                            }
                        }
                        if(charr.getUuid().toString().equalsIgnoreCase("0000c304-0000-1000-8000-00805f9b34fb")) {
                            mWriteChar = charr;
//                            mWriteChar.setValue(new String("123456789012345678901234567890").getBytes());
//                            Log.d(TAG, "Launch Write: " + gatt.writeCharacteristic(mWriteChar));
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
            if(null != mOnDataXferListener) mOnDataXferListener.onReceived(new String(characteristic.getValue()));
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if(status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "[Characteristic Write]: " + characteristic.getUuid() + "\n" + Arrays.toString(characteristic.getValue()));
                if(null != mOnDataXferListener) mOnDataXferListener.onSent(new String(characteristic.getValue()));
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

    BluetoothGatt gatt;

    @Override
    public void connect() {
        Log.d(TAG, "Connect to BLE Server.");
        gatt = mDevice.connectGatt(mContext, true, mGattCallback);

        if(null != gatt) {
            if(gatt.connect()) {
                Log.d(TAG, mDevice.getName() + " connected.");
            } else {
                Log.d(TAG, "Connection failed.");
            }
        } else {
            Log.d(TAG, "BluetoothGatt null.");
        }
    }

    @Override
    public void disconnect() {
        Log.d(TAG, "Disconnect from BLE Server.");
//        BluetoothGatt gatt = mDevice.connectGatt(mContext, true, mGattCallback);

        if(null != gatt) {
            gatt.disconnect();
            Log.d(TAG, mDevice.getName() + " disconnected.");
        }
    }

    public void sendString(final String msg, OnDataXferListener l) {
        Log.d(TAG, "Writting String = [" + msg + "]");

        mOnDataXferListener = l;

        if(null == mWriteChar) {
            if(null != mOnDataXferListener) mOnDataXferListener.onFailed("Writting channel not exist.");
        } else {
            mWriteChar.setValue(msg.getBytes());
            if(null != gatt) gatt.writeCharacteristic(mWriteChar);
        }
    }
}
