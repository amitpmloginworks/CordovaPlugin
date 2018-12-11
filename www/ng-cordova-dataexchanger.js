// install  :     cordova plugin add https://github.com/mingdee/cordova-plugin-dataexchanger.git
// link     :     https://github.com/mingdee/cordova-plugin-dataexchanger

angular.module('ngCordovaDataExchanger', []).factory('$cordovaDataExchanger', ['$q', '$timeout', function($q, $timeout){
  var errorUnsupported = {
    error: "unsupported",
    message: "Operation unsupported"
  };

  var init = function(params) {
    var q = $q.defer();
    if (window.dx === undefined) {
      q.reject(errorUnsupported);
    } else {
      window.dx.init(
        params.devCount,
		params.proximityPowerLevel,
		params.discoveryActiveTimeout,
		params.autoConnect,
		params.enableCommandChannel,
		params.enableChannelScrambler,
		params.enableTransmitBackpressure,
        params.serviceUUIDStrings,
        function(obj) {
          q.notify(obj);
        },
        function(obj) {
          q.reject(obj);
        }
      );
    }
    return q.promise;
  };

  var isEnabled = function() {
    var q = $q.defer();
    if (window.dx === undefined) {
      q.reject(errorUnsupported);
    } else {
      window.dx.isEnabled(
        function(obj) {
          q.notify(obj);
        },
        function(obj) {
          q.reject(obj);
        }
      );
    }
    return q.promise;
  };

  var isConnected = function(params) {
    var q = $q.defer();
    if (window.dx === undefined) {
      q.reject(errorUnsupported);
    } else {
      window.dx.isConnected(
        params.uuid,
        function(obj) {
          q.notify(obj);
        },
        function(obj) {
          q.reject(obj);
        }
      );
    }
    return q.promise;
  };

  var startScan = function() {
    var q = $q.defer();
    if (window.dx === undefined) {
      q.reject(errorUnsupported);
    } else {
      window.dx.startScan(
        function(obj) {
          q.notify(obj);
        },
        function(obj) {
          q.reject(obj);
        }
      );
    }
    return q.promise;
  };

  var stopScan = function() {
    var q = $q.defer();
    if (window.dx === undefined) {
      q.reject(errorUnsupported);
    } else {
      window.dx.stopScan(
        function(obj) {
          q.notify(obj);
        },
        function(obj) {
          q.reject(obj);
        }
      );
    }
    return q.promise;
  };
  
  var connect = function(params) {
    var q = $q.defer();
    if (window.dx === undefined) {
      q.reject(errorUnsupported);
    } else {
      window.dx.connect(
        params.uuid,
		function(obj) {
          q.notify(obj);
        },
        function(obj) {
          q.reject(obj);
        }
      );
    }
    return q.promise;
  };
  
  var disconnect = function(params) {
    var q = $q.defer();
    if (window.dx === undefined) {
      q.reject(errorUnsupported);
    } else {
      window.dx.disconnect(
        params.uuid,
		function(obj) {
          q.notify(obj);
        },
        function(obj) {
          q.reject(obj);
        }
      );
    }
    return q.promise;
  };
  
  var sendData = function(params) {
    var q = $q.defer();
    if (window.dx === undefined) {
      q.reject(errorUnsupported);
    } else {
      window.dx.sendData(
        params.uuid,
		params.data,
        function(obj) {
          q.notify(obj);
        },
        function(obj) {
          q.reject(obj);
        }
      );
    }
    return q.promise;
  };
  
  var sendCmd = function(params) {
    var q = $q.defer();
    if (window.dx === undefined) {
      q.reject(errorUnsupported);
    } else {
      window.dx.sendCmd(
        params.uuid,
		params.cmd,
        function(obj) {
          q.notify(obj);
        },
        function(obj) {
          q.reject(obj);
        }
      );
    }
    return q.promise;
  };
  
  var readTxCredit = function(params) {
    var q = $q.defer();
    if (window.dx === undefined) {
      q.reject(errorUnsupported);
    } else {
      window.dx.readTxCredit(
        params.uuid,
        function(obj) {
          q.notify(obj);
        },
        function(obj) {
          q.reject(obj);
        }
      );
    }
    return q.promise;
  };
  
  var enableRxDataNotification = function(params) {
    var q = $q.defer();
    if (window.dx === undefined) {
      q.reject(errorUnsupported);
    } else {
      window.dx.enableRxDataNotification(
        params.uuid,
        function(obj) {
          //console.log ('NG RxData: ' +obj);
          q.notify(obj);
        },
        function(obj) {
          q.reject(obj);
        }
      );
    }
    return q.promise;
  };
  
  var disableRxDataNotification = function(params) {
    var q = $q.defer();
    if (window.dx === undefined) {
      q.reject(errorUnsupported);
    } else {
      window.dx.disableRxDataNotification(
        params.uuid,
        function(obj) {
          q.notify(obj);
        },
        function(obj) {
          q.reject(obj);
        }
      );
    }
    return q.promise;
  };
  
  var enableRxCmdNotification = function(params) {
    var q = $q.defer();
    if (window.dx === undefined) {
      q.reject(errorUnsupported);
    } else {
      window.dx.enableRxCmdNotification(
        params.uuid,
        function(obj) {
          //console.log ('NG RxCmd: ' +obj);
          q.notify(obj);
        },
        function(obj) {
          q.reject(obj);
        }
      );
    }
    return q.promise;
  };
  
  var disableRxCmdNotification = function(params) {
    var q = $q.defer();
    if (window.dx === undefined) {
      q.reject(errorUnsupported);
    } else {
      window.dx.disableRxCmdNotification(
        params.uuid,
        function(obj) {
          q.notify(obj);
        },
        function(obj) {
          q.reject(obj);
        }
      );
    }
    return q.promise;
  };
  
  var enableTxCreditNotification = function(params) {
    var q = $q.defer();
    if (window.dx === undefined) {
      q.reject(errorUnsupported);
    } else {
      window.dx.enableTxCreditNotification(
        params.uuid,
        function(obj) {
          q.notify(obj);
        },
        function(obj) {
          q.reject(obj);
        }
      );
    }
    return q.promise;
  };
  
  var disableTxCreditNotification = function(params) {
    var q = $q.defer();
    if (window.dx === undefined) {
      q.reject(errorUnsupported);
    } else {
      window.dx.disableTxCreditNotification(
        params.uuid,
        function(obj) {
          q.notify(obj);
        },
        function(obj) {
          q.reject(obj);
        }
      );
    }
    return q.promise;
  };
  
  var retrieveFirmwareMeta = function(params) {
    var q = $q.defer();
    if (window.dx === undefined) {
      q.reject(errorUnsupported);
    } else {
      window.dx.retrieveFirmwareMeta(
        params.uuid,
        function(obj) {
          q.notify(obj);
        },
        function(obj) {
          q.reject(obj);
        }
      );
    }
    return q.promise;
  };
  
  var primeFirmwareBinary = function(params) {
    var q = $q.defer();
    if (window.dx === undefined) {
      q.reject(errorUnsupported);
    } else {
      window.dx.primeFirmwareBinary(
        params.uuid,
        params.firmBin,
        params.firmName,
        params.interleaveCommand,
        params.interleaveCount,
        function(obj) {
          q.notify(obj);
        },
        function(obj) {
          q.reject(obj);
        }
      );
    }
    return q.promise;
  };
  
  var switchFirmwareToSlot = function(params) {
    var q = $q.defer();
    if (window.dx === undefined) {
      q.reject(errorUnsupported);
    } else {
      window.dx.switchFirmwareToSlot(
        params.uuid,
        params.slotIdx,
        params.keepConfig,
        function(obj) {
          q.notify(obj);
        },
        function(obj) {
          q.reject(obj);
        }
      );
    }
    return q.promise;
  };
  
  var encodedStringToBytes = function(value) {
    if (window.DataExchanger === undefined) {
      return;
    }
    return window.DataExchanger.encodedStringToBytes(value);
  };

  var bytesToEncodedString = function(value) {
    if (window.DataExchanger === undefined) {
      return;
    }
    return window.DataExchanger.bytesToEncodedString(value);
  };

  var stringToBytes = function(value) {
    if (window.DataExchanger === undefined) {
      return;
    }
    return window.DataExchanger.stringToBytes(value);
  };

  var bytesToString = function(value) {
    if (window.DataExchanger === undefined) {
      return;
    }
    return window.DataExchanger.bytesToString(value);
  };

  var bytesToHex = function(value) {
    if (window.DataExchanger === undefined) {
      return;
    }
    return window.DataExchanger.bytesToHex(value);
  };

  var createTimeout = function(params, q) {
    if (params.timeout) {
      return $timeout(function() {
        params.error = "timeout";
        q.reject(params);
      }, params.timeout);
    }
    return null;
  };

  return {
    encodedStringToBytes: encodedStringToBytes,
    bytesToEncodedString: bytesToEncodedString,
    stringToBytes: stringToBytes,
    bytesToString: bytesToString,
    bytesToHex: bytesToHex,
	  
	init: init,
	isEnabled: isEnabled,
	isConnected: isConnected,
	startScan: startScan,
	stopScan: stopScan,
	connect: connect,
	disconnect: disconnect,
	sendData: sendData,
	sendCmd: sendCmd,
	readTxCredit: readTxCredit,
	enableRxDataNotification: enableRxDataNotification,
	disableRxDataNotification: disableRxDataNotification,
	enableRxCmdNotification: enableRxCmdNotification,
	disableRxCmdNotification: disableRxCmdNotification,
	enableTxCreditNotification: enableTxCreditNotification,
	disableTxCreditNotification: disableTxCreditNotification,
	retrieveFirmwareMeta: retrieveFirmwareMeta, 
	primeFirmwareBinary: primeFirmwareBinary, 
	switchFirmwareToSlot: switchFirmwareToSlot 
  };
}]);
