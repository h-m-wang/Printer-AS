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

#define VERSION_CODE                            "1.0.014"

/***********************************************************
 *  Customization
 *
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

    // fill in transfer array for each byte (non-default, non-zero)
    for (int i=0; i<length; i++) {
        memset(&(transfer[i]), 0, sizeof(transfer[i]));
        transfer[i].tx_buf  = (unsigned long)(message+i);
        transfer[i].rx_buf  = (unsigned long)(message+i);
        transfer[i].len = sizeof(*(message+i));
    }
    // execute message
    if (ioctl(spidev, SPI_IOC_MESSAGE(length), &transfer) < 0) {
        LOGE("ERROR: ioctl message failed for %s (%d)\n", SPI_DEV_NAME, errno);
        return -1;
    }

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

void PDGWaitBuffer(uint32_t wait_for) {
    uint32_t ui;

    while (true) {
        if (PDGRead(2, &ui) < 0 ||
            (ui == wait_for))
            break;
        LOGI("Waiting for %u, ret = %u\n", wait_for, ui);

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
        buffer[0] = 5;
        buffer[1] = write_words;
        if (fread((buffer+2), 1, write_bytes, file) != write_bytes) {
            LOGE("ERROR: reading %s\n", IMAGE_FILE);
            return -1;
        }
        buffer[write_bytes+2] = 0;
        if (SPIMessage(buffer, buffer_size) < 0) return -1;
        PDGWaitBuffer(write_words);

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
        PDGWaitBuffer(0);

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
    int reg = 7 + (PEN_IDX * 4);
    for (int col=0; col<4; col++) {
        if (PDGWrite(reg+col, 0x0000) < 0) return -1; // image address is 0
    }

    // calculate encoder and TOF values
    int encoder = (int)(CLOCK_HZ / ENCODER_FREQ_HZ);
    int tof_freq = (int)(TOF_PERIOD_SEC * CLOCK_HZ);

    // use all 4 columns of selected pen
    int col_mask = 0xf;
    if (PEN_IDX == 1) col_mask <<= 4;

    if (PDGWrite(15, encoder) < 0 ||    // R15 internal encoder period (divider of clock freq)
        PDGWrite(16, tof_freq) < 0 ||   // R16 internal TOF frequency (Hz)
        PDGWrite(17, 0) < 0 ||          // R17 0 = internal encoder
        PDGWrite(18, 2) < 0 ||          // R18 external encoder divider (2=600 DPI)
        PDGWrite(19, 0) < 0 ||          // R19 0 = internal TOF
        PDGWrite(20, IMAGE_TOF) < 0 ||  // R20 pen 0 encoder counts from TOF to start print
        PDGWrite(21, IMAGE_TOF) < 0 ||  // R21 pen 1 encoder counts from TOF to start print
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
    pd_check_ph("pd_power_off", pd_power_off(PD_INSTANCE, PEN_IDX), PEN_IDX);
    PrintThread = (pthread_t)NULL;     // (done printing)
    return (void*)NULL;
}

int PDGTriggerPrint(int external, int count) {
    if (IsPrinting()) {
        LOGE("ERROR: already printing\n");
        return -1;
    }

    if (pd_check_ph("pd_power_on", pd_power_on(PD_INSTANCE, PEN_IDX), PEN_IDX)) {
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

JNIEXPORT jint JNICALL Java_com_StartPrint(JNIEnv *env, jclass arg) {
/*    if (!IsPressurized) {
        if (CmdPressurize() != 0) {
            LOGE("ERROR: Java_com_StartPrint() of PrintThread failed. Not Pressurized\n");
            return -1;
        }
    }
*/
    if(PDGWriteImage() != 0) {
        LOGE("ERROR: PDGWriteImage failed\n");
        return -1;
    };

    if(PDGPrintSetup() != 0) {
        LOGE("ERROR: PDGPrintSetup failed\n");
        return -1;
    };

    return PDGTriggerPrint(0, 3);
}

JNIEXPORT jint JNICALL Java_com_StopPrint(JNIEnv *env, jclass arg) {
    PDGCancelPrint();
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

    if (PDGInit()) {
        LOGE("PDGInit failed\n");
        return -1;
    }

    return 0;
}

