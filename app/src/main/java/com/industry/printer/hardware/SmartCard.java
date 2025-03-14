package com.industry.printer.hardware;

import com.industry.printer.Utils.Debug;

/**
 * Created by hmwan on 2019/10/20.
 */

public class SmartCard {
    static {
		System.loadLibrary("smartcard");
    }

// H.M.Wang 2024-8-16 内部测试版本特殊功能。(1) 墨盒代替墨袋，(2) 阈值由 138->280 (平时为正常版本)
    public final static int FUNC_TYPE_NORMAL                = 0;
    public final static int FUNC_TYPE_INTERNAL              = 1;
// H.M.Wang 2024-11-25 FUNC_TYPE修改为从参数95获取，而非硬码
////    public final static int FUNC_TYPE                       = FUNC_TYPE_NORMAL;
//    public final static int FUNC_TYPE                       = FUNC_TYPE_INTERNAL;
    public static int FUNC_TYPE                       = FUNC_TYPE_NORMAL;
// End of H.M.Wang 2024-11-25 FUNC_TYPE修改为从参数95获取，而非硬码
// End of H.M.Wang 2024-8-16 内部测试版本特殊功能。(1) 墨盒代替墨袋，(2) 阈值由 138->280

// H.M.Wang 2024-11-25 增加两个参数，参数95=小卡自有天线模式启动标识，0=正常模式；1=小卡自有天线模式。参数96=小卡自由天线模式加墨阈值，缺省0=310；其余值=实际值。当FUNC_TYPE=INTERNAL时，加墨阈值修改为从参数96获取，正常时仍为138
//    public int ADD_INK_THRESHOLD = 14000000;
//    public int ADD_INK_THRESHOLD = 13900000;
    public static int ADD_INK_THRESHOLD = 13800000;
//    public int ADD_INK_THRESHOLD = 13700000;
//    public int ADD_INK_THRESHOLD = 13600000;
//    public int ADD_INK_THRESHOLD = 13500000;

    public static void setType(int ftype, int addink) {
        FUNC_TYPE = ftype;
        if(FUNC_TYPE == FUNC_TYPE_INTERNAL) {
            CARD_TYPE_BULK = CARD_TYPE_BULKX;
            ADD_INK_THRESHOLD = (addink == 0 ? 31000000 : addink * 100000);
        } else {
            CARD_TYPE_BULK = CARD_TYPE_BULK1;
            ADD_INK_THRESHOLD = 13800000;
        }
Debug.d("SmartCard", "FUNC_TYPE = " + (FUNC_TYPE == FUNC_TYPE_INTERNAL ? "INTERNAL" : "NORMAL") + "; CARD_TYPE_BULK = " + (FUNC_TYPE == FUNC_TYPE_INTERNAL ? "BULKX" : "BULK1") + "; ADD_INK_THRESHOLD = " + ADD_INK_THRESHOLD);
    }
// End of H.M.Wang 2024-11-25 当FUNC_TYPE=INTERNAL时，加墨阈值修改为从参数96获取，正常时仍为138

    public final static int CARD_TYPE_PEN1                  = 11;
    public final static int CARD_TYPE_PEN2                  = 12;
    private final static int CARD_TYPE_BULK1                 = 13;               // 真实墨袋
    private final static int CARD_TYPE_BULKX                 = 14;             // 墨盒代替墨袋
// H.M.Wang 2024-11-25 CARD_TYPE_BULK修改为从参数95获取，而非硬码
////    public static int CARD_TYPE_BULK                 = CARD_TYPE_BULK1;
//    public static int CARD_TYPE_BULK                 = CARD_TYPE_BULKX;
    public static int CARD_TYPE_BULK                 = CARD_TYPE_BULK1;
// End of H.M.Wang 2024-11-25 CARD_TYPE_BULK修改为从参数95获取，而非硬码
    public final static int CARD_TYPE_LEVEL1                = 21;
    public final static int CARD_TYPE_LEVEL2                = 22;
    public final static int SC_FAILED                       = -1;
    public final static int SC_SUCCESS                      = 0;
    public final static int SC_INIT_HOST_CARD_NOT_PRESENT   = 100;
    public final static int SC_INIT_PRNT_CTRG_NOT_PRESENT   = 110;
    public final static int SC_INIT_BULK_CTRG_NOT_PRESENT   = 111;
    public final static int SC_INIT_PRNT_CTRG_INIT_FAILED   = 120;
    public final static int SC_INIT_BULK_CTRG_INIT_FAILED   = 121;
    public final static int SC_PRINT_CARTRIDGE_ACCESS_FAILED = 200;
    public final static int SC_BULK_CARTRIDGE_ACCESS_FAILED = 201;
    public final static int SC_LEVEL_CENSOR_ACCESS_FAILED   = 202;
    public final static int SC_CONSISTENCY_FAILED           = 300;
    public final static int SC_OUT_OF_INK_ERROR             = 301;
    public final static int SC_CHECKSUM_FAILED              = 400;

