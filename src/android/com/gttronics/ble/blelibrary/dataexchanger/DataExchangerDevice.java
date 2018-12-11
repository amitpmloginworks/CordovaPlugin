package com.gttronics.ble.blelibrary.dataexchanger;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import com.gttronics.ble.blelibrary.*;
import com.gttronics.ble.blelibrary.dataexchanger.DataExchangerProfile;

import android.content.Context;
import android.util.Log;

public class DataExchangerDevice extends BleDevice 
{
	protected static final String TAG = "[DX_D]";

	private static final int BLE_DEVICE_CONNECTED = 1;

	private int minPowerLevel = -70;

	// mleung 20180426
	// - duplicated from BleDevice
	// private boolean autoConnect = false;

	private double timeout = 0;
	private boolean proximityConnecting = false;

	private int state;


	public DataExchangerDevice(Context context, BleDeviceCallback callback) 
	{
		super(context, callback);
	}

	public void setMinPowerLevel(int minPowerLevel) {
		this.minPowerLevel = minPowerLevel;
	}

	public void setTimeout(double timeout) {
		this.timeout = timeout;
	}

	public void setProximityConnecting(boolean proximityConnecting) {
		this.proximityConnecting = proximityConnecting;
	}

	// mleung 20180426
	// - duplicated
	// @Override
	// public void setAutoConnect(boolean autoConnect) {
	//	this.autoConnect = autoConnect;
	// }

	// This method is called when system discovers a BLE device. This method
	// should check things and decide whether it should be connected. 
	//
	// Things to check:
	// 1. rssi       - radio power of the discovered device. Usually this is a coarse 
	//                 indication of the distance between the phone/tablet and the 
	//                 discovered device.
	// 2. scanRecord - the advertising packet which contains information like:
	//                 ServiceUUID (mandatory), ServiceData (optional),
	//                 TxPower(optional), ManufacturingData(options), etc.
	//
	// The return code inform the BLE controller to accept the connection or not.
	// 1. return -1     : don't initiate connection
	// 2. return  0-100 : initiate connection but let the BLE controller to prioritize
	//                    which BleDevice should get connected. BLE controller will
	//                    chooses the BLEDevice that returns the largest #. If all
	//                    BLEDevice return 0, it will choose the 1st in the registered
	//                    list (maintained by the controller).
	// 3. In typical case, return 0 if the discovered device should be connected.
	//
	// Important App Notes:
	// 1. some android devices would call this method multiple times (the desire
	//    behavior) when advertising packets of a particular device are continuously 
	//    detected. This is particularly useful to check the continuous power level
	//    and/or to connect the discovered device at a later time.
	// 2. However, some android devices would only call once (the not-so
	//    desire behavior) even advertising of the BLE device is continuous. This 
	//    would disallow the app to check the continuous power level, and eliminate 
	//    the opportunity to make connection with the device at a later time.
	// 3. The app should prepare to work around this problem by toggling the BLE
	//    scanning using the BLE controller's startScan and stopScan methods. After
	//    the stopScan and startScan, the android device will call this method again,
	//    if the BLE device is rediscovered.
	//
	protected int evaluateDeviceMatchingScore(final int rssi, byte[] scanRecord)
	{
        List<AdRecord> records = AdRecord.parseScanRecord(scanRecord);
        
        for(AdRecord packet : records) 
        {
            //Find the service data record that contains our service's UUID
            if( packet.getType() == AdRecord.TYPE_UUID128 ||
            	packet.getType() == AdRecord.TYPE_UUID128_INC )
            {
            	UUID uuid = AdRecord.getServiceUUID(packet);
            	//Log.d(TAG,"UUID1:"+uuid.toString());
            	
            	String serviceUuidConcat = "";
            	for (UUID serviceUuid : listOfServiceUUIDs()) 
            	{
            		serviceUuidConcat = serviceUuidConcat + serviceUuid.toString() + ";"; 
            	}            	
            	//Log.d(TAG,"Service UUIDs:"+serviceUuidConcat);
            	//Log.d(TAG,"RSSI:"+rssi);
            	
            	for (UUID serviceUuid : listOfServiceUUIDs()) 
            	{
            		// Make sure the service uuid is corrected
	            	if( uuid.equals(serviceUuid) )
	            	{
	            		// based on power level, you can decide whether this connection 
	            		// should be accepted or not. Uncomment this block to 
	            		{
	            			// enable the power level check.
	            			if(minPowerLevel != -9999 && rssi < minPowerLevel ) //-45
	            			{
	            				return -1;
	            			}
	            		}
	            		
	            		// Accept
	            		return 0;
	            	}
            	}
            }
        }        
        
        // No match
        return -1;
	}

	public HashSet<BleProfile> getRegisteredProfile() {
		return registeredProfiles;
	}

	public boolean sendData(byte[] data) {
		for (BleProfile profile: registeredProfiles) {
			if (profile instanceof DataExchangerProfile) {
				return ((DataExchangerProfile) profile).sendDataCh(data, 0);
			}
		}

		return false;
	}

	public boolean sendData(byte[] data, int signature) {
		for (BleProfile profile: registeredProfiles) {
			if (profile instanceof DataExchangerProfile) {
				return ((DataExchangerProfile) profile).sendDataCh(data, signature);
			}
		}

		return false;
	}

	public boolean sendCmd(byte[] data, int signature) {
		for (BleProfile profile: registeredProfiles) {
			if (profile instanceof DataExchangerProfile) {
				return ((DataExchangerProfile) profile).sendCmdCh(data, signature);
			}
		}

		return false;
	}

	public boolean enableCmdChannel(boolean enabled) {
		for (BleProfile profile: registeredProfiles) {
			if (profile instanceof DataExchangerProfile) {
				return ((DataExchangerProfile) profile).enableRx2Notification(enabled);
			}
		}

		return false;
	}

	public boolean enableTxCredit(boolean enabled) {
		for (BleProfile profile: registeredProfiles) {
			if (profile instanceof DataExchangerProfile) {
				return ((DataExchangerProfile) profile).enableTxCreditNotification(enabled);
			}
		}

		return false;
	}

	public boolean readTxCredit() {
		for (BleProfile profile: registeredProfiles) {
			if (profile instanceof DataExchangerProfile) {
				return ((DataExchangerProfile) profile).readTxCredit();
			}
		}

		return false;
	}

	public boolean writeTxCreditReportLoopCount(int count) {
		for (BleProfile profile: registeredProfiles) {
			if (profile instanceof DataExchangerProfile) {
				return ((DataExchangerProfile) profile).writeTxCreditReportLoopCount(count);
			}
		}
		return false;
	}
}
