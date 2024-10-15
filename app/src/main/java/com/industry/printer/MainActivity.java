package com.industry.printer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import android.app.Activity;
import android.app.Dialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TextView;

import com.industry.printer.FileFormat.QRReader;
import com.industry.printer.FileFormat.SystemConfigFile;
import com.industry.printer.Utils.ConfigPath;
import com.industry.printer.Utils.Configs;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.FileUtil;
import com.industry.printer.Utils.KZFileObserver;
import com.industry.printer.Utils.PlatformInfo;
import com.industry.printer.Utils.ToastUtil;
import com.industry.printer.Utils.ZipUtil;
import com.industry.printer.hardware.BarcodeScanParser;
import com.industry.printer.hardware.ExtGpio;
import com.industry.printer.hardware.FpgaGpioOperation;
import com.industry.printer.ui.CustomerDialog.ConfirmDialog;
import com.industry.printer.ui.CustomerDialog.DialogListener;
import com.industry.printer.ui.CustomerDialog.ImportDialog;
import com.industry.printer.ui.CustomerDialog.ImportDialog.IListener;
import com.industry.printer.ui.CustomerDialog.LoadingDialog;
import com.industry.printer.ui.CustomerDialog.ScreenSaveDialog;
import com.industry.printer.ui.Test.TestMain;
import com.printer.corelib.JarTest;

public class MainActivity extends Activity implements OnCheckedChangeListener, OnTouchListener, OnClickListener {

	public static final String TAG="MainActivity";
	public static final String ACTION_USB_PERMISSION="com.industry.printer.USB_PERMISSION";
	
	public Context mContext;
	private Activity mActivity;
	private String mLanguage;
	
	TabHost mTab;

	private RadioGroup mTabGroup;
	public RadioButton	mRadioCtl;
	public RadioButton	mRadioSet;
	public RadioButton	mRadioEdit;
	public RadioButton	mRadioCustom;
	public RadioButton	btn_Other;
	public TextView		mExtStatus;
	
	public ControlTabActivity 	mControlTab;
	public EditTabActivity		mEditTab;
	public EditMultiTabActivity mEditFullTab;
	public EditTabSmallActivity mEditSmallTab;
	public SettingsTabActivity	mSettingsTab;
	public Custom1TabActivity   mCustomTab1;
//	public Custom2TabActivity   mCustomTab2;
	public RelativeLayout 		mCustomExtra;

	public LinearLayout mCtrlExtra;
// H.M.Wang 2020-8-11 将原来显示在画面头部的墨量和减锁信息移至ControlTab
//	public TextView mCtrlTitle;
//	public TextView mCountdown;
// End of H.M.Wang 2020-8-11 将原来显示在画面头部的墨量和减锁信息移至ControlTab
	public TextView mEditTitle;
	public RelativeLayout mSettings;
	public TextView mSettingTitle;
	public LinearLayout mEditExtra;
	public TextView mDelete;
	public TextView mVersion;
	private TextView mVerTitle;
	private ImageButton mCopy;

//	public TextView		mQuitBtn;

	private RelativeLayout mPgBack;
	private RelativeLayout mPgFore;

	// H.M.Wang 2019-12-7 追加两个启动画面
// H.M.Wang 2023-8-18 将启动页面的两个图片从MainActivity移到WelcomeActivity
//	private ImageView mBlack05s;
//	private ImageView mLoading1s;
// End of H.M.Wang 2023-8-18 将启动页面的两个图片从MainActivity移到WelcomeActivity

	private LoadingDialog mProgressDialog;
	
	private boolean mScreensaveMode = false;
	private Dialog screenSaver;
	
	private TextView IP_address;// localhost ip
	private TextView mCode;// localhost ip

// H.M.Wang 2020-8-11 将原来显示在画面头部的墨量和减锁信息更改为显示时间
	private LinearLayout mCtrlTabTimeArea = null;
	private TextView mDispDate = null;
	private TextView mDispTime = null;
// End of H.M.Wang 2020-8-11 将原来显示在画面头部的墨量和减锁信息更改为显示时间

	static {
		System.loadLibrary("Hardware_jni");
	}
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			Debug.d(TAG, "--->stat: " + savedInstanceState.getBoolean("printing"));
		}
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		//setLocale();

// H.M.Wang 2023-6-25 新的用户定制界面
		if(Configs.UI_TYPE == Configs.UI_CUSTOMIZED0) {
			setContentView(R.layout.activity_main2);
		} else {
			setContentView(R.layout.activity_main);
		}
// End of H.M.Wang 2023-6-25 新的用户定制界面
		IP_address=(TextView)findViewById(R.id.IP_address);
		mCode = (TextView) findViewById(R.id.code);
		boolean isroot=false;
		mContext = getApplicationContext();
		mActivity = this;
		mLanguage = getResources().getConfiguration().locale.getLanguage();

		IP_address.setText(getLocalIpAddress());
		/*get write permission of ttyACM0*/
		//SystemProperties.set("ctl.start","mptty");

		String pinyinId = "";
		InputMethodManager inputManager = (InputMethodManager) getApplicationContext().getSystemService(INPUT_METHOD_SERVICE);
		List<InputMethodInfo> inputMethodInfos = inputManager.getInputMethodList();

//	    for (InputMethodInfo inputMethodInfo : inputMethodInfos) {
//	    		Debug.d(TAG, "inputMethod="+inputMethodInfo.getId());
//	            if (inputMethodInfo.getId().equals("com.android.inputmethod.pinyin/.PinyinIME")) {
//	                    inputManager.setInputMethod(getApplicationContext(), inputMethodInfo.getId());
//	            }
//	    }

        mTabGroup = (RadioGroup) findViewById(R.id.tab_group);
	    mRadioCtl = (RadioButton) findViewById(R.id.btn_control);
	    mRadioCtl.setOnCheckedChangeListener(this);
	    mRadioCtl.setOnTouchListener(this);
	    
	    mRadioEdit = (RadioButton) findViewById(R.id.btn_edit);
	    mRadioEdit.setOnCheckedChangeListener(this);
	    mRadioEdit.setOnTouchListener(this);
	    
	    mRadioSet = (RadioButton) findViewById(R.id.btn_setting);
	    mRadioSet.setOnCheckedChangeListener(this);
	    mRadioSet.setOnTouchListener(this);

		mRadioCustom = (RadioButton) findViewById(R.id.btn_custom);
		mRadioCustom.setOnCheckedChangeListener(this);
		mRadioCustom.setOnTouchListener(this);

	    btn_Other = (RadioButton) findViewById(R.id.btn_Other);
	   
	   
	    //mExtStatus = (TextView) findViewById(R.id.tv_counter_msg);
	    
		/*
		try {
			isroot = LinuxShell.isRoot(Runtime.getRuntime(), 50);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		Debug.d(TAG, "ControlTab get root "+ isroot);

		//mTab = getTabHost();
		
		//mTab.addTab(mTab.newTabSpec("Control").setIndicator(getResources().getString(R.string.ControlTab)).setContent(new Intent(this, ControlTabActivity.class)));
		//mTab.addTab(mTab.newTabSpec("Edit").setIndicator(getResources().getString(R.string.Edit)).setContent(new Intent(this, EditTabActivity.class)));
		//mTab.addTab(mTab.newTabSpec("Settings").setIndicator(getResources().getString(R.string.Settings)).setContent(new Intent(this, SettingsTabActivity.class)));
		/*adjust control tab title*/
		//for(int i=0;i<3; i++)
		//{
		//	TextView v1= (TextView) mTab.getTabWidget().getChildAt(i).findViewById(android.R.id.title);
		//	v1.setTextSize(40);
		//	v1.setGravity(Gravity.CENTER);
		//}
		
		//set current tab
		//mTab.setCurrentTab(0);
		
		/** system config does not load from USB, so no need to listen the usb attachment state*/
		registerBroadcast();
		//FpgaGpioOperation.updateSettings(this.getApplicationContext());

		initView();
