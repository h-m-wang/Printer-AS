package com.industry.printer.ui.CustomerDialog;

import com.industry.printer.R;
import com.industry.printer.Utils.ConfigPath;
import com.industry.printer.Utils.Configs;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.ExportLog2Usb;
import com.industry.printer.Utils.ToastUtil;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

// H.M.Wang 2023-11-28 追加RelightableDialog作为所有对话窗的父类，用来支持点按屏幕点亮屏幕
public class ImportDialog extends RelightableDialog implements android.view.View.OnClickListener{
//public class ImportDialog extends Dialog implements android.view.View.OnClickListener{
// End of H.M.Wang 2023-11-28 追加RelightableDialog作为所有对话窗的父类，用来支持点按屏幕点亮屏幕
	public static final String TAG = ImportDialog.class.getSimpleName();
	
	private TextView mImport;
	private TextView mExport;
	private TextView mFlush;
// H.M.Wang 2022-11-27 追加一个导入用户群组(User Group)的按钮
	private TextView mImportUG;
// End of H.M.Wang 2022-11-27 追加一个导入用户群组(User Group)的按钮
// H.M.Wang 2023-9-4 追加一个输出log到U盘的按键
	private TextView mLog2Usb;
// End of H.M.Wang 2023-9-4 追加一个输出log到U盘的按键

	private RelativeLayout mCancel;
	private Button mBtncl;

	private IListener mListener;

	private static final int MESSAGE_EXPORT_START = 7;
	private static final int MESSAGE_EXPORT_FINISH = 8;

	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case MESSAGE_EXPORT_START:
					ToastUtil.show(getContext(), R.string.str_btn_start);
					break;
				case MESSAGE_EXPORT_FINISH:
					ToastUtil.show(getContext(), R.string.toast_save_success);
					break;
			}
		}
	};

	public ImportDialog(Context context) {
		super(context);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.setContentView(R.layout.import_dialog);
		
		mImport = (TextView) findViewById(R.id.ib_import);
		mExport = (TextView) findViewById(R.id.ib_export);
		mFlush = (TextView) findViewById(R.id.ib_flush);
// H.M.Wang 2022-11-27 追加一个导入用户群组(User Group)的按钮
		mImportUG = (TextView) findViewById(R.id.btn_import_ug);
// End of H.M.Wang 2022-11-27 追加一个导入用户群组(User Group)的按钮
// H.M.Wang 2023-9-4 追加一个输出log到U盘的按键
		mLog2Usb = (TextView) findViewById(R.id.btn_log_2usb);
// End of H.M.Wang 2023-9-4 追加一个输出log到U盘的按键
		mBtncl = (Button) findViewById(R.id.btn_cancel);

		mImport.setOnClickListener(this);
		mExport.setOnClickListener(this);
		mFlush.setOnClickListener(this);
// H.M.Wang 2022-11-27 追加一个导入用户群组(User Group)的按钮
		mImportUG.setOnClickListener(this);
// End of H.M.Wang 2022-11-27 追加一个导入用户群组(User Group)的按钮
// H.M.Wang 2023-9-4 追加一个输出log到U盘的按键
		mLog2Usb.setOnClickListener(this);
// End of H.M.Wang 2023-9-4 追加一个输出log到U盘的按键
		mBtncl.setOnClickListener(this);
	}

	public void setListener(IListener listener) {
		mListener = listener;
	}
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.ib_import:
				dismiss();
				if (mListener != null) {
					mListener.onImport();
				}
				break;
			case R.id.ib_export:
				dismiss();
				if (mListener != null) {
					mListener.onExport();
				}
				break;
			case R.id.ib_flush:
				dismiss();
				if (mListener != null) {
					mListener.onFlush();
				}
				break;
			case R.id.btn_cancel:
				dismiss();
				break;
// H.M.Wang 2022-11-27 追加一个导入用户群组(User Group)的按钮
			case R.id.btn_import_ug:
				dismiss();
				if (mListener != null) {
					mListener.onImportUG();
				}
				break;
// End of H.M.Wang 2022-11-27 追加一个导入用户群组(User Group)的按钮
// H.M.Wang 2023-9-4 追加一个输出log到U盘的按键
			case R.id.btn_log_2usb:
				final ArrayList<String> usbs = ConfigPath.getMountedUsb();
				if (usbs.size() <= 0) {
					ToastUtil.show(getContext(), R.string.toast_plug_usb);
					break;
				}
				dismiss();
				new Thread(new Runnable() {
					@Override
					public void run() {
						mHandler.sendEmptyMessage(MESSAGE_EXPORT_START);
						ExportLog2Usb.exportLog(usbs.get(0) + "/export.log");
						mHandler.sendEmptyMessage(MESSAGE_EXPORT_FINISH);
					}
				}).start();
				break;
// End of H.M.Wang 2023-9-4 追加一个输出log到U盘的按键
			default:
				break;	
		}
	}
	
	public interface IListener {
		public void onImport();
		public void onExport();
		public void onFlush();
		public void onImportUG();
	}
}
