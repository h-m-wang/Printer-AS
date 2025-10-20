package com.industry.printer.ui.Test;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.industry.printer.FileFormat.ImExPort;
import com.industry.printer.R;
import com.industry.printer.Serial.SerialHandler;
import com.industry.printer.Utils.ByteArrayUtils;
import com.industry.printer.Utils.ConfigPath;
import com.industry.printer.Utils.Configs;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.LibUpgrade;
import com.industry.printer.Utils.PlatformInfo;
import com.industry.printer.WelcomeActivity;
import com.industry.printer.hardware.ExtGpio;
import com.industry.printer.hardware.FpgaGpioOperation;
import com.industry.printer.hardware.IInkDevice;
import com.industry.printer.hardware.InkManagerFactory;
import com.industry.printer.hardware.RTCDevice;
import com.industry.printer.ui.CustomerDialog.ConfirmDialog;
import com.industry.printer.ui.CustomerDialog.DialogListener;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;

/*
  测试8个输出口（写）和8个输入口（读）的电平变化
 */
public class TestGpioPinsNew implements ITestOperation {
    public static final String TAG = TestGpioPinsNew.class.getSimpleName();

    private Context mContext = null;
    private FrameLayout mContainer = null;
    private LinearLayout mTestAreaLL = null;

// H.M.Wang 2025-7-3 区分标准版本img和hp22mm版本img的输入输出管脚
/*    private static final String[] IN_PINS = new String[] {
            "PG0", "PI5", "PI6", "PE7", "PE8", "PE9", "PE10", "PE11"
    };

    private static final String[] OUT_PINS = new String[] {
            "PI8", "PB11", "PG4", "PH26", "PH27", "PE4", "PE5", ""
    };*/
    private static final String[] IN_PINS_STD = new String[] {
            "PG0", "PI5", "PI6", "PE7", "PE8", "PE9", "PE10", "PE11"
    };

    private static final String[] OUT_PINS_STD = new String[] {
            "PI8", "PB11", "PG4", "PH26", "PH27", "PE4", "PE5", ""
    };
    private static final String[] IN_PINS_HP22MM = new String[] {
            "PI7", "PG1", "PH26", "PG2", "PG8", "PG9", "PE8", "PE9"
    };

    private static final String[] OUT_PINS_HP22MM = new String[] {
            "PI8", "PB11", "PG4", "PI9", "PG0", "", "", ""
    };
    private String[] IN_PINS;
    private String[] OUT_PINS;
// End of H.M.Wang 2025-7-3 区分标准版本img和hp22mm版本img的输入输出管脚

    private static final String[] OUT_PIN_TITLES = new String[] {
            "OUT-1", "OUT-2", "OUT-3", "OUT-4", "OUT-5", "ValveOut2", "ValveOut1", ""
    };

    private static final String[] IN_PIN_TITLES = new String[] {
            "IN-1", "IN-2", "IN-3", "IN-4", "IN-5", "IN-6", "IN-7", "IN-8"
    };

    private final int PIN_ENABLE = 1;
    private final int PIN_DISABLE = 0;

    private final int COLOR_OUT_OF_CONTROL = Color.RED;
    private final int COLOR_DISABLED = Color.GRAY;
    private final int COLOR_ENABLED = Color.GREEN;

    private LinearLayout mOutPinLayout = null;
    private LinearLayout mInPinLayout = null;
    private TextView[] mOutPins = null;
    private TextView[] mInPins = null;
    private TextView mGpioTestBtn = null;

    private TextView mInitTest = null;
    private LinearLayout mEtherTest = null;
    private ProgressBar mEthProgress = null;
    private LinearLayout mSerialTest = null;
    private ProgressBar mSerialProgress = null;
    private TextView mRFIDTest = null;

    private TextView mResImport = null;
    private TextView mResFPGA = null;
    private TextView mResKOs = null;
    private TextView mResIME = null;
    private TextView mResCNTs = null;
    private TextView mResTime = null;
    private TextView mResEth = null;
    private TextView mResSerial = null;
// H.M.Wang 2024-5-2 追加一个FPGA升级的进度查询命令
    private TextView mProgressMsg = null;
// End of H.M.Wang 2024-5-2 追加一个FPGA升级的进度查询命令

    private Thread mInPinsThread = null;
    private boolean mRunning = false;
    private boolean mGpioPinTesting = false;

    private final String TITLE = "M9测试";

