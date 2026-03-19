package com.industry.printer.Collaboration;

import android.content.Context;

import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.StreamTransport;

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

    public ServerManager(Context ctx) {
        mContext = ctx;
        mExecutor = Executors.newFixedThreadPool(3);
    }

    public void start() {
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
                            Socket socket = mServerSocket.accept();
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
                        if(null != cmd) {       // 连接还在
                            if(!cmd.isEmpty()) handle(cmd);
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

    private void handle(final String msg) {
        try {
            if(HelloKitty.equals(msg)) {
                StreamTransport st = new StreamTransport(mWorkingSocket.getInputStream(), mWorkingSocket.getOutputStream());
                st.writeLine(msg + "01");
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
