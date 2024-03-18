package com.industry.printer.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import com.industry.printer.FileFormat.FilenameSuffixFilter;
import com.industry.printer.Socket_Server.Db.Server_Socket_Database;
import com.industry.printer.Utils.PlatformInfo;

import android.R.integer;
import android.graphics.Bitmap;
import android.graphics.Path;
import android.hardware.usb.UsbManager;

/**
 * ConfigPath 选择系统设置的保持路径
 * @author kevin
 *
 */
public class ConfigPath {
	
	private static final String TAG = ConfigPath.class.getSimpleName();
	
	private static ArrayList<String> mUsbPaths=null;
	
	public static ArrayList<String> getMountedUsb() {
		return mUsbPaths;
	}
	
	public static ArrayList<String> updateMountedUsb() {
		ArrayList<String> mPaths = new ArrayList<String>();
		Debug.d(TAG, "===>getMountedUsb");
		try {
			FileInputStream file = new FileInputStream(Configs.PROC_MOUNT_FILE);
			BufferedReader reader = new BufferedReader(new InputStreamReader(file));
			String line = reader.readLine();
			for(;line != null;) {
				Debug.d(TAG, "===>getMountUsb: " + line);
				if (!line.contains(PlatformInfo.getMntPath())) {
					line = reader.readLine();
					continue;
				}
				String items[] = line.split(" ");
				if (items == null || items.length < 2) {
					continue;
				}
				mPaths.add(items[1]);
				line = reader.readLine();
			}
			file.close();
			reader.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		mUsbPaths = mPaths;
		return mPaths;
	}
	
	/**
	 * 在插入的USB设备上创建系统所需的目录
	 * 根目录： system
	 */
	public static ArrayList<String> makeSysDirsIfNeed() {
		ArrayList<String> paths = getMountedUsb();
		if (paths == null || paths.size() <= 0) {
			return paths;
		}
		for (String path : paths) {
			File rootDir = new File(path+Configs.SYSTEM_CONFIG_DIR);
			if (rootDir.exists() && rootDir.isDirectory()) {
				File tlkDir = new File(path+Configs.TLK_FILE_SUB_PATH);
				if (tlkDir.exists() && tlkDir.isDirectory()) {
					
				} else {
					tlkDir.mkdirs();
				}
			} else {
				File tlkDir = new File(path+Configs.TLK_FILE_SUB_PATH);
				tlkDir.mkdirs();
			}
		}
		return paths;
	}
	
	public static String getFont() {
		ArrayList<String> paths = makeSysDirsIfNeed();
		if (paths == null || paths.size() <= 0) {
			return null;
		}
		return paths.get(0) + Configs.FONT_METRIC_PATH + Configs.FONT_16_16;
	}
	
	/**
	 * 获取当前TLK文件目录
	 * 如果有多个u盘挂载，则只使用第一个u盘
	 */
	
	public static String getTlkPath() {
		
		/*
		 * ArrayList<String> paths = makeSysDirsIfNeed();
		if (paths == null || paths.size() <= 0) {
			return null;
		}
		return paths.get(0)+Configs.TLK_FILE_SUB_PATH;
		*/
		//Db = new Server_Socket_Database(null);
		//wangjing
		// File tlkDir = new File(Configs.TLK_PATH_FLASH+"/"+Configs.Devic_Number_Path+"/");
		File tlkDir = new File(Configs.TLK_PATH_FLASH);
		if (!tlkDir.exists() || !tlkDir.isDirectory()) {
			tlkDir.mkdirs();
		}
		//Fill_Infor(Configs.Devic_Number_Path,Configs.TLK_PATH_FLASH+"/"+Configs.Devic_Number_Path+"/"+Configs.Devic_File_Path+"/");
		// Configs.Device_Printer_Path=Configs.Devic_Number_Path+"/"+Configs.Devic_File_Path+"/";
		// return  Configs.TLK_PATH_FLASH+"/"+Configs.Devic_Number_Path+"/";
		return  Configs.TLK_PATH_FLASH;
	}
	
	public static String getTxtPath() {
		ArrayList<String> paths = getMountedUsb();
		if (paths == null || paths.size() <= 0) {
			return null;
		}
		return paths.get(0)+Configs.TXT_FILES_PATH;
	}
	
	
	/**
	 * 通过MessageTask的name构造出tlk文件夹的路径
	 * @param name
	 * @return
	 */
	public static String getTlkDir(String name) {
		return getTlkPath() + "/" + name;
	}
	
	/**
	 * 通过MessageTask的name构造出tlk文件的路径
	 * @param name
	 * @return
	 */
	public static String getTlkAbsolute(String name) {
		return getTlkPath() + "/" + name + Configs.TLK_FILE_NAME;
	}
	
	/**
	 * 通过MessageTask的name构造出1.bin文件的路径
	 * @param name
	 * @return
	 */
	public static String getBinAbsolute(String name) {
		return getTlkPath() + "/" + name + Configs.BIN_FILE_NAME;
	}
	
	/**
	 * 通过MessageTask的name构造出vx.bin文件的路径
	 * @param name
	 * @return
	 */
	public static String getVBinAbsolute(String name, int index) {
		return getTlkPath() + "/" + name + "/v" + String.valueOf(index) + ".bin";
	}
	
	public static String getUpgradePath() {
		ArrayList<String> paths = getMountedUsb();
		if (paths == null || paths.size() <= 0) {
			return null;
		}
		return paths.get(0) + Configs.UPGRADE_APK_FILE;
	}

// H.M.Wang 2024-1-3 追加ko的升级功能
	public static String getKoPath(final String ko) {
		ArrayList<String> paths = getMountedUsb();
		if (paths == null || paths.size() <= 0) {
			return null;
		}

// H.M.Wang 2024-3-12 将升级ko的方法，从使用USB根目录下的固定.ko文件名，改为使用KKK_xxxxx.ko的可变名称格式
// 如： FPGA的ko，从fpga-sunxi.ko改为SPI_xxxxx.ko
//      GPIO的ko，从ext_gpio.ko.ko改为GPIO_xxxxx.ko
//      触屏的ko，从gslX680.ko改为LCD_xxxxx.ko
//      RTC的ko，从rtc-ds1307.ko.ko改为RTC_xxxxx.ko
// 并且，每个ko文件都有一个相同文件名（但扩展名位.txt）的相随文件，该文件中保存有该ko文件的md5值
// 所有这些文件保存在根目录下的KOupdate目录下
		File path = new File(paths.get(0) + File.separator + Configs.KOUPDATE_FOLDER);
		String files[] = path.list(new FilenameFilter() {
			@Override
			public boolean accept(File file, String s) {
				if(s.startsWith(ko)) return true;
				return false;
			}
		});
		if(null != files && files.length > 0) return path.getAbsoluteFile() + File.separator + files[0]; else return null;
//		return paths.get(0) + File.separator + ko;
// End of H.M.Wang 2024-3-12 将升级ko的方法，从使用USB根目录下的固定.ko文件名，改为使用KKK_xxxxx.ko的可变名称格式
	}
// End of H.M.Wang 2024-1-3 追加ko的升级功能

// H.M.Wang 2024-3-12 FPGA固件的升级文件改为从USB根目录下的FWupdate目录下，读取FW_xxxxx.bin的形式的文件，同时伴随有一个FW_xxxxx.txt的MD5校验文件
	public static String getFWUpgradePath() {
		ArrayList<String> paths = getMountedUsb();
		if (paths == null || paths.size() <= 0) {
			return null;
		}

		File path = new File(paths.get(0) + File.separator + Configs.FWUPDATE_FOLDER);
		String files[] = path.list(new FilenameFilter() {
			@Override
			public boolean accept(File file, String s) {
				if(s.startsWith(Configs.PREFIX_FW)) return true;
				return false;
			}
		});
		if(null != files && files.length > 0) return path.getAbsoluteFile() + File.separator + files[0]; else return null;
	}
// End of H.M.Wang 2024-3-12 FPGA固件的升级文件改为从USB根目录下的FWupdate目录下，读取FW_xxxxx.bin的形式的文件，同时伴随有一个FW_xxxxx.txt的MD5校验文件

	public static String getPictureDir() {
//		ArrayList<String> paths = getMountedUsb();
//		if (paths == null || paths.size() <= 0) {
//			return null;
//		}
		return Configs.CONFIG_PATH_FLASH + Configs.PICTURE_SUB_PATH;
	}
}
