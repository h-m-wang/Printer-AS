package com.industry.printer.ui.CustomerDialog;

import com.industry.printer.R;
import com.industry.printer.Utils.ConfigPath;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.ExportLog2Usb;
import com.industry.printer.Utils.ToastUtil;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class ImportDialog extends Dialog implements android.view.View.OnClickListener{

	
	private ImageButton mImport;
	private ImageButton mExport;
	private ImageButton mFlush;
// H.M.Wang 2022-11-27 追加一个导入用户群组(User Group)的按钮
	private TextView mImportUG;
// End of H.M.Wang 2022-11-27 追加一个导入用户群组(User Group)的按钮
// H.M.Wang 2023-9-4 追加一个输出log到U盘的按键
	private TextView mLog2Usb;
	private Timer mTimer;
	private boolean mWrittingLog = false;
// End of H.M.Wang 2023-9-4 追加一个输出log到U盘的按键

	private RelativeLayout mCancel;
	private Button mBtncl;

	private IListener mListener;
	
	public ImportDialog(Context context) {
		super(context);
		
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.setContentView(R.layout.import_dialog);
		
		mImport = (ImageButton) findViewById(R.id.ib_import);
		mExport = (ImageButton) findViewById(R.id.ib_export);
		mFlush = (ImageButton) findViewById(R.id.ib_flush);
// H.M.Wang 2022-11-27 追加一个导入用户群组(User Group)的按钮
		mImportUG = (TextView) findViewById(R.id.btn_import_ug);
// End of H.M.Wang 2022-11-27 追加一个导入用户群组(User Group)的按钮

// H.M.Wang 2023-9-4 追加一个输出log到U盘的按键
		mLog2Usb = (TextView) findViewById(R.id.btn_log_2usb);
		mTimer = new Timer();
		mTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				mLog2Usb.post(new Runnable() {
					@Override
					public void run() {
						final ArrayList<String> usbs = ConfigPath.getMountedUsb();
						if (usbs.size() <= 0) {
							mLog2Usb.setTextColor(Color.GRAY);
							mLog2Usb.setClickable(false);
						} else {
							if(!mWrittingLog) {
								mLog2Usb.setTextColor(Color.BLUE);
								mLog2Usb.setClickable(true);
							}
						}
					}
				});
			}
		}, 0L, 500L);
// End of H.M.Wang 2023-9-4 追加一个输出log到U盘的按键

		mCancel = (RelativeLayout) findViewById(R.id.cancel);
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
		mCancel.setOnClickListener(this);
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
			case R.id.cancel:
			case R.id.btn_cancel:
// H.M.Wang 2023-9-4 追加一个输出log到U盘的按键
				if(null != mTimer) {
					mTimer.cancel();
					mTimer = null;
				}
// End of H.M.Wang 2023-9-4 追加一个输出log到U盘的按键
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
				mWrittingLog = true;
				mLog2Usb.setTextColor(Color.GRAY);
				ExportLog2Usb.exportLog(usbs.get(0) + "/export.log");
				mWrittingLog = false;
				mLog2Usb.setTextColor(Color.BLUE);
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
