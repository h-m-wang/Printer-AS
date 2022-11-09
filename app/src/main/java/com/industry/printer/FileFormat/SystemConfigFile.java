package com.industry.printer.FileFormat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.industry.printer.Serial.EC_DOD_Protocol;
import com.industry.printer.data.DataTask;
import com.industry.printer.hardware.FpgaGpioOperation;
import com.industry.printer.hardware.RTCDevice;
import org.xml.sax.InputSource;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.R.integer;
import android.R.xml;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Xml;

import com.industry.printer.PHeader.PrinterNozzle;
import com.industry.printer.Utils.ConfigPath;
import com.industry.printer.Utils.Configs;
import com.industry.printer.Utils.Debug;
import com.industry.printer.object.BarcodeObject;
import com.industry.printer.object.BaseObject;
import com.industry.printer.object.DynamicText;
import com.industry.printer.ui.CustomerAdapter.SettingsListAdapter;

import static android.R.attr.key;
import static android.R.attr.value;


public class SystemConfigFile{
	private static final String TAG = SystemConfigFile.class.getSimpleName();
	
	
	public static final String PH_SETTING_ENCODER = "_10001";
	public static final String PH_SETTING_TRIGER_MODE = "_10002";
	public static final String PH_SETTING_HIGH_DELAY = "_10003";
	public static final String PH_SETTING_LOW_DELAY = "_10004";
	public static final String PH_SETTING_PHOOUTPUT_PERIOD = "_10005";
	public static final String PH_SETTING_TIMED_PERIOD = "_10006";
	public static final String PH_SETTING_TRIGER_PULSE = "_10007";
	public static final String PH_SETTING_LENFIXED_PULSE = "_10008";
	public static final String PH_SETTING_DELAY_PULSE = "_10009";
	public static final String PH_SETTING_HIGH_LEN = "_10010";
	public static final String PH_SETTING_RESERVED_11 = "_10011";
	public static final String PH_SETTING_RESERVED_12 = "_10012";
	public static final String PH_SETTING_RESERVED_13 = "_10013";
	public static final String PH_SETTING_RESERVED_14 = "_10014";
	public static final String PH_SETTING_RESERVED_15 = "_10015";
	public static final String PH_SETTING_RESERVED_16 = "_10016";
	public static final String PH_SETTING_RESERVED_17 = "_10017";
	public static final String PH_SETTING_RESERVED_18 = "_10018";
	public static final String PH_SETTING_RESERVED_19 = "_10019";
	public static final String PH_SETTING_RESERVED_20 = "_10020";
	public static final String PH_SETTING_RESERVED_21 = "_10021";
	public static final String PH_SETTING_RESERVED_22 = "_10022";
	public static final String PH_SETTING_RESERVED_23 = "_10023";
	public static final String PH_SETTING_RESERVED_24 = "_10024";
	public static final String PH_SETTING_RESERVED_25 = "_10025";
	public static final String PH_SETTING_RESERVED_26 = "_10026";
	public static final String PH_SETTING_RESERVED_27 = "_10027";
	public static final String PH_SETTING_RESERVED_28 = "_10028";
	public static final String PH_SETTING_RESERVED_29 = "_10029";
	public static final String PH_SETTING_RESERVED_30 = "_10030";
	public static final String PH_SETTING_RESERVED_31 = "_10031";
	public static final String PH_SETTING_RESERVED_32 = "_10032";
	public static final String PH_SETTING_RESERVED_33 = "_10033";
	public static final String PH_SETTING_RESERVED_34 = "_10034";
	public static final String PH_SETTING_RESERVED_35 = "_10035";
	public static final String PH_SETTING_RESERVED_36 = "_10036";
	public static final String PH_SETTING_RESERVED_37 = "_10037";
	public static final String PH_SETTING_RESERVED_38 = "_10038";
	public static final String PH_SETTING_RESERVED_39 = "_10039";
	public static final String PH_SETTING_RESERVED_40 = "_10040";
	public static final String PH_SETTING_RESERVED_41 = "_10041";
	public static final String PH_SETTING_RESERVED_42 = "_10042";
	public static final String PH_SETTING_RESERVED_43 = "_10043"; 	// 实际的打印喷头数量
	public static final String PH_SETTING_RESERVED_44 = "_10044";
	public static final String PH_SETTING_RESERVED_45 = "_10045";
	public static final String PH_SETTING_RESERVED_46 = "_10046";
	public static final String PH_SETTING_RESERVED_47 = "_10047";
	public static final String PH_SETTING_RESERVED_48 = "_10048";
	public static final String PH_SETTING_RESERVED_49 = "_10049";
	public static final String PH_SETTING_RESERVED_50 = "_10050";
	public static final String PH_SETTING_RESERVED_51 = "_10051";
	public static final String PH_SETTING_RESERVED_52 = "_10052";
	public static final String PH_SETTING_RESERVED_53 = "_10053";
	public static final String PH_SETTING_RESERVED_54 = "_10054";
	public static final String PH_SETTING_RESERVED_55 = "_10055";
	public static final String PH_SETTING_RESERVED_56 = "_10056";
	public static final String PH_SETTING_RESERVED_57 = "_10057";
	public static final String PH_SETTING_RESERVED_58 = "_10058";
	public static final String PH_SETTING_RESERVED_59 = "_10059";
	public static final String PH_SETTING_RESERVED_60 = "_10060";
	public static final String PH_SETTING_RESERVED_61 = "_10061";
	public static final String PH_SETTING_RESERVED_62 = "_10062";
	public static final String PH_SETTING_RESERVED_63 = "_10063";
	public static final String PH_SETTING_RESERVED_64 = "_10064";
// H.M.Wang 2022-10-18 参数扩容32项目
	public static final String PH_SETTING_RESERVED_65 = "_10065";
	public static final String PH_SETTING_RESERVED_66 = "_10066";
	public static final String PH_SETTING_RESERVED_67 = "_10067";
	public static final String PH_SETTING_RESERVED_68 = "_10068";
	public static final String PH_SETTING_RESERVED_69 = "_10069";
	public static final String PH_SETTING_RESERVED_70 = "_10070";
	public static final String PH_SETTING_RESERVED_71 = "_10071";
	public static final String PH_SETTING_RESERVED_72 = "_10072";
	public static final String PH_SETTING_RESERVED_73 = "_10073";
	public static final String PH_SETTING_RESERVED_74 = "_10074";
	public static final String PH_SETTING_RESERVED_75 = "_10075";
	public static final String PH_SETTING_RESERVED_76 = "_10076";
	public static final String PH_SETTING_RESERVED_77 = "_10077";
	public static final String PH_SETTING_RESERVED_78 = "_10078";
	public static final String PH_SETTING_RESERVED_79 = "_10079";
	public static final String PH_SETTING_RESERVED_80 = "_10080";
	public static final String PH_SETTING_RESERVED_81 = "_10081";
	public static final String PH_SETTING_RESERVED_82 = "_10082";
	public static final String PH_SETTING_RESERVED_83 = "_10083";
	public static final String PH_SETTING_RESERVED_84 = "_10084";
	public static final String PH_SETTING_RESERVED_85 = "_10085";
	public static final String PH_SETTING_RESERVED_86 = "_10086";
	public static final String PH_SETTING_RESERVED_87 = "_10087";
	public static final String PH_SETTING_RESERVED_88 = "_10088";
	public static final String PH_SETTING_RESERVED_89 = "_10089";
	public static final String PH_SETTING_RESERVED_90 = "_10090";
	public static final String PH_SETTING_RESERVED_91 = "_10091";
	public static final String PH_SETTING_RESERVED_92 = "_10092";
	public static final String PH_SETTING_RESERVED_93 = "_10093";
	public static final String PH_SETTING_RESERVED_94 = "_10094";
	public static final String PH_SETTING_RESERVED_95 = "_10095";
	public static final String PH_SETTING_RESERVED_96 = "_10096";
// End of H.M.Wang 2022-10-18 参数扩容32项目

	public static final String LAST_MESSAGE = "message";
	public static final String FEATURE_CODE = "code";

	// H.M.Wang 2020-4-18 增加打印密度索引定义，避免硬码满天飞的现象
	public static final int INDEX_PRINT_DENSITY = 2;
	// End of H.M.Wang 2020-4-18 增加打印密度索引定义，避免硬码满天飞的现象

	// 设置里的计数器
	// H.M.Wang 修改下列2行。为计数器清楚前置0
//	public static final int INDEX_COUNTER = 17;
	public static final int INDEX_CLEAR_ZERO = 17;

	public static final int INDEX_DAY_START = 16;
	
	public static final int INDEX_HEAD_TYPE = 30;

	public static final int INDEX_COUNTER_RESET = 31;

    public static final int INDEX_DOT_SIZE = 32;

// H.M.Wang 2022-9-29 增加一个现有参数的索引
	public static final int INDEX_STR = 34;
// End of H.M.Wang 2022-9-29 增加一个现有参数的索引

	public static final int INDEX_SLANT = 35;

// H.M.Wang 2019-9-28 增加1带多参数索引
	public static final int INDEX_ONE_MULTIPLE = 37;

	// H.M.Wang 2019-12-19 追加参数39，数据源的索引,作废原来的索引INDEX_LAN_PRINT
	public static final int INDEX_DATA_SOURCE = 38;

// H.M.Wang 2020-4-27 追加重复打印和编码器脉冲索引值
	public static final int INDEX_REPEAT_PRINT = 6;
	public static final int INDEX_ENCODER_PPR = 9;
// End of H.M.Wang 2020-4-27 追加重复打印和编码器脉冲索引值

	// Kevin-zhao
	public static final int INDEX_LOG_ENABLE = 59;

// H.M.Wang 2021-11-18 追击双列打印头索引
	public static final int INDEX_DUAL_COLUMNS = 60;
// End of H.M.Wang 2021-11-18 追击双列打印头索引

// H.M.Wang 2022-8-25 追加喷嘴加热参数项
	public static final int INDEX_NOZZLE_WARMING = 61;
// End of H.M.Wang 2022-8-25 追加喷嘴加热参数项

