package com.industry.printer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;

import com.industry.printer.FileFormat.SystemConfigFile;
import com.industry.printer.FileFormat.TlkFileWriter;
import com.industry.printer.PHeader.PrinterNozzle;
import com.industry.printer.Utils.ConfigPath;
import com.industry.printer.Utils.Configs;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.FileUtil;
import com.industry.printer.Utils.PlatformInfo;
import com.industry.printer.Utils.ToastUtil;
import com.industry.printer.data.BinFileMaker;
import com.industry.printer.data.BinFromBitmap;
import com.industry.printer.exception.PermissionDeniedException;
import com.industry.printer.exception.RussianCharException;
import com.industry.printer.exception.TlkNotFoundException;
import com.industry.printer.hardware.FpgaGpioOperation;
import com.industry.printer.object.BarcodeObject;
import com.industry.printer.object.BaseObject;
import com.industry.printer.object.CounterObject;
import com.industry.printer.object.DynamicText;
import com.industry.printer.object.GraphicObject;
import com.industry.printer.object.HyperTextObject;
import com.industry.printer.object.JulianDayObject;
import com.industry.printer.object.LetterHourObject;
import com.industry.printer.object.MessageObject;
import com.industry.printer.object.RealtimeObject;
import com.industry.printer.object.ShiftObject;
import com.industry.printer.object.TLKFileParser;
import com.industry.printer.object.TextObject;
import com.industry.printer.object.WeekDayObject;
import com.industry.printer.object.WeekOfYearObject;
import com.industry.printer.object.data.BitmapWriter;

/**
 * MessageTask包含多个object
 * @author zhaotongkai
 *
 */
public class MessageTask {

	private static final String TAG = MessageTask.class.getSimpleName();
	public static final String MSG_PREV_IMAGE = "/1.bmp";
	public static final String MSG_PREV_IMAGE2 = "/2.bmp";
	private Context mContext;
	private int mDots[];
	private String mName;
	private ArrayList<BaseObject> mObjects;

	private boolean isPrintable;

	// error message resource id
	private int unPrintableTips;

	public int mType;
	private int mIndex;
	
	private SaveTask mSaveTask;
	private Handler mCallback; 

	public interface SaveProgressListener {
		public void onSaved();
	};

	private SaveProgressListener mSaveProgressListener;

	public MessageTask(Context context) {
		mName="";
		mContext = context;
		mObjects = new ArrayList<BaseObject>();
		isPrintable = true;
	}
	
	/**
	 * 通过tlk文件解析构造出Task
	 * @param tlk  tlk path, absolute path
	 * @param name
	 */
	public MessageTask(Context context, String tlk, String name) {
		this(context);
		if (tlk == null || TextUtils.isEmpty(tlk)) {
			return;
		}
//		String tlkPath="";
//		File file = new File(tlk);
		setName(name);
		TLKFileParser parser = new TLKFileParser(context, mName);
		parser.setTlk(tlk);
		try {
			parser.parse(context, this, mObjects);
		} catch (PermissionDeniedException e) {
			ToastUtil.show(mContext, R.string.str_no_permission);
			isPrintable = false;
			Debug.d(TAG, "PermissionDeniedException");
		} catch (TlkNotFoundException ex) {
			ToastUtil.show(mContext, R.string.str_tlk_not_found);
			isPrintable = false;
			Debug.d(TAG, "TlkNotFoundException");
// H.M.Wang 2023-10-30 禁止含有俄文字符的内容
		} catch (RussianCharException ex) {
			ToastUtil.show(mContext, ex.getMessage());
			isPrintable = false;
			Debug.d(TAG, ex.getMessage());
// End of H.M.Wang 2023-10-30 禁止含有俄文字符的内容
		}
		mDots = parser.getDots();
		if (!checkBin()) {
			isPrintable = false;
			Debug.d(TAG, "!checkBin()");
			unPrintableTips = R.string.str_toast_no_bin;
		}

		if (!checkVBin()) {
			isPrintable = false;
			Debug.d(TAG, "!checkVBin()");
			unPrintableTips = R.string.str_toast_no_bin;
		}
	}
	
	/**
	 * 通过tlk名稱解析构造出Task
	 * @param context
	 * @param tlk
	 */
	public MessageTask(Context context, String tlk) {
		this(context);
		Debug.d(TAG, "--->tlk: " + tlk);
		String path = ""; 
		if (tlk.startsWith(File.separator)) {
			File file = new File(tlk);
			setName(file.getName());
			path = file.getParent();
		} else {
			setName(tlk);
		}
		// check 1.bin

		TLKFileParser parser = new TLKFileParser(context, mName);
		try {
			parser.parse(context, this, mObjects);
			if (tlk.startsWith(File.separator)) {
				parser.setTlk(path);
			}
		} catch (PermissionDeniedException e) {
			isPrintable = false;
			Debug.d(TAG, "PermissionDeniedException");
			((MainActivity)context).runOnUiThread(new  Runnable() {
				@Override
				public void run() {
					ToastUtil.show(mContext, R.string.str_no_permission);
				}
			});
			
		} catch (TlkNotFoundException ex) {
			((MainActivity)context).runOnUiThread(new  Runnable() {
				@Override
				public void run() {
					ToastUtil.show(mContext, R.string.str_tlk_not_found);
				}
			});

			isPrintable = false;
			Debug.d(TAG, "TlkNotFoundException");
// H.M.Wang 2023-10-30 禁止含有俄文字符的内容
		} catch (final RussianCharException ex) {
			((MainActivity)context).runOnUiThread(new  Runnable() {
				@Override
				public void run() {
					ToastUtil.show(mContext, ex.getMessage());
				}
			});
			isPrintable = false;
			Debug.d(TAG, ex.getMessage());
// End of H.M.Wang 2023-10-30 禁止含有俄文字符的内容
		}

		mDots = parser.getDots();
		if (!checkBin()) {
			isPrintable = false;
			Debug.d(TAG, "!checkBin()");
			unPrintableTips = R.string.str_toast_no_bin;
		}

		if (!checkVBin()) {
			isPrintable = false;
			Debug.d(TAG, "!checkVBin()");
			unPrintableTips = R.string.str_toast_no_bin;
		}
	}
	
	/**
	 * 通过字符串解析构造出Task，
	 * @param tlk tlk信息名称
	 * @param content 树莓3上编辑的字符串内容
	 */
//	public MessageTask(Context context, String tlk, String content) {
//		this(context);
//		String tlkPath="";
//		setName(tlk);
//		
//		mObjects = ObjectsFromString.makeObjs(mContext, content);
//	}
	/**
	 * set Task name
	 * @param name
	 */
	public void setName(String name) {
		mName = name;
	}
	
	/**
	 * get Task name
	 * @return
	 */
	public String getName() {
		return mName;
	}
	
	public ArrayList<BaseObject> getObjects() {
		Debug.d(TAG, "--->size:" + mObjects.size());
		return mObjects;
	}
	/**
	 * 添加object到Task
	 * @param object
	 */
	public void addObject(BaseObject object) {
		if (mObjects == null) {
			mObjects = new ArrayList<BaseObject>();
		}
		object.setTask(this);
		mObjects.add(object);
		Debug.d(TAG, "--->size:" + mObjects.size());
	}
	
	/**
	 * 从Task中删除指定object
	 * @param object
	 */
	public void removeObject(BaseObject object) {
		if (mObjects == null) {
			return;
		}
		mObjects.remove(object);
	}
	
	/**
	 * 删除Task中所有objects
	 */
	public void removeAll() {
		if (mObjects == null) {
			return;
		}
		mObjects.clear();
	}
	
	/**
	 * 获取当前打印信息的墨点数
	 * 此API只对树莓小板系统有效
	 * @return
	 */
	public int[] getDots() {
		return mDots;
	}
	
