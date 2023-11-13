package com.industry.printer.Rfid;

import com.industry.printer.Utils.ByteArrayUtils;
import com.industry.printer.Utils.Debug;

public class N_RFIDModule_M104DPCS extends N_RFIDModule {
    private static final String TAG = N_RFIDModule_M104DPCS.class.getSimpleName();

    // 寻卡
    // [报文] 02 00 00 04 20 10 02 26 03
    //      寻所有卡
    // [返回] 02 00 08 07 20 RR CC CC CC CC f6 03
    // 		 RR: 00 成功；非零：失败；CCCCCCCC: 4字节卡号
    // [例子] 02 00 08 07 20 00 52 50 30 F5 F6 03
    // 		  成功，卡号：52 50 30 F5
    public static final byte 				CMD_SEARCH_CARD = 0x20;
    public static final byte[] 				DATA_SEARCH_CARD_ALL_TOLERANT_TO_COPY = {0x00};
    public static final byte[] 				DATA_SEARCH_CARD_WAKE_TOLERANT_TO_COPY = {0x01};
    public static final byte[] 				DATA_SEARCH_CARD_ALL = {0x02};
    public static final byte[] 				DATA_SEARCH_CARD_WAKE = {0x03};

    public static final byte 				RES_COPY_CARD = (byte)0xAA;

    // 读块
    // [报文] 02 00 00 0B 21 TT NN KK KK KK KK KK KK B6 03
    //       TT: 密钥种类，0-A密钥，1-B密钥；NN: 块号；KKx6：密钥
    // [例子] 02 00 00 0B 21 00 10 10 AD AF C7 CF 0A 7E B6 03
    //       A密钥；0x10块；AD AF C7 CF 0A 7E：密钥
    // [返回] 02 00 08 13 21 00 1D 10 02 00 00 00 00 00 00 00 00 00 00 00 00 00 00 5B 03
    // 		 RR: 00 成功；非零：失败.
    //       （0x10 块数据：1D 10 02 00 00 00 00 00 00 00 00 00 00 00 00 00 00）
    public static final byte 				CMD_READ_BLOCK = 0x21;
    public static final byte 				DATA_KEY_A = 0x00;
    public static final byte 				DATA_KEY_B = 0x01;

    // 写块
    // [报文] 02 00 00 1B 23 TT NN KK KK KK KK KK KK CC CC CC CC CC CC CC CC CC CC CC CC CC CC CC CC C2 03
    //       TT: 密钥种类，0-A密钥，1-B密钥；NN: 块号；KKx6：密钥；CCx16：数据
    // [报文] 02 00 00 1B 23 00 12 AD AF C7 CF 0A 7E 00 11 22 33 44 55 66 77 88 99 AA BB CC DD EE FF C2 03
    //       A密钥；0x12块；AD AF C7 CF 0A 7E：密钥；00 11 22 33 44 55 66 77 88 99 AA BB CC DD EE FF：数据
    // [返回] 02 00 BE 10 03 23 00 E4 03
    // 		 RR: 00 成功；非零：失败.
    public static final byte 				CMD_WRITE_BLOCK = 0x23;

    private static final byte SECTOR_00 							= 0x00;
    private static final byte SECTOR_04 							= 0x04;
    private static final byte SECTOR_05 							= 0x05;

    private static final byte BLOCK_NUM_PER_SECTOR 					= 4;									// 每个扇区的块数
    private static final byte SECTOR_INK_MAX 						= SECTOR_04;							// 锁值最大值，特征值及锁值，保存在第04扇区
    private static final byte SECTOR_INKLEVEL 						= SECTOR_04;
    private static final byte SECTOR_FEATURE 						= SECTOR_04;
    private static final byte BLOCK_INK_MAX 						= SECTOR_INK_MAX * BLOCK_NUM_PER_SECTOR + 0x00;				// 第04扇区中的第00块
    private static final byte BLOCK_FEATURE 						= SECTOR_FEATURE * BLOCK_NUM_PER_SECTOR + 0x01;				// 第04扇区中的第01块
    private static final byte BLOCK_INKLEVEL 						= SECTOR_INKLEVEL * BLOCK_NUM_PER_SECTOR + 0x02;			// 第04扇区中的第02块

    //	private static final byte SECTOR_COPY_FEATURE 					= SECTOR_05;							// 特征值及锁值的备份，保存在第05扇区
    private static final byte SECTOR_COPY_INKLEVEL 					= SECTOR_05;
    //	private static final byte BLOCK_COPY_FEATURE 					= SECTOR_COPY_FEATURE * BLOCK_NUM_PER_SECTOR + 0x01;		// 第05扇区中的第01块
    private static final byte BLOCK_COPY_INKLEVEL 					= SECTOR_COPY_INKLEVEL * BLOCK_NUM_PER_SECTOR + 0x02;		// 第05扇区中的第02块

    public N_RFIDModule_M104DPCS() {
        super();
    }

