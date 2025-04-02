package com.industry.printer.Bluetooth;

import com.industry.printer.Rfid.N_RFIDSerialPort;
import com.industry.printer.Utils.ByteArrayUtils;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.StreamTransport;
import com.industry.printer.Utils.StringUtil;
import com.industry.printer.hardware.ExtGpio;
import com.industry.printer.hardware.RFIDDevice;
import com.industry.printer.pccommand.PCCommandHandler;
import com.industry.printer.pccommand.PCCommandManager;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Created by hmwan on 2025/3/15.
 * 通过AiThinker的串口蓝牙芯片实现蓝牙服务器。
 */

public class BLEServer extends BluetoothServer {
    private static String TAG = BLEServer.class.getSimpleName();

    private final static String CMD_RSTORE = "AT+RST";
    private final static String CMD_INIT_SERVER = "AT+BLEINIT=2";
    private final static String CMD_GATTS_CREATE_SERVICE = "AT+BLEGATTSSRVCRE";
    private final static String CMD_GATTS_START_SERVICE = "AT+BLEGATTSSRVSTART";
    private final static String CMD_SET_ADV_DATA = "AT+BLEADVDATA=\"020106%02X09%s\"";
    private final static String CMD_START_ADVERTISE = "AT+BLEADVSTART";
    private final static String CMD_STOP_ADVERTISE = "AT+BLEADVSTOP";
    private final static String RECV_CONNECTED = "+BLECONN:";
    private final static String RECV_CONNECTPARAM = "+BLECONNPARAM";    // BLECONNPARAM:<conn_index>,<min_interval>,<max_interval>,<cur_interval>,<latency>,<timeout>
    // 【例】+BLECONNPARAM:0,0,0,6,0,500
    private final static String RECV_DISCONNECTED = "+BLEDISCONN:";
    private final static String RECV_CLIENT_WRITE = "+WRITE";
    private final static String CMD_GATTS_INDICATE = "AT+BLEGATTSIND=0,1,7,";
    private final static String CMD_GATTS_NOTIFY = "AT+BLEGATTSNTFY=0,1,6,";
    private final static String CMD_SPP_CFG = "AT+BLESPPCFG=1,1,6,1,5";
    private final static String CMD_SPP = "AT+BLESPP";

    private StreamTransport mStreamTransport;
    private boolean mClientConnected;
    private String mClientMacAddress;

    protected BLEServer() {
        super();

        FileDescriptor fd = RFIDDevice.cnvt2FileDescriptor(N_RFIDSerialPort.mFd + RFIDDevice.mFd);  // 两者之间没有被启用的为0，被启用的是一个大于0的值（一个是复旦卡，另一个是23卡）
        mStreamTransport = new StreamTransport(new FileInputStream(fd), new FileOutputStream(fd));

        mClientConnected = false;
        mClientMacAddress = "";
    }

    private boolean sendATCmd(String cmd) {
        boolean ret = false;
        for(int i=0; i<3; i++) {    // 尝试3次，直到成功或者超过次数
            Debug.d(TAG, "RFID-SEND: [" + cmd + "]");
            mStreamTransport.writeLine(cmd);
            for(int j=0; j<10; j++) {
                if(mStreamTransport.readerReady()) {
                    String rcv = mStreamTransport.readLine();
                    Debug.d(TAG, "RFID-RECV: [" + rcv + "]");
                    if(rcv.startsWith("OK")) {
                        ret = true;
                        break;
                    } else if(rcv.startsWith("ERROR")) {
                        break;
                    }
                }
                try {Thread.sleep(100);} catch(Exception e){}
            }

            if(ret == true) break;
            else try {Thread.sleep(1000);} catch(Exception e){}
        }
        return ret;
    }

    private void waitString(String prompt) {
        while(true) {
            String rcv = "";
            if (mStreamTransport.readerReady()) {
                rcv = mStreamTransport.readLine();
                Debug.d(TAG, "RFID-RECV: [" + rcv + "]");
            }
            try {Thread.sleep(100);} catch (Exception e) {}
            if(rcv.startsWith(prompt)) break;
        }
    }

    private void clearReceivingBuffer() {
        for(int i=0; i<30; i++) {
            if (mStreamTransport.readerReady()) {
                String rcv = mStreamTransport.readLine();
//                    Debug.d(TAG, "RECV: [" + rcv + "]");
            }
            try {Thread.sleep(100);} catch(Exception e){}
        }
    }

