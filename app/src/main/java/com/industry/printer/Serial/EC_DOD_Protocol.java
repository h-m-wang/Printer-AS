package com.industry.printer.Serial;

import com.industry.printer.Utils.ByteArrayUtils;
import com.industry.printer.Utils.Debug;

import org.apache.http.util.ByteArrayBuffer;

/**
 * Created by hmwan on 2019/10/26.
 */

public class EC_DOD_Protocol {
    public static String TAG = EC_DOD_Protocol.class.getSimpleName();

    private final byte TAG_STX = 0x7E;
//    private final byte TAG_STX = '[';     // 0x5B Just for test

    private final byte TAG_ETX = 0x7F;
//    private final byte TAG_ETX = ']';     // 0x5D Just for test

/*
    发送方帧格式：
            -------------------------------------------------------------
            [STX] 				帧起始标志0x7e	1字节	    固定位置
            [A] 				地址			    2字节 	    固定位置
            [CMDID] 			命令    		    2字节	    固定位置
            <DATAINFO>     	    数据信息  		不定长       不固定位置，
            [CHKSUM]   		    帧校验   		2字节	    不固定位置,  [ETX]前一位
            [ETX]           	帧终止标志0x7f    1字节	    不固定位置
  			-------------------------------------------------------------
*/
/*
    喷码机返回帧格式：
            -------------------------------------------------------------
            [STX] 				帧起始标志0x7e	1字节	    固定位置
            [A] 				地址			    2字节	    固定位置
            [CMDID] 			命令    		    2字节	    固定位置
            [ACK/NAK]    		命令回应		    1字节	    固定位置
            [DEVSTATUS]		    设备状态		    1字节	    固定位置
            [CMDSTATUS]		    命令状态		    1字节	    固定位置
            <DATAINFO>     	    数据信息  		不定长       不固定位置
            [CHKSUM]   		    帧校验   		2字节	    不固定位置,  [ETX]前一位
            [ETX]           	帧终止标志0x7f   1字节	    不固定位置
  			-------------------------------------------------------------
*/
    private final int TAG_STX_POS               = 0;
//    private final int TAG_ADDRESS_POS           = 1;
    private final int TAG_CMDID_POS             = 3;
    private final int TAG_DATA_POS              = 5;
    private final int TAG_CHECKSUM_POS          = -3;
    private final int TAG_ETX_POS               = -1;

//    private final int TAG_ACK_NAK_POS           = 5;
//    private final int TAG_DEVSTATUS_POS         = 6;
//    private final int TAG_CMDSTATUS_POS         = 7;
//    private final int TAG_DATA_OUT_POS          = 8;

