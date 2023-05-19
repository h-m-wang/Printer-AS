package com.industry.printer.data;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;
import java.lang.System;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;

import com.industry.printer.BinInfo;
import com.industry.printer.MessageTask;
import com.industry.printer.FileFormat.QRReader;
import com.industry.printer.FileFormat.SystemConfigFile;
import com.industry.printer.PHeader.PrinterNozzle;
import com.industry.printer.Utils.ConfigPath;
import com.industry.printer.Utils.Configs;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.FileUtil;
import com.industry.printer.Utils.PlatformInfo;
import com.industry.printer.hardware.FpgaGpioOperation;
import com.industry.printer.interceptor.ExtendInterceptor;
import com.industry.printer.interceptor.ExtendInterceptor.ExtendStat;
import com.industry.printer.object.BarcodeObject;
import com.industry.printer.object.BaseObject;
import com.industry.printer.object.CounterObject;
import com.industry.printer.object.DynamicText;
import com.industry.printer.object.HyperTextObject;
import com.industry.printer.object.JulianDayObject;
import com.industry.printer.object.LetterHourObject;
import com.industry.printer.object.MessageObject;
import com.industry.printer.object.RealtimeDate;
import com.industry.printer.object.RealtimeHour;
import com.industry.printer.object.RealtimeMinute;
import com.industry.printer.object.RealtimeMonth;
import com.industry.printer.object.RealtimeObject;
import com.industry.printer.object.RealtimeSecond;
import com.industry.printer.object.RealtimeYear;
import com.industry.printer.object.ShiftObject;
import com.industry.printer.object.WeekDayObject;
import com.industry.printer.object.WeekOfYearObject;

import org.apache.http.util.CharArrayBuffer;


/**
 * 用于生成打印数据和预览图
 * @author zhaotongkai
 *
 */
public class DataTask {
	
	public static final String TAG = DataTask.class.getSimpleName();
	
	public Context	mContext;
	public ArrayList<BaseObject> mObjList;
	public MessageTask mTask;

	private ExtendStat mExtendStat;
	/**
	 * background buffer
	 *   used for save the background bin buffer
	 *   fill the variable buffer into this background buffer so we get printing buffer
	 */
	public char[] mBgBuffer;
	public char[] mPrintBuffer;
	public char[] mBuffer;
	
	private int mDots = 0;
	private int[] mDotsEach = new int[8];
	
	public boolean isReady = true;
	
	/**
	 * 背景的binInfo，
	 */
	public BinInfo mBinInfo;
	/**
	 * 保存所有变量bin文件的列表；在prepareBackgroudBuffer时解析所有变量的bin文件
	 * 然后保存到这个列表中，以便在填充打印buffer时直接从这个binInfo的列表中读取相应变量buffer
	 * 无需重新解析bin文件，提高效率
	 */
	public HashMap<BaseObject, BinInfo> mVarBinList;
	
	public DataTask(Context context, MessageTask task) {
		mContext = context;
		init(task);
	}
	
	public void setTask(MessageTask task) {
		if (task == null) {
			return;
		}
		init(task);
	}
	
	private void init(MessageTask task) {
		mTask = task;
		isReady = true;
		if (task != null) {
			mObjList = task.getObjects();
		}
		
		ExtendInterceptor interceptor = new ExtendInterceptor(mContext);
		mExtendStat = interceptor.getExtend();
		mDots = 0;
		mVarBinList = new HashMap<BaseObject, BinInfo>();
	}
	/**
	 * prepareBackgroudBuffer
	 * parse the 1.bin, and then read the file content into mBgBuffer, one bit extends to one byte
	 */
	public boolean prepareBackgroudBuffer()
	{
		if (mTask == null) {
			return false;
		}
		
		/**记录当前打印的信息路径**/
		mBinInfo = new BinInfo(ConfigPath.getBinAbsolute(mTask.getName()), mTask, mExtendStat);
		if (mBinInfo == null) {
			Debug.e(TAG, "--->binInfo null");
			return false;
		}
		mBgBuffer = mBinInfo.getBgBuffer();
		
		if (mBgBuffer == null) {
			return false;
		}
		
		Debug.d(TAG, "--->bgbuffer = " + mBgBuffer.length);
		mPrintBuffer = new char[mBgBuffer.length];
		return true;
	}
// H.M.Wang 2020-6-16 追加是否保存print.bin标记，以控制保存行为
	public char[] getPrintBuffer(boolean bSave) {
		return getPrintBuffer(false, bSave);
	}
// End of H.M.Wang 2020-6-16 追加是否保存print.bin标记，以控制保存行为

	private char[] getPrintBuffer(boolean isPreview, boolean bSave) {
		Debug.d(TAG, "--->getPrintBuffer");
		long startTime = System.currentTimeMillis();

		if (mBgBuffer == null) {
			return null;
		}
//		Debug.d(TAG, "--->getPrintBuffer  111" );
		CharArrayReader cReader = new CharArrayReader(mBgBuffer);

		// H.M.Wang 将1.bin写入打印缓冲区，然后将v*.bin写入缓冲区
		try {
// H.M.Wang 2020-7-27 由于32DN会对mPrintBuffer进行扩容，如果这里不重新生成，则会使用扩容后的容量，再次被扩容，这使得打印缓冲区持续放大
			mPrintBuffer = new char[mBgBuffer.length];
// End of H.M.Wang 2020-7-27 由于32DN会对mPrintBuffer进行扩容，如果这里不重新生成，则会使用扩容后的容量，再次被扩容，这使得打印缓冲区持续放大
			cReader.read(mPrintBuffer);
			if (isNeedRefresh()) {
				// 将v*.bin写入缓冲区
				refreshVariables(isPreview);
			}
		} catch (IOException e) {
			Debug.d(TAG, "--->e : " + e.getMessage());
		}
// H.M.Wang 2021-7-28 放开该部分功能，在获取预览图的时候，直接返回生成的还未进行加工的图
// H.M.Wang 2020-6-30 这段代码可能会在isPreview=true时，导致后面的处理不能进行，应该注释掉
		if (isPreview) {
			return mPrintBuffer;
		}
// End of H.M.Wang 2020-6-30 这段代码可能会在isPreview=true时，导致后面的处理不能进行
// End of H.M.Wang 2021-7-28 放开该部分功能，在获取预览图的时候，直接返回生成的还未进行加工的图

// H.M.Wang 2022-6-11 删除打印缓冲区后部的空白
		int rmCols = 0;
		boolean notZero = false;
		while(!notZero) {
			for(int i=1; i<=mBinInfo.getCharsFeed(); i++) {
				if(mPrintBuffer.length-rmCols*mBinInfo.getCharsFeed()-i < 0) {
					notZero = true;
					break;
				}
				if(mPrintBuffer[mPrintBuffer.length-rmCols*mBinInfo.getCharsFeed()-i] != 0x0000) {
					notZero = true;
					break;
				}
			}
			if(!notZero) rmCols++;
		}
		if(rmCols > 0) {
			char[] pbuf = new char[mPrintBuffer.length - rmCols * mBinInfo.getCharsFeed()];
			System.arraycopy(mPrintBuffer, 0, pbuf, 0, pbuf.length);
			mPrintBuffer = pbuf;
			mBinInfo.mColumn -= rmCols;
		}
// End of H.M.Wang 2022-6-11 删除打印缓冲区后部的空白

// H.M.Wang 2020-7-23 追加32DN打印头时的移位处理
		if(mTask.getNozzle() == PrinterNozzle.MESSAGE_TYPE_32DN) {
//			Debug.d(TAG, "mPrintBuffer.length = " + mPrintBuffer.length);
// H.M.Wang 2022-3-29 追加32DN的双列打印，根据slant的设置，如果slant==0，则按着原来的操作，如果不为0，则按着bitShiftFor32DNSlant的说明操作
            int slant = SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_SLANT);
/* 2022-4-3 修改 by 吕总要求
32DN， 也要改一下，
选择32DN 喷头时：、
1 . Slant =0 , 不变
2. Slant>8, 维持上周做的逻辑不变
3. 1<=slant<=8.    调整逻辑：
a:  32 bit 每列 变为 64 bit/列。 （规则和=0 相同）
b:  按slant 设置，  和=0 做相同偏移， 不过=0 是固定移动4 列， 这个是按slant设置，值移动1-8 列
*/
// H.M.Wang 2022-4-4 按着吕总要求修改
//			if(slant == 0) {
//				mPrintBuffer = bitShiftFor32DN();
            if(slant >= 0 && slant <= 8) {
                mPrintBuffer = bitShiftFor32DN(slant);
// End of H.M.Wang 2022-4-4 按着吕总要求修改
            } else {
                mPrintBuffer = bitShiftFor32DNSlant(slant);
            }
// End of H.M.Wang 2022-3-29 追加32DN的双列打印，根据slant的设置，如果slant==0，则按着原来的操作，如果不为0，则按着bitShiftFor32DNSlant的说明操作
//			Debug.d(TAG, "mPrintBuffer.length = " + mPrintBuffer.length);
//			Debug.d(TAG, mTask.getPath() + "/print.bin");
//			BinCreater.saveBin(mTask.getPath() + "/printDN.bin", mPrintBuffer, 64);
		}
// End of H.M.Wang 2020-7-23 追加32DN打印头时的移位处理

// H.M.Wang 2020-8-17 追加32SN打印头
		if(mTask.getNozzle() == PrinterNozzle.MESSAGE_TYPE_32SN) {
//			Debug.d(TAG, "mPrintBuffer.length = " + mPrintBuffer.length);
			mPrintBuffer = bitShiftFor32SN();
//			Debug.d(TAG, "mPrintBuffer.length = " + mPrintBuffer.length);
//			Debug.d(TAG, mTask.getPath() + "/print.bin");
//			BinCreater.saveBin(mTask.getPath() + "/print.bin", mPrintBuffer, 64);
		}
// End of H.M.Wang 2020-8-17 追加32SN打印头

		// H.M.Wang 追加下列8行
//		if(mTask.getNozzle() == PrinterNozzle.MESSAGE_TYPE_64_DOT && Configs.getMessageShift(3) == 1) {
		if(mTask.getNozzle() == PrinterNozzle.MESSAGE_TYPE_64_DOT) {
//			Debug.d(TAG, "mPrintBuffer.length = " + mPrintBuffer.length);
			mPrintBuffer = evenBitShiftFor64Dot();
//			Debug.d(TAG, "mPrintBuffer.length = " + mPrintBuffer.length);
//			Debug.d(TAG, mTask.getPath() + "/print.bin");
//			BinCreater.saveBin(mTask.getPath() + "/print.bin", mPrintBuffer, 64);
		}
// H.M.Wang 2022-5-27 追加32x2头类型
		if(mTask.getNozzle() == PrinterNozzle.MESSAGE_TYPE_32X2) {
			mPrintBuffer = bitShiftFor32X2();
		}
// End of H.M.Wang 2022-5-27 追加32x2头类型

// H.M.Wang 2021-8-16 追加96DN头
		if(mTask.getNozzle() == PrinterNozzle.MESSAGE_TYPE_96DN) {
			mPrintBuffer = evenBitShiftFor96Dot();
		}
// End of H.M.Wang 2021-8-16 追加96DN头

// H.M.Wang 2020-9-6 取消64SN的打印缓冲区转换
//// H.M.Wang 2020-8-26 追加64SN打印头
//		if(mTask.getNozzle() == PrinterNozzle.MESSAGE_TYPE_64SN) {
////			Debug.d(TAG, "mPrintBuffer.length = " + mPrintBuffer.length);
//			mPrintBuffer = bitShiftFor64SN();
////			Debug.d(TAG, "mPrintBuffer.length = " + mPrintBuffer.length);
////			Debug.d(TAG, mTask.getPath() + "/print.bin");
////			BinCreater.saveBin(mTask.getPath() + "/print.bin", mPrintBuffer, 64);
//		}
// End of H.M.Wang 2020-8-26 追加64SN打印头
// End of H.M.Wang 2020-9-6 取消64SN的打印缓冲区转换

//		BinCreater.saveBin("/mnt/sdcard/print1.bin", mPrintBuffer, 32);
///./...		Debug.d(TAG, "--->BytesPerColumn: " + mBinInfo.mBytesPerColumn);
//		if (mBinInfo.mBytesPerColumn == 4)  {
//			evenBitShift();
//		} // else{
			/*完成平移/列变换得到真正的打印buffer*/

