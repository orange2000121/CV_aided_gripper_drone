package com.dji.activationDemo;
import static com.dji.sdk.sample.internal.utils.ToastUtils.showToast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
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


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.math.MathUtils;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ColorSpace;
import android.graphics.SurfaceTexture;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dji.sdk.sample.internal.utils.ModuleVerificationUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import dji.common.gimbal.GimbalState;
import dji.common.product.Model;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.FlightController;

public class CameraCalib extends AppCompatActivity implements TextureView.SurfaceTextureListener {
    private static final String TAG = DemoApplication.class.getName();
    protected VideoFeeder.VideoDataListener mReceivedVideoDataListener = null;
    protected DJICodecManager mCodecManager = null;
    protected TextureView mVideoSurface = null;
    protected ImageView mImageSurface;
    private Button mCaptureBtn;
    private Bitmap sourceBitmap, BitmapFromFeedersSurface;
    private Mat RGBmatFromBitmap;
    private CameraBridgeViewBase camera;
    private MatOfInt ids;
    //private List<Mat> corners;
    private Dictionary dictionary;
    private DetectorParameters parameters;
    private int STORAGE_PERMISSION_CODE= 1;
    double[] dannyisgoinghome = {0,0,0};
    Date startDate=null;

    double zapato = 323.37471;

    //-----------------------------------------------------------

    //----Image Processing
    ArrayList<Float> allx = new ArrayList<>();
    ArrayList<Float> ally = new ArrayList<>();
    ArrayList<Float> allz = new ArrayList<>();
    ArrayList<Float> allroll = new ArrayList<>();
    ArrayList<Float> allpitch = new ArrayList<>();
    ArrayList<Float> allyaw = new ArrayList<>();
    float avgx=0,avgy=0,avgz=0,globalavgx=0,globalavgy=0,globalavgz=0;
    private float mPitch;
    private float mRoll;
    private float mYaw;
    private float mThrottle;
    double p11,p12,p21,p22,p31,p32,p41,p42;
    float zarucofloat, yarucofloat,xarucofloat,yawarucofloat;
    float radtodeg = (float) (180/3.141592);
    double arucoroll;
    double arucopitch;
    double arucoyaw;
    double que2;
    double que3;
    double que;
    double[] arucotranslationvector;
    float previous = System.currentTimeMillis();
    float deltatime = 0;

