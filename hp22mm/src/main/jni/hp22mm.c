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

#define VERSION_CODE                            "1.0.081"
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
int sPenIdx = 0;

static bool IsPressurized = false;
void CmdDepressurize();
int CmdPressurize();

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

#if 0
#define SPI_DEV_NAME "/dev/spidev0.0"
#define SPI_BITS 8
#define SPI_SPEED 8000000
int spidev = -1;

#define IMAGE_FILE "/mnt/sdcard/image.bin"
uint image_cols = 0;
#define IMAGE_ROWS 1056
#define IMAGE_ADDR 0x0000

#define CLOCK_HZ 90000000
// Encoder frequency (Hz)
#define ENCODER_FREQ_HZ 20000
// TOF period (decimal seconds)
#define TOF_PERIOD_SEC 1.0
// Image TOF - distance in encoder tics
#define IMAGE_TOF 1000

#define PRINT_COMPLETE_CHECK_USEC 50000

int PDGInit() {
    if(spidev >= 0 ) return 0;

    spidev = open(SPI_DEV_NAME, O_RDWR);
    if (spidev < 0) {
        LOGE("ERROR: cannot open %s (%d)\n", SPI_DEV_NAME, errno);
        return -1;
    }

    unsigned char mode = SPI_MODE_0;
    if (ioctl(spidev, SPI_IOC_WR_MODE, &mode) < 0 ||
        ioctl(spidev, SPI_IOC_RD_MODE, &mode) < 0) {
        LOGE("ERROR: ioctl WR/RD mode failed for %s (%d)\n", SPI_DEV_NAME, errno);
        return -1;
    }
    unsigned int speed = SPI_SPEED;
    if (ioctl(spidev, SPI_IOC_WR_MAX_SPEED_HZ, &speed) < 0 ||
        ioctl(spidev, SPI_IOC_RD_MAX_SPEED_HZ, &speed) < 0) {
        LOGE("ERROR: ioctl WR/RD max speed failed for %s (%d)\n", SPI_DEV_NAME, errno);
        return -1;
    }
    unsigned char bits = SPI_BITS;
    if (ioctl(spidev, SPI_IOC_WR_BITS_PER_WORD, &bits) < 0 ||
        ioctl(spidev, SPI_IOC_RD_BITS_PER_WORD, &bits) < 0) {
        LOGE("ERROR: ioctl WR/RD bits per word failed for %s (%d)\n", SPI_DEV_NAME, errno);
        return -1;
    }

    LOGI("PDGInit succeeded\n");
    return 0;
}

#define PDG_READ_REG     3
#define PDG_WRITE_REG    4
#define PDG_WRITE_FIFO   5
#define PDG_READ_FIFO    6
#define PDG_MEM_TRANS    7
#define FIFO_2_DDR       0
#define DDR_2_FIFO       1

int SPIMessage(unsigned char *message, int length) {
    struct spi_ioc_transfer transfer;

//    LOGD("Sent: [0x%02X, 0x%02X, 0x%02X, 0x%02X, 0x%02X, 0x%02X, 0x%02X]", *(message+0), *(message+1), *(message+2), *(message+3), *(message+4), *(message+5), *(message+6));
    memset(&(transfer), 0, sizeof(transfer));
    transfer.tx_buf  = (unsigned long)(message);
    transfer.rx_buf  = (unsigned long)(message);
    transfer.len = length;

    // execute message
    if (ioctl(spidev, SPI_IOC_MESSAGE(1), &transfer) < 0) {
        LOGE("ERROR: ioctl message failed for %s (%s)\n", SPI_DEV_NAME, strerror(errno));
        return -1;
    }
//    LOGD("Recv: [0x%02X, 0x%02X, 0x%02X, 0x%02X, 0x%02X, 0x%02X, 0x%02X]", *(message+0), *(message+1), *(message+2), *(message+3), *(message+4), *(message+5), *(message+6));
    return 0;
}

#define REG_MSG_LEN 7

int PDGRead(unsigned char reg, uint32_t *four_bytes) {
    unsigned char bytes[REG_MSG_LEN];

    // Read sequence
    bytes[0] = PDG_READ_REG;   // Read
    bytes[1] = reg;
    for (int i=2; i<REG_MSG_LEN; i++) bytes[i] = 0;
    // execute read
    if (SPIMessage(bytes, REG_MSG_LEN) < 0) return -1;
    // unpack bytes
    *four_bytes = (bytes[2] << 24) + (bytes[3] << 16) + (bytes[4] << 8) + bytes[5];

    return 0;
}

