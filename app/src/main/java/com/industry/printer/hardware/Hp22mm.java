package com.industry.printer.hardware;

import android.content.res.AssetManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Printer;

import com.industry.printer.FileFormat.SystemConfigFile;
import com.industry.printer.PHeader.PrinterNozzle;
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
        Debug.d(TAG, "Loading Hp22mm library...");
    }

    static public native int init_ids(int idsIndex);
    static public native String ids_get_sys_info();
    static public native int init_pd(int penArg);
    static public native String pd_get_sys_info();
    static public native int ids_set_platform_info();
    static public native int pd_set_platform_info();
    static public native int ids_set_date();
    static public native int pd_set_date();
    static public native int ids_set_stall_insert_count();
    static public native int ids_get_supply_status();
    static public native String ids_get_supply_status_info();
    static public native int pd_get_print_head_status(int penIndex);
    static public native String pd_get_print_head_status_info();
    static public native int pd_sc_get_status(int penIndex);
    static public native String pd_sc_get_status_info();
    static public native int pd_sc_get_info(int penIndex);
    static public native String pd_sc_get_info_info();
    static public native int DeletePairing();
    static public native int DoPairing();
    static public native int DoOverrides();
// H.M.Wang 2024-11-10 修改so中的控制逻辑，函数参数变化
//    static public native int Pressurize();
    static public native int StartMonitor();
    static public native int Pressurize(boolean async);
// End of H.M.Wang 2024-11-10 修改so中的控制逻辑，函数参数变化
// H.M.Wang 2025-2-10 追加要给控制是否加热的功能
    static public native int EnableWarming(int enable);
// End of H.M.Wang 2025-2-10 追加要给控制是否加热的功能
    static public native String getPressurizedValue();
    static public native int Depressurize();
    static public native int UpdatePDFW();
    static public native int UpdateFPGAFlash();
    static public native int UpdateIDSFW();
    static public native int pdPowerOn(int penIndex, int temp);
    static public native int pdPowerOff(int penIndex);
// H.M.Wang 2024-11-13 追加22mm打印头purge功能
    static public native int pdPurge(int penIndex);
// End of H.M.Wang 2024-11-13 追加22mm打印头purge功能
    static public native String getErrString();
    static public native int getRunningState(int index);
    static public native int getConsumedVol();
    static public native int getUsableVol();
// H.M.Wang 2024-12-10 22mm本来应该使用内部的统计系统统计墨水的消耗情况，但是暂时看似乎没有动作，因此启用独自的统计系统，计数值保存在OEM_RW区域
    static public native int getLocalInk(int head);
    static public native int downLocal(int head, int count);
// End of H.M.Wang 2024-12-10 22mm本来应该使用内部的统计系统统计墨水的消耗情况，但是暂时看似乎没有动作，因此启用独自的统计系统，计数值保存在OEM_RW区域
// H.M.Wang 2025-6-9 修改为log可设置为输出和不输出
    static public native int enableLog(int output);
// End of H.M.Wang 2025-6-9 修改为log可设置为输出和不输出
//    static public native String startPrint();
//    static public native String dumpRegisters();
//    static public native String spiTest();
//    static public native int mcu2fifo();
//    static public native int fifo2ddr();
//    static public native int ddr2fifo();
//    static public native int fifo2mcu();

