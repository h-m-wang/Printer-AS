package com.industry.printer;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

import com.industry.printer.Bluetooth.BluetoothServerManager;
import com.industry.printer.FileFormat.SystemConfigFile;
import com.industry.printer.Utils.*;
import com.industry.printer.data.DataTask;
import com.industry.printer.hardware.ExtGpio;
import com.industry.printer.hardware.FpgaGpioOperation;
import com.industry.printer.hardware.Hp22mm;
import com.industry.printer.ui.ExtendMessageTitleFragment;
import com.industry.printer.ui.CustomerAdapter.SettingsListAdapter;
import com.industry.printer.ui.CustomerDialog.CalendarDialog;
import com.industry.printer.ui.CustomerDialog.CustomerDialogBase.OnPositiveListener;
import com.industry.printer.ui.CustomerDialog.CustomerDialogBase.OnNagitiveListener;;
import com.industry.printer.ui.CustomerDialog.PasswordDialog;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
//import android.os.SystemProperties;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class SettingsTabActivity extends Fragment implements OnClickListener, OnTouchListener {
public static final String TAG="SettingsTabActivity";
	
	SharedPreferences mPreference=null;
	public final static String PREFERENCE_NAME="Settings";
/*	
	1.  Byte 2-3,  setting 00,  Reserved. 
	2.  Byte 4-5,  setting 01,  printing frequency.  Unit: Hz, (0-43K Hz)
	3.  Byte 6-7,  setting 02,  Delay.               Unit: 0.1mmm  
	4.  Byte 8-9,  setting 03,  Reserved. 
	5.  Byte 10-11,  setting 04, Photocell.           00 00 : ON.  00 01 OFF. 
	6.  Byte 12-13,  setting 05, Encoder.             00 00 : ON.  00 01 OFF.  
	7.  Byte 14-15,  setting 06,  Bold.  
	8.  Byte 16-17,  setting 07,  Fix length trigger.  Unit: 0.1mmm 
	9.  Byte 18-19,  setting 08,  Fix time  trigger.  Unit: ms
	10. Byte 20-21,  setting 09,  Temperature of print head.  Unit: C 00-130.
	11. Byte 20-21,   setting 09,  Temperature of reservoir.  Unit: C   00-130.
	12. Others ,     Setting 10-63,  Reserved. 
*/
	public static final String PREF_PARAM_0="param0";			//00
	public static final String PREF_PRINTSPEED="printspeed";	//01
	public static final String PREF_DELAY="delay";				//02
	public static final String PREF_TRIGER="triger";			//04
	public static final String PREF_ENCODER="encoder";			//05
	public static final String PREF_BOLD="bold";				//06
	public static final String PREF_FIX_LEN="fix_length_triger";	//07
	public static final String PREF_FIX_TIME="fix_time_triger";	//08
	public static final String PREF_HEAD_TEMP="head_temp";		//09
	public static final String PREF_RESV_TEMP="reservoir_temp";	//10
	public static final String PREF_FONT_WIDTH="fontwidth";		//11
	public static final String PREF_DOT_NUMBER="dot_number";	//12
	public static final String PREF_RESERVED_12="reserved12";	//13
	public static final String PREF_RESERVED_13="reserved13";	//14
	public static final String PREF_RESERVED_14="reserved14";	//15
	public static final String PREF_RESERVED_15="reserved15";	//16
	public static final String PREF_RESERVED_16="reserved16";	//17
	public static final String PREF_RESERVED_17="reserved17";	//18
	public static final String PREF_RESERVED_18="reserved18";	//19
	public static final String PREF_RESERVED_19="reserved19";	//20
	public static final String PREF_RESERVED_20="reserved20";	//21
	public static final String PREF_RESERVED_21="reserved21";	//22
	public static final String PREF_RESERVED_22="reserved22";	//23
	public static final String PREF_RESERVED_23="reserved23";	//24
		
	public static final String PREF_DIRECTION="direction";
	public static final String PREF_HORIRES="horires";
	public static final String PREF_VERTRES="vertres";
	
	
	public TextView mTime;
	public TextView mVersion;

	public RelativeLayout		mSave;

	// 2024-11-4 mUpgrade实际上没有被使用到
	public RelativeLayout		mUpgrade;
	public RelativeLayout		mSetDate;
	public RelativeLayout		mSettings;
	public RelativeLayout 		mPagePrev;
	public RelativeLayout 		mPageNext;
	public RelativeLayout		mTimeset;
	public RelativeLayout		mClean;

	public Context 			mContext;
	
	public ScrollView			mScrollView;
	public PHSettingFragment 	mPHSettings;
	public SettingsFragment		mSettingsFragment;
	public ListView 			mListView;
	public SettingsListAdapter 	mAdapter;
	
	public ExtendMessageTitleFragment mMsgTitle;
	
	public SystemConfigFile mSysconfig;
	
	public SettingsTabActivity() {
//		mMsgTitle = (ExtendMessageTitleFragment)fragment;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Debug.d(TAG, "-->onCreateView");
		return inflater.inflate(R.layout.setting_layout, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		
		super.onActivityCreated(savedInstanceState);
		//requestWindowFeature(Window.FEATURE_NO_TITLE);
		//this.addPreferencesFromResource(R.xml.settings_layout);
		mContext = getActivity();
		Debug.d(TAG, "--->onActivityCreated");
		
//		mTime = (TextView) findViewById(R.id.tv_systemTime);
//		mTimeRefreshHandler.sendEmptyMessageDelayed(0, 2000);
		
		mPagePrev = (RelativeLayout) getView().findViewById(R.id.btn_prev);
		mPagePrev.setOnClickListener(this);
		mPagePrev.setOnTouchListener(this);
		
		mPageNext = (RelativeLayout) getView().findViewById(R.id.btn_next);
		mPageNext.setOnClickListener(this);
		mPageNext.setOnTouchListener(this);
		
		mSave = (RelativeLayout) getView().findViewById(R.id.btn_setting_ok);
		mSave.setOnClickListener(this);
		mSave.setOnTouchListener(this);
		
		mUpgrade = (RelativeLayout) getView().findViewById(R.id.btn_setting_upgrade);
		mUpgrade.setOnClickListener(this);
		mUpgrade.setOnTouchListener(this);
		
		mTimeset = (RelativeLayout) getView().findViewById(R.id.btn_setting_timeset);
		mTimeset.setOnClickListener(this);
		mTimeset.setOnTouchListener(this);
		
		mSettings = (RelativeLayout) getView().findViewById(R.id.btn_system_setting);
		mSettings.setOnClickListener(this);
		mSettings.setOnTouchListener(this);
		
		mClean = (RelativeLayout) getView().findViewById(R.id.btn_setting_clean);
		mClean.setOnClickListener(this);
		
		//mScrollView = (ScrollView) getView().findViewById(R.id.setting_frame);
		
		FragmentTransaction transaction = getFragmentManager().beginTransaction();
		/*
		mPHSettings = new PHSettingFragment(mContext);
		transaction.replace(R.id.phsetting_fragment, mPHSettings);
		*/
		/*从U盘中读取系统设置，解析*/
		mSysconfig = SystemConfigFile.getInstance(mContext);
		
		//mSettingsFragment = new SettingsFragment(mContext);
		//transaction.replace(R.id.phsetting_fragment, mSettingsFragment);
		mListView = (ListView) getView().findViewById(R.id.settings_list_view);
		mAdapter = new SettingsListAdapter(mContext);
		mListView.setAdapter(mAdapter);
		//transaction.commit();
		// setupViews();

		PrinterApplication application = (PrinterApplication) mContext.getApplicationContext();
		application.registerCallback("system_config.xml", new KZFileObserver.KZFileObserverInterface() {
			@Override
			public void onChanged() {
// H.M.Wang 2019-12-27 更改为在UI线程当中调用
				SettingsTabActivity.this.getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						reloadSettings();
					}
				});
// End of H.M.Wang 2019-12-27 更改为在UI线程当中调用
			}
		});

	}

	// 2024-11-4 由于该函数没有被调用，因此mUpgrade始终是不可视
	private void setupViews() {
		if (PlatformInfo.PRODUCT_SMFY_SUPER3.equals( PlatformInfo.getProduct())) {
			mUpgrade.setVisibility(View.GONE);
			mPagePrev.setVisibility(View.VISIBLE);
			mPageNext.setVisibility(View.VISIBLE);
			mTimeset.setVisibility(View.VISIBLE);
		} else if (PlatformInfo.PRODUCT_FRIENDLY_4412.equals( PlatformInfo.getProduct())) {
			mUpgrade.setVisibility(View.VISIBLE);
			mPagePrev.setVisibility(View.GONE);
			mPageNext.setVisibility(View.GONE);
			mTimeset.setVisibility(View.GONE);
		}
		
	}
	
	@Override
	public void onStart() {
		super.onStart();
		Debug.d(TAG, "--->onstart");
	}
	

	@Override
	public void onResume() {
		super.onResume();
		Debug.d(TAG, "--->onResume");
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		Debug.d(TAG, "--->onDestroyView");
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if(mAdapter != null) {
			mAdapter.destroy();
		}
		Debug.d(TAG, "--->onDestroy");
	}

	@Override
	public void onHiddenChanged(boolean hidden) {
		super.onHiddenChanged(hidden);
		Debug.d(TAG, "--->onHiddenChanged: " + hidden);
		if (!hidden) {
			mAdapter.notifyDataSetChanged();
		}
	}

	private TextView tv_btnOk;
	private TextView tv_btnCancel;
	private TextView tv_btnTime;
	private TextView tv_btnSetting; 
	
	public void onConfigureChanged() {
//		mAdapter.loadSettings();
		loadSettings();
		mAdapter.notifyDataSetChanged();
		tv_btnOk = (TextView) getView().findViewById(R.id.btn_ok_tv);
		tv_btnOk.setText(R.string.str_btn_settings_sync);
		
		tv_btnTime = (TextView) getView().findViewById(R.id.btn_time_tv);
		tv_btnTime.setText(R.string.str_setting_time);
		
		tv_btnSetting = (TextView) getView().findViewById(R.id.btn_setting_tv);
		tv_btnSetting.setText(R.string.str_system_setting);
    }
