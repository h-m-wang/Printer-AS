/*
 * Confidential computer software. Valid license from HP required for possession, use or copying.  Consistent with FAR 12.211 and 12.212, Commercial Computer Software, Computer Software Documentation, and Technical Data for Commercial Items are licensed to the U.S. Government under vendor's standard commercial license.
 *
 * THE LICENSED SOFTWARE IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY DESCRIPTION.  HP SPECIFICALLY DISCLAIMS ANY IMPLIED WARRANTY OF MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  YOU ASSUME THE ENTIRE RISK RELATING TO THE USE OR PERFORMANCE OF THE LICENSED SOFTWARE.
 *
 * HP Company Confidential
 * © Copyright 2019 HP Development Company, L.P.
 * Made in U.S.A.
 */


#include <stdio.h>

//#include <linux/i2c.h>
//#include <linux/i2c-dev.h>
//#include <unistd.h>
#include <fcntl.h>
#include <linux/spi/spidev.h>
#include <sys/stat.h>
#include <pthread.h>

#include "hp22mm.h"
#include "common.h"
#include "ids.h"

#ifdef __cplusplus
extern "C"
{
#endif

#define VERSION_CODE                            "1.0.162"
// 1.0.162 2025-5-27
// ids.c中的i2c设备，A20是i2c-1，A133应该是i2c-2
// 1.0.161 2025-5-27
// 修改uart.c的bug，在uart_recv函数中，设置timeout值的时候，tv_usec不能大于1秒(1000000)，原来的判断在等于1秒的时候会出现这种情况
// 1.0.160 2025-5-27
// 查找uart.c中select发挥22号错误的原因
// 1.0.159 2025-5-27
// 1.0.158 2025-5-27
// uart.c增加一些log输出
// 1.0.157 2025-5-27
// 修改uart.c打开串口的逻辑，因为A20是ttyS3，但是A133是ttyS7，所以不能只考虑A20了，需要考虑A133
// 1.0.156 2025-5-26
// pd_set_temperature_override改在上电之前设置
// 1.0.155 2025-5-23
// 取消 monitorThread 中 pd_set_recirc_override 的调用
// 1.0.154 2025-5-23
// 增加max_freq, recovery_time, between_pages_time值的输出
// 1.0.153 2025-5-22
// 在 monitorThread 中增加 pd_set_recirc_override调用，设为recovery，200%
// 1.0.152 2025-5-22
// 取消 monitorThread 中的 pd_set_temperature_override 调用
// 1.0.151 2025-5-22
// 在monitorThread函数中追加pd_set_over_energy_override的地方，换成pd_set_temperature_override，设置35度，看看能否执行成功
// 1.0.150 2025-5-20
// 在monitorThread函数中追加pd_set_over_energy_override
// 1.0.149 2025-5-20
// 暂时关闭所有set_temperature_override
// 1.0.148 2025-5-20
// 在monitorThread中，追加一个 pd_set_temperature_override 调用，以确认该调用是否每次都会被拒绝执行
// 1.0.147 2025-5-20
// 在monitorThread中，追加一个 pd_get_temperature_override 调用，以确认设置的目标温度
// 1.0.146 2025-2-24
// 大幅修改分头的有效性管理，具体内容包括：
// 1. 增加一个分别保存头有效性的数据变量
//      int RunningState[3];             // [0]: IDS状态； [1]: PEN0状态； [2]: PEN1状态
//    当相应的打印头出现错误时，标记相应的状态为false
// 2. 增加一个上传接口，getRunningState共apk调用取得相应状态
// 3. 修改PenArg参数的传入机制，由initPd函数传入，作为一个全局变量保存，取消分别由DoPairing，DoOverrid和StartMonitor函数传入
// 4. 增加一个StopMonitor接口，用来停止守护线程，这个实际上可以用来实现apk在不重新开机的情况下重新初始化，但单枪并未支持，更换打印头的配置必须重新启动
// 1.0.145 2025-2-20
// 给几处调用pd_sc_read_oem_field的函数入口增加了log输出，确认导致pd_sc_read_oem_field执行时发生14号错误的原因是什么
// 1.0.144 2025-2-19
// 为getLocalInk和downLocal函数追加head参数，当head==0时，代表ids，当head>=1时，代表pen0,...
// 1.0.143 2025-2-18
// 注释掉ids.c中的注释，已经service.c中有关UART的注释，以减少log输出量。修改守护线程的休眠时间
// #define POLL_SEC 1 -> 2
// #define INK_POLL_SEC 2 -> 4

// 1.0.142 2025-2-10
// 增加一个是否加热的控制属性(EnableWarming)和控制函数
// 1.0.141 2025-2-7
// 暂时修改PowerOn函数，增加一个设定温度的参数
// 1.0.140 2025-1-17
// Java_com_purge修改为，根据PD的SC中系统保存的purge状态和OEM保存的purge状态，当两者有一个为1时，判定为做过purge，不再purge，并且写入OEM区1。当两个Slot都purge过时，不再执行purge返回错误。
// 执行purge后，无论pd_start_purging函数返回成功还是失败，均按着成功处理，写入OEM，并且返回成功
// 1.0.139 2025-1-17
// 修改Java_com_purge的清洗方法，改为两个slot同时清洗
// 1.0.138 2025-1-16
// 修改purge_complete_mark_oem的定义，原来错误的定义为bool了，因该是uint32_t型(因为是保存在PD_SC_OEM_RW_FIELD_1中）
// 1.0.137 2025-1-16
// 整理Java_com_purge代码，原来的判断成否是看pd_check_ph，这个里面因为要再次执行pd_get_print_head_status，所以当pd_start_purging返回失败的时候，可能反应的结果是pd_get_print_head_status的结果，这个结果可能显示没有问题
// 1.0.136 2025-1-16
// 取消1.0.134增加的mutex
// 1.0.135 2025-1-16
// 给monitorThread传递的参数，改为通过内部static变量传递，因为使用pthread_create函数传递的参数有时候是错的，原因不明（现在这个方式是否可行也得确认）
// 1.0.134 2025-1-16
// 在守护线程和purge操作之间使用mutex进行排斥，以后可能其它的临界处理也需要这么做
// 1.0.133 2025-1-15
// 修改PDSmartCardStatus数据结构，增加一个purge_complete_mark_oem数据项，用来反应写在PD的SC卡的OEM区中的purge的执行状态。同时，在hp22mm.c的purge函数中，增加写purge状态到OEM区的功能，在pd_sc_get_status中增加从OEM区读这个状态，并且返回给测试页面的功能
// 1.0.132 2025-1-15
// 取消规避ERROR_DEVICE_SEEN_BEFORE错误，恢复原样。同时对DoPairing和DoOverrides函数进行修改，使之支持多个头
// 1.0.131 2025-1-14
// 在DoPairing函数中，暂时规避ERROR_DEVICE_SEEN_BEFORE错误
// 1.0.130 2025-1-13
// 取消固定一个打印头(sPenIdx)，改为可灵活使用两个打印头
// 1.0.129 2025-1-11
// 在守护线程中追加pd_enable_warming，尝试持续加热
// 1.0.128 2025-1-2
// 1.0.127的修改，改为8(6%)
// 1.0.127 2025-1-2
// pd_set_voltage_override设10（10%）临时改在PowerOn函数中
// 1.0.126 2024-12-31
// monitorThread中pd_get_print_head_status返回状态2时，函数本身并不返回错误。因此不能导致尝试重启。修改为忽略函数返回值，只要状态是2（或者1）都尝试上电
// 1.0.125 2024-12-31
// 临时取消pd_disable_warming功能
// 1.0.124 2024-12-31
// 临时在守护线程中添加读取打印头温度的功能（pd_get_temperature）。并且暂时取消1.0.122和1.0.123版本增加的电压调整的尝试
// 1.0.123 2024-12-27
// pd_set_voltage_override设10（10%）
// 1.0.122 2024-12-27
// 初始化之后，设置pd_set_voltage_override，暂时为8（+6%），在MonitorThread中读取确认
// 1.0.121 2024-12-20
// 在上电之前先检查温度是否到位，否则等待，最多5秒
// 1.0.120 2024-12-18
// monitorThread中暂时取消pd_set_temperature_override，可能引起14号错误。pd_power_on成功后也取消该函数
// 1.0.119 2024-12-18
// Java_com_pd_get_print_head_status函数中，如果state和error返回错误，但是函数本身没有发挥错误，也给ERR_STRING设置错误信息
// 1.0.118 2024-12-11
// 墨水最大值的定位原则：由于根据实验，4列同时打印，打印200次的时候，使用了全部775ml中的10ml，因此，最大值设置为15500会与实际情况同步。同时考虑到可能会有1列单独，2列，3列打印的情况，因此将最大值按1列打印为标准设置，乘以4（即15500*4）
// 当列数大于1时，一次减记要交列数份的值，因此修改downLocal的参数
// 1.0.117 2024-12-10
// 内部锁值最大值设为15500，这样会与全部墨量为775克的进度（实测内部打印计数为200时，消耗10g墨水）同步
// 1.0.116 2024-12-10
// 22mm本来应该使用内部的统计系统统计墨水的消耗情况，但是暂时看似乎没有动作，因此启用独自的统计系统，计数值保存在OEM_RW区域
// 1.0.115 2024-12-06
// 在1.0.114的基础上，增加pd_disable_warming，并且将这两个调用同时放在守护线程中调用
// 1.0.114 2024-12-06
// 取消1.0.108之后进行的左右尝试，改为在上电（pd_power_on）成功后强制关闭加热功能（pd_set_temperature_override, 暂时不用pd_disable_warming）
// 1.0.113 2024-11-16
// 1.0.112增加的enable_warming前再加上set_temperature_override函数
// 1.0.112 2024-11-15
// 取消1.0.111的修改，不再设目标温度，取消pd_calibrate_pulsewidth，在守护线程中增加enable_warming和测温函数
// 1.0.111 2024-11-15
// 取消1.0.109的修改，添加在power_on时设置目标温度为55度
// 1.0.110 2024-11-15
// 取消守护线程中get_print_head_status返回15号错误后的尝试power_on
// 1.0.109 2024-11-15
// 取消测温函数，power_on时调用pd_calibrate_pulsewidth
// 1.0.108 2024-11-15
// 在守护线程中，增加测温函数
// 1.0.107 2024-11-14
// 先判断SlotA和B哪个没清洗，没清洗的清洗
// 1.0.106 2024-11-14
// 清洗B两次，清洗A两次
// 1.0.105 2024-11-14
// 仅清洗B
// 1.0.104 2024-11-14
// 清洗A两次，清洗B两次
// 1.0.103 2024-11-14
// 先清洗B，再清洗A
// 1.0.102 2024-11-14
// 仅清洗A
// 1.0.101 2024-11-14
// 再次暂时不清洗SlotA
// 1.0.100 2024-11-14
// 修改PD的purge功能，取消清洗时停止守护线程的做法
// 1.0.099 2024-11-13
// 增加PD的purge功能
// 1.0.098 2024-11-12
// 再临时取消_pd_fpga_setreset( instance, 1);
// 1.0.097 2024-11-12
// 临时取消_pd_fpgaflash_writeprotect
// 1.0.096 2024-11-12
// 修改FPGA.s19升级前的初始化流程
// 1.0.095 2024-11-12
// 修改守护线程bug
// 1.0.094 2024-11-11
// 升级IDS，PD和FPGA固件时，使守护线程避让
// 1.0.093 2024-11-11
// 1.0.092 2024-11-10
// 大幅修改22MM的PD_Power_On, PD_Power_Off, void CmdDepressurize和CmdPressurize()的操作方法，并且大幅修改了设备状态的管理
// 1.0.091 2024-11-9
// 取消1.0.090的修改。将SECURE_INK_POLL_SEC恢复为20，取消对INK_POLL_SEC及SECURE_INK_POLL_SEC的检查，即，每秒都全部执行一遍
// 1.0.090 2024-11-9
// 取消1.0.088的修改。将SECURE_INK_POLL_SEC暂时修改为2，增加PD与IDS交换数据的频率
// 1.0.089 2024-11-7
// _print_thread当中读ids_get_supply_status后，输出supply_status.consumed_volume, supply_info.usable_vol的log，以便确认
// 1.0.088 2024-11-7
// 修改当 守护线程执行pd_get_print_head_status出现超时的时候，尝试另外一个pd的操作和另外一个ids的操作。并且uart中的切换PE4的函数增加一个读取PE4，已确认切换结果的操作
// 1.0.087 2024-11-6
// 取消pd_get_print_head_status函数中的严格判断，原设计仅为开机初始化使用，正常运行时调用会导致报错（其实是正确）
// 1.0.086 2024-10-28
// _print_thread中放开ids_get_supply_status函数的调用，否则可能会不能实时更新状态数据，尤其是supply_status的consumed_volume不被更新
// 1.0.085 2024-10-9
// 修改bug，GetAndProcessInkUse函数中，当执行失败时，使用的exit函数没有更改为return，导致发生失败时进程整体退出，貌似apk崩溃
// 1.0.084 2024-9-28
// 修改了_print_thread打印头监视线程，尽量贴近例子程序的实现方法
// 1.0.083 2024-9-26
// 1. apk方面，将原来开始打印时执行pd_power_on，开启打印监视线程，改为初始化时执行。并且，停止打印时也不再pd_power_off，也不关闭打印监视线程
// 2. 生成监视打印线程时检查线程是否存在，如果存在就不再生成，主要是为了防止多次初始化时会生成多个线程
// 3. 将原来的_startPrint和_stopPrint函数改名为pdPowerOn和pdPowerOff
// 1.0.082 2024-7-25
// 升级到新代码Demo_05_03.
// 1. New code to set SPI quad-enable bit (NOTE: FOR MACRONIX SPI FLASH ONLY!!)
//    Set the Quad SPI enable bit in the status read from status register
// 2. 增加pd_enable_warming和pd_disable_warming接口及其它相关辅助代码（但是这个API似乎还没有被真正使用）
//    service.c, service.h, print_head_driver_ifc.h, extension.h etc
// 1.0.081 2024-7-8
// 追加一个接口函数getErrString（内部：Java_com_GetErrorString），用来向apk返回开始、停止打印时发生的错误
// 1.0.080 2024-6-7
// 单纯版本更新
// 1.0.079 2024-6-5
// res.res_size返回0时修改为返回错误，原来HP代码未作处理
// 1.0.078 2024-6-4
// 修改了一些log输出语句
// 1.0.077 2024-6-1
// print_head_driver.c 中的 #define FPGA_FLASH_SIZE (2 * 1024 *1024)修改为#define FPGA_FLASH_SIZE (3 * 1024 *1024)
// 1.0.076 2024-4-8
//    1. 在_print_thread函数中，增加定期读取supplay的status
//       ids_get_supply_status(IDS_INSTANCE, sIdsIdx, &supply_status);
//    2. 增肌两个接口函数Java_com_GetConsumedVol(JNIEnv *env, jclass arg) 和 Java_com_GetUsableVol(JNIEnv *env, jclass arg) {
//       用来读取IDS的墨水总量和墨水消费量

// 1.0.075 2024-4-3
//    加压超时改为两分钟（PRESSURIZE_SEC=120）
// 1.0.074 2024-4-3
//    修改PDPowerOn和PDPowerOff函数名，改为StartPrint和StopPrint，对应于apk的函数名为_startPrint和_stopPrint
// 1.0.073 2024-3-28
//    追加打印守护进程，内容为定时（20秒）GetAndProcessInkUse
// 1.0.072 2024-3-22
//    SPI的访问转移到img的内核层，原来通过应用层spidev的访问取消，临时的修改，还没有全部完成
// 1.0.071 2024-3-17
//    修改SPIMessage函数的数据区，从原来的每个字节一个数据结构修改为所有数据使用一个数据结构，这样可以节省为每个字节准备数据结构的时间，也可以节约对内存的使用
// 1.0.070 2024-3-15
//    增加
//        {"readRegisters",	    "()[I",	    (void *)Java_com_ReadRegisters},
//        {"writeSettings",	    "([I)I",	    (void *)Java_com_WriteSettins},
//        {"writeImage",	    "(III[B)I",	    (void *)Java_com_WriteImage},
//        {"start",	    "([I)I",	    (void *)Java_com_LaunchPrint},
//    以实现从apk的自动控制打印
// 1.0.069 2024-3-8
//    修改几个参数的值
// 1.0.068 2024-1-31
//    1.删除以前写入FPGA的Flash的部分代码，因为这部分代码已经移到img里面实现了，这里不需要了，apk已经很久之前去掉了该连接
//    2.放开startPrint中读取正式的image.bin的代码
// 1.0.067 2024-1-30 追加一个SpiTest

int sIdsIdx = 1;
// H.M.Wang 2025-1-13 取消固定一个打印头，改为可灵活使用两个打印头
//int sPenIdx = 0;
// End of H.M.Wang 2025-1-13 取消固定一个打印头，改为可灵活使用两个打印头

// static pthread_mutex_t mutex;
// H.M.Wang 2025-6-9 修改为log可设置为输出和不输出
char gOutputLog = 1;
// End of H.M.Wang 2025-6-9 修改为log可设置为输出和不输出

void CmdDepressurize();
int CmdPressurize(jboolean async);
/***********************************************************
 *  Customization
 *f
 *  Settings for basic customization.
 ***********************************************************/
#define I2C_DEVICE "/dev/i2c-1"

void IDSCallback(int ids, int level, const char *message) {
    switch (level) {
        case 1:
            LOGE(">>> IDS %d WARNING: %s\n", ids, message);
            break;
        case 2:
            LOGE(">>> IDS %d ERROR: %s\n", ids, message);
            break;
        default:
            LOGE(">>> IDS %d: %s\n", ids, message);
    }
}

extern char ERR_STRING[];

static pthread_t sMonitorThread = (pthread_t)NULL;
typedef enum {
    PD_POWER_STATE_OFF           =  0,
    PD_POWER_STATE_ON            =  1
} PD_Power_State_t;

typedef enum {
    AIR_STATE_PUMP_OFF           =  0,
    AIR_STATE_LAUNCH_PUMP        =  1,
    AIR_STATE_PUMPING            =  2,
    AIR_STATE_PUMPED             =  3
} Air_Pump_State_t;

static PD_Power_State_t PD_Power_State = PD_POWER_STATE_OFF;
static Air_Pump_State_t Air_Pump_State = AIR_STATE_PUMP_OFF;
static bool CancelMonitor = true;

// POLL_SEC - how often the background thread runs
#define POLL_SEC 2
// INK_POLL_SEC - when pen is On, ink is polled at this frequency for PILS use
#define INK_POLL_SEC 4
// SECURE_INK_POLL_SEC - secure ink is polled at this frequency (must be < 60 seconds)
#define SECURE_INK_POLL_SEC 20

#define SUPPLY_PRESSURE 6.0
#define PRESSURIZE_SEC 120  // 两分钟

#define LED_R 0
#define LED_Y 1
#define LED_G 2
#define LED_OFF 0
#define LED_ON 1
#define LED_BLINK 2

static SupplyStatus_t supply_status;
static SupplyInfo_t supply_info;
static PrintHeadStatus print_head_status;
static PDSmartCardInfo_t pd_sc_info;
static PDSmartCardStatus pd_sc_status;
static volatile int PenArg;             // 装载打印头的状态。=1:仅装载Pen0， =2:仅装载Pen1， =3:两个均装载
#define PEN0_INSTALLED 1
#define PEN1_INSTALLED 2
#define BOTH_PEN_INSTALLED 3
// H.M.Wang 2025-2-23 增加分别管理个Pen和IDS错误状态的功能
int RunningState[3];             // [0]: IDS状态； [1]: PEN0状态； [2]: PEN1状态
#define IDS_STATE      0
#define PEN0_STATE     1
#define PEN1_STATE     2
#define STATE_VALID    1
#define STATE_INVALID  0
// End of H.M.Wang 2025-2-23
static volatile int EnableWarming = 1;

void *monitorThread(void *arg) {
    int nonsecure_sec = 0;
    int secure_sec = 0;
    float ink_weight;
//    int degree_c;
    int limit_sec;

    LOGD("[Async] Monitor thread started.");
    uint8_t sc_result;

    while (!CancelMonitor) {
        // sleep until next poll, then increment time counters
        sleep(POLL_SEC);

//        pthread_mutex_lock(&mutex);

        nonsecure_sec += POLL_SEC;
        secure_sec += POLL_SEC;

//        LOGD("[Async] Air_Pump_State = %d, PD_Power_State = %d\n", Air_Pump_State, PD_Power_State);

        // 已经加压成功以后，监视压力变化，如果过低则重新开始加压
        RunningState[IDS_STATE] = STATE_VALID;
        if(Air_Pump_State == AIR_STATE_PUMPED) {
            if (IDS_GPIO_ReadBit(sIdsIdx, GPIO_I_AIR_PRESS_LOW)) {
                sprintf(ERR_STRING, "WARNING: Air press low\n");
                LOGE("[Async]WARNING: Air press low\n");
                RunningState[IDS_STATE] = STATE_INVALID;
                Air_Pump_State = AIR_STATE_LAUNCH_PUMP;
            }
        }
        // 正在加压的过程当中监视压力变化，如果超过PRESSURIZE_SEC秒后仍然压力过低，则判断为失败，重新尝试加压。如果压力满足要求，则标注为加压成功
        if(Air_Pump_State == AIR_STATE_PUMPING) {
            if(IDS_GPIO_ReadBit(sIdsIdx, GPIO_I_AIR_PRESS_LOW)) {
                if (--limit_sec <= 0) {
                    sprintf(ERR_STRING, "ERROR: Supply not pressurized in %d seconds\n", PRESSURIZE_SEC);
                    LOGE("[Async]ERROR: Supply not pressurized in %d seconds\n", PRESSURIZE_SEC);
                    RunningState[IDS_STATE] = STATE_INVALID;
                    IDS_GPIO_ClearBits(sIdsIdx, COMBO_INK_AIR_PUMP_ALL);     // pump disabled; ink/air valves closed
                    IDS_MonitorOff(sIdsIdx);
                    IDS_LED_Off(sIdsIdx, LED_Y);
                    Air_Pump_State = AIR_STATE_LAUNCH_PUMP;
                }
            } else {
                LOGD("[Async]Opening ink valves...\n");
                IDS_GPIO_SetBits(sIdsIdx, COMBO_INK_BOTH);        // ink valve On/Hold
                sleep(1);
                IDS_GPIO_ClearBits(sIdsIdx, GPIO_O_INK_VALVE_ON); // turn Off ink valve (leave Hold)
//    IDS_MonitorPILS(sIdsIdx);
                Air_Pump_State = AIR_STATE_PUMPED;
            }
        }
       // 如果接收到启动加压的命令，则开始加压，将当前状态修改为正在加压，从而监视压力变化
        if(Air_Pump_State == AIR_STATE_LAUNCH_PUMP) {
            IDS_MonitorPressure(sIdsIdx);
            IDS_DAC_SetSetpointPSI(sIdsIdx, SUPPLY_PRESSURE);        // set pressure target
            IDS_GPIO_ClearBits(sIdsIdx, COMBO_INK_BOTH);      // ink Off
            IDS_GPIO_SetBits(sIdsIdx, COMBO_AIR_PUMP_ALL);    // pump enabled; air valve On/Hold
            IDS_LED_On(sIdsIdx, LED_Y);
            LOGD("[Async]Pressurizing Supply %d to %.1f psi...\n", sIdsIdx, SUPPLY_PRESSURE);

            IDS_GPIO_ClearBits(sIdsIdx, GPIO_O_AIR_VALVE_ON); // turn Off air valve (leave Hold)
            limit_sec = PRESSURIZE_SEC;
            LOGD("[Async]Waiting on pumps...\n");

            Air_Pump_State = AIR_STATE_PUMPING;
        }

        // 如果读取IDS状态失败，则睡眠一秒后重新尝试，不做其它操作
        if (ids_check("ids_get_supply_status", ids_get_supply_status(IDS_INSTANCE, sIdsIdx, &supply_status))) {
            continue;
        }

        // 当已经给PD上点成功的情况下，
        if(PD_Power_State == PD_POWER_STATE_ON) {
            int penNum;
            if(PenArg == BOTH_PEN_INSTALLED) {
                penNum = 2;
            } else if(PenArg == PEN0_INSTALLED || PenArg == PEN1_INSTALLED) {
                penNum = 1;
            } else {
                LOGE("Monitor thread starting failed. Wrong parametres(arg=%d)", PenArg);
                continue;
            }

            int penIndexs[penNum];
            if(PenArg == PEN0_INSTALLED)
                penIndexs[0] = 0;
            else if(PenArg == PEN1_INSTALLED)
                penIndexs[0] = 1;
            else if(PenArg >= BOTH_PEN_INSTALLED) {
                penIndexs[0] = 0;
                penIndexs[1] = 1;
            }

            for(int i=0; i<penNum; i++) {
                RunningState[PEN0_STATE+i] = STATE_VALID;
                // 如果读取PD状态失败，当失败后的状态为掉电的话，尝试重新上电
                pd_check_ph("pd_get_print_head_status", pd_get_print_head_status(PD_INSTANCE, penIndexs[i], &print_head_status), penIndexs[i]);
                if(print_head_status.print_head_state == PH_STATE_POWERED_OFF || print_head_status.print_head_state == PH_STATE_PRESENT) {
                    pd_check_ph("pd_power_on", pd_power_on(PD_INSTANCE, penIndexs[i]), penIndexs[i]);
                }

                uint8_t v;
    // 暂时取消这个临时错误            pd_check_ph("pd_get_voltage_override", pd_get_voltage_override(PD_INSTANCE, penIndexs[i], &v), penIndexs[i]);
                pd_check_ph("pd_get_temperature", pd_get_temperature(PD_INSTANCE, penIndexs[i], &v), penIndexs[i]);
                pd_check_ph("pd_get_temperature_override", pd_get_temperature_override(PD_INSTANCE, penIndexs[i], &v), penIndexs[i]);
//                pd_set_recirc_override(PD_INSTANCE, penIndexs[i], 0, 13);

                if(EnableWarming)
                    pd_check_ph("pd_enable_warming", pd_enable_warming(PD_INSTANCE, penIndexs[i]), penIndexs[i]);
                else
                    pd_check_ph("pd_disable_warming", pd_disable_warming(PD_INSTANCE, penIndexs[i]), penIndexs[i]);

                // 当失败后的状态为还处于上电状态的话，忽略发生的错误，尝试做IDS与PD的数据交换
                if (print_head_status.print_head_state == PH_STATE_POWERED_ON) {
                    // NON-SECURE ink use (for PILS algorithm)
                    if (nonsecure_sec >= INK_POLL_SEC) {
                        nonsecure_sec = 0;
                        // NON-SECURE ink use (for PILS algorithm)
                        ink_weight = GetInkWeight(penIndexs[i]);
                        if (ink_weight < 0) {
                            LOGD("GetInkWeight failed.");
                        } else {
    //                    if (ink_weight > 0) ProcessInkForPILS(ink_weight);
                            LOGD("GetInkWeight = %f", ink_weight);
                        }
                    }

                    // SECURE ink use
                    if (secure_sec >= SECURE_INK_POLL_SEC) {
                        secure_sec = 0;
                        if (GetAndProcessInkUse(penIndexs[i], sIdsIdx) < 0) {
                            LOGD("GetAndProcessInkUse failed.");
                        } else {
                            LOGD("GetAndProcessInkUse succeeded.");
                        }
                    }
                }
            }
        }
//        pthread_mutex_unlock(&mutex);
    }
    return (void*)NULL;
}

JNIEXPORT jint JNICALL Java_com_EnableWarming(JNIEnv *env, jclass arg, jint enable) {
    EnableWarming = enable;
    return 0;
}

JNIEXPORT jint JNICALL Java_com_StartMonitor(JNIEnv *env, jclass arg) {
    CancelMonitor = false;

    if(NULL == sMonitorThread) {
        if (pthread_create(&sMonitorThread, NULL, monitorThread, NULL)) {
            sMonitorThread = (pthread_t) NULL;
            LOGE("ERROR: pthread_create() of monitorThread failed\n");
            return -1;
        }
    }
    return 0;
}

JNIEXPORT jint JNICALL Java_com_PDPowerOn(JNIEnv *env, jclass arg, jint penIndex, jint temp) {
// H.M.Wang 2024-12-20 在上电之前先检查温度是否到位，否则等待，最多5秒
//    PrintHeadStatus status;
//    PDResult_t pd_r;
//    for(int i=0; i<5; i++) {
//        pd_r = pd_get_print_head_status(PD_INSTANCE, penIndex, &status);
//        if(pd_r == PD_OK && (status.print_head_state == PH_STATE_POWERED_ON || status.print_head_state == PH_STATE_PRESENT) && status.print_head_error == PH_NO_ERROR) break;
//        sleep(1);
//    }
// End of H.M.Wang 2024-12-20 在上电之前先检查温度是否到位，否则等待

    pd_check_ph("pd_set_voltage_override", pd_set_voltage_override(PD_INSTANCE, penIndex, 8), penIndex);
    if(temp < 30)
        pd_set_temperature_override(PD_INSTANCE, penIndex, 0);
    else if(temp < 35)
        pd_set_temperature_override(PD_INSTANCE, penIndex, 1);
    else if(temp < 40)
        pd_set_temperature_override(PD_INSTANCE, penIndex, 2);
    else if(temp < 45)
        pd_set_temperature_override(PD_INSTANCE, penIndex, 3);
    else if(temp < 50)
        pd_set_temperature_override(PD_INSTANCE, penIndex, 4);
    else if(temp < 55)
        pd_set_temperature_override(PD_INSTANCE, penIndex, 5);
    else if(temp < 60)
        pd_set_temperature_override(PD_INSTANCE, penIndex, 6);
    else if(temp < 65)
        pd_set_temperature_override(PD_INSTANCE, penIndex, 7);
    else if(temp < 70)
        pd_set_temperature_override(PD_INSTANCE, penIndex, 8);

    if (pd_check_ph("pd_power_on", pd_power_on(PD_INSTANCE, penIndex), penIndex)) {
        PD_Power_State = PD_POWER_STATE_OFF;
        return -1;
    } else {
        PD_Power_State = PD_POWER_STATE_ON;
    }
    return 0;
}

JNIEXPORT jint JNICALL Java_com_PDPowerOff(JNIEnv *env, jclass arg, jint penIndex) {
    PD_Power_State = PD_POWER_STATE_OFF;
    if (pd_check_ph("pd_power_off", pd_power_off(PD_INSTANCE, penIndex), penIndex)) {
        return -1;
    }
    return 0;
}

// H.M.Wang 2024-11-13 追加22mm打印头purge功能
JNIEXPORT jint JNICALL Java_com_purge(JNIEnv *env, jclass arg, jint penIndex) {
    jint ret = -1, slot = 0x03;
    PDSmartCardStatus __pd_sc_status;
    uint8_t sc_result;
    PDResult_t pd_r;

    LOGD("print_head_status.print_head_state = %d", print_head_status.print_head_state);

//    pthread_mutex_lock(&mutex);

    if(print_head_status.print_head_state != PH_STATE_POWERED_ON) {
        pd_check_ph("pd_power_on", pd_power_on(PD_INSTANCE, penIndex), penIndex);
    }

    if(print_head_status.print_head_state == PH_STATE_POWERED_ON) {
        pd_sc_get_status(PD_INSTANCE, penIndex, &__pd_sc_status, &sc_result);
        pd_sc_read_oem_field(PD_INSTANCE, penIndex, PD_SC_OEM_RW_FIELD_1, &(__pd_sc_status.purge_complete_mark_oem), &sc_result);
        if(__pd_sc_status.purge_complete_slot_b || (__pd_sc_status.purge_complete_mark_oem & 0x02)) {
            slot &= (~(0x02));
        }
        if(__pd_sc_status.purge_complete_slot_a || (__pd_sc_status.purge_complete_mark_oem & 0x01)) {
            slot &= (~(0x01));
        }
        if(slot > 0) {
            LOGD("Pen%d purge slot %d", penIndex, slot);
            pd_r = pd_start_purging(PD_INSTANCE, penIndex, slot-1);
            if(PD_OK == pd_r) {
                LOGD("Pen%d purge done", penIndex);
            } else {
                LOGE("Pen%d purge failed", penIndex);
                pd_check_ph("pd_start_purging", pd_r, penIndex);
            }

            // Process a secure ink message to clear system
            GetAndProcessInkUse(penIndex, sIdsIdx);

            // Check status after
            if (pd_check_ph("pd_sc_get_status",pd_sc_get_status(PD_INSTANCE, penIndex, &pd_sc_status, &sc_result), penIndex) == PD_OK && sc_result == 0) {
                LOGD("Slot B = %s \n", (pd_sc_status.purge_complete_slot_b ? "Purge Complete" : "NOT PURGED"));
                LOGD("Slot A = %s \n", (pd_sc_status.purge_complete_slot_a ? "Purge Complete" : "NOT PURGED"));
            }

            if (pd_check_ph("pd_sc_write_oem_field", pd_sc_write_oem_field(PD_INSTANCE, penIndex, PD_SC_OEM_RW_FIELD_1, 3, &sc_result), penIndex) == PD_OK && sc_result == 0) {
                LOGD("Purged mark written to PD_SC_OEM_RW_FIELD_1(%d)\n", 3);
            }
        } else {
            LOGD("Purge already done");
        }
    }

//    pthread_mutex_unlock(&mutex);

    return slot;
}
// End of H.M.Wang 2024-11-13 追加22mm打印头purge功能

JNIEXPORT jstring JNICALL Java_com_GetErrorString(JNIEnv *env, jclass arg) {
    return (*env)->NewStringUTF(env, ERR_STRING);
}

JNIEXPORT jint JNICALL Java_com_GetRunningState(JNIEnv *env, jclass arg, jint index) {
    if(index >= IDS_STATE && index <= PEN1_STATE) {
        return RunningState[index];
    }
    return 0;
}

JNIEXPORT jint JNICALL Java_com_GetConsumedVol(JNIEnv *env, jclass arg) {
    return supply_status.consumed_volume;
}

JNIEXPORT jint JNICALL Java_com_GetUsableVol(JNIEnv *env, jclass arg) {
    return supply_info.usable_vol;
}

// H.M.Wang 2024-12-10 22mm本来应该使用内部的统计系统统计墨水的消耗情况，但是暂时看似乎没有动作，因此启用独自的统计系统，计数值保存在OEM_RW区域
JNIEXPORT jint JNICALL Java_com_getLocalInk(JNIEnv *env, jclass arg, jint head) {
    LOGI("Enter %s (head = %d)", __FUNCTION__, head);

    uint32_t value;
    if(head == 0) {
        IDSResult_t ids_r = ids_read_oem_field(IDS_INSTANCE, sIdsIdx, OEM_RW_1, &value);
        if (ids_check("ids_read_oem_field", ids_r)) return(-1);
        LOGD("IDS_OEM_RW_1 = %d", value);
    } else {
        uint8_t pd_sc_result;
        PDResult_t pd_r = pd_sc_read_oem_field(PD_INSTANCE, head-1, PD_SC_OEM_RW_FIELD_1, &value, &pd_sc_result);
        if (pd_check_ph("pd_sc_read_oem_field", pd_r, head-1) || pd_sc_result != 0) return(-1);
        LOGD("PD_OEM_RW_1[%d] = %d", head-1, value);
    }
    return value;
}

#define MAX_BAG_INK_VOLUME_MAXIMUM              (15500 * 4)

JNIEXPORT jint JNICALL Java_com_downLocal(JNIEnv *env, jclass arg, jint head, jint count) {
    LOGI("Enter %s (head = %d, value = %d)", __FUNCTION__, head, count);
    uint32_t value;

    if(head == 0) {
        IDSResult_t ids_r = ids_read_oem_field(IDS_INSTANCE, sIdsIdx, OEM_RW_1, &value);
        if (ids_check("ids_read_oem_field", ids_r)) return(-1);

        value += count;

        if(value >= MAX_BAG_INK_VOLUME_MAXIMUM) {
            ids_r = ids_set_out_of_ink(IDS_INSTANCE, sIdsIdx);
            if (ids_check("ids_set_out_of_ink", ids_r)) return(-1);
        }

        ids_r = ids_write_oem_field(IDS_INSTANCE, sIdsIdx, OEM_RW_1, value);
        if (ids_check("ids_write_oem_field", ids_r)) return(-1);

        ids_r = ids_flush_smart_card(IDS_INSTANCE, sIdsIdx);
        if (ids_check("ids_flush_smart_card", ids_r)) return(-1);
    } else {
        uint8_t pd_sc_result;
        PDResult_t pd_r = pd_sc_read_oem_field(PD_INSTANCE, head-1, PD_SC_OEM_RW_FIELD_1, &value, &pd_sc_result);
        if (pd_check_ph("pd_sc_read_oem_field", pd_r, head-1) || pd_sc_result != 0) return(-1);

        value += count;

        pd_r = pd_sc_write_oem_field(PD_INSTANCE, head-1, PD_SC_OEM_RW_FIELD_1, value, &pd_sc_result);
        if (pd_check_ph("pd_sc_write_oem_field", pd_r, head-1) || pd_sc_result != 0) return(-1);
    }

    return 0;
}
// End of H.M.Wang 2024-12-10 22mm本来应该使用内部的统计系统统计墨水的消耗情况，但是暂时看似乎没有动作，因此启用独自的统计系统，计数值保存在OEM_RW区域

static IdsSysInfo_t ids_sys_info;

JNIEXPORT jint JNICALL Java_com_hp22mm_init_ids(JNIEnv *env, jclass arg, jint idsIndex) {
    LOGI("Initializing IDS%d....\n", idsIndex);

    IDSResult_t ids_r;

    sIdsIdx = idsIndex;

    RunningState[IDS_STATE] = STATE_INVALID;
    ids_r = ids_lib_init();
    if (ids_check("ids_lib_init", ids_r)) return -1;
    ids_r = ids_init(IDS_INSTANCE);
    if (ids_check("ids_init", ids_r)) return -1;
    ids_r = ids_info(IDS_INSTANCE, &ids_sys_info);
    if (ids_check("ids_info", ids_r)) return -1;

    LOGD("FW Rev = %d.%d\nFPGA Rev = %d.%d\nBoard Rev bd1 = %d, bd0 = %d, bd = %d\nStatus = %d\nBootloader = %d.%d\nBoard ID = %d",
            ids_sys_info.fw_major_rev, ids_sys_info.fw_minor_rev,
            ids_sys_info.fpga_major_rev, ids_sys_info.fpga_minor_rev,
            ids_sys_info.board_rev_bd1, ids_sys_info.board_rev_bd0, ids_sys_info.board_rev_bd,
            ids_sys_info.status,
            ids_sys_info.bootload_major, ids_sys_info.bootload_minor,
            ids_sys_info.board_id);

    if (IDS_Init(sIdsIdx, IDSCallback)) {
        LOGE("IDS_Init failed\n");
        return -1;
    }
    RunningState[IDS_STATE] = STATE_VALID;
    return 0;
}

JNIEXPORT jstring JNICALL Java_com_ids_get_sys_info(JNIEnv *env, jclass arg) {
    char strTemp[256];

    sprintf(strTemp, "Hp22mm Lib REV. = %s\nIDS%d\nFW Rev = %d.%d\nFPGA Rev = %d.%d\nBoard Rev bd1 = %d, bd0 = %d, bd = %d\nStatus = %d\nBootloader = %d.%d\nBoard ID = %d",
            VERSION_CODE,
            sIdsIdx,
            ids_sys_info.fw_major_rev, ids_sys_info.fw_minor_rev,
            ids_sys_info.fpga_major_rev, ids_sys_info.fpga_minor_rev,
            ids_sys_info.board_rev_bd1, ids_sys_info.board_rev_bd0, ids_sys_info.board_rev_bd,
            ids_sys_info.status,
            ids_sys_info.bootload_major, ids_sys_info.bootload_minor,
            ids_sys_info.board_id);

    return (*env)->NewStringUTF(env, strTemp);
}

static PDSystemStatus pd_system_status;

JNIEXPORT jint JNICALL Java_com_hp22mm_init_pd(JNIEnv *env, jclass arg, jint penArg) {
    LOGI("Initializing PD(PenArg=%d)....\n", penArg);

    PDResult_t pd_r;

    PenArg = penArg;
//    sPenIdx = penIndex;

    pd_r = pd_lib_init();
    if (pd_check("pd_lib_init", pd_r)) return -1;
    pd_r = pd_init(PD_INSTANCE);
    if (pd_check("pd_init", pd_r)) return -1;
    pd_r = pd_get_system_status(PD_INSTANCE, &pd_system_status);
    if (pd_check("pd_get_system_status", pd_r)) return -1;
//暂时取消这个临时措施    pd_r = pd_set_voltage_override(PD_INSTANCE, penIndex, 10);
//暂时取消这个临时措施    if (pd_check_ph("pd_set_voltage_override", pd_r), penIndex) return -1;

    LOGD("uC FW REV. = %d.%d\n", pd_system_status.fw_rev_major, pd_system_status.fw_rev_minor);
    LOGD("Bootloader REV = %d.%d\n", pd_system_status.boot_rev_major, pd_system_status.boot_rev_minor);
    LOGD("FPGA REV = %d.%d\n", pd_system_status.fpga_rev_major, pd_system_status.fpga_rev_minor);
    LOGD("BLUR(PD PCA) REV = %d\n", pd_system_status.blur_board_rev);
    LOGD("BOARD0 REV = %d\n", pd_system_status.driver_board0_rev);
    LOGD("BOARD1 REV = %d\n", pd_system_status.driver_board1_rev);
    LOGD("BOARD ID = %d\n", pd_system_status.board_id);
    LOGD("BOARD STATUS = %d\n", pd_system_status.pd_status);

    return 0;
}

JNIEXPORT jstring JNICALL Java_com_pd_get_sys_info(JNIEnv *env, jclass arg) {
    char strTemp[256];

    sprintf(strTemp,
            "Hp22mm Lib REV. = %s\nFW Rev = %d.%d\nBootloader Rev = %d.%d\nFPGA Rev = %d.%d\nBlur board Rev = %d\nDriver Board0 = %d, Board1 = %d\nStatus = %d\nBoard ID = %d",
            VERSION_CODE,
            pd_system_status.fw_rev_major, pd_system_status.fw_rev_minor,
            pd_system_status.boot_rev_major, pd_system_status.boot_rev_minor,
            pd_system_status.fpga_rev_major, pd_system_status.fpga_rev_minor,
            pd_system_status.blur_board_rev,
            pd_system_status.driver_board0_rev, pd_system_status.driver_board1_rev,
            pd_system_status.pd_status,
            pd_system_status.board_id);

    return (*env)->NewStringUTF(env, strTemp);
}

JNIEXPORT jint JNICALL Java_com_ids_set_platform_info(JNIEnv *env, jclass arg) {
    IDSResult_t ids_r;
    PlatformInfo_t platform_info;

    strncpy(platform_info.model, PLATFORM_MODEL, sizeof(platform_info.model));
    platform_info.mfg_year = PLATFORM_YEAR;
    platform_info.mfg_woy = PLATFORM_WOY;
    platform_info.mfg_country = PLATFORM_COUNTRY;
    platform_info.mfg_rev_major = PLATFORM_REV_MAJOR;
    platform_info.mfg_rev_minor = PLATFORM_REV_MINOR;
    platform_info.orientation = PLATFORM_ORIENTATION;      // used by PD only

    ids_r = ids_set_platform_info(IDS_INSTANCE, &platform_info);
    if (ids_check("ids_set_platform_info", ids_r)) return -1;

    return 0;
}

JNIEXPORT jint JNICALL Java_com_pd_set_platform_info(JNIEnv *env, jclass arg) {
    PDResult_t pd_r;
    PlatformInfo_t platform_info;

    strncpy(platform_info.model, PLATFORM_MODEL, sizeof(platform_info.model));
    platform_info.mfg_year = PLATFORM_YEAR;
    platform_info.mfg_woy = PLATFORM_WOY;
    platform_info.mfg_country = PLATFORM_COUNTRY;
    platform_info.mfg_rev_major = PLATFORM_REV_MAJOR;
    platform_info.mfg_rev_minor = PLATFORM_REV_MINOR;
    platform_info.orientation = PLATFORM_ORIENTATION;      // used by PD only

    pd_r = pd_set_platform_info(PD_INSTANCE, &platform_info);
    if (pd_check("pd_set_platform_info", pd_r)) return -1;

    return 0;
}

JNIEXPORT jint JNICALL Java_com_ids_set_date(JNIEnv *env, jclass arg) {
    IDSResult_t ids_r;
    struct tm* tdetail = get_time();

    ids_r = ids_set_date(IDS_INSTANCE, (1900 + tdetail->tm_year), tdetail->tm_mon, tdetail->tm_mday);
    if (ids_check("ids_set_date", ids_r)) return -1;

    return 0;
}

JNIEXPORT jint JNICALL Java_com_pd_set_date(JNIEnv *env, jclass arg) {
    PDResult_t pd_r;
    struct tm* tdetail = get_time();

    pd_r = pd_set_date(PD_INSTANCE, (1900 + tdetail->tm_year), tdetail->tm_mon, tdetail->tm_mday);
    if (pd_check("pd_set_date", pd_r)) return -1;

    return 0;
}

JNIEXPORT jint JNICALL Java_com_ids_set_stall_insert_count(JNIEnv *env, jclass arg) {
    IDSResult_t ids_r;

    ids_r = ids_set_stall_insert_count(IDS_INSTANCE, sIdsIdx, IDS_STALL_INSERT);
    if (ids_check("ids_set_stall_insert_count", ids_r)) return (-1);

    return 0;
}

static SupplyID_t supply_id;
static char SN[30] = "";

char *GetSupplyID() {
    // create SN string (storing in global)
    SN[0] = '\0';
    snprintf(SN, sizeof(SN), "%d_%d_%04d_%02d_%d_%02d_%02d_%02d_%d",
             (int)supply_id.mfg_site,
             (int)supply_id.mfg_line,
             (int)supply_id.mfg_year,
             (int)supply_id.mfg_woy,
             (int)supply_id.mfg_dow,
             (int)supply_id.mfg_hour,
             (int)supply_id.mfg_min,
             (int)supply_id.mfg_sec,
             (int)supply_id.mfg_pos);
    return SN;
}

JNIEXPORT jint JNICALL Java_com_ids_get_supply_status(JNIEnv *env, jclass arg) {
    if (ids_check("ids_get_supply_status", ids_get_supply_status(IDS_INSTANCE, sIdsIdx, &supply_status))) return (-1);
    if (supply_status.state != SUPPLY_SC_VALID) {
        LOGE("Supply state not valid: %d[0x%02x]\n", (int)supply_status.state, supply_status.status_bits);
        return (-1);
    }

    if (supply_status.state == SUPPLY_SC_VALID) {
        if (ids_check("ids_get_supply_info", ids_get_supply_info(IDS_INSTANCE, sIdsIdx, &supply_info))) return (-1);
        if (ids_check("ids_get_supply_id", ids_get_supply_id(IDS_INSTANCE, sIdsIdx, &supply_id))) return (-1);

        LOGD("============= SupplyID =============\n");
        LOGD("supply_id.mfg_site = 0x%02X\n", supply_id.mfg_site);                   /**< Dry cartridge manufacture site. */
        LOGD("supply_id.mfg_line = 0x%02X\n", supply_id.mfg_line);                   /**< Dry cartridge manufacture line. */
        LOGD("supply_id.mfg_year = 0x%04X\n", supply_id.mfg_year);                   /**< Dry cartridge manufacture year (e.g. 2018). */
        LOGD("supply_id.mfg_woy = 0x%02X\n", supply_id.mfg_woy);                     /**< Dry cartridge manufacture week of year (1-52). */
        LOGD("supply_id.mfg_dow = 0x%02X\n", supply_id.mfg_dow);                     /**< Dry cartridge manufacture day of week (0-6 = Sunday-Saturday). */
        LOGD("supply_id.mfg_hour = 0x%02X\n", supply_id.mfg_hour);                   /**< Dry cartridge manufacture hour (0-23). */
        LOGD("supply_id.mfg_min = 0x%02X\n", supply_id.mfg_min);                     /**< Dry cartridge manufacture minute (0-59). */
        LOGD("supply_id.mfg_sec = 0x%02X\n", supply_id.mfg_sec);                     /**< Dry cartridge manufacture second (0-59). */
        LOGD("supply_id.mfg_pos = 0x%02X\n", supply_id.mfg_pos);                     /**< Dry cartridge manufacture position. */

        LOGD("============= SupplyStatus =============\n");
        LOGD("supply_status.state = 0x%02X\n", supply_status.state);                 /**< Non-zero indicates Out of ink. */
        LOGD("supply_status.status_bits = 0x%02X\n", supply_status.status_bits);     /**< Bits set to indicate various conditions */
        LOGD("supply_status.consumed_volume = 0x%04X\n", supply_status.consumed_volume); /**< Consumed ink volume (10ths of ml). */

        LOGD("============= SupplyInfo =============\n");
        LOGD("supply_info.mfg_site = 0x%02X\n", supply_info.mfg_site);               /**< Ink fill site. */
        LOGD("supply_info.mfg_line = 0x%02X\n", supply_info.mfg_line);               /**< Ink fill line. */
        LOGD("supply_info.mfg_year = 0x%04X\n", supply_info.mfg_year);               /**< Ink fill year (e.g. 2018). */
        LOGD("supply_info.mfg_woy = 0x%02X\n", supply_info.mfg_woy);                 /**< Ink fill week of year (1-52). */
        LOGD("supply_info.mfg_dow = 0x%02X\n", supply_info.mfg_dow);                 /**< Ink fill day of week (0-6 = Sunday-Saturday). */
        LOGD("supply_info.mfg_hour = 0x%02X\n", supply_info.mfg_hour);               /**< Ink fill hour (0-23). */
        LOGD("supply_info.mfg_min = 0x%02X\n", supply_info.mfg_min);                 /**< Ink fill minute (0-59). */
        LOGD("supply_info.mfg_sec = 0x%02X\n", supply_info.mfg_sec);                 /**< Ink fill second (0-59). */
        LOGD("supply_info.mfg_pos = 0x%02X\n", supply_info.mfg_pos);                 /**< Ink fill position. */
        LOGD("supply_info.sensor_gain = 0x%04X\n", supply_info.sensor_gain);         /**< PILS sensor gain (mV/PSI * 100). */
        LOGD("supply_info.ink_density = 0x%04X\n", supply_info.ink_density);         /**< Ink density (g/ml * 1000). */
        LOGD("supply_info.usable_vol = 0x%04X\n", supply_info.usable_vol);           /**< Usable ink volume (10ths of ml). */
        LOGD("supply_info.insert_count = 0x%02X\n", supply_info.insert_count);       /**< Insertion count for this cartridge. */
        LOGD("supply_info.ext_oem_id = 0x%02X\n", supply_info.ext_oem_id);           /**< Extended OEM ID. */
        LOGD("supply_info.hp_oem_designate = 0x%02X\n", supply_info.hp_oem_designate);   /**< HP/OEM ink designator. */
        LOGD("supply_info.formulator_id = 0x%02X\n", supply_info.formulator_id);     /**< Ink formulator ID. */
        LOGD("supply_info.ink_vehicle = 0x%02X\n", supply_info.ink_vehicle);         /**< Ink vehicle. */
        LOGD("supply_info.ink_family = 0x%02X\n", supply_info.ink_family);           /**< Ink family. */
        LOGD("supply_info.ink_member = 0x%04X\n", supply_info.ink_member);           /**< Ink family member. */
        LOGD("supply_info.ink_revision = 0x%02X\n", supply_info.ink_revision);       /**< Ink revision. */
    }

    return 0;
}

JNIEXPORT jstring JNICALL Java_com_ids_get_supply_status_info(JNIEnv *env, jclass arg) {
    char strTemp[256];

    sprintf(strTemp,"ID = %s\nState = %d\nOut of Ink = %s\nAltered = %s\nExpired = %s\nFaulty = %s\nConsumed vol = %.1f\nUsable vol = %.1f\nSensor gain = %.1f\nInk density = %.1f",
            GetSupplyID(),
            supply_status.state,
            (supply_status.status_bits & STATUS_OOI ? "True" : "False"),
            (supply_status.status_bits & STATUS_ALTERED ? "True" : "False"),
            (supply_status.status_bits & STATUS_EXPIRED ? "True" : "False"),
            (supply_status.status_bits & STATUS_FAULTY ? "True" : "False"),
            supply_status.consumed_volume/10.0,
            supply_info.usable_vol/10.0,
            supply_info.sensor_gain / 100.0,
            supply_info.ink_density / 1000.0);

    return (*env)->NewStringUTF(env, strTemp);
}

JNIEXPORT jint JNICALL Java_com_pd_get_print_head_status(JNIEnv *env, jclass arg, jint penIndex) {
    PDResult_t pd_r;

    LOGI("pd_get_print_head_status PEN(%d) ....\n", penIndex);

    if (pd_check_ph("pd_get_print_head_status", pd_get_print_head_status(PD_INSTANCE, penIndex, &print_head_status), penIndex)) return (-1);
// H.M.Wang 2024-11-6 取消该判断，这个是为开机时的初始化设计的，正常运行时执行该操作，会得到    print_head_status.print_head_state == PH_STATE_POWERED_ON，所以会报错
//    if (print_head_status.print_head_state != PH_STATE_PRESENT && print_head_status.print_head_state != PH_STATE_POWERED_OFF) {
//        LOGE("Print head state not valid. print_head_state=%d, print_head_error=%d\n", (int)print_head_status.print_head_state, (int)print_head_status.print_head_error);
//        return (-1);
//    }
// End of H.M.Wang 2024-11-6 取消该判断，这个是为开机时的初始化设计的，正常运行时执行该操作，会得到    print_head_status.print_head_state == PH_STATE_POWERED_ON，所以会报错

    RunningState[PEN0_STATE+penIndex] = STATE_VALID;
    if (print_head_status.print_head_state >= PH_STATE_NOT_PRESENT) {
        LOGE("Print head state[%d]\n", print_head_status.print_head_state);
        sprintf(ERR_STRING, "Print head state[%d]\n", print_head_status.print_head_state);
        RunningState[PEN0_STATE+penIndex] = STATE_INVALID;
        return (-1);
    }

    if (print_head_status.print_head_error != PH_NO_ERROR) {
        LOGE("Print head error = %s\n", ph_error_description(print_head_status.print_head_error));
        sprintf(ERR_STRING, "Print head error = %s\n", ph_error_description(print_head_status.print_head_error));
        RunningState[PEN0_STATE+penIndex] = STATE_INVALID;
        return (-1);
    }

    LOGD("============= PrintHeadStatus =============\n");
    LOGD("print_head_state = 0x%02X\n", print_head_status.print_head_state);       /**< Printhead state */
    LOGD("print_head_error = 0x%02X\n", print_head_status.print_head_error);       /**< Printhead error code */
    LOGD("energy_calibrated = 0x%02X\n", print_head_status.energy_calibrated);     /**< Is the Printhead energy calibrated */
    LOGD("temp_calibrated = 0x%02X\n", print_head_status.temp_calibrated);         /**< Is the Printhead temeprature calibrated */
    LOGD("slot_a_purge_completed = 0x%02X\n", print_head_status.slot_a_purge_completed);         /**< Is purge completed for slot a */
    LOGD("slot_b_purge_completed = 0x%02X\n", print_head_status.slot_b_purge_completed);         /**< Is purge completed for slot b */
    LOGD("overdrive_warning = 0x%02X\n", print_head_status.overdrive_warning);     /**< Overdrive warning has occured after the last read of status */
    LOGD("overtemp_warning = 0x%02X\n", print_head_status.overtemp_warning);       /**< Overtemp warning has occured after the last read of status */
    LOGD("supplyexpired_warning = 0x%02X\n", print_head_status.supplyexpired_warning);           /**< Idsexpired warning has occured after the last read of status */

    return 0;
}

JNIEXPORT jstring JNICALL Java_com_pd_get_print_head_status_info(JNIEnv *env, jclass arg) {
    char strTemp[1024];

    sprintf(strTemp,
            "print_head_state = %d\nprint_head_error = %d\nenergy_calibrated = %d\ntemp_calibrated = %d\nslot_a_purge_completed = %d\nslot_b_purge_completed = %d\noverdrive_warning = %d\novertemp_warning = %d\nsupplyexpired_warning = %d\n",
            print_head_status.print_head_state,
            print_head_status.print_head_error,
            print_head_status.energy_calibrated,
            print_head_status.temp_calibrated,
            print_head_status.slot_a_purge_completed,
            print_head_status.slot_b_purge_completed,
            print_head_status.overdrive_warning,
            print_head_status.overtemp_warning,
            print_head_status.supplyexpired_warning);

    return (*env)->NewStringUTF(env, strTemp);
}

JNIEXPORT jint JNICALL Java_com_pd_sc_get_status(JNIEnv *env, jclass arg, jint penIndex) {
    LOGI("Enter %s (head = %d)", __FUNCTION__, penIndex);

    uint8_t pd_sc_result;
    if (pd_check_ph("pd_sc_get_status", pd_sc_get_status(PD_INSTANCE, penIndex, &pd_sc_status, &pd_sc_result), penIndex) || pd_sc_result != 0) {
        LOGE("pd_sc_get_status error\n");
        return (-1);
    }
    if (pd_check_ph("pd_sc_read_oem_field", pd_sc_read_oem_field(PD_INSTANCE, penIndex, PD_SC_OEM_RW_FIELD_1, &(pd_sc_status.purge_complete_mark_oem), &pd_sc_result), penIndex) || pd_sc_result != 0) {
        LOGE("pd_sc_read_oem_field error\n");
        return (-1);
    }

    LOGD("============= PDSmartCardStatus =============\n");
    LOGD("out_of_ink : %s\n", (pd_sc_status.out_of_ink ? "out of ink" : "Not out of ink"));     /**< Out of ink. Used only in the case of single-use printheads. 0 = Not out of ink, 1 = out of ink */
    LOGD("purge_complete_slot_a : %s\n", (pd_sc_status.purge_complete_slot_a ? "Purge completed" : "Purge not completed")); /**< Shipping fluid Purge complete for slot A. 0 = Purge not complete, 1 = Purge completed */
    LOGD("purge_complete_slot_b : %s\n", (pd_sc_status.purge_complete_slot_b ? "Purge completed" : "Purge not completed")); /**< Shipping fluid Purge complete for slot B. 0 = Purge not complete, 1 = Purge completed */
    LOGD("purge_complete_mark_oem : %d\n", pd_sc_status.purge_complete_mark_oem); /**< Purge completion mark written in OEM area. 1:SlotA done, 2:SlotB done, 3:Both done */
    LOGD("altered_ph_detected_slot_a : %s\n", (pd_sc_status.altered_ph_detected_slot_a ? "Altered printhead detected" : "Altered printhead not detected")); /**< Altered Printhead detected on slot A. 0 = Altered printhead not detected, 1 = Altered printhead detected */
    LOGD("altered_ph_detected_slot_b : %s\n", (pd_sc_status.altered_ph_detected_slot_b ? "Altered printhead detected" : "Altered printhead not detected")); /**< Altered Printhead detected on slot A. 0 = Altered printhead not detected, 1 = Altered printhead detected */
    LOGD("altered_supply_detected_slot_a : %s\n", (pd_sc_status.altered_supply_detected_slot_a ? "Altered supply detected" : "Altered supply not detected")); /**< Altered supply detected on slot A. 0 = Altered supply not detected, 1 = Altered supply detected */
    LOGD("altered_supply_detected_slot_b : %s\n", (pd_sc_status.altered_supply_detected_slot_b ? "Altered supply detected" : "Altered supply not detected")); /**< Altered supply detected on slot A. 0 = Altered supply not detected, 1 = Altered supply detected */
    LOGD("faulty_replace_immediately : %s\n", (pd_sc_status.faulty_replace_immediately ? "Faulty printhead replace immediately" : "OK"));     /**< Faulty printhead. 0 = OK, 1 = Faulty printhead replace immediately */
    LOGD("pen_short_detected_slot_a : %s\n", (pd_sc_status.pen_short_detected_slot_a ? "Pen short detected" : "No short")); /**< Pen short short for slot A. 0 = No short, 1 = Pen short detected */
    LOGD("pen_short_detected_slot_b : %s\n", (pd_sc_status.pen_short_detected_slot_b ? "Pen short detected" : "No short")); /**< Pen short short for slot B. 0 = No short, 1 = Pen short detected */
    LOGD("expired_supply_slot_a : %s\n", (pd_sc_status.expired_supply_slot_a ? "Expired supply" : "Not expired supply")); /**< Expired supply for slot A. 0 = Not expired supply, 1 = Expired supply */
    LOGD("expired_supply_slot_b : %s\n", (pd_sc_status.expired_supply_slot_b ? "Expired supply" : "Not expired supply")); /**< Expired supply for slot B. 0 = Not expired supply, 1 = Expired supply */
    LOGD("crtdg_insertion_count = 0x%02X\n", pd_sc_status.crtdg_insertion_count);       /**< Cartridge insertion count */
    LOGD("last_failure_code = 0x%02X\n", pd_sc_status.last_failure_code);       /**< Last failure code */

    return 0;
}

JNIEXPORT jstring JNICALL Java_com_pd_sc_get_status_info(JNIEnv *env, jclass arg) {
    char strTemp[1024];

    sprintf(strTemp,
            "Slot A = %s\nSlot B = %s\nOEM Purge Mark - %d (1:SlotA done; 2:SlotB done; 3:Both done)\nFaulty = %s",
            (pd_sc_status.purge_complete_slot_a ? "Purge Complete" : "Not Purged"),
            (pd_sc_status.purge_complete_slot_b ? "Purge Complete" : "Not Purged"),
            pd_sc_status.purge_complete_mark_oem,
            (pd_sc_status.faulty_replace_immediately ? "True" : "False"));

    return (*env)->NewStringUTF(env, strTemp);
}

JNIEXPORT jint JNICALL Java_com_pd_sc_get_info(JNIEnv *env, jclass arg, jint penIndex) {
    PDResult_t pd_r;

    uint8_t pd_sc_result;
    if (pd_check_ph("pd_sc_get_info", pd_sc_get_info(PD_INSTANCE, penIndex, &pd_sc_info, &pd_sc_result), penIndex) || pd_sc_result != 0) {
        LOGE("pd_sc_get_info error\n");
        return (-1);
    }

    LOGD("============= PDSmartCardInfo =============\n");
    LOGD("max_usable_slot_volume = 0x%04X\n", pd_sc_info.max_usable_slot_volume);           /**< Usable volume per slot in ml */
    LOGD("column_spacing_slota_ldw = 0x%02X\n", pd_sc_info.column_spacing_slota_ldw);       /**< column-to-column spacing in 1/2400th of an inch */
    LOGD("column_spacing_slota_hdw = 0x%02X\n", pd_sc_info.column_spacing_slota_hdw);       /**< column-to-column spacing in 1/2400th of an inch */
    LOGD("column_spacing_slotb_ldw = 0x%02X\n", pd_sc_info.column_spacing_slotb_ldw);       /**< column-to-column spacing in 1/2400th of an inch */
    LOGD("column_spacing_slotb_hdw = 0x%02X\n", pd_sc_info.column_spacing_slotb_hdw);       /**< column-to-column spacing in 1/2400th of an inch */
    LOGD("pen_serial_num1 = 0x%08X\n", pd_sc_info.pen_serial_num1);                         /**< Pen serial number 1 */
    LOGD("pen_serial_num2 = 0x%08X\n", pd_sc_info.pen_serial_num2);                         /**< Pen serial number 2 */

    LOGD("ctrdg_fill_site_id = 0x%02X\n", pd_sc_info.ctrdg_fill_site_id);                   /**< Cartridge Fill manufacturer location identifier. */
    LOGD("ctrdg_fill_line = 0x%02X\n", pd_sc_info.ctrdg_fill_line);                         /**< Cartridge Fill manufacturer line identifier */
    LOGD("ctrdg_fill_year = 0x%04X\n", pd_sc_info.ctrdg_fill_year);                         /**< Cartridge Fill year (e.g. 2018). */
    LOGD("ctrdg_fill_woy = 0x%02X\n", pd_sc_info.ctrdg_fill_woy);                           /**< Cartridge Fill week of the year (1-52). */
    LOGD("ctrdg_fill_dow = 0x%02X\n", pd_sc_info.ctrdg_fill_dow);                           /**< Cartridge Fill day of week (0-6 = Sunday-Saturday). */
    LOGD("ctrdg_fill_hour = 0x%02X\n", pd_sc_info.ctrdg_fill_hour);                         /**< Cartridge Fill hour of day. */
    LOGD("ctrdg_fill_min = 0x%02X\n", pd_sc_info.ctrdg_fill_min);                           /**< Cartridge Fill minute of hour. */
    LOGD("ctrdg_fill_sec = 0x%02X\n", pd_sc_info.ctrdg_fill_sec);                           /**< Cartridge Fill second of minute. */
    LOGD("ctrdg_fill_procpos = 0x%02X\n", pd_sc_info.ctrdg_fill_procpos);                   /**< Cartridge Fill tooling process position. */

    LOGD("ink_formulator_id_slota = 0x%02X\n", pd_sc_info.ink_formulator_id_slota);         /**< Cartridge Fill tooling process position. */
    LOGD("ink_vehicle_slota = 0x%02X\n", pd_sc_info.ink_vehicle_slota);                     /**< ink vehicle type. */
    LOGD("ink_family_slota = 0x%04X\n", pd_sc_info.ink_family_slota);                       /**< Ink formulator ID. */
    LOGD("ink_family_member_slota = 0x%04X\n", pd_sc_info.ink_family_member_slota);         /**< Ink family member. */
    LOGD("ink_revision_slota = 0x%02X\n", pd_sc_info.ink_revision_slota);                   /**< Ink revision number. */

    LOGD("ink_formulator_id_slotb = 0x%02X\n", pd_sc_info.ink_formulator_id_slotb);         /**< Cartridge Fill tooling process position. */
    LOGD("ink_vehicle_slotb = 0x%02X\n", pd_sc_info.ink_vechicle_slotb);                     /**< ink vehicle type. */
    LOGD("ink_family_slotb = 0x%04X\n", pd_sc_info.ink_family_slotb);                       /**< Ink formulator ID. */
    LOGD("ink_family_member_slotb = 0x%04X\n", pd_sc_info.ink_family_member_slotb);         /**< Ink family member. */
    LOGD("ink_revision_slotb = 0x%02X\n", pd_sc_info.ink_revision_slotb);                   /**< Ink revision number. */


    LOGD("ink_drop_wt_slota_hi = 0x%02X\n", pd_sc_info.ink_drop_wt_slota_hi);               /**< Drop weight(ng) of High Drop Weight Nozzles in Slot A */
    LOGD("ink_drop_wt_slota_lo = 0x%02X\n", pd_sc_info.ink_drop_wt_slota_lo);               /**< Drop weight(ng) of Low Drop Weight Nozzles in Slot A */
    LOGD("ink_drop_wt_slotb_hi = 0x%02X\n", pd_sc_info.ink_drop_wt_slotb_hi);               /**< Drop weight(ng) of High Drop Weight Nozzles in Slot B */
    LOGD("ink_drop_wt_slotb_lo = 0x%02X\n", pd_sc_info.ink_drop_wt_slotb_lo);               /**< Drop weight(ng) of Low Drop Weight Nozzles in Slot B */
    LOGD("ink_density_slota = %f\n", pd_sc_info.ink_density_slota);                         /**< Ink density in g/ml */
    LOGD("ink_density_slotb = %f\n", pd_sc_info.ink_density_slotb);                         /**< Ink density in g/ml */
    LOGD("shelf_life_weeks = 0x%02X\n", pd_sc_info.shelf_life_weeks);                       /**< Shelf life of ink in weeks from the time of cartrdige fill */
    LOGD("shelf_life_days = 0x%02X\n", pd_sc_info.shelf_life_days);                         /**< Shelf life of ink in days from the time of cartrdige fill */
    LOGD("installed_life_weeks = 0x%02X\n", pd_sc_info.installed_life_weeks);               /**< Installed life of the cartridge in weeks from the time it was first installed in the printer */
    LOGD("installed_life_days = 0x%02X\n", pd_sc_info.installed_life_days);                 /**< Installed life of the cartridge in days from the time it was first installed in the printer */
    LOGD("vert_print_usable_ink_wt_slota = %f\n", pd_sc_info.vert_print_usable_ink_wt_slota); /**< Usable ink weight in grams when printing vertically (single-use) */
    LOGD("horz_print_usable_ink_wt_slota = %f\n", pd_sc_info.horz_print_usable_ink_wt_slota); /**< Usable ink weight in grams when printing horizonally (single-use) */
    LOGD("vert_print_usable_ink_wt_slotb = %f\n", pd_sc_info.vert_print_usable_ink_wt_slotb); /**< Usable ink weight in grams when printing vertically (single-use) */
    LOGD("horz_print_usable_ink_wt_slotb = %f\n", pd_sc_info.horz_print_usable_ink_wt_slotb); /**< Usable ink weight in grams when printing horizonally (single-use) */

    LOGD("operating_temp = 0x%02X\n", pd_sc_info.operating_temp);                             /**< Operating temperature */
    LOGD("high_temp_warning = 0x%02X\n", pd_sc_info.high_temp_warning);                       /**< Warning temperature */
    LOGD("max_firing_freq = 0x%02X\n", pd_sc_info.max_firing_freq);                           /**< Maximum firing frequency (KHz) */
    LOGD("oe_override_percent_slota = 0x%02X\n", pd_sc_info.oe_override_percent_slota);       /**< Over energy percent */
    LOGD("oe_override_percent_slotb = 0x%02X\n", pd_sc_info.oe_override_percent_slotb);       /**< Over energy percent */
    LOGD("volt_offset_override_percent = 0x%02X\n", pd_sc_info.volt_offset_override_percent); /**< Voltage offset override percent */
    LOGD("operating_temp_ovrride = 0x%02X\n", pd_sc_info.operating_temp_ovrride);             /**< Operating Temperature override */
    LOGD("shf_present_slota = 0x%02X\n", pd_sc_info.shf_present_slota);                       /**< "1" if this printhead was shipped with shipping fluid */
    LOGD("shf_present_slotb = 0x%02X\n", pd_sc_info.shf_present_slotb);                       /**< "1" if this printhead was shipped with shipping fluid */

    LOGD("first_platform_mfg_year = 0x%04X\n", pd_sc_info.first_platform_mfg_year);           /**< Printer platform manufacture year (e.g. 2018) in which this cartrdige was first installed. */
    LOGD("first_platform_mfg_woy = 0x%02X\n", pd_sc_info.first_platform_mfg_woy);             /**< Printer platform manufacture week of year (1-52) in which this cartrdige was first installed. */
    LOGD("first_platform_mfg_country = 0x%02X\n", pd_sc_info.first_platform_mfg_country);     /**< Printer platform country of manufacture in which this cartrdige was first installed. */
    LOGD("first_platform_fw_rev_major = 0x%02X\n", pd_sc_info.first_platform_fw_rev_major);   /**< Printer platform FW major revision into which this cartrdige was first installed */
    LOGD("first_platform_fw_rev_minor = 0x%02X\n", pd_sc_info.first_platform_fw_rev_minor);   /**< Printer platform FW major revision into which this cartrdige was first installed */

    /* Check the data type */
    LOGD("first_install_ctrdg_count = 0x%02X\n", pd_sc_info.first_install_ctrdg_count);       /**< number of unique cartridges used on the print platform */
    LOGD("ctrdg_first_install_year = 0x%04X\n", pd_sc_info.ctrdg_first_install_year);         /**< Year (e.g.2018) in which this cartridge was first installed in a printer platform */
    LOGD("ctrdg_first_install_woy = 0x%02X\n", pd_sc_info.ctrdg_first_install_woy);           /**< Week of year (1-52) in which this cartridge was first installed in a printer platform */
    LOGD("ctrdg_first_install_dow = 0x%02X\n", pd_sc_info.ctrdg_first_install_dow);           /**< Day of week (0-6 = Sunday-Saturday) this cartridge was first installed in a printer platform */
    LOGD("ilg_resolution = 0x%02X\n", pd_sc_info.ilg_resolution);                             /**< Ink Level Gauge Resolution */

    /* Check data type */
    LOGD("ph_orientation = 0x%02X\n", pd_sc_info.ph_orientation);       /**< PH Orientation. 0=Vertical, 1=Horizontal */
    LOGD("post_purged_ink_family_member_slota = 0x%04X\n", pd_sc_info.post_purged_ink_family_member_slota); /**< Post purged ink family member on slot a */
    LOGD("post_purged_ink_family_member_slotb = 0x%04X\n", pd_sc_info.post_purged_ink_family_member_slotb); /**< Post purged ink family member on slot b */
    LOGD("ext_oem_id = 0x%02X\n", pd_sc_info.ext_oem_id);       /**< Extended OEM ID */
    LOGD("single_use = 0x%02X\n", pd_sc_info.single_use);       /**< 0=Single use, 1=Bulk use */
    LOGD("hp_or_oem_ink_designator_slota = 0x%02X\n", pd_sc_info.hp_or_oem_ink_designator_slota);   /**< 0=OEM Ink, 1=HP Ink */
    LOGD("hp_or_oem_ink_designator_slotb = 0x%02X\n", pd_sc_info.hp_or_oem_ink_designator_slotb);   /**< 0=OEM Ink, 1=HP Ink */
    LOGD("regionalization_id = 0x%02X\n", pd_sc_info.regionalization_id);                     /**< Region where this cartridge will be accepted by the printer. */

    LOGD("drop_weight_cfg_slota = 0x%02X\n", pd_sc_info.drop_weight_cfg_slota);               /**< 0=Single drop weight, 1=Dual drop weight */
    LOGD("nozzle_density_cfg_slota = 0x%02X\n", pd_sc_info.nozzle_density_cfg_slota);         /**< Dots per inch. 0=2400, 1=1200, 2=600, 3=300 */
    LOGD("drop_weight_cfg_slotb = 0x%02X\n", pd_sc_info.drop_weight_cfg_slotb);               /**< 0=Single drop weight, 1=Dual drop weight */
    LOGD("nozzle_density_cfg_slotb = 0x%02X\n", pd_sc_info.nozzle_density_cfg_slotb);         /**< Dots per inch. 0=2400, 1=1200, 2=600, 3=300 */

    LOGD("platform_id = [%s]\n", pd_sc_info.platform_id);                                     /**< 12-character Platform ID string */
    LOGD("trademark_string = [%s]\n", pd_sc_info.trademark_string);                           /**< 5-character trademark string */
    LOGD("ctrdg_reorder_part_num = [%s]\n", pd_sc_info.ctrdg_reorder_part_num);               /**< 12-character reorder part number string */

    return 0;
}

JNIEXPORT jstring JNICALL Java_com_pd_sc_get_info_info(JNIEnv *env, jclass arg) {
    char strTemp[1024];

    sprintf(strTemp,
            "ID = %d_%d_%d_%d_%d_%d_%d_%d_%d",
            pd_sc_info.ctrdg_fill_site_id,
            pd_sc_info.ctrdg_fill_line,
            pd_sc_info.ctrdg_fill_year,
            pd_sc_info.ctrdg_fill_woy,
            pd_sc_info.ctrdg_fill_dow,
            pd_sc_info.ctrdg_fill_hour,
            pd_sc_info.ctrdg_fill_min,
            pd_sc_info.ctrdg_fill_sec,
            pd_sc_info.ctrdg_fill_procpos);

    return (*env)->NewStringUTF(env, strTemp);
}

JNIEXPORT jint JNICALL Java_com_DeletePairing(JNIEnv *env, jclass arg) {
    if (DeletePairing()) {
        LOGE("DeletePairing failed!\n");
        return (-1);
    }
    return 0;
}

JNIEXPORT jint JNICALL Java_com_DoPairing(JNIEnv *env, jclass arg) {
    if (DoPairing(sIdsIdx, PenArg)) {
        LOGE("DoPairing failed!\n");
        return (-1);
    };
    return 0;
}

JNIEXPORT jint JNICALL Java_com_DoOverrides(JNIEnv *env, jclass arg) {
    if (DoOverrides(sIdsIdx, PenArg)) {
        LOGE("DoOverrides failed!\n");
        return (-1);
    }
    return 0;
}

JNIEXPORT jint JNICALL Java_com_Pressurize(JNIEnv *env, jclass arg, jboolean async) {
    return CmdPressurize(async);
}

JNIEXPORT jstring JNICALL Java_com_getPressurizedValue(JNIEnv *env, jclass arg) {
    char strTemp[256];

    sprintf(strTemp,"Press Value = %f", ADCGetPressurePSI(sIdsIdx));

    return (*env)->NewStringUTF(env, strTemp);
}

JNIEXPORT jint JNICALL Java_com_Depressurize(JNIEnv *env, jclass arg) {
    CmdDepressurize();
    return 0;
}

JNIEXPORT jint JNICALL Java_com_UpdatePDFW(JNIEnv *env, jclass arg) {
    PDResult_t pd_r;

    Air_Pump_State_t a = Air_Pump_State;
    PD_Power_State_t p = PD_Power_State;

    Air_Pump_State = AIR_STATE_PUMP_OFF;
    PD_Power_State = PD_POWER_STATE_OFF;

    sleep(1);

    pd_r = pd_micro_fw_reflash(PD_INSTANCE, "/mnt/usbhost1/PD.s19", true);
    if (pd_check("pd_micro_fw_reflash_no_reset", pd_r)) {
        pd_r = pd_micro_fw_reflash(PD_INSTANCE, "/mnt/usbhost1/PD.s19", true);
        if (pd_check("pd_micro_fw_reflash_no_reset", pd_r)) {
            Air_Pump_State = a;
            PD_Power_State = p;
            return (-1);
        }
    }

    Air_Pump_State = a;
    PD_Power_State = p;
    return 0;
}

JNIEXPORT jint JNICALL Java_com_UpdateFPGAFlash(JNIEnv *env, jclass arg) {
    PDResult_t pd_r;

    Air_Pump_State_t a = Air_Pump_State;
    PD_Power_State_t p = PD_Power_State;

    Air_Pump_State = AIR_STATE_PUMP_OFF;
    PD_Power_State = PD_POWER_STATE_OFF;

    sleep(1);

    pd_r = pd_lib_init();
    if (pd_check("pd_lib_init", pd_r)) return -1;
    pd_r = pd_init(PD_INSTANCE);
    if (pd_check("pd_init", pd_r)) return -1;

    pd_r = pd_fpga_fw_reflash(PD_INSTANCE, "/mnt/usbhost1/FPGA.s19", true);
    if (pd_check("pd_fpga_fw_reflash", pd_r)) {
        pd_r = pd_fpga_fw_reflash(PD_INSTANCE, "/mnt/usbhost1/FPGA.s19", true);
        if (pd_check("pd_fpga_fw_reflash", pd_r)) {
            Air_Pump_State = a;
            PD_Power_State = p;
            return (-1);
        }
    }

    Air_Pump_State = a;
    PD_Power_State = p;
    return 0;
}

JNIEXPORT jint JNICALL Java_com_UpdateIDSFW(JNIEnv *env, jclass arg) {
    IDSResult_t ids_r;

    Air_Pump_State_t a = Air_Pump_State;
    PD_Power_State_t p = PD_Power_State;

    Air_Pump_State = AIR_STATE_PUMP_OFF;
    PD_Power_State = PD_POWER_STATE_OFF;

    sleep(1);

    ids_r = ids_micro_fw_reflash(IDS_INSTANCE, "/mnt/usbhost1/IDS.s19", true);
    if (ids_check("ids_micro_fw_reflash", ids_r)) {
        ids_r = ids_micro_fw_reflash(IDS_INSTANCE, "/mnt/usbhost1/IDS.s19", true);
        if (ids_check("ids_micro_fw_reflash", ids_r)) {
            Air_Pump_State = a;
            PD_Power_State = p;
            return (-1);
        }
    }

    Air_Pump_State = a;
    PD_Power_State = p;
    return 0;
}

int _CmdPressurize(float set_pressure) {
    float pressure;

    if (set_pressure > 0.0)
        pressure = set_pressure;
    else
        pressure = SUPPLY_PRESSURE;

    IDS_MonitorPressure(sIdsIdx);
    IDS_DAC_SetSetpointPSI(sIdsIdx, pressure);        // set pressure target
    IDS_GPIO_ClearBits(sIdsIdx, COMBO_INK_BOTH);      // ink Off
    IDS_GPIO_SetBits(sIdsIdx, COMBO_AIR_PUMP_ALL);    // pump enabled; air valve On/Hold
    IDS_LED_On(sIdsIdx, LED_Y);
    LOGD("Pressurizing Supply %d to %.1f psi...\n", sIdsIdx, pressure);

    sleep(1);       // delay

    IDS_GPIO_ClearBits(sIdsIdx, GPIO_O_AIR_VALVE_ON); // turn Off air valve (leave Hold)

    LOGD("Waiting on pumps...\n");
    int limit_sec = PRESSURIZE_SEC;
    bool pressurized;
    do {
        sleep(1);   // delay 1 sec

        pressurized = true;
        if (IDS_GPIO_ReadBit(sIdsIdx, GPIO_I_AIR_PRESS_LOW)) pressurized = false;

        // check for timeout
        if (--limit_sec <= 0) {
            LOGE("ERROR: Supply not pressurized in %d seconds\n", PRESSURIZE_SEC);
            CmdDepressurize();
            RunningState[IDS_STATE] = STATE_INVALID;
            return -1;
        }
    } while (!pressurized);

    LOGD("Opening ink valves...\n");

    IDS_GPIO_SetBits(sIdsIdx, COMBO_INK_BOTH);        // ink valve On/Hold

    sleep(1);

    IDS_GPIO_ClearBits(sIdsIdx, GPIO_O_INK_VALVE_ON); // turn Off ink valve (leave Hold)
//    IDS_MonitorPILS(sIdsIdx);

    RunningState[IDS_STATE] = STATE_VALID;
    return 0;
}

int CmdPressurize(jboolean async) {
    if (async) {
        if(Air_Pump_State == AIR_STATE_PUMP_OFF) Air_Pump_State = AIR_STATE_LAUNCH_PUMP;
        return 0;
    } else {
        return _CmdPressurize(-1);
    }
}

void CmdDepressurize() {
    LOGD("Depressurizing...\n");
    Air_Pump_State = AIR_STATE_PUMP_OFF;

    IDS_GPIO_ClearBits(sIdsIdx, COMBO_INK_AIR_PUMP_ALL);     // pump disabled; ink/air valves closed
    IDS_MonitorOff(sIdsIdx);
    IDS_LED_Off(sIdsIdx, LED_Y);
}

JNIEXPORT jint JNICALL Java_com_setLogOutput(JNIEnv *env, jclass arg, jint output) {
    gOutputLog = output;
    return gOutputLog;
}

/**
 * HP22MM操作jni接口
 */
static JNINativeMethod gMethods[] = {
        {"init_ids",				        "(I)I",	                    (void *)Java_com_hp22mm_init_ids},
        {"ids_get_sys_info",	            "()Ljava/lang/String;",	    (void *)Java_com_ids_get_sys_info},
        {"init_pd",				            "(I)I",	                    (void *)Java_com_hp22mm_init_pd},
        {"pd_get_sys_info",	                "()Ljava/lang/String;",	    (void *)Java_com_pd_get_sys_info},
        {"ids_set_platform_info",           "()I",	                    (void *)Java_com_ids_set_platform_info},
        {"pd_set_platform_info",	        "()I",	                    (void *)Java_com_pd_set_platform_info},
        {"ids_set_date",                    "()I",	                    (void *)Java_com_ids_set_date},
        {"pd_set_date",	                    "()I",	                    (void *)Java_com_pd_set_date},
        {"ids_set_stall_insert_count",	    "()I",	                    (void *)Java_com_ids_set_stall_insert_count},
        {"ids_get_supply_status",		    "()I",	                    (void *)Java_com_ids_get_supply_status},
        {"ids_get_supply_status_info",	    "()Ljava/lang/String;",	    (void *)Java_com_ids_get_supply_status_info},
        {"pd_get_print_head_status",		"(I)I",	                    (void *)Java_com_pd_get_print_head_status},
        {"pd_get_print_head_status_info",   "()Ljava/lang/String;",     (void *)Java_com_pd_get_print_head_status_info},
        {"pd_sc_get_status",		"(I)I",	                    (void *)Java_com_pd_sc_get_status},
        {"pd_sc_get_status_info",   "()Ljava/lang/String;",     (void *)Java_com_pd_sc_get_status_info},
        {"pd_sc_get_info",		"(I)I",	                    (void *)Java_com_pd_sc_get_info},
        {"pd_sc_get_info_info",   "()Ljava/lang/String;",     (void *)Java_com_pd_sc_get_info_info},
        {"DeletePairing",		            "()I",	                    (void *)Java_com_DeletePairing},
        {"DoPairing",		                "()I",	                    (void *)Java_com_DoPairing},
        {"DoOverrides",		                "()I",	                    (void *)Java_com_DoOverrides},
        {"Pressurize", "(Z)I",	                    (void *)Java_com_Pressurize},
        {"EnableWarming",		                "(I)I",	                    (void *)Java_com_EnableWarming},
        {"StartMonitor",		                "()I",	                    (void *)Java_com_StartMonitor},
        {"getPressurizedValue",	            "()Ljava/lang/String;",     (void *)Java_com_getPressurizedValue},
        {"Depressurize",		            "()I",	                    (void *)Java_com_Depressurize},
        {"pdPowerOn",	    "(II)I",	    (void *)Java_com_PDPowerOn},
        {"pdPowerOff",		                "(I)I",	                    (void *)Java_com_PDPowerOff},
// H.M.Wang 2024-11-13 追加22mm打印头purge功能
        {"pdPurge",		                "(I)I",	                    (void *)Java_com_purge},
// End of H.M.Wang 2024-11-13 追加22mm打印头purge功能
        {"getErrString",		            "()Ljava/lang/String;",	                    (void *)Java_com_GetErrorString},
        {"getRunningState",				            "(I)I",	                    (void *)Java_com_GetRunningState},
        {"getConsumedVol",		                "()I",	                    (void *)Java_com_GetConsumedVol},
        {"getUsableVol",		                "()I",	                    (void *)Java_com_GetUsableVol},
// H.M.Wang 2024-12-10 22mm本来应该使用内部的统计系统统计墨水的消耗情况，但是暂时看似乎没有动作，因此启用独自的统计系统，计数值保存在OEM_RW区域
        {"getLocalInk",		        "(I)I",						(void *)Java_com_getLocalInk},
        {"downLocal",		        "(II)I",						(void *)Java_com_downLocal},
// End of H.M.Wang 2024-12-10 22mm本来应该使用内部的统计系统统计墨水的消耗情况，但是暂时看似乎没有动作，因此启用独自的统计系统，计数值保存在OEM_RW区域
        {"UpdatePDFW",		                "()I",	                    (void *)Java_com_UpdatePDFW},
        {"UpdateFPGAFlash",		            "()I",	                    (void *)Java_com_UpdateFPGAFlash},
        {"UpdateIDSFW",		                "()I",	                    (void *)Java_com_UpdateIDSFW},
        {"enableLog",	    	    "(I)I",						(void *)Java_com_setLogOutput},

/*
// H.M.Wang 2023-7-27 将startPrint函数的返回值修改为String型，返回错误的具体内容
        {"startPrint",		            "()Ljava/lang/String;",	                    (void *)Java_com_StartPrint},
// End of H.M.Wang 2023-7-27 将startPrint函数的返回值修改为String型，返回错误的具体内容
        {"dumpRegisters",	    "()Ljava/lang/String;",	    (void *)Java_com_DumpRegisters},
        {"spiTest",	    "()Ljava/lang/String;",	    (void *)Java_com_SpiTest},
        {"mcu2fifo",		            "()I",	                    (void *)Java_com_MCU2FIFO},
        {"fifo2ddr",		            "()I",	                    (void *)Java_com_FIFO2DDR},
        {"ddr2fifo",		            "()I",	                    (void *)Java_com_DDR2FIFO},
        {"fifo2mcu",		            "()I",	                    (void *)Java_com_FIFO2MCU},*/
};

/**
 * 注册HP22MM操作的JNI方法
 */
int register_hp22mm(JNIEnv* env) {
//    if (pthread_mutex_init(&mutex, NULL) != 0){
//        return JNI_FALSE;
//    }

    const char* kClassPathName = "com/industry/printer/hardware/Hp22mm";
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

    LOGI("hp22mm.so %s Loaded.", VERSION_CODE);

    if ((*vm)->GetEnv(vm, (void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        //__android_log_print(ANDROID_LOG_INFO, JNI_TAG,"ERROR: GetEnv failed\n");
        goto fail;
    }

    if (register_hp22mm(env) < 0) {
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
