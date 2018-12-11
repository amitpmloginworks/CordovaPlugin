package com.gttronics.ble.blelibrary.dataexchanger.callbacks;

import java.util.Map;

/**
 * Created by EGUSI16 on 12/7/2016.
 */

public interface FirmwareWriteCompletedCallback {
    void onWriteCompleted(int status, Map<String, Object> metas, String msg);
}
