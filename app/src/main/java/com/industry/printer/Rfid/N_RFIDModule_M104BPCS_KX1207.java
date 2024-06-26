package com.industry.printer.Rfid;

import com.industry.printer.Utils.ByteArrayUtils;
import com.industry.printer.Utils.Debug;
import com.industry.printer.hardware.RFIDDevice;

public class N_RFIDModule_M104BPCS_KX1207 extends N_RFIDModule {
    private static final String TAG = N_RFIDModule_M104BPCS_KX1207.class.getSimpleName();

    // 寻卡
    // [报文] 02 00 00 04 20 04 28 03
    //      04为固定值
    // [返回] 02 00 00 0A 20 RR 56 E8 7B 7E 00 00 4B AC 03
    // 		 RR: 00 成功；非零：失败(FF:未插卡，F7卡不识别)；56 E8 7B 7E 00 00 4B: 7字节卡号
    // [例子] 02 00 00 0A 20 00 4B 00 00 00 0E 31 62 16 03
    // 		  成功，卡号：4B 00 00 00 0E 31 62
    public static final byte CMD_SEARCH_CARD = 0x20;
    public static final byte[] DATA_FIXED = {0x04};

    // 卡密钥验证. 此处需特别注意的地方:卡片密钥未开启前, 千万不要进行密钥验证操作.千万千万!!!
    // [报文] 02 00 00 08 99 3A 12 34 56 79 F0 03
    //      3A: 固定值（貌似密钥保存的页号）
    //      12 34 56 79:4字节密钥
    // [返回] 02 00 00 10 03 99 RR 9C 03
    // 		 RR: 00 成功；非零：失败.
    // * 此处需特别注意的地方:卡片密钥未开启前, 千万不要进行密钥验证操作.千万千万!!!
    // * 仅写操作需要验证密钥后方能正确执行, 读操作无需密钥验证
    public static final byte CMD_KEY_VERIFICATION = (byte) 0x99;
    public static final byte DATA_KEY_VERIFY = 0x3A;

    // 读页 - 实际上独到的是该页号以及其后连续的4个页
    // [报文] 02 00 00 04 4B 06 55 03
    //       06: 读数据的开始页码
    // [返回] 02 00 00 13 4B RR 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 5E 03
    // 		 RR: 00 成功；非零：失败.
    //       （16字节返回数据为从开始页码开始的连续4个页的内容）
    public static final byte CMD_READ_PAGE = 0x4B;

// H.M.Wang 2023-12-18 对应模块版本V0302，增加新的带密钥读页命令
    // 带密钥读页 - 实际上独到的是该页号以及其后连续的4个页
    // [报文] 02 00 00 0B 0E 04 3A 09 01 13 54 26 3D 2B 03
    //       04 3A: 固定值
    //       09: 读数据的开始页码
    //       01: 固定值
    //       13 54 26 3D: 密钥
    //       2B: 校验值
    // [返回] 02 00 00 13 0E RR 07 00 00 80 00 00 00 00 00 00 00 00 FF FF FF FF A4 03
    // 		 RR: 00 成功；非零：失败.
    //       （16字节返回数据为从开始页码开始的连续4个页的内容）
// End of H.M.Wang 2023-12-18 对应模块版本V0302，增加新的带密钥读页命令
    public static final byte CMD_NEW_READ_PAGE = 0x0E;

    // 写页
    // [报文] 02 00 00 08 35 06 00 00 00 01 44 03
    //       06：写数据的页号，00 00 00 01：为写入的4个字节数据
    // [返回] 02 00 00 10 03 35 RR 38 03
    // 		 RR: 00 成功；非零：失败.
    public static final byte CMD_WRITE_PAGE = 0x35;

// H.M.Wang 2023-12-18 对应模块版本V0302，增加新的带密钥写页命令
    // 带密钥写页
    // [报文] 02 00 00 0F 0F 04 3A 39 01 13 54 26 3D B9 FE 8C 97 3A 03
    //       04 3A: 固定值
    //       39：写数据的页号，00 00 00 01：为写入的4个字节数据
    //       01: 固定值
    //       13 54 26 3D: 密钥
    //       B9 FE 8C 97: 写入值
    //       3A: 校验值
    // [返回] 02 00 00 10 03 0F RR 12 03
    // 		 RR: 00 成功；非零：失败.
    public static final byte CMD_NEW_WRITE_PAGE = 0x0F;
// End of H.M.Wang 2023-12-18 对应模块版本V0302，增加新的带密钥写页命令

    // 设置密钥
    // [报文] 02 00 00 08 35 3A 12 34 56 79 78 03
    //      3A: 固定值（貌似密钥保存的页号）
    //      12 34 56 79:4字节密钥
    // [返回] 02 00 00 10 03 35 RR 38 03
    // 		 RR: 00 成功；非零：失败.
    // 需特别注意的是原厂卡片, 寻卡操作成功之后就执行此操作, 中间不能进行密码验证操作, 在真正启动密钥功能前可以读出验证是否写入数据正确。
    public static final byte CMD_WRITE_KEY = 0x35;
    public static final byte DATA_KEY_PAGE = 0x3A;