    private final int MSG_PINSTEST_NEXT = 103;
    private final int MSG_TERMINATE_TEST = 105;
    private final int MSG_DISP_OUT_PINS = 106;
    private final int MSG_DISP_IN_PINS = 107;
    private final int MSG_DISP_TEST_RESULT = 108;
    private final int MSG_TEST_RESULT_ERROR = 109;
// H.M.Wang 2024-5-2 追加一个FPGA升级的进度查询命令
    private final int MSG_SHOW_FPGA_UPGRADING_PROGRESS = 119;
    private final int MSG_HIDE_FPGA_UPGRADING_PROGRESS = 120;
// End of H.M.Wang 2024-5-2 追加一个FPGA升级的进度查询命令
// H.M.Wang 2025-10-15 增加升级IME时的进度显示
    private final int MSG_SHOW_IME_UPGRADING_PROGRESS = 121;
    private final int MSG_HIDE_IME_UPGRADING_PROGRESS = 122;
// End of H.M.Wang 2025-10-15 增加升级IME时的进度显示

    private final int TEST_SEC_IMPORT = 0;
    private final int TEST_SEC_WRITE_FPGA = 1;
    private final int TEST_SEC_UPDATE_KOS = 2;
    private final int TEST_SEC_UPDATE_IME = 7;
    private final int TEST_SEC_RESET_CNTS = 3;
    private final int TEST_SEC_RESET_TIME = 4;
    private final int TEST_SEC_ETHERNET = 5;
    private final int TEST_SEC_SERIAL = 6;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_PINSTEST_NEXT:
                    if(msg.arg1 >= OUT_PINS.length) {
                        sendEmptyMessage(MSG_TERMINATE_TEST);
                        break;
                    }
                    toggleOutPin(msg.arg1);
                    Message nmsg = obtainMessage(MSG_PINSTEST_NEXT, msg.arg1 + 1, 0);
                    sendMessageDelayed(nmsg, 1000);
                    break;
                case MSG_DISP_OUT_PINS:
                    mOutPins[msg.arg1].setBackgroundColor(msg.arg2 == 0 ? COLOR_DISABLED : COLOR_ENABLED);
                    break;
                case MSG_DISP_IN_PINS:
                    mInPins[msg.arg1].setBackgroundColor(msg.arg2 == 0 ? COLOR_ENABLED : COLOR_DISABLED);
                    break;
                case MSG_TERMINATE_TEST:
                    mGpioPinTesting = false;
                    mGpioTestBtn.setBackgroundColor(Color.GREEN);
                    mGpioTestBtn.setEnabled(true);
                    break;
                case MSG_DISP_TEST_RESULT:
                    if(msg.arg1 == TEST_SEC_IMPORT) {
                        mResImport.setBackgroundColor(msg.arg2);
                    } if(msg.arg1 == TEST_SEC_WRITE_FPGA) {
                        mResFPGA.setBackgroundColor(msg.arg2);
// H.M.Wang 2025-10-15 升级成功后提示重启
                        if(msg.arg2 == Color.GREEN) new AlertDialog.Builder(mContext).setMessage(R.string.str_urge2restart).create().show();
// End of H.M.Wang 2025-10-15 升级成功后提示重启
                    } if(msg.arg1 == TEST_SEC_UPDATE_KOS) {
                        mResKOs.setBackgroundColor(msg.arg2);
// H.M.Wang 2025-10-15 升级成功后提示重启
                        if(msg.arg2 == Color.GREEN) new AlertDialog.Builder(mContext).setMessage(R.string.str_urge2restart).create().show();
// End of H.M.Wang 2025-10-15 升级成功后提示重启
                    } if(msg.arg1 == TEST_SEC_UPDATE_IME) {
                        mResIME.setBackgroundColor(msg.arg2);
// H.M.Wang 2025-10-15 升级成功后提示重启
                        if(msg.arg2 == Color.GREEN) new AlertDialog.Builder(mContext).setMessage(R.string.str_urge2restart).create().show();
// End of H.M.Wang 2025-10-15 升级成功后提示重启
                    } if(msg.arg1 == TEST_SEC_RESET_CNTS) {
                        mResCNTs.setBackgroundColor(msg.arg2);
                    } if(msg.arg1 == TEST_SEC_RESET_TIME) {
                        mResTime.setBackgroundColor(msg.arg2);
                    } if(msg.arg1 == TEST_SEC_ETHERNET) {
                        mResEth.setBackgroundColor(msg.arg2);
                        if(msg.arg2 == Color.GREEN) mResEth.setText(msg.obj + "ms");
                    } if(msg.arg1 == TEST_SEC_SERIAL) {
                        mResSerial.setBackgroundColor(msg.arg2);
                    }
                    if(msg.arg2 == Color.RED) ExtGpio.playClick();
                    break;
                case MSG_SHOW_FPGA_UPGRADING_PROGRESS:
                    mProgressMsg.setVisibility(View.VISIBLE);
                    int prog = FpgaGpioOperation.getUpgradingProgress();
// H.M.Wang 2025-9-2 修改进度的显示方法，0-100为写入进度，100-200为验证进度
                    if((prog%1000) < 100) {
                        mProgressMsg.setText("Upgrading: " + (prog/1000 + 1) + ", " + (prog%100) + "%");
                    } else {
                        mProgressMsg.setText("Verifying: " + (prog/1000 + 1) + ", " + (prog%100) + "%");
                    }
//                    mProgressMsg.setText("FPGA Upgrading: " + (prog/1000 + 1) + ", " + (prog%1000) + "%");
// End of H.M.Wang 2025-9-2 修改进度的显示方法，0-100为写入进度，100-200为验证进度
                    sendMessageDelayed(obtainMessage(MSG_SHOW_FPGA_UPGRADING_PROGRESS), 500);
                    break;
                case MSG_HIDE_FPGA_UPGRADING_PROGRESS:
                    removeMessages(MSG_SHOW_FPGA_UPGRADING_PROGRESS);
                    mProgressMsg.setVisibility(View.GONE);
                    break;
// H.M.Wang 2025-10-15 增加升级IME时的进度显示
                case MSG_SHOW_IME_UPGRADING_PROGRESS:
                    mProgressMsg.setVisibility(View.VISIBLE);
                    mProgressMsg.setText("Upgrading: " + msg.arg1 + "%");
                    if(msg.arg1 < 100)
                        sendMessageDelayed(obtainMessage(MSG_SHOW_IME_UPGRADING_PROGRESS, msg.arg1+10, 0), 800);
                    break;
                case MSG_HIDE_IME_UPGRADING_PROGRESS:
                    removeMessages(MSG_SHOW_IME_UPGRADING_PROGRESS);
                    mProgressMsg.setVisibility(View.GONE);
                    break;
// End of H.M.Wang 2025-10-15 增加升级IME时的进度显示
            }
        }
    };

    public TestGpioPinsNew(Context ctx, int index) {
        mContext = ctx;
// H.M.Wang 2025-7-3 区分标准版本img和hp22mm版本img的输入输出管脚
        if(PlatformInfo.getImgUniqueCode().startsWith("22MM")) {
            IN_PINS = IN_PINS_HP22MM;
            OUT_PINS = OUT_PINS_HP22MM;
        } else {
            IN_PINS = IN_PINS_STD;
            OUT_PINS = OUT_PINS_STD;
        }
// End of H.M.Wang 2025-7-3 区分标准版本img和hp22mm版本img的输入输出管脚
    }

    private boolean mSerialRecv = false;

    private void testImport() {
        final ArrayList<String> usbs = ConfigPath.getMountedUsb();
        if (usbs.size() <= 0) {
            mHandler.obtainMessage(MSG_DISP_TEST_RESULT, TEST_SEC_IMPORT, Color.RED).sendToTarget();
            return;
        }

        for(String path : usbs) {
            new File(path).delete();
        }

        ImExPort importTest = new ImExPort(mContext);
        importTest.msgImport(usbs);
        mHandler.obtainMessage(MSG_DISP_TEST_RESULT, TEST_SEC_IMPORT, Color.GREEN).sendToTarget();
    }

    private void testWriteFPGA() {
// H.M.Wang 2025-7-14 A133的22mm机型无PI7的切换
        if(!PlatformInfo.isA133Product() || !PlatformInfo.getImgUniqueCode().startsWith("22MM")) {
            ExtGpio.writeGpioTestPin('I', 7, 0);
        }
// End of H.M.Wang 2025-7-14 A133的22mm机型无PI7的切换
        mHandler.obtainMessage(MSG_SHOW_FPGA_UPGRADING_PROGRESS).sendToTarget();
        if (0 == FpgaGpioOperation.updateFlash()) {
            mHandler.obtainMessage(MSG_DISP_TEST_RESULT, TEST_SEC_WRITE_FPGA, Color.GREEN).sendToTarget();
        } else {
            mHandler.obtainMessage(MSG_DISP_TEST_RESULT, TEST_SEC_WRITE_FPGA, Color.RED).sendToTarget();
        }
        mHandler.obtainMessage(MSG_HIDE_FPGA_UPGRADING_PROGRESS).sendToTarget();
// H.M.Wang 2025-7-14 A133的22mm机型无PI7的切换
        if(!PlatformInfo.isA133Product() || !PlatformInfo.getImgUniqueCode().startsWith("22MM")) {
            ExtGpio.writeGpioTestPin('I', 7, 1);
        }
// End of H.M.Wang 2025-7-14 A133的22mm机型无PI7的切换
    }

    private void testUpgradeKOs() {
        LibUpgrade libUp = new LibUpgrade();
        boolean ret = libUp.upgradeKOs();

        if(ret) {
            mHandler.obtainMessage(MSG_DISP_TEST_RESULT, TEST_SEC_UPDATE_KOS, Color.GREEN).sendToTarget();
        } else {
            mHandler.obtainMessage(MSG_DISP_TEST_RESULT, TEST_SEC_UPDATE_KOS, Color.RED).sendToTarget();
        }
    }

    private void testUpgradeIME() {
        LibUpgrade libUp = new LibUpgrade();
        boolean ret = libUp.updateIME(Configs.UPGRADE_IME_APK, Configs.SYSTEM_IME_PATH);

        if(ret) {
            mHandler.obtainMessage(MSG_SHOW_IME_UPGRADING_PROGRESS, 0, 0).sendToTarget();
            try {Runtime.getRuntime().exec("sync");} catch (IOException e) {}
            try{Thread.sleep(10* 1000);}catch(Exception e){}
            mHandler.obtainMessage(MSG_HIDE_IME_UPGRADING_PROGRESS).sendToTarget();
            mHandler.obtainMessage(MSG_DISP_TEST_RESULT, TEST_SEC_UPDATE_IME, Color.GREEN).sendToTarget();
        } else {
            mHandler.obtainMessage(MSG_DISP_TEST_RESULT, TEST_SEC_UPDATE_IME, Color.RED).sendToTarget();
        }
    }

    private void testSerial() {
        mSerialTest.post(new Runnable() {
            @Override
            public void run() {
                mSerialTest.setEnabled(false);
                mSerialTest.setBackgroundColor(Color.GRAY);
                mSerialProgress.setVisibility(View.VISIBLE);
            }
        });
        SerialHandler sh = SerialHandler.getInstance();

        mSerialRecv = false;

        // 未选通串口时如果动作则为错
        ExtGpio.writeGpioTestPin(OUT_PINS[1].charAt(1), Integer.valueOf(OUT_PINS[1].substring(2)), PIN_DISABLE);
        sh.sendTestString("123456", new SerialHandler.ReadSerialListener() {
            @Override
            public void onRecvSerial(byte[] msg) {
                mSerialRecv = true;
            }
        });

        try{ Thread.sleep(3000);}catch(Exception e){}

        if(mSerialRecv) {
            mHandler.obtainMessage(MSG_DISP_TEST_RESULT, TEST_SEC_SERIAL, Color.RED).sendToTarget();
        } else {
//            mHandler.obtainMessage(MSG_DISP_TEST_RESULT, TEST_SEC_SERIAL, Color.GREEN).sendToTarget();
        }

        try{ Thread.sleep(3000);}catch(Exception e){}

        mSerialRecv = false;

        // 选通串口时如果不动作则为错
        ExtGpio.writeGpioTestPin(OUT_PINS[1].charAt(1), Integer.valueOf(OUT_PINS[1].substring(2)), PIN_ENABLE);

        for(int i=0; i<400 && !mSerialRecv; i++) {
            try{ Thread.sleep(500);}catch(Exception e){}

            sh.sendTestString("123456", new SerialHandler.ReadSerialListener() {
                @Override
                public void onRecvSerial(byte[] msg) {
                    String rmsg = new String(msg);
                    if(rmsg.startsWith("123456")) {
                        mSerialRecv = true;
                    } else {
                        mSerialRecv = false;
                    }
                }
            });
        }
        sh.sendTestString("123456", null);

        if(!mSerialRecv) {
            mHandler.obtainMessage(MSG_DISP_TEST_RESULT, TEST_SEC_SERIAL, Color.RED).sendToTarget();
        } else {
            mHandler.obtainMessage(MSG_DISP_TEST_RESULT, TEST_SEC_SERIAL, Color.GREEN).sendToTarget();
        }

        mSerialTest.post(new Runnable() {
            @Override
            public void run() {
                mSerialTest.setEnabled(true);
                mSerialTest.setBackgroundColor(Color.GREEN);
                mSerialProgress.setVisibility(View.GONE);
            }
        });
    }

    private void testRFID() {
        int rfid_ids[] = new int[] {R.id.rfid0, R.id.rfid1, R.id.rfid2, R.id.rfid3};
        IInkDevice inkManager = InkManagerFactory.inkManager(mContext);
        for(int i=0; i<4; i++) {
            TextView rfid = (TextView) mTestAreaLL.findViewById(rfid_ids[i]);
            if(inkManager.isValid(i)) {
                rfid.setBackgroundColor(Color.GREEN);
            } else {
                rfid.setBackgroundColor(Color.RED);
            }
        }
    }

    private void testInit() {
        mInitTest.post(new Runnable() {
            @Override
            public void run() {
                mInitTest.setEnabled(false);
                mInitTest.setBackgroundColor(Color.GRAY);
            }
        });

        testImport();
        try{Thread.sleep(3000);}catch (Exception e){}

        testWriteFPGA();
        try{Thread.sleep(3000);}catch (Exception e){}

        testUpgradeKOs();
        try{Thread.sleep(3000);}catch (Exception e){}

        Calendar c = Calendar.getInstance();
        c.set(2020, 2, 1, 3, 20,0);
        long when = c.getTimeInMillis();
        if (when / 1000 < Integer.MAX_VALUE) {
            SystemClock.setCurrentTimeMillis(when);
        }
        RTCDevice rtcDevice = RTCDevice.getInstance(mContext);
        rtcDevice.syncSystemTimeToRTC(mContext);
        mHandler.obtainMessage(MSG_DISP_TEST_RESULT, TEST_SEC_RESET_TIME, Color.GREEN).sendToTarget();
        try{Thread.sleep(3000);}catch (Exception e){}

        rtcDevice.write(0,9);
        mHandler.obtainMessage(MSG_DISP_TEST_RESULT, TEST_SEC_RESET_CNTS, Color.GREEN).sendToTarget();

        mInitTest.post(new Runnable() {
            @Override
            public void run() {
                mInitTest.setEnabled(true);
                mInitTest.setBackgroundColor(Color.GREEN);
            }
        });
    }

    @Override
    public void show(FrameLayout f) {
        mContainer = f;

        mTestAreaLL = (LinearLayout)LayoutInflater.from(mContext).inflate(R.layout.test_gpio_pins_new, null);

        mOutPinLayout = (LinearLayout) mTestAreaLL.findViewById(R.id.out_pin_area);
        mOutPins = new TextView[OUT_PINS.length];
        for(int i=0; i<OUT_PINS.length; i++) {
            TextView tv = new TextView(mContext);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0,1,0,1);
            tv.setLayoutParams(lp);
            tv.setPadding(0,3,0,3);
            tv.setGravity(Gravity.CENTER);
            tv.setBackgroundColor(COLOR_DISABLED);
            tv.setTextColor(Color.BLACK);
            tv.setTextSize(20);
            tv.setTag(i);
            tv.setText(OUT_PIN_TITLES[i] + " (" + (OUT_PINS[i].length() > 0 ? OUT_PINS[i] : " - ") + ")");
            tv.setOnClickListener(mOutPinBtnClickListener);
            mOutPinLayout.addView(tv);
            mOutPins[i] = tv;
        }

        mInPinLayout = (LinearLayout) mTestAreaLL.findViewById(R.id.in_pin_area);
        mInPins = new TextView[IN_PINS.length];
        for(int i=0; i<IN_PINS.length; i++) {
            TextView tv = new TextView(mContext);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0,1,0,1);
            tv.setLayoutParams(lp);
            tv.setPadding(0,3,0,3);
            tv.setGravity(Gravity.CENTER);
            tv.setBackgroundColor(COLOR_DISABLED);
            tv.setTextColor(Color.BLACK);
            tv.setTextSize(20);
            tv.setTag(i);
            tv.setText(IN_PIN_TITLES[i] + " (" + IN_PINS[i] + ")");
            mInPinLayout.addView(tv);
            mInPins[i] = tv;
        }

