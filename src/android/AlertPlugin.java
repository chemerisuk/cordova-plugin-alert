package by.chemerisuk.cordova;

import java.util.Arrays;
import android.util.Log;

import android.app.Activity;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.EditText;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;

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
        AlertDialog.Builder dlg;
        int dlgTheme = settings.optInt("theme", 0);

        if (dlgTheme > 0) {
            dlg = new AlertDialog.Builder(cordova.getActivity(), dlgTheme);
        } else {
            dlg = new AlertDialog.Builder(cordova.getActivity());
        }

        dlg.setMessage(settings.getString("message"));
        dlg.setTitle(settings.optString("title", ""));
        dlg.setCancelable(true);

        EditText textView = null;
        JSONArray inputs = settings.optJSONArray("inputs");
        if (inputs != null) {
            JSONObject inputSettings = inputs.getJSONObject(0);
            textView = new AppCompatEditText(cordova.getActivity());
            textView.setInputType(inputSettings.getInt("type"));
            textView.setHint(inputSettings.optString("hint"));

            dlg.setView(textView);
        }

        final EditText textViewFinal = textView;
        final DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (textViewFinal == null) {
                    callbackContext.success(-which);
                } else {
                    callbackContext.success(new JSONArray(
                        Arrays.asList(-which, textViewFinal.getText())));
                }
            }
        };

        JSONArray actions = settings.getJSONArray("actions");
        for (int i = 0, n = actions.length(); i < n; ++i) {
            String title = actions.getString(i);

            if (i == 0) {
                dlg.setPositiveButton(title, clickListener);
            } else if (i == 1) {
                dlg.setNegativeButton(title, clickListener);
            } else {
                dlg.setNeutralButton(title, clickListener);
            }
        }

        final AlertDialog alertDialog = dlg.show();

        if (textView != null) {
            textView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View viev, boolean hasFocus) {
                    if (hasFocus) {
                        alertDialog.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                    }
                }
            });
        }
    }
}