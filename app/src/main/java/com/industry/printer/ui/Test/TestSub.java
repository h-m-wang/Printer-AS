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
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.industry.printer.R;
import com.industry.printer.Utils.Debug;

public class TestSub {
    public static final String TAG = TestSub.class.getSimpleName();

    private Context mContext = null;
    private PopupWindow mPopupWindow = null;

    private final int SUB_TEST_INDEX_BAGINK = 0;
    private final int SUB_TEST_INDEX_SC = 1;
    private final int SUB_TEST_INDEX_ALBIG = 2;
    private int mSubTestIndex = 0;

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

//    private final String[] SUB_TEST_ITEMS_BAGINK = {"Level ID Test", "Bagink-Valve", "GPIO Pin Test"};
//    private final String[] SUB_TEST_ITEMS_SC = {"Level ID Test", "SC-SC", "SC-Valve"};
//    private final String[] SUB_TEST_ITEMS_ALBIG = {"GPIO Pin Test"};

//    private final String[][] SUB_TEST_ITEMS = {SUB_TEST_ITEMS_BAGINK, SUB_TEST_ITEMS_SC, SUB_TEST_ITEMS_ALBIG};

    public TestSub(Context ctx, int index) {
        mContext = ctx;
        mSubTestIndex = index;
    }

    public void show(final View v) {
        if (null == mContext) {
            return;
        }

        View popupView = LayoutInflater.from(mContext).inflate(R.layout.test_sub, null);

        mPopupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);
        mPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#CC000000")));
        mPopupWindow.setOutsideTouchable(true);
        mPopupWindow.setTouchable(true);
        mPopupWindow.update();

        TextView quitTV = (TextView)popupView.findViewById(R.id.btn_quit);
        quitTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPopupWindow.dismiss();
                TestMain tmp = new TestMain(mContext);
                tmp.show(v);
            }
        });

        TextView titleTV = (TextView)popupView.findViewById(R.id.test_title);
        titleTV.setText("");

        ListView testSubMenu = (ListView) popupView.findViewById(R.id.test_sub_menu);
        testSubMenu.setAdapter(new BaseAdapter() {
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

        testSubMenu.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Debug.d(TAG, mTestItems[mSubTestIndex][i].mCaption);
                switch(mTestItems[mSubTestIndex][i].mTestID) {
                    case ID_GPIO_PIN_TEST:
                        TestGpioPins tgp = new TestGpioPins(mContext, mSubTestIndex);
                        tgp.show(v);
                        break;
                    case ID_LEVEL_ID_TEST:
                        TestLevelID tlid = new TestLevelID(mContext, mSubTestIndex);
                        tlid.show(v);
                        break;
                    case ID_SC_INIT_TEST:
                        TestSCInit tsci = new TestSCInit(mContext, mSubTestIndex);
                        tsci.show(v);
                        break;
                    case ID_READ_LEVEL_TEST:
                        TestReadLevel trl = new TestReadLevel(mContext, mSubTestIndex);
                        trl.show(v);
                        break;
                    case ID_BAG_REDUCTION_TEST:
                        TestReduction tr = new TestReduction(mContext, mSubTestIndex);
                        tr.show(v);
                        break;
                    case ID_SC_VALVE_ONOFF_TEST:
                        TestValveOnOff tvo = new TestValveOnOff(mContext, mSubTestIndex);
                        tvo.show(v);
                        break;
                    case ID_BAGINK_TEST:
                        TestBagink tb = new TestBagink(mContext, mSubTestIndex);
                        tb.show(v);
                        break;
                    case ID_HP22MM_TEST:
                        TestHp22mm th = new TestHp22mm(mContext, mSubTestIndex);
                        th.show(v);
                        break;
                }
            }
        });

        mPopupWindow.showAtLocation(v, Gravity.NO_GRAVITY, 0, 0);
    }
}
