#import <Cordova/CDV.h>

@interface AlertPlugin : CDVPlugin

- (void)showDialog:(CDVInvokedUrlCommand *)command;
- (void)showSheet:(CDVInvokedUrlCommand *)command;
- (void)showProgress:(CDVInvokedUrlCommand *)command;
- (void)hide:(CDVInvokedUrlCommand *)command;

@property (nonatomic, retain) UIAlertController *lastAlert;
@property (nonatomic, retain) UIAlertController *lastProgress;

@end