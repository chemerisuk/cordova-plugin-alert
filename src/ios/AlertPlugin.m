#import "AlertPlugin.h"
#import "HCSStarRatingView.h"

@implementation AlertPlugin

- (void)pluginInitialize {
    self.keyboardStyle = UIKeyboardAppearanceDefault;
}

- (void)showDialog:(CDVInvokedUrlCommand *)command {
    NSDictionary* options = [command argumentAtIndex:0];
    NSString *title = options[@"title"] ?: @"";
    NSString *message = options[@"message"] ?: @"";
    NSArray* actions = options[@"actions"];
    NSArray* inputs = options[@"inputs"];
    HCSStarRatingView *starRatingView = NULL;

    if (options[@"rating"]) {
        starRatingView = [[HCSStarRatingView alloc] initWithFrame:CGRectMake(35, 75, 200, 50)];
        // add extra padding for rating bar
        message = [NSString stringWithFormat:@"%@\n\n\n\n", message];
    }

    if (self.lastAlert) {
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Only single alert can be displayed"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

        return;
    }

    [self hideProgress:^{
        self.lastAlert = [UIAlertController alertControllerWithTitle:title
                                                                   message:message
                                                            preferredStyle:UIAlertControllerStyleAlert];

        if (starRatingView) {
            starRatingView.backgroundColor = [UIColor clearColor];
            starRatingView.spacing = 15;
            starRatingView.maximumValue = 5;
            starRatingView.minimumValue = 0;
            starRatingView.value = 0;

            [self.lastAlert.view addSubview:starRatingView];
        }

        void (^actionHandler)(UIAlertAction*) = ^(UIAlertAction *action) {
            NSMutableArray* result = [[NSMutableArray alloc] init];

            long actionIndex = [actions indexOfObject:action.title] + 1;
            [result addObject:[NSNumber numberWithLong:actionIndex]];

            if (starRatingView) {
                [result addObject:[NSNumber numberWithFloat:starRatingView.value]];
            }

            for (int j = 0, n = (int)[inputs count]; j < n; ++j) {
                [result addObject:[[self.lastAlert.textFields objectAtIndex:j] text]];
            }

            self.lastAlert = NULL;

            CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:result];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        };

        for (int i = 0, n = (int)[actions count]; i < n; ++i) {
            UIAlertActionStyle actionStyle = i == 1 ? UIAlertActionStyleCancel : UIAlertActionStyleDefault;
            UIAlertAction *action = [UIAlertAction actionWithTitle:[actions objectAtIndex:i]
                                                             style:actionStyle
                                                           handler:actionHandler
                                    ];

            [self.lastAlert addAction:action];
            if (i == 0 && n > 1) {
                // set preferredAction only for iOS9+
                if ([self.lastAlert respondsToSelector:@selector(setPreferredAction:)]) {
                    self.lastAlert.preferredAction = action;
                }
            }
        }

        if (inputs) {
            for (int j = 0, n = (int)[inputs count]; j < n; ++j) {
                NSDictionary *inputSettings = inputs[j];
                UIKeyboardAppearance keyboardStyle = self.keyboardStyle;

                [self.lastAlert addTextFieldWithConfigurationHandler:^(UITextField *textField) {
                    NSString *autocapitalize = inputSettings[@"autocapitalize"] ?: @"";
                    if ([autocapitalize isEqualToString:@"words"]) {
                        textField.autocapitalizationType = UITextAutocapitalizationTypeWords;
                    } else if ([autocapitalize isEqualToString:@"characters"]) {
                        textField.autocapitalizationType = UITextAutocapitalizationTypeAllCharacters;
                    } else if ([autocapitalize isEqualToString:@"sentences"]) {
                        textField.autocapitalizationType = UITextAutocapitalizationTypeSentences;
                    }

                    NSString *autocorrect = inputSettings[@"autocorrect"] ?: @"";
                    if ([autocorrect isEqualToString:@"off"]) {
                        textField.autocorrectionType = UITextAutocorrectionTypeNo;
                        textField.spellCheckingType = UITextSpellCheckingTypeNo;
                    } else if ([autocorrect isEqualToString:@"on"]) {
                        textField.autocorrectionType = UITextAutocorrectionTypeYes;
                        textField.spellCheckingType = UITextSpellCheckingTypeYes;
                    }

                    NSString* autofill = inputSettings[@"autofill"] ?: @"";
                    if ([autofill isEqualToString:@"one-time-code"]) {
                        if (@available(iOS 12.0, *)) {
                            textField.textContentType = UITextContentTypeOneTimeCode;
                        }
                    }

                    textField.text = inputSettings[@"value"];
                    textField.placeholder = inputSettings[@"placeholder"];
                    textField.keyboardType = [inputSettings[@"type"] intValue];
                    textField.returnKeyType = j < n - 1 ? UIReturnKeyNext : UIReturnKeyDone;
                    textField.keyboardAppearance = keyboardStyle;
                }];
            }
        }

        [self.getTopPresentedViewController presentViewController:self.lastAlert animated:YES completion:NULL];
    }];
}

