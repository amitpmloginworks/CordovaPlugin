package com.gttronics.ble.blelibrary.dataexchanger;

import com.gttronics.ble.blelibrary.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.os.Looper;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;

class QueueItem
{
    public enum Channel {TX, TX2, TXC};
    
    public byte[] bytes;
    public Channel ch;
    public boolean isRsp;
    public int signature;
    public boolean isLast;
}

public class DataExchangerProfile extends BleProfile
{
	public static final String SVC_UUID    	    = "e0678212-80ae-49d6-a337-fb4786fcbc14";
	public static final String SVC_UUID2        = "7615cafe-5d2b-4ee8-a868-8ee93160a6ca";
	public static final String TX_UUID        	= "4498f6ce-c8d3-4826-9454-3da3702a33e4";
	public static final String RX_UUID       	= "7a27450e-5a7c-4749-bfd3-ad0b0750294d";
	public static final String TX2_UUID      	= "74da350a-68f7-45e0-8346-7edd286f4cc1";
	public static final String RX2_UUID      	= "4f547a21-9d03-44ec-95ce-5118bd60228e";
	public static final String TX_CREDIT_UUID 	= "957e6cde-a96a-4536-992b-2f249c291421";
	public static final String RX_CREDIT_UUID 	= "bf5aefc2-e53f-45e6-87e9-b5ccf264a274";
	public static final String DX_SCRAMBLE_UUID = "f4edc3c5-7a5c-4c26-b967-112d398883c2";


	private static final String UUID_180A   = "0000180A-0000-1000-8000-00805F9B34FB";
	private static final String UUID_2A29   = "00002A29-0000-1000-8000-00805F9B34FB";
	private static final String UUID_2A00   = "00002A00-0000-1000-8000-00805F9B34FB";
	private static final String UUID_1801   = "00001801-0000-1000-8000-00805F9B34FB";

	private static final ReentrantLock lock = new ReentrantLock();


	private final static String TAG = "[DX_P]";
	
	protected boolean mHasTx  				= false;
	protected boolean mHasRx  				= false;
	protected boolean mHasTx2				= false;
	protected boolean mHasRx2 				= false;
	protected boolean mHasTxCredit			= false;
	protected boolean mHasRxCredit			= false;
	protected boolean mHasChScrmb			= false;

	protected boolean mIsRxNotiEnabled 		 = false;
	protected boolean mIsRx2NotiEnabled 	 = false;
	protected boolean mIsTxCreditNotiEnabled = false;
	protected boolean mIsChScrmbEnabled      = false;


	protected boolean mShouldRxEnable       = true;
	protected boolean mShouldRx2Enable      = true;
	protected boolean mShouldTxCreditEnable = true;
	protected boolean mShouldChScrmbEnabled = false;
    private   boolean mShouldReportReady 	= true;

    private byte[] mScrambler = new byte[256];
    private byte[] mDescrambler = new byte[256];
    private int mTmpSeed = 0;
    private int mScramblerSeed;
    private int mDescrambleSeed;

	private AtomicBoolean mIsSending        = new AtomicBoolean();
    private List<QueueItem> mSendQ          = new ArrayList<QueueItem>();
    private UUID mCurServiceUUID;

	private DataExchangerProfileCallback	mDelegate = null;
	private Handler 						mHandler  = new Handler(Looper.getMainLooper());
	
	private int mLockTimeout = 2000;
	
	private Timer mSendLockTimer;
    private Timer mCheckReadyTimer;

	public DataExchangerProfile(BleDevice dev, DataExchangerProfileCallback callback)
	{
		super(dev);

		mPrimaryServiceUUIDs.add(UUID.fromString(SVC_UUID));
		mPrimaryServiceUUIDs.add(UUID.fromString(SVC_UUID2));

		mDelegate = callback;
	}

