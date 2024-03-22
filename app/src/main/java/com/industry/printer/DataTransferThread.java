package com.industry.printer;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.industry.printer.Constants.Constants;
import com.industry.printer.FileFormat.PackageListReader;
import com.industry.printer.FileFormat.QRReader;
import com.industry.printer.FileFormat.SystemConfigFile;
import com.industry.printer.PHeader.PrinterNozzle;
import com.industry.printer.Rfid.IInkScheduler;
import com.industry.printer.Rfid.InkSchedulerFactory;
import com.industry.printer.Rfid.N_RfidScheduler;
import com.industry.printer.Rfid.RfidScheduler;
import com.industry.printer.Serial.EC_DOD_Protocol;
import com.industry.printer.Serial.Scaner2Protocol;
import com.industry.printer.Serial.SerialHandler;
import com.industry.printer.Serial.SerialProtocol;
import com.industry.printer.Serial.SerialProtocol7;
import com.industry.printer.Serial.SerialProtocol8;
import com.industry.printer.Utils.ByteArrayUtils;
import com.industry.printer.Utils.Configs;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.FileUtil;
import com.industry.printer.Utils.PlatformInfo;
import com.industry.printer.Utils.ToastUtil;
import com.industry.printer.data.BinCreater;
import com.industry.printer.data.BufferRebuilder;
import com.industry.printer.data.DataTask;
import com.industry.printer.data.NativeGraphicJni;
import com.industry.printer.data.PC_FIFO;
import com.industry.printer.data.TxtDT;
import com.industry.printer.hardware.BarcodeScanParser;
import com.industry.printer.hardware.FpgaGpioOperation;
import com.industry.printer.hardware.IInkDevice;
import com.industry.printer.hardware.InkManagerFactory;
import com.industry.printer.hardware.SmartCardManager;
import com.industry.printer.interceptor.LogIntercepter;
import com.industry.printer.object.BarcodeObject;
import com.industry.printer.object.BaseObject;
import com.industry.printer.object.CounterObject;
import com.industry.printer.object.DynamicText;
import com.industry.printer.object.HyperTextObject;
import com.industry.printer.ui.CustomerDialog.RemoteMsgPrompt;

import org.apache.http.util.ByteArrayBuffer;

/**
 * class DataTransferThread
 * 用一个独立的线程读取fpga的buffer状态，
 * 如果kernel已经把打印数据发送给FPGA，那么kernel的Buffer状态为空，可写
 * 此时，需要把下一条打印数据下发给kernel Buffer；
 * 如果kernel的buffer状态不为空，不可写
 * 此时，线程轮训buffer，直到kernel buffer状态为空；
 * @author kevin
 *
 */
public class DataTransferThread {
	
	public static final String TAG = DataTransferThread.class.getSimpleName();
	private static final int MESSAGE_EXCEED_TIMEOUT = 60 * 1000;
	
	public static boolean mRunning;
	public static boolean mStopped;
	public boolean pcReset;
	public static volatile DataTransferThread mInstance;
	
	private Context mContext;
	
	public boolean mNeedUpdate=false;
	private boolean isBufferReady = false;

// H.M.Wang 2023-12-3 修改锁值记录方法。阈值计数器修改为浮点型，以便于管理调整后的阈值（必须为浮点型，否则不准确）
//	private int mcountdown[];
	private float mcountdown[];
// End of H.M.Wang 2023-12-3 修改锁值记录方法。阈值计数器修改为浮点型，以便于管理调整后的阈值（必须为浮点型，否则不准确）

	/**打印数据buffer**/
	public List<DataTask> mDataTask;
	/* task index currently printing */
	private int mIndex;
	IInkScheduler mScheduler;
	private static long mInterval = 0;
	private int mThreshold;

	private PrintTask mPrinter;
	
	private int testCount = 0;

	//
	private Lock mPurgeLock;
	public  boolean isCleaning;

	private static Map<String,char[]> mLanBuffer;


	private InkLevelListener mInkListener = null;

// H.M.Wang 2022-11-8 添加一个显示Bagink当中Level值的信息框
	private AlertDialog mRecvedLevelPromptDlg = null;
// End of H.M.Wang 2022-11-8 添加一个显示Bagink当中Level值的信息框

	public static DataTransferThread getInstance(Context ctx) {
		if(mInstance == null) {
			synchronized (DataTransferThread.class) {
				if (mInstance == null) {
					mInstance = new DataTransferThread(ctx);
				}
			}
			Debug.d(TAG, "===>new thread");
		}
		return mInstance;
	}
	
	public DataTransferThread(Context ctx) {
		mContext = ctx;
		mPurgeLock = new ReentrantLock();
// H.M.Wang 2022-11-8 添加一个显示Bagink当中Level值的信息框
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		mRecvedLevelPromptDlg = builder.setTitle("读取LEVEL值").setMessage("").setPositiveButton("关闭", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mRecvedLevelPromptDlg.hide();
			}
		}).create();
// End of H.M.Wang 2022-11-8 添加一个显示Bagink当中Level值的信息框
	}
	
	/**
	 * 数据更新机制：
	 * 每次发送数据时同时触发一个delay 10s的handler
	 * 如果pollState返回不为0，即数据打印完毕，则remove handlermessage
	 * 否则，处理这个message并置数据更新状态为true
	 * run函数中一旦检测到数据更新状态变为true，就重新生成buffer并下发
	 */
	


	private synchronized void next() {
		mIndex++;
		if (isLanPrint()) {
			if (!mLanBuffer.containsKey(String.valueOf(mIndex))){
				mIndex = 0;
			}
		} else {
			if (mIndex >= mDataTask.size()) {
				mIndex = 0;
			}
		}
// H.M.Wang 2021-3-3 从QR.txt文件当中读取的变量信息的功能从DataTask类转移至此
		if(index() == 0) setContentsFromQRFile();
// End of H.M.Wang 2021-3-3 从QR.txt文件当中读取的变量信息的功能从DataTask类转移至此
		Debug.i(TAG, "--->next: " + mIndex);
	}

	private boolean isLanPrint() {
		return (SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_BIN);
	}

	public static synchronized void setLanBuffer(Context context, int index, char[] buffer) {
		if (mLanBuffer == null) {
			mLanBuffer = new HashMap<String, char[]>();
		}
//		Debug.i(TAG, "--->setlanBuffer: [" + index + "," + buffer.length + "]");
		mLanBuffer.put(String.valueOf(index), Arrays.copyOf(buffer, buffer.length));
		if (index == DataTransferThread.getInstance(context).index()) {
			FpgaGpioOperation.writeData(FpgaGpioOperation.DATA_GENRE_UPDATE, FpgaGpioOperation.FPGA_STATE_OUTPUT, buffer, buffer.length * 2);
		}
	}

	public static synchronized char[] getLanBuffer(int index) {
		if (mLanBuffer != null) {
			return mLanBuffer.get(String.valueOf(index));
		}
		return new char[2];
	}

	public static synchronized void deleteLanBuffer(int index) {
		if (mLanBuffer != null) {
			mLanBuffer.remove(String.valueOf(index));
		}
	}

	public static synchronized void cleanLanBuffer() {
		if (mLanBuffer != null) {
			mLanBuffer.clear();
		}
	}

	public synchronized void setIndex(int index) {
		mIndex = index;
	}

	public synchronized int index() {
		return mIndex;
	}

	public synchronized void resetIndex() {
		mIndex = 0;
		//pcReset = true;
		char[] buffer = getLanBuffer(index());
		FpgaGpioOperation.writeData(FpgaGpioOperation.DATA_GENRE_UPDATE, FpgaGpioOperation.FPGA_STATE_OUTPUT, buffer, buffer.length * 2);
	}
	
	boolean needRestore = false;
// H.M.Wang 2020-8-21 追加正在清洗标志，此标志为ON的时候不能对FPGA进行某些操作，如开始，停止等，否则死机
	public boolean isPurging = false;
// End of H.M.Wang 2020-8-21 追加正在清洗标志，此标志为ON的时候不能对FPGA进行某些操作，如开始，停止等，否则死机

	public void purge(final Context context) {
		SystemConfigFile config = SystemConfigFile.getInstance(mContext);
		final int headIndex = config.getParam(SystemConfigFile.INDEX_HEAD_TYPE);
		PrinterNozzle head = PrinterNozzle.getInstance(headIndex);

		// H.M.Wang 修改下列两行
//		final boolean dotHd = (head == PrinterNozzle.MESSAGE_TYPE_16_DOT || head == PrinterNozzle.MESSAGE_TYPE_32_DOT);
//		final boolean dotHd = (head == PrinterNozzle.MESSAGE_TYPE_16_DOT || head == PrinterNozzle.MESSAGE_TYPE_32_DOT || head == PrinterNozzle.MESSAGE_TYPE_64_DOT);
		final boolean dotHd =
				(head == PrinterNozzle.MESSAGE_TYPE_16_DOT ||
				head == PrinterNozzle.MESSAGE_TYPE_32_DOT ||
// H.M.Wang 2020-7-23 追加32DN打印头
				head == PrinterNozzle.MESSAGE_TYPE_32DN ||
// End of H.M.Wang 2020-7-23 追加32DN打印头
// H.M.Wang 2020-8-18 追加32SN打印头
                head == PrinterNozzle.MESSAGE_TYPE_32SN ||
// End of H.M.Wang 2020-8-18 追加32SN打印头
// H.M.Wang 2020-8-26 追加64SN打印头
                head == PrinterNozzle.MESSAGE_TYPE_64SN ||
// End of H.M.Wang 2020-8-26 追加64SN打印头
// H.M.Wang 2022-10-19 追加64SLANT头
				head == PrinterNozzle.MESSAGE_TYPE_64SLANT ||
// End of H.M.Wang 2022-10-19 追加64SLANT头
// H.M.Wang 2022-5-27 追加32x2头类型
				head == PrinterNozzle.MESSAGE_TYPE_32X2 ||
// End of H.M.Wang 2022-5-27 追加32x2头类型
				head == PrinterNozzle.MESSAGE_TYPE_64_DOT ||
// H.M.Wang 2023-7-29 追加48点头
				head == PrinterNozzle.MESSAGE_TYPE_48_DOT ||
// End of H.M.Wang 2023-7-29 追加48点头
// H.M.Wang 2021-8-16 追加96DN头
				head == PrinterNozzle.MESSAGE_TYPE_96DN);
// End of H.M.Wang 2021-8-16 追加96DN头

// H.M.Wang 2021-4-13 取消3-5修改内容，恢复原来的停止和打开操作
// H.M.Wang 2021-3-5 取消purge之前停止打印，purge之后恢复打印的做法。因为停止打印可能会产生计数器跳数
		if (isRunning()) {
			FpgaGpioOperation.uninit();
// 2020-7-21 取消清洗时停止打印操作，改为下发适当设置参数
//			finish();
// End of 2020-7-21 取消清洗时停止打印操作，改为下发适当设置参数
			FpgaGpioOperation.clean();
			needRestore = true;
		}
// End of H.M.Wang2021-3-5 取消purge之前停止打印，purge之后恢复打印的做法。因为停止打印可能会产生计数器跳数
// End of H.M.Wang 2021-4-13 取消3-5修改内容，恢复原来的停止和打开操作

		ThreadPoolManager.mThreads.execute(new Runnable() {
			
			@Override
			public void run() {
				isPurging = true;

				DataTask task = new DataTask(context, null);
				Debug.e(TAG, "--->task: " + task + "   dotHD: " + dotHd);
				
				String purgeFile = "purge/single.bin";
				if (dotHd) {
					purgeFile = "purge/purge4big.bin";
				}
// H.M.Wang 2023-8-11 32SN/DN, 48点的使用新的purge32.bin
				PrinterNozzle head = PrinterNozzle.getInstance(headIndex);
				if(head == PrinterNozzle.MESSAGE_TYPE_32DN ||
  				   head == PrinterNozzle.MESSAGE_TYPE_32SN ||
				   head == PrinterNozzle.MESSAGE_TYPE_48_DOT) {
					purgeFile = "purge/purge32.bin";
				}
// End of H.M.Wang 2023-8-11 32SN/DN, 48点的使用新的purge32.bin

				char[] buffer = task.preparePurgeBuffer(purgeFile, dotHd);

// H.M.Wang 2020-8-21 取消大字机清洗后直接退出，该恢复打印的还是应该恢复打印
//				if (dotHd) {
//					purge(mContext, task, buffer, FpgaGpioOperation.SETTING_TYPE_PURGE1);
//					return;
//				}
// End of H.M.Wang 2020-8-21 取消大字机清洗后直接退出，该恢复打印的还是应该恢复打印

				purge(mContext, task, buffer, FpgaGpioOperation.SETTING_TYPE_PURGE1);
//				purge(mContext, task, buffer, FpgaGpioOperation.SETTING_TYPE_PURGE2);
//
//
//				purge(mContext, task, buffer, FpgaGpioOperation.SETTING_TYPE_PURGE1);
//				purge(mContext, task, buffer, FpgaGpioOperation.SETTING_TYPE_PURGE2);

// H.M.Wang 2021-4-13 取消3-5修改内容，恢复原来的停止和打开操作
// H.M.Wang 2021-3-5 取消purge之前停止打印，purge之后恢复打印的做法。因为停止打印可能会产生计数器跳数，结束purge之后，恢复到数据传输状态
				if (needRestore) {
// 2020-7-21 取消清洗时停止打印操作，改为下发适当设置参数
//					launch(mContext);
// End of 2020-7-21 取消清洗时停止打印操作，改为下发适当设置参数
// H.M.Wang 2022-1-25 使用task设置参数可能会与打印的数据不符，比如打印数据是25.4头的，但是清洗是12.7头的，就会导致每列字节数不同而是恢复打印后的数据产生偏差，以前mDataTask为空的情况可能是还没有加needRestore变量，现在应该不会了，所以恢复原来的实现
    				FpgaGpioOperation.updateSettings(mContext, mDataTask.get(mIndex), FpgaGpioOperation.SETTING_TYPE_NORMAL);
// H.M.Wang 2021-3-19 未开始打印前启动purge时，mDataTask为空，会导致崩溃
//					FpgaGpioOperation.updateSettings(mContext, task, FpgaGpioOperation.SETTING_TYPE_NORMAL);
//    				FpgaGpioOperation.updateSettings(mContext, mDataTask.get(mIndex), FpgaGpioOperation.SETTING_TYPE_NORMAL);
// End of H.M.Wang 2021-3-19 未开始打印前启动purge时，mDataTask为空，会导致崩溃
// End of H.M.Wang 2022-1-25 使用task设置参数可能会与打印的数据不符，比如打印数据是25.4头的，但是清洗是12.7头的，就会导致每列字节数不同而是恢复打印后的数据产生偏差，以前mDataTask为空的情况可能是还没有加needRestore变量，现在应该不会了，所以恢复原来的实现
					FpgaGpioOperation.init(mContext);
// H.M.Wang 2021-3-5 暂时取消
//					resendBufferToFPGA();
// End of H.M.Wang 2021-3-5 暂时取消
					needRestore = false;
				}
// End of H.M.Wang 2021-3-5 取消purge之前停止打印，purge之后恢复打印的做法。因为停止打印可能会产生计数器跳数
// End of H.M.Wang 2021-4-13 取消3-5修改内容，恢复原来的停止和打开操作
				isPurging = false;
			}
		
		});
	}

// H.M.Wang 2023-8-7 增加12头清洗功能。通过CMD_CLEAN或CMD_CLEAN_S的content指定，0为全清洗，1-12为指定头清洗
	public static int CleanHead = 0;
// H.M.Wang 2023-8-7 增加12头清洗功能。通过CMD_CLEAN或CMD_CLEAN_S的content指定，0为全清洗，1-12为指定头清洗
// H.M.Wang 2023-8-8 增加一个新的网络命令，SelectPen
	public static int SelectPen = 0;
// End of H.M.Wang 2023-8-8 增加一个新的网络命令，SelectPen
	private void purge(Context context, DataTask task, char[] buffer, int purgeType) {
		
		Debug.e(TAG, "--->buffer len: " + buffer.length);
		FpgaGpioOperation.updateSettings(context, task, purgeType);
// H.M.Wang 2022-3-18 在3.5寸老板新屏的设备上，由于不支持自动打印，恢复到原来的清洗模式
		if(PlatformInfo.getImgUniqueCode().startsWith("GZJ")) {    // GZJ盖章机直接按着清洗数据下发，因为GZJ没有自动打印
			FpgaGpioOperation.writeData(FpgaGpioOperation.DATA_GENRE_IGNORE, FpgaGpioOperation.FPGA_STATE_PURGE, buffer, buffer.length*2);
		} else {			// 其他的还是按打印数据下发
			FpgaGpioOperation.init(mContext);
			FpgaGpioOperation.writeData(FpgaGpioOperation.DATA_GENRE_NEW, FpgaGpioOperation.FPGA_STATE_OUTPUT, buffer, buffer.length*2);
		}
// End of H.M.Wang 2022-3-18 在3.5寸老板新屏的设备上，由于不支持自动打印，恢复到原来的清洗模式
// H.M.Wang 2021-10-22 修改清洗，从特别处理改为按普通打印下发，但是与正常的打印不共存，先停止正常打印，在开始清洗打印，然后在恢复打印
////		FpgaGpioOperation.writeData(FpgaGpioOperation.DATA_GENRE_IGNORE, FpgaGpioOperation.FPGA_STATE_PURGE, buffer, buffer.length*2);
//		FpgaGpioOperation.init(mContext);
//		FpgaGpioOperation.writeData(FpgaGpioOperation.DATA_GENRE_NEW, FpgaGpioOperation.FPGA_STATE_OUTPUT, buffer, buffer.length*2);
// End of H.M.Wang 2021-10-22 修改清洗，从特别处理改为按普通打印下发，但是与正常的打印不共存，先停止正常打印，在开始清洗打印，然后在恢复打印
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
// H.M.Wang 2021-10-22 修改清洗，从特别处理改为按普通打印下发，但是与正常的打印不共存，先停止正常打印，在开始清洗打印，然后在恢复打印
		} finally {
// H.M.Wang 2022-3-18 在3.5寸老板新屏的设备上，由于不支持自动打印，恢复到原来的清洗模式，这里取消停止打印的操作
//			FpgaGpioOperation.uninit();
			if(!PlatformInfo.getImgUniqueCode().startsWith("GZJ")) {    // 不是老板新屏标识
				FpgaGpioOperation.uninit();
			}
// End of H.M.Wang 2022-3-18 在3.5寸老板新屏的设备上，由于不支持自动打印，恢复到原来的清洗模式，这里取消停止打印的操作
// End of H.M.Wang 2021-10-22 修改清洗，从特别处理改为按普通打印下发，但是与正常的打印不共存，先停止正常打印，在开始清洗打印，然后在恢复打印
		}

		FpgaGpioOperation.clean();
	}

	/**
	 *
	 * @param context
	 */
	public void clean(final Context context) {
		SystemConfigFile config = SystemConfigFile.getInstance(mContext);
		final int headIndex = config.getParam(SystemConfigFile.INDEX_HEAD_TYPE);
		final PrinterNozzle head = PrinterNozzle.getInstance(headIndex);

		// H.M.Wang 修改下列两行
//		if (head != PrinterNozzle.MESSAGE_TYPE_16_DOT && head != PrinterNozzle.MESSAGE_TYPE_32_DOT) {
//		if (head != PrinterNozzle.MESSAGE_TYPE_16_DOT && head != PrinterNozzle.MESSAGE_TYPE_32_DOT && head != PrinterNozzle.MESSAGE_TYPE_64_DOT) {
		if (head != PrinterNozzle.MESSAGE_TYPE_16_DOT &&
			head != PrinterNozzle.MESSAGE_TYPE_32_DOT &&
// H.M.Wang 2020-7-23 追加32DN打印头
			head != PrinterNozzle.MESSAGE_TYPE_32DN &&
// End of H.M.Wang 2020-7-23 追加32DN打印头
// H.M.Wang 2020-8-18 追加32SN打印头
            head != PrinterNozzle.MESSAGE_TYPE_32SN &&
// End of H.M.Wang 2020-8-18 追加32SN打印头
// H.M.Wang 2020-8-26 追加64SN打印头
            head != PrinterNozzle.MESSAGE_TYPE_64SN &&
// End of H.M.Wang 2020-8-26 追加64SN打印头
// H.M.Wang 2022-10-19 追加64SLANT头
			head != PrinterNozzle.MESSAGE_TYPE_64SLANT &&
// End of H.M.Wang 2022-10-19 追加64SLANT头
// H.M.Wang 2022-5-27 追加32x2头类型
			head != PrinterNozzle.MESSAGE_TYPE_32X2 &&
// End of H.M.Wang 2022-5-27 追加32x2头类型
			head != PrinterNozzle.MESSAGE_TYPE_64_DOT &&
// H.M.Wang 2023-7-29 追加48点头
			head != PrinterNozzle.MESSAGE_TYPE_48_DOT &&
// End of H.M.Wang 2023-7-29 追加48点头
// H.M.Wang 2021-8-16 追加96DN头
			head != PrinterNozzle.MESSAGE_TYPE_96DN) {
// End of H.M.Wang 2021-8-16 追加96DN头
			return;
		}

		ThreadPoolManager.mThreads.execute(new Runnable() {
			
			@Override
			public void run() {
				// access lock before cleaning begin
				mPurgeLock.lock();
				isCleaning = true;
				DataTask task = new DataTask(context, null);
				Debug.e(TAG, "--->task: " + task);
//				String purgeFile = "purge/single.bin";

/*				// H.M.Wang 修改下列两行
//				if (head == PrinterNozzle.MESSAGE_TYPE_16_DOT || head == PrinterNozzle.MESSAGE_TYPE_32_DOT) {
// H.M.Wang 2020-7-23 追加32DN打印头
//				if (head == PrinterNozzle.MESSAGE_TYPE_16_DOT || head == PrinterNozzle.MESSAGE_TYPE_32_DOT || head == PrinterNozzle.MESSAGE_TYPE_64_DOT) {
				if (head == PrinterNozzle.MESSAGE_TYPE_16_DOT ||
					head == PrinterNozzle.MESSAGE_TYPE_32_DOT ||
					head == PrinterNozzle.MESSAGE_TYPE_32DN ||
// H.M.Wang 2020-8-18 追加32SN打印头
					head == PrinterNozzle.MESSAGE_TYPE_32SN ||
// End of H.M.Wang 2020-8-18 追加32SN打印头
// H.M.Wang 2020-8-26 追加64SN打印头
					head == PrinterNozzle.MESSAGE_TYPE_64SN ||
// End of H.M.Wang 2020-8-26 追加64SN打印头
					head == PrinterNozzle.MESSAGE_TYPE_64_DOT) {
// End of H.M.Wang 2020-7-23 追加32DN打印头*/
				String purgeFile = "purge/purge4big.bin";
//				}
				char[] buffer = task.preparePurgeBuffer(purgeFile, true);
				
// H.M.Wang 2022-1-4 取消PURGE2的清洗，只留PURGE1，间隔还是10s，重复30次
				FpgaGpioOperation.clean();
				FpgaGpioOperation.updateSettings(context, task, FpgaGpioOperation.SETTING_TYPE_PURGE1);
				FpgaGpioOperation.init(mContext);
				for (int i = 0; i < 50; i++) {
// End of H.M.Wang 2022-1-4 取消PURGE2的清洗，只留PURGE1，间隔还是10s，重复30次
					Debug.e(TAG, "(" + (i+1) + ")--->buffer len: " + buffer.length);
					
					FpgaGpioOperation.writeData(FpgaGpioOperation.DATA_GENRE_NEW, FpgaGpioOperation.FPGA_STATE_OUTPUT, buffer, buffer.length*2);
					try {
						Thread.sleep(1000 * 6);
//						mPurgeLock.tryLock(10, TimeUnit.SECONDS);
//						break;
					} catch (Exception e) {
						// e.printStackTrace();
						// mPurgeLock.unlock();
					}

// H.M.Wang 2022-1-4 取消PURGE2的清洗，只留PURGE1，间隔还是10s，重复30次
/*					FpgaGpioOperation.clean();
					FpgaGpioOperation.updateSettings(context, task, FpgaGpioOperation.SETTING_TYPE_PURGE2);
					FpgaGpioOperation.writeData(FpgaGpioOperation.DATA_GENRE_IGNORE, FpgaGpioOperation.FPGA_STATE_PURGE, buffer, buffer.length*2);
					try {
						mPurgeLock.tryLock(10, TimeUnit.SECONDS);
//						break;
					} catch (InterruptedException e) {
						// e.printStackTrace();
					}
*/
// End of H.M.Wang 2022-1-4 取消PURGE2的清洗，只留PURGE1，间隔还是10s，重复30次
				}
				FpgaGpioOperation.uninit();
				FpgaGpioOperation.dispLog();
				FpgaGpioOperation.clean();
				try {
					mPurgeLock.unlock();
				} catch (Exception e) {
					// e.printStackTrace();
					// mPurgeLock.unlock();
				}
				isCleaning = false;
			}
			
		});
	}

	/**
	 * interrupt the cleaning task
	 */
	public void interruptClean() {
		mPurgeLock.unlock();
	}
	
	public boolean isRunning() {
		return mRunning;
	}

