package com.industry.printer.ui.Test;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.industry.printer.R;
import com.industry.printer.Rfid.RfidScheduler;
import com.industry.printer.Utils.Debug;
import com.industry.printer.hardware.ExtGpio;
import com.industry.printer.hardware.SmartCard;

public class TestSub implements ITestOperation {
    public static final String TAG = TestSub.class.getSimpleName();

    private Context mContext = null;
    private FrameLayout mContainer = null;
    private TextView mTitleTV = null;
    private ListView mSubMenuLV = null;
    private ITestOperation mIFTestOp = null;

    private final int SUB_TEST_INDEX_BAGINK = 0;
    private final int SUB_TEST_INDEX_SC = 1;
    private final int SUB_TEST_INDEX_ALBIG = 2;
    private int mSubTestIndex = 0;

    private final String TITLE = "SUB";

    private static final int ID_NOT_DEFINED = 0;
    private static final int ID_GPIO_PIN_TEST = 1;
    private static final int ID_LEVEL_ID_TEST = 2;
    private static final int ID_SC_INIT_TEST = 3;
    private static final int ID_READ_LEVEL_TEST = 4;
    private static final int ID_BAG_REDUCTION_TEST = 5;
    private static final int ID_SC_VALVE_ONOFF_TEST = 6;
// H.M.Wang 2025-3-7 追加一个自动开关阀试验，每隔10s开阀1次，持续1秒后关闭
    private static final int ID_SC_AUTO_VALVE_ONOFF_TEST = 7;
// End of H.M.Wang 2025-3-7 追加一个自动开关阀试验，每隔10s开阀1次，持续1秒后关闭
    private static final int ID_BAGINK_TEST = 8;
    private static final int ID_HP22MM_TEST = 9;
// H.M.Wang 2024-5-24 临时追加一个ADS1115芯片的读数功能
    private static final int ID_ADS1115_READING_TEST = 10;
// End of H.M.Wang 2024-5-24 临时追加一个ADS1115芯片的读数功能
// H.M.Wang 2024-7-4 追加一个MCP-H21系列芯片测量压力的读写功能
    private static final int ID_MCP_H21XXXX_READING_TEST = 11;
// End of H.M.Wang 2024-7-4 追加一个MCP-H21系列芯片测量压力的读写功能

// H.M.Wang 2024-5-24 临时追加一个ADS1115芯片的读数功能
    private AlertDialog mRecvedLevelPromptDlg = null;
    private boolean mADS1115Reading = false;
// End of H.M.Wang 2024-5-24 临时追加一个ADS1115芯片的读数功能
// H.M.Wang 2024-7-4 追加一个MCP-H21系列芯片测量压力的读写功能
    private boolean mMCPH21xxxxReading = false;
// End of H.M.Wang 2024-7-4 追加一个MCP-H21系列芯片测量压力的读写功能

    private class TestItem {
        public String mCaption;
        public int mTestID;

        public TestItem(String cap, int id) {
            mCaption = cap;
            mTestID = id;
        }
    }

    private TestItem[][] mTestItems = {
            {       // Bagink
                    new TestItem("Level ID Test", ID_LEVEL_ID_TEST),
                    new TestItem("Read Level Test", ID_READ_LEVEL_TEST),
                    new TestItem("Bag Reduction Test", ID_BAG_REDUCTION_TEST),
                    new TestItem("Bagink Test", ID_BAGINK_TEST),
                    new TestItem("GPIO Pin Test", ID_GPIO_PIN_TEST),
                    new TestItem("Hp22mm Test", ID_HP22MM_TEST),
// H.M.Wang 2024-7-4 追加一个MCP-H21系列芯片测量压力的读写功能
                    new TestItem("MCP-H21xxxx Reading Test", ID_MCP_H21XXXX_READING_TEST),
// End of H.M.Wang 2024-7-4 追加一个MCP-H21系列芯片测量压力的读写功能
            },
            {       // Smartcard
                    new TestItem("Level ID Test", ID_LEVEL_ID_TEST),
                    new TestItem("Read Level Test", ID_READ_LEVEL_TEST),
                    new TestItem("Bag Reduction Test", ID_BAG_REDUCTION_TEST),
                    new TestItem("Smartcard Init Test", ID_SC_INIT_TEST),
                    new TestItem("SmartCard Valve On/Off Test", ID_SC_VALVE_ONOFF_TEST),
// H.M.Wang 2025-3-7 追加一个自动开关阀试验，每隔10s开阀1次，持续1秒后关闭
                    new TestItem("Auto SmartCard Valve On/Off Test", ID_SC_AUTO_VALVE_ONOFF_TEST),
// End of H.M.Wang 2025-3-7 追加一个自动开关阀试验，每隔10s开阀1次，持续1秒后关闭
            },
            {       // ALBIG
                    new TestItem("GPIO Pin Test", ID_GPIO_PIN_TEST),
// H.M.Wang 2024-5-24 临时追加一个ADS1115芯片的读数功能
                    new TestItem("ADS1115 Reading", ID_ADS1115_READING_TEST),
// End of H.M.Wang 2024-5-24 临时追加一个ADS1115芯片的读数功能
            },
    };

