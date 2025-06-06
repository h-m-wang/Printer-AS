#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <termios.h>
#include <errno.h>
#include <string.h>
#include <jni.h>
//#include <utils/Log.h>
#include <com_industry_printer_HardwareJni.h>

#define JNI_TAG "RFID_jni"

#define ALOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,JNI_TAG,__VA_ARGS__)

static int speed_arr[]={ B921600,B460800,B230400, B115200, B38400, B19200, B9600, B4800, B2400, B1200, B300,
    B38400, B19200, B9600, B4800, B2400, B1200, B300,};
static int name_arr[]={921600,460800,230400,115200, 38400, 19200, 9600, 4800, 2400, 1200, 300, 38400,
    19200, 9600, 4800, 2400, 1200, 300,};

int set_options(int fd, int databits, int stopbits, int parity)
{
    struct termios opt;
    if(fd <= 0)
    	return 0;
    if(tcgetattr(fd, &opt) != 0)
    {
        ALOGD("SetupSerial 1\n");
        return -1;
    }
    opt.c_cflag &= ~CSIZE;
    opt.c_lflag &= ~(ICANON|ECHO|ECHOE|ISIG);
    opt.c_oflag &= ~OPOST;

    switch(databits)
    {
        case 7: opt.c_cflag |= CS7; break;
        case 8: opt.c_cflag |= CS8; break;
        default: fprintf(stderr, "Unsupported data size\n");
             return -1;
    }
    switch(parity)
    {
        case 'n':
        case 'N': opt.c_cflag &= ~PARENB;
              opt.c_iflag &= ~INPCK;
              break;
        case 'o':
        case 'O': opt.c_cflag |= (PARODD|PARENB);
              opt.c_iflag |= INPCK;
              break;
        case 'e':
        case 'E': opt.c_cflag |= PARENB;
              opt.c_cflag &= ~PARODD;
              opt.c_iflag |= INPCK;
              break;
        case 's':
        case 'S': opt.c_cflag &= ~PARENB;
              opt.c_cflag &= ~CSTOPB;
              break;
        default: fprintf(stderr, "Unsupported parity\n");
             return -1;

    }
    switch(stopbits)
    {
        case 1: opt.c_cflag &= ~CSTOPB;
                           break;
        case 2: opt.c_cflag |= CSTOPB;
            break;
        default: fprintf(stderr,"Unsupported stop bits\n");
             return -1;
    }

    if (parity != 'n')  opt.c_iflag |= INPCK;
//    tcflush(fd,TCIFLUSH);
    opt.c_cc[VTIME] = 1500; /*ds*/
    opt.c_cc[VMIN] = 8;

    //
    opt.c_iflag &= ~(IXON | IXOFF | IXANY);
    // 处理无法接收特殊字符的问题
    opt.c_iflag &= ~(BRKINT | ISTRIP);
    opt.c_iflag &= ~(INLCR | ICRNL | IGNCR);

    opt.c_oflag &= ~(ONLCR | OCRNL);
    //opt.c_oflag &= ~OPOST;
    opt.c_cflag |= CLOCAL | CREAD;
    //opt.c_lflag &= ~(ICANON | ECHO | ECHOE | ISIG);
    //tcsetattr(fd,TCSAFLUSH,&opt);
    cfmakeraw(&opt);

    tcflush(fd, TCIOFLUSH);
    if (tcsetattr(fd,TCSANOW,&opt) != 0)
    {
        ALOGD("SetupSerial 3\n");
        return -1;
    }
     return 0;
}

/*
 * Class:     com_industry_printer_UsbSerial
 * Method:    setBaudrate
 * Signature: ()I
 */
JNIEXPORT jint Java_com_industry_printer_RFID_setBaudrate(JNIEnv *env, jclass arg, int fd, int speed)
{
	int i;
	int status;
	struct termios Opt;
	tcgetattr(fd, &Opt);
	//__android_log_print(ANDROID_LOG_INFO,JNI_TAG, "setBaudrate: ===>setBaudrate\n");
	for(i=0; i<sizeof(speed_arr)/sizeof(int); i++)
	{
	    if(speed == name_arr[i])
	    {
	        tcflush(fd, TCIOFLUSH);
	        cfsetispeed(&Opt, speed_arr[i]);
	        cfsetospeed(&Opt, speed_arr[i]);
	        status = tcsetattr(fd, TCSANOW, &Opt);
	        if(status != 0)
	            ALOGD("tcsetattr fd1\n");
	        tcgetattr(fd, &Opt);

	        return 0;
	    }
	    tcflush(fd, TCIOFLUSH);
	}
	return 0;
}