	/**
	 * 获取打印对象的内容缩略信息
	 * 暂时只支持文本对象，后续会添加对变量的支持
	 */
	public String getAbstract() {
		
		String objString="";
		String content="";
		if (mObjects == null) {
			return null;
		}
		for (BaseObject item : mObjects) {
			if (item instanceof TextObject) {
        		objString = item.getContent();
        	} else if (item instanceof RealtimeObject) {
				objString = item.getContent();
// H.M.Wang 2020-2-17 追加HyperText控件
			} else if(item instanceof HyperTextObject) {
				objString = item.getContent();
// End of H.M.Wang 2020-2-17 追加HyperText控件
			} else if (item instanceof CounterObject) {
				objString = item.getContent();
			} else {
				continue;
			}
        	content += objString;
		}
		return content;
	}
	
	public void saveTlk(Context context) {
		for(BaseObject o:mObjects)
		{
			if((o instanceof MessageObject)	) {

				// H.M.Wang 修改下列两行
//				if (getNozzle() == PrinterNozzle.MESSAGE_TYPE_16_DOT || getNozzle() == PrinterNozzle.MESSAGE_TYPE_32_DOT) {
//				if (getNozzle() == PrinterNozzle.MESSAGE_TYPE_16_DOT || getNozzle() == PrinterNozzle.MESSAGE_TYPE_32_DOT || getNozzle() == PrinterNozzle.MESSAGE_TYPE_64_DOT) {
				if (getNozzle() == PrinterNozzle.MESSAGE_TYPE_16_DOT ||
					getNozzle() == PrinterNozzle.MESSAGE_TYPE_32_DOT ||
// H.M.Wang 2020-7-23 追加32DN打印头
					getNozzle() == PrinterNozzle.MESSAGE_TYPE_32DN ||
// End of H.M.Wang 2020-7-23 追加32DN打印头
// H.M.Wang 2020-8-18 追加32SN打印头
					getNozzle() == PrinterNozzle.MESSAGE_TYPE_32SN ||
// End of H.M.Wang 2020-8-18 追加32SN打印头
// H.M.Wang 2020-8-26 追加64SN打印头
					getNozzle() == PrinterNozzle.MESSAGE_TYPE_64SN ||
// End of H.M.Wang 2020-8-26 追加64SN打印头
// H.M.Wang 2022-10-19 追加64SLANT头
					getNozzle() == PrinterNozzle.MESSAGE_TYPE_64SLANT ||
// End of H.M.Wang 2022-10-19 追加64SLANT头
// H.M.Wang 2024-4-29 追加64_DOT_ONE喷头类型
					getNozzle() == PrinterNozzle.MESSAGE_TYPE_64DOTONE ||
// End of H.M.Wang 2024-4-29 追加64_DOT_ONE喷头类型
// H.M.Wang 2024-9-10 增加一个16DOTX4头类型，
					getNozzle() == PrinterNozzle.MESSAGE_TYPE_16DOTX4 ||
// End of H.M.Wang 2024-9-10 增加一个16DOTX4头类型，
// H.M.Wang 2022-5-27 追加32x2头类型
					getNozzle() == PrinterNozzle.MESSAGE_TYPE_32X2 ||
// End of H.M.Wang 2022-5-27 追加32x2头类型
					getNozzle() == PrinterNozzle.MESSAGE_TYPE_64_DOT ||
// H.M.Wang 2023-7-29 追加48点头
					getNozzle() == PrinterNozzle.MESSAGE_TYPE_48_DOT ||
// End of H.M.Wang 2023-7-29 追加48点头
// H.M.Wang 2021-8-16 追加96DN头
					getNozzle() == PrinterNozzle.MESSAGE_TYPE_96DN) {
// End of H.M.Wang 2021-8-16 追加96DN头

					for (int i = 0; i < mDots.length; i++) {
						mDots[i] = mDots[i] * 200;
					}
				}
				Debug.d(TAG, "mDots[0] = " + mDots[0] + "; [1] = " + mDots[1] + "; [2] = " + mDots[2]);
				((MessageObject) o).setDotCountPer(mDots);
				((MessageObject) o).setDotCount(dotTotal());
				break;
			}
		}
		TlkFileWriter tlkFile = new TlkFileWriter(context, this);
		tlkFile.write();
	}
	
	public boolean createTaskFolderIfNeed() {
		File dir = new File(ConfigPath.getTlkDir(mName));
		if (dir.exists()) {
			//FileUtil.deleteFolder(dir.getAbsolutePath());
			return false;
		}
		if(!dir.mkdirs())
		{
			Debug.d(TAG, "create dir error "+dir.getPath());
			return false;
		}
		return true;
	}
	
	public void saveVarBin() {
		if (mObjects == null || mObjects.size() <= 0) {
			return;
		}
		MessageObject msgObj = getMsgObject();
		float scaleW = msgObj.getPNozzle().getScaleW();
		float scaleH = msgObj.getPNozzle().getScaleH();
		int h = msgObj.mPNozzle.getHeight();
		
		for (BaseObject object : mObjects) {
			if((object instanceof CounterObject) ||
				(object instanceof RealtimeObject) ||
				(object instanceof JulianDayObject) ||
				(object instanceof ShiftObject) ||
// H.M.Wang 2020-2-16 追加HyperText控件
				(object instanceof HyperTextObject) ||
// End of H.M.Wang 2020-2-16 追加HyperText控件
// H.M.Wang 2020-6-10 追加DynamicText控件
// H.M.Wang 2021-5-25 取消DynamicText控件生成vbin
//				(object instanceof DynamicText) ||
// End of H.M.Wang 2021-5-25 取消DynamicText控件生成vbin
// End of H.M.Wang 2020-6-10 追加DynamicText控件
				(object instanceof WeekOfYearObject) ||
				(object instanceof WeekDayObject))
			{
				if(PlatformInfo.isBufferFromDotMatrix()==1) {
					object.generateVarbinFromMatrix(ConfigPath.getTlkDir(mName));
				} else {
					// mDots += object.drawVarBitmap();
					int dot = object.makeVarBin(mContext, scaleW, scaleH, h);
					dealDot(dot);
				}
			} else if ((object instanceof LetterHourObject)) {
				int dot = object.makeVarBin(mContext, scaleW, scaleH, h);
				dealDot(dot);
			} else if (object instanceof BarcodeObject && object.getSource() == true) {
				int dots[] = ((BarcodeObject) object).getDotcount();
				MessageObject msg = getMsgObject();
				if (msg.getPNozzle() == PrinterNozzle.MESSAGE_TYPE_1_INCH ) {
					dealDot(dots, 2);
				} else if (msg.getPNozzle() == PrinterNozzle.MESSAGE_TYPE_1_INCH_DUAL) {
					dealDot(dots, 2 * 4);

				// H.M.Wang 追加下列4行
				} else if (msg.getPNozzle() == PrinterNozzle.MESSAGE_TYPE_1_INCH_TRIPLE) {
					dealDot(dots, 2 * 6);
				} else if (msg.getPNozzle() == PrinterNozzle.MESSAGE_TYPE_1_INCH_FOUR) {
					dealDot(dots, 2 * 8);

				} else {
					dealDot(dots, 0.5f);
				}
			}
		}
	}
	
	private int dotTotal() {
		int dots = 0;
		for (int i = 0; i < mDots.length; i++) {
			dots += mDots[i];
		}
		return dots;
	}

	private void dealDot(int[] dots, float multiple) {
		for (int i = 0; i < dots.length; i++) {
			mDots[i] += dots[i] * multiple;
		}	
	}
	
	private void dealDot(int dots) {
		MessageObject msgObj = getMsgObject();
		switch (msgObj.getPNozzle().mHeads) {
			case 1:
				mDots[0] += dots;
				break;
			case 2:
				mDots[0] += dots/2;
				mDots[1] += dots/2;
				break;
			case 3:
				mDots[0] += dots/3;
				mDots[1] += dots/3;
				mDots[2] += dots/3;
				break;
			case 4:
				for (int i = 0; i < 4; i++) {
					mDots[i] += dots/4;
				}
				break;
			case 8:
				for (int i = 0; i < 8; i++) {
					mDots[i] += dots/8;
				}
				break;		
			default:
				break;
		}
	}
	public void saveBin() {
		if (PlatformInfo.isBufferFromDotMatrix()==1) {
			saveBinDotMatrix();
		} else {
			// saveObjectBin();
			saveBinNoScale();
		}
		
	}
	
