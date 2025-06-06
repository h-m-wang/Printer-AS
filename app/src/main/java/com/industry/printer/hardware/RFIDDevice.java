/**
 * RFID 存储分配表
 * ————————————————————————————————————————————
 * 	SECTOR    |    BLOCK    |   Description
 * ————————————————————————————————————————————
 *     4      |	     0      |    墨水总量
 * ————————————————————————————————————————————
 *     4      |	     1      |    特征值
 * ————————————————————————————————————————————
 *     4      |      2      |    墨水量
 * ————————————————————————————————————————————
 *     4      |      3      |    秘钥
 * ————————————————————————————————————————————
 *     5      |	     0      |    墨水总量备份
 * ————————————————————————————————————————————
 *     5      |      1      |    特征值备份
 * ————————————————————————————————————————————
 *     5      |      2      |    墨水量备份
 * ————————————————————————————————————————————
 *     5      |      3      |    秘钥
 * ————————————————————————————————————————————
 */

package com.industry.printer.hardware;

import java.io.FileDescriptor;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.util.ByteArrayBuffer;

import android.os.SystemClock;

import com.industry.printer.BLE.BLEDevice;
import com.industry.printer.Bluetooth.BLEServer;
import com.industry.printer.Serial.SerialPort;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.PlatformInfo;
import com.industry.printer.Rfid.RFIDAsyncTask;
import com.industry.printer.Rfid.RFIDAsyncTask.RfidCallback;

import com.industry.printer.Rfid.RFIDData;
import com.industry.printer.Rfid.EncryptionMethod;

public class RFIDDevice implements RfidCallback{

	//RFID操作 native接口
	public static native int open(String dev);
	public static native int close(int fd);
//	public static native int write(int fd, short[] buf, int len);
	public static native int write(int fd, byte[] buf, int len);
	public static native byte[] read(int fd, int len);
	public static native int setBaudrate(int fd, int rate);
	public static native byte[] calKey(byte[] uid);
	public static native FileDescriptor cnvt2FileDescriptor(int fd);

	public static Object SERIAL_LOCK = new Object();

	/************************
	 * RFID命令操作部分
	 ***********************/
	public static final String TAG = RFIDDevice.class.getSimpleName();
	
	public static volatile RFIDDevice mRfidDevice;
	//串口节点
	// public static final String SERIAL_INTERFACE = "/dev/ttyS3";
	
	/*校驗特徵值*/
	public static final int FEATURE_HIGH = 100;
	public static final int FEATURE_LOW = 1;
	
	/*墨水量上下限*/
	public static final int INK_LEVEL_MAX = 100000;
	public static final int INK_LEVEL_MIN = 0;
	
	/**
	 * UUID
	 */
	public static byte SECTOR_UUID = 0;
	public static byte BLOCK_UUID = 0;
	/**
	 * 特征值
	 */
	public static byte SECTOR_FEATURE = 0x04;
	public static byte BLOCK_FEATURE = 0x01;
	
	/**
	 * 秘钥块
	 */
	public static byte BLOCK_KEY = 0x03;
	
	/**
	 * 特征值备份
	 */
	public static byte SECTOR_COPY_FEATURE = 0x05;
	public static byte BLOCK_COPY_FEATURE = 0x01;
	
	/**
	 * 墨水量
	 */
	public static byte SECTOR_INKLEVEL = 0x04;
	public static byte BLOCK_INKLEVEL = 0x02;
	
	/**
	 * 墨水量备份
	 */
	public static byte SECTOR_COPY_INKLEVEL = 0x05;
	public static byte BLOCK_COPY_INKLEVEL = 0x02;
	
	/**
	 * 墨水总量
	 */
	public static byte SECTOR_INK_MAX = 0x04;
	public static byte BLOCK_INK_MAX = 0x00;
	
	
	//Command
	public static final byte RFID_CMD_CONNECT = 0x15;
	public static final byte RFID_CMD_AUTO_SEARCH = 0x20;
	public static final byte RFID_CMD_READ_VERIFY = 0x21;
	public static final byte RFID_CMD_WRITE_VERIFY = 0x23;
	public static final byte RFID_CMD_TYPEA = 0x3A;
	public static final byte RFID_CMD_SEARCHCARD = 0x46;
	public static final byte RFID_CMD_MIFARE_CONFLICT_PREVENTION = 0x47;
	public static final byte RFID_CMD_MIFARE_CARD_SELECT = 0x48;
	public static final byte RFID_CMD_MIFARE_KEY_VERIFICATION = 0x4A;
	public static final byte RFID_CMD_MIFARE_READ_BLOCK = 0x4B;
	public static final byte RFID_CMD_MIFARE_WRITE_BLOCK = 0x4C;
	public static final byte RFID_CMD_MIFARE_WALLET_INIT = 0x4D;
	public static final byte RFID_CMD_MIFARE_WALLET_READ = 0x4E;
	public static final byte RFID_CMD_MIFARE_WALLET_CHARGE = 0x50;
	public static final byte RFID_CMD_MIFARE_WALLET_DEBIT = 0x4F;

	// state
	public static final int STATE_RFID_UNINITED = 0;
	public static final int STATE_RFID_CONNECTED = 1;
	public static final int STATE_RFID_SERACH_OK = 2;
	public static final int STATE_RFID_AVOIDCONFLICT = 3;
	public static final int STATE_RFID_SELECTED = 4;
	
	public static final int STATE_RFID_MAX_KEY_VERFYING = 10;
	public static final int STATE_RFID_MAX_KEY_VERFIED = 11;
	public static final int STATE_RFID_MAX_READING = 12;
	public static final int STATE_RFID_MAX_READY = 13;
	
	public static final int STATE_RFID_FEATURE_KEY_VERFYING = 14;
	public static final int STATE_RFID_FEATURE_KEY_VERFYED = 15;
	public static final int STATE_RFID_FEATURE_READING = 16;
	public static final int STATE_RFID_FEATURE_READY = 17;
	
	public static final int STATE_RFID_VALUE_KEY_VERFYING = 18;
	public static final int STATE_RFID_VALUE_KEY_VERFYED = 19;
	public static final int STATE_RFID_VALUE_READING = 20;
	public static final int STATE_RFID_VALUE_READY = 21;
	public static final int STATE_RFID_VALUE_WRITING = 22;
	public static final int STATE_RFID_VALUE_SYNCED = 23;
	
	
	public static final int STATE_RFID_BACKUP_KEY_VERFYING = 24;
	public static final int STATE_RFID_BACKUP_KEY_VERFYED = 25;
	public static final int STATE_RFID_BACKUP_READING = 26;
	public static final int STATE_RFID_BACKUP_READY = 27;
	public static final int STATE_RFID_BACKUP_WRITING = 28;
	public static final int STATE_RFID_BACKUP_SYNCED = 29;
	
