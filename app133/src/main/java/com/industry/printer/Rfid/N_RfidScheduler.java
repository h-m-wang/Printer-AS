package com.industry.printer.Rfid;

import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;

import com.industry.printer.DataTransferThread;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.PlatformInfo;
import com.industry.printer.hardware.ExtGpio;
import com.industry.printer.hardware.InkManagerFactory;
import com.industry.printer.hardware.N_RFIDManager;
import com.industry.printer.hardware.RFIDManager;
import com.industry.printer.hardware.SmartCard;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class N_RfidScheduler implements IInkScheduler {
    private String TAG = N_RfidScheduler.class.getSimpleName();

    public static N_RfidScheduler mInstance = null;

    private Context mContext;
    private int mHeads;
    private N_RFIDManager mManager;

    // H.M.Wang 2022-10-28 BAGINK专用的墨位管理表
    private static boolean mBaginkImg = false;
    public Handler mCallbackHandler = null;

    public static final int LEVELS[] = {
            ExtGpio.RFID_CARD1,
            ExtGpio.RFID_CARD3,
            ExtGpio.RFID_CARD4,
            ExtGpio.RFID_CARD2,
    };

    private final static int READ_LEVEL_TIMES           = 1;        // 每次读取LEVEL值时尝试的最大次数，然后从成功的次数当中获取平均值，作为本次的读取值。如设置10次，则从下层读取10次，如成功5次，则使用成功的5次获取平均值作为本次读取的最终值
    private final static int PROC_LEVEL_NUMS            = 10;       // 对读取数据进行处理的最小次数，当达到这个数字的时候，处理是否加墨的处理
    private final static int READ_LEVEL_INTERVAL        = 10;		// 10ms
    private final static int ADD_INK_TRY_LIMITS         = 10;       // 加墨的尝试次数

    private int VALID_INK_MIN = 33000000;
    // H.M.Wang 2022-11-9 修改VALID_INK_MAX: 37000000 -> 56000000; ADD_INK_THRESHOLD: 34700000 -> 38000000
    private int VALID_INK_MAX = 56000000;
    private int ADD_INK_THRESHOLD = 38000000;
    // End of H.M.Wang 2022-11-9 修改VALID_INK_MAX: 37000000 -> 56000000; ADD_INK_THRESHOLD: 34700000 -> 38000000
//	private int mShowMsgCD = 10;		// 显示Level信息的倒数计数器，计数器减到0时显示信息，以避免过于频繁的显示信息
    private boolean mLevelReading = false;

    private class BaginkLevel {
        public int mLevelIndex;
        public ArrayList<Integer> mRecentLevels;
        public ArrayList<Integer> mValidLevels;
        public int mHX24LCValue;
        public int mInkAddedTimes;
        public long mInkAddedTime;
        public int mLevelLowCount;
        public int mLevelHighCount;

        public BaginkLevel(int idx) {
            mLevelIndex = idx;
            mRecentLevels = new ArrayList<Integer>();
            mValidLevels = new ArrayList<Integer>();
            mInkAddedTimes = 0;
            mInkAddedTime = 0L;
            mLevelLowCount = 0;
            mLevelHighCount = 0;

            synchronized (ExtGpio.RFID_ACCESS_LOCK) {
                ExtGpio.rfidSwitch(idx);
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                }
                SmartCard.initLevelDirect();
                mHX24LCValue = SmartCard.readHX24LC();
            }
        }
    }

    private BaginkLevel mBaginkLevels[] = null;

//	private ArrayList<Integer>[] mRecentLevels;
//	private int[] mInkAddedTimes;
//	private long[] mInkAddedTime;

    ExecutorService mCachedThreadPool = null;
