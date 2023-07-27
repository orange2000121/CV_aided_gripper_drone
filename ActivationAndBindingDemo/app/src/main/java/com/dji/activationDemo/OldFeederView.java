package com.dji.activationDemo;

import static com.dji.sdk.sample.internal.utils.ToastUtils.showToast;

import com.dji.sdk.sample.internal.utils.ModuleVerificationUtil;
import com.dji.sdk.sample.internal.utils.ToastUtils;

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
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.dji.activationDemo.payload.PayloadDataTransmission;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
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
    private static final String TAG = OldFeederView.class.getName();
    private PayloadDataTransmission payload = new PayloadDataTransmission(OldFeederView.this);
    protected VideoFeeder.VideoDataListener mReceivedVideoDataListener = null;
    protected DJICodecManager mCodecManager = null;

//--------Flight Controller
    private FlightController flightController= null;
    private FlightAssistant flightAssistant = null;
    private Button TurnOnMotorsBtn,TurnOffMotorsBtn, TakeOffBtn, LandBtn;
    private Button EmergencyBtn, ForwardBtn, YawBtn,BackwardsBtn, RightBtn ,LeftBtn,UpBtn, DownBtn;
    private Button mBtnStart,ArucoBtn, MoveTo;
    private Timer mSendVirtualStickDataTimer;
    private SendVirtualStickDataTask mSendVirtualStickDataTask;
    private final FlightControlMethod flight = new FlightControlMethod();
    private Switch SwitchVirtualStick;
    Thread flight_thread = null;

//--------Camera
    private Button mCaptureBtn;

//--------Payload
//    private PayloadDataTransmission payload = new PayloadDataTransmission();

//--------Video Feed
    protected TextureView mVideoTexture = null;
    protected ImageView mImageSurface;
    private Bitmap BitmapFromFeedersSurface;
//--------Aruco variables
//    private final ArrayList<Point3> aruco_coordinate_buffer = new ArrayList<>(Collections.nCopies(10, null));
//    private double[] aruco_coordinates = {0,0,0};//todo : change aruco_translation_vector to aruco_coordinates in this file
//    private List<ArucoCoordinate> current_arucos = new ArrayList<>();
    private final ArucoMethod arucoMethod = new ArucoMethod();
//--------
    double pX = 0, pY = 0;
    private float pitch = 0;
    private float roll = 0;
    private float yaw = 0;
    private float throttle = 0;

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
                assert flightAssistant != null;
                //todo: 完成測量到地面距離
//                flightAssistant.setVisualPerceptionInformationCallback(new CommonCallbacks.CompletionCallbackWith<PerceptionInformation>() {
//                    @Override
//                    public void onSuccess(PerceptionInformation perceptionInformation) {
//                        int[] distances = perceptionInformation.getDistances();
//                        int downwardObstacleDistance = perceptionInformation.getDownwardObstacleDistance();
//                        int upwardObstacleDistance = perceptionInformation.getUpwardObstacleDistance();
//                        //compute mean of distances from 350 to 10 degrees
//                        int backward_distance = 0;
//                        for (int i = 175; i < 185; i++) {
//                            backward_distance += distances[i];
//                        }
//                        backward_distance /= 10;
////                        Log.i(TAG, "onSuccess: downwardObstacleDistance: " + downwardObstacleDistance);
////                        Log.i(TAG, "onSuccess: upwardObstacleDistance: " + upwardObstacleDistance);
////                        Log.i(TAG, "onSuccess: forward distances: " + distances[0]);
////                        Log.i(TAG, "onSuccess: right distances: " + distances[90]);
//                        Log.i(TAG, "onSuccess: backward distances: " + backward_distance);
////                        Log.i(TAG, "onSuccess: left distances: " + distances[270]);
//                    }
//
//                    @Override
//                    public void onFailure(DJIError djiError) {
//
//                    }
//                });
            }
        }
        flightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
        flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
        flightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);    // degrees/second
        flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);

        // set Obstacle Avoidance
        flightAssistant.setLandingProtectionEnabled(false,null);
        flightAssistant.setCollisionAvoidanceEnabled(false, null);
        flightAssistant.setUpwardVisionObstacleAvoidanceEnabled(false, null);
//        flightAssistant.setVisualObstaclesAvoidanceDistance(1.2f, PerceptionInformation.DJIFlightAssistantObstacleSensingDirection.Horizontal,null); //Horizontal Field. The horizontal distance range is 1.1m~40m
//        flightAssistant.setVisualObstaclesAvoidanceDistance(0.4f, PerceptionInformation.DJIFlightAssistantObstacleSensingDirection.Downward,null); //Downward sensing. The downward distance range is 0.6m~30m
//        flightAssistant.setVisualObstaclesAvoidanceDistance(1.1f, PerceptionInformation.DJIFlightAssistantObstacleSensingDirection.Upward,null); //Upward sensing. The upward distance range is 1.1m~30m
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_old_feeder_view);
        initUI();



