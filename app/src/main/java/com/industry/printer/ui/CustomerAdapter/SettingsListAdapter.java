package com.industry.printer.ui.CustomerAdapter;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.industry.printer.DataTransferThread;
import com.industry.printer.FileFormat.QRReader;
import com.industry.printer.R;
import com.industry.printer.FileFormat.SystemConfigFile;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.PlatformInfo;
import com.industry.printer.hardware.RTCDevice;
import com.industry.printer.ui.CustomerAdapter.PopWindowAdapter.IOnItemClickListener;
import com.industry.printer.ui.CustomerDialog.CounterEditDialog;
import com.industry.printer.ui.CustomerDialog.DataSourceSelectDialog;
import com.industry.printer.ui.CustomerDialog.EncoderPPREditDialog;
import com.industry.printer.ui.CustomerDialog.HeaderSelectDialog;
import com.industry.printer.ui.CustomerDialog.Hp22mmNozzleSelectDialog;
import com.industry.printer.ui.CustomerDialog.PrintRepeatEditDialog;
import com.industry.printer.ui.CustomerDialog.QRLastEditDialog;
import com.industry.printer.widget.PopWindowSpiner;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsListAdapter extends BaseAdapter implements OnClickListener, IOnItemClickListener {

	private final static String TAG = SettingsListAdapter.class.getSimpleName();
	
	private Context mContext;
	public SystemConfigFile mSysconfig;
	private ItemViewHolder mHolder;
	
	/**
	 * An inflater for inflate the view
	 */
	private LayoutInflater mInflater;
	
	private String[] mTitles;
	
	public PopWindowSpiner mSpiner;
	public PopWindowAdapter mEncoderAdapter;
	public PopWindowAdapter mTrigerMode;
	private PopWindowAdapter mDirection;
	private PopWindowAdapter mResolution;
	private PopWindowAdapter mPhotocell;
// H.M.Wang 2020-4-27 重复打印设置改为对话窗
//	private PopWindowAdapter mRepeat;
// End of H.M.Wang 2020-4-27 重复打印设置改为对话窗
	private PopWindowAdapter mNozzle;
	private PopWindowAdapter mPen1Mirror;
	private PopWindowAdapter mPen2Mirror;
	private PopWindowAdapter mPen3Mirror;
	private PopWindowAdapter mPen4Mirror;

	private PopWindowAdapter mPen1Invert;
	private PopWindowAdapter mPen2Invert;
	private PopWindowAdapter mPen3Invert;
	private PopWindowAdapter mPen4Invert;
//	private PopWindowAdapter mLogSwitch;

	// H.M.Wang 增加1行。为计数器清楚前置0
	private PopWindowAdapter mClearZero;

	private PopWindowAdapter mPens;
	private PopWindowAdapter mAutoVol;
	private PopWindowAdapter mAutoPulse;
	private PopWindowAdapter mDots;
	private PopWindowAdapter mHandle;
	private PopWindowAdapter mCntReset;
//	private PopWindowAdapter mQRsource;
	private PopWindowAdapter mBeep;
	// H.M.Wang 2019-12-19 追加对参数39的修改为数据源选择的参数，该设置适配器停用
//	private PopWindowAdapter mLan;
	// End of H.M.Wang 2019-12-19 追加对参数39的修改为数据源选择的参数，该设置适配器停用

// H.M.Wang 2021-9-24 追加输入设置参数
	private PopWindowAdapter mInputProc;
// End of H.M.Wang 2021-9-24 追加输入设置参数

// H.M.Wang 2022-11-30 追加ENCDir方向选项
	private PopWindowAdapter mEncDir;
// End of H.M.Wang 2022-11-30 追加ENCDir方向选项

// H.M.Wang 2022-8-25 追加喷嘴加热参数项
	private PopWindowAdapter mNozzleWarm;
// End of H.M.Wang 2022-8-25 追加喷嘴加热参数项

// H.M.Wang 2023-1-5 增加一个快速打印(Fast Print)的参数。通过S24[4]下发给FPGA
	private PopWindowAdapter mFastPrint;
// End of H.M.Wang 2023-1-5 增加一个快速打印(Fast Print)的参数。通过S24[4]下发给FPGA
// H.M.Wang 2023-2-15 增加一个快捷模式/Easy mode的参数。用来区分启动哪个用户界面
	private PopWindowAdapter mUserMode;
// End of H.M.Wang 2023-2-15 增加一个快捷模式/Easy mode的参数。用来区分启动哪个用户界面

// H.M.Wang 2023-5-15 增加旋转屏幕，在180度之间转换
	private PopWindowAdapter mLCDInverse;
// End of H.M.Wang 2023-5-15 增加旋转屏幕，在180度之间转换

// H.M.Wang 2023-10-13 增加一个AD参数，当AD=0时，按原有策略(根据img的类型显示电池图标）；当AD=1时，无条件显示电池图标；当AD=2时，显示气压参数，具体方法待定
	private PopWindowAdapter mParamAD;
// End of H.M.Wang 2023-10-13 增加一个AD参数，当AD=0时，按原有策略(根据img的类型显示电池图标）；当AD=1时，无条件显示电池图标；当AD=2时，显示气压参数，具体方法待定

// H.M.Wang 2024-7-27 追加蓝牙设备号和蓝牙开关功能
	private PopWindowAdapter mBLEEnableAdapter;
// End of H.M.Wang 2024-7-27 追加蓝牙设备号和蓝牙开关功能

	private ItemViewHolder mEncoderHolder;
//	private HashMap<Integer, ItemViewHolder> mHoldMap;

// H.M.Wang 2022-10-18 参数扩容32项目
//	private ItemOneLine[] mSettingItems = new ItemOneLine[64];
	private ItemOneLine[] mSettingItems = new ItemOneLine[96];
// End of H.M.Wang 2022-10-18 参数扩容32项目

	// H.M.Wang 增加16行。接收计数器更新值，设置到编辑区内
	public static final String ACTION_PARAM_CHANGED = "com.industry.printer.PARAM_CHANGED";
	public static final String TAG_INDEX = "TagIndex";
	public static final String TAG_VALUE = "TagCount";

	BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(ACTION_PARAM_CHANGED.equals(intent.getAction())) {
				int index = intent.getIntExtra(TAG_INDEX, -1);
				String count = intent.getStringExtra(SettingsListAdapter.TAG_VALUE);

				// H.M.Wang 2019-12-9 将广播范围扩大到所有参数
				// (2020-7-29 发现为了将广播范围扩大到所有参数而注释掉的if语句于2020-5-16被恢复了，这是不对的。
				// 可能是追加QRLast时的误操作，恢复原样。同时QRLast的添加部分无用，注释掉)
//				if( index >= SystemConfigFile.INDEX_COUNT_1 && index <= SystemConfigFile.INDEX_COUNT_10) {
					if(null != mSettingItems[index]) {
						mSettingItems[index].setValue(count);
						notifyDataSetChanged();
					}
//				}
				// End. -----------
// H.M.Wang 2020-5-16 QRLast移植RTC的0x38地址保存，可以通过参数设置管理
//				if( index == SystemConfigFile.INDEX_QRCODE_LAST) {
//					if(null != mSettingItems[index]) {
//						mSettingItems[index].setValue(count);
//						notifyDataSetChanged();
//					}
//				}
// End of H.M.Wang 2020-5-16 QRLast移植RTC的0x38地址保存，可以通过参数设置管理
			}
		}
	};
	
	/**
	 * A customerized view holder for widgets 
	 * @author zhaotongkai
	 */
	private class ItemViewHolder{
		public TextView	mTitleL;		//message title
		public TextView	mValueLTv;
		public EditText	mValueLEt;
		public TextView	mUnitL;
		
		public TextView	mTitleR;		//message title
		public TextView	mValueRTv;
		public EditText	mValueREt;
		public TextView	mUnitR;
	}
	
	private class ItemType {
		public static final int TYPE_NONE = 0;
		public static final int TYPE_SWITCH = 1;
		public static final int TYPE_DIRECTION = 2;
		public static final int TYPE_VALUE = 3;
		public static final int TYPE_ARRAY = 4;
// H.M.Wang 2020-4-24 追加计数器编辑类型
		public static final int TYPE_DIALOG = 5;
// End of H.M.Wang 2020-4-24 追加计数器编辑类型
	}

	public final static int MSG_SELECTED_HEADER = 3;
	// H.M.Wang 2019-12-19 追加对参数39的修改，使得其成为数据源选择的参数
	public static final int MSG_DATA_SOURCE_SELECTED = 38;		// 数据源选择确定事件定义
	// End of H.M.Wang 2019-12-19 追加对参数39的修改，使得其成为数据源选择的参数