	public static final int STATE_RFID_UUID_KEY_VERFYING = 51;
	public static final int STATE_RFID_UUID_KEY_VERFYED = 52;
	public static final int STATE_RFID_UUID_READING = 53;
	public static final int STATE_RFID_UUID_READY = 54;
	
	private int mState;
	
	//Data
	public static byte[] RFID_DATA_CONNECT = {0x07};
	public static byte[] RFID_DATA_TYPEA = {0x41};
	public static byte[] RFID_DATA_SEARCH_MODE = {0x02};
	public static byte[] RFID_DATA_SEARCHCARD_WAKE = {0x26};
	public static byte[] RFID_DATA_SEARCHCARD_ALL = {0x52};
	public static byte[] RFID_DATA_MIFARE_CONFLICT_PREVENTION = {0x04};
// H.M.Wang 2021-11-9 暂时修改缺省密钥为FF（按厂家的规定），取消00，为什么这里设为00了不清楚
	//public static byte[] RFID_DATA_MIFARE_KEY_A = {0x60,0x00, 0x00,0x00,0x00,0x00,0x00,0x00};
	public static byte[] RFID_DATA_MIFARE_KEY_A = {(byte) 0x0ff, (byte) 0x0ff, (byte) 0x0ff, (byte) 0x0ff, (byte) 0x0ff, (byte) 0x0ff};
// End of H.M.Wang 2021-11-9 暂时修改缺省密钥为FF（按厂家的规定），取消00，为什么这里设为00了不清楚
	public static byte[] RFID_DATA_MIFARE_KEY_B = {0x61,0x00, 0x00,0x00,0x00,0x00,0x00,0x00};
	//返回值
	public static byte	RFID_RESULT_OK = 0x00;
	public static byte[] RFID_RESULT_MIFARE_S50 = {0X04, 0X00};
	public static byte[] RFID_RESULT_MIFARE_S70 = {0X02, 0X00};
	public static byte[] RFID_RESULT_UTRALIGHT = {0X44, 0X00};
	
	//默认密钥
	public static byte[] RFID_DEFAULT_KEY_A = { (byte) 0x0ff, (byte) 0x0ff, (byte) 0x0ff, (byte) 0x0ff, (byte) 0x0ff, (byte) 0x0ff};
	public static byte[] RFID_DEFAULT_KEY_B = { (byte) 0x0ff, (byte) 0x0ff, (byte) 0x0ff, (byte) 0x0ff, (byte) 0x0ff, (byte) 0x0FF};
	
	//计算得到的密钥
	public byte[] mRFIDKeyA = null;
	public byte[] mRFIDKeyB = null;
	
	// UID
	public byte[] mSN = null;
	
	// 是否支持符合命令的新模塊
	public static boolean isNewModel = false;
	
	private static List<RfidCallback> mCallbacks = new ArrayList<RFIDAsyncTask.RfidCallback>();

	// 当前墨水量
	private int mCurInkLevel = 0;
// H.M.Wang 2021-8-23 加入验证减数后是否一致的检验，意义应该不大
    private int mTepInkLevel = 0;
// End of H.M.Wang 2021-8-23 加入验证减数后是否一致的检验，意义应该不大
    private int mLastInkLevel = 0;
	public 	int mInkMax = 0;
	public boolean mReady = false;
	public boolean mValid = false;
	// 错误码定义
	public static final int RFID_ERRNO_NOERROR = 0;
	public static final int RFID_ERRNO_NOCARD = 1;
	public static final int RFID_ERRNO_SERIALNO_UNAVILABLE = 2;
	public static final int RFID_ERRNO_SELECT_FAIL = 3;
	public static final int RFID_ERRNO_KEYVERIFICATION_FAIL = 4;
	
	
	// tags 
	public static final String RFID_DATA_SEND = "RFID-SEND:";
	public static final String RFID_DATA_RECV = "RFID-RECV:";
	public static final String RFID_DATA_RSLT = "RFID-RSLT:";
	
	//串口
	public static int mFd=0;
	
	public static RFIDDevice getInstance() {
		if (mRfidDevice == null) {
			synchronized (RFIDDevice.class) {
				if (mRfidDevice == null) {
					mRfidDevice = new RFIDDevice();
				}
			}
		}
		return mRfidDevice;
	}
	
	public RFIDDevice() {
		mState = STATE_RFID_UNINITED;
		mCurInkLevel = 0;
        mTepInkLevel = 0;
        mLastInkLevel = 0;
		openDevice();
	}

	public synchronized List<RfidCallback> getCallbacks() {
		new Exception().printStackTrace();
		return mCallbacks;
	}
	/*
	 * 端口连接
	 */
	public boolean connect() {
		Debug.d(TAG, "--->RFID connect: " + PlatformInfo.getRfidDevice());
//		mFd = open(PlatformInfo.getRfidDevice());
		RFIDData data = new RFIDData(RFID_CMD_CONNECT, RFID_DATA_CONNECT);
		byte[] readin = writeCmd(data);
		return isCorrect(readin);
		// RFIDAsyncTask.execute(mFd, data, this);
	}
	/*
	 * 设置读卡器工作模式
	 */
	public boolean setType() {
		Debug.d(TAG, "--->RFID setType");
		RFIDData data = new RFIDData(RFID_CMD_TYPEA, RFID_DATA_TYPEA);
		byte[] readin = writeCmd(data);
		return isCorrect(readin);
	}
	/*
	 * 寻卡
	 */
	public boolean lookForCards(boolean blind) {
		Debug.d(TAG, "--->RFID lookForCards");
		openDevice();
		RFIDData data = new RFIDData(RFID_CMD_SEARCHCARD, RFID_DATA_SEARCHCARD_ALL);
		RFIDAsyncTask.execute(mFd, data, this);
		return true;
		/*
		if (blind) {
			writeCmd(data, true);
			return true;
		}
		byte[] readin = writeCmd(data);
		
		if (readin == null) {
			return false;
		}
		RFIDData rfidData = new RFIDData(readin, true);
		byte[] rfid = rfidData.getData();
		if (rfid == null) {
			return false;
		}
		Debug.d(TAG, "--->RFID type: 0x" + Integer.toHexString(rfid[0]));
		if ((rfid[0]&0x0FF) == 0xA9) {	// 新模塊
			isNewModel = true;
			return true;
		} else if (rfid[0] != 0 || rfid.length < 3) { // 老模塊
			return false;
		}
		if (rfid[1] == 0x04 && rfid[2] == 0x00) {
			Debug.d(TAG, "===>rfid type S50");
			return true;
		} else if (rfid[1] == 0x02 && rfid[2] == 0x00) {
			Debug.d(TAG, "===>rfid type S70");
			return true;
		} else if (rfid[1] == 0x44 && rfid[2] == 0x00) {
			Debug.d(TAG, "===>rfid type utralight");
			return true;
		} else {
			Debug.e(TAG, "===>unknow rfid type");
			return false;
		}*/
	}

