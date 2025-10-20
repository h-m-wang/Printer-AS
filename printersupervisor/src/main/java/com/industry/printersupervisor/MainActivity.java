package com.industry.printersupervisor;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();

    public static final boolean AVOID_CROSS_UPGRADE = true;			// 禁止交叉升级
//	public static final boolean AVOID_CROSS_UPGRADE = false;		// 自由升级

    private ProgressBar mProgressBar;
    private TextView mPrompt;
    private TextView mUpgrade;

    private static final int LAUNCH_UPGRADE = 8;
    private static final int UPGRADE_SUCCESS = 9;
    private static final int UPGRADE_FAILED = 10;

    public Handler mHander = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case LAUNCH_UPGRADE:
                    mProgressBar.setVisibility(View.VISIBLE);
                    break;
                case UPGRADE_SUCCESS:
                    mPrompt.setText(R.string.str_urge2restart);
                    mProgressBar.setVisibility(View.GONE);
                    mUpgrade.setClickable(false);
                    mUpgrade.setBackgroundColor(Color.GRAY);
                    break;
                case UPGRADE_FAILED:
                    ToastUtil.show(MainActivity.this, "Failed!");
                    mProgressBar.setVisibility(View.GONE);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        Debug.d(TAG, "onCreate");
        mProgressBar = (ProgressBar)findViewById(R.id.progressBar1);
        mPrompt = (TextView)findViewById(R.id.prompt);
        mUpgrade = (TextView)findViewById(R.id.launch_upgrade);
        mUpgrade.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mHander.sendEmptyMessage(LAUNCH_UPGRADE);
                        if(upgrade()) {
                            try {Thread.sleep(15000);} catch(Exception e) {}
                            mHander.sendEmptyMessage(UPGRADE_SUCCESS);
                        } else {
                            mHander.sendEmptyMessage(UPGRADE_FAILED);
                        }
                    }
                }).start();
            }
        });
    }

    private boolean upgrade() {
        boolean ret = false;

        Debug.d(TAG, "upgrade");
        if (PlatformInfo.isSmfyProduct() || PlatformInfo.isA133Product()) {
            Debug.d(TAG, "upgrade2");
            PackageInstaller installer = PackageInstaller.getInstance(this);
            long start = System.currentTimeMillis();
            while(System.currentTimeMillis() - start < 3000) {
                if(ConfigPath.getUpgradePath() != null) {
                    Debug.d(TAG, "Path = [" + ConfigPath.getUpgradePath() + "]");
                    if(AVOID_CROSS_UPGRADE) {
                        ret |= installer.silentUpgrade3();
                    } else {
                        ret |= installer.silentUpgrade();
                    }
                    break;
                } else {
                    Debug.d(TAG, "Path = null. " + (System.currentTimeMillis() - start));
                    ConfigPath.updateMountedUsb();
                    try { Thread.sleep(100);} catch(Exception e) {}
                }
            }
        }
        return ret;
    }
}
