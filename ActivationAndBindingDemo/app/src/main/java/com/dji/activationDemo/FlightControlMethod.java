package com.dji.activationDemo;

import static com.dji.activationDemo.ToastUtils.showToast;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.sqrt;

import android.nfc.Tag;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;


import java.util.List;

import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.sdk.flightcontroller.FlightController;

public class FlightControlMethod {
    public float roll, pitch, throttle, yaw;// control the drone flying
    public boolean emg_now = false;// emergency button
    String TAG = "FlightControlMethod";
    public List<ArucoCoordinate> arucoCoordinateList=null; //current aruco coordinate list
    private FlightController flightController; //flight controller from dji sdk
    public int function_times = 0;
    /* ------------------------------- structure ------------------------------- */
    public FlightControlMethod(){}
    public boolean register(FlightController flightController){
        if(flightController==null) return false;
        this.flightController = flightController;
        return true;
    }
    public boolean register(List<ArucoCoordinate> arucoCoordinateList){
        if(arucoCoordinateList==null) return false;
        this.arucoCoordinateList = arucoCoordinateList;
        return true;
    }
    /* -------------------------------------------------------------------------- */
    /*                                main activity                               */
    /* -------------------------------------------------------------------------- */

    /**
     * Find the specified id in the list
     * @param aruco_id : the id you want to find,type: int
     * @return : if found it return aruco coordinate, else return null, type: ArucoCoordinate
     */
    public ArucoCoordinate findAruco(int aruco_id){
        if (arucoCoordinateList == null) {
            showToast("Didn't register aruco list");
            return null;
        };
        if (function_times >100) return null;
        Log.i("flydemo", "findAruco: "+aruco_id);
        function_times++;
        yaw = 5;// can be changed
        //determine the aruco is in the list
        for(ArucoCoordinate arucoCoordinate : arucoCoordinateList){
            if(arucoCoordinate.id == aruco_id){
                setZero();
                Log.i("flydemo", "found : "+aruco_id);
                return arucoCoordinate;
            }
        }
        //if not found, keep searching
        SystemClock.sleep(1000);
        return findAruco(aruco_id);
    }

    public void goToSecondAruco(ArucoCoordinate aruco){
        Log.i("flydemo", "goToSecondAruco: "+aruco.id);
        moveTo(aruco.x,aruco.y-1,aruco.z,0);
    }

    /**
     * Change the yaw control mode velocity or angle
     * @param mode : "VELOCITY" or "ANGLE", type: String
     */
    public void setFlightMode(String mode){
        switch(mode){
            case "VELOCITY":
                 flightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
                 flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
                 flightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
                 flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
                 break;
             case "ANGLE":
                 flightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
                 flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
                 flightController.setYawControlMode(YawControlMode.ANGLE);
                 flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
                 break;
        }
    }
    /* -------------------------------------------------------------------------- */
    /*                            functional function                             */
    /* -------------------------------------------------------------------------- */
    public void moveTo(double right_left_gap, double front_back_gap, double up_down_gap, double yaw) {
        showToast("going");
        flightController.setVirtualStickModeEnabled(true, djiError -> {
            flightController.setVirtualStickAdvancedModeEnabled(true);
            if (djiError != null) {
                ToastUtils.setResultToToast(djiError.getDescription());
            }
        });
        double distance = sqrt(right_left_gap * right_left_gap + front_back_gap * front_back_gap);
        float higher_speed = (float) max(abs(right_left_gap), abs(front_back_gap));

        if (abs(right_left_gap) < 0.1 && abs(front_back_gap) < 0.1) return; // if the distance is too small, don't move

        if (higher_speed > 1) {
            /*
            * Make the max speed of the drone is 1m/s
            * Calculate speed proportional to distance
            */
            roll = (float) front_back_gap / higher_speed;
            throttle = (float) up_down_gap / higher_speed;
            pitch = (float) right_left_gap / higher_speed;
        } else {
            /*
            * If the distance is less than 1 meter, the speed is proportional to distance
            */
            float basic_speed = 1;
            roll = basic_speed * (float) front_back_gap;
            throttle = basic_speed * (float) up_down_gap;
            pitch = basic_speed * (float) right_left_gap;
        }

        /*
        * Reduce the speed of the drone
        */
        roll /= 2;
        throttle /= 2;
        pitch /= 2;
        double flying_time = distance / sqrt(roll * roll + pitch * pitch);
        if (flying_time > 10) {// if the flying time is too long, don't move
            setZero();
        } else {
            SystemClock.sleep((long) (flying_time * 1000));// when the drone is moving, wait for the drone to move
            setZero();
        }
    }
    private void setZero(){
        pitch = (float)0.0;
        roll = (float) 0.0;
        throttle = (float)0.0;
        yaw = (float)(0.0);
        showToast("STOP");

    }

}
