package com.industry.printer.object;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.industry.printer.FileFormat.SystemConfigFile;
import com.industry.printer.PHeader.PrinterNozzle;
import com.industry.printer.Utils.ConfigPath;
import com.industry.printer.Utils.Configs;
import com.industry.printer.Utils.Debug;
import com.industry.printer.cache.FontCache;
import com.industry.printer.data.BinFileMaker;
import com.industry.printer.data.BinFromBitmap;
import com.industry.printer.hardware.FpgaGpioOperation;

/* H.M.Wang 2022-11-25 修改mContent的管理方法
    当前的策略是保存最新的内容（可能是编辑页面返回的内容（也许是连续的#，也许是有DT的桶取来的内容），也可能是DT桶取来的内容），总而言之，内容是固定的。
    这样，在生成编辑页面的图片或者打印图片的时候，总是生成一样的图片。但是，如果内容的后部分是空格（位数大于实际输入的内容的个数），那么，编辑页面后面
    就看不出还有内容，操作人员容易将后续的变量与改DT重叠；但是如果后面简单的加#，虽然可以避免编辑页面的误操作，但是打印出来的话，会出来多余的#，不美观。
    因此，做如下修改：
    1. 这里保存的内容（mContent)均保存足位的#，setContent函数会给父类函数传递这个足位#的字符串，所有通过getContent获取内容的函数均会得到该结果
    2. 在生成打印图片的函数里（getPrintBitmap），忽略掉内部的mContent，而是从桶中获取相应的内容，如果这个内容不为空，则后面添加空格后作为打印内容使用；
       如果桶中无内容，则去掉添加的空格后为空，这时使用内部内容的足位#字符串作为打印内容
 */
public class DynamicText extends BaseObject {
    private static final String TAG = DynamicText.class.getSimpleName();

    private int mBits;
    private int mDtIndex;

    public DynamicText(Context ctx, float x) {
        super(ctx, OBJECT_TYPE_DYN_TEXT, x);
        mBits = 5;
        mDtIndex = 0;
        mContent = "#####";
    }

    public DynamicText(Context context, BaseObject parent, float x) {
        this(context, x);
        mParent = parent;
    }

    @Override
    public String getMeatureString() {
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<mBits; i++) {
            if(mContent.charAt(i) >= 0x00 && mContent.charAt(i) < 0x7F) {
                sb.append('#');
            } else {
                sb.append('中');
            }
        }
        return sb.toString();
    }

    private String getDefaultContent() {
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<mBits; i++) {
            sb.append('#');
        }
        return sb.toString();
    }

    private String getValidString(String cnt) {
        StringBuilder sb = new StringBuilder();
        sb.append(cnt);
        for(int i=0; i<mBits-cnt.length(); i++) {
            sb.append(' ');
        }
        return sb.toString().substring(0, mBits);
    }

    @Override
    public void setContent(String cnt) {
        super.setContent(getDefaultContent());
    }

    public void adjustWidth() {
        try {
            mPaint.setTypeface(FontCache.get(mContext, mFont));
        } catch (Exception e) {}
        mPaint.setTextSize(mHeight);
        setWidth(mPaint.measureText(getMeatureString()));
    }

    public void setBits(int n) {
        Debug.d(TAG, "setBits: [" + n + "]");
        mBits = n;
        setContent(getDefaultContent());
    }

    public int getBits()
    {
        return mBits;
    }

    public void setDtIndex(int n) {
        Debug.d(TAG, "setDtIndex: [" + n + "]");
        mDtIndex = n;
    }

    public int getDtIndex()
    {
        return mDtIndex;
    }

// H.M.Wang 2022-4-24 追加这个继承自父类的函数，用来在编辑页面也显示蓝色
    @Override
    protected void drawNormal() {
        // Debug.d(TAG, "--->drawNormal");
        mPaint.setColor(Color.BLUE);
        mBitmap = super.draw();     // 将父类的该函数从private修改为protected，否则无法调用
    }
