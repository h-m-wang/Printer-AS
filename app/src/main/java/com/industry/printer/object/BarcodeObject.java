package com.industry.printer.object;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Dimension;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.datamatrix.DataMatrixWriter;
import com.google.zxing.datamatrix.encoder.SymbolShapeHint;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.ByteMatrix;
import com.google.zxing.qrcode.encoder.Encoder;
import com.google.zxing.qrcode.encoder.QRCode;
import com.industry.printer.FileFormat.SystemConfigFile;
import com.industry.printer.MainActivity;
import com.industry.printer.PHeader.PrinterNozzle;
import com.industry.printer.Utils.Configs;
import com.industry.printer.Utils.Debug;
import com.industry.printer.data.BarcodeGeneratorJNI;
import com.industry.printer.data.BinFileMaker;
import com.industry.printer.data.BinFromBitmap;
import com.industry.printer.R;
import com.industry.printer.data.ZIntSymbol;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextUtils;
import android.util.Log;

import uk.org.okapibarcode.backend.Code128;
import uk.org.okapibarcode.backend.DataMatrix;
import uk.org.okapibarcode.backend.HumanReadableLocation;
import uk.org.okapibarcode.backend.OkapiException;
import uk.org.okapibarcode.backend.QrCode;
import uk.org.okapibarcode.backend.Symbol;
import uk.org.okapibarcode.output.Java2DRenderer;
import uk.org.okapibarcode.util.Gs1;

public class BarcodeObject extends BaseObject {
	private static final String TAG = BarcodeObject.class.getSimpleName();

	/** EAN-8 1D format. */
	public static final String BARCODE_FORMAT_EAN8 				= "EAN8";
	/** EAN-13 1D format. */
	public static final String BARCODE_FORMAT_EAN13 			= "EAN13";
	/** EAN-128 1D format? Not Found */
	public static final String BARCODE_FORMAT_EAN128 			= "EAN128";
	/** Code 128 1D format. */
	public static final String BARCODE_FORMAT_CODE128 			= "CODE_128";
	/** Code 39 1D format. */
	public static final String BARCODE_FORMAT_CODE39 			= "CODE_39";
	/** UPC-A 1D format. */
	public static final String BARCODE_FORMAT_UPC_A 			= "UPC_A";
	/** ITF (Interleaved Two of Five) 1D format. */
	public static final String BARCODE_FORMAT_ITF_14 			= "ITF_14";
	/** QR Code 2D barcode format. */
	public static final String BARCODE_FORMAT_QR 				= "QR";
	/** Data Matrix 2D barcode format. */
	public static final String BARCODE_FORMAT_DM 				= "DM";

	/** Data Matrix 2D barcode format. */
	public static final String BARCODE_FORMAT_DATA_MATRIX 		= "DATA_MATRIX";

	/** Code 93 1D format. */
	public static final String BARCODE_FORMAT_CODE93 			= "CODE_93";
	/** CODABAR 1D format. */
	public static final String BARCODE_FORMAT_CODABAR 			= "CODABAR";
	/** UPC-E 1D format. */
	public static final String BARCODE_FORMAT_UPC_E 			= "UPC_E";
	/** RSS 14 */
	public static final String BARCODE_FORMAT_RSS14 			= "RSS14";
	/** RSS EXPANDED */
	public static final String BARCODE_FORMAT_RSS_EXPANDED 		= "RSS_EXPANDED";
	/** Aztec 2D barcode format. */
	public static final String BARCODE_FORMAT_RSS_AZTEC 		= "AZTEC";
	/** PDF417 format. */
	public static final String BARCODE_FORMAT_RSS_PDF_417 		= "PDF_417";
	// H.M.Wang 2023-11-21 追加GS1的QR和DM
	public static final String BARCODE_FORMAT_GS1128 			= "GS1128";
	public static final String BARCODE_FORMAT_GS1QR 			= "GS1QR";
	public static final String BARCODE_FORMAT_GS1DM 			= "GS1DM";
// End of H.M.Wang 2023-11-21 追加GS1的QR和DM

	/** MaxiCode 2D barcode format. */
	// MAXICODE,
	/** UPC/EAN extension format. Not a stand-alone format. */
	// UPC_EAN_EXTENSION

// H.M.Wang 2021-10-7 EAN128的生成有错误
// 参照模型：CODE(本机支持的格式如下，仅作为保存数值使用) -> FORMAT(内部的格式定义，如上）-> FORMAT（Zxing的BarcodeFormat格式，如getBarcodeFormat的返回，但该函数直接使用的字符串）
	public static final int CODE_EAN8 							= 0;
	public static final int CODE_EAN13 						= 1;
	public static final int CODE_EAN128 						= 2;
	public static final int CODE_CODE128 						= 3;
	// H.M.Wang 2023-11-21 追加GS1的QR和DM
	public static final int CODE_GS1128 						= 4;
	// End of H.M.Wang 2023-11-21 追加GS1的QR和DM
	public static final int CODE_CODE39 						= 5;
	public static final int CODE_UPC_A 						= 6;
	public static final int CODE_ITF_14 						= 7;
	public static final int CODE_QR 							= 0;
	public static final int CODE_DM 							= 1;
	// H.M.Wang 2023-11-21 追加GS1的QR和DM
	public static final int CODE_GS1QR							= 2;
	public static final int CODE_GS1DM							= 3;
// End of H.M.Wang 2023-11-21 追加GS1的QR和DM

	private static final int QUIET_ZONE_SIZE = 4;
	private static final int STROKE_WIDTH = 3;

	public String mFormat;
	public int mCode;
	public boolean mShow;
	// H.M.Wang 2022-12-20 追加反白设置
	public boolean mRevert;
	// End of H.M.Wang 2022-12-20 追加反白设置
	public int mTextSize;

	// H.M.Wang 2020-2-25 追加ITF_14边框有无的设置
	private boolean mWithFrame;
// End of H.M.Wang 2020-2-25 追加ITF_14边框有无的设置

	// H.M.Wang 2023-2-14 追加QR码的纠错级别
	private int mErrorCorrectionLevel;
// End of H.M.Wang 2023-2-14 追加QR码的纠错级别

	// H.M.Wang 2020-7-31 追加超文本内容，条码的内容可能是超文本
	private HyperTextObject mHTContent = null;
	private String mOrgContent = "";
// End of H.M.Wang 2020-7-31 追加超文本内容，条码的内容可能是超文本

// H.M.Wang 2024-10-24 追加DM码的种类选择
	private final static int DM_TYPE_SQUARE = 0;
	private final static int DM_TYPE_RECTANGLE = 1;
	private int mDMType;
// End of H.M.Wang 2024-10-24 追加DM码的种类选择

//	public Bitmap mBinmap;

	private Map<String, Integer> code_format;
	private Map<Integer, String> format_code;

	public BarcodeObject(Context context, float x) {
		super(context, BaseObject.OBJECT_TYPE_BARCODE, x);
		// TODO Auto-generated constructor stub
		mShow = true;
// H.M.Wang 2022-12-20 追加反白设置
		mRevert = false;
// End of H.M.Wang 2022-12-20 追加反白设置
		mCode = 3;
		mTextSize = 20;
// H.M.Wang 2020-2-25 追加ITF_14边框有无的设置
		mWithFrame = true;
// End of H.M.Wang 2020-2-25 追加ITF_14边框有无的设置

// H.M.Wang 2023-2-14 追加QR码的纠错级别
		mErrorCorrectionLevel = 0;
// End of H.M.Wang 2023-2-14 追加QR码的纠错级别

// H.M.Wang 2020-7-31 追加超文本内容，条码的内容可能是超文本
		mHTContent = new HyperTextObject(context, x);
// End of H.M.Wang 2020-7-31 追加超文本内容，条码的内容可能是超文本

//		mFormat="CODE_128";
		mFormat = BARCODE_FORMAT_CODE128;

// H.M.Wang 2024-10-24 追加DM码的种类选择
		mDMType = DM_TYPE_SQUARE;
// End of H.M.Wang 2024-10-24 追加DM码的种类选择

		if (mSource == true) {
			setContent("123456789");
		} else {
			setContent("");
		}
		mWidth=0;
	}
	public void setCode(String code)
	{

		mId = BaseObject.OBJECT_TYPE_BARCODE;
//		if ("EAN8".equals(code)) {
		if (BARCODE_FORMAT_EAN8.equals(code)) {
//			mCode = 0;
			mCode = CODE_EAN8;
//		} else if ("EAN13".equals(code)) {
		} else if (BARCODE_FORMAT_EAN13.equals(code)) {
//			mCode = 1;
			mCode = CODE_EAN13;
//		} else if ("EAN128".equals(code)) {
		} else if (BARCODE_FORMAT_EAN128.equals(code)) {
//			mCode = 2;
			mCode = CODE_EAN128;
//		} else if ("CODE_128".equals(code)) {
		} else if (BARCODE_FORMAT_CODE128.equals(code)) {
//			mCode = 3;
			mCode = CODE_CODE128;
// H.M.Wang 2023-11-21 追加GS1的QR和DM
		} else if (BARCODE_FORMAT_GS1128.equals(code)) {
			mCode = CODE_GS1128;
// End of H.M.Wang 2023-11-21 追加GS1的QR和DM
//		} else if ("CODE_39".equals(code)) {
		} else if (BARCODE_FORMAT_CODE39.equals(code)) {
//			mCode = 5;
			mCode = CODE_CODE39;
//		} else if ("ITF_14".equals(code)) {
		} else if (BARCODE_FORMAT_ITF_14.equals(code)) {
//			mCode = 6;
			mCode = CODE_ITF_14;
//		} else if ("UPC_A".equals(code)) {
		} else if (BARCODE_FORMAT_UPC_A.equals(code)) {
//			mCode = 7;
			mCode = CODE_UPC_A;
//		} else if ("QR".equals(code)) {
		} else if (BARCODE_FORMAT_QR.equals(code)) {
//			mCode = 0;
			mCode = CODE_QR;
			mId = BaseObject.OBJECT_TYPE_QR;
//		} else if ("DM".equals(code)) {
		} else if (BARCODE_FORMAT_DM.equals(code)) {
//			mCode = 1;
			mCode = CODE_DM;
			mId = BaseObject.OBJECT_TYPE_QR;
// H.M.Wang 2023-11-21 追加GS1的QR和DM
//		} else if ("GS1QR".equals(code)) {
		} else if (BARCODE_FORMAT_GS1QR.equals(code)) {
//			mCode = 2;
			mCode = CODE_GS1QR;
			mId = BaseObject.OBJECT_TYPE_QR;
//		} else if ("GS1DM".equals(code)) {
		} else if (BARCODE_FORMAT_GS1DM.equals(code)) {
//			mCode = 3;
			mCode = CODE_GS1DM;
			mId = BaseObject.OBJECT_TYPE_QR;
// End of H.M.Wang 2023-11-21 追加GS1的QR和DM
		} else {
			return;
		}
		mFormat = code;
		isNeedRedraw = true;
	}

	public void setCode(int code)
	{
		if (code == CODE_EAN8) {
			mCode = CODE_EAN8;
			mFormat = BARCODE_FORMAT_EAN8;
		} else if (code == CODE_EAN13) {
			mCode = CODE_EAN13;
			mFormat = BARCODE_FORMAT_EAN13;
		} else if (code == CODE_EAN128) {
			mCode = CODE_EAN128;
			mFormat = BARCODE_FORMAT_EAN128;
		} else if (code == CODE_CODE128) {
			mCode = CODE_CODE128;
			mFormat = BARCODE_FORMAT_CODE128;
// H.M.Wang 2023-11-21 追加GS1的QR和DM
		} else if (code == CODE_GS1128) {
			mCode = CODE_GS1128;
			mFormat = BARCODE_FORMAT_GS1128;
// End of H.M.Wang 2023-11-21 追加GS1的QR和DM
		} else if (code == CODE_CODE39) {
			mCode = CODE_CODE39;
			mFormat = BARCODE_FORMAT_CODE39;
		} else if (code == CODE_ITF_14) {
			mCode = CODE_ITF_14;
			mFormat = BARCODE_FORMAT_ITF_14;
		} else if (code == CODE_UPC_A) {
			mCode = CODE_UPC_A;
			mFormat = BARCODE_FORMAT_UPC_A;
		}
/*		if (code == 0) {
			mCode = 0;
			mFormat = "EAN8";
		} else if (code == 1) {
			mCode = 1;
			mFormat = "EAN13";
		} else if (code == 2) {
			mCode = 2;
			mFormat = "EAN128";
		} else if (code == 3) {
			mCode = 3;
			mFormat = "CODE_128";
		} else if (code == 5) {
			mCode = 5;
			mFormat = "CODE_39";
		} else if (code == 6) {
			mCode = 6;
			mFormat = "ITF_14";
		} else if (code == 7) {
			mCode = 7;
			mFormat = "UPC_A";
		}*/
		mId = BaseObject.OBJECT_TYPE_BARCODE;
		isNeedRedraw = true;
	}

	public String getCode()
	{
		return mFormat;
	}

	public boolean isQRCode() {
//		return "QR".equals(mFormat);
		return BARCODE_FORMAT_QR.equals(mFormat);
	}

