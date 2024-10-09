package com.industry.printer.hardware;

import android.content.res.AssetManager;
import android.os.Handler;

import com.industry.printer.FileFormat.SystemConfigFile;
import com.industry.printer.PrinterApplication;
import com.industry.printer.Utils.Debug;

import java.io.IOException;
import java.io.InputStream;

public class Hp22mm {
    public static final String TAG = Hp22mm.class.getSimpleName();

    static {
        System.loadLibrary("hp22mm");
    }

    public static void loadLibrary() {
        Debug.d("hp22mm", "Loading Hp22mm library...");
    }

    static public native int init_ids(int idsIndex);
    static public native String ids_get_sys_info();
    static public native int init_pd(int penIndex);
    static public native String pd_get_sys_info();
    static public native int ids_set_platform_info();
    static public native int pd_set_platform_info();
    static public native int ids_set_date();
    static public native int pd_set_date();
    static public native int ids_set_stall_insert_count();
    static public native int ids_get_supply_status();
    static public native String ids_get_supply_status_info();
    static public native int pd_get_print_head_status();
    static public native String pd_get_print_head_status_info();
    static public native int pd_sc_get_status();
    static public native String pd_sc_get_status_info();
    static public native int pd_sc_get_info();
    static public native String pd_sc_get_info_info();
    static public native int DeletePairing();
    static public native int DoPairing();
    static public native int DoOverrides();
    static public native int Pressurize();
    static public native String getPressurizedValue();
    static public native int Depressurize();
    static public native int UpdatePDFW();
    static public native int UpdateFPGAFlash();
    static public native int UpdateIDSFW();
    static public native int pdPowerOn();
    static public native int pdPowerOff();
    static public native String getErrString();
    static public native int getConsumedVol();
    static public native int getUsableVol();
//    static public native String startPrint();
//    static public native String dumpRegisters();
//    static public native String spiTest();
//    static public native int mcu2fifo();
//    static public native int fifo2ddr();
//    static public native int ddr2fifo();
//    static public native int fifo2mcu();

    private static final int IDS_INDEX = 1;
    private static final int PEN_INDEX = 0;

    public static final int DOTS_PER_COL = 1056;
    public static final int BYTES_PER_COL = DOTS_PER_COL / 8;       // 132
    public static final int WORDS_PER_COL = BYTES_PER_COL / 4;      // 8
    public static final int IMAGE_ADDR = 0x0000;

    private static final int REG04_32BIT_WORDS_PER_COL = 4;
    private static final int REG05_BYTES_PER_COL = 5;
    private static final int REG06_COLUMNS = 6;
    private static final int REG07_START_ADD_P0S0_ODD = 7;
    private static final int REG08_START_ADD_P0S0_EVEN = 8;
    private static final int REG09_START_ADD_P0S1_ODD = 9;
    private static final int REG10_START_ADD_P0S1_EVEN = 10;
    private static final int REG11_START_ADD_P1S0_ODD = 11;
    private static final int REG12_START_ADD_P1S0_EVEN = 12;
    private static final int REG13_START_ADD_P1S1_ODD = 13;
    private static final int REG14_START_ADD_P1S1_EVEN = 14;
    private static final int REG15_INTERNAL_ENC_FREQ = 15;
    private static final int REG16_INTERNAL_TOF_FREQ = 16;
    private static final int REG17_ENCODER_SOURCE = 17;
    private static final int REG18_ENCODER_DIVIDER = 18;
    private static final int REG19_TOF_SOURCE = 19;
    private static final int REG20_P1_TOF_OFFSET = 20;
    private static final int REG21_P0_TOF_OFFSET = 21;
    private static final int REG22_PRINT_DIRECTION = 22;
    private static final int REG23_COLUMN_SPACING = 23;
    private static final int REG24_SLOT_SPACING = 24;
    private static final int REG25_PRINT_ENABLE = 25;
    private static final int REG26_PRINT_COUNT = 26;
    private static final int REG27_MAX_PRINT_COUNT = 27;
    private static final int REG28_RESET = 28;
    private static final int REG29_COLUMN_ENABLE = 29;
    private static final int REG30_FLASH_ENABLE = 30;
    private static final int REG31_REVISION = 31;           // Read Only
    private static final int REG32_CLOCK = 32;              // Read Only
    private static final int REG33_READY = 33;

// H.M.Wang 2024-9-26 在Hp22mm类中，新增加一个mInitialize变量，用来记忆初始化状态。在点按开始打印时，判断Hp22mm.mInitialized是否为true，时则启动打印，否则停止打印
    public static boolean mInitialized = false;
// End of H.M.Wang 2024-9-26 在Hp22mm类中，新增加一个mInitialize变量，用来记忆初始化状态。在点按开始打印时，判断Hp22mm.mInitialized是否为true，时则启动打印，否则停止打印