    // 启用密钥
    // [报文] 02 00 00 08 35 3F 00 00 40 00 BC 03
    //      3F: 固定值
    //      00 00 40 00:4字节启用固定值
    // [返回] 02 00 00 10 03 35 RR 38 03
    // 		 RR: 00 成功；非零：失败.
    // 需特别注意的是原厂卡片, 寻卡操作成功之后就执行此操作, 中间不能进行密码验证操作, 在真正启动密钥功能前可以读出验证是否写入数据正确。
    public static final byte CMD_ENABLE_KEY = 0x35;
    public static final byte[] DATA_ENABLE_KEY = {0x3F, 0x00, 0x00, 0x40, 0x00};

    // 0x00 - 0x03 为系统占用页
    private static final byte PAGE_FEATURE = 0x04;    // 0x04-0x07 为4页，16字节的特征值区
    private static final byte PAGE_MAX_LEVEL = 0x08;    // 1页，4个字节的锁值最大值
    private static final byte PAGE_QUICK_JUMP = 0x09;    // ILG管理表的快速索引，B1[2:1]与B0[7:0]每个位标识已经标注的ILG取得已标记4个页
    // 0x0A - 0x0B 为内部预留
    private static final byte PAGE_ILG_START = 0x0C;    // 12， ILG的开始页
    private static final byte PAGE_ILG_END = 0x33;    // 51， ILG的结束页（包含该页）
    // 0x34 - 0x39 为内部预留
    // 0x3A - 0x3F 为系统占用页

    private static final byte PAGES_PER_BLOCK = 4;
    private static final byte BYTES_PER_PAGE = 4;

    private static final byte KEY_CREATED_MARK = (byte) 0x80;

    private static boolean APK_USAGE = true;        // 设备使用
//    private static boolean APK_USAGE = false;     // 制卡工具使用

    private boolean mKeyExist = false;            // 密钥是否已经写入到了卡中，如果写入了则无需再写，如果没有写入，则禁止任何写入和密钥验证操作，否则卡就废了

    private int mBlockCnt = 0;
    private int mPageCnt = 0;
    private int mByteCnt = 0;
    private int mBitsCnt = 0;

    private int mMaxInkLevel = 0;

