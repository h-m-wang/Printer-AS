package com.industry.printer.hardware;

import android.content.Context;

import com.industry.printer.DataTransferThread;
import com.industry.printer.FileFormat.SystemConfigFile;
import com.industry.printer.Utils.Debug;
import com.industry.printer.data.DataTask;
import com.industry.printer.object.BaseObject;
import com.industry.printer.object.CounterObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class PI11Monitor {
    public static final String TAG = PI11Monitor.class.getSimpleName();

    private Context mContext;
    private Thread mThread = null;

    public interface PI11MonitorFunc {
        public void onStartPrint();
        public void onStopPrint();
        public void onMirror();
        public void onResetCounter();
        public void onPrintFile(int index);
        public void onLevelLow();
        public void onSolventLow();
        public void onLevelHigh();
        public void onSolventHigh();
    }
    private PI11MonitorFunc mCallbackFunc = null;

    private SystemConfigFile mSysconfig;
    private DataTransferThread mDTransThread;

// H.M.Wang 2022-2-13 将PI11状态的读取，并且根据读取的值进行控制的功能扩展为对IN管脚的读取，并且做相应的控制
// H.M.Wang 2021-9-19 追加PI11状态读取功能
//        private Timer mGpio11Timer = null;
//		private int mPI11State = 0;
// End of H.M.Wang 2021-9-19 追加PI11状态读取功能
//    private Timer mInPinReadTimer = null;
    private int mInPinState = 0;
    // H.M.Wang 2023-8-12 追加P6，对于墨位低及溶剂低报警，这里定义时间间隔，单位为秒
    private boolean mP6LevelLow = false;
    private int mP6AlarmCount = 0;
    private boolean mP6SolventLow = false;
//	private int mP6SolventAlarmCount = 0;
// H.M.Wang 2023-8-12 追加P6，对于墨位低及溶剂低报警，这里定义时间间隔，单位为秒
// End of H.M.Wang 2022-2-13 将PI11状态的读取，并且根据读取的值进行控制的功能扩展为对IN管脚的读取，并且做相应的控制
    private int ccc = 0;

    public PI11Monitor(Context ctx, PI11MonitorFunc callback) {
        mContext = ctx;
        mCallbackFunc = callback;
        mSysconfig = SystemConfigFile.getInstance(mContext);
        mDTransThread = DataTransferThread.getInstance(mContext);
    }

// H.M.Wang 2022-3-21 根据工作中心对输入管脚协议的重新定义，大幅度修改相应的处理方法
// H.M.Wang 2022-2-13 将PI11状态的读取，并且根据读取的值进行控制的功能扩展为对IN管脚的读取，并且做相应的控制
// H.M.Wang 2021-9-19 追加PI11状态读取功能
    public void start(final long delay) {
        if(null == mThread) {
            mThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try{Thread.sleep(delay);}catch(Exception e){};
                    while(true) {
                        try{Thread.sleep(1000);}catch(Exception e){};
                        //     协议１：　禁止GPIO　
                        if (mSysconfig.getParam(SystemConfigFile.INDEX_IPURT_PROC) == SystemConfigFile.INPUT_PROTO_1) {
                            continue;
                        }

                        final int newState = ExtGpio.readPI11State();

/*                        int aaa = 0;

                        if (ccc > 30) {
                            aaa = 0x01;
                        }
                        if (ccc > 60) {
                            aaa = 0x00;
                        }
                        if (ccc > 90) {
                            aaa = 0x02;
                        }
                        if (ccc > 120) {
                            aaa = 0x00;
                        }
                        if (ccc > 150) {
                            aaa = 0x04;
                        }
                        if (ccc > 180) {
                            aaa = 0x00;
                        }
                        if (ccc > 210) {
                            aaa = 0x30;
                        }
                        if (ccc > 240) {
                            aaa = 0x10;
                        }
                        if (ccc > 270) {
                            aaa = 0x00;
                        }

                        Debug.d(TAG, "ccc = " + ccc);
                        final int newState = aaa;
                        ccc++;
*/
                        //     协议６：　
                        //            0x10：墨位低（Output1输出，弹窗）
                        //            0x20：溶剂低（Output1输出，弹窗）
                        if (mSysconfig.getParam(SystemConfigFile.INDEX_IPURT_PROC) == SystemConfigFile.INPUT_PROTO_6) {
                            if (mP6LevelLow || mP6SolventLow) {
                                mP6AlarmCount++;
                            }
                            if (mP6AlarmCount >= 5) {
                                mP6AlarmCount = 0;
                                if (null != mCallbackFunc) {
                                    if (mP6LevelLow)
                                        mCallbackFunc.onLevelLow();
                                    if (mP6SolventLow)
                                        mCallbackFunc.onSolventLow();
                                }
                            }
                        }

                        if (mInPinState == newState) continue;
                        Debug.d(TAG, "oldState = " + mInPinState + "; newState = " + newState);

                        // H.M.Wang 2022-12-15 修改在线程当中生成DataTransferThread类的实例的话，会造成初始化是Handler生命部分异常，因为Handler不能在线程中生成
                        //					final DataTransferThread thread = DataTransferThread.getInstance(mContext);
                        // H.M.Wang 2022-12-15 修改在线程当中生成DataTransferThread类的实例的话，会造成初始化是Handler生命部分异常，因为Handler不能在线程中生成

                        //     协议２：　
                        //            0x01：是打印开始停止
                        //     协议４：
                        //            0x01：是打印“开始／停止”控制位。其中，打印开始停止是在apk里面处理的，方向控制是在img里面控制的
                        //     协议６：　
                        //            0x01：是打印开始停止
                        if (mSysconfig.getParam(SystemConfigFile.INDEX_IPURT_PROC) == SystemConfigFile.INPUT_PROTO_2 ||
                                mSysconfig.getParam(SystemConfigFile.INDEX_IPURT_PROC) == SystemConfigFile.INPUT_PROTO_4 ||
                                mSysconfig.getParam(SystemConfigFile.INDEX_IPURT_PROC) == SystemConfigFile.INPUT_PROTO_6) {
                            if ((mInPinState & 0x01) != (newState & 0x01) && (newState & 0x01) == 0x01) {
                                if (null != mCallbackFunc) mCallbackFunc.onStartPrint();
                            }
                            if ((mInPinState & 0x01) != (newState & 0x01) && (newState & 0x01) == 0x00) {
                                if (null != mCallbackFunc) mCallbackFunc.onStopPrint();
                            }
                        }

                        //     协议３：
                        //            0x01：方向切换
                        if (mSysconfig.getParam(SystemConfigFile.INDEX_IPURT_PROC) == SystemConfigFile.INPUT_PROTO_3) {
                            if ((mInPinState & 0x01) != (newState & 0x01)) {
                                Debug.d(TAG, "设置方向调整：" + (newState & 0x01));
                                FpgaGpioOperation.setMirror(newState & 0x01);
                            }
                        }
                        //     协议４：
                        //            0x04：方向切换
                        //     协议６：　
                        //            0x04：方向切换
                        if (mSysconfig.getParam(SystemConfigFile.INDEX_IPURT_PROC) == SystemConfigFile.INPUT_PROTO_4 ||
                            mSysconfig.getParam(SystemConfigFile.INDEX_IPURT_PROC) == SystemConfigFile.INPUT_PROTO_6) {
                            if ((mInPinState & 0x04) != (newState & 0x04)) {
                                Debug.d(TAG, "设置方向调整：" + ((newState & 0x04) != 0 ? 1 : 0));
                                FpgaGpioOperation.setMirror(((newState & 0x04) != 0 ? 1 : 0));
                            }
                        }

                        //     协议５：
                        //            0x01：是计数器清零，包括RTC的数据和正在打印的数据
                        //     协议４：
                        //            0x02：是计数器清零，包括RTC的数据和正在打印的数据
                        //     协议６：　
                        //            0x02：是计数器清零，包括RTC的数据和正在打印的数据
                        if ((mSysconfig.getParam(SystemConfigFile.INDEX_IPURT_PROC) == SystemConfigFile.INPUT_PROTO_5 && (mInPinState & 0x01) == 0x00 && (newState & 0x01) == 0x01) ||
                            (mSysconfig.getParam(SystemConfigFile.INDEX_IPURT_PROC) == SystemConfigFile.INPUT_PROTO_4 && (mInPinState & 0x02) == 0x00 && (newState & 0x02) == 0x02) ||
                            (mSysconfig.getParam(SystemConfigFile.INDEX_IPURT_PROC) == SystemConfigFile.INPUT_PROTO_6 && (mInPinState & 0x02) == 0x00 && (newState & 0x02) == 0x02)) {
                            Debug.d(TAG, "Clear counters");
                            SystemConfigFile sysConfigFile = SystemConfigFile.getInstance();
                            long[] counters = new long[10];
                            for (int i = 0; i < 10; i++) {
                                counters[i] = 0;
                                sysConfigFile.setParamBroadcast(i + SystemConfigFile.INDEX_COUNT_1, 0);
                            }
                            RTCDevice.getInstance(mContext).writeAll(counters);

                            // H.M.Wang 2022-5-31 P-5的时候向FPGA的PG1和PG2下发11，3ms后再下发00
                            if (mDTransThread != null && mDTransThread.isRunning() && mSysconfig.getParam(SystemConfigFile.INDEX_IPURT_PROC) == SystemConfigFile.INPUT_PROTO_5) {
                                FpgaGpioOperation.clear();
                            }
                            // End of H.M.Wang 2022-5-31 P-5的时候向FPGA的PG1和PG2下发11，3ms后再下发00

                            List<DataTask> tasks = null;
                            if (mDTransThread != null) {
                                tasks = mDTransThread.getData();
                            }
                            if (null != tasks) {
                                for (DataTask task : tasks) {
                                    ArrayList<BaseObject> objList = task.getObjList();
                                    for (BaseObject obj : objList) {
                                        if (obj instanceof CounterObject) {
                                            ((CounterObject) obj).setValue(((CounterObject) obj).getStart());
                                        }
                                    }
                                }
                                if (mDTransThread.isRunning()) {
                                    DataTask task = mDTransThread.getCurData();
                                    ArrayList<BaseObject> objList = task.getObjList();
                                    for (BaseObject obj : objList) {
                                        if (obj instanceof CounterObject) {
                                            mDTransThread.mNeedUpdate = true;
                                        }
                                    }
                                }
                            }
                        }

                        //     协议４：
                        //            0xF0段，即0x10 - 0xF0)为打印文件的文件名（数字形式，1-15）
                        if (mSysconfig.getParam(SystemConfigFile.INDEX_IPURT_PROC) == SystemConfigFile.INPUT_PROTO_4) {
                            if ((mInPinState & 0x0F0) != (newState & 0x0F0)) {
                                if (null != mCallbackFunc)
                                    mCallbackFunc.onPrintFile(0x0F & (newState >> 4));
                            }
                        }

                        //     协议６：　
                        //            0x10：墨位低（Output1输出，弹窗）
                        //            0x20：溶剂低（Output1输出，弹窗）
                        if (mSysconfig.getParam(SystemConfigFile.INDEX_IPURT_PROC) == SystemConfigFile.INPUT_PROTO_6) {
                            if ((mInPinState & 0x10) == 0x00 && (newState & 0x10) == 0x10) {
                                Debug.d(TAG, "Level Low");
                                mP6LevelLow = true;
                                mP6AlarmCount = 0;
                            } else if ((mInPinState & 0x10) == 0x10 && (newState & 0x10) == 0x00) {
                                Debug.d(TAG, "Level High");
                                mP6LevelLow = false;
                                mP6AlarmCount = 0;
                                if (null != mCallbackFunc) mCallbackFunc.onLevelHigh();
                            }
                            if ((mInPinState & 0x20) == 0x00 && (newState & 0x20) == 0x20) {
                                Debug.d(TAG, "Solvent Low");
                                mP6SolventLow = true;
                                mP6AlarmCount = 0;
                            } else if ((mInPinState & 0x20) == 0x20 && (newState & 0x20) == 0x00) {
                                Debug.d(TAG, "Solvent High");
                                mP6SolventLow = false;
                                mP6AlarmCount = 0;
                                if (null != mCallbackFunc) mCallbackFunc.onSolventHigh();
                            }
                        } else {
                            mP6LevelLow = false;
                            mP6SolventLow = false;
                        }
                        mInPinState = newState;
                    }
                }
            });
            mThread.start();
        }
    }
// End of H.M.Wang 2021-9-19 追加PI11状态读取功能
// End of H.M.Wang 2022-2-13 将PI11状态的读取，并且根据读取的值进行控制的功能扩展为对IN管脚的读取，并且做相应的控制
// End of H.M.Wang 2022-3-21 根据工作中心对输入管脚协议的重新定义，大幅度修改相应的处理方法

    public boolean getLevelLow() {
        return mP6LevelLow;
    }

    public boolean getSolventLow() {
        return mP6SolventLow;
    }

}
