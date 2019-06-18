package by.chemerisuk.cordova;

import java.util.List;
import java.util.ArrayList;
import android.util.Log;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.text.InputType;
import android.text.InputFilter;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatRatingBar;
import androidx.appcompat.widget.LinearLayoutCompat;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class AlertPlugin extends CordovaPlugin {
    private static final String TAG = "AlertPlugin";

    private AlertDialog lastAlert;
    private ProgressDialog lastProgress;
    private int nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;

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
        } else if ("setNightMode".equals(action)) {
            setNightMode(args.getBoolean(0), callbackContext);
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

        if (lastAlert != null) {
            lastAlert.hide();
            lastAlert = null;
        }

        callbackContext.success();
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
                callbackContext.success(result);
            }
        });

        setupActions(dlg, settings, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                JSONArray result = new JSONArray();
                result.put(-which);
                callbackContext.success(result);
            }
        });

        final AlertDialog alertDialog = dlg.create();
        alertDialog.setCancelable(false);
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.getDelegate().setLocalNightMode(this.nightMode);
        alertDialog.show();

        return alertDialog;
    }

    private AlertDialog showDialog(JSONObject settings, final CallbackContext callbackContext) throws JSONException {
        hideProgress();

        AlertDialog.Builder dlg = createBuilder(settings);
        dlg.setMessage(settings.getString("message"));

        LinearLayoutCompat layout = null;
        final RatingBar ratingBar;

        if (settings.optBoolean("rating", false)) {
            layout = createLayout();
            ratingBar = createRatingBar();
            layout.addView(ratingBar);
        } else {
            ratingBar = null;
        }

        JSONArray inputs = settings.optJSONArray("inputs");
        final List<TextView> inputControls = new ArrayList<TextView>();
        if (inputs != null) {
            if (layout == null) {
                layout = createLayout();
            }

            for (int i = 0; i < inputs.length(); ++i) {
                final EditText input = createInput(inputs.getJSONObject(i));
                inputControls.add(input);
                layout.addView(input);
            }
        }

        if (layout != null) {
            dlg.setView(layout);
        }

        setupActions(dlg, settings, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                JSONArray result = new JSONArray();
                result.put(-which);

                if (ratingBar != null) {
                    result.put((int)ratingBar.getRating());
                }

                for (TextView textInput : inputControls) {
                    result.put(textInput.getText());
                }

                callbackContext.success(result);
            }
        });

        final AlertDialog alertDialog = dlg.create();
        alertDialog.setCancelable(false);
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.getDelegate().setLocalNightMode(this.nightMode);
        alertDialog.show();

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

        final ProgressDialog progressDlg = new ProgressDialog(cordova.getActivity());
        progressDlg.setTitle(settings.optString("title", ""));
        progressDlg.setMessage(settings.getString("message"));
        progressDlg.setCancelable(true);
        progressDlg.show();

        return progressDlg;
    }

    private void setNightMode(boolean enabled, final CallbackContext callbackContext) throws JSONException {
        if (enabled) {
            this.nightMode = AppCompatDelegate.MODE_NIGHT_YES;
        } else {
            this.nightMode = AppCompatDelegate.MODE_NIGHT_NO;
        }
        // clear cached? instance so the next alert will have the right colors
        final AlertDialog alertDialog = createBuilder(new JSONObject()).create();
        alertDialog.getDelegate().setLocalNightMode(this.nightMode);

        callbackContext.success();
    }

    private AlertDialog.Builder createBuilder(JSONObject settings) throws JSONException {
        AlertDialog.Builder builder = new AlertDialog.Builder(cordova.getActivity());
        builder.setTitle(settings.optString("title", ""));
        builder.setCancelable(true);

        return builder;
    }

    private LinearLayoutCompat createLayout() {
        LinearLayoutCompat layout = new LinearLayoutCompat(cordova.getActivity());
        layout.setOrientation(LinearLayoutCompat.VERTICAL);
        layout.setGravity(android.view.Gravity.CENTER);

        return layout;
    }

    private RatingBar createRatingBar() {
        RatingBar ratingBar = new AppCompatRatingBar(cordova.getActivity());
        ratingBar.setRating(0);
        ratingBar.setMax(5);
        ratingBar.setStepSize(1);
        ratingBar.setNumStars(5);
        ratingBar.setLayoutParams(new LinearLayoutCompat.LayoutParams(
            LinearLayoutCompat.LayoutParams.WRAP_CONTENT, LinearLayoutCompat.LayoutParams.WRAP_CONTENT));
        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                if (rating < 1) {
                    ratingBar.setRating(1);
                }
            }
        });

        return ratingBar;
    }

    private EditText createInput(JSONObject settings) throws JSONException {
        EditText input = new AppCompatEditText(cordova.getActivity());

        input.setText(settings.optString("value"), TextView.BufferType.NORMAL);
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

        String autocorrect = settings.optString("autocorrect", "");
        if ("off".equals(autocorrect)) {
            inputType |= InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
        }

        int maxLength = settings.optInt("maxlength");
        if (maxLength > 0) {
            input.setFilters(new InputFilter[] {new InputFilter.LengthFilter(maxLength)});
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
}
