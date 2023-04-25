package com.dji.activationDemo.payload;

import android.util.Log;

import com.dji.activationDemo.DemoApplication;
import com.dji.sdk.sample.internal.utils.ModuleVerificationUtil;
import com.dji.sdk.sample.internal.utils.ViewHelper;

import java.util.concurrent.Future;

import dji.sdk.payload.Payload;

public class PayloadDataTransmission {
    private static final String TAG = "PayloadSendGetData";
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
                }
            });
        }
    }

    public float[] getBottomLocation(){
        sendDataToPayload(Constants.getLocation);
        //get current time
        long startTime = System.currentTimeMillis();
        while (payloadReceiveData.equals("")){
            //wait for the data to be received
            if (System.currentTimeMillis() - startTime > 3000){
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
