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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
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
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

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
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.FlightAssistant;
import dji.sdk.flightcontroller.FlightController;

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
    private boolean emg_now = false;
    double fixz;
    double[] seg1_dist ={0,0,0};
    double[] seg2_dist ={0,0,0};
    double[] seg3_dist ={0,0,0};
    double totalflytime=0;
    private final FlightControlMethod flight = new FlightControlMethod();
    Thread flight_thread = null;

//--------Camera
    private Button mCaptureBtn;

//--------Video Feed
    protected TextureView mVideoTexture = null;
    protected ImageView mImageSurface;
    private Bitmap BitmapFromFeedersSurface;
    private Mat RGBmatFromBitmap;
    private MatOfInt ids;
    private Dictionary dictionary;
    private DetectorParameters parameters;
//--------Aruco variables
    private final ArrayList<Point3> aruco_coordinate_buffer = new ArrayList<>(Collections.nCopies(10, null));
    private double[] aruco_coordinates = {0,0,0};//todo : change aruco_translation_vector to aruco_coordinates in this file
    private List<ArucoCoordinate> current_arucos = new ArrayList<>();
//--------
    ArrayList<Float> allx = new ArrayList<>();
    ArrayList<Float> ally = new ArrayList<>();
    ArrayList<Float> all_z = new ArrayList<>();
    ArrayList<Float> all_roll = new ArrayList<>();
    ArrayList<Float> all_pitch = new ArrayList<>();
    ArrayList<Float> all_yaw = new ArrayList<>();
    //Todo : 刪除無用變數
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
    double[] aruco_translation_vector;
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
        // We recommend you use the below settings, a standard american hand style.
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

        // Turn off the avoidance system
        flightAssistant.setLandingProtectionEnabled(true,null);
        flightAssistant.setCollisionAvoidanceEnabled(true, null);


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_old_feeder_view);
        initUI();
        initParams();
        dronestart();
        FlightController flightController = ModuleVerificationUtil.getFlightController();
        flight.register(flightController);
        flight.register(current_arucos);
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
        /* -------------------------------------------------------------------------- */
        /*                               Button Function                              */
        /* -------------------------------------------------------------------------- */
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
                flight.emergency();
                saveImageToExternalStorage(BitmapFromFeedersSurface);
                if(flight.emg_now){
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
                flight_thread = new Thread(()->{
                    // 往需提取物品的方向移動
                    ArucoCoordinate goal = flight.findAruco(23);
                    if(goal == null) return;
                    flight.moveTo(goal.x,goal.z -1.2,-goal.y + 0.9);
                    // 再靠近一點
                    SystemClock.sleep(2000); // wait for the Aruco detection
                    goal = flight.findAruco(23);
                    if(goal == null) return;
                    flight.moveTo(goal.x,goal.z - 0.5,-goal.y + 0.75);
                    //往前往上吊起物品
                    flight.moveTo(0,0.5,0);
                    flight.moveTo(0,0,0.5);
                    //往終點移動
                    goal = flight.findAruco(15);
                    if(goal == null) return;
                    flight.moveTo(goal.x,goal.z - 1.2,-goal.y + 1);
                    flightController.startLanding(djiError -> Log.i(TAG, "Landing: " + djiError));
                });
                flight_thread.start();
            }
        });

        MoveTo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flight_thread = new Thread(()->{
                    flight.goToArucoMarker(23);
                });
                flight_thread.start();
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
        File myDir = new File(root + "/CV_drone");
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
        //set shadow color of the emergency button
        if(flight.emg_now){
            EmergencyBtn.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
        }
        else{
            EmergencyBtn.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
        }
        aruco_coordinates=ArucoDetector();
        if(!current_arucos.isEmpty()){
            ArucoCoordinate aruco = current_arucos.get(0);
            showScreenText(aruco.x,aruco.y,aruco.z,aruco.yaw,aruco.pitch,aruco.roll);
        }
        else{
            showScreenText(null,null,null,null,null,null);
        }
    }

    void showScreenText(Float x, Float y, Float z, Float yaw, Float pitch, Float roll){

        TextView theTextView1 = (TextView) findViewById(R.id.textView1);
        TextView theTextView2 = (TextView) findViewById(R.id.textView2);
        TextView theTextView3 = (TextView) findViewById(R.id.textView3);
        TextView theTextView4 = (TextView) findViewById(R.id.textView4);
        TextView theTextView5 = (TextView) findViewById(R.id.textView5);
        TextView theTextView6 = (TextView) findViewById(R.id.textView6);
        theTextView1.setText("X: " + x);
        theTextView2.setText("Y: " + y);
        theTextView3.setText("Z: " + z);
        theTextView6.setText("Roll: " + roll);
        theTextView5.setText("Pitch: " + pitch);
        theTextView4.setText("Yaw: " + yaw);
        theTextView1.setTextColor(Color.BLUE);
        theTextView2.setTextColor(Color.BLUE);
        theTextView3.setTextColor(Color.BLUE);
        theTextView4.setTextColor(Color.BLUE);
        theTextView5.setTextColor(Color.BLUE);
        theTextView6.setTextColor(Color.BLUE);

    }

    private double[] ArucoDetector() {

        int pic_width = 1280;
        int pic_height = 960;
        float MarkerSizeinm = (float) 0.182;  //A4 paper
        //float MarkerSizeinm = (float) 0.282;  //A3 paper

        double tall = 0, with = 0;
        tall = mCodecManager.getVideoHeight();
        with = mCodecManager.getVideoWidth();
        //showToast("height= "+tall+"    width= "+with);

        List<Mat> corners = new ArrayList();
        corners.clear();
        Mat droneImage = new Mat();
        Mat grayImage = new Mat();
        MatOfInt ids = new MatOfInt();
        DetectorParameters parameters = DetectorParameters.create();
        parameters.set_cornerRefinementMethod(1);
        parameters.set_cornerRefinementMinAccuracy(0.05);
        parameters.set_cornerRefinementWinSize(5);
        dictionary = Aruco.getPredefinedDictionary(Aruco.DICT_6X6_50); //MARKER NUMBER 23
        //dictionary = Aruco.getPredefinedDictionary(Aruco.DICT_4X4_50); //MARKER NUMBER 23
        BitmapFromFeedersSurface = Bitmap.createScaledBitmap(mVideoTexture.getBitmap(), pic_width, pic_height, true);
        //BitmapFromFeedersSurface = mVideoTexture.getBitmap();
        //showToast(("Width"+BitmapFromFeedersSurface.getWidth())+"   Height"+(BitmapFromFeedersSurface.getHeight()));
        RGBmatFromBitmap = new Mat();

        Utils.bitmapToMat(BitmapFromFeedersSurface, droneImage);
        Imgproc.cvtColor(droneImage, grayImage, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(droneImage, RGBmatFromBitmap, Imgproc.COLOR_RGBA2RGB);
        Aruco.detectMarkers(grayImage, dictionary, corners, ids, parameters);

        if (corners.size() > 0) {
            //Draw lines at center of the image
            //Vertical line
            Point startverlin = new Point(pic_width / 2, 0);
            Point endverlin = new Point(pic_width / 2, pic_height);
            Scalar colorlin = new Scalar(255, 0, 0);
            int thickness = 3;
            Imgproc.line(RGBmatFromBitmap, startverlin, endverlin, colorlin, thickness);

            //Horizontal line
            Point starthorlin = new Point(0, pic_height / 2);
            Point endhorlin = new Point(pic_width, pic_height / 2);
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
            cameraMatrix.put(0, 0, 577.12265401 - 30); //fx
            cameraMatrix.put(1, 1, 577.12265401 - 30); //fy
            cameraMatrix.put(0, 2, 624.27619131); //cx
            cameraMatrix.put(1, 2, 493.79682551); //cy
            cameraMatrix.put(2, 2, 1);
//            showToast(cameraMatrix.toString());

            // Distorsion coefficients
            Mat distCoeffs = Mat.zeros(8, 1, CvType.CV_64F);
            distCoeffs.put(0, 0, -1.00067366e-01);
            distCoeffs.put(1, 0, -4.24388662e-02);
            distCoeffs.put(2, 0, 5.06785331e-05);
            distCoeffs.put(3, 0, -2.77194518e-03);
            distCoeffs.put(4, 0, 3.90751297e-01);
            distCoeffs.put(5, 0, 1.76991593e-02);
            distCoeffs.put(6, 0, -3.57610859e-02);
            distCoeffs.put(7, 0, 4.21848915e-01);

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
            MatOfPoint3f objPoints = new MatOfPoint3f(new Point3(-MarkerSizeinm / 2, MarkerSizeinm / 2, 0), new Point3(MarkerSizeinm / 2, MarkerSizeinm / 2, 0),
                    new Point3(-MarkerSizeinm / 2, -MarkerSizeinm / 2, 0), new Point3(MarkerSizeinm / 2, -MarkerSizeinm / 2, 0));

            //Pose Estimation
            Aruco.drawDetectedMarkers(RGBmatFromBitmap, corners, ids);

            //Corners values with format (y,x)
            double[] tl = (corners.get(0).get(0, 0));
            double[] tr = corners.get(0).get(0, 1);
            double[] br = corners.get(0).get(0, 2);
            double[] bl = corners.get(0).get(0, 3);

            //AVG = 0.1715M
            Aruco.estimatePoseSingleMarkers(corners, MarkerSizeinm, cameraMatrix, distCoeffs, rvecs, tvecs);

            //REAL WORLD CORNERS IN MM
            List<Point3> corners4 = new ArrayList<>(4);
            corners4.add(new Point3(-MarkerSizeinm / 2, MarkerSizeinm / 2, 0));        // Top-Left
            corners4.add(new Point3(MarkerSizeinm / 2, MarkerSizeinm / 2, 0));        // Top-Right
            corners4.add(new Point3(MarkerSizeinm / 2, -MarkerSizeinm / 2, 0));        // Bottom-Right
            corners4.add(new Point3(-MarkerSizeinm / 2, -MarkerSizeinm / 2, 0));        // Bottom-Left


            MatOfPoint3f mcorners = new MatOfPoint3f();
            mcorners.fromList(corners4);
            current_arucos.clear(); //reset the list of arucos
//            List<ArucoCoordinate> tempAruco = new ArrayList<>(); //暫時存放aruco的資料
            for (int i = 0; i < ids.toArray().length; i++) {
                Calib3d.drawFrameAxes(RGBmatFromBitmap, cameraMatrix, distCoeffs, rvecs.row(i), tvecs.row(i), 0.13f);
                Mat aruco_rotation_vec = new Mat(3, 3, 6);
                Calib3d.Rodrigues(rvecs.row(i), aruco_rotation_vec);
                Mat cameraMatrixAruco = new Mat();
                Mat rotMatrixAru = new Mat();
                Mat transVectAru = new Mat();
                Mat ArucoeulerAngles = new Mat();
                Mat rotMatrixX22 = new Mat();
                Mat rotMatrixY22 = new Mat();
                Mat rotMatrixZ22 = new Mat();
                Mat projMatrix22 = new Mat();
                Mat RT = Mat.zeros(3, 4, CvType.CV_64F);

                RT.put(0, 0, aruco_rotation_vec.get(0, 0)[0]);
                RT.put(0, 1, aruco_rotation_vec.get(0, 0)[0]);
                RT.put(0, 2, aruco_rotation_vec.get(0, 2)[0]);
                RT.put(0, 3, tvecs.get(i, 0)[0]);
                RT.put(1, 0, aruco_rotation_vec.get(1, 0)[0]);
                RT.put(1, 1, aruco_rotation_vec.get(1, 1)[0]);
                RT.put(1, 2, aruco_rotation_vec.get(1, 2)[0]);
                RT.put(1, 3, tvecs.get(i, 0)[1]);
                RT.put(2, 0, aruco_rotation_vec.get(2, 0)[0]);
                RT.put(2, 1, aruco_rotation_vec.get(2, 1)[0]);
                RT.put(2, 2, aruco_rotation_vec.get(2, 2)[0]);
                RT.put(2, 3, tvecs.get(i, 0)[2]);

                Core.gemm(cameraMatrix, RT, 1, new Mat(), 0, projMatrix22, 0);

                Calib3d.decomposeProjectionMatrix(projMatrix22, cameraMatrixAruco, rotMatrixAru, transVectAru, rotMatrixX22, rotMatrixY22, rotMatrixZ22, ArucoeulerAngles);

                aruco_translation_vector = tvecs.get(i, 0); //for debugging, printing on screen
                arucoroll = ArucoeulerAngles.get(0, 0)[0];  //for debugging, printing on screen
                arucopitch = ArucoeulerAngles.get(1, 0)[0];
                arucoyaw = -ArucoeulerAngles.get(2, 0)[0];// change sign to get the rotation needed by the drone not the paper
                //Add the aruco to the currently detected aruco
                /*
                * todo: check why need change z-axis distance
                * Correct the z-axis distance
                */
                if (abs(aruco_translation_vector[0]) < 0.65) {
                    aruco_translation_vector[2] = (aruco_translation_vector[2] - .03);
                } else {
                    aruco_translation_vector[2] = (aruco_translation_vector[2] - .03) - (abs(aruco_translation_vector[0]) * 0.09);
                }

                current_arucos.add(new ArucoCoordinate((float) aruco_translation_vector[0], (float) aruco_translation_vector[1], (float) aruco_translation_vector[2], (float) arucoroll, (float) arucopitch, (float) arucoyaw,(int) ids.get(i, 0)[0]));

                distCoeffs = new MatOfDouble(distCoeffs);

                MatOfPoint2f projected = new MatOfPoint2f();

                Calib3d.projectPoints(mcorners, rvecs.row(i), tvecs.row(i), cameraMatrix, (MatOfDouble) distCoeffs, projected);

                Point[] points = projected.toArray();

                if (points != null) {
                    //Draw the circle of the aruco corners
                    for (Point point : points) {
                        Imgproc.circle(RGBmatFromBitmap, points[0], 10, new Scalar(255, 0, 0), 4);
                        Imgproc.circle(RGBmatFromBitmap, points[1], 10, new Scalar(0, 0, 0), 4);
                        Imgproc.circle(RGBmatFromBitmap, points[2], 10, new Scalar(0, 255, 0, 150), 4);
                        Imgproc.circle(RGBmatFromBitmap, points[3], 10, new Scalar(0, 0, 255), 4);

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
            zarucofloat = (float) aruco_translation_vector[2]; //the two value is the z axis through the camera
            yarucofloat = (float) -aruco_translation_vector[1]; //times -1 to make up distances positives
            xarucofloat = (float) aruco_translation_vector[0]; //the zero value is the x axis, side to side of the camera
            yawarucofloat = (float) arucoyaw;

            //Turn negatives angles into positives
            if (yawarucofloat < 0) {
                yawarucofloat = yawarucofloat + 360;
            }

            //arraylists to hold values
            allx.add(xarucofloat);
            ally.add(yarucofloat);
            all_z.add(zarucofloat);
            all_yaw.add(yawarucofloat);

            int checksize = 5;

            if (all_z.size() == checksize) {//checksize = 5 but therer are actually 6 elements in the arrays

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


                if (abs(meanx) < 0.65) {
                    fixz = (meanz - .03);
                } else {
                    fixz = (meanz - .03) - (abs(meanx) * 0.09);
                }
                allx.clear();
                ally.clear();
                all_z.clear();
                all_yaw.clear();

            }

        } else {
            aruco_coordinate_buffer.remove(0);
            aruco_coordinate_buffer.add(null);
            current_arucos.clear();
        }
        //todo : get the median of the aruco_coordinate_buffer
        List<Double> x_list =new ArrayList<Double>(), y_list = new ArrayList<Double>(), z_list = new ArrayList<Double>();
        for (int i = 0; i < aruco_coordinate_buffer.size(); i++) {
            if (aruco_coordinate_buffer.get(i) != null) {
                x_list.add(aruco_coordinate_buffer.get(i).x);
                y_list.add(aruco_coordinate_buffer.get(i).y);
                z_list.add(aruco_coordinate_buffer.get(i).z);
            }
        }
        //Bitmap DisplayBitmap = Bitmap.createBitmap(RGBmatFromBitmap.cols(),RGBmatFromBitmap.rows(), Bitmap.Config.ARGB_8888);
        Bitmap DisplayBitmap = Bitmap.createBitmap(1280, 960, Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(RGBmatFromBitmap, DisplayBitmap);
        mImageSurface.setImageBitmap(null);
        mImageSurface.setImageBitmap(DisplayBitmap);

        if (x_list.size() > 0) {
            return new double[]{median(x_list), median(y_list), median(z_list)};
        } else {
            emg_now = true;
            return null;
        }
    }
    private static Double median(List<Double> values) {
        Collections.sort(values);
        if (values.size() % 2 == 1)
            return values.get((values.size() + 1) / 2 - 1);
        else {
            Double lower = values.get(values.size() / 2 - 1);
            Double upper = values.get(values.size() / 2);
            return (lower + upper) / 2.0;
        }
    }

    //Todo : 移動到呼叫的地方
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
    public void setZero(){
        pitch = (float)0.0;
        roll = (float) 0.0;
        throttle = (float)0.0;
        yaw = (float)(0.0);
        showToast("STOP");

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
            pitch = flight.pitch;
            roll = flight.roll;
            throttle = flight.throttle;
            yaw = flight.yaw;
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

/**
 * all data of the aruco marker
 */
class ArucoCoordinate{
    float x,y,z,yaw,pitch,roll;
    int id;
    ArucoCoordinate(float x, float y, float z, float yaw, float pitch, float roll, int id){
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.roll = roll;
        this.id = id;
    }
    ArrayList<Float> x_list = new ArrayList<>(Collections.nCopies(10, null));
    ArrayList<Float> y_list = new ArrayList<>(Collections.nCopies(10, null));
    ArrayList<Float> z_list = new ArrayList<>(Collections.nCopies(10, null));
    ArrayList<Float> yaw_list = new ArrayList<>(Collections.nCopies(10, null));
    ArrayList<Float> pitch_list = new ArrayList<>(Collections.nCopies(10, null));
    ArrayList<Float> roll_list = new ArrayList<>(Collections.nCopies(10, null));
    /*
    update the coordinate of the aruco marker
     */
    public void updateAruco(float x, float y, float z, float yaw, float pitch, float roll){
        this.x =newListMedian(x_list,x);
        this.y =newListMedian(y_list,y);
        this.z =newListMedian(z_list,z);
        this.yaw =newListMedian(yaw_list,yaw);
        this.pitch =newListMedian(pitch_list,pitch);
        this.roll =newListMedian(roll_list,roll);
    }
    /*
    get the median of the data in the new list
     */
    private float newListMedian(ArrayList<Float> list, float data) {
        list.add(data);
        list.remove(0); //add and remove the data, make the new list always have 10 data
        //remove the null data
        list = new ArrayList<>(list); // copy the list
        list.removeAll(Collections.singleton(null));
        Collections.sort(list);
        if (list.size() % 2 == 0) {
            return (list.get(list.size() / 2) + list.get(list.size() / 2 - 1)) / 2;
        } else {
            return list.get(list.size() / 2);
        }
    }

}