    private boolean execCmdRST() {
        if(sendATCmd(CMD_RSTORE)) {
            waitString("ready");
            return true;
        }
        return false;
    }

    private boolean execCmdSetServerMode() {
        return sendATCmd(CMD_INIT_SERVER);
    }
    private boolean execCmdCreateGattsService() {
        return sendATCmd(CMD_GATTS_CREATE_SERVICE);
    }
    private boolean execCmdStartGattsService() {
        return sendATCmd(CMD_GATTS_START_SERVICE);
    }
    private boolean execGattGetChars() {
        return sendATCmd("AT+BLEGATTSCHAR?");
    }

    private boolean execCmdSetAdvData() {
        return sendATCmd(String.format(CMD_SET_ADV_DATA, mServerName.length()+1, ByteArrayUtils.toHexString(mServerName.getBytes()).replace(" ", "")));
    }

    private boolean execCmdStartAdvertise() {
        return sendATCmd(CMD_START_ADVERTISE);
    }

    private boolean execCmdStopAdvertise() {
        return sendATCmd(CMD_STOP_ADVERTISE);
    }

    public void closeServer() {
        mInitialized = false;
        synchronized (RFIDDevice.SERIAL_LOCK) {
            ExtGpio.writeGpioTestPin('I', 9, 1);
            execCmdStopAdvertise();
            ExtGpio.writeGpioTestPin('I', 9, 0);
            PCCommandManager pcCM = PCCommandManager.getInstance();
            if(null != pcCM) {
                PCCommandHandler pcCH = pcCM.getBLEHandler();
                if(pcCH != null) {
                    pcCH.close();
                }
                pcCM.addBLEHandler(null);
            }
        }
        Debug.d(TAG, "RFID-CLOSED");
    }

    public static boolean BLERequiring = false;

