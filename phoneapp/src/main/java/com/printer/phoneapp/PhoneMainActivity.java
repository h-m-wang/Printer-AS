package com.printer.phoneapp;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.printer.phoneapp.Devices.ConnectDevice;
import com.printer.phoneapp.Sockets.BTDriver;
import com.printer.phoneapp.Sockets.DataXfer;
import com.printer.phoneapp.Sockets.SocketThread;
import com.printer.phoneapp.UIs.AddBTDevicePopWindow;
import com.printer.phoneapp.UIs.AddWifiDevicePopWindow;
import com.printer.phoneapp.UIs.BarcodeScanPopupWindow;
import com.printer.phoneapp.UIs.SendStringCmdPopWindow;
import com.printer.phoneapp.Utils.HTPermission;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;

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
    private TextView mCmdSent = null;
    private TextView mDataRecvd = null;

    private LinearLayout mCommandArea = null;

    private TextView mSendStringCmd = null;
    private TextView mScanBarcodeCmd = null;

    private BTDriver mBluetoothManager = null;
    private ConnectDevice mConDevice = null;
    public interface OnDeviceSelectListener {
        public void onSelected(ConnectDevice dev);
    }

    private void adjustCommandAreaEnability() {
        for(int i=0; i<mCommandArea.getChildCount(); i++) {
            mCommandArea.getChildAt(i).setEnabled(null != mConDevice && mConDevice.isConnected() ? true : false);
        }
    }

    private DataXfer.OnDeviceConnectionListener mOnDeviceConnectionListener = new DataXfer.OnDeviceConnectionListener() {
        @Override
        public void onConnected() {
            mDevState.post(new Runnable() {
                @Override
                public void run() {
                    mDevState.setText("Connected");
                    mDevState.setTextColor(Color.BLUE);
                    adjustCommandAreaEnability();
                }
            });
        }

        @Override
        public void onDisConnected() {
            mDevState.post(new Runnable() {
                @Override
                public void run() {
                    mDevState.setText("Closed");
                    mDevState.setTextColor(Color.RED);
                    adjustCommandAreaEnability();
                    if(null != mConDevice) mConDevice.connect(mOnDeviceConnectionListener);
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
                public void onSelected(ConnectDevice dev) {
                    if(null != mConDevice) mConDevice.disconnect();
                    mDevIcon.setImageResource(R.drawable.wifi);
                    mDevIcon.setVisibility(View.VISIBLE);
                    mDevName.setText(dev.getName());
                    mDevState.setText("Connecting...");
                    mDevState.setTextColor(Color.GRAY);
                    dev.connect(mOnDeviceConnectionListener);
                    mConDevice = dev;
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
        AddBTDevicePopWindow popWindow = new AddBTDevicePopWindow(PhoneMainActivity.this);
        popWindow.show(mAddBTDevice, new OnDeviceSelectListener() {
            @Override
            public void onSelected(ConnectDevice dev) {
                ConnectDevice cdev = mConDevice;
                mConDevice = null;
                if(null != cdev) cdev.disconnect();
                mDevIcon.setImageResource(R.drawable.bt);
                mDevIcon.setVisibility(View.VISIBLE);
                mDevName.setText(dev.getName());
                mDevState.setText("Connecting...");
                mDevState.setTextColor(Color.GRAY);
                dev.connect(mOnDeviceConnectionListener);
                mConDevice = dev;
            }
        });
    }

    private class DataXferListener implements DataXfer.OnDataXferListener {
        @Override
        public void onSent(final String sent) {
            mCommandArea.post(new Runnable() {
                @Override
                public void run() {
                    mCmdSent.setText("Sent: " + sent);
//                Toast.makeText(PhoneMainActivity.this, "Sent: " + sent, Toast.LENGTH_LONG).show();
                }
            });
        }

        @Override
        public void onSent(final byte[] sent) {
            mCommandArea.post(new Runnable() {
                @Override
                public void run() {
                    mCmdSent.setText("Sent: " + sent);
//                    Toast.makeText(PhoneMainActivity.this, "Sent: " + Arrays.toString(sent), Toast.LENGTH_LONG).show();
                }
            });
        }

        @Override
        public void onFailed(final String errMsg) {
            mCommandArea.post(new Runnable() {
                @Override
                public void run() {
                    mCmdSent.setText("Failed: " + errMsg);
//                Toast.makeText(PhoneMainActivity.this, "Failed: " + errMsg , Toast.LENGTH_LONG).show();
                }
            });
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
            String text = intent.getExtras().getString("code");
            Log.i(TAG, "ScanBroadcastReceiver code:" + text);
            Toast.makeText(PhoneMainActivity.this, "ScanBroadcastReceiver code:" + text, Toast.LENGTH_LONG).show();
        }
    }

    private ScanBroadcastReceiver scanBroadcastReceiver = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.phone_main_layout);

        mBluetoothManager = new BTDriver(this);
        mBluetoothManager.enableBluetooth();

        mConnectStatusArea = (LinearLayout) findViewById(R.id.ConnectStatusArea);
        mAddWIFIDevice = (TextView) findViewById(R.id.CmdAddWIFIDevice);
        mAddWIFIDevice.setOnClickListener(AddWIFIDeviceButtonClickListener);
        mAddBTDevice= (TextView) findViewById(R.id.CmdAddBTDevice);
        mAddBTDevice.setOnClickListener(AddBluetoothDeviceButtonClickListener);

        mDevIcon = (ImageView) findViewById(R.id.DevIcon);
        mDevName = (TextView) findViewById(R.id.DevName);
        mDevState = (TextView) findViewById(R.id.DevState);

        mCommandArea = (LinearLayout) findViewById(R.id.CommandsArea);
        mCmdSent = (TextView) findViewById(R.id.TextSent);
        mDataRecvd = (TextView) findViewById(R.id.TextRecvd);

        mScanBarcodeCmd = (TextView) findViewById(R.id.CmdScanBarcode);
        mScanBarcodeCmd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCmdSent.setText("");
                mDataRecvd.setText("");
                launchScanBarcodeProcess();
            }
        });

        mSendStringCmd = (TextView) findViewById(R.id.CmdSendString);
        mSendStringCmd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCmdSent.setText("");
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
