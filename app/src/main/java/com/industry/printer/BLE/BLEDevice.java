package com.industry.printer.BLE;

import com.industry.printer.Serial.SerialPort;
import com.industry.printer.Utils.ByteArrayUtils;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.PlatformInfo;
import com.industry.printer.Utils.StreamTransport;
import com.industry.printer.Utils.StringUtil;
import com.industry.printer.hardware.ExtGpio;
import com.industry.printer.hardware.RFIDDevice;
import com.industry.printer.pccommand.PCCommandManager;

/**
 * Created by hmwan on 2024/1/27.
 */

public class BLEDevice {
    private static String TAG = BLEDevice.class.getSimpleName();

    private SerialPort mSerialPort;
    private StreamTransport mStreamTransport;
    private BLEStreamTransport mBLEStreamTransport;

    private final static String BLE_NAME = "Printer";

    private final static String CMD_RSTORE = "AT+RST";
    private final static String CMD_INIT_SERVER = "AT+BLEINIT=2";
    private final static String CMD_GATTS_CREATE_SERVICE = "AT+BLEGATTSSRVCRE";
    private final static String CMD_GATTS_START_SERVICE = "AT+BLEGATTSSRVSTART";
    private final static String CMD_SET_ADV_DATA = "AT+BLEADVDATA=\"020106%02X09%s\"";
    private final static String CMD_START_ADVERTISE = "AT+BLEADVSTART";
    private final static String RECV_CONNECTED = "+BLECONN:";
    private final static String RECV_CONNECTPARAM = "+BLECONNPARAM";
    private final static String RECV_DISCONNECTED = "+BLEDISCONN:";
    private final static String RECV_CLIENT_WRITE = "+WRITE";
    private final static String CMD_GATTS_INDICATE = "AT+BLEGATTSIND=0,1,7,";
    private final static String CMD_GATTS_NOTIFY = "AT+BLEGATTSNTFY=0,1,6,";

    private String mErrorMsg = "";

    private boolean mInitialized;
    private boolean mClientConnected;
    private String mClientMacAddress;

    private static BLEDevice mBLEDevice = null;

    public static BLEDevice getInstance() {
        if(null == mBLEDevice) {
            mBLEDevice = new BLEDevice();
        }
        return mBLEDevice;
    }

    private BLEDevice() {
        mSerialPort = new SerialPort();
        mStreamTransport = mSerialPort.spOpenStream(PlatformInfo.getRfidDevice(), 115200);
        mInitialized = false;
        mClientConnected = false;
        mClientMacAddress = "";
        mBLEStreamTransport = new BLEStreamTransport(mStreamTransport.getInputStream(), mStreamTransport.getOutputStream(), this);
        PCCommandManager.getInstance().addBLEHandler(mBLEStreamTransport);
    }

    private boolean sendATCmd(String cmd) {
        mErrorMsg = "TIMEOUT";
        boolean ret = false;
        for(int i=0; i<3; i++) {
            synchronized (RFIDDevice.SERIAL_LOCK) {
                ExtGpio.writeGpioTestPin('I', 9, 1);
                mStreamTransport.writeLine(cmd);
                for(int j=0; j<10; j++) {
                    if(mStreamTransport.readerReady()) {
                        String rcv = mStreamTransport.readLine();
//                        Debug.d(TAG, "RECV: [" + rcv + "]");
                        if(rcv.startsWith("OK")) {
                            mErrorMsg = "OK";
                            ret = true;
                            break;
                        } else if(rcv.startsWith("ERROR")) {
                            mErrorMsg = "ERROR";
                            break;
                        }
                    }
                    try {Thread.sleep(100);} catch(Exception e){}
                }
            }
            if(ret == true) break;
            else try {Thread.sleep(1000);} catch(Exception e){}
        }
        return ret;
    }

    private void sendString(String data) {
        synchronized (RFIDDevice.SERIAL_LOCK) {
            ExtGpio.writeGpioTestPin('I', 9, 1);
            mStreamTransport.writeLine(data);
        }
    }

    private void waitString(String prompt) {
        while(true) {
            synchronized (RFIDDevice.SERIAL_LOCK) {
                String rcv = "";
                ExtGpio.writeGpioTestPin('I', 9, 1);
                if (mStreamTransport.readerReady()) {
                    rcv = mStreamTransport.readLine();
//                    Debug.d(TAG, "RECV: [" + rcv + "]");
                }
                try {Thread.sleep(100);} catch (Exception e) {}
                if(rcv.startsWith(prompt)) break;
            }
        }
    }

    private void clearReceivingBuffer() {
        for(int i=0; i<30; i++) {
            synchronized (RFIDDevice.SERIAL_LOCK) {
                ExtGpio.writeGpioTestPin('I', 9, 1);
                if (mStreamTransport.readerReady()) {
                    String rcv = mStreamTransport.readLine();
//                    Debug.d(TAG, "RECV: [" + rcv + "]");
                }
                try {Thread.sleep(100);} catch(Exception e){}
            }
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

    private boolean execCmdSetAdvData() {
        return sendATCmd(String.format(CMD_SET_ADV_DATA, BLE_NAME.length()+1, ByteArrayUtils.toHexString(BLE_NAME.getBytes()).replace(" ", "")));
    }

    private boolean execCmdStartAdvertise() {
        return sendATCmd(CMD_START_ADVERTISE);
    }

    public boolean initServer() {
        mInitialized = false;
        mClientConnected = false;
        mClientMacAddress = "";
        return (mInitialized =
            execCmdRST() &&
            execCmdSetServerMode() &&
            execCmdCreateGattsService() &&
            execCmdStartGattsService() &&
            execCmdSetAdvData() &&
            execCmdStartAdvertise());
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
                rcvString = mStreamTransport.readLine();
//                Debug.d(TAG, "RECV: [" + rcvString + "]");
            }
        }

        if(StringUtil.isEmpty(rcvString)) {
            try{Thread.sleep(100);}catch(Exception e){};
            return rcvString;
        }

        if(!mClientConnected) {
            if(rcvString.startsWith(RECV_CONNECTED)) {
                mClientConnected = true;
                mClientMacAddress = rcvString.substring(RECV_CONNECTED.length()+3, RECV_CONNECTED.length()+20);
//                Debug.d(TAG, "Client [" + mClientMacAddress + "] connected.");
                waitString(RECV_CONNECTPARAM);
            }
        } else {
            if(rcvString.startsWith(RECV_DISCONNECTED)) {
                mClientConnected = false;
//                Debug.d(TAG, "Client [" + mClientMacAddress + "] disconnected.");
                mClientMacAddress = "";
            } else if(rcvString.startsWith(RECV_CLIENT_WRITE)) {
                rcvString = rcvString.substring(RECV_CLIENT_WRITE.length()+8);
//                Debug.d(TAG, "Received Data [" + rcvString.substring(rcvString.indexOf(',')+1) + "].");
                return rcvString.substring(rcvString.indexOf(',')+1);
            }
        }
        return "";
    }

    public void writeLine(String msg) {
        if(!mInitialized || !mClientConnected) return;

        if(sendATCmd(CMD_GATTS_INDICATE+msg.length())) {
            sendString(msg);
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