	/**
	 * 使用系统字库，生成bitmap，然后通过灰度化和二值化之后提取点阵生成buffer
	 */

	private void saveObjectBin()
	{
		int width=0;
		Paint p=new Paint();
		if(mObjects==null || mObjects.size() <= 0)
			return ;
		for(BaseObject o:mObjects)
		{
			if (o instanceof MessageObject) {
				continue;
			}
			// H.M.Wang 2019-09-11 将简单取整修改为四舍五入
			width = Math.round(width > o.getXEnd() ? width : o.getXEnd());
//			width = (int)(width > o.getXEnd() ? width : o.getXEnd());
		}
		float div = (float) (2.0/(getHeads() == 0 ? 1 : getHeads()));

		Bitmap bmp = Bitmap.createBitmap(width , Configs.gDots, Configs.BITMAP_CONFIG);
		Debug.d(TAG, "drawAllBmp width="+width+", height="+Configs.gDots);
		Canvas can = new Canvas(bmp);
		can.drawColor(Color.WHITE);
		for(BaseObject o:mObjects)
		{
			if((o instanceof MessageObject)	)
				continue;

			if(o instanceof CounterObject)
			{
				// o.drawVarBitmap();
			}
			else if(o instanceof RealtimeObject)
			{
				Bitmap t = ((RealtimeObject)o).getBgBitmap(mContext, 1, 1);
				can.drawBitmap(t, o.getX(), o.getY(), p);
				BinFromBitmap.recyleBitmap(t);
// H.M.Wang 2020-2-17 追加HyperText控件
			} else if(o instanceof HyperTextObject) {
				Bitmap t = ((HyperTextObject)o).getBgBitmap(mContext, 1, 1);
				can.drawBitmap(t, o.getX(), o.getY(), p);
				BinFromBitmap.recyleBitmap(t);
// End of H.M.Wang 2020-2-17 追加HyperText控件
			} else if(o instanceof JulianDayObject) {
				// o.drawVarBitmap();
			} else if (o instanceof BarcodeObject && o.getSource()) {

			} else if(o instanceof ShiftObject)	{
				// o.drawVarBitmap();
			} else if (o instanceof LetterHourObject) {

			} else if (o instanceof BarcodeObject) {
				Bitmap t = ((BarcodeObject) o).getScaledBitmap(mContext);
				can.drawBitmap(t, o.getX(), o.getY(), p);
			} else if (o instanceof GraphicObject) {
				Bitmap t = ((GraphicObject) o).getScaledBitmap(mContext);
				if (t != null) {
					can.drawBitmap(t, o.getX(), o.getY(), p);
				}
			} else {
				Bitmap t = o.getScaledBitmap(mContext);
				can.drawBitmap(t, o.getX(), o.getY(), p);
				// BinFromBitmap.recyleBitmap(t);
			}
		//can.drawText(mContent, 0, height-30, mPaint);
		}
		/**
		 * 爲了兼容128點，152點和384點高的三種列高信息，需要計算等比例縮放比例
		 */
		float dots = 152;///SystemConfigFile.getInstance(mContext).getParam(39);
		/*對於320列高的 1 Inch打印頭，不使用參數40的設置*/
		MessageObject msg = getMsgObject();
		if (msg != null &&
			(msg.getPNozzle() == PrinterNozzle.MESSAGE_TYPE_1_INCH ||
 			 msg.getPNozzle() == PrinterNozzle.MESSAGE_TYPE_1_INCH_DUAL ||

			// H.M.Wang 追加下列2行
			 msg.getPNozzle() == PrinterNozzle.MESSAGE_TYPE_1_INCH_TRIPLE ||
			 msg.getPNozzle() == PrinterNozzle.MESSAGE_TYPE_1_INCH_FOUR)) {

			dots = 304;
		}
		Debug.d(TAG, "+++dots=" + dots);
		float prop = dots/Configs.gDots;
		Debug.d(TAG, "+++prop=" + prop);
		/** 生成bin的bitmap要进行处理，高度根据message的类型调整
		 * 注： 为了跟PC端保持一致，生成的bin文件宽度为1.tlk中坐标的四分之一，在提取点阵之前先对原始Bitmap进行X坐标缩放（为原图的1/4）
		 * 	  然后进行灰度和二值化处理；
		 */
		Debug.d(TAG, "--->div=" + div + "  h=" + bmp.getHeight() + "  prop = " + prop);
		// H.M.Wang 2019-09-11 将简单取整修改为四舍五入
		Bitmap bitmap = Bitmap.createScaledBitmap(bmp, Math.round(bmp.getWidth()/div * prop), Math.round(bmp.getHeight() * getHeads() * prop), true);
//		Bitmap bitmap = Bitmap.createScaledBitmap(bmp, (int) (bmp.getWidth()/div * prop), (int) (bmp.getHeight() * getHeads() * prop), true);
		/*對於320列高的 1 Inch打印頭，不使用參數40的設置*/
		if (msg != null && (msg.getPNozzle() == PrinterNozzle.MESSAGE_TYPE_1_INCH)) {
			Bitmap b = Bitmap.createBitmap(bitmap.getWidth(), 320, Configs.BITMAP_CONFIG);
			can.setBitmap(b);
			can.drawColor(Color.WHITE);
			can.drawBitmap(bitmap, 0, 0, p);
			bitmap.recycle();
			bitmap = b;
		} else if (msg != null && (msg.getPNozzle() == PrinterNozzle.MESSAGE_TYPE_1_INCH_DUAL)) {
			Bitmap b = Bitmap.createBitmap(bitmap.getWidth(), 640, Configs.BITMAP_CONFIG);
			// can.setBitmap(b);
			Canvas c = new Canvas(b);
			c.drawColor(Color.WHITE);
			int h = bitmap.getHeight()/2;
			int dstH = b.getHeight()/2;
			c.drawBitmap(bitmap, new Rect(0, 0, bitmap.getWidth(), h), new Rect(0, 0, b.getWidth(), 308), p);
			c.drawBitmap(bitmap, new Rect(0, h, bitmap.getWidth(), h*2), new Rect(0, 320, b.getWidth(), 320 + 308), p);
			// c.drawBitmap(Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight()/2), new Matrix(), null);
			// c.drawBitmap(Bitmap.createBitmap(bitmap, 0, bitmap.getHeight()/2, bitmap.getWidth(), bitmap.getHeight()/2), 0, 320, null);
			bitmap.recycle();
			bitmap = b;

		// H.M.Wang 追加下列11行
		} else if (msg != null && (msg.getPNozzle() == PrinterNozzle.MESSAGE_TYPE_1_INCH_TRIPLE)) {
			Bitmap b = Bitmap.createBitmap(bitmap.getWidth(), 960, Configs.BITMAP_CONFIG);
			// can.setBitmap(b);
			Canvas c = new Canvas(b);
			c.drawColor(Color.WHITE);
			int h = bitmap.getHeight()/3;
			c.drawBitmap(bitmap, new Rect(0, 0, bitmap.getWidth(), h), new Rect(0, 0, b.getWidth(), 308), p);
			c.drawBitmap(bitmap, new Rect(0, h, bitmap.getWidth(), h*2), new Rect(0, 320, b.getWidth(), 320 + 308), p);
			c.drawBitmap(bitmap, new Rect(0, h*2, bitmap.getWidth(), h*3), new Rect(0, 640, b.getWidth(), 640 + 308), p);
			bitmap.recycle();
			bitmap = b;

			// H.M.Wang 追加下列12行
		} else if (msg != null && (msg.getPNozzle() == PrinterNozzle.MESSAGE_TYPE_1_INCH_FOUR)) {
			Bitmap b = Bitmap.createBitmap(bitmap.getWidth(), 1280, Configs.BITMAP_CONFIG);
			// can.setBitmap(b);
			Canvas c = new Canvas(b);
			c.drawColor(Color.WHITE);
			int h = bitmap.getHeight()/4;
			c.drawBitmap(bitmap, new Rect(0, 0, bitmap.getWidth(), h), new Rect(0, 0, b.getWidth(), 308), p);
			c.drawBitmap(bitmap, new Rect(0, h, bitmap.getWidth(), h*2), new Rect(0, 320, b.getWidth(), 320 + 308), p);
			c.drawBitmap(bitmap, new Rect(0, h*2, bitmap.getWidth(), h*3), new Rect(0, 640, b.getWidth(), 640 + 308), p);
			c.drawBitmap(bitmap, new Rect(0, h*3, bitmap.getWidth(), h*4), new Rect(0, 960, b.getWidth(), 960 + 308), p);
			bitmap.recycle();
			bitmap = b;
		}
		// 生成bin文件
		BinFileMaker maker = new BinFileMaker(mContext);

		// H.M.Wang 追加一个是否移位的参数
		mDots = maker.extract(bitmap, msg.getPNozzle().mHeads,
					( null != msg &&
					(msg.getPNozzle() == PrinterNozzle.MESSAGE_TYPE_1_INCH ||
					 msg.getPNozzle() == PrinterNozzle.MESSAGE_TYPE_1_INCH_DUAL ||
					 msg.getPNozzle() == PrinterNozzle.MESSAGE_TYPE_1_INCH_TRIPLE ||
					 msg.getPNozzle() == PrinterNozzle.MESSAGE_TYPE_1_INCH_FOUR)));
		// 保存bin文件
		maker.save(ConfigPath.getBinAbsolute(mName));

		return ;
	}

