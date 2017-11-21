package com.microblink.reactnative.blinkid;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.microblink.activity.ScanActivity;
import com.microblink.hardware.camera.CameraType;
import com.microblink.image.ImageListener;
import com.microblink.metadata.ImageMetadata;
import com.microblink.reactnative.blinkid.R;
import com.microblink.detectors.DetectorResult;
import com.microblink.detectors.quad.QuadDetectorResult;
import com.microblink.hardware.SuccessCallback;
import com.microblink.hardware.orientation.Orientation;
import com.microblink.metadata.DetectionMetadata;
import com.microblink.metadata.Metadata;
import com.microblink.metadata.MetadataListener;
import com.microblink.metadata.MetadataSettings;
import com.microblink.metadata.MetadataSettings.ImageMetadataSettings;
import com.microblink.recognition.InvalidLicenceKeyException;
import com.microblink.recognizers.RecognitionResults;
import com.microblink.recognizers.settings.RecognitionSettings;
import com.microblink.recognizers.settings.RecognizerSettings;
import com.microblink.util.CameraPermissionManager;
import com.microblink.util.Log;
import com.microblink.view.CameraAspectMode;
import com.microblink.view.CameraEventsListener;
import com.microblink.view.OnSizeChangedListener;
import com.microblink.view.OrientationAllowedListener;
import com.microblink.view.recognition.DetectionStatus;
import com.microblink.view.recognition.RecognizerView;
import com.microblink.view.recognition.ScanResultListener;
import com.microblink.view.viewfinder.quadview.QuadViewManager;
import com.microblink.view.viewfinder.quadview.QuadViewManagerFactory;
import com.microblink.view.viewfinder.quadview.QuadViewPreset;

public class ScanDLActivity extends Activity implements ScanResultListener, CameraEventsListener, OnSizeChangedListener, MetadataListener{

    public static final int RESULT_SKIP = 100;

    public static final String EXTRAS_LICENSE_KEY = "EXTRAS_LICENSE_KEY";
    public static final String EXTRAS_CAMERA_TYPE = "EXTRAS_CAMERA_TYPE";
    public static final String EXTRAS_CAMERA_OVERLAY = "EXTRAS_CAMERA_OVERLAY";
    public static final String EXTRAS_RECOGNITION_SETTINGS = "EXTRAS_RECOGNITION_SETTINGS";
    public static final String EXTRAS_ENABLE_BEEP = "EXTRAS_ENABLE_BEEP";
    public static final String EXTRAS_IMAGE_METADATA_SETTINGS = "EXTRAS_IMAGE_METADATA_SETTINGS";
    public static final String EXTRAS_IMAGE_LISTENER = "EXTRAS_IMAGE_LISTENER";
    public static final String EXTRAS_RECOGNITION_RESULTS = "EXTRAS_RECOGNITION_RESULTS";

    public static final String USDL_FRONT = "USDLFront";
    public static final String USDL_BACK = "USDLBack";

    public static final String TAG = "ScanDLActivity";

    private Handler mHandler = new Handler();

    /** This is a RecognizerView - it contains camera view and can contain camera overlays */
    RecognizerView mRecognizerView;
    /** CameraPermissionManager is provided helper class that can be used to obtain the permission to use camera.
     * It is used on Android 6.0 (API level 23) or newer.
     */
    private CameraPermissionManager mCameraPermissionManager;
    /** This is the skip link */
    private TextView mSkipLink = null;
    /** This is a torch control button */
    private Button mTorchButton = null;
    /** Is torch enabled? */
    private boolean mTorchEnabled = false;
    /** This is a text field that contains status messages */
    private TextView mStatusTextView = null;
    /** This is BlinkID's built-in helper for built-in view that draws detection location */
    QuadViewManager mQvManager = null;
    /** MediaPlayer will be used for beep sound */
    private MediaPlayer mMediaPlayer = null;

