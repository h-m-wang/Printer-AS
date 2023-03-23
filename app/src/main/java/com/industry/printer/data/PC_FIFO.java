package com.industry.printer.data;

import android.content.Context;

import com.industry.printer.FileFormat.SystemConfigFile;
import com.industry.printer.Utils.Debug;
import com.industry.printer.pccommand.PCCommandManager;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class PC_FIFO {
    private static final String TAG = PC_FIFO.class.getSimpleName();

    private static PC_FIFO mInstance = null;
    private Context mContext = null;

    private int mPCFIFOSize;                            // PCFIFO数据区的大小
    private ArrayList<String> mPCFIFOBuffer;            // 从PC接收到的数据保存区
    private ArrayList<String> mDeliveredBuffer;         // 已经下发到img，等待打印的数据区

    private Object mPCFIFOLock;                         // mPCFIFOBuffer的Lock
    private Object mDelBufLock;                         // mDeliveredBuffer的Lock

    public boolean PCFIFOEnabled() {
        SystemConfigFile config = SystemConfigFile.getInstance(mContext);
        mPCFIFOSize = config.getParam(SystemConfigFile.INDEX_PC_FIFO);
//        Debug.d(TAG, "PCFIFOEnabled: " + (mPCFIFOSize > 0));
        return mPCFIFOSize > 0;
    }

    public static PC_FIFO getInstance(Context ctx) {
        if (mInstance == null) {
            mInstance = new PC_FIFO(ctx);
        }
        return mInstance;
    }

    private PC_FIFO(Context ctx) {
        mContext = ctx;

        SystemConfigFile config = SystemConfigFile.getInstance(mContext);
        mPCFIFOSize = config.getParam(SystemConfigFile.INDEX_PC_FIFO);
//        mPCFIFOSize = 10;
        mPCFIFOBuffer = new ArrayList<String>();
        mDeliveredBuffer = new ArrayList<String>();
        mPCFIFOLock = new Object();
        mDelBufLock = new Object();
    }

    public int getPCFIFOAvailableSize() {
        if(PCFIFOEnabled())
            return mPCFIFOSize - mPCFIFOBuffer.size();
        else return 0;
    }

    public boolean PCFIFOAvailable() {
//        Debug.d(TAG, "PCFIFOAvailable: " + (mPCFIFOBuffer.size() > 0));
        if(mPCFIFOBuffer.size() <= 0) sendNoDataError();

        return mPCFIFOBuffer.size() > 0;
    }

    public boolean appendToFIFO(String str) {
        boolean ret = false;
        synchronized (mPCFIFOLock) {
            if(mPCFIFOBuffer.size() < mPCFIFOSize) {
                mPCFIFOBuffer.add(str);
                ret = true;
            }
            Debug.d(TAG, "appendToFIFO: (" + mPCFIFOBuffer.size() + "-" + ret + ")[" + str + "]");
        }
        return ret;
    }

    public void useString() {
        synchronized (mPCFIFOLock) {
            Debug.d(TAG, "useString: 0/" + mPCFIFOBuffer.size());
            if(mPCFIFOBuffer.size() > 0) {
                SystemConfigFile.getInstance().setRemoteSeparated(mPCFIFOBuffer.get(0));
                mPCFIFOBuffer.remove(0);
            } else {
                sendNoDataError();
            }
        }
        synchronized (mDelBufLock) {
            mDeliveredBuffer.add(SystemConfigFile.getInstance().getDTBuffer(0));
        }
    }

    public void onPrinted() {
        synchronized (mDelBufLock) {
            PCCommandManager manager = PCCommandManager.getInstance();
            manager.sendMessage("000B|0000|1000|0|" + mDeliveredBuffer.get(0) + "|0|0000|0000|0D0A");
            mDeliveredBuffer.remove(0);
        }
    }

    public void onCompleted() {
        PCCommandManager manager = PCCommandManager.getInstance();
        manager.sendMessage("000B|0000|1000|0|" + SystemConfigFile.getInstance().getDTBuffer(0) + "|0|0001|0000|0D0A");
    }

    public void clearBuffer() {
        synchronized (mPCFIFOLock) {
            mPCFIFOBuffer.clear();
        }
        synchronized (mDelBufLock) {
            mDeliveredBuffer.clear();
        }
    }

    private void sendNoDataError() {
        PCCommandManager manager = PCCommandManager.getInstance();
        manager.sendMessage("000B|0000|1000|0|0000|0|0002|0000|0D0A");
    }
}
