package com.printer.corelib;
// https://zhuanlan.zhihu.com/p/503679319
// https://www.jianshu.com/p/ad109dac0708
// https://blog.csdn.net/xiaoyifeishuang1/article/details/130345558

import android.util.Log;

@Deprecated
public class JarTest {
	private static final String TAG = JarTest.class.getSimpleName();

	public JarTest() {
	}
	
	public static int d(String tag, String log)	{
		Log.d(TAG, getLineNumber()+ " " + tag + ":" + log);
		return 0;
	}
	
	public static int i(String tag, String log)	{
		Log.i(TAG, getLineNumber()+ " " + tag + ":"+log);
		return 0;
	}
	
	public static int v(String tag, String log)	{
		Log.v(TAG, getLineNumber()+ " " + tag + ":"+log);
		return 0;
	}

	public static int e(String tag, String log) {
		Log.e(TAG, getLineNumber()+ " " + tag+":"+log);
		return 0;
	}

	private static String getLineNumber() {
		Exception e = new Exception();
		StackTraceElement[] trace =e.getStackTrace();
		if(trace==null||trace.length==0) 
			return "";
		int line = trace[2].getLineNumber();
		String file = trace[2].getFileName();
		return file + ":" + line;
	}
}
