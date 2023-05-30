package com.industry.printer.ui;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;

import com.industry.printer.FileFormat.SystemConfigFile;
import com.industry.printer.R;
import com.industry.printer.Rfid.RfidScheduler;
import com.industry.printer.Serial.SerialHandler;
import com.industry.printer.Serial.SerialPort;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.ToastUtil;
import com.industry.printer.hardware.ExtGpio;
import com.industry.printer.hardware.FpgaGpioOperation;
import com.industry.printer.hardware.Hp22mm;
import com.industry.printer.hardware.IInkDevice;
import com.industry.printer.hardware.InkManagerFactory;
import com.industry.printer.hardware.SmartCard;
import com.industry.printer.hardware.SmartCardManager;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by hmwan on 2022/1/17.
 */

public class GpioTestPopWindow {
    public static final String TAG = GpioTestPopWindow.class.getSimpleName();

    private final int TEST_PHASE_NONE = 0;
    private final int TEST_PHASE_PINS = 1;              // 测试8个In和8个Out管脚的状态
    private final int TEST_PHASE_ID = 2;                // 读取Level的设备ID100次实验
    private final int TEST_PHASE_SC = 3;                // 对Pen1，Pen2和Bulk进行初始化100次实验
    private final int TEST_PHASE_LEVEL = 4;             // 读取100次Level1和Level2的值的测试，每读4次，关闭一次Level，第5次读后，再开启
// H.M.Wang 2022-7-20 增加Bag减1的操作内容
    private final int TEST_PHASE_REDUCE_BAG = 5;        // 墨袋减锁实验，每点击一次DO，检索一次
// End of H.M.Wang 2022-7-20 增加Bag减1的操作内容
// H.M.Wang 2022-10-15 增加Hp22mm库的测试
    private final int TEST_PHASE_HP22MM = 6;            // hp22mm相关测试
// End of H.M.Wang 2022-10-15 增加Hp22mm库的测试
// H.M.Wang 2022-11-02 增加Bagink的测试
    private final int TEST_PHASE_BAGINK = 7;            // BagInk相关测试，4个Level值的读取，和4个阀的开关
// End of H.M.Wang 2022-11-02 增加Bagink的测试
// H.M.Wang 2022-11-9 增加连供的开关阀测试
    private final int TEST_PHASE_SC_VALVE = 8;          // 连供的阀开关测试
// End of H.M.Wang 2022-11-9 增加连供的开关阀测试

    private int mCurrentTestPhase = TEST_PHASE_NONE;

    String[] IN_PINS = new String[] {
            "PG0", "PI5", "PI6", "PE7", "PE8", "PE9", "PE10", "PE11"
    };

    String[] OUT_PINS = new String[] {
            "PI8", "PB11", "PG4", "PH26", "PH27", "PE4", "PE5", "Serial"
    };

// H.M.Wang 2022-10-15 增加Hp22mm库的测试
    private String[] HP22MM_TEST_ITEMS = new String[] {
        "1 -- Init IDS",
        "2 -- Init PD",
        "3 -- ids_get_supply_status[1]",
//        "ids_get_supply_id[1]",
        "4 -- pd_get_print_head_status[0]",
        "5 -- pd_get_print_head_status[1]",
        "6 -- pd_sc_get_status[0]",
        "7 -- pd_sc_get_status[1]",
        "8 -- pd_sc_get_info[0]",
        "9 -- pd_sc_get_info[1]",
        "10 -- DeletePairing",
        "11 -- DoPairing(1,0)",
        "12 -- DoPairing(1,1)",
        "13 -- DoOverrides(1,0)",
        "14 -- DoOverrides(1,1)",
        "15 -- Pressurize(1)",
        "16 -- Depressurize(1)",
        "17 -- ids_set_platform_info",
        "18 -- pd_set_platform_info",
        "19 -- ids_set_date",
        "20 -- pd_set_date",
        "21 -- ids_set_stall_insert_count[1]",
        "22 -- Start Print",
        "23 -- Stop Print",
        "24 -- Dump Registers",
        "25 -- Write 1 Column",
        "26 -- Write 1KB",
        "27 -- Write 10 Columns",
        "28 -- Update PD MCU\nPut s19 file into [/mnt/sdcard/system/PD_FW.s19]",
        "29 -- Update FPGA FLASH\nPut s19 file into [/mnt/sdcard/system/FPGA.s19]",
        "30 -- Update IDS MCU\nPut s19 file into [/mnt/sdcard/system/IDS_FW.s19]",
        "31 -- Toggle PI4",
        "32 -- Toggle PI5",
        "32 -- Write SPI FPGA"
    };

    private String[] mHp22mmTestResult = new String[HP22MM_TEST_ITEMS.length];

    private final static int HP22MM_TEST_INIT_IDS                       = 0;
    private final static int HP22MM_TEST_INIT_PD                        = 1;
    private final static int HP22MM_TEST_IDS_GET_SUPPLY_STATUS          = 2;
//    private final static int HP22MM_TEST_IDS_GET_SUPPLY_INFO              = 8;
//    private final static int HP22MM_TEST_IDS_GET_SUPPLY_ID              = 8;
    private final static int HP22MM_TEST_PD_GET_PRINT_HEAD0_STATUS      = 3;
    private final static int HP22MM_TEST_PD_GET_PRINT_HEAD1_STATUS      = 4;
    private final static int HP22MM_TEST_PD_SC_GET_STATUS0              = 5;
    private final static int HP22MM_TEST_PD_SC_GET_STATUS1              = 6;
    private final static int HP22MM_TEST_PD_SC_GET_INFO0                = 7;
    private final static int HP22MM_TEST_PD_SC_GET_INFO1                = 8;
    private final static int HP22MM_TEST_DELETE_PAIRING                 = 9;
    private final static int HP22MM_TEST_DO_PAIRING10                   = 10;
    private final static int HP22MM_TEST_DO_PAIRING11                   = 11;
    private final static int HP22MM_TEST_DO_OVERRIDES10                 = 12;
    private final static int HP22MM_TEST_DO_OVERRIDES11                 = 13;
    private final static int HP22MM_TEST_PRESSURIZE                     = 14;
    private final static int HP22MM_TEST_DEPRESSURIZE                   = 15;
    private final static int HP22MM_TEST_IDS_SET_PF_INFO                = 16;
    private final static int HP22MM_TEST_PD_SET_PF_INFO                 = 17;
    private final static int HP22MM_TEST_IDS_SET_DATE                   = 18;
    private final static int HP22MM_TEST_PD_SET_DATE                    = 19;
    private final static int HP22MM_TEST_IDS_SET_STALL_INSERT_COUNT     = 20;
    private final static int HP22MM_TEST_START_PRINT                    = 21;
    private final static int HP22MM_TEST_STOP_PRINT                     = 22;
    private final static int HP22MM_TEST_DUMP_REGISTERS                 = 23;
    private final static int HP22MM_TEST_WRITE_1_COLUMN                 = 24;
    private final static int HP22MM_TEST_WRITE_1KB                      = 25;
    private final static int HP22MM_TEST_WRITE_10_COLUMNS               = 26;
    private final static int HP22MM_TEST_UPDATE_PD_MCU                  = 27;
    private final static int HP22MM_TEST_UPDATE_FPGA_FLASH              = 28;
    private final static int HP22MM_TEST_UPDATE_IDS_MCU                 = 29;
    private final static int HP22MM_TOGGLE_PI4                          = 30;
    private final static int HP22MM_TOGGLE_PI5                          = 31;
    private final static int HP22MM_WRITE_SPIFPGA                       = 32;

// End of H.M.Wang 2022-10-15 增加Hp22mm库的测试

