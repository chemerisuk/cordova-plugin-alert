#import "AlertPlugin.h"

@implementation AlertPlugin

- (void)show:(CDVInvokedUrlCommand *)command {
    NSDictionary* options = [command argumentAtIndex:0];
    NSString *title = options[@"title"] ?: nil;
    NSString *message = options[@"message"] ?: nil;
    NSArray* actions = options[@"actions"];

    UIAlertController *alertController = [UIAlertController
        alertControllerWithTitle:title message:message preferredStyle:UIAlertControllerStyleAlert];

    void (^actionHandler)() = ^(UIAlertAction *action) {
        int actionIndex = [actions indexOfObject:action.title] + 1;
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsInt:actionIndex];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    };

    for (int i = 0, n = (int)[actions count]; i < n; ++i) {
        [alertController addAction:[UIAlertAction actionWithTitle:[actions objectAtIndex:i]
                                                            style:UIAlertActionStyleDefault
                                                          handler:actionHandler
        ]];
    }

    [self.viewController presentViewController:alertController animated:YES completion:NULL];
}

@end