package com.industry.printer;

import com.industry.printer.Serial.SerialPort;
import com.industry.printer.Utils.Configs;
import com.industry.printer.Utils.CrashCatcher;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.FileUtil;
import com.industry.printer.Utils.KZFileObserver;
import com.industry.printer.Utils.PreferenceConstants;
import com.industry.printer.Utils.ToastUtil;
import com.industry.printer.data.NativeGraphicJni;
import com.industry.printer.hardware.Hp22mm;
import com.industry.printer.hardware.SmartCard;
import com.industry.printer.hardware.SmartCardManager;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class PrinterApplication extends Application {
	public static final String TAG="PrinterApplication";

	private KZFileObserver sysObserver;
	private KZFileObserver qrObserver;

	private static PrinterApplication sInstance = null;

	public static PrinterApplication getInstance() {
		return sInstance;
	}

	private boolean upgradeFpgaSunxiKO(DataOutputStream os) {
		boolean ret = false;
		InputStream is = null;

		try {
			File file = new File("/system/vendor/modules/" + Configs.FPGA_SUNXI_KO);
			AssetManager assetManager = sInstance.getAssets();
			is = assetManager.open(Configs.FPGA_SUNXI_KO);

			if(file.length() != is.available()) {
				Debug.d(TAG, "chmod 777 /system/vendor/modules/" + Configs.FPGA_SUNXI_KO);
				os.writeBytes("chmod 777 /system/vendor/modules/" + Configs.FPGA_SUNXI_KO);
				os.flush();
				Thread.sleep(5000);

				FileUtil.writeFile("/system/vendor/modules/" + Configs.FPGA_SUNXI_KO, is);
				Thread.sleep(5000);

				Debug.d(TAG, "chmod 644 /system/vendor/modules/" + Configs.FPGA_SUNXI_KO);
				os.writeBytes("chmod 644 /system/vendor/modules/" + Configs.FPGA_SUNXI_KO);
				os.flush();
				Thread.sleep(5000);

				ret = true;
			}
		} catch(Exception e) {
			Debug.e(TAG, e.getMessage());
		} finally {
			try{if(null != is) is.close();}catch(IOException e){}
		}

		return ret;
	}

	private boolean upgradeHardwareSO(DataOutputStream os) {
		boolean ret = false;
		InputStream is = null;

		try {
			File file = new File("/system/lib/" + Configs.HARDWARE_SO);
			AssetManager assetManager = sInstance.getAssets();
			is = assetManager.open(Configs.HARDWARE_SO);

			if(file.length() != is.available()) {
// H.M.Wang 2020-12-26 追加硬件库复制功能
				Debug.d(TAG, "chmod 777 /system/lib/" + Configs.HARDWARE_SO);
				os.writeBytes("chmod 777 /system/lib/" + Configs.HARDWARE_SO);
				Thread.sleep(100);

				FileUtil.writeFile("/system/lib/" + Configs.HARDWARE_SO, is);
// End of H.M.Wang 2020-12-26 追加硬件库复制功能
				ret = true;
			}
		} catch(Exception e) {
			Debug.e(TAG, e.getMessage());
		} finally {
			try{if(null != is) is.close();}catch(IOException e){}
		}
		return ret;
	}

	private boolean upgradeNativeGraphicSO(DataOutputStream os) {
		boolean ret = false;
		InputStream is = null;

		try {
			File file = new File("/system/lib/" + Configs.NATIVEGRAPHIC_SO);
			AssetManager assetManager = sInstance.getAssets();
			is = assetManager.open(Configs.NATIVEGRAPHIC_SO);

			if(file.length() != is.available()) {
				Debug.d(TAG, "chmod 777 /system/lib/" + Configs.NATIVEGRAPHIC_SO);
				os.writeBytes("chmod 777 /system/lib/" + Configs.NATIVEGRAPHIC_SO);
				Thread.sleep(100);

				FileUtil.writeFile("/system/lib/" + Configs.NATIVEGRAPHIC_SO, is);
				ret = true;
			}
		} catch(Exception e) {
			Debug.e(TAG, e.getMessage());
		} finally {
			try{if(null != is) is.close();}catch(IOException e){}
		}
		return ret;
	}

	private boolean upgradeSmartCardSO(DataOutputStream os) {
		boolean ret = false;
		InputStream is = null;

		try {
			File file = new File("/system/lib/" + Configs.SMARTCARD_SO);
			AssetManager assetManager = sInstance.getAssets();
			is = assetManager.open(Configs.SMARTCARD_SO);

			if(file.length() != is.available()) {
				Debug.d(TAG, "chmod 777 /system/lib/" + Configs.SMARTCARD_SO);
				os.writeBytes("chmod 777 /system/lib/" + Configs.SMARTCARD_SO);
				Thread.sleep(100);

				FileUtil.writeFile("/system/lib/" + Configs.SMARTCARD_SO, is);
				ret = true;
			}
		} catch(Exception e) {
			Debug.e(TAG, e.getMessage());
		} finally {
			try{if(null != is) is.close();}catch(IOException e){}
		}
		return ret;
	}

	private boolean upgradeSerialSO(DataOutputStream os) {
		boolean ret = false;
		InputStream is = null;

		try {
			File file = new File("/system/lib/" + Configs.SERIAL_SO);
			AssetManager assetManager = sInstance.getAssets();
			is = assetManager.open(Configs.SERIAL_SO);

			if(file.length() != is.available()) {
				Debug.d(TAG, "chmod 777 /system/lib/" + Configs.SERIAL_SO);
				os.writeBytes("chmod 777 /system/lib/" + Configs.SERIAL_SO);
				Thread.sleep(100);

				FileUtil.writeFile("/system/lib/" + Configs.SERIAL_SO, is);
				ret = true;
			}
		} catch(Exception e) {
			Debug.e(TAG, e.getMessage());
		} finally {
			try{if(null != is) is.close();}catch(IOException e){}
		}
		return ret;
	}

	private boolean upgradeHp22mmSO(DataOutputStream os) {
		boolean ret = false;
		InputStream is = null;

		try {
			File file = new File("/system/lib/" + Configs.HP22MM_SO);
			AssetManager assetManager = sInstance.getAssets();
			is = assetManager.open(Configs.HP22MM_SO);

			if(file.length() != is.available()) {
				Debug.d(TAG, "chmod 777 /system/lib/" + Configs.HP22MM_SO);
				os.writeBytes("chmod 777 /system/lib/" + Configs.HP22MM_SO);
				Thread.sleep(100);

				FileUtil.writeFile("/system/lib/" + Configs.HP22MM_SO, is);
				ret = true;
			}
		} catch(Exception e) {
			Debug.e(TAG, e.getMessage());
		} finally {
			try{if(null != is) is.close();}catch(IOException e){}
		}
		return ret;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		CrashCatcher catcher = CrashCatcher.getInstance();
		catcher.init(getApplicationContext());
		registerFileListener();
		sInstance = this;
		asyncInit();

		// H.M.Wang 增加一个线程，用来将so文件拷贝到/system/lib目录下
//		new Thread() {
//			@Override
//			public void run() {
				try {
					boolean needReboot = false;

				Process process = Runtime.getRuntime().exec("su");
					DataOutputStream os = new DataOutputStream(process.getOutputStream());
					Thread.sleep(100);

/*							Debug.d(TAG, "chmod 777 /system/vendor/modules/" + Configs.FPGA_SUNXI_KO);
							os.writeBytes("chmod 777 /system/vendor/modules/" + Configs.FPGA_SUNXI_KO);
							os.flush();
							Thread.sleep(10000);

//							FileUtil.writeFile("/system/vendor/modules/" + Configs.FPGA_SUNXI_KO, is);
//							Thread.sleep(5000);

							Debug.d(TAG, "chmod 644 /system/vendor/modules/" + Configs.FPGA_SUNXI_KO);
							os.writeBytes("chmod 644 /system/vendor/modules/" + Configs.FPGA_SUNXI_KO);
							os.flush();
							Thread.sleep(5000);
					needReboot = true;
*/
//					needReboot |= upgradeFpgaSunxiKO(os);
					needReboot |= upgradeHardwareSO(os);
					needReboot |= upgradeNativeGraphicSO(os);
					needReboot |= upgradeSmartCardSO(os);
					needReboot |= upgradeSerialSO(os);
					needReboot |= upgradeHp22mmSO(os);

					if(needReboot) {
						Debug.e(TAG, "Reboot!!!");
						os.writeBytes("reboot\n");
					}
					os.flush();
					os.close();

					NativeGraphicJni.loadLibrary();
					if(SmartCardManager.SMARTCARD_ACCESS) SmartCard.loadLibrary();
					SerialPort.loadLibrary();
					Hp22mm.loadLibrary();
				} catch (ExceptionInInitializerError e) {
					Debug.e(TAG, "--->e: " + e.getMessage());
				} catch (Exception e) {
					Debug.e(TAG, "--->e: " + e.getMessage());
				}
//			}
//		}.start();
	}

	private void registerFileListener() {
		// system_config.xml
		sysObserver = new KZFileObserver(this, Configs.CONFIG_PATH_FLASH + Configs.SYSTEM_CONFIG_DIR);
		qrObserver = new KZFileObserver(this, Configs.QR_DIR);
		sysObserver.startWatching();
		qrObserver.startWatching();
	}

	/**
	 * pause listening
	 */
	public void pauseFileListener() {
		sysObserver.stopWatching();
	}

	/**
	 * resume listening
	 */
	public void resumeFileListener() {
		sysObserver.startWatching();
	}

	public void registerCallback(String path, KZFileObserver.KZFileObserverInterface callback) {
		sysObserver.registerCallback(path, callback);
//		sysObserver.registerCallback(Configs.QR_DATA,callback);
	}

	public void registeQRCallback(String path, KZFileObserver.KZFileObserverInterface callback) {
		qrObserver.registerCallback(path, callback);
//		sysObserver.registerCallback(Configs.QR_DATA,callback);
	}

	private void asyncInit() {
		long version = PreferenceConstants.getLong(this, PreferenceConstants.LAST_VERSION_CODE);

		Debug.d(TAG, "BuildConfig.VERSION_CODE: " + BuildConfig.VERSION_CODE);
		Debug.d(TAG, "version: " + version);

		if (BuildConfig.VERSION_CODE == version) {
			return;
		}
		new Thread() {
			@Override
			public void run() {
				File font = new File(Configs.FONT_DIR);

				if (font.exists()) {
					FileUtil.deleteFolder(Configs.FONT_DIR);
				}
				font.mkdirs();
				FileUtil.releaseAssets(sInstance, "fonts", Configs.CONFIG_PATH_FLASH);
				PreferenceConstants.putLong(sInstance, PreferenceConstants.LAST_VERSION_CODE, BuildConfig.VERSION_CODE);
			}
		}.start();
	}
}
