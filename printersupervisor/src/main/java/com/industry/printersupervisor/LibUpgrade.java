package com.industry.printersupervisor;

import java.io.File;
import java.io.FileInputStream;

/**
 * Created by hmwan on 2023/12/29.
 */

public class LibUpgrade {
    private static String TAG = LibUpgrade.class.getSimpleName();

    public LibUpgrade() {

    }

    public boolean copyToTempForA133(String file, String dst) {
        String tmpPath, srcPath, dstPath;
        File srcFile;

        try {
            tmpPath = "/data/audio_d" + (file.startsWith(File.separator) ? "" : File.separator) + file;
            if(ConfigPath.getUpgradePath() == null) return false;
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
}
