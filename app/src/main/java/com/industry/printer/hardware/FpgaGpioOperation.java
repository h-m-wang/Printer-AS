package com.industry.printer.hardware;

import android.content.Context;
import android.os.RecoverySystem;

import com.industry.printer.BinInfo;
import com.industry.printer.DataTransferThread;
import com.industry.printer.FileFormat.SystemConfigFile;
import com.industry.printer.PHeader.PrinterNozzle;
import com.industry.printer.Utils.ConfigPath;
import com.industry.printer.Utils.Configs;
import com.industry.printer.Utils.CypherUtils;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.PlatformInfo;
import com.industry.printer.Utils.StringUtil;
import com.industry.printer.data.DataTask;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;

/**
 * @author kevin
 *         用于操作Fpga Gpio的类
 */
public class FpgaGpioOperation {


    /**
     * IOCMD
     */
    public static final int FPGA_CMD_SETTING = 0x01;
    public static final int FPGA_CMD_SENDDATA = 0x02;
    public static final int FPGA_CMD_SYNCDATA = 0x03;
    public static final int FPGA_CMD_STARTPRINT = 0x04;
    public static final int FPGA_CMD_STOPPRINT = 0x05;
    public static final int FPGA_CMD_CLEAN = 0x06;
// H.M.Wang 2020-12-25 追加两个命令
    public static final int FPGA_CMD_DATAGENRE = 0x07;
    public static final int FPGA_CMD_BUCKETSIZE = 0x08;
    public static final int FPGA_CMD_DISPLOG = 0x09;
    public static final int FPGA_CMD_SOFTPHO = 0x0A;
// H.M.Wang 2021-4-9 追加ioctl的分辨率信息获取命令
    public static final int FPGA_CMD_GET_DPI_VER = 0x0B;
// End of H.M.Wang 2021-4-9 追加ioctl的分辨率信息获取命令
// H.M.Wang 2022-3-21 修改为设置是否反向生成打印缓冲区
// H.M.Wang 2021-9-24 追加输入设置参数
//    public static final int FPGA_CMD_INPUT_PROC = 0x0C;
    public static final int FPGA_CMD_MIRROR = 0x0C;
// End of H.M.Wang 2021-9-24 追加输入设置参数
// End of H.M.Wang 2022-3-21 修改为设置是否反向生成打印缓冲区
/* H.M.Wang 2022-6-10 取消连续打印模式的修改
// H.M.Wang 2022-6-6 追加连续打印模式
    public static final int FPGA_CMD_PERSIST_PRINT = 0x0D;
// End of H.M.Wang 2022-6-6 追加连续打印模式
*/
// H.M.Wang 2022-12-21 追加一个从FPGA驱动中获取FPGA版本号的调用
    public static final int FPGA_CMD_GETVERSION = 0x0D;
// End of H.M.Wang 2022-12-21 追加一个从FPGA驱动中获取FPGA版本号的调用

// H.M.Wang 2023-3-13 追加一个清除PCFIFO的网络命令
    public static final int FPGA_CMD_CLEAR_FIFO = 0x0E;
// End of H.M.Wang 2023-3-13 追加一个清除PCFIFO的网络命令

// H.M.Wang 2024-1-3 增加一个获取驱动版本号的功能
    public static final int FPGA_CMD_DRVVERSION = 0x0F;
// End of H.M.Wang 2024-1-3 增加一个获取驱动版本号的功能

// H.M.Wang 2024-3-25 添加一个读取22mm寄存器值的功能
    public static final int FPGA_CMD_READ_HP22MM_REG_VALUE = 0x10;
// End of H.M.Wang 2024-3-25 添加一个读取22mm寄存器值的功能
// H.M.Wang 2024-4-19 增加一个写入大块数据的测试项目
    public static final int FPGA_CMD_HP22MM_WRITE_BULK_TEST = 0x11;
// End of H.M.Wang 2024-4-19 增加一个写入大块数据的测试项目
// H.M.Wang 2024-5-2 追加一个FPGA升级的进度查询命令
    public static final int FPGA_CMD_UPGRADING_PROGRESS = 0x12;
// End of H.M.Wang 2024-5-2 追加一个FPGA升级的进度查询命令
// H.M.Wang 2024-6-20 追加一个22mm通过SPI进行24M速率的写试验
    public static final int FPGA_CMD_HP22MM_HI_SPEED_WTEST = 0x13;
// End of H.M.Wang 2024-6-20 追加一个22mm通过SPI进行24M速率的写试验
// H.M.Wang 2024-9-21 追加一个获取FPGA驱动状态的命令
    public static final int FPGA_CMD_GET_DRIVER_STATE = 0x14;
// End of H.M.Wang 2024-9-21 追加一个获取FPGA驱动状态的命令
// H.M.Wang 2024-10-14 追加一个PHO-ENC Test的开始命令和读取测试结果的命令
    public static final int FPGA_CMD_EXEC_PHOENC_TEST = 0x15;
    public static final int FPGA_CMD_READ_PHOENC_TEST = 0x16;
// End of H.M.Wang 2024-10-14 追加一个PHO-ENC Test的开始命令和读取测试结果的命令
// H.M.Wang 2021-4-9 追加ioctl的分辨率信息获取命令
    public static final int DPI_VERSION_NONE  = 0;
    public static final int DPI_VERSION_150   = 1;
    public static final int DPI_VERSION_300   = 2;
// End of H.M.Wang 2021-4-9 追加ioctl的分辨率信息获取命令

    public static final int DATA_GENRE_UPDATE   = 0;
    public static final int DATA_GENRE_NEW      = 1;
    public static final int DATA_GENRE_IGNORE   = 0;
// End of H.M.Wang 2020-12-25 追加两个命令

    /**
     * 0x00 输出数据状态
     * 0x01 设置状态
     * 0x02 保留
     * 0x03 清空状态
     */
    public static final int FPGA_STATE_OUTPUT = 0x00;
    public static final int FPGA_STATE_SETTING = 0x01;
    public static final int FPGA_STATE_RESERVED = 0x02;
    public static final int FPGA_STATE_CLEAN = 0x03;
    public static final int FPGA_STATE_PURGE = 0x05;
// H.M.Wang 2023-7-8 追加写FPGA的Flash的功能
    public static final int FPGA_STATE_UPDATE_FLASH = 0x06;
// End of H.M.Wang 2023-7-8 追加写FPGA的Flash的功能
// H.M.Wang 2024-3-24 追加一个从apk的测试页面启动22mm打印测试的功能
    public static final int FPGA_STATE_HP22MM_TEST_PRINT = 0x07;
// End of H.M.Wang 2024-3-24 追加一个从apk的测试页面启动22mm打印测试的功能

    public static final String FPGA_DRIVER_FILE = "/dev/fpga-gpio";
    public static int mFd = 0;
    /**
     * GPIO JNI APIs
     **/
    /**
     * 打开GPIO设备文件
     *
     * @param dev GPIO驱动设备文件
     * @return
     */
    static public native int open(String dev);

    /**
     * 向GPIO写入数据
     *
     * @param fd     设备句柄
     * @param buffer 要写到GPIO的数据buffer
     * @param count  写入数据长度，单位 sizeof（char）
     * @return
     */
    static public native int write(int fd, char[] buffer, int count);

