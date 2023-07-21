package com.industry.printer.ui.CustomerDialog;

import com.industry.printer.R;
import com.industry.printer.Utils.Debug;
import com.industry.printer.object.BaseObject;
import com.industry.printer.object.MessageObject;
import com.industry.printer.ui.CustomerAdapter.SettingsListAdapter;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

public class HeaderSelectDialog extends Dialog implements android.view.View.OnClickListener, OnItemClickListener {

	private Context mContext;
	private GridView mHeaderlist;
	private HeaderItemAdapter mAdapter;
	
	private TextView mOk;
	private TextView mCancel;
	
	private Handler mHandler;
	private int mSelected;
	
	public HeaderSelectDialog(Context context, Handler handler, int current) {
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
	    this.setContentView(R.layout.height_select_layout);

// H.M.Wang 2023-7-20 取消Theme，因为这样生成的对话窗会在显示的时候，屏幕亮度随系统的亮度立即调整，如系统的亮度设的偏暗，则屏幕会立即变暗，看起来很费劲
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		WindowManager.LayoutParams lp = getWindow().getAttributes();
		lp.width = WindowManager.LayoutParams.MATCH_PARENT;
		lp.height = WindowManager.LayoutParams.MATCH_PARENT;
		getWindow().setAttributes(lp);
// End of H.M.Wang 2023-7-20 取消Theme，因为这样生成的对话窗会在显示的时候，屏幕亮度随系统的亮度立即调整，如系统的亮度设的偏暗，则屏幕会立即变暗，看起来很费劲

	    mOk = (TextView) findViewById(R.id.confirm);
	    mCancel = (TextView) findViewById(R.id.cancel);
	    
	    mOk.setOnClickListener(this);
	    mCancel.setOnClickListener(this);

		mHeaderlist = (GridView) findViewById(R.id.sizelist);
	    mAdapter = new HeaderItemAdapter(mSelected);


		mHeaderlist.setAdapter(mAdapter);
		mHeaderlist.setOnItemClickListener(this);
	}

	
	public class HeaderItemAdapter extends BaseAdapter {

		private int position=0;
		private Holder mHolder;
		private LayoutInflater mInflater;
		private String[] mHeaders; // = new String[MessageObject.mBaseList.length];
		
		
		public HeaderItemAdapter(int pos) {
			mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			position = pos;
			init();
		}
		
		private void init() {
			mHeaders = mContext.getResources().getStringArray(R.array.strPrinterArray);
		}
		
		public void setSelect(int pos) {
			position = pos;
			notifyDataSetChanged();
		}
		
		public String getSelectedItem() {
			return (position >= mHeaders.length ? "" : mHeaders[position]);
		}
		
		public int getSelected() {
			return position;
		}
		
		@Override
		public int getCount() {
			return mHeaders.length;
		}

		@Override
		public Object getItem(int arg0) {
			return (arg0 >= mHeaders.length ? null :mHeaders[arg0]);
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
			
			mHolder.mText.setText(position >= mHeaders.length ? "" : mHeaders[position]);
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
			Message msg = mHandler.obtainMessage(SettingsListAdapter.MSG_SELECTED_HEADER);
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
