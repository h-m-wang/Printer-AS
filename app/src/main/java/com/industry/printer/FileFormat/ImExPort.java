package com.industry.printer.FileFormat;

import android.content.Context;
import android.view.View;
import android.widget.ImageButton;

import com.industry.printer.ControlTabActivity;
import com.industry.printer.MainActivity;
import com.industry.printer.MessageTask;
import com.industry.printer.R;
import com.industry.printer.Utils.ConfigPath;
import com.industry.printer.Utils.Configs;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.FileUtil;
import com.industry.printer.Utils.ToastUtil;
import com.industry.printer.Utils.ZipUtil;
import com.industry.printer.ui.CustomerDialog.LoadingDialog;
import com.industry.printer.ui.Test.TestHp22mm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class ImExPort {
    public static final String TAG = ImExPort.class.getSimpleName();

    private Context mContext;

    public ImExPort(Context ctx) {
        mContext = ctx;
    }

    /**
     * import from USB to flash
     */
    public void msgImportOnly(final ArrayList<String> usbs) {
        Observable.just(Configs.SYSTEM_CONFIG_MSG_PATH, Configs.PICTURE_SUB_PATH, Configs.QR_DIR, Configs.FONT_DIR)
                .flatMap(new Func1<String, Observable<Map<String, String>>>() {

                    @Override
                    public Observable<Map<String, String>> call(String arg0) {
                        // TODO Auto-generated method stub
                        Map<String, String> src = new HashMap<String, String>();
                        if (Configs.SYSTEM_CONFIG_MSG_PATH.equals(arg0)) {
                            src.put("source",usbs.get(0) + arg0);
                            src.put("dest", Configs.TLK_PATH_FLASH);
                            src.put("tips", mContext.getString(R.string.tips_import_message));
                        } else if (Configs.PICTURE_SUB_PATH.equals(arg0)) {
                            src.put("source",usbs.get(0) + arg0);
                            src.put("dest", Configs.CONFIG_PATH_FLASH + Configs.PICTURE_SUB_PATH);
                            src.put("tips", mContext.getString(R.string.tips_import_resource));
                        } else if ( Configs.QR_DIR.equals(arg0)) {
                            src.put("source",usbs.get(0) + arg0);
                            src.put("dest", Configs.CONFIG_PATH_FLASH + Configs.SYSTEM_CONFIG_DIR + Configs.QR_DIR);
                            src.put("tips", mContext.getString(R.string.tips_import_sysconf));
                        }
                        else if (Configs.FONT_DIR.equals(arg0)) {
                            src.put("source",usbs.get(0) + Configs.FONT_DIR_USB + File.separator + Configs.FONT_ZIP_FILE);
                            src.put("dest", Configs.CONFIG_PATH_FLASH + File.separator + Configs.FONT_ZIP_FILE);
                            src.put("tips", mContext.getString(R.string.tips_import_font));
                        }
                        Debug.d(TAG, "--->flatMap");
                        return Observable.just(src);
                    }
                })
                .map(new Func1<Map<String, String>, Observable<Void>>() {

                    @Override
                    public Observable<Void> call(Map<String, String> arg0) {
                        try {
                            FileUtil.copyDirectiory(arg0.get("source"), arg0.get("dest"));
                            String dest = arg0.get("dest");

                            if (dest.contains(Configs.FONT_ZIP_FILE)) {
                                Debug.d(TAG, "--->unZipping....");
                                ZipUtil.UnZipFolder(Configs.CONFIG_PATH_FLASH + File.separator + Configs.FONT_ZIP_FILE, Configs.CONFIG_PATH_FLASH);
                            }
                        } catch (Exception e) {
                            // TODO: handle exception
                        }
                        Debug.d(TAG, "--->map");
                        return null;
                    }

                })
                .subscribeOn(Schedulers.io())
                //.observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Observable<Void>>() {
                               @Override
                               public void call(Observable<Void> arg0) {

                               }
                           },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable arg0) {

                            }
                        },
                        new Action0() {

                            @Override
                            public void call() {
                                Debug.d(TAG, "--->complete");
                            }
                        });
    }
    /**
     * import from USB to flash
     */
    public void msgImport(final ArrayList<String> usbs) {
// H.M.Wang 2025-12-15 修改fonts的升级办法，从usb/fonts目录直接复制数字开头的无扩展名文件到/sdcard/fonts目录
//        Observable.just(Configs.SYSTEM_CONFIG_MSG_PATH, Configs.PICTURE_SUB_PATH, Configs.SYSTEM_CONFIG_DIR , Configs.FONT_DIR)
        Observable.just(Configs.SYSTEM_CONFIG_MSG_PATH, Configs.PICTURE_SUB_PATH, Configs.SYSTEM_CONFIG_DIR , Configs.FONT_DIR_USB)
// End of H.M.Wang 2025-12-15 修改fonts的升级办法，从usb/fonts目录直接复制数字开头的无扩展名文件到/sdcard/fonts目录
                .flatMap(new Func1<String, Observable<Map<String, String>>>() {

                    @Override
                    public Observable<Map<String, String>> call(String arg0) {
                        // TODO Auto-generated method stub
                        Map<String, String> src = new HashMap<String, String>();
                        if (Configs.SYSTEM_CONFIG_MSG_PATH.equals(arg0)) {
                            src.put("source",usbs.get(0) + arg0);
                            src.put("dest", Configs.TLK_PATH_FLASH);
                            src.put("tips", mContext.getString(R.string.tips_import_message));
                        } else if (Configs.PICTURE_SUB_PATH.equals(arg0)) {
                            src.put("source",usbs.get(0) + arg0);
                            src.put("dest", Configs.CONFIG_PATH_FLASH + Configs.PICTURE_SUB_PATH);
                            src.put("tips", mContext.getString(R.string.tips_import_resource));
                        } else if ( Configs.SYSTEM_CONFIG_DIR.equals(arg0)) {
                            src.put("source",usbs.get(0) + arg0);
                            src.put("dest", Configs.CONFIG_PATH_FLASH + Configs.SYSTEM_CONFIG_DIR);
                            src.put("tips", mContext.getString(R.string.tips_import_sysconf));
                        }
// H.M.Wang 2025-12-15 修改fonts的升级办法，从usb/fonts目录直接复制数字开头的无扩展名文件到/sdcard/fonts目录
//                        else if (Configs.FONT_DIR.equals(arg0)) {
//                            src.put("source",usbs.get(0) + Configs.FONT_DIR_USB + File.separator + Configs.FONT_ZIP_FILE);
//                            src.put("dest", Configs.CONFIG_PATH_FLASH + File.separator + Configs.FONT_ZIP_FILE);
                        else if (Configs.FONT_DIR_USB.equals(arg0)) {
                            src.put("source",usbs.get(0) + arg0);
                            src.put("dest", Configs.CONFIG_PATH_FLASH + File.separator + Configs.FONT_DIR_USB);
// End of H.M.Wang 2025-12-15 修改fonts的升级办法，从usb/fonts目录直接复制数字开头的无扩展名文件到/sdcard/fonts目录
                            src.put("tips", mContext.getString(R.string.tips_import_font));
                        }
                        Debug.d(TAG, "--->flatMap: " + src.get("tips"));
                        return Observable.just(src);
                    }
                })
                .map(new Func1<Map<String, String>, Observable<Void>>() {

                    @Override
                    public Observable<Void> call(Map<String, String> arg0) {
                        try {
                            Debug.d(TAG, "--->map: " + arg0.get("source") + " -> " + arg0.get("dest"));
// H.M.Wang 2025-12-15 修改fonts的升级办法，从usb/fonts目录直接复制数字开头的无扩展名文件到/sdcard/fonts目录
//                            FileUtil.copyClean(arg0.get("source"), arg0.get("dest"));
                            String dest = arg0.get("dest");
//                            if (dest.endsWith(Configs.FONT_ZIP_FILE)) {
                            if (dest.endsWith(Configs.FONT_DIR_USB)) {
//                                ZipUtil.UnZipFolder(Configs.CONFIG_PATH_FLASH + File.separator + Configs.FONT_ZIP_FILE, Configs.CONFIG_PATH_FLASH);
                                FileUtil.copyFonts(arg0.get("source"), arg0.get("dest"));
                            } else {
                                FileUtil.copyClean(arg0.get("source"), arg0.get("dest"));
// End of H.M.Wang 2025-12-15 修改fonts的升级办法，从usb/fonts目录直接复制数字开头的无扩展名文件到/sdcard/fonts目录
                            }
                        } catch (Exception e) {
                            // TODO: handle exception
                        }
                        return null;
                    }

                })
                .subscribeOn(Schedulers.io())
                //.observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Observable<Void>>() {
                               @Override
                               public void call(Observable<Void> arg0) {

                               }
                           },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable arg0) {

                            }
                        },
                        new Action0() {

                            @Override
                            public void call() {
                                Debug.d(TAG, "--->complete");
                            }
                        });
    }

    /**
     * export out to USB from flash
     */
    public void msgExport(final ArrayList<String> usbs) {
        Observable.just(Configs.SYSTEM_CONFIG_MSG_PATH, Configs.PICTURE_SUB_PATH, Configs.SYSTEM_CONFIG_DIR, "print.bin", Configs.LOG_1, Configs.LOG_2)
                .flatMap(new Func1<String, Observable<Map<String, String>>>() {

                    @Override
                    public Observable<Map<String, String>> call(String arg0) {
                        Debug.d(TAG, "--->flatMap: " + arg0);
                        // TODO Auto-generated method stub
                        Map<String, String> src = new HashMap<String, String>();
                        if (Configs.SYSTEM_CONFIG_MSG_PATH.equals(arg0)) {
                            src.put("source",Configs.TLK_PATH_FLASH);
                            src.put("dest", usbs.get(0) + arg0);
                            src.put("tips", mContext.getString(R.string.tips_export_message));
                        } else if (Configs.PICTURE_SUB_PATH.equals(arg0)) {
                            Debug.d(TAG, "--->copy pictures");
                            src.put("source", Configs.CONFIG_PATH_FLASH + Configs.PICTURE_SUB_PATH);
                            src.put("dest", usbs.get(0) + arg0);
                            src.put("tips", mContext.getString(R.string.tips_export_resource));
                        } else if ( Configs.SYSTEM_CONFIG_DIR.equals(arg0)) {
                            src.put("source", Configs.CONFIG_PATH_FLASH + Configs.SYSTEM_CONFIG_DIR);
                            src.put("dest", usbs.get(0) + arg0);
                            src.put("tips", mContext.getString(R.string.tips_export_sysconf));
// H.M.Wang 2020-2-19 修改导出错误
                        } else if (Configs.LOG_1.equals(arg0)) {
//				} else if (Configs.LOG_1.equals(Configs.LOG_1)) {
                            src.put("source", Configs.LOG_1);
                            src.put("dest", usbs.get(0) + "/log1.txt");
                        } else if (Configs.LOG_2.equals(arg0)) {
//				} else if (Configs.LOG_1.equals(Configs.LOG_2)) {
                            src.put("source", Configs.LOG_2);
                            src.put("dest", usbs.get(0) + "/log2.txt");
// End of H.M.Wang 2020-2-19 修改导出错误
                        } else {
                            FileUtil.deleteFolder(usbs.get(0) + "/print.bin");
                            src.put("source", "/mnt/sdcard/print.bin");
                            src.put("dest", usbs.get(0) + "/print.bin");
                        }
                        Debug.d(TAG, "--->flatMap");
                        return Observable.just(src);
                    }
                })
                .map(new Func1<Map<String, String>, Observable<Void>>() {

                    @Override
                    public Observable<Void> call(Map<String, String> arg0) {
                        try {
                            Debug.d(TAG, "--->start copy");
                            FileUtil.copyDirectiory(arg0.get("source"), arg0.get("dest"));
                        } catch (Exception e) {
                            // TODO: handle exception
                            Debug.d(TAG, "--->copy e: " + e.getMessage());
                        }
                        Debug.d(TAG, "--->map");
                        return null;
                    }

                })
                .subscribeOn(Schedulers.io())
                //.observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Observable<Void>>() {
                               @Override
                               public void call(Observable<Void> arg0) {

                               }
                           },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable arg0) {

                            }
                        },
                        new Action0() {
                            @Override
                            public void call() {
                                Debug.d(TAG, "--->complete");
                            }
                        });
    }
}