	// H.M.Wang 2019-12-15 原来判断是否为二维动态二维码的逻辑可能有问题
	// H.M.Wang 2019-9-21 二维码有两种QRCode和DynamicQRCode，只有第二种需要隐藏内容编辑窗，为此增加判断动态二维码的函数
	public boolean isDynamicCode() {
//		return isQRCode() && mName.equals(mContext.getString(R.string.object_dynamic_qr));
// H.M.Wang 2020-7-29 所有条码均可以设置为动态条码
//		return is2D() && mSource;
		return mSource;
// End of H.M.Wang 2020-7-29 所有条码均可以设置为动态条码
	}
// End. ----

	public void setShow(boolean show) {
		mShow = show;
	}
	public boolean getShow() {
		return mShow;
	}

	// H.M.Wang 2022-12-20 追加反白设置
	public void setRevert(boolean revert) {
		mRevert = revert;
	}
	public boolean getRevert() {
		return mRevert;
	}
// End of H.M.Wang 2022-12-20 追加反白设置

// H.M.Wang 2020-2-25 追加ITF_14边框有无的设置
	public void setWithFrame(boolean withFrame)
	{
		mWithFrame = withFrame;
	}
	public boolean getWithFrame()
	{
		return mWithFrame;
	}
// End of H.M.Wang 2020-2-25 追加ITF_14边框有无的设置
// H.M.Wang 2024-10-24 追加DM码的种类选择
	public void setDMType(int type) {
		mDMType = type;
	}
	public int getDMType()
	{
		return mDMType;
	}
// End of H.M.Wang 2024-10-24 追加DM码的种类选择

	public void setTextsize(int size) {
		if (size == mTextSize) {
			return;
		}
		if (size > 0 && size < 100) {
			mTextSize = size;
			isNeedRedraw = true;
		}
	}

	public int getTextsize() {
		return mTextSize;
	}

	// H.M.Wang 2023-2-14 追加QR码的纠错级别
	public void setErrorCorrectionLevel(int level) {
		mErrorCorrectionLevel = level;
	}

	public int getErrorCorrectionLevel() {
		return mErrorCorrectionLevel;
	}
// End of H.M.Wang 2023-2-14 追加QR码的纠错级别

	// H.M.Wang 2022-9-7 将根据字数重新计算宽度的操作独立出来，只有在编辑页面保存的时候才会调用，中途内容发生变化的时候，不修改宽度
	public void calWidth() {
		if (!is2D()) {
//			if (mWidth <= 0) {
			mWidth = mContent.length() * 50 * mRatio * mHeight / 152;
//			}
		} else {
//			if (mWidth <= 0) {
			mWidth = mHeight;
// H.M.Wang 2024-10-24 追加DM码的种类选择
			if(mDMType == DM_TYPE_RECTANGLE) {
				Debug.d(TAG, "DM_TYPE_RECTANGLE");
				mWidth = mHeight * 18 / 8;
			}
// End of H.M.Wang 2024-10-24 追加DM码的种类选择
//			}
		}
		setWidth(mWidth);
	}
// End of H.M.Wang 2022-9-7 将根据字数重新计算宽度的操作独立出来，只有在编辑页面保存的时候才会调用，中途内容发生变化的时候，不修改宽度

	@Override
	public void setContent(String content)
	{
// H.M.Wang 2020-7-31 追加超文本内容，条码的内容可能是超文本
		Debug.d(TAG, "setContent = " + content);
		mOrgContent = content;
		mHTContent.setContent(content);
		mContent = mHTContent.getExpandedContent();
//		mContent=content;
// End of H.M.Wang 2020-7-31 追加超文本内容，条码的内容可能是超文本

// H.M.Wang 2020-12-25 这里当生成条码的时候，会因为宽度为0，而在getPrintBitmap函数里面异常退出
//		if (!is2D()) {
//			mWidth = 0;
//		}
		check();
// H.M.Wang 2022-9-7 取消设置内容是重新计算宽度，否则会在运行时由于内容字数的变化而产生宽度的变化，而倒置后面的内容被后移。只有在编辑页面保存时才根据字数重新计算宽度
/*		if (!is2D()) {
//			if (mWidth <= 0) {
				mWidth = mContent.length() * 50 * mRatio * mHeight / 152;
//			}
		} else {
//			if (mWidth <= 0) {
				mWidth = mHeight;
//			}
		}
		setWidth(mWidth);*/
// End of H.M.Wang 2022-9-7 取消设置内容是重新计算宽度，否则会在运行时由于内容字数的变化而产生宽度的变化，而倒置后面的内容被后移。只有在编辑页面保存时才根据字数重新计算宽度
// End of H.M.Wang 2020-12-25 这里当生成条码的时候，会因为宽度为0，而在getPrintBitmap函数里面异常退出

		isNeedRedraw = true;
	}

	// H.M.Wang 2020-7-31 追加超文本内容，条码的内容可能是超文本，所以返回原始内容，而非转化内容
	@Override
	public String getContent() {
		return mOrgContent;
	}
	// End of H.M.Wang 2020-7-31 追加超文本内容，条码的内容可能是超文本，所以返回原始内容，而非转化内容
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
	@Override
	public void setSource(boolean dynamic) {
		mSource  = dynamic;
		if (dynamic) {
// H.M.Wang 2020-7-29 所有条码均可以设置为动态条码
			if (is2D()) {
				mName = mContext.getString(R.string.object_dynamic_qr);
			} else {
				mName = mContext.getString(R.string.object_dynamic_barcode);
			}
//			mName = mContext.getString(R.string.object_dynamic_qr);
// End of H.M.Wang 2020-7-29 所有条码均可以设置为动态条码
// H.M.Wang 2020-8-11 动态条码内容设置为123456
//			setContent("dynamic");
// H.M.Wang 2022-4-22 如果没有设置内容，则使用缺省设置
			if(mContent.isEmpty()) setContent("123456");
// End of H.M.Wang 2022-4-22 如果没有设置内容，则使用缺省设置
// End of H.M.Wang 2020-8-11 动态条码内容设置为123456
		} else {
			mName = mContext.getString(R.string.object_bar);
		}
	}

	@Override
	public String getTitle() {
		if (is2D()) {
			if (mSource) {
				mName = mContext.getString(R.string.object_dynamic_qr);
			} else {
				mName = mContext.getString(R.string.object_qr);
			}
		} else {
// H.M.Wang 2020-7-29 所有条码均可以设置为动态条码
			if (mSource) {
				mName = mContext.getString(R.string.object_dynamic_barcode);
			} else {
				mName = mFormat + " " + mContext.getString(R.string.object_bar);
			}
//			mName = mFormat + " " + mContext.getString(R.string.object_bar);
// End of H.M.Wang 2020-7-29 所有条码均可以设置为动态条码
		}

		return mName;
	}

	private static final String CODE = "utf-8";
	/*
        // H.M.Wang 追加该函数。用来获取初始的横向缩放比例
        @Override
        public void setXRatio() {
            if (!is2D()) {
                mRatio = (mWidth == 0 ? 1.0f : (mWidth / (mContent.length() * 70)));
            } else {
                mRatio = (mWidth == 0 ? 1.0f : (mWidth / 152));
            }
        }
    */
// H.M.Wang 2019-9-27 追加该函数，用来在条码/二维码设置字号的时候，恢复到原始比例状态，并且根据新的高计算新的宽
	@Override
	public void resizeByHeight() {
		if (!is2D()) {
			mWidth = mContent.length() * 50 * mHeight / 152;
		} else {
			mWidth = mHeight;
// H.M.Wang 2024-10-24 追加DM码的种类选择
			if(mDMType == DM_TYPE_RECTANGLE) {
				Debug.d(TAG, "DM_TYPE_RECTANGLE");
				mWidth = mHeight * 18 / 8;
			}
// End of H.M.Wang 2024-10-24 追加DM码的种类选择
		}
	}

	// H.M.Wang 2020-7-31 追加超文本的计数器打印后调整
	public void goNext() {
		mHTContent.goNext();
	}
// End of H.M.Wang 2020-7-31 追加超文本的计数器打印后调整

	// H.M.Wang 2021-5-7 追加实际打印数调整函数
	public void goPrintedNext() {
		mHTContent.goPrintedNext();
	}
// End of H.M.Wang 2021-5-7 追加实际打印数调整函数

	// H.M.Wang 修改该函数。以对应于纵向和横向的比例变化
	public Bitmap getScaledBitmap(Context context) {
		Debug.i(TAG, "getScaledBitmap()");

		if (!isNeedRedraw) {
			return mBitmap;
		}

// H.M.Wang 2020-7-31 追加超文本内容，条码的内容可能是超文本
		String cnt = mHTContent.getExpandedContent();
		mContent = cnt;
// End of H.M.Wang 2020-7-31 追加超文本内容，条码的内容可能是超文本

		isNeedRedraw = false;
		check();
		if (!is2D()) {
			if (mWidth <= 0) {
// H.M.Wang 2019-9-26 这个宽度的设置，由于参照的元是根据字数直接算的，而不是像其他的元素根据高计算的，因此如果高做了调整，mRatio里面已经考虑了高变化的因素，因此需要将高的因素化解后使用
				mWidth = mContent.length() * 50 * mRatio * mHeight / 152;
//                mWidth = mContent.length() * 70;
			}
// H.M.Wang2019-9-26 恢复使用mWidth和mHeight进行画图
			mBitmap = draw(mContent, (int)mWidth, (int)mHeight);
//			mBitmap = draw(mContent, mContent.length() * 70, (int)mHeight);
		} else {
			if (mWidth <= 0) {
				mWidth = mHeight;
// H.M.Wang 2024-10-24 追加DM码的种类选择
				if(mDMType == DM_TYPE_RECTANGLE) {
					Debug.d(TAG, "DM_TYPE_RECTANGLE");
					mWidth = mHeight * 18 / 8;		// 生成的DM码，当高度为8的时候，宽度为18，因此16点的时候，如果简单的以高度的倍数取宽，则32不能满足36个点，因此DM生成时会主动缩小，为避免这个情况，宽度设为*18/2
				}
// End of H.M.Wang 2024-10-24 追加DM码的种类选择
			}
//			mWidth = mRatio * 152;
//			mWidth = mRatio * mHeight;
//            mWidth = mHeight;
//			if (mFormat.equalsIgnoreCase("DM") || mFormat.equalsIgnoreCase("DATA_MATRIX")) {
			if (mFormat.equalsIgnoreCase(BARCODE_FORMAT_DM)) {
				mBitmap = drawDataMatrix(mContent, (int) mWidth, (int) mHeight);
// H.M.Wang 2023-11-21 追加GS1的QR和DM
			} else if (mFormat.equalsIgnoreCase(BARCODE_FORMAT_GS1QR)) {
				mBitmap = drawOkapiQR(mContent, (int) mWidth, (int) mHeight);
			} else if (mFormat.equalsIgnoreCase(BARCODE_FORMAT_GS1DM)) {
				mBitmap = drawGS1Datamatrix(mContent, (int) mWidth, (int) mHeight);
// End of H.M.Wang 2023-11-21 追加GS1的QR和DM
			} else {
				mBitmap = drawQR(mContent, (int) mWidth, (int) mHeight);
//				mBitmap = drawQR(mContent, 152, 152);
			}
		}
		// mBitmap = draw(mContent, (int)mWidth, (int)mHeight);
		setWidth(mWidth);

// H.M.Wang 2022-4-23 追加动态条码、二维码使用蓝色图案
		if(mSource) {
			int[] pixels = new int[(int)mWidth * (int)mHeight];
			mBitmap.getPixels(pixels, 0, (int)mWidth, 0, 0, (int)mWidth, (int)mHeight);

			for(int i=0; i<(int)mWidth; i++) {
				for(int j=0; j<(int)mHeight; j++) {
					if(pixels[i*(int)mHeight + j] == 0xff000000) {
						pixels[i*(int)mHeight + j] = 0xff0000ff;
					}
				}
			}
			mBitmap.setPixels(pixels, 0, (int)mWidth, 0, 0, (int)mWidth, (int)mHeight);
		}
// End of H.M.Wang 2022-4-23 追加动态条码、二维码使用蓝色图案

		return mBitmap;
	}


	// H.M.Wang 修改该函数。以对应于纵向和横向的比例变化
	@Override
	public Bitmap getpreviewbmp() {
/*
		if (mFormat.equalsIgnoreCase("DM") || mFormat.equalsIgnoreCase("DATA_MATRIX")) {
//				mBitmap = drawDataMatrix(mContent, (int) mWidth, (int) mHeight);
			return drawDataMatrix(mContent, 152, 152);
		} else {
//				mBitmap = drawQR(mContent, (int) mWidth, (int) mHeight);
			return drawQR(mContent, 152, 152);
		}
*/
//H.M.Wang 2019-9-27 追加判断是否已经回收
//		if (mBitmap == null || mWidth == 0 || mHeight == 0) {
		if (mBitmap == null || mBitmap.isRecycled() || mWidth == 0 || mHeight == 0) {
			isNeedRedraw = true;
			mBitmap = getScaledBitmap(mContext);
		}

		return Bitmap.createScaledBitmap(mBitmap, (int) mWidth, (int) mHeight, false);
	}

