package com.dji.activationDemo.payload;

import static java.lang.Math.max;

import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import com.dji.activationDemo.DemoApplication;
import com.dji.sdk.sample.internal.utils.ModuleVerificationUtil;
import com.dji.sdk.sample.internal.utils.ViewHelper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import dji.sdk.payload.Payload;

public class PayloadDataTransmission extends AppCompatActivity {
    private static final String TAG = "PayloadDataTransmission";
    public Payload payload = null;
    private Context context = null;
    public String payloadReceiveData = "";
    private String lastRequest = "";
    private final Map<Float,List<Float>> param_of_bottom_aruco = new HashMap<Float,List<Float>>();

    /*constructor*/
    public PayloadDataTransmission(Context context){
        this.context = context;
        //-----marker lenth: 0.058m
        param_of_bottom_aruco.put(25f, List.of(-0.1535f,0.134f));
        param_of_bottom_aruco.put(26f, List.of(0.1535f,0.134f));
        param_of_bottom_aruco.put(27f, List.of(0.1535f,-0.134f));
        param_of_bottom_aruco.put(28f, List.of(-0.1535f,-0.134f));
        //-----marker lenth: 0.03m
//        param_of_bottom_aruco.put(25f, List.of(0.15f,0.118f));
//        param_of_bottom_aruco.put(26f, List.of(0.143f,-0.16f));
//        param_of_bottom_aruco.put(27f, List.of(-0.0455f,-0.145f));
//        param_of_bottom_aruco.put(28f, List.of(-0.165f,0.12f));
        initListener();
    }
    static class Constants{
        static final String openGripper = "y";
        static final String closeGripper = "n";
        static final String getLocation = "location";
        static final String isReceived = "received";
        static final String findCircle = "circle";
        static final String stopFindCircle = "stop_circle";
        static final String throwBall = "throw";
        static final String gripBall = "grip";
        static final String sonarDistance = "sonar";
    }

    private void initListener() {
        if (ModuleVerificationUtil.isPayloadAvailable()) {
            payload = DemoApplication.getAircraftInstance().getPayload();
            payload.setCommandDataCallback(new Payload.CommandDataCallback() {
                @Override
                public void onGetCommandData(byte[] bytes) {
                    String temp = ViewHelper.getString(bytes);
//                    linkReceiveData(temp);
                    payloadReceiveData = temp;
                    Log.i(TAG, "raw receive data: " + temp);
                }
            });
        }
    }
    String temp_receive_data = "";
    private void linkReceiveData(String data){
        if(data.equals("[]")){
            payloadReceiveData = "";
            return;
        }
        if(data.isEmpty()) {
            payloadReceiveData = "";
            return;
        }
        if (data.equals("null")){
            payloadReceiveData = "";
            return;
        }
        if(data.charAt(0) == '[' && data.charAt(data.length() - 1) == ']'){
            payloadReceiveData = data;
        }else if(data.charAt(0) == '['){
            temp_receive_data = data;
        }else if(data.charAt(data.length() - 1) == ']'){
            temp_receive_data += data;
            payloadReceiveData = temp_receive_data;
            temp_receive_data = "";
        }else if(temp_receive_data.equals("null")){
            payloadReceiveData = "null";
        }
        else{
            temp_receive_data += data;
        }
    }

    float[] circle_location;
    boolean findCircleLocationFlag = false;

    public Float[] getCircleLocation(){
        sendDataToPayload(Constants.findCircle);
        lastRequest = Constants.findCircle;
        //get current time
        long startTime = System.currentTimeMillis();
        while (payloadReceiveData.equals("")){
            //wait for the data to be received
            if (System.currentTimeMillis() - startTime > 1000){
                //if the data is not received in 3 second, return null
                return null;
            }
        }
        if(payloadReceiveData.equals("(null)")) return null;
        if(payloadReceiveData.equals("null")) return null;
        if(payloadReceiveData.equals("[]")) return null;

        Log.i(TAG, "getCircleLocation: " + payloadReceiveData);
        Gson gson = new Gson();
        JsonArray jsonArray = gson.fromJson(payloadReceiveData, JsonArray.class);
        if(jsonArray == null) return null;
        if(jsonArray.size() == 0) return null;
        payloadReceiveData = "";
        for (JsonElement jsonElement : jsonArray) {
            Float[] circle = new Float[2];
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            if(jsonObject.get("x_coor") == null) continue;
            if(jsonObject.get("x_coor").getAsString().equals("nan")) continue;
            circle[0] = jsonObject.get("x_coor").getAsFloat();
            circle[1] = jsonObject.get("y_coor").getAsFloat();
            Log.i(TAG, "getCircleLocation: " + circle[0] + " " + circle[1]);
            if(circle[0] == 0 && circle[1] == 0) return null;
            return circle;
        }
        return null;
    }
//    public void throwBll(){
//        sendDataToPayload(Constants.throwBall);
//        lastRequest = Constants.throwBall;
//    }
    public void stopFindCircleLocation(){
        findCircleLocationFlag = false;
        sendDataToPayload(Constants.stopFindCircle);
        lastRequest = Constants.stopFindCircle;
    }