//		Configs.initConfigs(mContext);
		String sDev = DeviceInfosr(mContext);
		if (sDev.indexOf("SoftwinerEvb") <= 0) {
			Intent intent = new Intent(getApplicationContext(), Socket_Control_Activity.class);
			startActivityForResult(intent, 0);
		}
		if (sDev.indexOf("SoftwinerEvb") >= 0) {
			btn_Other.setVisibility(View.GONE);
		}

		 btn_Other.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					
					Intent intent = new Intent();
					intent.setClass(MainActivity.this, Socket_Control_Activity.class);
					startActivity(intent);
					
				}
			});

// H.M.Wang 2021-5-21 追加用户特色页面显示开关标识
		if(SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_USER_MODE) == SystemConfigFile.USER_MODE_1) {
			mRadioCustom.setVisibility(View.VISIBLE);
			mRadioCustom.postDelayed(new Runnable() {
				@Override
				public void run() {
					mRadioCtl.setChecked(false);
					mRadioCustom.setChecked(true);
				}
			}, 500);
		}
// End of H.M.Wang 2021-5-21 追加用户特色页面显示开关标识
//		Settings.System.putInt(mContext.getContentResolver(), "rotate_screen", ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        BarcodeScanParser.setContext(this);

		/*App 鍚姩浠ュ悗锛屾壃澹板櫒鍝嶄袱澹�*/
		new Thread() {
			@Override
			public void run() {
// H.M.Wang 2024-5-6 增加开机拉低PE4和PE5
				ExtGpio.writeGpioTestPin('E', 4, 0);
				ExtGpio.writeGpioTestPin('E', 5, 0);
// End of H.M.Wang 2024-5-6 增加开机拉低PE4和PE5
				ExtGpio.playClick();
				try{
					Thread.sleep(500);
				} catch (Exception e) {
				}
				ExtGpio.playClick();
			}
		}.start();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean("printing", true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		mHander.removeMessages(ENTER_LOWLIGHT_MODE);
		mHander.sendEmptyMessageDelayed(ENTER_LOWLIGHT_MODE, 0*1000);
	}

	public void drawObjects()
	{
		
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		Debug.d(TAG, "--->onConfigurationChanged: " + newConfig.locale.getLanguage());

		if (!newConfig.locale.getLanguage().equals(mLanguage)) {
			onConfigChange();
		}
		mLanguage = newConfig.locale.getLanguage();
//		finish();
//		Intent intent = new Intent(this, MainActivity.class);
//		this.startActivity(intent);

	}
	
	
	@Override
	public void onBackPressed() {
		return ;
	}
	
//	public void setLocale()
//	{
//		Configuration config = getResources().getConfiguration(); 
//		DisplayMetrics dm = getResources().getDisplayMetrics(); 
//		config.locale = Locale.SIMPLIFIED_CHINESE; 
//		getResources().updateConfiguration(config, dm); 
//		LocalePicker.updateLocale(Locale.CHINA);
//	}
	
	private void initView() {
		mCtrlExtra = (LinearLayout) findViewById(R.id.ctrl_extra);
// H.M.Wang 2020-8-11 将原来显示在画面头部的墨量和减锁信息移至ControlTab
//		mCtrlTitle = (TextView) findViewById(R.id.ctrl_counter_view);
//		mCountdown = (TextView) findViewById(R.id.count_down);
// End of H.M.Wang 2020-8-11 将原来显示在画面头部的墨量和减锁信息移至ControlTab
		mEditTitle = (TextView) findViewById(R.id.edit_message_view);
		mEditExtra = (LinearLayout) findViewById(R.id.edit_extra);
		mDelete = (TextView) findViewById(R.id.delete);
		mDelete.setOnClickListener(this);
		
		mSettings = (RelativeLayout) findViewById(R.id.settings_view);
		mSettings.setOnClickListener(this);
		mSettingTitle = (TextView) findViewById(R.id.setting_ext_view);
// H.M.Wang 2021-4-16 追加机器类型码的取得和显示
		mSettingTitle.setText(PlatformInfo.getImgUniqueCode());
// End of H.M.Wang 2021-4-16 追加机器类型码的取得和显示

		mVersion = (TextView) findViewById(R.id.setting_version);
		mVerTitle = (TextView) findViewById(R.id.setting_version_key);

		mCustomExtra = (RelativeLayout) findViewById(R.id.custom_extra);

		mCopy = (ImageButton) findViewById(R.id.msg_transfer);
		mCopy.setOnClickListener(this);

		try {
			// InputStreamReader sReader = new InputStreamReader(getAssets().open("Version"));cd .
			// BufferedReader reader = new BufferedReader(sReader);
//			String ver = SystemPropertiesProxy.get(this, "ro.build.version.incremental", "1970");
			PackageInfo packageInfo = getApplicationContext().getPackageManager().getPackageInfo(getPackageName(), 0);
			mVersion.setText(packageInfo.versionName);
		} catch (Exception e) {
			
		}
		mControlTab = new ControlTabActivity();
		mEditTab = new EditTabActivity();
		mEditSmallTab = new EditTabSmallActivity();
		mEditFullTab = new EditMultiTabActivity();
		mSettingsTab = new SettingsTabActivity();
		if(SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_USER_MODE) == SystemConfigFile.USER_MODE_1) {
			mCustomTab1 = new Custom1TabActivity();
		}
/*
		if(Configs.USER_MODE == Configs.USER_MODE_2) {
			mCustomTab2 = new Custom2TabActivity();
		}
*/
		mControlTab.setCallback(mHander);
		
		mPgBack = (RelativeLayout) findViewById(R.id.btn_page_backward);
		mPgBack.setOnClickListener(this);
		mPgFore = (RelativeLayout) findViewById(R.id.btn_page_forward);
		mPgFore.setOnClickListener(this);

		// H.M.Wang 2019-12-7 追加两个启动画面
// H.M.Wang 2023-8-18 将启动页面的两个图片从MainActivity移到WelcomeActivity
//		mBlack05s = (ImageView) findViewById(R.id.image05s);
//		mLoading1s = (ImageView) findViewById(R.id.image1s);
//		mHander.sendEmptyMessageDelayed(SHUT_BLACK_IMAGE, 0);
// End of H.M.Wang 2023-8-18 将启动页面的两个图片从MainActivity移到WelcomeActivity
		// End -----------------

// H.M.Wang 2020-8-11 将原来显示在画面头部的墨量和减锁信息更改为显示时间
		mCtrlTabTimeArea = (LinearLayout) findViewById(R.id.time_disp_area);
		mDispDate = (TextView) findViewById(R.id.disp_date);
		mDispTime = (TextView) findViewById(R.id.disp_time);
// End of H.M.Wang 2020-8-11 将原来显示在画面头部的墨量和减锁信息更改为显示时间

/*
		mQuitBtn = (TextView) findViewById(R.id.quitBtn);
		mQuitBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(Configs.USER_MODE == Configs.USER_MODE_1 || Configs.USER_MODE == Configs.USER_MODE_2) {
					AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
					builder.setTitle(R.string.str_quit)
							.setMessage(R.string.str_quit_confirm)
							.setPositiveButton(R.string.str_ok, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
									mRadioCustom.setChecked(false);
									mRadioCtl.setChecked(true);
								}
							})
							.setNegativeButton(R.string.str_btn_cancel, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
								}
							})
							.create()
							.show();
				}
			}
		});
*/
		FragmentTransaction transaction = getFragmentManager().beginTransaction();