	public byte[] autoSearch(RfidCallback callback) {
		Debug.e(TAG, "--->autoSearch callback");
		openDevice();
		RFIDData data = new RFIDData(RFID_CMD_AUTO_SEARCH, RFID_DATA_SEARCH_MODE);
		RFIDAsyncTask.execute(mFd, data, callback);
		return null;
	}
	/**
	 * 自動尋卡
	 * @param blind
	 * @return
	 */
	public byte[] autoSearch(boolean blind) { 
		Debug.e(TAG, "--->autoSearch");
		openDevice();
		RFIDData data = new RFIDData(RFID_CMD_AUTO_SEARCH, RFID_DATA_SEARCH_MODE);
		RFIDAsyncTask.execute(mFd, data, this);
		return null;
		/*
		if (blind) {
			writeCmd(data, true);
			return mSN;
		}
		byte[] in = null;
		in = writeCmd(data);
		RFIDData inData = new RFIDData(in, true);
		byte[] rfid = inData.getData();
		if (rfid == null || rfid[0] != 0 || rfid.length != 5) {
			Debug.e(TAG, "===>rfid data error");
			return null;
		}
		ByteBuffer buffer = ByteBuffer.wrap(rfid);
		buffer.position(1);
		byte[] serialNo = new byte[4]; 
		buffer.get(serialNo, 0, serialNo.length);
		Debug.print(RFID_DATA_RSLT, serialNo);
		return serialNo;
		*/
	}
	/*
	 * 防冲突
	 */
	public byte[] avoidConflict(boolean blind) {
		int limit = 0; 
		openDevice();
		Debug.e(TAG, "--->RFID avoidConflict");
		RFIDData data = new RFIDData(RFID_CMD_MIFARE_CONFLICT_PREVENTION, RFID_DATA_MIFARE_CONFLICT_PREVENTION);
		RFIDAsyncTask.execute(mFd, data, this);
		return null;
		/*
		if (blind) {
			writeCmd(data, true);
			return mSN;
		}
		byte[] readin = null;
		for (; readin == null && limit < 3; ) {
			readin = writeCmd(data);
			limit++;
		}
		
		RFIDData rfidData = new RFIDData(readin, true);
		byte[] rfid = rfidData.getData();
		if (rfid == null || rfid[0] != 0 || rfid.length != 5) {
			Debug.e(TAG, "===>rfid data error");
			return null;
		}
		ByteBuffer buffer = ByteBuffer.wrap(rfid);
		buffer.position(1);
		byte[] serialNo = new byte[4]; 
		buffer.get(serialNo, 0, serialNo.length);
		Debug.print(RFID_DATA_RSLT, serialNo);
		return serialNo;
		*/
	}
	/*
	 * 选卡
	 */ 
	public boolean selectCard(byte[] cardNo, boolean blind) {
		int limit = 0;
		if (cardNo == null || cardNo.length != 4) {
			Debug.e(TAG, "===>select card No is null");
			return false;
		}
		openDevice();
		Debug.e(TAG, "--->RFID selectCard");
		RFIDData data = new RFIDData(RFID_CMD_MIFARE_CARD_SELECT, cardNo);
		RFIDAsyncTask.execute(mFd, data, this);
		return true;
		/*
		if (blind) {
			writeCmd(data, true);
			return true;
		}
		byte[] readin = null;
		for (;!isCorrect(readin) && limit < 3;) {
			readin = writeCmd(data);
			limit++;
		}
		return isCorrect(readin);
		*/
	}
	
	public boolean keyVerfication(byte sector, byte block, byte[] key) {
		return keyVerfication(sector, block, key, false);
	}
	/**
	 * 密钥验证
	 * @return
	 */
	public boolean keyVerfication(byte sector, byte block, byte[] key, boolean blink) {
		boolean certify = false;
		
		Debug.e(TAG, "--->keyVerfication sector:" + sector + ", block:" +block);
		if (sector >= 16 || block >= 4) {
			Debug.e(TAG, "===>block over");
			return false;
		}
		openDevice();
		if (sector == SECTOR_INK_MAX && block == BLOCK_INK_MAX) {
			mState = STATE_RFID_MAX_KEY_VERFYING;
		} else if (sector == SECTOR_FEATURE && block == BLOCK_FEATURE) {
			mState = STATE_RFID_FEATURE_KEY_VERFYING;
		} else if (sector == SECTOR_INKLEVEL && block == BLOCK_INKLEVEL) {
			mState = STATE_RFID_VALUE_KEY_VERFYING;
		} else if (sector == SECTOR_COPY_INKLEVEL && block == BLOCK_COPY_INKLEVEL) {
			mState = STATE_RFID_BACKUP_KEY_VERFYING;
		} else if(sector == SECTOR_UUID && block == BLOCK_UUID) {
			mState = STATE_RFID_UUID_KEY_VERFYING;
		}
		
		byte blk = (byte) (sector*4 + block); 
		if (key == null || key.length != 6) {
			Debug.e(TAG, "===>invalide key");
			// init();
			return false;
		}
		// for(int i = 0; i < 3; i++) {
		byte[] keyA = {0x60,blk, key[0], key[1], key[2], key[3], key[4], key[5]};
		RFIDData data = new RFIDData(RFID_CMD_MIFARE_KEY_VERIFICATION, keyA);
		RFIDAsyncTask.execute(mFd, data, this);
		/*
		if (blink) {
			writeCmd(data, blink);
			return true;
		}
		byte[] readin = writeCmd(data, blink);
		certify = isCorrect(readin);
		*/
		return true;
	}
	
	/**
	 * Mifare one 卡写块
	 * @param block 1字节绝对块号
	 * @param content 16字节内容
	 * @return true 成功， false 失败
	 */
	public boolean writeBlock(byte sector, byte block, byte[] content) {
		Debug.d(TAG, "--->writeBlock sector:" + sector + ", block:" +block);
		if (sector >= 16 || block >= 4) {
			Debug.e(TAG, "===>block over");
			return false;
		}
		openDevice();
		byte blk = (byte) (sector*4 + block); 
		if (content == null || content.length != 16) {
			Debug.d(TAG, "block no large than 0x3f");
		}
		ByteArrayBuffer buffer = new ByteArrayBuffer(0);
		buffer.append(blk);
		buffer.append(content, 0, content == null ? 0 : content.length);
		RFIDData data = new RFIDData(RFID_CMD_MIFARE_WRITE_BLOCK, buffer.toByteArray());
		
		RFIDAsyncTask.execute(mFd, data, this);
		return true;
	}