// H.M.Wang 2025-4-3 追加PG589的测试按键
        TextView pg5 = (TextView) mTestAreaLL.findViewById(R.id.test_pg5);
        if(ExtGpio.readGpioTestPin('G', 5) == 0) {
            pg5.setBackgroundColor(COLOR_DISABLED);
            pg5.setTag(0);
        } else {
            pg5.setBackgroundColor(COLOR_ENABLED);
            pg5.setTag(1);
        }
        pg5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if((Integer) (view.getTag()) == 0) {
                    ExtGpio.writeGpioTestPin('G', 5, 1);
                } else {
                    ExtGpio.writeGpioTestPin('G', 5, 0);
                }
                if(ExtGpio.readGpioTestPin('G', 5) == 0) {
                    view.setBackgroundColor(COLOR_DISABLED);
                    view.setTag(0);
                } else {
                    view.setBackgroundColor(COLOR_ENABLED);
                    view.setTag(1);
                }
            }
        });
        TextView pg8 = (TextView) mTestAreaLL.findViewById(R.id.test_pg8);
        if(ExtGpio.readGpioTestPin('G', 8) == 0) {
            pg8.setBackgroundColor(COLOR_DISABLED);
            pg8.setTag(0);
        } else {
            pg8.setBackgroundColor(COLOR_ENABLED);
            pg8.setTag(1);
        }
        pg8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if((Integer) (view.getTag()) == 0) {
                    ExtGpio.writeGpioTestPin('G', 8, 1);
                } else {
                    ExtGpio.writeGpioTestPin('G', 8, 0);
                }
                if(ExtGpio.readGpioTestPin('G', 8) == 0) {
                    view.setBackgroundColor(COLOR_DISABLED);
                    view.setTag(0);
                } else {
                    view.setBackgroundColor(COLOR_ENABLED);
                    view.setTag(1);
                }
            }
        });
        TextView pg9 = (TextView) mTestAreaLL.findViewById(R.id.test_pg9);
        if(ExtGpio.readGpioTestPin('G', 9) == 0) {
            pg9.setBackgroundColor(COLOR_DISABLED);
            pg9.setTag(0);
        } else {
            pg9.setBackgroundColor(COLOR_ENABLED);
            pg9.setTag(1);
        }
        pg9.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if((Integer) (view.getTag()) == 0) {
                    ExtGpio.writeGpioTestPin('G', 9, 1);
                } else {
                    ExtGpio.writeGpioTestPin('G', 9, 0);
                }
                if(ExtGpio.readGpioTestPin('G', 9) == 0) {
                    view.setBackgroundColor(COLOR_DISABLED);
                    view.setTag(0);
                } else {
                    view.setBackgroundColor(COLOR_ENABLED);
                    view.setTag(1);
                }
            }
        });
