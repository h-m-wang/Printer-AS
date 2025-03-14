package com.industry.printer.ui.CustomerDialog;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes.Name;

import com.industry.printer.R;
import com.industry.printer.Utils.Configs;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.FileUtil;
import com.industry.printer.cache.FontCache;

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

// H.M.Wang 2023-11-28 追加RelightableDialog作为所有对话窗的父类，用来支持点按屏幕点亮屏幕
public class FontSelectDialog extends RelightableDialog implements android.view.View.OnClickListener, OnItemClickListener {
//public class FontSelectDialog extends Dialog implements android.view.View.OnClickListener, OnItemClickListener {
// End of H.M.Wang 2023-11-28 追加RelightableDialog作为所有对话窗的父类，用来支持点按屏幕点亮屏幕

	private static final String TAG = FontSelectDialog.class.getSimpleName(); 
	private Context mContext;
	private GridView mFontlist;
	private FontItemAdapter mAdapter;
	
	private TextView mOk;
	private TextView mCancel;
	
	private Handler mHandler;
	
	public FontSelectDialog(Context context, Handler handler) {
// H.M.Wang 2023-7-20 取消Theme，因为这样生成的对话窗会在显示的时候，屏幕亮度随系统的亮度立即调整，如系统的亮度设的偏暗，则屏幕会立即变暗，看起来很费劲
// 这里不指定Theme，然后在onCreate函数中通过指定Layout为Match_Parent的方法，既可以达到全屏的效果，也可以避免变暗
//		super(context, R.style.Dialog_Fullscreen);
		super(context);
// End of H.M.Wang 2023-7-20 取消Theme，因为这样生成的对话窗会在显示的时候，屏幕亮度随系统的亮度立即调整，如系统的亮度设的偏暗，则屏幕会立即变暗，看起来很费劲
		mContext = context;
		mHandler = handler;
	}

	@Override
	 protected void onCreate(Bundle savedInstanceState) {
	        // TODO Auto-generated method stub
	    super.onCreate(savedInstanceState);
	    this.requestWindowFeature(Window.FEATURE_NO_TITLE);
	    this.setContentView(R.layout.font_select_layout);

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
	    
	    mFontlist = (GridView) findViewById(R.id.fontlist);
	    mAdapter = new FontItemAdapter();
	    
	    
	    mFontlist.setAdapter(mAdapter);
	    mFontlist.setOnItemClickListener(this);
	}

	
	public class FontItemAdapter extends BaseAdapter {

		private static final String FONT_NAME = "title";
		private static final String FONT_TIPS = "tips";
		private int position=0;
		private Holder mHolder;
		private LayoutInflater mInflater;
		private List<Map<String, String>> mFonts;
//		private String[] mFonts = mContext.getResources().getStringArray(R.array.strFontArray);
		
		public FontItemAdapter() {
			mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			init();
		}
		
		private void init() {
			// String[] name = mContext.getResources().getStringArray(R.array.strFontArray);
			// String[] tips = mContext.getResources().getStringArray(R.array.strFontTipsArray);
			String[] name = FontCache.getFonts(mContext);
			if (name == null) {
				return;
			}
			mFonts = new ArrayList<Map<String,String>>();
			for (int i = 0; i < name.length; i++) {
				Map f = new HashMap<String, String>();
				f.put(FONT_NAME, name[i]);
//				f.put(FONT_TIPS, name[i] + " font");
				f.put(FONT_TIPS, name[i]);
				Debug.d("XXX", "--->title: " + name[i]);
				mFonts.add(f);
			}
		}
		
		public void setSelect(int pos) {
			position = pos;
			notifyDataSetChanged();
		}
		
		public String getSelectedItem() {
			return mFonts.get(position).get(FONT_NAME);
		}
		
		@Override
		public int getCount() {
			return mFonts.size();
		}

		@Override
		public Object getItem(int arg0) {
			return mFonts.get(arg0).get(FONT_NAME);
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
			String font = mFonts.get(position).get(FONT_NAME);
			mHolder.mText.setText(font);
			Debug.d(TAG, "--->font: " + font);
			Typeface tf = FontCache.get(mContext, font);
			mHolder.mTips.setTypeface(tf);
			mHolder.mTips.setText(mFonts.get(position).get(FONT_TIPS));
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