	private void saveBinNoScale() {
		int width=0;
		Paint p=new Paint();
		if(mObjects==null || mObjects.size() <= 1)
			return ;
		for(BaseObject o:mObjects)
		{
			if (o instanceof MessageObject) {
				continue;
			}
			// H.M.Wang 2019-09-11 将简单取整修改为四舍五入
			width = Math.round(width > o.getXEnd() ? width : o.getXEnd());
//			width = (int)(width > o.getXEnd() ? width : o.getXEnd());
		}
		/*計算得到的width爲152點陣對應的寬度，要根據噴頭類型轉換成實際寬度 */
		MessageObject msgObj = getMsgObject();
		float scaleW = getNozzle().getScaleW();
		float scaleH = getNozzle().getScaleH();
		Debug.d(TAG, "drawAllBmp scaleW = " + scaleW + ", scaleH = " + scaleH);
		//實際寬度

		// H.M.Wang 修改下列一行
// H.M.Wang 2020-8-11 64点内容有时保存时最后却一列问题解决，如果算出来的宽度是12.3，原来直接取整和四舍五入都不能解决问题，只有取天花板才正确
        int bWidth = (int)(width * scaleW) + 1;
//        int bWidth = Math.round(width * scaleW);
//        int bWidth = (int) (width * scaleW);
// End of H.M.Wang 2020-8-11 64点内容有时保存时最后却一列问题解决，如果算出来的宽度是12.3，原来直接取整和四舍五入都不能解决问题，只有取天花板才正确
		int bHeight = msgObj.getPNozzle().getHeight();
//		Debug.d(TAG, "SaveTime: - Start CreateBitmap : " + System.currentTimeMillis());
		Bitmap bmp = Bitmap.createBitmap(bWidth , bHeight, Configs.BITMAP_CONFIG);
		Debug.d(TAG, "drawAllBmp width=" + bWidth + ", height=" + bHeight);
		Canvas can = new Canvas(bmp);
		can.drawColor(Color.WHITE);
		for(BaseObject o:mObjects)
		{
//			Debug.d(TAG, "SaveTime: - Start System.gc()" + System.currentTimeMillis());
			// H.M.Wang 增加1行
//			System.gc();
//			Debug.d(TAG, "SaveTime: - Start DrawObject(" + o.mName + ") : " + System.currentTimeMillis());
			if((o instanceof MessageObject)	)
				continue;
			
			if((o instanceof CounterObject)
// H.M.Wang 2020-6-10 追加DynamicText控件
				|| (o instanceof DynamicText)
// End of H.M.Wang 2020-6-10 追加DynamicText控件
				|| (o instanceof JulianDayObject)
				|| (o instanceof BarcodeObject && o.getSource())
				|| (o instanceof ShiftObject)
				|| (o instanceof LetterHourObject)
				|| (o instanceof WeekOfYearObject)
				|| (o instanceof WeekDayObject))
			{
				// o.drawVarBitmap();
			}
			else if(o instanceof RealtimeObject) {
				Bitmap t = ((RealtimeObject) o).getBgBitmap(mContext, scaleW, scaleH);

				// H.M.Wang 修改1行
				can.drawBitmap(t, Math.round((o.getX() * scaleW)), Math.round(o.getY() * scaleH), p);
//				can.drawBitmap(t, (int)(o.getX() * scaleW), o.getY() * scaleH, p);
//				Debug.d(TAG, "drawBitmap: x=" + Math.round((o.getX() * scaleW)) + "; y=" + Math.round(o.getY() * scaleH) + "; w= " + t.getWidth() + "; h= " + t.getHeight());

				// H.M.Wang 增加1行。注释掉1行
				t.recycle();
//				BinFromBitmap.recyleBitmap(t);
// H.M.Wang 2020-2-16 追加HyperText控件
			} else if(o instanceof HyperTextObject) {
				Debug.d(TAG, "HypertextObject.content = " + o.getContent());
				Bitmap t = ((HyperTextObject) o).getBgBitmap(mContext, scaleW, scaleH);
				Debug.d(TAG, "Draw HypertextObject.fixed.bitmap[" + t.getWidth() + ", " + t.getHeight() + "] at [" + (o.getX() * scaleW) + "," + Math.round(o.getY() * scaleH) + "]");
				can.drawBitmap(t, Math.round((o.getX() * scaleW)), Math.round(o.getY() * scaleH), p);
				t.recycle();
// End of H.M.Wang 2020-2-16 追加HyperText控件
			} else if (o instanceof BarcodeObject) {
				// Bitmap t = ((BarcodeObject) o).getScaledBitmap(mContext);
				Debug.d(TAG, "--->save height=" + o.getHeight() + " scaleH = " + scaleH);
				// H.M.Wang 2019-09-11 将简单取整修改为四舍五入
				int h = Math.round(o.getHeight() * scaleH);
//				int h = (int)(o.getHeight() * scaleH);
				Debug.d(TAG, "--->save height=" + h);
				h = h%2 == 0? h : h + 1;
				// H.M.Wang 2019-09-11 将简单取整修改为四舍五入
				int w = Math.round(o.getWidth() * scaleW);
//				int w = (int) (o.getWidth() * scaleW);
				Bitmap t = o.makeBinBitmap(mContext, o.getContent(), w, h, o.getFont());
				if (t == null) {
					continue;
				}
				// BinFromBitmap.saveBitmap(bmp, "barcode.png");

				// H.M.Wang 修改1行
//				can.drawBitmap(t, o.getX() * scaleW, o.getY() * scaleH, p);
				can.drawBitmap(t, Math.round(o.getX() * scaleW), Math.round(o.getY() * scaleH), p);

				// H.M.Wang 增加1行
//                t.recycle();
				// BinFromBitmap.saveBitmap(bmp, "barcode_1.png");
			} else if (o instanceof GraphicObject) {
				// H.M.Wang 2019-09-11 将简单取整修改为四舍五入
				int h = Math.round(o.getHeight() * scaleH);
//				int h = (int)(o.getHeight() * scaleH);
				int w = Math.round(o.getWidth() * scaleW);
//				int w = (int) (o.getWidth() * scaleW);
				Bitmap t = ((GraphicObject) o).makeBinBitmap(mContext, null, w, h, null);
				if (t != null) {
					Debug.d(TAG, "---> w= " + t.getWidth() * scaleW + " h= " + t.getHeight() * scaleH);
					Debug.d(TAG, "---> x= " + o.getX() * scaleW + " y= " + o.getY() * scaleH);

					// H.M.Wang 修改1行
//					can.drawBitmap(t, o.getX() * scaleW, o.getY() * scaleH, p);
					can.drawBitmap(t, Math.round(o.getX() * scaleW), Math.round(o.getY() * scaleH), p);

					// H.M.Wang2019-9-17 注释掉1行。否则，将可能把GraphicObject的元图片回收，后续操作死机
//					t.recycle();
				}
			} else {
//				Debug.d(TAG, "SaveTime: - Start MakeBinBitmap() : " + System.currentTimeMillis());
				// H.M.Wang 2019-09-11 将简单取整修改为四舍五入
				Bitmap t = o.makeBinBitmap(mContext, o.getContent(), Math.round(o.getWidth() * scaleW), Math.round(o.getHeight() * scaleH), o.getFont());
//				Bitmap t = o.makeBinBitmap(mContext, o.getContent(), (int)(o.getWidth() * scaleW), (int)(o.getHeight() * scaleH), o.getFont());
				if (t != null) {
//					Debug.d(TAG, "1.bin drawBitmap = [" + Math.round(o.getX() * scaleW) + ", " + Math.round(o.getY() * scaleH) + "]");
//					Debug.d(TAG, "SaveTime: - Start drawBitmap() : " + System.currentTimeMillis());
					// H.M.Wang 修改1行
					can.drawBitmap(t, Math.round(o.getX() * scaleW), Math.round(o.getY() * scaleH), p);
//					can.drawBitmap(t, (int)(o.getX() * scaleW), (int)(o.getY() * scaleH), p);

					// H.M.Wang 增加1行
					t.recycle();
				} else {
					Debug.d(TAG, "--->bitmap null");
				}
				
				// BinFromBitmap.recyleBitmap(t);
			}
		//can.drawText(mContent, 0, height-30, mPaint);
		}
//		BinFromBitmap.saveBitmap(bmp, getName()+".png");
		// 生成bin文件
//		Debug.d(TAG, "SaveTime: - Start BinFileMaker : " + System.currentTimeMillis());
		BinFileMaker maker = new BinFileMaker(mContext);
		/** if high resolution, keep original width */

//		Debug.d(TAG, "SaveTime: - Start maker.extract : " + System.currentTimeMillis());
		// H.M.Wang 修改下列两行
//		if (msgObj.getResolution() || (getNozzle() == PrinterNozzle.MESSAGE_TYPE_16_DOT) || (getNozzle() == PrinterNozzle.MESSAGE_TYPE_32_DOT)) {
// H.M.Wang 2021-4-9 将msgObj.getResolution()移到大字机分支，因为如果在这里，将导致300dpi的时候不会插空
//		if (msgObj.getResolution() ||
		if(
// End of H.M.Wang 2021-4-9 将msgObj.getResolution()移到大字机分支，因为如果在这里，将导致300dpi的时候不会插空
			(getNozzle() == PrinterNozzle.MESSAGE_TYPE_16_DOT) ||
			(getNozzle() == PrinterNozzle.MESSAGE_TYPE_32_DOT) ||
// H.M.Wang 2020-7-23 追加32DN打印头
			(getNozzle() == PrinterNozzle.MESSAGE_TYPE_32DN) ||
// End of H.M.Wang 2020-7-23 追加32DN打印头
// H.M.Wang 2020-8-18 追加32SN打印头
			(getNozzle() == PrinterNozzle.MESSAGE_TYPE_32SN) ||
// End of H.M.Wang 2020-8-18 追加32SN打印头
// H.M.Wang 2020-8-26 追加64SN打印头
			(getNozzle() == PrinterNozzle.MESSAGE_TYPE_64SN) ||
// End of H.M.Wang 2020-8-26 追加64SN打印头
// H.M.Wang 2022-10-19 追加64SLANT头
			(getNozzle() == PrinterNozzle.MESSAGE_TYPE_64SLANT) ||
// End of H.M.Wang 2022-10-19 追加64SLANT头
// H.M.Wang 2024-4-29 追加64_DOT_ONE喷头类型
			(getNozzle() == PrinterNozzle.MESSAGE_TYPE_64DOTONE) ||
// End of H.M.Wang 2024-4-29 追加64_DOT_ONE喷头类型
// H.M.Wang 2024-9-10 增加一个16DOTX4头类型，
			(getNozzle() == PrinterNozzle.MESSAGE_TYPE_16DOTX4) ||
// End of H.M.Wang 2024-9-10 增加一个16DOTX4头类型，
// H.M.Wang 2022-5-27 追加32x2头类型
			(getNozzle() == PrinterNozzle.MESSAGE_TYPE_32X2) ||
// End of H.M.Wang 2022-5-27 追加32x2头类型
			(getNozzle() == PrinterNozzle.MESSAGE_TYPE_64_DOT) ||
// H.M.Wang 2023-7-29 追加48点头
			(getNozzle() == PrinterNozzle.MESSAGE_TYPE_48_DOT) ||
// End of H.M.Wang 2023-7-29 追加48点头
// H.M.Wang 2021-8-16 追加96DN头
			(getNozzle() == PrinterNozzle.MESSAGE_TYPE_96DN)) {
// End of H.M.Wang 2021-8-16 追加96DN头

			// H.M.Wang 追加一个是否移位的参数
			mDots = maker.extract(Bitmap.createScaledBitmap(bmp, bWidth, bHeight, false), getNozzle().mHeads, false);
		} else {
			// H.M.Wang 追加一个是否移位的参数
// H.M.Wang 2021-2-26 取消过滤选项，过滤选项的目的是使得图像平滑，但是会打乱图像的内容
//			mDots = maker.extract(Bitmap.createScaledBitmap(bmp, bWidth/2, bHeight, true), msgObj.getPNozzle().mHeads1
// End of H.M.Wang 2021-2-26 取消过滤选项，过滤选项的目的是使得图像平滑，但是会打乱图像的内容
// H.M.Wang 2021-4-9 将msgObj.getResolution()移到大字机分支，因为如果在这里，将导致300dpi的时候不会插空
//			mDots = maker.extract(Bitmap.createScaledBitmap(bmp, bWidth/2, bHeight, false), msgObj.getPNozzle().mHeads,
// H.M.Wang 2021-4-9 追加ioctl的分辨率信息获取命令
//			mDots = maker.extract(Bitmap.createScaledBitmap(bmp, bWidth/(msgObj.getResolution()?1:2), bHeight, false), msgObj.getPNozzle().mHeads,
			mDots = maker.extract(Bitmap.createScaledBitmap(bmp, bWidth/(Configs.GetDpiVersion() == FpgaGpioOperation.DPI_VERSION_300 ? 1 : 2), bHeight, false), msgObj.getPNozzle().mHeads,
// End of H.M.Wang 2021-4-9 追加ioctl的分辨率信息获取命令
// End of H.M.Wang 2021-4-9 将msgObj.getResolution()移到大字机分支，因为如果在这里，将导致300dpi的时候不会插空
				((getNozzle() == PrinterNozzle.MESSAGE_TYPE_1_INCH ||
				  getNozzle() == PrinterNozzle.MESSAGE_TYPE_1_INCH_DUAL ||
				  getNozzle() == PrinterNozzle.MESSAGE_TYPE_1_INCH_TRIPLE ||
				  getNozzle() == PrinterNozzle.MESSAGE_TYPE_1_INCH_FOUR)));
			Debug.d(TAG, "mDots[0] = " + mDots[0] + "; [1] = " + mDots[1] + "; [2] = " + mDots[2]);
		}
//		Debug.d(TAG, "SaveTime: - End maker.extract : " + System.currentTimeMillis());

		// H.M.Wang 增加1行
		bmp.recycle();

		// 保存bin文件
		maker.save(ConfigPath.getBinAbsolute(mName));
	}
	
