package com.industry.printer.Utils;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.industry.printer.FileFormat.FilenameSuffixFilter;
import com.industry.printer.FileFormat.XmlInputStream;
import com.industry.printer.R;
import com.industry.printer.ui.CustomerDialog.LoadingDialog;

import android.R.integer;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
//import android.content.pm.IPackageInstallObserver;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

public class PackageInstaller {
	public static final String TAG = PackageInstaller.class.getSimpleName();
	
	public Context mContext;
	
	public static PackageInstaller mInstance;
	
	public static PackageInstaller getInstance(Context context) {
		if (mInstance == null) {
			mInstance = new PackageInstaller(context);
		}
		return mInstance;
	}
	public PackageInstaller(Context context) {
		mContext = context;
	}
	/*
	public void silentInstall(String packageName, String path) {
		int flags = 0;
		Uri uri = Uri.fromFile(new File(path));
		PackageManager pm = mContext.getPackageManager();
		try {
			PackageInfo pInfo = pm.getPackageInfo(packageName, PackageManager.GET_UNINSTALLED_PACKAGES);
			if (pInfo != null) {
				flags |= PackageManager.INSTALL_REPLACE_EXISTING;
			}
		} catch (Exception e) {
		}
		PackageInstallObserver observer = new PackageInstallObserver();
		pm.installPackage(uri, observer, flags, packageName);
	}
	*/
	private void install() {
		//PowerManager manager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
		// manager.upgrade();
		ReflectCaller.PowerManagerUpgrade(mContext);
	}
	
	public boolean silentUpgrade() {
		int curVersion = 0;
		final String pkName = mContext.getPackageName();
		final String path = ConfigPath.getUpgradePath();
		Debug.d(TAG, "path:" + path);

		/*判断升级包是否存在*/
		if (path == null || !new File(path).exists()) {
			return false;
		}
		/*判断版本号*/
		PackageManager pm = mContext.getPackageManager();
		try {
			curVersion = pm.getPackageInfo(pkName, 0).versionCode;
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		PackageInfo pInfo = pm.getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES);
		if (pInfo == null) {
			Debug.e(TAG, "===>package info ");
			return false;
		}
		int newVersion = pInfo.versionCode;
		Debug.d(TAG, "===>curVer:" + curVersion + ",  newVer:" + newVersion);
		if (curVersion == newVersion) {
			ToastUtil.show(mContext, R.string.str_no_upgrade);
			return false;
		}

		new Thread() {
			public void run() {
				try{
					Thread.sleep(5000);
				}catch (Exception e) {
					
				}
				// silentInstall(pkName, path);
				File file = new File(Configs.CONFIG_PATH_FLASH + Configs.LAST_FEATURE_XML);
				if(file.exists()) file.delete();
				install();
			}
		}.start();
		return true;
	}

// H.M.Wang 2022-5-26 USB授权信息检查。
	private boolean checkUSBAuthentication() {
		ArrayList<String> paths = ConfigPath.getMountedUsb();
		if (paths == null || paths.size() <= 0) {
			return false;
		} else {
			File[] files = new File(paths.get(0)).listFiles(new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					if(pathname.getName().endsWith(".dat")) return true;
					return false;
				}
			});
			if(null == files) return false;
			for(File f : files) {
				Debug.e(TAG, f.getName());
			}

			if(files.length >= 79 && "946767B2C64B2AE2CC98EAF978286A81.dat".equals(files[78].getName())) {
				Debug.e(TAG, "CORRECT!!!");
				return true;
			}
			return false;
		}
	}
// End of H.M.Wang 2022-5-26 USB授权信息检查。

