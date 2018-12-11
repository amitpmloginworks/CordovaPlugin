package com.gttronics.ble;

import java.util.*;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothGattCharacteristic;

import com.gttronics.ble.blelibrary.BleControllerCallback;
import com.gttronics.ble.blelibrary.BleDevice;
import com.gttronics.ble.blelibrary.BleDeviceCallback;
import com.gttronics.ble.blelibrary.dataexchanger.DataExchangerProfileCallback;
import com.gttronics.ble.blelibrary.dataexchanger.DataExchangerDevice;
import com.gttronics.ble.blelibrary.dataexchanger.DxAppController;
import com.gttronics.ble.blelibrary.dataexchanger.callbacks.FirmwareMetaRetrievedCallback;
import com.gttronics.ble.blelibrary.dataexchanger.callbacks.FirmwareProgressCallback;
import com.gttronics.ble.blelibrary.dataexchanger.callbacks.FirmwareWriteCompletedCallback;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class DxPlugin extends CordovaPlugin implements BleDeviceCallback, DataExchangerProfileCallback, BleControllerCallback{

    //
    // Class static member variables
    //
    
	private static final String TAG = "[DX_PI]";
	
	//
    // Class member variables
    //
    
    private static final int BLUETOOTH_ADMIN_PERMISSION = 1001;
    private static final int BLUETOOTH_PERMISSION = 1002;

    private boolean mIsAlive = true;
    
	private DxAppController dxAppController = null;
    private DxAppController.Config mConfig;

	private HashMap<String, CallbackContext> mCallbackMap = new HashMap<String, CallbackContext>();

    private BroadcastReceiver mReceiver;


    //
    // Class member functions
    //
    
    public DxPlugin() {}

    protected void pluginInitialize() {
        Log.d(TAG, "Cordova DataExchanger Plugin");
        Log.d(TAG,"(c)2016-2018 GT-tronics Canada Ltd");

        super.pluginInitialize();
    }

    @Override
	public boolean execute(final String action, JSONArray args, CallbackContext cbContext) throws JSONException {

		Log.d(TAG, "execute - " + action + ": " + args.toString());

		if (action.equals("init")) {
			init(args, cbContext);
		}
        else if (action.equals("startScan")) {
            startScan(cbContext);
        }
        else if (action.equals("startScanAndConnect")) {
            Log.e(TAG, "startScanAndConnect: called deprecated function");
        }
        else if (action.equals("stopScan")) {
            stopScan(cbContext);
        }
		else if (action.equals("connect")) {
			connect(args.getString(0), cbContext);
		}
        else if (action.equals("disconnect")) {
            disconnect(args.getString(0), cbContext);
        }
        else if (action.equals("sendData")) {
            sendData(args.getString(0), args.getString(1), cbContext);
        }
        else if (action.equals("sendCmd")) {
            sendCmd(args.getString(0), args.getString(1), cbContext);
        }
        else if (action.equals("readTxCredit")) {
            readTxCredit(args.getString(0), cbContext);
        }
        else if (action.equals("writeTxCreditReportLoopCount")) {
		    writeTxCreditReportLoopCount(args.getString(0), args.getInt(1), cbContext);
        }
        else if (action.equals("isConnected")) {
		    isConnected(args.getString(0), cbContext);
        }
//        else if (action.equals("isEnabled")) {
//            isEnabled(true);
//        }
		else if (action.equals("enableRxDataNotification")) {
			enableRxDataNotification(args.getString(0), cbContext);
		}
        else if (action.equals("enableRxCmdNotification")) {
            enableRxCmdNotification(args.getString(0), cbContext);
        }
        else if (action.equals("enableTxCreditNotification")) {
		    enableTxCreditNotification(args.getString(0), cbContext);
        }
        else if (action.equals("disableRxDataNotification")) {
		    disableRxDataNotification(args.getString(0), cbContext);
        }
        else if (action.equals("disableRxCmdNotification")) {
            disableRxCmdNotification(args.getString(0), cbContext);
        }
        else if (action.equals("retrieveFirmwareMeta")) {
            retrieveFirmwareMeta(args.getString(0), cbContext);
        }
        else if (action.equals("primeFirmwareBinary")) {
            String uuid = args.getString(0);
            String base64Bin = args.getString(1);
            byte[] firmBin = Base64.decode(base64Bin, Base64.DEFAULT);

            String firmName = args.getString(2);

            primeFirmwareBinary(uuid, firmBin, firmName, cbContext);

        }
        else if (action.equals("switchFirmwareToSlot")) {
            String uuid = args.getString(0);
            int slotIdx = Integer.parseInt(args.getString(1));
            boolean keepConfig = args.getString(2).getBytes().equals("true");
            switchFirmwareToSlot(uuid, slotIdx, keepConfig, cbContext);
        }


		return true;
	}

    // App lifecycle
    @Override
    public void onStart() {
        super.onStart();
        mIsAlive = true;
        if (dxAppController == null && mConfig != null) {
            dxAppController = DxAppController.getInstance(cordova.getActivity().getApplicationContext(), mConfig);
        }
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        if (dxAppController == null) {
            Log.e(TAG, "onResume - Controller already cleared.");
            return;
        }

        if (dxAppController.isAnyConnected()) {
            dxAppController.disconnectAll();
            return;
        };
        
        mIsAlive = true;
    }

    @Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);
        if (dxAppController == null) {
            Log.e(TAG, "onPause - Controller is not initiated.");
            return;
        }

        if (dxAppController.isAnyConnected()) {
            dxAppController.disconnectAll();
        }

        dxAppController.stopScan();
        mIsAlive = false;
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        if (dxAppController != null) {
            if (dxAppController.isAnyConnected()) {
                dxAppController.disconnectAll();
            }
            
            dxAppController.stopScan();
            //dxAppController.reset();
            dxAppController = null;
        }
        mIsAlive = false;
    }


    @Override
    public void onRequestPermissionResult(int requestCode, String permissions[], int[] grantResults) throws JSONException {
        super.onRequestPermissionResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult -" + requestCode);
        if (requestCode == BLUETOOTH_ADMIN_PERMISSION) {
            if (ContextCompat.checkSelfPermission(cordova.getActivity(), android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(cordova.getActivity(), new String[]{android.Manifest.permission.BLUETOOTH}, BLUETOOTH_PERMISSION);
                return;
            }

            allPermissionGranted();
        }
        else if (requestCode == BLUETOOTH_PERMISSION) {
            allPermissionGranted();
        }
    }

    // Plugin function
	public void init(JSONArray params, CallbackContext cbContext) throws JSONException {

		if (dxAppController != null) {
			Log.e(TAG, "already initialized.");
            cbContext.error(getImmediateResponse(-1,"already initialized"));
			return;
		}

		mConfig = new DxAppController.Config();
		mConfig.setDevCount(params.length() > 0? Integer.parseInt(params.getString(0)) : 1);
		mConfig.setPwrLevel(params.length() > 1? Float.parseFloat(params.getString(1)) : -127);
		mConfig.setTimeout(params.length() > 2? Double.parseDouble(params.getString(2)) : 5.0);
		mConfig.setAutoConnect(params.length() > 3 && params.getString(3).toLowerCase().equals("true"));
		mConfig.setEnableCmdChannel(params.length() > 4 && params.getString(4).toLowerCase().equals("true"));
		mConfig.setEnableChannelScram(params.length() > 5 && params.getString(5).toLowerCase().equals("true"));
		mConfig.setEnableTxCredit(params.length() > 6 && params.getString(6).toLowerCase().equals("true"));

		if( params.length() > 7 )
		{
            JSONArray arr = params.getJSONArray(7);
            List<String> list = new ArrayList<String>();
            for (int i = 0; i < arr.length(); i++) {
                String uuid = arr.getString(i);
                if( !uuid.toLowerCase().matches("^[0-9a-f]{8}-?[0-9a-f]{4}-?[0-5][0-9a-f]{3}-?[089ab][0-9a-f]{3}-?[0-9a-f]{12}$") )
                {
                    cbContext.error(getImmediateResponse(-2,"invalid service uuids"));
                    return;
                }
                list.add(uuid);
            }

            mConfig.setServiceUUID(list);
        }

		Log.d(TAG, "initiating controller with config " + params.toString());
        mCallbackMap.put("INIT", cbContext);

        // check bluetooth permission
        if (!cordova.hasPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)) {
            cordova.requestPermission(this, BLUETOOTH_ADMIN_PERMISSION, android.Manifest.permission.ACCESS_COARSE_LOCATION);
            return;
        }

        if (!cordova.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            cordova.requestPermission(this, BLUETOOTH_PERMISSION, android.Manifest.permission.ACCESS_FINE_LOCATION);
            return;
        }

        allPermissionGranted();
	}

    private void allPermissionGranted() {
        dxAppController = DxAppController.getInstance(cordova.getActivity().getApplicationContext(), mConfig);
        dxAppController.setProfileCallback(this);
        dxAppController.setBleDeviceCallback(this);
        dxAppController.setBleControllerCallback(this);

        Log.d(TAG, "controller initiated successfully.");
        
        sendSuccessResponse(mCallbackMap.get("INIT"), getInitSuccessResponse());

        registerBluetoothReceiver();
    }
	
	public void startScan(CallbackContext cbContext) {
        if (!mIsAlive) {
            Log.e(TAG, "startScan - App is not active");
            cbContext.error(getImmediateResponse(-1,"app is not active"));
            return;
        }

        if (dxAppController == null) {
            Log.e(TAG, "startScan - Controller is not initiated");
            cbContext.error(getImmediateResponse(-2,"controller is not initialized"));
            return;
        }
        
        dxAppController.startScan();
        mCallbackMap.put("SCAN", cbContext);
    }

    public void stopScan(CallbackContext cbContext) {
        if (!mIsAlive) {
            Log.e(TAG, "stopScan - App is not active");
            cbContext.error(getImmediateResponse(-1,"app is not active"));
            return;
        }

        if (dxAppController == null) {
            Log.e(TAG, "stopScan - Controller is not initiated");
            cbContext.error(getImmediateResponse(-2,"controller is not initialized"));
            return;
        }

        dxAppController.stopScan();
        CallbackContext context = mCallbackMap.get("SCAN");
        if( context != null )
        {
            sendSuccessResponse(context, new JSONObject());
        }
    }

    public void connect(String uuid, CallbackContext cbContext) {
        if (!mIsAlive) {
            Log.e(TAG, "connect - App is not active");
            cbContext.error(getImmediateResponse(-1,"app is not active"));
            return;
        }

        if (dxAppController == null) {
            Log.e(TAG, "connect - Controller is not initiated");
            cbContext.error(getImmediateResponse(-2,"controller is not initialized"));
            return;
        }
        
        boolean success = dxAppController.connect(uuid);
        if( success )
        {
            mCallbackMap.put("CONNECT" + uuid, cbContext);
        }
        else
        {
            cbContext.error(getImmediateResponse(-3,"connect failed"));
        }
    }

    public void disconnect(String uuid, CallbackContext cbContext) {
        if (!mIsAlive) {
            Log.e(TAG, "disconnect - App is not active");
            cbContext.error(getImmediateResponse(-1,"app is not active"));
            return;
        }

        if (dxAppController == null) {
            Log.e(TAG, "disconnect - Controller is not initiated");
            cbContext.error(getImmediateResponse(-2,"controller is not initialized"));
            return;
        }
        
        boolean success = dxAppController.disconnect(uuid);
        if( !success )
        {
            cbContext.error(getImmediateResponse(-3,"disconnect failed"));
        }
    }

	public void sendData(String uuid, String dataStr, CallbackContext cbContext) {
        if (!mIsAlive) {
            Log.e(TAG, "sendData - App is not active");
            cbContext.error(getImmediateResponse(-1,"app is not active"));
            return;
        }

        if (dxAppController == null) {
            Log.e(TAG, "sendData - Controller is not initiated");
            cbContext.error(getImmediateResponse(-2,"controller is not initialized"));
            return;
        }

		boolean success = dxAppController.sendData(uuid, convertStringToBytes(dataStr));
        if( success )
        {
            cbContext.success(getImmediateResponse(0,"success"));
        }
        else
        {
            cbContext.error(getImmediateResponse(-3,"sendData failed"));
        }
    }

	public void sendCmd(String uuid, String cmdStr, CallbackContext cbContext) {
        if (!mIsAlive) {
            Log.e(TAG, "sendCmd - App is not active");
            cbContext.error(getImmediateResponse(-1,"app is not active"));
            return;
        }

        if (dxAppController == null) {
            Log.e(TAG, "sendCmd - Controller is not initiated");
            cbContext.error(getImmediateResponse(-2,"controller is not initialized"));
            return;
        }

        boolean success = dxAppController.sendCmd(uuid, convertStringToBytes(cmdStr));
        if( success )
        {
            cbContext.success(getImmediateResponse(0,"success"));
        }
        else
        {
            cbContext.error(getImmediateResponse(-3,"sendCmd failed"));
        }
	}
	
	public void isEnabled(boolean enabled, CallbackContext cbContext) {
        cbContext.success(getImmediateResponse(0,"success"));
    }

	public void isConnected(String uuid, CallbackContext cbContext) {
        if (!mIsAlive) {
            Log.e(TAG, "isConnected - App is not active");
            cbContext.error(getImmediateResponse(-1,"app is not active"));
            return;
        }

        if (dxAppController == null) {
            Log.e(TAG, "isConnected - Controller is not initiated");
            cbContext.error(getImmediateResponse(-2,"controller is not initialized"));
            return;
        }

        cbContext.success(dxAppController.isConnected(uuid) ?1 :0);
    }

	public void readTxCredit(String uuid, CallbackContext cbContext) {
        if (!mIsAlive) {
            Log.e(TAG, "readTxCredit - App is not active");
            cbContext.error(getImmediateResponse(-1,"app is not active"));
            return;
        }

        if (dxAppController == null) {
            Log.e(TAG, "readTxCredit - Controller is not initiated");
            cbContext.error(getImmediateResponse(-2,"controller is not initialized"));
            return;
        }

        boolean success = dxAppController.readTxCredit(uuid);

        if (!success) {
            mCallbackMap.get("TXC_" + uuid).error(getImmediateResponse(-3, "readTxCredit failed"));
        }
    }

	public void writeTxCreditReportLoopCount(String uuid, int count, CallbackContext cbContext) {
        if (!mIsAlive) {
            Log.e(TAG, "writeTxCreditReportLoopCount - App is not active");
            cbContext.error(getImmediateResponse(-1,"app is not active"));
            return;
        }

        if (dxAppController == null) {
            Log.e(TAG, "writeTxCreditReportLoopCount - Controller is not initiated");
            cbContext.error(getImmediateResponse(-2,"controller is not initialized"));
            return;
        }

        boolean success = dxAppController.writeTxCreditReportLoopCount(uuid, count);
        if( success )
        {
            cbContext.success(getImmediateResponse(0,"success"));
        }
        else
        {
            cbContext.error(getImmediateResponse(-3,"writeTxCreditReportLoopCount failed"));
        }
    }

	public void enableRxDataNotification(String uuid, CallbackContext cbContext) {
        if (!mIsAlive) {
            Log.e(TAG, "enableRxDataNotification - App is not active");
            cbContext.error(getImmediateResponse(-1,"app is not active"));
            return;
        }

        if (dxAppController == null) {
            Log.e(TAG, "enableRxDataNotification - Controller is not initiated");
            cbContext.error(getImmediateResponse(-2,"controller is not initialized"));
            return;
        }

        mCallbackMap.put("RX_" + uuid, cbContext);
    }

	public void disableRxDataNotification(String uuid, CallbackContext cbContext) {
        if (!mIsAlive) {
            Log.e(TAG, "disableRxDataNotification - App is not active");
            cbContext.error(getImmediateResponse(-1,"app is not active"));
            return;
        }

        if (dxAppController == null) {
            Log.e(TAG, "disableRxDataNotification - Controller is not initiated");
            cbContext.error(getImmediateResponse(-2,"controller is not initialized"));
            return;
        }

        mCallbackMap.remove("RX_" + uuid);
    }

	public void enableRxCmdNotification(String uuid, CallbackContext cbContext) {
        if (!mIsAlive) {
            Log.e(TAG, "enableRxCmdNotification - App is not active");
            cbContext.error(getImmediateResponse(-1,"app is not active"));
            return;
        }

        if (dxAppController == null) {
            Log.e(TAG, "enableRxCmdNotification - Controller is not initiated");
            cbContext.error(getImmediateResponse(-2,"controller is not initialized"));
            return;
        }

        mCallbackMap.put("RX2_" + uuid, cbContext);
	}

    public void disableRxCmdNotification(String uuid, CallbackContext cbContext) {
        if (!mIsAlive) {
            Log.e(TAG, "disableRxCmdNotification - App is not active");
            cbContext.error(getImmediateResponse(-1,"app is not active"));
            return;
        }

        if (dxAppController == null) {
            Log.e(TAG, "disableRxCmdNotification - Controller is not initiated");
            cbContext.error(getImmediateResponse(-2,"controller is not initialized"));
            return;
        }

        mCallbackMap.remove("RX2_" + uuid);
    }

    public void enableTxCreditNotification(String uuid, CallbackContext cbContext) {
        if (!mIsAlive) {
            Log.e(TAG, "enableTxCreditNotification - App is not active");
            cbContext.error(getImmediateResponse(-1,"app is not active"));
            return;
        }

        if (dxAppController == null) {
            Log.e(TAG, "enableTxCreditNotification - Controller is not initiated");
            cbContext.error(getImmediateResponse(-2,"controller is not initialized"));
            return;
        }

        mCallbackMap.put("TXC_" + uuid, cbContext);
    }

    public void disableTxCreditNotification(String uuid, CallbackContext cbContext) {
        if (!mIsAlive) {
            Log.e(TAG, "disableTxCreditNotification - App is not active");
            cbContext.error(getImmediateResponse(-1,"app is not active"));
            return;
        }

        if (dxAppController == null) {
            Log.e(TAG, "disableTxCreditNotification - Controller is not initiated");
            cbContext.error(getImmediateResponse(-2,"controller is not initialized"));
            return;
        }

        mCallbackMap.remove("TXC_" + uuid);
    }

    private void registerBluetoothReceiver() {

        if (mReceiver != null) {
            Log.e(TAG, "Receiver already registered");
            return;
        }

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF) {
                        //sendSuccessResponse(mConnectCallback, getDisconnectResponse());

                        sendSuccessResponse(mCallbackMap.get("INIT"), getInitBleResponse(false));

                        // New instance of dxAppController will be created.
                        // - but all callback and config are maintained
                        dxAppController = DxAppController.reset();

                        // mCallbackMap = new HashMap<String, CallbackContext>();
                    }
                    else if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_ON) {
                        Log.d(TAG, "On bluetooth alive");
                        sendSuccessResponse(mCallbackMap.get("INIT"), getInitBleResponse(true));
                    }
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        cordova.getActivity().registerReceiver(mReceiver, intentFilter);

    }

    // Data exchanger interface
	@Override
	public void onDeviceStateChanged(BleDevice dev, BLE_DEV_STATE state) {
		Log.d(TAG, "onDeviceStateChanged - \"" + dev.mUUID + "\" " + state);

		// Device is discovered and connected but its service has not been discovered
		if( state == BLE_DEV_STATE.CONNECTED_NOT_SVC_RDY ) {
			// Stop discovery scan if a device is connected
            if( dev.isAutoConnect() ) {
                dxAppController.stopScan();
            }
		}

		// Device is disconnected
		else if( state == BLE_DEV_STATE.DISCONNECTED ) {
            sendSuccessResponse(mCallbackMap.get("CONNECT" + dev.mUUID), getDisconnectResponse(dev.mUUID));

            // Fully decommision the device
            // - reset the device state to IDLE
            // - set UUID to null
            // - set NAME to null
            dev.decommission();
        }
	}

	@Override
	public void onAllProfilesReady(BleDevice dev, boolean isAllReady) {
		if( isAllReady )
		{
			// App is ready to interact with the BLE device.
            Log.d(TAG, "All profiles are ready, device " + dev.getDeviceName() + " connected successfully.");
            sendSuccessResponse(mCallbackMap.get("CONNECT" + dev.mUUID), getConnectSuccessResponse(dev.mUUID));
		}
		else
		{
			Log.d(TAG, "Not all profiles are ready.");
			dev.close();
		}
	}
	
	@Override
	public void onRxDataAvailable(DataExchangerDevice dev, byte[] bytes) {
		if (mCallbackMap.get("RX_" + dev.mUUID) != null) {
            //Log.d(TAG, "onRxDataAvailable " + "[RX_" + dev.mUUID + "]");
			sendSuccessResponse(mCallbackMap.get("RX_" + dev.mUUID), getDataReceiveResponse(dev.mUUID, bytes));
		}
	}

	@Override
	public void onRx2DataAvailable(DataExchangerDevice dev, byte[] bytes) {
		if (mCallbackMap.get("RX2_" + dev.mUUID) != null) {
            //Log.d(TAG, "onRx2DataAvailable " + "[RX2_" + dev.mUUID + "]");
			sendSuccessResponse(mCallbackMap.get("RX2_" + dev.mUUID), getDataReceiveResponse(dev.mUUID, bytes));
		}
	}

	@Override
	public void onTxCreditDataAvailable(DataExchangerDevice dev, byte[] bytes) {
        if (mCallbackMap.get("TXC_" + dev.mUUID) != null) {
            //Log.d(TAG, "onRx2DataAvailable " + "[TXC_" + dev.mUUID + "]");
            sendSuccessResponse(mCallbackMap.get("TXC_" + dev.mUUID), getDataReceiveResponse(dev.mUUID, bytes));
        }
	}

	@Override
	public void onCharacteristicWrite(DataExchangerDevice dev, int signature) {
	}

	@Override
	public void onUnknownDeviceDiscovered(BluetoothDevice btDev, int rssi, byte[] scanRecord) {}

	@Override
	public void onDeviceDiscovered(BleDevice bleDev, BluetoothDevice btDev, int rssi, byte[] scanRecord) {
        String uuid = UUID.nameUUIDFromBytes(btDev.getAddress().getBytes()).toString();
        sendSuccessResponse(mCallbackMap.get("SCAN"), getScanSuccessResponse(btDev.getName(), rssi, uuid));
	}

    // helper functions
	private JSONObject getInitSuccessResponse() {
		JSONObject rspJson = new JSONObject();

		try {
			rspJson.put("state", "init");
		}
		catch (JSONException e) {
			e.printStackTrace();
		}

		return rspJson;
	}


    private JSONObject getInitBleResponse(boolean isOn) {
        JSONObject rspJson = new JSONObject();

        try {
            rspJson.put("state", isOn ? "syson" : "sysoff");
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        return rspJson;
    }

	private JSONObject getScanSuccessResponse(String deviceName, int rssi, String uuid) {
		JSONObject rspJson = new JSONObject();
		JSONObject infoJson = new JSONObject();
		try {
			infoJson.put("NAME", deviceName);
			infoJson.put("RSSI", String.valueOf(rssi));
            infoJson.put("UUID", uuid);
			rspJson.put("info", infoJson);
			rspJson.put("state", "active");

			return rspJson;
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private JSONObject getConnectSuccessResponse(String uuid) {
		JSONObject rspJson = new JSONObject();
		JSONObject infoJson = new JSONObject();

		try {
			infoJson.put("UUID", uuid);
			rspJson.put("info", infoJson);
			rspJson.put("state", "connected");
			return rspJson;
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private JSONObject getDisconnectResponse(String uuid) {
		JSONObject rspJson = new JSONObject();
		JSONObject infoJson = new JSONObject();

		try {
			infoJson.put("UUID", uuid);
			rspJson.put("info", infoJson);
			rspJson.put("state", "disconnected");
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		return rspJson;
	}

	private JSONObject getDataReceiveResponse(String uuid, byte[] bytes) {
		String dataStr = convertBytesToBase64String(bytes);
		JSONObject rspJson = new JSONObject();
        JSONObject infoJson = new JSONObject();
        JSONObject innerDataJson = new JSONObject();
		try {
            infoJson.put("UUID", uuid);
            rspJson.put("info", infoJson);
            innerDataJson.put("data", dataStr);
            rspJson.put("data", innerDataJson);

			return rspJson;
		}
		catch(Exception e) {
			e.printStackTrace();
		}

		return null;
	}
	
	private JSONObject getImmediateResponse(int code, String status) {
        JSONObject rspJson = new JSONObject();

        try {
            rspJson.put("retCode", code);
            rspJson.put( "status", status);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        return rspJson;
    }

	private void sendSuccessResponse(CallbackContext cbContext, JSONObject response) {
        if (cbContext == null) {
            Log.e(TAG, "Failed send success cbContext, NULL cbContext context");
            return;
        }
		PluginResult result = new PluginResult(PluginResult.Status.OK, response);
		result.setKeepCallback(true);
		cbContext.sendPluginResult(result);
	}

    private void sendFailResponse(CallbackContext cbContext, JSONObject response) {
        PluginResult result = new PluginResult(PluginResult.Status.ERROR, response);
        result.setKeepCallback(true);
        cbContext.sendPluginResult(result);

    }

//	private void sendSuccessResponse(CallbackContext cbContext, String response) {
//		PluginResult result = new PluginResult(PluginResult.Status.OK, response);
//		result.setKeepCallback(true);
//		cbContext.sendPluginResult(result);
//	}
//
    // decode from base 64
    private byte[]  convertStringToBytes(String input) {
        byte[] base64 = Base64.decode(input.getBytes(), Base64.DEFAULT);
        return base64;
    }
    
	// encode to base 64
	private String convertBytesToBase64String(byte[] bytes) {
		byte[] base64 = Base64.encode(bytes, Base64.NO_WRAP);
		return new String(base64);
	}

//	private void makeToast(final String msg) {
//		cordova.getActivity().runOnUiThread(new Runnable() {
//			public void run() {
//				Toast.makeText(cordova.getActivity(), msg, Toast.LENGTH_SHORT).show();
//
//			}
//		});
//	}

//    private String bytesToHex(byte[] bytes) {
//        final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
//        char[] hexChars = new char[bytes.length * 2];
//        int v;
//        for ( int j = 0; j < bytes.length; j++ ) {
//            v = bytes[j] & 0xFF;
//            hexChars[j * 2] = hexArray[v >>> 4];
//            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
//        }
//        return new String(hexChars);
//    }

    public void retrieveFirmwareMeta(String uuid, CallbackContext cbContext) {
        if (!mIsAlive) {
            Log.e(TAG, "writeTxCreditReportLoopCount - App is not active");
            cbContext.error(getImmediateResponse(-1,"app is not active"));
            return;
        }

        if (dxAppController == null) {
            Log.e(TAG, "writeTxCreditReportLoopCount - Controller is not initiated");
            cbContext.error(getImmediateResponse(-2,"controller is not initialized"));
            return;
        }

        mCallbackMap.put("RFM_" + uuid, cbContext);

        boolean success = dxAppController.retrieveFirmwareMetaWithProgress(
                            uuid,
                            new FirmwareProgressCallback() {
                                @Override
                                public void onProgress(int stage, double progress) {
                                    JSONObject rspJson = new JSONObject();

                                    try {
                                        rspJson.put("stage", String.valueOf(stage));
                                        rspJson.put("progress", String.valueOf(progress));
                                        rspJson.put("isdone", "false");
                                    }
                                    catch (JSONException e) {
                                        e.printStackTrace();
                                    }

                                    sendSuccessResponse(mCallbackMap.get("RFM_" + uuid), rspJson);
                                }
                            },
                            new FirmwareMetaRetrievedCallback() {
                                @Override
                                public void onMeteRetrieved(int status, Map<String, Object> meta, String msg) {

                                    JSONObject rspJson = new JSONObject(meta);
                                    JSONObject metaJson = new JSONObject();

                                    try {
                                        for (Map.Entry<String, Object> entry : meta.entrySet()) {
                                            metaJson.put(entry.getKey(), String.valueOf(entry.getValue()));
                                        }

                                        rspJson.put("metas", metaJson);
                                        rspJson.put("status", TextUtils.isEmpty(msg) ? "OK" : "FAILED");
                                        rspJson.put("reason", TextUtils.isEmpty(msg) ? "" : msg);
                                        rspJson.put("isdone", "true");
                                    }
                                    catch (JSONException e) {
                                        e.printStackTrace();
                                    }

                                    sendSuccessResponse(mCallbackMap.get("RFM_" + uuid), rspJson);
                                }
                            });
        if (success) {
            cbContext.success(getImmediateResponse(0, "success"));
        }
        else {
            cbContext.error(getImmediateResponse(-3, "retrieveFirmwareMeta failed"));
        }
    }

    public void primeFirmwareBinary(String uuid, byte[] firmBin, String firmName, CallbackContext cbContext) {
        if (!mIsAlive) {
            Log.e(TAG, "writeTxCreditReportLoopCount - App is not active");
            cbContext.error(getImmediateResponse(-1,"app is not active"));
            return;
        }

        if (dxAppController == null) {
            Log.e(TAG, "writeTxCreditReportLoopCount - Controller is not initiated");
            cbContext.error(getImmediateResponse(-2,"controller is not initialized"));
            return;
        }

        mCallbackMap.put("PFB_" + uuid, cbContext);

        boolean success = dxAppController.primeFirmwareBinary(uuid, firmBin, firmName,
                            new FirmwareWriteCompletedCallback() {
                                @Override
                                public void onWriteCompleted(int status, Map<String, Object> metas, String msg) {
                                    Log.d(TAG, "prime complete cbContext");
                                    JSONObject rspJson = new JSONObject();
                                    JSONObject metaJson = new JSONObject();

                                    try {
                                        if (metas != null) {
                                            metas.remove("SlotScratchPads");
                                            metas.remove("SlotValidMask");

                                            for (Map.Entry<String, Object> entry : metas.entrySet()) {
                                                metaJson.put(entry.getKey(), String.valueOf(entry.getValue()));
                                            }
                                        }

                                        rspJson.put("metas", metaJson);
                                        rspJson.put("status", TextUtils.isEmpty(msg) ? "OK" : "FAILED");
                                        rspJson.put("reason", TextUtils.isEmpty(msg) ? "" : msg);
                                        rspJson.put("isdone", "true");
                                    }
                                    catch(JSONException e) {
                                        e.printStackTrace();
                                    }

                                    sendSuccessResponse(mCallbackMap.get("PFB_" + uuid), rspJson);

                                }
                            },
                            new FirmwareProgressCallback() {
                                @Override
                                public void onProgress(int stage, double progress) {
                                    Log.d(TAG, "prime progress cbContext");
                                    JSONObject rspJson = new JSONObject();

                                    try {
                                        rspJson.put("stage", String.valueOf(stage));
                                        rspJson.put("progress", String.valueOf(progress));
                                    }
                                    catch(JSONException e) {
                                        e.printStackTrace();
                                    }

                                    sendSuccessResponse(mCallbackMap.get("PFB_" + uuid), rspJson);
                                }
                            });
        if (success) {
            cbContext.success(getImmediateResponse(0, "success"));
        }
        else {
            cbContext.error(getImmediateResponse(-3, "primeFirmwareBinary failed"));
        }
    }

    public void switchFirmwareToSlot(String uuid, int slotIdx, boolean keepConfig, CallbackContext cbContext) {
        if (!mIsAlive) {
            Log.e(TAG, "writeTxCreditReportLoopCount - App is not active");
            cbContext.error(getImmediateResponse(-1,"app is not active"));
            return;
        }

        if (dxAppController == null) {
            Log.e(TAG, "writeTxCreditReportLoopCount - Controller is not initiated");
            cbContext.error(getImmediateResponse(-2,"controller is not initialized"));
            return;
        }

        if (dxAppController == null) {
            Log.e(TAG, "switchFirmwareToSlot - Controller already cleared");
        }

        boolean success = dxAppController.switchFirmwareImageToSlot(uuid, slotIdx, keepConfig);

        if (success) {
            cbContext.success(getImmediateResponse(0, "success"));
        }
        else {
            cbContext.error(getImmediateResponse(-3, "switchFirmwareToSlot failed"));
        }
    }

}
