package com.industry.printer.pccommand;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.widget.Toast;

import com.industry.printer.Constants.Constants;
import com.industry.printer.ControlTabActivity;
import com.industry.printer.DataTransferThread;
import com.industry.printer.EditTabSmallActivity;
import com.industry.printer.FileFormat.SystemConfigFile;
import com.industry.printer.MessageForPc;
import com.industry.printer.R;
import com.industry.printer.Utils.StreamTransport;
import com.industry.printer.Socket_Server.PCCommand;
import com.industry.printer.Socket_Server.Paths_Create;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.StringUtil;
import com.industry.printer.Utils.ToastUtil;
import com.industry.printer.data.DataTask;
import com.industry.printer.data.PC_FIFO;
import com.industry.printer.hardware.BarcodeScanParser;
import com.industry.printer.hardware.FpgaGpioOperation;
import com.industry.printer.hardware.IInkDevice;
import com.industry.printer.hardware.InkManagerFactory;
import com.industry.printer.hardware.RTCDevice;
import com.industry.printer.object.BaseObject;
import com.industry.printer.object.CounterObject;
import com.industry.printer.ui.CustomerDialog.RemoteMsgPrompt;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by hmwan on 2021/10/28.
 */

public class PCCommandHandler {
    private static final String TAG = PCCommandHandler.class.getSimpleName();

    private Context mContext = null;

    private StreamTransport mStreamTransport = null;
//    private Handler mProcHandler = null;

    private ControlTabActivity mControlTabActivity = null;

    private boolean mWorking = false;
    private static RemoteMsgPrompt mRemoteRecvedPromptDlg = null;

