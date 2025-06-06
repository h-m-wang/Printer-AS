package com.industry.printer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.industry.printer.Bluetooth.BluetoothServerManager;
import com.industry.printer.Constants.Constants;
import com.industry.printer.FileFormat.DotMatrixFont;
import com.industry.printer.FileFormat.QRReader;
import com.industry.printer.FileFormat.SystemConfigFile;
import com.industry.printer.PHeader.PrinterNozzle;
import com.industry.printer.Serial.EC_DOD_Protocol;
import com.industry.printer.Serial.SerialHandler;
import com.industry.printer.Socket_Server.Network;
import com.industry.printer.Socket_Server.PCCommand;
import com.industry.printer.Socket_Server.Paths_Create;
import com.industry.printer.Socket_Server.Printer_Database;
import com.industry.printer.Utils.ByteArrayUtils;
import com.industry.printer.Utils.ConfigPath;
import com.industry.printer.Utils.Configs;
import com.industry.printer.Utils.Debug;

import com.industry.printer.Utils.ExportLog2Usb;
import com.industry.printer.Utils.FileUtil;
import com.industry.printer.Utils.PlatformInfo;
import com.industry.printer.Utils.PreferenceConstants;
import com.industry.printer.Utils.PrinterDBHelper;
import com.industry.printer.Utils.SystemFs;
import com.industry.printer.Utils.ToastUtil;
import com.industry.printer.data.BinFromBitmap;
import com.industry.printer.data.DataTask;
import com.industry.printer.data.PC_FIFO;
import com.industry.printer.data.TxtDT;
import com.industry.printer.hardware.BarcodeScanParser;
import com.industry.printer.hardware.ExtGpio;
import com.industry.printer.hardware.FpgaGpioOperation;
import com.industry.printer.hardware.Hp22mm;
import com.industry.printer.hardware.Hp22mmSCManager;
import com.industry.printer.hardware.IInkDevice;
import com.industry.printer.hardware.InkManagerFactory;
import com.industry.printer.hardware.LRADCBattery;
import com.industry.printer.hardware.N_RFIDManager;
import com.industry.printer.hardware.PI11Monitor;
import com.industry.printer.hardware.RFIDDevice;
import com.industry.printer.hardware.RFIDManager;
import com.industry.printer.hardware.RTCDevice;
import com.industry.printer.hardware.SmartCard;
import com.industry.printer.hardware.SmartCardManager;
import com.industry.printer.hardware.UsbSerial;
import com.industry.printer.interceptor.ExtendInterceptor;
import com.industry.printer.interceptor.ExtendInterceptor.ExtendStat;
import com.industry.printer.object.BarcodeObject;
import com.industry.printer.object.BaseObject;
import com.industry.printer.object.CounterObject;
import com.industry.printer.object.TextObject;
import com.industry.printer.object.TlkObject;
import com.industry.printer.pccommand.PCCommandManager;
import com.industry.printer.ui.CustomerDialog.ConfirmDialog;
import com.industry.printer.ui.CustomerDialog.DialogListener;
import com.industry.printer.ui.CustomerDialog.MessageGroupsortDialog;
import com.industry.printer.ui.CustomerDialog.RemoteMsgPrompt;
import com.industry.printer.ui.CustomerDialog.SubStepDialog;
import com.industry.printer.ui.ExtendMessageTitleFragment;
import com.industry.printer.ui.CustomerAdapter.PreviewAdapter;
import com.industry.printer.ui.CustomerDialog.CustomerDialogBase.OnPositiveListener;
import com.industry.printer.ui.CustomerDialog.LoadingDialog;
import com.industry.printer.ui.CustomerDialog.MessageBrowserDialog;
import com.industry.printer.ui.CustomerDialog.MessageBrowserDialog.OpenFrom;

import android.app.ActionBar.LayoutParams;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ControlTabActivity extends Fragment implements OnClickListener, InkLevelListener, OnTouchListener, DataTransferThread.Callback {
	private static final String TAG = ControlTabActivity.class.getSimpleName();
	
	public static final String ACTION_REOPEN_SERIAL="com.industry.printer.ACTION_REOPEN_SERIAL";
	public static final String ACTION_CLOSE_SERIAL="com.industry.printer.ACTION_CLOSE_SERIAL";
	public static final String ACTION_BOOT_COMPLETE="com.industry.printer.ACTION_BOOT_COMPLETED";

	private int repeatTimes = 10;

	public Context mContext;
	public ExtendMessageTitleFragment mMsgTitle;
	public int mCounter;
	public RelativeLayout mBtnStart;
	public TextView		  mTvStart; 
	public RelativeLayout mBtnStop;
	public TextView		  mTvStop;
	public RelativeLayout mBtnClean;
	public TextView		  mTvClean;
	public Button mBtnOpen;
	public TextView		  mTvOpen;
	//public Button mGoto;
	//public EditText mDstline;

// H.M.Wang 2024-7-27 追加蓝牙设备号和蓝牙开关功能
	public TextView		  mBleState;
// End of H.M.Wang 2024-7-27 追加蓝牙设备号和蓝牙开关功能

	public RelativeLayout	mBtnOpenfile;
	public LinearLayout mllPreview;
	public HorizontalScrollView mScrollView;
	public TextView mMsgFile;
	private TextView tvMsg;

// H.M.Wang 2023-6-14 追加一个监视SC初始化出现失败状态的功能，监视信息包括：初始化失败次数，致命失败次数，写锁值失败次数，致命写锁值失败次数
	private TextView mSCMonitorInfo;
// End of H.M.Wang 2023-6-14 追加一个监视SC初始化出现失败状态的功能，监视信息包括：初始化失败次数，致命失败次数，写锁值失败次数，致命写锁值失败次数

// H.M.Wang 2020-1-7 追加群组打印时，显示正在打印的MSG的序号
    public TextView mGroupIndex;
// End of H.M.Wang 2020-1-7 追加群组打印时，显示正在打印的MSG的序号

	// public EditText mMsgPreview;
//	public TextView mMsgPreview;
//	public ImageView mMsgPreImg;
//	public Button 	mBtnview;
//	public RelativeLayout	mForward;
//	public RelativeLayout 	mBackward;
	
//	public TextView mRecords;
	
	public LinkedList<Map<String, String>>	mMessageMap;
	public PreviewAdapter mMessageAdapter;
	public ListView mMessageList;
	
//	public PreviewScrollView mPreview;
	public ArrayList<BaseObject> mObjList;
	
//	public static int mFd;
	
	BroadcastReceiver mReceiver;
	public Handler mCallback;

	private boolean mFlagAlarming = false;
	
//	public static FileInputStream mFileInputStream;
//	Vector<Vector<TlkObject>> mTlkList;
//	Map<Vector<TlkObject>, byte[]> mBinBuffer;
	/*
	 * whether the print-head is doing print work
	 * if no, poll state Thread will read print-header state
	 * 
	 */
	public boolean isRunning;
	// public PrintingThread mPrintThread;
	public DataTransferThread mDTransThread;
	
	private ExtendStat extendStat = null;
	
	public int mIndex;
	public TextView mPrintStatus;
// H.M.Wang 2023-1-17 修改主页面的显示逻辑，取消原来的锁值显示，将原来的锁值和剩余打印次数合并，显示在画面的左下角，并且同时显示最多6个头的锁值和剩余次数
//	public TextView mtvInk;
//	public TextView mInkLevel;
//	public TextView mInkLevel2;
	private TextView[] mInkValues;
	private LinearLayout mInkValuesGroup1;
	private LinearLayout mInkValuesGroup2;
// End of H.M.Wang 2023-1-17 修改主页面的显示逻辑，取消原来的锁值显示，将原来的锁值和剩余打印次数合并，显示在画面的左下角，并且同时显示最多6个头的锁值和剩余次数
	public TextView mTVPrinting;
	public TextView mTVStopped;
	public TextView mPhotocellState;
	public TextView mEncoderState;
//	public RelativeLayout mPower;
	public TextView mPowerV;
	private ImageView mPowerStat;
	public TextView mTime;
// H.M.Wang 2023-9-20 追加一个步长细分数值显示的功能
	public TextView mSubStepTV;
// End of H.M.Wang 2023-9-20 追加一个步长细分数值显示的功能

	private ImageButton mMsgNext;
	private ImageButton mMsgPrev;

	private TextView mImportBtn;
	private EditText mCntET;
	private TextView mOKBtn;
	private LinearLayout mEditArea;
	private LoadingDialog mProgressDialog;
	private MessageTask mEditTask;
	private TextObject mEditObject;

// H.M.Wang 2024-7-10 追加错误信息返回主控制页面显示的功能
	private TextView mHp22mmErrTV;
// End of H.M.Wang 2024-7-10 追加错误信息返回主控制页面显示的功能
// H.M.Wang 2024-9-21 追加一个显示FPGA驱动状态的功能，当前只显示跳空次数
	private TextView mDriverState;
// End of H.M.Wang 2024-9-21 追加一个显示FPGA驱动状态的功能，当前只显示跳空次数

	// H.M.Wang 2020-8-11 将原来显示在画面头部的墨量和减锁信息移至ControlTab
	public TextView mCtrlTitle;
// H.M.Wang 2023-1-17 修改主页面的显示逻辑，取消原来的锁值显示，将原来的锁值和剩余打印次数合并，显示在画面的左下角，并且同时显示最多6个头的锁值和剩余次数
//	public TextView mCountdown;
// End of H.M.Wang 2023-1-17 修改主页面的显示逻辑，取消原来的锁值显示，将原来的锁值和剩余打印次数合并，显示在画面的左下角，并且同时显示最多6个头的锁值和剩余次数
// End of H.M.Wang 2020-8-11 将原来显示在画面头部的墨量和减锁信息移至ControlTab

	public SystemConfigFile mSysconfig;
	/**
	 * UsbSerial device name
	 */
	public String mSerialdev;
	
	private RFIDDevice mRfidDevice;
	private IInkDevice mInkManager;
	/**
	 * current tlk path opened
	 */
	public String mObjPath=null;
// H.M.Wang 2022-11-29 追加一个UserGroup的子信息名称管理队列和当前子信息位置的变量
	private ArrayList<String> mUGSubObjs = null;
	private int mUGSubIndex = 0;
// End of H.M.Wang 2022-11-29 追加一个UserGroup的子信息名称管理队列和当前子信息位置的变量

// H.M.Wang 2021-3-2 修改初值为0
//    private int mRfid = 100;
//    private int mRfid = 0;
// End of H.M.Wang 2021-3-2 修改初值为0
	/**
	 * MESSAGE_OPEN_TLKFILE
	 *   message tobe sent when open tlk file
	 */
	public static final int MESSAGE_OPEN_TLKFILE=0;
	/**
	 * MESSAGE_UPDATE_PRINTSTATE
	 *   message tobe sent when update print state
	 */
	public static final int MESSAGE_UPDATE_PRINTSTATE=1;
	/**
	 * MESSAGE_UPDATE_INKLEVEL
	 *   message tobe sent when update ink level
	 */
	public static final int MESSAGE_UPDATE_INKLEVEL=2;
	/**
	 * MESSAGE_DISMISS_DIALOG
	 *   message tobe sent when dismiss loading dialog 
	 */
	public static final int MESSAGE_DISMISS_DIALOG=3;
	
	/**
	 * MESSAGE_PAOMADENG_TEST
	 *   message tobe sent when dismiss loading dialog 
	 */
	public static final int MESSAGE_PAOMADENG_TEST=4;

	/**
	 * MESSAGE_PRINT_START
	 *   message tobe sent when dismiss loading dialog 
	 */
	public static final int MESSAGE_PRINT_START = 5;
	public static final int MESSAGE_PRINT_CHECK_UID = 15;
	
	/**
	 * MESSAGE_PRINT_STOP
	 *   message tobe sent when dismiss loading dialog 
	 */
	public static final int MESSAGE_PRINT_STOP = 6;
	
	public static final int MESSAGE_PRINT_END = 14;
	
	/**
	 * MESSAGE_INKLEVEL_DOWN
	 *   message tobe sent when ink level change 
	 */
	public static final int MESSAGE_INKLEVEL_CHANGE = 7;
	
	/**
	 * MESSAGE_COUNT_CHANGE
	 *   message tobe sent when count change 
	 */
	public static final int MESSAGE_COUNT_CHANGE = 8;
	
	public static final int MESSAGE_REFRESH_POWERSTAT = 9;
	
	public static final int MESSAGE_SWITCH_RFID = 10;
	
	
	public static final int MESSAGE_RFID_LOW = 11;
	
	public static final int MESSAGE_RFID_ZERO = 12;
	
	public static final int MESSAGE_RFID_ALARM = 13;
	
	public static final int MESSAGE_RECOVERY_PRINT = 16;
	
	public static final int MESSAGE_OPEN_MSG_SUCCESS = 17;
	
	public static final int MESSAGE_RFID_OFF_H7 = 18;

	public static final int MESSAGE_OPEN_GROUP = 19;

	public static final int MESSAGE_OPEN_NEXT_MSG_SUCCESS = 20;

	public static final int MESSAGE_OPEN_PREVIEW = 21;

	private static final int MSG_IMPORT_DONE = 22;

// H.M.Wang 2024-12-28 增加两个消息，一个是显示计数器当前值，另外一个是计数器到了边界值报警
	private static final int MSG_SHOW_COUNTER = 25;
	private static final int MSG_ALARM_CNT_EDGE = 26;
// End of H.M.Wang 2024-12-28 增加两个消息，一个是显示计数器当前值，另外一个是计数器到了边界值报警

	/**
	 * the bitmap for preview
	 */
	private Bitmap mPreBitmap;
// H.M.Wang 2021-7-26 追加实际打印内容预览图显示功能
	private Bitmap mPrePrintBitmap;
// End of H.M.Wang 2021-7-26 追加实际打印内容预览图显示功能
	/**
	 * 
	 */
	public int[]	mPreBytes;
	
	/**
	 * background buffer
	 *   used for save the background bin buffer
	 *   fill the variable buffer into this background buffer so we get printing buffer
	 */
	public byte[] mBgBuffer;
	/**
	 *printing buffer
	 *	you should use this buffer for print
	 */
	public byte[] mPrintBuffer;
	
	/**
	 * 褰撳墠鎵撳嵃浠诲姟
	 */
	public List<MessageTask> mMsgTask = new ArrayList<MessageTask>();
	/**
	 * preview buffer
	 * 	you should use this buffer for preview
	 */
	public byte[] mPreviewBuffer;
	
	private boolean mFeatureCorrect = false;
	//Socket___________________________________________________________________________________________
		private Network Net;//checking net;
		private String hostip,aimip;// ip addr
		private Handler myHandler=null;//rec infor prpcess handle
		private String Commands="";// command word;
		private static final int PORT =3550; // port number;
		private volatile ServerSocket server=null; //socket service object
		private ExecutorService mExecutorService = null; //hnadle ExecutorService
		private List<Socket> mList = new ArrayList<Socket>(); //socket list
		private Map<Socket, Service> mServices = new HashMap<Socket, Service>();
		private volatile boolean flag= true;// status flag
		public String PrnComd="";//printing word
		private Printer_Database Querydb;// database class
		private Paths_Create Paths=new Paths_Create();//get and creat path class
		private String AddPaths;//create paths
		private String Scounts;//add counter
		private Stack<String> StrInfo_Stack  = new Stack<String>();// str stack infor
		private PackageInfo pi; //system infor pack
		private StringBuffer sb = new StringBuffer(); //str area word
		private HashMap<String, String> map = new HashMap<String, String>();//map area word
		private int PrinterFlag=0;
		private int SendFileFlag=0;
		private int CleanFlag=0;
//		private int StopFlag=0;
		private Socket Gsocket;

// H.M.Wang 2021-10-30 更新网络命令实现机制
	private PCCommandManager mPCCommandManager = null;
// End of H.M.Wang 2021-10-30 更新网络命令实现机制

// H.M.Wang 2020-9-28 追加一个心跳协议
		Timer mHeartBeatTimer = null;
		public long mLastHeartBeat = System.currentTimeMillis();
// End of H.M.Wang 2020-9-28 追加一个心跳协议
// H.M.Wang 2022-2-13 将PI11状态的读取，并且根据读取的值进行控制的功能扩展为对IN管脚的读取，并且做相应的控制
// H.M.Wang 2021-9-19 追加PI11状态读取功能
//        private Timer mGpio11Timer = null;
//		private int mPI11State = 0;
// End of H.M.Wang 2021-9-19 追加PI11状态读取功能
	private PI11Monitor mPI11Monitor = null;
// End of H.M.Wang 2022-2-13 将PI11状态的读取，并且根据读取的值进行控制的功能扩展为对IN管脚的读取，并且做相应的控制
		//Socket___________________________________________________________________________________________

// H.M.Wang 2023-3-11 追加网络通讯前置缓冲区功能
	private PC_FIFO mPC_FIFO = null;
// End of H.M.Wang 2023-3-11 追加网络通讯前置缓冲区功能

// H.M.Wang 2023-6-27 增加一个用户定义界面模式，增加该界面当中的特殊变量
	private static final int PRINT_TYPE_NORMAL = 0;
	private static final int PRINT_TYPE_UPWARD_CNT = 1;
	private static final int PRINT_TYPE_DWWARD_CNT = 2;
	private int mPrintType = PRINT_TYPE_NORMAL;

	private TextView mUpCntPrint;
	private TextView mDnCntPrint;

	public TextView mTVCntUpPrinting;
	public TextView mTVCntDownPrinting;
	public RelativeLayout	mBtnImport;
	public TextView		  mTvImport;
// End of H.M.Wang 2023-6-27 增加一个用户定义界面模式，增加该界面当中的特殊变量

// H.M.Wang 2024-5-28 增加气压显示控件
	private LinearLayout mPresArea;
	private TextView mPresValue;
// End of H.M.Wang 2024-5-28 增加气压显示控件

	public ControlTabActivity() {
		//mMsgTitle = (ExtendMessageTitleFragment)fragment;
		mCounter = 0;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		//requestWindowFeature(Window.FEATURE_NO_TITLE);
		if(SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_USER_MODE) == SystemConfigFile.USER_MODE_2) {
			return inflater.inflate(R.layout.control2_frame, container, false);
		}
// H.M.Wang 2023-2-12 增加一个工作模式，使用外接U盘当中的文件作为DT的数据源来打印。后续使用哪个方法
		if(TxtDT.getInstance(getActivity()).isTxtDT()) {
			return inflater.inflate(R.layout.control3_frame, container, false);
		}
// End of H.M.Wang 2023-2-12 增加一个工作模式，使用外接U盘当中的文件作为DT的数据源来打印

// H.M.Wang 2023-6-26 增加一个用户定义界面模式
		if(Configs.UI_TYPE == Configs.UI_CUSTOMIZED0) {
			return inflater.inflate(R.layout.control4_frame, container, false);
		}
// End of H.M.Wang 2023-6-26 增加一个用户定义界面模式
		return inflater.inflate(R.layout.control_frame, container, false);
	}
	private RemoteMsgPrompt mScanPromptDlg;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {	
		super.onActivityCreated(savedInstanceState);
		mIndex=0;
//		mTlkList = new Vector<Vector<TlkObject>>();
//		mBinBuffer = new HashMap<Vector<TlkObject>, byte[]>();
		mObjList = new ArrayList<BaseObject>();
		mContext = this.getActivity();
		mSysconfig = SystemConfigFile.getInstance(mContext);
		mDTransThread = DataTransferThread.getInstance(mContext);
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_REOPEN_SERIAL);
		filter.addAction(ACTION_CLOSE_SERIAL);
		filter.addAction(ACTION_BOOT_COMPLETE);
		mReceiver = new SerialEventReceiver(); 
		mContext.registerReceiver(mReceiver, filter);

// H.M.Wang 2023-2-12 增加一个工作模式，使用外接U盘当中的文件作为DT的数据源来打印。后续使用哪个方法
		if(TxtDT.getInstance(mContext).isTxtDT()) {
			TxtDT.getInstance(mContext).initView(getView());
		}
// End of H.M.Wang 2023-2-12 增加一个工作模式，使用外接U盘当中的文件作为DT的数据源来打印

// H.M.Wang 2023-6-14 追加一个监视SC初始化出现失败状态的功能，监视信息包括：初始化失败次数，致命失败次数，写锁值失败次数，致命写锁值失败次数
		mSCMonitorInfo = (TextView) getView().findViewById(R.id.sc_monitor_info);
// End of H.M.Wang 2023-6-14 追加一个监视SC初始化出现失败状态的功能，监视信息包括：初始化失败次数，致命失败次数，写锁值失败次数，致命写锁值失败次数

		mMsgFile = (TextView) getView().findViewById(R.id.opened_msg_name);
		tvMsg = (TextView) getView().findViewById(R.id.tv_msg_name);

// H.M.Wang 2020-1-7 追加群组打印时，显示正在打印的MSG的序号
        mGroupIndex = (TextView) getView().findViewById(R.id.group_index);
// End of H.M.Wang 2020-1-7 追加群组打印时，显示正在打印的MSG的序号

//		mPreview = (PreviewScrollView ) getView().findViewById(R.id.sv_preview);

// H.M.Wang 2023-6-27 增加一个用户定义界面模式，增加该界面当中的特殊变量
		if(Configs.UI_TYPE == Configs.UI_CUSTOMIZED0) {
			mUpCntPrint = (TextView) getView().findViewById(R.id.upward_cnt_print);
			mUpCntPrint.setOnClickListener(this);
			mUpCntPrint.setOnTouchListener(this);
			mDnCntPrint = (TextView) getView().findViewById(R.id.downward_cnt_print);
			mDnCntPrint.setOnClickListener(this);
			mDnCntPrint.setOnTouchListener(this);
		}
// End of H.M.Wang 2023-6-27 增加一个用户定义界面模式，增加该界面当中的特殊变量

		mBtnStart = (RelativeLayout) getView().findViewById(R.id.StartPrint);
		mBtnStart.setOnClickListener(this);
		mBtnStart.setOnTouchListener(this);
		mTvStart = (TextView) getView().findViewById(R.id.tv_start);
		if(PlatformInfo.DEVICE_SMARTCARD.equals(PlatformInfo.getInkDevice()) &&	Configs.SMARTCARDMANAGER) {
			mTvStart.setBackgroundColor(Color.RED);
		}
		mBleState = (TextView) getView().findViewById(R.id.ble_state);

		mBtnStop = (RelativeLayout) getView().findViewById(R.id.StopPrint);
		mBtnStop.setOnClickListener(this);
		mBtnStop.setOnTouchListener(this);
		mTvStop = (TextView) getView().findViewById(R.id.tv_stop);
		//mRecords = (TextView) getView().findViewById(R.id.tv_records);
		/*
		 *clean the print head
		 *this command unsupported now 
		 */
		
		mBtnClean = (RelativeLayout) getView().findViewById(R.id.btnFlush);
		mBtnClean.setOnClickListener(this);
		mBtnClean.setOnTouchListener(this);
		mTvClean = (TextView) getView().findViewById(R.id.tv_flush);
				
		mBtnOpenfile = (RelativeLayout) getView().findViewById(R.id.btnBinfile);
		mBtnOpenfile.setOnClickListener(this);
		mBtnOpenfile.setOnTouchListener(this);
		mTvOpen = (TextView) getView().findViewById(R.id.tv_binfile);

		if(mSysconfig.getParam(SystemConfigFile.INDEX_USER_MODE) == SystemConfigFile.USER_MODE_2) {
			mImportBtn = (TextView) getView().findViewById(R.id.tv_import);
			mImportBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					final ArrayList<String> usbs = ConfigPath.getMountedUsb();
					if (usbs.size() <= 0) {
						ToastUtil.show(mContext, R.string.toast_plug_usb);
						return;
					}
					mProgressDialog = LoadingDialog.show(mContext, R.string.strCopying);

					new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								FileUtil.copyDirectiory(usbs.get(0) + Configs.SYSTEM_CONFIG_MSG_PATH, Configs.TLK_PATH_FLASH);
							} catch(IOException e) {
								ToastUtil.show(mContext, R.string.toast_plug_usb);
							} finally {
								mHandler.sendEmptyMessage(MSG_IMPORT_DONE);
							}
						}
					}).start();

				}
			});

			mEditArea = (LinearLayout) getView().findViewById(R.id.edit_area);
			mCntET = (EditText) getView().findViewById(R.id.text_item);

			mOKBtn = (TextView) getView().findViewById(R.id.btn_ok);
			mOKBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					mEditObject.setContent(mCntET.getText().toString(), true);
					mEditTask.save(new MessageTask.SaveProgressListener() {
						@Override
						public void onSaved() {
							Message msg = mHandler.obtainMessage(MESSAGE_OPEN_PREVIEW);
							Bundle bundle = new Bundle();
							bundle.putString("file", mObjPath);
							msg.setData(bundle);
							mHandler.sendMessage(msg);
						}
					});
				}
			});
		}

// H.M.Wang 2023-6-27 增加一个用户定义界面模式，增加该界面当中的特殊变量
		if(Configs.UI_TYPE == Configs.UI_CUSTOMIZED0) {
			mBtnImport = (RelativeLayout) getView().findViewById(R.id.btnTransfer);
			mBtnImport.setOnClickListener(this);
			mBtnImport.setOnTouchListener(this);
			mTvImport = (TextView) getView().findViewById(R.id.tv_Transfer);
		}
// End of H.M.Wang 2023-6-27 增加一个用户定义界面模式，增加该界面当中的特殊变量

		mMsgPrev = (ImageButton) getView().findViewById(R.id.ctrl_btn_up);
		mMsgNext = (ImageButton) getView().findViewById(R.id.ctrl_btn_down);
		mMsgPrev.setOnClickListener(this);
		mMsgNext.setOnClickListener(this);

		setupViews();

// H.M.Wang 2023-6-27 增加一个用户定义界面模式，增加该界面当中的特殊变量
		if(Configs.UI_TYPE == Configs.UI_CUSTOMIZED0) {
			mTVCntUpPrinting = (TextView) getView().findViewById(R.id.tv_cntPrintUp);
			mTVCntDownPrinting = (TextView) getView().findViewById(R.id.tv_cntPrintDown);
		}
// End of H.M.Wang 2023-6-27 增加一个用户定义界面模式，增加该界面当中的特殊变量

		mTVPrinting = (TextView) getView().findViewById(R.id.tv_printState);
		mTVStopped = (TextView) getView().findViewById(R.id.tv_stopState);

// H.M.Wang 2020-8-11 将原来显示在画面头部的墨量和减锁信息移至ControlTab
		mCtrlTitle = (TextView) getView().findViewById(R.id.ctrl_counter_view);
// H.M.Wang 2023-1-17 修改主页面的显示逻辑，取消原来的锁值显示，将原来的锁值和剩余打印次数合并，显示在画面的左下角，并且同时显示最多6个头的锁值和剩余次数
//		mCountdown = (TextView) getView().findViewById(R.id.count_down);
// End of H.M.Wang 2023-1-17 修改主页面的显示逻辑，取消原来的锁值显示，将原来的锁值和剩余打印次数合并，显示在画面的左下角，并且同时显示最多6个头的锁值和剩余次数
// End of H.M.Wang 2020-8-11 将原来显示在画面头部的墨量和减锁信息移至ControlTab

		switchState(STATE_STOPPED);
		mScrollView = (HorizontalScrollView) getView().findViewById(R.id.preview_scroll);
// H.M.Wang 2023-2-13 增加一个工作模式，使用外接U盘当中的文件作为DT的数据源来打印。后续使用哪个方法
		if(TxtDT.getInstance(mContext).isTxtDT()) {
			mScrollView.setVisibility(View.GONE);
		}
// End of H.M.Wang 2023-2-13 增加一个工作模式，使用外接U盘当中的文件作为DT的数据源来打印
		mllPreview = (LinearLayout) getView().findViewById(R.id.ll_preview);
