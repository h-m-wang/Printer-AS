package com.industry.printer.Rfid;

import com.industry.printer.Utils.Debug;

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
    protected boolean mInitialized = false;
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

    abstract public byte[] readUID();

    public N_RFIDModule() {
        mRFIDSerialPort = N_RFIDSerialPort.getInstance();
        mInitialized = false;
        mUID = null;
        mCardType = CARD_TYPE_UNKNOWN;
        mErrorMessage = null;
    }

    public boolean open(String portName) {
        boolean success = mRFIDSerialPort.open(portName);
        if(!success) {
            mErrorMessage = "COM异常：" + mRFIDSerialPort.getErrorMessage();
            Debug.e(TAG, mErrorMessage);
        }
        return success;
    }

    protected N_RFIDData transfer(byte cmd, byte[] data) {
        if(null == mRFIDSerialPort) {
            mErrorMessage = "COM未链接";
            Debug.e(TAG, mErrorMessage);
            return null;
        }

        N_RFIDData rfidData = new N_RFIDData();

        if(0 == mRFIDSerialPort.write(rfidData.make(cmd, data))) {
            mErrorMessage = "COM异常：" + mRFIDSerialPort.getErrorMessage();
            Debug.e(TAG, mErrorMessage);
            return null;
        }

        byte[] recvData = mRFIDSerialPort.read();
        if(null == recvData) {
            mErrorMessage = "COM异常：" + mRFIDSerialPort.getErrorMessage();
            Debug.e(TAG, mErrorMessage);
            return null;
        }

        if(!rfidData.parse(recvData)) {
            mErrorMessage = "数据包异常：" + rfidData.getErrorMessage();
            Debug.e(TAG, mErrorMessage);
            return null;
        }

        return rfidData;
    }

    public boolean setBaudrate(int baudrate) {
        byte[] writeBytes;

        if(baudrate == 19200) {
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
                return mRFIDSerialPort.setBaudrate(baudrate);
            } else {
                mErrorMessage = "设备返回失败：" + rfidData.getResult();
                Debug.e(TAG, mErrorMessage);
            }
        }

        Debug.e(TAG, "波特率设置失败");

        return false;
    }

    public byte[] getUID() {
        if(mInitialized) return mUID;
        else return null;
    }

    public String getErrorMessage() {
        return mErrorMessage;
    }
}
