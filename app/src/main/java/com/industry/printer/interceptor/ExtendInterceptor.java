package com.industry.printer.interceptor;

import android.content.Context;

import com.industry.printer.FileFormat.SystemConfigFile;
import com.industry.printer.PHeader.PrinterNozzle;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.PlatformInfo;


/**
 *
 */
public class ExtendInterceptor {
	
	private final static String TAG = ExtendInterceptor.class.getSimpleName();

// H.M.Wang 2020-2-28 取消该定义，与SystemConfigFile.INDEX_ONE_MULTIPLE重复，这个定义才是本源
//    private static final int EXTEND_PARAM = 37;
// End of H.M.Wang 2020-2-28 取消该定义，与SystemConfigFile.INDEX_ONE_MULTIPLE重复，这个定义才是本源
    private Context mContext;

    public ExtendInterceptor(Context ctx) {
        mContext = ctx;
    }


    public ExtendStat getExtend() {
        SystemConfigFile config = SystemConfigFile.getInstance(mContext);
// H.M.Wang 2020-2-28 取消该定义，与SystemConfigFile.INDEX_ONE_MULTIPLE重复，这个定义才是本源
//        int extend = config.getParam(EXTEND_PARAM);
        int extend = config.getParam(SystemConfigFile.INDEX_ONE_MULTIPLE);
// End of H.M.Wang 2020-2-28 取消该定义，与SystemConfigFile.INDEX_ONE_MULTIPLE重复，这个定义才是本源

        int base = extend/10;
        int ext = extend%10;
///./...        Debug.d(TAG, "--->getExtend  base = " + base + "  ext = " + ext);
        if (base < 1 || ext <= 1) {
            // no extension
            return ExtendStat.NONE;
        }
        if (base == 1) {
            switch (ext) {
                case 1:
                    return ExtendStat.NONE;
                case 2:
                    return ExtendStat.EXT_1_TO_2;
                case 3:
                    return ExtendStat.EXT_1_TO_3;
                case 4:
                    return ExtendStat.EXT_1_TO_4;
                case 5:
                    return ExtendStat.EXT_1_TO_5;
                case 6:
                    return ExtendStat.EXT_1_TO_6;
                case 7:
                    return ExtendStat.EXT_1_TO_7;
                case 8:
                    return ExtendStat.EXT_1_TO_8;
                default:
                    return ExtendStat.NONE;
            }
        } else if (base == 2) {
            switch (ext) {

                case 4:
                    return ExtendStat.EXT_2_TO_4;
                case 6:
                    return ExtendStat.EXT_2_TO_6;
                case 8:
                    return ExtendStat.EXT_2_TO_8;
                default:
                    return ExtendStat.NONE;
            }
        } else {
            return ExtendStat.NONE;
        }
    }


    public enum ExtendStat {

        NONE(0,0),
        EXT_1_TO_2(1, 2),
        EXT_1_TO_3(1, 3),
        EXT_1_TO_4(1, 4),
        EXT_1_TO_5(1, 5),
        EXT_1_TO_6(1, 6),
        EXT_1_TO_7(1, 7),
        EXT_1_TO_8(1, 8),
        EXT_2_TO_4(2, 4),
        EXT_2_TO_6(2, 6),
        EXT_2_TO_8(2, 8);

        private int source;
        private int target;

        private ExtendStat(int source, int target) {
            this.source = source;
            this.target = target;
        }

        public int getScale() {
            if (this.equals(NONE)) {
                return 1;
            } else {
// H.M.Wang 2025-2-27 增加一带二的判断。当22MM的时候，只有C31=HP22MM时才允许一带二，否则不允许
                if(PlatformInfo.getImgUniqueCode().startsWith("22MM")) {
                    if(SystemConfigFile.getInstance().getPNozzle() == PrinterNozzle.MESSAGE_TYPE_22MM && SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_ONE_MULTIPLE) == 12)
                        return target/source;  // 仅支持22MM时，打印头类型为MESSAGE_TYPE_22MM时的一带二，其它打印头类型或者其它的n带m都不支持
                    else
                        return 1;
                }
// End H.M.Wang 2025-2-27 增加一带二的判断。当22MM的时候，只有C31=HP22MM时才允许一带二，否则不允许
//            	Debug.d("ExtendStat", "--->target: " + target +  "   source: " + source);
                return target/source;
            }
        }

        /**
         * real print headers after extension
         * @return
         */
        public int activeNozzleCount() {
            return target;
        }
    }
}
