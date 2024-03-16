package com.industry.printer.hardware;

import java.lang.reflect.Method;

import android.app.AlarmManager;
import android.content.Context;
import android.graphics.Bitmap.Config;

import com.industry.printer.Utils.Configs;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.ReflectCaller;
import com.industry.printer.Utils.SystemFs;

/**
 * RTC地址分配说明
 * 0x08 ~ 0x0C   打印计数及校验和 共5Bytes
 * 0x0f 		 用于RTC的I2C地址校验
 * 0x10 ~ 0x37   保存0-9计数器值
 */
public class RTCDevice {
	public static native int open(String dev);
	public static native int close(int fd);
	public static native void syncSystemTimeToRTC(int fd);	//write
	public static native void syncSystemTimeFromRTC(int fd); //read

	private static final String TAG = RTCDevice.class.getSimpleName();
	public static final String RTC_DEV = "/dev/rtc1"; 

	//I2C debug
	private final String I2C_READ = "/sys/class/device_of_i2c/read";
	private final String I2C_WRITE = "/sys/class/device_of_i2c/write";
	private final String I2C_DEVICE = "/sys/class/device_of_i2c/device";

	private static int I2C_ADDRESS = 1;
	private static int I2C_CHECK_VALUE = 0x55;
	private final String[] ADDRESS = {"0x10", "0x14", "0x18", "0x1C", "0x20", "0x24", "0x28", "0x2C", "0x30", "0x34"};
// H.M.Wang 2023-9-20 追加一个读写计数细分值的功能，地址为0x3c
	private final static String SUB_STEP_REGISTER = "0x3c";
// End of H.M.Wang 2023-9-20 追加一个读写计数细分值的功能，地址为0x3c
// H.M.Wang 2020-5-15 QRLast移植RTC的0x38地址保存
	private final static String QRLAST_REGISTER = "0x38";
// End of H.M.Wang 2020-5-15 QRLast移植RTC的0x38地址保存

	public static RTCDevice mInstance = null;
	
	public static RTCDevice getInstance(Context context) {
		if (mInstance == null) {
			mInstance = new RTCDevice(context);
		}
		return mInstance;
	}
	
	public RTCDevice(Context context) {
		
		SystemFs.writeSysfs(I2C_DEVICE, "1,0x68");
		// 自动探测RTC的I2C地址
		String cmdWR = "0x0f,0x55";
		SystemFs.writeSysfs(I2C_WRITE, cmdWR);
		String cmdRD = "1,0x0f";
		SystemFs.writeSysfs(I2C_READ, cmdRD);
		String out = SystemFs.readSysfs(I2C_READ);
		if (out==null) {
			return ;
		}
		Debug.d(TAG, "--->NVRAM init out = " + out);

		String[] bytes = out.split("/r/n");
		if (bytes == null || bytes.length < 4) {
			return ;
		}
		int pos = bytes[3].lastIndexOf("0x");

		byte check = (byte) Integer.parseInt(bytes[3].substring(pos+2), 16);

		Debug.d(TAG, "--->NVRAM init out = " + String.valueOf(check));

		if (check == I2C_CHECK_VALUE) {

			I2C_ADDRESS = 1;
		} else {
			I2C_ADDRESS = 2;
		}
		Debug.d(TAG, "--->NVRAM init out = " + I2C_ADDRESS);
	}
	
	public void initSystemTime(Context context) {
		// AlarmManager aManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		// aManager.HwToSystemClock();
		ReflectCaller.AlarmManagerHwToSystemClock(context);
	}
	
	public void syncSystemTimeToRTC(Context context) {
		// AlarmManager aManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		// aManager.SystemClockToHw();
		ReflectCaller.AlarmManagerSystemClockToHw(context);
	}
	
	/**
	 * 
	 */
	public boolean isFirst() {
		
		return false;
	}
	/**
	 * 总计数为8位10进制数字，保存在DS1338的NVRAM中的08H～0CH 4个字节中
	 */
	public void writeCounter(Context context, int count) {
		byte byte0 = (byte) (count & 0x0ff);
		byte byte1 = (byte) ((count >> 8) & 0x0ff);
		byte byte2 = (byte) ((count >> 16) & 0x0ff);
		byte byte3 = (byte) ((count >> 24) & 0x0ff);
		byte checkSum = (byte) ((byte0 + byte1 + byte2 + byte3) & 0x0ff);
		String cmd = "0x08," + Integer.toHexString(byte0) + ","
					+ Integer.toHexString(byte1) + "," + Integer.toHexString(byte2) + "," 
					+ Integer.toHexString(byte3) + "," + Integer.toHexString(checkSum);
		SystemFs.writeSysfs(I2C_WRITE, cmd);
	}
	
