# Android Wear DataApi for Cordova

This plugin enables data synchronization between an Android Phone and an Android wear device via the [Android Wear DataApi](https://developers.google.com/android/reference/com/google/android/gms/wearable/DataApi). The plugin is only available for Android.

* Put data into the Data Layer
* Query data from the Data Layer
* Delete data from the Data Layer
* Register for Data Layer change event callbacks

*Note: Because Cordova does not run on Android Wear as of 3/12/2017, the Android Wear application must be written natively (or with Xamarin).*

## Platform

* Android

## DataApi Essentials

Each data item is identified by a URI, accessible with getUri(), that indicates the item's creator and path. Fully specified URIs follow the following format:

    wear://<node_id>/<path>

There is a lower-level API which works with sending raw bytes which is not implemented. The implementation of this plugin translates Javascript objects into [`DataMap`](https://developers.google.com/android/reference/com/google/android/gms/wearable/DataMap) objects in Android. Read more at the documentation here: [https://developer.android.com/training/wearables/data-layer/data-items.html](https://developer.android.com/training/wearables/data-layer/data-items.html)

# Installing

### Cordova

    $ cordova plugin add cordova-androidwear-dataapi --variable DATAAPI_PATH_FILTER="/"

The `DATAAPI_PATH_FILTER` is optional and defaults to "/". Keep in mind that if you do not add a filter prefix that differentiates the phone from the Wear device, all data events put to the Android Wear network by your application will be captured by `addListener`, including data items put by the Cordova phone app.

# API

## Methods

- [WearDataApi.putDataItem](#putDataItem)
- [WearDataApi.getDataItems](#getDataItems)
- [WearDataApi.deleteDataItems](#deleteDataItems)
- [WearDataApi.addListener](#addlistener)

## WearDataApi.putDataItem

    WearDataApi.putDataItem(path, data, success, failure);

### Description

Adds a DataItem to the Android Wear network. The updated item is synchronized across all devices.

*Note: calling this method multiple times with the same data will cause change events only once.*

### Parameters

- __path__: The path of the data item
- __data__: A javascript object. The object properties that can be translated from the object are:
    - Strings
    - Numbers
    - Boolean values
    - Nested javascript objects (following the above rules)
    - Arrays:
        - containing **only integers**, or
        - containing **only strings**, or
        - containing **only javascript objects** (following the above rules)
- __success__: Success callback function that is invoked when the put is complete.
- __failure__: Error callback function, invoked when error occurs. [optional]

### Example

    var data = { "a": 123, "b": { "c": ["d"] } }
    WearDataApi.putDataItem("/item_path/item123", data, function() {
        // success callback
    },
    function(err) {
        // error callback
    })

## WearDataApi.getDataItems

    getDataItems(uri, filterType, success, error) 

### Description

Retrieves all data items matching the provided URI, from the Android Wear network.

### Parameters

- __uri__: The URI must contain a path. If `uri` is fully specified, at most one data item will be returned. If `uri` contains a wildcard host, multiple data items may be returned, since different nodes may create data items with the same path. See DataApi for details of the URI format.
- __filterType__: Either `WearDataItem.FILTER_LITERAL` (0) or `WearDataApi.FILTER_PREFIX` (1). The filterType parameter changes the interpretation of `uri`. For example, if `uri` represents a path prefix, all items matching that prefix will be returned.
- __success__: Success callback function that is invoked when the get is complete, with the data returned.
- __failure__: Error callback function, invoked when error occurs. [optional]

### Example

    WearDataApi.getDataItems("wear://*/item_path", WearDataApi.FILTER_PREFIX, function(data) {
        console.log(data);
    })
    // outputs:
    [
        { 
            "Uri": "wear://abcd1234/item_path/item123",
            "Data": { "a": 123, "b": { "c": ["d"] } }
        }  
    ]

## WearDataApi.deleteDataItems

    deleteDataItems(uri, filterType, success, error)

### Description

Removes all specified data items from the Android Wear network.

### Parameters

- __uri__: If uri is fully specified, this method will delete at most one data item. If uri contains a wildcard host, multiple data items may be deleted, since different nodes may create data items with the same path
- __filterType__: Either `WearDataItem.FILTER_LITERAL` (0) or `WearDataApi.FILTER_PREFIX` (1). The filterType parameter changes the interpretation of `uri`. For example, if `uri` represents a path prefix, all items matching that prefix will be returned.
- __success__: Success callback function that is invoked when the delete is complete, with the number of items deleted.
- __failure__: Error callback function, invoked when error occurs. [optional]

### Example

    WearDataApi.deleteDataItems("wear://*/item_path", WearDataApi.FILTER_PREFIX, function(result) {
        console.log(result)
    });
    // outputs:
    { "NumDeleted": 1 }

## WearDataApi.addListener

    addListener(handler);

### Description

Registers a listener to receive data item changed and deleted events.

### Parameters

- __handler__: A javascript callback that is invoked when there is a data event. The callback value is an array of data events.

### Example

    WearDataApi.addListener(function(events) {
        for (event in events) {
            console.log("event for : " + event.Uri);
            console.log("data: " + event.Data)
            if (event.Type==WearDataApi.TYPE_CHANGED) {
                // handle change event
            }
            else if (event.Type==WearDataApi.TYPE_DELETED) {
                // handle delete event
            }
        }
    });

# Receiving Data from Android Wear

In this example, we add an WearableListenerService to the Wear app that will be created and invoked when a Data Item matches the given `host` and `pathPrefix` values:

### Add to AndroidManifest.xml

    <manifest>
        <application
            <service android:name=".MyService">
                <intent-filter>
                    <action android:name="com.google.android.gms.wearable.DATA_CHANGED"/>
                    <data android:scheme="wear" android:host="*" android:pathPrefix="/my_path"/>
                </intent-filter>
            </service>
        </application>
    </manifest>

### Send Data from Cordova:

    var data = {
        "id": 5, 
        "values": [1,2,3], 
        "nested": { "am_i_nested": true } 
    };
    WearDataApi.putDataItem("/my_path/test", data, <handlers>...);

### Receive Data in WearableListenerService (Java):

    public class MyService extends WearableListenerService {
        public MyService() { super("MyService"); } // constructor

        @Override
        public void onDataChanged(DataEventBuffer dataEventBuffer) {
            for (DataEvent event : dataEventBuffer) {
                int type = event.getType(); // TYPE_CHANGED (1)
                DataItem item = event.getDataItem();
                DataMap data = DataMapItem.fromDataItem(item).getDataMap();
                int id = data.getInt("id"); // 5
                ArrayList<Integer> values = data.getIntegerArrayList("values"); // [1,2,3]
                DataMap nested = data.getDataMap("nested");
                Boolean am_i_nested = nested.getBoolean("am_i_nested"); // true
            }
        }
    }

### Send Data back to Cordova from Android Wear native app (Java):

    private GoogleApiClient client; // note: need a connected client first

    public void SendDataBack() {
        // create base data map object
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/put_from_wear/1");
        DataMap dataMap = putDataMapRequest.getDataMap();
        dataMap.putBoolean("foobar", true);
        dataMap.putStringArray("keys", new String[] {"a", "b", "c"});
        DataMap d2 = new DataMap();
        d2.putString("mach", "facula");
        dataMap.putDataMap("bomb_baby", d2);
        Wearable.DataApi.putDataItem(client, putDataMapRequest.asPutDataRequest());
    }

### Received in Cordova:

    WearDataApi.addListener(function(events) { console.log(events) }); // register listener

    // outputs the object received from the callback:
    [
        {
            "foobar": true,
            "keys": ["a", "b", "c"],
            "bomb_baby": {
                "mach": "facula"
            }
        }
    ]


## License

Copyright (c) 2017 Marcus Lum

[Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0)