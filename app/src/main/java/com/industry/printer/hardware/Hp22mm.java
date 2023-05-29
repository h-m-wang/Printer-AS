package com.industry.printer.hardware;

import com.industry.printer.Utils.Debug;

public class Hp22mm {
    static {
        System.loadLibrary("hp22mm");
    }

    public static void loadLibrary() {
        Debug.d("hp22mm", "Loading Hp22mm library...");
    }

    static public native int init();
    static public native int init_ids();
    static public native String ids_get_sys_info();
    static public native int init_pd();
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
    static public native int pd_get_print_head_status(int penIdx);
    static public native String pd_get_print_head_status_info();
    static public native int pd_sc_get_status(int penIdx);
    static public native String pd_sc_get_status_info();
    static public native int pd_sc_get_info(int penIdx);
    static public native String pd_sc_get_info_info();
    static public native int DeletePairing();
    static public native int DoPairing(int penIdx);
    static public native int DoOverrides(int penIdx);
    static public native int Pressurize();
    static public native String getPressurizedValue();
    static public native int Depressurize();
    static public native int UpdatePDFW();
    static public native int UpdateFPGAFlash();
    static public native int UpdateIDSFW();
    static public native int startPrint();
    static public native int stopPrint();
    static public native String dumpRegisters();
    static public native int Write1Column();
    static public native int Write1KB();
    static public native int Write10Columns();
    static public native int WriteSPIFPGA();
}
