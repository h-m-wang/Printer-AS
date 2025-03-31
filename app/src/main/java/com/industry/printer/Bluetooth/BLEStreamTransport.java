package com.industry.printer.Bluetooth;

import com.industry.printer.Utils.StreamTransport;

import java.io.InputStream;
import java.io.OutputStream;

public class BLEStreamTransport extends StreamTransport {
    private static String TAG = com.industry.printer.BLE.BLEStreamTransport.class.getSimpleName();

    private BLEServer mBLEServer;

    public BLEStreamTransport(InputStream is, OutputStream os, BLEServer ble) {
        super(is, os);
        mBLEServer = ble;
    }

    @Override
    public String readLine() {
        return mBLEServer.readLine();
    }

    @Override
    public void writeLine(String str) {
        mBLEServer.writeLine(str);
    }

    @Override
    public int read(byte[] buffer, int offset, int count) {
        return mBLEServer.read(buffer, offset, count);
    }

    @Override
    public int read(byte[] buffer) {
        return this.read(buffer, 0, buffer.length);
    }
}