// H.M.Wang 2022-6-1 新的外部文本处理函数，支持新的PC或者串口PC当中的DT设置命令（CMD_SET_REMOTE1和CMD_SET_REMOTE1_S),接收到的10个DT对应于10个全局DT桶的顺序
	public void setRemote1TextSeparated(final String data) {
		Debug.d(TAG, "String from Remote = [" + data + "]");
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				if(null != mRemoteRecvedPromptDlg) {
					mRemoteRecvedPromptDlg.show();		// 不知道为啥，hide之后，必须要show两次才能够及时显示出来
					mRemoteRecvedPromptDlg.setMessage(data);
//					mRemoteRecvedPromptDlg.show();
				}
			}
		});
		String[] recvStrs = data.split(EC_DOD_Protocol.TEXT_SEPERATOR);

		boolean needUpdate = false;

// H.M.Wang 2021-5-21 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，编辑页面显示#####
		for(int i=0; i<Math.min(recvStrs.length, 10); i++) {
			SystemConfigFile.getInstance().setDTBuffer(i, recvStrs[i]);
		}
// End of H.M.Wang 2021-5-21 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，编辑页面显示#####
// H.M.Wang 2022-6-15 追加条码内容的保存桶
		if(recvStrs.length >= 11) {
			SystemConfigFile.getInstance().setBarcodeBuffer(recvStrs[10]);
		}
// End of H.M.Wang 2022-6-15 追加条码内容的保存桶

// H.M.Wang 2020-9-10 协议收到的数值对群组也有效
		for(DataTask dataTask : mDataTask) {
			ArrayList<BaseObject> objList = dataTask.getObjList();
			for(BaseObject baseObject: objList) {
				if(baseObject instanceof DynamicText) {
					int strIndex = ((DynamicText) baseObject).getDtIndex();
						Debug.d(TAG, "DynamicText[" + baseObject.getIndex() + "](DT Index: " + strIndex + "): " + recvStrs[strIndex]);
// H.M.Wang 2023-2-5 这一段应该不需要，因为前面已经设置了
//// H.M.Wang 2021-5-21 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，编辑页面显示#####
//						SystemConfigFile.getInstance().setDTBuffer(strIndex, recvStrs[strIndex]);
//// End of H.M.Wang 2021-5-21 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，编辑页面显示#####
// End of H.M.Wang 2023-2-5 这一段应该不需要，因为前面已经设置了
						baseObject.setContent(recvStrs[strIndex]);
						needUpdate = true;
				} else if(baseObject instanceof BarcodeObject) {
					if(((BarcodeObject)baseObject).isDynamicCode() && recvStrs.length >= 11) {
// H.M.Wang 2024-2-22 追加一个GS1网络协议。内容与DATA_SOURCE_GS1_BRACE一样，只是数据从LAN来，走650或者600命令
						if(SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_LAN_GS1_BRACE) {
							recvStrs[10] = parseGS1Brace(recvStrs[10]);
						}
// End of H.M.Wang 2024-2-22 追加一个GS1网络协议。内容与DATA_SOURCE_GS1_BRACE一样，只是数据从LAN来，走650或者600命令
// H.M.Wang 2023-2-5 这一段应该不需要，因为前面已经设置了
//// H.M.Wang 2022-6-15 追加条码内容的保存桶
//						SystemConfigFile.getInstance().setBarcodeBuffer(recvStrs[10]);
//// End of H.M.Wang 2022-6-15 追加条码内容的保存桶
// End of H.M.Wang 2023-2-5 这一段应该不需要，因为前面已经设置了
						((BarcodeObject)baseObject).setContent(recvStrs[10]);
						needUpdate = true;
// H.M.Wang 2024-1-12 静态文本当含有超文本中的可变内容时，重新画
// End of H.M.Wang 2024-1-12 静态文本当含有超文本中的可变内容时，重新画
					} else if(!((BarcodeObject) baseObject).isDynamicCode() && ((BarcodeObject) baseObject).containsDT()) {
						needUpdate = true;
					}
// H.M.Wang 2023-12-30 增加对超文本中的DT的支持。
				} else if(baseObject instanceof HyperTextObject) {
					needUpdate = ((HyperTextObject)baseObject).setDTCntByIndex(recvStrs);
// End of H.M.Wang 2023-12-30 增加对超文本中的DT的支持。
				}
			}
		}
// End of H.M.Wang 2020-9-10 协议收到的数值对群组也有效
		mNeedUpdate = needUpdate;

// 2020-7-3 标识网络快速打印状态下数据更新
// H.M.Wang 2024-1-13 扫描协议5的打印行为，只有接收到扫描数据时才下发，否则不下发
//		if(SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_FAST_LAN) {
		if(SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_FAST_LAN ||
			SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_SCANER5) {
// End of H.M.Wang 2024-1-13 扫描协议5的打印行为，只有接收到扫描数据时才下发，否则不下发
// H.M.Wang 2024-1-13 扫描协议5的打印行为，只有接收到扫描数据时才下发，否则不下发
			mDataUpdatedForFastLan = true;
		}
// End of 2020-7-3 标识网络快速打印状态下数据更新
	}
// End of H.M.Wang 2022-6-1 新的外部文本处理函数，支持新的PC或者串口PC当中的DT设置命令（CMD_SET_REMOTE1和CMD_SET_REMOTE1_S),接收到的10个DT对应于10个全局DT桶的顺序

// H.M.Wang 2019-12-19 函数名变更，处理由分隔符分开的字符串，主要满足数据源为以太网和串口协议2的情况
// H.M.Wang 2019-12-16 将计数器和动态二维码替代部分函数化，以对应串口和网络两方面的需求
	public void setRemoteTextSeparated(final String data) {
		Debug.d(TAG, "String from Remote = [" + data + "]");
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				if(null != mRemoteRecvedPromptDlg) {
					mRemoteRecvedPromptDlg.show();		// 不知道为啥，hide之后，必须要show两次才能够及时显示出来
					mRemoteRecvedPromptDlg.setMessage(data);
//					mRemoteRecvedPromptDlg.show();
				}
			}
		});
		String[] recvStrs = data.split(EC_DOD_Protocol.TEXT_SEPERATOR);

		int strIndex = 0;
		boolean needUpdate = false;

// H.M.Wang 2020-9-10 协议收到的数值对群组也有效
		for(DataTask dataTask : mDataTask) {
			ArrayList<BaseObject> objList = dataTask.getObjList();
			for(BaseObject baseObject: objList) {
				if(baseObject instanceof DynamicText) {
// H.M.Wang 2019-12-15 支持串口文本通过间隔符分割，对于计数器的文本如果超过位数，多余部分切割功能移至计数器Object类，不在这里处理
					if(strIndex < recvStrs.length) {
						Debug.d(TAG, "DynamicText[" + strIndex + "]: " + recvStrs[strIndex]);
// H.M.Wang 2021-5-21 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，编辑页面显示#####
						SystemConfigFile.getInstance().setDTBuffer(((DynamicText) baseObject).getDtIndex(), recvStrs[strIndex]);
// End of H.M.Wang 2021-5-21 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，编辑页面显示#####
// H.M.Wang 2023-5-30 放开这个注释，否则新的DT可能不能及时反映到当前的变量中
						baseObject.setContent(recvStrs[strIndex]);
// End of H.M.Wang 2023-5-30 放开这个注释，否则新的DT可能不能及时反映到当前的变量中
						strIndex++;
						needUpdate = true;
					}
				} else if(baseObject instanceof BarcodeObject) {
					if(((BarcodeObject)baseObject).isDynamicCode() && recvStrs.length >= 11) {
						Debug.d(TAG, "Dynamic QRCode: " + recvStrs[10]);
// H.M.Wang 2024-2-22 追加一个GS1网络协议。内容与DATA_SOURCE_GS1_BRACE一样，只是数据从LAN来，走650或者600命令
						if(SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_LAN_GS1_BRACE) {
							recvStrs[10] = parseGS1Brace(recvStrs[10]);
						}
// End of H.M.Wang 2024-2-22 追加一个GS1网络协议。内容与DATA_SOURCE_GS1_BRACE一样，只是数据从LAN来，走650或者600命令
// H.M.Wang 2022-6-15 追加条码内容的保存桶
						SystemConfigFile.getInstance().setBarcodeBuffer(recvStrs[10]);
// End of H.M.Wang 2022-6-15 追加条码内容的保存桶
// H.M.Wang 2023-5-30 放开这个注释，否则新的BC可能不能及时反映到当前的变量中
						((BarcodeObject)baseObject).setContent(recvStrs[10]);
// End of H.M.Wang 2023-5-30 放开这个注释，否则新的BC可能不能及时反映到当前的变量中
						needUpdate = true;
// End of H.M.Wang 2024-1-12 静态文本当含有超文本中的可变内容时，重新画
					} else if(!((BarcodeObject) baseObject).isDynamicCode() && ((BarcodeObject) baseObject).containsDT()) {
						needUpdate = true;
					}
// H.M.Wang 2023-12-30 增加对超文本中的DT的支持。
				} else if(baseObject instanceof HyperTextObject) {
					needUpdate = ((HyperTextObject)baseObject).setDTCntByOrder(recvStrs);
// End of H.M.Wang 2023-12-30 增加对超文本中的DT的支持。
				}
			}
		}
// End of H.M.Wang 2020-9-10 协议收到的数值对群组也有效
		mNeedUpdate = needUpdate;

// 2020-7-3 标识网络快速打印状态下数据更新
// H.M.Wang 2024-1-13 扫描协议5的打印行为，只有接收到扫描数据时才下发，否则不下发
//		if(SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_FAST_LAN) {
		if(SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_FAST_LAN ||
			SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_SCANER5) {
// End of H.M.Wang 2024-1-13 扫描协议5的打印行为，只有接收到扫描数据时才下发，否则不下发
            mDataUpdatedForFastLan = true;
		}
// End of 2020-7-3 标识网络快速打印状态下数据更新
	}
// End. -----

// H.M.Wang 2019-12-19 追加函数，处理未由分隔符分开的字符串，根据计数器的位数向前逐个填充计数器，数据不足时计数器内容为空，主要满足串口协议1
	public void setRemoteTextFitCounter(final String data) {
		Debug.d(TAG, "String from Remote = [" + data + "]");
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				if(null != mRemoteRecvedPromptDlg) {
					mRemoteRecvedPromptDlg.show();
//					mRemoteRecvedPromptDlg.show();
					mRemoteRecvedPromptDlg.setMessage(data);
				}
			}
		});

		int strIndex = 0;
		int counterIndex = 0;
		boolean needUpdate = false;

// H.M.Wang 2020-9-10 协议收到的数值对群组也有效
		for(DataTask dataTask : mDataTask) {
			ArrayList<BaseObject> objList = dataTask.getObjList();
			for(BaseObject baseObject: objList) {
				if(baseObject instanceof DynamicText) {
					if(strIndex < data.length()) {
						int readLen = Math.min(((DynamicText)baseObject).getBits(), data.length() - strIndex);
						Debug.d(TAG, "DynamicText[" + counterIndex + "]: " + data.substring(strIndex, strIndex + readLen));
// H.M.Wang 2021-5-21 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，编辑页面显示#####
						SystemConfigFile.getInstance().setDTBuffer(((DynamicText) baseObject).getDtIndex(), data.substring(strIndex, strIndex + readLen));
// End of H.M.Wang 2021-5-21 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，编辑页面显示#####
						baseObject.setContent(data.substring(strIndex, strIndex + readLen));
						strIndex += readLen;
						needUpdate = true;
					} else {
						Debug.d(TAG, "DynamicText[" + counterIndex + "]: ");
					}
					counterIndex++;
				}
			}
		}
// End of H.M.Wang 2020-9-10 协议收到的数值对群组也有效

		mNeedUpdate = needUpdate;
	}
// End. -----

// H.M.Wang 2019-12-19 追加函数，将字符串直接付给第一个计数器，主要满足串口协议3和协议4
	public void setRemoteTextDirect(final String data) {
		Debug.d(TAG, "String from Remote = [" + data + "]");
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				if(null != mRemoteRecvedPromptDlg) {
					mRemoteRecvedPromptDlg.show();
//					mRemoteRecvedPromptDlg.show();
					mRemoteRecvedPromptDlg.setMessage(data);
				}
			}
		});

		boolean needUpdate = false;

// H.M.Wang 2020-9-10 协议收到的数值对群组也有效
		for(DataTask dataTask : mDataTask) {
			ArrayList<BaseObject> objList = dataTask.getObjList();
			for(BaseObject baseObject: objList) {
				if(baseObject instanceof DynamicText) {
					Debug.d(TAG, "DynamicText[0]: " + data);
// H.M.Wang 2021-5-21 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，编辑页面显示#####
					SystemConfigFile.getInstance().setDTBuffer(((DynamicText) baseObject).getDtIndex(), data);
// End of H.M.Wang 2021-5-21 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，编辑页面显示#####
					baseObject.setContent(data);
					needUpdate = true;
					break;
				}
			}
		}
// End of H.M.Wang 2020-9-10 协议收到的数值对群组也有效
		mNeedUpdate = needUpdate;
	}
// End. -----

	public void setScanDataToDt(final String data) {
		Debug.d(TAG, "String from Remote = [" + data + "]");
// H.M.Wang 2021-9-14 这个应该是最初的实现中在这里判断合法性，但是没有取消，结果双重判断导致失败
//		if(data.length() != 33) {                               // 扫描到的字符串长度为32+1
//			return;
//		} else if(data.charAt(1) != data.charAt(32)) {          // 最后一位与第二位的值需要一致
//			return;
//		}
		if(data.length() != 32) {                               // 扫描到的字符串长度为32+1。但是已经去掉了最后一个数字
			return;
		}
// End H.M.Wang 2021-9-14 这个应该是最初的实现中在这里判断合法性，但是没有取消，结果双重判断导致失败

		mHandler.post(new Runnable() {
			@Override
			public void run() {
				if(null != mRemoteRecvedPromptDlg) {
					mRemoteRecvedPromptDlg.show();
//					mRemoteRecvedPromptDlg.show();
					mRemoteRecvedPromptDlg.setMessage(data);
				}
			}
		});

		boolean needUpdate = false;

// H.M.Wang 2020-6-7 修改支持包号->批号检索功能
		String[] dts = new String[8];
// End of H.M.Wang 2020-6-7 修改支持包号->批号检索功能

		dts[0] = data.substring(7, 9);
		dts[1] = data.substring(9, 11);
		dts[2] = data.substring(11, 13);

		int dt34 = 0;
		try {
			dt34 = Integer.valueOf(data.substring(14, 18));
		} catch(NumberFormatException e) {
			Debug.e(TAG, e.getMessage());
		}
// H.M.Wang 2023-2-4 取消参数C63作为基数使用，另作他用
//		dt34 += SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_PARAM_63);
// End of H.M.Wang 2023-2-4 取消参数C63作为基数使用，另作他用
		dts[3] = String.valueOf(dt34/10);
		dts[4] = String.valueOf(dt34%10);

		dts[5] = data.substring(22, 23);
		dts[6] = data.substring(26, 32);

// H.M.Wang 2020-6-7 修改支持包号->批号检索功能
		PackageListReader plr = PackageListReader.getInstance(mContext);
		dts[7] = plr.getBatchCode(dts[6]);
		if(null == dts[7]) dts[7] = "";
// End of H.M.Wang 2020-6-7 修改支持包号->批号检索功能

		for(DataTask dataTask : mDataTask) {
			ArrayList<BaseObject> objList = dataTask.getObjList();
			for(BaseObject baseObject: objList) {
				if(baseObject instanceof DynamicText) {
					int dtIndex = ((DynamicText)baseObject).getDtIndex();
// H.M.Wang 2020-6-7 修改支持包号->批号检索功能
					if(dtIndex >= 0 && dtIndex < 8) {
// End of H.M.Wang 2020-6-7 修改支持包号->批号检索功能
// H.M.Wang 2021-5-21 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，编辑页面显示#####
						SystemConfigFile.getInstance().setDTBuffer(dtIndex, dts[dtIndex]);
// End of H.M.Wang 2021-5-21 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，编辑页面显示#####
						baseObject.setContent(dts[dtIndex]);
						needUpdate = true;
					}
				}
			}
		}
		mNeedUpdate = needUpdate;
	}
	// End. -----
// H.M.Wang 2020-6-9 追加串口6协议
	public void setSP6DataToDt(final String data) {
		Debug.d(TAG, "String from Remote = [" + data + "]");
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				if (null != mRemoteRecvedPromptDlg) {
					mRemoteRecvedPromptDlg.show();
//					mRemoteRecvedPromptDlg.show();
					mRemoteRecvedPromptDlg.setMessage(data);
				}
			}
		});

        if(data.length() != 19) {                               // 数据长度19
			ToastUtil.show(mContext, R.string.invalid_protocol);
			return;
		}

		boolean needUpdate = false;