	public DataExchangerProfile(BleDevice dev, List<String> serviceUUIDs, DataExchangerProfileCallback callback)
	{
		super(dev);

		if( serviceUUIDs != null )
		{
			for( String uuidStr : serviceUUIDs )
			{
				mPrimaryServiceUUIDs.add(UUID.fromString(uuidStr));
			}
		}
		else
		{
			mPrimaryServiceUUIDs.add(UUID.fromString(SVC_UUID));
			mPrimaryServiceUUIDs.add(UUID.fromString(SVC_UUID2));
		}

		// Super Beacon Service
		for( UUID suuid : mPrimaryServiceUUIDs )
		{
			HashSet<UUID> h = new HashSet<UUID>();
			h.add(UUID.fromString(TX_UUID));
			h.add(UUID.fromString(RX_UUID));
			h.add(UUID.fromString(TX2_UUID));
			h.add(UUID.fromString(RX2_UUID));
			h.add(UUID.fromString(TX_CREDIT_UUID));
			h.add(UUID.fromString(RX_CREDIT_UUID));
			h.add(UUID.fromString(DX_SCRAMBLE_UUID));
			mUuidTbl.put(suuid, h);
		}

		// GAP Device Info
		HashSet<UUID> h = new HashSet<UUID>();
		UUID suuid = UUID.fromString(UUID_180A);
		h.add(UUID.fromString(UUID_2A29));
		mUuidTbl.put(suuid, h);

		// GATT Device Name
		h = new HashSet<UUID>();
		suuid = UUID.fromString(UUID_1801);
		h.add(UUID.fromString(UUID_2A00));
		mUuidTbl.put(suuid, h);

		mDelegate = callback;
	}
	
	public void reset()
	{
		super.reset();
		mHasTx = mHasRx = mHasTx2 = mHasRx2 = mHasTxCredit = mHasChScrmb = false;
		mIsRxNotiEnabled = mIsRx2NotiEnabled = mIsTxCreditNotiEnabled = mIsChScrmbEnabled = false;
		mIsReady = false;
        mSendQ.clear();
	}
	
	public void setShouldReportReady(boolean flag)
	{
		mShouldReportReady = flag;
	}
	
	private boolean checkReady()
	{
//		if( mHasTx && mHasRx )
//		{
//			return true;
//		}

		// mleung
		// if( mHasTx && mHasRx && mHasTx2 && mHasRx2 && mHasChScrmb)

		boolean ready = true;

		if( ready && mShouldRxEnable )
		{
			if( !(mHasTx && mHasRx) )
				ready = false;
		}

		if( ready && mShouldRx2Enable )
		{
			if( !(mHasTx2 && mHasRx2) )
				ready = false;
		}

		if( ready && mShouldChScrmbEnabled )
		{
			if( !mHasChScrmb )
				ready = false;
		}

		return ready;
	}