int PDGWrite(unsigned char reg, uint32_t four_bytes) {
    unsigned char bytes[REG_MSG_LEN];
    uint32_t read_bytes;

    // Write sequence (data is MSB first)
    bytes[0] = PDG_WRITE_REG;   // Write
    bytes[1] = reg;
    bytes[2] = (four_bytes >> 24) & 0xff;
    bytes[3] = (four_bytes >> 16) & 0xff;
    bytes[4] = (four_bytes >> 8) & 0xff;
    bytes[5] = (four_bytes) & 0xff;
    bytes[6] = 0;
    // execute write
    if (SPIMessage(bytes, REG_MSG_LEN) < 0) return -1;

    // read and compare after write
    // NOTE: reg 28 cannot be read
    return 0;
}

void PDGWaitWBuffer(uint32_t wait_for) {
    int count = 0;
    uint32_t ui;

    while (true) {
        if (PDGRead(2, &ui) < 0 ||
            (ui == wait_for))
            break;
        LOGI("Waiting R2 for %u (%d times), ret = %u\n", wait_for, count++, ui);
        if(count == 5) break;

        usleep(500000);     // 0.5 sec
    }
}

void PDGWaitRBuffer(uint32_t wait_for) {
    int count = 0;
    uint32_t ui;

    while (true) {
        if (PDGRead(3, &ui) < 0 ||
            (ui == wait_for))
            break;
        LOGI("Waiting R3 for %u (%d times), ret = %u\n", wait_for, count++, ui);
        if(count == 5) break;

        usleep(500000);     // 0.5 sec
    }
}

int PDGWriteImage() {
    // get image file size
    struct stat st;
    if (stat(IMAGE_FILE, &st) != 0) {
        LOGE("ERROR: cannot process %s\n", IMAGE_FILE);
        return -1;
    }
    image_cols = (st.st_size * 8 / IMAGE_ROWS);

    // open binary image
    FILE *file = fopen(IMAGE_FILE, "rb");
    if (file == NULL) return -1;

    // SLOT IMAGES - rows and increment are fixed
    // write image DDR using data Mask; Cols determines size
    unsigned int addr = IMAGE_ADDR;
    int write_bytes = IMAGE_ROWS / 8;       // column write size in bytes
    int write_words = write_bytes / 4;
    int buffer_size = write_bytes + 3;
    unsigned char buffer[buffer_size];
    unsigned char buffer2[8];

    for (int col=0; col < image_cols; col++) {
        // read from image file and write to PDG
        buffer[0] = PDG_WRITE_FIFO;
        buffer[1] = write_words;
        if (fread((buffer+2), 1, write_bytes, file) != write_bytes) {
            LOGE("ERROR: reading %s\n", IMAGE_FILE);
            return -1;
        }
        buffer[write_bytes+2] = 0;
        if (SPIMessage(buffer, buffer_size) < 0) return -1;
        PDGWaitWBuffer(write_words);

        // Generic command (write)
        buffer2[0] = PDG_MEM_TRANS;
        buffer2[1] = (write_words - 1);
        buffer2[2] = FIFO_2_DDR;
        buffer2[3] = (addr >> 24) & 0xff;
        buffer2[4] = (addr >> 16) & 0xff;
        buffer2[5] = (addr >> 8) & 0xff;
        buffer2[6] = addr & 0xff;
        buffer2[7] = 0;
        if (SPIMessage(buffer2, 8) < 0) return -1;
        PDGWaitWBuffer(0);

        addr += write_bytes;
    }

    LOGI("%d total image bytes (%d rows x %d cols) written to DDR\n", addr, IMAGE_ROWS, image_cols);
    return 0;
}

int PDGPrintSetup() {
    // SLOT IMAGES
    // row size and increment are fixed
    if (PDGWrite(4, IMAGE_ROWS/32) < 0 || // R04 = words/col
        PDGWrite(5, IMAGE_ROWS/8) < 0 ||   // R5 = byte memory advance (1 column)
        PDGWrite(6, image_cols) < 0)       // R6 = columns (all cols using same image)
        return -1;
    // Image address for each slot - all slots using same image
//    int reg = 7 + (PEN_IDX * 4);
    for (int i=0; i<2; i++) {
        int reg = 7 + (i * 4);  // 7 + (PEN_IDX * 4)
        for (int col=0; col<4; col++) {
            if (PDGWrite(reg+col, 0x0000) < 0) return -1; // image address is 0
        }
    }

    // calculate encoder and TOF values
//    int encoder = (int)(CLOCK_HZ / ENCODER_FREQ_HZ);
//    int tof_freq = (int)(TOF_PERIOD_SEC * CLOCK_HZ);
    int encoder = 180000;
    int tof_freq = 180000000;

    // use all 4 columns of selected pen
    int col_mask = 0xf;
    if (sPenIdx == 1) col_mask <<= 4;

    if (PDGWrite(15, encoder) < 0 ||    // R15 internal encoder period (divider of clock freq)
        PDGWrite(16, tof_freq) < 0 ||   // R16 internal TOF frequency (Hz)
        PDGWrite(17, 0) < 0 ||          // R17 0 = internal encoder
        PDGWrite(18, 1) < 0 ||          // R18 external encoder divider (2=600 DPI)
        PDGWrite(19, 0) < 0 ||          // R19 0 = internal TOF
        PDGWrite(20, 0/*IMAGE_TOF*/) < 0 ||  // R20 pen 0 encoder counts from TOF to start print
        PDGWrite(21, 0/*IMAGE_TOF*/) < 0 ||  // R21 pen 1 encoder counts from TOF to start print
        PDGWrite(22, 0) < 0 ||          // R22 0 - print direction forward
        PDGWrite(23, 4) < 0 ||          // R23 column-to-column spacing (rows)
        PDGWrite(24, 52) < 0 ||         // R24 slot-to-slot spacing (rows)
        PDGWrite(25, 0) < 0 ||          // R25 0 - print disabled
        PDGWrite(28, 0) < 0 ||          // R28 0 - not reset
        PDGWrite(29, col_mask) < 0)     // R29 column enable bits
        return -1;

    return 0;
}

