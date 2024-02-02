//
// Created by kevin on 2019/2/17.
//

#include <jni.h>

#ifndef _PRINTER_AS_COM_HP22MM_H
#define _PRINTER_AS_COM_HP22MM_H
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jstring JNICALL Java_com_ids_get_sys_info(JNIEnv *env, jclass arg);
JNIEXPORT jint JNICALL Java_com_hp22mm_init_ids(JNIEnv *env, jclass arg, jint idsIndex);
JNIEXPORT jstring JNICALL Java_com_pd_get_sys_info(JNIEnv *env, jclass arg);
JNIEXPORT jint JNICALL Java_com_hp22mm_init_pd(JNIEnv *env, jclass arg, jint penIndex);
JNIEXPORT jint JNICALL Java_com_ids_set_platform_info(JNIEnv *env, jclass arg);
JNIEXPORT jint JNICALL Java_com_pd_set_platform_info(JNIEnv *env, jclass arg);
JNIEXPORT jint JNICALL Java_com_ids_set_date(JNIEnv *env, jclass arg);
JNIEXPORT jint JNICALL Java_com_pd_set_date(JNIEnv *env, jclass arg);
JNIEXPORT jint JNICALL Java_com_ids_set_stall_insert_count(JNIEnv *env, jclass arg);
JNIEXPORT jint JNICALL Java_com_ids_get_supply_status(JNIEnv *env, jclass arg);
JNIEXPORT jstring JNICALL Java_com_ids_get_supply_status_info(JNIEnv *env, jclass arg);
JNIEXPORT jint JNICALL Java_com_pd_get_print_head_status(JNIEnv *env, jclass arg);
JNIEXPORT jstring JNICALL Java_com_pd_get_print_head_status_info(JNIEnv *env, jclass arg);
JNIEXPORT jint JNICALL Java_com_pd_get_sc_status(JNIEnv *env, jclass arg);
JNIEXPORT jstring JNICALL Java_com_pd_get_sc_status_info(JNIEnv *env, jclass arg);
JNIEXPORT jint JNICALL Java_com_pd_get_sc_info(JNIEnv *env, jclass arg);
JNIEXPORT jstring JNICALL Java_com_pd_get_sc_info_info(JNIEnv *env, jclass arg);
JNIEXPORT jint JNICALL Java_com_DeletePairing(JNIEnv *env, jclass arg);
JNIEXPORT jint JNICALL Java_com_DoPairing(JNIEnv *env, jclass arg);
JNIEXPORT jint JNICALL Java_com_DoOverrides(JNIEnv *env, jclass arg);
JNIEXPORT jint JNICALL Java_com_Pressurize(JNIEnv *env, jclass arg);
JNIEXPORT jstring JNICALL Java_com_getPressurizedValue(JNIEnv *env, jclass arg);
JNIEXPORT jint JNICALL Java_com_Depressurize(JNIEnv *env, jclass arg);
JNIEXPORT jint JNICALL Java_com_UpdatePDFW(JNIEnv *env, jclass arg);
JNIEXPORT jint JNICALL Java_com_UpdateFPGAFlash(JNIEnv *env, jclass arg);
JNIEXPORT jint JNICALL Java_com_UpdateIDSFW(JNIEnv *env, jclass arg);
// H.M.Wang 2023-7-27 将startPrint函数的返回值修改为String型，返回错误的具体内容
JNIEXPORT jstring JNICALL Java_com_StartPrint(JNIEnv *env, jclass arg);
// End of H.M.Wang 2023-7-27 将startPrint函数的返回值修改为String型，返回错误的具体内容
JNIEXPORT jint JNICALL Java_com_StopPrint(JNIEnv *env, jclass arg);
JNIEXPORT jstring JNICALL Java_com_DumpRegisters(JNIEnv *env, jclass arg);
JNIEXPORT jstring JNICALL Java_com_SpiTest(JNIEnv *env, jclass arg);
JNIEXPORT jint JNICALL Java_com_MCU2FIFO(JNIEnv *env, jclass arg);
JNIEXPORT jint JNICALL Java_com_FIFO2DDR(JNIEnv *env, jclass arg);
JNIEXPORT jint JNICALL Java_com_DDR2FIFO(JNIEnv *env, jclass arg);
JNIEXPORT jint JNICALL Java_com_FIFO2MCU(JNIEnv *env, jclass arg);

#ifdef __cplusplus
}
#endif
#endif //_PRINTER_AS_COM_HP22MM_H
