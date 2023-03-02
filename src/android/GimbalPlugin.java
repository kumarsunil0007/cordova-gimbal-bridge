/* Copyright Airship and Contributors */

package com.urbanairship.cordova.gimbal;

import android.Manifest;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.gimbal.airship.AirshipAdapter;
import com.urbanairship.Autopilot;
import com.urbanairship.cordova.PluginLogger;
import com.urbanairship.gimbal.GimbalAdapter;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class GimbalPlugin extends CordovaPlugin {

    static final String TAG = "UAGimbalPlugin";

    private static final String START_COMMAND = "start";
    private static final String STOP_COMMAND = "stop";
    private static final String START_CUSTOM_COMMAND = "startCustom";
    private static final String STOP_CUSTOM_COMMAND = "stopCustom";

    private static final String PERMISSION_DENIED_ERROR = "permission denied";

    private boolean isStarted = false;
    private boolean isCustomStarted = false;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        Log.i(TAG, "Initializing Urban Airship Gimbal cordova plugin.");

        GimbalAdapter.shared(cordova.getActivity()).enableGimbalApiKeyManagement(getGimbalKey());



        // Asking permissions
//      if (ActivityCompat.checkSelfPermission(cordova.getActivity(),
//        android.Manifest.permission.ACCESS_FINE_LOCATION)
//        != PackageManager.PERMISSION_GRANTED) {
//        // Should we show an explanation?
//        if (ActivityCompat.shouldShowRequestPermissionRationale(cordova.getActivity(),
//          android.Manifest.permission.ACCESS_FINE_LOCATION)) {
//          // Show an explanation to the user *asynchronously* -- don't block
//          // this thread waiting for the user's response! After the user
//          // sees the explanation, try again to request the permission.
//        } else {
//          // No explanation needed, we can request the permission.
//          ActivityCompat.requestPermissions(cordova.getActivity(),
//            new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
//          // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
//          // app-defined int constant. The callback method gets the
//          // result of the request.
//        }
//      }

        if (GimbalPluginConfig.getInstance(cordova.getActivity()).getAutoStart()) {
            Log.i(TAG, "Auto starting Gimbal Adapter.");
            if(cordova.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION))
            {
              start(null);
            }
            else
            {
              getReadPermission(1);
            }
        }
    }
    @Override
    public boolean execute(String action, CordovaArgs data, CallbackContext callbackContext) throws JSONException {
        // Start
        if (START_COMMAND.equalsIgnoreCase(action)) {
            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    start(callbackContext);
                }
            });
            return true;
        }

        // Stop
        if (STOP_COMMAND.equalsIgnoreCase(action)) {
            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    stop(callbackContext);
                }
            });
            return true;
        }

        // Start Custom Events
        if (START_CUSTOM_COMMAND.equalsIgnoreCase(action)) {
            JSONObject options = data.getJSONObject(0);
            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    startCustom(options, callbackContext);
                }
            });
            return true;
        }

         // Stop Custom Events
        if (STOP_CUSTOM_COMMAND.equalsIgnoreCase(action)) {
            JSONObject options = data.getJSONObject(0);
            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    stopCustom(options, callbackContext);
                }
            });
            return true;
        }

        return false;
    }


    protected void getReadPermission(int requestCode)
    {
      cordova.requestPermission(this, requestCode, Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private String getGimbalKey() {
        return GimbalPluginConfig.getInstance(cordova.getActivity()).getGimbalKey();
    }

    private void stop(CallbackContext callbackContext) {
        isStarted = false;
        GimbalAdapter.shared(cordova.getActivity()).stop();
        callbackContext.success();
    }

    private void start(CallbackContext callbackContext) {
        isStarted = true;


        // com.urbanairship.cordova.CordovaAutopilot.automaticTakeOff(cordova.getContext());
        GimbalAdapter.shared(cordova.getActivity()).startWithPermissionPrompt(new GimbalAdapter.PermissionResultCallback() {
            @Override
            public void onResult(boolean enabled) {
                PluginLogger.debug("Gimbal Plugin attempted to start with result: %s", enabled);
                isStarted = enabled;
            }
        });

        if (callbackContext == null) {
            return;
        }

        if (isStarted) {
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
        } else {
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, PERMISSION_DENIED_ERROR));
        }
    }


    private void stopCustom(JSONObject data, CallbackContext callbackContext) {
      isCustomStarted = false;
      if (data.length() > 0) {
        try {
          if (data.getBoolean("entry")) {
            AirshipAdapter.shared(cordova.getContext()).setShouldTrackCustomEntryEvent(false);
          }
        } catch (JSONException e) {
          isCustomStarted = false;
          Log.i(TAG, "Start Custom entry event " + e.getMessage());
        }

        try {
          if (data.getBoolean("entry")) {
            AirshipAdapter.shared(cordova.getContext()).setShouldTrackCustomExitEvent(false);
          }
        } catch (JSONException e) {
          isCustomStarted = false;
          Log.i(TAG, "Stop Custom exit event" + e.getMessage());
        }
      }else{
        AirshipAdapter.shared(cordova.getContext()).setShouldTrackCustomEntryEvent(false);
        AirshipAdapter.shared(cordova.getContext()).setShouldTrackCustomExitEvent(false);
      }
      callbackContext.success();
    }

    private void startCustom(JSONObject data, CallbackContext callbackContext) {
        isCustomStarted = true;
        boolean argsAvailable = false;
        String msg = "";
        AirshipAdapter.shared(cordova.getContext()).startWithPermissionPrompt(getGimbalKey());
        if(data.length() > 0) {
          try {
            if (data.getBoolean("entry")) {
              argsAvailable = true;
              AirshipAdapter.shared(cordova.getContext()).setShouldTrackCustomEntryEvent(true);
            }
          } catch (JSONException e) {
            isCustomStarted = false;
            Log.i(TAG, "Start Custom Events for entry " + e.getMessage());
            msg = e.getMessage();
          }


          try {
            if (data.getBoolean("entry")) {
              argsAvailable = true;
              AirshipAdapter.shared(cordova.getContext()).setShouldTrackCustomExitEvent(true);
            }
          } catch (JSONException e) {
            isCustomStarted = false;
            Log.i(TAG, "Start Custom Events for exit " + e.getMessage());
            msg = e.getMessage();
          }
        }else{
            isCustomStarted = true;
            AirshipAdapter.shared(cordova.getContext()).setShouldTrackCustomEntryEvent(true);
            AirshipAdapter.shared(cordova.getContext()).setShouldTrackCustomExitEvent(true);
        }

        if (callbackContext == null) {
            return;
        }

        if (isCustomStarted) {
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
        } else {
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, msg));
        }
    }


}
