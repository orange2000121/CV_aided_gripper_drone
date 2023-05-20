package com.dji.activationDemo;

import static com.dji.sdk.sample.internal.utils.ToastUtils.showToast;

import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.sdk.flightcontroller.FlightController;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.sqrt;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.dji.activationDemo.payload.PayloadActivity;
import com.dji.activationDemo.payload.PayloadDataTransmission;
import com.dji.sdk.sample.internal.utils.ToastUtils;

import org.opencv.core.Core;

import java.util.List;


public class FlightControlMethod extends AppCompatActivity {
    public float roll, pitch, throttle, yaw;// control the drone flying
    public boolean emg_now = false;// emergency button
    String TAG = FlightControlMethod.class.getName();
    public List<ArucoCoordinate> arucoCoordinateList=null; //current aruco coordinate list
    public FlightController flightController; //flight controller from dji sdk
    public int function_times = 0;
    private PayloadDataTransmission payload = new PayloadDataTransmission(FlightControlMethod.this);
    private Context context = null;
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
    public boolean register(Context context){
        if(context==null) return false;
        this.context = context;
        return true;
    }
    /* -------------------------------------------------------------------------- */
    /*                                main activity                               */
    /* -------------------------------------------------------------------------- */

    public void test1(){
        float payload_x_offset = 0.0f;
        float payload_y_offset = 0.0f;
        float payload_z_offset = 0.9f;

        if(emg_now) return;
        switchVirtualStickMode(true);
//---------go to second aruco
//        moveTo(2f,2f,0);
        payload.gripperControl(true);
        flightAboveAruco(0, 0.08f, 0.9f, 2);
        flightAboveAruco(0,0.08f,0.5f, 2);
        flightAboveAruco(0,0.08f,0.25f, 2);
//        flightAboveAruco(0,0.08f,0.185f,2);
        moveTo(0,0,-0.07f,0.1f);
        payload.gripperControl(false);
        moveTo(0,0,1f,0.35f);
        switchVirtualStickMode(false);
    }



    public void flightAboveAruco(float payload_x_offset, float payload_y_offset, float payload_z_offset, int run_time) {
        if (emg_now) return;
        int count = 0, error_count = 0;
        while (true) {
            if (emg_now) return;
            float[] location = payload.getBottomLocation();
            if(location==null){
                Log.w(TAG, "flightAboveAruco: location is null");
                //show toast on ui thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "location is null", Toast.LENGTH_SHORT).show();
                    }
                });
                if(error_count++>5) break;
                continue;
            }
            Log.i(TAG, "flightAboveAruco: "+location[0]+" "+location[1]+" "+location[2]);
            float x = location[0] + payload_x_offset;
            float y = -(location[1] + payload_y_offset);
            float z = -location[2] + payload_z_offset;
            if (max(abs(x), max(abs(y), abs(z))) < 0.02f) break;
            if(count++>run_time) break;
            moveTo(x, y, z, 0.1f);
        }
    }

    public void startPos(){
        if(emg_now) return;
        switchVirtualStickMode(true);
        ArucoCoordinate goal = findAruco(30);
        facingTheFront(goal);
        goal = findAruco(30);
        moveTo(goal.x, goal.z -2.5f, -goal.y+1.0f);
        goal = findAruco(30);
        facingTheFront(goal);
        goal = findAruco(30);
        moveTo(goal.x, goal.z -2.5f, -goal.y+1.0f);
        switchVirtualStickMode(false);
    }
    public void calib(){
        if(emg_now) return;
        switchVirtualStickMode(true);
        ArucoCoordinate goal = findAruco(23);
        facingTheFront(goal);
        goal = findAruco(23);
        moveTo(goal.x, goal.z -1.5f, -goal.y+0.5f);
        switchVirtualStickMode(false);
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

    private boolean  facingTheFront(ArucoCoordinate aruco){
        if (emg_now) return false;
//        if (aruco.roll <= -25 || aruco.roll >= 25) {
//            Log.e(TAG, "normalizeForwardAngle: aruco.roll is not zero");
//            return false;
//        }
        float rotation_speed = 10;
        float time = abs(aruco.pitch / rotation_speed);
        yaw = aruco.pitch < 0 ? -rotation_speed : rotation_speed;
        SystemClock.sleep((long) time * 1000);
        setZero();
        return true;
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
    public void moveTo(float right_left_gap, float front_back_gap, float up_down_gap){
        moveTo(right_left_gap, front_back_gap, up_down_gap, 0.5f);
    }
    /**
     * @param right_left_gap : the gap between drone and aruco marker in x axis
     * @param front_back_gap : the gap between drone and aruco marker in z axis
     * @param up_down_gap : the gap between drone and aruco marker in y axis
     * @param max_speed : limit the max speed of the drone
     */
    public void moveTo(float right_left_gap, float front_back_gap, float up_down_gap, float max_speed) {
        if(emg_now) return;
        if(max_speed > 1) {
            Log.e(TAG, "moveTo: max_speed is too large");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showToast("max_speed is too large");
                }
            });
            return;
        };
        float distance = (float) sqrt(right_left_gap * right_left_gap + front_back_gap * front_back_gap + up_down_gap * up_down_gap);
        float higher_distance = (float) max(abs(right_left_gap),max(abs(front_back_gap), abs(up_down_gap)) );
        // if the distance is less than 1 meter, make the max speed smaller
        if (higher_distance < 1 && max_speed > 0.5f) {
            max_speed *= 0.5f;
        }
        roll = (float) front_back_gap / distance * max_speed;
        throttle = (float) up_down_gap / distance * max_speed;
        pitch = (float) right_left_gap / distance * max_speed;
        double flying_time = distance / sqrt(roll * roll + pitch * pitch + throttle * throttle);
        Log.i(TAG, "roll: " + roll);
        Log.i(TAG, "pitch: " + pitch);
        Log.i(TAG, "throttle: " + throttle);
        Log.i(TAG, "flying_time: " + flying_time);
        if (distance > 5) {// if the distance is too long, don't move
            setZero();
        } else {
            SystemClock.sleep((long) (flying_time * 1000));// when the drone is moving, wait for the drone to move
            setZero();
        }
    }
    private void rotateTo(double radius, double yaw_angle) {
        if(emg_now) return;
        float roll_speed = 0.3f ;
        double arc_length = radius * abs(yaw_angle) * 3.1416 / 180;
        double time = abs(arc_length / roll_speed);
        yaw = (float) ((float) yaw_angle/time);
        roll = (float) roll_speed;
        SystemClock.sleep((long) time * 1000);
        setZero();

    }
    private void rotation(double yaw_angle){
        if(emg_now) return;
        float yaw_speed = 15;
        double time = abs(yaw_angle / yaw_speed);
        yaw = yaw_angle < 0 ? -yaw_speed : yaw_speed;
        SystemClock.sleep((long) time * 1000);
        setZero();
    }
    private void switchVirtualStickMode(boolean enable){
        flightController.setVirtualStickModeEnabled(enable, djiError -> {
            flightController.setVirtualStickAdvancedModeEnabled(true);
            if (djiError != null) {
                ToastUtils.setResultToToast(djiError.getDescription());
            } else {
                showToast("VS Enabled");
            }
        });
    }
    public void setZero(){
        pitch = (float)0.0;
        roll = (float) 0.0;
        throttle = (float)0.0;
        yaw = (float)(0.0);
    }
}
