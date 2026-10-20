package com.industry.printer.Collaboration;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.industry.printer.Constants.Constants;
import com.industry.printer.ControlTabActivity;
import com.industry.printer.FileFormat.SystemConfigFile;
import com.industry.printer.MainActivity;
import com.industry.printer.Socket_Server.PCCommand;
import com.industry.printer.Socket_Server.Paths_Create;
import com.industry.printer.Utils.ConfigPath;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.LibUpgrade;
import com.industry.printer.Utils.StreamTransport;
import com.industry.printer.pccommand.PCCommandHandler;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PrinterSide {
    private static final String TAG = PrinterSide.class.getSimpleName();

    private static final int PORT = 4551;           // port number;
    private Context mContext;
    private boolean mRunning;
    private ServerSocket mServerSocket;
    private Socket mWorkingSocket;
    private StreamTransport mStreamTransport;
    private ExecutorService mExecutor;
    private ControlTabActivity mControlTabActivity;

    public PrinterSide(Context ctx, ControlTabActivity act) {
        mContext = ctx;
        mExecutor = Executors.newFixedThreadPool(3);
        mControlTabActivity = act;
    }

    public void start() {
        Debug.i(TAG, "Start as Server");
        mServerSocket = null;
        mWorkingSocket = null;
        mStreamTransport = null;

        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    mServerSocket = new ServerSocket(PORT);
                    mRunning = true;

                    while(mRunning) {
                        try {
                            Debug.i(TAG, "Waiting connection");
                            Socket socket = mServerSocket.accept();
                            Debug.i(TAG, "Incoming connection");
                            socket.setSoTimeout(15*1000);
                            work(socket);
                        } catch(IOException e) {
                            e.printStackTrace();
                        }
                        try {Thread.sleep(1000);} catch(Exception e){}
                    }
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void close() {
        mRunning = false;
        try {
            if(null != mWorkingSocket && !mWorkingSocket.isClosed()) mWorkingSocket.close();
            if(null != mServerSocket && !mServerSocket.isClosed()) mServerSocket.close();
            if(null != mStreamTransport) mStreamTransport.close();

        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private void work(final Socket socket) {
        Debug.i(TAG, "Started to work");
        if(null != mWorkingSocket && !mWorkingSocket.isClosed()) {
            try {
                mWorkingSocket.close();
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
        mWorkingSocket = socket;
        try {
            mStreamTransport = new StreamTransport(socket.getInputStream(), socket.getOutputStream());
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    while(true) {
                        String cmd = mStreamTransport.readLine();
                        if(null != cmd) {       // 连接还在
                            if(!cmd.isEmpty()) process(cmd); // 收到了实际数据
                        } else {                // 连接已经关闭
                            break;
                        }
                        try{Thread.sleep(10);}catch(Exception e){};
                    }
                    Debug.i(TAG, "Connection Closed");
                }
            });
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private final String HelloKitty = "Hello Kitty";
    private final String CMD_RECOGNIZER = "000C";
    PCCommandHandler pcCmdHdl;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            pcCmdHdl.handleReCreateResult(msg);
        }
    };

    private void process(String msg) {
        Debug.i(TAG, "--->fromController: " + msg);
        try {
            if(HelloKitty.equals(msg)) {
                mStreamTransport.writeLine(msg + " " + SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_LOCAL_ID));
            } else if(msg.startsWith(CMD_RECOGNIZER)){
                handle(msg);
            } else if(msg.startsWith("000B")){
                pcCmdHdl = new PCCommandHandler(
                        mContext,
                        new StreamTransport(mWorkingSocket.getInputStream(), mWorkingSocket.getOutputStream()),
                        mControlTabActivity,
                        mHandler);
                pcCmdHdl.handle(msg);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private int writeFiles(String savePath, int length) {
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(savePath, false);

            byte[] buffer = new byte[8192];
            int totalReceived = 0;

            while (totalReceived < length) {
                int readBytes = Math.min(length-totalReceived, 8192);

                readBytes = mStreamTransport.read(buffer, 0, readBytes);
                if(readBytes == -1) break;
                fos.write(buffer, 0, readBytes);
                totalReceived += readBytes;
            }
            Debug.d(TAG, "--->read: " + totalReceived + "; total: " + length);
            return 0;
        } catch(Exception e){
            Debug.e(TAG, e.getMessage());
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

        return -1;
    }

    private void handle(String msg) throws IOException {
        PCCommand cmd = PCCommand.fromString(msg);
        if(null == cmd) return;

        if("UpgradeAPK".equalsIgnoreCase(cmd.command)) { // 升级apk
            int length = Integer.valueOf(cmd.size);
            Debug.d(TAG, "--->length: " + length);

            String tmpFiles = msg.substring(msg.lastIndexOf("/"));
            tmpFiles = tmpFiles.substring(tmpFiles.indexOf("/")+1, tmpFiles.indexOf("|"));

            Paths_Create paths = new Paths_Create();
            String TmpsavePath= paths.CreateDir(msg);

            String savePath = TmpsavePath + tmpFiles;

            if(writeFiles(savePath, length) == 0) {
                Debug.d(TAG, Constants.pcOk(msg));
                mStreamTransport.writeLine(Constants.pcOk(msg));
            } else {
                Debug.d(TAG, Constants.pcErr(msg));
                mStreamTransport.writeLine(Constants.pcErr(msg));
            }
        }
    }
}