// H.M.Wang 2020-6-7 修改支持包号->批号检索功能
		String[] dts = new String[6];
// End of H.M.Wang 2020-6-7 修改支持包号->批号检索功能

		dts[0] = data.substring(7, 8);
		dts[1] = data.substring(8, 9);
		dts[2] = data.substring(9, 10);
		dts[3] = data.substring(10, 11);
		dts[4] = data.substring(12, 13);
		dts[5] = data.substring(13, 14);

		for(DataTask dataTask : mDataTask) {
			ArrayList<BaseObject> objList = dataTask.getObjList();
			for(BaseObject baseObject: objList) {
				if(baseObject instanceof DynamicText) {
					int dtIndex = ((DynamicText)baseObject).getDtIndex();
					if(dtIndex >= 0 && dtIndex < 6) {
// H.M.Wang 2021-5-21 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，编辑页面显示#####
						SystemConfigFile.getInstance().setDTBuffer(dtIndex, dts[dtIndex]);
// End of H.M.Wang 2021-5-21 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，编辑页面显示#####
						baseObject.setContent(dts[dtIndex]);
					} else {
						baseObject.setContent("");
					}
					needUpdate = true;
				}
			}
		}
		mNeedUpdate = needUpdate;
	}
// End of H.M.Wang 2020-6-9 追加串口6协议

// H.M.Wang 2020-10-30 追加扫描2串口协议
	public void setScan2DataToDt(final String data) {
		Debug.d(TAG, "String from Remote = [" + data + "]");
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				if(null != mRemoteRecvedPromptDlg) {
					mRemoteRecvedPromptDlg.show();
//					mRemoteRecvedPromptDlg.show();
					mRemoteRecvedPromptDlg.setMessage(data);
				}
			}
		});

		boolean needUpdate = false;
		String[] recvStrs = data.split(Scaner2Protocol.TEXT_SEPERATOR);

		for(DataTask dataTask : mDataTask) {
			ArrayList<BaseObject> objList = dataTask.getObjList();
			for(BaseObject baseObject: objList) {
				if(baseObject instanceof DynamicText) {
                    int dtIndex = ((DynamicText)baseObject).getDtIndex();
                    if(dtIndex >= 0 && dtIndex < recvStrs.length) {
// H.M.Wang 2021-5-21 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，编辑页面显示#####
						SystemConfigFile.getInstance().setDTBuffer(dtIndex, recvStrs[dtIndex]);
// End of H.M.Wang 2021-5-21 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，编辑页面显示#####
                        baseObject.setContent(recvStrs[dtIndex]);
                    } else {
						baseObject.setContent("");
					}
					needUpdate = true;
				}
			}
		}
		mNeedUpdate = needUpdate;
	}
// End of H.M.Wang 2020-10-30 追加扫描2串口协议

// H.M.Wang 2021-5-21 追加扫描协议4
	private int mDtIndex = 0;
	private String mLastRecvString = "";
//	private String[] mDTBuffer = {""};

	public void setScan4DataToDt(final String data) {
		Debug.d(TAG, "String from Remote = [" + data + "]");
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				if(null != mRemoteRecvedPromptDlg) {
					mRemoteRecvedPromptDlg.show();
//					mRemoteRecvedPromptDlg.show();
					mRemoteRecvedPromptDlg.setMessage(data);
				}
			}
		});

		boolean needUpdate = false;

		if(data.startsWith("Resetcode7799")) {
			mDtIndex = 0;
            mLastRecvString = "";
//        } else if(!data.equals(mLastRecvString)) {
		} else {
			for(DataTask dataTask : mDataTask) {
				ArrayList<BaseObject> objList = dataTask.getObjList();
				for(BaseObject baseObject: objList) {
					if(baseObject instanceof DynamicText) {
						int dtIndex = ((DynamicText)baseObject).getDtIndex();
						if(dtIndex == mDtIndex) {
// H.M.Wang 2023-3-16 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，因此接收到的数据应该存入桶里，这里是修改遗漏
// 在下面设了，这里不用设							SystemConfigFile.getInstance().setDTBuffer(((DynamicText)baseObject).getDtIndex(), data);
// End of H.M.Wang 2023-3-16 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，因此接收到的数据应该存入桶里，这里是修改遗漏
							baseObject.setContent(data);
							needUpdate = true;
						}
					}
				}
			}

			mNeedUpdate = needUpdate;

			mLastRecvString = data;
// H.M.Wang 2021-5-21 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，编辑页面显示#####
			SystemConfigFile.getInstance().setDTBuffer(mDtIndex++, data);
// End of H.M.Wang 2021-5-21 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，编辑页面显示#####
			mDtIndex %= 10;
		}
	}
// End of H.M.Wang 2021-5-21 追加扫描协议4

// H.M.Wang 2021-3-6 追加串口协议8
	private void setSerialProtocol8DTs(final byte[] data) {
		Debug.d(TAG, "String from Remote = [" + ByteArrayUtils.toHexString(data) + "]");

// H.M.Wang 2021-4-13 修改数据格式及处理方法
// H.M.Wang 2021-4-11 追加4字节品种代码
        int proCode = (0x000000ff & data[SerialProtocol8.TAG_PRODUCT_TYPE_POS+2]);
        proCode *= 0x100;
        proCode += (0x000000ff & data[SerialProtocol8.TAG_PRODUCT_TYPE_POS+3]);
        proCode *= 0x100;
        proCode += (0x000000ff & data[SerialProtocol8.TAG_PRODUCT_TYPE_POS]);
        proCode *= 0x100;
        proCode += (0x000000ff & data[SerialProtocol8.TAG_PRODUCT_TYPE_POS+1]);
// End of H.M.Wang 2021-4-11 追加4字节品种代码

		int writeValue = (0x000000ff & data[SerialProtocol8.TAG_WRITE_DATA_POS+2]);
        writeValue *= 0x100;
        writeValue += (0x000000ff & data[SerialProtocol8.TAG_WRITE_DATA_POS+3]);
        writeValue *= 0x100;
        writeValue += (0x000000ff & data[SerialProtocol8.TAG_WRITE_DATA_POS]);
        writeValue *= 0x100;
        writeValue += (0x000000ff & data[SerialProtocol8.TAG_WRITE_DATA_POS+1]);
// H.M.Wang 2021-4-13 修改数据格式及处理方法

		mHandler.post(new Runnable() {
			@Override
			public void run() {
				if(null != mRemoteRecvedPromptDlg) {
					mRemoteRecvedPromptDlg.show();
//					mRemoteRecvedPromptDlg.show();
					mRemoteRecvedPromptDlg.setMessage(ByteArrayUtils.toHexString(data));
				}
			}
		});

		boolean needUpdate = false;

		ArrayList<BaseObject> objList = mDataTask.get(index()).getObjList();
		for (BaseObject baseObject : objList) {
			if (baseObject instanceof DynamicText) {
				int dtIndex = ((DynamicText)baseObject).getDtIndex();
// H.M.Wang 2021-4-11 追加4字节品种代码
                if(dtIndex == 2) {
					StringBuilder sb = new StringBuilder();
					for(int i=0; i<((DynamicText) baseObject).getBits(); i++) {
						sb.append(" ");
					}
					sb.append(proCode / 10);
// H.M.Wang 2021-5-21 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，编辑页面显示#####
					SystemConfigFile.getInstance().setDTBuffer(dtIndex, sb.substring(sb.length() - ((DynamicText) baseObject).getBits()));
// End of H.M.Wang 2021-5-21 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，编辑页面显示#####
					baseObject.setContent(sb.substring(sb.length() - ((DynamicText) baseObject).getBits()));
					needUpdate = true;
				} else if(dtIndex == 3) {
// H.M.Wang 2021-5-21 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，编辑页面显示#####
					SystemConfigFile.getInstance().setDTBuffer(dtIndex, "" + proCode % 10);
// End of H.M.Wang 2021-5-21 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，编辑页面显示#####
					baseObject.setContent("" + proCode % 10);
					needUpdate = true;
				} else if(dtIndex == 1) {
// End of H.M.Wang 2021-4-11 追加4字节品种代码
// H.M.Wang 2021-5-21 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，编辑页面显示#####
					SystemConfigFile.getInstance().setDTBuffer(dtIndex, "" + writeValue % 10);
// End of H.M.Wang 2021-5-21 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，编辑页面显示#####
					baseObject.setContent("" + writeValue % 10);
					needUpdate = true;
				} else if(dtIndex == 0) {
					StringBuilder sb = new StringBuilder();
					for(int i=0; i<((DynamicText) baseObject).getBits(); i++) {
						sb.append(" ");
					}
					sb.append(writeValue / 10);
// H.M.Wang 2021-5-21 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，编辑页面显示#####
					SystemConfigFile.getInstance().setDTBuffer(dtIndex, sb.substring(sb.length() - ((DynamicText) baseObject).getBits()));
// End of H.M.Wang 2021-5-21 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，编辑页面显示#####
					baseObject.setContent(sb.substring(sb.length() - ((DynamicText) baseObject).getBits()));
					needUpdate = true;
				}
			}
		}
		mNeedUpdate = needUpdate;
	}
// End of H.M.Wang 2021-3-6 追加串口协议8

// H.M.Wang 2021-9-24 追加串口协议9
private void setSerialProtocol9DTs(final String data) {
	Debug.d(TAG, "String from Remote = [" + data + "]");

	String[] dts = new String[8];
	dts[0] = data.substring(0, 2);
	dts[1] = data.substring(2, 4);
	dts[2] = data.substring(4, 6);
	dts[3] = data.substring(6, 9);
	dts[4] = data.substring(9, 10);
	dts[5] = data.substring(10, 11);
	dts[6] = data.substring(11, 17);
	dts[7] = data.substring(17, 28);

	mHandler.post(new Runnable() {
		@Override
		public void run() {
			if(null != mRemoteRecvedPromptDlg) {
				mRemoteRecvedPromptDlg.show();
//					mRemoteRecvedPromptDlg.show();
				mRemoteRecvedPromptDlg.setMessage(data);
			}
		}
	});

	boolean needUpdate = false;

	ArrayList<BaseObject> objList = mDataTask.get(index()).getObjList();
	for (BaseObject baseObject : objList) {
		if (baseObject instanceof DynamicText) {
			int dtIndex = ((DynamicText)baseObject).getDtIndex();
			if(dtIndex >= 0 && dtIndex < 8) {
// H.M.Wang 2021-5-21 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，编辑页面显示#####
				SystemConfigFile.getInstance().setDTBuffer(dtIndex, dts[dtIndex]);
// End of H.M.Wang 2021-5-21 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，编辑页面显示#####
				baseObject.setContent(dts[dtIndex]);
			} else {
				baseObject.setContent("");
			}
			needUpdate = true;
		}
	}
	mNeedUpdate = needUpdate;
}
// End of H.M.Wang 2021-9-24 追加串口协议9

// H.M.Wang 2021-9-17 追加扫描协议1-FIFO
    private final int SCAN_FIFO_MAX_COUNT = 10;
    private ArrayList<String> mScan1FifoMsgList = null;
	private boolean mDataSetAlready = false;
	private boolean mIsAtBeginning = false;

    private String getFifoMsgList() {
        StringBuilder sb = new StringBuilder();

        for(String str : mScan1FifoMsgList) {
            sb.append(str + "\n");
        }
        return sb.toString();
    }

	private synchronized void setFifoDataToDtAtBeginning() {
		mIsAtBeginning = true;

		setFifoDataToDt();
	}

	private synchronized void setFifoDataToDtRemove() {
        mDataSetAlready = false;
        if(!mIsAtBeginning && null != mScan1FifoMsgList && mScan1FifoMsgList.size() > 0)
            mScan1FifoMsgList.remove(0);
		mIsAtBeginning = false;
        setFifoDataToDt();
    }

    private synchronized void setFifoDataToDt() {
        if(mDataSetAlready) return;

        if(null == mScan1FifoMsgList) {
            mScan1FifoMsgList = new ArrayList<String>();
            return;
        }

        if(mScan1FifoMsgList.size() > 0) {
			if (SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_RS232_9) {
				setSerialProtocol9DTs(mScan1FifoMsgList.get(0));
			} else if (SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_SCANER1_FIFO) {
				setScanDataToDt(mScan1FifoMsgList.get(0));
			}
            mDataSetAlready = true;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                if(null != mRemoteRecvedPromptDlg) {
                    mRemoteRecvedPromptDlg.show();
                    mRemoteRecvedPromptDlg.setMessage(getFifoMsgList());
                }
                }
            });
        }
    }

    private synchronized void setRemoteDataToFifo(String code) {
        Debug.d(TAG, "String from Remote = [" + code + "]");

        if(null == mScan1FifoMsgList) {
            mScan1FifoMsgList = new ArrayList<String>();
        }

        if(mScan1FifoMsgList.contains(code)) return;

        if(mScan1FifoMsgList.size() < SCAN_FIFO_MAX_COUNT) {
            mScan1FifoMsgList.add(code);
            if(!mDataSetAlready) mNeedUpdate = true;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if(null != mRemoteRecvedPromptDlg) {
                        mRemoteRecvedPromptDlg.show();
                        mRemoteRecvedPromptDlg.setMessage(getFifoMsgList());
                    }
                }
            });
        }
    }

// End of H.M.Wang 2021-9-17 追加扫描协议1-FIFO

// H.M.Wang 2021-9-28 追加串口协议10
    public void setSP10DataToDt(final String data) {
        Debug.d(TAG, "String from Remote = [" + data + "]");
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (null != mRemoteRecvedPromptDlg) {
                    mRemoteRecvedPromptDlg.show();
//					mRemoteRecvedPromptDlg.show();
                    mRemoteRecvedPromptDlg.setMessage(data);
                }
            }
        });

        if(data.length() < 14) {                               // 数据长度36
            ToastUtil.show(mContext, R.string.invalid_protocol);
            return;
        }

		boolean needUpdate = false;

		String dt0 = data.substring(8, 12);
// H.M.Wang 2021-10-11 追加第14位赋给DT1的功能
		String dt1 = data.substring(13, 14);
// End of H.M.Wang 2021-10-11 追加第14位赋给DT1的功能

        for(DataTask dataTask : mDataTask) {
            ArrayList<BaseObject> objList = dataTask.getObjList();
            for(BaseObject baseObject: objList) {
                if(baseObject instanceof DynamicText) {
                    int dtIndex = ((DynamicText)baseObject).getDtIndex();
                    if(dtIndex == 0) {
// H.M.Wang 2021-5-21 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，编辑页面显示#####
                        SystemConfigFile.getInstance().setDTBuffer(dtIndex, dt0);
// End of H.M.Wang 2021-5-21 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，编辑页面显示#####
                        baseObject.setContent(dt0);
// H.M.Wang 2021-10-11 追加第14位赋给DT1的功能
					} else if(dtIndex == 1) {
// H.M.Wang 2021-5-21 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，编辑页面显示#####
							SystemConfigFile.getInstance().setDTBuffer(dtIndex, dt1);
// End of H.M.Wang 2021-5-21 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，编辑页面显示#####
							baseObject.setContent(dt1);
// End of H.M.Wang 2021-10-11 追加第14位赋给DT1的功能
                    } else {
                        baseObject.setContent("");
                    }
					needUpdate = true;
                }
            }
        }
		mNeedUpdate = needUpdate;
    }
// End of H.M.Wang 2021-9-28 追加串口协议10

// H.M.Wang 2022-4-5 追加串口协议11(341串口)
	public void setCH341DataToDt(final byte[] data) {
		Debug.d(TAG, "String from Remote = [" + ByteArrayUtils.toHexString(data) + "]");

		byte check = 0x00;

		for(int i=0; i<data.length-3; i++) {
			check += data[i];
		}

		if(check != data[data.length-3]) {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					if (null != mRemoteRecvedPromptDlg) {
						mRemoteRecvedPromptDlg.show();
//					mRemoteRecvedPromptDlg.show();
						mRemoteRecvedPromptDlg.setMessage(ByteArrayUtils.toHexString(data) + "\n" + "Data error");
					}
				}
			});
		} else {
			byte[] result = new byte[data.length-7];

			for(int i=0; i<result.length; i++) {
				result[i] = data[i+4];
			}
			for(int i=0; i<result.length; i++) {
				if(result[i] == 0x30) {
					result[i] = 0x20;
				} else {
					break;
				}
			}

			final String resString = new String(result);

			mHandler.post(new Runnable() {
				@Override
				public void run() {
					if (null != mRemoteRecvedPromptDlg) {
						mRemoteRecvedPromptDlg.show();
//					mRemoteRecvedPromptDlg.show();
						mRemoteRecvedPromptDlg.setMessage(ByteArrayUtils.toHexString(data) + "\n" + resString);
					}
				}
			});

			boolean needUpdate = false;

			for(DataTask dataTask : mDataTask) {
				ArrayList<BaseObject> objList = dataTask.getObjList();
				for(BaseObject baseObject: objList) {
					if(baseObject instanceof DynamicText) {
						int dtIndex = ((DynamicText)baseObject).getDtIndex();
						if(dtIndex == 0) {
// H.M.Wang 2023-3-16 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，因此接收到的数据应该存入桶里，这里是修改遗漏
							SystemConfigFile.getInstance().setDTBuffer(dtIndex, resString);
// End of H.M.Wang 2023-3-16 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，因此接收到的数据应该存入桶里，这里是修改遗漏
							baseObject.setContent(resString);
						}
						needUpdate = true;
					}
				}
			}
			mNeedUpdate = needUpdate;
		}
	}
// End of H.M.Wang 2022-4-5 追加串口协议11(341串口)

// H.M.Wang 2022-12-19 追加一个串口，RS232_DOT_MARKER
    private int mDotMarkerDotCount = 0;
	private static byte[] mDotMarkerRecvBuffer = null;

    public static void setDotMarkerRecvBuffer(byte[] data) {
		mDotMarkerRecvBuffer = data;
		Debug.d(TAG, "DotMarkerRecvData = [" + ByteArrayUtils.toHexString(mDotMarkerRecvBuffer) + "]");
	}

    private char[] getDotMarkerPrintBuffer(boolean save) {
		char[] dotMarkerPrintBuffer;
		int dotMarkerDotCount;

		if (null == mDotMarkerRecvBuffer) {
			mDotMarkerDotCount = 15*32*200;		// 按缺省30列中的一半为有填充数据计算；每列32个点，大字节点数乘200
			dotMarkerPrintBuffer = new char[2];
		} else {
			int length = (mDotMarkerRecvBuffer[0] & 0x0ff);
			dotMarkerDotCount = 0;

			char[] whiteCol = new char[] {0x0000, 0x0000};
			char[] blackCol = new char[] {0xFFFF, 0xFFFF};
			char slantCharsPerCol = 32;

			dotMarkerPrintBuffer = new char[length * 2 * slantCharsPerCol];        // 每列4个字节，length列, 倾斜相当于每隔32列插入一列真值列，其余为空
			for (int i = 0; i < mDotMarkerRecvBuffer.length - 1; i++) {
				int desCol = 0;

				// 当打印方向和打印头1的镜像有一个设为反方向的话，按反方向设置，如果都没设置或者都设了反，则按正向设置
				if((SystemConfigFile.getInstance().getParam(1) ^ SystemConfigFile.getInstance().getParam(12)) == 1) {
					desCol = (mDotMarkerRecvBuffer.length - 2 - i) * slantCharsPerCol;
				} else {
					desCol = i * slantCharsPerCol;
				}

				for(int j=0; j<slantCharsPerCol; j++) {
					if(j == 0 && mDotMarkerRecvBuffer[i + 1] != 0x00) {
						System.arraycopy(blackCol, 0, dotMarkerPrintBuffer, (desCol + j) * 2, blackCol.length);
						dotMarkerDotCount += 32 * 200;
					} else {
						System.arraycopy(whiteCol, 0, dotMarkerPrintBuffer, (desCol + j) * 2, whiteCol.length);
					}
				}
			}
			mDotMarkerDotCount = dotMarkerDotCount;
			if(save) {
				FileUtil.deleteFolder("/mnt/sdcard/print.bin");
				BinCreater.saveBin("/mnt/sdcard/print.bin", dotMarkerPrintBuffer, 4 * 8);
			}
		}

		Debug.d(TAG, "dotMarkerPrintBuffer length=" + dotMarkerPrintBuffer.length);
		return dotMarkerPrintBuffer;
	}

	public void showDotMarkerData(final byte[] data) {
		Debug.d(TAG, "String from Remote = [" + ByteArrayUtils.toHexString(data) + "]");

		mHandler.post(new Runnable() {
			@Override
			public void run() {
				if (null != mRemoteRecvedPromptDlg) {
					mRemoteRecvedPromptDlg.show();
					mRemoteRecvedPromptDlg.setMessage(ByteArrayUtils.toHexString(data));
				}
			}
		});

		mNeedUpdate = true;
	}
