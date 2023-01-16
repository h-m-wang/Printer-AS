package com.industry.printer.Rfid;

import android.content.Context;

public class N_RfidScheduler implements IInkScheduler {
    private String TAG = N_RfidScheduler.class.getSimpleName();

    public static N_RfidScheduler mInstance = null;

    private Context mContext;
    private int mHeads;

    public static N_RfidScheduler getInstance(Context ctx) {
        if (mInstance == null) {
            mInstance = new N_RfidScheduler(ctx);
        }
        return mInstance;

    }

    public N_RfidScheduler(Context ctx) {
        mContext = ctx;
        mHeads = 0;
    }

    @Override
    public void init(int heads) {
        mHeads = heads;
    }

    @Override
    public int count() {
        return mHeads;
    }

    @Override
    public void schedule() {
    }

    @Override
    public void doAfterPrint() {
    }
}
