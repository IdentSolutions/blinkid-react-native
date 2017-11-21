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

BOOL backHidden = false;

-(void) setBackButton:(BOOL) value {
    backHidden = !value;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    _torch.hidden = ![[self containerViewController] overlayViewControllerShouldDisplayTorch:self];
    _back.hidden = backHidden;
}

- (IBAction)torchClicked:(id)sender {
    static BOOL torchOn = NO;
    torchOn = [[self containerViewController] isTorchOn];
    torchOn = !torchOn;
    if ([[self containerViewController] overlayViewControllerShouldDisplayTorch:self]) {
        [[self containerViewController] overlayViewController:self willSetTorch:torchOn];
    }
}

- (IBAction)skipClicked:(id)sender {
    [[[self containerViewController] scanningDelegate] scanningViewController:[self containerViewController] didFindError: [NSError alloc]];
}


- (IBAction)backClicked:(id)sender {
    [[self containerViewController] overlayViewControllerWillCloseCamera:self];
}

@end
