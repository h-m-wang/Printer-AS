package com.industry.printer.data;

import com.industry.printer.Serial.SerialPort;
import com.industry.printer.Utils.Debug;

/**
 * Created by hmwan on 2019/8/16.
 */

public class NativeGraphicJni {
    private static final String TAG = NativeGraphicJni.class.getSimpleName();

    static {
		System.loadLibrary("NativeGraphicJni");
    }

    public static void loadLibrary() {
        Debug.d(TAG, "Loading NativeGraphicJni library...");
    }

    public static native int[] ShiftImage(int[] src, int width, int height, int head, int orgLines, int tarLines);
    public static native byte[] Binarize(int[] src, int width, int height, int head, int value, int reset);
    public static native byte[] BinarizeBmp(Object bmp, int width, int height, int head, int value, int reset);
    public static native int[] GetDots();
    public static native char[] GetBgBuffer(byte[] src, int length, int bytesFeed, int bytesPerHFeed, int bytesPerH, int column, int type);
// H.M.Wang 2026-8-19 为了提高变量生成的速度，启用开窗的办法贴图，详细参照WORD文档《开创处理修改说明》
    public static native char[] GetBgBufferNew(byte[] src, int length, int bytesFeed, int bytesPerHFeed, int bytesPerH, int column, int type, int expandScale);
    public static native int PasteVarByVBin(char[] dst, int columns, int[] digits, char[] refDigitsBin, int bytesPerColInBase, int colPerElements, int sX, int sY, int eY, int expandScale);
    public static native int PasteDynamicBin(char[] dst, byte[] src, int bytesPerCol, int columns, int sX, int sY, int eY, int expandScale);
// End of H.M.Wang 2026-8-19 为了提高变量生成的速度，启用开窗的办法贴图，详细参照WORD文档《开创处理修改说明》
    public static native int PasteBmp2Bin(char[] dst, Object bmp, int width, int height, int bytesPerCol, int sX, int sY, int eY, int orgLines, int tarLines, int expandScale);

    public static native int[] GetPrintDots(char[] src, int length, int bytesPerHFeed, int heads);
}