	/**
	 * 从16*16的点阵字库中提取点阵，生成打印buffer
	 *
	 */
	private void saveBinDotMatrix() {
		if(mObjects==null || mObjects.size() <= 0)
			return ;
		
		String content="";
		for(BaseObject o:mObjects)
		{
			if((o instanceof MessageObject)	)
				continue;
			
			if(o instanceof CounterObject)
			{
				content += o.getContent();
			}
			else if(o instanceof RealtimeObject)
			{
				content += o.getContent();
				Debug.d(TAG, "--->realtime: " + content);
			}
// H.M.Wang 2020-2-16 追加HyperText控件
			else if(o instanceof HyperTextObject)
			{
				content += o.getContent();
				Debug.d(TAG, "--->hypertext: " + content);
			}
// End of H.M.Wang 2020-2-16 追加HyperText控件
// H.M.Wang 2020-6-10 追加DynamicText控件
			else if(o instanceof DynamicText)
			{
				content += o.getContent();
				Debug.d(TAG, "--->DynamicText: " + content);
			}
// End of H.M.Wang 2020-6-10 追加DynamicText控件
			else if(o instanceof JulianDayObject)
			{
				content += o.getContent();
			}
			else if(o instanceof ShiftObject)
			{
				content += o.getContent();
			}
			else
			{
				content += o.getContent();
			}
		//can.drawText(mContent, 0, height-30, mPaint);
		}
		// 生成bin文件
		BinFileMaker maker = new BinFileMaker(mContext);
		MessageObject msg = getMsgObject();
		mDots[0] = maker.extract(content);
		// 保存bin文件
		maker.save(ConfigPath.getTlkDir(mName) + "/1.bin");
		for(BaseObject o:mObjects)
		{
			if((o instanceof MessageObject)	) {
				((MessageObject) o).setDotCount(mDots[0]);
				break;
			}
		}
		
		return ;
	}

//	public void savePreview() {
//		int width=0;
//		Paint p=new Paint();
//		if(mObjects==null || mObjects.size() <= 0)
//			return ;
//		for(BaseObject o:mObjects)
//		{
//			width = (int)(width > o.getXEnd() ? width : o.getXEnd());
//		}
//
//		Bitmap bmp = Bitmap.createBitmap(width , Configs.gDots, Configs.BITMAP_CONFIG);
//		Debug.d(TAG, "drawAllBmp width="+width+", height="+Configs.gDots);
//		Canvas can = new Canvas(bmp);
//		can.drawColor(Color.WHITE);
//						
//		String content="";
//
//		for(BaseObject o:mObjects)
//		{
//			if (o instanceof MessageObject) {
//				continue;
//			}
//			
//			if(o instanceof CounterObject)
//			{						
//		//	o.setContent2(str_new_content)		;
//				Bitmap  t  = o.getpreviewbmp();
//
//				if (t== null) {
//					continue;
//					}
//				
//					can.drawBitmap(t, o.getX(), o.getY(), p);			
//			}
//
//			else if(o instanceof RealtimeObject)
//			{	Debug.e(TAG, "RealtimeObject");		
//				Bitmap  t  = o.getpreviewbmp();
//
//				if (t== null) {
//					continue;
//					}
//				can.drawBitmap(t, o.getX(), o.getY(), p);				
//			}			
//			else if(o instanceof JulianDayObject)
//			{
//				Bitmap  t  = o.getpreviewbmp();
//
//				if (t== null) {
//					continue;
//					}
//				can.drawBitmap(t, o.getX(), o.getY(), p);				
//			}else if(o instanceof TextObject)
//			{
//				Bitmap  t  = o.getpreviewbmp();
//
//				if (t== null) {
//					continue;
//					}
//				can.drawBitmap(t, o.getX(), o.getY(), p);				
//			}	
//					
//			/*	
//				TextObject
//			{
//				Bitmap t  = o.getScaledBitmap(mContext);
//					if (t== null) {
//					continue;
//				}	
//				can.drawBitmap(t, o.getX(), o.getY(), p);
//			
//			}*/
//			
//			/*	
//			else if(o instanceof ShiftObject)
//			{
//				content += o.getContent();
//			}
//			*/
//
//		//can.drawText(mContent, 0, height-30, mPaint);	
//			
//		//	if(o instanceof CounterObject)
//		//	{
//		//		o.setContent("lkaa");//mContext="liukun";
//		//	}	
//			
//		///	String o.setContent2("lkaa");//mContext="liukun";
//			
//		}
//		// Bitmap.createScaledBitmap();
//		float scale = bmp.getHeight() / 100;
//		width = (int) (width / scale);
//		
//		width=width/2; //addbylk 减半输出 
//		
//		Bitmap nBmp = Bitmap.createScaledBitmap(bmp, width, 100, false);
//		BitmapWriter.saveBitmap(nBmp, ConfigPath.getTlkDir(getName()), "1.bmp");
//	}

