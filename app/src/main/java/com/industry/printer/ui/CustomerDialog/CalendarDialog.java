package com.industry.printer.ui.CustomerDialog;


import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.logging.Logger;

import com.industry.printer.R;
import com.industry.printer.R.id;
import com.industry.printer.Utils.Configs;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.LibUpgrade;
import com.industry.printer.Utils.PackageInstaller;
import com.industry.printer.Utils.PlatformInfo;
import com.industry.printer.Utils.ToastUtil;
import com.industry.printer.WelcomeActivity;
import com.industry.printer.hardware.RTCDevice;

import android.R.string;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;

// H.M.Wang 2023-11-28 追加RelightableDialog作为所有对话窗的父类，用来支持点按屏幕点亮屏幕
public class CalendarDialog extends RelightableDialog {
//public class CalendarDialog extends Dialog {
// End of H.M.Wang 2023-11-28 追加RelightableDialog作为所有对话窗的父类，用来支持点按屏幕点亮屏幕
	public static final String TAG="CalendarDialog";

	private int mLayout;
	public Button mPositive;
	public Button mNegative;
	public Button mUpgrade;
	public DatePicker mDPicker;
	public TimePicker mTPicker;
	public EditText mYear;
	public EditText mMonth;
	public EditText mDate;
	public EditText mHour;
	public EditText mMinute;

	protected CalendarDialog(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	public  CalendarDialog(Context context, int resLayout)
	{
		super(context);
		mLayout = resLayout;
		Debug.d(TAG, "--->context="+context);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.setContentView(mLayout);
		
		//mDPicker = (DatePicker) findViewById(R.id.picker_date);
		//mTPicker = (TimePicker) findViewById(R.id.picker_time);
		mYear = (EditText) findViewById(R.id.et_year);
		mMonth = (EditText) findViewById(R.id.et_month);
		mDate = (EditText) findViewById(R.id.et_date);
		mHour = (EditText) findViewById(R.id.et_hour);
		mMinute = (EditText) findViewById(R.id.et_minute);
		
		Calendar c = Calendar.getInstance();
		// mDPicker.init(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH), null);
		// mTPicker.setIs24HourView(true);
		// mTPicker.setCurrentHour(c.get(Calendar.HOUR_OF_DAY));
		// mTPicker.setCurrentMinute(c.get(Calendar.MINUTE));
		mYear.setText(String.valueOf(c.get(Calendar.YEAR)));
		mMonth.setText(String.format("%02d", c.get(Calendar.MONTH) + 1));
		mDate.setText(String.format("%02d", c.get(Calendar.DAY_OF_MONTH)));
		mHour.setText(String.format("%02d", c.get(Calendar.HOUR_OF_DAY)));
		mMinute.setText(String.format("%02d", c.get(Calendar.MINUTE)));
		
		mPositive = (Button) findViewById(R.id.btn_setTimeOk);
		mPositive.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Calendar c = Calendar.getInstance();
				check();
				try {
// H.M.Wang 2023-9-24 设置时间增加秒的值
					c.set(Integer.parseInt(mYear.getText().toString()),
							Integer.parseInt(mMonth.getText().toString()) - 1,
							Integer.parseInt(mDate.getText().toString()),
							Integer.parseInt(mHour.getText().toString()),
//							Integer.parseInt(mMinute.getText().toString()));
							Integer.parseInt(mMinute.getText().toString()),
							0);
// End of H.M.Wang 2023-9-24 设置时间增加秒的值
					long when = c.getTimeInMillis();

					Debug.d(TAG, "===>setDate");
					if (when / 1000 < Integer.MAX_VALUE) {
						SystemClock.setCurrentTimeMillis(when);
					}
					RTCDevice rtcDevice = RTCDevice.getInstance(getContext());
					rtcDevice.syncSystemTimeToRTC(getContext());
				} catch (Exception e) {
					Debug.e(TAG, "--->" + e.getMessage());
				}
				dismiss();
			}
		});
		
		mNegative =  (Button) findViewById(R.id.btn_setTimeCnl);
		mNegative.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				dismiss();
			}
		});

		mUpgrade =  (Button) findViewById(id.btn_upgrade);
		mUpgrade.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (PlatformInfo.PRODUCT_SMFY_SUPER3.equals(PlatformInfo.getProduct())) {
					PackageInstaller installer = PackageInstaller.getInstance(CalendarDialog.super.getContext());
					boolean ret;
					if(WelcomeActivity.AVOID_CROSS_UPGRADE) {
						ret = installer.silentUpgrade3();
					} else {
						ret = installer.silentUpgrade();
					}
					if(ret) {
						dismiss();
						LoadingDialog.show(CalendarDialog.super.getContext(), R.string.str_upgrade_progress);
					} else {
// H.M.Wang 2024-1-3 追加ko的升级功能
						upgradeKOs();
// End of H.M.Wang 2024-1-3 追加ko的升级功能
					}
				}
			}
		});
	}

// H.M.Wang 2024-1-3 追加ko的升级功能
	private void upgradeKOs() {
		try {
			boolean needReboot = false;

			Process process = Runtime.getRuntime().exec("su");
			DataOutputStream os = new DataOutputStream(process.getOutputStream());
			Thread.sleep(100);

			LibUpgrade libUp = new LibUpgrade();
			needReboot |= libUp.upgradeKOs(os, Configs.PREFIX_FPGA_SUNXI_KO, Configs.FPGA_SUNXI_KO);
			needReboot |= libUp.upgradeKOs(os, Configs.PREFIX_EXT_GPIO_KO, Configs.EXT_GPIO_KO);
			needReboot |= libUp.upgradeKOs(os, Configs.PREFIX_GSLX680_KO, Configs.GSLX680_KO);
			needReboot |= libUp.upgradeKOs(os, Configs.PREFIX_RTC_DS1307_KO, Configs.RTC_DS1307_KO);

			if(needReboot) {
				ToastUtil.show(this.getContext(), "Rebooting...");
				Debug.e(TAG, "Reboot!!!");
				os.writeBytes("reboot\n");
			} else {
				ToastUtil.show(this.getContext(), "Upgrading failed!!!");
			}
			os.flush();
			os.close();

		} catch(IOException e) {
			Debug.e(TAG, e.getMessage());
			e.printStackTrace();
		} catch (InterruptedException e) {
			Debug.e(TAG, e.getMessage());
			e.printStackTrace();
		}

	}
// End of H.M.Wang 2024-1-3 追加ko的升级功能
	/**
	 * 验证时间是否有效
	 */
	
	private void check() {
		try {
			int year = Integer.parseInt(mYear.getText().toString());
			if (year > 2100 || year < 1970) {
				year = 1970;
				mYear.setText(String.valueOf(year));
			}
			int month = Integer.parseInt(mMonth.getText().toString());
			if (month > 12 || month < 1) {
				month = 1;
				mMonth.setText(String.valueOf(month - 1));
			}
			int date = Integer.parseInt(mDate.getText().toString());
			if (date > 31 || date < 1) {
				date = 1;
				mDate.setText(String.valueOf(date));
			}

			int hour = Integer.parseInt(mHour.getText().toString());
			if (hour < 0 || hour >= 24) {
				hour = 0;
				mHour.setText(String.valueOf(hour));
			}
			int minute = Integer.parseInt(mMinute.getText().toString());

			if (minute < 0 || minute >= 60) {
				minute = 0;
				mMinute.setText(String.valueOf(minute));
			}
			Calendar c = Calendar.getInstance();
			c.set(year, month, date, hour, minute);
		} catch (Exception e) {
			Debug.e(TAG, "--->" + e.getMessage());
		}
	}

	
}
