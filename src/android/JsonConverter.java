package us.m4rc.cordova.androidwear.dataapi;

//import android.util.Log;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Converts from Json to and from object model
 */

class JsonConverter {
  //private final static String TAG = JsonConverter.class.getSimpleName();

  static JSONArray dataItemBufferToJson(DataItemBuffer dataItemBuffer) {
    JSONArray results = new JSONArray();
    for (DataItem dataItem: dataItemBuffer) {
      results.put(dataItemToJson(dataItem));
    }
    return results;
  }

  static JSONArray dataEventBufferToJson(DataEventBuffer dataEventBuffer) {
    JSONArray results = new JSONArray();
    for (DataEvent dataEvent: dataEventBuffer) {
      JSONObject json = dataItemToJson(dataEvent.getDataItem());
      try {
        // add the event type to the return value
        json.put("Type", dataEvent.getType());
      }
      catch (Exception ex) {}
      results.put(json);
    }
    return results;
  }

  private static JSONObject dataItemToJson(DataItem dataItem) {
    DataMap dataMap = DataMapItem.fromDataItem(dataItem).getDataMap();
    JSONObject json = null;
    try {
      json = new JSONObject();
      json.put("Uri", dataItem.getUri().toString());
      json.put("Data", JsonConverter.dataMapToJson(dataMap));
    } catch (Exception ex) {
      try {
        json = new JSONObject();
        json.put("error", "Invalid DataMapItem received: " + ex.getMessage());
      } catch (Exception innerEx) {
        // at least we tried.
      }
    }
    return json;
  }

  static JSONObject dataMapToJson(DataMap dataMap) throws JSONException {
    JSONObject obj = new JSONObject();
    for (String key: dataMap.keySet()) {
      Object val = dataMap.get(key);
      if (val instanceof Integer ||
          val instanceof Long ||
          val instanceof Boolean ||
          val instanceof Double ||
          val instanceof String) {
        obj.put(key, val); // handle basic types
      }
      else if (val instanceof long[]) {
        JSONArray arr = new JSONArray();
        long[] valArr = (long[])val;
        for (int j=0; j<valArr.length; j++) {
          arr.put(valArr[j]);
        }
        obj.put(key,arr); // long[]
      }
      else if (val instanceof String[]) {
        JSONArray arr = new JSONArray();
        String[] valArr = (String[])val;
        for (int j=0; j<valArr.length; j++) {
          arr.put(valArr[j]);
        }
        obj.put(key,arr); // String[]
      }
      else if (val instanceof DataMap) {
        obj.put(key, dataMapToJson((DataMap)val));
      }
      else if (val instanceof ArrayList) {
        JSONArray arr = new JSONArray();
        ArrayList valArr = (ArrayList)val;
        for (int j=0; j<valArr.size(); j++) {
          Object valArrVal = valArr.get(j);
          if (valArrVal instanceof Integer || valArrVal instanceof String) {
            arr.put(valArrVal);
          }
          else if (valArrVal instanceof DataMap) {
            arr.put(dataMapToJson((DataMap)valArrVal));
          }
          obj.put(key,arr);
        }
      }
    }
    return obj;
  }

  static DataMap jsonToDataMap(JSONObject object, DataMap dataMap) throws JSONException {
    JSONArray names = object.names();
    for (int i=0; i<names.length(); i++) {
      String key = names.getString(i);
      Object val = object.get(key);
      //Log.d(TAG, key + "(" + val.getClass().toString() + "): " + val.toString());
      if (val instanceof String) {
        dataMap.putString(key, (String)val);
      }
      else if (val instanceof Integer) {
        dataMap.putInt(key, (Integer)val);
      }
      else if (val instanceof Double) {
        dataMap.putDouble(key, (Double)val);
      }
      else if (val instanceof Boolean) {
        dataMap.putBoolean(key, (Boolean)val);
      }
      else if (val instanceof JSONObject) {
        dataMap.putDataMap(key, jsonToDataMap((JSONObject)val, new DataMap()));
      }
      else if (val instanceof JSONArray) {
        JSONArray arr = (JSONArray)val;
        if (arr.length()==0) {
          dataMap.putIntegerArrayList(key, new ArrayList<Integer>()); // empty array
        }
        else {
          if (arr.get(0) instanceof Integer) {
            ArrayList<Integer> ints = new ArrayList<Integer>();
            for (int j=0; j<arr.length(); j++) {
              ints.add(arr.getInt(j));
            }
            dataMap.putIntegerArrayList(key, ints);
          }
          else if (arr.get(0) instanceof String) {
            ArrayList<String> strings = new ArrayList<String>();
            for (int j=0; j<arr.length(); j++) {
              strings.add(arr.getString(j));
            }
            dataMap.putStringArrayList(key, strings);
          }
          else if (arr.get(0) instanceof JSONObject) {
            ArrayList<DataMap> dataMaps = new ArrayList<DataMap>();
            for (int j=0; j<arr.length(); j++) {
              dataMaps.add(jsonToDataMap(arr.getJSONObject(j), new DataMap()));
            }
            dataMap.putDataMapArrayList(key, dataMaps);
          }
        }
      }
    }
    return dataMap;
  }
}
