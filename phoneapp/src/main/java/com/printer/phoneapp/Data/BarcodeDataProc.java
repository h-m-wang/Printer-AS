package com.printer.phoneapp.Data;

import android.util.Log;

import com.printer.phoneapp.Sockets.BLEDataXfer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class BarcodeDataProc {
    private static final String TAG = BarcodeDataProc.class.getSimpleName();

    private static String getBodyJsUrl(String scaned) {
        if(null == scaned || scaned.isEmpty()) return null;
        String str = null;

        try {
            URL url = new URL(scaned);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            if(con.getResponseCode() != 200) return null;

            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String buf;
            while((buf = br.readLine()) != null) {
                int index = buf.indexOf("Body.js");
                if(index >= 0) {
                    int s_index = buf.substring(0, index).lastIndexOf("'");
                    int e_index = buf.indexOf("'", index);
                    if(s_index >= 0 && e_index >= 0) {
                        str = buf.substring(s_index+1, e_index);
                        Log.d(TAG, "BodyJsURL = " + str);
                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return str;
    }

    private static String[] getDataFromJs(String jsUrl) {
        if(null == jsUrl || jsUrl.isEmpty()) return null;
        String[] params = null;

        try {
            URL url = new URL(jsUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            if(con.getResponseCode() != 200) return null;

            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String buf;
            while((buf = br.readLine()) != null) {
                int s_index = buf.indexOf("****(");
                int e_index = buf.indexOf(")", s_index);
                if(s_index >= 0 && e_index >= 0) {
                    params = buf.substring(s_index+5, e_index).split("_");
                    for(String s : params) {
                        Log.d(TAG, s);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return params;
    }

    private static String[] getDataFromPowerChinaWeb(String scaned) {
        if(null == scaned || scaned.isEmpty()) return null;
        boolean dataFound = false;
        String[] params = new String[5];
        String startTag = "<div class=\"weui-cell__ft\">";
        String endTag = "</div>";

        try {
            URL url = new URL(scaned);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            if(con.getResponseCode() != 200) return null;

            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String buf;
            boolean checkFlag = false, rmoveStd = false;
            int curInsPos = 0;

            while((buf = br.readLine()) != null) {
                Log.d("Barcode", buf);
                int hit_pos1 = buf.indexOf("管片尺寸");
                int hit_pos2 = buf.indexOf("管片配筋比");
                int hit_pos3 = buf.indexOf("流水号");
                int hit_pos4 = buf.indexOf("模具型号");
                int hit_pos5 = buf.indexOf("注浆孔");  // 2024-11-6 增加客户需求，当内容为标准是清空，其余保留
                if(hit_pos1 >= 0 || hit_pos2 >= 0 || hit_pos3 >= 0 || hit_pos4 >= 0 || hit_pos5 >= 0) {
                    Log.d("Barcode20", buf);
                    checkFlag = true;
                    if(hit_pos5 >= 0) rmoveStd = true; else rmoveStd = false;
                }
                if(checkFlag) {
                    int s_index = buf.indexOf(startTag);
                    int e_index = buf.indexOf(endTag, s_index);
                    if (s_index >= 0 && e_index >= 0) {
                        if(curInsPos < 5) {
                            String str = buf.substring(s_index + startTag.length(), e_index);
                            Log.d("Barcode21", str);
                            if(rmoveStd) str = str.replace("标准", "");
                            params[curInsPos] = str;
                            Log.d("Barcode22", "[" + params[curInsPos] + "]");
                            curInsPos++;
                        }
                        checkFlag = false;
                        dataFound = true;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return (dataFound ? params : null);
    }

    public static String makeup650CmdString(String scaned) {
        Log.d(TAG, scaned);

/*        String jsUrl = getBodyJsUrl(scaned);
        if(null == jsUrl || jsUrl.isEmpty()) return null;

        String[] params = getDataFromJs(jsUrl);
        if(null == params || params.length == 0) return null;
*/
        String[] params = getDataFromPowerChinaWeb(scaned);
        if(null == params || params.length == 0) return null;

        StringBuilder sb = new StringBuilder();
        sb.append("000B|0000|650|");
        for(int i=0; i<11; i++) {
            if(i < params.length) {
                if(null != params[i]) sb.append(params[i]);
            }
            if(i < 10) sb.append(",");
        }
        sb.append("|0|0000|0|0008|0D0A");

        return sb.toString();
    }
}
