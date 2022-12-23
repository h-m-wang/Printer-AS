package com.industry.printer.Serial;

import android.content.Context;

import com.industry.printer.DataTransferThread;
import com.industry.printer.Utils.ByteArrayUtils;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.StringUtil;

import org.apache.http.util.ByteArrayBuffer;

import java.nio.charset.Charset;

/**
 * Created by hmwan on 2022/12/19.
 */

public class SerialDotMarker extends SerialProtocol {
    public static String TAG = SerialProtocol5.class.getSimpleName();

    public SerialDotMarker(/*StreamTransport st*/SerialPort serialPort, Context ctx){
        super(serialPort, ctx);
    }

    @Override
    protected int parseFrame(ByteArrayBuffer recvMsg) {
        try {
            byte[] msg = recvMsg.toByteArray();

            if(msg.length < 2) {                            // 帧长度异常
                sendCommandProcessResult(0, 0, 0, 0, "ER-01-" + ByteArrayUtils.toHexString(msg) + "\n");
                return ERROR_FAILED;
            }

            if((msg[0] & 0x0ff) != msg.length - 1) {        // 帧长度异常
                sendCommandProcessResult(0, 0, 0, 0, "ER-02-" + ByteArrayUtils.toHexString(msg) + "\n");
                return ERROR_FAILED;
            }

            sendCommandProcessResult(0, 0, 0, 0, "OK-01-" + ByteArrayUtils.toHexString(msg) + "\n");
            DataTransferThread.setDotMarkerRecvBuffer(msg);

            return ERROR_SUCESS;                        // 协议格式正确
        } catch(Exception e) {
            Debug.e(TAG, e.getMessage());
        }
        return ERROR_FAILED;
    }

    @Override
    protected byte[] createFrame(int cmd, int ack, int devStatus, int cmdStatus, byte[] msg) {
        return msg;
    }

    @Override
    public void handleCommand(SerialHandler.OnSerialPortCommandListenner normalCmdListenner, SerialHandler.OnSerialPortCommandListenner printCmdListenner, ByteArrayBuffer bab) {
        setListeners(normalCmdListenner, printCmdListenner);

        int result = parseFrame(bab);
        if (result == ERROR_SUCESS) {
            byte[] recvData = bab.toByteArray();
            procCommands(result, recvData);
        } else {
            procError("ERROR_FAILED");
        }
    }

    @Override
    public void sendCommandProcessResult(int cmd, int ack, int devStatus, int cmdStatus, String message) {
        byte[] retMsg = createFrame(cmd, ack, devStatus, cmdStatus, message.getBytes(Charset.forName("UTF-8")));
        super.sendCommandProcessResult(retMsg);
    }

    public void sendCommandProcessResult(int cmd, int ack, int devStatus, int cmdStatus, byte[] msg) {
        byte[] retMsg = createFrame(cmd, ack, devStatus, cmdStatus, msg);
        super.sendCommandProcessResult(retMsg);
    }

}
