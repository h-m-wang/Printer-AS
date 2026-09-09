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
 * Signature: (Ljava/lang/Object;IIIII)[B
 * 函数功能：将传入的Bitmap图做二值化处理，生成供打印使用的bin数据。Bitmap中的一个点，对应于结果中的1位。特别说明：
 *          (1) 传入的Bitmap已经做了旋转镜像，即将视觉正常的Bitmap生成为右上角到左下角，而左下角到右上角。即相当于原始Bitmap以左上角为远点做了顺时针90度旋转后，以y轴为中心左右反转，这样处理的原因是Bitmap的像素保存是
 *              按着先横后纵的方式保存的点阵数据，而生成bin中的数据是按着先纵后横的顺序保存的（这个符合打印的走纸方式），因此如果不对Bitmap的朝向做事先处理，将导致Bitmap和bin总有一个需要跳行，这样会导致频繁的内存调度，
 *              从而导致经常调度内存而大幅降低内存的访问速度
 *          (2) 将原来在apk中通过getPixels函数取得图片数据改为在Jni的函数中获取，这样做的好处是避免了一次内存复制，可以提高访问效率
 *          (3) apk生成的Bitmap使用的ARGB_4444，这个属性在KITKAT以后被废弃了，因此A20的Bitmap还是ARGB_4444，一个Pixel对应2个字节，但是A133系统会强制修改为ARGB_8888，一个Pixel对应4个字节
 *          (4) 当Bitmap是12.7xn类型头的数据时（width=152的倍数，需要申请bin的数据区时每行多申请一个字节，因为需要插入空挡；25.4xn打印头类型时，由于生成Bitmap时已经预留了空间，所以不需要额外在申请
 * 函数说明：接收到的图像示意图：（-代表内容，o代表空挡）
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
 * 2026-4-14 新修改的版本中，由于原图做了旋转镜像，因此横轴和纵轴交换
*/
JNIEXPORT jbyteArray JNICALL Java_com_industry_printer_data_NativeGraphicJni_BinarizeBmp
        (JNIEnv *env, jclass thiz, jobject bmp, jint width, jint height, jint head, jint value, jint reset) {
    LOGI("Enter BinarizeBmp. Width = %d, Height = %d, Head = %d, OrgLines = %d, TarLines = %d", width, height, head, OrgLines, TarLines);

    int colEach = (((width % 152) == 0) ? width/8+head : width/8);       // 如果是12.7xn的头，为每个头多取得一个字节，因为12.7xn打印头的原始数据中没有预留这个空间
    int newSize = height * colEach;                                 // 二值化后数据实际需要的字节数
    jbyte *cbuf_tmp  = NULL;                                         // 原位图数据区指针
    int pixel_step = 4;                                             // 缺省按买个像素4个字节来处理（A20的情况下，每个字节需要2个字节，因为apk使用的是RGB4444作为bitmap的配置，这个配置已经在API level 13后被取消了
    jbyte *rbuf = new jbyte[newSize];                               // 二值化结果保存区
    jbyte *rbuf_tmp = rbuf;                                         // 二值化结果保存区的指针
    memset(rbuf_tmp, 0x00, newSize);                             // 保存区清零
    int rbuf_bit_pos = 0;                                           // 二值化结果区该写入的字节内的位顺序
    jbyte vals[8] = {0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, (jbyte)0x80};    // 位点，用于二值化时快速定位为点，避免位移操作（耗时）
    int head_index = 0;                                             // 头索引 0-head
    int dot_count = 0;                                              // 每个头对应的数据中的点数计数
    unsigned int curr_color;                                        // 每个点的颜色值
    int pos = 0;                                                    // 原数据检索位置

    void* pixels;
    AndroidBitmapInfo info;

    // 1. 获取 Bitmap 信息
    if (AndroidBitmap_getInfo(env, bmp, &info) < 0) {
        LOGE("AndroidBitmap_getInfo() failed");
        goto quit;
    }

    // 2. 检查格式 (推荐 ARGB_8888)
    LOGD("info.format = %d", info.format);
    if (info.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
        pixel_step = 4;
    } else if (info.format == ANDROID_BITMAP_FORMAT_RGBA_4444) {
        pixel_step = 2;
    } else {
        LOGE("Unsupported bitmap format %d", info.format);
        goto quit;
    }

    // 3. 锁定像素缓冲区
    if (AndroidBitmap_lockPixels(env, bmp, &pixels) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed");
        goto quit;
    }

    if(reset) memset(DOTS, 0x00, 8 * sizeof(jint));

    cbuf_tmp  = (jbyte *)pixels;
    while(pos < height*width) {        // 对于所有的点，每8个点一个单位处理二值化，保存在一个目标保存区的字节中
        if(OrgLines > 0 && TarLines > OrgLines) {  // 当需要插值的时候
            if(dot_count == OrgLines) {     // 扫描到开始插值的位置后启动插值处理
                while(dot_count < TarLines) {   // 从插值开始位置到插值结束位置，空跳过每个对应的二值化结果区的对应位（字节内满位后跳转到下一个字节）
                    dot_count++;
                    rbuf_bit_pos++;
                    if(rbuf_bit_pos == 8) {
                        rbuf_tmp++;
                        rbuf_bit_pos = 0;
                    }
                }
                dot_count = 0;          // 头内计数清零
                head_index++;           // 头索引+1
                if (head_index == head) {    // 当最后一个头结束时
                    head_index = 0;         // 头索引清零
                    if ((width % 152) != 0) {        // 非12.7xn头类型时，跳过原数据中的末尾空位（一连串的o）
                        cbuf_tmp += (TarLines - OrgLines) * head * pixel_step;
                        pos += (TarLines - OrgLines) * head;
                        if(pos >= height*width) break;
                    }
                }
            }
        }

        if(rbuf_tmp - rbuf >= newSize) {
            LOGE("ERROR: beyond target band. rbuf_tmp-rbuf=%d, newSize=%d", rbuf_tmp - rbuf, newSize);
            break;
        }

        if(info.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
            curr_color = *((jint *)cbuf_tmp);       // Java的颜色值位置对应 ARGB，但是Jni当中的对应关系是ABGR，不过对于我们的使用情况不影响
        } else {
            curr_color = *((jchar *)cbuf_tmp);       // Java的颜色值位置对应 ARGB，但是Jni当中的对应关系是RGBA，每个占4位，不过对于我们的使用情况不影响
        }

        if((info.format == ANDROID_BITMAP_FORMAT_RGBA_8888 && curr_color != (unsigned int)0xFFFFFFFF) ||    // 必须判断非白即黑，而不能判断非黑即白，因为大字机可能得到的颜色是深灰色
           (info.format == ANDROID_BITMAP_FORMAT_RGBA_4444 && curr_color != (unsigned int)0x0000FFFF)) {
            *rbuf_tmp |= vals[rbuf_bit_pos];               // 有黑点的在相应位置黑
            DOTS[head_index]++;                 // 黑点数统计+1
        }

        cbuf_tmp += pixel_step;
        pos++;
        rbuf_bit_pos++;
        if(rbuf_bit_pos == 8) {
            rbuf_tmp++;
            rbuf_bit_pos = 0;
        }
        dot_count++;
        if(TarLines == 0 && dot_count == width / head) {   // 不需要插值点数计数到了头的分解处时，切换头
            dot_count = 0;          // 头内计数清零
            head_index++;           // 头索引+1
            if (head_index == head) {    // 当最后一个头结束时
                head_index = 0;         // 头索引清零
            }
        }
    }
    OrgLines = 0;
    TarLines = 0;

    AndroidBitmap_unlockPixels(env, bmp);

quit:
    jbyteArray result = env->NewByteArray(newSize);
    env->SetByteArrayRegion(result, 0, newSize, rbuf);
    delete[] rbuf;

    return result;
}

/*
 * Class:     com_industry_printer_data_NativeGraphicJni
 * Method:    PasteBmp2Bin
* Signature: ([CLjava/lang/Object;IIIIIIIII)I
 * 函数功能：将传入的Bitmap图做二值化处理，生成供打印使用的bin数据。Bitmap中的一个点，对应于结果中的1位。特别说明：
 *          (1) 传入的Bitmap已经做了旋转镜像，即将视觉正常的Bitmap生成为右上角到左下角，而左下角到右上角。即相当于原始Bitmap以左上角为远点做了顺时针90度旋转后，以y轴为中心左右反转，这样处理的原因是Bitmap的像素保存是
 *              按着先横后纵的方式保存的点阵数据，而生成bin中的数据是按着先纵后横的顺序保存的（这个符合打印的走纸方式），因此如果不对Bitmap的朝向做事先处理，将导致Bitmap和bin总有一个需要跳行，这样会导致频繁的内存调度，
 *              从而导致经常调度内存而大幅降低内存的访问速度
 *          (2) 将原来在apk中通过getPixels函数取得图片数据改为在Jni的函数中获取，这样做的好处是避免了一次内存复制，可以提高访问效率
 *          (3) apk生成的Bitmap使用的ARGB_4444，这个属性在KITKAT以后被废弃了，因此A20的Bitmap还是ARGB_4444，一个Pixel对应2个字节，但是A133系统会强制修改为ARGB_8888，一个Pixel对应4个字节
 *          (4) 与BinarizeBmp函数处理的不同点是，BinarizeBmp传入的Bitmap图是全高的（12.7xn不带插值的保存空间，25.4xn的后部带有插值空间），
 *              而本函数传入的Bitmap不是全高（全高是其特例），并且，都是实际绘图空间，不带有插值空间
 * 函数说明：接收到的图像示意图：（+代表内容，-代表背景空间，o代表空挡）
 * -------- -+++++++ ++++++++ ++------
 * -------- -+++++++ ++++++++ ++------
 * -------- -+++++++ ++++++++ ++------
 * -------- -+++++++ ++++++++ ++------
 * (后续省略)
 * 这个示意图是绘图内容对于全画布背景（即全高）的对应位置，内容的开始位置sY，结束位置eY
 * 这个示意图为原始生成的位图的旋转90后镜像的结果
 * 即：现在看到的图案的右上角为原位图的左下角；左下角为原位图的右上角
 * （这么处理是为了二值化处理时与目标bin同向，防止地址的反复横条，带来CPU额外开销）
 * 【第一步】需要算出插入空档后对应的位置，如图
 * --------oo -+++++++oo ++++++++oo ++------oo
 * --------oo -+++++++oo ++++++++oo ++------oo
 * --------oo -+++++++oo ++++++++oo ++------oo
 * --------oo -+++++++oo ++++++++oo ++------oo
 * 即：根据sY的值，计算前面需要插入多少空挡，算式为：
 *  sY / OrgLines   : 开始位置前应该插入的空挡块的个数
 *  sY / OrgLines * (TarLines - OrgLines)   : 开始位置前应该插入的空挡的个数
 *  sY1 = sY + sY / OrgLines * (TarLines - OrgLines)    : 开始位置对应于插入空档后的位置
 *  sY0 = sY1 / 8 * 8   ; 包含开始位置的字节，这个位置将作为后续贴入时的开始字节
 *  sBitInByte = sY1 % 8    : 包含开始位置的字节中，对应于开始位置的位，这个是二值化生成数据保存的起点，然后，随着逐个点的贴入位置+1，到8后恢复0，形成在字节间，逐位对应于每个图像点的关系
 *  eY / OrgLines   : 结束位置前应该插入的空挡块的个数
 *  eY1 = eY + eY / OrgLines * (TarLines - OrgLines)    : 结束位置对应于插入空档后的位置
 *  eY0 = (eY1 + 7) / 8 * 8   : 包含结束位置的字节的，
 *  eY0 - sY0   : 生成的插值后的bin数据的每行字节数，
 *  needBytes = height * (eY0 - sY0)   : 生成的插值后的bin数据总字节数，根据这个数值申请内存，
 * 【第二步】二值化处理
 *  1. 首先获取needBytes字节的空间，用来保存二值化结果bin数据
 *  2. 先在行内逐点，行结束后跳到下一行，遍历所有的原图像素点（width*height）
 *  3. 对于每一行，Source(x,y) -> sY0[sBitInByte]对应，
 *      如果Source(x,y)==0， 则sY0[sBitInByte]=1， 否则sY0[sBitInByte]=0
 *      然后x++，sBitInByte++，sBitInByte取8的余
 *  4. 当x=OrgLines时，目标区逐位跳过TarLines-OrgLines个位，在此过程中满位后字节加1，位清零，结束后，如果x,y已经大于等于width和height，则结束
 * 【第三步】直接按位贴入目标区
 * 参数说明：
 *  dst：    将bmp二值化后贴入的目标bin数据区，即打印数据的当前底图区
 *  bmp：    已经经过旋转镜像的位图，作为二值化处理的原图
 *  width：  底图区的宽，由于经过了旋转，所以相当于本来图的高
 *  height： 底图区的高，由于经过了旋转，所以相当于本来图的宽，即原图的列数
 *  bytesPerCol：每列的字节数
 *  sX:      位图的行起始点，由于经过了旋转，因此是原图的横轴方向起点，对应于传入位图的纵轴（height）的起始位置
 *  sY:      位图的列起始点，由于经过了旋转，因此是原图的纵轴方向起点，对应于传入位图的横轴（width）的起始位置
 *  sY:      位图的列终止点，由于经过了旋转，因此是原图的纵轴方向终点，对应于传入位图的横轴（width）的终止位置
 *  orgLines：插入空挡的开始位置，如果为0，则表示不插入空挡
 *  tarLines：orgLines大于0时，是插入空挡的结束位置；如果orgLines等于0，没有意义
 *  expandScale：1带多的放大倍数
*/
static jbyte BitVals[8] = {0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, (jbyte)0x80};    // 位点，用于二值化时快速定位为点，避免位移操作（耗时）
static jbyte HeadMask[8] = {0x00, 0x01, 0x03, 0x07, 0x0F, 0x1F, 0x3F, 0x7F};          // 每列数据与上一个变量的边界如果共享字节（即本变量的开始位置不是正好在字节的切割处，而是在字节的中央，则清除自己的内容，保留其它变量的内容的掩码
static jbyte TailMask[8] = {(jbyte)0xFF, 0x7F, 0x3F, 0x1F, 0x0F, 0x07, 0x03, 0x01};   // 每列数据与下一个变量的边界如果共享字节（即本变量的结束位置不是正好在字节的切割处，而是在字节的中央，则清除自己的内容，保留其它变量的内容的掩码

JNIEXPORT jint JNICALL Java_com_industry_printer_data_NativeGraphicJni_PasteBmp2Bin
        (JNIEnv *env, jclass thiz, jcharArray dst, jobject bmp, jint width, jint height, jint bytesPerCol, jint sX, jint sY, jint eY, jint orgLines, jint tarLines, jint expandScale) {
    LOGI("Enter PasteBmp2Bin. Width = %d, Height = %d, BytesPerCol = %d, sX = %d, sY = %d, eY = %d, OrgLines = %d, TarLines = %d, Scale = %d", width, height, bytesPerCol, sX, sY, eY, orgLines, tarLines, expandScale);

    jbyte *dstBuf = (jbyte *)env->GetCharArrayElements(dst, 0);                   // 目标数据区按字节单位访问
    jbyte *rByteBuf;
    jbyte *cByteBuf = NULL;
    jint ret = 0;
    int newWidth, newHeight;

    int sY0 = 0, eY0 = 0, sBitInByte = 0, eBitInByte = 0;       // sY0: 本变量在目标数据区中，在本列的开始字节，可能与上一个变量共享字节
                                                                // sBitInByte: 本变量与下一变量共享发生时，位的开始位置，如果=1，表示本字节的第0位归上一个变量，从第1位开始为本变量使用
                                                                // eY0: 本变量在目标数据区中，在本列的结束字节，可能与下一个变量共享字节
                                                                // eBitInByte: 本变量与下一变量共享发生时，位的结束位置，如果=1，表示本字节的第0位为本变量使用，从第1位开始为下一变量使用
    int sY1 = sY, eY1 = eY;                                     // 过度变量
    if(orgLines > 0 && tarLines > orgLines) {
        int sY1 = sY + (sY / orgLines) * (tarLines - orgLines);
        int eY1 = eY + (eY / orgLines) * (tarLines - orgLines);
    }
    sY0 = sY1 / 8;
    sBitInByte = sY1 % 8;
    eY0 = (eY1 + 7) / 8;
    eBitInByte = eY1 % 8;
    int dstStartPos = sX * bytesPerCol * expandScale + sY0;     // 本变量在目标数据区开始的位置（字节位置），在其后连续(eY0-sY0)字节为本变量的数据区，然后需要跳过bytesPerCol后才能进入本变量的下一列数据区，1带多需要考虑
    int headOutdent = (sBitInByte == 0 ? 0 : 1);                // 每列头部延申入上一个变量的标识
    int tailIndent = (eBitInByte == 0 ? 0 : 1);                 // 每列头部延伸入下一个变量的标识

    int pixel_step;                                             // 缺省按买个像素4个字节来处理（A20的情况下，每个字节需要2个字节，因为apk使用的是RGB4444作为bitmap的配置，这个配置已经在API level 13后被取消了
    unsigned int curr_color;                                        // 每个点的颜色值

    void* pixels;
    AndroidBitmapInfo info;

    // 1. 获取 Bitmap 信息
    if (AndroidBitmap_getInfo(env, bmp, &info) < 0) {
        LOGE("AndroidBitmap_getInfo() failed");
        ret = -1;
        goto quit;
    }

    // 2. 检查格式 (推荐 ARGB_8888)
//    LOGD("info.format = %d", info.format);
    if (info.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
        pixel_step = 4;
    } else if (info.format == ANDROID_BITMAP_FORMAT_RGBA_4444) {
        pixel_step = 2;
    } else {
        LOGE("Unsupported bitmap format %d", info.format);
        ret = -2;
        goto quit;
    }

    // 3. 锁定像素缓冲区
    if (AndroidBitmap_lockPixels(env, bmp, &pixels) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed");
        ret = -3;
        goto quit;
    }

    newHeight = height < info.height + sX ? height - sX : info.height;
    if(orgLines > 0 && tarLines > orgLines) {
        if(width - (width / orgLines) * (tarLines - orgLines) < info.width + sY) {
            newWidth = width - (width / orgLines) * (tarLines - orgLines) - sY;
        } else {
            newWidth = info.width;
        }
    } else {
        if(width < info.width + sY) {
            newWidth = width - sY;
        } else {
            newWidth = info.width;
        }
    }

    LOGE("PasteBmp2Bin sY0 = %d, sBitInByte = %d, eY0 = %d, eBitInByte = %d, dstStartPos = %d", sY0, sBitInByte, eY0, eBitInByte, dstStartPos);
    rByteBuf = dstBuf + dstStartPos;        // 在目标数据去，指向本变量的开始位置（字节）的指针（从sY0开始），没完成一行的操作后，跳到下一列的相同位置
    for(int row=0; row < newHeight; row++, rByteBuf += bytesPerCol * expandScale) {
        int pos = sY;
        int rbuf_bit_pos = sBitInByte;
        jbyte *rBuf = rByteBuf;         // 实际进行访问的目标数据区指针

        // 清空本变量的当前列的数据，如果有字节内交叉，则保留上下变量的内容部分
        if(sBitInByte != 0) *rBuf &= HeadMask[sBitInByte];
        if(eBitInByte != 0) *(rBuf+eY0-sY0) &= TailMask[eBitInByte];
        memset(rBuf+headOutdent, 0x00, (eY0 - tailIndent) - (sY0 + headOutdent));

        cByteBuf = (jbyte *)pixels + row * info.width * pixel_step;
        for(int col=0; col<newWidth; col++) {
            if(orgLines > 0 && tarLines > orgLines) {       // 当需要插值时，跳过需要插值的点位（即逐位跳，同时保证字节也根据必要跳）
                if((pos+col) % orgLines == 0) {
                    while((pos+col) % tarLines != 0) {
                        pos++;
                        rbuf_bit_pos++;
                        if(rbuf_bit_pos == 8) {
                            rBuf++;
                            rbuf_bit_pos = 0;
                        }
                    }
                }
            }

            if(info.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
                curr_color = *((jint *)cByteBuf);       // Java的颜色值位置对应 ARGB，但是Jni当中的对应关系是ABGR，不过对于我们的使用情况不影响
            } else {
                curr_color = *((jchar *)cByteBuf);       // Java的颜色值位置对应 ARGB，但是Jni当中的对应关系是RGBA，每个占4位，不过对于我们的使用情况不影响
            }

            if((info.format == ANDROID_BITMAP_FORMAT_RGBA_8888 && curr_color != (unsigned int)0xFFFFFFFF) ||    // 必须判断非白即黑，而不能判断非黑即白，因为大字机可能得到的颜色是深灰色
               (info.format == ANDROID_BITMAP_FORMAT_RGBA_4444 && curr_color != (unsigned int)0x0000FFFF)) {
                for(int k=0; k<expandScale; k++) {
                    *(rBuf+k*bytesPerCol) |= BitVals[rbuf_bit_pos];               // 有黑点的在相应位置黑
                }
            }

            cByteBuf += pixel_step;
            pos++;
            rbuf_bit_pos++;
            if(rbuf_bit_pos == 8) {
                rBuf++;
                rbuf_bit_pos = 0;
            }
        }
    }

    AndroidBitmap_unlockPixels(env, bmp);

quit:
    env->ReleaseCharArrayElements((jcharArray)dst, (jchar*)dstBuf, 0);

    return ret;
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

// H.M.Wang 2026-8-19 为了提高变量生成的速度，启用开窗的办法贴图，详细参照WORD文档《开创处理修改说明》
/*
 * Class:     com_industry_printer_data_NativeGraphicJni
 * Method:    GetBgBufferNew
 * Function:  主要是给12.7x你的打印头展开每个头的最后一个字节，并且合并了1带多的复制数据
 * Parameters:
 *      src:        原始位图数据缓冲区，一个bit代表一个dot;
 *      length:     缓冲区总长度;
 *      bytesFeed:  目标缓冲区每列的数据长度（因为有不同打印头之间的缝隙，要比实际数据长）
 *      bytesPerHFeed：目标缓冲区每个打印头的数据长度
 *      bytesPerH： 原始缓冲区每个头侧数据长度
 *      column：    数据总列数
 *      type：      打印头的数量
 *      expandScale：一带多情况下的复制次数
 * Signature: ([BIIIIII})[C
 */
JNIEXPORT jcharArray JNICALL Java_com_industry_printer_data_NativeGraphicJni_GetBgBufferNew
        (JNIEnv *env, jclass thiz, jbyteArray src, jint length, jint bytesFeed, jint bytesPerHFeed, jint bytesPerH, jint column, jint type, jint expandScale) {
    LOGD("GetBgBufferNew length=%d, bytesFeed=%d, bytesPerHFeed=%d, bytesPerH=%d, column=%d, type=%d, expandScale=%d", length, bytesFeed, bytesPerHFeed, bytesPerH, column, type, expandScale);

    jbyte *cbuf = env->GetByteArrayElements(src, 0);
    jbyte *rByteBuf = NULL;
//    jchar *rCharBuf = NULL;

    // 当每个头需要的DOT数据字节数大于每个头实际拥有的DOT字节数（12.7xn喷头时，每个头的实际数据为152点，19个字节，但打印缓冲区每个头必须为20字节，因此会出现这个需要补齐的情况）
    // 当1带多需要展开的时候，也需要单独复制内容
    if(expandScale > 1) {           // 当需要1带多复制1到多时的处理
        rByteBuf = new jbyte[length*expandScale];       // 获取足够打的内存空间
        memset(rByteBuf, 0x00, length);             // 内存控件清零

        size_t orgPointer = 0;
        for(int i=0; i<column; i++) {               // 对于所有的列
            for(int k=0; k<expandScale; k++) {      // 对于1带多需要复制次数
                if(bytesPerHFeed > bytesPerH) {     // 如果目标数据区与实际数据区的大小不同（特指12.7xn打印头的情况，需要每个头末尾增加一个字节，实际上bytesPerHFeed就等于bytesPerH+1）
                    for (int j=0; j<type; j++) {    // 每个头的数据进行复制，每次复制目标区为跳过bytesPerHFeed字节，复制bytesPerH字节数据，自然就会空出最后一个字节
                        jint pos = i * expandScale * bytesFeed + k * bytesFeed + j * bytesPerHFeed;
                        memcpy(rByteBuf + pos, cbuf + orgPointer, bytesPerH);
                        orgPointer += bytesPerH;
                    }
                } else {                            // 目标区与原数据区每个头的数据字节数相同，即其它种类的打印头，不需要末尾插一个空字节，就直接一列完整复制
                    jint pos = i * expandScale * bytesFeed + k * bytesFeed;
                    memcpy(rByteBuf + pos, cbuf + orgPointer, bytesFeed);
                    orgPointer += bytesFeed;
                }
            }
        }
    } else if(bytesPerHFeed > bytesPerH) {          // 不是1带多的情况下，如果时12.7xn打印头，同样需要逐个打印头复制
        rByteBuf = new jbyte[length];
        memset(rByteBuf, 0x00, length);

        size_t orgPointer = 0;
        for(int i=0; i < column; i++) {
            for (int j = 0; j < type; j++) {
                jint pos = i * bytesFeed + j * bytesPerHFeed;
                memcpy(rByteBuf + pos, cbuf + orgPointer, bytesPerH);
                orgPointer += bytesPerH;
            }
        }
    } else {     // 既不是1带多，也不是12.7打印头的情况下，原数据区可以直接使用
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
 * Method:    PasteDynamicBin (已弃用)
 * 功能：     将给定的数字串，从refDigitsBin中保存的对应于0-9数字的bin提取相应的bin数据，贴入到目标数据区的相应位置中。其中：
 *            横向的开始位置为sX，纵向的开始位置为sY，结束位置为eY；
 *            如果有1带多的参数，则重复复制到对应的重复位置中；
 *            如果需要清空前置零，则前置零处贴入空白
 * Parameters:
 *      dst:        目标打印缓冲区;
 *      src:        待插入原bin数据区
 *      bytesPerCol：待插入bin数据区的每列字节数，对应于数据的全高，乘以expandScale后，会对应于目标打印缓冲区的全高
 *      columns：    待插入bin数据区的列数
 *      sX：目标区开始贴图的开始列
 *      sY：从参考bin数据区取数据的纵向开始字节位置，同时也是贴入目标区的纵向开始字节位置，需要1带多的情况下，每次增加bytesPerColInBase即为复制去的纵向位置
 *      eY：从参考bin数据区取数据的纵向结束字节位置，同时也是贴入目标区的纵向结束字节位置，需要1带多的情况下，每次增加bytesPerColInBase即为复制去的纵向位置
 *      expandScale：1带多的放大倍数
 * Signature: ([C[BIIIIII})I
 */
JNIEXPORT jint JNICALL Java_com_industry_printer_data_NativeGraphicJni_PasteDynamicBin
        (JNIEnv *env, jclass thiz, jcharArray dst, jbyteArray src, jint bytesPerCol, jint columns, jint sX, jint sY, jint eY, jint expandScale) {
    LOGD("PasteDynamicBin bytesPerCol=%d, columns=%d, sX=%d, sY=%d, eY=%d, expandScale=%d", bytesPerCol, columns, sX, sY, eY, expandScale);

    jbyte *rByteBuf = (jbyte *)env->GetCharArrayElements(dst, 0);                   // 目标数据区按字节单位访问
    jbyte *cByteBuf = (jbyte *)env->GetByteArrayElements(src, 0);                   // 待插入bin数据区按字节单位访问

    // 对于原数据的每一列，将原数据从开始位置到结束位置之间的字节复制到目标位置的所有对应列的相应位置中
    for(int i=0, desCol=sX * bytesPerCol * expandScale, srcCol=0; i<columns; i++, desCol += bytesPerCol * expandScale, srcCol += bytesPerCol) {
        for(int k=0; k<expandScale; k++) {      // 填充到所有的1带多复制区中
            memcpy(rByteBuf + desCol + k * bytesPerCol + sY, cByteBuf + srcCol + sY, eY - sY);
        }
    }

    env->ReleaseCharArrayElements((jcharArray)dst, (jchar*)rByteBuf, 0);
    env->ReleaseByteArrayElements((jbyteArray)src, (jbyte*)cByteBuf, 0);

    return 0;
}

/*
 * Class:     com_industry_printer_data_NativeGraphicJni
 * Method:    PasteVarByVBin
 * 功能：     将给定的数字串，从refDigitsBin中保存的对应于0-9数字的bin提取相应的bin数据，贴入到目标数据区的相应位置中。其中：
 *            横向的开始位置为sX，纵向的开始位置为sY，结束位置为eY；
 *            如果有1带多的参数，则重复复制到对应的重复位置中；
 *            如果需要清空前置零，则前置零处贴入空白
 * Parameters:
 *      dst:        目标打印缓冲区;
 *      digits:     需要变换为bin数据贴到目标打印缓冲区的数字串;
 *      refDigitsBin:  对应于0-9数字的参考bin数据区
 *      bytesPerColInBase： 参考bin数据区的每列字节数，对应于数据的全高，乘以expandScale后，会对应于目标打印缓冲区的全高
 *      colPerElements：    对应于每个数字的参考bin数据的列数
 *      clearLeadingZero：  清除前置零标识
 *      sX：目标区开始贴图的开始列
 *      sY：从参考bin数据区取数据的纵向开始字节位置，同时也是贴入目标区的纵向开始字节位置，需要1带多的情况下，每次增加bytesPerColInBase即为复制去的纵向位置
 *      eY：从参考bin数据区取数据的纵向结束字节位置，同时也是贴入目标区的纵向结束字节位置，需要1带多的情况下，每次增加bytesPerColInBase即为复制去的纵向位置
 *      expandScale：1带多的放大倍数
 * Signature: ([CI[I[CIIIIII})I
 */
JNIEXPORT jint JNICALL Java_com_industry_printer_data_NativeGraphicJni_PasteVarByVBin
        (JNIEnv *env, jclass thiz, jcharArray dst, jint columns, jintArray digits, jcharArray refDigitsBin, jint bytesPerColInRef, jint colPerElements, jint sX, jint sY, jint eY, jint expandScale) {
    LOGD("PasteVarByVBin columns = %d, bytesPerColInRef=%d, colPerElements=%d, sX=%d, sY=%d, eY=%d, expandScale=%d", columns, bytesPerColInRef, colPerElements, sX, sY, eY, expandScale);

    jbyte *rByteBuf = (jbyte *)env->GetCharArrayElements(dst, 0);                   // 目标数据区按字节单位访问
    jint *digitsBuf = env->GetIntArrayElements(digits, 0);                          // 待填bin的数字串
    jsize len = env->GetArrayLength(digits);
    jbyte *refByteBuf = (jbyte *)env->GetCharArrayElements(refDigitsBin, 0);       // 参考bin数据区按字节单位访问

    int startCol = sX * bytesPerColInRef * expandScale;                                     // 目标数据区中横向开始填充的位置（列）对应的字节数
    int bytesPerDigit = colPerElements * bytesPerColInRef * expandScale;                    // 目标数据区中，对应于每个数字的填充区（通高）的字节数

    for(int i=0, startDigitCol=startCol; i<len; i++, startDigitCol += bytesPerDigit) {     // 对于每个数字串中数字，每次操作跳过每个数字的填充区（通高）的字节数
        if(*(digitsBuf+i) >=0 && *(digitsBuf+i) <=9) {          // 如果数字为0-9则填充实际数值对应的bin，否则清空填充区
            for(int j=0, perColStart=0, refColStart=(*(digitsBuf+i))*colPerElements*bytesPerColInRef; j<colPerElements; j++, perColStart += bytesPerColInRef * expandScale) {       // 对于某个数字的所有需填充数据列
                if(sX + i*colPerElements + j >= columns) break;
                for(int k=0; k<expandScale; k++) {      // 填充到所有的1带多复制区中
                    memcpy(rByteBuf + startDigitCol + perColStart + k * bytesPerColInRef + sY, refByteBuf + refColStart + j * bytesPerColInRef + sY, eY - sY);
                }
            }
        } else {
            for(int j=0; j<colPerElements; j++) {
                if(sX + i*colPerElements + j >= columns) break;
                for(int k=0; k<expandScale; k++) {
                    memset(rByteBuf + startDigitCol + j * bytesPerColInRef * expandScale + k * bytesPerColInRef + sY, 0x00, eY - sY);
                }
            }
        }
    }

    env->ReleaseCharArrayElements((jcharArray)dst, (jchar*)rByteBuf, 0);
    env->ReleaseIntArrayElements(digits, digitsBuf, 0);
    env->ReleaseCharArrayElements(refDigitsBin, (jchar*)refByteBuf, 0);

    return 0;
}
// End of H.M.Wang 2026-8-19 为了提高变量生成的速度，启用开窗的办法贴图，详细参照WORD文档《开创处理修改说明》

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

// 2026-9-1 1.0.16
// 增加PasteBmp2Bin函数，用来实现将Bitmap位图直接二值化并且插入打印数据bin数据区
// 2026-8-26 1.0.15
//    1. 增加GetBgBufferNew函数，与GetBgBuffer函数功能一样，只是在此函数中直接复制1带多的部分，可以避免在BinInfo中再次在apk代码中进行复制，从而提高效率
//    2. 完善BinarizeBmp函数，增加1带多的复制，并且增加对12.7xn打印头插空的处理（通过调用apk中调用extract是的needShift参数指定，保存1.bin和vbin的时候指定false，保持保存的文件不被插空，以便与以前版本兼容。当生成动态内容是指定true，进行插值
//       这样可以避免再次在apk中进行插值，或者再次调用GetBgBufferNew进行插值，以便提高效率
//    3. 增加PasteDynamicBin函数，用来向背景板中粘贴动态生成的内容的bin，避免在apk中粘贴，提高效率。bin数据暂时是全高的，只是粘贴时仅取得需要的高度粘贴。下一步生成必要高度的bin，这样可以节省生成bin的时间（修改BinarizeBmp）
//    4. 增加PasteVarByVBin函数，用来向背景板中粘贴从根据0-9数字，从vbin中提取内容粘贴到背景板中，避免在apk中粘贴，提高效率。vbin仍然为全高的内容，只是粘贴时仅取得需要的高度粘贴
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
    LOGI("NativeGraphicJni.so 1.0.16 Loaded.");
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