	@Override
	public Bitmap makeBinBitmap(Context ctx, String content, int ctW, int ctH, String font) {
// H.M.Wang 2025-7-10 修改当内容为动态条码或者是静态条码中包含超文本时，不生成，直接返回空
		if(getSource() || needRedraw()) return null;
// End of H.M.Wang 2025-7-10 修改当内容为动态条码或者是静态条码中包含超文本时，不生成，直接返回空

		if (is2D()) {
//			if (mFormat.equalsIgnoreCase("DM") || mFormat.equalsIgnoreCase("DATA_MATRIX")) {
			if (mFormat.equalsIgnoreCase(BARCODE_FORMAT_DM)) {
				return drawDataMatrix(content, ctW, ctH);
// H.M.Wang 2023-11-21 追加GS1的QR和DM
			} else if (mFormat.equalsIgnoreCase(BARCODE_FORMAT_GS1QR)) {
				return drawOkapiQR(mContent, (int) mWidth, (int) mHeight);
			} else if (mFormat.equalsIgnoreCase(BARCODE_FORMAT_GS1DM)) {
				return drawGS1Datamatrix(mContent, (int) mWidth, (int) mHeight);
// End of H.M.Wang 2023-11-21 追加GS1的QR和DM
			} else {
				return drawQR(content, ctW, ctH);
			}
		} else {
			return draw(content, ctW, ctH);
		}
	}

	private Bitmap drawLcfQR(String strContent, int w, int h) {
		Debug.d(TAG, "drawLcfQR Content: " + strContent);
		try {
			if (w <= 0) {
				Debug.d(TAG, "drawLcfQR Width = 0");
				w = Math.round(mWidth);
			}
			if (h <= 0) {
				Debug.d(TAG, "drawLcfQR Height = 0");
				h = Math.round(mHeight);
			}

			ZIntSymbol zIntSymbol = new ZIntSymbol();
			zIntSymbol.text = strContent;
			zIntSymbol.symbology = 58;
			zIntSymbol.show_hrt = 0;

			zIntSymbol = BarcodeGeneratorJNI.generateBarcode(zIntSymbol, strContent, 0);

			Bitmap bmp = Bitmap.createBitmap(zIntSymbol.bitmap_width, zIntSymbol.bitmap_height, Configs.BITMAP_CONFIG);
			bmp.setPixels(zIntSymbol.pixels, 0, zIntSymbol.bitmap_width, 0, 0, zIntSymbol.bitmap_width, zIntSymbol.bitmap_height);
			return Bitmap.createScaledBitmap(bmp, w, h, false);
		} catch (Exception e) {
			Log.d(TAG, "--->exception: " + e.getMessage());
			if (mContext instanceof MainActivity) {
				((MainActivity) mContext).mControlTab.sendMsg("Wrong Command: " + e.getMessage());
			}
		}
		return Bitmap.createBitmap(w, h, Configs.BITMAP_CONFIG);
	}

	private Bitmap drawQR(String content, int w, int h) {
		try {
			Debug.d(TAG, "Content: " + content + "; w: " + w + "; h: " + h);

			HashMap<EncodeHintType, Object> hints = new HashMap<EncodeHintType, Object>();
			if(h == 32) {			// 32点头，强制使用版本3，因为版本3是29x29，最接近32点的尺寸，外边空白最小
				hints.put(EncodeHintType.QR_VERSION, 3);		// 强制生成一个29x29的QR码，但是如果要生成的QR码大于29x29，那么这个设置可能失效或者错误
			}

// H.M.Wang 2023-2-14 追加QR码的纠错级别
			switch(mErrorCorrectionLevel) {
				case 1:
					Debug.d(TAG, "ECL: M");
					hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
					break;
				case 2:
					Debug.d(TAG, "ECL: Q");
					hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.Q);
					break;
				case 3:
					Debug.d(TAG, "ECL: H");
					hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
					break;
				default:
					Debug.d(TAG, "ECL: L");
					hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
					break;
			}
// End of H.M.Wang 2023-2-14 追加QR码的纠错级别

// H.M.Wang 2025-7-21 当打印头是hp22mm或者hp22mmx2时，生成打印缓冲区时宽度和高度缩小的倍率不一样，导致生成的二维码会按着短边生成一个缩小的图，修改为按着短边生成的图放大至全空间
			BitMatrix matrix;
			if((SystemConfigFile.getInstance().getPNozzle() == PrinterNozzle.MESSAGE_TYPE_22MM || SystemConfigFile.getInstance().getPNozzle() == PrinterNozzle.MESSAGE_TYPE_22MMX2) && w != h) {
				matrix = encode(content, Math.min(w, h), Math.min(w, h), hints);
			} else {
				matrix = encode(content, w, h, hints);
			}
//			BitMatrix matrix = encode(content, w, h, hints);
// End of H.M.Wang 2025-7-21 当打印头是hp22mm或者hp22mmx2时，生成打印缓冲区时宽度和高度缩小的倍率不一样，导致生成的二维码会按着短边生成一个缩小的图，修改为按着短边生成的图放大至全空间

//			int tl[] = matrix.getTopLeftOnBit();
			int width = matrix.getWidth();
			int height = matrix.getHeight();
			Debug.d("BarcodeObject", "mWidth="+ w +", width="+width + "   height=" + height);
			int[] pixels = new int[width * height];
			for (int y = 0; y < height; y++)
			{
				for (int x = 0; x < width; x++)
				{
					if (matrix.get(x, y))
					{
// H.M.Wang 2022-12-20 追加反白设置
//						pixels[y * width + x] = mReverse ? 0xffffffff : 0xff000000;
						pixels[y * width + x] = mRevert ? 0xffffffff : (needRedraw() ? 0xff0000ff : 0xff000000);
// End of H.M.Wang 2022-12-20 追加反白设置
					} else {
// H.M.Wang 2022-12-20 追加反白设置
//						pixels[y * width + x] = mReverse ? 0xff000000 : 0xffffffff;
						pixels[y * width + x] = mRevert ? (needRedraw() ? 0xff0000ff : 0xff000000) : 0xffffffff;
// End of H.M.Wang 2022-12-20 追加反白设置
					}
				}
			}
			// 条码/二维码的四个边缘空出20像素作为白边
			Bitmap bitmap = Bitmap.createBitmap(width, height, Configs.BITMAP_CONFIG);

			bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

			// H.M.Wang 修改返回值一行
// H.M.Wang 2023-2-1 因为参数的width和height已经是目标宽高，因此不必再次调整位图大小
// H.M.Wang 2025-7-21 当打印头是hp22mm或者hp22mmx2时，生成打印缓冲区时宽度和高度缩小的倍率不一样，导致生成的二维码会按着短边生成一个缩小的图，修改为按着短边生成的图放大至全空间
			if((SystemConfigFile.getInstance().getPNozzle() == PrinterNozzle.MESSAGE_TYPE_22MM || SystemConfigFile.getInstance().getPNozzle() == PrinterNozzle.MESSAGE_TYPE_22MMX2) &&
					(width != w || height != h)) {
				return Bitmap.createScaledBitmap(bitmap, w, h, false);
			}
// End of H.M.Wang 2025-7-21 当打印头是hp22mm或者hp22mmx2时，生成打印缓冲区时宽度和高度缩小的倍率不一样，导致生成的二维码会按着短边生成一个缩小的图，修改为按着短边生成的图放大至全空间
// End of H.M.Wang 2023-2-1 因为参数的width和height已经是目标宽高，因此不必再次调整位图大小
			return bitmap;
//			return Bitmap.createScaledBitmap(bitmap, w, h, false);
//			return Bitmap.createScaledBitmap(bitmap, (int) mWidth, (int) mHeight, false);
//			return bitmap;
//		} catch (WriterException e) {
//			Debug.e(TAG, e.getMessage());
		} catch (Exception e) {
			Debug.e(TAG, e.getMessage());
		}
		return null;
	}

	// H.M.Wang 2023-11-21 追加GS1的QR和DM
	private Bitmap drawOkapiQR(String content, int w, int h) {
		long startTime = System.nanoTime();

// H.M.Wang 2025-7-22 当打印头是hp22mm或者hp22mmx2时，生成打印缓冲区时宽度和高度缩小的倍率不一样，导致生成的二维码会按着短边生成一个缩小的图，修改为按着短边生成的图放大至全空间
        int drawW = w, drawH = h;
        if(SystemConfigFile.getInstance().getPNozzle() == PrinterNozzle.MESSAGE_TYPE_22MM || SystemConfigFile.getInstance().getPNozzle() == PrinterNozzle.MESSAGE_TYPE_22MMX2) {
            drawW = Math.min(w, h);
            drawH = drawW;
        }
//		Bitmap outBmp = Bitmap.createBitmap(w, h, Configs.BITMAP_CONFIG);;
        Bitmap outBmp = Bitmap.createBitmap(drawW, drawH, Configs.BITMAP_CONFIG);
// End of H.M.Wang 2025-7-22 当打印头是hp22mm或者hp22mmx2时，生成打印缓冲区时宽度和高度缩小的倍率不一样，导致生成的二维码会按着短边生成一个缩小的图，修改为按着短边生成的图放大至全空间

//		Debug.d(TAG, "GS1 QR: cnt=[" + content + "]; \nw=" + w + "; h=" + h);

		try {
			QrCode qrcode = new QrCode();
			qrcode.setDataType(Symbol.DataType.GS1);
			qrcode.setSeparatorType(Symbol.SeparatorType.GS);
			switch(mErrorCorrectionLevel) {
				case 1:
					Debug.d(TAG, "ECL: M");
					qrcode.setPreferredEccLevel(QrCode.EccLevel.M);
					break;
				case 2:
					Debug.d(TAG, "ECL: Q");
					qrcode.setPreferredEccLevel(QrCode.EccLevel.Q);
					break;
				case 3:
					Debug.d(TAG, "ECL: H");
					qrcode.setPreferredEccLevel(QrCode.EccLevel.H);
					break;
				default:
					Debug.d(TAG, "ECL: L");
					qrcode.setPreferredEccLevel(QrCode.EccLevel.L);
					break;
			}
			qrcode.setContent(content);
//			Debug.d(TAG, "GS1 QR Height: " + qrcode.getHeight() + "; Bar Height: " + qrcode.getBarHeight());

// H.M.Wang 2025-7-22 当打印头是hp22mm或者hp22mmx2时，生成打印缓冲区时宽度和高度缩小的倍率不一样，导致生成的二维码会按着短边生成一个缩小的图，修改为按着短边生成的图放大至全空间
			if(qrcode.getHeight() <= drawH) {       // 生成位图，同时如果生成的QR码比容器小，需要调整（按比例缩放）
				Java2DRenderer renderer = new Java2DRenderer(drawH/qrcode.getHeight(), Color.WHITE, Color.BLACK);
// End of H.M.Wang 2025-7-22 当打印头是hp22mm或者hp22mmx2时，生成打印缓冲区时宽度和高度缩小的倍率不一样，导致生成的二维码会按着短边生成一个缩小的图，修改为按着短边生成的图放大至全空间
				renderer.setReverse(mRevert);
				if (isDynamicCode() || needRedraw()) {
					renderer.setInk(0xff0000ff);
				}
				Bitmap bitmap = renderer.render(qrcode);
//				Debug.d(TAG, "GS1 QR width: " + bitmap.getWidth() + "; height: " + bitmap.getHeight());

				Canvas canvas = new Canvas(outBmp);
// H.M.Wang 2025-7-22 当打印头是hp22mm或者hp22mmx2时，生成打印缓冲区时宽度和高度缩小的倍率不一样，导致生成的二维码会按着短边生成一个缩小的图，修改为按着短边生成的图放大至全空间
				canvas.drawBitmap(bitmap, (drawW - bitmap.getWidth())/2, (drawH - bitmap.getHeight())/2, new Paint());  // 将bitmap画在中央
// End of H.M.Wang 2025-7-22 当打印头是hp22mm或者hp22mmx2时，生成打印缓冲区时宽度和高度缩小的倍率不一样，导致生成的二维码会按着短边生成一个缩小的图，修改为按着短边生成的图放大至全空间
				bitmap.recycle();
			}
		} catch (OkapiException e) {
			Debug.e(TAG, e.getMessage());
		} catch (Exception e) {
			Debug.e(TAG, e.getMessage());
		}

		Debug.d(TAG, "GS1 QR spent time: " + ((System.nanoTime() - startTime) / 1000000));
// H.M.Wang 2025-7-22 当打印头是hp22mm或者hp22mmx2时，生成打印缓冲区时宽度和高度缩小的倍率不一样，导致生成的二维码会按着短边生成一个缩小的图，修改为按着短边生成的图放大至全空间
        if((SystemConfigFile.getInstance().getPNozzle() == PrinterNozzle.MESSAGE_TYPE_22MM || SystemConfigFile.getInstance().getPNozzle() == PrinterNozzle.MESSAGE_TYPE_22MMX2) &&
            (w != drawW || h != drawH)) {
            return Bitmap.createScaledBitmap(outBmp, w, h, false);
        }
// End of H.M.Wang 2025-7-22 当打印头是hp22mm或者hp22mmx2时，生成打印缓冲区时宽度和高度缩小的倍率不一样，导致生成的二维码会按着短边生成一个缩小的图，修改为按着短边生成的图放大至全空间
		return outBmp;
	}

	private Bitmap drawGS1Datamatrix(String content, int w, int h) {
		long startTime = System.nanoTime();
// H.M.Wang 2025-7-22 当打印头是hp22mm或者hp22mmx2时，生成打印缓冲区时宽度和高度缩小的倍率不一样，导致生成的二维码会按着短边生成一个缩小的图，修改为按着短边生成的图放大至全空间
        int drawW = w, drawH = h;
        if(SystemConfigFile.getInstance().getPNozzle() == PrinterNozzle.MESSAGE_TYPE_22MM || SystemConfigFile.getInstance().getPNozzle() == PrinterNozzle.MESSAGE_TYPE_22MMX2) {
            drawW = Math.min(w, h);
            drawH = drawW;
        }
//        Bitmap outBmp = Bitmap.createBitmap(w, h, Configs.BITMAP_CONFIG);
        Bitmap outBmp = Bitmap.createBitmap(drawW, drawH, Configs.BITMAP_CONFIG);
// End of H.M.Wang 2025-7-22 当打印头是hp22mm或者hp22mmx2时，生成打印缓冲区时宽度和高度缩小的倍率不一样，导致生成的二维码会按着短边生成一个缩小的图，修改为按着短边生成的图放大至全空间

//		Debug.d(TAG, "GS1 DM: cnt=[" + content + "]; \nw=" + w + "; h=" + h);

		try {
			DataMatrix dataMatrix = new DataMatrix();
			dataMatrix.setDataType(Symbol.DataType.GS1);
			dataMatrix.setSeparatorType(Symbol.SeparatorType.GS);
//			dataMatrix.setPreferredSize(8);				// 不指定的话，会根据内容自动确定大小；如果指定的话，则内容超过范围会失败
			dataMatrix.setForceMode(DataMatrix.ForceMode.SQUARE);
			dataMatrix.setContent(content);
//			Debug.d(TAG, "GS1 DM Height: " + dataMatrix.getHeight() + "; Actual Height: " + dataMatrix.getActualHeight() + "; Bar Height: " + dataMatrix.getBarHeight());

// H.M.Wang 2025-7-22 当打印头是hp22mm或者hp22mmx2时，生成打印缓冲区时宽度和高度缩小的倍率不一样，导致生成的二维码会按着短边生成一个缩小的图，修改为按着短边生成的图放大至全空间
			if(dataMatrix.getHeight() <= drawH) {
				Java2DRenderer renderer = new Java2DRenderer(drawH/dataMatrix.getHeight(), Color.WHITE, Color.BLACK);
// End of H.M.Wang 2025-7-22 当打印头是hp22mm或者hp22mmx2时，生成打印缓冲区时宽度和高度缩小的倍率不一样，导致生成的二维码会按着短边生成一个缩小的图，修改为按着短边生成的图放大至全空间
				renderer.setReverse(mRevert);
				if (isDynamicCode() || needRedraw()) {
					renderer.setInk(0xff0000ff);
				}
				Bitmap bitmap = renderer.render(dataMatrix);
//				Debug.d(TAG, "GS1 DM width: " + bitmap.getWidth() + "; height: " + bitmap.getHeight());

				Canvas canvas = new Canvas(outBmp);
// H.M.Wang 2025-7-22 当打印头是hp22mm或者hp22mmx2时，生成打印缓冲区时宽度和高度缩小的倍率不一样，导致生成的二维码会按着短边生成一个缩小的图，修改为按着短边生成的图放大至全空间
				canvas.drawBitmap(bitmap, (drawW - bitmap.getWidth())/2, (drawH - bitmap.getHeight())/2, new Paint());
// End of H.M.Wang 2025-7-22 当打印头是hp22mm或者hp22mmx2时，生成打印缓冲区时宽度和高度缩小的倍率不一样，导致生成的二维码会按着短边生成一个缩小的图，修改为按着短边生成的图放大至全空间
				bitmap.recycle();
			}
		} catch (Exception e) {
			Debug.e(TAG, e.getMessage());
/*			Canvas canvas = new Canvas(outBmp);
			Paint paint = new Paint();
			paint.setTextSize(h);
			paint.setTextScaleX(w/paint.measureText("X"));
			paint.setColor(Color.BLACK);
			paint.setTextAlign(Paint.Align.CENTER);
			canvas.drawText("X", w/2, h, paint);*/
		}

		Debug.d(TAG, "GS1 DM spent time: " + ((System.nanoTime() - startTime) / 1000000));
// H.M.Wang 2025-7-22 当打印头是hp22mm或者hp22mmx2时，生成打印缓冲区时宽度和高度缩小的倍率不一样，导致生成的二维码会按着短边生成一个缩小的图，修改为按着短边生成的图放大至全空间
        if((SystemConfigFile.getInstance().getPNozzle() == PrinterNozzle.MESSAGE_TYPE_22MM || SystemConfigFile.getInstance().getPNozzle() == PrinterNozzle.MESSAGE_TYPE_22MMX2) &&
                (w != drawW || h != drawH)) {
            return Bitmap.createScaledBitmap(outBmp, w, h, false);
        }
// End of H.M.Wang 2025-7-22 当打印头是hp22mm或者hp22mmx2时，生成打印缓冲区时宽度和高度缩小的倍率不一样，导致生成的二维码会按着短边生成一个缩小的图，修改为按着短边生成的图放大至全空间
		return outBmp;
	}