pthread_t PrintThread = (pthread_t)NULL;
static bool CancelPrint = false;

bool IsPrinting() {
    return (PrintThread != (pthread_t)NULL);
}

void *_print_thread(void *arg) {
    uint32_t ui;

    while (true) {
        // check for print done (or cancel/error)
        if (PDGRead(25, &ui) < 0 ||     // (R25 print enable)
            ui == 0 ||                  // print NOT enabled
            CancelPrint)                // cancelled by user
            break;

        // delay before checking again
        usleep(PRINT_COMPLETE_CHECK_USEC);
    }
    LOGI("<<< Printing Complete >>>\n");
    if (PDGWrite(25, 0) < 0) {          // R25 0 - disable print
        LOGE("ERROR: cannot disable print\n");
    }
    pd_check_ph("pd_power_off", pd_power_off(PD_INSTANCE, sPenIdx), sPenIdx);
    PrintThread = (pthread_t)NULL;     // (done printing)
    return (void*)NULL;
}

// H.M.Wang 2023-7-27 将startPrint函数的返回值修改为String型，返回错误的具体内容
char *PDGTriggerPrint(int external, int count) {
    if (IsPrinting()) {
        LOGE("ERROR: already printing\n");
        return "ERROR: already printing\n";
//        return -1;
    }

    if (pd_check_ph("pd_power_on", pd_power_on(PD_INSTANCE, sPenIdx), sPenIdx)) {
        LOGE("%s\n", ERR_STRING);
        return ERR_STRING;
//        return -1;
    }
// End of H.M.Wang 2023-7-27 将startPrint函数的返回值修改为String型，返回错误的具体内容

    CancelPrint = false;
    if (PDGWrite(17, external) < 0 ||    // R17 0=internal 1=external encoder
        PDGWrite(19, external) < 0 ||    // R19 0=internal 1=external TOF
        PDGWrite(26, 0) < 0 ||           // R26 init print count to 0
        PDGWrite(27, count) < 0 ||       // R27 set print count limit
        PDGWrite(25, 1) < 0) {            // R25 1 - enable print
        LOGE("ERROR: triggering print\n");
        return "ERROR: triggering print\n";
//        return -1;
    }

    // start the print thread
    printf("Triggering print from ");
    printf(external ? "External sensors...\n" : "Internal sensors...\n");
    if (pthread_create(&PrintThread, NULL, _print_thread, NULL)) {
        PrintThread = (pthread_t)NULL;
        LOGE("ERROR: pthread_create() of PrintThread failed\n");
        return "ERROR: pthread_create() of PrintThread failed\n";
//        return -1;
    }

    return "";
//    return 0;
}

void PDGCancelPrint() {
    CancelPrint = true;
}

// H.M.Wang 2023-7-27 将startPrint函数的返回值修改为String型，返回错误的具体内容
JNIEXPORT jstring JNICALL Java_com_StartPrint(JNIEnv *env, jclass arg) {
    if (!IsPressurized) {
        if (CmdPressurize() != 0) {
            LOGE("ERROR: Java_com_StartPrint() of PrintThread failed. Not Pressurized\n");
            return (*env)->NewStringUTF(env, "ERROR: Java_com_StartPrint() of PrintThread failed. Not Pressurized");
//            return -1;
        }
    }

    if (PDGInit()) {
        LOGE("PDGInit failed\n");
        return (*env)->NewStringUTF(env, "PDGInit failed");
//        return -1;
    }

    if(PDGWriteImage() != 0) {
        LOGE("ERROR: PDGWriteImage failed\n");
        return (*env)->NewStringUTF(env, "ERROR: PDGWriteImage failed");
//        return -1;
    };

    if(PDGPrintSetup() != 0) {
        LOGE("ERROR: PDGPrintSetup failed\n");
        return (*env)->NewStringUTF(env, "ERROR: PDGPrintSetup failed");
//        return -1;
    };

    return (*env)->NewStringUTF(env, PDGTriggerPrint(0, 10));
//    return PDGTriggerPrint(0, 100);
}
// End of H.M.Wang 2023-7-27 将startPrint函数的返回值修改为String型，返回错误的具体内容

