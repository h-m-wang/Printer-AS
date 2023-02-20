package com.industry.printer.data;

import com.industry.printer.Utils.Configs;
import com.industry.printer.Utils.Debug;

import android.graphics.Bitmap;


/**
 * bitmap图片提取点阵的步骤如下：
 * 1、对图片进行灰度化处理
 * 2、根据设定的阈值进行二值化处理 （文字纠错，有人把阈值写成了阀值，呵呵）
 * 3、每个pixel占一个bit，重新组织成buffer数据
 * 
 * 注： 为了跟PC端保持一致，生成的bin文件宽度为1.tlk中坐标的四分之一，在提取点阵之前先对原始Bitmap进行X坐标缩放（为原图的1/4）
 * 	  然后进行灰度和二值化处理；
 * @author kevin
 *
 */
public class BinFromBitmap extends BinCreater {
	
	public int mDots[]= new int[8];

	public BinFromBitmap() {
		super();
		init();
	}
	
	private void init() {
		for (int i = 0; i < mDots.length; i++) {
			mDots[i] = 0;
		}
	}

// H.M.Wang 2023-2-19 事先生成一个具有一定空间的pixels数组，在实际使用时，如果需要的空间在此范围内就不再申请空间，而是直接使用这个预先申请的空间，目的是避免运行时频繁申请内存而导致系统启动内存清理(GC_)而额外消耗时间
	private static int[] pixels = new int[152 * 152];
// End of H.M.Wang 2023-2-19 事先生成一个具有一定空间的pixels数组，在实际使用时，如果需要的空间在此范围内就不再申请空间，而是直接使用这个预先申请的空间，目的是避免运行时频繁申请内存而导致系统启动内存清理(GC_)而额外消耗时间

	/**
	 * 这个函数没有对bmp原图进行高度缩放，所以，得到的buffer列高与原图高度一致
	 * 如果要处理列高比较大（如110点的列高）的原图最好对源bmp进行缩放，然后做点阵提取操作
	 * @param bmp
	 */
	@Override
	// H.M.Wang 追加一个是否移位的参数
	public int[] extract(Bitmap bmp, int head, boolean needShift) {
    	mWidth = bmp.getWidth();         
        mHeight = bmp.getHeight(); 
        mHeighEachHead = mHeight / head;

/*
		// H.M.Wang 注释掉该部分JAVA实现的二值化功能，改用JNI实现以提高处理速度

        // 计算每列占的字节数
        int colEach = mHeight%8==0?mHeight/8:mHeight/8+1;
        //Debug.d(TAG, "=====width="+mWidth+", height="+mHeight+", colEach="+colEach);
        Debug.d(TAG, "--->mHeighEachHead: " + mHeighEachHead + "   height= " + mHeight);
        mBinBits = new byte[colEach * mWidth];
        // 将bitmap的每个像素读取到pixels数组中，数组的每个元素对应一个像素值
//        bmp.getPixels(pixels, 0, mWidth, 0, 0, mWidth, mHeight); 
        //int alpha = 0x00 << 24;  

		Debug.d(TAG, "SaveTime: - Start 二值化(JAVA) : " + System.currentTimeMillis());
        // 逐列进行灰度化和二值化处理
        for(int i = 0; i < mHeight; i++)  { 
        	
            for(int j = 0; j < mWidth; j++) {
            	int grey = bmp.getPixel(j, i);
//                int grey = pixels[mWidth * i + j]; 
                
                int red = ((grey  & 0x00FF0000 ) >> 16) & 0x0ff; 
                int green = ((grey & 0x0000FF00) >> 8) & 0x0ff; 
                int blue = (grey & 0x000000FF);
                
                grey = (int)((float) red * 0.3 + (float)green * 0.59 + (float)blue * 0.11);
                // System.out.print("  " + grey);
                // pixels[mWidth * i + j] = grey>128? 0x0:0xffffff;
                
                if(grey > 220)
                	mBinBits[j*colEach+i/8] &= ~(0x01<<(i%8));
                else {
                	mBinBits[j*colEach+i/8] |= 0x01<<(i%8);
                	mDots[i/mHeighEachHead]++;
//                	Debug.d(TAG, "--->i = " + i + "  j = " + j  + "  mDots[" + (i/mHeighEachHead) + "]=" + mDots[i/mHeighEachHead] + "   mHeighEachHead = " + mHeighEachHead);
                }
            }
            // System.out.println();
        }
        for(int i=0; i<8; i++) {
			Debug.d(TAG, "mDots[JAVA, " + i + "] = " + mDots[i]);
		}

		Debug.d(TAG, "SaveTime: - End 二值化(JAVA) : " + System.currentTimeMillis());

*/
//		Debug.d(TAG, "SaveTime: - Start 二值化(JNI) : " + System.currentTimeMillis());

		// H.M.Wang 增加9行 25.4xn情况下断档和实现JNI的二值化
//		int[] pixels = new int[mWidth * mHeight];
// H.M.Wang 2023-2-19 事先生成一个具有一定空间的pixels数组，在实际使用时，如果需要的空间在此范围内就不再申请空间，而是直接使用这个预先申请的空间，目的是避免运行时频繁申请内存而导致系统启动内存清理(GC_FOR_ALLOC)而额外消耗时间
		if(mWidth * mHeight > pixels.length) pixels = new int[mWidth * mHeight];
// End of H.M.Wang 2023-2-19 事先生成一个具有一定空间的pixels数组，在实际使用时，如果需要的空间在此范围内就不再申请空间，而是直接使用这个预先申请的空间，目的是避免运行时频繁申请内存而导致系统启动内存清理(GC_FOR_ALLOC)而额外消耗时间

		bmp.getPixels(pixels, 0, mWidth, 0, 0, mWidth, mHeight);

		if(needShift) {
			pixels = NativeGraphicJni.ShiftImage(pixels, mWidth, mHeight, head, 308, 320);
		}

// H.M.Wang 2020-9-10 大字机5x5字体的时候，vbin的全白问题，原来的220阈值有点低，修改为240
		mBinBits = NativeGraphicJni.Binarize(pixels, mWidth, mHeight, head, 240);
// H.M.Wang 2020-9-10 大字机5x5字体的时候，...

		mDots = NativeGraphicJni.GetDots();

		// H.M.Wang 增加1行
//		bmp.recycle();

        return mDots; 
    }
	