// H.M.Wang 2020-4-27 重复打印设置改为对话窗
	public static final int MSG_PRINT_REPEAT_SET = 39;		// 重复打印设置
// ENd of H.M.Wang 2020-4-27 重复打印设置改为对话窗
// H.M.Wang 2020-4-27 编码器脉冲设置改为对话窗
	public static final int MSG_ENCODER_PPR_SET = 40;		// 编码器脉冲设置
// End of H.M.Wang 2020-4-27 编码器脉冲设置改为对话窗
// H.M.Wang 2020-5-16 QRLast移植RTC的0x38地址保存，可以通过参数设置管理
	public static final int MSG_QRLAST_SET = 41;		// QRLast设置
// End of H.M.Wang 2020-5-16 QRLast移植RTC的0x38地址保存，可以通过参数设置管理
// H.M.Wang 2024-4-3 追加一个22mm的喷头选择参数
	public static final int MSG_HP22MM_NOZZLE_SEL = 42;		// 22MM选择喷嘴
// End of H.M.Wang 2024-4-3 追加一个22mm的喷头选择参数

	public Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what) {
			case MSG_SELECTED_HEADER:
				int index = msg.arg1;
				Debug.d(TAG, "--->index: " + index);
				mSettingItems[SystemConfigFile.INDEX_HEAD_TYPE].setValue(index);
				mSysconfig.setParam(SystemConfigFile.INDEX_HEAD_TYPE, mSettingItems[SystemConfigFile.INDEX_HEAD_TYPE].getValue());
				String value = mSettingItems[SystemConfigFile.INDEX_HEAD_TYPE].getDisplayValue();
				
				notifyDataSetChanged();
				break;
			// H.M.Wang 2019-12-19 追加数据源选择对话窗响应
			case MSG_DATA_SOURCE_SELECTED:
				mSettingItems[SystemConfigFile.INDEX_DATA_SOURCE].setValue(msg.arg1);
				mSysconfig.setParam(SystemConfigFile.INDEX_DATA_SOURCE, mSettingItems[SystemConfigFile.INDEX_DATA_SOURCE].getValue());

				notifyDataSetChanged();
				break;
			// End of H.M.Wang 2019-12-19 追加数据源选择对话窗响应
// H.M.Wang 2020-4-27 重复打印设置改为对话窗
			case MSG_PRINT_REPEAT_SET:
				mSettingItems[SystemConfigFile.INDEX_REPEAT_PRINT].setValue(msg.arg1);
				mSysconfig.setParam(SystemConfigFile.INDEX_REPEAT_PRINT, msg.arg1);
				notifyDataSetChanged();
				break;
// ENd of H.M.Wang 2020-4-27 重复打印设置改为对话窗
// H.M.Wang 2020-4-27 编码器脉冲设置改为对话窗
			case MSG_ENCODER_PPR_SET:
				mSettingItems[SystemConfigFile.INDEX_ENCODER_PPR].setValue(msg.arg1);
				mSysconfig.setParam(SystemConfigFile.INDEX_ENCODER_PPR, msg.arg1);
				notifyDataSetChanged();
				break;
// End of H.M.Wang 2020-4-27 编码器脉冲设置改为对话窗
// H.M.Wang 2020-5-16 QRLast移植RTC的0x38地址保存，可以通过参数设置管理
			case MSG_QRLAST_SET:
				mSettingItems[SystemConfigFile.INDEX_QRCODE_LAST].setValue(msg.arg1);		// 更新编辑页面数据
				mSysconfig.setParam(SystemConfigFile.INDEX_QRCODE_LAST, msg.arg1);			// 更新参数设置数据
				RTCDevice.getInstance(mContext).writeQRLast(msg.arg1);						// 更新不挥发保存区数据
// H.M.Wang 2020-5-19 写入不挥发存储区后，重新初始化QRReader类，否则，上次执行结果留存，新设置内容不起作用
				QRReader.reInstance(mContext);												// 重新初始化QRReader类，必须在更新了不挥发存储区后执行
// End of H.M.Wang 2020-5-19 写入不挥发存储区后，重新初始化QRReader类，否则，上次执行结果留存，新设置内容不起作用
				notifyDataSetChanged();
// H.M.Wang 2022-4-24 如果修改了QRLast值，则重新生成一次打印缓冲区
				DataTransferThread dt = DataTransferThread.getInstance(mContext);
				if(dt.isRunning()) {
					dt.setContentsFromQRFile();
					dt.mNeedUpdate = true;
				}
// End of H.M.Wang 2022-4-24 如果修改了QRLast值，则重新生成一次打印缓冲区
				break;
// End of H.M.Wang 2020-5-16 QRLast移植RTC的0x38地址保存，可以通过参数设置管理
			case MSG_HP22MM_NOZZLE_SEL:
				mSettingItems[SystemConfigFile.INDEX_22MM_NOZZLE_SEL].setValue(msg.arg1);
				mSysconfig.setParam(SystemConfigFile.INDEX_22MM_NOZZLE_SEL, msg.arg1);
				notifyDataSetChanged();
				break;
			}
		}
	};
	
	private class ItemOneLine {
		public int mParamId;
		public int mTitle;
		public String mValue;
		public int mEntry;
		public int mUnit;
		private int mType;
		
		public ItemOneLine(int param, int title, int unit) {
			mParamId = param;
			mTitle = title;
			mValue = String.valueOf(mSysconfig.getParam(param - 1));
			mUnit = unit;
			mType = ItemType.TYPE_NONE;
		}
		
		public ItemOneLine(int param, int title, int unit, int type) {
			this(param, title, unit);
			mType = type;
		}
		
		public ItemOneLine(int param, int title, int entry, int unit, int type) {
			mParamId = param;
			mTitle = title;
			mUnit = unit;
			mType = type;
			mEntry = entry;
			if (mType == ItemType.TYPE_SWITCH || mType == ItemType.TYPE_DIRECTION) {
				mValue = getEntry(entry, mSysconfig.getParam(param -1));
			} else if (mType == ItemType.TYPE_VALUE) {
				mValue = String.valueOf(mSysconfig.getParam(param-1));
			} else if (mType == ItemType.TYPE_ARRAY) {
				mValue = getEntry(entry, mSysconfig.getParam(param -1));
			}
			
		}
		
		public void setValue(int value) {
			switch (mType) {
			case ItemType.TYPE_NONE:
// H.M.Wang 2020-4-24 追加计数器编辑类型，值按着普通取值方法获取
			case ItemType.TYPE_DIALOG:
// H.M.Wang 2020-4-24 追加计数器编辑类型，值按着普通取值方法获取
				mValue = String.valueOf(value);
				break;
			case ItemType.TYPE_SWITCH:
			case ItemType.TYPE_DIRECTION:
				mSysconfig.setParam(mParamId-1, value);
				mValue = getEntry(mEntry, value);
				break;
			case ItemType.TYPE_VALUE:
				mValue = getEntry(mEntry, value);
				try {
					mSysconfig.setParam(mParamId-1, Integer.parseInt(mValue));
				} catch (Exception e) {
					mSysconfig.setParam(mParamId-1, value);
				}
				
				break;
			case ItemType.TYPE_ARRAY:
				mValue = getEntry(mEntry, value);
				mSysconfig.setParam(mParamId-1, value);
				break;
			default:
				break;
			}
		}
		
		public void setValue(String value) {
			mValue = value;
		}
		
		public String getDisplayValue() {
			return mValue;
		}
		
		public int getValue() {
			return mSysconfig.getParam(mParamId - 1);
		}
	}

	public void setParam(int param, int value) {
		if (mSettingItems == null ||param >= mSettingItems.length) {
			return;
		}
		mSettingItems[param].setValue(value);
		mSysconfig.setParam(param, value);
		notifyDataSetChanged();
	}

	public SettingsListAdapter(Context context) {
		mContext = context;
		mSysconfig = SystemConfigFile.getInstance(mContext);
		mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		// mTitles = mContext.getResources().getStringArray(R.array.str_settings_params);

		loadSettings();
//		mHoldMap = new HashMap<Integer, ItemViewHolder>();
		
		mEncoderAdapter = new PopWindowAdapter(mContext, null);
		mTrigerMode = new PopWindowAdapter(mContext, null);
		mDirection = new PopWindowAdapter(mContext, null);
		mResolution = new PopWindowAdapter(mContext, null);
		mPhotocell = new PopWindowAdapter(mContext, null);
// H.M.Wang 2020-4-27 重复打印设置改为对话窗
//		mRepeat = new PopWindowAdapter(mContext, null);
// End of H.M.Wang 2020-4-27 重复打印设置改为对话窗
		mNozzle = new PopWindowAdapter(mContext, null);
		mPen1Mirror = new PopWindowAdapter(mContext, null);
		mPen2Mirror = new PopWindowAdapter(mContext, null);
		mPen3Mirror = new PopWindowAdapter(mContext, null);
		mPen4Mirror = new PopWindowAdapter(mContext, null);
		
		mPen1Invert = new PopWindowAdapter(mContext, null);
		mPen2Invert = new PopWindowAdapter(mContext, null);
		mPen3Invert = new PopWindowAdapter(mContext, null);
		mPen4Invert = new PopWindowAdapter(mContext, null);
//		mLogSwitch = new PopWindowAdapter(mContext, null);

		// H.M.Wang 增加1行。为计数器清楚前置0
		mClearZero = new PopWindowAdapter(mContext, null);

		mPens = new PopWindowAdapter(mContext, null);
		mHandle = new PopWindowAdapter(mContext, null);
		
		mAutoVol = new PopWindowAdapter(mContext, null);
		mAutoPulse = new PopWindowAdapter(mContext, null);
		mDots = new PopWindowAdapter(mContext, null);
		mCntReset = new PopWindowAdapter(mContext, null);
		// mQRsource = new PopWindowAdapter(mContext, null);
		mBeep = new PopWindowAdapter(mContext, null);
		// H.M.Wang 2019-12-19 追加对参数39的修改为数据源选择的参数，该设置适配器停用
		// mLan = new PopWindowAdapter(mContext, null);
		// End of H.M.Wang 2019-12-19 追加对参数39的修改为数据源选择的参数，该设置适配器停用

// H.M.Wang 2021-9-24 追加输入设置参数
		mInputProc = new PopWindowAdapter(mContext, null);
// End of H.M.Wang 2021-9-24 追加输入设置参数

// H.M.Wang 2022-11-30 追加ENCDir方向选项
		mEncDir = new PopWindowAdapter(mContext, null);
// End of H.M.Wang 2022-11-30 追加ENCDir方向选项

// H.M.Wang 2022-8-25 追加喷嘴加热参数项
		mNozzleWarm = new PopWindowAdapter(mContext, null);
// End of H.M.Wang 2022-8-25 追加喷嘴加热参数项

// H.M.Wang 2023-1-5 增加一个快速打印(Fast Print)的参数。通过S24[4]下发给FPGA
		mFastPrint = new PopWindowAdapter(mContext, null);
// End of H.M.Wang 2023-1-5 增加一个快速打印(Fast Print)的参数。通过S24[4]下发给FPGA

// H.M.Wang 2023-2-15 增加一个快捷模式/Easy mode的参数。用来区分启动哪个用户界面
		mUserMode = new PopWindowAdapter(mContext, null);
// End of H.M.Wang 2023-2-15 增加一个快捷模式/Easy mode的参数。用来区分启动哪个用户界面

// H.M.Wang 2023-5-15 增加旋转屏幕，在180度之间转换
		mLCDInverse = new PopWindowAdapter(mContext, null);
// End of H.M.Wang 2023-5-15 增加旋转屏幕，在180度之间转换

// H.M.Wang 2023-10-13 增加一个AD参数，当AD=0时，按原有策略(根据img的类型显示电池图标）；当AD=1时，无条件显示电池图标；当AD=2时，显示气压参数，具体方法待定
		mParamAD = new PopWindowAdapter(mContext, null);
// End of H.M.Wang 2023-10-13 增加一个AD参数，当AD=0时，按原有策略(根据img的类型显示电池图标）；当AD=1时，无条件显示电池图标；当AD=2时，显示气压参数，具体方法待定

// H.M.Wang 2024-7-27 追加蓝牙设备号和蓝牙开关功能
		mBLEEnableAdapter = new PopWindowAdapter(mContext, null);
// End of H.M.Wang 2024-7-27 追加蓝牙设备号和蓝牙开关功能

		initAdapters();

		// H.M.Wang 增加3行。注册广播接收器，接收计数器更新值，设置到编辑区内
		IntentFilter iFilter = new IntentFilter();
		iFilter.addAction(ACTION_PARAM_CHANGED);
		context.registerReceiver(mReceiver, iFilter);

		/////////////////////////////////////////////////////////////
		// 注意：mReceiver正常情况下应该在析构函数中unregister掉，由于Adapter
		//      实例仅在系统启动时生成一次，并不会被析构，因此暂时无碍
		/////////////////////////////////////////////////////////////
	}

	public void destroy() {
		mContext.unregisterReceiver(mReceiver);
	}

	@Override
	public int getCount() {
		Debug.d(TAG, "--->getCount=" + mSettingItems.length);
		return mSettingItems.length/2;
		//return Configs.gParams;
	}

	@Override
	public Object getItem(int arg0) {
		Debug.d(TAG, "--->getItem");
		return null;
	}

	@Override
	public long getItemId(int arg0) {
		return arg0;
	}