// End of H.M.Wang 2023-11-21 追加GS1的QR和DM

	private Bitmap drawDataMatrix(String content, int w, int h) {
		DataMatrixWriter writer = new DataMatrixWriter();

// H.M.Wang 2019-12-21 修改DM生成器的调用方法，设置生成的DM码为正方形
		HashMap<EncodeHintType,Object> hints = new HashMap<EncodeHintType, Object>();
		hints.put(EncodeHintType.DATA_MATRIX_SHAPE, (mDMType == DM_TYPE_RECTANGLE ? SymbolShapeHint.FORCE_RECTANGLE : SymbolShapeHint.FORCE_SQUARE));
		hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H); // 设置纠错等级（可选）
// H.M.Wang 2024-12-6 修改DM码的生成方法，在生成DM码时就按四边缩小一个像素的方式生成
//		BitMatrix matrix = writer.encode(content, getBarcodeFormat(mFormat), w, h, hints);
// H.M.Wang 2025-7-22 当打印头是hp22mm或者hp22mmx2时，生成打印缓冲区时宽度和高度缩小的倍率不一样，导致生成的二维码会按着短边生成一个缩小的图，修改为按着短边生成的图放大至全空间
//        BitMatrix matrix = writer.encode(content, getBarcodeFormat(mFormat), w-2, h-2, hints);
        int drawW = w, drawH = h;
        BitMatrix matrix;
        if((SystemConfigFile.getInstance().getPNozzle() == PrinterNozzle.MESSAGE_TYPE_22MM || SystemConfigFile.getInstance().getPNozzle() == PrinterNozzle.MESSAGE_TYPE_22MMX2)) {
            if(mDMType == DM_TYPE_RECTANGLE) {
                drawH = Math.min(w*8, h*18)/18;
                drawW = drawH*18/8;
            } else {
                drawH = Math.min(w, h);
                drawW = drawH;
            }
        }
        matrix = writer.encode(content, getBarcodeFormat(mFormat), drawW-2, drawH-2, hints);
// End of H.M.Wang 2025-7-22 当打印头是hp22mm或者hp22mmx2时，生成打印缓冲区时宽度和高度缩小的倍率不一样，导致生成的二维码会按着短边生成一个缩小的图，修改为按着短边生成的图放大至全空间
//			BitMatrix matrix = encode(content, w, h, hints);
// End of H.M.Wang 2023-2-1 因为参数的width和height已经是目标宽高，因此不必再次调整位图大小
// End of H.M.Wang 2024-12-6 修改DM码的生成方法，在生成DM码时就按四边缩小一个像素的方式生成
//		BitMatrix matrix = writer.encode(content, getBarcodeFormat(mFormat), w, h);
// End of 2019-12-21 修改DM生成器的调用方法，设置生成的DM码为正方形

		int width = matrix.getWidth();
		int height = matrix.getHeight();
// H.M.Wang 2024-12-6 生成的DM码放置于申请大小中心（四边各留一个像素的空格）
//		int[] pixels = new int[width * height];
// H.M.Wang 2025-7-22 当打印头是hp22mm或者hp22mmx2时，生成打印缓冲区时宽度和高度缩小的倍率不一样，导致生成的二维码会按着短边生成一个缩小的图，修改为按着短边生成的图放大至全空间
//		int[] pixels = new int[w * h];
        int[] pixels = new int[drawW * drawH];
// End of H.M.Wang 2025-7-22 当打印头是hp22mm或者hp22mmx2时，生成打印缓冲区时宽度和高度缩小的倍率不一样，导致生成的二维码会按着短边生成一个缩小的图，修改为按着短边生成的图放大至全空间
// End of H.M.Wang 2024-12-6 生成的DM码放置于申请大小中心（四边各留一个像素的空格）

		Debug.d(TAG, "Content: " + content + "; drawW: " + drawW + "; drawH: " + drawH + "; Width: " + width + "; Height: " + height);
//		StringBuilder sb = new StringBuilder();
		for (int y = 0; y < height; y++)
		{
//			sb.append('\n');
			for (int x = 0; x < width; x++)
			{
				if (matrix.get(x, y))
				{
// H.M.Wang 2022-12-20 追加反白设置
//					pixels[y * width + x] = mReverse ? 0xffffffff : 0xff000000;
// H.M.Wang 2024-12-6 生成的DM码放置于申请大小中心（四边各留一个像素的空格）
//					pixels[y * w + x] = mRevert ? 0xffffffff : (needRedraw() ? 0xff0000ff : 0xff000000);
// H.M.Wang 2025-7-22 当打印头是hp22mm或者hp22mmx2时，生成打印缓冲区时宽度和高度缩小的倍率不一样，导致生成的二维码会按着短边生成一个缩小的图，修改为按着短边生成的图放大至全空间
//                    pixels[(y+1) * w + x+1] = mRevert ? 0xffffffff : (needRedraw() ? 0xff0000ff : 0xff000000);
					pixels[(y+1) * drawW + x+1] = mRevert ? 0xffffffff : (needRedraw() ? 0xff0000ff : 0xff000000);
// End of H.M.Wang 2025-7-22 当打印头是hp22mm或者hp22mmx2时，生成打印缓冲区时宽度和高度缩小的倍率不一样，导致生成的二维码会按着短边生成一个缩小的图，修改为按着短边生成的图放大至全空间
// End of H.M.Wang 2024-12-6 生成的DM码放置于申请大小中心（四边各留一个像素的空格）
// End of H.M.Wang 2022-12-20 追加反白设置
//					sb.append('#');
				} else {
// H.M.Wang 2022-12-20 追加反白设置
//					pixels[y * width + x] = mReverse ? 0xff000000 : 0xffffffff;
// H.M.Wang 2024-12-6 生成的DM码放置于申请大小中心（四边各留一个像素的空格）
//					pixels[y * w + x] = mRevert ? (needRedraw() ? 0xff0000ff : 0xff000000) : 0xffffffff;
// H.M.Wang 2025-7-22 当打印头是hp22mm或者hp22mmx2时，生成打印缓冲区时宽度和高度缩小的倍率不一样，导致生成的二维码会按着短边生成一个缩小的图，修改为按着短边生成的图放大至全空间
//                    pixels[(y+1) * w + x+1] = mRevert ? (needRedraw() ? 0xff0000ff : 0xff000000) : 0xffffffff;
					pixels[(y+1) * drawW + x+1] = mRevert ? (needRedraw() ? 0xff0000ff : 0xff000000) : 0xffffffff;
// End of H.M.Wang 2025-7-22 当打印头是hp22mm或者hp22mmx2时，生成打印缓冲区时宽度和高度缩小的倍率不一样，导致生成的二维码会按着短边生成一个缩小的图，修改为按着短边生成的图放大至全空间
// End of H.M.Wang 2024-12-6 生成的DM码放置于申请大小中心（四边各留一个像素的空格）
// End of H.M.Wang 2022-12-20 追加反白设置
//					sb.append('.');
				}
			}
		}
//		Debug.d(TAG, sb.toString());
		// 条码/二维码的四个边缘空出20像素作为白边
// H.M.Wang 2025-7-22 当打印头是hp22mm或者hp22mmx2时，生成打印缓冲区时宽度和高度缩小的倍率不一样，导致生成的二维码会按着短边生成一个缩小的图，修改为按着短边生成的图放大至全空间
//        Bitmap bitmap = Bitmap.createBitmap(w, h, Configs.BITMAP_CONFIG);
		Bitmap bitmap = Bitmap.createBitmap(drawW, drawH, Configs.BITMAP_CONFIG);