// End of H.M.Wang 2022-12-19 追加一个串口，RS232_DOT_MARKER

// H.M.Wang 2023-12-13 追加一个串口协议12
	public void setProtocol12Data(final byte[] data) {
		Debug.d(TAG, "String from Remote = [" + ByteArrayUtils.toHexString(data) + "]");

		final String cnt = new String(data).substring(10,14);

		mHandler.post(new Runnable() {
			@Override
			public void run() {
				if (null != mRemoteRecvedPromptDlg) {
					mRemoteRecvedPromptDlg.show();
					mRemoteRecvedPromptDlg.setMessage(ByteArrayUtils.toHexString(data) + "\n" + cnt);
				}
			}
		});

		for(DataTask dataTask : mDataTask) {
			ArrayList<BaseObject> objList = dataTask.getObjList();
			for(BaseObject baseObject: objList) {
				if(baseObject instanceof DynamicText) {
					int dtIndex = ((DynamicText)baseObject).getDtIndex();
					if(dtIndex == 0) {
						SystemConfigFile.getInstance().setDTBuffer(dtIndex, cnt);
						baseObject.setContent(cnt);
						mNeedUpdate = true;
					}
				}
			}
		}
	}
// End of H.M.Wang 2023-12-13 追加一个串口协议12

//	private AlertDialog mRemoteRecvedPromptDlg = null;
	private RemoteMsgPrompt mRemoteRecvedPromptDlg = null;

	public boolean launch(Context ctx) {
		// H.M.Wang 2019-12-31 设置mContext，以避免因为mContext=null而导致程序崩溃
		mContext = ctx;
		// End of H.M.Wang 2019-12-31 设置mContext，以避免因为mContext=null而导致程序崩溃

		// H.M.Wang 2019-12-19 支持多种串口协议的修改
		// H.M.Wang 2019-10-23 串口发送数据支持
		final SerialHandler serialHandler = SerialHandler.getInstance(mContext);
		serialHandler.setPrintCommandListener(new SerialHandler.OnSerialPortCommandListenner() {
			@Override
			public void onError(final String errCode) {
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						ToastUtil.show(mContext, errCode);
					}
				});
			}
			@Override
			public void onCommandReceived(int cmd, byte[] data) {
				if (SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_RS232_1 ||
					SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_RS232_2 ||
// H.M.Wang 2022-5-16 追加串口协议2无线
					SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_RS232_2_WIFI) {
// End of H.M.Wang 2022-5-16 追加串口协议2无线

					if (cmd == EC_DOD_Protocol.CMD_TEXT) {                         // 发送一条文本	0x0013
// H.M.Wang 2021-5-21 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，编辑页面显示#####
//						ArrayList<BaseObject> objList = mDataTask.get(index()).getObjList();
//						for (BaseObject baseObject : objList) {
//							if (baseObject instanceof DynamicText) {
//								baseObject.setContent("");
//							}
//						}
// End of H.M.Wang 2021-5-21 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，编辑页面显示#####

						String datastring = new String(data, 7, data.length - 7);
						if (SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_RS232_1) {
							setRemoteTextFitCounter(datastring);
						} else if (SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_RS232_2 ||
// H.M.Wang 2022-5-16 追加串口协议2无线
							SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_RS232_2_WIFI) {
// End of H.M.Wang 2022-5-16 追加串口协议2无线
							setRemoteTextSeparated(datastring);
						}
						serialHandler.sendCommandProcessResult(EC_DOD_Protocol.CMD_TEXT, 1, 0, 0, "");
						// H.M.Wang 2019-12-7 反转命令立即生效
					} else if (cmd == EC_DOD_Protocol.CMD_SET_REVERSE) {
						mNeedUpdate = true;
					}
				} else if (SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_RS232_3) {
					String datastring = new String(data, 0, data.length);
					setRemoteTextDirect(datastring);
					serialHandler.sendCommandProcessResult(SerialProtocol.ERROR_SUCESS, 1, 0, 0, datastring + " set.");
				} else if (SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_RS232_4) {
					String datastring = new String(data, 0, data.length);
					setRemoteTextDirect(datastring);
					serialHandler.sendCommandProcessResult(SerialProtocol.ERROR_SUCESS, 1, 0, 0, datastring + " set.");
				} else if (SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_SCANER1) {
					String datastring = new String(data, 0, data.length);
					setScanDataToDt(datastring);
					serialHandler.sendCommandProcessResult(SerialProtocol.ERROR_SUCESS, 1, 0, 0, datastring + " set.");
// H.M.Wang 2020-6-9 追加串口6协议
				} else if (SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_RS232_6) {
					String datastring = new String(data, 0, data.length);
					setSP6DataToDt(datastring);
					serialHandler.sendCommandProcessResult(SerialProtocol.ERROR_SUCESS, 1, 0, 0, datastring + " set.");
// End of H.M.Wang 2020-6-9 追加串口6协议
// H.M.Wang 2020-8-13 追加串口7协议
				} else if (SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_RS232_7) {
					if (cmd == SerialProtocol7.CMD_TEXT) {                         // 发送一条文本	0x0013
// H.M.Wang 2021-5-21 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，编辑页面显示#####
//						ArrayList<BaseObject> objList = mDataTask.get(index()).getObjList();
//						for (BaseObject baseObject : objList) {
//							if (baseObject instanceof DynamicText) {
//								baseObject.setContent("");
//							}
//						}
// End of H.M.Wang 2021-5-21 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，编辑页面显示#####

						String dataString = new String(data, 7, data.length - 7);
						setRemoteTextSeparated(dataString);
						serialHandler.sendCommandProcessResult(SerialProtocol7.CMD_TEXT, 1, 0, 0, "");
						// H.M.Wang 2019-12-7 反转命令立即生效
					} else if (cmd == SerialProtocol7.CMD_SET_REVERSE) {
						mNeedUpdate = true;
					}
// End of H.M.Wang 2020-8-13 追加串口7协议
// H.M.Wang 2021-3-6 追加串口协议8
				} else if (SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_RS232_8) {
					setSerialProtocol8DTs(data);
// End of H.M.Wang 2021-3-6 追加串口协议8
// H.M.Wang 2020-10-30 追加扫描2串口协议
				} else if (SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_SCANER2) {
					String datastring = new String(data, 0, data.length);
					setScan2DataToDt(datastring);
					serialHandler.sendCommandProcessResult(SerialProtocol.ERROR_SUCESS, 1, 0, 0, datastring + " set.");
// End of H.M.Wang 2020-10-30 追加扫描2串口协议
// H.M.Wang 2021-1-15 追加扫描协议3，协议内容与扫描2协议完全一致，仅在打印的时候，仅可以打印一次
				} else if (SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_SCANER3) {
					String datastring = new String(data, 0, data.length);
					setScan2DataToDt(datastring);
					serialHandler.sendCommandProcessResult(SerialProtocol.ERROR_SUCESS, 1, 0, 0, datastring + " set.");
// End of H.M.Wang 2021-1-15 追加扫描协议3，协议内容与扫描2协议完全一致，仅在打印的时候，仅可以打印一次
// H.M.Wang 2021-9-24 追加串口协议9
				} else if (SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_RS232_9) {
					setRemoteDataToFifo(new String(data, 0, data.length));
//					setSerialProtocol9DTs(new String(data, 0, data.length));
// End of H.M.Wang 2021-9-24 追加串口协议9
// H.M.Wang 2021-9-28 追加串口协议10
                } else if (SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_RS232_10) {
                    String datastring = new String(data, 0, data.length);
                    setSP10DataToDt(datastring);
                    serialHandler.sendCommandProcessResult(SerialProtocol.ERROR_SUCESS, 1, 0, 0, datastring + " set.");
// End of H.M.Wang 2021-9-28 追加串口协议10
// H.M.Wang 2022-4-5 追加串口协议11(341串口)
				} else if (SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_RS232_11) {
					setCH341DataToDt(data);
					serialHandler.sendCommandProcessResult(SerialProtocol.ERROR_SUCESS, 1, 0, 0, data);
// End of H.M.Wang 2022-4-5 追加串口协议11(341串口)
// H.M.Wang 2022-12-19 追加一个串口，RS232_DOT_MARKER
				} else if (SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_DOT_MARKER) {
					showDotMarkerData(data);
					serialHandler.sendCommandProcessResult(SerialProtocol.ERROR_SUCESS, 1, 0, 0, data);
// End of H.M.Wang 2022-12-19 追加一个串口，RS232_DOT_MARKER
// H.M.Wang 2023-12-13 追加一个串口协议12
				} else if (SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_RS232_12) {
					setProtocol12Data(data);
					serialHandler.sendCommandProcessResult(SerialProtocol.ERROR_SUCESS, 1, 0, 0, data);
// End of H.M.Wang 2023-12-13 追加一个串口协议12
				}
			}
		});

		// End of H.M.Wang 2019-12-19 支持多种串口协议的修改
// H.M.Wang 2020-10-30 追加数据源判断，启动扫描处理，因为有两个处理从一个扫码枪途径获取数据
		if (SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_SCANER1) {
			BarcodeScanParser.setListener(new BarcodeScanParser.OnScanCodeListener() {
				@Override
				public void onCodeReceived(String code) {
					setScanDataToDt(code);
				}
			});
		} else if (SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_SCANER2) {
			BarcodeScanParser.setListener(new BarcodeScanParser.OnScanCodeListener() {
				@Override
				public void onCodeReceived(String code) {
					setScan2DataToDt(code);
				}
			});
// H.M.Wang 2021-1-15 追加扫描协议3，协议内容与扫描2协议完全一致，仅在打印的时候，仅可以打印一次
		} else if (SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_SCANER3) {
			BarcodeScanParser.setListener(new BarcodeScanParser.OnScanCodeListener() {
				@Override
				public void onCodeReceived(String code) {
					setScan2DataToDt(code);
				}
			});
// End of H.M.Wang 2021-1-15 追加扫描协议3，协议内容与扫描2协议完全一致，仅在打印的时候，仅可以打印一次
// H.M.Wang 2021-5-21 追加扫描协议4
		} else if (SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_SCANER4) {
			mDtIndex = 0;
			mLastRecvString = "";
			BarcodeScanParser.setListener(new BarcodeScanParser.OnScanCodeListener() {
				@Override
				public void onCodeReceived(String code) {
					setScan4DataToDt(code);
				}
			});
// End of H.M.Wang 2021-5-21 追加扫描协议4
// H.M.Wang 2021-9-17 追加扫描协议1-FIFO
        } else if (SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_SCANER1_FIFO) {
            BarcodeScanParser.setListener(new BarcodeScanParser.OnScanCodeListener() {
                @Override
                public void onCodeReceived(String code) {
					setRemoteDataToFifo(code);
                }
            });
// End of H.M.Wang 2021-9-17 追加扫描协议1-FIFO
// H.M.Wang 2024-1-12 增加一个扫描协议5，要点： (1) 不做第二位和最后一位的一致性检查；(2)扫描内容按网络协议650的规范，DT0-DT9,BC的格式，分别保存到桶和条码桶中
		} else if (SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_SCANER5) {
			BarcodeScanParser.setListener(new BarcodeScanParser.OnScanCodeListener() {
				@Override
				public void onCodeReceived(String code) {
					setRemote1TextSeparated(code);
				}
			});
// End of H.M.Wang 2024-1-12 增加一个扫描协议5，要点： (1) 不做第二位和最后一位的一致性检查；(2)扫描内容按网络协议650的规范，DT0-DT9,BC的格式，分别保存到桶和条码桶中
		}
// End of H.M.Wang 2020-10-30 追加数据源判断，启动扫描处理，因为有两个处理从一个扫码枪途径获取数据

// H.M.Wang 2020-11-4 取消使用标准对话窗，改为自己定义对话窗，这样可以自由设置文字大小
/*
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		mRemoteRecvedPromptDlg = builder.setTitle(R.string.strRecvedRemote).setMessage("").setPositiveButton(R.string.str_ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
//				WindowManager.LayoutParams attrs = mRemoteRecvedPromptDlg.getWindow().getAttributes();
//				attrs.width = WindowManager.LayoutParams.MATCH_PARENT;// attrs.width =580;
//				attrs.height = WindowManager.LayoutParams.MATCH_PARENT;// attrs.height = 600;
//				mRemoteRecvedPromptDlg.getWindow().setAttributes(attrs);

				LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
				layoutParams.setMargins(30,30,30,30);//4个参数按顺序分别是左上右下

				mRemoteRecvedPromptDlg.getWindow().getDecorView().setLayoutParams(layoutParams);

				try {
					Field mAlert = AlertDialog.class.getDeclaredField("mAlert");
					mAlert.setAccessible(true);
					Object mAlertController = mAlert.get(dialog);
					Field mMessage = mAlertController.getClass().getDeclaredField("mMessageView");
					mMessage.setAccessible(true);
					TextView mMessageView = (TextView) mMessage.get(mAlertController);
					mMessageView.setTextSize(mMessageView.getTextSize() + 20);
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (NoSuchFieldException e) {
					e.printStackTrace();
				}
				mRemoteRecvedPromptDlg.hide();
			}
		}).create();
*/
		mRemoteRecvedPromptDlg = new RemoteMsgPrompt(mContext);
// End of H.M.Wang 2020-11-4 取消使用标准对话窗，改为自己定义对话窗，这样可以自由设置文字大小

// H.M.Wang 2020-6-3 解决提示对话窗在显示时，扫码枪的信息被其劫持，而无法识别的问题
        mRemoteRecvedPromptDlg.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				if(event.getAction() == KeyEvent.ACTION_DOWN) {
					if(keyCode == KeyEvent.KEYCODE_ENTER) {
						return true;
					} else {
						BarcodeScanParser.append(keyCode, event.isShiftPressed());
					}
				}
                return false;
            }
        });
// End of H.M.Wang 2020-6-3 解决提示对话窗在显示时，扫码枪的信息被其劫持，而无法识别的问题

		if (!isBufferReady || mDataTask == null) {
			return false;
		}

// H.M.Wang 2021-5-6 只有在FIFO的size大于1，并且不是群组打印的时候，才启动该标识
		mUsingFIFO = (SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_FIFO_SIZE) > 1) && (mDataTask.size() == 1);
		mPrintedCount = 0;
// End of H.M.Wang 2021-5-6 只有在FIFO的size大于1，并且不是群组打印的时候，才启动该标识
// H.M.Wang 2023-10-20 追加下发总数计数
		mDownWrittenCount = 0;
		mLastPrintedCount = 0;
// End of H.M.Wang 2023-10-20 追加下发总数计数

// H.M.Wang 2023-2-13 增加一个工作模式，使用外接U盘当中的文件作为DT的数据源来打印。后续使用哪个方法
		if(TxtDT.getInstance(mContext).isTxtDT()) {
			TxtDT.getInstance(mContext).startPrint();
		}
// End of H.M.Wang 2023-2-13 增加一个工作模式，使用外接U盘当中的文件作为DT的数据源来打印

		mPrinter = new PrintTask();
		mPrinter.start();

		mRunning = true;

//		thread.start();
		return true;
	}

// H.M.Wang 2021-5-6 追击是否正在使用FIFO的标识，用来控制当使用计数器的时候，如果FIFO当中还残留未打印的任务，会导致计数器在下次开始打印时跳数
	private boolean mUsingFIFO = false;
	private int mPrintedCount = 0;
// End of H.M.Wang 2021-5-6 追击是否正在使用FIFO的标识，用来控制当使用计数器的时候，如果FIFO当中还残留未打印的任务，会导致计数器在下次开始打印时跳数

	public void finish() {
		mRunning = false;

		while(!mStopped) {try {Thread.sleep(1);} catch(Exception e){}}

		if(null != mRemoteRecvedPromptDlg) {
			mRemoteRecvedPromptDlg.dismiss();
			mRemoteRecvedPromptDlg = null;
		}
		// H.M.Wang 2019-10-23 串口发送数据支持
		SerialHandler serialHandler =  SerialHandler.getInstance(mContext);
		serialHandler.setPrintCommandListener(null);
		// End --------------------

		BarcodeScanParser.setListener(null);

// H.M.Wang 2023-2-13 增加一个工作模式，使用外接U盘当中的文件作为DT的数据源来打印。后续使用哪个方法
		if(TxtDT.getInstance(mContext).isTxtDT()) {
			TxtDT.getInstance(mContext).stopPrint();
		}
// End of H.M.Wang 2023-2-13 增加一个工作模式，使用外接U盘当中的文件作为DT的数据源来打印
// H.M.Wang 2023-7-24 停止打印时，清空PC_FIFO
		PC_FIFO pc_FIFO = PC_FIFO.getInstance(mContext);
		if(pc_FIFO.PCFIFOEnabled()) {
			pc_FIFO.clearBuffer();
		}
// End of H.M.Wang 2023-7-24 停止打印时，清空PC_FIFO

		PrintTask t = mPrinter;
		mPrinter = null;
		mHandler.removeMessages(MESSAGE_DATA_UPDATE);
		if (t != null) {
			t.interrupt();
		}
		if (mScheduler == null) {
			return;
		}
		mScheduler.doAfterPrint();
	}
	
	public void setOnInkChangeListener(InkLevelListener listener) {
		mInkListener = listener;
	}
	
	
	public static final int MESSAGE_DATA_UPDATE = 1;
// H.M.Wang 2022-11-8 添加一个显示Bagink当中Level值的信息框
	public static final int MESSAGE_SHOW_LEVEL = 101;
	public static final int MESSAGE_LEVEL_ERROR = 102;
// End of H.M.Wang 2022-11-8 添加一个显示Bagink当中Level值的信息框

	public Handler mHandler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case MESSAGE_DATA_UPDATE:
					mNeedUpdate = true;
					break;
// H.M.Wang 2022-11-8 添加一个显示Bagink当中Level值的信息框
				case MESSAGE_LEVEL_ERROR:
					mCallback.onError(true);
				case MESSAGE_SHOW_LEVEL:
					if (null != mRecvedLevelPromptDlg) {
						mRecvedLevelPromptDlg.setTitle("Levels");
						mRecvedLevelPromptDlg.setMessage((String)msg.obj);
						mRecvedLevelPromptDlg.show();
						mRecvedLevelPromptDlg.show();
					}
					break;
// End of H.M.Wang 2022-11-8 添加一个显示Bagink当中Level值的信息框
			}
		}
	};

	public void resetTask(List<MessageTask> task) {
		synchronized (DataTransferThread.class) {
			mIndex = 0;
			mDataTask.clear();
			for (MessageTask t : task) {
				DataTask data = new DataTask(mContext, t);
				mDataTask.add(data);
				data.prepareBackgroudBuffer();
			}
			setDotCount(task);
			mNeedUpdate = true;
		}
	}

	public void initDataBuffer(Context context, List<MessageTask> task) {
		if (mDataTask == null) {
			mDataTask = new ArrayList<DataTask>();
		}
		mIndex = 0;
		mDataTask.clear();
		for (MessageTask t : task) {
			DataTask data = new DataTask(mContext, t);
			mDataTask.add(data);
		}
		Debug.d(TAG, "--->prepare buffer: " + mDataTask.size());

		for (DataTask tk : mDataTask) {
			isBufferReady |= tk.prepareBackgroudBuffer();
		}

		if (mScheduler == null) {
			mScheduler = InkSchedulerFactory.getScheduler(mContext);
		}

		SystemConfigFile configFile = SystemConfigFile.getInstance(context);

		int headIndex = configFile.getParam(SystemConfigFile.INDEX_HEAD_TYPE);
		int heads = PrinterNozzle.getInstance(headIndex).mHeads;
// H.M.Wang 2022-11-8 添加一个显示Bagink当中Level值的信息框
		if(PlatformInfo.getImgUniqueCode().startsWith("BAGINK") && (mScheduler instanceof RfidScheduler)) {
			((RfidScheduler) mScheduler).setCallbackHandler(mHandler);
		}
		if(PlatformInfo.getImgUniqueCode().startsWith("BAGINK") && (mScheduler instanceof N_RfidScheduler)) {
			((N_RfidScheduler) mScheduler).setCallbackHandler(mHandler);
		}
// End of H.M.Wang 2022-11-8 添加一个显示Bagink当中Level值的信息框
		/**如果是4合2的打印头，需要修改为4头*/
		mScheduler.init(heads * configFile.getHeadFactor());
//		heads = configFile.getParam(SystemConfigFile.INDEX_SPECIFY_HEADS) > 0 ? configFile.getParam(42) : heads;
	}


	public List<DataTask> getData() {
		synchronized (DataTransferThread.class) {
			return mDataTask;
		}
	}

	public DataTask getCurData() {
		return mDataTask.get(index());
	}

	public void setDotCount(List<MessageTask> messages) {
		if (isLanPrint()) return;
		for (int i = 0; i < mDataTask.size(); i++) {
			DataTask t = mDataTask.get(i);
			if (messages.size() <= i) {
				break;
			}
			int[] dots = messages.get(i).getDots();
			int totalDot = 0;
			for (int j = 0; j < dots.length; j++) {
				totalDot += dots[j];
			}
			t.setDots(totalDot);
			t.setDotsEach(dots);
		}
		initCount();
	}
	
	public int getDotCount(DataTask task, int head) {
		if (task == null) {
			Debug.e(TAG, "---> task is null");
			return 1;
		}
			
		return task.getDots(head);
	}
	
	
	public void initCount() {
		if (mcountdown == null) {
// H.M.Wang 2023-12-3 修改锁值记录方法。阈值计数器修改为浮点型，以便于管理调整后的阈值（必须为浮点型，否则不准确）
//			mcountdown = new int[8];
			mcountdown = new float[8];
// End of H.M.Wang 2023-12-3 修改锁值记录方法。阈值计数器修改为浮点型，以便于管理调整后的阈值（必须为浮点型，否则不准确）
// H.M.Wang 2019-10-10 取消内部初始化0的操作，这样不全面
//		} else {
//			for (int i = 0; i < mcountdown.length; i++) {
//				mcountdown[i] = 0;
//			}
		}

		// H.M.Wang 初始化0的部分移到外部，这样更全面
		for (int i = 0; i < mcountdown.length; i++) {
			mcountdown[i] = 0;
		}

		Arrays.fill(mThresHolds, 0);
		// H.M.Wang 2019-10-10 注释掉添加初值的部分，如果初值为0，则表示该处置还没有初始化，待后续计算后添加
//		for (int i = 0; i < mcountdown.length; i++) {
//			mcountdown[i] = getInkThreshold(i);
//			//Debug.d(TAG, "--->initCount countdown[" + i + "] = " + mcountdown[i]);
//		}
	}

