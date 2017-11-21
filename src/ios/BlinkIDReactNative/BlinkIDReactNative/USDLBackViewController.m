//
//  USDLBackViewController.m
//  BlinkIDReactNative
//
//  Created by Greyson Hensley on 11/13/17.
//  Copyright © 2017 Jura Skrlec. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "USDLBackViewController.h"
#import <MicroBlink/MicroBlink.h>

@implementation USDLBackViewController : PPOverlayViewController
- (void)viewDidLoad
{
    [super viewDidLoad];
    _torch.hidden = ![[self containerViewController] overlayViewControllerShouldDisplayTorch:self];
}
- (IBAction)torchClicked:(id)sender {
    static BOOL torchOn = NO;
    torchOn = [[self containerViewController] isTorchOn];
    torchOn = !torchOn;
    if ([[self containerViewController] overlayViewControllerShouldDisplayTorch:self]) {
        [[self containerViewController] overlayViewController:self willSetTorch:torchOn];
    }
}
- (IBAction)cancelClicked:(id)sender {
    //[[self containerViewController] skip:self];
    [[[self containerViewController] scanningDelegate] scanningViewController:[self containerViewController] didFindError: [NSError alloc]];
    //[[self containerViewController] overlayViewControllerWillCloseCamera:self];
}


@end
