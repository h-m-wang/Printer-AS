package com.industry.printer.object;

import java.util.Calendar;

import com.industry.printer.FileFormat.SystemConfigFile;
import com.industry.printer.Utils.Configs;
import com.industry.printer.Utils.Debug;
import com.industry.printer.cache.FontCache;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.Paint.FontMetrics;
import android.os.SystemClock;
import android.text.format.Time;

public class RealtimeDate extends BaseObject {

	private static final String TAG = RealtimeDate.class.getSimpleName();

	public String mFormat;
//	public int mOffset;
//	public BaseObject mParent;
	
	public RealtimeDate(Context context, float x) {
		super(context, BaseObject.OBJECT_TYPE_DL_DATE, x);
		mFormat = "DD";
		Time t = new Time();
		t.set(System.currentTimeMillis());
		mOffset = 0;
//		mParent = null;
		setContent(BaseObject.intToFormatString(t.monthDay, 2));
	}

	public RealtimeDate(Context context, BaseObject parent, float x) {
		this(context, x);
		mParent = parent;
	}

// H.M.Wang 2020-11-13 追加这个函数，目的是提供一个内容是否变化的模板，当日，时和分有变化时重新生成打印缓冲区
	@Override
	public boolean contentChanged() {
		if (mParent != null) {
			mOffset = mParent.getOffset();
		}
		Time t = new Time();

		t.set(System.currentTimeMillis() + mOffset * RealtimeObject.MS_DAY - timeDelay());

		if(!mContent.equals(BaseObject.intToFormatString(t.monthDay, 2))) {
			Debug.d(TAG, "Day changed.");
		}

		return !mContent.equals(BaseObject.intToFormatString(t.monthDay, 2));
	}
// End of H.M.Wang 2020-11-13 追加这个函数，目的是提供一个内容是否变化的模板，当日，时和分有变化时重新生成打印缓冲区

	@Override
	public String getContent()
	{
		if (mParent != null) {
			mOffset = mParent.getOffset();
		}
		Time t = new Time();
		
		t.set(System.currentTimeMillis() + mOffset * RealtimeObject.MS_DAY - timeDelay());
///./...		Debug.d(TAG, "--->Date: " + t.monthDay);
		setContent(BaseObject.intToFormatString(t.monthDay, 2));
///./...		Debug.d(TAG, ">>getContent: " + mContent);
		return mContent;
	}

	@Override
	public String getMeatureString() {
// 如果取00为标准字符计算宽度，在生成预览画面的时候可能会出现字符被切掉的问题
//		return "00";
		return mFormat;
	}

	public String toString()
	{
		float prop = getProportion();
		StringBuilder builder = new StringBuilder(mId);
		
		builder.append("^")
				.append(BaseObject.floatToFormatString(getX() * prop, 5))
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
				.append("000^000^000^000^000^")
				.append(mParent == null? "00000":BaseObject.intToFormatString(mParent.getOffset(), 5))
				.append("^")
// H.M.Wang 2019-9-24 追加所属信息
				.append("00000000^00000000^00000000^" + (mParent == null ? "0000" : String.format("%03d", mParent.mIndex)) + "^0000^")
//				.append("00000000^00000000^00000000^0000^0000^")
				.append(mFont)
				.append("^000^000");
		String str = builder.toString();
		//str += BaseObject.intToFormatString(mIndex, 3)+"^";
//		str += mId+"^";
//		str += BaseObject.floatToFormatString(getX() * prop, 5)+"^";
//		
//		str += BaseObject.floatToFormatString(getY()*2 * prop, 5)+"^";
//		str += BaseObject.floatToFormatString(getXEnd() * prop, 5)+"^";
//		//str += BaseObject.floatToFormatString(getY() + (getYEnd()-getY())*2, 5)+"^";
//		str += BaseObject.floatToFormatString(getYEnd()*2 * prop, 5)+"^";
//		str += BaseObject.intToFormatString(0, 1)+"^";
//		str += BaseObject.boolToFormatString(mDragable, 3)+"^";
//		//str += BaseObject.intToFormatString(mContent.length(), 3)+"^";
//		str += "000^000^000^000^000^";
//		str += mParent == null? "00000":BaseObject.intToFormatString(mParent.getOffset(), 5);
//		str += "^00000000^00000000^00000000^0000^0000^" + mFont + "^000^000";
		Debug.d(TAG, "toString = [" + str + "]");
//		System.out.println("file string ["+str+"]");
		return str;
	}
	
	public void getBgBitmap()
	{ 
	}
//////addbylk 
	@Override	 
	public Bitmap getpreviewbmp()
	{
		Debug.d(TAG, "1===== " + getContent() );
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

		String str_new_content="";
		str_new_content =	mContent;	
		
		str_new_content =	str_new_content.replace('0', 'D');		
		str_new_content =	str_new_content.replace('1', 'D');	
		str_new_content =	str_new_content.replace('2', 'D');	
		str_new_content =	str_new_content.replace('3', 'D');	
		str_new_content =	str_new_content.replace('4', 'D');	
		str_new_content =	str_new_content.replace('5', 'D');	
		str_new_content =	str_new_content.replace('6', 'D');	
		str_new_content =	str_new_content.replace('7', 'D');	
		str_new_content =	str_new_content.replace('8', 'D');	
		str_new_content =	str_new_content.replace('9', 'D');	
		// Debug.e(TAG, "--->content: " + getContent() + "  width=" + width);

		int width = (int)mPaint.measureText(str_new_content);//addbylk
		Debug.d(TAG, "--->content: " + getContent() + "  width=" + width);

		bitmap = Bitmap.createBitmap(width , (int)mHeight, Configs.BITMAP_PRE_CONFIG);
		Debug.d(TAG,"--->getBitmap width="+width+", mHeight="+mHeight);
		mCan = new Canvas(bitmap);
		FontMetrics fm = mPaint.getFontMetrics();
		mPaint.setColor(Color.BLUE);

		mCan.drawText(str_new_content , 0, mHeight-fm.descent, mPaint);

		return Bitmap.createScaledBitmap(bitmap, (int)mWidth, (int)mHeight, false);
	}
	
}
