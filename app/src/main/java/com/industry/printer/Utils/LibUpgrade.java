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

    private boolean upgradeKO(DataOutputStream os, String prefix, String ko) {
        boolean ret = false;

        try {
            String path = ConfigPath.getKoPath(prefix);
            if(StringUtil.isEmpty(path)) {
                Debug.e(TAG, "Source file[" + prefix + "] does not exist.");
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

// H.M.Wang 2024-5-10 MD5值相同的文件升级也执行，返回真
//            String dstMD5 = CypherUtils.getFileMD5(new File("/system/vendor/modules/" + ko));
//            if(!srcMD5Cal.equalsIgnoreCase(dstMD5)) {
// End of H.M.Wang 2024-5-10 MD5值相同的文件升级也执行，返回真
// H.M.Wang 2024-11-6 修改A133平台的临时目录，因为没有camera目录，改用audio_d目录
                String koPath = "";
                if(PlatformInfo.isA133Product()) {
                    koPath = "/data/audio_d/";
                } else {
                    koPath = "/data/camera/";
                }
// End of H.M.Wang 2024-11-6 修改A133平台的临时目录，因为没有camera目录，改用audio_d目录

                FileUtil.writeFile(koPath + ko, new FileInputStream(srcKoFile));
                Debug.d(TAG, koPath + ko + " written.");
                Thread.sleep(100);

                Debug.d(TAG, "chmod 0644 " + koPath + ko);
                os.writeBytes("chmod 0644 " + koPath + ko + "\n");

                if(!srcMD5Cal.equalsIgnoreCase(CypherUtils.getFileMD5(koPath + ko))) {
                    Debug.e(TAG, "Copy to temp failed.");
                    return false;
                }
                ret = true;
//            }
        } catch(Exception e) {
            Debug.e(TAG, e.getMessage());
            e.printStackTrace();
        }

        return ret;
    }

    private boolean upgradeSO(DataOutputStream os, String so) {
        boolean ret = false;
        InputStream is = null;
        String libPath = "/system/lib/";

        try {
            File file = new File(libPath + so);
            AssetManager assetManager = PrinterApplication.getInstance().getAssets();
            is = assetManager.open(so);

            Debug.d(TAG, "Updating [" + so + "] ...");
            boolean needUpdate = false;
            if(!file.exists()) {
                Debug.d(TAG, file.getPath() + " does not exist.");
                needUpdate = true;
            } else {
                Debug.d(TAG, "FileMD5: [" + CypherUtils.getFileMD5(file) + "].");
                Debug.d(TAG, "AssetMD5: [" + CypherUtils.getStreamMD5(is) + "].");
                needUpdate = !CypherUtils.getFileMD5(file).equalsIgnoreCase(CypherUtils.getStreamMD5(is));
            }
            if(needUpdate) {
// H.M.Wang 2020-12-26 追加硬件库复制功能
                Debug.d(TAG, "chmod 777 " + libPath + so);
                os.writeBytes("chmod 777 " + libPath + so);
                Thread.sleep(100);

                is.reset();
                FileUtil.writeFile(libPath + so, is);
// End of H.M.Wang 2020-12-26 追加硬件库复制功能
                ret = true;
            } else {
                Debug.d(TAG, "Do not need update");
            }
        } catch(Exception e) {
            Debug.e(TAG, e.getMessage());
        } finally {
            try{if(null != is) is.close();}catch(IOException e){}
        }
        return ret;
    }

    public boolean upgradeSOs() {
        boolean ret = false;
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            Thread.sleep(100);
            if(!PlatformInfo.isA133Product()) {		// 只有A20版本才通过复制asset->system/lib的方式升级so文件，A133使用其他方式升级
                ret |= upgradeSO(os, Configs.HARDWARE_SO);
                ret |= upgradeSO(os, Configs.NATIVEGRAPHIC_SO);
                ret |= upgradeSO(os, Configs.SMARTCARD_SO);
                ret |= upgradeSO(os, Configs.SERIAL_SO);
                ret |= upgradeSO(os, Configs.HP22MM_SO);
            }
        } catch(IOException e) {
            Debug.e(TAG, e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            Debug.e(TAG, e.getMessage());
            e.printStackTrace();
        }
        return ret;
    }

    public boolean upgradeKOs() {
        boolean ret = false;
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            Thread.sleep(100);

            ret |= upgradeKO(os, Configs.PREFIX_FPGA_SUNXI_KO, Configs.FPGA_SUNXI_KO);
            ret |= upgradeKO(os, Configs.PREFIX_EXT_GPIO_KO, Configs.EXT_GPIO_KO);
            ret |= upgradeKO(os, Configs.PREFIX_GSLX680_KO, Configs.GSLX680_KO);
            ret |= upgradeKO(os, Configs.PREFIX_RTC_DS1307_KO, Configs.RTC_DS1307_KO);
        } catch(IOException e) {
            Debug.e(TAG, e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            Debug.e(TAG, e.getMessage());
            e.printStackTrace();
        }
        return ret;
    }

    public boolean copyToTempForA133(String file, String dst) {
        String tmpPath, srcPath, dstPath;
        File srcFile;

        try {
            tmpPath = "/data/audio_d" + (file.startsWith(File.separator) ? "" : File.separator) + file;
            srcPath = ConfigPath.getUpgradePath() + (file.startsWith(File.separator) ? "" : File.separator) + file;
            dstPath = dst + (file.startsWith(File.separator) ? "" : File.separator) + file;
            Debug.d(TAG, "Upgrade [" + srcPath + "] -> [" + tmpPath + "] -> [" + dstPath + "]");

            srcFile = new File(srcPath);
            if(srcFile.exists()) {
                if(!CypherUtils.getFileMD5(srcFile).equalsIgnoreCase(CypherUtils.getFileMD5(dstPath))) {
                    FileUtil.writeFile(tmpPath, new FileInputStream(srcFile));
                    Debug.d(TAG, "Done");
                    return true;
                } else {
                    Debug.d(TAG, "[" + srcPath + "] is same as [" + dstPath + "]");
                }
            } else {
                Debug.d(TAG, "[" + srcPath + "] not exist.");
            }
        } catch(Exception e) {
            Debug.e(TAG, e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean upgradeLibs() {
        boolean ret = false;
        if(PlatformInfo.isA133Product()) {
            ret |= copyToTempForA133(Configs.FPGA_SUNXI_KO, "/vendor/modules");
            ret |= copyToTempForA133(Configs.EXT_GPIO_KO, "/vendor/modules");
            ret |= copyToTempForA133(Configs.HARDWARE_SO, "/system/lib");
            ret |= copyToTempForA133(Configs.NATIVEGRAPHIC_SO, "/system/lib");
            ret |= copyToTempForA133(Configs.SMARTCARD_SO, "/system/lib");
            ret |= copyToTempForA133(Configs.SERIAL_SO, "/system/lib");
            ret |= copyToTempForA133(Configs.HP22MM_SO, "/system/lib");
        }
        return ret;
    }
}