// End of H.M.Wang 2025-7-22 当打印头是hp22mm或者hp22mmx2时，生成打印缓冲区时宽度和高度缩小的倍率不一样，导致生成的二维码会按着短边生成一个缩小的图，修改为按着短边生成的图放大至全空间
//		Bitmap bitmap = Bitmap.createBitmap(w, h, Configs.BITMAP_CONFIG);
// H.M.Wang 2025-7-22 当打印头是hp22mm或者hp22mmx2时，生成打印缓冲区时宽度和高度缩小的倍率不一样，导致生成的二维码会按着短边生成一个缩小的图，修改为按着短边生成的图放大至全空间
//        bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
		bitmap.setPixels(pixels, 0, drawW, 0, 0, drawW, drawH);
// End of H.M.Wang 2025-7-22 当打印头是hp22mm或者hp22mmx2时，生成打印缓冲区时宽度和高度缩小的倍率不一样，导致生成的二维码会按着短边生成一个缩小的图，修改为按着短边生成的图放大至全空间

		// H.M.Wang 修改返回值一行
// H.M.Wang 2023-2-1 因为参数的width和height已经是目标宽高，因此不必再次调整位图大小
// H.M.Wang 2025-7-22 当打印头是hp22mm或者hp22mmx2时，生成打印缓冲区时宽度和高度缩小的倍率不一样，导致生成的二维码会按着短边生成一个缩小的图，修改为按着短边生成的图放大至全空间
        if((SystemConfigFile.getInstance().getPNozzle() == PrinterNozzle.MESSAGE_TYPE_22MM || SystemConfigFile.getInstance().getPNozzle() == PrinterNozzle.MESSAGE_TYPE_22MMX2) &&
            (drawW != w || drawH != h)) {
            return Bitmap.createScaledBitmap(bitmap, w, h, false);
        }
// End of H.M.Wang 2025-7-22 当打印头是hp22mm或者hp22mmx2时，生成打印缓冲区时宽度和高度缩小的倍率不一样，导致生成的二维码会按着短边生成一个缩小的图，修改为按着短边生成的图放大至全空间
		return bitmap;
//		return Bitmap.createScaledBitmap(bitmap, w, h, false);
// End of H.M.Wang 2023-2-1 因为参数的width和height已经是目标宽高，因此不必再次调整位图大小
//			return Bitmap.createScaledBitmap(bitmap, (int) mWidth, (int) mHeight, false);
//		return bitmap;
	}

	private int[] BitMatrix2IntArray(BitMatrix matrix, int width, int height) {
		int[] pixels = new int[width * height];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if (matrix.get(x, y)) {
					pixels[y * width + x] = 0xff000000;
				} else {
					pixels[y * width + x] = 0xffffffff;
				}
			}
		}
		return pixels;
	}

	// H.M.Wang 2020-6-19 修改EAN-13和EAN-8的图片输出格式
	private Bitmap drawEAN8(int w, int h) {
		BitMatrix matrix=null;

		Debug.d(TAG, "--->drawEAN8 w : " + w + "  h: " + h);

		try {
			int textH = (h * mTextSize) / 100;

			MultiFormatWriter writer = new MultiFormatWriter();
			Hashtable<EncodeHintType, String> hints = new Hashtable<EncodeHintType, String>();
			hints.put(EncodeHintType.CHARACTER_SET, CODE);
			BarcodeFormat format = getBarcodeFormat(mFormat);

			Debug.d(TAG, "--->drawEAN8 mContent: " + mContent);
			String content = check();
			Debug.d(TAG, "--->drawEAN8 content: " + content);
			matrix = writer.encode(content, format, w, h-textH/2, null);

			int tl[] = matrix.getTopLeftOnBit();
			int width = matrix.getWidth();
			int height = matrix.getHeight();
			int br[] = matrix.getBottomRightOnBit();
			Debug.d(TAG, "width="+ width +", height="+height + "， left=" + tl[0] + "， top=" + tl[1] + "， right=" + br[0] + "， bottom=" + br[1]);
			int[] pixels = BitMatrix2IntArray(matrix, width, height);

			Bitmap bmp = Bitmap.createBitmap(width, height, Configs.BITMAP_CONFIG);
			bmp.setPixels(pixels, 0, width, 0, 0, width, height);
			Bitmap bitmap = Bitmap.createBitmap(width, h, Configs.BITMAP_CONFIG);
			Canvas can = new Canvas(bitmap);

			if(mShow) {
				int left = tl[0];
				int top = tl[1];
				float modWidth = 1.0f * (br[0]-tl[0]+1) / 67;		// [3 28 5 28 3]

				Paint paint = new Paint();
				paint.setColor(Color.WHITE);
				paint.setAntiAlias(true);
				paint.setFilterBitmap(true);

				can.drawBitmap(bmp, 0, 0, paint);

				paint.setStyle(Paint.Style.FILL);

				// 清空文字区背景
				can.drawRect(left + modWidth * 3, top + h - textH - 5, left + modWidth * 31, top + h, paint);
				can.drawRect(left + modWidth * 36, top + h - textH - 5, left + modWidth * 64, top + h, paint);

				paint.setTextSize(textH);

				float numWid = paint.measureText("0");
				float numDispWid = 1.0f * 28 * modWidth / 4;

				paint.setTextScaleX(Math.min(2, numDispWid/numWid));
				paint.setColor(Color.BLACK);
				paint.setTextAlign(Paint.Align.CENTER);

				// 写入文字区
				can.drawText(content.substring(0, 4), left + modWidth * 17, h-3, paint);
				can.drawText(content.substring(4, 8), left + modWidth * 50, h-3, paint);
//				for(int i=0; i<4; i++) {
//					can.drawText(content.substring(i, i+1), left + modWidth * 3 + (i+0.5f)* numDispWid, h-3, paint);
//					can.drawText(content.substring(i+4, i+5), left + modWidth * 36 + (i+0.5f) * numDispWid, h-3, paint);
//				}
			}

			return Bitmap.createScaledBitmap(bitmap, w, h, false);
		} catch (Exception e) {
			Debug.d(TAG, "--->exception: " + e.getMessage());
		}
// H.M.Wang 2020-8-10 如果条码格式有误，会在这里返回null，但是后续没有对null的情况进行排查，所以会导致异常发生，如EAN8赋值了非数字就会发生这种情形，改为返回一个空的bitmap
		return Bitmap.createBitmap(w, h, Configs.BITMAP_CONFIG);
//		return null;
// End of H.M.Wang 2020-8-10 如果条码格式有误，会在这里返回null，但是后续没有对null的情况进行排查，所以会导致异常发生，如EAN8赋值了非数字就会发生这种情形，改为返回一个空的bitmap
	}

	private Bitmap drawEAN13(int w, int h) {
		BitMatrix matrix=null;

		Debug.d(TAG, "--->drawEAN13 w : " + w + "  h: " + h);

		try {
			int textH = (h * mTextSize) / 100;

			MultiFormatWriter writer = new MultiFormatWriter();
			Hashtable<EncodeHintType, String> hints = new Hashtable<EncodeHintType, String>();
			hints.put(EncodeHintType.CHARACTER_SET, CODE);
			BarcodeFormat format = getBarcodeFormat(mFormat);

			Debug.d(TAG, "--->drawEAN13 mContent: " + mContent);
			String content = check();
			Debug.d(TAG, "--->drawEAN13 content: " + content);
			matrix = writer.encode(content, format, w, h-textH/2, null);

			int tl[] = matrix.getTopLeftOnBit();
			int width = matrix.getWidth();
			int height = matrix.getHeight();
			int br[] = matrix.getBottomRightOnBit();
			Debug.d(TAG, "width="+ width +", height="+height + "， left=" + tl[0] + "， top=" + tl[1] + "， right=" + br[0] + "， bottom=" + br[1]);
			int[] pixels = BitMatrix2IntArray(matrix, width, height);

			Bitmap bmp = Bitmap.createBitmap(width, height, Configs.BITMAP_CONFIG);
			bmp.setPixels(pixels, 0, width, 0, 0, width, height);
			Bitmap bitmap = Bitmap.createBitmap(width, h, Configs.BITMAP_CONFIG);
			Canvas can = new Canvas(bitmap);

			if(mShow) {
				int left = tl[0];
				int top = tl[1];
				float modWidth = 1.0f * (br[0]-tl[0]+1) / 95;		// [3 42 5 42 3]

				Paint paint = new Paint();
				paint.setColor(Color.WHITE);
				paint.setAntiAlias(true);
				paint.setFilterBitmap(true);

				can.drawBitmap(bmp, 0, 0, paint);

				paint.setStyle(Paint.Style.FILL);

				// 清空文字区背景
				can.drawRect(left + modWidth * 3, top + h - textH - 5, left + modWidth * 45, top + h, paint);
				can.drawRect(left + modWidth * 50, top + h - textH - 5, left + modWidth * 92, top + h, paint);

				paint.setTextSize(textH);

				float numWid = paint.measureText("0");
				float numDispWid = 1.0f * 42 * modWidth / 6;

				paint.setTextScaleX(Math.min(2, numDispWid/numWid));
				paint.setColor(Color.BLACK);
				paint.setTextAlign(Paint.Align.CENTER);
// H.M.Wang 2021-10-6 修改显示编码内容，追加导入码的显示
				// 写入文字区
				can.drawText(content.substring(0, 1), left / 2, h-3, paint);
				can.drawText(content.substring(1, 7), left + modWidth * 24, h-3, paint);
				can.drawText(content.substring(7, 13), left + modWidth * 71, h-3, paint);
//				for(int i=0; i<6; i++) {
//					can.drawText(content.substring(i+1, i+2), left + modWidth * 3 + (i+0.5f)* numDispWid, h-3, paint);
//					can.drawText(content.substring(i+7, i+8), left + modWidth * 50 + (i+0.5f) * numDispWid, h-3, paint);
//				}
// End of H.M.Wang 2021-10-6 修改显示编码内容，追加导入码的显示
			}

			return Bitmap.createScaledBitmap(bitmap, w, h, false);
		} catch (Exception e) {
			Debug.d(TAG, "--->exception: " + e.getMessage());
		}
// H.M.Wang 2020-8-10 如果条码格式有误，会在这里返回null，但是后续没有对null的情况进行排查，所以会导致异常发生，如EAN8赋值了非数字就会发生这种情形，改为返回一个空的bitmap
		return Bitmap.createBitmap(w, h, Configs.BITMAP_CONFIG);
//		return null;
// End of H.M.Wang 2020-8-10 如果条码格式有误，会在这里返回null，但是后续没有对null的情况进行排查，所以会导致异常发生，如EAN8赋值了非数字就会发生这种情形，改为返回一个空的bitmap
	}
