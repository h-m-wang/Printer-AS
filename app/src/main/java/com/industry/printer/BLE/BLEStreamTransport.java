package com.industry.printer.BLE;

import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.StreamTransport;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;

@Deprecated
public class BLEStreamTransport extends StreamTransport {
    private static String TAG = BLEStreamTransport.class.getSimpleName();

    private BLEDevice mBLEDevice;

    public BLEStreamTransport(InputStream is, OutputStream os, BLEDevice ble) {
        super(is, os);
        mBLEDevice = ble;
    }

    @Override
    public String readLine() {
        return mBLEDevice.readLine();
    }

    @Override
    public void writeLine(String str) {
        mBLEDevice.writeLine(str);
    }

    @Override
    public int read(byte[] buffer, int offset, int count) {
        return mBLEDevice.read(buffer, offset, count);
    }

    @Override
    public int read(byte[] buffer) {
        return this.read(buffer, 0, buffer.length);
    }


}
