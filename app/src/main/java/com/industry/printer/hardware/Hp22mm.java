package com.industry.printer.hardware;

import android.content.Context;
import android.os.Handler;

import com.industry.printer.FileFormat.SystemConfigFile;
import com.industry.printer.Utils.Debug;
import com.industry.printer.data.DataTask;

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
    //    static public native int ids_get_supply_id();
//    static public native String ids_get_supply_id_info();
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
    static public native String startPrint();
    static public native int stopPrint();
    static public native String dumpRegisters();
    static public native int[] readRegisters();
    static public native int writeSettings(int[] regs);
    static public native int writeImage(int addr, int cols, int bytes_per_col, byte[] image);
    static public native int start(int[] regs);
    static public native String spiTest();
    static public native int mcu2fifo();
    static public native int fifo2ddr();
    static public native int ddr2fifo();
    static public native int fifo2mcu();

    private static final int IDS_INDEX = 1;
    private static final int PEN_INDEX = 0;

    public static final int IMAGE_ROWS = 1056;
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

    public static Handler gCtrlHandler = null;

    // Refer to "HP 22mm PDG FPGA Electrical Reference Specification.pdf"
    private static void updateSettings() {
        int[] regs = readRegisters();

        for(int i=0; i<regs.length; i++) {
            Debug.d(TAG, "Read Reg[" + i + "] = " + regs[i] + "(0x" + Integer.toHexString(regs[i]) + ")");
        }

        if(null != regs) {
            SystemConfigFile config = SystemConfigFile.getInstance();

            regs[REG04_32BIT_WORDS_PER_COL] = IMAGE_ROWS / 32;
            regs[REG05_BYTES_PER_COL] = IMAGE_ROWS / 8;
// 下发数据时再设           regs[REG06_COLUMNS] = 0;
            regs[REG07_START_ADD_P0S0_ODD] = 0;
            regs[REG08_START_ADD_P0S0_EVEN] = 0;
            regs[REG09_START_ADD_P0S1_ODD] = 0;
            regs[REG10_START_ADD_P0S1_EVEN] = 0;
            regs[REG11_START_ADD_P1S0_ODD] = 0;
            regs[REG12_START_ADD_P1S0_EVEN] = 0;
            regs[REG13_START_ADD_P1S1_ODD] = 0;
            regs[REG14_START_ADD_P1S1_EVEN] = 0;
            regs[REG14_START_ADD_P1S1_EVEN] = 0;

/*            regs[REG15_INTERNAL_ENC_FREQ] = 180000;                              // R15=90M/(C1*24)
            regs[REG16_INTERNAL_TOF_FREQ] = 180000000;     // R16=C7/C1*90M
            regs[REG17_ENCODER_SOURCE] = 0;                                      // C6 = off  R17=0; C6 = on  R17=1
            regs[REG18_ENCODER_DIVIDER] = 1;                                                      // C3=150  R18=4; C3=300  R18=2; C3=600  R18=1
            regs[REG19_TOF_SOURCE] = 0;                                                           // C5 = off  R19=0; C5 = on  R19=1 (!!! C5=0: OFF; C5=1: INTERNAL; C5=2: EXTERNAL)
            regs[REG20_P1_TOF_OFFSET] = 0;              // R20= C2x24+c12
            regs[REG21_P0_TOF_OFFSET] = 0;              // R21= C2x24+c11
            regs[REG22_PRINT_DIRECTION] = 0;                                     // R22= C2???????????  0 = forward, 1 = reverse, 2 = no offsets?????????????????
            regs[REG23_COLUMN_SPACING] = 4;                                                     // 固定数据待定
            regs[REG24_SLOT_SPACING] = 52;                                                      // 固定数据待定
            regs[REG25_PRINT_ENABLE] = 0;                                                       // Enables printing. 1=enable, 0= disable; 1=打印 2=停止???????
            regs[REG28_RESET] = 0;                                                              // R28 rest 1= Reset; 0= Not Reset
            regs[REG29_COLUMN_ENABLE] = 0x0f;                                                   // (sPenIdx == 0) col_mask = 0x0f; (sPenIdx == 1) col_mask = 0xf0

            if (PDGWrite(15, encoder) < 0 ||    // R15 internal encoder period (divider of clock freq)
                    PDGWrite(16, tof_freq) < 0 ||   // R16 internal TOF frequency (Hz)
                    PDGWrite(17, 0) < 0 ||          // R17 0 = internal encoder
                    PDGWrite(18, 1) < 0 ||          // R18 external encoder divider (2=600 DPI)
                    PDGWrite(19, 0) < 0 ||          // R19 0 = internal TOF
                    PDGWrite(20, 0) < 0 ||  // R20 pen 0 encoder counts from TOF to start print
                    PDGWrite(21, 0) < 0 ||  // R21 pen 1 encoder counts from TOF to start print
                    PDGWrite(22, 0) < 0 ||          // R22 0 - print direction forward
                    PDGWrite(23, 4) < 0 ||          // R23 column-to-column spacing (rows)
                    PDGWrite(24, 52) < 0 ||         // R24 slot-to-slot spacing (rows)
                    PDGWrite(25, 0) < 0 ||          // R25 0 - print disabled
                    PDGWrite(28, 0) < 0 ||          // R28 0 - not reset
                    PDGWrite(29, col_mask) < 0)     // R29 column enable bits
*/
            regs[REG15_INTERNAL_ENC_FREQ] = 90000000 / (config.mParam[0] * 24);                 // R15=90M/(C1*24)
            regs[REG16_INTERNAL_TOF_FREQ] = 90000000 / (config.mParam[6] / config.mParam[0]);   // R16=90M/(C7/C1)
            regs[REG17_ENCODER_SOURCE] = config.mParam[5];                                      // C6 = off  R17=0; C6 = on  R17=1
            regs[REG18_ENCODER_DIVIDER] =                                                       // C3=150  R18=4; C3=300  R18=2; C3=600  R18=1
                (config.mParam[2] == 0 ? 4 : (config.mParam[2] == 1 ? 2 : (config.mParam[2] == 3 ? 1 : 1)));
            regs[REG19_TOF_SOURCE] =                                                            // C5 = off  R19=0; C5 = on  R19=1 (!!! C5=0: OFF; C5=1: INTERNAL; C5=2: EXTERNAL)
                (config.mParam[4] > 0 ? config.mParam[4] - 1 : 0);
            regs[REG20_P1_TOF_OFFSET] = config.mParam[1] * 24 + config.mParam[11];              // R20= C2x24+c12
            regs[REG21_P0_TOF_OFFSET] = config.mParam[1] * 24 + config.mParam[10];              // R21= C2x24+c11
            regs[REG22_PRINT_DIRECTION] = config.mParam[1];                                     // R22= C2???????????  0 = forward, 1 = reverse, 2 = no offsets?????????????????
            regs[REG23_COLUMN_SPACING] = 4;                                                     // 固定数据待定
            regs[REG24_SLOT_SPACING] = 52;                                                      // 固定数据待定
            regs[REG25_PRINT_ENABLE] = 0;                                                       // Enables printing. 1=enable, 0= disable; 1=打印 2=停止???????
            regs[REG26_PRINT_COUNT] = 0;                                                        // R26 打印次数计数 1
            regs[REG27_MAX_PRINT_COUNT] = 0;                                                    // R27 最大打印次数 1
            regs[REG28_RESET] = 0;                                                              // R28 rest 1= Reset; 0= Not Reset
            regs[REG29_COLUMN_ENABLE] = 0x0f;                                                   // (sPenIdx == 0) col_mask = 0x0f; (sPenIdx == 1) col_mask = 0xf0
            regs[REG30_FLASH_ENABLE] = 0;                                                       // Connects the SPI interface to the EEPROM so that application software can update the configuration
            regs[REG33_READY] = 0;                                                              // Bit 0 returns the status of the Ready input, which should be driven by the Printhead Driver subsystem. Bit 1 overrides the input so that software can force the outputs into a tristate mode.

            for(int i=0; i<regs.length; i++) {
                Debug.d(TAG, "Write Reg[" + i + "] = " + regs[i] + "(0x" + Integer.toHexString(regs[i]) + ")");
            }

            if(writeSettings(regs) == 0) {
                Debug.d(TAG, "Write settings success.");
            } else {
                Debug.e(TAG, "Write settings failed.");
                gCtrlHandler.obtainMessage(888888, "Write settings failed.");
            }
        }
    }

    public static synchronized int writeData(char data[], int len) {
        byte[] image = new byte[data.length*2];
        for(int i=0; i<data.length; i++) {
            image[i*2] = (byte)(data[i] & 0x0ff);
            image[i*2+1] = (byte)((data[i] >> 8) & 0x0ff);
        }

        int ret = writeImage(IMAGE_ADDR, image.length / (IMAGE_ROWS / 8), IMAGE_ROWS / 8, image);
        if(ret > 0) {
            updateSettings();
            triggerPrint();
        } else {
            gCtrlHandler.obtainMessage(888888, "Write print data failed.");
        }
        return ret;
    }

    public static void initPrint() {
        if (0 != Hp22mm.init_ids(IDS_INDEX)) {
            Debug.d(TAG, "init_ids failed\n");
            gCtrlHandler.obtainMessage(888888, "init_ids failed.");
        } else {
            Debug.d(TAG, "init_ids succeeded\n");
        }

        if (0 != Hp22mm.init_pd(PEN_INDEX)) {
            Debug.d(TAG, "init_pd failed\n");
            gCtrlHandler.obtainMessage(888888, "init_pd failed.");
        } else {
            Debug.d(TAG, "init_pd succeeded\n");
        }

        if (0 != Hp22mm.DeletePairing()) {
            Debug.d(TAG, "DeletePairing failed\n");
            gCtrlHandler.obtainMessage(888888, "DeletePairing failed.");
        } else {
            Debug.d(TAG, "DeletePairing succeeded\n");
        }

        if (0 != Hp22mm.DoPairing()) {
            Debug.d(TAG, "DoPairing failed\n");
            gCtrlHandler.obtainMessage(888888, "DoPairing failed.");
        } else {
            Debug.d(TAG, "DoPairing succeeded\n");
        }

        if (0 != Hp22mm.DoOverrides()) {
            Debug.d(TAG, "DoOverrides failed\n");
            gCtrlHandler.obtainMessage(888888, "DoOverrides failed.");
        } else {
            Debug.d(TAG, "DoOverrides succeeded\n");
        }

        if (0 != Hp22mm.Pressurize()) {
            Debug.d(TAG, "Pressurize failed\n");
            gCtrlHandler.obtainMessage(888888, "Pressurize failed.");
        } else {
            Debug.d(TAG, "Pressurize succeeded\n");
        }
    }

    private static void triggerPrint() {
        int[] regs = readRegisters();

        for(int i=0; i<regs.length; i++) {
            Debug.d(TAG, "Read Reg[" + i + "] = " + regs[i] + "(0x" + Integer.toHexString(regs[i]) + ")");
        }

        if(null != regs) {
            SystemConfigFile config = SystemConfigFile.getInstance();

            regs[REG17_ENCODER_SOURCE] = 0;                                      // C6 = off  R17=0; C6 = on  R17=1
            regs[REG19_TOF_SOURCE] = 0;                                                           // C5 = off  R19=0; C5 = on  R19=1 (!!! C5=0: OFF; C5=1: INTERNAL; C5=2: EXTERNAL)
//            regs[REG17_ENCODER_SOURCE] = config.mParam[5];                                      // C6 = off  R17=0; C6 = on  R17=1
//            regs[REG19_TOF_SOURCE] =                                                            // C5 = off  R19=0; C5 = on  R19=1 (!!! C5=0: OFF; C5=1: INTERNAL; C5=2: EXTERNAL)
//                    (config.mParam[4] > 0 ? config.mParam[4] - 1 : 0);
            regs[REG26_PRINT_COUNT] = 0;                                                        // R26 打印次数计数 1
            regs[REG27_MAX_PRINT_COUNT] = 10000;                                                    // R27 最大打印次数 1
            regs[REG25_PRINT_ENABLE] = 1;                                                       // Enables printing. 1=enable, 0= disable; 1=打印 2=停止???????
// 内部强制设为1            regs[REG25_PRINT_ENABLE] = 1;                                                       // Enables printing. 1=enable, 0= disable; 1=打印 2=停止???????

            if(start(regs) == 0) {
                Debug.d(TAG, "Launch print success.");
            } else {
                Debug.e(TAG, "Launch print failed.");
                gCtrlHandler.obtainMessage(888888, "Pressurize failed.");
            }
        }
    }
}