//
//  USDLFrontViewController.m
//  BlinkIDReactNative
//
//  Created by Greyson Hensley on 11/13/17.
//  Copyright © 2017 Jura Skrlec. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "USDLFrontViewController.h"
#import <MicroBlink/MicroBlink.h>

@implementation USDLFrontViewController : PPOverlayViewController
- (IBAction)torchClicked:(id)sender {
    NSLog(@"front torch");
    static BOOL torchOn = NO;
    torchOn = [[self containerViewController] isTorchOn];
    torchOn = !torchOn;
    if ([[self containerViewController] overlayViewControllerShouldDisplayTorch:self]) {
        [[self containerViewController] overlayViewController:self willSetTorch:torchOn];
    }
}
- (IBAction)cancelClicked:(id)sender {
    NSLog(@"Scan cancelled");
    [[self containerViewController] overlayViewControllerWillCloseCamera:self];
}


@end
