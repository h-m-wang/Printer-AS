#include "com_industry_printer_data_NativeGraphicJni.h"
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <math.h>
#include <time.h>
#include "android/bitmap.h"

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
//static jint ShiftBitsOdd=0;
//static jint ShiftBitsEven=0;
//static jint OverlapBits=0;
/*
 * Class:     com_industry_printer_data_NativeGraphicJni
 * Method:    ShiftImage
 * Signature: ([IIIIII)[I
 */
JNIEXPORT jintArray JNICALL Java_com_industry_printer_data_NativeGraphicJni_ShiftImage
  (JNIEnv *env, jclass thiz, jintArray src, jint width, jint height, jint head, jint orgLines, jint tarLines) {

//    LOGD("ShiftImage: [%d, %d], head=%d, orgLines=%d, tarLines=%d", width, height, head, orgLines, tarLines);
//    ShiftBitsOdd = width;          // 用width传递单数头横向位移点数。
//    ShiftBitsEven = height;        // 用height传递单数头横向位移点数。
//    OverlapBits = head;            // 用head传递508位重叠次数
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
        (JNIEnv *env, jclass thiz, jintArray src, jint width, jint height, jint head, jint value, jint reset) {

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
    unsigned int curr_color;
//    int shiftBits = ShiftBitsOdd;
    jbyte vals[8] = {0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, (jbyte)0x80};

//    LOGD("ShiftImage: head_index=%d, shiftBits=%d, OverlapBits=%d", head_index, shiftBits, OverlapBits);
    for(int j=0; j<height * width; j+=8, rbuf_tmp++) {
        if((TarLines > 0 && dot_count == TarLines) || (TarLines == 0 && dot_count == width / head)) {
//            a--;
            dot_count = 0;
            head_index++;
            if(head_index == head) {
                head_index = 0;
                cbuf_tmp += (TarLines - OrgLines) * head;
            }
//            shiftBits = (((head_index & 0x01) == 0) ? ShiftBitsOdd : ShiftBitsEven);       // 根据当前头修改对应于当前头的位移量，head_index=0,2,4代表1，3，5头，使用ShiftBitsOdd， 1，3代表2，4头使用ShiftBitsEven
        }
        for(int i=0; i<8; i++, dot_count++) {
            // 主要是对于108mm头的508后重叠做处理
//            if(OrgLines > 0 && dot_count >= OrgLines+shiftBits && dot_count < OrgLines+shiftBits+OverlapBits) {
//                if(curr_color < (unsigned int)0xFFF0F0F0) {     // 由于处理的原图基本上都是黑白的，因此可以简略处理，对于彩色图，需要先做灰度化
//                    *rbuf_tmp |= vals[i];
//                    DOTS[head_index]++;
//                }
//                continue;
//            }
            // 主要是对于108mm头的平行移位做处理
//            if(OrgLines > 0 && (dot_count < shiftBits || dot_count >= OrgLines+shiftBits)) {
            if(OrgLines > 0 && dot_count >= OrgLines) {
                continue;
            }

            curr_color = *cbuf_tmp++;
//            int pixR = RED(curr_color);
//            int pixG = GREEN(curr_color);
//            int pixB = BLUE(curr_color);
//    	    int pixA = ALPHA(curr_color);

//            int grey = (int)((float) pixR * 0.3 + (float)pixG * 0.59 + (float)pixB * 0.11);

//            if(grey <= value) {
//            if(curr_color == 0xFF000000) {
            if(curr_color < (unsigned int)0xFFF0F0F0) {     // 由于处理的原图基本上都是黑白的，因此可以简略处理，对于彩色图，需要先做灰度化
                *rbuf_tmp |= vals[i];
                DOTS[head_index]++;
            }
        }
//        if(j<height)LOGD("ProcTime rbuf_tmp[%d] = %08x", j/8, *rbuf_tmp);
    }
    OrgLines = 0;
    TarLines = 0;
//    ShiftBitsOdd = 0;
//    ShiftBitsEven = 0;
//    OverlapBits = 0;

    jbyteArray result = env->NewByteArray(newSize);
    env->SetByteArrayRegion(result, 0, newSize, rbuf);
    env->ReleaseIntArrayElements(src, cbuf, 0);
    delete[] rbuf;

    return result;
}

/*
 * Class:     com_industry_printer_data_NativeGraphicJni
 * Method:    BinarizeBmp
 * Signature: (Ljava/lang/Object;III)[B
 * 2026-4-14 新修改的版本中，由于原图做了旋转镜像，因此横轴和纵轴交换
 * 函数说明：接收到的图像示意图：（以4个头为例说明，-代表内容，o代表空挡）
 * -------- -------- -------- -------- oooooooo
 * -------- -------- -------- -------- oooooooo
 * -------- -------- -------- -------- oooooooo
 * -------- -------- -------- -------- oooooooo
 * (后续省略)
 * 这个示意图为原始生成的位图的旋转90后镜像的结果
 * 即：现在看到的图案的右上角为原位图的左下角；左下角为原位图的右上角
 * （这么处理是为了二值化处理时与目标bin同向，防止地址的反复横条，带来CPU额外开销）
 * 这个位图的特点是高度为满高，只是图案是画在可见高度部分（-），底部空出空位（o）
 * 二值化处理后的结果示意图：
 * --------oo --------oo --------oo --------oo
 * --------oo --------oo --------oo --------oo
 * --------oo --------oo --------oo --------oo
 * --------oo --------oo --------oo --------oo
 * 即：进行二值化的同时，将每个头的不打印部分空出，相当于给可打印部分的每个头的部分插入空挡，躲过不打印部分
 *
 * 参数说明：
 *  bmp：    已经经过旋转镜像的位图，作为二值化处理的原图
 *  width：  bmp的宽，由于经过了旋转，所以相当于本来图的高
 *  height： bmp的高，由于经过了旋转，所以相当于本来图的宽，即原图的列数
 *  head：   头数，二值化时的插值会发生在每个头的末尾处
 *  value：  二值化的阈值，大于该值按白色（无点），反之按黑色（有点）处理。但是当前按0xFFF0F0F0（ABGR）来判断了，因此等于弃用
 *  reset：  二值化后的黑色点个数计数是否清零，这个由于原来会将一个位图切割成几块分别二值化再合并，因此，中间过程点数不能清零，当前已经取消分块二值化，所以该参数等于弃用
 *  由ShiftImage函数传递的参数：
 *  ShiftImage原来的功能是在二值化处理之前，先行将位图数据中插入每个头的空档位至，但是由于二值化处理改为在二值化处理的同时插入空挡，因此，ShiftImage函数本身的功能已经弃用，只用其传递下列参数
 *  OrgLines：插入空挡的开始位置，如果为0，则表示不插入空挡
 *  TarLines：插入空挡的结束位置，如果为0，则表示不插入空挡
 */
JNIEXPORT jbyteArray JNICALL Java_com_industry_printer_data_NativeGraphicJni_BinarizeBmp
        (JNIEnv *env, jclass thiz, jobject bmp, jint width, jint height, jint head, jint value, jint reset) {
//    LOGI("Enter BinarizeBmp. Width = %d, Height = %d", width, height);

    int colEach = (((width % 8) == 0) ? width/8 : width/8+1);       // 原图高取8的倍数，以便包含未满格的位，然后对应的字节数（8个点占用一个字节），当前原图高肯定是8的倍数，所以实际就等于width
    int newSize = height * colEach;                                 // 二值化后数据实际需要的字节数
    jint *cbuf_tmp  = NULL;                                         // 原位图数据区指针
    jbyte *rbuf = new jbyte[newSize];                               // 二值化结果保存区
    jbyte *rbuf_tmp = rbuf;                                         // 二值化结果保存区的指针
    memset(rbuf_tmp, 0x00, newSize);                             // 保存区清零
    jbyte vals[8] = {0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, (jbyte)0x80};    // 位点，用于二值化时快速定位为点，避免位移操作（耗时）
    int head_index = 0;                                             // 头索引 0-head
    int dot_count = 0;                                              // 每个头对应的数据中的点数计数
    unsigned int curr_color;                                        // 每个点的颜色值
//    int shiftBits = ShiftBitsOdd;

    void* pixels;
/*    AndroidBitmapInfo info;

    // 1. 获取 Bitmap 信息
    if (AndroidBitmap_getInfo(env, bmp, &info) < 0) {
        LOGE("AndroidBitmap_getInfo() failed");
        goto quit;
    }

    // 2. 检查格式 (推荐 ARGB_8888)
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Bitmap format is not RGBA_8888! (%d)", info.format);
        goto quit;
    }
*/
    // 3. 锁定像素缓冲区
    if (AndroidBitmap_lockPixels(env, bmp, &pixels) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed");
        goto quit;
    }

//    int width = info.width;
//    int height = info.height;
    if(reset) memset(DOTS, 0x00, 8 * sizeof(jint));

    cbuf_tmp  = (jint *)pixels;
//    LOGD("ShiftImage: head_index=%d, shiftBits=%d, OverlapBits=%d", head_index, shiftBits, OverlapBits);
    for(int j=0; j<height * width; j+=8, rbuf_tmp++) {      // 对于所有的点，每8个点一个单位处理二值化，保存在一个目标保存区的字节中
        if((TarLines > 0 && dot_count == TarLines) || (TarLines == 0 && dot_count == width / head)) {   // 当需要插值，并且点数计数已经到了插值结束位置，或者 不需要插值点数计数到了头的分解处时，切换头
//            a--;
            dot_count = 0;          // 头内计数清零
            head_index++;           // 头索引+1
            if(head_index == head) {    // 当最后一个头结束时
                head_index = 0;         // 头索引清零
                cbuf_tmp += (TarLines - OrgLines) * head;   // 跳过元数据中的末尾空位（一连串的o）
            }
//            shiftBits = (((head_index & 0x01) == 0) ? ShiftBitsOdd : ShiftBitsEven);       // 根据当前头修改对应于当前头的位移量，head_index=0,2,4代表1，3，5头，使用ShiftBitsOdd， 1，3代表2，4头使用ShiftBitsEven
        }
        for(int i=0; i<8; i++, dot_count++) {       // 对于每个连续的8个点位
            // 主要是对于108mm头的508后重叠做处理
//            if(OrgLines > 0 && dot_count >= OrgLines+shiftBits && dot_count < OrgLines+shiftBits+OverlapBits) {
//                if(curr_color < (unsigned int)0xFFF0F0F0) {     // 由于处理的原图基本上都是黑白的，因此可以简略处理，对于彩色图，需要先做灰度化
//                    *rbuf_tmp |= vals[i];
//                    DOTS[head_index]++;
//                }
//                continue;
//            }
            // 主要是对于108mm头的平行移位做处理
//            if(OrgLines > 0 && (dot_count < shiftBits || dot_count >= OrgLines+shiftBits)) {
            if(OrgLines > 0 && dot_count >= OrgLines) {     // 如果处理插值，并且点数计数到了开始插值的位置，则跳过该位（即插入空白）
                continue;
            }

            curr_color = *cbuf_tmp++;       // Java的颜色值位置对应 ARGB，但是Jni当中的对应关系是ABGR，不过对于我们的使用情况不影响
//            int pixR = RED(curr_color);
//            int pixG = GREEN(curr_color);
//            int pixB = BLUE(curr_color);
//    	    int pixA = ALPHA(curr_color);

//            int grey = (int)((float) pixR * 0.3 + (float)pixG * 0.59 + (float)pixB * 0.11);

//            if(grey <= value) {
//            if(curr_color == 0xFF000000) {
            if(curr_color < (unsigned int)0xFFF0F0F0) {     // 由于处理的原图基本上都是黑白的，因此可以简略处理，对于彩色图，需要先做灰度化
                *rbuf_tmp |= vals[i];               // 有黑点的在相应位置黑
                DOTS[head_index]++;                 // 黑点数统计+1
            }
        }
//        if(j<height)LOGD("ProcTime rbuf_tmp[%d] = %08x", j/8, *rbuf_tmp);
    }
    OrgLines = 0;
    TarLines = 0;
//    ShiftBitsOdd = 0;
//    ShiftBitsEven = 0;
//    OverlapBits = 0;

    AndroidBitmap_unlockPixels(env, bmp);
quit:
    jbyteArray result = env->NewByteArray(newSize);
    env->SetByteArrayRegion(result, 0, newSize, rbuf);
    delete[] rbuf;
//    LOGI("BinarizeBmp done");

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

// 2026-4-29 取消jbyte->jchar的转换，经过确认，此操作没有必要，直接将jbyte数组的指针传递给SetCharArrayRegion函数，转化为jchararray就可以，节约用时一倍
/*  if(NULL != rByteBuf) {
        rCharBuf = new jchar[length/2];
        for(int i=0; i<length/2; i++) {
            rCharBuf[i] = (jchar) (((jchar)(rByteBuf[2*i+1] << 8) & 0x0ff00) | (rByteBuf[2*i] & 0x0ff));
        }
        if(rByteBuf != cbuf) {
            delete[] rByteBuf;
        }
    }
*/
// End of 2026-4-29 取消jbyte->jchar的转换，经过确认，此操作没有必要，直接将jbyte数组的指针传递给SetCharArrayRegion函数，转化为jchararray就可以

    jcharArray result = NULL;
    env->ReleaseByteArrayElements(src, cbuf, 0);
// 2026-4-29 取消jbyte->jchar的转换
/*  if(NULL != rCharBuf) {
        result = env->NewCharArray(length/2);
        env->SetCharArrayRegion(result, 0, length/2, rCharBuf);
        delete[] rCharBuf;
    }
*/
    result = env->NewCharArray(length/2);
    env->SetCharArrayRegion(result, 0, length/2, (jchar *)rByteBuf);
    if(rByteBuf != cbuf) {
        delete[] rByteBuf;
    }
// End of 2026-4-29 取消jbyte->jchar的转换

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

// 2026-8-17 1.0.14 增加一个以Java的Bitmap为参数的二值化函数BinarizeBmp，这个通过引入这个参数，可以直接访问数据区（Pixels）而不需要通过getPixels函数获取，因为这个函数会复制数据，导致时间和内存的开销
// 2026-5-13 1.0.13 二值化调整头的算法，增加不中间填充空白操作的分头统计
//     (TarLines == 0 && dot_count == width / head)
// 2026-5-9 1.0.12 取消1.0.11中追加的单双数头位移和重叠的功能，恢复到原来的处理
// 2026-4-29 1.0.11 增加单双数头的位移参数和重叠参数的传递和对应处理
// 即原来1-508为实际内容，509-544填空；扩展为，加入设置位移值为n，则1-(1+n)填空，(1+n)-(508+n)填充实际值，(508+n+1)-544填空
// 2026-4-27 1.0.10 510,511也复制508的值
// 2026-4-27 1.0.9 509复制508的值
// 2026-4-26 1.0.8 二值化时，修改黑白判断标准，取消原来的
//          if(curr_color == 0xFF000000) {
// 修改为
//          curr_color < (unsigned int)0xFF888888
// 2026-4-14 1.0.7 新修改的版本中，由于原图做了旋转镜像，因此横轴和纵轴交换
// 2026-4-9 1.0.6 修改GetPrintDots获取点数的方法，可以大大提高处理性能。二值化的处理暂时不修改，因为改善的不太多
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved){
    LOGI("NativeGraphicJni.so 1.0.14 Loaded.");
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
/*
#include <jni.h>
#include <android/bitmap.h>
#include <android/log.h>

#define TAG "BitmapJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

extern "C" JNIEXPORT void JNICALL
Java_com_example_yourpackage_BitmapUtil_processBitmap(JNIEnv *env, jobject thiz, jobject bitmap) {
    AndroidBitmapInfo info;
    void* pixels;

    // 1. 获取 Bitmap 信息
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) {
        LOGI("AndroidBitmap_getInfo() failed");
        return;
    }

    // 2. 检查格式 (推荐 ARGB_8888)
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGI("Bitmap format is not RGBA_8888!");
        return;
    }

    // 3. 锁定像素缓冲区
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) {
        LOGI("AndroidBitmap_lockPixels() failed");
        return;
    }

    // 4. 操作像素数据
    // pixels 是一个指向像素数组的指针，每个像素占 4 字节 (RGBA)
    int width = info.width;
    int height = info.height;
    uint32_t* pixelData = (uint32_t*) pixels;

    // 示例：将所有像素的红色通道置零
    for (int y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            uint32_t color = pixelData[y * width + x];
            // 提取 RGBA 分量 (注意字节序可能是 ARGB 或 RGBA，建议按 32 位处理)
            uint8_t r = (color >> 16) & 0xFF;
            uint8_t g = (color >> 8) & 0xFF;
            uint8_t b = color & 0xFF;
            uint8_t a = (color >> 24) & 0xFF;
            // 进行像素修改...
            // pixelData[y * width + x] = newColor;
        }
    }

    // 5. 解锁像素缓冲区 (重要!)
    AndroidBitmap_unlockPixels(env, bitmap);
    LOGI("Bitmap processed in native code.");
}
 */