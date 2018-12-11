package com.gttronics.ble.blelibrary.dataexchanger.helpers;

import android.bluetooth.BluetoothGattCharacteristic;
import android.nfc.Tag;
import android.text.TextUtils;
import android.util.Log;

import com.gttronics.ble.blelibrary.dataexchanger.DataExchangerDevice;

import java.security.interfaces.RSAKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import com.gttronics.ble.blelibrary.dataexchanger.DataExchangerProfile;
import com.gttronics.ble.blelibrary.dataexchanger.callbacks.FirmwareMetaRetrievedCallback;
import com.gttronics.ble.blelibrary.dataexchanger.callbacks.FirmwareProgressCallback;
import com.gttronics.ble.blelibrary.dataexchanger.callbacks.FirmwareWriteCompletedCallback;
import com.gttronics.ble.blelibrary.dataexchanger.models.FirmwareMeta;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by EGUSI16 on 11/29/2016.
 */

public class DxAppFirmLogStateMachine {

    class FirmBinSeg {
        public int id;
        public byte[] binSeg;
    }

    private static final int BT_MTU = (27 - 4 - 3);
    private static final int BT_PKT_ON_FLIGHT = 30;
//    private static final int BT_PKT_INIT_INJECT = 17;
//    private static final int BT_PKT_BUFFER = BT_PKT_INIT_INJECT;
//
//    private static int pktBuffer = BT_PKT_BUFFER;

    public static final int REQUEST_FAILED = -1;
    public static final int REQUEST_SUCCEED = 2;
    public static final int REQUEST_DLD_META = 3;
    public static final int REQUEST_ERR_READ_META = 0;


    public static final int RETRIEVE_META = 0;

    private static final String TAG = "LOG_STATE_MACHINE";

    private static final int FIRM_IMG_BLK_LEN = 0x20000;
    private static final int FIRM_IMG_BASE_ADDR = 0x800000;
    private static final int FIRM_META_LEN = 0x1000;
    private static final int FIRM_META_ADDR = FIRM_IMG_BASE_ADDR - FIRM_META_LEN;


    private static final int STATE_IDLE = 0;
    private static final int STATE_FIRM_READ_FLASH_SZ = 1;
    private static final int STATE_FIRM_READ_META = 2;
    private static final int STATE_FIRM_VERSIONS = 3;
    private static final int STATE_FIRM_UPDATE_META = 4;
    private static final int STATE_FIRM_READ_IMG = 5;
    private static final int STATE_FIRM_WRITE_IMG = 6;
    private static final int STATE_FIRM_WHICH_SLOT = 7;
    private static final int STATE_FIRM_CRC_CHECK = 8;
    private static final int STATE_FIRM_CRC_CHECK_AFTER_WRITE = 9;

    private static final int STATE_DLGR_META = 10;
    private static final int STATE_DLGR_READ_A = 11;
    private static final int STATE_DLGR_READ_B = 12;

    private String mRspStr = null;
    private String mDataLogIdxStr = null;
    private int mDataStartAddrNext;
    private int mDataReadBytesNext;
    private boolean mDataLogIsFlush;

    private long mFirmImgBaseaAddr = 0x800000;
    private int mFirImgBlkLen = 0x20000;
    private int mFirmMetaLen = 0x1000;
    private int mFirmImgBlkLen = 0x20000;
    private long mFirmMetaAddr = mFirmImgBaseaAddr - mFirmMetaLen;
    private byte[] mFirmBinary;
    private byte[] mFirmNewMetaBlk;
    private byte[] mFirmMetaBlk;
    private int mFirmBinLen;
    private boolean mFirmStart = false;
    private Map<String, Object> mFirmMetas;
    private int mFirmCrcChkSlotIdx;
    private Map<String, Object> mFirmCrcCodes;
    private LinkedHashSet<Integer> mFirmSlotValidSet;
    private int mFirmSlotValidTotal;
    private Map<String, Object> mFirmSysVersions;
    private boolean mFirmDownFast = true;
    private int mFirmSlotIdx;
    private int mFirmOfs;

    private List<FirmBinSeg> mFirmwareQueue;

    private boolean ismDataLogIsFlush;
    private int mDataLogStartAddrNext;
    private int mDataLogStartAddrNow;
    private int mDataLogReadBytesNext;
    private int mDataLogReadBytesNow;
    private int mDataLogLastAddrNew;
    private String mDataIdxStr;
    private Map<String, Object> mDataLogMeta;
    private byte[] mDataLogData;

    private boolean mShouldTryRetrieveFlashSize = true;

    private boolean mRebaseTxCredit;
    private int mTxCredit;
    private int mLastTxCredit;


    private FirmwareProgressCallback mFirmwareProgressCallback = null;
    private FirmwareMetaRetrievedCallback mFirmwareMetaRetrievedCallback = null;
    private FirmwareWriteCompletedCallback mFirmwareWriteCompletedCallabck = null;

    private DataExchangerDevice mDxDev;

    private Timer timer;
    private TimerTask timerTask;

    protected int mState = STATE_IDLE;

    public DxAppFirmLogStateMachine(DataExchangerDevice dxDev) {
        mDxDev = dxDev;
    }

    public static int crc16CalcOnData(byte[] buf, int len) {
//        int crc = 0xFFFF;
//
//        for (int pos = 0; pos < len; pos++) {
//            crc ^= (int)buf[pos] & 0xFF;   // XOR byte into least sig. byte of crc
//
//            for (int i = 8; i != 0; i--) {    // Loop over each bit
//                if ((crc & 0x0001) != 0) {      // If the LSB is set
//                    crc >>= 1;                    // Shift right and XOR 0xA001
//                    crc ^= 0xA001;
//                }
//                else                            // Else LSB is not set
//                    crc >>= 1;                    // Just shift right
//            }
//        }
// Note, this number has low and high bytes swapped, so use it accordingly (or swap bytes)

        int poly = 0x1021;
        int cnt;
        int i;
        int crc = 0;

        for (i = 0; i < len; i++)
        {
            byte val = buf[i];
            for (cnt = 0; cnt < 8; cnt++, val <<= 1)
            {
                int tmp = crc & 0x8000;
                boolean msb = (tmp > 1);

                crc <<= 1;

                if ((val & 0x80) > 0)
                {
                    crc |= 0x0001;
                }

                if (msb)
                {
                    crc ^= poly;
                }
            }
        }

        return crc & 0xffff;
    }