    public static int initHp22mm() {
        mInitialized = false;
        if (0 != init_ids(IDS_INDEX)) {
            Debug.d(TAG, "init_ids failed\n");
            return -1;
        } else {
            Debug.d(TAG, "init_ids succeeded\n");
        }

        if (0 != init_pd(PEN_INDEX)) {
            Debug.d(TAG, "init_pd failed\n");
            return -2;
        } else {
            Debug.d(TAG, "init_pd succeeded\n");
        }

        if (0 != ids_get_supply_status()) {
            Debug.d(TAG, "ids_get_supply_status failed\n");
            return -3;
        } else {
            Debug.d(TAG, "ids_get_supply_status succeeded\n");
        }

        if (0 != pd_get_print_head_status()) {
            Debug.d(TAG, "pd_get_print_head_status failed\n");
            return -4;
        } else {
            Debug.d(TAG, "pd_get_print_head_status succeeded\n");
        }

        if (0 != DeletePairing()) {
            Debug.d(TAG, "DeletePairing failed\n");
            return -5;
        } else {
            Debug.d(TAG, "DeletePairing succeeded\n");
        }

        if (0 != DoPairing()) {
            Debug.d(TAG, "DoPairing failed\n");
            return -6;
        } else {
            Debug.d(TAG, "DoPairing succeeded\n");
        }

        if (0 != DoOverrides()) {
            Debug.d(TAG, "DoOverrides failed\n");
            return -7;
        } else {
            Debug.d(TAG, "DoOverrides succeeded\n");
        }

        if (0 != Pressurize()) {
            Debug.d(TAG, "Pressurize failed\n");
            return -8;
        } else {
            Debug.d(TAG, "Pressurize succeeded\n");
        }
// H.M.Wang 2024-9-26 暂时改为初始化的时候打印头上电
        if (0 != pdPowerOn()) {
            Debug.d(TAG, "PD power on failed\n");
            return -9;
        } else {
            Debug.d(TAG, "PD power on succeeded\n");
        }
// End of H.M.Wang 2024-9-26 暂时改为初始化的时候打印头上电

        mInitialized = true;

        return 0;
    }

