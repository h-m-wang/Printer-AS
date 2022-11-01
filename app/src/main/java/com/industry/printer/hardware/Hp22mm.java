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
    static public native int init_pd();

}
