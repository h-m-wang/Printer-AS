/*
   Confidential computer software. Valid license from HP required for possession, use or copying.  Consistent with FAR 12.211 and 12.212, Commercial Computer Software, Computer Software Documentation, and Technical Data for Commercial Items are licensed to the U.S. Government under vendor's standard commercial license.

   THE LICENSED SOFTWARE IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY DESCRIPTION.  HP SPECIFICALLY DISCLAIMS ANY IMPLIED WARRANTY OF MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  YOU ASSUME THE ENTIRE RISK RELATING TO THE USE OR PERFORMANCE OF THE LICENSED SOFTWARE.

   HP Company Confidential
   © Copyright 2015-2017 HP Development Company, L.P.
   Made in U.S.A.
 */

//*****************************************************************************
// File : hp_smart_card_i2c.c
//-----------------------------------------------------------------------------
// Description: I2C driver for Raspberry Pi board.
//
//*****************************************************************************

#include <stdio.h>
#include <stdlib.h>

#include "hp_smart_card_config.h"
#include "internal_ifc/hp_smart_card_i2c_ifc.h"
#include "internal_ifc/hp_smart_card_gpio_ifc.h"
#include "internal_ifc/sc_i2c_driver.h"
#include "internal_ifc/sc_gpio_adapter.h"

#include "hp_smart_card.h"
#include "hp_debug_log_internal.h"
#include "common_log.h"

// Removed by H.M.Wang 2019-10-17
// #include "bcm2835.h"
// #define MAX_DATA_SIZE    255
// Removed by H.M.Wang 2019-10-17 end

int I2CGroupId = 1;

/***********************************************
* Implementation of APIs
***********************************************/
void HP_SMART_CARD_i2c_init(void)
{
    // call the shared Init code
    HP_SMART_CARD_gpio_init();
    return;
}

/* Removed by H.M.Wang 2019-10-17
uint8_t DeviceIDtoAddr(HP_SMART_CARD_device_id_t device_id)
{
    if (device_id == HP_SMART_CARD_DEVICE_HOST)
    {
        return(0xF0 >> 1);
    }
    else if (device_id == HP_SMART_CARD_DEVICE_ID_0)
    {
        HP_SMART_CARD_gpio_set_value(HP_SMART_CARD_SUPPLY_SELECT, HP_SMART_CARD_FALSE);     // select Pen
        return(0x42 >> 1);
    }
#ifdef INCLUDE_HP_SMART_CARD_SUPPLY
    else if (device_id == HP_SMART_CARD_DEVICE_ID_1)
    {
        HP_SMART_CARD_gpio_set_value(HP_SMART_CARD_SUPPLY_SELECT, HP_SMART_CARD_TRUE);      // select Supply
        return(0x42 >> 1);
    }
#endif
    return 0;
}
*/

/* Added by H.M.Wang 2019-10-17 */
static uint8_t DeviceIDtoAddr(HP_SMART_CARD_device_id_t device_id) {
    if (device_id == HP_SMART_CARD_DEVICE_HOST) {
        return (0xF0 >> 1);
    } else if (device_id == HP_SMART_CARD_DEVICE_PEN1) {
        SC_GPIO_ADAPTER_select_device(GPIO_DEVICE_PEN1);               // Select Pen1
        return (0x42 >> 1);
    } else if (device_id == HP_SMART_CARD_DEVICE_PEN2) {
        SC_GPIO_ADAPTER_select_device(GPIO_DEVICE_PEN2);               // Select Pen2
        return (0x42 >> 1);
    } else if (device_id == HP_SMART_CARD_DEVICE_BULK1) {
        SC_GPIO_ADAPTER_select_device(GPIO_DEVICE_BULK1);              // Select Bulk
        return (0x42 >> 1);
    }
    return 0;
}
/* Added by H.M.Wang 2019-10-17 end */

/* Removed by H.M.Wang 2019-10-17
HP_SMART_CARD_i2c_result_t HP_SMART_CARD_i2c_read(HP_SMART_CARD_device_id_t device_id,
                                                  uint8_t addr,
                                                  uint8_t                   *data,
                                                  size_t num_bytes_to_read)
{
    uint8_t reason;

    // set device address
    bcm2835_i2c_setSlaveAddress(DeviceIDtoAddr(device_id));
    // read data from address
    reason = bcm2835_i2c_read_register_rs((char *) (&addr), (char *) (data), (uint32_t) num_bytes_to_read);

    if (reason != BCM2835_I2C_REASON_OK)
    {
        //HP_DEBUG_printf("Pi",
        //	HP_DBG_LEVEL_ERROR, 0,
        //	"I2C READ ERROR from device %d, reason %d\n", (int)device_id, (int)reason);
        return HP_SMART_CARD_I2C_FAILED;
    }
    //HP_DEBUG_printf("Pi",
    //	HP_DBG_LEVEL_CUSTOMER, 5,
    //	"I2C READ %d bytes from device %d, address %d\n", (int)num_bytes_to_read, (int)device_id, (int)addr);
    return HP_SMART_CARD_I2C_SUCCESS;
}
*/

