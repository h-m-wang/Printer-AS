package com.industry.printer.hardware;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.industry.printer.R;
import com.industry.printer.R.string;
import com.industry.printer.Utils.Configs;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.PlatformInfo;

public class LRADCBattery {

	private static final String TAG = LRADCBattery.class.getSimpleName();
	
	/*LRADC Battery 驱动的系统文件目录*/
	private static final String SYSFS_BATTERY_LRADC_PATH = "/sys/class/power_supply/battery-lradc/";

	/*power now属性对应的文件*/
	private static final String SYSFS_BATTERY_LRADC_POWERNOW =  SYSFS_BATTERY_LRADC_PATH + "power_now";

	public static int getPower() {
		int state = 0;

		if(PlatformInfo.isA133Product()) {
// H.M.Wang 2025-12-30 修改A133的电池存量数据的获取方法
			state = ExtGpio.getBatteryVolume();
			Debug.d(TAG, "Battery = " + state);
			if (state >= 90) {
				state = 41;			// 100%
			} else if (state >= 70) {
				state = 38;			// 75%
			} else if (state >= 40) {
				state = 36;			// 50%
			} else if (state >= 20) {
				state = 35;			// 25%
			} else if (state >= 10) {
				state = 33;			// 0%
			} else if (state >= 0) {
				state = 20;			// 0%
			}
// End of H.M.Wang 2025-12-30 修改A133的电池存量数据的获取方法
		} else {
			String power = readSysfs(SYSFS_BATTERY_LRADC_POWERNOW);
			if (power == null) {
				return 0;
			}
			try {
				state = Integer.parseInt(power);
			} catch (Exception e) {}
		}

		return state;
	}
	
	
	public static boolean writeSysfs(String file, String content) {
		FileOutputStream mStream = null;
		try {
			mStream = new FileOutputStream(file);
			mStream.write(content.getBytes());
			mStream.flush();
			mStream.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static String readSysfs(String file) {
		if (file == null) {
			return null;
		}
		
		try {
			FileReader fr = new FileReader(file);
			BufferedReader mReader = new BufferedReader(fr);
			String content = mReader.readLine();
			mReader.close();
			fr.close();
			return content;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
