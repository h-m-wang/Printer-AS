package com.industry.printersupervisor;

import java.lang.reflect.Method;


//import android.os.SystemProperties;

/**
 * 系统属性调用SystemProperties是隐藏的{hide}无法直接调用
 * 因此通过类反射机制调用
 * 
 * 各个产品区分方式：
 * 1. 树莓32位系统： (mProduct = PRODUCT_SMFY_SUPER3) + fpga-sunxi-32.ko
 * 2. 树莓HP系统： (mProduct = PRODUCT_SMFY_SUPER3) + fpga-sunxi-hp.ko
 * 3. 4412系统： mProduct = 
 * @author zhaotongkai
 *
 */

public class PlatformInfo {

	private static final String TAG = PlatformInfo.class.getSimpleName();
	
	private static final String PROPERTY_PRODUCT = "ro.build.product";
	private static final String PROPERTY_INK_DEVICE = "ro.product.inkdevice";

// H.M.Wang 2021-4-16 追加机器类型码的取得和显示
	private static final String PROPERTY_BUILD_ID = "ro.build.id";
	private static final String PROPERTY_BUILD_VERSION_INC = "ro.build.version.incremental";
// End of H.M.Wang 2021-4-16 追加机器类型码的取得和显示

	// H.M.Wang 2021-4-16 追加机器类型码的取得和显示
	public static final String DEVICE_SMARTCARD = "smartcard";
// H.M.Wang 2024-11-3 这个值有误，修改为新值
	public static final String PRODUCT_SMFY_SUPER3 = "smfy-super3";		// "smfy_super3"
// End of H.M.Wang 2024-11-3 这个值有误，修改为新值
// H.M.Wang 2024-11-3 A133 的ro.build.product
	public static final String PRODUCT_CERES_C3 = "ceres-c3";
// End H.M.Wang 2024-11-3 A133 的ro.build.product
	public static final String PRODUCT_3INCH = "3inch";
	public static final String PRODUCT_7INCH = "7inch";
	public static final String PRODUCT_FRIENDLY_4412 = "tiny4412";
	
	/**
	 * The Serial Used for RFID device
	 * 4412平台: /dev/ttySAC2
	 * 树莓平台： /dev/ttyS3 
	 */
	private static final String RFID_SERIAL_4412 = "/dev/ttySAC3";
	private static final String RFID_SERIAL_SMFY = "/dev/ttyS3";
// H.M.Wang 2024-11-3 A133的rfid串口
	private static final String RFID_SERIAL_A133 = "/dev/ttyS7";
// End of H.M.Wang 2024-11-3 A133的rfid串口
// H.M.Wang 2025-5-17 A133-M2004的rfid串口
	private static final String RFID_SERIAL_A133M2 = "/dev/ttyS2";
// End of H.M.Wang 2025-5-17 A133-M2004的rfid串口

	private static final String GRAFT_SERIAL_4412 = "/dev/ttySAC2";
	private static final String GRAFT_SERIAL_SMFY = "/dev/ttyS2";
	
	/**
	 * usb mount paths
	 */
	// 4412
	public static final String USB_MOUNT_PATH_4412 = "/storage/usbdisk";
	// smfy
	public static final String USB_MOUNT_PATH_SMFY = "/mnt/usb";
// H.M.Wang 2024-11-4 A133的USB路径
	public static final String USB_MOUNT_PATH_A133 = "/mnt/media_rw";		// 目录下直接包含USB的子目录，可以直接listfile来查看
// End of H.M.Wang 2024-11-4 A133的USB路径

	/* 大屏全編輯 */
	public static final int LARGE_SCREEN = 1;
	/* 小屏部分編輯 */
	public static final int SMALL_SCREEN_PART = 2;
	/* 小屏全編輯 - 按鈕合併 */
	public static final int SMALL_SCREEN_FULL = 3;
	
	/* 通過點陣字庫獲取buffer */
	public static int DotMatrixType = 0;
	
	/* 通过该常量来区分硬件平台 */
// H.M.Wang 2024-11-4 由于增加了A133，因此不能设初始值，必须首先从硬件读取初值
//	private static String mProduct = PRODUCT_SMFY_SUPER3;
	private static String mProduct = "";
// End of H.M.Wang 2024-11-4 由于增加了A133，因此不能设初始值，必须首先从硬件读取初值

	private static String mInkDevice = null;

	public static void init() {
		// mProduct = getProduct();
	}
	
	/**
	 * 判断buffer获取方式，通过BMP图片提取或者点阵字库提取
	 * 目前支持点阵字库提取的设备为：树莓3
	 * 其他设备都是通过BMP提取
	 * @return
	 */
	public static final int isBufferFromDotMatrix() {
		
		return DotMatrixType;
	}
	
	public static String getProduct() {
		// return SystemProperties.get(PROPERTY_PRODUCT);
		//String product = null;
		if(!StringUtil.isEmpty(mProduct)) {
			return mProduct;
		}
		try {
			Class<?> mClassType = Class.forName("android.os.SystemProperties");
			Method mGetMethod = mClassType.getDeclaredMethod("get", String.class);
			mProduct = (String) mGetMethod.invoke(mClassType, PROPERTY_PRODUCT);
		} catch (Exception e) {
			Debug.d(TAG, "Exception: " + e.getMessage());
		}
		Debug.d(TAG, "===>product: " + mProduct);
		return mProduct;
	}

	public static boolean isFriendlyProduct() {
		if (PRODUCT_FRIENDLY_4412.equalsIgnoreCase(getProduct())) {
			return true;
		}
		return false;
	}

	public static boolean isSmfyProduct() {
		if (PRODUCT_SMFY_SUPER3.equalsIgnoreCase(getProduct())) {
			return true;
		}
		return false;
	}

	public static boolean isA133Product() {
		if (PRODUCT_CERES_C3.equalsIgnoreCase(getProduct())) {
			return true;
		}
		return false;
	}

	public static String getMntPath() {
		if (isFriendlyProduct()) {
			return USB_MOUNT_PATH_4412;
		} else if (isSmfyProduct()) {
			return USB_MOUNT_PATH_SMFY;
// H.M.Wang 2024-11-3 A133获取串口
		} else if (isA133Product()) {
			return USB_MOUNT_PATH_A133;
// End of H.M.Wang 2024-11-3 A133获取串口
		} else {
			Debug.d(TAG, "unsupported platform right now");
			Debug.d(TAG, "use 4412 as default");
		}
		return USB_MOUNT_PATH_4412;
	}

}
