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

import com.industry.printer.MessageTask;
import com.industry.printer.R;
import com.industry.printer.Utils.ConfigPath;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.ToastUtil;

import java.io.File;

public class TestMain {
    public static final String TAG = TestMain.class.getSimpleName();

    private Context mContext = null;
    private PopupWindow mPopupWindow = null;
//    private TextView mQuitBtnTV = null;
    private TextView mTitleTV = null;
    private FrameLayout mClientAreaFL = null;
    private ListView mMainMenuLV = null;
    private ITestOperation mIFTestOp = null;

    private final String TITLE = "";
// H.M.Wang 2023-10-8 临时添加一个保存1000次的强度试验，暂时放在这里，待以后再次确定
//    private final String[] MAIN_TEST_ITEMS = {"墨袋机", "连供", "AL大字机"};
    private final String[] MAIN_TEST_ITEMS = {"墨袋机", "连供", "AL大字机", "Save Test001"};
// H.M.Wang 2023-10-8 临时添加一个保存1000次的强度试验，暂时放在这里，待以后再次确定

    public TestMain(Context ctx) {
        mContext = ctx;
        mIFTestOp = null;
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
                if(null != mIFTestOp) {
                    if(mIFTestOp.quit()) {
                        mMainMenuLV.setVisibility(View.VISIBLE);
                        mTitleTV.setText(TITLE);
                        mIFTestOp = null;
                    }
                } else {
                    mPopupWindow.dismiss();
                }
            }
        });

        mTitleTV = (TextView)popupView.findViewById(R.id.test_title);
        mTitleTV.setText(TITLE);

        mClientAreaFL = (FrameLayout) popupView.findViewById(R.id.client_area);

        mMainMenuLV = (ListView) popupView.findViewById(R.id.test_main_menu);
        mMainMenuLV.setAdapter(new BaseAdapter() {
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

        mMainMenuLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
// H.M.Wang 2023-10-8 临时添加一个保存1000次的强度试验，暂时放在这里，待以后再次确定
                if(i == 3) {
                    final MessageTask mTask = new MessageTask(mContext, ConfigPath.getTlkPath() + File.separator + "Test001");
                    if(new File(ConfigPath.getTlkPath() + File.separator + "Test001").exists()) {
                        mTask.setName("Test001_");
                        mSaving = false;
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try{
                                    for(int i=0; i<SAVE_COUNT_LIMIT; i++) {
                                        final int pos = i+1;
                                        mSaving = true;
                                        mTask.save(new MessageTask.SaveProgressListener() {
                                            @Override
                                            public void onSaved() {
                                                mSaving = false;
                                                mMainMenuLV.post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        ToastUtil.show(mContext, "Saved: " + pos);
                                                    }
                                                });
                                            }
                                        });
                                        while(mSaving) {
                                            Thread.sleep(10);
                                        }
                                    }
                                } catch(Exception e) {
                                    Debug.e(TAG, e.getMessage());
                                }
                            }
                        }).start();
                    }
                    return;
                }
// End of H.M.Wang 2023-10-8 临时添加一个保存1000次的强度试验，暂时放在这里，待以后再次确定
                mIFTestOp = new TestSub(mContext, i);
                mIFTestOp.show(mClientAreaFL);
                mIFTestOp.setTitle(mTitleTV);
                mMainMenuLV.setVisibility(View.GONE);
            }
        });

        mPopupWindow.showAtLocation(v, Gravity.NO_GRAVITY, 0, 0);
    }

// H.M.Wang 2023-10-8 临时添加一个保存1000次的强度试验，暂时放在这里，待以后再次确定
    private int SAVE_COUNT_LIMIT = 1000;
    private boolean mSaving;
// End of H.M.Wang 2023-10-8 临时添加一个保存1000次的强度试验，暂时放在这里，待以后再次确定
}