    /**
     * 讀取FPGA數據
     *
     * @param fd
     * @return
     */
    static public native int read(int fd);

    /**
     * 向GPIO写入数据
     *
     * @param fd 设备句柄
     * @return
     */
    static public native int ioctl(int fd, int cmd, long arg);

    /**
     * 查询GPIO是否可写
     *
     * @param fd 设备句柄
     * @return
     */
    static public native int poll(int fd);

    /**
     * 关闭GPIO驱动设备文件
     *
     * @param fd 设备句柄
     * @return
     */
    static public native int close(int fd);


    //TAG
    public final static String TAG = FpgaGpioOperation.class.getSimpleName();


    public static volatile FpgaGpioOperation mInstance;

    public static FpgaGpioOperation getInstance() {
        if (mInstance == null) {
            synchronized (FpgaGpioOperation.class) {
                if (mInstance == null) {
                    mInstance = new FpgaGpioOperation();
                }
            }
        }
        return mInstance;
    }

    public FpgaGpioOperation() {

    }

    public static int open() {
        if (mFd <= 0) {
            mFd = open(FPGA_DRIVER_FILE);
            Debug.e("FpgaGpioOperation", FPGA_DRIVER_FILE + " = " + mFd);
        }
        return mFd;
    }

    public static void close() {
        if (mFd > 0) {
            close(mFd);
        }
    }

    public int read() {
        open();
        return read(mFd);
    }

    /**
     * writeData 下发打印数据接口
     * 每次在启动打印的时候设置为输出，在打印过程中不允许修改PG0 PG1状态
     *
     * @param type 数据类型，设置or打印数据
     * @param data
     * @param len
     * @return
     */
// H.M.Wang 2020-12-25 追加两个命令
//    public static synchronized int writeData(int type, char data[], int len) {
    public static synchronized int writeData(int dataGenre, int type, char data[], int len) {
// End of H.M.Wang 2020-12-25 追加两个命令
        int fd = open();
        if (fd <= 0) {
            return -1;
        }
        if (type < FPGA_STATE_OUTPUT || type > FPGA_STATE_PURGE) {
            Debug.d(TAG, "===>wrong data type");
            return -1;
        }
// H.M.Wang 2020-12-25 追加两个命令
        ioctl(fd, FPGA_CMD_DATAGENRE, dataGenre);   // 0:update; 1:new data
        Debug.d(TAG, "FPGA_CMD_DATAGENRE -> GENRE = " + dataGenre);
// End of H.M.Wang 2020-12-25 追加两个命令
        ioctl(fd, FPGA_CMD_SETTING, type);
        Debug.d(TAG, "FPGA_CMD_SETTING -> TYPE = " + type);
// H.M.Wang 2022-3-19 当type为FPGA_STATE_PURGE的时候，设置ExtGpio的FpgaState为Output，ioctl(fd, FPGA_CMD_SETTING, type)用来控制apk发送数据的用途，ExtGpio.setFpgaState用来切换FPGA的工作状态
        if(type == FPGA_STATE_PURGE) {
            ExtGpio.setFpgaState(ExtGpio.FPGA_STATE_OUTPUT);
        }
// End of H.M.Wang 2022-3-19 当type为FPGA_STATE_PURGE的时候，设置ExtGpio的FpgaState为Output，ioctl(fd, FPGA_CMD_SETTING, type)用来控制apk发送数据的用途，ExtGpio.setFpgaState用来切换FPGA的工作状态

        Debug.d(TAG, "--->writeData len=" + len);
        int wlen = write(fd, data, len);
        if (wlen != len) {
            //close(fd);
            return -1;
        }

// H.M.Wang 2023-1-5 取消开始打印命令下发后立即将GPIO切换到 FPGA_STATE_OUTPUT(00)，因为这会导致PH14立即发生，此时有可能数据还没有准备好，改为开始打印后第一次下发数据后切换
        if(type == FPGA_STATE_OUTPUT && mJustStartedPrint) {
            ExtGpio.setFpgaState(ExtGpio.FPGA_STATE_OUTPUT);
            mJustStartedPrint = false;
        }
// End of H.M.Wang 2023-1-5 取消开始打印命令下发后立即将GPIO切换到 FPGA_STATE_OUTPUT(00)，因为这会导致PH14立即发生，此时有可能数据还没有准备好，改为开始打印后第一次下发数据后切换

        // close(fd);
        return wlen;
    }

    public static int getPrintedCount() {
        int ret = 0;
        int fd = open();
        if (fd <= 0) {
            return -1;
        }

        ret = read(fd);
        return ret;
    }

    /**
     * pollState 轮训内核buffer状态
     * 由于该函数会调用native的poll函数，native的poll函数会一直阻塞直到内核kernel Buffer状态为空，
     * 所以不能在UI线程内调用该函数，请在单独的Thread中调用，防止ANR
     *
     * @return
     */
    public static int pollState() {
        int ret = -1;
        int fd = open();
        if (fd <= 0) {
            return -1;
        }

        ret = poll(fd);
        return ret;
    }

// H.M.Wang 2022-5-31 向FPGA的PG1和PG2下发11，3ms后再下发00
    public static void clear() {
        int fd = open();
        if (fd <= 0) {
            Debug.d(TAG, "===>open fpga file error");
            return;
        }
        ExtGpio.setFpgaState(ExtGpio.FPGA_STATE_CLEAN);
        Debug.d(TAG, "FPGA_STATE_CLEAN");

        try{Thread.sleep(3);}catch(Exception e){};

        ExtGpio.setFpgaState(ExtGpio.FPGA_STATE_OUTPUT);
        Debug.d(TAG, "FPGA_STATE_OUTPUT");
    }
// End of H.M.Wang 2022-5-31 向FPGA的PG1和PG2下发11，3ms后再下发00

    /**
     * clean 下发清空数据命令到FPGA
     */
    public static void clean() {
        int fd = open();
        if (fd <= 0) {
            Debug.d(TAG, "===>open fpga file error");
            return;
        }
        ioctl(fd, FPGA_CMD_CLEAN, 0);
// H.M.Wang 2021-12-14 将FPGA的状态设置转移到EXT-GPIO驱动里面，目的是避免这两个驱动（FPGA驱动和EXT-GPIO驱动）都操作PG管脚组，并且无法互斥，而产生互相干扰
        ExtGpio.setFpgaState(ExtGpio.FPGA_STATE_CLEAN);
// End of H.M.Wang 2021-12-14 将FPGA的状态设置转移到EXT-GPIO驱动里面，目的是避免这两个驱动（FPGA驱动和EXT-GPIO驱动）都操作PG管脚组，并且无法互斥，而产生互相干扰
        Debug.d(TAG, "FPGA_CMD_CLEAN");
        // close(fd);
    }

    public static final int SETTING_TYPE_NORMAL = 1;
    public static final int SETTING_TYPE_PURGE1 = 2;
    public static final int SETTING_TYPE_PURGE2 = 3;

