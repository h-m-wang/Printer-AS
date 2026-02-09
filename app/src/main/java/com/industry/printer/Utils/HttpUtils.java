package com.industry.printer.Utils;

import android.content.ContentValues;
import android.util.Log;

import com.google.zxing.Charsets.StandardCharsets;
import com.industry.printer.ThreadPoolManager;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

// H.M.Wang 2025-9-8 增加新类
public class HttpUtils {
    private static final String TAG = HttpUtils.class.getSimpleName();

    public interface HttpResponseListener {
        public void onReceived(String str);
    }
    private HttpResponseListener mListener;
    private String mUri;
    private String mMethod;
    private String mParams;
    private ContentValues mHeaderParams;

    public HttpUtils() {
        mListener = null;
        mUri = "";
        mMethod = "";
        mParams = "";
        mHeaderParams = new ContentValues();
    }

    public HttpUtils setUrl(String uri) {
        mUri = uri;
        return this;
    }

    public HttpUtils setMethod(String method) {
        mMethod = method;
        return this;
    }

    public HttpUtils setParams(String params) {
        mParams = params;
        return this;
    }

    public HttpUtils setHeaderParams(String title, String value) {
        mHeaderParams.put(title, value);
        return this;
    }

    public HttpUtils setListeners(HttpResponseListener cb) {
        mListener = cb;
        return this;
    }

    public void access() {
        try {
            URL url = new URL(mUri);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            con.setRequestMethod(mMethod);
            con.setRequestProperty("Content-Type", "application/json; utf-8");
            con.setRequestProperty("Accept", "application/json");
            for(String key : mHeaderParams.keySet()) {
                con.setRequestProperty(key, mHeaderParams.getAsString(key));
            }
            con.setConnectTimeout(15000);
            con.setReadTimeout(30000);

            if("POST".equalsIgnoreCase(mMethod)) {
                if(null != mParams && !mParams.isEmpty()) {
                    con.setDoOutput(true);
                    OutputStream os = con.getOutputStream();
                    byte[] input = mParams.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }

            int responseCode = con.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) { // 200
                InputStream is = con.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String buf;

                while((buf = br.readLine()) != null) {
                    Debug.d(TAG, buf);
                    if(null != mListener) mListener.onReceived(buf);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(null != mListener) mListener.onReceived(null);   // 标识接收作业结束
    }
}
