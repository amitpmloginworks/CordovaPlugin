package com.gttronics.ble.blelibrary.dataexchanger.models;

import java.nio.ByteBuffer;

import com.gttronics.ble.blelibrary.dataexchanger.helpers.DxAppHelper;

/**
 * Created by EGUSI16 on 11/24/2016.
 */


/*
*
* first 6 bytes meta header length, version, maxslot
* following 2 bytes slot valid mask indicate whether a slot is available (one bit per slot total 16 bits)
* then follow by 16 x 4 byte info which indicate the start and length of a slot meta
* following bytes are slot info total 16 block (1 per slot) first 2 byte indicate starting pos and second 2 bytes indicate length
*
* */

public class FirmwareMeta {
    public short length;    //2bytes
    public short version;   //2bytes
    public short maxSlot;   //2bytes
    public short[] slotValidMask = new short[1];        //2bytes
    public byte[] bytes;

    public static FirmwareMeta fromBinary(byte[] binary) {
        FirmwareMeta meta = new FirmwareMeta();
        byte[] lenArr = new byte[2];
        byte[] verArr = new byte[2];
        byte[] slotArr = new byte[2];
        byte[] validMask = new byte[2];

        lenArr[0] = binary[1];
        lenArr[1] = binary[0];
        verArr[0] = binary[3];
        verArr[1] = binary[2];
        slotArr[0] = binary[5];
        slotArr[1] = binary[4];
        validMask[0] = binary[7];
        validMask[1] = binary[6];

        meta.length = ByteBuffer.wrap(lenArr).getShort();
        meta.version = ByteBuffer.wrap(verArr).getShort();
        meta.maxSlot = ByteBuffer.wrap(slotArr).getShort();
        meta.slotValidMask[0] = ByteBuffer.wrap(validMask).getShort();

        meta.bytes = new byte[binary.length];
        System.arraycopy(binary, 0, meta.bytes, 0, meta.bytes.length);

        return meta;
    }

    public static void testDeserialize() {
        String[] rspStrs = testMeta.split("\r\n");
        boolean success = true;
        byte[] metaData = null;
        for (String rspStr : rspStrs) {
            if (rspStr.startsWith("AT+") || rspStr.startsWith("OK") || rspStr.startsWith("+FLSR:"))
                continue;
            String[] hexStrs = rspStr.split(" ");
            for (String hexStr : hexStrs) {
                int val;
                try {
                    //TODO implement substring by regex pattern
                    val = Integer.parseInt(hexStr, 16);
                    success = true;
                }
                catch(Exception e) {
                    e.printStackTrace();
                    success = false;
                    break;
                }

                byte[] unsignedBin = DxAppHelper.signedIntToUsignedByteArray(val);
                if (metaData == null) {
                    metaData = new byte[1];
                    metaData[0] = unsignedBin[unsignedBin.length - 1];
                }
                else {
                    //metaData = DxAppHelper.appendData(metaData, unsignedBin, 1);
                    byte[] tmp = new byte[metaData.length + 1];
                    System.arraycopy(metaData, 0, tmp, 0, metaData.length);
                    tmp[tmp.length - 1] = unsignedBin[unsignedBin.length - 1];
                    metaData = tmp;
                }
            }

            if (!success) {
                break;
            }
        }

        FirmwareMeta meta = fromBinary(metaData);


    }

    public static void testSerialize() {

    }

    final static String testMeta =
            "4A 00 00 00 10 00 01 00 4A 00 48 00 00 00 00 00 \r\n" +
            "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 \r\n" +
            "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 \r\n" +
            "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 \r\n" +
            "00 00 00 00 00 00 00 00 50 0A 7B 22 4E 61 6D 65 \r\n" +
            "22 3A 20 22 44 61 74 61 45 78 63 68 61 6E 67 65 \r\n" +
            "72 41 54 4D 65 72 67 65 64 5F 54 49 45 4D 5F 56 \r\n" +
            "32 5F 4F 41 44 5F 52 33 34 38 2E 62 69 6E 22 2C \r\n" +
            "20 22 43 72 63 43 6F 64 65 22 3A 20 34 36 33 31 \r\n" +
            "36 7D FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF \r\n";


}