	/**
	 * 複合指令：寫卡
	 * @param sector
	 * @param block
	 * @param key
	 * @param content
	 * @return
	 */
	public boolean writeBlock(byte sector, byte block, byte key[], byte[] content) {
		Debug.d(TAG, "--->writeBlock sector:" + sector + ", block:" +block);
		if (sector >= 16 || block >= 4) {
			Debug.e(TAG, "===>block over");
			return false;
		}
		openDevice();
		byte blk = (byte) (sector*4 + block);
		
		if (content == null || content.length != 16 || key == null || key.length != 6) {
			Debug.d(TAG, "block no large than 0x3f");
		}
		ByteArrayBuffer buffer = new ByteArrayBuffer(0);
		buffer.append(0x00);
		buffer.append(blk);
		buffer.append(key, 0, key == null ? 0 : key.length);
		buffer.append(content, 0, content == null ? 0 : content.length);
		RFIDData data = new RFIDData(RFID_CMD_WRITE_VERIFY, buffer.toByteArray());
		RFIDAsyncTask.execute(mFd, data, this);
		// writeCmd(data, true);
		return true;
	}
	
	/**
	 * Mifare one 卡读块
	 * @param sector 1字节区号
	 * @param block 1字节相对块号
	 * @return 16字节内容
	 */
	public byte[] readBlock(byte sector, byte block) {
		if (sector >= 16 || block >= 4) {
			Debug.d(TAG, "===>block over");
			return null;
		}

		if (sector == SECTOR_INK_MAX && block == BLOCK_INK_MAX) {
			mState = STATE_RFID_MAX_READING;
		} else if (sector == SECTOR_FEATURE && block == BLOCK_FEATURE) {
			mState = STATE_RFID_FEATURE_READING;
		} else if (sector == SECTOR_INKLEVEL && block == BLOCK_INKLEVEL) {
			mState = STATE_RFID_VALUE_READING;
		} else if (sector == SECTOR_COPY_INKLEVEL && block == BLOCK_COPY_INKLEVEL) {
			mState = STATE_RFID_BACKUP_READING;
		} else if (sector == SECTOR_UUID && block == BLOCK_UUID) {
			mState = STATE_RFID_UUID_READING;
		}
		openDevice();
		Debug.d(TAG, "--->readBlock sector:" + sector + ", block:" +block);
		byte blk = (byte) (sector*4 + block); 
		byte[] b = {blk};
		RFIDData data = new RFIDData(RFID_CMD_MIFARE_READ_BLOCK, b);
		RFIDAsyncTask.execute(mFd, data, this);
		/*byte[] readin = writeCmd(data);
		if (!isCorrect(readin)) {
			return null;
		}
		RFIDData rfidData = new RFIDData(readin, true);
		byte[] blockData = rfidData.getData();
		*/
		return null;
	}
	
	/**
	 * 複合指令：讀卡
	 * @param sector
	 * @param block
	 * @param key
	 * @return
	 */
	public byte[] readBlock(byte sector, byte block, byte[] key) {
		if (sector >= 16 || block >= 4) {
			Debug.d(TAG, "===>block over");
			return null;
		}
		if (sector == SECTOR_INK_MAX && block == BLOCK_INK_MAX) {
			mState = STATE_RFID_MAX_READING;
		} else if (sector == SECTOR_FEATURE && block == BLOCK_FEATURE) {
			mState = STATE_RFID_FEATURE_READING;
		} else if (sector == SECTOR_INKLEVEL && block == BLOCK_INKLEVEL) {
			mState = STATE_RFID_VALUE_READING;
		} else if (sector == SECTOR_COPY_INKLEVEL && block == BLOCK_COPY_INKLEVEL) {
			mState = STATE_RFID_BACKUP_READING;
		} else if (sector == SECTOR_UUID && block == BLOCK_UUID) {
			mState = STATE_RFID_UUID_READING;
		}
		
		Debug.d(TAG, "--->readBlock sector:" + sector + ", block:" +block);
		byte blk = (byte) (sector*4 + block);
		byte[] b = {0x00, blk, key[0], key[1], key[2], key[3], key[4], key[5]};
		RFIDData data = new RFIDData(RFID_CMD_READ_VERIFY, b);
		RFIDAsyncTask.execute(mFd, data, this);
		return null;
		/*
		byte[] readin = writeCmd(data);
		if (!isCorrect(readin)) {
			return null;
		}
		RFIDData rfidData = new RFIDData(readin, true);
		byte[] blockData = rfidData.getData();
		return blockData;
		*/
	}
	
	/*
	 * write command to RFID model
	 */
	private byte[] writeCmd(RFIDData data) {
		byte[] readin = null;

		openDevice();
if(BLEServer.BLERequiring) return null;
synchronized (RFIDDevice.SERIAL_LOCK) { // 2024-1-29添加
	Debug.print(RFID_DATA_SEND, data.mTransData);
	ExtGpio.writeGpioTestPin('I', 9, 0);

// H.M.Wang 2023-1-12 将jshortArray buf修改为jbyteArray buf，short没有意义
//		int writed = write(mFd, data.transferData(), data.getLength());
	int writed = write(mFd, data.mTransData, data.mTransData.length);

	try {
		Thread.sleep(500);
	} catch (Exception e) {
	}
	readin = read(mFd, 64);
	Debug.print(RFID_DATA_RECV, readin);
	ExtGpio.writeGpioTestPin('I', 9, 1);
}
		if (readin == null || readin.length == 0) {
			Debug.e(TAG, "===>read err");
			closeDevice();
			return null;
		}
		return readin;
	}
	
	private boolean isCorrect(byte[] value) {
		
		if (value == null || value.length <= 0 || value[0] != 0) {
			Debug.d(TAG, "===>rfid data error");
			return false;
		}
		return true;
	}
	
	public class RFIDCardType {
		public static final int TYPE_S50 = 0;
		public static final int TYPE_S70 = 1;
		public static final int TYPE_UTRALIGHT = 2;
	}
	