// End of H.M.Wang 2025-4-3 追加PG589的测试按键

        mGpioTestBtn = (TextView) mTestAreaLL.findViewById(R.id.gpio_test_btn);
        mGpioTestBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mGpioTestBtn.setBackgroundColor(Color.DKGRAY);
                mGpioTestBtn.setEnabled(false);
                resetOutPins();
                mGpioPinTesting = true;
                mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_PINSTEST_NEXT, 0, 0), 1000);
            }
        });

        TextView clearBtn = (TextView) mTestAreaLL.findViewById(R.id.clear_gpio_btn);
        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetOutPins();
            }
        });

        mResImport = (TextView) mTestAreaLL.findViewById(R.id.result_import);
        mResFPGA = (TextView) mTestAreaLL.findViewById(R.id.result_fpga);
        mResKOs = (TextView) mTestAreaLL.findViewById(R.id.result_kos);
        mResIME = (TextView) mTestAreaLL.findViewById(R.id.result_ime);
        mResCNTs = (TextView) mTestAreaLL.findViewById(R.id.result_counter);
        mResTime = (TextView) mTestAreaLL.findViewById(R.id.result_time);;
        mResEth = (TextView) mTestAreaLL.findViewById(R.id.result_eth);;
        mResSerial = (TextView) mTestAreaLL.findViewById(R.id.result_serial);;

