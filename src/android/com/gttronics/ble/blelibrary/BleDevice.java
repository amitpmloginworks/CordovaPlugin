package com.gttronics.ble.blelibrary;

import java.util.*;

import com.gttronics.ble.blelibrary.BleDeviceCallback.BLE_DEV_STATE;
import com.gttronics.ble.blelibrary.dataexchanger.DataExchangerProfile;

import android.bluetooth.*;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.os.Looper;

public class BleDevice 
{
	public  BLE_DEV_STATE		mState = BLE_DEV_STATE.IDLE;
	public  String				mUUID = null;

	private BluetoothDevice 	mBtDevice = null;
	private boolean				mAutoConnect = true;
	private BleDeviceCallback	mDelegate = null;
	private BluetoothGatt 		mGattClient = null;
	private Context 			mContext = null;
	private Timer 				mRssiTimer = new Timer();
	private Handler				mHandler = new Handler(Looper.getMainLooper());
	private BleDevice			mSelf;
	private String				mDevName;
	private boolean				mRssiReadEnabled = false;
	private int					mRssiReadPeriod = 1000;
	
	protected HashSet<BleProfile>	registeredProfiles = new HashSet<BleProfile>();

	protected static final String TAG = "[BLE_D]";
	protected static final UUID CHAR_UPDATE_NOTI_DESC_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"); 

	public void setAutoConnect(boolean flag)
	{
		mAutoConnect = flag;
	}
	public boolean isAutoConnect() { return mAutoConnect; }
	
	public void setRssiReadEnabled(boolean flag)
	{
		mRssiReadEnabled = flag;
		
		if( flag == true && mState == BLE_DEV_STATE.CONNECTED_SVC_RDY )
		{
			if( mGattClient != null )
			{
				mGattClient.readRemoteRssi();
			}
		}
		else if( flag == false )
		{
			mRssiTimer.cancel();
			mRssiTimer.purge();
		}
	}
	
	public void setRssiReadPeriod(int val)
	{
		if( val < 500 )
		{
			val = 1000;
		}
		mRssiReadPeriod = val;
	}

	public BluetoothDevice getBtDevice() {
		return mBtDevice;
	}
	
	public BleDevice(Context context, BleDeviceCallback callback)
	{
		mContext = context;
		mDelegate = callback;
		mSelf = this;
	}
	
	public boolean isConnected()
	{
		return (mState == BLE_DEV_STATE.CONNECTED_NOT_SVC_RDY || mState == BLE_DEV_STATE.CONNECTED_SVC_RDY || mState == BLE_DEV_STATE.DLGE_META );
	}

	public void registerProfile( BleProfile profile )
	{
		registeredProfiles.add(profile);
	}

	private BluetoothGattCallback mGattCallbacks = new BluetoothGattCallback() 
	{
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) 
		{
			super.onConnectionStateChange(gatt, status, newState);
			
			if (newState == BluetoothProfile.STATE_CONNECTED && 
				mGattClient != null) 
			{
				mGattClient.discoverServices();
				mState = BLE_DEV_STATE.CONNECTED_NOT_SVC_RDY;
				if( mDelegate != null )
				{
		        	mHandler.post(new Runnable() 
		        	{
		                @Override
		                public void run() 
		                {
		    				mDelegate.onDeviceStateChanged(mSelf, mState);
		                }
		            });  
				}	        					
		        //Log.d(TAG, "[" + mDevName + "] connected and discovering services ..." );
			} 
			
			else if (newState == BluetoothProfile.STATE_DISCONNECTED) 
			{
				if( mState == BLE_DEV_STATE.IDLE )
				{
					Log.e( TAG, "unexpected disconnect msg from GATT" );
				}
				else
				{
					mState = BLE_DEV_STATE.DISCONNECTED;

					close();

					if( mDelegate != null )
					{
						mHandler.post(new Runnable()
						{
							@Override
							public void run()
							{
								mDelegate.onDeviceStateChanged(mSelf, mState);
							}
						});
					}
				}
			}
		}

