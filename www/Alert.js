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

AlertBuilder.prototype = {
    addAction: function(title) {
        if (Array.isArray(title)) {
            title.push.apply(this.actions, title);
        } else {
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
            inputType = 0;
        } else if (config && config.multiline) {
            inputType = 16385 | 131072;
        }

        return this.addInput(inputType, config);
    },
    addNumberInput: function(config) {
        var inputType = 2;

        if (isIOS) {
            inputType = 4;
        } else if (config && config.decimal) {
            inputType |= 4096 | 8192;
        }

        return this.addInput(inputType, config);
    },
    addPhoneInput: function(config) {
        return this.addInput(isIOS ? 5 : 3, config);
    },
    show: function(success, error) {
        if (!this.actions.length) {
            this.actions.push("OK");
        }

        exec(function(args) {
            if (Array.isArray(args)) {
                success.apply(this, args);
            } else {
                success(args);
            }
        }, error, PLUGIN_NAME, "show", [this]);

        return this;
    }
};

module.exports = {
    create: function(message, title, theme) {
        return new AlertBuilder(message, title, theme);
    }
};