    public static void loadLibrary() {
        Debug.d("SmartCard", "Loading smartcard library...");
// H.M.Wang 2024-8-16 内部测试版本特殊功能。(1) 墨盒代替墨袋，(2) 阈值由 138->280 (平时为正常版本)
        if(FUNC_TYPE == FUNC_TYPE_INTERNAL) CARD_TYPE_BULK = CARD_TYPE_BULKX;
// End of H.M.Wang 2024-8-16 内部测试版本特殊功能。(1) 墨盒代替墨袋，(2) 阈值由 138->280 (平时为正常版本)
    }

    /**
     * SmartCard JNI APIs
     **/
    static public native int exist(int imgtype, int i2cgroupid);

    static public native int init();

    static public native int initComponent(int card);

// H.M.Wang 2022-11-1 Add this API for Bagink Use
    static public native int initLevelDirect();
// End of H.M.Wang 2022-11-1 Add this API for Bagink Use

    static public native int writeCheckSum(int card, int clientUniqueCode);

    static public native int checkSum(int card, int clientUniqueCode);

    static public native int checkConsistency(int card, int supply);

    static public native int getMaxVolume(int card);

    static public native String readConsistency(int card);

    static public native int checkOIB(int card);

    static public native int getLocalInk(int card);

    static public native int downLocal(int card);

    static public native int writeOIB(int card);

    static public native int readLevel(int card, int min, int max);

// H.M.Wang 2024-5-27 临时追加一个DAC5571的设置功能
    static public native int writeDAC5571(int value);
// End of H.M.Wang 2024-5-27 临时追加一个DAC5571的设置功能
// H.M.Wang 2024-5-24 临时追加一个ADS1115芯片的读数功能
    static public native int readADS1115(int index);
// End of H.M.Wang 2024-5-24 临时追加一个ADS1115芯片的读数功能

// H.M.Wang 2022-12-24 追加一个读写HX24LC芯片的功能，用来保存对应Bagink墨位的调整值
    static public native int readHX24LC();
    static public native int writeHX24LC(int value);
// End of H.M.Wang 2022-12-24 追加一个读写HX24LC芯片的功能，用来保存对应Bagink墨位的调整值

// H.M.Wang 2024-8-6 增加一个判断Level测量芯片种类的函数，apk会根据不同的芯片种类，执行不同的逻辑。读取Level值也会根据不同的种类而调用不同的接口
    static public native int getLevelType(int index);
// End of H.M.Wang 2024-8-6 增加一个判断Level测量芯片种类的函数，apk会根据不同的芯片种类，执行不同的逻辑。读取Level值也会根据不同的种类而调用不同的接口
// H.M.Wang 2022-11-1 Add this API for Bagink Use
    static public native int readLevelDirect(int index);
// End of H.M.Wang 2022-11-1 Add this API for Bagink Use

// H.M.Wang 2024-7-4 追加一个MCP-H21系列芯片测量压力的读写功能
    static public native int readMCPH21Level(int index);
// End of H.M.Wang 2024-7-4 追加一个MCP-H21系列芯片测量压力的读写功能

// H.M.Wang 2024-10-28 增加9555A的读写试验，速录在100k和200k，每次读写500次，读写结果输出log。切换速录需要切换img
    static public native int read9555ATest();
// End of H.M.Wang 2024-10-28 增加9555A的读写试验，速录在100k和200k，每次读写500次，读写结果输出log。切换速录需要切换img

// H.M.Wang 2024-11-5 借用SmartCard的I2C通道实现A133平台的RTC计数器读取（A20的时候是使用/sys/class/device_of_i2c通道实现的）
    static public native byte[] readRTC(byte group, byte addr, byte reg, int len);
    static public native int writeRTC(byte group, byte addr, byte reg, byte[] data, int len);
// End of H.M.Wang 2024-11-5 借用SmartCard的I2C通道实现A133平台的RTC计数器读取（A20的时候是使用/sys/class/device_of_i2c通道实现的）
    static public native int testLevel(int card);

    static public native int readManufactureID(int card);

    static public native int readDeviceID(int card);

    static public native int shutdown();

}
