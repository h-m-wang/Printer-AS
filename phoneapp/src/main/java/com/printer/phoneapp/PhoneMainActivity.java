package com.printer.phoneapp;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.printer.phoneapp.Data.BarcodeDataProc;
import com.printer.phoneapp.Devices.ConnectDevice;
import com.printer.phoneapp.Sockets.BLEDriver;
import com.printer.phoneapp.Sockets.BTDriver;
import com.printer.phoneapp.Sockets.DataXfer;
import com.printer.phoneapp.Sockets.NonBLEDriver;
import com.printer.phoneapp.Sockets.SocketThread;
import com.printer.phoneapp.UIs.AddBTDevicePopWindow;
import com.printer.phoneapp.UIs.AddWifiDevicePopWindow;
import com.printer.phoneapp.UIs.BarcodeScanPopupWindow;
import com.printer.phoneapp.UIs.SendStringCmdPopWindow;
import com.printer.phoneapp.Utils.HTPermission;
import com.printer.phoneapp.Utils.MySharedPreferences;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by hmwan on 2021/9/7.
 */

public class PhoneMainActivity extends AppCompatActivity {
    private static final String TAG = PhoneMainActivity.class.getSimpleName();

    public static final int PERMISSION_REQUEST_STORAGE      = 17101;
    public static final int PERMISSION_REQUEST_CAMERA       = 17102;
    public static final int PERMISSION_REQUEST_BLUETOOTH    = 17103;

//    private LinearLayout mDeviceAddingArea = null;
//    private TextView mAddWIFIDevice0 = null;
//    private TextView mAddBTDevice0 = null;

    private LinearLayout mConnectStatusArea = null;
    private TextView mAddWIFIDevice = null;
    private TextView mAddBTDevice = null;
//    private ScrollView mDeviceScrollView = null;
//    private LinearLayout mDevicesList = null;
    private ImageView mDevIcon = null;
    private TextView mDevName = null;
    private TextView mDevState = null;
//    private TextView mCmdSent = null;
    private TextView mDataRecvd = null;
    private TextView mScanResult = null;

    private LinearLayout mCommandArea = null;

    private TextView mSendStringCmd = null;
    private TextView mScanBarcodeCmd = null;

    private BTDriver mBluetoothManager;
    private ConnectDevice mConDevice = null;

    ExecutorService mCachedThreadPool = null;
    private int mConnectingStatus;
    private final static int CON_STATUS_DISCONNECTED = 0;
    private final static int CON_STATUS_CONNECTING = 1;
    private final static int CON_STATUS_CONNECTED = 2;

    public interface OnDeviceSelectListener {
        public void onSelected(ConnectDevice dev);
    }