	@Deprecated
	public int cardInit() {
		//寻卡
		if (!lookForCards(false)) {
			return RFID_ERRNO_NOCARD;
		}
		try {
			Thread.sleep(100);
		} catch (Exception e) {
		}
		Debug.e(TAG, "--->newModel: " + isNewModel);
		if (isNewModel) { 
			mSN = autoSearch(false);
			Debug.print(TAG, mSN);
			if (mSN != null && mSN.length == 4) {
				return RFID_ERRNO_NOERROR;
			}
			return RFID_ERRNO_SERIALNO_UNAVILABLE;
		}
		//防冲突
		mSN = avoidConflict(false);
		if (mSN == null || mSN.length == 0) {
			return RFID_ERRNO_SERIALNO_UNAVILABLE;
		}
		try {
			Thread.sleep(100);
		} catch (Exception e) {
		}
		//选卡
		if (!selectCard(mSN, false)) {
			return RFID_ERRNO_SELECT_FAIL;
		}
		return RFID_ERRNO_NOERROR;
	}

	@Deprecated
	public int cardInitBlind() {
		//寻卡
		if (!lookForCards(true)) {
			return RFID_ERRNO_NOCARD;
		}
		try {
			Thread.sleep(50);
		} catch (Exception e) {
		}
		if (isNewModel) {
			autoSearch(true);
			if (mSN != null && mSN.length == 4) {
				return RFID_ERRNO_NOERROR;
			}
			return RFID_ERRNO_SERIALNO_UNAVILABLE;
		}
		//防冲突
		mSN = avoidConflict(true);
		if (mSN == null || mSN.length == 0) {
			return RFID_ERRNO_SERIALNO_UNAVILABLE;
		}
		try {
			Thread.sleep(50);
		} catch (Exception e) {
			
		}
		Debug.e(TAG, "--->selectCard");
		//选卡
		if (!selectCard(mSN, true)) {
			return RFID_ERRNO_SELECT_FAIL;
		}
		try {
			Thread.sleep(50);
		} catch (Exception e) {
		}
		return RFID_ERRNO_NOERROR;
	}
	
	/**
	 * read serial No. using default key
	 * @return 4bytes serial No.
	 */
	@Deprecated
	public void getSerialNo() {
		byte[] block = readBlock((byte)0, (byte) 0);
	}

	@Deprecated
	public boolean checkUID(byte[] uid) {
		boolean noChange = true;
		if (mSN == null || mSN.length <= 0) {
			return false;
		}
		if (uid == null || uid.length <= 0) {
			return false;
		}
		for (int i = 0; i < mSN.length; i++) {
			Debug.e(TAG, "--->mSN[" +  i + "] = " + (int)mSN[i] + ",  uid[" + i +"] = " + (int)uid[i]);
			if (mSN[i] != uid[i]) {
				noChange = false;
				break;
			}
		}
		return noChange;
	}
	/**
	 * 初始化流程：1、寻卡； 2、防冲突； 3、选卡
	 * 使用默认密钥读取uid 和block1的数据
	 * 通过相应算法得到A、B密钥
	 * @return 选卡成功返回true，失败返回false
	 */
	@Deprecated
	public synchronized int init() {
		int errno = 0;
		if (( errno = cardInit()) != RFID_ERRNO_NOERROR) {
			Debug.d(TAG, "--->errno: " + errno);
			return errno;
		}
		Debug.d(TAG, "--->errno: " + errno);
		// 读UID
		//byte[] uid = getSerialNo();
		if (mSN == null || mSN.length != 4) {
			Debug.d(TAG, "===>get uid fail");
			return RFID_ERRNO_SERIALNO_UNAVILABLE;
		}
		EncryptionMethod method = EncryptionMethod.getInstance();
		byte [] key = method.getKeyA(mSN);
		setKeyA(key);
		Debug.print(RFID_DATA_RSLT, key);
		mReady = true;
		mInkMax = getInkMax();
		Debug.e(TAG, "===>max ink: " + mInkMax);
		readFeatureCode();
		mValid = checkFeatureCode();
		
		return 0;
	}
	
	
	/**
	 * 读取墨水总量
	 * @return
	 */
	@Deprecated
	private int getInkMax() {
		if (isNewModel) {
			byte[] ink = readBlock(SECTOR_INK_MAX, BLOCK_INK_MAX, mRFIDKeyA);
			EncryptionMethod encrypt = EncryptionMethod.getInstance();
			return encrypt.decryptInkMAX(ink);
		}
		if ( !keyVerfication(SECTOR_INK_MAX, BLOCK_INK_MAX, mRFIDKeyA))
		{
			Debug.e(TAG, "--->key verfy fail,init and try once");
			// 如果秘钥校验失败则重新初始化RFID卡
			//init();
			if (!keyVerfication(SECTOR_INK_MAX, BLOCK_INK_MAX, mRFIDKeyA)) {
				return 0;
			}
			
		}
		byte[] ink = readBlock(SECTOR_INK_MAX, BLOCK_INK_MAX);
		EncryptionMethod encrypt = EncryptionMethod.getInstance();
		return encrypt.decryptInkMAX(ink);
	}
	
	/**
	 * 寿命值读取，寿命值保存在sector 4， 和 sector5  的 block 2.
	 * 寿命值采用双区备份，因此需要对读取的数据进行校验
	 */
	@Deprecated
	private int getInkLevel(boolean isBackup) {
		byte sector = 0;
		byte block = 0;
		if (isBackup) {
			sector = SECTOR_INKLEVEL;
			block = BLOCK_INKLEVEL;
		} else {
			sector = SECTOR_COPY_INKLEVEL;
			block = BLOCK_COPY_INKLEVEL;
		}
		Debug.e(TAG, "--->getInkLevel sector:" + sector + ", block:" + block);
		if (isNewModel) {
			byte[] ink = readBlock(sector, block, mRFIDKeyA);
			EncryptionMethod encryt = EncryptionMethod.getInstance();
			Debug.e(TAG, "--->ink level:" + encryt.decryptInkLevel(ink));
			return encryt.decryptInkLevel(ink);
		}
		// 只使用唯一密钥验证
		if ( !keyVerfication(sector, block, mRFIDKeyA))
		{
			Debug.e(TAG, "--->key verfy fail,init and try once");
			// 如果秘钥校验失败则重新初始化RFID卡
			//init();
			if (!keyVerfication(sector, block, mRFIDKeyA)) {
				return 0;
			}
			
		}
		byte[] ink = readBlock(sector, block);
		EncryptionMethod encryt = EncryptionMethod.getInstance();
		Debug.e(TAG, "--->ink level:" + encryt.decryptInkLevel(ink));
		return encryt.decryptInkLevel(ink);
	}
	
	public float getLocalInk() {
//		Debug.d(TAG, "===>curInk=" + mCurInkLevel);
		return mCurInkLevel;
	}

	@Deprecated
	public void setLocalInk(int level) {
		 Debug.d(TAG, "===>curInk=" + mCurInkLevel);
		mCurInkLevel = level;
	}
	
	public int getMax() {
		return mInkMax;
	}

