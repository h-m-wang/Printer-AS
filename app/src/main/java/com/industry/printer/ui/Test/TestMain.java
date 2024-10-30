package com.industry.printer.ui.Test;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Message;
import android.text.TextUtils;
import android.util.Printer;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.industry.printer.BLE.BLEDevice;
import com.industry.printer.MessageTask;
import com.industry.printer.R;
import com.industry.printer.Rfid.N_RFIDSerialPort;
import com.industry.printer.Rfid.RFIDData;
import com.industry.printer.Serial.SerialPort;
import com.industry.printer.Utils.ByteArrayUtils;
import com.industry.printer.Utils.ConfigPath;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.PlatformInfo;
import com.industry.printer.Utils.StreamTransport;
import com.industry.printer.Utils.ToastUtil;
import com.industry.printer.hardware.ExtGpio;
import com.industry.printer.hardware.FpgaGpioOperation;
import com.industry.printer.hardware.IInkDevice;
import com.industry.printer.hardware.InkManagerFactory;
import com.industry.printer.hardware.N_RFIDDevice;
import com.industry.printer.hardware.N_RFIDManager;
import com.industry.printer.hardware.RFIDDevice;
import com.industry.printer.hardware.RFIDManager;
import com.industry.printer.hardware.SmartCard;
import com.industry.printer.hardware.SmartCardManager;

import java.io.File;
import java.util.List;

public class TestMain {
    public static final String TAG = TestMain.class.getSimpleName();

    private Context mContext = null;
    private PopupWindow mPopupWindow = null;
//    private TextView mQuitBtnTV = null;
    private TextView mTitleTV = null;
    private FrameLayout mClientAreaFL = null;
    private ListView mMainMenuLV = null;
    private ITestOperation mIFTestOp = null;

    private final String TITLE = "";
// H.M.Wang 2023-10-8 临时添加一个保存1000次的强度试验，暂时放在这里，待以后再次确定
//    private final String[] MAIN_TEST_ITEMS = {"墨袋机", "连供", "AL大字机"};
    private final int[] MAIN_TEST_ITEMS = {
        R.string.str_test_main_printer_with_bag,
        R.string.str_test_main_bulk_printer,
        R.string.str_test_main_al_printer,
        R.string.str_test_main_save_100,
        R.string.str_test_main_ble_module_on,
        R.string.str_test_main_m9_test,
        R.string.str_m9_test_rfid,
// H.M.Wang 2024-10-14 追加一个PHO-ENC Test的开始命令和读取测试结果的命令
        R.string.str_m9_test_phoenc,
// End of H.M.Wang 2024-10-14 追加一个PHO-ENC Test的开始命令和读取测试结果的命令
// H.M.Wang 2024-10-28 增加9555A的读写试验，速录在100k和200k，每次读写500次，读写结果输出log。切换速录需要切换img
        R.string.str_m9_test_9555A,
// End of H.M.Wang 2024-10-28 增加9555A的读写试验，速录在100k和200k，每次读写500次，读写结果输出log。切换速录需要切换img
};
// H.M.Wang 2023-10-8 临时添加一个保存1000次的强度试验，暂时放在这里，待以后再次确定

    private boolean mQuit = false;

    public TestMain(Context ctx) {
        mContext = ctx;
        mIFTestOp = null;
        mQuit = false;
    }

    private byte[] rMMM;

    public void show(final View v) {
        if (null == mContext) {
            return;
        }

        View popupView = LayoutInflater.from(mContext).inflate(R.layout.test_main, null);

        mPopupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);
        mPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#CC000000")));
        mPopupWindow.setOutsideTouchable(true);
        mPopupWindow.setTouchable(true);
        mPopupWindow.update();

