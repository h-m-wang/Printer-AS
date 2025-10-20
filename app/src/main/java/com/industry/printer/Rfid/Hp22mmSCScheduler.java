package com.industry.printer.Rfid;

import android.content.Context;

public class Hp22mmSCScheduler  implements IInkScheduler {
    private int mHeads;

    public Hp22mmSCScheduler(Context ctx) {
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