		SystemConfigFile sysconf = SystemConfigFile.getInstance(mContext);

// H.M.Wang 2021-7-23 对应于重复打印次数，横向复制打印缓冲区
///./...		Debug.d(TAG, "INDEX_PRINT_TIMES = " + sysconf.getParam(SystemConfigFile.INDEX_PRINT_TIMES));
		if(sysconf.getParam(SystemConfigFile.INDEX_PRINT_TIMES) > 1 && sysconf.getParam(SystemConfigFile.INDEX_PRINT_TIMES) < 31) {
			int maxColNumPerUnit = 0;
			if( sysconf.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_12_7 ||
				sysconf.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_25_4 ||
				sysconf.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_38_1 ||
				sysconf.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_50_8 ||
				sysconf.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_1_INCH ||
// H.M.Wang 2022-4-29 追加25.4x10头类型
				sysconf.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_254X10 ||
// End of H.M.Wang 2022-4-29 追加25.4x10头类型
				sysconf.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_1_INCH_DUAL ||
				sysconf.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_1_INCH_TRIPLE ||
				sysconf.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_1_INCH_FOUR ||
				sysconf.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_9MM ) {
				if(Configs.GetDpiVersion() == FpgaGpioOperation.DPI_VERSION_150) {
					maxColNumPerUnit = sysconf.getParam(SystemConfigFile.INDEX_REPEAT_PRINT) * 6;	// 1mm有6列
				} else {
					maxColNumPerUnit = sysconf.getParam(SystemConfigFile.INDEX_REPEAT_PRINT) * 12;	// 1mm有12列
				}
			} else if (
				sysconf.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_16_DOT ||
				sysconf.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_32_DOT ||
				sysconf.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_32DN ||
				sysconf.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_32SN ||
				sysconf.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_64_DOT ||
// H.M.Wang 2022-5-27 追加32x2头类型
				sysconf.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_32X2 ||
// End of H.M.Wang 2022-5-27 追加32x2头类型
// H.M.Wang 2022-10-19 追加64SLANT头
				sysconf.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_64SLANT ||
// End of H.M.Wang 2022-10-19 追加64SLANT头
				sysconf.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_64SN ||
// H.M.Wang 2021-8-16 追加96DN头
				sysconf.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_96DN ) {
// End of H.M.Wang 2021-8-16 追加96DN头
// H.M.Wang 2021-8-20 由于复制操作移到了倾斜操作之前，所以不需要这个SLANT的判断了
//				if(sysconf.getParam(SystemConfigFile.INDEX_SLANT) >= 100) {
//					maxColNumPerUnit = sysconf.getParam(SystemConfigFile.INDEX_REPEAT_PRINT) * 8;
//				} else {
					maxColNumPerUnit = sysconf.getParam(SystemConfigFile.INDEX_REPEAT_PRINT) / 4;		//4mm一列
//				}
// End of H.M.Wang 2021-8-20 由于复制操作移到了倾斜操作之前，所以不需要这个SLANT的判断了
			}

			Debug.d(TAG, "maxColNumPerUnit = " + maxColNumPerUnit + "; mBinInfo.getBytesFeed() / 2 = " + mBinInfo.getBytesFeed() / 2);
			if(maxColNumPerUnit != 0) {
				CharArrayBuffer caBuf = new CharArrayBuffer(0);
				int emptyChars = (maxColNumPerUnit - mBinInfo.mColumn) * mBinInfo.getBytesFeed() / 2;	// 不能用mBinInfo.mCharsPerColumn，因为这个变量是基于没有做过调整的mBytesPerColumn算的，如果mBytesPerColumn少一个字节，那么就会少一个字
				emptyChars = (emptyChars < 0 ? 0 : emptyChars);
				char[] empty = new char[emptyChars];
				Arrays.fill(empty, (char)0x0000);

				Debug.d(TAG, "emptyChars = " + emptyChars);
				for(int i=0; i<sysconf.getParam(SystemConfigFile.INDEX_PRINT_TIMES); i++) {
					if(i != 0) {
						caBuf.append(empty, 0, emptyChars);
					}
					if(i < sysconf.getParam(SystemConfigFile.INDEX_PRINT_TIMES) - 1) {
						caBuf.append(mPrintBuffer, 0, Math.min(mPrintBuffer.length, maxColNumPerUnit * mBinInfo.getBytesFeed() / 2));
					} else {
						caBuf.append(mPrintBuffer, 0, mPrintBuffer.length);
					}
				}
				mPrintBuffer = caBuf.toCharArray();
			}
		}
// End of H.M.Wang 2021-7-23 对应于重复打印次数，横向复制打印缓冲区

// H.M.Wang 2022-5-5 将MB的偏移（25.4x10头偏移）单独处理
		if(sysconf.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_254X10) {
			rebuildBuffer254x10();
		} else {
			rebuildBuffer();
		}
// End of H.M.Wang 2022-5-5 将MB的偏移（25.4x10头偏移）单独处理

		// }
		//BinCreater.Bin2Bitmap(mPrintBuffer);
		/*test bin*/
		/*
		byte[] buffer = new byte[mBgBuffer.length * 2];
		for (int i = 0; i < buffer.length/2; i++) {
			buffer[2*i] = (byte)(mBgBuffer[i] & 0x00ff);
			buffer[2*i+1] = (byte) (((int) mBgBuffer[i])/256 & 0x00ff);
		}
		BinCreater.saveBin("/mnt/usbhost1/print.bin", buffer, 32);
		*/
		/*test bin*/
///./...		Debug.d(TAG, "--->buffer = " + mBuffer.length);

// H.M.Wang 2020-4-18 从DataTransferThread移至此
        if(bSave) {
            FileUtil.deleteFolder("/mnt/sdcard/print.bin");
			BinCreater.saveBin("/mnt/sdcard/print.bin", mBuffer, mBinInfo.mBytesPerHFeed * 8 * mTask.getNozzle().mHeads);
        }
// End of H.M.Wang 2020-4-18 从DataTransferThread移至此

// H.M.Wang 2020-4-18 追加12.7R5头
// H.M.Wang 2020-5-9 12.7R5d打印头类型不参与信息编辑，因此不通过信息的打印头类型判断其是否为12.7R5的信息，而是通过参数来规定现有信息的打印行为
//        SystemConfigFile sysconf = SystemConfigFile.getInstance(mContext);
//		Debug.d(TAG, "Params = " + sysconf.getParam(SystemConfigFile.INDEX_HEAD_TYPE));
//		Debug.d(TAG, "Nozzle Index = " + PrinterNozzle.MessageType.NOZZLE_INDEX_12_7_R5);
// H.M.Wang 2020-5-21 12.7R5头改为RX48，追加RX50头
//		if(sysconf.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_12_7_R5) {
		if(sysconf.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_R6X48 ||
			sysconf.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_R6X50) {
// End of H.M.Wang 2020-5-21 12.7R5头改为RX48，追加RX50头

//		if(mTask.getNozzle() == PrinterNozzle.MESSAGE_TYPE_12_7_R5) {
// End of H.M.Wang 2020-5-9 12.7R5d打印头类型不参与信息编辑，因此不通过信息的打印头类型判断其是否为12.7R5的信息，而是通过参数来规定现有信息的打印行为
			CharArrayBuffer caBuf = new CharArrayBuffer(0);
			int orgCharsOfHead = mBinInfo.mCharsPerHFeed * mTask.getNozzle().mHeads;
			int orgCols = mBuffer.length / orgCharsOfHead;
			char[] empty = new char[orgCharsOfHead];
			Arrays.fill(empty, (char)0x0000);

// H.M.Wang 2020-5-21 12.7R5头改为RX48，追加RX50头
			int maxColNumPerUnit = 0;
			if(sysconf.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_R6X48) {
				maxColNumPerUnit = PrinterNozzle.R6X48_MAX_COL_NUM_EACH_UNIT;
			} else if(sysconf.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_R6X50) {
				maxColNumPerUnit = PrinterNozzle.R6X50_MAX_COL_NUM_EACH_UNIT;
			}
// H.M.Wang 2021-4-23 增加根据DPI对列数进行调整
			maxColNumPerUnit *= Configs.GetDpiVersion();
// End of H.M.Wang 2021-4-23 增加根据DPI对列数进行调整

			for(int i=0; i<PrinterNozzle.R6_PRINT_COPY_NUM; i++) {
				for(int k=0; k<maxColNumPerUnit; k++) {
					for(int j=0; j<PrinterNozzle.R6_HEAD_NUM; j++) {
// End of H.M.Wang 2020-5-21 12.7R5头改为RX48，追加RX50头
// H.M.Wang 2021-3-18 取消周后一个单元后面添加空格
						if(j % 2 != 0 && i == 0) {							// 双数行第一列
							caBuf.append(empty, 0, orgCharsOfHead);
						} else if(k >= orgCols) {							// 原始块中宽度不足部分
							if(i < PrinterNozzle.R6_PRINT_COPY_NUM) {		// 不是最后一列
								caBuf.append(empty, 0, orgCharsOfHead);
							}
// End of H.M.Wang 2021-3-18 取消周后一个单元后面添加空格
						} else {
							caBuf.append(mBuffer, k * orgCharsOfHead, orgCharsOfHead);
						}
					}
				}
			}

			mBuffer = caBuf.toCharArray();

            if(bSave) {
                FileUtil.deleteFolder("/mnt/sdcard/printR6.bin");
                BinCreater.saveBin("/mnt/sdcard/printR6.bin", mBuffer, mBinInfo.mBytesPerHFeed * 8 * mTask.getNozzle().mHeads * PrinterNozzle.R6_HEAD_NUM);
            }
		}
// End of H.M.Wang 2020-4-18 追加12.7R5头

// H.M.Wang 2021-8-25 追加E5X48和E5X50头类型
		if( sysconf.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E5X48 ||
			sysconf.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E5X50) {

			CharArrayBuffer caBuf = new CharArrayBuffer(0);
			int orgCharsOfHead = mBinInfo.mCharsPerHFeed * mTask.getNozzle().mHeads;
			int orgCols = mBuffer.length / orgCharsOfHead;
			char[] empty = new char[orgCharsOfHead];
			Arrays.fill(empty, (char)0x0000);

			int maxColNumPerUnit = 0;
			if(sysconf.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E5X48) {
				maxColNumPerUnit = PrinterNozzle.E5X48_MAX_COL_NUM_EACH_UNIT;
			} else if(sysconf.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E5X50) {
				maxColNumPerUnit = PrinterNozzle.E5X50_MAX_COL_NUM_EACH_UNIT;
			}

// H.M.Wang 2021-4-23 增加根据DPI对列数进行调整
			maxColNumPerUnit *= Configs.GetDpiVersion();
// End of H.M.Wang 2021-4-23 增加根据DPI对列数进行调整

			for(int i=0; i<PrinterNozzle.E5_PRINT_COPY_NUM; i++) {
				for(int k=0; k<maxColNumPerUnit; k++) {
// H.M.Wang 2021-8-27 E5头在减锁的时候按着5个头计算，但是生成打印缓冲区的时候按6个头生成
					for(int j=0; j<PrinterNozzle.E5_HEAD_NUM+1; j++) {  // 生成打印缓冲区的时候，按着6个头的空间生成
// End of H.M.Wang 2021-8-27 E5头在减锁的时候按着5个头计算，但是生成打印缓冲区的时候按6个头生成
						if(k >= orgCols) {	// 原始块中宽度不足部分
							if(i < PrinterNozzle.E5_PRINT_COPY_NUM - 1) {    // 原始块中宽度不足部分
								caBuf.append(empty, 0, orgCharsOfHead);
							}
						} else {
							caBuf.append(mBuffer, k * orgCharsOfHead, orgCharsOfHead);
						}
					}
				}
			}

			mBuffer = caBuf.toCharArray();

			if(bSave) {
				FileUtil.deleteFolder("/mnt/sdcard/printE5.bin");
				BinCreater.saveBin("/mnt/sdcard/printE5.bin", mBuffer, mBinInfo.mBytesPerHFeed * 8 * mTask.getNozzle().mHeads * PrinterNozzle.E5_HEAD_NUM);
			}
		}
// End of H.M.Wang 2021-8-25 追加E5X48和E5X50头类型

// H.M.Wang 2021-3-6 追加E6X48,E6X50头
		if( sysconf.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E6X48 ||
			sysconf.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E6X50) {

			CharArrayBuffer caBuf = new CharArrayBuffer(0);
			int orgCharsOfHead = mBinInfo.mCharsPerHFeed * mTask.getNozzle().mHeads;
			int orgCols = mBuffer.length / orgCharsOfHead;
			char[] empty = new char[orgCharsOfHead];
			Arrays.fill(empty, (char)0x0000);

			int maxColNumPerUnit = 0;
			if(sysconf.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E6X48) {
				maxColNumPerUnit = PrinterNozzle.E6X48_MAX_COL_NUM_EACH_UNIT;
			} else if(sysconf.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E6X50) {
				maxColNumPerUnit = PrinterNozzle.E6X50_MAX_COL_NUM_EACH_UNIT;
			}

// H.M.Wang 2021-4-23 增加根据DPI对列数进行调整
			maxColNumPerUnit *= Configs.GetDpiVersion();
// End of H.M.Wang 2021-4-23 增加根据DPI对列数进行调整

			for(int i=0; i<PrinterNozzle.E6_PRINT_COPY_NUM; i++) {
				for(int k=0; k<maxColNumPerUnit; k++) {
					for(int j=0; j<PrinterNozzle.E6_HEAD_NUM; j++) {
// H.M.Wang 2021-3-18 取消奇数行(第一行为0)的向后位移一个单位的操作)
//						if((j % 2 == 0 && i == PrinterNozzle.E6_PRINT_COPY_NUM - 1) || 	// 单数行最后一列
//								(j % 2 != 0 && i == 0) ||    									// 双数行第一列
						if(
// End of H.M.Wang 2021-3-18 取消奇数行(第一行为0)的向后位移一个单位的操作)
// H.M.Wang 2021-3-18 取消周后一个单元后面添加空格
//								(k >= orgCols)) {												// 原始块中宽度不足部分
								k >= orgCols) {	// 原始块中宽度不足部分
							if(i < PrinterNozzle.E6_PRINT_COPY_NUM - 1) {    // 原始块中宽度不足部分
								caBuf.append(empty, 0, orgCharsOfHead);
							}
// H.M.Wang 2021-3-18 取消奇数行(第一行为0)的向后位移一个单位的操作)
						} else {
							caBuf.append(mBuffer, k * orgCharsOfHead, orgCharsOfHead);
						}
					}
				}
			}

			mBuffer = caBuf.toCharArray();

			if(bSave) {
				FileUtil.deleteFolder("/mnt/sdcard/printE6.bin");
				BinCreater.saveBin("/mnt/sdcard/printE6.bin", mBuffer, mBinInfo.mBytesPerHFeed * 8 * mTask.getNozzle().mHeads * PrinterNozzle.E6_HEAD_NUM);
			}
		}
// End of H.M.Wang 2021-3-6 追加E6X48,E6X50头
		if(sysconf.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E6X1) {
			CharArrayBuffer caBuf = new CharArrayBuffer(0);
			int orgCharsOfHead = mBinInfo.mCharsPerHFeed * mTask.getNozzle().mHeads;
			int orgCols = mBuffer.length / orgCharsOfHead;

			for(int i=0; i<orgCols; i++) {
				for(int j=0; j<PrinterNozzle.E6_HEAD_NUM; j++) {
					caBuf.append(mBuffer, i * orgCharsOfHead, orgCharsOfHead);
				}
			}

			mBuffer = caBuf.toCharArray();

			if(bSave) {
				FileUtil.deleteFolder("/mnt/sdcard/printE1.bin");
				BinCreater.saveBin("/mnt/sdcard/printE1.bin", mBuffer, mBinInfo.mBytesPerHFeed * 8 * mTask.getNozzle().mHeads * PrinterNozzle.E6_HEAD_NUM);
			}
		}

		if(sysconf.getParam(SystemConfigFile.INDEX_PRINT_TIMES) > 1 && sysconf.getParam(SystemConfigFile.INDEX_PRINT_TIMES) < 31) {
			if(bSave) {
				FileUtil.deleteFolder("/mnt/sdcard/printRpt.bin");
				BinCreater.saveBin("/mnt/sdcard/printRpt.bin", mBuffer, mBinInfo.mBytesPerHFeed * 8 * mTask.getNozzle().mHeads);
			}
		}

// H.M.Wang 2020-10-23 计算点数从DataTransferThread移到这里
		calDots();
// End of H.M.Wang 2020-10-23 计算点数从DataTransferThread移到这里

// H.M.Wang 2022-6-11 删除打印缓冲区后部的空白
		mBinInfo.mColumn += rmCols;
// End of H.M.Wang 2022-6-11 删除打印缓冲区后部的空白