JNIEXPORT jstring JNICALL Java_com_ids_get_sys_info(JNIEnv *env, jclass arg) {
    char strTemp[256];

    sprintf(strTemp, "Hp22mm Lib REV. = %s\nFW Rev = %d.%d\nFPGA Rev = %d.%d\nBoard Rev bd1 = %d, bd0 = %d, bd = %d\nStatus = %d\nBootloader = %d.%d\nBoard ID = %d",
            VERSION_CODE,
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
    IDSResult_t ids_r;

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

        LOGD("ID = %s\n", GetSupplyID());
        LOGD("State = %d\n", supply_status.state);
        LOGD("Out of Ink = %s\n", (supply_status.status_bits & STATUS_OOI ? "True" : "False"));
        LOGD("Altered = %s\n", (supply_status.status_bits & STATUS_ALTERED ? "True" : "False"));
        LOGD("Expired = %s\n", (supply_status.status_bits & STATUS_EXPIRED ? "True" : "False"));
        LOGD("Faulty = %s\n", (supply_status.status_bits & STATUS_FAULTY ? "True" : "False"));
        LOGD("Consumed vol = %.1f\n", supply_status.consumed_volume/10.0);
        LOGD("Usable vol = %.1f\n", supply_info.usable_vol/10.0);
        LOGD("Sensor gain = %.1f\n", supply_info.sensor_gain / 100.0);
        LOGD("Ink density = %.1f", supply_info.ink_density / 1000.0);
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

    uint8_t pd_sc_result;
    if (pd_check("pd_sc_get_info", pd_sc_get_info(PD_INSTANCE, PEN_IDX, &pd_sc_info, &pd_sc_result)) || pd_sc_result != 0) {
        LOGE("pd_sc_get_info error\n");
        return (-1);
    }

    if (pd_check("pd_sc_get_status", pd_sc_get_status(PD_INSTANCE, PEN_IDX, &pd_sc_status, &pd_sc_result)) || pd_sc_result != 0) {
        LOGE("pd_sc_get_status error\n");
        return (-1);
    }

    LOGD("print_head_status.print_head_state = %d\nprint_head_error = %d\nenergy_calibrated = %d\ntemp_calibrated = %d\nslot_a_purge_completed = %d\nslot_b_purge_completed = %d\noverdrive_warning = %d\novertemp_warning = %d\nsupplyexpired_warning = %d",
         print_head_status.print_head_state,
         print_head_status.print_head_error,
         print_head_status.energy_calibrated,
         print_head_status.temp_calibrated,
         print_head_status.slot_a_purge_completed,
         print_head_status.slot_b_purge_completed,
         print_head_status.overdrive_warning,
         print_head_status.overtemp_warning,
         print_head_status.supplyexpired_warning);

    LOGD("ID = %d_%d_%d_%d_%d_%d_%d_%d_%d\n",
             pd_sc_info.ctrdg_fill_site_id, pd_sc_info.ctrdg_fill_line, pd_sc_info.ctrdg_fill_year,
             pd_sc_info.ctrdg_fill_woy, pd_sc_info.ctrdg_fill_dow, pd_sc_info.ctrdg_fill_hour,
             pd_sc_info.ctrdg_fill_min, pd_sc_info.ctrdg_fill_sec, pd_sc_info.ctrdg_fill_procpos);

    LOGD("Slot A = %s\n", (pd_sc_status.purge_complete_slot_a ? "Purge Complete" : "Not Purged"));
    LOGD("Slot B = %s\n", (pd_sc_status.purge_complete_slot_b ? "Purge Complete" : "Not Purged"));
    LOGD("Faulty = %s\n", (pd_sc_status.faulty_replace_immediately ? "True" : "False"));

    return 0;
}

JNIEXPORT jstring JNICALL Java_com_pd_get_print_head_status_info(JNIEnv *env, jclass arg) {
    char strTemp[1024];

    sprintf(strTemp,
            "print_head_state = %d\nprint_head_error = %d\nenergy_calibrated = %d\ntemp_calibrated = %d\nslot_a_purge_completed = %d\nslot_b_purge_completed = %d\noverdrive_warning = %d\novertemp_warning = %d\nsupplyexpired_warning = %d\nID = %d_%d_%d_%d_%d_%d_%d_%d_%d\nSlot A = %s\nSlot B = %s\nFaulty = %s",
            print_head_status.print_head_state,
            print_head_status.print_head_error,
            print_head_status.energy_calibrated,
            print_head_status.temp_calibrated,
            print_head_status.slot_a_purge_completed,
            print_head_status.slot_b_purge_completed,
            print_head_status.overdrive_warning,
            print_head_status.overtemp_warning,
            print_head_status.supplyexpired_warning,
            pd_sc_info.ctrdg_fill_site_id,
            pd_sc_info.ctrdg_fill_line,
            pd_sc_info.ctrdg_fill_year,
            pd_sc_info.ctrdg_fill_woy,
            pd_sc_info.ctrdg_fill_dow,
            pd_sc_info.ctrdg_fill_hour,
            pd_sc_info.ctrdg_fill_min,
            pd_sc_info.ctrdg_fill_sec,
            pd_sc_info.ctrdg_fill_procpos,
            (pd_sc_status.purge_complete_slot_a ? "Purge Complete" : "Not Purged"),
            (pd_sc_status.purge_complete_slot_b ? "Purge Complete" : "Not Purged"),
            (pd_sc_status.faulty_replace_immediately ? "True" : "False"));

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

    pd_r = pd_micro_fw_reflash(PD_INSTANCE, "/mnt/sdcard/system/PD_FW.s19", true);
    if (pd_check("pd_micro_fw_reflash_no_reset", pd_r)) return (-1);

    return 0;
}

JNIEXPORT jint JNICALL Java_com_UpdateFPGAFlash(JNIEnv *env, jclass arg) {
    PDResult_t pd_r;

    pd_r = pd_fpga_fw_reflash(PD_INSTANCE, "/mnt/sdcard/system/FPGA.s19", true);
    if (pd_check("pd_fpga_fw_reflash", pd_r)) return (-1);

    return 0;
}

JNIEXPORT jint JNICALL Java_com_UpdateIDSFW(JNIEnv *env, jclass arg) {
    IDSResult_t ids_r;

    ids_r = ids_micro_fw_reflash(IDS_INSTANCE, "/mnt/sdcard/system/IDS_FW.s19", true);
    if (ids_check("ids_micro_fw_reflash", ids_r)) return (-1);

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