        TextView quitTV = (TextView)popupView.findViewById(R.id.btn_quit);
        quitTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(null != mIFTestOp) {
                    if (mIFTestOp.quit()) {
                        mMainMenuLV.setVisibility(View.VISIBLE);
                        mTitleTV.setText(TITLE);
                        mIFTestOp = null;
                    }
// H.M.Wang 2024-10-14 追加一个PHO-ENC Test的开始命令和读取测试结果的命令
                } else if(null != mPhoEncTestDlg) {
                    FpgaGpioOperation.stopPhoEncTest();
                    mPhoEncTesting = false;
                    mPhoEncTestDlg.dismiss();
                    mPhoEncTestDlg = null;
// End of H.M.Wang 2024-10-14 追加一个PHO-ENC Test的开始命令和读取测试结果的命令
                } else {
                    mPopupWindow.dismiss();
                    mQuit = true;
                }
            }
        });

        mTitleTV = (TextView)popupView.findViewById(R.id.test_title);
        mTitleTV.setText(TITLE);

        mClientAreaFL = (FrameLayout) popupView.findViewById(R.id.client_area);

        mMainMenuLV = (ListView) popupView.findViewById(R.id.test_main_menu);
        mMainMenuLV.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return MAIN_TEST_ITEMS.length;
            }

            @Override
            public Object getItem(int i) {
                return MAIN_TEST_ITEMS[i];
            }

            @Override
            public long getItemId(int i) {
                return 0;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if(null == convertView) {
//                    convertView = ((LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.hp22mm_test_item, null);
                    convertView = LayoutInflater.from(mContext).inflate(R.layout.test_menu_item, null);
                }

                TextView itemTV = (TextView) convertView.findViewById(R.id.test_main_item);
                if(position < MAIN_TEST_ITEMS.length) itemTV.setText(MAIN_TEST_ITEMS[position]);
                else itemTV.setText("");
                return convertView;
            }
        });

        mMainMenuLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
// H.M.Wang 2023-10-8 临时添加一个保存1000次的强度试验，暂时放在这里，待以后再次确定
                if(i == 3) {
                    final MessageTask mTask = new MessageTask(mContext, ConfigPath.getTlkPath() + File.separator + "Test001");
                    if(new File(ConfigPath.getTlkPath() + File.separator + "Test001").exists()) {
                        mTask.setName("Test001_");
                        mSaving = false;
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try{
                                    for(int i=0; i<SAVE_COUNT_LIMIT; i++) {
                                        if(mQuit) break;
                                        final int pos = i+1;
                                        mSaving = true;
                                        mTask.save(new MessageTask.SaveProgressListener() {
                                            @Override
                                            public void onSaved() {
                                                mSaving = false;
                                                mMainMenuLV.post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        ToastUtil.show(mContext, "Saved: " + pos);
                                                    }
                                                });
                                            }
                                        });
                                        while(mSaving) {
                                            Thread.sleep(10);
                                        }
                                    }
                                } catch(Exception e) {
                                    Debug.e(TAG, e.getMessage());
                                }
                            }
                        }).start();
                    }
                    return;
                }
// End of H.M.Wang 2023-10-8 临时添加一个保存1000次的强度试验，暂时放在这里，待以后再次确定
// H.M.Wang 2024-1-17 临时增加一个蓝牙模块测试功能（后续再优化）
                else if(i == 4) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            BLEDevice ble = BLEDevice.getInstance();
//for(int i=0; i<100; i++) {
//    if(mQuit) break;
    mMainMenuLV.post(new Runnable() {
        @Override
        public void run() {
            ToastUtil.show(mContext, "Starting BLE ...!");
        }
    });
    if (ble.initServer()) {
        mMainMenuLV.post(new Runnable() {
            @Override
            public void run() {
                ToastUtil.show(mContext, "Succeeded!");
            }
        });
    } else {
        mMainMenuLV.post(new Runnable() {
            @Override
            public void run() {
                ToastUtil.show(mContext, "Failed!");
            }
        });
    }
    try{Thread.sleep(1000);}catch(Exception e){}
//}
                        }
                    }).start();
                    return;
                }
// End of H.M.Wang 2024-1-17 临时增加一个蓝牙模块测试功能（后续再优化）
                else if(i == 5) {
                    mIFTestOp = new TestGpioPinsNew(mContext, 0);
                    mIFTestOp.show(mClientAreaFL);
                    mIFTestOp.setTitle(mTitleTV);
                    mMainMenuLV.setVisibility(View.GONE);
                    return;
                }