// End of H.M.Wang 2022-4-24 追加这个继承自父类的函数，用来在编辑页面也显示蓝色

    @Override
    public Bitmap getpreviewbmp()
    {
        Bitmap bitmap;
        mPaint.setTextSize(getfeed());
        mPaint.setAntiAlias(true); //
        mPaint.setFilterBitmap(true); //

        try {
            mPaint.setTypeface(FontCache.get(mContext, mFont));
        } catch (Exception e) {}

        String prevString = getMeatureString();

        int width = (int)mPaint.measureText(prevString);
        Debug.d(TAG, "--->content: " + getContent() + "  width=" + width);
        if (mWidth == 0) {
            setWidth(width);
        }
        bitmap = Bitmap.createBitmap(width , (int)mHeight, Configs.BITMAP_PRE_CONFIG);
        Debug.d(TAG,"--->getBitmap width="+mWidth+", mHeight="+mHeight);
        mCan = new Canvas(bitmap);
        Paint.FontMetrics fm = mPaint.getFontMetrics();
        mPaint.setColor(Color.BLUE);

        mCan.drawText(prevString, 0, mHeight-fm.descent, mPaint);

        Bitmap result = Bitmap.createScaledBitmap(bitmap, (int)mWidth, (int)mHeight, false);
        BinFromBitmap.recyleBitmap(bitmap);
        return result;
    }

    public Bitmap getPrintBitmap(float scaledW, float scaledH, int dstHeight) {
        Debug.d(TAG,"getPrintBitmap scaledW = " + scaledW + ", scaledH = " + scaledH + ", dstHeight = " + dstHeight);
        Debug.d(TAG,"Original width = " + mWidth + ", height = " + mHeight + ", ratio = " + mRatio);
        Debug.d(TAG,"O.Index = " + mIndex + "; DT.Index = " + mDtIndex);

        Paint paint = new Paint();

        // 将内部高度调整为打印高度
        float drawY = getY() / scaledH;
        float drawHeight = mHeight / scaledH;

        paint.setTextSize(drawHeight);
        paint.setColor(Color.BLACK);
        paint.setAntiAlias(true); //去除锯齿
        paint.setFilterBitmap(true); //对位图进行滤波处理
        try {
            paint.setTypeface(FontCache.get(mContext, mFont));
        } catch (Exception e) {

        }

        // 将系统标准打印宽度调整为必要宽度，考虑人为的调整宽度(mRatio)和特种头的宽度调整(HP头宽度减半)
//        float ratio = mRatio * scaledH / scaledW;
//        int drawWidth = Math.round(paint.measureText(getContent()) * ratio);

        String cnt = getValidString(SystemConfigFile.getInstance().getDTBuffer(mDtIndex));
        if(cnt.trim().isEmpty()) {
            cnt = getContent();
        }

        int charWidth = (int)(paint.measureText(cnt));
        int drawWidth = (int)(mWidth / scaledW);
//        Debug.d(TAG,"drawWidth = " + drawWidth + ", charWidth = " + charWidth);

        PrinterNozzle head = mTask.getNozzle();

        if (head == PrinterNozzle.MESSAGE_TYPE_16_DOT ||
            head == PrinterNozzle.MESSAGE_TYPE_32_DOT ||
            head == PrinterNozzle.MESSAGE_TYPE_32DN ||
            head == PrinterNozzle.MESSAGE_TYPE_32SN ||
            head == PrinterNozzle.MESSAGE_TYPE_64SN ||
// H.M.Wang 2022-10-19 追加64SLANT头。
            head == PrinterNozzle.MESSAGE_TYPE_64SLANT ||
// End of H.M.Wang 2022-10-19 追加64SLANT头。
// H.M.Wang 2022-5-27 追加32x2头类型
            head == PrinterNozzle.MESSAGE_TYPE_32X2 ||
// End of H.M.Wang 2022-5-27 追加32x2头类型
// H.M.Wang 2021-8-16 追加96DN头
//            head == PrinterNozzle.MESSAGE_TYPE_64_DOT) {
            head == PrinterNozzle.MESSAGE_TYPE_64_DOT ||
            head == PrinterNozzle.MESSAGE_TYPE_96DN) {
// End of H.M.Wang 2021-8-16 追加96DN头
            paint.setTextScaleX(1.0f);
        } else {
            paint.setTextScaleX(1.0f * drawWidth / charWidth);
        }

        Paint.FontMetrics fm = paint.getFontMetrics();

        // 按调整了大小的高度进行正常图片绘制
        Bitmap drawBmp = Bitmap.createBitmap(drawWidth, dstHeight, Configs.BITMAP_CONFIG);
        Debug.d(TAG,"Draw [" + cnt + "] with [" + drawWidth + ", " + drawHeight + "]");
        Canvas drawCanvas = new Canvas(drawBmp);
        drawCanvas.drawColor(Color.WHITE);
        drawCanvas.drawText(cnt, 0, drawY + drawHeight - fm.descent, paint);

        return drawBmp;
    }

    @Override
    public void generateVarbinFromMatrix(String f) {
        BinFileMaker maker = new BinFileMaker(mContext);
        maker.extract("0123456789ABCDEFGHIJKLMNOPQRSTUV !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~~");
        maker.save(f + getVarBinFileName());
    }

    /**
     * 根據content生成變量的bin
     * 1、32点信息的bin统一不支持缩放
     * 2、7点和16点锁定字库
     * @param ctx 內容
     * @param scaleW	單個字符的實際寬度
     * @param scaleH	字符實際高度
     * @param dstH  背景圖高度
     * @return
     */
    @Override
    public int makeVarBin(Context ctx, float scaleW, float scaleH, int dstH) {
        int dots[] = new int[1];
        int singleW;
        Paint paint = new Paint();
        int height = Math.round(mHeight * scaleH);
        paint.setTextSize(height);
        paint.setAntiAlias(true); //去除锯齿
        paint.setFilterBitmap(true); //对位图进行滤波处理

        try {
            paint.setTypeface(FontCache.get(ctx, mFont));
        } catch (Exception e) {

        }

//		Debug.d(TAG, "SaveTime: - Start makeVarBin : " + System.currentTimeMillis());
        int width = Math.round(paint.measureText("0"));
        Paint.FontMetrics fm = paint.getFontMetrics();

		/*draw Bitmap of single digit*/
        Bitmap bmp = Bitmap.createBitmap(width, height, Configs.BITMAP_CONFIG);
        Canvas can = new Canvas(bmp);

        PrinterNozzle head = mTask.getNozzle();

        // H.M.Wang 修改下列两行
//		if (head == PrinterNozzle.MESSAGE_TYPE_16_DOT || head == PrinterNozzle.MESSAGE_TYPE_32_DOT) {
// H.M.Wang 2020-7-23 追加32DN打印头
//        if (head == PrinterNozzle.MESSAGE_TYPE_16_DOT || head == PrinterNozzle.MESSAGE_TYPE_32_DOT || head == PrinterNozzle.MESSAGE_TYPE_64_DOT) {
        if (head == PrinterNozzle.MESSAGE_TYPE_16_DOT ||
            head == PrinterNozzle.MESSAGE_TYPE_32_DOT ||
            head == PrinterNozzle.MESSAGE_TYPE_32DN ||
// H.M.Wang 2020-8-17 追加32SN打印头
            head == PrinterNozzle.MESSAGE_TYPE_32SN ||
// End of H.M.Wang 2020-8-17 追加32SN打印头
// H.M.Wang 2020-8-26 追加64SN打印头
            head == PrinterNozzle.MESSAGE_TYPE_64SN ||
// End of H.M.Wang 2020-8-26 追加64SN打印头
// H.M.Wang 2022-10-19 追加64SLANT头。
            head == PrinterNozzle.MESSAGE_TYPE_64SLANT ||
// End of H.M.Wang 2022-10-19 追加64SLANT头。
// H.M.Wang 2022-5-27 追加32x2头类型
            head == PrinterNozzle.MESSAGE_TYPE_32X2 ||
// End of H.M.Wang 2022-5-27 追加32x2头类型
// H.M.Wang 2021-8-16 追加96DN头
//            head == PrinterNozzle.MESSAGE_TYPE_64_DOT) {
            head == PrinterNozzle.MESSAGE_TYPE_64_DOT ||
            head == PrinterNozzle.MESSAGE_TYPE_96DN) {
// End of H.M.Wang 2021-8-16 追加96DN头
// End of H.M.Wang 2020-7-23 追加32DN打印头

            singleW = width;
        } else {
            singleW = Math.round(mWidth * scaleW/mContent.length());
        }

		/*draw 0-9 totally 10 digits Bitmap*/

        /** if message isn`t high resolution, divid by 2 because the buffer bitmap is halfed, so the variable buffer should be half too*/
        MessageObject msgObj = mTask.getMsgObject();
// H.M.Wang 2021-4-9 追加ioctl的分辨率信息获取命令
//        if (!msgObj.getResolution() ) {
        if (Configs.GetDpiVersion() != FpgaGpioOperation.DPI_VERSION_300) {
// End of H.M.Wang 2021-4-9 追加ioctl的分辨率信息获取命令
            singleW = singleW / msgObj.getPNozzle().getFactorScale();
        }

        Debug.d(TAG, "--->mWidth=" + mWidth + " ;scaleW=" + scaleW + " ;mContent.length()=" + mContent.length());

		/* 最終生成v.bin使用的bitmap */
        Bitmap gBmp = Bitmap.createBitmap(singleW*128, dstH, Configs.BITMAP_CONFIG);
        Canvas gCan = new Canvas(gBmp);

        gCan.drawColor(Color.WHITE);	/*white background*/

        String smpStr = "0123456789ABCDEFGHIJKLMNOPQRSTUV !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~~";

        for(int i=0; i<smpStr.length(); i++) {
            can.drawColor(Color.WHITE);
            can.drawText(smpStr.substring(i, i+1), 0, height - fm.descent, paint);
            gCan.drawBitmap(Bitmap.createScaledBitmap(bmp, singleW, height, false), i*singleW, Math.round(getY() * scaleH), paint);
        }

        BinFromBitmap.recyleBitmap(bmp);

        BinFileMaker maker = new BinFileMaker(mContext);

        // H.M.Wang 追加一个是否移位的参数。修改喷头数
        dots = maker.extract(Bitmap.createScaledBitmap(gBmp, gBmp.getWidth(), dstH, false), head.mHeads,
                (mTask.getNozzle() == PrinterNozzle.MESSAGE_TYPE_1_INCH ||
                        mTask.getNozzle() == PrinterNozzle.MESSAGE_TYPE_1_INCH_DUAL ||
                        mTask.getNozzle() == PrinterNozzle.MESSAGE_TYPE_1_INCH_TRIPLE ||
                        mTask.getNozzle() == PrinterNozzle.MESSAGE_TYPE_1_INCH_FOUR));

        Debug.d(TAG, "--->id: " + mId + " index:  " + mIndex);
        maker.save(ConfigPath.getVBinAbsolute(mTask.getName(), mIndex));
        //
        BinFromBitmap.recyleBitmap(gBmp);
		/*根據變量內容的實際長度計算點數*/
        dots[0] = (dots[0]* getContent().length()/10) + 1;

//		Debug.d(TAG, "SaveTime: - End makeVarBin : " + System.currentTimeMillis());
        return dots[0];
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(mId);
        float prop = getProportion();
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
                .append("^000^000^000^000")
                .append("^")
                .append("00000000^00000000^00000000")
                .append("^")
                .append(String.format("%03d", mDtIndex))
                .append("^0000^0000^")
                .append(mFont)
                .append("^000^")
                .append(mContent);

        String str = builder.toString();

        Debug.d(TAG, "toString = [" + str + "]");

        return str;
    }
}