    private final int PIN_ENABLE = 1;
    private final int PIN_DISABLE = 0;

    private final int FOR_OUT_PINS = 0;
    private final int FOR_IN_PINS = 1;

    private Context mContext = null;
    private PopupWindow mPopupWindow = null;

// H.M.Wang 2022-5-20 修改测试页面的布局，追加ID，SC及Level的测试功能。当前仅实现了ID，后续完善SC和Level。修改量很大，未一一标记
    private TextView mTestTitle = null;
    private ScrollView mTestArea = null;
    private TextView mTestResult = null;
// End of H.M.Wang 2022-5-20 修改测试页面的布局，追加ID，SC及Level的测试功能。当前仅实现了ID，后续完善SC和Level
// H.M.Wang 2022-10-15 增加Hp22mm库的测试
    private ListView mHp22mmTestLV = null;
// End of H.M.Wang 2022-10-15 增加Hp22mm库的测试
// H.M.Wang 2022-11-02 增加Bagink的测试
    private LinearLayout mBaginkTestArea = null;
// End of H.M.Wang 2022-11-02 增加Bagink的测试
// H.M.Wang 2022-11-9 增加连供的开关阀测试
    private LinearLayout mSCValveTestArea = null;
// End of H.M.Wang 2022-11-9 增加连供的开关阀测试

    private LinearLayout mPinsTestArea = null;
    private LinearLayout mOutPinLayout = null;
    private LinearLayout mInPinLayout = null;
    private TextView[] mOutPins = null;
    private TextView[] mInPins = null;
    private Timer mTimer = null;
    private int mCurrentOutPin = 0;
    private boolean mSerialWritting = false;

    private final int MESSAGE_SET_BGCOLOR = 102;
    private final int MSG_PINSTEST_NEXT = 103;
    private final int MSG_CHECK_PINS = 104;
    private final int MSG_TERMINATE_TEST = 105;
    private final int MSG_TEST_IN_PIN = 106;
    private final int MSG_SHOW_TEST_RESULT = 107;
    private final int MSG_SHOW_BAG_CONFIRM_DLG = 108;
    private final int MSG_SHOW_22MM_TEST_RESULT = 109;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_SET_BGCOLOR:
                    TextView tv = (TextView)msg.obj;
                    if(msg.arg1 == PIN_ENABLE) {
                        tv.setBackgroundColor(Color.GREEN);
                    } else {
                        tv.setBackgroundColor(Color.GRAY);
                    }
                    break;
                case MSG_PINSTEST_NEXT:
                    toggleOutPin(mCurrentOutPin);
                    mCurrentOutPin++;
                    mCurrentOutPin %= 8;
                    if(mCurrentOutPin != 0) sendEmptyMessageDelayed(MSG_PINSTEST_NEXT, 1000); else sendEmptyMessageDelayed(MSG_TERMINATE_TEST, 1000);
                    break;
                case MSG_TEST_IN_PIN:
                    testInPin(msg.arg1);
                    break;
                case MSG_TERMINATE_TEST:
                    mHandler.removeMessages(MSG_TEST_IN_PIN);
                    mSerialWritting = false;
                    mBaginkTestRunning = false;
                    break;
                case MSG_SHOW_TEST_RESULT:
                    mTestResult.setTextColor(msg.arg1);
                    mTestResult.setText((CharSequence)msg.obj);
                    break;
                case MSG_SHOW_BAG_CONFIRM_DLG:
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