		@Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) 
        {
            super.onReadRemoteRssi(gatt, rssi, status);
            
            if( mRssiReadEnabled == true )
            {
                mRssiTimer.schedule( new TimerTask() 
                {
                	@Override
                	public void run() 
                	{
            			mGattClient.readRemoteRssi();	
                	}
                }, mRssiReadPeriod);
            }
 		}
		
		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) 
		{
			if (status == BluetoothGatt.GATT_SUCCESS) 
			{
                //performNotificationRequest(BtSmartUuid.KF_SERVICE.getUuid(), BtSmartUuid.KF_KEY_PRESSED.getUuid());
				

				// loop through all registered profiles
				// - report service discovered
				//
				for( BleProfile p : registeredProfiles )
				{
					p.reportServiceDiscovered(mGattClient);
				}
			
				mState = BLE_DEV_STATE.CONNECTED_SVC_RDY;
				if( mDelegate != null )
				{
		        	mHandler.post(new Runnable() 
					{
		                @Override
		                public void run() 
		                {
	    					mDelegate.onDeviceStateChanged(mSelf, mState);
		                }
		            });  
				}	        	
	        	//Log.d(TAG, "[" + mDevName + "] connected service ready" );
                
                // run the 1st time
	        	if( mRssiReadEnabled )
	        	{
	        		gatt.readRemoteRssi();
	        	}
			}
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) 
		{
			// A notification for a characteristic has been received, so notify
			// the registered Handler.
			for( BleProfile p : registeredProfiles)
			{
				p.onCharacteristicsChanged(characteristic);
			}
		}

		@Override
		public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) 
		{
            Log.d(TAG, "onDescriptorRead");
		}

		/**
		 * After writing the CCC for a notification this callback should trigger. It could also be called when a
		 * descriptor write was requested directly, so that case is handled too.
		 */
		@Override
		public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) 
		{
			if (status == BluetoothGatt.GATT_SUCCESS)
			{
				for( BleProfile p : registeredProfiles)
				{
					p.onDescriptorWrite(gatt, descriptor, status);
				}
			}

		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) 
		{
			if (status == BluetoothGatt.GATT_SUCCESS)
			{
				for( BleProfile p : registeredProfiles)
				{
					p.onCharacteristicsRead(characteristic);
				}
			} 
		}	
		
		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) 
		{
			if (status == BluetoothGatt.GATT_SUCCESS)
			{
				for( BleProfile p : registeredProfiles)
				{
					p.onCharacteristicsWrite(characteristic);
				}
			} 
		}

	}; 

	public boolean enableNotification(String suuid, String cuuid, boolean enable) {
		BluetoothGattCharacteristic c = mGattClient.getService(UUID.fromString(suuid)).getCharacteristic(UUID.fromString(cuuid));
		return enableNotification(c, enable);
	}

	protected boolean enableNotification(BluetoothGattCharacteristic characteristic, boolean enable)
	{
		if (mGattClient == null) 
		{
			throw new NullPointerException("GATT client not started.");
		}
		
		if (!mGattClient.setCharacteristicNotification(characteristic, enable)) 
		{
			return false;
		}
		
		BluetoothGattDescriptor clientConfig = characteristic.getDescriptor(CHAR_UPDATE_NOTI_DESC_UUID);
		if (clientConfig == null) 
		{
			return false;
		}
		
		if (enable) 
		{
			clientConfig.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
		}
		else 
		{
			clientConfig.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
		}

		boolean success =  mGattClient.writeDescriptor(clientConfig);
		Log.d(TAG, characteristic.getUuid().toString() + " notification is " + (success?"successfully ":"unsuccessfully ") + (enable?"enabled ":"disabled "));
		return success;
	}

	public boolean writeValue( UUID suuid, UUID cuuid, byte[] data )
	{
		if (mGattClient == null)
			return false;

		BluetoothGattCharacteristic c = mGattClient.getService(suuid).getCharacteristic(cuuid);
		c.setValue(data);
		
		return mGattClient.writeCharacteristic(c);
	}

	public boolean writeValue(UUID suuid, UUID cuuid, byte[] data, boolean response) {

		if (mGattClient == null)
			return false;

		BluetoothGattService srv = mGattClient.getService(suuid);
		if (srv == null)
			return false;
		BluetoothGattCharacteristic c = srv.getCharacteristic(cuuid);
		c.setValue(data);
		c.setWriteType(response?
				BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT:
				BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

		return mGattClient.writeCharacteristic(c);
	}

//	public boolean writeValue( UUID suuid, UUID cuuid, int val, boolean unsigned )
//	{
//		int formatTyp = BluetoothGattCharacteristic.FORMAT_SINT32;
//		if( unsigned )
//		{
//			formatTyp = BluetoothGattCharacteristic.FORMAT_UINT32;
//		}
//
//		BluetoothGattCharacteristic c = mGattClient.getService(suuid).getCharacteristic(cuuid);
//		c.setValue(val, formatTyp, 0);
//
//		return mGattClient.writeCharacteristic(c);
//	}
//
//	public boolean writeValue( UUID suuid, UUID cuuid, short val, boolean unsigned )
//	{
//		int formatTyp = BluetoothGattCharacteristic.FORMAT_SINT16;
//		if( unsigned )
//		{
//			formatTyp = BluetoothGattCharacteristic.FORMAT_UINT16;
//		}
//
//		BluetoothGattCharacteristic c = mGattClient.getService(suuid).getCharacteristic(cuuid);
//		c.setValue(val, formatTyp, 0);
//
//		return mGattClient.writeCharacteristic(c);
//	}
//
	public boolean readValue(UUID suuid, UUID cuuid)
	{
		BluetoothGattCharacteristic c = mGattClient.getService(suuid).getCharacteristic(cuuid);
		return mGattClient.readCharacteristic(c);
	}

//	public String readValueForCharacteristic(UUID suuid, UUID cuuid) {
//		//TODO implement
//		BluetoothGattCharacteristic c = mGattClient.getService(suuid).getCharacteristic(cuuid);
//		byte[] binValue = c.getValue();
//		String value = new String(binValue);
//		mGattClient.readCharacteristic(c);
//		mDelegate.onUpdateValueForCharacteristic(c);
//
//		return value;
//	}

	public void profileReportReady( BleProfile profile, boolean isReady )
	{
		for( BleProfile p : registeredProfiles )
		{
			if( !p.isReady() )
			{
				return;
			}
		}
		
		if( mDelegate != null )
		{
			//Log.d(TAG, "\"" + mDevName + "\" connected successfully.");
        	mHandler.post(new Runnable() 
        	{
                @Override
                public void run() 
                {
                	mDelegate.onAllProfilesReady(mSelf, true);
                }
            });  
		}
	}

	void connect(BluetoothDevice dev)
	{		
		mBtDevice = dev;

		if( mDelegate == null )
		{
			return;
		}
		
		// Reset all registered profiles
		for( BleProfile p : registeredProfiles )
		{
			p.reset();
		}

		mState = BLE_DEV_STATE.CONNECTING;

        mHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                mGattClient = mBtDevice.connectGatt(mContext, false, mGattCallbacks);

				mDevName = mBtDevice.getName();
				mUUID = UUID.nameUUIDFromBytes(mBtDevice.getAddress().getBytes()).toString();

                if( mDelegate != null )
                {
                    mHandler.post(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            mDelegate.onDeviceStateChanged(mSelf, mState);
                        }
                    });
                }

                //Log.d(TAG, "\"" + mDevName + "\" connecting ..." );
            }
        });

	}
	
	public void disconnect()
	{
		if (mGattClient == null) 
		{
			throw new NullPointerException("GATT client not started.");
		}
		
		if( isConnected() )
		{
            mHandler.post(new Runnable()
            {
                @Override
                public void run()
                {
                    mState = BLE_DEV_STATE.DISCONNECTING;
                    mGattClient.disconnect();

                    Log.e(TAG, "disconnecting from device");
                    if( mDelegate != null )
                    {
                        mHandler.post(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                mDelegate.onDeviceStateChanged(mSelf, mState);
                            }
                        });
                    }

					//Log.d(TAG, "\"" + mDevName + "\" disconnecting ..." );
				}
            });

		}
		else
		{
			Log.e(TAG, "device is not connected - " + mState);
		}
	}
	
	public void close()
	{

		if( mState == BLE_DEV_STATE.DISCONNECTED )
		{
			if (mGattClient == null)
			{
				throw new NullPointerException("GATT client not started.");
			}

				mBtDevice = null;
			mGattClient.close();
			mGattClient = null;


			Log.d(TAG, "[" + mDevName + "] disconnected" );
		}
		else if( isConnected() ||
				 mState == BLE_DEV_STATE.DLGE_META )

		{
			disconnect();
			Log.d(TAG, "[" + mDevName + "] disconnecting." );
		}
		else if( mState == BLE_DEV_STATE.DISCONNECTING )
		{
			Log.d(TAG, "[" + mDevName + "] still disconnecting." );
		}
		else if( mState == BLE_DEV_STATE.IDLE )
		{
			Log.d(TAG, "device is idling." );
			mUUID = null;
		}
		else
		{
			Log.d(TAG, "[" + mDevName + "] Unknown state." );
		}
	}

	public void decommission()
	{
		mState = BLE_DEV_STATE.IDLE;
		mDevName = null;
		mUUID = null;
	}
	
	protected int evaluateDeviceMatchingScore(final int rssi, byte[] scanRecord)
	{
        return -1;
	}
	
	public List<UUID> listOfServiceUUIDs()
	{
		Set<UUID> suuids = new LinkedHashSet<UUID>();
    
        for( BleProfile p : registeredProfiles )
        {
        	suuids.addAll(p.hashSetOfServiceUUIDs());
        }
        
        return new ArrayList<UUID>(suuids);
	}

	public String getDeviceName() { return mDevName; }
}
