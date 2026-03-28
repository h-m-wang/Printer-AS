package com.industry.printer.pccommand;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.industry.printer.ControlTabActivity;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.StreamTransport;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class CustomApk {
    private static final String TAG = CustomApk.class.getSimpleName();

    private static final int PORT = 3660;

    private static CustomApk mInstance = null;

    private Context mContext;
    private ControlTabActivity mControlTabActivity;
    private Handler mHandler;
    private PCCommandHandler mSocketHandler;

    private ServerSocket mServerSocket;
    private ClientSocket mClientSocket;
    private boolean mCustomRunning;
    private Thread mCustomThread;

    public static CustomApk getInstance(Context ctx) {
        if(null == mInstance) {
            mInstance = new CustomApk(ctx);
        }
        return mInstance;
    }

    public CustomApk(Context ctx) {
        mContext = ctx;
        mControlTabActivity = null;
        mSocketHandler = null;
        mCustomThread = null;
        mCustomRunning = false;
        mClientSocket = null;

        mHandler = new Handler(){
            public void handleMessage(Message msg) {
                if(null != mSocketHandler)mSocketHandler.handleReCreateResult(msg);
            }
        };
    }

    public void start() {
        if(null == mCustomThread) {
            mCustomThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    mCustomRunning = true;
                    try {
                        mServerSocket = new ServerSocket(PORT);
                        while(mCustomRunning) {
                            Socket client = mServerSocket.accept();
                            client.setSoTimeout(2 * 1000);
                            if(null != mSocketHandler) {
                                mSocketHandler.close();
                            }
                            if(null != mClientSocket) {
                                mClientSocket.close();
                            }

                            mClientSocket = new ClientSocket(client, mContext);
                            mSocketHandler = new PCCommandHandler(
                                    mContext,
                                    mClientSocket.getStreamTransport(),
                                    mControlTabActivity,
                                    mHandler);
                            mSocketHandler.work();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    mCustomThread = null;
                }
            });
            mCustomThread.start();
        }
    }

    public void stop() {
        mCustomRunning = false;
        try {
            if(null != mServerSocket && !mServerSocket.isClosed()) mServerSocket.close();
            mServerSocket = null;
            if(null != mClientSocket) mClientSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setControlTabActivity(ControlTabActivity act) {
        mControlTabActivity = act;
    }
}
