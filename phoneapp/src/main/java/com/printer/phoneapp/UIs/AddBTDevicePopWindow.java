package com.printer.phoneapp.UIs;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.printer.phoneapp.Devices.ConnectDevice;
import com.printer.phoneapp.PhoneMainActivity;
import com.printer.phoneapp.R;
import com.printer.phoneapp.Sockets.BLEDriver;
import com.printer.phoneapp.Sockets.BTDriver;
import com.printer.phoneapp.Sockets.NonBLEDriver;

/**
 * Created by hmwan on 2021/9/10.
 */

public class AddBTDevicePopWindow {
    private static final String TAG = AddBTDevicePopWindow.class.getSimpleName();

    private Context mContext = null;
    private PopupWindow mPopupWindow = null;
    private LinearLayout mDevicesList = null;

    private BTDriver mBluetoothManager = null;

    public AddBTDevicePopWindow(Context ctx) {
        mContext = ctx;
        if(!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.d(TAG, "FEATURE_BLUETOOTH_LE not supported");
            mBluetoothManager = NonBLEDriver.getInstance(ctx);
        } else {
            Log.d(TAG, "FEATURE_BLUETOOTH_LE supported");
            mBluetoothManager = BLEDriver.getInstance(ctx);
        }
    }

    private void clearDeviceView() {
        mDevicesList.removeAllViews();
    }

    private void addDeviceView(final BluetoothDevice dev) {
        final LinearLayout linearLayout;

        linearLayout = (LinearLayout) LayoutInflater.from(mContext).inflate(R.layout.phone_add_bt_device_item, null);
        mDevicesList.addView(linearLayout);

        linearLayout.setTag(dev);

        final TextView deviceTV = (TextView) linearLayout.findViewById(R.id.idDevice);
        deviceTV.setText(dev.getName());

        final ImageView selIV = (ImageView) linearLayout.findViewById(R.id.idSelected);
        selIV.setImageBitmap(null);
        linearLayout.setSelected(false);

        linearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!linearLayout.isSelected()) {
                    selIV.setImageResource(R.drawable.check_mark);
                    linearLayout.setBackgroundColor(Color.YELLOW);
                } else {
                    selIV.setImageBitmap(null);
                    linearLayout.setBackgroundColor(Color.TRANSPARENT);
                }
                linearLayout.setSelected(!selIV.isSelected());
            }
        });
    }

    public void show(View v, final PhoneMainActivity.OnDeviceSelectListener l) {
        if(null == mContext) {
            return;
        }

        View popupView = LayoutInflater.from(mContext).inflate(R.layout.phone_add_bt_device, null);
        mPopupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);
        mPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#CC000000")));
        mPopupWindow.setOutsideTouchable(true);
        mPopupWindow.setTouchable(true);
        mPopupWindow.update();

        ImageView closeIV = (ImageView)popupView.findViewById(R.id.Close);
        closeIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPopupWindow.dismiss();
            }
        });

        mDevicesList = (LinearLayout) popupView.findViewById(R.id.idDevicesList);

        final TextView searchTV = (TextView)popupView.findViewById(R.id.idDiscovery);
        if(mBluetoothManager.isEnabled()) {
            searchTV.setEnabled(true);
        } else {
            searchTV.setEnabled(false);
        }

        final TextView connectTV = (TextView)popupView.findViewById(R.id.idConnect);
        if(mDevicesList.getChildCount() > 0) {
            connectTV.setEnabled(true);
        } else {
            connectTV.setEnabled(false);
        }
        final ProgressBar progressBar = (ProgressBar)popupView.findViewById(R.id.idDiscovering);

        searchTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mBluetoothManager.isEnabled()) {
                    Toast.makeText(mContext, "Bluetooth not enabled.", Toast.LENGTH_LONG).show();
                    return;
                }
                clearDeviceView();
                mBluetoothManager.startDiscovery(new BTDriver.OnDiscoveryListener() {
                    @Override
                    public void onDiscoveryStarted() {
                        Log.d(TAG, "Discovery started.");
                        searchTV.post(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        searchTV.setEnabled(false);
                                        connectTV.setEnabled(false);
                                        progressBar.setVisibility(View.VISIBLE);
                                    }
                                }
                        );
                    }

                    @Override
                    public void onDiscoveryFinished() {
                        Log.d(TAG, "Discovery finished.");
                        searchTV.post(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        searchTV.setEnabled(true);
                                        connectTV.setEnabled(true);
                                        progressBar.setVisibility(View.GONE);
                                    }
                                }
                        );
                    }

                    @Override
                    public void onDeviceFound(BluetoothDevice device) {
                        Log.d(TAG, "Device [" + device.getName() + "," + device.getAddress() + "," + device.getType() + "] found.");
                        addDeviceView(device);
                        connectTV.setEnabled(true);
                    }
                });
            }
        });

        connectTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(null != l) {
                    mBluetoothManager.stopDiscovery();
                    for(int i=mDevicesList.getChildCount()-1; i>=0; i--) {
                        View iv = mDevicesList.getChildAt(i);
                        if(iv.isSelected()) {
                            BluetoothDevice dev = (BluetoothDevice)iv.getTag();
                            if(mBluetoothManager instanceof BLEDriver) {
                                l.onSelected(new ConnectDevice(mContext, dev, ConnectDevice.DEVICE_TYPE_BLE));
                            } else {
                                l.onSelected(new ConnectDevice(mContext, dev, ConnectDevice.DEVICE_TYPE_BT));
                            }
                        }
                    }
                }
                mPopupWindow.dismiss();
            }
        });

        mPopupWindow.showAtLocation(v, Gravity.NO_GRAVITY, 0, 0);
    }
}
