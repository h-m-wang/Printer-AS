package com.industry.printer.Rfid;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.industry.printer.DataTransferThread;
import com.industry.printer.FileFormat.SystemConfigFile;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.PlatformInfo;
import com.industry.printer.hardware.ExtGpio;
import com.industry.printer.hardware.FpgaGpioOperation;
import com.industry.printer.hardware.InkManagerFactory;
import com.industry.printer.hardware.RFIDManager;
import com.industry.printer.hardware.SmartCard;

import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;

public class RfidScheduler implements IInkScheduler {
	private String TAG = RfidScheduler.class.getSimpleName();
	
	public static RfidScheduler mInstance = null;
	// 5S间隔
	public static final long TASK_SCHEDULE_INTERVAL = 3000;
	public static final long RFID_SWITCH_INTERVAL = 1000;
	
	private Context mContext;
	private List<RfidTask> mRfidTasks = null;
	private int mCurrent = 0;
	private long mSwitchTimeStemp=0;
	private Thread mAfter;
	private boolean running=false;
	private RFIDManager mManager;

// H.M.Wang 2022-10-28 BAGINK专用的墨位管理表
	private static boolean mBaginkImg = false;
	public Handler mCallbackHandler = null;

	private static final int LEVEL_NUM_A133 = 16;

	public static final int LEVELS[] = {
		ExtGpio.RFID_CARD1,
		ExtGpio.RFID_CARD3,
		ExtGpio.RFID_CARD4,
		ExtGpio.RFID_CARD2,
	};

	private final static int READ_LEVEL_TIMES           = 1;        // 每次读取LEVEL值时尝试的最大次数，然后从成功的次数当中获取平均值，作为本次的读取值。如设置10次，则从下层读取10次，如成功5次，则使用成功的5次获取平均值作为本次读取的最终值
	private final static int PROC_LEVEL_NUMS            = 10;       // 对读取数据进行处理的最小次数，当达到这个数字的时候，处理是否加墨的处理
	private final static int READ_LEVEL_INTERVAL        = 10;		// 10ms
	private final static int ADD_INK_TRY_LIMITS         = 20;       // 加墨的尝试次数	2025-8-20 由于调整了加墨量，次数从10次修改为20次

	private int VALID_INK_MIN = 33000000;
// H.M.Wang 2022-11-9 修改VALID_INK_MAX: 37000000 -> 56000000; ADD_INK_THRESHOLD: 34700000 -> 38000000
	private int VALID_INK_MAX = 56000000;
	private int ADD_INK_THRESHOLD = 38000000;
// End of H.M.Wang 2022-11-9 修改VALID_INK_MAX: 37000000 -> 56000000; ADD_INK_THRESHOLD: 34700000 -> 38000000
//	private int mShowMsgCD = 10;		// 显示Level信息的倒数计数器，计数器减到0时显示信息，以避免过于频繁的显示信息
	private boolean mLevelReading = false;

// H.M.Wang 2024-8-6 增加一个判断Level测量芯片种类的函数，apk会根据不同的芯片种类，执行不同的逻辑。读取Level值也会根据不同的种类而调用不同的接口
// 1. RFID 烧94 ， 则在传感器连续读到高于44，加墨
// 2. 连续读到高于50，报错， 不加墨
// 3. 连续读到低于38报错， 不加墨
// 4. 此项目为H21
	private static int LEVEL_CHIP_TYPE_NONE = 0;
	private static int LEVEL_CHIP_TYPE_1614 = 1;
	private static int LEVEL_CHIP_TYPE_MCPH21 = 2;
	private int VALID_INK_MIN_MCPH21 = 3500000;     // H.M.Wang 2024-8-26 4000000 -> 3800000 -> 3500000 (2024-8-28)
	private int VALID_INK_MAX_MCPH21 = 5000000;     // H.M.Wang 2024-8-26 4600000 -> 5000000
	private int ADD_INK_THRESHOLD_MCPH21 = 4400000;
// End of H.M.Wang 2024-8-6 增加一个判断Level测量芯片种类的函数，apk会根据不同的芯片种类，执行不同的逻辑。读取Level值也会根据不同的种类而调用不同的接口

// H.M.Wang 2023-4-1 临时增加异常值管理，当最近5分钟内大于560的次数超过5%时，报警，停止加墨；当相邻两次取值相差50点以上的次数>30%时，报警，停止加墨
	private class Level_Record {
		public long RecordedTime;
		public int  Level;

		public Level_Record(long rt, int level) {
			RecordedTime = rt;
			Level = level;
		}
	};
// End of H.M.Wang 2023-4-1 临时增加异常值管理，当最近5分钟内大于560的次数超过5%时，报警，停止加墨；当相邻两次取值相差50点以上的次数>30%时，报警，停止加墨

