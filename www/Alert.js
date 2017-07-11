var exec = require("cordova/exec");
var PLUGIN_NAME = "Alert";
var isIOS = cordova.platformId === "ios";

// https://developer.android.com/reference/android/text/InputType.html
// https://developer.apple.com/library/ios/documentation/UIKit/Reference/UITextInputTraits_Protocol/#//apple_ref/c/tdef/UIKeyboardType

function AlertBuilder(message, title, theme) {
    this.message = message;
    this.title = title;
    this.theme = theme;
    this.actions = [];
}

AlertBuilder.getCurrentTheme = function() { return 0; };

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
            inputType = 4; // UIKeyboardTypeNumberPad
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
    show: function(success, error) {
        exec(function(args) {
            if (typeof success === "function") {
                if (Array.isArray(args)) {
                    success.apply(null, args);
                } else {
                    success(args);
                }
            }
        }, error, PLUGIN_NAME, "show", [this]);

        return this;
    }
};

module.exports = {
    create: function(message, title) {
        return new AlertBuilder(message, title, AlertBuilder.getCurrentTheme());
    },
    setTheme: function(theme) {
        if (typeof theme === "function") {
            AlertBuilder.getCurrentTheme = theme;
        } else if (typeof theme === "number") {
            AlertBuilder.getCurrentTheme = function() { return theme; };
        } else {
            throw new TypeError("theme can be only number or a functor");
        }
    }
};
