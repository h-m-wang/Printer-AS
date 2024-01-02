package com.industry.printer.Serial;

import com.industry.printer.Utils.Debug;

/**
 * Created by hmwan on 2021/3/7.
 */

public class CRC16_MODBUS {
    private static String TAG = CRC16_MODBUS.class.getSimpleName();

    // Step1. 多项式 = x^16 + x^15 + x^2 + 1 = 1000 0000 0000 0101 (去掉x^16)
    // Step2. 将高低位置换 1000 0000 0000 0101(0x8005); 颠倒过来就是 1010 0000 0000 0001 = 0xA001
    // 即得到CRC16_BASE 的值
    private static int CRC16_BASE = 0x0000A001;

    public static short getCRCCode(byte[] data) {
        if (null == data || data.length <= 0) {
            return 0;
        }

        // CRC寄存器初始值置为0xFFFF（即全为1）
        int crcCode = 0x0000FFFF;

        // 对于待处理字符串的每个字节
        for(int i = 0; i < data.length; i++) {
            // 当前字节与CRC寄存器低位字节异或，结果保存在CRC寄存器
            crcCode ^= ((int) data[i] & 0x000000FF);
            // 对于CRC寄存器的值，根据低位字节的各位的值进行下列处理
            for(int j = 0; j < 8; j++) {
                if ((crcCode & 0x00000001) != 0) {
                    // 如果最低位为1，则CRC寄存器值右移一位后与CRC_BASE进行异或
                    crcCode >>= 1;
                    crcCode ^= CRC16_BASE;
                } else {
                    // 如果最低位为0，则CRC寄存器值右移一位
                    crcCode >>= 1;
                }
            }
        }

        // 结果与0x0000异或
        crcCode ^= 0x00000000;

        Debug.d(TAG, "crcCode = " + Integer.toHexString(crcCode & 0x0000FFFF));
        return (short)(crcCode & 0x0000FFFF);
    }
}