		Debug.d(TAG, "--->getPrintBuffer: " + (System.currentTimeMillis() - startTime));

		return mBuffer;
	}

// H.M.Wang 2020-10-23 计算点数从DataTransferThread移到这里
	private void calDots(){
//		Debug.d(TAG, "GetPrintDots Start Time: " + System.currentTimeMillis());
// H.M.Wang 2020-10-18 重新开放打印前计算墨点数
		int[] dots = NativeGraphicJni.GetPrintDots(mPrintBuffer, mPrintBuffer.length, getInfo().mCharsPerHFeed, getPNozzle().mHeads);

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
// H.M.Wang 2022-10-19 追加64SLANT头
				head != PrinterNozzle.MESSAGE_TYPE_64SLANT &&
// End of H.M.Wang 2022-10-19 追加64SLANT头
// H.M.Wang 2022-5-27 追加32x2头类型
				head != PrinterNozzle.MESSAGE_TYPE_32X2 &&
// End of H.M.Wang 2022-5-27 追加32x2头类型
				head != PrinterNozzle.MESSAGE_TYPE_64_DOT &&
// H.M.Wang 2021-8-16 追加96DN头
				head != PrinterNozzle.MESSAGE_TYPE_96DN) {
// End of H.M.Wang 2021-8-16 追加96DN头
                dots[j] *= 2;
			} else {
				dots[j] *= 200;
			}
			totalDot += dots[j];
		}
		setDots(totalDot);
		setDotsEach(dots);
// End of H.M.Wang 2020-10-18 重新开放打印前计算墨点数
//		Debug.d(TAG, "GetPrintDots Done Time: " + System.currentTimeMillis());
	}
// End of H.M.Wang 2020-10-23 计算点数从DataTransferThread移到这里

	private boolean isNeedRebuild() {
		MessageObject object = mTask.getMsgObject();
		PrinterNozzle nozzle = object.getPNozzle();

// 2022-9-1 因为64SN头需要按着4个头进行变形操作，但是PrinterNozzle当中定义的是1个头，因此这里要偷梁换柱一下，否则无法检查到各个头的变形参数设置
		int heads = nozzle.mHeads;
		if( nozzle == PrinterNozzle.MESSAGE_TYPE_16_DOT ||
			nozzle == PrinterNozzle.MESSAGE_TYPE_32_DOT ||
			nozzle == PrinterNozzle.MESSAGE_TYPE_32DN ||
			nozzle == PrinterNozzle.MESSAGE_TYPE_32SN ||
			nozzle == PrinterNozzle.MESSAGE_TYPE_64SN ||
// H.M.Wang 2022-10-19 追加64SLANT头
			nozzle == PrinterNozzle.MESSAGE_TYPE_64SLANT ||
// End of H.M.Wang 2022-10-19 追加64SLANT头
			nozzle == PrinterNozzle.MESSAGE_TYPE_32X2 ||
			nozzle == PrinterNozzle.MESSAGE_TYPE_64_DOT ||
			nozzle == PrinterNozzle.MESSAGE_TYPE_96DN) {
			heads = 4;
		}
//		for (int i = 0; i < nozzle.mHeads; i++) {
		for (int i = 0; i < heads; i++) {
// End of 2022-9-1 因为64SN头需要按着4个头进行变形操作，但是PrinterNozzle当中定义的是1个头，因此这里要偷梁换柱一下，否则无法检查到各个头的变形参数设置
			int shift = nozzle.shiftEnable ? Configs.getMessageShift(i) : 0;
			if (shift > 0 ) {
				return true;
			}
			int mirror = nozzle.mirrorEnable ? Configs.getMessageDir(i) : SystemConfigFile.DIRECTION_NORMAL;
			if (mirror == SystemConfigFile.DIRECTION_REVERS) {
				return true;
			}

		}
		int revert = 0;
		SystemConfigFile sysconf = SystemConfigFile.getInstance(mContext);
		if (nozzle.reverseEnable) {
			if (sysconf.getParam(14) > 0) {
				revert |= 0x01;
			}
			if (sysconf.getParam(15) > 0) {
				revert |= 0x02;
			}
// H.M.Wang 2021-2-20 修改原来笔误，20->22, 21->23
			if (sysconf.getParam(22) > 0) {
				revert |= 0x04;
			}
			if (sysconf.getParam(23) > 0) {
				revert |= 0x08;
			}
// End of H.M.Wang 2021-2-20 修改原来笔误，20->22, 21->23
		}
		if (revert > 0 ) {
			return true;
		}
		int rotate = nozzle.rotateAble ? sysconf.getParam(SystemConfigFile.INDEX_SLANT): 0;
		if (rotate > 0) {
			return  true;
		}
		return false;
	}

// H.M.Wang 2020-11-13 追加这个函数，当日，时和分有变化时重新生成打印缓冲区
    public boolean contentChanged() {
        boolean changed = false;

        for (BaseObject object : mObjList) {
            changed |= object.contentChanged();
        }

        return changed;
    }
