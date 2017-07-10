var exec = require("cordova/exec");
var PLUGIN_NAME = "Alert";
var noop = function() {};

function AlertBuilder(message, title) {
    this.message = message;
    this.title = title;
    this.actions = [];
}

AlertBuilder.prototype = {
    addAction: function(title) {
        this.actions.push(title);

        return this;
    },
    show: function(callback) {
        if (!this.actions.length) {
            this.actions.push("OK");
        }

        exec(callback, noop, PLUGIN_NAME, "show", [this]);

        return this;
    }
};

module.exports = {
    create: function(message, title) {
        return new AlertBuilder(message, title);
    }
};
