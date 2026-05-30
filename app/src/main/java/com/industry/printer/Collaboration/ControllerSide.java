package com.industry.printer.Collaboration;

import android.content.Context;

import com.industry.printer.ControlTabActivity;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.StreamTransport;
import com.industry.printer.Utils.StringUtil;
import com.industry.printer.pccommand.PCCommandManager;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ControllerSide {
    private static final String TAG = ControllerSide.class.getSimpleName();

    private static final int PORT = 4551;           // port number;
    private Context mContext;
    private ExecutorService mExecutor;

    private List<ConnectedDevice> mDevices;

    public interface TransferListener {
        //        public void onSent();
        public void onRecv(ConnectedDevice dev, String recv);
//        public void onError(String err);
    }
    private TransferListener mConnectionStatusListner;

    public class ConnectedDevice {
        private String mDevID;
        private String mIPAddress;
        private Socket mSocket;

        public ConnectedDevice(String id, String ip, Socket socket) {
            mDevID = id;
            mIPAddress = ip;
            mSocket = socket;
        }

        public void setDevID(String id) {
            mDevID = id;
        }

        public String getDevID() {
            return mDevID;
        }

        public void setIPAddress(String addr) {
            mIPAddress = addr;
        }

        public String getIPAddress() {
            return mIPAddress;
        }

        public void setSocket(Socket socket) {
            mSocket = socket;
        }

        public Socket getSocket() {
            return mSocket;
        }

        public void sendCmd(final String cmd, final TransferListener l) {
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    synchronized (ConnectedDevice.this) {
                        try {
                            StreamTransport st = new StreamTransport(mSocket.getInputStream(), mSocket.getOutputStream());
                            st.writeLine(cmd);
                            String recv;
                            while(true) {
                                recv = st.readLine();
                                if(null == recv) break;     // 连接已经断开，直接返回
                                if(!recv.isEmpty()) {       // 如果接收到实际内容，则回调返回；如果超时返回""，则继续等待
                                    if (null != l) l.onRecv(ConnectedDevice.this, recv);
                                    break;
                                }
                                try{Thread.sleep(10);}catch(Exception e){};
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    }

    private Timer mTimer;
    private static ControllerSide mInstance = null;
    public static ControllerSide getInstance() {
        return mInstance;
    }

    public ControllerSide(Context ctx, TransferListener l) {
        mInstance = this;
        mContext = ctx;
        mExecutor = Executors.newFixedThreadPool(20);
        mDevices = new ArrayList<>();
        mConnectionStatusListner = l;
        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(null != mDevices && mDevices.size() > 0) {
                    for(ConnectedDevice dev : mDevices) {
                        dev.sendCmd(HelloKitty, new TransferListener() {
                            @Override
                            public void onRecv(ConnectedDevice dev, String recv) {
                                if(null != mConnectionStatusListner) mConnectionStatusListner.onRecv(dev, recv);
//                                if(null != recv && recv.isEmpty()) {
//                                    try {
//                                        dev.mSocket.connect(new InetSocketAddress(dev.mIPAddress, PORT),3000);
//                                    } catch(IOException e) {
//                                        e.printStackTrace();
//                                    }
//                                }
                            }
                        });
                    }
                }
            }
        }, 3000L, 5000L);
        Debug.i(TAG, "Start as Client");
    }

    private List<String> findReachables() {
        String hostAddress = ControlTabActivity.getLocalIpAddress();
//        Debug.d(TAG, "本机IP: " + hostAddress);

        String subnet = hostAddress.substring(0, hostAddress.lastIndexOf('.'));
//        Debug.d(TAG, "扫描网段: " + subnet + ".1/254");

        final List<String> aliveIPs = new ArrayList<>();
        final CountDownLatch latch = new CountDownLatch(254);

        for (int i = 1; i <= 254; i++) {
            final String targetIp = subnet + "." + i;
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        InetAddress addr = InetAddress.getByName(targetIp);
                        if (addr.isReachable(1000)) {
                            Debug.d(TAG, "发现设备: " + targetIp);
                            synchronized (aliveIPs) {
                                aliveIPs.add(targetIp);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return aliveIPs;
    }

    private final String HelloKitty = "Hello Kitty";

    public void findPrinters() {
        List<String> serverCandidates = findReachables();
        if(null != serverCandidates) {
            final List<ConnectedDevice> devices = new ArrayList<>();
            final CountDownLatch latch = new CountDownLatch(serverCandidates.size());

            for (final String server : serverCandidates) {
                mExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Socket socket = new Socket();
                            socket.setSoTimeout(2000);
                            socket.connect(new InetSocketAddress(server, PORT),3000);

                            final ConnectedDevice device = new ConnectedDevice("", server, socket);
                            device.sendCmd(HelloKitty, new TransferListener() {
                                @Override
                                public void onRecv(ConnectedDevice dev, String recv) {
                                    if (null != recv && recv.startsWith(HelloKitty)) {
                                        synchronized (devices) {
                                            devices.add(device);
                                            Debug.d(TAG, "Device: [" + server + "] added.");
                                        }
                                    }
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            latch.countDown();
                        }
                    }
                });
            }
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mDevices = devices;
            Debug.d(TAG, "Done.");
        }
    }
    /*
        private String sayHello(Socket socket) {
            try {
                StreamTransport st = new StreamTransport(socket.getInputStream(), socket.getOutputStream());
                st.writeLine(HelloKitty);
                String recv = st.readLine();
                if (recv.startsWith(HelloKitty)) {
                    return recv.substring(HelloKitty.length());
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    */
    public void startPrint(final TransferListener l) {
        String cmd = "000B|0000|100|/mnt/sdcard/MSG/1/|0|0000|0|0000|0D0A";
        for(ConnectedDevice dev : mDevices) {
            dev.sendCmd(cmd, new TransferListener() {
                @Override
                public void onRecv(ConnectedDevice dev, String recv) {
                    if(null != l) l.onRecv(dev, recv);
                }
            });
        }
    }

    public void sendSettings() {

    }
}