//	@Override
//	public boolean  onKeyDown(int keyCode, KeyEvent event)  
//	{
//		Debug.d(TAG, "keycode="+keyCode);
//		
//		if(keyCode==KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME)
//		{
//			Debug.d(TAG, "back key pressed, ignore it");
//			return true;	
//		}
//		return false;
//	}
//	
//
//	@Override
//	public boolean onTouchEvent(MotionEvent event)
//	{
//		Debug.d(TAG, "event:"+event.toString());
//		InputMethodManager manager = (InputMethodManager)getSystemService(Service.INPUT_METHOD_SERVICE);
//		Debug.d(TAG, "ime is active? "+manager.isActive());
//		manager.hideSoftInputFromWindow(SettingsTabActivity.this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
////			manager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_NOT_ALWAYS);
//		return true;
//	}
	
	private void loadSettings() {
//		int[] counters = RTCDevice.getInstance(mContext).readAll();
//		for (int i = SystemConfigFile.INDEX_COUNT_1; i <= SystemConfigFile.INDEX_COUNT_10; i++) {
//			SystemConfigFile.getInstance(mContext).setParam(i, counters[i - SystemConfigFile.INDEX_COUNT_1]);
//		}
		mAdapter.loadSettings();
		mAdapter.notifyDataSetChanged();
	}
	
	public void setLocale()
	{
		Configuration config = getResources().getConfiguration(); 
		DisplayMetrics dm = getResources().getDisplayMetrics(); 
		config.locale = Locale.SIMPLIFIED_CHINESE; 
		getResources().updateConfiguration(config, dm); 
		
	}
	
	public void savePreference()
	{
		
	}
	
	public void reloadSettings() {
		Configs.initConfigs(mContext);
		loadSettings();
		// mPHSettings.reloadSettings();
		
	}
	
	public void setParam(int param, int value) {
		mAdapter.setParam(param, value);
	}

	public Handler mTimeRefreshHandler = new Handler(){
		public void handleMessage(Message msg) { 
			switch(msg.what)
			{
				case 0:		//
					 long sysTime = System.currentTimeMillis();
					 SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
//					 mTime.setText(dateFormat.format(new Date()));
					 
					break;
			}
			mTimeRefreshHandler.sendEmptyMessageDelayed(0, 500);
		}
	};