	public static final int INDEX_PARAM_63 = 62;

// H.M.Wang 2022-9-1 追加一个客户apk启动的参数，该参数为1时，启动客户的apk，为0时不启动
	public static final int INDEX_USER_APK_START = 63;
// End of H.M.Wang 2022-9-1 追加一个客户apk启动的参数，该参数为1时，启动客户的apk，为0时不启动
// H.M.Wang 2022-10-18 追加两个参数，Slant2和ADJ2。Slant2用于定义64SLANT喷头的第二头倾斜斜率，ADJ2用于定义64SLANT喷头的第二头间隔列数
	public static final int INDEX_SLANT2 = 64;
	public static final int INDEX_ADJ2 = 65;
// End of H.M.Wang 2022-10-18 追加两个参数，Slant2和ADJ2。Slant2用于定义64SLANT喷头的第二头倾斜斜率，ADJ2用于定义64SLANT喷头的第二头间隔列数

// H.M.Wang 11-13 调整各项目的排列顺序，使得相同接近的数据源排在一起。同时调整arrays.xml的数据源排列顺序
	public static final int DATA_SOURCE_DISABLED 	= 0;		// 数据源禁用
	public static final int DATA_SOURCE_BIN 		= 1;		// 数据源使用BIN
	public static final int DATA_SOURCE_FILE 		= 2;		// 数据源使用文件
// H.M.Wang 2021-1-4 追加数据源FILE2，也是从QR.txt读取DT0,DT1,...,DT9,BARCODE的信息，但是DT赋值根据DT变量内部的序号匹配
	public static final int DATA_SOURCE_FILE2 		= 3;		// 数据源使用文件
// End of H.M.Wang 2021-1-4 追加数据源FILE2，也是从QR.txt读取DT0,DT1,...,DT9,BARCODE的信息，但是DT赋值根据DT变量内部的序号匹配
	public static final int DATA_SOURCE_LAN 		= 4;		// 数据源使用以太网数据
// H.M.Wang 2020-6-28 追加专门为网络快速打印设置
	public static final int DATA_SOURCE_FAST_LAN 	= 5;		// 快速网络打印
// End of H.M.Wang 2020-6-28 追加专门为网络快速打印设置
// H.M.Wang 2020-9-30 追加网络心跳数据源
	public static final int DATA_SOURCE_LAN_HEART 	= 6;		// 接收网络心跳信号，2s内未接到报警
// End of H.M.Wang 2020-9-30 追加网络心跳数据源
	public static final int DATA_SOURCE_SCANER1 	= 7;		// 数据源使用串口协议5。接收33位字符串，第2位与第33位必须一致，截取7组数据，添加到DT0-DT6，DT7通过DT6从QR.txt当中检索得到(作为扫码枪的替代输入源)
// H.M.Wang 2021-9-16 追加扫描协议1-FIFO
	public static final int DATA_SOURCE_SCANER1_FIFO= 8;		// 与扫描协议1一致，只是接收到扫描数据后，不直接使用其重新生成打印缓冲区，而是保存在一个FIFO的尾部，FIFO深度暂定为6，
																// 每次打印完成后，重新生成打印缓冲区时，从FIFO头部取数使用，
// End of H.M.Wang 2021-9-16 追加扫描协议1-FIFO
// H.M.Wang 2020-10-30 追加扫描2串口协议
	public static final int DATA_SOURCE_SCANER2 	= 9;		// 扫描2等效串口协议，由4条信息构成，以#为间隔符，间隔符可以变更，4条信息分别设置给DT0-DT3，对群组有效
// End of H.M.Wang 2020-10-30 追加扫描2串口协议
// H.M.Wang 2021-1-15 追加扫描协议3
    public static final int DATA_SOURCE_SCANER3 	= 10;		// 与扫描2一致，但仅允许打印一回，打印完成后即使再次触发也不打印
// End of H.M.Wang 2021-1-15 追加扫描协议3
// H.M.Wang 2021-5-21 追加扫描协议4
	public static final int DATA_SOURCE_SCANER4 	= 11;		// 	添加scanner 4.
																// 1. 扫描reset 码 （内容是Resetcode7799*********后面不管， 只看前13字符）此时指针指向DT0
																// 2. 扫描如以前协议，值给DT0
																// 3. 同上， 但是检查， 如果新内容和老内容相同， 不动作
																// 3. 以此类推， 至DT9
																// 4. DT9 以后回DT0
// End of H.M.Wang 2021-5-21 追加扫描协议4
	public static final int DATA_SOURCE_RS232_1 	= 12;		// 数据源使用串口协议1。EC_DOD协议，按位数紧凑填充前面的计数器。位数不足时，后续计数器不填充，位数超出所有计数器的位数总和时，后面的剪切
	public static final int DATA_SOURCE_RS232_2 	= 13;		// 数据源使用串口协议2。EC_DOD协议，用逗号等分隔符分开各计数器的内容。每个计数器的接收位数大于计数器的预设位数时剪切
	public static final int DATA_SOURCE_RS232_3 	= 14;		// 数据源使用串口协议3。平文直接填充第一个计数器。超出计数器位数部分剪切
	public static final int DATA_SOURCE_RS232_4 	= 15;		// 数据源使用串口协议4。XK3190协议
// H.M.Wang 2020-6-9 追加串口6协议
	public static final int DATA_SOURCE_RS232_6 	= 16;		// 数据源使用串口协议6。接收19位字符串，第8, 9, 10, 11, 13, 14分别设置给DT0-DT5
// End of H.M.Wang 2020-6-9 追加串口6协议
// H.M.Wang 2020-8-13 追加串口协议7
	public static final int DATA_SOURCE_RS232_7 	= 17;		// 数据源使用串口协议7。与串口协议1一致，仅校验位奇偶校验
// End of H.M.Wang 2020-8-13 追加串口协议7
// End of H.M.Wang 11-13 调整各项目的排列顺序，使得相同接近的数据源排在一起。同时调整arrays.xml的数据源排列顺序
// H.M.Wang 2021-3-6 追加串口协议8
	public static final int DATA_SOURCE_RS232_8 	= 18;		// 数据源使用串口协议8。
// End of H.M.Wang 2021-3-6 追加串口协议8
// H.M.Wang 2021-9-24 追加串口协议9
	public static final int DATA_SOURCE_RS232_9 	= 19;		// 数据源使用串口协议9。具体内容是：
																// 接收上位机查询指令 "AA 0D 0A"，如处于待机状态（即打印状态）则返回"DD 0D 0A"，如果不是该状态则不返回或返回其他内容，选择"EE 0D 0A"返回
																// 接收到28位数字字符串 如：2110042290110005265129211009
																// 其中：Bit1-2 (21) -> DT0
																// 		Bit3-4 (10) -> DT1
																// 		Bit5-6 (04) -> DT2
																// 		Bit7-9 (229) -> DT3
																// 		Bit10 (0) -> DT4
																// 		Bit11 (1) -> 如果为0，1，2，3，则分别向DT5赋值N，L，M，H
																// 		Bit12-17 (100052) -> DT6
																// 		Bit18-28 (65129211009) -> DT7
// End of H.M.Wang 2021-9-24 追加串口协议9
// H.M.Wang 2021-9-28 追加串口协议10
    public static final int DATA_SOURCE_RS232_10 	= 20;		// 数据源使用串口协议10。具体内容是：
                                                                // 数据总长度是36位，   1-18 位和19-36 完全相同

                                                                // 这是表头传输数据格
                                                                // 53 54 2C 4E 54 2C 2B 20 20 20 20 35 2E 30 6B 67 0D 0A
                                                                // 我门要做
                                                                // 1. 接收前18位
                                                                // 2.9-12位赋给DT 0.
// End of H.M.Wang 2021-9-28 追加串口协议10

// H.M.Wang 2021-1-30 追加PC命令
	public static final int DATA_PC_COMMAND 	    = 21;		// PC Command
// End of H.M.Wang 2021-1-30 追加PC命令

// H.M.Wang 2022-4-5 追加串口协议11(341串口)
	public static final int DATA_SOURCE_RS232_11 	= 22;		// 数据源使用串口协议11。具体内容是：
	// 走CH341串口（ttyUSB0）
	// 具体要求参照《341串口协议.doc》文档
	// 数据格式为
	// 	帧头：0x1B, 0x53
	// 	功能码：0x31, 0x31
	// 	内容：0x30, 0x30, 0x31, 0x38, 0x2E, 0x32, 0x6B, 0x67
	// 	校验位：0xCB
	// 	帧尾：0x0D, 0x0A,
	// 	（全文实力）：0x1B, 0x53, 0x31, 0x31, 0x30, 0x30, 0x31, 0x38, 0x2E, 0x32, 0x6B, 0x67, 0xCB, 0x0D, 0x0A
// End of H.M.Wang 2022-4-5 追加串口协议11(341串口)
// H.M.Wang 2022-5-16 追加串口协议2无线
	public static final int DATA_SOURCE_RS232_2_WIFI = 23;      // 串口协议2无线。与串口协议2完全一致，只是走CH341串口（ttyUSB0）
// End of H.M.Wang 2022-5-16 追加串口协议2无线
// H.M.Wang 2022-10-8 追加数据源：变量/Auto-data
	public static final int DATA_SOURCE_AUTO_DATA = 24;      	// 该数据源的意图是，使得动态条码使用变量本身保存的信息作为生成条码的依据，而不使用桶里的内容，也不影响桶里的内容，此时，本信息的内容主要为超文本
// End of H.M.Wang 2022-10-8 追加数据源：变量/Auto-data

// H.M.Wang 2021-3-6 追加串口协议8
	public static final int INDEX_LOCAL_ID 			= 57;		// 用于串口协议8当中的本地机器ID。
// End of H.M.Wang 2021-3-6 追加串口协议8

// H.M.Wang 2021-9-24 追加输入设置参数
//	输入信号/Input:
//			0.  屏蔽  None
//			1. 打印/停止Print/Stop
//			2.  镜像    /Mirror
//    public static final int INPUT_PROC_NONE = 0;
//    public static final int INPUT_PROC_PRINT = 1;
//    public static final int INPUT_PROC_MIRROR = 2;
// H.M.Wang 2022-3-20 新的定义 by 工作中心
//     协议１：　禁止GPIO　
//     协议２：　　
//            0x01：是打印开始停止
//            其他禁止
//     协议３：
//            0x01：方向切换
//            其他禁止
//     协议４：
//            0x01：是打印“开始／停止”控制位。其中，打印开始停止实在apk里面处理的，方向控制是在img里面控制的
//            0x02：是计数器清零，包括RTC的数据和正在打印的数据
//            0x04：方向切换
//            0x08：空
//            0xF0段，即0x10 - 0xF0)为打印文件的文件名（数字形式，1-15）