	/**
	 * 寿命值写入
	 */
	@Deprecated
	private void setInkLevel(int level, boolean isBack) {
		byte sector = 0;
		byte block = 0;
		if (!mReady) {
			return;
		}
		if (isBack) {
			sector = SECTOR_INKLEVEL;
			block = BLOCK_INKLEVEL;
		} else {
			sector = SECTOR_COPY_INKLEVEL;
			block = BLOCK_COPY_INKLEVEL;
		}
		EncryptionMethod encryte = EncryptionMethod.getInstance();
		byte[] content = encryte.encryptInkLevel(level);
		if (content == null) {
			return ;
		}
		if (isNewModel) {
			writeBlock(sector, block, mRFIDKeyA, content);
			return;
		}
		if ( !keyVerfication(sector, block, mRFIDKeyA))
		{
			return ;
		}
		Debug.d(TAG, "--->setInkLevel sector:" + sector + ", block:" + block);
		writeBlock(sector, block, content);
	}

	public void down() {
// H.M.Wang 2024-12-11 对于一个特殊用户的特殊处理，当最大值为91234的时候，不做减锁处理，相当于锁值永远有效
		if(mInkMax == 91234) return;
// End of H.M.Wang 2024-12-11 对于一个特殊用户的特殊处理，当最大值为91234的时候，不做减锁处理，相当于锁值永远有效
		if (mCurInkLevel > 0) {
			mCurInkLevel = mCurInkLevel -1;
// H.M.Wang 2020-4-9 修改当值为212+255*n的时候，再减1
			if((mCurInkLevel+43)/255*255-43 == mCurInkLevel) {
				mCurInkLevel--;
			}
// End of H.M.Wang 2020-4-9 修改当值为212+255*n的时候，再减1

		} else if (mCurInkLevel <= 0) {
			mCurInkLevel = 0;
		}
		Debug.e(TAG, "--->ink=" + mCurInkLevel);

// H.M.Wang 2021-8-23 加入验证减数后是否一致的检验，意义应该不大
        if (mTepInkLevel > 0) {
            mTepInkLevel = mTepInkLevel -1;
// H.M.Wang 2020-4-9 修改当值为212+255*n的时候，再减1
            if((mTepInkLevel+43)/255*255-43 == mTepInkLevel) {
                mTepInkLevel--;
            }
// End of H.M.Wang 2020-4-9 修改当值为212+255*n的时候，再减1

        } else if (mTepInkLevel <= 0) {
            mTepInkLevel = 0;
        }
        Debug.e(TAG, "--->temp ink=" + mTepInkLevel);

        if(mTepInkLevel != mCurInkLevel) {
			mTepInkLevel = mLastInkLevel;
			mCurInkLevel = mLastInkLevel;
			down();
        } else {
            mLastInkLevel = mCurInkLevel;
        }
// End of H.M.Wang 2021-8-23 加入验证减数后是否一致的检验，意义应该不大
	}

	public byte[] mFeature = new byte[24];
	
	public int getFeature(int index) {
		if(mFeature == null || index >= mFeature.length) {
			return 0;
		} else {
			return (0x0FF & mFeature[index]);
		}
	}
	/**
	 * 特征码读取
	 */
	@Deprecated
	public void readFeatureCode() {
		int errno = 0;
		Debug.e(TAG, "--->RFID getFeatureCode");
		if (isNewModel) {
			mFeature = readBlock(SECTOR_FEATURE, BLOCK_FEATURE, mRFIDKeyA);
			return;
		}
		if ( !keyVerfication(SECTOR_FEATURE, BLOCK_FEATURE, mRFIDKeyA))
		{
			return ;
		}
		mFeature = readBlock(SECTOR_FEATURE, BLOCK_FEATURE);
	}
	/**
	 * 特征码读取
	 */
	public boolean checkFeatureCode() {
		int errno = 0;
		if (mFeature== null || mFeature.length<2) {
			return false;
		}
		Debug.e(TAG, "--->RFID getFeatureCode: " + mFeature[1] + ", " +mFeature[2]);
// H.M.Wang 2020-10-10 考虑有符号的情形(>128时)
//		if (mFeature[1] == FEATURE_HIGH && mFeature[2] == FEATURE_LOW) {
		if ((mFeature[1] ^ (byte)FEATURE_HIGH) == 0x00 && (mFeature[2] ^ (byte)FEATURE_LOW) == 0x00) {
// H.M.Wang 2020-10-10 考虑有符号的情形(>128时)
			return true;
		}
		return false;
	}
	
	public void setKeyA(byte[] key) {
		if (key == null || key.length != 6) {
			return ;
		}
		mRFIDKeyA = key;
		return;
	}
	
	/*
	 * 通過判斷特恆指來確定RFID卡是否準備就緒
	 */
	public boolean isReady() {
		if (mFeature == null) {
			return false;
// H.M.Wang 2020-10-10 考虑有符号的情形(>128时)
//		} else if (mFeature[1] != FEATURE_HIGH || mFeature[2] != FEATURE_LOW) {
		} else if ((mFeature[1] ^ (byte)FEATURE_HIGH) != 0x00 || (mFeature[2] ^ (byte)FEATURE_LOW) != 0x00) {
// H.M.Wang 2020-10-10 考虑有符号的情形(>128时)
			return false;
		}
		return true;
	}
	
	public void setReady(boolean ready) {
		mReady = ready;
	}
	
	public boolean getReady() {
		Debug.d(TAG, "--->mReady: " + mReady);
		return mReady;
	}

	@Deprecated
	public void setBaudrate(int rate) {
		
	}

	@Deprecated
	public void makeCard() {
		//修改秘钥
		/*
		if (!keyVerfication(SECTOR_FEATURE, BLOCK_KEY, RFID_DEFAULT_KEY_A)) {
			Debug.d(TAG, "===>makeCard key verify fail");
			return;
		}
		byte[] key = readBlock(SECTOR_FEATURE, BLOCK_KEY);
		for (int i = 0; i < 6; i++) {
			key[i] = (byte) (mRFIDKeyA[i] & 0x0ff);
			key[10+i] = (byte) (mRFIDKeyA[i] & 0x0ff);
		}
		//重写秘钥,秘钥存放在每个sector的block3
		writeBlock(SECTOR_FEATURE, BLOCK_KEY, key);
		*/
		//特征码
//		if (!keyVerfication(SECTOR_FEATURE, BLOCK_FEATURE, mRFIDKeyA)) {
//			Debug.d(TAG, "===>makeCard key verify fail");
//		}
//		byte[] block = readBlock(SECTOR_FEATURE, BLOCK_FEATURE);
//		Debug.print(block);
		Debug.d(TAG, "=============================");
		//byte[] keya = {0x11, 0x11, 0x11, 0x11, 0x11, 0x11};
		if (!keyVerfication((byte)6, (byte)0, RFID_DEFAULT_KEY_A)) {
			Debug.d(TAG, "===>makeCard key verify fail");
		}
		
		byte[] block = readBlock((byte)6, (byte)0);
		
		for(int i = 0; i < 6; i++) {
			block[i] = 0x11;
		}
		writeBlock((byte)6, (byte)0, block);
		
		block = readBlock((byte)6, (byte)0);
		
		
		//byte[] feature = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,0x0A, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f};
		//writeBlock(SECTOR_FEATURE, BLOCK_FEATURE, feature);
	}
	