                    AlertDialog dlg;
                    dlg = builder.setTitle("Confirmation")
                            .setMessage("")
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
//                                    Message msg = mHandler.obtainMessage(MSG_SHOW_TEST_RESULT);
//                                    msg.obj = "Cancelled.\n";
//                                    msg.arg1 = Color.RED;
//                                    mHandler.sendMessage(msg);
                                    dialog.dismiss();
                                }
                            })
                            .setPositiveButton("Do", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    final IInkDevice scm = InkManagerFactory.inkManager(mContext);
                                    if(scm instanceof SmartCardManager) {
                                        ((SmartCardManager) scm).reduceBag(new SmartCardManager.SCTestListener() {
                                            @Override
                                            public void onError(String result) {
                                                Message msg = mHandler.obtainMessage(MSG_SHOW_TEST_RESULT);
                                                msg.obj = result;
                                                msg.arg1 = Color.RED;
                                                mHandler.sendMessage(msg);
                                            }

                                            @Override
                                            public void onResult(String result) {
                                                Message msg = mHandler.obtainMessage(MSG_SHOW_TEST_RESULT);
                                                msg.obj = result;
                                                msg.arg1 = Color.BLACK;
                                                mHandler.sendMessage(msg);
                                            }
                                        });
                                        dialog.dismiss();
                                    }
                                }
                            })
                            .create();
                    dlg.show();
                    break;
                case MSG_SHOW_22MM_TEST_RESULT:
                    View view = (View)msg.obj;
                    dispHp22mmTestItem(view);
                    view.invalidate();
                    break;
            }
        }
    };

    private void dispHp22mmTestItem(View view) {
        int position = ((Integer)view.getTag());
        TextView cmd = (TextView) view.findViewById(R.id.hp22mm_test_cmd);
        cmd.setText(HP22MM_TEST_ITEMS[position]);
        View pb = view.findViewById(R.id.hp22mm_test_progress_bar);
        pb.setVisibility(View.GONE);
        TextView result = (TextView) view.findViewById(R.id.hp22mm_test_result);
        result.setText(mHp22mmTestResult[position]);
        if(null == mHp22mmTestResult[position] || mHp22mmTestResult[position].isEmpty()) {
            result.setVisibility(View.GONE);
        } else if(mHp22mmTestResult[position].startsWith("Success")) {
            result.setTextColor(Color.BLACK);
            result.setVisibility(View.VISIBLE);
        } else {
            result.setTextColor(Color.RED);
            result.setVisibility(View.VISIBLE);
        }
    }

    private View.OnClickListener mOutPinBtnClickListener = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            Integer index = (Integer)v.getTag();
            Debug.d(TAG, "Out Index: " + index);

            toggleOutPin(index);
        }
    };

    private View.OnClickListener mInPinBtnClickListener = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            Integer index = (Integer)v.getTag();
            Debug.d(TAG, "In Index: " + index);
        }
    };

    public GpioTestPopWindow(Context ctx) {
        mContext = ctx;
    }

    private void setTestPhase(int testPhase) {
//        if(testPhase == mCurrentTestPhase) return;

        final IInkDevice scm = InkManagerFactory.inkManager(mContext);
        switch(mCurrentTestPhase) {
            case TEST_PHASE_PINS:
                mHandler.removeMessages(MSG_TEST_IN_PIN);
                mHandler.removeMessages(MSG_PINSTEST_NEXT);
                mPinsTestArea.setVisibility(View.GONE);
                break;
            case TEST_PHASE_ID:
                if(scm instanceof SmartCardManager) {
                    ((SmartCardManager)scm).stopIDTest();
                }
                mTestArea.setVisibility(View.GONE);
                break;
            case TEST_PHASE_SC:
                if(scm instanceof SmartCardManager) {
                    ((SmartCardManager)scm).stopSCTest();
                }
                mTestArea.setVisibility(View.GONE);
                break;
            case TEST_PHASE_LEVEL:
                if(scm instanceof SmartCardManager) {
                    ((SmartCardManager)scm).stopLevelTest();
                }
                mTestArea.setVisibility(View.GONE);
                break;
            case TEST_PHASE_REDUCE_BAG:
                if (scm instanceof SmartCardManager) {
                    ((SmartCardManager) scm).stopBagReduce();
                }
                mTestArea.setVisibility(View.GONE);
                break;
// H.M.Wang 2022-10-15 增加Hp22mm库的测试
            case TEST_PHASE_HP22MM:
                mHp22mmTestLV.setVisibility(View.GONE);
                break;
// End of H.M.Wang 2022-10-15 增加Hp22mm库的测试
// H.M.Wang 2022-11-02 增加Bagink的测试
            case TEST_PHASE_BAGINK:
                mBaginkTestRunning = false;
                mBaginkTestArea.setVisibility(View.GONE);
                break;
// End of H.M.Wang 2022-11-02 增加Bagink的测试
// H.M.Wang 2022-11-9 增加连供的开关阀测试
            case TEST_PHASE_SC_VALVE:
                mSCValveTestArea.setVisibility(View.GONE);
                break;
// End of H.M.Wang 2022-11-9 增加连供的开关阀测试
        }

        mCurrentTestPhase = testPhase;

        switch(mCurrentTestPhase) {
            case TEST_PHASE_PINS:
                mTestTitle.setText("Pin Test");
                mPinsTestArea.setVisibility(View.VISIBLE);
                resetOutPins();
                resetInPins();
                mSerialWritting = false;
                mCurrentOutPin = 0;
                mAutoTest = true;
                mHandler.sendEmptyMessage(MSG_PINSTEST_NEXT);
                break;
            case TEST_PHASE_ID:
                mTestTitle.setText("ID Test");
                mTestArea.setVisibility(View.VISIBLE);
                if(scm instanceof SmartCardManager) {
                    ((SmartCardManager)scm).startIDTest(new SmartCardManager.SCTestListener() {
                        @Override
                        public void onError(String result) {
                            Message msg = mHandler.obtainMessage(MSG_SHOW_TEST_RESULT);
                            msg.obj = result;
                            msg.arg1 = Color.RED;
                            mHandler.sendMessage(msg);
                        }

                        @Override
                        public void onResult(String result) {
                            Message msg = mHandler.obtainMessage(MSG_SHOW_TEST_RESULT);
                            msg.obj = result;
                            msg.arg1 = Color.BLACK;
                            mHandler.sendMessage(msg);
                        }
                    });
                } else {
                    Message msg = mHandler.obtainMessage(MSG_SHOW_TEST_RESULT);
                    msg.obj = "Smart card not installed.\n";
                    msg.arg1 = Color.RED;
                    mHandler.sendMessage(msg);
                }
                break;
            case TEST_PHASE_SC:
                mTestTitle.setText("SC Test");
                mTestArea.setVisibility(View.VISIBLE);
                if(scm instanceof SmartCardManager) {
                    ((SmartCardManager)scm).startSCTest(new SmartCardManager.SCTestListener() {
                        @Override
                        public void onError(String result) {
                            Message msg = mHandler.obtainMessage(MSG_SHOW_TEST_RESULT);
                            msg.obj = result;
                            msg.arg1 = Color.RED;
                            mHandler.sendMessage(msg);
                        }

                        @Override
                        public void onResult(String result) {
                            Message msg = mHandler.obtainMessage(MSG_SHOW_TEST_RESULT);
                            msg.obj = result;
                            msg.arg1 = Color.BLACK;
                            mHandler.sendMessage(msg);
                        }
                    });
                } else {
                    Message msg = mHandler.obtainMessage(MSG_SHOW_TEST_RESULT);
                    msg.obj = "Smart card not installed.\n";
                    msg.arg1 = Color.RED;
                    mHandler.sendMessage(msg);
                }
                break;
            case TEST_PHASE_LEVEL:
                mTestTitle.setText("Level Test");
                mTestArea.setVisibility(View.VISIBLE);
                if(scm instanceof SmartCardManager) {
                    ((SmartCardManager)scm).startLevelTest(new SmartCardManager.SCTestListener() {
                        @Override
                        public void onError(String result) {
                            Message msg = mHandler.obtainMessage(MSG_SHOW_TEST_RESULT);
                            msg.obj = result;
                            msg.arg1 = Color.RED;
                            mHandler.sendMessage(msg);
                        }

                        @Override
                        public void onResult(String result) {
                            Message msg = mHandler.obtainMessage(MSG_SHOW_TEST_RESULT);
                            msg.obj = result;
                            msg.arg1 = Color.BLACK;
                            mHandler.sendMessage(msg);
                        }
                    });
                } else {
                    Message msg = mHandler.obtainMessage(MSG_SHOW_TEST_RESULT);
                    msg.obj = "Smart card not installed.\n";
                    msg.arg1 = Color.RED;
                    mHandler.sendMessage(msg);
                }
                break;
            case TEST_PHASE_REDUCE_BAG:
                mTestTitle.setText("Bag Reduction");
                mTestArea.setVisibility(View.VISIBLE);
                if(scm instanceof SmartCardManager) {
                    ((SmartCardManager)scm).startBagReduce(new SmartCardManager.SCTestListener() {
                        @Override
                        public void onError(String result) {
                            Message msg = mHandler.obtainMessage(MSG_SHOW_TEST_RESULT);
                            msg.obj = result;
                            msg.arg1 = Color.RED;
                            mHandler.sendMessage(msg);
                        }

                        @Override
                        public void onResult(String result) {
                            Message msg = mHandler.obtainMessage(MSG_SHOW_TEST_RESULT);
                            msg.obj = result;
                            msg.arg1 = Color.BLACK;
                            mHandler.sendMessage(msg);

                            mHandler.sendEmptyMessageDelayed(MSG_SHOW_BAG_CONFIRM_DLG, 100);
                        }
                    });
                } else {
                    Message msg = mHandler.obtainMessage(MSG_SHOW_TEST_RESULT);
                    msg.obj = "Smart card not installed.\n";
                    msg.arg1 = Color.RED;
                    mHandler.sendMessage(msg);
                }
                break;
// H.M.Wang 2022-10-15 增加Hp22mm库的测试
            case TEST_PHASE_HP22MM:
                mTestTitle.setText("Hp22mm Test");
                mHp22mmTestLV.setVisibility(View.VISIBLE);
                break;
// End of H.M.Wang 2022-10-15 增加Hp22mm库的测试
// H.M.Wang 2022-11-02 增加Bagink的测试
            case TEST_PHASE_BAGINK:
                mTestTitle.setText("Bagink Test");
                mBaginkTestRunning = true;
                mBaginkTestArea.setVisibility(View.VISIBLE);
                break;
// End of H.M.Wang 2022-11-02 增加Bagink的测试
// H.M.Wang 2022-11-9 增加连供的开关阀测试
            case TEST_PHASE_SC_VALVE:
                mTestTitle.setText("SC Valve Test");
                mSCValveTestArea.setVisibility(View.VISIBLE);
                break;
// End of H.M.Wang 2022-11-9 增加连供的开关阀测试
        }
    }

    private void doHp22mmTest(final View view, final int index) {
        View pb = view.findViewById(R.id.hp22mm_test_progress_bar);
        pb.setVisibility(View.VISIBLE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mHp22mmTestResult[index] = "Processing ... ";
                    Message msg = mHandler.obtainMessage(MSG_SHOW_22MM_TEST_RESULT);
                    msg.obj = view;
                    mHandler.sendMessage(msg);

                    switch (index) {
                        case HP22MM_TEST_INIT_IDS:
                            if (0 == Hp22mm.init_ids()) {
                                mHp22mmTestResult[index] = "Success\n" + Hp22mm.ids_get_sys_info();
                            } else {
                                mHp22mmTestResult[index] = "Failed\n" + Hp22mm.ids_get_sys_info();
                            }
                            break;
                        case HP22MM_TEST_INIT_PD:
                            if (0 == Hp22mm.init_pd()) {
                                mHp22mmTestResult[index] = "Success\n" + Hp22mm.pd_get_sys_info();
                            } else {
                                mHp22mmTestResult[index] = "Failed\n" + Hp22mm.pd_get_sys_info();
                            }
                            break;
                        case HP22MM_TEST_IDS_GET_SUPPLY_STATUS:
                            if (0 == Hp22mm.ids_get_supply_status()) {
                                mHp22mmTestResult[index] = "Success\n" + Hp22mm.ids_get_supply_status_info();
                            } else {
                                mHp22mmTestResult[index] = "Failed\n" + Hp22mm.ids_get_supply_status_info();
                            }
                            break;
//                        case HP22MM_TEST_IDS_GET_SUPPLY_ID:
//                            if (0 == Hp22mm.ids_get_supply_id()) {
//                                mHp22mmTestResult[index] = "Success\n" + Hp22mm.ids_get_supply_id_info();
//                            } else {
//                                mHp22mmTestResult[index] = "Failed\n" + Hp22mm.ids_get_supply_id_info();
//                            }
//                            break;
                        case HP22MM_TEST_PD_GET_PRINT_HEAD0_STATUS:
                            if (0 == Hp22mm.pd_get_print_head_status(0)) {
                                mHp22mmTestResult[index] = "Success\n" + Hp22mm.pd_get_print_head_status_info();
                            } else {
                                mHp22mmTestResult[index] = "Failed\n" + Hp22mm.pd_get_print_head_status_info();
                            }
                            break;
                        case HP22MM_TEST_PD_GET_PRINT_HEAD1_STATUS:
                            if (0 == Hp22mm.pd_get_print_head_status(1)) {
                                mHp22mmTestResult[index] = "Success\n" + Hp22mm.pd_get_print_head_status_info();
                            } else {
                                mHp22mmTestResult[index] = "Failed\n" + Hp22mm.pd_get_print_head_status_info();
                            }
                            break;
                        case HP22MM_TEST_PD_SC_GET_STATUS0:
                            if (0 == Hp22mm.pd_sc_get_status(0)) {
                                mHp22mmTestResult[index] = "Success\n" + Hp22mm.pd_sc_get_status_info();
                            } else {
                                mHp22mmTestResult[index] = "Failed";
                            }
                            break;
                        case HP22MM_TEST_PD_SC_GET_STATUS1:
                            if (0 == Hp22mm.pd_sc_get_status(1)) {
                                mHp22mmTestResult[index] = "Success\n" + Hp22mm.pd_sc_get_status_info();
                            } else {
                                mHp22mmTestResult[index] = "Failed";
                            }
                            break;
                        case HP22MM_TEST_PD_SC_GET_INFO0:
                            if (0 == Hp22mm.pd_sc_get_info(0)) {
                                mHp22mmTestResult[index] = "Success\n" + Hp22mm.pd_sc_get_info_info();
                            } else {
                                mHp22mmTestResult[index] = "Failed";
                            }
                            break;
                        case HP22MM_TEST_PD_SC_GET_INFO1:
                            if (0 == Hp22mm.pd_sc_get_info(1)) {
                                mHp22mmTestResult[index] = "Success\n" + Hp22mm.pd_sc_get_info_info();
                            } else {
                                mHp22mmTestResult[index] = "Failed";
                            }
                            break;
                        case HP22MM_TEST_DELETE_PAIRING:
                            if (0 == Hp22mm.DeletePairing()) {
                                mHp22mmTestResult[index] = "Success";
                            } else {
                                mHp22mmTestResult[index] = "Failed";
                            }
                            break;
                        case HP22MM_TEST_DO_PAIRING10:
                            if (0 == Hp22mm.DoPairing(0)) {
                                mHp22mmTestResult[index] = "Success";
                            } else {
                                mHp22mmTestResult[index] = "Failed";
                            }
                            break;
                        case HP22MM_TEST_DO_PAIRING11:
                            if (0 == Hp22mm.DoPairing(1)) {
                                mHp22mmTestResult[index] = "Success";
                            } else {
                                mHp22mmTestResult[index] = "Failed";
                            }
                            break;
                        case HP22MM_TEST_DO_OVERRIDES10:
                            if (0 == Hp22mm.DoOverrides(0)) {
                                mHp22mmTestResult[index] = "Success";
                            } else {
                                mHp22mmTestResult[index] = "Failed";
                            }
                            break;
                        case HP22MM_TEST_DO_OVERRIDES11:
                            if (0 == Hp22mm.DoOverrides(1)) {
                                mHp22mmTestResult[index] = "Success";
                            } else {
                                mHp22mmTestResult[index] = "Failed";
                            }
                            break;
                        case HP22MM_TEST_PRESSURIZE:
                            if (0 == Hp22mm.Pressurize()) {
                                mHp22mmTestResult[index] = "Success\n" + Hp22mm.getPressurizedValue();
                            } else {
                                mHp22mmTestResult[index] = "Failed";
                            }
                            break;
                        case HP22MM_TEST_DEPRESSURIZE:
                            if (0 == Hp22mm.Depressurize()) {
                                Thread.sleep(1000);
                                mHp22mmTestResult[index] = "Success\n" + Hp22mm.getPressurizedValue();
                            } else {
                                mHp22mmTestResult[index] = "Failed";
                            }
                            break;
                        case HP22MM_TEST_IDS_SET_PF_INFO:
                            if (0 == Hp22mm.ids_set_platform_info()) {
                                mHp22mmTestResult[index] = "Success";
                            } else {
                                mHp22mmTestResult[index] = "Failed";
                            }
                            break;
                        case HP22MM_TEST_PD_SET_PF_INFO:
                            if (0 == Hp22mm.pd_set_platform_info()) {
                                mHp22mmTestResult[index] = "Success";
                            } else {
                                mHp22mmTestResult[index] = "Failed";
                            }
                            break;
                        case HP22MM_TEST_IDS_SET_DATE:
                            if (0 == Hp22mm.ids_set_date()) {
                                mHp22mmTestResult[index] = "Success";
                            } else {
                                mHp22mmTestResult[index] = "Failed";
                            }
                            break;
                        case HP22MM_TEST_PD_SET_DATE:
                            if (0 == Hp22mm.pd_set_date()) {
                                mHp22mmTestResult[index] = "Success";
                            } else {
                                mHp22mmTestResult[index] = "Failed";
                            }
                            break;
                        case HP22MM_TEST_IDS_SET_STALL_INSERT_COUNT:
                            if (0 == Hp22mm.ids_set_stall_insert_count()) {
                                mHp22mmTestResult[index] = "Success";
                            } else {
                                mHp22mmTestResult[index] = "Failed";
                            }
                            break;
                        case HP22MM_TEST_START_PRINT:
                            if (0 == Hp22mm.startPrint()) {
                                mHp22mmTestResult[index] = "Success";
                            } else {
                                mHp22mmTestResult[index] = "Failed";
                            }
                            break;
                        case HP22MM_TEST_STOP_PRINT:
                            if (0 == Hp22mm.stopPrint()) {
                                mHp22mmTestResult[index] = "Success";
                            } else {
                                mHp22mmTestResult[index] = "Failed";
                            }
                            break;
                        case HP22MM_TEST_DUMP_REGISTERS:
                            String str = Hp22mm.dumpRegisters();
                            if (null != str) {
                                mHp22mmTestResult[index] = "Success\n" + str;
                            } else {
                                mHp22mmTestResult[index] = "Failed\nRegister read error";
                            }
                            break;
                        case HP22MM_TEST_WRITE_1_COLUMN:
                            if (0 == Hp22mm.Write1Column()) {
                                mHp22mmTestResult[index] = "Success";
                            } else {
                                mHp22mmTestResult[index] = "Failed";
                            }
                            break;
                        case HP22MM_TEST_WRITE_1KB:
                            if (0 == Hp22mm.Write1KB()) {
                                mHp22mmTestResult[index] = "Success";
                            } else {
                                mHp22mmTestResult[index] = "Failed";
                            }
                            break;
                        case HP22MM_TEST_WRITE_10_COLUMNS:
                            if (0 == Hp22mm.Write10Columns()) {
                                mHp22mmTestResult[index] = "Success";
                            } else {
                                mHp22mmTestResult[index] = "Failed";
                            }
                            break;
                        case HP22MM_TEST_UPDATE_PD_MCU:
                            if (0 == Hp22mm.UpdatePDFW()) {
                                mHp22mmTestResult[index] = "Success";
                            } else {
                                mHp22mmTestResult[index] = "Failed";
                            }
                            break;
                        case HP22MM_TEST_UPDATE_FPGA_FLASH:
                            if (0 == Hp22mm.UpdateFPGAFlash()) {
                                mHp22mmTestResult[index] = "Success";
                            } else {
                                mHp22mmTestResult[index] = "Failed";
                            }
                            break;
                        case HP22MM_TEST_UPDATE_IDS_MCU:
                            if (0 == Hp22mm.UpdateIDSFW()) {
                                mHp22mmTestResult[index] = "Success";
                            } else {
                                mHp22mmTestResult[index] = "Failed";
                            }
                            break;
                        case HP22MM_TOGGLE_PI4:
                            int valPI4 = ExtGpio.readGpioTestPin('I', 4);
                            ExtGpio.writeGpioTestPin('I', 4, (valPI4 != 0 ? 0 : 1));
                            int valPI4_1 = ExtGpio.readGpioTestPin('I', 4);
                            mHp22mmTestResult[index] = "Success\n" + valPI4 + " -> " + valPI4_1;
                            break;
                        case HP22MM_TOGGLE_PI5:
                            int valPI5 = ExtGpio.readGpioTestPin('I', 5);
                            ExtGpio.writeGpioTestPin('I', 5, (valPI5 != 0 ? 0 : 1));
                            int valPI5_1 = ExtGpio.readGpioTestPin('I', 5);
                            mHp22mmTestResult[index] = "Success\n" + valPI5 + " -> " + valPI5_1;
                            break;
                        case HP22MM_WRITE_SPIFPGA:
                            valPI4 = ExtGpio.readGpioTestPin('I', 4);
                            ExtGpio.writeGpioTestPin('I', 4, 0);
                            if (0 == Hp22mm.WriteSPIFPGA()) {
                                mHp22mmTestResult[index] = "Success";
                            } else {
                                mHp22mmTestResult[index] = "Failed";
                            }
                            ExtGpio.writeGpioTestPin('I', 4, valPI4);
                            break;

                    }
                    msg = mHandler.obtainMessage(MSG_SHOW_22MM_TEST_RESULT);
                    msg.obj = view;
                    mHandler.sendMessage(msg);
                } catch(UnsatisfiedLinkError e) {
                    Debug.e(TAG, e.getMessage());
                } catch(Exception e) {
                    Debug.e(TAG, e.getMessage());
                }
            }
        }).start();
    }

    public void show(View v) {
        if(null == mContext) {
            return;
        }

        View popupView = LayoutInflater.from(mContext).inflate(R.layout.gpio_test_pop, null);

        mPopupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);
        mPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#CC000000")));
        mPopupWindow.setOutsideTouchable(true);
        mPopupWindow.setTouchable(true);
        mPopupWindow.update();

        TextView quitIV = (TextView)popupView.findViewById(R.id.btn_quit);
        quitIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                mTimer.cancel();
