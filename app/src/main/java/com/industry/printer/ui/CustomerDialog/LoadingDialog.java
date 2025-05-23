package com.industry.printer.ui.CustomerDialog;

import com.industry.printer.R;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

// H.M.Wang 2023-11-28 追加RelightableDialog作为所有对话窗的父类，用来支持点按屏幕点亮屏幕
public class LoadingDialog extends RelightableDialog {
//public class LoadingDialog extends Dialog {
// End of H.M.Wang 2023-11-28 追加RelightableDialog作为所有对话窗的父类，用来支持点按屏幕点亮屏幕

	private Context mContext;
	private ImageView mRotationView;
	private TextView mMessage;
	
	private Animation operatingAnim;

	private static LoadingDialog gLoadingDialog = null;

	public LoadingDialog(Context context) {
		super(context);
		mContext = context;
		this.setCanceledOnTouchOutside(false);
	}
	
//	@Override
//	protected void onCreate(Bundle savedInstanceState)
//	{
//		super.onCreate(savedInstanceState);
		
//		this.setContentView(R.layout.layout_loading);
//		mRotationView = (ImageView) findViewById(R.id.rotationImg);
//		mMessage = (TextView) findViewById(R.id.message);
//		operatingAnim = AnimationUtils.loadAnimation(mContext, R.anim.loading_anim);
//		LinearInterpolator lin = new LinearInterpolator();
//		operatingAnim.setInterpolator(lin);
//		operatingAnim.start();
//	}
	
	public void onWindowFocusChanged(boolean hasFocus) {  
		 operatingAnim = AnimationUtils.loadAnimation(mContext, R.anim.loading_anim);
		 LinearInterpolator lin = new LinearInterpolator();
		 operatingAnim.setInterpolator(lin);
		 mRotationView.startAnimation(operatingAnim);
	}  
	
	@Override
	protected void onStop() {
		super.onStop();
		mRotationView.clearAnimation();
	}
	
	private void init(CharSequence message) {
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.setContentView(R.layout.layout_loading);
		mRotationView = (ImageView) findViewById(R.id.rotationImg);
		mMessage = (TextView) findViewById(R.id.message);
		mMessage.setText(message);
	}

	public void setMessage(CharSequence message) {
		mMessage.setText(message);
		mMessage.invalidate();
	}

	public static LoadingDialog showMessageOnly(Context ctx, String message) {
		if(null == gLoadingDialog) {
			gLoadingDialog = show(ctx, message);
		}
		gLoadingDialog.mRotationView.setVisibility(View.GONE);
		if(!gLoadingDialog.isShowing()) gLoadingDialog.show();
		return gLoadingDialog;
	}

	public static LoadingDialog show(Context ctx, int message) {
		String msg = ctx.getString(message);
		gLoadingDialog = show(ctx, msg);
		return gLoadingDialog;
	}
	
	public static LoadingDialog show(Context ctx, CharSequence message) {
		LoadingDialog dialog = new LoadingDialog(ctx);
		dialog.init(message);
//		dialog.setMessage(message);
		dialog.show();
		return dialog;
	}
}
