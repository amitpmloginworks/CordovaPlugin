package com.gttronics.ble.blelibrary.dataexchanger;

import java.util.*;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.gttronics.ble.blelibrary.BleController;
import com.gttronics.ble.blelibrary.BleControllerCallback;
import com.gttronics.ble.blelibrary.BleDevice;

import com.gttronics.ble.blelibrary.BleDeviceCallback;
import com.gttronics.ble.blelibrary.dataexchanger.callbacks.FirmwareMetaRetrievedCallback;
import com.gttronics.ble.blelibrary.dataexchanger.callbacks.FirmwareProgressCallback;
import com.gttronics.ble.blelibrary.dataexchanger.callbacks.FirmwareWriteCompletedCallback;
import com.gttronics.ble.blelibrary.dataexchanger.helpers.DxAppFirmLogStateMachine;
import com.gttronics.ble.blelibrary.dataexchanger.helpers.DxAppHelper;

/**
 * Created by EGUSI16 on 11/21/2016.
 */

public class DxAppController implements DataExchangerProfileCallback, BleDeviceCallback, BleControllerCallback {

    public static class Config {
        private int devCount = 1;
        private float pwrLevel = -70;
        private double timeout = 10.0;
        private boolean autoConnect = false;
        private boolean enableCmdChannel = true;
        private boolean enableChannelScram = false;
        private boolean enableTxCredit = true;
        private List<String> serviceUUIDs = new ArrayList<String>();

        public void setDevCount(int devCount) {
            this.devCount = devCount;
        }

        public void setPwrLevel(float pwrLevel) {
            this.pwrLevel = pwrLevel;
        }

        public void setTimeout(double timeout) {
            this.timeout = timeout;
        }

        public void setAutoConnect(boolean autoConnect) {
            this.autoConnect = autoConnect;
        }

        public void setEnableCmdChannel(boolean enableCmdChannel) {
            this.enableCmdChannel = enableCmdChannel;
        }

        public void setEnableChannelScram(boolean enableChannelScram) {
            this.enableChannelScram = enableChannelScram;
        }

        public void setEnableTxCredit(boolean enableTxCredit) {
            this.enableTxCredit = enableTxCredit;
        }

        public void addServiceUUID(String uuid) {
            serviceUUIDs.add(uuid);
        }

        public void setServiceUUID(List<String> uuids) {
            serviceUUIDs = uuids;
        }
    }

    private static final String TAG = "[DX_AC]";

    private static DxAppController dxAppController = null;

    public Context mContext;
    public Config mConfig;

    private BleController bleController;

    private DataExchangerProfileCallback mDataReceiveCallback = null;
    private BleDeviceCallback mBleCallback = null;
    private BleControllerCallback mBleControllerCallback = null;

    private HashMap<String,DxAppFirmLogStateMachine> mBleFupgSms = new HashMap<String,DxAppFirmLogStateMachine>();

    private DxAppController(Context context, Config config) {
        bleController = BleController.getInstance(context);

        if (config != null) {

            for (int i = 0; i < config.devCount; i++) {
                if (config.pwrLevel == -127) {
//                    dxDev.setProximityConnecting(false);
                    continue;
                }

                DataExchangerDevice dxDev = new DataExchangerDevice(context, this);
                dxDev.setAutoConnect(config.autoConnect);
                dxDev.setTimeout(config.timeout);
                dxDev.setProximityConnecting(true);
                dxDev.setMinPowerLevel((int) (config.pwrLevel > -20? -2 : config.pwrLevel));
                DataExchangerProfile dxProfile;
                if( config.serviceUUIDs.size() == 0 )
                {
                    // Let the profile to create the default service UUIDs
                    dxProfile = new DataExchangerProfile(dxDev, this);
                }
                else
                {
                    dxProfile = new DataExchangerProfile(dxDev, config.serviceUUIDs,this);
                }
                dxProfile.setRx2NotiDefault(config.enableCmdChannel);
                dxProfile.setTxCreditNotiDefault(config.enableTxCredit);
                dxProfile.setChannelScramblerDefault(config.enableChannelScram);

                dxDev.registerProfile(dxProfile);
                bleController.registerDevice(dxDev);
            }
            bleController.setDelegate(this);
        }

        mContext = context;
        mConfig = config;
    }

    public static DxAppController getInstance(Context context, Config config) {
        if (dxAppController == null)
            dxAppController = new DxAppController(context, config);

        return dxAppController;
    }

    public static DxAppController getInstance() {
        return dxAppController;
    }