// End of H.M.Wang 2020-11-13 追加这个函数，当日，时和分有变化时重新生成打印缓冲区

	public void refreshVariables(boolean prev)
	{
		float scaleW = 2, scaleH = 1;
		String substr=null;
		char[] var;
		if(mObjList==null || mObjList.isEmpty())
			return;
		int heads = mTask.getHeads() == 0 ? 1 : mTask.getHeads();
		SystemConfigFile config = SystemConfigFile.getInstance(mContext);
		ExtendInterceptor interceptor = new ExtendInterceptor(mContext);
		ExtendStat stat = interceptor.getExtend();
		float div = (float) (2.0/heads);
		MessageObject msg = mTask.getMsgObject();
		// Debug.d(TAG, "+++++type:" + msg.getType());
//		if (msg != null && (msg.getType() == MessageType.MESSAGE_TYPE_1_INCH || msg.getType() == MessageType.MESSAGE_TYPE_1_INCH_FAST)) {
//			div = 1;
//			scaleW = 1;
//			scaleH = 0.5f;
//		} else if (msg != null && (msg.getType() == MessageType.MESSAGE_TYPE_1_INCH_DUAL || msg.getType() == MessageType.MESSAGE_TYPE_1_INCH_DUAL_FAST)) {
//			div = 0.5f;
//			scaleW = 0.5f;
//			scaleH = 0.25f;
//		} else if (msg != null && msg.getType() == MessageType.MESSAGE_TYPE_16_DOT) {
//			div = 152f/16f;
//		}
//		/**if high resolution message, do not divide width by 2 */
//		if (msg.getResolution()) {
//			Debug.d(TAG, "--->High Resolution");
//			scaleW = scaleW/2;
//			div = div/2;
//		}

		PrinterNozzle headType = mTask.getNozzle();
// H.M.Wang 2021-1-8 取消这个计算好像不行，12.7多头的时候似乎有问题
// H.M.Wang 2020-10-29 取消在计算scale的计算，采用Nozzle类里面的计算值
		if (headType == PrinterNozzle.MESSAGE_TYPE_1_INCH) {
// H.M.Wang 修改
//			div = 1;
//			scaleW = 1;
			scaleW /= 1.0f * 308 / 152;
			div = scaleW;
			scaleH = 0.5f;
// H.M.Wang 2022-4-29 追加25.4x10头类型
		} else if (headType == PrinterNozzle.MESSAGE_TYPE_254X10) {
			scaleW /= 10.0f * 308 / 152;
			div = scaleW;
			scaleH = 0.05f;
// End of H.M.Wang 2022-4-29 追加25.4x10头类型
		} else if (headType == PrinterNozzle.MESSAGE_TYPE_1_INCH_DUAL) {
// H.M.Wang 修改
//			div = 0.5f;
//			scaleW = 0.5f;
			scaleW /= 2.0f * 308 / 152;
			div = scaleW;
			scaleH = 0.25f;
		// H.M.Wang 追加下列8行
		} else if (headType == PrinterNozzle.MESSAGE_TYPE_1_INCH_TRIPLE) {
// H.M.Wang 修改
//			div = 0.3333333333f;
//			scaleW = 0.3333333333f;
			scaleW /= 3.0f * 308 / 152;
			div = scaleW;
			scaleH = 0.1666666667f;
		} else if (headType == PrinterNozzle.MESSAGE_TYPE_1_INCH_FOUR) {
// H.M.Wang 修改
//			div = 0.25f;
//			scaleW = 0.25f;
			scaleW /= 4.0f * 308 / 152;
			div = scaleW;
			scaleH = 0.125f;
		} else if (headType == PrinterNozzle.MESSAGE_TYPE_16_DOT) {
			div = 152f/16f;
			scaleW = 152f/16;
			scaleH = 152f/16;
// H.M.Wang 2020-7-23 追加32DN打印头
//		} else if (headType == PrinterNozzle.MESSAGE_TYPE_32_DOT) {
// H.M.Wang 2020-8-17 追加32SN打印头
//		} else if (headType == PrinterNozzle.MESSAGE_TYPE_32_DOT || headType == PrinterNozzle.MESSAGE_TYPE_32DN) {
		} else if (headType == PrinterNozzle.MESSAGE_TYPE_32_DOT || headType == PrinterNozzle.MESSAGE_TYPE_32DN || headType == PrinterNozzle.MESSAGE_TYPE_32SN) {
// End of H.M.Wang 2020-8-17 追加32SN打印头
			div = 152f/32f;
			scaleW = 152f/32;
			scaleH = 152f/32;
// End of H.M.Wang 2020-7-23 追加32DN打印头

		// H.M.Wang 追加下列两行
// H.M.Wang 2020-8-26 追加64SN打印头
//		} else if (headType == PrinterNozzle.MESSAGE_TYPE_64_DOT) {
// H.M.Wang 2022-5-27 追加32x2头类型
//		} else if (headType == PrinterNozzle.MESSAGE_TYPE_64_DOT || headType == PrinterNozzle.MESSAGE_TYPE_64SN) {
// H.M.Wang 2022-10-19 追加64SLANT头
//		} else if (headType == PrinterNozzle.MESSAGE_TYPE_64_DOT || headType == PrinterNozzle.MESSAGE_TYPE_64SN || headType == PrinterNozzle.MESSAGE_TYPE_32X2) {
		} else if (headType == PrinterNozzle.MESSAGE_TYPE_64_DOT || headType == PrinterNozzle.MESSAGE_TYPE_64SN || headType == PrinterNozzle.MESSAGE_TYPE_32X2 || headType == PrinterNozzle.MESSAGE_TYPE_64SLANT) {
// End of H.M.Wang 2022-10-19 追加64SLANT头
// End of H.M.Wang 2022-5-27 追加32x2头类型
// H.M.Wang 2020-8-26 追加64SN打印头
			div = 152f/64f;
			scaleW = 152f/64;
			scaleH = 152f/64;
// H.M.Wang 2021-3-6 追加E6X48,E6X50头
//		} else if (headType == PrinterNozzle.MESSAGE_TYPE_9MM) {
		} else if (headType == PrinterNozzle.MESSAGE_TYPE_9MM ||
			headType == PrinterNozzle.MESSAGE_TYPE_E6X48 ||
			headType == PrinterNozzle.MESSAGE_TYPE_E6X50 ||
// End of H.M.Wang 2021-3-6 追加E6X48,E6X50头
// H.M.Wang 2021-8-25 追加E5X48和E5X50头类型
			headType == PrinterNozzle.MESSAGE_TYPE_E5X48 ||
			headType == PrinterNozzle.MESSAGE_TYPE_E5X50 ||
// End of H.M.Wang 2021-8-25 追加E5X48和E5X50头类型
			headType == PrinterNozzle.MESSAGE_TYPE_E6X1 ) {
// H.M.Wang 2021-4-23 修改div和scaleW的计算公式，当前的计算可能不对
//			div = 1.0f * 104/104;
			scaleW = 152f / 104f * 2;
			div = scaleW;
// H.M.Wang 2021-4-23 修改div和scaleW的计算公式，当前的计算可能不对
			scaleH = 152f / 104f;
// H.M.Wang 2021-8-16 追加96DN头
		} else if (headType == PrinterNozzle.MESSAGE_TYPE_96DN) {
			div = 152f/96f;
			scaleW = 152f/96;
			scaleH = 152f/96;
// End of H.M.Wang 2021-8-16 追加96DN头
		}

//		scaleW = 1.0f * headType.getFactorScale() / headType.getScaleW();
//		div = scaleW;
//		scaleH = 1.0f / headType.getScaleW();
// End of H.M.Wang 2020-10-29 取消在计算scale的计算，采用Nozzle类里面的计算值
// End of H.M.Wang 2021-1-8 取消这个计算好像不行，12.7多头的时候似乎有问题

		/**if high resolution message, do not divide width by 2 */
// H.M.Wang 2021-4-9 追加ioctl的分辨率信息获取命令
//		if (msg.getResolution()) {
		if (Configs.GetDpiVersion() == FpgaGpioOperation.DPI_VERSION_300) {
// End of H.M.Wang 2021-4-9 追加ioctl的分辨率信息获取命令
// H.M.Wang 2021-4-9 修改为只有在非大字机的时候才处理高清
			if((headType != PrinterNozzle.MESSAGE_TYPE_16_DOT) &&
				(headType != PrinterNozzle.MESSAGE_TYPE_32_DOT) &&
				(headType != PrinterNozzle.MESSAGE_TYPE_32DN) &&
				(headType != PrinterNozzle.MESSAGE_TYPE_32SN) &&
				(headType != PrinterNozzle.MESSAGE_TYPE_64SN) &&
// H.M.Wang 2022-10-19 追加64SLANT头
				(headType != PrinterNozzle.MESSAGE_TYPE_64SLANT) &&
// End of H.M.Wang 2022-10-19 追加64SLANT头
// H.M.Wang 2022-5-27 追加32x2头类型
				(headType != PrinterNozzle.MESSAGE_TYPE_32X2) &&
// End of H.M.Wang 2022-5-27 追加32x2头类型
				(headType != PrinterNozzle.MESSAGE_TYPE_64_DOT) &&
// H.M.Wang 2021-8-16 追加96DN头
				(headType != PrinterNozzle.MESSAGE_TYPE_96DN)) {
// End of H.M.Wang 2021-8-16 追加96DN头
///./...				Debug.d(TAG, "--->High Resolution");
				scaleW = scaleW / 2;
				div = div / 2;
			}
// End of H.M.Wang 2021-4-9 修改为只有在非大字机的时候才处理高清
		}
//		div = div/stat.getScale();
///./...		Debug.d(TAG, "-----scaleW = " + scaleW + " div = " + div);
		//mPreBitmap = Arrays.copyOf(mBg.mBits, mBg.mBits.length);

// H.M.Wang 2021-3-3 由于从QR.txt文件当中读取的变量信息要对群组有效，在这里会导致每个任务都会读取一行，所以需要移植DataTransferThread类处理
/*
// H.M.Wang 2021-1-4 修改QR文件的应用范围，以前只支持Barcode，改为支持DT和Barcode，格式为：<序号>,DT0,DT1,DT2,DT3,DT4,DT5,DT6,DT7,DT8,DT9,QRString
		int strIndex = -1;
		String[] recvStrs = new String[1];
// H.M.Wang 2021-1-4 追加数据源FILE2，也是从QR.txt读取DT0,DT1,...,DT9,BARCODE的信息，但是DT赋值根据DT变量内部的序号匹配
//		if (!prev && SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_FILE) {
		if (!prev &&
			(SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_FILE ||
			SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_FILE2)) {
// End of H.M.Wang 2021-1-4 追加数据源FILE2，也是从QR.txt读取DT0,DT1,...,DT9,BARCODE的信息，但是DT赋值根据DT变量内部的序号匹配
			QRReader reader = QRReader.getInstance(mContext);
			String content = reader.read();

			if (TextUtils.isEmpty(content)) {
				isReady = false;
				return;
			}

			recvStrs = content.split(",");
			strIndex = 0;
		}
// End of H.M.Wang 2021-1-4 修改QR文件的应用范围，以前只支持Barcode，改为支持DT和Barcode，格式为：<序号>,DT0,DT1,DT2,DT3,DT4,DT5,DT6,DT7,DT8,DT9,QRString
*/
// End of H.M.Wang 2021-3-3 由于从QR.txt文件当中读取的变量信息要对群组有效，在这里会导致每个任务都会读取一行，所以需要移植DataTransferThread类处理

		for(BaseObject o:mObjList)
		{
///./...			Debug.d(TAG, "Name " + o.mName);
			if (o instanceof BarcodeObject) {
// H.M.Wang 2020-7-31 该判断的内容相当于非动态二维码不继续执行后续代码，但是由于存在超文本，可能内容会发生变化
//				Debug.d(TAG, "+++++++++++++>source: " + o.getSource());
//				/* 如果二維碼從QR文件中讀 */
// H.M.Wang 2021-4-4 恢复如果是静态二维码，则不再生成二维码图片
				if (!o.getSource()) {
					continue;
				}
// End of H.M.Wang 2021-4-4 恢复如果是静态二维码，则不再生成二维码图片
// End of H.M.Wang 2020-7-31 该判断的内容相当于非动态二维码不继续执行后续代码，但是由于存在超文本，可能内容会发生变化
// 2019-12-18 H.M.Wang 当数据源为外部的时候，不去读取内部的QR文件。并且修改content的设置方式，原来的方式如果不从reader读数据，则可能就是“123456789”，新的方式是如果读并且读到，则设置，否则跳过
//				String content = "123456789";
//				if (!prev) {
// H.M.Wang 2020-5-18 由于数据源参数索引使用立即数，未及时发现修改，改为固定变量索引
//				if (!prev && SystemConfigFile.getInstance().getParam(40) == SystemConfigFile.DATA_SOURCE_FILE) {

// H.M.Wang 2021-3-3 由于从QR.txt文件当中读取的变量信息要对群组有效，在这里会导致每个任务都会读取一行，所以需要移植DataTransferThread类处理
/*
// H.M.Wang 2021-1-4 追加数据源FILE2，也是从QR.txt读取DT0,DT1,...,DT9,BARCODE的信息，但是DT赋值根据DT变量内部的序号匹配
//				if (!prev && SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_FILE) {
				if (!prev &&
					(SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_FILE ||
					SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_FILE2)) {
// End of H.M.Wang 2021-1-4 追加数据源FILE2，也是从QR.txt读取DT0,DT1,...,DT9,BARCODE的信息，但是DT赋值根据DT变量内部的序号匹配
// End of H.M.Wang 2020-5-18 由于数据源参数索引使用立即数，未及时发现修改，改为固定变量索引
// H.M.Wang 2021-1-4 修改QR文件的应用范围，以前只支持QRCode，改为支持DT和Barcode，格式为：<序号>,DT0,DT1,DT2,DT3,DT4,DT5,DT6,DT7,DT8,DT9,QRString
//					QRReader reader = QRReader.getInstance(mContext);
//					String content = reader.read();

//					if (TextUtils.isEmpty(content)) {
//						isReady = false;
//						continue;
//					}
//					o.setContent(content);
// End of H.M.Wang 2021-1-4 修改QR文件的应用范围，以前只支持QRCode，改为支持DT和Barcode，格式为：<序号>,DT0,DT1,DT2,DT3,DT4,DT5,DT6,DT7,DT8,DT9,QRString
					if (recvStrs.length >= 11) {
						Debug.d(TAG, "--->set content to BarcodeObject = " + recvStrs[10]);
						o.setContent(recvStrs[10]);
					}
				}
// End.
*/
// End of H.M.Wang 2021-3-3 由于从QR.txt文件当中读取的变量信息要对群组有效，在这里会导致每个任务都会读取一行，所以需要移植DataTransferThread类处理
				// Bitmap bmp = o.getScaledBitmap(mContext);
				Debug.d(TAG,"--->cover barcode w = " + o.getWidth() + "  h = " + o.getHeight() + " total=" + (mBinInfo.getBytesFeed()*8) + " " + (o.getWidth()/scaleW) + " " + (o.getHeight()/scaleH));
// H.M.Wang 2021-2-20 o.getY()坐标直接传递改为除以scaleH后传递，因为这个是生成打印缓冲区，需要考虑scale
				Bitmap bmp = ((BarcodeObject)o).getPrintBitmap((int)(o.getWidth()/scaleW), mBinInfo.getBytesFeed()*8, (int)(o.getWidth()/scaleW), (int)(o.getHeight()/scaleH), (int)(o.getY()/scaleH));
// End of H.M.Wang 2021-2-20 o.getY()坐标直接传递改为除以scaleH后传递，因为这个是生成打印缓冲区，需要考虑scale
				// BinCreater.saveBitmap(bmp, "bar.png");
				BinInfo info = new BinInfo(mContext, bmp, mTask.getHeads(), mExtendStat);

// 2023-5-19 因为PC保存的时候已经不在保存动态二维码的假图，因此此修改已无意义，取消
// 2020-12-12 二维码每次打印都会重新生成，由于PC和Android生成的不一样，而且每次生成的由于内容可能变化也可能不一样，如果用或的方式试着可能会重叠，改为覆盖
				BinInfo.overlap(mPrintBuffer, info.getBgBuffer(), (int)(o.getX()/div), info.getCharsFeed() * stat.getScale());
//				BinInfo.cover(mPrintBuffer, info.getBgBuffer(), (int)(o.getX()/div), info.getCharsFeed() * stat.getScale());
// End of 2020-12-12 二维码每次打印都会重新生成，由于PC和Android生成的不一样，而且每次生成的由于内容可能变化也可能不一样，如果用或的方式试着可能会重叠，改为覆盖
// End of 2023-5-19 因为PC保存的时候已经不在保存动态二维码的假图，因此此修改已无意义，取消
				continue;
// H.M.Wang 2020-5-22 串口数据启用DynamicText，取消代用CounterObject
            } else if(o instanceof DynamicText) {
				Debug.d(TAG, "--->object index=" + o.getIndex() + "; headType = " + headType);

// H.M.Wang 2021-3-3 由于从QR.txt文件当中读取的变量信息要对群组有效，在这里会导致每个任务都会读取一行，所以需要移植DataTransferThread类处理
/*
// H.M.Wang 2021-1-4 修改QR文件的应用范围，以前只支持QRCode，改为支持DT和Barcode，格式为：<序号>,DT0,DT1,DT2,DT3,DT4,DT5,DT6,DT7,DT8,DT9,QRString
				if (!prev && SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_FILE) {
					Debug.d(TAG, "--->set content to DynamicText = " + recvStrs[strIndex]);
					o.setContent(recvStrs[strIndex++]);
				}
// H.M.Wang 2021-1-4 追加数据源FILE2，也是从QR.txt读取DT0,DT1,...,DT9,BARCODE的信息，但是DT赋值根据DT变量内部的序号匹配
				if (!prev && SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_FILE2) {
					int dtIndex = ((DynamicText)o).getDtIndex();
					if(dtIndex >= 0 && dtIndex < recvStrs.length) {
						Debug.d(TAG, "--->set content to DynamicText = " + recvStrs[dtIndex]);
						o.setContent(recvStrs[dtIndex]);
					} else {
						o.setContent("");
					}
				}
// End of H.M.Wang 2021-1-4 追加数据源FILE2，也是从QR.txt读取DT0,DT1,...,DT9,BARCODE的信息，但是DT赋值根据DT变量内部的序号匹配
// End of H.M.Wang 2021-1-4 修改QR文件的应用范围，以前只支持QRCode，改为支持DT和Barcode，格式为：<序号>,DT0,DT1,DT2,DT3,DT4,DT5,DT6,DT7,DT8,DT9,QRString
*/
// End of H.M.Wang 2021-3-3 由于从QR.txt文件当中读取的变量信息要对群组有效，在这里会导致每个任务都会读取一行，所以需要移植DataTransferThread类处理

// H.M.Wang 2020-10-29 修改DynamicText实时生成打印缓冲区，而不是使用Vbin贴图
// H.M.Wang 2022-4-1 根据12.7的头数，调整倍率，原来的算法中没有调整，如果不调整，使用事先生成的vbin没有问题，动态生成则会生成变小的图案
				float wx = 1.0f, hx=1.0f;
				if (headType == PrinterNozzle.MESSAGE_TYPE_25_4) {
					wx = 2.0f;
					hx = 2.0f;
				} else if (headType == PrinterNozzle.MESSAGE_TYPE_38_1) {
					wx = 3.0f;
					hx = 3.0f;
				} else if (headType == PrinterNozzle.MESSAGE_TYPE_50_8) {
					wx = 4.0f;
					hx = 4.0f;
				}

                Bitmap bmp = ((DynamicText)o).getPrintBitmap(scaleW/wx, scaleH/hx, headType.getHeight());
// End of H.M.Wang 2022-4-1 根据12.7的头数，调整倍率，原来的算法中没有调整，如果不调整，使用事先生成的vbin没有问题，动态生成则会生成变小的图案
//				Debug.d(TAG, "Bitmat: Width=" + bmp.getWidth() + "; Height=" + bmp.getHeight());
                BinInfo info = new BinInfo(mContext, bmp, mTask.getHeads(), mExtendStat);
//				Debug.d(TAG, "Overlap: x=" + (int)(o.getX()/div) + "; Height=" + info.getCharsFeed() * stat.getScale());
                BinInfo.overlap(mPrintBuffer, info.getBgBuffer(), (int)(o.getX()/div), info.getCharsFeed() * stat.getScale());

/*
				BinInfo info = null;
				if(SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_LAN ||
					SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_RS232_1 ||
					SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_RS232_2 ||
					SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_RS232_3 ||
					SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_SCANER1 ||
// H.M.Wang 2020-6-9 追加串口6协议
					SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_RS232_6 ||
// End of H.M.Wang 2020-6-9 追加串口6协议
// H.M.Wang 2020-7-17 追加串口7协议
					SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_RS232_7 ||
// End of H.M.Wang 2020-7-17 追加串口7协议
// H.M.Wang 2020-6-29 追加网络快速打印数据源
					SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_FAST_LAN ) {
// H.M.Wang 2020-6-29 追加网络快速打印数据源
					info = new BinInfo(ConfigPath.getVBinAbsolute(mTask.getName(), o.getIndex()), mTask, 128, mExtendStat);
					var = info.getVarBuffer(o.getContent(), true, true);
				} else if(SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_RS232_4 ) {
					info = new BinInfo(ConfigPath.getVBinAbsolute(mTask.getName(), o.getIndex()), mTask, mExtendStat);
					var = info.getVarBuffer(o.getContent(), true, false);
				} else {
					info = new BinInfo(ConfigPath.getVBinAbsolute(mTask.getName(), o.getIndex()), mTask, mExtendStat);
					var = info.getVarBuffer(o.getContent(), true, false);
				}

				BinInfo.overlap(mPrintBuffer, var, (int)(o.getX()/div), info.getCharsFeed() * stat.getScale());
*/
// End of H.M.Wang 2020-10-29 修改DynamicText实时生成打印缓冲区，而不是使用Vbin贴图
            } else if(o instanceof CounterObject)
			{
// 2019-12-17 H.M.Wang 彻底取消原来的事先保存与Object对应的BinInfo的做法，因为修改为通过useSerialContent()来判断是何种打印方式的话，每次都有可能发生变化
				// H.M.Wang 2019-10-27 修改。适应从串口来的打印数据
				// End ------------------------------------------
//				BinInfo info = mVarBinList.get(o);
				BinInfo info = null;
///./...				Debug.d(TAG, "--->object index=" + o.getIndex());
//				if (info == null) {
				// H.M.Wang 2019-12-19 追加多种协议支持
				// H.M.Wang 2019-12-5 为对应串口打印时，vbin的元素个数不是传统计数器的10位，而是128位，做了区分
/*					if(SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_LAN ||
						SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_RS232_1 ||
						SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_RS232_2 ||
						SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_RS232_3 ) {
						info = new BinInfo(ConfigPath.getVBinAbsolute(mTask.getName(), o.getIndex()), mTask, 128, mExtendStat);
						var = info.getVarBuffer(((CounterObject) o).getRemoteContent(), true, true);
					} else if(SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_RS232_4 ) {
						info = new BinInfo(ConfigPath.getVBinAbsolute(mTask.getName(), o.getIndex()), mTask, mExtendStat);
						var = info.getVarBuffer(((CounterObject) o).getRemoteContent(), true, false);
					} else {*/
						info = new BinInfo(ConfigPath.getVBinAbsolute(mTask.getName(), o.getIndex()), mTask, mExtendStat);
// H.M.Wang 2020-7-2 调整计数器增量策略，在打印完成时调整	，因此生成打印缓冲区的时候，只要取内容即可
//				var = info.getVarBuffer(prev? ((CounterObject) o).getContent() : ((CounterObject) o).getNext(), true, false);
						var = info.getVarBuffer(o.getContent(), true, false);
// End of H.M.Wang 2020-7-2 调整计数器增量策略，在打印完成时调整	，因此生成打印缓冲区的时候，只要取内容即可
/*					}*/
//					info = new BinInfo(ConfigPath.getVBinAbsolute(mTask.getName(), o.getIndex()), mTask, mExtendStat);
					// End. 2019-12-5 -----------
//					mVarBinList.put(o, info);
//				}

				// H.M.Wang 2019-12-4 修改变量缓冲区获取方式，如果是串口，则按ASCII进行索引，如果是普通变量则按原来处理方式处理
				// 2019-12-17 H.M.Wang 追加对使用的数据源区分
//				if(SystemConfigFile.getInstance().getParam(40) == 1) {
//				if(((CounterObject) o).useSerialContent()) {
//					var = info.getVarBuffer(((CounterObject) o).getRemoteContent(), true, true);
//				} else {
//					var = info.getVarBuffer(prev? ((CounterObject) o).getContent() : ((CounterObject) o).getNext(), true, false);
//				}
				// End. .......................H.M.Wang 2019-12-4

//				BinCreater.saveBin("/sdcard/" + o.getIndex() + ".bin", var, info.getCharsPerHFeed()*16);

				// Debug.d(TAG, "--->object x=" + o.getX()/div);

				BinInfo.overlap(mPrintBuffer, var, (int)(o.getX()/div), info.getCharsFeed() * stat.getScale());
// End of H.M.Wang 2020-5-22 串口数据启用DynamicText，取消代用CounterObject
			}
			else if(o instanceof RealtimeObject) {

				Vector<BaseObject> rt = ((RealtimeObject) o).getSubObjs();

				for (BaseObject rtSub : rt) {
					if (rtSub instanceof RealtimeYear) {
						substr = ((RealtimeYear) rtSub).getContent();
					} else if (rtSub instanceof RealtimeMonth) {
						substr = ((RealtimeMonth) rtSub).getContent();
						//continue;
					} else if (rtSub instanceof RealtimeDate) {
						substr = ((RealtimeDate) rtSub).getContent();
						//continue;
					} else if (rtSub instanceof RealtimeHour) {
						substr = ((RealtimeHour) rtSub).getContent();
					} else if (rtSub instanceof RealtimeMinute) {
						substr = ((RealtimeMinute) rtSub).getContent();
// H.M.Wang 2020-8-6 增加SS秒时间格式
					} else if (rtSub instanceof RealtimeSecond) {
						substr = ((RealtimeSecond) rtSub).getContent();
// End of H.M.Wang 2020-8-6 增加SS秒时间格式
					} else
						continue;
					BinInfo info = mVarBinList.get(rtSub);
					if (info == null) {
						info = new BinInfo(ConfigPath.getVBinAbsolute(mTask.getName(), rtSub.getIndex()), mTask, mExtendStat);
						mVarBinList.put(rtSub, info);
					}
					var = info.getVarBuffer(substr, false, false);
					//BinCreater.saveBin("/mnt/usbhost1/v" + o.getIndex() + ".bin", var, info.mBytesPerHFeed*8);
// H.M.Wang 2020-1-2 添加 * stat.getScale()以调整1带多时的高度，info.getCharsFeed()只是取一个头的高
					BinInfo.overlap(mPrintBuffer, var, (int) (rtSub.getX() / div), info.getCharsFeed() * stat.getScale());
// End of H.M.Wang 2020-1-2 添加 * stat.getScale()以调整1带多时的高度，info.getCharsFeed()只是取一个头的高
///./...					Debug.d(TAG, "--->real x=" + rtSub.getX() / div);
//					BinCreater.saveBin("/sdcard/" + o.getIndex() + substr + ".bin", var, info.getCharsFeed() * stat.getScale() * 16);
				}
// H.M.Wang 2020-2-17 追加HyperText控件
			} else if(o instanceof HyperTextObject) {
				Vector<BaseObject> htObjs = ((HyperTextObject) o).getSubObjs();

				for (BaseObject htObj : htObjs) {
					if (htObj instanceof RealtimeYear) {
						substr = ((RealtimeYear) htObj).getContent();
					} else if (htObj instanceof RealtimeMonth) {
						substr = ((RealtimeMonth) htObj).getContent();
					} else if (htObj instanceof RealtimeDate) {
						substr = ((RealtimeDate) htObj).getContent();
					} else if (htObj instanceof RealtimeHour) {
						substr = ((RealtimeHour) htObj).getContent();
					} else if (htObj instanceof RealtimeMinute) {
						substr = ((RealtimeMinute) htObj).getContent();
					} else if (htObj instanceof RealtimeSecond) {
						substr = ((RealtimeSecond) htObj).getContent();
					} else if (htObj instanceof ShiftObject) {
// H.M.Wang 2020-2-24 超文本班次打印崩溃问题解决
//						substr = ((ShiftObject) htObj).getContent();
// End of H.M.Wang 2020-2-24 超文本班次打印崩溃问题解决
					} else if (htObj instanceof WeekDayObject) {
						substr = ((WeekDayObject) htObj).getContent();
					} else if (htObj instanceof WeekOfYearObject) {
						substr = ((WeekOfYearObject) htObj).getContent();
					} else if (htObj instanceof CounterObject) {
// H.M.Wang 2020-7-2 调整计数器增量策略，在打印完成时调整	，因此生成打印缓冲区的时候，只要取内容即可
						substr = htObj.getContent();
// End of H.M.Wang 2020-7-2 调整计数器增量策略，在打印完成时调整	，因此生成打印缓冲区的时候，只要取内容即可
					} else
						continue;
					BinInfo info = mVarBinList.get(htObj);
					if (info == null) {
						info = new BinInfo(ConfigPath.getVBinAbsolute(mTask.getName(), htObj.getIndex()), mTask, mExtendStat);
						mVarBinList.put(htObj, info);
					}
// H.M.Wang 2020-2-24 超文本班次打印崩溃问题解决
					if (htObj instanceof ShiftObject) {
						var = info.getVarBuffer(((ShiftObject)htObj).getShiftIndex(), ((ShiftObject)htObj).getBits());
					} else {
						var = info.getVarBuffer(substr, false, false);
					}
// End of H.M.Wang 2020-2-24 超文本班次打印崩溃问题解决
					//BinCreater.saveBin("/mnt/usbhost1/v" + o.getIndex() + ".bin", var, info.mBytesPerHFeed*8);
// H.M.Wang 2020-1-2 添加 * stat.getScale()以调整1带多时的高度，info.getCharsFeed()只是取一个头的高
					BinInfo.overlap(mPrintBuffer, var, (int) (htObj.getX() / div), info.getCharsFeed() * stat.getScale());
// End of H.M.Wang 2020-1-2 添加 * stat.getScale()以调整1带多时的高度，info.getCharsFeed()只是取一个头的高
					Debug.d(TAG, "--->content = " + substr + "; real x=" + htObj.getX() / div);
//					BinCreater.saveBin("/sdcard/" + o.getIndex() + substr + ".bin", var, info.getCharsFeed() * stat.getScale() * 16);
				}
// End of H.M.Wang 2020-2-17 追加HyperText控件
			}
			else if(o instanceof JulianDayObject)
			{
				String vString = ((JulianDayObject)o).getContent();
				BinInfo varbin= mVarBinList.get(o);
				if (varbin == null) {
					varbin = new BinInfo(ConfigPath.getVBinAbsolute(mTask.getName(), o.getIndex()), mTask, mExtendStat);
					mVarBinList.put(o, varbin);
				}
				Debug.d(TAG, "--->real x=" + o.getX()+ ", div-x=" + o.getX()/div );
				var = varbin.getVarBuffer(vString, false, false);
// H.M.Wang 2020-1-2 添加 * stat.getScale()以调整1带多时的高度，info.getCharsFeed()只是取一个头的高
				BinInfo.overlap(mPrintBuffer, var, (int)(o.getX()/div), varbin.getCharsFeed() * stat.getScale());
// End of H.M.Wang 2020-1-2 添加 * stat.getScale()以调整1带多时的高度，info.getCharsFeed()只是取一个头的高

			} else if (o instanceof ShiftObject) {
				/*班次變量特殊處理，生成v.bin時固定爲兩位有效位，如果shift的bit爲1，那前面補0，
				 *所以，shift變量的v.bin固定爲8位，如果bit=1，需要跳過前面的0*/
				int shift = ((ShiftObject)o).getShiftIndex();
				Debug.d(TAG, "--->shift ******: " + shift);
				BinInfo varbin= mVarBinList.get(o);
				if (varbin == null) {
					varbin = new BinInfo(ConfigPath.getVBinAbsolute(mTask.getName(), o.getIndex()), mTask, mExtendStat);
					mVarBinList.put(o, varbin);
				}
				// Debug.d(TAG, "--->real x=" + o.getX()+ ", div-x=" + o.getX()/div );
				var = varbin.getVarBuffer(shift, ((ShiftObject)o).getBits());
// H.M.Wang 2020-1-2 添加 * stat.getScale()以调整1带多时的高度，info.getCharsFeed()只是取一个头的高
				BinInfo.overlap(mPrintBuffer, var, (int)(o.getX()/div), varbin.getCharsFeed() * stat.getScale());
// End of H.M.Wang 2020-1-2 添加 * stat.getScale()以调整1带多时的高度，info.getCharsFeed()只是取一个头的高
			} else if (o instanceof LetterHourObject) {
				BinInfo varbin= mVarBinList.get(o);
				if (varbin == null) {
					varbin = new BinInfo(ConfigPath.getVBinAbsolute(mTask.getName(), o.getIndex()), mTask, 24, mExtendStat);
					mVarBinList.put(o, varbin);
				}
				String t = ((LetterHourObject) o).getContent();
				var = varbin.getVarBuffer(t, false, false);
// H.M.Wang 2020-1-2 添加 * stat.getScale()以调整1带多时的高度，info.getCharsFeed()只是取一个头的高
				BinInfo.overlap(mPrintBuffer, var, (int)(o.getX()/div), varbin.getCharsFeed() * stat.getScale());
// End of H.M.Wang 2020-1-2 添加 * stat.getScale()以调整1带多时的高度，info.getCharsFeed()只是取一个头的高
			} else if (o instanceof WeekOfYearObject) {
				BinInfo varbin= mVarBinList.get(o);
				if (varbin == null) {
					varbin = new BinInfo(ConfigPath.getVBinAbsolute(mTask.getName(), o.getIndex()), mTask, mExtendStat);
					mVarBinList.put(o, varbin);
				}
				String t = ((WeekOfYearObject) o).getContent();
				var = varbin.getVarBuffer(t, false, false);
// H.M.Wang 2020-1-2 添加 * stat.getScale()以调整1带多时的高度，info.getCharsFeed()只是取一个头的高
				BinInfo.overlap(mPrintBuffer, var, (int)(o.getX()/div), varbin.getCharsFeed() * stat.getScale());
// End of H.M.Wang 2020-1-2 添加 * stat.getScale()以调整1带多时的高度，info.getCharsFeed()只是取一个头的高
			}  else if (o instanceof WeekDayObject) {
				BinInfo varbin= mVarBinList.get(o);
				if (varbin == null) {
					varbin = new BinInfo(ConfigPath.getVBinAbsolute(mTask.getName(), o.getIndex()), mTask, mExtendStat);
					mVarBinList.put(o, varbin);
				}
				String t = ((WeekDayObject) o).getContent();
				var = varbin.getVarBuffer(t, false, false);
// H.M.Wang 2020-1-2 添加 * stat.getScale()以调整1带多时的高度，info.getCharsFeed()只是取一个头的高
				BinInfo.overlap(mPrintBuffer, var, (int)(o.getX()/div), varbin.getCharsFeed() * stat.getScale());
// End of H.M.Wang 2020-1-2 添加 * stat.getScale()以调整1带多时的高度，info.getCharsFeed()只是取一个头的高
			} else
			{
///./...				Debug.d(TAG, "not Variable object");
			}

		}
	}

	
	
	public ArrayList<BaseObject> getObjList() {
		return mObjList;
	}
	
	
	public void setDots(int dots) {
///./...		Debug.d(TAG, "--->dotcount: " + dots);
		mDots = dots;
	}
	
	public void setDotsEach(int[] dots) {
		if (dots == null) {
			return;
		}
		
		for (int i = 0; i < dots.length; i++) {
///./...			Debug.d(TAG, "--->setDotsEach: dots[" + i + "]=" + dots[i]);
			if (mDotsEach.length <= i) {
				break;
			}
			mDotsEach[i] = dots[i];
		}
	}
	
	
	public int getDots() {
		return mDots;
	}
	
	public int getDots(int index) {
		Debug.d(TAG, "--->getDots: " + mDotsEach[index]);
		if (index >= mDotsEach.length) {
			return 0;
		}
		return mDotsEach[index];
	}
	
	public boolean isNeedRefresh() {
		
		if (mObjList == null || mObjList.isEmpty()) {
			return false;
		}
		
		for(BaseObject o:mObjList)
		{
			if((o instanceof CounterObject)
					|| (o instanceof RealtimeObject)
// H.M.Wang 2020-7-31 条码支持超文本，因此需要每次更新，具体是否更新看超文本时候有变化
					|| (o instanceof BarcodeObject)
// End of H.M.Wang 2020-7-31 条码支持超文本，因此需要每次更新，具体是否更新看超文本时候有变化
// H.M.Wang 2020-2-16 追加HyperText控件
					|| (o instanceof HyperTextObject)
// End of H.M.Wang 2020-2-16 追加HyperText控件
// H.M.Wang 2020-5-22 追加DynamicText控件
					|| (o instanceof DynamicText)
// End of H.M.Wang 2020-5-22 追加DynamicText控件
					|| (o instanceof JulianDayObject)
					|| (o instanceof ShiftObject)
					|| (o instanceof LetterHourObject)
					|| (o instanceof WeekOfYearObject)
					|| (o instanceof WeekDayObject)
					|| o.getSource())
			{
				return true;
			}
		}
		return false;
	}

