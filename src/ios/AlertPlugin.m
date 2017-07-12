#import "AlertPlugin.h"

@implementation AlertPlugin

- (void)showDialog:(CDVInvokedUrlCommand *)command {
    NSDictionary* options = [command argumentAtIndex:0];
    NSString *title = options[@"title"];
    NSString *message = options[@"message"];
    NSArray* actions = options[@"actions"];
    NSArray* inputs = options[@"inputs"];

    [self.commandDelegate runInBackground:^{
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
                    NSString *autocapitalize = inputSettings[@"autocapitalize"] ?: @"";
                    if ([autocapitalize isEqualToString:@"words"]) {
                        textField.autocapitalizationType = UITextAutocapitalizationTypeWords;
                    } else if ([autocapitalize isEqualToString:@"characters"]) {
                        textField.autocapitalizationType = UITextAutocapitalizationTypeAllCharacters;
                    } else if ([autocapitalize isEqualToString:@"sentences"]) {
                        textField.autocapitalizationType = UITextAutocapitalizationTypeSentences;
                    }

                    textField.placeholder = inputSettings[@"placeholder"];
                    textField.keyboardType = [inputSettings[@"type"] intValue];
                }];
            }
        }

        dispatch_async(dispatch_get_main_queue(), ^{
            [self.viewController presentViewController:alertController animated:YES completion:NULL];
        });
    }];
}

- (void)showSheet:(CDVInvokedUrlCommand *)command {
    NSDictionary* options = [command argumentAtIndex:0];
    NSString *title = options[@"title"];
    NSArray* actions = options[@"options"];

    [self.commandDelegate runInBackground:^{
        UIAlertController *alertController = [UIAlertController
            alertControllerWithTitle:@"" message:title preferredStyle:UIAlertControllerStyleActionSheet];

        void (^actionHandler)() = ^(UIAlertAction *action) {
            NSMutableArray* result = [[NSMutableArray alloc] init];

            long actionIndex = [actions indexOfObject:action.title] + 1;
            [result addObject:[NSNumber numberWithLong:actionIndex]];
            [result addObject:action.title];

            CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:result];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        };

        if (actions) {
            for (int i = 0, n = (int)[actions count]; i < n; ++i) {
                [alertController addAction:[UIAlertAction actionWithTitle:[actions objectAtIndex:i]
                                                                    style:UIAlertActionStyleDefault
                                                                  handler:actionHandler
                ]];
            }
        }

        dispatch_async(dispatch_get_main_queue(), ^{
            UIPopoverPresentationController *popover = alertController.popoverPresentationController;
            if (popover) {
                popover.permittedArrowDirections = UIPopoverArrowDirectionUnknown;
                popover.sourceView = self.webView.superview;
                popover.sourceRect = CGRectMake(CGRectGetMidX(self.webView.bounds), CGRectGetMidY(self.webView.bounds), 0, 0);

            }

            [self.viewController presentViewController:alertController animated:YES completion:NULL];
        });
    }];
}

@end
