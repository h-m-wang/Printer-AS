package com.industry.printer.hardware;

import com.industry.printer.FileFormat.SystemConfigFile;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.PlatformInfo;

public class Paramter {

	private static final String TAG = Paramter.class.getSimpleName();
	
	public static Paramter mInstance;
	
	public int mFPGAParam[] = new int[24];
	
	public static Paramter getInstance() {
		if (mInstance == null) {
			mInstance = new Paramter();
		}
		return mInstance;
	}
	
	public void paramTrans(int[] param, int feature4, int feature5, int heads) {

		if(param[0] <= 0) {
			param[0] = 1;
		}
		if (param[2] <= 0) {
			param[2] = 150;
		}
		if (param[8] <= 0) {
			param[8] = 1;
		}
		// S16
		mFPGAParam[15] = param[2]/150;

		// S5
		mFPGAParam[4] = 170000/(param[0]*mFPGAParam[15]);
		if (mFPGAParam[4] > 65535) {
			mFPGAParam[4] = 65535;
		} else if (mFPGAParam[4] == 9) {
		} else if (mFPGAParam[4] < 10) {
			mFPGAParam[4] = 10;
		}
		
		// S7
		mFPGAParam[6] = (int) (param[9]*25.4/(param[8] *3.14)/param[2] + 0.5);
		if (mFPGAParam[6] < 1 || mFPGAParam[6] > 200) {
			mFPGAParam[6] = 1;
		}
		// S4
		mFPGAParam[3] = param[3] * mFPGAParam[15] * 6 * mFPGAParam[4]/1000;
		if (mFPGAParam[3] <= 2) {
			mFPGAParam[3] = 3;
		} else if (mFPGAParam[3] >= 65535) {
			mFPGAParam[3] = 65534;
		}
		
		// S9
		// mFPGAParam[8] = (int) (param[3]*param[9]/(param[8]*3.14) * mFPGAParam[15]);
		mFPGAParam[8] = (int) (param[3]*param[9]/(param[8]*3.14));
		if (mFPGAParam[ 8] <= 10) {
			mFPGAParam[8] = 11;
		} else if (mFPGAParam[8] >= 65535) {
			mFPGAParam[8] = 65534;
		}
		// S1
		if (param[4] == 0 || param[4] == 1) {
			mFPGAParam[0] = 0;
		} else if (param[4] == 2) {
			mFPGAParam[0] = 1;
		}
		
		// S2
		if (param[4] == 0 && param[5] == 0) {
			mFPGAParam[1] = 4;
		} else if (param[4] == 0 && param[5] == 1) {
			mFPGAParam[1] = 3;
		} else if (param[4] != 0 && param[5] == 0) {
			mFPGAParam[1] = 2;
		} else if (param[4] != 0 && param[5] == 1) {
			mFPGAParam[1] = 1;
		}
		// S6
		mFPGAParam[5] = param[6] * mFPGAParam[15] * 6 * mFPGAParam[4]/1000;
		if (mFPGAParam[5] < 3) {
			mFPGAParam[5] = 3;
		} else if (mFPGAParam[5] > 65534) {
			mFPGAParam[5] = 65534;
		}
		
		// S8
		mFPGAParam[7] = (int) (param[6]*param[9]/(param[8]*3.14));
		if (mFPGAParam[7] < 11) {
			mFPGAParam[7] = 11;
		} else if (mFPGAParam[7] > 65534) {
			mFPGAParam[7] = 65534;
		}
		
		// S18
		if (param[7] == 0) {
			mFPGAParam[17] = mFPGAParam[17] | 0x10;
		} else if (param[7] == 1) {
			mFPGAParam[17] = mFPGAParam[17] & 0xef;
		}
		
		if (param[14] == 0) {
			mFPGAParam[17] = mFPGAParam[17] & 0xfe;
		} else if (param[14] == 1) {
			mFPGAParam[17] = mFPGAParam[17] | 0x01;
		}

		if (param[15] == 0) {
			mFPGAParam[17] = mFPGAParam[17] & 0xfd;
		} else if (param[15] == 1) {
			mFPGAParam[17] = mFPGAParam[17] | 0x02;
		}

		// S17
		Debug.d(TAG, "--->heads=" + heads + ", " + (mFPGAParam[16] & 0xe7f));
// H.M.Wang 2019-12-31 修改参数17的设置，根据系统参数38(n带m)的取值设置
		if(param[37] / 10 == 0) {
			if (heads == 1) {
				mFPGAParam[16] = mFPGAParam[16] & 0xe7f;
			} else if (heads == 2) {
				mFPGAParam[16] = mFPGAParam[16] & 0xe7f;
				mFPGAParam[16] = mFPGAParam[16] | 0x080;
			} else if (heads == 3) {
				mFPGAParam[16] = mFPGAParam[16] & 0xe7f;
				mFPGAParam[16] = mFPGAParam[16] | 0x100;
			} else if (heads == 4) {
				mFPGAParam[16] = mFPGAParam[16] & 0xe7f;
				mFPGAParam[16] = mFPGAParam[16] | 0x180;
			}
		} else if(param[37] / 10 == 1 || param[37] / 10 == 2) {
			int inh = Math.max(0, param[37] % 10 - 1);
			inh <<= 7;
			mFPGAParam[16] = mFPGAParam[16] & 0xc7f;			// 1100 0111 1111, Bit9-7为头的数量定义
			mFPGAParam[16] = mFPGAParam[16] | (0x0380 & inh);	// 把实际的头的数量设置进去
		}
// End of H.M.Wang 2019-12-31 修改参数17的设置，根据系统参数38(n带m)的取值设置
		Debug.d(TAG, "--->param[16]=" + mFPGAParam[16]);

		// S23
		if (param[22] == 0) {
			mFPGAParam[17] = mFPGAParam[17] & 0xfb;
		} else if (param[22] == 1) {
			mFPGAParam[17] = mFPGAParam[17] | 0x04;
		}
		// S24
	    if (param[23] == 0) {
			mFPGAParam[17] = mFPGAParam[17] & 0xf7;
		} else if (param[23] == 1) {
			mFPGAParam[17] = mFPGAParam[17] | 0x08;
		}
	    // S25
	    int feature = 0;
	    if (param[24] == 0) {
			feature = param[25];
		} else if (param[24] == 1) {
//			RFIDManager manager = RFIDManager.getInstance(mContext);
//			RFIDDevice device = manager.getDevice(0);
//			if (device == null || !device.isReady()) {
//				feature = 50;	//如果參數不合法就按默認值
//			} else {
			feature = feature4;
		}
	    if (feature < 0 || feature > 118) {
			feature = 118;
		}
	    mFPGAParam[18] = feature;
	    // 参数27
	    
	    //RFID特征值6
	    int info = 17;
	    // 参数28
	    if (param[26] == 0) {
	    	info = param[27];
//	    	mFPGAParam[16] = mFPGAParam[16] & 0xff8f;
//			mFPGAParam[16] = mFPGAParam[16] | ((param[27]-17) << 4);
//			Debug.d(TAG, "--->param=" + ((param[27]-17) << 4));
//			Debug.d(TAG, "--->fpgaparam=" + mFPGAParam[16]);
		} else if (param[26] == 1) {
//			RFIDManager manager = RFIDManager.getInstance(mContext);
//			RFIDDevice device = manager.getDevice(0);
//			if (device == null || !device.isReady()) {
//				info = 17;	//如果參數不合法就按默認值
//			} else {
			info = feature5;
				
		}
	    if (info > 24 || info < 17) {
			info = 17;
		}
	    mFPGAParam[16] = mFPGAParam[16] & 0xff8f;
	    mFPGAParam[16] = mFPGAParam[16] | ((info-17) << 4);
	    
	    // S20
	    mFPGAParam[19] = param[28];
	    // 参数30
	    mFPGAParam[2] = param[29];
//	    for (int i = 0; i < mFPGAParam.length; i++) {
//			Debug.e(TAG, "--->mFPGAParam[" + i + "]=" + mFPGAParam[i]);
//		}
	    mFPGAParam[13] = (int) ((param[9] * 25.4 * 128)/(param[8] * 3.14)/param[2]);
// H.M.Wang 2022-5-30 增加编码器变倍
		mFPGAParam[13] *= (param[SystemConfigFile.INDEX_WIDTH_RATIO] <= 10 ? 100 : param[SystemConfigFile.INDEX_WIDTH_RATIO]);
		mFPGAParam[13] /= 100;
// End of H.M.Wang 2022-5-30 增加编码器变倍
// H.M.Wang 2023-9-21 当img为4FIFO的时候，S15，S21，S22，S23用来向FPGA传递位移数据
		if(!PlatformInfo.getImgUniqueCode().startsWith("4FIFO")) {
			mFPGAParam[14] = (6 * param[3] + param[10]) * param[2] / 150;					// 6 * C3 / 150 * C4 + C11 * C3 / 150
			mFPGAParam[20] = (6 * param[3] + param[11]) * param[2] / 150;					// 6 * C3 / 150 * C4 + C12 * C3 / 150
			mFPGAParam[21] = (6 * param[3] + param[18]) * param[2] / 150;					// 6 * C3 / 150 * C4 + C19 * C3 / 150
			mFPGAParam[22] = (6 * param[3] + param[19]) * param[2] / 150;					// 6 * C3 / 150 * C4 + C20 * C3 / 150
		} else {
			mFPGAParam[14] = param[SystemConfigFile.INDEX_STR];
			mFPGAParam[20] = param[36];
			mFPGAParam[21] = param[33];
			mFPGAParam[22] = param[SystemConfigFile.INDEX_DOT_SIZE];
		}
// End of H.M.Wang 2023-9-21 当img为4FIFO的时候，S15，S21，S22，S23用来向FPGA传递位移数据
		mFPGAParam[23] = param[39] == 0 ? (mFPGAParam[23] & 0xFFFE) : (mFPGAParam[23] | 0x0001);

// H.M.Wang 2023-2-4 修改参数C62和参数C63
// H.M.Wang 2022-8-25 追加喷嘴加热参数项
//		mFPGAParam[23] = param[SystemConfigFile.INDEX_NOZZLE_WARMING] == 0 ? (mFPGAParam[23] & 0xFFFD) : (mFPGAParam[23] | 0x0002);
		mFPGAParam[10] = (param[SystemConfigFile.INDEX_WARM_LIMIT] & 0x07);
// 2023-7-7 恢复到原来的算式：(param[SystemConfigFile.INDEX_WARMING]/2)
// 2023-2-23 修改计算公式，改为 C63 = (param[SystemConfigFile.INDEX_WARMING] - 7)/2，但不能为负数
		mFPGAParam[10] += (((param[SystemConfigFile.INDEX_WARMING]/2) << 3) & 0x0F8);
//		mFPGAParam[10] += (((Math.max(0, param[SystemConfigFile.INDEX_WARMING] - 7)/2) << 3) & 0x0F8);
// End of 2023-2-23 修改计算公式，改为 C63 = (param[SystemConfigFile.INDEX_WARMING] - 7)/2，但不能为负数
// End of 2023-7-7 恢复到原来的算式：(param[SystemConfigFile.INDEX_WARMING]/2)
// End of H.M.Wang 2022-8-25 追加喷嘴加热参数项
// End of H.M.Wang 2023-2-4 修改参数C62和参数C63

// H.M.Wang 2022-11-30 取消2022-11-26对S24[Bit2设置的修改，改为使用S24[3:2]设置ENCDir方向，00:None; 10:Left; 11:Right
// H.M.Wang 2022-11-26 追加S24[Bit2]的设置，0:Left; 1:Right
//		mFPGAParam[23] = param[1] == 0 ? (mFPGAParam[23] & 0xFFFB) : (mFPGAParam[23] | 0x0004);
		if(param[SystemConfigFile.INDEX_ENC_DIR] == 0x01) {    // Left
			mFPGAParam[23] = (mFPGAParam[23] & 0xFFF3) | 0x0008;
		} else if(param[SystemConfigFile.INDEX_ENC_DIR] == 0x02) {    // Right
			mFPGAParam[23] = (mFPGAParam[23] & 0xFFF3) | 0x000C;
		} else {
			mFPGAParam[23] = (mFPGAParam[23] & 0xFFF3);
		}
// End of H.M.Wang 2022-11-26 追加S24[Bit2]的设置，0:Left; 1:Right
// End of H.M.Wang 2022-11-30 取消2022-11-26对S24[Bit2设置的修改，改为使用S24[3:2]设置ENCDir方向，00:None; 10:Left; 11:Right
// H.M.Wang 2023-1-5 增加一个快速打印(Fast Print)的参数。通过S24[4]下发给FPGA
		if(param[SystemConfigFile.INDEX_FAST_PRINT] == 0x01) {    // FAST PRINT
			mFPGAParam[23] = (mFPGAParam[23] & 0xFFEF) | 0x0010;
		} else {
			mFPGAParam[23] = (mFPGAParam[23] & 0xFFEF);
		}
// End of H.M.Wang 2023-1-5 增加一个快速打印(Fast Print)的参数。通过S24[4]下发给FPGA

// H.M.Wang 2022-11-24 追加参数67，ENC_FILGER(ENC滤波)。S24[15:8] = C67 * 1/2
// H.M.Wang 2022-11-30 ENC_FILGER(ENC滤波)修改为：S24[15:8] = （1204.77 - C67) / 4.77。但如果C67=[0-9]，则S24为0
//		mFPGAParam[23] = ((param[SystemConfigFile.INDEX_ENC_FILTER] / 2) << 8) | (mFPGAParam[23] & 0x00FF);
		if(param[SystemConfigFile.INDEX_ENC_FILTER] >= 0 && param[SystemConfigFile.INDEX_ENC_FILTER] <= 9) {
			mFPGAParam[23] = (mFPGAParam[23] & 0x00FF);
		} else {
			mFPGAParam[23] = ((int)((1204.77f - param[SystemConfigFile.INDEX_ENC_FILTER]) / 4.77f) << 8) | (mFPGAParam[23] & 0x00FF);
		}
// End of H.M.Wang 2022-11-30 ENC_FILGER(ENC滤波)修改为：S24[15:8] = （1204.77 - C67) / 4.77。但如果C67=[0-9]，则S24为0
// End of H.M.Wang 2022-11-24 追加参数67，ENC_FILGER(ENC滤波)。S24[15:8] = C67 * 1/2
	}
	
	public int getFPGAParam(int index) {
		if (index >= mFPGAParam.length) {
			return 0;
		}
		return mFPGAParam[index];
	}
}
