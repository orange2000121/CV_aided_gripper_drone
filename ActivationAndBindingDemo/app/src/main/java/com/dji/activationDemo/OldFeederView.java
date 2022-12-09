package com.dji.activationDemo;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.aruco.Aruco;
import org.opencv.aruco.DetectorParameters;
import org.opencv.aruco.Dictionary;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import static com.dji.activationDemo.ToastUtils.showToast;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.sqrt;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import dji.common.camera.CameraVideoStreamSource;
import dji.common.camera.ResolutionAndFrameRate;
import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.flightcontroller.LEDsSettings;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;

import dji.common.gimbal.GimbalState;
import dji.common.product.Model;
import dji.common.util.CommonCallbacks;
import dji.midware.data.model.P3.DataCameraGetPushTauParam;
import dji.midware.usb.P3.UsbAccessoryService;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.Lens;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.Compass;
import dji.sdk.flightcontroller.FlightAssistant;
import dji.sdk.flightcontroller.FlightController;
import dji.waypointv2.common.waypointv2.ActionEvent;

public class OldFeederView extends AppCompatActivity implements TextureView.SurfaceTextureListener {
    private static final String TAG = DemoApplication.class.getName();
    protected VideoFeeder.VideoDataListener mReceivedVideoDataListener = null;
    protected DJICodecManager mCodecManager = null;

//--------Flight Controller
    private FlightController flightController= null;
    private FlightAssistant flightAssistant = null;
    private Button TurnOnMotorsBtn,TurnOffMotorsBtn, TakeOffBtn, LandBtn,DisableVirtualStick,EnableVirtualStick,ArucoBtn;
    private Button EmergencyBtn,ForwardBtn, YawBtn,BackwardsBtn, RightBtn ,LeftBtn,UpBtn, DownBtn, MoveTo;
    private OnScreenJoystick screenJoystickRight,screenJoystickLeft;
    private Timer mSendVirtualStickDataTimer;
    private SendVirtualStickDataTask mSendVirtualStickDataTask;
    private Compass compass;
    private boolean emg_now = false;
    double fixz;
    double seg1_dist[]={0,0,0};
    double seg2_dist[]={0,0,0};
    double seg3_dist[]={0,0,0};
    double totalflytime=0;
    double back_first_fly_time;
    double back_pitch;
    double back_roll;
    double back_throttle;
    double back_yaw;

//--------Camera
    private Button mCaptureBtn;
    private Camera camera;
    private Lens lens;

//--------Video Feed
    protected TextureView mVideoTexture = null;
    protected ImageView mImageSurface;
    private Bitmap sourceBitmap, BitmapFromFeedersSurface;
    private Mat RGBmatFromBitmap;
    private MatOfInt ids;
    private Dictionary dictionary;
    private DetectorParameters parameters;
//--------
    ArrayList<Float> allx = new ArrayList<>();
    ArrayList<Float> ally = new ArrayList<>();
    ArrayList<Float> all_z = new ArrayList<>();
    ArrayList<Float> all_roll = new ArrayList<>();
    ArrayList<Float> all_pitch = new ArrayList<>();
    ArrayList<Float> all_yaw = new ArrayList<>();
    float avgx=0,avgy=0,avgz=0,globalavgx=0,globalavgy=0,globalavgz=0;
    double pX = 0, pY = 0;
    private float pitch = 0;
    private float roll = 0;
    private float yaw = 0;
    private float throttle = 0;
    private float MaxAllowedDistance = 0;
    private float MaxAllowedSpeed = 0;
    private float MaxAllowedAcceleration = 0;


    double p11,p12,p21,p22,p31,p32,p41,p42;
    double p11f,p12f,p21f,p22f,p31f,p32f,p41f,p42f;

    float zarucofloat, yarucofloat,xarucofloat,yawarucofloat;
    //Real time store

    float radtodeg = (float) (180/3.141592);
    double arucoroll;
    double arucopitch;
    double arucoyaw;
    double que2;
    double que3;
    double que;
    double[] arucotranslationvector;
    float pitchJoyControlMaxSpeed = 10;
    float rollJoyControlMaxSpeed = 10;
    float verticalJoyControlMaxSpeed = 2;
    float yawJoyControlMaxSpeed = 20;

    private final BaseLoaderCallback loaderCallback = new BaseLoaderCallback(this){
        @Override
        public void onManagerConnected(int status){
            if(status == LoaderCallbackInterface.SUCCESS){
                String message = "";
                Toast.makeText(OldFeederView.this,  message,  Toast.LENGTH_SHORT).show();
            }
            else {
                super.onManagerConnected(status);
            }
        }
    };

    private void initParams() {

        // We recommand you use the below settings, a standard american hand style.
        if (flightController == null) {
            if (ModuleVerificationUtil.isFlightControllerAvailable()) {
                flightController = DemoApplication.getAircraftInstance().getFlightController();
                flightAssistant = flightController.getFlightAssistant();
            }
        }
        flightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
        flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
        flightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
        flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);