// H.M.Wang 2024-7-19 增加该函数，目的是将正常打开的文件号转化为FileDescriptor，用在通过流访问串口（有多处相应修改，未一一添加注释）
/*
 * Class:     Java_com_industry_printer_RFID_cnvt2FileDescriptor
 * Method:    cnvt2FileDescriptor
 * Signature: (I)Ljava/io/FileDescriptor;
 */
JNIEXPORT jobject Java_com_industry_printer_RFID_cnvt2FileDescriptor(JNIEnv *env, jclass arg, int fd)
{
    jobject fileDescriptor;

    jclass cFileDescriptor = (*env)->FindClass(env, "java/io/FileDescriptor");
    jmethodID iFileDescriptor = (*env)->GetMethodID(env, cFileDescriptor, "<init>", "()V");
    jfieldID descriptorID = (*env)->GetFieldID(env, cFileDescriptor, "descriptor", "I");
    fileDescriptor = (*env)->NewObject(env, cFileDescriptor, iFileDescriptor);
    (*env)->SetIntField(env, fileDescriptor, descriptorID, (jint)fd);

    return fileDescriptor;
}
// End of H.M.Wang 2024-7-19 增加该函数，目的是将正常打开的文件号转化为FileDescriptor，用在通过流访问串口

JNIEXPORT jint JNICALL Java_com_industry_printer_RFID_open
  (JNIEnv *env, jclass arg, jstring dev)
{
	int ret;
	char *dev_utf = (*env)->GetStringUTFChars(env, dev, JNI_FALSE);
	ret = open(dev_utf, O_RDWR|O_NOCTTY|O_NONBLOCK |O_NDELAY);

	if( ret == -1)
	{
		ALOGD("can not open Serial port");
	}
	else
	{
		ALOGD("Serial open success");
	}
	/*
	 *RFID串口设置： 数据长度：8bits；起始位：1bit；奇偶校验：无； 停止位：1bit
	*/
	set_options(ret, 8, 1, 'n');
	/*
	 * RFID串口波特率 19200
	 */
	Java_com_industry_printer_RFID_setBaudrate(env, arg, ret, 19200);
	(*env)->ReleaseStringUTFChars(env, dev, dev_utf);
	return ret;
}


/*
 * Class:     com_industry_printer_RFID
 * Method:    write
 * Signature: (Ljava/lang/String;)I
 */
// H.M.Wang 2023-1-12 将jshortArray buf修改为jbyteArray buf，short没有意义
JNIEXPORT jint JNICALL Java_com_industry_printer_RFID_write
//  (JNIEnv *env, jclass arg, jint fd, jshortArray buf, jint len)
  (JNIEnv *env, jclass arg, jint fd, jbyteArray buf, jint len)
{
	int i,ret;
	char tempBuff[256];
	jbyte *buf_utf = (*env)->GetByteArrayElements(env, buf, NULL);

//	ALOGD("RFID-WRITE: [%02X %02X %02X %02X %02X %02X]", buf_utf[0], buf_utf[1], buf_utf[2], buf_utf[3], buf_utf[4], buf_utf[5]);

	if(fd <= 0)
		return 0;

	tcflush(fd, TCIFLUSH);
	ret = write(fd, buf_utf, len);

	/**/
//	ret = read(fd, tempBuff, 64);
//	if(ret<=0)
//		{
//			ALOGD("********line180 read ret=%d,error=%d\n",ret, errno);
//			return NULL;
//		}
	(*env)->ReleaseByteArrayElements(env, buf, buf_utf, 0);
	return ret;
}


/*
 * Class:     com_industry_printer_UsbSerial
 * Method:    read
 * Signature: ()I
 */
