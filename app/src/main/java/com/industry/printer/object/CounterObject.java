package com.industry.printer.object;

import com.industry.printer.ControlTabActivity;
import com.industry.printer.MainActivity;
import com.industry.printer.R;
import com.industry.printer.FileFormat.SystemConfigFile;
import com.industry.printer.Utils.Configs;
import com.industry.printer.Utils.Debug;
import com.industry.printer.data.BinFromBitmap;
import com.industry.printer.cache.FontCache;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.Paint.FontMetrics;
import android.renderscript.Sampler.Value;
import android.util.Log;

import com.industry.printer.hardware.ExtGpio;
import com.industry.printer.hardware.RTCDevice;

public class CounterObject extends BaseObject {
	private static final String TAG = CounterObject.class.getSimpleName();

	private enum Direction {
		INCREASE, DECREASE
	};

	private int mBits;
	private int mStart;
	private int mEnd;
	private Direction mDirection;
	private int mValue;
// H.M.Wang 2021-5-7 追加实际打印计数器变量，目的是记忆实际打印（而非apk下发，这个在FIFO打印时可能不一致）数量
	private int mPrintedValue;
// End of H.M.Wang 2021-5-7 追加实际打印计数器变量，目的是记忆实际打印（而非apk下发，这个在FIFO打印时可能不一致）数量
	private int mStepLen;
	// 计数器初始值对应设置里10个计数器值的编号
	private int mCounterIndex;
	//public int mCurVal;
// H.M.Wang 2023-1-4 追加一个参数步长细分/Sub step。其功能是决定计数器在打印过程中何时进行调整，n=0或n=1为每次打印均调整，n>1时为打印n次后调整
	private int mSubStepValue;
	private int mSubStepCount;
// End of H.M.Wang 2023-1-4 追加一个参数步长细分/Sub step。
// H.M.Wang 2024-10-11 重新整理步长细分的管理方法，具体内容如下
// (1) 	mSubStepValue作为细分计数的总次数，比如设为5，就是同一个计数器值被打印5次再变化，同时作为是否启用细分计数功能的标识，0为不启用，大于0为启用
// (2) 	mSubStepCount作为细分计数采用的计数值，范围为[0, mSubStepValue)，表示当前计数器的细分计数的当前值。初始值从RTC(0x3c)中获得
// (3) 	mSubStepPrintedCount作为细分计数已打印的计数值，范围为[0, mSubStepValue)，表示当前计数器的细分计数待打印的最早值。初始值从RTC(0x3c)中获得
// (4) 	goNext更新维护mSubStepCount， goPrintedNext更新维护mSubStepPrintedCount，RTC(0x3c)中保存mSubStepPrintedCount的值
// (5)  setCounterIndex的时候，如果是计数器9，则	mSubStepValue， mSubStepCount， mSubStepPrintedCount清零，不支持细分计数
	private int mSubStepPrintedCount;
// End of H.M.Wang 2024-10-11 重新整理步长细分的管理方法

