package com.industry.printer.ExcelDataProc;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.industry.printer.R;
import com.industry.printer.Utils.ConfigPath;
import com.industry.printer.Utils.ToastUtil;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.ArrayList;

public class ExeclFile {
    private Context mContext = null;

    public interface ExeclFileSelect {
        public void onFileSelected(String path);
    }
    private ExeclFileSelect mExeclFileSelect;

    public ExeclFile(Context ctx) {
        mContext = ctx;
        mExeclFileSelect = null;
    }

    public void pickupFile(final View v, ExeclFileSelect l) {
        final ArrayList<String> usbs = ConfigPath.getMountedUsb();
        if (usbs.size() <= 0) {
            ToastUtil.show(mContext, R.string.toast_plug_usb);
            return;
        }

        File dir = new File(usbs.get(0));

        final File files[] = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File f) {
                if(f.getName().endsWith(".xls") || f.getName().endsWith(".xlsx")) return true;
                return false;
            }
        });

        mExeclFileSelect = l;

        RelativeLayout fl = new RelativeLayout(mContext);

        final PopupWindow popupWindow = new PopupWindow(fl, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#CC000000")));
        popupWindow.setOutsideTouchable(true);
        popupWindow.setTouchable(true);
        popupWindow.update();
        popupWindow.showAtLocation(v, Gravity.NO_GRAVITY, 0, 0);

        ListView filesView = new ListView(mContext);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        filesView.setLayoutParams(lp);
        filesView.setBackgroundColor(Color.parseColor("#FFCCCCCC"));
        filesView.setDivider(null);

        filesView.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return files.length;
            }

            @Override
            public Object getItem(int i) {
                return files[i];
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
                ((TextView)convertView).setText(files[position].getName());
                ((TextView)convertView).setTextColor(Color.BLACK);
                ((TextView)convertView).setTextSize(32);

                return convertView;
            }
        });

        filesView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                popupWindow.dismiss();
                if(mExeclFileSelect != null) mExeclFileSelect.onFileSelected(files[i].getAbsolutePath());
            }
        });

        fl.addView(filesView);

        ImageView iv = new ImageView(mContext);
        iv.setPadding(10,10,10,10);
        lp = new RelativeLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
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
    }
}