	public static final int INDEX_IPURT_PROC 		= 56;		// 外部输入（PI11）的动作定义参数

	public static final int INPUT_PROTO_1           = 0;        // 禁止GPIO
	public static final int INPUT_PROTO_2           = 1;        // 0x01控制打印，其他管脚禁止
	public static final int INPUT_PROTO_3           = 2;        // 0x01控制方向，其他管脚禁止
// H.M.Wang 2022-5-28 增加清零操作，将原综合操作的PROTO_4改为PROTO_5，清零操作使用PROTO_4
	public static final int INPUT_PROTO_4           = 3;        // 综合
	public static final int INPUT_PROTO_5           = 4;        // 0x01清零
// End of H.M.Wang 2022-5-28 增加清零操作，将原综合操作的PROTO_4改为PROTO_5，清零操作使用PROTO_4
// End of H.M.Wang 追加输入设置参数

// H.M.Wang 2020-3-3 镜像方向定义，影响到参数12，13，20，21
	public static final int DIRECTION_NORMAL = 0;
	public static final int DIRECTION_REVERS = 1;
// End of H.M.Wang 2020-3-3 镜像方向定义，影响到参数12，13，20，21

	/**
	 * 参数39接收网络bin并直接打印
	 * 1 像以前一样载入信息
	 * 2 读参数。 网络打印/LAN print, 默认0， 若是1，  打印就是直接用网络收到的bin， 去掉文件头16byte，  代替生成bin下发。   不在生成bin。
	 * 3每次下发完， 发个消息给上位机。  消息类似查询结果。
	 * 4上位机收到消息，发下一个bin
	 */
//	public static final int INDEX_LAN_PRINT = 38;
	// End of H.M.Wang 2019-12-19 追加参数39，数据源的索引,作废原来的索引INDEX_LAN_PRINT


	
	public static final int INDEX_SPECIFY_HEADS = 42;

	// lightness during screen save mode
	public static final int INDEX_LIGHTNESS = 43;


	/**
	 * 计数器值设置索引号
	 */
	public static final int INDEX_COUNT_1 = 44;
	public static final int INDEX_COUNT_2 = 45;
	public static final int INDEX_COUNT_3 = 46;
	public static final int INDEX_COUNT_4 = 47;
	public static final int INDEX_COUNT_5 = 48;
	public static final int INDEX_COUNT_6 = 49;
	public static final int INDEX_COUNT_7 = 50;
	public static final int INDEX_COUNT_8 = 51;
	public static final int INDEX_COUNT_9 = 52;
	public static final int INDEX_COUNT_10 = 53;

    public static final int INDEX_QRCODE_LAST = 54;

// H.M.Wang 2021-7-23 追加触发一次后重复打印的次数的设置。定位C41
	public static final int INDEX_PRINT_TIMES = 40;
// End of H.M.Wang 2021-7-23 追加触发一次后重复打印的次数的设置。定位C41

// H.M.Wang 2022-5-30 编码器变倍，  希望启用一个参数，  Width Ratio /宽度调节（w）。
//					  用途就是把S14 换算一次，      新的S14 =老的S14 * w/100.
//					  也就是说， 用户设80， 打印出来是以前80% 宽，   200， 就是200% 宽了。
//					  0当100 计算，  这样兼容不设置的情况。10%都当100
	public static final int INDEX_WIDTH_RATIO = 58;
// End of H.M.Wang 2022-5-30 编码器变倍，  希望启用一个参数，  Width Ratio /宽度调节（w）。

// H.M.Wang 2021-5-21 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，编辑页面显示#####
	private String[] mDTBuffer = new String[] {
		"#####", "#####", "#####", "#####", "#####", "#####", "#####", "#####", "#####", "#####"
	};

	public String getDTBuffer(int index) {
		if(index < 0 || index > 9) return "";
		return mDTBuffer[index];
	}

	public void setDTBuffer(int index, String dtStr) {
		if(index < 0 || index > 9) return;
		mDTBuffer[index] = dtStr;
		writeDTPrefs(index, dtStr);
	}
// End of H.M.Wang 2021-5-21 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，编辑页面显示#####

// H.M.Wang 2022-6-13 追加开机时从xml读入已保存的10个DT初始值，更改后保存仅xml文件
	private static final String DT_PREFS = "DTPrefs";
	private static final String TAG_DT = "DT";

	private void readDTPrefs() {
		for(int i=0; i<mDTBuffer.length; i++) {
			try {
				SharedPreferences sp = mContext.getSharedPreferences(DT_PREFS, Context.MODE_PRIVATE);
				mDTBuffer[i] = sp.getString(TAG_DT + i, "#####");
				Debug.d(TAG, "Read: " + TAG_DT + i + " = [" + mDTBuffer[i] + "]");
			} catch(Exception e) {
				Debug.e(TAG, e.getMessage());
			}
		}
	}

	public void writeDTPrefs(int index, String pref) {
		if(index < 0 || index > 9) return;
		try {
			SharedPreferences sp = mContext.getSharedPreferences(DT_PREFS, Context.MODE_PRIVATE);
			SharedPreferences.Editor prefEditor = sp.edit();
			prefEditor.putString(TAG_DT+index, pref);
			prefEditor.apply();
			Debug.d(TAG, "Write: " + TAG_DT + index + " = [" + pref + "]");
		} catch(Exception e) {
			Debug.e(TAG, e.getMessage());
		}
	}
// End of H.M.Wang 2022-6-13 追加开机时从xml读入已保存的10个DT初始值，更改后保存仅xml文件

// H.M.Wang 2022-6-13 即使没有开始打印，也能够设置DT
	public void setRemoteSeparated(final String data) {
		Debug.d(TAG, "String from Remote = [" + data + "]");
		String[] recvStrs = data.split(EC_DOD_Protocol.TEXT_SEPERATOR);

		for(int i=0; i<Math.min(recvStrs.length, 10); i++) {
			setDTBuffer(i, recvStrs[i]);
		}
// H.M.Wang 2022-6-15 追加条码内容的保存桶
		if(recvStrs.length >= 11) {
			setBarcodeBuffer(recvStrs[10]);
		}
// End of  H.M.Wang 2022-6-15 追加条码内容的保存桶
	}
// End of H.M.Wang 2022-6-13 即使没有开始打印，也能够设置DT

// H.M.Wang 2022-6-15 追加条码内容的保存桶
	private static final String BC_PREFS = "BCPrefs";
	private static final String TAG_BC = "BC";

	private String mBarCodeBuffer = "123456";
	public String getBarcodeBuffer() {
		return mBarCodeBuffer;
	}

	public void setBarcodeBuffer(String bcStr) {
		mBarCodeBuffer = bcStr;
		writeBarcodePrefs(bcStr);
	}
	private void readBarcodePrefs() {
		try {
			SharedPreferences sp = mContext.getSharedPreferences(BC_PREFS, Context.MODE_PRIVATE);
			mBarCodeBuffer = sp.getString(TAG_BC, "123456");
			Debug.d(TAG, "Read: " + TAG_BC + " = [" + mBarCodeBuffer + "]");
		} catch(Exception e) {
			Debug.e(TAG, e.getMessage());
		}
	}
	public void writeBarcodePrefs(String pref) {
		try {
			SharedPreferences sp = mContext.getSharedPreferences(BC_PREFS, Context.MODE_PRIVATE);
			SharedPreferences.Editor prefEditor = sp.edit();
			prefEditor.putString(TAG_BC, pref);
			prefEditor.apply();
			Debug.d(TAG, "Write: " + TAG_BC + " = [" + pref + "]");
		} catch(Exception e) {
			Debug.e(TAG, e.getMessage());
		}
	}
// End of H.M.Wang 2022-6-15 追加条码内容的保存桶

// H.M.Wang 2020-12-28 追加FIFO打印缓冲区数量设置
	public static final int INDEX_FIFO_SIZE = 55;
// End of H.M.Wang 2020-12-28 追加FIFO打印缓冲区数量设置

	/*
	 * 目前參數使用情況：
	 * 1、參數1~24：分配給FPGA
	 * 2、參數25：每列列高
	 * 3、參數31：是否双列
	 * 4、參數32：双列偏移量
	 */
// H.M.Wang 2022-10-18 参数扩容32项目
//	public int mParam[] = new int[64];
	public int mParam[] = new int[96];
// End of H.M.Wang 2022-10-18 参数扩容32项目
	public int mFPGAParam[] = new int[24];
	
	public Context mContext;
	public static SystemConfigFile mInstance;
	
	public static HashMap<Integer, HashMap<String,Integer>> mParamRange = new HashMap<Integer, HashMap<String,Integer>>();

	public static SystemConfigFile getInstance(Context context) {
		if (mInstance == null) {
			mInstance = new SystemConfigFile(context);
		}
		return mInstance;
	}

	// H.M.Wang 下列函数。为计数器清楚前置0
	public static SystemConfigFile getInstance() {
		return mInstance;
	}

