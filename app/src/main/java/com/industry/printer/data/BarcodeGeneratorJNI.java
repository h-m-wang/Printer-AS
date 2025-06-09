package com.industry.printer.data;

import com.industry.printer.Utils.Debug;

public class BarcodeGeneratorJNI {
    static {
        System.loadLibrary("BarcodeGeneratorJNI"); // Load the JNI library
    }

    public static void loadLibrary() {
        Debug.d("BarcodeGeneratorJNI", "Loading BarcodeGeneratorJNI library...");
    }

    public static native ZIntSymbol generateBarcode(ZIntSymbol obj, String text, int rotateAngle);
}
