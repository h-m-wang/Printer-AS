package com.industry.printer.Utils;

        import java.io.BufferedReader;
        import java.io.InputStreamReader;

/**
 * Created by hmwan on 2023/9/4.
 */

public class ExportLog2Usb {
    public static final String TAG = ExportLog2Usb.class.getSimpleName();

    public static void exportLog(String path) {
        Process pid = null;
        try {
            pid = Runtime.getRuntime().exec("logcat -v time -d -f " + path);
            if(null != pid) {
                Debug.d(TAG, "Export Log to " + path);
                pid.waitFor();
//                pid.destroy();
            }
        } catch(Exception e) {
            Debug.e(TAG, e.getMessage());
        }
    }
}