//    private static final int IDS_INDEX = 0;
//    private static final int PEN_INDEX = 1;

    public static final int DOTS_PER_COL = 1056;
    public static final int BYTES_PER_COL = DOTS_PER_COL / 8;       // 132
    public static final int WORDS_PER_COL = BYTES_PER_COL / 4;      // 33
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

    private static boolean mIDSInitialized = false;

    public static int initHp22mm(int nozzle_sel) {
// H.M.Wang 2024-12-25 增加IDS和PEN的选择功能，不再使用代码中固定指定的IDS和PEN。暂时只支持IDS和PEN各选1个
//        int nozzle_sel = SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_22MM_NOZZLE_SEL);
        int idsIndex = 1;
        if(((nozzle_sel >> 10) & 0x03) == 0x01) idsIndex = 0;   // IDS0=1时无论IDS1为何值，IDSINDEX=0，其余IDSINDEX=1
// End of H.M.Wang 2024-12-25 增加IDS和PEN的选择功能，不再使用代码中固定指定的IDS和PEN。暂时只支持IDS和PEN各选1个

        if(!mIDSInitialized) {
            if (0 != init_ids(idsIndex)) {
                Debug.d(TAG, "init_ids failed\n");
                return -1;
            } else {
                Debug.d(TAG, "init_ids succeeded\n");
            }

            if (0 != ids_get_supply_status()) {
                Debug.d(TAG, "ids_get_supply_status failed\n");
                return -3;
            } else {
                Debug.d(TAG, "ids_get_supply_status succeeded\n");
            }

// H.M.Wang 2024-11-10
            if (0 != StartMonitor()) {
                Debug.d(TAG, "StartMonitor failed\n");
                return -7;
            } else {
                Debug.d(TAG, "StartMonitor succeeded\n");
            }
// End of H.M.Wang 2024-11-10

// H.M.Wang 2024-11-10
//        if (0 != Pressurize()) {
            if (0 != Pressurize(true)) {
// End of H.M.Wang 2024-11-10
                Debug.d(TAG, "Pressurize failed\n");
                return -8;
            } else {
                Debug.d(TAG, "Pressurize succeeded\n");
            }
            mIDSInitialized = true;
        }

        int penArg = ((nozzle_sel >> 8) & 0x00000003);

// H.M.Wang 2025-1-20 当C31指定为hp22mmx2打印头类型时，无论C77如何制定，均按双头处理。如果C31指定为hp22mm，但是C77指定双头时，返回-255错误
        if(SystemConfigFile.getInstance().getPNozzle() == PrinterNozzle.MESSAGE_TYPE_22MMX2) {
            if(penArg != 3) {
                return -253;    // 22MMx2的头，但是实际头没有两个，报错，头太少
            }
// H.M.Wang 2025-2-27 增加一带二的判断。当一带二时，C31=HP22MM，但数据区被纵向方法一倍，使得每列的字节数翻倍；C77=两个头
        } else if(SystemConfigFile.getInstance().getPNozzle() == PrinterNozzle.MESSAGE_TYPE_22MM) {
            if(penArg == 3) {
                if (SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_ONE_MULTIPLE) != 12) {
                    return -255;    // 22MM的头，实际头有两个，当没有设置一带二，报错，头太多
                }
            } else {
                if (SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_ONE_MULTIPLE) == 12) {
                    return -253;    // 22MM的头，实际头没有两个，却设置了一带二，报错，头太少
                }
                if(penArg != 1 && penArg != 2) {
                    return -253;    // 22MM的头，实际头既不是1头也不是2头，当前就是没有设置头，报错，头太少
                }
            }
// End of H.M.Wang 2025-2-27 增加一带二的判断。当一带二时，C31=HP22MM，但数据区被纵向方法一倍，使得每列的字节数翻倍；C77=两个头
        }
