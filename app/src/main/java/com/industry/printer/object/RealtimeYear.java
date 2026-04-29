package com.industry.printer.object;

import java.util.Calendar;

import com.industry.printer.FileFormat.SystemConfigFile;
import com.industry.printer.Utils.Debug;
import com.industry.printer.cache.FontCache;
import com.industry.printer.Utils.Configs;
/*
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
*/

import android.R.color;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.FontMetrics;
import android.graphics.Typeface;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;

import android.graphics.Typeface;
import android.graphics.Paint.FontMetrics;
import android.text.format.Time;
import android.util.Log;

public class RealtimeYear extends BaseObject {

	public static final String TAG="RealtimeYear";
	public String mFormat;
//	public int mOffset;
//	public BaseObject mParent;
	
	public RealtimeYear(Context context, float x, boolean f) {
		super(context, BaseObject.OBJECT_TYPE_DL_YEAR, x);
		mOffset = 0;
//		mParent = null;
		Time t = new Time();
		t.set(System.currentTimeMillis());
		if(!f)
		{
			mFormat="YY";
			setContent(BaseObject.intToFormatString(t.year%100, 2));
		}
		else if(f)
		{
			mFormat="YYYY";
			setContent(BaseObject.intToFormatString(t.year, 4));
		}
		Debug.d(TAG, ">>Format, "+mFormat);
	}
	
	public RealtimeYear(Context context, BaseObject parent, float x, boolean f) {
		this(context, x ,f);
		mParent = parent;
	}

	@Override
	public String getContent()
	{
		if (mParent != null) {
			mOffset = mParent.getOffset();
		}
		Time t = new Time();
		
		t.set(System.currentTimeMillis() + mOffset * RealtimeObject.MS_DAY - timeDelay());
		
		if(mFormat.length()==2)
			setContent(BaseObject.intToFormatString(t.year%100, 2));
		else if(mFormat.length()==4)
			setContent(BaseObject.intToFormatString(t.year, 4));
///./...		Debug.d(TAG, ">>getContent: "+mContent);
		return mContent;
	}

	@Override
	public String getMeatureString() {
// 如果取00为标准字符计算宽度，在生成预览画面的时候可能会出现字符被切掉的问题
//		return (mFormat.equals("YY") ? "00" : (mFormat.equals("YYYY") ? "0000" : ""));
		return mFormat;
	}

	public String toString()
	{
		float prop = getProportion();
		StringBuilder builder = new StringBuilder(mId);
		builder.append("^")
// H.M.Wang 2026-4-25 在原坐标位置仍然保存整数x,y,xend,yend，在末尾增加浮点数坐标的保存，因为如果直接保存浮点数，以前版本的apk将无法将其读入，因为只识别整数
//				.append(BaseObject.floatToFormatString(getX()*prop, 5))
//				.append("^")
//				.append(BaseObject.floatToFormatString(getY()*2 * prop, 5))
//				.append("^")
//				.append(BaseObject.floatToFormatString(getXEnd() * prop, 5))
//				.append("^")
//				.append(BaseObject.floatToFormatString(getYEnd()*2 * prop, 5))
				.append(BaseObject.intToFormatString(Math.round(getX()*prop), 5))		// Tag 2
				.append("^")
				.append(BaseObject.intToFormatString(Math.round(getY()*2*prop), 5))		// Tag 3
				.append("^")
				.append(BaseObject.intToFormatString(Math.round(getXEnd()*prop), 5))		// Tag 4
				.append("^")
				.append(BaseObject.intToFormatString(Math.round(getYEnd()*2*prop), 5))		// Tag 5
// End of H.M.Wang 2026-4-25 在原坐标位置仍然保存整数x,y,xend,yend，在末尾增加浮点数坐标的保存，因为如果直接保存浮点数，以前版本的apk将无法将其读入，因为只识别整数
				.append("^")
				.append(BaseObject.intToFormatString(0, 1))
				.append("^")
				.append(BaseObject.boolToFormatString(mDragable, 3))
				.append("^")
				.append("000^000^000^000^000^")
				.append(mParent == null? "00000":BaseObject.intToFormatString(mParent.getOffset(), 5))
// H.M.Wang 2019-9-24 追加所属信息
				.append("^00000000^00000000^00000000^" + (mParent == null ? "0000" : String.format("%03d", mParent.mIndex)) + "^0000^")
//				.append("^00000000^00000000^00000000^0000^0000^")
				.append(mFont)
				.append("^000^000")
// H.M.Wang 2026-4-25 在原坐标位置仍然保存整数x,y,xend,yend，在末尾增加浮点数坐标的保存，因为如果直接保存浮点数，以前版本的apk将无法将其读入，因为只识别整数
				.append("^")
				.append(BaseObject.floatToFormatString(getX()*prop, 4))		// Tag 22
				.append("^")
				.append(BaseObject.floatToFormatString(getY()*2*prop, 4))		// Tag 23
				.append("^")
				.append(BaseObject.floatToFormatString(getXEnd()*prop, 4))		// Tag 24
				.append("^")
				.append(BaseObject.floatToFormatString(getYEnd()*2*prop, 4));		// Tag 25
// End op H.M.Wang 2026-4-25 在原坐标位置仍然保存整数x,y,xend,yend，在末尾增加浮点数坐标的保存，因为如果直接保存浮点数，以前版本的apk将无法将其读入，因为只识别整数

		String str = builder.toString();
		//str += BaseObject.intToFormatString(mIndex, 3)+"^";
//		str += mId+"^";
//		str += BaseObject.floatToFormatString(getX() * prop, 5)+"^";
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
//		Debug.d(TAG, "file string ["+str+"]");
		return str;
	}
//////addbylk 
	@Override	 
	public Bitmap getpreviewbmp()
	{
		Debug.d(TAG, "1===== " + getContent() );
		Bitmap bitmap;
	    Paint Paint; 
		Paint = new Paint();
		Paint.setTextSize(getfeed());
		Paint.setAntiAlias(true);//.setAntiAlias(true);
		Paint.setFilterBitmap(true); 
	
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
			Paint.setTypeface(FontCache.get(mContext, mFont));
		} catch (Exception e) {}

		String str_new_content="";
		str_new_content =	mContent;	
		
		str_new_content =	str_new_content.replace('0', 'Y');		
		str_new_content =	str_new_content.replace('1', 'Y');	
		str_new_content =	str_new_content.replace('2', 'Y');	
		str_new_content =	str_new_content.replace('3', 'Y');	
		str_new_content =	str_new_content.replace('4', 'Y');	
		str_new_content =	str_new_content.replace('5', 'Y');	
		str_new_content =	str_new_content.replace('6', 'Y');	
		str_new_content =	str_new_content.replace('7', 'Y');	
		str_new_content =	str_new_content.replace('8', 'Y');	
		str_new_content =	str_new_content.replace('9', 'Y');	
		//Debug.e(TAG, "--->content: " + getContent() + "  width=" + width);

		int width = (int)Paint.measureText(str_new_content);
		Debug.d(TAG, "--->content: " + getContent() + "  width=" + width);

		bitmap = Bitmap.createBitmap(width , (int)mHeight, Configs.BITMAP_PRE_CONFIG);
		Debug.d(TAG,"--->getBitmap width="+width+", mHeight="+mHeight);

		Canvas can = new Canvas(bitmap);
		FontMetrics fm = Paint.getFontMetrics();

		Paint.setColor(Color.BLUE);

		can.drawText(str_new_content , 0, mHeight-fm.descent, Paint);

		return Bitmap.createScaledBitmap(bitmap, (int)mWidth, (int)mHeight, false);
	}
}