// H.M.Wang 2022-5-5 将MB的偏移（25.4x10头偏移）单独处理
	private void rebuildBuffer254x10() {
		mBuffer = mPrintBuffer;

		int shift0 = Configs.getMessageShift(0);
		int shift1 = Configs.getMessageShift(1);
		int dir = Configs.getMessageDir(5);
		int shiftBand = 0;
		int expandCols = 0;

		if(shift0 > 0) {				// 参数10设置时，偏移13579头
			shiftBand = 0;
			expandCols = shift0;
		} else if(shift1 > 0) {			// 参数11设置时，偏移24680头
			shiftBand = 1;
			expandCols = shift1;
		} else if(dir != SystemConfigFile.DIRECTION_NORMAL ){
		} else {
			return;
		}

		int charsPerColumn = 200;       // 每列的双字节数。400个字节，3200个点
		int charsPerBlock = 20;			// 每个头的双字节数。40个字节，320个点
		int orgCols = mBuffer.length/200;
		int newCols = orgCols + expandCols;
		char[] newBuf = new char[mBuffer.length + expandCols * charsPerColumn];		// 扩大缓冲区

		for(int i=0; i<orgCols; i++) {			// 遍历每个列
			for(int j=0; j<10; j++) {
				if(j%2 == shiftBand) {
                    if(dir == SystemConfigFile.DIRECTION_NORMAL) {
                        System.arraycopy(mBuffer, i * charsPerColumn + j * charsPerBlock, newBuf, (i + expandCols) * charsPerColumn + j * charsPerBlock,  charsPerBlock);
                    } else {
                        System.arraycopy(mBuffer, i * charsPerColumn + j * charsPerBlock, newBuf, (newCols - 1 - i - expandCols) * charsPerColumn + j * charsPerBlock,  charsPerBlock);
                    }
				} else {
                    if(dir == SystemConfigFile.DIRECTION_NORMAL) {
                        System.arraycopy(mBuffer, i * charsPerColumn + j * charsPerBlock, newBuf, i * charsPerColumn + j * charsPerBlock,  charsPerBlock);
                    } else {
                        System.arraycopy(mBuffer, i * charsPerColumn + j * charsPerBlock, newBuf, (newCols - 1 - i) * charsPerColumn + j * charsPerBlock,  charsPerBlock);
                    }
				}
			}
		}
		mBuffer = newBuf;
	}
