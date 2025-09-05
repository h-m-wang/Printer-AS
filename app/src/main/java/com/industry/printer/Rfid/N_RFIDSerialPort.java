package com.industry.printer.Rfid;

import com.industry.printer.BLE.BLEDevice;
import com.industry.printer.Bluetooth.BLEServer;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.PlatformInfo;
import com.industry.printer.hardware.ExtGpio;
import com.industry.printer.hardware.RFIDDevice;

public class N_RFIDSerialPort {
    private static final String TAG = N_RFIDSerialPort.class.getSimpleName();

    public static int mFd = 0;
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

    // 设置串口速率
    // [报文] 02 00 00 04 15 10 03 1C 03
    //       设置19200速率
    // [报文] 02 00 00 04 15 07 20 03
    //       设置115200速率
    // [返回] 02 XX XX LL 15 NN CC 03
    // 		 NN: 00 成功；非零：失败
    //       LL:数据长度；CC：校验码
    public static final byte 				CMD_SET_BAUDRATE 	= 0x15;
    public static final byte[] 				DATA_BITRATE_9600	= {0x01};
    public static final byte[] 				DATA_BITRATE_19200	= {0x03};
    public static final byte[] 				DATA_BITRATE_115200	= {0x07};

    public static final byte RESULT_OK				= 0x00;
    public static final byte RESULT_ERROR			= ~(0x00);

// H.M.Wang 2024-7-19 将波特率的设置，从N_RFIDModule转移至此，并且自动尝试以不同的波特率与设备互动
    private boolean trySetBaudrate(int baudrate, int at) {
        byte[] writeBytes;

        RFIDDevice.setBaudrate(mFd, at);

        if(baudrate == 9600) {
            writeBytes = DATA_BITRATE_9600;
        } else if(baudrate == 19200) {
            writeBytes = DATA_BITRATE_19200;
        } else if(baudrate == 115200) {
            writeBytes = DATA_BITRATE_115200;
        } else {
            mErrorMessage = "波特率错误：" + baudrate;
            Debug.e(TAG, mErrorMessage);
            return false;
        }

        N_RFIDData rfidData = transfer(CMD_SET_BAUDRATE, writeBytes);

        if(null != rfidData) {
            if(rfidData.getResult() == RESULT_OK) {
                Debug.d(TAG, "波特率设置成功");
                RFIDDevice.setBaudrate(mFd, baudrate);
                try { Thread.sleep(500);} catch(Exception e){}
                return true;
            } else {
                mErrorMessage = "设备返回失败：" + rfidData.getResult();
                Debug.e(TAG, mErrorMessage);
            }
        }

        Debug.e(TAG, "波特率设置失败");

        return false;
    }

    public boolean setBaudrate(int baudrate) {
        if(isOpened())	{
            if(!trySetBaudrate(baudrate, 115200) && !trySetBaudrate(baudrate, 19200) && !trySetBaudrate(baudrate, 9600)) return false;
            return true;
        }
        return false;
    }
// End of H.M.Wang 2024-7-19 将波特率的设置，从N_RFIDModule转移至此，并且自动尝试以不同的波特率与设备互动

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

    protected N_RFIDData transfer(byte cmd, byte[] data) {
        N_RFIDData rfidData = new N_RFIDData();
        while(BLEServer.BLERequiring) {
            try{Thread.sleep(100);}catch(Exception e){}
        }
synchronized (RFIDDevice.SERIAL_LOCK) {
// H.M.Wang 2025-9-5 修改为A133的情况下不执行此操作，A20的时候执行
// H.M.Wang 2025-8-15 永久取消蓝牙与串口通过PI9的切换功能        ExtGpio.writeGpioTestPin('I', 9, 0);
    if(!PlatformInfo.isA133Product()) ExtGpio.writeGpioTestPin('I', 9, 0);
// End of H.M.Wang 2025-9-5 修改为A133的情况下不执行此操作，A20的时候执行
        if (0 == write(rfidData.make(cmd, data))) {
            mErrorMessage = "COM异常：" + getErrorMessage();
            Debug.e(TAG, mErrorMessage);
            return null;
        }

        byte[] recvData = read();
// H.M.Wang 2025-9-5 修改为A133的情况下不执行此操作，A20的时候执行
// H.M.Wang 2025-8-15 永久取消蓝牙与串口通过PI9的切换功能        ExtGpio.writeGpioTestPin('I', 9, 1);
    if(!PlatformInfo.isA133Product()) ExtGpio.writeGpioTestPin('I', 9, 1);
// End of H.M.Wang 2025-9-5 修改为A133的情况下不执行此操作，A20的时候执行
        if (null == recvData) {
            mErrorMessage = "COM异常：" + getErrorMessage();
            Debug.e(TAG, mErrorMessage);
            return null;
        }

        if(!rfidData.parse(recvData)) {
            mErrorMessage = "数据包异常：" + rfidData.getErrorMessage();
            Debug.e(TAG, mErrorMessage);
            return null;
        }
}
        return rfidData;
    }
}