	@Override
	protected void reportDiscoveredCharacteristic(BluetoothGattCharacteristic c)
	{
		UUID suuid = c.getService().getUuid();
		if( mPrimaryServiceUUIDs.contains(suuid) )
		{
			//check if TX RX characteristic present
			if( c.getUuid().equals(UUID.fromString(TX_UUID)) )
			{
				mCurServiceUUID = suuid;
				mHasTx = true;
				Log.d(TAG, "Tx CHAR discovered");
			}			
			else if( c.getUuid().equals(UUID.fromString(RX_UUID)) )
			{
				mHasRx = true;
				Log.d(TAG, "Rx CHAR discovered");
			}

			//check if RX2 TX2 characteristic presented
			else if (c.getUuid().equals(UUID.fromString(TX2_UUID))) {
				mHasTx2 = true;
				Log.d(TAG, "Tx2 CHAR discovered");
			}
			else if (c.getUuid().equals(UUID.fromString(RX2_UUID))) {
				mHasRx2 = true;
				Log.d(TAG, "Rx2 CHAR discovered");
			}

			//check if RX credit TX credit characteristic present
			else if(c.getUuid().equals(UUID.fromString(TX_CREDIT_UUID))) {
				mHasTxCredit = true;
				Log.d(TAG, "TxCredit CHAR discovered");

			}
			else if (c.getUuid().equals(UUID.fromString(RX_CREDIT_UUID))) {
				mHasRxCredit = true;
				Log.d(TAG, "RxCredit CHAR discovered");
			}

			else if (c.getUuid().equals(UUID.fromString(DX_SCRAMBLE_UUID))) {
				mHasChScrmb = true;
				if( mShouldChScrmbEnabled )
				{
					mIsChScrmbEnabled = true;
				}
				Log.d(TAG, "DxSrambler CHAR discovered");

			}
			else {
				Log.d(TAG, "Unknown characteristic discovered");
			}

			if( !mIsReady && checkReady() )
			{
				mIsReady = true;
				if( mShouldReportReady )
				{
					enableNotification();
					mHandler .postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (mIsChScrmbEnabled) {
                                // TODO generate random int
                                mTmpSeed = (int) Math.random();
                                byte[] data = ByteBuffer.allocate(4).putInt(mTmpSeed).array();
                                boolean succeed = mDevice.writeValue(mCurServiceUUID, UUID.fromString(DX_SCRAMBLE_UUID), data, true);
                                Log.d(TAG, "DXScrambler enable sent " + (succeed? "succeed" : " failed"));
                            }
                            mDevice.profileReportReady(DataExchangerProfile.this, true);
                        }
                    }, 0);
				}
			}
		}
		
		else if( c.getService().getUuid().equals(UUID.fromString(UUID_180A)) )
		{
			if( c.getUuid().equals(UUID.fromString(UUID_2A29)) )
			{
				Log.d(TAG, "Device Info Name CHAR discovered");
			}		
		}
		
		else if( c.getService().getUuid().equals(UUID.fromString(UUID_1801)) )
		{
			if( c.getUuid().equals(UUID.fromString(UUID_2A00)) )
			{
				Log.d(TAG, "GAP Device Name CHAR discovered");
			}		
		}

		//mDelegate.onCharacteristicDiscovered(c);
	}

	@Override
	protected void onCharacteristicsChanged(BluetoothGattCharacteristic c)
	{
		UUID suuid = c.getService().getUuid();
		if( mCurServiceUUID.equals(suuid) )
		{
			if( c.getUuid().equals(UUID.fromString(RX_UUID)) )
			{
				if( mDelegate != null )
				{
					final byte[] data = descrambleData(c.getValue());
                    //Log.d(TAG, new String(data) + " [" + data.length + "]");
		        	mHandler.post(new Runnable()
		        	{
		                @Override
		                public void run() 
		                {
							mDelegate.onRxDataAvailable((DataExchangerDevice)mDevice, data);
		                }
		            });  
		        }
			}
			else if ( c.getUuid().equals(UUID.fromString(RX2_UUID)) )
			{
				if( mDelegate != null )
				{
					final byte[] data = descrambleData(c.getValue());
					//Log.d(TAG, new String(data) + " [" + data.length + "]");
					mHandler.post(new Runnable()
					{
						@Override
						public void run()
						{
							mDelegate.onRx2DataAvailable((DataExchangerDevice)mDevice, data);
						}
					});
				}
			}
			else if ( c.getUuid().equals(UUID.fromString(TX_CREDIT_UUID)) )
			{
				if( mDelegate != null )
				{
                    final byte[] data = descrambleData(c.getValue());
					mHandler.post(new Runnable()
					{
						@Override
						public void run()
						{
							mDelegate.onTxCreditDataAvailable((DataExchangerDevice)mDevice, data);
						}
					});
				}
			}
            else if (c.getUuid().equals(UUID.fromString(DX_SCRAMBLE_UUID))) {
				byte[] data = c.getValue();
                if (data != null) {
                    byte[] tmp =  new byte[4];
                    System.arraycopy(data, 0, tmp, 0, 4);
                    mScramblerSeed = ByteBuffer.wrap(tmp).getInt();
                    mScrambler = setScramblerWithSeed(mScramblerSeed, mScrambler, false);
                }

                mDescrambleSeed = mTmpSeed;
                if (mDescrambleSeed != 0) {
                    setScramblerWithSeed(mDescrambleSeed, mDescrambler, true);
                }
            }
			else
			{
				super.onCharacteristicsChanged(c);
			}
		}
		else
		{
			Log.d(TAG, "onCharacteristicsChanged - for unknown service");
			super.onCharacteristicsChanged(c);
		}
	}

	@Override
	protected void onCharacteristicsRead(BluetoothGattCharacteristic c)
	{
		UUID suuid = c.getService().getUuid();
		if( mCurServiceUUID.equals(suuid) )
		{
			if( c.getUuid().equals(UUID.fromString(RX_UUID)) )
			{
			    if (c.getUuid().equals(UUID.fromString(TX_CREDIT_UUID))) {
                    if (mDelegate != null) {
                        final byte[] data = c.getValue();
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mDelegate.onTxCreditDataAvailable((DataExchangerDevice)mDevice, data);
                            }
                        });
                    }
                }
			}
		}
	}

	@Override
	protected void onCharacteristicsWrite(BluetoothGattCharacteristic c)
	{
		UUID suuid = c.getService().getUuid();
		if( mCurServiceUUID.equals(suuid) )
		{
			if( c.getUuid().equals(UUID.fromString(TX_UUID))  ||
				c.getUuid().equals(UUID.fromString(TX2_UUID)) ||
				c.getUuid().equals(UUID.fromString(TX_CREDIT_UUID)) )
			{
				if (mSendQ.size() > 0) {
                    QueueItem item = popQueue();
                    
                    // Response to delegate only when signature is not 0
                    if (item.signature > 0) {
                        mDelegate.onCharacteristicWrite((DataExchangerDevice)mDevice, item.signature);
                    }                                    
				}
				else
					Log.e(TAG, "empty queue, something wrong");

                mIsSending.set(false);

                checkQueueAndResend();
			}
            else if (c.getUuid().equals(UUID.fromString(DX_SCRAMBLE_UUID))) {
                Log.d(TAG, "DX Channel Scrambler Enabled " + mDescrambleSeed + ", " + mScramblerSeed);
            }
		}
	}

	@Override
	protected void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
		enableNotification();
	}

    private byte[] descrambleData(byte[] data) {
        if (mDescrambleSeed == 0)
            return data;

        for (int i = 0; i < data.length; i++) {
            data[i] = mDescrambler[data[i]];
        }

        return data;
    }

	private void enableNotification() {
		if (mHasRx && mShouldRxEnable && !mIsRxNotiEnabled) {
			mDevice.enableNotification(SVC_UUID, RX_UUID, true);
			mIsRxNotiEnabled = true;
		}
		else if (mHasRx2 && mShouldRx2Enable && !mIsRx2NotiEnabled) {
			mDevice.enableNotification(SVC_UUID, RX2_UUID, true);
			mIsRx2NotiEnabled = true;
		}
		else if (mHasTxCredit && mShouldTxCreditEnable && !mIsTxCreditNotiEnabled) {
			mDevice.enableNotification(SVC_UUID, TX_CREDIT_UUID, true);
			mIsTxCreditNotiEnabled = true;
		}
	}

	private synchronized QueueItem pushQueueReturnFirstItem(QueueItem item) {
	    mSendQ.add(item);
		return mSendQ.get(0);
	}
	
	private synchronized  QueueItem popQueue() {
	    QueueItem item = mSendQ.get(0);
	    mSendQ.remove(0);
	    return item;
    }

	public boolean sendDataCh(byte[] data, int signature)
	{
		if( !mHasTx )
		{
            Log.d(TAG, "TX characteristic not available");
			return false;
		}

        byte[] bytesLeft = data.clone();

        do {
            QueueItem newItem = new QueueItem();
            newItem.ch = QueueItem.Channel.TX;
            newItem.isRsp = signature > 0 ?true :false;
            newItem.isLast = false;

            if( bytesLeft.length > 20 ) {
                newItem.bytes = Arrays.copyOfRange(bytesLeft, 0, 20);
                bytesLeft = Arrays.copyOfRange(bytesLeft, 20, bytesLeft.length);
            }
            else
            {
                newItem.bytes = bytesLeft;
                newItem.isLast = true;
            }
            pushQueueReturnFirstItem(newItem);
        } while( bytesLeft.length > 20 );

		boolean succeed = false;

		if( mIsSending.compareAndSet(false, true) )
		{
            QueueItem item = mSendQ.get(0);

			succeed = mDevice.writeValue(mCurServiceUUID, UUID.fromString(TX_UUID), item.bytes, item.isRsp);
            Log.d(TAG, "" + "Tx data " + new String(bytesLeft) + " sent " + (succeed? "succeed" : "failed") + ", queue size: " + mSendQ.size());
		}
        else {
            Log.d(TAG, "sendDataCh pending, queue size[" + mSendQ.size() );
        }
		return succeed;
	}

	public boolean sendCmdCh(byte[] data, int signature) {
		if (!mHasTx2) {
			Log.d(TAG, "TX2 characteristic not available");
			return false;
		}

		setTimer();

        byte[] bytesLeft = data.clone();

        do {
            QueueItem newItem = new QueueItem();
            newItem.ch = QueueItem.Channel.TX2;
            newItem.isRsp = signature > 0 ?true :false;
            newItem.isLast = false;

            if( bytesLeft.length > 20 ) {
                newItem.bytes = Arrays.copyOfRange(bytesLeft, 0, 20);
                bytesLeft = Arrays.copyOfRange(bytesLeft, 20, bytesLeft.length);
            }
            else
            {
                newItem.bytes = bytesLeft;
                newItem.isLast = true;
            }
            pushQueueReturnFirstItem(newItem);
        } while( bytesLeft.length > 20 );

        boolean succeed = false;

        if( mIsSending.compareAndSet(false, true) )
        {
            QueueItem item = mSendQ.get(0);

            String cmd = new String(bytesLeft);
            succeed = mDevice.writeValue(mCurServiceUUID, UUID.fromString(TX2_UUID), item.bytes, item.isRsp);

//            // Special case: for AT+IMG, which will not get back
//            if (succeed && cmd.contains("AT+IMG=")) {
//                mSendQ.remove(0);
//            }
            Log.d(TAG, "" + "Tx cmd " + new String(bytesLeft) + " sent " + (succeed? "succeed" : "failed") + ", queue size: " + mSendQ.size());
        }
        else {
            Log.d(TAG, "sendCmdCh pending, queue size[" + mSendQ.size() );
        }

		return succeed;
	}

	public boolean writeTxCreditReportLoopCount(int count)
    {
		if (!mHasTxCredit) {
            Log.d(TAG, "TXC characteristic not available");
            return false;
        }

        QueueItem newItem = new QueueItem();
        newItem.ch = QueueItem.Channel.TXC;
        newItem.isRsp = true;
        newItem.signature = 0;
        newItem.isLast = true;

        //newItem.bytes = new byte[]{(byte)count};
        ByteBuffer byteBuf = ByteBuffer.allocate(4);
        byteBuf.order(ByteOrder.LITTLE_ENDIAN);
        byteBuf.putInt(count);
        newItem.bytes = byteBuf.array();

//		if (bytesLeft.length > 20) {
//			throw new IllegalStateException("tx credit too large");
//		}

        boolean succeed = false;
		
		if (mIsSending.compareAndSet(false, true)) {
            QueueItem item = pushQueueReturnFirstItem(newItem);
            succeed = mDevice.writeValue(mCurServiceUUID, UUID.fromString(TX_CREDIT_UUID), item.bytes);
            Log.d(TAG, "TxCredit data " + new String(item.bytes) + " sent " + (succeed?"succeed":"failed"));
        }
        else {
            Log.d(TAG, "writeTxCreditReportLoopCount pending, queue size[" + mSendQ.size() );
        }
		return succeed;
	}

    private boolean checkQueueAndResend() {
        Boolean success = false;

        if (mSendQ.size() > 0) {
            setTimer();

            //keep sending data is queue is not empty
            QueueItem item = mSendQ.get(0);
            byte[] bytesLeft = item.bytes;

            switch (item.ch) {
                case TX:
                    success = mDevice.writeValue(mCurServiceUUID, UUID.fromString(TX_UUID), item.bytes, item.isRsp);
                    break;
                case TX2:
                    success = mDevice.writeValue(mCurServiceUUID, UUID.fromString(TX2_UUID), item.bytes, item.isRsp);
                    break;
                case TXC:
                    success = mDevice.writeValue(mCurServiceUUID, UUID.fromString(TX_CREDIT_UUID), bytesLeft, item.isRsp);
                    break;
            }

            if (success) {
                mIsSending.set(true);
                success = true;
            }
        }

        return success;
    }

	public boolean enableRxNotification(boolean enabled) {
		if (!mHasRx2)
			return false;

		//attempt to enable but is already enabled
		if (mIsRxNotiEnabled && enabled)
			return true;

		//attempt to disable buyt is already disabled
		if (!mIsRxNotiEnabled && !enabled)
			return true;

		boolean succeed = mDevice.enableNotification(SVC_UUID, RX_UUID, true);
		if (succeed)
			mIsRxNotiEnabled = enabled;

		return succeed;
	}

	public void setRxNotiDefault(boolean enabled) {
		mShouldRxEnable = enabled;
	}

	public boolean enableRx2Notification(boolean enabled) {
		if (!mHasRx2)
			return false;

		//attempt to enable but is already enabled
		if (mIsRx2NotiEnabled && enabled)
			return true;

		//attempt to disable buyt is already disabled
		if (!mIsRx2NotiEnabled && !enabled)
			return true;

		boolean succeed = mDevice.enableNotification(SVC_UUID, RX2_UUID, true);
		if (succeed)
			mIsRx2NotiEnabled = enabled;

		return succeed;
	}

	public void setRx2NotiDefault(boolean enabled) {
		mShouldRx2Enable = enabled;
	}

	public boolean enableTxCreditNotification(boolean enable) {
		if (!mHasTxCredit)
			return false;

		//attempt to enable but is already enabled
		if (mIsTxCreditNotiEnabled && enable)
			return true;

		//attempt to disable buyt is already disabled
		if (!mIsTxCreditNotiEnabled && !enable)
			return true;

		boolean succeed = mDevice.enableNotification(SVC_UUID, TX_CREDIT_UUID, true);
		if (succeed) mIsTxCreditNotiEnabled = enable;
		return succeed;
	}

	public void setTxCreditNotiDefault(boolean enable) {
		mShouldTxCreditEnable = enable;
	}

    public void setChannelScramblerDefault(boolean enabled) {
		mShouldChScrmbEnabled = enabled;
    }

    public byte[] setScramblerWithSeed(int seed, byte[] pScrambler, boolean isDescramble) {
        byte[] tmp = new byte[256];
        for (int i = 0; i < 256; i++) {
            tmp[i] = 0;
        }
        int i = -1;
        int zeroIdx = -1;
        long X = seed;

        for (i = 0; i < 256; i++) {
            X = (long) Math.random();//(X * 1103515245L + 12345) % 0x7FFFFFFF;
            byte idx = (byte)( X % 64);
            if ( zeroIdx < 0) {
                tmp[idx] = (byte) i;
                pScrambler[i] = idx;
                zeroIdx = idx;
            }
            else if (zeroIdx != idx && tmp[idx] == 0) {
                tmp[idx] = (byte) i;
                pScrambler[i] = idx;
            }
            else {
                int j = idx % (256 - i);
                while (true) {
                    idx++;
                    if (zeroIdx != idx && tmp[idx] == 0) {
                        if (j-- <= 0) {
                            tmp[idx] = (byte) i;
                            pScrambler[i] = idx;
                            break;
                        }
                    }
                }
            }
        }

        if (!isDescramble) {
            return tmp;
        }
        return pScrambler;
    }

	public boolean readTxCredit() {
		if (!mHasTxCredit) {
			return false;
		}

		// mleung
        // - should just use readValue
        //return mDevice.readValueForCharacteristic(mCurServiceUUID, UUID.fromString(TX_CREDIT_UUID));
        return mDevice.readValue(mCurServiceUUID, UUID.fromString(TX_CREDIT_UUID));
	}

    private void resetmCheckReadyTimer(TimerTask timerTask) {
        if (mCheckReadyTimer != null) {
            mCheckReadyTimer.cancel();
            mCheckReadyTimer = null;
        }

        mCheckReadyTimer = new Timer();
        mCheckReadyTimer.schedule(timerTask, 1000);
    }

	private void setTimer() {
		if (mSendLockTimer != null) {
			mSendLockTimer.cancel();
		}

		mSendLockTimer = new Timer();
		mSendLockTimer.schedule(new TimerTask(){

			@Override
			public void run() {
				mIsSending.set(false);
                checkQueueAndResend();
			}
		}, mLockTimeout);
	}

}