	public SystemConfigFile(Context context) {
		mContext = context;
		init();
	}
	public void init() {
// H.M.Wang 2022-6-13 追加开机时从xml读入已保存的10个DT初始值
		readDTPrefs();
// End of H.M.Wang 2022-6-13 追加开机时从xml读入已保存的10个DT初始值
// H.M.Wang 2022-6-15 追加条码内容的保存桶
		readBarcodePrefs();
// End of H.M.Wang 2022-6-15 追加条码内容的保存桶
		initParamRange();
		if(parseSystemCofig())
			return;
		//default param
		for (int i = 0; i < mParam.length; i++) {
			mParam[i] = checkParam(i+1, mParam[i]);
		}
	}
	public boolean parseSystemCofig() {
		FileReader reader=null;
		BufferedReader br = null;
		String tag;
//		ArrayList<String> paths = ConfigPath.getMountedUsb();
//		if (paths == null || paths.isEmpty()) {
//			Debug.d(TAG, "--->no usb storage mounted");
//			return false;
//		}
		/*
		 * use this first usb as default 
		 */
		//Debug.d(TAG, "--->usb root path:" + paths.get(0));
		XmlInputStream inStream = new XmlInputStream(Configs.CONFIG_PATH_FLASH + Configs.SYSTEM_CONFIG_XML);
		List<XmlTag> list = inStream.read();
		if (list == null) {
			Debug.d(TAG, "--->read system_config file fail");
			return false;
		}
		for (XmlTag t : list) {
			try {
				tag = t.getKey();
				if (tag.equalsIgnoreCase(PH_SETTING_ENCODER)) {
					mParam[0] = Integer.parseInt(t.getValue());
					mParam[0] = checkParam(1, mParam[0]);
				} else if (tag.equalsIgnoreCase(PH_SETTING_TRIGER_MODE)) {
					mParam[1] = Integer.parseInt(t.getValue());
					mParam[1] = checkParam(2, mParam[1]);
				} else if (tag.equalsIgnoreCase(PH_SETTING_HIGH_DELAY)) {
					mParam[2] = Integer.parseInt(t.getValue());
					mParam[2] = checkParam(3, mParam[2]);
				} else if (tag.equalsIgnoreCase(PH_SETTING_LOW_DELAY)) {
					mParam[3] = Integer.parseInt(t.getValue());
				/*光电延时 0-65535 默认值100*/
					mParam[3] = checkParam(4, mParam[3]);
				} else if (tag.equalsIgnoreCase(PH_SETTING_PHOOUTPUT_PERIOD)) {
					mParam[4] = Integer.parseInt(t.getValue());
				/*字宽(毫秒） 下发FPGA-S5 0-65535*/
					mParam[4] = checkParam(5, mParam[4]);
				} else if (tag.equalsIgnoreCase(PH_SETTING_TIMED_PERIOD)) {
					mParam[5] = Integer.parseInt(t.getValue());
					mParam[5] = checkParam(6, mParam[5]);
				} else if (tag.equalsIgnoreCase(PH_SETTING_TRIGER_PULSE)) {
					mParam[6] = Integer.parseInt(t.getValue());
				/*列间脉冲 下发FPGA- S7	1-50*/
					mParam[6] = checkParam(7, mParam[6]);
				} else if (tag.equalsIgnoreCase(PH_SETTING_LENFIXED_PULSE)) {
					mParam[7] = Integer.parseInt(t.getValue());
				/*定长脉冲 下发FPGA-S8 	1-65535*/
					mParam[7] = checkParam(8, mParam[7]);
				} else if (tag.equalsIgnoreCase(PH_SETTING_DELAY_PULSE)) {
					mParam[8] = Integer.parseInt(t.getValue());
				/*脉冲延时 下发FPGA-S9 	1-65535*/
					mParam[8] = checkParam(9, mParam[8]);
				} else if (tag.equalsIgnoreCase(PH_SETTING_HIGH_LEN)) {
					mParam[9] = Integer.parseInt(t.getValue());
				/*墨点大小 200-2000 默认值800*/
					mParam[9] = checkParam(10, mParam[9]);
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_11)) {
					mParam[10] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_12)) {
					mParam[11] = Integer.parseInt(t.getValue());
					mParam[11] = checkParam(12, mParam[11]);
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_13)) {
					mParam[12] = Integer.parseInt(t.getValue());
					mParam[12] = checkParam(13, mParam[12]);
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_14)) {
					mParam[13] = Integer.parseInt(t.getValue());
					mParam[13] = checkParam(14, mParam[13]);
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_15)) {
					mParam[14] = Integer.parseInt(t.getValue());
					mParam[14] = checkParam(15, mParam[14]);
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_16)) {
					mParam[15] = Integer.parseInt(t.getValue());
				/*加重 0-9 默认值0*/
					mParam[15] = checkParam(16, mParam[15]);
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_17)) {
					mParam[16] = Integer.parseInt(t.getValue());
					mParam[16] = checkParam(17, mParam[16]);
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_18)) {
					mParam[17] = Integer.parseInt(t.getValue());
					mParam[17] = checkParam(18, mParam[17]);
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_19)) {
					mParam[18] = Integer.parseInt(t.getValue());
					mParam[18] = checkParam(19, mParam[18]);
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_20)) {
					mParam[19] = Integer.parseInt(t.getValue());
					mParam[19] = checkParam(20, mParam[19]);
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_21)) {
					mParam[20] = Integer.parseInt(t.getValue());
					mParam[20] = checkParam(21, mParam[20]);
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_22)) {
					mParam[21] = Integer.parseInt(t.getValue());
					mParam[21] = checkParam(22, mParam[21]);
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_23)) {
					mParam[22] = Integer.parseInt(t.getValue());
					mParam[23] = checkParam(23, mParam[22]);
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_24)) {
					mParam[23] = Integer.parseInt(t.getValue());
					mParam[23] = checkParam(24, mParam[23]);
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_25)) {
					mParam[24] = Integer.parseInt(t.getValue());
					mParam[24] = checkParam(25, mParam[24]);
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_26)) {
					mParam[25] = Integer.parseInt(t.getValue());
					mParam[25] = checkParam(26, mParam[25]);
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_27)) {
					mParam[26] = Integer.parseInt(t.getValue());
					mParam[26] = checkParam(27, mParam[26]);
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_28)) {
					mParam[27] = Integer.parseInt(t.getValue());
					mParam[27] = checkParam(28, mParam[27]);
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_29)) {
					mParam[28] = Integer.parseInt(t.getValue());
					mParam[28] = checkParam(29, mParam[28]);
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_30)) {
					mParam[29] = Integer.parseInt(t.getValue());
					mParam[29] = checkParam(30, mParam[29]);
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_31)) {
					mParam[30] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_32)) {
					mParam[31] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_33)) {
					mParam[32] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_34)) {
					mParam[33] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_35)) {
					mParam[34] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_36)) {
					mParam[35] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_37)) {
					mParam[36] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_38)) {
					mParam[37] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_39)) {
					mParam[38] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_40)) {
					mParam[39] = Integer.parseInt(t.getValue());
					mParam[39] = checkParam(40, mParam[39]);
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_41)) {
					mParam[40] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_42)) {
					mParam[41] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_43)) {
					mParam[42] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_44)) {
					mParam[43] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_45)) {
					mParam[44] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_46)) {
					mParam[45] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_47)) {
					mParam[46] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_48)) {
					mParam[47] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_49)) {
					mParam[48] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_50)) {
					mParam[49] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_51)) {
					mParam[50] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_52)) {
					mParam[51] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_53)) {
					mParam[52] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_54)) {
					mParam[53] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_55)) {
					mParam[54] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_56)) {
					mParam[55] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_57)) {
					mParam[56] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_58)) {
					mParam[57] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_59)) {
					mParam[58] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_60)) {
					mParam[59] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_61)) {
					mParam[60] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_62)) {
					mParam[61] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_63)) {
					mParam[62] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_64)) {
					mParam[63] = Integer.parseInt(t.getValue());
// H.M.Wang 2022-10-18 参数扩容32项目
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_65)) {
					mParam[64] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_66)) {
					mParam[65] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_67)) {
					mParam[66] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_68)) {
					mParam[67] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_69)) {
					mParam[68] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_70)) {
					mParam[69] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_71)) {
					mParam[70] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_72)) {
					mParam[71] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_73)) {
					mParam[72] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_74)) {
					mParam[73] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_75)) {
					mParam[74] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_76)) {
					mParam[75] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_77)) {
					mParam[76] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_78)) {
					mParam[77] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_79)) {
					mParam[78] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_80)) {
					mParam[79] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_81)) {
					mParam[80] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_82)) {
					mParam[81] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_83)) {
					mParam[82] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_84)) {
					mParam[83] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_85)) {
					mParam[84] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_86)) {
					mParam[85] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_87)) {
					mParam[86] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_88)) {
					mParam[87] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_89)) {
					mParam[88] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_90)) {
					mParam[89] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_91)) {
					mParam[90] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_92)) {
					mParam[91] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_93)) {
					mParam[92] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_94)) {
					mParam[93] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_95)) {
					mParam[94] = Integer.parseInt(t.getValue());
				} else if (tag.equalsIgnoreCase(PH_SETTING_RESERVED_96)) {
					mParam[95] = Integer.parseInt(t.getValue());
// End of H.M.Wang 2022-10-18 参数扩容32项目
				}
				Debug.d(TAG, "===>tag key:" + tag + ", value:" + t.getValue());
			} catch ( Exception e) {
				continue;
			}
		}
		inStream.close();

		for (int i = INDEX_COUNT_1; i <= INDEX_COUNT_10; i++) {
			mParam[i] = (int)(RTCDevice.getInstance(mContext).read(i - INDEX_COUNT_1));
			Debug.d(TAG, "mParam[" + i + "] = " + mParam[i]);
		}

// H.M.Wang 2020-5-15 QRLast移植RTC的0x38地址保存，可以通过参数设置管理
        mParam[INDEX_QRCODE_LAST] = RTCDevice.getInstance(mContext).readQRLast();
