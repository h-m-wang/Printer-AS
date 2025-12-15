package com.industry.printer.object;

import com.industry.printer.FileFormat.SystemConfigFile;
import com.industry.printer.PHeader.PrinterNozzle;
import com.industry.printer.R;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.Configs;
import com.industry.printer.cache.FontCache;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.Paint.FontMetrics;
import android.widget.EditText;

public class TextObject extends BaseObject {
	private static final String TAG = TextObject.class.getSimpleName();

// H.M.Wang 2025-10-26 追加字间距标识
	protected int mLetterSpacing;
// End of H.M.Wang 2025-10-26 追加字间距标识
	// H.M.Wang 追加时间对象的所属信息
//	public BaseObject mParent;

	public TextObject(Context context, float x) {
		super( context, BaseObject.OBJECT_TYPE_TEXT, x);
// H.M.Wang 2025-10-26 追加字间距标识
		mLetterSpacing = 0;
// End of H.M.Wang 2025-10-26 追加字间距标识
//		mParent = null;
	}

	public TextObject(Context context, BaseObject parent, float x) {
		this(context, x);
		mParent = parent;
	}

// H.M.Wang 2025-10-26 追加字间距标识
	public void setLetterSpacing(String val) {
		PrinterNozzle type = SystemConfigFile.getInstance(mContext).getPNozzle();	// TLKFileParser在调用这个函数的时候，mTask还没有设置，因此不能从mTask中获取Nozzle类型，本来mTask中的类型也是跟随SystemConfigFile中的，所以就直接从这里获取比较稳妥
		Debug.d(TAG, "type = " + type);
// H.M.Wang 2025-12-12 将大字机的判断集中到类rinterNozzle中
		if(type.isBigdotType()) {
/*		if (type == PrinterNozzle.MESSAGE_TYPE_16_DOT ||
			type == PrinterNozzle.MESSAGE_TYPE_32_DOT ||
			type == PrinterNozzle.MESSAGE_TYPE_32DN ||
			type == PrinterNozzle.MESSAGE_TYPE_32SN ||
			type == PrinterNozzle.MESSAGE_TYPE_64SN ||
// H.M.Wang 2022-10-19 追加64SLANT头。
			type == PrinterNozzle.MESSAGE_TYPE_64SLANT ||
// End of H.M.Wang 2022-10-19 追加64SLANT头。
// H.M.Wang 2024-4-29 追加64_DOT_ONE喷头类型
			type == PrinterNozzle.MESSAGE_TYPE_64DOTONE ||
// End of H.M.Wang 2024-4-29 追加64_DOT_ONE喷头类型
// H.M.Wang 2024-9-10 增加一个16DOTX4头类型，
			type == PrinterNozzle.MESSAGE_TYPE_16DOTX4 ||
// End of H.M.Wang 2024-9-10 增加一个16DOTX4头类型，
// H.M.Wang 2022-5-27 追加32x2头类型
			type == PrinterNozzle.MESSAGE_TYPE_32X2 ||
// End of H.M.Wang 2022-5-27 追加32x2头类型
// H.M.Wang 2021-8-16 追加96DN头
//			type == PrinterNozzle.MESSAGE_TYPE_64_DOT) {
			type == PrinterNozzle.MESSAGE_TYPE_64_DOT ||
// H.M.Wang 2023-7-29 追加48点头
			type == PrinterNozzle.MESSAGE_TYPE_48_DOT ||
// End of H.M.Wang 2023-7-29 追加48点头
			type == PrinterNozzle.MESSAGE_TYPE_96DN) {
// End of H.M.Wang 2021-8-16 追加96DN头
// End of H.M.Wang 2020-7-23 追加32DN打印头
*/
// End of H.M.Wang 2025-12-12 将大字机的判断集中到类rinterNozzle中
			try {
				mLetterSpacing = Integer.valueOf(val);
			} catch(NumberFormatException e){}
		}
	}

	public int getLetterSpacing() {
		return mLetterSpacing;
	}
// End of H.M.Wang 2025-10-26 追加字间距标识