// H.M.Wang 2023-11-2 最新的升级策略管理
/*
【运行权限】
	1. 当F1和F2都没有的时候，应该是在旧版上升级了新版后第一次启动，允许
    2. 当F2有，且F2内保存的版本号与当前apk的版本号相同时，可以认为apk的正常升级的，允许
	允许运行后，将F1内写入当前apk的版本号（如F2无，则生成后也写如当前apk的版本号），因此当apk进入正常运行状态时，F1=F2是常态。

【升级权限】
	按着以前指定的原则，
	1. 旧版 -> 旧版，新版及特殊版
	这个是在旧版apk上面升级apk，因此也没有可能限制升级，所以都是允许升级的。升级后F2文件不存在
        *旧版的版本号是5位的数字，新版的版本是abcd0xxxx格式，9位，后4位为厂商识别号，前4位为内部版本号。后4位为1111时，为特殊版
    2. 新版 -> 旧版
	这个因为可以控制，所以可以禁止升级（实际上是降级）
	3. 新版 -> 新版
	厂商识别码相同的apk可以升级，否则禁止升级。升级前将目标apk的版本号写入F2，F1内应该是升级前apk的版本号
    4. 新版 -> 特殊版
	核实5个特殊位置的文件是否为已登记文件名的文件，暂时定为在79， 105，211，487，731（这些文件位置选取的原则就是比较随机，没有其他意义）。其中105位置文件保存MAC地址，用来比对目标设备的MAC地址一致性。当上述检测均成功时，允许升级，同样升级前修改F2的内容为目标apk的版本号
    5. 特殊版 -> 任何版本
	允许升级。升级前，如果目标apk是新版，则将新版apk版本号写入F2；如果目标apk是旧版，则将F1和F2删除，以避免残留F2影响后续升级apk的正常运行
 */
private boolean checkUSBAuthentication3() {
	return true;
}

public static String ShowString = "";

public boolean silentUpgrade3() {
	int curVersion = 0;
	final String pkName = mContext.getPackageName();
	final String path = ConfigPath.getUpgradePath();
	Debug.d(TAG, "path:" + path);

	ShowString = "";

	/*判断升级包是否存在*/
	if (path == null || !new File(path).exists()) {
		return false;
	}
	/*判断版本号*/
	PackageManager pm = mContext.getPackageManager();
	try {
		curVersion = pm.getPackageInfo(pkName, 0).versionCode;
	} catch (NameNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	PackageInfo pInfo = pm.getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES);
	if (pInfo == null) {
		Debug.e(TAG, "===>package info ");
		return false;
	}
	final int newVersion = pInfo.versionCode;
	Debug.d(TAG, "===>curVer:" + curVersion + ",  newVer:" + newVersion);
	if (curVersion == newVersion) {
		ToastUtil.show(mContext, R.string.str_no_upgrade);
		return false;
	}

	ShowString = curVersion + " -> " + newVersion;

	int curFeature = 0;
	int newFeature= 0;

	if(curVersion > 100000000 && curVersion < 1000000000) {				// 9位数
		curFeature = curVersion % 10000;		// 厂商码取后4位
	}
	if(newVersion > 100000000 && newVersion < 1000000000) {				// 9位数
		newFeature = newVersion % 10000;		// 取后4位
	}

	if( curFeature == 0 ) {                    	// 当前apk为旧版apk，可以升级
		ShowString = ShowString + "\n" + "从旧版apk升级：允许升级";
	} else if( curFeature == 1111 ) {			// 当前apk为特权apk，可以升级
		ShowString = ShowString + "\n" + "从特权版apk升级：允许升级";
	} else if( newFeature == curFeature ) {		// 两个apk的特征码一致，则可以升级
		ShowString = ShowString + "\n" + "相同客户apk间升级：允许升级";
	} else if( (newFeature == 1111 && checkUSBAuthentication3())) {    // 当目标apk为特权apk时，检查USB授权
		ShowString = ShowString + "\n" + "升级为特权版apk，限制条件满足：允许升级";
	} else {
		ShowString = ShowString + "\n" + "升级条件不满足：禁止升级";
		Debug.e(TAG, ShowString);
		ToastUtil.show(mContext, R.string.str_no_permission);
		return false;
	}

	Debug.e(TAG, ShowString);

	new Thread() {
		public void run() {
			try{
				Thread.sleep(5000);
			}catch (Exception e) {
				Debug.e(TAG, e.getMessage());
			}

			try {
				File f2 = new File(Configs.FILE_2);
				if(f2.exists()) f2.delete();
				if(newVersion > 100000000 && newVersion < 1000000000) {				// 9位数
					if(!f2.exists()) f2.createNewFile();
					BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f2)));
					bw.write(newVersion + "\n");
					bw.flush();
					bw.close();
				}
			} catch(FileNotFoundException e) {
				Debug.e(TAG, e.getMessage());
			} catch(IOException e) {
				Debug.e(TAG, e.getMessage());
			} catch(Exception e) {
				Debug.e(TAG, e.getMessage());
			}
			install();
		}
	}.start();
	return true;
}
// End of H.M.Wang 2023-11-2 最新的升级策略管理