//                mTimer = null;
                mHandler.sendEmptyMessage(MSG_TERMINATE_TEST);
//                mSerialWritting = false;
                mPopupWindow.dismiss();
            }
        });

        mTestTitle = (TextView)popupView.findViewById(R.id.test_title);
        mTestArea = (ScrollView) popupView.findViewById(R.id.id_test_area);
        TextView testID = (TextView)popupView.findViewById(R.id.btn_testID);
        testID.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTestPhase(TEST_PHASE_ID);
            }
        });
        TextView testSC = (TextView)popupView.findViewById(R.id.btn_testSC);
        testSC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTestPhase(TEST_PHASE_SC);
            }
        });
        TextView testLVL = (TextView)popupView.findViewById(R.id.btn_testLVL);
        testLVL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTestPhase(TEST_PHASE_LEVEL);
            }
        });
// H.M.Wang 2022-7-20 增加Bag减1的操作内容
        TextView bagMinus1 = (TextView)popupView.findViewById(R.id.btn_bag_1);
        bagMinus1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTestPhase(TEST_PHASE_REDUCE_BAG);
            }
        });
// End of H.M.Wang 2022-7-20 增加Bag减1的操作内容
        mTestResult = (TextView)popupView.findViewById(R.id.id_test_result);

// H.M.Wang 2022-10-15 增加Hp22mm库的测试
        mHp22mmTestLV = (ListView) popupView.findViewById(R.id.lv_hp22mm_test);
        mHp22mmTestLV.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return HP22MM_TEST_ITEMS.length;
            }

            @Override
            public Object getItem(int i) {
                return HP22MM_TEST_ITEMS[i];
            }

            @Override
            public long getItemId(int i) {
                return 0;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if(null == convertView) {
                    convertView = ((LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.hp22mm_test_item, null);
                }

                convertView.setTag(position);
                dispHp22mmTestItem(convertView);

                return convertView;
            }
        });
        mHp22mmTestLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                doHp22mmTest(view, i);
            }
        });
        TextView testHp22mm = (TextView)popupView.findViewById(R.id.btn_hp22mm);
        testHp22mm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTestPhase(TEST_PHASE_HP22MM);
            }
        });
