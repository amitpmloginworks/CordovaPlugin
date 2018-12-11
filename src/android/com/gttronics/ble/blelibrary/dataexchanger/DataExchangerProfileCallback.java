package com.gttronics.ble.blelibrary.dataexchanger;

import android.bluetooth.BluetoothGattCharacteristic;

import com.gttronics.ble.blelibrary.*;

public interface DataExchangerProfileCallback {
	void onRxDataAvailable(DataExchangerDevice dev, byte[] data);
	void onRx2DataAvailable(DataExchangerDevice dev, byte[] data);
	void onTxCreditDataAvailable(DataExchangerDevice dev, byte[] data);
	void onCharacteristicWrite(DataExchangerDevice dev, int signature);
	//void onCharacteristicDiscovered(BluetoothGattCharacteristic c);
}