// 2023-1-18 追加这个函数，目的是在第一次生成打印缓冲区后，重新计算阈值和计数器
	private void recalCount() {
		if (mcountdown == null) {
// H.M.Wang 2023-12-3 修改锁值记录方法。阈值计数器修改为浮点型，以便于管理调整后的阈值（必须为浮点型，否则不准确）
//			mcountdown = new int[8];
			mcountdown = new float[8];
// End of H.M.Wang 2023-12-3 修改锁值记录方法。阈值计数器修改为浮点型，以便于管理调整后的阈值（必须为浮点型，否则不准确）
		}

		for (int i = 0; i < mcountdown.length; i++) {
			mcountdown[i] = getInkThreshold(i);
			//Debug.d(TAG, "--->initCount countdown[" + i + "] = " + mcountdown[i]);
		}
	}
// End of 2023-1-18 追加这个函数，目的是在第一次生成打印缓冲区后，重新计算阈值和计数器

	/**
	 * 倒计数，当计数倒零时表示墨水量需要减1，同时倒计数回归
	 * @return true 墨水量需要减1； false 墨水量不变
	 */
	private void countDown() {
// H.M.Wang 2020-10-23 修改计算Threshold的算法，改为以打印群组的所有任务的点数为准，单独任务作为一个元素的特殊群组
		if(mIndex > 0) return;
// End of H.M.Wang 2020-10-23 修改计算Threshold的算法，改为以打印群组的所有任务的点数为准，单独任务作为一个元素的特殊群组
// H.M.Wang 2024-2-3 当使用SmartCard的时候，墨袋对应的头的数量也要减记
		int count = mScheduler.count();
		IInkDevice scm = InkManagerFactory.inkManager(mContext);
		if(scm instanceof SmartCardManager) {
			count++;
		}
		for (int i = 0; i < count; i++) {
//		for (int i = 0; i < mScheduler.count(); i++) {
// End of H.M.Wang 2024-2-3 当使用SmartCard的时候，墨袋对应的头的数量也要减记
// H.M.Wang 2024-1-9 取消在此处判断计数是否需要填充一次阈值的操作，因为：(1)开始打印的时候通过recalCount函数已经预填数据，因此这里不需要再填充；(2)由于本函数后部有判断填充操作，因此在打印过程中，这里无需再次判断
/*
// H.M.Wang 2023-12-3 修改锁值记录方法。修改阈值计数的方法，>=1时减1，<1时重新添加阈值
			// H.M.Wang 2019-10-10 添加初值是否为0的判断，如果为0，则判定为还没有初始化，首先进行初始化
//			if(mcountdown[i] == 0) mcountdown[i] = getInkThreshold(i);
			if(mcountdown[i] < 1.0f) mcountdown[i] += getInkThreshold(i);
// End of H.M.Wang 2023-12-3 修改锁值记录方法。修改阈值计数的方法，>=1时减1，<1时重新添加阈值
*/
// End of H.M.Wang 2024-1-9 取消在此处判断计数是否需要填充一次阈值的操作，因为：(1)开始打印的时候通过recalCount函数已经预填数据，因此这里不需要再填充；(2)由于本函数后部有判断填充操作，因此在打印过程中，这里无需再次判断

			Debug.d(TAG, "mCountDown[" + i + "] = " + mcountdown[i]);

// H.M.Wang 2023-12-3 修改锁值记录方法。修改阈值计数的方法，>=1时减1，<1时重新添加阈值
//			mcountdown[i]--;
			mcountdown[i] -= 1.0f;

//			if (mcountdown[i] <= 0) {
//				mcountdown[i] = getInkThreshold(i);
// H.M.Wang 2024-1-9 由于阈值可能小于1，即打印一次需要减锁数次才符合要求，因此，这里需要重复获取阈值并且减锁才可以。
//			if (mcountdown[i] < 1.0f) {
			while (mcountdown[i] <= 0.0f) {
// End of H.M.Wang 2024-1-9 由于阈值可能小于1，即打印一次需要减锁数次才符合要求，因此，这里需要重复获取阈值并且减锁才可以。
				mcountdown[i] += getInkThreshold(i);
// End of H.M.Wang 2023-12-3 修改锁值记录方法。修改阈值计数的方法，>=1时减1，<1时重新添加阈值
				mInkListener.onInkLevelDown(i);
			}
		}
// H.M.Wang 2023-10-22 由于主界面显示的打印计数已经修改为实际打印的数量，因此，修改打印计数器的操作改为PrintTask中获取实际打印数量的流程中调用，这里取消
//		mInkListener.onCountChanged();
// End of H.M.Wang 2023-10-22 由于主界面显示的打印计数已经修改为实际打印的数量，因此，修改打印计数器的操作改为PrintTask中获取实际打印数量的流程中调用，这里取消
		mScheduler.schedule();
	}

// H.M.Wang 2023-12-3 修改锁值记录方法。阈值计数器修改为浮点型，以便于管理调整后的阈值（必须为浮点型，否则不准确）
//	public int getCount(int head) {
	public float getCount(int head) {
// End of H.M.Wang 2023-12-3 修改锁值记录方法。阈值计数器修改为浮点型，以便于管理调整后的阈值（必须为浮点型，否则不准确）
		if (mcountdown == null) {
			initCount();
		}
		return mcountdown[head];
	}

// H.M.Wang 2020-10-23 修改计算Threshold的算法，改为以打印群组的所有任务的点数为准，单独任务作为一个元素的特殊群组
	private int[] mPrintDots = new int[8];
// End of H.M.Wang 2020-10-23 修改计算Threshold的算法，改为以打印群组的所有任务的点数为准，单独任务作为一个元素的特殊群组
// H.M.Wang 2021-1-25 追加Threshold的保存，当处于快速打印（根据FIFO判断）时，不再计算，直接返回值，但这个对群组无效，因此只能适应快速打印
// H.M.Wang 2023-12-3 修改锁值记录方法。阈值计数器修改为浮点型，以便于管理调整后的阈值（必须为浮点型，否则不准确）
//	private int[] mThresHolds = new int[8];
	private float[] mThresHolds = new float[8];
// End of H.M.Wang 2023-12-3 修改锁值记录方法。阈值计数器修改为浮点型，以便于管理调整后的阈值（必须为浮点型，否则不准确）
// H.M.Wang 2021-1-25 追加Threshold的保存，当处于快速打印（根据FIFO判断）时，不再计算，直接返回值，但这个对群组无效，因此只能适应快速打印

// H.M.Wang 2023-1-17 追加这个函数，用来避免每次显示剩余次数时都要重新计算阈值（以前调用的是getInkThreshold，所以会实际计算
// H.M.Wang 2023-12-3 修改锁值记录方法。阈值计数器修改为浮点型，以便于管理调整后的阈值（必须为浮点型，否则不准确）
//	public int getKeptInkThreshold(int head) {
	public float getKeptInkThreshold(int head) {
// End of H.M.Wang 2023-12-3 修改锁值记录方法。阈值计数器修改为浮点型，以便于管理调整后的阈值（必须为浮点型，否则不准确）
		return mThresHolds[head];
	}
// End of H.M.Wang 2023-1-17 追加这个函数，用来避免每次显示剩余次数时都要重新计算阈值（以前调用的是getInkThreshold，所以会实际计算
	/**
	 * 通过dot count计算RFID减1的阀值
	 * @param head 喷头索引
	 * @return
	 */
// H.M.Wang 2023-12-3 修改锁值记录方法。阈值计数器修改为浮点型，以便于管理调整后的阈值（必须为浮点型，否则不准确）
//	public int getInkThreshold(int head) {
	public float getInkThreshold(int head) {
// End of H.M.Wang 2023-12-3 修改锁值记录方法。阈值计数器修改为浮点型，以便于管理调整后的阈值（必须为浮点型，否则不准确）
		//if (isLanPrint()) return 1;
		float bold = 1.0f;
		int index = isLanPrint() ? 0 : index();

// H.M.Wang 2024-2-3 当使用SmartCard的时候，墨袋对应的是墨盒(1个或者2个)后面的头来显示在主页面，但是在计算这个对应头的阈值的时候，因为这个头实际上是没有真实头的，所以没有打印数据，点数为0，因此阈值会成为一个固定数65536*8
// 这会导致两个问题：(1) 开始打印时显示的墨袋(B)的初始剩余次数为一个固定值524288(65536*8)，(2) 打印过程当中减记打印次数时墨袋的次数不被减记。对此问题修改为：
//     (1) 对应于墨袋的阈值按下列公式结算： threshold[B] = threshold[P1] * threshold[P2] / (threshold[P1] * threshold[P2])
//     (2) 在countDown函数中，减记实际头的次数时也同时减记墨袋的剩余次数
		IInkDevice scm = InkManagerFactory.inkManager(mContext);
		if(scm instanceof SmartCardManager) {
			if(head == ((SmartCardManager)scm).getInkCount()-1) {
				if(head == 1) {
					mThresHolds[head] = mThresHolds[0];
				} else if(head == 2) {
					mThresHolds[head] = mThresHolds[0] * mThresHolds[1] / (mThresHolds[0] + mThresHolds[1]);
				} else {
					mThresHolds[head] = 65536 * 8;
				}
				return mThresHolds[head];
			}
		}
// End of H.M.Wang 2024-2-3 当使用SmartCard的时候，墨袋对应的是墨盒(1个或者2个)后面的头来显示在主页面，但是在计算这个对应头的阈值的时候，因为这个头实际上是没有真实头的，所以没有打印数据，点数为0，因此阈值会成为一个固定数65536*8

//		int dotCount = getDotCount(mDataTask.get(index), head);
		SystemConfigFile config = SystemConfigFile.getInstance(mContext);
		if(config.getParam(SystemConfigFile.INDEX_FIFO_SIZE) > 1 && mThresHolds[head] > 0) {
			return mThresHolds[head];
		}
// H.M.Wang2019-9-28 考虑1带多的情况
//		int one2multiple = SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_ONE_MULTIPLE);
//		if( one2multiple == 2 || 		// 1带2
//			one2multiple == 3 || 		// 1带3
//			one2multiple == 4  			// 1带4
//		) {
//			dotCount = getDotCount(mDataTask.get(index), 0);		// 使用第一个头的数据
//		}

// H.M.Wang 2020-10-23 修改计算Threshold的算法，改为以打印群组的所有任务的点数为准，单独任务作为一个元素的特殊群组
// Kevin.Zhao 2019-11-12 1带多用12，13，14表示1带2，1带3，1带4....
//		int dotCount = getDotCount(mDataTask.get(index), config.getMainHeads(head));
// H.M.Wang 2022-12-20 这个遍历8个头的算法似乎有问题，求的是头head的threshold，但是这个头可能是依附于其他头的，这个靠getMainHeads取得就可以了，没有必要遍历所有头
// 原来的调用getDotCount(mDataTask.get(j), i)，获取的是当前头的数据，而非主头的数据，这些附属头的数据，当然，当前头的数据，由于生成的时候已经填入了（1对多在BinInfo的extend函数，其他的实在DataTask的getPrintBuffer函数的后部
// 因此，获取当前头的数据也可以得到正确的数值。在PrintTask.run函数中，显示各个头的点数的时候，也调整了从主头读取数据，所以运行的时候也不会出错，只是这里的处理逻辑有点不对
/*		head = config.getMainHeads(head);
		for(int i=0; i<8; i++) {
			mPrintDots[i] = 0;
			for(int j=0; j<mDataTask.size(); j++) {
				mPrintDots[i] += getDotCount(mDataTask.get(j), i);
				Debug.d(TAG, "--->dotCount[" + i + "]: " + mPrintDots[i] + "  task=" + j);
			}
		}
*/
		mPrintDots[head] = 0;
		for(int j=0; j<mDataTask.size(); j++) {

// H.M.Wang 2022-12-19 追加一个串口，RS232_DOT_MARKER
			if (config.getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_DOT_MARKER) {
				mPrintDots[head] += mDotMarkerDotCount;
			} else {
				mPrintDots[head] += getDotCount(mDataTask.get(j), config.getMainHeads(head));
			}
// End of H.M.Wang 2022-12-19 追加一个串口，RS232_DOT_MARKER
			Debug.d(TAG, "--->dotCount[" + head + "]: " + mPrintDots[head] + "  task=" + j);
		}
// End of H.M.Wang 2022-12-20 这个遍历8个头的算法似乎有问题，求的是头head的threshold，但是这个头可能是依附于其他头的，这个靠getMainHeads取得就可以了，没有必要遍历所有头

		Debug.d(TAG, "--->dotCount[" + head + "]: " + mPrintDots[head] + "  bold=" + bold);

// H.M.Wang 2020-10-23 修改计算Threshold的算法，改为以打印群组的所有任务的点数为准，单独任务作为一个元素的特殊群组


//		dotCount = dotCount/config.getHeadFactor();		// ??????为什么要除以头数
		// Debug.d(TAG, "--->getInkThreshold  head: " + head + "   index = " + index + " dataTask: " + mDataTask.size());
		if (mPrintDots[head] <= 0) {
// H.M.Wang 2019-9-28 当该打印头没有数据可打印的时候，原来返回1，会产生错误效果，返回一个尽量大的数以避免之
//			return 1;
			return 65536 * 8;						// 无打印内容时可能错误减记
		}

		float rate = 1.0f;

		if (config.getParam(SystemConfigFile.INDEX_PRINT_DENSITY) <= 0) {
			bold = 1.0f;
		} else {
// H.M.Wang 2020-6-12 16,32,64点头减锁修改为不受分辨率影响
			final int headIndex = config.getParam(SystemConfigFile.INDEX_HEAD_TYPE);
			final PrinterNozzle hType = PrinterNozzle.getInstance(headIndex);
//			if (hType != PrinterNozzle.MESSAGE_TYPE_16_DOT && hType != PrinterNozzle.MESSAGE_TYPE_32_DOT && hType != PrinterNozzle.MESSAGE_TYPE_64_DOT) {
			if (hType != PrinterNozzle.MESSAGE_TYPE_16_DOT &&
				hType != PrinterNozzle.MESSAGE_TYPE_32_DOT &&
// H.M.Wang 2020-7-23 追加32DN打印头
				hType != PrinterNozzle.MESSAGE_TYPE_32DN &&
// End of H.M.Wang 2020-7-23 追加32DN打印头
// H.M.Wang 2020-8-18 追加32SN打印头
                hType != PrinterNozzle.MESSAGE_TYPE_32SN &&
// End of H.M.Wang 2020-8-18 追加32SN打印头
// H.M.Wang 2020-8-26 追加64SN打印头
                hType != PrinterNozzle.MESSAGE_TYPE_64SN &&
// End of H.M.Wang 2020-8-26 追加64SN打印头
// H.M.Wang 2022-10-19 追加64SLANT头
				hType != PrinterNozzle.MESSAGE_TYPE_64SLANT &&
// End of H.M.Wang 2022-10-19 追加64SLANT头
// H.M.Wang 2022-5-27 追加32x2头类型
				hType != PrinterNozzle.MESSAGE_TYPE_32X2 &&
// End of H.M.Wang 2022-5-27 追加32x2头类型
				hType != PrinterNozzle.MESSAGE_TYPE_64_DOT &&
// H.M.Wang 2023-7-29 追加48点头
				hType != PrinterNozzle.MESSAGE_TYPE_48_DOT &&
// End of H.M.Wang 2023-7-29 追加48点头
// H.M.Wang 2021-8-16 追加96DN头
				hType != PrinterNozzle.MESSAGE_TYPE_96DN) {
// End of H.M.Wang 2021-8-16 追加96DN头
// H.M.Wang 2021-7-9 300dpi的时候生成的打印图案会比原来宽一倍，参数设置为300dpi的时候，返回值会差一倍，最如下修正
//				bold = config.getParam(SystemConfigFile.INDEX_PRINT_DENSITY)/150;
				if(Configs.GetDpiVersion() == FpgaGpioOperation.DPI_VERSION_300) {
// H.M.Wang 2021-8-25 追加E5X48和E5X50头类型
//					if(hType == PrinterNozzle.MESSAGE_TYPE_E6X50 || hType == PrinterNozzle.MESSAGE_TYPE_E6X48 || hType == PrinterNozzle.MESSAGE_TYPE_E6X1) {
					if(hType == PrinterNozzle.MESSAGE_TYPE_E6X50 ||
						hType == PrinterNozzle.MESSAGE_TYPE_E6X48 ||
						hType == PrinterNozzle.MESSAGE_TYPE_E5X48 ||
						hType == PrinterNozzle.MESSAGE_TYPE_E5X50 ||
						hType == PrinterNozzle.MESSAGE_TYPE_E6X1) {
// End of H.M.Wang 2021-8-25 追加E5X48和E5X50头类型
						// 由于已经通过对锁的调整进行了对应，因此，这里不做更改
						bold = 1.0f * config.getParam(SystemConfigFile.INDEX_PRINT_DENSITY)/150;
					} else {
						bold = 1.0f * config.getParam(SystemConfigFile.INDEX_PRINT_DENSITY)/300;
					}
				} else {
					bold = 1.0f * config.getParam(SystemConfigFile.INDEX_PRINT_DENSITY)/150;
				}
// H.M.Wang 2021-7-9 300dpi的时候生成的打印图案会比原来宽一倍，参数设置为300dpi的时候，返回值会差一倍，最如下修正
			} else {
				bold = 1.0f;
// H.M.Wang 2020-10.17 大字机墨水消耗计算， 加入墨点大小修正
//                rate = Math.max(0.5f, ((1.0f * config.getParam(SystemConfigFile.INDEX_DOT_SIZE)-450)*4+1000)/1200);
// H.M.Wang 2020-11-24 修改计算公式
// H.M.Wang 2022-9-29 修改下列计算公式，取参数35和参数33的最大值
//				rate = Math.max(0.5f, (1.0f * config.getParam(SystemConfigFile.INDEX_DOT_SIZE)+640)/1600);
				rate = Math.max(0.5f, (1.0f * Math.max(config.getParam(SystemConfigFile.INDEX_STR), config.getParam(SystemConfigFile.INDEX_DOT_SIZE))+640)/1600);
// End of H.M.Wang 2022-9-29 修改下列计算公式，取参数35和参数33的最大值
			}
// End of H.M.Wang 2020-10.17 大字机墨水消耗计算， 加入墨点大小修正
// End of H.M.Wang 2020-6-12 16,32,64点头减锁修改为不受分辨率影响
		}

// H.M.Wang 2020-4-19 追加12.7R5头。dotcount放大相应倍数
// H.M.Wang 2020-5-9 12.7R5d打印头类型不参与信息编辑，因此不通过信息的打印头类型判断其是否为12.7R5的信息，而是通过参数来规定现有信息的打印行为
// H.M.Wang 2020-5-21 12.7R5头改为RX48，追加RX50头
//		if(config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_12_7_R5) {
		if(config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_R6X48 ||
			config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_R6X50) {
//		if(mDataTask.get(index).getPNozzle() == PrinterNozzle.MESSAGE_TYPE_12_7_R5) {
// End of H.M.Wang 2020-5-9 12.7R5d打印头类型不参与信息编辑，因此不通过信息的打印头类型判断其是否为12.7R5的信息，而是通过参数来规定现有信息的打印行为
			mPrintDots[head] *= (PrinterNozzle.R6_PRINT_COPY_NUM - 1);
// End of H.M.Wang 2020-5-21 12.7R5头改为RX48，追加RX50头
        }
// End of H.M.Wang 2020-4-19 追加12.7R5头。dotcount放大相应倍数

// H.M.Wang 2021-3-6 追加E6X48,E6X50头
		if( config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E6X48 ||
			config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E6X50) {
			mPrintDots[head] *= PrinterNozzle.E6_PRINT_COPY_NUM;
		}
// End of H.M.Wang 2021-3-6 追加E6X48,E6X50头
// H.M.Wang 2021-8-25 追加E5X48和E5X50头类型
		if( config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E5X48 ||
			config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E5X50) {
			mPrintDots[head] *= PrinterNozzle.E5_PRINT_COPY_NUM;
		}
// End of H.M.Wang 2021-8-25 追加E5X48和E5X50头类型

// H.M.Wang 2022-11-16 取消这个点数的调整，因为DataTask在生成打印缓冲区的时候已经重复生成过了，这里在计算会导致重复计算，以致于将10扩大到100
// H.M.Wang 2021-7-26 追加重复打印时的打印点数计数值的计算
//		if( config.getParam(SystemConfigFile.INDEX_PRINT_TIMES) > 1 && config.getParam(SystemConfigFile.INDEX_PRINT_TIMES) < 21 ) {
//			mPrintDots[head] *= config.getParam(SystemConfigFile.INDEX_PRINT_TIMES);
//		}
// End of H.M.Wang 2021-7-26 追加重复打印时的打印点数计数值的计算
// End of H.M.Wang 2022-11-16 取消这个点数的调整，因为DataTask在生成打印缓冲区的时候已经重复生成过了，这里在计算会导致重复计算，以致于将10扩大到100

		Debug.d(TAG, "--->dotCount[" + head + "]: " + mPrintDots[head] + "  bold=" + bold + "  dotrate=" + rate);

// H.M.Wang 2023-12-3 修改锁值记录方法。阈值计数器修改为浮点型，以便于管理调整后的阈值（必须为浮点型，否则不准确）
//		mThresHolds[head] = (int)(1.0f * Configs.DOTS_PER_PRINT/(mPrintDots[head] * bold)/rate);
//		return (int)(1.0f * Configs.DOTS_PER_PRINT/(mPrintDots[head] * bold)/rate);
		mThresHolds[head] = 1.0f * Configs.DOTS_PER_PRINT/(mPrintDots[head] * bold) / rate * scm.getMaxRatio(head);
// H.M.Wang 2024-1-8 计算时考虑双列的设置，如果设置了双列，因为同样内容要被打印两次，所以要消耗墨水量也要加倍(对应于threshold减少)
		mThresHolds[head] /= (config.getParam(SystemConfigFile.INDEX_DUAL_COLUMNS) > 0 ? 2 : 1);
// End of H.M.Wang 2024-1-8 计算时考虑双列的设置，如果设置了双列，因为同样内容要被打印两次，所以要消耗墨水量也要加倍
		Debug.d(TAG, "mThresHolds[" + head + "]: " + mThresHolds[head]);
		return mThresHolds[head];
// End of H.M.Wang 2023-12-3 修改锁值记录方法。阈值计数器修改为浮点型，以便于管理调整后的阈值（必须为浮点型，否则不准确）
	}
	
	public int getHeads() {
		if (mDataTask != null && mDataTask.size() > 0) {
			return mDataTask.get(0).getPNozzle().mHeads;
		}
		return 1;
	}
	/**
	 * 打印間隔0~100ms（每秒鐘打印 > 20次），爲高速打印，每個打印間隔只執行1步操作
	 * 打印間隔100~200ms（每秒鐘打印 > 20次），爲高速打印，每個打印間隔只執行2步操作
	 * 打印間隔200~500ms（每秒鐘打印 > 20次），爲高速打印，每個打印間隔只執行4步操作
	 * 打印間隔500~1000ms（每秒鐘打印 > 20次），爲高速打印，每個打印間隔只執行8步操作
	 * @return
	 */
	public static long getInterval() {
		if (mInterval >= 1000) {
			return 8;
		} else if (mInterval >= 500) {
			return 4;
		} else if (mInterval >= 200) {
			return 2;
		} else {
			return 1;
		}
	}

	
	private Callback mCallback;
	
	public void setCallback(Callback callback) {
		mCallback = callback;
	}
	
	
	public interface Callback {
		/**
		 * 整個任務打印完成
		 */
		public void onFinished(int code);
		/**
		 * 一個任務打印完成
		 */
		public void onComplete(int index);

// H.M.Wang 2023-10-26 将原来的onPrinted修改为onPrinted0000，用来完成以前onPrinted的功能。增加一个新的onPrinted0002，仅在INDEX_FEEDBACK=1的时候，回送给PC相应的信息
		void onPrinted0000(int index);
		void onPrinted0002(int index);
// End of H.M.Wang 2023-10-26 将原来的onPrinted修改为onPrinted0000，用来完成以前onPrinted的功能。增加一个新的onPrinted0002，仅在INDEX_FEEDBACK=1的时候，回送给PC相应的信息

        void onPrint(int index);

// H.M.Wang 2022-11-10 追加一个中止回调，主要是在RfidScheduler类中如果初始化Bagink的Level时出现Feature6的值不合法，则中止打印的执行。当然，也可以用于其他的中止打印的操作
		void onError(boolean cancel);
// End of H.M.Wang 2022-11-10 追加一个中止回调，主要是在RfidScheduler类中如果初始化Bagink的Level时出现Feature6的值不合法，则中止打印的执行
	}
	
	public static final int CODE_BARFILE_END = 1;
	public static final int CODE_NO_BARFILE = 2;
	public static final int CODE_PRINT_FAILED = 3;