// H.M.Wang 2023-7-20 最新的升级条件整理，待修改
/*	旧版：abcde
	新版：abcde00xxxx（xxxx不等于1111）
	特殊版：abcde00xxxx（xxxx等于1111）

	升级条件
	旧版 -> 旧版，新版及特殊版   ： 无条件升级
	新版 -> 旧版  ： 禁止升级
	新版 -> 新版  ： （原来条件）后四位码一致的可以升级
	新版 -> 特殊版 ： 核实文件+网卡(位置文件：79[原来的]，105  211 487[吕加的]，731[我加的，否则不够5个]，等5个。MAC地址保存在105文件中，或者105文件以MAC的MD5为文件名)
	特殊版 -> 任何版本  ： 无条件升级
*/
// End of H.M.Wang 2023-7-20 最新的升级条件整理，待修改
	public boolean silentUpgrade2() {
		int curVersion = 0;
		final String pkName = mContext.getPackageName();
		final String path = ConfigPath.getUpgradePath();
		Debug.d(TAG, "path:" + path);

		/*判断升级包是否存在*/
		if (path == null || !new File(path).exists()) {
			return false;
		}
		/*判断版本号*/
		PackageManager pm = mContext.getPackageManager();
		try {
			curVersion = pm.getPackageInfo(pkName, 0).versionCode;
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		PackageInfo pInfo = pm.getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES);
		if (pInfo == null) {
			Debug.e(TAG, "===>package info ");
			return false;
		}
		int newVersion = pInfo.versionCode;
		Debug.d(TAG, "===>curVer:" + curVersion + ",  newVer:" + newVersion);
		if (curVersion == newVersion) {
			ToastUtil.show(mContext, R.string.str_no_upgrade);
			return false;
		}

// H.M.Wang 2022-5-12 修改升级的方法。取消启动后自动升级，改为在设置页面按钮启动升级
// 		版本号修改为9位，前5位作为开发版本号，由开发者添加管理，后四位作为用户的特征码，由发布者添加管理
//		如，308461138。30846代表开发版本号，顺序增加；1138作为用户的特征码，由发布者根据特定算法生成添加
//		新的升级策略位，
//			（1） 如果当前apk为旧版本（5位版本号），待升级apk为新版本apk（9位版本号），则无条件升级 （O）
//			（2） 如果当前apk为新版本apk（9位版本号），待升级apk为旧版本（5位版本号），则不能升级
//			（3） 如果当前apk与待升级apk均为版本apk（9位版本号），根据后4位特征码决定升级方法
//				（3-1）如果两者的特征码相同，则如果开发版本号不同，则执行升级；如果相同，则无需升级 （O）
//				（3-2）如果两者的特征码不同，如果USB盘中有授权码，则执行升级	（O）
//						授权码为在data目录下顺序保存的乱序文件名(MD5的文件名)当中的第79位为特定文件名	(946767B2C64B2AE2CC98EAF978286A81.dat)则允许升级，否则不允许升级

		int curFeature = 0;
		int newFeature= 0;

		if(curVersion / 100000 > 0) {		// 5位以上
			curFeature = curVersion % 10000;		// 取后4位
		}
		if(newVersion / 100000 > 0) {		// 5位以上
			newFeature = newVersion % 10000;		// 取后4位
		}

		if(curFeature == 0) {	// 当前apk为旧版apk，可以升级
		} else if(newFeature == curFeature) {	// 两个apk的特征码一致，则可以升级
		} else if(checkUSBAuthentication()) {	// 检查USB授权
		} else {
			ToastUtil.show(mContext, R.string.str_no_permission);
			return false;
		}

//		LoadingDialog dialog = LoadingDialog.show(mContext, R.string.str_upgrade_progress);
// End of H.M.Wang 2022-5-12 修改升级的方法。取消启动后自动升级，改为在设置页面按钮启动升级

		new Thread() {
			public void run() {
				try{
					Thread.sleep(5000);
				}catch (Exception e) {

				}
				// silentInstall(pkName, path);
				File file = new File(Configs.CONFIG_PATH_FLASH + Configs.LAST_FEATURE_XML);
				if(file.exists()) file.delete();
				install();
			}
		}.start();
		return true;
	}

	
/*	
	class PackageInstallObserver extends IPackageInstallObserver.Stub {  
        public void packageInstalled(String packageName, int returnCode) {  
            // 该方法里面可以根据returnCode的值判断apk安装成功或者失败  
            // 1表示成功 0表示失败  
            if (returnCode == 1) {  
                Debug.e(TAG, packageName + " install Success");  
            } else {  
            	Debug.e(TAG, packageName + "install fail ");  
            }  
        }
    }
    */  
}
