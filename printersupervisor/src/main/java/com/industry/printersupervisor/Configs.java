package com.industry.printersupervisor;


import android.content.Context;
import android.graphics.Bitmap;

public class Configs {

	public static boolean DEBUG=true;

	public static final String PROC_MOUNT_FILE = "/proc/mounts";
	public static  final String CONFIG_PATH_FLASH = "/mnt/sdcard";
	
	public static final String UPGRADE_APK_FILE = "/Printer.apk";

	public static final String SYSTEM_CONFIG_DIR = "/system";

// H.M.Wang 2023-11-2 追击F1和F2的定义，这两个文件用来管理apk是否正确升级。具体的使用方法参照PackageInstaller类中的说明
	public static final String FILE_1 = CONFIG_PATH_FLASH + SYSTEM_CONFIG_DIR + "/F1.txt";
	public static final String FILE_2 = CONFIG_PATH_FLASH + SYSTEM_CONFIG_DIR + "/F2.txt";
// End of H.M.Wang 2023-11-2 追击F1和F2的定义，这两个文件用来管理apk是否正确升级。具体的使用方法参照PackageInstaller类中的说明
}