    public final static int CMD_CHECK_CHANNEL         = 0x0001;       // 检查信道	0x0001
    public final static int CMD_SET_NOZZLE_HEIGHT     = 0x0002;       // 设定喷头喷印高度	0x0002
    public final static int CMD_GET_NOZZLE_HEIGHT     = 0x0003;       // 读取喷头喷印高度	0x0003
    public final static int CMD_SET_DOT_INTERVAL      = 0x0004;       // 设定喷头喷印点距	0x0004
    public final static int CMD_GET_DOT_INTERVAL      = 0x0005;       // 读取喷头喷印点距	0x0005
    public final static int CMD_SET_DROP_SIZE         = 0x0006;       // 设定喷头喷印墨滴大小	0x0006
    public final static int CMD_GET_DROP_SIZE         = 0x0007;       // 读取喷头墨滴大小	0x0007
    public final static int CMD_SET_PRINT_DELAY       = 0x0008;       // 设定喷头喷印延时	0x0008
    public final static int CMD_GET_PRINT_DELAY       = 0x0009;       // 读取喷头喷印延时	0x0009
    public final static int CMD_SET_MOVE_SPEED        = 0x000a;       // 设定物体移动速度	0x000a
//    public final static int CMD_SET_MOVE_SPEED        = 0x6130;       // 设定物体移动速度	0x000a
    public final static int CMD_GET_MOVE_SPEED        = 0x000b;       // 读取物体移动速度	0x000b
    public final static int CMD_SET_EYE_STATUS        = 0x000c;       // 设定喷头电眼状态	0x000c
    public final static int CMD_GET_EYE_STATUS        = 0x000d;       // 读取喷头电眼状态	0x000d
    public final static int CMD_SET_SYNC_STATUS       = 0x000e;       // 设定喷头同步器状态	0x000e
    public final static int CMD_GET_SYNC_STATUS       = 0x000f;       // 读取喷头同步器状态	0x000f
    public final static int CMD_SET_REVERSE           = 0x0010;       // 设定喷头翻转喷印	0x0010
//    public final static int CMD_SET_REVERSE           = 0x3031;       // 设定喷头翻转喷印	0x0010
    public final static int CMD_GET_REVERSE           = 0x0011;       // 读取喷头翻转喷印	0x0011
    public final static int CMD_GET_USABLE_IDS        = 0x0012;       // 获取当前文件中可用的ID	0x0012
    public final static int CMD_TEXT                  = 0x0013;       // 发送一条文本	0x0013
//    public final static int CMD_TEXT                  = 0x3331;       // 发送一条文本	0x0013
    public final static int CMD_SAVE_CURRENT_INFO     = 0x0014;       // 保存当前信息	0x0014
    public final static int CMD_START_PRINT           = 0x0015;       // 启动喷码机开始喷印	0x0015
//    public final static int CMD_START_PRINT           = 0x3531;       // 启动喷码机开始喷印	0x0015
    public final static int CMD_STOP_PRINT            = 0x0016;       // 停机命令	0x0016
//    public final static int CMD_STOP_PRINT            = 0x3631;       // 停机命令	0x0016
    public final static int CMD_START_PRINT_A         = 0x0017;       // A喷头喷印	0x0017
    public final static int CMD_START_PRINT_B         = 0x0018;       // B喷头喷印	0x0018
    public final static int CMD_STOP_PRINT_A          = 0x0019;       // A喷印完成	0x0019
    public final static int CMD_STOP_PRINT_B          = 0x0020;       // B喷印完成	0x0020

    public final static int ERROR_SUCESS              = 0x00000000;   // 无错误
    public final static int ERROR_INVALID_STX         = 0x81000000;   // 起始标识错误
    public final static int ERROR_INVALID_ETX         = 0x82000000;   // 终止标识错误
    public final static int ERROR_UNKNOWN_CMD         = 0x83000000;   // 不可识别的命令
    public final static int ERROR_CRC_FAILED          = 0x84000000;   // CRC校验失败
    public final static int ERROR_FAILED              = 0x85000000;   // 解析帧失败

    public EC_DOD_Protocol(){}