// End of H.M.Wang 2022-10-15 增加Hp22mm库的测试

        mPinsTestArea = (LinearLayout) popupView.findViewById(R.id.pins_test_area);
        TextView testpins = (TextView)popupView.findViewById(R.id.btn_testpins);
        testpins.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTestPhase(TEST_PHASE_PINS);
            }
        });

        mOutPinLayout = (LinearLayout) popupView.findViewById(R.id.out_pin_area);
        mOutPins = new TextView[OUT_PINS.length];
        for(int i=0; i<OUT_PINS.length; i++) {
            TextView tv = new TextView(mContext);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0,1,0,1);
            tv.setLayoutParams(lp);
            tv.setPadding(0,5,0,5);
            tv.setGravity(Gravity.CENTER);
            tv.setBackgroundColor(Color.GRAY);
            tv.setTextColor(Color.BLACK);
            tv.setTextSize(20);
            tv.setTag(i);
//            tv.setText(OUT_PINS[i]);
            tv.setText("Out-" + (i+1));
            tv.setOnClickListener(mOutPinBtnClickListener);
            mOutPinLayout.addView(tv);
            mOutPins[i] = tv;
        }

        mInPinLayout = (LinearLayout) popupView.findViewById(R.id.in_pin_area);
        mInPins = new TextView[IN_PINS.length];
        for(int i=0; i<IN_PINS.length; i++) {
            TextView tv = new TextView(mContext);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0,1,0,1);
            tv.setLayoutParams(lp);
            tv.setPadding(0,5,0,5);
            tv.setGravity(Gravity.CENTER);
            tv.setBackgroundColor(Color.GRAY);
            tv.setTextColor(Color.BLACK);
            tv.setTextSize(20);
            tv.setTag(i);