// End of H.M.Wang 2025-1-20 当C31指定为hp22mmx2打印头类型时，无论C77如何制定，均按双头处理。如果C31指定为hp22mm，但是C77指定双头时，返回-255错误

        int[] penIdxs;
        if(penArg == 0x01) {
            penIdxs = new int[] {0};
        } else if(penArg == 0x02) {
            penIdxs = new int[]{1};
        } else {
            penIdxs = new int[]{0,1};
        }

        if (0 != init_pd(penArg)) {
            Debug.d(TAG, "init_pd failed\n");
            return -2;
        } else {
            Debug.d(TAG, "init_pd succeeded\n");
        }

        if (0 != DeletePairing()) {
            Debug.d(TAG, "DeletePairing failed\n");
            return -5;
        } else {
            Debug.d(TAG, "DeletePairing succeeded\n");
        }

        if (0 != DoPairing()) {
            Debug.d(TAG, "DoPairing failed\n");
// H.M.Wang 2025-1-20 虽然C31指定为hp22mm，C77指定了单头，但是两者不匹配，会发生DoPairing错误，此时返回-254错误
            return -254;
// End of H.M.Wang 2025-1-20 虽然C31指定为hp22mm，C77指定了单头，但是两者不匹配，会发生DoPairing错误
        } else {
            Debug.d(TAG, "DoPairing succeeded\n");
        }

        if (0 != DoOverrides()) {
            Debug.d(TAG, "DoOverrides failed\n");
            return -7;
        } else {
            Debug.d(TAG, "DoOverrides succeeded\n");
        }

        for(int i=0; i<penIdxs.length; i++) {
// H.M.Wang 2024-9-26 暂时改为初始化的时候打印头上电
            if (0 != pdPowerOn(penIdxs[i], SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_WARMING))) {
                Debug.d(TAG, "PD power on failed\n");
                return -9;
            } else {
                Debug.d(TAG, "PD power on succeeded\n");
            }
// End of H.M.Wang 2024-9-26 暂时改为初始化的时候打印头上电
        }

// H.M.Wang 2025-2-17 上电后停止加热，只有开始打印后再加热
        EnableWarming(0);
// End of H.M.Wang 2025-2-17 上电后停止加热，只有开始打印后再加热

        return 0;
    }

    public static boolean CleanHeads[] = new boolean[] {true, true};

// H.M.Wang 2025-3-5 固定清洗时的参数（寄存器）值
    private static char[] getPurgeSettings() {
        char[] regs = new char[34];

        regs[0] = 0x3a;
        regs[1] = 0x0;
        regs[2] = 0x0;
        regs[3] = 0x0;
        regs[4] = 0x21;
//            regs[5] = 0x108;
            regs[5] = 0x84;
        regs[6] = 0x0;
        regs[7] = 0x0;
        regs[8] = 0x0;
        regs[9] = 0x0;
        regs[10] = 0x0;
        regs[11] = 0x1;
        regs[12] = 0x0;
        regs[13] = 0x0;
        regs[14] = 0x0;
        regs[15] = 0x1388;
        regs[16] = 0x5d68;
        regs[17] = 0x0;
        regs[18] = 0x4;
        regs[19] = 0x0;
        regs[20] = 0x0;
        regs[21] = 0x0;
        regs[22] = 0x0;
        regs[23] = 0x0;
        regs[24] = 0x0;
        regs[25] = 0x0;
        regs[26] = 0x0;
        regs[27] = 0x0;
        regs[28] = 0x0;
            regs[29] = (char)(0x00 | (CleanHeads[0] ? 0x0f : 0x00) | (CleanHeads[1] ? 0xf0 : 0x00));
        regs[30] = 0x0;
        regs[31] = 0x0;
        regs[32] = 0x0;
        regs[33] = 0x0;

        return regs;
    }
