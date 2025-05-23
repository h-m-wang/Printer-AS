package com.industry.printer;

import com.industry.printer.PHeader.PrinterNozzle;
import com.industry.printer.Utils.Debug;
import com.industry.printer.data.BinCreater;
import com.industry.printer.data.BinFromBitmap;
import com.industry.printer.object.BaseObject;
import com.industry.printer.object.MessageObject;

import android.R.color;
import android.R.integer;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;

@Deprecated
public class EditScrollView extends View {
	
	public static final String TAG="EditScrollView";
	
	private int mScreenW;
	private int mScreenH;
	public Paint p;
	public HorizontalScrollView mParent;
	public Context mContext;
	public MessageTask mTask;
	/* 防止鍵盤彈出時刷新界面，導致文本寬度被設定爲默認寬度 */
	private boolean needDraw = false;
	
	public EditScrollView(Context context) {
		super(context);
		mContext = context;
		// TODO Auto-generated constructor stub
		p = new Paint();
		mParent= (HorizontalScrollView) this.getParent();
		getWindowPixels();
		Debug.d(TAG, "==>EditScrollView 1");
	}
	
	public EditScrollView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		mContext = context;
		//mParent= (HorizontalScrollView) this.getParent().getParent();
		p = new Paint();
		getWindowPixels();
		Debug.d(TAG, "==>EditScrollView 2");
	}
	public EditScrollView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs,defStyle);
		//mParent= (HorizontalScrollView) this.getParent().getParent();
		p = new Paint();
		getWindowPixels();
		Debug.d(TAG, "==>EditScrollView 3");
	}
	@Override  
	protected void onDraw(Canvas canvas) {
		Debug.d(TAG, "====>onDraw needDraw = " + needDraw);
		if (!needDraw) {
			return;
		}
		needDraw = false;
		int scrollx = 0;
		if (mParent != null) {
			scrollx = mParent.getScrollX();
		}
		MessageObject msgObject = mTask.getMsgObject();
		if (msgObject != null) {
			p.setColor(Color.GRAY);
			PrinterNozzle type = msgObject.getPNozzle();
			switch (type) {
				case MESSAGE_TYPE_12_7:
				case MESSAGE_TYPE_1_INCH:
// H.M.Wang 2022-4-29 追加25.4x10头类型
				case MESSAGE_TYPE_254X10:
// End of H.M.Wang 2022-4-29 追加25.4x10头类型
				case MESSAGE_TYPE_16_DOT:
				case MESSAGE_TYPE_32_DOT:
// H.M.Wang 2020-7-23 追加32DN打印头
				case MESSAGE_TYPE_32DN:
// End of H.M.Wang 2020-7-23 追加32DN打印头
// H.M.Wang 2020-8-18 追加32SN打印头
				case MESSAGE_TYPE_32SN:
// End of H.M.Wang 2020-8-18 追加32SN打印头

// H.M.Wang 2022-5-27 追加32x2头类型
				case MESSAGE_TYPE_32X2:
// End of H.M.Wang 2022-5-27 追加32x2头类型
				// H.M.Wang 追加下列一行
				case MESSAGE_TYPE_64_DOT:
// H.M.Wang 2020-8-26 追加64SN打印头
				case MESSAGE_TYPE_64SN:
// End of H.M.Wang 2020-8-26 追加64SN打印头
// H.M.Wang 2022-10-19 追加64SLANT头
				case MESSAGE_TYPE_64SLANT:
// End of H.M.Wang 2022-10-19 追加64SLANT头
// H.M.Wang 2024-4-29 追加64_DOT_ONE喷头类型
				case MESSAGE_TYPE_64DOTONE:
// End of H.M.Wang 2024-4-29 追加64_DOT_ONE喷头类型
// H.M.Wang 2024-9-10 增加一个16DOTX4头类型，
				case MESSAGE_TYPE_16DOTX4:
// End of H.M.Wang 2024-9-10 增加一个16DOTX4头类型，
// H.M.Wang 2023-7-29 追加48点头
				case MESSAGE_TYPE_48_DOT:
// End of H.M.Wang 2023-7-29 追加48点头
// H.M.Wang 2021-8-16 追加96DN头
				case MESSAGE_TYPE_96DN:
// End of H.M.Wang 2021-8-16 追加96DN头
// H.M.Wang 2024-3-11 追加hp22mm打印头，以生成1056点高的打印image
				case MESSAGE_TYPE_22MM:
// End of H.M.Wang 2024-3-11 追加hp22mm打印头，以生成1056点高的打印image
// H.M.Wang 2025-1-19 增加22mmx2打印头类型
				case MESSAGE_TYPE_22MMX2:
// End of H.M.Wang 2025-1-19 增加22mmx2打印头类型
					break;
				case MESSAGE_TYPE_25_4:
				case MESSAGE_TYPE_1_INCH_DUAL:
					canvas.drawLine(0, 76, 5000, 76, p);
					break;
				case MESSAGE_TYPE_38_1:

				// H.M.Wang 追加下列一行
				case MESSAGE_TYPE_1_INCH_TRIPLE:

					canvas.drawLine(0, 50, 5000, 50, p);
					canvas.drawLine(0, 101, 5000, 101, p);
					break;
				case MESSAGE_TYPE_50_8:

				// H.M.Wang 追加下列一行
				case MESSAGE_TYPE_1_INCH_FOUR:

					canvas.drawLine(0, 38, 5000, 38, p);
					canvas.drawLine(0, 76, 5000, 76, p);
					canvas.drawLine(0, 114, 5000, 114, p);
					break;
			default:
				break;
			}
		}
		// p.setColor(0x000000);
		
		Debug.d(TAG, "--->scrollx: " + scrollx + ",  mScreenW: " + mScreenW);
		for(BaseObject obj : mTask.getObjects())
		{
			Debug.d(TAG, "index=" + obj.getIndex() + "  c: " + obj.getContent());
			/* 只有当cursor选中时才显示十字标线  */
			if(obj instanceof MessageObject) {
				if (!obj.getSelected()) {
					continue;
				}
				
				float[] points = {
					/* 画水平线 */
					0, obj.getY(), 5000, obj.getY(),
					/* 画垂直线 */
					obj.getX(), 0, obj.getX(), 280
				};
				p.setColor(Color.BLUE);
				p.setStrokeWidth(1);
				canvas.drawLines(points, p);
				continue;
			} 
			/* 不在显示区域内的对象可以不画，优化效率  */
//			if ((obj.getXEnd() < getScrollX()) || (obj.getX() > getScrollX() + mScreenW)) {
//				Debug.d(TAG, "index=" + obj.getIndex() + "  c: " + obj.getContent() + "  x=" + obj.getX() + " end=" + obj.getXEnd());
//				Debug.d(TAG, "scrollx=" + getScrollX());
//				continue;
//			}
			if(mContext == null)
				Debug.d(TAG, "$$$$$$$$$$context=null");
			
			Bitmap bitmap = obj.getScaledBitmap(mContext);
			if (bitmap == null) {
				Debug.d(TAG, "--->obj: " + obj.getContent());
				continue;
			}
			// canvas.drawBitmap(bitmap, obj.getX(), obj.getY(), p);
			dispImg(canvas, bitmap, obj.getX(), obj.getY());
			if (obj.getSelected()) {
				p.setStyle(Style.STROKE);
				canvas.drawRect(new RectF(obj.getX(), obj.getY(), obj.getXEnd(), obj.getYEnd()), p);
			}
			
		}
		// p.setColor(Color.BLACK);
		// canvas.drawLine(0, 153, getW(), 153, p);
		Debug.d(TAG, "<<<==onDraw");
		 //mParent.fling(100);
	} 
	
	private void dispImg(Canvas canvas, Bitmap bmp, float x, float y) {
		int end = 0, cutWidth = 0;
		for (;;) {
			if (end + 2000 > bmp.getWidth()) {
				cutWidth = bmp.getWidth() - end;
			} else {
				cutWidth =1200;
			}
			Bitmap child = Bitmap.createBitmap(bmp, end, 0, cutWidth, bmp.getHeight());
			canvas.drawBitmap(child, x+end, y, p);
			end += cutWidth;
			if (end >= bmp.getWidth() - 1) {
				break;
			}
		}
		
	}
	
	public void setParent(View view) {
		mParent = (HorizontalScrollView) view;
	}
	
	public void setTask(MessageTask task) {
		mTask = task;
	}

	private int getW() {
		DisplayMetrics metrics = getResources().getDisplayMetrics();
		float density = metrics.density;
		return (int) (5000 * density);
	}
	
	public void getWindowPixels() {
		DisplayMetrics metrics = getResources().getDisplayMetrics();
		float density = metrics.density;
		float w = metrics.widthPixels;
		float h = metrics.heightPixels;
		
		// mScreenW = (int) (w * density + 0.5f);
		// mScreenH = (int) (h * density + 0.5f);
		mScreenW = (int) (w + 0.5f);
	}
	
	public void beginDraw() {
		needDraw = true;
	}
	
	public void endDraw() {
		needDraw = false;
	}
}
