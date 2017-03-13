var _isString = function isString(x) {
    return Object.prototype.toString.call(x) === '[object String]';
};
// Helper method to execute Cordova native method
var _execNative = function (method, args, success, error) {
    // provide default callbacks in case they were not provided
    var optSuccess = function(result) {
        if (success !== undefined) {
            success(result);
        }
    }
    var optFail = function(err) {
        var err;
        if (typeof err === 'undefined') {
            err = new Error('Error occured while executing native method.')
        }
        else {
            err = _isString(err) ? new Error(err) : err
        }
        if (error === undefined) {
            console.error(err);
        }
        else {
            error(err);
        }
    };
    cordova.exec(optSuccess, optFail, 'WearDataApi', method, args);
};

module.exports = {

    // for documentation on how these functions are intended to function see:
    // https://developers.google.com/android/reference/com/google/android/gms/wearable/DataApi.html

    FILTER_LITERAL: 0,
    FILTER_PREFIX: 1,
    TYPE_CHANGED: 1,
    TYPE_DELETED: 2,
 
    addListener: function(handler) {
        if (typeof handler !== "function") {
            throw "handler must be a function";
        }
        _execNative("addListener", [], handler, function(err) { console.error(err); });
    },

    putDataItem: function(path, data, success, error) {
        _execNative("putDataItem", [path, data], success, error);
    },

    getDataItems: function(uri, filterType, success, error) {
        if (filterType==undefined) {
            filterType = FILTER_LITERAL;
        }
        _execNative("getDataItems", [uri, filterType], success, error);
    },

    deleteDataItems: function(uri, filterType, success, error) {
        if (filterType==undefined) {
            filterType = FILTER_LITERAL;
        }
        _execNative("deleteDataItems", [uri, filterType], success, error);
    }
}