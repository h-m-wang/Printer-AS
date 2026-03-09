package com.industry.printer.Collaboration;

import com.industry.printer.ControlTabActivity;
import com.industry.printer.MainActivity;
import com.industry.printer.Utils.Debug;
import com.industry.printer.hardware.RTCDevice;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SocketManager {
    private static final String TAG = SocketManager.class.getSimpleName();

    private static final int PORT = 4551;           // port number;

    public static List<String> scanAllDevices() {
        try {
            String hostAddress = ControlTabActivity.getLocalIpAddress();
            Debug.d(TAG, "本机IP: " + hostAddress);

            String subnet = hostAddress.substring(0, hostAddress.lastIndexOf('.'));
            Debug.d(TAG, "扫描网段: " + subnet + ".1/254");

            final List<String> aliveIPs = new ArrayList<>();
            ExecutorService executor = Executors.newFixedThreadPool(20); // 使用线程池加速

            for (int i = 1; i <= 254; i++) {
                final String targetIp = subnet + "." + i;
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            InetAddress addr = InetAddress.getByName(targetIp);
                            if (addr.isReachable(200)) {
                                Debug.d(TAG, "发现设备: " + targetIp);
                                synchronized (aliveIPs) {
                                    aliveIPs.add(targetIp);
                                }
                            }
                        } catch (Exception e) {
                        }
                    }
                });
            }

            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.MINUTES);
            return aliveIPs;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
