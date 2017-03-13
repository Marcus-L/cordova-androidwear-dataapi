package us.m4rc.cordova.androidwear.dataapi;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

public class WearDataApiService extends Service
  implements DataApi.DataListener,
             GoogleApiClient.ConnectionCallbacks,
             GoogleApiClient.OnConnectionFailedListener {

  private final String TAG = WearDataApiService.class.getSimpleName();
  private final IBinder mBinder = new LocalBinder();
  private GoogleApiClient mGoogleApiClient;
  private DataApi.DataListener DataListener = null;

  @Override
  public void onConnected(@Nullable Bundle bundle) {
    Log.d(TAG, "Google API Client connected");
    Wearable.DataApi.addListener(mGoogleApiClient, this);
  }

  @Override
  public void onConnectionSuspended(int i) {
  }

  @Override
  public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    Log.d(TAG, "Google API Client connection failed, WearDataApiService is unusable.");
  }

  public class LocalBinder extends Binder {
    WearDataApiService getService() {
      return WearDataApiService.this;
    }
  }

  @Override
  public void onCreate() {
    super.onCreate();

    // set up the Google API client
    if (mGoogleApiClient==null) {
      mGoogleApiClient = new GoogleApiClient.Builder(this)
        .addApi(Wearable.API)
        .addConnectionCallbacks(this)
        .build();
    }
    if (!mGoogleApiClient.isConnected()) {
      mGoogleApiClient.connect();
    }
    Log.d(TAG, "WearDataApiService started.");
  }

  @Override
  public void onDestroy() {

    // clean up Google API client
    if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
      Wearable.DataApi.removeListener(mGoogleApiClient, this);
      mGoogleApiClient.disconnect();
    }
    super.onDestroy();
  }

  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }

  /**
   * Registers the plugin with this service, for receiving forwarded data events
   */
  public void setDataEventHandler(DataApi.DataListener callback) {
    DataListener = callback;
  }
  /**
   * Forwards data changed to the plugin for callback to Cordova
   */
  public void onDataChanged(DataEventBuffer dataEventBuffer) {
    if (DataListener != null) {
      DataListener.onDataChanged(dataEventBuffer);
    }
  }

  // methods below form a thin wrapper around the DataApi for use in the Plugin

  public PendingResult<DataApi.DataItemResult> putDataRequest(PutDataRequest data) {
    return Wearable.DataApi.putDataItem(mGoogleApiClient, data);
  }

  public PendingResult<DataApi.DeleteDataItemsResult> deleteDataItems(Uri uri, int filterType) {
    return Wearable.DataApi.deleteDataItems(mGoogleApiClient, uri, filterType);
  }

  public PendingResult<DataItemBuffer> getDataItems(Uri uri, int filterType) {
    return Wearable.DataApi.getDataItems(mGoogleApiClient, uri, filterType);
  }
}