    // H.M.Wang 2023-12-3 修改锁值记录方法
//  【原方法】
//      1. 将锁值记载在 PAGE_ILG_START(0x0C) 到 PAGE_ILG_END(0x33) 之间的40页，160字节，1280位的空间中。
//      2. apk每打印完阈值次数的打印任务，减锁一次。
//      3. 如果最大墨水量小于等于1280，则一个点位对应于一次减锁。即每次减锁增记一位1
//      4. 如果最大墨水量大于1280，每一个点位对应于 (MAX/1280) 次减锁，即无论最大值是多少，都要映射到1280这个位图空间来记录消耗进度
//  【修改为】
//      1. 将锁值记载在 PAGE_ILG_START(0x0C) 到 PAGE_ILG_END(0x33) 之间的1000个位置空间中。
//    private static final int				ILG_MAX_BIT_COUNT = 1280;
    private static final int ILG_MAX_BIT_COUNT = 1000;
    //      2. 无论最大墨水量的值为多少，均将消耗进度映射到该位置空间中。即，从apk开来，不再参照具体的最大墨水量，而是参照一个固定值为1000的虚拟最大墨水量。实际最大墨水量与虚拟最大墨水量之间的比值最为一个参数
//    private float mStep = 0.0f;			// = Max(MAX / 1280（总刻度数）, 1)
    private float mStep = 0.0f;            // = MAX / 1000
//      3. apk通过该比值参数调整阈值。使得：新阈值 = 原阈值 * mStep
//          举例说明：
//          按着原方法的话，如实际最大墨水量=40，那么阈值 = 6018000 / 点数，每打印阈值次数即触发减锁一次。每减锁一次位图增加标记一位，共标记40位
//          如果按着新方法，实际最大墨水量仍=40，但是位图记录空间由于放大到了1000，增加了25倍。因此，需要增加触发减锁的次数（由原来的40次增加到1000次），因此，新的阈值就 = 6018000 / 点数 * mStep
// End of H.M.Wang 2023-12-3 修改锁值记录方法

//  ============================= Sector, Page, Byte, Bit 概念图示	=====================================================================================================
//  |-                                                                            Sector                                                                           -|
//  ----------------------------------------------------------------------------------------------------------------------------------------------------------------
//  |-              Page                   -|
//  ----------------------------------------
//  |-  Byte -|
//  -----------
//  |0000 0000|0000 0000|0000 0000|0000 0000|0000 0000|0000 0000|0000 0000|0000 0000|0000 0000|0000 0000|0000 0000|0000 0000|0000 0000|0000 0000|0000 0000|0000 0000|
//	|                                       |                                       |                                       |                                       |
//  =================================================================================================================================================================

//  ============================= 1207存储空间分配图示	=====================================================================================================
//  -- P0 ~ P3 系统占用
//  P0     |   SN0   |   SN1   |   SN2   |   BCC0  |
//  P1     |   SN3   |   SN4   |   SN5   |   SN6   |
//  P2     |   BCC1  |Internal |  Lock0  |  Lock1  |
//  P3     |         Capability Container          |
//  -- P4 ~ P7 特征值保存区
//  P4     |   F0    |   F1    |   F2    |   F3    |
//  P5     |   F4    |   F5    |   F6    |   F7    |
//  P6     |   F8    |   F9    |   F10   |   F11   |
//  P7     |   F12   |   F13   |   F14   |   F15   |
//  -- P8 最大值保存区
//  P8     |   LSB   |   ->    |   ->    |   MSB   |   MAX = (((B3*256)+B2)*256+B1)*256+B0
//  -- P9 块标识区
//  P9     |   B0    |   B1    |   B2    |   B3    |   B1:B0 = [0000 0000 0000 0000] 后11位标识当前块位置； B3=0x80:表示此卡已经写入密钥；=0x00:表示未写入密钥
//  -- P10 ~ P11 预留
//  P10    |0000 0000|0000 0000|0000 0000|0000 0000|
//  P11    |0000 0000|0000 0000|0000 0000|0000 0000|
//  -- P12 ~ P51 墨量消耗记录点位区（共1280个记录点位）
//  P12    |0000 0000|0000 0000|0000 0000|0000 0000|   1个块包含4个页，1个页包含4个字节，1个字节有8个标记位。标记顺序位S0:P0:B0:b0开始顺序将上一位标记为1
//  P13    |0000 0000|0000 0000|0000 0000|0000 0000|
//  P14    |0000 0000|0000 0000|0000 0000|0000 0000|
//  P15    |0000 0000|0000 0000|0000 0000|0000 0000|
//  ~
//  P51    |0000 0000|0000 0000|0000 0000|0000 0000|
//  -- P52 ~ P57 预留
//  P52    |0000 0000|0000 0000|0000 0000|0000 0000|    2023-12-18 写入密钥的0xAA异或值，用来验证卡内是否使用了正确的密钥
//  ~
//  P57    |0000 0000|0000 0000|0000 0000|0000 0000|
//  -- P58 ~ P63 系统占用
//  P58    |   PW0   |   PW1   |   PW2   |   PW3   |
//  P59    |  Key0   |  Key1   |  Key2   |  Key3   |
//  P60    |  Key4   |  Key5   |  Key6   |  Key7   |
//  P61    |  Key8   |  Key9   |  Key10  |  Key11  |
//  P62    |  Key12  |  Key13  |  Key14  |  Key15  |
//  P63    |  Lock2  |  Lock3  |  Lock4  |   RFU   |
//  =====================================================================================================================================================

    private void initData() {
        mKeyExist = false;

        mBlockCnt = 0;
        mPageCnt = 0;
        mByteCnt = 0;
        mBitsCnt = 0;

        mStep = 0.0f;
        mMaxInkLevel = 0;
    }

    public N_RFIDModule_M104BPCS_KX1207() {
        super();

        initData();
    }

