/* DO NOT EDIT THIS FILE - it is machine generated */


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

//#include <nativehelper/jni.h>
//#include <JNIHelp.h>
//#include <android_runtime/AndroidRuntime.h>
#define JNI_TAG "serial_jni"
#define ALOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,JNI_TAG,__VA_ARGS__)

static int speed_arr[]={ B921600,B460800,B230400, B115200, B38400, B19200, B9600, B4800, B2400, B1200, B300,
    B38400, B19200, B9600, B4800, B2400, B1200, B300,};
static int name_arr[]={921600,460800,230400,115200, 38400, 19200, 9600, 4800, 2400, 1200, 300, 38400,
    19200, 9600, 4800, 2400, 1200, 300,};
/*
 * Class:     com_industry_printer_UsbSerial
 * Method:    open
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_industry_printer_UsbSerial_open
  (JNIEnv *env, jclass arg, jstring dev)
{
	int ret;
	char *dev_utf = (*env)->GetStringUTFChars(env, dev, JNI_FALSE);
	ALOGD("===>open usb2serial\n");
	ret = open(dev_utf, O_RDWR|O_NOCTTY|O_NONBLOCK);

	if( ret == -1)
	{
		//__android_log_print(ANDROID_LOG_INFO,JNI_TAG, "open: can not open Serial port %s, error=%s",dev_utf, strerror(errno));
		//ALOGD("can not open Serial port");
	}
	else
	{
		ALOGD("Serial open success");
	}
	tcflush(ret, TCIFLUSH);
	ALOGD("********ret=%d\n",ret);
	(*env)->ReleaseStringUTFChars(env, dev, dev_utf);
	return ret;
}

/*
 * Class:     com_industry_printer_UsbSerial
 * Method:    setBaudrate
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_industry_printer_UsbSerial_setBaudrate
  (JNIEnv *env, jclass arg, jint fd, jint speed)
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
	        //cfsetispeed(&Opt, B230400);
	        //cfsetospeed(&Opt, B230400);
	        status = tcsetattr(fd, TCSANOW, &Opt);
	        if(status != 0)
	            ALOGD("tcsetattr fd1\n");
	        tcgetattr(fd, &Opt);
	        //__android_log_print(ANDROID_LOG_INFO,JNI_TAG,"setBaudrate: bauterate=%d\n",cfgetospeed(&Opt));

	        return 0;
	    }
	    tcflush(fd, TCIOFLUSH);
	}
	return 0;
}

/*
 * Class:     com_industry_printer_UsbSerial
 * Method:    close
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_industry_printer_UsbSerial_close
  (JNIEnv *env, jclass arg, jint fd)
{
	ALOGD("*******fd=%d",fd);
	int ret = close(fd);
	ALOGD("*******ret=%d",ret);
	return ret;
}

/*
 * Class:     com_industry_printer_UsbSerial
 * Method:    write
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_industry_printer_UsbSerial_write
  (JNIEnv *env, jclass arg, jint fd, jshortArray buf, jint len)
{
	int i,ret;
	jchar *buf_utf = (*env)->GetByteArrayElements(env, buf, NULL);
	//__android_log_print(ANDROID_LOG_INFO, JNI_TAG, "write: length=%d\n",(*env)->GetArrayLength(env, buf));
	if(fd <= 0)
		return 0;
/*
	for(i=0;i<len; i++)
	{
		ALOGD("buf_utf[%d]=%x\n", i, buf_utf[i]);
	}
*/
	tcflush(fd, TCIOFLUSH);
	ret = write(fd, buf_utf, len);

	(*env)->ReleaseCharArrayElements(env, buf, buf_utf, 0);
	return ret;
}

/*
 * Class:     com_industry_printer_UsbSerial
 * Method:    read
 * Signature: ()I
 */
JNIEXPORT jbyteArray JNICALL Java_com_industry_printer_UsbSerial_read
  (JNIEnv *env, jclass arg, jint fd, jint len)
{
	int i;
	int nread=0;
	int repeat=0;
	char tempBuff[256];
	//char response[];
	jbyteArray jResp=NULL;

	struct termios Opt;
	tcgetattr(fd, &Opt);
	Opt.c_cc[VMIN] = len;
	Opt.c_cc[VTIME] = 1000;
	tcsetattr(fd, TCSANOW, &Opt);
	bzero(tempBuff, sizeof(tempBuff));
	if(fd <= 0)
		return NULL;
	ALOGD("remove tcflush\n");
	while((nread = read(fd, tempBuff, len))<=0 && repeat<10)
	{
		//__android_log_print(ANDROID_LOG_INFO, JNI_TAG, "read:  read len=%d\n", nread);
		usleep(20000);
		repeat++;
	}

	//nread = read(fd, tempBuff, len);

	if(nread<=0)
	{
		//ALOGD("********read ret=%d\n",nread);
		ALOGD("********read ret=%d,error=%d\n",nread, errno);
		//ALOGD("********read ret=%d\n",nread);
		return NULL;
	}
		/*
		ALOGD("nread len = %d\n",nread);
		__android_log_print(ANDROID_LOG_INFO, JNI_TAG, "read: nread len = %d\n",nread);
		for(i=0; i<nread; i++)
		{
			__android_log_print(ANDROID_LOG_INFO, JNI_TAG, "read: tempBuff[%d]=0x%x\n",i, tempBuff[i]);
		}
		*/
	    tempBuff[nread+1] = '\0';
	    //__android_log_print(ANDROID_LOG_INFO, "read","nread tempBuff = %s\n",tempBuff);
	    //jstr = (*env)->NewStringUTF(env, tempBuff);
	    //response = (char*)malloc(nread*sizeof(char));

	jResp = (*env)->NewByteArray(env, nread);
	if (jResp != NULL) {
		(*env)->SetByteArrayRegion(env, jResp, 0, nread, tempBuff);
	}
	return jResp;
}


JNIEXPORT jint JNICALL Java_com_industry_printer_UsbSerial_set_options
  (JNIEnv *env, jobject obj, jint fd, jint databits, jint stopbits, jint parity)
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
    opt.c_cc[VTIME] = 150; /*ds*/
    opt.c_cc[VMIN] = 0;
    tcflush(fd, TCIFLUSH);
    if (tcsetattr(fd,TCSANOW,&opt) != 0)
    {
        ALOGD("SetupSerial 3\n");
        return -1;
    }
     return 0;
}

JNIEXPORT jstring JNICALL Java_com_industry_printer_UsbSerial_get_BuildDate
  (JNIEnv *env, jobject obj)
{
	//__android_log_print(ANDROID_LOG_INFO, JNI_TAG, "getBuildDate: build time=%s, %s\n",__DATE__, __TIME__);
	return __DATE__;

}