// End of H.M.Wang 2020-6-19 修改EAN-13和EAN-8的图片输出格式

	private Bitmap draw(String content, int w, int h) {
// H.M.Wang 2020-6-19 修改EAN-13和EAN-8的图片输出格式
//		if ("EAN13".equals(mFormat)) {
		if (BARCODE_FORMAT_EAN13.equals(mFormat)) {
			return drawEAN13(w, h);
		}

//		if ("EAN8".equals(mFormat)) {
		if (BARCODE_FORMAT_EAN8.equals(mFormat)) {
			return drawEAN8(w, h);
		}
// End of H.M.Wang 2020-6-19 修改EAN-13和EAN-8的图片输出格式

		BitMatrix matrix=null;
		int margin = 4;
//		if (h <= mTextSize) {
//			h = mTextSize + 10;
//		}
		int textH = (h * mTextSize) / 100;
		int width = w - 2 * margin;
		int height = h - textH - 2 * margin;
		Bitmap outBmp = Bitmap.createBitmap(w, h, Configs.BITMAP_CONFIG);

		Paint paint = new Paint();
		Debug.d(TAG, "--->draw TotalW : " + w + "; TotalH: " + h + "; textH = " + textH + "; barWidth: " + width + "; barHeight: " + height);
		try {
			Bitmap barBmp = Bitmap.createBitmap(w, h, Configs.BITMAP_CONFIG);

// H.M.Wang 2023-11-21 追加GS1的QR和DM
			if(BARCODE_FORMAT_GS1128.equalsIgnoreCase(mFormat)) {
				Code128 code128 = new Code128();
				code128.setDataType(Symbol.DataType.GS1);
				code128.unsetCc();
				code128.setContent(content);
				code128.setFontSize(0);
//				code128.setHumanReadableLocation(HumanReadableLocation.NONE);
				code128.setHumanReadableLocation(HumanReadableLocation.BOTTOM);

				Java2DRenderer renderer = new Java2DRenderer(10, Color.WHITE, Color.BLACK);
				code128.setBarHeight(height);
				barBmp = renderer.render(code128);
				if(barBmp.getWidth() != width) {
					barBmp = Bitmap.createScaledBitmap(barBmp, width, height, false);
				}
			} else {
// End of H.M.Wang 2023-11-21 追加GS1的QR和DM
				MultiFormatWriter writer = new MultiFormatWriter();
				Hashtable<EncodeHintType, String> hints = new Hashtable<EncodeHintType, String>();
				hints.put(EncodeHintType.CHARACTER_SET, CODE);
				// hints.put(EncodeHintType.MARGIN, margin);
				BarcodeFormat format = getBarcodeFormat(mFormat);

				Debug.d(TAG, "--->content: " + mContent + "   format:" + mFormat);
				/* 条形码的宽度设置:每个数字占70pix列  */
/*			if ("EAN13".equals(mFormat)) {
				content = checkSum();
			} else if ("EAN8".equals(mFormat)) {
				content = checkLen(8);
			} else if ("ITF_14".equals(mFormat)) {
				content = checkLen(14);
			} else if ("UPC_A".equals(mFormat)) {
				content = checkLen(11,12);
			}
*/
				content = check();

				matrix = writer.encode(content, format, width, height, null);

				int tl[] = matrix.getTopLeftOnBit();
				int br[] = matrix.getBottomRightOnBit();

				int[] pixels = new int[width * height];
				for (int y = 0; y < height; y++)
				{
					for (int x = 0; x < width; x++)
					{
						if (matrix.get(x, y))
						{
							pixels[y * width + x] = 0xff000000;
						} else {
							pixels[y * width + x] = 0xffffffff;
						}
					}
				}
				/* 条码/二维码的四个边缘空出20像素作为白边 */
				barBmp.setPixels(pixels, 0, width, 0, 0, width, height);

// H.M.Wang 2020-2-25 追加ITF_14边框有无的设置
				if (BARCODE_FORMAT_ITF_14.equals(mFormat) && mWithFrame) {
//			if ("ITF_14".equals(mFormat) && mWithFrame) {
//			if ("ITF_14".equals(mFormat)) {
					Canvas cvs = new Canvas(barBmp);
					paint.setStrokeWidth(STROKE_WIDTH * 2);
					cvs.drawLine(/* top */  0, STROKE_WIDTH, barBmp.getWidth(), STROKE_WIDTH, paint);
					cvs.drawLine(/* left */ STROKE_WIDTH, 0, STROKE_WIDTH, barBmp.getHeight(), paint);
					cvs.drawLine(/* right */barBmp.getWidth() - STROKE_WIDTH*2, 0, barBmp.getWidth() - STROKE_WIDTH*2, barBmp.getHeight(), paint);
					cvs.drawLine(/* bottom*/0, barBmp.getHeight() - STROKE_WIDTH *2, barBmp.getWidth(), barBmp.getHeight() - STROKE_WIDTH*2, paint);
// End of H.M.Wang 2020-2-25 追加ITF_14边框有无的设置
				}
			}

			if(mShow) {
				Bitmap code = createCodeBitmapFromDraw(content, width, textH);

				//BinCreater.saveBitmap(code, "barcode.png");
				Canvas can = new Canvas(outBmp);
				can.drawBitmap(barBmp, margin, margin, paint);
				can.drawBitmap(code, margin, height+margin, paint);
				BinFromBitmap.recyleBitmap(barBmp);
				BinFromBitmap.recyleBitmap(code);
			}

			// H.M.Wang 2019-9-26 因为传入的元素已经是mWidth和mHeight，因此直接使用参数
//			return Bitmap.createScaledBitmap(bitmap, (int) mWidth, (int) mHeight, false);
//			return bitmap;

		} catch (Exception e) {
			Debug.d(TAG, "--->exception: " + e.getMessage());
		}
// H.M.Wang 2020-8-10 如果条码格式有误，会在这里返回null，但是后续没有对null的情况进行排查，所以会导致异常发生，如EAN8赋值了非数字就会发生这种情形，改为返回一个空的bitmap
		return outBmp;
//		return null;
// End of H.M.Wang 2020-8-10 如果条码格式有误，会在这里返回null，但是后续没有对null的情况进行排查，所以会导致异常发生，如EAN8赋值了非数字就会发生这种情形，改为返回一个空的bitmap
	}

	public BitMatrix encode(String contents, int width, int height, Map<EncodeHintType, ?> hints)
			throws WriterException {

		if (contents.isEmpty()) {
			throw new IllegalArgumentException("Found empty contents");
		}

		if (width < 0 || height < 0) {
			throw new IllegalArgumentException("Requested dimensions are too small: " + width + 'x' + height);
		}

		ErrorCorrectionLevel errorCorrectionLevel = ErrorCorrectionLevel.L;
		int quietZone = QUIET_ZONE_SIZE;
		if (hints != null) {
			ErrorCorrectionLevel requestedECLevel = (ErrorCorrectionLevel) hints.get(EncodeHintType.ERROR_CORRECTION);
			if (requestedECLevel != null) {
				errorCorrectionLevel = requestedECLevel;
			}
			Integer quietZoneInt = (Integer) hints.get(EncodeHintType.MARGIN);
			if (quietZoneInt != null) {
				quietZone = quietZoneInt;
			}
		}
		try {
			contents = new String(contents.getBytes("UTF-8"), "ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			Debug.d(TAG, "--->e: " + e.getMessage());
		}
		QRCode code = Encoder.encode(contents, errorCorrectionLevel, hints);
		return renderResult(code, width, height, quietZone);
	}

	private BitMatrix drasDM() {
		return null;
	}
	// Note that the input matrix uses 0 == white, 1 == black, while the output
	// matrix uses
	// 0 == black, 255 == white (i.e. an 8 bit greyscale bitmap).
	//修改如下代码
	private static BitMatrix renderResult(QRCode code, int width, int height, int quietZone) {
		ByteMatrix input = code.getMatrix();
		if (input == null) {
			throw new IllegalStateException();
		}
		int inputWidth = input.getWidth();
		int inputHeight = input.getHeight();
		int qrWidth = inputWidth ;
		int qrHeight = inputHeight;
		int outputWidth = Math.max(width, qrWidth);
		int outputHeight = Math.max(height, qrHeight);

		int multiple = Math.min(outputWidth / qrWidth, outputHeight / qrHeight);
		// Padding includes both the quiet zone and the extra white pixels to
		// accommodate the requested
		// dimensions. For example, if input is 25x25 the QR will be 33x33
		// including the quiet zone.
		// If the requested size is 200x160, the multiple will be 4, for a QR of
		// 132x132. These will
		// handle all the padding from 100x100 (the actual QR) up to 200x160.

		int leftPadding = (outputWidth - (inputWidth * multiple)) / 2;
		int topPadding = (outputHeight - (inputHeight * multiple)) / 2;
/*
        if(leftPadding >= 0 ) {
            outputWidth = outputWidth - 2 * leftPadding ;
            leftPadding = 0;
        }
        if(topPadding >= 0) {
            outputHeight = outputHeight - 2 * topPadding;
            topPadding = 0;
        }
*/
		BitMatrix output = new BitMatrix(outputWidth, outputHeight);

		for (int inputY = 0, outputY = topPadding; inputY < inputHeight; inputY++, outputY += multiple) {
			// Write the contents of this row of the barcode
			for (int inputX = 0, outputX = leftPadding; inputX < inputWidth; inputX++, outputX += multiple) {
				if (input.get(inputX, inputY) == 1) {
					output.setRegion(outputX, outputY, multiple, multiple);
				}
			}
		}
		return output;
	}

	// H.M.Wang 2023-2-1 修改条码生成打印缓冲区的算法，原来的算法的步骤是(1) ZXING生成BitMatrix(21x21) -> 放大到变量的大小(一般152x152） -> 反白处理(152x152) -> (动态二维码）改蓝色处理(152x152) -> 变换为打印尺寸(如，32x32）
// 由于经过放大再缩小，即使都是按整数比例放大缩小，仍然会产生变形，导致生成的二维码无法识别(为32点打印头生成123456的二维码即无法识别），这是一个需要解决的问题。
// 另外，由于中间处理步骤太多，导致耗时严重，自身耗时以外，也会增加被线程调度或者内存清理所裹挟而大幅增加处理时间，这个是第二个要解决的问题
/*
	public Bitmap getPrintBitmap(int totalW, int totalH, int w, int h, int y) {
//		BitMatrix matrix=null;
//		Debug.d(TAG, "--->getPrintBitmap : totalW = " + totalW + "  w = " + w);
//		MultiFormatWriter writer = new MultiFormatWriter();
//		Hashtable<EncodeHintType, String> hints = new Hashtable<EncodeHintType, String>();
//        hints.put(EncodeHintType.CHARACTER_SET, CODE);
//        try {
//			matrix = writer.encode(mContent,
//					BarcodeFormat.QR_CODE, w, w, hints);
//			matrix = deleteWhite(matrix);
//        } catch (Exception e) {
//        	return null;
//        }
//		int width = matrix.getWidth();
//		int height = matrix.getHeight();
//		int[] pixels = new int[width * height];
//		for (int y1 = 0; y1 < height; y1++)
//		{
//			for (int x = 0; x < width; x++)
//			{
//				if (matrix.get(x, y1))
//				{
//					pixels[y1 * width + x] = 0xff000000;
//				} else {
//					pixels[y1 * width + x] = 0xffffffff;
//				}
//			}
//		}

		Bitmap bg = Bitmap.createBitmap(totalW, totalH, Configs.BITMAP_CONFIG);
		Canvas canvas = new Canvas(bg);
//		Bitmap bitmap = Bitmap.createBitmap(width, height, Configs.BITMAP_CONFIG);

//		bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
		Debug.d(TAG, "--->mFormat: " + mFormat);

		Bitmap bitmap = null;
// H.M.Wang 2020-7-31 动态条码以及超文本内容的条码均需要重新画图，不仅限于二维码

		isNeedRedraw = true;
		bitmap = getScaledBitmap(mContext);
// End of H.M.Wang 2020-7-31 动态条码以及超文本内容的条码均需要重新画图，不仅限于二维码
		canvas.drawColor(Color.WHITE);
		canvas.drawBitmap(Bitmap.createScaledBitmap(bitmap, w, h, true), 0, y, mPaint);
		return bg;

	}
*/
// H.M.Wang 2024-1-12 231230-115300001版本增加的超文本支持动态文本的功能，从在动态条码中实现改为在静态条码中实现，因此在生成打印缓冲区的时候，判断是否需要重画
	public boolean needRedraw() {
		Vector<BaseObject> subOjbs = mHTContent.getSubObjs();
		for (BaseObject object : subOjbs) {
			if(!(object instanceof TextObject)) return true;
		}
		return false;
	}

	public boolean containsDT() {
		Vector<BaseObject> subOjbs = mHTContent.getSubObjs();
		for (BaseObject object : subOjbs) {
			if(object instanceof DynamicText) return true;
		}
		return false;
	}
// End of H.M.Wang 2024-1-12 231230-115300001版本增加的超文本支持动态文本的功能，从在动态条码中实现改为在静态条码中实现，因此在生成打印缓冲区的时候，判断是否需要重画

	public Bitmap getPrintBitmap(int totalW, int totalH, int w, int h, int y) {
		Bitmap bg = Bitmap.createBitmap(totalW, totalH, Configs.BITMAP_CONFIG);
		Canvas canvas = new Canvas(bg);

		Bitmap bitmap = null;

// H.M.Wang 2023-8-23 这个getPrintBitmap是专门给动态二维码使用的，在打印过程中生成打印缓冲区的函数，此时不能以原有内容为依据，而是要使用桶里面的内容
// H.M.Wang 2024-1-12 231230-115300001版本增加的超文本支持动态文本的功能，从在动态条码中实现改为在静态条码中实现，因此静态条码也可能会被重画，这里只有动态条码才使用桶里面的内容
		if(isDynamicCode())	mHTContent.setContent(SystemConfigFile.getInstance().getBarcodeBuffer());
// End of H.M.Wang 2024-1-12 231230-115300001版本增加的超文本支持动态文本的功能，从在动态条码中实现改为在静态条码中实现，因此静态条码也可能会被重画，这里只有动态条码才使用桶里面的内容
// End of H.M.Wang 2023-8-23 这个getPrintBitmap是专门给动态二维码使用的，在打印过程中生成打印缓冲区的函数，此时不能以原有内容为依据，而是要使用桶里面的内容
		String cnt = mHTContent.getExpandedContent();
		mContent = cnt;

		check();

		if (!is2D()) {
			bitmap = draw(mContent, w, h);
		} else {
//			if (mFormat.equalsIgnoreCase("DM") || mFormat.equalsIgnoreCase("DATA_MATRIX")) {
			if (mFormat.equalsIgnoreCase(BARCODE_FORMAT_DM)) {
				bitmap = drawDataMatrix(mContent, w, h);
// H.M.Wang 2023-11-21 追加GS1的QR和DM
			} else if (mFormat.equalsIgnoreCase(BARCODE_FORMAT_GS1QR)) {
// H.M.Wang 2024-2-3 当生成GS1DM或者GS1QR的时候，如果缺少标签，则会异常返回，导致生成的二维码为空，导致img获取不到数据而频繁发送empty，导致频繁下发，频繁的向网络发送回馈消息，修改方法为如果没有标签则加入21标签
//				bitmap = drawOkapiQR(mContent, w, h);
// H.M.Wang 2024-2-20 追加一个GS1串口协议。该协议使用花括号作为AI的分隔符
//				bitmap = drawOkapiQR(((mContent.startsWith("[")  || mContent.startsWith("("))  ? mContent : "[21]" + mContent), w, h);
// H.M.Wang 2024-2-22 追加一个GS1网络协议。内容与DATA_SOURCE_GS1_BRACE一样，只是数据从LAN来，走650或者600命令
//				if(SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_GS1_BRACE) {
				if( SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_GS1_BRACE ||
						SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_LAN_GS1_BRACE ||
// End of H.M.Wang 2024-2-22 追加一个GS1网络协议。内容与DATA_SOURCE_GS1_BRACE一样，只是数据从LAN来，走650或者600命令
// H.M.Wang 2024-6-12 追加GS1-1，GS1-2，GS1-3的条码解析功能
						SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_GS1_1 ||
						SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_GS1_2 ||
						SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_GS1_3) {
// End of H.M.Wang 2024-6-12 追加GS1-1，GS1-2，GS1-3的条码解析功能
					Gs1.AIType = Gs1.AI_TYPE_BRACE;
					bitmap = drawOkapiQR((mContent.startsWith("{")  ? mContent : "{21}" + mContent), w, h);
					Gs1.AIType = Gs1.AI_TYPE_NORMAL;
				} else {
					bitmap = drawOkapiQR(((mContent.startsWith("[")  || mContent.startsWith("("))  ? mContent : "[21]" + mContent), w, h);
				}
// End of H.M.Wang 2024-2-20 追加一个GS1串口协议。该协议使用花括号作为AI的分隔符
// End of H.M.Wang 2024-2-3 当生成GS1DM或者GS1QR的时候，如果缺少标签，则会异常返回，导致生成的二维码为空，导致img获取不到数据而频繁发送empty，导致频繁下发，频繁的向网络发送回馈消息，修改方法为如果没有标签则加入21标签
			} else if (mFormat.equalsIgnoreCase(BARCODE_FORMAT_GS1DM)) {
// H.M.Wang 2024-2-3 当生成GS1DM或者GS1QR的时候，如果缺少标签，则会异常返回，导致生成的二维码为空，导致img获取不到数据而频繁发送empty，导致频繁下发，频繁的向网络发送回馈消息，修改方法为如果没有标签则加入21标签
//				bitmap = drawGS1Datamatrix(mContent, w, h);
// H.M.Wang 2024-2-20 追加一个GS1串口协议。该协议使用花括号作为AI的分隔符
//				bitmap = drawGS1Datamatrix(((mContent.startsWith("[")  || mContent.startsWith("(")) ? mContent : "[21]" + mContent), w, h);
// H.M.Wang 2024-2-22 追加一个GS1网络协议。内容与DATA_SOURCE_GS1_BRACE一样，只是数据从LAN来，走650或者600命令
//				if(SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_GS1_BRACE) {
				if( SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_GS1_BRACE ||
						SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_LAN_GS1_BRACE ||
// End of H.M.Wang 2024-2-22 追加一个GS1网络协议。内容与DATA_SOURCE_GS1_BRACE一样，只是数据从LAN来，走650或者600命令
// H.M.Wang 2024-6-12 追加GS1-1，GS1-2，GS1-3的条码解析功能
						SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_GS1_1 ||
						SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_GS1_2 ||
						SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_GS1_3) {
// End of H.M.Wang 2024-6-12 追加GS1-1，GS1-2，GS1-3的条码解析功能
					Gs1.AIType = Gs1.AI_TYPE_BRACE;
					bitmap = drawGS1Datamatrix((mContent.startsWith("{")  ? mContent : "{21}" + mContent), w, h);
					Gs1.AIType = Gs1.AI_TYPE_NORMAL;
				} else {
					bitmap = drawGS1Datamatrix(((mContent.startsWith("[")  || mContent.startsWith("("))  ? mContent : "[21]" + mContent), w, h);
				}
// End of H.M.Wang 2024-2-20 追加一个GS1串口协议。该协议使用花括号作为AI的分隔符
// End of H.M.Wang 2024-2-3 当生成GS1DM或者GS1QR的时候，如果缺少标签，则会异常返回，导致生成的二维码为空，导致img获取不到数据而频繁发送empty，导致频繁下发，频繁的向网络发送回馈消息，修改方法为如果没有标签则加入21标签
// End of H.M.Wang 2023-11-21 追加GS1的QR和DM
			} else {
				bitmap = drawQR(mContent, w, h);
//				mBitmap = drawLcfQR(content, (int) mHeight, (int) mWidth);
			}
		}

		canvas.drawColor(Color.WHITE);
		canvas.drawBitmap(bitmap, 0, y, mPaint);

		return bg;
	}
// End of H.M.Wang 2023-2-1 修改条码生成打印缓冲区的算法，

	public int[] getDotcount() {
		Bitmap bmp = getScaledBitmap(mContext);
		BinFileMaker maker = new BinFileMaker(mContext);

		// H.M.Wang 追加一个是否移位的参数
		int[] dots = maker.extract(bmp, mTask.getHeads(), false);
//		int[] dots = maker.extract(bmp, mTask.getHeads(),
//				(mTask.getNozzle() == PrinterNozzle.MESSAGE_TYPE_1_INCH ||
//						mTask.getNozzle() == PrinterNozzle.MESSAGE_TYPE_1_INCH_DUAL ||
//						mTask.getNozzle() == PrinterNozzle.MESSAGE_TYPE_1_INCH_TRIPLE ||
//						mTask.getNozzle() == PrinterNozzle.MESSAGE_TYPE_1_INCH_FOUR));
		return dots;
	}
	/*
        protected Bitmap createCodeBitmapFromTextView(String contents,int width,int height, boolean isBin) {
            int heads = mTask.getHeads();
            if (heads == 0) {
                heads = 1;
            }
            float div = (float) (4.0/heads);
            Debug.d(TAG, "===>width=" + width);
            width = (int) (width/div);
            TextView tv=new TextView(mContext);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
            tv.setLayoutParams(layoutParams);
            tv.setText(contents);
            tv.setTextSize(15);
            tv.setHeight(height);
            tv.setGravity(Gravity.CENTER_HORIZONTAL);
            tv.setWidth(width);
            tv.setDrawingCacheEnabled(true);
            tv.setTextColor(Color.BLACK);
            tv.measure(
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            tv.layout(0, 0, tv.getMeasuredWidth(),
                    tv.getMeasuredHeight());

            tv.buildDrawingCache();
            Bitmap bitmapCode=tv.getDrawingCache();
            Debug.d(TAG, "===>width=" + width + ", bmp width=" + bitmapCode.getWidth(
            return isBin?Bitmap.createScaledBitmap(bitmapCode, (int) (bitmapCode.getWidth()*div), bitmapCode.getHeight(), true) : bitmapCode;
        }
    */
	protected Bitmap createCodeBitmapFromDraw(String content, int width, int height) {
		if (TextUtils.isEmpty(content)) {
			return null;
		}
		Paint paint = new Paint();

		paint.setTextSize(height-2*paint.getFontMetrics().descent);
// H.M.Wang 2025-3-24 修改倍率的计算方法，根据字数计算
//		paint.setTextScaleX(2);
		paint.setTextScaleX(1.8f * width / height / content.length());
// End of H.M.Wang 2025-3-24 修改倍率的计算方法，根据字数计算
		paint.setColor(Color.BLACK);
		Bitmap bitmap = Bitmap.createBitmap(width, height, Configs.BITMAP_CONFIG);
		Canvas canvas = new Canvas(bitmap);
		//每个字符占的宽度
		int perPix = width/content.length();
		//字符本身的宽度
		for (int i = 0; i < content.length(); i++) {
			String n = content.substring(i, i+1);
			canvas.drawText(n, i*perPix + (int) ((perPix - paint.measureText(n))/2), height-paint.getFontMetrics().descent, paint);
		}
		return bitmap;
	}
	/* 2020-6-18 这个函数没用到，注释掉
        public int getBestWidth()
        {
            int width=0;
            BitMatrix matrix=null;
            try{
                MultiFormatWriter writer = new MultiFormatWriter();
                BarcodeFormat format = getBarcodeFormat(mFormat);
                if(is2D())
                {
                    Hashtable<EncodeHintType, String> hints = new Hashtable<EncodeHintType, String>();
                    hints.put(EncodeHintType.CHARACTER_SET, CODE);

                    matrix = writer.encode(mContent,
                                        format, (int)mWidth, (int)mHeight, null);
                } else {
                    matrix = writer.encode(mContent,
                            format, (int)mWidth, (int)mHeight);
                }
                width = matrix.getWidth();
                int height = matrix.getHeight();
                Debug.d(TAG, "mWidth="+mWidth+", width="+width);
            }
            catch(Exception e)
            {
                Debug.e(TAG, "exception:"+e.getMessage());
            }
            return width;
        }
    */
	private boolean is2D() {
		Debug.d(TAG, "is2D? " + mFormat);
		if (TextUtils.isEmpty(mFormat)) {
			return false;
		}
		if (mFormat.equalsIgnoreCase(BARCODE_FORMAT_QR)
				|| mFormat.equalsIgnoreCase(BARCODE_FORMAT_DM)
// H.M.Wang 2023-11-21 追加GS1的QR和DM
				|| mFormat.equalsIgnoreCase(BARCODE_FORMAT_GS1QR)
				|| mFormat.equalsIgnoreCase(BARCODE_FORMAT_GS1DM)
// End of H.M.Wang 2023-11-21 追加GS1的QR和DM
				|| mFormat.equalsIgnoreCase(BARCODE_FORMAT_RSS_AZTEC)
				|| mFormat.equalsIgnoreCase(BARCODE_FORMAT_RSS_PDF_417)) {
			return true;
		}
		return false;
	}

	private BarcodeFormat getBarcodeFormat(String format) {
		int i;
		if (BARCODE_FORMAT_CODE128.equals(format)) {
			return BarcodeFormat.CODE_128;
		} else if (BARCODE_FORMAT_CODE39.equals(format)) {
			return BarcodeFormat.CODE_39;
		} else if (BARCODE_FORMAT_CODE93.equals(format)) {
			return BarcodeFormat.CODE_93;
		} else if (BARCODE_FORMAT_CODABAR.equals(format)) {
			return BarcodeFormat.CODABAR;
		} else if (BARCODE_FORMAT_EAN8.equals(format)) {
			return BarcodeFormat.EAN_8;
		} else if (BARCODE_FORMAT_EAN13.equals(format)) {
			return BarcodeFormat.EAN_13;
		} else if (BARCODE_FORMAT_UPC_E.equals(format)) {
			return BarcodeFormat.UPC_E;
		} else if (BARCODE_FORMAT_UPC_A.equals(format)) {
			return BarcodeFormat.UPC_A;
		} else if (BARCODE_FORMAT_ITF_14.equals(format)) {
			return BarcodeFormat.ITF;
		} else if (BARCODE_FORMAT_RSS14.equals(format)) {
			return BarcodeFormat.RSS_14;
		} else if (BARCODE_FORMAT_RSS_EXPANDED.equals(format)) {
			return BarcodeFormat.RSS_EXPANDED;
		} else if (BARCODE_FORMAT_QR.equals(format)) {
			return BarcodeFormat.QR_CODE;
		} else if (BARCODE_FORMAT_DATA_MATRIX.equals(format)) {
			return BarcodeFormat.DATA_MATRIX;
		} else if (BARCODE_FORMAT_RSS_AZTEC.equals(format)) {
			return BarcodeFormat.AZTEC;
		} else if (BARCODE_FORMAT_RSS_PDF_417.equals(format)) {
			return BarcodeFormat.PDF_417;
		} else if (BARCODE_FORMAT_DM.equalsIgnoreCase(format)) {
			return BarcodeFormat.DATA_MATRIX;
		} else {
			return BarcodeFormat.CODE_128;
		}

	}

/*
	private static BitMatrix deleteWhite(BitMatrix matrix) {
        int[] rec = matrix.getEnclosingRectangle();
        int resWidth = rec[2] + 1;
        int resHeight = rec[3] + 1;
        Debug.d("BarcodeObject", "--->deleteWhite: " + resWidth + " " + resHeight );
        BitMatrix resMatrix = new BitMatrix(resWidth, resHeight);
        resMatrix.clear();
        for (int i = 0; i < resWidth; i++) {
            for (int j = 0; j < resHeight; j++) {
                if (matrix.get(i + rec[0], j + rec[1]))
                    resMatrix.set(i, j);
            }
        }
        return resMatrix;
    }
*/

	private String checkSum(int length) {
		String code = "";
		int odd = 0, even = 0;
		if (mContent.length() < length) {
			String add = "";
			for (int i = 0; i < length - mContent.length(); i++) {
				add += "0";
			}
			code = mContent + add;
		} else if (mContent.length() > length) {
			code = mContent.substring(0, length);
		} else {
			code = mContent;
		}
		Debug.d(TAG, "--->content: " + mContent);
		mContent = code;
		Debug.d(TAG, "--->code: " + code);
		for (int i = 0; i < code.length(); i++) {
			try {
				if (i%2 == 0) {
					odd += Integer.parseInt(code.substring(i, i+1));
				} else {
					even += Integer.parseInt(code.substring(i, i+1));
				}
			} catch (Exception e){
				Debug.e(TAG, "--->" + e.getMessage());
			}
		}
		int temp = odd * 3 + even;
		int sum = 10 - temp%10;
		if (sum >= 10) {
			sum = 0;
		}
		Debug.d(TAG, "--->sum: " + sum);
		code += sum;
		Debug.d(TAG, "--->code: " + code);
		return code;
	}
	/**
	 * 計算EAN13的校驗和
	 * 奇数位和：6 + 0 + 2 + 4 + 6 + 8 = 26
	 * 偶数位和：9 + 1 + 3 + 5 + 7 + 9 = 34
	 * 将奇数位和与偶数位和的三倍相加：26 + 34 * 3 = 128
	 * 取结果的个位数：128的个位数为8
	 * 用10减去这个个位数：10 - 8 = 2
	 * @return
	 */
	/*
		H.M.Wang 2021-10-6 计算EAN13的校验和的方法
		预输入的数字位数为12位：[N0 N1 N2 N3 N4 N5 N6 N7 N8 N9 NA NB]
		Sum-Even = N0 + N2 + N4 + N6 + N8 + NA
		Sum-Odd = N1 + N3 + N5 + N7 + N9 + NB
		Sum = Sum-Even + 3 * Sum-Odd
		C = 10 - Sum % 10
		组成EAN-13码：
		  N0 [N1 N2 N3 N4 N5 N6] [N7 N8 N9 NA NB C]
		[举例]	1 2 3 4 5 6 7 8 9 0 1 2
			Sum-Even = 1 + 3 + 5 + 7 + 9 + 1 = 26
			Sum-Odd = 2 + 4 + 6 + 8 + 0 + 2 = 22
			Sum = 26 + 22 * 3 = 92
			C = 8
			组成EAN-13码：1 [2 3 5 5 6 7][8 9 0 1 2 8]
	 */
	private String checkSum() {
		String code = "";
		int odd = 0, even = 0;
		if (mContent.length() < 12) {
			String add = "";
			for (int i = 0; i < 12 - mContent.length(); i++) {
				add += "0";
			}
			code = mContent + add;
		} else if (mContent.length() > 12) {
			code = mContent.substring(0, 12);
		} else {
			code = mContent;
		}
		Debug.d(TAG, "--->content: " + mContent);
		mContent = code;
		Debug.d(TAG, "--->code: " + code);
		for (int i = 0; i < code.length(); i++) {
			try {
				if (i%2 == 0) {
					odd += Integer.parseInt(code.substring(i, i+1));
				} else {
					even += Integer.parseInt(code.substring(i, i+1));
				}
			} catch (Exception e){
				Debug.e(TAG, "--->" + e.getMessage());
			}
		}
		int temp = odd + even * 3;
		int sum = 10 - temp%10;
		if (sum >= 10) {
			sum = 0;
		}
		Debug.d(TAG, "--->sum: " + sum);
		code += sum;
		Debug.d(TAG, "--->code: " + code);
		return code;
	}

	private String check() {
		String content = mContent;
		if (BARCODE_FORMAT_EAN13.equals(mFormat)) {
			content = checkSum();
		} else if (BARCODE_FORMAT_EAN8.equals(mFormat)) {
//			content = checkLen(8);
			content = checkSum(7);
		} else if (BARCODE_FORMAT_ITF_14.equals(mFormat)) {
			content = checkLen(14);
		} else if (BARCODE_FORMAT_UPC_A.equals(mFormat)) {
			content = checkLen(11,12);
		} else {

		}
		mContent = content;
		return mContent;
	}

	/**
	 * EAN8只支持8位長度
	 * @return
	 */
	private String checkLen(int dstLen) {
		int len = mContent.length();
		if (len < dstLen) {
			for (int i = 0; i < dstLen - len; i++) {
				mContent += "0";
			}
		} else if (mContent.length() > dstLen) {
			return mContent.substring(0, dstLen);
		}
		return mContent;
	}

	private String checkLen(int min, int max) {
		int len = mContent.length();
		if (len < min) {
			for (int i = 0; i < min - len; i++) {
				mContent += "0";
			}
		} else if (len > max) {
			mContent = mContent.substring(0, max - 1);
		}
		return mContent;
	}

	public String toString()
	{
		int dots = 152;//SystemConfigFile.getInstance(mContext).getParam(39);
		float prop = dots/Configs.gDots;

		StringBuilder builder = new StringBuilder(mId);							// Tag 1    对象编号
		if (BaseObject.OBJECT_TYPE_QR.equalsIgnoreCase(mId)) {
			builder.append("^")
					.append(BaseObject.floatToFormatString(getX()*2*prop, 5))		// Tag 2
					.append("^")
					.append(BaseObject.floatToFormatString(getY()*2*prop, 5))		// Tag 3
					.append("^")
					.append(BaseObject.floatToFormatString(getXEnd()*2*prop, 5))		// Tag 4
					.append("^")
					.append(BaseObject.floatToFormatString(getYEnd()*2*prop, 5))		// Tag 5
					.append("^")
					.append(BaseObject.intToFormatString(0, 1))		// Tag 6
					.append("^")
					.append(BaseObject.boolToFormatString(mDragable, 3))		// Tag 7
					.append("^")
// H.M.Wang 2024-10-24 追加DM码的种类选择
					.append("00" + mDMType + "^")												// Tag 8
//					.append("000^")												// Tag 8
// End of H.M.Wang 2024-10-24 追加DM码的种类选择
// H.M.Wang 2023-11-21 追加GS1的QR和DM
//				.append("DM".equalsIgnoreCase(mFormat) ? "001" : "000")		// Tag 9
					.append(BARCODE_FORMAT_QR.equalsIgnoreCase(mFormat) ? "000" :
							(BARCODE_FORMAT_DM.equalsIgnoreCase(mFormat) ? "001" :
									(BARCODE_FORMAT_GS1QR.equalsIgnoreCase(mFormat) ? "002" :
											(BARCODE_FORMAT_GS1DM.equalsIgnoreCase(mFormat) ? "003" : "000"))))		// Tag 9
// End of H.M.Wang 2023-11-21 追加GS1的QR和DM
					.append("^000^")											// Tag 10
					.append(BaseObject.boolToFormatString(mReverse, 3))		// Tag 11
					.append("^")
					.append("000")												// Tag 12
					.append("^")
					.append(BaseObject.boolToFormatString(mSource, 8))		// Tag 13
					.append("^")
// H.M.Wang 2022-12-20 追加反白设置
					.append(BaseObject.boolToFormatString(mRevert, 3))		// Tag 14
// End of H.M.Wang 2022-12-20 追加反白设置
					.append("^00000000^00000000^00000000^00000000^00000000^00000000^")		// Tag 15-20
// H.M.Wang 2020-7-31 追加超文本内容，条码的内容可能是超文本
//				.append(mContent);
					.append(mOrgContent);										// Tag 21
// End of H.M.Wang 2020-7-31 追加超文本内容，条码的内容可能是超文本
		} else {
			builder.append("^")
					.append(BaseObject.floatToFormatString(getX()*2*prop, 5))			// Tag 2    X开始坐标
					.append("^")
					.append(BaseObject.floatToFormatString(getY()*2*prop, 5))			// Tag 3    Y开始坐标
					.append("^")
					.append(BaseObject.floatToFormatString(getXEnd()*2*prop, 5))		// Tag 4    X终止坐标
					.append("^")
					.append(BaseObject.floatToFormatString(getYEnd()*2*prop, 5))		// Tag 5    Y终止坐标
					.append("^")
					.append(BaseObject.intToFormatString(0, 1))							// Tag 6    字符大小
					.append("^")
					.append(BaseObject.boolToFormatString(mDragable, 3))				// Tag 7    支持拖拉标识
					.append("^")
// H.M.Wang 2020-7-31 追加超文本内容，条码的内容可能是超文本
//			.append(BaseObject.floatToFormatString(mContent.length(), 3))		// Tag 8    条码字符长度
					.append(BaseObject.floatToFormatString(mOrgContent.length(), 3))		// Tag 8    条码字符长度
// End of H.M.Wang 2020-7-31 追加超文本内容，条码的内容可能是超文本
					.append("^")
					.append(mCode)														// Tag 9    条码类型
					.append("^")
					.append("000^")														// Tag 10   字符字体大小
					.append(BaseObject.boolToFormatString(mShow, 3))					// Tag 11   是否显示字符
					.append("^")
// H.M.Wang 2020-7-31 追加超文本内容，条码的内容可能是超文本
//			.append(mContent)													// Tag 12   条码字符内容
					.append(mOrgContent)													// Tag 12   条码字符内容
// End of H.M.Wang 2020-7-31 追加超文本内容，条码的内容可能是超文本
					.append("^")
					.append(BaseObject.boolToFormatString(mSource, 8))					// Tag 13   什么源？
					.append("^")
// H.M.Wang 2022-12-20 追加反白设置
					.append(BaseObject.boolToFormatString(mRevert, 3))		// Tag 14
// End of H.M.Wang 2022-12-20 追加反白设置
					.append("^00000000^00000000^0000^0000^")					// Tag 15-18
					.append(mFont)														// Tag 19   字体
//			.append("^000^")													// Tag 20
// H.M.Wang 2020-2-25 追加ITF_14边框有无的设置
					.append("^")														// Tag 20   (新)边框有无
					.append(BaseObject.boolToFormatString(mWithFrame, 3))				// Tag 20   (新)边框有无
					.append("^")														// Tag 20   (新)边框有无
// End of H.M.Wang 2020-2-25 追加ITF_14边框有无的设置
					.append(BaseObject.intToFormatString(mTextSize, 3));				// Tag 21   文本大小？
		}

		String str = builder.toString();
		//str += BaseObject.intToFormatString(mIndex, 3)+"^";
//		if (BaseObject.OBJECT_TYPE_QR.equalsIgnoreCase(mId)) {
//			str += mId+"^";
//			str += BaseObject.floatToFormatString(getX()*2*prop, 5)+"^";
//			str += BaseObject.floatToFormatString(getY()*2*prop, 5)+"^";
//			str += BaseObject.floatToFormatString(getXEnd()*2*prop, 5)+"^";
//			str += BaseObject.floatToFormatString(getYEnd()*2*prop, 5)+"^";
//			str += BaseObject.intToFormatString(0, 1)+"^";
//			str += BaseObject.boolToFormatString(mDragable, 3)+"^";
//			str += "000^000^000^000^000^";
//			str += BaseObject.boolToFormatString(mSource, 8) + "^";
//			str += "00000000^00000000^00000000^00000000^00000000^00000000^00000000" + "^";
//			str += mContent;
//		} else {
//			str += mId+"^";
//			str += BaseObject.floatToFormatString(getX()*2*prop, 5)+"^";
//			str += BaseObject.floatToFormatString(getY()*2*prop, 5)+"^";
//			str += BaseObject.floatToFormatString(getXEnd()*2*prop, 5)+"^";
//			str += BaseObject.floatToFormatString(getYEnd()*2*prop, 5)+"^";
//			str += BaseObject.intToFormatString(0, 1)+"^";
//			str += BaseObject.boolToFormatString(mDragable, 3)+"^";
//			str += BaseObject.floatToFormatString(mContent.length(), 3)+"^";
//			str += mCode +"^";
//			str += "000^";
//			str += BaseObject.boolToFormatString(mShow, 3)+"^";
//			str += mContent+"^";
//			str += BaseObject.boolToFormatString(mSource, 8) + "^";
//			str += "00000000^00000000^00000000^0000^0000^" + mFont + "^000" + "^";
//			str += BaseObject.intToFormatString(mTextSize, 3);
//		}
//		System.out.println("file string ["+str+"]");
		Debug.d(TAG, "toString = [" + str + "]");
		return str;
	}
}

