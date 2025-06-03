package com.industry.printer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.industry.printer.Utils.ConfigPath;
import com.industry.printer.Utils.Configs;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.FileUtil;
import com.industry.printer.Utils.LibUpgrade;
import com.industry.printer.Utils.PackageInstaller;
import com.industry.printer.Utils.PlatformInfo;
import com.industry.printer.Utils.ToastUtil;
import com.industry.printer.ui.CustomerDialog.CalendarDialog;
import com.industry.printer.ui.CustomerDialog.LoadingDialog;
import com.industry.printer.ui.CustomerDialog.RelightableDialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class WelcomeActivity extends Activity {
	
	private static final String TAG = WelcomeActivity.class.getSimpleName(); 
	private Context mContext;

// H.M.Wang 2023-8-18 将启动页面的两个图片从MainActivity移到WelcomeActivity
	private ImageView mLoading1s;
	private View mClickView;
	private StartupDialog mStartupDialog = null;

	private static final int LAUNCH_MAINACTIVITY = 7;

	public static final boolean AVOID_CROSS_UPGRADE = true;			// 禁止交叉升级
//	public static final boolean AVOID_CROSS_UPGRADE = false;		// 自由升级

	public Handler mHander = new Handler() {

		public void handleMessage(Message msg) {
			switch (msg.what) {
				case LAUNCH_MAINACTIVITY:
					Debug.d(TAG, "-------- LAUNCH_MAINACTIVITY --------");
					if(AVOID_CROSS_UPGRADE) {
						try {
							int curVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
							if (curVersion <= 100000000 || curVersion >= 1000000000) {                // 非9位数
								Debug.d(TAG, "版本号：" + curVersion + "\n" + "旧版apk，不检查启动合法性，允许启动");
							} else {
								File f1 = new File(Configs.FILE_1);
								File f2 = new File(Configs.FILE_2);
								if (!f1.exists() && !f2.exists()) {
									Debug.d(TAG, "版本号：" + curVersion + "\n" + "F1和F2均不存在，疑似从旧版升级，允许启动");
									f1.createNewFile();
									BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f1)));
									bw.write(curVersion + "\n");
									bw.flush();
									bw.close();
									f2.createNewFile();
									BufferedWriter bw1 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f2)));
									bw1.write(curVersion + "\n");
									bw1.flush();
									bw1.close();
								} else if (f2.exists()) {
									BufferedReader br = new BufferedReader(new FileReader(f2));
									if (null != br) {
										String tmp = br.readLine();
										int tmpInt = Integer.parseInt(tmp);
										br.close();
										if (curVersion == tmpInt) {
											Debug.d(TAG, "版本号：" + curVersion + "\n" + "F2存在，记录版本号与apk版本号一致，判断为正常升级，允许启动");
											if (!f1.exists()) f1.createNewFile();
											BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f1)));
											bw.write(curVersion + "\n");
											bw.flush();
											bw.close();
										} else {
											Debug.d(TAG, "版本号：" + curVersion + "\n" + "F2版本号：" + tmpInt + "\n" + "F2存在，记录版本号与apk版本号不一致，疑似push升级，不允许启动");
											return;
										}
									}
								} else if (f1.exists()) {
									BufferedReader br = new BufferedReader(new FileReader(f1));
									if (null != br) {
										String tmp = br.readLine();
										int tmpInt = Integer.parseInt(tmp);
										br.close();
										if (curVersion == tmpInt) {
											Debug.d(TAG, "版本号：" + curVersion + "\n" + "F1存在，记录版本号与apk版本号一致，判断为正常升级，允许启动");
											if (!f2.exists()) f2.createNewFile();
											BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f2)));
											bw.write(curVersion + "\n");
											bw.flush();
											bw.close();
										} else {
											Debug.d(TAG, "版本号：" + curVersion + "\n" + "F1版本号：" + tmpInt + "\n" + "F1存在，记录版本号与apk版本号不一致，疑似push升级，不允许启动");
											return;
										}
									}
								}
							}
						} catch (PackageManager.NameNotFoundException e) {
							Debug.e(TAG, e.getMessage());
						} catch (FileNotFoundException e) {
							Debug.e(TAG, e.getMessage());
						} catch (IOException e) {
							Debug.e(TAG, e.getMessage());
						} catch (NumberFormatException e) {
							Debug.e(TAG, e.getMessage());
						} catch (Exception e) {
							Debug.e(TAG, e.getMessage());
						}
					}
					mLoading1s.setVisibility(View.GONE);
					if(null != mStartupDialog) mStartupDialog.dismiss();
					Intent intent = new Intent();
					intent.setClass(mContext, MainActivity.class);
					startActivity(intent);
					finish();

					break;
			}
		}
	};