    public boolean primeFirmwareBinary(byte[] firmData, String firmName, FirmwareWriteCompletedCallback completeCallback, FirmwareProgressCallback progressCallback) {

        if (mState != STATE_IDLE) {
            return false;
        }
//
//        if (true) {
//
//            String cmdStr = "AT+IMG=128\r\n";
//            boolean succeed = mDxDev.sendCmd(cmdStr.getBytes(), 1);
//            mState = STATE_IDLE;
//            return true;
//        }

        mFirmwareWriteCompletedCallabck = completeCallback;

        // verify source file
        if (firmData.length != 0x20000) {
            mFirmwareWriteCompletedCallabck.onWriteCompleted(REQUEST_FAILED, null, "Invalid firmware binary");
            return true;
        }

        // override the control block
        firmData[0x1CFFC] = (byte)0xFF;
        firmData[0x1CFFD] = (byte)0x00;
        firmData[0x1CFFE] = (byte)0xFF;
        firmData[0x1CFFF] = (byte)0x0F;

        // Only take 31 x 41KB last
        mFirmBinary = Arrays.copyOfRange(firmData, 0, 0x1F000);
        mFirmBinLen = mFirmBinary.length;
        mFirmOfs = 0;

        // for interleave command
        // TODO - align to 4KB (per flash sector)

        int imgAddr = (int)calculateFirmwareImageBlockAddress(0) + mFirmOfs;
        String cmdStr = "AT+FLSW=" + imgAddr + "," + mFirmBinLen + ",0,1,1\r\n";
        Log.d(TAG, "primeFirmwareCommand: " + cmdStr);
        boolean succeed = mDxDev.sendCmd(cmdStr.getBytes(), 1);

        if (!succeed) {
            Log.d(TAG, "Failed to send flash write command");
            mState = STATE_IDLE;
            mFirmwareWriteCompletedCallabck.onWriteCompleted(REQUEST_FAILED, null, "Failed to send flash write command");
            return true;
        }

        // Prepare new firmware meta
        byte[] newMetaBlk =
        {
                0x4A, 0x00, 0x00, 0x00, 0x10, 0x00, 0x01, 0x00,
                0x4A, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00
        };
        // calculate firmware binary CRC
        int crcFirmBin = crc16CalcOnData(mFirmBinary, mFirmBinary.length);

        // add scratch pad and update scratch pad length in the header
        JSONObject scratchPadJon = new JSONObject();
        try {
            scratchPadJon.put("CrcCode", crcFirmBin);
            scratchPadJon.put("Name", TextUtils.isEmpty(firmName)? "Unknown" : firmName);
        }
        catch(JSONException e) {
            e.printStackTrace();
        }

        String scratchPadStr = scratchPadJon.toString();
        byte[] scratchPad = scratchPadStr.getBytes();
        short l = (short) scratchPad.length;
        newMetaBlk[10] = (byte) l;
        newMetaBlk[11] = (byte) (l >> 8);

        // append scratch pad to meta
        byte[] tmp = new byte[newMetaBlk.length + scratchPad.length];
        System.arraycopy(newMetaBlk, 0, tmp, 0,  newMetaBlk.length);
        System.arraycopy(scratchPad, 0, tmp, newMetaBlk.length, scratchPad.length);
        newMetaBlk = tmp;

        // Calculate haeder CRC
        short crcHdr = (short) crc16CalcOnData(newMetaBlk, 72);
        byte[] crcHdrBin = {0, 0};
        crcHdrBin[0] = (byte) crcHdr;
        crcHdrBin[1] = (byte) (crcHdr >> 8);
        newMetaBlk[72] = crcHdrBin[0];
        newMetaBlk[73] = crcHdrBin[1];

        int firmwareCrc = crc16CalcOnData(mFirmBinary, mFirmBinary.length);
        int metaCrc = crc16CalcOnData(newMetaBlk, newMetaBlk.length);
        Log.i(TAG, "Firmware crc: " + firmwareCrc + ", Meta crc : " + metaCrc);
        mFirmCrcCodes = new HashMap<String, Object>();
        mFirmCrcCodes.put("0", String.valueOf(firmwareCrc));

        mFirmNewMetaBlk = newMetaBlk;
        mFirmwareProgressCallback = progressCallback;
        mRspStr = null;
        mFirmOfs = -1;
        mFirmStart = true;
        mFirmSlotIdx = 0;
        mState = STATE_FIRM_WRITE_IMG;

        return true;
    }

    public boolean retrieveFirmwareMetaWithProgress(FirmwareProgressCallback firmwareProgressCallback,
                                                    FirmwareMetaRetrievedCallback firmwareMetaRetrievedCallback) {

        if (mState != STATE_IDLE)
            return false;

        mFirmwareProgressCallback = firmwareProgressCallback;
        mFirmwareMetaRetrievedCallback = firmwareMetaRetrievedCallback;

        if (mShouldTryRetrieveFlashSize) {
            //retrieve flash size
            String cmdStr = "AT+FLSZ?\r\n";
            boolean success = mDxDev.sendCmd(cmdStr.getBytes(), 1);

            if (!success) {
                mFirmwareMetaRetrievedCallback.onMeteRetrieved(-1, null, "Invalid Firmware binary");
                mFirmwareMetaRetrievedCallback = null;

                return true;
            }

            mRspStr = null;
            mFirmMetaBlk = null;
            mState = STATE_FIRM_READ_FLASH_SZ;
            mShouldTryRetrieveFlashSize = false;
        }
        else {
            //use 0xFF000, 4069 as flash size if cmd AT_FLSR is not supported
            String cmdStr = "AT+FLSR=" + mFirmMetaAddr + "," + mFirmMetaLen + "\r\n";
            boolean success = mDxDev.sendCmd(cmdStr.getBytes(), 1);
            if (!success) {
                mFirmwareMetaRetrievedCallback.onMeteRetrieved(-1, null, "Invalid Firmware binary");
                mFirmwareMetaRetrievedCallback = null;

                return true;
            }

            mRspStr = null;
            mFirmMetaBlk = null;
            mState = STATE_FIRM_READ_META;
        }

        return false;
    }

