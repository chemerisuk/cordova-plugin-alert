package by.chemerisuk.cordova;

import java.util.List;
import java.util.ArrayList;
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
import android.support.v7.widget.LinearLayoutCompat;

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

        final List<TextView> inputControls = new ArrayList<TextView>();
        JSONArray items = settings.optJSONArray("message");
        if (items != null) {
            final String[] itemsArray = new String[items.length()];
            for (int i = 0; i < items.length(); ++i) {
                itemsArray[i] = items.getString(i);
            }

            dlg.setItems(itemsArray, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    JSONArray result = new JSONArray();
                    result.put(which + 1);
                    result.put(itemsArray[which]);
                    sendResult(result, callbackContext);
                }
            });
        } else {
            dlg.setMessage(settings.getString("message"));

            JSONArray inputs = settings.optJSONArray("inputs");
            if (inputs != null) {
                LinearLayoutCompat layout = new LinearLayoutCompat(cordova.getActivity());
                layout.setOrientation(LinearLayoutCompat.VERTICAL);

                for (int i = 0; i < inputs.length(); ++i) {
                    EditText input = createInput(inputs.getJSONObject(i));
                    inputControls.add(input);
                    layout.addView(input);
                }

                dlg.setView(layout);
            }
        }

        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                JSONArray result = new JSONArray();
                result.put(-which);
                for (TextView textInput : inputControls) {
                    result.put(textInput.getText());
                }

                sendResult(result, callbackContext);
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
        alertDialog.setCanceledOnTouchOutside(false);

        if (inputControls.size() > 0) {
            final TextView messageView = (TextView)alertDialog.findViewById(android.R.id.message);
            // fix text colors for a non-default theme
            for (TextView textInput : inputControls) {
                textInput.setTextColor(messageView.getTextColors());
                textInput.setHintTextColor(messageView.getHintTextColors());
            }
            // set focus on the first input and show keyboard
            inputControls.get(0).setOnFocusChangeListener(new View.OnFocusChangeListener() {
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

    private void sendResult(final JSONArray result, final CallbackContext callbackContext) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                callbackContext.success(result);
            }
        });
    }
}