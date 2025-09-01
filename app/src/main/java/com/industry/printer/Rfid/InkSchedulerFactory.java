package com.industry.printer.Rfid;

import android.content.Context;

import com.industry.printer.FileFormat.SystemConfigFile;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.PlatformInfo;
import com.industry.printer.hardware.InkManagerFactory;
import com.industry.printer.hardware.N_RFIDManager;
import com.industry.printer.hardware.RFIDDevice;
import com.industry.printer.hardware.RFIDManager;
import com.industry.printer.hardware.SmartCard;
import com.industry.printer.hardware.SmartCardManager;

public class InkSchedulerFactory {
    private static final String TAG = InkSchedulerFactory.class.getSimpleName();

    private static volatile IInkScheduler scheduler = null;

    public static IInkScheduler getScheduler(Context ctx) {
        if (scheduler == null) {
            synchronized (InkManagerFactory.class) {
                if (scheduler == null) {
                    scheduler = create(ctx);
                }
            }
        }
        return scheduler;
    }

    private static IInkScheduler create(Context ctx) {
        String inkDev = PlatformInfo.getInkDevice();

        if (PlatformInfo.DEVICE_SMARTCARD.equals(inkDev)) {
            return new SmardCardScheduler(ctx);
//            return new RfidScheduler(ctx);
        } else {
// H.M.Wang 2022-11-5 追加一个根据hp22mm的img返回Manager的判断
            if(PlatformInfo.getImgUniqueCode().startsWith("22MM")) {
// H.M.Wang 2024-3-19 切换到新的Hp22mmSCScheduler，停止使用临时的SmardCardScheduler
//                return new SmardCardScheduler(ctx);       // 测试HP22MM，目的是避免访问/dev/ttyS3
                return new Hp22mmSCScheduler(ctx);       // 测试HP22MM，目的是避免访问/dev/ttyS3
// End of H.M.Wang 2024-3-19 切换到新的Hp22mmSCScheduler，停止使用临时的SmardCardScheduler
// End of H.M.Wang 2022-11-5 追加一个根据hp22mm的img返回Manager的判断
            }
// H.M.Wang 2025-3-18 临时增加一个通过参数切换RFID和SmartCard的功能
            if(SystemConfigFile.getInstance(ctx).getParam(SystemConfigFile.INDEX_RFID_SC_SWITCH) == SystemConfigFile.PROC_TYPE_RFID) {
                return new RfidScheduler(ctx);
            } else if(SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_RFID_SC_SWITCH) == SystemConfigFile.PROC_TYPE_SC) {
                SmartCard.exist((PlatformInfo.isMImgType(PlatformInfo.getImgUniqueCode()) ? 1 : 0), (PlatformInfo.isA133Product() ? 2 : 1));
                return new SmardCardScheduler(ctx);
            }
// End of H.M.Wang 2025-3-18 临时增加一个通过参数切换RFID和SmartCard的功能

// H.M.Wang 2022-4-12 追加try，以避免旧so里面没有这个函数导致死机
            try {
// H.M.Wang 2022-1-23 根据SmartCard是否连接来判断走SC还是RFID
                if(SmartCard.exist((PlatformInfo.isMImgType(PlatformInfo.getImgUniqueCode()) ? 1 : 0), (PlatformInfo.isA133Product() ? 2 : 1)) == SmartCard.SC_SUCCESS) {
                    return new SmardCardScheduler(ctx);
                }
// End of H.M.Wang 2022-1-23 根据SmartCard是否连接来判断走SC还是RFID
// H.M.Wang 2022-5-9 Exception e修改为UnsatisfiedLinkError e，并且打印log输出，否则catch不到
            } catch(UnsatisfiedLinkError e) {
                Debug.e(TAG, "Error: " + e.getMessage());
            }
// End of H.M.Wang 2022-5-9 Exception e修改为UnsatisfiedLinkError e，并且打印log输出，否则catch不到
// End of H.M.Wang 2022-4-12 追加try，以避免旧so里面没有这个函数导致死机

// H.M.Wang 2023-10-18 取消2023-5-17的修改，恢复到根据img的版本号，为NNM2的时候启用N_RfidScheduler，其他的启用RfidScheduler
// H.M.Wang 2023-5-17 RFIDDevice.FEATURE_HIGH >= 101时，启动N_RFIDManager，否则启动RFIDManager
// H.M.Wang 2023-11-15 取消2023-10-18和2023-5-17的修改，改为在这里通过自动判断来选择，如果是复旦卡就走原路径，如果是23(1207)卡就走新路径
            N_RFIDModuleChecker checker = new N_RFIDModuleChecker();
            if(N_RFIDModuleChecker.RFID_MOD_M104BPCS_KX1207 == checker.check(PlatformInfo.getRfidDevice())) {
                return new N_RfidScheduler(ctx);
            } else {
                return new RfidScheduler(ctx);
            }
/* 2023-10-18           String imgCode = PlatformInfo.getImgUniqueCode();
            if(imgCode.startsWith("NSM2") || imgCode.startsWith("NGM2") || imgCode.startsWith("OS07") || imgCode.startsWith("OG07")) {
                 return new N_RfidScheduler(ctx);
            } else {
                return new RfidScheduler(ctx);
            }
*/
/* 2023-5-17           if(RFIDDevice.FEATURE_HIGH >= 102) {
                return new N_RfidScheduler(ctx);
            } else {
                return new RfidScheduler(ctx);
            }*/
// End of H.M.Wang 2023-5-17 FEATURE_HIGH >= 101时，启动N_RFIDManager，否则启动RFIDManager
// End of H.M.Wang 2023-10-18 取消2023-5-17的修改，恢复到根据img的版本号，为NNM2的时候启用N_RfidScheduler，其他的启用RfidScheduler
        }
    }
}