	public void savePreview() {
		int width=0;
		Paint p=new Paint();
		if(mObjects==null || mObjects.size() <= 0)
		return ;
		for(BaseObject o:mObjects)
		{
			// H.M.Wang 2019-09-11 将简单取整修改为四舍五入
			width = Math.round(width > o.getXEnd() ? width : o.getXEnd());
//			width = (int)(width > o.getXEnd() ? width : o.getXEnd());
		}

		Bitmap bmp = Bitmap.createBitmap(width , Configs.gDots, Configs.BITMAP_PRE_CONFIG);
		Debug.d(TAG, "drawPreviewBmp width="+width+", height="+Configs.gDots);
		Canvas can = new Canvas(bmp);
		can.drawColor(Color.WHITE);

		String content="";
		Bitmap t = null;
		for(BaseObject o:mObjects)
		{
			t = null;
			if (o instanceof MessageObject) {
				continue;
			}
			t  = o.getpreviewbmp();
			if (t == null) {
				t = o.getScaledBitmap(mContext);
				Debug.d(TAG, "++++++++++++++++++>bmp: " + t);
				if (t== null) {
					continue;
				}
			}
			if (t == null) {
				continue;
			}
			Debug.d(TAG, "--->preview w : " + t.getWidth() + ",  h = " + t.getHeight() + ",  x = " + o.getX() + " , y = " + o.getY());
			can.drawBitmap(t, o.getX(), o.getY(), p);
			/*if(o instanceof CounterObject)
			{
				t  = o.getpreviewbmp();
				if (t== null) {
				continue;
				}
				can.drawBitmap(t, o.getX(), o.getY(), p);
			} else if(o instanceof RealtimeObject) {	
				Debug.e(TAG, "RealtimeObject");
				t  = o.getpreviewbmp();	
				if (t== null) {
				continue;
				}
				can.drawBitmap(t, o.getX(), o.getY(), p);
			} else if(o instanceof JulianDayObject) {
				t  = o.getpreviewbmp();	
				if (t== null) {
					continue;
				}
				can.drawBitmap(t, o.getX(), o.getY(), p);
			} else if(o instanceof TextObject) {
				t  = o.getpreviewbmp();
				if (t== null) {
				continue;
				}
				can.drawBitmap(t, o.getX(), o.getY(), p);
			} else if (o instanceof GraphicObject) {
				t  = o.getpreviewbmp();
				if (t== null) {
				continue;
				}
				can.drawBitmap(t, o.getX(), o.getY(), p);
				
			} else if (o instanceof WeekOfYearObject) {
				t  = o.getpreviewbmp();
				if (t== null) {
				continue;
				}
				can.drawBitmap(t, o.getX(), o.getY(), p);
				
			} else if (o instanceof WeekOfYearObject) {
				t  = o.getpreviewbmp();
				if (t== null) {
				continue;
				}
				can.drawBitmap(t, o.getX(), o.getY(), p);
				
			} else {//addbylk
				t = o.getScaledBitmap(mContext);
				Debug.d(TAG, "++++++++++++++++++>bmp: " + t);
				if (t== null) {
					continue;
				}
				can.drawBitmap(t, o.getX(), o.getY(), p);
			}*/
//			if (t != null) {
//				BinFromBitmap.recyleBitmap(t);
//			}
		}
		// Bitmap.createScaledBitmap();
		float scale = bmp.getHeight() / 100f;
		// H.M.Wang 2019-09-11 将简单取整修改为四舍五入
		width = Math.round(width / scale);
//		width = (int) (width / scale);
		Debug.d(TAG, "---> +++++++ height = " + bmp.getHeight() + "   scale = " + scale);
		Bitmap nBmp = Bitmap.createScaledBitmap(bmp, width, 100, false);
		BitmapWriter.saveBitmap(nBmp, ConfigPath.getTlkDir(getName()), "1.bmp");
	}
	/**
	 * save picture to tlk dir
	 */
	private void saveExtras() {
		for (BaseObject object : getObjects()) {
			if (object instanceof GraphicObject) {
				((GraphicObject) object).afterSave();
			}
		}
	}