// H.M.Wang 2023-2-27 追加TextView的右侧箭头记忆功能，因为如果是对话窗种类的内容的时候，会将右侧的箭头设为空，然后就不再有这个剪头了，导致后续即使是SWITCH类型的需要箭头也不再显示了
	private Drawable mLeftDrawable;
	private Drawable mRightDrawable;
// End of H.M.Wang 2023-2-27 追加TextView的右侧箭头记忆功能，因为如果是对话窗种类的内容的时候，会将右侧的箭头设为空，然后就不再有这个剪头了，导致后续即使是SWITCH类型的需要箭头也不再显示了

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		if(convertView != null) {
			mHolder = (ItemViewHolder) convertView.getTag();
		}
		else
		{
			//prepare a empty view 
			convertView = mInflater.inflate(R.layout.settings_frame_item, null);
			mHolder = new ItemViewHolder();
			//Left 
			mHolder.mTitleL = (TextView) convertView.findViewById(R.id.setting_title_left);
			mHolder.mValueLTv = (TextView) convertView.findViewById(R.id.setting_value_left_tv);
			mLeftDrawable =mHolder.mValueLTv.getCompoundDrawables()[2];
			mHolder.mValueLEt = (EditText) convertView.findViewById(R.id.setting_value_left_et);
			mHolder.mUnitL = (TextView) convertView.findViewById(R.id.setting_unit_left);
			//Right
			mHolder.mTitleR = (TextView) convertView.findViewById(R.id.setting_title_right);
			mHolder.mValueRTv = (TextView) convertView.findViewById(R.id.setting_value_right_tv);
			mRightDrawable =mHolder.mValueRTv.getCompoundDrawables()[2];
			mHolder.mValueREt = (EditText) convertView.findViewById(R.id.setting_value_right_et);
			mHolder.mUnitR = (TextView) convertView.findViewById(R.id.setting_unit_right);

			// H.M.Wang 修改 20190905 添加这两行，生成控件的时候添加设置
			mHolder.mValueLEt.addTextChangedListener(new SelfTextWatcher(mHolder.mValueLEt));
			mHolder.mValueREt.addTextChangedListener(new SelfTextWatcher(mHolder.mValueREt));

			convertView.setTag(mHolder);
//			mHoldMap.put(position, mHolder);
			
		}
		mHolder.mValueLEt.setTag(2*position);
		mHolder.mValueLTv.setTag(2*position);
		mHolder.mValueREt.setTag(2*position+1);
		mHolder.mValueRTv.setTag(2*position+1);
		// H.M.Wang 修改 20190905 注释掉这两行，改在生成控件的时候设置
