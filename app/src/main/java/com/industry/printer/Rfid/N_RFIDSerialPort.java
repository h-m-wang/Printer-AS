package com.industry.printer.Rfid;

import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.PlatformInfo;
import com.industry.printer.hardware.RFIDDevice;

public class N_RFIDSerialPort {
    private static final String TAG = N_RFIDSerialPort.class.getSimpleName();

    private static int mFd = 0;
    private String mErrorMessage;
    private static N_RFIDSerialPort mRFIDSerialPort = null;

    public static N_RFIDSerialPort getInstance() {
        if(null == mRFIDSerialPort) {
            mRFIDSerialPort = new N_RFIDSerialPort();
        }
        return mRFIDSerialPort;
    }

    private N_RFIDSerialPort() {
        mErrorMessage = "";
    }

    public boolean isOpened() {
        return (mFd > 0);
    }

    public String getErrorMessage() {
        return mErrorMessage;
    }

    public boolean open(String portName) {
        if(isOpened()) {
            return true;
        }

        synchronized (N_RFIDSerialPort.this) {
            mFd = RFIDDevice.open(portName);
            Debug.d(TAG, portName + " opened as [" + mFd + "]");
        }

        return isOpened();
    }

    public void close() {
        if(isOpened())	{
            RFIDDevice.close(mFd);
            mFd = 0;
        }
    }

    public boolean setBaudrate(int baudrate) {
        if(isOpened())	{
            RFIDDevice.setBaudrate(mFd, baudrate);
            return true;
        }
        return false;
    }

    public int write(byte[] data) {
        if(!isOpened()) {
            mErrorMessage = "Serial port not opened";
            Debug.d(TAG, mErrorMessage);
            return 0;
        }
        if(null == data) {
            mErrorMessage = "Data null";
            Debug.d(TAG, mErrorMessage);
            return 0;
        }

        Debug.print("RFID-SEND", data);

        int ret;
        synchronized (N_RFIDSerialPort.this) {
            ret = RFIDDevice.write(mFd, data, data.length);
        }

        if(ret <= 0) {
            mErrorMessage = "Sending data failed.";
        }
        return ret;
    }

    public byte[] read() {
        if(!isOpened()) {
            mErrorMessage = "Serial port not opened";
            Debug.d(TAG, mErrorMessage);
            return null;
        }

        byte[] readin;
        synchronized (N_RFIDSerialPort.this) {
            readin = RFIDDevice.read(mFd, 128);
        }

        Debug.print("RFID-RECV", readin);

        if(null == readin) {
            mErrorMessage = "Receiving data failed.";
        }
        return readin;
    }
}