// H.M.Wang 2021-7-26 追加实际打印内容预览图显示功能
		mllPreview.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Debug.d(TAG, "Preview Clicked");
				if(null == mDTransThread || !mDTransThread.isRunning()) return;
				DataTask dt = mDTransThread.getCurData();
				if(null == mPrePrintBitmap) {
                    Debug.d(TAG, "Enter Realtime Preview");
					mPrePrintBitmap = dt.getPreview();
					dispPreview(mPrePrintBitmap);
				} else if(null != mPrePrintBitmap && !mPrePrintBitmap.isRecycled()) {
                    Debug.d(TAG, "Quit Realtime Preview");
					mPrePrintBitmap.recycle();
					mPrePrintBitmap = null;
					if (mObjPath.startsWith(Configs.GROUP_PREFIX)) {   // group messages
						List<String> paths = MessageTask.parseGroup(mObjPath);
						mPreBitmap = BitmapFactory.decodeFile(MessageTask.getPreview(paths.get(mDTransThread.index())));
// H.M.Wang 2022-11-29 补充 2022-10-25修改QuickGroup时的修改遗漏
					} else if (mObjPath.startsWith(Configs.QUICK_GROUP_PREFIX)) {   // quick group messages
						List<String> paths = MessageTask.parseQuickGroup(mObjPath);
						mPreBitmap = BitmapFactory.decodeFile(MessageTask.getPreview(mObjPath + paths.get(mDTransThread.index())));
// End of H.M.Wang 2022-11-29 补充 2022-10-25修改QuickGroup时的修改遗漏
// H.M.Wang 2022-11-29 增加UG的预览图显示功能
					} else if (mObjPath.startsWith(Configs.USER_GROUP_PREFIX) && mUGSubObjs.size() > 0) {   // user group messages
						mPreBitmap = BitmapFactory.decodeFile(MessageTask.getPreview(mObjPath + mUGSubObjs.get(mDTransThread.index())));
// End of H.M.Wang 2022-11-29 增加UG的预览图显示功能
					} else {
						mPreBitmap = BitmapFactory.decodeFile(MessageTask.getPreview(mObjPath));
					}
					dispPreview(mPreBitmap);
				}
			}
		});
// End of H.M.Wang 2021-7-26 追加实际打印内容预览图显示功能

// H.M.Wang 2023-7-6 增加一个用户定义界面模式，长按预览区进入编辑页面，编辑当前任务
		if(Configs.UI_TYPE == Configs.UI_CUSTOMIZED0) {
			mllPreview.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View view) {
					((MainActivity)getActivity()).onPreviewLongClicked(mObjPath);
					return false;
				}
			});
		}
// End of H.M.Wang 2023-7-6 增加一个用户定义界面模式，长按预览区进入编辑页面，编辑当前任务

		// mMsgPreview = (TextView) getView().findViewById(R.id.message_preview);
		// mMsgPreImg = (ImageView) getView().findViewById(R.id.message_prev_img);
		//
//		mPrintState = (TextView) findViewById(R.id.tvprintState);
// H.M.Wang 2023-1-17 修改主页面的显示逻辑，取消原来的锁值显示，将原来的锁值和剩余打印次数合并，显示在画面的左下角，并且同时显示最多6个头的锁值和剩余次数
//		mtvInk = (TextView) getView().findViewById(R.id.tv_inkValue);
//		mInkLevel = (TextView) getView().findViewById(R.id.ink_value);
//		mInkLevel2 = (TextView) getView().findViewById(R.id.ink_value2);
		mInkValuesGroup1 = (LinearLayout) getView().findViewById(R.id.ink_value_group1);
		mInkValuesGroup2 = (LinearLayout) getView().findViewById(R.id.ink_value_group2);

		int heads = mSysconfig.getPNozzle().mHeads * mSysconfig.getHeadFactor();

		if(PlatformInfo.getImgUniqueCode().startsWith("M5")) {
			mInkValues = new TextView[] {	// 先上下后左右
					(TextView) getView().findViewById(R.id.ink_value1),
					(TextView) getView().findViewById(R.id.ink_value2),
					(TextView) getView().findViewById(R.id.ink_value3),
					(TextView) getView().findViewById(R.id.ink_value4),
					(TextView) getView().findViewById(R.id.ink_value5),
					(TextView) getView().findViewById(R.id.ink_value6),
			};
			if(heads < 4) {
				mInkValuesGroup2.setVisibility(View.GONE);
			}
			if(heads == 2) {
				mInkValues[0].setTextSize(mInkValues[0].getTextSize() * 1.2f);
				mInkValues[1].setTextSize(mInkValues[1].getTextSize() * 1.2f);
			}
			if(heads <= 2 ) {
				mInkValuesGroup2.setVisibility(View.GONE);
			}
		} else {
			mInkValues = new TextView[] {	// 先左右，后上下
					(TextView) getView().findViewById(R.id.ink_value1),
					(TextView) getView().findViewById(R.id.ink_value4),
					(TextView) getView().findViewById(R.id.ink_value2),
					(TextView) getView().findViewById(R.id.ink_value5),
					(TextView) getView().findViewById(R.id.ink_value3),
					(TextView) getView().findViewById(R.id.ink_value6)
			};
			if(heads <= 1 ) {
				mInkValuesGroup2.setVisibility(View.GONE);
			}
		}

		for(int i=heads; i<6; i++) {
			mInkValues[i].setVisibility(View.GONE);
		}
// End of H.M.Wang 2023-1-17 修改主页面的显示逻辑，取消原来的锁值显示，将原来的锁值和剩余打印次数合并，显示在画面的左下角，并且同时显示最多6个头的锁值和剩余次数

// H.M.Wang 2024-5-28 增加气压显示控件
		try {
			mPresArea = (LinearLayout) getView().findViewById(R.id.pressure_area);
			mPresValue = (TextView) getView().findViewById(R.id.pressure_value);
		} catch(Exception e) {
			Debug.e(TAG, e.getMessage());
		}
// End of H.M.Wang 2024-5-28 增加气压显示控件

		mPowerStat = (ImageView) getView().findViewById(R.id.power_value);
// H.M.Wang 2022-11-15 取消2022-11-5的修改，即对mPower的启用和根据img的类型决定是否显示(电池图标，电压和脉宽全部受控)，而改为如果是M5/M7/M9/BAGINK/22MM/Smartcard的img时，不显示电池图标，电压和脉宽继续显示
// H.M.Wang 2023-10-13 由于在refreshPower函数中做了调整，所以取消这里的设置
//		String imgUC = PlatformInfo.getImgUniqueCode();
//		if(PlatformInfo.isMImgType(imgUC) || imgUC.startsWith("O7GS")) {
//			mPowerStat.setVisibility(View.INVISIBLE);
//		}
// End of H.M.Wang 2023-10-13 由于在refreshPower函数中做了调整，所以取消这里的设置
// End of H.M.Wang 2022-11-15 取消2022-11-5的修改，即对mPower的启用和根据img的类型决定是否显示(电池图标，电压和脉宽全部受控)，而改为如果是M5/M7/M9/BAGINK/22MM/Smartcard的img时，不显示电池图标，电压和脉宽继续显示
//		mPower = (RelativeLayout) getView().findViewById(R.id.power);
		mPowerV = (TextView) getView().findViewById(R.id.powerV);
		mTime = (TextView) getView().findViewById(R.id.time);
// H.M.Wang 2024-7-10 追加错误信息返回主控制页面显示的功能
		if(PlatformInfo.getImgUniqueCode().startsWith("22MM")) {
			mHp22mmErrTV = (TextView) getView().findViewById(R.id.tv_hp22mm_result);
		}
// End of H.M.Wang 2024-7-10 追加错误信息返回主控制页面显示的功能
// H.M.Wang 2024-9-21 追加一个显示FPGA驱动状态的功能，当前只显示跳空次数
		mDriverState = (TextView) getView().findViewById(R.id.tv_driver_state);
// End of H.M.Wang 2024-9-21 追加一个显示FPGA驱动状态的功能，当前只显示跳空次数
// H.M.Wang 2023-9-20 追加一个步长细分数值显示的功能
		if(SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_USER_MODE) == SystemConfigFile.USER_MODE_NONE && Configs.UI_TYPE == Configs.UI_STANDARD) {
			mSubStepTV = (TextView) getView().findViewById(R.id.sub_step_tv);
			mSubStepTV.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					SubStepDialog ssDialog = new SubStepDialog(mContext);
					ssDialog.show();
				}
			});
		}
// End of H.M.Wang 2023-9-20 追加一个步长细分数值显示的功能

		refreshPower();
		//  鍔犺浇鎵撳嵃璁℃暟
		PrinterDBHelper db = PrinterDBHelper.getInstance(mContext);
		//mCounter = db.getCount(mContext);
		RTCDevice rtcDevice = RTCDevice.getInstance(mContext);

// H.M.Wang 2024-11-5 增加A133平台的判断
//		if (PlatformInfo.PRODUCT_SMFY_SUPER3.equalsIgnoreCase(PlatformInfo.getProduct())) {
		if (PlatformInfo.isSmfyProduct() || PlatformInfo.isA133Product()) {
// End of H.M.Wang 2024-11-5 增加A133平台的判断
			rtcDevice.initSystemTime(mContext);
			mCounter = rtcDevice.readCounter(mContext);
			if (mCounter == 0) {
				rtcDevice.writeCounter(mContext, 0);
				db.setFirstBoot(mContext, false);
			}
		}
		/* 濡傛灉瑷疆鍙冩暩32鐖瞣n锛岃▓鏁稿櫒閲嶇疆 */
		if (mSysconfig.getParam(SystemConfigFile.INDEX_COUNTER_RESET) == 1) {
			mCounter = 0;
		}
		/***PG1 PG2杈撳嚭鐘舵�佷负 0x11锛屾竻闆舵ā寮�**/
		FpgaGpioOperation.clean();
// H.M.Wang 2022-3-21 由于实现方法做了修改，有apk获取相应管脚的状态后，设置是否对打印缓冲区进行反向操作，因此这里无需再通知驱动参数57的状态
// H.M.Wang 2022-3-1 开机后将参数57下发给FPGA驱动
//		FpgaGpioOperation.setInputProc(mSysconfig.getParam(SystemConfigFile.INDEX_IPURT_PROC));
// End of H.M.Wang 2022-3-1 开机后将参数57下发给FPGA驱动
// End of H.M.Wang 2022-3-21 由于实现方法做了修改，有apk获取相应管脚的状态后，设置是否对打印缓冲区进行反向操作，因此这里无需再通知驱动参数57的状态

// H.M.Wang 2021-4-9 追加ioctl的分辨率信息获取命令
		Configs.GetSystemDpiVersion();
		Debug.d(TAG, "SystemDpiVersion: " + Configs.GetDpiVersion());
// End of H.M.Wang 2021-4-9 追加ioctl的分辨率信息获取命令

		//Debug.d(TAG, "===>loadMessage");
		// 閫氳繃鐩戝惉绯荤粺骞挎挱鍔犺浇
		SharedPreferences p = mContext.getSharedPreferences(SettingsTabActivity.PREFERENCE_NAME, Context.MODE_PRIVATE);
		boolean loading = p.getBoolean(PreferenceConstants.LOADING_BEFORE_CRASH, false);
		/**
		 * if crash happened when load the last message, don`t load it again
		 * avoid endless loop of crash
		 */
		Debug.d(TAG, "--->loading: " + loading);
// H.M.Wang 2021-3-2 移到这里，可能崩溃后不显示预览原因在这里
        loadMessage();
// End of H.M.Wang 2021-3-2 移到这里，可能崩溃后不显示预览原因在这里

		if (!loading) {
//            loadMessage();
			new Thread(new Runnable() {
				@Override
				public void run() {
                    if(SystemConfigFile.getInstance().getParam(41) == 1) {
// H.M.Wang 2021-3-2 追加等待初始化成功事件
                        while(!mRfidInit) {
                            try {Thread.sleep(1000);} catch(InterruptedException e) {};
                        }
// End of H.M.Wang 2021-3-2 追加等待初始化成功事件
//                        Toast.makeText(mContext, "Launching Print...", Toast.LENGTH_SHORT).show();
                        mHandler.sendEmptyMessageDelayed(MESSAGE_OPEN_TLKFILE, 1000);
                    }
				}
			}).start();

		}

		FpgaGpioOperation.updateSettings(mContext, null, FpgaGpioOperation.SETTING_TYPE_NORMAL);
// H.M.Wang 2022-10-24 如果不是盖章机，则停在停止状态。目的是尝试防止待机后就去清洗的话，有不启动清洗的问题
		if(!PlatformInfo.getImgUniqueCode().startsWith("GZJ")) {
			FpgaGpioOperation.uninit();
		}
// End of H.M.Wang 2022-10-24 如果不是盖章机，则停在停止状态
		/****鍒濆鍖朢FID****/
		mInkManager = InkManagerFactory.inkManager(mContext);
		mHandler.sendEmptyMessageDelayed(RFIDManager.MSG_RFID_INIT, 1000);

// H.M.Wang 2021-10-30 更新网络命令实现机制
//// 暂时关闭		SocketBegin();// Beging Socket service start;
		mPCCommandManager = new PCCommandManager(mContext, this);
// End of H.M.Wang 2021-10-30 更新网络命令实现机制
		refreshCount();

		Querydb=new Printer_Database(mContext);

		// H.M.Wang 2019-12-19 修改支持多种协议
		// H.M.Wang 2019-10-26 追加串口命令处理部分
		final SerialHandler sHandler = SerialHandler.getInstance(mContext);
		sHandler.setNormalCommandListener(new SerialHandler.OnSerialPortCommandListenner() {
			@Override
			public void onError(final String errCode) {
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						ToastUtil.show(mContext, errCode);
					}
				});
			}
			@Override
			public void onCommandReceived(int cmd, byte[] data) {
				if( SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_RS232_1 ||
					SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_RS232_2 ||
// H.M.Wang 2022-5-16 追加串口协议2无线
					SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_RS232_2_WIFI ||
// End of H.M.Wang 2022-5-16 追加串口协议2无线
// H.M.Wang 2020-7-17 追加串口7协议
					SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_RS232_7) {
// End of H.M.Wang 2020-7-17 追加串口7协议
					Debug.d(TAG, "CMD = " + Integer.toHexString(cmd) + "; DATA = [" + ByteArrayUtils.toHexString(data) + "]");
					switch (cmd) {
						case EC_DOD_Protocol.CMD_CHECK_CHANNEL:                // 检查信道	0x0001
//							sHandler.sendCommandProcessResult(EC_DOD_Protocol.CMD_CHECK_CHANNEL, 0, 0, 0, "");
							sHandler.sendCommandProcessResult(EC_DOD_Protocol.CMD_CHECK_CHANNEL, 1, 0, 0, "");
							break;
						case EC_DOD_Protocol.CMD_SET_PRINT_DELAY:              // 设定喷头喷印延时	0x0008
							if (data.length != 3) {
								sHandler.sendCommandProcessResult(EC_DOD_Protocol.CMD_SET_PRINT_DELAY, 1, 0, 1, "");
							} else {
								SystemConfigFile.getInstance().setParamBroadcast(3, (0x00ff & data[2]) * 0x0100 + (0x00ff & data[1]));
// H.M.Wang 2019-12-9 串口设置参数实时保存
								SystemConfigFile.getInstance().saveConfig();
// End. ---------
//							sHandler.sendCommandProcessResult(EC_DOD_Protocol.CMD_SET_PRINT_DELAY, 0, 0, 0, "");
								sHandler.sendCommandProcessResult(EC_DOD_Protocol.CMD_SET_PRINT_DELAY, 1, 0, 0, "");
							}
							break;
						case EC_DOD_Protocol.CMD_SET_MOVE_SPEED:               // 设定物体移动速度	0x000a
							if (data.length != 3) {
								sHandler.sendCommandProcessResult(EC_DOD_Protocol.CMD_SET_MOVE_SPEED, 1, 0, 1, "");
							} else {
// H.M.Wang 2019-12-29 取消3.7系数
								SystemConfigFile.getInstance().setParamBroadcast(0, Math.round((0x00ff & data[2]) * 0x0100 + (0x00ff & data[1])));
// H.M.Wang 2019-12-9 串口设置参数实时保存
								SystemConfigFile.getInstance().saveConfig();
// End. ---------
//							sHandler.sendCommandProcessResult(EC_DOD_Protocol.CMD_SET_MOVE_SPEED, 0, 0, 0, "Set Speed Done!");
							sHandler.sendCommandProcessResult(EC_DOD_Protocol.CMD_SET_MOVE_SPEED, 1, 0, 0, "");
							}
							break;
						case EC_DOD_Protocol.CMD_SET_REVERSE:                  // 设定喷头翻转喷印	0x0010
							SystemConfigFile.getInstance().setParamBroadcast(1, (0x01 & data[1]));
// H.M.Wang 2019-12-9 串口设置参数实时保存
							SystemConfigFile.getInstance().saveConfig();
// End. ---------
//							sHandler.sendCommandProcessResult(EC_DOD_Protocol.CMD_SET_REVERSE, 0, 0, 0, "Set Reverse Done!");
							sHandler.sendCommandProcessResult(EC_DOD_Protocol.CMD_SET_REVERSE, 1, 0, 0, "");
							break;
						case EC_DOD_Protocol.CMD_START_PRINT:                  // 启动喷码机开始喷印	0x0015
							mHandler.sendEmptyMessage(MESSAGE_OPEN_TLKFILE);
//							sHandler.sendCommandProcessResult(EC_DOD_Protocol.CMD_START_PRINT, 0, 0, 0, "Launch Print Done!");
							sHandler.sendCommandProcessResult(EC_DOD_Protocol.CMD_START_PRINT, 1, 0, 0, "");
							break;
						case EC_DOD_Protocol.CMD_STOP_PRINT:                   // 停机命令	0x0016
							mHandler.sendEmptyMessage(MESSAGE_PRINT_STOP);
//							sHandler.sendCommandProcessResult(EC_DOD_Protocol.CMD_STOP_PRINT, 0, 0, 0, "Stop Print Done!");
							sHandler.sendCommandProcessResult(EC_DOD_Protocol.CMD_STOP_PRINT, 1, 0, 0, "");
							break;
// H.M.Wang 2021-1-26 将17命令作为清洗命令
						case EC_DOD_Protocol.CMD_START_PRINT_A:
							DataTransferThread thread = DataTransferThread.getInstance(mContext);
							thread.purge(mContext);
							sHandler.sendCommandProcessResult(EC_DOD_Protocol.CMD_START_PRINT_A, 1, 0, 0, "");
							break;
// End of H.M.Wang 2021-1-26 将17命令作为清洗命令
// H.M.Wang 2022-5-16 追加启动打印特定编号信息的命令
						case EC_DOD_Protocol.CMD_START_PRINT_X:                // 启动打印特定编号信息	0x0021
							if (data.length != 3) {
								sHandler.sendCommandProcessResult(EC_DOD_Protocol.CMD_START_PRINT_X, 1, 0, 1, "");
							}

							String fileName = "" + ((0x00ff & data[2]) * 0x0100 + (0x00ff & data[1]));

							Debug.d(TAG, "RS232_2 select file: " + fileName);
							if(new File(ConfigPath.getTlkDir(fileName)).exists()) {
								Message msg = mHandler.obtainMessage(MESSAGE_OPEN_PREVIEW);
								Bundle bundle = new Bundle();
								bundle.putString("file", fileName);
								if (mDTransThread != null && mDTransThread.isRunning()) {
									bundle.putBoolean("printNext", true);
								}
								msg.setData(bundle);
								mHandler.sendMessage(msg);
								sHandler.sendCommandProcessResult(EC_DOD_Protocol.CMD_START_PRINT_X, 1, 0, 0, "");
							} else {
								sHandler.sendCommandProcessResult(EC_DOD_Protocol.CMD_START_PRINT_X, 1, 0, 1, "");
							}
// End of H.M.Wang 2022-5-16 追加启动打印特定编号信息的命令
					}
				}
			}
		});

// H.M.Wang 2025-5-28 新增加扫描协议8
		if (SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_SCANER9) {
			mScanPromptDlg = new RemoteMsgPrompt(mContext);
			mScanPromptDlg.setOnKeyListener(new DialogInterface.OnKeyListener() {
				@Override
				public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
					if(mScanPromptDlg.isShowing()) mScanPromptDlg.hide();
					if(event.getAction() == KeyEvent.ACTION_DOWN) {
						if(keyCode == KeyEvent.KEYCODE_ENTER) {
							return true;
						} else {
							Debug.d(TAG, "----");
							BarcodeScanParser.append(keyCode, event.isShiftPressed());
						}
					}
					return false;
				}
			});
			BarcodeScanParser.setListener(new BarcodeScanParser.OnScanCodeListener() {
				@Override
				public void onCodeReceived(final String code) {
					Debug.d(TAG, "String from Remote = [" + code + "]");

					mHandler.post(new Runnable() {
						@Override
						public void run() {
							mScanPromptDlg.show();
							mScanPromptDlg.setMessage(code);
						}
					});

					String[] recvStrs = code.split("#");
					if(recvStrs.length < 2) return;
					try {
						int dataNo = Integer.parseInt(recvStrs[0]);
						String data = "";
						if(dataNo >= 10 && dataNo <=39) {
							data = "" + dataNo;
						} else if(dataNo <=99) {
							data = Configs.GROUP_PREFIX + dataNo;
						} else return;

						if(null != mDTransThread && mDTransThread.isRunning()) {
							mHandler.sendEmptyMessage(MESSAGE_PRINT_STOP);
							while(mDTransThread.isRunning()) {
								Thread.sleep(10);
							}
						} else if (mDTransThread.isPurging) {
							ToastUtil.show(mContext, R.string.str_under_purging);
							return;
						}

						for(int i=1; i<Math.min(recvStrs.length, 4); i++) {
							SystemConfigFile.getInstance().setDTBuffer(i-1, recvStrs[i]);
						}

						Message msg = mHandler.obtainMessage(MESSAGE_OPEN_PREVIEW);
						Bundle bundle = new Bundle();
						bundle.putString("file", data);
						msg.setData(bundle);

						mHandler.sendMessageDelayed(msg, 1000);

						mHandler.sendEmptyMessageDelayed(MESSAGE_OPEN_TLKFILE, 1000);
						if(PlatformInfo.isA133Product()) SystemFs.writeSysfs("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor", "performance");
					} catch(NumberFormatException e) {
						Debug.e(TAG, e.getMessage());
					} catch(Exception e) {
						Debug.e(TAG, e.getMessage());
					}
				}
			});
		}
// End of H.M.Wang 2025-5-28 新增加扫描协议8

// H.M.Wang 2021-3-1 移到延时线程里面
//		if(SystemConfigFile.getInstance().getParam(41) == 1) {
//			Toast.makeText(mContext, "Launching Print...", Toast.LENGTH_SHORT).show();
//			mHandler.sendEmptyMessageDelayed(MESSAGE_OPEN_TLKFILE, 1000);
//		}
// End of H.M.Wang 2021-3-1 移到延时线程里面

// H.M.Wang 2020-9-28 追加一个心跳协议
		mLastHeartBeat = System.currentTimeMillis();
		mHeartBeatTimer = new Timer();
		mHeartBeatTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if(mSysconfig.getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_LAN_HEART) {
					if(System.currentTimeMillis() - mLastHeartBeat > 2000L) {
						mBtnStart.post(new Runnable() {
							@Override
							public void run() {
								ToastUtil.show(mContext, "No Lan Heart Beat!!!");
							}
						});
						try{
							ExtGpio.playClick();
							Thread.sleep(50);
							ExtGpio.playClick();
							Thread.sleep(50);
							ExtGpio.playClick();
						} catch (Exception e) {
							Debug.e(TAG, e.getMessage());
						}
					}
				}
// H.M.Wang 2023-2-19 借用这个常驻线程，完成10个DT桶向闪存的写入更新
				mSysconfig.writePrefs();
// End of H.M.Wang 2023-2-19 借用这个常驻线程，完成10个DT桶向闪存的写入更新
///// 2024-10-14 测试目的，已取消				if(null != mDTransThread && mDTransThread.isRunning()) mDTransThread.setRemoteTextSeparated("ABCEDFGHIJKLMN");
			}
		}, 0L, 1000L);

// End of H.M.Wang 2020-9-28 追加一个心跳协议

// H.M.Wang 2022-3-21 根据工作中心对输入管脚协议的重新定义，大幅度修改相应的处理方法
// H.M.Wang 2022-2-13 将PI11状态的读取，并且根据读取的值进行控制的功能扩展为对IN管脚的读取，并且做相应的控制
// H.M.Wang 2021-9-19 追加PI11状态读取功能
		if(null == mPI11Monitor) {
			mPI11Monitor = new PI11Monitor(mContext, new PI11Monitor.PI11MonitorFunc() {
				@Override
				public void onStartPrint() {
					mBtnStart.post(new Runnable() {
						@Override
						public void run() {
// H.M.Wang 2022-12-15 因此修改为如果已经有实例则根据原来的逻辑处理，如果没有，则模拟开始按键按下
							if(null == mDTransThread) {
								mBtnStart.performClick();
							} else {
// End of H.M.Wang 2022-12-15 因此修改为如果已经有实例则根据原来的逻辑处理，如果没有，则模拟开始按键按下
								if (mDTransThread.isPurging) {
									ToastUtil.show(mContext, R.string.str_under_purging);
//												return;
								} else {
									if (!mBtnStart.isClickable()) {
//									ToastUtil.show(mContext, "Not executable");
//												return;
									} else if (mDTransThread.isRunning()) {
//									ToastUtil.show(mContext, "Already in printing");
//												return;
									} else {
////                                                mInPinState |= 0x01;
//								Debug.d(TAG, "Launch Print by pressing PI11!");
										mBtnStart.performClick();
									}
								}
							}
						}
					});
				}

				@Override
				public void onStopPrint() {
					mBtnStart.post(new Runnable() {
						@Override
						public void run() {
							if(null != mDTransThread) {
								if (mDTransThread.isPurging) {
									ToastUtil.show(mContext, R.string.str_under_purging);
//												return;
								} else {
									if (!mBtnStop.isClickable()) {
//									ToastUtil.show(mContext, "Not executable");
//												return;
									}
									if (!mDTransThread.isRunning()) {
//									ToastUtil.show(mContext, "Not in printing");
//												return;
									}
//								Debug.d(TAG, "Stop Print by releasing PI11!");
									mBtnStop.performClick();
								}
							}
						}
					});
				}

				@Override
				public void onMirror() {

				}

				@Override
				public void onResetCounter() {

				}

				@Override
				public void onPrintFile(int index) {
					if(index != 0x00) {
						String fileName = "" + index;
						Debug.d(TAG, "IN8-IN5 select file: " + fileName);
						if(new File(ConfigPath.getTlkDir(fileName)).exists()) {
							Message msg = mHandler.obtainMessage(MESSAGE_OPEN_PREVIEW);
							Bundle bundle = new Bundle();
							bundle.putString("file", fileName);
// H.M.Wang 2022-3-1 正在打印中的时候切换打印信息，重新生成打印缓冲区
							if (mDTransThread != null && mDTransThread.isRunning()) {
								bundle.putBoolean("printNext", true);
							}
// End of H.M.Wang 2022-3-1 正在打印中的时候切换打印信息，重新生成打印缓冲区
							msg.setData(bundle);
							mHandler.sendMessage(msg);
						}
					}
				}

				@Override
				public void onLevelLow() {
					ExtGpio.writeGpio('h', 7, 1);
					mBtnStart.post(new Runnable() {
						@Override
						public void run() {
							ToastUtil.show(mContext, R.string.strLevelLow);
						}
					});
					ThreadPoolManager.mControlThread.execute(new Runnable() {
						@Override
						public void run() {
							if(!mIsAlarming) {
								mIsAlarming = true;
								ExtGpio.playClick();
								try{Thread.sleep(50);}catch(Exception e){};
								ExtGpio.playClick();
								try{Thread.sleep(50);}catch(Exception e){};
								ExtGpio.playClick();
								mIsAlarming = false;
							}
						}
					});
				}

				@Override
				public void onSolventLow() {
					ExtGpio.writeGpio('h', 7, 1);
					mBtnStart.post(new Runnable() {
						@Override
						public void run() {
							ToastUtil.show(mContext, R.string.strSolventLow);
						}
					});
					ThreadPoolManager.mControlThread.execute(new Runnable() {
						@Override
						public void run() {
							if(!mIsAlarming) {
								mIsAlarming = true;
								ExtGpio.playClick();
								try{Thread.sleep(50);}catch(Exception e){};
								ExtGpio.playClick();
								try{Thread.sleep(50);}catch(Exception e){};
								ExtGpio.playClick();
								mIsAlarming = false;
							}
						}
					});
				}

				@Override
				public void onLevelHigh() {
					ExtGpio.writeGpio('h', 7, 0);
				}

				@Override
				public void onSolventHigh() {
					ExtGpio.writeGpio('h', 7, 0);
				}

// H.M.Wang 2023-10-26 0x80：低有效，常态高。IN-8=0时，喷码机在等待打印状态下清洗一次
				@Override
				public void onLaunchPurge() {
					DataTransferThread thread = DataTransferThread.getInstance(mContext);
					DataTransferThread.SelectPen = 0x3F;			// 选择所有头
// H.M.Wang 2023-12-11 修改为喷码机在等待打印及正在打印的状态下据可以清洗一次
//					if(!thread.isRunning())	thread.purge(mContext);	// 在非打印状态下清洗一次
					thread.purge(mContext);	// 在非打印状态下清洗一次
// End of H.M.Wang 2023-12-11 修改为喷码机在等待打印及正在打印的状态下据可以清洗一次
				}
// End of H.M.Wang 2023-10-26 0x80：低有效，常态高。IN-8=0时，喷码机在等待打印状态下清洗一次
			});
			mPI11Monitor.start(5000L);
		}
