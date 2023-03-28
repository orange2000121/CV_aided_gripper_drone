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

import android.os.SystemClock;

import android.util.Log;

import com.dji.sdk.sample.internal.utils.ToastUtils;

import java.util.ArrayList;
import java.util.List;


public class FlightControlMethod {
    public float roll, pitch, throttle, yaw;// control the drone flying
    public boolean emg_now = false;// emergency button
    String TAG = FlightControlMethod.class.getName();
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
//        if(emg_now) return;
//        //---------go to first aruco
//        ArucoCoordinate goal = findAruco(23);
//        if(goal==null) return;
//        moveTo(goal.x, goal.z-1.5, -goal.y+1.2);
//        goal = findAruco(23);
//        facingTheFront(goal);
//        goal = findAruco(23);
//        moveTo(goal.x, goal.z-0.45, -goal.y+1.2);
//        switchVirtualStickMode(false);
        if(emg_now) return;
        switchVirtualStickMode(true);
//---------go to second aruco
        ArucoCoordinate goal = findAruco(23);
        if(goal==null) return;
        moveTo(goal.x/3, (goal.z-1.48)/3, (-goal.y+0.45)/3);
        goal = findAruco(23);
        facingTheFront(goal);
        goal = findAruco(23);
        moveTo((goal.x-0.03)*2/3, (goal.z -1.48)*2/3, (-goal.y+0.45)*2/3);
//        goal = findAruco(23);
//        facingTheFront(goal);
//        goal = findAruco(23);
//        moveTo(goal.x-0.01, goal.z -1.70, -goal.y+0.45);
//        goal = findAruco(23);
//        facingTheFront(goal);
        switchVirtualStickMode(false);

    }

    public void test1_1(){
        float x_offset = 0.0f;
        float y_offset = 1.0f;
        float z_offset = -1.7f;
        if(emg_now) return;
        switchVirtualStickMode(true);
        //-----------------go to first aruco
        ArucoCoordinate goal = findAruco(23);
        if(goal==null) return;
//        for(int i=0;i<2;i++){
//            if(facingTheFront(goal)) break;
//            goal = findAruco(23);
//        }
//        goal = findAruco(23);
        moveTo((goal.x+x_offset)/2, (goal.z+z_offset)/2, -goal.y+y_offset);
        goal = findAruco(23);
        moveTo((goal.x+x_offset)/2, (goal.z+z_offset)/2, -goal.y+y_offset);
        goal = findAruco(23);
        for(int i=0;i<2;i++){
            if(facingTheFront(goal)) break;
            goal = findAruco(23);
        }
        goal = findAruco(23);
        moveTo(goal.x+x_offset, goal.z+z_offset, -goal.y+y_offset);
        switchVirtualStickMode(false);
    }
    public void test1_2(){
        if(emg_now) return;
        switchVirtualStickMode(true);
        float x_offset = -0.64f;
        float y_offset = 1.0f;
        float z_offset = -.95f;
        //-----------------go to first aruco
        ArucoCoordinate goal = findAruco(23);
        if(goal==null) return;
        moveTo((goal.x+x_offset)/2, (goal.z+z_offset)/2, -goal.y+y_offset);
        goal = findAruco(23);
        moveTo((goal.x+x_offset)/2, (goal.z+z_offset)/2, -goal.y+y_offset);
        goal = findAruco(23);
        for(int i=0;i<2;i++){
            if(facingTheFront(goal)) break;
            goal = findAruco(23);
        }
        goal = findAruco(23);
        moveTo(goal.x+x_offset, goal.z+z_offset, -goal.y+y_offset);
        switchVirtualStickMode(false);
    }
    public void calib(){
        if(emg_now) return;
        switchVirtualStickMode(true);
        ArucoCoordinate goal = findAruco(23);
        facingTheFront(goal);
        goal = findAruco(23);
        moveTo(goal.x, goal.z -1.5, -goal.y+0.5);
        switchVirtualStickMode(false);
    }
    public void test2(){
        if(emg_now) return;
        switchVirtualStickMode(true);
//---------go to second aruco
        rotation(85);
        ArucoCoordinate goal = findAruco(31);
        if(goal==null) return;
//        facingTheFront(goal);
//        SystemClock.sleep(500);
//        goal = findAruco(31);
        moveTo((goal.x-0.38)*0.3, (goal.z-1)*0.3, -goal.y+1);
        goal = findAruco(31);
//        facingTheFront(goal);
//        goal = findAruco(31);
        moveTo((goal.x-0.38)*2/3, (goal.z -1)*2/3, -goal.y+.8);
//        goal = findAruco(31);
//        facingTheFront(goal);
//        goal = findAruco(31);
//        moveTo(goal.x, goal.z -1.2, -goal.y+.8);
        switchVirtualStickMode(false);
    }
    public void test2_1(){
        float x_offset = -0.38f;
        float y_offset = 1.0f;
        float z_offset = -1.2f;
        if(emg_now) return;
        switchVirtualStickMode(true);
//---------go to second aruco
        rotation(90);
        ArucoCoordinate goal = findAruco(31);
        if(goal==null) return;
        SystemClock.sleep(500);
        goal = findAruco(31);
        moveTo((goal.x+x_offset)/3, (goal.z+z_offset)/3, -goal.y+y_offset);
        goal = findAruco(31);
        moveTo((goal.x+x_offset)/3, (goal.z+z_offset)/3, -goal.y+y_offset);
        goal = findAruco(31);
        for (int i=0;i<2;i++){
            if(facingTheFront(goal)) break;
            goal = findAruco(31);
        }
        goal = findAruco(31);
        moveTo(goal.x+x_offset, goal.z+z_offset, -goal.y+y_offset);
        switchVirtualStickMode(false);
    }
    public void test2_2(){
        if(emg_now) return;
        switchVirtualStickMode(true);
        float x_offset = -1.4f;
        float y_offset = 1.0f;
        float z_offset = -1.0f;
        //-----------------go to first aruco
        rotation(-90);
        ArucoCoordinate goal = findAruco(31);
        if(goal==null) return;
        moveTo((goal.x+x_offset)/2, (goal.z+z_offset)/2, -goal.y+y_offset);
        goal = findAruco(31);
        moveTo((goal.x+x_offset)/2, (goal.z+z_offset)/2, -goal.y+y_offset);
        goal = findAruco(31);
        for(int i=0;i<2;i++){
            if(facingTheFront(goal)) break;
            goal = findAruco(31);
        }
        goal = findAruco(31);
        moveTo(goal.x+x_offset, goal.z+z_offset, -goal.y+y_offset);
        switchVirtualStickMode(false);
    }
    public void test3() {
        float x_offset = 1.3f;
        float y_offset = 0.8f;
        float z_offset = -3f;
        if(emg_now) return;
        switchVirtualStickMode(true);
//---------go to second aruco
        ArucoCoordinate goal = findAruco(23);
        if(goal==null) return;
        moveTo((goal.x+x_offset)/2, (goal.z+z_offset)/2, (-goal.y+y_offset)/3);
        goal = findAruco(23);
        moveTo((goal.x+x_offset)/2, (goal.z+z_offset)/2, (-goal.y+y_offset)*2/3);
        goal = findAruco(23);
        facingTheFront(goal);
        goal = findAruco(23);
        moveTo(goal.x+x_offset, goal.z+z_offset, -goal.y+y_offset);
        goal = findAruco(23);
//        facingTheFront(goal);
        switchVirtualStickMode(false);
    }

    public void test4() {
        if(emg_now) return;
        switchVirtualStickMode(true);
        ArucoCoordinate aruco = findAruco(23);
        if(aruco==null) return;
        moveTo(0,0,0.7) ;
        findAruco(23);
        facingTheFront(aruco);

        moveTo(.4,4.8,0);
        rotateTo(1.2, -180);
        SystemClock.sleep(500);
        yaw =10;
        SystemClock.sleep(1000);
        setZero();
        aruco = findAruco(23);
        moveTo(aruco.x+2.48,0,0);
        moveTo(0,0,0.8);
        moveTo(0,3,0 );
        moveTo(0,2.5,0 );

//        rotateTo(-90);
//        ArucoCoordinate goal =  findAruco(31);
//        facingTheFront(goal);
//        goal =  findAruco(31);
//        moveTo(0,goal.z+2,0);

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

    private boolean facingTheFront(ArucoCoordinate aruco){
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
//                 flightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
//                 flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
//                 flightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
//                 flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
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


    public void SCurveProfiling (double frontback,double leftright,double updown){


        double jmax=12, vmax =2 ,amax=3 ,xf=5*3.14/6;
        double T=(amax/jmax)+(vmax/amax)+(xf/vmax), t1=amax/jmax ,t3=(amax/jmax)+(vmax/amax),t2=t3-t1,t4=T-t3,t5=T-t2,t6=T-t1, t7=T;
        double breakpoints[] = {t1,t2,t3,t4,t5,t6,t7};
        double segments[] = {t1,t2-t1,t3-t2,t4-t3,t5-t4,t6-t5,t7-t6};
        ArrayList<Double> arr_speed= new ArrayList<>();
        ArrayList<Double> arr_time= new ArrayList<>();
        int which_stage =1;

//        boolean equality = true;
//        while(equality == true){
//            vmax=+0.5;
//            if(((amax/jmax)+(vmax/amax))<=(((amax/jmax)+(vmax/amax)+(xf/vmax))/2)){
//                equality=true;
//            }
//        }
        int counter=1;

        double time_increment = (breakpoints[0] - breakpoints[1]) / 1000,current_time = counter*time_increment;

        int now_at=0;
        while(now_at<=6){

            double last_segment = breakpoints[now_at-1];
            if(now_at==0)
                last_segment=0;
            current_time = last_segment;
            time_increment = segments[now_at]/400;

            while(breakpoints[now_at]>current_time){
                double v= MotionConstants(current_time,now_at,xf);
                current_time=(time_increment*counter)+last_segment;
                arr_speed.add(v);
                arr_speed.add(current_time);
                counter++;
            }
            now_at++;
            counter=1;
        }

    }

    public double MotionConstants(double current_time, int which_stage, double xf){

//      double[] array;
        double t = current_time;
        double v=0,jmax=12, vmax =2 ,amax=3;
        double T=(amax/jmax)+(vmax/amax)+(xf/vmax), t1=amax/jmax ,t3=(amax/jmax)+(vmax/amax),t2=t3-t1,t4=T-t3,t5=T-t2,t6=T-t1, t7=T;
        double a1=jmax, a2 =0, a3= -jmax, a4=0, a5=-jmax,a6=0, a7=jmax;

        double b1=0, b2= amax, b3=amax+(jmax*t2),b4=0,b5=jmax*t4,b6=-amax,b7=-amax-(jmax*t6);

        double c1=0,c2=((a1*t1*t1)/2+c1+(b1*t1))-((a2*t1*t1)/2+(b2*t1)),
                c3=((a2*t2*t2)/2+c2+(b2*t2))-((a3*t2*t2)/2+(b3*t2)),
                c4=((a3*t3*t3)/2+c3+(b3*t3))-((a4*t3*t3)/2+(b4*t3)),
                c5=((a4*t4*t4)/2+c4+(b4*t4))-((a5*t4*t4)/2+(b5*t4)),
                c6=((a5*t5*t5)/2+c5+(b5*t5))-((a6*t5*t5)/2+(b6*t5)),
                c7=((a6*t6*t6)/2+c6+(b6*c6))-((a7*t6*t6)/2+(b7*t6));

        //double v1=a1*t1*t1/2+b1*t1+c1,v2=a2*t2*t2/2+b2*t2+c2,v3=a3*t3*t3/2+b3*t3+c3,v4=a4*t4*t4/2+b4*t4+c4,v5=a5*t5*t5/2+b5*t5+c5,v6=a6*t6*t6/2+b6*t6+c6,v7=a7*t7*t7/2+b7*t7+c7;

        switch(which_stage){
            case 1:
                v=a1*t*t/2+b1*t+c1;
                return v;
            case 2:
                v=a2*t*t/2+b2*t+c2;
                break;  //optional
            case 3:
                v=a3*t*t/2+b3*t+c3;
                return v;
            case 4:
                v=a4*t*t/2+b4*t+c4;
                return v;
            case 5:
                v=a5*t*t/2+b5*t+c5;
                return v;
            case 6:
                v=a6*t*t/2+b6*t+c6;
                return v;
            case 7:
                v=a7*t*t/2+b7*t+c7;
                return v;
            default:
                setZero();
                break;

        }
        return v;
    }

}