char *reg_name[] = {
        "",
        "",
        "2-Write FIFO(RO)    ",
        "3-Read FIFO(RO)     ",
        "4-Words/Col         ",
        "5-Advance Bytes     ",
        "6-Image Columns     ",
        "7-Start P0 S0 Odd   ",
        "8-Start P0 S0 Even  ",
        "9-Start P0 S1 Odd   ",
        "10-Start P0 S1 Even ",
        "11-Start P1 S0 Odd  ",
        "12-Start P1 S0 Even ",
        "13-Start P1 S1 Odd  ",
        "14-Start P1 S1 Even ",
        "15-Encoder Freq     ",
        "16-TOF Freq         ",
        "17-External Encoder ",
        "18-Encoder Divider  ",
        "19-External TOF     ",
        "20-P0 TOF Offset    ",
        "21-P1 TOF Offset    ",
        "22-Print Direction  ",
        "23-Column Spacing   ",
        "24-Slot Spacing     ",
        "25-Enable Print     ",
        "26-Start Print Count",
        "27-End Print Count  ",
        "28-Reset(WR)        ",
        "29-Column Enable    ",
        "30-Flash Enable     ",
        "31-PDG Revision(RO) ",
        "32-Clock Freq(RO)   ",
        "33-Ready            ",
//        "   Action Bits P0 S0",
//        "   Action Bits P0 S1",
//        "   Action Bits P1 S0",
//        "   Action Bits P1 S1",
};

JNIEXPORT jstring JNICALL Java_com_DumpRegisters(JNIEnv *env, jclass arg) {
    uint32_t ui;
    char strTemp[2048];
    char str[128];

    if (PDGInit()) {
        LOGE("PDGInit failed\n");
        return NULL;
    }

    memset(strTemp, 0x00, 1024);
    for (int reg=2; reg < (sizeof(reg_name)/sizeof(reg_name[0])); reg++) {
        if (PDGRead(reg, &ui) < 0) {
            return NULL;
        } else {
            sprintf(str,"%s(R%02d) = 0x%08X\n", reg_name[reg], reg, ui);
            strcat(strTemp, str);
        }
    }

    return (*env)->NewStringUTF(env, strTemp);
}

JNIEXPORT jstring JNICALL Java_com_SpiTest(JNIEnv *env, jclass arg) {
    uint32_t ui;
    int done_count = 0;
    char str[128];

    if (PDGInit()) {
        LOGE("PDGInit failed\n");
        return NULL;
    }

    for (int i=0; i<100; i++) {
        if (PDGWrite(6, 10000+i) < 0) {
            return NULL;
        }
        if (PDGRead(6, &ui) < 0) {
            return NULL;
        }
        if(ui == 10000+i) done_count++;
    }
    sprintf(str,"Success Rate: %d/%d\n", done_count, 100);

    return (*env)->NewStringUTF(env, str);
}

static char skip = 0;

JNIEXPORT jint JNICALL Java_com_MCU2FIFO(JNIEnv *env, jclass arg) {
    LOGI("Enter %s.", __FUNCTION__);

    char sendData[132];

    for(int i=0; i<132; i++) {
        sendData[i] = skip + i;
    }
    skip++;

    if (PDGInit()) {
        LOGE("PDGInit failed\n");
        return -1;
    }

    unsigned char buffer[135];

    buffer[0] = PDG_WRITE_FIFO;
    buffer[1] = 33;
    memcpy((buffer+2), sendData, 132);
    buffer[134] = 0;
    if (SPIMessage(buffer, 135) < 0) return -1;

    usleep(1000);           // Sleep 1ms 等待硬件回暖

    uint32_t ui;
    if (PDGRead(2, &ui) < 0) {
        return -1;
    }

    LOGI("Exit %s. R2=%d", __FUNCTION__, ui);
    return ui;
}

JNIEXPORT jint JNICALL Java_com_FIFO2MCU(JNIEnv *env, jclass arg) {
    LOGI("Enter %s.", __FUNCTION__);

    if (PDGInit()) {
        LOGE("PDGInit failed\n");
        return -1;
    }

    unsigned char buffer[135];

    buffer[0] = PDG_READ_FIFO;
    buffer[1] = 33;
    memset((buffer+2), 0x00, 132);
    buffer[134] = 0;
    if (SPIMessage(buffer, 135) < 0) return -1;

    usleep(1000);           // Sleep 1ms 等待硬件回暖

    uint32_t ui;
    if (PDGRead(3, &ui) < 0) {
        return -1;
    }

    LOGI("Exit %s. R3=%d", __FUNCTION__, ui);
    return ui;
}