// End of H.M.Wang 2021-9-19 追加PI11状态读取功能
// End of H.M.Wang 2022-2-13 将PI11状态的读取，并且根据读取的值进行控制的功能扩展为对IN管脚的读取，并且做相应的控制
// End of H.M.Wang 2022-3-21 根据工作中心对输入管脚协议的重新定义，大幅度修改相应的处理方法
		// End ---------------------------------

// H.M.Wang 2023-3-11 追加网络通讯前置缓冲区功能
		mPC_FIFO = PC_FIFO.getInstance(mContext);
// End of H.M.Wang 2023-3-11 追加网络通讯前置缓冲区功能
	}

	public boolean getLevelLow() {
		if(null != mPI11Monitor) return mPI11Monitor.getLevelLow();
		return false;
	}

	public boolean getSolventLow() {
		if(null != mPI11Monitor) return mPI11Monitor.getSolventLow();
		return false;
	}

	@Override
	public void onResume() {
		super.onResume();
		Debug.e(TAG, "--->onResume");
	}

	private String opendTlks() {
		StringBuilder name = new StringBuilder();
		if (mMsgTask != null) {
			for (MessageTask task : mMsgTask) {
				name.append(task.getName());
				name.append("^");
			}
			if (name.length() > 0) {
				name.deleteCharAt(name.length() - 1);
			}
			return name.toString();
		}
		return null;
	}
	public void onConfigureChanged() {
//	2022-11-08 H.M.Wang	mMsgFile.setText(opendTlks());
		mMsgFile.setText(mObjPath);
		int heads = 1;
		tvMsg.setText(R.string.str_msg_name);
		mTvStart.setText(R.string.str_btn_print);
		mTvStop.setText(R.string.str_btn_stop);
		mTvOpen.setText(R.string.str_openfile);
		mTvClean.setText(R.string.str_btn_clean);
		mTVPrinting.setText(R.string.str_state_printing);
// H.M.Wang 2023-6-27 增加一个用户定义界面模式，增加该界面当中的特殊变量
		if(Configs.UI_TYPE == Configs.UI_CUSTOMIZED0) {
			mTVCntUpPrinting.setText(R.string.str_state_cnt_print_up);
			mTVCntDownPrinting.setText(R.string.str_state_cnt_printdown);
			mTvImport.setText(R.string.tips_import);
			mTvOpen.setText(R.string.str_open_msg);
		}
// End of H.M.Wang 2023-6-27 增加一个用户定义界面模式，增加该界面当中的特殊变量
		mTVStopped.setText(R.string.str_state_stopped);
//		mtvInk.setText(R.string.str_state_inklevel);

		heads = mSysconfig.getPNozzle().mHeads * mSysconfig.getHeadFactor();

		Debug.d(TAG, "--->onConfigChanged: " + heads + "   -- " + RFIDManager.TOTAL_RFID_DEVICES);
		if (heads > RFIDManager.TOTAL_RFID_DEVICES) {
			mInkManager = InkManagerFactory.reInstance(mContext);
			mHandler.sendEmptyMessageDelayed(RFIDManager.MSG_RFID_INIT, 1000);
		}
		onConfigChange();
	}
	
	private void setupViews() {
		if (PlatformInfo.PRODUCT_FRIENDLY_4412.equalsIgnoreCase(PlatformInfo.getProduct())) {
//			mForward.setVisibility(View.GONE);
//			mBackward.setVisibility(View.GONE);
		}
	}

// H.M.Wang 2021-10-30 更新网络命令实现机制
	@Override
	public void onDestroyView() {
		if(null != mPCCommandManager) mPCCommandManager.close();
		super.onDestroyView();
	}
// End of H.M.Wang 2021-10-30 更新网络命令实现机制

	@Override
	public void onDestroy()
	{
		mContext.unregisterReceiver(mReceiver);
		super.onDestroy();

		//UsbSerial.close(mFd);
	}
	
	public void loadMessage() {
		String f = mSysconfig.getLastMsg();
		Debug.d(TAG, "--->load message: " + f );
		if (f == null || f.isEmpty()) {
			Debug.e(TAG, "--->please ensure the file name");
			return;
		}
		File file = new File(ConfigPath.getTlkDir(f));
		Debug.d(TAG, "--->load message: " + file.getAbsolutePath());
		if (!file.exists()) {
			Debug.e(TAG, "--->file not exist!!! quit loading");
			return;
		}
		Message msg = mHandler.obtainMessage(MESSAGE_OPEN_PREVIEW);
		Bundle bundle = new Bundle();
		bundle.putString("file", f);
		msg.setData(bundle);
		mHandler.sendMessageDelayed(msg, 1000);
	}

	/**
	 * load tlk and print it after load success
	 * @param message
	 */
	public void loadAndPrint(String message) {
		Message msg = mHandler.obtainMessage(MESSAGE_OPEN_PREVIEW);
		Bundle bundle = new Bundle();
		bundle.putString("file", message);
		bundle.putBoolean("printAfterLoad", true);
		msg.setData(bundle);
		mHandler.sendMessageDelayed(msg, 1000);
	}

	@Deprecated
	public void reloadSettingAndMessage() {
		mSysconfig.init();
		loadMessage();
	}

// H.M.Wang 2023-1-18 增加一个时间戳，用来判定显示墨水信息得时间间隔，时间间隔太短得话显示也没有意义，只是占用资源
	private long inkDispInterval = 0;
// End of H.M.Wang 2023-1-18 增加一个时间戳，用来判定显示墨水信息得时间间隔，时间间隔太短得话显示也没有意义，只是占用资源

	private void switchRfid() {
// H.M.Wang 2023-1-17 修改主页面的显示逻辑，取消原来的锁值显示，将原来的锁值和剩余打印次数合并，显示在画面的左下角，并且同时显示最多6个头的锁值和剩余次数
/*		mRfid += 1;

		int heads = mSysconfig.getPNozzle().mHeads * mSysconfig.getHeadFactor();
// M.M.Wang 2020-11-16 增加墨盒墨量显示
		if(mInkManager instanceof SmartCardManager) {
			heads = ((SmartCardManager)mInkManager).getInkCount();
		}
// End of M.M.Wang 2020-11-16 增加墨盒墨量显示

		if (mRfid >= RFIDManager.TOTAL_RFID_DEVICES || mRfid >= heads) {
			mRfid = 0;
		}
//		Debug.d(TAG, "--->switchRfid to: " + mRfid);
		Debug.d(TAG, "--- refreshInk ---");
*/
		refreshInk();
		// refreshCount();
		mHandler.sendEmptyMessageDelayed(MESSAGE_SWITCH_RFID, 3000);
	}
// End of H.M.Wang 2023-1-17 修改主页面的显示逻辑，取消原来的锁值显示，将原来的锁值和剩余打印次数合并，显示在画面的左下角，并且同时显示最多6个头的锁值和剩余次数

	boolean mInkLow = false;
	boolean mInkZero = false;

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
	}

// H.M.Wang 2023-1-17 修改主页面的显示逻辑，取消原来的锁值显示，将原来的锁值和剩余打印次数合并，显示在画面的左下角，并且同时显示最多6个头的锁值和剩余次数
	private void refreshInk() {
		Debug.d(TAG,  "[" + PlatformInfo.getImgUniqueCode() + "-" + BuildConfig.VERSION_CODE + "]");
// H.M.Wang 2024-9-21 追加一个显示FPGA驱动状态的功能，当前只显示跳空次数
		int skipNum = (FpgaGpioOperation.getDriverState() & 0x0FF);
		if(skipNum > 0) {
			mDriverState.setText("" + skipNum);
		} else {
			mDriverState.setText("");
		}
// End of H.M.Wang 2024-9-21 追加一个显示FPGA驱动状态的功能，当前只显示跳空次数
// H.M.Wang 2024-7-27 追加蓝牙设备号和蓝牙开关功能
//		if((mInkManager instanceof RFIDManager || mInkManager instanceof N_RFIDManager) && mSysconfig.getParam(SystemConfigFile.INDEX_BLE_ENABLE) == 1 && BLEDevice.getInstance().isInitialized()) {
		if((mInkManager instanceof RFIDManager || mInkManager instanceof N_RFIDManager) && mSysconfig.getParam(SystemConfigFile.INDEX_BLE_ENABLE) == 1 && BluetoothServerManager.getInstance().isInitialized()) {
			mBleState.setVisibility(View.VISIBLE);
		} else {
			mBleState.setVisibility(View.GONE);
		}
// End of H.M.Wang 2024-7-27 追加蓝牙设备号和蓝牙开关功能
// H.M.Wang 2024-5-28 增加气压显示控件，借用refreshInk间隔性显示的机制，显示气压数据
		if(null != mPresArea && null != mPresValue) {
			if(mSysconfig.getParam(SystemConfigFile.INDEX_PRESURE) > 0) {
				mPresArea.setVisibility(View.VISIBLE);
				mPresValue.setText((int)(1.0f * SmartCard.readADS1115(0) / 32767 * (4.0f * 66.7f + 12) - 6.67f) + " " + mContext.getString(R.string.str_unit_kPa));
			} else {
				mPresArea.setVisibility(View.GONE);
			}
		}
// End of H.M.Wang 2024-5-28 增加气压显示控件

		inkDispInterval = System.currentTimeMillis();

		int heads = mSysconfig.getPNozzle().mHeads * mSysconfig.getHeadFactor();
		if(mInkManager instanceof SmartCardManager) {		// 当初始化这些显示控件的时候，还没有初始化SmartCardManager，因此还无法获得正确的heads，因此在这里再根据实际SC的头数调节控件的显示属性
			heads = ((SmartCardManager)mInkManager).getInkCount();
			if(heads >= 2) mInkValuesGroup2.setVisibility(View.VISIBLE);	// 流出显示B的位置
			if(heads == 3) mInkValues[heads].setVisibility(View.INVISIBLE);	// 占个位置，不实际显示
		}
// H.M.Wang 2025-2-19 修改能够显示两个头的寿命锁值功能
        if(mInkManager instanceof Hp22mmSCManager) {		// 当初始化这些显示控件的时候，还没有初始化SmartCardManager，因此还无法获得正确的heads，因此在这里再根据实际SC的头数调节控件的显示属性
            heads = ((Hp22mmSCManager)mInkManager).getInkCount();
            if(heads >= 2) mInkValuesGroup2.setVisibility(View.VISIBLE);	// 流出显示B的位置
            if(heads == 3) mInkValues[heads].setVisibility(View.INVISIBLE);	// 占个位置，不实际显示
        }
// End of H.M.Wang 2025-2-19 修改能够显示两个头的寿命锁值功能

		boolean valid = !(null != mDTransThread && mDTransThread.isRunning());
// H.M.Wang 2024-7-10 当打印头的数量多余6时，由于数据区mInkValues的最大容量为6，所以会越界，出现异常，暂时取消7，8头的信息显示
//		for(int i=0; i<heads; i++) {
		for(int i=0; i<Math.min(heads, 6); i++) {
// End of H.M.Wang 2024-7-10 当打印头的数量多余6时，由于数据区mInkValues的最大容量为6，所以会越界，出现异常，暂时取消7，8头的信息显示
			float count = mInkManager.getLocalInk(i) - 1;
			float ink = mInkManager.getLocalInkPercentage(i);

			String down = "";
			if (mDTransThread != null) {
				Debug.d(TAG, "--->refreshCount: " + i + "-" + count + " [" + mDTransThread.getKeptInkThreshold(i) + "-" + mDTransThread.getCount(i) + "]");
				down = "" + (int)(count * mDTransThread.getKeptInkThreshold(i) + mDTransThread.getCount(i));
			}

			String level = "";
            if(mInkManager instanceof SmartCardManager) {
				level = (i == ((SmartCardManager) mInkManager).getInkCount() - 1 ? "B" : "P" + (i + 1)) + "-" + (ink >= 100f ? "100%" : (ink < 0f ? "0" : ((int) ink + "." + ((int) (ink * 10)) % 10 + "%")));
// H.M.Wang 2025-2-19 修改能够显示两个头的寿命锁值功能
			} else if(mInkManager instanceof Hp22mmSCManager) {
				level = (i == ((Hp22mmSCManager) mInkManager).getInkCount() - 1 ? "B" : "P" + (((Hp22mmSCManager)mInkManager).getHeadId(i))) + "-" + (ink >= 100f ? "100%" : (ink < 0f ? "0" : ((int) ink + "." + ((int) (ink * 10)) % 10 + "%")));
// End of H.M.Wang 2025-2-19 修改能够显示两个头的寿命锁值功能
			} else {
				level = "P" + (i + 1) + "-" + (ink >= 100f ? "100%" : (ink < 0f ? "0" : ((int)ink + "." + ((int)(ink*10))%10 + "%")));
			}

			level = level + (down.isEmpty() || down.equals("0") ? "" : "-" + down);

			Debug.d(TAG,  "Pen" + (i+1) + "[" + level + "]");

			mInkValues[i].setVisibility(View.VISIBLE);

			if (!mInkManager.isValid(i)) {
				mInkValues[i].setBackgroundColor(Color.RED);
				if(mInkManager instanceof SmartCardManager) {
					level = (i == ((SmartCardManager) mInkManager).getInkCount() - 1 ? "B" : "P" + (i + 1)) + "-INVALID";
				} else if(mInkManager instanceof Hp22mmSCManager) {
						level = (i == ((Hp22mmSCManager)mInkManager).getInkCount()-1 ? "B" : "P" + (((Hp22mmSCManager)mInkManager).getHeadId(i))) + "-INVALID";
				} else {
					level = "P" + (i + 1) + "-INVALID";
				}
				mInkValues[i].setText(level);

//				if(mInkManager instanceof SmartCardManager) {
//					mInkValues[i].setText((i == ((SmartCardManager)mInkManager).getInkCount()-1 ? "B" : "P" + (i + 1)) + "--");
//				} else {
//					mInkValues[i].setText(String.valueOf(i + 1) + "--");
//				}
				mBtnStart.setClickable(false);
				mTvStart.setTextColor(Color.GRAY);
// H.M.Wang 2023-6-27 增加一个用户定义界面模式，增加该界面当中的特殊变量
				if(Configs.UI_TYPE == Configs.UI_CUSTOMIZED0) {
					mUpCntPrint.setClickable(false);
					mUpCntPrint.setTextColor(Color.GRAY);
					mDnCntPrint.setClickable(false);
					mDnCntPrint.setTextColor(Color.GRAY);
				}
// End of H.M.Wang 2023-6-27 增加一个用户定义界面模式，增加该界面当中的特殊变量

				valid = false;

				mHandler.sendEmptyMessage(MESSAGE_RFID_ALARM);
			} else if (mInkManager instanceof SmartCardManager && ink >= 5.0f ||
					mInkManager instanceof Hp22mmSCManager && ink >= 5.0f ||
					mInkManager instanceof RFIDManager && ink >= 1.0f){
				mInkValues[i].setBackgroundColor(mContext.getResources().getColor(R.color.transparent));
				mInkValues[i].setText(level);
			} else if (ink > 0.0f){
				mInkValues[i].setBackgroundColor(Color.YELLOW);
				mInkValues[i].setText(level);
			} else {
				valid = false;
				mInkValues[i].setBackgroundColor(Color.RED);
				mInkValues[i].setText(level);
				if (mDTransThread != null && mDTransThread.isRunning()) {
					mHandler.sendEmptyMessage(MESSAGE_PRINT_STOP);
				}
			}

			if((mInkManager instanceof SmartCardManager && ink < 5.0f ||
					mInkManager instanceof RFIDManager && ink < 1.0f) &&
					ink > 0f && mInkLow == false) {
				mInkLow = true;
				mHandler.sendEmptyMessageDelayed(MESSAGE_RFID_LOW, 200);
			} else if (ink <= 0f && mInkZero == false) {
				mInkZero = true;
				mHandler.removeMessages(MESSAGE_RFID_LOW);
				if (!Configs.READING) {
					mHandler.sendEmptyMessageDelayed(MESSAGE_RFID_ZERO, 200);
				}
			} else {
				mFlagAlarming = false;
// H.M.Wang 2024-6-15 当ink值正常时，取消报警
				mHandler.removeMessages(MESSAGE_RFID_LOW);
				mHandler.removeMessages(MESSAGE_RFID_ZERO);
// End of H.M.Wang 2024-6-15 当ink值正常时，取消报警
			}
		}
		if(valid) {
			mBtnStart.setClickable(true);
			mTvStart.setTextColor(Color.BLACK);
// H.M.Wang 2023-6-27 增加一个用户定义界面模式，增加该界面当中的特殊变量
			if(Configs.UI_TYPE == Configs.UI_CUSTOMIZED0) {
				mUpCntPrint.setClickable(true);
				mUpCntPrint.setTextColor(Color.BLACK);
				mDnCntPrint.setClickable(true);
				mDnCntPrint.setTextColor(Color.BLACK);
			}
// End of H.M.Wang 2023-6-27 增加一个用户定义界面模式，增加该界面当中的特殊变量
		}
// H.M.Wang 2023-6-14 借用这个常驻线程，显示SC初始化出现失败的状态
		if(mInkManager instanceof SmartCardManager) {
			Debug.d(TAG, ((SmartCardManager)mInkManager).getSCFailedNums());
			mSCMonitorInfo.setText(((SmartCardManager)mInkManager).getSCFailedNums());
		}
// End of H.M.Wang 2023-6-14 借用这个常驻线程，显示SC初始化出现失败的状态
		refreshVoltage();
		refreshPulse();
// H.M.Wang 2023-9-20 追加一个步长细分数值显示的功能
		if(SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_USER_MODE) == SystemConfigFile.USER_MODE_NONE && Configs.UI_TYPE == Configs.UI_STANDARD) {
// H.M.Wang 2023-12-14 当INDEX_USER_MODE从其它的模式改回到USER_MODE_NONE时，可能mSubStepTV并没有被初始化，因此会死机
			if(null != mSubStepTV) {
// End of H.M.Wang 2023-12-14 当INDEX_USER_MODE从其它的模式改回到USER_MODE_NONE时，可能mSubStepTV并没有被初始化，因此会死机
				if(mSysconfig.getParam(SystemConfigFile.INDEX_SUB_STEP) > 1) {
					mSubStepTV.setVisibility(View.VISIBLE);
					mSubStepTV.setText("" + RTCDevice.getInstance(mContext).readSubStep());
				} else {
					mSubStepTV.setVisibility(View.GONE);
				}
			}
		}
// End of H.M.Wang 2023-9-20 追加一个步长细分数值显示的功能
	}

	private void refreshCount() {
		Debug.d(TAG, "refreshCount = " + mCounter);
		mCtrlTitle.setText(String.valueOf(mCounter));
// H.M.Wang 2023-1-18 系统启动大概需要25000ms，如果立即启动refreshInk，会因为没有数据而导致误警报，所以放大到50000ms后才显示refreshInk
		if(SystemClock.uptimeMillis() > 50000 && System.currentTimeMillis() - inkDispInterval > 100) {
// End of H.M.Wang 2023-1-18 系统启动大概需要25000ms，如果立即启动refreshInk，会因为没有数据而导致误警报，所以放大到50000ms后才显示refreshInk
//Debug.d(TAG, "SystemClock.uptimeMillis() = " + SystemClock.uptimeMillis());
			refreshInk();
		}
	}

