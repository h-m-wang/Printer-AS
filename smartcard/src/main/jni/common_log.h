#include <jni.h>
#include <android/log.h>

#ifndef _COMMON_LOG
#define _COMMON_LOG

#ifdef __cplusplus
extern "C" {
#endif // __cplusplus

#ifndef LOG_TAG
#define LOG_TAG "SmartCard"
#endif

//#define filename(x) (strrchr(x,'/')?strrchr(x,'/')+1:x)
// H.M.Wang 2025-6-9 修改为log可设置为输出和不输出
extern char gOutputLog;
// End of H.M.Wang 2025-6-9 修改为log可设置为输出和不输出

#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)
//#define LOGD(...) {__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "%s:%s(%d)\n", filename(__FILE__), __FUNCTION__, __LINE__);__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__);}
// H.M.Wang 2025-6-9 修改为log可设置为输出和不输出
#define LOGD(...) if(gOutputLog) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
// End of H.M.Wang 2025-6-9 修改为log可设置为输出和不输出
//#define LOGI(...) {__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "%s:%s(%d)\n", filename(__FILE__), __FUNCTION__, __LINE__);__android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__);}
// H.M.Wang 2025-6-9 修改为log可设置为输出和不输出
#define LOGI(...) if(gOutputLog) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
// End of H.M.Wang 2025-6-9 修改为log可设置为输出和不输出
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
//#define LOGE(...) {__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "%s:%s(%d)\n", filename(__FILE__), __FUNCTION__, __LINE__);__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__);}
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL, LOG_TAG, __VA_ARGS__)

#ifdef __cplusplus
}
#endif // __cplusplus
#endif // _COMMON_LOG