    /**
     * updateSettings 下发系统设置
     * 如果要下发设置数据，必须先停止打印
     * FPGA驅動接收32個參數，其中前24個參數是下發給FPGA設備的，後8個給驅動備用
     * 參數24： 表示列高（經過補償後的字節數），用於加重處理
     *
     * @param context
     */

    public static void updateSettings(Context context, DataTask task, int type) {

/*
		if (DataTransferThread.getInstance().isRunning()) {
			Debug.d(TAG, "===>print Thread is running now, please stop it then update settings");
			return;
		}
*/
        SystemConfigFile config = SystemConfigFile.getInstance(context);

        int fd = open();
        if (fd <= 0) {
            return;
        }
        char data[] = new char[Configs.gParams];
// H.M.Wang 2021-12-31 在大字机的时候，将分辨率参数强制设为150，（其实我认为300dpi的img应该设为300，150dpi的img应该设为150）
        if (config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_16_DOT ||
            config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_32_DOT ||
// H.M.Wang 2022-5-27 追加32x2头类型
            config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_32X2 ||
// End of H.M.Wang 2022-5-27 追加32x2头类型
            config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_64_DOT ||
            config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_32DN ||
            config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_32SN ||
            config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_64SN ||
// H.M.Wang 2022-10-19 追加64SLANT头。
            config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_64SLANT ||
// H.M.Wang 2024-4-29 追加64_DOT_ONE喷头类型
            config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_64DOTONE ||
// End of H.M.Wang 2024-4-29 追加64_DOT_ONE喷头类型
// H.M.Wang 2024-9-10 增加一个16DOTX4头类型，
            config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_16DOTX4 ||
// End of H.M.Wang 2024-9-10 增加一个16DOTX4头类型，
// H.M.Wang 2023-7-29 追加48点头
            config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_48_DOT ||
// End of H.M.Wang 2023-7-29 追加48点头
// End of H.M.Wang 2022-10-19 追加64SLANT头。
            config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_96DN) {
            config.setParam(2, 150);
        }
// End of H.M.Wang 2021-12-31 在大字机的时候，将分辨率参数强制设为150，（其实我认为300dpi的img应该设为300，150dpi的img应该设为150）

//		config.paramTrans();
        //		RFIDManager manager = RFIDManager.getInstance(context);
//		RFIDDevice device = manager.getDevice(0);

        IInkDevice device = InkManagerFactory.inkManager(context);
        Paramter paramter = Paramter.getInstance();
        int feature4 = 0;
        int feature5 = 0;
        if (device != null) {
            feature4 = device.getFeature(0, 4);
            feature5 = device.getFeature(0, 5);
        }
        paramter.paramTrans(config.mParam, feature4, feature5, config.getPNozzle().mHeads);
        for (int i = 0; i < 24; i++) {
            data[i] = (char) paramter.getFPGAParam(i);
        }
        // S10 lower 4 bits represent print-header type
        int index = (char) config.getParam(SystemConfigFile.INDEX_HEAD_TYPE);
        data[9] = (char) PrinterNozzle.getInstance(index).mType;

        if (type != SETTING_TYPE_NORMAL) {
            data[1] = 4;
            data[3] = 100 * 4;
            data[4] = 600;
// H.M.Wang 2021-10-22 修改清洗，重复打印设置改为2000ms，这样防止在清洗完成后还连续产生PH14
//            data[5] = 100 * 4;
            data[5] = 500 * 4;
// End of H.M.Wang 2021-10-22 修改清洗，重复打印设置改为2000ms，这样防止在清洗完成后还连续产生PH14
// H.M.Wang 2021-4-1 当清洗时，将bold设为头数，以避免清洗变淡
            data[15] = (char) (config.getPNozzle().mHeads);
            if (config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_R6X48 ||
                config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_R6X50 ||
// H.M.Wang 2021-8-25 追加E5X48和E5X50头类型
                config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E5X48 ||
                config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E5X50 ||
// End of H.M.Wang 2021-8-25 追加E5X48和E5X50头类型
                config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E6X48 ||
                config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E6X50) {
// H.M.Wang 2021-10-20 E5,E6头的清洗加重值，从16改为128。2021-10-22 回复为16
                data[15] = 16;
// End of H.M.Wang 2021-10-20 E5,E6头的清洗加重值，从16改为128。2021-10-22 回复为16
            }
            if (config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E6X1) {
// H.M.Wang 2021-10-20 E5,E6头的清洗加重值，从16改为128。2021-10-22 回复为16
                data[15] = 16;
// End of H.M.Wang 2021-10-20 E5,E6头的清洗加重值，从16改为128。2021-10-22 回复为16
            }
            if (config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_16_DOT ||
                config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_32_DOT ||
// H.M.Wang 2022-5-27 追加32x2头类型
                config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_32X2 ||
// End of H.M.Wang 2022-5-27 追加32x2头类型
                config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_64_DOT ||
                config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_32DN ||
                config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_32SN ||
// H.M.Wang 2022-10-19 追加64SLANT头。
                config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_64SLANT ||
// End of H.M.Wang 2022-10-19 追加64SLANT头。
// H.M.Wang 2024-4-29 追加64_DOT_ONE喷头类型
                config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_64DOTONE ||
// End of H.M.Wang 2024-4-29 追加64_DOT_ONE喷头类型
// H.M.Wang 2024-9-10 增加一个16DOTX4头类型，
                config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_16DOTX4 ||
// End of H.M.Wang 2024-9-10 增加一个16DOTX4头类型，
                config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_64SN ||
// H.M.Wang 2023-7-29 追加48点头
                config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_48_DOT ||
// End of H.M.Wang 2023-7-29 追加48点头
// H.M.Wang 2021-8-16 追加96DN头
                config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_96DN) {
// End of H.M.Wang 2021-8-16 追加96DN头
// H.M.Wang 2021-12-31 将data[15]原来强制设为8改为2（其实2是对应于300dpi的，150dpi应该是1
                data[15] = 2;
// End of H.M.Wang 2021-12-31 将data[15]原来强制设为8改为2（其实2是对应于300dpi的，150dpi应该是1
// H.M.Wang 2025-2-21 修改清洗时的S5=2400
                data[4] = 2400;
// End of H.M.Wang 2025-2-21 修改清洗时的S5=2400
            }
// End of H.M.Wang 2021-4-1 当清洗时，将bold设为头数，以避免清洗变淡
// H.M.Wang 2021-4-22 如果打印头的类型是打字机，则取消加重的设置。如果img为300dpi的话，强制设置为300dpi，如果img为150dpi的话，设置为150dpi
        } else {
            if (config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_16_DOT ||
                config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_32_DOT ||
// H.M.Wang 2022-5-27 追加32x2头类型
                config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_32X2 ||
// End of H.M.Wang 2022-5-27 追加32x2头类型
                config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_64_DOT ||
                config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_32DN ||
                config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_32SN ||
// H.M.Wang 2022-10-19 追加64SLANT头。
                config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_64SLANT ||
// End of H.M.Wang 2022-10-19 追加64SLANT头。
// H.M.Wang 2024-4-29 追加64_DOT_ONE喷头类型
                config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_64DOTONE ||
// End of H.M.Wang 2024-4-29 追加64_DOT_ONE喷头类型
// H.M.Wang 2024-9-10 增加一个16DOTX4头类型，
                config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_16DOTX4 ||
// End of H.M.Wang 2024-9-10 增加一个16DOTX4头类型，
                config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_64SN ||
// H.M.Wang 2023-7-29 追加48点头
                config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_48_DOT ||
// End of H.M.Wang 2023-7-29 追加48点头
// H.M.Wang 2021-8-16 追加96DN头
                config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_96DN) {
// End of H.M.Wang 2021-8-16 追加96DN头
                data[15] = (char) (Configs.GetDpiVersion() == DPI_VERSION_300 ? 2 : 1);
// End of H.M.Wang 2021-4-22 如果打印头的类型是打字机，则取消加重的设置。如果img为300dpi的话，强制设置为300dpi，如果img为150dpi的话，设置为150dpi
            }
// H.M.Wang 2021-5-22 25.4x(1-4)头，打印的时候，S18[4]强制设为0
// H.M.Wang 2021-5-20 25.4x(1-4)头，打印的时候，S18[4]强制设为1
            if (config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_1_INCH ||
// H.M.Wang 2022-4-29 追加25.4x10头类型
                config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_254X10 ||
// End of H.M.Wang 2022-4-29 追加25.4x10头类型
                config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_1_INCH_DUAL ||
                config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_1_INCH_TRIPLE ||
                config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_1_INCH_FOUR) {
//                data[17] |= 0x0010;
                data[17] &= 0xFF0F;
            }
// End of H.M.Wang 2021-5-20 25.4x(1-4)头，打印的时候，S18[4]强制设为1
// End of H.M.Wang 2021-5-22 25.4x(1-4)头，打印的时候，S18[4]强制设为0
// H.M.Wang 2021-5-22 打印的时候，E6不能允许选反表， 即S18[3:0]取0
            if (config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E6X48 ||
                config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E6X50) {
                data[17] &= 0xFFF0;
            }
// End of H.M.Wang 2021-5-22 打印的时候，E6不能允许选反表， 即S18[3:0]取0
            if (config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E6X1) {
                data[17] &= 0xFFF0;
            }
// H.M.Wang 2021-8-25 追加E5X48和E5X50头类型
            if (config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E5X48 ||
                config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E5X50) {
                data[17] &= 0xFFF0;
            }
// End of H.M.Wang 2021-8-25 追加E5X48和E5X50头类型
// H.M.Wang 2021-11-18 追加根据双列打印对参数的修改
            if (config.getParam(SystemConfigFile.INDEX_DUAL_COLUMNS) > 0) { // >0 代表启用双列打印
                if (config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_12_7) {
                    data[9] = (char) PrinterNozzle.NozzleType.NOZZLE_TYPE_1_INCH;
                } else if (config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_25_4) {
                    data[9] = (char) PrinterNozzle.NozzleType.NOZZLE_TYPE_1_INCH_DUAL;
                } else if (config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_38_1) {
                    data[9] = (char) PrinterNozzle.NozzleType.NOZZLE_TYPE_1_INCH_TRIPLE;
                } else if (config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_50_8) {
                    data[9] = (char) PrinterNozzle.NozzleType.NOZZLE_TYPE_1_INCH_FOUR;
                }
//                data[17] &= 0xFFF0;
                data[17] |= 0x0010;

//                Debug.d(TAG, "data[17] = " + Integer.toHexString(data[17]));
            }
// End of H.M.Wang 2021-11-18 追加根据双列打印对参数的修改
// H.M.Wang 2022-12-5 25.4 的喷头， 不管双列偏移设了什么， S18[4]=0
            if (config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_1_INCH ||
                config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_1_INCH_DUAL ||
                config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_1_INCH_TRIPLE ||
                config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_1_INCH_FOUR ) {
                data[17] &= 0xFFEF;
            }
// End of H.M.Wang 2022-12-5 25.4 的喷头， 不管双列偏移设了什么， S18[4]=0
// H.M.Wang 2023-8-8 增加一个新的网络命令，SelectPen
            data[11] = (char)((data[11] & 0xF000) | (0x0FFF & DataTransferThread.SelectPen));
// End of H.M.Wang 2023-8-8 增加一个新的网络命令，SelectPen
        }

        if (type == SETTING_TYPE_PURGE1) {
// H.M.Wang 2021-12-29 修改S5，S15，S21，S22，S23为下列固定值
///////            data[4] = (char) (data[4] * 2);
// H.M.Wang 2023-12-5 4FIFO版本清洗的时候S15，S21，S22，S23均设为0
            if(PlatformInfo.getImgUniqueCode().startsWith("4FIFO")) {
                data[20] = 0;
                data[21] = 0;
                data[22] = 0;
                data[14] = 0;
            } else {
// End of H.M.Wang 2023-12-5 4FIFO版本清洗的时候S15，S21，S22，S23均设为0
                data[20] = 50;
                data[21] = 50;
                data[22] = 1000;
                data[14] = 500;
            }
// H.M.Wang 2022-3-4 data[4]设为200
//            data[4] = 100;
// H.M.Wang 2023-5-29 data[4]设为200x3
//            data[4] = 200;
// H.M.Wang 2023-5-31 data[4]在喷头为32SN和32DN的时候，不设为200x3，反倒设为200/4
// H.M.Wang 2023-8-11 32SN/DN的S5不在特殊处理，改为标准的600
//            if (config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_32DN ||
//                config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_32SN) {
//                data[4] = 50;
//            } else {
//                data[4] = 600;
//            }
// End of H.M.Wang 2023-8-11 32SN/DN的S5不在特殊处理，改为标准的600
// End of H.M.Wang 2023-5-31 data[4]在喷头为32SN和32DN的时候，不设为200x3，反倒设为200/4
// End of H.M.Wang 2023-5-29 data[4]设为200x3
// End of H.M.Wang 2022-3-4 data[4]设为200
// H.M.Wang 2023-5-29 data[5]设为1500x2
// H.M.Wang 2023-5-31 取消data[5]设为1500x2， 恢复到原来的值
            data[5] = 1500;
// End of H.M.Wang 2023-5-31 取消data[5]设为1500x2， 恢复到原来的值
// End of H.M.Wang 2023-5-29 data[5]设为1500x2
// H.M.Wang 2022-3-17 data[5]减半，追加data[7]减半
            data[5] /= 2;
            data[7] /= 2;
// End of H.M.Wang 2022-3-17 data[5]减半，追加data[7]减半
// End of H.M.Wang 2021-12-29 修改S5，S15，S21，S22，S23为下列固定值

// H.M.Wang 2021-3-30 当清洗时，头类型设为25.4x4
            data[9] = (char) PrinterNozzle.NozzleType.NOZZLE_TYPE_1_INCH_FOUR;
// End of H.M.Wang 2021-3-30 当清洗时，头类型设为25.4x4
// H.M.Wang 2021-5-20 清洗的时候，都选反表，S18[3:0]都设为1
//            data[17] = (char) (data[17] | 0x010);
            data[17] = (char) (data[17] | 0x001F);
// End of H.M.Wang 2021-5-20 清洗的时候，都选反表，S18[3:0]都设为1
// H.M.Wang 2023-8-7 增加12头清洗功能。S12[5:0]标准1-12头的清洗。S12[0]为1-2头的清洗，S12[1]为3-4头的清洗，以此类推
            if(DataTransferThread.CleanHead > 0 && DataTransferThread.CleanHead <=12) {
                char s12 = 0x0001;
                for (int i=0; i<DataTransferThread.CleanHead-1; i++) {
                    s12 <<= 1;
                }
                data[11] = s12;
            } else {
                data[11] = 0xFFFF;
            }
// End of H.M.Wang 2023-8-7 增加12头清洗功能。S12[5:0]标准1-12头的清洗。S12[0]为1-2头的清洗，S12[1]为3-4头的清洗，以此类推
        } else if (type == SETTING_TYPE_PURGE2) {
            data[4] = (char) (data[4] * 2);
            data[17] = (char) (data[17] & 0xffef);
        }
		/* else {
			data[1] = (char) SystemConfigFile.mParam2;
			data[3] = (char) SystemConfigFile.mParam4;
			data[4] = (char) SystemConfigFile.mParam5;
			data[5] = (char) SystemConfigFile.mParam6;
			data[15] = (char) SystemConfigFile.mResv16;
		}
		data[2] = (char) SystemConfigFile.mParam3;
		data[6] = (char) SystemConfigFile.mParam7;
		data[7] = (char) SystemConfigFile.mParam8;
		Debug.d(TAG, "===>data7:" + Integer.toHexString(data[7]));
		data[8] = (char) SystemConfigFile.mParam9;
		data[9] = (char) SystemConfigFile.mParam10;
		data[10] = (char) SystemConfigFile.mResv11;
		data[11] = (char) SystemConfigFile.mResv12;
		data[12] = (char) SystemConfigFile.mResv13;
		data[13] = (char) SystemConfigFile.mResv14;
		data[14] = (char) SystemConfigFile.mResv15;
		
		data[16] = (char) SystemConfigFile.mResv17;
		data[17] = (char) SystemConfigFile.mResv18;
		data[18] = (char) SystemConfigFile.mResv19;
		data[19] = (char) SystemConfigFile.mResv20;
		data[20] = (char) SystemConfigFile.mResv21;
		data[21] = (char) SystemConfigFile.mResv22;
		data[22] = (char) SystemConfigFile.mResv23;
		data[23] = (char) SystemConfigFile.mResv24;
		*/

        if (task != null) {
            BinInfo info = task.getInfo();
            data[24] = (char) info.getBytesFeed();
        }
// H.M.Wang 2020-5-7 12.7R5头的时候，设置头的数，强制设置打印头类型为12.7->3x25.4->12.7
        // S17
// H.M.Wang 2020-5-9 12.7R5d打印头类型不参与信息编辑，因此不通过信息的打印头类型判断其是否为12.7R5的信息，而是通过参数来规定现有信息的打印行为
// H.M.Wang 2020-5-21 12.7R5头改为RX48，追加RX50头
//		if(config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_12_7_R5) {
        if (config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_R6X48 ||
                config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_R6X50) {
// End of H.M.Wang 2020-5-21 12.7R5头改为RX48，追加RX50头
//		final int headIndex = config.getParam(SystemConfigFile.INDEX_HEAD_TYPE);
//		PrinterNozzle head = PrinterNozzle.getInstance(headIndex);
//		if(head == PrinterNozzle.MESSAGE_TYPE_12_7_R5) {
// End of H.M.Wang 2020-5-9 12.7R5d打印头类型不参与信息编辑，因此不通过信息的打印头类型判断其是否为12.7R5的信息，而是通过参数来规定现有信息的打印行为
            data[9] = (char) PrinterNozzle.NozzleType.NOZZLE_TYPE_12_7;
            data[16] &= 0xfc7f;        // Bit9-7
            data[16] |= 0x0280;        // 6个头
            data[24] *= 6;
        }
// End of H.M.Wang 2020-5-7 12.7R5头的时候，设置头的数

// H.M.Wang 2021-3-6 追加E6X48,E6X50头
        if (config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E6X48 ||
            config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E6X50) {
            data[9] = (char) PrinterNozzle.NozzleType.NOZZLE_TYPE_9MM;
            data[16] &= 0xfc7f;        // Bit9-7
            data[16] |= 0x0280;        // 6个头
            data[24] *= 6;
        }
// End of H.M.Wang 2021-3-6 追加E6X48,E6X50头
        if (config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E6X1) {
            data[9] = (char) PrinterNozzle.NozzleType.NOZZLE_TYPE_9MM;
            data[16] &= 0xfc7f;        // Bit9-7
            data[16] |= 0x0280;        // 6个头
            data[24] *= 6;
        }
// H.M.Wang 2021-8-25 追加E5X48和E5X50头类型
        if (config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E5X48 ||
            config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E5X50) {
            data[9] = (char) PrinterNozzle.NozzleType.NOZZLE_TYPE_9MM;
            data[16] &= 0xfc7f;        // Bit9-7
            data[16] |= 0x0280;        // 按6个头设置，下发6个头的数据，但是FPGA会会略掉最后一个头的数据
            data[24] *= 6;
        }
// End of H.M.Wang 2021-8-25 追加E5X48和E5X50头类型

/* H.M.Wang 2022-6-10 取消连续打印模式的修改
// H.M.Wang 2022-6-7 连续打印模式时设置内容的修改，2022-6-6的修改中忘记了
        if(config.getParam(SystemConfigFile.INDEX_PRINT_TIMES) == 65535) {
            data[5] = data[3];
            data[7] = data[8];
        }
// End of H.M.Wang 2022-6-7 连续打印模式时设置内容的修改，2022-6-6的修改中忘记了
*/

        //是否雙列打印
// H.M.Wang 2021-11-17 修改参数61为双列位移设项
//        data[25] = (char) config.getParam(31 - 1);
// H.M.Wang 2021-12-9 这个参数是int型的，不能只取一个字节
// H.M.Wang 2022-12-4 双列仅对12.7系列的打印头有效，1英寸系列及大字机不需要双列设置
//        data[25] = (char) config.getParam(SystemConfigFile.INDEX_DUAL_COLUMNS);
        if (config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_12_7 ||
            config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_25_4 ||
            config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_38_1 ||
            config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_50_8) {
            data[25] = (char) config.getParam(SystemConfigFile.INDEX_DUAL_COLUMNS);
        } else {
            data[25] = 0;
        }
// End of H.M.Wang 2022-12-4 双列仅对12.7系列的打印头有效，1英寸系列及大字机不需要双列设置
// End of H.M.Wang 2021-12-9 这个参数是int型的，不能只取一个字节
// End of H.M.Wang 2021-11-17 修改参数61为双列位移设项
        //雙列偏移量
// H.M.Wang 2021-11-19 增加打印方向参数传递
//        data[26] = (char) config.getParam(32 - 1);
        data[26] = (char) config.getParam(1);
// End of H.M.Wang 2021-11-19 增加打印方向参数传递

/*
        data[0] = 1;
        data[1] = 4;
        data[2] = 10;
        data[3] = 61;
        data[4] = 170;
        data[5] = 1632;
        data[6] = 1;
        data[7] = 11;
        data[8] = 11;
        data[9] = 35;
        data[10] = 0;
        data[11] = 0;
        data[12] = 0;
        data[13] = 0;
        data[14] = 0;
        data[15] = 2;
        data[16] = 656;
        data[17] = 16;
        data[18] = 112;
        data[19] = 200;
        data[20] = 0;
        data[21] = 0;
        data[22] = 0;
        data[23] = 1;
        data[24] = 20;
        data[25] = 17;
        data[26] = 0;
*/
// H.M.Wang 2024-3-13 当打印头为hp22mm的时候，使用22mm头的专用参数设置
        if(PlatformInfo.getImgUniqueCode().startsWith("22MM")) {
            data = Hp22mm.getSettings(type);
        }
// End of H.M.Wang 2024-3-13 当打印头为hp22mm的时候，使用22mm头的专用参数设置

        for (int i = 0; i < data.length; i++) {
            Debug.e(TAG, "--->mFPGAParam[" + i + "] = 0x" + Integer.toHexString(data[i]));
        }
        //时间参数放在最后3个
		/*
		Calendar c = Calendar.getInstance();
		int hour = c.get(Calendar.HOUR_OF_DAY);  
        int minute = c.get(Calendar.MINUTE);
        int second = c.get(Calendar.SECOND);
		data[Configs.gParams - 3] = (char)hour;
		data[Configs.gParams - 2] = (char)minute;
		data[Configs.gParams - 1] = (char)second;
		*/

// H.M.Wang 2021-12-14 将FPGA的状态设置转移到EXT-GPIO驱动里面，目的是避免这两个驱动（FPGA驱动和EXT-GPIO驱动）都操作PG管脚组，并且无法互斥，而产生互相干扰
        ExtGpio.setFpgaState(ExtGpio.FPGA_STATE_SETTING);
// End of H.M.Wang 2021-12-14 将FPGA的状态设置转移到EXT-GPIO驱动里面，目的是避免这两个驱动（FPGA驱动和EXT-GPIO驱动）都操作PG管脚组，并且无法互斥，而产生互相干扰
        writeData(DATA_GENRE_IGNORE, FPGA_STATE_SETTING, data, data.length * 2);
// H.M.Wang 2022-3-12 设置之后恢复CLEAN（双高）
// H.W.Wang 2022-3-17 暂时取消CLEAN设置
//        ExtGpio.setFpgaState(ExtGpio.FPGA_STATE_CLEAN);
// End of H.W.Wang 2022-3-17 暂时取消CLEAN设置
// End of H.M.Wang 2022-3-12 设置之后恢复CLEAN（双高）
// H.M.Wang 2023-7-15 这个下发， 打印中也会。打印中， 回打印，停止中， 回停止，
        DataTransferThread dt = DataTransferThread.mInstance;
        if(null != dt && dt.isRunning()) {
            ExtGpio.setFpgaState(ExtGpio.FPGA_STATE_OUTPUT);
        } else {
            ExtGpio.setFpgaState(ExtGpio.FPGA_STATE_CLEAN);
        }
// End of H.M.Wang 2023-7-15 这个下发， 打印中也会。打印中， 回打印，停止中， 回停止，
// H.M.Wang 2024-3-25 将设置img的FIFO的大小移到下发参数的地方，以避免原来放在init函数中，则init函数必须在打印的最前端执行，这可能导致打印开始后，FPGA发出中断，但是驱动还没有接收到下发数据，而空跑，4FIFO就出现了第一次不打印的问题
        Debug.d(TAG, "FPGA_CMD_BUCKETSIZE -> " + config.getParam(SystemConfigFile.INDEX_FIFO_SIZE));
        ioctl(fd, FPGA_CMD_BUCKETSIZE, config.getParam(SystemConfigFile.INDEX_FIFO_SIZE));
// End of H.M.Wang 2024-3-25 将设置img的FIFO的大小移到下发参数的地方，以避免原来放在init函数中，则init函数必须在打印的最前端执行，这可能导致打印开始后，FPGA发出中断，但是驱动还没有接收到下发数据，而空跑，4FIFO就出现了第一次不打印的问题
// H.M.Wang 2024-9-12 取消设置img的fpga-sunxi驱动中的左右镜像反转。因为这个可以通过在FPGA内部通过设置寄存器实现
//// H.M.Wang 2024-4-7 如果是22mm的打印头，则根据参数2的值，给img设定打印方向
//        if(config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_22MM) {
//            setMirror(config.getParam(1));
//        }
//// End of H.M.Wang 2024-4-7 如果是22mm的打印头，则根据参数2的值，给img设定打印方向
// End of H.M.Wang 2024-9-12 取消设置img的fpga-sunxi驱动中的左右镜像反转。因为这个可以通过在FPGA内部通过设置寄存器实现
// H.M.Wang 2024-5-27 设置参数78为新的压力
        if(config.getParam(SystemConfigFile.INDEX_PRESURE) == 0) {
            SmartCard.writeDAC5571(0);
        } else {
            SmartCard.writeDAC5571((int)((config.getParam(SystemConfigFile.INDEX_PRESURE) + 6.67f) / 66.7f / 3.3f * 255));
        }
// End of H.M.Wang 2024-5-27 设置参数78为新的压力
    }

// H.M.Wang 2023-1-5 取消开始打印命令下发后立即将GPIO切换到 FPGA_STATE_OUTPUT(00)，因为这会导致PH14立即发生，此时有可能数据还没有准备好，改为开始打印后第一次下发数据后切换
    private static boolean mJustStartedPrint = false;
// End of H.M.Wang 2023-1-5 取消开始打印命令下发后立即将GPIO切换到 FPGA_STATE_OUTPUT(00)，因为这会导致PH14立即发生，此时有可能数据还没有准备好，改为开始打印后第一次下发数据后切换