	public void save(SaveProgressListener l) {
		mSaveProgressListener = l;
		save(null, null);
	}

	public void save(Handler handler) {
		save(handler, null);
	}

	public void save(Handler handler, String message) {

//		resetIndexs();
//		//保存1.TLK文件
//		// saveTlk(mContext);
//		//保存1.bin文件
//		saveBin();
//		
//		//保存其他必要的文件
//		saveExtras();
//		
//		//保存vx.bin文件
//		saveVarBin();
//		
//		//保存1.TLK文件
//		saveTlk(mContext);
//				
//		//保存1.bmp文件
//		savePreview();
		mCallback = handler;
		if (mSaveTask != null) {
			Debug.e(TAG, "--->There is already a save task running");
			return;
		}
		mSaveTask = new SaveTask(message);
		mSaveTask.execute((Void[])null);
	}

	private void resetIndexs() {
		int i = 0;
		for (BaseObject o : mObjects) {
			o.setIndex(i + 1);
			if (o instanceof RealtimeObject) {
				i = o.getIndex() + ((RealtimeObject) o).getSubObjs().size();
// H.M.Wang 2020-2-17 追加HyperText控件
			} else if(o instanceof HyperTextObject) {
				i = o.getIndex() + ((HyperTextObject) o).getSubObjs().size();
// End of H.M.Wang 2020-2-17 追加HyperText控件
			} else {
				i = o.getIndex();
			}
		}
	}
	
	
	public MessageObject getMsgObject() {
		MessageObject msg = null;
		for (BaseObject object: mObjects) {
			if (object instanceof MessageObject) {
				msg = (MessageObject) object;
				break;
			}
		}
		return msg;
	}
	
	public int getHeads() {

		int heads = 1;
		MessageObject obj = getMsgObject();
		if (obj == null) {
			return heads;
		}
		return obj.getPNozzle().mHeads;
	}


	public PrinterNozzle getNozzle() {
		MessageObject obj = getMsgObject();

		if (obj == null) {
			return PrinterNozzle.MESSAGE_TYPE_12_7;
		}
		return obj.getPNozzle();
	}

	public String getPreview() {
		String messageFolder = ConfigPath.getTlkDir(mName);
		String previewBmp = messageFolder + MSG_PREV_IMAGE ;
		File bmp2 = new File(messageFolder, MSG_PREV_IMAGE2);
		if (bmp2.exists()) {
			previewBmp = messageFolder + MSG_PREV_IMAGE2;
		}
		Debug.i(TAG, "--->previewBmp: " + previewBmp);
		return previewBmp;
	}
	
	public static String getPreview(String name) {
		String messageFolder = ConfigPath.getTlkDir(name);
		String previewBmp = messageFolder + MSG_PREV_IMAGE ;
// H.M.Wang 2020-2-27 不参照2.bmp
//		File bmp2 = new File(messageFolder, MSG_PREV_IMAGE2);
//		if (bmp2.exists()) {
//			previewBmp = messageFolder + MSG_PREV_IMAGE2;
//		}
// End of H.M.Wang 2020-2-27 不参照2.bmp
		Debug.i(TAG, "--->previewBmp: " + previewBmp);
		return previewBmp;
	}
	
	public String getPath() {
		return ConfigPath.getTlkDir(mName);
	}
	
	public float getDiv() {
		return 4f/getHeads();
	}

// H.M.Wang 2022-11-27 追加一个用户群组(User Group)，实现群组信息从U盘导入
	public static ArrayList<String> parseUserGroup(String ugFolder) {
		ArrayList<String> gl = new ArrayList<String>();

		File file = new File(ConfigPath.getTlkDir(ugFolder) + File.separator + "UG.TLK");
		if (!file.exists()) {
			return gl;
		}
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String content = reader.readLine();
			String[] group = content.split("\\^");
			if (group == null) {
				return gl;
			}
			for (int i = 0; i < group.length; i++) {
				File f = new File(ConfigPath.getTlkDir(ugFolder + File.separator + group[i]));
				if (!f.exists()) {
					continue;
				}
				gl.add(group[i]);
			}
			return gl;
		} catch (Exception e) {
			Debug.e(TAG, e.getMessage());
		}
		return gl;
	}

	public static void saveUGTLK(String ugFolder, ArrayList<String> subMsgs) {
		StringBuilder sb = new StringBuilder();

		for(String msg : subMsgs) {
			sb.append(msg).append("^");
		}
		sb.delete(sb.length()-1, sb.length());

		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(ConfigPath.getTlkDir(ugFolder) + File.separator + "UG.TLK"));
			writer.write(sb.toString());
			writer.flush();
		} catch(Exception e) {
			Debug.e(TAG, e.getMessage());
		} finally {
			try {if(null != writer) writer.close();} catch(IOException e) {}
		}
	}

	public static int getUGIndex(String ugFolder) {
		File file = new File(ConfigPath.getTlkDir(ugFolder) + File.separator + "last_message.txt");
		if (!file.exists()) {
			return -1;
		}
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String content = reader.readLine();
			return Integer.valueOf(content);
		} catch (Exception e) {
			Debug.e(TAG, e.getMessage());
		}
		return -1;
	}

	public static void saveUGIndex(String ugFolder, int index) {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(ConfigPath.getTlkDir(ugFolder) + File.separator + "last_message.txt"));
			writer.write(String.valueOf(index));
			writer.flush();
		} catch(Exception e) {
			Debug.e(TAG, e.getMessage());
		} finally {
			try {if(null != writer) writer.close();} catch(IOException e) {}
		}
	}

	public static void cleanUGFolder(String ugFolder) {
		File file = new File(ConfigPath.getTlkDir(ugFolder));
		File[] files = file.listFiles();
		for(File f : files) {
			if(f.isDirectory()) {
				f.delete();
			}
			if(f.getName().equals("TG.TLK")) {
				f.delete();
			}
			if(f.getName().equals("last_message.txt")) {
				f.delete();
			}
		}
	}

	public void replaceUGTag(String ugStr) {
		float lastEnd = 0;

		for(BaseObject baseObject : mObjects) {
			if(baseObject instanceof MessageObject) continue;
			baseObject.setX(lastEnd);
			if(baseObject instanceof HyperTextObject) {
				if(baseObject.getContent().indexOf("@UG") >= 0) {
					String hyperContent = baseObject.getContent().replace("@UG", ugStr);
//										Debug.d(TAG, "HyperText: " + hyperContent);
					baseObject.setContent(hyperContent);
				}
			}
			lastEnd = baseObject.getXEnd();
		}
	}
// End of H.M.Wang 2022-11-27 追加一个用户群组(User Group)，实现群组信息从U盘导入

// H.M.Wang 2022-10-25 追加一个“快速分组”的信息类型，该类型以Configs.QUICK_GROUP_PREFIX为文件名开头，信息中的每个超文本作为一个独立的信息保存该信息的目录当中，并且所有的子信息组成一个群组，该群组的信息也保存到该信息的目录当中
	private void saveQuickGroup() {
		StringBuilder sb = new StringBuilder();
		int objIndex = 1;
		for (BaseObject obj: mObjects) {
			if (obj instanceof HyperTextObject) {
				MessageTask msgTask = new MessageTask(mContext);

				MessageObject msgObject = new MessageObject(mContext, 0);
				msgObject.setType(SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_HEAD_TYPE));
				msgTask.addObject(msgObject);

				HyperTextObject hypertext = new HyperTextObject(mContext, 0);
				hypertext.setY(0);
				hypertext.setFont(obj.getFont());
				hypertext.setContent(obj.getContent());
				msgTask.addObject(hypertext);

				msgTask.setName(mName + "/" + objIndex);
				msgTask.createTaskFolderIfNeed();
				msgTask.save(null, null);

				if(objIndex > 1) sb.append("^");
				sb.append(objIndex++);
			}
		}

		String cnt = sb.toString();
		if(!cnt.isEmpty())
			saveGroup(mName + "/" + Configs.GROUP_PREFIX + "1", cnt);
	}
