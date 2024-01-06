package com.industry.printer.Utils;

import android.content.res.AssetManager;

import com.industry.printer.PrinterApplication;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by hmwan on 2023/12/29.
 */

public class LibUpgrade {
    private static String TAG = LibUpgrade.class.getSimpleName();

    public LibUpgrade() {

    }

    public boolean upgradeKOs(DataOutputStream os, String ko) {
        boolean ret = false;

        try {
            String path = ConfigPath.getKoPath(ko);
            if(StringUtil.isEmpty(path)) {
                Debug.e(TAG, "Source file not indicated.");
                return false;
            }
            File src = new File(path);
            if(!src.exists()) {
                Debug.e(TAG, "Source ko not exists.");
                return false;
            }

            Debug.d(TAG, "[" + ko + "]");

            String srcMD5 = CypherUtils.getFileMD5(src);
            Debug.d(TAG, "SrcMD5: [" + srcMD5 + "].");

            ;
            if(!new File(path.substring(0, path.lastIndexOf(File.separator)+1) + srcMD5 + ".dat").exists()) {
                Debug.e(TAG, "Source md5 file not exists or incorrect.");
                return false;
            }

            String dstMD5 = CypherUtils.getFileMD5(new File("/system/vendor/modules/" + ko));
            Debug.d(TAG, "DstMD5: [" + dstMD5 + "].");
            if(!srcMD5.equals(dstMD5)) {
                FileUtil.writeFile("/data/camera/" + ko, new FileInputStream(src));
                Debug.d(TAG, "/data/camera/" + ko + " written.");
                Thread.sleep(100);

                Debug.d(TAG, "chmod 0644 /data/camera/" + ko);
                os.writeBytes("chmod 0644 /data/camera/" + ko + "\n");

                if(!srcMD5.equals(CypherUtils.getFileMD5("/data/camera/" + ko))) {
                    Debug.e(TAG, "Copy to temp failed.");
                    return false;
                }
                ret = true;
            } else {
                File dst = new File("/data/camera/" + ko);
                if(dst.exists()) {
                    dst.delete();
                }
            }
        } catch(Exception e) {
            Debug.e(TAG, e.getMessage());
            e.printStackTrace();
        }

        return ret;
    }

    public boolean upgradeHardwareSO(DataOutputStream os) {
        boolean ret = false;
        InputStream is = null;

        try {
            File file = new File("/system/lib/" + Configs.HARDWARE_SO);
            AssetManager assetManager = PrinterApplication.getInstance().getAssets();
            is = assetManager.open(Configs.HARDWARE_SO);

            Debug.d(TAG, "[HardwareSO]");
            Debug.d(TAG, "FileMD5: [" + CypherUtils.getFileMD5(file) + "].");
            Debug.d(TAG, "AssetMD5: [" + CypherUtils.getStreamMD5(is) + "].");
            is.reset();
            if(!CypherUtils.getFileMD5(file).equals(CypherUtils.getStreamMD5(is))) {
// H.M.Wang 2020-12-26 追加硬件库复制功能
                Debug.d(TAG, "chmod 777 /system/lib/" + Configs.HARDWARE_SO);
                os.writeBytes("chmod 777 /system/lib/" + Configs.HARDWARE_SO);
                Thread.sleep(100);

                is.reset();
                FileUtil.writeFile("/system/lib/" + Configs.HARDWARE_SO, is);
// End of H.M.Wang 2020-12-26 追加硬件库复制功能
                ret = true;
            }
        } catch(Exception e) {
            Debug.e(TAG, e.getMessage());
        } finally {
            try{if(null != is) is.close();}catch(IOException e){}
        }
        return ret;
    }

    public boolean upgradeNativeGraphicSO(DataOutputStream os) {
        boolean ret = false;
        InputStream is = null;

        try {
            File file = new File("/system/lib/" + Configs.NATIVEGRAPHIC_SO);
            AssetManager assetManager = PrinterApplication.getInstance().getAssets();
            is = assetManager.open(Configs.NATIVEGRAPHIC_SO);

            Debug.d(TAG, "[NativeGraphicSO]");
            Debug.d(TAG, "FileMD5: [" + CypherUtils.getFileMD5(file) + "].");
            Debug.d(TAG, "AssetMD5: [" + CypherUtils.getStreamMD5(is) + "].");
            is.reset();
            if(!CypherUtils.getFileMD5(file).equals(CypherUtils.getStreamMD5(is))) {
                Debug.d(TAG, "chmod 777 /system/lib/" + Configs.NATIVEGRAPHIC_SO);
                os.writeBytes("chmod 777 /system/lib/" + Configs.NATIVEGRAPHIC_SO);
                Thread.sleep(100);

                is.reset();
                FileUtil.writeFile("/system/lib/" + Configs.NATIVEGRAPHIC_SO, is);
                ret = true;
            }
        } catch(Exception e) {
            Debug.e(TAG, e.getMessage());
        } finally {
            try{if(null != is) is.close();}catch(IOException e){}
        }
        return ret;
    }

