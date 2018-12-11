package com.gttronics.ble.blelibrary;

import android.bluetooth.BluetoothDevice;

public interface BleControllerCallback 
{
	void onUnknownDeviceDiscovered(BluetoothDevice btDev, int rssi, byte[] scanRecord);

	// mleung 20180426
	// - added a new parameter BleDevice for its auto connect status
	void onDeviceDiscovered(BleDevice bleDev, BluetoothDevice btDev, int rssi, byte[] scanRecord);
}
