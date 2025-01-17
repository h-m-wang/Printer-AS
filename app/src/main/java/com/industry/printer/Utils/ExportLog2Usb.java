package com.industry.printer.Utils;

        import com.industry.printer.R;

        import java.io.BufferedReader;
        import java.io.File;
        import java.io.IOException;
        import java.io.InputStreamReader;
        import java.io.RandomAccessFile;
        import java.util.ArrayList;

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
            }
            pid = Runtime.getRuntime().exec("umount " + path);
            if(null != pid) {
                Debug.d(TAG, "Export Log to " + path);
                pid.waitFor();
            }
            try{Thread.sleep(5000);}catch(Exception e){}
        } catch(Exception e) {
            Debug.e(TAG, e.getMessage());
        }
    }

// H.M.Wang 2025-1-11 增加一个手机hp22mm库错误信息的功能
    private static RandomAccessFile S22mmErrLog = null;
    private static String LastErrString = "";

    public static void writeHp22mmErrLog(String str) {
        if(str.equals(LastErrString)) return;

        if(null == S22mmErrLog) {
            try {
                File log = new File(Configs.CONFIG_PATH_FLASH + "/hp22mm_err_log.txt");
                if (!log.exists()) {
                    log.createNewFile();
                }
                S22mmErrLog = new RandomAccessFile(log, "rw");
                S22mmErrLog.setLength(0);
            } catch(IOException e) {
                Debug.e(TAG, e.getMessage());
            }
        }

        if(null != S22mmErrLog) {
            try {
                S22mmErrLog.seek(S22mmErrLog.length());
                S22mmErrLog.writeUTF(str);
                LastErrString = str;
            } catch(IOException e) {
                Debug.e(TAG, e.getMessage());
            }
        }
    }

    public static void exportHp22mmErrLog() {
        final ArrayList<String> usbs = ConfigPath.getMountedUsb();
        if (usbs.size() <= 0) {
            return;
        }

        FileUtil.copyFile(Configs.CONFIG_PATH_FLASH + "/hp22mm_err_log.txt", usbs.get(0) + "/hp22mm_err_log.txt");
    }

    public static String getFirstLine() {
        try {
            S22mmErrLog.seek(0);
            return S22mmErrLog.readUTF();
        } catch(IOException e) {
            Debug.e(TAG, e.getMessage());
        }
        return "";
    }

    public static String getNextLine() {
        try {
            return S22mmErrLog.readUTF();
        } catch(IOException e) {
            Debug.e(TAG, e.getMessage());
        }
        return "";
    }
// End of H.M.Wang 2025-1-11 增加一个手机hp22mm库错误信息的功能
}