    public int parseFrame(ByteArrayBuffer recvMsg) {
        int recvCmd = 0;

        try {
            byte[] msg = recvMsg.toByteArray();
//            Debug.d(TAG, "[" + ByteArrayUtils.toHexString(msg) + "]");

            msg = cutCRLFonLineEnd(msg);
//            Debug.d(TAG, "[" + ByteArrayUtils.toHexString(msg) + "]");

            if(msg[TAG_STX_POS] != TAG_STX) {
                return ERROR_INVALID_STX;
            }

            if(msg[msg.length + TAG_ETX_POS] != TAG_ETX) {
                return ERROR_INVALID_ETX;
            }

            if(msg.length < 4) {
                return ERROR_UNKNOWN_CMD;
            }

            short recvCRC = (short)((0x00ff & msg[msg.length + TAG_CHECKSUM_POS + 1]) * 0x0100 + ((0x00ff) & msg[msg.length + TAG_CHECKSUM_POS]));
//            Debug.d(TAG, "recvCRC = " + Integer.toHexString((0x0000ffff & recvCRC)));
            byte[] procData = ByteArrayUtils.pickPartial(msg, 1, msg.length - 4);
//            Debug.d(TAG, "[" + ByteArrayUtils.toHexString(procData) + "]");

//            if(CRC16_X25.getCRCCode(procData) != recvCRC) {
//                recvCmd |= ERROR_CRC_FAILED;
//                // return ERROR_CRC_FAILED;
//            }

            msg = replaceTransformBytes(msg, true);
//            Debug.d(TAG, "[" + ByteArrayUtils.toHexString(msg) + "]");

            recvCmd |= ((0x000000ff & msg[TAG_CMDID_POS + 1]) * 0x0100 + ((0x000000ff) & msg[TAG_CMDID_POS]));
            recvMsg.clear();
            recvMsg.append(msg, TAG_DATA_POS, msg.length + TAG_CHECKSUM_POS - TAG_DATA_POS);

            Debug.d(TAG, "CMD = " + Integer.toHexString((0x0000ffff & recvCmd)) + "; DATA = [" + ByteArrayUtils.toHexString(recvMsg.toByteArray()) + "]");
            return recvCmd;
        } catch(Exception e) {
            Debug.e(TAG, e.getMessage());
        }
        return ERROR_FAILED;
    }

    public byte[] createFrame(int cmd, int ack, int devStatus, int cmdStatus, byte[] msg) {
//        Debug.d(TAG, "[" + ByteArrayUtils.toHexString(msg) + "]");

        ByteArrayBuffer sendBuffer = new ByteArrayBuffer(0);

        // 添加地址
        sendBuffer.append(0x00);
        sendBuffer.append(0x00);

        // 添加命令
        sendBuffer.append(0x000000ff & cmd);
        sendBuffer.append(0x000000ff & (cmd >> 8));

        // 添加ACK/NAK
        sendBuffer.append(ack);

        // 添加设备状态
        sendBuffer.append(devStatus);

        // 添加命令状态
        sendBuffer.append(cmdStatus);

        // 添加回送字串
        sendBuffer.append(msg, 0, msg.length);

        msg = replaceTransformBytes(sendBuffer.toByteArray(), false);

//        Debug.d(TAG, "[" + ByteArrayUtils.toHexString(msg) + "]");

        sendBuffer.clear();

        short crc = CRC16_X25.getCRCCode(msg);

        sendBuffer.append(TAG_STX);
        sendBuffer.append(msg, 0, msg.length);
        sendBuffer.append(crc);
        sendBuffer.append(crc >> 8);
        sendBuffer.append(TAG_ETX);

        Debug.d(TAG, "Created Response: [" + ByteArrayUtils.toHexString(sendBuffer.toByteArray()) + "]");
        return sendBuffer.toByteArray();
    }

    private byte[] cutCRLFonLineEnd(byte[] msg) {
        int length = msg.length;

        while( msg[length-1] == 0x0a || msg[length-1] == 0x0d) {
            length--;
        }

        if(length == msg.length) {
            return msg;
        }

        return ByteArrayUtils.pickPartial(msg, 0, length);
    }

    private byte[] replaceTransformBytes(byte[] msg, boolean isRecvData) {
        ByteArrayBuffer replace = new ByteArrayBuffer(0);

        for(int i=0; i<msg.length; i++) {
            if(isRecvData) {
                if(msg[i] == 0x7d) {
                    replace.append(msg[++i] + 0x20);
                } else {
                    replace.append(msg[i]);
                }
            } else {
                if(msg[i] == 0x7d) {
                    replace.append(0x7d);
                    replace.append(0x5d);
                } else if(msg[i] == 0x7e) {
                    replace.append(0x7d);
                    replace.append(0x5e);
                } else if(msg[i] == 0x7f) {
                    replace.append(0x7d);
                    replace.append(0x5f);
                } else {
                    replace.append(msg[i]);
                }
            }
        }

        return replace.toByteArray();
    }
}