//            tv.setText(IN_PINS[i]);
            tv.setText("In-" + (i+1));
            tv.setOnClickListener(mInPinBtnClickListener);
            mInPinLayout.addView(tv);
            mInPins[i] = tv;
        }

        mPopupWindow.showAtLocation(v, Gravity.NO_GRAVITY, 0, 0);

        resetOutPins();
        resetInPins();

// H.M.Wang 2022-11-9 增加连供的开关阀测试
        mSCValveTestArea = (LinearLayout) popupView.findViewById(R.id.sc_valve_test_area);
        TextView scValveTest = (TextView)popupView.findViewById(R.id.btn_sc_valve);
        scValveTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTestPhase(TEST_PHASE_SC_VALVE);
            }
        });
        TextView scValve1 = (TextView)popupView.findViewById(R.id.sc_valve1);
        scValve1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final IInkDevice scm = InkManagerFactory.inkManager(mContext);
                if(scm instanceof SmartCardManager) {
                    ((SmartCardManager)scm).addInkOn(0);
                    try {Thread.sleep(100);} catch (Exception e) {}
                    ((SmartCardManager)scm).addInkOff(0);
                }
            }
        });
        TextView scValve2 = (TextView)popupView.findViewById(R.id.sc_valve2);
        scValve2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final IInkDevice scm = InkManagerFactory.inkManager(mContext);
                if(scm instanceof SmartCardManager) {
                    ((SmartCardManager)scm).addInkOn(1);
                    try {Thread.sleep(100);} catch (Exception e) {}
                    ((SmartCardManager)scm).addInkOff(1);
                }
            }
        });
// End of H.M.Wang 2022-11-9 增加连供的开关阀测试

// H.M.Wang 2022-11-02 增加Bagink的测试
        mBaginkTestArea = (LinearLayout) popupView.findViewById(R.id.bagink_test_area);
        mBaginkTest = new BaginkTest[] {
            new BaginkTest(popupView, 0),
            new BaginkTest(popupView, 1),
            new BaginkTest(popupView, 2),
            new BaginkTest(popupView, 3)
        };

        TextView testBagink = (TextView)popupView.findViewById(R.id.btn_bagink);
        testBagink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTestPhase(TEST_PHASE_BAGINK);
            }
        });
// End of H.M.Wang 2022-11-02 增加Bagink的测试
    }

// H.M.Wang 2022-12-24 将读BaginkLevel的功能独立为一个函数，并且加入线程互斥，还有增加HX24LC的读写功能
    private final int mBaginkLevelResIDs[] = new int[] {R.id.bagink_level1, R.id.bagink_level2, R.id.bagink_level3, R.id.bagink_level4};
    private final int mBaginkHX24LCResIDs[] = new int[] {R.id.bagink_hx24lc1, R.id.bagink_hx24lc2, R.id.bagink_hx24lc3, R.id.bagink_hx24lc4};
    private final int mBaginkValveResIDs[] = new int[] {R.id.bagink_valve1, R.id.bagink_valve2, R.id.bagink_valve3, R.id.bagink_valve4};
    private BaginkTest mBaginkTest[];
    private boolean mBaginkTestRunning;
    private int mButtonClickCount;

    private class BaginkTest {
        private boolean mPause;
        private TextView mBaginkLevelTV;
        private TextView mBaginkHX24LCTV;
        private TextView mBaginkValveTV;
        private ArrayList<Integer> mLevels;
        private ArrayList<Integer> mHX24LCValues;
// H.M.Wang 2023-3-31 临时增加异常值管理，暂时只是看看是否能够捕捉住异常并按策略显示出来
//        连续统计五分钟以内，超高位， 超过高墨位阈值20的， 占到1%，   红字提示1，  停加墨， 但不停打印
//        高位为超过阈值10-20，  5%的比例， 认为高， 停加墨， 不听打印。  普通色 提示3
//        数据离散性， 估计超过正负十， 即为异常数据，  占30%， 即为异常， 该停加墨， 屏幕红字提示2， 但不停止打印
        private class Level_Record {
            public long RecordedTime;
            public int  Level;

            public Level_Record(long rt, int level) {
                RecordedTime = rt;
                Level = level;
            }
        };
        private ArrayList<Level_Record> mLevelRecords = new ArrayList<Level_Record>();
        private int mCountGt580 = 0;
        private int mCountGt570_580 = 0;
        private int mCountGap = 0;
        private int mCountError = 0;

        private LinearLayout mAdditionalTest;
        private LinearLayout mLine1;
        private LinearLayout mLine2;
        private LinearLayout mLine3;
        private LinearLayout mLine4;
        private TextView mLine1Text;
        private TextView mLine2Text;
        private TextView mLine3Text;
        private TextView mLine4Text;
// End of H.M.Wang 2023-3-31 临时增加异常值管理，暂时只是看看是否能够捕捉住异常并按策略显示出来

        public BaginkTest(View parent, final int index) {
            mPause = true;

            mLevels = new ArrayList<Integer>();
            mHX24LCValues = new ArrayList<Integer>();

// H.M.Wang 2023-3-31 临时增加异常值管理，暂时只是看看是否能够捕捉住异常并按策略显示出来
            mAdditionalTest = (LinearLayout)parent.findViewById(R.id.additional_test);
            mAdditionalTest.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mAdditionalTest.setVisibility(View.GONE);
                }
            });
            mLine1 = (LinearLayout)parent.findViewById(R.id.line1);
            mLine2 = (LinearLayout)parent.findViewById(R.id.line2);
            mLine3 = (LinearLayout)parent.findViewById(R.id.line3);
            mLine4 = (LinearLayout)parent.findViewById(R.id.line4);
            mLine1Text = (TextView)parent.findViewById(R.id.line1_text);
            mLine2Text = (TextView)parent.findViewById(R.id.line2_text);
            mLine3Text = (TextView)parent.findViewById(R.id.line3_text);
            mLine4Text = (TextView)parent.findViewById(R.id.line4_text);