/*
	private void refreshInk() {
		float ink = mInkManager.getLocalInkPercentage(mRfid);
// H.M.Wang 2022-5-9 增加img的版本号的输出，同时输出apk的版本号
		Debug.d(TAG,  "[" + PlatformInfo.getImgUniqueCode() + "-" + BuildConfig.VERSION_CODE + "] --->refresh ink: " + mRfid + " = " + ink);
// End of H.M.Wang 2022-5-9 增加img的版本号的输出，同时输出apk的版本号
		String level = "";
		if(mInkManager instanceof RFIDManager) {
// H.M.Wang 2022-11-20 修改字符串生成方法，在阿拉伯文版本当中，String.format会产生乱码
//			level = String.valueOf(mRfid + 1) + "-" + (String.format("%.1f", ink) + "%");
			level = (mRfid + 1) + "-" + (int)ink + "." + ((int)(ink*10))%10 + "%";
// End of H.M.Wang 2022-11-20 修改字符串生成方法，在阿拉伯文版本当中，String.format会产生乱码
		} else {
// H.M.Wang 2022-11-20 修改字符串生成方法，在阿拉伯文版本当中，String.format会产生乱码
//			level = (mRfid == mSysconfig.getPNozzle().mHeads ? "B" : "P" + (mRfid + 1)) + "-" + (ink >= 100f ? "100%" : (ink < 0f ? "-" : (String.format("%.1f", ink) + "%")));
			level = (mRfid == ((SmartCardManager)mInkManager).getInkCount()-1 ? "B" : "P" + (mRfid + 1)) + "-" + (ink >= 100f ? "100%" : (ink < 0f ? "-" : ((int)ink + "." + ((int)(ink*10))%10 + "%")));
// End of H.M.Wang 2022-11-20 修改字符串生成方法，在阿拉伯文版本当中，String.format会产生乱码
		}

		if (!mInkManager.isValid(mRfid)) {
			mInkLevel.setBackgroundColor(Color.RED);
// H.M.Wang 2022-1-22 修改错误信息，在Smartcard的时候避免显示1--这样的错误，而是B，P开头的错误信息，与正常信息保持一致
            if(mInkManager instanceof RFIDManager) {
                mInkLevel.setText(String.valueOf(mRfid + 1) + "--");
            } else {
                mInkLevel.setText((mRfid == ((SmartCardManager)mInkManager).getInkCount()-1 ? "B" : "P" + (mRfid + 1)) + "--");
            }
// End of H.M.Wang 2022-1-22 修改错误信息，在Smartcard的时候避免显示1--这样的错误，而是B，P开头的错误信息，与正常信息保持一致

			// H.M.Wang RFID错误时报警，禁止打印
			mBtnStart.setClickable(false);
			mTvStart.setTextColor(Color.GRAY);

			mHandler.sendEmptyMessage(MESSAGE_RFID_ALARM);
// H.M.Wang 2020-11-27 追加当墨量小于5%的时候，黄底字报警
// H.M.Wang 2020-12-21 Smartcard时为5%，RFID时为1%
//		} else if (ink >= 5.0f){
		} else if (mInkManager instanceof SmartCardManager && ink >= 5.0f ||
					mInkManager instanceof RFIDManager && ink >= 1.0f){
// End of H.M.Wang 2020-12-21 Smartcard时为5%，RFID时为1%
// End of H.M.Wang 2020-11-27 追加当墨量小于5%的时候，黄底字报警
// H.M.Wang 2020-11-17 这个设置导致多个RFID头墨量显示时，会把开始打印按键错误激活
			// H.M.Wang RFID恢复正常，打开打印
//			mBtnStart.setClickable(true);
//			mTvStart.setTextColor(Color.BLACK);
// End of H.M.Wang 2020-11-17 这个设置导致

			//mInkLevel.clearAnimation();
			mInkLevel.setBackgroundColor(mContext.getResources().getColor(R.color.background));
			mInkLevel.setText(level);
		} else if (ink > 0.0f){
			mInkLevel.setBackgroundColor(Color.YELLOW);
			mInkLevel.setText(level);
		} else {
			mInkLevel.setBackgroundColor(Color.RED);
			mInkLevel.setText(level);
			//閹栧�肩埐0鍋滄鎵撳嵃
			if (mDTransThread != null && mDTransThread.isRunning()) {
				mHandler.sendEmptyMessage(MESSAGE_PRINT_STOP);
			}
			
		}

		// Debug.e(TAG, "--->ink = " + ink + ", " + (ink <= 1.0f) + ", " + (ink > 0f));
		// Debug.e(TAG, "--->ink = " + ink + ", " + (ink <= 0f));
// H.M.Wang 2020-11-27 追加当墨量小于5%的时候，黄底字报警
// H.M.Wang 2020-12-21 Smartcard时为5%，RFID时为1%
//		if (ink < 5.0f && ink > 0f && mInkLow == false) {
		if((mInkManager instanceof SmartCardManager && ink < 5.0f ||
			mInkManager instanceof RFIDManager && ink < 1.0f) &&
			ink > 0f && mInkLow == false) {
// End of H.M.Wang 2020-12-21 Smartcard时为5%，RFID时为1%
// End of H.M.Wang 2020-11-27 追加当墨量小于5%的时候，黄底字报警
			mInkLow = true;
			mHandler.sendEmptyMessageDelayed(MESSAGE_RFID_LOW, 200);
		} else if (ink <= 0f && mInkZero == false) {
			mInkZero = true;
			mHandler.removeMessages(MESSAGE_RFID_LOW);
			if (!Configs.READING) {
				mHandler.sendEmptyMessageDelayed(MESSAGE_RFID_ZERO, 200);
			}
		} else {
			mFlagAlarming = false;
		}
		refreshVoltage();
		refreshPulse();
	}

	private void refreshCount() {
		float count = 0;
		// String cFormat = getResources().getString(R.string.str_print_count);
		// ((MainActivity)getActivity()).mCtrlTitle.setText(String.format(cFormat, mCounter));

		count = mInkManager.getLocalInk(mRfid) - 1;
		if (mDTransThread != null) {
//			Debug.d(TAG, "--->count: " + count);
			Debug.d(TAG, "--->refreshCount: " + mRfid + "-" + count + " [" + mDTransThread.getKeptInkThreshold(mRfid) + "-" + mDTransThread.getCount(mRfid) + "]");
			count = count * mDTransThread.getKeptInkThreshold(mRfid) + mDTransThread.getCount(mRfid);
//			Debug.d(TAG, "--->refreshCount: " + mRfid + "-" + count + " [" + mDTransThread.getKeptInkThreshold(mRfid) + "-" + mDTransThread.getCount(mRfid) + "]");
		}
		if (count < 0) {
			count = 0;
		}
// H.M.Wang 2020-8-11 将原来显示在画面头部的墨量和减锁信息移至ControlTab
//		((MainActivity) getActivity()).setCtrlExtra(mCounter, (int) count);
		mCtrlTitle.setText(String.valueOf(mCounter));
		mCountdown.setText(String.valueOf((int)count));
// End of H.M.Wang 2020-8-11 将原来显示在画面头部的墨量和减锁信息移至ControlTab
	}
*/
// End of H.M.Wang 2023-1-17 修改主页面的显示逻辑，取消原来的锁值显示，将原来的锁值和剩余打印次数合并，显示在画面的左下角，并且同时显示最多6个头的锁值和剩余次数

	private void setDevNo(String dev) {
		((MainActivity) getActivity()).setDevNo(dev);
	}
	
	/**
	 * 娓│瀵﹂殯鎯呮硜鐖瞤ower鍊煎湪35-44涔嬮枔锛屽皪瀵﹂殯鍊奸�茶灏嶆噳
	 */
	private void refreshPower() {
		Debug.d(TAG, "--->refreshPower");
// H.M.Wang 2024-11-5 增加A133平台的判断
//		if (PlatformInfo.PRODUCT_SMFY_SUPER3.equalsIgnoreCase(PlatformInfo.getProduct())) {
		if (PlatformInfo.isSmfyProduct() || PlatformInfo.isA133Product()) {
// End of H.M.Wang 2024-11-5 增加A133平台的判断
// H.M.Wang 2023-10-13 增加一个AD参数，当AD=0时，按原有策略(根据img的类型显示电池图标）；当AD=1时，无条件显示电池图标；当AD=2时，显示气压参数，具体方法待定
			if(mSysconfig.getParam(SystemConfigFile.INDEX_AD) == 1) {
				mPowerStat.setVisibility(View.VISIBLE);
			} else if(mSysconfig.getParam(SystemConfigFile.INDEX_AD) == 2) {
				mPowerStat.setVisibility(View.GONE);
				// 气压的显示算法及方法待定
			} else if(mSysconfig.getParam(SystemConfigFile.INDEX_AD) == 0) {
				String imgUC = PlatformInfo.getImgUniqueCode();
				if(PlatformInfo.isMImgType(imgUC) || imgUC.startsWith("O7GS")) {
					mPowerStat.setVisibility(View.GONE);
					return;
				} else {
					mPowerStat.setVisibility(View.VISIBLE);
				}
			}
// End of H.M.Wang 2023-10-13 增加一个AD参数，当AD=0时，按原有策略(根据img的类型显示电池图标）；当AD=1时，无条件显示电池图标；当AD=2时，显示气压参数，具体方法待定
			int power = LRADCBattery.getPower();
			Debug.d(TAG, "--->power: " + power);
			if (power >= 41) {
//				mPower.setText(String.valueOf(100));
				if(Configs.UI_TYPE == Configs.UI_CUSTOMIZED0) {
					mPowerStat.setImageResource(R.drawable.battery_100);
				} else {
					mPowerStat.setImageResource(R.drawable.battery100);
				}
			} else if (power >= 38) {
//				mPower.setText(String.valueOf(75));
				if(Configs.UI_TYPE == Configs.UI_CUSTOMIZED0) {
					mPowerStat.setImageResource(R.drawable.battery_75);
				} else {
					mPowerStat.setImageResource(R.drawable.battery75);
				}
			} else if (power >= 36) {
//				mPower.setText(String.valueOf(50));
				if(Configs.UI_TYPE == Configs.UI_CUSTOMIZED0) {
					mPowerStat.setImageResource(R.drawable.battery_50);
				} else {
					mPowerStat.setImageResource(R.drawable.battery50);
				}
			} else if (power >= 35) {
//				mPower.setText(String.valueOf(25));
				if(Configs.UI_TYPE == Configs.UI_CUSTOMIZED0) {
					mPowerStat.setImageResource(R.drawable.battery_25);
				} else {
					mPowerStat.setImageResource(R.drawable.battery25);
				}
			} else if (power >= 33) {
//				mPower.setText(String.valueOf(0));
				if(Configs.UI_TYPE == Configs.UI_CUSTOMIZED0) {
					mPowerStat.setImageResource(R.drawable.battery_0);
				} else {
					mPowerStat.setImageResource(R.drawable.battery0);
				}
			} else if (power >= 20) {
				if(Configs.UI_TYPE == Configs.UI_CUSTOMIZED0) {
					mPowerStat.setImageResource(R.drawable.battery_0);
				} else {
					mPowerStat.setImageResource(R.drawable.battery0);
				}
			} else {
				// mPower.setText("--");
				mPowerStat.setVisibility(View.GONE);
			}
			//mPowerV.setText(String.valueOf(power));
			// mTime.setText("0");
			// display Voltage & pulse width

// H.M.Wang 2023-10-13 循环时间从5分钟改为2秒
			mHandler.sendEmptyMessageDelayed(MESSAGE_REFRESH_POWERSTAT, 2*1000);
// End of H.M.Wang 2023-10-13 循环时间从5分钟改为2秒
		}
	}

	/**
	 * if setting param25 == on, read from RFID feature 5
	 * if setting param25 == off, read from setting param 26
	 */
	private void refreshVoltage() {
		boolean auto = false;
		if (mInkManager == null) {
			auto = false;
		} else {
			int vol = mSysconfig.getParam(24);
			if (vol > 0) {
				auto = true;
			}
		}
		if (auto) {
			int vol = mInkManager.getFeature(0, 4);
// H.M.Wang 2023-1-17 调整电压的显示单位
//			mPowerV.setText(String.valueOf(vol));
			mPowerV.setText(String.valueOf(vol/10) + "." + String.valueOf(vol%10) + " V");
// End of H.M.Wang 2023-1-17 调整电压的显示单位
		} else {
// H.M.Wang 2023-1-17 调整电压的显示单位
//			mPowerV.setText(String.valueOf(mSysconfig.getParam(25)));
			mPowerV.setText(String.valueOf(mSysconfig.getParam(25)/10) + "." + String.valueOf(mSysconfig.getParam(25)%10) + "V");
// End of H.M.Wang 2023-1-17 调整电压的显示单位
		}
	}

	/**
	 * if setting param27 == on, read from RFID feature 4
	 * if setting param27 == off, read from setting param 28
	 */
	private void refreshPulse() {
		boolean auto = false;
		if (mInkManager == null) {
			auto = false;
		} else {
			int p = mSysconfig.getParam(26);
			if (p > 0) {
				auto = true;
			}
		}
		if (auto) {
			int pulse = mInkManager.getFeature(0, 5);
// H.M.Wang 2023-1-17 调整脉宽的显示单位
//			mTime.setText(String.valueOf(pulse));
			mTime.setText(String.valueOf(pulse/10) + "." + String.valueOf(pulse%10) + " μs");
// End of H.M.Wang 2023-1-17 调整脉宽的显示单位
		} else {
// H.M.Wang 2023-1-17 调整脉宽的显示单位
//			mTime.setText(String.valueOf(mSysconfig.getParam(27)));
			mTime.setText(String.valueOf(mSysconfig.getParam(27)/10) + "." + String.valueOf(mSysconfig.getParam(27)%10) + "uS");
// End of H.M.Wang 2023-1-17 调整脉宽的显示单位
		}
	}

// H.M.Wang 2022-11-29 初始化UG的信息
	public void initUGParams(String message) {
		mUGSubObjs = MessageTask.parseUserGroup(message);
		mUGSubIndex = MessageTask.getUGIndex(message);
		Debug.d(TAG, "mUGSubObjs.size() = " + mUGSubObjs.size() + "; mUGSubIndex = " + mUGSubIndex);
	}
