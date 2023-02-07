package com.dji.activationDemo;
//---------------------------------------------------------------
import static com.dji.activationDemo.ToastUtils.setResultToToast;
import static com.dji.activationDemo.ToastUtils.showToast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.YuvImage;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ToggleButton;

import com.dji.sdk.sample.internal.utils.VideoFeedView;

import dji.common.airlink.PhysicalSource;
import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;
import dji.keysdk.callback.SetCallback;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.media.FetchMediaTask;
import dji.sdk.sdkmanager.DJISDKManager;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Random;
//-------------------------------------------------------------------


public class VideoFeederClass extends AppCompatActivity implements View.OnClickListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_feeder);
        init(); //Initialize UI, Callbacks, VideoListeners and

        CaptureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                  showToast("dentroCapture");
            }
        });

    }

    // ------------------------- Video Feeder Local Variables --------------------------
    private SetCallback setBandwidthCallback;
    private FetchMediaTask.Callback fetchMediaFileTaskCallback;
    private dji.sdk.camera.VideoFeeder.PhysicalSourceListener sourceListener;
    private VideoFeedView primaryVideoFeed;
    private VideoFeedView fpvVideoFeed;
    private Camera camera;
    protected DJICodecManager mCodecManager = null;

    // ------------------------- Video Feeder Local Variables ends --------------------------
    protected TextureView mVideoSurface = null;
    protected ImageView mImageSurface;
    private Bitmap Photobm;
    private Button CaptureBtn, ShootPhotoModeBtn, RecordVideoModeBtn;
    private ToggleButton RecordBtn;
    YuvImage yuv;


    //    ----------------------- Video Feeder Jambo Mambo Starts --------------------------
    private void init() {
        initui();
        initCallbacks();
        setUpListeners();

    }

    private void initui() {
        primaryVideoFeed = (VideoFeedView) findViewById(R.id.primary_video_feed);
        fpvVideoFeed = (VideoFeedView) findViewById(R.id.fpv_video_feed);
        CaptureBtn = findViewById(R.id.btn_capture);
        RecordBtn = findViewById(R.id.btn_down);

    }

    private void initCallbacks() {
        setBandwidthCallback = new SetCallback() {
            @Override
            public void onSuccess() {
                showToast("Set key value successfully");
                if (fpvVideoFeed != null) {
                    fpvVideoFeed.changeSourceResetKeyFrame();
                }
                if (primaryVideoFeed != null) {
                    primaryVideoFeed.changeSourceResetKeyFrame();
                }
            }
            @Override
            public void onFailure(@NonNull DJIError error) {
                showToast("Failed to set: " + error.getDescription());
            }
        };

    }



    private void setUpListeners() {
        sourceListener = new dji.sdk.camera.VideoFeeder.PhysicalSourceListener() {
            @Override
            public void onChange(dji.sdk.camera.VideoFeeder.VideoFeed videoFeed, PhysicalSource newPhysicalSource) {
                if (videoFeed == dji.sdk.camera.VideoFeeder.getInstance().getPrimaryVideoFeed()) {
                    String newText = "Primary Source: " + newPhysicalSource.toString();
                    setResultToToast("Primary Source: " + newPhysicalSource.toString());
                }
                if (videoFeed == dji.sdk.camera.VideoFeeder.getInstance().getSecondaryVideoFeed()) {
                    ToastUtils.setResultToToast("Secondary Source: " + newPhysicalSource.toString());
                }
            }
        };
        setVideoFeederListeners(true);
    }

    private void setVideoFeederListeners(boolean isOpen) {
        if (dji.sdk.camera.VideoFeeder.getInstance() == null) return;

        final BaseProduct product = DJISDKManager.getInstance().getProduct();
        //updateM210SeriesButtons();
        //updateM300Buttons();
        if (product != null) {
            dji.sdk.camera.VideoFeeder.VideoDataListener primaryVideoDataListener =
                    primaryVideoFeed.registerLiveVideo(dji.sdk.camera.VideoFeeder.getInstance().getPrimaryVideoFeed(), true);
            dji.sdk.camera.VideoFeeder.VideoDataListener secondaryVideoDataListener =
                    fpvVideoFeed.registerLiveVideo(dji.sdk.camera.VideoFeeder.getInstance().getSecondaryVideoFeed(), false);

            if (isOpen) {
                String newText =
                        "Primary Source: " + dji.sdk.camera.VideoFeeder.getInstance().getPrimaryVideoFeed().getVideoSource().name();
                //ToastUtils.setResultToText(primaryVideoFeedTitle, newText);
                if (Helper.isMultiStreamPlatform()) {
                    String newTextFpv = "Secondary Source: " + dji.sdk.camera.VideoFeeder.getInstance()
                            .getSecondaryVideoFeed()
                            .getVideoSource()
                            .name();
                    //ToastUtils.setResultToText(fpvVideoFeedTitle, newTextFpv);
                }
                dji.sdk.camera.VideoFeeder.getInstance().addPhysicalSourceListener(sourceListener);
                showToast("videofeeder");
            } else {
                dji.sdk.camera.VideoFeeder.getInstance().removePhysicalSourceListener(sourceListener);
                dji.sdk.camera.VideoFeeder.getInstance().getPrimaryVideoFeed().removeVideoDataListener(primaryVideoDataListener);
                if (Helper.isMultiStreamPlatform()) {
                    dji.sdk.camera.VideoFeeder.getInstance()
                            .getSecondaryVideoFeed()
                            .removeVideoDataListener(secondaryVideoDataListener);
                    showToast("que sopa");
                }
            }
        }
    }
//      ----------------------- Video Feeder Jambo Mambo Ends --------------------------

    //      ----------------------- Maybe take pictures Starts ---------------
    private void saveImageToExternalStorage(Bitmap finalBitmap) {
        String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
        File myDir = new File(root + "/Selfie_Drone");
        myDir.mkdirs();
        Random generator = new Random();
        int n = 10000;
        n = generator.nextInt(n);
        String fname = "Image-" + n + ".jpg";
        File file = new File(myDir, fname);
        if (file.exists())
            file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        MediaScannerConnection.scanFile(this, new String[]{file.toString()}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                        showToast("FOTOOOOOOOOOOO");

                    }
                });
    }

    // Method for taking photo
    private void captureAction() {

        if (ModuleVerificationUtil.isCameraModuleAvailable()) {
            camera = DemoApplication.getAircraftInstance().getCamera();
            if (ModuleVerificationUtil.isMatrice300RTK() || ModuleVerificationUtil.isMavicAir2()) {
                camera.setFlatMode(SettingsDefinitions.FlatCameraMode.PHOTO_SINGLE, djiError -> ToastUtils.setResultToToast("setFlatMode to PHOTO_SINGLE"));
            } else {
                camera.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, djiError -> ToastUtils.setResultToToast("setMode to shoot_PHOTO"));
            }

            if (isModuleAvailable()) {

                DemoApplication.getProductInstance()
                        .getCamera()
                        .startShootPhoto(new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if (null == djiError) {
                                    //ToastUtils.setResultToToast(getContext().getString(R.string.success));
                                } else {
                                    ToastUtils.setResultToToast(djiError.getDescription());
                                }

                            }
                        });
            }

        }
    }

    private boolean isModuleAvailable() {
        return (null != DemoApplication.getProductInstance()) && (null != DemoApplication.getProductInstance()
                .getCamera());
    }

//---------------------- Maybe take pictures Ends ---------------


    @Override
    public void onClick(View v) {


    }
}