	public CounterObject(Context context, float x) {
		super(context, BaseObject.OBJECT_TYPE_CNT, x);
		mBits = 5;
		mStart = 0;
		mEnd = 99999;
		mValue = 0;
		mPrintedValue = 0;
		mDirection = Direction.INCREASE;
		mStepLen=1;
		mCounterIndex = 0;
		mContent = "00000";
// H.M.Wang 2023-1-4 追加一个参数步长细分/Sub step
		mSubStepValue = SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_SUB_STEP);
// H.M.Wang 2024-10-11 重新整理步长细分的管理方法
		if(mSubStepValue > 0) {
			mSubStepPrintedCount = RTCDevice.getInstance(mContext).readSubStep();
			if(mSubStepPrintedCount < 0 || mSubStepPrintedCount >= mSubStepValue) mSubStepPrintedCount = 0;
		} else {
			mSubStepPrintedCount = 0;
		}
		mSubStepCount = mSubStepPrintedCount;
/*
// H.M.Wang 2023-9-20 步长细分的取值，不再从参数中取最大值，而是从RTC中取上次的保存值
		mSubStepCount = RTCDevice.getInstance(mContext).readSubStep();
		if(mSubStepCount < 0 || mSubStepCount >= mSubStepValue) mSubStepCount = 0;
//		mSubStepCount = mSubStepValue;
// End of H.M.Wang 2023-9-20 步长细分的取值，不再从参数中取最大值，而是从RTC中取上次的保存值
 */
// End of H.M.Wang 2024-10-11 重新整理步长细分的管理方法
// End of H.M.Wang 2023-1-4 追加一个参数步长细分/Sub step
	}

	public CounterObject(Context context, BaseObject parent, float x) {
		this(context, x);
		mParent = parent;
	}

	public void setBits(int bits) {
		if(mBits == bits) return;

		mBits = bits;
		Debug.d(TAG, "Set bits: " + mBits);

		int max = (int)Math.pow(10, mBits) - 1;

		mStart = (mDirection == Direction.INCREASE ? (mStart > max ? 0 : mStart) : (mStart > max ? max : mStart));
		mEnd = (mDirection == Direction.INCREASE ? (mEnd > max ? max : mEnd) : (mEnd > max ? 0 : mEnd));

		mValue = (mDirection == Direction.INCREASE ? Math.max(mValue, mStart) : Math.min(mValue, mStart));
		mValue = (mDirection == Direction.INCREASE ? Math.min(mValue, mEnd) : Math.max(mValue, mEnd));

		super.setContent(BaseObject.intToFormatString(mValue, mBits));

		// H.M.Wang 2019-10-8 根据位数调整宽度
		setWidth(mPaint.measureText(getContent()));
	}
	
	public int getBits()
	{
		return mBits;
	}

	public void setStart(int begin)	{
		if(mStart == begin) return;

		int max = (int)Math.pow(10, mBits) - 1;
		mStart = Math.max(1, Math.min(max, begin));
		Debug.d(TAG, "Set start: " + mStart);

		mDirection = (mStart <= mEnd ? Direction.INCREASE : Direction.DECREASE);
		Debug.d(TAG, "direction: " + mDirection);

		mValue = (mDirection == Direction.INCREASE ? Math.max(mValue, mStart) : Math.min(mValue, mStart));
//		mValue = (mDirection == Direction.INCREASE ? Math.min(mValue, mEnd) : Math.max(mValue, mEnd));

// H.M.Wang 2022-2-14 对mValue进行了操作，反映到mContent里面
		super.setContent(BaseObject.intToFormatString(mValue, mBits));
// End of H.M.Wang 2022-2-14 对mValue进行了操作，反映到mContent里面
	}
	
	public int getStart()
	{
		return mStart;
	}
	
	public void setEnd(int end)	{
		if(mEnd == end) return;

		int max = (int)Math.pow(10, mBits) - 1;
		mEnd = Math.max(1, Math.min(max, end));
		Debug.d(TAG, "Set end: " + mEnd);

		mDirection = (mStart <= mEnd ? Direction.INCREASE : Direction.DECREASE);
		Debug.d(TAG, "direction: " + mDirection);

//		mValue = (mDirection == Direction.INCREASE ? Math.max(mValue, mStart) : Math.min(mValue, mStart));
		mValue = (mDirection == Direction.INCREASE ? Math.min(mValue, mEnd) : Math.max(mValue, mEnd));

// H.M.Wang 2022-2-14 对mValue进行了操作，反映到mContent里面
		super.setContent(BaseObject.intToFormatString(mValue, mBits));
// End of H.M.Wang 2022-2-14 对mValue进行了操作，反映到mContent里面
	}
	
	public int getEnd()
	{
		return mEnd;
	}
	
	public void setRange(int start, int end) {
		if(mStart == start && mEnd == end) return;

		int max = (int)Math.pow(10, mBits) - 1;
		mStart = Math.max(0, Math.min(max, start));
		mEnd = Math.max(0, Math.min(max, end));

		Debug.d(TAG, "Set range: [" + mStart + ", " + mEnd + "]");

		mDirection = (mStart <= mEnd ? Direction.INCREASE : Direction.DECREASE);
		Debug.d(TAG, "direction: " + mDirection);

		mValue = (mDirection == Direction.INCREASE ? Math.max(mValue, mStart) : Math.min(mValue, mStart));
		mValue = (mDirection == Direction.INCREASE ? Math.min(mValue, mEnd) : Math.max(mValue, mEnd));

// H.M.Wang 2022-2-14 对mValue进行了操作，反映到mContent里面
		super.setContent(BaseObject.intToFormatString(mValue, mBits));
// End of H.M.Wang 2022-2-14 对mValue进行了操作，反映到mContent里面
	}

	public String getDirection() {
		String[] directions = mContext.getResources().getStringArray(R.array.strDirectArray);
		return (mDirection == Direction.INCREASE ? directions[0] : directions[1]);
	}
	
	public void setSteplen(int step) {
		mStepLen = (step < 1 ? 1 : step);
		Debug.d(TAG, "Set step: " + mStepLen);
	}
	
	public int getSteplen()
	{
		return mStepLen;
	}

	public void setValue(int value) {
//		if(mDirection == Direction.INCREASE ? value > mEnd || value < mStart : value < mEnd || value > mStart) return;
// H.M.Wang 2022-2-14 取消这个值相同不操作的判断。原因是value和mValue可能不在start和end之间，需要后续的调整。还有在本次修改之前，有些对mValue的修改没有反应到super.SetContent里面，导致Value和Content不一致
//		if(mValue == value) return;
// End of H.M.Wang 2022-2-14 取消这个值相同不操作的判断。原因是value和mValue可能不在start和end之间，需要后续的调整。还有在本次修改之前，有些对mValue的修改没有反应到super.SetContent里面，导致Value和Content不一致

		mValue = Math.min(Math.max(value, Math.min(mStart, mEnd)), Math.max(mStart, mEnd));
		mPrintedValue = mValue;

		SystemConfigFile.getInstance(mContext).setParamBroadcast(mCounterIndex + SystemConfigFile.INDEX_COUNT_1, mValue);
		RTCDevice.getInstance(mContext).write(mValue, mCounterIndex);
// H.M.Wang 2024-10-11 重新整理步长细分的管理方法
/*
// H.M.Wang 2023-3-14 修改计数器的当前值，则计数细分重置，新计数器值和新计数细分数同时生成
//   当前出现的错误是：如：本次任务共打印十次01，十次02，五次03，此时修改计数器当前值为1，设备会继续打印上一组03的计数细分的剩余数量，打印五次01，再接着打印十次02
// H.M.Wang 2023-9-20 步长细分的取值，不再从参数中取最大值，而是从RTC中取上次的保存值
// H.M.Wang 2024-10-9 当计数器索引为9（即最后一个计数器）时，无论计数细分为何值，均不考虑而直接修改计数
		if(mCounterIndex == 9) {
			mSubStepValue = 0;		// 修改本变量的细分计数上限值到0，以避免实际的细分计数操作
		}
// End of H.M.Wang 2024-10-9 当计数器索引为9（即最后一个计数器）时，无论计数细分为何值，均不考虑而直接修改计数
		mSubStepCount = RTCDevice.getInstance(mContext).readSubStep();
		if(mSubStepCount < 0 || mSubStepCount >= mSubStepValue) mSubStepCount = 0;
//		mSubStepCount = mSubStepValue;
// End of H.M.Wang 2023-9-20 步长细分的取值，不再从参数中取最大值，而是从RTC中取上次的保存值
// End of H.M.Wang 2023-3-14 修改计数器的当前值，则计数细分重置，新计数器值和新计数细分数同时生成
*/
// End of H.M.Wang 2024-10-11 重新整理步长细分的管理方法

		Debug.d(TAG, "Set value: " + mValue);

		super.setContent(BaseObject.intToFormatString(mValue, mBits));
	}

	@Override
	public void setContent(String content) {
		try{
			setValue(Integer.parseInt(content));
			Debug.d(TAG, "Set content: [" + getContent() + "]");
		} catch (Exception e) {
			Debug.e(TAG, "Set Content Exception: " + e.getMessage());
		}
	}

	public void goNext() {
// H.M.Wang 2023-1-4 追加一个参数步长细分/Sub step
// H.M.Wang 2024-10-11 重新整理步长细分的管理方法
		if(mSubStepValue > 0) {
			mSubStepCount++;
			Debug.d(TAG, "SubStep Used Status [CounterIndex: " + mCounterIndex + "; Value: " + mValue + "; SubStep: " + mSubStepCount + "/" + mSubStepValue + "]");
			if (mSubStepCount < mSubStepValue) return;
// H.M.Wang 2023-10-16 追加协议7。当细分计数器到达本轮重点的时候， 比如60细分， 到了60次，报警灯亮30s
			if (SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_IPURT_PROC) == SystemConfigFile.INPUT_PROTO_7) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							ExtGpio.writeGpio('h', 7, 1);
							Thread.sleep(30 * 1000);
							ExtGpio.writeGpio('h', 7, 0);
						} catch (Exception e) {
							Debug.e(TAG, e.getMessage());
						}
					}
				}).start();
			}
