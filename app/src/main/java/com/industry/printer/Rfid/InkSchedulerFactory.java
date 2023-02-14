package com.industry.printer.Rfid;

import android.content.Context;

import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.PlatformInfo;
import com.industry.printer.hardware.InkManagerFactory;
import com.industry.printer.hardware.N_RFIDManager;
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
                return new SmardCardScheduler(ctx);       // 测试HP22MM，目的是避免访问/dev/ttyS3
// End of H.M.Wang 2022-11-5 追加一个根据hp22mm的img返回Manager的判断
            }
// H.M.Wang 2022-4-12 追加try，以避免旧so里面没有这个函数导致死机
            try {
// H.M.Wang 2022-1-23 根据SmartCard是否连接来判断走SC还是RFID
                if(SmartCard.exist(PlatformInfo.isMImgType() ? 1 : 0) == SmartCard.SC_SUCCESS) {
                    return new SmardCardScheduler(ctx);
                }
// End of H.M.Wang 2022-1-23 根据SmartCard是否连接来判断走SC还是RFID
// H.M.Wang 2022-5-9 Exception e修改为UnsatisfiedLinkError e，并且打印log输出，否则catch不到
            } catch(UnsatisfiedLinkError e) {
                Debug.e(TAG, "Error: " + e.getMessage());
            }
// End of H.M.Wang 2022-5-9 Exception e修改为UnsatisfiedLinkError e，并且打印log输出，否则catch不到
// End of H.M.Wang 2022-4-12 追加try，以避免旧so里面没有这个函数导致死机
//            if(PlatformInfo.getImgUniqueCode().startsWith("NNM2")) {
// 暂时为了push取消，待所有动作确认后修改回来                 return new N_RfidScheduler(ctx);
//            }
            return new N_RfidScheduler(ctx);
//            return new RfidScheduler(ctx);
        }
    }
}
