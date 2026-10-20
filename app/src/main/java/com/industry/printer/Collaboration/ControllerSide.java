package com.industry.printer.Collaboration;

import android.content.Context;
import android.util.Patterns;

import com.industry.printer.ControlTabActivity;
import com.industry.printer.MainActivity;
import com.industry.printer.Utils.ConfigPath;
import com.industry.printer.Utils.Configs;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.LibUpgrade;
import com.industry.printer.Utils.StreamTransport;
import com.industry.printer.Utils.StringUtil;
import com.industry.printer.pccommand.PCCommandManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
    private ExecutorService mCmdExecutor;
    private ExecutorService mFindExecutor;
    private String mSelfIpAddress;

    public final static String ERR_DISCONNECTED = "Connection broken";
    public final static String ERR_TIMEOUT = "Timeout";
    private volatile boolean mCmdOnDoing;        // 当处于命令处理中时，跳过搜寻新设备

    private List<ConnectedDevice> mDevices;

    public interface CmdTransferListener {
        //        public void onSent();
        public void onRecv(ConnectedDevice dev, String recv);
//        public void onError(String err);
    }
    private CmdTransferListener mCmdTransferListner;

    public interface FileTransferListener {
        //        public void onSent();
        public void onRecv(ConnectedDevice dev, String recv);
        public void onProgress(ConnectedDevice dev, float percentage);
//        public void onError(String err);
    }
    private FileTransferListener mFileTransferListner;

    public class ConnectedDevice {
        private String mDevID;
        private String mIPAddress;
        private Socket mSocket;
        private boolean mConnected;
        private volatile boolean mCmdProcessing;         // 当处在命令处理中时，跳过心跳

        public ConnectedDevice(String id, String ip, Socket socket) {
            mDevID = id;
            mIPAddress = ip;
            mSocket = socket;
            mConnected = false;
            mCmdProcessing = false;
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

        public void setConnected(boolean conState) {
            mConnected = conState;
        }

        public boolean isConnected() {
            return mConnected;
        }

        public void sendCmd(final String cmd, final CmdTransferListener l) {
            synchronized (this) {
                try {
                    StreamTransport st = new StreamTransport(mSocket.getInputStream(), mSocket.getOutputStream());
                    st.writeLine(cmd);
                    String recv = st.readLine();
                    if (null == recv) {     // 连接已经断开
                        if (null != l) l.onRecv(ConnectedDevice.this, ERR_DISCONNECTED);
                    } else if(recv.isEmpty()) {   // 超时
                        if (null != l) l.onRecv(ConnectedDevice.this, ERR_TIMEOUT);
                    } else {               // 如果接收到实际内容，则回调返回；如果超时返回""，则继续等待
                        if (null != l) l.onRecv(ConnectedDevice.this, recv);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    if (null != l) l.onRecv(ConnectedDevice.this, e.getMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                    if (null != l) l.onRecv(ConnectedDevice.this, e.getMessage());
                }
            }
        }

        public void sendFile(final String cmd, final String src, final String dst, final FileTransferListener l) {
            synchronized (this) {
                try {
                    FileInputStream fis = new FileInputStream(src);
                    StreamTransport st = new StreamTransport(mSocket.getInputStream(), mSocket.getOutputStream());
                    st.writeLine(String.format(cmd, dst, fis.available()));
                    byte[] data = new byte[8192];
                    int total = fis.available(), sent = 0;
                    while (fis.available() > 0) {
                        int cnt = fis.read(data, 0, Math.min(fis.available(), 8192));
                        if (cnt == -1) break;
                        if (cnt > 0) st.write(data, 0, cnt);
                        sent += cnt;
                        if (null != l) l.onProgress(ConnectedDevice.this, 1.0f * sent / total);
                    }
                    String recv = st.readLine();
                    if (null == recv) {     // 连接已经断开
                        if (null != l) l.onRecv(ConnectedDevice.this, ERR_DISCONNECTED);
                    } else if(recv.isEmpty()) {   // 超时
                        if (null != l) l.onRecv(ConnectedDevice.this, ERR_TIMEOUT);
                    } else {               // 如果接收到实际内容，则回调返回；如果超时返回""，则继续等待
                        if (null != l) l.onRecv(ConnectedDevice.this, recv);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    if (null != l) l.onRecv(ConnectedDevice.this, e.getMessage());
                } catch (IOException e) {
                    e.printStackTrace();
                    if (null != l) l.onRecv(ConnectedDevice.this, e.getMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                    if (null != l) l.onRecv(ConnectedDevice.this, e.getMessage());
                }
            }
        }
    }

    private Timer mTimer;
    private static ControllerSide mInstance = null;
    public static ControllerSide getInstance() {
        return mInstance;
    }

    public ControllerSide(Context ctx, CmdTransferListener l) {
        mInstance = this;
        mContext = ctx;
        mFindExecutor = Executors.newFixedThreadPool(20);
        mCmdExecutor = Executors.newFixedThreadPool(5);
        mDevices = new ArrayList<>();
        mCmdTransferListner = l;
        mSelfIpAddress = null;
        mCmdOnDoing = false;
        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(!mCmdOnDoing && mFindingCount <= 0) findPrinters();
                if(null != mDevices && mDevices.size() > 0) {
                    for(ConnectedDevice dev : mDevices) {
                        if(dev.mCmdProcessing) continue;
                        dev.sendCmd(HelloKitty, new CmdTransferListener() {
                            @Override
                            public void onRecv(ConnectedDevice dev, String recv) {
                                if(ERR_DISCONNECTED.equals(recv)) {
                                    dev.setConnected(false);
                                    if(null != mCmdTransferListner) mCmdTransferListner.onRecv(dev, recv);
                                }
                                if(ERR_TIMEOUT.equals(recv))
                                    if(null != mCmdTransferListner) mCmdTransferListner.onRecv(dev, recv);
                            }
                        });
                    }
                }
            }
        }, 3000L, 3000L);
        Debug.i(TAG, "Start as Client");
    }

    private volatile int mFindingCount = 0;      // 记录搜索完成数量，当==0时表示搜索完成，未完成之前不再启用新的搜索

    private void findPrinters() {
        if(null == mSelfIpAddress) {
            mSelfIpAddress = MainActivity.getLocalIpAddress();
            Debug.d(TAG, "本机IP: " + mSelfIpAddress);
            if(null == mSelfIpAddress) return;
            if(!Patterns.IP_ADDRESS.matcher(mSelfIpAddress).matches()) return;
        }

        String subnet = mSelfIpAddress.substring(0, mSelfIpAddress.lastIndexOf('.'));
        Debug.d(TAG, "扫描网段: " + subnet + ".1~254");

        mFindingCount = 254;
        for (int i=0; i<254; i++) {
            final String targetIp = subnet + "." + (i+1);
            mFindExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (!targetIp.equals(mSelfIpAddress)) {        // 跳过本机及非本服务子机
                            InetAddress addr = InetAddress.getByName(targetIp);
                            if (addr.isReachable(1000)) {
                                Debug.d(TAG, "发现设备: " + targetIp);
                                handShakeWith(targetIp);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        synchronized (mTimer) {
                            mFindingCount--;
                        }
                    }
                }
            });
        }
        Debug.d(TAG, "Done. ");
    }

    private final String HelloKitty = "Hello Kitty";

    public void handShakeWith(final String server) {
        try {
            ConnectedDevice existedDevice = null;
            for(ConnectedDevice d : mDevices) {
                if(d.mIPAddress.equals(server)) {
                    if(!d.isConnected()) {
                        existedDevice = d;
                        break;
                    } else {
                        return;
                    }
                }
            }

            Socket socket = new Socket();
            socket.setSoTimeout(15*000);

            Debug.d(TAG, "Connect to: [" + server + "]");
            socket.connect(new InetSocketAddress(server, PORT),1000);

            // 对于已经存在的设备，设置新的socket，对于不存在的设备，生成新的设备
            final boolean isNewDevice = (null == existedDevice);
            if(null == existedDevice) {
                existedDevice = new ConnectedDevice("", server, socket);
            } else {
                existedDevice.setSocket(socket);
            }

            final ConnectedDevice device = existedDevice;
            device.sendCmd(HelloKitty, new CmdTransferListener() {
                @Override
                public void onRecv(ConnectedDevice dev, String recv) {
                    if (null != recv && recv.startsWith(HelloKitty)) {
                        device.setConnected(true);
                        if(isNewDevice) {
                            mDevices.add(device);
                            Debug.d(TAG, "Device: [" + server + "] added.");
                        } else {
                            Debug.d(TAG, "Device: [" + server + "] reconnected.");
                        }
                    }
                }
            });
        } catch (IOException e) {
            Debug.d(TAG, "Connect failed to: [" + server + "]");
            e.printStackTrace();
        } catch (Exception e) {
            Debug.d(TAG, "Connect failed to: [" + server + "]");
            e.printStackTrace();
        }
    }

    public void startPrint(final CmdTransferListener l) {
        for(final ConnectedDevice dev : mDevices) {
            dev.mCmdProcessing = true;
            mCmdOnDoing = true;
            mCmdExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    String cmd = "000B|0000|100|/mnt/sdcard/MSG/1/|0|0000|0|0000|0D0A";
                    dev.sendCmd(cmd, new CmdTransferListener() {
                        @Override
                        public void onRecv(ConnectedDevice dev, String recv) {
                            dev.mCmdProcessing = false;
                            mCmdOnDoing = false;
                            Debug.d(TAG, recv);
                            if (null != l) l.onRecv(dev, recv);
                        }
                    });
                }
            });
        }
    }

    public void sendSettings(final FileTransferListener l) {
        Debug.d(TAG, "Enter sendSettings");
        for(int i=0; i<mDevices.size(); i++) {
            final ConnectedDevice dev = mDevices.get(i);
            dev.mCmdProcessing = true;
            mCmdOnDoing = true;
            final String src = Configs.CONFIG_PATH_FLASH + Configs.SYSTEM_CONFIG_DIR + "/system_config." + (i+1) + ".xml";
            mCmdExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    String cmd = "000B|0000|300|%s|%d|0000|0|0000|0D0A";
//                    dev.sendFile(cmd, src, "/mnt/sdcard/system/system_config.xml", new FileTransferListener() {
                    dev.sendFile(cmd, src, "/mnt/sdcard/system/system_config_test.xml", new FileTransferListener() {
                        @Override
                        public void onRecv(ConnectedDevice dev, String recv) {
                            dev.mCmdProcessing = false;
                            mCmdOnDoing = false;
                            Debug.d(TAG, recv);
                            if (null != l) l.onRecv(dev, recv);
                        }

                        @Override
                        public void onProgress(ConnectedDevice dev, float percentage) {
//                            Debug.d(TAG, "Progress: " + String.format("%.2f%%", percentage * 100.0f));
                            if (null != l) l.onProgress(dev, percentage);
                        }
                    });
                }
            });
        }
    }

    public void upgradeAPK(final FileTransferListener l) {
        Debug.d(TAG, "Enter upgradeAPK");
        for(int i=0; i<mDevices.size(); i++) {
            final ConnectedDevice dev = mDevices.get(i);
            dev.mCmdProcessing = true;
            mCmdOnDoing = true;
            mCmdExecutor.execute(new Runnable() {
                @Override
                public void run() {
//                    if(ConfigPath.getUpgradePath() == null) return;
//                    String src = ConfigPath.getUpgradePath() + Configs.UPGRADE_APK_FILE;
                    String src = "/sdcard/Printer.apk";
                    String cmd = "000C|0000|UpgradeAPK|%s|%d|0000|0|0000|0D0A";
//                    dev.sendFile(cmd, src, "/data/audio_d/Printer.apk", new FileTransferListener() {
                    dev.sendFile(cmd, src, "/sdcard/PrinterTest.apk", new FileTransferListener() {
                        @Override
                        public void onRecv(ConnectedDevice dev, String recv) {
                            dev.mCmdProcessing = false;
                            mCmdOnDoing = false;
                            Debug.d(TAG, recv);
                            if(null != l) l.onRecv(dev, recv);
                        }
                        @Override
                        public void onProgress(ConnectedDevice dev, float percentage) {
//                            Debug.d(TAG, "Progress: " + String.format("%.2f%%", percentage * 100.0f));
                            if(null != l) l.onProgress(dev, percentage);
                        }
                    });
                }
            });
        }
    }
}