// End of H.M.Wang 2022-11-29 初始化UG的信息

	private boolean messageNew = false;

	private void setMessage(String message) {
// H.M.Wang 2022-11-29 追加设置新的信息路径的处理。在浏览信息时，从前不同的信息mObjPath肯定是不同的，但是UserGroup的情况下，mObjPath会保持不变，只是会调整内部群组的序号，这样就需要考虑这种情况
		if(message.startsWith(Configs.USER_GROUP_PREFIX)) {
			if(!message.equals(mObjPath)) {
				initUGParams(message);
			}
		}
// End of H.M.Wang 2022-11-29 追加设置新的信息路径的处理。在浏览信息时，从前不同的信息mObjPath肯定是不同的，但是UserGroup的情况下，mObjPath会保持不变，只是会调整内部群组的序号，这样就需要考虑这种情况

		mObjPath = message;
		messageNew = true;
	}

	public Handler mHandler = new Handler(){
		public void handleMessage(Message msg) {
			final String pcMsg = msg.getData().getString(Constants.PC_CMD, "");
			final boolean printAfterLoad = msg.getData().getBoolean("printAfterLoad", false);
			final boolean printNext = msg.getData().getBoolean("printNext", false);
			switch(msg.what)
			{
				case MESSAGE_OPEN_PREVIEW:
					String tlk = msg.getData().getString("file");
					setMessage(tlk);
					if (mPreBitmap != null) {
						BinFromBitmap.recyleBitmap(mPreBitmap);
					}
					Debug.d(TAG, "--->mObjPath: " + mObjPath);

// H.M.Wang 2022-11-29 支持显示UG的预览图片，如果子信息有指定，则显示子信息的预览，如果无，则显示母信息的预览，非UG信息照旧
//					mPreBitmap = BitmapFactory.decodeFile(MessageTask.getPreview(mObjPath));
					if(mObjPath.startsWith(Configs.USER_GROUP_PREFIX) && mUGSubIndex >=0 && mUGSubIndex < mUGSubObjs.size()) {
						mPreBitmap = BitmapFactory.decodeFile(MessageTask.getPreview(mObjPath + File.separator + mUGSubObjs.get(mUGSubIndex)));
					} else {
						mPreBitmap = BitmapFactory.decodeFile(MessageTask.getPreview(mObjPath));
					}
// End of H.M.Wang 2022-11-29 支持显示UG的预览图片，如果子信息有指定，则显示子信息的预览，如果无，则显示母信息的预览，非UG信息照旧

					dispPreview(mPreBitmap);
					mMsgFile.setText(mObjPath);
					mSysconfig.saveLastMsg(mObjPath);

					Message message = mHandler.obtainMessage(MESSAGE_OPEN_TLKFILE);
					if (printAfterLoad) {
// H.M.Wang 2020-4-30 修改打印过程当中编辑内容保存并打印时，重复打印以前内容的问题
						if(null != mDTransThread && mDTransThread.isRunning()) {
							mDTransThread.getCurData().prepareBackgroudBuffer();
						} else {
							message.sendToTarget();
						}
// End of H.M.Wang 2020-4-30 修改打印过程当中编辑内容保存并打印时，重复打印以前内容的问题
					}
					if (printNext) {
						Bundle bundle = new Bundle();
						bundle.putBoolean("printNext", printNext);
						message.setData(bundle);
						message.sendToTarget();
					}

					if(mSysconfig.getParam(SystemConfigFile.INDEX_USER_MODE) == SystemConfigFile.USER_MODE_2) {
// H.M.Wang 2022-11-29 补充 2022-10-25修改QuickGroup时的修改遗漏
//						if (mObjPath.startsWith(Configs.GROUP_PREFIX)) {
						if (mObjPath.startsWith(Configs.GROUP_PREFIX) || mObjPath.startsWith(Configs.QUICK_GROUP_PREFIX)) {
// H.M.Wang 2022-11-29 补充 2022-10-25修改QuickGroup时的修改遗漏
							mEditArea.setVisibility(View.GONE);
						} else {
							mEditTask = new MessageTask(mContext, mObjPath);
							for(BaseObject obj : mEditTask.getObjects()) {
								if(obj instanceof TextObject) {
									mEditObject = (TextObject)obj;
									mCntET.setText(mEditObject.getContent());
									break;
								}
							}
						}
					}
					break;

				case MESSAGE_OPEN_TLKFILE:		//
					// H.M.Wang 2019-10-27 修改原来代码BUG，当mObjPath == null的时候，并且(!printNext && !messageNew )的时候，会出现死机现象
					if (mObjPath == null) {
						ToastUtil.show(mContext, R.string.str_toast_no_message);
						break;
					}
					Debug.d(TAG, "--->Print open message");
					// End -------------------------------
					if (!printNext && !messageNew ) {
						mHandler.sendEmptyMessage(MESSAGE_OPEN_MSG_SUCCESS);
						break;
					}
					progressDialog();
					
//					mObjPath = msg.getData().getString("file");
					final String msgPC = msg.getData().getString(Constants.PC_CMD);

					Debug.d(TAG, "open tlk :" + mObjPath );
					//startPreview();
					if (mObjPath == null) {
						dismissProgressDialog();
						break;
					}
					
					//鏂规2锛氫粠tlk鏂囦欢閲嶆柊缁樺埗鍥剧墖锛岀劧鍚庤В鏋愮敓鎴恇uffer
					//parseTlk(f);
					//initBgBuffer();
					mMsgTask.clear();
					new Thread() {
						@Override
						public void run() {
							mMsgTask.clear();
							/**鑾峰彇鎵撳嵃缂╃暐鍥撅紝鐢ㄤ簬棰勮灞曠幇**/
// H.M.Wang 2022-11-29 增加UG的内部群组的追加功能
							if (mObjPath.startsWith(Configs.USER_GROUP_PREFIX) && mUGSubObjs.size() > 0) {
								for (String path : mUGSubObjs) {
									MessageTask task = new MessageTask(mContext, mObjPath + "/" + path);
									mMsgTask.add(task);
								}
							} else
// End of H.M.Wang 2022-11-29 增加UG的内部群组的追加功能
// H.M.Wang 2022-10-25 追加一个“快速分组”的信息类型，该类型以Configs.QUICK_GROUP_PREFIX为文件名开头，信息中的每个超文本作为一个独立的信息保存在母信息的目录当中，并且所有的子信息作为一个群组管理，该子群组的信息也保存到木信息的目录当中
							if (mObjPath.startsWith(Configs.QUICK_GROUP_PREFIX)) {
								List<String> paths = MessageTask.parseQuickGroup(mObjPath);
								for (String path : paths) {
									MessageTask task = new MessageTask(mContext, mObjPath + "/" + path);
									mMsgTask.add(task);
								}
							} else
// End of H.M.Wang 2022-10-25 追加一个“快速分组”的信息类型，该类型以Configs.QUICK_GROUP_PREFIX为文件名开头，信息中的每个超文本作为一个独立的信息保存在母信息的目录当中，并且所有的子信息作为一个群组管理，该子群组的信息也保存到木信息的目录当中
							if (mObjPath.startsWith(Configs.GROUP_PREFIX)) {   // group messages
								List<String> paths = MessageTask.parseGroup(mObjPath);
								for (String path : paths) {
									MessageTask task = new MessageTask(mContext, path);
									mMsgTask.add(task);
								}
							} else {
								MessageTask task = new MessageTask(mContext, mObjPath);
								mMsgTask.add(task);
							}
							if (printNext) {
								mHandler.sendEmptyMessage(MESSAGE_OPEN_NEXT_MSG_SUCCESS);
								return;
							}
							Message mesg = mHandler.obtainMessage(MESSAGE_OPEN_MSG_SUCCESS);
							if (msgPC != null) {
								Bundle bundle = new Bundle();
								bundle.putString(Constants.PC_CMD, msgPC);
								mesg.setData(bundle);
							}
//							mHandler.sendEmptyMessage(MESSAGE_OPEN_MSG_SUCCESS);
							mesg.sendToTarget();

						}
					}.start();

					break;
				case MESSAGE_OPEN_GROUP:
					Debug.d(TAG, "--->group");
					ArrayList<String> files = msg.getData().getStringArrayList("file");
					MessageGroupsortDialog dialog = new MessageGroupsortDialog(mContext, files);
					dialog.setOnPositiveClickedListener(new OnPositiveListener() {
						@Override
						public void onClick() {

						}

						@Override
						public void onClick(String content) {
							Debug.d(TAG, "--->group: " + content);
							// save group information & send message to handle opening
							Message msg = mHandler.obtainMessage(MESSAGE_OPEN_PREVIEW);
							Bundle b = new Bundle();
							b.putString("file", content);
							msg.setData(b);
							msg.sendToTarget();
						}
					});
					dialog.show();
					break;
				case MESSAGE_OPEN_NEXT_MSG_SUCCESS:
					if (mDTransThread == null) {
						break;
					}
					mDTransThread.resetTask(mMsgTask);

// H.M.Wang 2022-11-29 支持显示UG的预览图片，如果子信息有指定，则显示子信息的预览，如果无，则显示母信息的预览，非UG信息照旧
//					mPreBitmap = BitmapFactory.decodeFile(MessageTask.getPreview(mObjPath));
					if(mObjPath.startsWith(Configs.USER_GROUP_PREFIX) && mUGSubIndex >=0 && mUGSubIndex < mUGSubObjs.size()) {
// H.M.Wang 2022-11-29 当信息类型为UG的时候，开始打印时，从当前的子信息开始打印，而不是一概从头开始打印
						mDTransThread.setIndex(mUGSubIndex);
// End of H.M.Wang 2022-11-29 当信息类型为UG的时候，开始打印时，从当前的子信息开始打印，而不是一概从头开始打印
						mPreBitmap = BitmapFactory.decodeFile(MessageTask.getPreview(mObjPath + File.separator + mUGSubObjs.get(mUGSubIndex)));
					} else {
						mPreBitmap = BitmapFactory.decodeFile(MessageTask.getPreview(mObjPath));
					}
// End of H.M.Wang 2022-11-29 支持显示UG的预览图片，如果子信息有指定，则显示子信息的预览，如果无，则显示母信息的预览，非UG信息照旧

					dispPreview(mPreBitmap);
//					refreshCount();
					mMsgFile.setText(mObjPath);

					mSysconfig.saveLastMsg(mObjPath);
					dismissProgressDialog();

					break;
				case MESSAGE_OPEN_MSG_SUCCESS:
					Debug.d(TAG, "--->Print open message success");
					if (mPreBitmap != null) {
						BinFromBitmap.recyleBitmap(mPreBitmap);
					}
					//鏂规1锛氫粠bin鏂囦欢鐢熸垚buffer
					initDTThread();

					Debug.d(TAG, "--->init thread ok");

// H.M.Wang 2022-11-29 支持显示UG的预览图片，如果子信息有指定，则显示子信息的预览，如果无，则显示母信息的预览，非UG信息照旧
//					mPreBitmap = BitmapFactory.decodeFile(MessageTask.getPreview(mObjPath));
					if(mObjPath.startsWith(Configs.USER_GROUP_PREFIX) && mUGSubIndex >=0 && mUGSubIndex < mUGSubObjs.size()) {
// H.M.Wang 2022-11-29 当信息类型为UG的时候，开始打印时，从当前的子信息开始打印，而不是一概从头开始打印
						mDTransThread.setIndex(mUGSubIndex);
// End of H.M.Wang 2022-11-29 当信息类型为UG的时候，开始打印时，从当前的子信息开始打印，而不是一概从头开始打印
						mPreBitmap = BitmapFactory.decodeFile(MessageTask.getPreview(mObjPath + File.separator + mUGSubObjs.get(mUGSubIndex)));
					} else {
						mPreBitmap = BitmapFactory.decodeFile(MessageTask.getPreview(mObjPath));
					}
// End of H.M.Wang 2022-11-29 支持显示UG的预览图片，如果子信息有指定，则显示子信息的预览，如果无，则显示母信息的预览，非UG信息照旧

// H.M.Wang 2020-6-23 打开注释，显示预览图
					dispPreview(mPreBitmap);
// End of H.M.Wang 2020-6-23 打开注释，显示预览图

					Debug.d(TAG, "--- refreshInk ---");
					refreshInk();
					refreshCount();
					mMsgFile.setText(mObjPath);

					mSysconfig.saveLastMsg(mObjPath);
//					dismissProgressDialog();

					if (Configs.READING) {
					// H.M.Wang 2019-09-13 RGNORE_RFID的时候将原来的网络命令传递下去
//						mHandler.sendEmptyMessage(MESSAGE_PRINT_START);
						msg = mHandler.obtainMessage(MESSAGE_PRINT_START);

						if (pcMsg != null) {
							Bundle bundle = new Bundle();
							bundle.putString(Constants.PC_CMD, pcMsg);
							msg.setData(bundle);
						}

						mHandler.sendMessage(msg);
					} else {
						mHandler.sendEmptyMessage(MESSAGE_PRINT_CHECK_UID);
					}

					if("100".equals(PrnComd))	
					{
						 msg = mHandler.obtainMessage(MESSAGE_PRINT_START);

						if (pcMsg != null) {
							Bundle bundle = new Bundle();
							bundle.putString(Constants.PC_CMD, pcMsg);
							msg.setData(bundle);
						}

						 mHandler.sendMessage(msg);

						PrnComd="";
					}
					break;
				case MESSAGE_UPDATE_PRINTSTATE:
					String text = msg.getData().getString("text");
					mPrintStatus.setText("result: "+text);
					break;
				case MESSAGE_UPDATE_INKLEVEL:
					Bundle bundle = msg.getData();
					int level = bundle.getInt("ink_level");
					mFeatureCorrect = bundle.getBoolean("feature", true);
					Debug.d(TAG, "--- refreshInk ---");
					refreshInk();
					break;
				case MESSAGE_DISMISS_DIALOG:
					mLoadingDialog.dismiss();
					break;
				case MESSAGE_PAOMADENG_TEST:
					
					char[] data = new char[32];
					for (char i = 0; i < 15; i++) {
						data[2*i] = (char)(0x01<<i);
						data[2*i+1] = 0xffff;
					}
					data[30] = 0xff;
					data[31] = 0xff;
//					char[] data = new char[2];
//					if (testdata < 0 || testdata > 15)
//						testdata = 0;
//					data[0] = (char) (0x0001 << testdata);
//					data[1] = (char) (0x0001 << testdata);
//					testdata++;
					FpgaGpioOperation.writeData(FpgaGpioOperation.DATA_GENRE_NEW, FpgaGpioOperation.FPGA_STATE_OUTPUT,data, data.length*2);
					mHandler.sendEmptyMessageDelayed(MESSAGE_PAOMADENG_TEST, 1000);
					break;
				case MESSAGE_PRINT_CHECK_UID:
					Debug.d(TAG, "--->Print check UUID");
					if (mDTransThread != null && mDTransThread.isRunning()) {
						Debug.d(TAG, "--->printing...");
						handleError(R.string.str_print_printing, pcMsg);
						break;
					}
					//Debug.d(TAG, "--->initDTThread");
					ExtendInterceptor interceptor = new ExtendInterceptor(mContext);
					ExtendStat stat = interceptor.getExtend();
					boolean statChanged = false;
					if (stat != extendStat) {
						statChanged = true;
						extendStat = stat;
					}
					if (mDTransThread == null  || statChanged) {
						initDTThread();
					}
					if (mDTransThread == null) {
						handleError(R.string.str_toast_no_message, pcMsg);
						break;
					}
					Debug.d(TAG, "--->prepare buffer");
					List<DataTask> dt = mDTransThread.getData();
					int heads = 1;
					if (dt != null && dt.size() > 0) {
						heads = dt.get(0).getPNozzle().mHeads;
					}
					mInkManager.checkUID(heads);
					break;
				case SmartCardManager.MSG_SMARTCARD_CHECK_FAILED:
// H.M.Wang 2020-5-18 Smartcard定期检测出现错误显示错误码
// H.M.Wang 2021-1-7 取消显示错误号，这个是为了调试
//					mInkLevel.setBackgroundColor(Color.RED);
//					mInkLevel.setText("" + msg.arg1);
// End of H.M.Wang 2021-1-7 取消显示错误号，这个是为了调试
// End of H.M.Wang 2020-5-18 Smartcard定期检测出现错误显示错误码
					Debug.d(TAG, "--->Smartcard check UUID fail");
					handleError(R.string.str_toast_ink_error, pcMsg);
					ThreadPoolManager.mControlThread.execute(new Runnable() {
						@Override
						public void run() {
							ExtGpio.playClick();
							try{Thread.sleep(50);}catch(Exception e){};
							ExtGpio.playClick();
							try{Thread.sleep(50);}catch(Exception e){};
							ExtGpio.playClick();
						}
					});
					break;
				case RFIDManager.MSG_RFID_CHECK_FAIL:
					Debug.d(TAG, "--->Rfid check UUID fail");
					handleError(R.string.str_toast_ink_error, pcMsg);
					ThreadPoolManager.mControlThread.execute(new Runnable() {
						@Override
						public void run() {
							ExtGpio.playClick();
							try{Thread.sleep(50);}catch(Exception e){};
							ExtGpio.playClick();
							try{Thread.sleep(50);}catch(Exception e){};
							ExtGpio.playClick();
						}
					});
					break;
// H.M.Wang 2022-8-31 追加一个消息，显示提示不要带电更换墨盒
				case RFIDManager.MSG_RFID_CHECK_FAIL_INK_CHANGED:
					Debug.d(TAG, "--->Print check UUID fail. Ink Changed!!!");
					handleError(R.string.str_toast_ink_error_ink_changed, pcMsg);
					ThreadPoolManager.mControlThread.execute(new Runnable() {
						@Override
						public void run() {
							ExtGpio.playClick();
							try{Thread.sleep(50);}catch(Exception e){};
							ExtGpio.playClick();
							try{Thread.sleep(50);}catch(Exception e){};
							ExtGpio.playClick();
						}
					});
					break;
// End of  H.M.Wang 2022-8-31 追加一个消息，显示提示不要带电更换墨盒
// H.M.Wang 2022-1-13 追加获取RFID写3次失败后的通知
                case RFIDManager.MSG_RFID_WRITE_FAIL:
                    ToastUtil.show(mContext, "rfid write failed.");
                    break;
// End of H.M.Wang 2022-1-13 追加获取RFID写3次失败后的通知
				case SmartCardManager.MSG_SMARTCARD_CHECK_SUCCESS:
				case RFIDManager.MSG_RFID_CHECK_SUCCESS:
				case MESSAGE_PRINT_START:
					Debug.d(TAG, "--->Print check success = " + msg.what);
					if (mDTransThread != null && mDTransThread.isRunning()) {
						// H.M.Wang注释掉一行
//						handleError(R.string.str_print_printing, pcMsg);
						break;
					}

// H.M.Wang 2023-12-13 通过编译，禁止大字机的功能，也就是只能用于HP
					if (Configs.PROHIBIT_BIG_DOTS_FUNCTION) {
// H.M.Wang 2023-12-21 禁止大字机功能的标识要甄别是否为大字机
						PrinterNozzle head = PrinterNozzle.getInstance(mSysconfig.getParam(SystemConfigFile.INDEX_HEAD_TYPE));
						if(head == PrinterNozzle.MESSAGE_TYPE_16_DOT ||
							head == PrinterNozzle.MESSAGE_TYPE_32_DOT ||
// H.M.Wang 2020-7-23 追加32DN打印头
							head == PrinterNozzle.MESSAGE_TYPE_32DN ||
// End of H.M.Wang 2020-7-23 追加32DN打印头
// H.M.Wang 2020-8-18 追加32SN打印头
							head == PrinterNozzle.MESSAGE_TYPE_32SN ||
// End of H.M.Wang 2020-8-18 追加32SN打印头
// H.M.Wang 2020-8-26 追加64SN打印头
							head == PrinterNozzle.MESSAGE_TYPE_64SN ||
// End of H.M.Wang 2020-8-26 追加64SN打印头
// H.M.Wang 2022-10-19 追加64SLANT头
							head == PrinterNozzle.MESSAGE_TYPE_64SLANT ||
// End of H.M.Wang 2022-10-19 追加64SLANT头
// H.M.Wang 2024-4-29 追加64_DOT_ONE喷头类型
							head == PrinterNozzle.MESSAGE_TYPE_64DOTONE ||
// End of H.M.Wang 2024-4-29 追加64_DOT_ONE喷头类型
// H.M.Wang 2024-9-10 增加一个16DOTX4头类型，
                            head == PrinterNozzle.MESSAGE_TYPE_16DOTX4 ||
// End of H.M.Wang 2024-9-10 增加一个16DOTX4头类型，
// H.M.Wang 2022-5-27 追加32x2头类型
							head == PrinterNozzle.MESSAGE_TYPE_32X2 ||
// End of H.M.Wang 2022-5-27 追加32x2头类型
							head == PrinterNozzle.MESSAGE_TYPE_64_DOT ||
// H.M.Wang 2023-7-29 追加48点头
							head == PrinterNozzle.MESSAGE_TYPE_48_DOT ||
// End of H.M.Wang 2023-7-29 追加48点头
// H.M.Wang 2021-8-16 追加96DN头
							head == PrinterNozzle.MESSAGE_TYPE_96DN) {
// End of H.M.Wang 2023-12-21 禁止大字机功能的标识要甄别是否为大字机
							handleError(R.string.str_print_thread_create_err, pcMsg);
							break;
						}
					}
// End of H.M.Wang 2023-12-13 通过编译，禁止大字机的功能，也就是只能用于HP

					if (!checkRfid()) {
						handleError(R.string.str_toast_no_ink, pcMsg);
						return;
					}

					Debug.d(TAG, "--->check rfid ok");

					if (mObjPath == null || mObjPath.isEmpty()) {
						handleError(R.string.str_toast_no_message, Constants.pcErr(pcMsg));
						break;
					}
					// reset value of counter object to system config value
					resetCounterIfNeed();
// H.M.Wang 2020-6-2 只有数据源为文件打印的时候采取检查QR.txt或者QR.csv的存在
//					if (!checkQRFile()) {
// H.M.Wang 2021-1-4 追加数据源FILE2，也是从QR.txt读取DT0,DT1,...,DT9,BARCODE的信息，但是DT赋值根据DT变量内部的序号匹配
//					if (SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_FILE && !checkQRFile()) {
					if((SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_FILE ||
						SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_FILE2) &&
						!checkQRFile()) {
// End of H.M.Wang 2021-1-4 追加数据源FILE2，也是从QR.txt读取DT0,DT1,...,DT9,BARCODE的信息，但是DT赋值根据DT变量内部的序号匹配
// End of H.M.Wang 2020-6-2 只有数据源为文件打印的时候采取检查QR.txt或者QR.csv的存在
						handleError(R.string.str_toast_no_qrfile, Constants.pcErr(pcMsg));
						mHandler.sendEmptyMessage(MESSAGE_RFID_ALARM);
						break;
					} else {
						mFlagAlarming = false;
					}
					Debug.d(TAG, "--->checkQRFile ok");
					List<DataTask> tasks = mDTransThread.getData();
					DataTask task = tasks.get(0);
					if (!task.mTask.isPrintable()) {
						handleError(task.mTask.unPrintableTips(), pcMsg);
						break;
					}

					////////////////// H.M.Wang 2019-9-11 特殊客户需求，仅32点喷头允许打印。不需要时注释掉，
					////// ！！！！！！！！ 请谨慎放开注释，会严重影响功能 ！！！！！！！ ////////
//					if(task.mTask.getNozzle() != PrinterNozzle.MESSAGE_TYPE_32_DOT) {
//						handleError(task.mTask.unPrintableTips(), pcMsg);
//						break;
//					}
					////////////////// H.M.Wang 2019-9-11 特殊客户需求，仅32点喷头允许打印 - 完

					Debug.d(TAG, "--->clean");
					/**
					 * 鍚姩鎵撳嵃鍚庤瀹屾垚鐨勫嚑涓伐浣滐細
					 * 1銆佹瘡娆℃墦鍗帮紝  鍏堟竻绌� 锛堣鏂囦欢锛夛紝 鐒跺悗 鍙戣缃�
					 * 2銆佸惎鍔―ataTransfer绾跨▼锛岀敓鎴愭墦鍗癰uffer锛屽苟涓嬪彂鏁版嵁
					 * 3銆佽皟鐢╥octl鍚姩鍐呮牳绾跨▼锛屽紑濮嬭疆璁璅PGA鐘舵��
					 */
					
					/*鎵撳嵃杩囩▼涓姝㈠垏鎹㈡墦鍗板璞�*/
					switchState(STATE_PRINTING);
					FpgaGpioOperation.clean();
					Debug.d(TAG, "--->update settings");
// H.M.Wang 2024-3-13 当打印头为hp22mm的时候，使用22mm头的专用参数设置
					if(PlatformInfo.getImgUniqueCode().startsWith("22MM")) {
						((Hp22mmSCManager)mInkManager).startPrint();
// H.M.Wang 2025-3-19 追加一个循环功能
						Hp22mm.hp22mmCirculation();
// End of H.M.Wang 2025-3-19 追加一个循环功能
					}
// End of H.M.Wang 2024-3-13 当打印头为hp22mm的时候，使用22mm头的专用参数设置
					FpgaGpioOperation.updateSettings(mContext, task, FpgaGpioOperation.SETTING_TYPE_NORMAL);
					Debug.d(TAG, "--->launch thread");

					if (!mDTransThread.launch(mContext)) {
						handleError(R.string.str_toast_no_bin, pcMsg);
						break;
					}
					Debug.d(TAG, "--->finish ThreadId=" + Thread.currentThread().getId());
					handlerSuccess(R.string.str_print_startok, pcMsg);
					break;
				case MESSAGE_PRINT_STOP:
// 2021-1-11 取消停止打印时重新下发参数
//					FpgaGpioOperation.updateSettings(mContext, null, FpgaGpioOperation.SETTING_TYPE_NORMAL);
// End of 2021-1-11 取消停止打印时重新下发参数
					// do nothing if not in printing state
					if (mDTransThread == null || !mDTransThread.isRunning()) {
						sendToRemote(Constants.pcOk(msg.getData().getString(Constants.PC_CMD)));
						break;
					}
					if (mDTransThread != null && !mDTransThread.isRunning()) {
						switchState(STATE_STOPPED);
// 2021-1-11 取消停止打印时重新下发CLEAN
//						FpgaGpioOperation.clean();
// End of 2021-1-11 取消停止打印时重新下发CLEAN
						break;
					}

					if (mDTransThread != null) {
						mDTransThread.finish();
//						mDTransThread = null;
//						initDTThread();
					}

// H.M.Wang 2024-3-13 当打印头为hp22mm的时候，使用22mm头的专用参数设置
					if(PlatformInfo.getImgUniqueCode().startsWith("22MM")) {
						((Hp22mmSCManager)mInkManager).stopPrint();
					}
// End of H.M.Wang 2024-3-13 当打印头为hp22mm的时候，使用22mm头的专用参数设置

					FpgaGpioOperation.uninit();

					sendToRemote(Constants.pcOk(msg.getData().getString(Constants.PC_CMD)));
					/*鎵撳嵃浠诲姟鍋滄鍚庡厑璁稿垏鎹㈡墦鍗板璞�*/
					switchState(STATE_STOPPED);
					
					ToastUtil.show(mContext, R.string.str_print_stopok);
					FpgaGpioOperation.clean();
//					rollback();
					/* 濡傛灉鐣跺墠鎵撳嵃淇℃伅涓湁瑷堟暩鍣紝闇�瑕佽閷勭暥鍓嶅�煎埌TLK鏂囦欢涓�*/
					updateCntIfNeed();
// H.M.Wang 2020-6-24 停止打印时清空网络打印标识
//					StopFlag=0;
					PrinterFlag=0;
// End of H.M.Wang 2020-6-24 停止打印时清空网络打印标识

					if(mInkManager instanceof SmartCardManager) {
						((SmartCardManager)mInkManager).shutdown();
					}

					new Thread(new Runnable() {
						@Override
						public void run() {
							FpgaGpioOperation.dispLog();
// H.M.Wang 2022-1-11 延时400ms再次下发CLEAN，目的是错过beep，因为这个beep可能会导致PG1，2始终为低
							try{Thread.sleep(400);}catch(Exception e){};
							FpgaGpioOperation.clean();
// End of H.M.Wang 2022-1-11 延时400ms再次下发CLEAN，目的是错过beep，因为这个beep可能会导致PG1，2始终为低
						}
					}).start();
// H.M.Wang 2022-6-20 该部分代码从StopPrint按键响应部分转移到这里，如果在StopPrint按键响应部分，对于网络或者程序控制的停止打印将不会做此操作
// H.M.Wang 2020-1-7 追加群组打印时，显示正在打印的MSG的序号
					mGroupIndex.setVisibility(View.GONE);
// H.M.Wang 2020-4-15 追加群组打印时，显示每个正在打印的message的1.bmp
// H.M.Wang 2021-7-26 改用mPreBitmap，取消aotu变量bmp
					mPreBitmap = BitmapFactory.decodeFile(MessageTask.getPreview(mObjPath));
					dispPreview(mPreBitmap);
					mPrePrintBitmap = null;
// End of H.M.Wang 2021-7-26 改用mPreBitmap，取消aotu变量bmp
// End of H.M.Wang 2020-4-15 追加群组打印时，显示每个正在打印的message的1.bmp
// End of H.M.Wang 2020-1-7 追加群组打印时，显示正在打印的MSG的序号
// End of H.M.Wang 2022-6-20 该部分代码从StopPrint按键响应部分转移到这里，如果在StopPrint按键响应部分，对于网络或者程序控制的停止打印将不会做此操作
					break;
				case MESSAGE_PRINT_END:
					FpgaGpioOperation.uninit();
					switchState(STATE_STOPPED);
					FpgaGpioOperation.clean();
					break;
				case MESSAGE_INKLEVEL_CHANGE:
					int devIndex = msg.arg1;
					// for (int i = 0; i < mSysconfig.getHeads(); i++) {
					// H.M.Wang 2019-09-12 修改在Configs.READING = true时，跳过减记操作
					if(!Configs.READING) {
						mInkManager.downLocal(devIndex);
					}
					// }
					/*鎵撳嵃鏅備笉鍐嶅鏅傛洿鏂板ⅷ姘撮噺*/
					Debug.d(TAG, "--- refreshInk ---");
					refreshInk();
					// mInkManager.write(mHandler);
					break;
				case MESSAGE_COUNT_CHANGE:
/*					mCounter++;
					refreshCount();
					//PrinterDBHelper db = PrinterDBHelper.getInstance(mContext);
					//db.updateCount(mContext, (int) mCounter);
					RTCDevice device = RTCDevice.getInstance(mContext);
					device.writeCounter(mContext, mCounter);
*/
					RTCDevice device = RTCDevice.getInstance(mContext);
					device.writeCounter(mContext, mCounter);
					refreshCount();
					break;
				case MESSAGE_REFRESH_POWERSTAT:
					refreshPower();
					break;
				case MESSAGE_SWITCH_RFID:
					switchRfid();
					break;
				case RFIDManager.MSG_RFID_INIT:
                    mInkManager.init(mHandler);
// H.M.Wang 2020-5-18 取消初始化时调用墨水量显示更新，否则RFID的时候会显示红底101--字样
//					refreshInk();
// End of H.M.Wang 2020-5-18 取消初始化时调用墨水量显示更新，否则RFID的时候会显示红底101--字样
					break;
				case SmartCardManager.MSG_SMARTCARD_INIT_SUCCESS:
					Debug.i(TAG, "Smartcard Initialization Success!");
// H.M.Wang 2020-5-18 取消初始化时调用墨水量显示更新，待Smartcard初始化成功后更新墨水量显示，以满足及时更新Smartcard状态下的墨水量显示
					if (mRfidInit == false) {
						switchRfid();
						refreshCount();
						mRfidInit = true;
					}
//					refreshInk();
// End of H.M.Wang 2020-5-18 取消初始化时调用墨水量显示更新，待Smartcard初始化成功后更新墨水量显示，以满足及时更新Smartcard状态下的墨水量显示
					break;
				case RFIDManager.MSG_RFID_INIT_SUCCESS:
					// mInkManager.read(mHandler);
					break;
				case SmartCardManager.MSG_SMARTCARD_INIT_FAILED:
					Debug.e(TAG, "Smartcard Initialization Failed! [" + msg.arg1 + "]");
// H.M.Wang 2020-5-18 初始化失败时显示错误码
// H.M.Wang 2023-1-17 这个显示应该在refreshInk的地方被重新执行，所以可以省略
//					mInkLevel.setBackgroundColor(Color.RED);
//					mInkLevel.setText("" + msg.arg1);
// End of H.M.Wang 2023-1-17 这个显示应该在refreshInk的地方被重新执行，所以可以省略
// End of H.M.Wang 2020-5-18 初始化失败时显示错误码
//					ToastUtil.show(mContext, "Smartcard Initialization Failed.");
					mHandler.sendEmptyMessage(MESSAGE_RFID_ZERO);
					break;
				case RFIDManager.MSG_RFID_READ_SUCCESS:
					boolean ready = true;
					Bundle bd = (Bundle) msg.getData();
					for (int i=0; i < mSysconfig.getPNozzle().mHeads; i++) {

						if (mInkManager.getLocalInk(i) <= 0) {
							ready = false;
							break;
						}
					}

// H.M.Wang 2021-1-4 当RFID初始化成功的时候，激活打印键，这个方案可能不完美，暂时可以。要解决的问题是，系统刚刚启动的时候，RFID正在初始化，但是refreshInk函数会按着初始化失败关闭打印键，迟到的初始化成功没办法激活打印键
                    if(ready) {
                        mBtnStart.setClickable(true);
                        mTvStart.setTextColor(Color.BLACK);
                    }
// End of H.M.Wang 2021-1-4 当RFID初始化成功的时候，激活打印键，这个方案可能不完美，暂时可以。要解决的问题是，系统刚刚启动的时候，RFID正在初始化，但是refreshInk函数会按着初始化失败关闭打印键，迟到的初始化成功没办法激活打印键

					if (Configs.READING) {
						// H.M.Wang 2019-09-12 修改在Configs.READING = true时，直接显示缺省值，而不是在尝试10次后显示
						mInkManager.defaultInkForIgnoreRfid();

//						if (repeatTimes <= 0) {
//							mInkManager.defaultInkForIgnoreRfid();
//							ready = true;
//						} else {
//							repeatTimes--;
//						}
					}
					if (!ready) {
						mHandler.sendEmptyMessageDelayed(RFIDManager.MSG_RFID_INIT, 5000);
					} else {
						mHandler.removeMessages(MESSAGE_RFID_ZERO);
						mFlagAlarming = false;
						mHandler.sendEmptyMessageDelayed(MESSAGE_RFID_OFF_H7, 2000);
					}
					if (mRfidInit == false) {
						switchRfid();
						refreshCount();
						mRfidInit = true;
					}
// H.M.Wang 2024-7-27 追加蓝牙设备号和蓝牙开关功能
					new Thread(new Runnable() {
						@Override
						public void run() {
//							BLEDevice ble = BLEDevice.getInstance();
//							ble.paramsChanged();
							BluetoothServerManager bsm = BluetoothServerManager.getInstance();
							bsm.paramsChanged();
						}
					}).start();
// End of H.M.Wang 2024-7-27 追加蓝牙设备号和蓝牙开关功能
					break;
				case MESSAGE_RFID_OFF_H7:
					ExtGpio.writeGpio('h', 7, 0);
					break;
				case RFIDManager.MSG_RFID_WRITE_SUCCESS:
					float ink = mInkManager.getLocalInk(0);
					Debug.d(TAG, "--- refreshInk ---");
					refreshInk();
					break;
				case MESSAGE_RFID_LOW:
					Debug.e(TAG, "--->low: play error");
// H.M.Wang 2020-11-27 追加当墨量小于5%的时候，出声报警
					mHandler.sendEmptyMessage(MESSAGE_RFID_ALARM);
// End of H.M.Wang 2020-11-27 追加当墨量小于5%的时候，出声报警
					mHandler.sendEmptyMessageDelayed(MESSAGE_RFID_LOW, 5000);
					break;
				case MESSAGE_RFID_ZERO:
					Debug.e(TAG, "--->zero: play error");
					mHandler.sendEmptyMessage(MESSAGE_RFID_ALARM);
					mHandler.sendEmptyMessageDelayed(MESSAGE_RFID_ZERO, 5000);
					break;
				case MESSAGE_RFID_ALARM:
					Debug.e(TAG, "--->MESSAGE_RFID_ALARM");
					mFlagAlarming = true;
					// GPIO版本的img时，PH7是错误指示灯，SPI版本的img的时候，PI8是错误指示灯。但是apk仍然调用PH7，在img里面根据img的版本进行PH7或者PI8的调整
					ExtGpio.writeGpio('h', 7, 1);

                    ThreadPoolManager.mControlThread.execute(new Runnable() {
						@Override
						public void run() {
							if(!mIsAlarming) {
								mIsAlarming = true;
								ExtGpio.playClick();
								try{Thread.sleep(50);}catch(Exception e){};
								ExtGpio.playClick();
								try{Thread.sleep(50);}catch(Exception e){};
								ExtGpio.playClick();
								mIsAlarming = false;
							}
						}
					});

					break;
				case MESSAGE_RECOVERY_PRINT:
					SharedPreferences preference = mContext.getSharedPreferences(SettingsTabActivity.PREFERENCE_NAME, Context.MODE_PRIVATE);
					boolean pCrash = preference.getBoolean("stat_before_crash", false);
					if (pCrash) {
						ToastUtil.show(mContext, R.string.str_recover_print);
						mHandler.sendEmptyMessageDelayed(MESSAGE_PRINT_START, 2000);
						preference.edit().putBoolean("stat_before_crash", false).commit();
					}
					break;
				case MSG_IMPORT_DONE:
					mProgressDialog.dismiss();
					break;
				case Hp22mmSCManager.MSG_HP22MM_ERROR:
// H.M.Wang 2024-7-10 追加错误信息返回主控制页面显示的功能
					if(null != mHp22mmErrTV) {
						mHp22mmErrTV.setText((String)msg.obj);
					}
// H.M.Wang 2025-1-20 当22mm的初始化失败时，显示提示窗
					if(!((String)msg.obj).isEmpty()) ToastUtil.show(mContext, (String)msg.obj);
// End of H.M.Wang 2025-1-20 当22mm的初始化失败时，显示提示窗
					if(!TextUtils.isEmpty((String)msg.obj)) {
						ExportLog2Usb.writeHp22mmErrLog((String)msg.obj);
						ThreadPoolManager.mControlThread.execute(new Runnable() {
							@Override
							public void run() {
								ExtGpio.playClick();
								try{Thread.sleep(50);}catch(Exception e){};
								ExtGpio.playClick();
								try{Thread.sleep(50);}catch(Exception e){};
								ExtGpio.playClick();
							}
						});
					}
// End of H.M.Wang 2024-7-10 追加错误信息返回主控制页面显示的功能
					break;
// H.M.Wang 2024-12-28 增加两个消息，一个是显示计数器当前值，另外一个是计数器到了边界值报警
				case MSG_SHOW_COUNTER:
					TextView cntTV = (TextView) getView().findViewById(R.id.counterTV);
					cntTV.setText((String)msg.obj);
					break;
				case MSG_ALARM_CNT_EDGE:
					ToastUtil.show(mContext, (String.format(mContext.getResources().getString(R.string.strCounterIndex), msg.arg1) + " " + mContext.getResources().getString(R.string.strCounterClear)));
					ThreadPoolManager.mControlThread.execute(new Runnable() {
						@Override
						public void run() {
						ExtGpio.playClick();
						try{Thread.sleep(50);}catch(Exception e){};
						ExtGpio.playClick();
						try{Thread.sleep(50);}catch(Exception e){};
						ExtGpio.playClick();
						}
					});
					break;
// End of H.M.Wang 2024-12-28 增加两个消息，一个是显示计数器当前值，另外一个是计数器到了边界值报警
				default:
					break;
			}
		}
	};

	private volatile boolean mIsAlarming = false;

	private void handlerSuccess(int toastRs, String pcMsg) {
		ToastUtil.show(mContext, toastRs);
		sendToRemote(Constants.pcOk(pcMsg));
		dismissProgressDialog();
	}
	/**
	 * handle print error message
	 * @param toast
	 * @param pcMsg
	 */
	private void handleError(String toast, String pcMsg) {

		ToastUtil.show(mContext, toast == null ? "failed" : toast);
		sendToRemote(Constants.pcErr(pcMsg));
		dismissProgressDialog();
	}
	/**
	 * handle print error message
	 * @param toastRs
	 * @param pcMsg
	 */
	private void handleError(int toastRs, String pcMsg) {
// H.M.Wang 2023-10-31 暂时停止未指定错误内容的情况下使用str_toast_no_bin 错误
//		if (toastRs == 0) {
//			toastRs = R.string.str_toast_no_bin;
//		}
		if(toastRs != 0) ToastUtil.show(mContext, toastRs);
// End of H.M.Wang 2023-10-31 暂时停止未指定错误内容的情况下使用str_toast_no_bin 错误
		sendToRemote(Constants.pcErr(pcMsg));
		dismissProgressDialog();
	}

	private boolean checkRfid() {
		boolean ready = true;
		if (Configs.READING) {
			return true;
		}
		if (mDTransThread == null) {
			return true;
		}
		//DataTask task = mDTransThread.getData();
		int heads = SystemConfigFile.getInstance(mContext).getPNozzle().mHeads;// task.getHeads();
		for (int i = 0; i < heads; i++) {
			Debug.d(TAG, "Checking Rfid of Head = " + i);
			float ink = mInkManager.getLocalInk(i);
			if (ink <= 0) {
				ready = false;
			}
		}
		return ready;
	}
	
	private boolean checkQRFile() {
		boolean ready = true;
		if (mDTransThread == null) {
			return true;
		}
// H.M.Wang 2020-12-18 reInstance重新生成QRReader，以使得对QR文件的修改生效
//		QRReader reader = QRReader.getInstance(mContext);
		QRReader reader = QRReader.reInstance(mContext);
// H.M.Wang 2020-12-18 reInstance重新生成QRReader，以使得对QR文件的修改生效
		boolean qrReady = reader.isReady();
		Debug.d(TAG, "--->checkQRfile = " + qrReady);
		DataTask task = mDTransThread.getCurData();
		for (BaseObject obj : task.getObjList()) {
			if (!(obj instanceof BarcodeObject)) {
				continue;
			}
			if (!((BarcodeObject) obj).isQRCode() || !obj.getSource()) {
				continue;
			}
			ready = qrReady;
		}
		return ready;
	}

	private void resetCounterIfNeed() {
		List<DataTask> tasks = mDTransThread.getData();
		if (tasks == null) {
			return;
		}
		SystemConfigFile config = SystemConfigFile.getInstance(mContext);
		for (DataTask task : tasks) {
			if (task == null) {
				continue;
			}
			List<BaseObject> objects = task.getObjList();
			for (BaseObject object : objects) {
				if (object instanceof CounterObject) {
					((CounterObject) object).setValue(config.getParam(SystemConfigFile.INDEX_COUNT_1 + ((CounterObject) object).getCounterIndex()));
				}
			}
		}
	}

// H.M.Wang 2021-9-24 注释：由于这个Segment创建的太早，导致屏幕还没有反转就已经画图完毕了，因此首次进入这个函数时屏幕是竖屏的，变为横屏的时候onConfigurationChanged事件被过滤掉了，因此
// 如果把preview的高度设成match_parent，就会获得一个竖屏模式下获取的高度，从而变大

	private void dispPreview(Bitmap bmp) {
// H.M.Wang 2023-2-13 增加一个工作模式，使用外接U盘当中的文件作为DT的数据源来打印。后续使用哪个方法
		if(TxtDT.getInstance(mContext).isTxtDT()) {
			TxtDT.getInstance(mContext).dispPreview();
			return;
		}
// End of H.M.Wang 2023-2-13 增加一个工作模式，使用外接U盘当中的文件作为DT的数据源来打印
// H.M.Wang 2021-6-1 修改预览页面显示方法。保修切割图片，延迟显示方法
//		int x=0,y=0;
//		int cutWidth = 0;
//		float scale = 1;
		if (bmp == null) {
			Debug.e(TAG, "--->dispPreview error: bmp is NULL!!!");
			return;
		}
// H.M.Wang 2022-5-10 取消直接在线程当中使用final Bitmap bmp，改为使用成员变量。可能是因为这个参数传递导致时间长了bitmap被回收
		mPreBitmap = bmp;
// End of H.M.Wang 2022-5-10 取消直接在线程当中使用final Bitmap bmp，改为使用成员变量。可能是因为这个参数传递导致时间长了bitmap被回收
//		String product = SystemPropertiesProxy.get(mContext, "ro.product.name");
//		DisplayMetrics dm = new DisplayMetrics();
//		getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
//		Debug.d(TAG, "--->screen width: " + dm.widthPixels + " height: " + dm.heightPixels + "  dpi= " + dm.densityDpi);

//		float height = mllPreview.getHeight();
//		scale = (height/bmp.getHeight());
//		mllPreview.removeAllViews();

// H.M.Wang 2021-5-21 修改预览页面显示方法
		mllPreview.postDelayed(new Runnable() {
			@Override
			public void run() {
				int x=0,y=0;
				int cutWidth = 0;
				float scale = 1;
				float height = mllPreview.getHeight();
				scale = (height/mPreBitmap.getHeight());
				mllPreview.removeAllViews();

//				ImageView imgView = new ImageView(mContext);
//				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams((int)(1.0f * bmp.getWidth()/bmp.getHeight() * mllPreview.getHeight()), mllPreview.getHeight());
//				params.gravity = Gravity.LEFT;
//				imgView.setLayoutParams(params);
//				imgView.setScaleType(ScaleType.FIT_XY);
//				imgView.setBackgroundColor(Color.WHITE);
//				imgView.setImageBitmap(bmp);
//				mllPreview.addView(imgView);


				for (int i = 0;x < mPreBitmap.getWidth(); i++) {
					if (x + 1200 > mPreBitmap.getWidth()) {
						cutWidth = mPreBitmap.getWidth() - x;
					} else {
						cutWidth =1200;
					}
					Bitmap child = Bitmap.createBitmap(mPreBitmap, x, 0, cutWidth, mPreBitmap.getHeight());
					if (cutWidth * scale < 1 || mPreBitmap.getHeight() * scale < 1) {
						child.recycle();
						break;
					}
					Debug.d(TAG, "-->child: [" + child.getWidth() + ", " + child.getHeight() + "]; view h: " + mllPreview.getHeight() + "]; orientation: " + mContext.getResources().getConfiguration().orientation);
					Bitmap scaledChild = Bitmap.createScaledBitmap(child, (int) (cutWidth*scale), (int) (mPreBitmap.getHeight() * scale), true);
					//child.recycle();
					//Debug.d(TAG, "--->scaledChild  width = " + child.getWidth() + " scale= " + scale);
					x += cutWidth;
					ImageView imgView = new ImageView(mContext);
					imgView.setScaleType(ScaleType.FIT_XY);
//				if (density == 1) {
					imgView.setLayoutParams(new LayoutParams(scaledChild.getWidth(),scaledChild.getHeight()));
//				} else {
//					imgView.setLayoutParams(new LayoutParams(cutWidth,LayoutParams.MATCH_PARENT));
//				}

					imgView.setBackgroundColor(Color.WHITE);
					imgView.setImageBitmap(scaledChild);
					mllPreview.addView(imgView);
					// scaledChild.recycle();
				}
// H.M.Wang 2023-7-6 加上这个步骤，使得打印内容很少的时候，将实际内容区的宽度也能扩充到满屏，以实现整个区域对点击和长按时间作出反应
				if(mllPreview.getWidth() < mScrollView.getWidth()) {
					mllPreview.setMinimumWidth(mScrollView.getWidth());
// End of H.M.Wang 2023-7-6 加上这个步骤，使得打印内容很少的时候，将实际内容区的宽度也能扩充到满屏，以实现整个区域对点击和长按时间作出反应
				}
			}
		}, 10);
// End of H.M.Wang 2021-5-21 修改预览页面显示方法
// End of H.M.Wang 2021-6-1 修改预览页面显示方法。保修切割图片，延迟显示方法
	}
	
	private int mRfiAlarmTimes = 0;
	private boolean mRfidInit = false;
	
	/**
	 * Counter & dynamic QR objects need a roll-back operation after each print-stop
	 * because these dynamic objects generate the next value after each single print finished;
	 * then, if stop printing at that time these values will step forward by "1" to the real value;
	 * a mistake will happen at the next continue printing
	 * Deprecated: move to DataTransferThread to do this  
	 */
