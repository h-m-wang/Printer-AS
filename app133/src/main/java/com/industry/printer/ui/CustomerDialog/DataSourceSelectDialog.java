package com.industry.printer.ui.CustomerDialog;

import com.industry.printer.R;
import com.industry.printer.ui.CustomerAdapter.SettingsListAdapter;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

/**
 * Created by hmwan on 2019/12/19.
 */

// H.M.Wang 2023-11-28 追加RelightableDialog作为所有对话窗的父类，用来支持点按屏幕点亮屏幕
public class DataSourceSelectDialog extends RelightableDialog implements android.view.View.OnClickListener, AdapterView.OnItemClickListener {
//public class DataSourceSelectDialog extends Dialog implements android.view.View.OnClickListener, AdapterView.OnItemClickListener {
// End of H.M.Wang 2023-11-28 追加RelightableDialog作为所有对话窗的父类，用来支持点按屏幕点亮屏幕

    private Context                 mContext;

    private GridView                mDataSourceGV;
    private DataSourceGVAdapter     mAdapter;

    private TextView                mConfirm;
    private TextView                mCancel;

    private Handler                 mHandler;
    private int                     mSelected;

    public DataSourceSelectDialog(Context context, Handler handler, int current) {
// H.M.Wang 2023-7-20 取消Theme，因为这样生成的对话窗会在显示的时候，屏幕亮度随系统的亮度立即调整，如系统的亮度设的偏暗，则屏幕会立即变暗，看起来很费劲
// 这里不指定Theme，然后在onCreate函数中通过指定Layout为Match_Parent的方法，既可以达到全屏的效果，也可以避免变暗
//		super(context, R.style.Dialog_Fullscreen);
        super(context);
// End of H.M.Wang 2023-7-20 取消Theme，因为这样生成的对话窗会在显示的时候，屏幕亮度随系统的亮度立即调整，如系统的亮度设的偏暗，则屏幕会立即变暗，看起来很费劲
        mContext = context;
        mHandler = handler;
        mSelected = current;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.data_source_select_layout);

// H.M.Wang 2023-7-20 取消Theme，因为这样生成的对话窗会在显示的时候，屏幕亮度随系统的亮度立即调整，如系统的亮度设的偏暗，则屏幕会立即变暗，看起来很费劲
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        getWindow().setAttributes(lp);
// End of H.M.Wang 2023-7-20 取消Theme，因为这样生成的对话窗会在显示的时候，屏幕亮度随系统的亮度立即调整，如系统的亮度设的偏暗，则屏幕会立即变暗，看起来很费劲

        mConfirm = (TextView) findViewById(R.id.confirm);
        mCancel = (TextView) findViewById(R.id.cancel);

        mConfirm.setOnClickListener(this);
        mCancel.setOnClickListener(this);

        mDataSourceGV = (GridView) findViewById(R.id.data_source_grid);
        mAdapter = new DataSourceGVAdapter(mSelected);


        mDataSourceGV.setAdapter(mAdapter);
        mDataSourceGV.setOnItemClickListener(this);
    }


    public class DataSourceGVAdapter extends BaseAdapter {

        private int             position=0;
        private Holder          mHolder;
        private LayoutInflater  mInflater;
        private String[]        mDataSources;


        public DataSourceGVAdapter(int pos) {
            mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            position = pos;
            init();
        }

        private void init() {
            mDataSources = mContext.getResources().getStringArray(R.array.strDataSourceArray);
        }

        public void setSelect(int pos) {
            position = pos;
            notifyDataSetChanged();
        }

        public String getSelectedItem() {
            return (position >= mDataSources.length ? "" : mDataSources[position]);
        }

        public int getSelected() {
            return position;
        }

        @Override
        public int getCount() {
            return mDataSources.length;
        }

        @Override
        public Object getItem(int arg0) {
            return (arg0 >= mDataSources.length ? null :mDataSources[arg0]);
        }

        @Override
        public long getItemId(int arg0) {
            return arg0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView != null) {
                mHolder = (Holder) convertView.getTag();
            } else {
                convertView = mInflater.inflate(R.layout.header_item_layout, null);
                mHolder = new Holder();
                mHolder.mText = (TextView) convertView.findViewById(R.id.font);
            }

            mHolder.mText.setText(position >= mDataSources.length ? "" : mDataSources[position]);
            if (position == this.position) {
                mHolder.mText.setSelected(true);
            } else {
                mHolder.mText.setSelected(false);
            }

            convertView.setTag(mHolder);
            return convertView;
        }


    }

    public class Holder {
        public TextView mText;
    }

    @Override
    public void onClick(View arg0) {
        switch (arg0.getId()) {
            case R.id.confirm:
                Message msg = mHandler.obtainMessage(SettingsListAdapter.MSG_DATA_SOURCE_SELECTED);
                msg.arg1 = mAdapter.getSelected();
                mHandler.sendMessage(msg);
                dismiss();
                break;
            case R.id.cancel:
                dismiss();
            default:
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
        mAdapter.setSelect(position);
    }


}