// End of H.M.Wang 2023-10-16 追加协议7。当细分计数器到达本轮重点的时候， 比如60细分， 到了60次，报警灯亮20s
			mSubStepCount = 0;
		}
/*
// H.M.Wang 2023-9-20 为了配合步长细分从RTC当中读写，将mSubStepCount的初值为mSubStepValue，逐次递减，改为初值为0，逐次递增，最大值为mSubStepValue。
		mSubStepCount++;
// H.M.Wang 2024-10-9 当计数器索引为9（即最后一个计数器）时，无论计数细分为何值，均不考虑而直接修改计数
		if(mCounterIndex != 9) {		// 忽略细分计数时，不保存修改值，以免影响其它的计数器
			RTCDevice.getInstance(mContext).writeSubStep(mSubStepCount);
		}
// End of H.M.Wang 2024-10-9 当计数器索引为9（即最后一个计数器）时，无论计数细分为何值，均不考虑而直接修改计数
		Debug.d(TAG, "CounterIndex: " + mCounterIndex + "; Value: " + mValue + "; SubStep: " + mSubStepCount + "/" + mSubStepValue);
		if (mSubStepCount < mSubStepValue) return;
// H.M.Wang 2023-10-16 追加协议7。当细分计数器到达本轮重点的时候， 比如60细分， 到了60次，报警灯亮30s
		if (mSubStepValue > 0 && SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_IPURT_PROC) == SystemConfigFile.INPUT_PROTO_7) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						ExtGpio.writeGpio('h', 7, 1);
						Thread.sleep(30 * 1000);
						ExtGpio.writeGpio('h', 7, 0);
					} catch (Exception e) {
						Debug.e(TAG, e.getMessage());
					}
				}
			}).start();
		}
// End of H.M.Wang 2023-10-16 追加协议7。当细分计数器到达本轮重点的时候， 比如60细分， 到了60次，报警灯亮20s
		mSubStepCount = 0;

//		mSubStepCount--;
//		if(mSubStepCount > 0) return;
//		mSubStepCount = mSubStepValue;
*/
// End of H.M.Wang 2023-9-20 为了配合步长细分从RTC当中读写，将mSubStepCount的初值为mSubStepValue，逐次递减，改为初值为0，逐次递增，最大值为mSubStepValue。
// End of H.M.Wang 2024-10-11 重新整理步长细分的管理方法

