package com.industry.printer.Utils;

import java.lang.reflect.Method;
import java.security.PublicKey;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

import com.industry.printer.hardware.FpgaGpioOperation;
import com.industry.printer.hardware.SmartCard;


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
	private static final String RFID_SERIAL_A133 = "/dev/ttyS7";	// 20241103
// End of H.M.Wang 2024-11-3 A133的rfid串口

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

	public static boolean isMImgType(String imgUC) {
		return (imgUC.startsWith("M9") || imgUC.startsWith("M7") || imgUC.startsWith("M5") || imgUC.startsWith("4FIFO") || imgUC.startsWith("BAGINK") || imgUC.startsWith("22MM"));
	}

// H.M.Wang 2021-4-16 追加机器类型码的取得和显示
	public static String getImgUniqueCode() {
		try {
			Class<?> mClassType = Class.forName("android.os.SystemProperties");
			Method mGetMethod = mClassType.getDeclaredMethod("get", String.class);
			String buildID = (String) mGetMethod.invoke(mClassType, PROPERTY_BUILD_ID);
			String verInc = (String) mGetMethod.invoke(mClassType, PROPERTY_BUILD_VERSION_INC);
			String ret = "";
			if("JDQ39".equals(buildID)) {
				ret = "OLD-" + verInc;
			} else {
//				ret = buildID.substring(0,2) + "xx" + verInc.substring(1);
// H.M.Wang 2022-12-21 追加一个从FPGA驱动中获取FPGA版本号的调用
//				ret = buildID + verInc + getFPGAVersion(buildID);
				ret = buildID + verInc + getFPGAVersion(buildID) + "-" + String.format("%03d", (FpgaGpioOperation.getDriverVersion() % 1000));
// End of H.M.Wang 2022-12-21 追加一个从FPGA驱动中获取FPGA版本号的调用
			}
//			Debug.d(TAG, "===>Img Unique Code: " + ret);
			return ret;
		} catch (Exception e) {
			Debug.d(TAG, "Exception: " + e.getMessage());
		}
		return "";
	}
// End of H.M.Wang 2021-4-16 追加机器类型码的取得和显示

// H.M.Wang 2022-12-21 追加一个从FPGA驱动中获取FPGA版本号的调用
// 算法(2进制描述)
// 	(1) 从FPGA收到
//		 A000 0000 1111 1111 CDEF FHIJ KLMN OPQR
//	(2)	移位
//		 0000 0001 1111 111C DEFF HIJK LMNO PQRA
//  (3) bank = DEFF HIJ
//      code = K LMNO PQRA
//	4b009000
//  0100 1011 0000 0000 1001 0000 0000 0000
//  1001 0110 0000 0001 0010 0000 0000 0000 (G000)

//	d0000b00
//  1101 0000 0000 0000 0000 1011 0000 0000
//  1010 0000 0000 0000 0001 0110 0000 0001 (B001)

	public static String getFPGAVersion(String buildID) {
		int fpgaVersion = FpgaGpioOperation.getFPGAVersion();

//		Debug.d(TAG, "FPGA Version = " + String.format("%08x", fpgaVersion));

		if(fpgaVersion == 0) return "";

		int bit31 = ((fpgaVersion & 0x80000000) == 0x80000000 ? 1 : 0);

		fpgaVersion = ((fpgaVersion << 1) | bit31);

// H.M.Wang 2023-11-12 暂时变更4FIFO版本号的取位规则
		int bank;
		int code;
		if (buildID.startsWith("4FIFO") || buildID.startsWith("22MM")) {
			bank = (int)((fpgaVersion & 0x7FFF0000) >> 25);
			code = (int)((fpgaVersion & 0x01FF0000) >> 16);
		} else {
			bank = (int)((fpgaVersion & 0x00007FFF) >> 9);
			code = (int)(fpgaVersion & 0x000001FF);
		}
//		int bank = (int)((fpgaVersion & 0x00007FFF) >> 9);
//		int code = (int)(fpgaVersion & 0x000001FF);
// End of H.M.Wang 2023-11-12 暂时变更4FIFO版本号的取位规则

		String codeStr = "000" + code;
		codeStr = codeStr.substring(codeStr.length()-3, codeStr.length());

		String bankStr = "";
		if(bank >= 0 && bank <= 9) {
			bankStr = String.format("%c", 0x30 + bank);
		} else if(bank >= 10 && bank <= 35) {
			bankStr = String.format("%c", 'A' + bank-10);
		} else if(bank >= 36 && bank <= 61) {
			bankStr = String.format("%c", 'a' + bank-36);
		} else if(bank == 62) {
			bankStr = "#";
		} else if(bank == 63) {
			bankStr = "*";
		}

		return (bankStr + codeStr);
	}