    public Float[] getBottomLocation(){
//        Float[] tempLocation = getReceiveLocation();
//        SystemClock.sleep(300);
//        Float[] tempLocation2 = getReceiveLocation();
//        if(tempLocation == null || tempLocation2 == null){
//            return null;
//        }
//        if(!Objects.equals(tempLocation[6], tempLocation2[6])){
//            return null;
//        }
//        float deltaX = tempLocation[0] - tempLocation2[0], deltaY = tempLocation[1] - tempLocation2[1], deltaZ = tempLocation[2] - tempLocation2[2];
//        if(max(deltaX, max(deltaY, deltaZ)) > 0.1){
//            return null;
//        }
        Float[] tempLocation = getReceiveLocation();
        if(tempLocation == null) return null;
        float id = tempLocation[6];
        if(id != 25 && id != 26 && id != 27 && id !=28) return null;
        tempLocation[0] = tempLocation[0] - Objects.requireNonNull(param_of_bottom_aruco.get(id)).get(0);
        tempLocation[1] = -tempLocation[1] - Objects.requireNonNull(param_of_bottom_aruco.get(id)).get(1);
        return tempLocation;
    }
    public Float[] getReceiveLocation(){
        sendDataToPayload(Constants.getLocation);
        if(!lastRequest.equals(Constants.getLocation)){
            SystemClock.sleep(300);
        }
        lastRequest = Constants.getLocation;
        //get current time
        long startTime = System.currentTimeMillis();
        while (payloadReceiveData.equals("")){
            //wait for the data to be received
            if (System.currentTimeMillis() - startTime > 1500){
                //if the data is not received in 3 second, return null
                return null;
            }
        }
        if(payloadReceiveData.equals("(null)")) return null;
        if(payloadReceiveData.equals("null")) return null;
        if(payloadReceiveData.equals("[]")) return null;
        Gson gson = new Gson();
        JsonArray jsonArray = gson.fromJson(payloadReceiveData, JsonArray.class);
        if (jsonArray != null) {
            if(jsonArray.size() == 0){
                return null;
            }
        }else {
            payloadReceiveData = "";
            return null;
        }
        for (JsonElement jsonElement : jsonArray) {
            Float[] location = new Float[7];
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            if(jsonObject.get("x") == null) continue;
            if(jsonObject.get("x").getAsString().equals("nan")) continue;
            location[0] = jsonObject.get("x").getAsFloat();
            location[1] = jsonObject.get("y").getAsFloat();
            location[2] = jsonObject.get("z").getAsFloat();
            location[3] = jsonObject.get("yaw").getAsFloat();
            location[4] = jsonObject.get("pitch").getAsFloat();
            location[5] = jsonObject.get("roll").getAsFloat();
            location[6] = jsonObject.get("id").getAsFloat();
            return location;
        }
        return null;
    }

    public void startGripBall(){
        sendDataToPayload(Constants.gripBall);
        lastRequest = Constants.gripBall;
    }
    public boolean getGripStatus(){
        JsonArray jsonArray = analyzeReceiveData();
        if(jsonArray == null) return false;
        for (JsonElement jsonElement : jsonArray) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            if(jsonObject.get("triggered") == null) continue;
            if(jsonObject.get("triggered").getAsString().equals("nan")) continue;
            Log.i(TAG, "getGripStatus: " + jsonObject.get("triggered").getAsBoolean());
            return jsonObject.get("triggered").getAsBoolean();
        }
        return false;
    }
    public Float getSonarDistance(){
        if(!lastRequest.equals(Constants.sonarDistance)){
            SystemClock.sleep(300);
        }
        sendDataToPayload(Constants.sonarDistance);
        lastRequest = Constants.sonarDistance;
        JsonArray jsonArray = analyzeReceiveData();
        if(jsonArray == null) return null;
        for (JsonElement jsonElement : jsonArray) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            if(jsonObject.get("sonar") == null) continue;
            if(jsonObject.get("sonar").getAsString().equals("nan")) continue;
            Log.i(TAG, "getSonarDistance: " + jsonObject.get("sonar").getAsFloat());
            return jsonObject.get("sonar").getAsFloat();
        }
        return null;
    }

    /**
     * @param isGripperOpen The boolean value to control the gripper open or close.
     */
    public void gripperControl(boolean isGripperOpen){
        if (isGripperOpen) {
            sendDataToPayload(Constants.openGripper);
            lastRequest = Constants.openGripper;
        } else {
            sendDataToPayload(Constants.closeGripper);
            lastRequest = Constants.closeGripper;
        }
    }

    private JsonArray analyzeReceiveData(){
        long startTime = System.currentTimeMillis();
        while (payloadReceiveData.equals("")){
            //wait for the data to be received
            if (System.currentTimeMillis() - startTime > 1500){
                //if the data is not received in 3 second, return null
                return null;
            }
        }
        if(payloadReceiveData.equals("(null)")) return null;
        if(payloadReceiveData.equals("null")) return null;
        if(payloadReceiveData.equals("[]")) return null;
        Gson gson = new Gson();
        JsonArray jsonArray = gson.fromJson(payloadReceiveData, JsonArray.class);
        payloadReceiveData = "";
        if (jsonArray != null) {
            if(jsonArray.size() == 0){
                return null;
            }
        }else {
//            payloadReceiveData = "";
            return null;
        }
        return jsonArray;
    }

    /**
     * @param sendingDataStr The data to be sent to the payload device.
     */
    private void sendDataToPayload(String sendingDataStr) {
//        Log.e(TAG, "sending:" + sendingDataStr);
        final byte[] data = ViewHelper.getBytes(sendingDataStr);
        if(ModuleVerificationUtil.isPayloadAvailable() && null != payload) {
            payload.sendDataToPayload(data, djiError ->{
                if (djiError != null) {
                    Log.e(TAG, "sendDataToPayload: " + djiError.getDescription());
                }
            });
        }
    }
}
