package com.industry.printer.hardware;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

import com.industry.printer.R;
import com.industry.printer.R.string;
import com.industry.printer.Utils.Configs;
import com.industry.printer.Utils.PlatformInfo;

public class LRADCBattery {

	private static final String TAG = LRADCBattery.class.getSimpleName();
	
	/*LRADC Battery 驱动的系统文件目录*/
	private static final String SYSFS_BATTERY_LRADC_PATH = "/sys/class/power_supply/battery-lradc/";
// H.M.Wang 2024-11-4 A133的电池存量数据路径
	private static final String SYSFS_BATTERY_AXP2202_PATH = "/sys/class/power_supply/axp2202-battery/";
// End of H.M.Wang 2024-11-4 A133的电池存量数据路径

	/*power now属性对应的文件*/
	private static final String SYSFS_BATTERY_LRADC_POWERNOW =  SYSFS_BATTERY_LRADC_PATH + "power_now";
// H.M.Wang 2024-11-4 A133的电池存量数据路径
	private static final String SYSFS_BATTERY_AXP2202_CAPACITY = SYSFS_BATTERY_AXP2202_PATH + "capacity";
// End of H.M.Wang 2024-11-4 A133的电池存量数据路径

	public static int getPower() {
// H.M.Wang 2024-11-4 A133的电池存量数据
//		String power = readSysfs(SYSFS_BATTERY_LRADC_POWERNOW);
		String power = readSysfs(PlatformInfo.isA133Product() ? SYSFS_BATTERY_AXP2202_CAPACITY : SYSFS_BATTERY_LRADC_POWERNOW);
// End of H.M.Wang 2024-11-4 A133的电池存量数据
		if (power == null) {
			return 0;
		}
		int state = 0;
		try {
			state = Integer.parseInt(power);
// H.M.Wang 2024-11-4 A133的电池存量数据。调整至A20是的刻度，以方便留用原来的显示模块
			if(PlatformInfo.isA133Product()) {
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
			}
// End of H.M.Wang 2024-11-4 A133的电池存量数据
		} catch (Exception e) {

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
