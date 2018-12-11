package com.gttronics.ble.blelibrary.dataexchanger.models;

import com.gttronics.ble.blelibrary.dataexchanger.helpers.DxAppHelper;

import java.io.Serializable;

/**
 * Created by EGUSI16 on 11/28/2016.
 */

public class TransferPkt implements Serializable {
    public short signature;
    public int pktId;
    public byte[] pad = new byte[8];
    public int crc16 = 0;

    public byte[] toBinary() {
        byte[] tmp = DxAppHelper.toBytes(signature);
        tmp = DxAppHelper.appendData(tmp, DxAppHelper.toBytes(pktId));
        tmp = DxAppHelper.appendData(tmp, pad);
        tmp = DxAppHelper.appendData(tmp, DxAppHelper.toBytes(crc16));
        return tmp;
    }

    public static int getSize() {
        return 18; //byte
    }
}