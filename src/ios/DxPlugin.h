//
//  DxPlugin.h
//  DataExchanger Cordova Plugin
//
//  (c) 2016 GT-tronics Canada Ltd 
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

#ifndef __DX_PLUGIN_H__
#define __DX_PLUGIN_H__

#import <Cordova/CDV.h>

@interface DxPlugin : CDVPlugin
{
}

- (void) suspendCallbacks;
- (void) resumeCallbacks;

- (void) init:(CDVInvokedUrlCommand *)command;
- (void)isEnabled:(CDVInvokedUrlCommand *)command;

- (void)startScan:(CDVInvokedUrlCommand *)command;
- (void)stopScan:(CDVInvokedUrlCommand *)command;

- (void)connect:(CDVInvokedUrlCommand *)command;
- (void)disconnect:(CDVInvokedUrlCommand *)command;

- (void)sendData:(CDVInvokedUrlCommand *)command;
- (void)sendCmd:(CDVInvokedUrlCommand *)command;

- (void)isConnected:(CDVInvokedUrlCommand *)command;

- (void)readTxCredit:(CDVInvokedUrlCommand *)command;
- (void)writeTxCreditReportLoopCount:(CDVInvokedUrlCommand *)command;

- (void)enableRxDataNotification:(CDVInvokedUrlCommand *)command;
- (void)disableRxDataNotification:(CDVInvokedUrlCommand *)command;

- (void)enableRxCmdNotification:(CDVInvokedUrlCommand *)command;
- (void)disableRxCmdNotification:(CDVInvokedUrlCommand *)command;

- (void)enableTxCreditNotification:(CDVInvokedUrlCommand *)command;
- (void)disableTxCreditNotification:(CDVInvokedUrlCommand *)command;

- (void) retrieveFirmwareMeta:(CDVInvokedUrlCommand *)command;
- (void) primeFirmwareBinary:(CDVInvokedUrlCommand *)command;
- (void) switchFirmwareToSlot:(CDVInvokedUrlCommand*)command;

@end

#endif