// End of H.M.Wang 2023-3-31 临时增加异常值管理，暂时只是看看是否能够捕捉住异常并按策略显示出来

            mBaginkLevelTV = (TextView)parent.findViewById(mBaginkLevelResIDs[index]);
            mBaginkLevelTV.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(mPause && mBaginkTestRunning) {
                        mBaginkLevelTV.setBackgroundColor(Color.parseColor("#008800"));
                        mAdditionalTest.setVisibility(View.VISIBLE);
                        mPause = false;
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Debug.d(TAG, "Enter Bagink Level[" + index + "] Test ");
                                synchronized (GpioTestPopWindow.this) {
                                    ExtGpio.rfidSwitch(RfidScheduler.LEVELS[index]);
                                    try {Thread.sleep(50);} catch (Exception e) {}
                                    SmartCard.initLevelDirect();
                                }
// H.M.Wang 2023-3-31 临时增加异常值管理，暂时只是看看是否能够捕捉住异常并按策略显示出来
                                mCountGt580 = 0;
                                mCountGt570_580 = 0;
                                mCountGap = 0;
                                mCountError = 0;
                                int last_level = -1;
// End of H.M.Wang 2023-3-31 临时增加异常值管理，暂时只是看看是否能够捕捉住异常并按策略显示出来
                                while(!mPause && mBaginkTestRunning) {
                                    synchronized (GpioTestPopWindow.this) {
                                        ExtGpio.rfidSwitch(RfidScheduler.LEVELS[index]);
                                        try {Thread.sleep(50);} catch (Exception e) {}
                                        int level = SmartCard.readLevelDirect();
                                        Debug.d(TAG, "Level[" + index + "] = " + level);
                                        mLevels.add(level);
                                        if(mLevels.size() > 3) {
                                            mLevels.remove(0);
                                        }
// H.M.Wang 2023-3-31 临时增加异常值管理，暂时只是看看是否能够捕捉住异常并按策略显示出来
                                        long rt = System.currentTimeMillis();
                                        while(mLevelRecords.size() > 0) {
                                            Level_Record lr = mLevelRecords.get(0);
                                            if(rt - lr.RecordedTime > 5 * 60 * 1000) {
                                                if (lr.Level == 0x0FFFFFFF) {
                                                    mCountError--;
                                                } else {
                                                    if (lr.Level > 58000000) {
                                                        mCountGt580--;
                                                    } else if (lr.Level > 57000000) {
                                                        mCountGt570_580--;
                                                    }
                                                    if(last_level != -1) {
                                                        if(Math.abs(lr.Level - last_level) > 1000000) {
                                                            mCountGap--;
                                                        }
                                                    }
                                                }
                                                last_level = lr.Level;
                                                mLevelRecords.remove(0);
                                            } else {
                                                break;
                                            }
                                        }

                                        if (level == 0x0FFFFFFF) {
                                            mCountError++;
                                        } else {
                                            if (level > 58000000) {
                                                mCountGt580++;
                                            } else if (level > 57000000) {
                                                mCountGt570_580++;
                                            }
                                            if(mLevelRecords.size() > 0) {
                                                if(Math.abs(level - mLevelRecords.get(mLevelRecords.size()-1).Level) > 1000000) {
                                                    mCountGap++;
                                                }
                                            }
                                        }
                                        mLevelRecords.add(new Level_Record(rt, level));

                                        mAdditionalTest.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                mLine1Text.setText("" + mCountGt580 + "/" + mLevelRecords.size() + "(" + (1.0f * Math.round(1.0f * mCountGt580 / mLevelRecords.size() * 1000) / 10) + "%)");
                                                if(1.0f * mCountGt580 / mLevelRecords.size() > 0.01f) mLine1Text.setTextColor(Color.RED); else mLine1Text.setTextColor(Color.WHITE);
                                                mLine2Text.setText("" + mCountGt570_580 + "/" + mLevelRecords.size() + "(" + (1.0f * Math.round(1.0f * mCountGt570_580 / mLevelRecords.size() * 1000) / 10) + "%)");
                                                if(1.0f * mCountGt570_580 / mLevelRecords.size() > 0.05f) mLine2Text.setTextColor(Color.BLUE); else mLine1Text.setTextColor(Color.WHITE);
                                                mLine3Text.setText("" + mCountGap + "/" + mLevelRecords.size() + "(" + (1.0f * Math.round(1.0f * mCountGap / mLevelRecords.size() * 1000) / 10) + "%)");
                                                if(1.0f * mCountGap / mLevelRecords.size() > 0.3f) mLine3Text.setTextColor(Color.RED); else mLine1Text.setTextColor(Color.WHITE);
                                                mLine4Text.setText("" + mCountError + "/" + mLevelRecords.size() + "(" + (1.0f * Math.round(1.0f * mCountError / mLevelRecords.size() * 1000) / 10) + "%)");
                                            }
                                        });