    public static DxAppController reset() {
        if( dxAppController != null )
        {
            Context context = dxAppController.mContext;
            Config config = dxAppController.mConfig;
            DataExchangerProfileCallback profileCb = dxAppController.mDataReceiveCallback;
            BleDeviceCallback devCb = dxAppController.mBleCallback;
            BleControllerCallback ctrlrCb = dxAppController.mBleControllerCallback;

            dxAppController.mBleFupgSms.clear();
            dxAppController = null;
            dxAppController = new DxAppController(context, config);

            dxAppController.setProfileCallback(profileCb);
            dxAppController.setBleDeviceCallback(devCb);
            dxAppController.setBleControllerCallback(ctrlrCb);
        }

        return dxAppController;
    }

    public void setProfileCallback(DataExchangerProfileCallback callback)
    {
        mDataReceiveCallback = callback;
    }

    public void setBleDeviceCallback(BleDeviceCallback callback)
    {
        mBleCallback = callback;
    }

    public void setBleControllerCallback(BleControllerCallback callback)
    {
        mBleControllerCallback = callback;
    }

    public boolean isBleSupport() {
        return bleController.isBleSupport();
    }

    public boolean isBluetoothEnabled() {
        return bleController.isBluetoothEnabled();
    }

    public void startScan() {
        bleController.startScan();
    }

    public void stopScan()
    {
        bleController.stopScan();
    }

    public boolean disconnect(String uuid)
    {
        return bleController.disconnect(uuid);
    }

    public void disconnectAll()
    {
        bleController.disconnectAll();
    }

    public boolean isAnyConnected()
    {
        return bleController.isAnyConnected();
    }

    public boolean isConnected(String uuid)
    {
        return bleController.isConnected(uuid);
    }

    public boolean connect(String uuid)
    {
        DataExchangerDevice dxDev = (DataExchangerDevice) bleController.connectDevice(uuid);
        if( dxDev == null )
        {
            return false;
        }
        mBleFupgSms.put(uuid, new DxAppFirmLogStateMachine(dxDev));
        return true;
    }

    public boolean isBtOn()
    {
        return bleController.isBtOn();
    }

//    public void enableTxCreditNoti(boolean enable) { }
//
//    public void enableRxNoti(boolean enable) { }
//
//    public void enableRx2Noti(boolean enable) { }

    public boolean sendData(String uuid, byte[] data)
    {
        if (bleController.getConnectedDevice(uuid) == null)
            return false;

        return ((DataExchangerDevice) bleController.getConnectedDevice(uuid)).sendData(data);
    }

    public boolean sendCmd(String uuid, byte[] data)
    {
        if (bleController.getConnectedDevice(uuid) == null)
            return false;

        return ((DataExchangerDevice) bleController.getConnectedDevice(uuid)).sendCmd(data, 0);
    }

//    public boolean enableCmdChannel(boolean enabled) {
//        if (bleController.getConnectedDevice() == null)
//            return false;
//
//        return ((DataExchangerDevice) bleController.getConnectedDevice()).enableCmdChannel(enabled);
//    }
//
//    public boolean enableTransmitBackpressure(boolean enabled) {
//        if (bleController.getConnectedDevice() == null)
//            return false;
//
//        return ((DataExchangerDevice) bleController.getConnectedDevice()).enableTxCredit(enabled);
//    }
    
    public boolean readTxCredit(String uuid)
    {
        DataExchangerDevice dev = (DataExchangerDevice) bleController.getConnectedDevice(uuid);
        if ( dev == null)
        {
            return false;
        }
        return dev.readTxCredit();
    }

    public boolean switchFirmwareImageToSlot(String uuid, int slotId, boolean bKeepConfig)
    {
        DataExchangerDevice dev = (DataExchangerDevice) bleController.getConnectedDevice(uuid);
        if( dev == null)
        {
            return false;
        }
        String cmdStr = "AT+IMG=" + (bKeepConfig? slotId : slotId + 128) + "\r\n";
        return dev.sendCmd(cmdStr.getBytes(), 0);
    }

    public boolean writeTxCreditReportLoopCount(String uuid, int count)
    {
        DataExchangerDevice dev = (DataExchangerDevice) bleController.getConnectedDevice(uuid);
        if( dev == null)
        {
            return false;
        }
        return dev.writeTxCreditReportLoopCount(count);
    }
    

    public boolean retrieveFirmwareMetaWithProgress(String uuid, FirmwareProgressCallback progressCallback, FirmwareMetaRetrievedCallback metaRetrievedCallback)
    {
        DataExchangerDevice dev = (DataExchangerDevice) bleController.getConnectedDevice(uuid);
        if( dev == null)
        {
            return false;
        }
        return mBleFupgSms.get(uuid).retrieveFirmwareMetaWithProgress(progressCallback, metaRetrievedCallback);
    }

