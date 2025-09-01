package com.industry.printer.ExcelDataProc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.industry.printer.DataTransferThread;
import com.industry.printer.FileFormat.SystemConfigFile;
import com.industry.printer.R;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.ToastUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ExcelMainWindow {
    public static final String TAG = ExcelMainWindow.class.getSimpleName();

    private Context mContext = null;
    private Handler mCallback;
    private PopupWindow mPopupWindow = null;

    private ListView mExcelDataLV = null;
    private ProgressBar mProcessing;
    private TextView mImportExcel = null;
    private TextView mClearData = null;
    private TextView mDeleteData = null;
    private TextView mSaveData = null;
    private TextView mConfirm = null;
    private TextView mPrintRecord = null;
    private TextView mExportRecord = null;
    private TextView mSelectTemplete = null;
    private TextView mSearch = null;
    private ExeclPrintRecord mPrintRecordProc;

    private EditText mSearchCnt = null;
    private EditText mLine1 = null;
    private EditText mLine2 = null;
    private EditText mLine3 = null;
    private EditText mLine4 = null;
    private ImageView mLogoView = null;

    private List<String[]> mData;
    private List<Integer> mDataIndex;
    private int mSelectedItemNo = -1;

    private BaseAdapter mExcelDataLVAdapter = null;

    public ExcelMainWindow(Context ctx, Handler callback) {
        mContext = ctx;
        mCallback = callback;
        mData = new ArrayList<String[]>();
        mDataIndex = new ArrayList<Integer>();
    }

    public static Bitmap LOGO_BITMAP = null;

    private void selectPosition(int pos) {
        if(pos >= 0 && pos <mData.size()) {
            mSelectedItemNo = pos;
            mExcelDataLVAdapter.notifyDataSetChanged();
            mLine1.setText(mData.get(mDataIndex.get(mSelectedItemNo))[1]);
            mLine2.setText(mData.get(mDataIndex.get(mSelectedItemNo))[2]);
            mLine3.setText(mData.get(mDataIndex.get(mSelectedItemNo))[3]);
            mLine4.setText(mData.get(mDataIndex.get(mSelectedItemNo))[4]);

            if(mData.get(mDataIndex.get(mSelectedItemNo))[0].equalsIgnoreCase("金元素厂标")) {
                mLogoView.setImageResource(R.drawable.logo1);
                LOGO_BITMAP = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.logo1);
            } else if(mData.get(mDataIndex.get(mSelectedItemNo))[0].equalsIgnoreCase("南钢厂标")) {
                mLogoView.setImageResource(R.drawable.logo2);
                LOGO_BITMAP = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.logo2);
            } else if(mData.get(mDataIndex.get(mSelectedItemNo))[0].equalsIgnoreCase("船级社图标")) {
                mLogoView.setImageResource(R.drawable.logo3);
                LOGO_BITMAP = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.logo3);
            }
        }
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

        mProcessing = (ProgressBar) popupView.findViewById(R.id.processing);
        mProcessing.setVisibility(View.GONE);

        ImageView cancel = (ImageView) popupView.findViewById(R.id.cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPopupWindow.dismiss();
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if(!(new File("/mnt/sdcard/execldata.txt").exists())) return;
                    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("/mnt/sdcard/execldata.txt"), "UTF-8"));
                    String line;
                    mData.clear();
                    mDataIndex.clear();
                    int pos=0;
                    while((line = br.readLine()) != null) {
                        String[] data = line.split(",");
                        mData.add(data);
                        mDataIndex.add(pos++);
                    }
                    br.close();
                    mExcelDataLV.post(new Runnable() {
                        @Override
                        public void run() {
                            selectPosition(0);
                            mExcelDataLV.setSelection(0);
                        }
                    });
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        mImportExcel = (TextView) popupView.findViewById(R.id.btn_import_file);
        mImportExcel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ExeclFile ecf = new ExeclFile(mContext);
                ecf.pickupFile(v, new ExeclFile.ExeclFileSelect() {
                    @Override
                    public void onFileSelected(final String path) {
                        mProcessing.setVisibility(View.VISIBLE);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                ImportExcelData ied = new ImportExcelData();
                                List<String[]> data = ied.importExcel(path);
                                if(null != data) {
                                    mData = data;
                                    for(int i=0; i<mData.size(); i++) mDataIndex.add(i);
                                    mImportExcel.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            mProcessing.setVisibility(View.GONE);
                                            selectPosition(0);
                                            mExcelDataLV.setSelection(0);
                                        }
                                    });
                                }
                            }
                        }).start();
                    }
                });
            }
        });
        mClearData = (TextView) popupView.findViewById(R.id.btn_clear);
        mClearData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mData.clear();
                mDataIndex.clear();
                mExcelDataLVAdapter.notifyDataSetChanged();
            }
        });
        mDeleteData = (TextView) popupView.findViewById(R.id.btn_delete);
        mDeleteData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mData.remove(mDataIndex.get(mSelectedItemNo));
                mDataIndex.remove(mSelectedItemNo);
                selectPosition(Math.min(mSelectedItemNo, mDataIndex.size()-1));
                mExcelDataLV.setSelection(mSelectedItemNo);
            }
        });
        mSaveData = (TextView) popupView.findViewById(R.id.btn_save);
        mSaveData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("/mnt/sdcard/execldata.txt"), "UTF-8"));
                    for(int i=0; i<mData.size(); i++) {
                        bw.write(mData.get(i)[0] + "," + mData.get(i)[1] + "," + mData.get(i)[2] + "," + mData.get(i)[3] + "," + mData.get(i)[4] + "\n");
                    }
                    bw.close();
                    ToastUtil.show(mContext, "Data saved");
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
        });
        mConfirm = (TextView) popupView.findViewById(R.id.btn_confirm);
        mConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mSelectedItemNo >= 0) {
                    mData.get(mDataIndex.get(mSelectedItemNo))[1] = mLine1.getText().toString();
                    mData.get(mDataIndex.get(mSelectedItemNo))[2] = mLine2.getText().toString();
                    mData.get(mDataIndex.get(mSelectedItemNo))[3] = mLine3.getText().toString();
                    mData.get(mDataIndex.get(mSelectedItemNo))[4] = mLine4.getText().toString();

                    SystemConfigFile.getInstance(mContext).setDTBuffer(0, mData.get(mDataIndex.get(mSelectedItemNo))[1]);
                    SystemConfigFile.getInstance(mContext).setDTBuffer(1, mData.get(mDataIndex.get(mSelectedItemNo))[2]);
                    SystemConfigFile.getInstance(mContext).setDTBuffer(2, mData.get(mDataIndex.get(mSelectedItemNo))[3]);
                    SystemConfigFile.getInstance(mContext).setDTBuffer(3, mData.get(mDataIndex.get(mSelectedItemNo))[4]);

                    DataTransferThread dTransThread = DataTransferThread.getInstance(mContext);
                    if (dTransThread != null && dTransThread.isRunning()) {
                        dTransThread.mNeedUpdate = true;
                        mPrintRecordProc.appendRecord(mData.get(mDataIndex.get(mSelectedItemNo)));
                    }
                }
            }
        });

        mPrintRecord = (TextView) popupView.findViewById(R.id.btn_record);
        mPrintRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPrintRecordProc.showPrintRecord(v);
            }
        });
        mExportRecord = (TextView) popupView.findViewById(R.id.btn_export);
        mExportRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPrintRecordProc.exportPrintRecord();
            }
        });

