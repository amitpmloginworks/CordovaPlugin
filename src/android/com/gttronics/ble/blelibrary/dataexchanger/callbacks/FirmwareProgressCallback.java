package com.gttronics.ble.blelibrary.dataexchanger.callbacks;

/**
 * Created by EGUSI16 on 12/6/2016.
 */

public interface FirmwareProgressCallback {
    void onProgress(int stage, double progress);
}