// 2020-6-29 处于打印状态时，如果用户确认设置，需要向FPGA下发设置内容，按一定原则延迟下发
	private static final int MSG_DELAYED_FPGA_SETTING = 100;

	public Handler mHandler = new Handler(){
		public void handleMessage(Message msg) {
			switch(msg.what)
			{
				case MSG_DELAYED_FPGA_SETTING:
					Debug.d(TAG, "Writing to FPGA");
					DataTransferThread dt = DataTransferThread.getInstance(mContext);
					DataTask task = dt.getCurData();
// 2020-5-8							FpgaGpioOperation.clean();
					FpgaGpioOperation.updateSettings(mContext, task, FpgaGpioOperation.SETTING_TYPE_NORMAL);
//							dt.mNeedUpdate = true;
//							while (dt.mNeedUpdate) {
//								try{Thread.sleep(10);}catch(Exception e){};
//							}
					FpgaGpioOperation.init();
// H.M.Wang 2020-7-9 取消下发参数设置后重新下发打印缓冲区操作
//					dt.resendBufferToFPGA();
// End of H.M.Wang 2020-7-9 取消下发参数设置后重新下发打印缓冲区操作
					break;
			}
		}
	};
// End of 2020-6-29 处于打印状态时，如果用户确认设置，需要向FPGA下发设置内容，按一定原则延迟下发

	@Override
	public void onClick(View arg0) {
		if (arg0 == null) {
			return;
		}
		ExtGpio.playClick();
		switch (arg0.getId()) {
			case R.id.btn_prev:
				// mScrollView.arrowScroll(View.FOCUS_UP);
				// mScrollView.scrollBy(0, -300);
				 mListView.smoothScrollBy(-300, 2);
				break;
			case R.id.btn_next:
				// mScrollView.arrowScroll(View.FOCUS_DOWN);
				// mScrollView.scrollBy(0, 300);
				mListView.smoothScrollBy(300, 2);
				break;
			case R.id.btn_setting_ok:
				PasswordDialog pdialog = new PasswordDialog(mContext);
				pdialog.setOnPositiveClickedListener(new OnPositiveListener() {

					@Override
					public void onClick() {
						saveParam();
// H.M.Wang 2024-7-27 追加蓝牙设备号和蓝牙开关功能
						new Thread(new Runnable() {
							@Override
							public void run() {
//								BLEDevice ble = BLEDevice.getInstance();
//								ble.paramsChanged();
								BluetoothServerManager bsm = BluetoothServerManager.getInstance();
								bsm.paramsChanged();
							}
						}).start();
// End of H.M.Wang 2024-7-27 追加蓝牙设备号和蓝牙开关功能
// H.M.Wang 2020-4-25 设置按确认， 所有参数都生效，  下发一次fpga
						DataTransferThread dt = DataTransferThread.getInstance(mContext);
						if(null != dt && dt.isRunning()) {
// H.M.Wang 2023-1-7 取消打印时下发参数
/*
// 2020-6-29 处于打印状态时，如果用户确认设置，需要向FPGA下发设置内容，按一定原则延迟下发
							Debug.d(TAG, "Time1 = " + dt.Time1 + "; Time2 = " + dt.Time2 + "; Now = " + System.currentTimeMillis() + "; DataRatio = " + dt.DataRatio);
							int a = (int)(dt.Time2 - dt.Time1);
							int b = Math.round(a * (1.0f * 3 / (2 * dt.DataRatio + 4)));
                            int c = (int)(System.currentTimeMillis() - dt.Time2);
							Debug.d(TAG, "a = " + a + "; b = " + b + "; c = " + c);
							long delay = Math.min(
											Math.max(
// 2020-7-21 为修改计算等待时间添加倍率变量（新公式为：N=(打印缓冲区字节数-1）/16K；时长=3/(2N+4)
//                                                    Math.round((dt.Time2 - dt.Time1) * (1.0f * 3 / (2 * dt.DataRatio + 4))) - (System.currentTimeMillis() - dt.Time2),
                                                    ((b - c) > 0 ? (b - c) : (a + b - c)),
// End of 2020-7-21 为修改计算等待时间添加倍率变量（新公式为：N=(打印缓冲区字节数-1）/16K；时长=3/(2N+4)
												0),
											40000);
							Debug.d(TAG, "Delay " + delay + "ms to write to FPGA");
							if(delay > 0) {
								mHandler.sendEmptyMessageDelayed(MSG_DELAYED_FPGA_SETTING, delay);
							} else {
								mHandler.sendEmptyMessage(MSG_DELAYED_FPGA_SETTING);
							}
// End of 2020-6-29 处于打印状态时，如果用户确认设置，需要向FPGA下发设置内容，按一定原则延迟下发
*/
// End of H.M.Wang 2023-1-7 取消打印时下发参数
						} else {
							FpgaGpioOperation.updateSettings(mContext, null, FpgaGpioOperation.SETTING_TYPE_NORMAL);
						}
// End of H.M.Wang 2020-4-25 设置按确认， 所有参数都生效，  下发一次fpga
						ToastUtil.show(mContext, R.string.toast_save_success);
// H.M.Wang 2023-5-15 增加旋转屏幕，在180度之间转换
						if(mSysconfig.getParam(SystemConfigFile.INDEX_LCD_INVERSE) == 0) {
							Settings.System.putInt(mContext.getContentResolver(), "rotate_screen", ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
						} else {
// H.M.Wang 2024-10-15 翻转屏幕仅限3.5寸屏，因为7寸屏反转好像是以1920*1080为展面操作的，但是7寸屏的大小是1024*600，导致反转后大部分内容跑到了屏幕外面
							String info = PlatformInfo.getImgUniqueCode();
							if(info.startsWith("NNG3") || info.startsWith("ONG3") || info.startsWith("GZJ") || info.startsWith("NSM2") || info.startsWith("FNG3") || info.startsWith("FGZJ") || info.startsWith("FNSM")) {
								Settings.System.putInt(mContext.getContentResolver(), "rotate_screen", ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
							}
// End of H.M.Wang 2024-10-15 翻转屏幕仅限3.5寸屏，因为7寸屏反转好像是以1920*1080为展面操作的，但是7寸屏的大小是1024*600，导致反转后大部分内容跑到了屏幕外面
						}
// End of H.M.Wang 2023-5-15 增加旋转屏幕，在180度之间转换
					}

					@Override
					public void onClick(String content) {
						saveParam();
					}
				});
				pdialog.setOnNagitiveClickedListener(new OnNagitiveListener() {
					
					@Override
					public void onClick() {
						reloadSettings();
					}
				});
				pdialog.show();
				break;
			case R.id.btn_setting_cancel:
				// mPHSettings.reloadSettings();
				break;
			case R.id.btn_system_setting:	//进入系统设置
				Intent intent = new Intent();
				intent.setClassName("com.android.settings","com.android.settings.Settings");
				startActivity(intent);
				break;
			case R.id.btn_setting_upgrade:
				// ArrayList<String> paths = ConfigPath.getMountedUsb();
// H.M.Wang 2024-11-5 增加A133平台的判断
				// 2024-11-4 由于R.id.btn_setting_upgrade对应的变量mUpgrade没有被设置为可视，因此这部分代码不会被执行到
//				if (PlatformInfo.PRODUCT_SMFY_SUPER3.equals(PlatformInfo.getProduct())) {
				if (PlatformInfo.isSmfyProduct() || PlatformInfo.isA133Product()) {
// End of H.M.Wang 2024-11-5 增加A133平台的判断
					PackageInstaller installer = PackageInstaller.getInstance(getActivity());
					if(WelcomeActivity.AVOID_CROSS_UPGRADE) {
						installer.silentUpgrade3();
					} else {
						installer.silentUpgrade();
					}
				} else {
					String str = ConfigPath.getUpgradePath() + Configs.UPGRADE_APK_FILE;
					File file = new File(str);
					Debug.d(TAG, "===>file:"+file.getPath());
					if (!file.exists()) {
						ToastUtil.show(mContext, R.string.strUpgradeNoApk);
						break;
					}
					Debug.d(TAG, "===>start upgrade service");
					// System.setProperty("ctl.start", "Upgrade");
					// SystemProperties.set("ctl.start","Upgrade");
					ToastUtil.show(mContext, R.string.str_upgrade_progress);
					ReflectCaller.SysPropUpgrade();
				}
				break;
			case R.id.btn_setting_timeset:
				CalendarDialog dialog = new CalendarDialog(this.getActivity(), R.layout.calendar_setting);
				dialog.show();
				break;

			case R.id.btn_setting_clean:
// H.M.Wang 2020-8-21 追加正在清洗标志，此标志为ON的时候不能对FPGA进行某些操作，如开始，停止等，否则死机
				DataTransferThread thread = DataTransferThread.getInstance(mContext);
				if(thread.isPurging) {
					ToastUtil.show(mContext, R.string.str_under_purging);
					break;
				}
// End of H.M.Wang 2020-8-21 追加正在清洗标志，此标志为ON的时候不能对FPGA进行某些操作，如开始，停止等，否则死机
// H.M.Wang 2022-1-4 追加是否正在打印的判断，打印中不支持长清洗
				if(thread.isRunning()) {
					ToastUtil.show(mContext, R.string.str_under_printing);
					break;
				}
// End of H.M.Wang 2022-1-4 追加是否正在打印的判断，打印中不支持长清洗

// H.M.Wang 2020-8-21 追加点按清洗按键以后提供确认对话窗
				AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
				View alertView = (View)LayoutInflater.from(mContext).inflate(R.layout.clean_alert, null);
				builder.setTitle(R.string.str_setting_clean)
//						.setMessage(R.string.str_clean_confirm)
						.setView(alertView)
						.setPositiveButton(R.string.str_ok, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								DataTransferThread dThread = DataTransferThread.getInstance(mContext);
								if (dThread.isCleaning) {
									dThread.interruptClean();
								} else {
									dThread.clean(mContext);
								}
								dialog.dismiss();
							}
						})
						.setNegativeButton(R.string.str_btn_cancel, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}
						});
