package com.industry.printer.Rfid;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.industry.printer.ControlTabActivity;
import com.industry.printer.DataTransferThread;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.PlatformInfo;
import com.industry.printer.hardware.ExtGpio;
import com.industry.printer.hardware.InkManagerFactory;
import com.industry.printer.hardware.RFIDManager;
import com.industry.printer.hardware.SmartCard;
import com.industry.printer.hardware.SmartCardManager;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
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

	private int mLevelIndexs[] = {
		ExtGpio.RFID_CARD1,
		ExtGpio.RFID_CARD2,
		ExtGpio.RFID_CARD3,
		ExtGpio.RFID_CARD4,
	};
	private final static int READ_LEVEL_TIMES           = 1;        // 每次读取LEVEL值时尝试的最大次数，然后从成功的次数当中获取平均值，作为本次的读取值。如设置10次，则从下层读取10次，如成功5次，则使用成功的5次获取平均值作为本次读取的最终值
	private final static int PROC_LEVEL_NUMS            = 10;       // 对读取数据进行处理的最小次数，当达到这个数字的时候，处理是否加墨的处理
	private final static int READ_LEVEL_INTERVAL        = 10;		// 10ms
	private final static int ADD_INK_TRY_LIMITS         = 10;       // 加墨的尝试次数

	private int ADD_INK_THRESHOLD = 13800000;
	private int mPrintCount = 10;

	private ArrayList<Integer>[] mRecentLevels;
	private int[] mInkAddedTimes;

	ExecutorService mCachedThreadPool = null;

	private void readLevelValue(final int cardIdx) {
		Debug.d(TAG, "---> enter readLevelValue(" + cardIdx + ")");

		mCachedThreadPool.execute(new Runnable() {
			@Override
			public void run() {
				if(cardIdx >= mLevelIndexs.length) return;
				synchronized (RfidScheduler.this) {
					Debug.d(TAG, "---> launch readLevelValue process(" + cardIdx + ")");
					try {
						long readLevels = 0L;
						int readCount = 0;

						// Read Level READ_LEVEL_TIMES times
						for(int i=0; i<READ_LEVEL_TIMES; i++) {
							synchronized (RfidScheduler.this) {
								int level = SmartCard.readLevelDirect();
								if ((level & 0xF0000000) == 0x00000000) {
									Debug.d(TAG, "Read Level " + (readCount + 1) + " times = " + level);
									readLevels += level;
									readCount++;
								} else {
									Debug.e(TAG, "Read Level Error: " + Integer.toHexString(level));
									ExtGpio.playClick();
								}
							}
							try{Thread.sleep(READ_LEVEL_INTERVAL);}catch(Exception e){};
						}

						int avgLevel = (readCount == 0 ? 0 : (int)(readLevels / readCount));
						Debug.d(TAG, "Read Level = " + avgLevel);

						if(avgLevel >= 12000000 && avgLevel <= 16000000) {
							mRecentLevels[cardIdx].add(avgLevel);
							if(mRecentLevels[cardIdx].size() > PROC_LEVEL_NUMS) {
								mRecentLevels[cardIdx].remove(0);
							}
						}

						// Calculate average level if the count of read data bigger than PROC_LEVEL_NUMS
						avgLevel = ADD_INK_THRESHOLD;
						if(mRecentLevels[cardIdx].size() >= PROC_LEVEL_NUMS) {
							long totalLevel = 0;
							for(int i=0; i<PROC_LEVEL_NUMS; i++) {
								totalLevel += mRecentLevels[cardIdx].get(i);
							}
							avgLevel = (int)(totalLevel / PROC_LEVEL_NUMS);
						}
						Debug.d(TAG, "Average Level = " + avgLevel);

						// Launch add ink if the level less than ADD_INK_THRESHOLD.
						if(avgLevel < ADD_INK_THRESHOLD) {
							// If still less than ADD_INK_THRESHOLD after ADD_INK_TRY_LIMITS times of add-ink action, alarm.
							if(mInkAddedTimes[cardIdx] >= ADD_INK_TRY_LIMITS) {
								ExtGpio.playClick();
								Thread.sleep(50);
								ExtGpio.playClick();
								Thread.sleep(50);
								ExtGpio.playClick();
							} else {
								ExtGpio.setValve(cardIdx, 1);

								try{Thread.sleep(100);ExtGpio.setValve(cardIdx, 0);}catch(Exception e){
									ExtGpio.setValve(cardIdx, 0);
								};

/*								long startTiem = System.currentTimeMillis();
								while(true) {
									try{Thread.sleep(100);}catch(Exception e){};
									if(System.currentTimeMillis() - startTiem >= 11500) break;
								}
*/
								mInkAddedTimes[cardIdx]++;
							}
						} else {
							mInkAddedTimes[cardIdx] = 0;
						}
					} catch(Exception e) {
						Debug.e(TAG, e.getMessage());
					}
					Debug.d(TAG, "---> quit readLevelValue process " + cardIdx + ")");
				}
			}
		});

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
		removeAll();
		mCurrent = -1;
		// mManager.switchRfid(mCurrent);
		initTasks(heads);
		load();
// H.M.Wang 2022-10-28 追加BAGINK专用的墨位检查功能，这里完成初始化
		mBaginkImg = PlatformInfo.getImgUniqueCode().startsWith("BAGINK");
		if(mBaginkImg) {
			Debug.d(TAG, "Initiate BAGINK variables.");
			mRecentLevels = new ArrayList[mLevelIndexs.length];
			mInkAddedTimes = new int[mLevelIndexs.length];

			for(int i=0; i<mLevelIndexs.length; i++) {
				ExtGpio.rfidSwitch(mLevelIndexs[i]);
//				try {Thread.sleep(200);} catch (Exception e) {}
				SmartCard.initLevelDirect();
				mRecentLevels[i] = new ArrayList<Integer>();
				mInkAddedTimes[i] = 0;
			}


			if(mManager.getFeature(0,5) < 300) {
				mCallbackHandler.obtainMessage(DataTransferThread.MESSAGE_SHOW_LEVEL, "Valve threshold too low").sendToTarget();
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
			ADD_INK_THRESHOLD = mManager.getFeature(0,5) * 100000;
			mCachedThreadPool = Executors.newCachedThreadPool();
		}
// End of H.M.Wang 2022-10-28 追加BAGINK专用的墨位检查功能，这里完成初始化
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
		
		if (mRfidTasks.size() <= 0 || time - mSwitchTimeStemp < RFID_SWITCH_INTERVAL) {
			return;
		}
		if (mRfidTasks.size() <= mCurrent) {
			 mCurrent = 0;
		}
		task = mRfidTasks.get(mCurrent);
		
		// 
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
// H.M.Wang 2022-10-28 追加BAGINK专用的墨位检查功能，这里完成初始化
		if(mBaginkImg) {
			readLevelValue(mCurrent);
			if (mPrintCount == 0) {
				mPrintCount = 10;
				StringBuilder sb = new StringBuilder();
				sb.append("Thres: " + mManager.getFeature(0,5) + "\n");
				for (int i = 0; i < mLevelIndexs.length; i++) {
					sb.append("Level" + (i+1) + ": ");
					if(mRecentLevels[i].size() > 0) {
						for(int j=0; j<mRecentLevels[i].size(); j++) {
							if(j > 0) sb.append(",");
							sb.append(mRecentLevels[i].get(j) / 100000);
						}
					} else {
						sb.append("n/a");
					}
					sb.append("\n");
				}
				mCallbackHandler.obtainMessage(DataTransferThread.MESSAGE_SHOW_LEVEL, sb.toString()).sendToTarget();
			}
			mPrintCount--;
		}
// End of H.M.Wang 2022-10-28 追加BAGINK专用的墨位检查功能，这里完成初始化
		if (task.getStat() >= RfidTask.STATE_SYNCED) {
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