//	private int mLevelLowCount = 0;
//	private int mLevelHighCount = 0;

    private void readLevelValue(final int cardIdx) {
        Debug.d(TAG, "---> enter readLevelValue(" + cardIdx + ")");

        if(null == mBaginkLevels) {
            Debug.e(TAG, "---> Bagink level data null");
            return;
        }

        if(cardIdx >= mBaginkLevels.length) {
            Debug.e(TAG, "---> Index beyond valid");
            return;
        }

        try {
            long readLevels = 0L;
            int readCount = 0;

            // Read Level READ_LEVEL_TIMES times
            for(int i=0; i<READ_LEVEL_TIMES; i++) {
                synchronized (ExtGpio.RFID_ACCESS_LOCK) {
                    ExtGpio.rfidSwitch(mBaginkLevels[cardIdx].mLevelIndex);
                    try{Thread.sleep(100);}catch(Exception e){};
                    int level = SmartCard.readLevelDirect(cardIdx);
//				ExtGpio.rfidSwitch(mCurrent);
                    if ((level & 0xF0000000) == 0x00000000) {
//					Debug.d(TAG, "Read Level[" + cardIdx + "](" + (readCount + 1) + " times) = " + level);
                        readLevels += (level  -  mBaginkLevels[cardIdx].mHX24LCValue * 100000);
                        readCount++;
                    } else {
                        Debug.e(TAG, "Read Level[" + cardIdx + "]" + Integer.toHexString(level));
                        ExtGpio.playClick();
                    }
                    try{Thread.sleep(READ_LEVEL_INTERVAL);}catch(Exception e){};
                }
            }
            mLevelReading = false;

            int avgLevel = (readCount == 0 ? 0 : (int)(readLevels / readCount));
            Debug.d(TAG, "Read Level[" + cardIdx + "] = " + avgLevel);

            if(avgLevel < VALID_INK_MIN) {
                mBaginkLevels[cardIdx].mLevelLowCount++;
            } else {
                mBaginkLevels[cardIdx].mLevelLowCount = 0;
            }

            if(mBaginkLevels[cardIdx].mLevelLowCount > 3) {
                ExtGpio.playClick();
                Thread.sleep(50);
                ExtGpio.playClick();
                Thread.sleep(50);
                ExtGpio.playClick();
                mCallbackHandler.obtainMessage(DataTransferThread.MESSAGE_LEVEL_ERROR, "Level " + (cardIdx+1) + " value too low, check line").sendToTarget();
            }

            if(avgLevel > VALID_INK_MAX) {
                mBaginkLevels[cardIdx].mLevelHighCount++;
            } else {
                mBaginkLevels[cardIdx].mLevelHighCount = 0;
            }

            if(mBaginkLevels[cardIdx].mLevelHighCount > 3) {
                ExtGpio.playClick();
                Thread.sleep(50);
                ExtGpio.playClick();
                Thread.sleep(50);
                ExtGpio.playClick();
                mCallbackHandler.obtainMessage(DataTransferThread.MESSAGE_LEVEL_ERROR, "Level " + (cardIdx+1) + " might be overfilled").sendToTarget();
            }

            mBaginkLevels[cardIdx].mRecentLevels.add(avgLevel);
            if(mBaginkLevels[cardIdx].mRecentLevels.size() > PROC_LEVEL_NUMS) {
                mBaginkLevels[cardIdx].mRecentLevels.remove(0);
            }
            if(avgLevel >= VALID_INK_MIN && avgLevel <= VALID_INK_MAX) {
                mBaginkLevels[cardIdx].mValidLevels.add(avgLevel);
                if(mBaginkLevels[cardIdx].mValidLevels.size() > PROC_LEVEL_NUMS) {
                    mBaginkLevels[cardIdx].mValidLevels.remove(0);
                }
            }

            // Calculate average level if the count of read data bigger than PROC_LEVEL_NUMS
            Debug.d(TAG, "mValidLevels[" + cardIdx + "].size() = " + mBaginkLevels[cardIdx].mValidLevels.size());
            avgLevel = VALID_INK_MAX;
            if(mBaginkLevels[cardIdx].mValidLevels.size() >= PROC_LEVEL_NUMS) {
                long totalLevel = 0;
                int count = 0;
                for(int i=0; i<PROC_LEVEL_NUMS; i++) {
                    totalLevel += mBaginkLevels[cardIdx].mValidLevels.get(i);
                    count++;
                }
                if(count > 0) avgLevel = (int)(totalLevel / count);
                Debug.d(TAG, "totalLevel = " + totalLevel + "; count = " + count + "; avgLevel = " + avgLevel);
            }
            Debug.d(TAG, "Average Level = " + avgLevel);

            // Launch add ink if the level less than ADD_INK_THRESHOLD.
            if(avgLevel <= ADD_INK_THRESHOLD) {
                // If still less than ADD_INK_THRESHOLD after ADD_INK_TRY_LIMITS times of add-ink action, alarm.
                if(mBaginkLevels[cardIdx].mInkAddedTimes >= ADD_INK_TRY_LIMITS) {
                    ExtGpio.playClick();
                    Thread.sleep(50);
                    ExtGpio.playClick();
                    Thread.sleep(50);
                    ExtGpio.playClick();
                    mCallbackHandler.obtainMessage(DataTransferThread.MESSAGE_LEVEL_ERROR, "Level " + (cardIdx+1) + " might failed in adding ink").sendToTarget();
                } else if(System.currentTimeMillis() - mBaginkLevels[cardIdx].mInkAddedTime > 1000L*60*2) {		// 上次加墨后等待3秒再允许再次开阀
                    Debug.d(TAG, "Add Ink");
                    ExtGpio.setValve(cardIdx, 1);

                    try{Thread.sleep(100);ExtGpio.setValve(cardIdx, 0);}catch(Exception e){
                        ExtGpio.setValve(cardIdx, 0);
                    };

                    mBaginkLevels[cardIdx].mValidLevels.clear();

                    mBaginkLevels[cardIdx].mInkAddedTimes++;
                    mBaginkLevels[cardIdx].mInkAddedTime = System.currentTimeMillis();
                }
            } else {
                mBaginkLevels[cardIdx].mInkAddedTimes = 0;
            }
        } catch(Exception e) {
            Debug.e(TAG, e.getMessage());
        }
        Debug.d(TAG, "---> quit readLevelValue(" + cardIdx + ")");
    }

    public void setCallbackHandler(Handler callback) {
        mCallbackHandler = callback;
    }
