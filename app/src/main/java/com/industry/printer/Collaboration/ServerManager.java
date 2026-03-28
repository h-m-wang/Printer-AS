package com.industry.printer.Collaboration;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.industry.printer.ControlTabActivity;
import com.industry.printer.MainActivity;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.StreamTransport;
import com.industry.printer.pccommand.PCCommandHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerManager {
    private static final String TAG = ServerManager.class.getSimpleName();

    private static final int PORT = 4551;           // port number;
    private Context mContext;
    private boolean mRunning;
    private ServerSocket mServerSocket;
    private Socket mWorkingSocket;
    private ExecutorService mExecutor;
    private ControlTabActivity mControlTabActivity;

    public ServerManager(Context ctx, ControlTabActivity act) {
        mContext = ctx;
        mExecutor = Executors.newFixedThreadPool(3);
        mControlTabActivity = act;
    }

    public void start() {
        Debug.i(TAG, "Start as Server");
        mServerSocket = null;
        mWorkingSocket = null;

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
                            socket.setSoTimeout(2000);
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

        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    StreamTransport st = new StreamTransport(socket.getInputStream(), socket.getOutputStream());
                    while(!socket.isClosed()) {
                        String cmd = st.readLine();
                        if(null != cmd && !cmd.isEmpty()) {       // 连接还在并且没有超时，收到了实际数据
                            handle(cmd);
                        } else {                // 连接已经关闭
                            socket.close();
                        }
                        try{Thread.sleep(10);}catch(Exception e){};
                    }
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private final String HelloKitty = "Hello Kitty";
    PCCommandHandler pcCmdHdl;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            pcCmdHdl.handleReCreateResult(msg);
        }
    };

    private void handle(final String msg) {
        try {
            if(HelloKitty.equals(msg)) {
                StreamTransport st = new StreamTransport(mWorkingSocket.getInputStream(), mWorkingSocket.getOutputStream());
                st.writeLine(msg + "01");
            } else {
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
}
