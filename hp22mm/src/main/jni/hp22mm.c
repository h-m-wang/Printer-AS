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

#include <linux/i2c.h>
#include <linux/i2c-dev.h>
#include <unistd.h>
#include <fcntl.h>

#include "hp22mm.h"
#include "common.h"

#ifdef __cplusplus
extern "C"
{
#endif

#define VERSION_CODE                            "1.0.001.IDS_PD_INIT_ONLY"

/***********************************************************
 *  Customization
 *
 *  Settings for basic customization.
 ***********************************************************/
#define I2C_DEVICE "/dev/i2c-1"

JNIEXPORT jint JNICALL Java_com_hp22mm_init_ids(JNIEnv *env, jclass arg) {
    LOGI("Initializing IDS....\n");

    IDSResult_t ids_r;
    IdsSysInfo_t ids_sys_info;

    ids_r = ids_lib_init();
    if (ids_check("ids_lib_init", ids_r)) return -1;
    ids_r = ids_init(IDS_INSTANCE);
    if (ids_check("ids_init", ids_r)) return -1;
    ids_r = ids_info(IDS_INSTANCE, &ids_sys_info);
    if (ids_check("ids_info", ids_r)) return -1;
    LOGD("IDS FW = %d.%d\n", ids_sys_info.fw_major_rev, ids_sys_info.fw_minor_rev);

    return 0;
}

JNIEXPORT jint JNICALL Java_com_hp22mm_init_pd(JNIEnv *env, jclass arg) {
    LOGI("Initializing PD....\n");

    PDResult_t pd_r;
    PDSystemStatus pd_system_status;

    pd_r = pd_lib_init();
    if (pd_check("pd_lib_init", pd_r)) return -1;
    pd_r = pd_init(PD_INSTANCE);
    if (pd_check("pd_init", pd_r)) return -1;
    pd_r = pd_get_system_status(PD_INSTANCE, &pd_system_status);
    if (pd_check("pd_get_system_status", pd_r)) return -1;

    return 0;
}

JNIEXPORT jint JNICALL Java_com_hp22mm_init(JNIEnv *env, jclass arg) {
    LOGI("Initializing hp22mm library....%s, PEN_IDX=%d\n", VERSION_CODE, PEN_IDX);

    IDSResult_t ids_r;
    PDResult_t pd_r;
    uint32_t ui32;
    int i;

    // Initialize system
    int I2C_File = -1;
    I2C_File = open(I2C_DEVICE, O_RDWR);
    if (I2C_File < 0) {
        LOGE("Open %s failed!\n", I2C_DEVICE);
        return -1;          // failure
    }
    LOGI("Open %s succeded!\n", I2C_DEVICE);

    if (InitSystem()) return (-1);

    // Update PD MCU
//    pd_r = pd_micro_fw_reflash(PD_INSTANCE, "/mnt/sdcard/system/PD_FW.s19", true);
//    if (pd_check("pd_micro_fw_reflash_no_reset", pd_r)) return (-1);
//    return 0;

    // Update FPGA FLASH
//    pd_r = pd_fpga_fw_reflash(PD_INSTANCE, "/mnt/sdcard/system/FPGA_FW.s19", true);
//    if (pd_check("pd_fpga_fw_reflash", pd_r)) return (-1);
//    return 0;

    // Update IDS MCU
//    ids_r = ids_micro_fw_reflash(IDS_INSTANCE, "/mnt/sdcard/system/IDS_FW.s19", true);
//    if (ids_check("ids_micro_fw_reflash", ids_r)) return (-1);
//    return 0;

    // Set platform info, date, and insertion count

    SetInfo();
    ids_r = ids_set_stall_insert_count(IDS_INSTANCE, SUPPLY_IDX, IDS_STALL_INSERT);
    if (ids_check("ids_set_stall_insert_count", ids_r)) return (-1);

    FpgaRecord_t fpgaRecord[100];
    size_t rsize = 0;
    pd_r = pd_get_fpga_log(PD_INSTANCE, fpgaRecord, 100, &rsize);
    if (pd_check("pd_get_fpga_log", pd_r)) return (-1);
    for(int i=0; i<rsize; i++) {
        LOGD("FPGA LOG[%d]: %d_%d_%d_%d_%d_%d\n", i, fpgaRecord[i].timeStamp, fpgaRecord[i].b1, fpgaRecord[i].b2, fpgaRecord[i].b3, fpgaRecord[i].reply, fpgaRecord[i].result);
    }

    // Check Supply
    SupplyStatus_t supply_status;
    float consumed;
    SupplyID_t supply_id;
    char id_string[BUFFER_SIZE];

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

    int ret = 1;
    while(ret) {
        pd_r = pd_get_print_head_status(PD_INSTANCE, PEN_IDX, &print_head_status);
        if (pd_check("pd_get_print_head_status", pd_r)) return (-1);
        if (print_head_status.print_head_state != PH_STATE_PRESENT && print_head_status.print_head_state != PH_STATE_POWERED_OFF) {
            LOGE("Print head state not valid: %d, %d\n", (int)print_head_status.print_head_state, (int)print_head_status.print_head_error);
//            return (-1);
        } else {ret=0;}
    }

    pd_r = pd_sc_get_info(PD_INSTANCE, PEN_IDX, &pd_sc_info, &pd_sc_result);
    if (pd_check("pd_sc_get_info", pd_r)) exit(-1);
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
/*
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
*/
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
        {"init",					"()I",	                    (void *)Java_com_hp22mm_init},
        {"init_ids",				"()I",	                    (void *)Java_com_hp22mm_init_ids},
        {"init_pd",				"()I",	                    (void *)Java_com_hp22mm_init_pd},
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