    public boolean writeFirmwareImageInSlot(String uuid, byte slotIdx, byte[] firmData, byte[] scratchPad, FirmwareProgressCallback progressCallback, FirmwareWriteCompletedCallback completedCallback)
    {
        DataExchangerDevice dev = (DataExchangerDevice) bleController.getConnectedDevice(uuid);
        if( dev == null)
        {
            return false;
        }
        return mBleFupgSms.get(uuid).writeFirmwareImageInSlot(slotIdx, firmData, scratchPad, progressCallback, completedCallback);
    }

    public boolean deleteFirmwareImageFromSlot(String uuid, byte slotIdx, FirmwareProgressCallback progressCallback, FirmwareWriteCompletedCallback completedCallback)
    {
        DataExchangerDevice dev = (DataExchangerDevice) bleController.getConnectedDevice(uuid);
        if( dev == null)
        {
            return false;
        }
        return mBleFupgSms.get(uuid).deleteFirmwareImageFromSlot(slotIdx, progressCallback, completedCallback);
    }

    public boolean primeFirmwareBinary(String uuid, final byte[] firmBin, final String firmName, final FirmwareWriteCompletedCallback completedCallback, final FirmwareProgressCallback progressCallback)
    {
        DataExchangerDevice dev = (DataExchangerDevice) bleController.getConnectedDevice(uuid);
        if( dev == null)
        {
            return false;
        }
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Prime firmware");
                mBleFupgSms.get(uuid).primeFirmwareBinary(firmBin, firmName, completedCallback, progressCallback);
            }
        }, 3000);

        return true;
    }

    //Data Exchanger protocol
    @Override
    public void onRxDataAvailable(DataExchangerDevice dev, byte[] data)
    {
        //Log.d(TAG, "onRxDataAvailable " + new String(data));
        mDataReceiveCallback.onRxDataAvailable(dev, data);
        if (!mBleFupgSms.get(dev.mUUID).processRxData(data))
        {
            mDataReceiveCallback.onRxDataAvailable(dev, data);
        }
    }

    @Override
    public void onRx2DataAvailable(DataExchangerDevice dev, byte[] data)
    {
        //Log.d(TAG, "onRx2DataAvailable " + new String(data));
        if (!mBleFupgSms.get(dev.mUUID).processRx2Data(data))
        {
            mDataReceiveCallback.onRx2DataAvailable(dev, data);
        }
    }

    @Override
    public void onTxCreditDataAvailable(DataExchangerDevice dev, byte[] data)
    {
        data = DxAppHelper.reverseArray(data);
        int credits = DxAppHelper.byteArrayToInt(data);
        if( !mBleFupgSms.get(dev.mUUID).processTxCredit(credits) ) {
            mDataReceiveCallback.onTxCreditDataAvailable(dev, data);
        }
    }

    @Override
    public void onCharacteristicWrite(DataExchangerDevice dev, int signature)
    {
        //mBleFupgSms.get(dev.mUUID).onCharacteristicWrite(c);
        //mDataReceiveCallback.onCharacteristicWrite(c);
    }

    @Override
    public void onDeviceStateChanged(BleDevice device, BLE_DEV_STATE state)
    {
        if (mBleCallback != null)
            mBleCallback.onDeviceStateChanged(device, state);
    }

    @Override
    public void onAllProfilesReady(BleDevice devive, boolean isAllReady)
    {
        if (mBleCallback != null)
            mBleCallback.onAllProfilesReady(devive, isAllReady);
    }

    @Override
    public void onUnknownDeviceDiscovered(BluetoothDevice btDev, int rssi, byte[] scanRecord) {}

    @Override
    public void onDeviceDiscovered(BleDevice bleDev, BluetoothDevice btDev, int rssi, byte[] scanRecord)
    {
        String uuid = UUID.nameUUIDFromBytes(btDev.getAddress().getBytes()).toString();
        //Log.d(TAG, "onDeviceDiscovered: " + uuid);

        // mleung 20180426
        // - the purpose of auto reconnect is not neccessary
        // - it should be taking the auto connect status from the BLE device to determine whether
        //   immediate connect is neccessary
        // if (isAutoReconnecting && dxAppController != null) {
        if( bleDev.isAutoConnect() && dxAppController != null )
        {
            dxAppController.stopScan();
            dxAppController.connect(uuid);
            return;
        }
        else
        {
            if( mBleControllerCallback != null )
            {
                mBleControllerCallback.onDeviceDiscovered(bleDev, btDev, rssi, scanRecord);
            }
        }

    }

//    @Override
//    public void onUpdateValueForCharacteristic(BluetoothGattCharacteristic c) {
//        if (mBleCallback != null)
//            mBleCallback.onUpdateValueForCharacteristic(c);
//    }
//
//    @Override
//    public void onCharacteristicDiscovered(BluetoothGattCharacteristic c) {
//        Log.d(TAG, "onCharacteristicDiscovered");
//    }
}
