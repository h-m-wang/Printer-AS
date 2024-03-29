package com.industry.printer.Utils;

import android.content.res.AssetManager;

import com.industry.printer.PrinterApplication;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by hmwan on 2023/12/29.
 */

public class LibUpgrade {
    private static String TAG = LibUpgrade.class.getSimpleName();

    public LibUpgrade() {

    }

    public boolean upgradeKOs(DataOutputStream os, String prefix, String ko) {
        boolean ret = false;

        try {
            String path = ConfigPath.getKoPath(prefix);
            if(StringUtil.isEmpty(path)) {
                Debug.e(TAG, "Source file[" + prefix + "] not exist.");
                return false;
            }
// H.M.Wang 2024-3-12 将升级ko的方法，从使用USB根目录下的固定.ko文件名，改为使用KKK_xxxxx.ko的可变名称格式
            File srcKoFile = new File(path.substring(0, path.lastIndexOf(".")) + ".ko");
            File srcMD5File = new File(path.substring(0, path.lastIndexOf(".")) + ".txt");
// End of H.M.Wang 2024-3-12 将升级ko的方法，从使用USB根目录下的固定.ko文件名，改为使用KKK_xxxxx.ko的可变名称格式

            if(!srcKoFile.exists() || !srcMD5File.exists()) {
                Debug.e(TAG, "Source ko or md5 not exists.");
                return false;
            }

            BufferedReader br = new BufferedReader(new FileReader(srcMD5File));
            String srcMD5Read = br.readLine();

            String srcMD5Cal = CypherUtils.getFileMD5(srcKoFile);

            if(!srcMD5Read.equalsIgnoreCase(srcMD5Cal)) {
                Debug.e(TAG, "Source md5 not match.");
                return false;
            }

            String dstMD5 = CypherUtils.getFileMD5(new File("/system/vendor/modules/" + ko));
            if(!srcMD5Cal.equalsIgnoreCase(dstMD5)) {
                FileUtil.writeFile("/data/camera/" + ko, new FileInputStream(srcKoFile));
                Debug.d(TAG, "/data/camera/" + ko + " written.");
                Thread.sleep(100);

                Debug.d(TAG, "chmod 0644 /data/camera/" + ko);
                os.writeBytes("chmod 0644 /data/camera/" + ko + "\n");

                if(!srcMD5Cal.equalsIgnoreCase(CypherUtils.getFileMD5("/data/camera/" + ko))) {
                    Debug.e(TAG, "Copy to temp failed.");
                    return false;
                }
                ret = true;
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
            if(!CypherUtils.getFileMD5(file).equalsIgnoreCase(CypherUtils.getStreamMD5(is))) {
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
            if(!CypherUtils.getFileMD5(file).equalsIgnoreCase(CypherUtils.getStreamMD5(is))) {
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
            if(!CypherUtils.getFileMD5(file).equalsIgnoreCase(CypherUtils.getStreamMD5(is))) {
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
            if(!CypherUtils.getFileMD5(file).equalsIgnoreCase(CypherUtils.getStreamMD5(is))) {
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
            if(!CypherUtils.getFileMD5(file).equalsIgnoreCase(CypherUtils.getStreamMD5(is))) {
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
