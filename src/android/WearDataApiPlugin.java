package us.m4rc.cordova.androidwear.dataapi;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultTransform;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.PutDataMapRequest;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import static com.google.android.gms.wearable.DataApi.FILTER_LITERAL;

public class WearDataApiPlugin extends CordovaPlugin
    implements DataApi.DataListener {

  public static WearDataApiPlugin WearDataApiPluginSingleton = null;

  private final String TAG = WearDataApiPlugin.class.getSimpleName();
  private final String[] ACTIONS = {
    "putDataItem",
    "getDataItems",
    "deleteDataItems",
    "addListener"
  };

  // keep the callbacks for all registered receivers
  private Queue<CallbackContext> registeredListeners = new LinkedList<CallbackContext>();

  // for queueing actions while api is starting up
  private Queue<ExecuteAction> queuedActions = new LinkedList<ExecuteAction>();


  private class ExecuteAction {
    public String action;
    public JSONArray args;
    public CallbackContext callbackContext;

    ExecuteAction(String action,JSONArray args,CallbackContext callbackContext) {
      this.action = action;
      this.args = args;
      this.callbackContext = callbackContext;
    }
  }

  private WearDataApiService api() {
    return WearDataApiService.WearDataApiServiceSingleton;
  }

  public void onServiceStarted() {
    // run all queued actions
    while (queuedActions.size() > 0) {
      ExecuteAction ea = queuedActions.remove();
      try {
        _execute(ea);
      }
      catch (Exception ex) {
        ea.callbackContext.error(ex.getMessage());
      }
    }
  }

  @Override
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);
    Log.d(TAG, "WearDataApiPlugin initialized.");

    Activity context = cordova.getActivity();
    WearDataApiPluginSingleton = this;
    context.startService(new Intent(context, WearDataApiService.class));
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    WearDataApiPluginSingleton = null; // if we are called gracefully
  }

  @Override
  protected void finalize() throws Throwable {
    super.finalize();
    WearDataApiPluginSingleton = null; // when we are garbage collected
  }

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
    // verify action is valid
    if (!Arrays.asList(ACTIONS).contains(action)) {
      return false;
    }
    ExecuteAction ea = new ExecuteAction(action,args,callbackContext);
    if (this.api()!=null) {
      try {
        _execute(ea); // run immediately if already connected
      }
      catch (Exception ex) {
        ea.callbackContext.error(ex.getMessage());
      }
    }
    else {
      queuedActions.add(ea); // otherwise queue
    }
    return true;
  }

  /**
   * maps Cordova exec commands to plugin functions
   */
  private void _execute(ExecuteAction ea) throws Exception {
    if (ea.action.equals("putDataItem")) {
      putDataItem(ea.args, ea.callbackContext);
    }
    if (ea.action.equals("getDataItems")) {
      cmdDataItems("get", ea.args, ea.callbackContext);
    }
    if (ea.action.equals("deleteDataItems")) {
      cmdDataItems("delete", ea.args, ea.callbackContext);
    }
    if (ea.action.equals("addListener")) {
      addListener(ea.args, ea.callbackContext);
    }
  }

  @Override
  public void onReset() {
    super.onReset();
    // window reloaded, clear listeners to make sure we don't leak them
    registeredListeners.clear();
  }

  private void putDataItem(JSONArray args, final CallbackContext callbackContext) throws JSONException {
    if (args.length() != 2) {
      throw new JSONException("putDataItem error: invalid arguments");
    }
    PutDataMapRequest putDataMapReq = PutDataMapRequest.create(args.getString(0));
    JsonConverter.jsonToDataMap((JSONObject)args.get(1), putDataMapReq.getDataMap());
    this.api().putDataRequest(putDataMapReq.asPutDataRequest())
      .then(new ResultTransform<DataApi.DataItemResult, Result>() {
        @Override
        public PendingResult<Result> onSuccess(@NonNull DataApi.DataItemResult dataItemResult) {
          callbackContext.success();
          return null;
        }
      });
  }

  private void cmdDataItems(String cmd, JSONArray args, final CallbackContext callbackContext) throws Exception {
    if (args.length() < 1) {
      throw new JSONException(cmd + "DataItems error: invalid arguments");
    }
    int filterType = FILTER_LITERAL;
    Uri uri = Uri.parse(args.getString(0));
    if (args.length()==2) {
      filterType = args.getInt(1);
    }
    if (cmd.equals("get")) {
      this.api().getDataItems(uri, filterType).then(new ResultTransform<DataItemBuffer, Result>() {
        @Override
        public PendingResult<Result> onSuccess(@NonNull DataItemBuffer dataItems) {
          callbackContext.success(JsonConverter.dataItemBufferToJson(dataItems));
          return null;
        }
      });
    } else if (cmd.equals("delete")) {
      this.api().deleteDataItems(uri, filterType).then(new ResultTransform<DataApi.DeleteDataItemsResult, Result>() {
        @Override
        public PendingResult<Result> onSuccess(@NonNull DataApi.DeleteDataItemsResult deleteResult) {
          JSONObject json = new JSONObject();
          try {
            json.put("NumDeleted", deleteResult.getNumDeleted());
          }
          catch (Exception ignored) {}
          callbackContext.success(json);
          return null;
        }
      });
    } else {
      callbackContext.error("Invalid verb: "+ cmd);
    }
  }

  private void addListener(JSONArray args, CallbackContext callbackContext) throws JSONException {
    if (args.length() != 0) {
      throw new JSONException("addListener error: invalid arguments");
    }
    registeredListeners.add(callbackContext);
  }

  public void onDataChanged(DataEventBuffer dataEventBuffer) {
    JSONArray results = JsonConverter.dataEventBufferToJson(dataEventBuffer);
    for (CallbackContext registeredDataReceiver: registeredListeners) {
      // don't just call success() because that will dispose the callback
      // so we have to return a PluginResult instead
      PluginResult result = new PluginResult(PluginResult.Status.OK, results);
      result.setKeepCallback(true);
      registeredDataReceiver.sendPluginResult(result);
    }
  }
}