        flightAssistant.setLandingProtectionEnabled(false,null);
        flightAssistant.setCollisionAvoidanceEnabled(false, null);


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_old_feeder_view);
        initUI();
        initParams();
        dronestart();
        FlightController flightController = ModuleVerificationUtil.getFlightController();
        if (flightController == null) {
            return;
        }
        if(OpenCVLoader.initDebug()){
            loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            //showToast("Loads OpenCV");
        }
        mReceivedVideoDataListener = new VideoFeeder.VideoDataListener() {
            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                if (mCodecManager != null) {
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                }
            }
        };
        mCaptureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveImageToExternalStorage(BitmapFromFeedersSurface);
                SetLEDs(0);

            }
        });
        TakeOffBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flightController.startTakeoff(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        showToast("taking off");
                    }
                });
            }
        });
        LandBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flightController.startLanding(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        showToast("Landing");
                    }
                });
            }
        });
        YawBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SetYaw(50,3000);
            }
        });

        EmergencyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emg_now = !emg_now;
                setZero();
                saveImageToExternalStorage(BitmapFromFeedersSurface);
                if(emg_now){
                    showToast("Emergency");
                }else {
                    showToast("dismiss the alert");
                }
            }
        });
        ForwardBtn.setOnClickListener(new View.OnClickListener() {
            //SetForward(50,1000);
            @Override
            public void onClick(View v) {
                roll = (float) (1);
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setZero();
                        showToast("forward");
                    }
                }, (long) 2000);

            }
        });
        BackwardsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                roll = (float) (-1);
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setZero();
                        showToast("Backward");
                    }
                },(long) 2000 );
                //SetBackward(50,1000);
            }
        });
        RightBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pitch = (float) 1;
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setZero();
                        showToast("Right");
                    }
                },(long) 2000 );

                //SetRight(50,1000);
            }
        });
        LeftBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pitch = (float) -1;
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setZero();
                        showToast("LEft ---");
                    }
                },(long) 2000 );
                //SetLeft(50,1000);
            }
        });
        UpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                throttle = (float) (1);
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setZero();
                        showToast("Up ");
                    }
                },(long) 2000 );
                //SetLeft(50,1000);
            }
        });
        DownBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                throttle = (float) (-1);
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setZero();
                        showToast("Down -");
                    }
                },(long) 2000 );
                //SetLeft(50,1000);
            }
        });
        ArucoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                EnableVirtualStick.performClick();
                TakeOffBtn.performClick();

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //Do something here
                        Segment1(arucotranslationvector[0],fixz,-arucotranslationvector[1]);
                        //Segment2(arucotranslationvector[0],fixz,-arucotranslationvector[1]);
                    }
                }, (long) (5000));
//                showToast("0 %3.f"+arucotranslationvector[0]+"  1  %3.f"+arucotranslationvector[1]+"  2  %3.f"+arucotranslationvector[2]);
//                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
//                    @Override
//                    public void run() {