// End of H.M.Wang 2022-10-28 BAGINK专用的墨位管理表

    public static N_RfidScheduler getInstance(Context ctx) {
        if (mInstance == null) {
            mInstance = new N_RfidScheduler(ctx);
        }
        return mInstance;

    }

    public N_RfidScheduler(Context ctx) {
        mContext = ctx;
        mHeads = 0;

        mManager = (N_RFIDManager) InkManagerFactory.inkManager(mContext);
    }

    @Override
    public void init(int heads) {
        mHeads = heads;
        mCurrent = 0;

// H.M.Wang 2022-12-13 将2022-10-28日追加的下述操作提到load()函数调用之前，以避免ExtGpio.rfidSwitch(mLevelIndexs[i])将load函数中已经设置好的当前头改变
// H.M.Wang 2022-10-28 追加BAGINK专用的墨位检查功能，这里完成初始化
        mBaginkImg = PlatformInfo.getImgUniqueCode().startsWith("BAGINK");

        if(mBaginkImg) {
            Debug.d(TAG, "Initiate BAGINK variables.");
            mBaginkLevels = new BaginkLevel[LEVELS.length];
            for(int i=0; i<LEVELS.length; i++) {
                mBaginkLevels[i] = new BaginkLevel(LEVELS[i]);
            };

            ADD_INK_THRESHOLD = (mManager.getFeature(0,6) + 256) * 100000;
            if(ADD_INK_THRESHOLD < VALID_INK_MIN || ADD_INK_THRESHOLD > VALID_INK_MAX) {
                mCallbackHandler.obtainMessage(DataTransferThread.MESSAGE_LEVEL_ERROR, "Valve threshold too low").sendToTarget();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            ExtGpio.playClick();
                            Thread.sleep(50);
                            ExtGpio.playClick();
                            Thread.sleep(50);
                            ExtGpio.playClick();
                        } catch (Exception e) {
                            Debug.e(TAG, e.getMessage());
                        }
                    }
                }).start();
            }
            mCachedThreadPool = Executors.newCachedThreadPool();
        }
// End of H.M.Wang 2022-10-28 追加BAGINK专用的墨位检查功能，这里完成初始化
// End of H.M.Wang 2022-12-13 将2022-10-28日追加的下述操作提到load()函数调用之前，以避免ExtGpio.rfidSwitch(mLevelIndexs[i])将load函数中已经设置好的当前头改变
    }

    @Override
    public int count() {
        return mHeads;
    }

    private long mLastBaginkCheck = 0;
    private int mCurrent = 0;
    private static final long TASK_SCHEDULE_INTERVAL = 3000;

    @Override
    public void schedule() {
// H.M.Wang 2022-10-28 追加BAGINK专用的墨位检查功能，这里完成初始化
        if(mBaginkImg) {
            if(SystemClock.elapsedRealtime() - mLastBaginkCheck < TASK_SCHEDULE_INTERVAL) return;
            mLastBaginkCheck = SystemClock.elapsedRealtime();

            Debug.d(TAG, "Heads: " + mHeads + "; Current: " + mCurrent);

            mLevelReading = true;
            mCachedThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    synchronized (N_RfidScheduler.this) {
                        readLevelValue(mCurrent);
//							if (mShowMsgCD == 0) {
//								mShowMsgCD = 10;
                        StringBuilder sb = new StringBuilder();
                        sb.append("Thres: " + (mManager.getFeature(0,6) + 256) + "\n");
                        for (int i = 0; i < mBaginkLevels.length; i++) {
                            sb.append("Level" + (i+1) + "[" + mBaginkLevels[i].mHX24LCValue + "]: ");
                            if(mBaginkLevels[i].mRecentLevels.size() > 0) {
                                for(int j=0; j<mBaginkLevels[i].mRecentLevels.size(); j++) {
                                    if(j > 0) sb.append(",");
                                    sb.append(mBaginkLevels[i].mRecentLevels.get(j) / 100000);
                                }
                            } else {
                                sb.append("n/a");
                            }
                            sb.append("\n");
                            if(mBaginkLevels[i].mInkAddedTime > 0) {
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                                sb.append("    Fuel: " + sdf.format(new Date(mBaginkLevels[i].mInkAddedTime)) + "\n");
                            }
                        }
                        Debug.d(TAG, "Show Level: " + sb.toString());
                        mCallbackHandler.obtainMessage(DataTransferThread.MESSAGE_SHOW_LEVEL, sb.toString()).sendToTarget();
                    }
//							mShowMsgCD--;
//						}
                }
            });
            while(mLevelReading) {
                try{Thread.sleep(10);}catch(Exception e){};
            }
        }
        mCurrent++;
        mCurrent %= mHeads;

// End of H.M.Wang 2022-10-28 追加BAGINK专用的墨位检查功能，这里完成初始化
    }

    @Override
    public void doAfterPrint() {
    }
}