    /**
     * 启动打印时调用，用于初始化内核轮训线程
     */
    public static void init() {
        SystemConfigFile config = SystemConfigFile.getInstance();

        int fd = open();
        if (fd <= 0) {
            return;
        }
		/*设置状态为输出*/
        // ioctl(fd, FPGA_CMD_SETTING, FPGA_STATE_OUTPUT);
		/*启动内核轮训线程*/
/* H.M.Wang 2022-6-10 取消连续打印模式的修改
// H.M.Wang 2022-6-6 追加连续打印模式
        Debug.d(TAG, "FPGA_CMD_PERSIST_PRINT -> " + config.getParam(SystemConfigFile.INDEX_PRINT_TIMES));
        if(config.getParam(SystemConfigFile.INDEX_PRINT_TIMES) == 65535) {
            ioctl(fd, FPGA_CMD_PERSIST_PRINT, 1);
        } else {
            ioctl(fd, FPGA_CMD_PERSIST_PRINT, 0);
        }
// End of H.M.Wang 2022-6-6 追加连续打印模式
*/
//2024-3-25        Debug.d(TAG, "FPGA_CMD_BUCKETSIZE -> " + config.getParam(SystemConfigFile.INDEX_FIFO_SIZE));
//2024-3-25        ioctl(fd, FPGA_CMD_BUCKETSIZE, config.getParam(SystemConfigFile.INDEX_FIFO_SIZE));
// H.M.Wang 2024-10-24 增加这两条的目的是使得img当中过滤掉多余的PH14中断，否则开始打印后，会收到一个多余的PH14中断
// H.M.Wang 2025-2-12 如果是4FIFO不执行该操作
        if(!PlatformInfo.getImgUniqueCode().startsWith("4FIFO")) {
// H.M.Wang 2025-4-28 为A133平台专门设置一个讲状态从11改为10的操作，否则清理PhoEnc似乎无效，CB2没有这个问题
//            if(PlatformInfo.isA133Product()) ExtGpio.setFpgaState(ExtGpio.FPGA_STATE_SETTING);
// End of H.M.Wang 2025-4-28 为A133平台专门设置一个讲状态从11改为10的操作，否则清理PhoEnc似乎无效，CB2没有这个问题
            startPhoEncTest();
            stopPhoEncTest();
        }
// End of H.M.Wang 2025-2-12 如果是4FIFO不执行该操作
// End of H.M.Wang 2024-10-24 增加这两条的目的是使得img当中过滤掉多余的PH14中断，否则开始打印后，会收到一个多余的PH14中断
        Debug.d(TAG, "FPGA_CMD_STARTPRINT");
        ioctl(fd, FPGA_CMD_STARTPRINT, 0);
// H.M.Wang 2024-4-22 根据ko驱动的版本号，如果是3119以后的版本，则是修改到先下发数据，后开始打印的版本，否则按着先开始打印，后下发数据处理
/*
// H.M.Wang 2023-1-5 取消开始打印命令下发后立即将GPIO切换到 FPGA_STATE_OUTPUT(00)，因为这会导致PH14立即发生，此时有可能数据还没有准备好，改为开始打印后第一次下发数据后切换
//// H.M.Wang 2021-12-14 将FPGA的状态设置转移到EXT-GPIO驱动里面，目的是避免这两个驱动（FPGA驱动和EXT-GPIO驱动）都操作PG管脚组，并且无法互斥，而产生互相干扰
//        ExtGpio.setFpgaState(ExtGpio.FPGA_STATE_OUTPUT);
//// End of H.M.Wang 2021-12-14 将FPGA的状态设置转移到EXT-GPIO驱动里面，目的是避免这两个驱动（FPGA驱动和EXT-GPIO驱动）都操作PG管脚组，并且无法互斥，而产生互相干扰
        mJustStartedPrint = true;
// End of H.M.Wang 2023-1-5 取消开始打印命令下发后立即将GPIO切换到 FPGA_STATE_OUTPUT(00)，因为这会导致PH14立即发生，此时有可能数据还没有准备好，改为开始打印后第一次下发数据后切换
 */
        if(getDriverVersion() >= 3119) {
            ExtGpio.setFpgaState(ExtGpio.FPGA_STATE_OUTPUT);
        } else {
            mJustStartedPrint = true;
        }
// End of H.M.Wang 2024-4-22 根据ko驱动的版本号，如果是3119以后的版本，则是修改到先下发数据，后开始打印的版本，否则按着先开始打印，后下发数据处理
    }