    public PCCommandHandler(Context ctx, StreamTransport st, ControlTabActivity act, Handler hdlr) {
        mContext = ctx;
        mStreamTransport = st;
        mControlTabActivity = act;
        myHandler = hdlr;
        myHandler.post(new Runnable() {
            @Override
            public void run() {
                if(null == mRemoteRecvedPromptDlg) {
                    mRemoteRecvedPromptDlg = new RemoteMsgPrompt(mContext);
// H.M.Wang 2020-6-3 解决提示对话窗在显示时，扫码枪的信息被其劫持，而无法识别的问题
                    mRemoteRecvedPromptDlg.setOnKeyListener(new DialogInterface.OnKeyListener() {
                        @Override
                        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                            if(event.getAction() == KeyEvent.ACTION_DOWN) {
                                if(keyCode == KeyEvent.KEYCODE_ENTER) {
                                    return true;
                                } else {
                                    BarcodeScanParser.append(keyCode, event.isShiftPressed());
                                }
                            }
                            return false;
                        }
                    });
                }
// End of H.M.Wang 2020-6-3 解决提示对话窗在显示时，扫码枪的信息被其劫持，而无法识别的问题
            }
        });
    }

    public void work() {
        Debug.i(TAG, "Started to work");

        mWorking = true;
        new Thread() {
            @Override
            public void run() {
                while(mWorking) {
                    String cmd = mStreamTransport.readLine();
                    if(null != cmd) {       // 连接还在
                        if(!cmd.isEmpty()) handle(cmd);
                    } else {                // 连接已经关闭
                        mStreamTransport.close();
                        mWorking = false;
                    }
                }
            }
        }.start();
    }

    public void close() {
        mStreamTransport.close();
        mWorking = false;
    }

    public void sendmsg(String msg) {
        mStreamTransport.writeLine(msg);
    }

    private void showPromptDlg(final String msg) {
        if(null != myHandler) {
            myHandler.post(new Runnable() {
                @Override
                public void run() {
                    if(null != mRemoteRecvedPromptDlg) {
                        mRemoteRecvedPromptDlg.show();
                        SimpleDateFormat sdf = new SimpleDateFormat("[mm:ss]\n");
                        mRemoteRecvedPromptDlg.setMessage(sdf.format(new Date()) + msg);
                    }
                }
            });
            myHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(null != mRemoteRecvedPromptDlg) {
                        mRemoteRecvedPromptDlg.hide();
                    }
                }
            }, 5000L);
        }
    }

    public void handle(String msg) {
        Debug.i(TAG, "--->fromPc: " + msg);
        PCCommand cmd = PCCommand.fromString(msg);
        if(null == cmd) return;

        // H.M.Wang 2020-1-8 提取命令ID
        mControlTabActivity.mPCCmdId = cmd.check;
        // End of H.M.Wang 2020-1-8 提取命令ID

// H.M.Wang 当解析命令失败时，抛弃这个命令
// H.M.Wang 2019-12-30 收到空命令的时候，返回错误
        if(null == cmd) {
            sendmsg(Constants.pcErr("<Null Command>"));
            return;
        }
// End of H.M.Wang 2019-12-30 收到空命令的时候，返回错误

        if (PCCommand.CMD_SEND_BIN.equalsIgnoreCase(cmd.command) ||  // LAN Printing
            PCCommand.CMD_SEND_BIN_S.equalsIgnoreCase(cmd.command)) {  // LAN Printing
            cacheBin(msg);

        } else if (PCCommand.CMD_DEL_LAN_BIN.equalsIgnoreCase(cmd.command) ||
                   PCCommand.CMD_DEL_LAN_BIN_S.equalsIgnoreCase(cmd.command)) {
            DataTransferThread.deleteLanBuffer(Integer.valueOf(cmd.content));

        } else if (PCCommand.CMD_RESET_INDEX.equalsIgnoreCase(cmd.command) ||
                   PCCommand.CMD_RESET_INDEX_S.equalsIgnoreCase(cmd.command)) {
            DataTransferThread aDTThread = DataTransferThread.getInstance(mContext);
            aDTThread.resetIndex();
            sendmsg(Constants.pcOk(msg));

// H.M.Wang 2019-12-16 支持网络下发计数器和动态二维码的值
        } else if (PCCommand.CMD_SET_REMOTE.equalsIgnoreCase(cmd.command) ||
                   PCCommand.CMD_SET_REMOTE_S.equalsIgnoreCase(cmd.command)) {
// H.M.Wang 2019-12-18 判断参数41，是否采用外部数据源，为true时才起作用
            if (SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_LAN ||
// H.M.Wang 2022-5-28 当数据源定义为PC命令的时候，通过串口传递WIFI的命令。但是，由于PC命令占用了数据源的LAN和LAN_FAST的位置，这里协议是冲突的，因此视作LAN和LAN_FAST打开，但是LAN的动作能够全部实现，LAN_FAST由于有多余的打印流程控制，不会动作
                SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_PC_COMMAND ||
// End of H.M.Wang 2022-5-28 当数据源定义为PC命令的时候，通过串口传递WIFI的命令。但是，由于PC命令占用了数据源的LAN和LAN_FAST的位置，这里协议是冲突的，因此视作LAN和LAN_FAST打开
// H.M.Wang 2024-2-22 追加一个GS1网络协议。内容与DATA_SOURCE_GS1_BRACE一样，只是数据从LAN来，走650或者600命令
                SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_LAN_GS1_BRACE ||
// End of H.M.Wang 2024-2-22 追加一个GS1网络协议。内容与DATA_SOURCE_GS1_BRACE一样，只是数据从LAN来，走650或者600命令
// H.M.Wang 2024-7-20 追加一个数据源，用来接收蓝牙数据
                SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_BLUETOOTH ||
// End of H.M.Wang 2024-7-20 追加一个数据源，用来接收蓝牙数据
// H.M.Wang 2020-6-28 追加专门为网络快速打印设置
                SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_FAST_LAN) {
// End of H.M.Wang 2020-6-28 追加专门为网络快速打印设置

// H.M.Wang 2023-3-11 追加网络通讯前置缓冲区功能
                PC_FIFO pc_FIFO = PC_FIFO.getInstance(mContext);
                if(pc_FIFO.PCFIFOEnabled()) {
                    showPromptDlg(cmd.content);
                    if(pc_FIFO.appendToFIFO(cmd.content)) {
                        sendmsg(Constants.pcOk(msg));
                    } else {
                        sendmsg(Constants.pcErr(msg));
                    }
                    return;
                }
// End of H.M.Wang 2023-3-11 追加网络通讯前置缓冲区功能

                DataTransferThread aDTThread = DataTransferThread.getInstance(mContext);
                if(aDTThread.isRunning()) {
                    aDTThread.setRemoteTextSeparated(cmd.content);
                    sendmsg(Constants.pcOk(msg));
                } else {
// H.M.Wang 2022-6-13 即使没有开始打印，也能够设置DT
                    SystemConfigFile.getInstance().setRemoteSeparated(cmd.content);
                    showPromptDlg(cmd.content);
//                    sendmsg(Constants.pcErr(msg));
                    sendmsg(Constants.pcOk(msg));
// End of H.M.Wang 2022-6-13 即使没有开始打印，也能够设置DT
                }
            } else {
                sendmsg(Constants.pcErr(msg));
            }
            // End.
// End. -----
// H.M.Wang 2022-6-1 新的外部文本处理函数，支持新的PC或者串口PC当中的DT设置命令（CMD_SET_REMOTE1和CMD_SET_REMOTE1_S),接收到的10个DT对应于10个全局DT桶的顺序
        } else if (PCCommand.CMD_SET_REMOTE1.equalsIgnoreCase(cmd.command) ||
                PCCommand.CMD_SET_REMOTE1_S.equalsIgnoreCase(cmd.command)) {
// H.M.Wang 2019-12-18 判断参数41，是否采用外部数据源，为true时才起作用
            if (SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_LAN ||
// H.M.Wang 2022-5-28 当数据源定义为PC命令的时候，通过串口传递WIFI的命令。但是，由于PC命令占用了数据源的LAN和LAN_FAST的位置，这里协议是冲突的，因此视作LAN和LAN_FAST打开，但是LAN的动作能够全部实现，LAN_FAST由于有多余的打印流程控制，不会动作
                SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_PC_COMMAND ||
// End of H.M.Wang 2022-5-28 当数据源定义为PC命令的时候，通过串口传递WIFI的命令。但是，由于PC命令占用了数据源的LAN和LAN_FAST的位置，这里协议是冲突的，因此视作LAN和LAN_FAST打开
// H.M.Wang 2024-2-22 追加一个GS1网络协议。内容与DATA_SOURCE_GS1_BRACE一样，只是数据从LAN来，走650或者600命令
                SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_LAN_GS1_BRACE ||
// End of H.M.Wang 2024-2-22 追加一个GS1网络协议。内容与DATA_SOURCE_GS1_BRACE一样，只是数据从LAN来，走650或者600命令
// H.M.Wang 2024-7-20 追加一个数据源，用来接收蓝牙数据
                SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_BLUETOOTH ||
// End of H.M.Wang 2024-7-20 追加一个数据源，用来接收蓝牙数据
// H.M.Wang 2020-6-28 追加专门为网络快速打印设置
                SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_FAST_LAN) {
// End of H.M.Wang 2020-6-28 追加专门为网络快速打印设置

// H.M.Wang 2023-3-11 追加网络通讯前置缓冲区功能
                PC_FIFO pc_FIFO = PC_FIFO.getInstance(mContext);
                if(pc_FIFO.PCFIFOEnabled()) {
                    showPromptDlg(cmd.content);
                    if(pc_FIFO.appendToFIFO(cmd.content)) {
                        sendmsg(Constants.pcOk(msg));
                    } else {
                        sendmsg(Constants.pcErr(msg));
                    }
                    return;
                }
// End of H.M.Wang 2023-3-11 追加网络通讯前置缓冲区功能

                DataTransferThread aDTThread = DataTransferThread.getInstance(mContext);
                if(aDTThread.isRunning()) {
                    aDTThread.setRemote1TextSeparated(cmd.content);
                    sendmsg(Constants.pcOk(msg));
                } else {
// H.M.Wang 2022-6-13 即使没有开始打印，也能够设置DT
                    SystemConfigFile.getInstance().setRemoteSeparated(cmd.content);
                    showPromptDlg(cmd.content);
//                    sendmsg(Constants.pcErr(msg));
                    sendmsg(Constants.pcOk(msg));
// End of H.M.Wang 2022-6-13 即使没有开始打印，也能够设置DT
                }
            } else {
                sendmsg(Constants.pcErr(msg));
            }
            // End.
// End. -----
// End of H.M.Wang 2022-6-1 新的外部文本处理函数，支持新的PC或者串口PC当中的DT设置命令（CMD_SET_REMOTE1和CMD_SET_REMOTE1_S),接收到的10个DT对应于10个全局DT桶的顺序
        } else if (PCCommand.CMD_PRINT.equalsIgnoreCase(cmd.command) ||
                   PCCommand.CMD_PRINT_S.equalsIgnoreCase(cmd.command)) {
            File msgfile = new File(cmd.content);
            if (!isTlkReady(msgfile)) {
                sendmsg(Constants.pcErr(msg));
                return;
            }

            mControlTabActivity.PrnComd = "100";
            mControlTabActivity.mObjPath = msgfile.getName();

            DataTransferThread aDTThread = DataTransferThread.getInstance(mContext);
            if(!aDTThread.isRunning() && !aDTThread.isPurging && !aDTThread.isCleaning) {
                Message message = mControlTabActivity.mHandler.obtainMessage(ControlTabActivity.MESSAGE_OPEN_TLKFILE);
                Bundle bundle = new Bundle();
                bundle.putString("file", mControlTabActivity.mObjPath);  // f表示信息名称
                bundle.putString(Constants.PC_CMD, msg);
                message.setData(bundle);
                mControlTabActivity.mHandler.sendMessage(message);
            } else {
                sendmsg(Constants.pcErr(msg));
            }
        } else if (PCCommand.CMD_CLEAN.equalsIgnoreCase(cmd.command) ||
                   PCCommand.CMD_CLEAN_S.equalsIgnoreCase(cmd.command)) {
            DataTransferThread aDTThread = DataTransferThread.getInstance(mContext);
            if(!aDTThread.isPurging) {
// H.M.Wang 2023-8-7 增加12头清洗功能。通过CMD_CLEAN或CMD_CLEAN_S的content指定，0为全清洗，1-12为指定头清洗
                try {
                    DataTransferThread.CleanHead = Integer.parseInt(cmd.content);
                } catch(NumberFormatException e) {
                    Debug.e(TAG, e.getMessage());
                }
// End of H.M.Wang 2023-8-7 增加12头清洗功能。通过CMD_CLEAN或CMD_CLEAN_S的content指定，0为全清洗，1-12为指定头清洗
                aDTThread.purge(mContext);
                sendmsg(Constants.pcOk(msg));
            } else {
                sendmsg(Constants.pcErr(msg));
            }
// H.M.Wang 2023-8-8 增加一个新的网络命令，SelectPen
        } else if (PCCommand.CMD_SEL_PEN.equalsIgnoreCase(cmd.command)) {
            if(cmd.content.length() <= 0) {
                sendmsg(Constants.pcErr(msg));
                return;
            }
            int selectPen = 0;
            int startPos = cmd.content.length()-12;
            startPos = startPos < 0 ? 0 : startPos;

            try {
                selectPen = Integer.valueOf(cmd.content.substring(startPos), 2);
                DataTransferThread.SelectPen = selectPen;
                sendmsg(Constants.pcOk(msg));
            } catch (NumberFormatException e) {
                sendmsg(Constants.pcErr(msg));
                return;
            }
// End of H.M.Wang 2023-8-8 增加一个新的网络命令，SelectPen
        } else if (PCCommand.CMD_SEND_FILE.equalsIgnoreCase(cmd.command) ||
                   PCCommand.CMD_SEND_FILE_S.equalsIgnoreCase(cmd.command)) {
            sendmsg(WriteFiles(msg));

        } else if (PCCommand.CMD_READ_COUNTER.equalsIgnoreCase(cmd.command) ||
                   PCCommand.CMD_READ_COUNTER_S.equalsIgnoreCase(cmd.command)) {
// H.M.Wang 2020-4-22 修改读取Counter命令返回格式
            StringBuilder sb = new StringBuilder();

            sb.append("" + mControlTabActivity.mCounter);

            IInkDevice inkManager = InkManagerFactory.inkManager(mContext);
// H.M.Wang 2023-8-14 使用喷头7的墨水值位置传递墨位状态，喷头8的墨水值位置传递溶剂状态。低为T，高为F。当使用协议6以外时为F
//            for(int i=0; i<8; i++) {
            for(int i=0; i<6; i++) {
                sb.append("|" + (int)(inkManager.getLocalInk(i)));
            }
            sb.append("|" + (mControlTabActivity.getLevelLow() ? "T" : "F"));
            sb.append("|" + (mControlTabActivity.getSolventLow() ? "T" : "F"));
// End of H.M.Wang 2023-8-14 使用喷头7的墨水值位置传递墨位状态，喷头8的墨水值位置传递溶剂状态。低为T，高为F。当使用协议6以外时为F

// H.M.Wang 2020-6-29 打印任务还没有启动时，DataTransferThread.getInstance(mContext)会自动生成instance，导致错误，应避免使用
            DataTransferThread aDTThread = DataTransferThread.getInstance(mContext);
            sb.append("|" + (aDTThread.isRunning() ? "T" : "F") + "|");
// H.M.Wang 2020-6-29 打印任务还没有启动时，DataTransferThread.getInstance(mContext)会自动生成instance，导致错误，应避免使用
// H.M.Wang 2023-3-13 增加一个PC_FIFO空位数的字段
            PC_FIFO pc_FIFO = PC_FIFO.getInstance(mContext);
            sb.append(pc_FIFO.getPCFIFOAvailableSize() + "|");
// End of H.M.Wang 2023-3-13 增加一个PC_FIFO空位数的字段
            sb.append(msg);
            sendmsg(Constants.pcOk(sb.toString()));
// End of H.M.Wang 2020-4-22 修改读取Counter命令返回格式

/*
// H.M.Wang 2020-7-1 临时版本，回复原来的回复格式
                                        for(int i=0;i<7;i++)
                                        {
                                            sendmsg("counter:" + mCounter+" |ink:" + mInkManager.getLocalInkPercentage(i) + "|state:" + (null != mDTransThread && mDTransThread.isRunning()));
                                            //获取INK无显示问题，赵工这地方改好，前面注示去掉就OK了
                                            sendmsg(Constants.pcOk(msg));
                                        }
// End of H.M.Wang 2020-7-1 临时版本，回复原来的回复格式
*/
        } else if (PCCommand.CMD_STOP_PRINT.equalsIgnoreCase(cmd.command) ||
                   PCCommand.CMD_STOP_PRINT_S.equalsIgnoreCase(cmd.command)) {
            //500停止打印
            Message message = mControlTabActivity.mHandler.obtainMessage(ControlTabActivity.MESSAGE_PRINT_STOP);
            Bundle bundle = new Bundle();
            bundle.putString(Constants.PC_CMD, msg);
            message.setData(bundle);
            message.sendToTarget();

        } else if (PCCommand.CMD_MAKE_TLK.equalsIgnoreCase(cmd.command) ||
                   PCCommand.CMD_MAKE_TLK_S.equalsIgnoreCase(cmd.command)) {
            sendmsg(mContext.getString(R.string.str_build_tlk_start));
            String[] parts = msg.split("\\|");
            for (int j = 0; j < parts.length; j++) {
                Debug.d(TAG, "--->parts[" + j + "] = " + parts[j]);
            }

            if (parts != null || parts.length > 4) {
                MakeTlk(parts[3]);
            }

        } else if (PCCommand.CMD_DEL_FILE.equalsIgnoreCase(cmd.command) ||
                   PCCommand.CMD_DEL_FILE_S.equalsIgnoreCase(cmd.command)) {
            if (deleteFile(cmd.content)) {
                sendmsg(Constants.pcOk(msg));
            } else {
                sendmsg(Constants.pcErr(msg));
            }
        } else if (PCCommand.CMD_DEL_DIR.equalsIgnoreCase(cmd.command) ||
                   PCCommand.CMD_DEL_DIR_S.equalsIgnoreCase(cmd.command)) {
            if (deleteDirectory(cmd.content)) {
                sendmsg(Constants.pcOk(msg));
            } else {
                sendmsg(Constants.pcErr(msg));
            }
                // H.M.Wang 2019-12-25 追加速度和清洗命令
        } else if(PCCommand.CMD_SET_DOTSIZE.equalsIgnoreCase(cmd.command)) {
            try {
// H.M.Wang 2019-12-27 暂时取消3.7倍的系数。修改设置参数为23。取值范围0-6000。 2019-12-28 内部保存在参数33
                SystemConfigFile.getInstance().setParamBroadcast(SystemConfigFile.INDEX_DOT_SIZE, Math.max(0, Math.min(6000, Integer.parseInt(cmd.content))));
//                                            SystemConfigFile.getInstance().setParamBroadcast(0, Math.round(3.7f * Integer.parseInt(cmd.content)));
// End of H.M.Wang 2019-12-27 暂时取消3.7倍的系数。修改设置参数为23。取值范围0-6000
                SystemConfigFile.getInstance().saveConfig();
                DataTransferThread aDTThread = DataTransferThread.getInstance(mContext);
                if(aDTThread.isRunning()) {
// H.M.Wang 2019-12-29 修改在打印状态下设置FPGA参数的逻辑
                    DataTask task = aDTThread.getCurData();
// 2020-5-8												FpgaGpioOperation.clean();
                    FpgaGpioOperation.updateSettings(mContext, task, FpgaGpioOperation.SETTING_TYPE_NORMAL);
//												mDTransThread.mNeedUpdate = true;
//												while(mDTransThread.mNeedUpdate) {
//													Thread.sleep(10);
//												}
                    FpgaGpioOperation.init();
// H.M.Wang 2020-7-9 取消下发参数设置后重新下发打印缓冲区操作
//												mDTransThread.resendBufferToFPGA();
// End of H.M.Wang 2020-7-9 取消下发参数设置后重新下发打印缓冲区操作
// End of H.M.Wang 2019-12-29 修改在打印状态下设置FPGA参数的逻辑
                }
                sendmsg(Constants.pcOk(msg));
            } catch (NumberFormatException e) {
                Debug.e(TAG, e.getMessage());
            }
        } else if(PCCommand.CMD_SET_CLEAN.equalsIgnoreCase(cmd.command)) {
            DataTransferThread aDTThread = DataTransferThread.getInstance(mContext);
            if(aDTThread.isRunning()) {
                DataTask task = aDTThread.getCurData();

                int param0 = SystemConfigFile.getInstance().getParam(0);
// H.M.Wang 2019-12-27 修改取值，以达到下发FPGA时参数4的值为9
                SystemConfigFile.getInstance().setParam(0, 18888);
// End of H.M.Wang 2019-12-27 修改取值，以达到下发FPGA时参数4的值为9
                FpgaGpioOperation.updateSettings(mContext, task, FpgaGpioOperation.SETTING_TYPE_NORMAL);
// H.M.Wang 2019-12-27 间隔时间修改为10ms
                try{Thread.sleep(10);}catch(Exception e){};
// End of H.M.Wang 2019-12-27 间隔时间修改为10ms
                SystemConfigFile.getInstance().setParam(0, param0);
// 2020-5-8											FpgaGpioOperation.clean();
                FpgaGpioOperation.updateSettings(mContext, task, FpgaGpioOperation.SETTING_TYPE_NORMAL);
// H.M.Wang 2019-12-27 重新启动打印
//											mDTransThread.mNeedUpdate = true;
//											while(mDTransThread.mNeedUpdate) {
//												Thread.sleep(10);
//											}
                FpgaGpioOperation.init();
// H.M.Wang 2020-7-9 取消下发参数设置后重新下发打印缓冲区操作
//												mDTransThread.resendBufferToFPGA();
// End of H.M.Wang 2020-7-9 取消下发参数设置后重新下发打印缓冲区操作
// End of H.M.Wang 2019-12-27 重新启动打印
            }
                // End of H.M.Wang 2019-12-25 追加速度和清洗命令
            sendmsg(Constants.pcOk(msg));
// H.M.Wang 2020-7-1 追加一个计数器设置数值命令
        } else if(PCCommand.CMD_SET_COUNTER.equalsIgnoreCase(cmd.command)) {
            try {
                int cIndex = Integer.valueOf(cmd.content);
                if(cIndex < 0 || cIndex > 9) {
                    Debug.e(TAG, "CMD_SET_COUNTER command, Index overflow.");
                    sendmsg(Constants.pcErr(msg));
                } else {
                    try {
                        int cValue = Integer.valueOf(cmd.note2);
                        SystemConfigFile.getInstance().setParamBroadcast(cIndex + SystemConfigFile.INDEX_COUNT_1, cValue);
                        RTCDevice.getInstance(mContext).write(cValue, cIndex);
                        DataTransferThread dt = DataTransferThread.mInstance;
                        if(null != dt && dt.isRunning()) {
                            resetCounterIfNeed();
                            dt.mNeedUpdate = true;
// H.M.Wang 2020-7-9 追加计数器重置标识
                            dt.mCounterReset = true;
// End of H.M.Wang 2020-7-9 追加计数器重置标识
                        }

                        sendmsg(Constants.pcOk(msg));
                    } catch (NumberFormatException e) {
                        Debug.e(TAG, "CMD_SET_COUNTER command, invalid value.");
                        sendmsg(Constants.pcErr(msg));
                    }
                }
            } catch (NumberFormatException e) {
                Debug.e(TAG, "CMD_SET_COUNTER command, invalid index.");
                sendmsg(Constants.pcErr(msg));
            }
// End of H.M.Wang 2020-7-1 追加一个计数器设置数值命令

        } else if(PCCommand.CMD_SET_TIME.equalsIgnoreCase(cmd.command)) {
            try {
                int cYear = Integer.valueOf(cmd.content.substring(0,2));
                int cMonth = Integer.valueOf(cmd.content.substring(2,4));
                int cDate = Integer.valueOf(cmd.content.substring(4,6));
                int cHour = Integer.valueOf(cmd.content.substring(6,8));
                int cMinute = Integer.valueOf(cmd.content.substring(8,10));
                int cSecond = Integer.valueOf(cmd.content.substring(10,12));

                if(cYear < 0 || cYear > 99) {
                    Debug.e(TAG, "CMD_SET_TIME command, invalid year.");
                    sendmsg(Constants.pcErr(msg));
                } else if(cMonth < 1 || cMonth > 12) {
                    Debug.e(TAG, "CMD_SET_TIME command, invalid month.");
                    sendmsg(Constants.pcErr(msg));
                } else if(cDate < 1 || cDate > 31) {
                    Debug.e(TAG, "CMD_SET_TIME command, invalid date.");
                    sendmsg(Constants.pcErr(msg));
                } else if(cHour < 0 || cHour > 23) {
                    Debug.e(TAG, "CMD_SET_TIME command, invalid hour.");
                    sendmsg(Constants.pcErr(msg));
                } else if(cMinute < 0 || cMinute > 59) {
                    Debug.e(TAG, "CMD_SET_TIME command, invalid minute.");
                    sendmsg(Constants.pcErr(msg));
                } else if(cSecond < 0 || cSecond > 59) {
                    Debug.e(TAG, "CMD_SET_TIME command, invalid second.");
                    sendmsg(Constants.pcErr(msg));
                } else {
                    Calendar c = Calendar.getInstance();

                    c.set(cYear + 2000, cMonth-1, cDate, cHour, cMinute, cSecond);
                    SystemClock.setCurrentTimeMillis(c.getTimeInMillis());
                    RTCDevice rtcDevice = RTCDevice.getInstance(mContext);
                    rtcDevice.syncSystemTimeToRTC(mContext);
                    Debug.d(TAG, "Set time to: " + (cYear + 2000) + "-" + cMonth + "-" + cDate + " " + cHour + ":" + cMinute + ":" + cSecond);
                    sendmsg(Constants.pcOk(msg));
                }
            } catch (Exception e) {
                Debug.e(TAG, "CMD_SET_TIME command, " + e.getMessage() + ".");
                sendmsg(Constants.pcErr(msg));
            }
// H.M.Wang 2020-7-28 追加一个设置参数命令
        } else if(PCCommand.CMD_SET_PARAMS.equalsIgnoreCase(cmd.command)) {
            try {
                int cIndex = Integer.valueOf(cmd.content);
                cIndex--;
                if(cIndex < 0 || cIndex > 63) {
                    Debug.e(TAG, "Invalid PARAM index.");
                    sendmsg(Constants.pcErr(msg));
                } else {
                    try {
                        int cValue = Integer.valueOf(cmd.note2);
//													if(cIndex == 3 || cIndex == 0 || cIndex == 1 || cIndex == 32 || (cIndex >= SystemConfigFile.INDEX_COUNT_1 && cIndex <= SystemConfigFile.INDEX_COUNT_10)) {
                        SystemConfigFile.getInstance().setParamBroadcast(cIndex, cValue);
//													} else {
//														mSysconfig.setParam(cIndex, cValue);
//													}
                        sendmsg(Constants.pcOk(msg));
                    } catch (NumberFormatException e) {
                        Debug.e(TAG, "Invalid PARAM value.");
                        sendmsg(Constants.pcErr(msg));
                    }
                }
            } catch (NumberFormatException e) {
                Debug.e(TAG, "Invalid PARAM index.");
                sendmsg(Constants.pcErr(msg));
            }
// End of H.M.Wang 2020-7-28 追加一个设置参数命令
// H.M.Wang 2020-9-28 追加一个心跳协议
        } else if(PCCommand.CMD_HEARTBEAT.equalsIgnoreCase(cmd.command)) {
                mControlTabActivity.mLastHeartBeat = System.currentTimeMillis();
                sendmsg(Constants.pcOk(msg));
// End of H.M.Wang 2020-9-28 追加一个心跳协议
// H.M.Wang 2021-2-4 追加软启动打印命令
        } else if(PCCommand.CMD_SOFT_PHO.equalsIgnoreCase(cmd.command)) {
            FpgaGpioOperation.softPho();
            sendmsg(Constants.pcOk(msg));
// End of H.M.Wang 2021-2-4 追加软启动打印命令
// H.M.Wang 2023-3-13 追加一个清除PCFIFO的网络命令
        } else if(PCCommand.CMD_CLEAR_FIFO.equalsIgnoreCase(cmd.command)) {
            PC_FIFO pc_FIFO = PC_FIFO.getInstance(mContext);
            pc_FIFO.clearBuffer();
            FpgaGpioOperation.clearFIFO();
            sendmsg(Constants.pcOk(msg));
// End of H.M.Wang 2023-3-13 追加一个清除PCFIFO的网络命令
// H.M.Wang 2023-10-28 增加打印方向(Direction)和倒置(Inverse)
        } else if(PCCommand.CMD_DIRECTION.equalsIgnoreCase(cmd.command)) {
            try {
                DataTransferThread.sDirectionCmd = Integer.parseInt(cmd.content);
                DataTransferThread aDTThread = DataTransferThread.getInstance(mContext);
                if(null != aDTThread && aDTThread.isRunning()) {
                    aDTThread.mNeedUpdate = true;
                }
                sendmsg(Constants.pcOk(msg));
            } catch(NumberFormatException e) {
                Debug.e(TAG, e.getMessage());
                sendmsg(Constants.pcErr(msg));
            }
        } else if(PCCommand.CMD_INVERSE.equalsIgnoreCase(cmd.command)) {
            try {
                DataTransferThread.sInverseCmd = Integer.parseInt(cmd.content);
                DataTransferThread aDTThread = DataTransferThread.getInstance(mContext);
                if(null != aDTThread && aDTThread.isRunning()) {
                    aDTThread.mNeedUpdate = true;
                }
                sendmsg(Constants.pcOk(msg));
            } catch(NumberFormatException e) {
                Debug.e(TAG, e.getMessage());
                sendmsg(Constants.pcErr(msg));
            }
// End of H.M.Wang 2023-10-28 增加打印方向(Direction)和倒置(Inverse)
// H.M.Wang 2023-11-2 追加两个网络命令，同步器(encppr)和旋转(slant)
        } else if(PCCommand.CMD_ENCPPR.equalsIgnoreCase(cmd.command)) {
            try {
                SystemConfigFile.getInstance().setParamBroadcast(SystemConfigFile.INDEX_ENCODER_PPR, Integer.parseInt(cmd.content));
                sendmsg(Constants.pcOk(msg));
            } catch(NumberFormatException e) {
                Debug.e(TAG, e.getMessage());
                sendmsg(Constants.pcErr(msg));
            }
        } else if(PCCommand.CMD_SLANT.equalsIgnoreCase(cmd.command)) {
            try {
                SystemConfigFile.getInstance().setParamBroadcast(SystemConfigFile.INDEX_SLANT, Integer.parseInt(cmd.content));
                sendmsg(Constants.pcOk(msg));
            } catch(NumberFormatException e) {
                Debug.e(TAG, e.getMessage());
                sendmsg(Constants.pcErr(msg));
            }
// End of H.M.Wang 2023-11-2 追加两个网络命令，同步器(encppr)和旋转(slant)
        } else {
            sendmsg(Constants.pcErr(msg));
        }

    }

    private void cacheBin(String message) {
        Debug.i(TAG, "--->cacheBin: " + message);

        PCCommand cmd = PCCommand.fromString(message);
        int length = Integer.valueOf(cmd.size);
        Debug.d(TAG, "--->cacheBin length: " + length);
        if (length <= 16) {
            sendmsg(Constants.pcErr(message));
            return;
        }

        byte[] buffer = new byte[length];

        if( mStreamTransport.read(buffer) == -1) {
            sendmsg(Constants.pcErr(message));
        } else {
            char[] remoteBin = new char[(length - 16) / 2];
            for (int i = 0; i < remoteBin.length; i++) {
                remoteBin[i] = (char) ((char)(buffer[2 * i + 16 + 1] << 8) + (char)(buffer[2 * i + 16] & 0x0ff));
                remoteBin[i] = (char)(remoteBin[i] & 0x0ffff);
            }

            DataTransferThread.setLanBuffer(mContext, Integer.valueOf(cmd.content), remoteBin);

            sendmsg(Constants.pcOk(message));
        }
    }

    private boolean isTlkReady(File tlk) {
        if (tlk == null) {
            return false;
        }
        File[] files = tlk.listFiles();
        if (files == null || files.length <= 0) {
            return false;
        }
        List<String> list = new ArrayList<String>();
        for (int i = 0; i < files.length; i++) {
            list.add(files[i].getName());
        }
// H.M.Wang 2022-6-10 因为群组当中没有1.bin，因此网络打印群组会失败
//        if (list.contains("1.TLK") && list.contains("1.bin")) {
        if (list.contains("1.TLK")) {
// End of H.M.Wang 2022-6-10 因为群组当中没有1.bin，因此网络打印群组会失败
            return true;
        }
        return false;
    }

    private String WriteFiles(String msg ) {
        // H.M.Wang 追加接收文件长度信息
        PCCommand cmd = PCCommand.fromString(msg);
        int length = Integer.valueOf(cmd.size);
        Debug.d(TAG, "--->length: " + length);

        FileOutputStream fos = null;
        String retString = "";

        try {
            String savePath = msg.substring(msg.indexOf("/")+1,msg.lastIndexOf("/"));

            String TmpFiles = msg.substring(msg.lastIndexOf("/"));
            TmpFiles = TmpFiles.substring(TmpFiles.indexOf("/")+1, TmpFiles.indexOf("|"));

            Paths_Create paths = new Paths_Create();
            String TmpsavePath= paths.CreateDir(msg);

            savePath = TmpsavePath + TmpFiles;

            fos = new FileOutputStream(savePath, false);

            byte[] buffer = new byte[8192];
            int totalReceived = 0;

            while (totalReceived < length) {
                int readBytes = 0;

                readBytes = mStreamTransport.read(buffer);
                if(readBytes == -1) break;
                fos.write(buffer, 0, readBytes);
                totalReceived += readBytes;
                Debug.d(TAG, "--->read: " + totalReceived);
            }
            retString = Constants.pcOk(msg);
        } catch(Exception e){
            Debug.e(TAG, e.getMessage());
            retString = Constants.pcErr(msg);
        } finally {
            try {
                if(null != fos) {
                    fos.flush();
                    fos.close();
                }
            } catch(Exception e) {
                Debug.e(TAG, e.getMessage());
            }
        }

        return retString;
    }