// End of H.M.Wang 2023-1-4 追加一个参数步长细分/Sub step

		int value = (mDirection == Direction.INCREASE ? mValue + mStepLen : mValue - mStepLen);
// H.M.Wang 2022-2-14 追加在计数器到达end的时候，写OUT4两秒的操作
		if(mDirection == Direction.INCREASE ? value > mEnd : value < mEnd) {
			ExtGpio.setOut4_2sec();
		}
// End of H.M.Wang 2022-2-14 追加在计数器到达end的时候，写OUT4两秒的操作
		mValue = (mDirection == Direction.INCREASE ? (value > mEnd ? mStart : value) : (value < mEnd ? mStart : value));
		super.setContent(BaseObject.intToFormatString(mValue, mBits));

		Debug.d(TAG, "Go Next: " + mValue);
	}
// H.M.Wang 2024-12-28 增加显示计数器当前值所需要的接口和达到边界后报警的接口
	private boolean mReachEdge;

	public int getPrintedValue() {
		return mPrintedValue;
	}

	public boolean isReachedEdge() {
		return mReachEdge;
	}
// End of H.M.Wang 2024-12-28 增加显示计数器当前值所需要的接口和达到边界后报警的接口

	public void goPrintedNext() {
// H.M.Wang 2024-10-11 重新整理步长细分的管理方法
		if(mSubStepValue > 0) {
			mSubStepPrintedCount++;
			Debug.d(TAG, "SubStep Printed Status [CounterIndex: " + mCounterIndex + "; Value: " + mPrintedValue + "; SubStep: " + mSubStepPrintedCount + "/" + mSubStepValue + "]");
			if(mSubStepPrintedCount < mSubStepValue) {
				RTCDevice.getInstance(mContext).writeSubStep(mSubStepPrintedCount);
				return;
			}
			mSubStepPrintedCount = 0;
			RTCDevice.getInstance(mContext).writeSubStep(mSubStepPrintedCount);
		}
/*
// H.M.Wang 2023-3-14 当使用步长细分功能时（即一个计数值会打印多次），这里记忆的是实际打印的次数，需要修改为计数器的实际值。否则，会出现下列奇怪现象：
//   如：本次任务共打印十次01，十次02，五次03，此时停止打印，系统中计数器0的数值会变成25，再次启动打印后，会从25开始打印五次，再接着打印26
// H.M.Wang 2023-9-20 为了配合步长细分从RTC当中读写，将mSubStepCount的初值为mSubStepValue，逐次递减，改为初值为0，逐次递增，最大值为mSubStepValue。
		if(mSubStepCount != 0) return;
//		if(mSubStepCount != mSubStepValue) return;
// End of H.M.Wang 2023-9-20 为了配合步长细分从RTC当中读写，将mSubStepCount的初值为mSubStepValue，逐次递减，改为初值为0，逐次递增，最大值为mSubStepValue。
// H.M.Wang 2023-3-14 当使用步长细分功能时（即一个计数值会打印多次），这里记忆的是实际打印的次数，需要修改为计数器的实际值。
*/
// End of H.M.Wang 2024-10-11 重新整理步长细分的管理方法
		int value = (mDirection == Direction.INCREASE ? mPrintedValue + mStepLen : mPrintedValue - mStepLen);

		if(mDirection == Direction.INCREASE) {
			if(value > mEnd) {
				mPrintedValue = mStart;
				mReachEdge = true;
			} else {
				mPrintedValue = value;
				mReachEdge = false;
			}
		} else {
			if(value < mEnd) {
				mPrintedValue = mStart;
				mReachEdge = true;
			} else {
				mPrintedValue = value;
				mReachEdge = false;
			}
		}

		SystemConfigFile.getInstance(mContext).setParamBroadcast(mCounterIndex + SystemConfigFile.INDEX_COUNT_1, mPrintedValue);
		RTCDevice.getInstance(mContext).write(mPrintedValue, mCounterIndex);

		Debug.d(TAG, "Go Printed Next: " + mPrintedValue);
	}

	public void setCounterIndex(int index) {
		if(index < 0 || index >= 10) return;
		mCounterIndex = index;

// H.M.Wang 2024-10-11 重新整理步长细分的管理方法
		if(mCounterIndex == 9) {
			mSubStepValue = 0;
			mSubStepCount = 0;
			mSubStepPrintedCount = 0;
		}
// End of H.M.Wang 2024-10-11 重新整理步长细分的管理方法

// H.M.Wang 2020-8-4 变更计数器索引之后，用该计数器的值重新设置本地内容
		setContent("" + SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_COUNT_1 + index));