// End of H.M.Wang 2022-10-25 追加一个“快速分组”的信息类型，该类型以Configs.QUICK_GROUP_PREFIX为文件名开头，信息中的每个超文本作为一个独立的信息保存该信息的目录当中，并且所有的子信息组成一个群组，该群组的信息也保存到该信息的目录当中

	public class SaveTask extends AsyncTask<Void, Void, Void>{

		private String message;
		public SaveTask(String message) {
			this.message = message;
		}
		@Override
		protected Void doInBackground(Void... params) {
// H.M.Wang 2023-2-2 先删除该文件夹中所有的文件
			FileUtil.deleteFolder(ConfigPath.getTlkDir(mName));
			new File(ConfigPath.getTlkDir(mName)).mkdir();
// End of H.M.Wang 2023-2-2 先删除该文件夹中所有的文件
			resetIndexs();
			//保存1.TLK文件
			// saveTlk(mContext);
			//保存1.bin文件
//			Debug.d(TAG, "SaveTime: - Start : " + System.currentTimeMillis());
			saveBin();

//			Debug.d(TAG, "SaveTime: - Start saveExtras() : " + System.currentTimeMillis());
			//保存其他必要的文件
			saveExtras();

//			Debug.d(TAG, "SaveTime: - Start saveVarBin() : " + System.currentTimeMillis());
			//保存vx.bin文件
			saveVarBin();

//			Debug.d(TAG, "SaveTime: - Start saveTlk() : " + System.currentTimeMillis());
			//保存1.TLK文件
			saveTlk(mContext);

//			Debug.d(TAG, "SaveTime: - Start savePreview() : " + System.currentTimeMillis());
			//保存1.bmp文件
			savePreview();
//			Debug.d(TAG, "SaveTime: - Finished : " + System.currentTimeMillis());

// H.M.Wang 2022-10-25 追加一个“快速分组”的信息类型，该类型以Configs.QUICK_GROUP_PREFIX为文件名开头，信息中的每个超文本作为一个独立的信息保存在母信息的目录当中，并且所有的子信息作为一个群组管理，该子群组的信息也保存到木信息的目录当中
			if(mName.indexOf("/") == -1 && mName.startsWith(Configs.QUICK_GROUP_PREFIX)) {
				saveQuickGroup();
			}
// End of H.M.Wang 2022-10-25 追加一个“快速分组”的信息类型，该类型以Configs.QUICK_GROUP_PREFIX为文件名开头，信息中的每个超文本作为一个独立的信息保存在母信息的目录当中，并且所有的子信息作为一个群组管理，该子群组的信息也保存到木信息的目录当中

			//
			return null;
		}
		@Override
        protected void onPostExecute(Void result) {

			// H.M.Wang 增加6行。将计数器当前值设置到SystemConfigFile参数当中。后续因为编辑任务中输入的内容做无意义处理，因此取消该段功能
/*
			for(BaseObject o : mObjects) {
				if(o instanceof CounterObject) {
					SystemConfigFile systemConfigFile = SystemConfigFile.getInstance();
					systemConfigFile.setParam(((CounterObject) o).mCounterIndex + SystemConfigFile.INDEX_COUNT_1, ((CounterObject) o).getValue());
				}
			}
*/

			Message msg = new Message();
			msg.what = EditTabSmallActivity.HANDLER_MESSAGE_SAVE_SUCCESS;

			if (this.message != null) {
				Bundle bundle = new Bundle();
				bundle.putString("pcCommand", this.message);
				msg.setData(bundle);
			}
			if (mCallback != null) {
				mCallback.sendMessage(msg);
// H.M.Wang 2023-10-8 从这里移到if语句外面，因为这里应该跟if语句的判断条件无关
//				mSaveTask = null;
			}
			mSaveTask = null;
// End of H.M.Wang 2023-10-8 从这里移到if语句外面，因为这里应该跟if语句的判断条件无关

			if(null != mSaveProgressListener) {
				mSaveProgressListener.onSaved();
			}
		}
	}

	public static void saveGroup(String name, String contents) {
		File dir = new File(ConfigPath.getTlkDir(name));
		if(!dir.exists() && !dir.mkdirs())
		{
			Debug.d(TAG, "create dir error "+dir.getPath());
			return ;
		}
		File tlk = new File(dir, "1.TLK");
		try {
			OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(tlk));
			writer.write(contents);
			writer.flush();
			writer.close();

		} catch (Exception e) {

		}
		Debug.d("XXX", "--->contents: " + contents);
		String[] msgs = contents.split("\\^");
		Bitmap bmp = Bitmap.createBitmap(msgs.length * 500, 100, Configs.BITMAP_CONFIG);
		
		Canvas canvas = new Canvas(bmp);
		canvas.drawColor(Color.WHITE);
		int index = 0;
		Paint paint = new Paint();
		paint.setTextSize(40);
		paint.setStrokeWidth(2);
		paint.setFakeBoldText(true);
		FontMetrics fm = paint.getFontMetrics();
		int x = 0;
		for (String msg : msgs) {
			x += 50;

			// H.M.Wang 2019-09-11 将简单取整修改为四舍五入
			int w = Math.round(paint.measureText(msg));
//			int w = (int)paint.measureText(msg);
			Debug.d("XXX", "--->msg: " + msg);
			canvas.drawText(msg, x, 60, paint);
			x += w + 50;
			canvas.drawLine(x, 20, x, 80, paint);
			index++;
		}
		BitmapWriter.saveBitmap(bmp, dir.getAbsolutePath(), "1.bmp");

	}

// H.M.Wang 2022-10-25 追加一个“快速分组”的信息类型，打印的时候，因为内部设置的群组保存在信息目录的里面因此，parseGroup的处理方法需要改进
	public static List<String> parseQuickGroup(String name) {
		Debug.d(TAG, "--->parseQuickGroup: " + name);
		File file = new File(ConfigPath.getTlkDir(name + "/" + Configs.GROUP_PREFIX + "1"), "1.TLK");
		if (!file.exists()) {
			return null;
		}
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String content = reader.readLine();
			String[] group = content.split("\\^");
			if (group == null) {
				return null;
			}
			ArrayList<String> gl = new ArrayList<String>();
			for (int i = 0; i < group.length; i++) {
				File f = new File(ConfigPath.getTlkDir(name + "/" + group[i]));
				if (!f.exists()) {
					continue;
				}
				gl.add(group[i]);
			}
			return gl;
		} catch (Exception e) {
			Debug.e(TAG, e.getMessage());
		}
		return null;
	}
// End of H.M.Wang 2022-10-25 追加一个“快速分组”的信息类型，

	public static List<String> parseGroup(String name) {
		Debug.d(TAG, "--->parseGroup: " + name);
		File file = new File(ConfigPath.getTlkDir(name), "1.TLK");
		if (!file.exists()) {
			return null;
		}
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String content = reader.readLine();
			String[] group = content.split("\\^");
			if (group == null) {
				return null;
			}
			ArrayList<String> gl = new ArrayList<String>();
			for (int i = 0; i < group.length; i++) {
				File f = new File(ConfigPath.getTlkDir(group[i]));
				if (!f.exists()) {
					continue;
				}
				gl.add(group[i]);
			}
			return gl;
		} catch (Exception e) {
			Debug.e(TAG, e.getMessage());
		}
		return null;
	}

	public boolean isPrintable() {
		return isPrintable;
	}

	public int unPrintableTips() {
		return unPrintableTips;
	}

	/**
	 * check if 1.bin file exists
	 * @return true if exist, false otherwise
	 */
	private boolean checkBin() {
		String bin = ConfigPath.getBinAbsolute(mName);
		File bf = new File(bin);
		if (!bf.exists()) {
			return false;
		}
		return true;
	}

	/**
	 * check if all Vx.bin exist
	 * @return
	 */
	private boolean checkVBin() {
		if (mObjects == null || mObjects.size() == 0) {
			return true;
		}

		boolean vbOk = true;
		for (BaseObject object : mObjects) {
			if (!object.needVBin()) {
				continue;
			}
			String v = ConfigPath.getVBinAbsolute(mName, object.getIndex());
			File vf = new File(v);
			if (vf.exists()) {
				vbOk &= true;
			} else {
				vbOk &= false;
			}
		}
		return vbOk;
	}
}
