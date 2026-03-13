package com.industry.printer.Collaboration;

import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.StreamTransport;
import com.industry.printer.pccommand.ClientSocket;
import com.industry.printer.pccommand.PCCommandHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerManager {
    private static final String TAG = ServerManager.class.getSimpleName();

    private static final int PORT = 4551;           // port number;
    private boolean mRunning;
    private Socket mWordingSocket;
    private StreamTransport mStreamTransport;

    private void acceptConnection() {
        mRunning = true;
        mWordingSocket = null;
        mStreamTransport = null;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final ServerSocket serverSocket = new ServerSocket(PORT);

                    while(mRunning) {
                        try {
                            if(mWordingSocket == null || mWordingSocket.isClosed()) {
                                mWordingSocket = serverSocket.accept();
                                mWordingSocket.setSoTimeout(2000);
                                mStreamTransport = new StreamTransport(mWordingSocket.getInputStream(), mWordingSocket.getOutputStream());
                            }
                        } catch(IOException e) {
                            Debug.e(TAG, e.getMessage());
                        }
                        try {Thread.sleep(1000);} catch(Exception e){}
                    }
                } catch(IOException e) {
                    Debug.e(TAG, e.getMessage());
                }
            }
        }).start();
    }

    class ServerThread extends Thread {
        private static final int PORT = 3550; // port number;
        private ServerSocket mServerSocket = null; //socket service object

        public ServerThread() {
            mServerSocket = null;
        }

        public void stopServer(){
            try {
                if(null != mServerSocket){
                    interrupt();
                    mServerSocket.close();
                }
            } catch (IOException e) {
                Debug.e(TAG, e.getMessage());
            }
        }

        @Override
        public void run() {
            try {
                mServerSocket = new ServerSocket(PORT);
            } catch (IOException e) {
                Debug.e(TAG, e.getMessage());
                return;
            }

            Socket client;

            while(!isInterrupted()) {
                try {
                    client = mServerSocket.accept();
                    client.setSoTimeout(2 * 1000);
                    //client.setSoTimeout(5000);
/*
                    if(null != mSocketHandler) {
                        mSocketHandler.close();
                    }
                    if(null != mClientSocket) {
                        mClientSocket.close();
                    }

                    mClientSocket = new ClientSocket(client, mContext);
                    mSocketHandler = new PCCommandHandler(mContext, mClientSocket.getStreamTransport(), mControlTabActivity, mHandler);
                    mSocketHandler.work();*/
                }catch ( IOException e) {
                    Debug.e(TAG, e.getMessage());
                }
            }
        }
    }
}
