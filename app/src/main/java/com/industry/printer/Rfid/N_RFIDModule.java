package com.industry.printer.Rfid;

import com.industry.printer.BLE.BLEDevice;
import com.industry.printer.Utils.Debug;
import com.industry.printer.hardware.ExtGpio;
import com.industry.printer.hardware.RFIDDevice;

public abstract class N_RFIDModule {
    private static final String TAG = N_RFIDModule.class.getSimpleName();

    // 结果值
    public static final byte RESULT_OK				= 0x00;
    public static final byte RESULT_ERROR			= ~(0x00);

    public static final byte CARD_TYPE_UNKNOWN		= (byte)0x4F;
    public static final byte CARD_TYPE_NO_CARD		= (byte)0x4E;
    public static final byte CARD_TYPE_SUPPORT		= (byte)0x01;
    public static final byte CARD_TYPE_NO_SUPPORT	= (byte)0x02;

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

    // 设置模块工作在ISO14443 TYPE A 模式
    // [报文] 02 00 00 04 3A 41 7F 03
    //      41('A')表示工作在TYPEA模式
    // [返回] 02 XX XX LL 3A RR CC 03
    // 		 RR: 00 成功；非零：失败. LL:数据长度；CC：校验码；XX：地址
    public static final byte 				CMD_TYPEA = 0x3A;
    public static final byte[] 				DATA_WORK_TYPEA = {0x41};

    protected N_RFIDSerialPort mRFIDSerialPort = null;
    protected byte[] mUID;
    protected byte mCardType;
    protected String mErrorMessage = null;

    abstract public boolean searchCard();

    abstract public boolean initCard();

    abstract public boolean writeMaxInkLevel(int max);
    abstract public int readMaxInkLevel();

    abstract public boolean writeInkLevel(int ink);
    abstract public int readInkLevel();

    abstract public boolean writeFeature(byte[] feature);
    abstract public byte[] readFeature();

    abstract public boolean writeCopyInkLevel(int ink);
    abstract public int readCopyInkLevel();

    abstract public float getMaxRatio();

    abstract public byte[] readUID();

    public N_RFIDModule() {
        mRFIDSerialPort = N_RFIDSerialPort.getInstance();
        mUID = null;
        mCardType = CARD_TYPE_UNKNOWN;
        mErrorMessage = null;
    }

    public boolean open(String portName) {
        if(!mRFIDSerialPort.open(portName)) {
            mErrorMessage = "COM异常：" + mRFIDSerialPort.getErrorMessage();
            Debug.e(TAG, mErrorMessage);
            return false;
        }
        mRFIDSerialPort.setBaudrate(115200);
        return true;
    }

    protected N_RFIDData transfer(byte cmd, byte[] data) {
        if(null == mRFIDSerialPort) {
            mErrorMessage = "COM未链接";
            Debug.e(TAG, mErrorMessage);
            return null;
        }

        return mRFIDSerialPort.transfer(cmd, data);
    }

    public byte[] getUID() {
        return mUID;
    }

    public String getErrorMessage() {
        return mErrorMessage;
    }
}
