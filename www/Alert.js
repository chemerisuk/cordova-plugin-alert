var exec = require("cordova/exec");
var PLUGIN_NAME = "Alert";
var isIOS = cordova.platformId === "ios";

// https://developer.android.com/reference/android/text/InputType.html
// https://developer.apple.com/library/ios/documentation/UIKit/Reference/UITextInputTraits_Protocol/#//apple_ref/c/tdef/UIKeyboardType

function AlertBuilder(title, type) {
    this.title = title;
    this.actions = [];
    this.type = type;
}

AlertBuilder.TYPE_SHEET = "sheet";
AlertBuilder.TYPE_DIALOG = "dialog";
AlertBuilder.TYPE_PROGRESS = "progress";

AlertBuilder.prototype = {
    addAction: function(title) {
        if (arguments.length > 1) {
            title = Array.prototype.slice.call(arguments, 0);
        }

        if (Array.isArray(title)) {
            title.push.apply(this.actions, title);
        } else if (title) {
            this.actions.push(title);
        }

        return this;
    },
    addInput: function(type, config) {
        if (!this.inputs) {
            this.inputs = [];
        }
        // make a copy
        config = config ? JSON.parse(JSON.stringify(config)) : {};
        config.type = type;

        this.inputs.push(config);

        return this;
    },
    addTextInput: function(config) {
        var inputType = 1;

        if (isIOS) {
            inputType = 0; // UIKeyboardTypeDefault
        } else if (config && config.multiline) {
            inputType = 16385 | 131072;
        }

        return this.addInput(inputType, config);
    },
    addNumberInput: function(config) {
        var inputType = 2;

        if (isIOS) {
            // UIKeyboardTypeNumbersAndPunctuation / UIKeyboardTypeNumberPad
            inputType = config && config.decimal ? 2 : 4;
        } else if (config && config.decimal) {
            inputType |= 4096 | 8192;
        }

        return this.addInput(inputType, config);
    },
    addPhoneInput: function(config) {
        var inputType = 3;

        if (isIOS) {
            inputType = 5; // UIKeyboardTypePhonePad
        }

        return this.addInput(inputType, config);
    },
    addRatingBar: function() {
        this.rating = true;

        return this;
    },
    show: function(success, error) {
        var methodName = "show" + this.type[0].toUpperCase() + this.type.slice(1);

        exec(function(args) {
            if (typeof success === "function") {
                if (Array.isArray(args)) {
                    success.apply(null, args);
                } else {
                    success(args);
                }
            }
        }, error, PLUGIN_NAME, methodName, [this]);

        return this;
    },
    hide: function(success, error) {
        exec(success, error, PLUGIN_NAME, "hide", []);

        return this;
    }
};

module.exports = {
    createDialog: function(message, title) {
        var builder = new AlertBuilder(title, AlertBuilder.TYPE_DIALOG);
        builder.message = message;
        return builder;
    },
    createSheet: function(options, title) {
        var builder = new AlertBuilder(title, AlertBuilder.TYPE_SHEET);
        builder.options = options;
        return builder;
    },
    createProgress: function(message, title) {
        var builder = new AlertBuilder(title, AlertBuilder.TYPE_PROGRESS);
        builder.message = message;
        return builder;
    },
    setNightMode: function(value, success, error) {
        exec(success, error, PLUGIN_NAME, "setNightMode", [!!value]);
    }
};