	private class BaginkLevel {
		public int mLevelIndex;
		public ArrayList<Integer> mRecentLevels;			// 读取数据清单，读到的数据即收录
		public ArrayList<Integer> mValidLevels;				// 有效数据清单，独到的数据至于在最大值与最小值之间时才收录
		public int mHX24LCValue;							// 调整值，保存在HX24LC中的调整值，100000的单位数
		public int mInkAddedTimes;
		public ArrayList<Long> mInkAddedRecord;
		public int mLevelLowCount;
		public int mLevelHighCount;
// H.M.Wang 2023-4-1 临时增加异常值管理，当最近5分钟内大于560的次数超过5%时，报警，停止加墨；当相邻两次取值相差50点以上的次数>30%时，报警，停止加墨
		private ArrayList<Level_Record> mLevelRecords;
		private int mLastLevel;
		private int mCountGtMax;
		private int mCountGap;
		private int mCountError;
		private boolean mEnableAddInk;
// End of H.M.Wang 2023-4-1 临时增加异常值管理，当最近5分钟内大于560的次数超过5%时，报警，停止加墨；当相邻两次取值相差50点以上的次数>30%时，报警，停止加墨
// H.M.Wang 2024-8-6 增加一个判断Level测量芯片种类的函数，apk会根据不同的芯片种类，执行不同的逻辑。读取Level值也会根据不同的种类而调用不同的接口
		private int sLevelChipType;
		private int mInkMin;
		private int mInkMax;
		private int mAddInkThreshold;
// End of H.M.Wang 2024-8-6 增加一个判断Level测量芯片种类的函数，apk会根据不同的芯片种类，执行不同的逻辑。读取Level值也会根据不同的种类而调用不同的接口

		public BaginkLevel(int idx) {
			mLevelIndex = idx;
			mRecentLevels = new ArrayList<Integer>();
			mValidLevels = new ArrayList<Integer>();
			mInkAddedTimes = 0;
			mInkAddedRecord = new ArrayList<Long>();
			mLevelLowCount = 0;
			mLevelHighCount = 0;

// H.M.Wang 2023-4-1 临时增加异常值管理，当最近5分钟内大于560的次数超过5%时，报警，停止加墨；当相邻两次取值相差50点以上的次数>30%时，报警，停止加墨
			mLevelRecords = new ArrayList<Level_Record>();
			mLastLevel = -1;
			mCountGtMax = 0;
			mCountGap = 0;
			mCountError = 0;
			mEnableAddInk = false;
// End of H.M.Wang 2023-4-1 临时增加异常值管理，当最近5分钟内大于560的次数超过5%时，报警，停止加墨；当相邻两次取值相差50点以上的次数>30%时，报警，停止加墨
// H.M.Wang 2025-8-7 在A133平台中，不需要通过I2C读取墨位的值的方式获取墨位信息，而是通过SPI读取墨位信息，因此取消不必要的I2C读值相关的操作
			if(!PlatformInfo.isA133Product()) {
				ExtGpio.rfidSwitch(idx);
				try {Thread.sleep(100);} catch (Exception e) {}
				SmartCard.initLevelDirect();
				mHX24LCValue = SmartCard.readHX24LC();
// H.M.Wang 2024-8-6 增加一个判断Level测量芯片种类的函数，apk会根据不同的芯片种类，执行不同的逻辑。读取Level值也会根据不同的种类而调用不同的接口
				sLevelChipType = SmartCard.getLevelType(idx);
				Debug.d(TAG, "Level Chip Type = " + sLevelChipType + ")");
				if(LEVEL_CHIP_TYPE_MCPH21 == sLevelChipType) {
					mInkMin = VALID_INK_MIN_MCPH21;
					mInkMax = VALID_INK_MAX_MCPH21;
					mAddInkThreshold = ADD_INK_THRESHOLD_MCPH21;
				} else {
					mInkMin = VALID_INK_MIN;
					mInkMax = VALID_INK_MAX;
					mAddInkThreshold = ADD_INK_THRESHOLD;
				}
// End of H.M.Wang 2024-8-6 增加一个判断Level测量芯片种类的函数，apk会根据不同的芯片种类，执行不同的逻辑。读取Level值也会根据不同的种类而调用不同的接口
			}
// End of H.M.Wang 2025-8-7 在A133平台中，不需要通过I2C读取墨位的值的方式获取墨位信息，而是通过SPI读取墨位信息，因此取消不必要的I2C读值相关的操作
		}
	}

	private BaginkLevel mBaginkLevels[] = null;