    public boolean writeFirmwareImageInSlot(byte slotIdx, byte[] firmData, byte[] scratchPad,
                                            FirmwareProgressCallback progressCallback,
                                            FirmwareWriteCompletedCallback completedCallback) {
        if (mState != STATE_IDLE)
            return false;

        mFirmwareWriteCompletedCallabck = completedCallback;

        if (mFirmMetaBlk == null) {
            //must at least read firmware meta once before
            mFirmwareWriteCompletedCallabck.onWriteCompleted(REQUEST_FAILED, null, "error: must at least read firmware meta once before");
            mFirmwareWriteCompletedCallabck = null;
            return true;
        }

        if (slotIdx == 0) {
            //slot id must be > 1
            mFirmwareWriteCompletedCallabck.onWriteCompleted(REQUEST_FAILED, null, "slot idx must be larger than 1");
            mFirmwareWriteCompletedCallabck = null;
            return true;
        }

        //verify source file
        if (firmData.length != 0x20000) {
            mFirmwareWriteCompletedCallabck.onWriteCompleted(REQUEST_FAILED, null, "error: invalid firmware binary");
            mFirmwareWriteCompletedCallabck = null;
            return true;
        }

        // Only take 31 x 4KB last
        mFirmBinary = Arrays.copyOfRange(firmData, 0, 0x1F000);

        long imgAddr = calculateFirmwareImageBlockAddress(slotIdx);
        String cmdStr = "AT+FLSW=" + imgAddr + "," + mFirmBinary.length + ",0,1,1\r\n";
        Log.d(TAG, "cmd sending " + cmdStr);
        //not using fast down yet
        //String cmdStr = "AT+FLSW=" + imgAddr + "," + mFirmBinary.length + ",1,1\r\n";
        boolean success = mDxDev.sendCmd(cmdStr.getBytes(), 1);
        if (!success) {
            mState = STATE_IDLE;
            mFirmwareWriteCompletedCallabck.onWriteCompleted(REQUEST_FAILED, null, "Error: Failed to send flash write command");
            mFirmwareWriteCompletedCallabck = null;
            return true;
        }

        //prepare new firmware meta
        //FirmwareMeta metaBlk = new FirmwareMeta();
        FirmwareMeta metaBlk = FirmwareMeta.fromBinary(mFirmMetaBlk);

        if (metaBlk.version != 0) {
            Log.d(TAG, "Can't handle the newer meta");
            mState = STATE_IDLE;
            mFirmwareWriteCompletedCallabck.onWriteCompleted(REQUEST_FAILED, null, "Can't handle the newer meta");
            mFirmwareWriteCompletedCallabck = null;
            return true;
        }

        //
        int slotValidMaskSz = ((metaBlk.maxSlot+15)/16) * 2;
        byte[] slotValidMask = Arrays.copyOfRange(DxAppHelper.toBytes(metaBlk.slotValidMask), 0, slotValidMaskSz);
        byte[] scratchPadSum = null;

        for (int i = 0; i < slotValidMaskSz; i++) {
            if (slotIdx/8==i)
                slotValidMask[i] |= 1 << (slotIdx % 8);
        }

        int slotMetaOfs = 6 + slotValidMaskSz;
        int pt = slotMetaOfs;

        byte[] scratchPadHdr = Arrays.copyOfRange(metaBlk.bytes, pt, pt + metaBlk.maxSlot * 4);
        List<Integer> indexArray = new ArrayList<Integer>();
        for (int i = 0 ; i < metaBlk.maxSlot; i++) {

            int metaPtr = (int)DxAppHelper.toUnsignedInt(metaBlk.bytes[pt]);
            pt += 2;
            int metaLen = (int)DxAppHelper.toUnsignedInt(metaBlk.bytes[pt]);
            pt += 2;

            if (slotIdx == i) {

                if (scratchPad == null || scratchPad.length == 0)
                    indexArray.add(0);
                else {
                    scratchPadSum = (scratchPadSum == null) ?
                            scratchPad : DxAppHelper.appendData(scratchPadSum, scratchPad);
                    indexArray.add(scratchPad.length);
                }
                continue;

            }
            if(metaLen > 0)  {
                byte[] pad = Arrays.copyOfRange(metaBlk.bytes, metaPtr, metaPtr + metaLen);
                scratchPadSum = (scratchPadSum == null) ?
                        pad : DxAppHelper.appendData(scratchPadSum, pad);
                indexArray.add(pad.length);
            }
            else
                indexArray.add(0);
        }

        pt = 0;
        for (int i = 0, l = 0, k = slotMetaOfs+metaBlk.maxSlot*4+2; i < metaBlk.maxSlot; i++) {
            l = indexArray.get(i);
            scratchPadHdr[pt] = (byte)(l==0?0:k);
            pt += 2;
            scratchPadHdr[pt] = (byte)l;
            pt += 2;
            k+= l;
        }

        //create new firmware meta block
        byte[] newMetaBlk = Arrays.copyOfRange(mFirmMetaBlk, 0, 6);
        newMetaBlk = DxAppHelper.appendData(newMetaBlk, slotValidMask);
        newMetaBlk = DxAppHelper.appendData(newMetaBlk, scratchPadHdr);

        int crc = crc16CalcOnData(newMetaBlk, newMetaBlk.length);
        byte[] tmp = new byte[2];
        System.arraycopy(DxAppHelper.toBytes(crc), 2, tmp, 0, 2);
        byte t = tmp[0];
        tmp[0] = tmp[1];
        tmp[1] = t;
        newMetaBlk = DxAppHelper.appendData(newMetaBlk, tmp);
        newMetaBlk = DxAppHelper.appendData(newMetaBlk, scratchPadSum);
        //DxAppHelper.appendData(newMetaBlk, DxAppHelper.toBytes((short)crc));
        mFirmNewMetaBlk = newMetaBlk;

        mFirmCrcCodes.put(String.valueOf(slotIdx), crc16CalcOnData(mFirmBinary, mFirmBinary.length));

        //
        mFirmwareProgressCallback = progressCallback;
        mRspStr = null;
        mFirmOfs = -1;
        mFirmDownFast = true;
        //mFirmDownFast = false;
        mFirmSlotIdx = slotIdx;
        mState = STATE_FIRM_WRITE_IMG;

        return true;
    }

    public boolean deleteFirmwareImageFromSlot(byte slotIdx, FirmwareProgressCallback progressCallback, FirmwareWriteCompletedCallback completedCallback) {
        if (mState != STATE_IDLE) {
            return false;
        }

        mFirmwareWriteCompletedCallabck = completedCallback;
        if (mFirmMetaBlk == null) {
            //must retrieve meta first
            mState = STATE_IDLE;
            mFirmwareWriteCompletedCallabck.onWriteCompleted(REQUEST_FAILED, null, "Must read firmware meta once");
            mFirmwareWriteCompletedCallabck = null;

            return true;
        }

        if (slotIdx == 0) {
            //the first slot cannot be deleted
            mState = STATE_IDLE;
            mFirmwareWriteCompletedCallabck.onWriteCompleted(REQUEST_FAILED, null, "slot index must be larger 1");
            mFirmwareWriteCompletedCallabck = null;

            return true;
        }


        //prepare new meta
        byte[] meta = serializeMeta(mFirmMetaBlk, slotIdx, null);
        if (meta != null) {}
        else {
            mState = STATE_IDLE;
            mFirmwareWriteCompletedCallabck.onWriteCompleted(REQUEST_FAILED, null, "Can't handle new meta");
            mFirmwareWriteCompletedCallabck = null;

            return true;
        }

        String cmdStr = "AT+FLSW=" + mFirmMetaAddr + "," +   mFirmNewMetaBlk.length + ",1,1\r\n";
        boolean success = mDxDev.sendCmd(cmdStr.getBytes(), 1);

        Log.d(TAG, "Firmware: " + cmdStr);

        if (!success) {
            mState  = STATE_IDLE;
            mFirmwareWriteCompletedCallabck.onWriteCompleted(REQUEST_FAILED, null, "Cannot update firmware meta");
        }

        mFirmwareProgressCallback = progressCallback;
        mRspStr = null;
        mFirmOfs = -1;
        mState = STATE_FIRM_UPDATE_META;

        return true;
    }

    public boolean processDidWriteWithError() {
        return false;
    }

    public boolean processRxData(byte[] data) {
        return false;
    }