//		transaction.replace(R.id.tab_content, mControlTab);
//		transaction.commit();
		transaction.add(R.id.tab_content, mControlTab);
		if (PlatformInfo.getEditType() == PlatformInfo.LARGE_SCREEN) {
			transaction.add(R.id.tab_content, mEditFullTab);
		} else if (PlatformInfo.getEditType() == PlatformInfo.SMALL_SCREEN_FULL) {
			transaction.add(R.id.tab_content, mEditSmallTab);
		} else if (PlatformInfo.getEditType() == PlatformInfo.SMALL_SCREEN_PART) {
			transaction.add(R.id.tab_content, mEditTab);
		}
		transaction.add(R.id.tab_content, mSettingsTab);
		if(SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_USER_MODE) == SystemConfigFile.USER_MODE_1) {
			transaction.add(R.id.tab_content, mCustomTab1);
		}
/*		if(Configs.USER_MODE == Configs.USER_MODE_2) {
			transaction.add(R.id.tab_content, mCustomTab2);
		}*/
		JarTest.d(TAG, "JarTest");
		Debug.d(TAG, "===>transaction");
		// transaction.add(R.id.tv_counter_msg, mCtrlTitle);
		// transaction.add(R.id.tv_counter_msg, mEditTitle);
		// transaction.add(R.id.tv_counter_msg, mSettingTitle);
		transaction.commit();
		if (PlatformInfo.getEditType() == PlatformInfo.LARGE_SCREEN) {
			transaction.hide(mEditFullTab);
		} else if (PlatformInfo.getEditType() == PlatformInfo.SMALL_SCREEN_FULL) {
			transaction.hide(mEditSmallTab);
		} else if (PlatformInfo.getEditType() == PlatformInfo.SMALL_SCREEN_PART) {
			transaction.hide(mEditTab);
		}
		transaction.hide(mSettingsTab);
		if(SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_USER_MODE) == SystemConfigFile.USER_MODE_1) {
			transaction.hide(mCustomTab1);
		}
/*		if(Configs.USER_MODE == Configs.USER_MODE_2) {
			transaction.hide(mCustomTab2);
		}*/
		transaction.show(mControlTab);
		Debug.d(TAG, "===>show");


		PrinterApplication application = (PrinterApplication) mContext.getApplicationContext();
		application.registeQRCallback("QR.txt", new KZFileObserver.KZFileObserverInterface() {
			@Override
			public void onChanged() {
				QRReader.reInstance(MainActivity.this);
			}
		});
		application.registeQRCallback("QRlast.txt", new KZFileObserver.KZFileObserverInterface() {
			@Override
			public void onChanged() {
				QRReader.reInstance(MainActivity.this);
			}
		});

//		transaction.commit();
		mHander.sendEmptyMessage(REFRESH_TIME_DISPLAY);
		if(Configs.UI_TYPE != Configs.UI_CUSTOMIZED0) {
			mCtrlTabTimeArea.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
		if(arg1 == true) {
			ExtGpio.playClick();
		}
		FragmentTransaction fts = getFragmentManager().beginTransaction();
		switch (arg0.getId()) {
			case R.id.btn_control:
				if(arg1 == true) {
					fts.show(mControlTab);
					mCtrlExtra.setVisibility(View.VISIBLE);
// H.M.Wang 2020-8-11 将原来显示在画面头部的墨量和减锁信息更改为显示时间
					if(Configs.UI_TYPE != Configs.UI_CUSTOMIZED0) {
						mCtrlTabTimeArea.setVisibility(View.VISIBLE);
					}
//					mHander.sendEmptyMessage(REFRESH_TIME_DISPLAY);
// End of H.M.Wang 2020-8-11 将原来显示在画面头部的墨量和减锁信息更改为显示时间
				} else {
					fts.hide(mControlTab);
					mCtrlExtra.setVisibility(View.GONE);
// H.M.Wang 2020-8-11 将原来显示在画面头部的墨量和减锁信息更改为显示时间
					mCtrlTabTimeArea.setVisibility(View.GONE);
//					mHander.removeMessages(REFRESH_TIME_DISPLAY);
// End of H.M.Wang 2020-8-11 将原来显示在画面头部的墨量和减锁信息更改为显示时间
				}
				
				Debug.d(TAG, "====>control checked?"+arg1);
				break;
			case R.id.btn_edit:
				Debug.d(TAG, "====>edit checked?"+arg1);
				if( arg1 == true) {
					if (PlatformInfo.getEditType() == PlatformInfo.LARGE_SCREEN) {
						fts.show(mEditFullTab);
					} else if (PlatformInfo.getEditType() == PlatformInfo.SMALL_SCREEN_FULL) {
						fts.show(mEditSmallTab);
					} else if (PlatformInfo.getEditType() == PlatformInfo.SMALL_SCREEN_PART) {
						fts.show(mEditTab);
					}
					
					mEditExtra.setVisibility(View.VISIBLE);
				} else {
					if (PlatformInfo.getEditType() == PlatformInfo.LARGE_SCREEN) {
						fts.hide(mEditFullTab);
					} else if (PlatformInfo.getEditType() == PlatformInfo.SMALL_SCREEN_FULL) {
						fts.hide(mEditSmallTab);
					} else if (PlatformInfo.getEditType() == PlatformInfo.SMALL_SCREEN_PART) {
						fts.hide(mEditTab);
					}
					mEditExtra.setVisibility(View.GONE);
				}
				break;
			case R.id.btn_setting:
				Debug.d(TAG, "====>setting checked?"+arg1);
				
				if (arg1 == true) {
					Debug.d(TAG, "--->show SettingTab");
					fts.show(mSettingsTab);
					Debug.d(TAG, "--->show SettingTab ok");
					mSettings.setVisibility(View.VISIBLE);
					Debug.d(TAG, "--->show SettingTab visible");
					// mSettingTitle.setVisibility(View.VISIBLE);
					// mVersion.setVisibility(View.VISIBLE);
//					mHander.sendEmptyMessage(REFRESH_TIME_DISPLAY);
					mSettingTitle.setText(PlatformInfo.getImgUniqueCode());
				} else {
					fts.hide(mSettingsTab);
					mSettings.setVisibility(View.GONE);
					//mSettingTitle.setVisibility(View.GONE);
					//mVersion.setVisibility(View.GONE);
//					mHander.removeMessages(REFRESH_TIME_DISPLAY);
				}
				break;
			case R.id.btn_custom:
				Debug.d(TAG, "====>custom checked?"+arg1);

				if (arg1 == true) {
					Debug.d(TAG, "--->show CustomTab");
					if(SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_USER_MODE) == SystemConfigFile.USER_MODE_1) {
						mCustomTab1.setObjPath(mControlTab.mObjPath);
						mCustomTab1.setCtrlHandler(mControlTab.mHandler);
						mCustomTab1.refreshView();
						fts.show(mCustomTab1);
					}
/*					if(Configs.USER_MODE == Configs.USER_MODE_2) {
						mCustomTab2.setObjPath(mControlTab.mObjPath);
						mCustomTab2.setCtrlHandler(mControlTab.mHandler);
						mCustomTab2.refreshView();
						fts.show(mCustomTab2);
					}*/
					Debug.d(TAG, "--->show CustomTab ok");
					mCustomExtra.setVisibility(View.VISIBLE);
					Debug.d(TAG, "--->show CustomExtra visible");
				} else {
					if(SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_USER_MODE) == SystemConfigFile.USER_MODE_1) {
						fts.hide(mCustomTab1);
					}
/*					if(Configs.USER_MODE == Configs.USER_MODE_2) {
						fts.hide(mCustomTab2);
					}*/
					mCustomExtra.setVisibility(View.GONE);
				}
				break;
		}
		fts.commit();
	}

	/**
	 * 保存并打印信息
	 * @param message
	 */
	public void onSaveAndPrint(String message) {
	    Debug.d(TAG, "--->onSaveAndPrint");
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.hide(mEditSmallTab);
        transaction.show(mControlTab);
        transaction.commit();
        mRadioEdit.setChecked(false);
        mRadioSet.setChecked(false);
        mRadioCtl.setChecked(true);
        mControlTab.loadAndPrint(message);
	}