	ExecutorService mCachedThreadPool = null;

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
				ExtGpio.rfidSwitch(mBaginkLevels[cardIdx].mLevelIndex);
				try{Thread.sleep(100);}catch(Exception e){};
				int level = SmartCard.readLevelDirect(cardIdx);
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
			mLevelReading = false;

			int avgLevel = (readCount == 0 ? 0 : (int)(readLevels / readCount));
			Debug.d(TAG, "Read Level[" + cardIdx + "] = " + avgLevel);

			if(avgLevel < mBaginkLevels[cardIdx].mInkMin) {
				mBaginkLevels[cardIdx].mLevelLowCount++;
			} else {
				mBaginkLevels[cardIdx].mLevelLowCount = 0;
			}

			if(mBaginkLevels[cardIdx].mLevelLowCount > 9) {
				ExtGpio.playClick();
				Thread.sleep(50);
				ExtGpio.playClick();
				Thread.sleep(50);
				ExtGpio.playClick();
				mCallbackHandler.obtainMessage(DataTransferThread.MESSAGE_LEVEL_ERROR, "Level " + (cardIdx+1) + " value too low, check line").sendToTarget();
			}

			if(avgLevel > mBaginkLevels[cardIdx].mInkMax) {
				mBaginkLevels[cardIdx].mLevelHighCount++;
			} else {
				mBaginkLevels[cardIdx].mLevelHighCount = 0;
			}