//	private char[] getPrintBuffer() {
//		char[] buffer;
//		int htype = getHeads();
//		// specific process for 9mm header
//		if (htype == MessageType.MESSAGE_TYPE_9MM) {
//			int columns = mDataTask.getBufferColumns();
//			int h = mDataTask.getBufferHeightFeed();
//			char[] b1 = mDataTask.getPrintBuffer();
//			char[] b2 = mDataTask.getPrintBuffer();
//			char[] b3 = mDataTask.getPrintBuffer();
//			char[] b4 = mDataTask.getPrintBuffer();
//			char[] b5 = mDataTask.getPrintBuffer();
//			char[] b6 = mDataTask.getPrintBuffer();
//			buffer = new char[columns * h * 6];
//			for (int i = 0; i < columns; i++) {
//				System.arraycopy(b1, i * h, buffer, i * h *6, h);
//				System.arraycopy(b2, i * h, buffer, i * h * (6 + 1), h);
//				System.arraycopy(b3, i * h, buffer, i * h * (6 + 2), h);
//				System.arraycopy(b4, i * h, buffer, i * h * (6 + 3), h);
//				System.arraycopy(b5, i * h, buffer, i * h * (6 + 4), h);
//				System.arraycopy(b6, i * h, buffer, i * h * (6 + 5), h);
//			}
//		} else {
//			buffer = mDataTask.getPrintBuffer();
//		}
//		return buffer;
//	}

// H.M.Wang 2020-7-2 调整计数器增量策略，在打印完成时调整
	private void setCounterNext(DataTask task) {
		Debug.d(TAG, "--->setCounterNext");
// H.M.Wang 2020-12-17 以前没有参数，遍历打印群组，会出现打印一个任务，所有相关计数器都被更新的问题，追加参数，仅对当前任务进行修改
//		if (mDataTask == null) {
//			return;
//		}
//		for (DataTask task : mDataTask) {
// End of H.M.Wang 2020-12-17 以前没有参数，遍历打印群组，会出现打印一个任务，所有相关计数器都被更新的问题，追加参数，仅对当前任务进行修改
			for (BaseObject object : task.getObjList()) {
				if (object instanceof CounterObject) {
					((CounterObject) object).goNext();
// H.M.Wang 2023-10-26 由于2023-10-22取消了检测实际打印计数时进对于使用FIFO的限制，因此，未使用FIFO的也会在那里被修改
// H.M.Wang 2021-5-7 当不是FIFO模式的时候，在这里对实际打印次数进行修正
//					if(!mUsingFIFO) {
//						((CounterObject) object).goPrintedNext();
//					}
// End of H.M.Wang 2021-5-7 当不是FIFO模式的时候，在这里对实际打印次数进行修正
// End of H.M.Wang 2023-10-26 由于2023-10-22取消了检测实际打印计数时进对于使用FIFO的限制，因此，未使用FIFO的也会在那里被修改
// H.M.Wang 2020-7-31 追加超文本及条码当中超文本的计数器打印后调整
				} else if (object instanceof HyperTextObject) {
					((HyperTextObject) object).goNext();
// H.M.Wang 2023-10-26 由于2023-10-22取消了检测实际打印计数时进对于使用FIFO的限制，因此，未使用FIFO的也会在那里被修改
// H.M.Wang 2021-5-7 当不是FIFO模式的时候，在这里对实际打印次数进行修正
//					if(!mUsingFIFO) {
//						((HyperTextObject) object).goPrintedNext();
//					}
// End of H.M.Wang 2021-5-7 当不是FIFO模式的时候，在这里对实际打印次数进行修正
// End of H.M.Wang 2023-10-26 由于2023-10-22取消了检测实际打印计数时进对于使用FIFO的限制，因此，未使用FIFO的也会在那里被修改
				} else if (object instanceof BarcodeObject) {
					((BarcodeObject) object).goNext();
// H.M.Wang 2023-10-26 由于2023-10-22取消了检测实际打印计数时进对于使用FIFO的限制，因此，未使用FIFO的也会在那里被修改
// H.M.Wang 2021-5-7 当不是FIFO模式的时候，在这里对实际打印次数进行修正
//					if(!mUsingFIFO) {
//						((BarcodeObject) object).goPrintedNext();
//					}
// End of H.M.Wang 2021-5-7 当不是FIFO模式的时候，在这里对实际打印次数进行修正
// End of H.M.Wang 2023-10-26 由于2023-10-22取消了检测实际打印计数时进对于使用FIFO的限制，因此，未使用FIFO的也会在那里被修改
// End of H.M.Wang 2020-7-31 追加超文本及条码当中超文本的计数器打印后调整
				}
			}
//		}
	}
// End of H.M.Wang 2020-7-2 调整计数器增量策略，在打印完成时调整

// H.M.Wang 2021-5-7 当在FIFO模式的时候，在这里对实际打印次数进行修正
private void setCounterPrintedNext(DataTask task, int count) {
	Debug.d(TAG, "--->setCounterPrintedNext");

	for(int i=0; i<count; i++) {
		for (BaseObject object : task.getObjList()) {
			if (object instanceof CounterObject) {
				((CounterObject) object).goPrintedNext();
			} else if (object instanceof HyperTextObject) {
				((HyperTextObject) object).goPrintedNext();
			} else if (object instanceof BarcodeObject) {
				((BarcodeObject) object).goPrintedNext();
			}
		}
	}
}
// H.M.Wang 2021-5-7 当在FIFO模式的时候，在这里对实际打印次数进行修正

// H.M.Wang 2021-3-3 从QR.txt文件当中读取的变量信息的功能从DataTask类转移至此
	private boolean isReady = true;

// H.M.Wang 2024-2-20 追加一个GS1串口协议。该协议使用花括号作为AI的分隔符
	protected String parseGS1Brace(String recv) {
		int pos = 0;

		if(recv.length() < 21) return recv; // 至少包含示例字符串中的 01,21和93的AI，和14字的01段内容，以及一个空格。共计21个字。【0104607017595534215iD&U( 93CV0u】

// H.M.Wang 2024-2-28 txt文件有个不可见的文件头，UTF-8的文件头是EF BB BF，在从文件读入的第一行中，这个头会存在，并且以0xfeff的值存在，这个需要跳过，否则会导致GS1DM/QR生成失败。在这里排除是权宜之计，应该在QRReader里面排除，比较繁琐
		if(recv.charAt(pos) == (char)0xfeff) pos++;
// End of H.M.Wang 2024-2-28 txt文件有个不可见的文件头，UTF-8的文件头是EF BB BF，在从文件读入的第一行中，这个头会存在，并且以0xfeff的值存在，这个需要跳过，否则会导致GS1DM/QR生成失败。在这里排除是权宜之计，应该在QRReader里面排除，比较繁琐

		StringBuilder sb = new StringBuilder();

		if(recv.charAt(pos) != '0' || recv.charAt(pos+1) != '1') return recv;
		sb.append('{');
		sb.append(recv, pos, pos+2);
		sb.append('}');
		pos += 2;
		sb.append(recv, pos, pos+14);
		pos += 14;

		if(recv.charAt(pos) != '2' || recv.charAt(pos+1) != '1') return recv;
		sb.append('{');
		sb.append(recv, pos, pos+2);
		sb.append('}');
		pos += 2;
		for(; recv.charAt(pos) != ' '; pos++) {
			if(pos >= recv.length()) return recv;
			sb.append(recv, pos, pos+1);
		}
		pos++;

		if(pos+2 > recv.length()) return recv;
		if(recv.charAt(pos) != '9' || recv.charAt(pos+1) != '3') return recv;
		sb.append('{');
		sb.append(recv, pos, pos+2);
		sb.append('}');
		pos += 2;

		for(; pos < recv.length(); pos++) {
			sb.append(recv, pos, pos+1);
		}

		return sb.toString();
	}
// End of H.M.Wang 2024-2-20 追加一个GS1串口协议。该协议使用花括号作为AI的分隔符

	public void setContentsFromQRFile() {
		int strIndex = -1;
		String[] recvStrs = new String[1];
// H.M.Wang 2021-1-4 追加数据源FILE2，也是从QR.txt读取DT0,DT1,...,DT9,BARCODE的信息，但是DT赋值根据DT变量内部的序号匹配
//		if (!prev && SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_FILE) {
		if( SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_FILE ||
// H.M.Wang 2024-2-20 追加一个GS1串口协议。该协议使用花括号作为AI的分隔符
			SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_GS1_BRACE ||
// End of H.M.Wang 2024-2-20 追加一个GS1串口协议。该协议使用花括号作为AI的分隔符
			SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_FILE2) {
// End of H.M.Wang 2021-1-4 追加数据源FILE2，也是从QR.txt读取DT0,DT1,...,DT9,BARCODE的信息，但是DT赋值根据DT变量内部的序号匹配
			QRReader reader = QRReader.getInstance(mContext);
			String content = reader.read();

			if (TextUtils.isEmpty(content)) {
				isReady = false;
				return;
			}
			isReady = true;

			recvStrs = content.split(",");
			strIndex = 0;

			for(DataTask dataTask : mDataTask) {
				ArrayList<BaseObject> objList = dataTask.getObjList();
				for(BaseObject baseObject: objList) {
// H.M.Wang 2023-12-12 修改支持File的行格式没有序号，也没有DT0-9。直接是GS1条码的内容
					if(recvStrs.length == 1) {
						if(baseObject instanceof BarcodeObject) {
							if(((BarcodeObject) baseObject).getCode().equals(BarcodeObject.BARCODE_FORMAT_GS1QR) ||
							   ((BarcodeObject) baseObject).getCode().equals(BarcodeObject.BARCODE_FORMAT_GS1DM)) {
// H.M.Wang 2024-2-20 追加一个GS1串口协议。该协议使用花括号作为AI的分隔符
								if(SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_GS1_BRACE) {
									recvStrs[0] = parseGS1Brace(recvStrs[0]);
								}
// End of H.M.Wang 2024-2-20 追加一个GS1串口协议。该协议使用花括号作为AI的分隔符
								SystemConfigFile.getInstance().setBarcodeBuffer(recvStrs[0]);
								((BarcodeObject)baseObject).setContent(recvStrs[0]);
							}
						}
						continue;
					}
// End of H.M.Wang 2023-12-12 修改支持File的行格式没有序号，也没有DT0-9。直接是GS1条码的内容
					if(baseObject instanceof DynamicText) {
						if (SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_FILE) {
							if(strIndex < recvStrs.length) {
								Debug.d(TAG, "DynamicText[" + strIndex + "]: " + recvStrs[strIndex]);
// H.M.Wang 2023-3-16 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，因此接收到的数据应该存入桶里，这里是修改遗漏
								SystemConfigFile.getInstance().setDTBuffer(((DynamicText)baseObject).getDtIndex(), recvStrs[strIndex++]);
								baseObject.setContent(recvStrs[strIndex++]);
// End of H.M.Wang 2023-3-16 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，因此接收到的数据应该存入桶里，这里是修改遗漏
							}
						}

						if (SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_FILE2) {
							int dtIndex = ((DynamicText)baseObject).getDtIndex();
							if(dtIndex >= 0 && dtIndex < recvStrs.length) {
// H.M.Wang 2023-3-16 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，因此接收到的数据应该存入桶里，这里是修改遗漏
								SystemConfigFile.getInstance().setDTBuffer(dtIndex, recvStrs[dtIndex]);
								baseObject.setContent(recvStrs[dtIndex]);
//							} else {
								baseObject.setContent("");
							}
// End of H.M.Wang 2023-3-16 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，因此接收到的数据应该存入桶里，这里是修改遗漏
						}
					} else if(baseObject instanceof BarcodeObject) {
						if(recvStrs.length >= 11) {
							Debug.d(TAG, "BarcodeObject: " + recvStrs[10]);
// H.M.Wang 2024-2-20 追加一个GS1串口协议。该协议使用花括号作为AI的分隔符
							if(SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_GS1_BRACE) {
								if(((BarcodeObject) baseObject).getCode().equals(BarcodeObject.BARCODE_FORMAT_GS1QR) ||
									((BarcodeObject) baseObject).getCode().equals(BarcodeObject.BARCODE_FORMAT_GS1DM)) {
									recvStrs[10] = parseGS1Brace(recvStrs[10]);
								}
							}
// End of H.M.Wang 2024-2-20 追加一个GS1串口协议。该协议使用花括号作为AI的分隔符
// H.M.Wang 2023-3-16 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，因此接收到的数据应该存入桶里，这里是修改遗漏
							SystemConfigFile.getInstance().setBarcodeBuffer(recvStrs[10]);
							((BarcodeObject)baseObject).setContent(recvStrs[10]);
// End of H.M.Wang 2023-3-16 修改动态文本内容获取逻辑，从预留的10个盆子里面获取，因此接收到的数据应该存入桶里，这里是修改遗漏
						}
					}
				}
			}
		}

	}
// End of H.M.Wang 2021-3-3 从QR.txt文件当中读取的变量信息的功能从DataTask类转移至此


// H.M.Wang 2020-5-6 参数设置页面，在未修改计数器的情况下，点击OK保存后，计数器会跳数，分析是因为设置了	mNeedUpdate=true，重新生成打印缓冲区，重新生成时计数器会自动增量，所以增加了一个重新下发的函数，而不重新生成缓冲区
	public void resendBufferToFPGA() {
		if(null != mPrintBuffer) {
			FpgaGpioOperation.writeData(FpgaGpioOperation.DATA_GENRE_UPDATE, FpgaGpioOperation.FPGA_STATE_OUTPUT, mPrintBuffer, mPrintBuffer.length * 2);
		}
	}

	char[] mPrintBuffer = null;
// H.M.Wang 2020-5-6 参数设置页面，...

	private static int mPrintCount = 10;

// 2020-6-29 处于打印状态时，如果用户确认设置，需要向FPGA下发设置内容，按一定原则延迟下发
	public long Time1 = 0;
	public long Time2 = 0;
// 2020-7-21 为修改计算等待时间添加倍率变量（新公式为：N=(打印缓冲区字节数-1）/16K；时长=3/(2N+4)
// H.M.Wang 2023-1-7 取消打印时下发参数，因此该变量已经没有意义(2023-3-10追记)
//	public int  DataRatio = 0;
// End of H.M.Wang 2023-1-7 取消打印时下发参数，因此该变量已经没有意义(2023-3-10追记)
// End of 2020-7-21 为修改计算等待时间添加倍率变量（新公式为：N=(打印缓冲区字节数-1）/16K；时长=3/(2N+4)
// End of 2020-6-29 处于打印状态时，如果用户确认设置，需要向FPGA下发设置内容，按一定原则延迟下发

// H.M.Wang 2023-10-21 取消	mFirstForLanFast 变量的判断，改为直接用 mDataUpdatedForFastLan，响应代码已经删除
// 2020-6-30 追加是否为网络快速打印的第一次数据生成标识
//	private boolean mFirstForLanFast = false;
// End of 2020-6-30 追加是否为网络快速打印的第一次数据生成标识
// End of H.M.Wang 2023-10-21 取消	mFirstForLanFast 变量的判断，改为直接用 mDataUpdatedForFastLan，响应代码已经删除

// 2020-7-3 追加网络快速打印状态下数据是否更新的标识
	private boolean mDataUpdatedForFastLan = false;
// End of 2020-7-3 追加网络快速打印状态下数据是否更新的标识

// H.M.Wang 2020-7-9 追加计数器重置标识
	public boolean mCounterReset=false;
// End of H.M.Wang 2020-7-9 追加计数器重置标识
	private long last = 0;