// H.M.Wang 2023-7-6 增加一个用户定义界面模式，长按预览区进入编辑页面，编辑当前任务
	public void onPreviewLongClicked(String objPath) {
		if(Configs.UI_TYPE == Configs.UI_CUSTOMIZED0) {
			onEditTitleChanged(objPath);
			mRadioEdit.setChecked(true);
			mEditSmallTab.openObjectDirectly(objPath);
		}
	}
// End of H.M.Wang 2023-7-6 增加一个用户定义界面模式，长按预览区进入编辑页面，编辑当前任务

	public static final int USB_STORAGE_ATTACHED = 0;
	public static final int REFRESH_TIME_DISPLAY = 1;
	public static final int UPDATE_COUNTER = 2;
	
	public static final int NET_CONNECTED = 3;
	public static final int NET_DISCONNECTED = 4;

	/** 低亮度模式 */
	public static final int ENTER_LOWLIGHT_MODE = 5;
	/** 低亮度模式 */
	public static final int QUIT_LOWLIGHT_MODE = 9;

	/** 屏保模式 */
	public static final int ENTER_SCREENSAVE_MODE = 8;
	/** 屏保模式 */
	public static final int QUIT_SCREENSAVE_MODE = 10;
	// H.M.Wang 2019-12-7 追加两个启动画面（一个黑屏，一个启动），相关事件定义
// H.M.Wang 2023-8-18 将启动页面的两个图片从MainActivity移到WelcomeActivity
//	private static final int SHUT_BLACK_IMAGE = 6;
//	private static final int SHUT_LOADING_IMAGE = 7;
// End of H.M.Wang 2023-8-18 将启动页面的两个图片从MainActivity移到WelcomeActivity
	// End ----

	public Handler mHander = new Handler(){
		
		public void handleMessage(Message msg) {
			switch (msg.what) {
			// H.M.Wang 2019-12-7 追加两个画面的关闭事件响应
// H.M.Wang 2023-8-18 将启动页面的两个图片从MainActivity移到WelcomeActivity
//			case SHUT_BLACK_IMAGE:
//				if(mBlack05s.getWidth() == 0) {
//					sendEmptyMessageDelayed(SHUT_BLACK_IMAGE, 200);
//				} else {
//					mBlack05s.setVisibility(View.GONE);
//					sendEmptyMessageDelayed(SHUT_LOADING_IMAGE, 4500);
//				}
//				break;
//			case SHUT_LOADING_IMAGE:
//				mLoading1s.setVisibility(View.GONE);
//				break;
// End of H.M.Wang 2023-8-18 将启动页面的两个图片从MainActivity移到WelcomeActivity
			case USB_STORAGE_ATTACHED:
				Debug.d(TAG, "--->reload system settings");
				mControlTab.loadMessage();
				mSettingsTab.reloadSettings();
				QRReader reader = QRReader.getInstance(MainActivity.this);
				reader.reInstance(MainActivity.this);
				
				break;
			case REFRESH_TIME_DISPLAY:
				Calendar calendar = Calendar.getInstance();
				int year = calendar.get(Calendar.YEAR);
				int month = calendar.get(Calendar.MONTH) + 1;
				int day = calendar.get(Calendar.DAY_OF_MONTH);
				int hour = calendar.get(Calendar.HOUR_OF_DAY);
				int min = calendar.get(Calendar.MINUTE);
				int second = calendar.get(Calendar.SECOND);
// H.M.Wang 2021-4-16 追加机器类型码的取得和显示
//				String time = String.format(getResources().getString(R.string.str_time_format), year, month, day, hour, min, second);
//				mSettingTitle.setText(time);
// End of H.M.Wang 2021-4-16 追加机器类型码的取得和显示
// H.M.Wang 2020-8-11 将原来显示在画面头部的墨量和减锁信息更改为显示时间
				mDispDate.setText(String.format("%04d-%02d-%02d", year, month, day));
				mDispTime.setText(String.format("%02d:%02d:%02d", hour, min, second));
// End of H.M.Wang 2020-8-11 将原来显示在画面头部的墨量和减锁信息更改为显示时间
				mHander.sendEmptyMessageDelayed(REFRESH_TIME_DISPLAY, 1000);
				break;
			case UPDATE_COUNTER:
//				mSettingsTab.setParam(17, msg.arg1);
//				mSettingsTab.mSysconfig.saveConfig();
				break;
				
			case NET_CONNECTED:
				IP_address.setText(getLocalIpAddress());
				break;
			case NET_DISCONNECTED:
				IP_address.setText("");
				break;
			case ENTER_LOWLIGHT_MODE:
				setScreenBrightness(true);
				break;
			case ENTER_SCREENSAVE_MODE:
				screenSaver = new ScreenSaveDialog(mActivity, getActivityBitmap(mActivity));
				screenSaver.show();
				break;
			case QUIT_LOWLIGHT_MODE:
				mHander.removeMessages(ENTER_LOWLIGHT_MODE);
				mHander.sendEmptyMessageDelayed(ENTER_LOWLIGHT_MODE, 3*60 * 1000);
				break;
			default:
				break;
			}
		}
	};
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		Debug.d(TAG, "--->onDestroy");
		FragmentTransaction fts = getFragmentManager().beginTransaction();
		fts.remove(mControlTab);
		fts.remove(mSettingsTab);
		fts.remove(mEditSmallTab);
		fts.remove(mEditFullTab);
		if(SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_USER_MODE) == SystemConfigFile.USER_MODE_1) {
			fts.remove(mCustomTab1);
		}
/*		if(Configs.USER_MODE == Configs.USER_MODE_2) {
			fts.remove(mCustomTab2);
		}*/
		FpgaGpioOperation.close();
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
//			Debug.d(TAG, "--->dispatch down event");
			Debug.d(TAG, "--->onTouch：" + event.getX() + ", " + event.getY());
			setScreenBrightness(false);
		}
		return super.dispatchTouchEvent(event);
	}

	@Override
	public boolean onTouch(View view, MotionEvent event) {
//		Debug.d(TAG, "--->onTouch：" + event.getX() + ", " + event.getY());
		switch(view.getId()) {
		case R.id.btn_control:
		case R.id.btn_edit:
		case R.id.btn_setting:
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
//				PWMAudio.Play();
			}
		default:
			break;
		}
		
		return false;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
//		return super.onKeyDown(keyCode, event);

		Debug.d(TAG, "Device: " + event.getDeviceId() + "; KeyCode: " + keyCode);
// H.M.Wang 2024-9-20 如果条码文面包括TAB，会被编码为61，这回导致不被识别为空格，看到就像空格没有被识别的假象，因此强制变为空格(62)
		if(keyCode == 61) keyCode = 62;
// End of H.M.Wang 2024-9-20 如果条码文面包括TAB，会被编码为61，这回导致不被识别为空格，看到就像空格没有被识别的假象，因此强制变为空格(62)
		BarcodeScanParser.append(keyCode, event.isShiftPressed());
//		mCode.setText(Global.readCode());
		if (event.getDeviceId() == 10) return false;
// H.M.Wang 2024-9-19 取消下传后续键盘处理函数，因为有可能包含61，导致误操作，当前可确认的现象是产生将ControlTabActivity类中的上下翻页键点按的效果
		return true;