// H.M.Wang 2024-5-2 追加一个FPGA升级的进度查询命令
        mProgressMsg = (TextView) mTestAreaLL.findViewById(R.id.progressMsg);
        mProgressMsg.setVisibility(View.GONE);
// End of H.M.Wang 2024-5-2 追加一个FPGA升级的进度查询命令

        mInitTest = (TextView) mTestAreaLL.findViewById(R.id.init_test_btn);
        mInitTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        testInit();
                    }
                }).start();
            }
        });

        mEtherTest = (LinearLayout) mTestAreaLL.findViewById(R.id.eth_test_btn);
        mEtherTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        testWifiResponseTime();
                    }
                }).start();
            }
        });
        mEthProgress = (ProgressBar) mTestAreaLL.findViewById(R.id.net_progressing);

        mSerialTest = (LinearLayout) mTestAreaLL.findViewById(R.id.serial_test_btn);
        mSerialTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        testSerial();
                    }
                }).start();
            }
        });
        mSerialProgress = (ProgressBar) mTestAreaLL.findViewById(R.id.serial_progressing);

        mRFIDTest = (TextView) mTestAreaLL.findViewById(R.id.rfid_test_btn);
        mRFIDTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                testRFID();
            }
        });

        final TextView writeFPGA = (TextView) mTestAreaLL.findViewById(R.id.write_fpga_btn);
        writeFPGA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ConfirmDialog cd = new ConfirmDialog(mContext, "Upgrade FPGA firmware?");
                cd.setListener(new DialogListener() {
                    @Override
                    public void onConfirm() {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                writeFPGA.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        writeFPGA.setEnabled(false);
                                        writeFPGA.setBackgroundColor(Color.GRAY);
                                    }
                                });
                                testWriteFPGA();
                                writeFPGA.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        writeFPGA.setEnabled(true);
                                        writeFPGA.setBackgroundColor(Color.GREEN);
                                    }
                                });
                            }
                        }).start();
                    }
                    public void onCancel() {
                    }
                });
                cd.show();
            }
        });

        final TextView updateKOs = (TextView) mTestAreaLL.findViewById(R.id.updateKOs);
        updateKOs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ConfirmDialog cd = new ConfirmDialog(mContext, "Upgrade KOs?");
                cd.setListener(new DialogListener() {
                    @Override
                    public void onConfirm() {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                updateKOs.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        updateKOs.setEnabled(false);
                                        updateKOs.setBackgroundColor(Color.GRAY);
                                    }
                                });
                                testUpgradeKOs();
                                updateKOs.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        updateKOs.setEnabled(true);
                                        updateKOs.setBackgroundColor(Color.GREEN);
                                    }
                                });
                            }
                        }).start();
                    }
                    public void onCancel() {
                    }
                });
                cd.show();
            }
        });

        final TextView upgradeIME = (TextView) mTestAreaLL.findViewById(R.id.upgrade_pinyinime_btn);
        upgradeIME.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ConfirmDialog cd = new ConfirmDialog(mContext, "Upgrade PinyinIME?");
                cd.setListener(new DialogListener() {
                    @Override
                    public void onConfirm() {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                upgradeIME.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        upgradeIME.setEnabled(false);
                                        upgradeIME.setBackgroundColor(Color.GRAY);
                                    }
                                });
                                testUpgradeIME();
                                upgradeIME.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        upgradeIME.setEnabled(true);
                                        upgradeIME.setBackgroundColor(Color.GREEN);
                                    }
                                });
                            }
                        }).start();
                    }
                    public void onCancel() {
                    }
                });
                cd.show();
            }
        });

        final TextView totalBtn = (TextView) mTestAreaLL.findViewById(R.id.total_test_btn);
        totalBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        totalBtn.post(new Runnable() {
                            @Override
                            public void run() {
                                totalBtn.setEnabled(false);
                                totalBtn.setBackgroundColor(Color.GRAY);
                            }
                        });

                        mGpioPinTesting = true;
                        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_PINSTEST_NEXT, 0, 0), 1000);

                        while(mGpioPinTesting) {
                            try{Thread.sleep(1000);}catch (Exception e){}
                        }

                        testInit();
                        try{Thread.sleep(3000);}catch (Exception e){}
                        testWifiResponseTime();
                        try{Thread.sleep(3000);}catch (Exception e){}
                        testSerial();

                        totalBtn.post(new Runnable() {
                            @Override
                            public void run() {
                                testRFID();
                                totalBtn.setEnabled(true);
                                totalBtn.setBackgroundColor(Color.GREEN);
                            }
                        });
                    }
                }).start();
            }
        });

        mContainer.addView(mTestAreaLL);

        resetOutPins();

        Calendar c = Calendar.getInstance();
        if(c.get(Calendar.YEAR) != 2020 || c.get(Calendar.MONTH) != 2 || c.get(Calendar.DAY_OF_MONTH) != 1) {
            mHandler.obtainMessage(MSG_DISP_TEST_RESULT, TEST_SEC_RESET_TIME, Color.RED).sendToTarget();
        } else {
            mHandler.obtainMessage(MSG_DISP_TEST_RESULT, TEST_SEC_RESET_TIME, Color.GREEN).sendToTarget();
        }

        RTCDevice rtcDevice = RTCDevice.getInstance(mContext);
        if(rtcDevice.read(9) != 0) {
            mHandler.obtainMessage(MSG_DISP_TEST_RESULT, TEST_SEC_RESET_CNTS, Color.RED).sendToTarget();
        } else {
            mHandler.obtainMessage(MSG_DISP_TEST_RESULT, TEST_SEC_RESET_CNTS, Color.GREEN).sendToTarget();
        }

        mInPinsThread = new Thread(new Runnable() {
            @Override
            public void run() {
                mRunning = true;
                while(mRunning) {
                    testOutPins();
                    testInPins();
                    try{Thread.sleep(200);}catch(Exception e){}
                }
            }
        });
        mInPinsThread.start();
    }

    private void testOutPins() {
        for (int i = 0; i < mOutPins.length; i++) {
            if(!OUT_PINS[i].isEmpty()) {
                int value = ExtGpio.readGpioTestPin(OUT_PINS[i].charAt(1), Integer.valueOf(OUT_PINS[i].substring(2)));
                mHandler.obtainMessage(MSG_DISP_OUT_PINS, i, value).sendToTarget();
            }
        }
    }

    private void testInPins() {
        for (int i = 0; i < mInPins.length; i++) {
            int value = ExtGpio.readGpioTestPin(IN_PINS[i].charAt(1), Integer.valueOf(IN_PINS[i].substring(2)));
            mHandler.obtainMessage(MSG_DISP_IN_PINS, i, value).sendToTarget();
        }
    }

    @Override
    public void setTitle(TextView tv) {
        tv.setText(TITLE);
    }

    @Override
    public boolean quit() {
        mContainer.removeView(mTestAreaLL);
        if(null != mInPinsThread && mInPinsThread.isAlive()) {
            mRunning = false;
            mInPinsThread.interrupt();
        }
        return true;
    }

    private View.OnClickListener mOutPinBtnClickListener = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            Integer index = (Integer)v.getTag();
            Debug.d(TAG, "Out Index: " + index);

            toggleOutPin(index);
        }
    };

    // 将输出口管脚全部设为DISABLE（拉低）
    private void resetOutPins() {
        mBeepOn = false;
        for (int i = 0; i < mOutPins.length; i++) {
            try {
                if(!OUT_PINS[i].isEmpty()) ExtGpio.writeGpioTestPin(OUT_PINS[i].charAt(1), Integer.valueOf(OUT_PINS[i].substring(2)), PIN_DISABLE);
            } catch (NumberFormatException e) {
                Debug.e(TAG, e.getMessage());
            }
        }
    }

    private boolean mBeepOn = false;

    private boolean toggleOutPin(int index) {
        boolean enable = false;

        if (index < 8 && !OUT_PINS[index].isEmpty()) {
            try {
                if(index == 2) {
                    mBeepOn = !mBeepOn;
                    enable = mBeepOn;
                } else {
                    enable = !(ExtGpio.readGpioTestPin(OUT_PINS[index].charAt(1), Integer.valueOf(OUT_PINS[index].substring(2))) == PIN_DISABLE ? false : true);
                }
                ExtGpio.writeGpioTestPin(OUT_PINS[index].charAt(1), Integer.valueOf(OUT_PINS[index].substring(2)), (enable ? PIN_ENABLE : PIN_DISABLE));
            } catch (NumberFormatException e) {
                Debug.e(TAG, e.getMessage());
                return false;
            }
        }

        return true;
    }

    private void testWifiResponseTime() {
        mEtherTest.post(new Runnable() {
            @Override
            public void run() {
                mEtherTest.setEnabled(false);
                mEtherTest.setBackgroundColor(Color.GRAY);
                mEthProgress.setVisibility(View.VISIBLE);
            }
        });

        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.forName("UTF-8")));

            os.writeBytes("ping -c 3 192.168.1.253\n");

            long tStart = System.currentTimeMillis();
            while(true) {
                if(!br.ready()) {
                    Thread.sleep(100);
                    if(System.currentTimeMillis() - tStart > 3000) {
                        mHandler.obtainMessage(MSG_DISP_TEST_RESULT, TEST_SEC_ETHERNET, Color.RED).sendToTarget();
                        break;
                    }
                    continue;
                }

                String str = br.readLine();
                Debug.d(TAG, "ETH: " + str);
                if(str.startsWith("rtt min/avg/max/mdev = ")) {
                    str = str.substring("rtt min/avg/max/mdev = ".length());
                    String strs[] = str.split("/");
                    mHandler.obtainMessage(MSG_DISP_TEST_RESULT, TEST_SEC_ETHERNET, Color.GREEN, strs[1]).sendToTarget();
                    break;
                }
            }
            br.close();
        } catch (ExceptionInInitializerError e) {
            Debug.e(TAG, "--->e: " + e.getMessage());
            mHandler.obtainMessage(MSG_DISP_TEST_RESULT, TEST_SEC_ETHERNET, Color.RED).sendToTarget();
        } catch (IOException e) {
            Debug.e(TAG, "--->e: " + e.getMessage());
            mHandler.obtainMessage(MSG_DISP_TEST_RESULT, TEST_SEC_ETHERNET, Color.RED).sendToTarget();
        } catch (Exception e) {
            Debug.e(TAG, "--->e: " + e.getMessage());
            mHandler.obtainMessage(MSG_DISP_TEST_RESULT, TEST_SEC_ETHERNET, Color.RED).sendToTarget();
        }

        mEtherTest.post(new Runnable() {
            @Override
            public void run() {
                mEtherTest.setEnabled(true);
                mEtherTest.setBackgroundColor(Color.GREEN);
                mEthProgress.setVisibility(View.GONE);
            }
        });
    }
}
