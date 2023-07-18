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

public class TestMain {
    public static final String TAG = TestMain.class.getSimpleName();

    private Context mContext = null;
    private PopupWindow mPopupWindow = null;

    private final String[] MAIN_TEST_ITEMS = {"墨袋机", "连供", "AL大字机"};

    public TestMain(Context ctx) {
        mContext = ctx;
    }

    public void show(final View v) {
        if (null == mContext) {
            return;
        }

        View popupView = LayoutInflater.from(mContext).inflate(R.layout.test_main, null);

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
            }
        });

        TextView titleTV = (TextView)popupView.findViewById(R.id.test_title);
        titleTV.setText("");

        ListView testMainMenu = (ListView) popupView.findViewById(R.id.test_main_menu);
        testMainMenu.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return MAIN_TEST_ITEMS.length;
            }

            @Override
            public Object getItem(int i) {
                return MAIN_TEST_ITEMS[i];
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
                itemTV.setText(position < MAIN_TEST_ITEMS.length ? MAIN_TEST_ITEMS[position] : "");
                return convertView;
            }
        });
        testMainMenu.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                TestSub tmp = new TestSub(mContext, i);
                tmp.show(v);
            }
        });

        mPopupWindow.showAtLocation(v, Gravity.NO_GRAVITY, 0, 0);
    }
}
