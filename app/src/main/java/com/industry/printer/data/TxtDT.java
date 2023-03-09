package com.industry.printer.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.TextUtils;
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
import android.widget.TextView;

import com.industry.printer.FileFormat.FilenameSuffixFilter;
import com.industry.printer.FileFormat.SystemConfigFile;
import com.industry.printer.R;
import com.industry.printer.Utils.ConfigPath;
import com.industry.printer.Utils.Configs;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.ToastUtil;
import com.industry.printer.object.BarcodeObject;
import com.industry.printer.object.BaseObject;
import com.industry.printer.object.DynamicText;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class TxtDT {
    private static final String TAG = TxtDT.class.getSimpleName();

    private static TxtDT mInstance = null;
    private Context mContext = null;

    private TextView mPreviewTV;
    private TextView mFileNameTV;
    private TextView mTotalNumTV;
    private EditText mStartLineET;
    private EditText mEndLineET;
    private EditText mCurLineET;
    private EditText mCountET;
    private ImageView mCircleIV;

    private String mFileName;
    private int mTotalLines;
    private int mStartLine;
    private int mEndLine;
    private int mCurLine;
    private int mRepeatCnt;
    private int mPrintRptCnt;
    private boolean mCircle;

    public boolean isTxtDT() {
        SystemConfigFile config = SystemConfigFile.getInstance(mContext);
        return (config.getParam(SystemConfigFile.INDEX_USER_MODE) == SystemConfigFile.USER_MODE_3);
    }

    public static TxtDT getInstance(Context ctx) {
        if (mInstance == null) {
            mInstance = new TxtDT(ctx);
        }
        return mInstance;
    }

    private TxtDT(Context ctx) {
        mContext = ctx;

        mFileName = "";
        mTotalLines = 0;
        mStartLine = 0;
        mEndLine = 0;
        mCurLine = 0;
        mRepeatCnt = 1;
        mPrintRptCnt = 0;
        mCircle = false;
    }

    public void initView(View parent) {
        mPreviewTV = (TextView) parent.findViewById(R.id.txt_preview);
        mPreviewTV.setVisibility(View.VISIBLE);

        mFileNameTV = (TextView) parent.findViewById(R.id.idTxtFileName);
        mFileNameTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectTxtFile();
            }
        });

        mTotalNumTV = (TextView) parent.findViewById(R.id.idTxtTotalNum);
        mStartLineET = (EditText) parent.findViewById(R.id.idTxtStartLine);
        mStartLineET.addTextChangedListener(new TextWatcher(){
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                try {
                    int value = Integer.valueOf(editable.toString());
                    if(value >= 1 && value <= mEndLine && value <= mTotalLines) {
                        mStartLineET.setTextColor(Color.BLACK);
                        mStartLine = value;
                    } else {
                        mStartLineET.setTextColor(Color.RED);
                    }
                } catch(NumberFormatException e) {
                    mStartLineET.setTextColor(Color.RED);
                }
            }
        });
        mEndLineET = (EditText) parent.findViewById(R.id.idTxtEndLine);
        mEndLineET.addTextChangedListener(new TextWatcher(){
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                try {
                    int value = Integer.valueOf(editable.toString());
                    if(value >= 1 && value >= mStartLine && value <= mTotalLines) {
                        mEndLineET.setTextColor(Color.BLACK);
                        mEndLine = value;
                    } else {
                        mEndLineET.setTextColor(Color.RED);
                    }
                } catch(NumberFormatException e) {
                    mEndLineET.setTextColor(Color.RED);
                }
            }
        });
        mCurLineET = (EditText) parent.findViewById(R.id.idTxtCurLine);
        mCurLineET.addTextChangedListener(new TextWatcher(){
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                try {
                    int value = Integer.valueOf(editable.toString());
                    if(value >= mStartLine && value <= mEndLine) {
                        mCurLineET.setTextColor(Color.BLACK);
                        mCurLine = value;
                    } else {
                        mCurLineET.setTextColor(Color.RED);
                    }
                } catch(NumberFormatException e) {
                    mCurLineET.setTextColor(Color.RED);
                }
            }
        });
        mCountET = (EditText) parent.findViewById(R.id.idTxtRepeat);
        mCountET.addTextChangedListener(new TextWatcher(){
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                try {
                    int value = Integer.valueOf(editable.toString());
                    mCountET.setTextColor(Color.BLACK);
                    if(value >= 1) {
                        mCountET.setTextColor(Color.BLACK);
// H.M.Wang 2023-3-9 如果在这里获得值，开始打印前设置为4，开始打印后，会随着打印的进行，将实时数值修改到这个框中，导致1，2，3，4会逐个出现，如果某个值被执行到这一步，就会将mRepeatCnt修改掉，而导致错误动作，因此取消，改到开始开始打印时获取
//                        mRepeatCnt = value;
// End of H.M.Wang 2023-3-9 如果在这里获得值，开始打印前设置为4，开始打印后，会随着打印的进行，将实时数值修改到这个框中，导致1，2，3，4会逐个出现，如果某个值被执行到这一步，就会将mRepeatCnt修改掉，而导致错误动作
                    } else {
                        mCountET.setTextColor(Color.RED);
                    }
                } catch(NumberFormatException e) {
                    mCountET.setTextColor(Color.RED);
                }
            }
        });

        mCircleIV = (ImageView) parent.findViewById(R.id.idTxtCircle);
        mCircleIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCircle = !mCircle;
                if(mCircle) {
                    mCircleIV.setImageResource(R.drawable.checked);
                } else {
                    mCircleIV.setImageResource(R.drawable.check_nor);
                }
            }
        });
    }

    private void selectTxtFile() {
        final ArrayList<String> usbs = ConfigPath.getMountedUsb();
        if (usbs.size() <= 0) {
            ToastUtil.show(mContext, R.string.toast_plug_usb);
            return;
        }

        File dir = new File(usbs.get(0));

//        final File files[] = dir.listFiles(new FilenameSuffixFilter("txt"));
        final File files[] = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                if(new File(file,s).isDirectory())
                    return false;
                if(s.toLowerCase().endsWith("txt"))
                    return true;
                else
                    return false;
            }
        });

        ListView filesView = new ListView(mContext);

        final PopupWindow popupWindow = new PopupWindow(filesView, mFileNameTV.getWidth(), mFileNameTV.getHeight()*5, true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#CC000000")));
        popupWindow.setOutsideTouchable(true);
        popupWindow.setTouchable(true);
        popupWindow.update();
//        popupWindow.showAsDropDown(mFileNameTV, Gravity.NO_GRAVITY, (int)mFileNameTV.getX(), 0);
        popupWindow.showAsDropDown(mFileNameTV);

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
                ((TextView)convertView).setTextSize(mFileNameTV.getTextSize());

                return convertView;
            }
        });

        filesView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(!files[i].equals(mFileName)) {
                    readTxtFile(files[i]);
                    mFileNameTV.setText(mFileName);
                    mTotalNumTV.setText(String.valueOf(mTotalLines));
                    mStartLineET.setText(String.valueOf(mStartLine));
                    mEndLineET.setText(String.valueOf(mEndLine));
                    mCurLineET.setText(String.valueOf(mCurLine));
                    mCountET.setText(String.valueOf(mRepeatCnt));
                }

                popupWindow.dismiss();
            }
        });
    }

    private ArrayList<String> mTxtDataList;

    private void readTxtFile(File file) {
        try {
            FileReader r = new FileReader(file);
            BufferedReader br = new BufferedReader(r);

            ArrayList<String> txtList = new ArrayList<String>();
            int lineNum = 0;

            while(true) {
                String line = br.readLine();
                if (null == line) {
                    break;
                }
                line = line.trim();
                if(!line.isEmpty()) {
                    lineNum++;
                    txtList.add(line);
                }
            }
            mTotalLines = lineNum;
            mTxtDataList = txtList;
            mFileName = file.getName();
            mStartLine = 1;
            mEndLine = mTotalLines;
            mCurLine = 1;
            dispPreview();
        } catch(FileNotFoundException e) {
            Debug.e(TAG, e.getMessage());
        } catch(IOException e) {
            Debug.e(TAG, e.getMessage());
        }

    }

    public void dispPreview() {
        mPreviewTV.postDelayed(new Runnable() {
            @Override
            public void run() {
                mPreviewTV.setTextSize(mPreviewTV.getHeight()/2);
                if(null == mTxtDataList || mCurLine < 1 || mCurLine > mTxtDataList.size()) return;
                mPreviewTV.setText(mTxtDataList.get(mCurLine-1));
            }
        }, 10);
    }

    public void startPrint() {
        dispPreview();
        setData();

        mFileNameTV.setEnabled(false);
        mTotalNumTV.setEnabled(false);
        mStartLineET.setEnabled(false);
        mEndLineET.setEnabled(false);
        mCurLineET.setEnabled(false);
        mCountET.setEnabled(false);
// H.M.Wang 2023-3-9 获取重复次数的操作移到这里，避免打印进程中，显示进度数字会影响到整体的重复次数
        try {
            mRepeatCnt = Integer.valueOf(mCountET.getText().toString());
        } catch(NumberFormatException e) {
            mRepeatCnt = 1;
        }
// End of H.M.Wang 2023-3-9 获取重复次数的操作移到这里，避免打印进程中，显示进度数字会影响到整体的重复次数
        mCircleIV.setEnabled(false);
    }

    public void stopPrint() {
        mFileNameTV.setEnabled(true);
        mTotalNumTV.setEnabled(true);
        mStartLineET.setEnabled(true);
        mEndLineET.setEnabled(true);
        mCurLineET.setEnabled(true);
        mCountET.setEnabled(true);
        mCircleIV.setEnabled(true);
        mCountET.setText("" + mRepeatCnt);
    }

    public boolean gotoNext() {
        mPrintRptCnt++;
        Debug.d(TAG, "Count = [" + mPrintRptCnt + "]");
        final String cnt = mPrintRptCnt + ".";
        mCountET.postDelayed(new Runnable() {
            @Override
            public void run() {
                mCountET.setText(cnt);
            }
        }, 10);

        if(mPrintRptCnt >= mRepeatCnt) {
            mPrintRptCnt = 0;
            if(mCurLine + 1 > mEndLine) {
                if(!mCircle) {
                    return false;
                } else {
                    mCurLine = mStartLine;
                }
            } else {
                mCurLine++;
            }
            dispPreview();
            setData();
        }

        return true;
    }

    private void setData() {
        if (mCurLine < 1 || mCurLine > mTxtDataList.size()) return;

        Debug.d(TAG, "Content Line[" + mCurLine + "] = [" + mTxtDataList.get(mCurLine - 1) + "]");

        SystemConfigFile.getInstance().setRemoteSeparated(mTxtDataList.get(mCurLine - 1));
    }
}