- (void)showSheet:(CDVInvokedUrlCommand *)command {
    NSDictionary* options = [command argumentAtIndex:0];
    NSString *title = options[@"title"] ?: @"";
    NSArray* items = options[@"options"];
    NSArray* actions = options[@"actions"];

    if ([title length] == 0) {
        title = NULL; // fix sheet presentation with blank message
    }

    if (self.lastAlert) {
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Only single alert can be displayed"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

        return;
    }

    [self hideProgress:^{
        self.lastAlert = [UIAlertController alertControllerWithTitle:NULL
                                                             message:title
                                                      preferredStyle:UIAlertControllerStyleActionSheet];

        void (^actionHandler)(UIAlertAction*) = ^(UIAlertAction *action) {
            NSMutableArray* result = [[NSMutableArray alloc] init];

            if (action.style == UIAlertActionStyleCancel) {
                [result addObject:[NSNumber numberWithLong:1]];
            } else {
                long actionIndex = [items indexOfObject:action.title] + 1;
                [result addObject:[NSNumber numberWithLong:actionIndex]];
                [result addObject:action.title];
            }

            self.lastAlert = NULL;

            CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:result];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        };

        if (items) {
            for (int i = 0, n = (int)[items count]; i < n; ++i) {
                [self.lastAlert addAction:[UIAlertAction actionWithTitle:[items objectAtIndex:i]
                                                                   style:UIAlertActionStyleDefault
                                                                 handler:actionHandler
                ]];
            }
        }

        NSString* cancelActionTitle;
        UIPopoverPresentationController *popover = self.lastAlert.popoverPresentationController;

        if (actions && [actions count] > 0) {
            // only single cancel button available on iOS
            cancelActionTitle = [actions objectAtIndex:0];
        } else if (popover) {
            // for iPad add fake cancel button (that is invisible)
            // to handle tap outside and reset internal state
            cancelActionTitle = @"Cancel";
        }

        if (cancelActionTitle) {
            [self.lastAlert addAction:[UIAlertAction actionWithTitle:cancelActionTitle
                                                               style:UIAlertActionStyleCancel
                                                             handler:actionHandler
            ]];
        }

        if (popover) {
            popover.permittedArrowDirections = 0;
            popover.sourceView = self.webView.superview;
            popover.sourceRect = CGRectMake(CGRectGetMidX(self.webView.bounds), CGRectGetMidY(self.webView.bounds), 0, 0);
        }

        [self.getTopPresentedViewController presentViewController:self.lastAlert animated:YES completion:NULL];
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

    [self hideProgress:^{
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

            [self.getTopPresentedViewController presentViewController:self.lastProgress animated:YES completion:^{
               [self.lastProgress.view.superview addGestureRecognizer:[[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(hideProgressOnTap)]];
            }];
        });
    }];
}

- (void)hideProgressOnTap {
    [self.lastProgress dismissViewControllerAnimated:YES completion:NULL];
}

- (void)hideProgress:(void (^)(void))completion {
    if (self.lastProgress) {
        [self.lastProgress dismissViewControllerAnimated:YES completion:completion];

        self.lastProgress = NULL;
    } else if (completion) {
        completion();
    }
}

- (void)hide:(CDVInvokedUrlCommand *)command {
    [self hideProgress:^{
        if (self.lastAlert) {
            [self.lastAlert dismissViewControllerAnimated:YES completion:^{
                self.lastAlert = NULL;

                CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
                [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
            }];
        } else {
            CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }
    }];
}

- (void)setNightMode:(CDVInvokedUrlCommand *)command {
    id value = [command.arguments objectAtIndex:0];
    if ([value boolValue]) {
        self.keyboardStyle = UIKeyboardAppearanceDark;
    } else {
        self.keyboardStyle = UIKeyboardAppearanceLight;
    }
}

-(UIViewController *)getTopPresentedViewController {
    UIViewController *presentingViewController = self.viewController;
    while(presentingViewController.presentedViewController != nil && ![presentingViewController.presentedViewController isBeingDismissed])
    {
        presentingViewController = presentingViewController.presentedViewController;
    }
    return presentingViewController;
}

@end