// H.M.Wang 2025-3-6 追加Hp22mm类型的打印头询问清洗哪个头的功能
				if(PlatformInfo.getImgUniqueCode().startsWith("22MM")) {
					LinearLayout penSelectV = (LinearLayout) alertView.findViewById(R.id.pen_select);
					penSelectV.setVisibility(View.VISIBLE);
					CheckBox pen1 = (CheckBox) penSelectV.findViewById(R.id.clean_pen1);
					pen1.setChecked(Hp22mm.CleanHeads[0]);
					pen1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
						@Override
						public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
							Hp22mm.CleanHeads[0] = b;
						}
					});
					CheckBox pen2 = (CheckBox) penSelectV.findViewById(R.id.clean_pen2);
					pen2.setChecked(Hp22mm.CleanHeads[1]);
					pen2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
						@Override
						public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
							Hp22mm.CleanHeads[1] = b;
						}
					});
				}
// End of H.M.Wang 2025-3-6 追加Hp22mm类型的打印头询问清洗哪个头的功能
				builder.create().show();
// End of H.M.Wang 2020-8-21 追加点按清洗按键以后提供确认对话窗
				break;
			default :
				Debug.d(TAG, "===>unknown view clicked");
				break;
		}
	}

	@Override
	public boolean onTouch(View view, MotionEvent event) {
		
		switch(view.getId()) {
			case R.id.btn_prev:
			case R.id.btn_next:
			case R.id.btn_setting_ok:
			case R.id.btn_setting_upgrade:
			case R.id.btn_setting_timeset:
			case R.id.btn_system_setting:
//			case R.id.btn_setting_clean:
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
//					PWMAudio.Play();
				}
			default:
				break;
		}
		return false;
	}
	
	private void saveParam() {
		Debug.d(TAG, "===>onclick");
		mAdapter.checkParams();
//		mAdapter.notifyDataSetChanged();

		PrinterApplication application = (PrinterApplication) mContext.getApplicationContext();
		application.pauseFileListener();
		mSysconfig.saveConfig();
		((MainActivity) getActivity()).onConfigChange();
		application.resumeFileListener();
		// FpgaGpioOperation.updateSettings(mContext);
		//FpgaGpioOperation device = FpgaGpioOperation.getInstance();
		// device.read();
		if(mSysconfig.getParam(30)==7)
		{				Debug.i(TAG, "111===>onclick");
		   PlatformInfo.SetDotMatrixType(1);
		   
		}else if(mSysconfig.getPNozzle().mHeads==8)
		{				Debug.i(TAG, "222===>onclick");
		   PlatformInfo.SetDotMatrixType(2);
		} 
		else
		{				Debug.i(TAG, "=333==>onclick");
			  PlatformInfo.SetDotMatrixType(0);			
		}
		Debug.i(TAG, "===>onclick: " + mSysconfig.getParam(30));
	}
}