/*
    EAN-13商品条码是表示EAN/UCC-13商品标识代码的条码符号，由左侧空白区、起始符、左侧数据符、中间分隔符、右侧数据符、校验符、终止符、右侧空白区及供人识别字符组成。
        左侧空白区：位于条码符号最左侧与空的反射率相同的区域，其最小宽度为11个模块宽。
        起始符：位于条码符号左侧空白区的右侧，表示信息开始的特殊符号，由3个模块组成。
        左侧数据符：位于起始符右侧，表示6位数字信息的一组条码字符，由42个模块组成。
        中间分隔符：位于左侧数据符的右侧，是平分条码字符的特殊符号，由5个模块组成。
        右侧数据符：位于中间分隔符右侧，表示5位数字信息的一组条码字符，由35个模块组成。
        校验符：位于右侧数据符的右侧，表示校验码的条码字符，由7个模块组成。
        终止符：位于条码符号校验符的右侧，表示信息结束的特殊符号，由3个模块组成。
        右侧空白区：位于条码符号最右侧的与空的反射率相同的区域，其最小宽度为7个模块宽。为保护右侧空白区的宽度，可在条码符号右下角加“>”符号。
        供人识读字符：位于条码符号的下方，是与条码字符相对应的供人识别的13位数字，最左边一位称前置码。供人识别字符优先选用OCR-B字符集，字符顶部和条码底部的最小距离为0.5个模块宽。标准版商品条码中的前置码印制在条码符号起始符的左侧。

 	@参照
 	http://www.labelmx.com/tech/CodeKown/Code/201809/4992.html
 */