// H.M.Wang 2024-7-10 追加一个RFID读写测试，重复100次
                else if(i == 6) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            IInkDevice manager = InkManagerFactory.inkManager(mContext);
                            if (!(manager instanceof RFIDManager)) {
                                mMainMenuLV.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        ToastUtil.show(mContext, "Not RFID Device!");
                                    }
                                });
                                return;
                            }
                            while(BLEDevice.BLERequiring) {
                                try{Thread.sleep(100);}catch(Exception e){}
                            }

                            synchronized (RFIDDevice.SERIAL_LOCK) { // 2024-1-29添加
                                BLEDevice.BLERequiring = true;
                                ExtGpio.writeGpioTestPin('I', 9, 0);
                                IInkDevice rfidManager = InkManagerFactory.inkManager(mContext);
                                if (rfidManager instanceof RFIDManager) {
                                    for (int i = 0; i < 100; i++) {
                                        if (mQuit) break;
                                        mMainMenuLV.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                ToastUtil.show(mContext, "RFID Access Test ...!");
                                            }
                                        });
                                        List<RFIDDevice> devices = ((RFIDManager)manager).mRfidDevices;
                                        for(int j=0; j<devices.size(); j++) {
                                            final int devNo = j;
                                            byte[] key = devices.get(j).mRFIDKeyA;
                                            RFIDData[] data = new RFIDData[] {
                                                    new RFIDData(RFIDDevice.RFID_CMD_SEARCHCARD, RFIDDevice.RFID_DATA_SEARCHCARD_ALL),
                                                    new RFIDData(RFIDDevice.RFID_CMD_AUTO_SEARCH, RFIDDevice.RFID_DATA_SEARCH_MODE),
                                                    new RFIDData(RFIDDevice.RFID_CMD_READ_VERIFY, new byte[]{0x00, (byte)(RFIDDevice.SECTOR_INK_MAX*4+RFIDDevice.BLOCK_INK_MAX), key[0], key[1], key[2], key[3], key[4], key[5] }),
                                                    new RFIDData(RFIDDevice.RFID_CMD_READ_VERIFY, new byte[]{0x00, (byte)(RFIDDevice.SECTOR_FEATURE*4+RFIDDevice.BLOCK_FEATURE), key[0], key[1], key[2], key[3], key[4], key[5] }),
                                                    new RFIDData(RFIDDevice.RFID_CMD_READ_VERIFY, new byte[]{0x00, (byte)(RFIDDevice.SECTOR_INKLEVEL*4+RFIDDevice.BLOCK_INKLEVEL), key[0], key[1], key[2], key[3], key[4], key[5] }),
                                                    new RFIDData(RFIDDevice.RFID_CMD_READ_VERIFY, new byte[]{0x00, (byte)(RFIDDevice.SECTOR_COPY_INKLEVEL*4+RFIDDevice.BLOCK_COPY_INKLEVEL), key[0], key[1], key[2], key[3], key[4], key[5] }),
                                            };
                                            ExtGpio.rfidSwitch(j);
                                            try {
                                                Thread.sleep(100);
                                            } catch (Exception e) {
                                            }
                                            byte[] readin = null;
                                            for(int k=0; k<data.length; k++) {
                                                Debug.print(RFIDDevice.RFID_DATA_SEND, data[k].mTransData);
                                                RFIDDevice.write(RFIDDevice.mFd, data[k].mTransData, data[k].mTransData.length);
                                                readin = RFIDDevice.read(RFIDDevice.mFd, 64);
                                                Debug.print(RFIDDevice.RFID_DATA_RECV, readin);
                                                if(null == readin) break;
                                            }

                                            if (null != readin) {
                                                mMainMenuLV.post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        ToastUtil.show(mContext, "Rfid[" + devNo + "] Success!");
                                                    }
                                                });
                                            } else {
                                                mMainMenuLV.post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        ToastUtil.show(mContext, "Rfid[" + devNo + "] Failed!");
                                                    }
                                                });
                                            }
                                            try {Thread.sleep(1000);} catch (Exception e) {}
                                        }
                                    }
                                } else if(rfidManager instanceof N_RFIDManager) {
                                    for (int i = 0; i < 100; i++) {
                                        if (mQuit) break;
                                        mMainMenuLV.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                ToastUtil.show(mContext, "RFID Access Test ...!");
                                            }
                                        });
                                        List<N_RFIDDevice> devices = ((N_RFIDManager)manager).mRfidDevices;
                                        for(int j=0; j<devices.size(); j++) {
                                            final int devNo = j;
                                            ExtGpio.rfidSwitch(j);
                                            if (devices.get(i).init()) {
                                                mMainMenuLV.post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        ToastUtil.show(mContext, "Rfid[" + devNo + "] Success!");
                                                    }
                                                });
                                            } else {
                                                mMainMenuLV.post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        ToastUtil.show(mContext, "Rfid[" + devNo + "] Failed!");
                                                    }
                                                });
                                            }
                                            try {Thread.sleep(1000);} catch (Exception e) {}
                                        }
                                    }
                                } else {
                                    mMainMenuLV.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            ToastUtil.show(mContext, "Not supported RFID device!");
                                        }
                                    });
                                }
                                BLEDevice.BLERequiring = false;
                                ExtGpio.writeGpioTestPin('I', 9, 1);
                            }
                        }
                    }).start();
                    return;
                }