//        new Thread(()->{
//            Float xb =null, yb=null,zb=null, yawb=null, pitchb=null, rollb=null;
//            while(true){
//                if(flight.payload == null) continue;
//                float[] bottomLocation = flight.payload.getBottomLocation();
//                if(bottomLocation != null){
//                    xb =bottomLocation[0]; yb=bottomLocation[1];zb=bottomLocation[2]; yawb=bottomLocation[3]; rollb=bottomLocation[4]; pitchb=bottomLocation[5];
//                }
//
//                if(bottomLocation == null){
//                    xb =null; yb=null;zb=null; yawb=null; pitchb=null; rollb=null;
//                }
//
//                Float finalXb = xb;
//                Float finalYb = yb;
//                Float finalZb = zb;
//                Float finalYawb = yawb;
//                Float finalRollb = rollb;
//                Float finalPitchb = pitchb;
//
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        showScreenTextBottom(finalXb,  finalYb,  finalZb,  finalYawb,  finalRollb, finalPitchb);
//                    }
//                });
//                SystemClock.sleep(500);
//            }
//        }).start();




        initParams();
        droneStart();
        FlightController flightController = ModuleVerificationUtil.getFlightController();
        flight.register(flightController);
        flight.register(arucoMethod.current_arucos);
        flight.register(OldFeederView.this);
        flight.setFlightMode("VELOCITY");
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
        mCaptureBtn.setOnClickListener(v -> {
            saveImageToExternalStorage(BitmapFromFeedersSurface);
            SetLEDs(0);

        });
        TakeOffBtn.setOnClickListener(v -> flightController.startTakeoff(djiError -> showToast("taking off")));
        LandBtn.setOnClickListener(v -> flightController.startLanding(djiError -> showToast("Landing")));
        YawBtn.setOnClickListener(v -> flight.rotation(90));

        EmergencyBtn.setOnClickListener(v -> {
            flight.emergency();
            saveImageToExternalStorage(BitmapFromFeedersSurface);
            if(flight.emg_now){
                showToast("Emergency");
            }else {
                showToast("dismiss the alert");
            }
        });
        //SetForward(50,1000);
        ForwardBtn.setOnClickListener(v -> {
            try {

                Log.i("ForwardButton","Start to Fly");
                flight.threadMoveTo(3,0,0,0.5f);

            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.i("ForwardButton","Interrupted");
                Thread.currentThread().interrupt();

            }

        });
        BackwardsBtn.setOnClickListener(v -> {
            flight_thread = new Thread(()->{
                flightController.setVirtualStickAdvancedModeEnabled(true);
                flight.moveTo(-0,-1.5f,0);
                SystemClock.sleep(500);
                flightController.setVirtualStickAdvancedModeEnabled(false);
            });
            flight_thread.start();        });
        RightBtn.setOnClickListener(v -> {
            flight_thread = new Thread(()->{
                flightController.setVirtualStickAdvancedModeEnabled(true);
                flight.moveTo(1f,-0,0);
                SystemClock.sleep(500);
                flightController.setVirtualStickAdvancedModeEnabled(false);
            });
            flight_thread.start();
        });

        LeftBtn.setOnClickListener(v -> {
            flight.moveTo(-1,0,0);
        });
        UpBtn.setOnClickListener(v -> {
            showToast("Up");
            flight.moveTo(0,0,1f);
        });

        DownBtn.setOnClickListener(v -> {
            flight_thread = new Thread(()->{
                flight.switchVirtualStickMode(true);
                Float[] location = payload.getBottomLocation();
                if (location == null) return;
                Log.i("location",location.toString());
                flight.moveTo(0, 0, -location[2]+.22f, 0.1f);
                flight.switchVirtualStickMode(false);
            });
            flight_thread.start();
        });

        mBtnStart.setOnClickListener(v -> {

            flight_thread = new Thread(()->{
                try{
                    Thread.sleep(250);
                 payload.gripperControl(true);}
                catch(InterruptedException e){
                    e.printStackTrace();
                }
            });
            flight_thread.start();
            Float[] asda = payload.getBottomLocation();
            float danny= 10;
            if(danny==10){
                flight_thread.interrupt();
            }
        });

        ArucoBtn.setOnClickListener(v -> {
            flight_thread = new Thread(()->{
                flight.throughBall(0,4,0);
            });
            flight_thread.start();
        });

        MoveTo.setOnClickListener(v -> {
            flight_thread = new Thread(() -> {
                try {
                    // flight.demo3();
                    flight.demo4();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            flight_thread.start();
        });

        SwitchVirtualStick.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    flight.switchVirtualStickMode(true);
                }
                else {
                    flight.switchVirtualStickMode(false);
                }
            }
        });


        TurnOnMotorsBtn.setOnClickListener(v -> flight.flightController.turnOnMotors(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError != null) {
                    Log.e(TAG, "onResult: " + djiError.getErrorCode());
                    ToastUtils.setResultToToast(djiError.getDescription());
                }else{
                    showToast("Turning on Motors");
                }
            }
        }));
        TurnOffMotorsBtn.setOnClickListener(v -> flightController.turnOffMotors(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError != null) {
                    ToastUtils.setResultToToast(djiError.getDescription());
                }else{
                    showToast("Turning off Motors");
                }
            }
        }));
    }

    public void saveImageToExternalStorage(Bitmap finalBitmap) {
        //Bitmap resized = null;
        //resized = Bitmap.createScaledBitmap(finalBitmap,1280,960, true);
        String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
        File myDir = new File(root + "/CV_drone");
        if(!myDir.exists()){ //todo: check it can work
            myDir.mkdirs();
        }
        Random generator = new Random();
        int n = 10000;
        n = generator.nextInt(n);
        String fname = "Image-" + n + ".jpg";
        File file = new File(myDir, fname);
        if (file.exists()) {
            file.delete();
        }
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
        mBtnStart = findViewById(R.id.btn_startPos);

        SwitchVirtualStick = findViewById(R.id.SwitchVirtualStick);

        TurnOnMotorsBtn = findViewById(R.id.btn_turn_on_motors);
        TurnOffMotorsBtn = findViewById(R.id.btn_turn_off_motors);


        if (mVideoTexture != null) {
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
        if(flight.emg_now){EmergencyBtn.setBackgroundTintList(ColorStateList.valueOf(Color.RED));}
        else {EmergencyBtn.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));}
        BitmapFromFeedersSurface = Bitmap.createScaledBitmap(
                mVideoTexture.getBitmap(),
                arucoMethod.pic_width,
                arucoMethod.pic_height,
                true); // get bitmap from the drone video feed
        BitmapFromFeedersSurface = arucoMethod.ArucoDetector(BitmapFromFeedersSurface);
        mImageSurface.setImageBitmap(null);
        mImageSurface.setImageBitmap(BitmapFromFeedersSurface);
        if (!arucoMethod.current_arucos.isEmpty()) {
            ArucoCoordinate aruco = arucoMethod.current_arucos.get(0);
            showScreenText( aruco.x,aruco.y, aruco.z, aruco.yaw, aruco.pitch, aruco.roll);
        }
        else{
            showScreenText(null,null,null,null,null,null);
        }
    }

    void showScreenText(Float x,Float y, Float z, Float yaw, Float pitch, Float roll){

        //FPV CAMERA
        TextView theTextView1 = (TextView) findViewById(R.id.textView1);
        TextView theTextView2 = (TextView) findViewById(R.id.textView2);
        TextView theTextView3 = (TextView) findViewById(R.id.textView3);
        TextView theTextView4 = (TextView) findViewById(R.id.textView4);
        TextView theTextView5 = (TextView) findViewById(R.id.textView5);
        TextView theTextView6 = (TextView) findViewById(R.id.textView6);
        TextView theTextView7 = (TextView) findViewById(R.id.textView7);




        //FPV CAMERA
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
        if(flight != null){
            theTextView7.setText("orientation: " + flight.getOrientation());
            theTextView7.setTextColor(Color.BLUE);
        }


    }

    Runnable showScreenTextBottom(Float xb, Float yb, Float zb, Float yawb, Float pitchb, Float rollb){
        //BOTTOM CAMERA
        TextView theTextView7 = (TextView) findViewById(R.id.textView7);
        TextView theTextView8 = (TextView) findViewById(R.id.textView8);
        TextView theTextView9 = (TextView) findViewById(R.id.textView9);
        TextView theTextView10 = (TextView) findViewById(R.id.textView10);
        TextView theTextView11 = (TextView) findViewById(R.id.textView11);
        TextView theTextView12 = (TextView) findViewById(R.id.textView12);

        //BOTTOM CAMERA
        theTextView7.setText("X: " + xb);
        theTextView8.setText("Y: " + yb);
        theTextView9.setText("Z: " + zb);
        theTextView12.setText("Yaw: " + yawb);
        theTextView11.setText("Roll: " + rollb);
        theTextView10.setText("Pitch: " + pitchb);
        theTextView7.setTextColor(Color.YELLOW);
        theTextView8.setTextColor(Color.YELLOW);
        theTextView9.setTextColor(Color.YELLOW);
        theTextView12.setTextColor(Color.YELLOW);
        theTextView11.setTextColor(Color.YELLOW);
        theTextView10.setTextColor(Color.YELLOW);

        return null;
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

    /**
     * send "pitch, roll, throttle, yaw" to drone
     */
    //todo: move SendVirtualStickDataTask to the flight class
    private class SendVirtualStickDataTask extends TimerTask {
        @Override
        public void run() {
            pitch = flight.pitch;
            roll = flight.roll;
            throttle = flight.throttle;
            yaw = flight.yaw;
            if (flightController != null) {
                //接口写反了，setPitch()应该传入roll值，setRoll()应该传入pitch值
                flightController.sendVirtualStickFlightControlData(new FlightControlData(pitch, roll, yaw, throttle), djiError -> {
                    if (djiError != null) {
                        ToastUtils.setResultToToast(djiError.getDescription());
                    }
                });
            }
        }
    }

    private void droneStart() {
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
        super.onDestroy();
    }
}