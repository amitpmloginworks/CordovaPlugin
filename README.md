# DataExchanger Plugin for Apache Cordova

This plugin enables communication between a phone and DataExchanger (BLE) peripherals.

The plugin provides a simple [JavaScript API](#api) for iOS.

 * Scan and connect 
 * Send data
 * Send command
 * Receive data (through notification)
 * Receive command (through notification)

## Supported Platforms

* iOS

# Installing

### Cordova

    $ cordova plugin add cordova-plugin-dataexchanger

### PhoneGap

    $ phonegap plugin add cordova-plugin-dataexchanger

### PhoneGap Build

Edit config.xml to install the plugin for [PhoneGap Build](http://build.phonegap.com).

    <gap:plugin name="cordova-plugin-dataexchanger" source="npm" />


# API

## Methods

- [dx.isEnabled](#isenabled)
- [dx.isConnected](#isconnected)
- [dx.startScanAndConnect](#startscanandconnect)
- [dx.stopScan](#stopscan)
- [dx.sendData](#senddata)
- [dx.sendCmd](#sendcmd)
- [dx.startRxDataNotification](#startrxdatanotification)
- [dx.stopRxDataNotification](#stoprxdatanotification)
- [dx.startRxCmdNotification](#startrxcmdnotification)
- [dx.stopRxCmdNotification](#stoprxcmdnotification)
- [dx.startTxCreditNotification](#starttxcreditnotification)
- [dx.stopTxCreditNotification](#stoptxcreditnotification)


# License

Apache 2.0

# Feedback

Try the code. If you find an problem or missing feature, file an issue or create a pull request.

