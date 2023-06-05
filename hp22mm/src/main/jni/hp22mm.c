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

#define VERSION_CODE                            "1.0.048"

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

#define SPI_DEV_NAME "/dev/spidev0.0"
#define SPI_BITS 8
#define SPI_SPEED 8000000
int spidev = -1;

#define IMAGE_FILE "/system/lib/image.bin"
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

static bool IsPressurized = false;
void CmdDepressurize();
int CmdPressurize();

int PDGInit() {
    if(spidev >=0 ) return 0;

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

int SPIMessage(unsigned char *message, int length) {
    struct spi_ioc_transfer transfer[length];

    LOGD("Sent: [0x%02X, 0x%02X, 0x%02X, 0x%02X, 0x%02X, 0x%02X, 0x%02X]", *(message+0), *(message+1), *(message+2), *(message+3), *(message+4), *(message+5), *(message+6));
    // fill in transfer array for each byte (non-default, non-zero)
    for (int i=0; i<length; i++) {
        memset(&(transfer[i]), 0, sizeof(transfer[i]));
        transfer[i].tx_buf  = (unsigned long)(message+i);
        transfer[i].rx_buf  = (unsigned long)(message+i);
        transfer[i].len = sizeof(*(message+i));
    }
    // execute message
    if (ioctl(spidev, SPI_IOC_MESSAGE(length), &transfer) < 0) {
        LOGE("ERROR: ioctl message failed for %s (%s)\n", SPI_DEV_NAME, strerror(errno));
        return -1;
    }

    LOGD("Recv: [0x%02X, 0x%02X, 0x%02X, 0x%02X, 0x%02X, 0x%02X, 0x%02X]", *(message+0), *(message+1), *(message+2), *(message+3), *(message+4), *(message+5), *(message+6));
    return 0;
}

#define REG_MSG_LEN 7

int PDGRead(unsigned char reg, uint32_t *four_bytes) {
    unsigned char bytes[REG_MSG_LEN];

    // Read sequence
    bytes[0] = 3;   // Read
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
    bytes[0] = 4;   // Write
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
    if (reg == 28) return 0;
    if (PDGRead(reg, &read_bytes) < 0 ||
        read_bytes != four_bytes)
        return -1;

    return 0;
}

void PDGWaitWBuffer(uint32_t wait_for) {
    return;
    uint32_t ui;

    while (true) {
        if (PDGRead(2, &ui) < 0 ||
            (ui == wait_for))
            break;
        LOGI("Waiting R2 for %u, ret = %u\n", wait_for, ui);

        usleep(500000);     // 0.5 sec
    }
}

void PDGWaitRBuffer(uint32_t wait_for) {
    return;
    uint32_t ui;

    while (true) {
        if (PDGRead(3, &ui) < 0 ||
            (ui == wait_for))
            break;
        LOGI("Waiting R3 for %u, ret = %u\n", wait_for, ui);

        usleep(500000);     // 0.5 sec
    }
}

int PDGWriteImage() {
    // get image file size
/* 暂时取消从文件获取数据
    struct stat st;
    if (stat(IMAGE_FILE, &st) != 0) {
        LOGE("ERROR: cannot process %s\n", IMAGE_FILE);
        return -1;
    }
    image_cols = (st.st_size * 8 / IMAGE_ROWS);
*/
    image_cols = 100;

    // open binary image
/*    FILE *file = fopen(IMAGE_FILE, "rb");
    if (file == NULL) return -1;
*/
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
        buffer[0] = 5;
        buffer[1] = write_words;
        memset((buffer+2), 0x5A, write_bytes);
/*        if (fread((buffer+2), 1, write_bytes, file) != write_bytes) {
            LOGE("ERROR: reading %s\n", IMAGE_FILE);
            return -1;
        }*/
        buffer[write_bytes+2] = 0;
        if (SPIMessage(buffer, buffer_size) < 0) return -1;
        PDGWaitWBuffer(write_words);

        // Generic command (write)
        buffer2[0] = 7;
        buffer2[1] = (write_words - 1);
        buffer2[2] = 0;
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
    int encoder = 1500;
    int tof_freq = 45000000;

    // use all 4 columns of selected pen
//    int col_mask = 0xf;   // PEN0
    int col_mask = 0xf0; // PEN1
//    int col_mask = 0xf;
//    if (PEN_IDX == 1) col_mask <<= 4;

    if (PDGWrite(15, encoder) < 0 ||    // R15 internal encoder period (divider of clock freq)
        PDGWrite(16, tof_freq) < 0 ||   // R16 internal TOF frequency (Hz)
        PDGWrite(17, 0) < 0 ||          // R17 0 = internal encoder
        PDGWrite(18, 2) < 0 ||          // R18 external encoder divider (2=600 DPI)
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

    int cnt = 0;
    int cnt1 = 0;
    int cnt2 = 0;
    while (true) {
        // check for print done (or cancel/error)
        if (PDGRead(25, &ui) < 0 ||     // (R25 print enable)
            ui == 0 ||                  // print NOT enabled
            CancelPrint)                // cancelled by user
            break;

        // 通过log输出打印的次数
        PDGRead(26, &cnt1);

        PDGRead(27, &cnt2);

        // delay before checking again
        usleep(PRINT_COMPLETE_CHECK_USEC*500);
    }
    LOGI("<<< Printing Complete >>>\n");
    if (PDGWrite(25, 0) < 0) {          // R25 0 - disable print
        LOGE("ERROR: cannot disable print\n");
    }
    pd_check_ph("pd_power_off", pd_power_off(PD_INSTANCE, 0), 0);
    PrintThread = (pthread_t)NULL;     // (done printing)
    return (void*)NULL;
}

int PDGTriggerPrint(int external, int count) {
    if (IsPrinting()) {
        LOGE("ERROR: already printing\n");
        return -1;
    }

    if (pd_check_ph("pd_power_on", pd_power_on(PD_INSTANCE, 0), 0)) {
        LOGE("ERROR: pd_power_on()\n");
        return -1;
    }

    CancelPrint = false;
    if (PDGWrite(17, external) < 0 ||    // R17 0=internal 1=external encoder
        PDGWrite(19, external) < 0 ||    // R19 0=internal 1=external TOF
        PDGWrite(26, 0) < 0 ||           // R26 init print count to 0
        PDGWrite(27, count) < 0 ||       // R27 set print count limit
        PDGWrite(25, 1) < 0) {            // R25 1 - enable print
        LOGE("ERROR: triggering print\n");
        return -1;
    }

    // start the print thread
    printf("Triggering print from ");
    printf(external ? "External sensors...\n" : "Internal sensors...\n");
    if (pthread_create(&PrintThread, NULL, _print_thread, NULL)) {
        PrintThread = (pthread_t)NULL;
        LOGE("ERROR: pthread_create() of PrintThread failed\n");
        return -1;
    }

    return 0;
}

void PDGCancelPrint() {
    CancelPrint = true;
}

int SPISend(unsigned char *message, int length) {
    struct spi_ioc_transfer transfer[length];

    LOGD("SPISend.Sent: [0x%02X, 0x%02X, 0x%02X, 0x%02X, 0x%02X, 0x%02X, 0x%02X]", *(message+0), *(message+1), *(message+2), *(message+3), *(message+4), *(message+5), *(message+6));
    // fill in transfer array for each byte (non-default, non-zero)
    for (int i=0; i<length; i++) {
        memset(&(transfer[i]), 0, sizeof(transfer[i]));
        transfer[i].tx_buf  = (unsigned long)(message+i);
//        transfer[i].rx_buf  = (unsigned long)(message+i);
        transfer[i].rx_buf  = NULL;
        transfer[i].len = sizeof(*(message+i));
    }
    // execute message
    if (ioctl(spidev, SPI_IOC_MESSAGE(length), &transfer) < 0) {
        LOGE("ERROR: ioctl message failed for %s (%s)\n", SPI_DEV_NAME, strerror(errno));
        return -1;
    }
    return 0;
}

int SPIRecv(unsigned char *message, int length) {
    struct spi_ioc_transfer transfer[length];

    // fill in transfer array for each byte (non-default, non-zero)
    for (int i=0; i<length; i++) {
        memset(&(transfer[i]), 0, sizeof(transfer[i]));
//        transfer[i].tx_buf  = (unsigned long)(message+i);
        transfer[i].tx_buf  = NULL;
        transfer[i].rx_buf  = (unsigned long)(message+i);
        transfer[i].len = sizeof(*(message+i));
    }
    // execute message
    if (ioctl(spidev, SPI_IOC_MESSAGE(length), &transfer) < 0) {
        LOGE("ERROR: ioctl message failed for %s (%s)\n", SPI_DEV_NAME, strerror(errno));
        return -1;
    }

    LOGD("SPIRecv.Recv: [0x%02X, 0x%02X, 0x%02X, 0x%02X, 0x%02X, 0x%02X, 0x%02X]", *(message+0), *(message+1), *(message+2), *(message+3), *(message+4), *(message+5), *(message+6));
    return 0;
}

// 容量4MB闪存，8个扇区，每个扇区256页，每页256字节；整个芯片有2048页或者524288字节。写入可以以页为单位，擦除要以扇区为单位
/* command list */
#define CMD_WREN                    (0x06)  /* Write Enable */
#define CMD_WRDI                    (0x04)  /* Write Disable */
#define CMD_RDID                    (0x9F)  /* Read ID */
#define CMD_RDSR                    (0x05)  /* Read Status Register */
#define CMD_WRSR                    (0x01)  /* Write Status Register */
#define CMD_READ                    (0x03)  /* Read Data Bytes */
#define CMD_FAST_READ               (0x0B)  /* Fast Read */
#define CMD_PP                      (0x02)  /* Page Program */
#define CMD_SE                      (0xD8)  /* Sector Erase */
#define CMD_BE                      (0xC7)  /* Bulk Erase */
#define CMD_DP                      (0xB9)  /* Deep Power-down */
#define CMD_RES                     (0xAH)  /* Release from Deep Power-down */
#define DUMMY                       (0xFF)

#define SPIFPGA_FILE "/sdcard/spifpga.bin"
JNIEXPORT jint JNICALL Java_com_WriteSPIFPGA(JNIEnv *env, jclass arg) {
    if (PDGInit()) {
        LOGE("PDGInit failed\n");
        return -1;
    }

    FILE *file = fopen(SPIFPGA_FILE, "rb");
    if (file == NULL) return -1;

    uint8_t cmd_buffer[4];
    uint8_t data_buffer[260];
    uint32_t page_addr = 0;

    cmd_buffer[0] = CMD_WREN;
    SPISend(cmd_buffer, 1);        // 设置允许写

    usleep(1000);           // Sleep 1ms 等待硬件回暖

    cmd_buffer[0] = CMD_BE;
    SPISend(cmd_buffer, 1);        // 擦除全盘

    while(!feof(file)) {
        memset(data_buffer, 0xFF, 260);
        data_buffer[0] = CMD_PP;
        data_buffer[1] = (uint8_t)(page_addr >> 16);
        data_buffer[2] = (uint8_t)(page_addr >> 8);
        data_buffer[3] = (uint8_t)(page_addr);

        fread((data_buffer+4), 1, 256, file);

        cmd_buffer[0] = CMD_WREN;
        SPISend(cmd_buffer, 1);        // 设置允许写
        usleep(1000);           // Sleep 1ms 等待硬件回暖
        SPISend(data_buffer, 260);    // 写一页数据
        usleep(1000);           // Sleep 1ms 等待硬件回暖

        page_addr += 256;
    }

    page_addr = 0;
    cmd_buffer[0] = CMD_WRDI;
    SPISend(cmd_buffer, 1);
    usleep(1000);           // Sleep 1ms 等待硬件回暖
    cmd_buffer[0] = CMD_READ;
    data_buffer[1] = (uint8_t)(page_addr >> 16);
    data_buffer[2] = (uint8_t)(page_addr >> 8);
    data_buffer[3] = (uint8_t)(page_addr);
    SPISend(cmd_buffer, 4);
    memset(data_buffer, 0x00, 256);
    SPIRecv(data_buffer, 256);

    page_addr += 256;
    cmd_buffer[0] = CMD_WRDI;
    SPISend(cmd_buffer, 1);
    usleep(1000);           // Sleep 1ms 等待硬件回暖
    cmd_buffer[0] = CMD_READ;
    data_buffer[1] = (uint8_t)(page_addr >> 16);
    data_buffer[2] = (uint8_t)(page_addr >> 8);
    data_buffer[3] = (uint8_t)(page_addr);
    SPISend(cmd_buffer, 4);
    memset(data_buffer, 0x00, 256);
    SPIRecv(data_buffer, 256);

    fclose(file);
}

JNIEXPORT jint JNICALL Java_com_StartPrint(JNIEnv *env, jclass arg) {
    if (!IsPressurized) {
        if (CmdPressurize() != 0) {
            LOGE("ERROR: Java_com_StartPrint() of PrintThread failed. Not Pressurized\n");
            return -1;
        }
    }

    if (PDGInit()) {
        LOGE("PDGInit failed\n");
        return -1;
    }

    if(PDGWriteImage() != 0) {
        LOGE("ERROR: PDGWriteImage failed\n");
        return -1;
    };

    if(PDGPrintSetup() != 0) {
        LOGE("ERROR: PDGPrintSetup failed\n");
        return -1;
    };

    return PDGTriggerPrint(0, 100);
}

JNIEXPORT jint JNICALL Java_com_StopPrint(JNIEnv *env, jclass arg) {
    PDGCancelPrint();
    return 0;
}

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

JNIEXPORT jint JNICALL Java_com_Write1Column(JNIEnv *env, jclass arg) {
    LOGI("Enter %s.", __FUNCTION__);

    char sendData[132];

    memset(sendData, 0x5A, 132);

    if (PDGInit()) {
        LOGE("PDGInit failed\n");
        return -1;
    }

    unsigned char buffer[135];

    buffer[0] = 5;
    buffer[1] = 33;
    memcpy((buffer+2), sendData, 132);
    buffer[134] = 0;
    if (SPIMessage(buffer, 135) < 0) return -1;

    buffer[0] = 6;
    buffer[1] = 33;
    memset((buffer+2), 0x00, 132);
    buffer[134] = 0;
    if (SPIMessage(buffer, 135) < 0) return -1;

    if(memcmp(sendData, buffer+2, 132) == 0) {
        LOGI("Writting 1 column data succeeded.\n");
        return 0;
    } else {
        LOGE("Writting 1 column data failed.\n");
        return -1;
    }
}

JNIEXPORT jint JNICALL Java_com_Write1KB(JNIEnv *env, jclass arg) {
    LOGI("Enter %s.", __FUNCTION__);

    char sendData[1024];

    memset(sendData, 0x5A, 1024);

    if (PDGInit()) {
        LOGE("PDGInit failed\n");
        return -1;
    }

    unsigned char buffer[1027];

    buffer[0] = 5;
    buffer[1] = 255;
    memcpy((buffer+2), sendData, 1020);
    buffer[1022] = 0;
    if (SPIMessage(buffer, 1023) < 0) return -1;

    buffer[0] = 6;
    buffer[1] = 255;
    memset((buffer+2), 0x00, 1020);
    buffer[1022] = 0;
    if (SPIMessage(buffer, 1023) < 0) return -1;

    if(memcmp(sendData, buffer+2, 1020) == 0) {
        LOGI("Writting 1KB data succeeded.\n");
        return 0;
    } else {
        LOGE("Writting 1KB data failed.\n");
        return -1;
    }
}

JNIEXPORT jint JNICALL Java_com_Write10Columns(JNIEnv *env, jclass arg) {
    LOGI("Enter %s.", __FUNCTION__);

    if (PDGInit()) {
        LOGE("PDGInit failed\n");
        return -1;
    }

    unsigned int addr = 0x0000;
    unsigned char buffer[135];
    unsigned char buffer2[8];
    unsigned char c = 0x11;

    for (int col=0; col < 10; col++) {
        buffer[0] = 5;          // To Write to FIFO
        buffer[1] = 33;
        memset((buffer+2), c, 132);
        buffer[134] = 0;
        if (SPIMessage(buffer, 135) < 0) return -1;
        PDGWaitWBuffer(33);

        // Generic command (write)
        buffer2[0] = 7;
        buffer2[1] = 32;
        buffer2[2] = 0;         // FIFO -> DDR
        buffer2[3] = (addr >> 24) & 0xff;
        buffer2[4] = (addr >> 16) & 0xff;
        buffer2[5] = (addr >> 8) & 0xff;
        buffer2[6] = addr & 0xff;
        buffer2[7] = 0;
        if (SPIMessage(buffer2, 8) < 0) return -1;
        PDGWaitWBuffer(0);

        addr += 132;
        c += 0x11;
    }

    addr = 0x0000;
    c = 0x11;
    char sendData[132];

    for (int col=0; col < 10; col++) {
        // Generic command (write)
        buffer2[0] = 7;
        buffer2[1] = 32;
        buffer2[2] = 1;     // DDR -> FIFO
        buffer2[3] = (addr >> 24) & 0xff;
        buffer2[4] = (addr >> 16) & 0xff;
        buffer2[5] = (addr >> 8) & 0xff;
        buffer2[6] = addr & 0xff;
        buffer2[7] = 0;
        if (SPIMessage(buffer2, 8) < 0) return -1;
        PDGWaitRBuffer(33);

        buffer[0] = 6;      // To Read from FIFO
        buffer[1] = 33;
        memset((buffer+2), 0x00, 132);
        buffer[134] = 0;
        if (SPIMessage(buffer, 135) < 0) return -1;
        PDGWaitRBuffer(0);

        memset(sendData, c, 132);
        if(memcmp(buffer+2, sendData, 132) != 0) {
            LOGE("Writting 10 columns data failed.\n");
            return -1;
        }

        addr += 132;
        c += 0x11;
    }

    LOGI("Writting 10 columns data succeeded.\n");
    return 0;
}


static IdsSysInfo_t ids_sys_info;

JNIEXPORT jint JNICALL Java_com_hp22mm_init_ids(JNIEnv *env, jclass arg) {
    LOGI("Initializing IDS....\n");

    IDSResult_t ids_r;

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

    if (IDS_Init(IDSCallback)) {
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
            SUPPLY_IDX,
            ids_sys_info.fw_major_rev, ids_sys_info.fw_minor_rev,
            ids_sys_info.fpga_major_rev, ids_sys_info.fpga_minor_rev,
            ids_sys_info.board_rev_bd1, ids_sys_info.board_rev_bd0, ids_sys_info.board_rev_bd,
            ids_sys_info.status,
            ids_sys_info.bootload_major, ids_sys_info.bootload_minor,
            ids_sys_info.board_id);

    return (*env)->NewStringUTF(env, strTemp);
}

static PDSystemStatus pd_system_status;

JNIEXPORT jint JNICALL Java_com_hp22mm_init_pd(JNIEnv *env, jclass arg) {
    LOGI("Initializing PD....\n");

    PDResult_t pd_r;

    pd_r = pd_lib_init();
    if (pd_check("pd_lib_init", pd_r)) return -1;
    pd_r = pd_init(PD_INSTANCE);
    if (pd_check("pd_init", pd_r)) return -1;
    pd_r = pd_get_system_status(PD_INSTANCE, &pd_system_status);
    if (pd_check("pd_get_system_status", pd_r)) return -1;

    LOGD("FW Rev = %d.%d\nBootloader Rev = %d.%d\nFPGA Rev = %d.%d\nBlur board Rev = %d\nDriver Board0 = %d, Board1 = %d\nStatus = %d\nBoard ID = %d",
         pd_system_status.fw_rev_major, pd_system_status.fw_rev_minor,
         pd_system_status.boot_rev_major, pd_system_status.boot_rev_minor,
         pd_system_status.fpga_rev_major, pd_system_status.fpga_rev_minor,
         pd_system_status.blur_board_rev,
         pd_system_status.driver_board0_rev, pd_system_status.driver_board1_rev,
         pd_system_status.pd_status,
         pd_system_status.board_id);
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

    ids_r = ids_set_stall_insert_count(IDS_INSTANCE, SUPPLY_IDX, IDS_STALL_INSERT);
    if (ids_check("ids_set_stall_insert_count", ids_r)) return (-1);

    return 0;
}

static SupplyStatus_t supply_status;
static SupplyInfo_t supply_info;
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
    if (ids_check("ids_get_supply_status", ids_get_supply_status(IDS_INSTANCE, SUPPLY_IDX, &supply_status))) return (-1);
    if (supply_status.state != SUPPLY_SC_VALID) {
        LOGE("Supply state not valid: %d[0x%02x]\n", (int)supply_status.state, supply_status.status_bits);
        return (-1);
    }

    if (supply_status.state == SUPPLY_SC_VALID) {
        if (ids_check("ids_get_supply_info", ids_get_supply_info(IDS_INSTANCE, SUPPLY_IDX, &supply_info))) return (-1);
        if (ids_check("ids_get_supply_id", ids_get_supply_id(IDS_INSTANCE, SUPPLY_IDX, &supply_id))) return (-1);

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

JNIEXPORT jint JNICALL Java_com_pd_get_print_head_status(JNIEnv *env, jclass arg, jint penIndex) {
    PDResult_t pd_r;

    if (pd_check("pd_get_print_head_status", pd_get_print_head_status(PD_INSTANCE, penIndex, &print_head_status))) return (-1);
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

JNIEXPORT jint JNICALL Java_com_pd_sc_get_status(JNIEnv *env, jclass arg, jint penIndex) {
    PDResult_t pd_r;

    uint8_t pd_sc_result;
    if (pd_check("pd_sc_get_status", pd_sc_get_status(PD_INSTANCE, penIndex, &pd_sc_status, &pd_sc_result)) || pd_sc_result != 0) {
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

JNIEXPORT jint JNICALL Java_com_pd_sc_get_info(JNIEnv *env, jclass arg, jint penIndex) {
    PDResult_t pd_r;

    uint8_t pd_sc_result;
    if (pd_check("pd_sc_get_info", pd_sc_get_info(PD_INSTANCE, penIndex, &pd_sc_info, &pd_sc_result)) || pd_sc_result != 0) {
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

JNIEXPORT jint JNICALL Java_com_DoPairing(JNIEnv *env, jclass arg, jint penIdx) {
    if (DoPairing(SUPPLY_IDX, penIdx)) {
        LOGE("DoPairing failed!\n");
        return (-1);
    };
    return 0;
}

JNIEXPORT jint JNICALL Java_com_DoOverrides(JNIEnv *env, jclass arg, jint penIdx) {
    if (DoOverrides(SUPPLY_IDX, penIdx)) {
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

    sprintf(strTemp,"Press Value = %f", ADCGetPressurePSI(SUPPLY_IDX));

    return (*env)->NewStringUTF(env, strTemp);
}

JNIEXPORT jint JNICALL Java_com_Depressurize(JNIEnv *env, jclass arg) {
    CmdDepressurize();
    return 0;
}

JNIEXPORT jint JNICALL Java_com_UpdatePDFW(JNIEnv *env, jclass arg) {
    PDResult_t pd_r;

    pd_r = pd_micro_fw_reflash(PD_INSTANCE, "/mnt/usbhost0/PD.s19", true);
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

    pd_r = pd_fpga_fw_reflash(PD_INSTANCE, "/mnt/usbhost0/FPGA.s19", true);
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

    ids_r = ids_micro_fw_reflash(IDS_INSTANCE, "/mnt/usbhost0/IDS.s19", true);
    if (ids_check("ids_micro_fw_reflash", ids_r)) {
        ids_r = ids_micro_fw_reflash(IDS_INSTANCE, "/mnt/usbhost1/IDS.s19", true);
        if (ids_check("ids_micro_fw_reflash", ids_r)) {
            return (-1);
        }
    }

    return 0;
}

#define SUPPLY_PRESSURE 6.0
#define PRESSURIZE_SEC 60

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

    IDS_MonitorPressure(SUPPLY_IDX);
    IDS_DAC_SetSetpointPSI(SUPPLY_IDX, pressure);        // set pressure target
    IDS_GPIO_ClearBits(SUPPLY_IDX, COMBO_INK_BOTH);      // ink Off
    IDS_GPIO_SetBits(SUPPLY_IDX, COMBO_AIR_PUMP_ALL);    // pump enabled; air valve On/Hold
    IDS_LED_On(SUPPLY_IDX, LED_Y);
    LOGD("Pressurizing Supply %d to %.1f psi...\n", SUPPLY_IDX, pressure);

    sleep(1);       // delay

    IDS_GPIO_ClearBits(SUPPLY_IDX, GPIO_O_AIR_VALVE_ON); // turn Off air valve (leave Hold)

    LOGD("Waiting on pumps...\n");
    int limit_sec = PRESSURIZE_SEC;
    bool pressurized;
    do {
        sleep(1);   // delay 1 sec

        pressurized = true;
        if (IDS_GPIO_ReadBit(SUPPLY_IDX, GPIO_I_AIR_PRESS_LOW)) pressurized = false;

        // check for timeout
        if (--limit_sec <= 0) {
            LOGE("ERROR: Supply not pressurized in %d seconds\n", PRESSURIZE_SEC);
            CmdDepressurize();
            return -1;
        }
    } while (!pressurized);

    LOGD("Opening ink valves...\n");

    IDS_GPIO_SetBits(SUPPLY_IDX, COMBO_INK_BOTH);        // ink valve On/Hold

    sleep(1);

    IDS_GPIO_ClearBits(SUPPLY_IDX, GPIO_O_INK_VALVE_ON); // turn Off ink valve (leave Hold)
//    IDS_MonitorPILS(SUPPLY_IDX);

    IsPressurized = true;

    return 0;
}

int CmdPressurize() {
    return _CmdPressurize(-1);
}

void CmdDepressurize() {
    IsPressurized = false;

    IDS_GPIO_ClearBits(SUPPLY_IDX, COMBO_INK_AIR_PUMP_ALL);     // pump disabled; ink/air valves closed
    IDS_MonitorOff(SUPPLY_IDX);
    IDS_LED_Off(SUPPLY_IDX, LED_Y);

    LOGD("Depressurizing...\n");
}

JNIEXPORT jint JNICALL Java_com_hp22mm_init(JNIEnv *env, jclass arg) {
    LOGI("Initializing hp22mm library....%s, PEN_IDX=%d\n", VERSION_CODE, PEN_IDX);

    IDSResult_t ids_r;
    PDResult_t pd_r;
    uint32_t ui32;
    int i;

    if (InitSystem()) return (-1);

    // Update PD MCU
//    pd_r = pd_micro_fw_reflash(PD_INSTANCE, "/mnt/sdcard/system/PD_FW_4_19.s19", true);
//    if (pd_check("pd_micro_fw_reflash_no_reset", pd_r)) return (-1);
//    return 0;

    // Update FPGA FLASH
//    pd_r = pd_fpga_fw_reflash(PD_INSTANCE, "/mnt/sdcard/system/FPGA_3_12.s19", true);
//    if (pd_check("pd_fpga_fw_reflash", pd_r)) return (-1);
//    return 0;

    // Update IDS MCU
//    ids_r = ids_micro_fw_reflash(IDS_INSTANCE, "/mnt/sdcard/system/IDS_FW_4_14.s19", true);
//    if (ids_check("ids_micro_fw_reflash", ids_r)) return (-1);
//    return 0;

    // Set platform info, date, and insertion count

    SetInfo();
    ids_r = ids_set_stall_insert_count(IDS_INSTANCE, SUPPLY_IDX, IDS_STALL_INSERT);
    if (ids_check("ids_set_stall_insert_count", ids_r)) return (-1);
/*
    FpgaRecord_t fpgaRecord[100];
    size_t rsize = 0;
    pd_r = pd_get_fpga_log(PD_INSTANCE, fpgaRecord, 100, &rsize);
    if (pd_check("pd_get_fpga_log", pd_r)) return (-1);
    for(int i=0; i<rsize; i++) {
        LOGD("FPGA LOG[%d]: %d_%d_%d_%d_%d_%d\n", i, fpgaRecord[i].timeStamp, fpgaRecord[i].b1, fpgaRecord[i].b2, fpgaRecord[i].b3, fpgaRecord[i].reply, fpgaRecord[i].result);
    }
*/
    char id_string[BUFFER_SIZE];
    // Check Supply
    SupplyStatus_t supply_status;
    float consumed;
    SupplyID_t supply_id;

    ids_r = ids_get_supply_status(IDS_INSTANCE, SUPPLY_IDX, &supply_status);
    if (ids_check("ids_get_supply_status", ids_r)) return (-1);
    if (supply_status.state != SUPPLY_SC_VALID) {
        LOGE("Supply state not valid: %d[0x%02x]\n", (int)supply_status.state, supply_status.status_bits);
        return (-1);
    }
    consumed = (float)supply_status.consumed_volume / 10.0;
    ids_r = ids_get_supply_id(IDS_INSTANCE, SUPPLY_IDX, &supply_id);
    if (ids_check("ids_get_supply_id", ids_r)) return (-1);
    snprintf(id_string, BUFFER_SIZE, "%d_%d_%d_%d_%d_%d_%d_%d_%d",
             supply_id.mfg_site, supply_id.mfg_line, supply_id.mfg_year,
             supply_id.mfg_woy, supply_id.mfg_dow, supply_id.mfg_hour,
             supply_id.mfg_min, supply_id.mfg_sec, supply_id.mfg_pos);
    LOGD("Supply %d: id = %s, status = 0x%02x, consumed volume = %0.1f\n", SUPPLY_IDX,
              id_string, supply_status.status_bits, consumed);

    // Check Pen
    PrintHeadStatus print_head_status;
    PDSmartCardInfo_t pd_sc_info;
    uint8_t pd_sc_result;

    pd_r = pd_get_print_head_status(PD_INSTANCE, PEN_IDX, &print_head_status);
    if (pd_check("pd_get_print_head_status", pd_r)) return (-1);
    if (print_head_status.print_head_state != PH_STATE_PRESENT && print_head_status.print_head_state != PH_STATE_POWERED_OFF) {
        LOGE("Print head state not valid: %d, %d\n", (int)print_head_status.print_head_state, (int)print_head_status.print_head_error);
        return (-1);
    }

    pd_r = pd_sc_get_info(PD_INSTANCE, PEN_IDX, &pd_sc_info, &pd_sc_result);
    if (pd_check("pd_sc_get_info", pd_r)) return (-1);
    if (pd_sc_result != 0) {
        LOGE("pd_sc_get_info() failed, status = %d\n", (int)pd_sc_result);
        return (-1);
    }
    snprintf(id_string, BUFFER_SIZE, "%d_%d_%d_%d_%d_%d_%d_%d_%d",
             pd_sc_info.ctrdg_fill_site_id, pd_sc_info.ctrdg_fill_line, pd_sc_info.ctrdg_fill_year,
             pd_sc_info.ctrdg_fill_woy, pd_sc_info.ctrdg_fill_dow, pd_sc_info.ctrdg_fill_hour,
             pd_sc_info.ctrdg_fill_min, pd_sc_info.ctrdg_fill_sec, pd_sc_info.ctrdg_fill_procpos);
    LOGD("Pen %d: id = %s, state = %s, error = %s\n", PEN_IDX,
              id_string, ph_state_description(print_head_status.print_head_state), ph_error_description(print_head_status.print_head_error));

    // @@@ Pair Pen (both slots) with Supply @@@

    // delete pairing and reset sequence
    if (DeletePairing()) {
        LOGE("DeletePairing failed!\n");
        return (-1);
    }

    // loop through pairing steps
    if (DoPairing(SUPPLY_IDX, PEN_IDX)) {
        LOGE("DoPairing failed!\n");
        return (-1);
    };

    // Overrides from Supply to Pen (both slots)
    if (DoOverrides(SUPPLY_IDX, PEN_IDX)) {
        LOGE("DoOverrides failed!\n");
        return (-1);
    }

    return 0;
}

/*
int main()
{
    IDSResult_t ids_r;
    PDResult_t pd_r;
    uint32_t ui32;
    int i;

    // @@@ enable logging levels @@@

    enable_log_level(DEBUG_LEVEL_ERROR);
    enable_log_level(DEBUG_LEVEL_WARNING);
    //enable_log_level(DEBUG_LEVEL_INFO_PROTOCOL);
    //enable_log_level(DEBUG_LEVEL_API);
    //enable_log_level(DEBUG_LEVEL_TRACE);


    // @@@ Initialize system @@@

    if (InitSystem()) exit(-1);


    // @@@ Set platform info, date, and insertion count @@@

    SetInfo();
    ids_r = ids_set_stall_insert_count(IDS_INSTANCE, SUPPLY_IDX, IDS_STALL_INSERT);
    if (ids_check("ids_set_stall_insert_count", ids_r)) exit(-1);


    // @@@ Check Supply @@@

    SupplyStatus_t supply_status;
    float consumed;
    SupplyID_t supply_id;
    char id_string[BUFFER_SIZE];

    ids_r = ids_get_supply_status(IDS_INSTANCE, SUPPLY_IDX, &supply_status);
    if (ids_check("ids_get_supply_status", ids_r)) exit(-1);
    if (supply_status.state != SUPPLY_SC_VALID)
    {
        printf("Supply state not valid: %d\n", (int)supply_status.state);
        exit(-1);
    }
    consumed = (float)supply_status.consumed_volume / 10.0;
    ids_r = ids_get_supply_id(IDS_INSTANCE, SUPPLY_IDX, &supply_id);
    if (ids_check("ids_get_supply_id", ids_r)) exit(-1);
    snprintf(id_string, BUFFER_SIZE, "%d_%d_%d_%d_%d_%d_%d_%d_%d",
        supply_id.mfg_site, supply_id.mfg_line, supply_id.mfg_year,
        supply_id.mfg_woy, supply_id.mfg_dow, supply_id.mfg_hour,
        supply_id.mfg_min, supply_id.mfg_sec, supply_id.mfg_pos);
    printf("Supply %d: id = %s, status = 0x%02x, consumed volume = %0.1f\n", SUPPLY_IDX, id_string,
        supply_status.status_bits, consumed);


    // @@@ Check Pen @@@

    PrintHeadStatus print_head_status;
    PDSmartCardInfo_t pd_sc_info;
    uint8_t pd_sc_result;

    pd_r = pd_get_print_head_status(PD_INSTANCE, PEN_IDX, &print_head_status);
    if (pd_check("pd_get_print_head_status", pd_r)) exit(-1);
    if (print_head_status.print_head_state != PH_STATE_PRESENT && print_head_status.print_head_state != PH_STATE_POWERED_OFF)
    {
        printf("Print head state not valid: %d\n", (int)print_head_status.print_head_state);
        exit(-1);
    }
    pd_r = pd_sc_get_info(PD_INSTANCE, PEN_IDX, &pd_sc_info, &pd_sc_result);
    if (pd_check("pd_sc_get_info", pd_r)) exit(-1);
    if (pd_sc_result != 0)
    {
        printf("pd_sc_get_info() failed, status = %d\n", (int)pd_sc_result);
        exit(-1);
    }
    snprintf(id_string, BUFFER_SIZE, "%d_%d_%d_%d_%d_%d_%d_%d_%d",
        pd_sc_info.ctrdg_fill_site_id, pd_sc_info.ctrdg_fill_line, pd_sc_info.ctrdg_fill_year,
        pd_sc_info.ctrdg_fill_woy, pd_sc_info.ctrdg_fill_dow, pd_sc_info.ctrdg_fill_hour,
        pd_sc_info.ctrdg_fill_min, pd_sc_info.ctrdg_fill_sec, pd_sc_info.ctrdg_fill_procpos);
    printf("Pen %d: id = %s, state = %s, error = %s\n", PEN_IDX, id_string,
        ph_state_description(print_head_status.print_head_state), ph_error_description(print_head_status.print_head_error));


    // @@@ Pair Pen (both slots) with Supply @@@

    if (EnablePairing)
    {
        // delete pairing and reset sequence
        if (DeletePairing()) exit(-1);

        // loop through pairing steps
        DoPairing(SUPPLY_IDX, PEN_IDX);
    }
    else
    {
        printf("Pairing SKIPPED\n");
    }


    // @@@ Overrides from Supply to Pen (both slots) @@@

    if (DoOverrides(SUPPLY_IDX, PEN_IDX)) exit(-1);


    // @@@ Read/write Supply SC @@@

    printf("Supply %d________\n", SUPPLY_IDX);

    time_t seconds;
    seconds = time(NULL);
    i = seconds % 100;
    char sc_string[BUFFER_SIZE];

    // read OEM RW field 1
    ids_r = ids_read_oem_field(IDS_INSTANCE, SUPPLY_IDX, OEM_RW_1, &ui32);
    if (ids_check("ids_read_oem_field", ids_r)) exit(-1);
    printf("  OEM_RW_1 = %u\n", ui32);
    // write OEM RW field 1
    ids_r = ids_write_oem_field(IDS_INSTANCE, SUPPLY_IDX, OEM_RW_1, ++ui32);
    if (ids_check("ids_write_oem_field", ids_r)) exit(-1);
    // read OEM RW field 1
    ids_r = ids_read_oem_field(IDS_INSTANCE, SUPPLY_IDX, OEM_RW_1, &ui32);
    if (ids_check("ids_read_oem_field", ids_r)) exit(-1);
    printf("  OEM_RW_1 after increment = %u\n", ui32);

    // write Reorder string (12 characters)
    snprintf(sc_string, BUFFER_SIZE, "Test %02d {}01", i);
    printf("  Writing STR_REORDER_PN = %s\n", sc_string);
    ids_r = ids_write_oem_string(IDS_INSTANCE, SUPPLY_IDX, STR_REORDER_PN, strlen(sc_string), (uint8_t*)sc_string);
    if (ids_check("ids_write_oem_string", ids_r)) exit(-1);
    // read Reorder string
    memset((void*)sc_string, 0, BUFFER_SIZE);
    ids_r = ids_read_oem_string(IDS_INSTANCE, SUPPLY_IDX, STR_REORDER_PN, BUFFER_SIZE, (uint8_t*)sc_string);
    if (ids_check("ids_read_oem_string", ids_r)) exit(-1);
    printf("  STR_REORDER_PN = %s\n", sc_string);


    // @@@ Read/write Pen SC @@@

    printf("Pen %d___________\n", PEN_IDX);

    uint8_t sc_result;

    // read OEM RW field 1
    pd_r = pd_sc_read_oem_field(PD_INSTANCE, PEN_IDX, PD_SC_OEM_RW_FIELD_1, &ui32, &sc_result);
    if (pd_check("pd_sc_read_oem_field", pd_r)) exit(-1);
    if (sc_result != 0)
    {
        printf("pd_sc_read_oem_field() failed, status %d\n", sc_result);
        exit(-1);
    }
    printf("  PD_SC_OEM_RW_FIELD_1 = %u\n", ui32);
    // write OEM RW field 1
    pd_r = pd_sc_write_oem_field(PD_INSTANCE, PEN_IDX, PD_SC_OEM_RW_FIELD_1, ++ui32, &sc_result);
    if (pd_check("pd_sc_write_oem_field", pd_r)) exit(-1);
    if (sc_result != 0)
    {
        printf("pd_sc_write_oem_field() failed, status %d\n", sc_result);
        exit(-1);
    }
    // read OEM RW field 1
    pd_r = pd_sc_read_oem_field(PD_INSTANCE, PEN_IDX, PD_SC_OEM_RW_FIELD_1, &ui32, &sc_result);
    if (pd_check("pd_sc_read_oem_field", pd_r)) exit(-1);
    printf("  PD_SC_OEM_RW_FIELD_1 after increment = %u\n", ui32);
    if (sc_result != 0)
    {
        printf("pd_sc_read_oem_field() failed, status %d\n", sc_result);
        exit(-1);
    }

    // write Reorder string (12 characters)
    snprintf(sc_string, BUFFER_SIZE, "Test %02d {}01", i);
    printf("  Writing PD_SC_OEM_STR_REORDER_PN = %s\n", sc_string);
    pd_r = pd_sc_write_oem_string_field(PD_INSTANCE, PEN_IDX, PD_SC_OEM_STR_REORDER_PN, sc_string);
    if (pd_check("pd_sc_write_oem_string_field", pd_r)) exit(-1);
    // read Reorder string (in  SC info)
    memset((void*)sc_string, 0, BUFFER_SIZE);
    pd_r = pd_sc_read_oem_string_field(PD_INSTANCE, PEN_IDX, PD_SC_OEM_STR_REORDER_PN, sc_string, BUFFER_SIZE);
    if (pd_check("pd_sc_read_oem_string_field", pd_r)) exit(-1);
    printf("  PD_SC_OEM_STR_REORDER_PN = %s\n", sc_string);


    // @@@ Power ON Pen @@@
    // NOTE: Pen cannot be powered ON without valid Pairing and Overrides.

    pd_r = pd_power_on(PD_INSTANCE, PEN_IDX);
    if (pd_check_ph("pd_power_on", pd_r, PEN_IDX)) exit(-1);
    printf("%s Pen %d powered ON\n", time_string(), PEN_IDX);


    // @@@ Poll for ink use and pass secure payloads @@@

    int total_sec = 0;
    int secure_sec = 0;
    float ink_weight;
    float ink_weight_total = 0;
    bool firsttime = true;

    do
    {
        // sleep until next poll, then increment time counters
        sleep(INK_POLL_SEC);
        total_sec += INK_POLL_SEC;
        secure_sec += INK_POLL_SEC;

        // NON-SECURE ink use (for PILS algorithm)
        ink_weight = GetInkWeight(PEN_IDX);
        if (ink_weight < 0) exit(-1);
        ink_weight_total += ink_weight;
        if (firsttime)
        {
            firsttime = false;
            printf("%s First Non-Secure ink poll completed (%.1f mg)\n", time_string(), ink_weight);
        }

        // SECURE ink use
        if (secure_sec >= SECURE_INK_POLL_SEC)
        {
            secure_sec = 0;
            if (GetAndProcessInkUse(PEN_IDX, SUPPLY_IDX) < 0) exit(-1);
            printf("%s Secure ink poll completed\n", time_string());
        }

    } while (total_sec < INK_POLL_TOTAL_SEC);
    printf("%s Total Non-Secure ink use = %.1f mg\n", time_string(), ink_weight);


    // @@@ Power OFF Pen @@@

    pd_r = pd_power_off(PD_INSTANCE, PEN_IDX);
    if (pd_check_ph("pd_power_off", pd_r, PEN_IDX)) exit(-1);
    printf("%s Pen %d powered OFF\n", time_string(), PEN_IDX);


    // @@@ Shutdown libraries and specific IDS/PD instances @@@

    ShutdownSystem();
}
*/
/**
 * HP22MM操作jni接口
 */
static JNINativeMethod gMethods[] = {
        {"init",					        "()I",	                    (void *)Java_com_hp22mm_init},
        {"init_ids",				        "()I",	                    (void *)Java_com_hp22mm_init_ids},
        {"ids_get_sys_info",	            "()Ljava/lang/String;",	    (void *)Java_com_ids_get_sys_info},
        {"init_pd",				            "()I",	                    (void *)Java_com_hp22mm_init_pd},
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
        {"DoPairing",		                "(I)I",	                    (void *)Java_com_DoPairing},
        {"DoOverrides",		                "(I)I",	                    (void *)Java_com_DoOverrides},
        {"Pressurize",		                "()I",	                    (void *)Java_com_Pressurize},
        {"getPressurizedValue",	            "()Ljava/lang/String;",     (void *)Java_com_getPressurizedValue},
        {"Depressurize",		            "()I",	                    (void *)Java_com_Depressurize},
        {"UpdatePDFW",		                "()I",	                    (void *)Java_com_UpdatePDFW},
        {"UpdateFPGAFlash",		            "()I",	                    (void *)Java_com_UpdateFPGAFlash},
        {"UpdateIDSFW",		                "()I",	                    (void *)Java_com_UpdateIDSFW},
        {"startPrint",		            "()I",	                    (void *)Java_com_StartPrint},
        {"stopPrint",		                "()I",	                    (void *)Java_com_StopPrint},
        {"dumpRegisters",	    "()Ljava/lang/String;",	    (void *)Java_com_DumpRegisters},
        {"Write1Column",		            "()I",	                    (void *)Java_com_Write1Column},
        {"Write1KB",		            "()I",	                    (void *)Java_com_Write1KB},
        {"Write10Columns",		            "()I",	                    (void *)Java_com_Write10Columns},
        {"WriteSPIFPGA",		            "()I",	                    (void *)Java_com_WriteSPIFPGA},

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
