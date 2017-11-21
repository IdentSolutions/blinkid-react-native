//
//  USDLFrontViewController.h
//  BlinkIDReactNative
//
//  Created by Greyson Hensley on 11/13/17.
//  Copyright © 2017 Jura Skrlec. All rights reserved.
//

#import <MicroBlink/MicroBlink.h>
#import <UIKit/UIKit.h>

#ifndef USDLFrontViewController_h
#define USDLFrontViewController_h

#endif /* USDLFrontViewController_h */

@interface USDLFrontViewController: PPOverlayViewController
@property (weak, nonatomic) IBOutlet UIButton *back;
@property (weak, nonatomic) IBOutlet UIButton *torch;
- (void) setBackButton: (BOOL) value;
@end