    @Override
    public boolean searchCard() {
        Debug.d(TAG, "  ==> 开始寻卡");

        N_RFIDData rfidData = transfer(CMD_SEARCH_CARD, DATA_SEARCH_CARD_ALL);

        if(null != rfidData) {
            if(rfidData.getResult() == RESULT_OK) {
                byte[] resData = rfidData.getData();
                if( null != resData && resData.length == 4) {	// 4个字节卡号；如果是1207卡也可以查询到，其卡号为7个字节，但是对于本模块不适用
                    mUID = resData;
                    mCardType = CARD_TYPE_SUPPORT;
                    Debug.d(TAG, "  ==> 寻卡成功，获得卡号：[" + ByteArrayUtils.toHexString(mUID) + "]");
                    return true;
                } else {
                    mCardType = CARD_TYPE_NO_SUPPORT;
                    mErrorMessage = "不支持的卡：[" + (null != resData ? ByteArrayUtils.toHexString(resData) : "null") + "]";
                    Debug.e(TAG, mErrorMessage);
                }
            } else {
                mCardType = CARD_TYPE_UNKNOWN;
                mErrorMessage = "设备返回失败：" +  String.format("0x%02X", rfidData.getResult());
                Debug.e(TAG, mErrorMessage);
            }
        }

        Debug.e(TAG, "  ==> 寻卡失败");
        return false;
    }

    @Override
    public boolean initCard() {
        mInitialized = searchCard();
        return mInitialized;
    }

    private boolean writeBlock(byte type, byte block, byte[]data) {
        if(block < 0x00 || block > 0x3F) {
            mErrorMessage = "错误的页号";
            Debug.e(TAG, mErrorMessage);
            return false;
        }

        if(null == data) {
            mErrorMessage = "数据无效：" + null;
            Debug.e(TAG, mErrorMessage);
            return false;
        }

        if(data.length != 16) {
            mErrorMessage = "数据长度错误：" + data.length;
            Debug.e(TAG, mErrorMessage);
            return false;
        }

        Debug.d(TAG, "  ==> 开始写入块[" + String.format("0x%02X", block) + "]的值[" + ByteArrayUtils.toHexString(data) + "], 使用密钥：" + (type == DATA_KEY_A ? "A" : "B"));

        byte[] key;
        if(type == DATA_KEY_A) {
            key = EncryptionMethod.getInstance().getKeyA(mUID);
        } else if(type == DATA_KEY_B) {
            key = EncryptionMethod.getInstance().getKeyB(mUID);
        } else {
            Debug.e(TAG, "  ==> 密钥类型错误[" + type + "]");
            return false;
        }

        byte[] writeData = new byte[data.length+2+key.length];
        writeData[0] = type;
        writeData[1] = block;
        System.arraycopy(key, 0, writeData, 2, key.length);
        System.arraycopy(data, 0, writeData, 2+key.length, data.length);

        N_RFIDData rfidData = transfer(CMD_WRITE_BLOCK, writeData);

        if(null != rfidData) {
            if(rfidData.getResult() == RESULT_OK) {
                Debug.d(TAG, "  ==> 写入块成功");
                return true;
            } else {
                mErrorMessage = "设备返回失败：" +  String.format("0x%02X", rfidData.getResult());
                Debug.e(TAG, mErrorMessage);
            }
        }

        mInitialized = false;

        Debug.e(TAG, "  ==> 写入块失败");

        return false;
    }

    private byte[] readBlock(byte type, byte block) {
        Debug.d(TAG, "  ==> 开始读块[" + String.format("0x%02X", block) + "], 使用密钥：" + (type == DATA_KEY_A ? "A" : "B"));

        byte[] key;
        if(type == DATA_KEY_A) {
            key = EncryptionMethod.getInstance().getKeyA(mUID);
        } else if(type == DATA_KEY_B) {
            key = EncryptionMethod.getInstance().getKeyB(mUID);
        } else {
            Debug.e(TAG, "  ==> 密钥类型错误[" + type + "]");
            return null;
        }

        byte[] readData = new byte[2+key.length];
        readData[0] = type;
        readData[1] = block;
        System.arraycopy(key, 0, readData, 2, key.length);

        N_RFIDData rfidData = transfer(CMD_READ_BLOCK, readData);

        if(null != rfidData) {
            if(rfidData.getResult() == RESULT_OK) {
                byte[] resData = rfidData.getData();
                if( null != resData && resData.length == 16) { 		// 读到的16字节块值
                    Debug.d(TAG, "  ==> 成功读取块[" + String.format("0x%02X", block) + "]的值[" + ByteArrayUtils.toHexString(resData) + "]");
                    return resData;
                } else {
                    mErrorMessage = "数据空或长度错误：[" + (null != resData ? ByteArrayUtils.toHexString(resData) : "null") + "]";
                    Debug.e(TAG, mErrorMessage);
                }
            } else {
                mErrorMessage = "设备返回失败：" +  String.format("0x%02X", rfidData.getResult());
                Debug.e(TAG, mErrorMessage);
            }
        }

        mInitialized = false;

        Debug.e(TAG, "  ==> 读块失败");

        return null;
    }