			if(mBaginkLevels[cardIdx].mLevelHighCount > 9) {
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
			if(avgLevel >= mBaginkLevels[cardIdx].mInkMin && avgLevel <= mBaginkLevels[cardIdx].mInkMax) {
				mBaginkLevels[cardIdx].mValidLevels.add(avgLevel);
				if(mBaginkLevels[cardIdx].mValidLevels.size() > PROC_LEVEL_NUMS) {
					mBaginkLevels[cardIdx].mValidLevels.remove(0);
				}
			}

// H.M.Wang 2023-4-1 临时增加异常值管理，当最近5分钟内大于560的次数超过5%时，报警，停止加墨；当相邻两次取值相差50点以上的次数>30%时，报警，停止加墨
			long rt = System.currentTimeMillis();

			while(mBaginkLevels[cardIdx].mLevelRecords.size() > 0) {
				Level_Record lr = mBaginkLevels[cardIdx].mLevelRecords.get(0);
				if(rt - lr.RecordedTime > 5 * 60 * 1000) {
					if (lr.Level == 0x0FFFFFFF) {
						mBaginkLevels[cardIdx].mCountError--;
					} else {
						if (lr.Level > mBaginkLevels[cardIdx].mInkMax) {
							mBaginkLevels[cardIdx].mCountGtMax--;
						}
						if(mBaginkLevels[cardIdx].mLastLevel != -1) {
							if(Math.abs(lr.Level - mBaginkLevels[cardIdx].mLastLevel) > 5000000) {
								mBaginkLevels[cardIdx].mCountGap--;
							}
						}
					}
					mBaginkLevels[cardIdx].mLastLevel = lr.Level;
					mBaginkLevels[cardIdx].mLevelRecords.remove(0);
				} else {
					break;
				}
			}

			if (avgLevel == 0x0FFFFFFF) {
				mBaginkLevels[cardIdx].mCountError++;
			} else {
				if (avgLevel > mBaginkLevels[cardIdx].mInkMax) {
					mBaginkLevels[cardIdx].mCountGtMax++;
				}
				if (mBaginkLevels[cardIdx].mLevelRecords.size() > 0) {
					if (Math.abs(avgLevel - mBaginkLevels[cardIdx].mLevelRecords.get(mBaginkLevels[cardIdx].mLevelRecords.size()-1).Level) > 5000000) {
						mBaginkLevels[cardIdx].mCountGap++;
					}
				}
			}
			mBaginkLevels[cardIdx].mLevelRecords.add(new Level_Record(rt, avgLevel));
			Debug.d(TAG, "mCountGtMax[" + cardIdx + "] = " + mBaginkLevels[cardIdx].mCountGtMax);
			Debug.d(TAG, "mCountGap[" + cardIdx + "] = " + mBaginkLevels[cardIdx].mCountGap);
			Debug.d(TAG, "mCountError[" + cardIdx + "] = " + mBaginkLevels[cardIdx].mCountError);
			Debug.d(TAG, "mLevelRecords.size[" + cardIdx + "] = " + mBaginkLevels[cardIdx].mLevelRecords.size());

			mBaginkLevels[cardIdx].mEnableAddInk = true;
			if(mBaginkLevels[cardIdx].mCountGtMax / mBaginkLevels[cardIdx].mLevelRecords.size() > 0.05f) {
				mBaginkLevels[cardIdx].mEnableAddInk = false;
			}
			if(mBaginkLevels[cardIdx].mCountGap / mBaginkLevels[cardIdx].mLevelRecords.size() > 0.3f) {
				mBaginkLevels[cardIdx].mEnableAddInk = false;
			}
			if(!mBaginkLevels[cardIdx].mEnableAddInk) {
				ExtGpio.playClick();
			}
// End of H.M.Wang 2023-4-1 临时增加异常值管理，当最近5分钟内大于560的次数超过5%时，报警，停止加墨；当相邻两次取值相差50点以上的次数>30%时，报警，停止加墨

			// Calculate average level if the count of read data bigger than PROC_LEVEL_NUMS
			Debug.d(TAG, "mValidLevels[" + cardIdx + "].size() = " + mBaginkLevels[cardIdx].mValidLevels.size());
			if(LEVEL_CHIP_TYPE_MCPH21 == mBaginkLevels[cardIdx].sLevelChipType) {
				avgLevel = mBaginkLevels[cardIdx].mInkMin;
			} else {
				avgLevel = mBaginkLevels[cardIdx].mInkMax;
			}
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
// H.M.Wang 2023-4-1 临时增加异常值管理，当最近5分钟内大于560的次数超过5%时，报警，停止加墨；当相邻两次取值相差50点以上的次数>30%时，报警，停止加墨
//			if(avgLevel <= ADD_INK_THRESHOLD) {
			boolean needAdd = false;
			if(LEVEL_CHIP_TYPE_MCPH21 == mBaginkLevels[cardIdx].sLevelChipType) {
				needAdd = (avgLevel >= mBaginkLevels[cardIdx].mAddInkThreshold);
			} else {
				needAdd = (avgLevel <= mBaginkLevels[cardIdx].mAddInkThreshold);
			}
			if(needAdd && mBaginkLevels[cardIdx].mEnableAddInk) {
// End of H.M.Wang 2023-4-1 临时增加异常值管理，当最近5分钟内大于560的次数超过5%时，报警，停止加墨；当相邻两次取值相差50点以上的次数>30%时，报警，停止加墨
				// If still less than ADD_INK_THRESHOLD after ADD_INK_TRY_LIMITS times of add-ink action, alarm.
				if(mBaginkLevels[cardIdx].mInkAddedTimes >= ADD_INK_TRY_LIMITS) {
					ExtGpio.playClick();
					Thread.sleep(50);
					ExtGpio.playClick();
					Thread.sleep(50);
					ExtGpio.playClick();
					mCallbackHandler.obtainMessage(DataTransferThread.MESSAGE_LEVEL_ERROR, "Level " + (cardIdx+1) + " might failed in adding ink").sendToTarget();
				} else if(mBaginkLevels[cardIdx].mInkAddedRecord.size() == 0 || System.currentTimeMillis() - mBaginkLevels[cardIdx].mInkAddedRecord.get(mBaginkLevels[cardIdx].mInkAddedRecord.size()-1) > 1000L*60*2) {		// 上次加墨后等待3秒再允许再次开阀
					Debug.d(TAG, "Add Ink");
					ExtGpio.setValve(cardIdx, 1);

					try{Thread.sleep(100);ExtGpio.setValve(cardIdx, 0);}catch(Exception e){
						ExtGpio.setValve(cardIdx, 0);
					};

					mBaginkLevels[cardIdx].mValidLevels.clear();

					mBaginkLevels[cardIdx].mInkAddedTimes++;
					mBaginkLevels[cardIdx].mInkAddedRecord.add(System.currentTimeMillis());
				}
			} else {
				mBaginkLevels[cardIdx].mInkAddedTimes = 0;
			}
		} catch(Exception e) {
			Debug.e(TAG, e.getMessage());
		}
		Debug.d(TAG, "---> quit readLevelValue(" + cardIdx + ")");
	}