// End of H.M.Wang 2022-5-5 将MB的偏移（25.4x10头偏移）单独处理

	/**
	 * 对buffer进行左右移动变换，生成真正的打印数据
	 */
	public void rebuildBuffer() {
		if (!isNeedRebuild()) {
			mBuffer = mPrintBuffer;
		}
		MessageObject object = mTask.getMsgObject();
//		ArrayList<SegmentBuffer> buffers = new ArrayList<SegmentBuffer>();
//		for (BaseObject msg : mTask.getObjects()) {
//			if (msg instanceof MessageObject) {
//				object = msg;
//				break;
//			}
//		}
		if (object == null) {
			return;
		}
		/*分头处理*/
		int heads = 1;
//		if (object != null) {
			heads = mTask.getHeads();
//		}
		ExtendInterceptor interceptor = new ExtendInterceptor(mContext);
		if (interceptor.getExtend() != ExtendStat.NONE) {
			heads = interceptor.getExtend().activeNozzleCount();
		}
///./...		Debug.d(TAG, "--->type=" + heads);

// H.M.Wang 2020-3-3 修改生成偏移，镜像以及倒置的算法
/*
		for (int i = 0; i < heads; i++) {
			int revert = 0x00;
			int shift = object.getPNozzle().shiftEnable ? Configs.getMessageShift(i) : 0;
			int mirror = object.getPNozzle().mirrorEnable ? Configs.getMessageDir(i) : SegmentBuffer.DIRECTION_NORMAL;
			SystemConfigFile sysconf = SystemConfigFile.getInstance(mContext);
			if (object.getPNozzle().reverseEnable) {

				if (sysconf.getParam(14) > 0) {
					revert |= 0x01;
				}
				if (sysconf.getParam(15) > 0) {
					revert |= 0x02;
				}
				if (sysconf.getParam(22) > 0) {
					revert |= 0x04;
				}
				if (sysconf.getParam(23) > 0) {
					revert |= 0x08;
				}
			}

			int rotate = sysconf.getParam(35);

//			if (object.getPNozzle().shiftEnable) {
//				buffers.add(new SegmentBuffer(mContext, mPrintBuffer, i, heads, mBinInfo.getCharsFeed(), SegmentBuffer.DIRECTION_NORMAL, 0));
//			} else {
//				buffers.add(new SegmentBuffer(mContext, mPrintBuffer, i, heads, mBinInfo.getCharsFeed(), Configs.getMessageDir(i), Configs.getMessageShift(i)));
//			}
			buffers.add(new SegmentBuffer.Builder(mContext, mPrintBuffer)
					.type(i)
					.heads(heads)
					.ch(mBinInfo.getCharsFeed())
					.direction(mirror)
					.shift(shift)
					.revert(revert)
					.rotate(rotate)
					.build());
		}

		int columns=0;
		int hight = 0;
		for (SegmentBuffer segmentBuffer : buffers) {
			columns = segmentBuffer.getColumns() > columns?segmentBuffer.getColumns():columns;
			hight = segmentBuffer.mHight * buffers.size();
		}
		Debug.d(TAG, "--->columns: " + columns + "  hight: " + hight);
		mBuffer = new char[columns * hight];
		for (int j=0; j < columns; j++) {
			for (SegmentBuffer buffer : buffers) {
				buffer.readColumn(mBuffer, j, j*hight + buffer.mHight * buffer.mType);
			}
		}
*/
		int offsetDiv = 1;

//		if(object.getPNozzle() == PrinterNozzle.MESSAGE_TYPE_16_DOT || object.getPNozzle() == PrinterNozzle.MESSAGE_TYPE_32_DOT || object.getPNozzle() == PrinterNozzle.MESSAGE_TYPE_64_DOT) {
		if( object.getPNozzle() == PrinterNozzle.MESSAGE_TYPE_16_DOT ||
			object.getPNozzle() == PrinterNozzle.MESSAGE_TYPE_32_DOT ||
// H.M.Wang 2020-7-23 追加32DN打印头
			object.getPNozzle() == PrinterNozzle.MESSAGE_TYPE_32DN ||
// End of H.M.Wang 2020-7-23 追加32DN打印头
// H.M.Wang 2020-8-17 追加32SN打印头
			object.getPNozzle() == PrinterNozzle.MESSAGE_TYPE_32SN ||
// End of H.M.Wang 2020-8-17 追加32SN打印头
// H.M.Wang 2020-8-26 追加64SN打印头
			object.getPNozzle() == PrinterNozzle.MESSAGE_TYPE_64SN ||
// End of H.M.Wang 2020-8-26 追加64SN打印头
// H.M.Wang 2022-10-19 追加64SLANT头
			object.getPNozzle() == PrinterNozzle.MESSAGE_TYPE_64SLANT ||
// End of H.M.Wang 2022-10-19 追加64SLANT头
// H.M.Wang 2022-5-27 追加32x2头类型
			object.getPNozzle() == PrinterNozzle.MESSAGE_TYPE_32X2 ||
// End of H.M.Wang 2022-5-27 追加32x2头类型
			object.getPNozzle() == PrinterNozzle.MESSAGE_TYPE_64_DOT ||
// H.M.Wang 2021-8-16 追加96DN头
			object.getPNozzle() == PrinterNozzle.MESSAGE_TYPE_96DN) {
// End of H.M.Wang 2021-8-16 追加96DN头
			heads = 4;		// 16点，32点和64点，在这里假设按4个头来算，主要是为了就和当前的实现逻辑
// H.M.Wang 2021-11-3 大字机4mm是一列，参数设置的是1/6mm的单位数，因此，如果参数10（11，18，19都一样）设置24，才能够达到位移一位的效果
//			offsetDiv = 6;	// 打字机位移量除6
			offsetDiv = 24;
// End of H.M.Wang 2021-11-3 大字机4mm是一列，参数设置的是1/6mm的单位数，因此，如果参数10（11，18，19都一样）设置24，才能够达到位移一位的效果
		}

		SystemConfigFile sysconf = SystemConfigFile.getInstance(mContext);

		int[] shifts = new int[heads];
		int[] mirrors = new int[heads];
		for (int i = 0; i < heads; i++) {
			shifts[i] = (object.getPNozzle().shiftEnable ? Configs.getMessageShift(i) : 0) / offsetDiv;
// H.M.Wang 2021-2-26 位移量加权bold参数，加重打印时位移量相应提高
//			shifts[i] *= sysconf.getParam(2) / 150;
// End of H.M.Wang 2021-2-26 位移量加权bold参数，加重打印时位移量相应提高
			mirrors[i] = object.getPNozzle().mirrorEnable ? Configs.getMessageDir(i) : SystemConfigFile.DIRECTION_NORMAL;
		}

		int revert = 0x00;
		if (object.getPNozzle().reverseEnable) {
			if (sysconf.getParam(14) > 0) {
				revert |= 0x01;
			}
			if (sysconf.getParam(15) > 0) {
				revert |= 0x02;
			}
			if (sysconf.getParam(22) > 0) {
				revert |= 0x04;
			}
			if (sysconf.getParam(23) > 0) {
				revert |= 0x08;
			}
		}
// H.M.Wang 2022-10-19 对于64SLANT头做特殊处理，由于大字机缺省是每列按着4个头（分成上下4段）进行变换，4个头的变化参数负责指定每个段的变换方法，
// 但是，64SLANT是将每列的1-32和33-64点分成两个头来看待，由两个头的设置指定动作方法，因此需要特殊处理
		if(object.getPNozzle() == PrinterNozzle.MESSAGE_TYPE_64SLANT) {
			shifts[2] = shifts[1];
			shifts[3] = shifts[1];
			shifts[1] = shifts[0];
			mirrors[2] = mirrors[1];
			mirrors[3] = mirrors[1];
			mirrors[1] = mirrors[0];
			revert = 0x00;
			if (sysconf.getParam(14) > 0) {
				revert |= 0x03;
			}
			if (sysconf.getParam(15) > 0) {
				revert |= 0x0C;
			}
		}
// End of H.M.Wang 2022-10-19 对于64SLANT头做特殊处理。。。
		BufferRebuilder br = new BufferRebuilder(mPrintBuffer, mBinInfo.getCharsFeed(), heads);
		br.mirror(mirrors)
		  .shift(shifts)
		  .reverse(revert);
		mBuffer = br.getCharBuffer();
// End of H.M.Wang 2020-3-3 修改生成偏移，镜像以及倒置的算法

		if (mTask != null && mTask.getNozzle() != null && mTask.getNozzle().buffer8Enable) {
// H.M.Wang 2022-10-19 对于64SLANT头做特殊处理。64SLANT是将每列的1-32和33-64点分成两个头来看待，
// Slant2用于控制第二喷头倾斜。（原有SLANT  用于控制第一个32 点喷头倾斜）
// “调整2”“/”ADJ2”参数，  用于调整喷头2的宽度，规则：默认值是0， 设为n, 则展宽为 32+n，
			if(object.getPNozzle() == PrinterNozzle.MESSAGE_TYPE_64SLANT) {
				expendColumn(mBuffer,
						br.getColumnNum(),
						new int[] {0, SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_ADJ2)},
						new int[] {SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_SLANT), SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_SLANT2)});
			} else {
				expendColumn(mBuffer, br.getColumnNum(), SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_SLANT));
			}
		}

