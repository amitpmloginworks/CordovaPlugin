package com.gttronics.ble.blelibrary.dataexchanger.callbacks;

import java.util.Map;

/**
 * Created by EGUSI16 on 12/6/2016.
 */

public interface FirmwareMetaRetrievedCallback {
    void onMeteRetrieved(int status, Map<String, Object> meta, String msg);
}
