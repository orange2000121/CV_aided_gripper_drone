//package com.dji.activationDemo;
//
//import static com.dji.activationDemo.ToastUtils.showToast;
//
//import static java.lang.Math.abs;
//import static java.lang.Math.max;
//import static java.lang.Math.sqrt;
//
//import android.os.Handler;
//import android.util.Log;
//
//
//import dji.sdk.flightcontroller.FlightController;
//
//public class FlightControleMethod {
//    public double roll, pitch, throttle, yaw;// control the drone flying
//    private double[] aruco_coordinates={0,0,0};
//    public boolean emg_now = false;// emergency button
//    FlightController flightController;
//    public FlightControleMethod(FlightController flightController){
//        this.flightController = flightController;
//    }
//    public void TwoDAruco(double right_left_gap, double front_back_gap, double up_down_gap, double yaw) {
//        showToast("going");
//        flightController.setVirtualStickModeEnabled(true, djiError -> {
//            flightController.setVirtualStickAdvancedModeEnabled(true);
//            if (djiError != null) {
//                ToastUtils.setResultToToast(djiError.getDescription());
//            }
//        });
//        // todo set the mode in other place
//        // flightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
//        // flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
//        // flightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
//        // flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
//
//        front_back_gap -= 1; // 1 meter in front of aruco
//        double distance = sqrt(right_left_gap * right_left_gap + front_back_gap * front_back_gap);
//        float higher_speed = (float) max(abs(right_left_gap), abs(front_back_gap));
//
//        if (abs(right_left_gap) < 0.1 && abs(front_back_gap) < 0.1)
//            return; // if the distance is too small, don't move
//        if (higher_speed > 1) {
//            roll = (float) front_back_gap / higher_speed; // forward + backwards - MAX 15 From 8, overshoot
//            throttle = (float) up_down_gap / higher_speed; // up + down - MAX 4 From 3, overshoot
//            pitch = (float) right_left_gap / higher_speed; // right + left - MAX = 15 From 8, starts to overshoot
//        } else {
//            float basic_speed = 1;
//            roll = basic_speed * (float) front_back_gap;
//            throttle = basic_speed * (float) up_down_gap;
//            pitch = basic_speed * (float) right_left_gap;
//        }
//
//        // reduce speed
//        roll /= 2;
//        throttle /= 2;
//        pitch /= 2;
//        double flying_time = distance / sqrt(roll * roll + pitch * pitch);
//        Log.i("flying", String.format("x: %f, y: %f, z: %ff", right_left_gap, front_back_gap, up_down_gap));
//        Log.i("flying",
//                String.format("forward: %f, horizontal: %f, up: %f, fly time: %f", roll, pitch, throttle, flying_time));
//        if (flying_time > 10) {
//            setZero();
//        } else {
//            Handler handler = new Handler();
//            handler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    setZero();
//                    if (emg_now)
//                        return;
//                    TwoDAruco(aruco_coordinates[0], aruco_coordinates[2], 0, 0);
//                }
//            }, (long) flying_time * 1000);
//        }
//    }
//    private void setZero(){
//        pitch = (float)0.0;
//        roll = (float) 0.0;
//        throttle = (float)0.0;
//        yaw = (float)(0.0);
//        showToast("STOP");
//
//    }
//
//}