	@Override
	public void setContent(String content) {
// H.M.Wang 2020-4-20 在内容发生变化的情况下，设置新的内容，并且重新计算宽度，是保持比例
// H.M.Wang 2025-10-26 追加字间距标识
		if(mLetterSpacing > 0 && content.indexOf("¡") < 0) {
			StringBuilder sp = new StringBuilder();
			for(int i=0; i<mLetterSpacing; i++) {
				sp.append("¡");
			}
			StringBuilder sb = new StringBuilder();
			for(int i=0; i<content.length(); i++) {
				sb.append(content.charAt(i));
				sb.append(sp);
			}
			content = sb.toString();
		}
// End of H.M.Wang 2025-10-26 追加字间距标识
Debug.d(TAG, "mLetterSpacing: [" +  mLetterSpacing + "]; Content: [" + content + "]");
		if(mContent != null && !mContent.equals(content)) {
			super.setContent(content);
			meature();
		}
// End of H.M.Wang 2020-4-20 在内容发生变化的情况下，设置新的内容，并且重新计算宽度，是保持比例
// H.M.Wang 2020-4-19 不重新计算就会出现即使字数发生变化，总宽度不变的问题（如果原来是5个字，修改为1个字的话，后面的1个字会占原来的5个字的宽度
// 如果重新计算，如果对高度进行过手动调整，但宽度不调整的话，字数发生变化后，宽度也会按着高度的比率发生变化
// 总而言之，这里原来的逻辑有缺陷，最好的办法是再次确认需求之后，重新定义这里的逻辑，否则，逻辑互相牵制，无法达到最佳状态
//		meature();

		// H.M.Wang取消宽度归零的设置。如果有这行，用户对高度进行缩小以后，设置属性重新计算宽度，此时宽度也会变小
//		mWidth = 0;
	}
/*
	// H.M.Wang 修改。取消原来的子元素均等加减1的缩放方法，改为均等缩放
	public void wide() {
		float ratio = (getWidth() + 5) / getWidth();
		mRatio *= ratio;

		setWidth(getWidth()*ratio);
		isNeedRedraw = true;
	}

	// H.M.Wang 修改。取消原来的子元素均等加减1的缩放方法，改为均等缩放
	public void narrow() {
		float ratio = (getWidth() - 5) / getWidth();
		mRatio *= ratio;

        setWidth(getWidth()*ratio);
		isNeedRedraw = true;
	}
*/
	public String toString()
	{
		float prop = getProportion();
		StringBuilder builder = new StringBuilder(mId);
		
		builder.append("^")
				.append(BaseObject.floatToFormatString(getX()*2 * prop, 5))
				.append("^")
				.append(BaseObject.floatToFormatString(getY()*2 * prop, 5))
				.append("^")
				.append(BaseObject.floatToFormatString(getXEnd()*2 * prop, 5))
				.append("^")
				.append(BaseObject.floatToFormatString(getYEnd()*2 * prop, 5))
				.append("^")
				.append(BaseObject.intToFormatString(0, 1))
				.append("^")
				.append(BaseObject.boolToFormatString(mDragable, 3))
				.append("^")
				.append(BaseObject.intToFormatString(mContent.length(), 3))
				.append("^")
// H.M.Wang 2019-9-24 追加所属信息
				.append("000^000^000^000^00000000^00000000^00000000^00000000^" + (mParent == null ? "0000" : String.format("%03d", mParent.mIndex)) + "^0000^")
//				.append("000^000^000^000^00000000^00000000^00000000^00000000^0000^0000^")
				.append(mFont)
// H.M.Wang 2025-10-26 追加字间距标识
//				.append("^000^")
				.append("^")
				.append(mLetterSpacing)
				.append("^")
//				.append(mContent);
				.append(mContent.replaceAll("¡", ""));
// End of H.M.Wang 2025-10-26 追加字间距标识

		String str = builder.toString();
//		String str="";
//		//str += BaseObject.intToFormatString(mIndex, 3)+"^";
//		str += mId+"^";
//		str += BaseObject.floatToFormatString(getX()*2 * prop, 5)+"^";
//		str += BaseObject.floatToFormatString(getY()*2 * prop, 5)+"^";
//		str += BaseObject.floatToFormatString(getXEnd()*2 * prop, 5)+"^";
//		//str += BaseObject.floatToFormatString(getY() + (getYEnd()-getY())*2, 5)+"^";
//		str += BaseObject.floatToFormatString(getYEnd()*2 * prop, 5)+"^";
//		str += BaseObject.intToFormatString(0, 1)+"^";
//		str += BaseObject.boolToFormatString(mDragable, 3)+"^";
//		str += BaseObject.intToFormatString(mContent.length(), 3)+"^";
//		str += "000^000^000^000^00000000^00000000^00000000^00000000^0000^0000^" + mFont + "^000^"+mContent;
		Debug.d(TAG, "toString = [" + str + "]");
//		System.out.println("file string ["+str+"]");
		return str;
	}
//////addbylk 
	@Override	 
	public Bitmap getpreviewbmp()
	{	Debug.e(TAG, "1===== " + getContent() );
		Bitmap bitmap;
		mPaint.setTextSize(getfeed());
		mPaint.setAntiAlias(true);   
		mPaint.setFilterBitmap(true); 
	
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
		Debug.d(TAG, "2===== " + getContent() );
		bitmap = Bitmap.createBitmap(width , (int)mHeight, Configs.BITMAP_PRE_CONFIG);
		Debug.d(TAG,"--->getBitmap width="+mWidth+", mHeight="+mHeight);
		mCan = new Canvas(bitmap);
		FontMetrics fm = mPaint.getFontMetrics();
		Debug.d(TAG, "3===== " + getContent() );
		
				
		mCan.drawText(mContent , 0, mHeight-fm.descent, mPaint);
	
		return Bitmap.createScaledBitmap(bitmap, (int)mWidth, (int)mHeight, false);	
	}	
}
