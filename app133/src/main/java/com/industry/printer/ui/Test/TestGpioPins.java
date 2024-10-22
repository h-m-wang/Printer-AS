package com.industry.printer.ui.Test;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;

import com.industry.printer.R;
import com.industry.printer.Serial.SerialHandler;
import com.industry.printer.Serial.SerialPort;
import com.industry.printer.Utils.Debug;
import com.industry.printer.hardware.ExtGpio;
import com.industry.printer.hardware.SmartCardManager;

/*
  测试8个输出口（写）和8个输入口（读）的电平变化
 */
public class TestGpioPins implements ITestOperation {
    public static final String TAG = TestGpioPins.class.getSimpleName();

    private Context mContext = null;
    private FrameLayout mContainer = null;
    private LinearLayout mTestAreaLL = null;

    private int mSubIndex = 0;

    private static final String[] IN_PINS = new String[] {
            "PG0", "PI5", "PI6", "PE7", "PE8", "PE9", "PE10", "PE11"
    };

    private static final String[] OUT_PINS = new String[] {
            "PI8", "PB11", "PG4", "PH26", "PH27", "PE4", "PE5", "Serial"
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

    private final String TITLE = "GPIO Pin Test";

    private final int MSG_PINSTEST_NEXT = 103;
    private final int MSG_TERMINATE_TEST = 105;
    private final int MSG_TEST_IN_PIN = 106;

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
                case MSG_TEST_IN_PIN:
                    testInPin(msg.arg1);
                    break;
                case MSG_TERMINATE_TEST:
                    mHandler.removeMessages(MSG_TEST_IN_PIN);
                    mSerialWritting = false;
                    break;
            }
        }
    };

    public TestGpioPins(Context ctx, int index) {
        mContext = ctx;
        mSubIndex = index;
    }

    @Override
    public void show(FrameLayout f) {
        mContainer = f;

        mTestAreaLL = (LinearLayout)LayoutInflater.from(mContext).inflate(R.layout.test_gpio_pins, null);

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
//            tv.setText(OUT_PINS[i]);
            tv.setText("Out-" + (i+1));
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
//            tv.setText(IN_PINS[i]);
            tv.setText("In-" + (i+1));
            tv.setOnClickListener(mInPinBtnClickListener);
            mInPinLayout.addView(tv);
            mInPins[i] = tv;
        }

        mContainer.addView(mTestAreaLL);

        resetOutPins();
        resetInPins();
        mSerialWritting = false;
        mAutoTest = true;
        mHandler.obtainMessage(MSG_PINSTEST_NEXT, 0, 0).sendToTarget();
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
            mOutPins[i].setBackgroundColor(COLOR_DISABLED);
        }
    }

    private void resetInPins() {
        for (int i = 0; i < mInPins.length; i++) {
            mInPins[i].setBackgroundColor(COLOR_DISABLED);
        }
    }

    private boolean mBeepOn = false;
    private boolean mAutoTest = false;
    private boolean mSerialWritting = false;

    private boolean toggleOutPin(int index) {
        boolean enable = false;

        if (index != 7) {
            try {
                if(index == 2 && !mAutoTest) {
                    Thread.sleep(1000);
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

        mOutPins[index].setBackgroundColor(enable ? COLOR_ENABLED : COLOR_DISABLED);

        mHandler.removeMessages(MSG_TEST_IN_PIN);
        int value = ExtGpio.readGpioTestPin(IN_PINS[index].charAt(1), Integer.valueOf(IN_PINS[index].substring(2)));
        if(value != 0) {
            if(enable) {
                mInPins[index].setBackgroundColor(COLOR_OUT_OF_CONTROL);       // 输入口不受控
                Message msg = mHandler.obtainMessage(MSG_TEST_IN_PIN);
                msg.arg1 = index;
                mHandler.sendMessageDelayed(msg, 10);
            } else {
                mInPins[index].setBackgroundColor(COLOR_DISABLED);      // 输入口已设置为失效
            }
        } else {
            mInPins[index].setBackgroundColor(COLOR_ENABLED);         // 输入口已设置为有效
        }

        return true;
    }

    private void testInPin(int index) {
        int value = ExtGpio.readGpioTestPin(IN_PINS[index].charAt(1), Integer.valueOf(IN_PINS[index].substring(2)));
        if(value != 0) {
            mInPins[index].setBackgroundColor(COLOR_OUT_OF_CONTROL);
            Message msg = mHandler.obtainMessage(MSG_TEST_IN_PIN);
            msg.arg1 = index;
            mHandler.sendMessageDelayed(msg, 10);
        } else {
            mInPins[index].setBackgroundColor(COLOR_ENABLED);
        }
    }
}
