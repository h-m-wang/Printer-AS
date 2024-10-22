package com.industry.printer.ui.CustomerDialog;


import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

// H.M.Wang 2023-11-28 追加RelightableDialog作为所有对话窗的父类，用来支持点按屏幕点亮屏幕
public class CustomerDialogBase extends RelightableDialog {
//public class CustomerDialogBase extends Dialog {
// End of H.M.Wang 2023-11-28 追加RelightableDialog作为所有对话窗的父类，用来支持点按屏幕点亮屏幕

	/**
	 * 按键处理回调函数
	 */
	public OnPositiveListener pListener;
	public OnNagitiveListener nListener;
	public OnExtraListener eListener;
	
	public CustomerDialogBase(Context context) {
		super(context);
	}

	public CustomerDialogBase(Context context, int theme) {
		super(context, theme);
	}

	/**
	  *Interface definition when positive button clicked 
	  **/
	 public interface OnPositiveListener{
		 void onClick();
		 void onClick(String content);
	 }

	 /**
	  *Interface definition when nagitive button clicked 
	  **/
	 public interface OnNagitiveListener{
		 void onClick();
	 }
	 
	public interface OnExtraListener {
	 	void onClick();
	}
	 /**
	  * setOnPositiveClickedListener - set the positive button listener for further deal with
	  * @param listener the listener callback
	  * @see OnPositiveListener
	  */
	 public void setOnPositiveClickedListener(OnPositiveListener listener)
	 {
		 pListener = listener;
	 }

	 public void setOnExtraClickedListener(OnExtraListener listener) {
	 	eListener = listener;
	 }

	 /**
	  * setOnNagitiveClickedListener - set the Nagitive button listener for further deal with
	  * @param listener  the listener callback
	  * @see OnNagitiveListener
	  */
	 public void setOnNagitiveClickedListener(OnNagitiveListener listener)
	 {
		 nListener = listener;
	 }
	 
	 
	 @Override
	 protected void onCreate(Bundle savedInstanceState) {
		 super.onCreate(savedInstanceState);
	 }
}
