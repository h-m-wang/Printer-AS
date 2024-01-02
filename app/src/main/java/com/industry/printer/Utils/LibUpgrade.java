package com.industry.printer.Utils;

import android.content.res.AssetManager;

import com.industry.printer.PrinterApplication;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by hmwan on 2023/12/29.
 */

public class LibUpgrade {
    private static String TAG = LibUpgrade.class.getSimpleName();

    public LibUpgrade() {

    }

    public boolean upgradeFpgaSunxiKO(DataOutputStream os) {
        boolean ret = false;
        InputStream is = null;

        try {
            File file = new File("/system/vendor/modules/" + Configs.FPGA_SUNXI_KO);
            AssetManager assetManager = PrinterApplication.getInstance().getAssets();
            is = assetManager.open(Configs.FPGA_SUNXI_KO);

            Debug.d(TAG, "[FpgaSunxiKO]");
            Debug.d(TAG, "FileMD5: [" + CypherUtils.getFileMD5(file) + "].");
            Debug.d(TAG, "AssetMD5: [" + CypherUtils.getStreamMD5(is) + "].");
            is.reset();
            if(!CypherUtils.getFileMD5(file).equals(CypherUtils.getStreamMD5(is))) {
                is.reset();
                FileUtil.writeFile("/data/camera/" + Configs.FPGA_SUNXI_KO, is);
                Debug.d(TAG, "/data/camera/" + Configs.FPGA_SUNXI_KO + " written.");
                Thread.sleep(100);

                Debug.d(TAG, "chmod 0644 /data/camera/" + Configs.FPGA_SUNXI_KO);
                os.writeBytes("chmod 0644 /data/camera/" + Configs.FPGA_SUNXI_KO + "\n");

                ret = true;
            } else {
                file = new File("/data/camera/" + Configs.FPGA_SUNXI_KO);
                if(file.exists()) {
                    file.delete();
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            try{if(null != is) is.close();}catch(IOException e){}
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
