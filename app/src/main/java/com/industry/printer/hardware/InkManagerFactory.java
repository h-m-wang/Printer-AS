package com.industry.printer.hardware;

import android.content.Context;

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
                return new SmartCardManager(ctx);       // 测试HP22MM，目的是避免访问/dev/ttyS3
// End of H.M.Wang 2022-11-5 追加一个根据hp22mm的img返回Manager的判断
            }
// H.M.Wang 2022-4-12 追加try，以避免旧so里面没有这个函数导致死机
            try {
// H.M.Wang 2022-1-20 根据SmartCard是否连接来判断走SC还是RFID
                if(SmartCard.exist(PlatformInfo.isMImgType() ? 1 : 0) == SmartCard.SC_SUCCESS) {
                    return new SmartCardManager(ctx);
                }
// End of H.M.Wang 2022-1-20 根据SmartCard是否连接来判断走SC还是RFID
// H.M.Wang 2022-5-9 Exception e修改为UnsatisfiedLinkError e，并且打印log输出，否则catch不到
            } catch(UnsatisfiedLinkError e) {
                Debug.e(TAG, "Error: " + e.getMessage());
            }
// End of H.M.Wang 2022-5-9 Exception e修改为UnsatisfiedLinkError e，并且打印log输出，否则catch不到
// End of H.M.Wang 2022-4-12 追加try，以避免旧so里面没有这个函数导致死机
//            if(PlatformInfo.getImgUniqueCode().startsWith("NNM2")) {
// 暂时为了push取消，待所有动作确认后修改回来                return new N_RFIDManager(ctx);
//            }
            return new RFIDManager(ctx);
        }
    }
}
