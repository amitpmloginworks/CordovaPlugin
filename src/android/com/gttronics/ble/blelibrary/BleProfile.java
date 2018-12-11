package com.gttronics.ble.blelibrary;

import java.util.*;
import android.bluetooth.*;
import android.util.Log;

public class BleProfile 
{
	protected Hashtable<UUID,HashSet<UUID>> mUuidTbl 			   = new Hashtable<UUID,HashSet<UUID>>();
	protected BleDevice						mDevice  			   = null;
	protected boolean						mIsReady 			   = false;
	protected HashSet<UUID> 			    mPrimaryServiceUUIDs   = new HashSet<UUID>();


	private final static String TAG = "[BLE_P]";
	
	public BleProfile(BleDevice dev)
	{
		mDevice = dev;
	}
	
	public boolean isReady()
	{
		return mIsReady;
	}
	
	protected void reset()
	{
		mIsReady = false;
	}
	
	void reportServiceDiscovered(BluetoothGatt gatt)
	{
		List<BluetoothGattService> ss = gatt.getServices();
		for( BluetoothGattService s : ss )
		{
			List<BluetoothGattCharacteristic> cs = s.getCharacteristics();
			for( BluetoothGattCharacteristic c : cs)
			{
				reportDiscoveredCharacteristic(c);
			}
		}
	}
	
	public HashSet<UUID> hashSetOfServiceUUIDs()
	{
	    // Get the primary service UUIDs instead of all service UUIDs
		//return new HashSet<UUID>(mUuidTbl.keySet());
		return new HashSet<UUID>(mPrimaryServiceUUIDs);
	}
	
	protected void reportDiscoveredCharacteristic(BluetoothGattCharacteristic c)
	{
		// This function should normally be subclassed
		Log.d(TAG, "S[" + c.getService().getUuid().toString() + "] C[" + c.getUuid().toString() +"] discovered");
	}
	
	protected void onCharacteristicsChanged(BluetoothGattCharacteristic c)
	{
		// This function should normally be subclassed
		Log.d(TAG, "S[" + c.getService().getUuid().toString() + "] C[" + c.getUuid().toString() +"] changed");
	}
	
	protected void onCharacteristicsRead(BluetoothGattCharacteristic c)
	{
		// This function should normally be subclassed
		Log.d(TAG, "S[" + c.getService().getUuid().toString() + "] C[" + c.getUuid().toString() +"] read");
	}

	protected void onCharacteristicsWrite(BluetoothGattCharacteristic c)
	{
		// This function should normally be subclassed
		Log.d(TAG, "S[" + c.getService().getUuid().toString() + "] C[" + c.getUuid().toString() +"] write");
	}

	protected void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
		//This function should normally be subclassed
		Log.d(TAG, "Descriptor[" + descriptor.getUuid().toString() + "] write");
	}
}