/*	@Deprecated
 H.M.Wang 2020-7-2 取消未使用函数
	private void rollback() {
		if (mMsgTask == null) {
			return;
		}
		int index = mDTransThread.index();
		MessageTask task = mMsgTask.get(index);
		for (BaseObject object : task.getObjects()) {
			if (object instanceof CounterObject) {
				((CounterObject) object).rollback();
			}
		}
	}
*/
	private void updateCntIfNeed() {
		int index = mDTransThread.index();
		if (index >= mMsgTask.size()) return;
		MessageTask task = mMsgTask.get(index);

		for (BaseObject object : task.getObjects()) {
			if (object instanceof CounterObject) {
				Message msg = new Message();
				msg.what = MainActivity.UPDATE_COUNTER;
				try {
					msg.arg1 = Integer.valueOf(((CounterObject) object).getContent());
				} catch (Exception e) {
					break;
				}
				mCallback.sendMessage(msg);
				break;
			}
		}
	}
	
	public void initDTThread() {
		
		if (mMsgTask == null) {
			return;
		}
		if (mDTransThread == null) {
			mDTransThread = DataTransferThread.getInstance(mContext);
		}

		mDTransThread.setCallback(this);
		Debug.d(TAG, "--->init");

		// 鍒濆鍖朾uffer
		mDTransThread.initDataBuffer(mContext, mMsgTask);

		// 璁剧疆dot count
		mDTransThread.setDotCount(mMsgTask);
		// 璁剧疆UI鍥炶皟
		mDTransThread.setOnInkChangeListener(this);

// H.M.Wang 2021-4-11 追加检查任务的分辨率和设备的分辨率是否一致，不一致则停止打印
		for(MessageTask msgTask: mMsgTask) {
			if(msgTask.getMsgObject().getPrintDpi() != Configs.GetDpiVersion()) {
				ToastUtil.show(mContext, R.string.printDpiNotMatchError);
//				mHandler.sendEmptyMessage(MESSAGE_PRINT_STOP);
				return;
			}
		}
// End of H.M.Wang 2021-4-11 追加检查任务的分辨率和设备的分辨率是否一致，不一致则停止打印
	}
	
	private final int STATE_PRINTING = 0;
	private final int STATE_STOPPED = 1;
	
	public void switchState(int state) {
		Debug.d(TAG, "--->switchState=" + state);
		switch(state) {
			case STATE_PRINTING:
				mBtnStart.setClickable(false);
				mTvStart.setTextColor(Color.GRAY);
				mBtnStop.setClickable(true);
				mTvStop.setTextColor(Color.BLACK);
				mBtnOpenfile.setClickable(false);
				mTvOpen.setTextColor(Color.GRAY);
				mTVPrinting.setVisibility(View.VISIBLE);
// H.M.Wang 2023-6-27 增加一个用户定义界面模式，增加该界面当中的特殊变量
				if(Configs.UI_TYPE == Configs.UI_CUSTOMIZED0) {
					mBtnImport.setClickable(false);
					mTvImport.setTextColor(Color.GRAY);
					if(mPrintType == PRINT_TYPE_UPWARD_CNT) {
						mTVPrinting.setVisibility(View.GONE);
						mTVCntDownPrinting.setVisibility(View.GONE);
						mTVCntUpPrinting.setVisibility(View.VISIBLE);
					}
					if(mPrintType == PRINT_TYPE_DWWARD_CNT) {
						mTVPrinting.setVisibility(View.GONE);
						mTVCntUpPrinting.setVisibility(View.GONE);
						mTVCntDownPrinting.setVisibility(View.VISIBLE);
					}
					mUpCntPrint.setClickable(false);
					mUpCntPrint.setTextColor(Color.GRAY);
					mDnCntPrint.setClickable(false);
					mDnCntPrint.setTextColor(Color.GRAY);
				}
// End of H.M.Wang 2023-6-27 增加一个用户定义界面模式，增加该界面当中的特殊变量
				mTVStopped.setVisibility(View.GONE);
// 2020-7-21 取消打印状态下清洗按钮无效的设置
//				mBtnClean.setEnabled(false);
//				mTvClean.setTextColor(Color.GRAY);
// End of 2020-7-21 取消打印状态下清洗按钮无效的设置

				// mMsgNext.setClickable(false);
				// mMsgPrev.setClickable(false);
// H.M.Wang 2020-9-15 如果不是工作在Smart卡模式，则继续使用该管脚作为打印信号使用
//				if(!(mInkManager instanceof SmartCardManager)) {
					ExtGpio.writeGpio('b', 11, 1);
//				}
// End of H.M.Wang 2020-9-15 如果不是工作在Smart卡模式，则继续使用该管脚作为打印信号使用
				break;
			case STATE_STOPPED:
				mBtnStart.setClickable(true);
				mTvStart.setTextColor(Color.BLACK);
				mBtnStop.setClickable(false);
				mTvStop.setTextColor(Color.GRAY);
				mBtnOpenfile.setClickable(true);
				mTvOpen.setTextColor(Color.BLACK);
				mTVPrinting.setVisibility(View.GONE);
// H.M.Wang 2023-6-27 增加一个用户定义界面模式，增加该界面当中的特殊变量
				if(Configs.UI_TYPE == Configs.UI_CUSTOMIZED0) {
					mBtnImport.setClickable(true);
					mTvImport.setTextColor(Color.BLACK);
					mTVCntUpPrinting.setVisibility(View.GONE);
					mTVCntDownPrinting.setVisibility(View.GONE);
				}
// End of H.M.Wang 2023-6-27 增加一个用户定义界面模式，增加该界面当中的特殊变量
				mTVStopped.setVisibility(View.VISIBLE);
// 2020-7-21 取消打印状态下清洗按钮无效的设置
//				mBtnClean.setEnabled(true);
//				mTvClean.setTextColor(Color.BLACK);
// End of 2020-7-21 取消打印状态下清洗按钮无效的设置

				// mMsgNext.setClickable(true);
				// mMsgPrev.setClickable(true);
// H.M.Wang 2020-9-15 如果不是工作在Smart卡模式，则继续使用该管脚作为打印信号使用
//				if(!(mInkManager instanceof SmartCardManager)) {
					ExtGpio.writeGpio('b', 11, 0);
//				}
// End of H.M.Wang 2020-9-15 如果不是工作在Smart卡模式，则继续使用该管脚作为打印信号使用
				break;
			default:
				Debug.d(TAG, "--->unknown state");
		}
	}
	
/*
	public void startPreview()
	{
		Debug.d(TAG, "===>startPreview");
		
		try{
			mPreviewBuffer = Arrays.copyOf(mPrintBuffer, mPrintBuffer.length);
			BinInfo.Matrix880Revert(mPreviewBuffer);
			mPreBytes = new int[mPreviewBuffer.length*8];
			// BinCreater.bin2byte(mPreviewBuffer, mPreBytes);
			mPreview.createBitmap(mPreBytes, mBgBuffer.length/110, Configs.gDots);
			mPreview.invalidate();
			
			//mPreviewRefreshHandler.sendEmptyMessage(0);
		}catch(Exception e)
		{
			e.printStackTrace();
		}
		
	}
*/
	public void onCheckUsbSerial()
	{
		mSerialdev = null;
		File file = new File("/dev");
		if(file.listFiles() == null)
		{
			return ;
		}
		File[] files = file.listFiles(new PrinterFileFilter("ttyACM"));
		for(File f : files)
		{
			if(f == null)
			{
				break;
			}
			Debug.d(TAG, "file = "+f.getName());
			int fd = UsbSerial.open("/dev/"+f.getName());
			Debug.d(TAG, "open /dev/"+f.getName()+" return "+fd);
			if(fd < 0)
			{
				Debug.d(TAG, "open usbserial /dev/"+f.getName()+" fail");
				continue;
			}
			UsbSerial.close(fd);
			mSerialdev = "/dev/"+f.getName();
			break;
		}
	}

	public class SerialEventReceiver extends BroadcastReceiver
	{

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			Debug.d(TAG, "******intent="+intent.getAction());
			if(ACTION_REOPEN_SERIAL.equals(intent.getAction()))
			{
				onCheckUsbSerial();
			}
			else if(ACTION_CLOSE_SERIAL.equals(intent.getAction()))
			{
				Debug.d(TAG, "******close");
				mSerialdev = null;
			}
			else if(ACTION_BOOT_COMPLETE.equals(intent.getAction()))
			{
				onCheckUsbSerial();
				byte info[] = new byte[23];
				UsbSerial.printStart(mSerialdev);
				UsbSerial.getInfo(mSerialdev, info);
				//updateInkLevel(info);
				UsbSerial.printStop(mSerialdev);
			}
		}
		
	}
	
	
	public int calculateBufsize(Vector<TlkObject> list)
	{
		int length=0;
		for(int i=0; i<list.size(); i++)
		{
			Debug.d(TAG,"calculateBufsize list i="+i);
			TlkObject o = list.get(i);
			if(o.isTextObject())	//each text object take over 16*16/8 * length=32Bytes*length
			{
				Debug.d(TAG,"content="+o.mContent);
				DotMatrixFont font = new DotMatrixFont(DotMatrixFont.FONT_FILE_PATH+o.font+".txt");
				int l = font.getColumns();				
				length = (l*o.mContent.length()+o.x) > length?(l*o.mContent.length()+o.x):length;
			}
			else if(o.isPicObject()) //each picture object take over 32*32/8=128bytes
			{
				length = (o.x+128) > length?(o.x+128):length;
			}
		}
		return length;
	}
	
	/*
	 * make set param buffer
	 * 1.  Byte 2-3,param 00,	reserved
	 * 2.  Byte 4-5,param 01,	print speed, unit HZ,43kHZ for highest
	 * 3.  Byte 6-7, param 02,	delay,unit: 0.1mmm
	 * 13. Byte 8-9, param 03,	reserved
	 * 14. Byte 10-11, param 04,triger 00 00  on, 00 01 off
	 * 15. Byte 12-13, param 05,sync  00 00  on, 00 01 off
	 * 16. Byte 14-15, param 06
	 * 17. Byte 16-17, param 07, length, unit 0.1mm
	 * 18. Byte 18-19, param 08, timer, unit ms
	 * 19. Byte 20-21, param 09, print head Temperature
	 * 20. Byte 20-21, param 10,  Ink cartridge Temperature
	 * 21. others reserved  
	 */
	public static void makeParams(Context context, byte[] params)
	{
		if(params==null || params.length<128)
		{
			Debug.d(TAG,"params is null or less than 128, realloc it");
			params = new byte[128];
		}
		SharedPreferences preference = context.getSharedPreferences(SettingsTabActivity.PREFERENCE_NAME, 0);
		int speed = preference.getInt(SettingsTabActivity.PREF_PRINTSPEED, 0);
		params[2] = (byte) ((speed>>8)&0xff);
		params[3] = (byte) ((speed)&0xff);
		int delay = preference.getInt(SettingsTabActivity.PREF_DELAY, 0);
		params[4] = (byte) ((delay>>8)&0xff);
		params[5] = (byte) ((delay)&0xff);
		int triger = (int)preference.getLong(SettingsTabActivity.PREF_TRIGER, 0);
		params[8] = 0x00;
		params[9] = (byte) (triger==0?0x00:0x01);
		int encoder = (int)preference.getLong(SettingsTabActivity.PREF_ENCODER, 0);
		params[10] = 0x00;
		params[11] = (byte) (encoder==0?0x00:0x01);
		int bold = preference.getInt(SettingsTabActivity.PREF_BOLD, 0);
		params[12] = (byte) ((bold>>8)&0xff);
		params[13] = (byte) ((bold)&0xff);
		int fixlen = preference.getInt(SettingsTabActivity.PREF_FIX_LEN, 0);
		params[14] = (byte) ((fixlen>>8)&0xff);
		params[15] = (byte) ((fixlen)&0xff);
		int fixtime= preference.getInt(SettingsTabActivity.PREF_FIX_TIME, 0);
		params[16] = (byte) ((fixtime>>8)&0xff);
		params[17] = (byte) ((fixtime)&0xff);
		int headtemp = preference.getInt(SettingsTabActivity.PREF_HEAD_TEMP, 0);
		params[18] = (byte) ((headtemp>>8)&0xff);
		params[19] = (byte) ((headtemp)&0xff);
		int resvtemp = preference.getInt(SettingsTabActivity.PREF_RESV_TEMP, 0);
		params[20] = (byte) ((resvtemp>>8)&0xff);
		params[21] = (byte) ((resvtemp)&0xff);
		int fontwidth = preference.getInt(SettingsTabActivity.PREF_FONT_WIDTH, 0);
		params[22] = (byte) ((fontwidth>>8)&0xff);
		params[23] = (byte) ((fontwidth)&0xff);
		int dots = preference.getInt(SettingsTabActivity.PREF_DOT_NUMBER, 0);
		params[24] = (byte) ((dots>>8)&0xff);
		params[25] = (byte) ((dots)&0xff);
		int resv12 = preference.getInt(SettingsTabActivity.PREF_RESERVED_12, 0);
		params[26] = (byte) ((resv12>>8)&0xff);
		params[27] = (byte) ((resv12)&0xff);
		int resv13 = preference.getInt(SettingsTabActivity.PREF_RESERVED_13, 0);
		params[28] = (byte) ((resv13>>8)&0xff);
		params[29] = (byte) ((resv13)&0xff);
		int resv14 = preference.getInt(SettingsTabActivity.PREF_RESERVED_14, 0);
		params[30] = (byte) ((resv14>>8)&0xff);
		params[31] = (byte) ((resv14)&0xff);
		int resv15 = preference.getInt(SettingsTabActivity.PREF_RESERVED_15, 0);
		params[32] = (byte) ((resv15>>8)&0xff);
		params[33] = (byte) ((resv15)&0xff);
		int resv16 = preference.getInt(SettingsTabActivity.PREF_RESERVED_16, 0);
		params[34] = (byte) ((resv16>>8)&0xff);
		params[35] = (byte) ((resv16)&0xff);
		int resv17 = preference.getInt(SettingsTabActivity.PREF_RESERVED_17, 0);
		params[36] = (byte) ((resv17>>8)&0xff);
		params[37] = (byte) ((resv17)&0xff);
		int resv18 = preference.getInt(SettingsTabActivity.PREF_RESERVED_18, 0);
		params[38] = (byte) ((resv18>>8)&0xff);
		params[39] = (byte) ((resv18)&0xff);
		int resv19 = preference.getInt(SettingsTabActivity.PREF_RESERVED_19, 0);
		params[40] = (byte) ((resv19>>8)&0xff);
		params[41] = (byte) ((resv19)&0xff);
		int resv20 = preference.getInt(SettingsTabActivity.PREF_RESERVED_20, 0);
		params[42] = (byte) ((resv20>>8)&0xff);
		params[43] = (byte) ((resv20)&0xff);
		int resv21 = preference.getInt(SettingsTabActivity.PREF_RESERVED_21, 0);
		params[44] = (byte) ((resv21>>8)&0xff);
		params[45] = (byte) ((resv21)&0xff);
		int resv22 = preference.getInt(SettingsTabActivity.PREF_RESERVED_22, 0);
		params[46] = (byte) ((resv22>>8)&0xff);
		params[47] = (byte) ((resv22)&0xff);
		int resv23 = preference.getInt(SettingsTabActivity.PREF_RESERVED_23, 0);
		params[48] = (byte) ((resv23>>8)&0xff);
		params[49] = (byte) ((resv23)&0xff);
		
	}
	
	/**
	 * the loading dialog
	 */
	public LoadingDialog mLoadingDialog;
	public Thread mProgressThread;
	public boolean mProgressShowing;
	public void progressDialog()
	{
		SharedPreferences p = mContext.getSharedPreferences(SettingsTabActivity.PREFERENCE_NAME, Context.MODE_PRIVATE);
		p.edit().putBoolean(PreferenceConstants.LOADING_BEFORE_CRASH, true).commit();
		if (mProgressShowing || (mLoadingDialog != null && mLoadingDialog.isShowing())) {
			return;
		}
		mLoadingDialog = LoadingDialog.show(mContext, R.string.strLoading);
		Debug.d(TAG, "===>show loading");
		mProgressShowing = true;
		mProgressThread = new Thread(){
			
			@Override
			public void run(){
				
				try{
					for(;mProgressShowing==true;)
					{
						Thread.sleep(2000);
					}
					Debug.d(TAG, "===>dismiss loading");
					mHandler.sendEmptyMessage(MESSAGE_DISMISS_DIALOG);
				}catch(Exception e)
				{
					
				}
			}
		};
		mProgressThread.start();
	}
	
	public void dismissProgressDialog()
	{
		mProgressShowing=false;
		SharedPreferences p = mContext.getSharedPreferences(SettingsTabActivity.PREFERENCE_NAME, Context.MODE_PRIVATE);
		p.edit().putBoolean(PreferenceConstants.LOADING_BEFORE_CRASH, false).commit();
		Debug.d(TAG, "===>dismissProgressDialog");
	}

	public int currentRfid = 0;
	@Override
	public void onClick(View v) {
		// ExtGpio.playClick();
		switch (v.getId()) {
			case R.id.StartPrint:
// H.M.Wang 2020-9-15 追加在条件满足的情况下，启动写入Smart卡验证码工作模式
/* 2021-1-6 暂时取消				if(PlatformInfo.DEVICE_SMARTCARD.equals(PlatformInfo.getInkDevice()) &&	Configs.SMARTCARDMANAGER) {
					int ret = SmartCard.init();
					if(SmartCard.SC_SUCCESS == ret) {
						if(SmartCard.SC_SUCCESS == SmartCard.writeCheckSum(SmartCardManager.WORK_BULK, mSysconfig.getParam(0))) {
							ToastUtil.show(mContext, "Done.");
						} else {
							ToastUtil.show(mContext, "Failed.");
						}
						SmartCard.shutdown();
					} else {
						ToastUtil.show(mContext, "Failed.");
					}
					break;
				}
*/
// End of H.M.Wang 2020-9-15 追加在条件满足的情况下，启动写入Smart卡验证码工作模式

// H.M.Wang 2020-8-21 追加正在清洗标志，此标志为ON的时候不能对FPGA进行某些操作，如开始，停止等，否则死机
				DataTransferThread thread = DataTransferThread.getInstance(mContext);
				if(thread.isPurging) {
					ToastUtil.show(mContext, R.string.str_under_purging);
					break;
				}
// End of H.M.Wang 2020-8-21 追加正在清洗标志，此标志为ON的时候不能对FPGA进行某些操作，如开始，停止等，否则死机
//	死机			mInkManager.checkRfid();
//	拔出墨盒仍然返回有效数值56643			mInkManager.getLocalInk(0);
// H.M.Wang 2023-6-27 增加一个用户定义界面模式，响应“向上连续打印”和“向下连续打印”
				if(Configs.UI_TYPE == Configs.UI_CUSTOMIZED0) {
					mPrintType = PRINT_TYPE_NORMAL;
				}
// End of H.M.Wang 2023-6-27 增加一个用户定义界面模式，响应“向上连续打印”和“向下连续打印”
// H.M.Wang 2022-1-12 延时1秒下发打印命令
				mHandler.sendEmptyMessageDelayed(MESSAGE_OPEN_TLKFILE, 1000);
// End of H.M.Wang 2022-1-12 延时1秒下发打印命令
				if(PlatformInfo.isA133Product()) SystemFs.writeSysfs("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor", "performance");
				break;
			case R.id.StopPrint:
// H.M.Wang 2020-8-21 追加正在清洗标志，此标志为ON的时候不能对FPGA进行某些操作，如开始，停止等，否则死机
				thread = DataTransferThread.getInstance(mContext);
				if(thread.isPurging) {
					ToastUtil.show(mContext, R.string.str_under_purging);
					break;
				}
// End of H.M.Wang 2020-8-21 追加正在清洗标志，此标志为ON的时候不能对FPGA进行某些操作，如开始，停止等，否则死机
				// mHandler.removeMessages(MESSAGE_PAOMADENG_TEST);
				mHandler.sendEmptyMessage(MESSAGE_PRINT_STOP);
				if(PlatformInfo.isA133Product()) SystemFs.writeSysfs("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor", "schedutil");
				break;
			/*娓呮礂鎵撳嵃澶达紙涓�涓壒娈婄殑鎵撳嵃浠诲姟锛夛紝闇�瑕佸崟鐙殑璁剧疆锛氬弬鏁�2蹇呴』涓� 4锛屽弬鏁�4涓�200锛� 鍙傛暟5涓�20锛�*/
			case R.id.btnFlush:
// H.M.Wang 2020-8-21 追加正在清洗标志，此标志为ON的时候不能对FPGA进行某些操作，如开始，停止等，否则死机
				thread = DataTransferThread.getInstance(mContext);
				if(thread.isPurging) {
					ToastUtil.show(mContext, R.string.str_under_purging);
					break;
				}
// End of H.M.Wang 2020-8-21 追加正在清洗标志，此标志为ON的时候不能对FPGA进行某些操作，如开始，停止等，否则死机

// H.M.Wang 2020-8-21 追加点按清洗按键以后提供确认对话窗
				AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
				builder.setTitle(R.string.str_btn_clean)
						.setMessage(R.string.str_purge_confirm)
						.setPositiveButton(R.string.str_ok, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								DataTransferThread thread = DataTransferThread.getInstance(mContext);
								thread.purge(mContext);
								dialog.dismiss();
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
// End of H.M.Wang 2020-8-21 追加点按清洗按键以后提供确认对话窗
				break;
			case R.id.btnBinfile:
                MessageBrowserDialog dialog = new MessageBrowserDialog(mContext, OpenFrom.OPEN_PRINT, mObjPath);
				dialog.setOnPositiveClickedListener(new OnPositiveListener() {
					
					@Override
					public void onClick() {
						ArrayList<String> f = MessageBrowserDialog.getSelected();
						if (f==null || f.size() == 0) {
							return;
						}
						/** 如果选择内容为多个，表示需要新建组 */
						Message msg = mHandler.obtainMessage(MESSAGE_OPEN_PREVIEW);
						Bundle bundle = new Bundle();
						if (f.size() > 1) {
							msg = mHandler.obtainMessage(MESSAGE_OPEN_GROUP);
							bundle.putStringArrayList("file", f);
						} else {
							bundle.putString("file", f.get(0));
						}
// H.M.Wang 2022-3-1 正在打印中的时候切换打印信息，重新生成打印缓冲区
						if (mDTransThread != null && mDTransThread.isRunning()) {
							bundle.putBoolean("printNext", true);
						}
// End of H.M.Wang 2022-3-1 正在打印中的时候切换打印信息，重新生成打印缓冲区
						// bundle.putString("file", f);
						msg.setData(bundle);
						mHandler.sendMessage(msg);

					}

					@Override
					public void onClick(String content) {
						// TODO Auto-generated method stub
						
					}
					
				});
				dialog.show();
				break;
			case R.id.btn_page_forward:
				mScrollView.smoothScrollBy(-400, 0);
				break;
			case R.id.btn_page_backward:
				mScrollView.smoothScrollBy(400, 0);
				break;
			case R.id.ctrl_btn_up:
				ConfirmDialog dlg = new ConfirmDialog(mContext, R.string.message_confirm_printnext);
				dlg.setListener(new DialogListener() {
					@Override
					public void onConfirm() {
						super.onConfirm();
						loadMessage(false);
					}

				});
				dlg.show();

				break;
			case R.id.ctrl_btn_down:
				ConfirmDialog dlg1 = new ConfirmDialog(mContext, R.string.message_confirm_printnext);
				dlg1.setListener(new DialogListener() {
					@Override
					public void onConfirm() {
						super.onConfirm();
						loadMessage(true);
					}

				});
				dlg1.show();
				break;
// H.M.Wang 2023-6-27 增加一个用户定义界面模式，响应“向上连续打印”和“向下连续打印”
			case R.id.upward_cnt_print:
			case R.id.downward_cnt_print:
				thread = DataTransferThread.getInstance(mContext);
				if(thread.isPurging) {
					ToastUtil.show(mContext, R.string.str_under_purging);
					break;
				}
				if(v.getId() == R.id.upward_cnt_print) {
					mPrintType = PRINT_TYPE_UPWARD_CNT;
				} else {
					mPrintType = PRINT_TYPE_DWWARD_CNT;
				}
				mHandler.sendEmptyMessageDelayed(MESSAGE_OPEN_TLKFILE, 1000);
				break;
			case R.id.btnTransfer:
				((MainActivity) getActivity()).showImportDialog();
				break;
// End of H.M.Wang 2023-6-27 增加一个用户定义界面模式，响应“向上连续打印”和“向下连续打印”
			default:
				break;
		}
		
	}

	private void loadMessage(boolean forward) {
		String msg = null;

		if (forward) {
// H.M.Wang 2022-11-29 追加UG类型的信息，在点击上下按键时的内部浏览行为，按下键，从当前的子信息开始向下浏览，到最后一个后退出该信息，进入下一个信息
//			msg = loadNextMsg();
			if(mObjPath.startsWith(Configs.USER_GROUP_PREFIX)) {
				mUGSubIndex++;
				if(mUGSubIndex < mUGSubObjs.size()) {
					msg = mObjPath;
				} else {
					msg = loadNextMsg();
				}
			} else {
				msg = loadNextMsg();
			}
// End of H.M.Wang 2022-11-29 追加UG类型的信息，在点击上下按键时的内部浏览行为，按下键，从当前的子信息开始向下浏览，到最后一个后退出该信息，进入下一个信息
		} else {
// H.M.Wang 2022-11-29 追加UG类型的信息，在点击上下按键时的内部浏览行为，按上键，从当前的子信息开始向上浏览，到第一个后退出该信息，进入下一个信息
//			msg = loadPrevMsg();
			if(mObjPath.startsWith(Configs.USER_GROUP_PREFIX)) {
				mUGSubIndex--;
				if(mUGSubIndex >= 0) {
					msg = mObjPath;
				} else {
					mUGSubIndex = -1;
					msg = loadPrevMsg();
				}
			} else {
// H.M.Wang 2023-3-25 修改简单的函数调用错误
//				msg = loadNextMsg();
				msg = loadPrevMsg();
// End of H.M.Wang 2023-3-25 修改简单的函数调用错误
			}
// End of H.M.Wang 2022-11-29 追加UG类型的信息，在点击上下按键时的内部浏览行为，按上键，从当前的子信息开始向上浏览，到第一个后退出该信息，进入下一个信息
		}

		Message message = mHandler.obtainMessage(MESSAGE_OPEN_PREVIEW);
		Bundle bundle = new Bundle();
		bundle.putString("file", msg);
		if (mDTransThread != null && mDTransThread.isRunning()) {
			bundle.putBoolean("printNext", true);
		}
		message.setData(bundle);

		mHandler.sendMessageDelayed(message, 100);
	}

	private String loadNextMsg() {
		File msgDir = new File(Configs.TLK_PATH_FLASH);
		String[] tlks = msgDir.list();
		Arrays.sort(tlks, new Comparator<String>() {
			@Override
			public int compare(String s, String t1) {
				try {
// H.M.Wang 2023-3-27 修改排序算法，原来的排序算法如果名称不是数字则不会排序，并且与Open对话窗的排序不一致，全部修改为一致
/*					if (s.startsWith(Configs.GROUP_PREFIX) && t1.startsWith(Configs.GROUP_PREFIX)) {
						String g1 = s.substring(Configs.GROUP_PREFIX.length());
						String g2 = s.substring(Configs.GROUP_PREFIX.length());
						int gi1 = Integer.parseInt(g1);
						int gi2 = Integer.parseInt(g2);
						if (gi1 > gi2) {
							return 1;
						} else if (gi1 < gi2) {
							return -1;
						} else {
							return 0;
						}
					} else if (s.startsWith(Configs.GROUP_PREFIX)) {
						return -1;
					} else if (t1.startsWith(Configs.GROUP_PREFIX)) {
						return 1;
					} else {
						int gi1 = Integer.parseInt(s);
						int gi2 = Integer.parseInt(t1);
						if (gi1 > gi2) {
							return 1;
						} else if (gi1 < gi2) {
							return -1;
						} else {
							return 0;
						}
					}
 */
					if(s.length() < t1.length()) {
						return -1;
					} else if(s.length() > t1.length()) {
						return 1;
					} else {
						return s.compareTo(t1);
					}
// End of H.M.Wang 2023-3-27 修改排序算法，原来的排序算法如果名称不是数字则不会排序，并且与Open对话窗的排序不一致，全部修改为一致
				} catch (Exception e) {
					return 0;
				}
			}
		});

		for (int i = 0; i < tlks.length; i++) {
			String tlk = tlks[i];
			if (tlk.equalsIgnoreCase(mObjPath)) {
				if (i + 1 < tlks.length ) {
					return tlks[i + 1];
				} else {
					return tlks[0];
				}
			}
		}
		return null;
	}

	private String loadPrevMsg() {
		File msgDir = new File(Configs.TLK_PATH_FLASH);
		String[] tlks = msgDir.list();
		Arrays.sort(tlks, new Comparator<String>() {
			@Override
			public int compare(String s, String t1) {

				try {
// H.M.Wang 2023-3-27 修改排序算法，原来的排序算法如果名称不是数字则不会排序，并且与Open对话窗的排序不一致，全部修改为一致
/*					if (s.startsWith(Configs.GROUP_PREFIX) && t1.startsWith(Configs.GROUP_PREFIX)) {
						String g1 = s.substring(Configs.GROUP_PREFIX.length());
						String g2 = s.substring(Configs.GROUP_PREFIX.length());
						int gi1 = Integer.parseInt(g1);
						int gi2 = Integer.parseInt(g2);
						if (gi1 > gi2) {
							return 1;
						} else if (gi1 < gi2) {
							return -1;
						} else {
							return 0;
						}
					} else if (s.startsWith(Configs.GROUP_PREFIX)) {
						return -1;
					} else if (t1.startsWith(Configs.GROUP_PREFIX)) {
						return 1;
					} else {
						int gi1 = Integer.parseInt(s);
						int gi2 = Integer.parseInt(t1);
						if (gi1 > gi2) {
							return 1;
						} else if (gi1 < gi2) {
							return -1;
						} else {
							return 0;
						}
					}
*/
					if(s.length() < t1.length()) {
						return -1;
					} else if(s.length() > t1.length()) {
						return 1;
					} else {
						return s.compareTo(t1);
					}
// End of H.M.Wang 2023-3-27 修改排序算法，原来的排序算法如果名称不是数字则不会排序，并且与Open对话窗的排序不一致，全部修改为一致
				} catch (Exception e) {
					return 0;
				}
			}
		});

		for (int i = 0; i < tlks.length; i++) {
			String tlk = tlks[i];
			if (tlk.equalsIgnoreCase(mObjPath)) {
				if (i > 0 ) {
					return tlks[i - 1];
				} else {
					return tlks[tlks.length - 1];
				}
			}
		}
		return null;
	}

	@Override
	public void onInkLevelDown(int device) {
		Message message = mHandler.obtainMessage();
		message.arg1 = device;
		message.what = MESSAGE_INKLEVEL_CHANGE;
		mHandler.sendMessage(message);
	}

	@Override
	public void onInkEmpty() {
		
	}

	@Override
	public void onCountChanged() {
// H.M.Wang 2023-10-20 调整计数器的数值，从每次下发后加1（记忆的是下发的总次数），改为实际打印的总次数。这个在使用img的FIFO的时候，是有区别的，相差最大FIFO的个数
//		mCounter++;
		if(null != mDTransThread) mCounter += mDTransThread.getRecentPrintedCount();
// End of H.M.Wang 2023-10-20 调整计数器的数值，从每次下发后加1（记忆的是下发的总次数），改为实际打印的总次数。这个在使用img的FIFO的时候，是有区别的，相差最大FIFO的个数

		mHandler.removeMessages(MESSAGE_COUNT_CHANGE);
		mHandler.sendEmptyMessage(MESSAGE_COUNT_CHANGE);
	}

	@Override
	public boolean onTouch(View view, MotionEvent event) {
		switch(view.getId()) {
			case R.id.StartPrint:
			case R.id.StopPrint:
			case R.id.btnFlush:
			case R.id.btnBinfile:
			case R.id.btn_page_forward:
			case R.id.btn_page_backward:
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
//					PWMAudio.Play();
				}
			default:
				break;
		}
		return false;
	}

	public boolean isAlarming() {
		return mFlagAlarming;
	}

	public boolean isPrinting() {
		if (mDTransThread != null) {
			return mDTransThread.isRunning();
		}
		return false;
	}
	
	
	public void setCallback(Handler callback) {
		mCallback = callback;
	}
	
	public void onConfigChange() {
		if (mDTransThread == null || mDTransThread.isRunning()) {
			return;
		}
		mDTransThread.initCount();
		refreshCount();
	}
	
	@Override
	public void onFinished(int code) {
		Debug.d(TAG, "--->onFinished");
// H.M.Wang 2024-6-15 再次取消强制停止打印，恢复为2022-4-8的状态，只是为了避免频繁报警和显示提示，增加一个1秒钟的时间间隔，该时间间隔内静默
// H.M.Wang 2022-4-8 当QR_R.csv文件全部打印完成时，取消停止打印，因为取消太快的话，打印内容可能被切掉，改为报警
// H.M.Wang 2024-2-28 恢复打印完成后停止打印的功能，但是延时1s停止
//		mHandler.sendEmptyMessageDelayed(MESSAGE_PRINT_STOP, 1000);
// End of H.M.Wang 2024-2-28 恢复打印完成后停止打印的功能，但是延时1s停止
		ThreadPoolManager.mControlThread.execute(new Runnable() {
			@Override
			public void run() {
				ExtGpio.playClick();
				try{Thread.sleep(50);}catch(Exception e){};
				ExtGpio.playClick();
				try{Thread.sleep(50);}catch(Exception e){};
				ExtGpio.playClick();
			}
		});
// End of H.M.Wang 2022-4-8 当QR_R.csv文件全部打印完成时，取消停止打印，因为取消太快的话，打印内容可能被切掉，改为报警

		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				ToastUtil.show(mContext, R.string.str_barcode_end);
			}
		});
