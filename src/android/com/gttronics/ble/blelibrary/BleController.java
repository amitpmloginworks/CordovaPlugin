package com.gttronics.ble.blelibrary;

import java.util.*;

import android.bluetooth.*;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.os.Looper;

public class BleController 
{
	private static BleController bleController;
	
	private static BluetoothAdapter 	mBtAdapter = null;
	private static Context 				mContext = null;
    private static boolean 				mScanning = false;
	private static Handler 				mHandler = new Handler(Looper.getMainLooper());
	private static Handler              mTimerHandler = new Handler(Looper.getMainLooper());
	
	private HashSet<BleDevice> 			            mRegisteredDevices = new HashSet<BleDevice>();
    private HashMap<String, HashSet<BleDevice>> 	mPrematchedDeviceSets = new HashMap<String, HashSet<BleDevice>>();
	private HashMap<String, BluetoothDevice>        mScannedBtDevices = new HashMap<String, BluetoothDevice>();

	private BleControllerCallback 		mDelegate;

	private static final String 		TAG = "[BLE_C]";

	private static final boolean mEnableScanRestart = false;

	private Runnable mTimerRunnable = new Runnable() {

		@Override
		public void run() {
			if( mScanning )
			{
				mBtAdapter.getBluetoothLeScanner().stopScan(mLeScanCallback);
				startScanInternal();
			}
			else
			{
				Log.d(TAG, "Disarm scan timer due to not in scanning state");
				mTimerHandler.removeCallbacks(mTimerRunnable);
			}
		}
	};