// End of H.M.Wang 2020-8-4 变更计数器索引之后，用该计数器的值重新设置本地内容
		Debug.d(TAG, "Set counter index: " + mCounterIndex);
	}

	public int getCounterIndex() {
		return mCounterIndex;
	}

	public String toString()
	{
		float prop = getProportion();
		StringBuilder builder = new StringBuilder(mId);
		int v = 0;
		try {
			v = Integer.parseInt(mContent);
		} catch ( Exception e) {

		}
		builder.append("^")
				.append(BaseObject.floatToFormatString(getX()*prop, 5))
				.append("^")
				.append(BaseObject.floatToFormatString(getY()*2 * prop, 5))
				.append("^")
				.append(BaseObject.floatToFormatString(getXEnd() * prop, 5))
				.append("^")
				.append(BaseObject.floatToFormatString(getYEnd()*2 * prop, 5))
				.append("^")
				.append(BaseObject.intToFormatString(0, 1))
				.append("^")
				.append(BaseObject.boolToFormatString(mDragable, 3))
				.append("^")
				.append(BaseObject.intToFormatString(mBits, 3))
				.append("^")
				.append("000^000^000^000^")
				.append(BaseObject.intToFormatString(mStart, 8))
				.append("^")
				.append(BaseObject.intToFormatString(mEnd, 8))
				.append("^")
				.append(BaseObject.intToFormatString( mStepLen, 8))
				.append("^")
				.append(mCounterIndex)
				.append("^")
				.append((mParent == null ? "0000" : String.format("%03d", mParent.mIndex)) + "^0000^")
//				.append("0000^0000^")
				.append(mFont)
				.append("^000^000");

		String str = builder.toString();
		//str += BaseObject.intToFormatString(mIndex, 3)+"^";
//		str += mId+"^";
//		str += BaseObject.floatToFormatString(getX()*prop, 5)+"^";
//		str += BaseObject.floatToFormatString(getY()*2 * prop, 5)+"^";
//		str += BaseObject.floatToFormatString(getXEnd() * prop, 5)+"^";
//		str += BaseObject.floatToFormatString(getYEnd()*2 * prop, 5)+"^";
//		str += BaseObject.intToFormatString(0, 1)+"^";
//		str += BaseObject.boolToFormatString(mDragable, 3)+"^";
//		str += BaseObject.intToFormatString(mBits, 3)+"^";
//		str += "000^000^000^000^";
//		str += BaseObject.intToFormatString(mMax, 8)+"^";
//		str += BaseObject.intToFormatString(mMin, 8)+"^";
//		str += BaseObject.intToFormatString(Integer.parseInt(mContent) , 8)+"^";
//		str += "00000000^0000^0000^" + mFont + "^000^000";

		Debug.d(TAG, "toString = [" + str + "]");

//		System.out.println("counter string ["+str+"]");
		return str;
	}
