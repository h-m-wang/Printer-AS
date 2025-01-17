package com.printer.phoneapp;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private final int MSG_LAUNCH_PHONE_ACTIVITY = 102;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_LAUNCH_PHONE_ACTIVITY:
                    Intent htncIntent = new Intent(getBaseContext(), PhoneMainActivity.class);
                    startActivity(htncIntent);
                    finish();
                    break;
                default:
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView verTV = (TextView)findViewById(R.id.verName);
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            verTV.setText(info.versionName);
        } catch(PackageManager.NameNotFoundException e) {
        }

        mHandler.sendEmptyMessageDelayed(MSG_LAUNCH_PHONE_ACTIVITY, 3000);

    }
}