	/**
	 * 总计数为8位10进制数字，保存在DS1338的NVRAM中的08H～0CH 4个字节中
	 */
	public int readCounter(Context context) {
		SystemFs.writeSysfs(I2C_DEVICE, getAddress());

		SystemFs.writeSysfs(I2C_READ, "5,0x08");
		int index=0;
		String out = SystemFs.readSysfs(I2C_READ);
		if (out==null) {
			return 0;
		}
		Debug.d(TAG, "--->NVRAM out = " + out);
		String[] bytes = out.split("/r/n");
		if (bytes == null || bytes.length < 8) {
			return 0;
		}
		Debug.d(TAG, "---> NVRAM Counter <---");
		Debug.d(TAG, bytes[3]);
		Debug.d(TAG, bytes[4]);
		Debug.d(TAG, bytes[5]);
		Debug.d(TAG, bytes[6]);
		Debug.d(TAG, bytes[7]);
		Debug.d(TAG, "---> NVRAM Counter end<---");
		index = bytes[3].lastIndexOf("0x");
		//int byte1 = Integer.parseInt("123", 16);
		byte byte1 = (byte) Integer.parseInt(bytes[3].substring(index+2), 16);
		byte byte2 = (byte) Integer.parseInt(bytes[4].substring(index+2), 16);
		byte byte3 = (byte) Integer.parseInt(bytes[5].substring(index+2), 16);
		byte byte4 = (byte) Integer.parseInt(bytes[6].substring(index+2), 16);
		byte byte5 = (byte) Integer.parseInt(bytes[7].substring(index+2), 16);
		int count = (byte1 & 0x0ff) + (byte2 & 0x0ff) * 256 + (byte3 & 0x0ff) * 256 * 256 + (byte4 & 0x0ff) * 256 * 256 * 256;

		//检查校验和
		byte checksum = (byte) ((byte1 + byte2 + byte3 + byte4) & 0x0ff);
		if (checksum == byte5) {
			return count;
		}
		return 0;
	}

	/**
	 * 10个计数器值从0x10H ~ 0x27H, 每个计数器值占用4个字节
	 * @param count
	 * @param index
	 */

	public void write(long count, int index) {
// H.M.Wang 2023-9-20 函数化读写整形数的功能
		if(index >= 0 && index < 10) writeInt(ADDRESS[index], (int)count);
/*
		Debug.d(TAG, "--->NVRAM written count[" + index + "] : " + count);
		StringBuilder cmd = new StringBuilder(ADDRESS[index]);
		byte byte0 = (byte) (count & 0x0ff);
		byte byte1 = (byte) ((count >> 8) & 0x0ff);
		byte byte2 = (byte) ((count >> 16) & 0x0ff);
		byte byte3 = (byte) ((count >> 24) & 0x0ff);
		cmd.append(",");
		cmd.append("0x" + Integer.toHexString(byte0));
		cmd.append(",");
		cmd.append("0x" + Integer.toHexString(byte1));
		cmd.append(",");
		cmd.append("0x" + Integer.toHexString(byte2));
		cmd.append(",");
		cmd.append("0x" + Integer.toHexString(byte3));
		SystemFs.writeSysfs(I2C_WRITE, cmd.toString());
*/
// End of H.M.Wang 2023-9-20 函数化读写整形数的功能
	}

	/**
	 * 10个计数器值从0x10H ~ 0x27H, 每个计数器值占用4个字节
	 * @param count
	 */
	public void writeAll(long[] count) {
		SystemFs.writeSysfs(I2C_DEVICE, getAddress());
		for (int i = 0; i < count.length; i++) {
			Debug.d(TAG, "--->NVRAM written count[" + i + " : " + count[i]);
// H.M.Wang 2023-9-20 函数化读写整形数的功能
			if(i < 10)  writeInt(ADDRESS[i], (int)count[i]);
/*			StringBuilder cmd = new StringBuilder(ADDRESS[i]);
			byte byte0 = (byte) (count[i] & 0x0ff);
			byte byte1 = (byte) ((count[i] >> 8) & 0x0ff);
			byte byte2 = (byte) ((count[i] >> 16) & 0x0ff);
			byte byte3 = (byte) ((count[i] >> 24) & 0x0ff);
			cmd.append(",");
			cmd.append("0x" + Integer.toHexString(byte0));
			cmd.append(",");
			cmd.append("0x" + Integer.toHexString(byte1));
			cmd.append(",");
			cmd.append("0x" + Integer.toHexString(byte2));
			cmd.append(",");
			cmd.append("0x" + Integer.toHexString(byte3));
			Debug.i(TAG, "--->writeAll: " + cmd);
			SystemFs.writeSysfs(I2C_WRITE, cmd.toString());
 */
// End of H.M.Wang 2023-9-20 函数化读写整形数的功能
		}
		Debug.i(TAG, "--->writeAll: ok");
//		SystemFs.writeSysfs(I2C_WRITE, cmd.toString());

	}

