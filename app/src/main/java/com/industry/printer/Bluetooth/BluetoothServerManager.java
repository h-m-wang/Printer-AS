package com.industry.printer.Bluetooth;

import com.industry.printer.FileFormat.SystemConfigFile;

public class BluetoothServerManager {
    private static final String TAG = BluetoothServerManager.class.getSimpleName();

    private static BluetoothServerManager mBluetoothServerManager = null;

    public static BluetoothServerManager getInstance() {
        if(null == mBluetoothServerManager) {
            mBluetoothServerManager = new BluetoothServerManager();
        }
        return mBluetoothServerManager;
    }

    private BluetoothServer[] mServers;

    private BluetoothServerManager() {
        mServers = new BluetoothServer[] {
                new BLEServer(), new BTServer()
        };
    }

    public void paramsChanged() {
        int enable = SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_BLE_ENABLE);
        int devno = SystemConfigFile.getInstance().getParam(SystemConfigFile.INDEX_LOCAL_ID);

        for(BluetoothServer server : mServers) {
            server.paramsChanged(enable, devno);
        }
    }

    public boolean isInitialized() {
        for(BluetoothServer server : mServers) {
            if(server.mInitialized) return true;
        }
        return false;
    }
}
