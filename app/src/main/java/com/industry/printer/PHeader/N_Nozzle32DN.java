package com.industry.printer.PHeader;

/**
 * Created by hmwan on 2021/9/23.
 */

public class N_Nozzle32DN extends N_Nozzle {

    public N_Nozzle32DN(int index) {
        super(index);

        mType               = Type.NOZZLE_TYPE_32DN;
        mHeads              = 1;
        mEditZoomable       = false;
        mDrawHeight         = 32;
        mBufferHeight       = 32;
        mFactorScale        = 1;
        mShiftable          = true;
        mMirrorable         = false;
        mReversable         = false;
        mRotatable          = true;
        mBuffer8Enable      = true;

        mBigDot             = true;
    }
}