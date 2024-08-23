package com.printer.phoneapp.Utils;

import android.content.Context;
import android.content.SharedPreferences;

public class MySharedPreferences {
    private static final String PREFERENCE_KEY                             = "PrinterObjectPreference";
    private static final String PREF_DEV_ADDR                              = "PrefDeviceAddr";
    private static final String PREF_DEV_NAME                              = "PrefDeviceName";

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
}
