package com.industry.printer.ui.CustomerDialog;

import com.industry.printer.R;
import com.industry.printer.Utils.Debug;
import com.industry.printer.object.BaseObject;
import com.industry.printer.object.MessageObject;

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

// H.M.Wang 2023-11-28 追加RelightableDialog作为所有对话窗的父类，用来支持点按屏幕点亮屏幕
public class HeightSelectDialog extends RelightableDialog implements android.view.View.OnClickListener, OnItemClickListener {
//public class HeightSelectDialog extends Dialog implements android.view.View.OnClickListener, OnItemClickListener {
// End of H.M.Wang 2023-11-28 追加RelightableDialog作为所有对话窗的父类，用来支持点按屏幕点亮屏幕

	private Context mContext;
	private GridView mFontlist;
	private FontItemAdapter mAdapter;
	
	private TextView mOk;
	private TextView mCancel;
	
	private Handler mHandler;
	private BaseObject mObject;
	
	public HeightSelectDialog(Context context, Handler handler, BaseObject object) {
// H.M.Wang 2023-7-20 取消Theme，因为这样生成的对话窗会在显示的时候，屏幕亮度随系统的亮度立即调整，如系统的亮度设的偏暗，则屏幕会立即变暗，看起来很费劲
// 这里不指定Theme，然后在onCreate函数中通过指定Layout为Match_Parent的方法，既可以达到全屏的效果，也可以避免变暗
//		super(context, R.style.Dialog_Fullscreen);
		super(context);
// End of H.M.Wang 2023-7-20 取消Theme，因为这样生成的对话窗会在显示的时候，屏幕亮度随系统的亮度立即调整，如系统的亮度设的偏暗，则屏幕会立即变暗，看起来很费劲
		mContext = context;
		mHandler = handler;
		mObject = object;
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
	    
	    mFontlist = (GridView) findViewById(R.id.sizelist);
	    mAdapter = new FontItemAdapter();
	    
	    
	    mFontlist.setAdapter(mAdapter);
	    mFontlist.setOnItemClickListener(this);
	}

	
	public class FontItemAdapter extends BaseAdapter {

		private int position=0;
		private Holder mHolder;
		private LayoutInflater mInflater;
		private String[] mFonts; // = new String[MessageObject.mBaseList.length];
		
		
		public FontItemAdapter() {
			mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			init();
		}
		
		private void init() {
			if (mObject != null) {
				MessageObject msg = mObject.getTask().getMsgObject();
				mFonts = msg.getDisplayFSList();
			}			
		}
		
		public void setSelect(int pos) {
			position = pos;
			notifyDataSetChanged();
		}
		
		public String getSelectedItem() {
			if (mFonts == null || mFonts.length <= 0) {
				return "";
			}
			return position > mFonts.length ? mFonts[0]: mFonts[position];
		}
		
		@Override
		public int getCount() {
			return mFonts.length;
		}

		@Override
		public Object getItem(int arg0) {
			if (mFonts == null || mFonts.length <= 0) {
				return "";
			}
			return (arg0 > mFonts.length ? null: mFonts[arg0]);
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
				mHolder.mExtra = (TextView) convertView.findViewById(R.id.fontTips);
				mHolder.mExtra.setVisibility(View.GONE);
			}
			
			mHolder.mText.setText(position >= mFonts.length?  "" : mFonts[position]);
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
		public TextView mExtra;
	}

	@Override
	public void onClick(View arg0) {
		switch (arg0.getId()) {
		case R.id.confirm:
			Message msg = mHandler.obtainMessage(ObjectInfoDialog.MSG_SELECTED_SIZE);
			Bundle bundle = new Bundle();
			bundle.putString("height", mAdapter.getSelectedItem());
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
