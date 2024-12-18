package com.industry.printer.ui.CustomerDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes.Name;

import com.industry.printer.R;
import com.industry.printer.Utils.Debug;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
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

@Deprecated
// H.M.Wang 2023-11-28 追加RelightableDialog作为所有对话窗的父类，用来支持点按屏幕点亮屏幕
public class CommonSelectDialog extends RelightableDialog implements android.view.View.OnClickListener, OnItemClickListener {
//public class CommonSelectDialog extends Dialog implements android.view.View.OnClickListener, OnItemClickListener {
// End of H.M.Wang 2023-11-28 追加RelightableDialog作为所有对话窗的父类，用来支持点按屏幕点亮屏幕

	private Context mContext;
	private GridView mList;
	private ItemAdapter mAdapter;
	
	private TextView mOk;
	private TextView mCancel;
	
	private Handler mHandler;
	
	private int mLayoutId;
	
	public CommonSelectDialog(Context context, Handler handler, int layout) {
// H.M.Wang 2023-7-20 取消Theme，因为这样生成的对话窗会在显示的时候，屏幕亮度随系统的亮度立即调整，如系统的亮度设的偏暗，则屏幕会立即变暗，看起来很费劲
//		super(context, R.style.Dialog_Fullscreen);
		super(context);
// End of H.M.Wang 2023-7-20 取消Theme，因为这样生成的对话窗会在显示的时候，屏幕亮度随系统的亮度立即调整，如系统的亮度设的偏暗，则屏幕会立即变暗，看起来很费劲
		mContext = context;
		mHandler = handler;
		mLayoutId = layout;
	}

	@Override
	 protected void onCreate(Bundle savedInstanceState) {
	        // TODO Auto-generated method stub
	    super.onCreate(savedInstanceState);
	    this.requestWindowFeature(Window.FEATURE_NO_TITLE);
	    this.setContentView(mLayoutId);

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
	    
	    mList = (GridView) findViewById(R.id.fontlist);
	    mAdapter = new ItemAdapter();
	    
	    
	    mList.setAdapter(mAdapter);
	    mList.setOnItemClickListener(this);
	}

	public void setItems(String[] items) {
		mAdapter.setItems(items);
	}
	
	public class ItemAdapter extends BaseAdapter {

		private int position=0;
		private Holder mHolder;
		private LayoutInflater mInflater;
		private String[] mItems;
//		private String[] mFonts = mContext.getResources().getStringArray(R.array.strFontArray);
		
		public ItemAdapter() {
			mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			init();
		}
		
		private void init() {			
			
		}
		
		public void setItems(String[] items) {
			if (items == null || items.length <= 0) {
				return;
			}
			mItems = items;
		}
		
		public void setSelect(int pos) {
			position = pos;
			notifyDataSetChanged();
		}
		
		public String getSelectedItem() {
			if (mItems == null || mItems.length <= 0) {
				return "unknow";
			} else if (mItems.length <= position) {
				position = 0;
			}
			return mItems[position];
		}
		
		@Override
		public int getCount() {
			return mItems.length;
		}

		@Override
		public Object getItem(int arg0) {
			if (mItems == null || mItems.length <= arg0) {
				return null;
			}
			return mItems[arg0];
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
				convertView = mInflater.inflate(R.layout.font_item_layout, null);
				mHolder = new Holder();
				mHolder.mText = (TextView) convertView.findViewById(R.id.font);
				mHolder.mTips = (TextView) convertView.findViewById(R.id.fontTips);
			}
			
			mHolder.mText.setText(position >= mItems.length ? "" : mItems[position]);
			mHolder.mText.setSelected(position == this.position);
			
			convertView.setTag(mHolder);
			return convertView;
		}
		
		
	}
	
	public class Holder {
		public TextView mText;
		public TextView mTips;
	}

	@Override
	public void onClick(View arg0) {
		switch (arg0.getId()) {
		case R.id.confirm:
			Message msg = mHandler.obtainMessage(ObjectInfoDialog.MSG_SELECTED_FONT);
			Bundle bundle = new Bundle();
			bundle.putString("font", mAdapter.getSelectedItem());
			msg.setData(bundle);
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
