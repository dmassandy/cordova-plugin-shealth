package com.wopo.plugin;

import org.apache.cordova.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import android.app.Activity;
import android.util.Log;

import com.samsung.android.sdk.healthdata.HealthConstants;

public class SHealth extends CordovaPlugin {

    String APP_TAG = "CordovaSHealthPlugin";

    Activity activity = null;
    SHealthConnector connector = null;

    /** The function detects the called function
     *
     * @param action          The action to execute.
     * @param data            Function parameter as JSON array.
     * @param callbackContext The callback context used when calling back into JavaScript.
     * @return
     * @throws JSONException
     */
    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {

        if( activity == null) {
            Log.d(APP_TAG, "activity == null");
            activity = this.cordova.getActivity();
        }

        if( connector == null) {
            Log.d(APP_TAG, "connector == null");
            connector = new SHealthConnector(activity, callbackContext);
        }

        if (action.equals("greet")) {

            String name = data.getString(0);

            Log.d(APP_TAG, "Hello, " + name + " this is a cordova shealth plugin!");
            String message = "{\"TYPE\":\"MESSAGE\",\"MESSAGE\":\"Hello, " + name + " this is a cordova shealth plugin!\"}";

            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, message);
            // pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);

            return true;

        } else if (action.equals("connectToSHealth")) {
            Log.d(APP_TAG, "connectToSHealth");

            connector.connect();

            return true;

        } else if (action.equals("callHealthPermissionManager")) {
            Log.d(APP_TAG, "callHealthPermissionManager");

            connector.setCallbackContext(callbackContext);
            connector.callHealthPermissionManager();

            return true;

        } else if (action.equals("getSleepData")) {
            Log.d(APP_TAG, "getSleepData");

            JSONArray params = data.getJSONArray(0);
            Log.d(APP_TAG, "StartTime: " + params.getLong(0) + " - EndTime: " + params.getLong(1));

            connector.setCallbackContext(callbackContext);
            connector.startReporter(HealthConstants.Sleep.HEALTH_DATA_TYPE, params.getLong(0), params.getLong(1));

            return true;

        } else if (action.equals("getStepCountData")) {
            Log.d(APP_TAG, "getStepCountData");

            JSONArray params = data.getJSONArray(0);
            Log.d(APP_TAG, "StartTime: " + params.getLong(0) + " - EndTime: " + params.getLong(1));

            connector.setCallbackContext(callbackContext);
            connector.startReporter(HealthConstants.StepCount.HEALTH_DATA_TYPE, params.getLong(0), params.getLong(1));

            return true;
        } else if (action.equals("getStepCountTrendData")) {
            Log.d(APP_TAG, "getStepCountTrendData");

            JSONArray params = data.getJSONArray(0);
            Log.d(APP_TAG, "StartTime: " + params.getLong(0) + " - EndTime: " + params.getLong(1));

            connector.setCallbackContext(callbackContext);
            connector.startReporter("com.samsung.shealth.step_daily_trend", params.getLong(0), params.getLong(1));

            return true;
        } else if (action.equals("startObserver")) {
            Log.d(APP_TAG, "startObserver");

            JSONArray params = data.getJSONArray(0);
            connector.setObserverCallbackContext(callbackContext);
            for (int i = 0; i < params.length(); i++) {
                connector.startObserver(params.getString(i));
            }
            return true;
        } else if (action.equals("checkPermission")) {
            Log.d(APP_TAG, "checkPermission");

            connector.setCallbackContext(callbackContext);
            connector.checkPermissionHasBeenAcquired();

            return true;
        } else if (action.equals("stopObserver")) {
            Log.d(APP_TAG, "stopObserver");

            connector.setCallbackContext(callbackContext);
            connector.stopObserver();

            return true;
        } else if (action.equals("disconnect")) {
            Log.d(APP_TAG, "disconnect");

            connector.setCallbackContext(callbackContext);
            connector.disconnectService();

            return true;
        } else {
            PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, "{\"TYPE\":\"ERROR\",\"MESSAGE\":\"Action not found.\"}");
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
            return false;

        }
    }
}