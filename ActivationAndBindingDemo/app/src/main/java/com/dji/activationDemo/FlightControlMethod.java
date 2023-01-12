package com.dji.activationDemo;
import static com.dji.activationDemo.ToastUtils.showToast;

import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.sdk.flightcontroller.FlightController;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.sqrt;

import android.os.SystemClock;

import android.util.Log;
import java.util.List;


public class FlightControlMethod {
    public float roll, pitch, throttle, yaw;// control the drone flying
    public boolean emg_now = false;// emergency button
    String TAG = "FlightControlMethod";
    public List<ArucoCoordinate> arucoCoordinateList=null; //current aruco coordinate list
    private FlightController flightController; //flight controller from dji sdk
    public int function_times = 0;
    /* ------------------------------- Constructive ------------------------------- */
    public FlightControlMethod(){}

    /**
     * @param flightController : flight controller from dji sdk
     * @return : if register successfully, return true, else return false
     */
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

    public void test1(){
        if(emg_now) return;
        //---------go to first aruco
        ArucoCoordinate goal = findAruco(23);
        if(goal==null) return;
        moveTo(goal.x, goal.z-1.5, -goal.y+1.2);
        goal = findAruco(23);
        facingTheFront(goal);
        goal = findAruco(23);
        moveTo(goal.x, goal.z-0.25, -goal.y+1.2);
        //-----grip
        moveTo(0,0, -0.53); //move down to grip
        SystemClock.sleep(3000);
        moveTo(0,0, 0.53); //move up to take off
        //---------go to second aruco
        goal = findAruco(31);
        if(goal==null) return;
        facingTheFront(goal);
        SystemClock.sleep(500);
        goal = findAruco(31);
        moveTo(goal.x*0.8, goal.z-1.7, -goal.y+1);
        goal = findAruco(31);
        moveTo(goal.x, goal.z -1.5, -goal.y+1);
        goal = findAruco(31);
        facingTheFront(goal);
        goal = findAruco(31);
        moveTo(goal.x, goal.z -1.2, -goal.y+1);
    }
    public void test2(){

    }


    public void goToArucoMarker(int aruco_id){
        if(emg_now) return;
        ArucoCoordinate goal_aruco = findAruco(aruco_id);
        if(goal_aruco==null) return;
        moveTo(goal_aruco.x, goal_aruco.z - 2, -goal_aruco.y+1);
        goal_aruco = findAruco(aruco_id);
        if(goal_aruco==null){
            Log.e(TAG, "goToArucoMarker: goal_aruco==null");
            return;
        }
        normalizeForwardAngle(goal_aruco);
    }
    public void emergency(){
        emg_now = !emg_now;
        setZero();
        flightController.setVirtualStickModeEnabled(false, djiError -> {
            flightController.setVirtualStickAdvancedModeEnabled(true);
            if (djiError != null) {
                ToastUtils.setResultToToast(djiError.getDescription());
            } else {
                showToast("VS Disabled");
            }
        });
    }
    /**
     * Find the specified id in the list
     * @param aruco_id : the id you want to find,type: int
     * @return : if found it return aruco coordinate, else return null, type: ArucoCoordinate
     */
    public ArucoCoordinate findAruco(int aruco_id){
        if(emg_now) return null;
        function_times = 0;
        while (true){
            if(emg_now) return null;
            if (arucoCoordinateList == null) {
                Log.w(TAG,"Didn't register aruco list");
                return null;
            }
            if (function_times >100) return null;
            function_times++;
            yaw = 10;// can be changed
            //determine the aruco is in the list
            for(ArucoCoordinate arucoCoordinate : arucoCoordinateList){
                if(arucoCoordinate.id == aruco_id){
                    setZero();
                    return arucoCoordinate;
                }
            }
            //if not found, keep searching
            SystemClock.sleep(1000);
        }
    }

