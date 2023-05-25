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

import com.dji.activationDemo.payload.PayloadDataTransmission;
import com.dji.sdk.sample.internal.utils.ToastUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public class FlightControlMethod extends AppCompatActivity {
    public float roll, pitch, throttle, yaw;// control the drone flying
    public boolean emg_now = false;// emergency button
    String TAG = FlightControlMethod.class.getName();
    public List<ArucoCoordinate> arucoCoordinateList=null; //current aruco coordinate list
    public FlightController flightController; //flight controller from dji sdk
    public int function_times = 0;
    public PayloadDataTransmission payload = new PayloadDataTransmission(FlightControlMethod.this);
    private Context context = null;
    private final Map<Float,List<Float>> param_of_bottom_aruco = new HashMap<Float,List<Float>>();
    /* ------------------------------- Constructive ------------------------------- */
    public FlightControlMethod(){
        param_of_bottom_aruco.put(25f, List.of(0.166f,0.1525f));
        param_of_bottom_aruco.put(26f, List.of(0.18f,-0.1395f));
        param_of_bottom_aruco.put(27f, List.of(-0.175f,-0.1525f));
        param_of_bottom_aruco.put(28f, List.of(-0.1495f,0.1525f));
    }

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

    public void takeBall(){
        float payload_x_offset = 0.0f;
        float payload_y_offset = -0.07f;
        float payload_z_offset = 0.9f;

        if(emg_now) return;
        switchVirtualStickMode(true);
//---------go to second aruco
        payload.gripperControl(true);
        flightAboveAruco(0, -0.07f, 0.9f, 1,0.2f);
        flightAboveAruco(0,-0.07f,0.5f, 1,0.15f);
        flightAboveAruco(0,-0.07f,0.3f, 1,0.1f);
        moveTo(0,0,-0.09f,0.1f);
        payload.gripperControl(false);
        SystemClock.sleep(500);
        moveTo(0,0,1.5f,0.35f);
    }
    public void testTakeBall(){
        float payload_x_offset = 0.0f;
        float payload_y_offset = -0.07f;
        float payload_z_offset = 0.9f;

        if(emg_now) return;
        switchVirtualStickMode(true);
//---------go to second aruco
        payload.gripperControl(true);
        flightAboveAruco(-0.12f, -0.07f, 0.9f, 1,0.2f);
        flightAboveAruco(-0.12f, -0.07f, 0.7f, 1,0.2f);
        flightAboveAruco(-0.12f,-0.07f,0.5f, 1,0.15f);
        flightAboveAruco(-0.12f,-0.07f,0.35f, 1,0.1f);
        moveTo(0,0,-0.14f,0.1f);
        payload.gripperControl(false);
        SystemClock.sleep(500);
        payload.gripperControl(true);
        SystemClock.sleep(500);
        moveTo(0,0,0.5f,0.35f);
    }

    public void demo1(){
        if (emg_now) return;
        switchVirtualStickMode(true);
        float start_orientation = getOrientation();
        SystemClock.sleep(500);
        takeOff();
        SystemClock.sleep(6000);
        moveTo(2,2,0);
        SystemClock.sleep(500);
        takeBall();
        SystemClock.sleep(500);
        float end_orientation = getOrientation();
        float delta = end_orientation - start_orientation;
        if(delta>180) delta -= 360;
        if(delta<-180) delta += 360;
        rotation(-delta);
        SystemClock.sleep(500);
        moveTo(-2,1,0);
        moveTo(0,1,0,1);
        payload.gripperControl(true);
        moveTo(0,2.5f,0,0.8f);
        SystemClock.sleep(500);
//        landing();
        switchVirtualStickMode(false);
    }

    public void demo2(){
        if (emg_now) return;
        switchVirtualStickMode(true);
        float start_orientation = getOrientation();
        SystemClock.sleep(500);
        takeOff();
        SystemClock.sleep(6000);
        //go to ball
        moveTo(2,2,0);
        SystemClock.sleep(500);
        takeBall();
        SystemClock.sleep(500);
        float end_orientation = getOrientation();
        float delta = end_orientation - start_orientation;
        if(delta>180) delta -= 360;
        if(delta<-180) delta += 360;
        rotation(-delta);
        SystemClock.sleep(500);
        //go to fire
        moveTo(1.5f,1,0);
        moveTo(0,1.5f,0,1);
        payload.gripperControl(true);
        moveTo(0,3f,0,0.8f);
        SystemClock.sleep(500);
        switchVirtualStickMode(false);
        landing();
    }
    public void demo3(){
        if (emg_now) return;
        switchVirtualStickMode(true);
        float start_orientation = getOrientation();
        SystemClock.sleep(500);
        takeOff();
        SystemClock.sleep(6000);
        //go to ball
        moveTo(2,2,0);
        SystemClock.sleep(500);
        takeBall();
        SystemClock.sleep(500);
        float end_orientation = getOrientation();
        float delta = end_orientation - start_orientation;
        if(delta>180) delta -= 360;
        if(delta<-180) delta += 360;
        rotation(-delta);
        SystemClock.sleep(500);
        //go to fire
        moveTo(1.5f,1,0);
        throughBall(0,2,0);
        moveTo(0,3f,0,0.8f);
        SystemClock.sleep(500);
        switchVirtualStickMode(false);
        landing();
    }

    public void flightAboveAruco(float payload_x_offset, float payload_y_offset, float payload_z_offset, int run_time,float speed){
        if (emg_now) return;
        int count = 0, error_count = 0;
        while (true) {
            if (emg_now) return;
            if(count++>run_time) break;
            Float[] location = payload.getBottomLocation();
            if(location==null){
                Log.w(TAG, "flightAboveAruco: location is null");
                //show toast on ui thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "location is null", Toast.LENGTH_SHORT).show();
                    }
                });
                moveTo(0,0,0.1f,0.1f);
                count--;
                if(error_count++>5) break;
                continue;
            }
            Log.i(TAG, "flightAboveAruco: "+location[0]+" "+location[1]+" "+location[2]);
            runOnUiThread(()->{
                Toast.makeText(context, "x: "+location[0]+"\ny: "+location[1]+"\nz: "+location[2], Toast.LENGTH_SHORT).show();
            });
            float id = location[6];
            float x = (location[0] + payload_x_offset) - Objects.requireNonNull(param_of_bottom_aruco.get(id)).get(0);
            float y = -(location[1] - payload_y_offset) - Objects.requireNonNull(param_of_bottom_aruco.get(id)).get(1);
            float z = -location[2] + payload_z_offset;
            float yaw = location[3];
            if (max(abs(x), max(abs(y), abs(z))) < 0.02f) break;
            rotation(yaw);
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    Toast.makeText(context, "x: "+x+"\ny: "+y+"\nz: "+z, Toast.LENGTH_SHORT).show();
//                }
//            });
            Log.i(TAG, "PreMoveTo: "+x+" "+y+" "+z);
            if(speed>0.2f){
                speed = 0.2f;
            }
            moveTo(x, y, z, speed);
        }
    }

    public void throughBall(float init_x, float init_y, float init_z){
        if(emg_now) return;
        payload.findCircleLocation();
        int breakpoint_num = 10;
        for (int i=0;i<breakpoint_num;i++){
            if(emg_now) return;
            float[] location = payload.getCircleLocation();
            float x,y,z;
            x = init_x/breakpoint_num;
            y = init_y/breakpoint_num;
            z = init_z/breakpoint_num;
            if(location == null){
                moveTo(x,y,z);
            }else{
                if(abs(location[0])<0.2 && location[1] < 0.2){
                    moveTo(x,y,z);
                }else if(abs(location[0])>=0.2 && location[1] < 0.2){
                    moveTo(location[0],y,z);
                }else{
                    break;
                }
            }
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
        if(abs(yaw_angle) < yaw_speed) yaw_speed = (float) abs(yaw_angle);
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
    private float getOrientation(){
        return flightController.getCompass().getHeading();
    }
    private void takeOff(){
        if(emg_now) return;
        flightController.startTakeoff(djiError -> {
            if (djiError != null) {
               Log.e(TAG, "takeOff: " + djiError.getDescription());
            } else {
                Log.i(TAG, "takeOff: success");
            }
        });
    }
    private void landing(){
        if(emg_now) return;
        flightController.startLanding(djiError -> {
            if (djiError != null) {
                Log.e(TAG, "landing: " + djiError.getDescription());
            } else {
                Log.i(TAG, "landing: success");
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
