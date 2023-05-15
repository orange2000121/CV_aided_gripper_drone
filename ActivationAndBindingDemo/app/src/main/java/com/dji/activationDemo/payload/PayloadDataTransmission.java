package com.dji.activationDemo.payload;

import static java.lang.Math.max;

import android.os.SystemClock;
import android.util.Log;

import com.dji.activationDemo.DemoApplication;
import com.dji.sdk.sample.internal.utils.ModuleVerificationUtil;
import com.dji.sdk.sample.internal.utils.ViewHelper;

import java.util.concurrent.Future;

import dji.sdk.payload.Payload;

public class PayloadDataTransmission {
    private static final String TAG = "PayloadDataTransmission";
    private Payload payload = null;

    String payloadReceiveData = "";

    /*constructor*/
    public PayloadDataTransmission(){
        initListener();
    }
    static class Constants{
        static final String openGripper = "y";
        static final String closeGripper = "n";
        static final String getLocation = "location";
        static final String isReceived = "received";
    }

    private void initListener() {
        if (ModuleVerificationUtil.isPayloadAvailable()) {
            payload = DemoApplication.getAircraftInstance().getPayload();
            payload.setCommandDataCallback(new Payload.CommandDataCallback() {
                @Override
                public void onGetCommandData(byte[] bytes) {
                    payloadReceiveData = ViewHelper.getString(bytes);
                    Log.i(TAG, "onGetCommandData receive data: " + payloadReceiveData);
                }
            });
        }
    }
    public float[] getBottomLocation(){
        float[] tempLocation = getReceiveLocation();
        SystemClock.sleep(500);
        float[] tempLocation2 = getReceiveLocation();
        if(tempLocation == null || tempLocation2 == null){
            return null;
        }
        float deltaX = tempLocation[0] - tempLocation2[0], deltaY = tempLocation[1] - tempLocation2[1], deltaZ = tempLocation[2] - tempLocation2[2];
        if(max(deltaX, max(deltaY, deltaZ)) > 0.1){
            return null;
        }
        return tempLocation2;
    }
    public float[] getReceiveLocation(){
        sendDataToPayload(Constants.getLocation);
        //get current time
        long startTime = System.currentTimeMillis();
        SystemClock.sleep(100);
        while (payloadReceiveData.equals("")){
            //wait for the data to be received
//            Log.e(TAG, "getBottomLocation: waiting for data, received data: " + payloadReceiveData);
            if (System.currentTimeMillis() - startTime > 1000){
                //if the data is not received in 3 second, return null
                return null;
            }
        }
        Log.e(TAG, "payloadReceiveData: " + payloadReceiveData);
        String[] locationStr = payloadReceiveData.split(",");
        float[] location = new float[3];
        location[0] = Float.parseFloat(locationStr[0]);
        location[1] = Float.parseFloat(locationStr[1]);
        location[2] = Float.parseFloat(locationStr[2]);
        if (location[0] == 0 && location[1] == 0 && location[2] == 0){
            Log.i(TAG, "getReceiveLocation: location is 0");
            return null;
        }
        payloadReceiveData = "";
        return location;
    }

    /**
     * @param isGripperOpen The boolean value to control the gripper open or close.
     */
    public void gripperControl(boolean isGripperOpen){
        if (isGripperOpen) {
            sendDataToPayload(Constants.openGripper);
        } else {
            sendDataToPayload(Constants.closeGripper);
        }
    }

    /**
     * @param sendingDataStr The data to be sent to the payload device.
     */
    private void sendDataToPayload(String sendingDataStr) {
        Log.e(TAG, "sending:" + sendingDataStr);
        final byte[] data = ViewHelper.getBytes(sendingDataStr);
        if(ModuleVerificationUtil.isPayloadAvailable() && null != payload) {
            payload.sendDataToPayload(data, djiError -> Log.i(TAG, djiError == null ? "Send data successfully" : djiError.getDescription()));
        }
    }
}