JNIEXPORT jint JNICALL Java_com_FIFO2DDR(JNIEnv *env, jclass arg) {
    LOGI("Enter %s.", __FUNCTION__);

    if (PDGInit()) {
        LOGE("PDGInit failed\n");
        return -1;
    }

    unsigned char buffer[135];

    buffer[0] = PDG_MEM_TRANS;
    buffer[1] = 32;
    buffer[2] = FIFO_2_DDR;         // FIFO -> DDR
    buffer[3] = 0;                  // To Address:0
    buffer[4] = 0;
    buffer[5] = 0;
    buffer[6] = 0;
    buffer[7] = 0;

    if (SPIMessage(buffer, 135) < 0) return -1;

    usleep(1000);           // Sleep 1ms 等待硬件回暖

    uint32_t ui;
    if (PDGRead(2, &ui) < 0) {
        return -1;
    }

    LOGI("Exit %s. R2=%d", __FUNCTION__, ui);
    return ui;
}

JNIEXPORT jint JNICALL Java_com_DDR2FIFO(JNIEnv *env, jclass arg) {
    LOGI("Enter %s.", __FUNCTION__);

    if (PDGInit()) {
        LOGE("PDGInit failed\n");
        return -1;
    }

    unsigned char buffer[135];

    buffer[0] = PDG_MEM_TRANS;
    buffer[1] = 32;
    buffer[2] = DDR_2_FIFO;         // DDR -> FIFO
    buffer[3] = 0;                  // From Address:0
    buffer[4] = 0;
    buffer[5] = 0;
    buffer[6] = 0;
    buffer[7] = 0;

    if (SPIMessage(buffer, 135) < 0) return -1;

    usleep(1000);           // Sleep 1ms 等待硬件回暖

    uint32_t ui;
    if (PDGRead(3, &ui) < 0) {
        return -1;
    }

    LOGI("Exit %s. R3=%d", __FUNCTION__, ui);
    return ui;
}
#endif // if 0

pthread_t PrintThread = (pthread_t)NULL;
static bool CancelPrint = false;
// INK_POLL_SEC - when pen is On, ink is polled at this frequency for PILS use
#define INK_POLL_SEC 1
// SECURE_INK_POLL_SEC - secure ink is polled at this frequency (must be < 60 seconds)
#define SECURE_INK_POLL_SEC 20

static SupplyStatus_t supply_status;
static SupplyInfo_t supply_info;

void *_print_thread(void *arg) {
    int secure_sec = 0;
    float ink_weight;

    LOGD("_print_thread");

    while (!CancelPrint) {
        // sleep until next poll, then increment time counters
        sleep(INK_POLL_SEC);
        secure_sec += INK_POLL_SEC;

        ids_get_supply_status(IDS_INSTANCE, sIdsIdx, &supply_status);

        // NON-SECURE ink use (for PILS algorithm)
        ink_weight = GetInkWeight(sPenIdx);
        if (ink_weight < 0) {
            LOGD("GetInkWeight failed.");
        } else {
            LOGD("GetInkWeight");
        }

        // SECURE ink use
        if (secure_sec >= SECURE_INK_POLL_SEC) {
            secure_sec = 0;
            if(GetAndProcessInkUse(sPenIdx, sIdsIdx) < 0) {
                LOGD("GetAndProcessInkUse failed.");
            } else {
                LOGD("GetAndProcessInkUse");
            }
        }
    }

    PrintThread = (pthread_t)NULL;     // (done printing)
    return (void*)NULL;
}


extern char ERR_STRING[];

JNIEXPORT jint JNICALL Java_com_StartPrint(JNIEnv *env, jclass arg) {
    if (pd_check_ph("pd_power_on", pd_power_on(PD_INSTANCE, sPenIdx), sPenIdx)) {
//        LOGE("%s\n", ERR_STRING);
        return -1;
    }

    CancelPrint = false;

    if (pthread_create(&PrintThread, NULL, _print_thread, NULL)) {
        PrintThread = (pthread_t)NULL;
        LOGE("ERROR: pthread_create() of PrintThread failed\n");
        return -1;
    }

    return 0;
}

JNIEXPORT jint JNICALL Java_com_StopPrint(JNIEnv *env, jclass arg) {
    CancelPrint = true;

    if (pd_check_ph("pd_power_off", pd_power_off(PD_INSTANCE, sPenIdx), sPenIdx)) {
//        LOGE("%s\n", ERR_STRING);
        return -1;
    }
    return 0;
}

JNIEXPORT jstring JNICALL Java_com_GetErrorString(JNIEnv *env, jclass arg) {
    return (*env)->NewStringUTF(env, ERR_STRING);
}

JNIEXPORT jint JNICALL Java_com_GetConsumedVol(JNIEnv *env, jclass arg) {
    return supply_status.consumed_volume;
}

JNIEXPORT jint JNICALL Java_com_GetUsableVol(JNIEnv *env, jclass arg) {
    return supply_info.usable_vol;
}

static IdsSysInfo_t ids_sys_info;