// End of H.M.Wang 2024-6-15 再次取消强制停止打印，恢复为2022-4-8的状态，只是为了避免频繁报警和显示提示，增加一个1秒钟的时间间隔，该时间间隔内静默
	}

	private void sendToRemote(String msg) {
// H.M.Wang 2021-10-30 更新网络命令实现机制
		if(null != mPCCommandManager) {
			mPCCommandManager.sendMessage(msg);
			return;
		}
// End of H.M.Wang 2021-10-30 更新网络命令实现机制

		try {
			PrintWriter pout = new PrintWriter(new BufferedWriter(
                     new OutputStreamWriter(Gsocket.getOutputStream())),true);
             pout.println(msg);
		} catch (Exception e) {
		}
	}

// 2020-7-21 在未开始打印前，网络清洗命令不响应问题解决，追加一个类变量
	DataTransferThread mTempThread;
// End of 2020-7-21 在未开始打印前，网络清洗命令不响应问题解决，追加一个类变量

	//Soect_____________________________________________________________________________________________________________________________
			//通讯 开始
			private void SocketBegin()
			{
				//Net = new Network();
				int nRet = 0;
			//	if (!Net.checkNetWork(mContext)) {
				//	ToastUtil.show(mContext, "没有开启网络...!");
				//	return;
			//	}
				hostip = getLocalIpAddress(); //获取本机
				
				
				final ServerThread serverThread=new ServerThread();
				//flag=true;
				serverThread.start();//线程开始
				

				
		//接收线程处理
			myHandler =new Handler(){	
			public void handleMessage(Message msg)
				{ 
					if (msg.what == EditTabSmallActivity.HANDLER_MESSAGE_SAVE_SUCCESS) {
						String cmd = msg.getData().getString(Constants.PC_CMD);
						sendMsg(Constants.pcOk(cmd));
					} //else if (msg.what)
					else
					{
					 String ss=msg.obj.toString();
					}
				}
				};
			}
			public static String toStringHex(String s) {  
			    byte[] baKeyword = new byte[s.length() / 2];  
			    for (int i = 0; i < baKeyword.length; i++) {  
			        try {  
			            baKeyword[i] = (byte) (0xff & Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16));  
			        } catch (Exception e) {  
			            
			        }  
			    }  
			    try {  
			        s = new String(baKeyword, "utf-8");// UTF-16le:Not  
			    } catch (Exception e1) {  
			         
			    }  
			    return s;  
			}  
			
			//获取本机地址
			public static String getLocalIpAddress() {  
			        try {  
			            for (Enumeration<NetworkInterface> en = NetworkInterface  
			                            .getNetworkInterfaces(); en.hasMoreElements();) {  
			                        NetworkInterface intf = en.nextElement();  
			                       for (Enumeration<InetAddress> enumIpAddr = intf  
			                                .getInetAddresses(); enumIpAddr.hasMoreElements();) {  
			                            InetAddress inetAddress = enumIpAddr.nextElement();  
			                            if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress()) {  
			                            return inetAddress.getHostAddress().toString();  
			                            }  
			                       }  
			                    }  
			                } catch (SocketException ex) {  
			                    Debug.e("WifiPreference IpAddress", ex.toString());
			                }  
			             return null; 
			 }

			// H.M.Wang 2020-1-8 增加网络命令ID，在向PC报告打印状态的时候用来识别命令
			public String mPCCmdId = "";
			// End of H.M.Wang 2020-1-8 增加网络命令ID，在向PC报告打印状态的时候用来识别命令

			//Server服务
		    class ServerThread extends Thread {  
		          
		        public void stopServer(){  
		            try {                
		                if(server!=null){                   
		                	server.close();  
		                    System.out.println("close task successed");    
		                }  
		            } catch (IOException e) {               
		                System.out.println("close task failded");          
		                }  
		        }  
		    public void run() {  
		              
		                try {
		                	server = new ServerSocket(PORT);
							Socket so = new Socket();
		                } catch (IOException e1) {
		                    // TODO Auto-generated catch block  
		                    System.out.println("S2: Error");  
		                    e1.printStackTrace();
							return;
		                }  
		                mExecutorService = Executors.newCachedThreadPool();  //鍒涘缓涓?涓嚎绋嬫睜  
		                //System.out.println("鏈嶅姟鍣ㄥ凡鍚姩...");
		                Socket client = null;  
		                while(flag) {  
		                    try {  
		                        System.out.println("S3: Error");  
		                    client = server.accept();
		                    // set time out of Socket to 5s
		                    client.setSoTimeout(2 * 1000);
		                    //client.setSoTimeout(5000);
		                 //   System.out.println("S4: Error");  
		                    //鎶婂鎴风鏀惧叆瀹㈡埛绔泦鍚堜腑  
//		                    mList.add(client);

								Service service = new Service(client);
								stopOthers();
							mServices.put(client, service);
		                    mExecutorService.execute(service); //鍚姩涓?涓柊鐨勭嚎绋嬫潵澶勭悊杩炴帴
		                     }catch ( IOException e) {  
		                         System.out.println("S1: Error");  
		                        e.printStackTrace();  
		                    }  
		                }  
		             
		              
		        }  
		    }

		    public void sendMsg(String msg) {
// H.M.Wang 2021-10-30 更新网络命令实现机制
				if(null != mPCCommandManager) {
					mPCCommandManager.sendMessage(msg);
					return;
				}
// End of H.M.Wang 2021-10-30 更新网络命令实现机制
				Iterator<Socket> clients = mServices.keySet().iterator();
				for (;clients.hasNext();) {
					Socket socket = clients.next();
					Service service = mServices.get(socket);
					service.sendmsg(msg);
				}
			}
		    
			public void stopOthers() {
				Iterator<Socket> keys = mServices.keySet().iterator();
				for (;keys.hasNext();) {
					Socket socket = keys.next();
					Service service = mServices.get(socket);
					service.stop();
				}
				mServices.clear();
			}

		    //线程池，子线程
		    class Service implements Runnable {
                private volatile boolean kk=true;
		      
		         private BufferedReader in = null;
		         private String msg = "";  

		         public Service(Socket socket) {  
		        	 Gsocket = socket;  
		             try {  
		                 in = new BufferedReader(new InputStreamReader(socket.getInputStream()));  
		                 
		         		 	 
		         		//map=obtainSimpleInfo(mContext); 
		         		//msg=map.toString();
		         		
		                 //this.sendmsg(Querydb.QuerySqlData("select * from System"));  
		                 this.sendmsg("connected success!!!");  
		             } catch (IOException e) {  
		                 e.printStackTrace();  
		             }  
		         }

				public void stop() {
					Debug.d(TAG, "--->stop: " + Thread.currentThread().getName());
					kk = false;
				}

				private boolean isTlkReady(File tlk) {
					if (tlk == null) {
						return false;
					}
					File[] files = tlk.listFiles();
					if (files == null || files.length <= 0) {
						return false;
					}
					List<String> list = new ArrayList<String>();
					for (int i = 0; i < files.length; i++) {
						list.add(files[i].getName());
					}
					if (list.contains("1.TLK") && list.contains("1.bin")) {
						return true;
					}
					return false;
				}
		         public void run() {  
		               
		                 while(kk) {  
		                     try {
								 if((msg = in.readLine())!= null) {
									 //100是打印
									 //	msg= toStringHex(msg);
									 Debug.d(TAG, "--->fromPc: " + msg);
									 PCCommand cmd = PCCommand.fromString(msg);

									 // End of H.M.Wang 2020-1-8 提取命令ID
									 mPCCmdId = cmd.check;
									 // End of H.M.Wang 2020-1-8 提取命令ID

// H.M.Wang 当解析命令失败时，抛弃这个命令
// H.M.Wang 2019-12-30 收到空命令的时候，返回错误
									 if(null == cmd) {
										 this.sendmsg(Constants.pcErr("<Null Command>"));
										 continue;
									 }
// End of H.M.Wang 2019-12-30 收到空命令的时候，返回错误

									 if (PCCommand.CMD_SEND_BIN.equalsIgnoreCase(cmd.command) ||  // LAN Printing
											 PCCommand.CMD_SEND_BIN_S.equalsIgnoreCase(cmd.command)) {  // LAN Printing

										 cacheBin(Gsocket, msg);
									 } else if (PCCommand.CMD_DEL_LAN_BIN.equalsIgnoreCase(cmd.command) ||
											 PCCommand.CMD_DEL_LAN_BIN_S.equalsIgnoreCase(cmd.command)) {

										 DataTransferThread.deleteLanBuffer(Integer.valueOf(cmd.content));
									 } else if (PCCommand.CMD_RESET_INDEX.equalsIgnoreCase(cmd.command) ||
											 PCCommand.CMD_RESET_INDEX_S.equalsIgnoreCase(cmd.command)) {

										 if(null != mDTransThread) {
											 mDTransThread.resetIndex();
											 this.sendmsg(Constants.pcOk(msg));
										 } else {
											 sendmsg(Constants.pcErr(msg));
										 }
// H.M.Wang 2019-12-16 支持网络下发计数器和动态二维码的值
									 } else if (PCCommand.CMD_SET_REMOTE.equalsIgnoreCase(cmd.command) ||
											 PCCommand.CMD_SET_REMOTE_S.equalsIgnoreCase(cmd.command)) {
										 // H.M.Wang 2019-12-18 判断参数41，是否采用外部数据源，为true时才起作用
										 if (SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_LAN ||
// H.M.Wang 2020-6-28 追加专门为网络快速打印设置
												 SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_FAST_LAN) {
// End of H.M.Wang 2020-6-28 追加专门为网络快速打印设置
											 if(null != mDTransThread && mDTransThread.isRunning()) {
												 mDTransThread.setRemoteTextSeparated(cmd.content);
												 this.sendmsg(Constants.pcOk(msg));
											 } else {
												 this.sendmsg(Constants.pcErr(msg));
											 }
										 } else {
											 this.sendmsg(Constants.pcErr(msg));
										 }
										 // End.
// End. -----
									 } else if(PCCommand.CMD_PRINT.equalsIgnoreCase(cmd.command) ||
											 PCCommand.CMD_PRINT_S.equalsIgnoreCase(cmd.command)) {

										 File msgfile = new File(cmd.content);
										 if (!isTlkReady(msgfile)) {
											 sendmsg(Constants.pcErr(msg));
											 return;
										 }
										 if(PrinterFlag==0)
										 {
											 //打印赵工写好了，再测试
											 PrnComd="100";
											 PrinterFlag=1;
//		                            		StopFlag=1;
											 CleanFlag=0;

// H.M.Wang 2020-6-23 设置mObjPath，以便打开成功后显示信息名称
											 mObjPath= msgfile.getName();
// End of H.M.Wang 2020-6-23 设置mObjPath，以便打开成功后显示信息名称

											 Message message = mHandler.obtainMessage(MESSAGE_OPEN_TLKFILE);
											 Bundle bundle = new Bundle();
											 bundle.putString("file", mObjPath);  // f表示信息名称
											 bundle.putString(Constants.PC_CMD, msg);
											 message.setData(bundle);
											 mHandler.sendMessage(message);

										 }
									 } else if(PCCommand.CMD_CLEAN.equalsIgnoreCase(cmd.command) ||
											 PCCommand.CMD_CLEAN_S.equalsIgnoreCase(cmd.command)) {
										 //200是清洗
										 CleanFlag=1;
										 if(null != mDTransThread) {
											 mDTransThread.purge(mContext);
											 this.sendmsg(Constants.pcOk(msg));
										 } else {
// 2020-7-21 在未开始打印前，网络清洗命令不响应问题解决
											 mTempThread = DataTransferThread.getInstance(mContext);
											 mTempThread.purge(mContext);
											 this.sendmsg(Constants.pcOk(msg));
// End of 2020-7-21 在未开始打印前，网络清洗命令不响应问题解决
//											sendmsg(Constants.pcErr(msg));
										 }

									 } else if(PCCommand.CMD_SEND_FILE.equalsIgnoreCase(cmd.command) ||
											 PCCommand.CMD_SEND_FILE_S.equalsIgnoreCase(cmd.command)) {
										 //300发文件
										 AddPaths="";
										 if(SendFileFlag==0)//发文件等赵工写好了，再测试
										 {
											 SendFileFlag=1;
											 this.sendmsg(WriteFiles(Gsocket,msg));
										 }
									 } else if(PCCommand.CMD_READ_COUNTER.equalsIgnoreCase(cmd.command) ||
											 PCCommand.CMD_READ_COUNTER_S.equalsIgnoreCase(cmd.command)) {
										 //400取计数器
// H.M.Wang 2020-4-22 修改读取Counter命令返回格式
										 StringBuilder sb = new StringBuilder();

										 sb.append("" + mCounter);
										 for(int i=0; i<8; i++) {
											 sb.append("|" + (int)(mInkManager.getLocalInk(i)));
										 }
// H.M.Wang 2020-6-29 打印任务还没有启动时，DataTransferThread.getInstance(mContext)会自动生成instance，导致错误，应避免使用
										 sb.append("|" + (null != mDTransThread && mDTransThread.isRunning() ? "T" : "F") + "|");
// H.M.Wang 2020-6-29 打印任务还没有启动时，DataTransferThread.getInstance(mContext)会自动生成instance，导致错误，应避免使用
										 sb.append(msg);
										 this.sendmsg(Constants.pcOk(sb.toString()));
// End of H.M.Wang 2020-4-22 修改读取Counter命令返回格式

/*
// H.M.Wang 2020-7-1 临时版本，回复原来的回复格式
                                        for(int i=0;i<7;i++)
                                        {
                                            sendmsg("counter:" + mCounter+" |ink:" + mInkManager.getLocalInkPercentage(i) + "|state:" + (null != mDTransThread && mDTransThread.isRunning()));
                                            //获取INK无显示问题，赵工这地方改好，前面注示去掉就OK了
                                            this.sendmsg(Constants.pcOk(msg));
                                        }
// End of H.M.Wang 2020-7-1 临时版本，回复原来的回复格式
*/
									 } else if(PCCommand.CMD_STOP_PRINT.equalsIgnoreCase(cmd.command) ||
											 PCCommand.CMD_STOP_PRINT_S.equalsIgnoreCase(cmd.command)) {
										 //500停止打印
										 if(null != mDTransThread && mDTransThread.isRunning())
//		                            	if(StopFlag==1)
										 {
//		                            		StopFlag=0;
											 PrinterFlag=0;
											 Message message = mHandler.obtainMessage(MESSAGE_PRINT_STOP);
											 Bundle bundle = new Bundle();
											 bundle.putString(Constants.PC_CMD, msg);
											 message.setData(bundle);
											 message.sendToTarget();
											 //this.sendmsg(msg+"recv success!");

										 }
									 }
// H.M.Wang 2020-6-16 这个条件重复，应该注释掉
/*
		                            else if(PCCommand.CMD_SET_REMOTE.equalsIgnoreCase(cmd.command)) {
		                           //600字符串长成所需文件

		                    			StrInfo_Stack.push(cmd.content);//用堆栈存储收的信息，先进称出;
		                    			try {
											int count = Integer.parseInt(cmd.content);
											for (MessageTask task : mMsgTask) {
												for (BaseObject object : task.getObjects()) {
													if (object instanceof CounterObject) {
														object.setContent(cmd.content);
													}
												}
											}
										} catch (Exception e) {
											// TODO: handle exception
											this.sendmsg(Constants.pcErr(msg));
										}
										this.sendmsg(Constants.pcOk(msg));
		                            }*/
// End of H.M.Wang 2020-6-16 这个条件重复，应该注释掉

									 else if(PCCommand.CMD_MAKE_TLK.equalsIgnoreCase(cmd.command) ||
											 PCCommand.CMD_MAKE_TLK_S.equalsIgnoreCase(cmd.command)) {
										 //700
										 this.sendmsg(getString(R.string.str_build_tlk_start));
										 String[] parts = msg.split("\\|");
										 for (int j = 0; j < parts.length; j++) {
											 Debug.d(TAG, "--->parts[" + j + "] = " + parts[j]);
										 }

										 if (parts != null || parts.length > 4) {
											 MakeTlk(parts[3]);
										 }
									 }
									 else if(PCCommand.CMD_DEL_FILE.equalsIgnoreCase(cmd.command) ||
											 PCCommand.CMD_DEL_FILE_S.equalsIgnoreCase(cmd.command)) {
										 //600字符串长成所需文件
										 if (deleteFile(msg)) {
											 this.sendmsg(Constants.pcOk(msg));
										 } else {
											 this.sendmsg(Constants.pcErr(msg));
										 }
									 }
									 else if(PCCommand.CMD_DEL_DIR.equalsIgnoreCase(cmd.command) ||
											 PCCommand.CMD_DEL_DIR_S.equalsIgnoreCase(cmd.command)) {
										 //600字符串长成所需文件
										 if (deleteDirectory(msg)) {
											 this.sendmsg(Constants.pcOk(msg));
										 } else {
											 this.sendmsg(Constants.pcErr(msg));
										 }
										 // H.M.Wang 2019-12-25 追加速度和清洗命令
									 } else if(PCCommand.CMD_SET_DOTSIZE.equalsIgnoreCase(cmd.command)) {
										 try {
// H.M.Wang 2019-12-27 暂时取消3.7倍的系数。修改设置参数为23。取值范围0-6000。 2019-12-28 内部保存在参数33
											 SystemConfigFile.getInstance().setParamBroadcast(SystemConfigFile.INDEX_DOT_SIZE, Math.max(0, Math.min(6000, Integer.parseInt(cmd.content))));
//                                            SystemConfigFile.getInstance().setParamBroadcast(0, Math.round(3.7f * Integer.parseInt(cmd.content)));
// End of H.M.Wang 2019-12-27 暂时取消3.7倍的系数。修改设置参数为23。取值范围0-6000
											 SystemConfigFile.getInstance().saveConfig();
											 if(null != mDTransThread && mDTransThread.isRunning()) {
// H.M.Wang 2019-12-29 修改在打印状态下设置FPGA参数的逻辑
												 DataTask task = mDTransThread.getCurData();
// 2020-5-8												FpgaGpioOperation.clean();
												 FpgaGpioOperation.updateSettings(mContext, task, FpgaGpioOperation.SETTING_TYPE_NORMAL);
//												mDTransThread.mNeedUpdate = true;
//												while(mDTransThread.mNeedUpdate) {
//													Thread.sleep(10);
//												}
												 FpgaGpioOperation.init();
// H.M.Wang 2020-7-9 取消下发参数设置后重新下发打印缓冲区操作
//												mDTransThread.resendBufferToFPGA();
// End of H.M.Wang 2020-7-9 取消下发参数设置后重新下发打印缓冲区操作
// End of H.M.Wang 2019-12-29 修改在打印状态下设置FPGA参数的逻辑
											 }
											 this.sendmsg(Constants.pcOk(msg));
										 } catch (NumberFormatException e) {
											 Debug.e(TAG, e.getMessage());
										 }
									 } else if(PCCommand.CMD_SET_CLEAN.equalsIgnoreCase(cmd.command)) {
										 if(null != mDTransThread && mDTransThread.isRunning()) {
											 DataTask task = mDTransThread.getCurData();

											 int param0 = SystemConfigFile.getInstance().getParam(0);
// H.M.Wang 2019-12-27 修改取值，以达到下发FPGA时参数4的值为9
											 SystemConfigFile.getInstance().setParam(0, 18888);
// End of H.M.Wang 2019-12-27 修改取值，以达到下发FPGA时参数4的值为9
											 FpgaGpioOperation.updateSettings(mContext, task, FpgaGpioOperation.SETTING_TYPE_NORMAL);
// H.M.Wang 2019-12-27 间隔时间修改为10ms
											 Thread.sleep(10);
// End of H.M.Wang 2019-12-27 间隔时间修改为10ms
											 SystemConfigFile.getInstance().setParam(0, param0);
// 2020-5-8											FpgaGpioOperation.clean();
											 FpgaGpioOperation.updateSettings(mContext, task, FpgaGpioOperation.SETTING_TYPE_NORMAL);
// H.M.Wang 2019-12-27 重新启动打印
//											mDTransThread.mNeedUpdate = true;
//											while(mDTransThread.mNeedUpdate) {
//												Thread.sleep(10);
//											}
											 FpgaGpioOperation.init();
// H.M.Wang 2020-7-9 取消下发参数设置后重新下发打印缓冲区操作
//												mDTransThread.resendBufferToFPGA();
// End of H.M.Wang 2020-7-9 取消下发参数设置后重新下发打印缓冲区操作
// End of H.M.Wang 2019-12-27 重新启动打印
										 }
										 // End of H.M.Wang 2019-12-25 追加速度和清洗命令
										 this.sendmsg(Constants.pcOk(msg));
// H.M.Wang 2020-7-1 追加一个计数器设置数值命令
									 } else if(PCCommand.CMD_SET_COUNTER.equalsIgnoreCase(cmd.command)) {
										 try {
											 int cIndex = Integer.valueOf(cmd.content);
											 if(cIndex < 0 || cIndex > 9) {
												 Debug.e(TAG, "CMD_SET_COUNTER command, Index overflow.");
												 this.sendmsg(Constants.pcErr(msg));
											 } else {
												 try {
													 int cValue = Integer.valueOf(cmd.note2);
													 SystemConfigFile.getInstance().setParamBroadcast(cIndex + SystemConfigFile.INDEX_COUNT_1, cValue);
													 RTCDevice.getInstance(mContext).write(cValue, cIndex);
													 DataTransferThread dt = DataTransferThread.mInstance;
													 if(null != dt && dt.isRunning()) {
														 resetCounterIfNeed();
														 dt.mNeedUpdate = true;
// H.M.Wang 2020-7-9 追加计数器重置标识
														 dt.mCounterReset = true;
// End of H.M.Wang 2020-7-9 追加计数器重置标识
													 }

													 this.sendmsg(Constants.pcOk(msg));
												 } catch (NumberFormatException e) {
													 Debug.e(TAG, "CMD_SET_COUNTER command, invalid value.");
													 this.sendmsg(Constants.pcErr(msg));
												 }
											 }
										 } catch (NumberFormatException e) {
											 Debug.e(TAG, "CMD_SET_COUNTER command, invalid index.");
											 this.sendmsg(Constants.pcErr(msg));
										 }
// End of H.M.Wang 2020-7-1 追加一个计数器设置数值命令
									 } else if(PCCommand.CMD_SET_TIME.equalsIgnoreCase(cmd.command)) {
										 try {
											 int cYear = Integer.valueOf(cmd.content.substring(0,2));
											 int cMonth = Integer.valueOf(cmd.content.substring(2,4));
											 int cDate = Integer.valueOf(cmd.content.substring(4,6));
											 int cHour = Integer.valueOf(cmd.content.substring(6,8));
											 int cMinute = Integer.valueOf(cmd.content.substring(8,10));
											 int cSecond = Integer.valueOf(cmd.content.substring(10,12));

											 if(cYear < 0 || cYear > 99) {
												 Debug.e(TAG, "CMD_SET_TIME command, invalid year.");
												 this.sendmsg(Constants.pcErr(msg));
											 } else if(cMonth < 1 || cMonth > 12) {
												 Debug.e(TAG, "CMD_SET_TIME command, invalid month.");
												 this.sendmsg(Constants.pcErr(msg));
											 } else if(cDate < 1 || cDate > 31) {
												 Debug.e(TAG, "CMD_SET_TIME command, invalid date.");
												 this.sendmsg(Constants.pcErr(msg));
											 } else if(cHour < 0 || cHour > 23) {
												 Debug.e(TAG, "CMD_SET_TIME command, invalid hour.");
												 this.sendmsg(Constants.pcErr(msg));
											 } else if(cMinute < 0 || cMinute > 59) {
												 Debug.e(TAG, "CMD_SET_TIME command, invalid minute.");
												 this.sendmsg(Constants.pcErr(msg));
											 } else if(cSecond < 0 || cSecond > 59) {
												 Debug.e(TAG, "CMD_SET_TIME command, invalid second.");
												 this.sendmsg(Constants.pcErr(msg));
											 } else {
												 Calendar c = Calendar.getInstance();

												 c.set(cYear + 2000, cMonth-1, cDate, cHour, cMinute, cSecond);
												 SystemClock.setCurrentTimeMillis(c.getTimeInMillis());
												 RTCDevice rtcDevice = RTCDevice.getInstance(mContext);
												 rtcDevice.syncSystemTimeToRTC(mContext);
												 Debug.d(TAG, "Set time to: " + (cYear + 2000) + "-" + cMonth + "-" + cDate + " " + cHour + ":" + cMinute + ":" + cSecond);
												 this.sendmsg(Constants.pcOk(msg));
											 }
										 } catch (Exception e) {
											 Debug.e(TAG, "CMD_SET_TIME command, " + e.getMessage() + ".");
											 this.sendmsg(Constants.pcErr(msg));
										 }
// H.M.Wang 2020-7-28 追加一个设置参数命令
									 } else if(PCCommand.CMD_SET_PARAMS.equalsIgnoreCase(cmd.command)) {
										 try {
											 int cIndex = Integer.valueOf(cmd.content);
											 cIndex--;
											 if(cIndex < 0 || cIndex > 63) {
												 Debug.e(TAG, "Invalid PARAM index.");
												 this.sendmsg(Constants.pcErr(msg));
											 } else {
												 try {
													 int cValue = Integer.valueOf(cmd.note2);
//													if(cIndex == 3 || cIndex == 0 || cIndex == 1 || cIndex == 32 || (cIndex >= SystemConfigFile.INDEX_COUNT_1 && cIndex <= SystemConfigFile.INDEX_COUNT_10)) {
													 mSysconfig.setParamBroadcast(cIndex, cValue);
//													} else {
//														mSysconfig.setParam(cIndex, cValue);
//													}
													 this.sendmsg(Constants.pcOk(msg));
												 } catch (NumberFormatException e) {
													 Debug.e(TAG, "Invalid PARAM value.");
													 this.sendmsg(Constants.pcErr(msg));
												 }
											 }
										 } catch (NumberFormatException e) {
											 Debug.e(TAG, "Invalid PARAM index.");
											 this.sendmsg(Constants.pcErr(msg));
										 }
// End of H.M.Wang 2020-7-28 追加一个设置参数命令
// H.M.Wang 2020-9-28 追加一个心跳协议
									 } else if(PCCommand.CMD_HEARTBEAT.equalsIgnoreCase(cmd.command)) {
										 mLastHeartBeat = System.currentTimeMillis();
										 this.sendmsg(Constants.pcOk(msg));
// End of H.M.Wang 2020-9-28 追加一个心跳协议
// H.M.Wang 2021-2-4 追加软启动打印命令
									 } else if(PCCommand.CMD_SOFT_PHO.equalsIgnoreCase(cmd.command)) {
										 FpgaGpioOperation.softPho();
										 this.sendmsg(Constants.pcOk(msg));
// End of H.M.Wang 2021-2-4 追加软启动打印命令
									 } else {
										 this.sendmsg(Constants.pcErr(msg));
									 }

								 }
							 } catch (SocketTimeoutException e) {

                             }catch (IOException e) {
                                 Debug.i(TAG, "--->socketE: " + e.getMessage());
                                 kk=false;
                                 this.sendmsg(Constants.pcErr(msg + e.getMessage()));
                                 return;
                             // H.M.Wang 2019-12-25 追击异常处理
                             } catch (Exception e) {
                                 Debug.e(TAG, e.getMessage());
                             // End of H.M.Wang 2019-12-25 追击异常处理
                             }

		                 }
					     Debug.d(TAG, "--->thread " + Thread.currentThread().getName() + " stop");
		             
		         }  
		         //向客户端发信息
		         public void sendmsg(String msg) {  
		            //System.out.println(msg);
					 Debug.i(TAG, "--->send: " + msg);
		             PrintWriter pout = null;
		             try {  
		                 pout = new PrintWriter(new BufferedWriter(  
		                         new OutputStreamWriter(Gsocket.getOutputStream())),true);  
		                 pout.println(msg);  
		             }catch (IOException e) {  
		                 e.printStackTrace();  
		             }  
		      }  
		       

		}

		// cache bin
		private void cacheBin(Socket socket, String message) {
			Debug.i(TAG, "--->cacheBin: " + message);
			PCCommand cmd = PCCommand.fromString(message);
			int length = Integer.valueOf(cmd.size);
			int position = 0;
			byte[] buffer = new byte[length];
			byte[] readBuf = new byte[1024];
			Debug.i(TAG, "--->cacheBin length: " + length);
			if (length <= 16) {
			    this.sendMsg(Constants.pcErr(message));
			    return;
            }
			char[] remoteBin = new char[(length - 16) / 2];
			while (true) {
				try {
					InputStream stream = socket.getInputStream();
					int readLen = stream.read(readBuf);
					Debug.i(TAG, "--->read length: " + readLen);
					if (readLen == -1) { // EOF
						break;
					}
					if (position + readLen > buffer.length) {
						readLen = buffer.length - position;
					}
					System.arraycopy(readBuf, 0, buffer, position, readLen);
					position += readLen;
					if (position >= buffer.length) {
						break;
					}
				} catch (IOException ex) {

				}
			}
			for (int i = 0; i < remoteBin.length; i++) {
				remoteBin[i] = (char) ((char)(buffer[2 * i + 16 + 1] << 8) + (char)(buffer[2 * i + 16] & 0x0ff));
				remoteBin[i] = (char)(remoteBin[i] & 0x0ffff);
			}
			DataTransferThread.setLanBuffer(mContext, Integer.valueOf(cmd.content), remoteBin);

			this.sendMsg(Constants.pcOk(message));
		}
		//获取设备信息
		    private HashMap<String, String> obtainSimpleInfo(Context context){
				//HashMap<String, String> map = new HashMap<String, String>();
				PackageManager mPackageManager = context.getPackageManager();
				PackageInfo mPackageInfo = null;
				try {
					mPackageInfo = mPackageManager.getPackageInfo(context.getPackageName(), PackageManager.GET_ACTIVITIES);
				} catch (NameNotFoundException e) {
					e.printStackTrace();
				}
				 PackageManager pm = mContext.getPackageManager();
			        try {
						pi = pm.getPackageInfo(mContext.getPackageName(), PackageManager.GET_ACTIVITIES);
					} catch (NameNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

			   
			       

			        
			 
				map.put("versionName", mPackageInfo.versionName);
				map.put("versionCode", "_" + mPackageInfo.versionCode);
				map.put("Build_version", "_" + Build.VERSION.RELEASE);
				
				map.put("CPU ABI", "_" + Build.CPU_ABI);
			    map.put("Vendor", "_" + Build.MANUFACTURER);
				map.put("MODEL", "_" + Build.MODEL);
				map.put("SDK_INT", "_" + Build.VERSION.SDK_INT);
				map.put("PRODUCT", "_" +  Build.PRODUCT);
				
				return map;
			}
			//接收信息，并写文件	
			private String WriteFiles(Socket socket,String msg ) {
				
		        if (socket == null)
		        {
		            return "";
		        }

		        // H.M.Wang 追加接收文件长度信息
				PCCommand cmd = PCCommand.fromString(msg);
				int length = Integer.valueOf(cmd.size);
				Debug.d(TAG, "--->length: " + length);

		        InputStream in=null;
				FileOutputStream file = null;
				try {
		        	String savePath=msg.substring(msg.indexOf("/")+1,msg.lastIndexOf("/"));

		        	String[] Apath = savePath.split("\\/");
		        	 
		        	String TmpFiles=msg.substring(msg.lastIndexOf("/"));
		        	TmpFiles=TmpFiles.substring(TmpFiles.indexOf("/")+1,TmpFiles.indexOf("|"));

		        	String TmpsavePath= Paths.CreateDir(msg);
		        	       
		        
		        	savePath=TmpsavePath+TmpFiles;
				 	InputStream inb=null;
				 	AddPaths="";
			        	inb = socket.getInputStream();
				    	
			        	
			        file = new FileOutputStream(savePath, false);
				
					byte[] buffer = new byte[8192];
				
					int size = -1;
				
					int totalReceived = 0;
/* H.M.Wang 2019-10-1 原来赵工的代码
					while (true) {
						int read = 0;
						try {
							if (inb != null) {
								read = inb.read(buffer);
							}
						} catch (SocketTimeoutException e) {
							Debug.d(TAG, "--->SocketTimeout: " + read);
							if (totalReceived != 0) {
								read = -1;
							}

						}
						//passedlen += read;
						if (read == -1) {
							break;
						}
						Debug.d(TAG, "--->read: " + read);
						totalReceived += read;
						//下面进度条本为图形界面的prograssBar做的，这里如果是打文件，可能会重复打印出一些相同的百分比
						//System.out.println("文件接收了" +  (passedlen * 100/ len) + "%\n");
						file.write(buffer, 0, read);
					}
*/
/* H.M.Wang 2019-10-1 以前我修改的代码，修改的不全面
					// H.M.Wang对以下while语句进行修改，支持文件下载时指定字节数接收完成时结束
					while (totalReceived < length) {
//					while (true) {
						int read = 0;
						try {
							if (inb != null) {
								read = inb.read(buffer);
								if(read != 0) {
									Debug.d(TAG, "--->read: " + read);
									file.write(buffer, 0, read);
									totalReceived += read;
								}
							}
						} catch (SocketTimeoutException e) {
							Debug.d(TAG, "--->SocketTimeout: " + read);
//							if (totalReceived != 0) {
//								read = -1;
//							}
						}
						//passedlen += read;
//						if (read == -1) {
//							break;
//						}
//						Debug.d(TAG, "--->read: " + read);
//						totalReceived += read;
						//下面进度条本为图形界面的prograssBar做的，这里如果是打文件，可能会重复打印出一些相同的百分比
						//System.out.println("文件接收了" +  (passedlen * 100/ len) + "%\n");
//						file.write(buffer, 0, read);
//						totalReceived += read;
					}
*/

// H.M.Wang 2019-10-1 再次修改代码
					// H.M.Wang 2019-10-1
					while (totalReceived < length) {
						int read = 0;
						try {
							if (inb != null) {
								read = inb.read(buffer);
								if(read == -1) break;		// 到了输入流的结尾，退出接收
								Debug.d(TAG, "--->read: " + read);
								file.write(buffer, 0, read);
								totalReceived += read;
							} else {
								Debug.e(TAG, "--->Error: null InputStream");
								break;
							}
						} catch (SocketTimeoutException e) {
							Debug.d(TAG, "--->SocketTimeout: " + read);
							break;
						}
					}

					file.flush();
					file.close();

			} catch(Exception e){
					Debug.e(TAG, e.getMessage());
				return Constants.pcErr(msg);
			}
		    SendFileFlag=0;

			// H.M.Wang做一下修改
		 	return Constants.PC_RESULT_OK + msg;
//			return "000-ok: " + msg;
	}
	private void MakeTlk(String msg)
	{
		Debug.d(TAG, "--->msg: " + msg);
		File file = new File(msg);
		if (file == null) {
			return;
		}
		String tlk = file.getAbsolutePath();
		String Name = file.getParentFile().getName();
		if (!tlk.endsWith("TLK")) {
			return;
		}
		Debug.d(TAG, "--->tlk: " + tlk + "   Name = " + Name);
		MessageForPc message = new MessageForPc(mContext, tlk,Name);

		message.reCreate(mContext, myHandler, msg);
	}
	public boolean deleteFile(String filePath) {
		//delete file
		//getPath2();
	    File file = new File(filePath.substring(filePath.indexOf("/"), filePath.lastIndexOf("/")));
	    //file.setExecutable(true,false); 
	   // file.setReadable(true,false); 
	    //file.setWritable(true,false);
	    if(file.exists()) {
	    if(file.isFile()){
	       file.delete();
	       System.gc();
	       return true; 
	        }
	       
	}
	    return false;
	}
	    /**
	     * 删除文件夹以及目录下的文件
	     * @param   filePath 被删除目录的文件路径
	     * @return  目录删除成功返回true，否则返回false
	     */
	    public boolean deleteDirectory(String filePath) {
	    boolean flag = false;
			Debug.d(TAG, "--->filePath: " + filePath);
	        //如果filePath不以文件分隔符结尾，自动添加文件分隔符
	    filePath=filePath.substring(filePath.indexOf("/"), filePath.lastIndexOf("/"));
	        if (!filePath.endsWith(File.separator)) {
	            filePath = filePath + File.separator;
	        }
			Debug.d(TAG, "--->filePath: " + filePath);
	        File dirFile = new File(filePath);
	        if (!dirFile.exists() || !dirFile.isDirectory()) {
	            return false;
	        }
	        flag = true;
	        File[] files = dirFile.listFiles();
	        //遍历删除文件夹下的所有文件(包括子目录)
	        for (int i = 0; i < files.length; i++) {
	            if (files[i].isFile()) {
	            //删除子文件
	                flag = DeleteFolderFile(files[i].getAbsolutePath());
	                if (!flag) break;
	            } else {
	            //删除子目录
	                flag = deleteDirectory(files[i].getAbsolutePath());
	                if (!flag) break;
	            }
	        }
	        if (!flag) return false;
	        //删除当前空目录
	        return dirFile.delete();
	    }

	    /**
	     *  根据路径删除指定的目录或文件，无论存在与否
	     *@param filePath  要删除的目录或文件
	     *@return 删除成功返回 true，否则返回 false。
	     */
	    public boolean DeleteFolder(String filePath) {
	    File file = new File(filePath);
	        if (!file.exists()) {
	            return false;
	        } else {
	            if (file.isFile()) {
	            // 为文件时调用删除文件方法
	                return DeleteFolderFile(filePath);
	            } else {
	            // 为目录时调用删除目录方法
	                return deleteDirectory(filePath);
	            }
	        }
	    }
	    public boolean  DeleteFolderFile(String filePath) {
	    	//delete file
	    	//getPath2();
	        File file = new File(filePath);
	        //file.setExecutable(true,false); 
	       // file.setReadable(true,false); 
	        //file.setWritable(true,false);
	        if(file.exists()) {
	        if(file.isFile()){
	           file.delete();
	           System.gc();
	           return true; 
	            }
	           
	    }
	        return false;
	    }
	    public String getPath2() {
			String sdcard_path = null;
			String sd_default = Environment.getExternalStorageDirectory()
					.getAbsolutePath();
			Debug.d("text", sd_default);
			if (sd_default.endsWith("/")) {
				sd_default = sd_default.substring(0, sd_default.length() - 1);
			}
			// 得到路径
			try {
				Runtime runtime = Runtime.getRuntime();
				Process proc = runtime.exec("mount");
				InputStream is = proc.getInputStream();
				InputStreamReader isr = new InputStreamReader(is);
				String line;
				BufferedReader br = new BufferedReader(isr);
				while ((line = br.readLine()) != null) {
					if (line.contains("secure"))
						continue;
					if (line.contains("asec"))
						continue;
					if (line.contains("fat") && line.contains("/mnt/")) {
						String columns[] = line.split(" ");
						if (columns != null && columns.length > 1) {
							if (sd_default.trim().equals(columns[1].trim())) {
								continue;
							}
							sdcard_path = columns[1];
						}
					} else if (line.contains("fuse") && line.contains("/mnt/")) {
						String columns[] = line.split(" ");
						if (columns != null && columns.length > 1) {
							if (sd_default.trim().equals(columns[1].trim())) {
								continue;
							}
							sdcard_path = columns[1];
						}
					}
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Debug.d("text", sdcard_path);
			return sdcard_path;
		}

	    public void onComplete(int index) {
// H.M.Wang 2023-3-11 追加网络通讯前置缓冲区功能
			if(mPC_FIFO.PCFIFOEnabled()) {
				mPC_FIFO.onCompleted();
				return;
			}
// End of H.M.Wang 2023-3-11 追加网络通讯前置缓冲区功能

			Debug.d(TAG, "--->onComplete: mCounter:" + mCounter+"; ink:"+mInkManager.getLocalInk(0)+"; path:"+mObjPath+"\n");
			PrintWriter pout = null;
			// H.M.Wang 2020-1-8 向PC通报打印状态，附加命令ID
// H.M.Wang 2020-8-24 返回打印任务名称
//			this.sendMsg("000B|0000|1000|" + index + "|0000|0000|0001|" + mPCCmdId + "|0D0A");
			sendToRemote("000B|0000|1000|" + index + "|0000|" + mObjPath + "|0001|" + mPCCmdId + "|0D0A");
// End of H.M.Wang 2020-8-24 返回打印任务名称
//			this.sendMsg("000B|0000|1000|" + index + "|0000|0000|0000|0000|0D0A");
			// End of H.M.Wang 2020-1-8 向PC通报打印状态，附加命令ID
//	        try {
//	            pout = new PrintWriter(new BufferedWriter(  
//	                   new OutputStreamWriter(Gsocket.getOutputStream())),true);  
//	             pout.println(msg);  
//	         }catch (IOException e) {  
//	             e.printStackTrace();  
//	         }  
		}
		public void onPrinted0000(int index) {
// H.M.Wang 2023-3-11 追加网络通讯前置缓冲区功能
			if(mPC_FIFO.PCFIFOEnabled()) {
				mPC_FIFO.onPrinted();
				return;
			}
// End of H.M.Wang 2023-3-11 追加网络通讯前置缓冲区功能

			// H.M.Wang 2020-1-8 向PC通报打印状态，附加命令ID
// H.M.Wang 2020-8-24 返回打印任务名称
//			this.sendMsg("000B|0000|1000|" + index + "|0000|0000|0000|" + mPCCmdId + "|0D0A");
			sendToRemote("000B|0000|1000|" + index + "|0000|" + mObjPath + "|0000|" + mPCCmdId + "|0D0A");
// 将转移到0002中			sendToRemote("000B|0000|1000|" + index + "|" + mCounter + "," + (null != mDTransThread ? mDTransThread.getRemainCount() : 0) + "|" + mObjPath + "|0000|" + mPCCmdId + "|0D0A");
// End of H.M.Wang 2020-8-24 返回打印任务名称
//			this.sendMsg("000B|0000|1000|" + index + "|0000|0000|0000|0000|0D0A");
			// End of H.M.Wang 2020-1-8 向PC通报打印状态，附加命令ID
// H.M.Wang 2020-8-13 追加串口7协议
			if(SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_RS232_7) {
				final SerialHandler serialHandler = SerialHandler.getInstance(mContext);
// H.M.Wang 2020-11-18 cmdStatus=2,表示打印完成，msg里面放mCounter
// H.M.Wang 2024-5-11 计数器固定四位，0->0000，9999->9999，10000,20000->0000
//				serialHandler.sendCommandProcessResult(0, 1, 0, 2, String.valueOf(mCounter));
				String str = "0000" + mCounter;
				serialHandler.sendCommandProcessResult(0, 1, 0, 2, str.substring(str.length()-4));
// End of H.M.Wang 2024-5-11 计数器固定四位，0->0000，9999->9999，10000,20000->0000
// End of H.M.Wang 2020-11-18 cmdStatus=2,表示打印完成，msg里面放mCounter
			}
// End of H.M.Wang 2020-8-13 追加串口7协议
// H.M.Wang 2023-7-6 增加一个用户定义界面模式，支持向上连续打印和向下连续打印
			if(Configs.UI_TYPE == Configs.UI_CUSTOMIZED0) {
				if(mPrintType == PRINT_TYPE_UPWARD_CNT) {
					loadMessage(false);
				} else if(mPrintType == PRINT_TYPE_DWWARD_CNT) {
					loadMessage(true);
				}
			}
// End of H.M.Wang 2023-7-6 增加一个用户定义界面模式，长按预览区进入编辑页面，编辑当前任务
		}

		public void onPrinted0002(int index) {
			sendToRemote("000B|0000|1000|" + index + "|" + mCounter + "," + (null != mDTransThread ? mDTransThread.getRemainCount() : 0) + "|" + mObjPath + "|0002|" + mPCCmdId + "|0D0A");
		}

// H.M.Wang 2020-1-7 追加群组打印时，显示正在打印的MSG的序号
        public void onPrint(final int index) {
			Debug.d(TAG, "Index of Group: " + index);
// H.M.Wang 2020-4-15 追加群组打印时，显示每个正在打印的message的1.bmp
			mGroupIndex.post(new Runnable() {
				@Override
				public void run() {
					mGroupIndex.setText("No. " + (index + 1));
					mGroupIndex.setVisibility(View.VISIBLE);
// H.M.Wang 2022-11-29 追加UG当下发了打印任务以后，更新内部索引，并且保存该索引，更改预览图
					if (mObjPath.startsWith(Configs.USER_GROUP_PREFIX) && mUGSubObjs.size() > 0) {
						mUGSubIndex = index;
						MessageTask.saveUGIndex(mObjPath, index);
						mGroupIndex.setText(mUGSubObjs.get(index));
						mPreBitmap = BitmapFactory.decodeFile(MessageTask.getPreview(mObjPath + "/" + mUGSubObjs.get(index)));
						dispPreview(mPreBitmap);
					} else
// End of H.M.Wang 2022-11-29 追加UG当下发了打印任务以后，更新内部索引，并且保存该索引，更改预览图
// H.M.Wang 2022-10-25 追加一个“快速分组”的信息类型，该类型以Configs.QUICK_GROUP_PREFIX为文件名开头，信息中的每个超文本作为一个独立的信息保存在母信息的目录当中，并且所有的子信息作为一个群组管理，该子群组的信息也保存到木信息的目录当中
					if (mObjPath.startsWith(Configs.QUICK_GROUP_PREFIX)) {
						List<String> paths = MessageTask.parseQuickGroup(mObjPath);
// H.M.Wang 2021-7-26 改用mPreBitmap，取消aotu变量bmp
						mPreBitmap = BitmapFactory.decodeFile(MessageTask.getPreview(mObjPath + "/" + paths.get(index)));
						dispPreview(mPreBitmap);
// End of H.M.Wang 2021-7-26 改用mPreBitmap，取消aotu变量bmp
					} else
// End of H.M.Wang 2022-10-25 追加一个“快速分组”的信息类型，该类型以Configs.QUICK_GROUP_PREFIX为文件名开头，信息中的每个超文本作为一个独立的信息保存在母信息的目录当中，并且所有的子信息作为一个群组管理，该子群组的信息也保存到木信息的目录当中
					if (mObjPath.startsWith(Configs.GROUP_PREFIX)) {   // group messages
						List<String> paths = MessageTask.parseGroup(mObjPath);
// H.M.Wang 2021-7-26 改用mPreBitmap，取消aotu变量bmp
						mPreBitmap = BitmapFactory.decodeFile(MessageTask.getPreview(paths.get(index)));
						dispPreview(mPreBitmap);
// End of H.M.Wang 2021-7-26 改用mPreBitmap，取消aotu变量bmp
					}
				}
			});
// End of H.M.Wang 2020-4-15 追加群组打印时，显示每个正在打印的message的1.bmp
        }
// End of H.M.Wang 2020-1-7 追加群组打印时，显示正在打印的MSG的序号

// H.M.Wang 2022-11-10 追加一个打印时错误回调，用于报告错误，可以根据需要决定是否停止打印
		public void onError(boolean cancel) {
			if(cancel) {
				mHandler.sendEmptyMessage(MESSAGE_PRINT_STOP);
			}
		}
// End of H.M.Wang 2022-11-10 追加一个打印时错误回调，用于报告错误，可以根据需要决定是否停止打印
// H.M.Wang 2024-12-28 追加两个回调接口，一个是显示计数器的当前值，另一个是计数器到达边界是的报警
		public void onShowCounter(String str) {
	    	Message msg = mHandler.obtainMessage(MSG_SHOW_COUNTER, str);
	    	mHandler.sendMessage(msg);
		}
		public void onCounterReachEdge(int cntIndex) {
			Message msg = mHandler.obtainMessage(MSG_ALARM_CNT_EDGE, cntIndex, 0);
			mHandler.sendMessage(msg);
		}
// End of H.M.Wang 2024-12-28 追加两个回调接口，一个是显示计数器的当前值，另一个是计数器到达边界是的报警

	static char[] sRemoteBin;
	//Socket________________________________________________________________________________________________________________________________
	
}

