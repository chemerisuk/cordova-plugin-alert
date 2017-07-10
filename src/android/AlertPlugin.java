package by.chemerisuk.cordova;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class AlertPlugin extends CordovaPlugin {
    private static final String TAG = "AlertPlugin";

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if ("show".equals(action)) {
            show(args.getJSONObject(0), callbackContext);
        } else {
            return false;
        }

        return true;
    }

    private void show(JSONObject settings, final CallbackContext callbackContext) throws JSONException {
        AlertDialog.Builder dlg = new AlertDialog.Builder(this.webView.getContext());
        dlg.setMessage(settings.getString("message"));
        dlg.setTitle(settings.optString("title", ""));
        dlg.setCancelable(true);

        JSONArray actions = settings.getJSONArray("actions");
        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                callbackContext.success(-which);
            }
        };

        for (int i = 0, n = actions.length(); i < n; ++i) {
            String message = actions.getString(i);

            if (n - i == 1) {
                dlg.setPositiveButton(message, clickListener);
            } else if (n - i == 2) {
                dlg.setNegativeButton(message, clickListener);
            } else {
                dlg.setNeutralButton(message, clickListener);
            }
        }

        dlg.show();
    }
}