JNIEXPORT jint JNICALL Java_com_hp22mm_init_ids(JNIEnv *env, jclass arg, jint idsIndex) {
    LOGI("Initializing IDS%d....\n", idsIndex);

    IDSResult_t ids_r;

    sIdsIdx = idsIndex;

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
/*
    LOGD("Supply 1 Read/Write Test ________\n");

    uint32_t ui32;
    char sc_string[BUFFER_SIZE];

    // read OEM RW field 1
    ids_r = ids_read_oem_field(IDS_INSTANCE, 1, OEM_RW_1, &ui32);
    ids_check("ids_read_oem_field", ids_r);
    LOGD("Read OEM_RW_1 = %u\n", ui32);
    // write OEM RW field 1
    ids_r = ids_write_oem_field(IDS_INSTANCE, 1, OEM_RW_1, ++ui32);
    ids_check("ids_write_oem_field", ids_r);
    LOGD("Write OEM_RW_1 = %u\n", ui32);
    // read OEM RW field 1
    ids_r = ids_read_oem_field(IDS_INSTANCE, 1, OEM_RW_1, &ui32);
    ids_check("ids_read_oem_field", ids_r);
    LOGD("Read OEM_RW_1 after increment = %u\n", ui32);

    // write Reorder string (12 characters)
    snprintf(sc_string, BUFFER_SIZE, "Test IDS001 ");
    printf("Writing STR_REORDER_PN = %s\n", sc_string);
    ids_r = ids_write_oem_string(IDS_INSTANCE, 1, STR_REORDER_PN, strlen(sc_string), (uint8_t*)sc_string);
    ids_check("ids_write_oem_string", ids_r);
    LOGD("Write STR_REORDER_PN = %s\n", sc_string);

    // read Reorder string
    memset((void*)sc_string, 0, BUFFER_SIZE);
    ids_r = ids_read_oem_string(IDS_INSTANCE, 1, STR_REORDER_PN, BUFFER_SIZE, (uint8_t*)sc_string);
    ids_check("ids_read_oem_string", ids_r);
    LOGD("Read STR_REORDER_PN = %s\n", sc_string);
*/
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

JNIEXPORT jint JNICALL Java_com_hp22mm_init_pd(JNIEnv *env, jclass arg, jint penIndex) {
    LOGI("Initializing PD(PEN%d)....\n", penIndex);

    PDResult_t pd_r;

    sPenIdx = penIndex;

    pd_r = pd_lib_init();
    if (pd_check("pd_lib_init", pd_r)) return -1;
    pd_r = pd_init(PD_INSTANCE);
    if (pd_check("pd_init", pd_r)) return -1;
    pd_r = pd_get_system_status(PD_INSTANCE, &pd_system_status);
    if (pd_check("pd_get_system_status", pd_r)) return -1;
/*
    LOGD("PEN%d\nFW Rev = %d.%d\nBootloader Rev = %d.%d\nFPGA Rev = %d.%d\nBlur board Rev = %d\nDriver Board0 = %d, Board1 = %d\nStatus = %d\nBoard ID = %d",
         sPenIdx,
         pd_system_status.fw_rev_major, pd_system_status.fw_rev_minor,
         pd_system_status.boot_rev_major, pd_system_status.boot_rev_minor,
         pd_system_status.fpga_rev_major, pd_system_status.fpga_rev_minor,
         pd_system_status.blur_board_rev,
         pd_system_status.driver_board0_rev, pd_system_status.driver_board1_rev,
         pd_system_status.pd_status,
         pd_system_status.board_id);
*/
/*
    sleep(2);

    // NON-SECURE ink use (for PILS algorithm)
    LOGD("Ink Weight = %d\n", GetInkWeight(0));
    GetAndProcessInkUse(0, 1);

    LOGD("PD 0 Read/Write Test ________\n");

    uint32_t ui32;
    uint8_t sc_result;
    // read OEM RW field 1
    pd_r = pd_sc_read_oem_field(PD_INSTANCE, 0, PD_SC_OEM_RW_FIELD_1, &ui32, &sc_result);
    pd_check("pd_sc_read_oem_field", pd_r);
    LOGD("Read PD_SC_OEM_RW_FIELD_1 = %u\n", ui32);

    // write OEM RW field 1
    pd_r = pd_sc_write_oem_field(PD_INSTANCE, 0, PD_SC_OEM_RW_FIELD_1, ++ui32, &sc_result);
    pd_check("pd_sc_write_oem_field", pd_r);
    LOGD("Write PD_SC_OEM_RW_FIELD_1 = %u\n", ui32);

    // read OEM RW field 1
    pd_r = pd_sc_read_oem_field(PD_INSTANCE, 0, PD_SC_OEM_RW_FIELD_1, &ui32, &sc_result);
    pd_check("pd_sc_read_oem_field", pd_r);
    LOGD("Read PD_SC_OEM_RW_FIELD_1 = %u\n", ui32);


    char sc_string[BUFFER_SIZE];
    snprintf(sc_string, BUFFER_SIZE, "AAAA PD002  ");
    // write Reorder string (12 characters)
    LOGD("Write PD_SC_OEM_STR_REORDER_PN = %s\n", sc_string);
    pd_r = pd_sc_write_oem_string_field(PD_INSTANCE, 0, PD_SC_OEM_STR_REORDER_PN, sc_string);
    pd_check("pd_sc_write_oem_string_field", pd_r);
    // read Reorder string (in  SC info)
    memset((void*)sc_string, 0, BUFFER_SIZE);
    pd_r = pd_sc_read_oem_string_field(PD_INSTANCE, 0, PD_SC_OEM_STR_REORDER_PN, sc_string, BUFFER_SIZE);
    pd_check("pd_sc_read_oem_string_field", pd_r);
    LOGD("Read PD_SC_OEM_STR_REORDER_PN = %s\n", sc_string);
*/
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

static PrintHeadStatus print_head_status;
static PDSmartCardInfo_t pd_sc_info;
static PDSmartCardStatus pd_sc_status;

JNIEXPORT jint JNICALL Java_com_pd_get_print_head_status(JNIEnv *env, jclass arg) {
    PDResult_t pd_r;

    if (pd_check("pd_get_print_head_status", pd_get_print_head_status(PD_INSTANCE, sPenIdx, &print_head_status))) return (-1);
    if (print_head_status.print_head_state != PH_STATE_PRESENT && print_head_status.print_head_state != PH_STATE_POWERED_OFF) {
        LOGE("Print head state not valid. print_head_state=%d, print_head_error=%d\n", (int)print_head_status.print_head_state, (int)print_head_status.print_head_error);
        return (-1);
    }

    if (print_head_status.print_head_state >= PH_STATE_NOT_PRESENT) {
        LOGE("Print head state[%d]\n", print_head_status.print_head_state);
        return (-1);
    }

    if (print_head_status.print_head_error != PH_NO_ERROR) {
        LOGE("Print head error = %s\n", ph_error_description(print_head_status.print_head_error));
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

JNIEXPORT jint JNICALL Java_com_pd_sc_get_status(JNIEnv *env, jclass arg) {
    PDResult_t pd_r;

    uint8_t pd_sc_result;
    if (pd_check("pd_sc_get_status", pd_sc_get_status(PD_INSTANCE, sPenIdx, &pd_sc_status, &pd_sc_result)) || pd_sc_result != 0) {
        LOGE("pd_sc_get_status error\n");
        return (-1);
    }

    LOGD("============= PDSmartCardStatus =============\n");
    LOGD("out_of_ink : %s\n", (pd_sc_status.out_of_ink ? "out of ink" : "Not out of ink"));     /**< Out of ink. Used only in the case of single-use printheads. 0 = Not out of ink, 1 = out of ink */
    LOGD("purge_complete_slot_a : %s\n", (pd_sc_status.purge_complete_slot_a ? "Purge completed" : "Purge not completed")); /**< Shipping fluid Purge complete for slot A. 0 = Purge not complete, 1 = Purge completed */
    LOGD("purge_complete_slot_b : %s\n", (pd_sc_status.purge_complete_slot_b ? "Purge completed" : "Purge not completed")); /**< Shipping fluid Purge complete for slot B. 0 = Purge not complete, 1 = Purge completed */
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
            "Slot A = %s\nSlot B = %s\nFaulty = %s",
            (pd_sc_status.purge_complete_slot_a ? "Purge Complete" : "Not Purged"),
            (pd_sc_status.purge_complete_slot_b ? "Purge Complete" : "Not Purged"),
            (pd_sc_status.faulty_replace_immediately ? "True" : "False"));

    return (*env)->NewStringUTF(env, strTemp);
}

JNIEXPORT jint JNICALL Java_com_pd_sc_get_info(JNIEnv *env, jclass arg) {
    PDResult_t pd_r;

    uint8_t pd_sc_result;
    if (pd_check("pd_sc_get_info", pd_sc_get_info(PD_INSTANCE, sPenIdx, &pd_sc_info, &pd_sc_result)) || pd_sc_result != 0) {
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
    if (DoPairing(sIdsIdx, sPenIdx)) {
        LOGE("DoPairing failed!\n");
        return (-1);
    };
    return 0;
}

JNIEXPORT jint JNICALL Java_com_DoOverrides(JNIEnv *env, jclass arg) {
    if (DoOverrides(sIdsIdx, sPenIdx)) {
        LOGE("DoOverrides failed!\n");
        return (-1);
    }
    return 0;
}

JNIEXPORT jint JNICALL Java_com_Pressurize(JNIEnv *env, jclass arg) {
    return CmdPressurize();
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

    pd_r = pd_micro_fw_reflash(PD_INSTANCE, "/mnt/usbhost1/PD.s19", true);
    if (pd_check("pd_micro_fw_reflash_no_reset", pd_r)) {
        pd_r = pd_micro_fw_reflash(PD_INSTANCE, "/mnt/usbhost1/PD.s19", true);
        if (pd_check("pd_micro_fw_reflash_no_reset", pd_r)) {
            return (-1);
        }
    }

    return 0;
}

JNIEXPORT jint JNICALL Java_com_UpdateFPGAFlash(JNIEnv *env, jclass arg) {
    PDResult_t pd_r;

    pd_r = pd_fpga_fw_reflash(PD_INSTANCE, "/mnt/usbhost1/FPGA.s19", true);
    if (pd_check("pd_fpga_fw_reflash", pd_r)) {
        pd_r = pd_fpga_fw_reflash(PD_INSTANCE, "/mnt/usbhost1/FPGA.s19", true);
        if (pd_check("pd_fpga_fw_reflash", pd_r)) {
            return (-1);
        }
    }

    return 0;
}

JNIEXPORT jint JNICALL Java_com_UpdateIDSFW(JNIEnv *env, jclass arg) {
    IDSResult_t ids_r;

    ids_r = ids_micro_fw_reflash(IDS_INSTANCE, "/mnt/usbhost1/IDS.s19", true);
    if (ids_check("ids_micro_fw_reflash", ids_r)) {
        ids_r = ids_micro_fw_reflash(IDS_INSTANCE, "/mnt/usbhost1/IDS.s19", true);
        if (ids_check("ids_micro_fw_reflash", ids_r)) {
            return (-1);
        }
    }

    return 0;
}

#define SUPPLY_PRESSURE 6.0
#define PRESSURIZE_SEC 120  // 两分钟

#define LED_R 0
#define LED_Y 1
#define LED_G 2
#define LED_OFF 0
#define LED_ON 1
#define LED_BLINK 2

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
            return -1;
        }
    } while (!pressurized);

    LOGD("Opening ink valves...\n");

    IDS_GPIO_SetBits(sIdsIdx, COMBO_INK_BOTH);        // ink valve On/Hold

    sleep(1);

    IDS_GPIO_ClearBits(sIdsIdx, GPIO_O_INK_VALVE_ON); // turn Off ink valve (leave Hold)
//    IDS_MonitorPILS(sIdsIdx);

    IsPressurized = true;

    return 0;
}

int CmdPressurize() {
    return _CmdPressurize(-1);
}

void CmdDepressurize() {
    IsPressurized = false;

    IDS_GPIO_ClearBits(sIdsIdx, COMBO_INK_AIR_PUMP_ALL);     // pump disabled; ink/air valves closed
    IDS_MonitorOff(sIdsIdx);
    IDS_LED_Off(sIdsIdx, LED_Y);

    LOGD("Depressurizing...\n");
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
        {"pd_get_print_head_status",		"()I",	                    (void *)Java_com_pd_get_print_head_status},
        {"pd_get_print_head_status_info",   "()Ljava/lang/String;",     (void *)Java_com_pd_get_print_head_status_info},
        {"pd_sc_get_status",		"()I",	                    (void *)Java_com_pd_sc_get_status},
        {"pd_sc_get_status_info",   "()Ljava/lang/String;",     (void *)Java_com_pd_sc_get_status_info},
        {"pd_sc_get_info",		"()I",	                    (void *)Java_com_pd_sc_get_info},
        {"pd_sc_get_info_info",   "()Ljava/lang/String;",     (void *)Java_com_pd_sc_get_info_info},
        {"DeletePairing",		            "()I",	                    (void *)Java_com_DeletePairing},
        {"DoPairing",		                "()I",	                    (void *)Java_com_DoPairing},
        {"DoOverrides",		                "()I",	                    (void *)Java_com_DoOverrides},
        {"Pressurize",		                "()I",	                    (void *)Java_com_Pressurize},
        {"getPressurizedValue",	            "()Ljava/lang/String;",     (void *)Java_com_getPressurizedValue},
        {"Depressurize",		            "()I",	                    (void *)Java_com_Depressurize},
        {"_startPrint",	    "()I",	    (void *)Java_com_StartPrint},
        {"_stopPrint",		                "()I",	                    (void *)Java_com_StopPrint},
        {"getErrString",		            "()Ljava/lang/String;",	                    (void *)Java_com_GetErrorString},
        {"getConsumedVol",		                "()I",	                    (void *)Java_com_GetConsumedVol},
        {"getUsableVol",		                "()I",	                    (void *)Java_com_GetUsableVol},
        {"UpdatePDFW",		                "()I",	                    (void *)Java_com_UpdatePDFW},
        {"UpdateFPGAFlash",		            "()I",	                    (void *)Java_com_UpdateFPGAFlash},
        {"UpdateIDSFW",		                "()I",	                    (void *)Java_com_UpdateIDSFW},

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