// End of H.M.Wang 2020-5-15 QRLast移植RTC的0x38地址保存，可以通过参数设置管理

		return true;
		/*
		try {
			reader = new FileReader(file);
			br = new BufferedReader(reader);
			String line = br.readLine();
			while (line != null) {
				String[] args = line.split(" ");
				if (PH_SETTING_ENCODER.equals(args[0])) {
					Debug.d(TAG, "===>param: "+PH_SETTING_ENCODER);
					if (args.length < 2) {
						mParam[0] = 0;
					} else {
						mParam[0] = Integer.parseInt(args[1]);
					}
					
				} else if (PH_SETTING_TRIGER_MODE.equals(args[0])) {
					Debug.d(TAG, "===>param: "+PH_SETTING_TRIGER_MODE);
					if (args.length < 2) {
						mParam[1] = 0;
					} else {
						mParam[1] = Integer.parseInt(args[1]);
					}
				} else if (PH_SETTING_HIGH_DELAY.equals(args[0])) {
					Debug.d(TAG, "===>param: "+PH_SETTING_HIGH_DELAY);
					if (args.length < 2) {
						mParam[2] = 0;
					} else {
						mParam[2] = Integer.parseInt(args[1]);
					}
					
				} else if (PH_SETTING_LOW_DELAY.equals(args[0])) {
					Debug.d(TAG, "===>param: "+PH_SETTING_LOW_DELAY);
					if (args.length < 2) {
						mParam[3] = 0;
					} else {
						mParam[3] = Integer.parseInt(args[1]);
					}
					
				} else if (PH_SETTING_PHOOUTPUT_PERIOD.equals(args[0])) {
					Debug.d(TAG, "===>param: "+PH_SETTING_PHOOUTPUT_PERIOD);
					if (args.length < 2) {
						mParam[4] = 0;
					} else {
						mParam[4] = Integer.parseInt(args[1]);
					}
					
				} else if (PH_SETTING_TIMED_PERIOD.equals(args[0])) {
					Debug.d(TAG, "===>param: "+PH_SETTING_TIMED_PERIOD);
					if (args.length < 2) {
						mParam[5] = 0;
					} else {
						mParam[5] = Integer.parseInt(args[1]);
					}
					
				} else if (PH_SETTING_TRIGER_PULSE.equals(args[0])) {
					Debug.d(TAG, "===>param: "+PH_SETTING_TRIGER_PULSE);
					if (args.length < 2) {
						mParam[6] = 0;
					} else {
						mParam[6] = Integer.parseInt(args[1]);
					}
					
				} else if (PH_SETTING_LENFIXED_PULSE.equals(args[0])) {
					Debug.d(TAG, "===>param: "+PH_SETTING_LENFIXED_PULSE);
					if (args.length < 2) {
						mParam[7] = 0;
					} else {
						mParam[7] = Integer.parseInt(args[1]);
					}
					
				} else if (PH_SETTING_DELAY_PULSE.equals(args[0])) {
					Debug.d(TAG, "===>param: "+PH_SETTING_DELAY_PULSE);
					if (args.length < 2) {
						mParam[8] = 0;
					} else {
						mParam[8] = Integer.parseInt(args[1]);
					}
					
				} else if (PH_SETTING_HIGH_LEN.equals(args[0])) {
					Debug.d(TAG, "===>param: "+PH_SETTING_HIGH_LEN);
					if (args.length < 2) {
						mParam[9] = 0;
					} else {
						mParam[9] = Integer.parseInt(args[1]);
					}
				} else {
					Debug.d(TAG, "===>unknow param: "+args[0]);
				}
				line = br.readLine();
			}
			br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
		}
		*/
	}
	
	
	public void saveConfig() {
		
//		ArrayList<String> paths = ConfigPath.getMountedUsb();
//		if (paths == null || paths.isEmpty()) {
//			Debug.d(TAG, "===>saveConfig error");
//			return ;
//		}
		
		/*
		 * use the first usb as the default device
		 */
//		String dev = paths.get(0);
		File dir = new File(Configs.CONFIG_PATH_FLASH + Configs.SYSTEM_CONFIG_DIR);
		if (!dir.exists()) {
			if(dir.mkdirs() == false)
				return;
		}
		Debug.d(TAG, "===>dir:"+dir.getAbsolutePath());
		ArrayList<XmlTag> list = new ArrayList<XmlTag>();
		XmlTag tag1 = new XmlTag(PH_SETTING_ENCODER, String.valueOf(mParam[0]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_TRIGER_MODE, String.valueOf(mParam[1]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_HIGH_DELAY, String.valueOf(mParam[2]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_LOW_DELAY, String.valueOf(mParam[3]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_PHOOUTPUT_PERIOD, String.valueOf(mParam[4]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_TIMED_PERIOD, String.valueOf(mParam[5]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_TRIGER_PULSE, String.valueOf(mParam[6]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_LENFIXED_PULSE, String.valueOf(mParam[7]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_DELAY_PULSE, String.valueOf(mParam[8]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_HIGH_LEN, String.valueOf(mParam[9]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_11, String.valueOf(mParam[10]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_12, String.valueOf(mParam[11]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_13, String.valueOf(mParam[12]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_14, String.valueOf(mParam[13]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_15, String.valueOf(mParam[14]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_16, String.valueOf(mParam[15]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_17, String.valueOf(mParam[16]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_18, String.valueOf(mParam[17]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_19, String.valueOf(mParam[18]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_20, String.valueOf(mParam[19]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_21, String.valueOf(mParam[20]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_22, String.valueOf(mParam[21]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_23, String.valueOf(mParam[22]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_24, String.valueOf(mParam[23]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_25, String.valueOf(mParam[24]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_26, String.valueOf(mParam[25]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_27, String.valueOf(mParam[26]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_28, String.valueOf(mParam[27]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_29, String.valueOf(mParam[28]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_30, String.valueOf(mParam[29]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_31, String.valueOf(mParam[30]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_32, String.valueOf(mParam[31]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_33, String.valueOf(mParam[32]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_34, String.valueOf(mParam[33]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_35, String.valueOf(mParam[34]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_36, String.valueOf(mParam[35]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_37, String.valueOf(mParam[36]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_38, String.valueOf(mParam[37]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_39, String.valueOf(mParam[38]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_40, String.valueOf(mParam[39]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_41, String.valueOf(mParam[40]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_42, String.valueOf(mParam[41]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_43, String.valueOf(mParam[42]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_44, String.valueOf(mParam[43]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_45, String.valueOf(mParam[44]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_46, String.valueOf(mParam[45]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_47, String.valueOf(mParam[46]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_48, String.valueOf(mParam[47]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_49, String.valueOf(mParam[48]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_50, String.valueOf(mParam[49]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_51, String.valueOf(mParam[50]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_52, String.valueOf(mParam[51]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_53, String.valueOf(mParam[52]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_54, String.valueOf(mParam[53]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_55, String.valueOf(mParam[54]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_56, String.valueOf(mParam[55]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_57, String.valueOf(mParam[56]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_58, String.valueOf(mParam[57]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_59, String.valueOf(mParam[58]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_60, String.valueOf(mParam[59]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_61, String.valueOf(mParam[60]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_62, String.valueOf(mParam[61]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_63, String.valueOf(mParam[62]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_64, String.valueOf(mParam[63]));
		list.add(tag1);
// H.M.Wang 2022-10-18 参数扩容32项目
		tag1 = new XmlTag(PH_SETTING_RESERVED_65, String.valueOf(mParam[64]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_66, String.valueOf(mParam[65]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_67, String.valueOf(mParam[66]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_68, String.valueOf(mParam[67]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_69, String.valueOf(mParam[68]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_70, String.valueOf(mParam[69]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_71, String.valueOf(mParam[70]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_72, String.valueOf(mParam[71]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_73, String.valueOf(mParam[72]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_74, String.valueOf(mParam[73]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_75, String.valueOf(mParam[74]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_76, String.valueOf(mParam[75]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_77, String.valueOf(mParam[76]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_78, String.valueOf(mParam[77]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_79, String.valueOf(mParam[78]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_80, String.valueOf(mParam[79]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_81, String.valueOf(mParam[80]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_82, String.valueOf(mParam[81]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_83, String.valueOf(mParam[82]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_84, String.valueOf(mParam[83]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_85, String.valueOf(mParam[84]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_86, String.valueOf(mParam[85]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_87, String.valueOf(mParam[86]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_88, String.valueOf(mParam[87]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_89, String.valueOf(mParam[88]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_90, String.valueOf(mParam[89]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_91, String.valueOf(mParam[90]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_92, String.valueOf(mParam[91]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_93, String.valueOf(mParam[92]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_94, String.valueOf(mParam[93]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_95, String.valueOf(mParam[94]));
		list.add(tag1);
		tag1 = new XmlTag(PH_SETTING_RESERVED_96, String.valueOf(mParam[95]));
		list.add(tag1);
// End of H.M.Wang 2022-10-18 参数扩容32项目
		XmlOutputStream stream = new XmlOutputStream(Configs.CONFIG_PATH_FLASH + Configs.SYSTEM_CONFIG_XML);
		stream.write(list);
		stream.close();

		long[] counters = new long[10];
		for (int i = INDEX_COUNT_1; i <= INDEX_COUNT_10; i++) {
			counters[i - INDEX_COUNT_1] = mParam[i];
		}
		RTCDevice.getInstance(mContext).writeAll(counters);

// H.M.Wang 2020-5-15 QRLast移植RTC的0x38地址保存，可以通过参数设置管理
        RTCDevice.getInstance(mContext).writeQRLast(mParam[INDEX_QRCODE_LAST]);
// End of H.M.Wang 2020-5-15 QRLast移植RTC的0x38地址保存，可以通过参数设置管理
	}
	
	/*
	public void saveSettings() {
		ArrayList<XmlTag> tags = new ArrayList<XmlTag>();
		tags.add(new XmlTag(PH_SETTING_ENCODER, String.valueOf(mParam1)));
		tags.add(new XmlTag(PH_SETTING_TRIGER_MODE, String.valueOf(mParam2)));
		tags.add(new XmlTag(PH_SETTING_HIGH_DELAY, String.valueOf(mParam[2])));
		tags.add(new XmlTag(PH_SETTING_LOW_DELAY, String.valueOf(mParam[3])));
		tags.add(new XmlTag(PH_SETTING_PHOOUTPUT_PERIOD, String.valueOf(mParam[4])));
		tags.add(new XmlTag(PH_SETTING_TIMED_PERIOD, String.valueOf(mParam[5])));
		tags.add(new XmlTag(PH_SETTING_TRIGER_PULSE, String.valueOf(mParam[6])));
		tags.add(new XmlTag(PH_SETTING_LENFIXED_PULSE, String.valueOf(mParam[7])));
		tags.add(new XmlTag(PH_SETTING_DELAY_PULSE, String.valueOf(mParam[8])));
		tags.add(new XmlTag(PH_SETTING_HIGH_LEN, String.valueOf(mParam[9])));
		tags.add(new XmlTag(PH_SETTING_RESERVED_11, String.valueOf(mParam[10])));
		tags.add(new XmlTag(PH_SETTING_RESERVED_12, String.valueOf(mParam[11])));
		tags.add(new XmlTag(PH_SETTING_RESERVED_13, String.valueOf(mParam[12])));
		tags.add(new XmlTag(PH_SETTING_RESERVED_14, String.valueOf(mParam[13])));
		tags.add(new XmlTag(PH_SETTING_RESERVED_15, String.valueOf(mParam[14])));
		tags.add(new XmlTag(PH_SETTING_RESERVED_16, String.valueOf(mParam[15])));
		tags.add(new XmlTag(PH_SETTING_RESERVED_17, String.valueOf(mParam[16])));
		tags.add(new XmlTag(PH_SETTING_RESERVED_18, String.valueOf(mParam[17])));
		tags.add(new XmlTag(PH_SETTING_RESERVED_19, String.valueOf(mParam[18])));
		tags.add(new XmlTag(PH_SETTING_RESERVED_20, String.valueOf(mParam[19])));
		tags.add(new XmlTag(PH_SETTING_RESERVED_21, String.valueOf(mParam[20])));
		tags.add(new XmlTag(PH_SETTING_RESERVED_22, String.valueOf(mParam[21])));
		tags.add(new XmlTag(PH_SETTING_RESERVED_23, String.valueOf(mParam[22])));
		tags.add(new XmlTag(PH_SETTING_RESERVED_24, String.valueOf(mParam[23])));
		tags.add(new XmlTag(PH_SETTING_RESERVED_25, String.valueOf(mParam[24])));
		tags.add(new XmlTag(PH_SETTING_RESERVED_26, String.valueOf(mParam[25])));
		tags.add(new XmlTag(PH_SETTING_RESERVED_27, String.valueOf(mParam[26])));
		tags.add(new XmlTag(PH_SETTING_RESERVED_28, String.valueOf(mParam[27])));
		tags.add(new XmlTag(PH_SETTING_RESERVED_29, String.valueOf(mParam[28])));
		tags.add(new XmlTag(PH_SETTING_RESERVED_30, String.valueOf(mParam[29])));
		tags.add(new XmlTag(PH_SETTING_RESERVED_31, String.valueOf(mResv31)));
		tags.add(new XmlTag(PH_SETTING_RESERVED_32, String.valueOf(mResv32)));
		tags.add(new XmlTag(PH_SETTING_RESERVED_33, String.valueOf(mResv33)));
		tags.add(new XmlTag(PH_SETTING_RESERVED_34, String.valueOf(mResv34)));
		tags.add(new XmlTag(PH_SETTING_RESERVED_35, String.valueOf(mResv35)));
		tags.add(new XmlTag(PH_SETTING_RESERVED_36, String.valueOf(mResv36)));
		tags.add(new XmlTag(PH_SETTING_RESERVED_37, String.valueOf(mResv37)));
		tags.add(new XmlTag(PH_SETTING_RESERVED_38, String.valueOf(mResv38)));
		tags.add(new XmlTag(PH_SETTING_RESERVED_39, String.valueOf(mResv39)));
		tags.add(new XmlTag(PH_SETTING_RESERVED_40, String.valueOf(mResv40)));
		tags.add(new XmlTag(PH_SETTING_RESERVED_41, String.valueOf(mResv41)));
		tags.add(new XmlTag(PH_SETTING_RESERVED_42, String.valueOf(mResv42)));
		tags.add(new XmlTag(PH_SETTING_RESERVED_43, String.valueOf(mResv43)));
		tags.add(new XmlTag(PH_SETTING_RESERVED_44, String.valueOf(mResv44)));
		tags.add(new XmlTag(PH_SETTING_RESERVED_45, String.valueOf(mResv45)));
		tags.add(new XmlTag(PH_SETTING_RESERVED_46, String.valueOf(mResv46)));
		tags.add(new XmlTag(PH_SETTING_RESERVED_47, String.valueOf(mResv47)));
		tags.add(new XmlTag(PH_SETTING_RESERVED_48, String.valueOf(mResv48)));
		tags.add(new XmlTag(PH_SETTING_RESERVED_49, String.valueOf(mResv49)));
		tags.add(new XmlTag(PH_SETTING_RESERVED_50, String.valueOf(mResv50)));
		tags.add(new XmlTag(PH_SETTING_RESERVED_51, String.valueOf(mResv51)));
		tags.add(new XmlTag(PH_SETTING_RESERVED_52, String.valueOf(mResv52)));
		tags.add(new XmlTag(PH_SETTING_RESERVED_53, String.valueOf(mResv53)));
		tags.add(new XmlTag(PH_SETTING_RESERVED_54, String.valueOf(mResv54)));
		tags.add(new XmlTag(PH_SETTING_RESERVED_55, String.valueOf(mResv55)));
		tags.add(new XmlTag(PH_SETTING_RESERVED_56, String.valueOf(mResv56)));
		tags.add(new XmlTag(PH_SETTING_RESERVED_57, String.valueOf(mResv57)));
		tags.add(new XmlTag(PH_SETTING_RESERVED_58, String.valueOf(mResv58)));
		tags.add(new XmlTag(PH_SETTING_RESERVED_59, String.valueOf(mResv59)));
		tags.add(new XmlTag(PH_SETTING_RESERVED_60, String.valueOf(mResv60)));
		tags.add(new XmlTag(PH_SETTING_RESERVED_61, String.valueOf(mResv61)));
		tags.add(new XmlTag(PH_SETTING_RESERVED_62, String.valueOf(mResv62)));
	}
	*/

// H.M.Wang 2022-5-23 从feature.xml读取featurecode。策略是：
//	(1) 如果没有该文件，或者文件格式不正确，读不到featurecode，则作为没有该文件，而生成一个新的。并且将当前versionCode中的后4位（该versionCode必须是9位的）写入。并且作为featurecode的值
//	(2) 如果能够读到该文件中的featurecode，则作为featurecode的值

	private String mFeatureCode = "";

	public String getPackageFeatureCode() {
		PackageManager pm = mContext.getPackageManager();
		int curVersion;
		int curFeature;

		try {
			curVersion = pm.getPackageInfo(mContext.getPackageName(), 0).versionCode;
		} catch (PackageManager.NameNotFoundException e) {
			Debug.e(TAG, e.getMessage());
			return "";
		}

		if(curVersion / 100000 > 0) {		// 5位以上
			curFeature = curVersion % 10000;
		} else {
			return "";
		}

		Debug.d(TAG, "Package Feature Code: " + curFeature);

		return String.valueOf(curFeature);
	}

	public String getFeatureCode() {
		if(mFeatureCode.isEmpty()) mFeatureCode = readFeatureCode();

		Debug.d(TAG, "Local Feature Code: " + mFeatureCode);
		return mFeatureCode;
	}

	public String readFeatureCode() {
		String tag;

		XmlInputStream inStream = new XmlInputStream(Configs.CONFIG_PATH_FLASH + Configs.LAST_FEATURE_XML);
		if(null == inStream) {
			writeFeatureCode();
			return mFeatureCode;
		}

		List<XmlTag> list = inStream.read();
		if (list == null) {
			inStream.close();
			writeFeatureCode();
			return mFeatureCode;
		}

		for (XmlTag t : list) {
			tag = t.getKey();
			if (tag.equalsIgnoreCase(FEATURE_CODE)) {
				inStream.close();
				Debug.d(TAG, "Read: tag key:"+tag+", value:"+t.getValue());
				return t.getValue();
			}
		}
		inStream.close();
		writeFeatureCode();
		return mFeatureCode;
	}

	public void writeFeatureCode() {
		File dir = new File(Configs.CONFIG_PATH_FLASH + Configs.SYSTEM_CONFIG_DIR);
		if (!dir.exists()) {
			if(dir.mkdirs() == false)
				return;
		}

		String curFeature = getPackageFeatureCode();
		if(curFeature.isEmpty()) return;

		ArrayList<XmlTag> list = new ArrayList<XmlTag>();
		XmlTag tag1 = new XmlTag(FEATURE_CODE, curFeature);
		list.add(tag1);
		XmlOutputStream stream = new XmlOutputStream(Configs.CONFIG_PATH_FLASH + Configs.LAST_FEATURE_XML);
		Debug.d(TAG, "Write: tag key:"+FEATURE_CODE+", value:"+curFeature);
		if(stream.write(list)) {
			mFeatureCode = curFeature;
		}
	}
// End of H.M.Wang 2022-5-23 从feature.xml读取featurecode。策略是：

	public String getLastMsg() {
		
		String tag;
//		ArrayList<String> paths = ConfigPath.getMountedUsb();
//		if (paths == null || paths.isEmpty()) {
//			return null;
//		}
//		Debug.d(TAG, "===>path:"+paths.get(0));
		XmlInputStream inStream = new XmlInputStream(Configs.CONFIG_PATH_FLASH + Configs.LAST_MESSAGE_XML);
		List<XmlTag> list = inStream.read();
		if (list == null) {
			inStream.close();
			return null;
		}
		for (XmlTag t : list) {
			tag = t.getKey();
			if (tag.equalsIgnoreCase(LAST_MESSAGE)) {
				inStream.close();
				return t.getValue();
			} 
			Debug.d(TAG, "===>tag key:"+tag+", value:"+t.getValue());
		}
		inStream.close();
		return null;
	}
	
	public void saveLastMsg(String name) {
		
//		ArrayList<String> paths = ConfigPath.getMountedUsb();
//		if (paths == null || paths.isEmpty() || name == null) {
//			Debug.d(TAG, "===>saveConfig error");
//			return ;
//		}
		if (name == null) {
			return ;
		}
		File file = new File(name);
		
		/*
		 * use the first usb as the default device
		 */
//		String dev = paths.get(0);
		File dir = new File(Configs.CONFIG_PATH_FLASH + Configs.SYSTEM_CONFIG_DIR);
		if (!dir.exists()) {
			if(dir.mkdirs() == false)
				return;
		}
		ArrayList<XmlTag> list = new ArrayList<XmlTag>();
		XmlTag tag1 = new XmlTag(LAST_MESSAGE, file.getName());
		list.add(tag1);
		XmlOutputStream stream = new XmlOutputStream(Configs.CONFIG_PATH_FLASH + Configs.LAST_MESSAGE_XML);
		stream.write(list);
		
	}
	
	public static final String TAG_PARAMS = "params";
	public static final String TAG_PARAM = "param";
	public static final String TAG_ID = "id";
	public static final String TAG_MIN = "min";
	public static final String TAG_MAX = "max";
	public static final String TAG_DEFAULT = "default";
	
	/**
	 * 初始化已知参数的取值范围和默认值
	 * TODO:后期做成通过xml进行配置
	 */
	public void initParamRange() {
		Debug.d(TAG, "====>initParamRange");
		int id=1;
		HashMap<String, Integer> map = null;
		try {
			InputStream inputStream = mContext.getAssets().open("params.xml");
			XmlPullParser parser = Xml.newPullParser();
			parser.setInput(inputStream, "utf-8");
			int event = parser.getEventType();
			while (event != XmlPullParser.END_DOCUMENT) {
				switch (event) {
				case XmlPullParser.START_DOCUMENT:
					
					break;
				case XmlPullParser.START_TAG:
					Debug.d(TAG, "--->tag: " + parser.getName());
					if (TAG_PARAMS.equals(parser.getName())) {
						
					} else if (TAG_PARAM.equals(parser.getName())) {
						map = new HashMap<String, Integer>();
					} else if (TAG_ID.equals(parser.getName())) {
						parser.next();
						Debug.d(TAG, "--->id: " + parser.getText());
						id = Integer.parseInt(parser.getText());
					} else if (TAG_MIN.equals(parser.getName())) {
						parser.next();
						map.put("min", Integer.parseInt(parser.getText()));
					} else if (TAG_MAX.equals(parser.getName())) {
						parser.next();
						map.put("max", Integer.parseInt(parser.getText()));
					} else if (TAG_DEFAULT.equals(parser.getName())) {
						parser.next();
						map.put("default", Integer.parseInt(parser.getText()));
					}
					
					break;
				case XmlPullParser.END_TAG:
					if (TAG_PARAM.equals(parser.getName())) {
						mParamRange.put(id, map);
						Debug.d(TAG, "--->id=" + id + ", map=" + map);
					}
					break;
				default:
					break;
				}
				event = parser.next();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		/*编码器,有效值0,1*/
		
/*
		//参数1
		map = new HashMap<String, Integer>();
		map.put("min", 1);
		map.put("max", 65535);
		map.put("default", 1);
		mParamRange.put(1, map);

		//参数2
		map = new HashMap<String, Integer>();
		map.put("min", 0);
		map.put("max", 1);
		map.put("default", 0);
		mParamRange.put(2, map);

		//参数3
		map = new HashMap<String, Integer>();
		map.put("min", 150);
		map.put("max", 750);
		map.put("default", 150);
		mParamRange.put(3, map);

		//参数4
		map = new HashMap<String, Integer>();
		map.put("min", 0);
		map.put("max", 65535);
		map.put("default", 0);
		mParamRange.put(4, map);

		//参数5
		map = new HashMap<String, Integer>();
		map.put("min", 0);
		map.put("max", 2);
		map.put("default", 0);
		mParamRange.put(5, map);

		//参数6
		map = new HashMap<String, Integer>();
		map.put("min", 0);
		map.put("max", 1);
		map.put("default", 0);
		mParamRange.put(6, map);

		//参数7 
		map = new HashMap<String, Integer>();
		map.put("min", 1);
		map.put("max", 65535);
		map.put("default", 0);
		mParamRange.put(7, map);

		//参数8
		map = new HashMap<String, Integer>();
		map.put("min", 0);
		map.put("max", 1);
		map.put("default", 0);
		mParamRange.put(8, map);

		
		// param9
		map = new HashMap<String, Integer>();
		map.put("min", 1);
		map.put("max", 500);
		map.put("default", 1);
		mParamRange.put(9, map);
		
		// param10
		map = new HashMap<String, Integer>();
		map.put("min", 100);
		map.put("max", 20000);
		map.put("default", 100);
		mParamRange.put(10, map);
		
		// param11
		map = new HashMap<String, Integer>();
		map.put("min", 1);
		map.put("max", 65535);
		map.put("default", 1);
		mParamRange.put(11, map);
		
		// param12
		map = new HashMap<String, Integer>();
		map.put("min", 1);
		map.put("max", 1000);
		map.put("default", 1);
		mParamRange.put(12, map);
		
		// param13
		map = new HashMap<String, Integer>();
		map.put("min", 0);
		map.put("max", 1);
		map.put("default", 0);
		mParamRange.put(13, map);
		
		// param14
		map = new HashMap<String, Integer>();
		map.put("min", 0);
		map.put("max", 1);
		map.put("default", 0);
		mParamRange.put(14, map);
				
		// param15
		map = new HashMap<String, Integer>();
		map.put("min", 0);
		map.put("max", 1);
		map.put("default", 0);
		mParamRange.put(15, map);
				
		// param16
		map = new HashMap<String, Integer>();
		map.put("min", 0);
		map.put("max", 1);
		map.put("default", 0);
		mParamRange.put(16, map);
		
		// param17
		map = new HashMap<String, Integer>();
		map.put("min", 1);
		map.put("max", 4);
		map.put("default", 1);
		mParamRange.put(17, map);
		
		// param19
		map = new HashMap<String, Integer>();
		map.put("min", 1);
		map.put("max", 65535);
		map.put("default", 1);
		mParamRange.put(19, map);
				
		// param20
		map = new HashMap<String, Integer>();
		map.put("min", 1);
		map.put("max", 1000);
		map.put("default", 1);
		mParamRange.put(20, map);
				
		// param21
		map = new HashMap<String, Integer>();
		map.put("min", 0);
		map.put("max", 1);
		map.put("default", 0);
		mParamRange.put(21, map);
				
		// param22
		map = new HashMap<String, Integer>();
		map.put("min", 0);
		map.put("max", 1);
		map.put("default", 0);
		mParamRange.put(22, map);
				
		// param23
		map = new HashMap<String, Integer>();
		map.put("min", 0);
		map.put("max", 1);
		map.put("default", 0);
		mParamRange.put(23, map);
		
		// param24
		map = new HashMap<String, Integer>();
		map.put("min", 0);
		map.put("max", 1);
		map.put("default", 0);
		mParamRange.put(24, map);
		
		// param25
		map = new HashMap<String, Integer>();
		map.put("min", 0);
		map.put("max", 1);
		map.put("default", 0);
		mParamRange.put(25, map);
		
		// param26
		map = new HashMap<String, Integer>();
		map.put("min", 80);
		map.put("max", 120);
		map.put("default", 80);
		mParamRange.put(26, map);
		
		// param27
		map = new HashMap<String, Integer>();
		map.put("min", 0);
		map.put("max", 1);
		map.put("default", 0);
		mParamRange.put(27, map);
		// param28
		map = new HashMap<String, Integer>();
		map.put("min", 17);
		map.put("max", 24);
		map.put("default", 17);
		mParamRange.put(28, map);
				
		// param29
		map = new HashMap<String, Integer>();
		map.put("min", 200);
		map.put("max", 60000);
		map.put("default", 200);
		mParamRange.put(29, map);
		
		// param30
		map = new HashMap<String, Integer>();
		map.put("min", 0);
		map.put("max", 2000);
		map.put("default", 0);
		mParamRange.put(30, map);
*/		
		
	}
	
	public int checkParam(int param, int value) {
		if (mParamRange == null) {
			return value;
		}
		HashMap<String, Integer> p = mParamRange.get(param);
		if (p == null) {
			return value;
		}
		int min = p.get("min");
		int max = p.get("max");
		int def = p.get("default");
		Debug.d(TAG, "*************Param" + param + "************");
		Debug.d(TAG, "min=" + min + ", max=" + max +", default=" + def);
		if (value < min || value > max) {
			Debug.d(TAG, "resetTo:" + def);
			if (param == 29) {
				if ( value == 0) {
					return value;
				} else if (value > max) {
					return max;
				} else if (value < min) {
					return min;
				}
			}
			
			return def;
		}
		
		return value;
	}
	

	public int[] getParams() {
		return (int[])mParam;
	}

	public int getParam(int index) {
		if (index >= mParam.length) {
			return 0;
		}
//		Debug.d(TAG, "getParam ==> mParam[" + index + "]" + mParam[index]);
		return (int) mParam[index];
	}

	// H.M.Wang 2019-12-11 追加该函数，以满足计数器即串口设置内容与参数编辑区的同步
	public void setParamBroadcast(int index, int value) {
		setParam(index, value);

		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(SettingsListAdapter.ACTION_PARAM_CHANGED);
		broadcastIntent.putExtra(SettingsListAdapter.TAG_INDEX, index);
		broadcastIntent.putExtra(SettingsListAdapter.TAG_VALUE, String.valueOf(value));
		mContext.sendBroadcast(broadcastIntent);
	}

	public void setParam(int index, int value) {
		Debug.d(TAG, "--->index=" + index + ", value=" + value);
		if (index >= mParam.length) {
			return ;
		}
		mParam[index] = value;
// H.M.Wang 2022-3-21 由于实现方法做了修改，有apk获取相应管脚的状态后，设置是否对打印缓冲区进行反向操作，因此这里无需再通知驱动参数57的状态
// H.M.Wang 2021-9-24 追加输入设置参数
//		if(index == INDEX_IPURT_PROC) {
//			FpgaGpioOperation.setInputProc(value);
//		}
// End of H.M.Wang 2021-9-24 追加输入设置参数
// End of H.M.Wang 2022-3-21 由于实现方法做了修改，有apk获取相应管脚的状态后，设置是否对打印缓冲区进行反向操作，因此这里无需再通知驱动参数57的状态
	}

	public PrinterNozzle getPNozzle() {
		int index = (int) mParam[INDEX_HEAD_TYPE];

		// H.M.Wang 做一下修改
//        if (index > PrinterNozzle.MessageType.NOZZLE_INDEX_1_INCH_DUAL) {
//			return PrinterNozzle.MESSAGE_TYPE_12_7;
//		}
//		return PrinterNozzle.getInstance(index);

		PrinterNozzle nozzle = PrinterNozzle.MESSAGE_TYPE_12_7;

		switch(index) {
			case PrinterNozzle.MessageType.NOZZLE_INDEX_25_4:
				nozzle = PrinterNozzle.MESSAGE_TYPE_25_4;
				break;
			case PrinterNozzle.MessageType.NOZZLE_INDEX_38_1:
				nozzle = PrinterNozzle.MESSAGE_TYPE_38_1;
				break;
			case PrinterNozzle.MessageType.NOZZLE_INDEX_50_8:
				nozzle = PrinterNozzle.MESSAGE_TYPE_50_8;
				break;
			case PrinterNozzle.MessageType.NOZZLE_INDEX_1_INCH:
				nozzle = PrinterNozzle.MESSAGE_TYPE_1_INCH;
				break;
// H.M.Wang 2022-4-29 追加25.4x10头类型
			case PrinterNozzle.MessageType.NOZZLE_INDEX_254X10:
				nozzle = PrinterNozzle.MESSAGE_TYPE_254X10;
				break;
// End of H.M.Wang 2022-4-29 追加25.4x10头类型
			case PrinterNozzle.MessageType.NOZZLE_INDEX_1_INCH_DUAL:
				nozzle = PrinterNozzle.MESSAGE_TYPE_1_INCH_DUAL;
				break;
			case PrinterNozzle.MessageType.NOZZLE_INDEX_1_INCH_TRIPLE:
				nozzle = PrinterNozzle.MESSAGE_TYPE_1_INCH_TRIPLE;
				break;
			case PrinterNozzle.MessageType.NOZZLE_INDEX_1_INCH_FOUR:
				nozzle = PrinterNozzle.MESSAGE_TYPE_1_INCH_FOUR;
				break;
			case PrinterNozzle.MessageType.NOZZLE_INDEX_16_DOT:
				nozzle = PrinterNozzle.MESSAGE_TYPE_16_DOT;
				break;
			case PrinterNozzle.MessageType.NOZZLE_INDEX_32_DOT:
				nozzle = PrinterNozzle.MESSAGE_TYPE_32_DOT;
				break;
// H.M.Wang 2020-7-23 追加32DN打印头
			case PrinterNozzle.MessageType.NOZZLE_INDEX_32DN:
				nozzle = PrinterNozzle.MESSAGE_TYPE_32DN;
				break;
// End of H.M.Wang 2020-7-23 追加32DN打印头
// H.M.Wang 2020-8-14 追加32SN打印头
			case PrinterNozzle.MessageType.NOZZLE_INDEX_32SN:
				nozzle = PrinterNozzle.MESSAGE_TYPE_32SN;
				break;
// End of H.M.Wang 2020-8-14 追加32SN打印头
// H.M.Wang 2020-8-26 追加64SN打印头
			case PrinterNozzle.MessageType.NOZZLE_INDEX_64SN:
				nozzle = PrinterNozzle.MESSAGE_TYPE_64SN;
				break;
// End of H.M.Wang 2020-8-26 追加64SN打印头
// H.M.Wang 2022-10-19 追加64SLANT头
			case PrinterNozzle.MessageType.NOZZLE_INDEX_64SLANT:
				nozzle = PrinterNozzle.MESSAGE_TYPE_64SLANT;
				break;
// End of H.M.Wang 2022-10-19 追加64SLANT头
// H.M.Wang 2022-5-27 追加32x2头类型
			case PrinterNozzle.MessageType.NOZZLE_INDEX_32X2:
				nozzle = PrinterNozzle.MESSAGE_TYPE_32X2;
				break;
// End of H.M.Wang 2022-5-27 追加32x2头类型
			case PrinterNozzle.MessageType.NOZZLE_INDEX_64_DOT:
				nozzle = PrinterNozzle.MESSAGE_TYPE_64_DOT;
				break;
			case PrinterNozzle.MessageType.NOZZLE_INDEX_R6X48:
				nozzle = PrinterNozzle.MESSAGE_TYPE_R6X48;
				break;
			case PrinterNozzle.MessageType.NOZZLE_INDEX_R6X50:
				nozzle = PrinterNozzle.MESSAGE_TYPE_R6X50;
				break;
			case PrinterNozzle.MessageType.NOZZLE_INDEX_9MM:
				nozzle = PrinterNozzle.MESSAGE_TYPE_9MM;
				break;
// H.M.Wang 2021-3-6 追加E6X48,E6X50头
			case PrinterNozzle.MessageType.NOZZLE_INDEX_E6X48:
				nozzle = PrinterNozzle.MESSAGE_TYPE_E6X48;
				break;
			case PrinterNozzle.MessageType.NOZZLE_INDEX_E6X50:
				nozzle = PrinterNozzle.MESSAGE_TYPE_E6X50;
				break;
// End of H.M.Wang 2021-3-6 追加E6X48,E6X50头
			case PrinterNozzle.MessageType.NOZZLE_INDEX_E6X1:
				nozzle = PrinterNozzle.MESSAGE_TYPE_E6X1;
				break;
// H.M.Wang 2021-8-16 追加96DN头
			case PrinterNozzle.MessageType.NOZZLE_INDEX_96DN:
				nozzle = PrinterNozzle.MESSAGE_TYPE_96DN;
				break;
// End of H.M.Wang 2021-8-16 追加96DN头
// H.M.Wang 2021-8-25 追加E5X48和E5X50头类型
			case PrinterNozzle.MessageType.NOZZLE_INDEX_E5X48:
				nozzle = PrinterNozzle.MESSAGE_TYPE_E5X48;
				break;
			case PrinterNozzle.MessageType.NOZZLE_INDEX_E5X50:
				nozzle = PrinterNozzle.MESSAGE_TYPE_E5X50;
				break;
// End of H.M.Wang 2021-8-25 追加E5X48和E5X50头类型
		}

		return nozzle;
		// H.M.Wang 修改完
	}

	public void setCounters(int[] counters) {
		if (counters.length > 0) {
			mParam[INDEX_COUNT_1] = counters[0];
		}
		if (counters.length > 1) {
			mParam[INDEX_COUNT_2] = counters[1];
		}
		if (counters.length > 2) {
			mParam[INDEX_COUNT_3] = counters[2];
		}

		if (counters.length > 3) {
			mParam[INDEX_COUNT_4] = counters[3];
		}
		if (counters.length > 4) {
			mParam[INDEX_COUNT_5] = counters[4];
		}

		if (counters.length > 5) {
			mParam[INDEX_COUNT_6] = counters[5];
		}

		if (counters.length > 6) {
			mParam[INDEX_COUNT_7] = counters[6];
		}

		if (counters.length > 7) {
			mParam[INDEX_COUNT_8] = counters[7];
		}

		if (counters.length > 8) {
			mParam[INDEX_COUNT_9] = counters[8];
		}

		if (counters.length > 9) {
			mParam[INDEX_COUNT_10] = counters[9];
		}
	}
//	public int getHeads() {
//		int heads = 1;
//		Debug.d(TAG, "--->:getHeads: " + mParam[INDEX_HEAD_TYPE]);
//		switch (mParam[INDEX_HEAD_TYPE]) {
//		case MessageType.MESSAGE_TYPE_12_7:
//		case MessageType.MESSAGE_TYPE_12_7_S:
//		case MessageType.MESSAGE_TYPE_16_3:
//		case MessageType.MESSAGE_TYPE_1_INCH:
//		case MessageType.MESSAGE_TYPE_1_INCH_FAST:
//			heads = 1;
//			break;
//		case MessageType.MESSAGE_TYPE_1_INCH_DUAL:
//		case MessageType.MESSAGE_TYPE_1_INCH_DUAL_FAST:
//		case MessageType.MESSAGE_TYPE_25_4:
//		case MessageType.MESSAGE_TYPE_33:
//			heads = 2;
//			break;
//		case MessageType.MESSAGE_TYPE_38_1:
//			heads = 3;
//			break;
//		case MessageType.MESSAGE_TYPE_50_8:
//			heads = 4;
//			break;
//		case MessageType.MESSAGE_TYPE_16_DOT:
//			heads = 1;
//			break;
//		case MessageType.MESSAGE_TYPE_32_DOT:
//			heads = 1;
//			break;
//		case MessageType.MESSAGE_TYPE_NOVA:
//			heads = 6;
//			break;
//		default:
//			break;
//		}
//		return heads;
//	}

	/**
	 * 根据参数38计算真实的解锁数
	 * 1带多：
	 * 	 12，13，14....,18 分别表示 1带2，1带3，... , 1带8
	 * 2带多：
	 * 	24，26，28 分别表示 2带4，2带6，2带8
	 *
	 * 1带多，要根据情况计算解锁数量。1带2解两个锁，递推。
	 * 2带多，不变
	 * @return
	 */
	public int getHeadFactor() {
		int param = getParam(INDEX_ONE_MULTIPLE);
		if (param/20 > 0) {		// 2带多
			int f = (param%20)/2;
			return (f == 0 || f > 4) ? 1 : f;
		} else if (param/10 > 0) {
			return param%10;
		} else {
			// 2020-5-11
// H.M.Wang 2020-5-21 12.7R5头改为RX48，追加RX50头
//			if(getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_12_7_R5) {
			if(getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_R6X48 ||
				getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_R6X50) {
// End of H.M.Wang 2020-5-21 12.7R5头改为RX48，追加RX50头
				return PrinterNozzle.R6_HEAD_NUM;
			}

// H.M.Wang 2021-3-6 追加E6X48,E6X50头
			if( getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E6X48 ||
				getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E6X50) {
				return PrinterNozzle.E6_HEAD_NUM;
			}
// End of H.M.Wang 2021-3-6 追加E6X48,E6X50头
			if( getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E6X1) {
				return PrinterNozzle.E6_HEAD_NUM;
			}
// H.M.Wang 2021-8-25 追加E5X48和E5X50头类型
			if( getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E5X48 ||
					getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E5X50) {
				return PrinterNozzle.E5_HEAD_NUM;
			}
// End of H.M.Wang 2021-8-25 追加E5X48和E5X50头类型
			return 1;
		}
	}

	/**
	 * 根据参数38和喷头编号 @{head} 确定读取哪个信息的dotcount // 判断是1带多，还是2带多
	 *
	 * @return 1带多 返回1，2带多 返回2
	 */
	public int getMainHeads(int head) {
		int param = getParam(INDEX_ONE_MULTIPLE);
		if (param/20 > 0) {
			return head%2;
		} else if (param/10 > 0) {
			return 0;
		} else {
			// 2020-5-11
// H.M.Wang 2020-5-21 12.7R5头改为RX48，追加RX50头
//			if(getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_12_7_R5) {
			if(getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_R6X48 ||
				getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_R6X50) {
// End of H.M.Wang 2020-5-21 12.7R5头改为RX48，追加RX50头
				return (head < PrinterNozzle.R6_HEAD_NUM ? 0 : head);
			}
			// 2020-5-11
// H.M.Wang 2021-3-6 追加E6X48,E6X50头
			if( getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E6X48 ||
				getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E6X50) {
				return (head < PrinterNozzle.E6_HEAD_NUM ? 0 : head);
			}
// End of H.M.Wang 2021-3-6 追加E6X48,E6X50头
			if( getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E6X1) {
				return (head < PrinterNozzle.E6_HEAD_NUM ? 0 : head);
			}
// H.M.Wang 2021-8-25 追加E5X48和E5X50头类型
			if( getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E5X48 ||
					getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E5X50) {
				return (head < PrinterNozzle.E5_HEAD_NUM ? 0 : head);
			}
// End of H.M.Wang 2021-8-25 追加E5X48和E5X50头类型
			return head;
		}
	}
}