    /**
     * 停止打印时调用，用于停止内核轮训线程
     */
    public static void uninit() {
        int fd = open();
        if (fd <= 0) {
            return;
        }

        Debug.d(TAG, "FPGA_CMD_STOPPRINT");
        ioctl(fd, FPGA_CMD_STOPPRINT, 0);
// H.M.Wang 2021-12-14 将FPGA的状态设置转移到EXT-GPIO驱动里面，目的是避免这两个驱动（FPGA驱动和EXT-GPIO驱动）都操作PG管脚组，并且无法互斥，而产生互相干扰
        ExtGpio.setFpgaState(ExtGpio.FPGA_STATE_CLEAN);
// End of H.M.Wang 2021-12-14 将FPGA的状态设置转移到EXT-GPIO驱动里面，目的是避免这两个驱动（FPGA驱动和EXT-GPIO驱动）都操作PG管脚组，并且无法互斥，而产生互相干扰
// H.M.Wang 2024-4-7 对于2024-3-25修改的先下发数据后开始打印的修改，重新开始打印的时候，由于不再清理数据区，因此最好在停止打印的时候清理一下，否则清洗可能会出乱码
        clearFIFO();
// End of H.M.Wang 2024-4-7 对于2024-3-25修改的先下发数据后开始打印的修改，重新开始打印的时候，由于不再清理数据区，因此最好在停止打印的时候清理一下，否则清洗可能会出乱码
    }

