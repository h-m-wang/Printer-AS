//
// Created by kevin on 2019/2/17.
//

#include <jni.h>

#ifndef _PRINTER_AS_COM_SMARTCARD_H
#define _PRINTER_AS_COM_SMARTCARD_H
#ifdef __cplusplus
extern "C" {
#endif

#define CARD_SELECT_PEN1                        11
#define CARD_SELECT_PEN2                        12
#define CARD_SELECT_BULK1                       13
#define CARD_SELECT_BULKX                       14
#define SELECT_LEVEL1                           21
#define SELECT_LEVEL2                           22

JNIEXPORT jint JNICALL Java_com_Smartcard_shutdown(JNIEnv *env, jclass arg);

JNIEXPORT jint JNICALL Java_com_Smartcard_exist(JNIEnv *env, jclass arg, jint imgtype);

/*
 * 初始化HP智能卡设备和HOST卡
 */
JNIEXPORT jint JNICALL Java_com_Smartcard_init(JNIEnv *env, jclass arg);

// H.M.Wang 2022-11-1 Add this API for Bagink Use
JNIEXPORT jint JNICALL Java_com_Smartcard_init_level_direct(JNIEnv *env, jclass arg );
// End of H.M.Wang 2022-11-1 Add this API for Bagink Use

/*
 * 初始化HP智能卡其他设备，包括COMPONENT卡以及LEVEL
 */
JNIEXPORT jint JNICALL Java_com_Smartcard_init_comp(JNIEnv *env, jclass arg, jint card );

/**
 * 写入验证码
 */
JNIEXPORT jint JNICALL Java_com_Smartcard_writeCheckSum(JNIEnv *env, jclass arg, jint card, jint clientUniqueCode);

/**
 * 验证
 */
JNIEXPORT jint JNICALL Java_com_Smartcard_checkSum(JNIEnv *env, jclass arg, jint card, jint clientUniqueCode);

/**
 * 检查墨袋参数一致性
 */
JNIEXPORT jint JNICALL Java_com_Smartcard_checkConsistency(JNIEnv *env, jclass arg, jint card, jint supply);

JNIEXPORT int JNICALL Java_com_Smartcard_getMaxVolume(JNIEnv *env, jclass arg, jint card);

/**
 * 读取合法性检查数据，仅为认证使用
 */
JNIEXPORT jstring JNICALL Java_com_Smartcard_readConsistency(JNIEnv *env, jclass arg, jint card);

/**
 * 检查墨袋是否已经用完
 */
JNIEXPORT jint JNICALL Java_com_Smartcard_checkOIB(JNIEnv *env, jclass arg, jint card);

/**
 * 获取剩余墨量数据
 */
JNIEXPORT jint JNICALL Java_com_Smartcard_getLocalInk(JNIEnv *env, jclass arg, jint card);

/**
 * 减锁操作
 */
JNIEXPORT jint JNICALL Java_com_Smartcard_downLocal(JNIEnv *env, jclass arg, jint card);

/**
 * 写OIB(本人认为没有这个必要）
 */
JNIEXPORT jint JNICALL Java_com_Smartcard_writeOIB(JNIEnv *env, jclass arg, jint card);

JNIEXPORT jint JNICALL Java_com_Smartcard_writeDAC5571(JNIEnv *env, jclass arg, jint value);
JNIEXPORT jint JNICALL Java_com_Smartcard_readADS1115(JNIEnv *env, jclass arg, jint index);

JNIEXPORT jint JNICALL Java_com_Smartcard_readHX24LC(JNIEnv *env, jclass arg);
JNIEXPORT jint JNICALL Java_com_Smartcard_writeHX24LC(JNIEnv *env, jclass arg, jint value);
/**
 * 读取Level值
 */
// H.M.Wang 2022-11-1 Add this API for Bagink Use
JNIEXPORT jint JNICALL Java_com_Smartcard_readLevelDirect(JNIEnv *env, jclass arg);
// End of H.M.Wang 2022-11-1 Add this API for Bagink Use
JNIEXPORT jint JNICALL Java_com_Smartcard_readLevel(JNIEnv *env, jclass arg, jint card);

/**
 * 测试Level值
 */
JNIEXPORT jint JNICALL Java_com_Smartcard_testLevel(JNIEnv *env, jclass arg, jint card);
/**
 * 读取ManufactureID
 */
JNIEXPORT jint JNICALL Java_com_Smartcard_readManufactureID(JNIEnv *env, jclass arg, jint card);
/**
 * 读取DeviceID
 */
JNIEXPORT jint JNICALL Java_com_Smartcard_readDeviceID(JNIEnv *env, jclass arg, jint card);


#ifdef __cplusplus
}
#endif
#endif //_PRINTER_AS_COM_SMARTCARD_H
