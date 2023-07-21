package com.industry.printer.ui.CustomerDialog;

import com.industry.printer.DataTransferThread;
import com.industry.printer.FileFormat.SystemConfigFile;
import com.industry.printer.R;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.ToastUtil;
import com.industry.printer.hardware.RTCDevice;
import com.industry.printer.object.BaseObject;
import com.industry.printer.object.CounterObject;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by hmwan on 2020/4/24.
 */

public class CounterEditDialog extends Dialog implements android.view.View.OnClickListener {
    private static final String TAG = CounterEditDialog.class.getSimpleName();

    private Context                 mContext;

    private EditText                mCounterEdit;
    private TextView                mCounterIndex;
    private TextView                mCounterClear;

    private TextView                mConfirm;
    private TextView                mCancel;

    private int                     mIndex;
    private String                  mValue;
    public CounterEditDialog(Context context, int index, String value) {
// H.M.Wang 2023-7-20 取消Theme，因为这样生成的对话窗会在显示的时候，屏幕亮度随系统的亮度立即调整，如系统的亮度设的偏暗，则屏幕会立即变暗，看起来很费劲
// 这里不指定Theme，然后在onCreate函数中通过指定Layout为Match_Parent的方法，既可以达到全屏的效果，也可以避免变暗
//		super(context, R.style.Dialog_Fullscreen);
        super(context);
// End of H.M.Wang 2023-7-20 取消Theme，因为这样生成的对话窗会在显示的时候，屏幕亮度随系统的亮度立即调整，如系统的亮度设的偏暗，则屏幕会立即变暗，看起来很费劲
        mContext = context;
        mIndex = index;
        mValue = value;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.counter_edit_dialog);

// H.M.Wang 2023-7-20 取消Theme，因为这样生成的对话窗会在显示的时候，屏幕亮度随系统的亮度立即调整，如系统的亮度设的偏暗，则屏幕会立即变暗，看起来很费劲
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        getWindow().setAttributes(lp);
// End of H.M.Wang 2023-7-20 取消Theme，因为这样生成的对话窗会在显示的时候，屏幕亮度随系统的亮度立即调整，如系统的亮度设的偏暗，则屏幕会立即变暗，看起来很费劲

        mCounterEdit = (EditText) findViewById(R.id.counterEdit);
        mCounterEdit.setText(mValue);
        mCounterEdit.setSelection(mValue.length());

        mCounterIndex = (TextView) findViewById(R.id.counterIndex);
        mCounterIndex.setText(String.format(mContext.getResources().getString(R.string.strCounterIndex), mIndex));

        mCounterClear = (TextView) findViewById(R.id.idBtnClear);
        mCounterClear.setOnClickListener(this);

        mConfirm = (TextView) findViewById(R.id.confirm);
        mCancel = (TextView) findViewById(R.id.cancel);
        mConfirm.setOnClickListener(this);
        mCancel.setOnClickListener(this);
    }

    @Override
    public void onClick(View arg0) {
        switch (arg0.getId()) {
            case R.id.confirm:
                int value = 0;
                try{
                    value = Integer.parseInt(mCounterEdit.getText().toString());
                    DataTransferThread dt = DataTransferThread.getInstance(mContext);
                    if(null != dt && null != dt.mDataTask) {
                        boolean modified = false;
                        for(int i=0; i<dt.mDataTask.size(); i++) {
                            ArrayList<BaseObject> objList = dt.mDataTask.get(i).getObjList();
                            for(BaseObject o : objList) {
                                if(o instanceof CounterObject) {
                                    if(((CounterObject)o).getmCounterIndex() == mIndex) {
                                        ((CounterObject)o).setValue(value);
                                        modified = true;
                                    }
                                }
                            }
                        }
                        if(dt.isRunning() && modified) {
                            dt.mNeedUpdate = true;
// H.M.Wang 2020-7-9 追加计数器重置标识
                            dt.mCounterReset = true;
// End of H.M.Wang 2020-7-9 追加计数器重置标识
                        }
                    } else {
                        SystemConfigFile.getInstance().setParamBroadcast(mIndex + SystemConfigFile.INDEX_COUNT_1, value);
                        RTCDevice.getInstance(mContext).write(value, mIndex);
                    }
                    dismiss();
                } catch (Exception e) {
                    ToastUtil.show(mContext, e.getMessage());
//                    for(StackTraceElement s: e.getStackTrace()) {
//                        Debug.e(TAG, s.getClassName() + "." + s.getMethodName() + "(" + s.getFileName() + ":" + s.getLineNumber() + ")");
//                    }
                    return;
                }
                break;
            case R.id.cancel:
                dismiss();
            case R.id.idBtnClear:
                mCounterEdit.setText("0");
            default:
                break;
        }
    }
}
