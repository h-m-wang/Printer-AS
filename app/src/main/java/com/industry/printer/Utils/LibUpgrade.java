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

            String dstMD5 = CypherUtils.getFileMD5(new File("/system/vendor/modules/" + ko));
// H.M.Wang 2024-5-10 MD5值相同的文件升级也执行，返回真
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

    public boolean updateSO(DataOutputStream os, String so) {
        boolean ret = false;
        InputStream is = null;
        String libPath;
        if(PlatformInfo.isA133Product()) {
            libPath = "/product/lib/";
        } else {
            libPath = "/system/lib/";
        }

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
}
