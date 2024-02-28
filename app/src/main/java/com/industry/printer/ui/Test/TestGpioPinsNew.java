package com.industry.printer.ui.Test;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;

import com.industry.printer.MainActivity;
import com.industry.printer.R;
import com.industry.printer.Serial.SerialHandler;
import com.industry.printer.Serial.SerialPort;
import com.industry.printer.Utils.ByteArrayUtils;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.ToastUtil;
import com.industry.printer.hardware.ExtGpio;
import com.industry.printer.hardware.SmartCardManager;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.List;

/*
  测试8个输出口（写）和8个输入口（读）的电平变化
 */
public class TestGpioPinsNew implements ITestOperation {
    public static final String TAG = TestGpioPinsNew.class.getSimpleName();

    private Context mContext = null;
    private FrameLayout mContainer = null;
    private LinearLayout mTestAreaLL = null;

    private int mSubIndex = 0;

    private static final String[] IN_PINS = new String[] {
            "PG3", "PI5", "PI6", "PE7", "PE8", "PE9", "PE10", "PE11"
    };

    private static final String[] OUT_PINS = new String[] {
            "PI8", "PB11", "PG4", "PH26", "PH27", "", "PE4", "PE5"
    };

    private static final String[] OUT_PIN_TITLES = new String[] {
            "OUT-1", "OUT-2", "OUT-3", "OUT-4", "OUT-5", "OUT-6", "ValveOut1", "ValveOut2"
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
    private TextView mLaunchTestBtn = null;
    private TextView mPintTime = null;

    private final String TITLE = "New GPIO Pin Test";

    private final int MSG_PINSTEST_NEXT = 103;
    private final int MSG_TERMINATE_TEST = 105;
    private final int MSG_TEST_IN_PINS = 106;
    private final int MSG_TEST_PING_TIME = 107;

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
                case MSG_TEST_IN_PINS:
                    testInPins();
                    break;
                case MSG_TERMINATE_TEST:
                    mLaunchTestBtn.setBackgroundColor(Color.GREEN);
                    mLaunchTestBtn.setEnabled(true);
                    mSerialWritting = false;
                    break;
                case MSG_TEST_PING_TIME:
                    mPintTime.setText("Ping finished in " + msg.obj + "ms");
                    break;
            }
        }
    };

    public TestGpioPinsNew(Context ctx, int index) {
        mContext = ctx;
        mSubIndex = index;
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
            tv.setPadding(0,5,0,5);
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
            tv.setPadding(0,5,0,5);
            tv.setGravity(Gravity.CENTER);
            tv.setBackgroundColor(COLOR_DISABLED);
            tv.setTextColor(Color.BLACK);
            tv.setTextSize(20);
            tv.setTag(i);
            tv.setText(IN_PIN_TITLES[i] + " (" + IN_PINS[i] + ")");
            tv.setOnClickListener(mInPinBtnClickListener);
            mInPinLayout.addView(tv);
            mInPins[i] = tv;
        }

        mLaunchTestBtn = (TextView) mTestAreaLL.findViewById(R.id.launch_test_btn);
        mLaunchTestBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mLaunchTestBtn.setBackgroundColor(Color.DKGRAY);
                mLaunchTestBtn.setEnabled(false);
                resetOutPins();
                testInPins();
                mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_PINSTEST_NEXT, 0, 0), 1000);
            }
        });

        mPintTime = (TextView) mTestAreaLL.findViewById(R.id.ping_time);

        CheckBox cb = (CheckBox) mTestAreaLL.findViewById(R.id.net_test);
        cb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                testWifiResponseTime();
            }
        });
        mContainer.addView(mTestAreaLL);

        mSerialWritting = false;
//        mAutoTest = true;

        resetOutPins();
        testInPins();
    }

    @Override
    public void setTitle(TextView tv) {
        tv.setText(TITLE);
    }

    @Override
    public boolean quit() {
        mContainer.removeView(mTestAreaLL);
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

    private View.OnClickListener mInPinBtnClickListener = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            Integer index = (Integer)v.getTag();
            Debug.d(TAG, "In Index: " + index);
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
            mOutPins[i].setBackgroundColor(COLOR_DISABLED);
        }
    }

    private boolean mBeepOn = false;
