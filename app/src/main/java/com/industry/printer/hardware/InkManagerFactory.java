package com.industry.printer.hardware;

import android.content.Context;

import com.industry.printer.FileFormat.SystemConfigFile;
import com.industry.printer.Rfid.N_RFIDModuleChecker;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.PlatformInfo;

/**
 * factory for ink device manager: RFID, SmartCard
 */
public class InkManagerFactory {

    private static volatile IInkDevice manager = null;
    private static final String TAG = InkManagerFactory.class.getSimpleName();

    public static IInkDevice inkManager(Context ctx) {

        if (manager == null) {
            synchronized (InkManagerFactory.class) {
                if (manager == null) {
                    manager = getManager(ctx);
                }
            }
        }
        return manager;
    }

    public static IInkDevice reInstance(Context ctx) {
        manager = null;
        return inkManager(ctx);
    }

    private static IInkDevice getManager(Context ctx) {
        String inkDev = PlatformInfo.getInkDevice();
        Debug.d(TAG, "--->Platform: " + inkDev);
        if (PlatformInfo.DEVICE_SMARTCARD.equals(inkDev)) {
              return new SmartCardManager(ctx);
//            return new RFIDManager(ctx);
        } else {
// H.M.Wang 2022-11-5 追加一个根据hp22mm的img返回Manager的判断
            if(PlatformInfo.getImgUniqueCode().startsWith("22MM")) {
// H.M.Wang 2024-3-19 切换到新的Hp22mmSCManager，停止使用临时的SmartCardManager
//                return new SmartCardManager(ctx);       // 测试HP22MM，目的是避免访问/dev/ttyS3
                return new Hp22mmSCManager(ctx);       // 测试HP22MM，目的是避免访问/dev/ttyS3
// End of H.M.Wang 2024-3-19 切换到新的Hp22mmSCManager，停止使用临时的SmartCardManager
// End of H.M.Wang 2022-11-5 追加一个根据hp22mm的img返回Manager的判断
            }

// H.M.Wang 2025-3-18 临时增加一个通过参数切换RFID和SmartCard的功能
            if(SystemConfigFile.getInstance(ctx).getParam(SystemConfigFile.INDEX_RFID_SC_SWITCH) == SystemConfigFile.PROC_TYPE_RFID) {
                return new RFIDManager(ctx);
            } else if(SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_RFID_SC_SWITCH) == SystemConfigFile.PROC_TYPE_SC) {
                SmartCard.exist((PlatformInfo.isMImgType(PlatformInfo.getImgUniqueCode()) ? 1 : 0), (PlatformInfo.isA133Product() ? 2 : 1));
                return new SmartCardManager(ctx);
            }
// End of H.M.Wang 2025-3-18 临时增加一个通过参数切换RFID和SmartCard的功能

// H.M.Wang 2022-4-12 追加try，以避免旧so里面没有这个函数导致死机
            try {
// H.M.Wang 2022-1-20 根据SmartCard是否连接来判断走SC还是RFID
                if(SmartCard.exist((PlatformInfo.isMImgType(PlatformInfo.getImgUniqueCode()) ? 1 : 0), (PlatformInfo.isA133Product() ? 2 : 1)) == SmartCard.SC_SUCCESS) {
                    return new SmartCardManager(ctx);
                }
// End of H.M.Wang 2022-1-20 根据SmartCard是否连接来判断走SC还是RFID
// H.M.Wang 2022-5-9 Exception e修改为UnsatisfiedLinkError e，并且打印log输出，否则catch不到
            } catch(UnsatisfiedLinkError e) {
                Debug.e(TAG, "Error: " + e.getMessage());
            }
// End of H.M.Wang 2022-5-9 Exception e修改为UnsatisfiedLinkError e，并且打印log输出，否则catch不到
// End of H.M.Wang 2022-4-12 追加try，以避免旧so里面没有这个函数导致死机

// H.M.Wang 2023-10-18 取消2023-5-17的修改，恢复到根据img的版本号，为NNM2的时候启用N_RFIDManager，其他的启用RFIDManager
// H.M.Wang 2023-5-17 RFIDDevice.FEATURE_HIGH >= 102时，启动N_RFIDManager，否则启动RFIDManager
// H.M.Wang 2023-11-15 取消2023-10-18和2023-5-17的修改，改为在这里通过自动判断来选择，如果是复旦卡就走原路径，如果是23(1207)卡就走新路径
            N_RFIDModuleChecker checker = new N_RFIDModuleChecker();
            if(N_RFIDModuleChecker.RFID_MOD_M104BPCS_KX1207 == checker.check(PlatformInfo.getRfidDevice())) {
                return new N_RFIDManager(ctx);
            } else {
                return new RFIDManager(ctx);
            }
/* 2023-10-18           String imgCode = PlatformInfo.getImgUniqueCode();
            if(imgCode.startsWith("NSM2") || imgCode.startsWith("NGM2") || imgCode.startsWith("OS07") || imgCode.startsWith("OG07")) {
                return new N_RFIDManager(ctx);
            } else {
                return new RFIDManager(ctx);
            }
*/
/* 2023-5-17           if(RFIDDevice.FEATURE_HIGH >= 102) {
                return new N_RFIDManager(ctx);
            } else {
                return new RFIDManager(ctx);
            }*/
// End of H.M.Wang 2023-11-15 取消2023-10-18和2023-5-17的修改，改为在这里通过自动判断来选择，如果是复旦卡就走原路径，如果是23(1207)卡就走新路径
// End of H.M.Wang 2023-5-17 FEATURE_HIGH >= 102时，启动N_RFIDManager，否则启动RFIDManager
// End of H.M.Wang 2023-10-18 取消2023-5-17的修改，恢复到根据img的版本号，为NNM2的时候启用N_RFIDManager，其他的启用RFIDManager
        }
    }
}
