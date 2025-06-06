package com.industry.printer.hardware;

import android.content.ContentValues;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.INotificationSideChannel;
import android.view.KeyEvent;
import android.widget.Toast;

import com.industry.printer.FileFormat.SystemConfigFile;
import com.industry.printer.R;
import com.industry.printer.Utils.Configs;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.ToastUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.lang.Thread.sleep;

/**
 * Created by hmwan on 2020/6/2.
 */

public class BarcodeScanParser {
    private static final String TAG = BarcodeScanParser.class.getSimpleName();

    private static StringBuffer mCodes = new StringBuffer();
    private static long mLast = 0;

    private static Thread mThread = null;

    private static final Map<Integer, String> mMapShiftUnPressed = new HashMap<Integer, String>() {
        {
            put(KeyEvent.KEYCODE_0,             "0");
            put(KeyEvent.KEYCODE_1,             "1");
            put(KeyEvent.KEYCODE_2,             "2");
            put(KeyEvent.KEYCODE_3,             "3");
            put(KeyEvent.KEYCODE_4,             "4");
            put(KeyEvent.KEYCODE_5,             "5");
            put(KeyEvent.KEYCODE_6,             "6");
            put(KeyEvent.KEYCODE_7,             "7");
            put(KeyEvent.KEYCODE_8,             "8");
            put(KeyEvent.KEYCODE_9,             "9");
            put(KeyEvent.KEYCODE_A,             "a");
            put(KeyEvent.KEYCODE_B,             "b");
            put(KeyEvent.KEYCODE_C,             "c");
            put(KeyEvent.KEYCODE_D,             "d");
            put(KeyEvent.KEYCODE_E,             "e");
            put(KeyEvent.KEYCODE_F,             "f");
            put(KeyEvent.KEYCODE_G,             "g");
            put(KeyEvent.KEYCODE_H,             "h");
            put(KeyEvent.KEYCODE_I,             "i");
            put(KeyEvent.KEYCODE_J,             "j");
            put(KeyEvent.KEYCODE_K,             "k");
            put(KeyEvent.KEYCODE_L,             "l");
            put(KeyEvent.KEYCODE_M,             "m");
            put(KeyEvent.KEYCODE_N,             "n");
            put(KeyEvent.KEYCODE_O,             "o");
            put(KeyEvent.KEYCODE_P,             "p");
            put(KeyEvent.KEYCODE_Q,             "q");
            put(KeyEvent.KEYCODE_R,             "r");
            put(KeyEvent.KEYCODE_S,             "s");
            put(KeyEvent.KEYCODE_T,             "t");
            put(KeyEvent.KEYCODE_U,             "u");
            put(KeyEvent.KEYCODE_V,             "v");
            put(KeyEvent.KEYCODE_W,             "w");
            put(KeyEvent.KEYCODE_X,             "x");
            put(KeyEvent.KEYCODE_Y,             "y");
            put(KeyEvent.KEYCODE_Z,             "z");
            put(KeyEvent.KEYCODE_COMMA,         ",");
            put(KeyEvent.KEYCODE_PERIOD,        ".");
            put(KeyEvent.KEYCODE_SPACE,         " ");
            put(KeyEvent.KEYCODE_GRAVE,         "`");
            put(KeyEvent.KEYCODE_MINUS,         "-");
            put(KeyEvent.KEYCODE_EQUALS,        "=");
            put(KeyEvent.KEYCODE_LEFT_BRACKET,  "[");
            put(KeyEvent.KEYCODE_RIGHT_BRACKET, "]");
            put(KeyEvent.KEYCODE_BACKSLASH,     "\\");
            put(KeyEvent.KEYCODE_SEMICOLON,     ";");
            put(KeyEvent.KEYCODE_APOSTROPHE,    "‘");
            put(KeyEvent.KEYCODE_SLASH,         "/");
        }
    };

    private static final Map<Integer, String> mMapShiftPressed = new HashMap<Integer, String>() {
        {
            put(KeyEvent.KEYCODE_0,             ")");
            put(KeyEvent.KEYCODE_1,             "!");
            put(KeyEvent.KEYCODE_2,             "@");
            put(KeyEvent.KEYCODE_3,             "#");
            put(KeyEvent.KEYCODE_4,             "$");
            put(KeyEvent.KEYCODE_5,             "%");
            put(KeyEvent.KEYCODE_6,             "^");
            put(KeyEvent.KEYCODE_7,             "&");
            put(KeyEvent.KEYCODE_8,             "*");
            put(KeyEvent.KEYCODE_9,             "(");
            put(KeyEvent.KEYCODE_A,             "A");
            put(KeyEvent.KEYCODE_B,             "B");
            put(KeyEvent.KEYCODE_C,             "C");
            put(KeyEvent.KEYCODE_D,             "D");
            put(KeyEvent.KEYCODE_E,             "E");
            put(KeyEvent.KEYCODE_F,             "F");
            put(KeyEvent.KEYCODE_G,             "G");
            put(KeyEvent.KEYCODE_H,             "H");
            put(KeyEvent.KEYCODE_I,             "I");
            put(KeyEvent.KEYCODE_J,             "J");
            put(KeyEvent.KEYCODE_K,             "K");
            put(KeyEvent.KEYCODE_L,             "L");
            put(KeyEvent.KEYCODE_M,             "M");
            put(KeyEvent.KEYCODE_N,             "N");
            put(KeyEvent.KEYCODE_O,             "O");
            put(KeyEvent.KEYCODE_P,             "P");
            put(KeyEvent.KEYCODE_Q,             "Q");
            put(KeyEvent.KEYCODE_R,             "R");
            put(KeyEvent.KEYCODE_S,             "S");
            put(KeyEvent.KEYCODE_T,             "T");
            put(KeyEvent.KEYCODE_U,             "U");
            put(KeyEvent.KEYCODE_V,             "V");
            put(KeyEvent.KEYCODE_W,             "W");
            put(KeyEvent.KEYCODE_X,             "X");
            put(KeyEvent.KEYCODE_Y,             "Y");
            put(KeyEvent.KEYCODE_Z,             "Z");
            put(KeyEvent.KEYCODE_COMMA,         "<");
            put(KeyEvent.KEYCODE_PERIOD,        ">");
            put(KeyEvent.KEYCODE_SPACE,         " ");
            put(KeyEvent.KEYCODE_GRAVE,         "~");
            put(KeyEvent.KEYCODE_MINUS,         "_");
            put(KeyEvent.KEYCODE_EQUALS,        "+");
            put(KeyEvent.KEYCODE_LEFT_BRACKET,  "{");
            put(KeyEvent.KEYCODE_RIGHT_BRACKET, "}");
            put(KeyEvent.KEYCODE_BACKSLASH,     "|");
            put(KeyEvent.KEYCODE_SEMICOLON,     ":");
            put(KeyEvent.KEYCODE_APOSTROPHE,    "\"");
            put(KeyEvent.KEYCODE_SLASH,         "?");
        }
    };