// End of H.M.Wang 2023-8-18 将启动页面的两个图片从MainActivity移到WelcomeActivity

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.welcome_layout);
		mContext = getApplicationContext();
		/*初始化系统配置*/
		Configs.initConfigs(mContext);

		Debug.d(TAG, "-------- onCreate --------");
// H.M.Wang 2023-8-18 将启动页面的两个图片从MainActivity移到WelcomeActivity
		if (!upgrade()) {
			mLoading1s = (ImageView) findViewById(R.id.image1s);
			mClickView = (View) findViewById(R.id.clickView);
			mClickView.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View view) {
					mHander.removeMessages(LAUNCH_MAINACTIVITY);
					mStartupDialog = new StartupDialog(WelcomeActivity.this);
					mStartupDialog.show();
					mHander.sendEmptyMessageDelayed(LAUNCH_MAINACTIVITY, 15*1000);
					return false;
				}
			});
			mHander.sendEmptyMessageDelayed(LAUNCH_MAINACTIVITY, 5*1000);
		} else {
			try {Thread.sleep(3000);} catch(Exception e) {}
			new AlertDialog.Builder(this).setMessage(R.string.str_urge2restart).create().show();
//			ToastUtil.show(mContext, R.string.str_urge2restart);
		}
// End of H.M.Wang 2023-8-18 将启动页面的两个图片从MainActivity移到WelcomeActivity
// H.M.Wang 2023-8-18 将启动页面的两个图片从MainActivity移到WelcomeActivity
/*
// H.M.Wang 2022-5-12 修改升级的方法。取消启动后自动升级，改为在设置页面按钮启动升级
		if (!upgrade()) {
// End of H.M.Wang 2022-5-12 修改升级的方法。取消启动后自动升级，改为在设置页面按钮启动升级
//			asyncInit();
			Intent intent = new Intent();
			intent.setClass(this, MainActivity.class);
			startActivity(intent);
			finish();
// H.M.Wang 2022-5-12 修改升级的方法。取消启动后自动升级，改为在设置页面按钮启动升级
		}
// End of H.M.Wang 2022-5-12 修改升级的方法。取消启动后自动升级，改为在设置页面按钮启动升级
*/
// End of H.M.Wang 2023-8-18 将启动页面的两个图片从MainActivity移到WelcomeActivity
	}

// H.M.Wang 2023-8-18 将启动页面的两个图片从MainActivity移到WelcomeActivity
	@Override
	public void onDestroy() {
		super.onDestroy();
		mHander.removeMessages(LAUNCH_MAINACTIVITY);
		Debug.d(TAG, "-------- onDestroy --------");
	}
// End of H.M.Wang 2023-8-18 将启动页面的两个图片从MainActivity移到WelcomeActivity

	@Override
	public void onBackPressed() {
		return ;
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		Debug.d("", "--->onConfigurationChanged");
	}
	
	private boolean upgrade() {
		boolean ret = false;
// H.M.Wang 2024-11-5 增加A133平台的判断
//		if (PlatformInfo.PRODUCT_SMFY_SUPER3.equals(PlatformInfo.getProduct())) {
		if (PlatformInfo.isSmfyProduct() || PlatformInfo.isA133Product()) {
// End of H.M.Wang 2024-11-5 增加A133平台的判断
			//FileUtil.deleteFolder(Configs.FONT_DIR);
			LibUpgrade libUp = new LibUpgrade();
			ret = libUp.upgradeLibs();

			PackageInstaller installer = PackageInstaller.getInstance(this);
			if(AVOID_CROSS_UPGRADE) {
				ret |= installer.silentUpgrade3();
			} else {
				ret |= installer.silentUpgrade();
			}
		}
		return ret;
	}