    private final BaseLoaderCallback loaderCallback = new BaseLoaderCallback(this){
        @Override
        public void onManagerConnected(int status){
            if(status == LoaderCallbackInterface.SUCCESS){
                String message = "";
                Toast.makeText(CameraCalib.this,  message,  Toast.LENGTH_SHORT).show();
            }
            else {
                super.onManagerConnected(status);
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_calib);
        initUI();
        if(OpenCVLoader.initDebug()){
            loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            showToast("Loads OpenCV");}

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
                requestStoragepermission();
                saveImageToExternalStorage(BitmapFromFeedersSurface);
                showToast("Picture Taken");
            }
        });
    }
    public void saveImageToExternalStorage(Bitmap finalBitmap) {
        String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
        File myDir = new File(root + "/CameraCalib1");
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
        mVideoSurface = (TextureView) findViewById(R.id.video_previewer_surface);
        mImageSurface = (ImageView) findViewById(R.id.flight_image_previewer_surface);
        mCaptureBtn = (Button) findViewById(R.id.btn_capture);
        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
        }
    }


    private void initPreviewer() {

        BaseProduct product = DemoApplication.getProductInstance();

        if (product == null || !product.isConnected()) {
            showToast(getString(R.string.disconnected));
        } else {
            if (null != mVideoSurface) {
                mVideoSurface.setSurfaceTextureListener(this);
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

        if(mVideoSurface == null) {
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
        ArucoDetector();

    }

    private void requestStoragepermission(){

        if(ActivityCompat.shouldShowRequestPermissionRationale(CameraCalib.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            new AlertDialog.Builder(CameraCalib.this)
                    .setTitle("Permission needed")
                    .setMessage("Do It!")
                    .setPositiveButton("alright", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton("noooe", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create().show();

        }else{
            ActivityCompat.requestPermissions(CameraCalib.this,new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},STORAGE_PERMISSION_CODE);
        }

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {

        if(requestCode==STORAGE_PERMISSION_CODE){
            if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                showToast("Permission Granted");
            }else{
                showToast("Not Granted");
            }

        }
    }



    private void ArucoDetector() {

        int picwidth = 1280;
        int picheight = 960;
        float MarkerSizeinm = (float) 0.181;
        List<Mat> corners = new ArrayList();
        corners.clear();
        Mat droneImage = new Mat();
        Mat grayImage = new Mat();
        ids = new MatOfInt();
        parameters = DetectorParameters.create();
        parameters.set_cornerRefinementMethod(1);
        parameters.set_cornerRefinementWinSize(12);
        dictionary = Aruco.getPredefinedDictionary(Aruco.DICT_6X6_50); //MARKER NUMBER 23
        BitmapFromFeedersSurface = Bitmap.createScaledBitmap(mVideoSurface.getBitmap(),picwidth,picheight, true);
        RGBmatFromBitmap = new Mat();


//        double tall=0, with=0;
//        tall = mCodecManager.getVideoHeight();
//        with = mCodecManager.getVideoWidth();
//        showToast(("tall"+ tall+"    width"+with));

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


            Mat cameraMatrix = Mat.zeros(3, 3, CvType.CV_64F); //300 - 600
            cameraMatrix = Mat.zeros(3, 3, CvType.CV_64FC1);
            cameraMatrix.put(0, 0, 556.86218075);
            cameraMatrix.put(0, 2, 624.18715691);
            cameraMatrix.put(1, 1, 556.86218075);
            cameraMatrix.put(1, 2, 556.86218075);
            cameraMatrix.put(2, 2, 1.00000000e+00);
            Mat distCoeffs = Mat.zeros(0, 1, CvType.CV_64F);
            distCoeffs = Mat.zeros(1, 8, CvType.CV_64FC1);
            distCoeffs.put(0, 0, -9.95419459e-02);
            distCoeffs.put(0, 1, -3.91315171e-02);
            distCoeffs.put(0, 2, -1.41858075e-05);
            distCoeffs.put(0, 3, -2.83714318e-03);
            distCoeffs.put(0, 4, 3.91411052e-01);
            distCoeffs.put(0, 5, 1.89461260e-02);
            distCoeffs.put(0, 6, -3.46822362e-02);
            distCoeffs.put(0, 7, 4.24582720e-01);




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

                dannyisgoinghome = MatrixToYawPitchRoll(arucorotationmat);

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
            zarucofloat= (float) arucotranslationvector[2]; //the zero value is the z axis through the camera
            yarucofloat = (float) arucotranslationvector[1] ;
            xarucofloat = (float) arucotranslationvector[0]; //the zero value is the x axis, side to side of the camera
            yawarucofloat = (float) arucoyaw;

            //Turn negatives angles into positives
            if(yawarucofloat<0){
                yawarucofloat=yawarucofloat+360;
            }


            //arraylists to hold values
            allx.add(xarucofloat);
            ally.add(yarucofloat);
            allz.add(zarucofloat);
            allyaw.add(yawarucofloat);



            int checksize = 5;

            if (allz.size()==checksize) {//checksize = 5 but therer are actually 6 elements in the arrays

                float sumx = 0, sumy = 0, sumz = 0, sumyaw = 0, meanx = 0, meany = 0, meanz = 0, meanyaw = 0;

                for (int j = 0; j < checksize; j++) {


                    que = allyaw.get(j);
                    sumz += allz.get(j);
                    sumy += ally.get(j);
                    sumx += allx.get(j);
                    sumyaw += allyaw.get(j);
                }
                meanx = sumx / allx.size();
                meany = sumy / ally.size();
                meanz = sumz / allz.size();
                meanyaw = sumyaw / allyaw.size();

                //Taking error out of the y-axis
                double fixy = meany - (meanz * 7.4 / 100);
                meany = (float) fixy;
                int yawhowmany = allyaw.size();

                //showToast(" cuanto: " +sumyaw+" cauntas: " +yawhowmany+" X: " + String.format("%.4f", (meanx)) + "   Y: " + String.format("%.4f", (meany)) + "   Z: " + String.format("%.4f", (meanz)) + "  Yaw:" + String.format("%.4f", (yawarucofloat)));
                allx.clear();
                ally.clear();
                allz.clear();
                allyaw.clear();

            }

            TextView theTextView1 = (TextView) findViewById(R.id.textView1);
            TextView theTextView2 = (TextView) findViewById(R.id.textView2);
            TextView theTextView3 = (TextView) findViewById(R.id.textView3);
            TextView theTextView4 = (TextView) findViewById(R.id.textView4);
            TextView theTextView5 = (TextView) findViewById(R.id.textView5);
            TextView theTextView6 = (TextView) findViewById(R.id.textView6);
            theTextView1.setText("X: " + String.format("%.4f", dannyisgoinghome[0])  + " ,  ");
            theTextView2.setText("Y: " + String.format("%.4f", dannyisgoinghome[1])  + " ,  ");
            theTextView3.setText("Z: " + String.format("%.4f", dannyisgoinghome[2])  + " ,  ");
            theTextView4.setText("Yaw: " + String.format("%.4f", arucoyaw)  + " ,  ");
            theTextView5.setText("Roll: " + String.format("%.4f", arucoroll)  + " ,  ");
            theTextView6.setText("Pitch: " + String.format("%.4f", arucopitch)  + " ,  ");
            theTextView1.setTextColor(Color.BLUE);
            theTextView2.setTextColor(Color.BLUE);
            theTextView3.setTextColor(Color.BLUE);
            theTextView4.setTextColor(Color.BLUE);
            theTextView5.setTextColor(Color.BLUE);
            theTextView6.setTextColor(Color.BLUE);

        }

        float current = System.currentTimeMillis();
        deltatime += current - previous;
//        showToast(String.valueOf(deltatime));131072*200
        if(deltatime>300){
//            Toast.makeText(this, "differencia"+deltatime,Toast.LENGTH_SHORT).show();
            saveImageToExternalStorage(BitmapFromFeedersSurface);

            previous = current;
        }


        Bitmap DisplayBitmap = Bitmap.createBitmap(RGBmatFromBitmap.cols(),RGBmatFromBitmap.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(RGBmatFromBitmap, DisplayBitmap);
        mImageSurface.setImageBitmap(null);
        mImageSurface.setImageBitmap(DisplayBitmap);
    }
    public double[] MatrixToYawPitchRoll( Mat A )
    {
//        Log.i("TAG", "MatrixToYawPitchRoll: " + A.dump());
        double[] angle = new double[3];
        angle[1] = -Math.asin( A.get(2,0)[0] )*57.2957795;  //Pitch

        //Gymbal lock: pitch = -90
        if( A.get(2,0)[0]   == 1 ){
            angle[0] = 0.0;             //yaw = 0
            angle[2] = Math.atan2( -A.get(0,1)[0], -A.get(0,2)[0] )* 57.2957795;

            }


            //Roll
//            System.out.println("Gimbal lock: pitch = -90");



        //Gymbal lock: pitch = 90
        else if( A.get(2,0)[0] == -1 ){
            angle[0] = 0.0;             //yaw = 0
            angle[2] = Math.atan2( A.get(0,1)[0], A.get(0,2)[0] )* 57.2957795;
//Roll
//            System.out.println("Gimbal lock: pitch = 90");
        }
        //General solution
        else{
            angle[0] = Math.atan2(  A.get(1,0)[0], A.get(0,0)[0] )* 57.2957795;
            angle[2] = Math.atan2(  A.get(2,1)[0], A.get(2,2)[0] )* 57.2957795;


//            System.out.println("No gimbal lock");
        }
        return angle;   //Euler angles in order yaw, pitch, roll
    }
}