// H.M.Wang 2023-10-20 追加下发总数计数，结合以前已经定义的已打印计数(mPrintedCount)。两者相减即为img中FIFO的剩余未打印任务数量
	private int mDownWrittenCount;
	private int mLastPrintedCount;
// End of H.M.Wang 2023-10-20 追加下发总数计数，结合以前已经定义的已打印计数(mPrintedCount)。两者相减即为img中FIFO的剩余未打印任务数量
// H.M.Wang 2023-10-20 为ControlTabActivity提供三个变量的获取接口
	public int getRecentPrintedCount() {
		int lastRecentPrintedCount = mPrintedCount - mLastPrintedCount;
		mLastPrintedCount = mPrintedCount;
		return lastRecentPrintedCount;
	}
	public int getRemainCount() {
		return mDownWrittenCount - mPrintedCount;
	}
// End of H.M.Wang 2023-10-20 为ControlTabActivity提供三个变量的获取接口
// H.M.Wang 2023-10-28 增加打印方向(Direction)和倒置(Inverse)
	public static volatile int sDirectionCmd;
	public static volatile int sInverseCmd;

	private void doDirrectionCmd() {
		Debug.d(TAG, "doDirrectionCmd(" + sDirectionCmd + ")");
		if(sDirectionCmd == 1 && mDataTask.get(index()).getPNozzle().mirrorEnable) {
			BufferRebuilder br = new BufferRebuilder(mPrintBuffer, mDataTask.get(index()).mBinInfo.getCharsFeed(), 4);
			br.mirror(new int[]{0x01,0x01,0x01,0x01});
			mPrintBuffer = br.getCharBuffer();
		}
	}

	private void doInverseCmd() {
		Debug.d(TAG, "doInverseCmd(" + sInverseCmd + ")");
		if(sInverseCmd == 1 && mDataTask.get(index()).getPNozzle().reverseEnable) {
			BufferRebuilder br = new BufferRebuilder(mPrintBuffer, mDataTask.get(index()).mBinInfo.getCharsFeed(), 4);
			br.reverse(0x03);
			br.reverse(0x0C);
			mPrintBuffer = br.getCharBuffer();
		} else if(sInverseCmd == 2 && mDataTask.get(index()).getPNozzle().reverseEnable) {
			BufferRebuilder br = new BufferRebuilder(mPrintBuffer, mDataTask.get(index()).mBinInfo.getCharsFeed(), 4);
			br.reverse(0x0F);
			mPrintBuffer = br.getCharBuffer();
		}
	}

// End of H.M.Wang 2023-10-28 增加打印方向(Direction)和倒置(Inverse)

	public class PrintTask extends Thread {
		@Override
		public void run() {
			Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);

			FpgaGpioOperation.init(mContext);

// 2020-6-29 处于打印状态时，如果用户确认设置，需要向FPGA下发设置内容，按一定原则延迟下发
			Time1 = System.currentTimeMillis();
			Time2 = System.currentTimeMillis();
// End of 2020-6-29 处于打印状态时，如果用户确认设置，需要向FPGA下发设置内容，按一定原则延迟下发

// 2020-6-30 网络快速打印的第一次数据生成标识设真
// H.M.Wang 2024-1-13 扫描协议5的打印行为，只有接收到扫描数据时才下发，否则不下发
//			if(SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_FAST_LAN) {
			if(SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_FAST_LAN ||
				SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_SCANER5) {
// End of H.M.Wang 2024-1-13 扫描协议5的打印行为，只有接收到扫描数据时才下发，否则不下发
// H.M.Wang 2020-8-4 这个不设的话，可能上次停止打印时接收到数据后，mDataUpdatedForFastLan被设成true了，导致后面生成打印缓冲区误操作
				mDataUpdatedForFastLan = false;
// End of H.M.Wang 2020-8-4 这个不设的话，可能上次停止打印时接收到数据后，mDataUpdatedForFastLan被设成true了，导致后面生成打印缓冲区误操作
			}
// End of 2020-6-30 网络快速打印的第一次数据生成标识设真

			//逻辑要求，必须先发数据
			Debug.d(TAG, "--->print run from No." + index());

// H.M.Wang 2021-3-3 从QR.txt文件当中读取的变量信息的功能从DataTask类转移至此
			setContentsFromQRFile();
// End of H.M.Wang 2021-3-3 从QR.txt文件当中读取的变量信息的功能从DataTask类转移至此
// H.M.Wang 2021-9-17 追加扫描协议1-FIFO
            if(SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_SCANER1_FIFO) {
				setFifoDataToDtAtBeginning();
            }
// End of H.M.Wang 2021-9-17 追加扫描协议1-FIFO

// H.M.Wang 2023-3-11 追加网络通讯前置缓冲区功能
			PC_FIFO pc_FIFO = PC_FIFO.getInstance(mContext);
			if(pc_FIFO.PCFIFOEnabled()) {
				pc_FIFO.useString();
			}
// End of H.M.Wang 2023-3-11 追加网络通讯前置缓冲区功能

// H.M.Wang 2022-12-19 追加一个串口，RS232_DOT_MARKER
			if(SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_DOT_MARKER) {
				mPrintBuffer = getDotMarkerPrintBuffer(true);
			} else {
				mPrintBuffer = mDataTask.get(index()).getPrintBuffer(true);
			}
// End of H.M.Wang 2022-12-19 追加一个串口，RS232_DOT_MARKER
			if (isLanPrint()) {
				setLanBuffer(mContext, index(), mPrintBuffer);
			} else {
				Debug.d(TAG, "--->print buffer ready");

				// H.M.Wang 2019-10-10 追加计算打印区对应于各个头的打印点数
// H.M.Wang 2020-10-23 计算点数移到DataTask的getPrintBuffer函数内
/*				DataTask t = mDataTask.get(index());
				Debug.d(TAG, "GetPrintDots Start Time: " + System.currentTimeMillis());
// H.M.Wang 2020-10-18 重新开放打印前计算墨点数
				int[] dots = NativeGraphicJni.GetPrintDots(mPrintBuffer, mPrintBuffer.length, t.getInfo().mCharsPerHFeed, t.getPNozzle().mHeads);

				int totalDot = 0;
				for (int j = 0; j < dots.length; j++) {
					// H.M.Wang 2019-10-11 获得的点数乘2
					SystemConfigFile config = SystemConfigFile.getInstance(mContext);
					final int headIndex = config.getParam(SystemConfigFile.INDEX_HEAD_TYPE);
					final PrinterNozzle head = PrinterNozzle.getInstance(headIndex);

					if (head != PrinterNozzle.MESSAGE_TYPE_16_DOT &&
						head != PrinterNozzle.MESSAGE_TYPE_32_DOT &&
						head != PrinterNozzle.MESSAGE_TYPE_32DN &&
						head != PrinterNozzle.MESSAGE_TYPE_32SN &&
						head != PrinterNozzle.MESSAGE_TYPE_64SN &&
						head != PrinterNozzle.MESSAGE_TYPE_64_DOT) {
//						dots[j] *= 2;
					} else {
						dots[j] *= 200;
					}
					totalDot += dots[j];
				}
				t.setDots(totalDot);
				t.setDotsEach(dots);
// End of H.M.Wang 2020-10-18 重新开放打印前计算墨点数
				Debug.d(TAG, "GetPrintDots Done Time: " + System.currentTimeMillis());
*/
// End of H.M.Wang 2020-10-23 计算点数移到DataTask的getPrintBuffer函数内

// 2023-1-18 第一次生成打印缓冲区后，重新计算阈值和内部计数器，
				recalCount();
// End of 2023-1-18 第一次生成打印缓冲区后，重新计算阈值和内部计数器，

				final StringBuilder sb = new StringBuilder();
				sb.append("Dots per Head: [");
				for (int i=0; i<8; i++) {
					if(i != 0) {
						sb.append(", ");
					}
// H.M.Wang 2020-5-21 12.7R5头改为RX48，追加RX50头
//					if(config.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_12_7_R5) {
					if(SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_R6X48 ||
						SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_R6X50) {
//						sb.append(t.getDots(i<6?0:i)*(PrinterNozzle.R5x6_PRINT_COPY_NUM - 1));
						sb.append(mPrintDots[i < PrinterNozzle.R6_HEAD_NUM ? 0 : i]);
// End of H.M.Wang 2020-5-21 12.7R5头改为RX48，追加RX50头
// H.M.Wang 2021-3-6 追加E6X48,E6X50头
					} else if( SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E6X48 ||
								SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E6X50) {
//						sb.append(t.getDots(i<6?0:i)*(PrinterNozzle.R5x6_PRINT_COPY_NUM - 1));
							sb.append(mPrintDots[i < PrinterNozzle.E6_HEAD_NUM ? 0 : i]);
// End of H.M.Wang 2020-5-21 12.7R5头改为RX48，追加RX50头
					} else if( SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E6X1) {
//						sb.append(t.getDots(i<6?0:i)*(PrinterNozzle.R5x6_PRINT_COPY_NUM - 1));
						sb.append(mPrintDots[i < PrinterNozzle.E6_HEAD_NUM ? 0 : i]);
// H.M.Wang 2021-8-25 追加E5X48和E5X50头类型
					} else if( SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E5X48 ||
							SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E5X50) {
						sb.append(mPrintDots[i < PrinterNozzle.E5_HEAD_NUM ? 0 : i]);
// End of H.M.Wang 2021-8-25 追加E5X48和E5X50头类型
					} else {
						sb.append(mPrintDots[i]);
					}
// End of H.M.Wang 2021-3-6 追加E6X48,E6X50头

				}
				sb.append("]");
				Debug.d(TAG, sb.toString());
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(mContext, sb.toString(), Toast.LENGTH_LONG).show();
					}
				});

// 2020-6-30 网络快速打印的第一次数据生成后不下发
// H.M.Wang 2021-1-15 追加扫描协议3，协议内容与扫描2协议完全一致，仅在打印的时候，仅可以打印一次
// H.M.Wang 2023-10-21 对于网络快速打印的下发机制做调整，取消原来的mFirstForLanFast变量，改为直接用mDataUpdatedForFastLan来判断
// H.M.Wang 2024-1-13 扫描协议5的打印行为，只有接收到扫描数据时才下发，否则不下发
//				if((SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_DATA_SOURCE) != SystemConfigFile.DATA_SOURCE_FAST_LAN || mDataUpdatedForFastLan) &&
				if((SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_DATA_SOURCE) != SystemConfigFile.DATA_SOURCE_FAST_LAN || mDataUpdatedForFastLan) &&
					(SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_DATA_SOURCE) != SystemConfigFile.DATA_SOURCE_SCANER5 || mDataUpdatedForFastLan) &&
// End of H.M.Wang 2024-1-13 扫描协议5的打印行为，只有接收到扫描数据时才下发，否则不下发
					// 数据源不是网络快速打印，或者如果是网络快速打印，但是数据已经准备好则下发
					SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) != SystemConfigFile.DATA_SOURCE_SCANER3) {
					// 数据源不是扫描协议3
					// 综合起来，下发的条件就是：既不是网络快速打印也不是扫描协议3的时候下发，或者是网络快速打印但是数据已经准备好了，也下发
// End of H.M.Wang 2023-10-21 对于网络快速打印的下发机制做调整，取消原来的mFirstForLanFast变量，改为直接用mDataUpdatedForFastLan来判断
// End of H.M.Wang 2021-1-15 追加扫描协议3，协议内容与扫描2协议完全一致，仅在打印的时候，仅可以打印一次
// H.M.Wang 2023-10-28 增加打印方向(Direction)和倒置(Inverse)
					if(sDirectionCmd > 0) doDirrectionCmd();
					if(sInverseCmd > 0) doInverseCmd();
// End of H.M.Wang 2023-10-28 增加打印方向(Direction)和倒置(Inverse)
					Debug.e(TAG, "--->write data");
					FpgaGpioOperation.writeData(FpgaGpioOperation.DATA_GENRE_NEW, FpgaGpioOperation.FPGA_STATE_OUTPUT, mPrintBuffer, mPrintBuffer.length * 2);
// 2020-7-21 为修改计算等待时间添加倍率变量（新公式为：N=(打印缓冲区字节数-1）/16K；时长=3/(2N+4)
//					DataRatio = (mPrintBuffer.length * 2 - 1) / (16 * 1024);
// End of 2020-7-21 为修改计算等待时间添加倍率变量（新公式为：N=(打印缓冲区字节数-1）/16K；时长=3/(2N+4)
// H.M.Wang 2023-10-20 追加下发总数计数
					mDownWrittenCount++;
// End of H.M.Wang 2023-10-20 追加下发总数计数
				}
// End of 2020-6-30 网络快速打印的第一次数据生成后不下发

// H.M.Wang 2020-1-7 追加群组打印时，显示正在打印的MSG的序号
// H.M.Wang 2020-6-28 追加向网络发送打印数据缓冲区准备完成消息
				if(null != mCallback && mDataTask.size() > 1) {
					mCallback.onPrint(index());
//                    mCallback.onComplete(index());
				}
// H.M.Wang 2020-7-6 原来放在前面这个判断里面是给群组用的，不是群组不会起作用，移到这里
// H.M.Wang 2023-3-10 似乎无论是什么数据源，这里下发数据以后，就应该向网络回送一个已下发回复
//                if(SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_FAST_LAN) {
                    if(null != mCallback) {
                        mCallback.onComplete(index());
                    }
//                }
// End of H.M.Wang 2023-3-10 似乎无论是什么数据源，这里下发数据以后，就应该向网络回送一个已下发回复
// End of H.M.Wang 2020-7-6 原来放在前面这个判断里面是给群组用的，不是群组不会起作用，移到这里
// End of H.M.Wang 2020-6-28 追加向网络发送打印数据缓冲区准备完成消息
// End of H.M.Wang 2020-1-7 追加群组打印时，显示正在打印的MSG的序号

				// save log
				LogIntercepter.getInstance(mContext).execute(getCurData());

			}
			last = SystemClock.currentThreadTimeMillis();

// H.M.Wang 2021-1-15 追加扫描协议3，协议内容与扫描2协议完全一致，仅在打印的时候，仅可以打印一次
			boolean dataSent = false;
// End of H.M.Wang 2021-1-15 追加扫描协议3，协议内容与扫描2协议完全一致，仅在打印的时候，仅可以打印一次
// H.M.Wang 2021-4-20 增加一个放置频繁打印"--->FPGA buffer is empty"的逻辑锁，这种频繁打印会发生在网络快速打印的首发之前和SCAN3串口协议的时候
			boolean reportEmpty = true;
// End of H.M.Wang 2021-4-20 增加一个放置频繁打印"--->FPGA buffer is empty"的逻辑锁，这种频繁打印会发生在网络快速打印的首发之前和SCAN3串口协议的时候

			mStopped = false;
//			long startMillis = System.currentTimeMillis();
			int lastPrintedCount = 0;

			while(mRunning == true) {
// H.M.Wang 2021-12-30 当正在打印的时候，如果开始清洗，则暂停打印进程
                if(isPurging || SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_22MM) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Debug.e(TAG, e.getClass().getSimpleName() + ": " + e.getMessage());
                    }
                    continue;
                }
// End of H.M.Wang 2021-12-30 当正在打印的时候，如果开始清洗，则暂停打印进程

// H.M.Wang 2021-5-8 试图修改根据打印次数修改主屏幕显示打印数量的功能
// H.M.Wang 2021-5-7 当在FIFO模式的时候，在这里对实际打印次数进行修正
// H.M.Wang 2023-10-22 由于主界面显示的打印计数已经修改为实际打印的数量，因此，未采用FIFO时，也需要通过这里的处理调整打印计数
//				if(mUsingFIFO) {
// End of H.M.Wang 2023-10-22 由于主界面显示的打印计数已经修改为实际打印的数量，因此，未采用FIFO时，也需要通过这里的处理调整打印计数
					lastPrintedCount = FpgaGpioOperation.getPrintedCount();
					if(lastPrintedCount != mPrintedCount) {
						Debug.d(TAG, "lastPrintedCount = " + lastPrintedCount + "; mPrintedCount = " + mPrintedCount);
						int printedCount = lastPrintedCount - mPrintedCount;
						setCounterPrintedNext(mDataTask.get(index()), printedCount);
						for(int i=0; i<printedCount; i++) {
// H.M.Wang 2023-3-10 在群组打印的时候，把打印完成的回送也移到这里，这样就不会丢掉连续的次数了，并且，这时似乎没有必要做下发数据后的后续操作
//							afterDataSent();
							mPrintedCount++;
							if (mCallback != null) {
								mInkListener.onCountChanged();
								if(mUsingFIFO) mCallback.onPrinted0000(index());
// H.M.Wang 2023-10-26 追加一个向PC端的回复，当INDEX_FEEDBACK=1时，回复0002到PC端
								if((SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_FEEDBACK)) == 1) mCallback.onPrinted0002(index());
// End of H.M.Wang 2023-10-26 追加一个向PC端的回复，当INDEX_FEEDBACK=1时，回复0002到PC端
							}
// End of H.M.Wang 2023-3-10 在群组打印的时候，把打印完成的回送也移到这里，这样就不会丢掉连续的次数了，并且，这时似乎没有必要做下发数据后的后续操作
						}
					}
//				}
// End of H.M.Wang 2021-5-7 当在FIFO模式的时候，在这里对实际打印次数进行修正
// End of H.M.Wang 2021-5-8 试图修改根据打印次数修改主屏幕显示打印数量的功能

				int writable = FpgaGpioOperation.pollState();

//				if(System.currentTimeMillis() - startMillis > 1000) {
//					Debug.d(TAG, "Running... ");
//					startMillis = System.currentTimeMillis();
//				}

				if (writable == 0) { //timeout
					Debug.e(TAG, "--->FPGA timeout");
//					if (isLanPrint() && pcReset == true) {
//						buffer = getLanBuffer(index());
//						FpgaGpioOperation.writeData(FpgaGpioOperation.DATA_GENRE_UPDATE, FpgaGpioOperation.FPGA_STATE_OUTPUT, buffer, buffer.length * 2);
//						pcReset = false;
//					}
					reportEmpty = true;
				} else if (writable == -1) {
//					Debug.e(TAG, "--->FPGA error");
					reportEmpty = true;
				} else {
					if(reportEmpty) Debug.d(TAG, "--->FPGA buffer is empty");

// H.M.Wang 2023-2-13 增加一个工作模式，使用外接U盘当中的文件作为DT的数据源来打印。后续使用哪个方法
					if(TxtDT.getInstance(mContext).isTxtDT()) {
						TxtDT.getInstance(mContext).goNext();
					}
// End of H.M.Wang 2023-2-13 增加一个工作模式，使用外接U盘当中的文件作为DT的数据源来打印

// H.M.Wang 2022-12-22 接收到FPGA请求数据的信号(empty)后，向PC发送打印成功的反馈
					if(SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_DOT_MARKER) {
						final SerialHandler serialHandler = SerialHandler.getInstance(mContext);
						serialHandler.sendCommandProcessResult(0, 0, 0, 0, "OK-02-" + ByteArrayUtils.toHexString(mDotMarkerRecvBuffer) + "\n");
					}
// End of H.M.Wang 2022-12-22 接收到FPGA请求数据的信号(empty)后，向PC发送打印成功的反馈

					reportEmpty = false;
// 2020-7-3 在网络快速打印状态下，如果没有接收到新的数据，即使触发也不生成新的打印缓冲区下发
// H.M.Wang 2024-1-13 扫描协议5的打印行为，只有接收到扫描数据时才下发，否则不下发
//					if(SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_FAST_LAN) {
					if(SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_FAST_LAN ||
						SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_SCANER5) {
// End of H.M.Wang 2024-1-13 扫描协议5的打印行为，只有接收到扫描数据时才下发，否则不下发
						if(!mDataUpdatedForFastLan) {
							try { Thread.sleep(3); } catch (InterruptedException e) {Debug.e(TAG, e.getMessage());}
							continue;
						}
						mDataUpdatedForFastLan = false;
					}
// End of 2020-7-3 在网络快速打印状态下，如果没有接收到新的数据，即使触发也不生成新的打印缓冲区下发

					Time1 = Time2;
					Time2 = System.currentTimeMillis();
// 2023-10-21 PC回送打印完成的操作，
// H.M.Wang 2023-3-10 群组打印的完成通知已到了前面的处理当中，因此这里只考虑非群组打印的情形
					if(!mUsingFIFO &&
						(SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) != SystemConfigFile.DATA_SOURCE_SCANER3 ||
						(SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_SCANER3 && dataSent))) {
						if (mCallback != null) {
							mCallback.onPrinted0000(index());
						}
					}
// End of H.M.Wang 2023-3-10 群组打印的完成通知已到了前面的处理当中，因此这里只考虑非群组打印的情形
					mInterval = SystemClock.currentThreadTimeMillis() - last;
					mHandler.removeMessages(MESSAGE_DATA_UPDATE);
// H.M.Wang 2021-4-20 移到下面具体处理的next函数调用之前，如果放在这里，会导致SCAN3或者网络快速打印的第一次操作之前对计数器频繁调整
//					mNeedUpdate = false;