//                    }
//                }, 2000);
//
//                Segment1(1.8,1.2,-2.3);
//                GoForwardSequence();


            }
        });

        MoveTo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showToast("Empty block");
            }
        });

        EnableVirtualStick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flightController.setVirtualStickModeEnabled(true, djiError -> {
                    flightController.setVirtualStickAdvancedModeEnabled(true);
                    if (djiError != null) {
                        ToastUtils.setResultToToast(djiError.getDescription());
                    } else {
                        showToast("VS Enabled");
                    }
                });
            }
        });
        DisableVirtualStick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flightController.setVirtualStickModeEnabled(false, djiError -> {
                    flightController.setVirtualStickAdvancedModeEnabled(true);
                    if (djiError != null) {
                        ToastUtils.setResultToToast(djiError.getDescription());
                    } else {
                        showToast("VS Disabled");
                    }
                });
            }
        });
        TurnOnMotorsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flightController.turnOnMotors(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            ToastUtils.setResultToToast(djiError.getDescription());
                        }else{
                            showToast("Turning on Motors");
                        }
                    }
                });
            }
        });
        TurnOffMotorsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flightController.turnOffMotors(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            ToastUtils.setResultToToast(djiError.getDescription());
                        }else{
                            showToast("Turning off Motors");
                        }
                    }
                });
            }
        });
        screenJoystickRight.setJoystickListener(new OnScreenJoystickListener(){
            @Override
            public void onTouch(OnScreenJoystick joystick, float pX, float pY) {
                if(Math.abs(pX) < 0.02 ){
                    pX = 0;
                }
                if(Math.abs(pY) < 0.02 ){
                    pY = 0;
                }
                float pitchJoyControlMaxSpeed = 10;
                float rollJoyControlMaxSpeed = 10;

                pitch = (float)(pitchJoyControlMaxSpeed * pX);
                roll = (float)(rollJoyControlMaxSpeed * pY);

                TextView theTextView21  = (TextView) findViewById(R.id.textView4);
                TextView theTextView22  = (TextView) findViewById(R.id.textView5);
                theTextView21.setText(" L/R = " + String.format("%.2f",pitch));
                theTextView21.setTextColor(Color.RED);
                theTextView22.setText(" F/B = " + String.format("%.2f",roll));
                theTextView22.setTextColor(Color.RED);

                if (null == mSendVirtualStickDataTimer) {
                    mSendVirtualStickDataTask = new
                            SendVirtualStickDataTask();
                    mSendVirtualStickDataTimer = new Timer();
                    mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 100, 200);
                }
            }

        });

        screenJoystickLeft.setJoystickListener(new OnScreenJoystickListener() {
            @Override
            public void onTouch(OnScreenJoystick joystick, float pX, float pY) {
                if(Math.abs(pX) < 0.02 ){
                    pX = 0;
                }
                if(Math.abs(pY) < 0.02 ){
                    pY = 0;
                }
                float verticalJoyControlMaxSpeed = 2;
                float yawJoyControlMaxSpeed = 30;

                yaw = (float)(yawJoyControlMaxSpeed * pX);
                throttle = (float)(verticalJoyControlMaxSpeed * pY);

                TextView theTextView7  = (TextView) findViewById(R.id.textView7);
                TextView theTextView6  = (TextView) findViewById(R.id.textView6);
//
//                theTextView7.setText(" Yaw = " + String.format("%.2f",yaw));
//                theTextView7.setTextColor(Color.RED);
                theTextView6.setText(" U/D = " + String.format("%.2f",throttle));
                theTextView6.setTextColor(Color.RED);

                if (null == mSendVirtualStickDataTimer) {
                    //mSendVirtualStickDataTask = new SendVirtualStickDataTask();
                    mSendVirtualStickDataTimer = new Timer();
                    mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 0, 200);
                }
            }
        });

    }

    public void saveImageToExternalStorage(Bitmap finalBitmap) {
        //Bitmap resized = null;
        //resized = Bitmap.createScaledBitmap(finalBitmap,1280,960, true);
        String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
        File myDir = new File(root + "0/Selfie_Drone");
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
        }
        catch (Exception e) {
            e.printStackTrace();
            Writer writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            String s = writer.toString();
            showToast(s);
        }

        MediaScannerConnection.scanFile(this, new String[] { file.toString() }, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });
    }


    private void initUI() {
        mVideoTexture = (TextureView) findViewById(R.id.video_previewer_surface);
        mImageSurface = (ImageView) findViewById(R.id.flight_image_previewer_surface);
        mCaptureBtn = (Button) findViewById(R.id.btn_capture);
        DownBtn = findViewById(R.id.btn_down);
        UpBtn = findViewById(R.id.btn_up);
        TakeOffBtn = findViewById(R.id.btn_takeoff);
        LandBtn = findViewById(R.id.btn_land);
        EmergencyBtn = findViewById(R.id.btn_emg);
        ForwardBtn = findViewById(R.id.btn_forward);
        BackwardsBtn = findViewById(R.id.btn_backwards);
        LeftBtn = findViewById(R.id.btn_left);
        RightBtn = findViewById(R.id.btn_right);
        YawBtn = findViewById(R.id.btn_yaw);
        MoveTo = findViewById(R.id.btn_move_to);
        ArucoBtn = findViewById(R.id.btn_aruco);
        EnableVirtualStick = findViewById(R.id.btn_enable_virtual_stick);
        DisableVirtualStick = findViewById(R.id.btn_disable_virtual_stick);
        screenJoystickRight = (OnScreenJoystick)findViewById(R.id.directionJoystickRight);
        screenJoystickLeft = (OnScreenJoystick)findViewById(R.id.directionJoystickLeft);
        TurnOnMotorsBtn = findViewById(R.id.btn_turn_on_motors);
        TurnOffMotorsBtn = findViewById(R.id.btn_turn_off_motors);


        if (null != mVideoTexture) {
            mVideoTexture.setSurfaceTextureListener(this);
        }
    }



    private void initPreviewer() {

        BaseProduct product = DemoApplication.getProductInstance();

        if (product == null || !product.isConnected()) {
            showToast(getString(R.string.disconnected));
        } else {
            if (null != mVideoTexture) {
                mVideoTexture.setSurfaceTextureListener(this);
            }
            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(mReceivedVideoDataListener);
            }
        }
    }


    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
        super.onResume();
        initPreviewer();
        onProductChange();
        //initFlightController();

        if (ModuleVerificationUtil.isGimbalModuleAvailable()) {
            DemoApplication.getProductInstance().getGimbal().setStateCallback(new GimbalState.Callback() {
                @Override
                public void onUpdate(@NonNull GimbalState gimbalState) {
                    showToast("dentro module");
                }
            });
        }
        if(OpenCVLoader.initDebug())
            loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        if(mVideoTexture == null) {
            Log.e(TAG, "mVideoSurface is null");
        }
        DisableVirtualStick.performClick();
    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
        if (mCodecManager == null) {
            mCodecManager = new DJICodecManager(this, surface, width, height);
        }

    }
    protected void onProductChange() {
        initPreviewer();
    }


    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager = null;
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
        ArucoDetector();


    }

    private void ArucoDetector() {

        int picwidth = 1280;
        int picheight = 960;
        float MarkerSizeinm = (float) 0.181;  //A4 paper
        //float MarkerSizeinm = (float) 0.282;  //A3 paper

        double tall=0, with=0;
        tall = mCodecManager.getVideoHeight();
        with = mCodecManager.getVideoWidth();
        //showToast("height= "+tall+"    width= "+with);

        List<Mat> corners = new ArrayList();
        corners.clear();
        Mat droneImage = new Mat();
        Mat grayImage = new Mat();
        ids = new MatOfInt();
        parameters = DetectorParameters.create();
        parameters.set_cornerRefinementMethod(1);
        parameters.set_cornerRefinementMinAccuracy(0.05);
        parameters.set_cornerRefinementWinSize(5);
        dictionary = Aruco.getPredefinedDictionary(Aruco.DICT_6X6_50); //MARKER NUMBER 23
        //dictionary = Aruco.getPredefinedDictionary(Aruco.DICT_4X4_50); //MARKER NUMBER 23
        BitmapFromFeedersSurface = Bitmap.createScaledBitmap(mVideoTexture.getBitmap(),picwidth,picheight, true);
        //BitmapFromFeedersSurface = mVideoTexture.getBitmap();
        //showToast(("Width"+BitmapFromFeedersSurface.getWidth())+"   Height"+(BitmapFromFeedersSurface.getHeight()));
        RGBmatFromBitmap = new Mat();

        Utils.bitmapToMat(BitmapFromFeedersSurface, droneImage);
        Imgproc.cvtColor(droneImage, grayImage, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(droneImage, RGBmatFromBitmap, Imgproc.COLOR_RGBA2RGB);
        Aruco.detectMarkers(grayImage, dictionary, corners, ids, parameters);

        if(corners.size()>0){
            //Draw lines at center of the image
            //Vertical line
            Point  startverlin = new Point(picwidth/2, 0);
            Point  endverlin = new Point(picwidth/2, picheight);
            Scalar colorlin = new Scalar(255, 0, 0);
            int thickness = 3;
            Imgproc.line(RGBmatFromBitmap, startverlin, endverlin, colorlin, thickness);

            //Horizontal line
            Point starthorlin = new Point(0, picheight/2);
            Point endhorlin = new Point(picwidth, picheight/2);
            Imgproc.line(RGBmatFromBitmap, starthorlin, endhorlin, colorlin, thickness);

            Mat rvecs = new Mat();
            Mat tvecs = new Mat();
            Mat rvec1 = new Mat();
            Mat tvec1 = new Mat();


//                        //  Test camera matrix
//            Mat cameraMatrix = Mat.zeros(3, 3, CvType.CV_64F); //300 - 600
//            cameraMatrix.put(0, 0, 761.05352031); //fx
//            cameraMatrix.put(1, 1, 761.05352031); //fy
//            cameraMatrix.put(0, 2, 646.50638026); //cx
//            cameraMatrix.put(1, 2, 589.55309259); //cy
//            cameraMatrix.put(2, 2, 1);
////            showToast(cameraMatrix.toString());
//
//            // Distorsion coefficients
//            Mat distCoeffs = Mat.zeros(5, 1, CvType.CV_64F);
//            distCoeffs.put(0,0, -0.05962225);
//            distCoeffs.put(1,0,0.15009865 );
//            distCoeffs.put(2,0,0.00400874);
//            distCoeffs.put(3,0, -0.00682546);
//            distCoeffs.put(4,0,  0.48024688);
//            distCoeffs.put(5,0,   0.08495 );
//            distCoeffs.put(6,0,  0.08880626);
//            distCoeffs.put(7,0,  0.61245505);



//            //  Test camera matrix
            Mat cameraMatrix = Mat.zeros(3, 3, CvType.CV_64F); //300 - 600
            cameraMatrix.put(0, 0, 577.12265401-30); //fx
            cameraMatrix.put(1, 1, 577.12265401-30); //fy
            cameraMatrix.put(0, 2, 624.27619131); //cx
            cameraMatrix.put(1, 2, 493.79682551); //cy
            cameraMatrix.put(2, 2, 1);
//            showToast(cameraMatrix.toString());

            // Distorsion coefficients
            Mat distCoeffs = Mat.zeros(8, 1, CvType.CV_64F);
            distCoeffs.put(0,0, -1.00067366e-01);
            distCoeffs.put(1,0,-4.24388662e-02 );
            distCoeffs.put(2,0,5.06785331e-05);
            distCoeffs.put(3,0, -2.77194518e-03);
            distCoeffs.put(4,0,  3.90751297e-01);
            distCoeffs.put(5,0,   1.76991593e-02 );
            distCoeffs.put(6,0,  -3.57610859e-02);
            distCoeffs.put(7,0,  4.21848915e-01);

//              OG camera matrix
//            Mat cameraMatrix = Mat.zeros(3, 3, CvType.CV_64F);
//            cameraMatrix.put(0, 0, 727.38195801); //fx
//            cameraMatrix.put(1, 1, 639.74169922); //fy
//            cameraMatrix.put(0, 2, 659.16527361); //cx
//            cameraMatrix.put(1, 2, 309.58174906); //cy
//            cameraMatrix.put(2, 2, 1);
//
//            // Distorsion coefficients
//            Mat distCoeffs = Mat.zeros(5, 1, CvType.CV_64F);
//            distCoeffs.put(0,0, 0.04491797);
//            distCoeffs.put(1,0,-0.03559581);
//            distCoeffs.put(2,0,-0.00177915);
//            distCoeffs.put(3,0, 0.00825589);
//            distCoeffs.put(4,0, 0.00827315);


            //Objects Points

            // DIST FROM CENTER OF THE MARKER TO 4 CORNERS IN M WIDTH = 0.172M  HEIGHT = 0.171M  AVG = 0.1715 /2 = 0.08575
            MatOfPoint3f objPoints = new MatOfPoint3f(new Point3(-MarkerSizeinm/2, MarkerSizeinm/2, 0), new Point3(MarkerSizeinm/2, MarkerSizeinm/2, 0),
                    new Point3(-MarkerSizeinm/2, -MarkerSizeinm/2, 0),new Point3(MarkerSizeinm/2, -MarkerSizeinm/2, 0));

//Pose Estimation

            Aruco.drawDetectedMarkers(RGBmatFromBitmap, corners, ids);

            //Corners values with format (y,x)
            double[] tl = (corners.get(0).get(0,0));
            double[] tr = corners.get(0).get(0,1);
            double[] br = corners.get(0).get(0,2);
            double[] bl = corners.get(0).get(0,3);

            //AVG = 0.1715M
            Aruco.estimatePoseSingleMarkers(corners, MarkerSizeinm, cameraMatrix, distCoeffs, rvecs, tvecs);

            //REAL WORLD CORNERS IN MM
            List<Point3> corners4 = new ArrayList<>(4);
            corners4.add(new Point3(-MarkerSizeinm/2,MarkerSizeinm/2,0));		// Top-Left
            corners4.add(new Point3(MarkerSizeinm/2,MarkerSizeinm/2,0));		// Top-Right
            corners4.add(new Point3(MarkerSizeinm/2,-MarkerSizeinm/2,0));		// Bottom-Right
            corners4.add(new Point3(-MarkerSizeinm/2,-MarkerSizeinm/2,0));		// Bottom-Left


            MatOfPoint3f mcorners = new MatOfPoint3f();
            mcorners.fromList(corners4);

            for(int i = 0;i<ids.toArray().length;i++){

                Calib3d.drawFrameAxes(RGBmatFromBitmap, cameraMatrix, distCoeffs, rvecs.row(i), tvecs.row(i), 0.13f);
                Mat arucorotationmat = new Mat(3,3,6);
                Calib3d.Rodrigues (rvecs.row(i), arucorotationmat);
                Mat cameraMatrixAruco = new Mat();
                Mat rotMatrixAru = new Mat();
                Mat transVectAru = new Mat();
                Mat ArucoeulerAngles  =  new Mat();
                Mat rotMatrixX22 = new Mat();
                Mat rotMatrixY22 = new Mat();
                Mat rotMatrixZ22= new Mat();
                Mat projMatrix22 = new Mat();
                Mat RT = Mat.zeros(3,4,CvType.CV_64F);

                RT.put(0,0,arucorotationmat.get(0,0)[0]);
                RT.put(0,1,arucorotationmat.get(0,0)[0]);
                RT.put(0,2,arucorotationmat.get(0,2)[0]);
                RT.put(0,3, tvecs.get(i,0)[0]);
                RT.put(1,0,arucorotationmat.get(1,0)[0]);
                RT.put(1,1,arucorotationmat.get(1,1)[0]);
                RT.put(1,2,arucorotationmat.get(1,2)[0]);
                RT.put(1,3, tvecs.get(i,0)[1]);
                RT.put(2,0,arucorotationmat.get(2,0)[0]);
                RT.put(2,1,arucorotationmat.get(2,1)[0]);
                RT.put(2,2,arucorotationmat.get(2,2)[0]);
                RT.put(2,3,tvecs.get(i,0)[2]);

                Core.gemm(cameraMatrix, RT,  1,new Mat(),0,projMatrix22,0);

                Calib3d.decomposeProjectionMatrix(projMatrix22,cameraMatrixAruco,rotMatrixAru,transVectAru,rotMatrixX22,rotMatrixY22,rotMatrixZ22,ArucoeulerAngles);

                arucotranslationvector = tvecs.get(i,0); //for debugging, printing on screen
                arucoroll = ArucoeulerAngles.get(0,0)[0];  //for debugging, printing on screen
                arucopitch = ArucoeulerAngles.get(1,0)[0];
                arucoyaw = -ArucoeulerAngles.get(2,0)[0];// change sign to get the rotation needed by the drone not the paper


                distCoeffs = new MatOfDouble(distCoeffs);

                MatOfPoint2f projected = new MatOfPoint2f();

                Calib3d.projectPoints(mcorners, rvecs.row(i), tvecs.row(i), cameraMatrix, (MatOfDouble) distCoeffs, projected);

                Point[] points = projected.toArray();

                if(points != null){
                    for(Point point:points){
                        Imgproc.circle(RGBmatFromBitmap, points[0],10, new Scalar(255, 0, 0), 4);
                        Imgproc.circle(RGBmatFromBitmap, points[1],10, new Scalar(0, 0, 0), 4);
                        Imgproc.circle(RGBmatFromBitmap, points[2],10, new Scalar(0, 255, 0, 150), 4);
                        Imgproc.circle(RGBmatFromBitmap, points[3],10, new Scalar(0, 0, 255), 4);

                        //ESTIMATED ARUCO CORNERS IN PIXELS
                        p11 = (int) points[0].x; //TOP LEFT X
                        p12 = (int) points[0].y; //TOP LEFT Y
                        p21 = (int) points[1].x; //TOP RIGHT X
                        p22 = (int) points[1].y; //TOP RIGHT Y
                        p31 = (int) points[2].x; //BOTTOM RIGHT X
                        p32 = (int) points[2].y; //BOTTOM RIGHT Y
                        p41 = (int) points[3].x; //BOTTOM LEFT X
                        p42 = (int) points[3].y; //BOTTOM RIGHT Y

                    }
                }
                projected.release();
            }

            //doubles to float to make things faster
            zarucofloat= (float) arucotranslationvector[2]; //the two value is the z axis through the camera
            yarucofloat = (float) -arucotranslationvector[1] ; //times -1 to make up distances positives
            xarucofloat = (float) arucotranslationvector[0]; //the zero value is the x axis, side to side of the camera
            yawarucofloat = (float) arucoyaw;

            //Turn negatives angles into positives
            if(yawarucofloat<0){
                yawarucofloat=yawarucofloat+360;
            }

            //arraylists to hold values
            allx.add(xarucofloat);
            ally.add(yarucofloat);
            all_z.add(zarucofloat);
            all_yaw.add(yawarucofloat);

            int checksize = 5;

            if (all_z.size()==checksize) {//checksize = 5 but therer are actually 6 elements in the arrays

                float sumx = 0, sumy = 0, sumz = 0, sumyaw = 0, meanx = 0, meany = 0, meanz = 0, meanyaw = 0;

                for (int j = 0; j < checksize; j++) {

                    que = all_yaw.get(j);
                    sumz += all_z.get(j);
                    sumy += ally.get(j);
                    sumx += allx.get(j);
                    sumyaw += all_yaw.get(j);
                }
                meanx = sumx / allx.size();
                meany = sumy / ally.size();
                meanz = sumz / all_z.size();
                meanyaw = sumyaw / all_yaw.size();

                //Taking error out of the y-axis
                double fixy = meany - (meanz * 7.4 / 100);
                meany = (float) fixy;
                int yawhowmany = all_yaw.size();


                if (abs(meanx)<0.65){
                    fixz = (meanz-.03) ;
                }
                else{
                    fixz = (meanz-.03) - (abs(meanx)*0.09);
                }


//                showToast(" cuanto: " +sumyaw+" cauntas: " +yawhowmany+" X: " + String.format("%.3f", (meanx)) +
//                        "   Y: " + String.format("%.3f", (meany)) + "   Z: " + String.format("%.3f", (meanz)) + "  fixz:" + String.format("%.3f", (fixz)));

                allx.clear();
                ally.clear();
                all_z.clear();
                all_yaw.clear();

            }

            TextView theTextView1 = (TextView) findViewById(R.id.textView1);
            TextView theTextView2 = (TextView) findViewById(R.id.textView2);
            TextView theTextView3 = (TextView) findViewById(R.id.textView3);
            TextView theTextView4 = (TextView) findViewById(R.id.textView4);
            TextView theTextView5 = (TextView) findViewById(R.id.textView5);
            TextView theTextView6 = (TextView) findViewById(R.id.textView6);
            theTextView1.setText("X: " + String.format("%.3f", arucotranslationvector[0])  + " ,  ");
            theTextView2.setText("Y: " + String.format("%.3f", -arucotranslationvector[1])  + " ,  ");
            theTextView3.setText("Z: " + String.format("%.3f", arucotranslationvector[2])  + " ,  FixZ"+String.format("%.3f", fixz));
            theTextView4.setText("Yaw: " + String.format("%.3f", arucoyaw)  + " ,  ");
            theTextView5.setText("Roll: " + String.format("%.3f", arucoroll)  + " ,  ");
            theTextView6.setText("Pitch: " + String.format("%.3f", arucopitch)  + " ,  ");
            theTextView1.setTextColor(Color.BLUE);
            theTextView2.setTextColor(Color.BLUE);
            theTextView3.setTextColor(Color.BLUE);
            theTextView4.setTextColor(Color.BLUE);
            theTextView5.setTextColor(Color.BLUE);
            theTextView6.setTextColor(Color.BLUE);

        }
        else{
            TextView theTextView1 = (TextView) findViewById(R.id.textView1);
            TextView theTextView2 = (TextView) findViewById(R.id.textView2);
            TextView theTextView3 = (TextView) findViewById(R.id.textView3);
            TextView theTextView4 = (TextView) findViewById(R.id.textView4);
            TextView theTextView5 = (TextView) findViewById(R.id.textView5);
            TextView theTextView6 = (TextView) findViewById(R.id.textView6);
            theTextView1.setText("X: null");
            theTextView2.setText("Y: null");
            theTextView3.setText("Z: null");
            theTextView4.setText("Yaw: null");
            theTextView5.setText("Roll: null");
            theTextView6.setText("Pitch: null");
            theTextView1.setTextColor(Color.BLUE);
            theTextView2.setTextColor(Color.BLUE);
            theTextView3.setTextColor(Color.BLUE);
            theTextView4.setTextColor(Color.BLUE);
            theTextView5.setTextColor(Color.BLUE);
            theTextView6.setTextColor(Color.BLUE);
            emg_now = true;
        }
        //Bitmap DisplayBitmap = Bitmap.createBitmap(RGBmatFromBitmap.cols(),RGBmatFromBitmap.rows(), Bitmap.Config.ARGB_8888);
        Bitmap DisplayBitmap = Bitmap.createBitmap(1280,960, Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(RGBmatFromBitmap, DisplayBitmap);
        mImageSurface.setImageBitmap(null);
        mImageSurface.setImageBitmap(DisplayBitmap);
    }

    public void Segment1(double right_left_gap, double front_back_gap, double up_down_gap){

        EnableVirtualStick.performClick();
        flightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
        flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
        flightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
        flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);

        if(right_left_gap>0.8 || front_back_gap>1.5 || up_down_gap>1.4 ){
//          double front_back_gap_goback = front_back_gap;
            front_back_gap -= 1.2; //0.8 meter in front of aruco
            up_down_gap += 0.9;

            //Getting distance for first approach (1m stand off)
            double distance = sqrt(right_left_gap*right_left_gap + front_back_gap*front_back_gap + up_down_gap*up_down_gap);
            float higher_speed = (float) max(abs(right_left_gap),abs(front_back_gap));

            //Getting GoBack parametters
//                double distance_goback = sqrt(right_left_gap*right_left_gap + front_back_gap_goback*front_back_gap_goback);
//                float higher_speed_goback = (float) max(abs(right_left_gap),abs(front_back_gap_goback));
//                double roll_goback = front_back_gap_goback / (higher_speed_goback*2);
//                double pitch_goback = right_left_gap / (higher_speed_goback*2);
//                double flying_time_goback = distance_goback / sqrt(roll_goback*roll_goback + pitch_goback*pitch_goback);
//                back_first_fly_time = flying_time_goback*1000;
//                back_pitch= pitch_goback;
//                back_roll= roll_goback;
//                showToast(String.format("xxxx: %f, yyyyy: %f",back_roll,back_pitch));
//                back_throttle= 0;

//                if(higher_speed>){
//                    roll = (float) front_back_gap/higher_speed;     //forward +  backwards -   MAX 15 From 8, overshoot
//                    throttle = (float) up_down_gap/higher_speed;    //up      +  down      -   MAX 4 From 3, overshoot
//                    pitch = (float) right_left_gap/higher_speed;    //right   +  left      -   MAX = 15    From 8, starts to overshoot
//
//
//                }else{
//                    double basic_speed = 0.6;
//                    roll =  (float) (basic_speed * front_back_gap);
//                    throttle = (float)(basic_speed * up_down_gap);
//                    pitch = (float)(basic_speed * right_left_gap);
//                }
            double basic_speed = 0.8;
            roll =  (float) (basic_speed * front_back_gap);
            throttle = (float)(basic_speed * up_down_gap);
            pitch = (float)(basic_speed * right_left_gap);
            //reduce speed
            roll /= 2;
            throttle /= 2;
            pitch /= 2;

            //  Values to get the GetBack VELOCITY
            seg1_dist[0]=right_left_gap;
            seg1_dist[1]=up_down_gap;
            seg1_dist[2]=front_back_gap;

            double flying_time = distance /sqrt(roll*roll + pitch*pitch + throttle*throttle);
            double first_fly_time= flying_time*1000;
            totalflytime = first_fly_time;
            showToast(String.format("X: %.3f,  Y: %.3f,  Z: %.3f,  T: %.3f",roll,pitch,throttle,flying_time));
            Log.i("flying",String.format("x: %f, y: %f, z: %ff",right_left_gap,front_back_gap,up_down_gap));
            Log.i("flying",String.format("forward: %f, horizontal: %f, up: %f, fly time: %f",roll,pitch,throttle,flying_time));
            if (flying_time > 10) {
                setZero();
            } else {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setZero();
                        Wait();
                    }
                }, (long) first_fly_time);
            }
        }
        else{
            setZero();
            Wait();
            showToast("skipped s1");
        }

    }
    public void Wait(){
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                //Do something here
                Segment2(arucotranslationvector[0],fixz,-arucotranslationvector[1]);
                //Segment2(arucotranslationvector[0],fixz,-arucotranslationvector[1]);
            }
        }, 2000);

    }

    public void Segment2(double right_left_gap, double front_back_gap, double up_down_gap){
        flightController.setVirtualStickModeEnabled(true, djiError -> {
            flightController.setVirtualStickAdvancedModeEnabled(true);

            if (djiError != null) {
                ToastUtils.setResultToToast(djiError.getDescription());
            }
        });
        flightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
        flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
        flightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
        flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);

        if(right_left_gap<0.9 && front_back_gap<1.4 && up_down_gap<1.2 ) {

//            double front_back_gap_goback = front_back_gap;
            front_back_gap -= 0.1; //0.3 meter in front of the marker
            up_down_gap +=0.75; // 0.85m above the marker's center is the catch mechanism
            // side= 0.318   forw = 0.549   down= 0.656

            //Getting distance for first approach (1m stand off)
            double distance = sqrt(right_left_gap * right_left_gap + front_back_gap * front_back_gap + up_down_gap*up_down_gap);
            float higher_speed = (float) max(abs(up_down_gap), abs(front_back_gap));

            //Getting GoBack parametters
//            double distance_goback = sqrt(right_left_gap * right_left_gap + front_back_gap_goback * front_back_gap_goback);
//            float higher_speed_goback = (float) max(abs(right_left_gap), abs(front_back_gap_goback));
//            double roll_goback = front_back_gap_goback / (higher_speed_goback * 2);
//            double pitch_goback = right_left_gap / (higher_speed_goback * 2);
//            double flying_time_goback = distance_goback / sqrt(roll_goback * roll_goback + pitch_goback * pitch_goback);
//            back_first_fly_time = flying_time_goback * 1000;
//            back_pitch = pitch_goback;
//            back_roll = roll_goback;
//            showToast(String.format("xxxx: %f, yyyyy: %f", back_roll, back_pitch));
//            back_throttle = 0;

            double basic_speed = 0.6;
            roll =  (float) (basic_speed * front_back_gap);
            throttle = (float)(basic_speed * up_down_gap);
            pitch = (float)(basic_speed * right_left_gap);
            TextView theTextView7  = (TextView) findViewById(R.id.textView7);
            theTextView7.setText(" up_down_gap = " + String.format("%.2f",up_down_gap));
            theTextView7.setTextColor(Color.RED);

            //reduce speed
            roll /= 2;
            throttle /= 2;
            pitch /= 2;

            //  Values to get the GetBack VELOCITY
            seg1_dist[0]+=right_left_gap;
            seg1_dist[1]+=up_down_gap;
            seg1_dist[2]+=front_back_gap;

            seg2_dist[0]=pitch;
            seg2_dist[1]=throttle;
            seg2_dist[2]=roll;
            double flying_time = distance / sqrt(roll * roll + pitch * pitch + throttle*throttle);
            double first_fly_time = flying_time * 1000;
            totalflytime += first_fly_time;
            showToast(String.format("X: %.3f,  Y: %.3f,  Z: %.3f,  T: %.3f",roll,pitch,throttle,flying_time));
            Log.i("flying", String.format("x: %f, y: %f, z: %ff", right_left_gap, front_back_gap, up_down_gap));
            Log.i("flying", String.format("forward: %f, horizontal: %f, up: %f, fly time: %f", roll, pitch, throttle, flying_time));

            if (flying_time > 7) {
                setZero();
            } else {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setZero();
                        showToast("AFter S2");
                        GoForwardSequence();
                    }
                }, (long) first_fly_time);
            }
        }
        else{
            showToast("S2 evaded");
        }
    }


    public void GoForwardSequence() {
        EnableVirtualStick.performClick();
        roll=(float).75;
        throttle = (float)0.5;
        double time_s= 1000;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                setZero();
                BackafterS2();
                //GoBackNew();
            }
        }, (long) (time_s));

        double zdist= roll*time_s;
        double ydist= throttle*time_s;
        seg3_dist[1] = ydist;
        seg3_dist[2]=  zdist;

        seg1_dist[1]+=ydist;
        seg1_dist[2]+=zdist;
        totalflytime += time_s;
    }
    public void BackafterS2() {
        EnableVirtualStick.performClick();
        roll=-(float)0.7;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                setZero();
                showToast("Backward");
                LandBtn.performClick();
            }
        }, (long) (3000));


