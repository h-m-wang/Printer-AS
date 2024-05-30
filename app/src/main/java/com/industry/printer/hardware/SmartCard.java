package com.industry.printer.hardware;

import com.industry.printer.Utils.Debug;

/**
 * Created by hmwan on 2019/10/20.
 */

public class SmartCard {
    static {
		System.loadLibrary("smartcard");
    }

    public final static int CARD_TYPE_PEN1                  = 11;
    public final static int CARD_TYPE_PEN2                  = 12;
    private final static int CARD_TYPE_BULK1                 = 13;               // 真实墨袋
    private final static int CARD_TYPE_BULKX                 = 14;             // 墨盒代替墨袋
    public final static int CARD_TYPE_BULK                 = CARD_TYPE_BULK1;
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
    }

    /**
     * SmartCard JNI APIs
     **/
    static public native int exist(int imgtype);

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

    static public native int readLevel(int card);

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

// H.M.Wang 2022-11-1 Add this API for Bagink Use
    static public native int readLevelDirect();
// End of H.M.Wang 2022-11-1 Add this API for Bagink Use

    static public native int testLevel(int card);

    static public native int readManufactureID(int card);

    static public native int readDeviceID(int card);

    static public native int shutdown();

}
