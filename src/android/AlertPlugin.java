package by.chemerisuk.cordova;

import java.util.List;
import java.util.ArrayList;
import android.util.Log;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.text.InputType;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.EditText;
import android.widget.TextView;
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

    private AlertDialog lastAlert;
    private ProgressDialog lastProgress;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if ("hide".equals(action)) {
            hide(callbackContext);
        } else if (lastAlert != null && lastAlert.isShowing()) {
            callbackContext.error("Only single alert can be displayed");
        } else if ("showDialog".equals(action)) {
            lastAlert = showDialog(args.getJSONObject(0), callbackContext);
        } else if ("showSheet".equals(action)) {
            lastAlert = showSheet(args.getJSONObject(0), callbackContext);
        } else if ("showProgress".equals(action)) {
            lastProgress = showProgress(args.getJSONObject(0), callbackContext);
        } else {
            return false;
        }

        return true;
    }

    private void hideProgress() {
        if (lastProgress != null) {
            lastProgress.hide();
            lastProgress = null;
        }
    }

    private void hide(final CallbackContext callbackContext) {
        hideProgress();

        if (lastAlert == null) return;

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (lastAlert != null) {
                    lastAlert.hide();
                    lastAlert = null;
                }

                callbackContext.success();
            }
        });
    }

    private AlertDialog showSheet(JSONObject settings, final CallbackContext callbackContext) throws JSONException {
        hideProgress();

        AlertDialog.Builder dlg = createBuilder(settings);

        JSONArray options = settings.getJSONArray("options");
        final String[] optionsArray = new String[options.length()];
        for (int i = 0; i < options.length(); ++i) {
            optionsArray[i] = options.getString(i);
        }

        dlg.setItems(optionsArray, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                JSONArray result = new JSONArray();
                result.put(which + 1);
                result.put(optionsArray[which]);
                sendResult(result, callbackContext);
            }
        });

        setupActions(dlg, settings, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                JSONArray result = new JSONArray();
                result.put(-which);
                sendResult(result, callbackContext);
            }
        });

        final AlertDialog alertDialog = dlg.show();
        alertDialog.setCanceledOnTouchOutside(false);

        return alertDialog;
    }

    private AlertDialog showDialog(JSONObject settings, final CallbackContext callbackContext) throws JSONException {
        hideProgress();

        AlertDialog.Builder dlg = createBuilder(settings);
        dlg.setMessage(settings.getString("message"));

        JSONArray inputs = settings.optJSONArray("inputs");
        final List<TextView> inputControls = new ArrayList<TextView>();
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

        setupActions(dlg, settings, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                JSONArray result = new JSONArray();
                result.put(-which);
                for (TextView textInput : inputControls) {
                    result.put(textInput.getText());
                }

                sendResult(result, callbackContext);
            }
        });

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

        return alertDialog;
    }

    private ProgressDialog showProgress(JSONObject settings, CallbackContext callbackContext) throws JSONException {
        hideProgress();

        final ProgressDialog progressDlg;
        int dlgTheme = settings.optInt("theme", 0);

        if (dlgTheme > 0) {
            progressDlg = new ProgressDialog(cordova.getActivity(), dlgTheme);
        } else {
            progressDlg = new ProgressDialog(cordova.getActivity());
        }

        progressDlg.setTitle(settings.optString("title", ""));
        progressDlg.setMessage(settings.getString("message"));
        progressDlg.setCancelable(true);
        progressDlg.show();

        return progressDlg;
    }

    private AlertDialog.Builder createBuilder(JSONObject settings) throws JSONException {
        AlertDialog.Builder builder;
        int dlgTheme = settings.optInt("theme", 0);

        if (dlgTheme > 0) {
            builder = new AlertDialog.Builder(cordova.getActivity(), dlgTheme);
        } else {
            builder = new AlertDialog.Builder(cordova.getActivity());
        }

        builder.setTitle(settings.optString("title", ""));
        builder.setCancelable(true);

        return builder;
    }

    private EditText createInput(JSONObject settings) throws JSONException {
        EditText input = new AppCompatEditText(cordova.getActivity());

        input.setHint(settings.optString("placeholder"));

        int inputType = settings.getInt("type");
        String autocapitalize = settings.optString("autocapitalize", "");
        if ("words".equals(autocapitalize)) {
            inputType |= InputType.TYPE_TEXT_FLAG_CAP_WORDS;
        } else if ("characters".equals(autocapitalize)) {
            inputType |= InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS;
        } else if ("sentences".equals(autocapitalize)) {
            inputType |= InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
        }

        input.setInputType(inputType);

        return input;
    }

    private void setupActions(AlertDialog.Builder dlg, JSONObject settings, DialogInterface.OnClickListener clickListener) throws JSONException {
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