package com.industry.printer.Collaboration;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.industry.printer.FileFormat.SystemConfigFile;
import com.industry.printer.R;
import com.industry.printer.Utils.Debug;

/*
onAttach()          → 关联到 Activity（Context 可用）
onCreate()          → Fragment 初始化（savedInstanceState 可用）
onCreateView()      → 创建视图（返回 View）
onViewCreated()     → 视图创建完成（推荐在此初始化 View）
onActivityCreated() → 已废弃
onViewStateRestored() → 视图状态恢复完成
onStart()           → 可见（与 Activity onStart 对应）
onResume()          → 可交互（与 Activity onResume 对应）
onPause()           → 失去焦点
onStop()            → 不可见
onSaveInstanceState() → 保存状态
onDestroyView()     → 销毁视图（View 置空）
onDestroy()         → Fragment 销毁
onDetach()          → 与 Activity 解绑（Context 失效）
 */
public class CollaborationFragment extends Fragment {
    private static final String TAG = CollaborationFragment.class.getSimpleName();

    private Context mContext;

    private LinearLayout mClientView;
    private TextView mStartPrintBtn;
    private TextView mStopPrintBtn;
    private ControllerSide mControllerSide;
//    private PrinterSide mPrinterSide;

    public static final int MSG_DEVICE_ADDED = 10001;
    public static final int MSG_DEVICE_REMOVED = 10002;

    public Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case MSG_DEVICE_ADDED:
                    break;
                case MSG_DEVICE_REMOVED:
                    break;
                default:
                    break;
            }
        }
    };

    public CollaborationFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.collaboration_controler, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mContext = this.getActivity();

        mClientView = (LinearLayout) view.findViewById(R.id.client_area);
        mStartPrintBtn = (TextView) view.findViewById(R.id.tv_start);
        mStopPrintBtn = (TextView) view.findViewById(R.id.tv_stop);

        mControllerSide = new ControllerSide(mContext, new ControllerSide.CmdTransferListener() {
            @Override
            public void onRecv(ControllerSide.ConnectedDevice dev, String recv) {
                Debug.d(TAG, dev.getIPAddress() + ": " + recv);
            }
        });

/*
        if(SystemConfigFile.getInstance(mContext).getParam(89) == 1) {
            mControllerSide = new ControllerSide(mContext, new ControllerSide.CmdTransferListener() {
                @Override
                public void onRecv(ControllerSide.ConnectedDevice dev, String recv) {
                    Debug.d(TAG, dev.getIPAddress() + ": " + recv);
                }
            });
        } else if(SystemConfigFile.getInstance(mContext).getParam(89) == 2) {
            mPrinterSide = new PrinterSide(mContext, this);
            mPrinterSide.start();
        }
*/
    }
}
