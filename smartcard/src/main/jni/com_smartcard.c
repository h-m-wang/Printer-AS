//
// Created by kevin on 2019/2/17.
//

#include <stdio.h>
#include <jni.h>
#include <src/sc_ink_mem_access.h>
#include <src/sc_common_mem_access.h>
#include <src/sc_supply_mem_access.h>
#include <malloc.h>
#include <drivers/internal_ifc/sc_gpio_driver.h>
#include <unistd.h>
#include <pthread.h>
#include <drivers/internal_ifc/sc_i2c_driver.h>
#include <hp_debug_log_internal.h>

#include "hp_host_smart_card.h"
#include "drivers/internal_ifc/hp_smart_card_gpio_ifc.h"
#include "common_log.h"
#include "com_smartcard.h"
#include "drivers/internal_ifc/sc_gpio_adapter.h"
#include "src/level_memory_access.h"

#ifdef __cplusplus
extern "C"
{
#endif

#define VERSION_CODE                            "1.0.408"
// 1.0.408 2024-11-20
// 所有的I2C访问，完成后均将I2C修正到BULK1(IDS)
// 1.0.407 2024-11-19
// 读完Level后，将I2C切换到墨袋上
// 1.0.406 2024-11-5
// 借用SmartCard的I2C通道实现A133平台的RTC计数器读取（A20的时候是使用/sys/class/device_of_i2c通道实现的）
// 1.0.405 2024-10-30
// 完善9555A的读写试验
// 1.0.404 2024-10-28
// 增加9555A的读写试验，速录在100k和200k，每次读写500次，读写结果输出log。切换速录需要切换img
// 1.0.403 2024-9-6
// 将 MaxBagInkVolume, MaxPenInkVolume, InkVolOfBagPercentage, InkVolOfPenPercentage的计算，移到一个新的函数initVolumeParams中，并且，该函数在adjustLocalInkValue函数的前面调用，
// 因为，这些参数会在adjustLocalInkValue中参与计算。原来这些参数是在getMaxVolume函数中被计算，但是getMaxVolume的调用使用apk发起的，晚于adjustLocalInkValue的调用，
// 这会导致adjustLocalInkValue第一次被调用的时候，上述4个参数使用缺省值，但是在getMaxVolume被调用后，上述4个参数发生变化，再次初始化时，调用adjustLocalInkValue时，上述4个参数发生变化，而使得内部锁值被改写，apk会看到锁值发生大幅度的变化
// (但是，即使这次修改，如果PEN1和PEN2都是用的话，如果两个的drop_volume的值不同，一个在20-35区间，另外一个不在20-35区间，也会导致上述四个参数横跳)
// 1.0.402 2024-9-5
// readLevel函数中，重启的条件修改为，根据从参数获得最大值和最小值，如果超出100单位则重启（这个最大最小值对应于apk的标准情况下的最大最小值，或者时自由天线小卡的最大最小值）
// 1.0.401 2024-8-30
// 1. 调整Java_com_Smartcard_testLevel的读取逻辑，读取5次后，充值Level芯片
// 2. 调整Java_com_Smartcard_readLevel的重启条件，原来只有0x0FFFFFFF，扩大到0x00000000也重启。再次修改为138加减100以外的值时重启
// 1.0.400 2024-8-16
// 修改sLevelChipType的定义，原来是只考虑了一个头，现在是4个头动作，因此需要通过数组进行分别管理
// 1.0.399 2024-8-6
//   增加一个判断Level测量芯片种类的函数，apk会根据不同的芯片种类，执行不同的逻辑。读取Level值也会根据不同的种类而调用不同的接口
// 1.0.398 2024-7-4 暂时追加MCP-H21-xxxxx芯片的读压力值功能
// 1.0.397 FSR=4.096V
// 1.0.396 1115的地址修改为0x49
// 1.0.395 临时追加一个DAC5571的设置功能
// 1.0.394 临时追加一个ADS1115芯片的读数功能
// 1.0.393 临时取消对ILG的修改（已恢复）
// 1.0.392
//    (1) 增加Java_com_Smartcard_readHX24LC中，读取数据SC_I2C_DRIVER_read之前，写入WORD地址
//          SC_I2C_DRIVER_write(0x01, HX24LC_I2C_ADDRESS, 0, &data, 1);
//    (2) Java_com_Smartcard_writeHX24LC中写入函数的参数错误修正
//    误: write_length = SC_I2C_DRIVER_write(0x01, HX24LC_I2C_ADDRESS, 0, &data, 2);
//    正: write_length = SC_I2C_DRIVER_write(0x01, HX24LC_I2C_ADDRESS, 0, data, 2);

// 1.0.391
//    (1) Java_com_Smartcard_readHX24LC增加一个读数的log输出
//    (2) Java_com_Smartcard_readHX24LC及Java_com_Smartcard_writeHX24LC函数中，硬件操作加mutex保护
// 1.0.390
//   追加一个读写HX24LC芯片的功能，用来保存对应Bagink墨位的调整值
//      {"readHX24LC",	    	    "()I",						(void *)Java_com_Smartcard_readHX24LC},
//      {"writeHX24LC",	    	    "(I)I",						(void *)Java_com_Smartcard_writeHX24LC},
// 1.0.389
//   追加两个为Bagink专用的API，Java_com_Smartcard_init_level_direct 和 Java_com_Smartcard_readLevelDirect
// 1.0.388
//   (1) initComponent函数中，对于adjustLocalInkValue函数的调用，原来传递的参数错误，没有起到调整的作用
//   (2) 为adjustLocalInkValue函数和getMaxVolume函数添加log
// 1.0.387
//   (1) 在shutdown函数中，增加排斥锁的使用，在获取锁后才shutdown
// 1.0.386
//   (1) 暂时取消重新起振的尝试操作，改为每读5次关闭一次，再关闭的状态读，下一次读再打开
// 1.0.385
//   (1) 重复起振的方法从睡眠-等一会儿-苏醒，改为重启设备-等一会儿-苏醒
// 1.0.384
//   (1) 重复起振的判断逻辑由0xFFFFFFFF改为0x0FFFFFFF
//              if((chData & 0x0FFFFFFF) == 0x0FFFFFFF) {
// 1.0.383
//   (1) readLevel得到的是FF的时候，重启Level，并且在apk里面beep声音提示
//   (2) testLevel得到的是FF的时候，也重启Level
// 1.0.382
//   (1) 测试状态时，50ms改为硬性的循环语句 改为循环100万次
// 1.0.381
//   (1) 测试状态时，进入睡眠50ms，开机后50ms后读数
// 1.0.380
//   (1) 进入睡眠状态后等待100ms
//   (2) 增加设置几个参数, Set Full Current Mode, RP Override Enable, Disable Automatic Amplitude Correction, Set High Current Drive
// 1.0.379
//   (1) 取消1.0.378修改内容
//   (2) 追加testLevel函数，在读取Level值之前关闭Level5毫秒后，重新打开，然后读Level值
// 1.0.378
//   readDeviceID函数内，执行一次重启一次Level设备

#define SC_SUCCESS                              0
#define SC_INIT_HOST_CARD_NOT_PRESENT           100
#define SC_INIT_PRNT_CTRG_NOT_PRESENT           110
#define SC_INIT_BULK_CTRG_NOT_PRESENT           111
#define SC_INIT_PRNT_CTRG_INIT_FAILED           120
#define SC_INIT_BULK_CTRG_INIT_FAILED           121
#define SC_PRINT_CTRG_ACCESS_FAILED             200
#define SC_BULK_CTRG_ACCESS_FAILED              201
#define SC_LEVEL_CENSOR_ACCESS_FAILED           202
#define SC_CONSISTENCY_FAILED                   300
#define SC_CHECKSUM_FAILED                      400

#define PEN_VS_BAG_RATIO                        3
#define MAX_BAG_INK_VOLUME_MAXIMUM              4700
#define MAX_BAG_INK_VOLUME_MINIMUM              3150
#define MAX_PEN_INK_VOLUME                      MAX_BAG_INK_VOLUME_MINIMUM * PEN_VS_BAG_RATIO
#define INK_VOL_OF_BAG_PERCENTAGE               (MAX_BAG_INK_VOLUME_MINIMUM / 100)
#define INK_VOL_OF_PEN_PERCENTAGE               (MAX_PEN_INK_VOLUME / 100)

static int MaxBagInkVolume                      = MAX_BAG_INK_VOLUME_MINIMUM;
static int MaxPenInkVolume                      = MAX_PEN_INK_VOLUME;
static int InkVolOfBagPercentage                = INK_VOL_OF_BAG_PERCENTAGE;
static int InkVolOfPenPercentage                = INK_VOL_OF_PEN_PERCENTAGE;

static pthread_mutex_t mutex;

//#define DATA_SEPERATER                          100000      // 这之上是墨盒的减记次数（减记300次），这之下是墨盒/墨袋的减锁次数(MAX_INK_VOLUME)，

HP_SMART_CARD_result_t (*inkILGWriteFunc[4])(HP_SMART_CARD_device_id_t cardId, uint32_t ilg_bit) = {
        inkWriteTag9ILGBit01To25,
        inkWriteTag9ILGBit26To50,
        inkWriteTag9ILGBit51To75,
        inkWriteTag9ILGBit76To100
};

HP_SMART_CARD_result_t (*inkILGReadFunc[4])(HP_SMART_CARD_device_id_t cardId, uint32_t *ilg_bit) = {
        inkReadTag9ILGBit01To25,
        inkReadTag9ILGBit26To50,
        inkReadTag9ILGBit51To75,
        inkReadTag9ILGBit76To100
};

HP_SMART_CARD_result_t (*supplyILGWriteFunc[4])(HP_SMART_CARD_device_id_t cardId, uint32_t ilg_bit) = {
        supplyWriteTag9ILGBit01To25,
        supplyWriteTag9ILGBit26To50,
        supplyWriteTag9ILGBit51To75,
        supplyWriteTag9ILGBit76To100
};

HP_SMART_CARD_result_t (*supplyILGReadFunc[4])(HP_SMART_CARD_device_id_t cardId, uint32_t *ilg_bit) = {
        supplyReadTag9ILGBit01To25,
        supplyReadTag9ILGBit26To50,
        supplyReadTag9ILGBit51To75,
        supplyReadTag9ILGBit76To100
};

static void adjustLocalInkValue(jint card);
static void initVolumeParams(jint card);

static char* toBinaryString(char* dst, uint32_t src) {
    uint32_t mask = 0x01000000;
    for(int i=0; i<25; i++) {
        if(mask & src) {
            dst[i] = '1';
        } else {
            dst[i] = '0';
        }
        mask >>= 1;
    }
    dst[25] = 0x00;
    return dst;
}

void print_returns(HP_SMART_CARD_result_t result)
{
    HW_SMART_CARD_status_t status = LIB_HP_SMART_CARD_last_status();
    LOGD("Result = %s (%d)  Status = %s (%d)\n",
           LIB_HP_SMART_CARD_result_string(result), result,
           LIB_HP_SMART_CARD_status_string(status), status);
}

void assert_handler(const char *error_str)
{
    LOGD("=========================================\n");
    LOGD("Smartcard Main: HP_ASSERT Failed\n");
    LOGD("%s\n", error_str);
    LOGD("=========================================\n");
}

void cache_monitor_failure_handler(HP_SMART_CARD_device_id_t dev_id,
                                   HP_SMART_CARD_result_t result)
{
    LOGD("=========================================\n");
    LOGD("Smartcard Main: Cache monitor failure\n");
    LOGD("Device Id = %d, ", dev_id);
    print_returns(result);
    LOGD("=========================================\n");
}

JNIEXPORT jint JNICALL Java_com_Smartcard_shutdown(JNIEnv *env, jclass arg) {
    LOGI("Shutting down smart card library....\n");

    pthread_mutex_lock(&mutex);
// H.M.Wang 2023-6-18 这里不关闭库，否则再次访问时会因为未初始化而失败
//    LIB_HP_SMART_CARD_shutdown();
// End of H.M.Wang 2023-6-18 这里不关闭库，否则再次访问时会因为未初始化而失败

    pthread_mutex_unlock(&mutex);
// H.M.Wang 2023-6-18 初始化因为已到了library的初始化，因此这里释放太早了
//    pthread_mutex_destroy(&mutex);
// End of H.M.Wang 2023-6-18 初始化因为已到了library的初始化，因此这里释放太早了

    return SC_SUCCESS;
}

// imgtype==1 : M5, M7, M9 系列; 否则为非M5, M7, M9系列。GPIO的管脚使用不同
JNIEXPORT jint JNICALL Java_com_Smartcard_exist(JNIEnv *env, jclass arg, jint imgtype) {
    LOGI("Checking smart card existence....\n");

    HP_SMART_CARD_gpio_init();
    HP_SMART_CARD_i2c_init();

    LIB_HP_SMART_CARD_init();

    gMImgType = imgtype;
    LOGE(">>> （M9,M7,M5) Image Type? : %d.", gMImgType);

    if (HP_SMART_CARD_OK != LIB_HP_SMART_CARD_device_present(HP_SMART_CARD_DEVICE_HOST)) {
        LOGE(">>> LIB_HP_SMART_CARD_device_present(HP_SMART_CARD_DEVICE_HOST): NOT PRESENT.  ");
        return SC_INIT_HOST_CARD_NOT_PRESENT;
    }

    return SC_SUCCESS;
}

/*
 * 初始化HP智能卡设备，包括HOST卡，COMPONENT卡以及LEVEL
 */
JNIEXPORT jint JNICALL Java_com_Smartcard_init(JNIEnv *env, jclass arg) {
    LOGI("Initializing smart card library....%s\n", VERSION_CODE);
// H.M.Wang 2023-6-18 将mutex的初始化移到laborary初始化的地方。在这里处理的话，要在shutdown的地方释放，这样导致设备启动以后，如果没有插卡，后续插卡后无法识别。
// 原因是，开机后初始化处理以后，无论成功还是失败，都会先关闭该库，只从shutdown，在shutdown中会释放mutex，导致后续，只要不开始打印，就不能访问该库的功能了，因此即使中途插入卡，也无法识别
//    if (pthread_mutex_init(&mutex, NULL) != 0){
//        return -1;
//    }
// End of H.M.Wang 2023-6-18 将mutex的初始化移到laborary初始化的地方。

    pthread_mutex_lock(&mutex);

    HP_SMART_CARD_gpio_init();
    HP_SMART_CARD_i2c_init();

    LIB_HP_SMART_CARD_init();

    if (HP_SMART_CARD_OK != LIB_HP_SMART_CARD_device_present(HP_SMART_CARD_DEVICE_HOST)) {
        LOGE(">>> LIB_HP_SMART_CARD_device_present(HP_SMART_CARD_DEVICE_HOST): NOT PRESENT.  ");
        pthread_mutex_unlock(&mutex);
        return SC_INIT_HOST_CARD_NOT_PRESENT;
    }

    pthread_mutex_unlock(&mutex);

    return SC_SUCCESS;
}

extern char *inkFamilyGetFiledName(uint8_t id);

// H.M.Wang 2022-11-1 Add this API for Bagink Use
JNIEXPORT jint JNICALL Java_com_Smartcard_init_level_direct(JNIEnv *env, jclass arg ) {
    LOGI("Initializing level direct. for BAGINK use.\n");

    uint16_t config;

    if(LEVEL_I2C_OK != readConfig(&config)) {
        pthread_mutex_unlock(&mutex);
        return SC_LEVEL_CENSOR_ACCESS_FAILED;
    }
    LOGD(">>> Read config for [BAGINK]: 0x%04X", config);

    config &= CONFIG_ACTIVE_MODE_ENABLE;                // Set to Active mode
    if(LEVEL_I2C_OK != writeConfig(&config)) {
        pthread_mutex_unlock(&mutex);
        return SC_LEVEL_CENSOR_ACCESS_FAILED;
    }
    LOGD(">>> Write config for [BAGINK]: 0x%04X", config);

    return SC_SUCCESS;
}
// End of H.M.Wang 2022-11-1 Add this API for Bagink Use

JNIEXPORT jint JNICALL Java_com_Smartcard_init_comp(JNIEnv *env, jclass arg, jint card ) {
    LOGI("Initializing smart card component...%d\n", card);

    pthread_mutex_lock(&mutex);

    uint8_t family_id;

    if(CARD_SELECT_PEN1 == card) {
        // Initialize Smart Card 0, this should be a print cartridge
        if (HP_SMART_CARD_OK != LIB_HP_SMART_CARD_device_present(HP_SMART_CARD_DEVICE_PEN1)) {
            LOGE(">>> LIB_HP_SMART_CARD_device_present(%d): NOT PRESENT.  ", HP_SMART_CARD_DEVICE_PEN1);
            pthread_mutex_unlock(&mutex);
            return SC_INIT_PRNT_CTRG_NOT_PRESENT;
        }

        if (HP_SMART_CARD_OK != LIB_HP_SMART_CARD_device_init(HP_SMART_CARD_DEVICE_PEN1)) {
            LOGE(">>> LIB_HP_SMART_CARD_device_init(%d): Initialization Failed.  ", HP_SMART_CARD_DEVICE_PEN1);
            pthread_mutex_unlock(&mutex);
            return SC_INIT_PRNT_CTRG_INIT_FAILED;
        }
// H.M.Wang 2022-9-15 该函数调用的参数传递错误，导致该函数内部没有被执行
//        adjustLocalInkValue(HP_SMART_CARD_DEVICE_PEN1);
        initVolumeParams(card);
        adjustLocalInkValue(card);
// End of H.M.Wang 2022-9-15 该函数调用的参数传递错误，导致该函数内部没有被执行
    } else if(CARD_SELECT_PEN2 == card) {
        // Initialize Smart Card 0, this should be a print cartridge
        if (HP_SMART_CARD_OK != LIB_HP_SMART_CARD_device_present(HP_SMART_CARD_DEVICE_PEN2)) {
            LOGE(">>> LIB_HP_SMART_CARD_device_present(%d): NOT PRESENT.  ", HP_SMART_CARD_DEVICE_PEN2);
            pthread_mutex_unlock(&mutex);
            return SC_INIT_PRNT_CTRG_NOT_PRESENT;
        }

        if (HP_SMART_CARD_OK != LIB_HP_SMART_CARD_device_init(HP_SMART_CARD_DEVICE_PEN2)) {
            LOGE(">>> LIB_HP_SMART_CARD_device_init(%d): Initialization Failed.  ", HP_SMART_CARD_DEVICE_PEN2);
            pthread_mutex_unlock(&mutex);
            return SC_INIT_PRNT_CTRG_INIT_FAILED;
        }

// H.M.Wang 2022-9-15 该函数调用的参数传递错误，导致该函数内部没有被执行
//        adjustLocalInkValue(HP_SMART_CARD_DEVICE_PEN2);
        initVolumeParams(card);
        adjustLocalInkValue(card);
// End of H.M.Wang 2022-9-15 该函数调用的参数传递错误，导致该函数内部没有被执行
    } else if(CARD_SELECT_BULK1 == card || CARD_SELECT_BULKX == card) {
        if(CARD_SELECT_BULKX == card) {     // 2022-4-15 墨盒替代墨袋的时候，显示log的时候使用ink的访问信息，不适用supply的信息。
            FIELD_NAME[HP_SMART_CARD_DEVICE_BULK1] = inkFamilyGetFiledName;
        }

        if (HP_SMART_CARD_OK != LIB_HP_SMART_CARD_device_present(HP_SMART_CARD_DEVICE_BULK1)) {
            LOGE(">>> LIB_HP_SMART_CARD_device_present(%d): NOT PRESENT.  ", HP_SMART_CARD_DEVICE_BULK1);
            pthread_mutex_unlock(&mutex);
            return SC_INIT_BULK_CTRG_NOT_PRESENT;
        }

        if (HP_SMART_CARD_OK != LIB_HP_SMART_CARD_device_init(HP_SMART_CARD_DEVICE_BULK1)) {
            LOGE(">>> LIB_HP_SMART_CARD_device_init(%d): Initialization Failed.  ", HP_SMART_CARD_DEVICE_BULK1);
            pthread_mutex_unlock(&mutex);
            return SC_INIT_BULK_CTRG_INIT_FAILED;
        }

        if (HP_SMART_CARD_OK == readTag0FamilyID(HP_SMART_CARD_DEVICE_BULK1, &family_id)) {
            if((CARD_SELECT_BULKX == card && family_id != 27) || (CARD_SELECT_BULK1 == card && family_id != 28)) {
                LOGE(">>> LIB_HP_SMART_CARD_device_init(%d): FamilyID not match.  ", HP_SMART_CARD_DEVICE_BULK1);
                pthread_mutex_unlock(&mutex);
                return SC_INIT_BULK_CTRG_INIT_FAILED;
            }
        } else {
            LOGE(">>> LIB_HP_SMART_CARD_device_init(%d): FamilyID read error.  ", HP_SMART_CARD_DEVICE_BULK1);
            pthread_mutex_unlock(&mutex);
            return SC_INIT_BULK_CTRG_INIT_FAILED;
        }

// H.M.Wang 2022-9-15 该函数调用的参数传递错误，导致该函数内部没有被执行
//        adjustLocalInkValue(HP_SMART_CARD_DEVICE_BULK1);
        initVolumeParams(card);
        adjustLocalInkValue(card);
// End of H.M.Wang 2022-9-15 该函数调用的参数传递错误，导致该函数内部没有被执行
    } else if(SELECT_LEVEL1 == card) {
        SC_GPIO_ADAPTER_select_device(GPIO_DEVICE_PEN1);

        uint16_t config;

        if(LEVEL_I2C_OK != readConfig(&config)) {
            pthread_mutex_unlock(&mutex);
            return SC_LEVEL_CENSOR_ACCESS_FAILED;
        }
        LOGD(">>> Read config: 0x%04X", config);

        config &= CONFIG_ACTIVE_MODE_ENABLE;                // Set to Active mode
        if(LEVEL_I2C_OK != writeConfig(&config)) {
            pthread_mutex_unlock(&mutex);
            return SC_LEVEL_CENSOR_ACCESS_FAILED;
        }
        LOGD(">>> Write config: 0x%04X", config);
// H.M.Wang 2024-10-19 读完Level后，将I2C切换到墨袋上
        SC_GPIO_ADAPTER_select_device(GPIO_DEVICE_BULK1);
// End of H.M.Wang 2024-10-19 读完Level后，将I2C切换到墨袋上
    } else if(SELECT_LEVEL2 == card) {
        SC_GPIO_ADAPTER_select_device(GPIO_DEVICE_PEN2);

        uint16_t config;

        if(LEVEL_I2C_OK != readConfig(&config)) {
            pthread_mutex_unlock(&mutex);
            return SC_LEVEL_CENSOR_ACCESS_FAILED;
        }
        LOGD(">>> Read config: 0x%04X", config);

        config &= CONFIG_ACTIVE_MODE_ENABLE;                // Set to Active mode
        if(LEVEL_I2C_OK != writeConfig(&config)) {
            pthread_mutex_unlock(&mutex);
            return SC_LEVEL_CENSOR_ACCESS_FAILED;
        }
        LOGD(">>> Write config: 0x%04X", config);
// H.M.Wang 2024-10-19 读完Level后，将I2C切换到墨袋上
        SC_GPIO_ADAPTER_select_device(GPIO_DEVICE_BULK1);
// End of H.M.Wang 2024-10-19 读完Level后，将I2C切换到墨袋上
    }

    pthread_mutex_unlock(&mutex);

    return SC_SUCCESS;
}

static uint32_t calculateCheckSum(jint clientUniqueCode) {
    uint16_t manu_year = 0;
    supplyReadTag1ManufactureYear(HP_SMART_CARD_DEVICE_BULK1, &manu_year);
//    LOGD(">>>> HPSCS_SN_MFG_YEAR = %u", manu_year);

    uint8_t manu_woy = 0;
    supplyReadTag1ManufactureWeekOfYear(HP_SMART_CARD_DEVICE_BULK1, &manu_woy);
//    LOGD(">>>> HPSCS_SN_WEEK = %u", manu_woy);

    uint8_t manu_dow = 0;
    supplyReadTag1ManufactureDayOfWeek(HP_SMART_CARD_DEVICE_BULK1, &manu_dow);
//    LOGD(">>>> HPSCS_SN_DOW = %u", manu_dow);

    uint8_t manu_hod = 0;
    supplyReadTag1ManufactureHourOfDay(HP_SMART_CARD_DEVICE_BULK1, &manu_hod);
//    LOGD(">>>> HPSCS_SN_HOD = %u", manu_hod);

    uint8_t manu_moh = 0;
    supplyReadTag1ManufactureMinuteOfHour(HP_SMART_CARD_DEVICE_BULK1, &manu_moh);
//    LOGD(">>>> HPSCS_SN_MOH = %u", manu_moh);

    uint8_t manu_som = 0;
    supplyReadTag1ManufactureSecondOfMinute(HP_SMART_CARD_DEVICE_BULK1, &manu_som);
//    LOGD(">>>> HPSCS_SN_SOM = %u", manu_som);

    uint16_t fill_year = 0;
    supplyReadTag3CartridgeFillYear(HP_SMART_CARD_DEVICE_BULK1, &fill_year);
//    LOGD(">>>> HPSCS_FILL_YEAR = %u", fill_year);

    uint8_t fill_woy = 0;
    supplyReadTag3CartridgeFillWeekOfYear(HP_SMART_CARD_DEVICE_BULK1, &fill_woy);
//    LOGD(">>>> HPSCS_FILL_WEEK = %u", fill_woy);

    uint8_t fill_dow = 0;
    supplyReadTag3CartridgeFillDayOfWeek(HP_SMART_CARD_DEVICE_BULK1, &fill_dow);
//    LOGD(">>>> HPSCS_FILL_DOW = %u", fill_dow);

    uint8_t fill_hod = 0;
    supplyReadTag3CartridgeFillHourOfDay(HP_SMART_CARD_DEVICE_BULK1, &fill_hod);
//    LOGD(">>>> HPSCS_FILL_HOD = %u", fill_hod);

    uint8_t fill_moh = 0;
    supplyReadTag3CartridgeFillMinuteOfHour(HP_SMART_CARD_DEVICE_BULK1, &fill_moh);
//    LOGD(">>>> HPSCS_FILL_MOH = %u", fill_moh);

    uint8_t fill_som = 0;
    supplyReadTag3CartridgeFillSecondOfMinute(HP_SMART_CARD_DEVICE_BULK1, &fill_som);
//    LOGD(">>>> HPSCS_FILL_SOM = %u", fill_som);

    uint32_t sum = clientUniqueCode;

    sum += manu_year;
    sum += manu_woy;
    sum += manu_dow;
    sum += manu_hod;
    sum += manu_moh;
    sum += manu_som;

    sum *= 10000;

    sum += fill_year;
    sum += fill_woy;
    sum += fill_dow;
    sum += fill_hod;
    sum += fill_moh;
    sum += fill_som;

    uint32_t par = sum % 47;
    uint32_t check_sum = par * 100000000 + sum;
    check_sum ^= 0x55555555;
//    LOGD(">>>> CHECK_SUM = 0x%08X", check_sum);

    return check_sum;
}

/**
 * 写入验证码
 */
JNIEXPORT jint JNICALL Java_com_Smartcard_writeCheckSum(JNIEnv *env, jclass arg, jint card, jint clientUniqueCode) {
    uint32_t check_sum = calculateCheckSum(clientUniqueCode);
    HP_SMART_CARD_result_t result = SC_CHECKSUM_FAILED;

    pthread_mutex_lock(&mutex);

    if(CARD_SELECT_PEN1 == card) {
        result = inkWriteTag12OEMDefRWField2(HP_SMART_CARD_DEVICE_PEN1, check_sum);
    } else if(CARD_SELECT_PEN2 == card) {
        result = inkWriteTag12OEMDefRWField2(HP_SMART_CARD_DEVICE_PEN2, check_sum);
    } else if(CARD_SELECT_BULK1 == card) {
        result = supplyWriteTag12OEMDefRWField2(HP_SMART_CARD_DEVICE_BULK1, check_sum);
    } else if(CARD_SELECT_BULKX == card) {
        result = inkWriteTag12OEMDefRWField2(HP_SMART_CARD_DEVICE_BULK1, check_sum);
    }

    pthread_mutex_unlock(&mutex);

    return result;
}

/**
 * 读取验证码
 */
JNIEXPORT jint JNICALL Java_com_Smartcard_checkSum(JNIEnv *env, jclass arg, jint card, jint clientUniqueCode) {
    uint32_t check_sum = calculateCheckSum(clientUniqueCode);

    uint32_t read_check_sum = -1;
    if(CARD_SELECT_PEN1 == card) {
        if (HP_SMART_CARD_OK != inkReadTag12OEMDefRWField2(HP_SMART_CARD_DEVICE_PEN1, &read_check_sum)) {
            return SC_CHECKSUM_FAILED;
        }
    } else if(CARD_SELECT_PEN2 == card) {
        if (HP_SMART_CARD_OK != inkReadTag12OEMDefRWField2(HP_SMART_CARD_DEVICE_PEN2, &read_check_sum)) {
            return SC_CHECKSUM_FAILED;
        }
    } else if(CARD_SELECT_BULK1 == card) {
        if (HP_SMART_CARD_OK != supplyReadTag12OEMDefRWField2(HP_SMART_CARD_DEVICE_BULK1, &read_check_sum)) {
            return SC_CHECKSUM_FAILED;
        }
    } else if(CARD_SELECT_BULKX == card) {
        if (HP_SMART_CARD_OK != inkReadTag12OEMDefRWField2(HP_SMART_CARD_DEVICE_BULK1, &read_check_sum)) {
            return SC_CHECKSUM_FAILED;
        }
    }

//    LOGD(">>>> READ CHECK_SUM = 0x%08X", read_check_sum);

    if(check_sum != read_check_sum) {
        return SC_CHECKSUM_FAILED;
    }

    return SC_SUCCESS;
}

JNIEXPORT jint JNICALL Java_com_Smartcard_checkConsistency(JNIEnv *env, jclass arg, jint card, jint supply) {
    uint8_t ink_designator, supply_designator;
    uint8_t ink_formulator_id, supply_formulator_id;
    uint8_t ink_family, supply_ink_family;
    uint8_t ink_color_code, supply_ink_color_code;
    uint8_t ink_family_member, supply_ink_family_member;

    if(CARD_SELECT_PEN1 == card) {
        if (HP_SMART_CARD_OK != inkReadTag13OEMInkDesignator(HP_SMART_CARD_DEVICE_PEN1, &ink_designator) ||
            HP_SMART_CARD_OK != inkReadTag4FormulatorID(HP_SMART_CARD_DEVICE_PEN1, &ink_formulator_id) ||
            HP_SMART_CARD_OK != inkReadTag4InkFamily(HP_SMART_CARD_DEVICE_PEN1, &ink_family) ||
            HP_SMART_CARD_OK != inkReadTag4ColorCodesGeneral(HP_SMART_CARD_DEVICE_PEN1, &ink_color_code) ||
            HP_SMART_CARD_OK != inkReadTag4InkFamilyMember(HP_SMART_CARD_DEVICE_PEN1, &ink_family_member)) {
            return SC_PRINT_CTRG_ACCESS_FAILED;
        }
    } else if(CARD_SELECT_PEN2 == card) {
        if (HP_SMART_CARD_OK != inkReadTag13OEMInkDesignator(HP_SMART_CARD_DEVICE_PEN2, &ink_designator) ||
            HP_SMART_CARD_OK != inkReadTag4FormulatorID(HP_SMART_CARD_DEVICE_PEN2, &ink_formulator_id) ||
            HP_SMART_CARD_OK != inkReadTag4InkFamily(HP_SMART_CARD_DEVICE_PEN2, &ink_family) ||
            HP_SMART_CARD_OK != inkReadTag4ColorCodesGeneral(HP_SMART_CARD_DEVICE_PEN2, &ink_color_code) ||
            HP_SMART_CARD_OK != inkReadTag4InkFamilyMember(HP_SMART_CARD_DEVICE_PEN2, &ink_family_member)) {
            return SC_PRINT_CTRG_ACCESS_FAILED;
        }
    } else {
        return SC_SUCCESS;
    }

    if(CARD_SELECT_BULK1 == supply) {
        if (HP_SMART_CARD_OK != supplyReadTag1OEMInkDesignator(HP_SMART_CARD_DEVICE_BULK1, &supply_designator) ||
            HP_SMART_CARD_OK != supplyReadTag4FormulatorID(HP_SMART_CARD_DEVICE_BULK1, &supply_formulator_id) ||
            HP_SMART_CARD_OK != supplyReadTag4InkFamily(HP_SMART_CARD_DEVICE_BULK1, &supply_ink_family) ||
            HP_SMART_CARD_OK != supplyReadTag4ColorCodesGeneral(HP_SMART_CARD_DEVICE_BULK1, &supply_ink_color_code) ||
            HP_SMART_CARD_OK != supplyReadTag4InkFamilyMember(HP_SMART_CARD_DEVICE_BULK1, &supply_ink_family_member)) {
            return SC_BULK_CTRG_ACCESS_FAILED;
        }
    } else if(CARD_SELECT_BULKX == supply) {
        if (HP_SMART_CARD_OK != inkReadTag13OEMInkDesignator(HP_SMART_CARD_DEVICE_BULK1, &ink_designator) ||
            HP_SMART_CARD_OK != inkReadTag4FormulatorID(HP_SMART_CARD_DEVICE_BULK1, &ink_formulator_id) ||
            HP_SMART_CARD_OK != inkReadTag4InkFamily(HP_SMART_CARD_DEVICE_BULK1, &ink_family) ||
            HP_SMART_CARD_OK != inkReadTag4ColorCodesGeneral(HP_SMART_CARD_DEVICE_BULK1, &ink_color_code) ||
            HP_SMART_CARD_OK != inkReadTag4InkFamilyMember(HP_SMART_CARD_DEVICE_BULK1, &ink_family_member)) {
            return SC_PRINT_CTRG_ACCESS_FAILED;
        }
    } else {
        return SC_SUCCESS;
    }

    if(ink_designator != supply_designator ||
       ink_formulator_id != supply_formulator_id ||
       ink_family != supply_ink_family ||
       ink_color_code != supply_ink_color_code ||
       ink_family_member != supply_ink_family_member) {
        return SC_CONSISTENCY_FAILED;
    }

    return SC_SUCCESS;
}

static void initVolumeParams(jint card) {
    uint8_t drop_volume = 29;

    if(CARD_SELECT_PEN1 == card) {
        inkReadTag5DropVolume(HP_SMART_CARD_DEVICE_PEN1, &drop_volume);
    } else if(CARD_SELECT_PEN2 == card) {
        inkReadTag5DropVolume(HP_SMART_CARD_DEVICE_PEN2, &drop_volume);
    } else {
        LOGD(">>> initVolumeParams::getMaxVolume(#%d) = %d (pre-calculated)", card, MaxBagInkVolume);
        return;
    }

    // bulk 计算
    // DV 29 对应3150
    // DV x=20-35 ：   3150*(29/X)
    // DV x < 20  or  X>35:    4700

    if(drop_volume < 20 || drop_volume > 35) {
        MaxBagInkVolume = MAX_BAG_INK_VOLUME_MAXIMUM;
    } else {
        MaxBagInkVolume = MAX_BAG_INK_VOLUME_MINIMUM * 29 / drop_volume;
    }

    MaxPenInkVolume                      = MaxBagInkVolume * PEN_VS_BAG_RATIO;
    InkVolOfBagPercentage                = MaxBagInkVolume / 100;
    InkVolOfPenPercentage                = MaxPenInkVolume / 100;

    LOGD(">>> initVolumeParams::getMaxVolume(#%d) = %d", card, MaxBagInkVolume);
}

JNIEXPORT int JNICALL Java_com_Smartcard_getMaxVolume(JNIEnv *env, jclass arg, jint card) {
/*    uint8_t drop_volume = 29;

    if(CARD_SELECT_PEN1 == card) {
        inkReadTag5DropVolume(HP_SMART_CARD_DEVICE_PEN1, &drop_volume);
    } else if(CARD_SELECT_PEN2 == card) {
        inkReadTag5DropVolume(HP_SMART_CARD_DEVICE_PEN2, &drop_volume);
    } else {
        LOGD(">>> getMaxVolume(#%d) = %d (pre-calculated)", card, MaxBagInkVolume);
        return MaxBagInkVolume;
    }

    // bulk 计算
    // DV 29 对应3150
    // DV x=20-35 ：   3150*(29/X)
    // DV x < 20  or  X>35:    4700

    if(drop_volume < 20 || drop_volume > 35) {
        MaxBagInkVolume = MAX_BAG_INK_VOLUME_MAXIMUM;
    } else {
        MaxBagInkVolume = MAX_BAG_INK_VOLUME_MINIMUM * 29 / drop_volume;
    }

    MaxPenInkVolume                      = MaxBagInkVolume * PEN_VS_BAG_RATIO;
    InkVolOfBagPercentage                = MaxBagInkVolume / 100;
    InkVolOfPenPercentage                = MaxPenInkVolume / 100;

    LOGD(">>> getMaxVolume(#%d) = %d", card, MaxBagInkVolume);
*/
    return MaxBagInkVolume;
}

JNIEXPORT jstring JNICALL Java_com_Smartcard_readConsistency(JNIEnv *env, jclass arg, jint card) {
    char strTemp[1024];

    uint8_t ink_designator;
    uint8_t ink_formulator_id;
    uint8_t ink_family;
    uint8_t ink_color_code;
    uint8_t ink_family_member;
    uint8_t out_of_ink;
    float density, weight;
    uint8_t drop_volume;
    uint32_t x = 0;

    uint32_t value;
    char buf[4][26];
    char title[8];

    strcpy(title, "UNKNOWN");
    if(CARD_SELECT_PEN1 == card) {
        inkReadTag13OEMInkDesignator(HP_SMART_CARD_DEVICE_PEN1, &ink_designator);
        inkReadTag4FormulatorID(HP_SMART_CARD_DEVICE_PEN1, &ink_formulator_id);
        inkReadTag4InkFamily(HP_SMART_CARD_DEVICE_PEN1, &ink_family);
        inkReadTag4ColorCodesGeneral(HP_SMART_CARD_DEVICE_PEN1, &ink_color_code);
        inkReadTag4InkFamilyMember(HP_SMART_CARD_DEVICE_PEN1, &ink_family_member);
        inkReadTag9ILGOutOfInkBit(HP_SMART_CARD_DEVICE_PEN1, &out_of_ink);
        inkReadTag4Density(HP_SMART_CARD_DEVICE_PEN1, &density);
        inkReadTag4UsableInkWeight(HP_SMART_CARD_DEVICE_PEN1, &weight);
        inkReadTag5DropVolume(HP_SMART_CARD_DEVICE_PEN1, &drop_volume);
        for(int i=0; i<4; i++) {
            value = 0x00000000;
            inkILGReadFunc[i](HP_SMART_CARD_DEVICE_PEN1, &value);
            toBinaryString(buf[i], value);
        }
        strcpy(title, "PEN1");
    } else if(CARD_SELECT_PEN2 == card) {
        inkReadTag13OEMInkDesignator(HP_SMART_CARD_DEVICE_PEN2, &ink_designator);
        inkReadTag4FormulatorID(HP_SMART_CARD_DEVICE_PEN2, &ink_formulator_id);
        inkReadTag4InkFamily(HP_SMART_CARD_DEVICE_PEN2, &ink_family);
        inkReadTag4ColorCodesGeneral(HP_SMART_CARD_DEVICE_PEN2, &ink_color_code);
        inkReadTag4InkFamilyMember(HP_SMART_CARD_DEVICE_PEN2, &ink_family_member);
        inkReadTag9ILGOutOfInkBit(HP_SMART_CARD_DEVICE_PEN2, &out_of_ink);
        inkReadTag4Density(HP_SMART_CARD_DEVICE_PEN2, &density);
        inkReadTag4UsableInkWeight(HP_SMART_CARD_DEVICE_PEN2, &weight);
        inkReadTag5DropVolume(HP_SMART_CARD_DEVICE_PEN2, &drop_volume);
        for(int i=0; i<4; i++) {
            value = 0x00000000;
            inkILGReadFunc[i](HP_SMART_CARD_DEVICE_PEN2, &value);
            toBinaryString(buf[i], value);
        }
        strcpy(title, "PEN2");
    } else if(CARD_SELECT_BULK1 == card) {
        supplyReadTag1OEMInkDesignator(HP_SMART_CARD_DEVICE_BULK1, &ink_designator);
        supplyReadTag4FormulatorID(HP_SMART_CARD_DEVICE_BULK1, &ink_formulator_id);
        supplyReadTag4InkFamily(HP_SMART_CARD_DEVICE_BULK1, &ink_family);
        supplyReadTag4ColorCodesGeneral(HP_SMART_CARD_DEVICE_BULK1, &ink_color_code);
        supplyReadTag4InkFamilyMember(HP_SMART_CARD_DEVICE_BULK1, &ink_family_member);
        supplyReadTag9ILGOutOfInkBit(HP_SMART_CARD_DEVICE_BULK1, &out_of_ink);
        supplyReadTag4Density(HP_SMART_CARD_DEVICE_BULK1, &density);
        supplyReadTag4UsableInkWeight(HP_SMART_CARD_DEVICE_BULK1, &weight);
        drop_volume = 0;
        for(int i=0; i<4; i++) {
            value = 0x00000000;
            supplyILGReadFunc[i](HP_SMART_CARD_DEVICE_BULK1, &value);
            toBinaryString(buf[i], value);
        }
        if (HP_SMART_CARD_OK != supplyReadTag12OEMDefRWField1(HP_SMART_CARD_DEVICE_BULK1, &x)) {
            x = 0;
        }
        strcpy(title, "BULK1");
    } else if(CARD_SELECT_BULKX == card) {
        inkReadTag13OEMInkDesignator(HP_SMART_CARD_DEVICE_BULK1, &ink_designator);
        inkReadTag4FormulatorID(HP_SMART_CARD_DEVICE_BULK1, &ink_formulator_id);
        inkReadTag4InkFamily(HP_SMART_CARD_DEVICE_BULK1, &ink_family);
        inkReadTag4ColorCodesGeneral(HP_SMART_CARD_DEVICE_BULK1, &ink_color_code);
        inkReadTag4InkFamilyMember(HP_SMART_CARD_DEVICE_BULK1, &ink_family_member);
        inkReadTag9ILGOutOfInkBit(HP_SMART_CARD_DEVICE_BULK1, &out_of_ink);
        inkReadTag4Density(HP_SMART_CARD_DEVICE_BULK1, &density);
        inkReadTag4UsableInkWeight(HP_SMART_CARD_DEVICE_BULK1, &weight);
        drop_volume = 0;
        for(int i=0; i<4; i++) {
            value = 0x00000000;
            inkILGReadFunc[i](HP_SMART_CARD_DEVICE_BULK1, &value);
            toBinaryString(buf[i], value);
        }
        strcpy(title, "BULKX");
    }

    sprintf(strTemp, "[%s]\ndsg:%d,fml:%d,fam:%d,clr:%d,mem:%d,ds:%f,wt:%f,dv:%d\nILG:%s-%s-%s-%s\nOIB:%d\nCount:%d",
        title, ink_designator, ink_formulator_id, ink_family, ink_color_code, ink_family_member, density, weight, drop_volume,buf[3], buf[2], buf[1], buf[0],out_of_ink,x
    );

    jstring result = (*env)->NewStringUTF(env, strTemp);
//    (*env)->ReleaseStringUTFChars(env, result, strTemp);

    return result;
}

JNIEXPORT jint JNICALL Java_com_Smartcard_checkOIB(JNIEnv *env, jclass arg, jint card) {
    uint8_t out_of_ink = 1;

    HP_SMART_CARD_result_t ret = HP_SMART_CARD_ERROR;
    if(CARD_SELECT_PEN1 == card) {
        ret = inkReadTag9ILGOutOfInkBit(HP_SMART_CARD_DEVICE_PEN1, &out_of_ink);
    } else if(CARD_SELECT_PEN2 == card) {
        ret = inkReadTag9ILGOutOfInkBit(HP_SMART_CARD_DEVICE_PEN2, &out_of_ink);
    } else if(CARD_SELECT_BULK1 == card) {
        ret = supplyReadTag9ILGOutOfInkBit(HP_SMART_CARD_DEVICE_BULK1, &out_of_ink);
    } else if(CARD_SELECT_BULKX == card) {
        ret = inkReadTag9ILGOutOfInkBit(HP_SMART_CARD_DEVICE_BULK1, &out_of_ink);
    }

    if (HP_SMART_CARD_OK != ret) {
        LOGE(">>> checkOIB access error(#%d)!", card);
        out_of_ink = 1;
    }

    LOGD(">>> checkOIB(#%d) = %d", card, out_of_ink);

    return out_of_ink;
}

// H.M.Wang 2021-8-9 修改根据ILG调整OEM数值的算法，由原来的从高位开始检测改为从低位开始检测，如果低位为1则增加一个百分比，检查紧邻高位，一次类推，遇到为0则退出。访问错误时不进行根据ILG对OEM值的修改
static void adjustLocalInkValue(jint card) {
    HP_SMART_CARD_result_t ret = HP_SMART_CARD_ERROR;
    uint32_t ilg = 0;
    uint8_t quit = 0;

    for(int i=0; i<4; i++) {
        uint32_t tmp_value = 0x00000000;
        if(CARD_SELECT_PEN1 == card) {
            ret = inkILGReadFunc[i](HP_SMART_CARD_DEVICE_PEN1, &tmp_value);
        } else if(CARD_SELECT_PEN2 == card) {
            ret = inkILGReadFunc[i](HP_SMART_CARD_DEVICE_PEN2, &tmp_value);
        } else if(CARD_SELECT_BULK1 == card) {
            ret = supplyILGReadFunc[i](HP_SMART_CARD_DEVICE_BULK1, &tmp_value);
        } else if(CARD_SELECT_BULKX == card) {
            ret = inkILGReadFunc[i](HP_SMART_CARD_DEVICE_BULK1, &tmp_value);
        }

        if(HP_SMART_CARD_OK != ret) {
            break;
        }

        uint32_t mask = 0x00000001;
        for(int i=0; i<25; i++) {
            if(mask & tmp_value) {
                ilg++;
                mask <<= 1;
            } else {
                quit = 1;
                break;
            }
        }
        if(quit) break;
    }

    int vol_percentage = 1;
    uint32_t x = 0;
    if(CARD_SELECT_PEN1 == card) {
        ret = inkReadTag12OEMDefRWField1(HP_SMART_CARD_DEVICE_PEN1, &x);
        vol_percentage = InkVolOfPenPercentage;
    } else if(CARD_SELECT_PEN2 == card) {
        ret = inkReadTag12OEMDefRWField1(HP_SMART_CARD_DEVICE_PEN2, &x);
        vol_percentage = InkVolOfPenPercentage;
    } else if(CARD_SELECT_BULK1 == card) {
        ret = supplyReadTag12OEMDefRWField1(HP_SMART_CARD_DEVICE_BULK1, &x);
        vol_percentage = InkVolOfBagPercentage;
    } else if(CARD_SELECT_BULKX == card) {
        ret = inkReadTag12OEMDefRWField1(HP_SMART_CARD_DEVICE_BULK1, &x);
        vol_percentage = InkVolOfBagPercentage;
    }
    if (HP_SMART_CARD_OK != ret) {
        x = 0;
    }

    LOGD(">>> AdjustLocalInkValue(Card:%d): ILG=%d, Value=(%d -> %d)", card, ilg, x, (quit == 1 ? ilg * vol_percentage + x % vol_percentage : x));

    x = (quit == 1 ? ilg * vol_percentage + x % vol_percentage : x);

    if(CARD_SELECT_PEN1 == card) {
        inkWriteTag12OEMDefRWField1(HP_SMART_CARD_DEVICE_PEN1, x);
    } else if(CARD_SELECT_PEN2 == card) {
        inkWriteTag12OEMDefRWField1(HP_SMART_CARD_DEVICE_PEN2, x);
    } else if(CARD_SELECT_BULK1 == card) {
        supplyWriteTag12OEMDefRWField1(HP_SMART_CARD_DEVICE_BULK1, x);
    } else if(CARD_SELECT_BULKX == card) {
        inkWriteTag12OEMDefRWField1(HP_SMART_CARD_DEVICE_BULK1, x);
    }
}
// End of H.M.Wang 2021-8-9 修改根据ILG调整OEM数值的算法，由原来的从高位开始检测改为从低位开始检测，如果低位为1则增加一个百分比，检查紧邻高位，一次类推，遇到为0则退出。访问错误时不进行根据ILG对OEM值的修改

JNIEXPORT jint JNICALL Java_com_Smartcard_getLocalInk(JNIEnv *env, jclass arg, jint card) {
// 该判断另外逻辑处理，本函数如实返回读数
//    if(Java_com_Smartcard_checkOIB(env, arg, card) == 1) {
//        return 0;
//    }

    HP_SMART_CARD_result_t ret = HP_SMART_CARD_ERROR;

    uint32_t x = -1;
    if(CARD_SELECT_PEN1 == card) {
        ret = inkReadTag12OEMDefRWField1(HP_SMART_CARD_DEVICE_PEN1, &x);
    } else if(CARD_SELECT_PEN2 == card) {
        ret = inkReadTag12OEMDefRWField1(HP_SMART_CARD_DEVICE_PEN2, &x);
    } else if(CARD_SELECT_BULK1 == card) {
        ret = supplyReadTag12OEMDefRWField1(HP_SMART_CARD_DEVICE_BULK1, &x);
    } else if(CARD_SELECT_BULKX == card) {
        ret = inkReadTag12OEMDefRWField1(HP_SMART_CARD_DEVICE_BULK1, &x);
    }

    if (HP_SMART_CARD_OK != ret) {
        LOGE(">>> getLocalInk access error(#%d)!", card);
    }
    LOGD(">>> Ink Level(#%d) = %d", card, x);

    return x;
}

static void writeILG(jint card, int percent) {
    uint32_t value;
    char buf[4][26];

    if(percent < 0 || percent >= 100) return;

    for(int i=0; i<4; i++) {
        value = 0x00000000;
        if(CARD_SELECT_PEN1 == card) {
            inkILGReadFunc[i](HP_SMART_CARD_DEVICE_PEN1, &value);
        } else if(CARD_SELECT_PEN2 == card) {
            inkILGReadFunc[i](HP_SMART_CARD_DEVICE_PEN2, &value);
        } else if(CARD_SELECT_BULK1 == card) {
            supplyILGReadFunc[i](HP_SMART_CARD_DEVICE_BULK1, &value);
        } else if(CARD_SELECT_BULKX == card) {
            inkILGReadFunc[i](HP_SMART_CARD_DEVICE_BULK1, &value);
        }

        // percent/25选择ilg(0:1-25; 1:26-50; 2:51:75; 3:76-100)
        // percent%25选择ilg的写入位(1%, 26%, 51%, 76%在第0位)
        if(i == percent / 25) {
            value |= (0x00000001 << (percent % 25));
            if(CARD_SELECT_PEN1 == card) {
                inkILGWriteFunc[i](HP_SMART_CARD_DEVICE_PEN1, value);
            } else if(CARD_SELECT_PEN2 == card) {
                inkILGWriteFunc[i](HP_SMART_CARD_DEVICE_PEN2, value);
            } else if(CARD_SELECT_BULK1 == card) {
                supplyILGWriteFunc[i](HP_SMART_CARD_DEVICE_BULK1, value);
            } else if(CARD_SELECT_BULKX == card) {
                inkILGWriteFunc[i](HP_SMART_CARD_DEVICE_BULK1, value);
            }
        }
        toBinaryString(buf[i], value);
    }

    LOGD(">>> ILG(#%d) = %s-%s-%s-%s", card, buf[3], buf[2], buf[1], buf[0]);
}

JNIEXPORT jint JNICALL Java_com_Smartcard_downLocal(JNIEnv *env, jclass arg, jint card) {
    uint32_t x = 0;
    HP_SMART_CARD_result_t ret = HP_SMART_CARD_ERROR;

    pthread_mutex_lock(&mutex);

    int vol_percentage = 1;

    if(CARD_SELECT_PEN1 == card) {
        ret = inkReadTag12OEMDefRWField1(HP_SMART_CARD_DEVICE_PEN1, &x);
        vol_percentage *= InkVolOfPenPercentage;
    } else if(CARD_SELECT_PEN2 == card) {
        ret = inkReadTag12OEMDefRWField1(HP_SMART_CARD_DEVICE_PEN2, &x);
        vol_percentage *= InkVolOfPenPercentage;
    } else if(CARD_SELECT_BULK1 == card) {
        ret = supplyReadTag12OEMDefRWField1(HP_SMART_CARD_DEVICE_BULK1, &x);
        vol_percentage *= InkVolOfBagPercentage;
    } else if(CARD_SELECT_BULKX == card) {
        ret = inkReadTag12OEMDefRWField1(HP_SMART_CARD_DEVICE_BULK1, &x);
        vol_percentage *= InkVolOfBagPercentage;
    }

    if (HP_SMART_CARD_OK != ret) {
        if(CARD_SELECT_PEN1 == card || CARD_SELECT_PEN2 == card) {
            pthread_mutex_unlock(&mutex);
            return SC_PRINT_CTRG_ACCESS_FAILED;
        } else if(CARD_SELECT_BULK1 == card || CARD_SELECT_BULKX == card) {
            pthread_mutex_unlock(&mutex);
            return SC_BULK_CTRG_ACCESS_FAILED;
        }
    }

    int p1, p2;
    p1 = x / vol_percentage;
    x++;
    p2 = x / vol_percentage;

    if(CARD_SELECT_PEN1 == card) {
        ret = inkWriteTag12OEMDefRWField1(HP_SMART_CARD_DEVICE_PEN1, x);
    } else if(CARD_SELECT_PEN2 == card) {
        ret = inkWriteTag12OEMDefRWField1(HP_SMART_CARD_DEVICE_PEN2, x);
    } else if(CARD_SELECT_BULK1 == card) {
        ret = supplyWriteTag12OEMDefRWField1(HP_SMART_CARD_DEVICE_BULK1, x);
    } else if(CARD_SELECT_BULKX == card) {
        ret = inkWriteTag12OEMDefRWField1(HP_SMART_CARD_DEVICE_BULK1, x);
    }

    if (HP_SMART_CARD_OK != ret) {
        if(CARD_SELECT_PEN1 == card || CARD_SELECT_PEN2 == card) {
            pthread_mutex_unlock(&mutex);
            return SC_PRINT_CTRG_ACCESS_FAILED;
        } else if(CARD_SELECT_BULK1 == card || CARD_SELECT_BULKX == card) {
            pthread_mutex_unlock(&mutex);
            return SC_BULK_CTRG_ACCESS_FAILED;
        }
    }

    LOGD(">>> downLocal(#%d) -> %d", card, x);

    if(p1 != p2) {
        writeILG(card, p1);

        if(100 <= p2) {
            LOGD(">>> OIB(#%d)", card);
            if(CARD_SELECT_PEN1 == card) {
                inkWriteTag9ILGOutOfInkBit(HP_SMART_CARD_DEVICE_PEN1, 1);
            } else if(CARD_SELECT_PEN2 == card) {
                inkWriteTag9ILGOutOfInkBit(HP_SMART_CARD_DEVICE_PEN2, 1);
            } else if(CARD_SELECT_BULK1 == card) {
                supplyWriteTag9ILGOutOfInkBit(HP_SMART_CARD_DEVICE_BULK1, 1);
            } else if(CARD_SELECT_BULKX == card) {
                inkWriteTag9ILGOutOfInkBit(HP_SMART_CARD_DEVICE_BULK1, 1);
            }
        }
    }

    pthread_mutex_unlock(&mutex);

    return SC_SUCCESS;
}

JNIEXPORT jint JNICALL Java_com_Smartcard_writeOIB(JNIEnv *env, jclass arg, jint card) {
    HP_SMART_CARD_result_t ret = HP_SMART_CARD_ERROR;

    pthread_mutex_lock(&mutex);

    LOGD(">>> Write OIB(#%d)", card);

    if(CARD_SELECT_PEN1 == card) {
        ret = inkWriteTag9ILGOutOfInkBit(HP_SMART_CARD_DEVICE_PEN1, 1);
    } else if(CARD_SELECT_PEN2 == card) {
        ret = inkWriteTag9ILGOutOfInkBit(HP_SMART_CARD_DEVICE_PEN2, 1);
    } else if(CARD_SELECT_BULK1 == card) {
        ret = supplyWriteTag9ILGOutOfInkBit(HP_SMART_CARD_DEVICE_BULK1, 1);
    } else if(CARD_SELECT_BULKX == card) {
        ret = inkWriteTag9ILGOutOfInkBit(HP_SMART_CARD_DEVICE_BULK1, 1);
    }

    pthread_mutex_unlock(&mutex);

    return ret;
}

// H.M.Wang 2024-5-27 临时追加一个DAC5571的设置功能
#define DAC5571_I2C_ADDRESS      0x4C
JNIEXPORT jint JNICALL Java_com_Smartcard_writeDAC5571(JNIEnv *env, jclass arg, jint value) {
    LOGD(">>> Write DAC5571(I2C=0x%2x, value=%d)", DAC5571_I2C_ADDRESS, value);
    int write_length;
    uint8_t cmd[2];

    cmd[0] = ((value >> 4) & 0x0F);
    cmd[1] = ((value << 4) & 0xF0);

    pthread_mutex_lock(&mutex);
    // 似乎没有寄存器的概念: 数据的结构式: 第一个字节：0 0 PD1 PD0 MSB(4bits)。第二个字节：LSB(4bits),后面四个字节随意
    write_length = SC_I2C_DRIVER_write(0x01, DAC5571_I2C_ADDRESS, cmd[0], &cmd[1], 1);
    pthread_mutex_unlock(&mutex);

    if(write_length < 0) {
        LOGE("Write command error!");
        return LEVEL_I2C_FAILED;
    }
    return LEVEL_I2C_OK;
}
// End of H.M.Wang 2024-5-27 临时追加一个DAC5571的设置功能

// H.M.Wang 2024-5-24 临时追加一个ADS1115芯片的读数功能
#define ADS1115_I2C_ADDRESS      0x49
JNIEXPORT jint JNICALL Java_com_Smartcard_readADS1115(JNIEnv *env, jclass arg, jint index) {
    LOGD(">>> Read ADS1115(I2C=0x%2x, index=%d, addr=0)", ADS1115_I2C_ADDRESS, index);

    int write_length;
    int read_length;
    uint8_t data[2];
    uint8_t cmd[2] = {0xc2, 0x83};      // AIN0, FSR = ±4.096 V

    pthread_mutex_lock(&mutex);
    // 先读当前的数据，然后切换到新的输入口。这样，下次读数据时，数据就可以稳定的读到了
    read_length = SC_I2C_DRIVER_read(0x01, ADS1115_I2C_ADDRESS, 0, data, 2);
    write_length = SC_I2C_DRIVER_write(0x01, ADS1115_I2C_ADDRESS, 1, cmd, 2);
    pthread_mutex_unlock(&mutex);

    if(write_length < 0) {
        LOGE("Write command error!");
        return LEVEL_I2C_FAILED;
    }
    if(read_length < 0) {
        LOGE("Read data error!");
        return LEVEL_I2C_FAILED;
    }

    LOGD(">>> ADS1115[%d] data read: 0x%02X%02X", index, data[0], data[1]);

    return data[0] * 256 + data[1];
}

// End of H.M.Wang 2024-5-24 临时追加一个ADS1115芯片的读数功能

// H.M.Wang 2022-12-24 追加一个读写HX24LC芯片的功能，用来保存对应Bagink墨位的调整值
#define HX24LC_I2C_ADDRESS      0x50

JNIEXPORT jint JNICALL Java_com_Smartcard_readHX24LC(JNIEnv *env, jclass arg) {
    LOGD(">>> Read HX24LC(I2C=0x50,addr=0) Direct for [BAGINK]");

    int read_length;
    uint8_t data;

    pthread_mutex_lock(&mutex);
    read_length = SC_I2C_DRIVER_read(0x01, HX24LC_I2C_ADDRESS, 0, &data, 1);
    pthread_mutex_unlock(&mutex);

    if(read_length < 0) {
        LOGE("Read data error!");
        data = 0;
    }

    LOGD(">>> HX24LC for [BAGINK] data read: 0x%08X", data);

    return data;
}

JNIEXPORT jint JNICALL Java_com_Smartcard_writeHX24LC(JNIEnv *env, jclass arg, jint value) {
    LOGD(">>> Write value=%d into HX24LC(I2C=0x50,addr=0) Direct for [BAGINK]", value);

    int write_length;
    uint8_t data = (value & 0x0FF);

    pthread_mutex_lock(&mutex);
    write_length = SC_I2C_DRIVER_write(0x01, HX24LC_I2C_ADDRESS, 0, &data, 1);
    pthread_mutex_unlock(&mutex);

    if(write_length < 0) {
        LOGE("Write data error!");
        return LEVEL_I2C_FAILED;
    }

    return write_length;
}
// End of H.M.Wang 2022-12-24 追加一个读写HX24LC芯片的功能，用来保存对应Bagink墨位的调整值

// H.M.Wang 2024-8-6 增加一个判断Level测量芯片种类的函数，apk会根据不同的芯片种类，执行不同的逻辑。读取Level值也会根据不同的种类而调用不同的接口
#define LEVEL_CHIP_TYPE_NONE        0
#define LEVEL_CHIP_TYPE_1614        1
#define LEVEL_CHIP_TYPE_MCPH21      2
static int sLevelChipType[] = {LEVEL_CHIP_TYPE_NONE, LEVEL_CHIP_TYPE_NONE, LEVEL_CHIP_TYPE_NONE, LEVEL_CHIP_TYPE_NONE};

JNIEXPORT jint JNICALL Java_com_Smartcard_getLevelType(JNIEnv *env, jclass arg, jint index) {
    pthread_mutex_lock(&mutex);

    uint32_t chData;
    uint8_t cmd;

    if(LEVEL_I2C_OK == readMCPH21Byte(0x30, &cmd)) {
        sLevelChipType[index] = LEVEL_CHIP_TYPE_MCPH21;
    } else if(LEVEL_I2C_OK == readChannelData0(&chData)) {
        sLevelChipType[index] = LEVEL_CHIP_TYPE_1614;
    }

    pthread_mutex_unlock(&mutex);

    LOGD(">>> Level chip[%d] type: %d", index, sLevelChipType[index]);

    return sLevelChipType[index];
}
// End of H.M.Wang 2024-8-6 增加一个判断Level测量芯片种类的函数，apk会根据不同的芯片种类，执行不同的逻辑

// H.M.Wang 2022-11-1 Add this API for Bagink Use
JNIEXPORT jint JNICALL Java_com_Smartcard_readLevelDirect(JNIEnv *env, jclass arg, jint index) {
// H.M.Wang 2024-8-6 扩充该函数，原来是专供1614芯片使用，现在在apk中为与MCPH21共用，在进入本函数后，根据chip的种类再分开执行
    if(LEVEL_CHIP_TYPE_MCPH21 == sLevelChipType[index]) return Java_com_Smartcard_readMCPH21Level(env, arg, index);
// End of H.M.Wang 2024-8-6 扩充该函数，原来是专供1614芯片使用，现在在apk中为与MCPH21共用，在进入本函数后，根据chip的种类再分开执行

    LOGD(">>> Read Level Direct for [BAGINK]");

    pthread_mutex_lock(&mutex);

    uint32_t chData;

    if(LEVEL_I2C_OK != readChannelData0(&chData)) {
        chData = 0;
    }

    LOGD(">>> Level for [BAGINK] data read: 0x%08X", chData);

    pthread_mutex_unlock(&mutex);

    return chData;
}
// End of H.M.Wang 2022-11-1 Add this API for Bagink Use

// H.M.Wang 2024-7-4 追加一个MCP-H21系列芯片测量压力的读写功能
JNIEXPORT jint JNICALL Java_com_Smartcard_readMCPH21Level(JNIEnv *env, jclass arg, jint index) {
    LOGD(">>> Read MCP-H21 Level[%d]", index);

    pthread_mutex_lock(&mutex);

    uint32_t chData = 0;

    // 写入0x0A 开始单次压力采集模式， 当寄存器值变为0x02 时，单次采集完成；
    char cmd = 0x0A;
    writeMCPH21Byte(0x30, &cmd);

    do {
        usleep(1000);
        if(LEVEL_I2C_OK != readMCPH21Byte(0x30, &cmd)) break;
        if(cmd == 0x02) break;
    } while(1);

    // Code = Data0x06*2^16+ Data0x07*2^8 + Data0x08
    if(LEVEL_I2C_OK == readMCPH21Byte(0x06, &cmd)) {
        chData |= ((cmd << 16) & 0x00FF0000);
    }

    if(LEVEL_I2C_OK == readMCPH21Byte(0x07, &cmd)) {
        chData |= ((cmd << 8) & 0x0000FF00);
    }

    if(LEVEL_I2C_OK == readMCPH21Byte(0x08, &cmd)) {
        chData += ((cmd << 0) & 0x000000FF);
    }

    LOGD(">>> MCP-H21 Level[%d] data read: 0x%008X", index, chData);

    pthread_mutex_unlock(&mutex);

    return chData;
}
// End of H.M.Wang 2024-7-4 追加一个MCP-H21系列芯片测量压力的读写功能

// H.M.Wang 2024-10-28 增加9555A的读写试验，速录在100k和200k，每次读写500次，读写结果输出log。切换速录需要切换img
JNIEXPORT jint JNICALL Java_com_Smartcard_read9555ATest(JNIEnv *env, jclass arg) {
    LOGD(">>> Read 9555A Test Start");

    int read_length;
    uint8_t data[20];
    char outString[100];
    char hex[4];

    pthread_mutex_lock(&mutex);
    // DeviceAddr：[0 0 1 0 0 <A1> <A2> <A0>] [<0|1>读写]
    // Control Register Bits: // DeviceAddr：[ 0 0 0 0 0 <B2> <B1> <B0>]
    // B2 B1 B0  COMMAND-BYTE(HEX) REGISTER                   PROTOCOL          POWER-UP
    // 0  0  0   0x00              Input Port 0               Read byte         xxxx xxxx
    // 0  0  1   0x01              Input Port 1               Read byte         xxxx xxxx
    // 0  1  0   0x02              Output Port 0              Read/write byte   1111 1111
    // 0  1  1   0x03              Output Port 1              Read/write byte   1111 1111
    // 1  0  0   0x04              Polarity Inversion Port 0  Read/write byte   0000 0000
    // 1  0  1   0x05              Polarity Inversion Port 1  Read/write byte   0000 0000
    // 1  1  0   0x06              Configuration Port 0       Read/write byte   1111 1111
    // 1  1  1   0x07              Configuration Port 1       Read/write byte   1111 1111

    uint8_t d = 0x55;
    SC_I2C_DRIVER_write(0x01, 0x21, 0x02, &d, 1);
    d = 0xAA;
    SC_I2C_DRIVER_write(0x01, 0x21, 0x03, &d, 1);
    char check;
    int err = 0;

    for(int i=0; i<2500; i++) {
//        memset(outString, 0x00, 100);
        read_length = SC_I2C_DRIVER_read(0x01, 0x21, 0x02, data, 20);
        for(int j=0; j<20; j++) {
            if(read_length < 0) {
//                strcat(outString, "XX ");
                err += 8;
            } else {
                if(j%2 == 0) {
                    check = data[j] ^ 0x55;
                } else {
                    check = data[j] ^ 0xAA;
                }
                for(int k=0; k<8; k++) {
                    if(((0x01 << k) & check) != 0) err++;
                }
//                sprintf(hex, "%02X ", data[j]);
//                strcat(outString, hex);
            }
            // Device=00100001(0x21), ComandByte=0x01
/*            read_length = SC_I2C_DRIVER_read(0x01, 0x21, 0x01, &data, 1);
            if(read_length < 0) {
                strcat(outString, "XX ");
            } else {
                sprintf(hex, "%02X ", data);
                strcat(outString, hex);
            }*/
        }
//        LOGD("=====> 9555A Read %03d-%03d : %s\n", i*20+1, (i+1)*20, outString);
    }
    pthread_mutex_unlock(&mutex);

    LOGD(">>> Read 9555A Test End. Error Count: %d", err);

    return err;
}
// End of H.M.Wang 2024-10-28 增加9555A的读写试验，速录在100k和200k，每次读写500次，读写结果输出log。切换速录需要切换img

// H.M.Wang 2024-11-5 因为A133的RTC没有了sys/class/device_of_i2c的接口，因此必须通过I2C的驱动直接读写
JNIEXPORT jbyteArray JNICALL Java_com_Smartcard_readRTC(JNIEnv *env, jclass arg, jbyte group, jbyte addr, jbyte reg, jint len) {
    LOGD(">>> Read RTC %x,%x:%x (len=%d)", group, addr, reg, len);

    int read_length;
    uint8_t data[len];

    pthread_mutex_lock(&mutex);
    read_length = SC_I2C_DRIVER_read(group, addr, reg, data, len);
    pthread_mutex_unlock(&mutex);

    char dst[read_length*5];
    toHexString(data, dst, read_length, ',');
    LOGD(">>> Read RTC %x,%x:%x [%s]", group, addr, reg, dst);

    jbyteArray result = (*env)->NewByteArray(env, read_length);
    (*env)->SetByteArrayRegion(env, result, 0, read_length, data);

    return result;
}

JNIEXPORT jint JNICALL Java_com_Smartcard_writeRTC(JNIEnv *env, jclass arg, jbyte group, jbyte addr, jbyte reg, jbyteArray data, jint len) {
    jbyte *cbuf;
    cbuf = (*env)->GetByteArrayElements(env, data, 0);

    char dst[len*5];
    toHexString(cbuf, dst, len, ',');
    LOGD(">>> Write RTC %x,%x:%x (len=%d)", group, addr, reg, len);

    int write_length;
    pthread_mutex_lock(&mutex);
    write_length = SC_I2C_DRIVER_write(group, addr, reg, cbuf, len);
    pthread_mutex_unlock(&mutex);

    LOGD(">>> Read RTC %x,%x:%x (len=%d)", group, addr, reg, write_length);

    (*env)->ReleaseByteArrayElements(env, data, cbuf, 0);

    return write_length;
}
// End of H.M.Wang 2024-11-5 因为A133的RTC没有了sys/class/device_of_i2c的接口，因此必须通过I2C的驱动直接读写

/**
 * 读取Level值
 */
JNIEXPORT jint JNICALL Java_com_Smartcard_readLevel(JNIEnv *env, jclass arg, jint card, jint min, jint max) {
    LOGD(">>> Read Level(#%d)", card);

    pthread_mutex_lock(&mutex);

    if(SELECT_LEVEL1 == card) {
        SC_GPIO_ADAPTER_select_device(GPIO_DEVICE_PEN1);
    } else if(SELECT_LEVEL2 == card) {
        SC_GPIO_ADAPTER_select_device(GPIO_DEVICE_PEN2);
    } else {
        pthread_mutex_unlock(&mutex);
        return 0;
    }

    uint32_t chData;

    if(LEVEL_I2C_OK != readChannelData0(&chData)) {
        chData = 0;
    }

    LOGD(">>> Level data read: 0x%08X", chData);

//    if(chData == 0x0FFFFFFF || chData == 0x00000000) {
    if(chData < min - 10000000 || chData > max + 10000000) {
        uint16_t config;
        if(LEVEL_I2C_OK == readConfig(&config)) {
            config |= CONFIG_SLEEP_MODE_ENABLE;                // Set to Sleep mode
            writeConfig(&config);

            int temp = 0;
            for(int i=0; i<1000; i++){
                temp++;
            }

            config &= CONFIG_ACTIVE_MODE_ENABLE;                // Set to Active mode
            writeConfig(&config);

            LOGD(">>> Level Restart!");
        }
    }

// H.M.Wang 2024-10-19 读完Level后，将I2C切换到墨袋上
    SC_GPIO_ADAPTER_select_device(GPIO_DEVICE_BULK1);
// End of H.M.Wang 2024-10-19 读完Level后，将I2C切换到墨袋上

    pthread_mutex_unlock(&mutex);

    return chData;
}

/**
 * 测试Level值
 */
static int ppp_1 = 5;
static int ppp_2 = 5;

JNIEXPORT jint JNICALL Java_com_Smartcard_testLevel(JNIEnv *env, jclass arg, jint card) {
    LOGD(">>> Test Level(#%d)", card);

    pthread_mutex_lock(&mutex);
    int *p = NULL;

    if(SELECT_LEVEL1 == card) {
        SC_GPIO_ADAPTER_select_device(GPIO_DEVICE_PEN1);
        p = &ppp_1;
    } else if(SELECT_LEVEL2 == card) {
        SC_GPIO_ADAPTER_select_device(GPIO_DEVICE_PEN2);
        p = &ppp_2;
    } else {
        pthread_mutex_unlock(&mutex);
        return 0;
    }

    uint32_t chData;

    if(LEVEL_I2C_OK != readChannelData0(&chData)) {
        chData = 0;
    }

    LOGD(">>> Level data read(For Test Level): 0x%08X", chData);

    uint16_t config;
    (*p)--;
    if(*p == 0) {
        if(LEVEL_I2C_OK == readConfig(&config)) {
            config |= CONFIG_SLEEP_MODE_ENABLE;                // Set to Sleep mode
            writeConfig(&config);
            LOGD(">>> Level %d Sleep!", card);
            config &= CONFIG_ACTIVE_MODE_ENABLE;                // Set to Active mode
            writeConfig(&config);
            LOGD(">>> Level %d Awake!", card);
        }
        *p = 5;
    }

// H.M.Wang 2024-10-19 读完Level后，将I2C切换到墨袋上
    SC_GPIO_ADAPTER_select_device(GPIO_DEVICE_BULK1);
// End of H.M.Wang 2024-10-19 读完Level后，将I2C切换到墨袋上

    pthread_mutex_unlock(&mutex);

    return chData;
}

/**
 * 读取ManufactureID
 */
JNIEXPORT jint JNICALL Java_com_Smartcard_readManufactureID(JNIEnv *env, jclass arg, jint card) {
    LOGD(">>> Read ManufactureID(#%d)", card);

    pthread_mutex_lock(&mutex);

    if(SELECT_LEVEL1 == card) {
        SC_GPIO_ADAPTER_select_device(GPIO_DEVICE_PEN1);
    } else if(SELECT_LEVEL2 == card) {
        SC_GPIO_ADAPTER_select_device(GPIO_DEVICE_PEN2);
    } else {
        pthread_mutex_unlock(&mutex);
        return 0;
    }

    int16_t manID;

    if(LEVEL_I2C_OK != readManufactureID(&manID)) {
        manID = -1;
    }

    LOGD(">>> ManufactureID read: 0x%04X", manID);

// H.M.Wang 2024-10-19 读完Level后，将I2C切换到墨袋上
    SC_GPIO_ADAPTER_select_device(GPIO_DEVICE_BULK1);
// End of H.M.Wang 2024-10-19 读完Level后，将I2C切换到墨袋上

    pthread_mutex_unlock(&mutex);

    return manID;
}

/**
 * 读取DeviceID
 */
JNIEXPORT jint JNICALL Java_com_Smartcard_readDeviceID(JNIEnv *env, jclass arg, jint card) {
    LOGD(">>> Read DeviceID(#%d)", card);

    pthread_mutex_lock(&mutex);
    if(SELECT_LEVEL1 == card) {
        SC_GPIO_ADAPTER_select_device(GPIO_DEVICE_PEN1);
    } else if(SELECT_LEVEL2 == card) {
        SC_GPIO_ADAPTER_select_device(GPIO_DEVICE_PEN2);
    } else {
        pthread_mutex_unlock(&mutex);
        return 0;
    }

    int16_t devID;

    if(LEVEL_I2C_OK != readDeviceID(&devID)) {
        devID = -1;
    }

    LOGD(">>> DeviceID read: 0x%04X", devID);

// H.M.Wang 2024-10-19 读完Level后，将I2C切换到墨袋上
    SC_GPIO_ADAPTER_select_device(GPIO_DEVICE_BULK1);
// End of H.M.Wang 2024-10-19 读完Level后，将I2C切换到墨袋上

    pthread_mutex_unlock(&mutex);

    return devID;
}

/**
 * RTC操作jni接口
 */
static JNINativeMethod gMethods[] = {
        {"exist",					"(I)I",	                    (void *)Java_com_Smartcard_exist},
        {"init",					    "()I",	                    (void *)Java_com_Smartcard_init},
        {"initComponent",			"(I)I",	                    (void *)Java_com_Smartcard_init_comp},
        {"initLevelDirect",			"()I",	                    (void *)Java_com_Smartcard_init_level_direct},
        {"writeCheckSum",	        "(II)I",    					(void *)Java_com_Smartcard_writeCheckSum},
        {"checkSum",	                "(II)I",	    				(void *)Java_com_Smartcard_checkSum},
        {"checkConsistency",	        "(II)I",					    (void *)Java_com_Smartcard_checkConsistency},
        {"getMaxVolume",	            "(I)I",						(void *)Java_com_Smartcard_getMaxVolume},
        {"readConsistency",	        "(I)Ljava/lang/String;",	    (void *)Java_com_Smartcard_readConsistency},
        {"checkOIB",		            "(I)I",						(void *)Java_com_Smartcard_checkOIB},
        {"getLocalInk",		        "(I)I",						(void *)Java_com_Smartcard_getLocalInk},
        {"downLocal",		        "(I)I",						(void *)Java_com_Smartcard_downLocal},
        {"writeOIB",		            "(I)I",						(void *)Java_com_Smartcard_writeOIB},
        {"readLevel",		        "(III)I",					(void *)Java_com_Smartcard_readLevel},
        {"getLevelType",		        "(I)I",						(void *)Java_com_Smartcard_getLevelType},
        {"readLevelDirect",		    "(I)I",						(void *)Java_com_Smartcard_readLevelDirect},
        {"readMCPH21Level",		    "(I)I",						(void *)Java_com_Smartcard_readMCPH21Level},
        {"writeDAC5571",	    	    "(I)I",						(void *)Java_com_Smartcard_writeDAC5571},
        {"readADS1115",	    	    "(I)I",						(void *)Java_com_Smartcard_readADS1115},
        {"readHX24LC",	    	    "()I",						(void *)Java_com_Smartcard_readHX24LC},
        {"writeHX24LC",	    	    "(I)I",						(void *)Java_com_Smartcard_writeHX24LC},
        {"read9555ATest",	        "()I",						(void *)Java_com_Smartcard_read9555ATest},
        {"readRTC",     	            "(BBBI)[B",					(void *)Java_com_Smartcard_readRTC},
        {"writeRTC",     	        "(BBB[BI)I",					(void *)Java_com_Smartcard_writeRTC},
        {"testLevel",		        "(I)I",						(void *)Java_com_Smartcard_testLevel},
        {"readManufactureID",	    "(I)I",						(void *)Java_com_Smartcard_readManufactureID},
        {"readDeviceID",	            "(I)I",						(void *)Java_com_Smartcard_readDeviceID},
        {"shutdown",				    "()I",	                    (void *)Java_com_Smartcard_shutdown},
};

/**
 * 注册RTC操作的JNI方法
 */
int register_com_smartcard(JNIEnv* env) {
// H.M.Wang 2023-6-18 初始化移到这里
    if (pthread_mutex_init(&mutex, NULL) != 0){
        return JNI_FALSE;
    }
// End of H.M.Wang 2023-6-18 初始化移到这里

    const char* kClassPathName = "com/industry/printer/hardware/SmartCard";
    jclass clazz = (*env)->FindClass(env, kClassPathName);
    if(clazz == NULL) {
        return JNI_FALSE;
    }
    return (*env)->RegisterNatives(env, clazz, gMethods, sizeof(gMethods)/sizeof(gMethods[0]));
}


JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved)
{
    JNIEnv* env = NULL;
    jint result = -1;

    LOGI("SmartCard.so %s Loaded.", VERSION_CODE);

    if ((*vm)->GetEnv(vm, (void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        //__android_log_print(ANDROID_LOG_INFO, JNI_TAG,"ERROR: GetEnv failed\n");
        goto fail;
    }

    if (register_com_smartcard(env) < 0) {
        goto fail;
    }

    /* success -- return valid version number */
    result = JNI_VERSION_1_4;

    fail:
    return result;
}

#ifdef __cplusplus
}
#endif
