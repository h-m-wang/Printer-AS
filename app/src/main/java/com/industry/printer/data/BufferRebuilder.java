package com.industry.printer.data;

import com.industry.printer.FileFormat.SystemConfigFile;
import com.industry.printer.Utils.Debug;

/**
 * Created by hmwan on 2020/3/3.
 */

public class BufferRebuilder {
    public static final String TAG = BufferRebuilder.class.getSimpleName();

    private byte[] mByteBuffer;             // 打印数据缓存区，传入的数据为char(2个字节)，需要转化为byte(1个字节)的。转换时，char的高位低位在前，高位在后；处理完成合成char时原路重构
    private int mColNum = 0;                // 打印缓存区的列数
//    private int mBytesPerColumn = 0;        // 每列的字节数；mColNum * mBytesPerColumn === mByteBuffer.length
    private int mBlockNum = 0;              // 每列中包含的块的数量，每个块为一个处理的数据单元
//    private int mBytesPerBlock = 0;         // 每个块的字节数；mBlockNum * mBytesPerBlock === mBytesPerColumn

    public BufferRebuilder(char[] src, int colCharNum, int blockNum) {
///./...        Debug.d(TAG, "src.length = " + src.length + "; colCharNum = " + colCharNum + "; blockNum = " + blockNum);
        try {
            mColNum = src.length / colCharNum;
//            mBytesPerColumn = colCharNum * 2;
// H.M.Wang 2021-9-1 防止不能整除而产生目标空间不够大而出现OutOfBounds的异常
//            mByteBuffer = new byte[mColNum * colCharNum * 2];
            mByteBuffer = new byte[src.length * 2];
// End of H.M.Wang 2021-9-1 防止不能整除而产生目标空间不够大而出现OutOfBounds的异常

            mBlockNum = blockNum;
//            mBytesPerBlock = mBytesPerColumn / mBlockNum;

//            StringBuilder sb = new StringBuilder();
            for(int i=0; i<src.length; i++) {
                char tmp = src[i];
//                String str = "0000" + Integer.toHexString(tmp);
//                sb.append(str.substring(str.length()-4)  + " ");
                mByteBuffer[2*i] = (byte)(tmp & 0x00ff);
                mByteBuffer[2*i+1] = (byte)((tmp & 0xff00) >> 8);
            }
//            Debug.d(TAG, "[" + sb.toString() + "]");
//            Debug.d(TAG, "[" + ByteArrayUtils.toHexString(mByteBuffer) + "]");
//            Debug.d(TAG, "BufLength: " + mByteBuffer.length + "; Columns: " + mColNum + "; Bytes per Column: " + mBytesPerColumn + "; Blocks: " + mBlockNum + "; Bytes per Block: " + mBytesPerBlock);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public BufferRebuilder shift(int[] shifts) {
        try {
//            Debug.i(TAG, "shift");
            if(shifts.length != mBlockNum) {
                Debug.e(TAG, "Block number doesn't match!");
                return this;
            }

            int addedCols = 0;
            for(int s: shifts) {
//                Debug.d(TAG, "shift: " + s);
                addedCols = Math.max(s, addedCols);
            }
            if(addedCols == 0)  {
///./...                Debug.i(TAG, "No shift required!");
                return this;
            }

            int bytesPerColumn = mByteBuffer.length / mColNum;        // 每列的字节数
            int bytesPerBlock = bytesPerColumn / mBlockNum;
            int newColNum = mColNum + addedCols;
            byte[] newBuf = new byte[newColNum * bytesPerColumn];

            for(int i=0; i<mColNum; i++) {
                for(int j=0; j<mBlockNum; j++) {
                    System.arraycopy(mByteBuffer, i * bytesPerColumn + j * bytesPerBlock, newBuf, (i + shifts[j]) * bytesPerColumn + j * bytesPerBlock,  bytesPerBlock);
                }
            }

            mColNum = newColNum;
            mByteBuffer = newBuf;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public BufferRebuilder mirror(int[] mirrors) {
        try {
//            Debug.i(TAG, "mirror");
            if(mirrors.length != mBlockNum) {
                Debug.e(TAG, "Block number doesn't match!");
                return this;
            }

            boolean needed = false;
            for(int i=0; i<mirrors.length; i++) {
//                Debug.d(TAG, "mirror: " + mirrors[i]);
                if(mirrors[i] == SystemConfigFile.DIRECTION_REVERS) {
                    needed = true;
                }
            }
            if(!needed)  {
///./...                Debug.i(TAG, "No mirror required!");
                return this;
            }

            int bytesPerColumn = mByteBuffer.length / mColNum;        // 每列的字节数
            int bytesPerBlock = bytesPerColumn / mBlockNum;

            byte[] newBuf = new byte[mByteBuffer.length];

            for(int i=0; i<mColNum; i++) {
                for(int j=0; j<mBlockNum; j++) {
                    if (mirrors[j] == SystemConfigFile.DIRECTION_REVERS) {
                        System.arraycopy(mByteBuffer, i * bytesPerColumn + j * bytesPerBlock, newBuf, (mColNum-1-i) * bytesPerColumn + j * bytesPerBlock,  bytesPerBlock);
                    } else {
                        System.arraycopy(mByteBuffer, i * bytesPerColumn + j * bytesPerBlock, newBuf, i * bytesPerColumn + j * bytesPerBlock,  bytesPerBlock);
                    }
                }
            }

            mByteBuffer = newBuf;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    private byte revert(byte src) {
        byte dst = 0;
        for (int i=0; i<=6; i++) {
            if ((src & (0x01 << i)) > 0) {
                dst |= 0x01 << (6 - i);
            }
        }
        if ((src & 0x080) > 0) {
            dst |= 0x080;
        }

        return dst;
    }

    private byte[] revert(byte[] src) {
        byte[] dst = new byte[src.length];

        for (int i=0; i<src.length; i++) {
            byte tmp = 0x00;
            for(int j=0; j<8; j++) {
                if ((src[i] & (0x01 << j)) > 0) {
                    tmp |= (0x01 << (7 - j));
                }
            }
            dst[src.length-1-i] = tmp;
        }

        return dst;
    }
// H.M.Wang 2025-9-16 使用查表的方式将字节内的各位编程倒序
    private byte[] BitReverseTable256 = new byte[] {
        (byte)0x00, (byte)0x80, (byte)0x40, (byte)0xC0, (byte)0x20, (byte)0xA0, (byte)0x60, (byte)0xE0, (byte)0x10, (byte)0x90, (byte)0x50, (byte)0xD0, (byte)0x30, (byte)0xB0, (byte)0x70, (byte)0xF0,
        (byte)0x08, (byte)0x88, (byte)0x48, (byte)0xC8, (byte)0x28, (byte)0xA8, (byte)0x68, (byte)0xE8, (byte)0x18, (byte)0x98, (byte)0x58, (byte)0xD8, (byte)0x38, (byte)0xB8, (byte)0x78, (byte)0xF8,
        (byte)0x04, (byte)0x84, (byte)0x44, (byte)0xC4, (byte)0x24, (byte)0xA4, (byte)0x64, (byte)0xE4, (byte)0x14, (byte)0x94, (byte)0x54, (byte)0xD4, (byte)0x34, (byte)0xB4, (byte)0x74, (byte)0xF4,
        (byte)0x0C, (byte)0x8C, (byte)0x4C, (byte)0xCC, (byte)0x2C, (byte)0xAC, (byte)0x6C, (byte)0xEC, (byte)0x1C, (byte)0x9C, (byte)0x5C, (byte)0xDC, (byte)0x3C, (byte)0xBC, (byte)0x7C, (byte)0xFC,
        (byte)0x02, (byte)0x82, (byte)0x42, (byte)0xC2, (byte)0x22, (byte)0xA2, (byte)0x62, (byte)0xE2, (byte)0x12, (byte)0x92, (byte)0x52, (byte)0xD2, (byte)0x32, (byte)0xB2, (byte)0x72, (byte)0xF2,
        (byte)0x0A, (byte)0x8A, (byte)0x4A, (byte)0xCA, (byte)0x2A, (byte)0xAA, (byte)0x6A, (byte)0xEA, (byte)0x1A, (byte)0x9A, (byte)0x5A, (byte)0xDA, (byte)0x3A, (byte)0xBA, (byte)0x7A, (byte)0xFA,
        (byte)0x06, (byte)0x86, (byte)0x46, (byte)0xC6, (byte)0x26, (byte)0xA6, (byte)0x66, (byte)0xE6, (byte)0x16, (byte)0x96, (byte)0x56, (byte)0xD6, (byte)0x36, (byte)0xB6, (byte)0x76, (byte)0xF6,
        (byte)0x0E, (byte)0x8E, (byte)0x4E, (byte)0xCE, (byte)0x2E, (byte)0xAE, (byte)0x6E, (byte)0xEE, (byte)0x1E, (byte)0x9E, (byte)0x5E, (byte)0xDE, (byte)0x3E, (byte)0xBE, (byte)0x7E, (byte)0xFE,
        (byte)0x01, (byte)0x81, (byte)0x41, (byte)0xC1, (byte)0x21, (byte)0xA1, (byte)0x61, (byte)0xE1, (byte)0x11, (byte)0x91, (byte)0x51, (byte)0xD1, (byte)0x31, (byte)0xB1, (byte)0x71, (byte)0xF1,
        (byte)0x09, (byte)0x89, (byte)0x49, (byte)0xC9, (byte)0x29, (byte)0xA9, (byte)0x69, (byte)0xE9, (byte)0x19, (byte)0x99, (byte)0x59, (byte)0xD9, (byte)0x39, (byte)0xB9, (byte)0x79, (byte)0xF9,
        (byte)0x05, (byte)0x85, (byte)0x45, (byte)0xC5, (byte)0x25, (byte)0xA5, (byte)0x65, (byte)0xE5, (byte)0x15, (byte)0x95, (byte)0x55, (byte)0xD5, (byte)0x35, (byte)0xB5, (byte)0x75, (byte)0xF5,
        (byte)0x0D, (byte)0x8D, (byte)0x4D, (byte)0xCD, (byte)0x2D, (byte)0xAD, (byte)0x6D, (byte)0xED, (byte)0x1D, (byte)0x9D, (byte)0x5D, (byte)0xDD, (byte)0x3D, (byte)0xBD, (byte)0x7D, (byte)0xFD,
        (byte)0x03, (byte)0x83, (byte)0x43, (byte)0xC3, (byte)0x23, (byte)0xA3, (byte)0x63, (byte)0xE3, (byte)0x13, (byte)0x93, (byte)0x53, (byte)0xD3, (byte)0x33, (byte)0xB3, (byte)0x73, (byte)0xF3,
        (byte)0x0B, (byte)0x8B, (byte)0x4B, (byte)0xCB, (byte)0x2B, (byte)0xAB, (byte)0x6B, (byte)0xEB, (byte)0x1B, (byte)0x9B, (byte)0x5B, (byte)0xDB, (byte)0x3B, (byte)0xBB, (byte)0x7B, (byte)0xFB,
        (byte)0x07, (byte)0x87, (byte)0x47, (byte)0xC7, (byte)0x27, (byte)0xA7, (byte)0x67, (byte)0xE7, (byte)0x17, (byte)0x97, (byte)0x57, (byte)0xD7, (byte)0x37, (byte)0xB7, (byte)0x77, (byte)0xF7,
        (byte)0x0F, (byte)0x8F, (byte)0x4F, (byte)0xCF, (byte)0x2F, (byte)0xAF, (byte)0x6F, (byte)0xEF, (byte)0x1F, (byte)0x9F, (byte)0x5F, (byte)0xDF, (byte)0x3F, (byte)0xBF, (byte)0x7F, (byte)0xFF
    };
// End of H.M.Wang 2025-9-16 使用查表的方式将字节内的各位编程倒序

// H.M.Wang 2025-2-17 增加22mm的倒置处理，只是将字节位置倒置，字节内倒置由FPGA处理
    public BufferRebuilder reverseHp22mm(int pattern) {
        int bytesPerColumn = mByteBuffer.length / mColNum;        // 每列的字节数
        byte temp;
// H.M.Wang 2025-9-16 根据pattern的值，做不同的导致处理
        int reverseBond = bytesPerColumn;

        if((pattern & 0x20) != 0 || (pattern & 0x40) != 0) {
            reverseBond = bytesPerColumn / 2;
        }
// End of H.M.Wang 2025-9-16 根据pattern的值，做不同的导致处理

        for(int i=0; i<mColNum; i++) {
// H.M.Wang 2025-9-16 根据pattern的值，做不同的导致处理
//             for(int j=0; j<bytesPerColumn/2; j++) {
//            temp = mByteBuffer[bytesPerColumn*i+j];
//            mByteBuffer[bytesPerColumn*i+j] = mByteBuffer[bytesPerColumn*(i+1)-1-j];
//            mByteBuffer[bytesPerColumn*(i+1)-1-j] = temp;
            for(int j=0; j<reverseBond/2; j++) {
                if((pattern & 0x10) != 0x00) {
                    temp = mByteBuffer[bytesPerColumn*i+j];
                    mByteBuffer[bytesPerColumn*i+j] = BitReverseTable256[mByteBuffer[bytesPerColumn*(i+1)-1-j]&0x0FF];
                    mByteBuffer[bytesPerColumn*(i+1)-1-j] = BitReverseTable256[temp&0x0FF];
                }
                if((pattern & 0x20) != 0x00) {
                    temp = mByteBuffer[bytesPerColumn*i+j];
                    mByteBuffer[bytesPerColumn*i+j] = BitReverseTable256[mByteBuffer[bytesPerColumn*i+reverseBond-1-j]&0x0FF];
                    mByteBuffer[bytesPerColumn*i+reverseBond-1-j] = BitReverseTable256[temp&0x0FF];
                }
                if((pattern & 0x40) != 0x00) {
                    temp = mByteBuffer[bytesPerColumn*i+j+reverseBond];
                    mByteBuffer[bytesPerColumn*i+j+reverseBond] = BitReverseTable256[mByteBuffer[bytesPerColumn*(i+1)-1-j]&0x0FF];
                    mByteBuffer[bytesPerColumn*(i+1)-1-j] = BitReverseTable256[temp&0x0FF];
                }
// End of H.M.Wang 2025-9-16 根据pattern的值，做不同的导致处理
            }
        }
        return this;
    }
// End of H.M.Wang 2025-2-17 增加22mm的倒置处理，只是将字节位置倒置，字节内倒置由FPGA处理

    public BufferRebuilder reverse(int pattern) {
// H.M.Wang 2025-2-17 增加22mm的倒置处理，只是将字节位置倒置，字节内倒置由FPGA处理
        if((pattern & 0xf0) != 0x00) {
            return reverseHp22mm(pattern);
        }
// End of H.M.Wang 2025-2-17 增加22mm的倒置处理，只是将字节位置倒置，字节内倒置由FPGA处理

        try {
//            Debug.i(TAG, "reverse pattern: " + pattern);
            if ((pattern & 0x0f) == 0x00) {
                return this;
            }

            int bytesPerColumn = mByteBuffer.length / mColNum;        // 每列的字节数

// H.M.Wang 2022-9-1 取消每列必须是4个字节的限制，改为必须是4的倍数
//            if (bytesPerColumn != 4) {
            if ((bytesPerColumn % 4) != 0) {
                Debug.e(TAG, "Not proper bytes of column!");
                return this;
            }
// End of H.M.Wang 2022-9-1 取消每列必须是4个字节的限制，改为必须是4的倍数

            byte[] newBuf = new byte[mByteBuffer.length];

            for(int i=0; i<mColNum; i++) {
// H.M.Wang 2022-9-1 取消每列必须是4个字节的限制，改为必须是4的倍数
                System.arraycopy(mByteBuffer, i * bytesPerColumn, newBuf, i * bytesPerColumn,  bytesPerColumn);
// End of H.M.Wang 2022-9-1 取消每列必须是4个字节的限制，改为必须是4的倍数
                if (pattern == 0x0f) {
                    // 4头整体反转
                    byte[] tmp = new byte[bytesPerColumn];
                    System.arraycopy(mByteBuffer, i * bytesPerColumn, tmp, 0,  bytesPerColumn);
                    System.arraycopy(revert(tmp), 0, newBuf, i * bytesPerColumn,  bytesPerColumn);
                } else {
                    // 1-2反转
                    if ((pattern & 0x03) == 0x03) {
                        byte[] tmp = new byte[bytesPerColumn/2];
                        System.arraycopy(mByteBuffer, i * bytesPerColumn, tmp, 0,  bytesPerColumn/2);
                        System.arraycopy(revert(tmp), 0, newBuf, i * bytesPerColumn,  bytesPerColumn/2);
                    } else if ((pattern & 0x03) == 0x01) {		//仅1头反转
// H.M.Wang 2022-9-1 取消每列必须是4个字节的限制，改为必须是4的倍数
//                        newBuf[i * bytesPerColumn] = revert(mByteBuffer[i * bytesPerColumn]);
//                        newBuf[i * bytesPerColumn + 1] = mByteBuffer[i * bytesPerColumn + 1];
                        byte[] tmp = new byte[bytesPerColumn/4];
                        System.arraycopy(mByteBuffer, i * bytesPerColumn, tmp, 0,  bytesPerColumn/4);
                        System.arraycopy(revert(tmp), 0, newBuf, i * bytesPerColumn,  bytesPerColumn/4);
// End of H.M.Wang 2022-9-1 取消每列必须是4个字节的限制，改为必须是4的倍数
                    } else if ((pattern & 0x03) == 0x02) {		//仅2头反转
// H.M.Wang 2022-9-1 取消每列必须是4个字节的限制，改为必须是4的倍数
//                        newBuf[i * bytesPerColumn] = mByteBuffer[i * bytesPerColumn];
//                        newBuf[i * bytesPerColumn + 1] = revert(mByteBuffer[i * bytesPerColumn + 1]);
                        byte[] tmp = new byte[bytesPerColumn/4];
                        System.arraycopy(mByteBuffer, i * bytesPerColumn + bytesPerColumn / 4, tmp, 0,  bytesPerColumn/4);
                        System.arraycopy(revert(tmp), 0, newBuf, i * bytesPerColumn + bytesPerColumn / 4,  bytesPerColumn/4);
//                    } else {
//                        System.arraycopy(mByteBuffer, i * bytesPerColumn, newBuf, i * bytesPerColumn,  bytesPerColumn/2);
// End of H.M.Wang 2022-9-1 取消每列必须是4个字节的限制，改为必须是4的倍数
                    }
                    // 3-4反转
                    if ((pattern & 0x0C) == 0x0C) {
                        byte[] tmp = new byte[bytesPerColumn/2];
                        System.arraycopy(mByteBuffer, i * bytesPerColumn + bytesPerColumn/2, tmp, 0,  bytesPerColumn/2);
                        System.arraycopy(revert(tmp), 0, newBuf, i * bytesPerColumn + bytesPerColumn/2,  bytesPerColumn/2);
                    } else if ((pattern & 0x0C) == 0x04) {		//仅3头反转
// H.M.Wang 2022-9-1 取消每列必须是4个字节的限制，改为必须是4的倍数
//                        newBuf[i * bytesPerColumn + 2] = revert(mByteBuffer[i * bytesPerColumn + 2]);
//                        newBuf[i * bytesPerColumn + 3] = mByteBuffer[i * bytesPerColumn + 3];
                        byte[] tmp = new byte[bytesPerColumn/4];
                        System.arraycopy(mByteBuffer, i * bytesPerColumn + bytesPerColumn/2, tmp, 0,  bytesPerColumn/4);
                        System.arraycopy(revert(tmp), 0, newBuf, i * bytesPerColumn + bytesPerColumn/2,  bytesPerColumn/4);
// End of H.M.Wang 2022-9-1 取消每列必须是4个字节的限制，改为必须是4的倍数
                    } else if ((pattern & 0x0C) == 0x08) {		//仅4头反转
// H.M.Wang 2022-9-1 取消每列必须是4个字节的限制，改为必须是4的倍数
//                        newBuf[i * bytesPerColumn + 2] = mByteBuffer[i * bytesPerColumn + 2];
//                        newBuf[i * bytesPerColumn + 3] = revert(mByteBuffer[i * bytesPerColumn + 3]);
                        byte[] tmp = new byte[bytesPerColumn/4];
                        System.arraycopy(mByteBuffer, i * bytesPerColumn + bytesPerColumn*3/4, tmp, 0,  bytesPerColumn/4);
                        System.arraycopy(revert(tmp), 0, newBuf, i * bytesPerColumn + bytesPerColumn*3/4,  bytesPerColumn/4);
//                    } else {
//                        System.arraycopy(mByteBuffer, i * bytesPerColumn + bytesPerColumn/2, newBuf, i * bytesPerColumn + bytesPerColumn/2,  bytesPerColumn/2);
// End of H.M.Wang 2022-9-1 取消每列必须是4个字节的限制，改为必须是4的倍数
                    }
                }
            }

            mByteBuffer = newBuf;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return this;
    }

    public BufferRebuilder clear_then_revert(int clearHead, boolean revert) {
        try {
            if(clearHead == 0x0f && !revert)  {
                return this;
            }

            int bytesPerColumn = mByteBuffer.length / mColNum;        // 每列的字节数
            int bytesPerBlock = bytesPerColumn / mBlockNum;
            byte[] zero = new byte[bytesPerBlock];
            byte[] revertBuf = new byte[bytesPerBlock];
            for(int k=0; k<bytesPerBlock; k++) {
                zero[k] = 0x00;
            }

            for(int i=0; i<mColNum; i++) {
                for(int j=0; j<mBlockNum; j++) {
                    if (((0x01 << j) & clearHead) == 0x00) {
                        System.arraycopy(zero, 0, mByteBuffer, i * bytesPerColumn + j * bytesPerBlock,  bytesPerBlock);
                    } else {
                        if(revert) {
                            System.arraycopy(mByteBuffer, i * bytesPerColumn + j * bytesPerBlock, revertBuf, 0, bytesPerBlock);
                            System.arraycopy(revert(revertBuf), 0, mByteBuffer, i * bytesPerColumn + j * bytesPerBlock,  bytesPerBlock);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public char[] getCharBuffer() {
        char[] dstBuf = new char[mByteBuffer.length/2];

        for(int i=0; i<mByteBuffer.length; i+=2) {
            dstBuf[i/2] = (char)(((mByteBuffer[i+1] << 8) & 0xff00) | (mByteBuffer[i] & 0x00ff));
        }

        return dstBuf;
    }

    public int getColumnNum() {
        return mColNum;
    }
}
