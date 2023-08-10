package com.industry.printer.Socket_Server;

import android.text.TextUtils;

public class PCCommand {


    /** 打印 */
    public static final String CMD_PRINT = "100";
    public static final String CMD_PRINT_S = "Print";

    /** 清洗 */
    public static final String CMD_CLEAN = "200";
    public static final String CMD_CLEAN_S = "Purge";

// H.M.Wang 2023-8-8 增加一个新的网络命令，SelectPen。在content字段的后六位设置被选头的组，格式为六位二进制数，1代表选中，0代表未选中。其他值报错。
    public static final String CMD_SEL_PEN = "SelectPen";
// End of H.M.Wang 2023-8-8 增加一个新的网络命令，SelectPen

    /** 发送文件 */
    public static final String CMD_SEND_FILE = "300";
    public static final String CMD_SEND_FILE_S = "SendFile";

    /** 取计数器 */
    public static final String CMD_READ_COUNTER = "400";
    public static final String CMD_READ_COUNTER_S = "Inquery";

    /** 停止打印 */
    public static final String CMD_STOP_PRINT = "500";
    public static final String CMD_STOP_PRINT_S = "Stop";

    /** 设置计数器 */
    public static final String CMD_SET_REMOTE = "600";
    public static final String CMD_SET_REMOTE_S = "Dynamic";

// H.M.Wang 2022-6-1 增加新的文本设置指令，该文本设置格式与600命令一致，为10个文本+1个条码，但是文本的排列顺序不代表填充到消息里面DT变量的顺序，而是10个全局DT桶的顺序
    public static final String CMD_SET_REMOTE1 = "650";
    public static final String CMD_SET_REMOTE1_S = "Dynamic1";
// End of H.M.Wang 2022-6-1 增加新的文本设置指令，该文本设置格式与600命令一致，为10个文本+1个条码，但是文本的排列顺序不代表填充到消息里面DT变量的顺序，而是10个全局DT桶的顺序

    /** 生成tlk */
    public static final String CMD_MAKE_TLK = "700";
    public static final String CMD_MAKE_TLK_S = "MakeTLK";

    /** 删除文件 */
    public static final String CMD_DEL_FILE = "800";
    public static final String CMD_DEL_FILE_S = "DelFile";

    /** 删除文件 */
    public static final String CMD_DEL_DIR = "900";
    public static final String CMD_DEL_DIR_S = "DelDir";

    /** PC 发送bin文件 */
    public static final String CMD_SEND_BIN = "1000";
    public static final String CMD_SEND_BIN_S = "SendBin";

    /** 删除指定index的bin */
    public static final String CMD_DEL_LAN_BIN = "1100";
    public static final String CMD_DEL_LAN_BIN_S = "DelBin";

    /** 打印归零 */
    public static final String CMD_RESET_INDEX = "1200";
    public static final String CMD_RESET_INDEX_S = "Reset";

    // H.M.Wang 2019-12-25 追加速度和清洗命令
    /** 设置速度 */
    public static final String CMD_SET_DOTSIZE = "dotsize";

    /** 打印归零 */
    public static final String CMD_SET_CLEAN = "Clean";
    // End of H.M.Wang 2019-12-25 追加速度和清洗命令

// H.M.Wang 2020-7-1 追加一个计数器设置数值命令
    public static final String CMD_SET_COUNTER = "Counter";
// End of H.M.Wang 2020-7-1 追加一个计数器设置数值命令

    public static final String CMD_SET_TIME = "Time";

// H.M.Wang 2020-7-28 追加一个设置参数命令
    public static final String CMD_SET_PARAMS = "Settings";
// End of H.M.Wang 2020-7-28 追加一个设置参数命令

// H.M.Wang 2020-9-28 追加一个心跳协议
    public static final String CMD_HEARTBEAT = "Heartbeat";
// End of H.M.Wang 2020-9-28 追加一个心跳协议

// H.M.Wang 2021-2-4 追加软启动打印命令
    public static final String CMD_SOFT_PHO = "SoftPho";
// End of H.M.Wang 2021-2-4 追加软启动打印命令

// H.M.Wang 2023-3-13 追加一个清除PCFIFO的网络命令
    public static final String CMD_CLEAR_FIFO = "ClearFIFO";
// End of H.M.Wang 2023-3-13 追加一个清除PCFIFO的网络命令

    /** 包头 */
    public String header;

    /** 设备号 Device number */
    public String deviceId;

    /** 命令字 Command number  */
    public String command;


    /** 字符串 content */
    public String content;

    /** 数据包大小 */
    public String size;

    /** 结束标志 */
    public String note;

    /**Note 1 */
    public String note2;

    /** 校验位 */
    public String check;

    /** 包尾 */
    public String end;

    public static PCCommand fromString(String message) {
        if (TextUtils.isEmpty(message) ) {
            return  null;
        }

        PCCommand cmd = new PCCommand();
        String[] f = message.split("\\|");
        if (f.length > 0) {
            cmd.header = f[0];
        }
        if (f.length > 1) {
            cmd.deviceId = f[1];
        }

        if (f.length > 2) {
            cmd.command = f[2];
        }
        if (f.length > 3) {
            cmd.content = f[3];
        }
        if (f.length > 4) {
            cmd.size = f[4];
        }

        if (f.length > 5) {
            cmd.note = f[5];
        }

        if (f.length > 6) {
            cmd.note2 = f[6];
        }

        if (f.length > 7) {
            cmd.check = f[7];
        }


        if (f.length > 8) {
            cmd.end = f[8];
        }
        return cmd;
    }
}
