package com.industry.printer.Rfid;

import com.industry.printer.Utils.ByteArrayUtils;
import com.industry.printer.Utils.Debug;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class N_RFIDModule_M104BPCS extends N_RFIDModule {
    private static final String TAG = N_RFIDModule_M104BPCS.class.getSimpleName();

    // 结果值
    public static final byte RESULT_CARD_NOT_FOUND	= 0x01;
    public static final byte RESULT_NOT_SUPPORT		= (byte)0xA9;
    public static final byte RESULT_INVALID_KEY		= (byte)0xF5;
    public static final byte RESULT_BAD_PARAM		= (byte)0xF7;
    public static final byte RESULT_FAILED			= (byte)0xFF;

    // 寻卡
    // [报文] 02 00 00 04 46 52 9C 03
    //      寻所有卡
    // [返回] 02 XX XX LL 46 RR CC 03
    // 		 RR: 00 成功；非零：失败；01：未插卡；A9：不支持模块. LL:数据长度；CC：校验码；XX：地址
    // [例子]	02 00 08 10 03 46 A9 FA 03	[不支持模块，DPCS or 1207]
    //		02 00 00 10 03 46 01 4A 03	[未插卡]
    //		02 00 00 05 46 00 04 00 4F 03 [S50卡]
    public static final byte 				CMD_SEARCH_CARD = 0x46;
    public static final byte[] 				DATA_SEARCH_CARD_WAKE = {0x41};
    public static final byte[] 				DATA_SEARCH_CARD_ALL = {0x52};
    public static final byte[] 				RES_SEARCH_CARD_MIFARE_S50 = {0x04,0x00};
    public static final byte[] 				RES_SEARCH_CARD_MIFARE_S70 = {0x02,0x00};
    public static final byte[] 				RES_SEARCH_CARD_UTRALIGHT = {0x44,0x00};

    // 卡防冲突
    // [报文] 02 00 00 04 47 04 4F 03
    //      放冲突，返回4字节的卡号
    // [返回] 02 00 00 07 47 00 42 0B C2 08 65 03 （返回卡号：42 0B C2 08）
    // [返回] 02 XX XX LL 47 RR NN NN NN NN CC 03
    // 		 RR: 00 成功；非零：失败. LL:数据长度；CC：校验码；XX：地址
    //       RR=0时，NN为卡号
    public static final byte 				CMD_CONFLICT_PREVENTION = 0x47;
    public static final byte[] 				DATA_CONFLICT_PREVENTION_4BYTES = {0x04};	// Mifare S50,S70,FM11RF08 卡序列号为4 字节，
    public static final byte[] 				DATA_CONFLICT_PREVENTION_7BYTES = {0x07};	// 暂时不使用
    public static final byte[] 				DATA_CONFLICT_PREVENTION_10BYTES = {0x0A};	// 暂时不使用

    // 选卡
    // [报文] 02 00 00 07 48 42 0B C2 08 66 03
    //      附上4字节的卡号
    // [返回] 02 00 00 04 48 00 08 54 03 （根据卡容量0x08，可判断为S50 卡）
    public static final byte 				CMD_SELECT_CARD = 0x48;
    public static final byte 				RES_DATA_SELECT_CARD_S50 = 0x08;
    public static final byte 				RES_DATA_SELECT_CARD_S70 = 0x20;

    // 卡密钥验证
    // [报文] 02 00 00 0B 4A 60 NN FF FF FF FF FF FF AF 03
    //      60: 验证A密钥, 61: 验证B密钥
    //      NN: 块号
    //      FF FF FF FF FF FF:6字节密码
    // [返回] 02 00 00 10 03 4A 00 4D 03
    // 		 RR: 00 成功；非零：失败.
    public static final byte 				CMD_KEY_VERIFICATION = 0x4A;
    public static final byte 				DATA_KEY_A = 0x60;
    public static final byte 				DATA_KEY_B = 0x61;
    public static final byte[] 				DATA_KEY_DEFAULT = {(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF};
    public static final byte[] 				DATA_KEY_SECTOR0 = {(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00};

    // 读块
    // [报文] 02 00 00 04 4B NN 4F 03
    //       NN: 块号
    // [返回] 02 00 00 13 4B 00 42 0B C2 08 83 08 04 00 62 63 64 65 66 67 68 69 30 03
    // 		 RR: 00 成功；非零：失败.
    //       （0 块数据：42 0B C2 08 83 08 04 00 62 63 64 65 66 67 68 69）
    public static final byte 				CMD_READ_BLOCK = 0x4B;

    // 写块
    // [报文] 02 00 00 14 4C 01 11 11 11 11 11 11 11 11 11 11 11 11 11 11 11 11 71 03
    //       NN: 块号（01）
    //       16字节数据（11）
    // [返回] 02 00 00 10 03 4C 00 4F 03
    // 		 RR: 00 成功；非零：失败.
    public static final byte 				CMD_WRITE_BLOCK = 0x4C;

    public static final byte SECTOR_00 							= 0x00;
    public static final byte SECTOR_04 							= 0x04;
    public static final byte SECTOR_05 							= 0x05;

    private Map<Byte, Boolean> mKeyVerified;

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

    private static final byte BLOCK_KEY 							= 0x03;									// 每个扇区的第03块用来保存密钥，前6个字节保存密钥A，后部6个字节保存密钥B，中间的4个字节保留，不要动

    public N_RFIDModule_M104BPCS() {
        super();
        initVariables();
    }

    private void initVariables() {
    	mKeyVerified = new HashMap<Byte, Boolean>();
        mKeyVerified.put(SECTOR_00, false);
        mKeyVerified.put(SECTOR_04, false);
        mKeyVerified.put(SECTOR_05, false);
    }

    @Override
    public boolean searchCard() {
        Debug.d(TAG, "  ==> 开始寻卡");

        N_RFIDData rfidData = transfer(CMD_SEARCH_CARD, DATA_SEARCH_CARD_ALL);

        if(null != rfidData) {
            if(rfidData.getResult() == RESULT_OK) {
                byte[] resData = rfidData.getData();
                if( null != resData && resData.length == 2 &&
                        resData[0] == RES_SEARCH_CARD_MIFARE_S50[0] &&
                        resData[1] == RES_SEARCH_CARD_MIFARE_S50[1] ) {
                    mCardType = CARD_TYPE_SUPPORT;
                    Debug.d(TAG, "  ==> 寻卡成功");
                    return true;
                } else {
                    mErrorMessage = "不支持的卡：[" + (null != resData ? ByteArrayUtils.toHexString(resData) : "null") + "]";
                    Debug.e(TAG, mErrorMessage);
                }
            } else if(rfidData.getResult() == RESULT_CARD_NOT_FOUND) {
                mCardType = CARD_TYPE_NO_CARD;
                mErrorMessage = "设备返回失败：未插卡";
                Debug.e(TAG, mErrorMessage);
            } else if(rfidData.getResult() == RESULT_NOT_SUPPORT ) {
                mCardType = CARD_TYPE_NO_SUPPORT;
                mErrorMessage = "设备返回失败：不支持的模块";
                Debug.e(TAG, mErrorMessage);
            } else {
                mCardType = CARD_TYPE_UNKNOWN;
                mErrorMessage = "设备返回失败：" + String.format("0x%02X", rfidData.getResult());
                Debug.e(TAG, mErrorMessage);
            }
        }

        Debug.e(TAG, "  ==> 寻卡失败");

        return false;
    }

    private boolean avoidConflict() {
        Debug.d(TAG, "  ==> 开始防冲突");

        N_RFIDData rfidData = transfer(CMD_CONFLICT_PREVENTION, DATA_CONFLICT_PREVENTION_4BYTES);

        if(null != rfidData) {
            if(rfidData.getResult() == RESULT_OK) {
                byte[] resData = rfidData.getData();
                if( null != resData && resData.length == 4) { 		// 数据段为4个字节的卡号
                    mUID = resData;
                    Debug.d(TAG, "  ==> 防冲突成功，获得卡号：[" + ByteArrayUtils.toHexString(mUID) + "]");
                    return true;
                }
            } else {
                mErrorMessage = "设备返回失败：" +  String.format("0x%02X", rfidData.getResult());
                Debug.e(TAG, mErrorMessage);
            }
        }

        Debug.e(TAG, "  ==> 防冲突失败");

        return false;
    }

    private boolean selectCard() {
        Debug.d(TAG, "  ==> 开始选卡");

        N_RFIDData rfidData = transfer(CMD_SELECT_CARD, mUID);

        if(null != rfidData) {
            if(rfidData.getResult() == RESULT_OK) {
                byte[] resData = rfidData.getData();
                if(null != resData && resData.length == 1 && resData[0] == RES_DATA_SELECT_CARD_S50) { 		// 08表示S50卡，其他的卡报错
                    Debug.d(TAG, "  ==> 选卡成功");
                    return true;
                } else {
                    mErrorMessage = "卡种类错误：" + "[" + (null != resData ? ByteArrayUtils.toHexString(resData) : "null") + "]";
                    Debug.e(TAG, mErrorMessage);
                }
            } else {
                mErrorMessage = "设备返回失败：" +  String.format("0x%02X", rfidData.getResult());
                Debug.e(TAG, mErrorMessage);
            }
        }

        Debug.e(TAG, "  ==> 选卡失败");

        return false;
    }

    @Override
    public boolean initCard() {
        for(Entry<Byte, Boolean> entry : mKeyVerified.entrySet()) {
            entry.setValue(false);
        }
        mInitialized = searchCard();
        if(!mInitialized) return false;
        mInitialized = avoidConflict();
        if(!mInitialized) return false;
        mInitialized = selectCard();
        return mInitialized;
    }

    private boolean verifyKey(byte sector, byte type, byte[] key) {
        // 验证密钥
        byte[] writeData = new byte[key.length+2];

        Debug.d(TAG, "  ==> 开始验证密钥。扇区" + sector + "的验证密钥[" + ByteArrayUtils.toHexString(key) + "]");

        writeData[0] = type;
        writeData[1] = (byte)(sector * BLOCK_NUM_PER_SECTOR);
        System.arraycopy(key, 0, writeData, 2, key.length);

        N_RFIDData rfidData = transfer(CMD_KEY_VERIFICATION, writeData);

        if(null != rfidData) {
            if(rfidData.getResult() == RESULT_OK) {
                // 验证密钥成功，标注该扇区不再需要验证
            	mKeyVerified.put(sector, true);
                Debug.d(TAG, "  ==> 密钥验证成功");
                return true;
            } else {
                mErrorMessage = "设备返回失败：" +  String.format("0x%02X", rfidData.getResult());
                Debug.e(TAG, mErrorMessage);
            }
        }

        // 验证密钥失败，标注该卡需要重新初始化
        mInitialized = false;

        Debug.e(TAG, "  ==> 密钥验证失败");

        return false;
    }

    private boolean verifySector(byte sector, byte type) {
        // 如果未进行初始化，或者访问中途失败后，需要重新初始化
        if(!mInitialized) {
            Debug.d(TAG, "  ==> 需要(重新)初始化");
            if(!initCard()) return false;	// 初始化失败返回失败
        }

        // 如果该扇区不需要再进行密钥验证，则直接返回成功
        if(mKeyVerified.get(sector)) {
            Debug.d(TAG, "  ==> 无需验证扇区" + sector + "的验证密钥");
            return true;
        }

        Debug.d(TAG, "  ==> 验证A密钥的缺省密钥");
        if(verifyKey(sector, type, DATA_KEY_DEFAULT)) return true;

        Debug.d(TAG, "  ==> 验证A密钥的唯一密钥");
        if(verifyKey(sector, type, 
        	(sector == 0 ? 
        	DATA_KEY_SECTOR0 : 
           	(type == DATA_KEY_A ? EncryptionMethod.getInstance().getKeyA(mUID) : EncryptionMethod.getInstance().getKeyB(mUID))))) return true;

        return false;
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

        if(!verifySector((byte)(block/BLOCK_NUM_PER_SECTOR), type)) return false;

        Debug.d(TAG, "  ==> 开始写入块[" + String.format("0x%02X", block) + "]的值[" + ByteArrayUtils.toHexString(data) + "]");

        byte[] writeData = new byte[data.length+1];
        writeData[0] = block;
        System.arraycopy(data, 0, writeData, 1, data.length);

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
        if(!verifySector((byte)(block/BLOCK_NUM_PER_SECTOR), type)) return null;

        Debug.d(TAG, "  ==> 开始读块[" + String.format("0x%02X", block) + "]");

        N_RFIDData rfidData = transfer(CMD_READ_BLOCK, new byte[] {block});

        if(null != rfidData) {
            if(rfidData.getResult() == RESULT_OK) {
                byte[] resData = rfidData.getData();
                if( null != resData && resData.length == 16) {
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
    public boolean writeMaxInkLevel(int level) {
        Debug.d(TAG, "  ==> 开始写入墨水最大值");

        if(writeBlock(DATA_KEY_A, BLOCK_INK_MAX, EncryptionMethod.getInstance().encryptInkLevel(level))) {
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
    public boolean writeInkLevel(int level) {
        Debug.d(TAG, "  ==> 开始写入墨水值");

        if(writeBlock(DATA_KEY_A, BLOCK_INKLEVEL, EncryptionMethod.getInstance().encryptInkLevel(level))) {
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
    public boolean writeCopyInkLevel(int level) {
        Debug.d(TAG, "  ==> 开始写入备份墨水值");

        if(writeBlock(DATA_KEY_A, BLOCK_COPY_INKLEVEL, EncryptionMethod.getInstance().encryptInkLevel(level))) {
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

    public boolean writeKey(byte sector, byte type, byte[] key) {
        Debug.d(TAG, "  ==> 开始写入密钥，扇区=" + sector + "; 密钥种类=" + type + "; 密钥值=[" + ByteArrayUtils.toHexString(key) + "]");

        byte[] data = readBlock(DATA_KEY_A, (byte)(sector * BLOCK_NUM_PER_SECTOR + BLOCK_KEY));
        if(null != data) {
            for (int i=0; i<Math.min(key.length, 6); i++) {
                data[(type == DATA_KEY_A ? i : i+10)] = key[i];
            }
            if(writeBlock(DATA_KEY_A, (byte)(sector * BLOCK_NUM_PER_SECTOR + BLOCK_KEY), data)) {
                Debug.d(TAG, "  ==> 写入密钥成功");
                mKeyVerified.put(sector, false);
                return true;
            }
        }

        Debug.e(TAG, "  ==> 写入密钥失败");

        return false;
    }

    public boolean writeSector0Key() {
        Debug.d(TAG, "  ==> 开始写入密钥，0扇区; ; 密钥值=[" + ByteArrayUtils.toHexString(DATA_KEY_SECTOR0) + "]");

        byte[] data = readBlock(DATA_KEY_A, (byte)(0 * BLOCK_NUM_PER_SECTOR + BLOCK_KEY));
        if(null != data) {
            for (int i=0; i<Math.min(DATA_KEY_SECTOR0.length, 6); i++) {
                data[i] = DATA_KEY_SECTOR0[i];
                data[i+10] = DATA_KEY_SECTOR0[i];
            }
            if(writeBlock(DATA_KEY_A, (byte)(0 * BLOCK_NUM_PER_SECTOR + BLOCK_KEY), data)) {
                Debug.d(TAG, "  ==> 写入密钥成功");
                return true;
            }
        }

        Debug.e(TAG, "  ==> 写入密钥失败");

        return false;
    }

    @Override
    public byte[] readUID() {
        Debug.d(TAG, "  ==> 开始读UID");

        byte[] data = read0Block();
        if(null != data) {
            byte[] uid = new byte[4];

            System.arraycopy(data, 0, uid, 0, uid.length);
            Debug.d(TAG, "  ==> 读UID成功[" + ByteArrayUtils.toHexString(uid) + "]");
            return uid;
        }

        Debug.e(TAG, "  ==> 读UID失败");

        return null;
    }

    public byte[] read0Block() {
        Debug.d(TAG, "  ==> 开始读0块");

        byte[] data = readBlock(DATA_KEY_A, (byte)(SECTOR_00 * BLOCK_NUM_PER_SECTOR + 0));
        if(null != data) {
            Debug.d(TAG, "  ==> 读读0块成功[" + ByteArrayUtils.toHexString(data) + "]");
            return data;
        }

        Debug.e(TAG, "  ==> 读0块失败");

        return null;
    }
}