//		return super.onKeyDown(keyCode, event);
// End of H.M.Wang 2024-9-19 取消下传后续键盘处理函数，因为有可能包含61，导致误操作，当前可确认的现象是产生将ControlTabActivity类中的上下翻页键点按的效果
	}

	private long mLastClick = 0;
	private int mClickCount = 0;

	private void divAAATxt() {
		try {
			BufferedReader reader = new BufferedReader(new FileReader("/mnt/sdcard/aaa.txt"));
			BufferedWriter writter = new BufferedWriter(new FileWriter("/mnt/sdcard/out.txt"));
			String line;
			do {
				line = reader.readLine();
				if (!TextUtils.isEmpty(line)) {
					if(line.indexOf("Send FPGA print data. head=0") > 0) {
						writter.write("\t" + line + "\n");
					} else if(line.indexOf("Send FPGA print data. head=1") > 0) {
						writter.write("\t\t" + line + "\n");
					} else if(line.indexOf("Send FPGA print data. head=2") > 0) {
						writter.write("\t\t\t" + line + "\n");
					} else if(line.indexOf("Send FPGA print data. head=3") > 0) {
						writter.write("\t\t\t\t" + line + "\n");
					} else if(line.indexOf("=== expand_data ===") > 0) {
						writter.write(line + "\n");
					} else if(line.indexOf("===>startPrint") > 0) {
						writter.write(line + "\n");
					} else if(line.indexOf("===>stopPrint") > 0) {
						writter.write(line + "\n");
					} else if(line.indexOf("sPrintPool[") > 0) {
						writter.write(line + "\n");
					}
				}
			} while(!TextUtils.isEmpty(line));
			reader.close();
			writter.close();
		} catch(IOException e) {
			Debug.e(TAG, e.getMessage());
		}
	}

	@Override
	public void onClick(View arg0) {
		switch (arg0.getId()) {
		case R.id.btn_page_backward:
			mEditSmallTab.scrollPageBack();
			break;
		case R.id.btn_page_forward:
			mEditSmallTab.scrollPageFore();
			break;
		case R.id.delete:
			mEditSmallTab.deleteSelected();
			break;
		case R.id.msg_transfer:
//			Debug.e(TAG, "Transfer Clicked!");
			showImportDialog();
//			divAAATxt();
			// setScreenBrightness(50);
			break;
		case R.id.settings_view:
//			Debug.e(TAG, "settings_view Clicked! " + (System.currentTimeMillis() - mLastClick));
			if(System.currentTimeMillis() - mLastClick > 1000) {
// H.M.Wang 2022-5-5 点击题目条空白处，关闭软键盘
				try {
					InputMethodManager imm = (InputMethodManager) MainActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
					if (imm.isActive()) {
						if (MainActivity.this.getCurrentFocus().getWindowToken() != null) {
							imm.hideSoftInputFromWindow(MainActivity.this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
						}
					}
				} catch(Exception e) {
					Debug.e(TAG, e.getMessage());
				}
// End of H.M.Wang 2022-5-5 点击题目条空白处，关闭软键盘
				mClickCount = 1;
			} else {
				mClickCount++;
			}
			mLastClick = System.currentTimeMillis();

			if(mClickCount >= 5) {
				mClickCount = 0;
//				GpioTestPopWindow gtp = new GpioTestPopWindow(MainActivity.this);
//				gtp.show(mSettings);
				mHander.removeMessages(ENTER_LOWLIGHT_MODE);
				TestMain tmp = new TestMain(MainActivity.this);
				tmp.show(mSettings);
			}
			break;
		default:
			break;
		}
	}

// H.M.Wang 2020-8-11 将原来显示在画面头部的墨量和减锁信息移至ControlTab
	public void setCtrlExtra(int count, int down) {
//		mCtrlTitle.setText(String.valueOf(count));
//		mCountdown.setText(String.valueOf(down));
	}
// End of H.M.Wang 2020-8-11 将原来显示在画面头部的墨量和减锁信息移至ControlTab

	public void onConfigChange() {
// H.M.Wang 2020-9-15 追加是否为空判断，如果工作于写入Smart卡验证码模式，这些变量为空
		if(null == mControlTab || null == mEditSmallTab || null == mSettingsTab) return;
// End of H.M.Wang 2020-9-15 追加是否为空判断，如果工作于写入Smart卡验证码模式，这些变量为空
		mControlTab.onConfigureChanged();
		mEditSmallTab.onConfigureChanged();
		mSettingsTab.onConfigureChanged();
// H.M.Wang 2021-11-24 取消新建字样的显示
//		String title = mEditTitle.getText().toString();
//		if (title != null && (title.equals("New") || title.equals("新建"))) {
//			mEditTitle.setText(R.string.str_filename_no);
//		}
// End of H.M.Wang 2021-11-24 取消新建字样的显示
		mVerTitle.setText(R.string.app_version);

		mDelete.setText(R.string.str_del);
	}
	
	/**
	 * when message title changed in edit tab 
	 * @param title
	 */
	public void onEditTitleChanged(String title) {
		mEditTitle.setText(title);
	}
	
	public void hideKeyboard() {
		InputMethodManager im = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        im.hideSoftInputFromWindow(mEditTitle.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
	}
	
	private ImportDialog mImportDialog;
	
	public void showImportDialog() {
		if (mImportDialog == null) {
			mImportDialog = new ImportDialog(this);
		}
		mImportDialog.setListener(new IListener() {
			@Override
			public void onImport() {
				msgImportOnly();
			}
			
			@Override
			public void onExport() {
				msgExport();
			}
			@Override
			public void onFlush() {
				confirmDialog();
			}
// H.M.Wang 2022-11-27 追加一个导入用户群组(User Group)的按钮
			@Override
			public void onImportUG() { importUG(); }
// End of H.M.Wang 2022-11-27 追加一个导入用户群组(User Group)的按钮
		});
		mImportDialog.show();
	}
	
	private void dismissLoading() {
		if (mProgressDialog != null) {
			mProgressDialog.dismiss();
		}
	}
	
	private void confirmDialog() {
		ConfirmDialog dialog = new ConfirmDialog(this, R.string.str_flush_tips);
		dialog.setListener(new DialogListener() {
			
			@Override
			public void onConfirm() {
				msgImport();
			}
			
		});
		dialog.show();
	}

// H.M.Wang 2022-11-27 追加一个用户群组(User Group)，实现群组信息从U盘导入
	private void importUG() {
		final ArrayList<String> usbs = ConfigPath.getMountedUsb();
		if (usbs.size() <= 0) {
			ToastUtil.show(mContext, R.string.toast_plug_usb);
			return;
		}

		final File ugFile = new File(usbs.get(0) + File.separator + "UG.txt");
		if(!ugFile.exists()) {
			ToastUtil.show(mContext, "UG.txt file not found");
			return;
		}

		if(!mControlTab.mObjPath.startsWith(Configs.USER_GROUP_PREFIX)) {
			ToastUtil.show(mContext, "Please select message with prefix 'UG-' first");
			return;
		}

		mProgressDialog = LoadingDialog.show(this, R.string.strCopying);

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					MessageTask.cleanUGFolder(mControlTab.mObjPath);

					InputStream instream = new FileInputStream(ugFile);
					if (instream != null) {
						InputStreamReader inputreader = new InputStreamReader(instream, "UTF-8");
						BufferedReader buffreader = new BufferedReader(inputreader);
						String line;
						ArrayList<String> subMsgs = new ArrayList<String>();
						final Semaphore semaphore = new Semaphore(1);

						while (true) {
							while(true) {
								try { semaphore.acquire(); break;} catch(InterruptedException e){};
							}
//							Debug.d(TAG, "Semaphore got");
							line = buffreader.readLine();

							if(line == null) {
								MessageTask.saveUGTLK(mControlTab.mObjPath, subMsgs);
								MessageTask.saveUGIndex(mControlTab.mObjPath, -1);
								mControlTab.initUGParams(mControlTab.mObjPath);
								break;
							}

							Debug.d(TAG, "UG.txt: " + line);
							String parts[] = line.split(",");
							if(parts.length != 2) {
								mCopy.post(new Runnable() {
									@Override
									public void run() {
										ToastUtil.show(mContext, "Invalid UG line");
									}
								});
								Debug.e(TAG, "Invalid line [" + line + "]");
//								Debug.d(TAG, "Semaphore release");
								semaphore.release();
								continue;
							}

							MessageTask msgTask = new MessageTask(mContext, mControlTab.mObjPath);
							msgTask.replaceUGTag(parts[1]);
							msgTask.setName(mControlTab.mObjPath + "/" + parts[0]);
							msgTask.createTaskFolderIfNeed();
							msgTask.save(new MessageTask.SaveProgressListener() {
								@Override
								public void onSaved() {
//									Debug.d(TAG, "Semaphore release");
									semaphore.release();
								}
							});

							subMsgs.add(parts[0]);
						}
					}
				} catch(Exception e) {
					Debug.e(TAG, e.getMessage());
					mCopy.post(new Runnable() {
						@Override
						public void run() {
							ToastUtil.show(mContext, R.string.str_build_tlk_fail);
						}
					});
				} finally {
					mCopy.post(new Runnable() {
						@Override
						public void run() {
							mProgressDialog.dismiss();
						}
					});
				}
			}
		}).start();
	}
