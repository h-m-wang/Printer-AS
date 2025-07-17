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
// H.M.Wang 2023-5-17 RFIDDevice.FEATURE_HIGH >= 101时，启动N_RFIDManager，否则启动RFIDManager。暂时取消对本类当中FEATURE的参照
//    public static final int FEATURE_HIGH = 100;
//    public static final int FEATURE_LOW = 1;
// End of H.M.Wang 2023-5-17 RFIDDevice.FEATURE_HIGH >= 101时，启动N_RFIDManager，否则启动RFIDManager。暂时取消对本类当中FEATURE的参照

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

// H.M.Wang 2023-5-17 暂时取消自动判断模块种类，固定为1207。待以后对这里的RFID_MOD_M104DPCS和RFID_MOD_M104BPCS模块动作完成商业确认后，再恢复
/*
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
*/
        mRFIDModule = new N_RFIDModule_M104BPCS_KX1207();
// End of H.M.Wang 2023-5-17 暂时取消自动判断模块种类，固定为1207。待以后对这里的RFID_MOD_M104DPCS和RFID_MOD_M104BPCS模块动作完成商业确认后，再恢复

        ret = mRFIDModule.open(PlatformInfo.getRfidDevice());
        if(!ret) return false;

        ret = mRFIDModule.initCard();
        if(!ret) return false;

// H.M.Wang 2023-12-18 向P57(H39)写入密钥的0xAA异或值，如果写入成功，则说明卡已经写入了正确的密钥，否则则没有写入正确密钥，不允许继续使用。该功能在纸卡工具中不适用
        if(mRFIDModule instanceof N_RFIDModule_M104BPCS_KX1207) {
            if(!((N_RFIDModule_M104BPCS_KX1207) mRFIDModule).isKeyExist()) return false;
            if(!((N_RFIDModule_M104BPCS_KX1207) mRFIDModule).tryWrite()) return false;
        }
// End of H.M.Wang 2023-12-18 向P57(H39)写入密钥的0xAA异或值，如果写入成功，则说明卡已经写入了正确的密钥，否则则没有写入正确密钥，不允许继续使用。该功能在纸卡工具中不适用

        mInkMax = mRFIDModule.readMaxInkLevel();
        mCurInkLevel = mRFIDModule.readInkLevel();
        mFeature = mRFIDModule.readFeature();

        mValid = checkFeatureCode();

// H.M.Wang 2025-7-17 永久取消64-164的区间限制
//// H.M.Wang 2023-5-18 追加一个，当为Bagink的时候，如果特征值6的值不是64-164之间的值，则禁止打印
//        if(PlatformInfo.getImgUniqueCode().startsWith("BAGINK") && (mFeature[6] < 64 || mFeature[6] >= 164)) {
//            mValid = false;
//        }
//// End of H.M.Wang 2023-5-18 追加一个，当为Bagink的时候，如果特征值6的值不是64-164之间的值，则禁止打印
// End of H.M.Wang 2025-7-17 永久取消64-164的区间限制

        return mValid;
    }

    public float getLocalInk() {
        return mCurInkLevel;
    }

    public int getMax() {
        return mInkMax;
    }

// H.M.Wang 2023-12-3 修改锁值记录方法。增加一个mStep的传递方法
    public float getMaxRatio() {
        return mRFIDModule.getMaxRatio();
    }
// End of H.M.Wang 2023-12-3 修改锁值记录方法。增加一个mStep的传递方法

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
                if(mWriteRetryCount >= 10) mValid = false;
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

    public static final int CHECK_UID_FAILED = 0;
    public static final int CHECK_UID_CHANGED = -1;
    public static final int CHECK_UID_SUCCESS = 1;

    public int checkUID() {
        if(mValid && null != mRFIDModule) {
            byte[] uid = mRFIDModule.readUID();
            if(null == uid) return CHECK_UID_FAILED;
            if(!Arrays.equals(uid, mRFIDModule.getUID())) {
                return CHECK_UID_CHANGED;
            }
            Debug.d(TAG, "Check UID[" + mIndex + "] OK.");
            return CHECK_UID_SUCCESS;
        }
        return CHECK_UID_FAILED;
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
// H.M.Wang 2023-5-17 RFIDDevice.FEATURE_HIGH >= 101时，启动N_RFIDManager，否则启动RFIDManager。因此这里参照RFIDDevice的FEATURE，暂时取消本类中的FEATURE的参照
        Debug.d(TAG, "--->FeatureCode: " + mFeature[0] + ", " +mFeature[1] + "; FEATURE_HIGH: " + RFIDDevice.FEATURE_HIGH + ", FEATURE_LOW: " + RFIDDevice.FEATURE_LOW);
        if ((mFeature[0] ^ (byte)RFIDDevice.FEATURE_HIGH) == 0x00 && (mFeature[1] ^ (byte)RFIDDevice.FEATURE_LOW) == 0x00) {
// End of H.M.Wang 2023-5-17 RFIDDevice.FEATURE_HIGH >= 101时，启动N_RFIDManager，否则启动RFIDManager。因此这里参照RFIDDevice的FEATURE，暂时取消本类中的FEATURE的参照
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
