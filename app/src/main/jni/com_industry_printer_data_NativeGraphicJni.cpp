#include "com_industry_printer_data_NativeGraphicJni.h"
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <math.h>
#include <time.h>

#ifdef __cplusplus
extern "C"
{
#endif

#define RED(a) (((a) >> 16) & 0x000000ff)
#define GREEN(a) (((a) >> 8) & 0x000000ff)
#define BLUE(a) (((a) >> 0) & 0x000000ff)
#define MAX(a, b) ((a) > (b) ? (a) : (b))
#define MIN(a, b) ((a) > (b) ? (b) : (a))

static jint *DOTS = new jint[8];
static jint OrgLines=0, TarLines=0;
/*
 * Class:     com_industry_printer_data_NativeGraphicJni
 * Method:    ShiftImage
 * Signature: ([IIIIII)[I
 */
JNIEXPORT jintArray JNICALL Java_com_industry_printer_data_NativeGraphicJni_ShiftImage
  (JNIEnv *env, jclass thiz, jintArray src, jint width, jint height, jint head, jint orgLines, jint tarLines) {

//    LOGD("ShiftImage: [%d, %d], head=%d, orgLines=%d, tarLines=%d", width, height, head, orgLines, tarLines);
    OrgLines = orgLines;
    TarLines = tarLines;
    return src;

    jint *cbuf;
    cbuf = env->GetIntArrayElements(src, 0);

    jsize length = width * height;
    jint *rbuf = new jint[length];
    memset(rbuf, 0xff, length * sizeof(jint));

    for(int i=head-1; i>=0; i--) {
        memcpy(rbuf + i * tarLines * width, cbuf + i * orgLines * width, orgLines * width * sizeof(jint));
    }

    jintArray result = env->NewIntArray(length);
    env->SetIntArrayRegion(result, 0, length, rbuf);
    env->ReleaseIntArrayElements(src, cbuf, 0);

    return result;
}

/*
 * Class:     com_industry_printer_data_NativeGraphicJni
 * Method:    Binarize
 * Signature: ([IIIIII)[B
 * 2026-4-14 新修改的版本中，由于原图做了旋转镜像，因此横轴和纵轴交换
 */
JNIEXPORT jbyteArray JNICALL Java_com_industry_printer_data_NativeGraphicJni_Binarize
        (JNIEnv *env, jclass thiz, jintArray src, jint width, jint height, int head, jint value, jint reset) {

    jint *cbuf;
    cbuf = env->GetIntArrayElements(src, 0);

//    int colEach = (((height % 8) == 0) ? height/8 : height/8+1);
//    int newSize = colEach * width;
//    int heighEachHead = height / head;

    int colEach = (((width % 8) == 0) ? width/8 : width/8+1);
    int newSize = height * colEach;
    jbyte *rbuf = new jbyte[newSize];

    if(reset) memset(DOTS, 0x00, 8 * sizeof(jint));

/*
    for(int i=0; i<width; i++) {
        for(int j=0; j<height; j++) {
            int curr_color = *(cbuf + j * width + i);
            int pixR = RED(curr_color);
            int pixG = GREEN(curr_color);
            int pixB = BLUE(curr_color);
//    	    int pixA = ALPHA(curr_color);

            int grey = (int)((float) pixR * 0.3 + (float)pixG * 0.59 + (float)pixB * 0.11);

            if(grey > value)
                rbuf[i*colEach + j/8] &= ~( 0x01 << (j%8));
            else {
                rbuf[i*colEach + j/8] |= (0x01 << (j%8));
                DOTS[(int)(j / heighEachHead)]++;
            }
        }
    }
*/
    jint *cbuf_tmp  = cbuf;
    jbyte *rbuf_tmp = rbuf;
    memset(rbuf_tmp, 0x00, newSize);
    int head_index = 0;
    int dot_count = 0;

    for(int j=0; j<height * width; j+=8, rbuf_tmp++) {
        if(TarLines > 0 && dot_count == TarLines) {
//            if(j<2*height)LOGD("ProcTime Skip End: [%d, %d], head=%d", j, 0, head_index);
            dot_count = 0;
            head_index++;
            if(head_index == head) {
                head_index = 0;
                cbuf_tmp += (TarLines - OrgLines) * head;
//                if(j<2*height)LOGD("ProcTime New Line: orgaddr=%d", j);
            }
        }
        for(int i=0; i<8; i++, dot_count++) {
            if(OrgLines > 0 && dot_count >= OrgLines) {
//                if(j<2*height)LOGD("ProcTime Skip: [%d, %d], head=%d", j, i, head_index);
                continue;
            }
            int curr_color = *cbuf_tmp++;
//            int pixR = RED(curr_color);
//            int pixG = GREEN(curr_color);
//            int pixB = BLUE(curr_color);
//    	    int pixA = ALPHA(curr_color);

//            int grey = (int)((float) pixR * 0.3 + (float)pixG * 0.59 + (float)pixB * 0.11);

//            if(grey <= value) {
            if(curr_color == 0xFF000000) {
                *rbuf_tmp |= (0x01 << i);
                DOTS[head_index]++;
            }
        }
//        if(j<height)LOGD("ProcTime rbuf_tmp[%d] = %08x", j/8, *rbuf_tmp);
    }
    OrgLines = 0;
    TarLines = 0;

    jbyteArray result = env->NewByteArray(newSize);
    env->SetByteArrayRegion(result, 0, newSize, rbuf);
    env->ReleaseIntArrayElements(src, cbuf, 0);
    delete[] rbuf;

    return result;
}

/*
 * Class:     com_industry_printer_data_NativeGraphicJni
 * Method:    GetDots
 * Signature: ()[I
 */
JNIEXPORT jintArray JNICALL Java_com_industry_printer_data_NativeGraphicJni_GetDots
  (JNIEnv *env, jclass thiz) {

//    LOGD("GetDots");

    jintArray result = env->NewIntArray(8);
    env->SetIntArrayRegion(result, 0, 8, DOTS);

    return result;
}

/*
 * Class:     com_industry_printer_data_NativeGraphicJni
 * Method:    GetBgBuffer
 * Parameters:
 *      src:        原始位图数据缓冲区，一个bit代表一个dot;
 *      length:     缓冲区总长度;
 *      bytesFeed:  目标缓冲区每列的数据长度（因为有不同打印头之间的缝隙，要比实际数据长）
 *      bytesPerHFeed：目标缓冲区每个打印头的数据长度
 *      bytesPerH： 原始缓冲区每个头侧数据长度
 *      column：    数据总列数
 *      type：      打印头的数量
 * Signature: ([BIIIIII})[C
 */
JNIEXPORT jcharArray JNICALL Java_com_industry_printer_data_NativeGraphicJni_GetBgBuffer
        (JNIEnv *env, jclass thiz, jbyteArray src, jint length, jint bytesFeed, jint bytesPerHFeed, jint bytesPerH, jint column, jint type) {

//    LOGD("GetBgBuffer length=%d, bytesFeed=%d, bytesPerHFeed=%d, bytesPerH=%d, column=%d, type=%d", length, bytesFeed, bytesPerHFeed, bytesPerH, column, type);

    jbyte *cbuf;
    cbuf = env->GetByteArrayElements(src, 0);

    jbyte *rByteBuf = NULL;
    jchar *rCharBuf = NULL;

    // 当每个头需要的DOT数据字节数大于每个头实际拥有的DOT字节数（12.7xn喷头时，每个头的实际数据为152点，19个字节，但打印缓冲区每个头必须为20字节，因此会出现这个需要补齐的情况）
    if(bytesPerHFeed > bytesPerH) {
        rByteBuf = new jbyte[length];
        memset(rByteBuf, 0x00, length);

        size_t orgPointer = 0;
        for(int i=0; i < column; i++) {
//        LOGD("GetBgBuffer Column = %d", column);
            for (int j = 0; j < type; j++) {
//            LOGD("GetBgBuffer Type = %d", type);
                jint pos = i * bytesFeed + j * bytesPerHFeed;
                memcpy(rByteBuf + pos, cbuf + orgPointer, bytesPerH);
                orgPointer += bytesPerH;
            }
        }
    // 当每个头需要的DOT数据字节数等于每个头实际拥有的DOT字节数的时候，无需移位操作
    } else if(bytesPerHFeed == bytesPerH) {
        rByteBuf = cbuf;
    }

    if(NULL != rByteBuf) {
        rCharBuf = new jchar[length/2];
        for(int i=0; i<length/2; i++) {
            rCharBuf[i] = (jchar) (((jchar)(rByteBuf[2*i+1] << 8) & 0x0ff00) | (rByteBuf[2*i] & 0x0ff));
        }
        if(rByteBuf != cbuf) {
            delete[] rByteBuf;
        }
    }

    jcharArray result = NULL;
    env->ReleaseByteArrayElements(src, cbuf, 0);
    if(NULL != rCharBuf) {
        result = env->NewCharArray(length/2);
        env->SetCharArrayRegion(result, 0, length/2, rCharBuf);
        delete[] rCharBuf;
    }

//    LOGD("GetBgBuffer done");
    return result;
}

/*
 * Class:     com_industry_printer_data_NativeGraphicJni
 * Method:    GetPrintDots
 * Parameters:
 *      src:                原始位图数据缓冲区，一个bit代表一个dot;
 *      bytesPerHFeed：     目标缓冲区每个打印头的数据长度
 *      heads：             打印头的数量
 * Signature: ([CIII})[I
 */
static int nibble_dots[16] = {0,1,1,2,1,2,2,3,1,2,2,3,2,2,3,4}; // 0000 - 1111各个数值的1的个数
static int byte_dots[256];
JNIEXPORT jintArray JNICALL Java_com_industry_printer_data_NativeGraphicJni_GetPrintDots
        (JNIEnv *env, jclass thiz, jcharArray src, jint length, jint charsPerHFeed, jint heads) {

    jchar *srcBuf = env->GetCharArrayElements(src, 0);

//    LOGD("GetPrintDots- length=%d, charsPerHFeed=%d, heads=%d", length, charsPerHFeed, heads);

//    jint *dots = new jint[8];
    jint dots[8];
    memset(dots, 0x00, 8 * sizeof(jint));

    int headIndex = -1;      // 当前数据所属打印头。初值为-1，为进入循环处理做准备
    for(int i=0; i<length; i++) {
        if((i % charsPerHFeed) == 0) {
            headIndex++;
            headIndex %= heads;
        }
// H.M.Wang 2026-4-9 取消移位判定的做法，改为查表的方法获取点数，这样可以大幅度提高效率（8064*2720个点的bin，耗时由440->170ms）
/*        for(int j=0; j<16; j++) {
            if( (srcBuf[i] & (0x0001 << j)) != 0x0000) {
                dots[headIndex]++;
            }
        }*/

        dots[headIndex] += byte_dots[srcBuf[i]/256];
        dots[headIndex] += byte_dots[srcBuf[i]%256];
// End of H.M.Wang 2026-4-9 取消移位判定的做法，改为查表的方法获取点数，这样可以大幅度提高效率（8064*2720个点的bin，耗时由440->170ms）

    }

    env->ReleaseCharArrayElements(src, srcBuf, JNI_ABORT);

    jintArray result = env->NewIntArray(8);
    env->SetIntArrayRegion(result, 0, 8, dots);
//    delete[] dots;

    return result;
}

// 2026-4-9 1.0.6 修改GetPrintDots获取点数的方法，可以大大提高处理性能。二值化的处理暂时不修改，因为改善的不太多
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved){
    LOGI("NativeGraphicJni.so 1.0.7 Loaded.");
    for(int i=0; i<16; i++) {
        for(int j=0; j<16; j++) {
            byte_dots[i*16+j] = nibble_dots[i] + nibble_dots[j];
        }
    }
    return JNI_VERSION_1_4;     //这里很重要，必须返回版本，否则加载会失败。
}

#ifdef __cplusplus
}
#endif
