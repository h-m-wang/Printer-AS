package com.industry.printer.Constants;

public class Constants {

    /************************ PC ********************************/
    // communication between PC and Android
    public static final String PC_CMD = "pcCommand";

    // result ok
    public static final String PC_RESULT_OK = "0000-ok: ";

    // result error
    public static final String PC_RESULT_ERR = "1111-error: ";


    public static String pcOk(String command) {
        return PC_RESULT_OK + command;
    }

    public static String pcErr(String command) {
        return PC_RESULT_ERR + command;
    }

    /************************ PC ********************************/


    public static final int LOG_ENABLE = 1234;
// H.M.Wang 2025-7-10 增加一个参数60的值，当=5678时，输出log。当=0时，不输出log
    public static final int LOG_OUTPUT_ENABLE = 5678;
// End of H.M.Wang 2025-7-10 增加一个参数60的值，当=5678时，输出log。当=0时，不输出log
}
