package com.gttronics.ble.blelibrary;

import android.bluetooth.BluetoothGattCharacteristic;
import android.nfc.Tag;
import android.util.Log;

public interface BleDeviceCallback
{
	enum BLE_DEV_STATE
	{
		IDLE,
		CONNECTING,
		CONNECTED_NOT_SVC_RDY,
		DISCONNECTING,
		DISCONNECTED,
		CONNECTED_SVC_RDY,
		DLGE_META
	}

	static final String TAG = "BLE_DEV_CLLB";

	void onDeviceStateChanged(BleDevice device, BLE_DEV_STATE state);
	void onAllProfilesReady(BleDevice devive, boolean isAllReady);

	//void onUpdateValueForCharacteristic(BluetoothGattCharacteristic c);
}
