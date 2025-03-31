package com.printer.phoneapp.Utils;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;

import com.printer.phoneapp.Devices.ConnectDevice;
import com.printer.phoneapp.Sockets.BTDriver;

public class MySharedPreferences {
    private static final String PREFERENCE_KEY                             = "PrinterObjectPreference";
    private static final String PREF_DEV_ADDR                              = "PrefDeviceAddr";
    private static final String PREF_DEV_NAME                              = "PrefDeviceName";
    private static final String PREF_DEV_TYPE                              = "PrefDeviceType";

    public static void saveDeviceAddr(Context ctx, String dev) {
        try {
            if(null != ctx) {
                SharedPreferences sp = ctx.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE);
                SharedPreferences.Editor prefEditor = sp.edit();
                prefEditor.putString(PREF_DEV_ADDR, dev);
                prefEditor.apply();
            }
        } catch(Exception e) {
        }
    }

    public static String readDeviceAddr(Context ctx) {
        if(null != ctx) {
            SharedPreferences sp = ctx.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE);
            return sp.getString(PREF_DEV_ADDR, "");
        }
        return "";
    }

    public static void saveDeviceName(Context ctx, String name) {
        try {
            if(null != ctx) {
                SharedPreferences sp = ctx.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE);
                SharedPreferences.Editor prefEditor = sp.edit();
                prefEditor.putString(PREF_DEV_NAME, name);
                prefEditor.apply();
            }
        } catch(Exception e) {
        }
    }

    public static String readDeviceName(Context ctx) {
        if(null != ctx) {
            SharedPreferences sp = ctx.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE);
            return sp.getString(PREF_DEV_NAME, "");
        }
        return "";
    }

    public static void saveDeviceType(Context ctx, int type) {
        try {
            if(null != ctx) {
                SharedPreferences sp = ctx.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE);
                SharedPreferences.Editor prefEditor = sp.edit();
                prefEditor.putInt(PREF_DEV_TYPE, type);
                prefEditor.apply();
            }
        } catch(Exception e) {
        }
    }

    public static int readDeviceType(Context ctx) {
        if(null != ctx) {
            SharedPreferences sp = ctx.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE);
            return sp.getInt(PREF_DEV_TYPE, BluetoothDevice.DEVICE_TYPE_UNKNOWN);
        }
        return BluetoothDevice.DEVICE_TYPE_UNKNOWN;
    }
}