/* Added by H.M.Wang 2019-10-17 */
HP_SMART_CARD_i2c_result_t HP_SMART_CARD_i2c_read(HP_SMART_CARD_device_id_t device_id,
                                                  uint8_t addr,
                                                  uint8_t *data,
                                                  size_t num_bytes_to_read) {
//    if(LIB_HP_SMART_CARD_device_present(device_id) != HP_SMART_CARD_OK) {
//        return HP_SMART_CARD_I2C_FAILED;
//    }
    HP_SMART_CARD_i2c_result_t ret = HP_SMART_CARD_i2c_read_direct(DeviceIDtoAddr(device_id), addr, data, num_bytes_to_read);
//    SC_GPIO_ADAPTER_select_38_xlater(I2C_BUS_HANGUP);
// H.M.Wang 2024-10-19 读完Level后，将I2C切换到墨袋上
    SC_GPIO_ADAPTER_select_device(GPIO_DEVICE_BULK1);
// End of H.M.Wang 2024-10-19 读完Level后，将I2C切换到墨袋上
    return ret;
}

static HP_SMART_CARD_i2c_result_t HP_SMART_CARD_i2c_read_direct(uint8_t i2c_addr,
                                                  uint8_t addr,
                                                  uint8_t *data,
                                                  size_t num_bytes_to_read) {

    int read_length;

    if(data == NULL) {
        LOGE(">>> HP_SMART_CARD_i2c_read_direct: data buffer null!");
        return HP_SMART_CARD_I2C_FAILED;
    }

//    if(sizeof(data) < num_bytes_to_read) {
//        LOGE(">>> HP_SMART_CARD_i2c_read_direct: data buffer sufficient space[%d < %d]!", sizeof(data), num_bytes_to_read);
//        return HP_SMART_CARD_I2C_FAILED;
//    }

    read_length = SC_I2C_DRIVER_read(I2CGroupId, i2c_addr, addr, data, num_bytes_to_read);

    if(read_length < 0) {
        return HP_SMART_CARD_I2C_FAILED;
    }

    return HP_SMART_CARD_I2C_SUCCESS;
}
/* Added by H.M.Wang 2019-10-17 end */

/* Removed by H.M.Wang 2019-10-17
HP_SMART_CARD_i2c_result_t HP_SMART_CARD_i2c_write(HP_SMART_CARD_device_id_t device_id,
                                                   uint8_t addr,
                                                   uint8_t                   *data,
                                                   size_t num_bytes_to_write)
{
    int     i;
    uint8_t data_buff[MAX_DATA_SIZE + 1];
    uint8_t reason;

    // set device address
    bcm2835_i2c_setSlaveAddress(DeviceIDtoAddr(device_id));
    // prepend the address before the data
    data_buff[0] = addr;
    if (num_bytes_to_write > MAX_DATA_SIZE)
        return HP_SMART_CARD_I2C_FAILED;
    for (i = 0; i < num_bytes_to_write; i++)
        data_buff[i + 1] = data[i];
    // write address and data
    reason = bcm2835_i2c_write((const char *) (data_buff), (uint32_t) (num_bytes_to_write + 1));

    if (reason != BCM2835_I2C_REASON_OK)
    {
        //HP_DEBUG_printf("Pi",
        //	HP_DBG_LEVEL_ERROR, 0,
        //	"I2C WRITE ERROR from device %d, reason %d\n", (int)device_id, (int)reason);
        return HP_SMART_CARD_I2C_FAILED;
    }
    //HP_DEBUG_printf("Pi",
    //	HP_DBG_LEVEL_CUSTOMER, 5,
    //	"I2C WRITE %d bytes to device %d, address %d\n", (int)num_bytes_to_write, (int)device_id, (int)addr);
    return HP_SMART_CARD_I2C_SUCCESS;
}
*/

/* Added by H.M.Wang 2019-10-17 */
HP_SMART_CARD_i2c_result_t HP_SMART_CARD_i2c_write(HP_SMART_CARD_device_id_t device_id,
                                                   uint8_t addr,
                                                   uint8_t *data,
                                                   size_t num_bytes_to_write) {
//    if(LIB_HP_SMART_CARD_device_present(device_id) != HP_SMART_CARD_OK) {
//        return HP_SMART_CARD_I2C_FAILED;
//    }
    HP_SMART_CARD_i2c_result_t ret = HP_SMART_CARD_i2c_write_direct(DeviceIDtoAddr(device_id), addr, data, num_bytes_to_write);
//    SC_GPIO_ADAPTER_select_38_xlater(I2C_BUS_HANGUP);
// H.M.Wang 2024-10-19 读完Level后，将I2C切换到墨袋上
    SC_GPIO_ADAPTER_select_device(GPIO_DEVICE_BULK1);
// End of H.M.Wang 2024-10-19 读完Level后，将I2C切换到墨袋上
    return ret;
}

static HP_SMART_CARD_i2c_result_t HP_SMART_CARD_i2c_write_direct(uint8_t i2c_addr,
                                                   uint8_t addr,
                                                   uint8_t *data,
                                                   size_t num_bytes_to_write) {
    int ret;

    if(data == NULL) {
        return HP_SMART_CARD_I2C_FAILED;
    }

    ret = SC_I2C_DRIVER_write(I2CGroupId, i2c_addr, addr, data, num_bytes_to_write);

    if(ret < 0) {
        return HP_SMART_CARD_I2C_FAILED;
    }

    return HP_SMART_CARD_I2C_SUCCESS;
}
/* Added by H.M.Wang 2019-10-17 end */
