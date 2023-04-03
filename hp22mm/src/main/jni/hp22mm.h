//
// Created by kevin on 2019/2/17.
//

#include <jni.h>

#ifndef _PRINTER_AS_COM_HP22MM_H
#define _PRINTER_AS_COM_HP22MM_H
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL Java_com_hp22mm_init(JNIEnv *env, jclass arg);
JNIEXPORT jstring JNICALL Java_com_ids_get_sys_info(JNIEnv *env, jclass arg);
JNIEXPORT jint JNICALL Java_com_hp22mm_init_ids(JNIEnv *env, jclass arg);
JNIEXPORT jstring JNICALL Java_com_pd_get_sys_info(JNIEnv *env, jclass arg);
JNIEXPORT jint JNICALL Java_com_hp22mm_init_pd(JNIEnv *env, jclass arg);
JNIEXPORT jint JNICALL Java_com_ids_set_platform_info(JNIEnv *env, jclass arg);
JNIEXPORT jint JNICALL Java_com_pd_set_platform_info(JNIEnv *env, jclass arg);
JNIEXPORT jint JNICALL Java_com_ids_set_date(JNIEnv *env, jclass arg);
JNIEXPORT jint JNICALL Java_com_pd_set_date(JNIEnv *env, jclass arg);
JNIEXPORT jint JNICALL Java_com_ids_set_stall_insert_count(JNIEnv *env, jclass arg);
JNIEXPORT jint JNICALL Java_com_ids_get_supply_status(JNIEnv *env, jclass arg);
JNIEXPORT jstring JNICALL Java_com_ids_get_supply_status_info(JNIEnv *env, jclass arg);
//JNIEXPORT jint JNICALL Java_com_ids_get_supply_id(JNIEnv *env, jclass arg);
//JNIEXPORT jstring JNICALL Java_com_ids_get_supply_id_info(JNIEnv *env, jclass arg);
JNIEXPORT jint JNICALL Java_com_pd_get_print_head_status(JNIEnv *env, jclass arg, jint penIndex);
JNIEXPORT jstring JNICALL Java_com_pd_get_print_head_status_info(JNIEnv *env, jclass arg);
//JNIEXPORT jint JNICALL Java_com_pd_sc_get_info(JNIEnv *env, jclass arg, jint penIndex);
//JNIEXPORT jstring JNICALL Java_com_pd_sc_get_info_msg(JNIEnv *env, jclass arg);
JNIEXPORT jint JNICALL Java_com_DeletePairing(JNIEnv *env, jclass arg);
JNIEXPORT jint JNICALL Java_com_DoPairing(JNIEnv *env, jclass arg, jint penIdx);
JNIEXPORT jint JNICALL Java_com_DoOverrides(JNIEnv *env, jclass arg, jint penIdx);
JNIEXPORT jint JNICALL Java_com_Pressurize();
JNIEXPORT jstring JNICALL Java_com_getPressurizedValue();
JNIEXPORT jint JNICALL Java_com_Depressurize();
JNIEXPORT jint JNICALL Java_com_UpdatePDFW(JNIEnv *env, jclass arg);
JNIEXPORT jint JNICALL Java_com_UpdateFPGAFlash(JNIEnv *env, jclass arg);
JNIEXPORT jint JNICALL Java_com_UpdateIDSFW(JNIEnv *env, jclass arg);

#ifdef __cplusplus
}
#endif
#endif //_PRINTER_AS_COM_HP22MM_H