    public boolean processRx2Data(byte[] data) {
        if (mState == STATE_IDLE)
            return false;
        String newStr = new String(data);
        if (mRspStr == null)
            mRspStr = "";

        mRspStr += newStr;

        if ((mState < 6 || 9 < mState) && mState != 4) {
            throw new IllegalStateException("illegal state reached - " + mState);
        }

        switch (mState) {
//            case STATE_FIRM_READ_FLASH_SZ: {
//                if (mRspStr.contains("ERR=")) {
//                    //command AT+FLSZ? not supported
//                    //assumed flash size to be 0x800000
//
//                    //use AT+FLSR=0x7FF000,4096 to retrieve meta data
//                    String cmdStr = "AT+FLSR=" + mFirmMetaAddr + "," + mFirmMetaLen + "\r\n";
//                    boolean success = mDxDev.sendCmd(cmdStr.getBytes(), 1);
//
//                    if (!success) {
//                        mState = STATE_IDLE;
//                        mFirmwareMetaRetrievedCallback.onMeteRetrieved(REQUEST_FAILED, null, "Invalid firmware binary");
//                        mFirmwareMetaRetrievedCallback = null;
//                        return true;
//                    }
//
//
//                    mRspStr = null;
//                    mFirmMetaBlk = null;
//                    mState = STATE_FIRM_READ_META;
//                    return true;
//                }
//
//                if (!mRspStr.contains("OK\r\n"))
//                    return true;
//
//                if (!mRspStr.contains("+FLSZ:")) {
//                    //command not supported
//                    mState = STATE_IDLE;
//                    //mFirmwareMetaRetrievedCallback.onCompleted(REQUEST_FAILED, );
//                    return true;
//                }
//
//                String[] rspStrs = mRspStr.split("\r\n");
//                for (String rspStr : rspStrs) {
//                    if (rspStr.contains("+FLSZ:")) {
//                        long val = -1;
//                        String tmp = (rspStr.length() > 6) ? rspStr.substring(6): "";
//
//                        boolean success;
//                        try {
//                            if (tmp.startsWith("0x"))
//                                tmp = tmp.substring(2);
//                            val = Long.parseLong(tmp, 16);
//                            success = true;
//                        }
//                        catch(Exception e) {
//                            e.printStackTrace();
//                            success = false;
//                        }
//                        if (success) {
//                            //update firmware base address and meta address
//                            mFirmImgBaseaAddr = val;
//                            mFirmMetaAddr = mFirmImgBaseaAddr - mFirmMetaLen;
//
//                            if (val <= 0x20000) {
//                                mState = STATE_IDLE;
//                                mFirmwareMetaRetrievedCallback.onMeteRetrieved(REQUEST_FAILED, null, "Incorrect Flash Size");
//
//                                return true;
//                            }
//                        }
//
//                        // AT+FLSR=0x7FF000,4096 retrieve meta data
//                        String cmdStr = "AT+FLSR=" + mFirmMetaAddr + "," + mFirmMetaLen + "\r\n";
//                        success = mDxDev.sendCmd(cmdStr.getBytes(), 1);
//
//                        if (!success) {
//                            mState = STATE_IDLE;
//                            mFirmwareMetaRetrievedCallback.onMeteRetrieved(REQUEST_FAILED, null, "Invalid firmware binary");
//
//                            mFirmwareMetaRetrievedCallback = null;
//
//                            return true;
//                        }
//
//
//                        mRspStr = null;
//                        mFirmMetaBlk = null;
//                        mState = STATE_FIRM_READ_META;
//
//                        return true;
//                    }
//                }
//
//                break;
//            }
//
//            case STATE_FIRM_READ_META: {
//                if (mRspStr.contains("ERR=")) {
//                    mFirmwareMetaRetrievedCallback.onMeteRetrieved(REQUEST_FAILED, null, "Command not supported");
//                    mFirmwareMetaRetrievedCallback = null;
//                    return true;
//                }
//
//                if (mRspStr.contains("OK\r\n")) {
//                    String[] tmp = mRspStr.split("OK\r\n");
//                    if (tmp.length > 1)
//                        mRspStr = tmp[1];
//                }
//
//                if (!mRspStr.contains("+FLSR:")) {
//                    if (mFirmwareProgressCallback != null) {
//                        double progress = (double)mRspStr.length() / (4096 / 16 * 50);
//                        mFirmwareProgressCallback.onProgress(RETRIEVE_META, progress);
//                    }
//                    return true;
//                }
//
//                //check for \r\n
//                String[] rspStrs = mRspStr.split("\r\n");
//                boolean success = true;
//                byte[] metaData = null;
//                for (String rspStr : rspStrs) {
//                    if (rspStr.startsWith("AT+") || rspStr.startsWith("OK") || rspStr.startsWith("+FLSR:"))
//                        continue;
//                    String[] hexStrs = rspStr.split(" ");
//                    for (String hexStr : hexStrs) {
//                        int val;
//                        try {
//                            val = Integer.parseInt(hexStr, 16);
//                            success = true;
//                        }
//                        catch(Exception e) {
//                            e.printStackTrace();
//                            success = false;
//                            break;
//                        }
//
//                        byte[] unsignedBin = DxAppHelper.signedIntToUsignedByteArray(val);
//                        if (metaData == null) {
//                            metaData = new byte[1];
//                            metaData[0] = unsignedBin[unsignedBin.length - 1];
//                        }
//                        else {
//                            //metaData = DxAppHelper.appendData(metaData, unsignedBin, 1);
//                            byte[] tmp = new byte[metaData.length + 1];
//                            System.arraycopy(metaData, 0, tmp, 0, metaData.length);
//                            tmp[tmp.length - 1] = unsignedBin[unsignedBin.length - 1];
//                            metaData = tmp;
//                        }
//                    }
//
//                    if (!success) {
//                        break;
//                    }
//                }
//                if (!success) {
//                    mState = STATE_IDLE;
//                    mFirmwareMetaRetrievedCallback.onMeteRetrieved(REQUEST_ERR_READ_META, null, "Error reading meta data");
//                    mFirmwareMetaRetrievedCallback = null;
//                    return true;
//                }
//
//                //if meta data is ready
//                mFirmMetaBlk = metaData;
//                mRspStr = null;
//
//                mFirmMetas = deserializeMeta(metaData, false);
//                if (mFirmMetas == null) {
//
//                    //mFirmMetaBlk = serializeMeta(metaData)
////                    deleteFirmwareImageFromSlot((byte)2, new FirmwareProgressCallback() {
////                        @Override
////                        public void onProgress(int stage, double progress) {
////
////                        }},new FirmwareWriteCompletedCallback() {
////                        @Override
////                        public void onWriteCompleted(int status, Map<String, Object> metas, String msg) {
////
////                        }
////                    });
//                    mState = STATE_IDLE;
//                    mFirmwareMetaRetrievedCallback.onMeteRetrieved(REQUEST_FAILED, null, "");
//                    return true;
//                }
//
//                String cmdStr = "AT+VS?\r\n";
//                boolean succeed = mDxDev.sendCmd(cmdStr.getBytes(), 1);
//                if (!succeed) {
//                    mState = STATE_IDLE;
//                    mFirmwareWriteCompletedCallabck.onWriteCompleted(REQUEST_FAILED, null, "Invalid firmware binary");
//                    mFirmwareWriteCompletedCallabck = null;
//                    return true;
//                }
//
//                mState = STATE_FIRM_VERSIONS;
//                break;
//            }
//
//            case STATE_FIRM_VERSIONS: {
//
//                if (mRspStr.contains("ERR=")){
//                    mState = STATE_IDLE;
//                    mFirmwareMetaRetrievedCallback.onMeteRetrieved(REQUEST_FAILED, null, "Command not supported");
//                    return true;
//                }
//
//                if (!mRspStr.contains("OK\r\n"))
//                    return true;
//                if(!mRspStr.contains("+VS")) {
//                    mState = STATE_IDLE;
//                    mFirmwareMetaRetrievedCallback.onMeteRetrieved(REQUEST_FAILED, null, "Command not supported");
//                    mFirmwareMetaRetrievedCallback = null;
//                    return true;
//                }
//
//                String[] rspStrs  = mRspStr.split("\r\n");
//                for (String rspStr : rspStrs) {
//                    if (rspStr.contains("+VS:")) {
//                        String[] subRspStrs = rspStr.split(",");
//                        if (subRspStrs.length != 4) {
//                            mFirmwareMetaRetrievedCallback.onMeteRetrieved(REQUEST_FAILED, null, "Fail retrieving versions");
//                            mFirmwareMetaRetrievedCallback = null;
//                            return true;
//                        }
//
//                        String cmdStr = "AT+IMG?\r\n";
//                        boolean success = mDxDev.sendCmd(cmdStr.getBytes(), 1);
//                        Log.d(TAG, "firmware: " + cmdStr);
//
//                        if (!success) {
//                            mState = STATE_IDLE;
//                            mFirmwareMetaRetrievedCallback.onMeteRetrieved(REQUEST_FAILED, null, "Invalid firmware binary");
//                            mFirmwareMetaRetrievedCallback = null;
//                            return true;
//                        }
//
//                        mFirmSysVersions = new HashMap<String, Object>();
//                        mFirmSysVersions.put("HwVer", subRspStrs[0]);
//                        mFirmSysVersions.put("FirmVer", subRspStrs[1]);
//                        mFirmSysVersions.put("SerialNo", subRspStrs[2]);
//                        mFirmSysVersions.put("Capability", subRspStrs[3]);
//                        mFirmMetas.put("SysVersions", mFirmSysVersions);
//
//                        mState = STATE_FIRM_WHICH_SLOT;
//                        return true;
//
//                    }
//                }
//
//                mState = STATE_IDLE;
//                mFirmwareMetaRetrievedCallback.onMeteRetrieved(REQUEST_FAILED, null, "Failed retrieving version");
//                mFirmwareMetaRetrievedCallback = null;
//
//                break;
//            }
//
//            case STATE_FIRM_WHICH_SLOT: {
//
//                if (mRspStr.contains("ERR=")){
//                    mState = STATE_IDLE;
//                    mFirmwareMetaRetrievedCallback.onMeteRetrieved(REQUEST_FAILED, null, "Command not supported");
//                    return true;
//                }
//
//                if (!mRspStr.contains("OK\r\n"))
//                    return true;
//                if(!mRspStr.contains("+IMG:"))
//                    return true;
//
//                String[] rspStrs = mRspStr.split("\r\n");
//                boolean success = false;
//                int slotIdx = 0;
//                for (String rspStr : rspStrs) {
//                    if (rspStr.contains("+IMG:")) {
//                        String slotIdxStr = rspStr.substring(5);
//                        try {
//                            slotIdx = Integer.parseInt(slotIdxStr);
//                            success = true;
//                        }
//                        catch(Exception e) {
//                            success = false;
//                        }
//                        break;
//                    }
//                }
//
//                if (!success) {
//                    mState = STATE_IDLE;
//                    mFirmwareMetaRetrievedCallback.onMeteRetrieved(REQUEST_FAILED, null, "Command not find image slot");
//                    mFirmwareMetaRetrievedCallback = null;
//                    return true;
//                }
//
//                // Added image slot
//                mFirmMetas.put("SlotIdx", slotIdx);
//                mFirmCrcCodes = new HashMap<String, Object>();
//
//                //find all valid slots for validation
//                byte[] slotValidMask = (byte[]) mFirmMetas.get("SlotValidMask");
//                mFirmSlotValidSet = new LinkedHashSet<Integer>();
//                for (int x = 0; x < slotValidMask.length; x++) {
//                    byte b = slotValidMask[x];
//                    for (int y = 0; y  < 8; y++) {
//                        int tmp = b & (1 << y);
//                        if (tmp != 0) {
//                            mFirmSlotValidSet.add(y + x * 8);
//                        }
//                    }
//                }
//
//                if (mFirmSlotValidSet.size() == 0) {
//                    mState = STATE_IDLE;
//                    mFirmMetas.put("CrcCodes", null);
//                    mFirmwareMetaRetrievedCallback.onMeteRetrieved(REQUEST_SUCCEED, mFirmMetas, "");
//                    mFirmwareMetaRetrievedCallback = null;
//
//                    break;
//                }
//
//                mFirmSlotValidTotal = mFirmSlotValidSet.size();
//                mFirmCrcChkSlotIdx = (Integer)mFirmSlotValidSet.toArray()[0];
//                DxAppHelper.removeFromSet(mFirmSlotValidSet, mFirmCrcChkSlotIdx);
//
//                long imgAddr = calculateFirmwareImageBlockAddress(mFirmCrcChkSlotIdx);
//                String cmdStr = "AT+FLSV=0x" + Long.toHexString(imgAddr )+ ",0x1F000,0\r\n";
//                success =  mDxDev.sendCmd(cmdStr.getBytes(), 1);
//
//                if (!success) {
//                    mState = STATE_IDLE;
//                    mFirmwareMetaRetrievedCallback.onMeteRetrieved(REQUEST_FAILED, null, "Invalid firmware binary");
//                    mFirmwareMetaRetrievedCallback = null;
//
//                    return true;
//                }
//
//                if (mFirmwareProgressCallback != null) {
//                    mFirmwareProgressCallback.onProgress(mFirmCrcChkSlotIdx + 1, (double) mFirmSlotValidTotal - mFirmSlotValidSet.size() / mFirmSlotValidTotal);
//                }
//
//                mState = STATE_FIRM_CRC_CHECK;
//                break;
//            }
//
//            case STATE_FIRM_CRC_CHECK: {
//
//                if (mRspStr.contains("ERR=")){
//                    mState = STATE_IDLE;
//                    mFirmwareMetaRetrievedCallback.onMeteRetrieved(REQUEST_FAILED, null, "Command not supported");
//                    return true;
//                }
//
//                if (!mRspStr.contains("OK\r\n"))
//                    return true;
//
//                if (!mRspStr.contains("+FLSV:"))
//                    return true;
//
//                String[] rspStrs = mRspStr.split("\r\n");
//                boolean success = false;
//                int crcCalc = 0;
//
//                for (String rspStr : rspStrs ) {
//                    if (rspStr.contains("+FLSV:")) {
//                        String tmp = rspStr.substring(8);
//                        try {
//                            crcCalc = Integer.parseInt(tmp);
//                            success = true;
//                        }
//                        catch (Exception e) {
//                            success = false;
//                        }
//                        break;
//                    }
//                }
//
//                if (!success) {
//                    mState = STATE_IDLE;
//                    mFirmwareMetaRetrievedCallback.onMeteRetrieved(REQUEST_FAILED, null, "CRC not retrieved");
//                    mFirmwareMetaRetrievedCallback = null;
//                    return true;
//                }
//
//                mFirmCrcCodes.put(mFirmCrcChkSlotIdx+"", crcCalc);
//                if (mFirmSlotValidSet.size() == 0) {
//                    mState = STATE_IDLE;
//                    mFirmMetas.put("CrcCodes", mFirmCrcCodes);
//                    mFirmwareMetaRetrievedCallback.onMeteRetrieved(REQUEST_SUCCEED, mFirmMetas, null);
//                    mFirmwareMetaRetrievedCallback = null;
//                    break;
//                }
//
//                mFirmCrcChkSlotIdx = (Integer) mFirmSlotValidSet.toArray()[0];
//                mFirmSlotValidSet.remove(mFirmCrcChkSlotIdx);
//
//                long imgAddr = calculateFirmwareImageBlockAddress(mFirmCrcChkSlotIdx);
//                String cmdStr = "AT+FLSV=0x" + Long.toHexString(imgAddr )+ ",0x1F000,0\r\n";
//                success = mDxDev.sendCmd(cmdStr.getBytes(), 1);
//                Log.d(TAG, "Firmware: " + cmdStr);
//
//                if (!success) {
//                    mState = STATE_IDLE;
//                    mFirmwareMetaRetrievedCallback.onMeteRetrieved(REQUEST_FAILED, null, "CRC check failed");
//                    mFirmwareMetaRetrievedCallback = null;
//                    return true;
//                }
//
//                mRspStr = null;
//                if (mFirmwareProgressCallback != null) {
//                    mFirmwareProgressCallback.onProgress(mFirmCrcChkSlotIdx +1, (double)(mFirmSlotValidTotal - mFirmSlotValidSet.size()) / mFirmSlotValidTotal);
//                }
//
//                break;
//            }

            case STATE_FIRM_WRITE_IMG: {

                if (mRspStr.contains("ERR=")) {
                    if (mFirmDownFast) {
                        // Firmware device doesn't support fast download
                        // - do the slow way
                        Log.i(TAG, "Firmware device doesn't support fast download - do the slow way");

                        mFirmDownFast = false;
                        long imgAddr = calculateFirmwareImageBlockAddress(mFirmSlotIdx);
                        String cmdStr = "AT+FLSW=" + imgAddr + "," + mFirmBinary.length + ",0,1\r\n"; //Long.toHexString(imgAddr )
                        boolean success = mDxDev.sendCmd(cmdStr.getBytes(), 1);

                        if (!success) {
                            mState = STATE_IDLE;
                            mFirmwareWriteCompletedCallabck.onWriteCompleted(REQUEST_FAILED, null, "Failed to send flash write command");
                            mFirmwareWriteCompletedCallabck = null;

                            return true;
                        }

                        mRspStr = null;
                        return true;
                    }

                    // command not supported
                    mState = STATE_IDLE;
                    mFirmwareWriteCompletedCallabck.onWriteCompleted(REQUEST_FAILED, null, "Can't get current image slot");
                    mFirmwareWriteCompletedCallabck = null;
                    return true;
                }

                if (mRspStr.contains("OK\r\n")) {
                    if (!mFirmDownFast) {
                        Log.i(TAG, "Fast download not enabled - do the slow way");
                        int len = mFirmBinary.length;
                        if (len > 20 )
                            len = 20;

                        // Slow download - use cmd channel to send firmware
                        mDxDev.sendCmd(mFirmBinary, 1);
                        mFirmOfs = len;
                    }
                    else {
                        initFastDown();
                    }

                    mRspStr = null;
                    return true;
                }

                if (!mRspStr.contains("+FLSW:"))
                    return true;

                if (mRspStr.contains("+FLSW:0,")) {
                    //Success. next update firmware meta
                    Log.i(TAG, "Firmware downloaded complete - receive: " + mRspStr);

                    String cmdStr = "AT+FLSW=" + mFirmMetaAddr + "," + mFirmNewMetaBlk.length + ",0,1\r\n";
                    Log.i(TAG, "Write meta - send: " + cmdStr);

                    boolean success = mDxDev.sendCmd(cmdStr.getBytes(), 1);
//                    if (!success) {
//                        mState = STATE_IDLE;
//                        Log.e(TAG, "Invalid firmware binary");
//                        mFirmwareWriteCompletedCallabck.onWriteCompleted(REQUEST_FAILED, null, "Invalid firmware binary");
//                        mFirmwareWriteCompletedCallabck = null;
//
//                        return true;
//                    }

                    mRspStr = null;
                    mFirmOfs = -1;
                    mState  = STATE_FIRM_UPDATE_META;

                }
                else {
                    mState = STATE_IDLE;
                    mFirmwareWriteCompletedCallabck.onWriteCompleted(REQUEST_FAILED, null, "Failed to write image");
                    mFirmwareWriteCompletedCallabck = null;
                }

                break;
            }

            case STATE_FIRM_UPDATE_META: {
                if (mRspStr.contains("ERR=")) {
                    //Command not supported
                    Log.e(TAG, "Command not supported: " + mRspStr);
                    mState = STATE_IDLE;
                    mFirmwareWriteCompletedCallabck.onWriteCompleted(REQUEST_FAILED, null, "Command not supported");
                    mFirmwareWriteCompletedCallabck = null;

                    return true;
                }

                if (mRspStr.contains("OK\r\n")) {
                    int len = mFirmNewMetaBlk.length;
                    if (len > 20)
                        len = 20;

                    int metaCrc = crc16CalcOnData(mFirmNewMetaBlk, mFirmNewMetaBlk.length);
                    Log.i(TAG, "Send meta: " + metaCrc);
                    mDxDev.sendCmd(mFirmNewMetaBlk ,1);
                    mFirmOfs = len;
                    mRspStr = null;

                    return true;
                }

                if (!mRspStr.contains("+FLSW:"))
                    return true;

                if (mRspStr.contains("+FLSW:0,")) {
                    // Write meta is successful. Verify image CRC now
                    Log.i(TAG, "Write meta is successful - receive: " + mRspStr);
                    long imgAddr = calculateFirmwareImageBlockAddress(mFirmSlotIdx);
                    String cmdStr = "AT+FLSV=0x" + Long.toHexString(imgAddr).toUpperCase() + ",0x1F000,0\r\n";
                    Log.i(TAG, "Verify CRC - send: " + cmdStr);

                    boolean succeed = mDxDev.sendCmd(cmdStr.getBytes() , 1);
                    if (!succeed) {
                        mState = STATE_IDLE;
                        return true;
                    }

                    mRspStr = null;
                    mState = STATE_FIRM_CRC_CHECK_AFTER_WRITE;
                }
                else  {
                    Log.e(TAG, "Failed to write meta");
                    mState = STATE_IDLE;
                    mFirmwareWriteCompletedCallabck.onWriteCompleted(REQUEST_FAILED, null, "Failed to write image");
                    mFirmwareWriteCompletedCallabck = null;
                }

                break;
            }
            case STATE_FIRM_CRC_CHECK_AFTER_WRITE: {
                if (mRspStr.contains("ERR=")) {
                    // Command not supported
                    mState = STATE_IDLE;

                    mFirmwareWriteCompletedCallabck.onWriteCompleted(REQUEST_FAILED, null, "CRC check command error");
                    mFirmwareWriteCompletedCallabck = null;

                    return true;
                }

                if (!mRspStr.contains("OK\r\n")) {
                    return true;
                }

                if (!mRspStr.contains("+FLSV:")) {
                    return true;
                }

                Log.i(TAG, "Verify CRC - receive: " + mRspStr);

                //Check for "\r\n"
                String[] rspStrs = mRspStr.split("\r\n");
                boolean succeed = false;
                int crcVerify = 0;
                for (String rspStr : rspStrs) {
                    if (rspStr.contains("+FLSV:")) {
                        String crc = rspStr.substring(8);
                        Log.i(TAG, "crc: " + crc);
                        //succeed = rspStr.substring(8).contains(String.valueOf(crcVerify));
                        break;
                    }
                }

//                if (!succeed) {
//                    Log.e(TAG, "CRC not retrieved");
//                    mState = STATE_IDLE;
//                    mFirmwareWriteCompletedCallabck.onWriteCompleted(REQUEST_FAILED, null, "CRC not retrieved");
//                    return true;
//                }

                int slotIdxNum = mFirmSlotIdx;
                int crcCalc = Integer.parseInt((String)mFirmCrcCodes.get(String.valueOf(slotIdxNum)));
                mFirmCrcCodes.put(String.valueOf(slotIdxNum), crcVerify);

                mFirmMetas = deserializeMeta(mFirmNewMetaBlk, false);
                if (mFirmMetas == null) {
                    Log.e(TAG, "Failed deserialize meta");
                    mState = STATE_IDLE;
                    mFirmwareWriteCompletedCallabck.onWriteCompleted(REQUEST_FAILED, null, "Failed deserialize meta");
                    return true;
                }

                mFirmMetas.put("FlashSize", mFirmImgBaseaAddr);
                mFirmMetas.put("DevUUID", "");
                mFirmMetas.put("CrcCodes", mFirmCrcCodes);
                mFirmMetas.put("SysVersions", mFirmSysVersions);
                mFirmMetas.put("ImgIntegrity", true);

                mRspStr = null;
                mState = STATE_IDLE;
                mFirmwareWriteCompletedCallabck.onWriteCompleted(REQUEST_SUCCEED, mFirmMetas, "");
                mFirmwareWriteCompletedCallabck = null;

                return true;
            }
        }

        return true;
    }