//        roll = (float) (-1);
//        Handler handler = new Handler();
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                setZero();
//                showToast("Backward");
//            }
//        },(long) 2000 );

    }

    public void GoBackNew() {
        String data = String.format("X: %.3f,  Y: %.3f,  Z: %.3f,  T: %.3f",seg1_dist[0],seg1_dist[1],seg1_dist[2],totalflytime);
//        data.concat(String.format("\n   X: %.3f,  Y: %.3f,  Z: %.3f,  T: %.3f",seg2_dist[0],seg2_dist[1],seg2_dist[2],totalflytime));
//        data.concat(String.format(  "\n     X: %.3f,  Y: %.3f,  Z: %.3f,  T: %.3f",seg3_dist[0],seg3_dist[1],seg3_dist[2],totalflytime+"\n"));

        double totxdist = seg1_dist[0];
        double totydist = seg1_dist[1];
        double totzdist = seg1_dist[2];
        showToast("x: "+totxdist+"  y: "+totydist+" z: "+totzdist+"  t: "+totalflytime);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                setZero();
                writeToFile(data,OldFeederView.this);

            }
        }, (long) (2000));


    }

    public void GoBackSequence() {
        pitch = (float) -back_pitch;
        roll = (float) -back_roll;
        Handler bhandler = new Handler();
        bhandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                setZero();
                showToast( "p "+back_pitch+" r "+back_roll+ "  t "+ back_first_fly_time);
                SetDown(1,1);
            }
        },  (long)back_first_fly_time);
    }

    public void TwoDAruco(double right_left_gap, double front_back_gap, double up_down_gap, double yaw) {
        flightController.setVirtualStickModeEnabled(true, djiError -> {
            flightController.setVirtualStickAdvancedModeEnabled(true);
            if (djiError != null) {
                ToastUtils.setResultToToast(djiError.getDescription());
            }
        });
        flightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
        flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
        flightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
        flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);



        double front_back_gap_goback = front_back_gap;
        front_back_gap -= 1; //1 meter in front of aruco

        //up_down_gap -=1;

        //Getting distance for first approach (1m stand off)
        double distance = sqrt(right_left_gap*right_left_gap + front_back_gap*front_back_gap);
        float higher_speed = (float) max(abs(right_left_gap),abs(front_back_gap));

        //Getting GoBack parametters
        double distance_goback = sqrt(right_left_gap*right_left_gap + front_back_gap_goback*front_back_gap_goback);
        float higher_speed_goback = (float) max(abs(right_left_gap),abs(front_back_gap_goback));
        double roll_goback = front_back_gap_goback / (higher_speed_goback*2);
        double pitch_goback = right_left_gap / (higher_speed_goback*2);
        double flying_time_goback = distance_goback / sqrt(roll_goback*roll_goback + pitch_goback*pitch_goback);
        back_first_fly_time = flying_time_goback*1000;
        back_pitch= pitch_goback;
        back_roll= roll_goback;
        showToast(String.format("xxxx: %f, yyyyy: %f",back_roll,back_pitch));
        back_throttle= 0;

        if(abs(right_left_gap) <0.1 && abs(front_back_gap)<0.1) {

        } //if the distance is too small, don't move
        if(higher_speed>1){
            roll = (float) front_back_gap/higher_speed;     //forward +  backwards -   MAX 15 From 8, overshoot
            throttle = (float) up_down_gap/higher_speed;    //up      +  down      -   MAX 4 From 3, overshoot
            pitch = (float) right_left_gap/higher_speed;    //right   +  left      -   MAX = 15    From 8, starts to overshoot


        }else{
            float basic_speed = 1;
            roll =  basic_speed *(float) front_back_gap;
            throttle = basic_speed *(float) up_down_gap;
            pitch = basic_speed *(float) right_left_gap;
        }

        //reduce speed
        roll /= 2;
        throttle /= 2;
        pitch /= 2;

        showToast(String.format("x: %f, y: %f",roll,pitch));

        double flying_time = distance /sqrt(roll*roll + pitch*pitch);
        double first_fly_time= flying_time*1000;

        Log.i("flying",String.format("x: %f, y: %f, z: %ff",right_left_gap,front_back_gap,up_down_gap));
        Log.i("flying",String.format("forward: %f, horizontal: %f, up: %f, fly time: %f",roll,pitch,throttle,flying_time));

        if (flying_time > 10) {
            setZero();
        } else {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    setZero();
                    GoForwardSequence();
                }
            }, (long) first_fly_time);
        }
    }

    public void ContinousApproach (double right_left_gap, double front_back_gap, double up_down_gap) {


        int x = 0,y=0,z=0,count=0,maxattempt;
        maxattempt = 5;
        while(true) {
            try {
                // Some Code
                // break out of loop, or return, on success
                if(right_left_gap>1){
                    pitch = (float) 0.5;
                    showToast("positive gap     pitch"+pitch);
                }
                if(right_left_gap<-1) {
                    pitch = (float) -0.5;
                    showToast("negative gap     pitch" + pitch);
                }
                if(-1<right_left_gap && right_left_gap < 1){
                    pitch = 0;
                    x = 1;
                    showToast("negative gap     pitch" + pitch);
                }
//        if(abs(front_back_gap)>1){
//            roll = (float) 0.5;
//        }
//        else{
//            roll = 0;
//            y = 1;
//        }
//        if(up_down_gap<-1){
//            throttle = (float) -0.5;
//        }
//        else{
//            throttle = 0;
//            z = 1;
//        }
                if(x*y*z==0){

                }
                else {
//                   as

                }



            } catch (Exception e) {
                // handle exception
                if (++count >= maxattempt) throw e;
            }
        }


    }



    public void SetUp(int speed, int delayms) {
        throttle = (float) (speed);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                setZero();
                showToast("Up");

            }
        }, (long) delayms);
    }

    public void SetDown(int speed, int delayms) {
        throttle = (float) (speed);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                setZero();
                showToast("Down");
            }
        }, (long) delayms);
    }
    public void SetYaw(int ang_speed, int delay_ms) {
        yaw = (float) (ang_speed);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                setZero();
                showToast("+ Yaw");
            }
        }, (long) delay_ms);
    }
    public void SetForward(double speed, int delay_ms) {
        roll = (float) (speed);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                setZero();
                showToast("Forward");
            }
        },(long) delay_ms );
    }
    public void SetBackward(int speed, int delay_ms) {
        roll = (float) (-speed);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                setZero();
                showToast("Backward");
            }
        },(long) delay_ms );
    }
    public void SetRight(int speed, int delay_ms) {
        pitch = (float) (speed);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                setZero();
                showToast("Right");
            }
        },(long) delay_ms );
    }
    public void SetLeft(double speed, int delay_ms) {
        pitch = (float) (-speed);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                setZero();
                showToast("Left");
            }
        },(long) delay_ms );
    }

    public void setZero(){
        pitch = (float)0.0;
        roll = (float) 0.0;
        throttle = (float)0.0;
        yaw = (float)(0.0);
        showToast("STOP");

    }
    private void writeToFile(String data, Context context) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("config.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    public void SetLEDs(int t){
        flightController.setLEDsEnabledSettings(LEDsSettings.generateLEDsEnabledSettings(t), new CommonCallbacks.CompletionCallback() {
            // Legend
            // 0 all off -  1 rear solid on  -  2 front flash, rear solid
            @Override
            public void onResult(DJIError djiError) {
                if (djiError != null) {
                    ToastUtils.setResultToToast(djiError.getDescription());
                } else {
                    showToast("LIghts have changed");
                }
            }
        });
    }

    private class SendVirtualStickDataTask extends TimerTask {
        @Override
        public void run() {
            if (flightController != null) {
                //接口写反了，setPitch()应该传入roll值，setRoll()应该传入pitch值
                flightController.sendVirtualStickFlightControlData(new FlightControlData(pitch, roll, yaw, throttle), new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            ToastUtils.setResultToToast(djiError.getDescription());
                        }
                    }
                });
            }
        }
    }

    private void dronestart() {
        pY = 0;
        pX = 0;
        float verticalJoyControlMaxSpeed = 1;
        throttle = (float)(verticalJoyControlMaxSpeed * pY);
        float horizontalJoyControlMaxSpeed = 1;
        pitch = (float)(verticalJoyControlMaxSpeed * pX);

        if (null == mSendVirtualStickDataTimer) {
            mSendVirtualStickDataTask = new SendVirtualStickDataTask();
            mSendVirtualStickDataTimer = new Timer();
            mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 0, 200);
        }
    }


    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        if (null != mSendVirtualStickDataTimer) {
            mSendVirtualStickDataTask.cancel();
            mSendVirtualStickDataTask = null;
            mSendVirtualStickDataTimer.cancel();
            mSendVirtualStickDataTimer.purge();
            mSendVirtualStickDataTimer = null;
        }
        setZero();
        DisableVirtualStick.performClick();
        super.onDestroy();

    }

}