    public TestSub(Context ctx, int index) {
        mContext = ctx;
        mSubTestIndex = index;
        mIFTestOp = null;
    }

    @Override
    public void show(FrameLayout f) {
        mContainer = f;

        mSubMenuLV = (ListView)LayoutInflater.from(mContext).inflate(R.layout.test_sub, null);
        mSubMenuLV.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return mTestItems[mSubTestIndex].length;
            }

            @Override
            public Object getItem(int i) {
                return mTestItems[mSubTestIndex][i];
            }

            @Override
            public long getItemId(int i) {
                return 0;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if(null == convertView) {
//                    convertView = ((LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.hp22mm_test_item, null);
                    convertView = LayoutInflater.from(mContext).inflate(R.layout.test_menu_item, null);
                }

                TextView itemTV = (TextView) convertView.findViewById(R.id.test_main_item);
                itemTV.setText(position < mTestItems[mSubTestIndex].length ? mTestItems[mSubTestIndex][position].mCaption : "");
                return convertView;
            }
        });

        mContainer.addView(mSubMenuLV);

        mSubMenuLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Debug.d(TAG, mTestItems[mSubTestIndex][i].mCaption);
                switch(mTestItems[mSubTestIndex][i].mTestID) {
                    case ID_GPIO_PIN_TEST:
                        mIFTestOp = new TestGpioPins(mContext, mSubTestIndex);
                        break;
                    case ID_LEVEL_ID_TEST:
                        mIFTestOp = new TestLevelID(mContext, mSubTestIndex);
                        break;
                    case ID_SC_INIT_TEST:
                        mIFTestOp = new TestSCInit(mContext, mSubTestIndex);
                        break;
                    case ID_READ_LEVEL_TEST:
                        mIFTestOp = new TestReadLevel(mContext, mSubTestIndex);
                        break;
                    case ID_BAG_REDUCTION_TEST:
                        mIFTestOp = new TestReduction(mContext, mSubTestIndex);
                        break;
                    case ID_SC_VALVE_ONOFF_TEST:
                        mIFTestOp = new TestValveOnOff(mContext, mSubTestIndex);
                        break;
// H.M.Wang 2025-3-7 追加一个自动开关阀试验，每隔10s开阀1次，持续1秒后关闭
                    case ID_SC_AUTO_VALVE_ONOFF_TEST:
                        mIFTestOp = new TestAutoValveOnOff(mContext, mSubTestIndex);
                        break;
// End of H.M.Wang 2025-3-7 追加一个自动开关阀试验，每隔10s开阀1次，持续1秒后关闭
                    case ID_BAGINK_TEST:
                        mIFTestOp = new TestBagink(mContext, mSubTestIndex);
                        break;
                    case ID_HP22MM_TEST:
                        mIFTestOp = new TestHp22mm(mContext, mSubTestIndex);
                        break;
// H.M.Wang 2024-5-24 临时追加一个ADS1115芯片的读数功能
                    case ID_ADS1115_READING_TEST:
                        if(null == mRecvedLevelPromptDlg) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                            mRecvedLevelPromptDlg = builder.setTitle("ADS1115 读值测试").setMessage("").setPositiveButton("关闭", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mADS1115Reading = false;
                                    mRecvedLevelPromptDlg.dismiss();
                                    mRecvedLevelPromptDlg = null;
                                }
                            }).create();

                            mADS1115Reading = true;
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    int ain0;
                                    while(mADS1115Reading) {
                                        if(!mADS1115Reading) break;
                                        ain0 = SmartCard.readADS1115(0);
                                        Debug.d(TAG, "ADS1115-AIN0 = " + ain0);
                                        if(null != mRecvedLevelPromptDlg) {
                                            final String showStr =
                                                    "AIN0: " + ain0 + " (0x" + Integer.toHexString(ain0).toUpperCase() + ")";
                                            mContainer.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    mRecvedLevelPromptDlg.setMessage(showStr);
                                                    if(!mRecvedLevelPromptDlg.isShowing()) {
                                                        mMCPH21xxxxReading = false;
                                                        mRecvedLevelPromptDlg.dismiss();
                                                        mRecvedLevelPromptDlg = null;
                                                    }
                                                }
                                            });
                                        }
                                        try{Thread.sleep(100L);}catch(Exception e){}
                                    }
                                }
                            }).start();
                        }
                        mRecvedLevelPromptDlg.show();
                        mIFTestOp = null;
                        break;
