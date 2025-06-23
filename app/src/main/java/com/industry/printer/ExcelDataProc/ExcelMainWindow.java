package com.industry.printer.ExcelDataProc;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.industry.printer.R;

public class ExcelMainWindow {
    public static final String TAG = ExcelMainWindow.class.getSimpleName();

    private Context mContext = null;
    private PopupWindow mPopupWindow = null;

    private ListView mExcelDataLV = null;

    private TextView mImportExcel = null;
    private TextView mClearData = null;
    private TextView mDeleteData = null;
    private TextView mSaveData = null;
    private TextView mConfirm = null;
    private TextView mPrintRecord = null;
    private TextView mExportRecord = null;
    private TextView mSelectTemplete = null;
    private TextView mSearch = null;

    private EditText mSearchCnt = null;
    private EditText mLine1 = null;
    private EditText mLine2 = null;
    private EditText mLine3 = null;
    private EditText mLine4 = null;
    private ImageView mLogoView = null;

    public ExcelMainWindow(Context ctx) {
        mContext = ctx;
    }

    public void show(final View v) {
        if (null == mContext) {
            return;
        }

        View popupView = LayoutInflater.from(mContext).inflate(R.layout.excel_data_layout, null);

        mPopupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);
        mPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#CC000000")));
        mPopupWindow.setOutsideTouchable(true);
        mPopupWindow.setTouchable(true);
        mPopupWindow.update();

        mExcelDataLV = (ListView) popupView.findViewById(R.id.xl_data_list);
        mExcelDataLV.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return 30;
            }

            @Override
            public Object getItem(int i) {
                return i;
            }

            @Override
            public long getItemId(int i) {
                return 0;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if(null == convertView) {
                    convertView = LayoutInflater.from(mContext).inflate(R.layout.excel_data_list_item, null);
                }

                TextView searchIDTV = (TextView) convertView.findViewById(R.id.search_id);
                searchIDTV.setText("SerID." + position);
                TextView line1 = (TextView) convertView.findViewById(R.id.line_1);
                line1.setText("Line " + position + ".1");
                TextView line2 = (TextView) convertView.findViewById(R.id.line_2);
                line2.setText("Line " + position + ".2");
                TextView line3 = (TextView) convertView.findViewById(R.id.line_3);
                line3.setText("Line " + position + ".3");
                TextView line4 = (TextView) convertView.findViewById(R.id.line_4);
                line4.setText("Line " + position + ".4");
                return convertView;
            }
        });

        mImportExcel = (TextView) popupView.findViewById(R.id.btn_import_file);
        mImportExcel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(mContext, "mImportExcel", Toast.LENGTH_LONG).show();
            }
        });
        mClearData = (TextView) popupView.findViewById(R.id.btn_clear);
        mClearData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(mContext, "mClearData", Toast.LENGTH_LONG).show();
            }
        });
        mDeleteData = (TextView) popupView.findViewById(R.id.btn_delete);
        mDeleteData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(mContext, "mDeleteData", Toast.LENGTH_LONG).show();
            }
        });
        mSaveData = (TextView) popupView.findViewById(R.id.btn_save);
        mSaveData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(mContext, "mSaveData", Toast.LENGTH_LONG).show();
            }
        });
        mConfirm = (TextView) popupView.findViewById(R.id.btn_confirm);
        mConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(mContext, "mConfirm: " + mLine1.getText().toString() + ", " + mLine2.getText().toString() + ", " + mLine3.getText().toString() + ", " + mLine4.getText().toString(), Toast.LENGTH_LONG).show();
            }
        });
        mPrintRecord = (TextView) popupView.findViewById(R.id.btn_record);
        mPrintRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(mContext, "mPrintRecord", Toast.LENGTH_LONG).show();
            }
        });
        mExportRecord = (TextView) popupView.findViewById(R.id.btn_export);
        mExportRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(mContext, "mExportRecord", Toast.LENGTH_LONG).show();
            }
        });
        mSelectTemplete = (TextView) popupView.findViewById(R.id.btn_select);
        mSelectTemplete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(mContext, "mSelectTemplete", Toast.LENGTH_LONG).show();
            }
        });
        mSearch = (TextView) popupView.findViewById(R.id.search);
        mSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(mContext, "mSearch: " + mSearchCnt.getText().toString(), Toast.LENGTH_LONG).show();
            }
        });

        mSearchCnt = (EditText) popupView.findViewById(R.id.search_cnt);
        mLine1 = (EditText) popupView.findViewById(R.id.line1);
        mLine2 = (EditText) popupView.findViewById(R.id.line2);
        mLine3 = (EditText) popupView.findViewById(R.id.line3);
        mLine4 = (EditText) popupView.findViewById(R.id.line4);
        mLogoView = (ImageView) popupView.findViewById(R.id.logo);
        mLogoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(mContext, "mLogoView", Toast.LENGTH_LONG).show();
            }
        });

        mPopupWindow.showAtLocation(v, Gravity.NO_GRAVITY, 0, 0);
    }
}