    private boolean mPktSnding = false;
    private Timer resentTimer;
    private Object timerLock = new Object();
    private int sendPkt = 0;

    private int buffer = BT_PKT_ON_FLIGHT;
    private List<Byte> binCheck = new ArrayList<Byte>();

    private void initFastDown() {
        long crc = crc16CalcOnData(mFirmBinary, mFirmBinary.length);
        Log.i(TAG, "Start fast download - firmware size: " + mFirmBinary.length + " with crc: " + crc + " - receive: " + mRspStr);
        mDxDev.writeTxCreditReportLoopCount(BT_PKT_ON_FLIGHT);
        mRebaseTxCredit = true;
        mFirmOfs = 0;
        sendPkt = 0;
    }


//    private void initFastDown() {
//        long crc = crc16CalcOnData(mFirmBinary, mFirmBinary.length);
//        Log.i(TAG, "Start fast download - firmware size: " + mFirmBinary.length + " with crc: " + crc + " - receive: " + mRspStr);
//        sendFirmwareSegment();
//    }

    public boolean processTxCredit(int credits) {
        if (mState == STATE_FIRM_WRITE_IMG && mFirmBinary != null) {

            // reset buffer available size
            buffer = BT_PKT_ON_FLIGHT;
            if (!mPktSnding) {
                sendFirmwareSegment();
            }

            if (mFirmwareProgressCallback != null) {
                double progress = (double)mFirmOfs / (double)mFirmBinary.length;
                Log.i(TAG, "pkt send finished - firm offset: " + mFirmOfs + ", sent pkt: " + sendPkt + ", received pkt: " + credits + ", progress: " + progress);
                mFirmwareProgressCallback.onProgress(0, progress);
            }
        }

        return false;
    }