	/**
	 * 拆分成4個接口，給最新的rfid寫入方案
	 */
	public boolean keyVerify(boolean isBack, boolean blind) {
		byte sector = 0;
		byte block = 0;
		if (!mReady) {
			return false;
		}
		if (!isBack) {
			sector = SECTOR_INKLEVEL;
			block = BLOCK_INKLEVEL;
		} else {
			sector = SECTOR_COPY_INKLEVEL;
			block = BLOCK_COPY_INKLEVEL;
		}
		
		if ( !keyVerfication(sector, block, mRFIDKeyA, blind))
		{
			return false;
		}
		return true;
	}

	public void writeInk(boolean isBack) {
		byte sector = 0;
		byte block = 0;
		Debug.d(TAG, "--->writeInk: " + isBack);
		if (mSN == null || mSN.length != 4) {
			onFinish(null);
			return;
		}
		if (!isBack) {
			sector = SECTOR_INKLEVEL;
			block = BLOCK_INKLEVEL;
		} else {
			sector = SECTOR_COPY_INKLEVEL;
			block = BLOCK_COPY_INKLEVEL;
		}
		if (isBack) {
			mState = STATE_RFID_BACKUP_WRITING;
		} else {
			mState = STATE_RFID_VALUE_WRITING;
		}
		EncryptionMethod encryte = EncryptionMethod.getInstance();
		Debug.d(TAG, "--->InkLevel to write = " + mCurInkLevel);
		byte[] content = encryte.encryptInkLevel(mCurInkLevel);
		if (content == null) {
			return ;
		}
		if (isNewModel) {
			writeBlock(sector, block, mRFIDKeyA, content);
		} else {
			writeBlock(sector, block, content);
		}
	}
	
	/**
	 * 檢查特徵值是否正確
	 * @return
	 */
	public boolean isValid() {
		return mValid;
	}

	@Deprecated
	private boolean isLevelValid(int value) {
		if (value < INK_LEVEL_MIN || value > INK_LEVEL_MAX) {
			return false;
		}
		return true;
	}

	private int openDevice() {
		if (mFd <= 0) {
			mFd = open(PlatformInfo.getRfidDevice());
			if (!mReady && SystemClock.uptimeMillis() < 100*1000) { // 上電後
				//1.先修改模塊的baudrate
				connect();
			}
			//2.修改本地串口的baudrate
			setBaudrate(mFd, 115200);
		}
		Debug.d(TAG, "===>mFd=" + mFd);
		return mFd;
	}
	
	private void closeDevice() {
		if (mFd > 0) {
			close(mFd);
		}
		mFd = -1;
	}
	
	/**
	 * reopen時表示RFID的波特率已經修改過，不需要在修改
	 */
	private void reopen() {
		close(mFd);
		mFd = 0;
		mFd = open(PlatformInfo.getRfidDevice());
		setBaudrate(mFd, 115200);
	}
	
	public void addLisetener(RfidCallback callback) {
		synchronized (RFIDDevice.class) {
			if (mCallbacks.contains(callback)) {
				return;
			}
			mCallbacks.add(callback);
		}
	}
	
	public void removeListener(RfidCallback callback) {
		synchronized (RFIDDevice.class) {
			mCallbacks.remove(callback);
		}
	}
	
	private void parseSearch(RFIDData data) {
		byte[] rfid = data.getData();
		if (rfid == null) {
			return;
		}
		Debug.d(TAG, "--->RFID type: 0x" + Integer.toHexString(rfid[0]));
		if ((rfid[0]&0x0FF) == 0xA9) {	// 新模塊
			isNewModel = true;
			return;
		} else if (rfid[0] != 0 || rfid.length < 3) { // 老模塊
			return;
		}
		if (rfid[1] == 0x04 && rfid[2] == 0x00) {
			Debug.d(TAG, "===>rfid type S50");
		} else if (rfid[1] == 0x02 && rfid[2] == 0x00) {
			Debug.d(TAG, "===>rfid type S70");
		} else if (rfid[1] == 0x44 && rfid[2] == 0x00) {
			Debug.d(TAG, "===>rfid type utralight");
		} else {
			Debug.e(TAG, "===>unknow rfid type");
		}
		
	}
	
	public void parseAutosearch(RFIDData data) {
		byte[] rfid = data.getData();
		if (rfid == null || rfid[0] != 0 || rfid.length != 5) {
// H.M.Wang 2022-2-16 如果读取失败，则mValid设为false，避免后续的执行
			mValid = false;
// End of H.M.Wang 2022-2-16 如果读取失败，则mValid设为false，避免后续的执行
			Debug.e(TAG, "===>rfid data error");
			return;
		}
		ByteBuffer buffer = ByteBuffer.wrap(rfid);
		buffer.position(1);
		mSN = new byte[4]; 
		buffer.get(mSN, 0, mSN.length);
		Debug.print(RFID_DATA_RSLT, mSN);
		//計算key-A
		EncryptionMethod method = EncryptionMethod.getInstance();
		byte [] key = method.getKeyA(mSN);
		setKeyA(key);
		setReady(true);
	}

	private int parseMax(RFIDData data) {
		byte[] ink = data.getData();
		if (ink == null || ink.length <= 0 || ink[0] != 0) {
			return 0;
		}
		EncryptionMethod encrypt = EncryptionMethod.getInstance();
		return encrypt.decryptInkMAX(ink);
	}

// H.M.Wang 2021-8-24 追加专门获取InkLevel的函数
	private int parseLevel(RFIDData data) {
		byte[] ink = data.getData();
		if (ink == null || ink.length <= 0 || ink[0] != 0) {
			return 0;
		}
		EncryptionMethod encrypt = EncryptionMethod.getInstance();
		return encrypt.decryptInkLevel(ink);
	}
// End of H.M.Wang 2021-8-24 追加专门获取InkLevel的函数

	private int mWriteFailTimes = 0;
	private void parseWrite(RFIDData data) {
		byte[] rfid = data.getData();
		if (isCorrect(rfid)) {
			mWriteFailTimes = 0;
		} else {
			mWriteFailTimes++;
		}
	}
	