    public boolean upgradeSmartCardSO(DataOutputStream os) {
        boolean ret = false;
        InputStream is = null;

        try {
            File file = new File("/system/lib/" + Configs.SMARTCARD_SO);
            AssetManager assetManager = PrinterApplication.getInstance().getAssets();
            is = assetManager.open(Configs.SMARTCARD_SO);

            Debug.d(TAG, "[SmartCardSO]");
            Debug.d(TAG, "FileMD5: [" + CypherUtils.getFileMD5(file) + "].");
            Debug.d(TAG, "AssetMD5: [" + CypherUtils.getStreamMD5(is) + "].");
            is.reset();
            if(!CypherUtils.getFileMD5(file).equals(CypherUtils.getStreamMD5(is))) {
                Debug.d(TAG, "chmod 777 /system/lib/" + Configs.SMARTCARD_SO);
                os.writeBytes("chmod 777 /system/lib/" + Configs.SMARTCARD_SO);
                Thread.sleep(100);

                is.reset();
                FileUtil.writeFile("/system/lib/" + Configs.SMARTCARD_SO, is);
                ret = true;
            }
        } catch(Exception e) {
            Debug.e(TAG, e.getMessage());
        } finally {
            try{if(null != is) is.close();}catch(IOException e){}
        }
        return ret;
    }

    public boolean upgradeSerialSO(DataOutputStream os) {
        boolean ret = false;
        InputStream is = null;

        try {
            File file = new File("/system/lib/" + Configs.SERIAL_SO);
            AssetManager assetManager = PrinterApplication.getInstance().getAssets();
            is = assetManager.open(Configs.SERIAL_SO);

            Debug.d(TAG, "[SerialSO]");
            Debug.d(TAG, "FileMD5: [" + CypherUtils.getFileMD5(file) + "].");
            Debug.d(TAG, "AssetMD5: [" + CypherUtils.getStreamMD5(is) + "].");
            is.reset();
            if(!CypherUtils.getFileMD5(file).equals(CypherUtils.getStreamMD5(is))) {
                Debug.d(TAG, "chmod 777 /system/lib/" + Configs.SERIAL_SO);
                os.writeBytes("chmod 777 /system/lib/" + Configs.SERIAL_SO);
                Thread.sleep(100);

                is.reset();
                FileUtil.writeFile("/system/lib/" + Configs.SERIAL_SO, is);
                ret = true;
            }
        } catch(Exception e) {
            Debug.e(TAG, e.getMessage());
        } finally {
            try{if(null != is) is.close();}catch(IOException e){}
        }
        return ret;
    }

    public boolean upgradeHp22mmSO(DataOutputStream os) {
        boolean ret = false;
        InputStream is = null;

        try {
            File file = new File("/system/lib/" + Configs.HP22MM_SO);
            AssetManager assetManager = PrinterApplication.getInstance().getAssets();
            is = assetManager.open(Configs.HP22MM_SO);

            Debug.d(TAG, "[Hp22mmSO]");
            Debug.d(TAG, "FileMD5: [" + CypherUtils.getFileMD5(file) + "].");
            Debug.d(TAG, "AssetMD5: [" + CypherUtils.getStreamMD5(is) + "].");
            is.reset();
            if(!CypherUtils.getFileMD5(file).equals(CypherUtils.getStreamMD5(is))) {
                Debug.d(TAG, "chmod 777 /system/lib/" + Configs.HP22MM_SO);
                os.writeBytes("chmod 777 /system/lib/" + Configs.HP22MM_SO);
                Thread.sleep(100);

                is.reset();
                FileUtil.writeFile("/system/lib/" + Configs.HP22MM_SO, is);
                ret = true;
            }
        } catch(Exception e) {
            Debug.e(TAG, e.getMessage());
        } finally {
            try{if(null != is) is.close();}catch(IOException e){}
        }
        return ret;
    }
}