// End of H.M.Wang 2024-7-10 追加一个RFID读写测试，重复100次
// H.M.Wang 2024-10-14 追加一个PHO-ENC Test的开始命令和读取测试结果的命令
                else if(i == 7) {
                    execPhoEncTest();
                    return;
                }
// End of H.M.Wang 2024-10-14 追加一个PHO-ENC Test的开始命令和读取测试结果的命令
// H.M.Wang 2024-10-28 增加9555A的读写试验，速录在100k和200k，每次读写500次，读写结果输出log。切换速录需要切换img
                else if(i == 8) {
                    SmartCard.read9555ATest();
                    return;
                }
// End of H.M.Wang 2024-10-28 增加9555A的读写试验，速录在100k和200k，每次读写500次，读写结果输出log。切换速录需要切换img
                mIFTestOp = new TestSub(mContext, i);
                mIFTestOp.show(mClientAreaFL);
                mIFTestOp.setTitle(mTitleTV);
                mMainMenuLV.setVisibility(View.GONE);
            }
        });

        mPopupWindow.showAtLocation(v, Gravity.NO_GRAVITY, 0, 0);
    }

// H.M.Wang 2023-10-8 临时添加一个保存1000次的强度试验，暂时放在这里，待以后再次确定
    private int SAVE_COUNT_LIMIT = 1000;
    private boolean mSaving;
// End of H.M.Wang 2023-10-8 临时添加一个保存1000次的强度试验，暂时放在这里，待以后再次确定
// H.M.Wang 2024-10-14 追加一个PHO-ENC Test的开始命令和读取测试结果的命令
    private AlertDialog mPhoEncTestDlg = null;
    private boolean mPhoEncTesting = false;

    private void execPhoEncTest() {
        int res = FpgaGpioOperation.startPhoEncTest();
        if(res == 1) {      // Succeeded.
            mPhoEncTestDlg = new AlertDialog.Builder(mContext).setTitle("Pho-Enc Test")
                    .setMessage("")
                    .setNegativeButton("Stop", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            FpgaGpioOperation.stopPhoEncTest();
                            mPhoEncTesting = false;
                            dialog.dismiss();
                            mPhoEncTestDlg = null;
                        }
                    })
                    .create();
            mPhoEncTestDlg.show();
            mPhoEncTesting = true;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    while(mPhoEncTesting) {
                        mMainMenuLV.post(new Runnable() {
                            @Override
                            public void run() {
                                final int value = FpgaGpioOperation.readPhoEncTest();
                                Debug.d(TAG, "PHO-ENC = " + Integer.toHexString(value));
                                mMainMenuLV.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mPhoEncTestDlg.setMessage("Pho: " + ((value >> 16) & 0x0000FFFF) + "; Enc: " + (value & 0x0000FFFF));
                                    }
                                });
                            }
                        });
                        try{Thread.sleep(100);}catch(Exception e){}
                    }
                }
            }).start();
        } else if(res == -1) {
            ToastUtil.show(mContext, R.string.str_state_printing);
        } else {
            ToastUtil.show(mContext, "Not supported");
        }
    }
// End of H.M.Wang 2024-10-14 追加一个PHO-ENC Test的开始命令和读取测试结果的命令
}