    public void onCharacteristicWrite(BluetoothGattCharacteristic c) {
        if (mState == STATE_FIRM_WRITE_IMG && mFirmBinary != null && mFirmOfs >= 0) {

            if (c.getUuid().equals(UUID.fromString(DataExchangerProfile.TX_CREDIT_UUID))) {
                Log.i(TAG, "tx credit write completed");
            }
            if (mFirmOfs >= mFirmBinary.length) {
                Log.i(TAG, "all data sent");
                return;
            }

            if (buffer <= 0) {
                mPktSnding = false;
                return;
            }

            mPktSnding = true;

            sendFirmwareSegment();
        }
    }

    private synchronized boolean sendFirmwareSegment() {

        if (buffer == 0) {
            throw new IllegalStateException("empty buffer");
        }

        mPktSnding = true;
        int len = mFirmBinary.length - mFirmOfs;

        if (len <= 0) {
            return true;
        }

        if (len > 20)
            len = 20;

        byte[] tmp = new byte[len];
//        System.arraycopy(mFirmBinary, mFirmOfs, tmp, 0, len);
        for (int i = 0; i < len; i++) {
            tmp[i] = mFirmBinary[mFirmOfs + i];
        }
        for (byte b : tmp) {
            binCheck.add(b);
        }

        boolean succeed = mDxDev.sendData(tmp);

        if (succeed) {
            mFirmOfs += len;
            buffer--;
            sendPkt++;
        }
        else {
            Log.e(TAG, "firmware bin download was interrupted!");
        }
        return succeed;
    }