//////add by lk 
	@Override	 
	public Bitmap getpreviewbmp()
	{
		Bitmap bitmap;
		mPaint.setTextSize(getfeed());
		mPaint.setAntiAlias(true); //  
		mPaint.setFilterBitmap(true); //
	
		boolean isCorrect = false;
		// Debug.d(TAG,"--->getBitmap font = " + mFont);
////		for (String font : mFonts) {
////			if (font.equals(mFont)) {
////				isCorrect = true;
////				break;
////			}
////		}
////		if (!isCorrect) {
////			mFont = DEFAULT_FONT;
////		}
		try {
			mPaint.setTypeface(FontCache.get(mContext, mFont));
		} catch (Exception e) {}
		
		int width = (int)mPaint.measureText(getContent());
		Debug.d(TAG, "--->content: " + getContent() + "  width=" + width);
		if (mWidth == 0) {
			setWidth(width);
		}
		bitmap = Bitmap.createBitmap(width , (int)mHeight, Configs.BITMAP_PRE_CONFIG);
		Debug.d(TAG,"--->getBitmap width="+mWidth+", mHeight="+mHeight);
		mCan = new Canvas(bitmap);
		FontMetrics fm = mPaint.getFontMetrics();
		mPaint.setColor(Color.BLUE);//���� ���� �� λͼ �� Ϊ ��ɫ 
	 
		String str_new_content = mContent;
		str_new_content =	str_new_content.replace('0', 'c');	
		str_new_content =	str_new_content.replace('1', 'c');
		str_new_content =	str_new_content.replace('2', 'c');	
		str_new_content =	str_new_content.replace('3', 'c');	
		str_new_content =	str_new_content.replace('4', 'c');	
		str_new_content =	str_new_content.replace('5', 'c');	
		str_new_content =	str_new_content.replace('6', 'c');	
		str_new_content =	str_new_content.replace('7', 'c');	
		str_new_content =	str_new_content.replace('8', 'c');	
		str_new_content =	str_new_content.replace('9', 'c');	
		
		
		mCan.drawText(str_new_content , 0, mHeight-fm.descent, mPaint);
	
		Bitmap result = Bitmap.createScaledBitmap(bitmap, (int)mWidth, (int)mHeight, false);
		BinFromBitmap.recyleBitmap(bitmap);
		return result;
	}	
}