    private boolean enableBeep = true;
    private ImageListener imageListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_dl);

        Bundle extras = getIntent().getExtras();

        String licenseKey = extras.getString(EXTRAS_LICENSE_KEY);
        CameraType cameraType = extras.getParcelable(EXTRAS_CAMERA_TYPE);
        RecognitionSettings recognitionSettings = extras.getParcelable(EXTRAS_RECOGNITION_SETTINGS);
        if(recognitionSettings == null) {
            recognitionSettings = new RecognitionSettings();
        }
        this.enableBeep = extras.getBoolean(EXTRAS_ENABLE_BEEP);
        ImageMetadataSettings imageMetadataSettings = (ImageMetadataSettings) extras.getParcelable(EXTRAS_IMAGE_METADATA_SETTINGS);
        this.imageListener = (ImageListener) extras.getParcelable(EXTRAS_IMAGE_LISTENER);
        String cameraOverlay = extras.getString(EXTRAS_CAMERA_OVERLAY);
        // obtain reference to RecognizerView
        mRecognizerView = (RecognizerView) findViewById(R.id.recognizerView);

        /*
         * Prepare settings for recognition.
         */
        mRecognizerView.setRecognitionSettings(recognitionSettings);


        try {
            mRecognizerView.setLicenseKey(licenseKey);
        } catch (InvalidLicenceKeyException e) {
            e.printStackTrace();
            Log.e(TAG, "Invalid licence key!");
            Toast.makeText(this, "Invalid licence key!", Toast.LENGTH_SHORT).show();
            finish();
        }

        // scan result listener will be notified when scan result gets available
        mRecognizerView.setScanResultListener(this);
        // camera events listener receives events such as when camera preview has started
        // or there was an error while starting the camera
        mRecognizerView.setCameraEventsListener(this);
        // orientation allowed listener is asked if orientation is allowed when device orientation
        // changes - if orientation is allowed, rotatable views will be rotated to that orientation
        mRecognizerView.setOrientationAllowedListener(new OrientationAllowedListener() {
            @Override
            public boolean isOrientationAllowed(Orientation orientation) {
                // allow all orientations
                return false;
            }
        });
        // on size changed listener is notified whenever the size of the view is changed (for example
        // when transforming the view from portrait to landscape or vice versa)
        mRecognizerView.setOnSizeChangedListener(this);

        // define which metadata will be available in MetadataListener (onMetadataAvailable method)
        MetadataSettings metadataSettings = new MetadataSettings();
        // detection metadata should be available in MetadataListener
        // detection metadata are all metadata objects from com.microblink.metadata.detection package
        metadataSettings.setDetectionMetadataAllowed(true);

        metadataSettings.setImageMetadataSettings(imageMetadataSettings);
        // set metadata listener and defined metadata settings
        // metadata listener will obtain selected metadata
        mRecognizerView.setMetadataListener(this, metadataSettings);

        // set initial orientation
        mRecognizerView.setInitialOrientation(Orientation.ORIENTATION_PORTRAIT);

        // set camera aspect mode to FILL - this will use the entire surface
        // for camera preview, instead of letterboxing it
        mRecognizerView.setAspectMode(CameraAspectMode.ASPECT_FILL);

        // instantiate the camera permission manager
        mCameraPermissionManager = new CameraPermissionManager(this);
        // get the built in layout that should be displayed when camera permission is not given
        View v = mCameraPermissionManager.getAskPermissionOverlay();
        if (v != null) {
            // add it to the current layout that contains the recognizer view
            ViewGroup vg = (ViewGroup) findViewById(R.id.my_scan_root);
            vg.addView(v);
        }

        // create scanner (make sure scan settings and listeners were set prior calling create)
        mRecognizerView.create();

        // after scanner is created, you can add your views to it

        // initialize buttons and status view
        View view = null;
        if (USDL_FRONT.equals(cameraOverlay)) {
            view = getLayoutInflater().inflate(R.layout.front_dl_viewfinder, null);
        } else {
            view = getLayoutInflater().inflate(R.layout.back_dl_viewfinder, null);
        }

        /** setup skip button */
        mSkipLink = (TextView) view.findViewById(R.id.skipLink);
        if (mSkipLink != null) {
            mSkipLink.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    setResult(Activity.RESULT_CANCELED);
                    finish();
                }
            });
        }

        mTorchButton = (Button) view.findViewById(R.id.defaultTorchButton);
        mTorchButton.setVisibility(View.GONE);

        mStatusTextView = (TextView) view.findViewById(R.id.defaultStatusTextView);
        // hide status text
        mStatusTextView.setVisibility(View.INVISIBLE);

        // add buttons and status view as rotatable view to BlinkIdView (it will be rotated even if activity remains in portrait/landscape)
        // allowed orientations are controlled via OrientationAllowedListener
        mRecognizerView.addChildView(view, true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // all activity lifecycle events must be passed on to RecognizerView
        if (mRecognizerView != null) {
            mRecognizerView.resume();
        }

        if (this.enableBeep) {
            mMediaPlayer = MediaPlayer.create(this, R.raw.beep);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // all activity lifecycle events must be passed on to RecognizerView
        if(mRecognizerView != null) {
            mRecognizerView.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // all activity lifecycle events must be passed on to RecognizerView
        if(mRecognizerView != null) {
            mRecognizerView.pause();
        }
        if (mMediaPlayer != null) {
            mMediaPlayer = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // all activity lifecycle events must be passed on to RecognizerView
        if(mRecognizerView != null) {
            mRecognizerView.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // all activity lifecycle events must be passed on to RecognizerView
        if(mRecognizerView != null) {
            mRecognizerView.destroy();
        }
    }

    /**
     * Plays beep sound.
     */
    private void soundNotification() {
        if (mMediaPlayer != null && this.enableBeep) {
            Log.d(TAG, "Playing beep sound");
            mMediaPlayer.start();
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (mMediaPlayer == null) {
                        mp.release();
                    }
                }
            });
        }
    }

    @Override
    public void onScanningDone(RecognitionResults results) {
        soundNotification();
        mRecognizerView.pauseScanning();
        Intent resultIntent = new Intent();
        resultIntent.putExtra(ScanActivity.EXTRAS_RECOGNITION_RESULTS, results);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    public void onCameraPreviewStarted() {
        // this method is called just after camera preview has started
        enableTorchButtonIfPossible();
    }

    @Override
    public void onCameraPreviewStopped() {
        // this method is called just after camera preview has stopped
    }

    private void enableTorchButtonIfPossible() {
        if (mRecognizerView.isCameraTorchSupported() && mTorchButton != null) {
            mTorchButton.setVisibility(View.VISIBLE);
            mTorchButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    mRecognizerView.setTorchState(!mTorchEnabled, new SuccessCallback() {
                        @Override
                        public void onOperationDone(final boolean success) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (success) {
                                        mTorchEnabled = !mTorchEnabled;
                                        if (mTorchEnabled) {
                                            mTorchButton.setBackgroundResource(R.drawable.flash_on);
                                        } else {
                                            mTorchButton.setBackgroundResource(R.drawable.flash_off);
                                        }
                                    }
                                }
                            });
                        }
                    });
                }
            });
        }
    }


    @Override
    public void onError(Throwable ex) {
        // This method will be called when opening of camera resulted in exception or
        // recognition process encountered an error.
        // The error details will be given in ex parameter.
        com.microblink.util.Log.e(this, ex, "Error");
        handleError();
    }

    @SuppressWarnings("deprecation")
    private void handleError() {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(getString(R.string.error));
        alertDialog.setMessage(getString(R.string.errorDesc));

        alertDialog.setButton(getString(R.string.photopayOK), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (dialog != null) {
                    dialog.dismiss();
                }
                setResult(Activity.RESULT_CANCELED, null);
                finish();
            }
        });
        alertDialog.setCancelable(false);
        alertDialog.show();
    }

    private void displayText(final int textId) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mStatusTextView.setText(textId);
            }
        });
    }

    @Override
    public void onAutofocusFailed() {
        // this method is called if camera cannot perform autofocus
        // this method is called from background (focusing) thread
        // so make sure you post UI actions on UI thread
        displayText(R.string.AutofocusFail);
    }

    @Override
    public void onAutofocusStarted(Rect[] rects) {
    }

    @Override
    public void onAutofocusStopped(Rect[] rects) {
    }

    @Override
    public void onSizeChanged(int width, int height) {
        // this is called whenever size of the BlinkIDView changes
        // we will use this callback in this example to adjust the margins of buttons
        int horizontalMargin = (int) (width * 0.07);
        int verticalMargin = (int) (height * 0.07);
        // set margins for torch button
        FrameLayout.LayoutParams torchButtonParams = (FrameLayout.LayoutParams) mTorchButton.getLayoutParams();
        if (torchButtonParams.leftMargin != horizontalMargin && torchButtonParams.topMargin != verticalMargin) {
            torchButtonParams.setMargins(horizontalMargin, verticalMargin, horizontalMargin, verticalMargin);
            mTorchButton.setLayoutParams(torchButtonParams);
        }
        // set margins for text view
        FrameLayout.LayoutParams statusViewParams = (FrameLayout.LayoutParams) mStatusTextView.getLayoutParams();
        if (statusViewParams.bottomMargin != verticalMargin) {
            if (android.os.Build.VERSION.SDK_INT <= 7) {
                statusViewParams.setMargins(0, verticalMargin, 0, verticalMargin);
            } else {
                statusViewParams.setMargins(horizontalMargin, verticalMargin, horizontalMargin, verticalMargin);
            }
            mStatusTextView.setLayoutParams(statusViewParams);
        }
    }

    @Override
    public void onMetadataAvailable(Metadata metadata) {
        // This method will be called when metadata becomes available during recognition process.
        // Here, for every metadata type that is allowed through metadata settings,
        // desired actions can be performed.

        // detection metadata contains detection locations
        if (metadata instanceof ImageMetadata) {
            this.imageListener.onImageAvailable(((ImageMetadata)metadata).getImage());
        }
    }

    @Override
    @TargetApi(23)
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // on API level 23, request permission result should be passed to camera permission manager
        mCameraPermissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    @TargetApi(23)
    public void onCameraPermissionDenied() {
        // this method is called on Android 6.0 and newer if camera permission was not given
        // by user

        // ask user to give a camera permission. Provided manager asks for
        // permission only if it has not been already granted.
        // on API level < 23, this method does nothing
        mCameraPermissionManager.askForCameraPermission();
    }
}
