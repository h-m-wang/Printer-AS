package com.industry.printer.ui.Test;

import android.content.Context;
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
import com.industry.printer.Utils.Debug;

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
    private static final int ID_BAGINK_TEST = 7;
    private static final int ID_HP22MM_TEST = 8;

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
            },
            {       // Smartcard
                    new TestItem("Level ID Test", ID_LEVEL_ID_TEST),
                    new TestItem("Read Level Test", ID_READ_LEVEL_TEST),
                    new TestItem("Bag Reduction Test", ID_BAG_REDUCTION_TEST),
                    new TestItem("Smartcard Init Test", ID_SC_INIT_TEST),
                    new TestItem("SmartCard Valve On/Off Test", ID_SC_VALVE_ONOFF_TEST),

            },
            {       // ALBIG
                    new TestItem("GPIO Pin Test", ID_GPIO_PIN_TEST),
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
                    case ID_BAGINK_TEST:
                        mIFTestOp = new TestBagink(mContext, mSubTestIndex);
                        break;
                    case ID_HP22MM_TEST:
                        mIFTestOp = new TestHp22mm(mContext, mSubTestIndex);
                        break;
                    default:
                        mIFTestOp = null;
                        break;
                }
                if(null != mIFTestOp) {
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