// H.M.Wang 2020-7-2 调整计数器增量策略，在打印完成时调整，取消从前在生成打印缓冲区时调整
// H.M.Wang 2020-12-17 以前没有参数，遍历打印群组，会出现打印一个任务，所有相关计数器都被更新的问题，追加参数，仅对当前任务进行修改
//					setCounterNext(mDataTask.get(index()));
// End of H.M.Wang 2020-12-17 以前没有参数，遍历打印群组，会出现打印一个任务，所有相关计数器都被更新的问题，追加参数，仅对当前任务进行修改
// End of H.M.Wang 2020-7-2 调整计数器增量策略
// End of H.M.Wang 2021-4-20 移到下面具体处理的next函数调用之前，如果放在这里，会导致SCAN3或者网络快速打印的第一次操作之前对计数器频繁调整

// H.M.Wang 2023-3-11 追加网络通讯前置缓冲区功能
					if(pc_FIFO.PCFIFOEnabled()) {
						if(pc_FIFO.PCFIFOAvailable()) {
							pc_FIFO.useString();
						} else {
							continue;
						}
					}
// End of H.M.Wang 2023-3-11 追加网络通讯前置缓冲区功能

					synchronized (DataTransferThread.class) {
////////////////////////////////////////////////////////
						IInkDevice scm = InkManagerFactory.inkManager(mContext);
						if(scm instanceof SmartCardManager) {
							if(mPrintCount == 0) {
								mPrintCount = 10;
								((SmartCardManager) scm).updateLevel();
							}
							mPrintCount--;
						}
////////////////////////////////////////////////////////
// H.M.Wang 2021-1-15 追加扫描协议3，协议内容与扫描2协议完全一致，仅在打印的时候，仅可以打印一次
						if (SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) != SystemConfigFile.DATA_SOURCE_SCANER3) {
// H.M.Wang 2021-4-20 该函数的调用移到这里
							setCounterNext(mDataTask.get(index()));
// End of H.M.Wang 2021-4-20 该函数的调用移到这里
							next();
// H.M.Wang 2021-3-4 此断代码转移至此
// H.M.Wang 2021-3-4 DataTask中的isReady变量，由于读QR文件的操作转移至这里，已经失效，在本类中追加isReady变量，并且据此进行判断操作
// H.M.Wang 2021-3-5 修改判断条件，只有在FILE和FILE2数据源时才判断是否为到了文件末尾而结束
//								if (!mDataTask.get(index()).isReady) {
							if(((SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_FILE ||
// H.M.Wang 2024-2-20 追加一个GS1串口协议。该协议使用花括号作为AI的分隔符
									SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_GS1_BRACE ||
// End of H.M.Wang 2024-2-20 追加一个GS1串口协议。该协议使用花括号作为AI的分隔符
								SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_FILE2)) &&
								!isReady &&
								mCallback != null) {
// End of H.M.Wang 2021-3-5 修改判断条件，只有在FILE和FILE2数据源时才判断是否为到了文件末尾而结束
								mCallback.onFinished(CODE_BARFILE_END);
// H.M.Wang 2022-4-25 补充修改2022-4-8取消停止打印修改的遗漏，如果不停止打印，这里就不要break了
//								break;
// End of H.M.Wang 2022-4-25 补充修改2022-4-8取消停止打印修改的遗漏，如果不停止打印，这里就不要break了
							}
// End of H.M.Wang 2021-3-4 DataTask中的isReady变量，由于读QR文件的操作转移至这里，已经失效，在本类中追加isReady变量，并且据此进行判断操作
// End of H.M.Wang 2021-3-4 此断代码转移至此

// H.M.Wang 2021-9-17 追加扫描协议1-FIFO
                            if(SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_SCANER1_FIFO) {
								setFifoDataToDtRemove();
                            }
// End of H.M.Wang 2021-9-17 追加扫描协议1-FIFO

							if (isLanPrint()) {
								mPrintBuffer = getLanBuffer(index());
								Debug.i(TAG, "--->mPrintBuffer.length: " + mPrintBuffer.length);
							} else {
// H.M.Wang 2020-5-19 QR文件打印最后一行后无反应问题。应该先生成打印缓冲区，而不是先判断是否到了终点。顺序不对
								Debug.i(TAG, "mIndex: " + index());
// H.M.Wang 2022-12-19 追加一个串口，RS232_DOT_MARKER
								if(SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_DOT_MARKER) {
									mPrintBuffer = getDotMarkerPrintBuffer(false);
								} else {
									mPrintBuffer = mDataTask.get(index()).getPrintBuffer(false);
								}
// End of H.M.Wang 2022-12-19 追加一个串口，RS232_DOT_MARKER
//							Debug.i(TAG, "mIndex: " + index());
//							mPrintBuffer = mDataTask.get(index()).getPrintBuffer();
// End of H.M.Wang 2020-5-19 QR文件打印最后一行后无反应问题。应该先生成打印缓冲区，而不是先判断是否到了终点。顺序不对
							}

// H.M.Wang 2023-10-28 增加打印方向(Direction)和倒置(Inverse)
							if(sDirectionCmd > 0) doDirrectionCmd();
							if(sInverseCmd > 0) doInverseCmd();
// End of H.M.Wang 2023-10-28 增加打印方向(Direction)和倒置(Inverse)
							FpgaGpioOperation.writeData(FpgaGpioOperation.DATA_GENRE_NEW, FpgaGpioOperation.FPGA_STATE_OUTPUT, mPrintBuffer, mPrintBuffer.length * 2);
							Debug.d(TAG, "--->FPGA data sent!");
// H.M.Wang 2023-10-20 追加下发总数计数
							mDownWrittenCount++;
// End of H.M.Wang 2023-10-20 追加下发总数计数
							reportEmpty = true;
// H.M.Wang 2021-4-20 该函数的调用移到这里。否则在网络快速打印的首发之前，或者SCAN3协议的时候可能需要发送时被错误清除
							mNeedUpdate = false;
// End of H.M.Wang 2021-4-20 该函数的调用移到这里，或者SCAN3协议的时候可能需要发送时被错误清除
// H.M.Wang 2021-3-8 在实施了打印后调用
// H.M.Wang 2021-5-8 试图修改根据打印次数修改主屏幕显示打印数量的功能
// H.M.Wang 2023-3-10 每次下发数据后，均做下发数据后的处理，而不必区分是否为FIFO功能
//							if(!mUsingFIFO) {
								afterDataSent();
//							}
// End of H.M.Wang 2023-3-10 每次下发数据后，均做下发数据后的处理，而不必区分是否为FIFO功能
// End of H.M.Wang 2021-5-8 试图修改根据打印次数修改主屏幕显示打印数量的功能
// End of H.M.Wang 2021-3-8 在实施了打印后调用
						} else {
							if(dataSent) {
// H.M.Wang 2021-4-20 该函数的调用移到这里
								setCounterNext(mDataTask.get(index()));
// End of H.M.Wang 2021-4-20 该函数的调用移到这里
								next();				// 扫描3的逻辑是没有收到扫描数据不打印，打印之打印一次。实现策略是初始和empty时不下发新的缓冲区，仅在更新时下发。因此，如果更新没下发就保持当前的记录
													// 这个dataSent标识就是起到这个作用。初值为false，开始打印后第一个任务等待扫描数据，扫描数据下发后，更新下发。在更新下发结束后，dataSent置真。因此只有在有了更新下发以后，才为真
								dataSent = false;	// 更改打印指针后，立即设置为false，以避免没下发数据，频繁来empty导致不必要指针调整（这个在新的img里面不会发生，因此这一句仅为保险设置，实际不设也行）
// H.M.Wang 2021-3-8 在实施了打印后调用
// H.M.Wang 2021-5-8 试图修改根据打印次数修改主屏幕显示打印数量的功能
// H.M.Wang 2023-3-10 每次下发数据后，均做下发数据后的处理，而不必区分是否为FIFO功能
//								if(!mUsingFIFO) {
									afterDataSent();
//								}
// End of H.M.Wang 2021-5-8 试图修改根据打印次数修改主屏幕显示打印数量的功能
// End of H.M.Wang 2021-3-8 在实施了打印后调用
								if(index() > 0 && index() < mDataTask.size()) {
									mNeedUpdate = true;
								}
							}
// H.M.Wang 2021-3-8 对于扫描3，如果接收到了扫描数据，要逐个将群组当中的任务都打印一边，这个靠mNeedUpdate = true来控制，因此，需要放在执行打印的流程里面不应该在外面
//							if(index() > 0 && index() < mDataTask.size()) {
//								mNeedUpdate = true;
//							}
// End of H.M.Wang 2021-3-8 对于扫描3，如果接收到了扫描数据，要逐个将群组当中的任务都打印一边，这个靠mNeedUpdate = true来控制，因此，需要放在执行打印的流程里面不应该在外面
						}
// End of H.M.Wang 2021-1-15 追加扫描协议3，协议内容与扫描2协议完全一致，仅在打印的时候，仅可以打印一次

// H.M.Wang 2021-3-8 这一部分的时候修正，应该在实施了打印以后再进行，反在这里的话，如果是扫描3，并且还没有下发数据，这里就会被频繁执行，导致计数频繁增加，提出来作为函数，然后在实施了打印后调用
/*
// 2020-7-21 为修改计算等待时间添加倍率变量（新公式为：N=(打印缓冲区字节数-1）/16K；时长=3/(2N+4)
//						DataRatio = (mPrintBuffer.length * 2 - 1) / (16 * 1024);
// End of 2020-7-21 为修改计算等待时间添加倍率变量（新公式为：N=(打印缓冲区字节数-1）/16K；时长=3/(2N+4)

// H.M.Wang 2020-1-7 追加群组打印时，显示正在打印的MSG的序号
						if(mCallback != null && mDataTask.size() > 1) {
							mCallback.onPrint(index());
						}
// End of H.M.Wang 2020-1-7 追加群组打印时，显示正在打印的MSG的序号

						last = SystemClock.currentThreadTimeMillis();
						countDown();
//						mInkListener.onCountChanged();
//						mScheduler.schedule();
						if (mCallback != null) {
							mCallback.onComplete(index());
						}
						LogIntercepter.getInstance(mContext).execute(getCurData());
*/
// End of H.M.Wang 2021-3-8 这一部分的时候修正，应该在实施了打印以后再进行，反在这里的话，如果是扫描3，并且还没有下发数据，这里就会被频繁执行，导致计数频繁增加，提出来作为函数，然后在实施了打印后调用
					}
                }

// H.M.Wang 2020-11-13 追加内容是否变化的判断
// H.M.Wang 2021-1-4 在打印过程中，用户可能通过上下键（ControlTabActivity里的mMsgNext或者mMsgPrev，这回最终导致resetTask，在resetTask里面会对mDataTask清空，如果不排斥线程，这里可能会遇到空的情况而崩溃
 				synchronized (DataTransferThread.class) {
					mNeedUpdate |= mDataTask.get(index()).contentChanged();
				}
// End of H.M.Wang 2021-1-4 在打印过程中，用户可能通过上下键（ControlTabActivity里的mMsgNext或者mMsgPrev，这回最终导致resetTask，在resetTask里面会对mDataTask清空，如果不排斥线程，这里可能会遇到空的情况而崩溃
// End of H.M.Wang 2020-11-13 追加内容是否变化的判断

				if(mNeedUpdate == true) {
// H.M.Wang 2020-6-28 修改打印数据缓冲区更新策略，当网络快速打印的时候不再根据数据更新重新生成打印缓冲区
// H.M.Wang 2024-1-13 扫描协议5的打印行为，只有接收到扫描数据时才下发，否则不下发
//					if(SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) != SystemConfigFile.DATA_SOURCE_FAST_LAN) {
					if(SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) != SystemConfigFile.DATA_SOURCE_FAST_LAN &&
						SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) != SystemConfigFile.DATA_SOURCE_SCANER5) {
// End of H.M.Wang 2024-1-13 扫描协议5的打印行为，只有接收到扫描数据时才下发，否则不下发
// End of H.M.Wang 2020-6-28 修改打印数据缓冲区更新策略，当网络快速打印的时候不再根据数据更新重新生成打印缓冲区
						mHandler.removeMessages(MESSAGE_DATA_UPDATE);
					//在此处发生打印数据，同时
// H.M.Wang 2019-12-29 在重新生成打印缓冲区的时候，考虑网络打印的因素

// H.M.Wang 2021-9-17 追加扫描协议1-FIFO
                        if(SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_SCANER1_FIFO) {
                            setFifoDataToDt();
                        }
// End of H.M.Wang 2021-9-17 追加扫描协议1-FIFO

						if (isLanPrint()) {
							mPrintBuffer = getLanBuffer(index());
						} else {
// H.M.Wang 2022-12-19 追加一个串口，RS232_DOT_MARKER
							if(SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_DOT_MARKER) {
								mPrintBuffer = getDotMarkerPrintBuffer(false);
							} else {
								mPrintBuffer = mDataTask.get(index()).getPrintBuffer(false);
							}
// End of H.M.Wang 2022-12-19 追加一个串口，RS232_DOT_MARKER
						}
// End of H.M.Wang 2019-12-29 在重新生成打印缓冲区的时候，考虑网络打印的因素
						Debug.d(TAG, "===>mPrintBuffer size=" + mPrintBuffer.length);
						// H.M.Wang 2019-12-20 关闭print.bin保存
//						// H.M.Wang 2019-12-17 每次重新生成print内容后，都保存print.bin
//						BinCreater.saveBin("/mnt/sdcard/print.bin", buffer, mDataTask.get(mIndex).getInfo().mBytesPerHFeed * 8 * mDataTask.get(mIndex).getPNozzle().mHeads);
//						// End.
//						try {sleep(30);}catch(Exception e){};
// H.M.Wang 2021-4-20 增加是否为串口协议3的判断，否则，串口协议3的时候，底层会不断的申请数据，原来的判断会跳过下发数据
						if(SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_SCANER3) {
						    if(!dataSent) {
// H.M.Wang 2023-10-28 增加打印方向(Direction)和倒置(Inverse)
								if(sDirectionCmd > 0) doDirrectionCmd();
								if(sInverseCmd > 0) doInverseCmd();
// End of H.M.Wang 2023-10-28 增加打印方向(Direction)和倒置(Inverse)
								FpgaGpioOperation.writeData(FpgaGpioOperation.DATA_GENRE_NEW, FpgaGpioOperation.FPGA_STATE_OUTPUT, mPrintBuffer, mPrintBuffer.length * 2);
// H.M.Wang 2023-10-20 追加下发总数计数
								mDownWrittenCount++;
// End of H.M.Wang 2023-10-20 追加下发总数计数
// H.M.Wang 2021-1-15 追加扫描协议3，协议内容与扫描2协议完全一致，仅在打印的时候，仅可以打印一次
								dataSent = true;
								reportEmpty = true;
// End of H.M.Wang 2021-1-15 追加扫描协议3，协议内容与扫描2协议完全一致，仅在打印的时候，仅可以打印一次
// End of H.M.Wang 2021-4-20 增加是否为串口协议3的判断，否则，串口协议3的时候，底层会不断的申请数据，原来的判断会跳过下发数据
							}
// H.M.Wang 2020-11-13 检查一下底层驱动是否在要新数据，如果底层要的是新数据，这个更新数据可能就会冒名顶替，带来打印错误
						} else {
							if(FpgaGpioOperation.pollState() == 0) {
// H.M.Wang 2023-10-28 增加打印方向(Direction)和倒置(Inverse)
								if(sDirectionCmd > 0) doDirrectionCmd();
								if(sInverseCmd > 0) doInverseCmd();
// End of H.M.Wang 2023-10-28 增加打印方向(Direction)和倒置(Inverse)
								FpgaGpioOperation.writeData(FpgaGpioOperation.DATA_GENRE_UPDATE, FpgaGpioOperation.FPGA_STATE_OUTPUT, mPrintBuffer, mPrintBuffer.length * 2);
							}
						}
// End of H.M.Wang 2020-11-13 检查一下底层驱动是否在要新数据，如果底层要的是新数据，这个更新数据可能就会冒名顶替，带来打印错误
// 2020-7-21 为修改计算等待时间添加倍率变量（新公式为：N=(打印缓冲区字节数-1）/16K；时长=3/(2N+4)
//						DataRatio = (mPrintBuffer.length * 2 - 1) / (16 * 1024);
// End of 2020-7-21 为修改计算等待时间添加倍率变量（新公式为：N=(打印缓冲区字节数-1）/16K；时长=3/(2N+4)
						mHandler.sendEmptyMessageDelayed(MESSAGE_DATA_UPDATE, MESSAGE_EXCEED_TIMEOUT);
						mNeedUpdate = false;
// 2020-6-30 网络快速打印时第一次收到网络数据后下发
// H.M.Wang 2020-7-9 追加计数器重置标识
					} else if(mCounterReset) {
						mPrintBuffer = mDataTask.get(index()).getPrintBuffer(false);
						Debug.d(TAG, "Counter reset. rebuild print buffer and deliver to FPGA. size = " + mPrintBuffer.length);
						try {sleep(30);}catch(Exception e){};
// H.M.Wang 2020-11-13 检查一下底层驱动是否在要新数据，如果底层要的是新数据，这个更新数据可能就会冒名顶替，带来打印错误
						if(FpgaGpioOperation.pollState() == 0) {
// H.M.Wang 2023-10-28 增加打印方向(Direction)和倒置(Inverse)
							if(sDirectionCmd > 0) doDirrectionCmd();
							if(sInverseCmd > 0) doInverseCmd();
// End of H.M.Wang 2023-10-28 增加打印方向(Direction)和倒置(Inverse)
							FpgaGpioOperation.writeData(FpgaGpioOperation.DATA_GENRE_UPDATE, FpgaGpioOperation.FPGA_STATE_OUTPUT, mPrintBuffer, mPrintBuffer.length * 2);
						}
// End of H.M.Wang 2020-11-13 检查一下底层驱动是否在要新数据，如果底层要的是新数据，这个更新数据可能就会冒名顶替，带来打印错误
// 2020-7-21 为修改计算等待时间添加倍率变量（新公式为：N=(打印缓冲区字节数-1）/16K；时长=3/(2N+4)
//						DataRatio = (mPrintBuffer.length * 2 - 1) / (16 * 1024);
// End of 2020-7-21 为修改计算等待时间添加倍率变量（新公式为：N=(打印缓冲区字节数-1）/16K；时长=3/(2N+4)
						mNeedUpdate = false;
						mCounterReset = false;
// End of H.M.Wang 2020-7-9 追加计数器重置标识
// H.M.Wang 2020-7-14 设置计数器后再次向PC发送0001，要数据
                        if (mCallback != null) {
                            mCallback.onComplete(index());
                        }
// End of H.M.Wang 2020-7-14 设置计数器后再次向PC发送0001，要数据
					}
// End of 2020-6-30 网络快速打印时第一次收到网络数据后下发
				}

//				if(System.currentTimeMillis() - startMillis > 10) Debug.d(TAG, "Process time: " + (System.currentTimeMillis() - startMillis) + " from: " + writable);

				try { Thread.sleep(3); } catch (InterruptedException e) {Debug.e(TAG, e.getMessage());}

				//Debug.d(TAG, "===>kernel buffer empty, fill it");
				//TO-DO list 下面需要把打印数据下发

			}
//            Debug.d(TAG, "Running...Quit! ");
			mStopped = true;
// H.M.Wang 2020-7-2 由于调整计数器增量策略，在打印完成时调整，因此无需rollback
//			rollback();
// End of H.M.Wang 2020-7-2 调整计数器增量策略，在打印完成时调整，因此无需rollback
		}

		private void afterDataSent() {
// 2020-7-21 为修改计算等待时间添加倍率变量（新公式为：N=(打印缓冲区字节数-1）/16K；时长=3/(2N+4)
//			DataRatio = (mPrintBuffer.length * 2 - 1) / (16 * 1024);
// End of 2020-7-21 为修改计算等待时间添加倍率变量（新公式为：N=(打印缓冲区字节数-1）/16K；时长=3/(2N+4)

// H.M.Wang 2020-1-7 追加群组打印时，显示正在打印的MSG的序号
			if(mCallback != null && mDataTask.size() > 1) {
				mCallback.onPrint(index());
			}
// End of H.M.Wang 2020-1-7 追加群组打印时，显示正在打印的MSG的序号

			last = SystemClock.currentThreadTimeMillis();
			countDown();
//						mInkListener.onCountChanged();
//						mScheduler.schedule();
			if (mCallback != null) {
				mCallback.onComplete(index());
			}
			if (mCusCallback != null) {
				mCusCallback.onComplete(index());
			}
			LogIntercepter.getInstance(mContext).execute(getCurData());
		}
	}

	public interface CustomCallback {
		public void onComplete(int index);
	}

	private CustomCallback mCusCallback;

	public void setCusCallback(CustomCallback cb) {
		mCusCallback = cb;
	}
}