    private final static int MSG_DISP_SCANRESULT         = 101;
    private final static int MSG_CONNECT_DEVICE          = 102;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_DISP_SCANRESULT:
                    if(msg.arg1 == 1) {
                        mScanResult.setTextColor(Color.BLACK);
                    } else {
                        mScanResult.setTextColor(Color.RED);
                    }
                    mScanResult.setText((String)msg.obj);
                    mScanResult.setVisibility(View.VISIBLE);
                    break;
                case MSG_CONNECT_DEVICE:
                    if(mConnectingStatus != CON_STATUS_DISCONNECTED) break;
                    final String address = (String)msg.obj;
                    if(address.isEmpty()) break;
                    final int type = msg.arg1;
                    mDevIcon.setImageResource(R.drawable.bt);
                    mDevIcon.setVisibility(View.VISIBLE);
                    mDevName.setText(MySharedPreferences.readDeviceName(PhoneMainActivity.this));
                    mDevState.setText("正在连接...");
                    mDevState.setTextColor(Color.GRAY);
                    mCachedThreadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (PhoneMainActivity.this) {
                                BluetoothDevice btDevice = mBluetoothManager.getRemoteDevice(address);
                                ConnectDevice dev = new ConnectDevice(PhoneMainActivity.this, btDevice, type);
                                dev.connect(mOnDeviceConnectionListener);
                                mConDevice = dev;
                                Message msg = mHandler.obtainMessage(MSG_CONNECT_DEVICE);
                                msg.arg1 = type;
                                msg.obj = address;
                                mHandler.sendMessageDelayed(msg, 1000);
                            }
                        }
                    });
                    break;
            }
        }
    };

    private void adjustCommandAreaEnability() {
        for(int i=0; i<mCommandArea.getChildCount(); i++) {
            mCommandArea.getChildAt(i).setEnabled(null != mConDevice && mConDevice.isConnected() ? true : false);
        }
    }

    private DataXfer.OnDeviceConnectionListener mOnDeviceConnectionListener = new DataXfer.OnDeviceConnectionListener() {
        @Override
        public void onConnected(BluetoothDevice dev) {
            MySharedPreferences.saveDeviceAddr(PhoneMainActivity.this, (null != dev ? dev.getAddress() : ""));
            MySharedPreferences.saveDeviceName(PhoneMainActivity.this, (null != dev ? dev.getName() : ""));
            mHandler.removeMessages(MSG_CONNECT_DEVICE);
            mDevState.post(new Runnable() {
                @Override
                public void run() {
                    mConnectingStatus = CON_STATUS_CONNECTED;
                    mDevState.setText("已连接");
                    mDevState.setTextColor(Color.BLUE);
                    adjustCommandAreaEnability();
                }
            });
        }

        @Override
        public void onDisConnected() {
            MySharedPreferences.saveDeviceAddr(PhoneMainActivity.this, "");
            MySharedPreferences.saveDeviceName(PhoneMainActivity.this, "");
            mDevState.post(new Runnable() {
                @Override
                public void run() {
                    mConnectingStatus = CON_STATUS_DISCONNECTED;
                    mDevState.setText("断开");
                    mDevState.setTextColor(Color.RED);
                    adjustCommandAreaEnability();
                }
            });
        }
    };

    private View.OnClickListener AddWIFIDeviceButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            AddWifiDevicePopWindow popWindow = new AddWifiDevicePopWindow(PhoneMainActivity.this);
            popWindow.show(mAddWIFIDevice, new OnDeviceSelectListener() {
                @Override
                public void onSelected(final ConnectDevice dev) {
                    mDevIcon.setImageResource(R.drawable.wifi);
                    mDevIcon.setVisibility(View.VISIBLE);
                    mDevName.setText(dev.getName());
                    mDevState.setText("正在连接...");
                    mDevState.setTextColor(Color.GRAY);
                    mCachedThreadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (PhoneMainActivity.this) {
                                mConnectingStatus = CON_STATUS_CONNECTING;
                                ConnectDevice cdev = mConDevice;
                                mConDevice = null;
                                if (null != cdev) cdev.disconnect();
                                dev.connect(mOnDeviceConnectionListener);
                                mConDevice = dev;
                            }
                        }
                    });
                }
            });
        }
    };

    private View.OnClickListener AddBluetoothDeviceButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(checkBluetoothAccessPermission() == 0) {
                launchAddBluetoothDevices();
            }
        }
    };

    private void launchAddBluetoothDevices() {
        AddBTDevicePopWindow popWindow = new AddBTDevicePopWindow(PhoneMainActivity.this, mBluetoothManager);
        popWindow.show(mAddBTDevice, new OnDeviceSelectListener() {
            @Override
            public void onSelected(final ConnectDevice dev) {
                mDevIcon.setImageResource(R.drawable.bt);
                mDevIcon.setVisibility(View.VISIBLE);
                mDevName.setText(dev.getName());
                mDevState.setText("正在连接...");
                mDevState.setTextColor(Color.GRAY);
                mCachedThreadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (PhoneMainActivity.this) {
                            mConnectingStatus = CON_STATUS_CONNECTING;
                            ConnectDevice cdev = mConDevice;
                            mConDevice = null;
                            if (null != cdev) cdev.disconnect();
                            dev.connect(mOnDeviceConnectionListener);
                            mConDevice = dev;
                        }
                    }
                });
            }
        });
    }

    private class DataXferListener implements DataXfer.OnDataXferListener {
        @Override
        public void onSent(final String sent) {
            Message msg = mHandler.obtainMessage(MSG_DISP_SCANRESULT);
            if(null == sent || sent.isEmpty()) {
                msg.arg1 = 0;
                msg.obj = "Data not found.";
            } else {
                msg.arg1 = 1;
                msg.obj = sent;
            }
            mHandler.sendMessage(msg);
        }

        @Override
        public void onSent(final byte[] sent) {
        }

        @Override
        public void onFailed(final String errMsg) {
            Message msg = mHandler.obtainMessage(MSG_DISP_SCANRESULT);
            msg.arg1 = 0;
            msg.obj = errMsg;
            mHandler.sendMessage(msg);
        }

        @Override
        public void onReceived(final String recv) {
            mCommandArea.post(new Runnable() {
                @Override
                public void run() {
                mDataRecvd.setText("Recv: " + recv);
//                Toast.makeText(PhoneMainActivity.this, "Received: " + recv, Toast.LENGTH_LONG).show();
                }
            });
        }

        @Override
        public void onReceived(final byte[] recv) {
            mCommandArea.post(new Runnable() {
                @Override
                public void run() {
                    mDataRecvd.setText("Recv: " + recv);
//                    Toast.makeText(PhoneMainActivity.this, "Received: " + Arrays.toString(recv), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private class ScanBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            final String text = intent.getExtras().getString("code");
            mDataRecvd.setText("");
            mScanResult.setText("");
            mCachedThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    synchronized(PhoneMainActivity.this) {
                        final String cmd = BarcodeDataProc.makeup650CmdString(text);
                        Message msg = mHandler.obtainMessage(MSG_DISP_SCANRESULT);
                        if (null == cmd || cmd.isEmpty()) {
                            msg.arg1 = 0;
                            msg.obj = "Data not found.";
                        } else {
                            msg.arg1 = 1;
                            msg.obj = cmd;
                            if (null != mConDevice)
                                mConDevice.sendString(cmd, new DataXferListener());
                        }
                        mHandler.sendMessage(msg);
                    }
                }
            });
        }
    }

    private ScanBroadcastReceiver scanBroadcastReceiver = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.phone_main_layout);

        mCachedThreadPool = Executors.newCachedThreadPool();
        mConnectingStatus = CON_STATUS_DISCONNECTED;

        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.d(TAG, "FEATURE_BLUETOOTH_LE not supported");
            mBluetoothManager = NonBLEDriver.getInstance(this);
        } else {
            Log.d(TAG, "FEATURE_BLUETOOTH_LE supported");
            mBluetoothManager = BLEDriver.getInstance(this);
        }
        mBluetoothManager.enableBluetooth();

        Message msg = mHandler.obtainMessage(MSG_CONNECT_DEVICE);
        msg.arg1 = mBluetoothManager instanceof BLEDriver ? ConnectDevice.DEVICE_TYPE_BLE : ConnectDevice.DEVICE_TYPE_BT;
        msg.obj = MySharedPreferences.readDeviceAddr(PhoneMainActivity.this);
        mHandler.sendMessageDelayed(msg,1000);

        mConnectStatusArea = (LinearLayout) findViewById(R.id.ConnectStatusArea);
        TextView aaa = (TextView) findViewById(R.id.CmdAAA);
        aaa.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 主要是为了拦截扫码枪的结果
            }
        });
        mAddWIFIDevice = (TextView) findViewById(R.id.CmdAddWIFIDevice);
        mAddWIFIDevice.setOnClickListener(AddWIFIDeviceButtonClickListener);
        mAddBTDevice= (TextView) findViewById(R.id.CmdAddBTDevice);
        mAddBTDevice.setOnClickListener(AddBluetoothDeviceButtonClickListener);

        mDevIcon = (ImageView) findViewById(R.id.DevIcon);
        mDevName = (TextView) findViewById(R.id.DevName);
        mDevState = (TextView) findViewById(R.id.DevState);
        mDevState.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(null != mConDevice) {
                    mDevState.setTextColor(Color.GRAY);
                    mDevState.setText("...");
                    if(mConDevice.isConnected()) {
                        mConDevice.disconnect();
                    } else {
                        mConDevice.connect(mOnDeviceConnectionListener);
                    }
                }
            }
        });

        mScanResult = (TextView) findViewById(R.id.ScanResult);

        mCommandArea = (LinearLayout) findViewById(R.id.CommandsArea);