// H.M.Wang 2025-11-5 修改函数逻辑，忽略掉参数cardIdx，因为这个卡号的轮换是随着打印一点一点换的，不是一次性的，因此或有显示延后的问题。改为一次就显示所有头的墨量
//	private void readLevelValueA133(final int cardIdx) {
	private void readLevelValueA133(final int cardIdx) {
// End of H.M.Wang 2025-11-5 修改函数逻辑，忽略掉参数cardIdx，因为这个卡号的轮换是随着打印一点一点换的，不是一次性的，因此或有显示延后的问题。改为一次就显示所有头的墨量
		Debug.d(TAG, "---> enter readLevelValueA133(" + cardIdx + ")");

		if(null == mBaginkLevels) {
			Debug.e(TAG, "---> Bagink level data null");
			return;
		}

		if(cardIdx >= mBaginkLevels.length) {
			Debug.e(TAG, "---> Index beyond valid");
			return;
		}

		try {
			int inkStatus = FpgaGpioOperation.getBagStatus();
			Debug.d(TAG, "Bag Status[" + cardIdx + "]: " + Integer.toHexString(inkStatus));
			mLevelReading = false;

			for(int i=0; i<mBaginkLevels.length; i++) {
				if((inkStatus & (0x00000001 << i)) != 0x00000000) {	// 相应的墨位被置为1，标识缺墨
					inkStatus = 1;
				} else {
					inkStatus = 0;
				}

				long rt = System.currentTimeMillis();
				if(mBaginkLevels[i].mLevelRecords.size() > 0) {
					if(rt - mBaginkLevels[i].mLevelRecords.get(mBaginkLevels[i].mLevelRecords.size()-1).RecordedTime < 5000L) return;
				}

				mBaginkLevels[i].mLevelRecords.add(new Level_Record(rt, inkStatus));
				if(mBaginkLevels[i].mLevelRecords.size() > PROC_LEVEL_NUMS) {
					mBaginkLevels[i].mLevelRecords.remove(0);
				}

				if(mBaginkLevels[i].mLevelRecords.size() >= PROC_LEVEL_NUMS) {
					float totalLevel = 0L;
					int count = 0;
					for(int j=0; j<mBaginkLevels[i].mLevelRecords.size(); j++) {
						totalLevel += 0.9f * mBaginkLevels[i].mLevelRecords.get(j).Level;
						count++;
					}
					inkStatus = (int)(totalLevel / count / 0.7f);
				}

				// Launch add ink if the level less than ADD_INK_THRESHOLD.
				if(inkStatus == 1) {
					// If still less than ADD_INK_THRESHOLD after ADD_INK_TRY_LIMITS times of add-ink action, alarm.
					if(mBaginkLevels[i].mInkAddedTimes >= ADD_INK_TRY_LIMITS) {
						ExtGpio.playClick();
						Thread.sleep(50);
						ExtGpio.playClick();
						Thread.sleep(50);
						ExtGpio.playClick();
						mCallbackHandler.obtainMessage(DataTransferThread.MESSAGE_LEVEL_ERROR, "Level " + (i+1) + " might failed in adding ink").sendToTarget();
					} else if(mBaginkLevels[i].mInkAddedRecord.size() == 0 || System.currentTimeMillis() - mBaginkLevels[i].mInkAddedRecord.get(mBaginkLevels[i].mInkAddedRecord.size()-1) > 1000L*60*1) {		// 上次加墨后等待3秒再允许再次开阀
						Debug.d(TAG, "Add Ink");
						ExtGpio.setValve(i, 1);

						try{Thread.sleep(1000);ExtGpio.setValve(i, 0);}catch(Exception e){
							ExtGpio.setValve(i, 0);
						};

						mBaginkLevels[i].mInkAddedTimes++;
						mBaginkLevels[i].mInkAddedRecord.add(System.currentTimeMillis());
						if(mBaginkLevels[i].mInkAddedRecord.size() > ADD_INK_TRY_LIMITS) mBaginkLevels[i].mInkAddedRecord.remove(0);		// 保持ADD_INK_TRY_LIMITS次的加墨记录
					}
				} else {
					mBaginkLevels[i].mInkAddedTimes = 0;
				}
			}
		} catch(Exception e) {
			Debug.e(TAG, e.getMessage());
		}
		Debug.d(TAG, "---> quit readLevelValueA133(" + cardIdx + ")");
	}

	public void setCallbackHandler(Handler callback) {
		mCallbackHandler = callback;
	}
