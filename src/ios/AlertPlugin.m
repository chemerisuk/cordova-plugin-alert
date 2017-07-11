#import "AlertPlugin.h"

@implementation AlertPlugin

- (void)showDialog:(CDVInvokedUrlCommand *)command {
    NSDictionary* options = [command argumentAtIndex:0];
    NSString *title = options[@"title"] ?: nil;
    NSString *message = options[@"message"] ?: nil;
    NSArray* actions = options[@"actions"];
    NSArray* inputs = options[@"inputs"];

    UIAlertController *alertController = [UIAlertController
        alertControllerWithTitle:title message:message preferredStyle:UIAlertControllerStyleAlert];

    void (^actionHandler)() = ^(UIAlertAction *action) {
        NSMutableArray* result = [[NSMutableArray alloc] init];

        long actionIndex = [actions indexOfObject:action.title] + 1;
        [result addObject:[NSNumber numberWithLong:actionIndex]];

        for (int j = 0, n = (int)[inputs count]; j < n; ++j) {
            [result addObject:[[alertController.textFields objectAtIndex:j] text]];
        }

        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:result];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    };

    for (int i = (int)[actions count] - 1; i >= 0; --i) {
        [alertController addAction:[UIAlertAction actionWithTitle:[actions objectAtIndex:i]
                                                            style:UIAlertActionStyleDefault
                                                          handler:actionHandler
        ]];
    }

    if (inputs) {
        for (int j = 0, n = (int)[inputs count]; j < n; ++j) {
            NSDictionary *inputSettings = inputs[j];

            [alertController addTextFieldWithConfigurationHandler:^(UITextField *textField) {
                textField.placeholder = inputSettings[@"placeholder"];
                textField.keyboardType = [inputSettings[@"type"] intValue];
            }];
        }
    }

    [self.viewController presentViewController:alertController animated:YES completion:NULL];
}

@end
