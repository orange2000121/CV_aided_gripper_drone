package com.dji.activationDemo;

import static java.lang.Math.abs;

import android.graphics.Bitmap;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class ArucoMethod {
    /* ----------------------------- public variable ----------------------------- */
    public int pic_width = 1280;
    public int pic_height = 960;
    public List<ArucoCoordinate> current_arucos = new ArrayList<>();
    /* ---------------------------- construct variable --------------------------- */

    /* ----------------------------- private variable ---------------------------- */
    public Mat cameraMatrix, distCoeffs;//camera matrix and distortion coefficients
    String TAG = ArucoMethod.class.getName();

    /* -------------------------------------------------------------------------- */
    /*                                 constructor                                */
    /* -------------------------------------------------------------------------- */
    /**
     * initialize camera matrix and distortion coefficients
     */
    ArucoMethod() {
        OpenCVLoader.initDebug();
        /* --------------------------- camera calibration --------------------------- */
        cameraMatrix = Mat.zeros(3, 3, CvType.CV_64FC1);
        cameraMatrix.put(0, 0, 529.66735352);
        cameraMatrix.put(0, 2, 643.64118485);
        cameraMatrix.put(1, 1, 527.90085357);
        cameraMatrix.put(1, 2, 482.21164092);
        cameraMatrix.put(2, 2, 1.00000000e+00);
        distCoeffs = Mat.zeros(1, 5, CvType.CV_64FC1);
        distCoeffs.put(0, 0, -0.13242401);
        distCoeffs.put(0, 1, 0.00586409);
        distCoeffs.put(0, 2, 0.00050706);
        distCoeffs.put(0, 3, 0.00138085);
        distCoeffs.put(0, 4, 0.00189516);
    }
    /* -------------------------------------------------------------------------- */
    /*                                  Functions                                 */
    /* -------------------------------------------------------------------------- */
    public Bitmap ArucoDetector(Bitmap BitmapFromFeedersSurface) {

        float MarkerSize_mm = (float) 0.182;  //A4 paper

        List<Mat> corners = new ArrayList<>();
        Mat droneImage = new Mat();
        Mat grayImage = new Mat();
        MatOfInt ids = new MatOfInt();
        DetectorParameters parameters = DetectorParameters.create();
        parameters.set_cornerRefinementMethod(1);
        parameters.set_cornerRefinementMinAccuracy(0.05);
        parameters.set_cornerRefinementWinSize(5);
        Dictionary dictionary = Aruco.getPredefinedDictionary(Aruco.DICT_6X6_50); //MARKER NUMBER 23
//        Bitmap BitmapFromFeedersSurface = Bitmap.createScaledBitmap(mVideoTexture.getBitmap(), pic_width, pic_height, true);
        Mat RGBMatFromBitmap = new Mat();

        Utils.bitmapToMat(BitmapFromFeedersSurface, droneImage);
        Imgproc.cvtColor(droneImage, grayImage, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(droneImage, RGBMatFromBitmap, Imgproc.COLOR_RGBA2RGB);
        Aruco.detectMarkers(grayImage, dictionary, corners, ids, parameters);

        if (corners.size() > 0) {
            //Draw lines at center of the image
            //Vertical line
            Point start_vert_line = new Point((double) pic_width / 2, 0);
            Point end_vert_line = new Point((double) pic_width / 2, pic_height);
            Scalar color_line = new Scalar(255, 0, 0);
            int thickness = 3;
            Imgproc.line(RGBMatFromBitmap, start_vert_line, end_vert_line, color_line, thickness);

            //Horizontal line
            Point start_horiz_line = new Point(0, (double) pic_height / 2);
            Point end_horiz_line = new Point(pic_width, (double) pic_height / 2);
            Imgproc.line(RGBMatFromBitmap, start_horiz_line, end_horiz_line, color_line, thickness);

            Mat rvecs = new Mat();
            Mat tvecs = new Mat();

            //Pose Estimation
            Aruco.drawDetectedMarkers(RGBMatFromBitmap, corners, ids);

            //AVG = 0.1715M
            Aruco.estimatePoseSingleMarkers(corners, MarkerSize_mm, cameraMatrix, distCoeffs, rvecs, tvecs);

            //REAL WORLD CORNERS IN MM
            List<Point3> corners4 = new ArrayList<>(4);
            corners4.add(new Point3(-MarkerSize_mm / 2, MarkerSize_mm / 2, 0));        // Top-Left
            corners4.add(new Point3(MarkerSize_mm / 2, MarkerSize_mm / 2, 0));        // Top-Right
            corners4.add(new Point3(MarkerSize_mm / 2, -MarkerSize_mm / 2, 0));        // Bottom-Right
            corners4.add(new Point3(-MarkerSize_mm / 2, -MarkerSize_mm / 2, 0));        // Bottom-Left


            MatOfPoint3f m_corners = new MatOfPoint3f();
            m_corners.fromList(corners4);
            current_arucos.clear(); //reset the list of arucos
            for (int i = 0; i < ids.toArray().length; i++) {
                Calib3d.drawFrameAxes(RGBMatFromBitmap, cameraMatrix, distCoeffs, rvecs.row(i), tvecs.row(i), 0.13f);
                Mat aruco_rotation_vec = new Mat(3, 3, 6);
                Calib3d.Rodrigues(rvecs.row(i), aruco_rotation_vec);
                double[] aruco_translation_vector = tvecs.get(i, 0); //get the x,y,z translation vector
                double[] old_yawPitchRoll = oldMatrixToYawPitchRoll(rvecs,tvecs,i);// get the yaw pitch and roll
                double[] yawPitchRoll = MatrixToYawPitchRoll(aruco_rotation_vec);// get the yaw pitch and roll
                double aruco_roll = yawPitchRoll[2];
                double aruco_pitch = yawPitchRoll[1];
                double aruco_yaw = yawPitchRoll[0];
                //set the aruco offset
                aruco_translation_vector[0] -= 0.03;

                current_arucos.add(new ArucoCoordinate((float) aruco_translation_vector[0], (float) aruco_translation_vector[1], (float) aruco_translation_vector[2], (float) aruco_roll, (float) aruco_pitch, (float) aruco_yaw, (int) ids.get(i, 0)[0]));

                MatOfPoint2f projected = new MatOfPoint2f();

                distCoeffs = new MatOfDouble(distCoeffs);

                Calib3d.projectPoints(m_corners, rvecs.row(i), tvecs.row(i), cameraMatrix, (MatOfDouble) distCoeffs, projected);

                Point[] points = projected.toArray();

                if (points != null) {
                    //Draw the circle of the aruco corners
                    Imgproc.circle(RGBMatFromBitmap, points[0], 10, new Scalar(255, 0, 0), 4);
                    Imgproc.circle(RGBMatFromBitmap, points[1], 10, new Scalar(0, 0, 0), 4);
                    Imgproc.circle(RGBMatFromBitmap, points[2], 10, new Scalar(0, 255, 0, 150), 4);
                    Imgproc.circle(RGBMatFromBitmap, points[3], 10, new Scalar(0, 0, 255), 4);
                }
                projected.release();
            }
        }
        //Bitmap DisplayBitmap = Bitmap.createBitmap(RGBMatFromBitmap.cols(),RGBMatFromBitmap.rows(), Bitmap.Config.ARGB_8888);
        Bitmap DisplayBitmap = Bitmap.createBitmap(pic_width, pic_height, Bitmap.Config.ARGB_8888);
//        Mat displayMat = new Mat();
//        Calib3d.undistort(RGBMatFromBitmap, displayMat, cameraMatrix, distCoeffs);
        Utils.matToBitmap(RGBMatFromBitmap, DisplayBitmap);
//        Utils.matToBitmap(displayMat, DisplayBitmap);
        return DisplayBitmap;
    }
    // todo: 完成更新中位數
    private void updateAruco(ArucoCoordinate aruco){
        for (ArucoCoordinate aruco_temp : current_arucos) {
            if(aruco_temp.id == aruco.id){

            }
        }
    }
    private double[] oldMatrixToYawPitchRoll(Mat rvecs, Mat tvecs, int i){
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
        double arucoroll = ArucoeulerAngles.get(0, 0)[0];  //for debugging, printing on screen
        double arucopitch = ArucoeulerAngles.get(1, 0)[0];
        double arucoyaw = -ArucoeulerAngles.get(2, 0)[0];// change sign to get the rotation needed by the drone not the paper
        return new double[]{arucoyaw, arucopitch, arucoroll};
    }
    private double[] MatrixToYawPitchRoll( Mat A )
    {
        double[] angle = new double[3];
        angle[1] = -Math.asin( A.get(2,0)[0] )*57.2957795;  //Pitch
        //Gymbal lock: pitch = -90
        if( A.get(2,0)[0]   == 1 ){
            angle[0] = 0.0;             //yaw = 0
            angle[2] = Math.atan2( -A.get(0,1)[0], -A.get(0,2)[0] )* 57.2957795;
        }
        //Gymbal lock: pitch = 90
        else if( A.get(2,0)[0] == -1 ){
            angle[0] = 0.0;             //yaw = 0
            angle[2] = Math.atan2( A.get(0,1)[0], A.get(0,2)[0] )* 57.2957795;//Roll
        }
        //General solution
        else{
            angle[0] = Math.atan2(  A.get(1,0)[0], A.get(0,0)[0] )* 57.2957795;
            angle[2] = Math.atan2(  A.get(2,1)[0], A.get(2,2)[0] )* 57.2957795;
        }
        return angle;   //Euler angles in order yaw, pitch, roll
    }

}
/**
 * all data of the aruco marker
 * todo: 新增求中位數的方法
 * todo: 更新座標資料
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