package by.chemerisuk.cordova;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import android.util.Log;

import android.app.Activity;
import android.content.DialogInterface;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.EditText;
import android.widget.TextView;
import android.os.Build;
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

        dlg.setCancelable(true);
        dlg.setTitle(settings.optString("title", ""));

        final EditText textView;
        JSONArray items = settings.optJSONArray("message");
        if (items != null) {
            textView = null;

            final String[] itemsArray = new String[items.length()];
            for (int i = 0; i < items.length(); ++i) {
                itemsArray[i] = items.getString(i);
            }

            dlg.setItems(itemsArray, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    callbackContext.success(new JSONArray(
                        Arrays.asList(which + 1, itemsArray[which])));
                }
            });
        } else {
            dlg.setMessage(settings.getString("message"));

            JSONArray inputs = settings.optJSONArray("inputs");
            if (inputs == null) {
                textView = null;
            } else {
                // handle only first input for now
                textView = createInput(inputs.getJSONObject(0));

                dlg.setView(textView);
            }
        }

        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (textView == null) {
                    callbackContext.success(-which);
                } else {
                    callbackContext.success(new JSONArray(
                        Arrays.asList(-which, textView.getText().toString())));
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
        final TextView messageView = (TextView)alertDialog.findViewById(android.R.id.message);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            messageView.setTextDirection(View.TEXT_DIRECTION_LOCALE);
        }

        alertDialog.setCanceledOnTouchOutside(false);

        if (textView != null) {
            // fix text color for a dark theme
            textView.setTextColor(messageView.getTextColors());
            // set focus on the first input and show keyboard
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

    private EditText createInput(JSONObject settings) throws JSONException {
        EditText input = new AppCompatEditText(cordova.getActivity());
        input.setInputType(settings.getInt("type"));
        input.setHint(settings.optString("hint"));

        return input;
    }
}