	public static Bitmap Bin2Bitmap(byte []map)
    {
    	int k=0;
    	Bitmap bmp;
    	if (map == null || map.length < 16) {
    		return null;
		}
		// Debug.d(TAG, "map[0]="+map[0]+",map[1]="+map[1]+", map[2]="+map[2]);
		// Debug.d(TAG, "map[3]="+map[3]+",map[4]="+map[4]+", map[5]="+map[5]);
    	int grey =0;
    	int columns =  (map[0]&0xff) << 16 | (map[1] & 0xff)<<8 | (map[2]&0xff);
    	int row = (map[3]&0xff) << 16 | (map[4] & 0xff)<<8 | (map[5]&0xff);
    	int pixels[] = new int[columns*row];
    	Debug.d(TAG, "columns = "+columns+", row="+row);
    	/*110 bytes per Row*/
    	for(int i=0; i< columns; i++)
    	{
    		for(int j=0; j<row; j++)
    		{
    			if( (map[i*(row/8) + j/8+RESERVED_FOR_HEADER]&(0x01 <<(7-j%8))) != 0)
    				grey = 0x0;
    			else 
    				grey = 0xff;
    			pixels[j*columns+i] = 0xff<<24 | grey <<16 | grey<<8 | grey;
    		}
    	}
    	Debug.d(TAG, "===============");
    	bmp = Bitmap.createBitmap(pixels, columns, row, Configs.BITMAP_CONFIG);
    	Debug.d(TAG, "===============000000");
    	//bmp.setPixels(pixels, 0, columns, 0, 0, columns, row);
    	Debug.d(TAG, "===============111111");
    	return Bitmap.createScaledBitmap(bmp, columns, 150, true);
    }
    
	public static Bitmap Bin2Bitmap(char []map, int columns, int row)
    {
    	int k=0;
    	Bitmap bmp;
    	Debug.d(TAG, "--->length = " + map.length + " columns = " + columns + "  row = " + row);
		if (map == null || map.length < columns * row/16) {
			return null;
		}
    	int grey =0;
    	int pixels[] = new int[columns*row];
    	Debug.d(TAG, "columns = "+columns+", row="+row);
    	/*110 bytes per Row*/
    	for(int i=0; i< columns; i++)
    	{
    		for(int j=0; j<row; j++)
    		{
    			if( (map[i*(row/16) + j/16]&(0x01 <<(j%16))) != 0)
    				grey = 0x0;
    			else 
    				grey = 0xff;
    			pixels[j*columns+i] = 0xff<<24 | grey <<16 | grey<<8 | grey;
    		}
    	}
//    	Debug.d(TAG, "===============");
    	bmp = Bitmap.createBitmap(pixels, columns, row, Configs.BITMAP_CONFIG);
//    	Debug.d(TAG, "===============000000");
    	//bmp.setPixels(pixels, 0, columns, 0, 0, columns, row);
//    	Debug.d(TAG, "===============111111");
    	return bmp;
//		return Bitmap.createScaledBitmap(bmp, columns, 150, true);
    }

    public static void recyleBitmap(Bitmap bmp)
    {
    	if(!bmp.isRecycled())
		{
			bmp.recycle();
			System.gc();
		}
    }
}
