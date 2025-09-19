package com.industry.printer.Server1;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
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
import android.widget.RadioButton;
import android.widget.TextView;

import com.industry.printer.ControlTabActivity;
import com.industry.printer.DataTransferThread;
import com.industry.printer.FileFormat.SystemConfigFile;
import com.industry.printer.R;
import com.industry.printer.ThreadPoolManager;
import com.industry.printer.Utils.ConfigPath;
import com.industry.printer.Utils.Configs;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.HttpUtils;
import com.industry.printer.Utils.ToastUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class Server1MainWindow {
    public static final String TAG = Server1MainWindow.class.getSimpleName();

    private Context mContext;
    private Handler mCallback;
    private PopupWindow mPopupWindow;

    private ListView mPostResultLV;
    private BaseAdapter mPostResultLVAdapter;
    private ProgressBar mProcessing;
    private EditText mSearchWord;
    private TextView mGetFromHost;
    private TextView mPrint;

    private ArrayList<String[]> mResults;
    private int mSelectedItemNo;

    private static Server1MainWindow mInstance;

    public static Server1MainWindow getInstance(Context ctx) {
        if(null == mInstance) {
            mInstance = new Server1MainWindow(ctx);
        }
        return mInstance;
    }

    private Server1MainWindow(Context ctx) {
        mContext = ctx;
        mCallback = null;

        mResults = new ArrayList<String[]>();
        mSelectedItemNo = -1;

        ThreadPoolManager.mThreads.execute(new Runnable() {
            @Override
            public void run() {
                try{Thread.sleep(500);}catch(Exception e){}
                ArrayList<String[]> readData = readDataFromFile();
                if(null != readData) {
                    mResults = readData;
                }
            }
        });
    }

    private void selectPosition(int pos) {
        if(pos >= 0 && pos < mResults.size() && (pos != mSelectedItemNo || mSelectedItemNo == -1)) {
            mSelectedItemNo = pos;
            Debug.d(TAG, "mSelectedItemNo = " + mSelectedItemNo);

            SystemConfigFile sysConfig = SystemConfigFile.getInstance(mContext);
            sysConfig.setDTBuffer(0, mResults.get(pos)[0]);
            sysConfig.setDTBuffer(1, mResults.get(pos)[1]);
            sysConfig.setDTBuffer(2, mResults.get(pos)[2]);
            sysConfig.setDTBuffer(3, mResults.get(pos)[3]);
            sysConfig.setDTBuffer(4, mResults.get(pos)[4]);
            sysConfig.setDTBuffer(5, mResults.get(pos)[5]);
            sysConfig.setDTBuffer(6, mResults.get(pos)[6]);
            sysConfig.setDTBuffer(7, mResults.get(pos)[7]);

            DataTransferThread thread = DataTransferThread.getInstance(mContext);
            if (thread != null && thread.isRunning()) {
                thread.mNeedUpdate = true;
            }

            mGetFromHost.post(new Runnable() {
                @Override
                public void run() {
                    mPostResultLVAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    private ArrayList<String[]> readDataFromFile() {
        try {
            if(!(new File("/mnt/sdcard/server1data.txt").exists())) return null;
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("/mnt/sdcard/server1data.txt"), "UTF-8"));
            String line;
            ArrayList<String[]> readData = new ArrayList<String[]>();
            while((line = br.readLine()) != null) {
                String[] data = line.split(",");
                if(data.length >= 8) {
                    readData.add(data);
                }
            }
            br.close();
            return readData;
        } catch(IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean writeDataToFile(ArrayList<String[]> writeData) {
        try {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("/mnt/sdcard/server1data.txt"), "UTF-8"));
            for(int i=0; i<writeData.size(); i++) {
                bw.write(
                    writeData.get(i)[0] + "," +
                        writeData.get(i)[1] + "," +
                        writeData.get(i)[2] + "," +
                        writeData.get(i)[3] + "," +
                        writeData.get(i)[4] + "," +
                        writeData.get(i)[5] + "," +
                        writeData.get(i)[6] + "," +
                        writeData.get(i)[7] + "\n");
            }
            bw.close();
            return true;
        } catch(IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private String mErrMsg;

    private ArrayList<String[]> pickupResults(String gotData) {
        try {
            ArrayList<String[]> results = new ArrayList<String[]>();
            mErrMsg = "";

            JSONObject jObj = new JSONObject(gotData);
            boolean success = jObj.getBoolean("success");
            if(!success) {
                JSONObject errObj = jObj.getJSONObject("error");
                mErrMsg = errObj.getString("message");
                return null;
            }
            JSONArray jsonResultArray = jObj.getJSONArray("result");
            for(int i=0; i<jsonResultArray.length(); i++) {
                JSONObject aObj = jsonResultArray.getJSONObject(i);
                String[] vals = new String[8];
                vals[0] = aObj.getString("printRowMsg1");
                vals[1] = aObj.getString("printRowMsg2");
                vals[2] = aObj.getString("printRowMsg3");
                vals[3] = aObj.getString("printRowMsg4");
                vals[4] = aObj.getString("stove");
                vals[5] = aObj.getString("pieceNo");
                vals[6] = aObj.getString("printRowCnt");
                vals[7] = aObj.getString("id");
                results.add(vals);
            }
            writeDataToFile(results);
            return results;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setCallback(Handler callback) {
        mCallback = callback;
    }

    public void show(final View v) {
        if (null == mContext) {
            return;
        }

        View popupView = LayoutInflater.from(mContext).inflate(R.layout.server1_data_layout, null);

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
                if(v instanceof RadioButton) ((RadioButton)v).setChecked(true);
            }
        });

        mSearchWord = (EditText) popupView.findViewById(R.id.search_word);
        mSearchWord.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String keyWord = editable.toString();
                if(keyWord.length() >= 4) {
                    for(int i=0; i<mResults.size(); i++) {
                        if(mResults.get(i)[5].endsWith(keyWord)) {
                            selectPosition(i);
                            mPostResultLV.smoothScrollToPosition(i);
                            break;
                        }
                    }
                }
            }
        });

        TextView printID = (TextView) popupView.findViewById(R.id.printer_no);
        printID.setText(SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_LOCAL_ID) + "号喷码机");
        
        mGetFromHost = (TextView) popupView.findViewById(R.id.btn_post);
        mGetFromHost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mProcessing.setVisibility(View.VISIBLE);
                ThreadPoolManager.mThreads.execute(new Runnable() {
                    @Override
                    public void run() {
                        final StringBuilder sb = new StringBuilder();
                        HttpUtils httpUtils = new HttpUtils(
                                "http://175.170.155.72:9678/nancy/api-services/RV.Core.Services.SMB.InkPrintService/GetInkPrintMsg",
                                "POST",
                                "{\"inkQryReq\":{\"Dvc\":\"" + SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_LOCAL_ID) + "\"}}",
                                new HttpUtils.HttpResponseListener() {
                                    @Override
                                    public void onReceived(String str) {
                                        if(null != str) {
                                            sb.append(str);
                                        } else {
                                            if(sb.length() == 0) {
                                                mGetFromHost.post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        mProcessing.setVisibility(View.GONE);
                                                        ToastUtil.show(mContext, "Network access error!");
                                                    }
                                                });
                                            } else {
                                                Debug.d(TAG, sb.toString());
                                                ArrayList<String[]> results = pickupResults(sb.toString());
                                                if(results != null) {
                                                    mResults = results;
                                                    mGetFromHost.post(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            mProcessing.setVisibility(View.GONE);
                                                            selectPosition(0);
                                                            mPostResultLV.setSelection(0);
                                                        }
                                                    });
                                                } else {
                                                    mGetFromHost.post(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            mProcessing.setVisibility(View.GONE);
                                                            if(!mErrMsg.isEmpty()) {
                                                                ToastUtil.show(mContext, mErrMsg);
                                                            } else {
                                                                ToastUtil.show(mContext, "Unknown Error");
                                                            }
                                                        }
                                                    });
                                                }
                                            }
                                        }
                                    }
                                }
                        );
                        httpUtils.access();
                    }
                });
            }
        });

        mPrint = (TextView) popupView.findViewById(R.id.btn_print);
        mPrint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(null != mCallback) {
                    String filePath = ConfigPath.getTlkPath() + File.separator + Configs.GROUP_PREFIX + mResults.get(mSelectedItemNo)[6].substring(0,1);
                    if(new File(filePath).exists()) {
                        Message msg = mCallback.obtainMessage(ControlTabActivity.MESSAGE_OPEN_PREVIEW);
                        Bundle bundle = new Bundle();
                        bundle.putString("file", Configs.GROUP_PREFIX + mResults.get(mSelectedItemNo)[6].substring(0,1));
                        bundle.putBoolean("printAfterLoad", true);
                        msg.setData(bundle);
                        mCallback.sendMessage(msg);
                    } else {
                        ToastUtil.show(mContext, R.string.str_tlk_not_found);
                    }
                }
            }
        });

        mPostResultLV = (ListView) popupView.findViewById(R.id.post_data_list);
        mPostResultLVAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return mResults == null ? 0 : mResults.size();
            }

            @Override
            public Object getItem(int i) {
                return mResults == null ? 0 : mResults.get(i);
            }

            @Override
            public long getItemId(int i) {
                return 0;
            }

            @Override
            public View getView(final int position, View convertView, ViewGroup parent) {
                if(null == convertView) {
                    convertView = LayoutInflater.from(mContext).inflate(R.layout.server1_data_list_item, null);
                }

                if(mSelectedItemNo == position) {
                    convertView.setBackgroundColor(Color.YELLOW);
                } else {
                    convertView.setBackgroundColor(Color.TRANSPARENT);
                }

                TextView stoveTv = (TextView) convertView.findViewById(R.id.stove);
                stoveTv.setText(mResults.get(position)[4]);
                TextView pieceNoTv = (TextView) convertView.findViewById(R.id.pieceNo);
                pieceNoTv.setText(mResults.get(position)[5]);
                TextView printRowCntTv = (TextView) convertView.findViewById(R.id.printRowCnt);
                printRowCntTv.setText(mResults.get(position)[6]);
                TextView printRowMsg1Tv = (TextView) convertView.findViewById(R.id.printRowMsg1);
                printRowMsg1Tv.setText(mResults.get(position)[0]);
                TextView printRowMsg2Tv = (TextView) convertView.findViewById(R.id.printRowMsg2);
                printRowMsg2Tv.setText(mResults.get(position)[1]);
                TextView printRowMsg3Tv = (TextView) convertView.findViewById(R.id.printRowMsg3);
                printRowMsg3Tv.setText(mResults.get(position)[2]);
                TextView printRowMsg4Tv = (TextView) convertView.findViewById(R.id.printRowMsg4);
                printRowMsg4Tv.setText(mResults.get(position)[3]);
                TextView idTv = (TextView) convertView.findViewById(R.id.id);
                idTv.setText(mResults.get(position)[7]);

                return convertView;
            }
        };

        mPostResultLV.setAdapter(mPostResultLVAdapter);
        mPostResultLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                DataTransferThread thread = DataTransferThread.getInstance(mContext);
                if (thread != null && thread.isRunning()) {
                    if(!mResults.get(mSelectedItemNo)[6].equalsIgnoreCase(mResults.get(i)[6])) {
                        if(null != mCallback) {
                            String filePath = ConfigPath.getTlkPath() + File.separator + Configs.GROUP_PREFIX + mResults.get(i)[6].substring(0,1);
                            if(new File(filePath).exists()) {
                                Message msg = mCallback.obtainMessage(ControlTabActivity.MESSAGE_OPEN_PREVIEW);
                                Bundle bundle = new Bundle();
                                bundle.putString("file", Configs.GROUP_PREFIX + mResults.get(i)[6].substring(0,1));
                                bundle.putBoolean("printNext", true);
                                msg.setData(bundle);
                                mCallback.sendMessage(msg);
                                thread.setIndex(0);     // 从第一个成员开始
                            } else {
                                mCallback.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        ToastUtil.show(mContext, R.string.str_tlk_not_found);
                                    }
                                });
                            }
                        }
                    }
                }
                selectPosition(i);
            }
        });
        mPostResultLV.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                final int pos = i;
                final View tView = view;

                View detailView = LayoutInflater.from(mContext).inflate(R.layout.server1_data_detail, null);

                final PopupWindow detailWindow = new PopupWindow(detailView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
                detailWindow.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#CC000000")));
                detailWindow.setOutsideTouchable(true);
                detailWindow.setTouchable(true);
                detailWindow.update();

                TextView msg1TV = (TextView) detailView.findViewById(R.id.msg1);
                msg1TV.setText(mResults.get(pos)[0]);
                TextView msg2TV = (TextView) detailView.findViewById(R.id.msg2);
                msg2TV.setText(mResults.get(pos)[1]);
                TextView msg3TV = (TextView) detailView.findViewById(R.id.msg3);
                msg3TV.setText(mResults.get(pos)[2]);
                TextView msg4TV = (TextView) detailView.findViewById(R.id.msg4);
                msg4TV.setText(mResults.get(pos)[3]);
                TextView stoveTV = (TextView) detailView.findViewById(R.id.stove);
                stoveTV.setText(mResults.get(pos)[4]);
                view.setBackgroundColor(Color.GRAY);

                detailWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                    @Override
                    public void onDismiss() {
                        if(mSelectedItemNo == pos) {
                            tView.setBackgroundColor(Color.YELLOW);
                        } else {
                            tView.setBackgroundColor(Color.TRANSPARENT);
                        }
                    }
                });

                detailView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        detailWindow.dismiss();
                    }
                });

                detailWindow.showAtLocation(v, Gravity.CENTER, 0, 0);
                return false;
            }
        });
        mPopupWindow.showAtLocation(v, Gravity.NO_GRAVITY, 0, 0);

        if(mSelectedItemNo == -1) selectPosition(0); else selectPosition(mSelectedItemNo);
        mPostResultLV.setSelection(mSelectedItemNo);
    }

    public void goNextLine() {
        if(mSelectedItemNo >= 0 && mSelectedItemNo < mResults.size()) {
            final StringBuilder sb = new StringBuilder();
            HttpUtils httpUtils = new HttpUtils(
                    "http://175.170.155.72:9678/nancy/api-services/RV.Core.Services.SMB.InkPrintService/InkPrint",
                    "POST",
                    "{\"inkPrtReq\":{\"Dvc\":\"" + SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_LOCAL_ID) + "\",\"Id\":\"" + mResults.get(mSelectedItemNo)[7] + "\"}}",
                    new HttpUtils.HttpResponseListener() {
                        @Override
                        public void onReceived(final String str) {
                            if(null != str) {
                                sb.append(str);
                            } else {
                                Debug.d(TAG, sb.toString());
                                mGetFromHost.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            JSONObject jObj = new JSONObject(sb.toString());
                                            ToastUtil.show(mContext, SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_LOCAL_ID) + "# " + jObj.getString("result"));
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                            }
                        }
                    }
            );
            httpUtils.access();

            if(mSelectedItemNo+1 < mResults.size()) {
                selectPosition(mSelectedItemNo+1);
                if(!mResults.get(mSelectedItemNo)[6].equalsIgnoreCase(mResults.get(mSelectedItemNo-1)[6])) {
                    if(null != mCallback) {
                        String filePath = ConfigPath.getTlkPath() + File.separator + Configs.GROUP_PREFIX + mResults.get(mSelectedItemNo)[6].substring(0,1);
                        if(new File(filePath).exists()) {
                            Message msg = mCallback.obtainMessage(ControlTabActivity.MESSAGE_OPEN_PREVIEW);
                            Bundle bundle = new Bundle();
                            bundle.putString("file", Configs.GROUP_PREFIX + mResults.get(mSelectedItemNo)[6].substring(0,1));
                            bundle.putBoolean("printNext", true);
                            msg.setData(bundle);
                            mCallback.sendMessage(msg);
                        } else {
                            mCallback.post(new Runnable() {
                                @Override
                                public void run() {
                                    ToastUtil.show(mContext, R.string.str_tlk_not_found);
                                }
                            });
                        }
                    }
                }
                mGetFromHost.post(new Runnable() {
                    @Override
                    public void run() {
                        if(mPostResultLV.getLastVisiblePosition() < mSelectedItemNo+1) mPostResultLV.smoothScrollByOffset(1);
                    }
                });
            } else {
                if(null != mCallback) mCallback.sendEmptyMessageDelayed(ControlTabActivity.MESSAGE_PRINT_STOP, 1000L);
            }
        }
    }
}
