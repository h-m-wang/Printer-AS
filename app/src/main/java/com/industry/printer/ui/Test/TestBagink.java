package com.industry.printer.ui.Test;

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
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.industry.printer.R;
import com.industry.printer.Rfid.RfidScheduler;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.ToastUtil;
import com.industry.printer.hardware.ExtGpio;
import com.industry.printer.hardware.IInkDevice;
import com.industry.printer.hardware.InkManagerFactory;
import com.industry.printer.hardware.SmartCard;

import java.lang.reflect.Field;
import java.util.ArrayList;

/*
  墨袋减锁实验，每点击一次DO，检索一次
 */
public class TestBagink implements ITestOperation {
    public static final String TAG = TestBagink.class.getSimpleName();

    private Context mContext = null;
    private FrameLayout mContainer = null;
    private RelativeLayout mTestAreaRL = null;

    private int mSubIndex = 0;

    private final String TITLE = "Bagink Test";

    public TestBagink(Context ctx, int index) {
        mContext = ctx;
        mSubIndex = index;
    }

    @Override
    public void show(FrameLayout f) {
        mContainer = f;

        mTestAreaRL = (RelativeLayout) LayoutInflater.from(mContext).inflate(R.layout.test_bagink, null);

        mBaginkTest = new BaginkTest[] {
                new BaginkTest(mTestAreaRL, 0),
                new BaginkTest(mTestAreaRL, 1),
                new BaginkTest(mTestAreaRL, 2),
                new BaginkTest(mTestAreaRL, 3)
        };

        mContainer.addView(mTestAreaRL);
    }

    @Override
    public void setTitle(TextView tv) {
        tv.setText(TITLE);
    }

    @Override
    public boolean quit() {
        mContainer.removeView(mTestAreaRL);
        return true;
    }

    private final int mBaginkLevelResIDs[] = new int[] {R.id.bagink_level1, R.id.bagink_level2, R.id.bagink_level3, R.id.bagink_level4};
    private final int mBaginkHX24LCResIDs[] = new int[] {R.id.bagink_hx24lc1, R.id.bagink_hx24lc2, R.id.bagink_hx24lc3, R.id.bagink_hx24lc4};
    private final int mBaginkValveResIDs[] = new int[] {R.id.bagink_valve1, R.id.bagink_valve2, R.id.bagink_valve3, R.id.bagink_valve4};
    private BaginkTest mBaginkTest[];
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
        private ArrayList<BaginkTest.Level_Record> mLevelRecords = new ArrayList<BaginkTest.Level_Record>();
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
                    if(mPause) {
                        mBaginkLevelTV.setBackgroundColor(Color.parseColor("#008800"));
                        mAdditionalTest.setVisibility(View.VISIBLE);
                        mPause = false;
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Debug.d(TAG, "Enter Bagink Level[" + index + "] Test ");
                                synchronized (TestBagink.this) {
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
                                while(!mPause) {
                                    synchronized (TestBagink.this) {
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
                                            BaginkTest.Level_Record lr = mLevelRecords.get(0);
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
                                        mLevelRecords.add(new BaginkTest.Level_Record(rt, level));

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

                            synchronized (TestBagink.this) {
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
                    synchronized (TestBagink.this) {
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
}
