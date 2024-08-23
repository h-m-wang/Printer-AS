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

import java.io.BufferedReader;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;
import java.util.logging.Handler;

public class BLEDataXfer extends DataXfer {
    private static final String TAG = BLEDataXfer.class.getSimpleName();

    private BluetoothDevice mDevice;
    //    private BluetoothGattCharacteristic mCharRead;
    private BluetoothGattCharacteristic mWriteChar;
    private BluetoothGatt mGatt;
    private byte[] mSB;

    public BLEDataXfer(Context ctx, BluetoothDevice device) {
        super(ctx);
        mDevice = device;
        mSB = null;
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
                mNeedRecovery = true;
                mGatt = gatt;
                if(null != mOnDeviceConnectionListener) mOnDeviceConnectionListener.onConnected(gatt.getDevice());
            } else if(newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, gatt.getDevice().getName() + " disconnected from GATT server.");
                mConnected = false;
                if(null != mOnDeviceConnectionListener) mOnDeviceConnectionListener.onDisConnected();
                final BluetoothGatt gatt_buf = gatt;
                if(mNeedRecovery) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while(!mConnected) {
                                gatt_buf.connect();
                                try{Thread.sleep(1000);} catch (Exception e) {}
                            }
                        }
                    }).start();
                }
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
                            }
                        }
                        if(charr.getUuid().toString().equalsIgnoreCase("0000c304-0000-1000-8000-00805f9b34fb")) {
                            mWriteChar = charr;
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
            Log.d(TAG, "[Characteristic Changed]: " + characteristic.getUuid() + "\n" + new String(characteristic.getValue()) + "\n" + Arrays.toString(characteristic.getValue()));
            // 接收数据的时候，需要将数据按字节拼接，最后转变为字符串，而不是将部分字节直接转换为字符串后拼接，因为汉字的UTF-8编码可能有多个字节，可能被在中间切断，这样生成汉字的时候会出现乱码
            byte[] buf = null;
            if(null != mSB) {
                buf = new byte[mSB.length + characteristic.getValue().length];
                System.arraycopy(mSB, 0, buf, 0, mSB.length);
                System.arraycopy(characteristic.getValue(), 0, buf, mSB.length, characteristic.getValue().length);
            } else {
                buf = new byte[characteristic.getValue().length];
                System.arraycopy(characteristic.getValue(), 0, buf, 0, characteristic.getValue().length);
            }
            mSB = buf;

            if(mSB[mSB.length-5] == '.' && mSB[mSB.length-4] == '.' && mSB[mSB.length-3] == '|' && mSB[mSB.length-2] == '.' && mSB[mSB.length-1] == '.' ) {
                mSB = Arrays.copyOf(mSB, mSB.length - 5);
                if(null != mOnDataXferListener) mOnDataXferListener.onReceived(new String(mSB));
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if(status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "[Characteristic Write]: " + characteristic.getUuid() + "\n" + new String(characteristic.getValue()) + "\n" + Arrays.toString(characteristic.getValue()));
                mSB = null;
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

    @Override
    public void connect() {
        Log.d(TAG, "Connect to BLE Server.");
        mGatt = mDevice.connectGatt(mContext, true, mGattCallback);

        if(null != mGatt) {
            if(mGatt.connect()) {
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

        if(null != mGatt) {
            mNeedRecovery = false;
            mGatt.disconnect();
            Log.d(TAG, mDevice.getName() + " disconnected.");
        }
    }

    public void sendString(final String msg, OnDataXferListener l) {
        Log.d(TAG, "Writting String = [" + msg + "]");

        mOnDataXferListener = l;

        if(null == mWriteChar) {
            if(null != mOnDataXferListener) mOnDataXferListener.onFailed("Writing channel not exist.");
        } else {
            mWriteChar.setValue((msg + "..|..").getBytes());
            if(null != mGatt) mGatt.writeCharacteristic(mWriteChar);
        }
    }
}