//        mCmdSent = (TextView) findViewById(R.id.TextSent);
        mDataRecvd = (TextView) findViewById(R.id.TextRecvd);

        mScanBarcodeCmd = (TextView) findViewById(R.id.CmdScanBarcode);
        mScanBarcodeCmd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                mCmdSent.setText("");
                mDataRecvd.setText("");
                launchScanBarcodeProcess();
            }
        });

        mSendStringCmd = (TextView) findViewById(R.id.CmdSendString);
        mSendStringCmd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                mCmdSent.setText("");
                mDataRecvd.setText("");
                SendStringCmdPopWindow popWindow = new SendStringCmdPopWindow(PhoneMainActivity.this, mConDevice, new DataXferListener());
                popWindow.show(mSendStringCmd);
            }
        });

        adjustCommandAreaEnability();

        checkWriteExternalStoragePermission();

        if(scanBroadcastReceiver==null) {
            scanBroadcastReceiver = new ScanBroadcastReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("com.scancode.resault");
            this.registerReceiver(scanBroadcastReceiver, intentFilter);
        }

/*        new Thread(new Runnable() {
            @Override
            public void run() {
                BarcodeDataProc.makeup650CmdString("http://171.221.216.226:7901/SmartFactory/product/pvSegmentInfoByQRCode?id=127988");
            }
        }).start();*/
    }

    private void checkWriteExternalStoragePermission() {
        HTPermission mPermission = new HTPermission(PhoneMainActivity.this, PERMISSION_REQUEST_STORAGE);
        if(!mPermission.isPermissionGuaranteed(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            mPermission.requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    private void launchScanBarcodeProcess() {
        HTPermission mPermission = new HTPermission(PhoneMainActivity.this, PERMISSION_REQUEST_CAMERA);
        if(mPermission.requestPermission(Manifest.permission.CAMERA) == 0) {
            BarcodeScanPopupWindow popWindow = new BarcodeScanPopupWindow(PhoneMainActivity.this, mConDevice, new DataXferListener());
            popWindow.show(mScanBarcodeCmd);
        }
    }

    public int checkBluetoothAccessPermission() {
        String[] needPermissions = new String[] {
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };
        HTPermission mPermission = new HTPermission(PhoneMainActivity.this, PhoneMainActivity.PERMISSION_REQUEST_BLUETOOTH);
        return mPermission.requestPermissions(needPermissions);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult requestCode = " + requestCode);
        int grantResult = PackageManager.PERMISSION_DENIED;
        for(int i=0; i<grantResults.length; i++) {
            Log.d(TAG, permissions[i] +": " + grantResults[i]);
            grantResult = grantResults[i];
            if(grantResult != PackageManager.PERMISSION_GRANTED) {
                break;
            }
        }

        if(grantResult == PackageManager.PERMISSION_GRANTED) {
            switch(requestCode) {
                case PERMISSION_REQUEST_STORAGE:
                    break;
                case PERMISSION_REQUEST_CAMERA:
                    launchScanBarcodeProcess();
                    break;
                case PERMISSION_REQUEST_BLUETOOTH:
                    launchAddBluetoothDevices();
                    break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        if(requestCode == BTDriver.REQUEST_ENBLE_BT){
            Log.d(TAG, "onActivityResult resultCode = " + resultCode);
            mBluetoothManager.setEnablingResult(resultCode);
        }
    }
}
