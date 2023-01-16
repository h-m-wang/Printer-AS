package com.industry.printer.Rfid;

import java.io.ByteArrayOutputStream;

public class N_RFIDData {
    private String TAG = N_RFIDData.class.getSimpleName();

    private static final byte IDENTIFICATOR 		= 0x10;
    private static final byte HEADER 				= 0x02;
    private static final byte TAILER 				= 0x03;

    public static final byte ERROR_SUCCESS			= 0x00;
    public static final byte ERROR_DATA_NULL		= (byte)0xFF;
    public static final byte ERROR_INVALID_COVER	= (byte)0xFE;
    public static final byte ERROR_PACK_LENGTH		= (byte)0xFD;
    public static final byte ERROR_DATA_LENGTH		= (byte)0xFC;
    public static final byte ERROR_CHECK_CODE		= (byte)0xFB;

    private static final int POS_ADDRESS			= 0;
    private static final int POS_LENGTH				= 2;
    private static final int POS_CMD				= 3;
    private static final int POS_RESULT				= 4;

    private byte[] mAddress;
    private byte mCmd;
    private byte mResult;
    private byte[] mData;

    private byte mError = ERROR_SUCCESS;

    public N_RFIDData() {
        mError = ERROR_SUCCESS;
        mAddress = new byte[2];
        mCmd = 0;
        mResult = N_RFIDModule.RESULT_ERROR;
        mData = null;
    }

    public byte getErrorCode() {
        return mError;
    }

    public String getErrorMessage() {
        String errMsg = "";

        switch(mError) {
            case ERROR_SUCCESS:
                break;
            case ERROR_DATA_NULL:
                errMsg = "空数据";
                break;
            case ERROR_INVALID_COVER:
                errMsg = "数据包错误";
                break;
            case ERROR_PACK_LENGTH:
                errMsg = "数据包长度错误";
                break;
            case ERROR_DATA_LENGTH:
                errMsg = "数据长度错误";
                break;
            case ERROR_CHECK_CODE:
                errMsg = "校验码错误";
                break;
            default:
                errMsg = "其他错误";
                break;
        }

        return errMsg;
    }



    public byte[] getAddress() {
        return mAddress;
    }

    public byte getCmd() {
        return mCmd;
    }

    public byte getResult() {
        return mResult;
    }

    public byte[] getData() {
        return mData;
    }

    public byte[] make(byte cmd, byte[] data) {
        if(null == data) {
            mError = ERROR_DATA_NULL;
            return null;
        }

        int sendBufLen = data.length + 5;	// 加上2个地址长度, 长度字节1字节，命令1字节，校验位1字节
        byte[] sendData = new byte[sendBufLen];

        sendData[POS_ADDRESS] 	= mAddress[0];
        sendData[POS_ADDRESS+1] = mAddress[1];
        sendData[POS_LENGTH] 	= (byte)(sendBufLen-2);		// 不包含2个字节地址，发送时长度包含校验位，接收时不包括校验位
        sendData[POS_CMD] 		= cmd;
        System.arraycopy(data, 0, sendData, 4, data.length);
        sendData[sendData.length-1] = (byte)calCheckCode(sendData);

        mError = ERROR_SUCCESS;

        return addCover(addIdentificator(sendData));
    }

    public boolean parse(byte[] data) {
        if(null == data) {
            mError = ERROR_DATA_NULL;
            return false;
        }

        if(data.length < 2) {			// 至少需要报头与报尾
            mError = ERROR_PACK_LENGTH;
            return false;
        }

        byte[] recvData = removeIdentificator(removeCover(data));

        if(null == recvData) return false;

        if(recvData.length < 6) {			// 至少需要包含地址2字节，长度1字节，命令1字节，结果1字节，校验1字节
            mError = ERROR_PACK_LENGTH;
            return false;
        }

        if(recvData[POS_LENGTH] != recvData.length-3) {	// 长度需要等于去掉地址和校验，3个字节的长度。，发送时长度包含校验位，接收时不包括校验位
            mError = ERROR_DATA_LENGTH;
            return false;
        }

        if((byte)calCheckCode(recvData) != recvData[recvData.length-1]) {
            mError = ERROR_CHECK_CODE;
            return false;
        }

        mAddress[0] = recvData[POS_ADDRESS];
        mAddress[1] = recvData[POS_ADDRESS+1];
        mCmd 		= recvData[POS_CMD];
        mResult 	= recvData[POS_RESULT];

        byte[] realData = null;
        if(recvData.length-6 > 0) {
            realData = new byte[recvData.length-6];			// 去掉地址2字节，长度1字节，命令1字节，结果1字节，校验位1字节
            System.arraycopy(recvData, POS_RESULT+1, realData, 0, realData.length);
        }
        mData = realData;

        mError = ERROR_SUCCESS;

        return true;
    }

    // 校验值为从地址位开始到数据段的最后一位的逻辑和
    private int calCheckCode(byte[] realData) {
        int checkCode = 0;
        for (int i=0; i<realData.length-1; i++) {
            checkCode += realData[i];
        }
        return checkCode;
    }

    private byte[] addIdentificator(byte[] realData) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        for(int i=0; i<realData.length; i++) {
            if(realData[i] == HEADER || realData[i] == TAILER || realData[i] == IDENTIFICATOR) {
                baos.write(IDENTIFICATOR);
            }
            baos.write(realData[i]);
        }

        return baos.toByteArray();
    }

    private byte[] removeIdentificator(byte[] procData) {
        if(null == procData) {
            return null;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        boolean skip = true;

        for(int i=0; i<procData.length; i++) {
            if(procData[i] == IDENTIFICATOR && skip) {
                skip = false;
                continue;
            }
            baos.write(procData[i]);
            skip = true;
        }

        return baos.toByteArray();
    }

    private byte[] addCover(byte[] procData) {
        byte[] sendData = new byte[procData.length+2];		// Add Header & Tailer

        sendData[0] = HEADER;
        System.arraycopy(procData, 0, sendData, 1, procData.length);
        sendData[sendData.length-1] = TAILER;

        return sendData;
    }

    private byte[] removeCover(byte[] recvData) {
        if(null == recvData) {
            return null;
        }

        if(recvData[0] != HEADER || recvData[recvData.length-1] != TAILER) {
            mError = ERROR_INVALID_COVER;
            return null;
        }

        byte[] procData = new byte[recvData.length-2];

        System.arraycopy(recvData, 1, procData, 0, procData.length);

        return procData;
    }
}
