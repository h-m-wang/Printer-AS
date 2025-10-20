package com.industry.printersupervisor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by hmwan on 2023/12/22.
 */

public class CypherUtils {
    private static String TAG = CypherUtils.class.getSimpleName();

    private static String toHexString(byte[] buf) {
        if (buf == null) return "";
        final String HEX = "0123456789ABCDEF";
        StringBuffer result = new StringBuffer(2 * buf.length);
        for (int i = 0; i<buf.length; i++) {
            result.append(HEX.charAt((buf[i] >> 4) & 0x0f)).append(HEX.charAt(buf[i] & 0x0f));
        }
        return result.toString();
    }

    public static String getMD5(byte[] fileData) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(fileData);
            byte[] m = md5.digest();
            return toHexString(m);
        } catch(NoSuchAlgorithmException e) {
            Debug.e(TAG, "NoSuchAlgorithmException: " + e.getMessage());
        } catch (Exception e) {
            Debug.e(TAG, "Exception: " + e.getMessage());
        }
        return "";
    }

    public static String getMD5(String md5) {
        return getMD5(md5.getBytes());
    }

    public static String getStreamMD5(InputStream is) {
        try {
            if(null != is) {
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                byte buffer[] = new byte[1024];
                int len;

                while ((len = is.read(buffer, 0, 1024)) != -1) {
                    md5.update(buffer, 0, len);
                }

                byte[] m = md5.digest();

                return toHexString(m);
            } else {
                Debug.e(TAG, "InputStream null.");
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getFileMD5(File file) {
        try {
            if(null != file && file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                Debug.e(TAG, "fis [" + fis + "].");
                String md5 = getStreamMD5(fis);
                fis.close();
                return md5;
            } else {
                Debug.e(TAG, "File [" + file.getPath() + "] not exist.");
            }
        } catch (FileNotFoundException e) {
            Debug.e(TAG, "FileNotFoundException: " + e.getMessage());
        } catch (Exception e) {
            Debug.e(TAG, "Exception: " + e.getMessage());
        }
        return "";
    }

    public static String getFileMD5(String fileName) {
        return getFileMD5(new File(fileName));
    }

}