// End of H.M.Wang 2022-10-28 BAGINK专用的墨位管理表

	public static RfidScheduler getInstance(Context ctx) {
		if (mInstance == null) {
			mInstance = new RfidScheduler(ctx);
		}
		return mInstance;

	}
	
	public RfidScheduler(Context ctx) {
		mContext = ctx;
		mRfidTasks = new ArrayList<RfidTask>();

		mManager = (RFIDManager) InkManagerFactory.inkManager(mContext);
	}

	@Override
	public void init(int heads) {
		running = false;
		if (mAfter != null) {
			mAfter.interrupt();
			mAfter = null;
		}

// H.M.Wang 2022-12-13 将2022-10-28日追加的下述操作提到load()函数调用之前，以避免ExtGpio.rfidSwitch(mLevelIndexs[i])将load函数中已经设置好的当前头改变
// H.M.Wang 2022-10-28 追加BAGINK专用的墨位检查功能，这里完成初始化
// H.M.Wang 2025-8-17 img改为标准M9版本即支持BAGINK，而是否按BAGINK处理则看P94是否为0
//		mBaginkImg = PlatformInfo.getImgUniqueCode().startsWith("BAGINK");
		mBaginkImg = (SystemConfigFile.getInstance(mContext).getParam(SystemConfigFile.INDEX_RFID_SC_SWITCH) == SystemConfigFile.PROC_TYPE_BAGINK);
// End of H.M.Wang 2025-8-17 img改为标准M9版本即支持BAGINK，而是否按BAGINK处理则看P94是否为0
		if(mBaginkImg) {
			Debug.d(TAG, "Initiate BAGINK variables.");
			if(PlatformInfo.isA133Product()) {
				mBaginkLevels = new BaginkLevel[LEVEL_NUM_A133];
				for(int i=0; i<mBaginkLevels.length; i++) {
					mBaginkLevels[i] = new BaginkLevel(0);
				}
			} else {
				mBaginkLevels = new BaginkLevel[LEVELS.length];
				for(int i=0; i<mBaginkLevels.length; i++) {
					mBaginkLevels[i] = new BaginkLevel(LEVELS[i]);

					if(LEVEL_CHIP_TYPE_MCPH21 == mBaginkLevels[i].sLevelChipType) {
						mBaginkLevels[i].mAddInkThreshold = (mManager.getFeature(0,6) + 346) * 10000;
					} else {
						mBaginkLevels[i].mAddInkThreshold = (mManager.getFeature(0,6) + 256) * 100000;
					}
					if(mBaginkLevels[i].mAddInkThreshold < mBaginkLevels[i].mInkMin || mBaginkLevels[i].mAddInkThreshold > mBaginkLevels[i].mInkMax) {
						mCallbackHandler.obtainMessage(DataTransferThread.MESSAGE_LEVEL_ERROR, "Valve threshold too low/high").sendToTarget();
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
				}
			}

			mCachedThreadPool = Executors.newCachedThreadPool();
		}
// End of H.M.Wang 2022-10-28 追加BAGINK专用的墨位检查功能，这里完成初始化
// End of H.M.Wang 2022-12-13 将2022-10-28日追加的下述操作提到load()函数调用之前，以避免ExtGpio.rfidSwitch(mLevelIndexs[i])将load函数中已经设置好的当前头改变

		removeAll();
		mCurrent = -1;
		// mManager.switchRfid(mCurrent);
		initTasks(heads);
		load();
// H.M.Wang 2022-12-13 2022-10-28追加的前述处理，原来在这个位置，所以破坏了load函数对当前头定位的设置.
	}

	private void initTasks(int heads) {
		for (int i = 0; i < heads; i++) {
			add(new RfidTask(i, mContext));
		}
	}
	
	private void add(RfidTask task) {
		if (mRfidTasks == null) {
			mRfidTasks = new ArrayList<RfidTask>();
		}
		mRfidTasks.add(task);
	}

	@Override
	public int count() {
		if (mRfidTasks == null) {
			return 0;
		}
		return mRfidTasks.size();
	}

	private void load() {
		mCurrent = 0;
		mManager.switchRfid(mCurrent);
		if (mRfidTasks.size() <= 0) {
			return;
		}
		RfidTask task = mRfidTasks.get(mCurrent);
		task.onLoad();
		/*切換鎖之後需要等待1s才能進行讀寫操作*/
		mSwitchTimeStemp = SystemClock.elapsedRealtime();
	}

	/**
	 * Rfid調度函數
	 * 	打印間隔0~100ms（每秒鐘打印 > 20次），爲高速打印，每個打印間隔只執行1步操作
	 *  打印間隔100~200ms（每秒鐘打印 > 20次），爲高速打印，每個打印間隔只執行2步操作
	 *  打印間隔200~500ms（每秒鐘打印 > 20次），爲高速打印，每個打印間隔只執行4步操作
	 *  打印間隔500~1000ms（每秒鐘打印 > 20次），爲高速打印，每個打印間隔只執行8步操作
	 */
	@Override
	public void schedule() {
		long time = SystemClock.elapsedRealtime();
		RfidTask task = null;

		// 切换任务后1秒钟静默
		if (mRfidTasks.size() <= 0 || time - mSwitchTimeStemp < RFID_SWITCH_INTERVAL) {
			return;
		}
		if (mRfidTasks.size() <= mCurrent) {
			 mCurrent = 0;
		}
		task = mRfidTasks.get(mCurrent);
		
		// 当前的Rfid更新任务如果未处于工作状态，或者更新后未满3秒钟，则退出
		if (task.isIdle() && (time - task.getLast()) < TASK_SCHEDULE_INTERVAL) {
			return;
		}

		for (int i = 0; i < DataTransferThread.getInterval(); i++) {
			task.execute();
			try {
				Thread.sleep(30);
			} catch (Exception e) {
			}
			Debug.d(TAG, "--->stat=" + task.getStat());
			if(task.getStat() >= RfidTask.STATE_SYNCED) {
				break;
			}
		}

		if (task.getStat() >= RfidTask.STATE_SYNCED) {
// H.M.Wang 2022-10-28 追加BAGINK专用的墨位检查功能，这里完成初始化
			Debug.d(TAG, "Heads: " + mRfidTasks.size() + "; Current: " + mCurrent);
			if(mBaginkImg) {
				mLevelReading = true;
				mCachedThreadPool.execute(new Runnable() {
					@Override
					public void run() {
						synchronized (RfidScheduler.this) {
// H.M.Wang 2025-8-7 在A133平台中，不需要通过I2C读取墨位的值的方式获取墨位信息，而是通过SPI读取墨位信息，因此取消不必要的I2C读值相关的操作
							if(PlatformInfo.isA133Product()) {
								readLevelValueA133(mCurrent);
								StringBuilder sb = new StringBuilder();
								for (int i = 0; i < mBaginkLevels.length; i++) {
									sb.append("Level" + (i+1) + ": ");
									if(mBaginkLevels[i].mLevelRecords.size() > 0) {
										for(int j=0; j<mBaginkLevels[i].mLevelRecords.size(); j++) {
											if(j > 0) sb.append(",");
											sb.append(mBaginkLevels[i].mLevelRecords.get(j).Level);
										}
										if(mBaginkLevels[i].mInkAddedRecord.size() > 0) {
											SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
											sb.append("\n");
											for(int j=0; j<mBaginkLevels[i].mInkAddedRecord.size(); j++) {
												if(j > 0) sb.append(",");
												sb.append("" + sdf.format(new Date(mBaginkLevels[i].mInkAddedRecord.get(j))));
											}
											if(mBaginkLevels[i].mInkAddedTimes > 1) {
												sb.append("\nFailed " + mBaginkLevels[i].mInkAddedTimes + " times");
											}
										}
										sb.append("\n\n");
									} else {
										sb.append("n/a\n\n");
									}
								}
								Debug.d(TAG, "Show Level: " + sb.toString());
								mCallbackHandler.obtainMessage(DataTransferThread.MESSAGE_SHOW_LEVEL, sb.toString()).sendToTarget();
							} else {
								readLevelValue(mCurrent);
//							if (mShowMsgCD == 0) {
//								mShowMsgCD = 10;
								StringBuilder sb = new StringBuilder();
								if(LEVEL_CHIP_TYPE_MCPH21 == mBaginkLevels[0].sLevelChipType) {
									sb.append("Thres: " + (mManager.getFeature(0,6) + 346) + "\n");
								} else {
									sb.append("Thres: " + (mManager.getFeature(0,6) + 256) + "\n");
								}
								for (int i = 0; i < mBaginkLevels.length; i++) {
									sb.append("Level" + (i+1) + "[" + mBaginkLevels[i].mHX24LCValue + "]: ");
									if(mBaginkLevels[i].mRecentLevels.size() > 0) {
										for(int j=0; j<mBaginkLevels[i].mRecentLevels.size(); j++) {
											if(j > 0) sb.append(",");
											sb.append(mBaginkLevels[i].mRecentLevels.get(j) / 100000);
										}
										if(mBaginkLevels[i].mInkAddedRecord.size() > 0) {
											SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
											sb.append("\n    Fill: ");
											for(int j=0; j<mBaginkLevels[i].mInkAddedRecord.size(); j++) {
												if(j > 0) sb.append(",");
												sb.append("" + sdf.format(new Date(mBaginkLevels[i].mInkAddedRecord.get(j))));
											}
										}
										sb.append("\n");
// H.M.Wang 2023-4-1 临时增加异常值管理，当最近5分钟内大于560的次数超过5%时，报警，停止加墨；当相邻两次取值相差50点以上的次数>30%时，报警，停止加墨
										if(!mBaginkLevels[i].mEnableAddInk) {
											sb.append("> Max :  " + mBaginkLevels[i].mCountGtMax + "/" + mBaginkLevels[i].mLevelRecords.size() +
													"(" + (1.0f * Math.round(1.0f * mBaginkLevels[i].mCountGtMax / mBaginkLevels[i].mLevelRecords.size() * 1000) / 10) + "%)\n" +
													"> Gap :  " + mBaginkLevels[i].mCountGap + "/" + mBaginkLevels[i].mLevelRecords.size() +
													"(" + (1.0f * Math.round(1.0f * mBaginkLevels[i].mCountGap / mBaginkLevels[i].mLevelRecords.size() * 1000) / 10) + "%)\n" +
													"> Err :  " + mBaginkLevels[i].mCountError + "/" + mBaginkLevels[i].mLevelRecords.size() +
													"(" + (1.0f * Math.round(1.0f * mBaginkLevels[i].mCountError / mBaginkLevels[i].mLevelRecords.size() * 1000) / 10) + "%)\n");
										}
// End of H.M.Wang 2023-4-1 临时增加异常值管理，当最近5分钟内大于560的次数超过5%时，报警，停止加墨；当相邻两次取值相差50点以上的次数>30%时，报警，停止加墨
										sb.append("\n");
									} else {
										sb.append("n/a\n\n");
									}
								}
								Debug.d(TAG, "Show Level: " + sb.toString());
								mCallbackHandler.obtainMessage(DataTransferThread.MESSAGE_SHOW_LEVEL, sb.toString()).sendToTarget();
							}
//							mShowMsgCD--;
//						}
						}
// End of H.M.Wang 2025-8-7 在A133平台中，不需要通过I2C读取墨位的值的方式获取墨位信息，而是通过SPI读取墨位信息，因此取消不必要的I2C读值相关的操作
					}
				});
				while(mLevelReading) {
					try{Thread.sleep(10);}catch(Exception e){};
				}
			}
// End of H.M.Wang 2022-10-28 追加BAGINK专用的墨位检查功能，这里完成初始化
			loadNext();
		}
	}
	
	/**
	 * 停止打印後需要把所有的鎖值同步一遍
	 */
	@Override
	public void doAfterPrint() {
		running = true;
		mAfter = new Thread(){
			@Override
			public void run() {
				mCurrent = 0;
				int last = mCurrent;
				mManager.switchRfid(mCurrent);
				Debug.e(TAG, "--->sync inklevel after print finish...");
				while(running && mCurrent < mRfidTasks.size()) {
					Debug.d(TAG, "--->mCurrent: " + mCurrent + "  size=" + mRfidTasks.size());
					try {
						if (last != mCurrent) {
							last = mCurrent;
							Thread.sleep(1000);
						} else {
							Thread.sleep(50);
						}
						
					} catch (Exception e) {
					}
					if (mCurrent >= mRfidTasks.size()) {
						break;
					}
					RfidTask task = mRfidTasks.get(mCurrent);
					if ((mCurrent == mRfidTasks.size() -1) && task.getStat() >= RfidTask.STATE_SYNCED) {
						break;
					}
					schedule();
					task.clearStat();
					Debug.d(TAG, "--->last=" + last + "  current=" + mCurrent + "  state=" + task.getStat());
					//如果是單頭信息，需要加上這個條件來判斷是否同步完成
					if (last == mCurrent && task.getStat() == RfidTask.STATE_IDLE) {
						break;
					}
				}
				Debug.e(TAG, "--->sync inklevel after print finish ok");
			}

		};
		mAfter.start();
	}
	
	/**
	 * 装入下一个要处理的任务
	 */
	private void loadNext() {
		RfidTask task;
		if (mCurrent < 0) {
			mCurrent = 0;
			mManager.switchRfid(mCurrent);
		} else {
			task = mRfidTasks.get(mCurrent);
			if (task != null) {
				task.onUnload();
			}
			if (mRfidTasks.size() <= 0) {
				return;
			}
			if (mRfidTasks.size() - 1 <= mCurrent || mCurrent < 0) {
				 mCurrent = 0;
			} else {
				mCurrent++;
			}
			Debug.e(TAG, "--->loadNext");
		}
		// ExtGpio.rfidSwitch(mCurrent);
		if (mRfidTasks.size() > 1) {
			mManager.switchRfid(mCurrent);
		}
		task = mRfidTasks.get(mCurrent);
		task.onLoad();
		/*切換鎖之後需要等待1s才能進行讀寫操作*/
		mSwitchTimeStemp = SystemClock.elapsedRealtime();
	}
	
	/**
	 * 已经处理完的任务
	 */
	private void unload(RfidTask task) {
		task.onUnload();
	}
	
	public void removeAll() {
		if (mRfidTasks == null) {
			return;
		}
		for (RfidTask task : mRfidTasks) {
			task.onUnload();
		}
		mRfidTasks.clear();
	}
}
