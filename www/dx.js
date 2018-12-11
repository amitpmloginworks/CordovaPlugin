//cordova.define("cordova-plugin-dataexchanger.Dx", function(require, exports, module) {
// (c) 2016 GT-tronics Canada Ltd
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/* global cordova, module */
               
"use strict";

var stringToArrayBuffer = function(str) 
{
    var ret = new Uint8Array(str.length);
    for (var i = 0; i < str.length; i++) 
	{
        ret[i] = str.charCodeAt(i);
    }
    // TODO would it be better to return Uint8Array?
    return ret.buffer;
};

var base64ToArrayBuffer = function(b64)
{
    return stringToArrayBuffer(atob(b64));
};

function massageMessageNativeToJs(message) 
{
    if (message.CDVType == 'ArrayBuffer') 
	{
        message = base64ToArrayBuffer(message.data);
    }
    return message;
}

// Cordova 3.6 doesn't unwrap ArrayBuffers in nested data structures
// https://github.com/apache/cordova-js/blob/94291706945c42fd47fa632ed30f5eb811080e95/src/ios/exec.js#L107-L122
function convertToNativeJS(object) 
{
    Object.keys(object).forEach(function (key) 
	{
        var value = object[key];
        object[key] = massageMessageNativeToJs(value);
        if (typeof(value) === 'object') 
		{
            convertToNativeJS(value);
        }
    });
}

module.exports =
{
    init: function(devCount, proximityPowerLevel, discoveryActiveTimeout, autoConnect, enableCommandChannel, enableChannelScrambler, enableTransmitBackpressure, serviceUUIDStrings, success, failure)
    {
        cordova.exec(success, failure, 'DxPlugin', 'init', [devCount, proximityPowerLevel, discoveryActiveTimeout, autoConnect, enableCommandChannel, enableChannelScrambler, enableTransmitBackpressure, serviceUUIDStrings]);
    },
               
    startScan: function (success, failure)
    {
        cordova.exec(success, failure, 'DxPlugin', 'startScan', []);
    },

    stopScan: function (success, failure)
    {
        cordova.exec(success, failure, 'DxPlugin', 'stopScan', []);
    },

    connect: function (devUUIDStr, success, failure)
    {
        cordova.exec(success, failure, 'DxPlugin', 'connect', [devUUIDStr]);
    },

    disconnect: function (devUUIDStr, success, failure)
    {
        cordova.exec(success, failure, 'DxPlugin', 'disconnect', [devUUIDStr]);
    },

    // characteristic value comes back as ArrayBuffer in the success callback
    sendData: function (devUUIDStr, data, success, failure)
    {
        cordova.exec(success, failure, 'DxPlugin', 'sendData', [devUUIDStr, data]);
    },

    // RSSI value comes back as an integer
    sendCmd: function(devUUIDStr, data, success, failure)
    {
        cordova.exec(success, failure, 'DxPlugin', 'sendCmd', [devUUIDStr, data]);
    },

    // value must be an ArrayBuffer
    readTxCredit: function (devUUIDStr, success, failure)
    {
        cordova.exec(success, failure, 'DxPlugin', 'readTxCredit', [devUUIDStr]);
    },

    // value must be an ArrayBuffer
    writeTxCreditReportLoopCount: function (devUUIDStr, count, success, failure)
    {
        cordova.exec(success, failure, 'DxPlugin', 'writeTxCreditReportLoopCount', [devUUIDStr, count]);
    },

    isConnected: function (devUUIDStr, success, failure)
    {
        cordova.exec(success, failure, 'DxPlugin', 'isConnected', [devUUIDStr]);
    },

    isEnabled: function (success, failure)
    {
        cordova.exec(success, failure, 'DxPlugin', 'isEnabled', []);
    },
               
    enableRxDataNotification: function (devUUIDStr, success, failure)
    {
        cordova.exec(success, failure, 'DxPlugin', 'enableRxDataNotification', [devUUIDStr]);
    },

    disableRxDataNotification: function (devUUIDStr, success, failure)
    {
        cordova.exec(success, failure, 'DxPlugin', 'disableRxDataNotification', [devUUIDStr]);
    },

    enableRxCmdNotification: function (devUUIDStr, success, failure)
    {
        cordova.exec(success, failure, 'DxPlugin', 'enableRxCmdNotification', [devUUIDStr]);
    },
               
    disableRxCmdNotification: function (devUUIDStr, success, failure)
    {
        cordova.exec(success, failure, 'DxPlugin', 'disableRxCmdNotification', [devUUIDStr]);
    },

    enableTxCreditNotification: function (devUUIDStr, success, failure)
    {
        cordova.exec(success, failure, 'DxPlugin', 'enableTxCreditNotification', [devUUIDStr]);
    },
               
    disableTxCreditNotification: function (devUUIDStr, success, failure)
    {
        cordova.exec(success, failure, 'DxPlugin', 'disableTxCreditNotification', [devUUIDStr]);
    },

    retrieveFirmwareMeta: function (devUUIDStr, success, failure)
    {
        cordova.exec(success, failure, 'DxPlugin', 'retrieveFirmwareMeta', [devUUIDStr]);
    },

    primeFirmwareBinary: function (devUUIDStr, firmBin, firmName, interleaveCommand, interleaveCount, success, failure)
    {
        cordova.exec(success, failure, 'DxPlugin', 'primeFirmwareBinary', [devUUIDStr, firmBin, firmName, interleaveCommand, interleaveCount]);
    },

    switchFirmwareToSlot: function (devUUIDStr, slotIdx, keepConfig, success, failure)
    {
        cordova.exec(success, failure, 'DxPlugin', 'switchFirmwareToSlot', [devUUIDStr, slotIdx, keepConfig]);
    },
};

//});