// H.M.Wang 2022-6-8 线程里面不能new Handler, 必须使用预先生成好的Handler。这套机制挺别扭，但是为了将就以前的message.reCreate函数中的第二个参数，只能这样
    private boolean mCreatingTLK = false;
    private String mMakeTlkMsg = "";
    private Handler myHandler=null;//rec infor prpcess handle

    public void handleReCreateResult(Message msg) {
        if(mCreatingTLK) {
            if (msg.what == EditTabSmallActivity.HANDLER_MESSAGE_SAVE_SUCCESS) {
                String cmd = msg.getData().getString(Constants.PC_CMD);
                sendmsg(Constants.pcOk(cmd));
            } else {
                sendmsg(Constants.pcErr(mMakeTlkMsg));
            }
            mCreatingTLK = false;
        }
    }
// End of H.M.Wang 2022-6-8 线程里面不能new Handler, 必须使用预先生成好的Handler。这套机制挺别扭，但是为了将就以前的message.reCreate函数中的第二个参数，只能这样

    private void MakeTlk(final String msg) {
        Debug.d(TAG, "--->msg: " + msg);
        File file = new File(msg);
        if (file == null) {
            sendmsg(Constants.pcErr(msg));
            return;
        }
        String tlk = file.getAbsolutePath();
        String Name = file.getParentFile().getName();
        if (!tlk.endsWith("TLK")) {
            sendmsg(Constants.pcErr(msg));
            return;
        }
        Debug.d(TAG, "--->tlk: " + tlk + "   Name = " + Name);
        MessageForPc message = new MessageForPc(mContext, tlk, Name);

// H.M.Wang 2022-6-8 线程里面不能new Handler, 必须使用预先生成好的Handler。这套机制挺别扭，但是为了将就以前的message.reCreate函数中的第二个参数，只能这样
        mCreatingTLK = true;
        mMakeTlkMsg = msg;
        message.reCreate(mContext, myHandler, msg);
/*
        message.reCreate(mContext, new Handler(){
            public void handleMessage(Message msg1) {
                if (msg1.what == EditTabSmallActivity.HANDLER_MESSAGE_SAVE_SUCCESS) {
                    String cmd = msg1.getData().getString(Constants.PC_CMD);
                    sendmsg(Constants.pcOk(cmd));
                } else {
                    sendmsg(Constants.pcErr(msg));
                }
            }
        }, msg);
*/
// End of H.M.Wang 2022-6-8 线程里面不能new Handler, 必须使用预先生成好的Handler。这套机制挺别扭，但是为了将就以前的message.reCreate函数中的第二个参数，只能这样
    }

    private boolean deleteFile(String filePath) {
        Debug.d(TAG, "--->filePath: " + filePath);

        File file = new File(filePath);
        if(file.exists()) {
            if(file.isFile()){
                file.delete();
//                System.gc();
                return true;
            }
        }
        return false;
    }

    private boolean deleteDirectory(String filePath) {
        boolean flag = false;

        //如果filePath不以文件分隔符结尾，自动添加文件分隔符
//        filePath = filePath.substring(filePath.indexOf("/"), filePath.lastIndexOf("/"));
        if (!filePath.endsWith(File.separator)) {
            filePath = filePath + File.separator;
        }

        Debug.d(TAG, "--->filePath: " + filePath);

        File dirFile = new File(filePath);
        if (!dirFile.exists() || !dirFile.isDirectory()) {
            return false;
        }

        flag = true;
        File[] files = dirFile.listFiles();
        //遍历删除文件夹下的所有文件(包括子目录)
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) {
                //删除子文件
                flag = deleteFile(files[i].getAbsolutePath());
                if (!flag) break;
            } else {
                //删除子目录
                flag = deleteDirectory(files[i].getAbsolutePath());
                if (!flag) break;
            }
        }
        if (!flag) return false;
        //删除当前空目录
        return dirFile.delete();
    }

    private void resetCounterIfNeed() {
        DataTransferThread aDTThread = DataTransferThread.getInstance(mContext);

        List<DataTask> tasks = aDTThread.getData();
        if (tasks == null) {
            return;
        }

        SystemConfigFile config = SystemConfigFile.getInstance(mContext);
        for (DataTask task : tasks) {
            if (task == null) {
                continue;
            }
            List<BaseObject> objects = task.getObjList();
            for (BaseObject object : objects) {
                if (object instanceof CounterObject) {
                    ((CounterObject) object).setValue(config.getParam(SystemConfigFile.INDEX_COUNT_1 + ((CounterObject) object).getmCounterIndex()));
                }
            }
        }
    }

}