// End of H.M.Wang 2024-5-24 临时追加一个ADS1115芯片的读数功能
// H.M.Wang 2024-7-4 追加一个MCP-H21系列芯片测量压力的读写功能
                    case ID_MCP_H21XXXX_READING_TEST:
                        if(null == mRecvedLevelPromptDlg) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                            mRecvedLevelPromptDlg = builder.setTitle("MCP-H21xxxx 读值测试").setMessage("").setPositiveButton("关闭", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mMCPH21xxxxReading = false;
                                    mRecvedLevelPromptDlg.dismiss();
                                    mRecvedLevelPromptDlg = null;
                                }
                            }).create();

                            mMCPH21xxxxReading = true;
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    while(mMCPH21xxxxReading) {
                                        if(!mMCPH21xxxxReading) break;
                                        StringBuilder sb = new StringBuilder();
                                        for(int i=0; i< RfidScheduler.LEVELS.length; i++) {
                                            ExtGpio.rfidSwitch(RfidScheduler.LEVELS[i]);
                                            try{Thread.sleep(100);}catch(Exception e){};
                                            sb.append("Value[" + (i+1) + "] = " + SmartCard.readMCPH21Level(i) + "\n");
                                        }
                                        if(null != mRecvedLevelPromptDlg) {
                                            final String showStr = sb.toString();
                                            mContainer.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    mRecvedLevelPromptDlg.setMessage(showStr);
                                                    if(!mRecvedLevelPromptDlg.isShowing()) {
                                                        mMCPH21xxxxReading = false;
                                                        mRecvedLevelPromptDlg.dismiss();
                                                        mRecvedLevelPromptDlg = null;
                                                    }
                                                }
                                            });
                                        } else {
                                            mMCPH21xxxxReading = false;
                                            break;
                                        }
                                        try{Thread.sleep(100L);}catch(Exception e){}
                                    }
                                    Debug.d(TAG, "Quit MCPH21xxxxReading");
                                }
                            }).start();
                        }
                        mRecvedLevelPromptDlg.show();
                        mIFTestOp = null;
                        break;
// End of H.M.Wang 2024-7-4 追加一个MCP-H21系列芯片测量压力的读写功能
                    default:
                        mIFTestOp = null;
                        break;
                }
                if(null != mIFTestOp) {
// H.M.Wang 2024-5-24 临时追加一个ADS1115芯片的读数功能
                    mADS1115Reading = false;
                    if(null != mRecvedLevelPromptDlg) {
                        mRecvedLevelPromptDlg.dismiss();
                        mRecvedLevelPromptDlg = null;
                    }
// End of H.M.Wang 2024-5-24 临时追加一个ADS1115芯片的读数功能
                    mIFTestOp.show(mContainer);
                    mIFTestOp.setTitle(mTitleTV);
                    mSubMenuLV.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public void setTitle(TextView tv) {
        mTitleTV = tv;
        mTitleTV.setText(TITLE);
    }

    @Override
    public boolean quit() {
// H.M.Wang 2024-5-24 临时追加一个ADS1115芯片的读数功能
        mADS1115Reading = false;
        if(null != mRecvedLevelPromptDlg) {
            mRecvedLevelPromptDlg.dismiss();
            mRecvedLevelPromptDlg = null;
        }
// End of H.M.Wang 2024-5-24 临时追加一个ADS1115芯片的读数功能
        if(null != mIFTestOp) {
            if(mIFTestOp.quit()) {
                mTitleTV.setText(TITLE);
                mSubMenuLV.setVisibility(View.VISIBLE);
                mIFTestOp = null;
            }
            return false;
        } else {
            mContainer.removeView(mSubMenuLV);
            return true;
        }
    }
}