    private static Context mContext = null;

    public static void setContext(Context ctx) {
        mContext = ctx;
    }

    public interface OnScanCodeListener {
        public void onCodeReceived(String code);
    }

    private static OnScanCodeListener mCodeListener = null;

    public static void append(int keyCode, boolean shiftPressed) {
        synchronized(BarcodeScanParser.class) {
            long currency = System.currentTimeMillis();
//            if (currency - mLast > 200) {
//                mCodes.delete(0, mCodes.length());
//            }
            mLast = currency;
            String str = null;
            if(shiftPressed) {
                str = mMapShiftPressed.get(keyCode);
            } else {
                str = mMapShiftUnPressed.get(keyCode);
            }
            if(null != str) {
                if(mCodes.length() == 0) {
                    ExtGpio.playClick();
                    if(mContext != null) {
                        Toast.makeText(mContext, R.string.decoding, Toast.LENGTH_SHORT).show();
                    }
                }
                mCodes = mCodes.append(str);
            }
//            Debug.i(TAG, "Char: 0x" + Integer.toHexString(code));
        }

        if(null == mThread) {
            mThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while(true) {
                        synchronized(BarcodeScanParser.class) {
                            if(System.currentTimeMillis() - mLast > 1000) {
                                handleCode();
                                break;
                            } else if(mCodes.length() > 3 && mCodes.substring(mCodes.length()-3, mCodes.length()-1).equals("~~")) {
                                mCodes.setCharAt(mCodes.length()-3, mCodes.charAt(mCodes.length()-1));
                                mCodes.setLength(mCodes.length()-2);
                                handleCode();
                                break;
                            }
                        }
                        try{sleep(10);} catch (Exception e){}
                    }
                    mThread = null;
                }
            });
            mThread.start();
        }
    }

    public static void setListener(OnScanCodeListener l) {
        mCodeListener = l;
    }

    private static void parseCode() {
        StringBuilder sb = new StringBuilder();
        int pos = 0;
        String code = mCodes.toString();

        while(pos < code.length()) {
            int index = code.indexOf("%", pos);
            if(index < 0) {
                sb.append(code.substring(pos));
                break;
            } else {
                if(index > pos) sb.append(code, pos, index);
                if(index + 1 < code.length() && code.charAt(index+1) == '%') {
                    sb.append("%");
                    pos = index + 2;
                } else if(index + 4 < code.length()) {
                    int utf = Integer.parseInt(code.substring(index+1,index+3), 16);
                    utf += (Integer.parseInt(code.substring(index+3,index+5), 16) << 8);
                    sb.append((char) utf);
                    pos = index + 5;
                }
            }
        }

        Debug.i(TAG, "Code: [" + sb.toString() + "] to " + mCodeListener);

        if(null != mCodeListener) {
            mCodeListener.onCodeReceived(sb.toString());
        }
    }

    private static void handleCode() {
        Debug.i(TAG, "Code: [" + mCodes.toString() + "](" + mCodes.length() + ")");

// H.M.Wang 2024-1-12 增加一个扫描协议5，要点： (1) 不做第二位和最后一位的一致性检查；(2)扫描内容按网络协议650的规范，DT0-DT9,BC的格式，分别保存到桶和条码桶中
        if(SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_SCANER5) {
            parseCode();
        } else
// End of H.M.Wang 2024-1-12 增加一个扫描协议5，要点： (1) 不做第二位和最后一位的一致性检查；(2)扫描内容按网络协议650的规范，DT0-DT9,BC的格式，分别保存到桶和条码桶中
// H.M.Wan 2024-7-24 为特殊用户修改扫描协议2时的处理，去掉对末尾字符与第二个字符的匹配检查
//	public static final boolean SCANER2_4_SPECIAL_USE = false;		// 检查字符匹配
        if(SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_DATA_SOURCE) == SystemConfigFile.DATA_SOURCE_SCANER2 && Configs.SCANER2_4_SPECIAL_USE) {
            parseCode();
        } else
// End of H.M.Wan 2024-7-24 为特殊用户修改扫描协议2时的处理，去掉对末尾字符与第二个字符的匹配检查
        if(mCodes.length() > 2 && mCodes.charAt(1) == mCodes.charAt(mCodes.length()-1)) {          // 最后一位与第二位的值需要一致
            mCodes.deleteCharAt(mCodes.length()-1);
            parseCode();
        }
        mCodes.delete(0, mCodes.length());
    }
}