    public static char[] getSettings() {
        char[] regs = new char[34];

        SystemConfigFile config = SystemConfigFile.getInstance();

        regs[REG04_32BIT_WORDS_PER_COL] = WORDS_PER_COL;
        regs[REG05_BYTES_PER_COL] = BYTES_PER_COL;
// 下发数据时再设           regs[REG06_COLUMNS] = 0;
        regs[REG07_START_ADD_P0S0_ODD] = 132;
        regs[REG08_START_ADD_P0S0_EVEN] = 132;
        regs[REG09_START_ADD_P0S1_ODD] = 132;
        regs[REG10_START_ADD_P0S1_EVEN] = 132;
        regs[REG11_START_ADD_P1S0_ODD] = 132;
        regs[REG12_START_ADD_P1S0_EVEN] = 132;
        regs[REG13_START_ADD_P1S1_ODD] = 132;
        regs[REG14_START_ADD_P1S1_EVEN] = 132;
        regs[REG14_START_ADD_P1S1_EVEN] = 132;

// H.M.Wang 2024-9-3 修改R15的计算公式
//        int encFreq = (config.mParam[0] != 0 ? 90000000 / (config.mParam[0] * 24) : 150000);                                 // R15=90M/(C1*24)
        int encFreq = (config.mParam[0] != 0 ? 90000000 / (config.mParam[0] * config.mParam[2]) * 25: 150000);  // R15=90M * 25 /(C1*C3)   (2024-9-5)
// End of H.M.Wang 2024-9-3 修改R15的计算公式
        regs[1] = (char)((encFreq >> 16) & 0x0ffff);                                                                         // 借用Reg1来保存ENC的高16位
        regs[REG15_INTERNAL_ENC_FREQ] = (char)((char)(encFreq & 0x0ffff));                                                   // Reg15仅保存ENC的低16位，完整整数在img中合成
        regs[REG17_ENCODER_SOURCE] = (char)config.mParam[5];                                      // C6 = off  R17=0; C6 = on  R17=1
        regs[REG19_TOF_SOURCE] = (char)                                                           // C5 = off  R19=0; C5 = on  R19=1 (!!! C5=0: OFF; C5=1: INTERNAL; C5=2: EXTERNAL)
                (config.mParam[4] > 0 ? config.mParam[4] - 1 : 0);
        int tofFreq = (config.mParam[0] != 0 ? 90000000 / config.mParam[0] * config.mParam[6] / 2 : 45000000);                   // R16=90M * C7 / C1 / 2 (2024-9-5)
        if(regs[REG17_ENCODER_SOURCE] == 1 && regs[REG19_TOF_SOURCE] == 0) {
//            tofFreq = config.mParam[6] * 24;                                                                                 // R16=C7 * 24
            tofFreq = config.mParam[6] * 0;                                                                                 // R16=C7 * 0 (2024-9-5)
        }
        regs[0] = (char)((tofFreq >> 16) & 0x0ffff);                                                                         // 借用Reg0来保存TOF的高16位
        regs[REG16_INTERNAL_TOF_FREQ] = (char)((char)(tofFreq & 0x0ffff));                                                   // Reg16仅保存TOF的低16位，完整整数在img中合成
// H.M.Wang 2024-9-3 修改R18的计算公式
//        regs[REG18_ENCODER_DIVIDER] = (char)                                                      // C3=150  R18=4; C3=300  R18=2; C3=600  R18=1
//                (config.mParam[2] == 150 ? 4 : (config.mParam[2] == 300 ? 2 : (config.mParam[2] == 600 ? 1 : 1)));
        regs[REG18_ENCODER_DIVIDER] = (char)                                                      // R18=((C10*2*25.4)/(C9*3.14))/C3
                (((25.4f * 2 * config.mParam[9]) / (3.14f * config.mParam[8])) / config.mParam[2]);     // (2024-9-5)
// End of H.M.Wang 2024-9-3 修改R18的计算公式
// H.M.Wang 2024-9-3 修改R20,R21的计算公式
//        regs[REG20_P1_TOF_OFFSET] = (char)(config.mParam[3] * 24 + config.mParam[11]);              // R20= C4x24+c12
//        regs[REG21_P0_TOF_OFFSET] = (char)(config.mParam[3] * 24 + config.mParam[10]);              // R21= C4x24+c11
        regs[REG20_P1_TOF_OFFSET] = (char)(config.mParam[3] * (config.mParam[9] * 4 / (config.mParam[8] * 3.14f)) + config.mParam[11] * 150 / config.mParam[2]);              // R20=C4*(C10*4/(C9*3.14))+(C12*150/C3) (2024-9-5)
        regs[REG21_P0_TOF_OFFSET] = (char)(config.mParam[3] * (config.mParam[9] * 4 / (config.mParam[8] * 3.14f)) + config.mParam[10] * 150 / config.mParam[2]);              // R21=C4*(C10*4/(C9*3.14))+(C11*150/C3) (2024-9-5)
// End of H.M.Wang 2024-9-3 修改R20,R21的计算公式
        regs[REG22_PRINT_DIRECTION] = (char)config.mParam[1];                                     // R22= C2???????????  0 = forward, 1 = reverse, 2 = no offsets?????????????????
        regs[REG23_COLUMN_SPACING] = 4;                                                     // 固定数据待定
        regs[REG24_SLOT_SPACING] = 52;                                                      // 固定数据待定
        regs[REG25_PRINT_ENABLE] = 0;                                                       // Enables printing. 1=enable, 0= disable; 1=打印 2=停止???????
        regs[REG26_PRINT_COUNT] = 0;                                                        // R26 打印次数计数 1
        regs[REG27_MAX_PRINT_COUNT] = 0;                                                    // R27 最大打印次数 1
        regs[REG28_RESET] = 0;                                                              // R28 rest 1= Reset; 0= Not Reset
        regs[REG29_COLUMN_ENABLE] = (char)config.getParam(SystemConfigFile.INDEX_22MM_NOZZLE_SEL);  // (sPenIdx == 0) col_mask = 0x0f; (sPenIdx == 1) col_mask = 0xf0
        regs[REG30_FLASH_ENABLE] = 0;                                                       // Connects the SPI interface to the EEPROM so that application software can update the configuration
        regs[REG33_READY] = 0;                                                              // Bit 0 returns the status of the Ready input, which should be driven by the Printhead Driver subsystem. Bit 1 overrides the input so that software can force the outputs into a tristate mode.

        return regs;
    }