    private List<FirmBinSeg> toBinSendingQueue(byte[] firmBinary) {
        List<FirmBinSeg> queue = new ArrayList<FirmBinSeg>();
        int ofs = 0;
        int idx = 0;
        while(ofs < firmBinary.length) {
            int len = mFirmBinary.length - ofs;
            if (len > 20)
                len = 20;
            byte[] tmp = new byte[len];
            System.arraycopy(firmBinary, ofs, tmp, 0, len);
            ofs += len;
            FirmBinSeg seg = new FirmBinSeg();
            seg.id = idx;
            seg.binSeg = tmp;
            queue.add(seg);
            idx++;
        }


        return queue;
    }

    private void txCreditChkTimerExpired() {
        if (mLastTxCredit == mTxCredit) {
            int c = -1;
            try {
                //mDxDev.readTxCredit();
//                String txCreditHex = new String(credit);
//                c = DxAppHelper.byteArrayToInt(credit);
//                Log.d(TAG, "read txcredit " + c);
//                Log.d(TAG, "read txcredit hex " + txCreditHex);

            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        mLastTxCredit = mTxCredit;
    }

    private void initTimerTask() {
        timerTask = new TimerTask() {
            @Override
            public void run() {
                txCreditChkTimerExpired();
            }
        };
    }

    private boolean processDataLoggerDataWithResponseString(String rspStr) {
        return false;
    }

    private Map<String, Object> deserializeMeta(byte[] metaData, boolean isErr) {
        FirmwareMeta metaBlk = FirmwareMeta.fromBinary(metaData);

        String rspMsg=  new String(metaBlk.bytes);
        Log.d(TAG, "rspMsg: " + rspMsg);

        byte[] slotValidMask = null;
        List<byte[]> scratchPads = new ArrayList<byte[]>();

        if (metaBlk.version == 0) {
            Log.d(TAG, "Firmware meta header: " + metaBlk.length + "," + metaBlk.version + "," + metaBlk.maxSlot);
            int slotValidMaskWordSz = (metaBlk.maxSlot + 15) / 16;
            slotValidMask = new byte[slotValidMaskWordSz * 2];

            for (int i = 0; i < slotValidMaskWordSz; i++) {
                int word = metaBlk.slotValidMask[i];
                slotValidMask[i] = (byte)word;

                Log.d(TAG, "Firmware meta slot valid mask" + "[" + i + "]: " + word);
            }

            int slotMetaOfs = 6 + slotValidMaskWordSz * 2;
            int pt = slotMetaOfs;//metaBlk.bytes[slotMetaOfs];

            for (int i = 0; i < metaBlk.maxSlot; i++) {
                int metaPtr = (int)DxAppHelper.toUnsignedInt(metaBlk.bytes[pt]);
                pt+=2;
                int metaLen = (int)DxAppHelper.toUnsignedInt(metaBlk.bytes[pt]);
                pt+=2;

                if (metaLen == 0 || metaPtr == 0) {
                    scratchPads.add(new byte[1]);
                }
                else {
                    byte[] scratchPad = Arrays.copyOfRange(metaBlk.bytes, metaPtr, metaPtr+metaLen);
                    scratchPads.add(scratchPad);
                    String slotInfo = new String(scratchPad);
                    Log.d(TAG, "slot " + i + " info: " + slotInfo);

                }
            }
        }

        else  {
            if (isErr) {
                //TODO implement Version of meta not supported warning
                Log.d(TAG, "Version of meta not supported");
            }

            return null;
        }

        mState = STATE_IDLE;
        Map<String, Object> metas = new HashMap<String, Object>();
        metas.put("Version", metaBlk.version);
        metas.put("MaxSlot", metaBlk.maxSlot);
        metas.put("SlotValidMask", slotValidMask);
        metas.put("SlotScratchPads", scratchPads);

        return metas;
    }

    private byte[] serializeMeta(byte[] meta, int slotIdx, byte[] scratchPad) {
        FirmwareMeta metaBlk = FirmwareMeta.fromBinary(meta);
        if (metaBlk.version == 0) {
            int slotValidMaskSz = ((metaBlk.maxSlot + 15) / 16) * 2;
            byte[] slotValidMask = Arrays.copyOfRange(DxAppHelper.toBytes(metaBlk.slotValidMask), 0, slotValidMaskSz);
            byte[] scratchPadSum = null;

            for (int i = 0; i < slotValidMaskSz; i++) {
                if (slotIdx / 8 == i)
                    slotValidMask[i] &= ~(1<<(slotIdx % 8));
            }
            int slotMetaOfs = 6 + slotValidMaskSz;
            int pt = slotMetaOfs;

            byte[] scratchPadHdr = Arrays.copyOfRange(metaBlk.bytes, pt, pt + (metaBlk.maxSlot * 4));
            List<Integer>  indexArray = new ArrayList<Integer>();
            for (int i = 0; i < metaBlk.maxSlot; i++) {
                int metaPtr = (int)DxAppHelper.toUnsignedInt(metaBlk.bytes[pt]);
                pt+=2;
                int metaLen = (int)DxAppHelper.toUnsignedInt(metaBlk.bytes[pt]);
                pt+=2;

                if (slotIdx == i) {

                    if (scratchPad == null || scratchPad.length == 0)
                        metaLen = 0;
                    else {
                        scratchPadSum = (scratchPadSum == null)?
                            scratchPad : DxAppHelper.appendData(scratchPadSum, scratchPad);
                        indexArray.add(scratchPad.length);
                        continue;
                    }
                }

                if (metaLen > 0) {
                    byte[] pad = Arrays.copyOfRange(metaBlk.bytes, metaPtr, metaPtr + metaLen);
                    scratchPadSum = (scratchPadSum == null) ?
                            pad : DxAppHelper.appendData(scratchPadSum, pad);
                    indexArray.add(pad.length);
                }
                else {
                    indexArray.add(0);
                }
            }

            pt = 0;
            for (int i = 0, l = 0, k = slotMetaOfs + metaBlk.maxSlot * 4 + 2; i < metaBlk.maxSlot; i++) {
                l = indexArray.get(i);
                scratchPadHdr[pt] = (byte)((l == 0)?0:k); //ptr info
                pt += 2;
                scratchPadHdr[pt] = (byte)l;    //len info
                pt += 2;
                k += l;

            }

            //create new firmware meta block
            byte[] newMetaBlk = Arrays.copyOfRange(mFirmMetaBlk, 0, 6);
            newMetaBlk = DxAppHelper.appendData(newMetaBlk, slotValidMask);
            newMetaBlk = DxAppHelper.appendData(newMetaBlk, scratchPadHdr);
            int crc = DxAppFirmLogStateMachine.crc16CalcOnData(newMetaBlk, newMetaBlk.length);
            byte[] tmp = new byte[2];
            System.arraycopy(DxAppHelper.toBytes(crc), 2, tmp, 0, 2);
            byte t = tmp[0];
            tmp[0] = tmp[1];
            tmp[1] = t;
            newMetaBlk = DxAppHelper.appendData(newMetaBlk, tmp);
            newMetaBlk = DxAppHelper.appendData(newMetaBlk, scratchPadSum);
            mFirmNewMetaBlk = newMetaBlk;

            FirmwareMeta test = FirmwareMeta.fromBinary(newMetaBlk);

            //remove crc code
            mFirmCrcCodes.remove(slotIdx);

            Log.d(TAG, "test");
            return newMetaBlk;

        }

        return null;
    }

    private long calculateFirmwareImageBlockAddress(int slot) {
        return mFirmImgBaseaAddr - (mFirmImgBlkLen * (slot + 1));
    }

    private byte[] longToBytes(long from) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte)(from & 0xFF);
            from >>= 8;
        }
        return result;
    }

}
