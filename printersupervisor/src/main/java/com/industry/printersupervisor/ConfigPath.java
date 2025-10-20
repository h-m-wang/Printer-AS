package com.industry.printersupervisor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.util.ArrayList;

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
//		Debug.d(TAG, "===>getMountedUsb");
		try {
			FileInputStream file = new FileInputStream(Configs.PROC_MOUNT_FILE);
			BufferedReader reader = new BufferedReader(new InputStreamReader(file));
			String line = reader.readLine();
			for (; line != null; ) {
//				Debug.d(TAG, "===>getMountUsb: " + line);
				String items[] = line.split(" ");
				if (items != null && items.length >= 2) {
					if(items[1].startsWith(PlatformInfo.getMntPath())) mPaths.add(items[1]);
//					Debug.d(TAG, "===>getMountUsb: " + line);
				}
				line = reader.readLine();
			}
			file.close();
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		Debug.d(TAG, "===>mPaths: " + mPaths.toString() + "; Size: " + mPaths.size());
		mUsbPaths = mPaths;
		return mPaths;
	}
	
	/**
	 * 在插入的USB设备上创建系统所需的目录
	 * 根目录： system
	 */
	public static String getUpgradePath() {
		ArrayList<String> paths = getMountedUsb();
		if (paths == null || paths.size() <= 0) {
			return null;
		}
		return paths.get(0);
	}
}