    private final static String IMAGE_FILE = "image.bin";
    public static int printTestPage() {
        InputStream is = null;
        int ret = 0;

        try {
            AssetManager assetManager = PrinterApplication.getInstance().getAssets();
            is = assetManager.open(IMAGE_FILE);

            if(null != is) {
                byte[] image = new byte[is.available()];
                is.read(image);
                ret = FpgaGpioOperation.hp22mmPrintTestPage(image);
            }
        } catch(Exception e) {
            Debug.e(TAG, e.getMessage());
            ret = -1;
        } finally {
            try{if(null != is) is.close();}catch(IOException e){}
        }
        return ret;
    }

    public static int[] dumpRegisters(int count) {
        int[] regs = new int[count];
        for(int i=0; i<count; i++) {
            regs[i] = FpgaGpioOperation.hp22mmReadRegister(i+2);
        }
        return regs;
    }

// H.M.Wang 2024-9-26 取消开始打印时在pd_power_on, pd_power_on改在初始化阶段完成
/*    public static int startPrint() {
        if(_startPrint() < 0) {
            if(initHp22mm() < 0) {
                return -1;
            } else {
                return _startPrint();
            }
        }
        Debug.d(TAG, "startPrint succeeded\n");
        return 0;
    }*/
// End of H.M.Wang 2024-9-26 取消开始打印时在pd_power_on, pd_power_on改在初始化阶段完成

// H.M.Wang 2024-9-26 取消停止打印时pd_power_off，也不关闭打印监视线程（在so里面）
/*    public static int stopPrint() {
        return _stopPrint();
    }*/
// End of H.M.Wang 2024-9-26 取消停止打印时pd_power_off，也不关闭打印监视线程（在so里面）

// H.M.Wang 2024-4-19 增加一个写入大块数据的测试项目
    public static int hp22mmBulkWriteTest() {
        return FpgaGpioOperation.hp22mmBulkWriteTest();
    }
// End of H.M.Wang 2024-4-19 增加一个写入大块数据的测试项目
// H.M.Wang 2024-6-20 追加一个22mm通过SPI进行24M速率的写试验
    public static int hp22mmHiSpeedWTest() {
    return FpgaGpioOperation.hp22mmHiSpeedWTest();
}
// End of H.M.Wang 2024-6-20 追加一个22mm通过SPI进行24M速率的写试验
}