// H.M.Wang 2023-8-18 将启动页面的两个图片从MainActivity移到WelcomeActivity
	private class StartupDialog extends Dialog implements android.view.View.OnClickListener {
		private final String TAG = StartupDialog.class.getSimpleName();

		private LoadingDialog mProgressDialog;

		public StartupDialog(Context context) {
			super(context);
		}

		@Override
		protected void onCreate(Bundle savedInstanceState) {
			// TODO Auto-generated method stub
			super.onCreate(savedInstanceState);
			this.requestWindowFeature(Window.FEATURE_NO_TITLE);
			this.setContentView(R.layout.startup_dialog);

// H.M.Wang 2023-7-20 取消Theme，因为这样生成的对话窗会在显示的时候，屏幕亮度随系统的亮度立即调整，如系统的亮度设的偏暗，则屏幕会立即变暗，看起来很费劲
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
			WindowManager.LayoutParams lp = getWindow().getAttributes();
			lp.width = WindowManager.LayoutParams.MATCH_PARENT;
			lp.height = WindowManager.LayoutParams.MATCH_PARENT;
			getWindow().setAttributes(lp);
// End of H.M.Wang 2023-7-20 取消Theme，因为这样生成的对话窗会在显示的时候，屏幕亮度随系统的亮度立即调整，如系统的亮度设的偏暗，则屏幕会立即变暗，看起来很费劲

			TextView backupAndClear = (TextView) findViewById(R.id.backup_then_refresh);
			backupAndClear.setOnClickListener(this);
			TextView directClear = (TextView) findViewById(R.id.direct_refresh);
			directClear.setOnClickListener(this);
			TextView continueStartup = (TextView) findViewById(R.id.continue_startup);
			continueStartup.setOnClickListener(this);
		}

		@Override
		public void onClick(View view) {
			switch (view.getId()) {
				case R.id.backup_then_refresh:
					Debug.d(TAG, "-------- backup_then_refresh --------");
					mHander.removeMessages(LAUNCH_MAINACTIVITY);
					if(!msgExport()) break;
				case R.id.direct_refresh:
					Debug.d(TAG, "-------- direct_refresh --------");
					mHander.removeMessages(LAUNCH_MAINACTIVITY);
					clearData();
				case R.id.continue_startup:
					Debug.d(TAG, "-------- continue_startup --------");
					mHander.removeMessages(LAUNCH_MAINACTIVITY);
					mHander.sendEmptyMessage(LAUNCH_MAINACTIVITY);
					break;
			}
		}

		private boolean msgExport() {
			final ArrayList<String> usbs = ConfigPath.getMountedUsb();
			if (usbs.size() <= 0) {
				ToastUtil.show(mContext, R.string.toast_plug_usb);
				return false;
			}

			mProgressDialog = LoadingDialog.show(WelcomeActivity.this, R.string.strCopying);
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
								src.put("tips", WelcomeActivity.this.getString(R.string.tips_export_message));
							} else if (Configs.PICTURE_SUB_PATH.equals(arg0)) {
								Debug.d(TAG, "--->copy pictures");
								src.put("source", Configs.CONFIG_PATH_FLASH + Configs.PICTURE_SUB_PATH);
								src.put("dest", usbs.get(0) + arg0);
								src.put("tips", WelcomeActivity.this.getString(R.string.tips_export_resource));
							} else if ( Configs.SYSTEM_CONFIG_DIR.equals(arg0)) {
								src.put("source", Configs.CONFIG_PATH_FLASH + Configs.SYSTEM_CONFIG_DIR);
								src.put("dest", usbs.get(0) + arg0);
								src.put("tips", WelcomeActivity.this.getString(R.string.tips_export_sysconf));
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
								Debug.d(TAG, "--->start copy from " + arg0.get("source") + " to " + arg0.get("dest"));
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
								}
							});
			Debug.d(TAG, "--->return true");
			return true;
		}

		private void clearData() {
			FileUtil.deleteFolder(Configs.TLK_PATH_FLASH);
			FileUtil.deleteFolder(Configs.CONFIG_PATH_FLASH + Configs.PICTURE_SUB_PATH);
			FileUtil.deleteFolder(Configs.CONFIG_PATH_FLASH + Configs.SYSTEM_CONFIG_DIR);
		}
	}
// End of H.M.Wang 2023-8-18 将启动页面的两个图片从MainActivity移到WelcomeActivity
}