    @Override
    public boolean searchCard() {
        Debug.d(TAG, "  ==> 开始寻卡");

        N_RFIDData rfidData = transfer(CMD_SEARCH_CARD, DATA_FIXED);

        if (null != rfidData) {
            if (rfidData.getResult() == RESULT_OK) {    // 设备返回处理结果为成功，而非失败
                byte[] resData = rfidData.getData();
                if (null != resData && resData.length == 7) {    // 4个字节卡号；如果是1207卡也可以查询到，其卡号为7个字节，但是对于本模块不适用
                    mUID = resData;
                    mCardType = CARD_TYPE_SUPPORT;
                    Debug.d(TAG, "  ==> 寻卡成功，获得卡号：[" + ByteArrayUtils.toHexString(mUID) + "]");
                    return true;
                } else {
                    mErrorMessage = "不支持的卡：[" + (null != resData ? ByteArrayUtils.toHexString(resData) : "null") + "]";
                    Debug.e(TAG, mErrorMessage);
                }
            } else if (rfidData.getResult() == (byte) 0xFF) {
                mCardType = CARD_TYPE_NO_CARD;
                mErrorMessage = "设备返回失败：未插卡";
                Debug.e(TAG, mErrorMessage);
            } else if (rfidData.getResult() == (byte) 0xF7) {
                mCardType = CARD_TYPE_NO_SUPPORT;
                mErrorMessage = "设备返回失败：不支持的卡";
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

    private boolean writePage(byte page, byte[] data) {
        if (page < 0x04 || page > 0x39) {
            mErrorMessage = "错误的页号";
            Debug.e(TAG, mErrorMessage);
            return false;
        }

        if (null == data) {
            mErrorMessage = "数据无效：" + null;
            Debug.e(TAG, mErrorMessage);
            return false;
        }

        if (data.length != BYTES_PER_PAGE) {
            mErrorMessage = "数据长度错误：" + data.length;
            Debug.e(TAG, mErrorMessage);
            return false;
        }

        Debug.d(TAG, "  ==> 开始写入页[" + String.format("0x%02X", page) + "]的值[" + ByteArrayUtils.toHexString(data) + "]");

        N_RFIDData rfidData;

        if(APK_USAGE) {
            byte[] sendData = new byte[12];
            sendData[0] = 0x04;
            sendData[1] = 0x3A;
            sendData[2] = page;
            sendData[3] = 0x01;
            System.arraycopy(calKey(), 0, sendData, 4, 4);
            System.arraycopy(data, 0, sendData, 8, 4);
            rfidData = transfer(CMD_NEW_WRITE_PAGE, sendData);
        } else {
            byte[] writeData = new byte[data.length + 1];
            writeData[0] = page;
            System.arraycopy(data, 0, writeData, 1, data.length);
            rfidData = transfer(CMD_WRITE_PAGE, writeData);
        }

        if (null != rfidData) {
            if (rfidData.getResult() == RESULT_OK) {
                Debug.d(TAG, "  ==> 写入页成功");
                return true;
            } else {
                mErrorMessage = "设备返回失败：" + String.format("0x%02X", rfidData.getResult());
                Debug.e(TAG, mErrorMessage);
            }
        }

        Debug.e(TAG, "  ==> 写入页失败. 尝试重新初始化模块");

        if(APK_USAGE) initCard();

        return false;
    }

    private byte[] readPage(byte page) {
        if (page < 0x04 || page > 0x39) {
            mErrorMessage = "错误的页号";
            Debug.e(TAG, mErrorMessage);
            return null;
        }

        Debug.d(TAG, "  ==> 开始读页[" + String.format("0x%02X", page) + "]");

        N_RFIDData rfidData;

        if(APK_USAGE) {
            byte[] sendData = new byte[8];
            sendData[0] = 0x04;
            sendData[1] = 0x3A;
            sendData[2] = page;
            sendData[3] = 0x01;
            System.arraycopy(calKey(), 0, sendData, 4, 4);
            rfidData = transfer(CMD_NEW_READ_PAGE, sendData);
        } else {
            rfidData = transfer(CMD_READ_PAGE, new byte[]{page});
        }

        if (null != rfidData) {
            if (rfidData.getResult() == RESULT_OK) {
                byte[] resData = rfidData.getData();
                if (null != resData && resData.length == 4 * BYTES_PER_PAGE) {        // 读到的16字节页值
                    Debug.d(TAG, "  ==> 成功读取页[" + String.format("0x%02X", page) + "(共4页)]的值[" + ByteArrayUtils.toHexString(resData) + "]");
                    return resData;
                } else {
                    mErrorMessage = "数据空或长度错误：[" + (null != resData ? ByteArrayUtils.toHexString(resData) : "null") + "]";
                    Debug.e(TAG, mErrorMessage);
                }
            } else {
                mErrorMessage = "设备返回失败：" + String.format("0x%02X", rfidData.getResult());
                Debug.e(TAG, mErrorMessage);
            }
        }

        Debug.e(TAG, "  ==> 读页失败. 尝试重新初始化模块");

        if(APK_USAGE) initCard();

        return null;
    }

    public void readAllPages() {
        Debug.d(TAG, "========================= Read All Pages Start =========================");
        for(int i=0; i<64; i+=4) {
            N_RFIDData rfidData;
            if(APK_USAGE) {
                byte[] sendData = new byte[8];
                sendData[0] = 0x04;
                sendData[1] = 0x3A;
                sendData[2] = (byte)i;
                sendData[3] = 0x01;
                System.arraycopy(calKey(), 0, sendData, 4, 4);
                rfidData = transfer(CMD_NEW_READ_PAGE, sendData);
            } else {
                rfidData = transfer(CMD_READ_PAGE, new byte[]{(byte)i});
            }
            if (null != rfidData) {
                if (rfidData.getResult() == RESULT_OK) {
                    byte[] resData = rfidData.getData();
                    if (null != resData && resData.length == 4 * BYTES_PER_PAGE) {
                        Debug.d(TAG, "[" + ByteArrayUtils.toHexString(resData) + "]");
                    }
                }
            }
        }
        Debug.d(TAG, "========================= Read All Pages End =========================");
    }

// H.M.Wang 2023-12-18 向P57(H39)写入密钥的0xAA异或值，如果写入成功，则说明卡已经写入了正确的密钥，否则则没有写入正确密钥，不允许继续使用
    public boolean tryWrite() {
        byte[] verKey = calKey();
        for(int i=0; i<verKey.length; i++) {
            verKey[i] = (byte)(verKey[i] ^ 0xAA);
        }
        return writePage((byte)0x34, verKey);
    }
// End of H.M.Wang 2023-12-18 向P57(H39)写入密钥的0xAA异或值，如果写入成功，则说明卡已经写入了正确的密钥，否则则没有写入正确密钥，不允许继续使用

    private byte[] calKey() {
        if (null == mUID || mUID.length != 7) return null;
// H.M.Wang 2023-12-20 算法移至HardwareJni中计算
        return RFIDDevice.calKey(mUID);
    }

    public boolean verifyKey() {
        if (!mKeyExist) {
            mErrorMessage = "本卡片还没有写入密钥，不能验证";
            Debug.e(TAG, mErrorMessage);
            return false;
        }

        byte[] key = calKey();
        if (null == key) {
            mErrorMessage = "卡号有问题，无法生成密钥";
            Debug.e(TAG, mErrorMessage);
            return false;
        }

        Debug.d(TAG, "  ==> 开始验证密钥[" + ByteArrayUtils.toHexString(key) + "]");

        byte[] sendData = new byte[1 + key.length];
        sendData[0] = DATA_KEY_PAGE;
        System.arraycopy(key, 0, sendData, 1, key.length);

        N_RFIDData rfidData = transfer(CMD_KEY_VERIFICATION, sendData);

        if (null != rfidData) {
            if (rfidData.getResult() == RESULT_OK) {
                Debug.d(TAG, "  ==> 密钥验证成功");
                return true;
            } else {
                mErrorMessage = "验证失败：" + String.format("0x%02X", rfidData.getResult());
                Debug.e(TAG, mErrorMessage);
            }
        }

        Debug.e(TAG, "  ==> 密钥验证失败");

        return false;
    }

    protected boolean writeKeyMark() {
        if(APK_USAGE) return false;

        Debug.d(TAG, "  ==> 开始写入密钥注册标记");

        byte[] readBytes = readPage(PAGE_QUICK_JUMP);
        if (null != readBytes) {
            byte[] writeBytes = new byte[]{readBytes[0], readBytes[1], readBytes[2], readBytes[3]};
            writeBytes[3] |= KEY_CREATED_MARK;

            if (writePage(PAGE_QUICK_JUMP, writeBytes)) {
                // 验证密钥成功，标注该块不再需要验证
                Debug.d(TAG, "  ==> 成功写入密钥注册标记");
                mKeyExist = true;
                return true;
            }
        }

        Debug.e(TAG, "  ==> 密钥注册标记写入失败");

        return false;
    }

    private boolean readKeyMark() {
        Debug.d(TAG, "  ==> 开始读取密钥注册标记");

        mKeyExist = false;

        byte[] readBytes = readPage(PAGE_QUICK_JUMP);
        if (null != readBytes) {
            if ((readBytes[3] & (byte) KEY_CREATED_MARK) == (byte) KEY_CREATED_MARK) {
                mKeyExist = true;
                Debug.d(TAG, "  ==> 密钥注册标记已写入。使用该卡需要先验证密钥");
            } else {
                Debug.d(TAG, "  ==> 密钥注册标记未写入。执行验证密钥操作会废掉");
            }
            return true;
        }

        Debug.e(TAG, "  ==> 读取密钥注册标记失败");

        return false;
    }

    @Override
    public boolean initCard() {
        return (searchCard() ? readKeyMark() : false);
    }

    @Override
    public boolean writeMaxInkLevel(int max) {
        Debug.d(TAG, "  ==> 开始写入墨水最大值");

        if (mKeyExist) {
            Debug.e(TAG, "  ==> 此卡已经完成了制卡，不能再次制卡");
            return false;
        }

        byte[] maxbytes = new byte[BYTES_PER_PAGE];

        maxbytes[0] = (byte) (max & 0x0ff);
        maxbytes[1] = (byte) ((max >> 8) & 0x0ff);
        maxbytes[2] = (byte) ((max >> 16) & 0x0ff);
        maxbytes[3] = (byte) ((max >> 24) & 0x0ff);

        if (writePage(PAGE_MAX_LEVEL, maxbytes)) {
            Debug.d(TAG, "  ==> 写入墨水最大值成功");
            mMaxInkLevel = max;
            return true;
        }

        Debug.e(TAG, "  ==> 写入墨水最大值失败");

        return false;
    }

    @Override
    public int readMaxInkLevel() {
        Debug.d(TAG, "  ==> 开始读墨水最大值");

        byte[] data = readPage(PAGE_MAX_LEVEL);
        if (null != data) {
            int max = 0;

            for (int i = 3; i >= 0; i--) {
                max = max * 256 + (data[i] & 0x0ff);
            }

            mStep = 1.0f * max / ILG_MAX_BIT_COUNT;
            Debug.d(TAG, "  ==> mStep = [" + mStep + "]");
// H.M.Wang 2023-12-3 修改锁值记录方法。不再强制mStep必须大于等1。
//            if(mStep < 1.0f) mStep = 1.0f;
// End H.M.Wang 2023-12-3 修改锁值记录方法。不再强制mStep必须大于等1。

            Debug.d(TAG, "  ==> 读墨水最大值成功[" + max + "]（实际值）。 mStep = " + mStep);
// H.M.Wang 2023-12-3 修改锁值记录方法。虚拟墨水最大值固定为 ILG_MAX_BIT_COUNT
//            mMaxInkLevel = max;
            mMaxInkLevel = ILG_MAX_BIT_COUNT;
// End H.M.Wang 2023-12-3 修改锁值记录方法。虚拟墨水最大值固定为 ILG_MAX_BIT_COUNT
            return mMaxInkLevel;
        }

        Debug.e(TAG, "  ==> 读墨水最大值失败");

        return 0;
    }

    @Override
    public boolean writeInkLevel(int inkLevel) {
        Debug.d(TAG, "  ==> 开始写入墨水值");

        if (mMaxInkLevel <= 0) {
            mErrorMessage = "无有效的总墨水值";
            Debug.e(TAG, mErrorMessage);
            return false;
        }

// H.M.Wang 2023-12-3 修改锁值记录方法。由于mStep已经在阈值的计算当中被参照，因此这里就不再做调整
//        inkLevel = Math.round(((mMaxInkLevel - inkLevel) / mStep));
        inkLevel = mMaxInkLevel - inkLevel;
// End of H.M.Wang 2023-12-3 修改锁值记录方法。由于mStep已经在阈值的计算当中被参照，因此这里就不再做调整

        byte blockCnt = (byte) (inkLevel / (PAGES_PER_BLOCK * BYTES_PER_PAGE * 8));
        byte pageCnt = (byte) (inkLevel % (PAGES_PER_BLOCK * BYTES_PER_PAGE * 8) / (BYTES_PER_PAGE * 8));
        byte byteCnt = (byte) (inkLevel % (BYTES_PER_PAGE * 8) / 8);
        byte bitCnt = (byte) (inkLevel % 8);

        Debug.d(TAG, "InkLevel = " + inkLevel);
        Debug.d(TAG, "[新的] blockCnt=" + blockCnt + "; pageCnt=" + pageCnt + "; byteCnt=" + byteCnt + "; bitCnt=" + bitCnt);

        if (blockCnt != mBlockCnt) {
            writeBlockCnt(blockCnt);
        } else {
            Debug.d(TAG, "  ==> 块索引没有变化，不需写入块索引");
        }

        if (pageCnt != mPageCnt) {
            for (byte writeInPage = (byte) (PAGE_ILG_START + mBlockCnt * PAGES_PER_BLOCK + mPageCnt); writeInPage < (byte) (PAGE_ILG_START + blockCnt * PAGES_PER_BLOCK + pageCnt); writeInPage++) {
                if (writeInPage < PAGE_ILG_START || writeInPage > PAGE_ILG_END) {
                    Debug.e(TAG, "  ==> 页码超出范围");
                    return false;
                }

                byte[] sendData = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};

                if (!writePage(writeInPage, sendData)) {
                    Debug.e(TAG, "  ==> 写入封存前页值失败");
                    return false;
                }
            }
            Debug.d(TAG, "  ==> 写入封存前页值成功");
        } else {
            Debug.d(TAG, "  ==> 页码没有变化，不需写入");
        }

        if (byteCnt != mByteCnt || bitCnt != mBitsCnt) {
            byte writeInPage = (byte) (PAGE_ILG_START + blockCnt * PAGES_PER_BLOCK + pageCnt);
            if (writeInPage < PAGE_ILG_START || writeInPage > PAGE_ILG_END) {
                Debug.e(TAG, "  ==> 页码超出范围");
                return false;
            }

            byte bitmap = 0x00;
            byte tmpBitCnt = bitCnt;
            for (; tmpBitCnt > 0; tmpBitCnt--) {
                bitmap <<= 1;
                bitmap |= 0x01;
            }
            byte[] sendData = new byte[BYTES_PER_PAGE];
            for (int i = 0; i < BYTES_PER_PAGE; i++) {
                if (i < byteCnt) {
                    sendData[i] = (byte) 0xFF;
                } else if (i > byteCnt) {
                    sendData[i] = (byte) 0x00;
                } else {
                    sendData[i] = bitmap;
                }
            }

            if (!writePage(writeInPage, sendData)) {
                Debug.e(TAG, "  ==> 写入墨水刻度值失败");
                return false;
            }
            Debug.d(TAG, "  ==> 写入墨水刻度值成功");
        } else {
            Debug.d(TAG, "  ==> 墨水刻度值没有变化，不需写入");
        }

        mBlockCnt = blockCnt;
        mPageCnt = pageCnt;
        mByteCnt = byteCnt;
        mBitsCnt = bitCnt;

//        Debug.d(TAG, "  ==> 写入墨水值成功");

        return true;
    }

    private boolean writeBlockCnt(byte blockCnt) {
        Debug.d(TAG, "  ==> 开始写块索引");

        byte[] readBytes = readPage(PAGE_QUICK_JUMP);
        if (null != readBytes) {            // 如果有设备，肯定会在超时(3秒)之前返回数值，否则，超时返回空
            short blockBitmap = 0x00;
            for (byte tmpBlockCnt = blockCnt; tmpBlockCnt > 0; tmpBlockCnt--) {
                blockBitmap <<= 1;
                blockBitmap |= 0x01;
            }

            byte[] sendData = new byte[BYTES_PER_PAGE];
            sendData[0] = (byte) (blockBitmap & 0x0FF);
            sendData[1] = (byte) ((blockBitmap >> 8) & 0x0FF);
            sendData[2] = readBytes[2];
            sendData[3] = readBytes[3];

            if (writePage(PAGE_QUICK_JUMP, sendData)) {
                Debug.d(TAG, "  ==> 写块索引成功");
                return true;
            }
        }

        Debug.e(TAG, "  ==> 写块索引失败");
        return false;
    }

    private byte readBlockCnt() {
        Debug.d(TAG, "  ==> 开始读取块索引");

        byte[] readBytes = readPage(PAGE_QUICK_JUMP);
        if (null != readBytes) {            // 如果有设备，肯定会在超时(3秒)之前返回数值，否则，超时返回空
            short blockBitmap = (short) (readBytes[1] * 256 + readBytes[0]);
            byte blockCnt = 0;
            for (; (blockBitmap & 0x0001) == 0x0001; blockBitmap >>= 1) {
                blockCnt++;
            }
            Debug.d(TAG, "  ==> 读取块索引成功。[" + blockCnt + "]");
            return blockCnt;
        }

        Debug.e(TAG, "  ==> 读取块索引失败");

        return -1;
    }

    @Override
    public int readInkLevel() {
        Debug.d(TAG, "  ==> 开始读取墨水值");

        if (mMaxInkLevel <= 0) {
            mErrorMessage = "无有效的总墨水值";
            Debug.e(TAG, mErrorMessage);
            return 0;
        }

        byte blockCnt = readBlockCnt();
        if (blockCnt >= 0) {
            byte[] pagesBitmap = readPage((byte) (PAGE_ILG_START + blockCnt * PAGES_PER_BLOCK));
            if (null != pagesBitmap) {
                try {
                    byte pageCnt = PAGES_PER_BLOCK, byteCnt = 0, bitCnt = 0;

                    for (int i = 0; i < pagesBitmap.length; i++) {
                        if (pagesBitmap[i] != (byte) 0xFF) {
                            pageCnt = (byte) (i / BYTES_PER_PAGE);
                            byteCnt = (byte) (i % BYTES_PER_PAGE);
                            break;
                        }
                    }

                    for (byte bitmap = pagesBitmap[pageCnt * PAGES_PER_BLOCK + byteCnt]; (bitmap & 0x01) == 0x01; bitmap >>= 1) {
                        bitCnt++;
                    }

                    Debug.d(TAG, "blockCnt=" + blockCnt + "; pageCnt=" + pageCnt + "; byteCnt=" + byteCnt + "; bitCnt=" + bitCnt);
                    int inkLevel = ((blockCnt * PAGES_PER_BLOCK * BYTES_PER_PAGE + pageCnt * BYTES_PER_PAGE + byteCnt) * 8 + bitCnt);
                    Debug.d(TAG, "InkLevel = " + inkLevel);

                    mBlockCnt = blockCnt;
                    mPageCnt = pageCnt;
                    mByteCnt = byteCnt;
                    mBitsCnt = bitCnt;

// H.M.Wang 2023-12-3 修改锁值记录方法。由于mStep已经在阈值的计算当中被参照，因此这里就不再做调整
//                    return (mMaxInkLevel - Math.round(inkLevel * mStep));
                    return (mMaxInkLevel - inkLevel);
// End of H.M.Wang 2023-12-3 修改锁值记录方法。由于mStep已经在阈值的计算当中被参照，因此这里就不再做调整
                } catch (Exception e) {
                    mErrorMessage = e.getMessage();
                    Debug.e(TAG, e.getMessage());
                }
            }
        }

        Debug.e(TAG, "  ==> 读取墨水值失败");

        return 0;
    }

    @Override
    public boolean writeFeature(byte[] feature) {
        Debug.d(TAG, "  ==> 开始写入特征值");

        if (mKeyExist) {
            mErrorMessage = "此卡已经完成了制卡，不能再次制卡";
            Debug.e(TAG, mErrorMessage);
            return false;
        }

        if (null == feature) {
            mErrorMessage = "特征值无数据";
            Debug.e(TAG, mErrorMessage);
            return false;
        }

        if (feature.length != 16) {
            mErrorMessage = "特征值数据长度不正确";
            Debug.e(TAG, mErrorMessage);
            return false;
        }

        for (int i = 0; i < feature.length; i += 4) {
            byte[] writeData = new byte[]{feature[i], feature[i + 1], feature[i + 2], feature[i + 3]};
            if (!writePage((byte) (PAGE_FEATURE + i / 4), writeData)) {
                Debug.e(TAG, "  ==> 写入特征值失败");
                return false;
            }
        }

        Debug.d(TAG, "  ==> 写入特征值成功");

        return true;
    }

    @Override
    public byte[] readFeature() {
        Debug.d(TAG, "  ==> 开始读取特征值");

        byte[] readData = readPage(PAGE_FEATURE);
        if (null != readData) {
            Debug.d(TAG, "  ==> 读取特征值成功[" + ByteArrayUtils.toHexString(readData) + "]");
        } else {
            Debug.e(TAG, "  ==> 读取特征值失败");
        }

        return readData;
    }

    @Override
    public boolean writeCopyInkLevel(int ink) {
        Debug.e(TAG, "  ==> 不支持备份墨水值写入功能");

        return false;
    }

    @Override
    public int readCopyInkLevel() {
        Debug.e(TAG, "  ==> 不支持备份墨水值读取功能");

        return 0;
    }

    @Override
    public byte[] readUID() {
        Debug.d(TAG, "  ==> 开始读UID");

        N_RFIDData rfidData = transfer(CMD_SEARCH_CARD, DATA_FIXED);

        if (null != rfidData) {
            if (rfidData.getResult() == RESULT_OK) {    // 设备返回处理结果为成功，而非失败
                byte[] resData = rfidData.getData();
                if (null != resData && resData.length == 7) {    // 4个字节卡号；如果是1207卡也可以查询到，其卡号为7个字节，但是对于本模块不适用
                    Debug.d(TAG, "  ==> 获得卡号：[" + ByteArrayUtils.toHexString(mUID) + "]");
                    return resData;
                } else {
                    mErrorMessage = "不支持的卡：[" + (null != resData ? ByteArrayUtils.toHexString(resData) : "null") + "]";
                    Debug.e(TAG, mErrorMessage);
                }
            } else if (rfidData.getResult() == (byte) 0xFF) {
                mErrorMessage = "设备返回失败：未插卡";
                Debug.e(TAG, mErrorMessage);
            } else if (rfidData.getResult() == (byte) 0xF7) {
                mErrorMessage = "设备返回失败：不支持的卡";
                Debug.e(TAG, mErrorMessage);
            } else {
                mErrorMessage = "设备返回失败：" + String.format("0x%02X", rfidData.getResult());
                Debug.e(TAG, mErrorMessage);
            }
        }

        Debug.e(TAG, "  ==> 读UID失败");
        return null;
    }

    protected boolean writeKey() {
        if(APK_USAGE) return false;

        Debug.d(TAG, "  ==> 开始写入密钥");

        byte[] key = calKey();
        if (null == key) {
            mErrorMessage = "卡号有问题，无法生成密钥";
            Debug.e(TAG, mErrorMessage);
            return false;
        }

        Debug.d(TAG, "  ==> 开始写入密钥[" + ByteArrayUtils.toHexString(key) + "]");

        byte[] sendData = new byte[1 + key.length];
        sendData[0] = DATA_KEY_PAGE;
        System.arraycopy(key, 0, sendData, 1, key.length);

        N_RFIDData rfidData = transfer(CMD_WRITE_KEY, sendData);

        if (null != rfidData) {
            if (rfidData.getResult() == RESULT_OK) {
                // 验证密钥成功，标注该块不再需要验证
                Debug.d(TAG, "  ==> 写入密钥成功");
                return true;
            } else {
                mErrorMessage = "设备返回失败：" + String.format("0x%02X", rfidData.getResult());
                Debug.e(TAG, mErrorMessage);
            }
        }

        Debug.e(TAG, "  ==> 写入密钥失败");

        return false;
    }

    protected boolean enableKey() {
        if(APK_USAGE) return false;

        Debug.d(TAG, "  ==> 开始启用密钥");

        N_RFIDData rfidData = transfer(CMD_ENABLE_KEY, DATA_ENABLE_KEY);

        if (null != rfidData) {
            if (rfidData.getResult() == RESULT_OK) {
                // 验证密钥成功，标注该块不再需要验证
                Debug.d(TAG, "  ==> 成功启用密钥");
                return true;
            } else {
                mErrorMessage = "设备返回失败：" + String.format("0x%02X", rfidData.getResult());
                Debug.e(TAG, mErrorMessage);
            }
        }

        Debug.e(TAG, "  ==> 启用密钥失败");

        return false;
    }

    public boolean isKeyExist() {
        return mKeyExist;
    }

    @Override
    public float getMaxRatio() {
        return mStep;
    }
}