	public long read(int index) {
// H.M.Wang 2023-9-20 函数化读写整形数的功能
		SystemFs.writeSysfs(I2C_DEVICE, getAddress());
		return (long)((index >= 0 && index < 10) ? readInt(ADDRESS[index]) : 0);
/*
		String cmd = "4," + ADDRESS[index];
		SystemFs.writeSysfs(I2C_DEVICE, getAddress());
		SystemFs.writeSysfs(I2C_READ, cmd);
		String out = SystemFs.readSysfs(I2C_READ);
		if (out==null) {
			return 0;
		}
		int pos =0;
		String[] bytes = out.split("/r/n");
		if (bytes == null || bytes.length < 7) {
			return 0;
		}
		pos = bytes[3].lastIndexOf("0x");
		Debug.d(TAG, "--->NVRAM out = " + out);
		byte byte1 = (byte) Integer.parseInt(bytes[3].substring(pos+2), 16);
		byte byte2 = (byte) Integer.parseInt(bytes[4].substring(pos+2), 16);
		byte byte3 = (byte) Integer.parseInt(bytes[5].substring(pos+2), 16);
		byte byte4 = (byte) Integer.parseInt(bytes[6].substring(pos+2), 16);

		long count = (byte1 & 0x0ff) + (byte2 & 0x0ff) * 256 + (byte3 & 0x0ff) * 256 * 256 + (byte4 & 0x0ff) * 256 * 256 * 256;
		Debug.d(TAG, "--->NVRAM read count = " + count);
		return count;
 */
// End of H.M.Wang 2023-9-20 函数化读写整形数的功能
	}

	public long[] readAll() {
		long[] counts = new long[10];

		for (int i = 0; i < counts.length; i++) {
			counts[i] = read(i);
			Debug.d(TAG, "--->readAllcounts[  " + i + "]= " + counts[i]);
		}
		return counts;
	}

	private String getAddress() {
		return I2C_ADDRESS + ",0x68";
	}

// H.M.Wang 2020-5-15 QRLast移植RTC的0x38地址保存
	public int readQRLast() {
		return readInt(QRLAST_REGISTER);
	}

	public void writeQRLast(int count) {
		writeInt(QRLAST_REGISTER, count);
	}
// End of H.M.Wang 2020-5-15 QRLast移植RTC的0x38地址保存

// H.M.Wang 2023-9-20 追加一个读写计数细分值的功能，地址为0x3c
	public int readSubStep() {
		return readInt(SUB_STEP_REGISTER);
	}

	public void writeSubStep(int substep) {
		writeInt(SUB_STEP_REGISTER, substep);
	}
// End of H.M.Wang 2023-9-20 追加一个读写计数细分值的功能，地址为0x3c

// H.M.Wang 2023-9-20 函数化读写整形数的功能
	private int readInt(String reg) {
		SystemFs.writeSysfs(I2C_DEVICE, getAddress());

		String cmd = "4," + reg;

		SystemFs.writeSysfs(I2C_READ, cmd);
		String out = SystemFs.readSysfs(I2C_READ);
		if (out == null) {
			return 0;
		}
		Debug.d(TAG, "--->Read 4 int from register " + reg + ": " + out);

		int pos =0;
		String[] bytes = out.split("/r/n");
		if (bytes == null || bytes.length < 7) {
			return 0;
		}
		pos = bytes[3].lastIndexOf("0x");
		byte byte1 = (byte) Integer.parseInt(bytes[3].substring(pos+2), 16);
		byte byte2 = (byte) Integer.parseInt(bytes[4].substring(pos+2), 16);
		byte byte3 = (byte) Integer.parseInt(bytes[5].substring(pos+2), 16);
		byte byte4 = (byte) Integer.parseInt(bytes[6].substring(pos+2), 16);

		int count = (byte1 & 0x0ff) + (byte2 & 0x0ff) * 256 + (byte3 & 0x0ff) * 256 * 256 + (byte4 & 0x0ff) * 256 * 256 * 256;

		Debug.d(TAG, "--->Read int = " + count);

		return count;
	}

	private void writeInt(String reg, int count) {
		SystemFs.writeSysfs(I2C_DEVICE, getAddress());

		StringBuilder cmd = new StringBuilder(reg);
		Debug.d(TAG, "--->Writing int(" + count + ") to register " + reg);

		byte byte0 = (byte) (count & 0x0ff);
		byte byte1 = (byte) ((count >> 8) & 0x0ff);
		byte byte2 = (byte) ((count >> 16) & 0x0ff);
		byte byte3 = (byte) ((count >> 24) & 0x0ff);
		cmd.append(",");
		cmd.append("0x" + Integer.toHexString(byte0));
		cmd.append(",");
		cmd.append("0x" + Integer.toHexString(byte1));
		cmd.append(",");
		cmd.append("0x" + Integer.toHexString(byte2));
		cmd.append(",");
		cmd.append("0x" + Integer.toHexString(byte3));

		SystemFs.writeSysfs(I2C_WRITE, cmd.toString());
	}
// End of H.M.Wang 2023-9-20 函数化读写整形数的功能
}