	public int getState() {
		return mState;
	}
	
	public void setState(int state) {
		mState = state;
	}

// H.M.Wang 2022-1-13 追加写操作失败次数
	public int mWriteError = 0;
// End of H.M.Wang 2022-1-13 追加写操作失败次数

	@Override
	public void onFinish(RFIDData data) {
		byte[] rfid;
		if (data == null) {
			Debug.d(TAG, "--->onFinish: data null");
			synchronized (RFIDDevice.class) {
				for (RfidCallback callback : mCallbacks) {
					callback.onFinish(data);
				}
			}
			return;
		}
		Debug.d(TAG, "--->onFinish: 0x" + Integer.toHexString(data.getCommand()));

		for (int i = 0; i < data.mRealData.length; i++) {
			System.out.print(data.mRealData[i] + ", ");
		}

		switch (data.getCommand()) {
			case RFID_CMD_CONNECT:
				//
				setBaudrate(mFd, 115200);
				Debug.d(TAG, "--->connect rfid");
				break;
			case RFID_CMD_SEARCHCARD:
				Debug.d(TAG, "--->look card finish");
				parseSearch(data);
				mState = STATE_RFID_SERACH_OK;
				break;
			case RFID_CMD_AUTO_SEARCH:
				mState = STATE_RFID_CONNECTED;
				parseAutosearch(data);
				break;
			case RFID_CMD_MIFARE_CONFLICT_PREVENTION:
				parseAutosearch(data);
				mState = STATE_RFID_AVOIDCONFLICT;
				break;
			case RFID_CMD_MIFARE_CARD_SELECT:
				mState = STATE_RFID_SELECTED;
				break;
			case RFID_CMD_MIFARE_READ_BLOCK:
			case RFID_CMD_READ_VERIFY:
				if (mState == STATE_RFID_UUID_READING) {
					mState = STATE_RFID_UUID_READY;
					break;
				}

// H.M.Wang 2021-8-24 出现了再开机后InkLevel为100%的问题，发现原来的parseRead当中调用的是decrypteInkMax函数解密，并且使用了5个字节。由于30708版本以后，Ink值的后12字节填充了数值，因此可能导致Ink>Max的现象
// 因此通过修改parseRead为parseMax用来获取最大值，追加parseLevel，其中调用decryptInkLevel函数解密（仅用4个字节），这样就可以解决问题，同时将parseXXX函数调用分散到各个分支里面
//				int value = parseRead(data);
				if (mState == STATE_RFID_MAX_READING) {
					mInkMax = parseMax(data);
					mState = STATE_RFID_MAX_READY;
				} else if (mState == STATE_RFID_FEATURE_READING) {
					mFeature = data.getData();
//				for (int i = 0; i < mFeature.length; i++) {
//					Debug.d(TAG, "--->feature[" + i + "] = " + mFeature[i]);
//				}
					mValid = checkFeatureCode();

// H.M.Wang 2023-5-18 追加一个，当为Bagink的时候，如果特征值6的值不是64-164之间的值，则禁止打印
					if(PlatformInfo.getImgUniqueCode().startsWith("BAGINK") && (mFeature[6] < 64 || mFeature[6] >= 164)) {
						mValid = false;
					}
// End of H.M.Wang 2023-5-18 追加一个，当为Bagink的时候，如果特征值6的值不是64-164之间的值，则禁止打印

					mState = STATE_RFID_FEATURE_READY;
				} else if (mState == STATE_RFID_VALUE_READING) {
					mCurInkLevel = parseLevel(data);
					mTepInkLevel = mCurInkLevel;
					mState = STATE_RFID_VALUE_READY;
				} else if (mState == STATE_RFID_BACKUP_READING) {
					if (mCurInkLevel <= 0) {
						mCurInkLevel = parseLevel(data);
					}
					if (mTepInkLevel <= 0) {
						mTepInkLevel = parseLevel(data);
					}
					mState = STATE_RFID_BACKUP_READY;
				}
// End of H.M.Wang 2021-8-24 出现了再开机后InkLevel为100%的问题，发现原来的parseRead当中调用的是decrypteInkMax函数解密，并且使用了5个字节。由于30708版本以后，Ink值的后12字节填充了数值，因此可能导致Ink>Max的现象
				break;
			case RFID_CMD_MIFARE_KEY_VERIFICATION:
				rfid = data.getData();
				isCorrect(rfid);
				if (mState == STATE_RFID_MAX_KEY_VERFYING) {
					mState = STATE_RFID_MAX_KEY_VERFIED;
				} else if (mState == STATE_RFID_FEATURE_KEY_VERFYING) {
					mState = STATE_RFID_FEATURE_KEY_VERFYED;
				} else if (mState == STATE_RFID_VALUE_KEY_VERFYING) {
					mState = STATE_RFID_VALUE_KEY_VERFYED;
				} else if (mState == STATE_RFID_BACKUP_KEY_VERFYING) {
					mState = STATE_RFID_BACKUP_KEY_VERFYED;
				} else if (mState == STATE_RFID_UUID_KEY_VERFYING) {
					mState = STATE_RFID_UUID_KEY_VERFYED;
				}
				break;
			case RFID_CMD_MIFARE_WRITE_BLOCK:
			case RFID_CMD_WRITE_VERIFY:
				if (mState == STATE_RFID_BACKUP_WRITING) {
					mState = STATE_RFID_BACKUP_SYNCED;
				} else if (mState == STATE_RFID_VALUE_WRITING) {
// H.M.Wang 2022-1-13 追加写操作时的返回值判断，如果写操作出现异常，则记录次数
/*	暂时屏蔽				byte[] res = data.getData();
					if(null != res && res.length >= 1 && res[0] == 0) {
						Debug.d(TAG, "RFID_CMD_WRITE_VERIFY succeeded!");
						mState = STATE_RFID_VALUE_SYNCED;
						mWriteError = 0;
					} else {
						Debug.e(TAG, "RFID_CMD_WRITE_VERIFY failed! " + mWriteError);
						mState = STATE_RFID_VALUE_SYNCED;   // H.M.Wang 2022-1-23 这里如果不设STATE_RFID_VALUE_SYNCED，如果写失败会造成卡1一直尝试写而无法完成，234卡不会被写到
						mWriteError++;
					}
*/
					mState = STATE_RFID_VALUE_SYNCED;
// End of H.M.Wang 2022-1-13 追加写操作时的返回值判断，如果写操作出现异常，则记录次数
				}
				break;
			default:
				break;
		}

		synchronized (RFIDDevice.class) {
			for (RfidCallback callback : mCallbacks) {
				Debug.d(TAG, "--->callback " + mCallbacks.size());
				callback.onFinish(data);
			}
		}
	}
	
}