	//private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback()
	private ScanCallback mLeScanCallback = new ScanCallback()
	{
		@Override
		public void onScanResult(final int callbackType, final ScanResult result) {
			super.onScanResult(callbackType, result);

			if( mEnableScanRestart ) {
				// Cancel the timer
				Log.d(TAG, "Disarm scan timer");
				mTimerHandler.removeCallbacks(mTimerRunnable);
			}

			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					BleDevice dev = matchScanRecord(result.getRssi(), result.getScanRecord().getBytes());

					if( dev == null )
					{
						// Registered device not found, report to delegate if assigned
						if( mDelegate != null )
						{
							mDelegate.onUnknownDeviceDiscovered(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes());
						}
						Log.d(TAG, "Unrecognized Device [" + result.getDevice().getName() + "] Rssi:" + result.getRssi());
					}
					else {
						// Registered device found
						// - try to connect if auto connect is set,
						// - otherwise report to device's delegate
						//

						String uuid = UUID.nameUUIDFromBytes(result.getDevice().getAddress().getBytes()).toString();
						
						// Add the peripheral (android BLE device) in the discovered list
						mScannedBtDevices.put(uuid, result.getDevice());

						// Add the BleDevice into prematched list
                        HashSet<BleDevice> set = mPrematchedDeviceSets.get(uuid);
                        if( set == null ) {
                            HashSet<BleDevice> newSet = new HashSet<BleDevice>();
                            newSet.add(dev);
                            mPrematchedDeviceSets.put(uuid, newSet);
                        }
                        else {
                            set.add(dev);
                        }

						// report discovered
						if (mDelegate != null)
							mDelegate.onDeviceDiscovered(dev, result.getDevice(), result.getRssi(), result.getScanRecord().getBytes());

					}

					// mleung
                    // - FIXME: don't know what it is
					if( mEnableScanRestart ) {
						if (mScanning) {
							// Rearm the scan protect timer
							Log.d(TAG, "Rearm scan timer");
							mTimerHandler.postDelayed(mTimerRunnable, 5000);
						} else {
							// sometime we see the scan still going on even stopScan is requested.
							mBtAdapter.getBluetoothLeScanner().stopScan(mLeScanCallback);
						}
					}
				}
			});
		}
	};
	
	/**
	 * Get single instance of BleController by providing application context from activity.
	 * 
	 * @param context Application context from activity.
	 * @return
	 */
	public static BleController getInstance(Context context) 
	{
		mContext = context; 
		   
	    if (bleController == null)
	    {
	    	bleController = new BleController();
	    }
	   	    
	    return bleController;
	}

	private BleController() 
	{		
		BluetoothManager bluetoothManager =
		        (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
		mBtAdapter = bluetoothManager.getAdapter();
		
	}

    public boolean isBtOn() {
        return mBtAdapter.isEnabled();
    }
		
	/**
	 * Check if the device supports BLE
	 * 
	 * @return	true if device supports BLE, otherwise false
	 */
	public boolean isBleSupport() 
	{
		if (mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) 
		{
			return true;
		}
		
		return false;
	}

	/**
	 * Check if bluetooth of the device is enabled
	 * 
	 * @return	true if bluetooth is enabled, otherwise false
	 */
	public boolean isBluetoothEnabled() 
	{
		if (isBleSupport()) 
		{
			if (mBtAdapter != null) 
			{
				if (mBtAdapter.isEnabled()) 
				{
					return true;
				}
			}
		}
			
		return false;
	}

	public boolean isAnyConnected()
    {
        for( BleDevice dev : mRegisteredDevices )
        {
            if( dev.isConnected() )
            {
                return true;
            }
        }

        return false;
    }

	public boolean isConnected(String uuid) {

        for( BleDevice dev : mRegisteredDevices )
        {
            if( dev.isConnected() && dev.mUUID.equals(uuid))
            {
                return true;
            }
        }
        
        return false;
	}

	public void setDelegate(BleControllerCallback callback)
	{
		mDelegate = callback;
	}

	private void startScanInternal ()
	{
		UUID[] uuids = allRegisteredServiceUUIDs();

		Log.d(TAG, "Service UUIDs: " + Arrays.toString(uuids));

		if( uuids == null || uuids.length == 0 )
		{
			// mBtAdapter.startLeScan(mLeScanCallback);
			mBtAdapter.getBluetoothLeScanner().startScan(mLeScanCallback);
		}
		else
		{
			// mBtAdapter.startLeScan(mLeScanCallback);

			List<ScanFilter> filters = new ArrayList<ScanFilter>();
			for( UUID uuid : uuids )
			{
				ScanFilter scanFilter = new ScanFilter.Builder()
						.setServiceUuid(ParcelUuid.fromString(uuid.toString()))
						.build();
				filters.add(scanFilter);
			}

			ScanSettings settings = new ScanSettings.Builder()
					.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
					.build();

			mBtAdapter.getBluetoothLeScanner().startScan(filters, settings, mLeScanCallback);
		}

		if( mEnableScanRestart ) {
			// Arm the scan protect timer
			Log.d(TAG, "Arm scan timer");
			mTimerHandler.postDelayed(mTimerRunnable, 5000);
		}
	}
	
	public void startScan()
	{
		if( mScanning )
		{
	        Log.d(TAG, "scan already started" );
			return;
		}

		mScanning = true;

		startScanInternal();
	}
	
	public void stopScan()
	{
		if( !mScanning )
		{
	        Log.d(TAG, "scan already stopped" );
			return;
		}
		
		//mBtAdapter.stopLeScan(mLeScanCallback);
		mBtAdapter.getBluetoothLeScanner().stopScan(mLeScanCallback);
		mScanning = false;
		Log.d(TAG, "scan stopped" );

		if( mEnableScanRestart )
		{
			mTimerHandler.removeCallbacks(mTimerRunnable);
		}
	}

	public boolean disconnect(String uuid) {
		BleDevice dev = null;
        for( BleDevice d : mRegisteredDevices )
        {
            if( d.isConnected() && d.mUUID.equals(uuid))
            {
                dev = d;
            }
        }
		if (dev == null) {
			Log.e(TAG, "TAG1 - Failed disconnect, no device connected");
			return false;
		}

		Log.d(TAG, "TAG1 - on device disconnected");
		dev.disconnect();

		return true;
	}

	public void disconnectAll() {
	    for( BleDevice dev : mRegisteredDevices )
        {
            if( dev.isConnected() )
            {
                dev.disconnect();
            }
        }
    }

	public void registerDevice(BleDevice device)
	{
		mRegisteredDevices.add(device);
	}

	public void clear() {
		stopScan();
		mBtAdapter = null;
		mScanning = false;
		mContext = null;
		mDelegate = null;

		for( BleDevice dev : mRegisteredDevices ) {
            dev.close();
        }

		mScannedBtDevices.clear();
        mPrematchedDeviceSets.clear();

		bleController = null;
	}

	public BleDevice connectDevice(String uuid) {
        for( BleDevice d : mRegisteredDevices )
        {
            if( d.isConnected() && d.mUUID.equals(uuid))
            {
                Log.e(TAG, "Device is already connected.");
                return null;
            }
        }

        BluetoothDevice btDev = mScannedBtDevices.get(uuid);
        if( btDev == null )
        {
            // Unknown device
            Log.e(TAG, "Unknown device.");
            return null;
        }

	    BleDevice dev = null;

        HashSet<BleDevice> set = mPrematchedDeviceSets.get(uuid);
        if( set == null )
        {
            // No prematched device found
            // - FIXME: we should be able to search the register list to find a matching one
            // - for now just return with error
            Log.e(TAG, "No prematched device.");
            return null;
        }
        else
        {
            for( BleDevice d : set )
            {
                if( d.mState == BleDeviceCallback.BLE_DEV_STATE.IDLE )
                {
                    dev = d;
                }
            }
        }

		if (dev == null ) {
			Log.e(TAG, "All devices are connected.");
			return null;
		}

		// Tell BleDevice to connect
		dev.connect(btDev);
        dev.mUUID = uuid;

		return dev;
	}

	public BleDevice getConnectedDevice(String uuid) {
        for( BleDevice d : mRegisteredDevices )
        {
            if( d.isConnected() && d.mUUID.equals(uuid))
            {
                return d;
            }
        }
        
        return null;
    }

    private synchronized BleDevice matchScanRecord(final int rssi, byte[] scanRecord)
    {
    	int score = 0;
    	BleDevice targetDev = null;
    	
    	for( BleDevice d : mRegisteredDevices )
    	{
    		int tmpScore = d.evaluateDeviceMatchingScore(rssi, scanRecord);
    		if( tmpScore >=0 )
    		{
    			if( tmpScore > score || targetDev == null )
    			{
    				targetDev = d;
    				score = tmpScore;
    			}
    		}
    	}
    	
    	return targetDev;
    }
    
    UUID[] allRegisteredServiceUUIDs()
    {
    	Set<UUID> suuids = new LinkedHashSet<UUID>();
    	
    	for( BleDevice d : mRegisteredDevices )
    	{
    		suuids.addAll(d.listOfServiceUUIDs());
    	}
    	
    	return suuids.toArray(new UUID[0]);
    }   
}