/*        mSearch = (TextView) popupView.findViewById(R.id.search);
        mSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mProcessing.setVisibility(View.VISIBLE);
                for(int i=0; i<mData.size(); i++) {
                    if(mData.get(i)[1].equalsIgnoreCase(mSearchCnt.getText().toString().trim())) {
                        selectPosition(i);
                        mExcelDataLV.setSelection(i);
                        mProcessing.setVisibility(View.GONE);
                        return;
                    }
                }
                mProcessing.setVisibility(View.GONE);
                ToastUtil.show(mContext, "Not found");
            }
        });
*/
        mSearchCnt = (EditText) popupView.findViewById(R.id.search_cnt);
        mSearchCnt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                mDataIndex.clear();
                if(editable.toString().isEmpty()) {
                    for(int i=0; i<mData.size(); i++) {
                        mDataIndex.add(i);
                    }
                } else {
                    for(int i=0; i<mData.size(); i++) {
                        if(mData.get(i)[1].contains(editable.toString())) {
                            mDataIndex.add(i);
                        }
                    }
                }
                mSelectedItemNo = -1;
                mExcelDataLVAdapter.notifyDataSetChanged();
            }
        });
        mLine1 = (EditText) popupView.findViewById(R.id.line1);
        mLine2 = (EditText) popupView.findViewById(R.id.line2);
        mLine3 = (EditText) popupView.findViewById(R.id.line3);
        mLine4 = (EditText) popupView.findViewById(R.id.line4);
        mLogoView = (ImageView) popupView.findViewById(R.id.logo);

        mPrintRecordProc = new ExeclPrintRecord(mContext);

        mExcelDataLV = (ListView) popupView.findViewById(R.id.xl_data_list);
        mExcelDataLVAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return mDataIndex == null ? 0 : mDataIndex.size();
            }

            @Override
            public Object getItem(int i) {
                return mDataIndex == null || mData == null ? 0 : mData.get(mDataIndex.get(i));
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
                if(mSelectedItemNo == position) {
                    convertView.setBackgroundColor(Color.YELLOW);
                } else {
                    convertView.setBackgroundColor(Color.TRANSPARENT);
                }
//                TextView searchIDTV = (TextView) convertView.findViewById(R.id.search_id);
//                searchIDTV.setText(mData.get(position)[1]);
                TextView line1 = (TextView) convertView.findViewById(R.id.line_1);
                line1.setText(mData.get(mDataIndex.get(position))[1]);
                TextView line2 = (TextView) convertView.findViewById(R.id.line_2);
                line2.setText(mData.get(mDataIndex.get(position))[2]);
                TextView line3 = (TextView) convertView.findViewById(R.id.line_3);
                line3.setText(mData.get(mDataIndex.get(position))[3]);
                TextView line4 = (TextView) convertView.findViewById(R.id.line_4);
                line4.setText(mData.get(mDataIndex.get(position))[4]);
                return convertView;
            }
        };
        mSelectedItemNo = -1;

        mExcelDataLV.setAdapter(mExcelDataLVAdapter);
        mExcelDataLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                selectPosition(i);
            }
        });

        mPopupWindow.showAtLocation(v, Gravity.NO_GRAVITY, 0, 0);
    }
}