    @Override
    public boolean writeMaxInkLevel(int max) {
        Debug.d(TAG, "  ==> 开始写入墨水最大值");

        if(writeBlock(DATA_KEY_A, BLOCK_INK_MAX, EncryptionMethod.getInstance().encryptInkLevel(max))) {
            Debug.d(TAG, "  ==> 写入墨水最大值成功");
            return true;
        }

        Debug.e(TAG, "  ==> 写入墨水最大值失败");

        return false;
    }

    @Override
    public int readMaxInkLevel() {
        Debug.d(TAG, "  ==> 开始读墨水最大值");

        byte[] data = readBlock(DATA_KEY_A, BLOCK_INK_MAX);
        if(null != data) {
            int max = EncryptionMethod.getInstance().N_decryptInkLevel(data);
            Debug.d(TAG, "  ==> 读墨水最大值成功[" + max + "]");
            return max;
        }

        Debug.e(TAG, "  ==> 读墨水最大值失败");

        return 0;
    }

    @Override
    public boolean writeInkLevel(int ink) {
        Debug.d(TAG, "  ==> 开始写入墨水值");

        if(writeBlock(DATA_KEY_A, BLOCK_INKLEVEL, EncryptionMethod.getInstance().encryptInkLevel(ink))) {
            Debug.d(TAG, "  ==> 写入墨水值成功");
            return true;
        }

        Debug.e(TAG, "  ==> 写入墨水值失败");

        return false;
    }

    @Override
    public int readInkLevel() {
        Debug.d(TAG, "  ==> 开始读墨水值");

        byte[] data = readBlock(DATA_KEY_A, BLOCK_INKLEVEL);
        if(null != data) {
            int level = EncryptionMethod.getInstance().N_decryptInkLevel(data);
            Debug.d(TAG, "  ==> 读墨水值成功[" + level + "]");
            return level;
        }

        Debug.e(TAG, "  ==> 读墨水值失败");

        return 0;
    }

    @Override
    public boolean writeFeature(byte[] feature) {
        Debug.d(TAG, "  ==> 开始写入特征值");

        if(writeBlock(DATA_KEY_A, BLOCK_FEATURE, feature)) {
            Debug.d(TAG, "  ==> 写入特征值成功");
            return true;
        }

        Debug.e(TAG, "  ==> 写入特征值失败");

        return false;
    }

    @Override
    public byte[] readFeature() {
        Debug.d(TAG, "  ==> 开始读特征值");

        byte[] data = readBlock(DATA_KEY_A, BLOCK_FEATURE);
        if(null != data) {
            Debug.d(TAG, "  ==> 读特征值成功[" + ByteArrayUtils.toHexString(data) + "]");
            return data;
        }

        Debug.e(TAG, "  ==> 读墨水值失败");

        return null;
    }

    @Override
    public boolean writeCopyInkLevel(int ink) {
        Debug.d(TAG, "  ==> 开始写入备份墨水值");

        if(writeBlock(DATA_KEY_A, BLOCK_COPY_INKLEVEL, EncryptionMethod.getInstance().encryptInkLevel(ink))) {
            Debug.d(TAG, "  ==> 写入备份墨水值成功");
            return true;
        }

        Debug.e(TAG, "  ==> 写入备份墨水值失败");

        return false;
    }

    @Override
    public int readCopyInkLevel() {
        Debug.d(TAG, "  ==> 开始读备份墨水值");

        byte[] data = readBlock(DATA_KEY_A, BLOCK_COPY_INKLEVEL);
        if(null != data) {
            int level = EncryptionMethod.getInstance().N_decryptInkLevel(data);
            Debug.d(TAG, "  ==> 读备份墨水值成功[" + level + "]");
            return level;
        }

        Debug.e(TAG, "  ==> 读备份墨水值失败");

        return 0;
    }

    @Override
    public byte[] readUID() {
        Debug.d(TAG, "  ==> 开始读UID");

        N_RFIDData rfidData = transfer(CMD_SEARCH_CARD, DATA_SEARCH_CARD_ALL);

        if(null != rfidData) {
            if(rfidData.getResult() == RESULT_OK) {
                byte[] resData = rfidData.getData();
                if( null != resData && resData.length == 4) {	// 4个字节卡号；如果是1207卡也可以查询到，其卡号为7个字节，但是对于本模块不适用
                    Debug.d(TAG, "  ==> 获得卡号：[" + ByteArrayUtils.toHexString(mUID) + "]");
                    return resData;
                } else {
                    mErrorMessage = "不支持的卡：[" + (null != resData ? ByteArrayUtils.toHexString(resData) : "null") + "]";
                    Debug.e(TAG, mErrorMessage);
                }
            } else {
                mErrorMessage = "设备返回失败：" +  String.format("0x%02X", rfidData.getResult());
                Debug.e(TAG, mErrorMessage);
            }
        }

        Debug.e(TAG, "  ==> 读UID失败");
        return null;
    }
}