// End of H.M.Wang 2021-12-29 将下列判断移到这里，保证正常打印的逻辑不变
	}
	/**
	 * 双列喷嘴用的，  以后不用了，   替换为旋转逻辑
	 * 对齐设定, 只针对32Bit × N的buffer，其他buffer不处理
	 * 1.  原来在buffer的总长度， 增加N 列。 
	 * 2.原buffer列中第X列，所有偶数bit，   0,2,4。。。 34 bit，  后移到第n+X列。
	 * 例如·， 
	 * a. 如果设为 0，  就是现在的buffer , 完全没变化。 
	 * b. 如果设为4，  则 buffer 增加 4 列，  16 B。 
     * 例如：  没4B 为一列， 
	 * 第0 列的 的偶数bit （在0-3  字节中，）会 移到 新buffer的第（0+4）= 列的偶数bit， （在16-19Ｂ）。　
	 */
	@Deprecated
	public void evenBitShift() {
		int shift = Configs.getEvenShift();
		mBuffer = new char[mPrintBuffer.length + shift  * 2];
		CharArrayReader cReader = new CharArrayReader(mPrintBuffer);
		try {
			cReader.read(mBuffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
		/**/
		char next=0;
		char cur = 0;
		int columns = mBinInfo.mColumn + shift;
		for (int i = 0; i < columns; i++) {
			for (int j = 0; j < 2; j++) {
				cur = mBuffer[(columns-1-i)*2 + j];
				int col = shift + i + 1; 
				if (col >= columns) {
					next = 0;
				} else {
					next = mBuffer[2*(columns - col) + j];
				}
				cur = (char) (cur & 0x0AAAA);
				next = (char) (next & 0x05555);
				// System.out.println("--->cur&a0a0=" + String.valueOf((int)cur) + ",  next&a0a0=" + String.valueOf((int)next));
				mBuffer[(columns-1-i)*2 + j] = (char) (cur | next);
				// System.out.println("--->buffer[" + ((columns-1-i)*2 + j) + "]=" + String.valueOf((int)mBuffer[(columns-1-i)*2 + j]));
			}
			
		}
	}

// H.M.Wang 2021-8-31 修改打印缓冲区生成逻辑：
//   （1） 每相邻列中的第0，2，3，4，5双字节追加空列；第1双字节不变
//   （2） 每列的位右移从位移4个点（列）修改为8个点
//   （3） 总宽度设为512（原宽度）+8（位右移宽度）
// H.M.Wang 2021-8-16 追加96DN头
	public char[] evenBitShiftFor96Dot() {
/* H.M.Wang 2022-7-12 取消原来96DN的构造算法，改为新的构造算法，即
1.  原来没slant，  现在也不用
2.  0-15， ，  32-47 ， 64-79， 为第一组，16-31，  48-63， 80-95， 为第二组
3.  第一组根据喷头一偏移，后移相应列数
4.  第二组根据喷头二偏移， 后移相应列数
--> 以下为原算法，全部取消
		int MAX_COLUMNS = 512;
		int COLUMNS_TO_SHIFT= 8;
//		int COLUMNS_TO_SHIFT= 4;
		int CHARS_PER_COLOMN = 6;
//		char[] buffer = new char[mPrintBuffer.length + COLUMNS_TO_SHIFT * CHARS_PER_COLOMN];	// 每行增加4个字节，共增加48个字节(24个char)
		char[] buffer = new char[(MAX_COLUMNS + COLUMNS_TO_SHIFT) * CHARS_PER_COLOMN];	// 最大打印缓冲区（520列）

		for (int i = 0; i < mBinInfo.mColumn; i++) {
			for (int j = 0; j < CHARS_PER_COLOMN; j++) {
				int tarCol = (j == 1 ? i : i*2);
// H.M.Wang 2021-8-27 96Dot的情况下将1,3,5,7位的数据后移4列，0,2,4,6位保留原位。与64Dot的相反
				if(tarCol < MAX_COLUMNS) {
					buffer[tarCol * CHARS_PER_COLOMN + j] |= (char)(mPrintBuffer[i * CHARS_PER_COLOMN + j] & 0x5555);
					buffer[(tarCol + COLUMNS_TO_SHIFT) * CHARS_PER_COLOMN + j] |= (char)(mPrintBuffer[i * CHARS_PER_COLOMN + j] & 0xaaaa);
				}
// End of H.M.Wang 2021-8-27 96Dot的情况下将0,2,4,6位的数据后移4列，1,3,5,7位保留原位。与64Dot的不同
			}
		}
End of H.M.Wang 2022-7-12 取消原来96DN的构造算法，改为新的构造算法 */
// H.M.Wang 2022-7-12 修改后的新算法
		int CHARS_PER_COLOMN = 6;

		int shift0 = Configs.getMessageShift(0);
		int shift1 = Configs.getMessageShift(1);
		char[] buffer = new char[mPrintBuffer.length + Math.max(shift0, shift1) * CHARS_PER_COLOMN];	// 在原有的基础上，增加与位移需求多的列数相当的Char数(96点，每列为6个Char)

		for (int i = 0; i < mBinInfo.mColumn; i++) {
			for (int j = 0; j < CHARS_PER_COLOMN; j++) {
				if((j % 2) == 0) {
					buffer[(i + shift0) * CHARS_PER_COLOMN + j] = mPrintBuffer[i * CHARS_PER_COLOMN + j];
				} else {
					buffer[(i + shift1) * CHARS_PER_COLOMN + j] = mPrintBuffer[i * CHARS_PER_COLOMN + j];
				}
			}
		}
// End of H.M.Wang 2022-7-12 修改后的新算法

		return buffer;
	}
// End of H.M.Wang 2021-8-16 追加96DN头
// End of H.M.Wang 2021-8-31 修改打印缓冲区生成逻辑

	/* H.M.Wang
		64DOT喷头双列的时候，每个Byte的1，3，5，7Bit向后位移4个字节
	 */
	public char[] evenBitShiftFor64Dot() {
		int COLUMNS_TO_SHIFT= 4;
		int CHARS_PER_COLOMN = 4;
		char[] buffer = new char[mPrintBuffer.length + COLUMNS_TO_SHIFT * CHARS_PER_COLOMN];	// 每行增加4个字节，共增加32个字节(16个char)

		for (int i = 0; i < mBinInfo.mColumn; i++) {
			for (int j = 0; j < CHARS_PER_COLOMN; j++) {
				buffer[i * CHARS_PER_COLOMN + j] |= (char)(mPrintBuffer[i * CHARS_PER_COLOMN + j] & 0xaaaa);
				buffer[(i + COLUMNS_TO_SHIFT) * CHARS_PER_COLOMN + j] |= (char)(mPrintBuffer[i * CHARS_PER_COLOMN + j] & 0x5555);
			}
		}
		return buffer;
	}
// H.M.Wang 2022-3-29 追加32DN打印头的双列位移打印功能。功能的要求是
// ----------
//  1.  每16 bit 插16bit 0. (每列32 bit 变为64 bit)
//    示例：
//      1（代表4bit位）1
//      1            1
//      1            1
//      1            1
//      2            0
//      2            0
//      2            0
//      2            0
// (一个32点的完整列)  2
//                   2
//                   2
//                   2
//                   0
//                   0
//                   0
//                   0
//           (变为一个64点的插了空的列）
//  2.  每列插7列0 （64bit x7 的0）
//      1                      1
//      1                      1
//      1                      1
//      1                      1
//      0                      0
//      0                      0
//      0                      0
//      0  (中间插入7列64位的0)  0
//      2                      2
//      2                      2
//      2                      2
//      2                      2
//      0                      0
//      0                      0
//      0                      0
//      0                      0
//        (就是把相邻两列的插了空的列拉开，最终结果是原数据被插空后，将空间拉大16倍）
//  3.  偶数bit 后移slant 列

/*
2022-3-30 修改
	感觉是
32中 奇数bit  ,
 放在64 bit 的1-16.
16 bit 0
偶数bit， 放在32-48.
16 bit 0

插7列空  64 bit 0.

偏移的时候， 直接把下面32 bit, 后移 slant 列
 */

    public char[] bitShiftFor32DNSlant(int slant) {
        int CHARS_PER_COLOMN = 2;
// H.M.Wang 2022-4-2 插入7列（跳8列）改为插入3列（跳4列）
        char[] buffer = new char[(mPrintBuffer.length * 4 + slant * CHARS_PER_COLOMN) * 2];
// End of H.M.Wang 2022-4-2 插入7列（跳8列）改为插入3列（跳4列）
                // 2： 代表每列16个bit内容插16个bit空白，空间扩大一倍
                // CHARS_PER_COLOMN：代表原数据每列的双字节数，32点为2，插入空白后后移slant列，因此增加需要slant * CHARS_PER_COLOMN) * 2的空间
                // 源数据由于在插入空白后，向后扩展8倍（最终结果达到源数据扩展16倍的效果），因此需要增加mPrintBuffer.length * 8 * 2的空间
        Arrays.fill(buffer, (char)0x0000);

        for (int i=mBinInfo.mColumn-1; i>-0; i--) {
			char d1 = 0x0000;
			char d2 = 0x0000;
			for (int j=CHARS_PER_COLOMN-1; j>=0; j--) {
				char odd = (char)(mPrintBuffer[i * CHARS_PER_COLOMN + j] & 0x5555);
				char even = (char)(mPrintBuffer[i * CHARS_PER_COLOMN + j] & 0xaaaa);
				for(int k=0; k<8; k++) {
					d1 *= 2;
					if(((odd << (2*k+1)) & 0x8000) == 0x8000) {
						d1++;
					}
					d2 *= 2;
					if(((even << (2*k)) & 0x8000) == 0x8000) {
						d2++;
					}
				}
			}
// H.M.Wang 2022-4-2 插入7列（跳8列）改为插入3列（跳4列）
			buffer[4 * 2 * i * CHARS_PER_COLOMN] = d1;
//			buffer[4 * 2 * i * CHARS_PER_COLOMN + 1] = 0x0000;
			buffer[4 * 2 * i * CHARS_PER_COLOMN + slant * CHARS_PER_COLOMN * 2 + 2] = d2;
//			buffer[4 * 2 * i * CHARS_PER_COLOMN + slant * CHARS_PER_COLOMN * 2 + 3] = 0x0000;
// End of H.M.Wang 2022-4-2 插入7列（跳8列）改为插入3列（跳4列）
/* 2022-3-29 修改
            for (int j=0; j<CHARS_PER_COLOMN; j++) {
                char odd = (char)(mPrintBuffer[i * CHARS_PER_COLOMN + j] & 0x5555);
                char even = (char)(mPrintBuffer[i * CHARS_PER_COLOMN + j] & 0xaaaa);

                buffer[8 * 2 * i * CHARS_PER_COLOMN + j * 2] = odd;
                buffer[8 * 2 * i * CHARS_PER_COLOMN + slant * CHARS_PER_COLOMN * 2 + j * 2] = even;
            }
*/
        }
        return buffer;
    }

// End of H.M.Wang 2022-3-29 追加32DN打印头的双列位移打印功能。功能的要求是

// H.M.Wang 2022-5-27 追加32x2头类型。每列64点，奇数点上移到上32bit，偶数点下移到下32bit。然后上32bit后移3列（修改为下32bit后移3列）
	public char[] bitShiftFor32X2() {
		int CHARS_PER_COLOMN = 4;
		int COLUMNS_TO_SHIFT = 2;
		char[] buffer = new char[mPrintBuffer.length + CHARS_PER_COLOMN * COLUMNS_TO_SHIFT];
		Arrays.fill(buffer, (char)0x0000);

		for (int i = 0; i < mBinInfo.mColumn; i++) {
			for(int j1=0; j1<CHARS_PER_COLOMN/2; j1++) {
				char d1 = 0x0000;
				char d2 = 0x0000;
				for (int j2=1; j2>=0; j2--) {
					char odd = (char)(mPrintBuffer[i * CHARS_PER_COLOMN + j1*2+j2] & 0x5555);
					char even = (char)(mPrintBuffer[i * CHARS_PER_COLOMN + j1*2+j2] & 0xaaaa);
					for(int k=0; k<8; k++) {
						d1 *= 2;
						if(((odd << (2*k+1)) & 0x8000) == 0x8000) {
							d1++;
						}
						d2 *= 2;
						if(((even << (2*k)) & 0x8000) == 0x8000) {
							d2++;
						}
					}
				}
				buffer[i * CHARS_PER_COLOMN + j1] = d1;
				buffer[(i + COLUMNS_TO_SHIFT) * CHARS_PER_COLOMN + CHARS_PER_COLOMN/2 + j1] = d2;
			}
		}
		return buffer;
	}
// End of H.M.Wang 2022-5-27 追加32x2头类型

// H.M.Wang 2020-7-23 追加32DN打印头时的移位处理
// H.M.Wang 2022-4-4 按着吕总要求修改
	public char[] bitShiftFor32DN(int slant) {
//		int COLUMNS_TO_SHIFT= 4;
		int COLUMNS_TO_SHIFT= ((slant == 0) ? 4 : slant);
// End of H.M.Wang 2022-4-4 按着吕总要求修改
		int CHARS_PER_COLOMN = 2;
		char[] buffer = new char[(mPrintBuffer.length + COLUMNS_TO_SHIFT * CHARS_PER_COLOMN) * 2];	// 每行增加4个字节，共增加16个字节(8个char),并且每列16bit后空余16bit，相当于数据区翻倍
		Arrays.fill(buffer, (char)0x0000);
		for (int i = 0; i < mBinInfo.mColumn; i++) {
			char d1 = 0x0000;
			char d2 = 0x0000;
			for (int j=CHARS_PER_COLOMN-1; j>=0; j--) {
				char odd = (char)(mPrintBuffer[i * CHARS_PER_COLOMN + j] & 0x5555);
				char even = (char)(mPrintBuffer[i * CHARS_PER_COLOMN + j] & 0xaaaa);
				for(int k=0; k<8; k++) {
					d1 *= 2;
					if(((odd << (2*k+1)) & 0x8000) == 0x8000) {
						d1++;
					}
					d2 *= 2;
					if(((even << (2*k)) & 0x8000) == 0x8000) {
						d2++;
					}
				}
			}
			buffer[2 * i * CHARS_PER_COLOMN] = d1;
			buffer[2 * i * CHARS_PER_COLOMN + 1] = 0x0000;
			buffer[(2 * (i + COLUMNS_TO_SHIFT) + 1) * CHARS_PER_COLOMN] = d2;
			buffer[(2 * (i + COLUMNS_TO_SHIFT) + 1) * CHARS_PER_COLOMN+1] = 0x0000;
		}
		return buffer;
	}
// End of H.M.Wang 2020-7-23 追加32DN打印头时的移位处理

// H.M.Wang 2020-8-17 追加32SN打印头
	public char[] bitShiftFor32SN() {
		char[] buffer = new char[mPrintBuffer.length * 2];			// 每16位原内容之后插入16位的空格
		Arrays.fill(buffer, (char)0x0000);
		for (int i=0; i<mPrintBuffer.length; i++) {
			buffer[2 * i] = mPrintBuffer[i];
			buffer[2 * i + 1] = 0x0000;
		}
		return buffer;
	}
// End of H.M.Wang 2020-8-17 追加32SN打印头

// H.M.Wang 2020-8-26 追加64SN打印头
public char[] bitShiftFor64SN() {
	char[] buffer = new char[mPrintBuffer.length * 2];			// 每32位原内容之后插入32位的空格
	Arrays.fill(buffer, (char)0x0000);
	for (int i=0; i<mPrintBuffer.length; i+=2) {
		buffer[2 * i] = mPrintBuffer[i];
		buffer[2 * i + 1] = mPrintBuffer[i + 1];
		buffer[2 * i + 2] = 0x0000;
		buffer[2 * i + 3] = 0x0000;
	}
	return buffer;
}
// End of H.M.Wang 2020-8-26 追加64SN打印头

	public BinInfo getInfo() {
		return mBinInfo;
	}
	
	/**
	 * 用於清洗的buffer
	 * @return
	 */
	public char[] preparePurgeBuffer(String bin, boolean isDZJ) {
		InputStream stream;
		try {
			stream = mContext.getAssets().open(bin);
			mBinInfo = new BinInfo(stream, 1);
			char[] buffer = mBinInfo.getBgBuffer();

// H.M.Wang 2022-1-3 使用直接的bin（purge4big.bin)，不再做扩充的操作

//            BinCreater.saveBin("/mnt/sdcard/purge1.bin", buffer, 32);
			stream.close();
// H.M.Wang 2022-4-1 如果是大字机，则恢复回原来的12倍(暂时测试4倍）；如果是惠普则保持36倍
			char[] rb = new char[buffer.length * (isDZJ ? 1 : 36)];
// H.M.Wang 2021-12-29 在扩大3倍，到36倍（原来12倍）
			for(int i = 0; i < (isDZJ ? 1 : 36); i++) {
// End of H.M.Wang 2021-12-29 在扩大3倍，到36倍（原来12倍）
// End of H.M.Wang 2022-4-1 如果是大字机，则恢复回原来的12倍(暂时测试4倍）；如果是惠普则保持36倍
				System.arraycopy(buffer, 0, rb, i * buffer.length, buffer.length -1);
			}
// H.M.Wang 2021-12-29 追加为清洗打印缓冲区生成调用slant
//			BinCreater.saveBin("/mnt/sdcard/purge2.bin", rb, 32);
// H.M.Wang 2022-4-1 取消插值
//			if(!PlatformInfo.getImgUniqueCode().startsWith("GZJ")) {    // 不是老板新屏标识
//				expendColumn(rb, mBinInfo.mColumn*36, 100);
//				rb = mBuffer;
//			}
// End of H.M.Wang 2022-4-1 取消插值
//            BinCreater.saveBin("/mnt/sdcard/purge3.bin", mBuffer, 32);
// End of H.M.Wang 2021-12-29 追加为清洗打印缓冲区生成调用slant
			return rb;

// End of H.M.Wang 2022-1-3 使用直接的bin（purge4big.bin)，不再做扩充的操作
//			return buffer;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
//	public int getHeads() {
//		return mTask.getHeads();
//	}
	
	public PrinterNozzle getPNozzle() {
		return mTask.getNozzle();
	}
	
	public int getBufferHeightFeed() {
		if (mBinInfo == null) {
			return 0;
		}
		return mBinInfo.mCharsPerHFeed;
	}
	
	public int getBufferColumns() {
		if (mBinInfo == null) {
			return 0;
		}
		return mBinInfo.mColumn;
	}
	
	public Bitmap getPreview() {
		char[] preview = getPrintBuffer(true, false);
		if (preview == null) {
			return null;
		}
		// String path = "/mnt/usbhost1/prev.bin";
		// BinCreater.saveBin(path, preview, getInfo().mBytesPerHFeed*8*getHeads());
		Debug.d(TAG, "--->column=" + mBinInfo.mColumn + ", charperh=" + mBinInfo.mCharsPerHFeed);
		return BinFromBitmap.Bin2Bitmap(preview, mBinInfo.mColumn, mBinInfo.mCharsFeed*16);
/*
// H.M.Wang 2021-7-26 追加实际打印内容预览图显示功能
		SystemConfigFile sysconf = SystemConfigFile.getInstance(mContext);
		int rows = mBinInfo.mCharsFeed * 16;
		if (sysconf.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_R6X48) {
//			return BinFromBitmap.Bin2Bitmap(preview,
//					PrinterNozzle.R6_PRINT_COPY_NUM * Configs.GetDpiVersion() * PrinterNozzle.R6X48_MAX_COL_NUM_EACH_UNIT,
//					mBinInfo.mBytesPerHFeed * 8 * mTask.getNozzle().mHeads * PrinterNozzle.R6_HEAD_NUM);
			rows = mBinInfo.mBytesPerHFeed * 8 * mTask.getNozzle().mHeads * PrinterNozzle.R6_HEAD_NUM;
		} else if (sysconf.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_R6X50) {
//			return BinFromBitmap.Bin2Bitmap(preview,
//					PrinterNozzle.R6_PRINT_COPY_NUM * Configs.GetDpiVersion() * PrinterNozzle.R6X50_MAX_COL_NUM_EACH_UNIT,
//					mBinInfo.mBytesPerHFeed * 8 * mTask.getNozzle().mHeads * PrinterNozzle.R6_HEAD_NUM);
			rows = mBinInfo.mBytesPerHFeed * 8 * mTask.getNozzle().mHeads * PrinterNozzle.R6_HEAD_NUM;
		} else if (sysconf.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E6X48) {
//			return BinFromBitmap.Bin2Bitmap(preview,
//					PrinterNozzle.E6_PRINT_COPY_NUM * Configs.GetDpiVersion() * PrinterNozzle.E6X48_MAX_COL_NUM_EACH_UNIT,
//					mBinInfo.mBytesPerHFeed * 8 * mTask.getNozzle().mHeads * PrinterNozzle.E6_HEAD_NUM);
			rows = mBinInfo.mBytesPerHFeed * 8 * mTask.getNozzle().mHeads * PrinterNozzle.E6_HEAD_NUM;
		} else if (sysconf.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E6X50) {
//			return BinFromBitmap.Bin2Bitmap(preview,
//					PrinterNozzle.E6_PRINT_COPY_NUM * Configs.GetDpiVersion() * PrinterNozzle.E6X50_MAX_COL_NUM_EACH_UNIT,
//					mBinInfo.mBytesPerHFeed * 8 * mTask.getNozzle().mHeads * PrinterNozzle.E6_HEAD_NUM);
			rows = mBinInfo.mBytesPerHFeed * 8 * mTask.getNozzle().mHeads * PrinterNozzle.E6_HEAD_NUM;
		} else if (sysconf.getParam(SystemConfigFile.INDEX_HEAD_TYPE) == PrinterNozzle.MessageType.NOZZLE_INDEX_E6X1) {
//			return BinFromBitmap.Bin2Bitmap(preview,
//					mBuffer.length / mBinInfo.mCharsPerHFeed / mTask.getNozzle().mHeads,
//					mBinInfo.mBytesPerHFeed * 8 * mTask.getNozzle().mHeads * PrinterNozzle.E6_HEAD_NUM);
			rows = mBinInfo.mBytesPerHFeed * 8 * mTask.getNozzle().mHeads * PrinterNozzle.E6_HEAD_NUM;
		} else if (sysconf.getParam(SystemConfigFile.INDEX_PRINT_TIMES) > 1 && sysconf.getParam(SystemConfigFile.INDEX_PRINT_TIMES) < 21) {
//			return BinFromBitmap.Bin2Bitmap(preview,
////					maxColNumPerUnit * (sysconf.getParam(SystemConfigFile.INDEX_PRINT_TIMES) - 1) + mBinInfo.mColumn,
//					preview.length / (mBinInfo.mBytesPerHFeed * mTask.getNozzle().mHeads / 2),
//					mBinInfo.mBytesPerHFeed * 8 * mTask.getNozzle().mHeads);
			rows = mBinInfo.mBytesPerHFeed * 8 * mTask.getNozzle().mHeads;
//		} else {
//			return BinFromBitmap.Bin2Bitmap(preview, mBinInfo.mColumn, mBinInfo.mCharsFeed * 16);
		}
		Debug.d(TAG, "--->columns = " + preview.length * 16 / rows + ", rows = " + rows);
		return BinFromBitmap.Bin2Bitmap(preview, preview.length * 16 / rows, rows);*/
// End of H.M.Wang 2021-7-26 追加实际打印内容预览图显示功能
	}

	public Bitmap getBgPreview() {
		return BinFromBitmap.Bin2Bitmap(mBgBuffer, mBinInfo.mColumn, mBinInfo.mCharsFeed*16);
	}

	/**
	 * expend along horizontal space, 1 column to 32 or any columns
	 * big dot machine
	 * extend buffer to 8 times filled with 0
	 * 只针对大字机buffer处理：
	 * 	1、slant = 100，将buffer拓宽32（@link Configs.CONST_EXPAND）倍；
	 * 		详细描述：
	 * 			1、在每两列之间插入31列空白列
	 * 			2、总列数变为 Nx32
	 *
	 * 	2、slant > 100，在拓宽的基础上将buffer进行逐列偏移
	 * 		详细描述：shift = slant - 100；如果shift == 0，不执行旋转操作
	 * 				第0行，不动
	 * 				第1行，向右偏移shift列
	 * 				第2行，向右偏移（2 * shift）列
	 * 				...
	 * 				第n行，向右偏移（n * shift）列
	 */
	public void expendColumn(char[] buffer, int columns, int slant) {
// H.M.Wang 2021-12-29 将下列判断移到正常打印流程，取消这里的判断，否则清洗时做的slant会因为mTask为null而返回空
//		if (mTask == null || mTask.getNozzle() == null || !mTask.getNozzle().buffer8Enable) {
//			return;
//		}
// End of H.M.Wang 2021-12-29 将下列判断移到正常打印流程，取消这里的判断，否则清洗时做的slant会因为mTask为null而返回空
		Debug.d(TAG, "expendColumn---> slant: " + slant);
		int extension = 0;
		int shift = 0;
		if (slant >= 100 ) {
			extension = Configs.CONST_EXPAND;
			shift = slant - 100;
		} else {
			return;
		}
		// CharArrayWriter writer = new CharArrayWriter();
		Debug.d(TAG, "--->extension: " + extension + " shift: " + shift);
		int charsPerColumn = buffer.length/columns;
		int columnH = charsPerColumn * 16;
		int afterColumns = columns * Configs.CONST_EXPAND + (shift > 0 ? shift * (columnH - 1) : 0);
		// buffer extend 8 times - a temperary buffer
		char[] buffer_8 = new char[columns * Configs.CONST_EXPAND * charsPerColumn];
		
		// the  final extension and shift buffer
		// mBuffer = new char[afterColumns * charsPerColumn];
		Debug.d(TAG, "--->charsPerColumn: " + charsPerColumn + "  columnH: " + columnH + "  afterColumns: " + afterColumns + "  buffer.len: " + buffer_8.length);
		// 8 times extension buffer
		for (int i = 0; i < buffer.length/charsPerColumn; i++) {
			for (int j = 0; j < charsPerColumn; j++) {
				buffer_8[i * Configs.CONST_EXPAND * charsPerColumn + j] = buffer[i * charsPerColumn + j];
			}
		}
		if (shift == 0) {
			mBuffer = buffer_8;
			return;
		}

		// shift operation
		char[] shiftBuffer = new char[afterColumns * charsPerColumn];
		for (int i = 0; i < columns * Configs.CONST_EXPAND; i++) {
			for (int j = 0; j < columnH; j++) {
				int rowShift = shift * j;
				int bit = j%16;
				char data = buffer_8[i * charsPerColumn + j/16];
				if ((data & (0x0001<< bit)) != 0) {
					int index = (i+rowShift)*charsPerColumn + j/16;

					shiftBuffer[index] |= (0x0001<< bit);
				}	
			}
		}
		int realColumns = afterColumns;
		for (int i = afterColumns - 1; i > 0; i--) {
			if (shiftBuffer[charsPerColumn * i] != 0 || shiftBuffer[charsPerColumn *i + 1] != 0) {
				break;
			}
			realColumns--;
		}
		if (realColumns + 8 < afterColumns) {
			realColumns += 8;
		}

		mBuffer = Arrays.copyOf(shiftBuffer, realColumns * charsPerColumn);
	}

/* H.M.Wang 2022-10-17 将SLANT函数扩充为支持两路处理，即：
	第一路: 处理上一半数据的倾斜，如64点的碰头，处理0-31个bit的数据
	第二路: 处理下一半数据的倾斜，如64点的碰头，处理32-63个bit的数据
   主要目的是为了适应64Slant类型头的打印要求，该打印头的原始要求(by 吕总)是：
   		a.	增加64Slant喷头类型。 （此类型暂时理解为两个32 点喷头，1-31点和33-64点）。
		b.	原有 喷头一  镜像/倒置/偏移，  控制1 头。    二头的控制二头。
 		c.	增加 Slant2 参数。 用于控制第二喷头倾斜。（原有SLANT  用于控制第一个32 点喷头倾斜）
		d.	增加 “调整2”“/”ADJ2”参数，  用于调整喷头2的宽度，规则：默认值是0， 设为n, 则展宽为 32+n,
*/
	public void expendColumn(char[] buffer, int columns, int[] adj, int[] slant) {
		Debug.d(TAG, "expendColumn---> adj: " + adj[0] + ", " + adj[1] + "; slant: " + slant[0] + ", " + slant[1]);
		int[] extension = new int[2];
		float[] shift = new float[2];

		if(slant[0] < 100 && slant[1] < 100) {
			return;
		}
		extension[0] = 1;
		if (slant[0] >= 100 ) {
			extension[0] = Configs.CONST_EXPAND + adj[0];
			shift[0] = slant[0] >= 10000 ? 1.0f * slant[0] / 100 - 100 : slant[0] - 100;
		}
		extension[1] = 1;
		if (slant[1] >= 100 ) {
			extension[1] = Configs.CONST_EXPAND + adj[1];
			shift[1] = slant[1] >= 10000 ? 1.0f * slant[1] / 100 - 100 : slant[1] - 100;
		}
		// CharArrayWriter writer = new CharArrayWriter();
		Debug.d(TAG, "--->extension: " + extension[0] + ", " + extension[1] + "; shift: " +  + shift[0] + ", " + shift[1]);
		int charsPerColumn = buffer.length/columns;
		int columnH = charsPerColumn * 16;
		int afterColumns = columns * Math.max(extension[0], extension[1]) + (int)Math.max((shift[0] > 0 ? (shift[0]+1) * (columnH/2 - 1) : 0), (shift[1] > 0 ? (shift[1]+1) * (columnH/2 - 1) : 0));
		char[] buffer_8 = new char[columns * Math.max(extension[0], extension[1]) * charsPerColumn];

		// the  final extension and shift buffer
		// mBuffer = new char[afterColumns * charsPerColumn];
		Debug.d(TAG, "--->charsPerColumn: " + charsPerColumn + "  columnH: " + columnH + "  afterColumns: " + afterColumns + "  buffer.len: " + buffer_8.length);
		// 8 times extension buffer
		for (int i = 0; i < columns; i++) {
			for (int j = 0; j < charsPerColumn; j++) {
				buffer_8[i * extension[j*2/charsPerColumn] * charsPerColumn + j] = buffer[i * charsPerColumn + j];
			}
		}
		if (shift[0] == 0 && shift[1] == 0) {
			mBuffer = buffer_8;
			return;
		}

		// shift operation
		char[] shiftBuffer = new char[afterColumns * charsPerColumn];
		for (int i = 0; i < columns * Math.max(extension[0], extension[1]); i++) {
			for (int j = 0; j < columnH; j++) {
				int rowShift = Math.round(shift[j*2/columnH] * ((j*2)%columnH)/2);
				int bit = j%16;
				char data = buffer_8[i * charsPerColumn + j/16];
				if ((data & (0x0001<< bit)) != 0) {
					int index = (i+rowShift)*charsPerColumn + j/16;

					shiftBuffer[index] |= (0x0001<< bit);
				}
			}
		}
		int realColumns = afterColumns;
		outerloop: for (int i = afterColumns - 1; i > 0; i--) {
			for (int j=0; j<charsPerColumn; j++) {
				if (shiftBuffer[charsPerColumn * i + j] != 0) {
					break outerloop;
				}
			}
			realColumns--;
		}
		if (realColumns + 8 < afterColumns) {
			realColumns += 8;
		}

		mBuffer = Arrays.copyOf(shiftBuffer, realColumns * charsPerColumn);
	}

}