#import <Cordova/CDV.h>

@interface AlertPlugin : CDVPlugin

- (void)showDialog:(CDVInvokedUrlCommand *)command;
- (void)showSheet:(CDVInvokedUrlCommand *)command;

@end