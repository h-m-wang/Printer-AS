package com.industry.printer.ui.Items;

import android.graphics.Bitmap;

public class PictureItem {

	private String mPath;
	private String mTitle;
// H.M.Wang 2026-9-6 修改读取和显示图片的逻辑，提高显示图片的效率
	private Bitmap mBitmap;
// End of H.M.Wang 2026-9-6 修改读取和显示图片的逻辑，提高显示图片的效率

	public PictureItem(String path, String title) {
		mPath = path;
		mTitle = title;
	}
	
	public String getPath() {
		return mPath;
	}
	
	public String getTitle() {
		return mTitle;
	}

// H.M.Wang 2026-9-6 修改读取和显示图片的逻辑，提高显示图片的效率
	public void setBitmap(Bitmap bmp) {
		mBitmap = bmp;
	}
	public Bitmap getBitmap() {
		return mBitmap;
	}
// End of H.M.Wang 2026-9-6 修改读取和显示图片的逻辑，提高显示图片的效率
}