//		mHolder.mValueLEt.addTextChangedListener(new SelfTextWatcher(mHolder.mValueLEt));
//		mHolder.mValueREt.addTextChangedListener(new SelfTextWatcher(mHolder.mValueREt));

		if (mSettingItems[2*position].mUnit > 0) {
			mHolder.mUnitL.setText(mSettingItems[2*position].mUnit);
		} else {
			mHolder.mUnitL.setText("");
		}
		if (mSettingItems[2*position+1].mUnit > 0) {
			mHolder.mUnitR.setText(mSettingItems[2*position+1].mUnit);
		} else {
			mHolder.mUnitR.setText("");
		}
		mHolder.mTitleL.setText(mContext.getString(mSettingItems[2*position].mTitle));
		mHolder.mTitleR.setText(mContext.getString(mSettingItems[2*position+1].mTitle));
//		Debug.d(TAG, "===>getView position=" + position);

		if (mSettingItems[2*position].mType != ItemType.TYPE_NONE) {
			mHolder.mValueLTv.setVisibility(View.VISIBLE);
			mHolder.mValueLEt.setVisibility(View.GONE);
			mHolder.mValueLTv.setText(mSettingItems[2*position].getDisplayValue());
			mHolder.mValueLTv.setOnClickListener(this);
// H.M.Wang 2020-4-24 追加计数器编辑类型，但是取消右侧的小箭头
			if(mSettingItems[2*position].mType == ItemType.TYPE_DIALOG) {
				mHolder.mValueLTv.setCompoundDrawables(null, null, null, null);
			} else {
				mHolder.mValueLTv.setCompoundDrawables(null, null, mLeftDrawable, null);
			}
// End of H.M.Wang 2020-4-24 追加计数器编辑类型，但是取消右侧的小箭头
		} else {
			mHolder.mValueLTv.setVisibility(View.GONE);
			mHolder.mValueLEt.setVisibility(View.VISIBLE);
//			Debug.d(TAG, "--->getView:left=" + mSettingItems[2*position].mValue + "---right=" + mSettingItems[2*position+1].mValue);
			mHolder.mValueLEt.setText(mSettingItems[2*position].getDisplayValue());
		}
		
		if (mSettingItems[2*position + 1].mType != ItemType.TYPE_NONE) {
			mHolder.mValueRTv.setVisibility(View.VISIBLE);
			mHolder.mValueREt.setVisibility(View.GONE);
			mHolder.mValueRTv.setText(mSettingItems[2*position+1].getDisplayValue());
			mHolder.mValueRTv.setOnClickListener(this);
// H.M.Wang 2020-4-24 追加计数器编辑类型，但是取消右侧的小箭头
			if(mSettingItems[2*position + 1].mType == ItemType.TYPE_DIALOG) {
				mHolder.mValueRTv.setCompoundDrawables(null, null, null, null);
			} else {
				mHolder.mValueRTv.setCompoundDrawables(null, null, mRightDrawable, null);
			}
// End of H.M.Wang 2020-4-24 追加计数器编辑类型，但是取消右侧的小箭头
		} else {
			mHolder.mValueRTv.setVisibility(View.GONE);
			mHolder.mValueREt.setVisibility(View.VISIBLE);
			mHolder.mValueREt.setText(mSettingItems[2*position+1].getDisplayValue());
// H.M.Wang 2023-7-17 3.5寸盘亮度固定为50，其余不变
			if(!mHolder.mValueREt.isEnabled()) mHolder.mValueREt.setEnabled(true);
			if(2*position + 1 == 43) {
				if(PlatformInfo.is3InchType()) {
					mHolder.mValueREt.setEnabled(false);
					mHolder.mValueREt.setText("50");
				}
			}
// End of H.M.Wang 2023-7-17 3.5寸盘亮度固定为50，其余不变
		}

		return convertView;
	}

	public void loadSettings() {
		Debug.d(TAG, "--->loadSettings");
        long[] counters = RTCDevice.getInstance(mContext).readAll();
        if (counters != null && counters.length > 0) {
        	for (int i = 0; i < counters.length; i++) {
        		SystemConfigFile.getInstance(mContext).setParam(SystemConfigFile.INDEX_COUNT_1 + i, (int) counters[i]);
				Debug.d(TAG, "Counter[" + i + "] ---> " + (int) counters[i]);
			}
		}
		mSettingItems[0] = new ItemOneLine(1, R.string.str_textview_param1, R.string.str_time_unit_mm_s);
		mSettingItems[1] = new ItemOneLine(2, R.string.str_textview_param2, R.array.direction_item_entries, 0, ItemType.TYPE_DIRECTION);
		mSettingItems[2] = new ItemOneLine(3, R.string.str_textview_param3, R.array.resolution_item_entries, R.string.strResunit, ItemType.TYPE_VALUE);
		mSettingItems[3] = new ItemOneLine(4, R.string.str_textview_param4, R.string.str_length_unit_mm);
		mSettingItems[4] = new ItemOneLine(5, R.string.str_textview_param5, R.array.photo_item_entries, 	0, ItemType.TYPE_SWITCH);
		mSettingItems[5] = new ItemOneLine(6, R.string.str_textview_param6, R.array.switch_item_entries, 	0, ItemType.TYPE_SWITCH);
// H.M.Wang 2020-4-27 重复打印设置改为对话窗
		mSettingItems[6] = new ItemOneLine(7, R.string.str_textview_param7, R.string.str_length_unit_mm, ItemType.TYPE_DIALOG);
// End of H.M.Wang 2020-4-27 重复打印设置改为对话窗
		mSettingItems[7] = new ItemOneLine(8, R.string.str_textview_param8, R.array.direction_item_entries, 0, ItemType.TYPE_DIRECTION);
		mSettingItems[8] = new ItemOneLine(9, R.string.str_textview_param9, R.string.str_length_unit_mm);
// H.M.Wang 2020-4-27 编码器脉冲设置改为对话窗
		mSettingItems[9] = new ItemOneLine(10,R.string.str_textview_param10,0, ItemType.TYPE_DIALOG);
// H.M.Wang 2020-4-27 编码器脉冲设置改为对话窗
		mSettingItems[10] = new ItemOneLine(11, R.string.str_textview_param11, R.string.str_length_unit_6_1mm);
		mSettingItems[11] = new ItemOneLine(12, R.string.str_textview_param12, R.string.str_length_unit_6_1mm);
		mSettingItems[12] = new ItemOneLine(13, R.string.str_textview_param13, R.array.switch_item_entries, 0, ItemType.TYPE_SWITCH);
		mSettingItems[13] = new ItemOneLine(14, R.string.str_textview_param14, R.array.switch_item_entries, 0, ItemType.TYPE_SWITCH);
		mSettingItems[14] = new ItemOneLine(15, R.string.str_textview_param15, R.array.switch_item_entries, 0, ItemType.TYPE_SWITCH);
		mSettingItems[15] = new ItemOneLine(16, R.string.str_textview_param16, R.array.switch_item_entries, 0, ItemType.TYPE_SWITCH);
		mSettingItems[16] = new ItemOneLine(17, R.string.str_textview_param17, 0);

		// H.M.Wang 修改2行。为计数器清楚前置0
//		mSettingItems[17] = new ItemOneLine(18, R.string.str_textview_param18, 0);
		mSettingItems[17] = new ItemOneLine(18, R.string.str_textview_param18, R.array.switch_item_entries, 0, ItemType.TYPE_SWITCH);

		mSettingItems[18] = new ItemOneLine(19, R.string.str_textview_param19, R.string.str_length_unit_6_1mm);
		mSettingItems[19] = new ItemOneLine(20, R.string.str_textview_param20, R.string.str_length_unit_6_1mm);
		mSettingItems[20] = new ItemOneLine(21, R.string.str_textview_param21, R.array.switch_item_entries, 0, ItemType.TYPE_SWITCH);
		mSettingItems[21] = new ItemOneLine(22, R.string.str_textview_param22, R.array.switch_item_entries, 0, ItemType.TYPE_SWITCH);
		mSettingItems[22] = new ItemOneLine(23, R.string.str_textview_param23, R.array.switch_item_entries, 0, ItemType.TYPE_SWITCH);
		mSettingItems[23] = new ItemOneLine(24, R.string.str_textview_param24, R.array.switch_item_entries, 0, ItemType.TYPE_SWITCH);
		mSettingItems[24] = new ItemOneLine(25, R.string.str_textview_param25, R.array.switch_item_entries, 0, ItemType.TYPE_SWITCH);
		mSettingItems[25] = new ItemOneLine(26, R.string.str_textview_param26, R.string.str_time_unit_0_1v);
		mSettingItems[26] = new ItemOneLine(27, R.string.str_textview_param27, R.array.switch_item_entries, 0, ItemType.TYPE_SWITCH);
		mSettingItems[27] = new ItemOneLine(28, R.string.str_textview_param28, R.string.str_time_unit_0_1us);
		mSettingItems[28] = new ItemOneLine(29, R.string.str_textview_param29, R.string.str_time_unit_ms);
		mSettingItems[29] = new ItemOneLine(30, R.string.str_textview_param30, R.string.str_time_unit_ms);
		mSettingItems[30] = new ItemOneLine(31, R.string.str_textview_param31, R.array.strPrinterArray, 0, ItemType.TYPE_ARRAY);
		mSettingItems[31] = new ItemOneLine(32, R.string.str_textview_param32, R.array.switch_item_entries, 0, ItemType.TYPE_SWITCH);
		mSettingItems[32] = new ItemOneLine(33, R.string.str_textview_param33, R.string.str_time_unit_us);
		mSettingItems[33] = new ItemOneLine(34, R.string.str_textview_param34, R.string.str_time_unit_us);
		mSettingItems[34] = new ItemOneLine(35, R.string.str_textview_param35, R.string.str_time_unit_us);
		mSettingItems[35] = new ItemOneLine(36, R.string.str_textview_param36, R.string.str_time_unit_us);
		mSettingItems[36] = new ItemOneLine(37, R.string.str_textview_param37, R.string.str_time_unit_us);
		mSettingItems[37] = new ItemOneLine(38, R.string.str_textview_param38, 0);
		// H.M.Wang 2019-12-19 追加对参数39的修改，使得其成为数据源选择的参数
//		mSettingItems[38] = new ItemOneLine(39, R.string.str_textview_param39, R.array.switch_item_entries,	0,	ItemType.TYPE_SWITCH);
		mSettingItems[38] = new ItemOneLine(39, R.string.str_textview_param39, R.array.strDataSourceArray,	0,	ItemType.TYPE_ARRAY);
		// End of H.M.Wang 2019-12-19 追加对参数39的修改，使得其成为数据源选择的参数
		mSettingItems[39] = new ItemOneLine(40, R.string.str_textview_param40, R.array.switch_item_entries,	0,	ItemType.TYPE_SWITCH);
// H.M.Wang 2021-7-23 将C41更改为重复次数(Times)的设置，并且取消TYPE_SWITCH，改为数值输入，允许值范围为0-20，其余值均强制修改为0
		// H.M.Wang 2019-12-19 恢复原来参数41的内容
//		mSettingItems[40] = new ItemOneLine(41, R.string.str_textview_param41, R.array.switch_item_entries, 0, ItemType.TYPE_SWITCH);
//		mSettingItems[40] = new ItemOneLine(41, R.string.str_textview_param41, R.array.switch_item_entries, 0, ItemType.TYPE_SWITCH);
		mSettingItems[40] = new ItemOneLine(41, R.string.str_repeat_times, 0);
// H.M.Wang 2021-7-23 将C41更改为重复次数(Times)的设置，并且取消TYPE_SWITCH，改为数值输入，允许值范围为0-20，其余值均强制修改为0
		// End of H.M.Wang 2019-12-19 恢复原来参数41的内容
		mSettingItems[41] = new ItemOneLine(42, R.string.str_textview_param42, 0);
		mSettingItems[42] = new ItemOneLine(43, R.string.str_textview_param43, 0);
		mSettingItems[43] = new ItemOneLine(44, R.string.str_textview_param44, 0);
// H.M.Wang 2020-4-24 重新定义计数器编辑类型
		mSettingItems[44] = new ItemOneLine(45, R.string.str_textview_param45, 0, ItemType.TYPE_DIALOG);
		mSettingItems[45] = new ItemOneLine(46, R.string.str_textview_param46, 0, ItemType.TYPE_DIALOG);
		mSettingItems[46] = new ItemOneLine(47, R.string.str_textview_param47, 0, ItemType.TYPE_DIALOG);
		mSettingItems[47] = new ItemOneLine(48, R.string.str_textview_param48, 0, ItemType.TYPE_DIALOG);
		mSettingItems[48] = new ItemOneLine(49, R.string.str_textview_param49, 0, ItemType.TYPE_DIALOG);
		mSettingItems[49] = new ItemOneLine(50, R.string.str_textview_param50, 0, ItemType.TYPE_DIALOG);
		mSettingItems[50] = new ItemOneLine(51, R.string.str_textview_param51, 0, ItemType.TYPE_DIALOG);
		mSettingItems[51] = new ItemOneLine(52, R.string.str_textview_param52, 0, ItemType.TYPE_DIALOG);
		mSettingItems[52] = new ItemOneLine(53, R.string.str_textview_param53, 0, ItemType.TYPE_DIALOG);
		mSettingItems[53] = new ItemOneLine(54, R.string.str_textview_param54, 0, ItemType.TYPE_DIALOG);
// End of H.M.Wang 2020-4-24 重新定义计数器编辑类型
// H.M.Wang 2020-5-16 QRLast移植RTC的0x38地址保存，可以通过参数设置管理
		mSettingItems[54] = new ItemOneLine(55, R.string.str_textview_param55, 0, ItemType.TYPE_DIALOG);
// End of H.M.Wang 2020-5-16 QRLast移植RTC的0x38地址保存，可以通过参数设置管理
		mSettingItems[55] = new ItemOneLine(56, R.string.str_textview_param56, 0);
// H.M.Wang 2021-9-24 追加输入设置参数
//		mSettingItems[56] = new ItemOneLine(57, R.string.str_textview_param57, 0);
		mSettingItems[56] = new ItemOneLine(57, R.string.str_textview_param57, R.array.input_proc_item_entries, 0, ItemType.TYPE_SWITCH);
// End of H.M.Wang 2021-9-24 追加输入设置参数
		mSettingItems[57] = new ItemOneLine(58, R.string.str_textview_param58, 0);
// H.M.Wang 2022-5-30 增加编码器变倍
		mSettingItems[58] = new ItemOneLine(59, R.string.str_textview_param59, 0);
// End of H.M.Wang 2022-5-30 增加编码器变倍
		mSettingItems[59] = new ItemOneLine(60, R.string.str_textview_param60, 0);
		mSettingItems[60] = new ItemOneLine(61, R.string.str_textview_param61, 0);

// H.M.Wang 2023-2-4 修改参数C62 加热限制/Warm limit 及 参数C63 温度/Warming
// H.M.Wang 2022-8-25 追加喷嘴加热参数项
//		mSettingItems[61] = new ItemOneLine(62, R.string.str_textview_param62, 0);
//		mSettingItems[61] = new ItemOneLine(62, R.string.str_textview_param62, R.array.switch_item_entries, 0, ItemType.TYPE_SWITCH);
		mSettingItems[61] = new ItemOneLine(62, R.string.str_textview_param62, R.string.str_time_unit_min);
// End of H.M.Wang 2022-8-25 追加喷嘴加热参数项
//		mSettingItems[62] = new ItemOneLine(63, R.string.str_textview_param63, 0);
		mSettingItems[62] = new ItemOneLine(63, R.string.str_textview_param63, R.string.str_temperature);
// End of H.M.Wang 2023-2-4 修改参数C62 加热限制/Warm limit 及 参数C63 温度/Warming
		mSettingItems[63] = new ItemOneLine(64, R.string.str_textview_param64, 0);
// H.M.Wang 2022-10-18 参数扩容32项目
		mSettingItems[64] = new ItemOneLine(65, R.string.str_textview_param65, 0);
		mSettingItems[65] = new ItemOneLine(66, R.string.str_textview_param66, 0);
		mSettingItems[66] = new ItemOneLine(67, R.string.str_textview_param67, R.string.str_unit_ps);
// H.M.Wang 2022-11-30 追加ENCDir方向选项
		mSettingItems[67] = new ItemOneLine(68, R.string.str_textview_param68, R.array.enc_dir_item_entries, 0, ItemType.TYPE_SWITCH);
// End of H.M.Wang 2022-11-30 追加ENCDir方向选项
		mSettingItems[68] = new ItemOneLine(69, R.string.str_textview_param69, 0);
// H.M.Wang 2023-1-5 增加一个快速打印(Fast Print)的参数。通过S24[4]下发给FPGA
		mSettingItems[69] = new ItemOneLine(70, R.string.str_textview_param70, R.array.switch_item_entries, 0, ItemType.TYPE_SWITCH);
// End of H.M.Wang 2023-1-5 增加一个快速打印(Fast Print)的参数。通过S24[4]下发给FPGA
// H.M.Wang 2023-2-15 增加一个快捷模式/Easy mode的参数。用来区分启动哪个用户界面
		mSettingItems[70] = new ItemOneLine(71, R.string.str_textview_param71, R.array.user_mode, 0, ItemType.TYPE_SWITCH);
// End of H.M.Wang 2023-2-15 增加一个快捷模式/Easy mode的参数。用来区分启动哪个用户界面
// H.M.Wang 2023-3-12 增加一个PC_FIFO的参数，用来定义PC_FIFO的大小
		mSettingItems[71] = new ItemOneLine(72, R.string.str_textview_param72, 0);
// End of H.M.Wang 2023-3-12 增加一个PC_FIFO的参数，用来定义PC_FIFO的大小
// H.M.Wang 2023-5-15 增加旋转屏幕，在180度之间转换
		mSettingItems[72] = new ItemOneLine(73, R.string.str_textview_param73, R.array.switch_item_entries, 0, ItemType.TYPE_SWITCH);
// End of H.M.Wang 2023-5-15 增加旋转屏幕，在180度之间转换
// H.M.Wang 2023-10-13 增加一个AD参数，当AD=0时，按原有策略(根据img的类型显示电池图标）；当AD=1时，无条件显示电池图标；当AD=2时，显示气压参数，具体方法待定
		mSettingItems[73] = new ItemOneLine(74, R.string.str_textview_param74, R.array.param_ad, 0, ItemType.TYPE_SWITCH);
// End of H.M.Wang 2023-10-13 增加一个AD参数，当AD=0时，按原有策略(根据img的类型显示电池图标）；当AD=1时，无条件显示电池图标；当AD=2时，显示气压参数，具体方法待定
// H.M.Wang 2023-10-26 追加一个参数，当=0时，按当前逻辑回复PC端，当=1时，在打印完成后，回复0002到PC端
		mSettingItems[74] = new ItemOneLine(75, R.string.str_textview_param75, 0);
// End of H.M.Wang 2023-10-26 追加一个参数，当=0时，按当前逻辑回复PC端，当=1时，在打印完成后，回复0002到PC端
// H.M.Wang 2024-3-29 追加一个限制打印次数的参数
		mSettingItems[75] = new ItemOneLine(76, R.string.str_textview_param76, 0);
// End of H.M.Wang 2024-3-29 追加一个限制打印次数的参数
// H.M.Wang 2024-4-3 追加一个22mm的喷头选择参数
		mSettingItems[76] = new ItemOneLine(77, R.string.str_textview_param77, 0, ItemType.TYPE_DIALOG);
// H.M.Wang 2024-4-3 追加一个22mm的喷头选择参数
// H.M.Wang 2024-5-27 临时追加一个DAC5571的设置功能，值从参数中设置，范围为0-255
		mSettingItems[77] = new ItemOneLine(78, R.string.str_textview_param78, R.string.str_unit_kPa);
// End of H.M.Wang 2024-5-27 临时追加一个DAC5571的设置功能，值从参数中设置，范围为0-255
// H.M.Wang 2024-7-27 追加蓝牙设备号和蓝牙开关功能
		mSettingItems[78] = new ItemOneLine(79, R.string.str_textview_param79, R.array.switch_item_entries, 	0, ItemType.TYPE_SWITCH);
// End of H.M.Wang 2024-7-27 追加蓝牙设备号和蓝牙开关功能
// H.M.Wang 2025-3-19 增加Circulation/循环间隔设置
		mSettingItems[79] = new ItemOneLine(80, R.string.str_textview_param80, 0);
// End of H.M.Wang 2025-3-19 增加Circulation/循环间隔设置
		mSettingItems[80] = new ItemOneLine(81, R.string.str_textview_param81, R.string.str_time_unit_s);
		mSettingItems[81] = new ItemOneLine(82, R.string.str_textview_param82, 0);
		mSettingItems[82] = new ItemOneLine(83, R.string.str_textview_param83, 0);
		mSettingItems[83] = new ItemOneLine(84, R.string.str_textview_param84, 0);
		mSettingItems[84] = new ItemOneLine(85, R.string.str_textview_param85, 0);
		mSettingItems[85] = new ItemOneLine(86, R.string.str_textview_param86, 0);
		mSettingItems[86] = new ItemOneLine(87, R.string.str_textview_param87, 0);
		mSettingItems[87] = new ItemOneLine(88, R.string.str_textview_param88, 0);
		mSettingItems[88] = new ItemOneLine(89, R.string.str_textview_param89, 0);
		mSettingItems[89] = new ItemOneLine(90, R.string.str_textview_param90, 0);
		mSettingItems[90] = new ItemOneLine(91, R.string.str_textview_param91, 0);
		mSettingItems[91] = new ItemOneLine(92, R.string.str_textview_param92, 0);
		mSettingItems[92] = new ItemOneLine(93, R.string.str_textview_param93, 0);
		mSettingItems[93] = new ItemOneLine(94, R.string.str_textview_param94, 0);
		mSettingItems[94] = new ItemOneLine(95, R.string.str_textview_param95, 0);
		mSettingItems[95] = new ItemOneLine(96, R.string.str_textview_param96, 0);
// End of H.M.Wang 2022-10-18 参数扩容32项目
		Debug.d(TAG, "--->loadSettings");
	}

	private void initAdapters() {
		
		mSpiner = new PopWindowSpiner(mContext);
		mSpiner.setFocusable(true);
		mSpiner.setOnItemClickListener(this);
		
		String[] items = mContext.getResources().getStringArray(R.array.switch_item_entries); 
		// ArrayAdapter<String> adapter = new ArrayAdapter<String>(mContext, R.layout.spinner_item, R.id.textView_id, items);
		for (int i = 0; i < items.length; i++) {
			mEncoderAdapter.addItem(items[i]);
		}
		
		items = mContext.getResources().getStringArray(R.array.array_triger_mode); 
		// ArrayAdapter<String> adapter = new ArrayAdapter<String>(mContext, R.layout.spinner_item, R.id.textView_id, items);
		for (int i = 0; i < items.length; i++) {
			mTrigerMode.addItem(items[i]);
		}
		
		items = mContext.getResources().getStringArray(R.array.direction_item_entries);
		for (int i = 0; i < items.length; i++) {
			mDirection.addItem(items[i]);
		}
		
		items = mContext.getResources().getStringArray(R.array.resolution_item_entries);
		for (int i = 0; i < items.length; i++) {
			mResolution.addItem(items[i]);
		}

		items = mContext.getResources().getStringArray(R.array.photo_item_entries);
		for (int i = 0; i < items.length; i++) {
			mPhotocell.addItem(items[i]);
		}
		
		items = mContext.getResources().getStringArray(R.array.direction_item_entries);
// H.M.Wang 2020-4-27 重复打印设置改为对话窗
//		for (int i = 0; i < items.length; i++) {
//			mRepeat.addItem(items[i]);
//		}
// End of H.M.Wang 2020-4-27 重复打印设置改为对话窗
		for (int i = 0; i < items.length; i++) {
			mNozzle.addItem(items[i]);
		}
		
		items = mContext.getResources().getStringArray(R.array.switch_item_entries);
		for (int i = 0; i < items.length; i++) {
			mPen1Mirror.addItem(items[i]);
		}
		for (int i = 0; i < items.length; i++) {
			mPen2Mirror.addItem(items[i]);
		}
		for (int i = 0; i < items.length; i++) {
			mPen3Mirror.addItem(items[i]);
		}
		for (int i = 0; i < items.length; i++) {
			mPen4Mirror.addItem(items[i]);
		}
		for (int i = 0; i < items.length; i++) {
			mPen1Invert.addItem(items[i]);
		}
		for (int i = 0; i < items.length; i++) {
			mPen2Invert.addItem(items[i]);
		}
		for (int i = 0; i < items.length; i++) {
			mPen3Invert.addItem(items[i]);
		}
		for (int i = 0; i < items.length; i++) {
			mPen4Invert.addItem(items[i]);
		}


		// H.M.Wang 增加3行。为计数器清楚前置0
		for (int i = 0; i < items.length; i++) {
			mClearZero.addItem(items[i]);
		}

		for (int i = 0; i < items.length; i++) {
			mAutoVol.addItem(items[i]);
		}
		for (int i = 0; i < items.length; i++) {
			mAutoPulse.addItem(items[i]);
		}
		for (int i = 0; i < items.length; i++) {
			mCntReset.addItem(items[i]);
		}
		for (int i = 0; i < items.length; i++) {
			mHandle.addItem(items[i]);
		}
//		for (int i = 0; i < items.length; i++) {
//			mQRsource.addItem(items[i]);
//		}
		for (int i = 0; i < items.length; i++) {
			mBeep.addItem(items[i]);
		}

// H.M.Wang 2022-8-25 追加喷嘴加热参数项
		for (int i = 0; i < items.length; i++) {
			mNozzleWarm.addItem(items[i]);
		}
// End of H.M.Wang 2022-8-25 追加喷嘴加热参数项
// H.M.Wang 2023-1-5 增加一个快速打印(Fast Print)的参数。通过S24[4]下发给FPGA
		for (int i = 0; i < items.length; i++) {
			mFastPrint.addItem(items[i]);
		}
// End of H.M.Wang 2023-1-5 增加一个快速打印(Fast Print)的参数。通过S24[4]下发给FPGA

// H.M.Wang 2023-5-15 增加旋转屏幕，在180度之间转换
		for (int i = 0; i < items.length; i++) {
			mLCDInverse.addItem(items[i]);
		}
// End of H.M.Wang 2023-5-15 增加旋转屏幕，在180度之间转换

// H.M.Wang 2023-10-13 增加一个AD参数，当AD=0时，按原有策略(根据img的类型显示电池图标）；当AD=1时，无条件显示电池图标；当AD=2时，显示气压参数，具体方法待定
		items = mContext.getResources().getStringArray(R.array.param_ad);
		for (int i = 0; i < items.length; i++) {
			mParamAD.addItem(items[i]);
		}
// End of H.M.Wang 2023-10-13 增加一个AD参数，当AD=0时，按原有策略(根据img的类型显示电池图标）；当AD=1时，无条件显示电池图标；当AD=2时，显示气压参数，具体方法待定

// H.M.Wang 2023-2-15 增加一个快捷模式/Easy mode的参数。用来区分启动哪个用户界面
		items = mContext.getResources().getStringArray(R.array.user_mode);
		for (int i = 0; i < items.length; i++) {
			mUserMode.addItem(items[i]);
		}
// End of H.M.Wang 2023-2-15 增加一个快捷模式/Easy mode的参数。用来区分启动哪个用户界面

		// H.M.Wang 2019-12-19 追加对参数39的修改为数据源选择的参数，该设置适配器停用
//		for (int i = 0; i < items.length; i++) {
//			mLan.addItem(items[i]);
//		}
		// End of H.M.Wang 2019-12-19 追加对参数39的修改为数据源选择的参数，该设置适配器停用

		items = mContext.getResources().getStringArray(R.array.pens_item_entries);
		for (int i = 0; i < items.length; i++) {
			mPens.addItem(items[i]);
		}
		
		items = mContext.getResources().getStringArray(R.array.message_dots);
		for (int i = 0; i < items.length; i++) {
			mDots.addItem(items[i]);
		}

// H.M.Wang 2021-9-24 追加输入设置参数
		items = mContext.getResources().getStringArray(R.array.input_proc_item_entries);
		for (int i = 0; i < items.length; i++) {
			mInputProc.addItem(items[i]);
		}
// End of H.M.Wang 2021-9-24 追加输入设置参数

// H.M.Wang 2022-11-30 追加ENCDir方向选项
		items = mContext.getResources().getStringArray(R.array.enc_dir_item_entries);
		for (int i = 0; i < items.length; i++) {
			mEncDir.addItem(items[i]);
		}
// End of H.M.Wang 2022-11-30 追加ENCDir方向选项

// H.M.Wang 2024-7-27 追加蓝牙设备号和蓝牙开关功能
		items = mContext.getResources().getStringArray(R.array.switch_item_entries);
		for (int i = 0; i < items.length; i++) {
			mBLEEnableAdapter.addItem(items[i]);
		}
// End of H.M.Wang 2024-7-27 追加蓝牙设备号和蓝牙开关功能
	}

	private String getEntry(int id,int index) {
		String entries[] = mContext.getResources().getStringArray(id);
		// Debug.d(TAG, "--->getEncoder:entries[" + index + "]=" + entries[index]);
		if (entries == null || entries.length <= 0) {
			return null;
		}
		if (index<0 || index >= entries.length) {
			return entries[0];
		}
		return entries[index];
	}
	
	private int getIndexByEntry(int id, String entry) {
		String entries[] = mContext.getResources().getStringArray(id);
		if (entry == null || entries == null || entries.length <= 0) {
			return 0;
		}
		for (int i = 0; i < entries.length; i++) {
			if (entry.equalsIgnoreCase(entries[i])) {
				return i;
			}
		}
		return 0;
	}

	@Override
	public void onClick(View view) {
		int position = 0;
		if (view != null) {
			position = (Integer)view.getTag();
		} else {
			return;
		}
		if (mSettingItems == null ||position >= mSettingItems.length) {
			return;
		}
		if (mSettingItems[position].mType == ItemType.TYPE_NONE) {
			return;
		}

		Debug.d(TAG, "===>onclick " + position);

		mSpiner.setAttachedView(view);
		if (position == 1) { //參數2
			mSpiner.setAdapter(mDirection);
		} else if (position == 2) { //參數3
			mSpiner.setAdapter(mResolution);
		} else if (position == 4) { //參數5
			mSpiner.setAdapter(mPhotocell);
		} else if (position == 5) { //參數6
			mSpiner.setAdapter(mEncoderAdapter);
		} else if (position == 6) { //參數7
// H.M.Wang 2020-4-27 重复打印设置改为对话窗
			PrintRepeatEditDialog dialog = new PrintRepeatEditDialog(mContext, mHandler, mSettingItems[position].getDisplayValue());
			dialog.show();
			return;
//			mSpiner.setAdapter(mRepeat);
// End of H.M.Wang 2020-4-27 重复打印设置改为对话窗
		} else if (position == 7) { //參數8
			mSpiner.setAdapter(mNozzle);
// H.M.Wang 2020-4-27 编码器脉冲设置改为对话窗
		} else if (position == 9) { //參數10
			EncoderPPREditDialog dialog = new EncoderPPREditDialog(mContext, mHandler, mSettingItems[position].getDisplayValue());
			dialog.show();
			return;
// H.M.Wang 2020-4-27 编码器脉冲设置改为对话窗
		} else if (position == 12) { //參數13
			mSpiner.setAdapter(mPen1Mirror);
		} else if (position == 13) { //參數14
			mSpiner.setAdapter(mPen2Mirror);
		} else if (position == 14) { //參數15
			mSpiner.setAdapter(mPen1Invert);
		} else if (position == 15) { //參數16
			mSpiner.setAdapter(mPen2Invert);
		}/* else if (position == 16) { //參數17
			mSpiner.setAdapter(mQRsource);
		}*/ else if (position == 20) { //參數21
			mSpiner.setAdapter(mPen3Mirror);

		// H.M.Wang 增加2行。为计数器清楚前置0
		} else if (position == 17) { //參數18
			mSpiner.setAdapter(mClearZero);

		} else if (position == 21) { //參數22
			mSpiner.setAdapter(mPen4Mirror);
		} else if (position == 22) { //參數23
			mSpiner.setAdapter(mPen3Invert);
		} else if (position == 23) { //參數24
			mSpiner.setAdapter(mPen4Invert);
		} else if (position == 24) { //參數25
			mSpiner.setAdapter(mAutoVol);
		} else if (position == 26) { //參數27
			mSpiner.setAdapter(mAutoPulse);
		} else if (position == 30) { //參數31
			HeaderSelectDialog dialog = new HeaderSelectDialog(mContext, mHandler, mSysconfig.getParam(30));
			dialog.show();
			return;
		} else if (position == 31) {
			mSpiner.setAdapter(mCntReset);
		} else if (position == 38) {
			// H.M.Wang 2019-12-19 追加对参数39的修改为数据源选择的参数，该设置适配器停用。改为全屏对话窗模式
			// mSpiner.setAdapter(mLan);
			DataSourceSelectDialog dialog = new DataSourceSelectDialog(mContext, mHandler, mSysconfig.getParam(38));
			dialog.show();
			return;
			// End of H.M.Wang 2019-12-19 追加对参数39的修改为数据源选择的参数，该设置适配器停用。改为全屏对话窗模式
		} else if (position == 39) { //參數40
			mSpiner.setAdapter(mBeep);
		} else if (position == 40) { //鍙冩暩40
			mSpiner.setAdapter(mHandle);
// H.M.Wang 2020-4-24 追加计数器编辑功能，启动对话窗进行编辑，而不是直接在EditText当中编辑
		} else if (position >= 44 && position <= 53) { // Counter Setting
			CounterEditDialog dialog = new CounterEditDialog(mContext, position-44, mSettingItems[position].getDisplayValue());
			dialog.show();
			return;
// End of H.M.Wang 2020-4-24 追加计数器编辑功能，启动对话窗进行编辑，而不是直接在EditText当中编辑
// H.M.Wang 2020-5-16 QRLast移植RTC的0x38地址保存，可以通过参数设置管理
		} else if (position == 54) { 				// QRLast
			QRLastEditDialog dialog = new QRLastEditDialog(mContext, mHandler, mSettingItems[position].getDisplayValue());
			dialog.show();
			return;
// End of H.M.Wang 2020-5-16 QRLast移植RTC的0x38地址保存，可以通过参数设置管理
// H.M.Wang 2021-9-24 追加输入设置参数
		} else if (position == 56) { //參數57
			mSpiner.setAdapter(mInputProc);
// End of H.M.Wang 2021-9-24 追加输入设置参数
// H.M.Wang 2022-8-25 追加喷嘴加热参数项
		} else if (position == 61) { //參數62
			mSpiner.setAdapter(mNozzleWarm);
// End of H.M.Wang 2022-8-25 追加喷嘴加热参数项
// H.M.Wang 2022-11-30 追加ENCDir方向选项
		} else if (position == 67) { //參數68
			mSpiner.setAdapter(mEncDir);
// End of H.M.Wang 2022-11-30 追加ENCDir方向选项
// H.M.Wang 2023-1-5 增加一个快速打印(Fast Print)的参数。通过S24[4]下发给FPGA
		} else if (position == 69) { //參數70
			mSpiner.setAdapter(mFastPrint);
// End of H.M.Wang 2023-1-5 增加一个快速打印(Fast Print)的参数。通过S24[4]下发给FPGA
// H.M.Wang 2023-2-15 增加一个快捷模式/Easy mode的参数。用来区分启动哪个用户界面
		} else if (position == 70) { //參數71
			mSpiner.setAdapter(mUserMode);
// End of H.M.Wang 2023-2-15 增加一个快捷模式/Easy mode的参数。用来区分启动哪个用户界面
// H.M.Wang 2023-5-15 增加旋转屏幕，在180度之间转换
		} else if (position == 72) { //參數73
			mSpiner.setAdapter(mLCDInverse);
// End of H.M.Wang 2023-5-15 增加旋转屏幕，在180度之间转换
// H.M.Wang 2023-10-13 增加一个AD参数，当AD=0时，按原有策略(根据img的类型显示电池图标）；当AD=1时，无条件显示电池图标；当AD=2时，显示气压参数，具体方法待定
		} else if (position == 73) { //參數74
			mSpiner.setAdapter(mParamAD);
// End of H.M.Wang 2023-10-13 增加一个AD参数，当AD=0时，按原有策略(根据img的类型显示电池图标）；当AD=1时，无条件显示电池图标；当AD=2时，显示气压参数，具体方法待定
		} else if (position == 76) { //參數77
// H.M.Wang 2024-4-3 追加一个22mm的喷头选择参数
			Hp22mmNozzleSelectDialog dialog = new Hp22mmNozzleSelectDialog(mContext, mHandler, mSettingItems[position].getValue());
			dialog.show();
			return;
//			mSpiner.setAdapter(mRepeat);
// End of H.M.Wang 2024-4-3 追加一个22mm的喷头选择参数
// H.M.Wang 2024-7-27 追加蓝牙设备号和蓝牙开关功能
		} else if (position == 78) { //參數79
			mSpiner.setAdapter(mBLEEnableAdapter);
// End of H.M.Wang 2024-7-27 追加蓝牙设备号和蓝牙开关功能
		}
		mSpiner.setWidth(view.getWidth());
		//mSpiner.showAsDropDown(view);
		mSpiner.showAsDropUp(view);
	}

	@Override
	public void onItemClick(int index) {
		TextView view = mSpiner.getAttachedView();
		int position = (Integer) view.getTag();
		String value = null;
		if (mSettingItems == null ||position >= mSettingItems.length) {
			return;
		}
		mSettingItems[position].setValue(index);
		mSysconfig.setParam(position, mSettingItems[position].getValue());
		value = mSettingItems[position].getDisplayValue();
		view.setText(value);
	}
	
	/**
	 * 检查已给定参数是否越界，如果越界就重置为默认值	
	 */
	public void checkParams() {
		Debug.d(TAG, "===>checkParams");
		int value = 0;
		
		for (int i = 1; i < mSysconfig.getParams().length; i++) {
			 value = mSysconfig.checkParam(i, mSysconfig.getParam(i-1));
// H.M.Wang 2025-2-7 对于C63参数，当为22mm的时候，最大值设置为55，非22mm的时候，最大值为40
			if(!PlatformInfo.getImgUniqueCode().startsWith("22MM") && i == 63) {
				value = Math.min(40, value);
			}
// End of H.M.Wang 2025-2-7 对于C63参数，当为22mm的时候，最大值设置为55，非22mm的时候，最大值为40
			 if (value != mSysconfig.getParam(i-1)) {
				 mSysconfig.setParam(i-1, value);
				 mSettingItems[i-1].mValue = String.valueOf(value);
			 }
		}
		refresh();
	}
	
	public void refresh() {
		notifyDataSetChanged();
	}
	
	private class SelfTextWatcher implements TextWatcher {
		
		private EditText mEditText;
		
		public SelfTextWatcher(EditText e) {
			mEditText = e;
		}
		
		@Override
		public void afterTextChanged(Editable arg0) {
			int pos = (Integer) mEditText.getTag();
			
			Debug.d(TAG, "===>afterTextChanged, position=" + mEditText.getTag());
			if (mSettingItems == null ||pos >= mSettingItems.length) {
				return;
			}
			if (mSettingItems[pos].mType != ItemType.TYPE_NONE) {
				return;
			}
			mSettingItems[pos].mValue = arg0.toString();
//			Debug.d(TAG, "--->param=" + mSettingItems[pos].getDisplayValue());
// H.M.Wang 2021-7-23 修改参数设置，如果取值超出范围，则强制修正
//			mSysconfig.setParam(pos, getValueFromEditText(arg0));
// H.M.Wang 2025-2-7 对于C63参数，当为22mm的时候，最大值设置为55，非22mm的时候，最大值为40
			if(!PlatformInfo.getImgUniqueCode().startsWith("22MM") && pos+1 == 63) {
				mSysconfig.setParam(pos, Math.min(40, mSysconfig.checkParam(pos+1, getValueFromEditText(arg0))));
			} else {
				mSysconfig.setParam(pos, mSysconfig.checkParam(pos+1, getValueFromEditText(arg0)));
			}
// End of H.M.Wang 2025-2-7 对于C63参数，当为22mm的时候，最大值设置为55，非22mm的时候，最大值为40
// End of H.M.Wang 2021-7-23 修改参数设置，如果取值超出范围，则强制修正
		}
		@Override
		public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
				int arg3) {
			// TODO Auto-generated method stub
			
		}
		@Override
		public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
			// TODO Auto-generated method stub
			
		}
	}

	private int getValueFromEditText(Editable s) {
		int iv = 0;
		String value = s.toString();
		try {
			iv = Integer.parseInt(value);
			Debug.d(TAG, "--->getValueFromEditText:" + iv);
		} catch (Exception e) {
			
		}
		// mHandler.removeMessages(PRINTER_SETTINGS_CHANGED);
		// mHandler.sendEmptyMessageDelayed(PRINTER_SETTINGS_CHANGED, 10000);
		return iv;
	}

	private int getSwitchvalue(int index) {
		return index;
	}
	
	private int getDirectionvalue(int index) {
		return index;
	}
	
	private int getValue(PopWindowAdapter adapter, int index) {
		try {
			int v = Integer.parseInt((String) adapter.getItem(index));
			return v;
		} catch (Exception e) {
			Debug.e(TAG, "--->" + e.getMessage());
			return 0;
		}
	}
	
}