    public static void dispLog() {
        int fd = open();
        if (fd <= 0) {
            return;
        }

        Debug.d(TAG, "FPGA_CMD_DISPLOG");
        ioctl(fd, FPGA_CMD_DISPLOG, 0);
    }

    public static void softPho() {
        int fd = open();
        if (fd <= 0) {
            return;
        }

        Debug.d(TAG, "FPGA_CMD_SOFTPHO");
        ioctl(fd, FPGA_CMD_SOFTPHO, 0);
// H.M.Wang 2021-12-14 将FPGA的状态设置转移到EXT-GPIO驱动里面，目的是避免这两个驱动（FPGA驱动和EXT-GPIO驱动）都操作PG管脚组，并且无法互斥，而产生互相干扰
        ExtGpio.setFpgaState(ExtGpio.FPGA_STATE_SOFTPHO);
// End of H.M.Wang 2021-12-14 将FPGA的状态设置转移到EXT-GPIO驱动里面，目的是避免这两个驱动（FPGA驱动和EXT-GPIO驱动）都操作PG管脚组，并且无法互斥，而产生互相干扰
    }

// H.M.Wang 2021-4-9 追加ioctl的分辨率信息获取命令
    public static int getDpiVersion() {
        int fd = open();
        if (fd <= 0) {
            return DPI_VERSION_150;
        }

        Debug.d(TAG, "FPGA_CMD_GET_DPI_VER");
        return ioctl(fd, FPGA_CMD_GET_DPI_VER, 0);
    }
// End of H.M.Wang 2021-4-9 追加ioctl的分辨率信息获取命令

// H.M.Wang 2022-3-21 修改为设置是否反向生成打印缓冲区
// H.M.Wang 2021-9-24 追加输入设置参数
    public static int setMirror(int mirror) {
        int fd = open();
        if (fd > 0) {
            Debug.d(TAG, "FPGA_CMD_MIRROR");
            return ioctl(fd, FPGA_CMD_MIRROR, mirror);
        }
        return -1;
    }
// End of H.M.Wang 2021-9-24 追加输入设置参数
// End of H.M.Wang 2022-3-21 修改为设置是否反向生成打印缓冲区

// H.M.Wang 2022-12-21 追加一个从FPGA驱动中获取FPGA版本号的调用
    public static int getFPGAVersion() {
        int fd = open();
        if (fd > 0) {
//        Debug.d(TAG, "FPGA_CMD_GETVERSION");
            return ioctl(fd, FPGA_CMD_GETVERSION, 0);
        }
        return 0;
    }
// End of H.M.Wang 2022-12-21 追加一个从FPGA驱动中获取FPGA版本号的调用

// H.M.Wang 2024-1-3 增加一个获取驱动版本号的功能
    public static int getDriverVersion() {
        int fd = open();
        if (fd > 0) {
    //        Debug.d(TAG, "FPGA_CMD_DRVVERSION");
            return ioctl(fd, FPGA_CMD_DRVVERSION, 0);
        }
        return 0;
    }
// End of H.M.Wang 2024-1-3 增加一个获取驱动版本号的功能

// H.M.Wang 2023-3-13 追加一个清除PCFIFO的网络命令
    public static int clearFIFO() {
        int fd = open();
        if (fd > 0) {
            Debug.d(TAG, "FPGA_CMD_CLEAR_FIFO");
            return ioctl(fd, FPGA_CMD_CLEAR_FIFO, 0);
        }
        return 0;
    }
// End of H.M.Wang 2023-3-13 追加一个清除PCFIFO的网络命令
// H.M.Wang 2023-7-8 追加写FPGA的Flash的功能
    public static int updateFlash() {
        int ret = 0;
        int fd = open();
        if (fd > 0) {
            File file;
            try {
                String path = ConfigPath.getFWUpgradePath();
                if(StringUtil.isEmpty(path)) {
                    Debug.e(TAG, "Source file not exist.");
                    return -1;
                }

                File srcFWFile = new File(path.substring(0, path.lastIndexOf(".")) + ".bin");
                File srcMD5File = new File(path.substring(0, path.lastIndexOf(".")) + ".txt");

                if(!srcFWFile.exists() || !srcMD5File.exists()) {
                    Debug.e(TAG, "Source bin or md5 not exists.");
                    return -1;
                }

                BufferedReader br = new BufferedReader(new FileReader(srcMD5File));
                String srcMD5Read = br.readLine();
//            Debug.d(TAG, "SrcMD5Read: [" + srcMD5Read + "].");

                String srcMD5Cal = CypherUtils.getFileMD5(srcFWFile);
//            Debug.d(TAG, "SrcMD5Cal: [" + srcMD5Cal + "].");

                if(!srcMD5Read.equalsIgnoreCase(srcMD5Cal)) {
                    Debug.e(TAG, "Source md5 not match.");
                    return -1;
                }

                FileInputStream fis = new FileInputStream(srcFWFile);
                byte[] buffer = new byte[fis.available()];
                fis.read(buffer);
                char[] cbuf = new char[buffer.length/2];
                for(int i=0; i<cbuf.length; i++) {
                    cbuf[i] = (char)(((buffer[2*i+1] << 8) & 0xff00) + (buffer[2*i] & 0x00ff));
                }
                ioctl(fd, FPGA_CMD_SETTING, FPGA_STATE_UPDATE_FLASH);
                ret = write(fd, cbuf, cbuf.length*2);
                fis.close();
            } catch (Exception e) {
                Debug.d(TAG, ""+e.getMessage());
            }
        }
        return (ret == 0 ? -1 : 0);
    }
// End of H.M.Wang 2023-7-8 追加写FPGA的Flash的功能
// H.M.Wang 2024-3-24 追加一个从apk的测试页面启动22mm打印测试的功能
    public static int hp22mmPrintTestPage(byte[] image) {
        int ret = 0;
        int fd = open();

        if (fd > 0) {
            Debug.d(TAG, "FPGA_STATE_HP22MM_TEST_PRINT");
            ioctl(fd, FPGA_CMD_SETTING, FPGA_STATE_HP22MM_TEST_PRINT);
            char[] cbuf = new char[image.length/2];
            for(int i=0; i<cbuf.length; i++) {
                cbuf[i] = (char)(((image[2*i+1] << 8) & 0xff00) + (image[2*i] & 0x00ff));
            }
            ret = write(fd, cbuf, cbuf.length*2);
        }
        return ret;
    }
// End of H.M.Wang 2024-3-24 追加一个从apk的测试页面启动22mm打印测试的功能
// H.M.Wang 2024-3-25 添加一个读取22mm寄存器值的功能
    public static int hp22mmReadRegister(int reg) {
        int fd = open();
        if (fd > 0) {
            int regVal = ioctl(fd, FPGA_CMD_READ_HP22MM_REG_VALUE, reg);
            Debug.d(TAG, "Read Reg[" + reg + "] = " + regVal);
            return regVal;
        }
        return 0;
    }
// End of H.M.Wang 2024-3-25 添加一个读取22mm寄存器值的功能
// H.M.Wang 2024-4-19 增加一个写入大块数据的测试项目
    public static int hp22mmBulkWriteTest() {
        int fd = open();
        if (fd > 0) {
            Debug.d(TAG, "Bulk data writing test");
            return ioctl(fd, FPGA_CMD_HP22MM_WRITE_BULK_TEST, 0);
        }
        return -1;
    }
// End of H.M.Wang 2024-4-19 增加一个写入大块数据的测试项目
// H.M.Wang 2024-6-20 追加一个22mm通过SPI进行24M速率的写试验
    public static int hp22mmHiSpeedWTest() {
        int fd = open();
        if (fd > 0) {
            Debug.d(TAG, "HiSpeed write test");
            return ioctl(fd, FPGA_CMD_HP22MM_HI_SPEED_WTEST, 0);
        }
        return -1;
    }
// End of H.M.Wang 2024-6-20 追加一个22mm通过SPI进行24M速率的写试验

// H.M.Wang 2024-5-2 追加一个FPGA升级的进度查询命令
    public static int getUpgradingProgress() {
        int fd = open();
        if (fd > 0) {
            Debug.d(TAG, "Getting FPGA Upgrading Progress");
            return ioctl(fd, FPGA_CMD_UPGRADING_PROGRESS, 0);
        }
        return -1;
    }
// End of H.M.Wang 2024-5-2 追加一个FPGA升级的进度查询命令
// H.M.Wang 2024-9-21 追加一个获取FPGA驱动状态的命令
    public static int getDriverState() {
        int fd = open();
        if (fd > 0) {
//            Debug.d(TAG, "Getting FPGA Driver State");
            return ioctl(fd, FPGA_CMD_GET_DRIVER_STATE, 0);
        }
        return 0;
    }
// End of H.M.Wang 2024-9-21 追加一个获取FPGA驱动状态的命令
// H.M.Wang 2024-10-14 追加一个PHO-ENC Test的开始命令和读取测试结果的命令
    public static int startPhoEncTest() {
        int fd = open();
        if (fd > 0) {
            Debug.d(TAG, "Start PHO-ENC TEST");
            return ioctl(fd, FPGA_CMD_EXEC_PHOENC_TEST, 1);
        }
        return -1;
    }
    public static int stopPhoEncTest() {
        int fd = open();
        if (fd > 0) {
            Debug.d(TAG, "Stop PHO-ENC TEST");
            return ioctl(fd, FPGA_CMD_EXEC_PHOENC_TEST, 0);
        }
        return 0;
    }
    public static int readPhoEncTest() {
        int fd = open();
        if (fd > 0) {
            Debug.d(TAG, "Read PHO-ENC TEST");
            return ioctl(fd, FPGA_CMD_READ_PHOENC_TEST, 0);
        }
        return 0;
    }
// End of H.M.Wang 2024-10-14 追加一个PHO-ENC Test的开始命令和读取测试结果的命令
}