package com.industry.printer.Rfid;

import com.industry.printer.Utils.Debug;

public class N_RFIDModuleChecker {
    private static final String TAG = N_RFIDModuleChecker.class.getSimpleName();

    public static final int RFID_MOD_UNKNOWN 			= 0;
    public static final int RFID_MOD_M104BPCS 			= 1;
    public static final int RFID_MOD_M104DPCS 			= 2;
    public static final int RFID_MOD_M104BPCS_KX1207 	= 3;

    private static final byte CMD_SEARCH_CARD 				= 0x46;
    private static final byte[] DATA_SEARCH_CARD_ALL 		= {0x52};

    private static final byte RFID_CMD_AUTO_SEARCH 			= 0x20;
    private static final byte[] RFID_DATA_AUTOSEARCH_ALL 	= {0x02};

    private static final byte RFID_CMD_READ_VERIFY 			= 0x21;
    private static final byte[] RFID_DATA_READ_BLOCK 		= {0x00, 0x18, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF};

    private N_RFIDSerialPort mSerialPort = null;
    private String mErrorMessage = null;

    public N_RFIDModuleChecker() {
        mErrorMessage = null;
        mSerialPort = N_RFIDSerialPort.getInstance();
    }

    private N_RFIDData transfer(byte cmd, byte[] data) {
        N_RFIDData rfidData = new N_RFIDData();

        if(0 == mSerialPort.write(rfidData.make(cmd, data))) {
            mErrorMessage = "COM异常：" + mSerialPort.getErrorMessage();
            Debug.e(TAG, mErrorMessage);
            return null;
        }

        byte[] recvData = mSerialPort.read();
        if(null == recvData) {
            mErrorMessage = "COM异常：" + mSerialPort.getErrorMessage();
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

    public int check(String portName) {

        if(!mSerialPort.open(portName)) {
            mErrorMessage = "COM异常：" + mSerialPort.getErrorMessage();
            Debug.e(TAG, mErrorMessage);
            return RFID_MOD_UNKNOWN;
        }

        N_RFIDData rfidData;

        // 02 00 00 04 46 52 9C 03
        // 按着M104BPCS寻卡，如果是DPCS或者1207模块，会无条件返回A9错误，无论是否插卡
        rfidData = transfer(CMD_SEARCH_CARD, DATA_SEARCH_CARD_ALL);
        if(null == rfidData) {
            return RFID_MOD_UNKNOWN;
        }

        if(rfidData.getResult() != N_RFIDModule_M104BPCS.RESULT_NOT_SUPPORT) {
            Debug.d(TAG, "Module Type: RFID_MOD_M104BPCS");
            return RFID_MOD_M104BPCS;
        }

        // 02 00 00 0B 21 00 18 FF FF FF FF FF FF 37 03
        // 读取扇区6，块0的值，这个区没有使用，因此可以使用缺省密钥访问，如果返回A9，则说明模块是1207模块
        rfidData = transfer(RFID_CMD_READ_VERIFY, RFID_DATA_READ_BLOCK);
        if(null == rfidData) {
            return RFID_MOD_UNKNOWN;
        }

        if(rfidData.getResult() == N_RFIDModule_M104BPCS.RESULT_NOT_SUPPORT) {
            Debug.d(TAG, "Module Type: RFID_MOD_M104BPCS_KX1207");
            return RFID_MOD_M104BPCS_KX1207;
        }

        // 02 00 00 04 20 10 02 26 03
        // 最后尝试一下DPCS的寻卡，这一步也可以不做，直接确认气味DPCS模块
        rfidData = transfer(RFID_CMD_AUTO_SEARCH, RFID_DATA_AUTOSEARCH_ALL);
        if(null == rfidData) {
            return RFID_MOD_UNKNOWN;
        }

        Debug.d(TAG, "Module Type: RFID_MOD_M104DPCS");
        return RFID_MOD_M104DPCS;
    }
}