//    private boolean mAutoTest = false;
    private boolean mSerialWritting = false;

    private boolean toggleOutPin(int index) {
        boolean enable = false;

        if (index < 8 && !OUT_PINS[index].isEmpty()) {
            try {
                if(index == 2/* && !mAutoTest*/) {
//                    Thread.sleep(1000);
                    mBeepOn = !mBeepOn;
                    enable = mBeepOn;
                } else {
                    enable = !(ExtGpio.readGpioTestPin(OUT_PINS[index].charAt(1), Integer.valueOf(OUT_PINS[index].substring(2))) == PIN_DISABLE ? false : true);
                }
//                Debug.d(TAG, "Value = " + enable);
                ExtGpio.writeGpioTestPin(OUT_PINS[index].charAt(1), Integer.valueOf(OUT_PINS[index].substring(2)), (enable ? PIN_ENABLE : PIN_DISABLE));
                enable = ExtGpio.readGpioTestPin(OUT_PINS[index].charAt(1), Integer.valueOf(OUT_PINS[index].substring(2))) == PIN_DISABLE ? false : true;
            } catch (NumberFormatException e) {
                Debug.e(TAG, e.getMessage());
                return false;
//            } catch (InterruptedException e) {
//                Debug.e(TAG, e.getMessage());
//                return false;
            }
        }

        mOutPins[index].setBackgroundColor(enable ? COLOR_ENABLED : COLOR_DISABLED);

        mHandler.removeMessages(MSG_TEST_IN_PINS);
        mHandler.sendEmptyMessageDelayed(MSG_TEST_IN_PINS, 10);
        return true;
    }

    private void testInPins() {
        for (int i = 0; i < mInPins.length; i++) {
            int value = ExtGpio.readGpioTestPin(IN_PINS[i].charAt(1), Integer.valueOf(IN_PINS[i].substring(2)));
            Debug.d(TAG, "IN_PIN[" + i + "]= " + value);
            if(value != 0) {
                mInPins[i].setBackgroundColor(COLOR_DISABLED);
            } else {
                mInPins[i].setBackgroundColor(COLOR_ENABLED);
            }
        }
    }

    private static String int2ip(int ipInt) {
        StringBuilder sb = new StringBuilder();
        sb.append(ipInt & 0xFF).append(".");
        sb.append((ipInt >> 8) & 0xFF).append(".");
        sb.append((ipInt >> 16) & 0xFF).append(".");
        sb.append((ipInt >> 24) & 0xFF);
        return sb.toString();
    }

    private String testWifiResponseTime() {
        mPintTime.setText("Pinging...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo[] nis = connectivityManager.getAllNetworkInfo();

                if(null == nis) {
                    Debug.d(TAG, "NIS null");
                    return;
                }

                try {
                    Process process = Runtime.getRuntime().exec("su");
                    DataOutputStream os = new DataOutputStream(process.getOutputStream());
                    BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.forName("UTF-8")));

                    os.writeBytes("ping -c 3 169.254.173.207\n");
                    while(true) {
                        String str = br.readLine();
                        if(str.startsWith("rtt min/avg/max/mdev = ")) {
                            str = str.substring("rtt min/avg/max/mdev = ".length());
                            String strs[] = str.split("/");
                            Message msg = mHandler.obtainMessage();
                            msg.what = MSG_TEST_PING_TIME;
                            msg.obj = strs[1];
                            mHandler.sendMessage(msg);
                            Debug.d(TAG, strs[1]);
                            break;
                        }
                    }
                } catch (ExceptionInInitializerError e) {
                    Debug.e(TAG, "--->e: " + e.getMessage());
                } catch (Exception e) {
                    Debug.e(TAG, "--->e: " + e.getMessage());
                }

/*                try {
                    Socket socket = new Socket("169.254.173.207", 8899);
                    Debug.e(TAG, "Serial Send: 1234ABCD\n");
                    socket.getOutputStream().write("1234ABCD\n".getBytes());
                    socket.close();
                } catch (UnknownHostException e) {
                    Debug.e(TAG, "--->e: " + e.getMessage());
                } catch (Exception e) {
                    Debug.e(TAG, "--->e: " + e.getMessage());
                }*/
            }
        }).start();

        return "";
    }
}
