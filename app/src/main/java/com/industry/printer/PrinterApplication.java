package com.industry.printer;

import com.industry.printer.Serial.SerialPort;
import com.industry.printer.Utils.Configs;
import com.industry.printer.Utils.CrashCatcher;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.FileUtil;
import com.industry.printer.Utils.KZFileObserver;
import com.industry.printer.Utils.LibUpgrade;
import com.industry.printer.Utils.PlatformInfo;
import com.industry.printer.Utils.PreferenceConstants;
import com.industry.printer.data.NativeGraphicJni;
import com.industry.printer.hardware.Hp22mm;
import com.industry.printer.hardware.SmartCard;
import com.industry.printer.hardware.SmartCardManager;

import android.app.Application;
import java.io.DataOutputStream;
import java.io.File;

public class PrinterApplication extends Application {
	public static final String TAG="PrinterApplication";

	private KZFileObserver sysObserver;
	private KZFileObserver qrObserver;

	private static PrinterApplication sInstance = null;

	public static PrinterApplication getInstance() {
		return sInstance;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		CrashCatcher catcher = CrashCatcher.getInstance();
		catcher.init(getApplicationContext());
		registerFileListener();
		sInstance = this;
		asyncInit();

		try {
			if(PlatformInfo.isSmfyProduct()) {		// 只有A20版本才通过复制asset->system/lib的方式升级so文件，A133使用其他方式升级
				LibUpgrade libUp = new LibUpgrade();
				libUp.upgradeSOs();
			}
			NativeGraphicJni.loadLibrary();
			if(SmartCardManager.SMARTCARD_ACCESS) SmartCard.loadLibrary();
			SerialPort.loadLibrary();
			Hp22mm.loadLibrary();
		} catch (ExceptionInInitializerError e) {
			Debug.e(TAG, "--->e: " + e.getMessage());
		} catch (Exception e) {
			Debug.e(TAG, "--->e: " + e.getMessage());
		}
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
