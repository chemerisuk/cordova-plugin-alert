#import "AlertPlugin.h"

@implementation AlertPlugin

- (void)showDialog:(CDVInvokedUrlCommand *)command {
    NSDictionary* options = [command argumentAtIndex:0];
    NSString *title = options[@"title"] ?: @"";
    NSString *message = options[@"message"] ?: @"";
    NSArray* actions = options[@"actions"];
    NSArray* inputs = options[@"inputs"];
    int theme = [options[@"theme"] intValue];

    if (self.lastAlert) {
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Only single alert can be displayed"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

        return;
    }

    [self hideProgress];

    [self.commandDelegate runInBackground:^{
        self.lastAlert = [UIAlertController alertControllerWithTitle:title
                                                                   message:message
                                                            preferredStyle:UIAlertControllerStyleAlert];

        void (^actionHandler)() = ^(UIAlertAction *action) {
            NSMutableArray* result = [[NSMutableArray alloc] init];

            long actionIndex = [actions indexOfObject:action.title] + 1;
            [result addObject:[NSNumber numberWithLong:actionIndex]];

            for (int j = 0, n = (int)[inputs count]; j < n; ++j) {
                [result addObject:[[self.lastAlert.textFields objectAtIndex:j] text]];
            }

            self.lastAlert = nil;

            CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:result];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        };

        for (int i = (int)[actions count] - 1; i >= 0; --i) {
            [self.lastAlert addAction:[UIAlertAction actionWithTitle:[actions objectAtIndex:i]
                                                                     style:UIAlertActionStyleDefault
                                                                   handler:actionHandler
            ]];
        }

        if (inputs) {
            for (int j = 0, n = (int)[inputs count]; j < n; ++j) {
                NSDictionary *inputSettings = inputs[j];

                [self.lastAlert addTextFieldWithConfigurationHandler:^(UITextField *textField) {
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
                    textField.returnKeyType = UIReturnKeyNext;

                    if (theme > 0) {
                        textField.keyboardAppearance = UIKeyboardAppearanceDark;
                    }
                }];
            }
        }

        dispatch_async(dispatch_get_main_queue(), ^{
            [self.viewController presentViewController:self.lastAlert animated:YES completion:NULL];
        });
    }];
}

- (void)showSheet:(CDVInvokedUrlCommand *)command {
    NSDictionary* options = [command argumentAtIndex:0];
    NSString *title = options[@"title"] ?: @"";
    NSArray* actions = options[@"options"];

    if (self.lastAlert) {
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Only single alert can be displayed"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

        return;
    }

    [self hideProgress];

    [self.commandDelegate runInBackground:^{
        self.lastAlert = [UIAlertController alertControllerWithTitle:@""
                                                                   message:title
                                                            preferredStyle:UIAlertControllerStyleActionSheet];

        void (^actionHandler)() = ^(UIAlertAction *action) {
            NSMutableArray* result = [[NSMutableArray alloc] init];

            long actionIndex = [actions indexOfObject:action.title] + 1;
            [result addObject:[NSNumber numberWithLong:actionIndex]];
            [result addObject:action.title];

            self.lastAlert = nil;

            CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:result];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        };

        if (actions) {
            for (int i = 0, n = (int)[actions count]; i < n; ++i) {
                [self.lastAlert addAction:[UIAlertAction actionWithTitle:[actions objectAtIndex:i]
                                                                         style:UIAlertActionStyleDefault
                                                                       handler:actionHandler
                ]];
            }
        }

        dispatch_async(dispatch_get_main_queue(), ^{
            UIPopoverPresentationController *popover = self.lastAlert.popoverPresentationController;
            if (popover) {
                popover.permittedArrowDirections = 0;
                popover.sourceView = self.webView.superview;
                popover.sourceRect = CGRectMake(CGRectGetMidX(self.webView.bounds), CGRectGetMidY(self.webView.bounds), 0, 0);
            }

            [self.viewController presentViewController:self.lastAlert animated:YES completion:NULL];
        });
    }];
}

- (void)showProgress:(CDVInvokedUrlCommand *)command {
    NSDictionary* options = [command argumentAtIndex:0];
    NSString *title = options[@"title"] ?: @"";
    NSString *message = options[@"message"] ?: @"";

    if (self.lastAlert) {
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Only single alert can be displayed"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

        return;
    }

    [self hideProgress];

    [self.commandDelegate runInBackground:^{
        self.lastProgress = [UIAlertController alertControllerWithTitle:title
                                                                   message:[NSString stringWithFormat:@"%@\n\n\n\n", message]
                                                            preferredStyle:UIAlertControllerStyleAlert];

        dispatch_async(dispatch_get_main_queue(), ^{
            UIActivityIndicatorView* spinner = [[UIActivityIndicatorView alloc] initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleWhiteLarge];
            spinner.color = [UIColor blackColor];
            spinner.center = CGPointMake(self.lastProgress.view.bounds.size.width / 2,
                                         self.lastProgress.view.bounds.size.height / 1.4);
            spinner.autoresizingMask =
                UIViewAutoresizingFlexibleBottomMargin | UIViewAutoresizingFlexibleTopMargin |
                UIViewAutoresizingFlexibleLeftMargin | UIViewAutoresizingFlexibleRightMargin;
            [spinner startAnimating];

            [self.lastProgress.view addSubview:spinner];

            [self.viewController presentViewController:self.lastProgress animated:YES completion:^{
               [self.lastProgress.view.superview addGestureRecognizer:[[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(hideProgress)]];
            }];
        });
    }];
}

- (void)hideProgress {
    if (self.lastProgress) {
        [self.lastProgress dismissViewControllerAnimated:YES completion:nil];

        self.lastProgress = nil;
    }
}

- (void)hide:(CDVInvokedUrlCommand *)command {
    [self hideProgress];

    if (self.lastAlert) {
        [self.lastAlert dismissViewControllerAnimated:YES completion:^{
            self.lastAlert = nil;

            CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }];
    } else {
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
}

@end