    @Override
    public void initServer(int devno) {
        if(BLERequiring) return;            // 正在初始化，其他线程的初始化请求被拒绝
        BLERequiring = true;
        createServerName(devno);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Debug.d(TAG, "RFID-ENTER");
                mInitialized = false;
                mClientConnected = false;
                mClientMacAddress = "";
                synchronized (RFIDDevice.SERIAL_LOCK) {
                    ExtGpio.writeGpioTestPin('I', 9, 1);
                    mInitialized =
                            execCmdRST() &&
                                    execCmdSetServerMode() &&
                                    execCmdCreateGattsService() &&
                                    execCmdStartGattsService() &&
                                    execCmdSetAdvData() &&
                                    execCmdStartAdvertise();
//            execGattGetChars();
                    ExtGpio.writeGpioTestPin('I', 9, 0);
                    PCCommandManager pcCM = PCCommandManager.getInstance();
                    if(null != pcCM && mInitialized) {
                        pcCM.addBLEHandler(new BLEStreamTransport(mStreamTransport.getInputStream(), mStreamTransport.getOutputStream(), BLEServer.this));
                    }
                }
                Debug.d(TAG, "RFID-QUIT - " + mInitialized);
                BLERequiring = false;
            }
        }).start();
    }

    public boolean isClientConnected() {
        return mClientConnected;
    }

    public String readLine() {
        if(!mInitialized) return "";

        String rcvString = "";
        synchronized (RFIDDevice.SERIAL_LOCK) {
            ExtGpio.writeGpioTestPin('I', 9, 1);
            if(mStreamTransport.readerReady()) {
                // AITHINKER的C304通道每次最多可以传递144字节的数据
                rcvString = mStreamTransport.readLine();
                Debug.d(TAG, "RFID-RECV: [" + rcvString + "]\n[" + ByteArrayUtils.toHexString(rcvString.getBytes()) + "]");
            }

            if(StringUtil.isEmpty(rcvString)) {
                return rcvString;
            }

//            if(!mClientConnected) {       // 由于蓝牙与RFID共用一个串口，通过切换时间片来切换，因此，连接事件极有可能在处于RFID通道的时候发生，此时BLE芯片会建立连接，但无法接收到串口的连接通知
            if(rcvString.startsWith(RECV_CONNECTED)) {
                mClientConnected = true;
                mClientMacAddress = rcvString.substring(RECV_CONNECTED.length()+3, RECV_CONNECTED.length()+20);
                Debug.d(TAG, "Client [" + mClientMacAddress + "] connected.");
                waitString(RECV_CONNECTPARAM);
            }
//            } else {
            else if(rcvString.startsWith(RECV_DISCONNECTED)) {
                mClientConnected = false;
                Debug.d(TAG, "Client [" + mClientMacAddress + "] disconnected.");
                mClientMacAddress = "";
                execCmdStartAdvertise();
            } else if(rcvString.startsWith(RECV_CLIENT_WRITE)) {
                rcvString = rcvString.substring(RECV_CLIENT_WRITE.length()+8);
                while(!rcvString.endsWith("..|..")) {
                    String str = mStreamTransport.readLine();
                    rcvString += (str == null ? "" : str);
                }
                rcvString = rcvString.substring(0, rcvString.length()-5);
                Debug.d(TAG, "Received Data [" + rcvString.substring(rcvString.indexOf(',')+1) + "].");
                return rcvString.substring(rcvString.indexOf(',')+1);
            }
//            }
        }
        return "";
    }

    public void writeLine(String msg) {
//        if(!mInitialized || !mClientConnected) return;
        if(!mInitialized) return;

        synchronized (RFIDDevice.SERIAL_LOCK) {
            String str = msg + "..|..";
            int pos = 0;

            while(pos < str.length()) {
                int len = Math.min(40, str.length()-pos);
                String tmp = str.substring(pos, pos+len);

                Debug.d(TAG, "RFID-SEND: [" + CMD_GATTS_INDICATE+tmp.getBytes().length + "]");
                mStreamTransport.writeLine(CMD_GATTS_INDICATE+tmp.getBytes().length);
                boolean done = false;
                long start = System.currentTimeMillis();
                while(true) {
                    if(System.currentTimeMillis() - start > 3000) break;        // 一直失败的话，最多尝试3秒后，彻底退出
                    if(mStreamTransport.readerReady()) {
                        String rcv = mStreamTransport.readLine();
                        Debug.d(TAG, "RFID-RECV: [" + rcv + "]");
                        if(rcv.startsWith("ERROR")) {       // 发生错误时，休眠100ms后重新发送请求
                            try {Thread.sleep(100);} catch(Exception e){}
                            Debug.d(TAG, "RFID-SEND: [" + CMD_GATTS_INDICATE+tmp.getBytes().length + "]");
                            mStreamTransport.writeLine(CMD_GATTS_INDICATE+tmp.getBytes().length);
                            continue;
                        } else if(rcv.startsWith(">")) {
                            Debug.d(TAG, "RFID-SEND: [" + tmp + "]");
                            mStreamTransport.writeLine(tmp);
                            pos += len;
                            done = true;
                        } else if(rcv.startsWith("OK") && done) {
                            break;
                        }
                    }
                }
                if(!done) {
                    Debug.d(TAG, "RFID-SEND: Error Quit.");
                    break;
                }
            }
        }
    }

    public int read(byte[] buffer, int offset, int count) {
        int recv = 0;

        synchronized (RFIDDevice.SERIAL_LOCK) {
            ExtGpio.writeGpioTestPin('I', 9, 1);

            byte[] temp = new byte[RECV_CLIENT_WRITE.length()+8];

            // 假如读入：+WRITE:0,1,5,,5,ASDFG<0D><0A>
            while(recv < count) {
                int ret = mStreamTransport.read(temp);      // +WRITE:0,1,5,,
                if(ret != temp.length) {
                    Debug.e(TAG, "No package head");
                    return 0;
                }
                if(!(new String(temp).startsWith(RECV_CLIENT_WRITE))) {
                    Debug.e(TAG, "Invalid pakage head");
                    return 0;
                }
                int packCount = 0;
                while(true) {   // 5,
                    ret = mStreamTransport.read(temp, 0, 1);
                    if(ret != 1) {
                        Debug.e(TAG, "Invalid charactor number");
                        return 0;
                    }
                    if(temp[0] == ',') break;
                    if(temp[0] < '0' || temp[0] > '9') {
                        Debug.e(TAG, "Invalid charactor number");
                        return 0;
                    }
                    packCount *= 10;
                    packCount += (0x00f & temp[0]);
                }
                ret = mStreamTransport.read(buffer, offset + recv, Math.min(count-recv, packCount));      // ASDFG
                if (ret != Math.min(count-recv, packCount)) {
                    Debug.e(TAG, "Character number dont't match");
                    return 0;
                }
                recv += ret;
                mStreamTransport.read(temp, 0, packCount - ret + 2);      // 0D 0A
            }
            Debug.d(TAG, new String(buffer) + "[" + ByteArrayUtils.toHexString(buffer) + "]");
        }
        return recv;
    }
}