// End of H.M.Wang 2023-3-31 临时增加异常值管理，暂时只是看看是否能够捕捉住异常并按策略显示出来

                                        int hx24lc = SmartCard.readHX24LC();
                                        Debug.d(TAG, "HX24LC[" + index + "] = " + hx24lc);
                                        mHX24LCValues.add(hx24lc);
                                        if(mHX24LCValues.size() > 3) {
                                            mHX24LCValues.remove(0);
                                        }
                                        showTestMessage();
                                    }
                                    try {Thread.sleep(3000);} catch (Exception e) {}
                                }
                                Debug.d(TAG, "Quit Bagink Level[" + index + "] Test ");
                            }
                        }).start();
                    } else {
                        mBaginkLevelTV.setBackgroundColor(Color.parseColor("#8E8E8E"));
                        mAdditionalTest.setVisibility(View.GONE);
                        mPause = true;
                    }
                }
            });
            mBaginkLevelTV.setBackgroundColor(Color.parseColor("#8E8E8E"));

            mBaginkHX24LCTV = (TextView)parent.findViewById(mBaginkHX24LCResIDs[index]);
            mBaginkHX24LCTV.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final EditText editText = new EditText(mContext);
                    editText.setInputType(InputType.TYPE_CLASS_TEXT);
                    editText.setLines(1);
                    mButtonClickCount = 0;

                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    AlertDialog ad = builder.setTitle("Enter HX24LC Value").setView(editText).setPositiveButton(R.string.str_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Field field = null;
                            try{
                                field = dialog.getClass().getSuperclass().getDeclaredField("mShowing");
                                field.setAccessible(true);
                                field.set(dialog, false);       // 按键不返回
                            } catch(NoSuchFieldException e) {
                            } catch(IllegalAccessException e) {
                            }

                            mButtonClickCount++;
                            if(mButtonClickCount != 3) return;
                            mButtonClickCount = 0;

                            synchronized (GpioTestPopWindow.this) {
                                Debug.d(TAG, "Enter Bagink HX24LC[" + index + "] Set. ");
                                ExtGpio.rfidSwitch(RfidScheduler.LEVELS[index]);
                                try {Thread.sleep(50);} catch (Exception e) {}
                                try {
                                    int value = Integer.valueOf(editText.getText().toString());
                                    if(value >= 0 && value <= 255) {
                                        Debug.d(TAG, "Set Bagink HX24LC[" + index + " to " + value);
                                        SmartCard.writeHX24LC(Integer.valueOf(editText.getText().toString()));
                                    } else {
                                        ToastUtil.show(mContext, "Value should be in [0,255]");
//                                        editText.setText("Value should be in [0,255]");
//                                        editText.setTextColor(Color.RED);
                                        return;
                                    }
                                    field.set(dialog, true);        // 按键返回
                                } catch (NumberFormatException e) {
                                    Debug.e(TAG, e.getMessage());
                                    ToastUtil.show(mContext, e.getMessage());
//                                    editText.setText(e.getMessage());
//                                    editText.setTextColor(Color.RED);
                                    return;
                                } catch(IllegalAccessException e) {
                                }
                                Debug.d(TAG, "Quit Bagink HX24LC[" + index + "] Set. ");
                            }
                        }
                    }).setNegativeButton(R.string.str_btn_cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int i) {
                            Field field = null;
                            try{
                                field = dialog.getClass().getSuperclass().getDeclaredField("mShowing");
                                field.setAccessible(true);
                                field.set(dialog, true);        // 按键返回
                            } catch(NoSuchFieldException e) {
                            } catch(IllegalAccessException e) {
                            }
                        }
                    }).setNeutralButton("Write", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int i) {
                            if(mButtonClickCount > 0) {
                                mButtonClickCount++;
                            }
                        }
                    } ).create();
                    ad.show();
                }
            });

            mBaginkValveTV = (TextView)parent.findViewById(mBaginkValveResIDs[index]);
            mBaginkValveTV.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    synchronized (GpioTestPopWindow.this) {
                        Debug.d(TAG, "Enter Bagink[" + index + "] Set Valve. ");
                        ExtGpio.setValve(index, 1);
                        try {Thread.sleep(10);} catch (Exception e) {}
                        ExtGpio.setValve(index, 0);
                        Debug.d(TAG, "Quit Bagink[" + index + "' Set Valve. ");
                    }
                }
            });
        }

        private void showTestMessage() {
            mBaginkLevelTV.post(new Runnable() {
                @Override
                public void run() {
                    StringBuilder sb = new StringBuilder();
                    sb.append("LV: ");
                    for(int i=0; i<mLevels.size(); i++) {
                        if(i == 0) sb.append(mLevels.get(i)); else sb.append(", ").append(mLevels.get(i));
                    }
                    sb.append("\n");
                    sb.append("HX: ");
                    for(int i=0; i<mHX24LCValues.size(); i++) {
                        if(i == 0) sb.append(mHX24LCValues.get(i)); else sb.append(", ").append(mHX24LCValues.get(i));
                    }
                    mBaginkLevelTV.setText(sb.toString());
                    mBaginkLevelTV.setTextSize(24);
                }
            });
        }
    }

// End of H.M.Wang 2022-12-24 将读BaginkLevel的功能独立为一个函数，并且加入线程互斥，还有增加HX24LC的读写功能

    private void resetOutPins() {
        for (int i = 0; i < mOutPins.length; i++) {
            if(i != 7) {
                try {
                    ExtGpio.writeGpioTestPin(OUT_PINS[i].charAt(1), Integer.valueOf(OUT_PINS[i].substring(2)), PIN_DISABLE);
                } catch (NumberFormatException e) {
                    Debug.e(TAG, e.getMessage());
                }
            } else {
                // 串口
            }
            mOutPins[i].setBackgroundColor(Color.GRAY);
        }
    }

    private void resetInPins() {
        for (int i = 0; i < mInPins.length; i++) {
            mInPins[i].setBackgroundColor(Color.GRAY);
        }
    }

    private boolean mBeepOn = false;
    private boolean mAutoTest = false;

    private boolean toggleOutPin(int index) {
        boolean enable = false;

        if (index != 7) {
            try {
                if(index == 2 && !mAutoTest) {
                    Thread.sleep(1000);
                    mBeepOn = !mBeepOn;
                    enable = mBeepOn;
                } else {
                    enable = !(ExtGpio.readGpioTestPin(OUT_PINS[index].charAt(1), Integer.valueOf(OUT_PINS[index].substring(2))) == 0 ? false : true);
                }
//                Debug.d(TAG, "Value = " + enable);
                ExtGpio.writeGpioTestPin(OUT_PINS[index].charAt(1), Integer.valueOf(OUT_PINS[index].substring(2)), (enable ? 1 : 0));
                enable = ExtGpio.readGpioTestPin(OUT_PINS[index].charAt(1), Integer.valueOf(OUT_PINS[index].substring(2))) == 0 ? false : true;
            } catch (NumberFormatException e) {
                Debug.e(TAG, e.getMessage());
                return false;
            } catch (InterruptedException e) {
                Debug.e(TAG, e.getMessage());
                return false;
            }
        } else {
            // 写串口
            mSerialWritting = !mSerialWritting;
            enable = mSerialWritting;
            if(mSerialWritting) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        SerialPort sp = SerialHandler.getInstance().getSerialPort();
                        if(null != sp) {
                            while(mSerialWritting) {
                                sp.writeSerial(new byte[]{0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00});
                            }
                        }
                    }
                }).start();
            }
        }

        mOutPins[index].setBackgroundColor(enable ? Color.GREEN : Color.GRAY);

        mHandler.removeMessages(MSG_TEST_IN_PIN);
        int value = ExtGpio.readGpioTestPin(IN_PINS[index].charAt(1), Integer.valueOf(IN_PINS[index].substring(2)));
        if(value != 0) {
            if(enable) {
                mInPins[index].setBackgroundColor(Color.RED);
                Message msg = mHandler.obtainMessage(MSG_TEST_IN_PIN);
                msg.arg1 = index;
                mHandler.sendMessageDelayed(msg, 10);
            } else {
                mInPins[index].setBackgroundColor(Color.GRAY);
            }
        } else {
            mInPins[index].setBackgroundColor(Color.GREEN);
        }

        return true;
    }

    private void testInPin(int index) {
        int value = ExtGpio.readGpioTestPin(IN_PINS[index].charAt(1), Integer.valueOf(IN_PINS[index].substring(2)));
        if(value != 0) {
            mInPins[index].setBackgroundColor(Color.RED);
            Message msg = mHandler.obtainMessage(MSG_TEST_IN_PIN);
            msg.arg1 = index;
            mHandler.sendMessageDelayed(msg, 10);
        } else {
            mInPins[index].setBackgroundColor(Color.GREEN);
        }
    }

    private void updateOutPins() {
        for (int i = 0; i < OUT_PINS.length; i++) {
            try {
                Message.obtain(mHandler, MESSAGE_SET_BGCOLOR,
                        ExtGpio.readGpioTestPin(OUT_PINS[i].charAt(1), Integer.valueOf(OUT_PINS[i].substring(2))),
                        i, mOutPins[i]).sendToTarget();
            } catch (NumberFormatException e) {
                Debug.e(TAG, e.getMessage());
            }
        }
    }

    private void updateInPins() {
        for (int i = 0; i < IN_PINS.length; i++) {
            try {
                Message.obtain(mHandler, MESSAGE_SET_BGCOLOR,
                        ExtGpio.readGpioTestPin(IN_PINS[i].charAt(1), Integer.valueOf(IN_PINS[i].substring(2))) == 0 ? PIN_ENABLE : PIN_DISABLE,
                        i, mInPins[i]).sendToTarget();
            } catch (NumberFormatException e) {
                Debug.e(TAG, e.getMessage());
            }
        }
    }
}