    /**
     * aruco marker必須在正前方，Roll 必須為零
     * aruco.pitch當作旋轉的角度，aruco.z當作旋轉半徑。<br/>
     * 旋轉到aruco marker的正前方。<br/>
     * @param aruco : the aruco marker you want to rotate to
     */
    void normalizeForwardAngle(ArucoCoordinate aruco){
        if(emg_now) return;
        if(aruco.roll<=-15 || aruco.roll>=15){
            Log.e(TAG, "normalizeForwardAngle: aruco.roll is not zero");
            return;
        }
        //degree to radian
        float radian = (float) (aruco.pitch * Math.PI / 180);
        float radius = aruco.z;
        float arc_length = radius * radian;
        float time = abs(aruco.pitch/5) ; // 5 degree per second
        //set the speed
        pitch = -arc_length/time; // 左右移動和旋轉不同方向
        yaw = aruco.pitch<0 ? -5 : 5;// 5 degree per second
        Log.i(TAG, "arc_length: " + arc_length);
        Log.i(TAG, "angle: " + aruco.pitch);
        Log.i(TAG, "pitch: " + pitch);
        Log.i(TAG, "yaw: " + yaw);
        Log.i(TAG, "time: " + time);
        //set the time
        SystemClock.sleep((long) time*1000);
        setZero();
    }

    private void facingAndMoveTo(double right_left_gap, double front_back_gap, double up_down_gap, ArucoCoordinate aruco){
        if (emg_now) return;
        if (aruco.roll <= -15 || aruco.roll >= 15) {
            Log.e(TAG, "normalizeForwardAngle: aruco.roll is not zero");
            return;
        }
        double distance = sqrt(right_left_gap * right_left_gap + front_back_gap * front_back_gap + up_down_gap * up_down_gap);
        float higher_speed = (float) max(abs(right_left_gap),max(abs(front_back_gap), abs(up_down_gap)) );

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
        pitch /= 2;
        throttle /= 2;
        Log.i(TAG, "roll: " + roll);
        Log.i(TAG, "pitch: " + pitch);
        Log.i(TAG, "throttle: " + throttle);
        double flying_time = distance / sqrt(roll * roll + pitch * pitch + throttle * throttle);

        yaw = (float) (aruco.pitch / flying_time);

        if (flying_time > 10) {// if the flying time is too long, don't move
            setZero();
        } else {
            SystemClock.sleep((long) (flying_time * 1000));// when the drone is moving, wait for the drone to move
            setZero();
        }


    }

    private void facingTheFront(ArucoCoordinate aruco){
        if (emg_now) return;
        if (aruco.roll <= -15 || aruco.roll >= 15) {
            Log.e(TAG, "normalizeForwardAngle: aruco.roll is not zero");
            return;
        }
        float rotation_speed = 10;
        float time = abs(aruco.pitch / rotation_speed);
        yaw = aruco.pitch < 0 ? -rotation_speed : rotation_speed;
        SystemClock.sleep((long) time * 1000);
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
    /**
     * @param right_left_gap : the gap between drone and aruco marker in x axis
     * @param front_back_gap : the gap between drone and aruco marker in z axis
     * @param up_down_gap : the gap between drone and aruco marker in y axis
     */
    public void moveTo(double right_left_gap, double front_back_gap, double up_down_gap) {
        if(emg_now) return;
        // todo: 確認這段註解是否要保留
//        flightController.setVirtualStickModeEnabled(true, djiError -> {
//            flightController.setVirtualStickAdvancedModeEnabled(true);
//            if (djiError != null) {
//                ToastUtils.setResultToToast(djiError.getDescription());
//            }
//        });
        double distance = sqrt(right_left_gap * right_left_gap + front_back_gap * front_back_gap + up_down_gap * up_down_gap);
        float higher_speed = (float) max(abs(right_left_gap),max(abs(front_back_gap), abs(up_down_gap)) );

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
        pitch /= 2;
        throttle /= 2;
        Log.i(TAG, "roll: " + roll);
        Log.i(TAG, "pitch: " + pitch);
        Log.i(TAG, "throttle: " + throttle);
        double flying_time = distance / sqrt(roll * roll + pitch * pitch + throttle * throttle);
        if (flying_time > 10) {// if the flying time is too long, don't move
            setZero();
        } else {
            SystemClock.sleep((long) (flying_time * 1000));// when the drone is moving, wait for the drone to move
            setZero();
        }
    }
    public void setZero(){
        pitch = (float)0.0;
        roll = (float) 0.0;
        throttle = (float)0.0;
        yaw = (float)(0.0);
    }

}
