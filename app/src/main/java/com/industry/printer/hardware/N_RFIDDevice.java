/**
 * RFID 存储分配表
 * ————————————————————————————————————————————
 * 	SECTOR    |    BLOCK    |   Description
 * ————————————————————————————————————————————
 *     4      |	     0      |    墨水总量
 * ————————————————————————————————————————————
 *     4      |	     1      |    特征值
 * ————————————————————————————————————————————
 *     4      |      2      |    墨水量
 * ————————————————————————————————————————————
 *     4      |      3      |    秘钥
 * ————————————————————————————————————————————
 *     5      |	     0      |    墨水总量备份
 * ————————————————————————————————————————————
 *     5      |      1      |    特征值备份
 * ————————————————————————————————————————————
 *     5      |      2      |    墨水量备份
 * ————————————————————————————————————————————
 *     5      |      3      |    秘钥
 * ————————————————————————————————————————————
 */

package com.industry.printer.hardware;

import com.industry.printer.Rfid.N_RFIDModule;
import com.industry.printer.Rfid.N_RFIDModuleChecker;
import com.industry.printer.Rfid.N_RFIDModule_M104BPCS;
import com.industry.printer.Rfid.N_RFIDModule_M104BPCS_KX1207;
import com.industry.printer.Rfid.N_RFIDModule_M104DPCS;
import com.industry.printer.Rfid.N_RfidScheduler;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.PlatformInfo;

import java.util.Arrays;

public class N_RFIDDevice {
    public static final String TAG = N_RFIDDevice.class.getSimpleName();

    //  校驗特徵值
    public static final int FEATURE_HIGH = 100;
    public static final int FEATURE_LOW = 1;

    // 墨水量上下限
    public static final int INK_LEVEL_MAX = 100000;
    public static final int INK_LEVEL_MIN = 0;

    private int mIndex = 0;

    // 当前墨水量
    private int mCurInkLevel;
    private boolean mInkLevelModified;
    private int mInkMax;
    private boolean mValid;
    private byte[] mFeature;
    private N_RFIDModule mRFIDModule;
    private int mWriteRetryCount;

    public N_RFIDDevice(int index) {
        mCurInkLevel = 0;
        mInkLevelModified = false;
        mInkMax = 0;
        mFeature = null;
        mValid = false;
        mRFIDModule = null;
        mWriteRetryCount = 0;
        mIndex = index;
    }

    public boolean init() {
        boolean ret;

        mValid = false;

//        if(PlatformInfo.getImgUniqueCode().startsWith("NNM2")) {
//            mRFIDModule = new N_RFIDModule_M104BPCS_KX1207();
//        }

        if(null == mRFIDModule) {
            N_RFIDModuleChecker checker = new N_RFIDModuleChecker();
            switch(checker.check(PlatformInfo.getRfidDevice())) {
                case N_RFIDModuleChecker.RFID_MOD_M104BPCS:
                    mRFIDModule = new N_RFIDModule_M104BPCS();
//                    Debug.d(TAG, "Module Type: " + mRFIDModule.getClass().getSimpleName());
                    break;
                case N_RFIDModuleChecker.RFID_MOD_M104DPCS:
                    mRFIDModule = new N_RFIDModule_M104DPCS();
//                    Debug.d(TAG, "Module Type: " + mRFIDModule.getClass().getSimpleName());
                    break;
                case N_RFIDModuleChecker.RFID_MOD_M104BPCS_KX1207:
                    mRFIDModule = new N_RFIDModule_M104BPCS_KX1207();
//                    Debug.d(TAG, "Module Type: " + mRFIDModule.getClass().getSimpleName());
                    break;
                default:
//                    Debug.d(TAG, "Unknown Module Type");
                    ret = false;
                    break;
            }
        }

        ret = mRFIDModule.open(PlatformInfo.getRfidDevice());
        if(!ret) return false;

        ret = mRFIDModule.initCard();
        if(!ret) return false;

        mInkMax = mRFIDModule.readMaxInkLevel();
        mCurInkLevel = mRFIDModule.readInkLevel();
        mFeature = mRFIDModule.readFeature();

        mValid = checkFeatureCode();

        return mValid;
    }

    public float getLocalInk() {
        return mCurInkLevel;
    }

    public int getMax() {
        return mInkMax;
    }

    public void down() {
        if (mCurInkLevel > 0) {
            mCurInkLevel = mCurInkLevel -1;
// H.M.Wang 2020-4-9 修改当值为212+255*n的时候，再减1
            if((mCurInkLevel+43)/255*255-43 == mCurInkLevel) {
                mCurInkLevel--;
            }
// End of H.M.Wang 2020-4-9 修改当值为212+255*n的时候，再减1

        } else if (mCurInkLevel <= 0) {
            mCurInkLevel = 0;
        }
        Debug.d(TAG, "Ink[" + mIndex + "] down to [" + mCurInkLevel + "]");

        mInkLevelModified = true;
    }

    public void writeInkLevel() {
        if(mValid && mInkLevelModified && null != mRFIDModule) {
            Debug.d(TAG, "Write ink[" + mIndex + "](" + mCurInkLevel +")");
            if(!mRFIDModule.writeInkLevel(mCurInkLevel)) {
                mWriteRetryCount++;
                if(mWriteRetryCount > 10) mValid = false;
            } else {
                mWriteRetryCount = 0;
                mInkLevelModified = false;
            }
        }
    }

    public boolean checkCardAbsence() {
        if(mValid && null != mRFIDModule) {
            mValid = mRFIDModule.searchCard();
        }
        return mValid;
    }

    public int checkUID() {
        if(mValid && null != mRFIDModule) {
            byte[] uid = mRFIDModule.readUID();
            if(null == uid) return 0;
            if(!Arrays.equals(uid, mRFIDModule.getUID())) {
                return -1;
            }
        }
        Debug.d(TAG, "Check UID[" + mIndex + "] OK.");
        return 1;
    }

    public int getFeature(int index) {
        if(mFeature == null || index >= mFeature.length) {
            return 0;
        } else {
            return mFeature[index-1];
        }
    }

    public boolean checkFeatureCode() {
        if (mFeature== null || mFeature.length < 2) {
            return false;
        }
        Debug.d(TAG, "--->FeatureCode: " + mFeature[0] + ", " +mFeature[1] + "; FEATURE_HIGH: " + FEATURE_HIGH + ", FEATURE_LOW: " + FEATURE_LOW);
        if ((mFeature[0] ^ (byte)FEATURE_HIGH) == 0x00 && (mFeature[1] ^ (byte)FEATURE_LOW) == 0x00) {
            return true;
        }
        return false;
    }

    public boolean isValid() {
        return mValid;
    }

    public boolean inkModified() {
        return mInkLevelModified;
    }
}