JNIEXPORT jbyteArray JNICALL Java_com_industry_printer_RFID_read
  (JNIEnv *env, jclass arg, jint fd, jint len)
{
	int i;
	int ret=0,nread=0;
	int repeat=0;
	int nfds=0;
	fd_set readfds;
	char tempBuff[256];
	//char response[];
	jbyteArray jResp=NULL;
	struct timeval tv;

//	struct termios Opt;
//	tcgetattr(fd, &Opt);
//	Opt.c_cc[VMIN] = len;
//	Opt.c_cc[VTIME] = 1000;
//	tcsetattr(fd, TCSANOW, &Opt);
	bzero(tempBuff, sizeof(tempBuff));
	if(fd <= 0)
		return NULL;
//	ALOGD("remove tcflush\n");
	tv.tv_sec = 1;
	tv.tv_usec = 0;
	FD_ZERO(&readfds);
	FD_SET(fd, &readfds);
	for(;;) {
		nfds = select(fd+1, &readfds, NULL, NULL, &tv);
		if(nfds <= 0) {
			ALOGD("--->select timeout, nfds=%d\n", nfds);
			return NULL;
		} else if (nfds == 0) {
			continue;
		}
// H.M.Wang 2021-8-16 修改有些数值读取错误的问题
//		ret = read(fd, tempBuff, len);
		ret = read(fd, tempBuff + nread, len - nread);
// End of H.M.Wang 2021-8-16 修改有些数值读取错误的问题
		if(ret < 0) {
			return NULL;
		}
		nread += ret;

// H.M.Wang 2021-8-16 修改有些数值读取错误的问题
//		if(nread > 2 && tempBuff[nread-1] == 0x03 && tempBuff[nread-2] != 0x10) {
//			break;
//		}
		if(nread > 3 && tempBuff[nread-1] == 0x03) {	// 遇到结尾符号
			if(tempBuff[nread-2] != 0x10) {				// 结尾符号前面不是0x10，证明没有转义，确实是结尾符
				break;
			}
			if(tempBuff[nread-2] == 0x10 && tempBuff[nread-3] == 0x10) {	// 结尾符号前面是0x10，不知道是不是0x03转义，如果在前面是0x10，则断定是0x03前面的0x10的转义，而非0x03的转义
				break;
			}
		}
// End of H.M.Wang 2021-8-16 修改有些数值读取错误的问题
	/*
	while((nread = read(fd, tempBuff, len))<=0 && repeat<10)
	{
		usleep(20000);
		repeat++;
	}
	 */

	}
	if(nread<=0)
	{
		ALOGD("********read ret=%d,error=%d\n",nread, errno);
		return NULL;
	}
	/*
	for (i = 0; i< nread; i++) {
		ALOGD("********read ret=0x%x\n",tempBuff[i]);
	}*/
    tempBuff[nread+1] = '\0';

	jResp = (*env)->NewByteArray(env, nread);
	if (jResp != NULL) {
		(*env)->SetByteArrayRegion(env, jResp, 0, nread, tempBuff);
	}
	return jResp;
}

/*
 * Class:     com_industry_printer_RFID
 * Method:    close
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_industry_printer_RFID_close
  (JNIEnv *env, jclass arg, jint fd)
{
	ALOGD("*******fd=%d",fd);
	int ret = close(fd);
	ALOGD("*******ret=%d",ret);
	return ret;
}

/*
 * Class:     com_industry_printer_RFID
 * Method:    calKey
 * Signature: ([B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_industry_printer_RFID_calKey
		(JNIEnv *env, jclass arg, jbyteArray uid)
{
	jbyte *cbuf;
	cbuf = (*env)->GetByteArrayElements(env, uid, 0);

/* Java Code moved from N_RFIDModule_M104BPCS_KX1207.java::calKey
        byte[] key = new byte[4];

// 2023-12-19 前使用的密钥计算公式
//        key[0] = (byte) ((~(mUID[0] ^ mUID[4])) + mUID[1]);
//        key[1] = (byte) ((~(mUID[5] ^ mUID[6])) + mUID[2]);
//        key[2] = (byte) ((~(mUID[0] ^ mUID[5])) + mUID[3]);
//        key[3] = (byte) ((~(mUID[4] ^ mUID[6])) + mUID[1] + mUID[2] + mUID[3]);

// 2023-12-19 以后使用的密钥计算公式
        key[0] = (byte) ((((~(mUID[0] ^ mUID[1])) + mUID[2]) ^ mUID[3]) + mUID[6]);
        key[1] = (byte) ((((~(mUID[1] ^ mUID[2])) + mUID[3]) ^ mUID[6]) + mUID[0]);
        key[2] = (byte) ((((~(mUID[2] ^ mUID[3])) + mUID[6]) ^ mUID[0]) + mUID[1]);
        key[3] = (byte) ((((~(mUID[3] ^ mUID[6])) + mUID[0]) ^ mUID[1]) + mUID[2]);

        return key;*/

    char rBuf[4];

    rBuf[0] = (((~(cbuf[0] ^ cbuf[1])) + cbuf[2]) ^ cbuf[3]) + cbuf[6];
	rBuf[1] = (((~(cbuf[1] ^ cbuf[2])) + cbuf[3]) ^ cbuf[6]) + cbuf[0];
	rBuf[2] = (((~(cbuf[2] ^ cbuf[3])) + cbuf[6]) ^ cbuf[0]) + cbuf[1];
	rBuf[3] = (((~(cbuf[3] ^ cbuf[6])) + cbuf[0]) ^ cbuf[1]) + cbuf[2];

	(*env)->ReleaseByteArrayElements(env, uid, cbuf, 0);
	jbyteArray result = (*env)->NewByteArray(env, 4);
	(*env)->SetByteArrayRegion(env, result, 0, 4, rBuf);
//	(*env)->ReleaseByteArrayElements(env, result, rBuf, JNI_ABORT);

	return result;
}