// End of  H.M.Wang 2025-3-5 固定清洗时的参数（寄存器）值

    public static char[] getSettings(int type) {
        char[] regs = new char[34];
        SystemConfigFile config = SystemConfigFile.getInstance();

// H.M.Wang 2025-3-5 固定清洗时的参数（寄存器）值
        if(type == FpgaGpioOperation.SETTING_TYPE_PURGE1) return getPurgeSettings();
// End of  H.M.Wang 2025-3-5 固定清洗时的参数（寄存器）值

// H.M.Wang 2025-1-19 根据参数中选择的打印头类型决定R5和R11的值
        PrinterNozzle nozzle = PrinterNozzle.getInstance(config.getParam(SystemConfigFile.INDEX_HEAD_TYPE));
// End of H.M.Wang 2025-1-19 根据参数中选择的打印头类型决定R5和R11的值

// H.M.Wang 2025-2-10 取消这个修改，改为在R15和R16处直接根据是否为清洗分别计算设置
// H.M.Wang 2025-1-21 增加区分正常打印和清洗的不同的下发内容
//        int param0 = (type == FpgaGpioOperation.SETTING_TYPE_PURGE1 ? 1000 : config.mParam[0]);
//        int param6 = (type == FpgaGpioOperation.SETTING_TYPE_PURGE1 ? 200 : config.mParam[6]);
// End of H.M.Wang 2025-1-21 增加区分正常打印和清洗的不同的下发内容
// End of H.M.Wang 2025-2-10 取消这个修改，改为在R15和R16处直接根据是否为清洗分别计算设置

        regs[REG04_32BIT_WORDS_PER_COL] = WORDS_PER_COL;
        regs[REG05_BYTES_PER_COL] = BYTES_PER_COL;
// H.M.Wang 2025-2-27 增加一带二的判断。当一带二时，C31=HP22MM，但数据区被纵向方法一倍，使得每列的字节数翻倍；C77=两个头
        if((nozzle == PrinterNozzle.MESSAGE_TYPE_22MMX2) ||
           (nozzle == PrinterNozzle.MESSAGE_TYPE_22MM &&  config.getParam(SystemConfigFile.INDEX_ONE_MULTIPLE) == 12)) {
// End of H.M.Wang 2025-2-27 增加一带二的判断。当一带二时，C31=HP22MM，但数据区被纵向方法一倍，使得每列的字节数翻倍；C77=两个头
            regs[REG05_BYTES_PER_COL] *= 1;
        }
// End of H.M.Wang 2025-1-19 根据参数中选择的打印头类型决定R5和R11的值
// 下发数据时再设           regs[REG06_COLUMNS] = 0;
// H.M.Wang 2025-1-19 根据参数中选择的打印头类型决定R5和R11的值, R7-R14的的其它管脚失效
//        regs[REG07_START_ADD_P0S0_ODD] = 132;
// H.M.Wang 2024-12-29 临时修改为0x7FE084
//        regs[REG08_START_ADD_P0S0_EVEN] = 132;
//        regs[2] = (char)0x07F;
//        regs[REG08_START_ADD_P0S0_EVEN] = (char)0xE084;
// End of H.M.Wang 2024-12-29 临时修改为0x7FE084
//        regs[REG09_START_ADD_P0S1_ODD] = 132;
//        regs[REG10_START_ADD_P0S1_EVEN] = 132;
//        regs[REG11_START_ADD_P1S0_ODD] = 132;
        regs[REG11_START_ADD_P1S0_ODD] = 0; // 只有一个喷头为0
        if(nozzle == PrinterNozzle.MESSAGE_TYPE_22MMX2) {
            regs[REG11_START_ADD_P1S0_ODD] = 1; // 两个喷头时为1
        }
//        regs[REG12_START_ADD_P1S0_EVEN] = 132;
//        regs[REG13_START_ADD_P1S1_ODD] = 132;
//        regs[REG14_START_ADD_P1S1_EVEN] = 132;
// End of H.M.Wang 2025-1-19 根据参数中选择的打印头类型决定R5和R11的值, R7-R14的的其它管脚失效

// H.M.Wang 2024-9-3 修改R15的计算公式
//        int encFreq = (param0 != 0 ? 90000000 / (param0 * 24) : 150000);                                 // R15=90M/(C1*24)
// H.M.Wang 2025-1-10 param0最小不能小于110来计算
//        int encFreq = (param0 != 0 ? 90000000 / (param0 * config.mParam[2]) * 25: 150000);  // R15=90M * 25 /(C1*C3)   (2024-9-5)
// H.M.Wang 2025-2-10 R15和R16处直接根据是否为清洗分别计算设置
        int encFreq;
        if(type == FpgaGpioOperation.SETTING_TYPE_PURGE1) {
            encFreq = 5000;
        } else {
            encFreq = (config.mParam[0] != 0 ? 90000000 / (Math.max(config.mParam[0], 110) * config.mParam[2]) * 25: 150000);  // R15=90M * 25 /(C1*C3)   (2024-9-5)
        }
// End of H.M.Wang 2025-2-10 R15和R16处直接根据是否为清洗分别计算设置
// End of H.M.Wang 2025-1-10 param0最小不能小于110来计算
// End of H.M.Wang 2024-9-3 修改R15的计算公式
        regs[1] = (char)((encFreq >> 16) & 0x0ffff);                                                                         // 借用Reg1来保存ENC的高16位
        regs[REG15_INTERNAL_ENC_FREQ] = (char)((char)(encFreq & 0x0ffff));                                                   // Reg15仅保存ENC的低16位，完整整数在img中合成
        regs[REG17_ENCODER_SOURCE] = (char)config.mParam[5];                                      // C6 = off  R17=0; C6 = on  R17=1
        regs[REG19_TOF_SOURCE] = (char)                                                           // C5 = off  R19=0; C5 = on  R19=1 (!!! C5=0: OFF; C5=1: INTERNAL; C5=2: EXTERNAL)
                (config.mParam[4] > 0 ? config.mParam[4] - 1 : 0);
        int tofFreq = (config.mParam[0] != 0 ? 90000000 / config.mParam[0] * config.mParam[6] / 2 : 45000000);                   // R16=90M * C7 / C1 / 2 (2024-9-5)
        if(regs[REG17_ENCODER_SOURCE] == 1 && regs[REG19_TOF_SOURCE] == 0) {
//            tofFreq = param6 * 24;                                                                                 // R16=C7 * 24
            tofFreq = config.mParam[6] * 0;                                                                                 // R16=C7 * 0 (2024-9-5)
        }
// H.M.Wang 2025-2-10 R15和R16处直接根据是否为清洗分别计算设置
        if(type == FpgaGpioOperation.SETTING_TYPE_PURGE1) {
// H.M.Wang 2025-2-17 修改当清洗时R17和R19都设0
            regs[REG17_ENCODER_SOURCE] = 0;
            regs[REG19_TOF_SOURCE] = 0;
// End of H.M.Wang 2025-2-17 修改当清洗时R17和R19都设0
            tofFreq = 3825000;
        }
// End of H.M.Wang 2025-2-10 R15和R16处直接根据是否为清洗分别计算设置
        regs[0] = (char)((tofFreq >> 16) & 0x0ffff);                                                                         // 借用Reg0来保存TOF的高16位
        regs[REG16_INTERNAL_TOF_FREQ] = (char)((char)(tofFreq & 0x0ffff));                                                   // Reg16仅保存TOF的低16位，完整整数在img中合成
// H.M.Wang 2024-9-3 修改R18的计算公式
//        regs[REG18_ENCODER_DIVIDER] = (char)                                                      // C3=150  R18=4; C3=300  R18=2; C3=600  R18=1
//                (config.mParam[2] == 150 ? 4 : (config.mParam[2] == 300 ? 2 : (config.mParam[2] == 600 ? 1 : 1)));
        int C3A = Math.max(config.mParam[2]/300, 1) * 300;
        regs[REG18_ENCODER_DIVIDER] = (char)                                                      // R18=((C10*2*25.4)/(C9*3.14))/C3
                Math.max((((25.4f * 2 * config.mParam[9]) / (3.14f * config.mParam[8])) / C3A), 1);     // (2024-9-5)  (2025-1-9 最小值不小于1)(2025-4-10 config.mParam[2]取300整数倍)
// End of H.M.Wang 2024-9-3 修改R18的计算公式
// H.M.Wang 2024-9-3 修改R20,R21的计算公式
//        regs[REG20_P1_TOF_OFFSET] = (char)(config.mParam[3] * 24 + config.mParam[11]);              // R20= C4x24+c12
//        regs[REG21_P0_TOF_OFFSET] = (char)(config.mParam[3] * 24 + config.mParam[10]);              // R21= C4x24+c11
// H.M.Wang 2025-2-17 修改R20，R21计算公式为：R20= C4x(C10x2/(C9x3.14*4))/+(c11*24/6)； R21= C4x(C10x2/(C9x3.14*4))/+(c12*24/6)
//        regs[REG20_P1_TOF_OFFSET] = (char)(config.mParam[3] * (config.mParam[9] * 4 / (config.mParam[8] * 3.14f)) + config.mParam[11] * 150 / config.mParam[2]);              // R20=C4*(C10*4/(C9*3.14))+(C12*150/C3) (2024-9-5)
//        regs[REG21_P0_TOF_OFFSET] = (char)(config.mParam[3] * (config.mParam[9] * 4 / (config.mParam[8] * 3.14f)) + config.mParam[10] * 150 / config.mParam[2]);              // R21=C4*(C10*4/(C9*3.14))+(C11*150/C3) (2024-9-5)
        regs[REG20_P1_TOF_OFFSET] = (char)(config.mParam[3] * (config.mParam[9] * 2 / (config.mParam[8] * 3.14f * 4)) + config.mParam[10] * 12 / 6);
        regs[REG21_P0_TOF_OFFSET] = (char)(config.mParam[3] * (config.mParam[9] * 2 / (config.mParam[8] * 3.14f * 4)) + config.mParam[11] * 12 / 6);
// H.M.Wang 2025-2-21 清洗时固定参数R20=0；R21=0
        if(type == FpgaGpioOperation.SETTING_TYPE_PURGE1) {
            regs[REG20_P1_TOF_OFFSET] = 0;
            regs[REG21_P0_TOF_OFFSET] = 0;
        }
// End of H.M.Wang 2025-2-21 清洗时固定参数R20=0；R21=0
// End of H.M.Wang 2025-2-17 修改R20，R21计算公式为：R20= C4x(C10x2/(C9x3.14*4))/+(c11*24/6)； R21= C4x(C10x2/(C9x3.14*4))/+(c12*24/6)
// End of H.M.Wang 2024-9-3 修改R20,R21的计算公式

        regs[REG22_PRINT_DIRECTION] = (char)config.mParam[1];                                     // R22= C2???????????  0 = forward, 1 = reverse, 2 = no offsets?????????????????
        regs[REG23_COLUMN_SPACING] = (char)config.getParam(SystemConfigFile.INDEX_COLUMN_SPACING);                                                     // 固定数据待定=4
        regs[REG24_SLOT_SPACING] = (char)config.getParam(SystemConfigFile.INDEX_SLOT_SPACING);                                                      // 固定数据待定=52
// H.M.Wang 2025-5-19 修改Reg25的值，Circulation/循环间隔设置为Reg25[15:2]
//        regs[REG25_PRINT_ENABLE] = 0;                                                  // Enables printing. 1=enable, 0= disable; 1=打印 2=停止???????
// H.M.Wang 2025-5-24 扩充REG25到32bit
        regs[2] = (char)(0x00);        // Bit[18] = 1（Recycle) or 0 (Normal)
        regs[2] |= (char)(0x03 & (config.getParam(SystemConfigFile.INDEX_CIRCULATION) >> 14));        // Bit[17:2] = Recycle Index(P81[15:0])
        regs[REG25_PRINT_ENABLE] = (char)(0xFFFC & (config.getParam(SystemConfigFile.INDEX_CIRCULATION) << 2));  // Bit[1:0] = 00(停止打印； 11（开始打印）
// End of H.M.Wang 2025-5-24 扩充REG25到32bit
// End of H.M.Wang 2025-5-19 修改Reg25的值，Circulation/循环间隔设置为Reg25[15:2]
        regs[REG26_PRINT_COUNT] = 0;                                                        // R26 打印次数计数 1
// H.M.Wang 2024-12-27 该寄存器的意义改变，为DPI。当参数3的分辨率为300/450时，为0，600/750时，为1，以此类推
//        regs[REG27_MAX_PRINT_COUNT] = 0;                                                    // R27 最大打印次数 1
        regs[REG27_MAX_PRINT_COUNT] = (char)(config.mParam[2] / 300);                                                    // R27 最大打印次数 1
        regs[REG27_MAX_PRINT_COUNT] = (char)(regs[REG27_MAX_PRINT_COUNT] < 1 ? 0 : (regs[REG27_MAX_PRINT_COUNT]-1));
// H.M.Wang 2025-2-17 R27[3] = 1 双头倒置
        if(config.getParam(14) > 0)
            regs[REG27_MAX_PRINT_COUNT] |= 0x08;
        else
            regs[REG27_MAX_PRINT_COUNT] &= ~0x08;
// End of H.M.Wang 2025-2-17 R27[3] = 1 双头倒置
// End of H.M.Wang 2024-12-27 该寄存器的意义改变，为DPI。当参数3的分辨率为300/450时，为0，600/750时，为1，以此类推
        regs[REG28_RESET] = 0;                                                              // R28 rest 1= Reset; 0= Not Reset
// H.M.Wang 2025-1-21 增加区分正常打印和清洗的不同的下发内容
        if(type == FpgaGpioOperation.SETTING_TYPE_PURGE1)
            regs[REG29_COLUMN_ENABLE] = (char)0x0FF;  // (sPenIdx == 0) col_mask = 0x0f; (sPenIdx == 1) col_mask = 0xf0
        else
            regs[REG29_COLUMN_ENABLE] = (char)(0x0FF & config.getParam(SystemConfigFile.INDEX_22MM_NOZZLE_SEL));  // (sPenIdx == 0) col_mask = 0x0f; (sPenIdx == 1) col_mask = 0xf0
// End of H.M.Wang 2025-1-21 增加区分正常打印和清洗的不同的下发内容
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
// H.M.Wang 2025-3-19 追加一个循环功能
    public static void hp22mmCirculation() {
        char[] settings = getSettings(FpgaGpioOperation.SETTING_TYPE_PURGE1);
        int tofFreq = 1000000000;                   // 循环时按1000M
        settings[0] = (char)((tofFreq >> 16) & 0x0ffff);                                                                         // 借用Reg0来保存TOF的高16位
        settings[REG16_INTERNAL_TOF_FREQ] = (char)((char)(tofFreq & 0x0ffff));                                                   // Reg16仅保存TOF的低16位，完整整数在img中合成
        settings[2] = (char)(0x04);
        settings[REG25_PRINT_ENABLE] = (char)(0x07);  // 循环时R25=0x00040007（固定值，b18=1, b[17:2]=1, b[1:0]=3）
        ExtGpio.setFpgaState(ExtGpio.FPGA_STATE_SETTING);
        FpgaGpioOperation.writeData(FpgaGpioOperation.DATA_GENRE_IGNORE, FpgaGpioOperation.FPGA_STATE_SETTING, settings, settings.length * 2);

        try{Thread.sleep(2000);}catch(Exception e){}
        settings[REG25_PRINT_ENABLE] &= (char)0x00;

        ExtGpio.setFpgaState(ExtGpio.FPGA_STATE_SETTING);
        FpgaGpioOperation.writeData(FpgaGpioOperation.DATA_GENRE_IGNORE, FpgaGpioOperation.FPGA_STATE_SETTING, settings, settings.length * 2);
    }
// End of H.M.Wang 2025-3-19 追加一个循环功能
}