// End of H.M.Wang 2022-11-27 追加一个用户群组(User Group)，实现群组信息从U盘导入

	/**
	 * import from USB to flash
	 */
	private void msgImportOnly() {
		
		final ArrayList<String> usbs = ConfigPath.getMountedUsb();
		if (usbs.size() <= 0) {
			ToastUtil.show(mContext, R.string.toast_plug_usb);
			return;
		}
		mProgressDialog = LoadingDialog.show(this, R.string.strCopying);
		
// H.M.Wang 2020-12-17 取消该文件的访问		FileUtil.deleteFolder(Configs.QR_LAST);
		Observable.just(Configs.SYSTEM_CONFIG_MSG_PATH, Configs.PICTURE_SUB_PATH, Configs.QR_DIR, Configs.FONT_DIR)
				.flatMap(new Func1<String, Observable<Map<String, String>>>() {

					@Override
					public Observable<Map<String, String>> call(String arg0) {
						// TODO Auto-generated method stub
						Map<String, String> src = new HashMap<String, String>();
						if (Configs.SYSTEM_CONFIG_MSG_PATH.equals(arg0)) {
							src.put("source",usbs.get(0) + arg0);
							src.put("dest", Configs.TLK_PATH_FLASH);
							src.put("tips", MainActivity.this.getString(R.string.tips_import_message));
						} else if (Configs.PICTURE_SUB_PATH.equals(arg0)) {
							src.put("source",usbs.get(0) + arg0);
							src.put("dest", Configs.CONFIG_PATH_FLASH + Configs.PICTURE_SUB_PATH);
							src.put("tips", MainActivity.this.getString(R.string.tips_import_resource));
						} else if ( Configs.QR_DIR.equals(arg0)) {
							src.put("source",usbs.get(0) + arg0);
							src.put("dest", Configs.CONFIG_PATH_FLASH + Configs.SYSTEM_CONFIG_DIR + Configs.QR_DIR);
							src.put("tips", MainActivity.this.getString(R.string.tips_import_sysconf));
						}
						else if (Configs.FONT_DIR.equals(arg0)) {
							src.put("source",usbs.get(0) + Configs.FONT_DIR_USB + File.separator + Configs.FONT_ZIP_FILE);
							src.put("dest", Configs.CONFIG_PATH_FLASH + File.separator + Configs.FONT_ZIP_FILE);
							src.put("tips", MainActivity.this.getString(R.string.tips_import_font));
						}
						Debug.d(TAG, "--->flatMap");
						return Observable.just(src);
					}
				})
				.map(new Func1<Map<String, String>, Observable<Void>>() {

					@Override
					public Observable<Void> call(Map<String, String> arg0) {
						try {
							//mProgressDialog.setMessage(arg0.get("tips"));
							FileUtil.copyDirectiory(arg0.get("source"), arg0.get("dest"));
							String dest = arg0.get("dest");

							if (dest.contains(Configs.FONT_ZIP_FILE)) {
								Debug.d(TAG, "--->unZipping....");
								//mProgressDialog.setMessage("Unzipp...");
								ZipUtil.UnZipFolder(Configs.CONFIG_PATH_FLASH + File.separator + Configs.FONT_ZIP_FILE, Configs.CONFIG_PATH_FLASH);
							}
						} catch (Exception e) {
							// TODO: handle exception
						}
						Debug.d(TAG, "--->map");
						return null;
					}
					
				})
				.subscribeOn(Schedulers.io())
				//.observeOn(AndroidSchedulers.mainThread())
				.subscribe(new Action1<Observable<Void>>() {
					@Override
					public void call(Observable<Void> arg0) {
						
					}
				}, 
				new Action1<Throwable>() {
					@Override
					public void call(Throwable arg0) {
						
					}
				}, 
				new Action0() {
					
					@Override
					public void call() {
						Debug.d(TAG, "--->complete");
						mProgressDialog.dismiss();
//						QRReader.reInstance(MainActivity.this);
						//ToastUtil.show(mContext, "finished!!!");
					}
				});
								
				
//		try {
//			// Messages
//			if (usbs != null && usbs.size() > 0) {
//				FileUtil.copyDirectiory(usbs.get(0)  + Configs.SYSTEM_CONFIG_MSG_PATH, Configs.TLK_PATH_FLASH);
//			// pictutes
//				FileUtil.copyDirectiory(usbs.get(0)  + Configs.PICTURE_SUB_PATH, Configs.CONFIG_PATH_FLASH + Configs.PICTURE_SUB_PATH);
//			// system
//				FileUtil.copyDirectiory(usbs.get(0)  + Configs.SYSTEM_CONFIG_DIR, Configs.CONFIG_PATH_FLASH + Configs.SYSTEM_CONFIG_DIR);
//			}
//			QRReader reader = QRReader.getInstance(MainActivity.this);
//			reader.reInstance(MainActivity.this);
//		} catch (Exception e) {
//			Debug.d(TAG, "--->msgImport e: " + e.getMessage());
//		}
//		mProgressDialog.dismiss();
//		ToastUtil.show(mContext, "finished!!!");
	}
	/**
	 * import from USB to flash
	 */
	private void msgImport() {
		final ArrayList<String> usbs = ConfigPath.getMountedUsb();
		if (usbs.size() <= 0) {
			ToastUtil.show(mContext, R.string.toast_plug_usb);
			return;
		}
		mProgressDialog = LoadingDialog.show(this, R.string.strCopying);
		
		
		Observable.just(Configs.SYSTEM_CONFIG_MSG_PATH, Configs.PICTURE_SUB_PATH, Configs.SYSTEM_CONFIG_DIR , Configs.FONT_DIR)
		.flatMap(new Func1<String, Observable<Map<String, String>>>() {

			@Override
			public Observable<Map<String, String>> call(String arg0) {
				// TODO Auto-generated method stub
				Map<String, String> src = new HashMap<String, String>();
				if (Configs.SYSTEM_CONFIG_MSG_PATH.equals(arg0)) {
					src.put("source",usbs.get(0) + arg0);
					src.put("dest", Configs.TLK_PATH_FLASH);
					src.put("tips", MainActivity.this.getString(R.string.tips_import_message));
				} else if (Configs.PICTURE_SUB_PATH.equals(arg0)) {
					src.put("source",usbs.get(0) + arg0);
					src.put("dest", Configs.CONFIG_PATH_FLASH + Configs.PICTURE_SUB_PATH);
					src.put("tips", MainActivity.this.getString(R.string.tips_import_resource));
				} else if ( Configs.SYSTEM_CONFIG_DIR.equals(arg0)) {
					src.put("source",usbs.get(0) + arg0);
					src.put("dest", Configs.CONFIG_PATH_FLASH + Configs.SYSTEM_CONFIG_DIR);
					src.put("tips", MainActivity.this.getString(R.string.tips_import_sysconf));
				}
				else if (Configs.FONT_DIR.equals(arg0)) {
					src.put("source",usbs.get(0) + Configs.FONT_DIR_USB + File.separator + Configs.FONT_ZIP_FILE);
					src.put("dest", Configs.CONFIG_PATH_FLASH + File.separator + Configs.FONT_ZIP_FILE);
					src.put("tips", MainActivity.this.getString(R.string.tips_import_font));
				}
				Debug.d(TAG, "--->flatMap: " + src.get("tips"));
				return Observable.just(src);
			}
		})
		.map(new Func1<Map<String, String>, Observable<Void>>() {

			@Override
			public Observable<Void> call(Map<String, String> arg0) {
				try {
					//mProgressDialog.setMessage(arg0.get("tips"));
					Debug.d(TAG, "--->map: " + arg0.get("source") + " -> " + arg0.get("dest"));
					FileUtil.copyClean(arg0.get("source"), arg0.get("dest"));
					String dest = arg0.get("dest");
					if (dest.endsWith(Configs.FONT_ZIP_FILE)) {
						ZipUtil.UnZipFolder(Configs.CONFIG_PATH_FLASH + File.separator + Configs.FONT_ZIP_FILE, Configs.CONFIG_PATH_FLASH);
					}
				} catch (Exception e) {
					// TODO: handle exception
				}
				return null;
			}
			
		})
		.subscribeOn(Schedulers.io())
		//.observeOn(AndroidSchedulers.mainThread())
		.subscribe(new Action1<Observable<Void>>() {
			@Override
			public void call(Observable<Void> arg0) {
				
			}
		}, 
		new Action1<Throwable>() {
			@Override
			public void call(Throwable arg0) {
				
			}
		}, 
		new Action0() {
			
			@Override
			public void call() {
				Debug.d(TAG, "--->complete");
				mProgressDialog.dismiss();
//				QRReader.reInstance(MainActivity.this);
				// ToastUtil.show(mContext, "finished!!!");
			}
		});
//		try {
//			// Messages
//			if (usbs != null && usbs.size() > 0) {
//				FileUtil.copyClean(usbs.get(0)  + Configs.SYSTEM_CONFIG_MSG_PATH, Configs.TLK_PATH_FLASH);
//			}
//			// system
//			if (usbs != null && usbs.size() > 0) {
//				FileUtil.copyClean(usbs.get(0)  + Configs.SYSTEM_CONFIG_DIR, Configs.CONFIG_PATH_FLASH + Configs.SYSTEM_CONFIG_DIR);
//			}
//			// pictutes
//			if (usbs != null && usbs.size() > 0) {
//				FileUtil.copyClean(usbs.get(0)  + Configs.PICTURE_SUB_PATH, Configs.CONFIG_PATH_FLASH + Configs.PICTURE_SUB_PATH);
//			}
//			QRReader reader = QRReader.getInstance(MainActivity.this);
//			reader.reInstance(MainActivity.this);
//		} catch (Exception e) {
//			Debug.d(TAG, "--->msgImport e: " + e.getMessage());
//		}
//		mProgressDialog.dismiss();
		
	}
	
	/**
	 * export out to USB from flash
	 */
	private void msgExport() {
		final ArrayList<String> usbs = ConfigPath.getMountedUsb();
		if (usbs.size() <= 0) {
			ToastUtil.show(mContext, R.string.toast_plug_usb);
			return;
		}
		
		mProgressDialog = LoadingDialog.show(this, R.string.strCopying);
		Observable.just(Configs.SYSTEM_CONFIG_MSG_PATH, Configs.PICTURE_SUB_PATH, Configs.SYSTEM_CONFIG_DIR, "print.bin", Configs.LOG_1, Configs.LOG_2)
		.flatMap(new Func1<String, Observable<Map<String, String>>>() {

			@Override
			public Observable<Map<String, String>> call(String arg0) {
				Debug.d(TAG, "--->flatMap: " + arg0);
				// TODO Auto-generated method stub
				Map<String, String> src = new HashMap<String, String>();
				if (Configs.SYSTEM_CONFIG_MSG_PATH.equals(arg0)) {
					src.put("source",Configs.TLK_PATH_FLASH);
					src.put("dest", usbs.get(0) + arg0);
					src.put("tips", MainActivity.this.getString(R.string.tips_export_message));
				} else if (Configs.PICTURE_SUB_PATH.equals(arg0)) {
					Debug.d(TAG, "--->copy pictures");
					src.put("source", Configs.CONFIG_PATH_FLASH + Configs.PICTURE_SUB_PATH);
					src.put("dest", usbs.get(0) + arg0);
					src.put("tips", MainActivity.this.getString(R.string.tips_export_resource));
				} else if ( Configs.SYSTEM_CONFIG_DIR.equals(arg0)) {
					src.put("source", Configs.CONFIG_PATH_FLASH + Configs.SYSTEM_CONFIG_DIR);
					src.put("dest", usbs.get(0) + arg0);
					src.put("tips", MainActivity.this.getString(R.string.tips_export_sysconf));
// H.M.Wang 2020-2-19 修改导出错误
				} else if (Configs.LOG_1.equals(arg0)) {
//				} else if (Configs.LOG_1.equals(Configs.LOG_1)) {
					src.put("source", Configs.LOG_1);
					src.put("dest", usbs.get(0) + "/log1.txt");
				} else if (Configs.LOG_2.equals(arg0)) {
//				} else if (Configs.LOG_1.equals(Configs.LOG_2)) {
					src.put("source", Configs.LOG_2);
					src.put("dest", usbs.get(0) + "/log2.txt");
// End of H.M.Wang 2020-2-19 修改导出错误
				} else {
					FileUtil.deleteFolder(usbs.get(0) + "/print.bin");
					src.put("source", "/mnt/sdcard/print.bin");
					src.put("dest", usbs.get(0) + "/print.bin");
				}
				Debug.d(TAG, "--->flatMap");
				return Observable.just(src);
			}
		})
		.map(new Func1<Map<String, String>, Observable<Void>>() {

			@Override
			public Observable<Void> call(Map<String, String> arg0) {
				try {
					Debug.d(TAG, "--->start copy");
					//mProgressDialog.setMessage(arg0.get("tips"));
					FileUtil.copyDirectiory(arg0.get("source"), arg0.get("dest"));
				} catch (Exception e) {
					// TODO: handle exception
					Debug.d(TAG, "--->copy e: " + e.getMessage());
				}
				Debug.d(TAG, "--->map");
				return null;
			}
			
		})
		.subscribeOn(Schedulers.io())
		//.observeOn(AndroidSchedulers.mainThread())
		.subscribe(new Action1<Observable<Void>>() {
			@Override
			public void call(Observable<Void> arg0) {
				
			}
		}, 
		new Action1<Throwable>() {
			@Override
			public void call(Throwable arg0) {
				
			}
		}, 
		new Action0() {
			
			@Override
			public void call() {
				Debug.d(TAG, "--->complete");
				mProgressDialog.dismiss();
//				ToastUtil.show(mContext, "finished!!!");
			}
		});
//		try  {
//			Debug.d(TAG, "--->msgExport");
//			if (usbs != null && usbs.size() > 0) {
//				// Messages
//				FileUtil.copyDirectiory(Configs.TLK_PATH_FLASH, usbs.get(0)  + Configs.SYSTEM_CONFIG_MSG_PATH);
//				// system
//				FileUtil.copyDirectiory(Configs.CONFIG_PATH_FLASH + Configs.SYSTEM_CONFIG_DIR, usbs.get(0)  + Configs.SYSTEM_CONFIG_DIR);
//				// pictutes
//				FileUtil.copyDirectiory(Configs.CONFIG_PATH_FLASH + Configs.PICTURE_SUB_PATH, usbs.get(0)  + Configs.PICTURE_SUB_PATH);
//				// print.bin
//				FileUtil.copyFile("/mnt/sdcard/print.bin", usbs.get(0) + "/print.bin");
//			}
//		} catch (Exception e) {
//			Debug.d(TAG, "--->msgExport e: " + e.getMessage());
//		}
//		mProgressDialog.dismiss();
//		ToastUtil.show(mContext, "finished!!!");
	}
	
	private void registerBroadcast() {
		IntentFilter filter = new IntentFilter();
		//filter.addDataScheme("file");
		//filter.addAction(UsbManager.EXTRA_PERMISSION_GRANTED);
		
		filter.addAction(ACTION_USB_PERMISSION);
		filter.addDataScheme("file");
		filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
		filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
		
		filter.addAction(PrinterBroadcastReceiver.BOOT_COMPLETED);
		filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		
		PrinterBroadcastReceiver mReceiver = new PrinterBroadcastReceiver(mHander);
		mContext.registerReceiver(mReceiver, filter);
		
	}
	
	private void setScreenBrightness(boolean save) {
		Debug.d(TAG, "--->setScreenBrightness. mScreensaveMode=" + mScreensaveMode + " ;save=" + save);
		if (save == false) {
			Debug.d(TAG, "--->setScreenBrightness. save == false");
			mHander.removeMessages(ENTER_LOWLIGHT_MODE);
//			mHander.removeMessages(ENTER_SCREENSAVE_MODE);
			mHander.sendEmptyMessageDelayed(ENTER_LOWLIGHT_MODE, 3*60 * 1000);
		} else {
			Debug.d(TAG, "--->setScreenBrightness. save == true");
			mHander.removeMessages(ENTER_LOWLIGHT_MODE);
//			mHander.sendEmptyMessageDelayed(ENTER_SCREENSAVE_MODE, 10 * 6 * 1000);
		}
		if (mScreensaveMode == save) {
			return;
		}
		mScreensaveMode = save;
		SystemConfigFile config = SystemConfigFile.getInstance(mContext);
		float percent = config.getParam(SystemConfigFile.INDEX_LIGHTNESS) / 100.0f;
// H.M.Wang 2023-7-17 3.5寸盘亮度固定为50，其余不变
		String info = PlatformInfo.getImgUniqueCode();
		if(info.startsWith("NNG3") || info.startsWith("ONG3") || info.startsWith("GZJ") || info.startsWith("NSM2")) {
			percent = 0.5f;
		}
// End of H.M.Wang 2023-7-17 3.5寸盘亮度固定为50，其余不变
		int brightness = mScreensaveMode ? (int)(255 * percent) : 255;
		Window window = getWindow();
		WindowManager.LayoutParams localLP = window.getAttributes();
		float f = brightness / 255.0f;
		Debug.d(TAG, "--->setScreenBrightness. f=" + f);
		localLP.screenBrightness = f;
		window.setAttributes(localLP);
	}

	public Bitmap getActivityBitmap(Activity activity) {
		// 获取windows中最顶层的view
		View view = activity.getWindow().getDecorView();
		view.buildDrawingCache();
		// 获取状态栏高度
		Rect rect = new Rect();
		view.getWindowVisibleDisplayFrame(rect);
		int statusBarHeights = rect.top;
		Display display = activity.getWindowManager().getDefaultDisplay();
		// 获取屏幕宽和高
		int widths = display.getWidth();
		int heights = display.getHeight();
		// 允许当前窗口保存缓存信息
		view.setDrawingCacheEnabled(true);
		// 去掉状态栏
		Bitmap bmp = Bitmap.createBitmap(view.getDrawingCache(), 0,
		statusBarHeights, widths, heights - statusBarHeights);
		// 销毁缓存信息
		view.destroyDrawingCache();
		return bmp;
	}
	// locahost ip
	public static String getLocalIpAddress() {  
			String hostIp = null;  
		    try {  
		        Enumeration nis = NetworkInterface.getNetworkInterfaces();  
		        InetAddress ia = null;  
		        while (nis.hasMoreElements()) {  
		            NetworkInterface ni = (NetworkInterface) nis.nextElement();
		            Enumeration<InetAddress> ias = ni.getInetAddresses();  
		            while (ias.hasMoreElements()) {  
		                ia = ias.nextElement(); 
		                Debug.d(TAG, "--->ipAddr: " + ia.getHostAddress());
		                if (ia instanceof Inet6Address) {  
		                    continue;// skip ipv6  
		                }  
		                String ip = ia.getHostAddress();  
		                if (!"127.0.0.1".equals(ip)) {  
		                    hostIp = ia.getHostAddress();  
		                    break;  
		                }  
		            }  
		        }  
		    } catch (SocketException e) {  
		        Log.i("error", "SocketException");  
		        e.printStackTrace();  
		    }  
		    return hostIp; 
		}


	// locahost ip__________________________________________________________	
	
	public void setDevNo(String no) {
		IP_address.setText(getLocalIpAddress() + " - Dev: " + no);
	}

	public String DeviceInfosr(Context context){

		PackageManager mPackageManager = context.getPackageManager();
		PackageInfo mPackageInfo = null, mp = null;
		try {
			mPackageInfo = mPackageManager.getPackageInfo(context.getPackageName(), PackageManager.GET_ACTIVITIES);
// H.M.Wang 2022-9-1 追加一个启动客户APK的机制，如果参数64为1，并且客户apk存在，则启动，否则不启动
			if(SystemConfigFile.getInstance(this).getParam(SystemConfigFile.INDEX_USER_APK_START) == 1) {
				mp = mPackageManager.getPackageArchiveInfo("/system/app/UserAPK.apk", PackageManager.GET_ACTIVITIES);
				if(null != mp ) {
					Debug.d(TAG, "Package Name: " + mp.applicationInfo.packageName);
					Intent mIntent =  getPackageManager().getLaunchIntentForPackage(mp.applicationInfo.packageName);
					if(mIntent != null) startActivity(mIntent);
				} else {
//					ToastUtil.show(this, "User APK start fail");
					Debug.e(TAG, "UserAPK.apk not found!");
				}
			}
// End of H.M.Wang 2022-9-1 追加一个启动客户APK的机制，如果参数64为1，并且客户apk存在，则启动，否则不启动
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
		PackageManager pm = mContext.getPackageManager();
		try {
			mPackageInfo = pm.getPackageInfo(mContext.getPackageName(), PackageManager.GET_ACTIVITIES);
		} catch (PackageManager.NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		String sDeviceInfor="versionName:"+ mPackageInfo.versionName;
		sDeviceInfor+=" versionCode:" + mPackageInfo.versionCode;
		sDeviceInfor+=" Build_version:" + Build.VERSION.RELEASE;

		sDeviceInfor+=" CPU ABI:"+Build.CPU_ABI;
		sDeviceInfor+=" Vendor:"+ Build.MANUFACTURER;
		sDeviceInfor+=" MODEL:"+ Build.MODEL;
		sDeviceInfor+=" SDK_INT:" + Build.VERSION.SDK_INT;
		sDeviceInfor+=" PRODUCT:" +  Build.PRODUCT;

		return sDeviceInfor;
	}

}