// End of H.M.Wang 2022-12-21 追加一个从FPGA驱动中获取FPGA版本号的调用
	/**
	 * read system property
	 * @return
	 */
// H.M.Wang 2020-11-17 暂时恢复原来的方法
// H.M.Wang 2020-11-15 修改InkDevice的确定方法，不适用build.prop获取，而是根据SmartCard的初始化错误信息来判断
	public static String getInkDevice() {
		// return SystemProperties.get(PROPERTY_PRODUCT);
		//String product = null;
		if(!StringUtil.isEmpty(mInkDevice)) {
			return mInkDevice;
		}
		try {
			Class<?> mClassType = Class.forName("android.os.SystemProperties");
			Method mGetMethod = mClassType.getDeclaredMethod("get", String.class);
			mInkDevice = (String) mGetMethod.invoke(mClassType, PROPERTY_INK_DEVICE);
		} catch (Exception e) {
			Debug.d(TAG, "Exception: " + e.getMessage());
		}
		Debug.d(TAG, "===>InkDevice: " + mInkDevice);
		return mInkDevice;
	}

/*
	public static String getInkDevice() {
		if(null == mInkDevice) {
			int ret = SmartCard.exist();
			if(SmartCard.SC_INIT_HOST_CARD_NOT_PRESENT == ret) {
				mInkDevice = "";
			} else {
				mInkDevice = DEVICE_SMARTCARD;
			}
		}
		return mInkDevice;
	}
*/
// H.M.Wang 2020-11-15 修改InkDevice的确定方法，不适用build.prop获取，而是根据SmartCard的初始化错误信息来判断

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

// H.M.Wang 2024-11-3 A133判断产品平台是否位ceres-c3
	public static boolean isA133Product() {
		if (PRODUCT_CERES_C3.equalsIgnoreCase(getProduct())) {
			return true;
		}
		return false;
	}
// End of H.M.Wang 2024-11-3 A133判断产品平台是否位ceres-c3

	/**
	 * RFID device connected Serial Port
	 */
	public static String getRfidDevice() {
		if (isFriendlyProduct()) {
			return RFID_SERIAL_4412;
		} else if (isSmfyProduct()) {
			return RFID_SERIAL_SMFY;
// H.M.Wang 2024-11-3 A133获取串口
		} else if (isA133Product()) {
			return RFID_SERIAL_A133;
// End of H.M.Wang 2024-11-3 A133获取串口
		} else {
			Debug.d(TAG, "unsupported platform right now");
		}
		return null;
	}
	
	/**
	 * Graft device connected Serial Port
	 */
	// H.M.Wang 2024-11-3 该函数已经实际上不再使用了
	public static String getGraftDevice() {
		if (isFriendlyProduct()) {
			return GRAFT_SERIAL_4412;
		} else if (isSmfyProduct()) {
			return GRAFT_SERIAL_SMFY;
		} else {
			Debug.d(TAG, "unsupported platform right now");
		}
		return null;
	}
	
	/**
	 * usb storage device mounted path
	 * @return
	 */
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
	
	public static int getEditType() {
		return SMALL_SCREEN_FULL;
	}
	public static int  SetDotMatrixType(int Type) {
// H.M.Wang 2021-4-17 如果DotMatrixType==1，则很多地方会走DotMatrix路径，有时会造成死机，因为DotMatrix都不用了，所以废止该变量的使用，=2的情况也没有被用到
//		DotMatrixType=Type;
// End of H.M.Wang 2021-4-17 如果DotMatrixType==1，则很多地方会走DotMatrix路径，有时会造成死机，因为DotMatrix都不用了，所以废止该变量的使用，=2的情况也没有被用到
		 return 0;
	}
	
	/** 
     * 获取版本名 
     *  
     * @param context 
     * @return 获取失败则返回null 
     */  
    public static String getVersionName(Context context) {  
        // 包管理者  
        PackageManager mg = context.getPackageManager();  
        try {  
            // getPackageInfo(packageName 包名, flags 标志位（表示要获取什么数据）);  
            // 0表示获取基本数据  
            PackageInfo info = mg.getPackageInfo(context.getPackageName(), 0);  
            return info.versionName;  
        } catch (NameNotFoundException e) {  
            e.printStackTrace();  
        }  
        return null;  
    }  
    /** 
     * 获取版本号 
     *  
     * @param context 
     * @return 获取失败则返回0 
     */  
    public static int getVersionCode(Context context) {  
        // 包管理者  
        PackageManager mg = context.getPackageManager();  
        try {  
            // getPackageInfo(packageName 包名, flags 标志位（表示要获取什么数据）);  
            // 0表示获取基本数据  
            PackageInfo info = mg.getPackageInfo(context.getPackageName(), 0);  
            return info.versionCode;  
        } catch (NameNotFoundException e) {  
            e.printStackTrace();  
        }  
        return 0;  
    }  
}
