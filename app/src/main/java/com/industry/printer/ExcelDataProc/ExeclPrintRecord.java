package com.industry.printer.ExcelDataProc;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.industry.printer.R;
import com.industry.printer.Utils.ConfigPath;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.ToastUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ExeclPrintRecord {
    private Context mContext = null;
    private ArrayList<String> mRecords;
    private ListView mRecordsView;
    private BaseAdapter mRecordsLVAdapter = null;

    public ExeclPrintRecord(Context ctx) {
        mContext = ctx;
        mRecords = null;
    }

    private void readRecords() {
        try {
            if (!(new File("/mnt/sdcard/printrecord.txt").exists())) return;
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("/mnt/sdcard/printrecord.txt"), "UTF-8"));
            String line;
            mRecords = new ArrayList<String>();
            while ((line = br.readLine()) != null) {
                mRecords.add(line);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showPrintRecord(final View v) {
        RelativeLayout fl = new RelativeLayout(mContext);

        final PopupWindow popupWindow = new PopupWindow(fl, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#CC000000")));
        popupWindow.setOutsideTouchable(true);
        popupWindow.setTouchable(true);
        popupWindow.update();

        mRecordsView = new ListView(mContext);
        mRecordsView.setBackgroundColor(Color.parseColor("#FFCCCCCC"));
        mRecordsView.setDivider(null);

        readRecords();

        mRecordsLVAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return (null != mRecords ? mRecords.size() : 0);
            }

            @Override
            public Object getItem(int i) {
                return (null != mRecords ? mRecords.get(i) : 0);
            }

            @Override
            public long getItemId(int i) {
                return 0;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if(null == convertView) {
                    convertView = new TextView(mContext);
                }

                convertView.setPadding(10, 2, 10, 2);
                ((TextView)convertView).setText(mRecords.get(position));
                ((TextView)convertView).setTextColor(Color.BLACK);
                ((TextView)convertView).setTextSize(18);

                return convertView;
            }
        };

        mRecordsView.setAdapter(mRecordsLVAdapter);
        fl.addView(mRecordsView);

        ImageView iv = new ImageView(mContext);
        iv.setPadding(10,10,10,10);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(20,20,20,20);
        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        iv.setLayoutParams(lp);
        iv.setImageResource(R.drawable.cancel_icon);
        iv.setBackgroundColor(Color.parseColor("#88FF0000"));
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupWindow.dismiss();
            }
        });
        fl.addView(iv);

        popupWindow.showAtLocation(v, Gravity.NO_GRAVITY, 0, 0);
    }

    public void appendRecord(final String[] data) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("/mnt/sdcard/printrecord.txt", true), "UTF-8"));
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                    sdf.format(new Date());
                    bw.write(sdf.format(new Date()) + ": " + data[0] + "," + data[1] + "," + data[2] + "," + data[3] + "," + data[4] + "\n");
                    bw.close();
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void exportPrintRecord() {
        readRecords();
        if(null == mRecords) return;

        final ArrayList<String> usbs = ConfigPath.getMountedUsb();
        if (usbs.size() <= 0) {
            ToastUtil.show(mContext, R.string.toast_plug_usb);
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(usbs.get(0) + "/printrecord.txt", true), "UTF-8"));
                    for(String s : mRecords) {
                        bw.write(s + "\n");
                    }
                    bw.close();
                    ToastUtil.show(mContext, "Data Export");
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
